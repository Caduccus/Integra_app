package com.example.plataformaremota.data.repository

import com.example.plataformaremota.NotificacaoHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UsuarioRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // ─────────────────────────────────────────────
    // ENVIAR PEDIDO DE AMIZADE
    // ─────────────────────────────────────────────
    suspend fun enviarPedidoAmizade(uidDestino: String): Boolean {
        val meuUid = auth.currentUser?.uid ?: return false
        return try {
            // Adiciona o meu UID na lista de solicitações do destino
            db.collection("usuarios").document(uidDestino)
                .update("solicitacoesAmizade", FieldValue.arrayUnion(meuUid))
                .await()

            // Busca o meu nome para a notificação
            val meuDoc = db.collection("usuarios").document(meuUid).get().await()
            val meuNome = meuDoc.getString("nome") ?: "Usuário"

            // Envia Notificação Push
            NotificacaoHelper.enviar(
                uidDestino = uidDestino,
                titulo = "Nova solicitação de amizade 🤝",
                mensagem = "$meuNome quer ser seu amigo."
            )
            // Salva Notificação In-App
            NotificacaoHelper.salvarInApp(
                uidDestino = uidDestino,
                titulo = "Nova solicitação de amizade 🤝",
                mensagem = "$meuNome quer ser seu amigo.",
                tipo = "amizade",
                refId = meuUid
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    // ─────────────────────────────────────────────
    // ACEITAR PEDIDO DE AMIZADE
    // ─────────────────────────────────────────────
    suspend fun aceitarPedidoAmizade(uidSolicitante: String): Boolean {
        val meuUid = auth.currentUser?.uid ?: return false
        return try {
            // Adiciona o solicitante na minha lista de amigos e remove das solicitações
            db.collection("usuarios").document(meuUid)
                .update(
                    "amigos", FieldValue.arrayUnion(uidSolicitante),
                    "solicitacoesAmizade", FieldValue.arrayRemove(uidSolicitante)
                ).await()

            // Adiciona eu na lista de amigos dele
            db.collection("usuarios").document(uidSolicitante)
                .update("amigos", FieldValue.arrayUnion(meuUid))
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }

    // ─────────────────────────────────────────────
    // RECUSAR PEDIDO DE AMIZADE
    // ─────────────────────────────────────────────
    suspend fun recusarPedidoAmizade(uidSolicitante: String): Boolean {
        val meuUid = auth.currentUser?.uid ?: return false
        return try {
            // Apenas remove o solicitante da minha lista de pendentes
            db.collection("usuarios").document(meuUid)
                .update("solicitacoesAmizade", FieldValue.arrayRemove(uidSolicitante))
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }

    // ─────────────────────────────────────────────
    // DESFAZER AMIZADE
    // ─────────────────────────────────────────────
    suspend fun desfazerAmizade(uidAmigo: String): Boolean {
        val meuUid = auth.currentUser?.uid ?: return false
        return try {
            // Remove ele da minha lista
            db.collection("usuarios").document(meuUid)
                .update("amigos", FieldValue.arrayRemove(uidAmigo)).await()

            // Remove eu da lista dele
            db.collection("usuarios").document(uidAmigo)
                .update("amigos", FieldValue.arrayRemove(meuUid)).await()
            true
        } catch (e: Exception) {
            false
        }
    }
}