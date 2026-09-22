package com.example.plataformaremota.data.repository

import android.util.Log
import com.example.plataformaremota.data.entity.Chat
import com.example.plataformaremota.data.entity.Mensagem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class ChatRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val TAG = "CHAT_REPO"

    // ─────────────────────────────────────────────
    // Gera um ID único pra chat 1:1 (inclui o trabalho)
    // Assim, A→B sobre "Trabalho 1" e A→B sobre "Trabalho 2"
    // geram conversas DIFERENTES
    // ─────────────────────────────────────────────
    private fun gerarChatIdUmParaUm(uid1: String, uid2: String, trabalhoId: String): String {
        val ordenados = if (uid1 < uid2) "${uid1}_${uid2}" else "${uid2}_${uid1}"
        return if (trabalhoId.isEmpty()) {
            ordenados
        } else {
            "${ordenados}_${trabalhoId}"
        }
    }

    // ─────────────────────────────────────────────
    // CRIAR (OU BUSCAR) CHAT 1:1
    // Retorna o chatId
    // ─────────────────────────────────────────────
    suspend fun criarOuBuscarChatUmParaUm(
        uidOutro: String,
        nomeOutro: String,
        trabalhoId: String = ""
    ): String? {
        val meuUid = auth.currentUser?.uid ?: return null
        if (uidOutro.isEmpty() || uidOutro == meuUid) return null

        val chatId = gerarChatIdUmParaUm(meuUid, uidOutro, trabalhoId)

        return try {
            val docRef = db.collection("chats").document(chatId)
            val doc = docRef.get().await()

            if (!doc.exists()) {
                val nomeMeu = buscarNomeUsuario(meuUid) ?: "Usuário"

                val chat = Chat(
                    id = chatId,
                    participantes = listOf(meuUid, uidOutro),
                    nome = "",
                    criadorId = meuUid,
                    ehGrupo = false,
                    trabalhoId = trabalhoId,
                    ultimaMensagem = "",
                    ultimaMensagemRemetente = "",
                    timestamp = System.currentTimeMillis()
                )

                docRef.set(chat).await()
                Log.d(TAG, "✅ Chat 1:1 criado: $chatId")
            } else {
                Log.d(TAG, "✅ Chat 1:1 já existia: $chatId")
            }

            chatId
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao criar chat: ${e.message}")
            null
        }
    }

    // ─────────────────────────────────────────────
    // BUSCAR CHAT POR ID
    // ─────────────────────────────────────────────
    suspend fun buscarChat(chatId: String): Chat? {
        return try {
            val doc = db.collection("chats").document(chatId).get().await()
            doc.toObject(Chat::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao buscar chat: ${e.message}")
            null
        }
    }

    // ─────────────────────────────────────────────
    // ENVIAR MENSAGEM
    // ─────────────────────────────────────────────
    suspend fun enviarMensagem(chatId: String, texto: String): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        if (texto.isBlank()) return false

        return try {
            val nome = buscarNomeUsuario(uid) ?: "Usuário"

            val msg = Mensagem(
                remetenteId = uid,
                nomeRemetente = nome,
                texto = texto.trim(),
                timestamp = System.currentTimeMillis()
            )

            // Adiciona mensagem na subcoleção
            db.collection("chats")
                .document(chatId)
                .collection("mensagens")
                .add(msg)
                .await()

            // Atualiza a "última mensagem" no chat
            db.collection("chats").document(chatId).update(
                mapOf(
                    "ultimaMensagem" to texto.trim(),
                    "ultimaMensagemRemetente" to nome,
                    "timestamp" to System.currentTimeMillis()
                )
            ).await()

            Log.d(TAG, "✅ Mensagem enviada")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao enviar mensagem: ${e.message}")
            false
        }
    }

    // ─────────────────────────────────────────────
    // LISTAR MENSAGENS (uma vez)
    // ─────────────────────────────────────────────
    suspend fun listarMensagens(chatId: String): List<Mensagem> {
        return try {
            val snapshot = db.collection("chats")
                .document(chatId)
                .collection("mensagens")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .await()

            snapshot.documents.mapNotNull { it.toObject(Mensagem::class.java) }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao listar mensagens: ${e.message}")
            emptyList()
        }
    }

    // ─────────────────────────────────────────────
    // BUSCAR NOME DO USUÁRIO
    // ─────────────────────────────────────────────
    private suspend fun buscarNomeUsuario(uid: String): String? {
        return try {
            val doc = db.collection("usuarios").document(uid).get().await()
            doc.getString("nome")
        } catch (e: Exception) {
            null
        }
    }
}