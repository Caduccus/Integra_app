package com.example.plataformaremota.data.repository

import com.example.plataformaremota.NotificacaoHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AmizadeRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    suspend fun verificarStatus(uidAlvo: String): String {
        val meuUid = auth.currentUser?.uid ?: return "nenhum"
        return try {
            val docMeu = db.collection("usuarios").document(meuUid).get().await()
            val meusAmigos = (docMeu.get("amigos") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            val minhasSolicitacoes = (docMeu.get("solicitacoes") as? List<*>)?.filterIsInstance<String>() ?: emptyList()

            if (uidAlvo in meusAmigos) {
                "amigo"
            } else if (uidAlvo in minhasSolicitacoes) {
                "pendente_recebida"
            } else {
                val docAlvo = db.collection("usuarios").document(uidAlvo).get().await()
                val solicitacoesDele = (docAlvo.get("solicitacoes") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                if (meuUid in solicitacoesDele) "pendente_enviada" else "nenhum"
            }
        } catch (e: Exception) {
            "nenhum"
        }
    }

    suspend fun enviarSolicitacao(uidAlvo: String, meuNome: String): Boolean {
        val meuUid = auth.currentUser?.uid ?: return false
        return try {
            db.collection("usuarios").document(uidAlvo)
                .update("solicitacoes", FieldValue.arrayUnion(meuUid)).await()

            // ⭐ Notifica via Push e In-App
            NotificacaoHelper.notificar(
                uidDestino = uidAlvo,
                titulo = "Nova solicitação de amizade 🤝",
                mensagem = "$meuNome quer adicionar você como amigo.",
                tipo = "amizade",
                refId = meuUid
            )
            true
        } catch (e: Exception) { false }
    }

    suspend fun aceitarSolicitacao(uidRemetente: String): Boolean {
        val meuUid = auth.currentUser?.uid ?: return false
        return try {
            db.collection("usuarios").document(meuUid).update(
                mapOf(
                    "amigos" to FieldValue.arrayUnion(uidRemetente),
                    "solicitacoes" to FieldValue.arrayRemove(uidRemetente)
                )
            ).await()

            db.collection("usuarios").document(uidRemetente)
                .update("amigos", FieldValue.arrayUnion(meuUid)).await()

            val meuNome = db.collection("usuarios").document(meuUid).get().await().getString("nome") ?: "Alguém"
            NotificacaoHelper.notificar(
                uidDestino = uidRemetente,
                titulo = "Amizade aceita! 🎉",
                mensagem = "$meuNome aceitou sua solicitação de amizade.",
                tipo = "amizade",
                refId = meuUid
            )

            true
        } catch (e: Exception) { false }
    }

    suspend fun recusarSolicitacao(uidRemetente: String): Boolean {
        val meuUid = auth.currentUser?.uid ?: return false
        return try {
            db.collection("usuarios").document(meuUid)
                .update("solicitacoes", FieldValue.arrayRemove(uidRemetente)).await()
            true
        } catch (e: Exception) { false }
    }

    suspend fun removerAmigo(uidAlvo: String): Boolean {
        val meuUid = auth.currentUser?.uid ?: return false
        return try {
            db.collection("usuarios").document(meuUid)
                .update("amigos", FieldValue.arrayRemove(uidAlvo)).await()
            db.collection("usuarios").document(uidAlvo)
                .update("amigos", FieldValue.arrayRemove(meuUid)).await()
            true
        } catch (e: Exception) { false }
    }
}