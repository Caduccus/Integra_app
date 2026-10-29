package com.example.plataformaremota.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.plataformaremota.R

data class UsuarioChat(
    val uid: String,
    val nome: String,
    val username: String,
    val fotoUrl: String
)

class UsuarioChatAdapter(
    private var usuarios: List<UsuarioChat>,
    private val onClick: (UsuarioChat) -> Unit
) : RecyclerView.Adapter<UsuarioChatAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val img: ImageView = v.findViewById(R.id.imgUsuario)
        val txtNome: TextView = v.findViewById(R.id.txtNomeUsuario)
        val txtUsername: TextView = v.findViewById(R.id.txtUsernameUsuario)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return VH(LayoutInflater.from(parent.context).inflate(R.layout.item_selecionar_usuario, parent, false))
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val u = usuarios[position]
        holder.txtNome.text = u.nome
        holder.txtUsername.text = if (u.username.isNotEmpty()) "@${u.username}" else ""

        if (u.fotoUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context).load(u.fotoUrl).circleCrop().into(holder.img)
            holder.img.imageTintList = null
            holder.img.setPadding(0, 0, 0, 0)
        } else {
            holder.img.setImageResource(R.drawable.ic_person)
        }

        // Como estamos reutilizando o layout de selecionar usuário, escondemos o checkbox
        val check = holder.itemView.findViewById<View>(R.id.checkUsuario)
        check.visibility = View.GONE

        holder.itemView.setOnClickListener { onClick(u) }
    }

    override fun getItemCount() = usuarios.size

    fun atualizarLista(novaLista: List<UsuarioChat>) {
        usuarios = novaLista
        notifyDataSetChanged()
    }
}