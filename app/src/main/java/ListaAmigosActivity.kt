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
import com.example.plataformaremota.data.repository.ChatRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ListaAmigosActivity : BaseActivity() {

    private lateinit var rvAmigos: RecyclerView
    private lateinit var edtBusca: EditText
    private lateinit var layoutLoading: View
    private lateinit var txtVazio: View

    private lateinit var adapter: AmigoAdapter
    private lateinit var chatRepository: ChatRepository

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var todosAmigos: List<AmigoItem> = emptyList()
    private var amigosFiltrados: List<AmigoItem> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lista_amigos)

        chatRepository = ChatRepository()

        rvAmigos = findViewById(R.id.rvAmigos)
        edtBusca = findViewById(R.id.edtBuscaAmigos)
        layoutLoading = findViewById(R.id.layoutLoadingAmigos)
        txtVazio = findViewById(R.id.txtVazioAmigos)

        findViewById<ImageView>(R.id.btnVoltarAmigos).setOnClickListener { finish() }

        configurarRecyclerView()
        configurarBusca()
        carregarAmigos()
    }

    private fun configurarRecyclerView() {
        adapter = AmigoAdapter(emptyList()) { amigo ->
            abrirChatComAmigo(amigo)
        }
        rvAmigos.layoutManager = LinearLayoutManager(this)
        rvAmigos.adapter = adapter
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

    private fun carregarAmigos() {
        val meuUid = auth.currentUser?.uid ?: return

        lifecycleScope.launch {
            try {
                val meuDoc = db.collection("usuarios").document(meuUid).get().await()
                val listaUids = (meuDoc.get("amigos") as? List<*>)?.filterIsInstance<String>() ?: emptyList()

                if (listaUids.isEmpty()) {
                    mostrarVazio()
                    return@launch
                }

                // Busca os dados de cada amigo em paralelo
                val amigos = coroutineScope {
                    listaUids.map { uid ->
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
                }.sortedBy { it.nome.lowercase() }

                todosAmigos = amigos
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