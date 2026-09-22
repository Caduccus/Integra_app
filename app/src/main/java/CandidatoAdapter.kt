package com.example.plataformaremota.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.plataformaremota.R
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Candidato(
    val uid: String,
    val nome: String,
    val timestamp: Long
)

class CandidatoAdapter(
    private var candidatos: List<Candidato>,
    private val onChatClick: (Candidato) -> Unit
) : RecyclerView.Adapter<CandidatoAdapter.CandidatoViewHolder>() {

    class CandidatoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtNome: TextView = itemView.findViewById(R.id.txtNomeCandidato)
        val txtData: TextView = itemView.findViewById(R.id.txtDataCandidatura)
        val btnChat: MaterialButton = itemView.findViewById(R.id.btnAbrirChatCandidato)
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

        holder.btnChat.setOnClickListener {
            onChatClick(candidato)
        }
    }

    override fun getItemCount(): Int = candidatos.size

    fun atualizarLista(novaLista: List<Candidato>) {
        candidatos = novaLista
        notifyDataSetChanged()
    }
}