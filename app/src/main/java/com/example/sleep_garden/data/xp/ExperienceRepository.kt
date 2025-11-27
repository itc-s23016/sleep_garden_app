package com.example.sleep_garden.data.xp

import androidx.room.withTransaction
import com.example.sleep_garden.data.AppDatabase
import kotlinx.coroutines.flow.Flow

data class XpResult(
    val session: XpSessionEntity,
    val summary: XpSummaryEntity,
    val levelInfo: LevelInfo
)

class ExperienceRepository(
    private val db: AppDatabase
) {
    private val dao = db.experienceDao()

    fun observeSessions(): Flow<List<XpSessionEntity>> = dao.observeSessions()
    fun observeSummary(): Flow<XpSummaryEntity?> = dao.observeSummary()

    /**
     * ★ XP は UI から渡す（スヌーズ時は半減済み）
     */
    suspend fun recordSleep(
        sleepAtMillis: Long,
        wakeAtMillis: Long,
        note: String?,
        effectiveDurationMin: Int      // ←追加
    ): XpResult {

        val gainedXp = XpFormula.gained(effectiveDurationMin)

        val session = XpSessionEntity(
            sleepAtMillis = sleepAtMillis,
            wakeAtMillis = wakeAtMillis,
            durationMin = effectiveDurationMin,
            gainedXp = gainedXp,
            note = note
        )

        lateinit var inserted: XpSessionEntity
        lateinit var updatedSummary: XpSummaryEntity
        lateinit var levelInfo: LevelInfo

        db.withTransaction {
            val id = dao.insertSession(session)
            inserted = session.copy(id = id)

            val total = dao.sumTotalXp()
            val li = Leveling.compute(total)
            levelInfo = li

            updatedSummary = XpSummaryEntity(
                id = 1,
                totalXp = total,
                level = li.level,
                lastUpdatedMillis = System.currentTimeMillis()
            )
            dao.upsertSummary(updatedSummary)
        }

        return XpResult(inserted, updatedSummary, levelInfo)
    }
}
