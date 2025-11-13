package com.example.sleep_garden.data.flower

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FlowerDao {

    // すべて取得
    @Query("SELECT * FROM flowers ORDER BY id ASC")
    fun getAllFlow(): Flow<List<Flower>>

    @Query("SELECT * FROM flowers ORDER BY id ASC")
    suspend fun getAll(): List<Flower>

    // ★追加（複数）
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(list: List<Flower>)

    // ★追加（1件）
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(flower: Flower)

    // ★更新（既存データを更新できる）
    @Update
    suspend fun update(flower: Flower)

    // ★名前で検索（追加 or 更新の判定に使う）
    @Query("SELECT * FROM flowers WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): Flower?


}
