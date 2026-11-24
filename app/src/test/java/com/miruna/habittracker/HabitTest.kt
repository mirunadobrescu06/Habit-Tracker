package com.miruna.habittracker

import com.miruna.habittracker.model.Habit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HabitTest {

    private val today = LocalDate.of(2026, 3, 30) // Monday (Day 1)

    @Test
    fun isScheduledOn_respectsStartDateEndDateAndRecurringDays() {
        // Scheduled on Mon(1), Wed(3), Fri(5)
        val habit = Habit(
            title = "Gym Mon Wed Fri",
            startDate = "2026-03-25",
            endDate = "2026-12-24",
            recurringDays = setOf(1, 3, 5)
        )

        val mon = LocalDate.of(2026, 3, 30) // Mon (1) -> True
        val tue = LocalDate.of(2026, 3, 31) // Tue (2) -> False
        val wed = LocalDate.of(2026, 4, 1)  // Wed (3) -> True

        assertTrue(habit.isScheduledOn(mon))
        assertFalse(habit.isScheduledOn(tue))
        assertTrue(habit.isScheduledOn(wed))
    }

    @Test
    fun recurringDaysSummary_returnsFormattedLabels() {
        val daily = Habit(title = "H1", recurringDays = setOf(1, 2, 3, 4, 5, 6, 7))
        val weekdays = Habit(title = "H2", recurringDays = setOf(1, 2, 3, 4, 5))
        val weekends = Habit(title = "H3", recurringDays = setOf(6, 7))

        assertEquals("Daily", daily.recurringDaysSummary())
        assertEquals("Weekdays", weekdays.recurringDaysSummary())
        assertEquals("Weekends", weekends.recurringDaysSummary())
    }

    @Test
    fun isCompletedOn_returnsTrueForCompletedDate() {
        val habit = Habit(
            title = "Water",
            completedDates = setOf("2026-03-30", "2026-03-29")
        )

        assertTrue(habit.isCompletedOn(today))
        assertTrue(habit.isCompletedOn(today.minusDays(1)))
        assertFalse(habit.isCompletedOn(today.minusDays(2)))
    }
}
