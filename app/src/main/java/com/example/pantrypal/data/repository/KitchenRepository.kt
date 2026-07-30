package com.example.pantrypal.data.repository

import com.example.pantrypal.data.dao.ConsumptionDao
import com.example.pantrypal.data.dao.ConsumptionWithItem
import com.example.pantrypal.data.dao.InventoryDao
import com.example.pantrypal.data.dao.ItemDao
import com.example.pantrypal.data.dao.ShoppingDao
import com.example.pantrypal.data.dao.MealDao
import com.example.pantrypal.data.dao.MealWeekDao
import com.example.pantrypal.data.dao.ShoppingHistoryDao
import com.example.pantrypal.data.dao.ShoppingSectionDao
import com.example.pantrypal.data.entity.ConsumptionEntity
import com.example.pantrypal.data.entity.ConsumptionType
import com.example.pantrypal.data.entity.InventoryEntity
import com.example.pantrypal.data.entity.ItemEntity
import com.example.pantrypal.data.entity.ShoppingItemEntity
import com.example.pantrypal.data.entity.MealEntity
import com.example.pantrypal.data.entity.MealWeekEntity
import com.example.pantrypal.data.entity.ShoppingHistoryEntity
import com.example.pantrypal.data.entity.ShoppingSectionEntity
import com.example.pantrypal.data.api.OpenFoodFactsApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Locale

