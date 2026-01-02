package com.example.pantrypal.data.repository

import com.example.pantrypal.data.dao.ConsumptionDao
import com.example.pantrypal.data.dao.ConsumptionWithItem
import com.example.pantrypal.data.dao.InventoryDao
import com.example.pantrypal.data.dao.ItemDao
import com.example.pantrypal.data.dao.ShoppingDao
import com.example.pantrypal.data.entity.ConsumptionEntity
import com.example.pantrypal.data.entity.ConsumptionType
import com.example.pantrypal.data.entity.InventoryEntity
import com.example.pantrypal.data.entity.ItemEntity
import com.example.pantrypal.data.entity.ShoppingItemEntity
import com.example.pantrypal.data.api.OpenFoodFactsApi
import kotlinx.coroutines.flow.Flow
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class KitchenRepository(
    private val itemDao: ItemDao,
    private val inventoryDao: InventoryDao,
    private val consumptionDao: ConsumptionDao,
    private val shoppingDao: ShoppingDao? = null
) {
    private val api: OpenFoodFactsApi by lazy {
        Retrofit.Builder()
            .baseUrl(OPEN_FOOD_FACTS_API_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenFoodFactsApi::class.java)
    }

    companion object {
        private const val OPEN_FOOD_FACTS_API_BASE_URL = "https://world.openfoodfacts.org/"
    }

    // Items
    val allItems: Flow<List<ItemEntity>> = itemDao.getAllItems()

    suspend fun getItemById(id: Long): ItemEntity? = itemDao.getItemById(id)
    suspend fun getItemByBarcode(barcode: String): ItemEntity? = itemDao.getItemByBarcode(barcode)
    suspend fun insertItem(item: ItemEntity): Long = itemDao.insertItem(item)

    suspend fun getItemByBarcodeFromApi(barcode: String): ItemEntity? {
        return try {
            val response = api.getProduct(barcode)
            val product = response.product
            if (product != null) {
                // Map to temporary ItemEntity (id=0 because it's not in DB yet)
                // Use default unit "pcs" and category "General" as placeholders
                ItemEntity(
                    itemId = 0,
                    name = product.product_name ?: "Unknown Product",
                    barcode = barcode,
                    defaultUnit = "pcs",
                    category = "General",
                    imageUrl = product.image_url
                )
            } else {
                null
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error fetching product from API for barcode: $barcode", e)
            null
        }
    }

    // Inventory
    // Note: This matches the raw query return type in DAO
    val currentInventory = inventoryDao.getInventoryJoined()

    fun getExpiringItems(currentTime: Long) = inventoryDao.getExpiringItems(currentTime)

    suspend fun getInventoryByBarcode(barcode: String) = inventoryDao.getInventoryByBarcode(barcode)

    suspend fun addInventory(inventory: InventoryEntity) = inventoryDao.insertInventory(inventory)
    suspend fun removeInventory(inventory: InventoryEntity) = inventoryDao.deleteInventory(inventory)

    // Consumption
    suspend fun logConsumption(consumption: ConsumptionEntity) = consumptionDao.insertConsumption(consumption)

    suspend fun getUsageHistory(itemId: Long): List<ConsumptionEntity> = consumptionDao.getHistoryForItem(itemId)

    val allConsumptionHistory: Flow<List<ConsumptionWithItem>> = consumptionDao.getAllHistoryWithItemFlow()

    // Shopping List
    val shoppingList: Flow<List<ShoppingItemEntity>> = shoppingDao?.getAllShoppingItems() ?: kotlinx.coroutines.flow.flowOf(emptyList())

    suspend fun addShoppingItem(item: ShoppingItemEntity) = shoppingDao?.insertShoppingItem(item)
    suspend fun updateShoppingItem(item: ShoppingItemEntity) = shoppingDao?.updateShoppingItem(item)
    suspend fun deleteShoppingItem(item: ShoppingItemEntity) = shoppingDao?.deleteShoppingItem(item)
    suspend fun deleteCheckedShoppingItems() = shoppingDao?.deleteCheckedItems()

    // Smart Restock Logic
    suspend fun getRestockSuggestions(currentTime: Long): List<ItemEntity> {
        val history = consumptionDao.getAllHistory()
        // Group by itemId
        val itemHistory = history.filter { it.type == ConsumptionType.FINISHED }
            .groupBy { it.itemId }

        val candidateIds = mutableListOf<Long>()

        for ((itemId, events) in itemHistory) {
            if (events.size < 2) continue // Need at least 2 events to calculate interval

            // Calculate average interval
            val sortedDates = events.map { it.date }.sorted()
            var totalInterval = 0L
            for (i in 0 until sortedDates.size - 1) {
                totalInterval += (sortedDates[i+1] - sortedDates[i])
            }
            val avgInterval = totalInterval / (sortedDates.size - 1)
            val lastConsumed = sortedDates.last()

            // Check if due for restock (LastConsumed + AvgInterval < CurrentTime)
            if (lastConsumed + avgInterval < currentTime) {
                candidateIds.add(itemId)
            }
        }

        if (candidateIds.isEmpty()) return emptyList()

        // Batch check inventory
        // We only want items that have 0 stock.
        // So we get IDs of items that ARE in stock, and exclude them.
        val inStockIds = inventoryDao.getInStockItemIds(candidateIds)
        val outOfStockIds = candidateIds.filter { !inStockIds.contains(it) }

        if (outOfStockIds.isEmpty()) return emptyList()

        // Batch fetch items
        return itemDao.getItemsByIds(outOfStockIds)
    }

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
