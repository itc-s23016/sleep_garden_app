package com.example.sleep_garden.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class LocalAlarmRepository private constructor(context: Context) {

    private val dao = AppDatabase.get(context).alarmDao()

    /** 複数アラーム購読（時→分で並び替えはSQLで実施済み） */
    fun observeAlarms(): Flow<List<AlarmItem>> =
        dao.observeAll().map { list -> list.map { it.toItem() } }

    /** 追加（UUID を主キーに）。デフォルトで有効にして作成可能 */
    suspend fun addAlarm(hour: Int, minute: Int, enabled: Boolean = true): String {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val entity = AlarmEntity(
            id = id,
            hour = hour,
            minute = minute,
            enabled = enabled,
            updatedAt = now
        )
        dao.insert(entity)
        return id
    }

    /** 時刻更新 */
    suspend fun updateTime(id: String, hour: Int, minute: Int) {
        dao.updateTime(id, hour, minute, System.currentTimeMillis())
    }

    /** ON/OFF 切替 */
    suspend fun setEnabled(id: String, enabled: Boolean) {
        dao.setEnabled(id, enabled, System.currentTimeMillis())
    }

    /** 削除 */
    suspend fun delete(id: String) {
        dao.deleteById(id)
    }

    companion object {
        @Volatile private var INSTANCE: LocalAlarmRepository? = null

        fun getInstance(context: Context): LocalAlarmRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: LocalAlarmRepository(context.applicationContext).also { INSTANCE = it }
            }
    }
}