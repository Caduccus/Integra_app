package com.example.plataformaremota

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.plataformaremota.data.entity.Empresa
import com.example.plataformaremota.data.entity.Trabalho
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class BuscaGlobalActivity : BaseActivity() {

    private lateinit var edtBusca: EditText
    private lateinit var layoutInicial: View
    private lateinit var layoutVazio: View
    private lateinit var containerResultados: View

    private lateinit var secaoTrabalhos: View
    private lateinit var secaoEmpresas: View
    private lateinit var secaoUsuarios: View

    private lateinit var rvTrabalhos: RecyclerView
    private lateinit var rvEmpresas: RecyclerView
    private lateinit var rvUsuarios: RecyclerView

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var todosTrabalhos: List<Trabalho> = emptyList()
    private var todasEmpresas: List<Empresa> = emptyList()
    private var todosUsuarios: List<UsuarioResumo> = emptyList()

    data class UsuarioResumo(
        val uid: String,
        val nome: String,
        val username: String,
        val profissao: String,
        val fotoUrl: String
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_busca_global)

        edtBusca = findViewById(R.id.edtBuscaGlobal)
        layoutInicial = findViewById(R.id.layoutInicial)
        layoutVazio = findViewById(R.id.layoutVazioBusca)
        containerResultados = findViewById(R.id.containerResultados)

        secaoTrabalhos = findViewById(R.id.secaoTrabalhos)
        secaoEmpresas = findViewById(R.id.secaoEmpresas)
        secaoUsuarios = findViewById(R.id.secaoUsuarios)

        rvTrabalhos = findViewById(R.id.rvBuscaTrabalhos)
        rvEmpresas = findViewById(R.id.rvBuscaEmpresas)
        rvUsuarios = findViewById(R.id.rvBuscaUsuarios)

        rvTrabalhos.layoutManager = LinearLayoutManager(this)
        rvEmpresas.layoutManager = LinearLayoutManager(this)
        rvUsuarios.layoutManager = LinearLayoutManager(this)

        findViewById<ImageView>(R.id.btnVoltarBusca).setOnClickListener { finish() }

        edtBusca.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val termo = s?.toString()?.trim()?.lowercase() ?: ""
                aplicarBusca(termo)
            }
        })

        carregarDados()
    }

    private fun carregarDados() {
        lifecycleScope.launch {
            try {
                val trabalhosSnap = db.collection("trabalhos").get().await()
                todosTrabalhos = trabalhosSnap.documents.mapNotNull {
                    it.toObject(Trabalho::class.java)
                }

                val empresasSnap = db.collection("empresas").get().await()
                todasEmpresas = empresasSnap.documents.mapNotNull {
                    it.toObject(Empresa::class.java)
                }

                val usuariosSnap = db.collection("usuarios").get().await()
                todosUsuarios = usuariosSnap.documents.mapNotNull { doc ->
                    UsuarioResumo(
                        uid = doc.id,
                        nome = doc.getString("nome") ?: "Usuário",
                        username = doc.getString("username") ?: "",
                        profissao = doc.getString("profissao") ?: "",
                        fotoUrl = doc.getString("fotoUrl") ?: ""
                    )
                }
            } catch (_: Exception) { }
        }
    }

    private fun aplicarBusca(termo: String) {
        if (termo.isEmpty()) {
            layoutInicial.visibility = View.VISIBLE
            layoutVazio.visibility = View.GONE
            containerResultados.visibility = View.GONE
            return
        }

        val trabalhosFiltrados = todosTrabalhos.filter {
            it.titulo.lowercase().contains(termo) ||
                    it.descricao.lowercase().contains(termo) ||
                    it.categoria.lowercase().contains(termo)
        }.take(5)

        val empresasFiltradas = todasEmpresas.filter {
            it.nome.lowercase().contains(termo) ||
                    it.areaAtuacao.lowercase().contains(termo) ||
                    it.descricao.lowercase().contains(termo)
        }.take(5)

        val uidAtual = auth.currentUser?.uid
        val usuariosFiltrados = todosUsuarios.filter {
            it.uid != uidAtual && (
                    it.nome.lowercase().contains(termo) ||
                            it.username.lowercase().contains(termo) ||
                            it.profissao.lowercase().contains(termo)
                    )
        }.take(5)

        val temResultado = trabalhosFiltrados.isNotEmpty() ||
                empresasFiltradas.isNotEmpty() ||
                usuariosFiltrados.isNotEmpty()

        layoutInicial.visibility = View.GONE

        if (!temResultado) {
            layoutVazio.visibility = View.VISIBLE
            containerResultados.visibility = View.GONE
            return
        }

        layoutVazio.visibility = View.GONE
        containerResultados.visibility = View.VISIBLE

        // Trabalhos
        if (trabalhosFiltrados.isNotEmpty()) {
            secaoTrabalhos.visibility = View.VISIBLE
            rvTrabalhos.adapter = TrabalhosBuscaAdapter(trabalhosFiltrados)
        } else {
            secaoTrabalhos.visibility = View.GONE
        }

        // Empresas
        if (empresasFiltradas.isNotEmpty()) {
            secaoEmpresas.visibility = View.VISIBLE
            rvEmpresas.adapter = EmpresasBuscaAdapter(empresasFiltradas)
        } else {
            secaoEmpresas.visibility = View.GONE
        }

        // Usuários
        if (usuariosFiltrados.isNotEmpty()) {
            secaoUsuarios.visibility = View.VISIBLE
            rvUsuarios.adapter = UsuariosBuscaAdapter(usuariosFiltrados)
        } else {
            secaoUsuarios.visibility = View.GONE
        }
    }

    // ─────────────────────────────────────────────
    // Adapters inline
    // ─────────────────────────────────────────────
    inner class TrabalhosBuscaAdapter(private val itens: List<Trabalho>) :
        RecyclerView.Adapter<TrabalhosBuscaAdapter.VH>() {

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val titulo: TextView = v.findViewById(R.id.txtTituloBuscaTrabalho)
            val sub: TextView = v.findViewById(R.id.txtSubBuscaTrabalho)
        }

        override fun onCreateViewHolder(p: ViewGroup, t: Int): VH =
            VH(LayoutInflater.from(p.context).inflate(R.layout.item_busca_trabalho, p, false))

        override fun onBindViewHolder(h: VH, pos: Int) {
            val item = itens[pos]
            h.titulo.text = item.titulo
            h.sub.text = buildString {
                if (item.categoria.isNotEmpty()) append(item.categoria)
                if (item.empresaNome.isNotEmpty()) {
                    if (isNotEmpty()) append(" • ")
                    append(item.empresaNome)
                }
            }
            h.itemView.setOnClickListener {
                val i = Intent(this@BuscaGlobalActivity, MainActivity2::class.java)
                i.putExtra("trabalhoId", item.id)
                startActivity(i)
            }
        }

        override fun getItemCount() = itens.size
    }

    inner class EmpresasBuscaAdapter(private val itens: List<Empresa>) :
        RecyclerView.Adapter<EmpresasBuscaAdapter.VH>() {

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val img: ImageView = v.findViewById(R.id.imgLogoBuscaEmpresa)
            val nome: TextView = v.findViewById(R.id.txtNomeBuscaEmpresa)
            val sub: TextView = v.findViewById(R.id.txtSubBuscaEmpresa)
        }

        override fun onCreateViewHolder(p: ViewGroup, t: Int): VH =
            VH(LayoutInflater.from(p.context).inflate(R.layout.item_busca_empresa, p, false))

        override fun onBindViewHolder(h: VH, pos: Int) {
            val item = itens[pos]
            h.nome.text = item.nome
            h.sub.text = item.areaAtuacao

            if (item.logoUrl.isNotEmpty()) {
                Glide.with(h.itemView.context).load(item.logoUrl).circleCrop().into(h.img)
                h.img.imageTintList = null
                h.img.setPadding(0, 0, 0, 0)
            }

            h.itemView.setOnClickListener {
                val i = Intent(this@BuscaGlobalActivity, DetalhesEmpresaActivity::class.java)
                i.putExtra("empresaId", item.id)
                startActivity(i)
            }
        }

        override fun getItemCount() = itens.size
    }

    inner class UsuariosBuscaAdapter(private val itens: List<UsuarioResumo>) :
        RecyclerView.Adapter<UsuariosBuscaAdapter.VH>() {

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val img: ImageView = v.findViewById(R.id.imgFotoBuscaUsuario)
            val nome: TextView = v.findViewById(R.id.txtNomeBuscaUsuario)
            val sub: TextView = v.findViewById(R.id.txtSubBuscaUsuario)
        }

        override fun onCreateViewHolder(p: ViewGroup, t: Int): VH =
            VH(LayoutInflater.from(p.context).inflate(R.layout.item_busca_usuario, p, false))

        override fun onBindViewHolder(h: VH, pos: Int) {
            val item = itens[pos]
            h.nome.text = item.nome
            h.sub.text = buildString {
                if (item.username.isNotEmpty()) append("@${item.username}")
                if (item.profissao.isNotEmpty()) {
                    if (isNotEmpty()) append(" • ")
                    append(item.profissao)
                }
            }

            if (item.fotoUrl.isNotEmpty()) {
                Glide.with(h.itemView.context).load(item.fotoUrl).circleCrop().into(h.img)
                h.img.imageTintList = null
                h.img.setPadding(0, 0, 0, 0)
            }

            h.itemView.setOnClickListener {
                val i = Intent(this@BuscaGlobalActivity, PerfilUsuarioActivity::class.java)
                i.putExtra("uidUsuario", item.uid)
                i.putExtra("chatId", "")
                startActivity(i)
            }
        }

        override fun getItemCount() = itens.size
    }
}