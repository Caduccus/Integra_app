package com.example.plataformaremota.data.entity

import com.google.firebase.firestore.DocumentId

data class Chat(
    @DocumentId
    val id: String = "",
    val participantes: List<String> = emptyList(),
    val nome: String = "",
    val criadorId: String = "",
    val ehGrupo: Boolean = false,
    val trabalhoId: String = "",
    val ultimaMensagem: String = "",
    val ultimaMensagemRemetente: String = "",
    val timestamp: Long = 0L,
    val admins: List<String> = emptyList(),     // ⭐ NOVO
    val fotoUrl: String = ""                     // ⭐ NOVO
)