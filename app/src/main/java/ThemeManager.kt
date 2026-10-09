package com.example.plataformaremota

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputLayout

object ThemeManager {

    const val TEMA_PADRAO = "padrao"
    const val TEMA_VERMELHO = "vermelho"
    const val TEMA_AZUL = "azul"
    const val TEMA_VERDE = "verde"
    const val TEMA_PRETO = "preto"
    const val TEMA_BRANCO = "branco"
    const val TEMA_AMARELO = "amarelo"

    private const val PREFS = "tema_prefs"
    private const val KEY = "tema_atual"

    private const val COR_PADRAO_BOTAO = 0xFF0D226B.toInt()

    private const val TEXTO_ESCURO = 0xFF212121.toInt()
    private const val TEXTO_ESCURO_SEC = 0xFF616161.toInt()
    private const val TEXTO_MARROM = 0xFF4E342E.toInt()
    private const val TEXTO_MARROM_SEC = 0xFF6D4C41.toInt()
    private const val TEXTO_BRANCO = 0xFFFFFFFF.toInt()

    private const val COR_CARD_ALTO_CONTRASTE = 0xFF1A1A1A.toInt()

    private val CORES_FIXAS = listOf(
        0xFFD32F2F.toInt(), 0xFF43A047.toInt(), 0xFF1E88E5.toInt(),
        0xFFFB8C00.toInt(), 0xFF8E24AA.toInt(), 0xFF00897B.toInt(),
        0xFF757575.toInt(), 0xFF7F0000.toInt(), 0xFF455A64.toInt()
    )

