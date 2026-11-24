package com.miruna.habittracker

import com.miruna.habittracker.data.HabitRepository
import com.miruna.habittracker.model.GroceryItem
import com.miruna.habittracker.model.Habit
import com.miruna.habittracker.model.HabitCategory
import com.miruna.habittracker.model.HabitFrequency
import com.miruna.habittracker.ui.HabitViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class HabitViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeHabitRepository
    private lateinit var viewModel: HabitViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val initialHabits = listOf(
            Habit(id = "1", title = "Gym Workout", category = HabitCategory.GYM),
            Habit(id = "2", title = "Clean House", category = HabitCategory.HOUSEHOLD),
            Habit(id = "3", title = "Study Math", category = HabitCategory.STUDYING, archived = true)
        )
        val initialGrocery = listOf(
            GroceryItem(id = "g1", name = "Milk", isChecked = false),
            GroceryItem(id = "g2", name = "Eggs", isChecked = true)
        )
        fakeRepository = FakeHabitRepository(initialHabits, initialGrocery)
        viewModel = HabitViewModel(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun saveHabit_coercesPastStartDateToToday() = runTest {
        backgroundScope.launch { viewModel.uiState.collect() }
        testScheduler.advanceUntilIdle()

        val pastDate = "2020-01-01"
        viewModel.openAddHabitDialog()
        viewModel.saveHabit(
            title = "Future Habit",
            description = "No past start date allowed",
            category = HabitCategory.GYM,
            frequency = HabitFrequency.DAILY,
            targetDays = 5,
            colorHex = HabitCategory.GYM.colorHex,
            startDate = pastDate,
            endDate = "2026-12-24",
            recurringDays = setOf(1, 2, 3, 4, 5)
        )
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        val habit = state.habits.first { it.title == "Future Habit" }
        val todayStr = LocalDate.now().toString()
        assertEquals(todayStr, habit.startDate)
    }
}

class FakeHabitRepository(
    initialHabits: List<Habit> = emptyList(),
    initialGrocery: List<GroceryItem> = emptyList()
) : HabitRepository {
    private val _habits = MutableStateFlow(initialHabits)
    private val _grocery = MutableStateFlow(initialGrocery)

    override fun getHabits(): Flow<List<Habit>> = _habits.asStateFlow()

    override fun addHabit(habit: Habit) {
        _habits.value = listOf(habit) + _habits.value
    }

    override fun updateHabit(habit: Habit) {
        _habits.value = _habits.value.map { if (it.id == habit.id) habit else it }
    }

    override fun deleteHabit(habitId: String) {
        _habits.value = _habits.value.filter { it.id != habitId }
    }

    override fun toggleHabitCompletion(habitId: String, date: LocalDate) {
        _habits.value = _habits.value.map { habit ->
            if (habit.id == habitId) {
                val dates = habit.completedDates.toMutableSet()
                if (dates.contains(date.toString())) {
                    dates.remove(date.toString())
                } else {
                    dates.add(date.toString())
                }
                habit.copy(completedDates = dates)
            } else habit
        }
    }

    override fun toggleArchiveHabit(habitId: String) {
        _habits.value = _habits.value.map { habit ->
            if (habit.id == habitId) {
                habit.copy(archived = !habit.archived)
            } else habit
        }
    }

    override fun resetToSampleData() {
        _habits.value = emptyList()
        _grocery.value = emptyList()
    }

    override fun getGroceryItems(): Flow<List<GroceryItem>> = _grocery.asStateFlow()

    override fun addGroceryItem(name: String) {
        if (name.isBlank()) return
        val newItem = GroceryItem(name = name.trim())
        _grocery.value = listOf(newItem) + _grocery.value
    }

    override fun toggleGroceryItem(id: String) {
        _grocery.value = _grocery.value.map { item ->
            if (item.id == id) item.copy(isChecked = !item.isChecked) else item
        }
    }

    override fun deleteGroceryItem(id: String) {
        _grocery.value = _grocery.value.filter { it.id != id }
    }

    override fun clearCheckedGroceryItems() {
        _grocery.value = _grocery.value.filter { !it.isChecked }
    }
}
