package com.example.pantrypal.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.NoFood
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.foundation.clickable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.ZoneOffset
import com.example.pantrypal.data.entity.ItemEntity
import com.example.pantrypal.data.entity.InventoryEntity
import com.example.pantrypal.ui.components.ExpressiveHero
import com.example.pantrypal.ui.components.SectionHeading
import com.example.pantrypal.ui.components.StatusPill

private const val EXPIRATION_DATE_PATTERN = "yyyy-MM-dd"

class AddItemState {
    var name by mutableStateOf("")
    var qtyText by mutableStateOf("1.0")
    var unit by mutableStateOf("pcs")
    var category by mutableStateOf("General")
    var isVegetarian by mutableStateOf(false)
    var isGlutenFree by mutableStateOf(false)
    var expirationDate by mutableStateOf<LocalDate?>(null)
    var isUsual by mutableStateOf(false)
    var lowStockThresholdText by mutableStateOf("1")
    var storageLocation by mutableStateOf(InventoryEntity.LOCATION_PANTRY)
    var isOpened by mutableStateOf(false)

    val isValid: Boolean
        get() = name.isNotBlank() && (qtyText.toDoubleOrNull() ?: 0.0) > 0.0

    companion object {
        private const val KEY_NAME = "name"
        private const val KEY_QTY_TEXT = "qtyText"
        private const val KEY_UNIT = "unit"
        private const val KEY_CATEGORY = "category"
        private const val KEY_IS_VEGETARIAN = "isVegetarian"
        private const val KEY_IS_GLUTEN_FREE = "isGlutenFree"
        private const val KEY_EXPIRATION_DATE = "expirationDate"
        private const val KEY_IS_USUAL = "isUsual"
        private const val KEY_LOW_STOCK_THRESHOLD = "lowStockThreshold"
        private const val KEY_STORAGE_LOCATION = "storageLocation"
        private const val KEY_IS_OPENED = "isOpened"

        val Saver: Saver<AddItemState, *> = mapSaver(
            save = { state ->
                mapOf(
                    KEY_NAME to state.name,
                    KEY_QTY_TEXT to state.qtyText,
                    KEY_UNIT to state.unit,
                    KEY_CATEGORY to state.category,
                    KEY_IS_VEGETARIAN to state.isVegetarian,
                    KEY_IS_GLUTEN_FREE to state.isGlutenFree,
                    KEY_EXPIRATION_DATE to state.expirationDate?.toEpochDay(),
                    KEY_IS_USUAL to state.isUsual,
                    KEY_LOW_STOCK_THRESHOLD to state.lowStockThresholdText,
                    KEY_STORAGE_LOCATION to state.storageLocation,
                    KEY_IS_OPENED to state.isOpened
                )
            },
            restore = { map ->
                AddItemState().apply {
                    name = map[KEY_NAME] as? String ?: ""
                    qtyText = map[KEY_QTY_TEXT] as? String ?: "1.0"
                    unit = map[KEY_UNIT] as? String ?: "pcs"
                    category = map[KEY_CATEGORY] as? String ?: "General"
                    isVegetarian = map[KEY_IS_VEGETARIAN] as? Boolean ?: false
                    isGlutenFree = map[KEY_IS_GLUTEN_FREE] as? Boolean ?: false
                    val dateEpoch = map[KEY_EXPIRATION_DATE] as? Long
                    expirationDate = dateEpoch?.let { LocalDate.ofEpochDay(it) }
                    isUsual = map[KEY_IS_USUAL] as? Boolean ?: false
                    lowStockThresholdText = map[KEY_LOW_STOCK_THRESHOLD] as? String ?: "1"
                    storageLocation = map[KEY_STORAGE_LOCATION] as? String ?: InventoryEntity.LOCATION_PANTRY
                    isOpened = map[KEY_IS_OPENED] as? Boolean ?: false
                }
            }
        )
    }
}

