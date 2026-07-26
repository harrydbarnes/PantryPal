package com.example.pantrypal

import android.os.Bundle
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clip
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pantrypal.ui.theme.PantryPalTheme
import com.example.pantrypal.viewmodel.MainViewModel
import com.example.pantrypal.viewmodel.MainViewModelFactory
import com.example.pantrypal.viewmodel.InventoryUiModel
import com.example.pantrypal.data.entity.ConsumptionType
import com.example.pantrypal.ui.BarcodeScanner
import kotlinx.coroutines.launch
import com.example.pantrypal.data.entity.ItemEntity
import com.example.pantrypal.ui.screens.ScanOutScreen
import com.example.pantrypal.ui.screens.SettingsScreen
import com.example.pantrypal.ui.screens.PastItemsScreen
import com.example.pantrypal.ui.screens.AddScreen
import com.example.pantrypal.ui.screens.ShoppingListScreen
import com.example.pantrypal.ui.screens.MealPlanScreen
import com.example.pantrypal.ui.screens.OnboardingScreen
import com.example.pantrypal.ui.components.ExpressiveHero
import com.example.pantrypal.ui.components.FriendlyEmptyState
import com.example.pantrypal.ui.components.PantryPalSpacing
import com.example.pantrypal.ui.components.SectionHeading
import com.example.pantrypal.ui.components.StatusPill
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.ExistingPeriodicWorkPolicy
import java.util.concurrent.TimeUnit
import com.example.pantrypal.util.ExpirationWorker
import com.example.pantrypal.util.AppSettings
import com.example.pantrypal.util.AppThemeMode
import coil.compose.AsyncImage

import androidx.annotation.StringRes

sealed class AppScreen(@StringRes val titleResId: Int) {
    data object Dashboard : AppScreen(R.string.dashboard_title)
    data object Inventory : AppScreen(R.string.inventory_title)
    data object ShoppingList : AppScreen(R.string.shopping_list_title)
    data object AddManual : AppScreen(R.string.add_item_title)
    data object ScanIn : AppScreen(R.string.scan_in_title)
    data object ScanOut : AppScreen(R.string.scan_out_title)
    data object Settings : AppScreen(R.string.settings_title)
    data object PastItems : AppScreen(R.string.past_items_title)
    data object MealPlan : AppScreen(R.string.meal_plan_title)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as PantryPalApplication
        val repository = app.repository
        val viewModelFactory = MainViewModelFactory(repository, app)

        setContent {
            val viewModel: MainViewModel = viewModel(factory = viewModelFactory)
            val appSettings by viewModel.appSettings.collectAsState()
            val systemDarkTheme = isSystemInDarkTheme()
            val darkTheme = when (appSettings.themeMode) {
                AppThemeMode.SYSTEM -> systemDarkTheme
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
            }

            LaunchedEffect(appSettings.expiryRemindersEnabled) {
                updateExpirationWork(this@MainActivity, appSettings.expiryRemindersEnabled)
            }

            PantryPalTheme(
                darkTheme = darkTheme,
                dynamicColor = appSettings.dynamicColorEnabled
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    KitchenApp(viewModel, appSettings)
                }
            }
        }
    }
}

