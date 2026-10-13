package com.example.pantrypal

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.pantrypal.data.database.KitchenDatabase
import com.example.pantrypal.data.entity.*
import com.example.pantrypal.data.dao.ShoppingListDao
import com.example.pantrypal.domain.shopping.ShoppingIngestItem
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*

class ShoppingListDaoTest {
    private lateinit var db: KitchenDatabase
    private lateinit var dao: ShoppingListDao
    @Before fun setup() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), KitchenDatabase::class.java).build()
        dao = db.shoppingListDao()
        db.shoppingSectionDao().insertSection(ShoppingSectionEntity(3, "The rest", 0, false, "THE_REST"))
        Unit
    }
    @After fun close() = db.close()
    private suspend fun milk(quantity: Double = 0.0): Pair<Long, Long> {
        val product = db.itemDao().insertItem(ItemEntity(name = "Milk", defaultUnit = "l", category = "Dairy & Fridge", preferredStore = StoreTag.TESCO))
        return product to db.inventoryDao().insertInventory(InventoryEntity(itemId = product, quantity = quantity, unit = "l", preferredStore = StoreTag.TESCO))
    }
    @Test fun upsertInheritsStockMetadataAndSerialisesConcurrentIncrements() = runBlocking {
        val (_, batch) = milk()
        coroutineScope { repeat(2) { launch(Dispatchers.IO) { dao.upsertShoppingItem("mILk", 2.0, weekId = "A") } } }
        val row = dao.snapshot().single()
        assertEquals(4.0, row.quantity, 0.0); assertEquals(batch, row.inventoryId)
        assertEquals(StoreTag.TESCO, row.storeTag); assertEquals("Dairy & Fridge", row.category)
        dao.upsertShoppingItem("Milk", 1.0, weekId = "B")
        assertEquals(2, dao.snapshot().size)
    }
    @Test fun unknownProductsAreCategorisedAndCompleteRestocksOnlyTickedCurrentWeek() = runBlocking {
        milk()
        val row = dao.upsertShoppingItem("Milk", 2.0, weekId = "A")
        dao.update(row.copy(isChecked = true))
        val courgettes = dao.upsertShoppingItem("Courgettes", 3.0, weekId = "A", store = StoreTag.ALDI)
        dao.update(courgettes.copy(isChecked = true))
        val future = dao.upsertShoppingItem("Bread", 2.0, weekId = "B")
        dao.update(future.copy(isChecked = true))
        assertEquals(2, dao.completeShopAndRestock("A"))
        assertEquals(0, dao.completeShopAndRestock("A"))
        assertEquals(2.0, db.inventoryDao().getAllInventorySnapshot().first { it.unit == "l" }.quantity, 0.0)
        val product = dao.product("Courgettes")!!
        assertEquals("Fruit & Veg", product.category); assertEquals(StoreTag.ALDI, product.preferredStore)
        assertEquals(3.0, dao.stock(product.itemId).single().quantity, 0.0)
        assertEquals("Bread", dao.snapshot().single().name)
    }
    @Test fun runningLowRequiresRepeatedRecentConsumptionNoOtherStockAndNoUntickedListEntry() = runBlocking {
        val (product, _) = milk()
        val now = System.currentTimeMillis()
        repeat(2) { db.consumptionDao().insertConsumption(ConsumptionEntity(itemId = product, quantity = 1.0, type = ConsumptionType.FINISHED, date = now - it * 1000)) }
        assertEquals(1, dao.getLikelyRunningLowStaples(now - 30L * 86400000).first().size)
        dao.upsertShoppingItem("Milk", weekId = "A")
        assertTrue(dao.getLikelyRunningLowStaples(now - 30L * 86400000).first().isEmpty())
        dao.delete(dao.snapshot().single())
        db.inventoryDao().insertInventory(InventoryEntity(itemId = product, quantity = 1.0, unit = "l"))
        assertTrue(dao.getLikelyRunningLowStaples(now - 30L * 86400000).first().isEmpty())
    }
    @Test fun storeMemorySurvivesCompletionAndForeignKeyDeletionIsSafe() = runBlocking {
        val (product, batch) = milk()
        val row = dao.upsertShoppingItem("Milk", weekId = "A")
        dao.cycleStore(row.syncId)
        assertEquals(StoreTag.ALDI, db.inventoryDao().getById(batch)!!.preferredStore)
        assertEquals(StoreTag.ALDI, db.itemDao().getItemById(product)!!.preferredStore)
        db.inventoryDao().deleteInventory(db.inventoryDao().getById(batch)!!)
        assertNull(dao.snapshot().single().inventoryId)
        assertEquals(StoreTag.ALDI, dao.upsertShoppingItem("Milk", weekId = "A").storeTag)
    }
    @Test fun shareAndApiRetriesNeverInflateQuantities() = runBlocking {
        assertEquals(1, dao.importItems(listOf(ShoppingIngestItem("Milk", 2.0)), "KEEP_SHARE", "A", "operation"))
        assertEquals(0, dao.importItems(listOf(ShoppingIngestItem("Milk", 2.0)), "KEEP_SHARE", "A", "operation"))
        assertTrue(dao.importKeepItem("keep-line", ShoppingIngestItem("Bread", 2.0), "A"))
        assertFalse(dao.importKeepItem("keep-line", ShoppingIngestItem("Bread", 2.0), "A"))
        assertTrue(dao.importKeepItem("keep-line", ShoppingIngestItem("Bread", 3.0), "A"))
        assertEquals(3.0, dao.snapshot().first { it.name == "Bread" }.quantity, 0.0)
    }
    @Test fun emptySwipeIsAtomicAndUndoRejectsNewerListEdits() = runBlocking {
        val (_, batch) = milk(2.0)
        val change = dao.markEmptyAndAdd(batch, "A")
        assertEquals(0.0, db.inventoryDao().getById(batch)!!.quantity, 0.0)
        assertEquals(1, db.consumptionDao().getAllHistory().size)
        dao.undoEmpty(change)
        assertEquals(2.0, db.inventoryDao().getById(batch)!!.quantity, 0.0)
        assertTrue(db.consumptionDao().getAllHistory().isEmpty()); assertTrue(dao.snapshot().isEmpty())
        val next = dao.markEmptyAndAdd(batch, "A")
        dao.update(next.shoppingAfter.copy(quantity = 3.0))
        try { dao.undoEmpty(next); fail("Expected newer edit protection") } catch (_: IllegalArgumentException) {}
        assertEquals(0.0, db.inventoryDao().getById(batch)!!.quantity, 0.0)
    }
    @Test fun receiptRecordedStockIsNotCountedAgainAndArchiveFailureRollsBack() = runBlocking {
        val (product, batch) = milk(2.0)
        val row = dao.upsertShoppingItem("Milk", 2.0, weekId = "A")
        dao.update(row.copy(isChecked = true))
        db.purchaseRecordDao().put(PurchaseRecordEntity(shoppingSyncId = row.syncId, receiptId = "receipt", name = row.name,
            quantity = 2.0, unit = "l", stockRecorded = true, completed = false, storageLocation = "Fridge"))
        dao.completeShopAndRestock("A")
        assertEquals(2.0, dao.stock(product).single().quantity, 0.0)
        assertEquals("Fridge", db.shoppingDao().getShoppingArchiveSnapshot().single().storageLocation)
        val another = dao.upsertShoppingItem("Milk", 1.0, weekId = "A")
        dao.update(another.copy(isChecked = true))
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_archive BEFORE INSERT ON shopping_archive BEGIN SELECT RAISE(ABORT, 'test failure'); END")
        try { dao.completeShopAndRestock("A"); fail("Expected failure") } catch (_: android.database.sqlite.SQLiteException) {}
        assertEquals(2.0, db.inventoryDao().getById(batch)!!.quantity, 0.0)
        assertEquals(1, dao.snapshot().size)
    }
}
