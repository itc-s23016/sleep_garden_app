package com.example.sleep_garden.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// ★ 追加インポート（XP用）
import com.example.sleep_garden.data.xp.ExperienceDao
import com.example.sleep_garden.data.xp.XpSessionEntity
import com.example.sleep_garden.data.xp.XpSummaryEntity

@Database(
    entities = [
        AlarmEntity::class,              // 既存
        XpSessionEntity::class,          // ★追加
        XpSummaryEntity::class           // ★追加
        // ※FlowerEntity 等があるならここに追記
    ],
    version = 2,                         // ★ 1 → 2 に上げる
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao
    abstract fun experienceDao(): ExperienceDao   // ★追加

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sleep_garden.db"
                )
                    .addMigrations(MIGRATION_1_2)    // ★ マイグレーション登録
                    .build().also { INSTANCE = it }
            }

        // ★ v1 → v2：XPテーブルを追加
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `xp_session` (
                      `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                      `sleepAtMillis` INTEGER NOT NULL,
                      `wakeAtMillis` INTEGER NOT NULL,
                      `durationMin` INTEGER NOT NULL,
                      `gainedXp` INTEGER NOT NULL,
                      `note` TEXT,
                      `createdAtMillis` INTEGER NOT NULL
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `xp_summary` (
                      `id` INTEGER NOT NULL,
                      `totalXp` INTEGER NOT NULL,
                      `level` INTEGER NOT NULL,
                      `lastUpdatedMillis` INTEGER NOT NULL,
                      PRIMARY KEY(`id`)
                    )
                """.trimIndent())

                // 初期サマリ（total=0, level=1）を1行投入
                db.execSQL("""
                    INSERT OR REPLACE INTO xp_summary (id, totalXp, level, lastUpdatedMillis)
                    VALUES (1, 0, 1, strftime('%s','now')*1000)
                """.trimIndent())
            }
        }
    }
}
