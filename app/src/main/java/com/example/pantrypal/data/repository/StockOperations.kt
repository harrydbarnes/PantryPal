package com.example.pantrypal.data.repository

import androidx.room.withTransaction
import com.example.pantrypal.data.database.KitchenDatabase
import com.example.pantrypal.data.entity.*
import com.example.pantrypal.util.normalizeShoppingName

/** Read the latest rows and commit stock, history and restock together, including bulk actions. */
class StockOperations(private val db: KitchenDatabase) {
    data class Use(val inventoryId: Long, val itemId: Long, val quantity: Double)

    suspend fun consume(uses: List<Use>, type: ConsumptionType, week: String, reason: String? = null) = db.withTransaction {
        uses.forEach { use ->
            require(use.quantity.isFinite() && use.quantity > 0) { "Quantity must be a positive number." }
            val stock = db.inventoryDao().getById(use.inventoryId) ?: return@forEach
            require(stock.itemId == use.itemId) { "The stock item has changed. Refresh and try again." }
            val amount = minOf(use.quantity, stock.quantity)
            if (amount <= 0) return@forEach
            db.consumptionDao().insertConsumption(ConsumptionEntity(itemId = stock.itemId, quantity = amount, type = type, wasteReason = reason))
            db.inventoryDao().updateInventory(stock.copy(quantity = stock.quantity - amount))
            if (type == ConsumptionType.FINISHED) {
                val item = db.itemDao().getItemById(stock.itemId)
                if (item != null && item.isUsual && com.example.pantrypal.util.stockQuantityInUnit(db.inventoryDao().getBatchesForItem(item.itemId), item.defaultUnit)
                        ?.let { it <= (item.lowStockThreshold ?: 0.0) } == true) restock(item, week)
            }
        }
    }

    suspend fun restock(item: ItemEntity, week: String) = db.withTransaction {
        val listed = db.shoppingDao().getAllShoppingItemsSnapshot().any {
            !it.isChecked && normalizeShoppingName(it.name) == normalizeShoppingName(item.name) && (it.weekId == null || it.weekId == week)
        }
        if (!listed) db.shoppingListDao().upsertShoppingItem(item.name,
            (item.lowStockThreshold ?: 1.0).coerceAtLeast(1.0), "RESTOCK", week, unit = item.defaultUnit)
    }

    suspend fun edit(inventoryId: Long, transform: (InventoryEntity) -> InventoryEntity) = db.withTransaction {
        val current = db.inventoryDao().getById(inventoryId) ?: return@withTransaction
        val next = transform(current)
        require(next.quantity.isFinite()) { "Quantity must be a finite number." }
        db.inventoryDao().updateInventory(next.copy(quantity = next.quantity.coerceAtLeast(0.0)))
    }
}
