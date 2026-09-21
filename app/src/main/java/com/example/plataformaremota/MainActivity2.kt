package com.example.plataformaremota

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.plataformaremota.data.repository.TrabalhoRepository
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity2 : AppCompatActivity() {

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
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private var trabalhoId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)

        repository = TrabalhoRepository(this)

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
            Toast.makeText(this, "Chat em breve", Toast.LENGTH_SHORT).show()
        }
        btnCandidatar.setOnClickListener { cadastrarSeNoTrabalho() }

        carregarTrabalho()
    }

    // ─────────────────────────────────────────────
    // CARREGAR TRABALHO + VERIFICAR PERMISSÃO
    // ─────────────────────────────────────────────
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

            // Preenche os campos
            txtTituloDetalhe.text = trabalho.titulo
            txtDescricaoDetalhe.text = trabalho.descricao
            txtCategoriaDetalhe.text = trabalho.categoria
            txtTipoContratoDetalhe.text = trabalho.tipoContrato
            txtNivelDetalhe.text = trabalho.nivel
            txtPrazoDetalhe.text = trabalho.prazo

            val nomeExibir = trabalho.nomeCriador.ifEmpty { "Usuário" }
            txtCriador.text = "Publicado por: $nomeExibir"

            // ═══ APLICA AS PERMISSÕES ═══
            val uidAtual = auth.currentUser?.uid ?: ""
            val ehCriador = uidAtual == trabalho.criadorId

            if (ehCriador) {
                layoutBotoesCriador.visibility = View.VISIBLE
                btnCandidatar.visibility = View.GONE
            } else {
                layoutBotoesCriador.visibility = View.GONE
                btnCandidatar.visibility = View.VISIBLE

                // ═══ VERIFICA SE JÁ SE CANDIDATOU ═══
                verificarCandidatura(uidAtual)
            }
        }
    }

    // ─────────────────────────────────────────────
    // VERIFICAR SE O USUÁRIO JÁ SE CANDIDATOU
    // ─────────────────────────────────────────────
    private suspend fun verificarCandidatura(uid: String) {
        try {
            val doc = db.collection("trabalhos")
                .document(trabalhoId)
                .collection("candidaturas")
                .document(uid)
                .get()
                .await()

            if (doc.exists()) {
                // Já se candidatou — troca o botão
                btnCandidatar.isEnabled = false
                btnCandidatar.text = "✓ CANDIDATADO"
                btnCandidatar.setBackgroundColor(android.graphics.Color.parseColor("#757575"))
            }
        } catch (e: Exception) {
            // Ignora erro silenciosamente (se não conseguir verificar, deixa o botão normal)
        }
    }

    // ─────────────────────────────────────────────
    // CONFIRMAÇÃO DE EXCLUSÃO
    // ─────────────────────────────────────────────
    private fun confirmarExclusao() {
        AlertDialog.Builder(this)
            .setTitle("Excluir trabalho")
            .setMessage("Tem certeza que quer excluir este trabalho? Essa ação não pode ser desfeita.")
            .setPositiveButton("Excluir") { _, _ ->
                excluirTrabalho()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun excluirTrabalho() {
        lifecycleScope.launch {
            val sucesso = repository.excluir(trabalhoId)

            if (sucesso) {
                Toast.makeText(
                    this@MainActivity2,
                    "Trabalho excluído!",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            } else {
                Toast.makeText(
                    this@MainActivity2,
                    "Erro ao excluir. Tente novamente.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // ─────────────────────────────────────────────
    // CANDIDATAR-SE NO TRABALHO
    // ─────────────────────────────────────────────
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

                // ═══ TROCA O BOTÃO PRA "CANDIDATADO" ═══
                btnCandidatar.isEnabled = false
                btnCandidatar.text = "✓ CANDIDATADO"
                btnCandidatar.setBackgroundColor(android.graphics.Color.parseColor("#757575"))

                Toast.makeText(
                    this@MainActivity2,
                    "Candidatura enviada! O criador entrará em contato.",
                    Toast.LENGTH_LONG
                ).show()

            } catch (e: Exception) {
                btnCandidatar.isEnabled = true
                btnCandidatar.text = "CADASTRAR-SE NO TRABALHO"
                Toast.makeText(
                    this@MainActivity2,
                    "Erro: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}