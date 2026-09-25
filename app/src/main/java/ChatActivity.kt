package com.example.plataformaremota

import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.plataformaremota.adapter.MensagemAdapter
import com.example.plataformaremota.data.entity.Mensagem
import com.example.plataformaremota.data.repository.ChatRepository
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ChatActivity : BaseActivity() {

    private lateinit var btnVoltar: ImageView
    private lateinit var btnInfo: ImageView
    private lateinit var txtNomeChat: TextView
    private lateinit var rvMensagens: RecyclerView
    private lateinit var edtMensagem: EditText
    private lateinit var btnEnviar: MaterialButton

    private lateinit var adapter: MensagemAdapter
    private lateinit var repository: ChatRepository

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var chatId: String = ""
    private var listenerRegistration: ListenerRegistration? = null

    private var participantesAtuais: List<String> = emptyList()
    private var ehGrupoAtual: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        repository = ChatRepository()

        btnVoltar = findViewById(R.id.btnVoltarChat)
        btnInfo = findViewById(R.id.btnInfoChat)
        txtNomeChat = findViewById(R.id.txtNomeChat)
        rvMensagens = findViewById(R.id.rvMensagens)
        edtMensagem = findViewById(R.id.edtMensagem)
        btnEnviar = findViewById(R.id.btnEnviar)

        chatId = intent.getStringExtra("chatId") ?: ""

        if (chatId.isEmpty()) {
            Toast.makeText(this, "Chat não encontrado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        configurarRecyclerView()
        carregarNomeChat()
        ouvirMensagens()

        btnVoltar.setOnClickListener { finish() }
        btnEnviar.setOnClickListener { enviarMensagem() }
        btnInfo.setOnClickListener {
            Toast.makeText(this, "Info em breve", Toast.LENGTH_SHORT).show()
        }
    }

    private fun configurarRecyclerView() {
        adapter = MensagemAdapter(emptyList())
        rvMensagens.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        rvMensagens.adapter = adapter
    }

    private fun carregarNomeChat() {
        lifecycleScope.launch {
            val chat = repository.buscarChat(chatId) ?: return@launch
            val uidAtual = auth.currentUser?.uid ?: return@launch

            participantesAtuais = chat.participantes
            ehGrupoAtual = chat.ehGrupo

            if (chat.ehGrupo) {
                txtNomeChat.text = chat.nome.ifEmpty { "Grupo" }
            } else {
                val outroUid = chat.participantes.firstOrNull { it != uidAtual }
                if (outroUid != null) {
                    try {
                        val doc = db.collection("usuarios").document(outroUid).get().await()
                        txtNomeChat.text = doc.getString("nome") ?: "Usuário"
                    } catch (e: Exception) {
                        txtNomeChat.text = "Usuário"
                    }
                }
            }
        }
    }

    private fun ouvirMensagens() {
        listenerRegistration = db.collection("chats")
            .document(chatId)
            .collection("mensagens")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener

                val lista = snapshot?.documents?.mapNotNull {
                    it.toObject(Mensagem::class.java)
                } ?: emptyList()

                adapter.atualizarLista(lista)
                if (lista.isNotEmpty()) rvMensagens.scrollToPosition(lista.size - 1)
            }
    }

    private fun enviarMensagem() {
        val texto = edtMensagem.text.toString().trim()
        if (texto.isEmpty()) return

        edtMensagem.setText("")
        btnEnviar.isEnabled = false

        lifecycleScope.launch {
            val sucesso = repository.enviarMensagem(chatId, texto)
            btnEnviar.isEnabled = true

            if (sucesso) {
                // ⭐ Envia notificação pros participantes
                notificarParticipantes(texto)
            } else {
                Toast.makeText(this@ChatActivity, "Erro ao enviar", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ⭐ Notifica todos os participantes exceto eu
    private fun notificarParticipantes(texto: String) {
        val uidAtual = auth.currentUser?.uid ?: return
        val destinatarios = participantesAtuais.filter { it != uidAtual }

        if (destinatarios.isEmpty()) return

        lifecycleScope.launch {
            try {
                // Pega o nome de quem mandou
                val docMeu = db.collection("usuarios").document(uidAtual).get().await()
                val meuNome = docMeu.getString("nome") ?: "Usuário"

                val titulo = if (ehGrupoAtual) "Nova mensagem no grupo" else "Nova mensagem"
                val mensagem = "$meuNome: $texto"

                NotificacaoHelper.enviarParaVarios(destinatarios, titulo, mensagem)
            } catch (e: Exception) {
                // Ignora erro
            }
        }
    }

    override fun onDestroy() {
        listenerRegistration?.remove()
        super.onDestroy()
    }
}