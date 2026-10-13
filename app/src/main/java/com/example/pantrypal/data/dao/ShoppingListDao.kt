package com.example.pantrypal.data.dao

import androidx.room.*
import com.example.pantrypal.data.entity.*
import com.example.pantrypal.domain.shopping.*
import com.example.pantrypal.util.convertShoppingQuantity
import com.example.pantrypal.util.classifyExpiry
import com.example.pantrypal.util.ExpiryStatus
import com.example.pantrypal.util.ShoppingMetadata
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/** One shopping table for quick entry, Keep, voice, weekly plans and household sharing. */
@Dao
abstract class ShoppingListDao {
    @Query("SELECT * FROM shopping_list ORDER BY isChecked, category, addedAt")
    abstract fun getAllShoppingItems(): Flow<List<ShoppingListEntity>>
    @Query("SELECT * FROM shopping_list") abstract suspend fun snapshot(): List<ShoppingListEntity>
    @Query("SELECT * FROM shopping_list WHERE syncId = :id LIMIT 1") abstract suspend fun bySyncId(id: String): ShoppingListEntity?
    @Query("SELECT * FROM items WHERE lower(trim(name)) = lower(:name) ORDER BY itemId LIMIT 1")
    abstract suspend fun product(name: String): ItemEntity?
    @Query("SELECT * FROM items WHERE itemId = :id") abstract suspend fun productById(id: Long): ItemEntity?
    @Query("SELECT * FROM inventory WHERE itemId = :id ORDER BY quantity DESC, inventoryId")
    abstract suspend fun stock(id: Long): List<InventoryEntity>
    @Query("SELECT * FROM inventory WHERE inventoryId = :id") abstract suspend fun batch(id: Long): InventoryEntity?
    @Insert abstract suspend fun insert(row: ShoppingListEntity): Long
    @Update abstract suspend fun update(row: ShoppingListEntity)
    @Delete abstract suspend fun delete(row: ShoppingListEntity)
    @Insert abstract suspend fun insertProduct(row: ItemEntity): Long
    @Insert abstract suspend fun insertStock(row: InventoryEntity): Long
    @Update abstract suspend fun updateStock(row: InventoryEntity)
    @Query("UPDATE inventory SET preferredStore = :store WHERE itemId = :id") abstract suspend fun rememberStockStore(id: Long, store: StoreTag)
    @Query("UPDATE items SET preferredStore = :store WHERE itemId = :id") abstract suspend fun rememberProductStore(id: Long, store: StoreTag)
    @Query("SELECT * FROM shopping_sections") abstract suspend fun sections(): List<ShoppingSectionEntity>
    @Query("SELECT * FROM shopping_layout") abstract suspend fun layout(): List<ShoppingLayoutEntity>
    @Query("DELETE FROM shopping_layout WHERE layoutKey = :key") abstract suspend fun removeMetadata(key: String)
    @Query("SELECT * FROM purchase_records WHERE shoppingSyncId = :id AND completed = 0 LIMIT 1")
    abstract suspend fun pendingPurchase(id: String): PurchaseRecordEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) abstract suspend fun savePurchase(row: PurchaseRecordEntity)
    @Insert abstract suspend fun archive(row: ShoppingArchiveEntity)
    @Query("SELECT * FROM shopping_ingest_state WHERE ingestKey = :key") abstract suspend fun ingestState(key: String): ShoppingIngestEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) abstract suspend fun saveIngestState(row: ShoppingIngestEntity)
    @Query("SELECT * FROM shopping_ingest_state") abstract suspend fun ingestStates(): List<ShoppingIngestEntity>
    @Query("DELETE FROM shopping_ingest_state") abstract suspend fun clearIngestStates()
    @Query("DELETE FROM consumption_history WHERE eventId = :id") abstract suspend fun deleteConsumption(id: Long)
    @Insert abstract suspend fun logConsumption(row: ConsumptionEntity): Long
    @Query("SELECT * FROM consumption_history WHERE eventId = :id") abstract suspend fun consumption(id: Long): ConsumptionEntity?

    @Transaction
    open suspend fun upsertShoppingItem(rawName: String, quantity: Double = 1.0, source: String = "MANUAL",
        weekId: String? = null, store: StoreTag? = null, unit: String? = null): ShoppingListEntity {
        val name = rawName.trim().replace(Regex("\\s+"), " ")
        require(name.isNotBlank() && name.length <= 200) { "Enter a shopping item name of up to 200 characters." }
        require(quantity.isFinite() && quantity > 0 && quantity <= 100_000) { "Enter a valid positive quantity." }
        val item = product(name)
        val stocks = item?.let { stock(it.itemId) }.orEmpty()
        val linked = stocks.firstOrNull { unit == null || convertShoppingQuantity(quantity, unit, it.unit) != null }
        val targetUnit = unit ?: linked?.unit ?: item?.defaultUnit ?: "pcs"
        val recurring = sections().filter { it.recursEveryWeek }.map { it.sectionId }.toSet()
        // Unlike measures, different weeks and ticked purchases deliberately retain distinct identities.
        val existing = snapshot().firstOrNull { !it.isChecked && it.name.equals(name, true) &&
            (it.weekId == weekId || it.weekId == null || it.sectionId in recurring) &&
            convertShoppingQuantity(quantity, targetUnit, it.unit) != null }
        val preferred = store ?: linked?.preferredStore ?: item?.preferredStore ?: StoreTag.EITHER
        val result = if (existing != null) {
            existing.copy(quantity = existing.quantity + convertShoppingQuantity(quantity, targetUnit, existing.unit)!!,
                inventoryId = existing.inventoryId ?: linked?.inventoryId,
                category = if (existing.category == "Uncategorised") item?.category ?: AisleCategoryMapper.categoryFor(name) else existing.category,
                storeTag = store ?: existing.storeTag)
        } else ShoppingListEntity(name = item?.name ?: name, quantity = quantity, unit = targetUnit, weekId = weekId,
            inventoryId = linked?.inventoryId, category = item?.category ?: AisleCategoryMapper.categoryFor(name),
            storeTag = preferred, source = source)
        require(result.quantity.isFinite() && result.quantity <= 100_000) { "The combined quantity is too large." }
        val saved = if (existing == null) result.copy(shoppingId = insert(result)) else result.also { update(it) }
        if (store != null && item != null) { rememberProductStore(item.itemId, store); rememberStockStore(item.itemId, store) }
        return saved
    }

    @Transaction
    open suspend fun importItems(items: List<ShoppingIngestItem>, source: String, weekId: String, operationId: String): Int {
        if (ingestState("operation:$operationId") != null) return 0
        require(items.size <= 1000)
        items.forEach { upsertShoppingItem(it.name, it.quantity, source, weekId, unit = it.unit) }
        saveIngestState(ShoppingIngestEntity("operation:$operationId"))
        return items.size
    }

    @Transaction
    open suspend fun importKeepItem(key: String, item: ShoppingIngestItem, weekId: String): Boolean {
        val previous = ingestState(key)
        val delta = if (previous?.unit == item.unit.orEmpty()) (item.quantity - previous.quantity).coerceAtLeast(0.0) else item.quantity
        if (previous != null && delta <= 0) return false
        val saved = upsertShoppingItem(item.name, delta, "KEEP_API", weekId, unit = item.unit)
        saveIngestState(ShoppingIngestEntity(key, item.quantity, item.unit.orEmpty(), saved.syncId))
        return true
    }

    @Transaction
    open suspend fun cycleStore(syncId: String) {
        val row = bySyncId(syncId) ?: return
        val next = row.storeTag.next()
        update(row.copy(storeTag = next))
        val itemId = row.inventoryId?.let { batch(it)?.itemId } ?: product(row.name)?.itemId
        itemId?.let { rememberProductStore(it, next); rememberStockStore(it, next) }
    }

    @Query("""SELECT i.* FROM inventory i WHERE i.quantity = 0
        AND i.inventoryId = (SELECT MIN(s.inventoryId) FROM inventory s WHERE s.itemId = i.itemId)
        AND (SELECT COALESCE(SUM(s.quantity), 0) FROM inventory s WHERE s.itemId = i.itemId) = 0
        AND (SELECT COUNT(*) FROM consumption_history c WHERE c.itemId = i.itemId AND c.type = 'FINISHED' AND c.date >= :thirtyDaysAgo) >= 2
        AND NOT EXISTS (SELECT 1 FROM shopping_list l JOIN items p ON p.itemId = i.itemId
            WHERE l.isChecked = 0 AND lower(trim(l.name)) = lower(trim(p.name)))""")
    abstract fun getLikelyRunningLowStaples(thirtyDaysAgo: Long): Flow<List<InventoryEntity>>

    @Transaction
    open suspend fun completeShopAndRestock(weekId: String? = null, store: StoreTag? = null, restock: Boolean = true): Int {
        val sections = sections()
        val recurring = sections.filter { it.recursEveryWeek }.map { it.sectionId }.toSet()
        val metadata = layout().associate { it.layoutKey to it.value }
        val rows = snapshot().filter { it.isChecked && metadata[ShoppingMetadata.completed(it.syncId)] != "true" &&
            (weekId == null || it.weekId == null || it.weekId == weekId || it.sectionId in recurring) &&
            (store == null || it.storeTag == store || it.storeTag == StoreTag.EITHER) }
        val trip = UUID.randomUUID().toString()
        rows.forEach { row ->
            val previous = pendingPurchase(row.syncId)
            require(previous == null || convertShoppingQuantity(row.quantity, row.unit, previous.unit)?.let {
                kotlin.math.abs(it - previous.quantity) < 0.000001 } == true) { "${row.name} changed after receipt import. Review its purchased quantity." }
            val putAwayLocation = if (restock && previous?.stockRecorded != true && row.quantity > 0) this.restock(row) else null
            savePurchase((previous ?: PurchaseRecordEntity(shoppingSyncId = row.syncId, name = row.name, quantity = row.quantity,
                unit = row.unit, stockRecorded = false, completed = false)).copy(completed = true,
                stockRecorded = previous?.stockRecorded == true || restock,
                storageLocation = if (previous?.stockRecorded == true) previous.storageLocation else putAwayLocation))
            archive(ShoppingArchiveEntity(tripId = trip, weekId = weekId ?: row.weekId ?: "A", name = row.name,
                quantity = row.quantity, unit = row.unit, sectionName = sections.firstOrNull { it.sectionId == row.sectionId }?.name ?: "The rest",
                storageLocation = if (previous?.stockRecorded == true) previous.storageLocation else putAwayLocation))
            delete(row)
            listOf(ShoppingMetadata.completed(row.syncId), ShoppingMetadata.baseline(row.syncId), ShoppingMetadata.checkStock(row.syncId)).forEach { removeMetadata(it) }
        }
        return rows.size
    }

    @Transaction open suspend fun removeTickedOnly(weekId: String? = null, store: StoreTag? = null): Int =
        completeShopAndRestock(weekId, store, restock = false)

    private suspend fun restock(row: ShoppingListEntity): String {
        val linked = row.inventoryId?.let { batch(it) }
        val product = linked?.let { productById(it.itemId) } ?: product(row.name)
            ?: ItemEntity(name = row.name, defaultUnit = row.unit, category = row.category, preferredStore = row.storeTag)
                .let { it.copy(itemId = insertProduct(it)) }
        rememberProductStore(product.itemId, row.storeTag)
        val target = (listOfNotNull(linked) + stock(product.itemId)).distinctBy { it.inventoryId }.firstOrNull {
            it.expirationDate == null && (it.inventoryId == linked?.inventoryId || it.storageLocation == InventoryEntity.LOCATION_PANTRY) &&
                classifyExpiry(it.expirationDate) != ExpiryStatus.EXPIRED && convertShoppingQuantity(row.quantity, row.unit, it.unit) != null
        }
        if (target != null) updateStock(target.copy(quantity = target.quantity + convertShoppingQuantity(row.quantity, row.unit, target.unit)!!,
            preferredStore = row.storeTag))
        else insertStock(InventoryEntity(itemId = product.itemId, quantity = row.quantity, unit = row.unit, preferredStore = row.storeTag,
            storageLocation = linked?.storageLocation ?: InventoryEntity.LOCATION_PANTRY))
        return target?.storageLocation ?: linked?.storageLocation ?: InventoryEntity.LOCATION_PANTRY
    }

    data class EmptyChange(val before: InventoryEntity, val after: InventoryEntity, val historyId: Long,
        val shoppingBefore: ShoppingListEntity?, val shoppingAfter: ShoppingListEntity)

    @Transaction open suspend fun markEmptyAndAdd(inventoryId: Long, weekId: String): EmptyChange {
        val stock = batch(inventoryId) ?: error("This stock item changed. Refresh the pantry.")
        require(stock.quantity > 0) { "This item is already empty." }
        val item = productById(stock.itemId) ?: error("This item is no longer available.")
        val recurring = sections().filter { it.recursEveryWeek }.map { it.sectionId }.toSet()
        val before = snapshot().firstOrNull { !it.isChecked && it.name.equals(item.name, true) &&
            (it.weekId == null || it.weekId == weekId || it.sectionId in recurring) && convertShoppingQuantity(1.0, stock.unit, it.unit) != null }
        val event = logConsumption(ConsumptionEntity(itemId = stock.itemId, quantity = stock.quantity, type = ConsumptionType.FINISHED))
        val after = stock.copy(quantity = 0.0)
        updateStock(after)
        val shopping = upsertShoppingItem(item.name, 1.0, "EMPTY_STOCK", weekId, unit = stock.unit)
        return EmptyChange(stock, after, event, before, shopping)
    }

    @Transaction open suspend fun undoEmpty(change: EmptyChange) {
        require(batch(change.after.inventoryId) == change.after && bySyncId(change.shoppingAfter.syncId) == change.shoppingAfter && consumption(change.historyId) != null) {
            "This item changed after the swipe. Review it instead of undoing another person's edit."
        }
        updateStock(change.before)
        deleteConsumption(change.historyId)
        if (change.shoppingBefore == null) delete(change.shoppingAfter) else update(change.shoppingBefore)
    }
}
