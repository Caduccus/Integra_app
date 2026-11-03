package com.example.plataformaremota

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.plataformaremota.adapter.Amigo
import com.example.plataformaremota.adapter.AmigoAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ListaAmigosActivity : BaseActivity() {

    private lateinit var rvAmigos: RecyclerView
    private lateinit var layoutSemAmigos: View
    private lateinit var adapter: AmigoAdapter

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lista_amigos)

        findViewById<ImageView>(R.id.btnVoltarAmigos).setOnClickListener { finish() }

        rvAmigos = findViewById(R.id.rvAmigos)
        layoutSemAmigos = findViewById(R.id.layoutSemAmigos)

        // Usa o Adapter que está no arquivo separado
        adapter = AmigoAdapter(emptyList()) { amigo ->
            val intent = Intent(this, PerfilUsuarioActivity::class.java)
            intent.putExtra("uidUsuario", amigo.uid)
            startActivity(intent)
        }
        rvAmigos.layoutManager = LinearLayoutManager(this)
        rvAmigos.adapter = adapter

        carregarAmigos()
    }

    private fun carregarAmigos() {
        val meuUid = auth.currentUser?.uid ?: return

        lifecycleScope.launch {
            try {
                val meuDoc = db.collection("usuarios").document(meuUid).get().await()
                val listaUids = (meuDoc.get("amigos") as? List<*>)?.filterIsInstance<String>() ?: emptyList()

                if (listaUids.isEmpty()) {
                    layoutSemAmigos.visibility = View.VISIBLE
                    rvAmigos.visibility = View.GONE
                    return@launch
                }

                val amigos = coroutineScope {
                    listaUids.map { uid ->
                        async {
                            try {
                                val doc = db.collection("usuarios").document(uid).get().await()
                                Amigo(
                                    uid = uid,
                                    nome = doc.getString("nome") ?: "Usuário",
                                    profissao = doc.getString("profissao") ?: "",
                                    fotoUrl = doc.getString("fotoUrl") ?: ""
                                )
                            } catch (e: Exception) {
                                null
                            }
                        }
                    }.awaitAll().filterNotNull()
                }.sortedBy { it.nome.lowercase() }

                adapter.atualizarLista(amigos)
                layoutSemAmigos.visibility = if (amigos.isEmpty()) View.VISIBLE else View.GONE
                rvAmigos.visibility = if (amigos.isEmpty()) View.GONE else View.VISIBLE

            } catch (e: Exception) {
                layoutSemAmigos.visibility = View.VISIBLE
            }
        }
    }
}