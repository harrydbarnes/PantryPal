package com.example.pantrypal.data.backup

import java.util.UUID

data class BackupDocument(
    val format: String = FORMAT,
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val exportId: String = UUID.randomUUID().toString(),
    val exportedAtEpochMs: Long = System.currentTimeMillis(),
    val appVersion: String? = null,
    val payload: BackupPayload = BackupPayload()
) {
    companion object {
        const val FORMAT = "pantrypal-complete-backup"
        const val CURRENT_SCHEMA_VERSION = 1
        const val MIN_SUPPORTED_SCHEMA_VERSION = 1
    }
}

data class BackupPayload(
    val items: List<BackupItem> = emptyList(),
    val inventory: List<BackupInventory> = emptyList(),
    val consumption: List<BackupConsumption> = emptyList(),
    val shoppingSections: List<BackupShoppingSection> = emptyList(),
    val shoppingItems: List<BackupShoppingItem> = emptyList(),
    val shoppingArchive: List<BackupShoppingArchive> = emptyList(),
    val shoppingHistory: List<BackupShoppingHistory> = emptyList(),
    val mealWeeks: List<BackupMealWeek> = emptyList(),
    val meals: List<BackupMeal> = emptyList(),
    val recipes: List<BackupRecipe> = emptyList(),
    val recipeIngredients: List<BackupRecipeIngredient> = emptyList(),
    val priceHistory: List<BackupPriceHistory> = emptyList(),
    val weeklyBudgets: List<BackupWeeklyBudget> = emptyList(),
    val shoppingLocations: List<BackupShoppingLocation> = emptyList(),
    val preferences: BackupPreferences = BackupPreferences()
)

data class BackupItem(
    val itemId: Long = 0,
    val name: String = "",
    val barcode: String? = null,
    val defaultUnit: String = "pcs",
    val category: String = "General",
    val isVegetarian: Boolean = false,
    val isGlutenFree: Boolean = false,
    val isUsual: Boolean = false,
    val lowStockThreshold: Double? = null,
    val imageUrl: String? = null,
    val createdAt: Long = 0
)

data class BackupInventory(
    val inventoryId: Long = 0,
    val itemId: Long = 0,
    val quantity: Double = 0.0,
    val unit: String = "pcs",
    val addedDate: Long = 0,
    val expirationDate: Long? = null,
    val storageLocation: String = "Pantry",
    val isOpened: Boolean = false
)

data class BackupConsumption(
    val eventId: Long = 0,
    val itemId: Long = 0,
    val date: Long = 0,
    val quantity: Double = 0.0,
    val type: String = "FINISHED",
    val wasteReason: String? = null
)

data class BackupShoppingSection(
    val sectionId: Long = 0,
    val name: String = "",
    val sortOrder: Int = 0,
    val recursEveryWeek: Boolean = false,
    val systemKey: String? = null
)

data class BackupShoppingItem(
    val shoppingId: Long = 0,
    val name: String = "",
    val quantity: Double = 1.0,
    val unit: String = "pcs",
    val isChecked: Boolean = false,
    val addedAt: Long = 0,
    val frequency: String = "One-Off",
    val sectionId: Long = 0,
    val weekId: String? = null
)

data class BackupShoppingArchive(
    val archiveId: Long = 0,
    val tripId: String = "",
    val weekId: String = "",
    val name: String = "",
    val quantity: Double = 0.0,
    val unit: String = "pcs",
    val sectionName: String = "",
    val completedAt: Long = 0,
    val storageLocation: String? = null
)

data class BackupShoppingHistory(
    val normalizedName: String = "",
    val displayName: String = "",
    val lastUsedAt: Long = 0
)

data class BackupMealWeek(
    val weekId: String = "",
    val name: String = "",
    val emoji: String = "",
    val sortOrder: Int = 0
)

data class BackupMeal(
    val mealId: Long = 0,
    val name: String = "",
    val weekId: String = "",
    val ingredients: List<String> = emptyList(),
    val dayOfWeek: Int = 1,
    val mealSlot: String = "Dinner",
    val recipeId: Long? = null,
    val servings: Double? = null
)

data class BackupRecipe(
    val recipeId: Long = 0,
    val title: String = "",
    val normalizedTitle: String? = null,
    val sourceUrl: String? = null,
    val sourceName: String? = null,
    val attribution: String? = null,
    val externalId: String? = null,
    val imageUrl: String? = null,
    val yieldText: String? = null,
    val servings: Double? = null,
    val prepTimeMinutes: Int? = null,
    val cookTimeMinutes: Int? = null,
    val totalTimeMinutes: Int? = null,
    val instructions: List<String> = emptyList(),
    val rating: Int? = null,
    val tags: List<String> = emptyList(),
    val isFavourite: Boolean = false,
    val lastCookedAt: Long? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0
)

data class BackupRecipeIngredient(
    val ingredientId: Long = 0,
    val recipeId: Long = 0,
    val rawText: String = "",
    val name: String = "",
    val normalizedName: String = "",
    val quantity: Double? = null,
    val unit: String? = null,
    val isOptional: Boolean = false,
    val linkedPantryItemId: Long? = null,
    val sortOrder: Int = 0
)

data class BackupPriceHistory(
    val priceId: Long = 0,
    val itemId: Long? = null,
    val normalizedItemName: String = "",
    val displayName: String = "",
    val priceMinor: Long = 0,
    val quantity: Double = 1.0,
    val unit: String = "pcs",
    val retailer: String? = null,
    val purchasedAt: Long = 0,
    val currencyCode: String = "GBP",
    val source: String = "MANUAL"
)

data class BackupWeeklyBudget(
    val weekStartEpochDay: Long = 0,
    val budgetMinor: Long = 0,
    val currencyCode: String = "GBP",
    val updatedAt: Long = 0
)

data class BackupShoppingLocation(
    val id: String = "",
    val name: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val radiusMeters: Float = 200f
)

data class BackupPreferences(
    val onboardingComplete: Boolean = false,
    val mealPlanIntroSeen: Boolean = false,
    val settingsIntroSeen: Boolean = false,
    val themeMode: String = "SYSTEM",
    val dynamicColorEnabled: Boolean = true,
    val expiryRemindersEnabled: Boolean = true,
    val shoppingRemindersEnabled: Boolean = false,
    val shoppingDayOfWeek: Int = 6,
    val shoppingTimeMinutes: Int = 600,
    val shoppingReminderTiming: String = "NIGHT_BEFORE",
    val nearbyShoppingRemindersEnabled: Boolean = false,
    val activeMealWeekId: String? = null,
    val defaultCurrencyCode: String = "GBP",
    val householdName: String? = null,
    val householdId: String? = null
)
