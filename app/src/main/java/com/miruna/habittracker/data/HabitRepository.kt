package com.miruna.habittracker.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.miruna.habittracker.model.GroceryItem
import com.miruna.habittracker.model.Habit
import com.miruna.habittracker.model.HabitCategory
import com.miruna.habittracker.model.HabitFrequency
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

interface HabitRepository {
    fun getHabits(): Flow<List<Habit>>
    fun addHabit(habit: Habit)
    fun updateHabit(habit: Habit)
    fun deleteHabit(habitId: String)
    fun toggleHabitCompletion(habitId: String, date: LocalDate)
    fun toggleArchiveHabit(habitId: String)
    fun resetToSampleData()

    // Grocery Checklist
    fun getGroceryItems(): Flow<List<GroceryItem>>
    fun addGroceryItem(name: String)
    fun toggleGroceryItem(id: String)
    fun deleteGroceryItem(id: String)
    fun clearCheckedGroceryItems()
}

class LocalHabitRepositoryImpl(context: Context) : HabitRepository {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _habitsState = MutableStateFlow<List<Habit>>(emptyList())
    private val _groceryState = MutableStateFlow<List<GroceryItem>>(emptyList())

    init {
        // Load Habits
        val savedHabitsJson = prefs.getString(KEY_HABITS, null)
        if (savedHabitsJson.isNullOrBlank()) {
            val defaultHabits = createSampleHabits()
            saveHabitsToPrefs(defaultHabits)
            _habitsState.value = defaultHabits
        } else {
            _habitsState.value = parseHabitsJson(savedHabitsJson)
        }

        // Load Grocery Items
        val savedGroceryJson = prefs.getString(KEY_GROCERY, null)
        if (savedGroceryJson.isNullOrBlank()) {
            val defaultGrocery = createSampleGroceryItems()
            saveGroceryToPrefs(defaultGrocery)
            _groceryState.value = defaultGrocery
        } else {
            _groceryState.value = parseGroceryJson(savedGroceryJson)
        }
    }

    override fun getHabits(): Flow<List<Habit>> = _habitsState.asStateFlow()

    override fun addHabit(habit: Habit) {
        val current = _habitsState.value.toMutableList()
        current.add(0, habit)
        updateAndSaveHabits(current)
    }

    override fun updateHabit(habit: Habit) {
        val current = _habitsState.value.map { existing ->
            if (existing.id == habit.id) habit else existing
        }
        updateAndSaveHabits(current)
    }

    override fun deleteHabit(habitId: String) {
        val current = _habitsState.value.filter { it.id != habitId }
        updateAndSaveHabits(current)
    }

    override fun toggleHabitCompletion(habitId: String, date: LocalDate) {
        val dateString = date.toString()
        val current = _habitsState.value.map { habit ->
            if (habit.id == habitId) {
                val newCompleted = habit.completedDates.toMutableSet()
                if (newCompleted.contains(dateString)) {
                    newCompleted.remove(dateString)
                } else {
                    newCompleted.add(dateString)
                }
                habit.copy(completedDates = newCompleted)
            } else {
                habit
            }
        }
        updateAndSaveHabits(current)
    }

    override fun toggleArchiveHabit(habitId: String) {
        val current = _habitsState.value.map { habit ->
            if (habit.id == habitId) {
                habit.copy(archived = !habit.archived)
            } else {
                habit
            }
        }
        updateAndSaveHabits(current)
    }

    override fun resetToSampleData() {
        val sampleHabits = createSampleHabits()
        updateAndSaveHabits(sampleHabits)
        val sampleGrocery = createSampleGroceryItems()
        updateAndSaveGrocery(sampleGrocery)
    }

    // Grocery implementation
    override fun getGroceryItems(): Flow<List<GroceryItem>> = _groceryState.asStateFlow()

    override fun addGroceryItem(name: String) {
        if (name.isBlank()) return
        val newItem = GroceryItem(name = name.trim())
        val current = listOf(newItem) + _groceryState.value
        updateAndSaveGrocery(current)
    }

    override fun toggleGroceryItem(id: String) {
        val current = _groceryState.value.map { item ->
            if (item.id == id) item.copy(isChecked = !item.isChecked) else item
        }
        updateAndSaveGrocery(current)
    }

    override fun deleteGroceryItem(id: String) {
        val current = _groceryState.value.filter { it.id != id }
        updateAndSaveGrocery(current)
    }

    override fun clearCheckedGroceryItems() {
        val current = _groceryState.value.filter { !it.isChecked }
        updateAndSaveGrocery(current)
    }

    private fun updateAndSaveHabits(habits: List<Habit>) {
        _habitsState.value = habits
        saveHabitsToPrefs(habits)
    }

    private fun updateAndSaveGrocery(items: List<GroceryItem>) {
        _groceryState.value = items
        saveGroceryToPrefs(items)
    }

