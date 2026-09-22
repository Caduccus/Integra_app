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

    private lateinit var btnVoltar: ImageView
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

    private lateinit var repository: TrabalhoRepository
    private lateinit var chatRepository: ChatRepository

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private var trabalhoId: String = ""
    private var criadorId: String = ""
    private var nomeCriador: String = ""
    private var jaCandidatou: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)

        repository = TrabalhoRepository(this)
        chatRepository = ChatRepository()

        btnVoltar = findViewById(R.id.btnVoltar)
        txtCriador = findViewById(R.id.txtCriador)
        txtTituloDetalhe = findViewById(R.id.txtTituloDetalhe)
        txtDescricaoDetalhe = findViewById(R.id.txtDescricaoDetalhe)
        txtCategoriaDetalhe = findViewById(R.id.txtCategoriaDetalhe)
        txtTipoContratoDetalhe = findViewById(R.id.txtTipoContratoDetalhe)
        txtNivelDetalhe = findViewById(R.id.txtNivelDetalhe)
        txtPrazoDetalhe = findViewById(R.id.txtPrazoDetalhe)

        layoutBotoesCriador = findViewById(R.id.layoutBotoesCriador)
        btnExcluir = findViewById(R.id.btnExcluir)
        btnEditar = findViewById(R.id.btnEditar)
        btnChat = findViewById(R.id.btnChat)
        btnCandidatar = findViewById(R.id.btnCandidatar)

        trabalhoId = intent.getStringExtra("trabalhoId") ?: ""

        btnVoltar.setOnClickListener { finish() }
        btnExcluir.setOnClickListener { confirmarExclusao() }
        btnEditar.setOnClickListener {
            Toast.makeText(this, "Editar em breve", Toast.LENGTH_SHORT).show()
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

    private fun carregarTrabalho() {
        if (trabalhoId.isEmpty()) {
            Toast.makeText(this, "Trabalho não encontrado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        lifecycleScope.launch {
            val trabalho = repository.buscarPorId(trabalhoId)
            if (trabalho == null) {
                Toast.makeText(this@MainActivity2, "Trabalho não encontrado", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }

            criadorId = trabalho.criadorId
            nomeCriador = trabalho.nomeCriador

            txtTituloDetalhe.text = trabalho.titulo
            txtDescricaoDetalhe.text = trabalho.descricao
            txtCategoriaDetalhe.text = trabalho.categoria
            txtTipoContratoDetalhe.text = trabalho.tipoContrato
            txtNivelDetalhe.text = trabalho.nivel
            txtPrazoDetalhe.text = trabalho.prazo

            txtCriador.text = "Publicado por: ${nomeCriador.ifEmpty { "Usuário" }}"

            val uidAtual = auth.currentUser?.uid ?: ""
            val ehCriador = uidAtual == criadorId

            if (ehCriador) {
                layoutBotoesCriador.visibility = View.VISIBLE
                btnCandidatar.visibility = View.GONE
            } else {
                layoutBotoesCriador.visibility = View.GONE
                btnCandidatar.visibility = View.VISIBLE
                verificarCandidatura(uidAtual)
            }
        }
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
                jaCandidatou = true
                btnCandidatar.isEnabled = true
                btnCandidatar.text = "ABRIR CHAT COM O CRIADOR"
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
                Toast.makeText(this@MainActivity2, "Erro ao abrir chat", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun confirmarExclusao() {
        AlertDialog.Builder(this)
            .setTitle("Excluir trabalho")
            .setMessage("Tem certeza que quer excluir este trabalho?")
            .setPositiveButton("Excluir") { _, _ -> excluirTrabalho() }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun excluirTrabalho() {
        lifecycleScope.launch {
            val sucesso = repository.excluir(trabalhoId)
            if (sucesso) {
                Toast.makeText(this@MainActivity2, "Trabalho excluído!", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this@MainActivity2, "Erro ao excluir", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun cadastrarSeNoTrabalho() {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrEmpty()) {
            Toast.makeText(this, "Você precisa estar logado", Toast.LENGTH_SHORT).show()
            return
        }

        btnCandidatar.isEnabled = false
        btnCandidatar.text = "ENVIANDO..."

        lifecycleScope.launch {
            try {
                val docUsuario = db.collection("usuarios").document(uid).get().await()
                val nomeUsuario = docUsuario.getString("nome") ?: "Usuário"

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

                jaCandidatou = true
                btnCandidatar.isEnabled = true
                btnCandidatar.text = "ABRIR CHAT COM O CRIADOR"
                btnCandidatar.setBackgroundColor(android.graphics.Color.parseColor("#0D226B"))

                Toast.makeText(this@MainActivity2, "Candidatura enviada!", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                btnCandidatar.isEnabled = true
                btnCandidatar.text = "CADASTRAR-SE NO TRABALHO"
                Toast.makeText(this@MainActivity2, "Erro: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}