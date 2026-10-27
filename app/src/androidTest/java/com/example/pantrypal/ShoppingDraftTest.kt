package com.example.pantrypal

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.example.pantrypal.data.database.KitchenDatabase
import com.example.pantrypal.data.entity.*
import com.example.pantrypal.data.repository.KitchenRepository
import com.example.pantrypal.util.AppPreferences
import com.example.pantrypal.viewmodel.MainViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*

class ShoppingDraftTest {
    @Test fun draftChoicesAndWeekRestoreAndChangedStockRequiresRefresh() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val prefs = app.getSharedPreferences(AppPreferences.FILE_NAME, Context.MODE_PRIVATE)
        val keys = listOf("active_shopping_week", "shopping_preparation_draft")
        val previous = keys.associateWith { prefs.getString(it, null) }
        prefs.edit().remove("shopping_preparation_draft").commit()
        val db = Room.inMemoryDatabaseBuilder(app, KitchenDatabase::class.java).build()
        val repo = KitchenRepository(db.itemDao(), db.inventoryDao(), db.consumptionDao(), db.shoppingDao(),
            db.mealDao(), db.mealWeekDao(), db.shoppingSectionDao(), db.shoppingHistoryDao(), db)
        val stores = mutableListOf<ViewModelStore>()
        fun <T> main(block: () -> T): T {
            var result: T? = null
            InstrumentationRegistry.getInstrumentation().runOnMainSync { result = block() }
            @Suppress("UNCHECKED_CAST") return result as T
        }
        fun model(): MainViewModel = main {
            MainViewModel(repo, app).also { vm -> stores += ViewModelStore().apply { put("test", vm) } }
        }
        try {
            db.mealDao().insertMeal(MealEntity(name = "Pasta", week = "B", ingredients = listOf("500 g pasta")))
            val first = model()
            main { first.previewShoppingListForWeek("B") }
            val draft = withTimeout(10000) { first.shoppingBuildPreview.first { it != null }!! }
            main { first.editShoppingDraft(0, "300", "need"); first.pauseShoppingReview() }
            val restarted = model()
            assertEquals("B", restarted.shoppingWeek.value)
            assertEquals(draft.draftId, restarted.shoppingBuildPreview.value!!.draftId)
            assertEquals("300", restarted.shoppingBuildPreview.value!!.quantities.single())
            assertTrue(restarted.shoppingBuildPreview.value!!.explicit.single())
            main { restarted.resumeShoppingReview() }
            val id = db.itemDao().insertItem(ItemEntity(name = "Pasta", defaultUnit = "g", category = "General"))
            db.inventoryDao().insertInventory(InventoryEntity(itemId = id, quantity = 200.0, unit = "g"))
            main { restarted.commitShoppingBuildPreview(draft.lines) }
            val error = withTimeout(10000) { restarted.actionError.first { it != null }!! }
            assertTrue(error.contains("changed"))
            assertTrue(db.shoppingDao().getAllShoppingItemsSnapshot().isEmpty())
            assertNotNull(restarted.shoppingBuildPreview.value)
            main { restarted.dismissShoppingBuildPreview() }
            assertFalse(prefs.contains("shopping_preparation_draft"))
        } finally {
            main { stores.forEach { it.clear() } }
            db.close()
            prefs.edit().apply { previous.forEach { (key, value) -> if (value == null) remove(key) else putString(key, value) } }.commit()
        }
    }
    @Test fun setupRollsBackTogetherAndManualStockReusesOnlyAfterChoosing() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val prefs = app.getSharedPreferences(AppPreferences.FILE_NAME, Context.MODE_PRIVATE)
        val defaultKeys = listOf(AppPreferences.KEY_ADD_ITEM_UNIT, AppPreferences.KEY_ADD_ITEM_CATEGORY, AppPreferences.KEY_ADD_ITEM_LOCATION)
        val previousDefaults = defaultKeys.associateWith { prefs.getString(it, null) }
        val db = Room.inMemoryDatabaseBuilder(app, KitchenDatabase::class.java).build()
        val repo = KitchenRepository(db.itemDao(), db.inventoryDao(), db.consumptionDao(), db.shoppingDao(),
            db.mealDao(), db.mealWeekDao(), db.shoppingSectionDao(), db.shoppingHistoryDao(), db)
        fun <T> main(block: () -> T): T {
            var value: T? = null
            InstrumentationRegistry.getInstrumentation().runOnMainSync { value = block() }
            @Suppress("UNCHECKED_CAST") return value as T
        }
        val store = ViewModelStore()
        val vm = main { MainViewModel(repo, app).also { store.put("test", it) } }
        try {
            val regulars = listOf("Coffee")
            val meals = listOf(com.example.pantrypal.ui.screens.OnboardingMeal("Rice bowl", 3, listOf("300 g rice")))
            val firstStock = com.example.pantrypal.ui.screens.OnboardingStock("Rice", 1.0, "kg", null)
            var saved = CompletableDeferred<Unit>()
            db.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_setup BEFORE INSERT ON inventory BEGIN SELECT RAISE(ABORT, 'injected setup failure'); END")
            main { vm.saveOnboardingKitchen(regulars, meals, firstStock) { saved.complete(Unit) } }
            withTimeout(10000) { vm.actionError.first { it != null } }
            assertFalse(saved.isCompleted)
            assertTrue(db.mealDao().getAllMealsSnapshot().isEmpty())
            assertTrue(db.shoppingDao().getAllShoppingItemsSnapshot().isEmpty())
            assertTrue(db.itemDao().getAllItemsSnapshot().isEmpty())
            db.openHelper.writableDatabase.execSQL("DROP TRIGGER fail_setup")
            main { vm.saveOnboardingKitchen(regulars, meals, firstStock) { saved.complete(Unit) } }
            withTimeout(10000) { saved.await() }
            assertEquals(listOf("300 g rice"), db.mealDao().getAllMealsSnapshot().single().ingredients)
            assertEquals(vm.currentWeek.value, db.mealDao().getAllMealsSnapshot().single().week)
            saved = CompletableDeferred<Unit>()
            main { vm.addItem("Rice", 500.0, "g", "General", false, false, onSaved = { saved.complete(Unit) }) }
            val choice = withTimeout(10000) { vm.manualProductChoice.first { it != null }!! }
            assertFalse(saved.isCompleted)
            assertEquals(1, db.inventoryDao().getAllInventorySnapshot().size)
            main { choice.choose(choice.products.single().itemId) }
            withTimeout(10000) { saved.await() }
            assertEquals(1, db.itemDao().getAllItemsSnapshot().size)
            assertEquals(1.5, com.example.pantrypal.util.stockQuantityInUnit(db.inventoryDao().getAllInventorySnapshot(), "kg")!!, 0.0)
            saved = CompletableDeferred<Unit>()
            main { vm.addItem("Rice", 1.0, "pack", "General", false, false, forceNewProduct = true, onSaved = { saved.complete(Unit) }) }
            withTimeout(10000) { saved.await() }
            assertEquals(2, db.itemDao().getAllItemsSnapshot().size)
        } finally {
            main { store.clear() }; db.close()
            prefs.edit().apply { previousDefaults.forEach { (key, value) -> if (value == null) remove(key) else putString(key, value) } }.commit()
        }
    }

}
