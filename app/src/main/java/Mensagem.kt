package com.example.plataformaremota.data.entity

import com.google.firebase.firestore.DocumentId

data class Mensagem(
    @DocumentId
    val id: String = "",
    val remetenteId: String = "",
    val nomeRemetente: String = "",
    val texto: String = "",
    val timestamp: Long = 0L
)