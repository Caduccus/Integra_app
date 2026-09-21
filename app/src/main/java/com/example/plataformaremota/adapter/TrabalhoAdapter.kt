package com.example.plataformaremota.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.plataformaremota.R
import com.example.plataformaremota.data.entity.Trabalho
import com.google.android.material.card.MaterialCardView

class TrabalhoAdapter(
    private var trabalhos: List<Trabalho>,
    private val onClick: (Trabalho) -> Unit
) : RecyclerView.Adapter<TrabalhoAdapter.TrabalhoViewHolder>() {

    class TrabalhoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val card: MaterialCardView = itemView.findViewById(R.id.cardTrabalho)
        val txtTitulo: TextView = itemView.findViewById(R.id.txtTitulo)
        val txtCategoria: TextView = itemView.findViewById(R.id.txtCategoria)
        val txtDescricao: TextView = itemView.findViewById(R.id.txtDescricao)
        val txtPrazo: TextView = itemView.findViewById(R.id.txtPrazo)
        val txtNivel: TextView = itemView.findViewById(R.id.txtNivel)
        val txtTipoContrato: TextView = itemView.findViewById(R.id.txtTipoContrato)
        val cardTipoContrato: MaterialCardView = itemView.findViewById(R.id.cardTipoContrato)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrabalhoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.item_trabalho, parent, false
        )
        return TrabalhoViewHolder(view)
    }

    override fun onBindViewHolder(holder: TrabalhoViewHolder, position: Int) {
        val trabalho = trabalhos[position]

        holder.txtTitulo.text = trabalho.titulo
        holder.txtCategoria.text = trabalho.categoria
        holder.txtDescricao.text = trabalho.descricao
        holder.txtPrazo.text = trabalho.prazo
        holder.txtNivel.text = trabalho.nivel

        // Chip tipo contrato com cor dinâmica
        holder.txtTipoContrato.text = trabalho.tipoContrato
        val corChip = when (trabalho.tipoContrato.lowercase()) {
            "clt" -> Color.parseColor("#1E88E5")
            "pj" -> Color.parseColor("#43A047")
            "freelance" -> Color.parseColor("#FB8C00")
            "estágio", "estagio" -> Color.parseColor("#8E24AA")
            "temporário", "temporario" -> Color.parseColor("#00897B")
            else -> Color.parseColor("#757575")
        }
        holder.cardTipoContrato.setCardBackgroundColor(corChip)

        // Cor do nível
        val corNivel = when (trabalho.nivel.lowercase()) {
            "júnior", "junior" -> "#1E88E5"
            "pleno" -> "#8E24AA"
            "sênior", "senior" -> "#F9A825"
            "especialista" -> "#D32F2F"
            else -> "#333333"
        }
        holder.txtNivel.setTextColor(Color.parseColor(corNivel))

        holder.card.setOnClickListener {
            onClick(trabalho)
        }
    }

    override fun getItemCount(): Int = trabalhos.size

    fun atualizarLista(novaLista: List<Trabalho>) {
        trabalhos = novaLista
        notifyDataSetChanged()
    }
}