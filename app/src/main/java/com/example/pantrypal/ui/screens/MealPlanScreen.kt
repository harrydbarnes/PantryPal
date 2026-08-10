package com.example.pantrypal.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.pantrypal.data.entity.MealEntity
import com.example.pantrypal.data.entity.MealWeekEntity
import com.example.pantrypal.ui.components.ExpressiveHero
import com.example.pantrypal.ui.components.SectionHeading
import com.example.pantrypal.ui.components.StatusPill
import com.example.pantrypal.util.dayLabel
import com.example.pantrypal.util.MealIngredientSelection
import com.example.pantrypal.util.nextWeek
import com.example.pantrypal.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MealPlanScreen(
    viewModel: MainViewModel,
    onOpenRecipes: () -> Unit = {}
) {
    val currentWeek by viewModel.currentWeek.collectAsState()
    val meals by viewModel.mealsState.collectAsState()
    val weeks by viewModel.mealWeeksState.collectAsState()
    val inventory by viewModel.inventoryState.collectAsState()
    var displayedWeek by remember(currentWeek) { mutableStateOf(currentWeek) }
    var editingMeal by remember { mutableStateOf<MealEntity?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var showCopyWeekDialog by remember { mutableStateOf(false) }
    var editingWeek by remember { mutableStateOf<MealWeekEntity?>(null) }
    var copyingMeal by remember { mutableStateOf<MealEntity?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val weekMeals = remember(meals, displayedWeek) {
        meals.filter { it.week == displayedWeek }
            .sortedWith(compareBy<MealEntity> { it.dayOfWeek }.thenBy { MealEntity.SLOTS.indexOf(it.mealSlot) })
    }
    val mealsByDay = remember(weekMeals) {
        weekMeals.groupBy(MealEntity::dayOfWeek)
    }
    val hasIngredients = remember(weekMeals) {
        weekMeals.any { it.ingredients.isNotEmpty() }
    }
    val displayedWeekDetails = weeks.firstOrNull { it.weekId == displayedWeek }
    val weekOrder = weeks.map { it.weekId }
    val ingredientSuggestions = remember(meals, inventory) {
        (inventory.map { it.name } + meals.flatMap { it.ingredients })
            .map(String::trim)
            .filter(String::isNotEmpty)
            .distinctBy { it.lowercase() }
            .sortedBy { it.lowercase() }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    editingMeal = null
                    showEditor = true
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add meal") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ExpressiveHero(
                    eyebrow = "Four-week rhythm",
                    title = "Dinner plans, minus the daily scramble",
                    supportingText = "Shape each week once, reuse the good bits, and turn ingredients into a tidy shop.",
                    icon = Icons.Default.Restaurant,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }

            item {
                FilledTonalButton(
                    onClick = onOpenRecipes,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.MenuBook, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Browse recipes and cupboard ideas")
                }
            }

            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusPill(
                        label = "${weekMeals.size} meal${if (weekMeals.size == 1) "" else "s"} planned",
                        icon = Icons.Default.CalendarMonth
                    )
                    StatusPill(
                        label = if (displayedWeek == currentWeek) "Current rotation" else "Template preview",
                        icon = if (displayedWeek == currentWeek) Icons.Default.AutoAwesome else Icons.Default.ContentCopy,
                        containerColor = if (displayedWeek == currentWeek) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        },
                        contentColor = if (displayedWeek == currentWeek) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            item {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    weeks.forEach { week ->
                        FilterChip(
                            selected = displayedWeek == week.weekId,
                            onClick = { displayedWeek = week.weekId },
                            label = { Text(week.displayName) },
                            leadingIcon = if (displayedWeek == week.weekId) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                            } else null
                        )
                    }
                }
            }

            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = if (displayedWeek == currentWeek) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    if (displayedWeek == currentWeek) {
                                        "This week · ${displayedWeekDetails?.displayName ?: "Week $displayedWeek"}"
                                    } else {
                                        "${displayedWeekDetails?.displayName ?: "Week $displayedWeek"} template"
                                    },
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    if (displayedWeek == currentWeek) {
                                        val nextId = nextWeek(currentWeek, weekOrder)
                                        val next = weeks.firstOrNull { it.weekId == nextId }
                                        "The schedule rotates to ${next?.displayName ?: "Week $nextId"} next Monday."
                                    } else {
                                        "Preview or prepare this rotation week."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (displayedWeek != currentWeek) {
                                AssistChip(
                                    onClick = { viewModel.setCurrentWeek(displayedWeek) },
                                    label = { Text("Make current") }
                                )
                            }
                            IconButton(onClick = { editingWeek = displayedWeekDetails }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit week name and emoji")
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    viewModel.buildShoppingListForWeek(displayedWeek)
                                    scope.launch { snackbarHostState.showSnackbar("Week $displayedWeek added for review") }
                                },
                                enabled = hasIngredients,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.ShoppingCart, contentDescription = null)
                                Spacer(Modifier.size(8.dp))
                                Text("Build list")
                            }
                            OutlinedButton(
                                onClick = { showCopyWeekDialog = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null)
                                Spacer(Modifier.size(8.dp))
                                Text("Reuse week")
                            }
                        }
                    }
                }
            }

            if (weekMeals.isEmpty()) {
                item {
                    EmptyMealPlan(
                        week = displayedWeekDetails?.displayName ?: "Week $displayedWeek",
                        onAdd = {
                            editingMeal = null
                            showEditor = true
                        },
                        onCopy = { showCopyWeekDialog = true }
                    )
                }
            } else {
                item {
                    SectionHeading(
                        title = "The week at a glance",
                        supportingText = "Tap a day’s plus button to fill any gaps."
                    )
                }
                (1..7).forEach { day ->
                    val dayMeals = mealsByDay[day].orEmpty()
                    item(key = "day-$day") {
                        DaySchedule(
                            day = day,
                            meals = dayMeals,
                            onAdd = {
                                editingMeal = MealEntity(
                                    name = "",
                                    week = displayedWeek,
                                    ingredients = emptyList(),
                                    dayOfWeek = day
                                )
                                showEditor = true
                            },
                            onEdit = {
                                editingMeal = it
                                showEditor = true
                            },
                            onCopy = {
                                copyingMeal = it
                            },
                            onDelete = viewModel::deleteMeal
                        )
                    }
                }
            }
        }
    }

    if (showEditor) {
        MealEditorDialog(
            week = displayedWeek,
            meal = editingMeal,
            ingredientSuggestions = ingredientSuggestions,
            onDismiss = { showEditor = false },
            onSave = { name, day, slot, ingredients ->
                val existing = editingMeal?.takeIf { it.mealId != 0L }
                if (existing == null) {
                    viewModel.addMeal(name, displayedWeek, day, slot, ingredients)
                } else {
                    viewModel.updateMeal(existing, name, day, slot, ingredients)
                }
                showEditor = false
            }
        )
    }

    if (showCopyWeekDialog) {
        CopyWeekDialog(
            targetWeek = displayedWeek,
            weeks = weeks,
            onDismiss = { showCopyWeekDialog = false },
            onCopy = { sourceWeek ->
                viewModel.copyWeek(sourceWeek, displayedWeek)
                showCopyWeekDialog = false
            }
        )
    }

    editingWeek?.let { week ->
        WeekEditorDialog(
            week = week,
            onDismiss = { editingWeek = null },
            onSave = { name, emoji ->
                viewModel.updateMealWeek(week, name, emoji)
                editingWeek = null
            }
        )
    }

    copyingMeal?.let { meal ->
        CopyMealDialog(
            meal = meal,
            weeks = weeks,
            onDismiss = { copyingMeal = null },
            onCopy = { targetWeek ->
                viewModel.copyMealToWeek(meal, targetWeek)
                val target = weeks.firstOrNull { it.weekId == targetWeek }
                scope.launch { snackbarHostState.showSnackbar("Copied to ${target?.displayName ?: "Week $targetWeek"}") }
                copyingMeal = null
            }
        )
    }
}

