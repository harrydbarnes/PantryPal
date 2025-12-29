package com.example.pantrypal.data.repository

import com.example.pantrypal.data.dao.ConsumptionDao
import com.example.pantrypal.data.dao.InventoryDao
import com.example.pantrypal.data.dao.ItemDao
import com.example.pantrypal.data.entity.ConsumptionEntity
import com.example.pantrypal.data.entity.ConsumptionType
import com.example.pantrypal.data.entity.InventoryEntity
import com.example.pantrypal.data.entity.ItemEntity
import kotlinx.coroutines.flow.Flow

class KitchenRepository(
    private val itemDao: ItemDao,
    private val inventoryDao: InventoryDao,
    private val consumptionDao: ConsumptionDao
) {
    // Items
    val allItems: Flow<List<ItemEntity>> = itemDao.getAllItems()

    suspend fun getItemById(id: Long): ItemEntity? = itemDao.getItemById(id)
    suspend fun getItemByBarcode(barcode: String): ItemEntity? = itemDao.getItemByBarcode(barcode)
    suspend fun insertItem(item: ItemEntity): Long = itemDao.insertItem(item)

    // Inventory
    // Note: This matches the raw query return type in DAO
    val currentInventory = inventoryDao.getInventoryJoined()

    suspend fun addInventory(inventory: InventoryEntity) = inventoryDao.insertInventory(inventory)
    suspend fun removeInventory(inventory: InventoryEntity) = inventoryDao.deleteInventory(inventory)

    // Consumption
    suspend fun logConsumption(consumption: ConsumptionEntity) = consumptionDao.insertConsumption(consumption)

    suspend fun getUsageHistory(itemId: Long): List<ConsumptionEntity> = consumptionDao.getHistoryForItem(itemId)

    // Export Data (Fetch all)
    suspend fun getAllDataForExport(): ExportData {
        return ExportData(
            items = itemDao.getAllItemsSnapshot(),
            inventory = inventoryDao.getAllInventorySnapshot(),
            history = consumptionDao.getAllHistory()
        )
    }
}

data class ExportData(
    val items: List<ItemEntity>,
    val inventory: List<InventoryEntity>,
    val history: List<ConsumptionEntity>
)
