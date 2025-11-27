package com.example.sleep_garden.data.xp

object XpFormula {
    const val XP_PER_MIN: Int = 2
    fun gained(durationMin: Int): Int =
        (durationMin.coerceAtLeast(0) * XP_PER_MIN)
}

data class LevelInfo(
    val level: Int,
    val currentLevelStartXp: Int,
    val nextLevelStartXp: Int,
    val progressInLevel: Int,
    val needForNext: Int
)

object Leveling {


    private val LEVEL_START_XP = intArrayOf(
        0,      // Lv1
        300,    // Lv2
        900,    // Lv3
        1800,   // Lv4
        3000,   // Lv5
        4500,   // Lv6
        6300,   // Lv7
        8400,   // Lv8
        10800,  // Lv9
        13500   // Lv10
    )

    const val MAX_LEVEL = 10

    fun compute(totalXp: Int): LevelInfo {
        val xp = totalXp.coerceAtLeast(0)

        // ★ レベル決定
        var idx = 0
        for (i in LEVEL_START_XP.indices) {
            if (i == LEVEL_START_XP.size - 1 || xp < LEVEL_START_XP[i + 1]) {
                idx = i
                break
            }
        }

        val level = idx + 1
        val curStart = LEVEL_START_XP[idx]
        val nextStart =
            if (level >= MAX_LEVEL) Int.MAX_VALUE
            else LEVEL_START_XP[idx + 1]

        val progress = xp - curStart
        val need = if (level >= MAX_LEVEL) 0 else (nextStart - xp).coerceAtLeast(0)

        return LevelInfo(level, curStart, nextStart, progress, need)
    }
}
