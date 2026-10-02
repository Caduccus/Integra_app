package com.example.plataformaremota.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.plataformaremota.R
import com.google.android.material.button.MaterialButton

data class Pendente(
    val uid: String,
    val nome: String,
    val username: String,
    val fotoUrl: String = ""
)

class PendenteAdapter(
    private var pendentes: List<Pendente>,
    private val onAprovar: (Pendente) -> Unit,
    private val onRejeitar: (Pendente) -> Unit
) : RecyclerView.Adapter<PendenteAdapter.PendenteViewHolder>() {

    class PendenteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val img: ImageView = itemView.findViewById(R.id.imgPendente)
        val txtNome: TextView = itemView.findViewById(R.id.txtNomePendente)
        val txtUsername: TextView = itemView.findViewById(R.id.txtUsernamePendente)
        val btnAprovar: MaterialButton = itemView.findViewById(R.id.btnAprovarPendente)
        val btnRejeitar: MaterialButton = itemView.findViewById(R.id.btnRejeitarPendente)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PendenteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.item_pendente, parent, false
        )
        return PendenteViewHolder(view)
    }

    override fun onBindViewHolder(holder: PendenteViewHolder, position: Int) {
        val p = pendentes[position]

        holder.txtNome.text = p.nome
        holder.txtUsername.text = if (p.username.isNotEmpty()) "@${p.username}" else ""

        if (p.fotoUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(p.fotoUrl).circleCrop().into(holder.img)
            holder.img.imageTintList = null
            holder.img.setPadding(0, 0, 0, 0)
        }

        holder.btnAprovar.setOnClickListener { onAprovar(p) }
        holder.btnRejeitar.setOnClickListener { onRejeitar(p) }
    }

    override fun getItemCount(): Int = pendentes.size

    fun atualizarLista(novaLista: List<Pendente>) {
        pendentes = novaLista
        notifyDataSetChanged()
    }
}