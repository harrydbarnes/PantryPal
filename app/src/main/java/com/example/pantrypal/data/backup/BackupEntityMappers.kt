package com.example.pantrypal.data.backup

import com.example.pantrypal.data.entity.BudgetWeeklyEntity
import com.example.pantrypal.data.entity.ConsumptionEntity
import com.example.pantrypal.data.entity.ConsumptionType
import com.example.pantrypal.data.entity.InventoryEntity
import com.example.pantrypal.data.entity.ItemEntity
import com.example.pantrypal.data.entity.MealEntity
import com.example.pantrypal.data.entity.MealWeekEntity
import com.example.pantrypal.data.entity.PriceHistoryEntity
import com.example.pantrypal.data.entity.RecipeEntity
import com.example.pantrypal.data.entity.RecipeIngredientEntity
import com.example.pantrypal.data.entity.ShoppingHistoryEntity
import com.example.pantrypal.data.entity.ShoppingItemEntity
import com.example.pantrypal.data.entity.ShoppingSectionEntity
import java.util.Locale

fun ItemEntity.toBackupItem(): BackupItem = BackupItem(
    itemId = itemId,
    name = name,
    barcode = barcode,
    defaultUnit = defaultUnit,
    category = category,
    isVegetarian = isVegetarian,
    isGlutenFree = isGlutenFree,
    isUsual = isUsual,
    lowStockThreshold = lowStockThreshold,
    imageUrl = imageUrl,
    createdAt = createdAt
)

fun BackupItem.toEntity(): ItemEntity = ItemEntity(
    itemId = itemId,
    name = name,
    barcode = barcode,
    defaultUnit = defaultUnit,
    category = category,
    isVegetarian = isVegetarian,
    isGlutenFree = isGlutenFree,
    isUsual = isUsual,
    lowStockThreshold = lowStockThreshold,
    imageUrl = imageUrl,
    createdAt = createdAt
)

fun InventoryEntity.toBackupInventory(): BackupInventory = BackupInventory(
    inventoryId = inventoryId,
    itemId = itemId,
    quantity = quantity,
    unit = unit,
    addedDate = addedDate,
    expirationDate = expirationDate,
    storageLocation = storageLocation,
    isOpened = isOpened
)

fun BackupInventory.toEntity(): InventoryEntity = InventoryEntity(
    inventoryId = inventoryId,
    itemId = itemId,
    quantity = quantity,
    unit = unit,
    addedDate = addedDate,
    expirationDate = expirationDate,
    storageLocation = storageLocation,
    isOpened = isOpened
)

fun ConsumptionEntity.toBackupConsumption(): BackupConsumption = BackupConsumption(
    eventId = eventId,
    itemId = itemId,
    date = date,
    quantity = quantity,
    type = type.name,
    wasteReason = wasteReason
)

fun BackupConsumption.toEntity(): ConsumptionEntity = ConsumptionEntity(
    eventId = eventId,
    itemId = itemId,
    date = date,
    quantity = quantity,
    type = ConsumptionType.valueOf(type),
    wasteReason = wasteReason
)

fun ShoppingSectionEntity.toBackupShoppingSection(): BackupShoppingSection =
    BackupShoppingSection(sectionId, name, sortOrder, recursEveryWeek, systemKey)

fun BackupShoppingSection.toEntity(): ShoppingSectionEntity =
    ShoppingSectionEntity(sectionId, name, sortOrder, recursEveryWeek, systemKey)

fun ShoppingItemEntity.toBackupShoppingItem(): BackupShoppingItem = BackupShoppingItem(
    shoppingId = shoppingId,
    name = name,
    quantity = quantity,
    unit = unit,
    isChecked = isChecked,
    addedAt = addedAt,
    frequency = frequency,
    sectionId = sectionId,
    weekId = weekId
)

fun BackupShoppingItem.toEntity(): ShoppingItemEntity = ShoppingItemEntity(
    shoppingId = shoppingId,
    name = name,
    quantity = quantity,
    unit = unit,
    isChecked = isChecked,
    addedAt = addedAt,
    frequency = frequency,
    sectionId = sectionId,
    weekId = weekId
)

fun ShoppingHistoryEntity.toBackupShoppingHistory(): BackupShoppingHistory =
    BackupShoppingHistory(normalizedName, displayName, lastUsedAt)

fun BackupShoppingHistory.toEntity(): ShoppingHistoryEntity =
    ShoppingHistoryEntity(normalizedName, displayName, lastUsedAt)

fun MealWeekEntity.toBackupMealWeek(): BackupMealWeek =
    BackupMealWeek(weekId, name, emoji, sortOrder)

fun BackupMealWeek.toEntity(): MealWeekEntity =
    MealWeekEntity(weekId, name, emoji, sortOrder)

fun MealEntity.toBackupMeal(): BackupMeal = BackupMeal(
    mealId = mealId,
    name = name,
    weekId = week,
    ingredients = ingredients,
    dayOfWeek = dayOfWeek,
    mealSlot = mealSlot,
    recipeId = recipeId,
    servings = servings
)

fun BackupMeal.toEntity(): MealEntity = MealEntity(
    mealId = mealId,
    name = name,
    week = weekId,
    ingredients = ingredients,
    dayOfWeek = dayOfWeek,
    mealSlot = mealSlot,
    recipeId = recipeId,
    servings = servings ?: MealEntity.DEFAULT_SERVINGS
)

