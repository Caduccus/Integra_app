package com.example.plataformaremota.data.entity

import com.google.firebase.firestore.DocumentId

data class Empresa(
    @DocumentId
    val id: String = "",
    val nome: String = "",
    val descricao: String = "",
    val cnpj: String = "",
    val logoUrl: String = "",
    val areaAtuacao: String = "",
    val endereco: String = "",
    val emailCorporativo: String = "",
    val criadorId: String = "",
    val admins: List<String> = emptyList(),
    val membros: List<String> = emptyList(),
    val pendentes: List<String> = emptyList(),
    val timestamp: Long = 0L
)