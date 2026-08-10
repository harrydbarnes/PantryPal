package com.example.pantrypal.data.backup

import com.example.pantrypal.data.entity.MealEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCodecTest {
    private val codec = BackupCodec()

    @Test
    fun completeBackup_roundTripsEveryCollection() {
        val original = completeDocument()

        val encoded = codec.encode(original)
        val decoded = codec.decode(encoded)

        assertTrue(decoded is BackupDecodeResult.Success)
        assertEquals(original, (decoded as BackupDecodeResult.Success).document)
    }

    @Test
    fun validation_rejectsBrokenReferencesAndInvalidAmounts() {
        val invalid = completeDocument().copy(
            payload = completeDocument().payload.copy(
                inventory = listOf(
                    BackupInventory(
                        inventoryId = 1,
                        itemId = 999,
                        quantity = -1.0,
                        unit = "pcs",
                        storageLocation = "Pantry"
                    )
                ),
                weeklyBudgets = listOf(
                    BackupWeeklyBudget(
                        weekStartEpochDay = 1,
                        budgetMinor = -1,
                        currencyCode = "gbp"
                    )
                )
            )
        )

        val validation = BackupValidator.validate(invalid)

        assertFalse(validation.isValid)
        assertTrue(validation.errors.any { "missing item 999" in it })
        assertTrue(validation.errors.any { "invalid quantity" in it })
        assertTrue(validation.errors.any { "negative target" in it })
        assertTrue(validation.errors.any { "currency code" in it })
    }

    @Test
    fun decode_rejectsFutureSchemaBeforeImport() {
        val future = codec.encode(completeDocument())
            .replace(
                "\"schemaVersion\":${BackupDocument.CURRENT_SCHEMA_VERSION}",
                "\"schemaVersion\":999"
            )

        val result = codec.decode(future)

        assertTrue(result is BackupDecodeResult.Failure)
        assertTrue(
            (result as BackupDecodeResult.Failure).errors.any {
                "Unsupported backup schema version" in it
            }
        )
    }

    @Test
    fun validation_rejectsValuesThatWouldViolateUniqueIndexes() {
        val payload = completeDocument().payload
        val invalid = completeDocument().copy(
            payload = payload.copy(
                items = payload.items + payload.items.first().copy(
                    itemId = 2,
                    name = "Other milk",
                    barcode = "123"
                ) + payload.items.first().copy(
                    itemId = 3,
                    name = "Third milk",
                    barcode = "123"
                ),
                recipes = payload.recipes + payload.recipes.first().copy(
                    recipeId = 2,
                    title = "  PORRIDGE  ",
                    normalizedTitle = null
                )
            )
        )

        val validation = BackupValidator.validate(invalid)

        assertFalse(validation.isValid)
        assertTrue(validation.errors.any { "Duplicate item barcode" in it })
        assertTrue(validation.errors.any { "Duplicate recipe normalized title" in it })
    }

    @Test
    fun mealEntityMapping_preservesRecipeAndFractionalServings() {
        val entity = MealEntity(
            mealId = 7,
            name = "Porridge",
            week = "B",
            ingredients = listOf("Oats", "Milk"),
            dayOfWeek = 3,
            mealSlot = MealEntity.SLOT_BREAKFAST,
            recipeId = 42,
            servings = 2.5
        )

        val restored = entity.toBackupMeal().toEntity()

        assertEquals(entity, restored)
    }

    @Test
    fun mealBackupMapping_usesDefaultServingsForLegacyNull() {
        val restored = BackupMeal(
            mealId = 7,
            name = "Porridge",
            weekId = "B",
            servings = null
        ).toEntity()

        assertEquals(MealEntity.DEFAULT_SERVINGS, restored.servings, 0.0)
    }

    private fun completeDocument(): BackupDocument = BackupDocument(
        schemaVersion = BackupDocument.CURRENT_SCHEMA_VERSION,
        exportId = "export-1",
        exportedAtEpochMs = 1_000,
        appVersion = "1.0",
        payload = BackupPayload(
            items = listOf(
                BackupItem(
                    itemId = 1,
                    name = "Milk",
                    defaultUnit = "l",
                    category = "Dairy",
                    lowStockThreshold = 1.0,
                    createdAt = 10
                )
            ),
            inventory = listOf(
                BackupInventory(
                    inventoryId = 1,
                    itemId = 1,
                    quantity = 2.0,
                    unit = "l",
                    addedDate = 20,
                    storageLocation = "Fridge",
                    isOpened = true
                )
            ),
            consumption = listOf(
                BackupConsumption(
                    eventId = 1,
                    itemId = 1,
                    date = 30,
                    quantity = 0.5,
                    type = "FINISHED"
                )
            ),
            shoppingSections = listOf(
                BackupShoppingSection(1, "Every week", 0, true, "EVERY_WEEK")
            ),
            shoppingItems = listOf(
                BackupShoppingItem(
                    shoppingId = 1,
                    name = "Milk",
                    sectionId = 1,
                    weekId = "A"
                )
            ),
            shoppingHistory = listOf(
                BackupShoppingHistory("milk", "Milk", 40)
            ),
            mealWeeks = listOf(BackupMealWeek("A", "Week A", "A", 0)),
            meals = listOf(
                BackupMeal(
                    mealId = 1,
                    name = "Porridge",
                    weekId = "A",
                    ingredients = listOf("Milk"),
                    recipeId = 1,
                    servings = 2.5
                )
            ),
            recipes = listOf(
                BackupRecipe(
                    recipeId = 1,
                    title = "Porridge",
                    normalizedTitle = "porridge",
                    sourceName = "Family",
                    servings = 2.5,
                    instructions = listOf("Simmer"),
                    rating = 5,
                    isFavourite = true,
                    createdAt = 50,
                    updatedAt = 50
                )
            ),
            recipeIngredients = listOf(
                BackupRecipeIngredient(
                    ingredientId = 1,
                    recipeId = 1,
                    rawText = "500ml milk",
                    name = "Milk",
                    normalizedName = "milk",
                    quantity = 500.0,
                    unit = "ml",
                    linkedPantryItemId = 1
                )
            ),
            priceHistory = listOf(
                BackupPriceHistory(
                    priceId = 1,
                    itemId = 1,
                    normalizedItemName = "milk",
                    displayName = "Milk",
                    priceMinor = 145,
                    retailer = "Local shop",
                    purchasedAt = 60
                )
            ),
            weeklyBudgets = listOf(
                BackupWeeklyBudget(20_293, 10_000, "GBP", 70)
            ),
            preferences = BackupPreferences(
                onboardingComplete = true,
                mealPlanIntroSeen = true,
                settingsIntroSeen = true,
                activeMealWeekId = "A",
                householdName = "Home",
                householdId = "household-1"
            )
        )
    )
}
