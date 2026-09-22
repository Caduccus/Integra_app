package com.example.plataformaremota

import android.os.Bundle
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager

open class BaseActivity : AppCompatActivity() {

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

    /**
     * Aplica as cores em um fragment (chamado pelos próprios fragments)
     */
    protected fun aplicarCoresNoFragment(rootView: android.view.View) {
        ThemeManager.aplicarCores(this, rootView)
    }

    protected fun trocarTema(novoTema: String) {
        ThemeManager.setTema(this, novoTema)
        recreate()
    }
}