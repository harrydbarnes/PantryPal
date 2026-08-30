package com.example.pantrypal

import android.app.Application
import androidx.work.Configuration
import com.example.pantrypal.data.database.KitchenDatabase
import com.example.pantrypal.data.repository.KitchenRepository
import com.example.pantrypal.data.repository.PantryFeaturesRepository
import com.example.pantrypal.util.KitchenWorkerFactory
import com.example.pantrypal.widget.PantryPalWidgetProvider

class PantryPalApplication : Application(), Configuration.Provider {

    val database: KitchenDatabase by lazy {
        KitchenDatabase.getDatabase(this)
    }

    val repository: KitchenRepository by lazy {
        KitchenRepository(
            database.itemDao(),
            database.inventoryDao(),
            database.consumptionDao(),
            database.shoppingDao(),
            database.mealDao(),
            database.mealWeekDao(),
            database.shoppingSectionDao(),
            database.shoppingHistoryDao(),
            database = database
        )
    }

    val featuresRepository: PantryFeaturesRepository by lazy {
        PantryFeaturesRepository(this, database, repository)
    }

    override fun onCreate() {
        super.onCreate()
        database.invalidationTracker.addObserver(object : androidx.room.InvalidationTracker.Observer(
            "shopping_list",
            "inventory"
        ) {
            override fun onInvalidated(tables: Set<String>) {
                PantryPalWidgetProvider.updateWidgets(this@PantryPalApplication)
            }
        })
    }

    override val workManagerConfiguration: Configuration
        get() {
            return Configuration.Builder()
                .setWorkerFactory(KitchenWorkerFactory(repository))
                .build()
        }
}
