package com.example.plataformaremota

import android.content.Context

object AcessibilidadePrefs {

    private const val PREFS = "acessibilidade_prefs"
    private const val KEY_FONT_SCALE = "font_scale"
    private const val KEY_REDUZIR_ANIMACOES = "reduzir_animacoes"
    private const val KEY_ALTO_CONTRASTE = "alto_contraste"

    const val FONTE_NORMAL = "normal"
    const val FONTE_GRANDE = "grande"
    const val FONTE_EXTRA = "extra"

    fun getFontScaleValue(context: Context): Float {
        return when (getFontScale(context)) {
            FONTE_GRANDE -> 1.2f
            FONTE_EXTRA -> 1.4f
            else -> 1f
        }
    }

    fun getFontScale(context: Context): String {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_FONT_SCALE, FONTE_NORMAL) ?: FONTE_NORMAL
    }

    fun setFontScale(context: Context, valor: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_FONT_SCALE, valor).apply()
    }

    fun getNomeFonte(valor: String): String = when (valor) {
        FONTE_GRANDE -> "Grande"
        FONTE_EXTRA -> "Extra grande"
        else -> "Normal"
    }

    fun isReduzirAnimacoes(context: Context): Boolean {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_REDUZIR_ANIMACOES, false)
    }

    fun setReduzirAnimacoes(context: Context, ativo: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_REDUZIR_ANIMACOES, ativo).apply()
    }

    fun isAltoContraste(context: Context): Boolean {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_ALTO_CONTRASTE, false)
    }

    fun setAltoContraste(context: Context, ativo: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ALTO_CONTRASTE, ativo).apply()
    }
}