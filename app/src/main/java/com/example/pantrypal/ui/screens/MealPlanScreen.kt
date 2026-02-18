package com.example.pantrypal.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.pantrypal.R
import com.example.pantrypal.viewmodel.MainViewModel
import com.example.pantrypal.data.entity.MealEntity

@Composable
fun MealPlanScreen(viewModel: MainViewModel) {
    val currentWeekSetting by viewModel.currentWeek.collectAsState()
    val meals by viewModel.mealsState.collectAsState()

    var selectedTab by remember { mutableIntStateOf(if (currentWeekSetting == "A") 0 else 1) }

    // Sync tab with current week setting
    LaunchedEffect(currentWeekSetting) {
        selectedTab = if (currentWeekSetting == "A") 0 else 1
    }

    val displayWeek = if (selectedTab == 0) "A" else "B"

    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Meal")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Week A") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Week B") }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Current Week Indicator / Setter
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (currentWeekSetting == displayWeek) stringResource(R.string.this_is_current_week) else stringResource(R.string.not_current_week),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (currentWeekSetting == displayWeek) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                if (currentWeekSetting != displayWeek) {
                    Button(onClick = { viewModel.setCurrentWeek(displayWeek) }) {
                        Text(stringResource(R.string.set_as_current))
                    }
                } else {
                    Icon(Icons.Default.Check, contentDescription = "Current", tint = MaterialTheme.colorScheme.primary)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            val weekMeals = remember(meals, displayWeek) {
                meals.filter { it.week == displayWeek }
            }

            if (weekMeals.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No meals for Week $displayWeek")
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(weekMeals) { meal ->
                        MealItemRow(meal, onDelete = { viewModel.deleteMeal(meal) })
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddMealDialog(
            week = displayWeek,
            onDismiss = { showAddDialog = false },
            onAdd = { name, ingredients ->
                viewModel.addMeal(name, displayWeek, ingredients)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun MealItemRow(meal: MealEntity, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = meal.name, style = MaterialTheme.typography.titleMedium)
                if (meal.ingredients.isNotEmpty()) {
                    Text(
                        text = "Ingredients: ${meal.ingredients.joinToString()}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun AddMealDialog(week: String, onDismiss: () -> Unit, onAdd: (String, List<String>) -> Unit) {
    var name by remember { mutableStateOf("") }
    var ingredientsText by remember { mutableStateOf("") } // Comma separated for simplicity

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Meal for Week $week") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Meal Name") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = ingredientsText,
                    onValueChange = { ingredientsText = it },
                    label = { Text("Ingredients (comma separated)") },
                    minLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank()) {
                    val ingredients = ingredientsText.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    onAdd(name, ingredients)
                }
            }) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
