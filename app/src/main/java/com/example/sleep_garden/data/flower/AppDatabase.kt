package com.example.sleep_garden.data.flower

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Flower::class],
    version = 2,
    exportSchema = true
)
abstract class FlowerDatabase : RoomDatabase() {

    abstract fun flowerDao(): FlowerDao

    companion object {
        @Volatile
        private var INSTANCE: FlowerDatabase? = null

        fun getInstance(context: Context): FlowerDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    FlowerDatabase::class.java,
                    "flower_db"          // ← 花専用DBのファイル名
                ).build().also { INSTANCE = it }
            }
    }
}
