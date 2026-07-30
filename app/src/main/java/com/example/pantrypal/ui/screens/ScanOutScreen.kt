package com.example.pantrypal.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.material3.*
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.pantrypal.R
import com.example.pantrypal.data.dao.InventoryWithItemMap
import com.example.pantrypal.data.entity.ConsumptionType
import com.example.pantrypal.ui.BarcodeScanner
import com.example.pantrypal.viewmodel.MainViewModel
import java.time.format.DateTimeFormatter
import java.time.Instant
import java.time.ZoneId

private const val SCAN_DEBOUNCE_MS = 2000L

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
    val scanAmounts = remember { mutableStateMapOf<Long, Double>() }
    // State to show selection dialog for duplicate batches
    var duplicateBatches by remember { mutableStateOf<List<InventoryWithItemMap>?>(null) }

    // Refactored to use java.time API
    val dateFormat = remember { DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.SHORT) }
    val notFoundMessage by rememberUpdatedState(stringResource(R.string.item_not_found_in_inventory))

    var lastScanTime by remember { mutableLongStateOf(0L) }

    LaunchedEffect(detectedBarcode) {
        detectedBarcode?.let { code ->
             val currentTime = System.currentTimeMillis()
             if (currentTime - lastScanTime > SCAN_DEBOUNCE_MS) { // 2 second debounce
                 val inv = viewModel.getInventoryByBarcode(code)
                 if (inv.isNotEmpty()) {
                     // If only one batch, add to queue immediately
                     if (inv.size == 1) {
                         if (scanQueue.none { it.inventoryId == inv[0].inventoryId }) scanQueue.add(inv[0])
                         scanAmounts[inv[0].inventoryId] =
                             ((scanAmounts[inv[0].inventoryId] ?: 0.0) + 1.0).coerceAtMost(inv[0].quantity)
                     } else {
                         // Multiple batches found, let user select
                         duplicateBatches = inv
                     }
                 } else {
                     onShowSnackbar(notFoundMessage)
                 }
                 lastScanTime = currentTime
             }
             // Reset barcode detection
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

                 duplicateBatches?.let { batches ->
                     LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                         items(batches) { item ->
                             Card(
                                 modifier = Modifier
                                     .fillMaxWidth()
                                     .padding(vertical = 4.dp)
                                     .clickable {
                                         if (scanQueue.none { it.inventoryId == item.inventoryId }) scanQueue.add(item)
                                         scanAmounts[item.inventoryId] =
                                             ((scanAmounts[item.inventoryId] ?: 0.0) + 1.0).coerceAtMost(item.quantity)
                                         duplicateBatches = null
                                     },
                                 shape = MaterialTheme.shapes.large,
                                 colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
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
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Scanner taking up most space
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            BarcodeScanner(onBarcodeDetected = { code ->
                if (detectedBarcode == null && duplicateBatches == null) {
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
                    Icon(
                        androidx.compose.material.icons.Icons.Default.QrCodeScanner,
                        contentDescription = null
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (scanQueue.isEmpty()) "Scan what’s leaving the pantry" else "${scanQueue.size} queued • keep scanning",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            if (scanQueue.isEmpty()) {
                FilledTonalButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(32.dp)
                ) {
                    Text("Cancel")
                }
            }
        }

        // Queue UI Overlay
        if (scanQueue.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(16.dp),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Ready to check out", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "${scanQueue.size} item${if (scanQueue.size == 1) "" else "s"} queued",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(scanQueue) { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(item.name, modifier = Modifier.weight(1f))
                                IconButton(onClick = {
                                    val current = scanAmounts[item.inventoryId] ?: 1.0
                                    scanAmounts[item.inventoryId] = (current - 1.0).coerceAtLeast(0.1)
                                }) {
                                    Icon(androidx.compose.material.icons.Icons.Default.Remove, contentDescription = "Reduce amount")
                                }
                                Text("${scanAmounts[item.inventoryId] ?: 1.0} ${item.unit}")
                                IconButton(onClick = {
                                    val current = scanAmounts[item.inventoryId] ?: 1.0
                                    scanAmounts[item.inventoryId] = (current + 1.0).coerceAtMost(item.quantity)
                                }) {
                                    Icon(androidx.compose.material.icons.Icons.Default.Add, contentDescription = "Increase amount")
                                }
                                IconButton(onClick = {
                                    scanQueue.remove(item)
                                    scanAmounts.remove(item.inventoryId)
                                }) {
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
                             viewModel.consumeItemAmounts(
                                 scanQueue.map { it to (scanAmounts[it.inventoryId] ?: 1.0) },
                                 ConsumptionType.FINISHED
                             )
                             scanQueue.clear()
                             scanAmounts.clear()
                             onDismiss()
                         }) {
                             Text("Finish all")
                         }
                         OutlinedButton(onClick = {
                             scanQueue.clear()
                             scanAmounts.clear()
                         }) {
                             Text("Clear")
                         }
                    }
                }
            }
        }
    }
}
