package com.example.sleep_garden.data.flower

import kotlinx.coroutines.flow.Flow

class FlowerRepository(private val dao: FlowerDao) {

    val flow: Flow<List<Flower>> = dao.getAllFlow()

    // まとめて追加
    suspend fun insertAll(list: List<Flower>) = dao.insertAll(list)

    // 個別追加
    suspend fun insert(flower: Flower) = dao.insert(flower)

    // 更新
    suspend fun update(flower: Flower) = dao.update(flower)

    /**
     * ★ 正しい upsert
     *   - name で既存チェック
     *   - なければ insert
     *   - あれば ID を引き継いで update
     */
    suspend fun upsert(flower: Flower) {
        val exist = dao.findByName(flower.name)
        if (exist == null) {
            dao.insert(flower)       // 新規
        } else {
            val updated = flower.copy(id = exist.id)
            dao.update(updated)     // 更新
        }
    }

    suspend fun unlockRandomFlower(): Flower? {
        val all = dao.getAllOnce()
        val notFound = all.filter { !it.found }

        if (notFound.isEmpty()) return null

        val picked = notFound.random()
        dao.setFound(picked.name)
        return picked
    }
}
