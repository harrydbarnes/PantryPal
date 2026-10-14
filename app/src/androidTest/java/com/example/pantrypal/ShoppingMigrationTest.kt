package com.example.pantrypal

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.pantrypal.data.database.ShoppingSyncSchema
import org.junit.Test
import org.junit.Assert.*

/** An on-disk upgrade of the relevant v7 shopping tables, including legacy seed identities and journal triggers. */
class ShoppingMigrationTest {
    @Test fun preservesLegacyRowsAndCreatesDistinctDurableIdentities() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val name = "migration-${java.util.UUID.randomUUID()}.db"
        fun helper(version: Int) = FrameworkSQLiteOpenHelperFactory().create(SupportSQLiteOpenHelper.Configuration.builder(context).name(name)
            .callback(object : SupportSQLiteOpenHelper.Callback(version) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL("CREATE TABLE shopping_list (shoppingId INTEGER PRIMARY KEY, name TEXT NOT NULL, quantity REAL NOT NULL DEFAULT 1, unit TEXT NOT NULL DEFAULT 'pcs', isChecked INTEGER NOT NULL DEFAULT 0, sectionId INTEGER NOT NULL DEFAULT 3, weekId TEXT, frequency TEXT NOT NULL DEFAULT 'ONE_OFF', category TEXT NOT NULL DEFAULT 'Uncategorised', storeTag TEXT NOT NULL DEFAULT 'EITHER', source TEXT NOT NULL DEFAULT 'MANUAL')")
                    db.execSQL("CREATE TABLE shopping_sections (sectionId INTEGER PRIMARY KEY, name TEXT NOT NULL, systemKey TEXT)")
                    db.execSQL("CREATE TABLE meal_weeks (weekId TEXT PRIMARY KEY)")
                    db.execSQL("INSERT INTO shopping_list(shoppingId, name) VALUES (11, 'Existing milk'), (12, 'Existing eggs')")
                    db.execSQL("INSERT INTO shopping_sections VALUES (7, 'My section', NULL), (3, 'The rest', 'THE_REST')")
                }
                override fun onUpgrade(db: SupportSQLiteDatabase, old: Int, new: Int) { ShoppingSyncSchema.migrate(db) }
            }).build())
        try {
            helper(7).use { it.writableDatabase }
            helper(8).use { open ->
                val db = open.writableDatabase
                ShoppingSyncSchema.installTriggers(db)
                db.query("SELECT name, syncId FROM shopping_list ORDER BY shoppingId").use {
                    assertTrue(it.moveToFirst()); assertEquals("Existing milk", it.getString(0)); assertEquals("legacy-item-11", it.getString(1))
                    assertTrue(it.moveToNext()); assertEquals("legacy-item-12", it.getString(1))
                }
                db.execSQL("UPDATE OR ABORT shopping_list SET name = 'Changed' WHERE shoppingId = 11")
                db.execSQL("UPDATE OR ABORT shopping_list SET name = 'Changed again' WHERE shoppingId = 11")
                db.query("SELECT recordKey FROM shopping_sync_queue").use { assertTrue(it.moveToFirst()); assertTrue(it.getString(0) in setOf("item:legacy-item-11", "layout:item-field:legacy-item-11:name")); assertEquals(2, it.count) }
            }
        } finally { context.deleteDatabase(name) }
    }
    @Test fun roomUpgradeFromEightKeepsShoppingAndCreatesLocalLedger() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val name = "ledger-migration-${java.util.UUID.randomUUID()}.db"
        val original = androidx.room.Room.databaseBuilder(context, com.example.pantrypal.data.database.KitchenDatabase::class.java, name).build()
        try {
            original.shoppingDao().insertShoppingItem(com.example.pantrypal.data.entity.ShoppingItemEntity(name = "Milk", quantity = 2.0))
            original.openHelper.writableDatabase.apply {
                revertShoppingSchemaToNine(this)
                execSQL("DROP TABLE purchase_records")
                execSQL("DROP TABLE receipt_imports")
                execSQL("PRAGMA user_version = 8")
            }
            original.close()
            val upgraded = androidx.room.Room.databaseBuilder(context, com.example.pantrypal.data.database.KitchenDatabase::class.java, name)
                .addMigrations(com.example.pantrypal.data.database.KitchenDatabase.MIGRATION_8_9, com.example.pantrypal.data.database.KitchenDatabase.MIGRATION_9_10).build()
            try {
                assertEquals("Milk", upgraded.shoppingDao().getAllShoppingItemsSnapshot().single().name)
                assertTrue(upgraded.purchaseRecordDao().records().isEmpty())
                assertTrue(upgraded.purchaseRecordDao().imports().isEmpty())
            } finally { upgraded.close() }
        } finally { original.close(); context.deleteDatabase(name) }
    }
    private fun revertShoppingSchemaToNine(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE shopping_ingest_state")
        db.execSQL("ALTER TABLE inventory DROP COLUMN preferredStore")
        db.execSQL("ALTER TABLE items DROP COLUMN preferredStore")
        db.execSQL("CREATE TABLE legacy_shopping_list (shoppingId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, quantity REAL NOT NULL, unit TEXT NOT NULL, isChecked INTEGER NOT NULL, addedAt INTEGER NOT NULL, frequency TEXT NOT NULL, sectionId INTEGER NOT NULL, weekId TEXT, syncId TEXT NOT NULL DEFAULT '')")
        db.execSQL("INSERT INTO legacy_shopping_list SELECT shoppingId,name,quantity,unit,isChecked,addedAt,frequency,sectionId,weekId,syncId FROM shopping_list")
        db.execSQL("DROP TABLE shopping_list")
        db.execSQL("ALTER TABLE legacy_shopping_list RENAME TO shopping_list")
    }

    @Test fun roomUpgradeFromNinePreservesWeeklyRowsAndAddsSafeStockLinks() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val name = "shopping-upgrade-${java.util.UUID.randomUUID()}.db"
        val old = androidx.room.Room.databaseBuilder(context, com.example.pantrypal.data.database.KitchenDatabase::class.java, name).build()
        try {
            val itemId = old.itemDao().insertItem(com.example.pantrypal.data.entity.ItemEntity(name = "Milk", defaultUnit = "l", category = "Dairy & Fridge"))
            val batch = old.inventoryDao().insertInventory(com.example.pantrypal.data.entity.InventoryEntity(itemId = itemId, quantity = 1.0, unit = "l"))
            old.shoppingDao().insertShoppingItem(com.example.pantrypal.data.entity.ShoppingItemEntity(name = "Milk", weekId = "A"))
            old.shoppingDao().insertShoppingItem(com.example.pantrypal.data.entity.ShoppingItemEntity(name = "Milk", weekId = "B"))
            old.openHelper.writableDatabase.apply { revertShoppingSchemaToNine(this); execSQL("PRAGMA user_version = 9") }
            old.close()
            val upgraded = androidx.room.Room.databaseBuilder(context, com.example.pantrypal.data.database.KitchenDatabase::class.java, name)
                .addMigrations(com.example.pantrypal.data.database.KitchenDatabase.MIGRATION_9_10).build()
            try {
                val rows = upgraded.shoppingListDao().snapshot()
                assertEquals(setOf("A", "B"), rows.map { it.weekId }.toSet())
                assertTrue(rows.all { it.inventoryId == batch && it.storeTag == com.example.pantrypal.data.entity.StoreTag.EITHER })
                assertEquals("Dairy & Fridge", rows.first().category)
                upgraded.inventoryDao().deleteInventory(upgraded.inventoryDao().getById(batch)!!)
                assertTrue(upgraded.shoppingListDao().snapshot().all { it.inventoryId == null })
            } finally { upgraded.close() }
        } finally { old.close(); context.deleteDatabase(name) }
    }
}
