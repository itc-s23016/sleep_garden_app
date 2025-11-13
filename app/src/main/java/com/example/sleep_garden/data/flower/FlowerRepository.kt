package com.example.sleep_garden.data.flower

import kotlinx.coroutines.flow.Flow

class FlowerRepository(private val dao: FlowerDao) {

    val flow = dao.getAllFlow()

    // 追加
    suspend fun insertAll(list: List<Flower>) = dao.insertAll(list)

    // 更新
    suspend fun update(flower: Flower) = dao.update(flower)

    // ★ 名前で存在チェック → 追加 or 更新
    suspend fun upsert(flower: Flower) {
        val exist = dao.findByName(flower.name)
        if (exist == null) {
            dao.insert(flower) // 新規追加
        } else {
            // IDは既存のものを引き継ぐ
            val updated = flower.copy(id = exist.id)
            dao.update(updated)
        }
        suspend fun upsert(f: Flower) {
            dao.insert(f)  // `OnConflictStrategy.REPLACE` なら更新になる
        }
    }


}
