package com.miruna.habittracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.miruna.habittracker.model.Habit
import com.miruna.habittracker.model.HabitCategory
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TodayScreen(
    uiState: HabitTrackerUiState,
    onSelectDate: (LocalDate) -> Unit,
    onSelectCategory: (HabitCategory?) -> Unit,
    onToggleCompletion: (String) -> Unit,
    onHabitClick: (Habit) -> Unit,
    onEditHabit: (Habit) -> Unit,
    onToggleArchive: (String) -> Unit,
    onDeleteHabit: (String) -> Unit,
    onAddHabitClick: () -> Unit,
    onResetSampleData: () -> Unit,
    modifier: Modifier = Modifier
) {
    val today = remember { LocalDate.now() }

    val dateTitle = remember(uiState.selectedDate, today) {
        if (uiState.selectedDate == today) {
            "Today, " + today.format(DateTimeFormatter.ofPattern("MMM d", Locale.getDefault()))
        } else {
            uiState.selectedDate.format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault()))
        }
    }

    val displayHabits = uiState.filteredHabits

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddHabitClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add"
                )
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            DateStrip(
                selectedDate = uiState.selectedDate,
                onDateSelected = onSelectDate
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Minimalist Daily Progress Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = dateTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "${uiState.todayCompletedCount}/${uiState.todayTotalCount} completed",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }

                        Text(
                            text = "${(uiState.todayProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LinearProgressIndicator(
                        progress = { uiState.todayProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeCap = StrokeCap.Round
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            CategoryFilterRow(
                selectedCategory = uiState.selectedCategory,
                onCategorySelected = onSelectCategory
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (displayHabits.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "No Habits",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(onClick = onAddHabitClick) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Habit")
                        }
                        OutlinedButton(onClick = onResetSampleData) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sample Data")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(displayHabits, key = { it.id }) { habit ->
                        HabitCard(
                            habit = habit,
                            selectedDate = uiState.selectedDate,
                            onToggleCompletion = { onToggleCompletion(habit.id) },
                            onClick = { onHabitClick(habit) },
                            onEdit = { onEditHabit(habit) },
                            onToggleArchive = { onToggleArchive(habit.id) },
                            onDelete = { onDeleteHabit(habit.id) }
                        )
                    }
                }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun TodayScreenPreview() {
    val sampleHabit1 = Habit(
        title = "Gym Workout",
        description = "Leg day & cardio",
        category = HabitCategory.GYM
    )
    val sampleHabit2 = Habit(
        title = "Clean Kitchen",
        description = "Wipe counters",
        category = HabitCategory.HOUSEHOLD
    )
    val state = HabitTrackerUiState(
        habits = listOf(sampleHabit1, sampleHabit2)
    )
    com.miruna.habittracker.ui.theme.HabitTrackerTheme {
        TodayScreen(
            uiState = state,
            onSelectDate = {},
            onSelectCategory = {},
            onToggleCompletion = {},
            onHabitClick = {},
            onEditHabit = {},
            onToggleArchive = {},
            onDeleteHabit = {},
            onAddHabitClick = {},
            onResetSampleData = {}
        )
    }
}
