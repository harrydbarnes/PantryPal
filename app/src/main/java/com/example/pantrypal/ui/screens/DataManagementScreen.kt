package com.example.pantrypal.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.pantrypal.ui.components.PantryPalSpacing

data class DataManagementUiState(
    val isWorking: Boolean = false,
    val message: String? = null
)

@Composable
fun DataManagementScreen(
    state: DataManagementUiState,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onOpenHousehold: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
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
                ),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Row(
                    modifier = Modifier.padding(PantryPalSpacing.md),
                    horizontalArrangement = Arrangement.spacedBy(PantryPalSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Column {
                        Text(
                            "Your kitchen, in your hands",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            "PantryPal remains local-first. Backups use a readable file you choose.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
        item {
            DataCard(
                title = "Complete backup",
                supportingText = "Includes pantry batches, shopping, meal plans, recipes, prices, budgets and preferences."
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(PantryPalSpacing.sm)
                ) {
                    Button(
                        onClick = onExportBackup,
                        enabled = !state.isWorking,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Outlined.Download, contentDescription = null)
                        Text(" Export")
                    }
                    OutlinedButton(
                        onClick = onImportBackup,
                        enabled = !state.isWorking,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Outlined.Upload, contentDescription = null)
                        Text(" Restore")
                    }
                }
                Text(
                    "Restoring replaces the kitchen currently stored on this device after validation.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item {
            DataCard(
                title = "Share with your household",
                supportingText = "Exchange a checksummed snapshot with another PantryPal device and keep a clear route to future real-time sync."
            ) {
                FilledTonalButton(
                    onClick = onOpenHousehold,
                    enabled = !state.isWorking,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Outlined.Group, contentDescription = null)
                    Text(" Household sharing")
                }
            }
        }
        state.message?.let { message ->
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Text(
                        message,
                        modifier = Modifier.padding(PantryPalSpacing.md),
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun DataCard(
    title: String,
    supportingText: String,
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier.padding(PantryPalSpacing.md),
            verticalArrangement = Arrangement.spacedBy(PantryPalSpacing.sm)
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(
                supportingText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            content()
        }
    }
}
