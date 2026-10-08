package com.example.plataformaremota

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.plataformaremota.data.entity.Chat
import com.example.plataformaremota.data.repository.ChatRepository
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.math.roundToInt
class PerfilUsuarioActivity : BaseActivity() {

    private lateinit var btnVoltar: ImageView
    private lateinit var imgFoto: ImageView
    private lateinit var txtNome: TextView
    private lateinit var txtUsername: TextView
    private lateinit var txtProfissao: TextView
    private lateinit var txtEmail: TextView
    private lateinit var txtBioUsuarioPerfil: TextView
    private lateinit var dotStatusUsuario: View

    private lateinit var txtTituloGrupo: TextView
    private lateinit var layoutAcoesGrupo: View
    private lateinit var btnPromover: MaterialButton
    private lateinit var btnRemover: MaterialButton

    private lateinit var txtTituloBloquear: TextView
    private lateinit var btnBloquear: MaterialButton

    // Links
    private lateinit var containerLinksUsuario: LinearLayout
    private lateinit var txtSemLinksUsuario: TextView

    // Currículo
    private lateinit var txtTituloCurriculo: TextView
    private lateinit var cardCurriculoUsuario: MaterialCardView
    private lateinit var txtNomeCurriculoUsuario: TextView
    private var curriculoUrlUsuario: String = ""

    // ⭐ Avaliações
    private lateinit var txtTituloAvaliacoes: TextView
    private lateinit var cardAvaliacoesUsuario: MaterialCardView
    private lateinit var txtMediaAvaliacoes: TextView
    private lateinit var linhaEstrelasMedia: LinearLayout
    private lateinit var txtTotalAvaliacoes: TextView
    private lateinit var containerAvaliacoesUsuario: LinearLayout

