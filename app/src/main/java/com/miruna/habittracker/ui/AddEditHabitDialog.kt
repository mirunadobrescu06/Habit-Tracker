package com.miruna.habittracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.miruna.habittracker.model.Habit
import com.miruna.habittracker.model.HabitCategory
import com.miruna.habittracker.model.HabitFrequency
import java.time.LocalDate

private val COLOR_OPTIONS = listOf(
    0xFF1E3A8A, // Navy Blue
    0xFF0284C7, // Ocean Blue
    0xFF0D9488, // Teal
    0xFF0284C7, // Sky Blue
    0xFF3B82F6, // Bright Blue
    0xFF1E293B, // Dark Slate
    0xFF4F46E5, // Indigo
    0xFF2563EB  // Cobalt
)

private val DAYS_OF_WEEK = listOf(
    1 to "Mon",
    2 to "Tue",
    3 to "Wed",
    4 to "Thu",
    5 to "Fri",
    6 to "Sat",
    7 to "Sun"
)

@Composable
fun AddEditHabitDialog(
    habitToEdit: Habit?,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        description: String,
        category: HabitCategory,
        frequency: HabitFrequency,
        targetDays: Int,
        colorHex: Long,
        startDate: String,
        endDate: String?,
        recurringDays: Set<Int>
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    val isEditing = habitToEdit != null
    val today = remember { LocalDate.now() }
    val defaultDec24 = remember(today) { LocalDate.of(2026, 12, 24).toString() }

    var title by remember(habitToEdit) { mutableStateOf(habitToEdit?.title ?: "") }
    var description by remember(habitToEdit) { mutableStateOf(habitToEdit?.description ?: "") }
    var category by remember(habitToEdit) { mutableStateOf(habitToEdit?.category ?: HabitCategory.GYM) }
    var frequency by remember(habitToEdit) { mutableStateOf(habitToEdit?.frequency ?: HabitFrequency.DAILY) }
    var targetDays by remember(habitToEdit) { mutableIntStateOf(habitToEdit?.targetDaysPerWeek ?: 7) }
    var selectedColorHex by remember(habitToEdit) {
        mutableLongStateOf(habitToEdit?.colorHex ?: category.colorHex)
    }
    var startDate by remember(habitToEdit) {
        val initialStart = habitToEdit?.startDate ?: today.toString()
        mutableStateOf(if (initialStart < today.toString()) today.toString() else initialStart)
    }
    var endDate by remember(habitToEdit) { mutableStateOf<String?>(habitToEdit?.endDate ?: defaultDec24) }
    var recurringDays by remember(habitToEdit) {
        mutableStateOf(habitToEdit?.recurringDays ?: setOf(1, 2, 3, 4, 5, 6, 7))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEditing) "Edit Habit" else "New Habit",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title *") },
                    placeholder = { Text("e.g. Gym Workout") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Notes (Optional)") },
                    maxLines = 2,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Column {
                    Text(
                        text = "Category",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(HabitCategory.entries.toTypedArray()) { cat ->
                            val isSelected = category == cat
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    category = cat
                                    selectedColorHex = cat.colorHex
                                },
                                label = { Text(cat.displayName) },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color(cat.colorHex))
                                    )
                                }
                            )
                        }
                    }
                }

                Column {
                    Text(
                        text = "Recurring Days",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        DAYS_OF_WEEK.forEach { (dayNum, label) ->
                            val isSelected = recurringDays.contains(dayNum)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable {
                                        val newSet = recurringDays.toMutableSet()
                                        if (isSelected && newSet.size > 1) {
                                            newSet.remove(dayNum)
                                        } else {
                                            newSet.add(dayNum)
                                        }
                                        recurringDays = newSet
                                        targetDays = newSet.size
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Column {
                    Text(
                        text = "Schedule Duration",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = endDate == defaultDec24,
                            onClick = { endDate = defaultDec24 },
                            label = { Text("Until Dec 24") }
                        )
                        FilterChip(
                            selected = endDate == today.plusMonths(1).toString(),
                            onClick = { endDate = today.plusMonths(1).toString() },
                            label = { Text("1 Month") }
                        )
                        FilterChip(
                            selected = endDate == null,
                            onClick = { endDate = null },
                            label = { Text("Ongoing") }
                        )
                    }
                }

                Column {
                    Text(
                        text = "Color",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        COLOR_OPTIONS.forEach { colorHex ->
                            val isSelected = selectedColorHex == colorHex
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color(colorHex))
                                    .border(
                                        width = if (isSelected) 2.5.dp else 0.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColorHex = colorHex }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(
                            title,
                            description,
                            category,
                            frequency,
                            targetDays,
                            selectedColorHex,
                            startDate,
                            endDate,
                            recurringDays
                        )
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text(if (isEditing) "Save" else "Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        modifier = modifier
    )
}
