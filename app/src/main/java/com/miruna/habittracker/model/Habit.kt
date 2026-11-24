package com.miruna.habittracker.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.UUID

enum class HabitCategory(
    val displayName: String,
    val colorHex: Long
) {
    GYM("Gym", 0xFF1E3A8A),
    HOUSEHOLD("Household", 0xFF0284C7),
    STUDYING("Studying", 0xFF0D9488),
    OTHER("Other", 0xFF475569);

    companion object {
        fun fromName(name: String): HabitCategory {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: OTHER
        }
    }
}

enum class HabitFrequency(val displayName: String) {
    DAILY("Daily"),
    WEEKLY("Weekly");

    companion object {
        fun fromName(name: String): HabitFrequency {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: DAILY
        }
    }
}

data class Habit(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val category: HabitCategory = HabitCategory.GYM,
    val frequency: HabitFrequency = HabitFrequency.DAILY,
    val targetDaysPerWeek: Int = 7,
    val colorHex: Long = category.colorHex,
    val startDate: String = LocalDate.now().toString(),
    val endDate: String? = null,
    val recurringDays: Set<Int> = setOf(1, 2, 3, 4, 5, 6, 7), // 1=Mon .. 7=Sun
    val createdAt: String = LocalDate.now().toString(),
    val completedDates: Set<String> = emptySet(),
    val archived: Boolean = false
) {
    fun isScheduledOn(date: LocalDate): Boolean {
        val start = try { LocalDate.parse(startDate) } catch (_: Exception) { LocalDate.MIN }
        if (date.isBefore(start)) return false

        if (!endDate.isNullOrBlank()) {
            val end = try { LocalDate.parse(endDate) } catch (_: Exception) { LocalDate.MAX }
            if (date.isAfter(end)) return false
        }

        if (recurringDays.isNotEmpty() && !recurringDays.contains(date.dayOfWeek.value)) {
            return false
        }

        return true
    }

    fun recurringDaysSummary(): String {
        if (recurringDays.size == 7) return "Daily"
        if (recurringDays == setOf(1, 2, 3, 4, 5)) return "Weekdays"
        if (recurringDays == setOf(6, 7)) return "Weekends"

        return recurringDays.sorted().joinToString(", ") { dayNum ->
            DayOfWeek.of(dayNum).getDisplayName(TextStyle.SHORT, Locale.getDefault())
        }
    }

    fun isCompletedOn(date: LocalDate): Boolean {
        return completedDates.contains(date.toString())
    }

    fun currentStreak(today: LocalDate = LocalDate.now()): Int {
        if (completedDates.isEmpty()) return 0

        var dateToCheck = today
        if (!isCompletedOn(today)) {
            val yesterday = today.minusDays(1)
            if (!isCompletedOn(yesterday)) {
                return 0
            }
            dateToCheck = yesterday
        }

        var streak = 0
        while (isCompletedOn(dateToCheck)) {
            streak++
            dateToCheck = dateToCheck.minusDays(1)
        }
        return streak
    }

    fun bestStreak(): Int {
        if (completedDates.isEmpty()) return 0

        val sortedDates = completedDates
            .mapNotNull {
                try {
                    LocalDate.parse(it)
                } catch (_: Exception) {
                    null
                }
            }
            .sorted()

        if (sortedDates.isEmpty()) return 0

        var maxStreak = 1
        var currentStreakCount = 1

        for (i in 1 until sortedDates.size) {
            val daysBetween = ChronoUnit.DAYS.between(sortedDates[i - 1], sortedDates[i])
            if (daysBetween == 1L) {
                currentStreakCount++
                if (currentStreakCount > maxStreak) {
                    maxStreak = currentStreakCount
                }
            } else if (daysBetween > 1L) {
                currentStreakCount = 1
            }
        }

        return maxStreak
    }

    fun weeklyCompletions(currentDate: LocalDate = LocalDate.now()): Int {
        val startDate = currentDate.minusDays(6)
        return (0..6).count { offset ->
            isCompletedOn(startDate.plusDays(offset.toLong()))
        }
    }

    fun completionRateLast30Days(currentDate: LocalDate = LocalDate.now()): Int {
        val startDate = currentDate.minusDays(29)
        val completedCount = (0..29).count { offset ->
            isCompletedOn(startDate.plusDays(offset.toLong()))
        }
        val targetIn30Days = (targetDaysPerWeek * (30.0 / 7.0)).coerceAtLeast(1.0)
        val percentage = ((completedCount / targetIn30Days) * 100).toInt()
        return percentage.coerceIn(0, 100)
    }
}
