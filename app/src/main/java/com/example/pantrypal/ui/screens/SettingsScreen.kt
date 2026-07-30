package com.example.pantrypal.ui.screens

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    notificationPermissionGranted: Boolean,
    notificationPermissionRequired: Boolean,
    onThemeModeChange: (AppThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onExpiryRemindersChange: (Boolean) -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onLaunchOnboarding: () -> Unit,
    onOpenDataManagement: () -> Unit = {}
) {
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
            ExpressiveHero(
                eyebrow = "Make it yours",
                title = "PantryPal, set up your way",
                supportingText = "Choose how the app looks, when it nudges you, and revisit the essentials whenever you like.",
                icon = Icons.Default.Spa,
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
            )

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
                            permissionGranted = notificationPermissionGranted,
                            permissionRequired = notificationPermissionRequired,
                            onEnabledChange = onExpiryRemindersChange,
                            onOpenNotificationSettings = onOpenNotificationSettings
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
                    permissionGranted = notificationPermissionGranted,
                    permissionRequired = notificationPermissionRequired,
                    onEnabledChange = onExpiryRemindersChange,
                    onOpenNotificationSettings = onOpenNotificationSettings
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

@Composable
private fun ReminderSettingsCard(
    enabled: Boolean,
    permissionGranted: Boolean,
    permissionRequired: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onOpenNotificationSettings: () -> Unit
) {
    val permissionNeedsAttention = enabled && permissionRequired && !permissionGranted
    SettingsSectionCard(
        icon = Icons.Default.NotificationsActive,
        title = "Expiry reminders",
        supportingText = "A daily background check can flag food that is close to its use-by date."
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
                    !enabled -> "Paused"
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
