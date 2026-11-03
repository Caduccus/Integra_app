package com.example.plataformaremota

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.plataformaremota.data.repository.ChatRepository
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class NovaConversaActivity : BaseActivity() {

    private lateinit var rvUsuarios: RecyclerView
    private lateinit var edtBusca: TextInputEditText
    private lateinit var layoutVazio: View

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var repository: ChatRepository

    private var todosUsuarios: List<UsuarioBusca> = emptyList()
    private lateinit var adapter: UsuarioBuscaAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nova_conversa)

        repository = ChatRepository()

        findViewById<ImageView>(R.id.btnVoltarNovaConversa).setOnClickListener { finish() }

        rvUsuarios = findViewById(R.id.rvUsuariosNovaConversa)
        edtBusca = findViewById(R.id.edtBuscaUsuario)
        layoutVazio = findViewById(R.id.layoutVazioUsuarios)

        configurarRecyclerView()
        configurarBusca()
        carregarUsuarios()
    }

    private fun configurarRecyclerView() {
        adapter = UsuarioBuscaAdapter(emptyList()) { usuario ->
            abrirChatComUsuario(usuario)
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
                    if (uid == meuUid) return@mapNotNull null // Não mostra a si mesmo

                    UsuarioBusca(
                        uid = uid,
                        nome = doc.getString("nome") ?: "Usuário",
                        username = doc.getString("username") ?: "",
                        fotoUrl = doc.getString("fotoUrl") ?: ""
                    )
                }.sortedBy { it.nome.lowercase() }

                adapter.atualizarLista(todosUsuarios)
            } catch (e: Exception) {
                Toast.makeText(this@NovaConversaActivity, "Erro ao carregar usuários", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun filtrarUsuarios(termo: String) {
        val filtrados = if (termo.isEmpty()) {
            todosUsuarios
        } else {
            todosUsuarios.filter {
                it.nome.lowercase().contains(termo.lowercase()) ||
                        it.username.lowercase().contains(termo.lowercase())
            }
        }

        adapter.atualizarLista(filtrados)
        layoutVazio.visibility = if (filtrados.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun abrirChatComUsuario(usuario: UsuarioBusca) {
        lifecycleScope.launch {
            // Cria ou busca um chat já existente entre vocês dois
            val chatId = repository.criarOuBuscarChatUmParaUm(
                uidOutro = usuario.uid,
                nomeOutro = usuario.nome
            )

            if (chatId != null) {
                val intent = Intent(this@NovaConversaActivity, ChatActivity::class.java)
                intent.putExtra("chatId", chatId)
                startActivity(intent)
                finish() // Fecha a tela de busca
            } else {
                Toast.makeText(this@NovaConversaActivity, "Erro ao iniciar conversa", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // --- Classes internas do Adapter ---
    data class UsuarioBusca(val uid: String, val nome: String, val username: String, val fotoUrl: String)

    class UsuarioBuscaAdapter(
        private var lista: List<UsuarioBusca>,
        private val onClick: (UsuarioBusca) -> Unit
    ) : RecyclerView.Adapter<UsuarioBuscaAdapter.VH>() {

        class VH(v: View) : RecyclerView.ViewHolder(v) {
            val img: ImageView = v.findViewById(R.id.imgFotoBuscaUser)
            val nome: TextView = v.findViewById(R.id.txtNomeBuscaUser)
            val username: TextView = v.findViewById(R.id.txtUsernameBuscaUser)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            return VH(LayoutInflater.from(parent.context).inflate(R.layout.item_usuario_busca, parent, false))
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val user = lista[position]
            holder.nome.text = user.nome
            holder.username.text = if (user.username.isNotEmpty()) "@${user.username}" else ""

            if (user.fotoUrl.isNotEmpty()) {
                Glide.with(holder.itemView.context)
                    .load(user.fotoUrl).circleCrop().into(holder.img)
                holder.img.imageTintList = null
                holder.img.setPadding(0, 0, 0, 0)
            }

            holder.itemView.setOnClickListener { onClick(user) }
        }

        override fun getItemCount() = lista.size

        fun atualizarLista(nova: List<UsuarioBusca>) {
            lista = nova
            notifyDataSetChanged()
        }
    }
}