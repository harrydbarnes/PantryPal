package com.example.pantrypal

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.pantrypal.data.database.KitchenDatabase
import com.example.pantrypal.data.entity.*
import com.example.pantrypal.data.repository.KitchenRepository
import kotlinx.coroutines.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ShoppingCompletionTest {
    private lateinit var db: KitchenDatabase
    private lateinit var repo: KitchenRepository
    @Before fun setup() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), KitchenDatabase::class.java).build()
        repo = KitchenRepository(db.itemDao(), db.inventoryDao(), db.consumptionDao(), db.shoppingDao(),
            db.mealDao(), db.mealWeekDao(), db.shoppingSectionDao(), db.shoppingHistoryDao(), db)
        db.shoppingSectionDao().insertSection(ShoppingSectionEntity(1, "Regulars", 0, true))
        db.shoppingSectionDao().insertSection(ShoppingSectionEntity(3, "Other", 1, false))
        Unit
    }
    @After fun close() = db.close()

    @Test fun finishIsIdempotentAndRegularsResetOnlyWhenRequested() = runBlocking {
        val regular = ShoppingItemEntity(name = "Milk", quantity = 2.0, unit = "l", isChecked = true, sectionId = 1)
        db.shoppingDao().insertShoppingItem(regular)
        val results = coroutineScope { (1..2).map {
            async(Dispatchers.IO) { repo.finishShopping("A", mapOf(regular.syncId to "Fridge"), "Pantry", true) }
        }.awaitAll() }
        assertEquals(1, results.sum())
        assertEquals(1, db.shoppingDao().getShoppingArchiveSnapshot().size)
        assertTrue(db.shoppingDao().getAllShoppingItemsSnapshot().single().isChecked)
        assertEquals(2.0, db.inventoryDao().getAllInventorySnapshot().single().quantity, 0.0)
        assertEquals("Fridge", db.inventoryDao().getAllInventorySnapshot().single().storageLocation)
        repo.prepareNextShop()
        assertFalse(db.shoppingDao().getAllShoppingItemsSnapshot().single().isChecked)
        assertEquals(1, db.shoppingDao().getShoppingArchiveSnapshot().size)
    }
    @Test fun perItemLocationsAndNoPantryOptionAreRespected() = runBlocking {
        val milk = ShoppingItemEntity(name = "Milk", isChecked = true, sectionId = 3, weekId = "A")
        val peas = ShoppingItemEntity(name = "Peas", isChecked = true, sectionId = 3, weekId = "A")
        db.shoppingDao().insertShoppingItem(milk); db.shoppingDao().insertShoppingItem(peas)
        assertEquals(2, repo.finishShopping("A", mapOf(milk.syncId to "Fridge", peas.syncId to "Freezer"), "Pantry", true))
        assertEquals(setOf("Fridge", "Freezer"), db.inventoryDao().getAllInventorySnapshot().map { it.storageLocation }.toSet())
        val soap = ShoppingItemEntity(name = "Soap", isChecked = true, sectionId = 3, weekId = "A")
        db.shoppingDao().insertShoppingItem(soap)
        assertEquals(1, repo.finishShopping("A", emptyMap(), "Pantry", false))
        assertEquals(2, db.inventoryDao().getAllInventorySnapshot().size)
        assertNull(db.shoppingDao().getShoppingArchiveSnapshot().first { it.name == "Soap" }.storageLocation)
    }
    @Test fun quickAddUndoCannotOverwriteAnotherPersonsEdit() = runBlocking {
        val change = repo.quickAddShoppingItem(ShoppingItemEntity(name = "Milk", quantity = 2.0, sectionId = 3, weekId = "A"), "A")
        repo.updateShoppingItem(change.after.copy(quantity = 3.0))
        try { repo.undoQuickAdd(change); fail("Expected stale undo rejection") } catch (_: IllegalArgumentException) {}
        assertEquals(3.0, db.shoppingDao().getAllShoppingItemsSnapshot().single().quantity, 0.0)
    }
    @Test fun quickAddConvertsRepeatedQuantitiesAndUndoRestoresOriginal() = runBlocking {
        val original = ShoppingItemEntity(name = "Milk", quantity = 1.0, unit = "l", sectionId = 3, weekId = "A")
        db.shoppingDao().insertShoppingItem(original)
        val change = repo.quickAddShoppingItem(original.copy(shoppingId = 0, syncId = java.util.UUID.randomUUID().toString(), quantity = 500.0, unit = "ml"), "A")
        assertEquals(1.5, change.after.quantity, 0.0)
        repo.undoQuickAdd(change)
        assertEquals(1.0, db.shoppingDao().getAllShoppingItemsSnapshot().single().quantity, 0.0)
    }
}
