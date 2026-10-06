package com.example.plataformaremota

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificacoesActivity : BaseActivity() {

    private lateinit var rv: RecyclerView
    private lateinit var layoutVazio: View
    private lateinit var layoutLoading: View

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notificacoes)

        rv = findViewById(R.id.rvNotificacoes)
        layoutVazio = findViewById(R.id.layoutVazioNotif)
        layoutLoading = findViewById(R.id.layoutLoadingNotif)

        findViewById<ImageView>(R.id.btnVoltarNotif).setOnClickListener { finish() }

        rv.layoutManager = LinearLayoutManager(this)
        carregar()
    }

    private fun carregar() {
        val uid = auth.currentUser?.uid ?: return

        lifecycleScope.launch {
            try {
                val snap = db.collection("usuarios").document(uid)
                    .collection("notificacoes")
                    .get().await()

                val lista = snap.documents.mapNotNull { doc ->
                    Notif(
                        id = doc.id,
                        titulo = doc.getString("titulo") ?: "",
                        mensagem = doc.getString("mensagem") ?: "",
                        lida = doc.getBoolean("lida") ?: false,
                        timestamp = doc.getLong("timestamp") ?: 0L
                    )
                }.sortedByDescending { it.timestamp }

                if (lista.isEmpty()) {
                    rv.visibility = View.GONE
                    layoutVazio.visibility = View.VISIBLE
                } else {
                    rv.adapter = NotifAdapter(lista)
                    rv.visibility = View.VISIBLE
                    layoutVazio.visibility = View.GONE
                }

                for (n in lista) {
                    if (!n.lida) {
                        db.collection("usuarios").document(uid)
                            .collection("notificacoes").document(n.id)
                            .update("lida", true).await()
                    }
                }
            } catch (e: Exception) {
                layoutVazio.visibility = View.VISIBLE
            } finally {
                layoutLoading.visibility = View.GONE
            }
        }
    }

    data class Notif(
        val id: String,
        val titulo: String,
        val mensagem: String,
        val lida: Boolean,
        val timestamp: Long
    )

    inner class NotifAdapter(private val itens: List<Notif>) :
        RecyclerView.Adapter<NotifAdapter.VH>() {

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val dot: View = v.findViewById(R.id.dotNaoLida)
            val titulo: TextView = v.findViewById(R.id.txtTituloNotif)
            val msg: TextView = v.findViewById(R.id.txtMsgNotif)
            val data: TextView = v.findViewById(R.id.txtDataNotif)
        }

        override fun onCreateViewHolder(p: ViewGroup, t: Int): VH {
            return VH(LayoutInflater.from(p.context).inflate(R.layout.item_notificacao, p, false))
        }

        override fun onBindViewHolder(h: VH, pos: Int) {
            val n = itens[pos]
            h.titulo.text = n.titulo
            h.msg.text = n.mensagem
            h.dot.visibility = if (n.lida) View.INVISIBLE else View.VISIBLE

            val fmt = SimpleDateFormat("dd/MM 'às' HH:mm", Locale.getDefault())
            h.data.text = fmt.format(Date(n.timestamp))
        }

        override fun getItemCount() = itens.size
    }
}