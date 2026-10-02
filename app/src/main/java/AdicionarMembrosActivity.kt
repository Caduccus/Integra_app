package com.example.plataformaremota

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.plataformaremota.adapter.SelecionarUsuarioAdapter
import com.example.plataformaremota.adapter.UsuarioSelecionavel
import com.example.plataformaremota.data.repository.ChatRepository
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AdicionarMembrosActivity : BaseActivity() {

    private lateinit var btnVoltar: ImageView
    private lateinit var btnConfirmar: MaterialButton
    private lateinit var edtBusca: EditText
    private lateinit var txtSelecionados: TextView
    private lateinit var rvUsuarios: RecyclerView
    private lateinit var layoutLoading: View
    private lateinit var layoutVazio: View

    private lateinit var adapter: SelecionarUsuarioAdapter
    private lateinit var repository: ChatRepository

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var chatId: String = ""
    private var participantesAtuais: List<String> = emptyList()

    private var todosUsuarios: List<UsuarioSelecionavel> = emptyList()
    private var usuariosFiltrados: List<UsuarioSelecionavel> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_adicionar_membros)

        repository = ChatRepository()

        btnVoltar = findViewById(R.id.btnVoltarAddMembros)
        btnConfirmar = findViewById(R.id.btnConfirmarAdd)
        edtBusca = findViewById(R.id.edtBuscaAdd)
        txtSelecionados = findViewById(R.id.txtSelecionadosAdd)
        rvUsuarios = findViewById(R.id.rvUsuariosAdd)
        layoutLoading = findViewById(R.id.layoutLoadingAdd)
        layoutVazio = findViewById(R.id.layoutVazioAdd)

        chatId = intent.getStringExtra("chatId") ?: ""

        if (chatId.isEmpty()) {
            Toast.makeText(this, "Erro ao abrir", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // ⭐ Bloqueia acesso se for grupo de empresa
        lifecycleScope.launch {
            try {
                val doc = db.collection("chats").document(chatId).get().await()
                val empresaId = doc.getString("empresaId") ?: ""
                if (empresaId.isNotEmpty()) {
                    Toast.makeText(
                        this@AdicionarMembrosActivity,
                        "Este grupo é privado da empresa. Só quem entra pela empresa pode participar.",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                }
            } catch (_: Exception) { }
        }

        configurarRecyclerView()
        configurarBusca()

        btnVoltar.setOnClickListener { finish() }
        btnConfirmar.setOnClickListener { adicionarSelecionados() }

        carregarUsuarios()
    }

    private fun configurarRecyclerView() {
        adapter = SelecionarUsuarioAdapter(emptyList()) { _ ->
            atualizarContador()
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
                val docChat = db.collection("chats").document(chatId).get().await()
                participantesAtuais = (docChat.get("participantes") as? List<*>)?.filterIsInstance<String>() ?: emptyList()

                val snapshot = db.collection("usuarios").get().await()

                todosUsuarios = snapshot.documents.mapNotNull { doc ->
                    val uid = doc.id
                    if (uid == meuUid || uid in participantesAtuais) return@mapNotNull null

                    UsuarioSelecionavel(
                        uid = uid,
                        nome = doc.getString("nome") ?: "Usuário",
                        username = doc.getString("username") ?: "",
                        fotoUrl = doc.getString("fotoUrl") ?: ""
                    )
                }.sortedBy { it.nome.lowercase() }

                usuariosFiltrados = todosUsuarios
                adapter.atualizarLista(usuariosFiltrados)

                if (usuariosFiltrados.isEmpty()) {
                    layoutVazio.visibility = View.VISIBLE
                    rvUsuarios.visibility = View.GONE
                } else {
                    layoutVazio.visibility = View.GONE
                    rvUsuarios.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                Toast.makeText(this@AdicionarMembrosActivity, "Erro: ${e.message}", Toast.LENGTH_LONG).show()
            }
            layoutLoading.visibility = View.GONE
        }
    }

    private fun filtrarUsuarios(termo: String) {
        usuariosFiltrados = if (termo.isEmpty()) {
            todosUsuarios
        } else {
            val t = termo.lowercase()
            todosUsuarios.filter {
                it.nome.lowercase().contains(t) ||
                        it.username.lowercase().contains(t)
            }
        }
        adapter.atualizarLista(usuariosFiltrados)

        if (usuariosFiltrados.isEmpty()) {
            layoutVazio.visibility = View.VISIBLE
            rvUsuarios.visibility = View.GONE
        } else {
            layoutVazio.visibility = View.GONE
            rvUsuarios.visibility = View.VISIBLE
        }
    }

    private fun atualizarContador() {
        val qtd = todosUsuarios.count { it.selecionado }
        txtSelecionados.text = "Selecionados: $qtd"
    }

    private fun adicionarSelecionados() {
        val selecionados = todosUsuarios.filter { it.selecionado }
        val uids = selecionados.map { it.uid }

        if (uids.isEmpty()) {
            Toast.makeText(this, "Selecione pelo menos 1 pessoa", Toast.LENGTH_SHORT).show()
            return
        }

        btnConfirmar.isEnabled = false
        btnConfirmar.text = "ADICIONANDO..."

        lifecycleScope.launch {
            val ok = repository.adicionarMembros(chatId, uids)

            if (ok) {
                val nomes = selecionados.map { it.nome }.joinToString(", ")
                notificarNovosMembros(uids, nomes)

                Toast.makeText(this@AdicionarMembrosActivity, "Membros adicionados!", Toast.LENGTH_SHORT).show()
                setResult(RESULT_OK)
                finish()
            } else {
                btnConfirmar.isEnabled = true
                btnConfirmar.text = "ADICIONAR"
                Toast.makeText(this@AdicionarMembrosActivity, "Erro ao adicionar", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun notificarNovosMembros(uids: List<String>, nomes: String) {
        lifecycleScope.launch {
            try {
                val docChat = db.collection("chats").document(chatId).get().await()
                val nomeGrupo = docChat.getString("nome") ?: "um grupo"

                val uidAtual = auth.currentUser?.uid ?: return@launch
                val docMeu = db.collection("usuarios").document(uidAtual).get().await()
                val meuNome = docMeu.getString("nome") ?: "Usuário"

                NotificacaoHelper.enviarParaVarios(
                    uidsDestino = uids,
                    titulo = "Adicionado a um grupo",
                    mensagem = "$meuNome te adicionou ao grupo '$nomeGrupo'",
                    chatId = chatId
                )
            } catch (_: Exception) { }
        }
    }
}