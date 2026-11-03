package com.example.plataformaremota.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.plataformaremota.R

data class Amigo(
    val uid: String,
    val nome: String,
    val profissao: String,
    val fotoUrl: String
)

class AmigoAdapter(
    private var lista: List<Amigo>,
    private val onClick: (Amigo) -> Unit
) : RecyclerView.Adapter<AmigoAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        // ⭐ Esses são os IDs corretos que estão no item_amigo.xml
        val img: ImageView = v.findViewById(R.id.imgFotoAmigo)
        val nome: TextView = v.findViewById(R.id.txtNomeAmigo)
        val profissao: TextView = v.findViewById(R.id.txtProfissaoAmigo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return VH(LayoutInflater.from(parent.context).inflate(R.layout.item_amigo, parent, false))
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val amigo = lista[position]

        holder.nome.text = amigo.nome
        holder.profissao.text = amigo.profissao.ifEmpty { "—" }

        if (amigo.fotoUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(amigo.fotoUrl).circleCrop().into(holder.img)
            holder.img.imageTintList = null
            holder.img.setPadding(0, 0, 0, 0)
        }

        holder.itemView.setOnClickListener { onClick(amigo) }
    }

    override fun getItemCount() = lista.size

    fun atualizarLista(nova: List<Amigo>) {
        lista = nova
        notifyDataSetChanged()
    }
}