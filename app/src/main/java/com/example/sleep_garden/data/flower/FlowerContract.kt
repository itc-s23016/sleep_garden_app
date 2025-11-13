//package com.example.sleep_garden.data
//
//object FlowerContract {
//    //const val DB_NAME = "flowerdex.db"
//    const val DB_NAME = "sleep_garden.db"
//    const val DB_VERSION = 3 //
//
//    object Flowers {
//        const val TABLE        = "flowers"
//        const val COL_ID       = "flower_id"
//        const val COL_NAME     = "name"
//        const val COL_RARITY   = "rarity"
//        const val COL_DESC     = "description"
//        const val COL_IMAGE_URL= "image_url"
//        const val COL_FOUND    = "found" // ★ 見つけたかどうか（0/1）
//
//        val CREATE_TABLE = """
//            CREATE TABLE IF NOT EXISTS $TABLE (
//              $COL_ID        INTEGER PRIMARY KEY AUTOINCREMENT,
//              $COL_NAME      TEXT    NOT NULL,
//              $COL_RARITY    INTEGER NOT NULL CHECK($COL_RARITY BETWEEN 1 AND 5),
//              $COL_DESC      TEXT    NOT NULL,
//              $COL_IMAGE_URL TEXT,
//              $COL_FOUND     INTEGER NOT NULL DEFAULT 0 CHECK($COL_FOUND IN (0,1))
//            );
//        """.trimIndent()
//
//        val CREATE_INDEX_RARITY = """
//            CREATE INDEX IF NOT EXISTS idx_${TABLE}_${COL_RARITY}
//            ON $TABLE($COL_RARITY);
//        """.trimIndent()
//
//        // 名前検索が多いなら
//        val CREATE_INDEX_NAME = """
//            CREATE INDEX IF NOT EXISTS idx_${TABLE}_${COL_NAME}
//            ON $TABLE($COL_NAME);
//        """.trimIndent()
//    }
//}