@Composable
fun rememberAddItemState(): AddItemState {
    return rememberSaveable(saver = AddItemState.Saver) { AddItemState() }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddScreen(
    barcode: String? = null,
    onAdd: (String, Double, String, String, Boolean, Boolean, Long?, Boolean, Double?, String, Boolean) -> Unit,
    onCancel: (() -> Unit)? = null,
    preFillItem: com.example.pantrypal.data.entity.ItemEntity? = null
) {
    val state = rememberAddItemState()

    // Pre-fill if item provided
    LaunchedEffect(preFillItem) {
        preFillItem?.let { item ->
            state.name = item.name
            state.unit = item.defaultUnit
            state.category = item.category
            state.isVegetarian = item.isVegetarian
            state.isGlutenFree = item.isGlutenFree
            state.isUsual = item.isUsual
            state.lowStockThresholdText = item.lowStockThreshold?.toString() ?: "1"
        }
    }

    // Form scrolling
    val scrollState = rememberScrollState()

    // Date picker state
    val datePickerState = rememberDatePickerState()
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        state.expirationDate = java.time.Instant.ofEpochMilli(millis)
                            .atZone(ZoneOffset.UTC)
                            .toLocalDate()
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .imePadding()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ExpressiveHero(
            eyebrow = "Stock the pantry",
            title = "Add something delicious",
            supportingText = "A few useful details now make stock checks and shopping nudges much smarter later.",
            icon = Icons.Default.AddCircle
        )
        if (barcode != null) {
            StatusPill(label = "Barcode $barcode")
        }

        OutlinedTextField(
            value = state.name,
            onValueChange = { state.name = it },
            label = { Text("Product Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        SectionHeading(
            title = "Where will it live?",
            supportingText = "Locations make stocktakes and finding ingredients faster."
        )

        var expandedLocation by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = expandedLocation,
            onExpandedChange = { expandedLocation = !expandedLocation },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = state.storageLocation,
                onValueChange = { state.storageLocation = it },
                label = { Text("Storage location") },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedLocation) },
                singleLine = true
            )
            ExposedDropdownMenu(
                expanded = expandedLocation,
                onDismissRequest = { expandedLocation = false }
            ) {
                InventoryEntity.STORAGE_LOCATIONS.forEach { location ->
                    DropdownMenuItem(
                        text = { Text(location) },
                        onClick = {
                            state.storageLocation = location
                            expandedLocation = false
                        }
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Already opened", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Useful when deciding which batch to finish first",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = state.isOpened, onCheckedChange = { state.isOpened = it })
        }

        val categories = ItemEntity.CATEGORIES
        var expandedCategory by remember { mutableStateOf(false) }

        ExposedDropdownMenuBox(
            expanded = expandedCategory,
            onExpandedChange = { expandedCategory = !expandedCategory },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = state.category,
                onValueChange = { state.category = it },
                label = { Text("Category") },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategory) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                singleLine = true
            )
            ExposedDropdownMenu(
                expanded = expandedCategory,
                onDismissRequest = { expandedCategory = false }
            ) {
                categories.forEach { selectionOption ->
                    DropdownMenuItem(
                        text = { Text(selectionOption) },
                        onClick = {
                            state.category = selectionOption
                            expandedCategory = false
                        }
                    )
                }
            }
        }

        SectionHeading(
            title = "How much is going in?",
            supportingText = "Choose a quick amount or type the exact quantity."
        )

        OutlinedTextField(
            value = state.qtyText,
            onValueChange = { state.qtyText = it },
            label = { Text("Quantity") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (i in 1..5) {
                FilterChip(
                    selected = (state.qtyText.toDoubleOrNull() ?: 0.0) == i.toDouble(),
                    onClick = { state.qtyText = i.toDouble().toString() },
                    label = { Text("$i") }
                )
            }
        }

        OutlinedTextField(
            value = state.unit,
            onValueChange = { state.unit = it },
            label = { Text("Size (Unit)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Expiration Date
        Box(modifier = Modifier.fillMaxWidth()) {
            val expirationDateFormatter = remember { DateTimeFormatter.ofPattern(EXPIRATION_DATE_PATTERN) }
            val formattedDate = remember(state.expirationDate) {
                state.expirationDate?.format(expirationDateFormatter) ?: ""
            }
            val onDatePickerClick = { showDatePicker = true }

            OutlinedTextField(
                value = formattedDate,
                onValueChange = {}, // Read only
                label = { Text("Expiration Date (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = onDatePickerClick) {
                        Icon(Icons.Default.DateRange, contentDescription = "Select Date")
                    }
                }
            )

            val contentDesc = "Expiration Date ${formattedDate.ifEmpty { "(Optional)" }}"

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable(
                        onClick = onDatePickerClick,
                        role = Role.Button,
                        onClickLabel = "Select expiration date"
                    )
                    .semantics {
                        contentDescription = contentDesc
                    }
            )
        }

        // Flags
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(Icons.Default.Eco, contentDescription = null, modifier = Modifier.padding(10.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Vegetarian", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Helpful for meal ideas and dietary filters",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = state.isVegetarian, onCheckedChange = { state.isVegetarian = it })
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Always keep stocked", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Suggest it for the next shop when stock reaches your threshold",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = state.isUsual, onCheckedChange = { state.isUsual = it })
                }
                if (state.isUsual) {
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = state.lowStockThresholdText,
                        onValueChange = { state.lowStockThresholdText = it },
                        label = { Text("Low-stock threshold") },
                        supportingText = { Text("Suggest a restock at or below this amount") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                ) {
                    Icon(Icons.Default.NoFood, contentDescription = null, modifier = Modifier.padding(10.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Gluten free", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Keep dietary details visible at a glance",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = state.isGlutenFree, onCheckedChange = { state.isGlutenFree = it })
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onCancel != null) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
            }

            Button(
                onClick = {
                    val expDateMillis = state.expirationDate?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
                    onAdd(
                        state.name,
                        state.qtyText.toDoubleOrNull() ?: 1.0,
                        state.unit,
                        state.category,
                        state.isVegetarian,
                        state.isGlutenFree,
                        expDateMillis,
                        state.isUsual,
                        if (state.isUsual) state.lowStockThresholdText.toDoubleOrNull() else null,
                        state.storageLocation,
                        state.isOpened
                    )
                },
                modifier = Modifier.weight(1f).height(56.dp),
                enabled = state.isValid
            ) {
                Text("Save Item")
            }
        }
    }
}
