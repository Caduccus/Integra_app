package com.example.plataformaremota.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.plataformaremota.R
import com.example.plataformaremota.data.entity.Empresa
import com.google.android.material.card.MaterialCardView

class EmpresaAdapter(
    private var empresas: List<Empresa>,
    private val onCardClick: (Empresa) -> Unit,
    private val onBotaoClick: (Empresa) -> Unit,
    private val textoBotao: (Empresa) -> String,
    private val corBotao: (Empresa) -> Int
) : RecyclerView.Adapter<EmpresaAdapter.EmpresaViewHolder>() {

    class EmpresaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val card: MaterialCardView = itemView.findViewById(R.id.cardEmpresa)
        val imgLogo: ImageView = itemView.findViewById(R.id.imgLogoEmpresaItem)
        val txtNome: TextView = itemView.findViewById(R.id.txtNomeEmpresaItem)
        val txtArea: TextView = itemView.findViewById(R.id.txtAreaEmpresaItem)
        val txtDescricao: TextView = itemView.findViewById(R.id.txtDescricaoEmpresaItem)
        val btnAcao: com.google.android.material.button.MaterialButton =
            itemView.findViewById(R.id.btnAcaoEmpresa)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmpresaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.item_empresa, parent, false
        )
        return EmpresaViewHolder(view)
    }

    override fun onBindViewHolder(holder: EmpresaViewHolder, position: Int) {
        val empresa = empresas[position]

        holder.txtNome.text = empresa.nome
        holder.txtArea.text = empresa.areaAtuacao
        holder.txtDescricao.text = empresa.descricao

        if (empresa.logoUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(empresa.logoUrl).circleCrop().into(holder.imgLogo)
            holder.imgLogo.imageTintList = null
            holder.imgLogo.setPadding(0, 0, 0, 0)
        } else {
            holder.imgLogo.setImageResource(R.drawable.ic_work_outline)
        }

        val texto = textoBotao(empresa)
        if (texto.isEmpty()) {
            holder.btnAcao.visibility = View.GONE
        } else {
            holder.btnAcao.visibility = View.VISIBLE
            holder.btnAcao.text = texto
            holder.btnAcao.backgroundTintList =
                android.content.res.ColorStateList.valueOf(corBotao(empresa))
            holder.btnAcao.setOnClickListener { onBotaoClick(empresa) }
        }

        holder.card.setOnClickListener { onCardClick(empresa) }
    }

    override fun getItemCount(): Int = empresas.size

    fun atualizarLista(novaLista: List<Empresa>) {
        empresas = novaLista
        notifyDataSetChanged()
    }
}