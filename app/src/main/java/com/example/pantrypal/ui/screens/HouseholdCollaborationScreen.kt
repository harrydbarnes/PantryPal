package com.example.pantrypal.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.pantrypal.ui.components.PantryPalSpacing
import java.text.DateFormat
import java.util.Date
import androidx.compose.ui.unit.dp

data class HouseholdSyncUiState(
    val householdName: String = "My household",
    val deviceName: String = "This device",
    val lastSharedAtEpochMs: Long? = null,
    val lastImportedAtEpochMs: Long? = null,
    val isWorking: Boolean = false,
    val message: String? = null
)

/**
 * Callback-driven collaboration settings. Sharing and document picking are owned by the activity;
 * this surface never requests broad storage or network permission.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HouseholdCollaborationScreen(
    state: HouseholdSyncUiState,
    onShareSnapshot: () -> Unit,
    onImportSnapshot: () -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    showTopBar: Boolean = false
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            if (showTopBar) {
                TopAppBar(
                    title = { Text("Household sharing") },
                    navigationIcon = {
                        if (onBack != null) {
                            TextButton(onClick = onBack) { Text("Back") }
                        }
                    }
                )
            }
        }
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = PantryPalSpacing.sm,
                end = PantryPalSpacing.sm,
                top = PantryPalSpacing.xs,
                bottom = PantryPalSpacing.xl
            ),
            verticalArrangement = Arrangement.spacedBy(PantryPalSpacing.md)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(PantryPalSpacing.md),
                        horizontalArrangement = Arrangement.spacedBy(PantryPalSpacing.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.Group,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                state.householdName,
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                "Local-first sharing from ${state.deviceName}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(PantryPalSpacing.md),
                        verticalArrangement = Arrangement.spacedBy(PantryPalSpacing.sm)
                    ) {
                        Text("Portable snapshot", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Share the complete pantry, lists, planner, recipes, prices, budget, and settings through Android. The checksum detects damaged or edited files, but the file is not encrypted.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Sharing is a manual hand-off between devices; live household sync is not connected yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = onShareSnapshot,
                            enabled = !state.isWorking,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Outlined.Share, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Share snapshot")
                        }
                        OutlinedButton(
                            onClick = onImportSnapshot,
                            enabled = !state.isWorking,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Outlined.FileOpen, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Import and review snapshot")
                        }
                        SnapshotTime("Last shared", state.lastSharedAtEpochMs)
                        SnapshotTime("Last imported", state.lastImportedAtEpochMs)
                    }
                }
            }

            state.message?.let { message ->
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    ) {
                        Text(
                            message,
                            modifier = Modifier.padding(PantryPalSpacing.md),
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SnapshotTime(label: String, epochMs: Long?) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            epochMs?.let { DateFormat.getDateTimeInstance().format(Date(it)) } ?: "Not yet",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
