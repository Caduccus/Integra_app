package com.example.plataformaremota.adapter

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.plataformaremota.R
import com.example.plataformaremota.data.entity.Trabalho
import com.google.android.material.card.MaterialCardView

class TrabalhoAdapter(
    private var trabalhos: List<Trabalho>,
    private val onClick: (Trabalho) -> Unit,
    private val mostrarFavorito: Boolean = false,
    private var favoritos: Set<String> = emptySet(),
    private val onFavoritarClick: (Trabalho) -> Unit = {}
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
        val btnFavorito: ImageView = itemView.findViewById(R.id.btnFavorito)
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

        if (trabalho.tipoContrato.isNotEmpty()) {
            holder.cardTipoContrato.visibility = View.VISIBLE
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
        } else {
            holder.cardTipoContrato.visibility = View.GONE
        }

        val corNivel = when (trabalho.nivel.lowercase()) {
            "júnior", "junior" -> "#1E88E5"
            "pleno" -> "#8E24AA"
            "sênior", "senior" -> "#F9A825"
            "especialista" -> "#D32F2F"
            else -> "#333333"
        }
        holder.txtNivel.setTextColor(Color.parseColor(corNivel))

        // ⭐ Favorito
        if (mostrarFavorito) {
            holder.btnFavorito.visibility = View.VISIBLE
            val ehFavorito = trabalho.id in favoritos

            if (ehFavorito) {
                holder.btnFavorito.setImageResource(R.drawable.ic_heart_filled)
                holder.btnFavorito.imageTintList = ColorStateList.valueOf(
                    Color.parseColor("#E53935")
                )
            } else {
                holder.btnFavorito.setImageResource(R.drawable.ic_heart_outline)
                holder.btnFavorito.imageTintList = ColorStateList.valueOf(
                    Color.parseColor("#B0B0B0")
                )
            }

            holder.btnFavorito.setOnClickListener {
                animarCoracao(it)
                onFavoritarClick(trabalho)
            }
        } else {
            holder.btnFavorito.visibility = View.GONE
        }

        holder.card.setOnClickListener {
            onClick(trabalho)
        }
    }

    private fun animarCoracao(view: View) {
        view.animate().cancel()
        view.scaleX = 0.7f
        view.scaleY = 0.7f
        view.animate()
            .scaleX(1.15f)
            .scaleY(1.15f)
            .setDuration(150)
            .withEndAction {
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(120)
                    .setInterpolator(OvershootInterpolator())
                    .start()
            }
            .start()
    }

    override fun getItemCount(): Int = trabalhos.size

    fun atualizarLista(novaLista: List<Trabalho>) {
        trabalhos = novaLista
        notifyDataSetChanged()
    }

    fun atualizarFavoritos(novosFavoritos: Set<String>) {
        favoritos = novosFavoritos
        notifyDataSetChanged()
    }
}