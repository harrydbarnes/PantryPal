package com.example.pantrypal.data.database

import androidx.sqlite.db.SupportSQLiteDatabase

object ShoppingSyncSchema {
    fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE shopping_list ADD COLUMN syncId TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE shopping_sections ADD COLUMN syncId TEXT NOT NULL DEFAULT ''")
        db.execSQL("UPDATE shopping_list SET syncId = 'legacy-item-' || shoppingId")
        db.execSQL("UPDATE shopping_sections SET syncId = COALESCE(systemKey, 'legacy-section-' || sectionId)")
        db.execSQL("CREATE TABLE IF NOT EXISTS shopping_layout (layoutKey TEXT NOT NULL PRIMARY KEY, value TEXT NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS shopping_sync_queue (recordKey TEXT NOT NULL PRIMARY KEY, token TEXT NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS shopping_sync_batch (id INTEGER NOT NULL PRIMARY KEY, householdId TEXT NOT NULL, batchId TEXT NOT NULL, payload TEXT NOT NULL)")
    }

    fun installTriggers(db: SupportSQLiteDatabase) {
        // Also repair the built-in sections inserted by the original SQL seed helper.
        db.execSQL("UPDATE shopping_sections SET syncId = COALESCE(systemKey, 'legacy-section-' || sectionId) WHERE syncId = ''")
        for ((table, prefix, key) in listOf(
            Triple("shopping_list", "item:", "syncId"),
            Triple("shopping_sections", "section:", "syncId"),
            Triple("shopping_layout", "layout:", "layoutKey"),
            Triple("meal_weeks", "week:", "weekId")
        )) {
            for (operation in listOf("INSERT", "UPDATE", "DELETE")) {
                val row = if (operation == "DELETE") "OLD" else "NEW"
                db.execSQL("""
                    CREATE TRIGGER IF NOT EXISTS sync_${table}_$operation AFTER $operation ON $table
                    BEGIN
                      INSERT OR REPLACE INTO shopping_sync_queue(recordKey, token)
                      VALUES ('$prefix' || $row.$key, lower(hex(randomblob(16))));
                    END
                """.trimIndent())
            }
        }
    }
}
