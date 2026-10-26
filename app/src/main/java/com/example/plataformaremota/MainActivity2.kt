package com.example.plataformaremota

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.example.plataformaremota.data.repository.ChatRepository
import com.example.plataformaremota.data.repository.TrabalhoRepository
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity2 : BaseActivity() {

    private var empresaIdDoTrabalho: String = ""
    private var souMembroDaEmpresa: Boolean = false

    // Variáveis de UI
    private lateinit var btnVoltar: ImageView
    private lateinit var btnCompartilhar: ImageView
    private lateinit var txtCriador: TextView
    private lateinit var txtTituloDetalhe: TextView
    private lateinit var txtDescricaoDetalhe: TextView
    private lateinit var txtCategoriaDetalhe: TextView
    private lateinit var txtTipoContratoDetalhe: TextView
    private lateinit var txtNivelDetalhe: TextView
    private lateinit var txtPrazoDetalhe: TextView

    private lateinit var layoutBotoesCriador: View
    private lateinit var btnExcluir: MaterialButton
    private lateinit var btnEditar: MaterialButton
    private lateinit var btnChat: MaterialButton
    private lateinit var btnCandidatar: MaterialButton
    private lateinit var badgeCandidatos: TextView

    private lateinit var repository: TrabalhoRepository
    private lateinit var chatRepository: ChatRepository

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private var trabalhoId: String = ""
    private var criadorId: String = ""
    private var nomeCriador: String = ""
    private var tituloTrabalho: String = ""
    private var jaCandidatou: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)

        repository = TrabalhoRepository(this)
        chatRepository = ChatRepository()

        // Inicializando as Views
        btnVoltar = findViewById(R.id.btnVoltar)
        btnCompartilhar = findViewById(R.id.btnCompartilhar)
        txtCriador = findViewById(R.id.txtCriador)
        txtTituloDetalhe = findViewById(R.id.txtTituloDetalhe)
        txtDescricaoDetalhe = findViewById(R.id.txtDescricaoDetalhe)
        txtCategoriaDetalhe = findViewById(R.id.txtCategoriaDetalhe)
        txtTipoContratoDetalhe = findViewById(R.id.txtTipoContratoDetalhe)
        txtNivelDetalhe = findViewById(R.id.txtNivelDetalhe)
        txtPrazoDetalhe = findViewById(R.id.txtPrazoDetalhe)

        layoutBotoesCriador = findViewById(R.id.layoutBotoesCriador)
        btnExcluir = findViewById(R.id.btnExcluir) // ⭐ AQUI
        btnEditar = findViewById(R.id.btnEditar)
        btnChat = findViewById(R.id.btnChat)
        btnCandidatar = findViewById(R.id.btnCandidatar)
        badgeCandidatos = findViewById(R.id.badgeCandidatos)

        trabalhoId = intent.getStringExtra("trabalhoId") ?: ""

        btnVoltar.setOnClickListener { finish() }
        btnCompartilhar.setOnClickListener { compartilharTrabalho() }
        btnExcluir.setOnClickListener { confirmarExclusao() } // ⭐ AQUI DA ERRO SE NÃO ACHAR ACIMA

        btnEditar.setOnClickListener {
            val intent = Intent(this, EditarTrabalhoActivity::class.java)
            intent.putExtra("trabalhoId", trabalhoId)
            startActivity(intent)
        }

        btnChat.setOnClickListener {
            val intent = Intent(this, CandidatosActivity::class.java)
            intent.putExtra("trabalhoId", trabalhoId)
            startActivity(intent)
        }

        btnCandidatar.setOnClickListener {
            if (jaCandidatou) abrirChatComCriador() else cadastrarSeNoTrabalho()
        }

        carregarTrabalho()
    }

    override fun onResume() {
        super.onResume()
        if (trabalhoId.isNotEmpty()) {
            carregarTrabalho()
        }
    }

    private fun carregarTrabalho() {
        if (trabalhoId.isEmpty()) {
            Toast.makeText(this, getString(R.string.trabalho_nao_encontrado), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        lifecycleScope.launch {
            val trabalho = repository.buscarPorId(trabalhoId)
            if (trabalho == null) {
                Toast.makeText(this@MainActivity2, getString(R.string.trabalho_nao_encontrado), Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }

            criadorId = trabalho.criadorId
            nomeCriador = trabalho.nomeCriador
            tituloTrabalho = trabalho.titulo
            empresaIdDoTrabalho = trabalho.empresaId

            txtTituloDetalhe.text = trabalho.titulo
            txtDescricaoDetalhe.text = trabalho.descricao
            txtCategoriaDetalhe.text = trabalho.categoria
            txtTipoContratoDetalhe.text = trabalho.tipoContrato
            txtNivelDetalhe.text = trabalho.nivel
            txtPrazoDetalhe.text = trabalho.prazo

            txtCriador.text = getString(R.string.trabalho_publicado_por, nomeCriador.ifEmpty { getString(R.string.admin_usuario_padrao) })

            val uidAtual = auth.currentUser?.uid ?: ""
            val ehCriador = uidAtual == criadorId

            if (ehCriador) {
                layoutBotoesCriador.visibility = View.VISIBLE
                btnCandidatar.visibility = View.GONE
                atualizarBadgeCandidatos()
            } else {
                layoutBotoesCriador.visibility = View.GONE

                if (empresaIdDoTrabalho.isNotEmpty()) {
                    verificarSeMembroDaEmpresa(uidAtual)
                } else {
                    btnCandidatar.visibility = View.VISIBLE
                    verificarCandidatura(uidAtual)
                }
            }
        }
    }

    private fun atualizarBadgeCandidatos() {
        lifecycleScope.launch {
            try {
                val snap = db.collection("trabalhos")
                    .document(trabalhoId)
                    .collection("candidaturas")
                    .get()
                    .await()

                val pendentes = snap.documents.count { doc ->
                    val status = doc.getString("status")?.lowercase() ?: "pendente"
                    status !in listOf("aceito", "aceita", "rejeitado", "rejeitada")
                }

                if (pendentes > 0) {
                    badgeCandidatos.text = if (pendentes > 99) "99+" else pendentes.toString()
                    badgeCandidatos.visibility = View.VISIBLE
                } else {
                    badgeCandidatos.visibility = View.GONE
                }
            } catch (_: Exception) {
                badgeCandidatos.visibility = View.GONE
            }
        }
    }

    private fun compartilharTrabalho() {
        if (tituloTrabalho.isEmpty()) {
            Toast.makeText(this, getString(R.string.trabalho_aguarde_carregar), Toast.LENGTH_SHORT).show()
            return
        }

        val texto = buildString {
            append("📋 ").append(tituloTrabalho).append("\n\n")
            if (txtDescricaoDetalhe.text.isNotEmpty()) {
                append(txtDescricaoDetalhe.text).append("\n\n")
            }
            if (txtCategoriaDetalhe.text.isNotEmpty()) {
                append(getString(R.string.trabalho_share_extra, txtCategoriaDetalhe.text)).append("\n")
            }
            if (txtNivelDetalhe.text.isNotEmpty()) {
                append(getString(R.string.trabalho_share_nivel, txtNivelDetalhe.text)).append("\n")
            }
            if (txtPrazoDetalhe.text.isNotEmpty()) {
                append(getString(R.string.trabalho_share_prazo, txtPrazoDetalhe.text)).append("\n")
            }
            append("\n")
            append(getString(R.string.trabalho_share_encontrado))
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, tituloTrabalho)
            putExtra(Intent.EXTRA_TEXT, texto)
        }

        startActivity(Intent.createChooser(shareIntent, getString(R.string.trabalho_share_titulo)))
    }

    private suspend fun verificarCandidatura(uid: String) {
        try {
            val doc = db.collection("trabalhos")
                .document(trabalhoId)
                .collection("candidaturas")
                .document(uid)
                .get()
                .await()

            if (doc.exists()) {
                HapticHelper.media(this@MainActivity2)
                jaCandidatou = true
                btnCandidatar.isEnabled = true
                btnCandidatar.text = getString(R.string.trabalho_btn_abrir_chat)
                btnCandidatar.setBackgroundColor(android.graphics.Color.parseColor("#0D226B"))
            } else {
                jaCandidatou = false
            }
        } catch (_: Exception) { }
    }

    private fun abrirChatComCriador() {
        lifecycleScope.launch {
            val chatId = chatRepository.criarOuBuscarChatUmParaUm(
                uidOutro = criadorId,
                nomeOutro = nomeCriador,
                trabalhoId = trabalhoId
            )

            if (chatId != null) {
                val intent = Intent(this@MainActivity2, ChatActivity::class.java)
                intent.putExtra("chatId", chatId)
                startActivity(intent)
            } else {
                Toast.makeText(this@MainActivity2, getString(R.string.trabalho_erro_chat), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun verificarSeMembroDaEmpresa(uid: String) {
        try {
            val doc = db.collection("empresas").document(empresaIdDoTrabalho).get().await()
            val empresa = doc.toObject(com.example.plataformaremota.data.entity.Empresa::class.java)

            if (empresa == null || uid !in empresa.membros) {
                btnCandidatar.visibility = View.GONE
                Toast.makeText(
                    this@MainActivity2,
                    getString(R.string.empresa_privada_msg),
                    Toast.LENGTH_LONG
                ).show()
                return
            }

            btnCandidatar.visibility = View.VISIBLE
            verificarCandidatura(uid)
        } catch (e: Exception) {
            btnCandidatar.visibility = View.GONE
        }
    }

    private fun cadastrarSeNoTrabalho() {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrEmpty()) {
            Toast.makeText(this, getString(R.string.trabalho_logado_necessario), Toast.LENGTH_SHORT).show()
            return
        }

        btnCandidatar.isEnabled = false
        btnCandidatar.text = getString(R.string.trabalho_btn_enviando)

        lifecycleScope.launch {
            try {
                val docUsuario = db.collection("usuarios").document(uid).get().await()
                val nomeUsuario = docUsuario.getString("nome") ?: getString(R.string.admin_usuario_padrao)

                val candidatura = hashMapOf(
                    "usuarioId" to uid,
                    "nomeUsuario" to nomeUsuario,
                    "timestamp" to System.currentTimeMillis()
                )

                db.collection("trabalhos")
                    .document(trabalhoId)
                    .collection("candidaturas")
                    .document(uid)
                    .set(candidatura)
                    .await()

                NotificacaoHelper.enviar(
                    uidDestino = criadorId,
                    titulo = getString(R.string.trabalho_notif_nova_titulo),
                    mensagem = getString(R.string.trabalho_notif_nova_msg, nomeUsuario, tituloTrabalho)
                )
                NotificacaoHelper.salvarInApp(
                    uidDestino = criadorId,
                    titulo = getString(R.string.trabalho_notif_nova_titulo),
                    mensagem = getString(R.string.trabalho_notif_nova_msg, nomeUsuario, tituloTrabalho),
                    tipo = "candidatura",
                    refId = trabalhoId
                )

                NotificacaoHelper.enviar(
                    uidDestino = uid,
                    titulo = getString(R.string.trabalho_notif_enviada_titulo),
                    mensagem = getString(R.string.trabalho_notif_enviada_msg, tituloTrabalho)
                )

                jaCandidatou = true
                btnCandidatar.isEnabled = true
                btnCandidatar.text = getString(R.string.trabalho_btn_abrir_chat)
                btnCandidatar.setBackgroundColor(android.graphics.Color.parseColor("#0D226B"))

                Toast.makeText(this@MainActivity2, getString(R.string.trabalho_candidatura_enviada), Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                btnCandidatar.isEnabled = true
                btnCandidatar.text = getString(R.string.trabalho_btn_candidatar)
                Toast.makeText(this@MainActivity2, getString(R.string.geral_erro, e.message ?: ""), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun confirmarExclusao() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.trabalho_excluir_titulo))
            .setMessage(getString(R.string.trabalho_excluir_msg))
            .setPositiveButton(getString(R.string.trabalho_btn_excluir)) { _, _ -> excluirTrabalho() }
            .setNegativeButton(getString(R.string.geral_cancelar), null)
            .show()
    }

    private fun excluirTrabalho() {
        lifecycleScope.launch {
            val candidatos = buscarCandidatos()
            val sucesso = repository.excluir(trabalhoId)

            if (sucesso) {
                notificarCandidatosExclusao(candidatos)
                Toast.makeText(this@MainActivity2, getString(R.string.trabalho_excluido), Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this@MainActivity2, getString(R.string.trabalho_erro_excluir), Toast.LENGTH_LONG).show()
            }
        }
    }

    private suspend fun buscarCandidatos(): List<String> {
        return try {
            val snapshot = db.collection("trabalhos")
                .document(trabalhoId)
                .collection("candidaturas")
                .get()
                .await()

            snapshot.documents.mapNotNull { it.getString("usuarioId") }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun notificarCandidatosExclusao(uids: List<String>) {
        if (uids.isEmpty()) return

        lifecycleScope.launch {
            NotificacaoHelper.enviarParaVarios(
                uidsDestino = uids,
                titulo = getString(R.string.sys_trabalho_encerrado), // Usando a string traduzida
                mensagem = getString(R.string.trabalho_notif_excluido_msg, tituloTrabalho)
            )
        }
    }
}