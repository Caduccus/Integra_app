package com.example.plataformaremota

import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.cloudinary.android.MediaManager
import com.onesignal.OneSignal

open class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Status bar preta
        window.statusBarColor = Color.BLACK

        // Inicializa Cloudinary (só uma vez)
        if (!CloudinaryManager.iniciado) {
            val config = HashMap<String, String>()
            config["cloud_name"] = "qatmo4ge"
            MediaManager.init(this, config)
            CloudinaryManager.iniciado = true
        }

        // ⭐ Associa o usuário logado ao OneSignal
        com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid?.let { uid ->
            OneSignal.login(uid)
        }
    }

    override fun onContentChanged() {
        super.onContentChanged()
        aplicarTema()
    }

    private fun aplicarTema() {
        val root = findViewById<ViewGroup>(android.R.id.content)
        if (root.childCount > 0) {
            val firstChild = root.getChildAt(0)
            ThemeManager.aplicarBackground(this, firstChild)
            ThemeManager.aplicarCores(this, firstChild)
        }
    }

    protected fun trocarTema(novoTema: String) {
        ThemeManager.setTema(this, novoTema)
        recreate()
    }
}