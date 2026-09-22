package com.example.plataformaremota

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.plataformaremota.adapter.ChatAdapter
import com.example.plataformaremota.data.entity.Chat
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ChatListFragment : Fragment() {

    private lateinit var rvChats: RecyclerView
    private lateinit var layoutEstadoVazio: View
    private lateinit var layoutLoading: View
    private lateinit var btnNovoGrupo: MaterialButton

    private lateinit var adapter: ChatAdapter

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var nomesUsuarios: Map<String, String> = emptyMap()
    private var fotosUsuarios: Map<String, String> = emptyMap()
    private var titulosTrabalhos: Map<String, String> = emptyMap()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_chat_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvChats = view.findViewById(R.id.rvChats)
        layoutEstadoVazio = view.findViewById(R.id.layoutEstadoVazio)
        layoutLoading = view.findViewById(R.id.layoutLoading)
        btnNovoGrupo = view.findViewById(R.id.btnNovoGrupo)

        configurarRecyclerView()

        btnNovoGrupo.setOnClickListener {
            Toast.makeText(requireContext(), "Grupos em breve", Toast.LENGTH_SHORT).show()
        }

        carregarChats()
    }

    private fun configurarRecyclerView() {
        adapter = ChatAdapter(
            chats = emptyList(),
            nomeDoChat = { chat -> nomeDoChat(chat) },
            fotoDoChat = { chat -> fotoDoChat(chat) },
            contextoDoChat = { chat -> contextoDoChat(chat) },
            onClick = { chat ->
                val intent = Intent(requireContext(), ChatActivity::class.java)
                intent.putExtra("chatId", chat.id)
                startActivity(intent)
            }
        )

        rvChats.layoutManager = LinearLayoutManager(requireContext())
        rvChats.adapter = adapter
    }

    private fun nomeDoChat(chat: Chat): String {
        if (chat.ehGrupo) return chat.nome.ifEmpty { "Grupo" }
        val uidAtual = auth.currentUser?.uid ?: return "Chat"
        val outroUid = chat.participantes.firstOrNull { it != uidAtual } ?: return "Chat"
        return nomesUsuarios[outroUid] ?: "Usuário"
    }

    private fun fotoDoChat(chat: Chat): String {
        if (chat.ehGrupo) return ""
        val uidAtual = auth.currentUser?.uid ?: return ""
        val outroUid = chat.participantes.firstOrNull { it != uidAtual } ?: return ""
        return fotosUsuarios[outroUid] ?: ""
    }

    private fun contextoDoChat(chat: Chat): String {
        if (chat.trabalhoId.isEmpty()) return ""
        val titulo = titulosTrabalhos[chat.trabalhoId] ?: return ""
        return "Sobre: $titulo"
    }

    private fun carregarChats() {
        mostrarLoading()

        lifecycleScope.launch {
            try {
                val uid = auth.currentUser?.uid ?: run {
                    mostrarEstadoVazio()
                    return@launch
                }

                val snapshot = db.collection("chats")
                    .whereArrayContains("participantes", uid)
                    .get()
                    .await()

                val chats = snapshot.documents.mapNotNull {
                    it.toObject(Chat::class.java)
                }.sortedByDescending { it.timestamp }

                val uidsOutros = chats
                    .filter { !it.ehGrupo }
                    .flatMap { it.participantes }
                    .filter { it != uid }
                    .distinct()

                val idsTrabalhos = chats
                    .map { it.trabalhoId }
                    .filter { it.isNotEmpty() }
                    .distinct()

                buscarDadosUsuarios(uidsOutros)
                buscarTitulosTrabalhos(idsTrabalhos)

                adapter.atualizarLista(chats)

                if (chats.isEmpty()) mostrarEstadoVazio() else mostrarLista()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Erro: ${e.message}", Toast.LENGTH_SHORT).show()
                mostrarEstadoVazio()
            }
        }
    }

    private suspend fun buscarDadosUsuarios(uids: List<String>) {
        val mapaNomes = mutableMapOf<String, String>()
        val mapaFotos = mutableMapOf<String, String>()

        for (uid in uids) {
            try {
                val doc = db.collection("usuarios").document(uid).get().await()
                mapaNomes[uid] = doc.getString("nome") ?: "Usuário"
                mapaFotos[uid] = doc.getString("fotoUrl") ?: ""
            } catch (_: Exception) { }
        }

        nomesUsuarios = mapaNomes
        fotosUsuarios = mapaFotos
    }

    private suspend fun buscarTitulosTrabalhos(ids: List<String>) {
        val mapa = mutableMapOf<String, String>()

        for (id in ids) {
            try {
                val doc = db.collection("trabalhos").document(id).get().await()
                mapa[id] = doc.getString("titulo") ?: ""
            } catch (_: Exception) { }
        }

        titulosTrabalhos = mapa
    }

    private fun mostrarLoading() {
        layoutLoading.visibility = View.VISIBLE
        rvChats.visibility = View.GONE
        layoutEstadoVazio.visibility = View.GONE
    }

    private fun mostrarLista() {
        layoutLoading.visibility = View.GONE
        rvChats.visibility = View.VISIBLE
        layoutEstadoVazio.visibility = View.GONE
    }

    private fun mostrarEstadoVazio() {
        layoutLoading.visibility = View.GONE
        rvChats.visibility = View.GONE
        layoutEstadoVazio.visibility = View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        if (::adapter.isInitialized) {
            carregarChats()
        }
    }
}