    private lateinit var repository: ChatRepository

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var uidUsuario: String = ""
    private var chatId: String = ""
    private var chat: Chat? = null
    private var souAdmin: Boolean = false
    private var souCriador: Boolean = false
    private var usuarioEstaBloqueado: Boolean = false
    private var usuarioEhAdmin: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil_usuario)

        repository = ChatRepository()

        btnVoltar = findViewById(R.id.btnVoltarPerfilUsuario)
        imgFoto = findViewById(R.id.imgFotoUsuario)
        txtNome = findViewById(R.id.txtNomeUsuarioPerfil)
        txtUsername = findViewById(R.id.txtUsernameUsuarioPerfil)
        txtProfissao = findViewById(R.id.txtProfissaoUsuarioPerfil)
        txtEmail = findViewById(R.id.txtEmailUsuarioPerfil)
        txtBioUsuarioPerfil = findViewById(R.id.txtBioUsuarioPerfil)
        dotStatusUsuario = findViewById(R.id.dotStatusUsuario)

        txtTituloGrupo = findViewById(R.id.txtTituloAcoes)
        layoutAcoesGrupo = findViewById(R.id.layoutAcoesGrupo)
        btnPromover = findViewById(R.id.btnPromoverAdmin)
        btnRemover = findViewById(R.id.btnRemoverDoGrupo)

        txtTituloBloquear = findViewById(R.id.txtTituloBloquear)
        btnBloquear = findViewById(R.id.btnBloquearUsuario)

        containerLinksUsuario = findViewById(R.id.containerLinksUsuario)
        txtSemLinksUsuario = findViewById(R.id.txtSemLinksUsuario)

        txtTituloCurriculo = findViewById(R.id.txtTituloCurriculo)
        cardCurriculoUsuario = findViewById(R.id.cardCurriculoUsuario)
        txtNomeCurriculoUsuario = findViewById(R.id.txtNomeCurriculoUsuario)

        txtTituloAvaliacoes = findViewById(R.id.txtTituloAvaliacoes)
        cardAvaliacoesUsuario = findViewById(R.id.cardAvaliacoesUsuario)
        txtMediaAvaliacoes = findViewById(R.id.txtMediaAvaliacoes)
        linhaEstrelasMedia = findViewById(R.id.linhaEstrelasMedia)
        txtTotalAvaliacoes = findViewById(R.id.txtTotalAvaliacoes)
        containerAvaliacoesUsuario = findViewById(R.id.containerAvaliacoesUsuario)

        uidUsuario = intent.getStringExtra("uidUsuario") ?: ""
        chatId = intent.getStringExtra("chatId") ?: ""

        if (uidUsuario.isEmpty()) {
            Toast.makeText(this, "Erro ao abrir perfil", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        btnVoltar.setOnClickListener { finish() }

        registrarVisualizacao()
        carregarDados()
    }

    private fun registrarVisualizacao() {
        val uidAtual = auth.currentUser?.uid ?: return
        if (uidAtual == uidUsuario) return

        lifecycleScope.launch {
            try {
                db.collection("usuarios").document(uidUsuario)
                    .collection("visualizacoes").document(uidAtual)
                    .set(mapOf("timestamp" to System.currentTimeMillis()))
                    .await()
            } catch (_: Exception) { }
        }
    }

    private fun carregarDados() {
        val uidAtual = auth.currentUser?.uid ?: return

        lifecycleScope.launch {
            try {
                val docUser = db.collection("usuarios").document(uidUsuario).get().await()
                val nome = docUser.getString("nome") ?: "Usuário"
                val username = docUser.getString("username") ?: ""
                val profissao = docUser.getString("profissao") ?: ""
                val email = docUser.getString("email") ?: ""
                val fotoUrl = docUser.getString("fotoUrl") ?: ""
                val bio = docUser.getString("bio") ?: ""
                val status = docUser.getString("status") ?: ThemeManager.STATUS_ONLINE

                txtNome.text = nome
                txtUsername.text = if (username.isNotEmpty()) "@$username" else ""
                txtProfissao.text = profissao.ifEmpty { "—" }
                txtEmail.text = email
                txtBioUsuarioPerfil.text = if (bio.isEmpty()) "Sem bio ainda" else bio

                dotStatusUsuario.backgroundTintList =
                    android.content.res.ColorStateList.valueOf(
                        ThemeManager.getStatusColor(status)
                    )

                if (fotoUrl.isNotEmpty()) {
                    Glide.with(this@PerfilUsuarioActivity)
                        .load(fotoUrl).circleCrop().into(imgFoto)
                    imgFoto.imageTintList = null
                    imgFoto.setPadding(0, 0, 0, 0)
                }

                carregarLinksUsuario(docUser)
                carregarCurriculo(docUser)
                carregarAvaliacoesUsuario()

                if (chatId.isNotEmpty()) {
                    val docChat = db.collection("chats").document(chatId).get().await()
                    val c = docChat.toObject(Chat::class.java)
                    chat = c

                    if (c != null) {
                        souCriador = c.criadorId == uidAtual
                        souAdmin = c.admins.contains(uidAtual) || souCriador
                        usuarioEhAdmin = c.admins.contains(uidUsuario) || c.criadorId == uidUsuario

                        if (uidUsuario == uidAtual) {
                            txtTituloGrupo.visibility = View.GONE
                            layoutAcoesGrupo.visibility = View.GONE
                            txtTituloBloquear.visibility = View.GONE
                            btnBloquear.visibility = View.GONE
                        } else {
                            configurarBotoesGrupo()
                            configurarBloqueio(uidAtual)
                        }
                    }
                } else {
                    txtTituloGrupo.visibility = View.GONE
                    layoutAcoesGrupo.visibility = View.GONE

                    if (uidUsuario == uidAtual) {
                        txtTituloBloquear.visibility = View.GONE
                        btnBloquear.visibility = View.GONE
                    } else {
                        configurarBloqueio(uidAtual)
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@PerfilUsuarioActivity,
                    "Erro: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun carregarAvaliacoesUsuario() {
        lifecycleScope.launch {
            try {
                val snap = db.collection("avaliacoes")
                    .whereEqualTo("alvoId", uidUsuario)
                    .whereEqualTo("alvoTipo", "usuario")
                    .get().await()

                val avaliacoes = snap.documents.mapNotNull {
                    it.toObject(com.example.plataformaremota.data.entity.Avaliacao::class.java)
                }.sortedByDescending { it.timestamp }

                if (avaliacoes.isEmpty()) {
                    txtTituloAvaliacoes.visibility = View.GONE
                    cardAvaliacoesUsuario.visibility = View.GONE
                    return@launch
                }

                txtTituloAvaliacoes.visibility = View.VISIBLE
                cardAvaliacoesUsuario.visibility = View.VISIBLE

                val media = avaliacoes.map { it.estrelas }.average()
                txtMediaAvaliacoes.text = String.format("%.1f", media)
                txtTotalAvaliacoes.text =
                    "(${avaliacoes.size} ${if (avaliacoes.size == 1) "avaliação" else "avaliações"})"

                linhaEstrelasMedia.removeAllViews()
                for (i in 1..5) {
                    val iv = ImageView(this@PerfilUsuarioActivity)
                    val size = (18 * resources.displayMetrics.density).toInt()
                    val params = LinearLayout.LayoutParams(size, size)
                    params.marginEnd = (2 * resources.displayMetrics.density).toInt()
                    iv.layoutParams = params
                    if (i <= media.roundToInt()) {
                        iv.setImageResource(R.drawable.ic_star_filled)
                        iv.imageTintList =
                            android.content.res.ColorStateList.valueOf(0xFFFFC107.toInt())
                    } else {
                        iv.setImageResource(R.drawable.ic_star_outline)
                        iv.imageTintList =
                            android.content.res.ColorStateList.valueOf(0xFFBDBDBD.toInt())
                    }
                    linhaEstrelasMedia.addView(iv)
                }

                containerAvaliacoesUsuario.removeAllViews()
                for (av in avaliacoes.take(5)) {
                    val itemView = layoutInflater.inflate(
                        R.layout.item_avaliacao, containerAvaliacoesUsuario, false
                    )
                    itemView.findViewById<TextView>(R.id.txtAutorAvaliacao).text = av.autorNome
                    itemView.findViewById<TextView>(R.id.txtComentarioAvaliacao).text =
                        if (av.comentario.isEmpty()) "(sem comentário)" else av.comentario

                    val ctx = itemView.findViewById<TextView>(R.id.txtContextoAvaliacao)
                    if (av.trabalhoTitulo.isNotEmpty()) {
                        ctx.visibility = View.VISIBLE
                        ctx.text = "Sobre: ${av.trabalhoTitulo}"
                    }

                    val linha = itemView.findViewById<LinearLayout>(R.id.linhaEstrelasItem)
                    for (i in 1..5) {
                        val iv = ImageView(this@PerfilUsuarioActivity)
                        val size = (14 * resources.displayMetrics.density).toInt()
                        val params = LinearLayout.LayoutParams(size, size)
                        iv.layoutParams = params
                        if (i <= av.estrelas) {
                            iv.setImageResource(R.drawable.ic_star_filled)
                            iv.imageTintList =
                                android.content.res.ColorStateList.valueOf(0xFFFFC107.toInt())
                        } else {
                            iv.setImageResource(R.drawable.ic_star_outline)
                            iv.imageTintList =
                                android.content.res.ColorStateList.valueOf(0xFFBDBDBD.toInt())
                        }
                        linha.addView(iv)
                    }

                    containerAvaliacoesUsuario.addView(itemView)
                }
            } catch (_: Exception) { }
        }
    }

    private fun carregarCurriculo(doc: com.google.firebase.firestore.DocumentSnapshot) {
        val url = doc.getString("curriculoUrl") ?: ""
        val nome = doc.getString("curriculoNome") ?: "curriculo.pdf"

        if (url.isEmpty()) {
            txtTituloCurriculo.visibility = View.GONE
            cardCurriculoUsuario.visibility = View.GONE
            return
        }

        curriculoUrlUsuario = url
        txtTituloCurriculo.visibility = View.VISIBLE
        cardCurriculoUsuario.visibility = View.VISIBLE
        txtNomeCurriculoUsuario.text = nome

        cardCurriculoUsuario.setOnClickListener {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(curriculoUrlUsuario)))
            } catch (_: Exception) {
                Toast.makeText(
                    this@PerfilUsuarioActivity,
                    "Nenhum app pra abrir PDF",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun carregarLinksUsuario(doc: com.google.firebase.firestore.DocumentSnapshot) {
        containerLinksUsuario.removeAllViews()

        val listaRaw = doc.get("links") as? List<*> ?: emptyList<Any>()
        if (listaRaw.isEmpty()) {
            txtSemLinksUsuario.visibility = View.VISIBLE
            return
        }
        txtSemLinksUsuario.visibility = View.GONE

        for (item in listaRaw) {
            @Suppress("UNCHECKED_CAST")
            val map = item as? Map<String, Any?> ?: continue
            val tipo = map["tipo"] as? String ?: continue
            val valor = map["valor"] as? String ?: continue

            val itemView = layoutInflater.inflate(
                R.layout.item_link_contato, containerLinksUsuario, false
            )
            val txtTipo = itemView.findViewById<TextView>(R.id.txtLinkTipo)
            val txtValor = itemView.findViewById<TextView>(R.id.txtLinkValor)
            val btnRemover = itemView.findViewById<ImageView>(R.id.btnRemoverLink)

            txtTipo.text = tipo.replaceFirstChar { it.uppercase() }
            txtValor.text = valor
            btnRemover.visibility = View.GONE

            itemView.setOnClickListener {
                val url = when (tipo.lowercase()) {
                    "whatsapp"  -> "https://wa.me/${valor.filter { it.isDigit() }}"
                    "linkedin"  -> if (valor.startsWith("http")) valor else "https://linkedin.com/in/$valor"
                    "github"    -> if (valor.startsWith("http")) valor else "https://github.com/$valor"
                    "instagram" -> if (valor.startsWith("http")) valor else "https://instagram.com/$valor"
                    "email"     -> "mailto:$valor"
                    else        -> if (valor.startsWith("http")) valor else "https://$valor"
                }
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                } catch (_: Exception) { }
            }

            containerLinksUsuario.addView(itemView)
        }
    }

    private fun configurarBotoesGrupo() {
        txtTituloGrupo.visibility = View.VISIBLE
        layoutAcoesGrupo.visibility = View.VISIBLE

        if (souAdmin && !usuarioEhAdmin) {
            btnPromover.visibility = View.VISIBLE
            btnPromover.text = "PROMOVER A ADMIN"
            btnPromover.setBackgroundColor(0xFF43A047.toInt())
            btnPromover.setOnClickListener { confirmarPromover() }
        } else if (souCriador && usuarioEhAdmin && uidUsuario != chat?.criadorId) {
            btnPromover.visibility = View.VISIBLE
            btnPromover.text = "REBAIXAR ADMIN"
            btnPromover.setBackgroundColor(0xFFFB8C00.toInt())
            btnPromover.setOnClickListener { confirmarRebaixar() }
        } else {
            btnPromover.visibility = View.GONE
        }

        if (souAdmin && uidUsuario != chat?.criadorId) {
            btnRemover.visibility = View.VISIBLE
            btnRemover.setOnClickListener { confirmarRemover() }
        } else {
            btnRemover.visibility = View.GONE
        }
    }

    private fun configurarBloqueio(uidAtual: String) {
        txtTituloBloquear.visibility = View.VISIBLE
        btnBloquear.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val docMeu = db.collection("usuarios").document(uidAtual).get().await()
                val meusBloqueados = (docMeu.get("bloqueados") as? List<*>)
                    ?.filterIsInstance<String>() ?: emptyList()
                usuarioEstaBloqueado = meusBloqueados.contains(uidUsuario)

                atualizarBotaoBloquear()
            } catch (_: Exception) { }
        }
    }

    private fun atualizarBotaoBloquear() {
        if (usuarioEstaBloqueado) {
            btnBloquear.text = "DESBLOQUEAR USUÁRIO"
            btnBloquear.setBackgroundColor(0xFF43A047.toInt())
        } else {
            btnBloquear.text = "BLOQUEAR USUÁRIO"
            btnBloquear.setBackgroundColor(0xFF757575.toInt())
        }
        btnBloquear.setOnClickListener { toggleBloqueio() }
    }

    private fun confirmarPromover() {
        AlertDialog.Builder(this)
            .setTitle("Promover a admin")
            .setMessage("Tornar ${txtNome.text} admin do grupo?")
            .setPositiveButton("Promover") { _, _ ->
                lifecycleScope.launch {
                    val ok = repository.promoverAdmin(chatId, uidUsuario)
                    if (ok) {
                        Toast.makeText(
                            this@PerfilUsuarioActivity,
                            "Promovido!",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun confirmarRebaixar() {
        AlertDialog.Builder(this)
            .setTitle("Rebaixar admin")
            .setMessage("Remover ${txtNome.text} de admin?")
            .setPositiveButton("Rebaixar") { _, _ ->
                lifecycleScope.launch {
                    val ok = repository.rebaixarAdmin(chatId, uidUsuario)
                    if (ok) {
                        Toast.makeText(
                            this@PerfilUsuarioActivity,
                            "Rebaixado",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun confirmarRemover() {
        AlertDialog.Builder(this)
            .setTitle("Remover do grupo")
            .setMessage("Remover ${txtNome.text} do grupo?")
            .setPositiveButton("Remover") { _, _ ->
                lifecycleScope.launch {
                    val ok = repository.removerMembro(chatId, uidUsuario)
                    if (ok) {
                        Toast.makeText(
                            this@PerfilUsuarioActivity,
                            "Removido!",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun toggleBloqueio() {
        lifecycleScope.launch {
            if (usuarioEstaBloqueado) {
                val ok = repository.desbloquearUsuario(uidUsuario)
                if (ok) {
                    usuarioEstaBloqueado = false
                    atualizarBotaoBloquear()
                    Toast.makeText(
                        this@PerfilUsuarioActivity,
                        "Desbloqueado",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                val ok = repository.bloquearUsuario(uidUsuario)
                if (ok) {
                    usuarioEstaBloqueado = true
                    atualizarBotaoBloquear()
                    Toast.makeText(
                        this@PerfilUsuarioActivity,
                        "Bloqueado",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}