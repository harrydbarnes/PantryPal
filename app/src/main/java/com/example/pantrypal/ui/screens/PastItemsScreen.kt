package com.example.pantrypal.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.pantrypal.data.dao.ConsumptionWithItem
import com.example.pantrypal.ui.components.ExpressiveHero
import com.example.pantrypal.ui.components.FriendlyEmptyState
import com.example.pantrypal.ui.components.StatusPill
import com.example.pantrypal.viewmodel.MainViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun PastItemsScreen(viewModel: MainViewModel) {
    val pastItems by viewModel.pastItemsState.collectAsState()
    val dateFormat = remember { DateTimeFormatter.ofLocalizedDateTime(java.time.format.FormatStyle.SHORT) }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ExpressiveHero(
                eyebrow = "Pantry history",
                title = "A little record of what came and went",
                supportingText = "Finished and wasted items live here, making future restock suggestions more useful.",
                icon = Icons.Default.History,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
        if (pastItems.isEmpty()) {
            item {
                FriendlyEmptyState(
                    title = "No pantry history yet",
                    supportingText = "Items will appear here after you mark them finished or wasted.",
                    icon = Icons.Default.History
                )
            }
        } else {
            items(pastItems, key = { it.eventId }) { item ->
                PastItemRow(item, dateFormat)
            }
        }
    }
}

@Composable
fun PastItemRow(item: ConsumptionWithItem, dateFormat: DateTimeFormatter) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            val wasFinished = item.type.toString().contains("FINISHED", ignoreCase = true)
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = if (wasFinished) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.errorContainer
                },
                contentColor = if (wasFinished) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onErrorContainer
                }
            ) {
                Icon(
                    if (wasFinished) Icons.Default.CheckCircle else Icons.Default.RestoreFromTrash,
                    contentDescription = null,
                    modifier = Modifier.padding(10.dp).size(22.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
                Text(text = item.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${item.quantity} • ${item.category}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val date = Instant.ofEpochMilli(item.date).atZone(ZoneId.systemDefault())
                Text(
                    text = dateFormat.format(date),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            StatusPill(
                label = if (wasFinished) "Finished" else "Wasted",
                containerColor = if (wasFinished) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.errorContainer
                },
                contentColor = if (wasFinished) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onErrorContainer
                }
            )
        }
    }
}
