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
import com.example.plataformaremota.adapter.AmigoAdapter
import com.example.plataformaremota.adapter.AmigoItem
import com.example.plataformaremota.adapter.SolicitacaoAmigoAdapter
import com.example.plataformaremota.adapter.SolicitacaoItem
import com.example.plataformaremota.data.repository.ChatRepository
import com.example.plataformaremota.data.repository.UsuarioRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ListaAmigosActivity : BaseActivity() {

    private lateinit var rvAmigos: RecyclerView
    private lateinit var rvSolicitacoes: RecyclerView
    private lateinit var layoutSolicitacoes: View
    private lateinit var edtBusca: EditText
    private lateinit var layoutLoading: View
    private lateinit var txtVazio: View

    private lateinit var adapter: AmigoAdapter
    private lateinit var solicitacaoAdapter: SolicitacaoAmigoAdapter
    private lateinit var chatRepository: ChatRepository
    private lateinit var usuarioRepository: UsuarioRepository

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var todosAmigos: List<AmigoItem> = emptyList()
    private var amigosFiltrados: List<AmigoItem> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lista_amigos)

        chatRepository = ChatRepository()
        usuarioRepository = UsuarioRepository()

        rvAmigos = findViewById(R.id.rvAmigos)
        rvSolicitacoes = findViewById(R.id.rvSolicitacoes)
        layoutSolicitacoes = findViewById(R.id.layoutSolicitacoes)
        edtBusca = findViewById(R.id.edtBuscaAmigos)
        layoutLoading = findViewById(R.id.layoutLoadingAmigos)
        txtVazio = findViewById(R.id.txtVazioAmigos)

        findViewById<ImageView>(R.id.btnVoltarAmigos).setOnClickListener { finish() }

        configurarRecyclers()
        configurarBusca()
        carregarDados()
    }

    private fun configurarRecyclers() {
        adapter = AmigoAdapter(emptyList()) { amigo -> abrirChatComAmigo(amigo) }
        rvAmigos.layoutManager = LinearLayoutManager(this)
        rvAmigos.adapter = adapter

        solicitacaoAdapter = SolicitacaoAmigoAdapter(
            emptyList(),
            onAceitar = { solicitacao -> aceitarSolicitacao(solicitacao) },
            onRecusar = { solicitacao -> recusarSolicitacao(solicitacao) }
        )
        rvSolicitacoes.layoutManager = LinearLayoutManager(this)
        rvSolicitacoes.adapter = solicitacaoAdapter
    }

    private fun configurarBusca() {
        edtBusca.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filtrarAmigos(s?.toString()?.trim() ?: "")
            }
        })
    }

    private fun carregarDados() {
        val meuUid = auth.currentUser?.uid ?: return

        lifecycleScope.launch {
            try {
                val meuDoc = db.collection("usuarios").document(meuUid).get().await()
                val listaAmigosUids = (meuDoc.get("amigos") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                val listaSolicitacoesUids = (meuDoc.get("solicitacoesAmizade") as? List<*>)?.filterIsInstance<String>() ?: emptyList()

                // 1. Carrega Solicitações
                if (listaSolicitacoesUids.isNotEmpty()) {
                    layoutSolicitacoes.visibility = View.VISIBLE
                    val solicitacoes = buscarUsuarios(listaSolicitacoesUids).map {
                        SolicitacaoItem(it.uid, it.nome, it.username, it.fotoUrl)
                    }
                    solicitacaoAdapter.atualizarLista(solicitacoes)
                } else {
                    layoutSolicitacoes.visibility = View.GONE
                }

                // 2. Carrega Amigos
                if (listaAmigosUids.isEmpty()) {
                    mostrarVazio()
                    return@launch
                }

                todosAmigos = buscarUsuarios(listaAmigosUids).map {
                    AmigoItem(it.uid, it.nome, it.username, it.fotoUrl)
                }.sortedBy { it.nome.lowercase() }

                amigosFiltrados = todosAmigos
                adapter.atualizarLista(amigosFiltrados)

                layoutLoading.visibility = View.GONE
                if (amigosFiltrados.isEmpty()) mostrarVazio() else mostrarLista()

            } catch (e: Exception) {
                Toast.makeText(this@ListaAmigosActivity, "Erro: ${e.message}", Toast.LENGTH_SHORT).show()
                mostrarVazio()
            }
        }
    }

    // Função auxiliar para buscar dados de vários usuários no Firestore
    private suspend fun buscarUsuarios(uids: List<String>): List<AmigoItem> = coroutineScope {
        uids.map { uid ->
            async {
                try {
                    val doc = db.collection("usuarios").document(uid).get().await()
                    AmigoItem(
                        uid = uid,
                        nome = doc.getString("nome") ?: "Usuário",
                        username = doc.getString("username") ?: "",
                        fotoUrl = doc.getString("fotoUrl") ?: ""
                    )
                } catch (e: Exception) { null }
            }
        }.awaitAll().filterNotNull()
    }

    private fun aceitarSolicitacao(solicitacao: SolicitacaoItem) {
        lifecycleScope.launch {
            val ok = usuarioRepository.aceitarPedidoAmizade(solicitacao.uid)
            if (ok) {
                Toast.makeText(this@ListaAmigosActivity, "${solicitacao.nome} agora é seu amigo!", Toast.LENGTH_SHORT).show()
                carregarDados() // Recarrega as listas
            }
        }
    }

    private fun recusarSolicitacao(solicitacao: SolicitacaoItem) {
        lifecycleScope.launch {
            val ok = usuarioRepository.recusarPedidoAmizade(solicitacao.uid)
            if (ok) {
                carregarDados()
            }
        }
    }

    private fun filtrarAmigos(termo: String) {
        amigosFiltrados = if (termo.isEmpty()) {
            todosAmigos
        } else {
            val t = termo.lowercase()
            todosAmigos.filter {
                it.nome.lowercase().contains(t) || it.username.lowercase().contains(t)
            }
        }
        adapter.atualizarLista(amigosFiltrados)

        if (amigosFiltrados.isEmpty()) mostrarVazio() else mostrarLista()
    }

    private fun abrirChatComAmigo(amigo: AmigoItem) {
        lifecycleScope.launch {
            val chatId = chatRepository.criarOuBuscarChatUmParaUm(
                uidOutro = amigo.uid,
                nomeOutro = amigo.nome,
                trabalhoId = ""
            )
            if (chatId != null) {
                val intent = Intent(this@ListaAmigosActivity, ChatActivity::class.java)
                intent.putExtra("chatId", chatId)
                startActivity(intent)
            }
        }
    }

    private fun mostrarLista() {
        layoutLoading.visibility = View.GONE
        txtVazio.visibility = View.GONE
        rvAmigos.visibility = View.VISIBLE
    }

    private fun mostrarVazio() {
        layoutLoading.visibility = View.GONE
        rvAmigos.visibility = View.GONE
        txtVazio.visibility = View.VISIBLE
    }
}