    private fun saveHabitsToPrefs(habits: List<Habit>) {
        val jsonArray = JSONArray()
        for (habit in habits) {
            val obj = JSONObject().apply {
                put("id", habit.id)
                put("title", habit.title)
                put("description", habit.description)
                put("category", habit.category.name)
                put("frequency", habit.frequency.name)
                put("targetDaysPerWeek", habit.targetDaysPerWeek)
                put("colorHex", habit.colorHex)
                put("startDate", habit.startDate)
                put("endDate", habit.endDate ?: "")
                put("createdAt", habit.createdAt)
                put("archived", habit.archived)

                val recArr = JSONArray()
                for (day in habit.recurringDays) {
                    recArr.put(day)
                }
                put("recurringDays", recArr)

                val datesArr = JSONArray()
                for (d in habit.completedDates) {
                    datesArr.put(d)
                }
                put("completedDates", datesArr)
            }
            jsonArray.put(obj)
        }
        prefs.edit { putString(KEY_HABITS, jsonArray.toString()) }
    }

    private fun parseHabitsJson(jsonString: String): List<Habit> {
        val list = mutableListOf<Habit>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val completedSet = mutableSetOf<String>()
                val datesArr = obj.optJSONArray("completedDates")
                if (datesArr != null) {
                    for (j in 0 until datesArr.length()) {
                        completedSet.add(datesArr.getString(j))
                    }
                }

                val recSet = mutableSetOf<Int>()
                val recArr = obj.optJSONArray("recurringDays")
                if (recArr != null && recArr.length() > 0) {
                    for (k in 0 until recArr.length()) {
                        recSet.add(recArr.getInt(k))
                    }
                } else {
                    recSet.addAll(listOf(1, 2, 3, 4, 5, 6, 7))
                }

                val habit = Habit(
                    id = obj.getString("id"),
                    title = obj.getString("title"),
                    description = obj.optString("description", ""),
                    category = HabitCategory.fromName(obj.optString("category", HabitCategory.GYM.name)),
                    frequency = HabitFrequency.fromName(obj.optString("frequency", HabitFrequency.DAILY.name)),
                    targetDaysPerWeek = obj.optInt("targetDaysPerWeek", 7),
                    colorHex = obj.optLong("colorHex", HabitCategory.GYM.colorHex),
                    startDate = obj.optString("startDate", LocalDate.now().toString()),
                    endDate = obj.optString("endDate", "").ifBlank { null },
                    recurringDays = recSet,
                    createdAt = obj.optString("createdAt", LocalDate.now().toString()),
                    completedDates = completedSet,
                    archived = obj.optBoolean("archived", false)
                )
                list.add(habit)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun saveGroceryToPrefs(items: List<GroceryItem>) {
        val jsonArray = JSONArray()
        for (item in items) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("name", item.name)
                put("isChecked", item.isChecked)
                put("createdAt", item.createdAt)
            }
            jsonArray.put(obj)
        }
        prefs.edit { putString(KEY_GROCERY, jsonArray.toString()) }
    }

    private fun parseGroceryJson(jsonString: String): List<GroceryItem> {
        val list = mutableListOf<GroceryItem>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val item = GroceryItem(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    isChecked = obj.optBoolean("isChecked", false),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                )
                list.add(item)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun createSampleHabits(): List<Habit> {
        val today = LocalDate.now()
        val d0 = today.toString()
        val d1 = today.minusDays(1).toString()
        val d2 = today.minusDays(2).toString()
        val d3 = today.minusDays(3).toString()
        val d4 = today.minusDays(4).toString()
        val dec24 = LocalDate.of(2026, 12, 24).toString()

        return listOf(
            Habit(
                title = "Gym Workout",
                description = "Leg day & 20m cardio",
                category = HabitCategory.GYM,
                frequency = HabitFrequency.DAILY,
                targetDaysPerWeek = 5,
                colorHex = HabitCategory.GYM.colorHex,
                startDate = d4,
                endDate = dec24,
                recurringDays = setOf(1, 2, 3, 4, 5), // Mon..Fri
                completedDates = setOf(d0, d1, d2, d3)
            ),
            Habit(
                title = "Clean Household",
                description = "Tidy living space & kitchen",
                category = HabitCategory.HOUSEHOLD,
                frequency = HabitFrequency.DAILY,
                targetDaysPerWeek = 7,
                colorHex = HabitCategory.HOUSEHOLD.colorHex,
                startDate = d4,
                endDate = dec24,
                recurringDays = setOf(1, 2, 3, 4, 5, 6, 7),
                completedDates = setOf(d0, d1, d2)
            ),
            Habit(
                title = "Study Session",
                description = "Focus for 45 mins",
                category = HabitCategory.STUDYING,
                frequency = HabitFrequency.DAILY,
                targetDaysPerWeek = 6,
                colorHex = HabitCategory.STUDYING.colorHex,
                startDate = d4,
                endDate = dec24,
                recurringDays = setOf(1, 2, 3, 4, 5, 6),
                completedDates = setOf(d1, d2, d3, d4)
            )
        )
    }

    private fun createSampleGroceryItems(): List<GroceryItem> {
        return listOf(
            GroceryItem(name = "Whole Milk 2L", isChecked = false),
            GroceryItem(name = "Organic Eggs (12 pack)", isChecked = false),
            GroceryItem(name = "Fresh Bananas & Apples", isChecked = true),
            GroceryItem(name = "Almond Butter & Oats", isChecked = false)
        )
    }

    companion object {
        private const val PREFS_NAME = "habit_tracker_prefs"
        private const val KEY_HABITS = "key_habits_list"
        private const val KEY_GROCERY = "key_grocery_list"
    }
}
