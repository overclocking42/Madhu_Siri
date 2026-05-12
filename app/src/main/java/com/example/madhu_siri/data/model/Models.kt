package com.example.madhu_siri.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp

enum class UserRole {
    FARMER,
    BEEKEEPER;

    companion object {
        fun fromValue(value: String?): UserRole? = entries.firstOrNull { it.name == value }
    }
}

enum class AppThemePreference {
    SYSTEM,
    LIGHT,
    DARK;

    companion object {
        fun fromValue(value: String?): AppThemePreference = entries.firstOrNull { it.name == value } ?: SYSTEM
    }
}

enum class SprayType {
    PESTICIDE,
    FUNGICIDE,
    HERBICIDE,
    FOLIAR_NUTRIENT
}

enum class HealthLogType {
    HEALTH,
    HONEY
}

@Keep
data class User(
    val uid: String = "",
    val fullName: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val role: String = "",
    val preferredLanguage: String = "en",
    val themePreference: String = AppThemePreference.SYSTEM.name,
    val fcmToken: String = "",
    val createdAt: Timestamp = Timestamp.now()
)

@Keep
data class Hive(
    val id: String = "",
    val name: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val ownerId: String = "",
    val notes: String = "",
    val contactNumber: String = "",
    val activeStartTime: String = "08:00", // HH:mm
    val activeEndTime: String = "18:00",   // HH:mm
    val createdAt: Timestamp = Timestamp.now()
)

@Keep
data class SprayEvent(
    val id: String = "",
    val farmerId: String = "",
    val sprayType: String = "",
    val chemicalName: String = "",
    val durationHours: Int = 0,
    val scheduledAt: Timestamp = Timestamp.now(),
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val radiusKm: Double = 2.0,
    val notes: String = "",
    val createdAt: Timestamp = Timestamp.now()
) {
    fun sprayEndMillis(): Long {
        val start = scheduledAt.toDate().time
        return start + durationHours.coerceAtLeast(1) * 60L * 60L * 1000L
    }

    fun safeReleaseMillis(): Long {
        return sprayEndMillis() + 4L * 60L * 60L * 1000L
    }

    fun isActive(nowMillis: Long = System.currentTimeMillis()): Boolean {
        val start = scheduledAt.toDate().time
        return nowMillis in start..sprayEndMillis()
    }

    fun isVisibleForSafety(nowMillis: Long = System.currentTimeMillis()): Boolean {
        val start = scheduledAt.toDate().time
        return nowMillis in start..safeReleaseMillis()
    }

    fun remainingSafetyMillis(nowMillis: Long = System.currentTimeMillis()): Long {
        return (safeReleaseMillis() - nowMillis).coerceAtLeast(0L)
    }
}

@Keep
data class HealthLog(
    val id: String = "",
    val ownerId: String = "",
    val hiveId: String = "",
    val hiveName: String = "",
    val logType: String = "",
    val status: String = "",
    val metricValue: Double = 0.0,
    val metricUnit: String = "",
    val notes: String = "",
    val createdAt: Timestamp = Timestamp.now()
)

@Keep
data class NotificationAlert(
    val id: String = "",
    val recipientUserId: String = "",
    val sprayEventId: String = "",
    val title: String = "",
    val body: String = "",
    val timestamp: Timestamp = Timestamp.now()
)

data class SprayAlertDraft(
    val sprayType: SprayType = SprayType.PESTICIDE,
    val chemicalName: String = "",
    val durationHours: Int = 2,
    val scheduledAtMillis: Long = System.currentTimeMillis() + 60L * 60L * 1000L,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val notes: String = ""
)

data class SpraySubmissionResult(
    val eventId: String = "",
    val nearbyHiveCount: Int = 0,
    val notifiedBeekeeperCount: Int = 0,
    val error: String? = null,
    val conflictingHiveTimings: List<String> = emptyList(),
    val nextSafeStartMillis: Long? = null,
    val suggestedSafeTimes: List<Long> = emptyList(),
    val blockedByActiveBees: Boolean = false,
    val preventedDuplicate: Boolean = false
)
