package com.example.plataformaremota.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.plataformaremota.R
import com.example.plataformaremota.data.entity.Chat
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ChatAdapter(
    private var chats: List<Chat>,
    private val nomeDoChat: (Chat) -> String,
    private val fotoDoChat: (Chat) -> String,
    private val contextoDoChat: (Chat) -> String,
    private val onClick: (Chat) -> Unit
) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val card: MaterialCardView = itemView.findViewById(R.id.cardChat)
        val imgFoto: ImageView = itemView.findViewById(R.id.imgFotoChat)
        val txtNome: TextView = itemView.findViewById(R.id.txtNomeChat)
        val txtHora: TextView = itemView.findViewById(R.id.txtHoraChat)
        val txtContexto: TextView = itemView.findViewById(R.id.txtContextoChat)
        val txtUltima: TextView = itemView.findViewById(R.id.txtUltimaMensagem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.item_chat, parent, false
        )
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val chat = chats[position]

        holder.txtNome.text = nomeDoChat(chat)
        holder.txtHora.text = formatarHora(chat.timestamp)

        val contexto = contextoDoChat(chat)
        if (contexto.isNotEmpty()) {
            holder.txtContexto.visibility = View.VISIBLE
            holder.txtContexto.text = contexto
        } else {
            holder.txtContexto.visibility = View.GONE
        }

        holder.txtUltima.text = if (chat.ultimaMensagem.isEmpty()) {
            "Nenhuma mensagem ainda"
        } else {
            val prefixo = if (chat.ultimaMensagemRemetente.isNotEmpty() && chat.ehGrupo)
                "${chat.ultimaMensagemRemetente}: " else ""
            "$prefixo${chat.ultimaMensagem}"
        }

        val fotoUrl = fotoDoChat(chat)
        if (fotoUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(fotoUrl)
                .circleCrop()
                .into(holder.imgFoto)
        } else {
            holder.imgFoto.setImageResource(R.drawable.ic_person)
        }

        holder.card.setOnClickListener { onClick(chat) }
    }

    private fun formatarHora(timestamp: Long): String {
        if (timestamp == 0L) return ""
        val agora = Calendar.getInstance()
        val data = Calendar.getInstance().apply { timeInMillis = timestamp }

        return if (agora.get(Calendar.YEAR) == data.get(Calendar.YEAR)
            && agora.get(Calendar.DAY_OF_YEAR) == data.get(Calendar.DAY_OF_YEAR)
        ) {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
        } else {
            SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(timestamp))
        }
    }

    override fun getItemCount(): Int = chats.size

    fun atualizarLista(novaLista: List<Chat>) {
        chats = novaLista
        notifyDataSetChanged()
    }
}