package com.example.plataformaremota

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object AdminChecker {

    var isAdmin: Boolean = false
        private set
    var isCarregado: Boolean = false
        private set

    fun carregar(onResult: (Boolean) -> Unit = {}) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            isAdmin = false
            isCarregado = true
            onResult(false)
            return
        }

        FirebaseFirestore.getInstance()
            .collection("usuarios").document(uid).get()
            .addOnSuccessListener { doc ->
                isAdmin = doc.getBoolean("isAdmin") ?: false
                isCarregado = true
                onResult(isAdmin)
            }
            .addOnFailureListener {
                isAdmin = false
                isCarregado = true
                onResult(false)
            }
    }

    fun limpar() {
        isAdmin = false
        isCarregado = false
    }
}