@Composable
private fun WeekEditorDialog(
    week: MealWeekEntity,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var name by remember(week) { mutableStateOf(week.name) }
    var emoji by remember(week) { mutableStateOf(week.emoji) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Name this rotation week") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = emoji,
                    onValueChange = { emoji = it },
                    label = { Text("Emoji") },
                    placeholder = { Text("🍝") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Week name or theme") },
                    placeholder = { Text("e.g. Comfort food") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "The schedule and rotation position stay the same.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(name, emoji) }, enabled = name.isNotBlank()) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CopyWeekDialog(
    targetWeek: String,
    weeks: List<MealWeekEntity>,
    onDismiss: () -> Unit,
    onCopy: (String) -> Unit
) {
    val choices = weeks.filterNot { it.weekId == targetWeek }
    var selected by remember(targetWeek, choices) { mutableStateOf(choices.firstOrNull()?.weekId) }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
        title = { Text("Reuse another week") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Choose a template. Matching meals already in this week are kept once.")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    choices.forEach { week ->
                        FilterChip(
                            selected = selected == week.weekId,
                            onClick = { selected = week.weekId },
                            label = { Text(week.displayName) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { selected?.let(onCopy) }, enabled = selected != null) { Text("Add meals") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CopyMealDialog(
    meal: MealEntity,
    weeks: List<MealWeekEntity>,
    onDismiss: () -> Unit,
    onCopy: (String) -> Unit
) {
    val choices = weeks.filterNot { it.weekId == meal.week }
    var selected by remember(meal, choices) { mutableStateOf(choices.firstOrNull()?.weekId) }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
        title = { Text("Copy ${meal.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Choose the week to add this meal to.")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    choices.forEach { week ->
                        FilterChip(
                            selected = selected == week.weekId,
                            onClick = { selected = week.weekId },
                            label = { Text(week.displayName) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { selected?.let(onCopy) }, enabled = selected != null) { Text("Copy meal") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun DaySchedule(
    day: Int,
    meals: List<MealEntity>,
    onAdd: () -> Unit,
    onEdit: (MealEntity) -> Unit,
    onCopy: (MealEntity) -> Unit,
    onDelete: (MealEntity) -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(dayLabel(day), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                IconButton(onClick = onAdd) {
                    Icon(Icons.Default.Add, contentDescription = "Add meal on ${dayLabel(day)}")
                }
            }
            if (meals.isEmpty()) {
                Text(
                    "Nothing planned",
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                meals.forEachIndexed { index, meal ->
                    if (index > 0) Spacer(Modifier.height(8.dp))
                    MealRow(meal, onEdit, onCopy, onDelete)
                }
            }
        }
    }
}

@Composable
private fun MealRow(
    meal: MealEntity,
    onEdit: (MealEntity) -> Unit,
    onCopy: (MealEntity) -> Unit,
    onDelete: (MealEntity) -> Unit
) {
    val ingredientSummary = remember(meal.ingredients) { meal.ingredients.joinToString() }

    Card(
        onClick = { onEdit(meal) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                StatusPill(
                    label = meal.mealSlot,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(Modifier.height(4.dp))
                Text(meal.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                if (meal.ingredients.isNotEmpty()) {
                    Text(
                        ingredientSummary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            IconButton(onClick = { onCopy(meal) }) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy ${meal.name} to other week")
            }
            IconButton(onClick = { onDelete(meal) }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete ${meal.name}", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun EmptyMealPlan(week: String, onAdd: () -> Unit, onCopy: () -> Unit) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Default.Restaurant, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
            Text("$week is ready to plan", style = MaterialTheme.typography.titleLarge)
            Text(
                "Add meals day by day or start with the other week's schedule.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = onCopy) { Text("Reuse other week") }
                Button(onClick = onAdd) { Text("Add first meal") }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MealEditorDialog(
    week: String,
    meal: MealEntity?,
    ingredientSuggestions: List<String>,
    onDismiss: () -> Unit,
    onSave: (String, Int, String, List<String>) -> Unit
) {
    var name by remember(meal) { mutableStateOf(meal?.name.orEmpty()) }
    var ingredients by remember(meal) { mutableStateOf(meal?.ingredients?.toList().orEmpty()) }
    var day by remember(meal) { mutableIntStateOf(meal?.dayOfWeek ?: 1) }
    var slot by remember(meal) { mutableStateOf(meal?.mealSlot ?: MealEntity.SLOT_DINNER) }
    var addIngredientDialogVisible by remember(meal) { mutableStateOf(false) }
    var additionalIngredient by remember(meal) { mutableStateOf("") }

    val ingredientChoices = remember(ingredientSuggestions, ingredients) {
        MealIngredientSelection.choices(ingredients, ingredientSuggestions)
    }

    fun isIngredientSelected(ingredient: String): Boolean =
        ingredients.any { it.equals(ingredient, ignoreCase = true) }

    fun toggleIngredient(ingredient: String) {
        ingredients = MealIngredientSelection.toggle(ingredients, ingredient)
    }

    fun addIngredient(ingredient: String) {
        ingredients = MealIngredientSelection.add(ingredients, ingredient)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (meal?.mealId == 0L || meal == null) "Add to Week $week" else "Edit meal") },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 520.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Meal or plan") },
                        placeholder = { Text("e.g. Veggie chilli") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    SuggestionChip(
                        onClick = { name = "Eating out" },
                        label = { Text("Eating out") },
                        icon = { Icon(Icons.Default.Restaurant, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                }
                item {
                    Text("Day", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        (1..7).forEach { option ->
                            FilterChip(
                                selected = day == option,
                                onClick = { day = option },
                                label = { Text(dayLabel(option).take(3)) }
                            )
                        }
                    }
                }
                item {
                    Text("Meal", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MealEntity.SLOTS.forEach { option ->
                            FilterChip(
                                selected = slot == option,
                                onClick = { slot = option },
                                label = { Text(option) }
                            )
                        }
                    }
                }
                item {
                    Text("Shopping ingredients", style = MaterialTheme.typography.labelLarge)
                    Text(
                        "Tap ingredients to include them. Add something new with +.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ingredientChoices.forEach { ingredient ->
                            FilterChip(
                                selected = isIngredientSelected(ingredient),
                                onClick = { toggleIngredient(ingredient) },
                                label = {
                                    Text(
                                        ingredient,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            )
                        }
                        AssistChip(
                            onClick = { addIngredientDialogVisible = true },
                            label = { Text("Add ingredient") },
                            leadingIcon = {
                                Icon(Icons.Default.Add, contentDescription = null)
                            }
                        )
                    }
                    if (ingredients.isEmpty()) {
                        Text(
                            "No shopping ingredients selected.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(name, day, slot, ingredients)
                },
                enabled = name.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )

    if (addIngredientDialogVisible) {
        AlertDialog(
            onDismissRequest = {
                addIngredientDialogVisible = false
                additionalIngredient = ""
            },
            title = { Text("Add shopping ingredient") },
            text = {
                OutlinedTextField(
                    value = additionalIngredient,
                    onValueChange = { additionalIngredient = it },
                    label = { Text("Ingredient") },
                    placeholder = { Text("e.g. tomatoes") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    enabled = additionalIngredient.trim().isNotEmpty(),
                    onClick = {
                        addIngredient(additionalIngredient)
                        additionalIngredient = ""
                        addIngredientDialogVisible = false
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        addIngredientDialogVisible = false
                        additionalIngredient = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
