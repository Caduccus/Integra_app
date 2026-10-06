package com.example.plataformaremota

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.plataformaremota.adapter.ChatAdapter
import com.example.plataformaremota.adapter.SkeletonAdapter
import com.example.plataformaremota.data.entity.Chat
import com.example.plataformaremota.data.repository.ChatRepository
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class ChatListFragment : Fragment() {

    private lateinit var rvChats: RecyclerView
    private lateinit var layoutEstadoVazio: View
    private lateinit var layoutLoading: View
    private lateinit var btnNovoGrupo: MaterialButton
    private lateinit var btnCriarGrupoVazio: MaterialButton

    private lateinit var adapter: ChatAdapter
    private lateinit var skeletonAdapter: SkeletonAdapter
    private lateinit var repository: ChatRepository

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var nomesUsuarios: Map<String, String> = emptyMap()
    private var fotosUsuarios: Map<String, String> = emptyMap()
    private var titulosTrabalhos: Map<String, String> = emptyMap()
    private var statusUsuarios: Map<String, String> = emptyMap()

    private val skeletonHandler = Handler(Looper.getMainLooper())
    private var skeletonRunnable: Runnable? = null

    private val TAG = "CHAT_LIST"

    private val novoGrupoLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            carregarChats()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_chat_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvChats = view.findViewById(R.id.rvChats)
        layoutEstadoVazio = view.findViewById(R.id.layoutEstadoVazio)
        layoutLoading = view.findViewById(R.id.layoutLoading)
        btnNovoGrupo = view.findViewById(R.id.btnNovoGrupo)
        btnCriarGrupoVazio = view.findViewById(R.id.btnCriarGrupoVazio)

        repository = ChatRepository()

        configurarRecyclerView()

        btnNovoGrupo.setOnClickListener {
            val intent = Intent(requireContext(), NovoGrupoActivity::class.java)
            novoGrupoLauncher.launch(intent)
        }

        btnCriarGrupoVazio.setOnClickListener {
            val intent = Intent(requireContext(), NovoGrupoActivity::class.java)
            novoGrupoLauncher.launch(intent)
        }

        carregarChats()

        ThemeManager.aplicarCores(requireContext(), view)
        ThemeManager.aplicarCoresTexto(requireContext(), view)
    }

    private fun configurarRecyclerView() {
        adapter = ChatAdapter(
            chats = emptyList(),
            nomeDoChat = { chat -> nomeDoChat(chat) },
            fotoDoChat = { chat -> fotoDoChat(chat) },
            contextoDoChat = { chat -> contextoDoChat(chat) },
            statusDoChat = { chat -> statusDoChat(chat) },
            onClick = { chat ->
                val intent = Intent(requireContext(), ChatActivity::class.java)
                intent.putExtra("chatId", chat.id)
                startActivity(intent)
            },
            onProfileClick = { chat -> abrirPerfilOuGrupo(chat) },
            onLongClick = { chat -> abrirMenuConversa(chat) }
        )

        skeletonAdapter = SkeletonAdapter(R.layout.item_skeleton_chat, 6)

        rvChats.layoutManager = LinearLayoutManager(requireContext())
        rvChats.adapter = adapter
    }

    private fun abrirMenuConversa(chat: Chat) {
        val opcoes = mutableListOf<String>()
        if (!chat.ehGrupo) opcoes.add("Excluir conversa")

        if (opcoes.isEmpty()) {
            Toast.makeText(requireContext(), "Segure uma conversa pessoal para excluir", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(requireContext())
            .setTitle(nomeDoChat(chat))
            .setItems(opcoes.toTypedArray()) { _, which ->
                when (opcoes[which]) {
                    "Excluir conversa" -> confirmarExcluir(chat)
                }
            }
            .show()
    }

    private fun confirmarExcluir(chat: Chat) {
        AlertDialog.Builder(requireContext())
            .setTitle("Excluir conversa")
            .setMessage(
                "A conversa com ${nomeDoChat(chat)} será removida da sua lista.\n\n" +
                        "O outro participante ainda continuará vendo o histórico. " +
                        "Se qualquer um de vocês mandar uma nova mensagem, a conversa volta a aparecer."
            )
            .setPositiveButton("Excluir") { _, _ ->
                lifecycleScope.launch {
                    val ok = repository.excluirConversa(chat.id)
                    if (ok) {
                        Toast.makeText(requireContext(), "Conversa excluída", Toast.LENGTH_SHORT).show()
                        carregarChats()
                    } else {
                        Toast.makeText(requireContext(), "Erro ao excluir", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun abrirPerfilOuGrupo(chat: Chat) {
        if (chat.ehGrupo) {
            val intent = Intent(requireContext(), InfoGrupoActivity::class.java)
            intent.putExtra("chatId", chat.id)
            startActivity(intent)
        } else {
            val uidAtual = auth.currentUser?.uid
            if (uidAtual != null) {
                val outroUid = chat.participantes.firstOrNull { it != uidAtual }
                if (outroUid != null) {
                    val intent = Intent(requireContext(), PerfilUsuarioActivity::class.java)
                    intent.putExtra("uidUsuario", outroUid)
                    intent.putExtra("chatId", "")
                    startActivity(intent)
                }
            }
        }
    }

    private fun nomeDoChat(chat: Chat): String {
        if (chat.ehGrupo) return chat.nome.ifEmpty { "Grupo" }
        val uidAtual = auth.currentUser?.uid ?: return "Chat"
        val outroUid = chat.participantes.firstOrNull { it != uidAtual } ?: return "Chat"
        return nomesUsuarios[outroUid] ?: "Usuário"
    }

    private fun fotoDoChat(chat: Chat): String {
        if (chat.ehGrupo) return chat.fotoUrl
        val uidAtual = auth.currentUser?.uid ?: return ""
        val outroUid = chat.participantes.firstOrNull { it != uidAtual } ?: return ""
        return fotosUsuarios[outroUid] ?: ""
    }

    private fun contextoDoChat(chat: Chat): String {
        if (chat.trabalhoId.isEmpty()) return ""
        val titulo = titulosTrabalhos[chat.trabalhoId] ?: return ""
        return "Sobre: $titulo"
    }

    private fun statusDoChat(chat: Chat): String {
        if (chat.ehGrupo) return ThemeManager.STATUS_ONLINE
        val uidAtual = auth.currentUser?.uid ?: return ThemeManager.STATUS_ONLINE
        val outroUid = chat.participantes.firstOrNull { it != uidAtual }
            ?: return ThemeManager.STATUS_ONLINE
        return statusUsuarios[outroUid] ?: ThemeManager.STATUS_ONLINE
    }

    // ⭐ Carregamento com timeout TOTAL e logs
    private fun carregarChats() {
        mostrarLoading()

        lifecycleScope.launch {
            try {
                val uid = auth.currentUser?.uid
                if (uid == null) {
                    mostrarEstadoVazio()
                    return@launch
                }

                Log.d(TAG, "🔄 Iniciando carregamento...")

                val sucesso = withTimeoutOrNull(8000L) {
                    try {
                        executarCarregamento(uid)
                        true
                    } catch (e: Throwable) {
                        Log.e(TAG, "❌ Erro interno: ${e.message}", e)
                        false
                    }
                }

                if (sucesso == null) {
                    Log.w(TAG, "⏱ Timeout ao carregar chats")
                    Toast.makeText(requireContext(), "Tempo esgotado — tente novamente", Toast.LENGTH_SHORT).show()
                    mostrarEstadoVazio()
                }
            } catch (e: Throwable) {
                Log.e(TAG, "❌ Erro geral: ${e.message}", e)
                mostrarEstadoVazio()
            }
        }
    }

    private suspend fun executarCarregamento(uid: String) = coroutineScope {
        // 1) Busca os chats
        val snapshot = db.collection("chats")
            .whereArrayContains("participantes", uid)
            .get()
            .await()

        val chats = snapshot.documents
            .mapNotNull { it.toObject(Chat::class.java) }
            .filter { uid !in it.deletadosPara }
            .sortedByDescending { it.timestamp }

        Log.d(TAG, "✅ ${chats.size} chats encontrados")

        val uidsOutros = chats
            .filter { !it.ehGrupo }
            .flatMap { it.participantes }
            .filter { it != uid }
            .distinct()

        val idsTrabalhos = chats
            .map { it.trabalhoId }
            .filter { it.isNotEmpty() }
            .distinct()

        // 2) Carrega dados auxiliares EM PARALELO, cada um com try/catch interno
        val jobU = async { carregarUsuarios(uidsOutros) }
        val jobT = async { carregarTitulos(idsTrabalhos) }

        try { jobU.await() } catch (e: Throwable) { Log.e(TAG, "Erro usuários: ${e.message}") }
        try { jobT.await() } catch (e: Throwable) { Log.e(TAG, "Erro títulos: ${e.message}") }

        Log.d(TAG, "✅ Dados auxiliares OK")

        // 3) Atualiza a UI — sempre, mesmo se algo falhou em cima
        withContext(Dispatchers.Main) {
            adapter.atualizarLista(chats)
            if (chats.isEmpty()) mostrarEstadoVazio() else mostrarLista()
        }
    }

    private suspend fun carregarUsuarios(uids: List<String>) = coroutineScope {
        if (uids.isEmpty()) return@coroutineScope

        val resultados = uids.map { uid ->
            async {
                try {
                    val doc = db.collection("usuarios").document(uid).get().await()
                    uid to Triple(
                        doc.getString("nome") ?: "Usuário",
                        doc.getString("fotoUrl") ?: "",
                        doc.getString("status") ?: ThemeManager.STATUS_ONLINE
                    )
                } catch (e: Throwable) {
                    uid to Triple("Usuário", "", ThemeManager.STATUS_ONLINE)
                }
            }
        }.awaitAll()

        val mapaNomes = mutableMapOf<String, String>()
        val mapaFotos = mutableMapOf<String, String>()
        val mapaStatus = mutableMapOf<String, String>()
        resultados.forEach { (uid, t) ->
            mapaNomes[uid] = t.first
            mapaFotos[uid] = t.second
            mapaStatus[uid] = t.third
        }
        nomesUsuarios = mapaNomes
        fotosUsuarios = mapaFotos
        statusUsuarios = mapaStatus
    }

    private suspend fun carregarTitulos(ids: List<String>) = coroutineScope {
        if (ids.isEmpty()) return@coroutineScope

        val resultados = ids.map { id ->
            async {
                try {
                    val doc = db.collection("trabalhos").document(id).get().await()
                    id to (doc.getString("titulo") ?: "")
                } catch (e: Throwable) {
                    id to ""
                }
            }
        }.awaitAll()

        titulosTrabalhos = resultados.toMap()
    }

    private fun mostrarLoading() {
        rvChats.visibility = View.GONE
        layoutEstadoVazio.visibility = View.GONE
        layoutLoading.visibility = View.GONE

        // Cancela runnable anterior, se houver
        skeletonRunnable?.let { skeletonHandler.removeCallbacks(it) }

        skeletonRunnable = Runnable {
            if (!isAdded) return@Runnable
            rvChats.adapter = skeletonAdapter
            rvChats.visibility = View.VISIBLE
        }
        skeletonHandler.postDelayed(skeletonRunnable!!, 350)
    }

    private fun cancelarSkeleton() {
        skeletonRunnable?.let { skeletonHandler.removeCallbacks(it) }
        skeletonRunnable = null
    }

    private fun mostrarLista() {
        cancelarSkeleton()
        layoutLoading.visibility = View.GONE
        layoutEstadoVazio.visibility = View.GONE
        rvChats.adapter = adapter
        rvChats.visibility = View.VISIBLE
    }

    private fun mostrarEstadoVazio() {
        cancelarSkeleton()
        layoutLoading.visibility = View.GONE
        rvChats.visibility = View.GONE
        layoutEstadoVazio.visibility = View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        if (::adapter.isInitialized) carregarChats()
    }

    override fun onDestroyView() {
        cancelarSkeleton()
        super.onDestroyView()
    }
}