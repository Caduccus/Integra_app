package com.example.plataformaremota

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.plataformaremota.adapter.Candidato
import com.example.plataformaremota.adapter.CandidatoAdapter
import com.example.plataformaremota.data.repository.ChatRepository
import com.example.plataformaremota.data.repository.EmpresaRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class CandidatosActivity : BaseActivity() {

    private lateinit var btnVoltar: ImageView
    private lateinit var rvCandidatos: RecyclerView
    private lateinit var layoutEstadoVazio: View
    private lateinit var layoutLoading: View

    private lateinit var adapter: CandidatoAdapter
    private lateinit var chatRepository: ChatRepository
    private lateinit var empresaRepository: EmpresaRepository

    private val db = FirebaseFirestore.getInstance()
    private var trabalhoId: String = ""
    private var tituloTrabalho: String = ""
    private var empresaIdDoTrabalho: String = "" // ⭐ Guarda o ID da empresa se o trabalho for privado

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_candidatos)

        chatRepository = ChatRepository()
        empresaRepository = EmpresaRepository()

        btnVoltar = findViewById(R.id.btnVoltarCandidatos)
        rvCandidatos = findViewById(R.id.rvCandidatos)
        layoutEstadoVazio = findViewById(R.id.layoutEstadoVazioCandidatos)
        layoutLoading = findViewById(R.id.layoutLoadingCandidatos)

        trabalhoId = intent.getStringExtra("trabalhoId") ?: ""

        if (trabalhoId.isEmpty()) {
            Toast.makeText(this, getString(R.string.candidatos_erro_trabalho), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        configurarRecyclerView()
        btnVoltar.setOnClickListener { finish() }
        carregarCandidatos()
    }

    private fun configurarRecyclerView() {
        adapter = CandidatoAdapter(
            candidatos = emptyList(),
            onChatClick = { candidato -> abrirChatComCandidato(candidato) },
            onAceitar = { candidato -> confirmarAceitar(candidato) },
            onRejeitar = { candidato -> confirmarRejeitar(candidato) }
        )
        rvCandidatos.layoutManager = LinearLayoutManager(this)
        rvCandidatos.adapter = adapter
    }

    private fun carregarCandidatos() {
        mostrarLoading()
        lifecycleScope.launch {
            try {
                val docTrabalho = db.collection("trabalhos").document(trabalhoId).get().await()
                tituloTrabalho = docTrabalho.getString("titulo") ?: "um trabalho"
                empresaIdDoTrabalho = docTrabalho.getString("empresaId") ?: ""

                val snapshot = db.collection("trabalhos")
                    .document(trabalhoId)
                    .collection("candidaturas")
                    .get()
                    .await()

                val candidatos = snapshot.documents.mapNotNull { doc ->
                    val uid = doc.getString("usuarioId") ?: doc.id
                    val nome = doc.getString("nomeUsuario") ?: getString(R.string.admin_usuario_padrao)
                    val ts = doc.getLong("timestamp") ?: 0L
                    val status = doc.getString("status") ?: "pendente"
                    Candidato(uid, nome, ts, status)
                }.sortedByDescending { it.timestamp }

                adapter.atualizarLista(candidatos)
                if (candidatos.isEmpty()) mostrarEstadoVazio() else mostrarLista()

                marcarComoTodasVistas(candidatos)
            } catch (e: Exception) {
                Toast.makeText(this@CandidatosActivity, getString(R.string.geral_erro, e.message ?: ""), Toast.LENGTH_SHORT).show()
                mostrarEstadoVazio()
            }
        }
    }

    private suspend fun marcarComoTodasVistas(candidatos: List<Candidato>) {
        val pendentes = candidatos.filter { it.status == "pendente" }
        if (pendentes.isEmpty()) return

        coroutineScope {
            pendentes.map { c ->
                async {
                    try {
                        db.collection("trabalhos")
                            .document(trabalhoId)
                            .collection("candidaturas")
                            .document(c.uid)
                            .update("status", "vista")
                            .await()
                    } catch (_: Exception) { }
                }
            }.awaitAll()
        }

        val atualizado = candidatos.map {
            if (it.status == "pendente") it.copy(status = "vista") else it
        }
        adapter.atualizarLista(atualizado)
    }

    private fun confirmarAceitar(candidato: Candidato) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.candidatos_aceitar_titulo))
            .setMessage(getString(R.string.candidatos_aceitar_msg, candidato.nome, tituloTrabalho))
            .setPositiveButton(getString(R.string.candidatos_btn_aceitar)) { _, _ -> atualizarStatus(candidato, "aceito") }
            .setNegativeButton(getString(R.string.geral_cancelar), null)
            .show()
    }

    private fun confirmarRejeitar(candidato: Candidato) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.candidatos_rejeitar_titulo))
            .setMessage(getString(R.string.candidatos_rejeitar_msg, candidato.nome))
            .setPositiveButton(getString(R.string.candidatos_btn_rejeitar)) { _, _ -> atualizarStatus(candidato, "rejeitado") }
            .setNegativeButton(getString(R.string.geral_cancelar), null)
            .show()
    }

    private fun atualizarStatus(candidato: Candidato, novoStatus: String) {
        lifecycleScope.launch {
            try {
                // 1. Atualiza o status no Firestore
                db.collection("trabalhos")
                    .document(trabalhoId)
                    .collection("candidaturas")
                    .document(candidato.uid)
                    .update("status", novoStatus)
                    .await()

                HapticHelper.media(this@CandidatosActivity)

                // 2. Se foi aceito, faz as automações (Chat e Empresa)
                if (novoStatus == "aceito") {
                    // Cria o chat automaticamente entre o criador e o candidato
                    chatRepository.criarOuBuscarChatUmParaUm(
                        uidOutro = candidato.uid,
                        nomeOutro = candidato.nome,
                        trabalhoId = trabalhoId
                    )

                    // Se o trabalho for de empresa, adiciona o candidato como membro
                    if (empresaIdDoTrabalho.isNotEmpty()) {
                        empresaRepository.adicionarMembroAceito(empresaIdDoTrabalho, candidato.uid)
                    }
                }

                // 3. Prepara e envia as notificações
                val titulo = if (novoStatus == "aceito") getString(R.string.candidatos_aceito_notif_titulo) else getString(R.string.candidatos_rejeitado_notif_titulo)
                val msg = if (novoStatus == "aceito") {
                    getString(R.string.candidatos_aceito_notif_msg, tituloTrabalho)
                } else {
                    getString(R.string.candidatos_rejeitado_notif_msg, tituloTrabalho)
                }

                NotificacaoHelper.enviar(uidDestino = candidato.uid, titulo = titulo, mensagem = msg)
                NotificacaoHelper.salvarInApp(
                    uidDestino = candidato.uid,
                    titulo = titulo,
                    mensagem = msg,
                    tipo = "candidatura",
                    refId = trabalhoId
                )

                Toast.makeText(this@CandidatosActivity, getString(R.string.candidatos_atualizado), Toast.LENGTH_SHORT).show()
                carregarCandidatos()
            } catch (e: Exception) {
                Toast.makeText(this@CandidatosActivity, getString(R.string.geral_erro, e.message ?: ""), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun abrirChatComCandidato(candidato: Candidato) {
        lifecycleScope.launch {
            val chatId = chatRepository.criarOuBuscarChatUmParaUm(
                uidOutro = candidato.uid,
                nomeOutro = candidato.nome,
                trabalhoId = trabalhoId
            )

            if (chatId != null) {
                val intent = Intent(this@CandidatosActivity, ChatActivity::class.java)
                intent.putExtra("chatId", chatId)
                startActivity(intent)
            } else {
                Toast.makeText(this@CandidatosActivity, getString(R.string.chat_erro_chat_nao_encontrado), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun mostrarLoading() {
        layoutLoading.visibility = View.VISIBLE
        rvCandidatos.visibility = View.GONE
        layoutEstadoVazio.visibility = View.GONE
    }

    private fun mostrarLista() {
        layoutLoading.visibility = View.GONE
        rvCandidatos.visibility = View.VISIBLE
        layoutEstadoVazio.visibility = View.GONE
    }

    private fun mostrarEstadoVazio() {
        layoutLoading.visibility = View.GONE
        rvCandidatos.visibility = View.GONE
        layoutEstadoVazio.visibility = View.VISIBLE
    }
}