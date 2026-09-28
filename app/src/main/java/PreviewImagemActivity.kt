package com.example.plataformaremota

import android.net.Uri
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.plataformaremota.data.repository.ChatRepository
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class PreviewImagemActivity : BaseActivity() {

    private lateinit var btnCancelar: ImageView
    private lateinit var imgPreview: ImageView
    private lateinit var edtLegenda: EditText
    private lateinit var btnEnviar: MaterialButton

    private lateinit var repository: ChatRepository

    private var chatId: String = ""
    private var imagemUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preview_imagem)

        repository = ChatRepository()

        btnCancelar = findViewById(R.id.btnCancelarPreview)
        imgPreview = findViewById(R.id.imgPreview)
        edtLegenda = findViewById(R.id.edtLegenda)
        btnEnviar = findViewById(R.id.btnEnviarPreview)

        chatId = intent.getStringExtra("chatId") ?: ""
        val uriString = intent.getStringExtra("imagemUri")

        if (chatId.isEmpty() || uriString.isNullOrEmpty()) {
            Toast.makeText(this, "Erro ao abrir prévia", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        imagemUri = Uri.parse(uriString)

        // Mostra a imagem
        Glide.with(this)
            .load(imagemUri)
            .fitCenter()
            .into(imgPreview)

        btnCancelar.setOnClickListener { finish() }
        btnEnviar.setOnClickListener { enviarImagem() }
    }

    private fun enviarImagem() {
        val uri = imagemUri ?: return
        val legenda = edtLegenda.text.toString().trim()

        btnEnviar.isEnabled = false
        Toast.makeText(this, "Enviando...", Toast.LENGTH_SHORT).show()

        try {
            MediaManager.get().upload(uri)
                .unsigned("fotos_perfil")
                .option("folder", "chats/$chatId")
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String?) { }
                    override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) { }

                    override fun onSuccess(requestId: String?, resultData: MutableMap<Any?, Any?>?) {
                        val url = resultData?.get("secure_url") as? String
                            ?: resultData?.get("url") as? String
                            ?: return
                        val secureUrl = url.replace("http://", "https://")

                        lifecycleScope.launch {
                            val ok = repository.enviarImagem(chatId, secureUrl, legenda)
                            if (ok) {
                                // Seta o resultado pra ChatActivity saber que enviou
                                setResult(RESULT_OK)
                                finish()
                            } else {
                                btnEnviar.isEnabled = true
                                Toast.makeText(this@PreviewImagemActivity, "Erro ao enviar", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }

                    override fun onError(requestId: String?, error: ErrorInfo?) {
                        btnEnviar.isEnabled = true
                        Toast.makeText(this@PreviewImagemActivity, "Erro: ${error?.description}", Toast.LENGTH_LONG).show()
                    }

                    override fun onReschedule(requestId: String?, error: ErrorInfo?) { }
                })
                .dispatch()
        } catch (e: Exception) {
            btnEnviar.isEnabled = true
            Toast.makeText(this, "Erro: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}