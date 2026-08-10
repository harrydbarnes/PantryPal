package com.example.pantrypal.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.example.pantrypal.BuildConfig
import com.example.pantrypal.data.api.RecipeTheMealDbSearch
import com.example.pantrypal.data.backup.BackupCodec
import com.example.pantrypal.data.backup.BackupDecodeResult
import com.example.pantrypal.data.backup.BackupDocument
import com.example.pantrypal.data.backup.BackupEntitySnapshot
import com.example.pantrypal.data.backup.BackupPayload
import com.example.pantrypal.data.backup.BackupPreferences
import com.example.pantrypal.data.backup.toEntity
import com.example.pantrypal.data.database.KitchenDatabase
import com.example.pantrypal.data.entity.BudgetWeeklyEntity
import com.example.pantrypal.data.entity.InventoryEntity
import com.example.pantrypal.data.entity.MealEntity
import com.example.pantrypal.data.entity.MealWeekEntity
import com.example.pantrypal.data.entity.PriceHistoryEntity
import com.example.pantrypal.data.entity.ShoppingItemEntity
import com.example.pantrypal.data.entity.ShoppingSectionEntity
import com.example.pantrypal.data.entity.toDomain
import com.example.pantrypal.data.entity.toEntity as toRecipeEntity
import com.example.pantrypal.data.household.HouseholdSnapshotCodec
import com.example.pantrypal.data.household.HouseholdSnapshotDecodeResult
import com.example.pantrypal.data.household.HouseholdSnapshotPayload
import com.example.pantrypal.domain.receipt.ReceiptParser
import com.example.pantrypal.domain.receipt.ReceiptReviewCandidate
import com.example.pantrypal.domain.recipe.Recipe
import com.example.pantrypal.domain.recipe.RecipeExternalSearchMode
import com.example.pantrypal.domain.recipe.RecipeIngredient
import com.example.pantrypal.domain.recipe.RecipePantryIngredient
import com.example.pantrypal.util.AppPreferences
import com.example.pantrypal.util.RecipeJsonLdImporter
import com.example.pantrypal.util.RecipeMealConversion
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.time.LocalDate
import java.util.UUID
import java.io.Reader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Repository for the optional planning tools layered on top of the core kitchen repository.
 * The core repository remains small, while this class owns recipes, receipts, budgets and
 * complete portable backups.
 */
