package com.miruna.habittracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.miruna.habittracker.data.HabitRepository
import com.miruna.habittracker.model.GroceryItem
import com.miruna.habittracker.model.Habit
import com.miruna.habittracker.model.HabitCategory
import com.miruna.habittracker.model.HabitFrequency
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class HabitTab(val title: String) {
    TODAY("Today"),
    ALL_HABITS("Habits"),
    GROCERY("Grocery"),
    STATS("Stats")
}

data class HabitTrackerUiState(
    val habits: List<Habit> = emptyList(),
    val groceryItems: List<GroceryItem> = emptyList(),
    val selectedDate: LocalDate = LocalDate.now(),
    val selectedCategory: HabitCategory? = null,
    val searchQuery: String = "",
    val activeTab: HabitTab = HabitTab.TODAY,
    val showAddDialog: Boolean = false,
    val habitToEdit: Habit? = null,
    val selectedHabitDetails: Habit? = null,
    val showArchivedOnly: Boolean = false
) {
    val activeHabits: List<Habit>
        get() = habits.filter { !it.archived }

    val archivedHabits: List<Habit>
        get() = habits.filter { it.archived }

    val todayScheduledHabits: List<Habit>
        get() = activeHabits.filter { it.isScheduledOn(selectedDate) }

    val filteredHabits: List<Habit>
        get() {
            val source = if (activeTab == HabitTab.TODAY) todayScheduledHabits
            else if (showArchivedOnly) archivedHabits else activeHabits

            return source.filter { habit ->
                val matchesCategory = selectedCategory == null || habit.category == selectedCategory
                val matchesSearch = searchQuery.isBlank() ||
                        habit.title.contains(searchQuery, ignoreCase = true) ||
                        habit.description.contains(searchQuery, ignoreCase = true)
                matchesCategory && matchesSearch
            }
        }

    val todayCompletedCount: Int
        get() = todayScheduledHabits.count { it.isCompletedOn(selectedDate) }

    val todayTotalCount: Int
        get() = todayScheduledHabits.size

    val todayProgress: Float
        get() = if (todayTotalCount > 0) todayCompletedCount.toFloat() / todayTotalCount else 0f
}

class HabitViewModel(
    private val repository: HabitRepository
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    private val _selectedCategory = MutableStateFlow<HabitCategory?>(null)
    private val _searchQuery = MutableStateFlow("")
    private val _activeTab = MutableStateFlow(HabitTab.TODAY)
    private val _showAddDialog = MutableStateFlow(false)
    private val _habitToEdit = MutableStateFlow<Habit?>(null)
    private val _selectedHabitDetails = MutableStateFlow<Habit?>(null)
    private val _showArchivedOnly = MutableStateFlow(false)

    private val _filterState = combine(
        _selectedDate,
        _selectedCategory,
        _searchQuery,
        _activeTab
    ) { date, category, search, tab ->
        FilterState(date, category, search, tab)
    }

    private val _dialogState = combine(
        _showAddDialog,
        _habitToEdit,
        _selectedHabitDetails,
        _showArchivedOnly
    ) { showAdd, toEdit, details, showArchived ->
        DialogState(showAdd, toEdit, details, showArchived)
    }

    val uiState: StateFlow<HabitTrackerUiState> = combine(
        repository.getHabits(),
        repository.getGroceryItems(),
        _filterState,
        _dialogState
    ) { habits, grocery, filter, dialog ->
        HabitTrackerUiState(
            habits = habits,
            groceryItems = grocery,
            selectedDate = filter.date,
            selectedCategory = filter.category,
            searchQuery = filter.search,
            activeTab = filter.tab,
            showAddDialog = dialog.showAdd,
            habitToEdit = dialog.toEdit,
            selectedHabitDetails = dialog.details,
            showArchivedOnly = dialog.showArchived
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HabitTrackerUiState()
    )

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun setCategoryFilter(category: HabitCategory?) {
        _selectedCategory.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setActiveTab(tab: HabitTab) {
        _activeTab.value = tab
    }

    fun setShowArchivedOnly(show: Boolean) {
        _showArchivedOnly.value = show
    }

    fun toggleHabitCompletion(habitId: String, date: LocalDate = _selectedDate.value) {
        viewModelScope.launch {
            repository.toggleHabitCompletion(habitId, date)
        }
    }

    fun toggleArchiveHabit(habitId: String) {
        viewModelScope.launch {
            repository.toggleArchiveHabit(habitId)
        }
    }

    fun deleteHabit(habitId: String) {
        viewModelScope.launch {
            repository.deleteHabit(habitId)
        }
    }

    fun openAddHabitDialog() {
        _habitToEdit.value = null
        _showAddDialog.value = true
    }

    fun openEditHabitDialog(habit: Habit) {
        _habitToEdit.value = habit
        _showAddDialog.value = true
    }

    fun closeAddEditDialog() {
        _showAddDialog.value = false
        _habitToEdit.value = null
    }

    fun saveHabit(
        title: String,
        description: String,
        category: HabitCategory,
        frequency: HabitFrequency,
        targetDays: Int,
        colorHex: Long,
        startDate: String,
        endDate: String?,
        recurringDays: Set<Int>
    ) {
        val todayStr = LocalDate.now().toString()
        val validStartDate = if (startDate < todayStr) todayStr else startDate

        val habitToEdit = _habitToEdit.value
        if (habitToEdit != null) {
            val updated = habitToEdit.copy(
                title = title.trim(),
                description = description.trim(),
                category = category,
                frequency = frequency,
                targetDaysPerWeek = targetDays,
                colorHex = colorHex,
                startDate = validStartDate,
                endDate = endDate,
                recurringDays = recurringDays
            )
            viewModelScope.launch {
                repository.updateHabit(updated)
            }
        } else {
            val newHabit = Habit(
                title = title.trim(),
                description = description.trim(),
                category = category,
                frequency = frequency,
                targetDaysPerWeek = targetDays,
                colorHex = colorHex,
                startDate = validStartDate,
                endDate = endDate,
                recurringDays = recurringDays
            )
            viewModelScope.launch {
                repository.addHabit(newHabit)
            }
        }
        closeAddEditDialog()
    }

    fun openHabitDetail(habit: Habit) {
        _selectedHabitDetails.value = habit
    }

    fun closeHabitDetail() {
        _selectedHabitDetails.value = null
    }

    // Grocery actions
    fun addGroceryItem(name: String) {
        viewModelScope.launch {
            repository.addGroceryItem(name)
        }
    }

    fun toggleGroceryItem(id: String) {
        viewModelScope.launch {
            repository.toggleGroceryItem(id)
        }
    }

    fun deleteGroceryItem(id: String) {
        viewModelScope.launch {
            repository.deleteGroceryItem(id)
        }
    }

    fun clearCheckedGroceryItems() {
        viewModelScope.launch {
            repository.clearCheckedGroceryItems()
        }
    }

    fun resetToSampleData() {
        viewModelScope.launch {
            repository.resetToSampleData()
        }
    }

    private data class FilterState(
        val date: LocalDate,
        val category: HabitCategory?,
        val search: String,
        val tab: HabitTab
    )

    private data class DialogState(
        val showAdd: Boolean,
        val toEdit: Habit?,
        val details: Habit?,
        val showArchived: Boolean
    )

    class Factory(private val repository: HabitRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HabitViewModel::class.java)) {
                return HabitViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
