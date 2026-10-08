package com.example.plataformaremota

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.plataformaremota.adapter.CandidaturaAdapter
import com.example.plataformaremota.adapter.CandidaturaItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MinhasCandidaturasActivity : BaseActivity() {

    private lateinit var btnVoltar: ImageView
    private lateinit var rvCandidaturas: RecyclerView
    private lateinit var layoutVazio: View
    private lateinit var layoutLoading: View

    private lateinit var adapter: CandidaturaAdapter

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_minhas_candidaturas)

        btnVoltar = findViewById(R.id.btnVoltarCandidaturas)
        rvCandidaturas = findViewById(R.id.rvCandidaturas)
        layoutVazio = findViewById(R.id.layoutVazioCandidaturas)
        layoutLoading = findViewById(R.id.layoutLoadingCandidaturas)

        adapter = CandidaturaAdapter(
            itens = emptyList(),
            onClick = { item ->
                val intent = Intent(this, MainActivity2::class.java)
                intent.putExtra("trabalhoId", item.trabalhoId)
                startActivity(intent)
            },
            onAvaliar = { item -> avaliarEmpresa(item) }
        )

        rvCandidaturas.layoutManager = LinearLayoutManager(this)
        rvCandidaturas.adapter = adapter

        btnVoltar.setOnClickListener { finish() }

        carregar()

        ThemeManager.aplicarCores(this, findViewById(android.R.id.content))
        ThemeManager.aplicarCoresTexto(this, findViewById(android.R.id.content))
    }

    private fun avaliarEmpresa(item: CandidaturaItem) {
        AvaliarHelper.abrirDialog(
            context = this,
            alvoId = item.empresaId,
            alvoTipo = "empresa",
            alvoNome = item.empresaNome.ifEmpty { "a empresa" },
            trabalhoId = item.trabalhoId,
            trabalhoTitulo = item.tituloTrabalho
        )
    }

    private fun carregar() {
        val uid = auth.currentUser?.uid ?: return

        lifecycleScope.launch {
            try {
                val trabalhosSnap = db.collection("trabalhos").get().await()

                val itens = coroutineScope {
                    trabalhosSnap.documents.map { trabalhoDoc ->
                        async {
                            try {
                                val cand = trabalhoDoc.reference
                                    .collection("candidaturas")
                                    .document(uid)
                                    .get()
                                    .await()

                                if (!cand.exists()) return@async null

                                val status = cand.getString("status")?.lowercase() ?: "pendente"
                                val ts = cand.getLong("timestamp") ?: 0L

                                CandidaturaItem(
                                    trabalhoId = trabalhoDoc.id,
                                    tituloTrabalho = trabalhoDoc.getString("titulo") ?: "Sem título",
                                    empresaNome = trabalhoDoc.getString("empresaNome") ?: "",
                                    empresaId = trabalhoDoc.getString("empresaId") ?: "",
                                    statusCandidatura = status,
                                    timestamp = ts
                                )
                            } catch (e: Exception) {
                                null
                            }
                        }
                    }.awaitAll().filterNotNull()
                }.sortedByDescending { it.timestamp }

                adapter.atualizarLista(itens)

                if (itens.isEmpty()) {
                    rvCandidaturas.visibility = View.GONE
                    layoutVazio.visibility = View.VISIBLE
                } else {
                    rvCandidaturas.visibility = View.VISIBLE
                    layoutVazio.visibility = View.GONE
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@MinhasCandidaturasActivity,
                    "Erro: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                layoutVazio.visibility = View.VISIBLE
            } finally {
                layoutLoading.visibility = View.GONE
            }
        }
    }
}