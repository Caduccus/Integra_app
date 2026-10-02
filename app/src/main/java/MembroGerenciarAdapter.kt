package com.example.plataformaremota.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.plataformaremota.R

data class MembroGerenciar(
    val uid: String,
    val nome: String,
    val username: String,
    val fotoUrl: String = "",
    val ehAdmin: Boolean = false,
    val ehDono: Boolean = false
)

class MembroGerenciarAdapter(
    private var membros: List<MembroGerenciar>,
    private val onAcoes: (MembroGerenciar) -> Unit
) : RecyclerView.Adapter<MembroGerenciarAdapter.MembroViewHolder>() {

    class MembroViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val img: ImageView = itemView.findViewById(R.id.imgMembroGerenciar)
        val txtNome: TextView = itemView.findViewById(R.id.txtNomeMembroGerenciar)
        val txtUsername: TextView = itemView.findViewById(R.id.txtUsernameMembroGerenciar)
        val txtBadgeDono: TextView = itemView.findViewById(R.id.txtBadgeDono)
        val txtBadgeAdmin: TextView = itemView.findViewById(R.id.txtBadgeAdminGerenciar)
        val btnAcoes: ImageView = itemView.findViewById(R.id.btnAcoesMembro)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MembroViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.item_membro_gerenciar, parent, false
        )
        return MembroViewHolder(view)
    }

    override fun onBindViewHolder(holder: MembroViewHolder, position: Int) {
        val m = membros[position]

        holder.txtNome.text = m.nome
        holder.txtUsername.text = if (m.username.isNotEmpty()) "@${m.username}" else ""

        holder.txtBadgeDono.visibility = if (m.ehDono) View.VISIBLE else View.GONE
        holder.txtBadgeAdmin.visibility = if (m.ehAdmin && !m.ehDono) View.VISIBLE else View.GONE

        if (m.fotoUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(m.fotoUrl).circleCrop().into(holder.img)
            holder.img.imageTintList = null
            holder.img.setPadding(0, 0, 0, 0)
        }

        holder.btnAcoes.setOnClickListener { onAcoes(m) }
    }

    override fun getItemCount(): Int = membros.size

    fun atualizarLista(novaLista: List<MembroGerenciar>) {
        membros = novaLista
        notifyDataSetChanged()
    }
}