private fun updateExpirationWork(context: Context, enabled: Boolean) {
    val workManager = WorkManager.getInstance(context)
    if (enabled) {
        val workRequest = PeriodicWorkRequestBuilder<ExpirationWorker>(1, TimeUnit.DAYS)
            .build()
        workManager.enqueueUniquePeriodicWork(
            ExpirationWorker.UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    } else {
        workManager.cancelUniqueWork(ExpirationWorker.UNIQUE_WORK_NAME)
    }
}

@Composable
fun KitchenApp(
    viewModel: MainViewModel,
    appSettings: AppSettings
) {
    val inventory by viewModel.inventoryState.collectAsState()
    val expiringItems by viewModel.expiringItemsState.collectAsState()
    val restockSuggestions by viewModel.restockSuggestionsState.collectAsState()
    val hasCompletedOnboarding by viewModel.hasCompletedOnboarding.collectAsState()

    var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.Dashboard) }
    var showOnboarding by rememberSaveable {
        mutableStateOf(!hasCompletedOnboarding)
    }

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
    var notificationPermissionRequested by rememberSaveable {
        mutableStateOf(hasNotificationPermission)
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasNotificationPermission = granted
        }
    )
    val notificationSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = {
            hasNotificationPermission =
                Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
        }
    )

    LaunchedEffect(
        showOnboarding,
        appSettings.expiryRemindersEnabled,
        hasNotificationPermission,
        notificationPermissionRequested
    ) {
        if (
            !showOnboarding &&
            appSettings.expiryRemindersEnabled &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !hasNotificationPermission &&
            !notificationPermissionRequested
        ) {
            notificationPermissionRequested = true
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    if (showOnboarding) {
        OnboardingScreen(
            isReplay = hasCompletedOnboarding,
            onComplete = {
                viewModel.completeOnboarding()
                showOnboarding = false
            }
        )
        return
    }

    var showMenu by remember { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val isWideScreen = configuration.screenWidthDp >= 600
    val primaryScreens = PrimaryNavigationDestinations.map { it.screen }.toSet()

    fun selectScreen(screen: AppScreen) {
        when (screen) {
            AppScreen.ScanIn -> checkCameraPermission { currentScreen = AppScreen.ScanIn }
            AppScreen.ScanOut -> checkCameraPermission { currentScreen = AppScreen.ScanOut }
            else -> currentScreen = screen
        }
    }

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                navigationIcon = {
                    if (currentScreen !in primaryScreens) {
                        IconButton(
                            onClick = { currentScreen = parentScreenFor(currentScreen) }
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Go back")
                        }
                    } else {
                        Surface(
                            modifier = Modifier.padding(start = 8.dp).size(40.dp),
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            Icon(
                                Icons.Default.Eco,
                                contentDescription = "PantryPal",
                                modifier = Modifier.padding(9.dp)
                            )
                        }
                    }
                },
                title = {
                    Text(
                        stringResource(currentScreen.titleResId),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
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
            if (!isWideScreen && currentScreen in primaryScreens) {
                KitchenNavigationBar(
                    currentScreen = currentScreen,
                    onSelect = ::selectScreen
                )
            }
        },
        floatingActionButton = {
            if (currentScreen == AppScreen.Inventory) {
                ExtendedFloatingActionButton(
                    onClick = { currentScreen = AppScreen.AddManual },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Add item") }
                )
            }
        }
    ) { padding ->
        Row(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isWideScreen && currentScreen in primaryScreens) {
                KitchenNavigationRail(
                    currentScreen = currentScreen,
                    onSelect = ::selectScreen
                )
            }
            Box(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                contentAlignment = Alignment.TopCenter
            ) {
                Box(
                    modifier = if (
                        currentScreen == AppScreen.ScanIn ||
                            currentScreen == AppScreen.ScanOut
                    ) {
                        Modifier.fillMaxSize()
                    } else {
                        Modifier.fillMaxHeight().widthIn(max = 1040.dp).fillMaxWidth()
                    }
                ) {
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
                            InventoryScreen(
                                items = inventory,
                                onScanIn = { selectScreen(AppScreen.ScanIn) },
                                onScanOut = { selectScreen(AppScreen.ScanOut) },
                                onConsume = { item, type ->
                                    viewModel.consumeItem(item.inventoryId, item.itemId, 1.0, type)
                                }
                            )
                        }
                        AppScreen.ShoppingList -> ShoppingListScreen(viewModel)
                        AppScreen.MealPlan -> MealPlanScreen(viewModel)
                        AppScreen.AddManual -> {
                            AddScreen(
                                onAdd = { name, qty, unit, cat, veg, gf, exp ->
                                    viewModel.addItem(name, qty, unit, cat, veg, gf, expirationDate = exp)
                                    currentScreen = AppScreen.Inventory
                                },
                                onCancel = { currentScreen = AppScreen.Inventory }
                            )
                        }
                        AppScreen.Settings -> SettingsScreen(
                            settings = appSettings,
                            notificationPermissionGranted = hasNotificationPermission,
                            notificationPermissionRequired =
                                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU,
                            onThemeModeChange = viewModel::setThemeMode,
                            onDynamicColorChange = viewModel::setDynamicColorEnabled,
                            onExpiryRemindersChange = viewModel::setExpiryRemindersEnabled,
                            onOpenNotificationSettings = {
                                notificationSettingsLauncher.launch(
                                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                    }
                                )
                            },
                            onLaunchOnboarding = { showOnboarding = true }
                        )
                        AppScreen.PastItems -> PastItemsScreen(viewModel)
                    }
                }
            }
        }
    }
}