class PantryFeaturesRepository(
    private val context: Context,
    private val database: KitchenDatabase,
    private val kitchenRepository: KitchenRepository
) {
    private val backupDao = database.backupDao()
    private val recipeDao = database.recipeDao()
    private val priceHistoryDao = database.priceHistoryDao()
    private val budgetDao = database.budgetWeeklyDao()
    private val preferences =
        context.getSharedPreferences(AppPreferences.FILE_NAME, Context.MODE_PRIVATE)
    private val backupCodec = BackupCodec()
    private val householdCodec = HouseholdSnapshotCodec()
    private val recipeImporter = RecipeJsonLdImporter()
    private val recipeSearch by lazy { RecipeTheMealDbSearch.create() }

    val recipes: Flow<List<Recipe>> = recipeDao.observeRecipes()
        .map { rows -> rows.map { it.toDomain() } }

    val pantryForRecipes: Flow<List<RecipePantryIngredient>> =
        kitchenRepository.currentInventory.map { rows ->
            rows.groupBy { it.itemId }.map { (itemId, batches) ->
                val first = batches.first()
                RecipePantryIngredient(
                    itemId = itemId,
                    name = first.name,
                    quantity = batches.sumOf { it.quantity },
                    unit = first.unit,
                    expirationDate = batches.mapNotNull { it.expirationDate }.minOrNull()
                )
            }
        }

    val prices: Flow<List<PriceHistoryEntity>> = priceHistoryDao.observeAll()
    val budgets: Flow<List<BudgetWeeklyEntity>> = budgetDao.observeAll()

    suspend fun bootstrapRecipesFromMeals() {
        val existing = recipes.first()
        val generated = RecipeMealConversion.fromMeals(
            meals = kitchenRepository.allMeals.first(),
            existingRecipes = existing
        )
        generated.forEach { saveRecipe(it) }
    }

    suspend fun saveRecipe(recipe: Recipe): Long = recipeDao.saveRecipe(
        recipe = recipe.copy(updatedAt = System.currentTimeMillis()).toRecipeEntity(),
        ingredients = recipe.ingredients.map { it.toRecipeEntity(recipe.id) }
    )

    suspend fun searchOnline(
        query: String,
        mode: RecipeExternalSearchMode
    ): List<Recipe> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return@withContext emptyList()
        recipeSearch.search(trimmed, mode).getOrThrow().take(MAX_EXTERNAL_RESULTS)
    }

    suspend fun importRecipeUrl(url: String): Recipe = withContext(Dispatchers.IO) {
        val uri = URI(url.trim())
        require(uri.scheme.equals("https", ignoreCase = true)) {
            "Recipe links must use https."
        }
        require(!uri.host.isNullOrBlank()) { "Recipe link has no host." }

        val connection = URL(uri.toString()).openConnection() as HttpURLConnection
        connection.connectTimeout = URL_TIMEOUT_MILLIS
        connection.readTimeout = URL_TIMEOUT_MILLIS
        connection.instanceFollowRedirects = true
        connection.setRequestProperty("User-Agent", "PantryPal/${BuildConfig.VERSION_NAME}")
        try {
            connection.inputStream.bufferedReader().use { reader ->
                val html = reader.readTextBounded(MAX_RECIPE_PAGE_CHARS)
                recipeImporter.importFromHtml(html, uri.toString()).getOrThrow()
            }
        } finally {
            connection.disconnect()
        }
    }

    suspend fun addRecipeToPlan(recipe: Recipe, weekId: String): MealEntity {
        val weekMeals = kitchenRepository.allMeals.first().filter { it.week == weekId }
        val occupiedDays = weekMeals
            .filter { it.mealSlot == MealEntity.SLOT_DINNER }
            .mapTo(mutableSetOf()) { it.dayOfWeek }
        val day = (1..7).firstOrNull { it !in occupiedDays } ?: 7
        val savedId = if (recipe.id > 0) recipe.id else saveRecipe(recipe)
        val meal = MealEntity(
            name = recipe.title,
            week = weekId,
            ingredients = recipe.ingredients
                .filterNot(RecipeIngredient::isOptional)
                .map { it.rawText.ifBlank { it.name } },
            dayOfWeek = day,
            mealSlot = MealEntity.SLOT_DINNER,
            recipeId = savedId,
            servings = recipe.servings ?: MealEntity.DEFAULT_SERVINGS
        )
        kitchenRepository.insertMeal(meal)
        return meal
    }

    suspend fun addMissingIngredientsToShopping(
        ingredients: List<RecipeIngredient>,
        weekId: String
    ): Int {
        val existing = kitchenRepository.shoppingList.first()
            .filterNot { it.isChecked }
            .mapTo(mutableSetOf()) { ReceiptParser.normalizeName(it.name) }
        var added = 0
        ingredients.filterNot(RecipeIngredient::isOptional).forEach { ingredient ->
            if (ingredient.normalizedName !in existing) {
                kitchenRepository.addShoppingItem(
                    ShoppingItemEntity(
                        name = ingredient.name,
                        quantity = ingredient.quantity ?: 1.0,
                        unit = ingredient.unit ?: "pcs",
                        sectionId = ShoppingSectionEntity.ID_MEAL_PLAN,
                        weekId = weekId
                    )
                )
                kitchenRepository.rememberShoppingItem(ingredient.name)
                existing += ingredient.normalizedName
                added += 1
            }
        }
        return added
    }

    suspend fun importReceiptPurchases(
        candidates: List<ReceiptReviewCandidate>,
        storageLocation: String = InventoryEntity.LOCATION_PANTRY
    ): Int = database.withTransaction {
        val selected = candidates.filter { it.isIncluded && it.name.isNotBlank() }
        val now = System.currentTimeMillis()
        priceHistoryDao.upsertAll(
            selected.map { candidate ->
                PriceHistoryEntity(
                    normalizedItemName = candidate.normalizedName,
                    displayName = candidate.name,
                    priceMinor = candidate.totalPriceMinor,
                    quantity = candidate.quantity,
                    unit = candidate.unit,
                    purchasedAt = now,
                    currencyCode = candidate.currencyCode,
                    source = PriceHistoryEntity.SOURCE_RECEIPT
                )
            }
        )
        kitchenRepository.putAwayShoppingItems(
            selected.map { candidate ->
                ShoppingItemEntity(
                    name = candidate.name,
                    quantity = candidate.quantity,
                    unit = candidate.unit,
                    isChecked = true
                )
            },
            storageLocation
        )
        selected.size
    }

    suspend fun setWeeklyBudget(
        amountMinor: Long,
        currencyCode: String = DEFAULT_CURRENCY
    ) {
        val week = com.example.pantrypal.domain.budget.BudgetCalculator
            .mondayFor(LocalDate.now())
        budgetDao.upsert(
            BudgetWeeklyEntity(
                weekStartEpochDay = week.toEpochDay(),
                budgetMinor = amountMinor.coerceAtLeast(0),
                currencyCode = currencyCode,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun exportBackupJson(): String = backupCodec.encode(
        BackupDocument(
            appVersion = BuildConfig.VERSION_NAME,
            payload = snapshot().toPayload()
        ),
        pretty = true
    )

    suspend fun restoreBackupJson(json: String): BackupDecodeResult {
        val result = backupCodec.decode(json)
        if (result is BackupDecodeResult.Success) {
            restore(result.document.payload)
        }
        return result
    }

    suspend fun exportHouseholdSnapshot(): String {
        val backup = BackupDocument(
            appVersion = BuildConfig.VERSION_NAME,
            payload = snapshot().toPayload()
        )
        val now = System.currentTimeMillis()
        val householdId = householdId()
        val revision = preferences.getLong(KEY_HOUSEHOLD_REVISION, 0L) + 1L
        preferences.edit().putLong(KEY_HOUSEHOLD_REVISION, revision).apply()
        return householdCodec.encode(
            HouseholdSnapshotPayload(
                householdId = householdId,
                householdName = preferences.getString(KEY_HOUSEHOLD_NAME, null)
                    ?: DEFAULT_HOUSEHOLD_NAME,
                snapshotId = UUID.randomUUID().toString(),
                createdAtEpochMs = now,
                createdByDeviceId = deviceId(),
                baseRevision = (revision - 1).coerceAtLeast(0),
                revision = revision,
                completeBackup = backup
            ),
            pretty = true
        )
    }

    suspend fun importHouseholdSnapshot(json: String): Result<Int> =
        when (val result = householdCodec.decode(json)) {
            is HouseholdSnapshotDecodeResult.Failure ->
                Result.failure(IllegalArgumentException(result.errors.joinToString("\n")))

            is HouseholdSnapshotDecodeResult.Success -> runCatching {
                restore(result.payload.completeBackup.payload)
                preferences.edit()
                    .putString(KEY_HOUSEHOLD_ID, result.payload.householdId)
                    .putString(KEY_HOUSEHOLD_NAME, result.payload.householdName)
                    .putLong(KEY_HOUSEHOLD_REVISION, result.payload.revision)
                    .apply()
                result.warnings.size
            }
        }

    private suspend fun snapshot(): BackupEntitySnapshot = database.withTransaction {
        BackupEntitySnapshot(
            items = backupDao.items(),
            inventory = backupDao.inventory(),
            consumption = backupDao.consumption(),
            shoppingSections = backupDao.shoppingSections(),
            shoppingItems = backupDao.shoppingItems(),
            shoppingHistory = backupDao.shoppingHistory(),
            mealWeeks = backupDao.mealWeeks(),
            meals = backupDao.meals(),
            recipes = backupDao.recipes(),
            recipeIngredients = backupDao.recipeIngredients(),
            priceHistory = backupDao.priceHistory(),
            weeklyBudgets = backupDao.weeklyBudgets(),
            preferences = BackupPreferences(
                onboardingComplete = preferences.getBoolean(
                    AppPreferences.KEY_ONBOARDING_COMPLETE,
                    false
                ),
                themeMode = preferences.getString(
                    AppPreferences.KEY_THEME_MODE,
                    "SYSTEM"
                ) ?: "SYSTEM",
                dynamicColorEnabled = preferences.getBoolean(
                    AppPreferences.KEY_DYNAMIC_COLOR,
                    true
                ),
                expiryRemindersEnabled = preferences.getBoolean(
                    AppPreferences.KEY_EXPIRY_REMINDERS,
                    true
                ),
                shoppingRemindersEnabled = preferences.getBoolean(
                    AppPreferences.KEY_SHOPPING_REMINDERS,
                    false
                ),
                shoppingDayOfWeek = preferences.getInt(
                    AppPreferences.KEY_SHOPPING_DAY,
                    AppPreferences.DEFAULT_SHOPPING_DAY
                ),
                shoppingTimeMinutes = preferences.getInt(
                    AppPreferences.KEY_SHOPPING_TIME,
                    AppPreferences.DEFAULT_SHOPPING_TIME_MINUTES
                ),
                activeMealWeekId = preferences.getString(KEY_CURRENT_WEEK, null),
                defaultCurrencyCode = DEFAULT_CURRENCY,
                householdName = preferences.getString(KEY_HOUSEHOLD_NAME, null),
                householdId = preferences.getString(KEY_HOUSEHOLD_ID, null)
            )
        )
    }

    private suspend fun restore(payload: BackupPayload) {
        database.withTransaction {
            val restoredItemIds = payload.items.mapTo(mutableSetOf()) { it.itemId }
            backupDao.clearRecipeIngredients()
            backupDao.clearInventory()
            backupDao.clearConsumption()
            backupDao.clearShoppingItems()
            backupDao.clearShoppingHistory()
            backupDao.clearMeals()
            backupDao.clearPriceHistory()
            backupDao.clearWeeklyBudgets()
            backupDao.clearRecipes()
            backupDao.clearItems()
            backupDao.clearShoppingSections()
            backupDao.clearMealWeeks()

            backupDao.insertItems(payload.items.map { it.toEntity() })
            backupDao.insertShoppingSections(
                payload.shoppingSections.map { it.toEntity() }
                    .ifEmpty { defaultShoppingSections() }
            )
            backupDao.insertMealWeeks(
                payload.mealWeeks.map { it.toEntity() }.ifEmpty { defaultMealWeeks() }
            )
            backupDao.insertRecipes(payload.recipes.map { it.toEntity() })
            backupDao.insertRecipeIngredients(
                payload.recipeIngredients.map {
                    it.toEntity().copy(
                        linkedPantryItemId = it.linkedPantryItemId
                            ?.takeIf(restoredItemIds::contains)
                    )
                }
            )
            backupDao.insertInventory(payload.inventory.map { it.toEntity() })
            backupDao.insertConsumption(payload.consumption.map { it.toEntity() })
            backupDao.insertShoppingItems(payload.shoppingItems.map { it.toEntity() })
            backupDao.insertShoppingHistory(payload.shoppingHistory.map { it.toEntity() })
            backupDao.insertMeals(payload.meals.map { it.toEntity() })
            backupDao.insertPriceHistory(payload.priceHistory.map { it.toEntity() })
            backupDao.insertWeeklyBudgets(payload.weeklyBudgets.map { it.toEntity() })
        }
        val restored = payload.preferences
        preferences.edit()
            .putBoolean(AppPreferences.KEY_ONBOARDING_COMPLETE, restored.onboardingComplete)
            .putString(AppPreferences.KEY_THEME_MODE, restored.themeMode)
            .putBoolean(AppPreferences.KEY_DYNAMIC_COLOR, restored.dynamicColorEnabled)
            .putBoolean(AppPreferences.KEY_EXPIRY_REMINDERS, restored.expiryRemindersEnabled)
            .putBoolean(AppPreferences.KEY_SHOPPING_REMINDERS, restored.shoppingRemindersEnabled)
            .putInt(
                AppPreferences.KEY_SHOPPING_DAY,
                restored.shoppingDayOfWeek.takeIf { it in 1..7 }
                    ?: AppPreferences.DEFAULT_SHOPPING_DAY
            )
            .putInt(
                AppPreferences.KEY_SHOPPING_TIME,
                restored.shoppingTimeMinutes.coerceIn(0, 23 * 60 + 59)
            )
            .putString(KEY_CURRENT_WEEK, restored.activeMealWeekId ?: MealEntity.WEEK_A)
            .putString(KEY_HOUSEHOLD_NAME, restored.householdName)
            .putString(KEY_HOUSEHOLD_ID, restored.householdId)
            .apply()
    }

    private fun householdId(): String =
        preferences.getString(KEY_HOUSEHOLD_ID, null)
            ?: UUID.randomUUID().toString().also {
                preferences.edit().putString(KEY_HOUSEHOLD_ID, it).apply()
            }

    private fun deviceId(): String =
        preferences.getString(KEY_DEVICE_ID, null)
            ?: UUID.randomUUID().toString().also {
                preferences.edit().putString(KEY_DEVICE_ID, it).apply()
            }

    private fun defaultMealWeeks(): List<MealWeekEntity> = listOf(
        MealWeekEntity("A", "Week A", "A", 0),
        MealWeekEntity("B", "Week B", "B", 1),
        MealWeekEntity("C", "Week C", "C", 2),
        MealWeekEntity("D", "Week D", "D", 3)
    )

    private fun defaultShoppingSections(): List<ShoppingSectionEntity> = listOf(
        ShoppingSectionEntity(
            ShoppingSectionEntity.ID_EVERY_WEEK,
            "Every week",
            0,
            true,
            ShoppingSectionEntity.KEY_EVERY_WEEK
        ),
        ShoppingSectionEntity(
            ShoppingSectionEntity.ID_BABY_STUFF,
            "Baby stuff",
            1,
            true,
            ShoppingSectionEntity.KEY_BABY_STUFF
        ),
        ShoppingSectionEntity(
            ShoppingSectionEntity.ID_MEAL_PLAN,
            "Meal plan",
            2,
            false,
            ShoppingSectionEntity.KEY_MEAL_PLAN
        ),
        ShoppingSectionEntity(
            ShoppingSectionEntity.ID_THE_REST,
            "The rest",
            3,
            false,
            ShoppingSectionEntity.KEY_THE_REST
        )
    )

    private fun Reader.readTextBounded(maxChars: Int): String {
        val output = StringBuilder(minOf(maxChars, 16_384))
        val buffer = CharArray(8_192)
        while (true) {
            val read = read(buffer)
            if (read < 0) break
            require(output.length + read <= maxChars) {
                "Recipe page is too large to import safely."
            }
            output.append(buffer, 0, read)
        }
        return output.toString()
    }

    private companion object {
        const val MAX_EXTERNAL_RESULTS = 12
        const val MAX_RECIPE_PAGE_CHARS = 2_000_000
        const val URL_TIMEOUT_MILLIS = 15_000
        const val DEFAULT_CURRENCY = "GBP"
        const val DEFAULT_HOUSEHOLD_NAME = "My household"
        const val KEY_CURRENT_WEEK = "current_week"
        const val KEY_HOUSEHOLD_ID = "household_id"
        const val KEY_HOUSEHOLD_NAME = "household_name"
        const val KEY_HOUSEHOLD_REVISION = "household_revision"
        const val KEY_DEVICE_ID = "household_device_id"
    }
}
