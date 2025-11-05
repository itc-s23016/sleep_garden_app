package com.example.sleep_garden.data

import android.content.Context
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * 1分 = 1XP
 * Lv1→2 必要XP = 300、以後 1.5倍、上限 Lv10。
 * 例: Lv1:300 / Lv2:450 / Lv3:675 ...
 */
class XpRepository private constructor(ctx: Context) {

    private val sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    fun getLevel(): Int = sp.getInt(KEY_LEVEL, 1)
    fun getXp(): Int = sp.getInt(KEY_XP, 0)

    /** 指定レベルの必要XP（Lv>=MAX は 0） */
    fun getRequiredXpFor(level: Int): Int {
        if (level >= MAX_LEVEL) return 0
        val n = level - 1
        val req = BASE_REQ * (RATIO.pow(n))
        return req.roundToInt()
    }

    /**
     * XP を加算し、必要に応じて複数レベルアップも処理。
     * @return (付与量, 新Lv, 新カレントXP, 新必要値, レベルアップ到達Lv or null)
     */
    fun addXpAndLevelUp(add: Int): Result {
        if (add <= 0) {
            val lv = getLevel()
            return Result(0, lv, getXp(), getRequiredXpFor(lv), null)
        }
        var lv = getLevel()
        var xp = getXp() + add
        var leveledTo: Int? = null

        while (lv < MAX_LEVEL) {
            val req = getRequiredXpFor(lv)
            if (xp >= req) {
                xp -= req
                lv += 1
                leveledTo = lv
            } else break
        }
        if (lv >= MAX_LEVEL) {
            // MAX 到達後は XP は飾り。0で固定にする場合は下を 0 に。
            xp = minOf(xp, 0)
        }

        sp.edit().putInt(KEY_LEVEL, lv).putInt(KEY_XP, xp).apply()
        return Result(add, lv, xp, getRequiredXpFor(lv), leveledTo)
    }

    data class Result(
        val added: Int,
        val newLevel: Int,
        val newXp: Int,
        val newRequired: Int,
        val leveledUpTo: Int?
    )

    companion object {
        private const val PREF = "xp_pref"
        private const val KEY_LEVEL = "level"
        private const val KEY_XP = "xp"

        const val MAX_LEVEL = 10
        private const val BASE_REQ = 300.0
        private const val RATIO = 1.5

        @Volatile private var INSTANCE: XpRepository? = null
        fun getInstance(ctx: Context): XpRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: XpRepository(ctx.applicationContext).also { INSTANCE = it }
            }
    }
}
