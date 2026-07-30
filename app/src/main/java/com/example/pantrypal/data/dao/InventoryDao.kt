package com.example.pantrypal.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.pantrypal.data.entity.InventoryEntity
import com.example.pantrypal.data.entity.ItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventory(inventory: InventoryEntity): Long

    @Delete
    suspend fun deleteInventory(inventory: InventoryEntity)

    @Update
    suspend fun updateInventory(inventory: InventoryEntity)

    @Query("SELECT inventory.*, items.name, items.barcode, items.defaultUnit, items.category, items.isVegetarian, items.isGlutenFree, items.isUsual, items.lowStockThreshold, items.imageUrl, items.createdAt FROM inventory INNER JOIN items ON inventory.itemId = items.itemId")
    fun getInventoryJoined(): Flow<List<InventoryWithItemMap>>

    @Query("SELECT * FROM inventory")
    suspend fun getAllInventorySnapshot(): List<InventoryEntity>

    @Query("SELECT inventory.*, items.name, items.barcode, items.defaultUnit, items.category, items.isVegetarian, items.isGlutenFree, items.isUsual, items.lowStockThreshold, items.imageUrl, items.createdAt FROM inventory INNER JOIN items ON inventory.itemId = items.itemId WHERE inventory.expirationDate IS NOT NULL AND inventory.expirationDate < :dueSoonCutoff ORDER BY inventory.expirationDate ASC")
    fun getExpiringItems(dueSoonCutoff: Long): Flow<List<InventoryWithItemMap>>

    @Query("SELECT inventory.*, items.name, items.barcode, items.defaultUnit, items.category, items.isVegetarian, items.isGlutenFree, items.isUsual, items.lowStockThreshold, items.imageUrl, items.createdAt FROM inventory INNER JOIN items ON inventory.itemId = items.itemId WHERE items.barcode = :barcode")
    suspend fun getInventoryByBarcode(barcode: String): List<InventoryWithItemMap>

    @Query("SELECT COUNT(*) FROM inventory WHERE itemId = :itemId")
    suspend fun countInventoryForItem(itemId: Long): Int

    @Query("SELECT DISTINCT itemId FROM inventory WHERE itemId IN (:itemIds)")
    suspend fun getInStockItemIds(itemIds: List<Long>): List<Long>

    @Query("UPDATE items SET isUsual = :isUsual, lowStockThreshold = :lowStockThreshold WHERE itemId = :itemId")
    suspend fun updateStockSettings(itemId: Long, isUsual: Boolean, lowStockThreshold: Double?)
}

// Helper class for the join query
data class InventoryWithItemMap(
    val inventoryId: Long,
    val itemId: Long,
    val quantity: Double,
    val unit: String,
    val addedDate: Long,
    val expirationDate: Long?,
    val storageLocation: String = InventoryEntity.LOCATION_PANTRY,
    val isOpened: Boolean = false,
    // Item fields
    val name: String,
    val barcode: String?,
    val defaultUnit: String,
    val category: String,
    val isVegetarian: Boolean,
    val isGlutenFree: Boolean,
    val isUsual: Boolean,
    val lowStockThreshold: Double? = null,
    val imageUrl: String?,
    val createdAt: Long
)
