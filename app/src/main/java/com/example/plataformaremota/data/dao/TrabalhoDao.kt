package com.example.plataformaremota.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.plataformaremota.data.entity.Trabalho

@Dao
interface TrabalhoDao {

    @Insert
    suspend fun inserir(trabalho: Trabalho)

    @Query("SELECT * FROM trabalhos ORDER BY id DESC")
    suspend fun listarTodos(): List<Trabalho>

    @Query("SELECT * FROM trabalhos WHERE id = :id LIMIT 1")
    suspend fun buscarPorId(id: Int): Trabalho?
}