package com.example.sleep_garden.data.flower

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.sleep_garden.R
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class FlowerViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = FlowerDatabase.getInstance(app).flowerDao()
    private val repo = FlowerRepository(dao)

    val flowers: Flow<List<Flower>> = repo.flow

    /** アプリ起動時に実行 → 花の追加・更新を行う */
    fun insertInitialFlowers() = viewModelScope.launch {

        val list = listOf(
            Flower(
                name = "ひまわり",
                rarity = 3,
                description = "太陽のように明るい花。",
                imageResId = R.drawable.himawari,
                found = true
            ),
            Flower(
                name = "さくら",
                rarity = 4,
                description = "春を彩る花。写真はイメージです",
                imageResId = R.drawable.hana,
                found = true
            ),
            Flower(
                name = "たんぽぽ",
                rarity = 1,
                description = "綿毛をとばして、黄色い花から綿に変化する。",
                imageResId = R.drawable.tannpopo,
                found = true
            ),
            Flower(
                name = "バラ",
                rarity = 5,
                description = "棘があり、きれい生命力は強い1",
                imageResId = R.drawable.bara,
                found = true
            ),
            // 追加したい花をどんどんここに書いていく
        )

        // ★ ここ！ 新規なら追加／既存なら更新
        for (f in list) {
            repo.upsert(f)
        }
    }
}
