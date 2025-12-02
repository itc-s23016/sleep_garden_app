package com.example.sleep_garden.data.flower

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.sleep_garden.R
import com.example.sleep_garden.data.XpRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FlowerViewModel(app: Application) : AndroidViewModel(app) {

    private val rarityWeights = mapOf(
        1 to 49,
        2 to 25,
        3 to 15,
        4 to 7,
        5 to 3,
        6 to 1
    )

    private val dao = FlowerDatabase.getInstance(app).flowerDao()
    private val repo = FlowerRepository(dao)

    val flowers: Flow<List<Flower>> = repo.flow


    /** ←←← ここは一切変更していない（要求どおり） */
    fun insertInitialFlowers() = viewModelScope.launch {
        val list = listOf(
            Flower(
                name = "ひまわり",
                rarity = 3,
                description = "太陽のように明るい花。黄色いだけがとりえ、種食われがち",
                imageResId = R.drawable.himawari,
                found = false
            ),
            Flower(
                name = "さくら",
                rarity = 4,
                description = "春を彩る花。写真はイメージです。沖縄だと雑魚、満開なんて見れやしない.",
                imageResId = R.drawable.hana,
                found = false
            ),
            Flower(
                name = "たんぽぽ",
                rarity = 1,
                description = "綿毛をとばして、黄色い花から綿に変化する。棉たまに飛ばずむしり取りがち。",
                imageResId = R.drawable.tannpopo,
                found = false
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
                name = "イシクラゲ",
                rarity = 3,
                description = "校庭などに、いつの間にか生えてくる藻の一種、鉄分がぶどうの約３２倍あるらしい。" ,
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
                found = true
            ),
            Flower(
                name = "ラフレシア",
                rarity =5,
                description = "世界一大きい花、臭くハエに花粉を運んでもらう。" ,
                imageResId = R.drawable.rahuresia,
                found = true
            ),
        )

        val all = list
        for (f in all) {
            val exist = dao.findByName(f.name)
            if (exist == null) {
                repo.insert(f)
            } else {
                val keep = exist.found
                repo.update(
                    f.copy(
                        id = exist.id,
                        found = keep
                    )
                )
            }
        }
    }

    /* ここから下は追加・変更部分のみ */


    /** ★ 重み付きランダム抽選（必須！） */
    private fun pickWeighted(flowerList: List<Flower>): Flower {
        val totalWeight = flowerList.sumOf { rarityWeights[it.rarity] ?: 1 }
        var r = (0 until totalWeight).random()
        for (f in flowerList) {
            val weight = rarityWeights[f.rarity] ?: 1
            if (r < weight) return f
            r -= weight
        }
        return flowerList.last()
    }

    /** レベルごとの★確率 */
    private fun getStarProbabilities(level: Int): List<Float> {
        return when (level) {
            1 -> listOf(0.50f, 0.30f, 0.15f, 0.04f, 0.01f, 0f)
            2 -> listOf(0.48f, 0.30f, 0.16f, 0.05f, 0.01f, 0f)
            3 -> listOf(0.45f, 0.30f, 0.17f, 0.06f, 0.02f, 0f)
            4 -> listOf(0.42f, 0.30f, 0.18f, 0.07f, 0.03f, 0f)
            5 -> listOf(0.38f, 0.30f, 0.18f, 0.09f, 0.04f, 0.01f)
            6 -> listOf(0.34f, 0.28f, 0.20f, 0.10f, 0.06f, 0.02f)
            7 -> listOf(0.30f, 0.28f, 0.20f, 0.12f, 0.07f, 0.03f)
            8 -> listOf(0.25f, 0.27f, 0.20f, 0.13f, 0.10f, 0.05f)
            9 -> listOf(0.20f, 0.25f, 0.22f, 0.15f, 0.12f, 0.06f)
            10 -> listOf(0.15f, 0.23f, 0.22f, 0.17f, 0.14f, 0.09f)
            else -> listOf(1f, 0f, 0f, 0f, 0f, 0f)
        }
    }

    private fun drawStar(prob: List<Float>): Int {
        val r = Math.random().toFloat()
        var acc = 0f
        for (i in prob.indices) {
            acc += prob[i]
            if (r <= acc) return (i + 1)
        }
        return 1
    }

    /**
     * ★ ここが超重要 ★
     * snoozed=true のときは「絶対に花を返さない（null）」。
     */
    suspend fun rewardRandomFlowerIfEligible(
        minutes: Int,
        snoozed: Boolean
    ): Flower? = withContext(Dispatchers.IO) {

        // スヌーズだったら絶対に花ガチャしない
        if (snoozed) return@withContext null

        // そもそも0分以下なら何も出さない
        if (minutes <= 0) return@withContext null

        val all = dao.getAll()
        if (all.isEmpty()) return@withContext null

        // 未発見を優先
        val notFound = all.filter { !it.found }
        val baseList = if (notFound.isNotEmpty()) notFound else all

        // レベル取得
        val ctx: Context = getApplication<Application>().applicationContext
        val xpRepo = XpRepository.getInstance(ctx)
        val level = xpRepo.getLevel()

        // レア度（★）を抽選
        val star = drawStar(getStarProbabilities(level))
        val candidates = baseList.filter { it.rarity == star }
        if (candidates.isEmpty()) return@withContext null

        // 同じ★の中から重み付きで抽選
        val picked = pickWeighted(candidates)

        // 未発見なら found=true に更新
        if (!picked.found) {
            repo.update(picked.copy(found = true))
        }

        picked
    }
}