fun RecipeEntity.toBackupRecipe(): BackupRecipe = BackupRecipe(
    recipeId = recipeId,
    title = title,
    normalizedTitle = normalizedTitle,
    sourceUrl = sourceUrl,
    sourceName = sourceName,
    attribution = attribution,
    externalId = externalId,
    imageUrl = imageUrl,
    yieldText = yieldText,
    servings = servings,
    prepTimeMinutes = prepTimeMinutes,
    cookTimeMinutes = cookTimeMinutes,
    totalTimeMinutes = totalTimeMinutes,
    instructions = instructions,
    rating = rating,
    tags = tags,
    isFavourite = isFavourite,
    lastCookedAt = lastCookedAt,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun BackupRecipe.toEntity(): RecipeEntity = RecipeEntity(
    recipeId = recipeId,
    title = title,
    normalizedTitle = normalizedTitle
        ?.takeIf(String::isNotBlank)
        ?: title.trim().lowercase(Locale.ROOT).replace(Regex("""\s+"""), " "),
    sourceUrl = sourceUrl,
    sourceName = sourceName,
    attribution = attribution,
    externalId = externalId,
    imageUrl = imageUrl,
    yieldText = yieldText,
    servings = servings,
    prepTimeMinutes = prepTimeMinutes,
    cookTimeMinutes = cookTimeMinutes,
    totalTimeMinutes = totalTimeMinutes,
    instructions = instructions,
    tags = tags,
    rating = rating,
    isFavourite = isFavourite,
    lastCookedAt = lastCookedAt,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun RecipeIngredientEntity.toBackupRecipeIngredient(): BackupRecipeIngredient =
    BackupRecipeIngredient(
        ingredientId = ingredientId,
        recipeId = recipeId,
        rawText = rawText,
        name = name,
        normalizedName = normalizedName,
        quantity = quantity,
        unit = unit,
        isOptional = isOptional,
        linkedPantryItemId = linkedPantryItemId,
        sortOrder = sortOrder
    )

fun BackupRecipeIngredient.toEntity(): RecipeIngredientEntity = RecipeIngredientEntity(
    ingredientId = ingredientId,
    recipeId = recipeId,
    rawText = rawText,
    name = name,
    normalizedName = normalizedName,
    quantity = quantity,
    unit = unit,
    isOptional = isOptional,
    linkedPantryItemId = linkedPantryItemId,
    sortOrder = sortOrder
)

fun PriceHistoryEntity.toBackupPriceHistory(): BackupPriceHistory = BackupPriceHistory(
    priceId = priceId,
    itemId = itemId,
    normalizedItemName = normalizedItemName,
    displayName = displayName,
    priceMinor = priceMinor,
    quantity = quantity,
    unit = unit,
    retailer = retailer,
    purchasedAt = purchasedAt,
    currencyCode = currencyCode,
    source = source
)

fun BackupPriceHistory.toEntity(): PriceHistoryEntity = PriceHistoryEntity(
    priceId = priceId,
    itemId = itemId,
    normalizedItemName = normalizedItemName,
    displayName = displayName,
    priceMinor = priceMinor,
    quantity = quantity,
    unit = unit,
    retailer = retailer,
    purchasedAt = purchasedAt,
    currencyCode = currencyCode,
    source = source
)

fun BudgetWeeklyEntity.toBackupWeeklyBudget(): BackupWeeklyBudget =
    BackupWeeklyBudget(weekStartEpochDay, budgetMinor, currencyCode, updatedAt)

fun BackupWeeklyBudget.toEntity(): BudgetWeeklyEntity =
    BudgetWeeklyEntity(weekStartEpochDay, budgetMinor, currencyCode, updatedAt)

data class BackupEntitySnapshot(
    val items: List<ItemEntity> = emptyList(),
    val inventory: List<InventoryEntity> = emptyList(),
    val consumption: List<ConsumptionEntity> = emptyList(),
    val shoppingSections: List<ShoppingSectionEntity> = emptyList(),
    val shoppingItems: List<ShoppingItemEntity> = emptyList(),
    val shoppingHistory: List<ShoppingHistoryEntity> = emptyList(),
    val mealWeeks: List<MealWeekEntity> = emptyList(),
    val meals: List<MealEntity> = emptyList(),
    val recipes: List<RecipeEntity> = emptyList(),
    val recipeIngredients: List<RecipeIngredientEntity> = emptyList(),
    val priceHistory: List<PriceHistoryEntity> = emptyList(),
    val weeklyBudgets: List<BudgetWeeklyEntity> = emptyList(),
    val shoppingLocations: List<BackupShoppingLocation> = emptyList(),
    val preferences: BackupPreferences = BackupPreferences()
) {
    fun toPayload(): BackupPayload = BackupPayload(
        items = items.map(ItemEntity::toBackupItem),
        inventory = inventory.map(InventoryEntity::toBackupInventory),
        consumption = consumption.map(ConsumptionEntity::toBackupConsumption),
        shoppingSections = shoppingSections.map(ShoppingSectionEntity::toBackupShoppingSection),
        shoppingItems = shoppingItems.map(ShoppingItemEntity::toBackupShoppingItem),
        shoppingHistory = shoppingHistory.map(ShoppingHistoryEntity::toBackupShoppingHistory),
        mealWeeks = mealWeeks.map(MealWeekEntity::toBackupMealWeek),
        meals = meals.map(MealEntity::toBackupMeal),
        recipes = recipes.map(RecipeEntity::toBackupRecipe),
        recipeIngredients = recipeIngredients.map(RecipeIngredientEntity::toBackupRecipeIngredient),
        priceHistory = priceHistory.map(PriceHistoryEntity::toBackupPriceHistory),
        weeklyBudgets = weeklyBudgets.map(BudgetWeeklyEntity::toBackupWeeklyBudget),
        shoppingLocations = shoppingLocations,
        preferences = preferences
    )
}
