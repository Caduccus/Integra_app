package com.example.plataformaremota.adapter

import android.content.res.ColorStateList
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

data class CandidaturaItem(
    val trabalhoId: String,
    val tituloTrabalho: String,
    val empresaNome: String,
    val empresaId: String = "",        // ⭐ NOVO
    val statusCandidatura: String,
    val timestamp: Long
)

class CandidaturaAdapter(
    private var itens: List<CandidaturaItem>,
    private val onClick: (CandidaturaItem) -> Unit,
    private val onAvaliar: (CandidaturaItem) -> Unit = {}   // ⭐ NOVO
) : RecyclerView.Adapter<CandidaturaAdapter.CandidaturaViewHolder>() {

    class CandidaturaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val card: MaterialCardView = itemView.findViewById(R.id.cardCandidatura)
        val txtTitulo: TextView = itemView.findViewById(R.id.txtTituloCandidatura)
        val txtEmpresa: TextView = itemView.findViewById(R.id.txtEmpresaCandidatura)
        val linhaEmpresa: View = itemView.findViewById(R.id.linhaEmpresa)
        val cardStatus: MaterialCardView = itemView.findViewById(R.id.cardStatusCandidatura)
        val txtStatus: TextView = itemView.findViewById(R.id.txtStatusCandidatura)
        val step1: View = itemView.findViewById(R.id.step1)
        val line1: View = itemView.findViewById(R.id.line1)
        val step2: View = itemView.findViewById(R.id.step2)
        val line2: View = itemView.findViewById(R.id.line2)
        val step3: View = itemView.findViewById(R.id.step3)
        val txtData: TextView = itemView.findViewById(R.id.txtDataCandidatura)
        val btnAvaliar: MaterialButton = itemView.findViewById(R.id.btnAvaliarEmpresa)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CandidaturaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.item_candidatura, parent, false
        )
        return CandidaturaViewHolder(view)
    }

    override fun onBindViewHolder(holder: CandidaturaViewHolder, position: Int) {
        val item = itens[position]

        holder.txtTitulo.text = item.tituloTrabalho

        if (item.empresaNome.isNotEmpty()) {
            holder.linhaEmpresa.visibility = View.VISIBLE
            holder.txtEmpresa.text = item.empresaNome
        } else {
            holder.linhaEmpresa.visibility = View.GONE
        }

        val (textoChip, corChip) = when (item.statusCandidatura.lowercase()) {
            "aceito", "aceita" -> "Aceita" to Color.parseColor("#43A047")
            "rejeitado", "rejeitada" -> "Rejeitada" to Color.parseColor("#D32F2F")
            "vista" -> "Vista" to Color.parseColor("#1E88E5")
            else -> "Enviada" to Color.parseColor("#FB8C00")
        }

        holder.cardStatus.setCardBackgroundColor(corChip)
        holder.txtStatus.text = textoChip

        val passo2 = item.statusCandidatura.lowercase() in listOf("vista", "aceito", "aceita", "rejeitado", "rejeitada")
        val passo3 = item.statusCandidatura.lowercase() in listOf("aceito", "aceita", "rejeitado", "rejeitada")

        holder.step1.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#43A047"))
        holder.line1.backgroundTintList = ColorStateList.valueOf(
            if (passo2) Color.parseColor("#43A047") else Color.parseColor("#33000000")
        )
        holder.step2.backgroundTintList = ColorStateList.valueOf(
            if (passo2) Color.parseColor("#43A047") else Color.parseColor("#BDBDBD")
        )
        holder.line2.backgroundTintList = ColorStateList.valueOf(
            if (passo3) Color.parseColor("#43A047") else Color.parseColor("#33000000")
        )
        holder.step3.backgroundTintList = ColorStateList.valueOf(
            if (passo3) corChip else Color.parseColor("#BDBDBD")
        )

        holder.txtData.text = formatarData(item.timestamp)

        // ⭐ Botão Avaliar Empresa (só se aceita e tiver empresa)
        val podeAvaliar = item.statusCandidatura.lowercase() in listOf("aceito", "aceita")
                && item.empresaId.isNotEmpty()

        holder.btnAvaliar.visibility = if (podeAvaliar) View.VISIBLE else View.GONE
        holder.btnAvaliar.setOnClickListener { onAvaliar(item) }

        holder.card.setOnClickListener { onClick(item) }
    }

    private fun formatarData(timestamp: Long): String {
        if (timestamp == 0L) return "Data desconhecida"
        val formato = SimpleDateFormat("'Enviada em' dd/MM 'às' HH:mm", Locale.getDefault())
        return formato.format(Date(timestamp))
    }

    override fun getItemCount(): Int = itens.size

    fun atualizarLista(novaLista: List<CandidaturaItem>) {
        itens = novaLista
        notifyDataSetChanged()
    }
}