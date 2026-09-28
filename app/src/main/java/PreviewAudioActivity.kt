package com.example.plataformaremota

import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.plataformaremota.data.repository.ChatRepository
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import java.io.File

class PreviewAudioActivity : BaseActivity() {

    private lateinit var txtDuracao: TextView
    private lateinit var btnCancelar: MaterialButton
    private lateinit var btnEnviar: MaterialButton

    private lateinit var repository: ChatRepository

    private var chatId: String = ""
    private var audioPath: String = ""
    private var duracaoMs: Long = 0L

    private var mediaPlayer: MediaPlayer? = null
    private var tocando = false
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preview_audio)

        repository = ChatRepository()

        txtDuracao = findViewById(R.id.txtDuracaoPreview)
        btnCancelar = findViewById(R.id.btnCancelarAudio)
        btnEnviar = findViewById(R.id.btnEnviarAudio)

        chatId = intent.getStringExtra("chatId") ?: ""
        audioPath = intent.getStringExtra("audioPath") ?: ""
        duracaoMs = intent.getLongExtra("duracaoMs", 0L)

        if (chatId.isEmpty() || audioPath.isEmpty()) {
            Toast.makeText(this, "Erro ao abrir prévia", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        atualizarDuracao(duracaoMs)

        txtDuracao.setOnClickListener { togglePlayback() }

        btnCancelar.setOnClickListener {
            cancelarEVoltar()
        }

        btnEnviar.setOnClickListener {
            enviarAudio()
        }
    }

    private fun atualizarDuracao(ms: Long) {
        val seg = (ms / 1000).toInt()
        val min = seg / 60
        val s = seg % 60
        txtDuracao.text = String.format("%02d:%02d", min, s)
    }

    private fun togglePlayback() {
        val file = File(audioPath)
        if (!file.exists()) return

        if (tocando) {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            tocando = false
            atualizarDuracao(duracaoMs)
        } else {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(audioPath)
                prepare()
                start()
                tocando = true

                setOnCompletionListener {
                    tocando = false
                    atualizarDuracao(duracaoMs)
                }
            }

            // Atualiza a cada 100ms
            val inicio = System.currentTimeMillis()
            handler.post(object : Runnable {
                override fun run() {
                    if (!tocando) return
                    val decorrido = System.currentTimeMillis() - inicio
                    atualizarDuracao(decorrido)
                    handler.postDelayed(this, 100)
                }
            })
        }
    }

    private fun cancelarEVoltar() {
        try {
            File(audioPath).delete()
        } catch (_: Exception) { }
        finish()
    }

    private fun enviarAudio() {
        btnEnviar.isEnabled = false
        Toast.makeText(this, "Enviando áudio...", Toast.LENGTH_SHORT).show()

        try {
            MediaManager.get().upload(Uri.fromFile(File(audioPath)))
                .unsigned("fotos_perfil")
                .option("folder", "chats/$chatId/audios")
                .option("resource_type", "video")  // Cloudinary trata áudio como "video"
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String?) { }
                    override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) { }

                    override fun onSuccess(requestId: String?, resultData: MutableMap<Any?, Any?>?) {
                        val url = resultData?.get("secure_url") as? String
                            ?: resultData?.get("url") as? String
                            ?: return
                        val secureUrl = url.replace("http://", "https://")

                        lifecycleScope.launch {
                            val ok = repository.enviarAudio(chatId, secureUrl, duracaoMs)
                            if (ok) {
                                File(audioPath).delete()
                                setResult(RESULT_OK)
                                finish()
                            } else {
                                btnEnviar.isEnabled = true
                                Toast.makeText(this@PreviewAudioActivity, "Erro ao enviar", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }

                    override fun onError(requestId: String?, error: ErrorInfo?) {
                        btnEnviar.isEnabled = true
                        Toast.makeText(this@PreviewAudioActivity, "Erro: ${error?.description}", Toast.LENGTH_LONG).show()
                    }

                    override fun onReschedule(requestId: String?, error: ErrorInfo?) { }
                })
                .dispatch()
        } catch (e: Exception) {
            btnEnviar.isEnabled = true
            Toast.makeText(this, "Erro: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        mediaPlayer?.release()
        mediaPlayer = null
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}