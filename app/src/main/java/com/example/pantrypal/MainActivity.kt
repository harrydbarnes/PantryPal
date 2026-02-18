package com.example.pantrypal

import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.DateRange
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
import com.example.pantrypal.ui.screens.ScanOutScreen
import com.example.pantrypal.ui.screens.SettingsScreen
import com.example.pantrypal.ui.screens.PastItemsScreen
import com.example.pantrypal.ui.screens.AddScreen
import com.example.pantrypal.ui.screens.ShoppingListScreen
import com.example.pantrypal.ui.screens.MealPlanScreen
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.ExistingPeriodicWorkPolicy
import java.util.concurrent.TimeUnit
import com.example.pantrypal.util.ExpirationWorker
import coil.compose.AsyncImage

sealed class AppScreen {
    data object Dashboard : AppScreen()
    data object Inventory : AppScreen()
    data object ShoppingList : AppScreen()
    data object AddManual : AppScreen()
    data object ScanIn : AppScreen()
    data object ScanOut : AppScreen()
    data object Settings : AppScreen()
    data object PastItems : AppScreen()
    data object MealPlan : AppScreen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as PantryPalApplication
        val repository = app.repository
        val viewModelFactory = MainViewModelFactory(repository, app)

        // Schedule background work
        val workRequest = PeriodicWorkRequestBuilder<ExpirationWorker>(1, TimeUnit.DAYS)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "ExpirationCheck",
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )

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
    val restockSuggestions by viewModel.restockSuggestionsState.collectAsState()

    var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.Dashboard) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

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

    var pendingPermissionAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
            if (granted) {
                pendingPermissionAction?.invoke()
                pendingPermissionAction = null
            }
        }
    )

    fun checkCameraPermission(onGranted: () -> Unit) {
        if (hasCameraPermission) {
            onGranted()
        } else {
            pendingPermissionAction = onGranted
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    // Notification Permission (Android 13+)
    var hasNotificationPermission by remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            mutableStateOf(
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            )
        } else {
            mutableStateOf(true) // Always true for older versions
        }
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasNotificationPermission = granted
        }
    )

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!hasNotificationPermission) {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("PantryPal") },
                actions = {
                    IconButton(onClick = { showMenu = !showMenu }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                         DropdownMenuItem(
                            text = { Text("Past Items Log") },
                            onClick = {
                                currentScreen = AppScreen.PastItems
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Settings") },
                            onClick = {
                                currentScreen = AppScreen.Settings
                                showMenu = false
                            }
                        )
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
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
                    icon = { Icon(Icons.Default.DateRange, contentDescription = "Meal Plan") },
                    label = { Text("Meal Plan") },
                    selected = currentScreen == AppScreen.MealPlan,
                    onClick = { currentScreen = AppScreen.MealPlan }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.ShoppingCart, contentDescription = "Shopping") },
                    label = { Text("Shopping") },
                    selected = currentScreen == AppScreen.ShoppingList,
                    onClick = { currentScreen = AppScreen.ShoppingList }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Add, contentDescription = "Scan In") },
                    label = { Text("Scan In") },
                    selected = currentScreen == AppScreen.ScanIn,
                    onClick = {
                        checkCameraPermission { currentScreen = AppScreen.ScanIn }
                    }
                )
                 NavigationBarItem(
                    icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan Out") },
                    label = { Text("Scan Out") },
                    selected = currentScreen == AppScreen.ScanOut,
                    onClick = {
                        checkCameraPermission { currentScreen = AppScreen.ScanOut }
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
            when (currentScreen) {
                AppScreen.ScanIn -> {
                    ScanInScreen(
                        onDismiss = { currentScreen = AppScreen.Inventory },
                        viewModel = viewModel
                    )
                }
                AppScreen.ScanOut -> {
                     ScanOutScreen(
                        onDismiss = { currentScreen = AppScreen.Inventory },
                        onShowSnackbar = { msg ->
                            scope.launch { snackbarHostState.showSnackbar(msg) }
                        },
                        viewModel = viewModel
                    )
                }
                AppScreen.Dashboard -> {
                    DashboardScreen(
                        expiringItems,
                        restockSuggestions,
                        onOpenInventory = { currentScreen = AppScreen.Inventory }
                    )
                }
                AppScreen.Inventory -> {
                    InventoryScreen(inventory, onConsume = { item, type ->
                        viewModel.consumeItem(item.inventoryId, item.itemId, 1.0, type)
                    })
                }
                AppScreen.ShoppingList -> {
                    ShoppingListScreen(viewModel)
                }
                AppScreen.MealPlan -> {
                    MealPlanScreen(viewModel)
                }
                AppScreen.AddManual -> {
                    AddScreen(onAdd = { name, qty, unit, cat, veg, gf, exp ->
                        viewModel.addItem(name, qty, unit, cat, veg, gf, expirationDate = exp)
                        currentScreen = AppScreen.Inventory
                    })
                }
                AppScreen.Settings -> {
                    SettingsScreen()
                }
                AppScreen.PastItems -> {
                    PastItemsScreen(viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(expiringItems: List<InventoryUiModel>, restockSuggestions: List<ItemEntity>, onOpenInventory: () -> Unit) {
    LazyColumn(contentPadding = PaddingValues(16.dp)) {
        item {
            Text("Dashboard", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))

            Text("Expiring Soon", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (expiringItems.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("No expiring items", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        } else {
            item {
                 FlowRow(
                     modifier = Modifier.fillMaxWidth(),
                     horizontalArrangement = Arrangement.spacedBy(8.dp),
                     verticalArrangement = Arrangement.spacedBy(8.dp)
                 ) {
                    expiringItems.forEach { item ->
                         Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            modifier = Modifier.width(160.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(text = item.name, style = MaterialTheme.typography.titleMedium)
                                Text(text = "Qty: ${item.quantity}")
                                Text(text = "Expiring soon!", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                 }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
            Text("Suggested Restock", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (restockSuggestions.isEmpty()) {
            item {
                 Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("No suggestions yet.", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        } else {
            items(restockSuggestions) { item ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = item.name, style = MaterialTheme.typography.titleMedium)
                        Text(text = "Seems you are out of this.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onOpenInventory,
                modifier = Modifier.fillMaxWidth().height(80.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.List, contentDescription = null, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Text("Open Kitchen Cupboard", style = MaterialTheme.typography.headlineSmall)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanInScreen(onDismiss: () -> Unit, viewModel: MainViewModel) {
    var detectedBarcode by remember { mutableStateOf<String?>(null) }
    var showAddSheet by remember { mutableStateOf(false) }
    var foundItem by remember { mutableStateOf<ItemEntity?>(null) }
    var showManualAdd by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    // Logic to handle detection
    LaunchedEffect(detectedBarcode) {
        detectedBarcode?.let { code ->
            isLoading = true
            val item = viewModel.getItemByBarcode(code)
            isLoading = false
            if (item != null) {
                foundItem = item
                showAddSheet = true
            } else {
                showManualAdd = true
            }
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (showManualAdd && detectedBarcode != null) {
        // Navigate to add screen pre-filled
        AddScreen(
            barcode = detectedBarcode,
            onAdd = { name, qty, unit, cat, veg, gf, exp ->
                viewModel.addItem(name, qty, unit, cat, veg, gf, barcode = detectedBarcode, expirationDate = exp)
                onDismiss()
            },
            onCancel = {
                 // Reset state to go back to the scanner view
                 showManualAdd = false
                 detectedBarcode = null
            }
        )
    } else if (showAddSheet) {
        // Check if it is a temporary item from API (itemId == 0)
        val isTempItem = foundItem?.itemId == ItemEntity.TEMP_ID

        if (isTempItem) {
             // Redirect to AddScreen with pre-filled data
             AddScreen(
                barcode = detectedBarcode,
                onAdd = { name, qty, unit, cat, veg, gf, exp ->
                    viewModel.addItem(name, qty, unit, cat, veg, gf, barcode = detectedBarcode, expirationDate = exp, imageUrl = foundItem?.imageUrl)
                    onDismiss()
                },
                onCancel = {
                     showAddSheet = false
                     detectedBarcode = null
                },
                preFillItem = foundItem
            )
        } else {
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
                                 barcode = detectedBarcode,
                                 imageUrl = item.imageUrl
                             )
                        }
                        showAddSheet = false
                        onDismiss()
                    }) {
                        Text("Add 1")
                    }
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
        Row(modifier = Modifier.padding(16.dp)) {
            if (item.imageUrl != null) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = item.name,
                    modifier = Modifier.size(80.dp).padding(end = 16.dp),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.name, style = MaterialTheme.typography.titleMedium)
                Text(text = "Qty: ${item.quantity}")
                if (item.tags.isNotEmpty()) {
                    Text(text = "Tags: ${item.tags.joinToString()}", style = MaterialTheme.typography.bodySmall)
                }
                Spacer(modifier = Modifier.height(8.dp))
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
}
