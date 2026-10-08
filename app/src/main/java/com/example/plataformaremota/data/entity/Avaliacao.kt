package com.example.plataformaremota.data.entity

import com.google.firebase.firestore.DocumentId

data class Avaliacao(
    @DocumentId
    val id: String = "",
    val autorId: String = "",
    val autorNome: String = "",
    val alvoId: String = "",              // uid do usuário OU id da empresa
    val alvoTipo: String = "",            // "usuario" ou "empresa"
    val alvoNome: String = "",
    val trabalhoId: String = "",          // contexto (opcional)
    val trabalhoTitulo: String = "",
    val estrelas: Int = 5,                // 1 a 5
    val comentario: String = "",
    val timestamp: Long = 0L
)