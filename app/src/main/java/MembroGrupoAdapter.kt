package com.example.plataformaremota.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.plataformaremota.R
import com.google.android.material.card.MaterialCardView

data class Membro(
    val uid: String,
    val nome: String,
    val username: String,
    val fotoUrl: String = "",
    val ehAdmin: Boolean = false
)

class MembroGrupoAdapter(
    private var membros: List<Membro>,
    private val onClick: (Membro) -> Unit
) : RecyclerView.Adapter<MembroGrupoAdapter.MembroViewHolder>() {

    class MembroViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val card: MaterialCardView = itemView.findViewById(R.id.cardMembro)
        val img: ImageView = itemView.findViewById(R.id.imgMembro)
        val txtNome: TextView = itemView.findViewById(R.id.txtNomeMembro)
        val txtUsername: TextView = itemView.findViewById(R.id.txtUsernameMembro)
        val txtAdmin: TextView = itemView.findViewById(R.id.txtAdminBadge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MembroViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.item_membro_grupo, parent, false
        )
        return MembroViewHolder(view)
    }

    override fun onBindViewHolder(holder: MembroViewHolder, position: Int) {
        val m = membros[position]

        holder.txtNome.text = m.nome
        holder.txtUsername.text = if (m.username.isNotEmpty()) "@${m.username}" else ""
        holder.txtAdmin.visibility = if (m.ehAdmin) View.VISIBLE else View.GONE

        if (m.fotoUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(m.fotoUrl).circleCrop().into(holder.img)
            holder.img.imageTintList = null
            holder.img.setPadding(0, 0, 0, 0)
        }

        holder.card.setOnClickListener { onClick(m) }
    }

    override fun getItemCount(): Int = membros.size

    fun atualizarLista(novaLista: List<Membro>) {
        membros = novaLista
        notifyDataSetChanged()
    }
}