package com.example.pantrypal.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.pantrypal.data.entity.InventoryEntity
import com.example.pantrypal.data.entity.ItemEntity
import com.example.pantrypal.ui.components.ExpressiveHero
import com.example.pantrypal.ui.components.StatusPill
import com.example.pantrypal.util.AddItemDefaults
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private const val EXPIRATION_DATE_PATTERN = "yyyy-MM-dd"

class AddItemState(defaults: AddItemDefaults = AddItemDefaults()) {
    var name by mutableStateOf("")
    var qtyText by mutableStateOf("1")
    var unit by mutableStateOf(defaults.unit)
    var category by mutableStateOf(defaults.category)
    var storageLocation by mutableStateOf(defaults.storageLocation)
    var isOpened by mutableStateOf(false)
    var expirationDate by mutableStateOf<LocalDate?>(null)
    var isVegetarian by mutableStateOf(false)
    var isGlutenFree by mutableStateOf(false)
    var isUsual by mutableStateOf(false)
    var lowStockThresholdText by mutableStateOf("1")

    val isValid: Boolean
        get() = name.isNotBlank() && (qtyText.toDoubleOrNull() ?: 0.0) > 0.0

    fun prepareNextItem() {
        name = ""
        qtyText = "1"
        isOpened = false
        expirationDate = null
        isVegetarian = false
        isGlutenFree = false
        isUsual = false
        lowStockThresholdText = "1"
    }

    companion object {
        private const val KEY_NAME = "name"
        private const val KEY_QTY_TEXT = "qtyText"
        private const val KEY_UNIT = "unit"
        private const val KEY_CATEGORY = "category"
        private const val KEY_LOCATION = "location"
        private const val KEY_OPENED = "opened"
        private const val KEY_EXPIRY = "expiry"
        private const val KEY_VEGETARIAN = "vegetarian"
        private const val KEY_GLUTEN_FREE = "glutenFree"
        private const val KEY_USUAL = "usual"
        private const val KEY_THRESHOLD = "threshold"

        val Saver: Saver<AddItemState, *> = mapSaver(
            save = { state -> mapOf(
                KEY_NAME to state.name, KEY_QTY_TEXT to state.qtyText, KEY_UNIT to state.unit,
                KEY_CATEGORY to state.category, KEY_LOCATION to state.storageLocation,
                KEY_OPENED to state.isOpened, KEY_EXPIRY to state.expirationDate?.toEpochDay(),
                KEY_VEGETARIAN to state.isVegetarian, KEY_GLUTEN_FREE to state.isGlutenFree,
                KEY_USUAL to state.isUsual, KEY_THRESHOLD to state.lowStockThresholdText
            ) },
            restore = { map -> AddItemState().apply {
                name = map[KEY_NAME] as? String ?: ""
                qtyText = map[KEY_QTY_TEXT] as? String ?: "1"
                unit = map[KEY_UNIT] as? String ?: "pcs"
                category = map[KEY_CATEGORY] as? String ?: "General"
                storageLocation = map[KEY_LOCATION] as? String ?: InventoryEntity.LOCATION_PANTRY
                isOpened = map[KEY_OPENED] as? Boolean ?: false
                expirationDate = (map[KEY_EXPIRY] as? Long)?.let(LocalDate::ofEpochDay)
                isVegetarian = map[KEY_VEGETARIAN] as? Boolean ?: false
                isGlutenFree = map[KEY_GLUTEN_FREE] as? Boolean ?: false
                isUsual = map[KEY_USUAL] as? Boolean ?: false
                lowStockThresholdText = map[KEY_THRESHOLD] as? String ?: "1"
            } }
        )
    }
}

