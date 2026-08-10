package com.example.pantrypal.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.pantrypal.data.entity.ShoppingHistoryEntity
import com.example.pantrypal.data.entity.InventoryEntity
import com.example.pantrypal.data.entity.ShoppingItemEntity
import com.example.pantrypal.data.entity.ShoppingSectionEntity
import com.example.pantrypal.ui.components.ExpressiveHero
import com.example.pantrypal.ui.components.StatusPill
import com.example.pantrypal.viewmodel.MainViewModel
import com.example.pantrypal.util.ShoppingNeedStatus
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ShoppingListScreen(
    viewModel: MainViewModel,
    onScanReceipt: () -> Unit = {},
    onOpenShoppingTools: () -> Unit = {}
) {
    val shoppingList by viewModel.shoppingListState.collectAsState()
    val sections by viewModel.shoppingSectionsState.collectAsState()
    val history by viewModel.shoppingHistoryState.collectAsState()
    val mealWeeks by viewModel.mealWeeksState.collectAsState()
    val selectedWeek by viewModel.shoppingWeek.collectAsState()
    val buildPreview by viewModel.shoppingBuildPreview.collectAsState()

    var itemEditorSection by remember { mutableStateOf<ShoppingSectionEntity?>(null) }
    var editingItem by remember { mutableStateOf<ShoppingItemEntity?>(null) }
    var editingSection by remember { mutableStateOf<ShoppingSectionEntity?>(null) }
    var showSectionEditor by remember { mutableStateOf(false) }
    var showPutAwayDialog by remember { mutableStateOf(false) }

    val currentWeekDetails = mealWeeks.firstOrNull { it.weekId == selectedWeek }
    val visibleItems = remember(shoppingList, sections, selectedWeek) {
        val recurringIds = sections.filter { it.recursEveryWeek }.map { it.sectionId }.toSet()
        shoppingList.filter { item ->
            item.sectionId in recurringIds || item.weekId == null || item.weekId == selectedWeek
        }
    }

    var showPlanningControls by rememberSaveable { mutableStateOf(false) }
    var quickAddName by rememberSaveable { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val shoppingModeItems = remember(visibleItems) { splitShoppingItemsForShoppingMode(visibleItems) }
    val defaultQuickAddSection = sections.firstOrNull { it.systemKey == ShoppingSectionEntity.KEY_THE_REST }
        ?: sections.firstOrNull()

    fun submitQuickAdd() {
        val section = defaultQuickAddSection ?: return
        val name = quickAddName.trim()
        if (name.isNotEmpty()) {
            viewModel.addShoppingItem(name, 1.0, "pcs", section.sectionId, if (section.recursEveryWeek) null else selectedWeek)
            quickAddName = ""
        }
    }

    fun deleteWithUndo(item: ShoppingItemEntity) {
        viewModel.deleteShoppingItem(item)
        scope.launch {
            if (snackbarHostState.showSnackbar("${item.name} removed", "Undo") == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                viewModel.restoreShoppingItem(item)
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                QuickAddShoppingItem(
                    value = quickAddName,
                    onValueChange = { quickAddName = it },
                    onSubmit = ::submitQuickAdd,
                    suggestions = history.map { it.displayName }
                )
            }

            if (showPlanningControls) {
            item {
                ExpressiveHero(
                    eyebrow = currentWeekDetails?.displayName ?: "Week $selectedWeek",
                    title = if (visibleItems.all { it.isChecked } && visibleItems.isNotEmpty()) {
                        "That’s the whole shop ticked off"
                    } else {
                        "${visibleItems.count { !it.isChecked }} item${if (visibleItems.count { !it.isChecked } == 1) "" else "s"} left to gather"
                    },
                    supportingText = "Recurring essentials and meal-plan ingredients, sorted into a list that feels manageable.",
                    icon = if (visibleItems.all { it.isChecked } && visibleItems.isNotEmpty()) {
                        Icons.Default.CheckCircle
                    } else {
                        Icons.Default.ShoppingCart
                    },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = onScanReceipt,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Receipt")
                    }
                    OutlinedButton(
                        onClick = onOpenShoppingTools,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Budget")
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Shopping week", style = MaterialTheme.typography.titleMedium)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        mealWeeks.sortedBy { it.sortOrder }.forEach { week ->
                            FilterChip(
                                selected = selectedWeek == week.weekId,
                                onClick = { viewModel.setShoppingWeek(week.weekId) },
                                label = { Text(week.displayName) }
                            )
                        }
                    }
                    FilledTonalButton(
                        onClick = { viewModel.previewShoppingListForWeek(selectedWeek) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.RestaurantMenu, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Review meal-plan shopping")
                    }
                }
            }

            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusPill(
                        label = "${visibleItems.count { it.isChecked }} packed",
                        icon = Icons.Default.CheckCircle,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    StatusPill(
                        label = "${sections.size} sections",
                        icon = Icons.Default.Category
                    )
                    if (visibleItems.any { it.isChecked }) {
                        AssistChip(
                            onClick = { viewModel.clearCheckedShoppingItems(selectedWeek) },
                            label = { Text("Clear checked") }
                        )
                        AssistChip(
                            onClick = { showPutAwayDialog = true },
                            label = { Text("Finish shop & put away") },
                            leadingIcon = { Icon(Icons.Default.Inventory2, contentDescription = null) }
                        )
                    }
                }
            }

            }

            sections.forEach { section ->
                val sectionItems = shoppingModeItems.active.filter { it.sectionId == section.sectionId }
                if (sectionItems.isEmpty()) return@forEach
                item(key = "section-${section.sectionId}") {
                    ShoppingSectionCard(
                        section = section,
                        items = sectionItems,
                        onAdd = if (section.systemKey == ShoppingSectionEntity.KEY_MEAL_PLAN) null else {
                            {
                                editingItem = null
                                itemEditorSection = section
                            }
                        },
                        onEditSection = if (section.systemKey == null) {
                            {
                                editingSection = section
                                showSectionEditor = true
                            }
                        } else null,
                        onEditItem = { item ->
                            editingItem = item
                            itemEditorSection = section
                        },
                        onToggleItem = viewModel::toggleShoppingItem,
                        onDeleteItem = ::deleteWithUndo
                    )
                }
            }

            if (shoppingModeItems.active.isEmpty()) {
                item {
                    Text(
                        "No active items. Add something above or prepare your next list.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (shoppingModeItems.checked.isNotEmpty()) {
                item { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant) }
                item { Text("Checked items (${shoppingModeItems.checked.size})", style = MaterialTheme.typography.titleMedium) }
                sections.forEach { section ->
                    val sectionItems = shoppingModeItems.checked.filter { it.sectionId == section.sectionId }
                    if (sectionItems.isEmpty()) return@forEach
                    item(key = "checked-section-${section.sectionId}") {
                        ShoppingSectionCard(
                            section = section,
                            items = sectionItems,
                            onAdd = null,
                            onEditSection = null,
                            onEditItem = { item ->
                                editingItem = item
                                itemEditorSection = section
                            },
                            onToggleItem = viewModel::toggleShoppingItem,
                            onDeleteItem = ::deleteWithUndo
                        )
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssistChip(
                            onClick = { viewModel.clearCheckedShoppingItems(selectedWeek) },
                            label = { Text("Clear checked") }
                        )
                        AssistChip(
                            onClick = { showPutAwayDialog = true },
                            label = { Text("Finish shop & put away") },
                            leadingIcon = { Icon(Icons.Default.Inventory2, contentDescription = null) }
                        )
                    }
                }
            }

            item {
                OutlinedButton(onClick = { showPlanningControls = !showPlanningControls }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (showPlanningControls) "Hide planning & preparation" else "Plan & prepare")
                }
            }

            item {
                OutlinedButton(
                    onClick = {
                        editingSection = null
                        showSectionEditor = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Add shopping section")
                }
            }
        }
    }

    itemEditorSection?.let { section ->
        ShoppingItemEditorDialog(
            section = section,
            item = editingItem,
            history = history,
            onDismiss = {
                itemEditorSection = null
                editingItem = null
            },
            onSave = { name, quantity, unit ->
                val existing = editingItem
                if (existing == null) {
                    viewModel.addShoppingItem(
                        name = name,
                        quantity = quantity,
                        unit = unit,
                        sectionId = section.sectionId,
                        weekId = if (section.recursEveryWeek) null else selectedWeek
                    )
                } else {
                    viewModel.updateShoppingItem(existing, name, quantity, unit)
                }
                itemEditorSection = null
                editingItem = null
            }
        )
    }

    if (showSectionEditor) {
        ShoppingSectionEditorDialog(
            section = editingSection,
            onDismiss = { showSectionEditor = false },
            onSave = { name, everyWeek ->
                val existing = editingSection
                if (existing == null) viewModel.addShoppingSection(name, everyWeek)
                else viewModel.updateShoppingSection(existing, name, everyWeek)
                showSectionEditor = false
            },
            onDelete = editingSection?.let { section ->
                {
                    viewModel.deleteShoppingSection(section)
                    showSectionEditor = false
                }
            }
        )
    }

    buildPreview?.let { preview ->
        ShoppingBuildPreviewDialog(
            weekName = mealWeeks.firstOrNull { it.weekId == preview.weekId }?.displayName ?: "Week ${preview.weekId}",
            needToBuy = preview.lines.count { it.status == ShoppingNeedStatus.NEED_TO_BUY },
            alreadyAtHome = preview.lines.count { it.status == ShoppingNeedStatus.ALREADY_AT_HOME },
            checkStock = preview.lines.count { it.status == ShoppingNeedStatus.CHECK_STOCK },
            lines = preview.lines.map { line ->
                when (line.status) {
                    ShoppingNeedStatus.NEED_TO_BUY ->
                        "Need · ${line.name} · ${line.requiredQuantity.shoppingNumber()} ${line.unit}"
                    ShoppingNeedStatus.ALREADY_AT_HOME ->
                        "At home · ${line.name} · ${line.availableQuantity.shoppingNumber()} ${line.unit}"
                    ShoppingNeedStatus.CHECK_STOCK ->
                        "Check · ${line.name} · need ${line.requiredQuantity.shoppingNumber()}, " +
                            "found ${line.availableQuantity.shoppingNumber()} ${line.unit}"
                }
            },
            onDismiss = viewModel::dismissShoppingBuildPreview,
            onCommit = { viewModel.commitShoppingBuildPreview(includeCheckStock = true) }
        )
    }

    if (showPutAwayDialog) {
        PutAwayDialog(
            checkedCount = visibleItems.count { it.isChecked },
            onDismiss = { showPutAwayDialog = false },
            onPutAway = { location ->
                viewModel.finishShopping(selectedWeek, location)
                showPutAwayDialog = false
            }
        )
    }
}

