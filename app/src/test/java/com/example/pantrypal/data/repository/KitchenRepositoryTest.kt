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
        override suspend fun updateInventory(inventory: InventoryEntity) {}
        override fun getInventoryJoined(): Flow<List<InventoryWithItemMap>> = flowOf(emptyList())
        override suspend fun getAllInventorySnapshot(): List<InventoryEntity> = emptyList()
        override fun getExpiringItems(currentTime: Long): Flow<List<InventoryWithItemMap>> = flowOf(emptyList())
        override suspend fun getExpiringItemsSnapshot(
            dueSoonCutoff: Long,
            limit: Int
        ): List<InventoryWithItemMap> = emptyList()
        override suspend fun getInventoryByBarcode(barcode: String): List<InventoryWithItemMap> = emptyList()
        override suspend fun countInventoryForItem(itemId: Long): Int = 0
        override suspend fun updateStockSettings(itemId: Long, isUsual: Boolean, lowStockThreshold: Double?) {}

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

    class FakeShoppingDao(
        val rows: MutableList<ShoppingItemEntity> = mutableListOf()
    ) : ShoppingDao {
        val archives = mutableListOf<ShoppingArchiveEntity>()
        override fun getAllShoppingItems(): Flow<List<ShoppingItemEntity>> = flowOf(rows.toList())
        override fun getShoppingArchive(): Flow<List<ShoppingArchiveEntity>> = flowOf(emptyList())
        override suspend fun getAllShoppingItemsSnapshot(): List<ShoppingItemEntity> = rows.toList()
        override suspend fun getShoppingArchiveSnapshot(): List<ShoppingArchiveEntity> = emptyList()
        override suspend fun countOpenShoppingItems(): Int = rows.count { !it.isChecked }
        override suspend fun countOpenShoppingItemsForWeek(weekId: String): Int =
            rows.count { !it.isChecked && (it.weekId == null || it.weekId == weekId) }

        override suspend fun insertShoppingItem(item: ShoppingItemEntity): Long {
            val id = if (item.shoppingId > 0) item.shoppingId else (rows.maxOfOrNull { it.shoppingId } ?: 0) + 1
            rows += item.copy(shoppingId = id)
            return id
        }

        override suspend fun updateShoppingItem(item: ShoppingItemEntity) {
            val index = rows.indexOfFirst { it.shoppingId == item.shoppingId }
            if (index >= 0) rows[index] = item
        }

        override suspend fun deleteShoppingItem(item: ShoppingItemEntity) {
            rows.removeAll { it.shoppingId == item.shoppingId }
        }

        override suspend fun insertShoppingArchive(items: List<ShoppingArchiveEntity>) {
            archives += items
        }

        override suspend fun clearShoppingArchive() {
            archives.clear()
        }
        override suspend fun deleteCheckedWeekItems(weekId: String) {}
        override suspend fun resetCheckedRecurringItems() {}
        override suspend fun deleteItemsInSection(sectionId: Long) {}
        override suspend fun deleteItemsInSectionForWeek(sectionId: Long, weekId: String) {}
    }

    class FakeMealDao : MealDao {
        override fun getAllMeals(): Flow<List<MealEntity>> = flowOf(emptyList())
        override suspend fun getAllMealsSnapshot(): List<MealEntity> = emptyList()
        override fun getMealsByWeek(week: String): Flow<List<MealEntity>> = flowOf(emptyList())
        override suspend fun insertMeal(meal: MealEntity) {}
        override suspend fun updateMeal(meal: MealEntity) {}
        override suspend fun deleteMeal(meal: MealEntity) {}
    }

    class FakeMealWeekDao : MealWeekDao {
        override fun getAllWeeks(): Flow<List<MealWeekEntity>> = flowOf(emptyList())
        override suspend fun updateWeek(week: MealWeekEntity) {}
    }

    class FakeShoppingSectionDao : ShoppingSectionDao {
        override fun getAllSections(): Flow<List<ShoppingSectionEntity>> = flowOf(emptyList())
        override suspend fun insertSection(section: ShoppingSectionEntity): Long = 0
        override suspend fun updateSection(section: ShoppingSectionEntity) {}
        override suspend fun deleteSection(section: ShoppingSectionEntity) {}
    }

    class FakeShoppingHistoryDao : ShoppingHistoryDao {
        override fun getHistory(): Flow<List<ShoppingHistoryEntity>> = flowOf(emptyList())
        override suspend fun remember(entry: ShoppingHistoryEntity) {}
    }

    @Test
    fun getRestockSuggestions_callsDaoAndReturnsCandidates() = runBlocking {
        val repo = KitchenRepository(
            FakeItemDao(),
            FakeInventoryDao(),
            FakeConsumptionDao(),
            FakeShoppingDao(),
            FakeMealDao(),
            FakeMealWeekDao(),
            FakeShoppingSectionDao(),
            FakeShoppingHistoryDao()
        )

        val suggestions = repo.getRestockSuggestions(1672531200000L) // Use a fixed timestamp

        // 101L and 102L are candidates. 102L is in stock. So only 101L should be returned.
        assertEquals(1, suggestions.size)
        assertEquals(101L, suggestions[0].itemId)
    }
    @Test
    fun addShoppingItem_mergesUncheckedMatchingNameAndUnit() = runBlocking {
        val shoppingDao = FakeShoppingDao(
            mutableListOf(
                ShoppingItemEntity(
                    shoppingId = 1L,
                    name = "Milk",
                    quantity = 1.0,
                    unit = "pcs",
                    sectionId = ShoppingSectionEntity.ID_THE_REST,
                    weekId = "A"
                )
            )
        )
        val repo = KitchenRepository(
            FakeItemDao(),
            FakeInventoryDao(),
            FakeConsumptionDao(),
            shoppingDao,
            FakeMealDao(),
            FakeMealWeekDao(),
            FakeShoppingSectionDao(),
            FakeShoppingHistoryDao()
        )

        repo.addShoppingItem(
            ShoppingItemEntity(
                name = "milk",
                quantity = 2.0,
                unit = "pcs",
                sectionId = ShoppingSectionEntity.ID_THE_REST,
                weekId = "A"
            )
        )

        assertEquals(1, shoppingDao.rows.size)
        assertEquals("Milk", shoppingDao.rows.single().name)
        assertEquals(3.0, shoppingDao.rows.single().quantity, 0.0)
    }


    @Test
    fun completingShoppingTripArchivesCheckedItems() = runBlocking {
        val shoppingDao = FakeShoppingDao()
        val repo = KitchenRepository(
            FakeItemDao(),
            FakeInventoryDao(),
            FakeConsumptionDao(),
            shoppingDao,
            FakeMealDao(),
            FakeMealWeekDao(),
            FakeShoppingSectionDao(),
            FakeShoppingHistoryDao()
        )
        val checked = ShoppingItemEntity(
            shoppingId = 7L,
            name = "Milk",
            quantity = 2.0,
            unit = "litres",
            isChecked = true,
            sectionId = ShoppingSectionEntity.ID_THE_REST,
            weekId = "A"
        )

        repo.completeShoppingTrip(
            checkedItems = listOf(checked),
            sections = listOf(
                ShoppingSectionEntity(
                    sectionId = ShoppingSectionEntity.ID_THE_REST,
                    name = "The rest",
                    sortOrder = 3,
                    recursEveryWeek = false,
                    systemKey = ShoppingSectionEntity.KEY_THE_REST
                )
            ),
            weekId = "A",
            storageLocation = InventoryEntity.LOCATION_FRIDGE
        )

        assertEquals(1, shoppingDao.archives.size)
        assertEquals("Milk", shoppingDao.archives.single().name)
        assertEquals("A", shoppingDao.archives.single().weekId)
        assertEquals(InventoryEntity.LOCATION_FRIDGE, shoppingDao.archives.single().storageLocation)
    }

}