@Composable
fun rememberAddItemState(defaults: AddItemDefaults): AddItemState =
    rememberSaveable(saver = AddItemState.Saver) { AddItemState(defaults) }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddScreen(
    defaults: AddItemDefaults = AddItemDefaults(),
    barcode: String? = null,
    onAdd: (String, Double, String, String, Boolean, Boolean, Long?, Boolean, Double?, String, Boolean) -> Unit,
    onCancel: (() -> Unit)? = null,
    preFillItem: ItemEntity? = null
) {
    val state = rememberAddItemState(defaults)
    var detailsExpanded by rememberSaveable { mutableStateOf(false) }
    var preferencesExpanded by rememberSaveable { mutableStateOf(false) }
    var expandedLocation by remember { mutableStateOf(false) }
    var expandedCategory by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()
    val scrollState = rememberScrollState()

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

    fun save() {
        onAdd(
            state.name.trim(), state.qtyText.toDoubleOrNull() ?: 1.0, state.unit.trim().ifBlank { "pcs" },
            state.category.trim().ifBlank { "General" }, state.isVegetarian, state.isGlutenFree,
            state.expirationDate?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(), state.isUsual,
            if (state.isUsual) state.lowStockThresholdText.toDoubleOrNull() else null,
            state.storageLocation.trim().ifBlank { InventoryEntity.LOCATION_PANTRY }, state.isOpened
        )
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = {
                datePickerState.selectedDateMillis?.let { millis ->
                    state.expirationDate = java.time.Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                }
                showDatePicker = false
            }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = datePickerState) }
    }

    Scaffold(
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Column(Modifier.navigationBarsPadding().imePadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { save(); onCancel?.invoke() }, enabled = state.isValid, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                        Text("Add item")
                    }
                    TextButton(onClick = { save(); state.prepareNextItem() }, enabled = state.isValid, modifier = Modifier.fillMaxWidth()) {
                        Text("Save and add another")
                    }
                    if (onCancel != null) TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
                }
            }
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().verticalScroll(scrollState).imePadding().padding(padding).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ExpressiveHero(
                eyebrow = "Stock the pantry", title = "Add an item",
                supportingText = "Start with the essentials. Extra details are ready when they help.", icon = Icons.Default.AddCircle
            )
            if (barcode != null) StatusPill(label = "Barcode $barcode")

            OutlinedTextField(value = state.name, onValueChange = { state.name = it }, label = { Text("Name") }, supportingText = { Text("Required") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = state.qtyText, onValueChange = { state.qtyText = it }, label = { Text("Quantity") }, supportingText = { Text("Required") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(), singleLine = true)

            ExpandableSection(
                title = "Item details", subtitle = "Unit, location, category, opened state and expiry", expanded = detailsExpanded,
                onExpandedChange = { detailsExpanded = it }
            ) {
                OutlinedTextField(value = state.unit, onValueChange = { state.unit = it }, label = { Text("Unit") }, supportingText = { Text("Used as your next default") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                ExposedDropdownMenuBox(expanded = expandedLocation, onExpandedChange = { expandedLocation = !expandedLocation }, modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(value = state.storageLocation, onValueChange = { state.storageLocation = it }, label = { Text("Storage location") }, supportingText = { Text("Used as your next default") }, modifier = Modifier.fillMaxWidth().menuAnchor(), trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedLocation) }, singleLine = true)
                    ExposedDropdownMenu(expanded = expandedLocation, onDismissRequest = { expandedLocation = false }) {
                        InventoryEntity.STORAGE_LOCATIONS.forEach { location -> DropdownMenuItem(text = { Text(location) }, onClick = { state.storageLocation = location; expandedLocation = false }) }
                    }
                }
                ExposedDropdownMenuBox(expanded = expandedCategory, onExpandedChange = { expandedCategory = !expandedCategory }, modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(value = state.category, onValueChange = { state.category = it }, label = { Text("Category") }, supportingText = { Text("Used as your next default") }, modifier = Modifier.fillMaxWidth().menuAnchor(), trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedCategory) }, singleLine = true)
                    ExposedDropdownMenu(expanded = expandedCategory, onDismissRequest = { expandedCategory = false }) {
                        ItemEntity.CATEGORIES.forEach { category -> DropdownMenuItem(text = { Text(category) }, onClick = { state.category = category; expandedCategory = false }) }
                    }
                }
                SettingSwitch(title = "Already opened", subtitle = "Prioritise this batch when using food", checked = state.isOpened, onCheckedChange = { state.isOpened = it })
                val formatter = remember { DateTimeFormatter.ofPattern(EXPIRATION_DATE_PATTERN) }
                val dateLabel = state.expirationDate?.format(formatter) ?: "Not set"
                OutlinedTextField(value = dateLabel, onValueChange = {}, readOnly = true, label = { Text("Expiry date") }, modifier = Modifier.fillMaxWidth().clickable(role = Role.Button, onClickLabel = "Select expiry date") { showDatePicker = true }.semantics { contentDescription = "Expiry date $dateLabel" }, trailingIcon = { IconButton(onClick = { showDatePicker = true }) { Icon(Icons.Default.DateRange, "Select expiry date") } }, singleLine = true)
            }

            ExpandableSection(
                title = "Preferences and restock", subtitle = "Dietary flags and low-stock reminders", expanded = preferencesExpanded,
                onExpandedChange = { preferencesExpanded = it }
            ) {
                SettingSwitch("Vegetarian", "Helpful for meal ideas and dietary filters", state.isVegetarian) { state.isVegetarian = it }
                SettingSwitch("Gluten free", "Keep dietary details visible at a glance", state.isGlutenFree) { state.isGlutenFree = it }
                SettingSwitch("Always keep stocked", "Suggest this for the next shop at your threshold", state.isUsual) { state.isUsual = it }
                if (state.isUsual) OutlinedTextField(value = state.lowStockThresholdText, onValueChange = { state.lowStockThresholdText = it }, label = { Text("Low-stock threshold") }, supportingText = { Text("Suggest a restock at or below this amount") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(), singleLine = true)
            }
        }
    }
}

@Composable
private fun ExpandableSection(title: String, subtitle: String, expanded: Boolean, onExpandedChange: (Boolean) -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Surface(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth().clickable(role = Role.Button, onClickLabel = if (expanded) "Hide $title" else "Show $title") { onExpandedChange(!expanded) }, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text(title, style = MaterialTheme.typography.titleMedium); Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                Text(if (expanded) "Hide" else "Add details", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
            if (expanded) content()
        }
    }
}

@Composable
private fun SettingSwitch(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(title, style = MaterialTheme.typography.titleMedium); Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
