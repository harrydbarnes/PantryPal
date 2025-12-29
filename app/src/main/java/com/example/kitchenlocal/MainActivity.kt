package com.example.kitchenlocal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kitchenlocal.data.database.KitchenDatabase
import com.example.kitchenlocal.data.repository.KitchenRepository
import com.example.kitchenlocal.ui.theme.KitchenLocalTheme
import com.example.kitchenlocal.viewmodel.MainViewModel
import com.example.kitchenlocal.viewmodel.MainViewModelFactory
import com.example.kitchenlocal.viewmodel.InventoryUiModel
import com.example.kitchenlocal.data.entity.ConsumptionType

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = KitchenDatabase.getDatabase(this)
        val repository = KitchenRepository(database.itemDao(), database.inventoryDao(), database.consumptionDao())
        val viewModelFactory = MainViewModelFactory(repository)

        setContent {
            KitchenLocalTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    KitchenApp(viewModelFactory)
                }
            }
        }
    }
}

@Composable
fun KitchenApp(viewModelFactory: MainViewModelFactory) {
    val viewModel: MainViewModel = viewModel(factory = viewModelFactory)
    val inventory by viewModel.inventoryState.collectAsState()

    // Simple state for navigation (Screen switch)
    var currentScreen by remember { mutableStateOf("inventory") }

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(title = { Text("KitchenLocal") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                // Toggle screen for demo
                if (currentScreen == "inventory") currentScreen = "add" else currentScreen = "inventory"
            }) {
                Text(if (currentScreen == "inventory") "+" else "List")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (currentScreen == "inventory") {
                InventoryScreen(inventory, onConsume = { item, type ->
                    // Extract qty from string or pass logic.
                    // Simplified: just call vm
                    viewModel.consumeItem(item.inventoryId, item.itemId, 1.0, type)
                })
            } else {
                AddScreen(onAdd = { name, qty, unit, cat, veg, gf ->
                    viewModel.addItem(name, qty, unit, cat, veg, gf)
                    currentScreen = "inventory"
                })
            }
        }
    }
}

@Composable
fun InventoryScreen(items: List<InventoryUiModel>, onConsume: (InventoryUiModel, ConsumptionType) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(16.dp)) {
        items(items) { item ->
            InventoryItemRow(item, onConsume)
        }
    }
}

@Composable
fun InventoryItemRow(item: InventoryUiModel, onConsume: (InventoryUiModel, ConsumptionType) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = item.name, style = MaterialTheme.typography.titleMedium)
            Text(text = "Qty: ${item.quantity}")
            if (item.tags.isNotEmpty()) {
                Text(text = "Tags: ${item.tags.joinToString()}", style = MaterialTheme.typography.bodySmall)
            }
            Row {
                Button(onClick = { onConsume(item, ConsumptionType.FINISHED) }) {
                    Text("Finished")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { onConsume(item, ConsumptionType.WASTED) }) {
                    Text("Wasted")
                }
            }
        }
    }
}

@Composable
fun AddScreen(onAdd: (String, Double, String, String, Boolean, Boolean) -> Unit) {
    var name by remember { mutableStateOf("") }
    var qty by remember { mutableStateOf("1.0") }
    var unit by remember { mutableStateOf("pcs") }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Add New Item", style = MaterialTheme.typography.titleLarge)
        TextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
        TextField(value = qty, onValueChange = { qty = it }, label = { Text("Quantity") })
        TextField(value = unit, onValueChange = { unit = it }, label = { Text("Unit") })

        Button(
            onClick = {
                onAdd(name, qty.toDoubleOrNull() ?: 1.0, unit, "General", false, false)
            },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Save Item")
        }
    }
}
