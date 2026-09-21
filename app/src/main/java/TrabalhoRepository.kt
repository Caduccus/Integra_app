package com.example.plataformaremota.data.repository

import android.content.Context
import android.util.Log
import com.example.plataformaremota.data.database.AppDatabase
import com.example.plataformaremota.data.entity.Trabalho
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class TrabalhoRepository(private val context: Context) {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val roomDb = AppDatabase.getDatabase(context)

    private val TAG = "TRABALHO_REPO"

    // ─────────────────────────────────────────────
    // BUSCAR TODOS OS TRABALHOS DO FIRESTORE
    // ─────────────────────────────────────────────
    suspend fun listarTodos(): List<Trabalho> {
        return try {
            val snapshot = db.collection("trabalhos")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()

            val trabalhos = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Trabalho::class.java)
            }

            Log.d(TAG, "✅ ${trabalhos.size} trabalhos carregados do Firestore")

            // Cacheia no Room pra offline
            roomDb.trabalhoDao().deletarTodos()
            roomDb.trabalhoDao().inserirTodos(trabalhos)

            trabalhos
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao buscar: ${e.message}")

            // Fallback: usa o cache do Room se tiver internet ruim
            roomDb.trabalhoDao().listarTodos()
        }
    }

    // ─────────────────────────────────────────────
    // BUSCAR UM TRABALHO POR ID
    // ─────────────────────────────────────────────
    suspend fun buscarPorId(id: String): Trabalho? {
        return try {
            val doc = db.collection("trabalhos").document(id).get().await()
            doc.toObject(Trabalho::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao buscar por ID: ${e.message}")
            roomDb.trabalhoDao().buscarPorId(id)
        }
    }

    // ─────────────────────────────────────────────
    // PUBLICAR NOVO TRABALHO
    // ─────────────────────────────────────────────
    suspend fun publicar(trabalho: Trabalho): Boolean {
        return try {
            val docRef = db.collection("trabalhos").document() // ID automático
            val trabalhoComId = trabalho.copy(id = docRef.id)

            docRef.set(trabalhoComId).await()

            Log.d(TAG, "✅ Trabalho publicado: ${docRef.id}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao publicar: ${e.message}")
            false
        }
    }

    // ─────────────────────────────────────────────
    // EXCLUIR TRABALHO
    // ─────────────────────────────────────────────
    suspend fun excluir(id: String): Boolean {
        return try {
            db.collection("trabalhos").document(id).delete().await()
            Log.d(TAG, "✅ Trabalho excluído: $id")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao excluir: ${e.message}")
            false
        }
    }

    // ─────────────────────────────────────────────
    // VERIFICAR SE O USUÁRIO LOGADO É O CRIADOR
    // ─────────────────────────────────────────────
    fun ehCriador(criadorId: String): Boolean {
        return auth.currentUser?.uid == criadorId
    }
}