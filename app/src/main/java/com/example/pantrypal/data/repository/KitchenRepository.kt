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
import com.example.pantrypal.data.entity.ShoppingArchiveEntity
import com.example.pantrypal.data.entity.ShoppingItemEntity
import com.example.pantrypal.data.entity.MealEntity
import com.example.pantrypal.data.entity.MealWeekEntity
import com.example.pantrypal.data.entity.ShoppingHistoryEntity
import com.example.pantrypal.data.entity.ShoppingSectionEntity
import com.example.pantrypal.data.api.OpenFoodFactsApi
import com.example.pantrypal.util.normalizeShoppingName
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Locale
import java.util.UUID

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

    private val shoppingMutationMutex = Mutex()

    companion object {
        private const val OPEN_FOOD_FACTS_API_BASE_URL = "https://world.openfoodfacts.org/"
    }

    // Items
    fun getAllItems(): Flow<List<ItemEntity>> = itemDao.getAllItems()

    suspend fun getItemById(id: Long): ItemEntity? = itemDao.getItemById(id)
    suspend fun getItemByBarcode(barcode: String): ItemEntity? = itemDao.getItemByBarcode(barcode)
    suspend fun insertItem(item: ItemEntity): Long {
        requireValidItem(item)
        return itemDao.insertItem(item)
    }

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
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.e("KitchenRepository", "Error fetching product from API: $e")
            null
        }
    }

    // Inventory
    val currentInventory = inventoryDao.getInventoryJoined()

    fun getExpiringItems(dueSoonCutoff: Long) = inventoryDao.getExpiringItems(dueSoonCutoff)

    suspend fun getInventoryByBarcode(barcode: String) = inventoryDao.getInventoryByBarcode(barcode)

    suspend fun addInventory(inventory: InventoryEntity) {
        requireValidInventory(inventory)
        inventoryDao.insertInventory(inventory)
    }

    suspend fun updateInventory(inventory: InventoryEntity) {
        requireValidInventory(inventory)
        inventoryDao.updateInventory(inventory)
    }

    suspend fun removeInventory(inventory: InventoryEntity) = inventoryDao.deleteInventory(inventory)

    suspend fun updateStockSettings(itemId: Long, isUsual: Boolean, lowStockThreshold: Double?) {
        requireValidThreshold(lowStockThreshold)
        inventoryDao.updateStockSettings(itemId, isUsual, lowStockThreshold)
    }

    suspend fun getInventorySnapshot() = inventoryDao.getAllInventorySnapshot()

    // Consumption
    suspend fun logConsumption(consumption: ConsumptionEntity) {
        require(consumption.quantity.isFinite() && consumption.quantity >= 0.0) {
            "Consumption quantity must be finite and non-negative."
        }
        consumptionDao.insertConsumption(consumption)
    }

    suspend fun getUsageHistory(itemId: Long): List<ConsumptionEntity> = consumptionDao.getHistoryForItem(itemId)

    val allConsumptionHistory: Flow<List<ConsumptionWithItem>> = consumptionDao.getAllHistoryWithItemFlow()

    // Shopping List
    val shoppingList: Flow<List<ShoppingItemEntity>> = shoppingDao.getAllShoppingItems()
    val shoppingSections: Flow<List<ShoppingSectionEntity>> = shoppingSectionDao.getAllSections()
    val shoppingHistory: Flow<List<ShoppingHistoryEntity>> = shoppingHistoryDao.getHistory()
    val shoppingArchive: Flow<List<ShoppingArchiveEntity>> = shoppingDao.getShoppingArchive()

    suspend fun countOpenShoppingItemsForWeek(weekId: String): Int {
        require(weekId.isNotBlank()) { "Shopping week must not be blank." }
        return shoppingDao.countOpenShoppingItemsForWeek(weekId)
    }

    suspend fun getShoppingArchiveSnapshot(): List<ShoppingArchiveEntity> =
        shoppingDao.getShoppingArchiveSnapshot()

    suspend fun addShoppingItem(item: ShoppingItemEntity) {
        requireValidShoppingItem(item)
        shoppingMutationMutex.withLock {
            val recurringSectionIds = shoppingSectionDao.getAllSections()
                .first()
                .filter { it.recursEveryWeek }
                .mapTo(mutableSetOf()) { it.sectionId }
            val normalizedName = normalizeShoppingName(item.name)
            val existing = shoppingDao.getAllShoppingItemsSnapshot().firstOrNull { candidate ->
                !candidate.isChecked &&
                    candidate.sectionId == item.sectionId &&
                    normalizeShoppingName(candidate.name) == normalizedName &&
                    candidate.unit.equals(item.unit, ignoreCase = true) &&
                    (
                        item.sectionId in recurringSectionIds ||
                            candidate.weekId == item.weekId
                        )
            }
            if (existing == null) {
                shoppingDao.insertShoppingItem(item)
            } else {
                val combinedQuantity = existing.quantity + item.quantity
                if (combinedQuantity.isFinite()) {
                    shoppingDao.updateShoppingItem(existing.copy(quantity = combinedQuantity))
                } else {
                    shoppingDao.insertShoppingItem(item)
                }
            }
        }
    }

    suspend fun updateShoppingItem(item: ShoppingItemEntity) {
        requireValidShoppingItem(item)
        shoppingDao.updateShoppingItem(item)
    }

    suspend fun deleteShoppingItem(item: ShoppingItemEntity) = shoppingDao.deleteShoppingItem(item)

    /**
     * Completion is intentionally archive-first: the active list can be cleaned up without
     * losing what was bought or where it was put away.
     */
    suspend fun completeShoppingTrip(
        checkedItems: List<ShoppingItemEntity>,
        sections: List<ShoppingSectionEntity>,
        weekId: String,
        storageLocation: String?
    ) {
        require(weekId.isNotBlank()) { "Shopping week must not be blank." }
        storageLocation?.let {
            require(it.isNotBlank()) { "Inventory storage location must not be blank." }
        }
        checkedItems.forEach(::requireValidShoppingItem)

        val recurringSectionIds = sections
            .filter { it.recursEveryWeek }
            .mapTo(mutableSetOf()) { it.sectionId }
        val eligibleItems = checkedItems.filter {
            it.isChecked &&
                (it.sectionId in recurringSectionIds || it.weekId == null || it.weekId == weekId)
        }
        val sectionNames = sections.associate { it.sectionId to it.name }
        val completedAt = System.currentTimeMillis()
        val tripId = UUID.randomUUID().toString()
        val archivedItems = eligibleItems.map { item ->
            ShoppingArchiveEntity(
                tripId = tripId,
                weekId = weekId,
                name = item.name.trim(),
                quantity = item.quantity,
                unit = item.unit.trim(),
                sectionName = sectionNames[item.sectionId]?.trim().orEmpty().ifBlank { "Other" },
                completedAt = completedAt,
                storageLocation = storageLocation?.trim()?.takeIf(String::isNotBlank)
            )
        }
        shoppingDao.archiveAndClearCheckedItems(archivedItems, weekId)
    }

    suspend fun archiveAndClearCheckedShoppingItems(weekId: String) {
        require(weekId.isNotBlank()) { "Shopping week must not be blank." }
        val sections = shoppingSections.first()
        val recurringSectionIds = sections
            .filter { it.recursEveryWeek }
            .mapTo(mutableSetOf()) { it.sectionId }
        val checkedItems = shoppingList.first().filter {
            it.isChecked &&
                (it.sectionId in recurringSectionIds || it.weekId == null || it.weekId == weekId)
        }
        completeShoppingTrip(
            checkedItems = checkedItems,
            sections = sections,
            weekId = weekId,
            storageLocation = null
        )
    }

    suspend fun deleteCheckedShoppingItems(weekId: String) {
        archiveAndClearCheckedShoppingItems(weekId)
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
    suspend fun insertShoppingSection(section: ShoppingSectionEntity) {
        requireValidShoppingSection(section)
        shoppingSectionDao.insertSection(section)
    }

    suspend fun updateShoppingSection(section: ShoppingSectionEntity) {
        requireValidShoppingSection(section)
        shoppingSectionDao.updateSection(section)
    }
    suspend fun deleteShoppingSection(section: ShoppingSectionEntity) = shoppingSectionDao.deleteSection(section)

    suspend fun putAwayShoppingItems(
        shoppingItems: List<ShoppingItemEntity>,
        storageLocation: String
    ) {
        require(storageLocation.isNotBlank()) { "Inventory storage location must not be blank." }
        shoppingItems.forEach(::requireValidShoppingItem)
        val purchases = shoppingItems.filter { it.quantity > 0 }
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
    suspend fun insertMeal(meal: MealEntity) {
        requireValidMeal(meal)
        mealDao.insertMeal(meal)
    }

    suspend fun updateMeal(meal: MealEntity) {
        requireValidMeal(meal)
        mealDao.updateMeal(meal)
    }
    suspend fun deleteMeal(meal: MealEntity) = mealDao.deleteMeal(meal)
    suspend fun updateMealWeek(week: MealWeekEntity) {
        require(week.weekId.isNotBlank()) { "Meal week ID must not be blank." }
        require(week.name.isNotBlank()) { "Meal week name must not be blank." }
        require(week.sortOrder >= 0) { "Meal week order must not be negative." }
        mealWeekDao.updateWeek(week)
    }


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

    private fun requireValidItem(item: ItemEntity) {
        require(item.name.isNotBlank()) { "Item name must not be blank." }
        require(item.defaultUnit.isNotBlank()) { "Item unit must not be blank." }
        require(item.category.isNotBlank()) { "Item category must not be blank." }
        requireValidThreshold(item.lowStockThreshold)
    }

    private fun requireValidInventory(inventory: InventoryEntity) {
        require(inventory.quantity.isFinite() && inventory.quantity >= 0.0) {
            "Inventory quantity must be finite and non-negative."
        }
        require(inventory.unit.isNotBlank()) { "Inventory unit must not be blank." }
        require(inventory.storageLocation.isNotBlank()) {
            "Inventory storage location must not be blank."
        }
    }

    private fun requireValidShoppingItem(item: ShoppingItemEntity) {
        require(item.name.isNotBlank()) { "Shopping item name must not be blank." }
        require(item.quantity.isFinite() && item.quantity >= 0.0) {
            "Shopping quantity must be finite and non-negative."
        }
        require(item.unit.isNotBlank()) { "Shopping unit must not be blank." }
        item.weekId?.let { week ->
            require(week.isNotBlank()) { "Shopping week must not be blank." }
        }
    }

    private fun requireValidShoppingSection(section: ShoppingSectionEntity) {
        require(section.name.isNotBlank()) { "Shopping section name must not be blank." }
        require(section.sortOrder >= 0) { "Shopping section order must not be negative." }
    }

    private fun requireValidMeal(meal: MealEntity) {
        require(meal.name.isNotBlank()) { "Meal name must not be blank." }
        require(meal.week.isNotBlank()) { "Meal week must not be blank." }
        require(meal.mealSlot.isNotBlank()) { "Meal slot must not be blank." }
        require(meal.dayOfWeek in 1..7) { "Meal day must be between 1 and 7." }
        require(meal.servings.isFinite() && meal.servings > 0.0) {
            "Meal servings must be finite and greater than zero."
        }
        meal.ingredients.forEachIndexed { index, ingredient ->
            require(ingredient.isNotBlank()) {
                "Meal ingredient $index must not be blank."
            }
        }
    }

    private fun requireValidThreshold(threshold: Double?) {
        threshold?.let {
            require(it.isFinite() && it >= 0.0) {
                "Low-stock threshold must be finite and non-negative."
            }
        }
    }
}

data class ExportData(
    val items: List<ItemEntity>,
    val inventory: List<InventoryEntity>,
    val history: List<ConsumptionEntity>
)
