package com.example.pantrypal.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.pantrypal.data.dao.InventoryWithItemMap
import com.example.pantrypal.util.CookingReview
import com.example.pantrypal.util.planCooking

@Composable
fun CookingReviewDialog(review: CookingReview, saving: Boolean, error: String?, onDismiss: () -> Unit,
    onSave: (List<Pair<InventoryWithItemMap, Double>>, Boolean) -> Unit) {
    var portions by rememberSaveable(review.recipe.id, review.meal?.mealId) { mutableStateOf(review.initialServings.toString()) }
    val amount = portions.toDoubleOrNull()?.takeIf { it.isFinite() && it > 0 }
    val multiplier = amount?.div(review.recipe.servings ?: 1.0)
    val plan = remember(review, multiplier) { multiplier?.let { planCooking(review.recipe, review.stock, it) } }
    // Changing portions deliberately resets proposed deductions; each amount remains editable afterwards.
    var edits by rememberSaveable(review.recipe.id, review.meal?.mealId, portions) { mutableStateOf(hashMapOf<String, String>()) }
    val uses = plan?.lines?.map { line ->
        line.stock to (edits[line.stock.inventoryId.toString()]?.toDoubleOrNull() ?: if (edits.containsKey(line.stock.inventoryId.toString())) Double.NaN else line.suggestedQuantity)
    }.orEmpty()
    val valid = amount != null && uses.all { (stock, used) -> used.isFinite() && used >= 0 && used <= stock.quantity }
    AlertDialog(onDismissRequest = { if (!saving) onDismiss() }, title = { Text("Cook ${review.recipe.title}") },
        text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Review what you used. Only confirmed amounts change pantry stock. Optional ingredients are left for manual pantry usage.")
            OutlinedTextField(portions, { portions = it }, label = { Text(if (review.recipe.servings == null) "Recipe batches (1 = original amounts)" else "Servings cooked") },
                supportingText = { Text("Changing portions recalculates the amounts below.") }, enabled = !saving,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(), singleLine = true)
            plan?.notes?.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
            plan?.lines?.forEach { line ->
                OutlinedTextField(edits[line.stock.inventoryId.toString()] ?: line.suggestedQuantity.toString(),
                    { value -> edits = HashMap(edits).apply { put(line.stock.inventoryId.toString(), value) } },
                    label = { Text("${line.stock.name} (${line.stock.unit})") },
                    supportingText = { Text("${line.stock.quantity} ${line.stock.unit} · ${line.stock.storageLocation}") },
                    enabled = !saving, singleLine = true, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            TextButton(enabled = !saving, onClick = { onSave(emptyList(), true) }) { Text("Just record cooked; keep stock unchanged") }
        } },
        confirmButton = { Button(enabled = !saving && valid && uses.any { it.second > 0 }, onClick = { onSave(uses.filter { it.second > 0 }, false) }) { Text(if (saving) "Saving…" else "Confirm usage") } },
        dismissButton = { TextButton(enabled = !saving, onClick = onDismiss) { Text("Cancel") } })
}
