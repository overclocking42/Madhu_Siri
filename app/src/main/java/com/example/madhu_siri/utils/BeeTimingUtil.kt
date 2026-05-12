package com.example.madhu_siri.utils

import com.example.madhu_siri.data.model.Hive
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

object BeeTimingUtil {
    private val zoneId: ZoneId = ZoneId.systemDefault()
    private const val PRE_BEE_RELEASE_BUFFER_HOURS = 4L

    fun sanitizeTime(value: String, fallback: String): String {
        return runCatching { LocalTime.parse(value).toString().take(5) }.getOrElse { fallback }
    }

    fun isWithinActiveWindow(hive: Hive, timeMillis: Long): Boolean {
        val target = LocalDateTime.ofInstant(Instant.ofEpochMilli(timeMillis), zoneId)
        return activeWindowForDate(hive, target.toLocalDate()).let { target >= it.first && target < it.second } ||
            activeWindowForDate(hive, target.toLocalDate().minusDays(1)).let { target >= it.first && target < it.second }
    }

    fun isSprayBlockedByBeeWindow(hive: Hive, timeMillis: Long): Boolean {
        val target = LocalDateTime.ofInstant(Instant.ofEpochMilli(timeMillis), zoneId)
        val relevantDates = listOf(target.toLocalDate().minusDays(1), target.toLocalDate(), target.toLocalDate().plusDays(1))
        return relevantDates.any { date ->
            val activeWindow = activeWindowForDate(hive, date)
            val blockedStart = activeWindow.first.minusHours(PRE_BEE_RELEASE_BUFFER_HOURS)
            target >= blockedStart && target < activeWindow.second
        }
    }

    fun nextSafeSprayTimeMillis(hives: List<Hive>, scheduledAtMillis: Long): Long? {
        if (hives.isEmpty()) return null
        var candidate = LocalDateTime.ofInstant(Instant.ofEpochMilli(scheduledAtMillis), zoneId)
        repeat(96 * 3) {
            if (hives.none { isSprayBlockedByBeeWindow(it, candidate.atZone(zoneId).toInstant().toEpochMilli()) }) {
                return candidate.atZone(zoneId).toInstant().toEpochMilli()
            }
            val nextEnd = hives.filter { isSprayBlockedByBeeWindow(it, candidate.atZone(zoneId).toInstant().toEpochMilli()) }
                .map { hive ->
                    val windows = listOf(
                        activeWindowForDate(hive, candidate.toLocalDate().minusDays(1)),
                        activeWindowForDate(hive, candidate.toLocalDate()),
                        activeWindowForDate(hive, candidate.toLocalDate().plusDays(1))
                    )
                    windows.firstOrNull { candidate >= it.first.minusHours(PRE_BEE_RELEASE_BUFFER_HOURS) && candidate < it.second }?.second
                        ?: candidate
                }
                .maxOrNull() ?: candidate.plusMinutes(15)
            candidate = if (nextEnd.isAfter(candidate)) nextEnd else candidate.plusMinutes(15)
        }
        return candidate.atZone(zoneId).toInstant().toEpochMilli()
    }

    fun suggestedSafeSprayTimes(hives: List<Hive>, fromMillis: Long, limit: Int = 6): List<Long> {
        if (hives.isEmpty()) return emptyList()
        val suggestions = mutableListOf<Long>()
        var candidate = nextSafeSprayTimeMillis(hives, fromMillis) ?: return emptyList()
        val latest = LocalDateTime.ofInstant(Instant.ofEpochMilli(fromMillis), zoneId)
            .plusDays(1)
            .withHour(23)
            .withMinute(45)
            .withSecond(0)
            .withNano(0)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()

        while (candidate <= latest && suggestions.size < limit) {
            suggestions += candidate
            candidate = nextSafeSprayTimeMillis(hives, candidate + 30L * 60L * 1000L) ?: break
            if (suggestions.lastOrNull() == candidate) {
                break
            }
        }
        return suggestions.distinct()
    }

    private fun activeWindowForDate(hive: Hive, date: LocalDate): Pair<LocalDateTime, LocalDateTime> {
        val start = LocalTime.parse(sanitizeTime(hive.activeStartTime, "08:00"))
        val end = LocalTime.parse(sanitizeTime(hive.activeEndTime, "18:00"))
        val startDateTime = LocalDateTime.of(date, start)
        val endDateTime = if (end <= start) LocalDateTime.of(date.plusDays(1), end) else LocalDateTime.of(date, end)
        return startDateTime to endDateTime
    }
}
