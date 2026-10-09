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

class AdminEmpresasActivity : BaseActivity() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var rv: RecyclerView
    private val lista = mutableListOf<AdminEmpresa>()

    data class AdminEmpresa(
        val id: String,
        val nome: String,
        val area: String,
        val totalMembros: Int
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!AdminChecker.isAdmin) { finish(); return }

        setContentView(R.layout.activity_admin_lista)

        findViewById<ImageView>(R.id.btnVoltarAdminLista).setOnClickListener { finish() }
        findViewById<TextView>(R.id.txtTituloAdminLista).text = "Empresas"

        rv = findViewById(R.id.rvAdminLista)
        rv.layoutManager = LinearLayoutManager(this)

        carregar()
    }

    private fun carregar() {
        lifecycleScope.launch {
            try {
                val snap = db.collection("empresas").get().await()
                lista.clear()
                snap.documents.forEach { doc ->
                    val membros = (doc.get("membros") as? List<*>)?.size ?: 0
                    lista.add(
                        AdminEmpresa(
                            id = doc.id,
                            nome = doc.getString("nome") ?: "—",
                            area = doc.getString("areaAtuacao") ?: "—",
                            totalMembros = membros
                        )
                    )
                }
                rv.adapter = Adapter(lista)
            } catch (e: Exception) {
                Toast.makeText(this@AdminEmpresasActivity, "Erro: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun confirmarExcluir(e: AdminEmpresa) {
        AlertDialog.Builder(this)
            .setTitle("Excluir empresa")
            .setMessage("Excluir '${e.nome}'?\n\nVai apagar a empresa, o grupo Geral e todas as mensagens.")
            .setPositiveButton("EXCLUIR") { _, _ -> excluir(e) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun excluir(e: AdminEmpresa) {
        lifecycleScope.launch {
            try {
                val grupo = db.collection("chats").document("empresa_${e.id}_geral")
                try {
                    val msgs = grupo.collection("mensagens").get().await()
                    for (m in msgs.documents) m.reference.delete().await()
                    grupo.delete().await()
                } catch (_: Exception) {}

                db.collection("empresas").document(e.id).delete().await()

                Toast.makeText(this@AdminEmpresasActivity, "Empresa excluída", Toast.LENGTH_SHORT).show()
                carregar()
            } catch (ex: Exception) {
                Toast.makeText(this@AdminEmpresasActivity, "Erro: ${ex.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    inner class Adapter(private val itens: List<AdminEmpresa>) :
        RecyclerView.Adapter<Adapter.VH>() {

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val nome: TextView = v.findViewById(R.id.txtAdminItemTitulo)
            val sub: TextView = v.findViewById(R.id.txtAdminItemSub)
            val btnExcluir: ImageView = v.findViewById(R.id.btnAdminItemExcluir)
        }

        override fun onCreateViewHolder(p: ViewGroup, t: Int): VH =
            VH(LayoutInflater.from(p.context).inflate(R.layout.item_admin_lista, p, false))

        override fun onBindViewHolder(h: VH, pos: Int) {
            val e = itens[pos]
            h.nome.text = e.nome
            h.sub.text = "${e.area} • ${e.totalMembros} membros"
            h.btnExcluir.setOnClickListener { confirmarExcluir(e) }
        }

        override fun getItemCount() = itens.size
    }
}