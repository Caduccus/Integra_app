package com.example.plataformaremota

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.plataformaremota.adapter.Candidato
import com.example.plataformaremota.adapter.CandidatoAdapter
import com.example.plataformaremota.data.repository.ChatRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class CandidatosActivity : AppCompatActivity() {

    private lateinit var btnVoltar: ImageView
    private lateinit var rvCandidatos: RecyclerView
    private lateinit var layoutEstadoVazio: View
    private lateinit var layoutLoading: View

    private lateinit var adapter: CandidatoAdapter
    private lateinit var chatRepository: ChatRepository

    private val db = FirebaseFirestore.getInstance()

    private var trabalhoId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_candidatos)

        chatRepository = ChatRepository()

        btnVoltar = findViewById(R.id.btnVoltarCandidatos)
        rvCandidatos = findViewById(R.id.rvCandidatos)
        layoutEstadoVazio = findViewById(R.id.layoutEstadoVazioCandidatos)
        layoutLoading = findViewById(R.id.layoutLoadingCandidatos)

        trabalhoId = intent.getStringExtra("trabalhoId") ?: ""

        if (trabalhoId.isEmpty()) {
            Toast.makeText(this, "Trabalho não encontrado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        configurarRecyclerView()
        btnVoltar.setOnClickListener { finish() }
        carregarCandidatos()
    }

    private fun configurarRecyclerView() {
        adapter = CandidatoAdapter(emptyList()) { candidato ->
            abrirChatComCandidato(candidato)
        }

        rvCandidatos.layoutManager = LinearLayoutManager(this)
        rvCandidatos.adapter = adapter
    }

    private fun carregarCandidatos() {
        mostrarLoading()

        lifecycleScope.launch {
            try {
                val snapshot = db.collection("trabalhos")
                    .document(trabalhoId)
                    .collection("candidaturas")
                    .get()
                    .await()

                val candidatos = snapshot.documents.mapNotNull { doc ->
                    val uid = doc.getString("usuarioId") ?: return@mapNotNull null
                    val nome = doc.getString("nomeUsuario") ?: "Usuário"
                    val ts = doc.getLong("timestamp") ?: 0L
                    Candidato(uid, nome, ts)
                }.sortedByDescending { it.timestamp }

                adapter.atualizarLista(candidatos)

                if (candidatos.isEmpty()) mostrarEstadoVazio() else mostrarLista()
            } catch (e: Exception) {
                Toast.makeText(this@CandidatosActivity, "Erro: ${e.message}", Toast.LENGTH_SHORT).show()
                mostrarEstadoVazio()
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
                Toast.makeText(this@CandidatosActivity, "Erro ao abrir chat", Toast.LENGTH_SHORT).show()
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