package com.example.pantrypal.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.pantrypal.data.dao.ConsumptionDao
import com.example.pantrypal.data.dao.InventoryDao
import com.example.pantrypal.data.dao.ItemDao
import com.example.pantrypal.data.dao.ShoppingDao
import com.example.pantrypal.data.dao.MealDao
import com.example.pantrypal.data.entity.ConsumptionEntity
import com.example.pantrypal.data.entity.InventoryEntity
import com.example.pantrypal.data.entity.ItemEntity
import com.example.pantrypal.data.entity.ShoppingItemEntity
import com.example.pantrypal.data.entity.MealEntity
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.pantrypal.data.converter.Converters

@Database(
    entities = [ItemEntity::class, InventoryEntity::class, ConsumptionEntity::class, ShoppingItemEntity::class, MealEntity::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class KitchenDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun consumptionDao(): ConsumptionDao
    abstract fun shoppingDao(): ShoppingDao
    abstract fun mealDao(): MealDao

    companion object {
        @Volatile
        private var INSTANCE: KitchenDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add imageUrl to items table
                db.execSQL("ALTER TABLE items ADD COLUMN imageUrl TEXT DEFAULT NULL")

                // Create shopping_list table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `shopping_list` (
                        `shoppingId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `quantity` REAL NOT NULL,
                        `unit` TEXT NOT NULL,
                        `isChecked` INTEGER NOT NULL,
                        `addedAt` INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add frequency to shopping_list table
                val defaultFreq = ShoppingItemEntity.FREQ_ONE_OFF
                db.execSQL("ALTER TABLE shopping_list ADD COLUMN frequency TEXT DEFAULT '$defaultFreq' NOT NULL")

                // Create meals table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `meals` (
                        `mealId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `week` TEXT NOT NULL,
                        `ingredients` TEXT NOT NULL
                    )
                """.trimIndent())
            }
        }

        fun getDatabase(context: Context): KitchenDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    KitchenDatabase::class.java,
                    "pantry_pal_db"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
