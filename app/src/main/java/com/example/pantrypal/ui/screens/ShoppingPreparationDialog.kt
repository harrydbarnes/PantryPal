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
    val quantities = preview.quantities
    val decisions = preview.decisions
    val replaceEdited = preview.replaceEdited
    val valid = quantities.indices.all { decisions[it] == "have" ||
        quantities[it].replace(',', '.').toDoubleOrNull()?.let { q -> q.isFinite() && q > 0 } == true }
    val selected = preview.lines.mapIndexed { index, line ->
        line.copy(
            buyQuantity = if (decisions[index] == "have") 0.0 else quantities[index].replace(',', '.').toDoubleOrNull() ?: 0.0,
            status = if (decisions[index] == "check") ShoppingNeedStatus.CHECK_STOCK else ShoppingNeedStatus.NEED_TO_BUY,
            explicitDecision = preview.explicit[index]
        )
    }
    val changes = planShoppingUpdate(selected, preview.existingItems, preview.sections, preview.weekId, preview.layout, replaceEdited, preview.recordedPurchaseIds)
    AlertDialog(
        onDismissRequest = { if (!saving) viewModel.pauseShoppingReview() },
        title = { Text("Prepare $weekName") },
        text = {
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 460.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Text("${changes.count { it.kind == ShoppingChangeKind.ADD }} added · ${changes.count { it.kind == ShoppingChangeKind.UPDATE }} changed · ${changes.count { it.kind == ShoppingChangeKind.REMOVE }} removed")
                    Text("Purchased items stay ticked. Your explicit stock choices apply to this ingredient; other saved edits are kept. This review is saved while you work.", style = MaterialTheme.typography.bodySmall)
                }
                itemsIndexed(preview.lines) { index, line ->
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(line.name, style = MaterialTheme.typography.titleMedium)
                        Text("${line.requiredQuantity.readable()} ${line.unit} needed · ${line.availableQuantity.readable()} ${line.unit} at home", style = MaterialTheme.typography.bodySmall)
                        line.stockNote?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = decisions[index] == "have", enabled = !saving,
                                onClick = { viewModel.editShoppingDraft(index, decision = "have") }, label = { Text("Have it") })
                            FilterChip(selected = decisions[index] == "need", enabled = !saving,
                                onClick = {
                                    viewModel.editShoppingDraft(index,
                                        quantity = if (quantities[index].toDoubleOrNull() == 0.0) line.requiredQuantity.readable() else null,
                                        decision = "need")
                                }, label = { Text("Need it") })
                        }
                        if (decisions[index] != "have") {
                            OutlinedTextField(value = quantities[index], enabled = !saving,
                                onValueChange = { value -> viewModel.editShoppingDraft(index, quantity = value, decision = "need") },
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
                        Checkbox(checked = replaceEdited, enabled = !saving, onCheckedChange = viewModel::replaceShoppingDraftEdits)
                        Text("Replace my unpurchased meal-list edits with these quantities", Modifier.padding(top = 12.dp))
                    }
                }
                itemsIndexed(changes) { _, change ->
                    Text("${change.kind.name.lowercase().replaceFirstChar { it.uppercase() }} · ${change.item.name} · ${change.item.quantity.readable()} ${change.item.unit}\n${change.explanation}", style = MaterialTheme.typography.bodySmall)
                }
                error?.let { message -> item {
                    Text(message, color = MaterialTheme.colorScheme.error)
                    TextButton(enabled = !saving, onClick = { viewModel.previewShoppingListForWeek(preview.weekId, refresh = true) }) { Text("Refresh review") }
                } }
            }
        },
        confirmButton = { Button(enabled = valid && !saving, onClick = { viewModel.commitShoppingBuildPreview(selected, replaceEdited) }) { Text(if (saving) "Saving…" else "Apply changes") } },
        dismissButton = { Row {
            TextButton(enabled = !saving, onClick = viewModel::pauseShoppingReview) { Text("Save for later") }
            TextButton(enabled = !saving, onClick = viewModel::dismissShoppingBuildPreview) { Text("Discard") }
        } }
    )
}

private fun Double.readable() = if (this % 1.0 == 0.0) toLong().toString() else toString()
