package com.example.plataformaremota.data.entity

import com.google.firebase.firestore.DocumentId

data class Mensagem(
    @DocumentId
    val id: String = "",
    val remetenteId: String = "",
    val nomeRemetente: String = "",
    val texto: String = "",
    val tipo: String = "texto",       // "texto", "imagem", "audio"
    val urlMidia: String = "",
    val duracaoMs: Long = 0L,
    val timestamp: Long = 0L,
    // ⭐ NOVOS
    val replyToId: String = "",
    val replyToNome: String = "",
    val replyToTexto: String = "",
    val editada: Boolean = false,
    val deletada: Boolean = false
)