package com.example.pantrypal

import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pantrypal.data.database.KitchenDatabase
import com.example.pantrypal.data.repository.KitchenRepository
import com.example.pantrypal.ui.theme.PantryPalTheme
import com.example.pantrypal.viewmodel.MainViewModel
import com.example.pantrypal.viewmodel.MainViewModelFactory
import com.example.pantrypal.viewmodel.InventoryUiModel
import com.example.pantrypal.data.entity.ConsumptionType
import com.example.pantrypal.ui.BarcodeScanner
import kotlinx.coroutines.launch
import com.example.pantrypal.data.dao.InventoryWithItemMap
import com.example.pantrypal.data.entity.ItemEntity

sealed class AppScreen {
    data object Dashboard : AppScreen()
    data object Inventory : AppScreen()
    data object AddManual : AppScreen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = KitchenDatabase.getDatabase(this)
        val repository = KitchenRepository(database.itemDao(), database.inventoryDao(), database.consumptionDao())
        val viewModelFactory = MainViewModelFactory(repository)

        setContent {
            PantryPalTheme {
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
    val expiringItems by viewModel.expiringItemsState.collectAsState()

    var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.Dashboard) }
    var showScanIn by remember { mutableStateOf(false) }
    var showScanOut by remember { mutableStateOf(false) }

