package com.example.pantrypal

import android.app.Application
import androidx.work.Configuration
import com.example.pantrypal.data.database.KitchenDatabase
import com.example.pantrypal.data.repository.KitchenRepository
import com.example.pantrypal.util.KitchenWorkerFactory

class PantryPalApplication : Application(), Configuration.Provider {

    val repository: KitchenRepository by lazy {
        val database = KitchenDatabase.getDatabase(this)
        KitchenRepository(
            database.itemDao(),
            database.inventoryDao(),
            database.consumptionDao(),
            database.shoppingDao()
        )
    }

    override val workManagerConfiguration: Configuration
        get() {
            return Configuration.Builder()
                .setWorkerFactory(KitchenWorkerFactory(repository))
                .build()
        }
}
