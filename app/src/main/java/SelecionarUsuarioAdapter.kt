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
import com.google.android.material.checkbox.MaterialCheckBox

data class UsuarioSelecionavel(
    val uid: String,
    val nome: String,
    val username: String,
    val fotoUrl: String = "",
    var selecionado: Boolean = false
)

class SelecionarUsuarioAdapter(
    private var usuarios: List<UsuarioSelecionavel>,
    private val onToggle: (UsuarioSelecionavel) -> Unit
) : RecyclerView.Adapter<SelecionarUsuarioAdapter.UsuarioViewHolder>() {

    class UsuarioViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val card: MaterialCardView = itemView.findViewById(R.id.cardUsuario)
        val check: MaterialCheckBox = itemView.findViewById(R.id.checkUsuario)
        val img: ImageView = itemView.findViewById(R.id.imgUsuario)
        val txtNome: TextView = itemView.findViewById(R.id.txtNomeUsuario)
        val txtUsername: TextView = itemView.findViewById(R.id.txtUsernameUsuario)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UsuarioViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.item_selecionar_usuario, parent, false
        )
        return UsuarioViewHolder(view)
    }

    override fun onBindViewHolder(holder: UsuarioViewHolder, position: Int) {
        val u = usuarios[position]

        holder.txtNome.text = u.nome
        holder.txtUsername.text = if (u.username.isNotEmpty()) "@${u.username}" else ""
        holder.check.isChecked = u.selecionado

        if (u.fotoUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(u.fotoUrl).circleCrop().into(holder.img)
            holder.img.imageTintList = null
            holder.img.setPadding(0, 0, 0, 0)
        } else {
            holder.img.setImageResource(R.drawable.ic_person)
        }

        holder.card.setOnClickListener {
            u.selecionado = !u.selecionado
            holder.check.isChecked = u.selecionado
            onToggle(u)
        }
    }

    override fun getItemCount(): Int = usuarios.size

    fun atualizarLista(novaLista: List<UsuarioSelecionavel>) {
        usuarios = novaLista
        notifyDataSetChanged()
    }
}