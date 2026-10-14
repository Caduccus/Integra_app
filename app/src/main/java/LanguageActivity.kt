package com.example.plataformaremota

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog

class LanguageActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_language)

        findViewById<ImageView>(R.id.btnVoltarIdioma).setOnClickListener { finish() }

        findViewById<android.view.View>(R.id.opIdiomaSistema).setOnClickListener {
            HapticHelper.leve(this)
            trocarIdioma(LanguageManager.IDIOMA_SISTEMA)
        }
        findViewById<android.view.View>(R.id.opIdiomaPT).setOnClickListener {
            HapticHelper.leve(this)
            trocarIdioma(LanguageManager.IDIOMA_PT)
        }
        findViewById<android.view.View>(R.id.opIdiomaEN).setOnClickListener {
            HapticHelper.leve(this)
            trocarIdioma(LanguageManager.IDIOMA_EN)
        }
        findViewById<android.view.View>(R.id.opIdiomaES).setOnClickListener {
            HapticHelper.leve(this)
            trocarIdioma(LanguageManager.IDIOMA_ES)
        }
    }

    private fun trocarIdioma(idioma: String) {
        LanguageManager.setIdioma(this, idioma)

        AlertDialog.Builder(this)
            .setTitle("Aplicar idioma")
            .setMessage("O app vai reiniciar para aplicar.")
            .setPositiveButton("Reiniciar") { _, _ ->
                val intent = Intent(this, SplashActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                Runtime.getRuntime().exit(0)
            }
            .setCancelable(false)
            .show()
    }
}