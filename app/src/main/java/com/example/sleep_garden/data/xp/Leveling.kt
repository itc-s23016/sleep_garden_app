package com.example.sleep_garden.data.xp

object XpFormula {
    // 分あたりXP（お好みで調整）
    const val XP_PER_MIN: Int = 2
    fun gained(durationMin: Int): Int = (durationMin.coerceAtLeast(0) * XP_PER_MIN)
}

data class LevelInfo(
    val level: Int,
    val currentLevelStartXp: Int, // 現レベルの開始累計XP
    val nextLevelStartXp: Int,    // 次レベルの開始累計XP
    val progressInLevel: Int,     // 現レベル内で積んだXP
    val needForNext: Int          // 次レベル到達までに必要なXP
)

object Leveling {
    // レベル上限3。しきい値は「累計XPがこの値以上でそのレベル」。
    // L1: [0, 300) / L2: [300, 900) / L3: [900, ∞)
    private val LEVEL_START_XP = intArrayOf(0, 300, 900)
    const val MAX_LEVEL = 3

    fun compute(totalXp: Int): LevelInfo {
        val xp = totalXp.coerceAtLeast(0)
        val idx = when {
            xp < LEVEL_START_XP[1] -> 0
            xp < LEVEL_START_XP[2] -> 1
            else -> 2
        }
        val level = idx + 1
        val curStart = LEVEL_START_XP[idx]
        val nextStart = if (level >= MAX_LEVEL) Int.MAX_VALUE else LEVEL_START_XP[idx + 1]
        val progress = xp - curStart
        val need = if (level >= MAX_LEVEL) 0 else (nextStart - xp).coerceAtLeast(0)
        return LevelInfo(level, curStart, nextStart, progress, need)
    }
}
