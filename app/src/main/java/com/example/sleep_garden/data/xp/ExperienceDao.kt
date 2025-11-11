package com.example.sleep_garden.data.xp

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 1回の睡眠セッション（ログ） */
@Entity(tableName = "xp_session")
data class XpSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val sleepAtMillis: Long,   // 就寝時刻 (epoch millis)
    val wakeAtMillis: Long,    // 起床時刻 (epoch millis)
    val durationMin: Int,      // 睡眠時間(分)
    val gainedXp: Int,         // このセッションで獲得したXP
    val note: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis()
)

/** 累計の要約（1行だけ保持する） */
@Entity(tableName = "xp_summary")
data class XpSummaryEntity(
    @PrimaryKey val id: Int = 1,                  // 常に1固定
    val totalXp: Int = 0,                         // 累計XP
    val level: Int = 1,                           // 現在レベル（1..MAX）
    val lastUpdatedMillis: Long = System.currentTimeMillis()
)
