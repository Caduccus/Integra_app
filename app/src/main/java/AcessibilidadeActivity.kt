package com.example.plataformaremota

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
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

        // Estado inicial
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
            pedirReiniciar()
        }

        opGrande.setOnClickListener {
            HapticHelper.leve(this)
            AcessibilidadePrefs.setFontScale(this, AcessibilidadePrefs.FONTE_GRANDE)
            atualizarChecks()
            pedirReiniciar()
        }

        opExtra.setOnClickListener {
            HapticHelper.leve(this)
            AcessibilidadePrefs.setFontScale(this, AcessibilidadePrefs.FONTE_EXTRA)
            atualizarChecks()
            pedirReiniciar()
        }

        switchAnim.setOnCheckedChangeListener { _, isChecked ->
            AcessibilidadePrefs.setReduzirAnimacoes(this, isChecked)
        }

        switchContraste.setOnCheckedChangeListener { _, isChecked ->
            AcessibilidadePrefs.setAltoContraste(this, isChecked)
            recreate()
        }

        ThemeManager.aplicarCores(this, findViewById(android.R.id.content))
        ThemeManager.aplicarCoresTexto(this, findViewById(android.R.id.content))
    }

    private fun pedirReiniciar() {
        AlertDialog.Builder(this)
            .setTitle("Aplicar novo tamanho?")
            .setMessage("O app vai recarregar para aplicar o novo tamanho de fonte.")
            .setPositiveButton("Aplicar") { _, _ ->
                recreate()
            }
            .setCancelable(false)
            .show()
    }
}