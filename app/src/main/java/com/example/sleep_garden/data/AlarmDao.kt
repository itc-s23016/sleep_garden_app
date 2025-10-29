package com.example.sleep_garden.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmDao {

    @Query("SELECT * FROM alarms ORDER BY hour ASC, minute ASC")
    fun observeAll(): Flow<List<AlarmEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: AlarmEntity)

    @Update
    suspend fun update(entity: AlarmEntity)

    @Query("UPDATE alarms SET hour = :hour, minute = :minute, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateTime(id: String, hour: Int, minute: Int, updatedAt: Long)

    @Query("UPDATE alarms SET enabled = :enabled, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setEnabled(id: String, enabled: Boolean, updatedAt: Long)

    @Query("DELETE FROM alarms WHERE id = :id")
    suspend fun deleteById(id: String)
}