package com.example.plataformaremota

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.material.button.MaterialButton

object ThemeManager {

    const val TEMA_PADRAO = "padrao"
    const val TEMA_VERMELHO = "vermelho"
    const val TEMA_AZUL = "azul"
    const val TEMA_VERDE = "verde"

    private const val PREFS = "tema_prefs"
    private const val KEY = "tema_atual"

    // ⭐ Cor padrão dos botões que vão MUDAR conforme o tema
    private const val COR_PADRAO_BOTAO = 0xFF0D226B.toInt()

    // Cores especiais que NÃO mudam (vermelho = exclusão, verde = chat)
    private val CORES_FIXAS = listOf(
        0xFFD32F2F.toInt(), // Vermelho destrutivo
        0xFF43A047.toInt(), // Verde chat
        0xFF1E88E5.toInt(), // Azul médio (chips)
        0xFFFB8C00.toInt(), // Laranja (chips)
        0xFF8E24AA.toInt(), // Roxo (chips)
        0xFF00897B.toInt(), // Verde-água (chips)
        0xFF757575.toInt()  // Cinza
    )

    fun getTemaAtual(context: Context): String {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, TEMA_PADRAO) ?: TEMA_PADRAO
    }

    fun setTema(context: Context, tema: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, tema).apply()
    }

    fun getBackground(tema: String): Int {
        return when (tema) {
            TEMA_VERMELHO -> R.drawable.background_red
            TEMA_AZUL -> R.drawable.background_blue
            TEMA_VERDE -> R.drawable.background_green
            else -> R.drawable.background_gradiente
        }
    }

    fun getPrimary(tema: String): Int {
        return when (tema) {
            TEMA_VERMELHO -> 0xFFB71C1C.toInt()
            TEMA_AZUL -> 0xFF1A237E.toInt()
            TEMA_VERDE -> 0xFF1B5E20.toInt()
            else -> COR_PADRAO_BOTAO
        }
    }

    fun getNomeAmigavel(tema: String): String {
        return when (tema) {
            TEMA_VERMELHO -> "Vermelho"
            TEMA_AZUL -> "Azul"
            TEMA_VERDE -> "Verde"
            else -> "Padrão (Roxo)"
        }
    }

    /**
     * Aplica o background no root view da tela
     */
    fun aplicarBackground(context: Context, view: View) {
        val tema = getTemaAtual(context)
        view.setBackgroundResource(getBackground(tema))
    }

    /**
     * Aplica a cor primária em todos os botões/views que usam a cor padrão
     */
    fun aplicarCores(context: Context, rootView: View) {
        val tema = getTemaAtual(context)
        val primary = getPrimary(tema)
        aplicarCoresRecursivo(rootView, primary)
    }

    private fun aplicarCoresRecursivo(view: View, primary: Int) {
        when (view) {
            is MaterialButton -> {
                val atual = view.backgroundTintList?.defaultColor

                // Só troca se for a cor padrão (azul escuro 0D226B)
                // E não for uma cor especial (vermelho, verde, etc)
                val deveTrocar = atual != null &&
                        atual == COR_PADRAO_BOTAO &&
                        atual !in CORES_FIXAS

                if (deveTrocar || atual == null) {
                    // Aplica a cor primária
                    view.backgroundTintList = ColorStateList.valueOf(primary)

                    // Se for botão outlined (fundo transparente), ajusta só a borda
                    if (atual == null && view.strokeColor != null) {
                        // Nada — mantém a cor original se não era preenchido
                    }
                }
            }
            is ViewGroup -> {
                for (i in 0 until view.childCount) {
                    aplicarCoresRecursivo(view.getChildAt(i), primary)
                }
            }
        }
    }
}