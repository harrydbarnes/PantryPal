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
        // Repair rows inserted by the original SQL seed helpers before exposing them to Room.
        db.execSQL("UPDATE shopping_list SET syncId = 'legacy-item-' || shoppingId WHERE syncId = ''")
        db.execSQL("UPDATE shopping_sections SET syncId = COALESCE(systemKey, 'legacy-section-' || sectionId) WHERE syncId = ''")
        for ((table, prefix, key) in listOf(
            Triple("shopping_list", "item:", "syncId"),
            Triple("shopping_sections", "section:", "syncId"),
            Triple("shopping_layout", "layout:", "layoutKey"),
            Triple("meal_weeks", "week:", "weekId")
        )) {
            for (operation in listOf("INSERT", "UPDATE", "DELETE")) {
                val row = if (operation == "DELETE") "OLD" else "NEW"
                db.execSQL("DROP TRIGGER IF EXISTS sync_${table}_$operation")
                db.execSQL("""
                    CREATE TRIGGER IF NOT EXISTS sync_${table}_$operation AFTER $operation ON $table
                    BEGIN
                      DELETE FROM shopping_sync_queue WHERE recordKey = '$prefix' || $row.$key;
                      INSERT INTO shopping_sync_queue(recordKey, token)
                      VALUES ('$prefix' || $row.$key, lower(hex(randomblob(16))));
                    END
                """.trimIndent())
            }
        }
        // Separate field records let quantity edits and ticks converge independently.
        for (field in listOf("name", "quantity", "unit", "isChecked", "sectionId", "weekId", "frequency")) {
            val wireField = if (field == "sectionId") "sectionSyncId" else field
            val value = if (field == "sectionId") "COALESCE((SELECT syncId FROM shopping_sections WHERE sectionId = NEW.sectionId), 'THE_REST')"
                else "COALESCE(CAST(NEW.$field AS TEXT), '')"
            db.execSQL("DROP TRIGGER IF EXISTS shopping_field_$field")
            db.execSQL("""
                CREATE TRIGGER shopping_field_$field AFTER UPDATE OF $field ON shopping_list
                WHEN OLD.$field IS NOT NEW.$field
                BEGIN
                    INSERT OR REPLACE INTO shopping_layout(layoutKey, value)
                    VALUES ('item-field:' || NEW.syncId || ':$wireField', $value);
                END
            """.trimIndent())
        }
        db.execSQL("""
            CREATE TRIGGER IF NOT EXISTS shopping_fields_delete AFTER DELETE ON shopping_list
            BEGIN
                DELETE FROM shopping_layout
                WHERE substr(layoutKey, 1, length('item-field:' || OLD.syncId || ':')) = 'item-field:' || OLD.syncId || ':';
            END
        """.trimIndent())
    }
}
