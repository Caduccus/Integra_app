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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AdminUsuariosActivity : BaseActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var rv: RecyclerView
    private val lista = mutableListOf<AdminUser>()

    data class AdminUser(
        val uid: String,
        val nome: String,
        val email: String,
        val isAdmin: Boolean
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!AdminChecker.isAdmin) { finish(); return }

        setContentView(R.layout.activity_admin_lista)

        findViewById<ImageView>(R.id.btnVoltarAdminLista).setOnClickListener { finish() }
        findViewById<TextView>(R.id.txtTituloAdminLista).text = "Usuários"

        rv = findViewById(R.id.rvAdminLista)
        rv.layoutManager = LinearLayoutManager(this)

        carregar()
    }

    private fun carregar() {
        lifecycleScope.launch {
            try {
                val snap = db.collection("usuarios").get().await()
                lista.clear()
                snap.documents.forEach { doc ->
                    lista.add(
                        AdminUser(
                            uid = doc.id,
                            nome = doc.getString("nome") ?: "Usuário",
                            email = doc.getString("email") ?: "—",
                            isAdmin = doc.getBoolean("isAdmin") ?: false
                        )
                    )
                }
                rv.adapter = Adapter(lista)
            } catch (e: Exception) {
                Toast.makeText(this@AdminUsuariosActivity, "Erro: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun confirmarExcluir(u: AdminUser) {
        AlertDialog.Builder(this)
            .setTitle("Excluir usuário")
            .setMessage(
                "Excluir '${u.nome}'?\n\n" +
                        "Vai apagar:\n" +
                        "• Perfil\n" +
                        "• Trabalhos publicados\n" +
                        "• Candidaturas\n" +
                        "• Empresas criadas\n\n" +
                        "A conta de login permanece (não dá pra apagar pelo app)."
            )
            .setPositiveButton("EXCLUIR") { _, _ -> excluir(u) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun excluir(u: AdminUser) {
        lifecycleScope.launch {
            try {
                // Trabalhos do usuário
                val trabalhos = db.collection("trabalhos")
                    .whereEqualTo("criadorId", u.uid).get().await()
                for (t in trabalhos.documents) {
                    val cands = t.reference.collection("candidaturas").get().await()
                    for (c in cands.documents) c.reference.delete().await()
                    t.reference.delete().await()
                }

                // Candidaturas do usuário em outros trabalhos
                val todos = db.collection("trabalhos").get().await()
                for (t in todos.documents) {
                    try { t.reference.collection("candidaturas").document(u.uid).delete().await() } catch (_: Exception) {}
                }

                // Empresas do usuário
                val empresas = db.collection("empresas")
                    .whereEqualTo("criadorId", u.uid).get().await()
                for (e in empresas.documents) {
                    // apaga grupo geral
                    val grupo = db.collection("chats").document("empresa_${e.id}_geral")
                    try {
                        val msgs = grupo.collection("mensagens").get().await()
                        for (m in msgs.documents) m.reference.delete().await()
                        grupo.delete().await()
                    } catch (_: Exception) {}
                    e.reference.delete().await()
                }

                // Remove de chats (participantes)
                val chats = db.collection("chats")
                    .whereArrayContains("participantes", u.uid).get().await()
                for (c in chats.documents) {
                    c.reference.update("participantes",
                        com.google.firebase.firestore.FieldValue.arrayRemove(u.uid)).await()
                }

                // Deleta o doc do usuário
                db.collection("usuarios").document(u.uid).delete().await()

                Toast.makeText(this@AdminUsuariosActivity, "Usuário excluído", Toast.LENGTH_SHORT).show()
                carregar()
            } catch (e: Exception) {
                Toast.makeText(this@AdminUsuariosActivity, "Erro: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    inner class Adapter(private val itens: List<AdminUser>) :
        RecyclerView.Adapter<Adapter.VH>() {

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val nome: TextView = v.findViewById(R.id.txtAdminItemTitulo)
            val sub: TextView = v.findViewById(R.id.txtAdminItemSub)
            val btnExcluir: ImageView = v.findViewById(R.id.btnAdminItemExcluir)
        }

        override fun onCreateViewHolder(p: ViewGroup, t: Int): VH =
            VH(LayoutInflater.from(p.context).inflate(R.layout.item_admin_lista, p, false))

        override fun onBindViewHolder(h: VH, pos: Int) {
            val u = itens[pos]
            h.nome.text = u.nome + if (u.isAdmin) "  🛡️" else ""
            h.sub.text = u.email

            val euMesmo = u.uid == auth.currentUser?.uid
            h.btnExcluir.visibility = if (euMesmo) View.INVISIBLE else View.VISIBLE
            h.btnExcluir.setOnClickListener { confirmarExcluir(u) }
        }

        override fun getItemCount() = itens.size
    }
}