// app/src/main/java/com/example/sleep_garden/data/FlowerRepository.kt
package com.example.sleep_garden.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase

data class Flower(
    val id: Long? = null,
    val name: String,
    val rarity: Int,
    val description: String,
    val imageUrl: String? = null,
    val found: Boolean = false
)

class FlowerRepository(context: Context) {
    private val dbHelper = FlowerDbHelper.getInstance(context)

    /** 通常のINSERT（同名があれば例外：name UNIQUE + CONFLICT_ABORT） */
    fun insertFlower(f: Flower): Long {
        val db: SQLiteDatabase = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put(FlowerContract.Flowers.COL_NAME, f.name)
            put(FlowerContract.Flowers.COL_RARITY, f.rarity)
            put(FlowerContract.Flowers.COL_DESC, f.description)
            put(FlowerContract.Flowers.COL_IMAGE_URL, f.imageUrl)
            put(FlowerContract.Flowers.COL_FOUND, if (f.found) 1 else 0)
        }
        return db.insertWithOnConflict(
            FlowerContract.Flowers.TABLE, null, cv, SQLiteDatabase.CONFLICT_ABORT
        )
    }



    /** 一覧取得（レアリティ降順→名前昇順） */
    fun getAllFlowers(): List<Flower> {
        val db: SQLiteDatabase = dbHelper.readableDatabase
        val list = mutableListOf<Flower>()

        val c: Cursor = db.query(
            FlowerContract.Flowers.TABLE,
            arrayOf(
                FlowerContract.Flowers.COL_ID,
                FlowerContract.Flowers.COL_NAME,
                FlowerContract.Flowers.COL_RARITY,
                FlowerContract.Flowers.COL_DESC,
                FlowerContract.Flowers.COL_IMAGE_URL,
                FlowerContract.Flowers.COL_FOUND
            ),
            null, null, null, null,
            "${FlowerContract.Flowers.COL_RARITY} DESC, ${FlowerContract.Flowers.COL_NAME} ASC"
        )

        c.use {
            val idxId    = it.getColumnIndexOrThrow(FlowerContract.Flowers.COL_ID)
            val idxName  = it.getColumnIndexOrThrow(FlowerContract.Flowers.COL_NAME)
            val idxRt    = it.getColumnIndexOrThrow(FlowerContract.Flowers.COL_RARITY)
            val idxDesc  = it.getColumnIndexOrThrow(FlowerContract.Flowers.COL_DESC)
            val idxUrl   = it.getColumnIndexOrThrow(FlowerContract.Flowers.COL_IMAGE_URL)
            val idxFound = it.getColumnIndexOrThrow(FlowerContract.Flowers.COL_FOUND)

            while (it.moveToNext()) {
                list.add(
                    Flower(
                        id          = it.getLong(idxId),
                        name        = it.getString(idxName),
                        rarity      = it.getInt(idxRt),
                        description = it.getString(idxDesc),
                        imageUrl    = it.getString(idxUrl),
                        found       = it.getInt(idxFound) == 1
                    )
                )
            }
        }
        return list
    }



    /** 発見フラグをID指定で更新 */
    fun setFound(id: Long, value: Boolean): Int {
        val cv = ContentValues().apply {
            put(FlowerContract.Flowers.COL_FOUND, if (value) 1 else 0)
        }
        return dbHelper.writableDatabase.update(
            FlowerContract.Flowers.TABLE, cv,
            "${FlowerContract.Flowers.COL_ID}=?", arrayOf(id.toString())
        )
    }

    /** 発見フラグを名前指定で更新（IDが手元に無い時用） */
    fun setFoundByName(name: String, value: Boolean): Int {
        val cv = ContentValues().apply {
            put(FlowerContract.Flowers.COL_FOUND, if (value) 1 else 0)
        }
        return dbHelper.writableDatabase.update(
            FlowerContract.Flowers.TABLE, cv,
            "${FlowerContract.Flowers.COL_NAME}=?", arrayOf(name)
        )
    }

    /** 発見済みだけ取得 */
    fun getDiscovered(): List<Flower> = queryByFound(true)

    /** 未発見だけ取得 */
    fun getUndiscovered(): List<Flower> = queryByFound(false)

    /** found 条件付き共通クエリ */
    private fun queryByFound(found: Boolean): List<Flower> {
        val db = dbHelper.readableDatabase
        val list = mutableListOf<Flower>()
        val c = db.query(
            FlowerContract.Flowers.TABLE,
            arrayOf(
                FlowerContract.Flowers.COL_ID,
                FlowerContract.Flowers.COL_NAME,
                FlowerContract.Flowers.COL_RARITY,
                FlowerContract.Flowers.COL_DESC,
                FlowerContract.Flowers.COL_IMAGE_URL,
                FlowerContract.Flowers.COL_FOUND
            ),
            "${FlowerContract.Flowers.COL_FOUND}=?",
            arrayOf(if (found) "1" else "0"),
            null, null,
            "${FlowerContract.Flowers.COL_RARITY} DESC, ${FlowerContract.Flowers.COL_NAME} ASC"
        )
        c.use {
            val idxId    = it.getColumnIndexOrThrow(FlowerContract.Flowers.COL_ID)
            val idxName  = it.getColumnIndexOrThrow(FlowerContract.Flowers.COL_NAME)
            val idxRt    = it.getColumnIndexOrThrow(FlowerContract.Flowers.COL_RARITY)
            val idxDesc  = it.getColumnIndexOrThrow(FlowerContract.Flowers.COL_DESC)
            val idxUrl   = it.getColumnIndexOrThrow(FlowerContract.Flowers.COL_IMAGE_URL)
            val idxFound = it.getColumnIndexOrThrow(FlowerContract.Flowers.COL_FOUND)
            while (it.moveToNext()) {
                list.add(
                    Flower(
                        id          = it.getLong(idxId),
                        name        = it.getString(idxName),
                        rarity      = it.getInt(idxRt),
                        description = it.getString(idxDesc),
                        imageUrl    = it.getString(idxUrl),
                        found       = it.getInt(idxFound) == 1
                    )
                )
            }
        }
        return list
    }
    fun debugListTables(): List<String> {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null)
        val tables = mutableListOf<String>()
        cursor.use {
            while (it.moveToNext()) {
                tables.add(it.getString(0))
            }
        }
        return tables
    }
    fun clearAllFlowers() {
        val db = dbHelper.writableDatabase
        db.execSQL("DELETE FROM ${FlowerContract.Flowers.TABLE}")
        db.close()
    }


}
