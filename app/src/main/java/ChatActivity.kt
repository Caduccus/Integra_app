package com.example.plataformaremota

import android.app.Dialog
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.Window
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.plataformaremota.adapter.MensagemAdapter
import com.example.plataformaremota.data.entity.Mensagem
import com.example.plataformaremota.data.repository.ChatRepository
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File

class ChatActivity : BaseActivity() {

    private lateinit var btnVoltar: ImageView
    private lateinit var btnInfo: ImageView
    private lateinit var btnAnexar: ImageView
    private lateinit var btnGravarAudio: ImageView
    private lateinit var imgFotoHeader: ImageView
    private lateinit var txtNomeChat: TextView
    private lateinit var rvMensagens: RecyclerView
    private lateinit var edtMensagem: EditText
    private lateinit var btnEnviar: MaterialButton

    private lateinit var layoutReplyBar: LinearLayout
    private lateinit var txtReplyBarNome: TextView
    private lateinit var txtReplyBarTexto: TextView
    private lateinit var btnFecharReply: ImageView

    private lateinit var layoutAudioReady: LinearLayout
    private lateinit var btnCancelarAudioReady: ImageView
    private lateinit var btnPlayAudioReady: ImageView
    private lateinit var progressAudioReady: ProgressBar
    private lateinit var txtDuracaoReady: TextView

    private lateinit var adapter: MensagemAdapter
    private lateinit var repository: ChatRepository

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var chatId: String = ""
    private var listenerRegistration: ListenerRegistration? = null

    private var participantesAtuais: List<String> = emptyList()
    private var ehGrupoAtual: Boolean = false
    private var activityAtiva: Boolean = false
    private var fotosUsuarios: Map<String, String> = emptyMap()
    private var mensagensAtuais: List<Mensagem> = emptyList()

    private val MAX_NOTIFICACOES = 3L

    private var mensagemRespondendo: Mensagem? = null

    private var estaGravando: Boolean = false
    private var arquivoAudio: File? = null
    private var duracaoAudio: Long = 0L
    private var recorderHelper: AudioRecorderHelper? = null

    private var mediaPlayerPreview: MediaPlayer? = null
    private var tocandoPreview: Boolean = false
    private val handler = Handler(Looper.getMainLooper())

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val intent = Intent(this, PreviewImagemActivity::class.java)
            intent.putExtra("chatId", chatId)
            intent.putExtra("imagemUri", uri.toString())
            startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        repository = ChatRepository()

        // ⭐ Inicializa TODOS os campos
        btnVoltar = findViewById(R.id.btnVoltarChat)
        btnInfo = findViewById(R.id.btnInfoChat)
        btnAnexar = findViewById(R.id.btnAnexar)
        btnGravarAudio = findViewById(R.id.btnGravarAudio)
        imgFotoHeader = findViewById(R.id.imgFotoChatHeader)
        txtNomeChat = findViewById(R.id.txtNomeChat)
        rvMensagens = findViewById(R.id.rvMensagens)
        edtMensagem = findViewById(R.id.edtMensagem)
        btnEnviar = findViewById(R.id.btnEnviar)

        layoutReplyBar = findViewById(R.id.layoutReplyBar)
        txtReplyBarNome = findViewById(R.id.txtReplyBarNome)
        txtReplyBarTexto = findViewById(R.id.txtReplyBarTexto)
        btnFecharReply = findViewById(R.id.btnFecharReply)

        layoutAudioReady = findViewById(R.id.layoutAudioReady)
        btnCancelarAudioReady = findViewById(R.id.btnCancelarAudioReady)
        btnPlayAudioReady = findViewById(R.id.btnPlayAudioReady)
        progressAudioReady = findViewById(R.id.progressAudioReady)
        txtDuracaoReady = findViewById(R.id.txtDuracaoReady)

        chatId = intent.getStringExtra("chatId") ?: ""

