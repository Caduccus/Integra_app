package com.example.plataformaremota

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object LanguageManager {

    const val IDIOMA_SISTEMA = "sistema"
    const val IDIOMA_PT = "pt"
    const val IDIOMA_EN = "en"
    const val IDIOMA_ES = "es"

    private const val PREFS = "language_prefs"
    private const val KEY = "idioma_atual"

    fun getIdiomaAtual(context: Context): String {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, IDIOMA_SISTEMA) ?: IDIOMA_SISTEMA
    }

    fun setIdioma(context: Context, idioma: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, idioma).apply()
    }

    fun getNomeAmigavel(idioma: String): String = when (idioma) {
        IDIOMA_PT -> "Português"
        IDIOMA_EN -> "English"
        IDIOMA_ES -> "Español"
        else -> "Sistema"
    }

    // Aplica o idioma na config do Context
    fun aplicarIdioma(context: Context): Context {
        val idiomaSalvo = getIdiomaAtual(context)
        if (idiomaSalvo == IDIOMA_SISTEMA) return context

        val locale = Locale(idiomaSalvo)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)

        return context.createConfigurationContext(config)
    }
}