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
                    db.execSQL("CREATE TABLE shopping_list (shoppingId INTEGER PRIMARY KEY, name TEXT NOT NULL)")
                    db.execSQL("CREATE TABLE shopping_sections (sectionId INTEGER PRIMARY KEY, name TEXT NOT NULL, systemKey TEXT)")
                    db.execSQL("CREATE TABLE meal_weeks (weekId TEXT PRIMARY KEY)")
                    db.execSQL("INSERT INTO shopping_list VALUES (11, 'Existing milk'), (12, 'Existing eggs')")
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
                db.query("SELECT recordKey FROM shopping_sync_queue").use { assertTrue(it.moveToFirst()); assertEquals("item:legacy-item-11", it.getString(0)); assertEquals(1, it.count) }
            }
        } finally { context.deleteDatabase(name) }
    }
}