        if (chatId.isEmpty()) {
            Toast.makeText(this, "Chat não encontrado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        configurarRecyclerView()
        carregarDadosChat()
        ouvirMensagens()

        btnVoltar.setOnClickListener { finish() }
        btnEnviar.setOnClickListener { enviar() }
        btnAnexar.setOnClickListener {
            pickImageLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
        btnGravarAudio.setOnClickListener { toggleGravacao() }

        // ⭐ Abre Info do Grupo
        btnInfo.setOnClickListener {
            if (ehGrupoAtual) {
                // ⭐ É grupo → abre Info do Grupo
                val intent = Intent(this, InfoGrupoActivity::class.java)
                intent.putExtra("chatId", chatId)
                startActivity(intent)
            } else {
                // ⭐ É 1:1 → abre perfil do outro usuário
                val uidAtual = auth.currentUser?.uid ?: return@setOnClickListener
                val outroUid = participantesAtuais.firstOrNull { it != uidAtual }

                if (outroUid != null) {
                    val intent = Intent(this, PerfilUsuarioActivity::class.java)
                    intent.putExtra("uidUsuario", outroUid)
                    intent.putExtra("chatId", "")  // Sem chatId = sem botões de grupo
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "Não foi possível abrir o perfil", Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnFecharReply.setOnClickListener { cancelarReply() }
        btnCancelarAudioReady.setOnClickListener { cancelarAudio() }
        btnPlayAudioReady.setOnClickListener { togglePlayPreview() }
    }

    override fun onResume() {
        super.onResume()
        activityAtiva = true
        ChatAtivoManager.chatAtivo = chatId
        resetarContadorNaoLidas()
    }

    override fun onPause() {
        super.onPause()
        activityAtiva = false
        ChatAtivoManager.chatAtivo = null
    }

    private fun mostrarReply(msg: Mensagem) {
        mensagemRespondendo = msg
        layoutReplyBar.visibility = LinearLayout.VISIBLE

        txtReplyBarNome.text = if (msg.remetenteId == auth.currentUser?.uid) {
            "Você"
        } else {
            msg.nomeRemetente.ifEmpty { "Usuário" }
        }

        txtReplyBarTexto.text = when (msg.tipo) {
            "imagem" -> "📷 Imagem"
            "audio" -> "🎤 Áudio"
            else -> msg.texto
        }
    }

    private fun cancelarReply() {
        mensagemRespondendo = null
        layoutReplyBar.visibility = LinearLayout.GONE
    }

    private fun abrirDialogEditar(msg: Mensagem) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_editar_perfil, null)
        val edtNome = dialogView.findViewById<EditText>(R.id.edtDialogNome)
        val edtEmail = dialogView.findViewById<EditText>(R.id.edtDialogEmail)
        val edtProfissao = dialogView.findViewById<EditText>(R.id.edtDialogProfissao)

        edtEmail.visibility = android.view.View.GONE
        edtProfissao.visibility = android.view.View.GONE

        edtNome.hint = "Nova mensagem"
        edtNome.setText(msg.texto)
        edtNome.setSelection(msg.texto.length)

        AlertDialog.Builder(this)
            .setTitle("Editar mensagem")
            .setView(dialogView)
            .setPositiveButton("Salvar") { _, _ ->
                val novoTexto = edtNome.text.toString().trim()
                if (novoTexto.isNotEmpty() && novoTexto != msg.texto) {
                    lifecycleScope.launch {
                        val ok = repository.editarMensagem(chatId, msg.id, novoTexto)
                        if (!ok) {
                            Toast.makeText(this@ChatActivity, "Erro ao editar", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun confirmarDeletar(msg: Mensagem) {
        AlertDialog.Builder(this)
            .setTitle("Deletar mensagem")
            .setMessage("Tem certeza que quer deletar essa mensagem?")
            .setPositiveButton("Deletar") { _, _ ->
                lifecycleScope.launch {
                    val ok = repository.deletarMensagem(chatId, msg.id)
                    if (ok) {
                        Toast.makeText(this@ChatActivity, "Mensagem deletada", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@ChatActivity, "Erro ao deletar", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun abrirMenuMensagem(msg: Mensagem) {
        val uid = auth.currentUser?.uid ?: return
        val ehMinha = msg.remetenteId == uid

        val opcoes = mutableListOf<String>()
        opcoes.add("Responder")
        if (ehMinha && msg.tipo == "texto") opcoes.add("Editar")
        if (ehMinha) opcoes.add("Deletar")

        AlertDialog.Builder(this)
            .setItems(opcoes.toTypedArray()) { _, which ->
                when (opcoes[which]) {
                    "Responder" -> mostrarReply(msg)
                    "Editar" -> abrirDialogEditar(msg)
                    "Deletar" -> confirmarDeletar(msg)
                }
            }
            .show()
    }

    private fun toggleGravacao() {
        if (!estaGravando && arquivoAudio == null) {
            recorderHelper = AudioRecorderHelper(this)
            val arquivo = recorderHelper?.iniciarGravacao()

            if (arquivo != null) {
                estaGravando = true
                btnGravarAudio.setImageResource(R.drawable.ic_stop)
                btnGravarAudio.setColorFilter(android.graphics.Color.RED)
                edtMensagem.isEnabled = false
                edtMensagem.hint = "Gravando..."
            } else {
                Toast.makeText(this, "Erro ao iniciar gravação. Verifique permissão de microfone.", Toast.LENGTH_LONG).show()
            }
        } else if (estaGravando) {
            val (arquivo, duracao) = recorderHelper?.pararGravacao() ?: Pair(null, 0L)
            estaGravando = false
            btnGravarAudio.setImageResource(R.drawable.ic_mic)
            btnGravarAudio.clearColorFilter()

            if (arquivo != null && duracao > 500) {
                arquivoAudio = arquivo
                duracaoAudio = duracao
                mostrarPreviewAudio()
            } else {
                Toast.makeText(this, "Áudio muito curto", Toast.LENGTH_SHORT).show()
                arquivo?.delete()
                edtMensagem.isEnabled = true
                edtMensagem.hint = "Digite uma mensagem..."
            }
        }
    }

    private fun mostrarPreviewAudio() {
        btnAnexar.visibility = LinearLayout.GONE
        btnGravarAudio.visibility = LinearLayout.GONE
        edtMensagem.visibility = LinearLayout.GONE
        layoutAudioReady.visibility = LinearLayout.VISIBLE
        txtDuracaoReady.text = formatarDuracao(duracaoAudio)
        progressAudioReady.progress = 0
    }

    private fun cancelarAudio() {
        mediaPlayerPreview?.release()
        mediaPlayerPreview = null
        tocandoPreview = false
        handler.removeCallbacksAndMessages(null)

        arquivoAudio?.delete()
        arquivoAudio = null
        duracaoAudio = 0L

        layoutAudioReady.visibility = LinearLayout.GONE
        btnAnexar.visibility = LinearLayout.VISIBLE
        btnGravarAudio.visibility = LinearLayout.VISIBLE
        edtMensagem.visibility = LinearLayout.VISIBLE
        edtMensagem.isEnabled = true
        edtMensagem.hint = "Digite uma mensagem..."
    }

    private fun togglePlayPreview() {
        val arquivo = arquivoAudio ?: return
        if (!arquivo.exists()) return

        if (tocandoPreview) {
            mediaPlayerPreview?.stop()
            mediaPlayerPreview?.release()
            mediaPlayerPreview = null
            tocandoPreview = false
            progressAudioReady.progress = 0
            btnPlayAudioReady.rotation = 0f
        } else {
            try {
                mediaPlayerPreview = MediaPlayer().apply {
                    setDataSource(arquivo.absolutePath)
                    prepare()
                    start()
                }
                tocandoPreview = true
                btnPlayAudioReady.rotation = 90f

                val mp = mediaPlayerPreview!!
                val dur = mp.duration

                handler.post(object : Runnable {
                    override fun run() {
                        try {
                            if (mp.isPlaying) {
                                progressAudioReady.progress = (mp.currentPosition * 100 / dur)
                                handler.postDelayed(this, 200)
                            }
                        } catch (_: Exception) { }
                    }
                })

                mp.setOnCompletionListener {
                    progressAudioReady.progress = 0
                    btnPlayAudioReady.rotation = 0f
                    tocandoPreview = false
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Erro ao tocar", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun enviarAudio() {
        val arquivo = arquivoAudio ?: return
        val duracao = duracaoAudio

        Toast.makeText(this, "Enviando áudio...", Toast.LENGTH_SHORT).show()

        try {
            MediaManager.get().upload(Uri.fromFile(arquivo))
                .unsigned("fotos_perfil")
                .option("folder", "chats/$chatId/audios")
                .option("resource_type", "video")
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String?) { }
                    override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) { }

                    override fun onSuccess(requestId: String?, resultData: MutableMap<Any?, Any?>?) {
                        val url = resultData?.get("secure_url") as? String
                            ?: resultData?.get("url") as? String
                            ?: return
                        val secureUrl = url.replace("http://", "https://")

                        lifecycleScope.launch {
                            val ok = repository.enviarAudio(chatId, secureUrl, duracao)
                            if (ok) {
                                arquivoAudio?.delete()
                                arquivoAudio = null
                                duracaoAudio = 0L
                                layoutAudioReady.visibility = LinearLayout.GONE
                                btnAnexar.visibility = LinearLayout.VISIBLE
                                btnGravarAudio.visibility = LinearLayout.VISIBLE
                                edtMensagem.visibility = LinearLayout.VISIBLE
                                edtMensagem.isEnabled = true
                                edtMensagem.hint = "Digite uma mensagem..."
                            } else {
                                Toast.makeText(this@ChatActivity, "Erro ao salvar áudio", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }

                    override fun onError(requestId: String?, error: ErrorInfo?) {
                        Toast.makeText(this@ChatActivity, "Erro: ${error?.description}", Toast.LENGTH_LONG).show()
                    }

                    override fun onReschedule(requestId: String?, error: ErrorInfo?) { }
                })
                .dispatch()
        } catch (e: Exception) {
            Toast.makeText(this, "Erro: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun enviar() {
        if (arquivoAudio != null) {
            enviarAudio()
            return
        }

        val texto = edtMensagem.text.toString().trim()
        if (texto.isEmpty()) return

        val reply = mensagemRespondendo
        edtMensagem.setText("")
        cancelarReply()
        btnEnviar.isEnabled = false

        lifecycleScope.launch {
            val sucesso = if (reply != null) {
                repository.enviarMensagemComReply(chatId, texto, reply)
            } else {
                repository.enviarMensagem(chatId, texto)
            }
            btnEnviar.isEnabled = true

            if (sucesso) {
                notificarParticipantes(texto)
            } else {
                Toast.makeText(this@ChatActivity, "Erro ao enviar", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun resetarContadorNaoLidas() {
        val uid = auth.currentUser?.uid ?: return
        lifecycleScope.launch {
            try {
                db.collection("chats").document(chatId)
                    .collection("status").document(uid)
                    .set(
                        mapOf(
                            "unreadCount" to 0,
                            "ultimaVez" to System.currentTimeMillis()
                        ),
                        SetOptions.merge()
                    ).await()
            } catch (_: Exception) { }
        }
    }

    private fun configurarRecyclerView() {
        adapter = MensagemAdapter(
            mensagens = mensagensAtuais,
            fotosUsuarios = fotosUsuarios,
            onImagemClick = { url -> abrirImagemEmTelaCheia(url) },
            onLongClick = { msg -> abrirMenuMensagem(msg) }
        )
        rvMensagens.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        rvMensagens.adapter = adapter
    }

    private fun carregarDadosChat() {
        lifecycleScope.launch {
            val chat = repository.buscarChat(chatId) ?: return@launch
            val uidAtual = auth.currentUser?.uid ?: return@launch

            participantesAtuais = chat.participantes
            ehGrupoAtual = chat.ehGrupo

            if (chat.ehGrupo) {
                txtNomeChat.text = chat.nome.ifEmpty { "Grupo" }
                if (chat.fotoUrl.isNotEmpty()) {
                    Glide.with(this@ChatActivity).load(chat.fotoUrl)
                        .circleCrop().into(imgFotoHeader)
                    imgFotoHeader.imageTintList = null
                    imgFotoHeader.setPadding(0, 0, 0, 0)
                }
            } else {
                val outroUid = chat.participantes.firstOrNull { it != uidAtual }
                if (outroUid != null) {
                    try {
                        val doc = db.collection("usuarios").document(outroUid).get().await()
                        txtNomeChat.text = doc.getString("nome") ?: "Usuário"
                        val fotoUrl = doc.getString("fotoUrl") ?: ""
                        if (fotoUrl.isNotEmpty()) {
                            Glide.with(this@ChatActivity).load(fotoUrl)
                                .circleCrop().into(imgFotoHeader)
                            imgFotoHeader.imageTintList = null
                            imgFotoHeader.setPadding(0, 0, 0, 0)
                        }
                    } catch (e: Exception) { }
                }

                val mapaFotos = mutableMapOf<String, String>()
                for (uid in chat.participantes) {
                    if (uid == uidAtual) continue
                    try {
                        val d = db.collection("usuarios").document(uid).get().await()
                        mapaFotos[uid] = d.getString("fotoUrl") ?: ""
                    } catch (_: Exception) { }
                }
                fotosUsuarios = mapaFotos

                adapter = MensagemAdapter(
                    mensagens = mensagensAtuais,
                    fotosUsuarios = fotosUsuarios,
                    onImagemClick = { url -> abrirImagemEmTelaCheia(url) },
                    onLongClick = { msg -> abrirMenuMensagem(msg) }
                )
                rvMensagens.adapter = adapter
            }

            if (mensagensAtuais.isNotEmpty()) {
                rvMensagens.scrollToPosition(mensagensAtuais.size - 1)
            }
        }
    }

    private fun ouvirMensagens() {
        listenerRegistration = db.collection("chats")
            .document(chatId)
            .collection("mensagens")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (!activityAtiva || isDestroyed || isFinishing) return@addSnapshotListener
                if (error != null) return@addSnapshotListener

                val lista = snapshot?.documents?.mapNotNull {
                    it.toObject(Mensagem::class.java)
                } ?: emptyList()

                mensagensAtuais = lista
                adapter.atualizarLista(lista)
                if (lista.isNotEmpty()) rvMensagens.scrollToPosition(lista.size - 1)
            }
    }

    private fun abrirImagemEmTelaCheia(url: String) {
        val dialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_imagem_cheia)
        dialog.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT
        )

        val imgFull: ImageView = dialog.findViewById(R.id.imgFull)
        val btnFechar: ImageView = dialog.findViewById(R.id.btnFecharImagem)
        val btnBaixar: ImageView = dialog.findViewById(R.id.btnBaixarImagem)

        Glide.with(this).load(url).into(imgFull)

        btnFechar.setOnClickListener { dialog.dismiss() }
        imgFull.setOnClickListener { dialog.dismiss() }
        btnBaixar.setOnClickListener { baixarImagem(url) }

        dialog.show()
    }

    private fun baixarImagem(url: String) {
        Toast.makeText(this, "Baixando imagem...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            try {
                val bitmap = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    val stream = java.net.URL(url).openStream()
                    android.graphics.BitmapFactory.decodeStream(stream)
                }

                if (bitmap == null) {
                    Toast.makeText(this@ChatActivity, "Erro ao baixar", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val nomeArquivo = "integra_${System.currentTimeMillis()}.jpg"

                val valores = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, nomeArquivo)
                    put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Integra")
                    }
                }

                val uri = contentResolver.insert(
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    valores
                )

                if (uri != null) {
                    contentResolver.openOutputStream(uri)?.use { out ->
                        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 95, out)
                    }
                    Toast.makeText(this@ChatActivity, "Imagem salva na galeria! ✅", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this@ChatActivity, "Erro ao salvar", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ChatActivity, "Erro: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun notificarParticipantes(texto: String) {
        val uidAtual = auth.currentUser?.uid ?: return
        val destinatarios = participantesAtuais.filter { it != uidAtual }
        if (destinatarios.isEmpty()) return

        lifecycleScope.launch {
            try {
                val docMeu = db.collection("usuarios").document(uidAtual).get().await()
                val meuNome = docMeu.getString("nome") ?: "Usuário"

                val titulo = if (ehGrupoAtual) "Nova mensagem no grupo" else "Nova mensagem"
                val mensagem = "$meuNome: $texto"

                for (dest in destinatarios) {
                    val statusRef = db.collection("chats").document(chatId)
                        .collection("status").document(dest)

                    val statusDoc = statusRef.get().await()
                    val unreadCount = statusDoc.getLong("unreadCount") ?: 0L

                    if (unreadCount >= MAX_NOTIFICACOES) {
                        statusRef.set(
                            mapOf("unreadCount" to (unreadCount + 1), "ultimaVez" to System.currentTimeMillis()),
                            SetOptions.merge()
                        ).await()
                        continue
                    }

                    statusRef.set(
                        mapOf("unreadCount" to (unreadCount + 1), "ultimaVez" to System.currentTimeMillis()),
                        SetOptions.merge()
                    ).await()

                    NotificacaoHelper.enviar(dest, titulo, mensagem, chatId)
                }
            } catch (e: Exception) { }
        }
    }

    private fun formatarDuracao(ms: Long): String {
        val seg = (ms / 1000).toInt()
        val min = seg / 60
        val s = seg % 60
        return String.format("%02d:%02d", min, s)
    }

    override fun onDestroy() {
        listenerRegistration?.remove()
        mediaPlayerPreview?.release()
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}