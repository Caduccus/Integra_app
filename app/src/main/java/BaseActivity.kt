package com.example.plataformaremota

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.cloudinary.android.MediaManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.onesignal.OneSignal

open class BaseActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun attachBaseContext(newBase: Context) {
        val escala = AcessibilidadePrefs.getFontScaleValue(newBase)
        val config = Configuration(newBase.resources.configuration)
        config.fontScale = escala
        val ctx = newBase.createConfigurationContext(config)
        super.attachBaseContext(ctx)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.BLACK

        if (AcessibilidadePrefs.isReduzirAnimacoes(this)) {
            overridePendingTransition(0, 0)
        }

        if (!CloudinaryManager.iniciado) {
            val config = HashMap<String, String>()
            config["cloud_name"] = "qatmo4ge"
            MediaManager.init(this, config)
            CloudinaryManager.iniciado = true
        }

        auth.currentUser?.uid?.let { uid -> OneSignal.login(uid) }
        AdminChecker.carregar()
    }

    override fun onContentChanged() {
        super.onContentChanged()
        aplicarTema()
        aplicarInsets()
    }

    override fun onResume() {
        super.onResume()
        atualizarStatusAutomatico(online = true)

        // ⭐ Re-aplica o tema TODA vez que volta ao foreground
        //   Isso garante que trocar preferência na Acessibilidade
        //   ou trocar tema em outra tela se reflita aqui
        aplicarTema()
    }

    override fun onPause() {
        super.onPause()
        atualizarStatusAutomatico(online = false)
    }

    override fun finish() {
        super.finish()
        if (AcessibilidadePrefs.isReduzirAnimacoes(this)) {
            overridePendingTransition(0, 0)
        }
    }

    private fun atualizarStatusAutomatico(online: Boolean) {
        val uid = auth.currentUser?.uid ?: return
        val novoStatus = if (online) ThemeManager.STATUS_ONLINE else ThemeManager.STATUS_AUSENTE

        db.collection("usuarios").document(uid).get()
            .addOnSuccessListener { doc ->
                val statusAtual = doc.getString("status") ?: ThemeManager.STATUS_ONLINE
                val podeAtualizar = statusAtual == ThemeManager.STATUS_ONLINE ||
                        statusAtual == ThemeManager.STATUS_AUSENTE
                if (podeAtualizar && statusAtual != novoStatus) {
                    db.collection("usuarios").document(uid).update("status", novoStatus)
                }
            }
    }

    private fun aplicarTema() {
        val root = findViewById<ViewGroup>(android.R.id.content) ?: return
        val altoContraste = AcessibilidadePrefs.isAltoContraste(this)

        // 1) Background
        if (altoContraste) {
            root.setBackgroundColor(Color.BLACK)
        } else {
            ThemeManager.aplicarBackground(this, root)
        }

        if (root.childCount > 0) {
            val firstChild = root.getChildAt(0)

            if (altoContraste) {
                firstChild.setBackgroundColor(Color.BLACK)
            } else {
                ThemeManager.aplicarBackground(this, firstChild)
            }

            // 2) Cores de botões (mas ThemeManager já pula se alto contraste)
            ThemeManager.aplicarCores(this, firstChild)

            // 3) Texto
            if (altoContraste) {
                ThemeManager.aplicarAltoContraste(firstChild)
            } else {
                ThemeManager.aplicarCoresTexto(this, firstChild)
            }
        }
    }

    private fun aplicarInsets() {
        val root = findViewById<ViewGroup>(android.R.id.content)
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val sysBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())

            view.updatePadding(
                left = sysBars.left,
                top = sysBars.top,
                right = sysBars.right,
                bottom = maxOf(sysBars.bottom, ime.bottom)
            )

            WindowInsetsCompat.CONSUMED
        }
        ViewCompat.requestApplyInsets(root)
    }

    protected fun trocarTema(novoTema: String) {
        ThemeManager.setTema(this, novoTema)
        recreate()
    }
}