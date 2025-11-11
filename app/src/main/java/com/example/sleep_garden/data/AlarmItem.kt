package com.example.sleep_garden.data
data class AlarmItem(
    val id: String = "",         // Firestore docId
    val hour: Int = 7,
    val minute: Int = 0,
    val enabled: Boolean = false
)