package com.example.sleep_garden.data.xp

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExperienceDao {

    // ---- Session ----
    @Insert
    suspend fun insertSession(session: XpSessionEntity): Long

    @Query("SELECT * FROM xp_session ORDER BY id DESC")
    fun observeSessions(): Flow<List<XpSessionEntity>>

    @Query("SELECT COALESCE(SUM(gainedXp), 0) FROM xp_session")
    suspend fun sumTotalXp(): Int

    // ---- Summary ----
    @Query("SELECT * FROM xp_summary WHERE id = 1")
    fun observeSummary(): Flow<XpSummaryEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSummary(summary: XpSummaryEntity)
}
