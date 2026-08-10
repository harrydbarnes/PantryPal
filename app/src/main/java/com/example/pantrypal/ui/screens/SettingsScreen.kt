package com.example.pantrypal.ui.screens

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.pantrypal.BuildConfig
import com.example.pantrypal.ui.components.ExpressiveHero
import com.example.pantrypal.ui.components.PantryPalSpacing
import com.example.pantrypal.ui.components.StatusPill
import com.example.pantrypal.util.AppSettings
import com.example.pantrypal.util.AppThemeMode
import com.example.pantrypal.util.ShoppingLocation
import com.example.pantrypal.util.ShoppingReminderTiming
import com.example.pantrypal.util.ShoppingReminderSchedule
import java.time.DayOfWeek

private data class ThemeChoice(
    val mode: AppThemeMode,
    val label: String,
    val icon: ImageVector
)

private val ThemeChoices = listOf(
    ThemeChoice(AppThemeMode.SYSTEM, "System", Icons.Default.AutoMode),
    ThemeChoice(AppThemeMode.LIGHT, "Light", Icons.Default.LightMode),
    ThemeChoice(AppThemeMode.DARK, "Dark", Icons.Default.DarkMode)
)

@Composable
fun SettingsScreen(
    settings: AppSettings,
    hasSeenSettingsIntro: Boolean,
    onSettingsIntroSeen: () -> Unit,
    notificationPermissionGranted: Boolean,
    notificationPermissionRequired: Boolean,
    onThemeModeChange: (AppThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onExpiryRemindersChange: (Boolean) -> Unit,
    onShoppingRemindersChange: (Boolean) -> Unit,
    onShoppingDayChange: (Int) -> Unit,
    onShoppingTimeChange: (Int) -> Unit,
    onShoppingReminderTimingChange: (ShoppingReminderTiming) -> Unit,
    onNearbyShoppingRemindersChange: (Boolean) -> Unit,
    shoppingLocations: List<ShoppingLocation>,
    locationPermissionGranted: Boolean,
    backgroundLocationPermissionGranted: Boolean,
    onRequestLocationPermission: () -> Unit,
    onOpenLocationSettings: () -> Unit,
    onAddShoppingLocation: (String) -> Unit,
    onDeleteShoppingLocation: (ShoppingLocation) -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onLaunchOnboarding: () -> Unit,
    onOpenDataManagement: () -> Unit = {}
) {
    var showSettingsIntro by rememberSaveable { mutableStateOf(!hasSeenSettingsIntro) }

    LaunchedEffect(Unit) {
        onSettingsIntroSeen()
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useTwoColumns = maxWidth >= 720.dp
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .widthIn(max = 1040.dp)
                .verticalScroll(rememberScrollState())
                .padding(
                    start = PantryPalSpacing.sm,
                    end = PantryPalSpacing.sm,
                    top = PantryPalSpacing.xs,
                    bottom = 96.dp
                ),
            verticalArrangement = Arrangement.spacedBy(PantryPalSpacing.md)
        ) {
            if (showSettingsIntro) {
                ExpressiveHero(
                    eyebrow = "Make it yours",
                    title = "PantryPal, set up your way",
                    supportingText = "Choose how the app looks, when it nudges you, and revisit the essentials whenever you like.",
                    icon = Icons.Default.Spa,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    trailingContent = {
                        IconButton(
                            onClick = {
                                showSettingsIntro = false
                                onSettingsIntroSeen()
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Dismiss settings introduction"
                            )
                        }
                    }
                )
            }

            if (useTwoColumns) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(PantryPalSpacing.md),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(PantryPalSpacing.md)
                    ) {
                        AppearanceSettingsCard(
                            settings = settings,
                            onThemeModeChange = onThemeModeChange,
                            onDynamicColorChange = onDynamicColorChange
                        )
                        LocalDataCard(onOpenDataManagement)
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(PantryPalSpacing.md)
                    ) {
                        ReminderSettingsCard(
                            enabled = settings.expiryRemindersEnabled,
                            shoppingRemindersEnabled = settings.shoppingRemindersEnabled,
                            shoppingDayOfWeek = settings.shoppingDayOfWeek,
                            shoppingTimeMinutes = settings.shoppingTimeMinutes,
                            shoppingReminderTiming = settings.shoppingReminderTiming,
                            permissionGranted = notificationPermissionGranted,
                            permissionRequired = notificationPermissionRequired,
                            onEnabledChange = onExpiryRemindersChange,
                            onShoppingRemindersChange = onShoppingRemindersChange,
                            onShoppingDayChange = onShoppingDayChange,
                            onShoppingTimeChange = onShoppingTimeChange,
                            onShoppingReminderTimingChange = onShoppingReminderTimingChange,
                            onOpenNotificationSettings = onOpenNotificationSettings
                        )
                        NearbyShoppingSettingsCard(
                            enabled = settings.nearbyShoppingRemindersEnabled,
                            locations = shoppingLocations,
                            locationPermissionGranted = locationPermissionGranted,
                            backgroundLocationPermissionGranted = backgroundLocationPermissionGranted,
                            onEnabledChange = onNearbyShoppingRemindersChange,
                            onRequestLocationPermission = onRequestLocationPermission,
                            onOpenLocationSettings = onOpenLocationSettings,
                            onAddLocation = onAddShoppingLocation,
                            onDeleteLocation = onDeleteShoppingLocation
                        )
                        LearningCard(onLaunchOnboarding)
                        BuildDetailsCard()
                    }
                }
            } else {
                AppearanceSettingsCard(
                    settings = settings,
                    onThemeModeChange = onThemeModeChange,
                    onDynamicColorChange = onDynamicColorChange
                )
                ReminderSettingsCard(
                    enabled = settings.expiryRemindersEnabled,
                    shoppingRemindersEnabled = settings.shoppingRemindersEnabled,
                    shoppingDayOfWeek = settings.shoppingDayOfWeek,
                    shoppingTimeMinutes = settings.shoppingTimeMinutes,
                    shoppingReminderTiming = settings.shoppingReminderTiming,
                    permissionGranted = notificationPermissionGranted,
                    permissionRequired = notificationPermissionRequired,
                    onEnabledChange = onExpiryRemindersChange,
                    onShoppingRemindersChange = onShoppingRemindersChange,
                    onShoppingDayChange = onShoppingDayChange,
                    onShoppingTimeChange = onShoppingTimeChange,
                    onShoppingReminderTimingChange = onShoppingReminderTimingChange,
                    onOpenNotificationSettings = onOpenNotificationSettings
                )
                NearbyShoppingSettingsCard(
                    enabled = settings.nearbyShoppingRemindersEnabled,
                    locations = shoppingLocations,
                    locationPermissionGranted = locationPermissionGranted,
                    backgroundLocationPermissionGranted = backgroundLocationPermissionGranted,
                    onEnabledChange = onNearbyShoppingRemindersChange,
                    onRequestLocationPermission = onRequestLocationPermission,
                    onOpenLocationSettings = onOpenLocationSettings,
                    onAddLocation = onAddShoppingLocation,
                    onDeleteLocation = onDeleteShoppingLocation
                )
                LearningCard(onLaunchOnboarding)
                LocalDataCard(onOpenDataManagement)
                BuildDetailsCard()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppearanceSettingsCard(
    settings: AppSettings,
    onThemeModeChange: (AppThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit
) {
    val dynamicColorAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    SettingsSectionCard(
        icon = Icons.Default.Palette,
        title = "Appearance",
        supportingText = "Set the mood without losing PantryPal's friendly character."
    ) {
        Text("Theme", style = MaterialTheme.typography.labelLarge)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            ThemeChoices.forEachIndexed { index, choice ->
                SegmentedButton(
                    selected = settings.themeMode == choice.mode,
                    onClick = { onThemeModeChange(choice.mode) },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = ThemeChoices.size
                    ),
                    icon = {
                        Icon(
                            choice.icon,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                ) {
                    Text(choice.label)
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        SettingSwitchRow(
            title = "Dynamic colour",
            supportingText = if (dynamicColorAvailable) {
                "Borrow accent colours from your wallpaper."
            } else {
                "Available on Android 12 and newer."
            },
            checked = settings.dynamicColorEnabled && dynamicColorAvailable,
            enabled = dynamicColorAvailable,
            onCheckedChange = onDynamicColorChange
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderSettingsCard(
    enabled: Boolean,
    shoppingRemindersEnabled: Boolean,
    shoppingDayOfWeek: Int,
    shoppingTimeMinutes: Int,
    shoppingReminderTiming: ShoppingReminderTiming,
    permissionGranted: Boolean,
    permissionRequired: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onShoppingRemindersChange: (Boolean) -> Unit,
    onShoppingDayChange: (Int) -> Unit,
    onShoppingTimeChange: (Int) -> Unit,
    onShoppingReminderTimingChange: (ShoppingReminderTiming) -> Unit,
    onOpenNotificationSettings: () -> Unit
) {
    var dayMenuExpanded by remember { mutableStateOf(false) }
    var timePickerVisible by remember { mutableStateOf(false) }
    val anyReminderEnabled = enabled || shoppingRemindersEnabled
    val permissionNeedsAttention = anyReminderEnabled && permissionRequired && !permissionGranted
    SettingsSectionCard(
        icon = Icons.Default.NotificationsActive,
        title = "Reminders & nudges",
        supportingText = "Helpful check-ins for food that needs attention and shopping trips you already make."
    ) {
        SettingSwitchRow(
            title = "Remind me about expiring food",
            supportingText = if (enabled) {
                "PantryPal checks once a day for items due within two days."
            } else {
                "Background expiry checks are paused."
            },
            checked = enabled,
            onCheckedChange = onEnabledChange
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Text("Shopping nudge", style = MaterialTheme.typography.titleMedium)
        Text(
            "Choose whether the prompt arrives the night before, morning of, or one hour before your usual shop.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SettingSwitchRow(
            title = "Remind me before my usual shop",
            supportingText = if (shoppingRemindersEnabled) {
                "PantryPal will ask if you are still shopping and offer to refresh your list."
            } else {
                "Choose your usual day and time to get an optional pre-shop nudge."
            },
            checked = shoppingRemindersEnabled,
            onCheckedChange = onShoppingRemindersChange
        )
        if (shoppingRemindersEnabled) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PantryPalSpacing.xs)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { dayMenuExpanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(ShoppingReminderSchedule.formatShoppingDay(shoppingDayOfWeek))
                    }
                    DropdownMenu(
                        expanded = dayMenuExpanded,
                        onDismissRequest = { dayMenuExpanded = false }
                    ) {
                        (1..7).forEach { day ->
                            DropdownMenuItem(
                                text = {
                                    Text(ShoppingReminderSchedule.formatShoppingDay(day))
                                },
                                onClick = {
                                    onShoppingDayChange(day)
                                    dayMenuExpanded = false
                                }
                            )
                        }
                    }
                }
                OutlinedButton(
                    onClick = { timePickerVisible = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(ShoppingReminderSchedule.formatShoppingTime(shoppingTimeMinutes))
                }
            }
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                listOf(
                    ShoppingReminderTiming.NIGHT_BEFORE to "Night before",
                    ShoppingReminderTiming.MORNING_OF to "Morning",
                    ShoppingReminderTiming.HOUR_BEFORE to "1 hour before"
                ).forEachIndexed { index, (timing, label) ->
                    SegmentedButton(
                        selected = shoppingReminderTiming == timing,
                        onClick = { onShoppingReminderTimingChange(timing) },
                        shape = SegmentedButtonDefaults.itemShape(index, 3)
                    ) { Text(label) }
                }
            }
            Text(
                "Usual shop: ${ShoppingReminderSchedule.formatShoppingDay(shoppingDayOfWeek)} " +
                    "around ${ShoppingReminderSchedule.formatShoppingTime(shoppingTimeMinutes)}. " +
                    "Nudge: ${shoppingReminderTiming.settingsLabel(shoppingDayOfWeek)}.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Notification access", style = MaterialTheme.typography.titleMedium)
                Text(
                    when {
                        !permissionRequired -> "Ready on this Android version."
                        permissionGranted -> "Allowed by Android."
                        else -> "Permission is currently blocked."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            StatusPill(
                label = when {
                    !anyReminderEnabled -> "Paused"
                    permissionNeedsAttention -> "Action needed"
                    else -> "Ready"
                },
                containerColor = if (permissionNeedsAttention) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    MaterialTheme.colorScheme.secondaryContainer
                },
                contentColor = if (permissionNeedsAttention) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    MaterialTheme.colorScheme.onSecondaryContainer
                }
            )
        }
        if (permissionRequired) {
            OutlinedButton(
                onClick = onOpenNotificationSettings,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Manage Android notifications")
            }
        }
        if (timePickerVisible) {
            val timePickerState = rememberTimePickerState(
                initialHour = shoppingTimeMinutes / 60,
                initialMinute = shoppingTimeMinutes % 60,
                is24Hour = false
            )
            AlertDialog(
                onDismissRequest = { timePickerVisible = false },
                title = { Text("When do you usually shop?") },
                text = { TimePicker(state = timePickerState) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onShoppingTimeChange(timePickerState.hour * 60 + timePickerState.minute)
                            timePickerVisible = false
                        }
                    ) {
                        Text("Save time")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { timePickerVisible = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

private fun ShoppingReminderTiming.settingsLabel(shoppingDayOfWeek: Int): String = when (this) {
    ShoppingReminderTiming.NIGHT_BEFORE ->
        "${ShoppingReminderSchedule.formatShoppingDay(DayOfWeek.of(shoppingDayOfWeek.coerceIn(1, 7)).minus(1).value)} evening"
    ShoppingReminderTiming.MORNING_OF -> "the morning of your shop"
    ShoppingReminderTiming.HOUR_BEFORE -> "one hour before your shop"
}

@Composable
private fun NearbyShoppingSettingsCard(
    enabled: Boolean,
    locations: List<ShoppingLocation>,
    locationPermissionGranted: Boolean,
    backgroundLocationPermissionGranted: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onRequestLocationPermission: () -> Unit,
    onOpenLocationSettings: () -> Unit,
    onAddLocation: (String) -> Unit,
    onDeleteLocation: (ShoppingLocation) -> Unit
) {
    var addLocationDialogVisible by rememberSaveable { mutableStateOf(false) }
    var newLocationName by rememberSaveable { mutableStateOf("") }
    val backgroundPermissionNeeded = enabled &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !backgroundLocationPermissionGranted
    val permissionNeedsAttention = enabled &&
        (!locationPermissionGranted || backgroundPermissionNeeded)

    SettingsSectionCard(
        icon = Icons.Default.LocationOn,
        title = "Nearby shopping nudges",
        supportingText = "Open your list when you linger near one of your saved shopping spots."
    ) {
        SettingSwitchRow(
            title = "Remind me near saved places",
            supportingText = if (enabled) {
                "Only your saved spots are monitored. Background monitoring is opt-in."
            } else {
                "Off by default. PantryPal will not monitor location in the background until you enable it."
            },
            checked = enabled,
            onCheckedChange = onEnabledChange
        )
        Text(
            "This uses Android geofencing instead of continuous location tracking. Alerts can arrive a few minutes after you arrive, which helps preserve battery.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Location access", style = MaterialTheme.typography.titleMedium)
                Text(
                    when {
                        !locationPermissionGranted -> "Needed to save and monitor shopping spots."
                        backgroundPermissionNeeded -> "Allow all the time so reminders work while the app is closed."
                        locations.isEmpty() -> "Add a spot to make this useful."
                        else -> "Ready to watch your saved spots."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            StatusPill(
                label = when {
                    !enabled -> "Off"
                    permissionNeedsAttention -> "Action needed"
                    locations.isEmpty() -> "Add a place"
                    else -> "Ready"
                },
                containerColor = if (permissionNeedsAttention) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    MaterialTheme.colorScheme.secondaryContainer
                },
                contentColor = if (permissionNeedsAttention) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    MaterialTheme.colorScheme.onSecondaryContainer
                }
            )
        }
        if (!locationPermissionGranted) {
            OutlinedButton(
                onClick = onRequestLocationPermission,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = null)
                Spacer(Modifier.width(PantryPalSpacing.xs))
                Text("Allow location access")
            }
        } else if (backgroundPermissionNeeded) {
            OutlinedButton(
                onClick = onOpenLocationSettings,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Allow background location")
            }
        }
        if (locations.isEmpty()) {
            Text(
                "Save a spot while you are there, such as “Supermarket” or “Farm shop”. The default radius is ${ShoppingLocation.DEFAULT_RADIUS_METERS.toInt()} metres.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            locations.forEach { location ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = PantryPalSpacing.xs)
                    ) {
                        Text(location.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Within ${location.radiusMeters.toInt()} metres",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = { onDeleteLocation(location) }
                    ) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "Remove ${location.name}"
                        )
                    }
                }
            }
        }
        OutlinedButton(
            onClick = { addLocationDialogVisible = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(PantryPalSpacing.xs))
            Text("Add current location")
        }
        if (addLocationDialogVisible) {
            AlertDialog(
                onDismissRequest = {
                    addLocationDialogVisible = false
                    newLocationName = ""
                },
                title = { Text("Add a shopping spot") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(PantryPalSpacing.xs)) {
                        Text(
                            "Stand at the place you want to save. PantryPal will use your current location once, then rely on Android geofencing.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        OutlinedTextField(
                            value = newLocationName,
                            onValueChange = { newLocationName = it },
                            label = { Text("Spot name") },
                            placeholder = { Text("Supermarket") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        enabled = newLocationName.trim().isNotEmpty(),
                        onClick = {
                            onAddLocation(newLocationName.trim())
                            newLocationName = ""
                            addLocationDialogVisible = false
                        }
                    ) {
                        Text("Save spot")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            addLocationDialogVisible = false
                            newLocationName = ""
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun LearningCard(onLaunchOnboarding: () -> Unit) {
    SettingsSectionCard(
        icon = Icons.Default.School,
        title = "Learn PantryPal",
        supportingText = "Revisit the five-step guide to scanning, planning, shopping, and privacy.",
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Button(
            onClick = onLaunchOnboarding,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("View walkthrough")
        }
    }
}

@Composable
private fun LocalDataCard(onOpenDataManagement: () -> Unit) {
    SettingsSectionCard(
        icon = Icons.Default.Lock,
        title = "Your data",
        supportingText = "Pantry items, meals, and shopping lists stay on this device."
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "No account or cloud sync",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            StatusPill(label = "Local")
        }
        OutlinedButton(
            onClick = onOpenDataManagement,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.SaveAlt, contentDescription = null)
            Spacer(Modifier.width(PantryPalSpacing.xs))
            Text("Backup and household sharing")
        }
    }
}

@Composable
private fun BuildDetailsCard() {
    SettingsSectionCard(
        icon = Icons.Default.Info,
        title = "About",
        supportingText = "Version and build details for troubleshooting."
    ) {
        StatusPill(
            label = "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
            icon = Icons.Default.Info
        )
        Text(
            "Commit ${BuildConfig.GIT_HASH}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "Built ${BuildConfig.BUILD_DATE}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SettingsSectionCard(
    icon: ImageVector,
    title: String,
    supportingText: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Column(
            modifier = Modifier.padding(PantryPalSpacing.md),
            verticalArrangement = Arrangement.spacedBy(PantryPalSpacing.sm)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PantryPalSpacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = contentColor.copy(alpha = 0.12f),
                    contentColor = contentColor
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.padding(10.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleLarge,
                        color = contentColor
                    )
                    Text(
                        supportingText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = contentColor.copy(alpha = 0.78f)
                    )
                }
            }
            content()
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    supportingText: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange
            )
            .padding(vertical = PantryPalSpacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                }
            )
            Text(
                supportingText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                    alpha = if (enabled) 1f else 0.38f
                )
            )
        }
        Spacer(Modifier.width(PantryPalSpacing.sm))
        Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled
        )
    }
}
