package com.example.plataformaremota

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.cloudinary.android.MediaManager
import com.onesignal.OneSignal

open class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ⭐ Edge-to-edge + insets manuais (Android 15+)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.BLACK

        if (!CloudinaryManager.iniciado) {
            val config = HashMap<String, String>()
            config["cloud_name"] = "qatmo4ge"
            MediaManager.init(this, config)
            CloudinaryManager.iniciado = true
        }

        com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid?.let { uid ->
            OneSignal.login(uid)
        }
    }

    override fun onContentChanged() {
        super.onContentChanged()
        aplicarTema()
        aplicarInsets()
    }

    private fun aplicarTema() {
        val root = findViewById<ViewGroup>(android.R.id.content)
        if (root.childCount > 0) {
            val firstChild = root.getChildAt(0)
            ThemeManager.aplicarBackground(this, firstChild)
            ThemeManager.aplicarCores(this, firstChild)
            ThemeManager.aplicarCoresTexto(this, firstChild)
        }
    }

    // ⭐ Aplica padding de status bar + nav bar + IME (teclado)
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
            insets
        }
        ViewCompat.requestApplyInsets(root)
    }

    protected fun trocarTema(novoTema: String) {
        ThemeManager.setTema(this, novoTema)
        recreate()
    }
}