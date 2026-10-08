package com.example.plataformaremota.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.plataformaremota.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Candidato(
    val uid: String,
    val nome: String,
    val timestamp: Long,
    val status: String = "pendente"
)

class CandidatoAdapter(
    private var candidatos: List<Candidato>,
    private val onChatClick: (Candidato) -> Unit,
    private val onAceitar: (Candidato) -> Unit = {},
    private val onRejeitar: (Candidato) -> Unit = {},
    private val onAvaliar: (Candidato) -> Unit = {}   // ⭐ NOVO
) : RecyclerView.Adapter<CandidatoAdapter.CandidatoViewHolder>() {

    class CandidatoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtNome: TextView = itemView.findViewById(R.id.txtNomeCandidato)
        val txtData: TextView = itemView.findViewById(R.id.txtDataCandidatura)
        val btnChat: MaterialButton = itemView.findViewById(R.id.btnAbrirChatCandidato)
        val cardStatus: MaterialCardView = itemView.findViewById(R.id.cardStatusCandidato)
        val txtStatus: TextView = itemView.findViewById(R.id.txtStatusCandidato)
        val btnAceitar: MaterialButton = itemView.findViewById(R.id.btnAceitarCandidato)
        val btnRejeitar: MaterialButton = itemView.findViewById(R.id.btnRejeitarCandidato)
        val btnAvaliar: MaterialButton = itemView.findViewById(R.id.btnAvaliarCandidato)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CandidatoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.item_candidato, parent, false
        )
        return CandidatoViewHolder(view)
    }

    override fun onBindViewHolder(holder: CandidatoViewHolder, position: Int) {
        val candidato = candidatos[position]

        holder.txtNome.text = candidato.nome

        val formato = SimpleDateFormat("dd/MM 'às' HH:mm", Locale.getDefault())
        holder.txtData.text = "Candidatou-se em ${formato.format(Date(candidato.timestamp))}"

        val (textoChip, corChip) = when (candidato.status.lowercase()) {
            "aceito", "aceita" -> "Aceito" to Color.parseColor("#43A047")
            "rejeitado", "rejeitada" -> "Rejeitado" to Color.parseColor("#D32F2F")
            "vista" -> "Vista" to Color.parseColor("#1E88E5")
            else -> "Pendente" to Color.parseColor("#FB8C00")
        }
        holder.cardStatus.setCardBackgroundColor(corChip)
        holder.txtStatus.text = textoChip

        val decidido = candidato.status.lowercase() in listOf(
            "aceito", "aceita", "rejeitado", "rejeitada"
        )
        val aceito = candidato.status.lowercase() in listOf("aceito", "aceita")

        // Botões aceitar/rejeitar somem se já decidido
        if (decidido) {
            holder.btnAceitar.visibility = View.GONE
            holder.btnRejeitar.visibility = View.GONE
        } else {
            holder.btnAceitar.visibility = View.VISIBLE
            holder.btnRejeitar.visibility = View.VISIBLE
        }

        // ⭐ Botão Avaliar só aparece se aceito
        holder.btnAvaliar.visibility = if (aceito) View.VISIBLE else View.GONE

        holder.btnChat.setOnClickListener { onChatClick(candidato) }
        holder.btnAceitar.setOnClickListener { onAceitar(candidato) }
        holder.btnRejeitar.setOnClickListener { onRejeitar(candidato) }
        holder.btnAvaliar.setOnClickListener { onAvaliar(candidato) }
    }

    override fun getItemCount(): Int = candidatos.size

    fun atualizarLista(novaLista: List<Candidato>) {
        candidatos = novaLista
        notifyDataSetChanged()
    }
}