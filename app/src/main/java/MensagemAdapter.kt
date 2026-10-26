package com.example.plataformaremota.adapter

import android.graphics.Typeface
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
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.plataformaremota.AudioWaveformView
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
    private val onLongClick: (Mensagem) -> Unit = {},
    private val onArquivoClick: (Mensagem) -> Unit = {}
) : RecyclerView.Adapter<MensagemAdapter.MensagemViewHolder>() {

    private var mediaPlayer: MediaPlayer? = null
    private var handlerAudio: Handler? = null
    private var posicaoTocando: Int = -1

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
        val waveformAudio: AudioWaveformView = itemView.findViewById(R.id.waveformAudio)
        val txtDuracaoAudio: TextView = itemView.findViewById(R.id.txtDuracaoAudio)
        val layoutArquivo: LinearLayout = itemView.findViewById(R.id.layoutArquivo)
        val imgIconeArquivo: ImageView = itemView.findViewById(R.id.imgIconeArquivo)
        val txtNomeArquivo: TextView = itemView.findViewById(R.id.txtNomeArquivo)
        val txtTamanhoArquivo: TextView = itemView.findViewById(R.id.txtTamanhoArquivo)
        val txtTexto: TextView = itemView.findViewById(R.id.txtTextoMensagem)
        val txtEditada: TextView = itemView.findViewById(R.id.txtEditada)
        val txtHora: TextView = itemView.findViewById(R.id.txtHora)
        val imgCheck: ImageView = itemView.findViewById(R.id.imgCheck)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MensagemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_mensagem, parent, false)
        return MensagemViewHolder(view)
    }

    override fun onBindViewHolder(holder: MensagemViewHolder, position: Int) {
        val msg = mensagens[position]
        val uidAtual = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        val ehMinha = msg.remetenteId == uidAtual
        val ctx = holder.itemView.context

        if (posicaoTocando != position) {
            holder.btnPlayAudio.setImageResource(R.drawable.ic_play)
            holder.waveformAudio.setProgress(0f)
        }

        if (msg.tipo == "sistema") {
            holder.linha.gravity = Gravity.CENTER
            holder.cardFoto.visibility = View.GONE
            holder.bubble.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            holder.bubble.setPadding(16, 4, 16, 4)
            holder.txtNome.visibility = View.GONE
            holder.layoutReply.visibility = View.GONE
            holder.imgMensagem.visibility = View.GONE
            holder.layoutAudio.visibility = View.GONE
            holder.layoutArquivo.visibility = View.GONE
            holder.txtEditada.visibility = View.GONE
            holder.imgCheck.visibility = View.GONE

            holder.txtTexto.visibility = View.VISIBLE
            holder.txtTexto.setTextColor(0xFF8696A0.toInt())
            holder.txtTexto.textSize = 13f
            holder.txtTexto.gravity = Gravity.CENTER
            holder.txtTexto.setTypeface(null, Typeface.ITALIC)
            holder.txtHora.visibility = View.GONE

            val txt = msg.texto
            if (txt.startsWith("@SYS@")) {
                try {
                    val json = org.json.JSONObject(txt.substring(5))
                    val key = json.getString("key")
                    val paramsArray = json.getJSONArray("params")
                    val paramsList = mutableListOf<String>()
                    for (i in 0 until paramsArray.length()) {
                        paramsList.add(paramsArray.getString(i))
                    }
                    val resId = when (key) {
                        "sys_entrou_grupo" -> R.string.sys_entrou_grupo
                        "sys_removido_grupo" -> R.string.sys_removido_grupo
                        "sys_agora_admin" -> R.string.sys_agora_admin
                        "sys_nao_admin" -> R.string.sys_nao_admin
                        "sys_foto_atualizada" -> R.string.sys_foto_atualizada
                        else -> 0
                    }
                    if (resId != 0) holder.txtTexto.text = ctx.getString(resId, *paramsList.toTypedArray())
                    else holder.txtTexto.text = txt
                } catch (e: Exception) {
                    holder.txtTexto.text = txt
                }
            } else {
                holder.txtTexto.text = txt
            }
            return
        }

        holder.txtHora.visibility = View.VISIBLE
        holder.txtTexto.textSize = 15f
        holder.txtTexto.gravity = Gravity.START
        holder.txtTexto.setTypeface(null, Typeface.NORMAL)

        val horaFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        holder.txtHora.text = horaFormat.format(Date(msg.timestamp))

        val params = holder.bubble.layoutParams as LinearLayout.LayoutParams

        holder.imgMensagem.visibility = View.GONE
        holder.layoutAudio.visibility = View.GONE
        holder.layoutArquivo.visibility = View.GONE
        holder.txtTexto.visibility = View.GONE
        holder.layoutReply.visibility = View.GONE
        holder.txtEditada.visibility = View.GONE

        if (msg.replyToId.isNotEmpty()) {
            holder.layoutReply.visibility = View.VISIBLE
            holder.txtReplyNome.text = msg.replyToNome
            holder.txtReplyTexto.text = msg.replyToTexto
        }

        when (msg.tipo) {
            "imagem" -> {
                holder.imgMensagem.visibility = View.VISIBLE
                Glide.with(ctx).load(msg.urlMidia).fitCenter().into(holder.imgMensagem)
                holder.imgMensagem.setOnClickListener { onImagemClick(msg.urlMidia) }
                if (msg.texto.isNotBlank()) {
                    holder.txtTexto.visibility = View.VISIBLE
                    holder.txtTexto.text = msg.texto
                }
            }
            "audio" -> {
                holder.layoutAudio.visibility = View.VISIBLE
                val seg = (msg.duracaoMs / 1000).toInt()
                holder.txtDuracaoAudio.text = String.format("%d:%02d", seg / 60, seg % 60)
                holder.waveformAudio.setProgress(0f)
                holder.btnPlayAudio.setImageResource(R.drawable.ic_play)

                holder.btnPlayAudio.setOnClickListener {
                    tocarAudio(holder, position, msg.urlMidia)
                }
            }
            "arquivo" -> {
                holder.layoutArquivo.visibility = View.VISIBLE
                holder.txtNomeArquivo.text = msg.nomeArquivo.ifEmpty { ctx.getString(R.string.chat_arquivo_indisponivel) }
                holder.txtTamanhoArquivo.text = formatarTamanho(ctx, msg.tamanhoArquivo)
                holder.layoutArquivo.setOnClickListener { onArquivoClick(msg) }
            }
            else -> {
                holder.txtTexto.visibility = View.VISIBLE
                holder.txtTexto.text = msg.texto
            }
        }

        if (msg.editada) {
            holder.txtEditada.visibility = View.VISIBLE
            holder.txtEditada.text = ctx.getString(R.string.chat_editada)
        }

        // ⭐ CONFIGURAÇÃO DE CORES (INTEGRA BLUE)
        if (ehMinha) {
            holder.linha.gravity = Gravity.END
            holder.bubble.setBackgroundResource(R.drawable.bg_msg_out) // Azul Escuro Integra
            holder.txtTexto.setTextColor(0xFFE9EDEF.toInt()) // Texto Branco
            holder.txtNome.visibility = View.GONE

            holder.txtHora.setTextColor(0xFF99FFFFFF.toInt()) // Branco transparente
            holder.txtEditada.setTextColor(0xFF99FFFFFF.toInt())

            // ⭐ CHECK DE ENVIADO (Tique único branco/cinza)
            holder.imgCheck.visibility = View.VISIBLE
            holder.imgCheck.setImageResource(R.drawable.ic_check) // Ícone de 1 check
            holder.imgCheck.imageTintList = android.content.res.ColorStateList.valueOf(0xFF99FFFFFF.toInt())

            // Cores do Áudio (Minha)
            holder.txtDuracaoAudio.setTextColor(0xFF99FFFFFF.toInt())
            holder.btnPlayAudio.imageTintList = android.content.res.ColorStateList.valueOf(0xFFE9EDEF.toInt())
            holder.waveformAudio.corTocada = 0xFFE9EDEF.toInt() // Onda Branca
            holder.waveformAudio.corNaoTocada = 0x66FFFFFF // Branco transparente

            params.marginStart = 40
            params.marginEnd = 0

        } else {
            holder.linha.gravity = Gravity.START
            holder.bubble.setBackgroundResource(R.drawable.bg_msg_in) // Cinza Escuro WhatsApp
            holder.txtTexto.setTextColor(0xFFE9EDEF.toInt()) // Texto Branco
            holder.txtNome.visibility = View.VISIBLE

            holder.txtHora.setTextColor(0xFF8696A0.toInt()) // Cinza claro
            holder.txtEditada.setTextColor(0xFF8696A0.toInt())

            // Não mostra check nas mensagens recebidas
            holder.imgCheck.visibility = View.GONE

            // Cores do Áudio (Do outro) - Azul Claro do Integra
            holder.txtDuracaoAudio.setTextColor(0xFF8696A0.toInt())
            holder.btnPlayAudio.imageTintList = android.content.res.ColorStateList.valueOf(0xFF1E88E5.toInt()) // Azul
            holder.waveformAudio.corTocada = 0xFF1E88E5.toInt() // Onda Azul Claro
            holder.waveformAudio.corNaoTocada = 0xFF8696A0.toInt() // Cinza

            holder.cardFoto.visibility = View.GONE // Esconde a foto em conversas privadas

            params.marginStart = 0
            params.marginEnd = 40
        }

        holder.bubble.layoutParams = params
        holder.bubble.setOnLongClickListener {
            onLongClick(msg)
            true
        }
    }

    private fun tocarAudio(holder: MensagemViewHolder, position: Int, url: String) {
        try {
            if (posicaoTocando == position) {
                pararAudioAtual()
                return
            }

            pararAudioAtual()

            posicaoTocando = position
            holder.btnPlayAudio.setImageResource(R.drawable.ic_pause)
            holder.waveformAudio.setProgress(0f)

            mediaPlayer = MediaPlayer()
            mediaPlayer?.setDataSource(url)
            mediaPlayer?.prepareAsync()

            mediaPlayer?.setOnPreparedListener { mp ->
                mp.start()
                val duracao = mp.duration
                handlerAudio = Handler(Looper.getMainLooper())

                handlerAudio?.post(object : Runnable {
                    override fun run() {
                        try {
                            if (mp.isPlaying) {
                                val progresso = mp.currentPosition.toFloat() / duracao.toFloat()
                                holder.waveformAudio.setProgress(progresso)
                                handlerAudio?.postDelayed(this, 50)
                            }
                        } catch (_: Exception) {}
                    }
                })
            }

            mediaPlayer?.setOnCompletionListener { mp ->
                pararAudioAtual()
                mp.release()
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun pararAudioAtual() {
        handlerAudio?.removeCallbacksAndMessages(null)
        mediaPlayer?.let {
            if (it.isPlaying) it.stop()
            it.release()
        }
        mediaPlayer = null

        if (posicaoTocando != -1) {
            notifyItemChanged(posicaoTocando)
            posicaoTocando = -1
        }
    }

    override fun onViewRecycled(holder: MensagemViewHolder) {
        super.onViewRecycled(holder)
        if (holder.adapterPosition == posicaoTocando) {
            pararAudioAtual()
        }
    }

    private fun formatarTamanho(context: android.content.Context, bytes: Long): String {
        if (bytes <= 0) return context.getString(R.string.chat_arquivo_indisponivel)
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        return when {
            mb >= 1 -> String.format("%.1f MB", mb)
            kb >= 1 -> String.format("%.0f KB", kb)
            else -> "$bytes B"
        }
    }

    override fun getItemCount(): Int = mensagens.size

    fun atualizarLista(novaLista: List<Mensagem>) {
        mensagens = novaLista
        notifyDataSetChanged()
    }
}