package com.example.pantrypal.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.pantrypal.data.dao.ConsumptionDao
import com.example.pantrypal.data.dao.InventoryDao
import com.example.pantrypal.data.dao.ItemDao
import com.example.pantrypal.data.dao.ShoppingDao
import com.example.pantrypal.data.entity.ConsumptionEntity
import com.example.pantrypal.data.entity.InventoryEntity
import com.example.pantrypal.data.entity.ItemEntity
import com.example.pantrypal.data.entity.ShoppingItemEntity
import androidx.room.TypeConverters
import com.example.pantrypal.data.converter.Converters

@Database(
    entities = [ItemEntity::class, InventoryEntity::class, ConsumptionEntity::class, ShoppingItemEntity::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class KitchenDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun consumptionDao(): ConsumptionDao
    abstract fun shoppingDao(): ShoppingDao

    companion object {
        @Volatile
        private var INSTANCE: KitchenDatabase? = null

        fun getDatabase(context: Context): KitchenDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    KitchenDatabase::class.java,
                    "pantry_pal_db"
                )
                .fallbackToDestructiveMigration() // For development simplicity
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
