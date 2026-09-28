package com.example.plataformaremota

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

class AudioRecorderHelper(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var arquivoAtual: File? = null
    private var inicioMs: Long = 0L

    fun iniciarGravacao(): File? {
        return try {
            val arquivo = File(context.cacheDir, "audio_${System.currentTimeMillis()}.m4a")
            arquivoAtual = arquivo

            recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(96000)
                setOutputFile(arquivo.absolutePath)
                prepare()
                start()
            }

            inicioMs = System.currentTimeMillis()
            arquivo
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun pararGravacao(): Pair<File?, Long> {
        return try {
            val duracao = System.currentTimeMillis() - inicioMs
            recorder?.stop()
            recorder?.release()
            recorder = null
            Pair(arquivoAtual, duracao)
        } catch (e: Exception) {
            recorder?.release()
            recorder = null
            Pair(null, 0L)
        }
    }

    fun cancelar() {
        try {
            recorder?.stop()
        } catch (_: Exception) { }
        recorder?.release()
        recorder = null
        arquivoAtual?.delete()
        arquivoAtual = null
    }
}