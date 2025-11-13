package com.example.sleep_garden.data.flower

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "flowers")
data class Flower(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val name: String,
    val rarity: Int,
    val description: String,


    val imageResId: Int,

    val found: Boolean
)
