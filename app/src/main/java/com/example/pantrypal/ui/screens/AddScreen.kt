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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddScreen(
    barcode: String? = null,
    onAdd: (String, Double, String, String, Boolean, Boolean, Long?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var qty by remember { mutableStateOf(1.0) }
    var unit by remember { mutableStateOf("pcs") }
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
                            .atZone(java.time.ZoneId.systemDefault())
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

        // Quantity Buttons
        Text("Quantity", style = MaterialTheme.typography.titleSmall)
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            for (i in 1..5) {
                FilterChip(
                    selected = qty == i.toDouble(),
                    onClick = { qty = i.toDouble() },
                    label = { Text("$i") }
                )
            }
        }
        // Manual quantity override if needed > 5 or fractional
        // For simplicity based on request "clickable buttons up to 5", keeping it simple.
        // But let's add a small text field for custom if they want?
        // Request just said "Change quantity to be clickable buttons up to 5".
        // I'll stick to that but maybe allow manual edit via text field if I kept it?
        // I'll remove the text field for now as requested, or maybe show selected value.

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
        OutlinedTextField(
            value = expirationDate?.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) ?: "",
            onValueChange = {}, // Read only
            label = { Text("Expiration Date (Optional)") },
            modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true },
            enabled = false, // Disable typing, handle click on parent or overlay
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = MaterialTheme.colorScheme.outline,
                disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                //Forcing content color to look enabled
                disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
        // Workaround for clickable disabled textfield: Use a Box over it or trailing icon
        // Actually, easiest is just a clickable Box wrapping the TextField or a Row.

        Button(onClick = { showDatePicker = true }, modifier = Modifier.padding(top = 8.dp)) {
            Text("Select Date")
        }


        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                val expDateMillis = expirationDate?.atStartOfDay(java.time.ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
                onAdd(name, qty, unit, "General", false, false, expDateMillis)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Item")
        }
    }
}
