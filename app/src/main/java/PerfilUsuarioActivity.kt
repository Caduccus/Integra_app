package com.example.plataformaremota

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.plataformaremota.data.entity.Chat
import com.example.plataformaremota.data.repository.ChatRepository
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class PerfilUsuarioActivity : BaseActivity() {

    private lateinit var btnVoltar: ImageView
    private lateinit var imgFoto: ImageView
    private lateinit var txtNome: TextView
    private lateinit var txtUsername: TextView
    private lateinit var txtProfissao: TextView
    private lateinit var txtEmail: TextView
    private lateinit var txtBioUsuarioPerfil: TextView      // ⭐ NOVO

    private lateinit var txtTituloGrupo: TextView
    private lateinit var layoutAcoesGrupo: View
    private lateinit var btnPromover: MaterialButton
    private lateinit var btnRemover: MaterialButton

    private lateinit var txtTituloBloquear: TextView
    private lateinit var btnBloquear: MaterialButton

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
        txtBioUsuarioPerfil = findViewById(R.id.txtBioUsuarioPerfil)   // ⭐ NOVO

        txtTituloGrupo = findViewById(R.id.txtTituloAcoes)
        layoutAcoesGrupo = findViewById(R.id.layoutAcoesGrupo)
        btnPromover = findViewById(R.id.btnPromoverAdmin)
        btnRemover = findViewById(R.id.btnRemoverDoGrupo)

        txtTituloBloquear = findViewById(R.id.txtTituloBloquear)
        btnBloquear = findViewById(R.id.btnBloquearUsuario)

        uidUsuario = intent.getStringExtra("uidUsuario") ?: ""
        chatId = intent.getStringExtra("chatId") ?: ""

        if (uidUsuario.isEmpty()) {
            Toast.makeText(this, "Erro ao abrir perfil", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        btnVoltar.setOnClickListener { finish() }

        carregarDados()
    }

    private fun carregarDados() {
        val uidAtual = auth.currentUser?.uid ?: return

        lifecycleScope.launch {
            try {
                // ─── Carrega dados do usuário ───
                val docUser = db.collection("usuarios").document(uidUsuario).get().await()
                val nome = docUser.getString("nome") ?: "Usuário"
                val username = docUser.getString("username") ?: ""
                val profissao = docUser.getString("profissao") ?: ""
                val email = docUser.getString("email") ?: ""
                val fotoUrl = docUser.getString("fotoUrl") ?: ""
                val bio = docUser.getString("bio") ?: ""               // ⭐ NOVO

                txtNome.text = nome
                txtUsername.text = if (username.isNotEmpty()) "@$username" else ""
                txtProfissao.text = profissao.ifEmpty { "—" }
                txtEmail.text = email
                txtBioUsuarioPerfil.text =
                    if (bio.isEmpty()) "Sem bio ainda" else bio       // ⭐ NOVO

                if (fotoUrl.isNotEmpty()) {
                    Glide.with(this@PerfilUsuarioActivity)
                        .load(fotoUrl).circleCrop().into(imgFoto)
                    imgFoto.imageTintList = null
                    imgFoto.setPadding(0, 0, 0, 0)
                }

                // ─── Carrega dados do grupo (se tiver chatId) ───
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
                val meusBloqueados = (docMeu.get("bloqueados") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
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
                        Toast.makeText(this@PerfilUsuarioActivity, "Promovido!", Toast.LENGTH_SHORT).show()
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
                        Toast.makeText(this@PerfilUsuarioActivity, "Rebaixado", Toast.LENGTH_SHORT).show()
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
                        Toast.makeText(this@PerfilUsuarioActivity, "Removido!", Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(this@PerfilUsuarioActivity, "Desbloqueado", Toast.LENGTH_SHORT).show()
                }
            } else {
                val ok = repository.bloquearUsuario(uidUsuario)
                if (ok) {
                    usuarioEstaBloqueado = true
                    atualizarBotaoBloquear()
                    Toast.makeText(this@PerfilUsuarioActivity, "Bloqueado", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}