package com.example.plataformaremota.data.repository

import android.util.Log
import com.example.plataformaremota.NotificacaoHelper
import com.example.plataformaremota.data.entity.Chat
import com.example.plataformaremota.data.entity.Empresa
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class EmpresaRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val TAG = "EMPRESA_REPO"

    // ─────────────────────────────────────────────
    // CRIAR EMPRESA (já cria o grupo Geral com o dono)
    // ─────────────────────────────────────────────
    suspend fun criarEmpresa(empresa: Empresa): String? {
        val uid = auth.currentUser?.uid ?: return null
        if (empresa.nome.isBlank()) return null

        return try {
            val docRef = db.collection("empresas").document()
            val nova = empresa.copy(
                id = docRef.id,
                criadorId = uid,
                admins = listOf(uid),
                membros = listOf(uid),
                pendentes = emptyList(),
                timestamp = System.currentTimeMillis()
            )
            docRef.set(nova).await()

            val grupoId = "empresa_${docRef.id}_geral"
            val grupo = Chat(
                id = grupoId,
                participantes = listOf(uid),
                nome = "${empresa.nome} — Geral",
                criadorId = uid,
                ehGrupo = true,
                admins = listOf(uid),
                fotoUrl = empresa.logoUrl,
                empresaId = docRef.id,
                timestamp = System.currentTimeMillis()
            )
            db.collection("chats").document(grupoId).set(grupo).await()

            Log.d(TAG, "✅ Empresa + grupo criados: ${docRef.id}")
            docRef.id
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro criar empresa: ${e.message}")
            null
        }
    }

    // ─────────────────────────────────────────────
    // BUSCAR / LISTAR
    // ─────────────────────────────────────────────
    suspend fun buscarPorId(id: String): Empresa? {
        return try {
            val doc = db.collection("empresas").document(id).get().await()
            doc.toObject(Empresa::class.java)
        } catch (e: Exception) { null }
    }

    suspend fun listarMinhas(): List<Empresa> {
        val uid = auth.currentUser?.uid ?: return emptyList()
        return try {
            val snap = db.collection("empresas")
                .whereArrayContains("membros", uid)
                .get().await()
            snap.documents
                .mapNotNull { it.toObject(Empresa::class.java) }
                .sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            Log.e(TAG, "❌ listarMinhas: ${e.message}")
            emptyList()
        }
    }

    suspend fun listarPendentesMinhas(): List<Empresa> {
        val uid = auth.currentUser?.uid ?: return emptyList()
        return try {
            val snap = db.collection("empresas")
                .whereArrayContains("pendentes", uid)
                .get().await()
            snap.documents
                .mapNotNull { it.toObject(Empresa::class.java) }
                .sortedByDescending { it.timestamp }
        } catch (e: Exception) { emptyList() }
    }

    suspend fun descobrir(): List<Empresa> {
        val uid = auth.currentUser?.uid ?: return emptyList()
        return try {
            val snap = db.collection("empresas").get().await()
            snap.documents
                .mapNotNull { it.toObject(Empresa::class.java) }
                .filter { uid !in it.membros && uid !in it.pendentes }
                .sortedByDescending { it.timestamp }
        } catch (e: Exception) { emptyList() }
    }

    // ─────────────────────────────────────────────
    // PEDIR PRA ENTRAR / CANCELAR
    // ─────────────────────────────────────────────
    suspend fun pedirParaEntrar(empresaId: String): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        return try {
            db.collection("empresas").document(empresaId)
                .update("pendentes", FieldValue.arrayUnion(uid))
                .await()

            // ⭐ Notifica os admins
            val empresa = buscarPorId(empresaId)
            val nomeSolicitante = buscarNomeUsuario(uid) ?: "Alguém"
            if (empresa != null) {
                val admins = empresa.admins.filter { it != uid }
                if (admins.isNotEmpty()) {
                    NotificacaoHelper.enviarParaVarios(
                        uidsDestino = admins,
                        titulo = "Novo pedido para entrar 🏢",
                        mensagem = "$nomeSolicitante quer entrar em ${empresa.nome}"
                    )
                    for (admin in admins) {
                        NotificacaoHelper.salvarInApp(
                            uidDestino = admin,
                            titulo = "Novo pedido para entrar 🏢",
                            mensagem = "$nomeSolicitante quer entrar em ${empresa.nome}",
                            tipo = "empresa",
                            refId = empresaId
                        )
                    }
                }
            }
            true
        } catch (e: Exception) { false }
    }

    suspend fun cancelarPedido(empresaId: String): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        return try {
            db.collection("empresas").document(empresaId)
                .update("pendentes", FieldValue.arrayRemove(uid))
                .await()
            true
        } catch (e: Exception) { false }
    }

    // ─────────────────────────────────────────────
    // APROVAR / REJEITAR
    // ─────────────────────────────────────────────
    suspend fun aprovarMembro(empresaId: String, uidNovo: String): Boolean {
        return try {
            val empresa = buscarPorId(empresaId) ?: return false
            if (!podeAprovar(empresa)) return false

            val nomeNovo = buscarNomeUsuario(uidNovo) ?: "Um membro"

            db.collection("empresas").document(empresaId).update(
                mapOf(
                    "pendentes" to FieldValue.arrayRemove(uidNovo),
                    "membros" to FieldValue.arrayUnion(uidNovo)
                )
            ).await()

            entrarNoGrupoGeral(empresaId, uidNovo)

            val grupoId = "empresa_${empresaId}_geral"
            enviarMensagemSistema(grupoId, "$nomeNovo entrou no grupo")
            NotificacaoHelper.salvarInApp(
                uidDestino = uidNovo,
                titulo = "Bem-vindo à empresa! 🎉",
                mensagem = "Você foi aprovado em ${empresa.nome}",
                tipo = "empresa",
                refId = empresaId
            )
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ aprovarMembro: ${e.message}")
            false
        }
    }

    suspend fun rejeitarMembro(empresaId: String, uidRejeitar: String): Boolean {
        return try {
            db.collection("empresas").document(empresaId)
                .update("pendentes", FieldValue.arrayRemove(uidRejeitar))
                .await()
            true
        } catch (e: Exception) { false }
    }

    // ─────────────────────────────────────────────
    // REMOVER MEMBRO (só admin, nunca o dono)
    // ─────────────────────────────────────────────
    suspend fun removerMembro(empresaId: String, uidRemover: String): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        return try {
            val empresa = buscarPorId(empresaId) ?: return false

            if (uidRemover == empresa.criadorId) return false
            if (uid !in empresa.admins) return false

            val nomeRemovido = buscarNomeUsuario(uidRemover) ?: "Um membro"
            val nomeAdmin = buscarNomeUsuario(uid) ?: "Admin"

            val grupoId = "empresa_${empresaId}_geral"
            enviarMensagemSistema(
                grupoId,
                "$nomeRemovido foi removido do grupo por $nomeAdmin"
            )

            db.collection("chats").document(grupoId)
                .update("participantes", FieldValue.arrayRemove(uidRemover)).await()

            db.collection("empresas").document(empresaId).update(
                mapOf(
                    "membros" to FieldValue.arrayRemove(uidRemover),
                    "admins" to FieldValue.arrayRemove(uidRemover)
                )
            ).await()

            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ removerMembro: ${e.message}")
            false
        }
    }

    // ─────────────────────────────────────────────
    // PROMOVER / REBAIXAR
    // ─────────────────────────────────────────────
    suspend fun promoverAdmin(empresaId: String, uidPromover: String): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        return try {
            val empresa = buscarPorId(empresaId) ?: return false
            if (uid !in empresa.admins) return false

            val nome = buscarNomeUsuario(uidPromover) ?: "Um membro"

            db.collection("empresas").document(empresaId)
                .update("admins", FieldValue.arrayUnion(uidPromover)).await()

            val grupoId = "empresa_${empresaId}_geral"
            db.collection("chats").document(grupoId)
                .update("admins", FieldValue.arrayUnion(uidPromover)).await()
            enviarMensagemSistema(grupoId, "$nome agora é admin")

            true
        } catch (e: Exception) { false }
    }

    suspend fun rebaixarAdmin(empresaId: String, uidRebaixar: String): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        return try {
            val empresa = buscarPorId(empresaId) ?: return false
            if (uid !in empresa.admins) return false
            if (uidRebaixar == empresa.criadorId) return false

            val nome = buscarNomeUsuario(uidRebaixar) ?: "Um membro"

            db.collection("empresas").document(empresaId)
                .update("admins", FieldValue.arrayRemove(uidRebaixar)).await()

            val grupoId = "empresa_${empresaId}_geral"
            db.collection("chats").document(grupoId)
                .update("admins", FieldValue.arrayRemove(uidRebaixar)).await()
            enviarMensagemSistema(grupoId, "$nome não é mais admin")

            true
        } catch (e: Exception) { false }
    }

    // ─────────────────────────────────────────────
    // ATUALIZAR / DELETAR
    // ─────────────────────────────────────────────
    suspend fun atualizarEmpresa(empresaId: String, campos: Map<String, Any>): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        return try {
            val empresa = buscarPorId(empresaId) ?: return false
            if (uid !in empresa.admins) return false
            db.collection("empresas").document(empresaId).update(campos).await()
            true
        } catch (e: Exception) { false }
    }

    suspend fun deletarEmpresa(empresaId: String): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        return try {
            val empresa = buscarPorId(empresaId) ?: return false
            if (empresa.criadorId != uid) return false

            apagarGrupoGeral(empresaId)
            db.collection("empresas").document(empresaId).delete().await()
            true
        } catch (e: Exception) { false }
    }

    // ─────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────
    private suspend fun podeAprovar(empresa: Empresa): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        return uid in empresa.admins
    }

    private suspend fun buscarNomeUsuario(uid: String): String? {
        return try {
            val doc = db.collection("usuarios").document(uid).get().await()
            doc.getString("nome")
        } catch (e: Exception) { null }
    }

    private suspend fun enviarMensagemSistema(chatId: String, texto: String) {
        try {
            val msg = hashMapOf(
                "remetenteId" to "sistema",
                "nomeRemetente" to "Sistema",
                "texto" to texto,
                "tipo" to "sistema",
                "urlMidia" to "",
                "duracaoMs" to 0L,
                "timestamp" to System.currentTimeMillis(),
                "replyToId" to "",
                "replyToNome" to "",
                "replyToTexto" to "",
                "editada" to false,
                "deletada" to false
            )
            db.collection("chats").document(chatId)
                .collection("mensagens").add(msg).await()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro msg sistema: ${e.message}")
        }
    }

    private suspend fun entrarNoGrupoGeral(empresaId: String, uidNovo: String) {
        try {
            val grupoId = "empresa_${empresaId}_geral"
            val ref = db.collection("chats").document(grupoId)
            val doc = ref.get().await()

            if (!doc.exists()) {
                val empresa = buscarPorId(empresaId) ?: return
                val grupo = Chat(
                    id = grupoId,
                    participantes = listOf(uidNovo),
                    nome = "${empresa.nome} — Geral",
                    criadorId = empresa.criadorId,
                    ehGrupo = true,
                    admins = empresa.admins,
                    fotoUrl = empresa.logoUrl,
                    empresaId = empresaId,
                    timestamp = System.currentTimeMillis()
                )
                ref.set(grupo).await()
            } else {
                ref.update("participantes", FieldValue.arrayUnion(uidNovo)).await()
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ entrarNoGrupoGeral: ${e.message}")
        }
    }

    suspend fun atualizarLogoEmpresa(empresaId: String, urlLogo: String): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        return try {
            val empresa = buscarPorId(empresaId) ?: return false

            if (empresa.criadorId != uid) {
                Log.d(TAG, "⛔ Apenas o dono pode mudar a logo")
                return false
            }

            db.collection("empresas").document(empresaId)
                .update("logoUrl", urlLogo).await()

            val grupoId = "empresa_${empresaId}_geral"
            try {
                db.collection("chats").document(grupoId)
                    .update("fotoUrl", urlLogo).await()
            } catch (_: Exception) { }

            val nomeDono = buscarNomeUsuario(uid) ?: "Dono"
            enviarMensagemSistema(grupoId, "$nomeDono atualizou a foto da empresa")

            Log.d(TAG, "✅ Logo atualizada")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro logo: ${e.message}")
            false
        }
    }

    private suspend fun apagarGrupoGeral(empresaId: String) {
        try {
            val grupoId = "empresa_${empresaId}_geral"
            val ref = db.collection("chats").document(grupoId)
            val doc = ref.get().await()
            if (!doc.exists()) return

            val msgs = ref.collection("mensagens").get().await()
            for (m in msgs.documents) m.reference.delete().await()

            ref.delete().await()
        } catch (_: Exception) { }
    }
}