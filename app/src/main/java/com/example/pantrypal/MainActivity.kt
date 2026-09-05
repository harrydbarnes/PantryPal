package com.example.pantrypal

import android.os.Bundle
import android.Manifest
import android.content.Context
import android.content.ClipData
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.Settings as SettingsIcon
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pantrypal.ui.theme.PantryPalTheme
import com.example.pantrypal.viewmodel.MainViewModel
import com.example.pantrypal.viewmodel.MainViewModelFactory
import com.example.pantrypal.viewmodel.PantryFeaturesViewModel
import com.example.pantrypal.viewmodel.PantryFeaturesViewModelFactory
import com.example.pantrypal.viewmodel.InventoryUiModel
import com.example.pantrypal.data.entity.ConsumptionType
import com.example.pantrypal.ui.BarcodeScanner
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.pantrypal.data.entity.ItemEntity
import com.example.pantrypal.ui.screens.ScanOutScreen
import com.example.pantrypal.ui.screens.SettingsScreen
import com.example.pantrypal.ui.screens.PastItemsScreen
import com.example.pantrypal.ui.screens.AddScreen
import com.example.pantrypal.ui.screens.ShoppingListScreen
import com.example.pantrypal.ui.screens.MealPlanScreen
import com.example.pantrypal.ui.screens.OnboardingScreen
import com.example.pantrypal.ui.screens.DataManagementScreen
import com.example.pantrypal.ui.screens.HouseholdCollaborationScreen
import com.example.pantrypal.ui.screens.ReceiptReviewScreen
import com.example.pantrypal.ui.screens.RecipeScreen
import com.example.pantrypal.ui.screens.ShoppingToolsScreen
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
import com.example.pantrypal.util.ExpiryStatus
import com.example.pantrypal.util.InventorySort
import com.example.pantrypal.util.ShoppingLocation
import com.example.pantrypal.util.ShoppingLocationGeofenceManager
import com.example.pantrypal.util.ShoppingReminderScheduler
import com.example.pantrypal.util.ShoppingReminderWorker
import coil.compose.AsyncImage
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.android.gms.common.moduleinstall.InstallStatusListener
import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest
import com.google.android.gms.common.moduleinstall.ModuleInstallStatusUpdate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

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
    data object Recipes : AppScreen(R.string.recipes_title)
    data object Receipt : AppScreen(R.string.receipt_title)
    data object ShoppingTools : AppScreen(R.string.shopping_tools_title)
    data object DataManagement : AppScreen(R.string.data_management_title)
    data object Household : AppScreen(R.string.household_title)
}

class MainActivity : ComponentActivity() {
    private val sharedRecipeUrl = MutableStateFlow<String?>(null)
    private val shoppingReminderAction = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        sharedRecipeUrl.value = extractSharedRecipeUrl(intent)
        shoppingReminderAction.value = extractShoppingReminderAction(intent)

        val app = application as PantryPalApplication
        val repository = app.repository
        val viewModelFactory = MainViewModelFactory(repository, app)
        val featuresViewModelFactory = PantryFeaturesViewModelFactory(app.featuresRepository, app.householdSync)