class KitchenRepository(
    private val itemDao: ItemDao,
    private val inventoryDao: InventoryDao,
    private val consumptionDao: ConsumptionDao,
    private val shoppingDao: ShoppingDao,
    private val mealDao: MealDao,
    private val mealWeekDao: MealWeekDao,
    private val shoppingSectionDao: ShoppingSectionDao,
    private val shoppingHistoryDao: ShoppingHistoryDao
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
    fun getAllItems(): Flow<List<ItemEntity>> = itemDao.getAllItems()

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
            android.util.Log.e("KitchenRepository", "Error fetching product from API: $e")
            null
        }
    }

    // Inventory
    val currentInventory = inventoryDao.getInventoryJoined()

    fun getExpiringItems(dueSoonCutoff: Long) = inventoryDao.getExpiringItems(dueSoonCutoff)

    suspend fun getInventoryByBarcode(barcode: String) = inventoryDao.getInventoryByBarcode(barcode)

    suspend fun addInventory(inventory: InventoryEntity) = inventoryDao.insertInventory(inventory)
    suspend fun updateInventory(inventory: InventoryEntity) = inventoryDao.updateInventory(inventory)
    suspend fun removeInventory(inventory: InventoryEntity) = inventoryDao.deleteInventory(inventory)
    suspend fun updateStockSettings(itemId: Long, isUsual: Boolean, lowStockThreshold: Double?) =
        inventoryDao.updateStockSettings(itemId, isUsual, lowStockThreshold)

    suspend fun getInventorySnapshot() = inventoryDao.getAllInventorySnapshot()

    // Consumption
    suspend fun logConsumption(consumption: ConsumptionEntity) = consumptionDao.insertConsumption(consumption)

    suspend fun getUsageHistory(itemId: Long): List<ConsumptionEntity> = consumptionDao.getHistoryForItem(itemId)

    val allConsumptionHistory: Flow<List<ConsumptionWithItem>> = consumptionDao.getAllHistoryWithItemFlow()

    // Shopping List
    val shoppingList: Flow<List<ShoppingItemEntity>> = shoppingDao.getAllShoppingItems()
    val shoppingSections: Flow<List<ShoppingSectionEntity>> = shoppingSectionDao.getAllSections()
    val shoppingHistory: Flow<List<ShoppingHistoryEntity>> = shoppingHistoryDao.getHistory()

    suspend fun addShoppingItem(item: ShoppingItemEntity) = shoppingDao.insertShoppingItem(item)
    suspend fun updateShoppingItem(item: ShoppingItemEntity) = shoppingDao.updateShoppingItem(item)
    suspend fun deleteShoppingItem(item: ShoppingItemEntity) = shoppingDao.deleteShoppingItem(item)
    suspend fun deleteCheckedShoppingItems(weekId: String) {
        shoppingDao.deleteCheckedWeekItems(weekId)
        shoppingDao.resetCheckedRecurringItems()
    }
    suspend fun deleteShoppingItemsInSection(sectionId: Long) = shoppingDao.deleteItemsInSection(sectionId)
    suspend fun deleteShoppingItemsInSectionForWeek(sectionId: Long, weekId: String) =
        shoppingDao.deleteItemsInSectionForWeek(sectionId, weekId)
    suspend fun rememberShoppingItem(name: String) {
        val trimmed = name.trim()
        if (trimmed.isNotEmpty()) {
            shoppingHistoryDao.remember(
                ShoppingHistoryEntity(
                    normalizedName = trimmed.lowercase(Locale.getDefault()),
                    displayName = trimmed
                )
            )
        }
    }
    suspend fun insertShoppingSection(section: ShoppingSectionEntity) = shoppingSectionDao.insertSection(section)
    suspend fun updateShoppingSection(section: ShoppingSectionEntity) = shoppingSectionDao.updateSection(section)
    suspend fun deleteShoppingSection(section: ShoppingSectionEntity) = shoppingSectionDao.deleteSection(section)

    suspend fun putAwayShoppingItems(
        shoppingItems: List<ShoppingItemEntity>,
        storageLocation: String
    ) {
        val purchases = shoppingItems.filter {
            it.name.isNotBlank() && it.quantity > 0
        }
        if (purchases.isEmpty()) return

        val knownItems = itemDao.getAllItemsSnapshot().toMutableList()
        val inventory = inventoryDao.getAllInventorySnapshot().toMutableList()
        purchases.forEach { shoppingItem ->
            val item = knownItems.firstOrNull { it.name.equals(shoppingItem.name, ignoreCase = true) }
                ?: ItemEntity(
                    name = shoppingItem.name.trim(),
                    defaultUnit = shoppingItem.unit,
                    category = if (storageLocation == InventoryEntity.LOCATION_HOUSEHOLD) "Household" else "General"
                ).let { newItem ->
                    val id = itemDao.insertItem(newItem)
                    newItem.copy(itemId = id).also(knownItems::add)
                }

            val existingBatch = inventory.firstOrNull {
                it.itemId == item.itemId &&
                    it.unit.equals(shoppingItem.unit, ignoreCase = true) &&
                    it.storageLocation == storageLocation &&
                    it.expirationDate == null
            }
            if (existingBatch != null) {
                val updated = existingBatch.copy(quantity = existingBatch.quantity + shoppingItem.quantity)
                inventoryDao.updateInventory(updated)
                inventory[inventory.indexOf(existingBatch)] = updated
            } else {
                val newInventory = InventoryEntity(
                    itemId = item.itemId,
                    quantity = shoppingItem.quantity,
                    unit = shoppingItem.unit,
                    storageLocation = storageLocation
                )
                val inventoryId = inventoryDao.insertInventory(newInventory)
                inventory += newInventory.copy(inventoryId = inventoryId)
            }
        }
    }

    // Meals
    val allMeals: Flow<List<MealEntity>> = mealDao.getAllMeals()
    val mealWeeks: Flow<List<MealWeekEntity>> = mealWeekDao.getAllWeeks()
    fun getMealsByWeek(week: String): Flow<List<MealEntity>> = mealDao.getMealsByWeek(week)
    suspend fun insertMeal(meal: MealEntity) = mealDao.insertMeal(meal)
    suspend fun updateMeal(meal: MealEntity) = mealDao.updateMeal(meal)
    suspend fun deleteMeal(meal: MealEntity) = mealDao.deleteMeal(meal)
    suspend fun updateMealWeek(week: MealWeekEntity) = mealWeekDao.updateWeek(week)


    // Smart Restock Logic
    suspend fun getRestockSuggestions(currentTime: Long): List<ItemEntity> {
        val candidateIds = consumptionDao.getRestockCandidates(currentTime)
        val inStockCandidateIds = if (candidateIds.isEmpty()) {
            emptyList()
        } else {
            inventoryDao.getInStockItemIds(candidateIds)
        }
        val consumedAndOutOfStock = if (candidateIds.isEmpty()) {
            emptyList()
        } else {
            itemDao.getItemsByIds(candidateIds.filterNot(inStockCandidateIds::contains))
        }

        val inventoryTotals = inventoryDao.getAllInventorySnapshot()
            .groupBy { it.itemId }
            .mapValues { (_, batches) -> batches.sumOf { it.quantity } }
        val usualAndLow = itemDao.getAllItemsSnapshot().filter { item ->
            item.isUsual && (inventoryTotals[item.itemId] ?: 0.0) <= (item.lowStockThreshold ?: 0.0)
        }

        return (usualAndLow + consumedAndOutOfStock).distinctBy { it.itemId }
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
