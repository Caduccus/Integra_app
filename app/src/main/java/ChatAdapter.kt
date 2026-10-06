package com.example.plataformaremota.adapter

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.plataformaremota.R
import com.example.plataformaremota.ThemeManager
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
    private val statusDoChat: (Chat) -> String = { ThemeManager.STATUS_ONLINE },
    private val onClick: (Chat) -> Unit,
    private val onProfileClick: (Chat) -> Unit = {},
    private val onLongClick: (Chat) -> Unit = {}
) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val card: MaterialCardView = itemView.findViewById(R.id.cardChat)
        val containerAvatar: FrameLayout = itemView.findViewById(R.id.containerAvatarChat)
        val imgFoto: ImageView = itemView.findViewById(R.id.imgFotoChat)
        val txtNome: TextView = itemView.findViewById(R.id.txtNomeChat)
        val dotStatus: View = itemView.findViewById(R.id.dotStatusChat)
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

        if (!chat.ehGrupo) {
            holder.dotStatus.visibility = View.VISIBLE
            val cor = ThemeManager.getStatusColor(statusDoChat(chat))
            holder.dotStatus.backgroundTintList = ColorStateList.valueOf(cor)
        } else {
            holder.dotStatus.visibility = View.GONE
        }

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
            holder.imgFoto.imageTintList = null
            holder.imgFoto.setPadding(0, 0, 0, 0)
        } else {
            holder.imgFoto.setImageResource(R.drawable.ic_person)
            holder.imgFoto.imageTintList = ColorStateList.valueOf(0xFF0D226B.toInt())
            val pad = (10 * holder.itemView.resources.displayMetrics.density).toInt()
            holder.imgFoto.setPadding(pad, pad, pad, pad)
        }

        holder.card.setOnClickListener { onClick(chat) }
        holder.containerAvatar.setOnClickListener { onProfileClick(chat) }
        holder.txtNome.setOnClickListener { onProfileClick(chat) }
        holder.card.setOnLongClickListener {
            onLongClick(chat)
            true
        }
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