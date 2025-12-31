package com.example.pantrypal.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.pantrypal.data.entity.ConsumptionEntity
import com.example.pantrypal.viewmodel.MainViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun PastItemsScreen(viewModel: MainViewModel) {
    val pastItems by viewModel.pastItemsState.collectAsState(initial = emptyList())
    val dateFormat = remember { DateTimeFormatter.ofLocalizedDateTime(java.time.format.FormatStyle.SHORT) }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Past Items Log", style = MaterialTheme.typography.headlineMedium)

        LazyColumn(
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(pastItems, key = { it.eventId }) { item ->
                PastItemRow(item, dateFormat)
            }
        }
    }
}

@Composable
fun PastItemRow(item: ConsumptionEntity, dateFormat: DateTimeFormatter) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Item ID: ${item.itemId}", style = MaterialTheme.typography.titleMedium)
            Text(text = "Type: ${item.type}", style = MaterialTheme.typography.bodyMedium)

            val date = Instant.ofEpochMilli(item.date).atZone(ZoneId.systemDefault())
            Text(text = "Date: ${dateFormat.format(date)}", style = MaterialTheme.typography.bodySmall)
        }
    }
}
