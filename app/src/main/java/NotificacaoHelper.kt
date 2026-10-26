package com.example.plataformaremota

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
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
        if (REST_API_KEY.isEmpty()) return

        withContext(Dispatchers.IO) {
            try {
                // ⭐ Busca o idioma do destinatário no Firestore
                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                var idiomaDest = "pt"
                try {
                    val doc = db.collection("usuarios").document(uidDestino).get().await()
                    idiomaDest = doc.getString("idioma") ?: "pt"
                } catch (_: Exception) {}

                val tituloTraduzido = traduzirTitulo(titulo, idiomaDest)
                val msgTraduzida = traduzirMensagem(mensagem, idiomaDest)

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
                        "headings": {"en": "$tituloTraduzido"},
                        "contents": {"en": "$msgTraduzida"},
                        "android_group": "$grupoId",
                        "thread_id": "$grupoId"$dataJson
                    }
                """.trimIndent()

                val os = conn.outputStream
                val writer = OutputStreamWriter(os, "UTF-8")
                writer.write(jsonBody)
                writer.flush()
                writer.close()

                conn.disconnect()
            } catch (e: Exception) {
                Log.e("ONESIGNAL", "Erro: ${e.message}")
            }
        }
    }

    private fun traduzirTitulo(titulo: String, idioma: String): String {
        if (idioma == "pt" || idioma == "sistema") return titulo
        return when(titulo) {
            "Nova mensagem" -> if (idioma == "en") "New message" else "Nuevo mensaje"
            "Nova mensagem no grupo" -> if (idioma == "en") "New group message" else "Nuevo mensaje en el grupo"
            "Nova candidatura! 🎯" -> if (idioma == "en") "New application! 🎯" else "¡Nueva candidatura! 🎯"
            "Candidatura enviada ✅" -> if (idioma == "en") "Application sent ✅" else "Candidatura enviada ✅"
            "Você foi aceito! 🎉" -> if (idioma == "en") "You were accepted! 🎉" else "¡Fuiste aceptado! 🎉"
            "Candidatura rejeitada" -> if (idioma == "en") "Application rejected" else "Candidatura rechazada"
            "Novo pedido para entrar 🏢" -> if (idioma == "en") "New join request 🏢" else "Nueva solicitud para entrar 🏢"
            "Bem-vindo à empresa! 🎉" -> if (idioma == "en") "Welcome to the company! 🎉" else "¡Bienvenido a la empresa! 🎉"
            "Trabalho encerrado" -> if (idioma == "en") "Job closed" else "Trabajo cerrado"
            "Grupo apagado" -> if (idioma == "en") "Group deleted" else "Grupo eliminado"
            "Empresa excluída" -> if (idioma == "en") "Company deleted" else "Empresa eliminada"
            else -> titulo
        }
    }

    private fun traduzirMensagem(msg: String, idioma: String): String {
        if (idioma == "pt" || idioma == "sistema") return msg
        if (msg.startsWith("O trabalho '")) {
            return if (idioma == "en") msg.replace("O trabalho '", "The job '").replace("' foi excluído", "' was deleted")
            else msg.replace("O trabalho '", "El trabajo '").replace("' foi excluído", "' fue eliminado")
        }
        if (msg.startsWith("A empresa '")) {
            return if (idioma == "en") msg.replace("A empresa '", "The company '").replace("' foi excluída", "' was deleted")
            else msg.replace("A empresa '", "La empresa '").replace("' foi excluída", "' fue eliminada")
        }
        return msg
    }

    suspend fun enviarParaVarios(
        uidsDestino: List<String>,
        titulo: String,
        mensagem: String,
        chatId: String = ""
    ) {
        for (uid in uidsDestino) {
            enviar(uid, titulo, mensagem, chatId)
        }
    }

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
        } catch (e: Exception) {}
    }
}