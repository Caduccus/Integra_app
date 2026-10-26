package com.example.plataformaremota

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.max
import kotlin.random.Random

class AudioWaveformView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Cores padrão (pode ser alterada via código)
    var corTocada: Int = 0xFF43A047.toInt() // Verde
    var corNaoTocada: Int = 0x66FFFFFF     // Branco transparente

    private val paintPlayed = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL }
    private val paintUnplayed = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL }

    private var amplitudes: FloatArray = FloatArray(35) { Random.nextFloat() * 0.8f + 0.2f }
    private var progress: Float = 0f
    private val barWidth = 4f
    private val barGap = 4f

    // Para gravação em tempo real
    private var isLiveRecording = false

    fun setProgress(progresso: Float) {
        this.progress = progresso.coerceIn(0f, 1f)
        invalidate()
    }

    fun iniciarGravacaoAoVivo() {
        isLiveRecording = true
        amplitudes = FloatArray(35) { 0.1f }
        invalidate()
    }

    fun adicionarAmplitude(amplitudeNormalizada: Float) {
        if (!isLiveRecording) return
        // Empurra as ondas para a esquerda e adiciona a nova na direita
        for (i in 0 until amplitudes.size - 1) {
            amplitudes[i] = amplitudes[i + 1]
        }
        amplitudes[amplitudes.size - 1] = max(0.1f, amplitudeNormalizada)
        progress = 1f // Fica tudo verde enquanto grava
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        paintPlayed.color = corTocada
        paintUnplayed.color = corNaoTocada

        val centerY = height / 2f
        val totalBars = amplitudes.size
        val totalWidth = totalBars * (barWidth + barGap)
        var startX = (width - totalWidth) / 2f

        for (i in 0 until totalBars) {
            val amp = amplitudes[i]
            val barHeight = (amp * height * 0.8f).coerceAtLeast(6f)

            val top = centerY - (barHeight / 2f)
            val bottom = centerY + (barHeight / 2f)

            val rect = RectF(startX, top, startX + barWidth, bottom)

            val barProgress = i.toFloat() / totalBars
            val paint = if (isLiveRecording || barProgress <= progress) paintPlayed else paintUnplayed

            canvas.drawRoundRect(rect, barWidth / 2, barWidth / 2, paint)
            startX += barWidth + barGap
        }
    }
}