        setContent {
            val viewModel: MainViewModel = viewModel(factory = viewModelFactory)
            val featuresViewModel: PantryFeaturesViewModel =
                androidx.lifecycle.viewmodel.compose.viewModel(factory = featuresViewModelFactory)
            val appSettings by viewModel.appSettings.collectAsState()
            val shoppingLocations by viewModel.shoppingLocations.collectAsState()
            val systemDarkTheme = isSystemInDarkTheme()
            val darkTheme = when (appSettings.themeMode) {
                AppThemeMode.SYSTEM -> systemDarkTheme
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
            }

            LaunchedEffect(appSettings.expiryRemindersEnabled) {
                updateExpirationWork(this@MainActivity, appSettings.expiryRemindersEnabled)
            }
            LaunchedEffect(
                appSettings.shoppingRemindersEnabled,
                appSettings.shoppingDayOfWeek,
                appSettings.shoppingTimeMinutes,
                appSettings.shoppingReminderTiming
            ) {
                ShoppingReminderScheduler.update(this@MainActivity, appSettings)
            }
            PantryPalTheme(
                darkTheme = darkTheme,
                dynamicColor = appSettings.dynamicColorEnabled
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    KitchenApp(
                        viewModel = viewModel,
                        featuresViewModel = featuresViewModel,
                        appSettings = appSettings,
                        shoppingLocations = shoppingLocations,
                        incomingSharedRecipeUrl = sharedRecipeUrl,
                        onSharedRecipeUrlConsumed = { sharedRecipeUrl.value = null },
                        incomingShoppingReminderAction = shoppingReminderAction,
                        onShoppingReminderActionConsumed = { shoppingReminderAction.value = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        sharedRecipeUrl.value = extractSharedRecipeUrl(intent)
        shoppingReminderAction.value = extractShoppingReminderAction(intent)
    }
}

private fun extractSharedRecipeUrl(intent: Intent?): String? {
    if (intent?.action != Intent.ACTION_SEND || intent.type != "text/plain") return null
    val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
    return Regex("""https?://\S+""").find(sharedText)?.value?.trimEnd('.', ',', ')')
}

private fun extractShoppingReminderAction(intent: Intent?): String? = when (intent?.action) {
    ShoppingReminderWorker.ACTION_REVIEW_LIST,
    ShoppingReminderWorker.ACTION_UPDATE_LIST,
    ShoppingReminderWorker.ACTION_OPEN_LIST -> intent.action
    else -> null
}

private const val MAX_IMPORT_FILE_CHARS = 10_000_000

private fun java.io.Reader.readTextBounded(maxChars: Int): String {
    val output = StringBuilder(minOf(maxChars, 16_384))
    val buffer = CharArray(8_192)
    while (true) {
        val read = read(buffer)
        if (read < 0) break
        require(output.length + read <= maxChars) { "The selected file is too large." }
        output.append(buffer, 0, read)
    }
    return output.toString()
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
    featuresViewModel: PantryFeaturesViewModel,
    appSettings: AppSettings,
    shoppingLocations: List<ShoppingLocation>,
    incomingSharedRecipeUrl: StateFlow<String?>,
    onSharedRecipeUrlConsumed: () -> Unit,
    incomingShoppingReminderAction: StateFlow<String?>,
    onShoppingReminderActionConsumed: () -> Unit
) {
    val inventory by viewModel.inventoryState.collectAsState()
    val expiringItems by viewModel.expiringItemsState.collectAsState()
    val restockSuggestions by viewModel.restockSuggestionsState.collectAsState()
    val hasCompletedOnboarding by viewModel.hasCompletedOnboarding.collectAsState()
    val hasSeenSettingsIntro by viewModel.hasSeenSettingsIntro.collectAsState()

    var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.Dashboard) }
    var showOnboarding by rememberSaveable {
        mutableStateOf(!hasCompletedOnboarding)
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val locationClient = remember(context) {
        LocationServices.getFusedLocationProviderClient(context)
    }
    var hasLocationPermission by remember {
        mutableStateOf(ShoppingLocationGeofenceManager.hasForegroundPermission(context))
    }
    var hasBackgroundLocationPermission by remember {
        mutableStateOf(ShoppingLocationGeofenceManager.hasRequiredPermissions(context))
    }
    var pendingShoppingLocationName by rememberSaveable { mutableStateOf<String?>(null) }

    fun refreshLocationPermissions() {
        hasLocationPermission = ShoppingLocationGeofenceManager.hasForegroundPermission(context)
        hasBackgroundLocationPermission =
            ShoppingLocationGeofenceManager.hasRequiredPermissions(context)
    }

    LaunchedEffect(
        appSettings.nearbyShoppingRemindersEnabled,
        shoppingLocations,
        hasLocationPermission,
        hasBackgroundLocationPermission
    ) {
        ShoppingLocationGeofenceManager.update(
            context = context,
            enabled = appSettings.nearbyShoppingRemindersEnabled,
            locations = shoppingLocations
        )
    }

    fun captureCurrentLocation(name: String) {
        if (!ShoppingLocationGeofenceManager.hasForegroundPermission(context)) {
            refreshLocationPermissions()
            scope.launch {
                snackbarHostState.showSnackbar("Allow location access before saving a spot.")
            }
            return
        }

        runCatching {
            locationClient.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                CancellationTokenSource().token
            )
                .addOnSuccessListener { location ->
                    if (location == null) {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                "I could not get a location yet. Try again in a moment."
                            )
                        }
                    } else {
                        viewModel.addShoppingLocation(
                            name = name,
                            latitude = location.latitude,
                            longitude = location.longitude
                        )
                        scope.launch {
                            snackbarHostState.showSnackbar("Saved $name as a shopping spot.")
                        }
                    }
                }
                .addOnFailureListener {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            "I could not get your location. Try again in a moment."
                        )
                    }
                }
        }.onFailure {
            refreshLocationPermissions()
            scope.launch {
                snackbarHostState.showSnackbar("Location access is unavailable right now.")
            }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = {
            refreshLocationPermissions()
            pendingShoppingLocationName?.let { name ->
                if (ShoppingLocationGeofenceManager.hasForegroundPermission(context)) {
                    pendingShoppingLocationName = null
                    captureCurrentLocation(name)
                }
            }
        }
    )
    val locationSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { refreshLocationPermissions() }
    )

    fun requestLocationPermission() {
        if (!ShoppingLocationGeofenceManager.hasForegroundPermission(context)) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            refreshLocationPermissions()
        }
    }

    fun openLocationSettings() {
        locationSettingsLauncher.launch(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        )
    }

    val incomingRecipeUrl by incomingSharedRecipeUrl.collectAsState()
    LaunchedEffect(incomingRecipeUrl) {
        incomingRecipeUrl?.let { url ->
            currentScreen = AppScreen.Recipes
            featuresViewModel.importRecipeUrl(url)
            onSharedRecipeUrlConsumed()
        }
    }

    val incomingReminderAction by incomingShoppingReminderAction.collectAsState()
    LaunchedEffect(incomingReminderAction) {
        incomingReminderAction?.let { action ->
            currentScreen = AppScreen.ShoppingList
            onShoppingReminderActionConsumed()
        }
    }

    var showReceiptPasteDialog by rememberSaveable { mutableStateOf(false) }
    var pastedReceiptText by rememberSaveable { mutableStateOf("") }
    var pendingBackupRestore by remember { mutableStateOf<String?>(null) }
    var pendingHouseholdRestore by remember { mutableStateOf<String?>(null) }

    val textRecognizer = remember {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }
    DisposableEffect(textRecognizer) {
        onDispose { textRecognizer.close() }
    }
    val receiptImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            featuresViewModel.setReceiptProcessing(true)
            runCatching { InputImage.fromFilePath(context, uri) }
                .onSuccess { image ->
                    prepareReceiptTextRecognition(
                        context = context,
                        recognizer = textRecognizer,
                        onReady = {
                            textRecognizer.process(image)
                                .addOnSuccessListener { result ->
                                    featuresViewModel.parseReceiptText(result.text)
                                }
                                .addOnFailureListener(featuresViewModel::setReceiptError)
                        },
                        onFailure = {
                            featuresViewModel.setReceiptError(
                                "Receipt scanning needs Google Play services to finish downloading its text reader. Connect to the internet, then try again."
                            )
                        }
                    )
                }
                .onFailure(featuresViewModel::setReceiptError)
        }
    }
    val backupCreateLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                featuresViewModel.createBackupJson().onSuccess { json ->
                    runCatching {
                        withContext(Dispatchers.IO) {
                            context.contentResolver.openOutputStream(uri)?.bufferedWriter().use {
                                requireNotNull(it) { "The selected file could not be opened." }
                                    .write(json)
                            }
                        }
                    }.onFailure {
                        snackbarHostState.showSnackbar("Backup file could not be written.")
                    }
                }
            }
        }
    }
    val backupOpenLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.bufferedReader().use {
                            requireNotNull(it) { "The selected file could not be opened." }
                                .readTextBounded(MAX_IMPORT_FILE_CHARS)
                        }
                    }
                }.onSuccess {
                    pendingBackupRestore = it
                }.onFailure {
                    snackbarHostState.showSnackbar("Backup file could not be read.")
                }
            }
        }
    }
    val householdOpenLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.bufferedReader().use {
                            requireNotNull(it) { "The selected file could not be opened." }
                                .readTextBounded(MAX_IMPORT_FILE_CHARS)
                        }
                    }
                }.onSuccess {
                    pendingHouseholdRestore = it
                }.onFailure {
                    snackbarHostState.showSnackbar("Household file could not be read.")
                }
            }
        }
    }

    // Permission handling
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

    fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    if (showOnboarding) {
        OnboardingScreen(
            isReplay = hasCompletedOnboarding,
            initialShoppingDay = appSettings.shoppingDayOfWeek,
            initialShoppingTimeMinutes = appSettings.shoppingTimeMinutes,
            initialShoppingReminderTiming = appSettings.shoppingReminderTiming,
            onSaveShoppingRoutine = { day, time, timing, remindersEnabled ->
                viewModel.setShoppingReminderDay(day)
                viewModel.setShoppingReminderTime(time)
                viewModel.setShoppingReminderTiming(timing)
                viewModel.setShoppingRemindersEnabled(remindersEnabled)
            },
            onSaveRegulars = { regulars ->
                viewModel.addOnboardingRegulars(regulars)
            },
            onSaveMeals = { meals ->
                viewModel.addOnboardingMeals(meals.map { it.name to it.dayOfWeek })
            },
            onSaveShoppingSpot = { name ->
                viewModel.setNearbyShoppingRemindersEnabled(true)
                pendingShoppingLocationName = name
                if (ShoppingLocationGeofenceManager.hasForegroundPermission(context)) {
                    pendingShoppingLocationName = null
                    captureCurrentLocation(name)
                } else {
                    requestLocationPermission()
                }
            },
            onCompleteToMealPlan = { requestNotificationPermission ->
                if (requestNotificationPermission) requestNotificationPermissionIfNeeded()
                viewModel.completeOnboarding()
                showOnboarding = false
                if (!hasCompletedOnboarding) {
                    currentScreen = AppScreen.MealPlan
                }
            },
            onSkip = {
                viewModel.completeOnboarding()
                showOnboarding = false
            }
        )
        return
    }

    val configuration = LocalConfiguration.current
    val isWideScreen = configuration.screenWidthDp >= 600
    val primaryScreens = PrimaryNavigationDestinations.map { it.screen }.toSet()

    BackHandler(enabled = currentScreen !in primaryScreens) {
        currentScreen = parentScreenFor(currentScreen)
    }

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
                    }
                },
                title = {
                    Text(
                        stringResource(currentScreen.titleResId),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    if (currentScreen == AppScreen.Inventory) {
                        IconButton(onClick = { currentScreen = AppScreen.PastItems }) {
                            Icon(
                                Icons.Default.History,
                                contentDescription = "Open past items log"
                            )
                        }
                    }
                    IconButton(onClick = { currentScreen = AppScreen.Settings }) {
                        Icon(
                            Icons.Default.SettingsIcon,
                            contentDescription = "Open settings"
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
                                onOpenInventory = { currentScreen = AppScreen.Inventory },
                                onAddRestock = viewModel::addRestockToShopping
                            )
                        }
                        AppScreen.Inventory -> {
                            InventoryScreen(
                                items = inventory,
                                onScanIn = { selectScreen(AppScreen.ScanIn) },
                                onScanOut = { selectScreen(AppScreen.ScanOut) },
                                onConsume = { item, type ->
                                    viewModel.consumeItem(item.inventoryId, item.itemId, 1.0, type)
                                },
                                onAdjustQuantity = viewModel::adjustInventoryQuantity,
                                onToggleOpened = viewModel::toggleInventoryOpened,
                                onUpdateStockSettings = viewModel::updateStockSettings,
                                onUpdateLocation = viewModel::updateInventoryLocation
                            )
                        }
                        AppScreen.ShoppingList -> ShoppingListScreen(
                            viewModel = viewModel,
                            onScanReceipt = { currentScreen = AppScreen.Receipt },
                            onOpenShoppingTools = {
                                currentScreen = AppScreen.ShoppingTools
                            },
                            onOpenHousehold = { currentScreen = AppScreen.Household }
                        )
                        AppScreen.MealPlan -> MealPlanScreen(
                            viewModel = viewModel,
                            onOpenRecipes = { currentScreen = AppScreen.Recipes }
                        )
                        AppScreen.AddManual -> {
                            val addItemDefaults by viewModel.addItemDefaults.collectAsState()
                            AddScreen(
                                defaults = addItemDefaults,
                                onAdd = { name, qty, unit, cat, veg, gf, exp, usual, threshold, location, opened ->
                                    viewModel.addItem(
                                        name,
                                        qty,
                                        unit,
                                        cat,
                                        veg,
                                        gf,
                                        expirationDate = exp,
                                        isUsual = usual,
                                        lowStockThreshold = threshold,
                                        storageLocation = location,
                                        isOpened = opened
                                    )
                                    currentScreen = AppScreen.Inventory
                                },
                                onCancel = { currentScreen = AppScreen.Inventory }
                            )
                        }
                        AppScreen.Settings -> SettingsScreen(
                            settings = appSettings,
                            hasSeenSettingsIntro = hasSeenSettingsIntro,
                            onSettingsIntroSeen = viewModel::markSettingsIntroSeen,
                            notificationPermissionGranted = hasNotificationPermission,
                            notificationPermissionRequired =
                                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU,
                            onThemeModeChange = viewModel::setThemeMode,
                            onDynamicColorChange = viewModel::setDynamicColorEnabled,
                            onExpiryRemindersChange = { enabled ->
                                viewModel.setExpiryRemindersEnabled(enabled)
                                if (enabled) requestNotificationPermissionIfNeeded()
                            },
                            onShoppingRemindersChange = { enabled ->
                                viewModel.setShoppingRemindersEnabled(enabled)
                                if (enabled) requestNotificationPermissionIfNeeded()
                            },
                            onShoppingDayChange = viewModel::setShoppingReminderDay,
                            onShoppingTimeChange = viewModel::setShoppingReminderTime,
                            onShoppingReminderTimingChange = viewModel::setShoppingReminderTiming,
                            onNearbyShoppingRemindersChange = { enabled ->
                                viewModel.setNearbyShoppingRemindersEnabled(enabled)
                                if (enabled) {
                                    requestLocationPermission()
                                }
                            },
                            shoppingLocations = shoppingLocations,
                            locationPermissionGranted = hasLocationPermission,
                            backgroundLocationPermissionGranted =
                                hasBackgroundLocationPermission,
                            onRequestLocationPermission = ::requestLocationPermission,
                            onOpenLocationSettings = ::openLocationSettings,
                            onAddShoppingLocation = { name ->
                                pendingShoppingLocationName = name
                                if (ShoppingLocationGeofenceManager.hasForegroundPermission(context)) {
                                    pendingShoppingLocationName = null
                                    captureCurrentLocation(name)
                                } else {
                                    requestLocationPermission()
                                }
                            },
                            onDeleteShoppingLocation = viewModel::deleteShoppingLocation,
                            onOpenNotificationSettings = {
                                notificationSettingsLauncher.launch(
                                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                    }
                                )
                            },
                            onLaunchOnboarding = { showOnboarding = true },
                            onOpenDataManagement = {
                                currentScreen = AppScreen.DataManagement
                            }
                        )
                        AppScreen.Recipes -> {
                            val state by featuresViewModel.recipeState.collectAsState()
                            val currentWeek by viewModel.currentWeek.collectAsState()
                            RecipeScreen(
                                state = state,
                                onSearchQueryChange = featuresViewModel::setRecipeQuery,
                                onExternalSearch = featuresViewModel::searchRecipesOnline,
                                onImportUrl = featuresViewModel::importRecipeUrl,
                                onRecipeSelected = featuresViewModel::selectRecipe,
                                onRecipeDismissed = featuresViewModel::dismissRecipe,
                                onImportPreviewDismissed =
                                    featuresViewModel::dismissImportPreview,
                                onSaveRecipe = featuresViewModel::saveRecipe,
                                onToggleFavourite = featuresViewModel::setFavourite,
                                onRateRecipe = featuresViewModel::rateRecipe,
                                onMarkCooked = featuresViewModel::markRecipeCooked,
                                onOpenSource = { url ->
                                    runCatching {
                                        context.startActivity(
                                            Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                        )
                                    }.onFailure {
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                "No app could open this recipe source."
                                            )
                                        }
                                    }
                                },
                                onAddToPlan = {
                                    featuresViewModel.addRecipeToPlan(it, currentWeek)
                                },
                                onAddMissingToShopping = { recipe, ingredients ->
                                    featuresViewModel.addMissingToShopping(
                                        recipe,
                                        ingredients,
                                        currentWeek
                                    )
                                }
                            )
                        }
                        AppScreen.Receipt -> {
                            val result by featuresViewModel.receiptResult.collectAsState()
                            val isProcessing by
                                featuresViewModel.receiptProcessing.collectAsState()
                            val receiptError by featuresViewModel.receiptError.collectAsState()
                            ReceiptReviewScreen(
                                result = result,
                                isProcessing = isProcessing,
                                errorMessage = receiptError,
                                onCaptureReceipt = {
                                    receiptImageLauncher.launch("image/*")
                                },
                                onPasteReceiptText = {
                                    showReceiptPasteDialog = true
                                },
                                onCandidateChange =
                                    featuresViewModel::updateReceiptCandidate,
                                onImportSelected = { candidates ->
                                    featuresViewModel.importReceipt(candidates)
                                    currentScreen = AppScreen.ShoppingTools
                                }
                            )
                        }
                        AppScreen.ShoppingTools -> {
                            val state by
                                featuresViewModel.shoppingToolsState.collectAsState()
                            ShoppingToolsScreen(
                                state = state,
                                onSetBudgetMinor = featuresViewModel::setWeeklyBudget,
                                onScanReceipt = { currentScreen = AppScreen.Receipt }
                            )
                        }
                        AppScreen.DataManagement -> {
                            val state by featuresViewModel.dataState.collectAsState()
                            DataManagementScreen(
                                state = state,
                                onExportBackup = {
                                    backupCreateLauncher.launch(
                                        "PantryPal-backup-${java.time.LocalDate.now()}.json"
                                    )
                                },
                                onImportBackup = {
                                    backupOpenLauncher.launch(
                                        arrayOf("application/json", "text/plain")
                                    )
                                },
                                onOpenHousehold = {
                                    currentScreen = AppScreen.Household
                                }
                            )
                        }
                        AppScreen.Household -> {
                            val state by featuresViewModel.householdState.collectAsState()
                            HouseholdCollaborationScreen(
                                state = state,
                                onShareSnapshot = {
                                    scope.launch {
                                        featuresViewModel.createHouseholdJson().onSuccess { json ->
                                            runCatching {
                                                val snapshotUri = withContext(Dispatchers.IO) {
                                                    writeHouseholdSnapshot(context, json)
                                                }
                                                shareHouseholdSnapshot(context, snapshotUri)
                                            }
                                                .onFailure { snackbarHostState.showSnackbar("Household copy could not be shared.") }
                                        }
                                    }
                                },
                                onImportSnapshot = {
                                    householdOpenLauncher.launch(
                                        arrayOf("application/json", "text/plain")
                                    )
                                },
                                onGoogleSignIn = { featuresViewModel.signInToHousehold(this@MainActivity) },
                                onCreateLiveHousehold = featuresViewModel::createLiveHousehold,
                                onJoinLiveHousehold = featuresViewModel::joinLiveHousehold
                            )
                        }
                        AppScreen.PastItems -> PastItemsScreen(viewModel)
                    }
                }
            }
        }
    }

    if (showReceiptPasteDialog) {
        AlertDialog(
            onDismissRequest = { showReceiptPasteDialog = false },
            title = { Text("Paste receipt text") },
            text = {
                OutlinedTextField(
                    value = pastedReceiptText,
                    onValueChange = { pastedReceiptText = it },
                    label = { Text("Receipt text") },
                    minLines = 8,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        featuresViewModel.parseReceiptText(pastedReceiptText)
                        pastedReceiptText = ""
                        showReceiptPasteDialog = false
                        currentScreen = AppScreen.Receipt
                    },
                    enabled = pastedReceiptText.isNotBlank()
                ) {
                    Text("Review items")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReceiptPasteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    pendingBackupRestore?.let { json ->
        AlertDialog(
            onDismissRequest = { pendingBackupRestore = null },
            title = { Text("Replace this kitchen?") },
            text = {
                Text(
                    "PantryPal will validate the backup, then replace pantry, shopping, plans, recipes and settings on this device."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        featuresViewModel.restoreBackupJson(json)
                        pendingBackupRestore = null
                    }
                ) {
                    Text("Restore backup")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingBackupRestore = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    pendingHouseholdRestore?.let { json ->
        AlertDialog(
            onDismissRequest = { pendingHouseholdRestore = null },
            title = { Text("Import household snapshot?") },
            text = {
                Text(
                    "This checks the snapshot for damage, then replaces local kitchen data with the shared household copy."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        featuresViewModel.restoreHouseholdJson(json)
                        pendingHouseholdRestore = null
                    }
                ) {
                    Text("Import snapshot")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingHouseholdRestore = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun writeHouseholdSnapshot(context: Context, json: String): Uri {
    val shareDirectory = File(context.cacheDir, "household-shares").apply { mkdirs() }
    val snapshotFile = File(shareDirectory, "PantryPal-household-${java.time.LocalDate.now()}.json")
    snapshotFile.writeText(json)
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", snapshotFile)
}

private fun shareHouseholdSnapshot(context: Context, snapshotUri: Uri) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/json"
        putExtra(Intent.EXTRA_STREAM, snapshotUri)
        putExtra(Intent.EXTRA_TITLE, "PantryPal setup copy")
        clipData = ClipData.newRawUri("PantryPal setup copy", snapshotUri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Send PantryPal setup copy"))
}

private fun prepareReceiptTextRecognition(
    context: Context,
    recognizer: TextRecognizer,
    onReady: () -> Unit,
    onFailure: (Throwable) -> Unit
) {
    val installClient = ModuleInstall.getClient(context)
    installClient.areModulesAvailable(recognizer)
        .addOnSuccessListener { availability ->
            if (availability.areModulesAvailable()) {
                onReady()
                return@addOnSuccessListener
            }

            lateinit var listener: InstallStatusListener
            listener = InstallStatusListener { update ->
                when (update.installState) {
                    ModuleInstallStatusUpdate.InstallState.STATE_COMPLETED -> {
                        installClient.unregisterListener(listener)
                        onReady()
                    }
                    ModuleInstallStatusUpdate.InstallState.STATE_CANCELED,
                    ModuleInstallStatusUpdate.InstallState.STATE_FAILED -> {
                        installClient.unregisterListener(listener)
                        onFailure(IllegalStateException("Receipt text reader download did not complete."))
                    }
                }
            }
            val request = ModuleInstallRequest.newBuilder()
                .addApi(recognizer)
                .setListener(listener)
                .build()
            installClient.installModules(request)
                .addOnFailureListener { error ->
                    installClient.unregisterListener(listener)
                    onFailure(error)
                }
        }
        .addOnFailureListener(onFailure)
}

internal data class PantryNavigationDestination(
    val screen: AppScreen,
    val label: String,
    val icon: ImageVector
)

internal val PrimaryNavigationDestinations = listOf(
    PantryNavigationDestination(AppScreen.Dashboard, "Home", Icons.Default.Home),
    PantryNavigationDestination(AppScreen.Inventory, "Pantry", Icons.Default.Inventory2),
    PantryNavigationDestination(AppScreen.MealPlan, "Plan", Icons.Default.DateRange),
    PantryNavigationDestination(AppScreen.ShoppingList, "Shop", Icons.Default.ShoppingCart)
)

internal fun parentScreenFor(screen: AppScreen): AppScreen = when (screen) {
    AppScreen.AddManual,
    AppScreen.ScanIn,
    AppScreen.ScanOut -> AppScreen.Inventory
    AppScreen.Recipes -> AppScreen.MealPlan
    AppScreen.Receipt,
    AppScreen.ShoppingTools -> AppScreen.ShoppingList
    AppScreen.DataManagement,
    AppScreen.Household -> AppScreen.Settings
    AppScreen.PastItems -> AppScreen.Inventory
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
fun DashboardScreen(
    expiringItems: List<InventoryUiModel>,
    restockSuggestions: List<ItemEntity>,
    onOpenInventory: () -> Unit,
    onAddRestock: (ItemEntity) -> Unit
) {
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
                                    text = item.expiryLabel,
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
                        FilledTonalButton(onClick = { onAddRestock(item) }) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Add")
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
    val addItemDefaults by viewModel.addItemDefaults.collectAsState()

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
            defaults = addItemDefaults,
            barcode = detectedBarcode,
            onAdd = { name, qty, unit, cat, veg, gf, exp, usual, threshold, location, opened ->
                viewModel.addItem(
                    name,
                    qty,
                    unit,
                    cat,
                    veg,
                    gf,
                    barcode = detectedBarcode,
                    expirationDate = exp,
                    isUsual = usual,
                    lowStockThreshold = threshold,
                    storageLocation = location,
                    isOpened = opened
                )
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
                defaults = addItemDefaults,
                barcode = detectedBarcode,
                onAdd = { name, qty, unit, cat, veg, gf, exp, usual, threshold, location, opened ->
                    viewModel.addItem(
                        name,
                        qty,
                        unit,
                        cat,
                        veg,
                        gf,
                        barcode = detectedBarcode,
                        expirationDate = exp,
                        imageUrl = foundItem?.imageUrl,
                        isUsual = usual,
                        lowStockThreshold = threshold,
                        storageLocation = location,
                        isOpened = opened
                    )
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun InventoryScreen(
    items: List<InventoryUiModel>,
    onScanIn: () -> Unit,
    onScanOut: () -> Unit,
    onConsume: (InventoryUiModel, ConsumptionType) -> Unit,
    onAdjustQuantity: (Long, Double) -> Unit,
    onToggleOpened: (InventoryUiModel) -> Unit,
    onUpdateStockSettings: (Long, Boolean, Double?) -> Unit,
    onUpdateLocation: (InventoryUiModel, String) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf<String?>(null) }
    var location by rememberSaveable { mutableStateOf<String?>(null) }
    var expiryFilter by rememberSaveable { mutableStateOf<ExpiryStatus?>(null) }
    var lowStockOnly by rememberSaveable { mutableStateOf(false) }
    var openedOnly by rememberSaveable { mutableStateOf(false) }
    var sort by rememberSaveable { mutableStateOf(InventorySort.NAME) }
    var showCategoryMenu by remember { mutableStateOf(false) }
    var showLocationMenu by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var editingStockItem by remember { mutableStateOf<InventoryUiModel?>(null) }

    val filteredItems = remember(items, query, category, location, expiryFilter, lowStockOnly, openedOnly, sort) {
        val filtered = items.asSequence()
            .filter {
                query.isBlank() ||
                    it.name.contains(query, ignoreCase = true) ||
                    it.category.contains(query, ignoreCase = true)
            }
            .filter { category == null || it.category == category }
            .filter { location == null || it.storageLocation == location }
            .filter {
                expiryFilter == null ||
                    it.expiryStatus == expiryFilter ||
                    (expiryFilter == ExpiryStatus.DUE_SOON && it.expiryStatus == ExpiryStatus.TODAY)
            }
            .filter { !lowStockOnly || it.isRestockNeeded }
            .filter { !openedOnly || it.isOpened }
        when (sort) {
            InventorySort.NAME -> filtered.sortedBy { it.name.lowercase() }.toList()
            InventorySort.EXPIRY -> filtered.sortedWith(compareBy<InventoryUiModel> { it.expirationDate == null }.thenBy { it.expirationDate }).toList()
            InventorySort.RECENTLY_ADDED -> filtered.sortedByDescending { it.addedDate }.toList()
            InventorySort.CATEGORY -> filtered.sortedWith(compareBy<InventoryUiModel> { it.category }.thenBy { it.name }).toList()
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ExpressiveHero(
                eyebrow = "Kitchen cupboard",
                title = "${filteredItems.size} of ${items.size} pantry item${if (items.size == 1) "" else "s"}",
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
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                label = { Text("Search pantry") },
                placeholder = { Text("Name or category") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
        item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box {
                    FilterChip(
                        selected = category != null,
                        onClick = { showCategoryMenu = true },
                        label = { Text(category ?: "Category") }
                    )
                    DropdownMenu(expanded = showCategoryMenu, onDismissRequest = { showCategoryMenu = false }) {
                        DropdownMenuItem(text = { Text("All categories") }, onClick = { category = null; showCategoryMenu = false })
                        items.map { it.category }.distinct().sorted().forEach { value ->
                            DropdownMenuItem(text = { Text(value) }, onClick = { category = value; showCategoryMenu = false })
                        }
                    }
                }
                Box {
                    FilterChip(
                        selected = location != null,
                        onClick = { showLocationMenu = true },
                        label = { Text(location ?: "Location") }
                    )
                    DropdownMenu(expanded = showLocationMenu, onDismissRequest = { showLocationMenu = false }) {
                        DropdownMenuItem(text = { Text("All locations") }, onClick = { location = null; showLocationMenu = false })
                        items.map { it.storageLocation }.distinct().sorted().forEach { value ->
                            DropdownMenuItem(text = { Text(value) }, onClick = { location = value; showLocationMenu = false })
                        }
                    }
                }
                FilterChip(
                    selected = expiryFilter == ExpiryStatus.EXPIRED,
                    onClick = { expiryFilter = if (expiryFilter == ExpiryStatus.EXPIRED) null else ExpiryStatus.EXPIRED },
                    label = { Text("Expired") }
                )
                FilterChip(
                    selected = expiryFilter == ExpiryStatus.DUE_SOON || expiryFilter == ExpiryStatus.TODAY,
                    onClick = { expiryFilter = if (expiryFilter == ExpiryStatus.DUE_SOON) null else ExpiryStatus.DUE_SOON },
                    label = { Text("Due soon") }
                )
                FilterChip(selected = lowStockOnly, onClick = { lowStockOnly = !lowStockOnly }, label = { Text("Low stock") })
                FilterChip(selected = openedOnly, onClick = { openedOnly = !openedOnly }, label = { Text("Opened") })
                Box {
                    AssistChip(onClick = { showSortMenu = true }, label = { Text("Sort: ${sort.name.lowercase().replace('_', ' ')}") })
                    DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                        InventorySort.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.name.lowercase().replace('_', ' ').replaceFirstChar { it.titlecase() }) },
                                onClick = { sort = option; showSortMenu = false }
                            )
                        }
                    }
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
        } else if (filteredItems.isEmpty()) {
            item {
                FriendlyEmptyState(
                    title = "Nothing matches those filters",
                    supportingText = "Try a broader search or clear one of the filter chips.",
                    icon = Icons.Default.Search
                )
            }
        }
        items(filteredItems, key = { it.inventoryId }) { item ->
            InventoryItemRow(
                item = item,
                onConsume = onConsume,
                onAdjustQuantity = onAdjustQuantity,
                onToggleOpened = onToggleOpened,
                onEditStockSettings = { editingStockItem = item }
            )
        }
    }

    editingStockItem?.let { item ->
        var alwaysStocked by remember(item) { mutableStateOf(item.isUsual) }
        var thresholdText by remember(item) { mutableStateOf(item.lowStockThreshold?.toString() ?: "1") }
        var selectedLocation by remember(item) { mutableStateOf(item.storageLocation) }
        AlertDialog(
            onDismissRequest = { editingStockItem = null },
            title = { Text("Restock settings") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Always keep ${item.name} stocked", modifier = Modifier.weight(1f))
                        Switch(checked = alwaysStocked, onCheckedChange = { alwaysStocked = it })
                    }
                    if (alwaysStocked) {
                        OutlinedTextField(
                            value = thresholdText,
                            onValueChange = { thresholdText = it },
                            label = { Text("Low-stock threshold") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    Text("Storage location", style = MaterialTheme.typography.labelLarge)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        com.example.pantrypal.data.entity.InventoryEntity.STORAGE_LOCATIONS.forEach { option ->
                            FilterChip(
                                selected = selectedLocation == option,
                                onClick = { selectedLocation = option },
                                label = { Text(option) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    onUpdateStockSettings(
                        item.itemId,
                        alwaysStocked,
                        if (alwaysStocked) thresholdText.toDoubleOrNull() else null
                    )
                    onUpdateLocation(item, selectedLocation)
                    editingStockItem = null
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { editingStockItem = null }) { Text("Cancel") } }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InventoryItemRow(
    item: InventoryUiModel,
    onConsume: (InventoryUiModel, ConsumptionType) -> Unit,
    onAdjustQuantity: (Long, Double) -> Unit,
    onToggleOpened: (InventoryUiModel) -> Unit,
    onEditStockSettings: (InventoryUiModel) -> Unit
) {
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
                    text = "${item.quantity} · ${item.category} · ${item.storageLocation}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (item.expirationDate != null) {
                    Text(
                        text = item.expiryLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (item.expiryStatus == ExpiryStatus.EXPIRED) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
                if (item.tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item.tags.forEach { tag ->
                            StatusPill(label = tag)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onAdjustQuantity(item.inventoryId, -1.0.coerceAtMost(item.quantityValue)) },
                        enabled = item.quantityValue > 0
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Reduce ${item.name} by one")
                    }
                    Text(item.quantity, style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = { onAdjustQuantity(item.inventoryId, 1.0) }) {
                        Icon(Icons.Default.Add, contentDescription = "Add one ${item.name}")
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = item.isOpened,
                        onClick = { onToggleOpened(item) },
                        label = { Text(if (item.isOpened) "Opened" else "Unopened") }
                    )
                    AssistChip(
                        onClick = { onEditStockSettings(item) },
                        label = { Text(if (item.isUsual) "Restock at ${item.lowStockThreshold ?: 0}" else "Restock settings") }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = { onConsume(item, ConsumptionType.FINISHED) }) {
                        Text(if (item.quantityValue <= 1.0) "Finished" else "Use 1")
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
