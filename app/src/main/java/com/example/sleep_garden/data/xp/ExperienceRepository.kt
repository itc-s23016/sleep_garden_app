package com.example.sleep_garden.data.xp

import androidx.room.withTransaction
import com.example.sleep_garden.data.AppDatabase
import kotlinx.coroutines.flow.Flow
import kotlin.math.max

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
     * 就寝～起床の確定時に呼ぶ。
     * durationMin は (wake - sleep) から自動算出。
     */
    suspend fun recordSleep(sleepAtMillis: Long, wakeAtMillis: Long, note: String? = null): XpResult {
        val durMin = max(0, ((wakeAtMillis - sleepAtMillis) / 60000L).toInt())
        val gained = XpFormula.gained(durMin)
        val session = XpSessionEntity(
            sleepAtMillis = sleepAtMillis,
            wakeAtMillis = wakeAtMillis,
            durationMin = durMin,
            gainedXp = gained,
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
