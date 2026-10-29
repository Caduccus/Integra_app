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

data class SolicitacaoItem(
    val uid: String,
    val nome: String,
    val username: String,
    val fotoUrl: String
)

class SolicitacaoAmigoAdapter(
    private var solicitacoes: List<SolicitacaoItem>,
    private val onAceitar: (SolicitacaoItem) -> Unit,
    private val onRecusar: (SolicitacaoItem) -> Unit
) : RecyclerView.Adapter<SolicitacaoAmigoAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val img: ImageView = v.findViewById(R.id.imgSolicitante)
        val txtNome: TextView = v.findViewById(R.id.txtNomeSolicitante)
        val txtUsername: TextView = v.findViewById(R.id.txtUsernameSolicitante)
        val btnAceitar: MaterialButton = v.findViewById(R.id.btnAceitarSolicitacao)
        val btnRecusar: MaterialButton = v.findViewById(R.id.btnRecusarSolicitacao)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return VH(LayoutInflater.from(parent.context).inflate(R.layout.item_solicitacao_amigo, parent, false))
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val s = solicitacoes[position]
        holder.txtNome.text = s.nome
        holder.txtUsername.text = if (s.username.isNotEmpty()) "@${s.username}" else ""

        if (s.fotoUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context).load(s.fotoUrl).circleCrop().into(holder.img)
            holder.img.imageTintList = null
            holder.img.setPadding(0, 0, 0, 0)
        } else {
            holder.img.setImageResource(R.drawable.ic_person)
        }

        holder.btnAceitar.setOnClickListener { onAceitar(s) }
        holder.btnRecusar.setOnClickListener { onRecusar(s) }
    }

    override fun getItemCount() = solicitacoes.size

    fun atualizarLista(novaLista: List<SolicitacaoItem>) {
        solicitacoes = novaLista
        notifyDataSetChanged()
    }
}