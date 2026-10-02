package com.example.plataformaremota.data.repository

import android.util.Log
import com.example.plataformaremota.data.entity.Chat
import com.example.plataformaremota.data.entity.Mensagem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import com.example.plataformaremota.NotificacaoHelper

class ChatRepository {

    private var estaBloqueado: Boolean = false
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
                    timestamp = System.currentTimeMillis(),
                    deletadosPara = emptyList()
                )
                docRef.set(chat).await()
            } else {
                // ⭐ Se eu tinha deletado essa conversa, restauro pra mim
                docRef.update("deletadosPara", FieldValue.arrayRemove(meuUid)).await()
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

    // ⭐ Excluir conversa (soft delete — só pra mim)
    suspend fun excluirConversa(chatId: String): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        return try {
            db.collection("chats").document(chatId)
                .update("deletadosPara", FieldValue.arrayUnion(uid))
                .await()
            Log.d(TAG, "✅ Conversa escondida pra $uid")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro excluir: ${e.message}")
            false
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

            // ⭐ Ao enviar, restaura a conversa pra quem havia deletado
            restaurarParaTodos(chatId)

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

            restaurarParaTodos(chatId)

            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao enviar áudio: ${e.message}")
            false
        }
    }

    // ⭐ Enviar mensagem de IMAGEM
    suspend fun enviarImagem(chatId: String, urlImagem: String, legenda: String = ""): Boolean {
        val uid = auth.currentUser?.uid ?: return false

        return try {
            val nome = buscarNomeUsuario(uid) ?: "Usuário"

            val msg = Mensagem(
                remetenteId = uid,
                nomeRemetente = nome,
                texto = legenda.trim(),
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

            restaurarParaTodos(chatId)

            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao enviar imagem: ${e.message}")
            false
        }
    }

    // ⭐ Restaura a conversa pra todos (limpa a lista de deletados)
    private suspend fun restaurarParaTodos(chatId: String) {
        try {
            db.collection("chats").document(chatId)
                .update("deletadosPara", emptyList<String>())
                .await()
        } catch (_: Exception) { }
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

    suspend fun criarGrupo(
        nomeGrupo: String,
        participantes: List<String>
    ): String? {
        val meuUid = auth.currentUser?.uid ?: return null
        if (nomeGrupo.isBlank() || participantes.size < 1) return null

        return try {
            val docRef = db.collection("chats").document()
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

            restaurarParaTodos(chatId)

            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro: ${e.message}")
            false
        }
    }

    suspend fun atualizarNomeGrupo(chatId: String, novoNome: String): Boolean {
        return try {
            db.collection("chats").document(chatId)
                .update("nome", novoNome.trim()).await()
            true
        } catch (e: Exception) { false }
    }

    suspend fun atualizarFotoGrupo(chatId: String, urlFoto: String): Boolean {
        return try {
            db.collection("chats").document(chatId)
                .update("fotoUrl", urlFoto).await()
            true
        } catch (e: Exception) { false }
    }

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

            // ⭐ Se for grupo de empresa, remove TAMBÉM da empresa
            if (chat.empresaId.isNotEmpty()) {
                try {
                    db.collection("empresas").document(chat.empresaId).update(
                        mapOf(
                            "membros" to FieldValue.arrayRemove(uidRemover),
                            "admins" to FieldValue.arrayRemove(uidRemover)
                        )
                    ).await()
                    Log.d(TAG, "✅ Removido também da empresa ${chat.empresaId}")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Erro ao remover da empresa: ${e.message}")
                }
            }

            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao remover membro: ${e.message}")
            false
        }
    }

    suspend fun sairDoGrupo(chatId: String): Boolean {
        val uid = auth.currentUser?.uid ?: return false

        try {
            val chat = buscarChat(chatId)
            if (chat != null && chat.empresaId.isNotEmpty()) {
                Log.d(TAG, "⛔ Saída bloqueada: grupo de empresa")
                return false
            }
        } catch (_: Exception) { }

        return removerMembro(chatId, uid)
    }

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

    suspend fun estouBloqueado(chatId: String): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        return try {
            val chat = buscarChat(chatId) ?: return false
            if (chat.ehGrupo) return false

            val outroUid = chat.participantes.firstOrNull { it != uid } ?: return false

            val meuDoc = db.collection("usuarios").document(uid).get().await()
            val meusBloqueados = (meuDoc.get("bloqueados") as? List<*>)?.filterIsInstance<String>() ?: emptyList()

            val outroDoc = db.collection("usuarios").document(outroUid).get().await()
            val bloqueadosDele = (outroDoc.get("bloqueados") as? List<*>)?.filterIsInstance<String>() ?: emptyList()

            meusBloqueados.contains(outroUid) || bloqueadosDele.contains(uid)
        } catch (e: Exception) {
            false
        }
    }

    suspend fun euBloqueei(chatId: String): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        return try {
            val chat = buscarChat(chatId) ?: return false
            if (chat.ehGrupo) return false

            val outroUid = chat.participantes.firstOrNull { it != uid } ?: return false

            val meuDoc = db.collection("usuarios").document(uid).get().await()
            val meusBloqueados = (meuDoc.get("bloqueados") as? List<*>)?.filterIsInstance<String>() ?: emptyList()

            meusBloqueados.contains(outroUid)
        } catch (e: Exception) {
            false
        }
    }

    suspend fun adicionarMembros(chatId: String, novosUids: List<String>): Boolean {
        if (novosUids.isEmpty()) return false

        return try {
            val chatDoc = db.collection("chats").document(chatId).get().await()
            val chat = chatDoc.toObject(Chat::class.java)

            if (chat != null && chat.empresaId.isNotEmpty()) {
                Log.d(TAG, "⛔ Adição bloqueada: grupo de empresa")
                return false
            }

            if (chat == null) return false

            val participantesFinal = (chat.participantes + novosUids).distinct()

            db.collection("chats").document(chatId)
                .update("participantes", participantesFinal).await()

            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao adicionar membros: ${e.message}")
            false
        }
    }

    suspend fun apagarGrupo(chatId: String): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        return try {
            val doc = db.collection("chats").document(chatId).get().await()
            val chat = doc.toObject(Chat::class.java) ?: return false

            if (chat.criadorId != uid) return false

            val mensagens = doc.reference.collection("mensagens").get().await()
            for (m in mensagens.documents) {
                m.reference.delete().await()
            }

            val status = doc.reference.collection("status").get().await()
            for (s in status.documents) {
                s.reference.delete().await()
            }

            doc.reference.delete().await()

            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao apagar grupo: ${e.message}")
            false
        }
    }

    suspend fun atualizar(id: String, campos: Map<String, Any>): Boolean {
        return try {
            db.collection("trabalhos").document(id).update(campos).await()
            Log.d(TAG, "✅ Trabalho atualizado: $id")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao atualizar: ${e.message}")
            false
        }
    }

    suspend fun notificarParticipantes(
        chatId: String,
        textoPreview: String
    ) {
        val uidAtual = auth.currentUser?.uid ?: return
        try {
            val chat = buscarChat(chatId) ?: return
            val destinatarios = chat.participantes.filter { it != uidAtual }
            if (destinatarios.isEmpty()) return

            val meuNome = buscarNomeUsuario(uidAtual) ?: "Usuário"
            val titulo = if (chat.ehGrupo) "Nova mensagem no grupo" else "Nova mensagem"
            val mensagem = "$meuNome: $textoPreview"

            for (dest in destinatarios) {
                val statusRef = db.collection("chats").document(chatId)
                    .collection("status").document(dest)
                val statusDoc = statusRef.get().await()
                val unreadCount = statusDoc.getLong("unreadCount") ?: 0L

                statusRef.set(
                    mapOf(
                        "unreadCount" to (unreadCount + 1),
                        "ultimaVez" to System.currentTimeMillis()
                    ),
                    com.google.firebase.firestore.SetOptions.merge()
                ).await()

                if (unreadCount < 3L) {
                    NotificacaoHelper.enviar(dest, titulo, mensagem, chatId)
                }
            }
        } catch (_: Exception) { }
    }

    private suspend fun buscarNomeUsuario(uid: String): String? {
        return try {
            val doc = db.collection("usuarios").document(uid).get().await()
            doc.getString("nome")
        } catch (e: Exception) { null }
    }
}