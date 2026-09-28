package com.example.plataformaremota.adapter

import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.plataformaremota.R
import com.example.plataformaremota.data.entity.Mensagem
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MensagemAdapter(
    private var mensagens: List<Mensagem>,
    private val fotosUsuarios: Map<String, String> = emptyMap(),
    private val onImagemClick: (String) -> Unit = {},
    private val onLongClick: (Mensagem) -> Unit = {}
) : RecyclerView.Adapter<MensagemAdapter.MensagemViewHolder>() {

    class MensagemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val container: FrameLayout = itemView.findViewById(R.id.containerMensagem)
        val linha: LinearLayout = itemView.findViewById(R.id.linhaMensagem)
        val cardFoto: MaterialCardView = itemView.findViewById(R.id.cardFotoRemetente)
        val imgFoto: ImageView = itemView.findViewById(R.id.imgFotoRemetente)
        val bubble: LinearLayout = itemView.findViewById(R.id.bubbleContainer)
        val txtNome: TextView = itemView.findViewById(R.id.txtNomeRemetente)
        val layoutReply: LinearLayout = itemView.findViewById(R.id.layoutReply)
        val txtReplyNome: TextView = itemView.findViewById(R.id.txtReplyNome)
        val txtReplyTexto: TextView = itemView.findViewById(R.id.txtReplyTexto)
        val imgMensagem: ImageView = itemView.findViewById(R.id.imgMensagem)
        val layoutAudio: LinearLayout = itemView.findViewById(R.id.layoutAudio)
        val btnPlayAudio: ImageView = itemView.findViewById(R.id.btnPlayAudio)
        val progressAudio: ProgressBar = itemView.findViewById(R.id.progressAudio)
        val txtDuracaoAudio: TextView = itemView.findViewById(R.id.txtDuracaoAudio)
        val txtTexto: TextView = itemView.findViewById(R.id.txtTextoMensagem)
        val txtEditada: TextView = itemView.findViewById(R.id.txtEditada)
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

        val horaFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        holder.txtHora.text = horaFormat.format(Date(msg.timestamp))

        val params = holder.bubble.layoutParams as LinearLayout.LayoutParams

        // Reset
        holder.imgMensagem.visibility = View.GONE
        holder.layoutAudio.visibility = View.GONE
        holder.txtTexto.visibility = View.GONE
        holder.layoutReply.visibility = View.GONE
        holder.txtEditada.visibility = View.GONE

        // ⭐ Reply preview
        if (msg.replyToId.isNotEmpty()) {
            holder.layoutReply.visibility = View.VISIBLE
            holder.txtReplyNome.text = msg.replyToNome
            holder.txtReplyTexto.text = msg.replyToTexto
        }

        // ⭐ Conteúdo
        when (msg.tipo) {
            "imagem" -> {
                holder.imgMensagem.visibility = View.VISIBLE
                Glide.with(holder.itemView.context)
                    .load(msg.urlMidia).fitCenter().into(holder.imgMensagem)
                holder.imgMensagem.setOnClickListener { onImagemClick(msg.urlMidia) }

                if (msg.texto.isNotBlank()) {
                    holder.txtTexto.visibility = View.VISIBLE
                    holder.txtTexto.text = msg.texto
                }
            }
            "audio" -> {
                holder.layoutAudio.visibility = View.VISIBLE
                holder.txtDuracaoAudio.text = formatarDuracao(msg.duracaoMs)
                holder.progressAudio.progress = 0
                holder.btnPlayAudio.setOnClickListener {
                    tocarAudio(holder, msg.urlMidia)
                }
            }
            else -> {
                holder.txtTexto.visibility = View.VISIBLE
                holder.txtTexto.text = msg.texto
            }
        }

        // ⭐ Editada
        if (msg.editada) {
            holder.txtEditada.visibility = View.VISIBLE
        }

        if (ehMinha) {
            holder.linha.gravity = Gravity.END
            holder.cardFoto.visibility = View.GONE
            holder.bubble.setBackgroundResource(R.drawable.bubble_minha)
            holder.txtTexto.setTextColor(android.graphics.Color.WHITE)
            holder.txtHora.setTextColor(0xB3FFFFFF.toInt())
            holder.txtNome.visibility = View.GONE
            holder.txtDuracaoAudio.setTextColor(android.graphics.Color.WHITE)
            holder.txtReplyNome.setTextColor(android.graphics.Color.WHITE)
            holder.txtReplyTexto.setTextColor(0xCCFFFFFF.toInt())
            params.marginStart = 40
            params.marginEnd = 0
        } else {
            holder.linha.gravity = Gravity.START
            holder.bubble.setBackgroundResource(R.drawable.bubble_outra)
            holder.txtTexto.setTextColor(0xFF333333.toInt())
            holder.txtHora.setTextColor(0xFF888888.toInt())
            holder.txtDuracaoAudio.setTextColor(0xFF333333.toInt())
            holder.txtReplyNome.setTextColor(0xFF0D226B.toInt())
            holder.txtReplyTexto.setTextColor(0xFF666666.toInt())

            holder.cardFoto.visibility = View.VISIBLE
            val fotoUrl = fotosUsuarios[msg.remetenteId] ?: ""
            if (fotoUrl.isNotEmpty()) {
                Glide.with(holder.itemView.context)
                    .load(fotoUrl).circleCrop().into(holder.imgFoto)
            } else {
                holder.imgFoto.setImageResource(R.drawable.ic_person)
            }

            if (msg.nomeRemetente.isNotEmpty()) {
                holder.txtNome.visibility = View.VISIBLE
                holder.txtNome.text = msg.nomeRemetente
            } else {
                holder.txtNome.visibility = View.GONE
            }

            params.marginStart = 0
            params.marginEnd = 40
        }

        holder.bubble.layoutParams = params

        // ⭐ Long press
        holder.bubble.setOnLongClickListener {
            onLongClick(msg)
            true
        }
    }

    private fun tocarAudio(holder: MensagemViewHolder, url: String) {
        try {
            val mp = MediaPlayer()
            mp.setDataSource(url)
            mp.prepare()
            mp.start()

            holder.btnPlayAudio.rotation = 90f

            val handler = Handler(Looper.getMainLooper())
            val duracao = mp.duration

            handler.post(object : Runnable {
                override fun run() {
                    try {
                        if (mp.isPlaying) {
                            holder.progressAudio.progress = (mp.currentPosition * 100 / duracao)
                            handler.postDelayed(this, 200)
                        }
                    } catch (_: Exception) { }
                }
            })

            mp.setOnCompletionListener {
                holder.progressAudio.progress = 0
                holder.btnPlayAudio.rotation = 0f
                mp.release()
                handler.removeCallbacksAndMessages(null)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun formatarDuracao(ms: Long): String {
        val seg = (ms / 1000).toInt()
        val min = seg / 60
        val s = seg % 60
        return String.format("%02d:%02d", min, s)
    }

    override fun getItemCount(): Int = mensagens.size

    fun atualizarLista(novaLista: List<Mensagem>) {
        mensagens = novaLista
        notifyDataSetChanged()
    }
}