package com.example.pantrypal

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.pantrypal.data.database.*
import com.example.pantrypal.data.entity.*
import com.example.pantrypal.data.household.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ShoppingSyncStoreTest {
    private lateinit var db: KitchenDatabase
    private lateinit var store: ShoppingSyncStore
    @Before fun setup() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), KitchenDatabase::class.java)
            .addCallback(object : RoomDatabase.Callback() {
                override fun onOpen(db: SupportSQLiteDatabase) { ShoppingSyncSchema.installTriggers(db) }
            }).build()
        store = ShoppingSyncStore(db)
        db.shoppingSectionDao().insertSection(ShoppingSectionEntity(3, "The rest", 0, false, "THE_REST"))
        store.resetQueue()
    }
    @After fun close() { db.close() }

    @Test fun mutationAndDeletionAreDurableAndRetryable() = runBlocking {
        val item = ShoppingItemEntity(name = "Milk", sectionId = 3)
        val id = db.shoppingDao().insertShoppingItem(item)
        val batch = store.batch("home")!!
        val restartedStore = ShoppingSyncStore(db)
        assertEquals(batch, restartedStore.batch("home"))
        val remote = ShoppingRecordProtocol.commit(ShoppingWireState(), "a", batch.batchId, store.changes(batch))
        store.accept(remote, batch)
        assertTrue(db.shoppingSyncDao().pending().isEmpty())
        db.shoppingDao().deleteShoppingItem(item.copy(shoppingId = id))
        val deletion = store.changes(store.batch("home")!!).getValue("item:" + item.syncId)
        assertNull(deletion.data)
    }

    @Test fun editDuringNetworkRequestIsNotAcknowledgedOrOverwritten() = runBlocking {
        val item = ShoppingItemEntity(name = "Milk", sectionId = 3)
        val id = db.shoppingDao().insertShoppingItem(item)
        val batch = store.batch("home")!!
        db.shoppingDao().updateShoppingItem(item.copy(shoppingId = id, isChecked = true))
        val remote = ShoppingRecordProtocol.commit(ShoppingWireState(), "a", batch.batchId, store.changes(batch))
        store.accept(remote, batch)
        assertTrue(db.shoppingSyncDao().item(item.syncId)!!.isChecked)
        assertTrue(db.shoppingSyncDao().pending().any { it.recordKey == "item:" + item.syncId })
    }

    @Test fun remoteRecordUsesLocalSectionIdAndLeavesPantryAlone() = runBlocking {
        val pantry = ItemEntity(name = "Unrelated pantry item", defaultUnit = "pcs", category = "General")
        val pantryId = db.itemDao().insertItem(pantry)
        val remoteSection = ShoppingSectionEntity(900, "Bakery", 2, true, syncId = "shared-section")
        val gson = com.google.gson.Gson()
        val json = gson.toJsonTree(ShoppingItemEntity(name = "Bread", syncId = "shared-bread")).asJsonObject
        json.addProperty("sectionSyncId", "shared-section")
        store.accept(ShoppingWireState(records = mapOf(
            "section:shared-section" to ShoppingRecord("s", gson.toJson(remoteSection)),
            "item:shared-bread" to ShoppingRecord("i", json.toString())
        )), null)
        val section = db.shoppingSyncDao().section("shared-section")!!
        assertNotEquals(900L, section.sectionId)
        assertEquals(section.sectionId, db.shoppingSyncDao().item("shared-bread")!!.sectionId)
        assertEquals("Unrelated pantry item", db.itemDao().getItemById(pantryId)!!.name)
        assertTrue(db.shoppingSyncDao().pending().isEmpty())
    }
    @Test fun concurrentSectionDeletionKeepsNewLocalItemVisible() = runBlocking {
        val section = ShoppingSectionEntity(name = "Custom", sortOrder = 2, recursEveryWeek = false)
        val sectionId = db.shoppingSectionDao().insertSection(section)
        store.resetQueue()
        val item = ShoppingItemEntity(name = "Milk", sectionId = sectionId)
        db.shoppingDao().insertShoppingItem(item)
        store.accept(ShoppingWireState(records = mapOf("section:" + section.syncId to ShoppingRecord("deleted", null))), null)
        assertEquals(3L, db.shoppingSyncDao().item(item.syncId)!!.sectionId)
        assertTrue(db.shoppingSyncDao().pending().any { it.recordKey == "item:" + item.syncId })
    }

    @Test fun compactedDeletesRemoveStaleRowsButKeepOfflineEdits() = runBlocking {
        val old = ShoppingItemEntity(name = "Old", sectionId = 3)
        val edited = ShoppingItemEntity(name = "Offline edit", sectionId = 3)
        db.shoppingDao().insertShoppingItem(old)
        val editedId = db.shoppingDao().insertShoppingItem(edited)
        store.resetQueue()
        db.shoppingDao().updateShoppingItem(edited.copy(shoppingId = editedId, quantity = 2.0))
        store.accept(ShoppingWireState(), null, authoritative = true)
        assertNull(db.shoppingSyncDao().item(old.syncId))
        assertEquals(2.0, db.shoppingSyncDao().item(edited.syncId)!!.quantity, 0.0)
    }

    @Test fun largeOfflineQueueIsSentInBoundedBatches() = runBlocking {
        repeat(205) { db.shoppingDao().insertShoppingItem(ShoppingItemEntity(name = "Item $it", sectionId = 3)) }
        val first = store.batch("home")!!
        assertEquals(100, store.changes(first).size)
        store.accept(ShoppingWireState(records = store.changes(first)), first)
        assertEquals(100, store.changes(store.batch("home")!!).size)
    }
}