private data class PantryNavigationDestination(
    val screen: AppScreen,
    val label: String,
    val icon: ImageVector
)

private val PrimaryNavigationDestinations = listOf(
    PantryNavigationDestination(AppScreen.Dashboard, "Home", Icons.Default.Home),
    PantryNavigationDestination(AppScreen.Inventory, "Pantry", Icons.Default.Inventory2),
    PantryNavigationDestination(AppScreen.MealPlan, "Plan", Icons.Default.DateRange),
    PantryNavigationDestination(AppScreen.ShoppingList, "Shop", Icons.Default.ShoppingCart)
)

private fun parentScreenFor(screen: AppScreen): AppScreen = when (screen) {
    AppScreen.AddManual,
    AppScreen.ScanIn,
    AppScreen.ScanOut -> AppScreen.Inventory
    else -> AppScreen.Dashboard
}

@Composable
private fun KitchenNavigationBar(
    currentScreen: AppScreen,
    onSelect: (AppScreen) -> Unit
) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        PrimaryNavigationDestinations.forEach { destination ->
            NavigationBarItem(
                icon = { Icon(destination.icon, contentDescription = null) },
                label = { Text(destination.label) },
                selected = currentScreen == destination.screen,
                onClick = { onSelect(destination.screen) },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                    selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    }
}

