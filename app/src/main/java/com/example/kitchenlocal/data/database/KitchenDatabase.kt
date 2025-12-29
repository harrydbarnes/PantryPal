package com.example.kitchenlocal.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.kitchenlocal.data.dao.ConsumptionDao
import com.example.kitchenlocal.data.dao.InventoryDao
import com.example.kitchenlocal.data.dao.ItemDao
import com.example.kitchenlocal.data.entity.ConsumptionEntity
import com.example.kitchenlocal.data.entity.InventoryEntity
import com.example.kitchenlocal.data.entity.ItemEntity
import androidx.room.TypeConverters
import com.example.kitchenlocal.data.converter.Converters

@Database(
    entities = [ItemEntity::class, InventoryEntity::class, ConsumptionEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class KitchenDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun consumptionDao(): ConsumptionDao

    companion object {
        @Volatile
        private var INSTANCE: KitchenDatabase? = null

        fun getDatabase(context: Context): KitchenDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    KitchenDatabase::class.java,
                    "kitchen_local_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
