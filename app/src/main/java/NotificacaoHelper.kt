package com.example.plataformaremota

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object NotificacaoHelper {

    private val REST_API_KEY = BuildConfig.ONESIGNAL_REST_API_KEY
    private const val APP_ID = "eb63f7b4-c19e-4a5a-8a69-ecf5bc8413db"

    suspend fun enviar(
        uidDestino: String,
        titulo: String,
        mensagem: String,
        chatId: String = ""
    ) {
        if (REST_API_KEY.isEmpty()) {
            Log.e("ONESIGNAL", "REST_API_KEY não configurada")
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

                val grupoId = if (chatId.isNotEmpty()) "chat_$chatId" else "app"
                val dataJson = if (chatId.isNotEmpty()) ""","data":{"chat_id":"$chatId"}""" else ""

                val jsonBody = """
                    {
                        "app_id": "$APP_ID",
                        "include_aliases": {"external_id": ["$uidDestino"]},
                        "target_channel": "push",
                        "headings": {"en": "$titulo"},
                        "contents": {"en": "$mensagem"},
                        "android_group": "$grupoId",
                        "thread_id": "$grupoId"$dataJson
                    }
                """.trimIndent()

                val os = conn.outputStream
                val writer = OutputStreamWriter(os, "UTF-8")
                writer.write(jsonBody)
                writer.flush()
                writer.close()

                Log.d("ONESIGNAL", "Resposta: ${conn.responseCode}")
                conn.disconnect()
            } catch (e: Exception) {
                Log.e("ONESIGNAL", "Erro: ${e.message}")
            }
        }
    }

    suspend fun enviarParaVarios(
        uidsDestino: List<String>,
        titulo: String,
        mensagem: String,
        chatId: String = ""
    ) {
        if (uidsDestino.isEmpty() || REST_API_KEY.isEmpty()) return

        withContext(Dispatchers.IO) {
            try {
                val url = URL("https://api.onesignal.com/notifications")
                val conn = url.openConnection() as HttpURLConnection

                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                conn.setRequestProperty("Authorization", "Key $REST_API_KEY")
                conn.doOutput = true

                val uidsJson = uidsDestino.joinToString(",") { "\"$it\"" }
                val grupoId = if (chatId.isNotEmpty()) "chat_$chatId" else "app"
                val dataJson = if (chatId.isNotEmpty()) ""","data":{"chat_id":"$chatId"}""" else ""

                val jsonBody = """
                    {
                        "app_id": "$APP_ID",
                        "include_aliases": {"external_id": [$uidsJson]},
                        "target_channel": "push",
                        "headings": {"en": "$titulo"},
                        "contents": {"en": "$mensagem"},
                        "android_group": "$grupoId",
                        "thread_id": "$grupoId"$dataJson
                    }
                """.trimIndent()

                val os = conn.outputStream
                val writer = OutputStreamWriter(os, "UTF-8")
                writer.write(jsonBody)
                writer.flush()
                writer.close()

                Log.d("ONESIGNAL", "Resposta em massa: ${conn.responseCode}")
                conn.disconnect()
            } catch (e: Exception) {
                Log.e("ONESIGNAL", "Erro: ${e.message}")
            }
        }
    }

    // ⭐ Notificação in-app (salva no Firestore)
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
            Log.e("NOTIF", "Erro in-app: ${e.message}")
        }
    }
}