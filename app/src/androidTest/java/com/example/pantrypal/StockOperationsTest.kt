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
}
