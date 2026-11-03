package com.example.plataformaremota

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object NotificacaoHelper {

    // ⚠️ Lembre-se de colocar a chave no local.properties
    private val REST_API_KEY = BuildConfig.ONESIGNAL_REST_API_KEY
    private const val APP_ID = "d2bb6774-9d05-4241-8e20-b78a7a325119" // Substitua pelo seu novo App ID

    // Salva no banco para aparecer na aba "Notificações" da Home
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

    // Envia o Push pelo OneSignal (Funciona com o app aberto ou fechado)
    suspend fun enviarPush(
        uidDestino: String,
        titulo: String,
        mensagem: String,
        chatId: String = ""
    ) {
        if (REST_API_KEY.isEmpty()) {
            Log.e("ONESIGNAL", "ERRO: REST_API_KEY está vazia no local.properties!")
            return
        }

        withContext(Dispatchers.IO) {
            try {
                val url = URL("https://api.onesignal.com/notifications")
                val conn = url.openConnection() as HttpURLConnection

                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                conn.setRequestProperty("Authorization", "Key $REST_API_KEY")
                conn.doOutput = true

                val dataJson = if (chatId.isNotEmpty()) ""","data":{"chat_id":"$chatId"}""" else ""

                val jsonBody = """
                    {
                        "app_id": "$APP_ID",
                        "include_aliases": {"external_id": ["$uidDestino"]},
                        "target_channel": "push",
                        "headings": {"en": "$titulo", "pt": "$titulo"},
                        "contents": {"en": "$mensagem", "pt": "$mensagem"},
                        "android_group": "chat_$chatId"$dataJson
                    }
                """.trimIndent()

                val os = conn.outputStream
                val writer = OutputStreamWriter(os, "UTF-8")
                writer.write(jsonBody)
                writer.flush()
                writer.close()

                val responseCode = conn.responseCode
                if (responseCode !in 200..299) {
                    Log.e("ONESIGNAL", "Falha no envio: $responseCode - ${conn.errorStream?.bufferedReader()?.readText()}")
                } else {
                    Log.d("ONESIGNAL", "Push enviado com sucesso para $uidDestino")
                }
                conn.disconnect()
            } catch (e: Exception) {
                Log.e("ONESIGNAL", "Exceção ao enviar push: ${e.message}")
            }
        }
    }

    // Função unificada: Salva no banco E envia o push
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