    // ─────────────────────────────────────────────
    // TEMA
    // ─────────────────────────────────────────────
    fun getTemaAtual(context: Context): String {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, TEMA_PADRAO) ?: TEMA_PADRAO
    }

    fun setTema(context: Context, tema: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, tema).apply()
    }

    fun getBackground(tema: String): Int = when (tema) {
        TEMA_VERMELHO -> R.drawable.background_red
        TEMA_AZUL -> R.drawable.background_blue
        TEMA_VERDE -> R.drawable.background_green
        TEMA_PRETO -> R.drawable.background_preto
        TEMA_BRANCO -> R.drawable.background_branco
        TEMA_AMARELO -> R.drawable.background_amarelo
        else -> R.drawable.background_gradiente
    }

    fun getPrimary(tema: String): Int = when (tema) {
        TEMA_VERMELHO -> 0xFFB71C1C.toInt()
        TEMA_AZUL -> 0xFF1A237E.toInt()
        TEMA_VERDE -> 0xFF1B5E20.toInt()
        TEMA_PRETO -> 0xFF212121.toInt()
        TEMA_BRANCO -> 0xFF37474F.toInt()
        TEMA_AMARELO -> 0xFF6D4C41.toInt()
        else -> COR_PADRAO_BOTAO
    }

    fun getTextColor(tema: String): Int = when (tema) {
        TEMA_BRANCO -> TEXTO_ESCURO
        TEMA_AMARELO -> TEXTO_MARROM
        else -> TEXTO_BRANCO
    }

    fun getSecondaryTextColor(tema: String): Int = when (tema) {
        TEMA_BRANCO -> TEXTO_ESCURO_SEC
        TEMA_AMARELO -> TEXTO_MARROM_SEC
        else -> 0xB3FFFFFF.toInt()
    }

    fun getBubbleMinhaColor(tema: String): Int = when (tema) {
        TEMA_VERMELHO -> 0xFFB71C1C.toInt()
        TEMA_AZUL -> 0xFF1A237E.toInt()
        TEMA_VERDE -> 0xFF1B5E20.toInt()
        TEMA_PRETO -> 0xFF424242.toInt()
        TEMA_BRANCO -> 0xFF455A64.toInt()
        TEMA_AMARELO -> 0xFF6D4C41.toInt()
        else -> 0xFF0D226B.toInt()
    }

    fun getBubbleOutraColor(tema: String): Int = 0xFFF2FFFFFF.toInt()
    fun getBubbleMinhaTextColor(tema: String): Int = Color.WHITE
    fun getBubbleOutraTextColor(tema: String): Int = 0xFF333333.toInt()
    fun getBubbleMinhaHoraColor(tema: String): Int = 0xB3FFFFFF.toInt()
    fun getBubbleOutraHoraColor(tema: String): Int = 0xFF888888.toInt()

    fun getNomeAmigavel(tema: String): String = when (tema) {
        TEMA_VERMELHO -> "Vermelho"
        TEMA_AZUL -> "Azul"
        TEMA_VERDE -> "Verde"
        TEMA_PRETO -> "Preto"
        TEMA_BRANCO -> "Branco"
        TEMA_AMARELO -> "Amarelo"
        else -> "Padrão (Roxo)"
    }

    fun aplicarBackground(context: Context, view: View) {
        val tema = getTemaAtual(context)
        view.setBackgroundResource(getBackground(tema))
    }

    // ─────────────────────────────────────────────
    // ⭐ APLICAR CORES (botões)
    // ─────────────────────────────────────────────
    fun aplicarCores(context: Context, rootView: View) {
        // Se alto contraste, quem manda é o aplicarAltoContraste
        if (AcessibilidadePrefs.isAltoContraste(context)) return

        val tema = getTemaAtual(context)
        val primary = getPrimary(tema)
        aplicarCoresRecursivo(rootView, primary)
    }

    private fun aplicarCoresRecursivo(view: View, primary: Int) {
        when (view) {
            is MaterialButton -> {
                val atual = view.backgroundTintList?.defaultColor
                val deveTrocar = atual != null &&
                        atual == COR_PADRAO_BOTAO &&
                        atual !in CORES_FIXAS
                if (deveTrocar || atual == null) {
                    view.backgroundTintList = ColorStateList.valueOf(primary)
                }
            }
            is ViewGroup -> {
                for (i in 0 until view.childCount) {
                    aplicarCoresRecursivo(view.getChildAt(i), primary)
                }
            }
        }
    }

    // ─────────────────────────────────────────────
    // ⭐ APLICAR CORES DE TEXTO (só tema claro)
    // ─────────────────────────────────────────────
    fun aplicarCoresTexto(context: Context, rootView: View) {
        // Alto contraste manda — não pinta
        if (AcessibilidadePrefs.isAltoContraste(context)) return

        val tema = getTemaAtual(context)
        if (tema != TEMA_BRANCO && tema != TEMA_AMARELO) return

        val primaryText = getTextColor(tema)
        val secondaryText = getSecondaryTextColor(tema)

        aplicarTextoForcado(rootView, primaryText, secondaryText)

        rootView.post {
            aplicarTextoForcado(rootView, primaryText, secondaryText)
        }
    }

    private fun aplicarTextoForcado(view: View, primaryText: Int, secondaryText: Int) {
        if (view is BottomNavigationView) {
            val states = arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf()
            )
            val colors = intArrayOf(TEXTO_BRANCO, TEXTO_BRANCO)
            val colorStateList = ColorStateList(states, colors)
            view.itemIconTintList = colorStateList
            view.itemTextColor = colorStateList
            return
        }

        if (view is TextInputLayout) {
            try {
                val states = arrayOf(
                    intArrayOf(android.R.attr.state_focused),
                    intArrayOf()
                )
                val colors = intArrayOf(primaryText, secondaryText)
                view.setBoxStrokeColorStateList(ColorStateList(states, colors))
                view.boxStrokeWidth = 2
                view.boxStrokeWidthFocused = 3
                view.setHintTextColor(ColorStateList.valueOf(secondaryText))
                view.defaultHintTextColor = ColorStateList.valueOf(secondaryText)
                view.setStartIconTintList(ColorStateList.valueOf(primaryText))
                view.setEndIconTintList(ColorStateList.valueOf(primaryText))
                view.getEditText()?.let { et ->
                    et.setTextColor(primaryText)
                    et.setHintTextColor(secondaryText)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (view is MaterialButton) {
            val corFundo = view.backgroundTintList?.defaultColor ?: Color.TRANSPARENT
            val alpha = Color.alpha(corFundo)
            if (alpha < 50) {
                view.setTextColor(primaryText)
            } else {
                view.setTextColor(Color.WHITE)
            }
            return
        }

        if (view is ImageView) {
            if (view.imageTintList != null) {
                view.imageTintList = ColorStateList.valueOf(primaryText)
            }
        }

        if (view is TextView) {
            view.setTextColor(primaryText)
        }

        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                aplicarTextoForcado(view.getChildAt(i), primaryText, secondaryText)
            }
        }
    }

    // ─────────────────────────────────────────────
    // ⭐ ALTO CONTRASTE
    // ─────────────────────────────────────────────
    fun aplicarAltoContraste(rootView: View) {
        aplicarAltoContrasteRecursivo(rootView)

        // ⭐ Re-aplica depois do layout — pega views que foram infladas
        //   depois (fragments, listas, etc)
        rootView.post {
            aplicarAltoContrasteRecursivo(rootView)
        }
    }

    private fun aplicarAltoContrasteRecursivo(view: View) {
        when (view) {
            is BottomNavigationView -> {
                val states = arrayOf(
                    intArrayOf(android.R.attr.state_checked),
                    intArrayOf()
                )
                val colors = intArrayOf(TEXTO_BRANCO, TEXTO_BRANCO)
                val csl = ColorStateList(states, colors)
                view.itemIconTintList = csl
                view.itemTextColor = csl
                return
            }

            is TextInputLayout -> {
                try {
                    view.setBoxStrokeColorStateList(ColorStateList.valueOf(Color.WHITE))
                    view.boxStrokeWidth = 2
                    view.boxStrokeWidthFocused = 3
                    view.setHintTextColor(ColorStateList.valueOf(Color.WHITE))
                    view.defaultHintTextColor = ColorStateList.valueOf(Color.WHITE)
                    view.setStartIconTintList(ColorStateList.valueOf(Color.WHITE))
                    view.setEndIconTintList(ColorStateList.valueOf(Color.WHITE))
                    view.getEditText()?.let { et ->
                        et.setTextColor(Color.WHITE)
                        et.setHintTextColor(Color.WHITE)
                    }
                } catch (_: Exception) { }
            }

            // ⭐ Card → fundo escuro sólido
            is MaterialCardView -> {
                try {
                    view.setCardBackgroundColor(COR_CARD_ALTO_CONTRASTE)
                } catch (_: Exception) { }
            }

            is MaterialButton -> {
                view.setTextColor(Color.WHITE)
                view.iconTint = ColorStateList.valueOf(Color.WHITE)
                return
            }

            is ImageView -> {
                if (view.imageTintList != null) {
                    view.imageTintList = ColorStateList.valueOf(Color.WHITE)
                }
            }

            is TextView -> {
                view.setTextColor(Color.WHITE)
            }
        }

        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                aplicarAltoContrasteRecursivo(view.getChildAt(i))
            }
        }
    }
    // ─────────────────────────────────────────────
    // STATUS (online / ausente / nao_pertube / invisivel)
    // ─────────────────────────────────────────────
    const val STATUS_ONLINE = "online"
    const val STATUS_AUSENTE = "ausente"
    const val STATUS_NAO_PERTUBE = "nao_pertube"
    const val STATUS_INVISIVEL = "invisivel"

    fun getStatusColor(status: String): Int = when (status) {
        STATUS_ONLINE      -> 0xFF4CAF50.toInt()
        STATUS_AUSENTE     -> 0xFFFFC107.toInt()
        STATUS_NAO_PERTUBE -> 0xFFF44336.toInt()
        STATUS_INVISIVEL   -> 0xFF9E9E9E.toInt()
        else               -> 0xFF9E9E9E.toInt()
    }

    fun getStatusNomeAmigavel(status: String): String = when (status) {
        STATUS_ONLINE      -> "Online"
        STATUS_AUSENTE     -> "Ausente"
        STATUS_NAO_PERTUBE -> "Não perturbe"
        STATUS_INVISIVEL   -> "Invisível"
        else               -> "Online"
    }
}