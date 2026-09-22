package com.example.plataformaremota.adapter

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.plataformaremota.R
import com.example.plataformaremota.data.entity.Mensagem
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MensagemAdapter(
    private var mensagens: List<Mensagem>
) : RecyclerView.Adapter<MensagemAdapter.MensagemViewHolder>() {

    class MensagemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val container: LinearLayout = itemView.findViewById(R.id.containerMensagem)
        val bubble: LinearLayout = itemView.findViewById(R.id.bubbleContainer)
        val txtNome: TextView = itemView.findViewById(R.id.txtNomeRemetente)
        val txtTexto: TextView = itemView.findViewById(R.id.txtTextoMensagem)
        val txtHora: TextView = itemView.findViewById(R.id.txtHora)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MensagemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.item_mensagem, parent, false
        )
        return MensagemViewHolder(view)
    }

    override fun onBindViewHolder(holder: MensagemViewHolder, position: Int) {
        val msg = mensagens[position]
        val uidAtual = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        val ehMinha = msg.remetenteId == uidAtual

        holder.txtTexto.text = msg.texto

        // Formata hora
        val horaFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        holder.txtHora.text = horaFormat.format(Date(msg.timestamp))

        if (ehMinha) {
            // Minha mensagem: alinhada à direita, bubble azul
            holder.container.gravity = Gravity.END
            holder.bubble.setBackgroundResource(R.drawable.bubble_minha)
            holder.txtNome.visibility = View.GONE
        } else {
            // Mensagem de outro: alinhada à esquerda, bubble semi-transparente
            holder.container.gravity = Gravity.START
            holder.bubble.setBackgroundResource(R.drawable.bubble_outra)

            // Mostra o nome do remetente (importante em grupos)
            if (msg.nomeRemetente.isNotEmpty()) {
                holder.txtNome.visibility = View.VISIBLE
                holder.txtNome.text = msg.nomeRemetente
            } else {
                holder.txtNome.visibility = View.GONE
            }
        }
    }

    override fun getItemCount(): Int = mensagens.size

    fun atualizarLista(novaLista: List<Mensagem>) {
        mensagens = novaLista
        notifyDataSetChanged()
    }
}