package com.example.pantrypal.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.ZoneOffset

class AddItemState {
    var name by mutableStateOf("")
    var qtyText by mutableStateOf("1.0")
    var unit by mutableStateOf("pcs")
    var category by mutableStateOf("General")
    var isVegetarian by mutableStateOf(false)
    var isGlutenFree by mutableStateOf(false)
    var expirationDate by mutableStateOf<LocalDate?>(null)

    val isValid: Boolean
        get() = name.isNotBlank() && (qtyText.toDoubleOrNull() ?: 0.0) > 0.0

    companion object {
        val Saver: Saver<AddItemState, *> = Saver(
            save = { state ->
                listOf(
                    state.name,
                    state.qtyText,
                    state.unit,
                    state.category,
                    state.isVegetarian,
                    state.isGlutenFree,
                    state.expirationDate?.toEpochDay()
                )
            },
            restore = { stored ->
                val list = stored as List<*>
                val state = AddItemState()
                state.name = list[0] as String
                state.qtyText = list[1] as String
                state.unit = list[2] as String
                state.category = list[3] as String
                state.isVegetarian = list[4] as Boolean
                state.isGlutenFree = list[5] as Boolean
                val dateEpoch = list[6] as Long?
                state.expirationDate = dateEpoch?.let { LocalDate.ofEpochDay(it) }
                state
            }
        )
    }
}

@Composable
fun rememberAddItemState(): AddItemState {
    return rememberSaveable(saver = AddItemState.Saver) { AddItemState() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddScreen(
    barcode: String? = null,
    onAdd: (String, Double, String, String, Boolean, Boolean, Long?) -> Unit,
    onCancel: (() -> Unit)? = null
) {
    val state = rememberAddItemState()

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
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Add New Item", style = MaterialTheme.typography.headlineMedium)
        if (barcode != null) {
            Text("Barcode: $barcode", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = state.name,
            onValueChange = { state.name = it },
            label = { Text("Product Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = state.category,
            onValueChange = { state.category = it },
            label = { Text("Category") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Quantity Input and Chips
        Text("Quantity", style = MaterialTheme.typography.titleSmall)

        OutlinedTextField(
            value = state.qtyText,
            onValueChange = { state.qtyText = it },
            label = { Text("Quantity") },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            for (i in 1..5) {
                FilterChip(
                    selected = (state.qtyText.toDoubleOrNull() ?: 0.0) == i.toDouble(),
                    onClick = { state.qtyText = i.toDouble().toString() },
                    label = { Text("$i") }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = state.unit,
            onValueChange = { state.unit = it },
            label = { Text("Size (Unit)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Expiration Date
        OutlinedTextField(
            value = state.expirationDate?.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) ?: "",
            onValueChange = {}, // Read only
            label = { Text("Expiration Date (Optional)") },
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            trailingIcon = {
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(Icons.Default.DateRange, contentDescription = "Select Date")
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Flags
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Vegetarian")
            Switch(checked = state.isVegetarian, onCheckedChange = { state.isVegetarian = it })
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Gluten Free")
            Switch(checked = state.isGlutenFree, onCheckedChange = { state.isGlutenFree = it })
        }

        Spacer(modifier = Modifier.height(32.dp))

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
                    onAdd(state.name, state.qtyText.toDoubleOrNull() ?: 1.0, state.unit, state.category, state.isVegetarian, state.isGlutenFree, expDateMillis)
                },
                modifier = Modifier.weight(1f),
                enabled = state.isValid
            ) {
                Text("Save Item")
            }
        }
    }
}
