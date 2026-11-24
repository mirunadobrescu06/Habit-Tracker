package com.miruna.habittracker

import com.miruna.habittracker.model.Habit
import com.miruna.habittracker.model.HabitCategory
import com.miruna.habittracker.model.HabitFrequency
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HabitRepositoryTest {

    // checks that marking a habit as done for today actually works
    @Test
    fun testToggleCompletion() {
        val today = LocalDate.of(2026, 3, 30)

        // habit that was only done yesterday
        val habit = Habit(
            id = "h1",
            title = "Gym Workout",
            category = HabitCategory.GYM,
            frequency = HabitFrequency.DAILY,
            targetDaysPerWeek = 5,
            completedDates = setOf("2026-03-29")
        )

        // shouldn't be done today yet
        assertFalse(habit.isCompletedOn(today))

        // add today to the completed dates (same thing the toggle does)
        val newDates = habit.completedDates.toMutableSet()
        newDates.add(today.toString())
        val updatedHabit = habit.copy(completedDates = newDates)

        // now it should be done today, and have 2 dates total
        assertTrue(updatedHabit.isCompletedOn(today))
        assertEquals(2, updatedHabit.completedDates.size)
    }

    // fromName should ignore upper/lower case and fall back to OTHER
    @Test
    fun testCategoryFromName() {
        assertEquals(HabitCategory.GYM, HabitCategory.fromName("GYM"))
        assertEquals(HabitCategory.HOUSEHOLD, HabitCategory.fromName("household"))
        assertEquals(HabitCategory.OTHER, HabitCategory.fromName("UNKNOWN_CATEGORY"))
    }
}