package com.example.pantrypal.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.pantrypal.data.entity.InventoryEntity
import com.example.pantrypal.data.entity.ItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventory(inventory: InventoryEntity): Long

    @Delete
    suspend fun deleteInventory(inventory: InventoryEntity)

    // For manual mapping if Room can't figure out the JOIN return type directly without @Relation
    // But direct query is often cleaner for simple joins
    @Query("SELECT inventory.*, items.name, items.barcode, items.defaultUnit, items.category, items.isVegetarian, items.isGlutenFree, items.isUsual, items.createdAt FROM inventory INNER JOIN items ON inventory.itemId = items.itemId")
    fun getInventoryJoined(): Flow<List<InventoryWithItemMap>>

    @Query("SELECT * FROM inventory")
    suspend fun getAllInventorySnapshot(): List<InventoryEntity>
}

// Helper class for the join query
data class InventoryWithItemMap(
    val inventoryId: Long,
    val itemId: Long,
    val quantity: Double,
    val unit: String,
    val addedDate: Long,
    val expirationDate: Long?,
    // Item fields
    val name: String,
    val barcode: String?,
    val defaultUnit: String,
    val category: String,
    val isVegetarian: Boolean,
    val isGlutenFree: Boolean,
    val isUsual: Boolean,
    val createdAt: Long
)
