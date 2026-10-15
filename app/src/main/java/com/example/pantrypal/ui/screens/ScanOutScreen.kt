package com.example.pantrypal.ui.screens

import com.example.pantrypal.ui.components.expressiveItem
import com.example.pantrypal.ui.components.expressiveItems
import com.example.pantrypal.ui.components.expressiveItemsIndexed
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
    val saving by viewModel.saving.collectAsState()
    val error by viewModel.actionError.collectAsState()
    androidx.activity.compose.BackHandler(enabled = saving) { }
    var detectedBarcode by remember { mutableStateOf<String?>(null) }
    // Batch mode: Queue of items to consume
    val scanQueue = androidx.compose.runtime.saveable.rememberSaveable(saver = androidx.compose.runtime.saveable.listSaver<androidx.compose.runtime.snapshots.SnapshotStateList<InventoryWithItemMap>, String>(
        save = { entries -> entries.map { com.google.gson.Gson().toJson(it) } },
        restore = { entries -> entries.map { com.google.gson.Gson().fromJson(it, InventoryWithItemMap::class.java) }.toMutableStateList() }
    )) { mutableStateListOf<InventoryWithItemMap>() }
    val scanAmounts = androidx.compose.runtime.saveable.rememberSaveable(saver = androidx.compose.runtime.saveable.listSaver<androidx.compose.runtime.snapshots.SnapshotStateMap<Long, String>, String>(
        save = { entries -> entries.flatMap { listOf(it.key.toString(), it.value) } },
        restore = { entries -> mutableStateMapOf<Long, String>().apply { entries.chunked(2).forEach { put(it[0].toLong(), it[1]) } } }
    )) { mutableStateMapOf<Long, String>() }
    fun queueBatch(batch: InventoryWithItemMap) {
        if (scanQueue.none { it.inventoryId == batch.inventoryId }) scanQueue.add(batch)
        val counted = com.example.pantrypal.util.canonicalShoppingUnit(batch.unit) in setOf("pcs", "pack", "tin", "bottle")
        scanAmounts[batch.inventoryId] = if (counted)
            ((scanAmounts[batch.inventoryId]?.toDoubleOrNull() ?: 0.0) + 1.0).coerceAtMost(batch.quantity).toString()
        else scanAmounts[batch.inventoryId].orEmpty()
    }
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
                 val inv = viewModel.getInventoryByBarcode(code).filter { it.quantity > 0 }
                 if (inv.isNotEmpty()) {
                     // If only one batch, add to queue immediately
                     if (inv.size == 1) {
                         queueBatch(inv[0])
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
                         expressiveItems(batches) { item ->
                             Card(
                                 modifier = Modifier
                                     .fillMaxWidth()
                                     .padding(vertical = 4.dp)
                                     .clickable {
                                         queueBatch(item)
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

    Column(modifier = Modifier.fillMaxSize().imePadding()) {
        // Scanner taking up most space
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            BarcodeScanner(onBarcodeDetected = { code ->
                if (!saving && detectedBarcode == null && duplicateBatches == null) {
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
                    .heightIn(min = 240.dp, max = 400.dp)
                    .padding(16.dp),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Review food used", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "${scanQueue.size} item${if (scanQueue.size == 1) "" else "s"} queued",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        expressiveItems(scanQueue) { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(item.name, modifier = Modifier.weight(1f))
                                OutlinedTextField(value = scanAmounts[item.inventoryId].orEmpty(), onValueChange = { value ->
                                    scanAmounts[item.inventoryId] = value
                                }, label = { Text(item.unit) }, singleLine = true, enabled = !saving,
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                                    modifier = Modifier.width(104.dp))
                                TextButton(enabled = !saving, onClick = { scanAmounts[item.inventoryId] = item.quantity.toString() }) { Text("All") }
                                IconButton(enabled = !saving, onClick = {
                                    scanQueue.remove(item)
                                    scanAmounts.remove(item.inventoryId)
                                }) {
                                    Icon(androidx.compose.material.icons.Icons.Default.Close, contentDescription = "Remove")
                                }
                            }
                        }
                    }
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                         Button(enabled = !saving && scanQueue.all { item -> scanAmounts[item.inventoryId]?.toDoubleOrNull()?.let { it.isFinite() && it > 0 && it <= item.quantity } == true }, onClick = {
                             viewModel.consumeItemAmounts(
                                 scanQueue.map { it to requireNotNull(scanAmounts[it.inventoryId]?.toDoubleOrNull()) },
                                 ConsumptionType.FINISHED
                             ) {
                                 scanQueue.clear()
                                 scanAmounts.clear()
                                 onDismiss()
                             }
                         }) {
                             Text(if (saving) "Saving…" else "Save usage")
                         }
                         OutlinedButton(enabled = !saving, onClick = {
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
