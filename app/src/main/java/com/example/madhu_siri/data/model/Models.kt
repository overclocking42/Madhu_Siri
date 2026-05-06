package com.example.madhu_siri.data.model

import com.google.firebase.Timestamp

data class User(
    val uid: String = "",
    val role: String = "" // "BEEKEEPER" or "FARMER"
)

data class Hive(
    val id: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val ownerId: String = ""
)

data class SprayEvent(
    val id: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val timestamp: Timestamp = Timestamp.now()
)

data class NotificationAlert(
    val title: String = "",
    val body: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