@Composable
private fun KitchenNavigationRail(
    currentScreen: AppScreen,
    onSelect: (AppScreen) -> Unit
) {
    NavigationRail(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        header = {
            Text(
                "PantryPal",
                modifier = Modifier.padding(vertical = 16.dp),
                style = MaterialTheme.typography.labelLarge
            )
        }
    ) {
        PrimaryNavigationDestinations.forEach { destination ->
            NavigationRailItem(
                icon = { Icon(destination.icon, contentDescription = null) },
                label = { Text(destination.label) },
                selected = currentScreen == destination.screen,
                onClick = { onSelect(destination.screen) },
                colors = NavigationRailItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                    selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(expiringItems: List<InventoryUiModel>, restockSuggestions: List<ItemEntity>, onOpenInventory: () -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(
            start = PantryPalSpacing.sm,
            end = PantryPalSpacing.sm,
            top = PantryPalSpacing.xs,
            bottom = 104.dp
        ),
        verticalArrangement = Arrangement.spacedBy(PantryPalSpacing.sm)
    ) {
        item {
            ExpressiveHero(
                eyebrow = "Your kitchen today",
                title = if (expiringItems.isEmpty()) {
                    "Everything’s looking fresh"
                } else {
                    "${expiringItems.size} item${if (expiringItems.size == 1) "" else "s"} need a little love"
                },
                supportingText = "A quick peek at what to use next and what belongs on the next shop.",
                icon = Icons.Default.Kitchen
            )
        }

        item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusPill(
                    label = "${expiringItems.size} expiring soon",
                    icon = Icons.Default.WarningAmber,
                    containerColor = if (expiringItems.isEmpty()) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.errorContainer
                    },
                    contentColor = if (expiringItems.isEmpty()) {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        MaterialTheme.colorScheme.onErrorContainer
                    }
                )
                StatusPill(
                    label = "${restockSuggestions.size} restock ideas",
                    icon = Icons.Default.Replay
                )
            }
        }

        item {
            SectionHeading(
                title = "Use these next",
                supportingText = "A small nudge to keep good food out of the bin."
            )
        }

        if (expiringItems.isEmpty()) {
            item {
                FriendlyEmptyState(
                    title = "Nothing racing the clock",
                    supportingText = "Your cupboard is comfortably in date. Nicely done.",
                    icon = Icons.Default.CheckCircle
                )
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
                            shape = MaterialTheme.shapes.large,
                            modifier = Modifier.width(176.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.WarningAmber,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(text = item.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = "${item.quantity} left",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = "Plan it into a meal soon",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                 }
            }
        }

        item {
            SectionHeading(
                title = "Restock radar",
                supportingText = "Things you usually keep around but may have run out of."
            )
        }

        if (restockSuggestions.isEmpty()) {
            item {
                FriendlyEmptyState(
                    title = "No gaps spotted yet",
                    supportingText = "PantryPal will learn what you reach for as you use the app.",
                    icon = Icons.Default.AutoAwesome
                )
            }
        } else {
            items(restockSuggestions) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
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
                            Icon(
                                Icons.Default.Replay,
                                contentDescription = null,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = item.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "Looks like this one has run out",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        item {
            Button(
                onClick = onOpenInventory,
                modifier = Modifier.fillMaxWidth().height(72.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(26.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text("Open the pantry", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.weight(1f))
                Icon(Icons.Default.ArrowForward, contentDescription = null)
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
            Surface(
                modifier = Modifier.align(Alignment.TopCenter).padding(16.dp),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.inverseSurface,
                contentColor = MaterialTheme.colorScheme.inverseOnSurface
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Line up a barcode to scan it in", style = MaterialTheme.typography.labelLarge)
                }
            }
            FilledTonalButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.BottomCenter).padding(32.dp)
            ) {
                Text("Cancel")
            }
        }
    }
}

@Composable
fun InventoryScreen(
    items: List<InventoryUiModel>,
    onScanIn: () -> Unit,
    onScanOut: () -> Unit,
    onConsume: (InventoryUiModel, ConsumptionType) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ExpressiveHero(
                eyebrow = "Kitchen cupboard",
                title = "${items.size} pantry item${if (items.size == 1) "" else "s"}, all in one place",
                supportingText = "Finish what you have, spot what’s low, and make every ingredient count.",
                icon = Icons.Default.Inventory2,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PantryPalSpacing.xs)
            ) {
                FilledTonalButton(
                    onClick = onScanIn,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(PantryPalSpacing.xs))
                    Text("Scan in")
                }
                OutlinedButton(
                    onClick = onScanOut,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                    Spacer(Modifier.width(PantryPalSpacing.xs))
                    Text("Scan out")
                }
            }
        }
        if (items.isEmpty()) {
            item {
                FriendlyEmptyState(
                    title = "Your pantry is ready",
                    supportingText = "Scan something in or use the Add item button to start filling the shelves.",
                    icon = Icons.Default.Inventory2
                )
            }
        }
        items(items, key = { it.inventoryId }) { item ->
            InventoryItemRow(item, onConsume)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InventoryItemRow(item: InventoryUiModel, onConsume: (InventoryUiModel, ConsumptionType) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            if (item.imageUrl != null) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = item.name,
                    modifier = Modifier.size(80.dp).clip(MaterialTheme.shapes.medium),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            } else {
                Surface(
                    modifier = Modifier.size(80.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(
                        Icons.Default.Kitchen,
                        contentDescription = null,
                        modifier = Modifier.padding(22.dp)
                    )
                }
            }
            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )
                    if (item.isRestockNeeded) {
                        StatusPill(
                            label = "Low",
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
                Text(
                    text = "${item.quantity} available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (item.tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item.tags.forEach { tag ->
                            StatusPill(label = tag)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = { onConsume(item, ConsumptionType.FINISHED) }) {
                        Text("Finished")
                    }
                    TextButton(
                        onClick = { onConsume(item, ConsumptionType.WASTED) },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Wasted")
                    }
                }
            }
        }
    }
}
