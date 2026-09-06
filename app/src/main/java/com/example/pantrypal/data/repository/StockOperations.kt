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
            if (amount == stock.quantity) db.inventoryDao().deleteInventory(stock)
            else db.inventoryDao().updateInventory(stock.copy(quantity = stock.quantity - amount))
            if (type == ConsumptionType.FINISHED) {
                val item = db.itemDao().getItemById(stock.itemId)
                if (item != null && item.isUsual && db.inventoryDao().totalQuantity(item.itemId) <= (item.lowStockThreshold ?: 0.0)) restock(item, week)
            }
        }
    }

    suspend fun restock(item: ItemEntity, week: String) = db.withTransaction {
        val listed = db.shoppingDao().getAllShoppingItemsSnapshot().any {
            !it.isChecked && normalizeShoppingName(it.name) == normalizeShoppingName(item.name) && (it.weekId == null || it.weekId == week)
        }
        if (!listed) db.shoppingDao().insertShoppingItem(ShoppingItemEntity(name = item.name,
            quantity = (item.lowStockThreshold ?: 1.0).coerceAtLeast(1.0), unit = item.defaultUnit,
            sectionId = ShoppingSectionEntity.ID_THE_REST, weekId = week))
    }

    suspend fun edit(inventoryId: Long, transform: (InventoryEntity) -> InventoryEntity) = db.withTransaction {
        val current = db.inventoryDao().getById(inventoryId) ?: return@withTransaction
        val next = transform(current)
        require(next.quantity.isFinite()) { "Quantity must be a finite number." }
        if (next.quantity <= 0) db.inventoryDao().deleteInventory(current) else db.inventoryDao().updateInventory(next)
    }
}
