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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.ExperimentalFoundationApi
import com.example.pantrypal.data.entity.StoreTag
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.animation.animateContentSize
import com.example.pantrypal.util.ShoppingAisles
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.CloudOff
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.example.pantrypal.ui.components.expressiveItem
import com.example.pantrypal.ui.components.expressiveItems
import com.example.pantrypal.ui.components.expressiveItemsIndexed
import com.example.pantrypal.ui.components.shoppingMotion
import com.example.pantrypal.ui.components.shoppingSegmentShape
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
import com.example.pantrypal.util.ShoppingMetadata
import com.example.pantrypal.util.normalizeShoppingName
import com.example.pantrypal.util.parseQuickShoppingEntry
import androidx.compose.ui.text.input.KeyboardType
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun ShoppingListScreen(
    viewModel: MainViewModel,
    onScanReceipt: () -> Unit = {},
    onOpenShoppingTools: () -> Unit = {},
    onOpenHousehold: () -> Unit = {},
    household: HouseholdSyncUiState = HouseholdSyncUiState()
) {
    val layout by viewModel.shoppingLayoutState.collectAsState()
    val profile by viewModel.shoppingProfile.collectAsState()
    var newShopName by rememberSaveable { mutableStateOf("") }
    var showAddShop by rememberSaveable { mutableStateOf(false) }
    val profileNames = remember(layout) {
        val type = object : com.google.gson.reflect.TypeToken<Map<String, String>>() {}.type
        mapOf(ShoppingAisles.DEFAULT_PROFILE to "My Aldi") + runCatching {
            layout[ShoppingAisles.PROFILES_KEY]?.let { com.google.gson.Gson().fromJson<Map<String, String>>(it, type) }.orEmpty()
        }.getOrDefault(emptyMap())
    }
    val profileName = profileNames[profile] ?: "My shop"
    val aisleMode by viewModel.prefersAisleMode.collectAsState()
    val inventory by viewModel.inventoryState.collectAsState()
    val currentWeek by viewModel.currentWeek.collectAsState()
    val saving by viewModel.saving.collectAsState()
    val actionError by viewModel.actionError.collectAsState()
    var editAisles by rememberSaveable { mutableStateOf(false) }
    var movingItem by remember { mutableStateOf<ShoppingItemEntity?>(null) }
    val aisles = remember(layout, profile) {
        runCatching { com.google.gson.Gson().fromJson(layout[ShoppingAisles.orderKey(profile)], Array<String>::class.java)?.toList() }
            .getOrNull()?.let(ShoppingAisles::cleanOrder) ?: ShoppingAisles.exampleOrder
    }
    val shoppingList by viewModel.shoppingListState.collectAsState()
    val sections by viewModel.shoppingSectionsState.collectAsState()
    val history by viewModel.shoppingHistoryState.collectAsState()
    val mealWeeks by viewModel.mealWeeksState.collectAsState()
    val selectedWeek by viewModel.shoppingWeek.collectAsState()
    val buildPreview by viewModel.shoppingBuildPreview.collectAsState()
    val lastShoppingChangeAt by viewModel.shoppingLastChangedAt.collectAsState()
    val shoppingArchive by viewModel.shoppingArchiveState.collectAsState()

    val loading by viewModel.shoppingLoading.collectAsState()
    val shoppingError by viewModel.shoppingError.collectAsState()
    var itemEditorSection by remember { mutableStateOf<ShoppingSectionEntity?>(null) }
    var editingItem by remember { mutableStateOf<ShoppingItemEntity?>(null) }
    var editingSection by remember { mutableStateOf<ShoppingSectionEntity?>(null) }
    var showSectionEditor by remember { mutableStateOf(false) }
    var showPutAwayDialog by remember { mutableStateOf(false) }
    var showShoppingArchive by rememberSaveable { mutableStateOf(false) }
    var finishedCount by rememberSaveable { mutableStateOf<Int?>(null) }

    val lowStaples by viewModel.likelyRunningLowStaples.collectAsState()
    var selectedStoreName by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedStore = selectedStoreName?.let(StoreTag::valueOf)
    var showTicked by rememberSaveable { mutableStateOf(false) }
    var showShopMenu by remember { mutableStateOf(false) }
    val currentWeekDetails = mealWeeks.firstOrNull { it.weekId == selectedWeek }
    val visibleItems = remember(shoppingList, sections, selectedWeek, selectedStore) {
        val recurringIds = sections.filter { it.recursEveryWeek }.map { it.sectionId }.toSet()
        shoppingList.filter { item ->
            (item.sectionId in recurringIds || item.weekId == null || item.weekId == selectedWeek) &&
                (selectedStore == null || item.storeTag == selectedStore || item.storeTag == StoreTag.EITHER)
        }
    }

    var showPlanningControls by rememberSaveable { mutableStateOf(false) }
    var quickAddName by rememberSaveable { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val unfinishedPurchases = visibleItems.filter { it.isChecked && layout[ShoppingMetadata.completed(it.syncId)] != "true" }
    val hasFinishedRegulars = visibleItems.any { layout[ShoppingMetadata.completed(it.syncId)] == "true" }
    val weekOrder = mealWeeks.sortedBy { it.sortOrder }.map { it.weekId }
    val weekOffset = if (weekOrder.isEmpty()) 0 else
        (weekOrder.indexOf(selectedWeek) - weekOrder.indexOf(currentWeek)).mod(weekOrder.size)
    val monday = com.example.pantrypal.util.startOfWeek(java.time.LocalDate.now()).plusWeeks(weekOffset.toLong())
    val dateFormat = java.time.format.DateTimeFormatter.ofPattern("d MMM", java.util.Locale.UK)
    val dates = "${monday.format(dateFormat)}–${monday.plusDays(6).format(dateFormat)}"

    fun addQuickEntry(text: String) {
        viewModel.quickAddShoppingItem(text) { change ->
            if (quickAddName == text) quickAddName = ""
            scope.launch {
                val message = if (change.before == null) "${change.after.name} added" else
                    "${change.after.name} increased to ${formatQuantity(change.after.quantity)} ${change.after.unit}"
                if (snackbarHostState.showSnackbar(message, "Undo") == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                    viewModel.undoQuickAdd(change)
                }
            }
        }
    }

    fun submitQuickAdd() { if (parseQuickShoppingEntry(quickAddName) != null) addQuickEntry(quickAddName) }

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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            AnimatedVisibility(visible = unfinishedPurchases.isNotEmpty()) {
                Surface {
                    ExtendedFloatingActionButton(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                            .semantics {
                                contentDescription = if (saving) "Restocking" else "Complete Shop & Restock (${unfinishedPurchases.size})"
                                if (saving) disabled()
                            },
                        onClick = {
                            if (!saving) viewModel.completeShopAndRestock(selectedStore) { count ->
                                if (count > 0) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                scope.launch { snackbarHostState.showSnackbar("$count purchases completed and restocked") }
                            }
                        },
                        icon = { Icon(Icons.Default.CheckCircle, contentDescription = null) },
                        text = { Text(if (saving) "Restocking…" else "Complete Shop & Restock (${unfinishedPurchases.size})") }
                    )
                }
            }
        },
        topBar = {
            Surface {
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("${currentWeekDetails?.displayName ?: "Week $selectedWeek"} · ${visibleItems.count { !it.isChecked }} left", style = MaterialTheme.typography.titleMedium)
                            Text(dates, style = MaterialTheme.typography.bodySmall)
                        }
                        if (unfinishedPurchases.isNotEmpty()) androidx.compose.foundation.layout.Box {
                            IconButton(onClick = { showShopMenu = true }) { Icon(androidx.compose.material.icons.Icons.Default.MoreVert, contentDescription = "Shopping actions") }
                            DropdownMenu(expanded = showShopMenu, onDismissRequest = { showShopMenu = false }) {
                                DropdownMenuItem(text = { Text("Remove Ticked Only") }, enabled = !saving, onClick = {
                                    showShopMenu = false
                                    viewModel.completeShopAndRestock(selectedStore, removeOnly = true) { count ->
                                        scope.launch { snackbarHostState.showSnackbar("$count ticked items removed; pantry stock unchanged") }
                                    }
                                })
                                DropdownMenuItem(text = { Text("Choose put-away locations") }, onClick = { showShopMenu = false; showPutAwayDialog = true })
                            }
                        }
                        else if (hasFinishedRegulars) TextButton(enabled = !saving, onClick = { viewModel.prepareNextShop() }) { Text("Prepare next shop") }
                    }
                    TextButton(onClick = onOpenHousehold, contentPadding = PaddingValues(0.dp)) {
                        Icon(Icons.Outlined.Group, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(household.syncLabel)
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            if (loading) expressiveItem { androidx.compose.material3.LinearProgressIndicator(Modifier.fillMaxWidth()) }
            shoppingError?.let { message -> expressiveItem { Text(message, color = MaterialTheme.colorScheme.error) } }
            actionError?.let { message -> expressiveItem { Text(message, color = MaterialTheme.colorScheme.error) } }
            if (lowStaples.isNotEmpty()) expressiveItem {
                Text("Likely Running Low", style = MaterialTheme.typography.titleMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    expressiveItems(lowStaples, key = { it.first.itemId }) { (product, batch) ->
                        AssistChip(enabled = !saving, onClick = { viewModel.addLowStaple(product, batch) }, label = { Text(product.name) },
                            leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) })
                    }
                }
            }
            expressiveItem {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    expressiveItem { FilterChip(selected = selectedStore == null, onClick = { selectedStoreName = null }, label = { Text("All Stores") }) }
                    expressiveItems(listOf(StoreTag.TESCO, StoreTag.ALDI), key = { it.name }) { store ->
                        FilterChip(selected = selectedStore == store, onClick = { selectedStoreName = store.name }, label = { Text(store.label) })
                    }
                }
            }
            expressiveItem {
                QuickAddShoppingItem(
                    value = quickAddName,
                    onValueChange = { quickAddName = it },
                    onSubmit = ::submitQuickAdd,
                    onAddSuggestion = ::addQuickEntry,
                    suggestions = history.map { it.displayName },
                    listedNames = visibleItems.filterNot { it.isChecked }.map { normalizeShoppingName(it.name) }.toSet()

                )
            }

            expressiveItem {
                OutlinedButton(
                    onClick = { showPlanningControls = !showPlanningControls },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (showPlanningControls) "Hide planning & preparation" else "Plan & prepare")
                }
            }

            if (showPlanningControls) {
            expressiveItem {
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

            expressiveItem {
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

            expressiveItem {
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
                        Text(if (buildPreview != null) "Resume saved shopping review" else "Review meal-plan shopping")
                    }
                }
            }

            expressiveItem {
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
                    if (unfinishedPurchases.isNotEmpty()) {
                        AssistChip(
                            onClick = { viewModel.clearCheckedShoppingItems(selectedWeek) },
                            label = { Text("Archive checked") }
                        )
                        AssistChip(
                            onClick = { showPutAwayDialog = true; viewModel.dismissActionError() },
                            label = { Text("Finish shop & put away") },
                            leadingIcon = { Icon(Icons.Default.Inventory2, contentDescription = null) }
                        )
                    }
                }
            }

            }

            expressiveItem {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Shop by aisle · $profileName", modifier = Modifier.weight(1f))
                    Switch(checked = aisleMode, onCheckedChange = viewModel::setAisleMode)
                }
                if (aisleMode) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        expressiveItems(profileNames.entries.toList(), key = { it.key }) { entry ->
                            FilterChip(selected = profile == entry.key, onClick = { viewModel.selectShoppingProfile(entry.key) }, label = { Text(entry.value) })
                        }
                    }
                    Row {
                        TextButton(onClick = { editAisles = true }) { Text("Edit aisle order") }
                        TextButton(onClick = { showAddShop = true }) { Text("Add another shop") }
                    }
                }
            }
            if (aisleMode) {
                val groups = visibleItems.filterNot { it.isChecked }.groupBy { ShoppingAisles.aisleFor(it.name, aisles, layout, inventory.firstOrNull { stock -> normalizeShoppingName(stock.name) == normalizeShoppingName(it.name) }?.category, inventory.firstOrNull { stock -> normalizeShoppingName(stock.name) == normalizeShoppingName(it.name) }?.storageLocation, profile) }
                (aisles + ShoppingAisles.UNASSIGNED).forEach { aisle ->
                    val rows = groups[aisle].orEmpty().sortedBy { it.isChecked }
                    if (rows.isNotEmpty()) {
                        stickyHeader(key = "aisle-$aisle") { Surface(Modifier.fillMaxWidth().then(shoppingMotion()), color = MaterialTheme.colorScheme.surfaceContainer) { Text(aisle, Modifier.padding(12.dp), style = MaterialTheme.typography.titleLarge) } }
                        expressiveItemsIndexed(rows, key = { _, row -> "item-${row.syncId}" }) { index, row ->
                            Card(Modifier.fillMaxWidth(), shape = shoppingSegmentShape(index, rows.size)) {
                                ShoppingListItemRow(row, { viewModel.toggleShoppingItem(row) }, { editingItem = row; itemEditorSection = sections.firstOrNull { it.sectionId == row.sectionId } }, { deleteWithUndo(row) }, layout[ShoppingMetadata.checkStock(row.syncId)] == "true", { viewModel.confirmShoppingStock(row) })
                                TextButton(enabled = !saving, onClick = { viewModel.cycleShoppingStore(row) }) { Text("Store: ${row.storeTag.label}") }
                            TextButton(onClick = { movingItem = row }) { Text("Aisle / section") }
                            }
                        }
                    }
                }
            } else {
                sections.forEach { section ->
                    val sectionItems = shoppingItemsForSection(visibleItems.filterNot { it.isChecked }, section.sectionId)
                    expressiveItem(key = "section-${section.syncId}") {
                        ShoppingSectionHeader(section, sectionItems.isEmpty(),
                            onAdd = { editingItem = null; itemEditorSection = section },
                            onEditSection = if (section.systemKey == null) { { editingSection = section; showSectionEditor = true } } else null)
                    }
                    sectionItems.groupBy { it.category.takeUnless { category -> category == "Uncategorised" } ?: com.example.pantrypal.domain.shopping.AisleCategoryMapper.categoryFor(it.name) }.toSortedMap().forEach { (category, rows) ->
                        stickyHeader(key = "category-${section.syncId}-$category") {
                            Surface(Modifier.fillMaxWidth().then(shoppingMotion()), color = MaterialTheme.colorScheme.surfaceContainer) {
                                Text(category, Modifier.padding(12.dp), style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    expressiveItemsIndexed(rows, key = { _, row -> "item-${row.syncId}" }) { index, row ->
                        Card(Modifier.fillMaxWidth(), shape = shoppingSegmentShape(index, rows.size)) {
                            ShoppingListItemRow(row, { viewModel.toggleShoppingItem(row) }, { editingItem = row; itemEditorSection = section }, { deleteWithUndo(row) }, layout[ShoppingMetadata.checkStock(row.syncId)] == "true", { viewModel.confirmShoppingStock(row) })
                            TextButton(enabled = !saving, onClick = { viewModel.cycleShoppingStore(row) }) { Text("Store: ${row.storeTag.label}") }
                            TextButton(onClick = { movingItem = row }) { Text("Move section / assign aisle") }
                        }
                    }
                    }
                }
            }

            val tickedRows = visibleItems.filter { it.isChecked }
            if (tickedRows.isNotEmpty()) {
                expressiveItem(key = "ticked-heading") {
                    TextButton(onClick = { showTicked = !showTicked }) {
                        Text("Ticked Items (${tickedRows.size})")
                        Icon(if (showTicked) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (showTicked) "Collapse ticked items" else "Expand ticked items")
                    }
                }
                expressiveItem(key = "ticked-content") {
                    AnimatedVisibility(visible = showTicked) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            tickedRows.forEachIndexed { index, row ->
                                androidx.compose.runtime.key(row.syncId) {
                                    Card(Modifier.fillMaxWidth(), shape = shoppingSegmentShape(index, tickedRows.size),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                                        ShoppingListItemRow(row, { viewModel.toggleShoppingItem(row) }, { editingItem = row; itemEditorSection = sections.firstOrNull { it.sectionId == row.sectionId } }, { deleteWithUndo(row) })
                                        TextButton(enabled = !saving, onClick = { viewModel.cycleShoppingStore(row) }) { Text("Store: ${row.storeTag.label}") }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (!loading && visibleItems.none { !it.isChecked }) {
                expressiveItem {
                    Text(
                        if (visibleItems.isEmpty()) "No items yet. Add something above or prepare your next list." else "Everything is ticked off. Add something above or prepare your next list.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (shoppingArchive.isNotEmpty()) {
                expressiveItem {
                    OutlinedButton(
                        onClick = { showShoppingArchive = !showShoppingArchive },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (showShoppingArchive) "Hide recent shops" else "Show recent shops")
                    }
                }
                if (showShoppingArchive) {
                    expressiveItem {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("Recent shop history", style = MaterialTheme.typography.titleMedium)
                                shoppingArchive.take(12).forEach { archived ->
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth()) {
                                            Text(
                                                archived.name,
                                                modifier = Modifier.weight(1f),
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Text(
                                                "${formatQuantity(archived.quantity)} ${archived.unit}",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                        Text(
                                            "${archived.sectionName} · ${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(archived.completedAt))} · ${archived.storageLocation ?: "Location not recorded"}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            expressiveItem {
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

    if (showAddShop) AlertDialog(onDismissRequest = { showAddShop = false },
        title = { Text("Add a shop") },
        text = { OutlinedTextField(value = newShopName, onValueChange = { newShopName = it }, label = { Text("Shop name") }, singleLine = true) },
        confirmButton = { Button(enabled = newShopName.isNotBlank(), onClick = { viewModel.addShoppingProfile(newShopName) { newShopName = ""; showAddShop = false } }) { Text("Add shop") } },
        dismissButton = { TextButton(onClick = { showAddShop = false }) { Text("Cancel") } })

    if (editAisles) AisleOrderDialog(aisles, profileName, { editAisles = false }) { order -> viewModel.saveAisleOrder(order); editAisles = false }
    movingItem?.let { row ->
        AlertDialog(onDismissRequest = { movingItem = null }, title = { Text("Place ${row.name}") },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    expressiveItem { Text("$profileName aisle", style = MaterialTheme.typography.titleMedium) }
                    expressiveItems(aisles + ShoppingAisles.UNASSIGNED, key = { it }) { aisle ->
                        TextButton(onClick = { viewModel.assignAisle(row.name, aisle); movingItem = null }) { Text(aisle) }
                    }
                    expressiveItem { Text("Planning section", style = MaterialTheme.typography.titleMedium) }
                    expressiveItems(sections, key = { it.syncId }) { section ->
                        TextButton(onClick = { viewModel.moveShoppingItem(row, section); movingItem = null }) { Text(section.name) }
                    }
                }
            }, confirmButton = { TextButton(onClick = { movingItem = null }) { Text("Close") } })
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

    val reviewOpen by viewModel.shoppingReviewOpen.collectAsState()
    buildPreview?.takeIf { reviewOpen }?.let { preview ->
        ShoppingPreparationDialog(viewModel, preview, mealWeeks.firstOrNull { it.weekId == preview.weekId }?.displayName ?: "Week ${preview.weekId}")
    }

    if (showPutAwayDialog) {
        PutAwayDialog(
            items = unfinishedPurchases,
            inventory = inventory,
            saving = saving,
            error = actionError,
            onDismiss = { showPutAwayDialog = false },
            onFinish = { locations, defaultLocation, updatePantry ->
                viewModel.finishShopping(selectedWeek, locations, defaultLocation, updatePantry, unfinishedPurchases) { count ->
                    showPutAwayDialog = false
                    finishedCount = count
                }
            }
        )
    }
    finishedCount?.let { count ->
        AlertDialog(onDismissRequest = { finishedCount = null },
            title = { Text("Shop finished") },
            text = { Text("$count purchases saved to your shop history. ${visibleItems.count { !it.isChecked }} items are still unpurchased. Recurring essentials stay ticked until you prepare the next shop.") },
            confirmButton = { Button(enabled = !saving, onClick = { viewModel.prepareNextShop { finishedCount = null } }) { Text("Prepare next shop") } },
            dismissButton = { TextButton(onClick = { finishedCount = null }) { Text("Done") } })
    }

}

private fun Double.shoppingNumber(): String =
    if (this % 1.0 == 0.0) toLong().toString() else toString()

internal fun shoppingItemsForSection(
    items: List<ShoppingItemEntity>,
    sectionId: Long
): List<ShoppingItemEntity> =
    items.filter { it.sectionId == sectionId }.sortedBy { it.isChecked }

@Composable
private fun QuickAddShoppingItem(
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onAddSuggestion: (String) -> Unit,
    suggestions: List<String>,
    listedNames: Set<String>
) {
    var showPreviousItems by rememberSaveable { mutableStateOf(false) }
    val matchingSuggestions = remember(suggestions, value) {
        suggestions.filter { value.isBlank() || it.contains(value, ignoreCase = true) }
            .filterNot { it.equals(value, ignoreCase = true) }
            .distinctBy { it.lowercase() }
    }
    val visibleSuggestions = matchingSuggestions.take(8)
    val previousSuggestions = matchingSuggestions.drop(8)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text("Quick add") },
            placeholder = { Text("Milk x2 or 500 g pasta") },
            trailingIcon = { IconButton(enabled = parseQuickShoppingEntry(value) != null, onClick = onSubmit) { Icon(Icons.Default.Add, "Add shopping item") } },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onSubmit() }),
            modifier = Modifier.fillMaxWidth()
        )
        if (visibleSuggestions.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(end = 4.dp)
                ) {
                    expressiveItems(visibleSuggestions, key = { it.lowercase() }) { suggestion ->
                        AssistChip(
                            onClick = { onAddSuggestion(suggestion) },
                            label = { Text(if (normalizeShoppingName(suggestion) in listedNames) "$suggestion · On your list" else suggestion) },
                            leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) }
                        )
                    }
                }
                if (previousSuggestions.isNotEmpty()) {
                    IconButton(
                        onClick = { showPreviousItems = !showPreviousItems }
                    ) {
                        Icon(
                            if (showPreviousItems) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (showPreviousItems) "Hide previous items" else "Show previous items"
                        )
                    }
                }
            }
        }
        if (showPreviousItems && previousSuggestions.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Previous items", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    previousSuggestions.forEach { suggestion ->
                        AssistChip(
                            onClick = { onAddSuggestion(suggestion) },
                            label = { Text(if (normalizeShoppingName(suggestion) in listedNames) "$suggestion · On your list" else suggestion) },
                            leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PutAwayDialog(
    items: List<ShoppingItemEntity>,
    inventory: List<com.example.pantrypal.viewmodel.InventoryUiModel>,
    saving: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onFinish: (Map<String, String>, String, Boolean) -> Unit
) {
    var updatePantry by rememberSaveable { mutableStateOf(true) }
    var defaultLocation by rememberSaveable { mutableStateOf(InventoryEntity.LOCATION_PANTRY) }
    var locations by rememberSaveable(items.map { it.syncId }) {
        mutableStateOf(items.map { item ->
            inventory.firstOrNull { normalizeShoppingName(it.name) == normalizeShoppingName(item.name) }?.storageLocation
                ?: ShoppingAisles.suggestStorage(item.name)
        })
    }
    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text("Finish this shop") },
        text = {
            LazyColumn(Modifier.heightIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                expressiveItem {
                    Text("${items.size} purchases will be saved to your shop history.")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = updatePantry, enabled = !saving, onCheckedChange = { updatePantry = it })
                        Text("Also update pantry stock")
                    }
                    Text("If a receipt has already added these purchases to your pantry, switch this off to avoid adding them twice.", style = MaterialTheme.typography.bodySmall)
                }
                if (updatePantry) {
                    expressiveItem {
                        Text("Set all locations", style = MaterialTheme.typography.titleSmall)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            InventoryEntity.STORAGE_LOCATIONS.forEach { option ->
                                FilterChip(selected = defaultLocation == option, enabled = !saving,
                                    onClick = { defaultLocation = option; locations = items.map { option } }, label = { Text(option) })
                            }
                        }
                    }
                    expressiveItems(items.indices.toList(), key = { items[it].syncId }) { index ->
                        Column {
                            Text(items[index].name, style = MaterialTheme.typography.titleMedium)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                InventoryEntity.STORAGE_LOCATIONS.forEach { option ->
                                    FilterChip(selected = locations.getOrNull(index) == option, enabled = !saving,
                                        onClick = { locations = locations.toMutableList().apply { set(index, option) } }, label = { Text(option) })
                                }
                            }
                        }
                    }
                }
                error?.let { message -> expressiveItem { Text(message, color = MaterialTheme.colorScheme.error) } }
            }
        },
        confirmButton = { Button(enabled = !saving && items.isNotEmpty(), onClick = {
            onFinish(items.mapIndexed { index, item -> item.syncId to locations[index] }.toMap(), defaultLocation, updatePantry)
        }) { Text(if (saving) "Saving…" else "Finish shop") } },
        dismissButton = { TextButton(enabled = !saving, onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun ShoppingSectionHeader(section: ShoppingSectionEntity, empty: Boolean, onAdd: () -> Unit, onEditSection: (() -> Unit)?) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = sectionContainerColor(section))) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(section.name, style = MaterialTheme.typography.titleMedium)
                if (empty) Text("No items yet. Add the first item here.", style = MaterialTheme.typography.bodySmall)
            }
            onEditSection?.let { action -> IconButton(onClick = action) { Icon(Icons.Default.Edit, "Edit ${section.name}") } }
            IconButton(onClick = onAdd) { Icon(Icons.Default.Add, "Add to ${section.name}") }
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
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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
                enabled = name.isNotBlank() && quantity.toDoubleOrNull()?.let { it.isFinite() && it > 0 } == true && unit.isNotBlank()
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
    onDelete: () -> Unit,
    needsStockCheck: Boolean = false,
    onConfirmStock: () -> Unit = {}
) {
    val haptics = LocalHapticFeedback.current
    val toggleWithFeedback = {
        if (!item.isChecked) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        onToggle()
    }
    var actionsExpanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().clickable(enabled = item.quantity > 0 || item.isChecked, onClick = toggleWithFeedback)
            .padding(start = 8.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = item.isChecked, enabled = item.quantity > 0 || item.isChecked, onCheckedChange = { toggleWithFeedback() })
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
            if (needsStockCheck) Text("Check stock at home", style = MaterialTheme.typography.labelSmall)
            Text(
                text = "${formatQuantity(item.quantity)} ${item.unit}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = { actionsExpanded = true }) {
            Icon(androidx.compose.material.icons.Icons.Default.MoreVert, contentDescription = "More options for ${item.name}")
        }
        DropdownMenu(expanded = actionsExpanded, onDismissRequest = { actionsExpanded = false }) {
            if (needsStockCheck) DropdownMenuItem(text = { Text("Stock checked") }, onClick = { actionsExpanded = false; onConfirmStock() })
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

@Composable
private fun AisleOrderDialog(initial: List<String>, shopName: String, onDismiss: () -> Unit, onSave: (List<String>) -> Unit) {
    var order by rememberSaveable { mutableStateOf(initial) }
    var newName by rememberSaveable { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("$shopName aisle order") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                expressiveItem { Text("This is an editable example. Put aisles in the order you walk through $shopName. Removed aisles move their items to Unassigned.") }
                expressiveItems(order, key = { it }) { aisle ->
                    val index = order.indexOf(aisle)
                    Column {
                        Text(aisle, style = MaterialTheme.typography.titleMedium)
                        Row {
                            TextButton(enabled = index > 0, onClick = { order = order.toMutableList().apply { add(index - 1, removeAt(index)) } }) { Text("Up") }
                            TextButton(enabled = index < order.lastIndex, onClick = { order = order.toMutableList().apply { add(index + 1, removeAt(index)) } }) { Text("Down") }
                            TextButton(onClick = { order = order - aisle }) { Text("Remove") }
                        }
                    }
                }
                expressiveItem {
                    OutlinedTextField(newName, { newName = it }, label = { Text("New aisle") }, modifier = Modifier.fillMaxWidth())
                    TextButton(enabled = newName.trim().isNotEmpty() && newName.trim() !in order && newName.trim() != ShoppingAisles.UNASSIGNED,
                        onClick = { order = order + newName.trim(); newName = "" }) { Text("Add aisle") }
                }
            }
        }, confirmButton = { TextButton(onClick = { onSave(order) }) { Text("Save") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}
