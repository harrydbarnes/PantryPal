package com.example.pantrypal.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.pantrypal.R
import com.example.pantrypal.data.dao.InventoryWithItemMap
import com.example.pantrypal.data.entity.ConsumptionType
import com.example.pantrypal.ui.BarcodeScanner
import com.example.pantrypal.viewmodel.MainViewModel
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanOutScreen(
    onDismiss: () -> Unit,
    onShowSnackbar: (String) -> Unit,
    viewModel: MainViewModel
) {
    var detectedBarcode by remember { mutableStateOf<String?>(null) }
    // Batch mode: Queue of items to consume
    val scanQueue = remember { mutableStateListOf<InventoryWithItemMap>() }
    // State to show selection dialog for duplicate batches
    var duplicateBatches by remember { mutableStateOf<List<InventoryWithItemMap>?>(null) }

    // Refactored to use java.time API
    val dateFormat = remember { DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.SHORT) }
    val notFoundMessage by rememberUpdatedState(stringResource(R.string.item_not_found_in_inventory))

    LaunchedEffect(detectedBarcode) {
        detectedBarcode?.let { code ->
             val inv = viewModel.getInventoryByBarcode(code)
             if (inv.isNotEmpty()) {
                 // If only one batch, add to queue immediately
                 if (inv.size == 1) {
                     scanQueue.add(inv[0])
                 } else {
                     // Multiple batches found, let user select
                     duplicateBatches = inv
                 }
             } else {
                 onShowSnackbar(notFoundMessage)
             }
             // Reset barcode detection immediately to allow continuous scanning
             // But we need a delay or debounce in real app, here we just clear it
             detectedBarcode = null
        }
    }

    if (duplicateBatches != null) {
        ModalBottomSheet(onDismissRequest = {
             duplicateBatches = null
        }) {
             Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                 Text("Multiple batches found:", style = MaterialTheme.typography.headlineSmall)
                 Spacer(modifier = Modifier.height(16.dp))

                 LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                     items(duplicateBatches.orEmpty()) { item ->
                         Card(
                             modifier = Modifier
                                 .fillMaxWidth()
                                 .padding(vertical = 4.dp)
                                 .clickable {
                                     scanQueue.add(item)
                                     duplicateBatches = null
                                 },
                             colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                         ) {
                             Column(modifier = Modifier.padding(12.dp)) {
                                 Text(item.name, style = MaterialTheme.typography.titleMedium)
                                 Text("Qty: ${item.quantity} ${item.unit}")
                                 item.expirationDate?.let {
                                     val date = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                                     Text("Exp: ${dateFormat.format(date)}", style = MaterialTheme.typography.bodySmall)
                                 }
                             }
                         }
                     }
                 }
             }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Scanner taking up most space
        Box(modifier = Modifier.fillMaxSize().padding(bottom = if (scanQueue.isNotEmpty()) 150.dp else 0.dp)) {
            BarcodeScanner(onBarcodeDetected = { code ->
                if (detectedBarcode == null && duplicateBatches == null) {
                    detectedBarcode = code
                }
            })
        }

        // Queue UI Overlay
        if (scanQueue.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Queue (${scanQueue.size})", style = MaterialTheme.typography.titleMedium)
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(scanQueue) { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(item.name)
                                IconButton(onClick = { scanQueue.remove(item) }) {
                                    Icon(androidx.compose.material.icons.Icons.Default.Close, contentDescription = "Remove")
                                }
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                         Button(onClick = {
                             scanQueue.forEach {
                                 viewModel.consumeItem(it.inventoryId, it.itemId, 1.0, ConsumptionType.FINISHED)
                             }
                             scanQueue.clear()
                             onDismiss()
                         }) {
                             Text("Consume All")
                         }
                         OutlinedButton(onClick = { scanQueue.clear() }) {
                             Text("Clear")
                         }
                    }
                }
            }
        } else {
            Button(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.BottomCenter).padding(32.dp)
            ) {
                Text("Cancel")
            }
        }
    }
}
