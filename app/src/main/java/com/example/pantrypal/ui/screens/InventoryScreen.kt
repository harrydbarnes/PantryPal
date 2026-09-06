package com.example.pantrypal.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.pantrypal.viewmodel.InventoryUiModel
import com.example.pantrypal.data.entity.ConsumptionType
import com.example.pantrypal.ui.components.ExpressiveHero
import com.example.pantrypal.ui.components.FriendlyEmptyState
import com.example.pantrypal.ui.components.PantryPalSpacing
import com.example.pantrypal.ui.components.StatusPill
import com.example.pantrypal.util.ExpiryStatus
import com.example.pantrypal.util.InventorySort
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun InventoryScreen(
    items: List<InventoryUiModel>,
    onScanIn: () -> Unit,
    onScanOut: () -> Unit,
    onConsume: (InventoryUiModel, ConsumptionType) -> Unit,
    onAdjustQuantity: (Long, Double) -> Unit,
    onToggleOpened: (InventoryUiModel) -> Unit,
    onSaveDetails: (InventoryUiModel, Boolean, Double?, String, () -> Unit) -> Unit,
    saving: Boolean = false,
    error: String? = null
) {
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf<String?>(null) }
    var location by rememberSaveable { mutableStateOf<String?>(null) }
    var expiryFilter by rememberSaveable { mutableStateOf<ExpiryStatus?>(null) }
    var lowStockOnly by rememberSaveable { mutableStateOf(false) }
    var openedOnly by rememberSaveable { mutableStateOf(false) }
    var sort by rememberSaveable { mutableStateOf(InventorySort.NAME) }
    var showCategoryMenu by remember { mutableStateOf(false) }
    var showLocationMenu by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var editingStockItem by remember { mutableStateOf<InventoryUiModel?>(null) }

    val filteredItems = remember(items, query, category, location, expiryFilter, lowStockOnly, openedOnly, sort) {
        val filtered = items.asSequence()
            .filter {
                query.isBlank() ||
                    it.name.contains(query, ignoreCase = true) ||
                    it.category.contains(query, ignoreCase = true)
            }
            .filter { category == null || it.category == category }
            .filter { location == null || it.storageLocation == location }
            .filter {
                expiryFilter == null ||
                    it.expiryStatus == expiryFilter ||
                    (expiryFilter == ExpiryStatus.DUE_SOON && it.expiryStatus == ExpiryStatus.TODAY)
            }
            .filter { !lowStockOnly || it.isRestockNeeded }
            .filter { !openedOnly || it.isOpened }
        when (sort) {
            InventorySort.NAME -> filtered.sortedBy { it.name.lowercase() }.toList()
            InventorySort.EXPIRY -> filtered.sortedWith(compareBy<InventoryUiModel> { it.expirationDate == null }.thenBy { it.expirationDate }).toList()
            InventorySort.RECENTLY_ADDED -> filtered.sortedByDescending { it.addedDate }.toList()
            InventorySort.CATEGORY -> filtered.sortedWith(compareBy<InventoryUiModel> { it.category }.thenBy { it.name }).toList()
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ExpressiveHero(
                eyebrow = "Kitchen cupboard",
                title = "${filteredItems.size} of ${items.size} pantry item${if (items.size == 1) "" else "s"}",
                supportingText = "Finish what you have, spot what’s low, and make every ingredient count.",
                icon = Icons.Default.Inventory2,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PantryPalSpacing.xs)
            ) {
                FilledTonalButton(
                    onClick = onScanIn,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(PantryPalSpacing.xs))
                    Text("Scan in")
                }
                OutlinedButton(
                    onClick = onScanOut,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                    Spacer(Modifier.width(PantryPalSpacing.xs))
                    Text("Scan out")
                }
            }
        }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                label = { Text("Search pantry") },
                placeholder = { Text("Name or category") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
        item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box {
                    FilterChip(
                        selected = category != null,
                        onClick = { showCategoryMenu = true },
                        label = { Text(category ?: "Category") }
                    )
                    DropdownMenu(expanded = showCategoryMenu, onDismissRequest = { showCategoryMenu = false }) {
                        DropdownMenuItem(text = { Text("All categories") }, onClick = { category = null; showCategoryMenu = false })
                        items.map { it.category }.distinct().sorted().forEach { value ->
                            DropdownMenuItem(text = { Text(value) }, onClick = { category = value; showCategoryMenu = false })
                        }
                    }
                }
                Box {
                    FilterChip(
                        selected = location != null,
                        onClick = { showLocationMenu = true },
                        label = { Text(location ?: "Location") }
                    )
                    DropdownMenu(expanded = showLocationMenu, onDismissRequest = { showLocationMenu = false }) {
                        DropdownMenuItem(text = { Text("All locations") }, onClick = { location = null; showLocationMenu = false })
                        items.map { it.storageLocation }.distinct().sorted().forEach { value ->
                            DropdownMenuItem(text = { Text(value) }, onClick = { location = value; showLocationMenu = false })
                        }
                    }
                }
                FilterChip(
                    selected = expiryFilter == ExpiryStatus.EXPIRED,
                    onClick = { expiryFilter = if (expiryFilter == ExpiryStatus.EXPIRED) null else ExpiryStatus.EXPIRED },
                    label = { Text("Expired") }
                )
                FilterChip(
                    selected = expiryFilter == ExpiryStatus.DUE_SOON || expiryFilter == ExpiryStatus.TODAY,
                    onClick = { expiryFilter = if (expiryFilter == ExpiryStatus.DUE_SOON) null else ExpiryStatus.DUE_SOON },
                    label = { Text("Due soon") }
                )
                FilterChip(selected = lowStockOnly, onClick = { lowStockOnly = !lowStockOnly }, label = { Text("Low stock") })
                FilterChip(selected = openedOnly, onClick = { openedOnly = !openedOnly }, label = { Text("Opened") })
                Box {
                    AssistChip(onClick = { showSortMenu = true }, label = { Text("Sort: ${sort.name.lowercase().replace('_', ' ')}") })
                    DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                        InventorySort.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.name.lowercase().replace('_', ' ').replaceFirstChar { it.titlecase() }) },
                                onClick = { sort = option; showSortMenu = false }
                            )
                        }
                    }
                }
            }
        }
        if (items.isEmpty()) {
            item {
                FriendlyEmptyState(
                    title = "Your pantry is ready",
                    supportingText = "Scan something in or use the Add item button to start filling the shelves.",
                    icon = Icons.Default.Inventory2
                )
            }
        } else if (filteredItems.isEmpty()) {
            item {
                FriendlyEmptyState(
                    title = "Nothing matches those filters",
                    supportingText = "Try a broader search or clear one of the filter chips.",
                    icon = Icons.Default.Search
                )
            }
        }
        items(filteredItems, key = { it.inventoryId }) { item ->
            InventoryItemRow(
                item = item,
                onConsume = onConsume,
                onAdjustQuantity = onAdjustQuantity,
                onToggleOpened = onToggleOpened,
                onEditStockSettings = { editingStockItem = item }
            )
        }
    }

    editingStockItem?.let { item ->
        var alwaysStocked by remember(item) { mutableStateOf(item.isUsual) }
        var thresholdText by remember(item) { mutableStateOf(item.lowStockThreshold?.toString() ?: "1") }
        var selectedLocation by remember(item) { mutableStateOf(item.storageLocation) }
        AlertDialog(
            onDismissRequest = { if (!saving) editingStockItem = null },
            title = { Text("Restock settings") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Always keep ${item.name} stocked", modifier = Modifier.weight(1f))
                        Switch(checked = alwaysStocked, onCheckedChange = { alwaysStocked = it })
                    }
                    if (alwaysStocked) {
                        OutlinedTextField(
                            value = thresholdText,
                            onValueChange = { thresholdText = it },
                            label = { Text("Low-stock threshold") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    if (error != null) Text(error, color = MaterialTheme.colorScheme.error)
                    Text("Storage location", style = MaterialTheme.typography.labelLarge)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        com.example.pantrypal.data.entity.InventoryEntity.STORAGE_LOCATIONS.forEach { option ->
                            FilterChip(
                                selected = selectedLocation == option,
                                onClick = { selectedLocation = option },
                                label = { Text(option) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    onSaveDetails(item, alwaysStocked,
                        if (alwaysStocked) thresholdText.toDoubleOrNull() else null,
                        selectedLocation) { editingStockItem = null }
                }, enabled = !saving && (!alwaysStocked || thresholdText.toDoubleOrNull()?.let { it.isFinite() && it >= 0 } == true)) { Text(if (saving) "Saving…" else "Save") }
            },
            dismissButton = { TextButton(enabled = !saving, onClick = { editingStockItem = null }) { Text("Cancel") } }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InventoryItemRow(
    item: InventoryUiModel,
    onConsume: (InventoryUiModel, ConsumptionType) -> Unit,
    onAdjustQuantity: (Long, Double) -> Unit,
    onToggleOpened: (InventoryUiModel) -> Unit,
    onEditStockSettings: (InventoryUiModel) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            if (item.imageUrl != null) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = item.name,
                    modifier = Modifier.size(80.dp).clip(MaterialTheme.shapes.medium),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            } else {
                Surface(
                    modifier = Modifier.size(80.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(
                        Icons.Default.Kitchen,
                        contentDescription = null,
                        modifier = Modifier.padding(22.dp)
                    )
                }
            }
            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )
                    if (item.isRestockNeeded) {
                        StatusPill(
                            label = "Low",
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
                Text(
                    text = "${item.quantity} · ${item.category} · ${item.storageLocation}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (item.expirationDate != null) {
                    Text(
                        text = item.expiryLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (item.expiryStatus == ExpiryStatus.EXPIRED) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
                if (item.tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item.tags.forEach { tag ->
                            StatusPill(label = tag)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onAdjustQuantity(item.inventoryId, -1.0.coerceAtMost(item.quantityValue)) },
                        enabled = item.quantityValue > 0
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Reduce ${item.name} by one")
                    }
                    Text(item.quantity, style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = { onAdjustQuantity(item.inventoryId, 1.0) }) {
                        Icon(Icons.Default.Add, contentDescription = "Add one ${item.name}")
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = item.isOpened,
                        onClick = { onToggleOpened(item) },
                        label = { Text(if (item.isOpened) "Opened" else "Unopened") }
                    )
                    AssistChip(
                        onClick = { onEditStockSettings(item) },
                        label = { Text(if (item.isUsual) "Restock at ${item.lowStockThreshold ?: 0}" else "Restock settings") }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = { onConsume(item, ConsumptionType.FINISHED) }) {
                        Text(if (item.quantityValue <= 1.0) "Finished" else "Use 1")
                    }
                    TextButton(
                        onClick = { onConsume(item, ConsumptionType.WASTED) },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Wasted")
                    }
                }
            }
        }
    }
}
