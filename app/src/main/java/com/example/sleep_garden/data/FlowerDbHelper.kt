package com.example.sleep_garden.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class FlowerDbHelper private constructor(context: Context)
    : SQLiteOpenHelper(context, FlowerContract.DB_NAME, null, FlowerContract.DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(FlowerContract.Flowers.CREATE_TABLE)
        db.execSQL(FlowerContract.Flowers.CREATE_INDEX_RARITY)
        db.execSQL(FlowerContract.Flowers.CREATE_INDEX_NAME)
    }


    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // v2: found 列を追加
        if (oldVersion < 2) {
            db.execSQL(
                """
                ALTER TABLE ${FlowerContract.Flowers.TABLE}
                ADD COLUMN ${FlowerContract.Flowers.COL_FOUND} INTEGER NOT NULL DEFAULT 0
                """.trimIndent()
            )
            // 念のためインデックス再作成（IF NOT EXISTS なので多重でも安全）
            db.execSQL(FlowerContract.Flowers.CREATE_INDEX_RARITY)
            db.execSQL(FlowerContract.Flowers.CREATE_INDEX_NAME)
        }
        // v3以降の移行をここに積む
        // if (oldVersion < 3) { ... }
    }

    companion object {
        @Volatile private var INSTANCE: FlowerDbHelper? = null
        fun getInstance(context: Context): FlowerDbHelper =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: FlowerDbHelper(context.applicationContext).also { INSTANCE = it }
            }
    }
}
