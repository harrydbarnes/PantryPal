package com.example.pantrypal

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.pantrypal.data.database.KitchenDatabase
import com.example.pantrypal.data.entity.*
import com.example.pantrypal.data.repository.StockOperations
import kotlinx.coroutines.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StockOperationsTest {
    private lateinit var db: KitchenDatabase
    private lateinit var stock: StockOperations
    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), KitchenDatabase::class.java).build()
        stock = StockOperations(db)
    }
    @After fun close() = db.close()

    @Test fun historyFailureRollsBackStockAndRestock() = runBlocking {
        val id = db.itemDao().insertItem(ItemEntity(name = "Milk", defaultUnit = "pcs", category = "General", isUsual = true))
        val batch = db.inventoryDao().insertInventory(InventoryEntity(itemId = id, quantity = 1.0, unit = "pcs"))
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_history BEFORE INSERT ON consumption_history BEGIN SELECT RAISE(ABORT, 'injected history failure'); END")
        try { stock.consume(listOf(StockOperations.Use(batch, id, 1.0)), ConsumptionType.FINISHED, "A"); fail("Expected injected failure") }
        catch (_: android.database.sqlite.SQLiteException) { }
        assertEquals(1.0, db.inventoryDao().getById(batch)!!.quantity, 0.0)
        assertTrue(db.shoppingDao().getAllShoppingItemsSnapshot().isEmpty())
        assertTrue(db.consumptionDao().getAllHistory().isEmpty())
    }

    @Test fun concurrentFinishesCannotDoubleConsumeOrDoubleRestock() = runBlocking {
        val id = db.itemDao().insertItem(ItemEntity(name = "Milk", defaultUnit = "pcs", category = "General", isUsual = true))
        val batch = db.inventoryDao().insertInventory(InventoryEntity(itemId = id, quantity = 1.0, unit = "pcs"))
        coroutineScope { repeat(2) { launch(Dispatchers.IO) { stock.consume(listOf(StockOperations.Use(batch, id, 1.0)), ConsumptionType.FINISHED, "A") } } }
        assertNull(db.inventoryDao().getById(batch))
        assertEquals(1, db.consumptionDao().getAllHistory().size)
        assertEquals(1, db.shoppingDao().getAllShoppingItemsSnapshot().size)
    }

    @Test fun invalidSecondBulkUseRollsBackFirst() = runBlocking {
        val id = db.itemDao().insertItem(ItemEntity(name = "Milk", defaultUnit = "pcs", category = "General"))
        val batch = db.inventoryDao().insertInventory(InventoryEntity(itemId = id, quantity = 2.0, unit = "pcs"))
        try { stock.consume(listOf(StockOperations.Use(batch, id, 1.0), StockOperations.Use(batch, id, Double.NaN)), ConsumptionType.FINISHED, "A"); fail() }
        catch (_: IllegalArgumentException) { }
        assertEquals(2.0, db.inventoryDao().getById(batch)!!.quantity, 0.0)
        assertTrue(db.consumptionDao().getAllHistory().isEmpty())
    }
    @Test fun archiveFailureRollsBackPuttingAwayPurchases() = runBlocking {
        val repo = com.example.pantrypal.data.repository.KitchenRepository(db.itemDao(), db.inventoryDao(),
            db.consumptionDao(), db.shoppingDao(), db.mealDao(), db.mealWeekDao(), db.shoppingSectionDao(), db.shoppingHistoryDao(), db)
        db.shoppingSectionDao().insertSection(ShoppingSectionEntity(3, "The rest", 0, false, "THE_REST"))
        db.shoppingDao().insertShoppingItem(ShoppingItemEntity(name = "Purchased milk", isChecked = true, sectionId = 3, weekId = "A"))
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_archive BEFORE INSERT ON shopping_archive BEGIN SELECT RAISE(ABORT, 'injected archive failure'); END")
        try { repo.finishShopping("A", "Pantry"); fail("Expected archive failure") }
        catch (_: android.database.sqlite.SQLiteException) { }
        assertTrue(db.inventoryDao().getAllInventorySnapshot().isEmpty())
        assertTrue(db.itemDao().getAllItemsSnapshot().isEmpty())
        assertTrue(db.shoppingDao().getAllShoppingItemsSnapshot().single().isChecked)
    }
}
