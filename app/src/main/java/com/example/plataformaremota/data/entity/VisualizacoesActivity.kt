package com.example.plataformaremota

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VisualizacoesActivity : BaseActivity() {

    private lateinit var rv: RecyclerView
    private lateinit var layoutVazio: View
    private lateinit var layoutLoading: View

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_visualizacoes)

        rv = findViewById(R.id.rvVisualizacoes)
        layoutVazio = findViewById(R.id.layoutVazioVisualizacoes)
        layoutLoading = findViewById(R.id.layoutLoadingVisualizacoes)

        findViewById<ImageView>(R.id.btnVoltarVisualizacoes).setOnClickListener { finish() }

        rv.layoutManager = LinearLayoutManager(this)
        carregar()

        ThemeManager.aplicarCores(this, findViewById(android.R.id.content))
        ThemeManager.aplicarCoresTexto(this, findViewById(android.R.id.content))
    }

    private fun carregar() {
        val uid = auth.currentUser?.uid ?: return

        lifecycleScope.launch {
            try {
                val snap = db.collection("usuarios").document(uid)
                    .collection("visualizacoes")
                    .get().await()

                val itens = coroutineScope {
                    snap.documents.map { doc ->
                        async {
                            val visualizadorUid = doc.id
                            val ts = doc.getLong("timestamp") ?: 0L
                            try {
                                val u = db.collection("usuarios").document(visualizadorUid)
                                    .get().await()
                                VisualizacaoItem(
                                    uid = visualizadorUid,
                                    nome = u.getString("nome") ?: "Usuário",
                                    username = u.getString("username") ?: "",
                                    fotoUrl = u.getString("fotoUrl") ?: "",
                                    timestamp = ts
                                )
                            } catch (e: Exception) {
                                VisualizacaoItem(visualizadorUid, "Usuário", "", "", ts)
                            }
                        }
                    }.awaitAll()
                }.sortedByDescending { it.timestamp }

                if (itens.isEmpty()) {
                    rv.visibility = View.GONE
                    layoutVazio.visibility = View.VISIBLE
                } else {
                    rv.adapter = VisualizacaoAdapter(itens)
                    rv.visibility = View.VISIBLE
                    layoutVazio.visibility = View.GONE
                }
            } catch (e: Exception) {
                layoutVazio.visibility = View.VISIBLE
            } finally {
                layoutLoading.visibility = View.GONE
            }
        }
    }

    data class VisualizacaoItem(
        val uid: String,
        val nome: String,
        val username: String,
        val fotoUrl: String,
        val timestamp: Long
    )

    inner class VisualizacaoAdapter(private val itens: List<VisualizacaoItem>) :
        RecyclerView.Adapter<VisualizacaoAdapter.VH>() {

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val img: ImageView = v.findViewById(R.id.imgFotoVisualizador)
            val nome: TextView = v.findViewById(R.id.txtNomeVisualizador)
            val username: TextView = v.findViewById(R.id.txtUsernameVisualizador)
            val data: TextView = v.findViewById(R.id.txtDataVisualizacao)
        }

        override fun onCreateViewHolder(p: ViewGroup, t: Int): VH {
            return VH(LayoutInflater.from(p.context).inflate(R.layout.item_visualizacao, p, false))
        }

        override fun onBindViewHolder(h: VH, pos: Int) {
            val item = itens[pos]

            h.nome.text = item.nome
            h.username.text = if (item.username.isNotEmpty()) "@${item.username}" else ""
            h.data.text = formatarTempo(item.timestamp)

            if (item.fotoUrl.isNotEmpty()) {
                Glide.with(h.itemView.context).load(item.fotoUrl).circleCrop().into(h.img)
                h.img.imageTintList = null
                h.img.setPadding(0, 0, 0, 0)
            }

            h.itemView.setOnClickListener {
                val intent = Intent(this@VisualizacoesActivity, PerfilUsuarioActivity::class.java)
                intent.putExtra("uidUsuario", item.uid)
                intent.putExtra("chatId", "")
                startActivity(intent)
            }
        }

        override fun getItemCount() = itens.size
    }

    private fun formatarTempo(ts: Long): String {
        val diff = System.currentTimeMillis() - ts
        return when {
            diff < 60_000 -> "agora mesmo"
            diff < 3_600_000 -> "${diff / 60_000} min atrás"
            diff < 86_400_000 -> "${diff / 3_600_000}h atrás"
            diff < 604_800_000 -> "${diff / 86_400_000}d atrás"
            else -> SimpleDateFormat("dd/MM 'às' HH:mm", Locale.getDefault()).format(Date(ts))
        }
    }
}