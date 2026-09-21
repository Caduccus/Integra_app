package com.example.plataformaremota.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.plataformaremota.data.entity.Trabalho

@Dao
interface TrabalhoDao {

    @Query("SELECT * FROM trabalhos ORDER BY timestamp DESC")
    suspend fun listarTodos(): List<Trabalho>

    @Query("SELECT * FROM trabalhos WHERE id = :id LIMIT 1")
    suspend fun buscarPorId(id: String): Trabalho?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(trabalho: Trabalho)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirTodos(trabalhos: List<Trabalho>)

    @Query("DELETE FROM trabalhos")
    suspend fun deletarTodos()

    @Query("DELETE FROM trabalhos WHERE id = :id")
    suspend fun deletarPorId(id: String)
}