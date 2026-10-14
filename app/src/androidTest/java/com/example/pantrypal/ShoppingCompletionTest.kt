package com.example.pantrypal

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.pantrypal.data.database.KitchenDatabase
import com.example.pantrypal.data.entity.*
import com.example.pantrypal.data.repository.KitchenRepository
import kotlinx.coroutines.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ShoppingCompletionTest {
    private lateinit var db: KitchenDatabase
    private lateinit var repo: KitchenRepository
    @Before fun setup() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), KitchenDatabase::class.java).build()
        repo = KitchenRepository(db.itemDao(), db.inventoryDao(), db.consumptionDao(), db.shoppingDao(),
            db.mealDao(), db.mealWeekDao(), db.shoppingSectionDao(), db.shoppingHistoryDao(), db)
        db.shoppingSectionDao().insertSection(ShoppingSectionEntity(1, "Regulars", 0, true))
        db.shoppingSectionDao().insertSection(ShoppingSectionEntity(3, "Other", 1, false))
        Unit
    }
    @After fun close() = db.close()

    @Test fun finishIsIdempotentAndRegularsResetOnlyWhenRequested() = runBlocking {
        val regular = ShoppingItemEntity(name = "Milk", quantity = 2.0, unit = "l", isChecked = true, sectionId = 1)
        db.shoppingDao().insertShoppingItem(regular)
        val results = coroutineScope { (1..2).map {
            async(Dispatchers.IO) { repo.finishShopping("A", mapOf(regular.syncId to "Fridge"), "Pantry", true) }
        }.awaitAll() }
        assertEquals(1, results.sum())
        assertEquals(1, db.shoppingDao().getShoppingArchiveSnapshot().size)
        assertTrue(db.shoppingDao().getAllShoppingItemsSnapshot().single().isChecked)
        assertEquals(2.0, db.inventoryDao().getAllInventorySnapshot().single().quantity, 0.0)
        assertEquals("Fridge", db.inventoryDao().getAllInventorySnapshot().single().storageLocation)
        repo.prepareNextShop()
        assertFalse(db.shoppingDao().getAllShoppingItemsSnapshot().single().isChecked)
        assertEquals(1, db.shoppingDao().getShoppingArchiveSnapshot().size)
    }
    @Test fun nextShopDoesNotResetNewUnfinishedPurchases() = runBlocking {
        val regular = ShoppingItemEntity(name = "Milk", isChecked = true, sectionId = 1)
        db.shoppingDao().insertShoppingItem(regular)
        repo.finishShopping("A", emptyMap(), "Pantry", false)
        val newlyBought = ShoppingItemEntity(name = "Bread", isChecked = true, sectionId = 1)
        db.shoppingDao().insertShoppingItem(newlyBought)
        repo.prepareNextShop()
        val rows = db.shoppingDao().getAllShoppingItemsSnapshot().associateBy { it.name }
        assertFalse(rows.getValue("Milk").isChecked)
        assertTrue(rows.getValue("Bread").isChecked)
    }

    @Test fun perItemLocationsAndNoPantryOptionAreRespected() = runBlocking {
        val milk = ShoppingItemEntity(name = "Milk", isChecked = true, sectionId = 3, weekId = "A")
        val peas = ShoppingItemEntity(name = "Peas", isChecked = true, sectionId = 3, weekId = "A")
        db.shoppingDao().insertShoppingItem(milk); db.shoppingDao().insertShoppingItem(peas)
        assertEquals(2, repo.finishShopping("A", mapOf(milk.syncId to "Fridge", peas.syncId to "Freezer"), "Pantry", true))
        assertEquals(1, db.shoppingDao().getShoppingArchiveSnapshot().map { it.tripId }.distinct().size)
        assertEquals(setOf("Fridge", "Freezer"), db.inventoryDao().getAllInventorySnapshot().map { it.storageLocation }.toSet())
        val soap = ShoppingItemEntity(name = "Soap", isChecked = true, sectionId = 3, weekId = "A")
        db.shoppingDao().insertShoppingItem(soap)
        assertEquals(1, repo.finishShopping("A", emptyMap(), "Pantry", false))
        assertEquals(2, db.inventoryDao().getAllInventorySnapshot().size)
        assertNull(db.shoppingDao().getShoppingArchiveSnapshot().first { it.name == "Soap" }.storageLocation)
    }
    @Test fun changedPurchasesRequireAnotherReview() = runBlocking {
        val milk = ShoppingItemEntity(name = "Milk", isChecked = true, sectionId = 3, weekId = "A")
        val id = db.shoppingDao().insertShoppingItem(milk)
        val reviewed = milk.copy(shoppingId = id)
        repo.updateShoppingItem(reviewed.copy(quantity = 2.0))
        try { repo.finishShopping("A", emptyMap(), "Pantry", true, listOf(reviewed)); fail("Expected stale completion rejection") }
        catch (_: IllegalArgumentException) {}
        assertTrue(db.inventoryDao().getAllInventorySnapshot().isEmpty())
        assertTrue(db.shoppingDao().getShoppingArchiveSnapshot().isEmpty())
    }

    @Test fun quickAddUndoCannotOverwriteAnotherPersonsEdit() = runBlocking {
        val change = repo.quickAddShoppingItem(ShoppingItemEntity(name = "Milk", quantity = 2.0, sectionId = 3, weekId = "A"), "A")
        repo.updateShoppingItem(change.after.copy(quantity = 3.0))
        try { repo.undoQuickAdd(change); fail("Expected stale undo rejection") } catch (_: IllegalArgumentException) {}
        assertEquals(3.0, db.shoppingDao().getAllShoppingItemsSnapshot().single().quantity, 0.0)
    }
    @Test fun quickAddConvertsRepeatedQuantitiesAndUndoRestoresOriginal() = runBlocking {
        val original = ShoppingItemEntity(name = "Milk", quantity = 1.0, unit = "l", sectionId = 3, weekId = "A")
        db.shoppingDao().insertShoppingItem(original)
        val change = repo.quickAddShoppingItem(original.copy(shoppingId = 0, syncId = java.util.UUID.randomUUID().toString(), quantity = 500.0, unit = "ml"), "A")
        assertEquals(1.5, change.after.quantity, 0.0)
        repo.undoQuickAdd(change)
        assertEquals(1.0, db.shoppingDao().getAllShoppingItemsSnapshot().single().quantity, 0.0)
    }
    private fun features() = com.example.pantrypal.data.repository.PantryFeaturesRepository(
        ApplicationProvider.getApplicationContext(), db, repo)
    private fun receipt() = com.example.pantrypal.domain.receipt.ReceiptParser.parse("2 x Milk 3.00\nTOTAL 3.00")

    @Test fun receiptThenFinishAddsStockAndPricesOnce() = runBlocking {
        val milk = ShoppingItemEntity(name = "Milk", quantity = 2.0, unit = "pcs", isChecked = true, sectionId = 3, weekId = "A")
        db.shoppingDao().insertShoppingItem(milk)
        val receipt = receipt()
        val candidate = receipt.candidates.single().copy(purchaseLink = "shopping:" + milk.syncId)
        features().importReceiptPurchases(listOf(candidate), storageLocation = "Fridge", receiptId = receipt.receiptId)
        repo.finishShopping("A", emptyMap(), "Pantry", true)
        assertEquals(2.0, db.inventoryDao().getAllInventorySnapshot().single().quantity, 0.0)
        try { features().importReceiptPurchases(listOf(candidate), receiptId = receipt.receiptId); fail("Expected duplicate receipt rejection") }
        catch (_: IllegalArgumentException) {}
        assertEquals(1, db.backupDao().priceHistory().size)
        assertEquals(1, db.purchaseRecordDao().records().size)
        assertEquals("Fridge", db.shoppingDao().getShoppingArchiveSnapshot().single().storageLocation)
        val backup = features().exportBackupJson()
        db.purchaseRecordDao().clearRecords(); db.purchaseRecordDao().clearImports()
        features().restoreBackupJson(backup)
        assertEquals(1, db.purchaseRecordDao().records().size)
        assertTrue(db.purchaseRecordDao().imported(receipt.receiptId))
    }
    @Test fun finishThenReceiptLinksToCompletedPurchaseWithoutAddingStockAgain() = runBlocking {
        db.shoppingDao().insertShoppingItem(ShoppingItemEntity(name = "Milk", quantity = 2.0, isChecked = true, sectionId = 3, weekId = "A"))
        repo.finishShopping("A", emptyMap(), "Pantry", true)
        val option = features().receiptPurchaseOptions().single()
        val receipt = receipt()
        features().importReceiptPurchases(listOf(receipt.candidates.single().copy(purchaseLink = option.key)), receiptId = receipt.receiptId)
        assertEquals(2.0, db.inventoryDao().getAllInventorySnapshot().single().quantity, 0.0)
        assertEquals(1, db.purchaseRecordDao().records().size)
    }
    @Test fun invalidReceiptLinkRollsBackEntireImport() = runBlocking {
        db.shoppingDao().insertShoppingItem(ShoppingItemEntity(name = "Milk", quantity = 1.0, isChecked = true, sectionId = 3))
        val option = features().receiptPurchaseOptions().single()
        val receipt = receipt()
        try { features().importReceiptPurchases(listOf(receipt.candidates.single().copy(purchaseLink = option.key)), receiptId = receipt.receiptId); fail("Expected quantity mismatch") }
        catch (_: IllegalArgumentException) {}
        assertTrue(db.inventoryDao().getAllInventorySnapshot().isEmpty())
        assertTrue(db.purchaseRecordDao().imports().isEmpty())
        assertTrue(db.backupDao().priceHistory().isEmpty())
    }
    @Test fun recipeAdditionUsesStockCompatibleListCoverageAndSelectedWeek() = runBlocking {
        val itemId = db.itemDao().insertItem(ItemEntity(name = "Pasta", defaultUnit = "g", category = "General"))
        db.inventoryDao().insertInventory(InventoryEntity(itemId = itemId, quantity = 100.0, unit = "g"))
        db.shoppingDao().insertShoppingItem(ShoppingItemEntity(name = "Pasta", quantity = 0.1, unit = "kg", sectionId = 3, weekId = "A"))
        db.shoppingDao().insertShoppingItem(ShoppingItemEntity(name = "Pasta", quantity = 1.0, unit = "kg", sectionId = 3, weekId = "B"))
        val ingredients = listOf(com.example.pantrypal.domain.recipe.RecipeIngredientNormalizer.parse("500 g pasta"))
        assertEquals(1, features().addMissingIngredientsToShopping(ingredients, "A"))
        assertEquals(0, features().addMissingIngredientsToShopping(ingredients, "A"))
        assertEquals(300.0, db.shoppingDao().getAllShoppingItemsSnapshot().first { it.sectionId == ShoppingSectionEntity.ID_MEAL_PLAN }.quantity, 0.0)
    }
    @Test fun unmeasuredQuickAddInheritsStockUnitAndUndoRestoresExactMergedRow() = runBlocking {
        val id = db.itemDao().insertItem(ItemEntity(name = "Milk", defaultUnit = "l", category = "Dairy & Fridge"))
        db.inventoryDao().insertInventory(InventoryEntity(itemId = id, quantity = 1.0, unit = "l"))
        val row = ShoppingItemEntity(name = "Milk", quantity = 2.0, unit = "l", sectionId = 3, weekId = "A")
        db.shoppingDao().insertShoppingItem(row)
        val change = repo.quickAddShoppingItem(ShoppingItemEntity(name = "Milk", quantity = 1.0, unit = "pcs", weekId = "A"), "A", inheritUnit = true)
        assertEquals(row.syncId, change.before!!.syncId)
        assertEquals(3.0, change.after.quantity, 0.0)
        repo.undoQuickAdd(change)
        assertEquals(2.0, db.shoppingDao().getAllShoppingItemsSnapshot().single().quantity, 0.0)
    }
}
