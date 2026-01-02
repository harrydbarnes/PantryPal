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
    var showAddItemDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddItemDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Item")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
             Text(
                "Shopping List",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(16.dp)
            )

            if (shoppingList.isNotEmpty() && shoppingList.any { it.isChecked }) {
                Button(
                    onClick = { viewModel.clearCheckedShoppingItems() },
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Text("Clear Checked")
                }
            }

            LazyColumn(contentPadding = PaddingValues(16.dp)) {
                items(shoppingList) { item ->
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

        AlertDialog(
            onDismissRequest = { showAddItemDialog = false },
            title = { Text("Add to Shopping List") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newItemName,
                        onValueChange = { newItemName = it },
                        label = { Text("Item Name") }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newItemQty,
                        onValueChange = { newItemQty = it },
                        label = { Text("Quantity") }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newItemUnit,
                        onValueChange = { newItemUnit = it },
                        label = { Text("Unit") }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newItemName.isNotBlank()) {
                        viewModel.addShoppingItem(newItemName, newItemQty.toDoubleOrNull() ?: 1.0, newItemUnit)
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
            Text(
                text = "Qty: ${item.quantity} ${item.unit}",
                style = MaterialTheme.typography.bodySmall
            )
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
        }
    }
}
