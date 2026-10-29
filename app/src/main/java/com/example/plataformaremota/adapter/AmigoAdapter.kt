package com.example.plataformaremota.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.plataformaremota.R

data class AmigoItem(
    val uid: String,
    val nome: String,
    val username: String,
    val fotoUrl: String
)

class AmigoAdapter(
    private var amigos: List<AmigoItem>,
    private val onChatClick: (AmigoItem) -> Unit
) : RecyclerView.Adapter<AmigoAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val img: ImageView = v.findViewById(R.id.imgAmigo)
        val txtNome: TextView = v.findViewById(R.id.txtNomeAmigo)
        val txtUsername: TextView = v.findViewById(R.id.txtUsernameAmigo)
        val btnChat: ImageView = v.findViewById(R.id.btnMensagemAmigoLista)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return VH(LayoutInflater.from(parent.context).inflate(R.layout.item_amigo, parent, false))
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val a = amigos[position]
        holder.txtNome.text = a.nome
        holder.txtUsername.text = if (a.username.isNotEmpty()) "@${a.username}" else ""

        if (a.fotoUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context).load(a.fotoUrl).circleCrop().into(holder.img)
            holder.img.imageTintList = null
            holder.img.setPadding(0, 0, 0, 0)
        } else {
            holder.img.setImageResource(R.drawable.ic_person)
        }

        holder.btnChat.setOnClickListener { onChatClick(a) }
    }

    override fun getItemCount() = amigos.size

    fun atualizarLista(novaLista: List<AmigoItem>) {
        amigos = novaLista
        notifyDataSetChanged()
    }
}