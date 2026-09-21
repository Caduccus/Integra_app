package com.example.plataformaremota.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.DocumentId

@Entity(tableName = "trabalhos")
data class Trabalho(
    @PrimaryKey
    @DocumentId
    val id: String = "",
    val titulo: String = "",
    val descricao: String = "",
    val categoria: String = "",
    val prazo: String = "",
    val tipoContrato: String = "",
    val nivel: String = "",
    val criadorId: String = "",
    val nomeCriador: String = "",
    val timestamp: Long = 0L
)