package com.example.plataformaremota

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object NotificacaoHelper {

    // ⚠️ COLOQUE A URL DO SEU CLOUDFLARE WORKER AQUI
    private const val CLOUDFLARE_WORKER_URL = "https://integra-notificacoes.jefobolo.workers.dev"

    suspend fun salvarInApp(
        uidDestino: String,
        titulo: String,
        mensagem: String,
        tipo: String = "geral",
        refId: String = ""
    ) {
        try {
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val notif = hashMapOf(
                "titulo" to titulo,
                "mensagem" to mensagem,
                "tipo" to tipo,
                "refId" to refId,
                "lida" to false,
                "timestamp" to System.currentTimeMillis()
            )
            db.collection("usuarios").document(uidDestino)
                .collection("notificacoes").add(notif)
        } catch (e: Exception) {
            Log.e("NOTIF_IN_APP", "Erro ao salvar: ${e.message}")
        }
    }

    suspend fun enviarPush(
        uidDestino: String,
        titulo: String,
        mensagem: String,
        chatId: String = ""
    ) {
        withContext(Dispatchers.IO) {
            try {
                Log.d("TESTE_NOTIF", "5. Entrou no enviarPush. URL: $CLOUDFLARE_WORKER_URL")

                val url = URL(CLOUDFLARE_WORKER_URL)
                val conn = url.openConnection() as HttpURLConnection

                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                conn.doOutput = true

                // Criando o JSON de forma segura
                val jsonBody = JSONObject().apply {
                    put("uidDestino", uidDestino)
                    put("titulo", titulo)
                    put("mensagem", mensagem)
                    put("chatId", chatId)
                }.toString()

                Log.d("TESTE_NOTIF", "6. JSON montado, enviando requisição HTTP...")

                val os = conn.outputStream
                val writer = OutputStreamWriter(os, "UTF-8")
                writer.write(jsonBody)
                writer.flush()
                writer.close()

                val responseCode = conn.responseCode
                Log.d("TESTE_NOTIF", "7. Resposta do Cloudflare: HTTP $responseCode")

                if (responseCode !in 200..299) {
                    Log.e("CLOUDFLARE_PUSH", "Falha no envio: $responseCode - ${conn.errorStream?.bufferedReader()?.readText()}")
                } else {
                    Log.d("CLOUDFLARE_PUSH", "Push enviado com sucesso via Cloudflare para $uidDestino")
                }
                conn.disconnect()
            } catch (e: Exception) {
                Log.e("TESTE_NOTIF", "❌ ERRO FATAL NO ENVIO: ${e.message}", e)
            }
        }
    }

    suspend fun notificar(
        uidDestino: String,
        titulo: String,
        mensagem: String,
        tipo: String = "geral",
        refId: String = "",
        chatId: String = ""
    ) {
        salvarInApp(uidDestino, titulo, mensagem, tipo, refId)
        enviarPush(uidDestino, titulo, mensagem, chatId)
    }

    // Compatibilidade com código antigo
    suspend fun enviar(
        uidDestino: String,
        titulo: String,
        mensagem: String,
        chatId: String = ""
    ) {
        notificar(uidDestino, titulo, mensagem, "geral", "", chatId)
    }

    suspend fun enviarParaVarios(
        uidsDestino: List<String>,
        titulo: String,
        mensagem: String,
        chatId: String = ""
    ) {
        for (uid in uidsDestino) {
            enviarPush(uid, titulo, mensagem, chatId)
        }
    }
}