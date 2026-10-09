package com.example.plataformaremota

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AdminTrabalhosActivity : BaseActivity() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var rv: RecyclerView
    private val lista = mutableListOf<AdminTrabalho>()

    data class AdminTrabalho(
        val id: String,
        val titulo: String,
        val categoria: String,
        val criador: String
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!AdminChecker.isAdmin) { finish(); return }

        setContentView(R.layout.activity_admin_lista)

        findViewById<ImageView>(R.id.btnVoltarAdminLista).setOnClickListener { finish() }
        findViewById<TextView>(R.id.txtTituloAdminLista).text = "Trabalhos"

        rv = findViewById(R.id.rvAdminLista)
        rv.layoutManager = LinearLayoutManager(this)

        carregar()
    }

    private fun carregar() {
        lifecycleScope.launch {
            try {
                val snap = db.collection("trabalhos").get().await()
                lista.clear()
                snap.documents.forEach { doc ->
                    lista.add(
                        AdminTrabalho(
                            id = doc.id,
                            titulo = doc.getString("titulo") ?: "—",
                            categoria = doc.getString("categoria") ?: "—",
                            criador = doc.getString("nomeCriador") ?: "—"
                        )
                    )
                }
                rv.adapter = Adapter(lista)
            } catch (e: Exception) {
                Toast.makeText(this@AdminTrabalhosActivity, "Erro: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun confirmarExcluir(t: AdminTrabalho) {
        AlertDialog.Builder(this)
            .setTitle("Excluir trabalho")
            .setMessage("Excluir '${t.titulo}'?")
            .setPositiveButton("EXCLUIR") { _, _ -> excluir(t) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun excluir(t: AdminTrabalho) {
        lifecycleScope.launch {
            try {
                val ref = db.collection("trabalhos").document(t.id)
                val cands = ref.collection("candidaturas").get().await()
                for (c in cands.documents) c.reference.delete().await()
                ref.delete().await()

                Toast.makeText(this@AdminTrabalhosActivity, "Trabalho excluído", Toast.LENGTH_SHORT).show()
                carregar()
            } catch (e: Exception) {
                Toast.makeText(this@AdminTrabalhosActivity, "Erro: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    inner class Adapter(private val itens: List<AdminTrabalho>) :
        RecyclerView.Adapter<Adapter.VH>() {

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val nome: TextView = v.findViewById(R.id.txtAdminItemTitulo)
            val sub: TextView = v.findViewById(R.id.txtAdminItemSub)
            val btnExcluir: ImageView = v.findViewById(R.id.btnAdminItemExcluir)
        }

        override fun onCreateViewHolder(p: ViewGroup, t: Int): VH =
            VH(LayoutInflater.from(p.context).inflate(R.layout.item_admin_lista, p, false))

        override fun onBindViewHolder(h: VH, pos: Int) {
            val t = itens[pos]
            h.nome.text = t.titulo
            h.sub.text = "${t.categoria} • por ${t.criador}"
            h.btnExcluir.setOnClickListener { confirmarExcluir(t) }
        }

        override fun getItemCount() = itens.size
    }
}