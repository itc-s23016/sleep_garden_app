package com.example.sleep_garden.data

import android.content.Context

/**
 * 「寝る」ボタンで開始時刻(UTCミリ秒)を保存。「起きる」で取り出して差分(分)を計算。
 * 再起動しても保持できるように SharedPreferences を使用。
 */
class SleepSessionStore private constructor(context: Context) {
    private val sp = context.getSharedPreferences("sleep_session", Context.MODE_PRIVATE)

    fun start(now: Long = System.currentTimeMillis()) {
        sp.edit().putLong("sleep_start", now).apply()
    }

    fun clear() {
        sp.edit().remove("sleep_start").apply()
    }

    fun getStart(): Long = sp.getLong("sleep_start", 0L)

    fun isSleeping(): Boolean = getStart() > 0L

    companion object {
        @Volatile private var INSTANCE: SleepSessionStore? = null
        fun getInstance(context: Context): SleepSessionStore =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: SleepSessionStore(context.applicationContext).also { INSTANCE = it }
            }
    }
}
