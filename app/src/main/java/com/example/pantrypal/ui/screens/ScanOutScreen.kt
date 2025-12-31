package com.example.pantrypal.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
    var foundInventory by remember { mutableStateOf<List<InventoryWithItemMap>?>(null) }

    // Refactored to use java.time API
    val dateFormat = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault()) }

    LaunchedEffect(detectedBarcode) {
        detectedBarcode?.let { code ->
             val inv = viewModel.getInventoryByBarcode(code)
             if (inv.isNotEmpty()) {
                 foundInventory = inv
             } else {
                 onShowSnackbar("Item not found in inventory")
                 detectedBarcode = null
             }
        }
    }

    val currentInventory = foundInventory
    if (currentInventory != null) {
        ModalBottomSheet(onDismissRequest = {
             foundInventory = null
             detectedBarcode = null
        }) {
             Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                 Text("Found: ${currentInventory.firstOrNull()?.name ?: "Unknown"}", style = MaterialTheme.typography.headlineSmall)
                 Spacer(modifier = Modifier.height(16.dp))

                 Text("Select batch to consume:", style = MaterialTheme.typography.titleSmall)
                 LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                     items(currentInventory) { item ->
                         Card(
                             modifier = Modifier
                                 .fillMaxWidth()
                                 .padding(vertical = 4.dp),
                             colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                         ) {
                             Column(modifier = Modifier.padding(12.dp)) {
                                 Text("Qty: ${item.quantity} ${item.unit}")
                                 item.expirationDate?.let {
                                     val date = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                                     Text("Exp: ${dateFormat.format(date)}", style = MaterialTheme.typography.bodySmall)
                                 }
                                 Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                                     Button(
                                         onClick = {
                                             viewModel.consumeItem(item.inventoryId, item.itemId, 1.0, ConsumptionType.FINISHED)
                                             onDismiss()
                                         },
                                         modifier = Modifier.padding(end = 8.dp)
                                     ) {
                                         Text("Consume")
                                     }
                                     OutlinedButton(
                                         onClick = {
                                              viewModel.consumeItem(item.inventoryId, item.itemId, 1.0, ConsumptionType.WASTED)
                                              onDismiss()
                                         }
                                     ) {
                                         Text("Waste")
                                     }
                                 }
                             }
                         }
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
            Button(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.BottomCenter).padding(32.dp)
            ) {
                Text("Cancel")
            }
        }
    }
}
