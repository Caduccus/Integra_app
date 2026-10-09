package com.example.plataformaremota

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import com.google.android.material.switchmaterial.SwitchMaterial

class AcessibilidadeActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_acessibilidade)

        findViewById<ImageView>(R.id.btnVoltarAcess).setOnClickListener { finish() }

        val opNormal = findViewById<View>(R.id.opFonteNormal)
        val opGrande = findViewById<View>(R.id.opFonteGrande)
        val opExtra = findViewById<View>(R.id.opFonteExtra)
        val checkNormal = findViewById<ImageView>(R.id.checkFonteNormal)
        val checkGrande = findViewById<ImageView>(R.id.checkFonteGrande)
        val checkExtra = findViewById<ImageView>(R.id.checkFonteExtra)

        val switchAnim = findViewById<SwitchMaterial>(R.id.switchAnimacoes)
        val switchContraste = findViewById<SwitchMaterial>(R.id.switchContraste)

        switchAnim.isChecked = AcessibilidadePrefs.isReduzirAnimacoes(this)
        switchContraste.isChecked = AcessibilidadePrefs.isAltoContraste(this)

        fun atualizarChecks() {
            val atual = AcessibilidadePrefs.getFontScale(this)
            checkNormal.visibility = if (atual == AcessibilidadePrefs.FONTE_NORMAL) View.VISIBLE else View.GONE
            checkGrande.visibility = if (atual == AcessibilidadePrefs.FONTE_GRANDE) View.VISIBLE else View.GONE
            checkExtra.visibility  = if (atual == AcessibilidadePrefs.FONTE_EXTRA)  View.VISIBLE else View.GONE
        }
        atualizarChecks()

        opNormal.setOnClickListener {
            HapticHelper.leve(this)
            AcessibilidadePrefs.setFontScale(this, AcessibilidadePrefs.FONTE_NORMAL)
            atualizarChecks()
            pedirReiniciar("Tamanho da fonte")
        }

        opGrande.setOnClickListener {
            HapticHelper.leve(this)
            AcessibilidadePrefs.setFontScale(this, AcessibilidadePrefs.FONTE_GRANDE)
            atualizarChecks()
            pedirReiniciar("Tamanho da fonte")
        }

        opExtra.setOnClickListener {
            HapticHelper.leve(this)
            AcessibilidadePrefs.setFontScale(this, AcessibilidadePrefs.FONTE_EXTRA)
            atualizarChecks()
            pedirReiniciar("Tamanho da fonte")
        }

        // Reduzir animações → aplica direto, sem reiniciar
        switchAnim.setOnCheckedChangeListener { _, isChecked ->
            AcessibilidadePrefs.setReduzirAnimacoes(this, isChecked)
        }

        // ⭐ Alto contraste → precisa reiniciar (cards ficam pintados)
        switchContraste.setOnCheckedChangeListener { _, isChecked ->
            val valorAnterior = AcessibilidadePrefs.isAltoContraste(this)
            if (valorAnterior != isChecked) {
                AcessibilidadePrefs.setAltoContraste(this, isChecked)
                pedirReiniciar("Alto contraste")
            }
        }

        ThemeManager.aplicarCores(this, findViewById(android.R.id.content))
        ThemeManager.aplicarCoresTexto(this, findViewById(android.R.id.content))
    }

    private fun pedirReiniciar(motivo: String) {
        AlertDialog.Builder(this)
            .setTitle("Aplicar $motivo?")
            .setMessage("O app vai reiniciar para aplicar a mudança em todas as telas.")
            .setPositiveButton("Reiniciar") { _, _ -> reiniciarApp() }
            .setNegativeButton("Depois", null)
            .setCancelable(false)
            .show()
    }

    private fun reiniciarApp() {
        val intent = Intent(this, SplashActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        Runtime.getRuntime().exit(0)
    }
}