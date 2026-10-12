package com.example.pantrypal.util

import com.example.pantrypal.data.entity.ShoppingItemEntity
import com.example.pantrypal.data.entity.ShoppingSectionEntity

object ShoppingMetadata {
    fun baseline(id: String) = "meal-baseline:$id"
    fun checkStock(id: String) = "check-stock:$id"
    fun completed(id: String) = "completed-shop:$id"
    fun fingerprint(item: ShoppingItemEntity) = "${item.name}|${item.quantity}|${item.unit}"
}

enum class ShoppingChangeKind { ADD, UPDATE, REMOVE, KEEP }
data class ShoppingPlanChange(
    val kind: ShoppingChangeKind,
    val item: ShoppingItemEntity,
    val previous: ShoppingItemEntity? = null,
    val checkStock: Boolean = false,
    val explanation: String = ""
)

/** Purchased rows and manual edits are protected. Replacement is an explicit preview choice. */
fun planShoppingUpdate(
    lines: List<ShoppingReconciliationLine>,
    existing: List<ShoppingItemEntity>,
    sections: List<ShoppingSectionEntity>,
    week: String,
    layout: Map<String, String>,
    replaceEdited: Boolean = false
): List<ShoppingPlanChange> {
    val recurringIds = sections.filter { it.recursEveryWeek }.map { it.sectionId }.toSet()
    val visible = existing.filter { it.weekId == week || it.weekId == null || it.sectionId in recurringIds }
    val mealItems = visible.filter { it.sectionId == ShoppingSectionEntity.ID_MEAL_PLAN && it.weekId == week }
    val used = mutableSetOf<String>()
    val changes = mutableListOf<ShoppingPlanChange>()
    lines.filter { it.buyQuantity > 0 }.forEach { line ->
        val matches = mealItems.filter {
            normalizeShoppingName(it.name) == line.normalizedName &&
                convertShoppingQuantity(it.quantity, it.unit, line.unit) != null
        }
        val otherCoverage = visible.filter { it !in mealItems && normalizeShoppingName(it.name) == line.normalizedName }
            .sumOf { convertShoppingQuantity(it.quantity, it.unit, line.unit) ?: 0.0 }
        val boughtCoverage = matches.filter { it.isChecked }.sumOf {
            convertShoppingQuantity(it.quantity, it.unit, line.unit) ?: 0.0
        }
        val desired = (line.buyQuantity - otherCoverage - boughtCoverage).coerceAtLeast(0.0)
        val unchecked = matches.filterNot { it.isChecked }
        val protected = unchecked.filter {
            !replaceEdited && layout[ShoppingMetadata.baseline(it.syncId)] != ShoppingMetadata.fingerprint(it)
        }
        protected.forEach {
            used += it.syncId
            changes += ShoppingPlanChange(ShoppingChangeKind.KEEP, it, explanation = "Keep your edit")
        }
        val protectedCoverage = protected.sumOf { convertShoppingQuantity(it.quantity, it.unit, line.unit) ?: 0.0 }
        val remaining = (desired - protectedCoverage).coerceAtLeast(0.0)
        val generated = unchecked.filterNot { it in protected }
        val target = generated.firstOrNull()
        if (remaining > 0) {
            val item = target?.copy(quantity = remaining, unit = line.unit) ?: ShoppingItemEntity(
                name = line.name, quantity = remaining, unit = line.unit,
                sectionId = ShoppingSectionEntity.ID_MEAL_PLAN, weekId = week
            )
            changes += ShoppingPlanChange(
                if (target == null) ShoppingChangeKind.ADD else if (item == target) ShoppingChangeKind.KEEP else ShoppingChangeKind.UPDATE,
                item, target, line.status == ShoppingNeedStatus.CHECK_STOCK,
                if (target == null) "Add missing quantity" else "Update meal requirement"
            )
            target?.let { used += it.syncId }
        }
        // Any extra unedited rows are handled below, rather than increasing quantities on rebuild.
    }
    mealItems.filter { it.syncId !in used }.forEach { item ->
        val protected = item.isChecked || (!replaceEdited &&
            layout[ShoppingMetadata.baseline(item.syncId)] != ShoppingMetadata.fingerprint(item))
        changes += ShoppingPlanChange(
            if (protected) ShoppingChangeKind.KEEP else ShoppingChangeKind.REMOVE,
            item, item, layout[ShoppingMetadata.checkStock(item.syncId)] == "true",
            if (item.isChecked) "Already bought; keep ticked" else if (protected) "Keep your edit" else "No longer needed by the plan"
        )
    }
    return changes
}
