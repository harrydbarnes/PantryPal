package com.example.pantrypal.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddScreen(
    barcode: String? = null,
    onAdd: (String, Double, String, String, Boolean, Boolean, Long?) -> Unit,
    onCancel: (() -> Unit)? = null
) {
    var name by remember { mutableStateOf("") }
    var qtyText by remember { mutableStateOf("1.0") }
    var unit by remember { mutableStateOf("pcs") }
    var category by remember { mutableStateOf("General") }
    var isVegetarian by remember { mutableStateOf(false) }
    var isGlutenFree by remember { mutableStateOf(false) }
    var expirationDate by remember { mutableStateOf<LocalDate?>(null) }

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
                        expirationDate = java.time.Instant.ofEpochMilli(millis)
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
            value = name,
            onValueChange = { name = it },
            label = { Text("Product Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = category,
            onValueChange = { category = it },
            label = { Text("Category") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Quantity Input and Chips
        Text("Quantity", style = MaterialTheme.typography.titleSmall)

        OutlinedTextField(
            value = qtyText,
            onValueChange = { qtyText = it },
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
                    selected = (qtyText.toDoubleOrNull() ?: 0.0) == i.toDouble(),
                    onClick = { qtyText = i.toDouble().toString() },
                    label = { Text("$i") }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = unit,
            onValueChange = { unit = it },
            label = { Text("Size (Unit)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Expiration Date
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = expirationDate?.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) ?: "",
                onValueChange = {}, // Read only
                label = { Text("Expiration Date (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true
            )
            // Mask the click to the box to ensure it triggers
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { showDatePicker = true }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Flags
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Vegetarian")
            Switch(checked = isVegetarian, onCheckedChange = { isVegetarian = it })
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Gluten Free")
            Switch(checked = isGlutenFree, onCheckedChange = { isGlutenFree = it })
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
                    val expDateMillis = expirationDate?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
                    onAdd(name, qtyText.toDoubleOrNull() ?: 1.0, unit, category, isVegetarian, isGlutenFree, expDateMillis)
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Save Item")
            }
        }
    }
}
