package com.example.sleep_garden.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FlowerViewModel(app: Application) : AndroidViewModel(app) {

    private val repo by lazy { FlowerRepository(app.applicationContext) }

    private val _flowers = MutableStateFlow<List<Flower>>(emptyList())
    val flowers = _flowers.asStateFlow()

    /** 🌱 アプリ起動のたびにDBを更新（常に実行） */
    init {
        viewModelScope.launch(Dispatchers.IO) {
            println("🌼 アプリ起動：DBを最新データに更新します")
            addSampleAll()  // ← 毎回呼び出すように変更！
        }
    }

    /** 🌼 一覧を再読込（DBから取得） */
    fun refresh() = viewModelScope.launch(Dispatchers.IO) {
        try {
            val list = repo.getAllFlowers()
            _flowers.value = list
            println("🌸 花データ読込: ${list.size}件")
        } catch (e: Exception) {
            e.printStackTrace()
            _flowers.value = emptyList()
        }
    }

    /** 🌷 任意の花を追加（同名なら上書き） */
    fun addFlower(f: Flower) = viewModelScope.launch(Dispatchers.IO) {
        try {
            repo.insertFlower(f)
            println("🌺 花追加: ${f.name}")
            refresh()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** 🌻 サンプル花5種類を登録（起動時に毎回呼ばれる） */
    fun addSampleAll() = viewModelScope.launch(Dispatchers.IO) {
        val list = listOf(
            Flower(
                name = "ひまわり",
                rarity = 2,
                description = "夏を代表する花。太陽のように大きく咲く。",
                imageUrl = "https://upload.wikimedia.org/wikipedia/commons/4/40/Sunflower_sky_backdrop.jpg",
                found = true
            ),
            Flower(
                name = "チューリップ",
                rarity = 1,
                description = "春に咲く球根植物。色や形が豊富。",
                imageUrl = "https://upload.wikimedia.org/wikipedia/commons/4/45/Red_tulip_flower.jpg",
                found = true
            ),
            Flower(
                name = "バラ",
                rarity = 3,
                description = "愛を象徴する花。色によって意味が異なる。",
                imageUrl = "https://upload.wikimedia.org/wikipedia/commons/b/bf/Red_rose.jpg",
                found = true
            ),
            Flower(
                name = "ガーベラ",
                rarity = 1,
                description = "たくさん咲いてるときれい。",
                imageUrl = "https://upload.wikimedia.org/wikipedia/commons/8/84/Gerbera_daisy_yellow.jpg",
                found = true
            ),
            Flower(
                name = "ラフレシア",
                rarity = 5,
                description = "世界一臭くて大きな花。色は赤色",
                imageUrl = "https://upload.wikimedia.org/wikipedia/commons/9/9d/Rafflesia_arnoldii_bunga_terbesar_di_dunia.jpg",
                found = true
            ),
            Flower(
                name = "ガーベラ",
                rarity = 2,
                description = "いろいろな色があり、一番親しみやすい花",
                imageUrl = "Image/hana.png",
                found = true
        )
        )

        try {
            // ✅ 古いデータを一度削除して最新データを入れ直す
            repo.clearAllFlowers()
            list.forEach { repo.insertFlower(it) }

            println("🌷 花データを最新状態に更新しました（${list.size}件）")
            refresh()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