private fun Double.shoppingNumber(): String =
    if (this % 1.0 == 0.0) toLong().toString() else toString()

internal data class ShoppingModeItems(
    val active: List<ShoppingItemEntity>,
    val checked: List<ShoppingItemEntity>
)

internal fun splitShoppingItemsForShoppingMode(items: List<ShoppingItemEntity>): ShoppingModeItems =
    ShoppingModeItems(
        active = items.filterNot { it.isChecked },
        checked = items.filter { it.isChecked }
    )

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickAddShoppingItem(
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    suggestions: List<String>
) {
    val matchingSuggestions = remember(suggestions, value) {
        suggestions.filter { value.isBlank() || it.contains(value, ignoreCase = true) }
            .filterNot { it.equals(value, ignoreCase = true) }
            .take(5)
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text("Quick add") },
            placeholder = { Text("Type an item, then press done") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onSubmit() }),
            modifier = Modifier.fillMaxWidth()
        )
        if (matchingSuggestions.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                matchingSuggestions.forEach { suggestion ->
                    AssistChip(onClick = { onValueChange(suggestion) }, label = { Text(suggestion) })
                }
            }
        }
    }
}

@Composable
private fun ShoppingBuildPreviewDialog(
    weekName: String,
    needToBuy: Int,
    alreadyAtHome: Int,
    checkStock: Int,
    lines: List<String>,
    onDismiss: () -> Unit,
    onCommit: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Prepare $weekName") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("$needToBuy to buy · $alreadyAtHome already at home · $checkStock to check")
                lines.take(12).forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
                if (lines.size > 12) {
                    Text("+ ${lines.size - 12} more", style = MaterialTheme.typography.bodySmall)
                }
                Text(
                    "Items already at home will stay off the list. Check-stock items will be included so you can confirm them in the shop.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = { Button(onClick = onCommit) { Text("Build list") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PutAwayDialog(
    checkedCount: Int,
    onDismiss: () -> Unit,
    onPutAway: (String) -> Unit
) {
    var location by remember { mutableStateOf(InventoryEntity.LOCATION_PANTRY) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Put purchases away") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("$checkedCount checked item${if (checkedCount == 1) "" else "s"} will be added to your stock.")
                Text("Default location", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    InventoryEntity.STORAGE_LOCATIONS.forEach { option ->
                        FilterChip(
                            selected = location == option,
                            onClick = { location = option },
                            label = { Text(option) }
                        )
                    }
                }
                Text(
                    "Matching products in the same location are merged. You can move individual batches afterwards.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = { Button(onClick = { onPutAway(location) }) { Text("Put away") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun ShoppingSectionCard(
    section: ShoppingSectionEntity,
    items: List<ShoppingItemEntity>,
    onAdd: (() -> Unit)?,
    onEditSection: (() -> Unit)?,
    onEditItem: (ShoppingItemEntity) -> Unit,
    onToggleItem: (ShoppingItemEntity) -> Unit,
    onDeleteItem: (ShoppingItemEntity) -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = sectionContainerColor(section),
                    contentColor = sectionContentColor(section)
                ) {
                    Icon(
                        sectionIcon(section),
                        contentDescription = null,
                        modifier = Modifier.padding(10.dp).size(22.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(section.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        when {
                            section.recursEveryWeek -> "Appears every week"
                            section.systemKey == ShoppingSectionEntity.KEY_MEAL_PLAN -> "Generated from this week's meals"
                            else -> "Extra items for this week"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                onEditSection?.let { action ->
                    IconButton(onClick = action) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit ${section.name}")
                    }
                }
                onAdd?.let { action ->
                    IconButton(onClick = action) {
                        Icon(Icons.Default.Add, contentDescription = "Add to ${section.name}")
                    }
                }
            }
            if (items.isEmpty()) {
                Text(
                    if (section.systemKey == ShoppingSectionEntity.KEY_MEAL_PLAN) {
                        "Build a list from the meal planner to fill this section."
                    } else {
                        "Nothing here yet."
                    },
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                items.forEachIndexed { index, item ->
                    if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    ShoppingListItemRow(
                        item = item,
                        onToggle = { onToggleItem(item) },
                        onEdit = { onEditItem(item) },
                        onDelete = { onDeleteItem(item) }
                    )
                }
            }
        }
    }
}

@Composable
private fun sectionContainerColor(section: ShoppingSectionEntity) = when (section.systemKey) {
    ShoppingSectionEntity.KEY_MEAL_PLAN -> MaterialTheme.colorScheme.tertiaryContainer
    ShoppingSectionEntity.KEY_EVERY_WEEK -> MaterialTheme.colorScheme.primaryContainer
    else -> MaterialTheme.colorScheme.secondaryContainer
}

@Composable
private fun sectionContentColor(section: ShoppingSectionEntity) = when (section.systemKey) {
    ShoppingSectionEntity.KEY_MEAL_PLAN -> MaterialTheme.colorScheme.onTertiaryContainer
    ShoppingSectionEntity.KEY_EVERY_WEEK -> MaterialTheme.colorScheme.onPrimaryContainer
    else -> MaterialTheme.colorScheme.onSecondaryContainer
}

private fun sectionIcon(section: ShoppingSectionEntity): ImageVector = when (section.systemKey) {
    ShoppingSectionEntity.KEY_EVERY_WEEK -> Icons.Default.Repeat
    ShoppingSectionEntity.KEY_MEAL_PLAN -> Icons.Default.RestaurantMenu
    ShoppingSectionEntity.KEY_BABY_STUFF -> Icons.Default.Favorite
    ShoppingSectionEntity.KEY_THE_REST -> Icons.Default.ShoppingBag
    else -> Icons.Default.Category
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShoppingItemEditorDialog(
    section: ShoppingSectionEntity,
    item: ShoppingItemEntity?,
    history: List<ShoppingHistoryEntity>,
    onDismiss: () -> Unit,
    onSave: (String, Double, String) -> Unit
) {
    var name by remember(item) { mutableStateOf(item?.name.orEmpty()) }
    var quantity by remember(item) { mutableStateOf(item?.quantity?.let(::formatQuantity) ?: "1") }
    var unit by remember(item) { mutableStateOf(item?.unit ?: "pcs") }
    val suggestions = remember(history, name) {
        history.asSequence()
            .map { it.displayName }
            .filter { suggestion -> name.isBlank() || suggestion.contains(name, ignoreCase = true) }
            .filterNot { it.equals(name, ignoreCase = true) }
            .take(8)
            .toList()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (item == null) "Add to ${section.name}" else "Edit shopping item") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Item") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (suggestions.isNotEmpty()) {
                    Text("Used before", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        suggestions.forEach { suggestion ->
                            AssistChip(onClick = { name = suggestion }, label = { Text(suggestion) })
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { quantity = it },
                        label = { Text("Quantity") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Unit") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, quantity.toDoubleOrNull() ?: 1.0, unit) },
                enabled = name.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun ShoppingSectionEditorDialog(
    section: ShoppingSectionEntity?,
    onDismiss: () -> Unit,
    onSave: (String, Boolean) -> Unit,
    onDelete: (() -> Unit)?
) {
    var name by remember(section) { mutableStateOf(section?.name.orEmpty()) }
    var everyWeek by remember(section) { mutableStateOf(section?.recursEveryWeek ?: true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (section == null) "Add shopping section" else "Edit section") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Section name") },
                    placeholder = { Text("e.g. Baby stuff") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Show every week", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Keep this section and its items in every rotation week.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = everyWeek, onCheckedChange = { everyWeek = it })
                }
                onDelete?.let { action ->
                    TextButton(onClick = action) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Delete section")
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(name, everyWeek) }, enabled = name.isNotBlank()) {
                Text("Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun ShoppingListItemRow(
    item: ShoppingItemEntity,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var actionsExpanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle)
            .padding(start = 8.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = item.isChecked, onCheckedChange = { onToggle() })
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (item.isChecked) TextDecoration.LineThrough else null,
                color = if (item.isChecked) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            Text(
                text = "${formatQuantity(item.quantity)} ${item.unit}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = { actionsExpanded = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = "More options for ${item.name}")
        }
        DropdownMenu(expanded = actionsExpanded, onDismissRequest = { actionsExpanded = false }) {
            DropdownMenuItem(
                text = { Text("Edit") },
                onClick = {
                    actionsExpanded = false
                    onEdit()
                }
            )
            DropdownMenuItem(
                text = { Text("Delete") },
                onClick = {
                    actionsExpanded = false
                    onDelete()
                },
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
            )
        }
    }
}

private fun formatQuantity(quantity: Double): String =
    if (quantity % 1.0 == 0.0) quantity.toLong().toString() else quantity.toString()
