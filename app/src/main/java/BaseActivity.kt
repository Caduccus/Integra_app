package com.example.plataformaremota

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.BLACK

        if (!CloudinaryManager.iniciado) {
            val config = HashMap<String, String>()
            config["cloud_name"] = "qatmo4ge"
            MediaManager.init(this, config)
            CloudinaryManager.iniciado = true
        }

        auth.currentUser?.uid?.let { uid -> OneSignal.login(uid) }
    }

    override fun onContentChanged() {
        super.onContentChanged()
        aplicarTema()
        aplicarInsets()
    }

    override fun onResume() {
        super.onResume()
        atualizarStatusAutomatico(online = true)
    }

    override fun onPause() {
        super.onPause()
        atualizarStatusAutomatico(online = false)
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
        val root = findViewById<ViewGroup>(android.R.id.content)

        // ⭐ Aplica fundo no ROOT também (cobre a área do IME quando teclado abre)
        ThemeManager.aplicarBackground(this, root)

        if (root.childCount > 0) {
            val firstChild = root.getChildAt(0)
            ThemeManager.aplicarBackground(this, firstChild)
            ThemeManager.aplicarCores(this, firstChild)
            ThemeManager.aplicarCoresTexto(this, firstChild)
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

            // ⭐ Marca como consumido — impede os filhos de aplicarem de novo
            WindowInsetsCompat.CONSUMED
        }
        ViewCompat.requestApplyInsets(root)
    }

    protected fun trocarTema(novoTema: String) {
        ThemeManager.setTema(this, novoTema)
        recreate()
    }
}