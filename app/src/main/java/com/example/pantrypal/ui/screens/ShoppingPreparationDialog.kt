package com.example.pantrypal.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.pantrypal.util.*
import com.example.pantrypal.viewmodel.MainViewModel
import com.example.pantrypal.viewmodel.ShoppingBuildPreview

/** The same review is used from Plan and Shop. No list changes occur until Apply. */
@Composable
fun ShoppingPreparationDialog(viewModel: MainViewModel, preview: ShoppingBuildPreview, weekName: String) {
    val saving by viewModel.saving.collectAsState()
    val error by viewModel.actionError.collectAsState()
    var quantities by rememberSaveable(preview.weekId, preview.existingItems.hashCode()) {
        mutableStateOf(preview.lines.map { it.buyQuantity.readable() })
    }
    var decisions by rememberSaveable(preview.weekId, preview.existingItems.hashCode()) {
        mutableStateOf(preview.lines.map {
            when (it.status) {
                ShoppingNeedStatus.ALREADY_AT_HOME -> "have"
                ShoppingNeedStatus.CHECK_STOCK -> "check"
                ShoppingNeedStatus.NEED_TO_BUY -> "need"
            }
        })
    }
    var replaceEdited by rememberSaveable { mutableStateOf(false) }
    val valid = quantities.indices.all { decisions[it] == "have" ||
        quantities[it].replace(',', '.').toDoubleOrNull()?.let { q -> q.isFinite() && q > 0 } == true }
    val selected = preview.lines.mapIndexed { index, line ->
        line.copy(
            buyQuantity = if (decisions[index] == "have") 0.0 else quantities[index].replace(',', '.').toDoubleOrNull() ?: 0.0,
            status = if (decisions[index] == "check") ShoppingNeedStatus.CHECK_STOCK else ShoppingNeedStatus.NEED_TO_BUY
        )
    }
    val changes = planShoppingUpdate(selected, preview.existingItems, preview.sections, preview.weekId, preview.layout, replaceEdited)
    AlertDialog(
        onDismissRequest = { if (!saving) viewModel.dismissShoppingBuildPreview() },
        title = { Text("Prepare $weekName") },
        text = {
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 460.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Text("${changes.count { it.kind == ShoppingChangeKind.ADD }} added · ${changes.count { it.kind == ShoppingChangeKind.UPDATE }} changed · ${changes.count { it.kind == ShoppingChangeKind.REMOVE }} removed")
                    Text("Purchased items stay ticked. Your edits are kept unless you choose to replace them.", style = MaterialTheme.typography.bodySmall)
                }
                itemsIndexed(preview.lines) { index, line ->
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(line.name, style = MaterialTheme.typography.titleMedium)
                        Text("${line.requiredQuantity.readable()} ${line.unit} needed · ${line.availableQuantity.readable()} ${line.unit} at home", style = MaterialTheme.typography.bodySmall)
                        line.stockNote?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = decisions[index] == "have", enabled = !saving,
                                onClick = { decisions = decisions.toMutableList().apply { set(index, "have") } }, label = { Text("Have it") })
                            FilterChip(selected = decisions[index] == "need", enabled = !saving,
                                onClick = {
                                    decisions = decisions.toMutableList().apply { set(index, "need") }
                                    if (quantities[index].toDoubleOrNull() == 0.0) quantities = quantities.toMutableList().apply { set(index, line.requiredQuantity.readable()) }
                                }, label = { Text("Need it") })
                        }
                        if (decisions[index] != "have") {
                            OutlinedTextField(value = quantities[index], enabled = !saving,
                                onValueChange = { value -> quantities = quantities.toMutableList().apply { set(index, value) } },
                                label = { Text("Buy (${line.unit})") }, singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                isError = quantities[index].replace(',', '.').toDoubleOrNull()?.let { it.isFinite() && it > 0 } != true,
                                modifier = Modifier.fillMaxWidth())
                            if (decisions[index] == "check") Text("Still needs a stock check; this label will stay on the list.", style = MaterialTheme.typography.bodySmall)
                        }
                        HorizontalDivider()
                    }
                }
                item {
                    Row {
                        Checkbox(checked = replaceEdited, enabled = !saving, onCheckedChange = { replaceEdited = it })
                        Text("Replace my unpurchased meal-list edits with these quantities", Modifier.padding(top = 12.dp))
                    }
                }
                itemsIndexed(changes) { _, change ->
                    Text("${change.kind.name.lowercase().replaceFirstChar { it.uppercase() }} · ${change.item.name} · ${change.item.quantity.readable()} ${change.item.unit}\n${change.explanation}", style = MaterialTheme.typography.bodySmall)
                }
                error?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.error) } }
            }
        },
        confirmButton = { Button(enabled = valid && !saving, onClick = { viewModel.commitShoppingBuildPreview(selected, replaceEdited) }) { Text(if (saving) "Saving…" else "Apply changes") } },
        dismissButton = { TextButton(enabled = !saving, onClick = viewModel::dismissShoppingBuildPreview) { Text("Cancel") } }
    )
}

private fun Double.readable() = if (this % 1.0 == 0.0) toLong().toString() else toString()
