package com.miruna.habittracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.miruna.habittracker.data.LocalHabitRepositoryImpl
import com.miruna.habittracker.ui.AddEditHabitDialog
import com.miruna.habittracker.ui.GroceryScreen
import com.miruna.habittracker.ui.HabitDetailDialog
import com.miruna.habittracker.ui.HabitTab
import com.miruna.habittracker.ui.HabitTrackerUiState
import com.miruna.habittracker.ui.HabitViewModel
import com.miruna.habittracker.ui.HabitsScreen
import com.miruna.habittracker.ui.StatsScreen
import com.miruna.habittracker.ui.TodayScreen
import com.miruna.habittracker.ui.theme.HabitTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // lets the app draw behind the status bar
        setContent {
            HabitTrackerTheme {
                HabitTrackerApp()
            }
        }
    }
}

// the 4 tabs in the bottom bar (name, icon, and which tab it opens)
enum class HabitTrackerDestination(
    val label: String,
    val icon: ImageVector,
    val tab: HabitTab
) {
    TODAY("Today", Icons.Default.CheckCircle, HabitTab.TODAY),
    HABITS("Habits", Icons.AutoMirrored.Filled.ListAlt, HabitTab.ALL_HABITS),
    GROCERY("Grocery", Icons.Default.ShoppingCart, HabitTab.GROCERY),
    STATS("Stats", Icons.Default.BarChart, HabitTab.STATS)
}

@Composable
fun HabitTrackerApp() {
    // set up the repo + viewmodel once, then watch the ui state
    val context = LocalContext.current
    val repository = remember(context) { LocalHabitRepositoryImpl(context) }
    val viewModel: HabitViewModel = viewModel(factory = HabitViewModel.Factory(repository))
    val uiState by viewModel.uiState.collectAsState()

    HabitTrackerMainContent(
        uiState = uiState,
        viewModel = viewModel
    )
}

@Composable
fun HabitTrackerMainContent(
    uiState: HabitTrackerUiState,
    viewModel: HabitViewModel
) {
    // navigation bar with one item per tab
    NavigationSuiteScaffold(
        navigationSuiteItems = {
            HabitTrackerDestination.entries.forEach { destination ->
                item(
                    icon = {
                        Icon(
                            imageVector = destination.icon,
                            contentDescription = destination.label
                        )
                    },
                    label = { Text(destination.label) },
                    selected = uiState.activeTab == destination.tab,
                    onClick = { viewModel.setActiveTab(destination.tab) }
                )
            }
        }
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            val modifier = Modifier.padding(innerPadding)

            // show the screen for whichever tab is selected
            when (uiState.activeTab) {
                HabitTab.TODAY -> {
                    TodayScreen(
                        uiState = uiState,
                        onSelectDate = viewModel::selectDate,
                        onSelectCategory = viewModel::setCategoryFilter,
                        onToggleCompletion = viewModel::toggleHabitCompletion,
                        onHabitClick = viewModel::openHabitDetail,
                        onEditHabit = viewModel::openEditHabitDialog,
                        onToggleArchive = viewModel::toggleArchiveHabit,
                        onDeleteHabit = viewModel::deleteHabit,
                        onAddHabitClick = viewModel::openAddHabitDialog,
                        onResetSampleData = viewModel::resetToSampleData,
                        modifier = modifier
                    )
                }

                HabitTab.ALL_HABITS -> {
                    HabitsScreen(
                        uiState = uiState,
                        onSearchQueryChange = viewModel::setSearchQuery,
                        onSelectCategory = viewModel::setCategoryFilter,
                        onToggleShowArchived = viewModel::setShowArchivedOnly,
                        onToggleCompletion = viewModel::toggleHabitCompletion,
                        onHabitClick = viewModel::openHabitDetail,
                        onEditHabit = viewModel::openEditHabitDialog,
                        onToggleArchive = viewModel::toggleArchiveHabit,
                        onDeleteHabit = viewModel::deleteHabit,
                        onAddHabitClick = viewModel::openAddHabitDialog,
                        modifier = modifier
                    )
                }

                HabitTab.GROCERY -> {
                    GroceryScreen(
                        items = uiState.groceryItems,
                        onAddItem = viewModel::addGroceryItem,
                        onToggleItem = viewModel::toggleGroceryItem,
                        onDeleteItem = viewModel::deleteGroceryItem,
                        onClearChecked = viewModel::clearCheckedGroceryItems,
                        modifier = modifier
                    )
                }

                HabitTab.STATS -> {
                    StatsScreen(
                        uiState = uiState,
                        modifier = modifier
                    )
                }
            }

            // popup for adding a new habit or editing one
            if (uiState.showAddDialog) {
                AddEditHabitDialog(
                    habitToEdit = uiState.habitToEdit,
                    onDismiss = viewModel::closeAddEditDialog,
                    onSave = viewModel::saveHabit
                )
            }

            // popup with the details when you tap on a habit
            uiState.selectedHabitDetails?.let { habit ->
                HabitDetailDialog(
                    habit = habit,
                    selectedDate = uiState.selectedDate,
                    onDismiss = viewModel::closeHabitDetail
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HabitTrackerAppPreview() {
    HabitTrackerTheme {
        // TODO: add something to preview here
    }
}