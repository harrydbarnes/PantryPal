package com.example.pantrypal.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.pantrypal.data.entity.ShoppingItemEntity
import com.example.pantrypal.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListScreen(viewModel: MainViewModel) {
    val shoppingList by viewModel.shoppingListState.collectAsState()
    val currentWeek by viewModel.currentWeek.collectAsState()
    var showAddItemDialog by remember { mutableStateOf(false) }

    val filteredList = remember(shoppingList, currentWeek) {
        shoppingList.filter { item ->
            item.frequency == ShoppingItemEntity.FREQ_ONE_OFF ||
            item.frequency == ShoppingItemEntity.FREQ_ESSENTIAL ||
            (currentWeek == "A" && item.frequency == ShoppingItemEntity.FREQ_WEEK_A) ||
            (currentWeek == "B" && item.frequency == ShoppingItemEntity.FREQ_WEEK_B)
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddItemDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Item")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
             Row(
                 modifier = Modifier.fillMaxWidth().padding(16.dp),
                 horizontalArrangement = Arrangement.SpaceBetween,
                 verticalAlignment = Alignment.CenterVertically
             ) {
                 Text(
                    "Shopping List",
                    style = MaterialTheme.typography.headlineMedium
                )
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Text(
                        text = "Week $currentWeek",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
             }

            if (filteredList.isNotEmpty() && filteredList.any { it.isChecked }) {
                Button(
                    onClick = { viewModel.clearCheckedShoppingItems() },
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Text("Clear Checked")
                }
            }

            LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(16.dp)) {
                items(filteredList) { item ->
                    ShoppingListItemRow(
                        item = item,
                        onToggle = { viewModel.toggleShoppingItem(item) },
                        onDelete = { viewModel.deleteShoppingItem(item) }
                    )
                }
            }
        }
    }

    if (showAddItemDialog) {
        var newItemName by remember { mutableStateOf("") }
        var newItemQty by remember { mutableStateOf("1.0") }
        var newItemUnit by remember { mutableStateOf("pcs") }
        var newItemFrequency by remember { mutableStateOf(ShoppingItemEntity.FREQ_ONE_OFF) }

        val frequencies = listOf(
            ShoppingItemEntity.FREQ_ONE_OFF,
            ShoppingItemEntity.FREQ_ESSENTIAL,
            ShoppingItemEntity.FREQ_WEEK_A,
            ShoppingItemEntity.FREQ_WEEK_B
        )

        var expanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddItemDialog = false },
            title = { Text("Add to Shopping List") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newItemName,
                        onValueChange = { newItemName = it },
                        label = { Text("Item Name") },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newItemQty,
                        onValueChange = { newItemQty = it },
                        label = { Text("Quantity") },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newItemUnit,
                        onValueChange = { newItemUnit = it },
                        label = { Text("Unit") },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = newItemFrequency,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Frequency") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            frequencies.forEach { selectionOption ->
                                DropdownMenuItem(
                                    text = { Text(selectionOption) },
                                    onClick = {
                                        newItemFrequency = selectionOption
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newItemName.isNotBlank()) {
                        viewModel.addShoppingItem(newItemName, newItemQty.toDoubleOrNull() ?: 1.0, newItemUnit, newItemFrequency)
                        showAddItemDialog = false
                    }
                }) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddItemDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ShoppingListItemRow(
    item: ShoppingItemEntity,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (item.isChecked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (item.isChecked) TextDecoration.LineThrough else null
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Qty: ${item.quantity} ${item.unit}",
                    style = MaterialTheme.typography.bodySmall
                )
                if (item.frequency != ShoppingItemEntity.FREQ_ONE_OFF) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                        Text(
                            text = item.frequency,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
        }
    }
}
