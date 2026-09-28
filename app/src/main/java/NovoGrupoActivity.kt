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

class NovoGrupoActivity : BaseActivity() {

    private lateinit var btnVoltar: ImageView
    private lateinit var btnCriar: MaterialButton
    private lateinit var edtNomeGrupo: EditText
    private lateinit var edtBusca: EditText
    private lateinit var txtSelecionados: TextView
    private lateinit var rvUsuarios: RecyclerView
    private lateinit var layoutLoading: View
    private lateinit var layoutVazio: View

    private lateinit var adapter: SelecionarUsuarioAdapter
    private lateinit var repository: ChatRepository

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var todosUsuarios: List<UsuarioSelecionavel> = emptyList()
    private var usuariosFiltrados: List<UsuarioSelecionavel> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_novo_grupo)

        repository = ChatRepository()

        btnVoltar = findViewById(R.id.btnVoltarNovoGrupo)
        btnCriar = findViewById(R.id.btnCriarGrupo)
        edtNomeGrupo = findViewById(R.id.edtNomeGrupo)
        edtBusca = findViewById(R.id.edtBusca)
        txtSelecionados = findViewById(R.id.txtSelecionados)
        rvUsuarios = findViewById(R.id.rvUsuarios)
        layoutLoading = findViewById(R.id.layoutLoadingUsuarios)
        layoutVazio = findViewById(R.id.layoutVazioUsuarios)

        configurarRecyclerView()
        configurarBusca()

        btnVoltar.setOnClickListener { finish() }
        btnCriar.setOnClickListener { criarGrupo() }

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
                val snapshot = db.collection("usuarios").get().await()

                todosUsuarios = snapshot.documents.mapNotNull { doc ->
                    val uid = doc.id
                    if (uid == meuUid) return@mapNotNull null  // Exclui eu mesmo
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
                Toast.makeText(this@NovoGrupoActivity, "Erro: ${e.message}", Toast.LENGTH_LONG).show()
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

    private fun criarGrupo() {
        val nomeGrupo = edtNomeGrupo.text.toString().trim()
        val selecionados = todosUsuarios.filter { it.selecionado }.map { it.uid }

        if (nomeGrupo.isEmpty()) {
            Toast.makeText(this, "Digite um nome para o grupo", Toast.LENGTH_SHORT).show()
            return
        }

        if (selecionados.isEmpty()) {
            Toast.makeText(this, "Selecione pelo menos 1 pessoa", Toast.LENGTH_SHORT).show()
            return
        }

        btnCriar.isEnabled = false
        btnCriar.text = "CRIANDO..."

        lifecycleScope.launch {
            val chatId = repository.criarGrupo(nomeGrupo, selecionados)

            if (chatId != null) {
                Toast.makeText(this@NovoGrupoActivity, "Grupo criado!", Toast.LENGTH_SHORT).show()
                setResult(RESULT_OK)
                finish()
            } else {
                btnCriar.isEnabled = true
                btnCriar.text = "CRIAR"
                Toast.makeText(this@NovoGrupoActivity, "Erro ao criar grupo", Toast.LENGTH_SHORT).show()
            }
        }
    }
}