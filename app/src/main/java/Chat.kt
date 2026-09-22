package com.example.plataformaremota.data.entity

import com.google.firebase.firestore.DocumentId

data class Chat(
    @DocumentId
    val id: String = "",
    val participantes: List<String> = emptyList(),
    val nome: String = "",              // vazio se for 1:1
    val criadorId: String = "",
    val ehGrupo: Boolean = false,
    val trabalhoId: String = "",        // opcional
    val ultimaMensagem: String = "",
    val ultimaMensagemRemetente: String = "",
    val timestamp: Long = 0L
)