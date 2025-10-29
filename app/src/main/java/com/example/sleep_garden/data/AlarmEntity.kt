package com.example.sleep_garden.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey val id: String,   // UUID 文字列
    val hour: Int,
    val minute: Int,
    val enabled: Boolean,
    val updatedAt: Long           // epoch millis
)

fun AlarmEntity.toItem(): AlarmItem =
    AlarmItem(id = id, hour = hour, minute = minute, enabled = enabled)

fun AlarmItem.toEntity(updatedAt: Long): AlarmEntity =
    AlarmEntity(id = id, hour = hour, minute = minute, enabled = enabled, updatedAt = updatedAt)