    // Permission handling
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
        }
    )

    LaunchedEffect(key1 = true) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(title = { Text("PantryPal") })
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Dashboard") },
                    label = { Text("Dashboard") },
                    selected = currentScreen == AppScreen.Dashboard,
                    onClick = { currentScreen = AppScreen.Dashboard }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.List, contentDescription = "Inventory") },
                    label = { Text("Inventory") },
                    selected = currentScreen == AppScreen.Inventory,
                    onClick = { currentScreen = AppScreen.Inventory }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Add, contentDescription = "Scan In") },
                    label = { Text("Scan In") },
                    selected = showScanIn,
                    onClick = {
                        if (hasCameraPermission) {
                            showScanIn = true
                        } else {
                            launcher.launch(Manifest.permission.CAMERA)
                        }
                    }
                )
                 NavigationBarItem(
                    icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan Out") },
                    label = { Text("Scan Out") },
                    selected = showScanOut,
                    onClick = {
                        if (hasCameraPermission) {
                            showScanOut = true
                        } else {
                            launcher.launch(Manifest.permission.CAMERA)
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            if (currentScreen == AppScreen.Inventory) {
                 FloatingActionButton(onClick = { currentScreen = AppScreen.AddManual }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Manually")
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when {
                showScanIn -> {
                    ScanInScreen(
                        onDismiss = { showScanIn = false },
                        viewModel = viewModel
                    )
                }
                showScanOut -> {
                     ScanOutScreen(
                        onDismiss = { showScanOut = false },
                        viewModel = viewModel
                    )
                }
                currentScreen == AppScreen.Dashboard -> {
                    DashboardScreen(expiringItems)
                }
                currentScreen == AppScreen.Inventory -> {
                    InventoryScreen(inventory, onConsume = { item, type ->
                        viewModel.consumeItem(item.inventoryId, item.itemId, 1.0, type)
                    })
                }
                currentScreen == AppScreen.AddManual -> {
                    AddScreen(onAdd = { name, qty, unit, cat, veg, gf ->
                        viewModel.addItem(name, qty, unit, cat, veg, gf)
                        currentScreen = AppScreen.Inventory
                    })
                }
            }
        }
    }
}

@Composable
fun DashboardScreen(expiringItems: List<InventoryUiModel>) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Dashboard", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        Text("Expiring Soon", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(2),
            verticalItemSpacing = 8.dp,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            content = {
                items(expiringItems) { item ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = item.name, style = MaterialTheme.typography.titleMedium)
                            Text(text = "Qty: ${item.quantity}")
                            Text(text = "Expiring soon!", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                // Add dummy items if empty to show layout
                if (expiringItems.isEmpty()) {
                   item {
                       Card {
                           Column(modifier = Modifier.padding(16.dp)) {
                               Text("No expiring items", style = MaterialTheme.typography.bodyMedium)
                           }
                       }
                   }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanInScreen(onDismiss: () -> Unit, viewModel: MainViewModel) {
    var detectedBarcode by remember { mutableStateOf<String?>(null) }
    var showAddSheet by remember { mutableStateOf(false) }
    var foundItem by remember { mutableStateOf<ItemEntity?>(null) }
    var showManualAdd by remember { mutableStateOf(false) }

    // Logic to handle detection
    LaunchedEffect(detectedBarcode) {
        detectedBarcode?.let { code ->
            val item = viewModel.getItemByBarcode(code)
            if (item != null) {
                foundItem = item
                showAddSheet = true
            } else {
                showManualAdd = true
            }
        }
    }

    if (showManualAdd && detectedBarcode != null) {
        // Navigate to add screen pre-filled
        AddScreen(
            barcode = detectedBarcode,
            onAdd = { name, qty, unit, cat, veg, gf ->
                viewModel.addItem(name, qty, unit, cat, veg, gf, barcode = detectedBarcode)
                onDismiss()
            }
        )
    } else if (showAddSheet) {
        ModalBottomSheet(onDismissRequest = {
            showAddSheet = false
            detectedBarcode = null // Reset scanning
        }) {
            Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                val item = foundItem
                Text("Found: ${item?.name ?: "Unknown"}", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = {
                    // Add +1 quantity using item defaults
                    if (item != null) {
                         viewModel.addItem(
                             item.name,
                             1.0,
                             item.defaultUnit,
                             item.category,
                             item.isVegetarian,
                             item.isGlutenFree,
                             barcode = detectedBarcode
                         )
                    }
                    showAddSheet = false
                    onDismiss()
                }) {
                    Text("Add 1")
                }
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            BarcodeScanner(onBarcodeDetected = { code ->
                if (detectedBarcode == null) {
                    detectedBarcode = code
                }
            })
            Button(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.BottomCenter).padding(32.dp)
            ) {
                Text("Cancel")
            }
        }
    }
}

@Composable
fun ScanOutScreen(onDismiss: () -> Unit, viewModel: MainViewModel) {
    var detectedBarcode by remember { mutableStateOf<String?>(null) }
    var foundInventory by remember { mutableStateOf<List<InventoryWithItemMap>?>(null) }

    LaunchedEffect(detectedBarcode) {
        detectedBarcode?.let { code ->
             val inv = viewModel.getInventoryByBarcode(code)
             if (inv.isNotEmpty()) {
                 foundInventory = inv
             } else {
                 // Item not in inventory or not found
                 // Maybe show toast? For now just reset
                 detectedBarcode = null
             }
        }
    }

    val currentInventory = foundInventory
    if (currentInventory != null) {
        val handleConsume = { type: ConsumptionType ->
            currentInventory.firstOrNull()?.let { item ->
                viewModel.consumeItem(item.inventoryId, item.itemId, 1.0, type)
            }
            onDismiss()
        }

        // Show dialog to consume
        AlertDialog(
            onDismissRequest = {
                foundInventory = null
                detectedBarcode = null
            },
            title = { Text("Consume Item") },
            text = {
                Column {
                    Text("Found ${currentInventory.first().name}")
                    Text("Select action:")
                }
            },
            confirmButton = {
                TextButton(onClick = { handleConsume(ConsumptionType.FINISHED) }) {
                    Text("Consumed")
                }
            },
            dismissButton = {
                 TextButton(onClick = { handleConsume(ConsumptionType.WASTED) }) {
                    Text("Wasted")
                }
            }
        )
    } else {
         Box(modifier = Modifier.fillMaxSize()) {
            BarcodeScanner(onBarcodeDetected = { code ->
                if (detectedBarcode == null) {
                    detectedBarcode = code
                }
            })
            Button(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.BottomCenter).padding(32.dp)
            ) {
                Text("Cancel")
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
fun AddScreen(
    barcode: String? = null,
    onAdd: (String, Double, String, String, Boolean, Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var qty by remember { mutableStateOf("1.0") }
    var unit by remember { mutableStateOf("pcs") }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Add New Item", style = MaterialTheme.typography.titleLarge)
        if (barcode != null) {
             Text("Barcode: $barcode", style = MaterialTheme.typography.bodySmall)
        }

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
