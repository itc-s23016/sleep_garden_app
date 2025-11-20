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
        1 to 49,   // common
        2 to 25,   // uncommon
        3 to 15,   // rare
        4 to 7,    // epic
        5 to 3,
        6 to 1// legendary
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
                description = "太陽のように明るい花。黄色いだけがとりえ、種食われがち",
                imageResId = R.drawable.himawari,
                found = true
            ),
            Flower(
                name = "さくら",
                rarity = 4,
                description = "春を彩る花。写真はイメージです。沖縄だと雑魚、満開なんて見れやしない.",
                imageResId = R.drawable.hana,
                found = true
            ),
            Flower(
                name = "たんぽぽ",
                rarity = 1,
                description = "綿毛をとばして、黄色い花から綿に変化する。棉たまに飛ばずむしり取りがち。",
                imageResId = R.drawable.tannpopo,
                found = true
            ),
            Flower(
                name = "バラ",
                rarity = 5,
                description = "棘があり、きれい生命力は強い,きれいだから棘があるのか、棘があるからきれいなのか。" ,
                imageResId = R.drawable.bara,
                found = true
            ),
            Flower(
                name = "アルファナ",
                rarity = 6,
                description = "圧倒的な重厚感。周りに見せつける高級感。他とは一線を引く見た目。" ,
                imageResId = R.drawable.alfana,
                found = true
            ),
            Flower(
                name = "アサガオ",
                rarity = 1,
                description = "小学生の時よく育てる。土がアルカリだと青に、酸性だと赤になるはず。" ,
                imageResId = R.drawable.asagao,
                found = true
            ),
            Flower(
                name = "ガーベラ",
                rarity =2,
                description = "チェンソーマンにレぜにあげる花。花言葉は色によりけり" ,
                imageResId = R.drawable.gabera,
                found = true
            ),
            Flower(
                name = "ハイビスカス",
                rarity =5,
                description = "沖縄を象徴する花、観光客がよく頭につけている、どこで買えるかは謎である。" ,
                imageResId = R.drawable.haibisukasu,
                found = true
            ),
            Flower(
                name = "彼岸版",
                rarity = 5,
                description = "実物はあまり見たことがない、田舎ならそこらへんに生えてるイメージ、怖い話に使われがち" ,
                imageResId = R.drawable.higanbana,
                found = true
            ),
            Flower(
                name = "カーネーション",
                rarity = 3,
                description = "母の日に送られがちな花、色水を吸わされ色を変える拷問されがち" ,
                imageResId = R.drawable.kanesyon,
                found = true
            ),
            Flower(
                name = "金木犀",
                rarity =2,
                description = "香料にされがち、嗅いだことない。歌の題名にもされがち" ,
                imageResId = R.drawable.kinmokusei,
                found = true
            ),
            Flower(
                name = "コスモス",
                rarity = 1,
                description = "合唱曲での印象、コスモスはギリシャ語で宇宙、調和らしい。" ,
                imageResId = R.drawable.kosumosu,
                found = true
            ),
            Flower(
                name = "パンジー",
                rarity = 1,
                description = "よく見ると、ちょび髭もおっさんにんに似ている。" ,
                imageResId = R.drawable.panzi,
                found = true
            ),
            Flower(
                name = "睡蓮",
                rarity = 4,
                description = "睡蓮と蓮の違いは、睡蓮が浮いてて、蓮は茎が伸びている感じ" ,
                imageResId = R.drawable.suirenn,
                found = true
            ),
            Flower(
                name = "わかめ",
                rarity = 3,
                description = "校庭などに、いつの間にか生えてくるわかめ、一応食べれるらしい。" ,
                imageResId = R.drawable.wakame,
                found = true
            ),
            Flower(
                name = "ウツボカズラ",
                rarity =4,
                description = "あなに落ちる罠、溶かしてたべる。以外とホームセンターで売ってる。" ,
                imageResId = R.drawable.utubokazura,
                found = true
            ),
            Flower(
                name = "梅",
                rarity =5,
                description = "梅の花言葉は「上品」、「高潔」など女学生みたいな花言葉" ,
                imageResId = R.drawable.ume,
                found = true
            ),
            Flower(
                name = "すずらん",
                rarity =3,
                description = "見た目はガチで音なりそうな見た目、見たことはあんまない" ,
                imageResId = R.drawable.suzuran,
                found = true
            ),
            Flower(
                name = "ライラック",
                rarity =3,
                description = "あの頃の青を　覚えていようぜ　痛みが重なっても　アイシテル" ,
                imageResId = R.drawable.rairakku,
                found = true
            ),
            Flower(
                name = "ずんだもん",
                rarity =5,
                description = "そろそろ収穫なのだ、最近人気になって供給が追いつかないのだ" ,
                imageResId = R.drawable.zundamon,
                found = false
            ),
            Flower(
                name = "ラフレシア",
                rarity =5,
                description = "世界一大きい花、臭くハエに花粉を運んでもらう。" ,
                imageResId = R.drawable.rahuresia,
                found = true
            ),





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
