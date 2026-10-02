package com.example.plataformaremota.data.entity

import com.google.firebase.firestore.DocumentId

data class Candidatura(
    @DocumentId
    val id: String = "",              // = uid do usuário
    val usuarioId: String = "",
    val nomeUsuario: String = "",
    val status: String = "pendente",  // "pendente" | "aceito" | "rejeitado"
    val timestamp: Long = 0L
)