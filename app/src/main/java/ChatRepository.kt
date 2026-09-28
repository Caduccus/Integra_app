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

    private fun gerarChatIdUmParaUm(uid1: String, uid2: String, trabalhoId: String): String {
        val ordenados = if (uid1 < uid2) "${uid1}_${uid2}" else "${uid2}_${uid1}"
        return if (trabalhoId.isEmpty()) ordenados else "${ordenados}_${trabalhoId}"
    }

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
            }
            chatId
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao criar chat: ${e.message}")
            null
        }
    }

    suspend fun buscarChat(chatId: String): Chat? {
        return try {
            val doc = db.collection("chats").document(chatId).get().await()
            doc.toObject(Chat::class.java)
        } catch (e: Exception) {
            null
        }
    }

    // ⭐ Enviar mensagem de TEXTO
    suspend fun enviarMensagem(chatId: String, texto: String): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        if (texto.isBlank()) return false

        return try {
            val nome = buscarNomeUsuario(uid) ?: "Usuário"

            val msg = Mensagem(
                remetenteId = uid,
                nomeRemetente = nome,
                texto = texto.trim(),
                tipo = "texto",
                urlMidia = "",
                timestamp = System.currentTimeMillis()
            )

            db.collection("chats").document(chatId)
                .collection("mensagens").add(msg).await()

            db.collection("chats").document(chatId).update(
                mapOf(
                    "ultimaMensagem" to texto.trim(),
                    "ultimaMensagemRemetente" to nome,
                    "timestamp" to System.currentTimeMillis()
                )
            ).await()

            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao enviar: ${e.message}")
            false
        }
    }

    // ⭐ Enviar mensagem de ÁUDIO
    suspend fun enviarAudio(chatId: String, urlAudio: String, duracaoMs: Long): Boolean {
        val uid = auth.currentUser?.uid ?: return false

        return try {
            val nome = buscarNomeUsuario(uid) ?: "Usuário"

            val msg = Mensagem(
                remetenteId = uid,
                nomeRemetente = nome,
                texto = "",
                tipo = "audio",
                urlMidia = urlAudio,
                duracaoMs = duracaoMs,
                timestamp = System.currentTimeMillis()
            )

            db.collection("chats").document(chatId)
                .collection("mensagens").add(msg).await()

            db.collection("chats").document(chatId).update(
                mapOf(
                    "ultimaMensagem" to "🎤 Áudio",
                    "ultimaMensagemRemetente" to nome,
                    "timestamp" to System.currentTimeMillis()
                )
            ).await()

            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao enviar áudio: ${e.message}")
            false
        }
    }

    // ⭐ Enviar mensagem de IMAGEM (com legenda opcional)
    suspend fun enviarImagem(chatId: String, urlImagem: String, legenda: String = ""): Boolean {
        val uid = auth.currentUser?.uid ?: return false

        return try {
            val nome = buscarNomeUsuario(uid) ?: "Usuário"

            val msg = Mensagem(
                remetenteId = uid,
                nomeRemetente = nome,
                texto = legenda.trim(),        // ⭐ Legenda
                tipo = "imagem",
                urlMidia = urlImagem,
                timestamp = System.currentTimeMillis()
            )

            db.collection("chats").document(chatId)
                .collection("mensagens").add(msg).await()

            val preview = if (legenda.isBlank()) "📷 Imagem" else "📷 $legenda"

            db.collection("chats").document(chatId).update(
                mapOf(
                    "ultimaMensagem" to preview,
                    "ultimaMensagemRemetente" to nome,
                    "timestamp" to System.currentTimeMillis()
                )
            ).await()

            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao enviar imagem: ${e.message}")
            false
        }
    }

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
            emptyList()
        }
    }

    // ⭐ Editar mensagem
    suspend fun editarMensagem(chatId: String, mensagemId: String, novoTexto: String): Boolean {
        return try {
            db.collection("chats").document(chatId)
                .collection("mensagens").document(mensagemId)
                .update(
                    mapOf(
                        "texto" to novoTexto.trim(),
                        "editada" to true
                    )
                ).await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao editar: ${e.message}")
            false
        }
    }

    // ⭐ Deletar mensagem
    suspend fun deletarMensagem(chatId: String, mensagemId: String): Boolean {
        return try {
            db.collection("chats").document(chatId)
                .collection("mensagens").document(mensagemId)
                .delete().await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao deletar: ${e.message}")
            false
        }
    }

    // ⭐ Criar novo GRUPO
    suspend fun criarGrupo(
        nomeGrupo: String,
        participantes: List<String>
    ): String? {
        val meuUid = auth.currentUser?.uid ?: return null
        if (nomeGrupo.isBlank() || participantes.size < 1) return null

        return try {
            val docRef = db.collection("chats").document()  // ID automático
            val participantesFinal = (participantes + meuUid).distinct()

            val chat = Chat(
                id = docRef.id,
                participantes = participantesFinal,
                nome = nomeGrupo.trim(),
                criadorId = meuUid,
                ehGrupo = true,
                trabalhoId = "",
                ultimaMensagem = "",
                ultimaMensagemRemetente = "",
                timestamp = System.currentTimeMillis()
            )

            docRef.set(chat).await()
            Log.d(TAG, "✅ Grupo criado: ${docRef.id}")
            docRef.id
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao criar grupo: ${e.message}")
            null
        }
    }

    // ⭐ Enviar mensagem de TEXTO com reply
    suspend fun enviarMensagemComReply(
        chatId: String,
        texto: String,
        replyTo: Mensagem? = null
    ): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        if (texto.isBlank()) return false

        return try {
            val nome = buscarNomeUsuario(uid) ?: "Usuário"

            val msg = Mensagem(
                remetenteId = uid,
                nomeRemetente = nome,
                texto = texto.trim(),
                tipo = "texto",
                urlMidia = "",
                timestamp = System.currentTimeMillis(),
                replyToId = replyTo?.id ?: "",
                replyToNome = replyTo?.nomeRemetente ?: "",
                replyToTexto = if (replyTo?.tipo == "imagem") "📷 Imagem"
                else if (replyTo?.tipo == "audio") "🎤 Áudio"
                else replyTo?.texto ?: ""
            )

            db.collection("chats").document(chatId)
                .collection("mensagens").add(msg).await()

            db.collection("chats").document(chatId).update(
                mapOf(
                    "ultimaMensagem" to texto.trim(),
                    "ultimaMensagemRemetente" to nome,
                    "timestamp" to System.currentTimeMillis()
                )
            ).await()

            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro: ${e.message}")
            false
        }
    }

    // ⭐ Atualizar nome do grupo
    suspend fun atualizarNomeGrupo(chatId: String, novoNome: String): Boolean {
        return try {
            db.collection("chats").document(chatId)
                .update("nome", novoNome.trim()).await()
            true
        } catch (e: Exception) { false }
    }

    // ⭐ Atualizar foto do grupo
    suspend fun atualizarFotoGrupo(chatId: String, urlFoto: String): Boolean {
        return try {
            db.collection("chats").document(chatId)
                .update("fotoUrl", urlFoto).await()
            true
        } catch (e: Exception) { false }
    }

    // ⭐ Remover membro
    suspend fun removerMembro(chatId: String, uidRemover: String): Boolean {
        return try {
            val doc = db.collection("chats").document(chatId).get().await()
            val chat = doc.toObject(Chat::class.java) ?: return false

            val novosParticipantes = chat.participantes.filter { it != uidRemover }
            val novosAdmins = chat.admins.filter { it != uidRemover }

            db.collection("chats").document(chatId).update(
                mapOf(
                    "participantes" to novosParticipantes,
                    "admins" to novosAdmins
                )
            ).await()
            true
        } catch (e: Exception) { false }
    }

    // ⭐ Sair do grupo
    suspend fun sairDoGrupo(chatId: String): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        return removerMembro(chatId, uid)
    }

    // ⭐ Promover a admin
    suspend fun promoverAdmin(chatId: String, uidPromover: String): Boolean {
        return try {
            val doc = db.collection("chats").document(chatId).get().await()
            val chat = doc.toObject(Chat::class.java) ?: return false

            if (chat.admins.contains(uidPromover)) return true
            val novosAdmins = chat.admins + uidPromover

            db.collection("chats").document(chatId)
                .update("admins", novosAdmins).await()
            true
        } catch (e: Exception) { false }
    }

    // ⭐ Rebaixar admin
    suspend fun rebaixarAdmin(chatId: String, uidRebaixar: String): Boolean {
        return try {
            val doc = db.collection("chats").document(chatId).get().await()
            val chat = doc.toObject(Chat::class.java) ?: return false

            val novosAdmins = chat.admins.filter { it != uidRebaixar }

            db.collection("chats").document(chatId)
                .update("admins", novosAdmins).await()
            true
        } catch (e: Exception) { false }
    }

    // ⭐ Bloquear usuário
    suspend fun bloquearUsuario(uidBloquear: String): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        return try {
            val doc = db.collection("usuarios").document(uid).get().await()
            val listaAtual = (doc.get("bloqueados") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            if (listaAtual.contains(uidBloquear)) return true
            val novaLista = listaAtual + uidBloquear

            db.collection("usuarios").document(uid)
                .update("bloqueados", novaLista).await()
            true
        } catch (e: Exception) { false }
    }

    // ⭐ Desbloquear usuário
    suspend fun desbloquearUsuario(uidDesbloquear: String): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        return try {
            val doc = db.collection("usuarios").document(uid).get().await()
            val listaAtual = (doc.get("bloqueados") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            val novaLista = listaAtual.filter { it != uidDesbloquear }

            db.collection("usuarios").document(uid)
                .update("bloqueados", novaLista).await()
            true
        } catch (e: Exception) { false }
    }

    private suspend fun buscarNomeUsuario(uid: String): String? {
        return try {
            val doc = db.collection("usuarios").document(uid).get().await()
            doc.getString("nome")
        } catch (e: Exception) { null }
    }
}