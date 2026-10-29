package com.example.plataformaremota

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.plataformaremota.adapter.UsuarioChat
import com.example.plataformaremota.adapter.UsuarioChatAdapter
import com.example.plataformaremota.data.repository.ChatRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class NovoChatActivity : BaseActivity() {

    private lateinit var rvUsuarios: RecyclerView
    private lateinit var edtBusca: EditText
    private lateinit var layoutLoading: View
    private lateinit var txtVazio: View

    private lateinit var adapter: UsuarioChatAdapter
    private lateinit var repository: ChatRepository

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var todosUsuarios: List<UsuarioChat> = emptyList()
    private var usuariosFiltrados: List<UsuarioChat> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_novo_chat)

        repository = ChatRepository()

        rvUsuarios = findViewById(R.id.rvUsuariosChat)
        edtBusca = findViewById(R.id.edtBuscaChat)
        layoutLoading = findViewById(R.id.layoutLoadingChat)
        txtVazio = findViewById(R.id.txtVazioChat)

        findViewById<ImageView>(R.id.btnVoltarNovoChat).setOnClickListener { finish() }

        configurarRecyclerView()
        configurarBusca()
        carregarUsuarios()
    }

    private fun configurarRecyclerView() {
        adapter = UsuarioChatAdapter(emptyList()) { usuario ->
            iniciarChat(usuario)
        }
        rvUsuarios.layoutManager = LinearLayoutManager(this)
        rvUsuarios.adapter = adapter
    }

    private fun configurarBusca() {
        edtBusca.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filtrarUsuarios(s?.toString()?.trim() ?: "")
            }
        })
    }

    private fun carregarUsuarios() {
        val meuUid = auth.currentUser?.uid ?: return

        lifecycleScope.launch {
            try {
                val snapshot = db.collection("usuarios").get().await()

                todosUsuarios = snapshot.documents.mapNotNull { doc ->
                    val uid = doc.id
                    if (uid == meuUid) return@mapNotNull null // Não mostrar a si mesmo

                    UsuarioChat(
                        uid = uid,
                        nome = doc.getString("nome") ?: "Usuário",
                        username = doc.getString("username") ?: "",
                        fotoUrl = doc.getString("fotoUrl") ?: ""
                    )
                }.sortedBy { it.nome.lowercase() }

                usuariosFiltrados = todosUsuarios
                adapter.atualizarLista(usuariosFiltrados)

                layoutLoading.visibility = View.GONE
                if (usuariosFiltrados.isEmpty()) {
                    txtVazio.visibility = View.VISIBLE
                    rvUsuarios.visibility = View.GONE
                } else {
                    txtVazio.visibility = View.GONE
                    rvUsuarios.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                Toast.makeText(this@NovoChatActivity, "Erro: ${e.message}", Toast.LENGTH_SHORT).show()
                layoutLoading.visibility = View.GONE
            }
        }
    }

    private fun filtrarUsuarios(termo: String) {
        usuariosFiltrados = if (termo.isEmpty()) {
            todosUsuarios
        } else {
            val t = termo.lowercase()
            todosUsuarios.filter {
                it.nome.lowercase().contains(t) || it.username.lowercase().contains(t)
            }
        }
        adapter.atualizarLista(usuariosFiltrados)

        if (usuariosFiltrados.isEmpty()) {
            txtVazio.visibility = View.VISIBLE
            rvUsuarios.visibility = View.GONE
        } else {
            txtVazio.visibility = View.GONE
            rvUsuarios.visibility = View.VISIBLE
        }
    }

    private fun iniciarChat(usuario: UsuarioChat) {
        lifecycleScope.launch {
            // A função criarOuBuscarChatUmParaUm já existe no seu ChatRepository
            val chatId = repository.criarOuBuscarChatUmParaUm(
                uidOutro = usuario.uid,
                nomeOutro = usuario.nome,
                trabalhoId = "" // Chat direto, sem contexto de trabalho
            )

            if (chatId != null) {
                val intent = Intent(this@NovoChatActivity, ChatActivity::class.java)
                intent.putExtra("chatId", chatId)
                startActivity(intent)
                finish() // Fecha a tela de busca e volta direto pro chat
            } else {
                Toast.makeText(this@NovoChatActivity, "Erro ao abrir chat", Toast.LENGTH_SHORT).show()
            }
        }
    }
}