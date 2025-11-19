package com.example.sleep_garden.data.flower

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.sleep_garden.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FlowerViewModel(app: Application) : AndroidViewModel(app) {

    private val rarityWeights = mapOf(
        1 to 50,   // common
        2 to 25,   // uncommon
        3 to 15,   // rare
        4 to 7,    // epic
        5 to 3     // legendary
    )


    private val dao = FlowerDatabase.getInstance(app).flowerDao()
    private val repo = FlowerRepository(dao)

    val flowers: Flow<List<Flower>> = repo.flow

    /**
     * アプリ起動時：花の初期追加 / 更新
     */
    fun insertInitialFlowers() = viewModelScope.launch {
        val list = listOf(
            Flower(
                name = "ひまわり",
                rarity = 3,
                description = "太陽のように明るい花。",
                imageResId = R.drawable.himawari,
                found = false
            ),
            Flower(
                name = "さくら",
                rarity = 4,
                description = "春を彩る花。写真はイメージです",
                imageResId = R.drawable.hana,
                found = false
            ),
            Flower(
                name = "たんぽぽ",
                rarity = 1,
                description = "綿毛をとばして、黄色い花から綿に変化する。",
                imageResId = R.drawable.tannpopo,
                found = false
            ),
            Flower(
                name = "バラ",
                rarity = 5,
                description = "棘があり、きれい生命力は強い",
                imageResId = R.drawable.bara,
                found = false
            )
        )

        // ★ 新規なら追加、既存なら上書き
        for (f in list) {
            val exist = dao.findByName(f.name)

            if (exist == null) {
                // 新規追加のみ found=false を採用
                repo.insert(f)
            } else {
                // 既存の found 状態は壊さない！
                val keepFound = exist.found
                repo.update(
                    f.copy(
                        id = exist.id,
                        found = keepFound    // ここが重要！
                    )
                )
            }
        }

    }

    /** 重み付きランダム抽選 */
    private fun pickWeighted(flowerList: List<Flower>): Flower {
        // 合計重み
        val totalWeight = flowerList.sumOf { rarityWeights[it.rarity] ?: 1 }

        // 0 〜 合計重み の中から乱数を取る
        var r = (0 until totalWeight).random()

        for (f in flowerList) {
            val weight = rarityWeights[f.rarity] ?: 1
            if (r < weight) {
                return f
            }
            r -= weight
        }

        // ここに来ることはほとんどないが保険
        return flowerList.last()
    }


    /**
     * ランダムで未発見の花を1つ入手する
     */
    suspend fun unlockRandomFlower(): Flower? =
        withContext(Dispatchers.IO) {
            val all = dao.getAll()
            val notFound = all.filter { !it.found }
            if (notFound.isEmpty()) return@withContext null

            val picked = notFound.random()
            repo.update(picked.copy(found = true))
            picked
        }

    /**
     * 5時間(300分)以上眠ったらランダム報酬
     */
    /**
     * 5時間(=300分)以上寝ていたら、レア度に応じて花をランダム獲得
     */
    suspend fun rewardRandomFlowerIfEligible(minutes: Int): Flower? =
        withContext(Dispatchers.IO) {

            if (minutes < 0.1) return@withContext null

            val all = dao.getAll()
            if (all.isEmpty()) return@withContext null

            // 未発見を優先
            val notFound = all.filter { !it.found }
            val candidates = if (notFound.isNotEmpty()) notFound else all

            // ★ 重み付きランダム抽選
            val picked = pickWeighted(candidates)


            // 未発見なら found = true に更新
            if (!picked.found) {
                repo.update(picked.copy(found = true))
            }

            picked
        }
}
