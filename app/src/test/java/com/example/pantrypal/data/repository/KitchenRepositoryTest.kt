package com.example.pantrypal.data.repository

import com.example.pantrypal.data.dao.*
import com.example.pantrypal.data.entity.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class KitchenRepositoryTest {

    // Fakes
    class FakeConsumptionDao : ConsumptionDao {
        override suspend fun insertConsumption(consumption: ConsumptionEntity) {}
        override fun getConsumptionByType(type: ConsumptionType): Flow<List<ConsumptionEntity>> = flowOf(emptyList())
        override suspend fun getHistoryForItem(itemId: Long): List<ConsumptionEntity> = emptyList()
        override fun getAllHistoryWithItemFlow(): Flow<List<ConsumptionWithItem>> = flowOf(emptyList())
        override fun getAllHistoryFlow(): Flow<List<ConsumptionEntity>> = flowOf(emptyList())
        override suspend fun getAllHistory(): List<ConsumptionEntity> = emptyList()

        // The important one
        override suspend fun getRestockCandidates(currentTime: Long, type: ConsumptionType): List<Long> {
            // Assume logic is correct in SQL, just return test data
            return listOf(101L, 102L)
        }
    }

    class FakeInventoryDao : InventoryDao {
        override suspend fun insertInventory(inventory: InventoryEntity): Long = 0
        override suspend fun deleteInventory(inventory: InventoryEntity) {}
        override fun getInventoryJoined(): Flow<List<InventoryWithItemMap>> = flowOf(emptyList())
        override suspend fun getAllInventorySnapshot(): List<InventoryEntity> = emptyList()
        override fun getExpiringItems(currentTime: Long): Flow<List<InventoryWithItemMap>> = flowOf(emptyList())
        override suspend fun getInventoryByBarcode(barcode: String): List<InventoryWithItemMap> = emptyList()
        override suspend fun countInventoryForItem(itemId: Long): Int = 0

        override suspend fun getInStockItemIds(itemIds: List<Long>): List<Long> {
            // Assume 102L is in stock, so only 101L is needed
            return listOf(102L)
        }
    }

    class FakeItemDao : ItemDao {
        override fun getAllItems(): Flow<List<ItemEntity>> = flowOf(emptyList())
        override suspend fun getAllItemsSnapshot(): List<ItemEntity> = emptyList()
        override suspend fun getItemById(id: Long): ItemEntity? = null
        override suspend fun getItemByBarcode(barcode: String): ItemEntity? = null
        override suspend fun insertItem(item: ItemEntity): Long = 0

        override suspend fun getItemsByIds(ids: List<Long>): List<ItemEntity> {
            // Should be called with 101L (since 102L is in stock)
            return ids.map {
                ItemEntity(itemId = it, name = "Item $it", barcode = "$it", defaultUnit = "pcs", category = "Gen")
            }
        }
    }

    class FakeShoppingDao : ShoppingDao {
        override fun getAllShoppingItems(): Flow<List<ShoppingItemEntity>> = flowOf(emptyList())
        override suspend fun insertShoppingItem(item: ShoppingItemEntity): Long = 0
        override suspend fun updateShoppingItem(item: ShoppingItemEntity) {}
        override suspend fun deleteShoppingItem(item: ShoppingItemEntity) {}
        override suspend fun deleteCheckedItems() {}
    }

    class FakeMealDao : MealDao {
        override fun getAllMeals(): Flow<List<MealEntity>> = flowOf(emptyList())
        override fun getMealsByWeek(week: String): Flow<List<MealEntity>> = flowOf(emptyList())
        override suspend fun insertMeal(meal: MealEntity) {}
        override suspend fun deleteMeal(meal: MealEntity) {}
    }

    @Test
    fun getRestockSuggestions_callsDaoAndReturnsCandidates() = runBlocking {
        val repo = KitchenRepository(
            FakeItemDao(),
            FakeInventoryDao(),
            FakeConsumptionDao(),
            FakeShoppingDao(),
            FakeMealDao()
        )

        val suggestions = repo.getRestockSuggestions(1672531200000L) // Use a fixed timestamp

        // 101L and 102L are candidates. 102L is in stock. So only 101L should be returned.
        assertEquals(1, suggestions.size)
        assertEquals(101L, suggestions[0].itemId)
    }
}
