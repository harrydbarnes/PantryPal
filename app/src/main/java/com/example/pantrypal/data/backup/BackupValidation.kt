package com.example.pantrypal.data.backup

import java.util.Locale

data class BackupValidationResult(
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList()
) {
    val isValid: Boolean
        get() = errors.isEmpty()
}

object BackupValidator {
    fun validate(document: BackupDocument): BackupValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        val payload = document.payload

        if (document.format != BackupDocument.FORMAT) {
            errors += "Unsupported backup format '${document.format}'."
        }
        if (
            document.schemaVersion !in
            BackupDocument.MIN_SUPPORTED_SCHEMA_VERSION..BackupDocument.CURRENT_SCHEMA_VERSION
        ) {
            errors += "Unsupported backup schema version ${document.schemaVersion}."
        }
        if (document.exportId.isBlank()) errors += "Export ID is missing."
        if (document.exportedAtEpochMs <= 0) warnings += "Export timestamp is missing or invalid."

        checkUniqueIds("item", payload.items.map(BackupItem::itemId), errors)
        checkUniqueIds("inventory", payload.inventory.map(BackupInventory::inventoryId), errors)
        checkUniqueIds("consumption event", payload.consumption.map(BackupConsumption::eventId), errors)
        checkUniqueIds("shopping section", payload.shoppingSections.map(BackupShoppingSection::sectionId), errors)
        checkUniqueIds("shopping item", payload.shoppingItems.map(BackupShoppingItem::shoppingId), errors)
        checkUniqueStrings(
            "shopping history name",
            payload.shoppingHistory.map(BackupShoppingHistory::normalizedName),
            errors
        )
        checkUniqueStrings("meal week", payload.mealWeeks.map(BackupMealWeek::weekId), errors)
        checkUniqueIds("meal", payload.meals.map(BackupMeal::mealId), errors)
        checkUniqueIds("recipe", payload.recipes.map(BackupRecipe::recipeId), errors)
        checkUniqueStrings(
            "recipe normalized title",
            payload.recipes.map { recipe ->
                recipe.normalizedTitle
                    ?.trim()
                    ?.takeIf(String::isNotBlank)
                    ?: recipe.title.trim().lowercase().replace(Regex("""\s+"""), " ")
            },
            errors
        )
        checkUniqueIds(
            "recipe ingredient",
            payload.recipeIngredients.map(BackupRecipeIngredient::ingredientId),
            errors
        )
        checkUniqueIds("price", payload.priceHistory.map(BackupPriceHistory::priceId), errors)
        checkUniqueStrings(
            "item barcode",
            payload.items.mapNotNull { it.barcode?.trim()?.takeIf(String::isNotBlank) },
            errors
        )
        checkUniqueLongs(
            "weekly budget",
            payload.weeklyBudgets.map(BackupWeeklyBudget::weekStartEpochDay),
            errors
        )

        val itemIds = payload.items.map(BackupItem::itemId).toSet()
        val sectionIds = payload.shoppingSections.map(BackupShoppingSection::sectionId).toSet()
        val mealWeekIds = payload.mealWeeks.map(BackupMealWeek::weekId).toSet()
        val recipeIds = payload.recipes.map(BackupRecipe::recipeId).toSet()
        val duplicateBarcodes = payload.items
            .mapNotNull { it.barcode?.trim()?.takeIf(String::isNotEmpty) }
            .groupingBy { it }
            .eachCount()
            .filterValues { it > 1 }
            .keys
        if (duplicateBarcodes.isNotEmpty()) {
            errors += "Duplicate item barcodes were found."
        }
        val duplicateRecipeTitles = payload.recipes
            .map { recipe ->
                recipe.normalizedTitle
                    ?.trim()
                    ?.takeIf(String::isNotEmpty)
                    ?: recipe.title.trim()
                        .lowercase(Locale.ROOT)
                        .replace(Regex("""\s+"""), " ")
            }
            .groupingBy { it }
            .eachCount()
            .filterValues { it > 1 }
            .keys
        if (duplicateRecipeTitles.isNotEmpty()) {
            errors += "Duplicate normalized recipe titles were found."
        }

        payload.items.forEachIndexed { index, item ->
            if (item.name.isBlank()) errors += "items[$index] has a blank name."
            if (item.defaultUnit.isBlank()) errors += "items[$index] has a blank default unit."
            if (item.lowStockThreshold?.let { !it.isFinite() || it < 0 } == true) {
                errors += "items[$index] has an invalid low-stock threshold."
            }
        }
        payload.inventory.forEachIndexed { index, inventory ->
            if (inventory.itemId !in itemIds) {
                errors += "inventory[$index] refers to missing item ${inventory.itemId}."
            }
            if (!inventory.quantity.isFinite() || inventory.quantity < 0) {
                errors += "inventory[$index] has an invalid quantity."
            }
            if (inventory.unit.isBlank()) errors += "inventory[$index] has a blank unit."
            if (inventory.storageLocation.isBlank()) {
                errors += "inventory[$index] has a blank storage location."
            }
        }
        payload.consumption.forEachIndexed { index, consumption ->
            if (consumption.itemId !in itemIds) {
                errors += "consumption[$index] refers to missing item ${consumption.itemId}."
            }
            if (!consumption.quantity.isFinite() || consumption.quantity <= 0) {
                errors += "consumption[$index] has an invalid quantity."
            }
            if (consumption.type !in setOf("FINISHED", "WASTED")) {
                errors += "consumption[$index] has unsupported type '${consumption.type}'."
            }
        }
        payload.shoppingSections.forEachIndexed { index, section ->
            if (section.name.isBlank()) errors += "shoppingSections[$index] has a blank name."
        }
        payload.shoppingItems.forEachIndexed { index, item ->
            if (item.name.isBlank()) errors += "shoppingItems[$index] has a blank name."
            if (!item.quantity.isFinite() || item.quantity < 0) {
                errors += "shoppingItems[$index] has an invalid quantity."
            }
            if (sectionIds.isNotEmpty() && item.sectionId !in sectionIds) {
                errors += "shoppingItems[$index] refers to missing section ${item.sectionId}."
            }
            if (item.weekId != null && mealWeekIds.isNotEmpty() && item.weekId !in mealWeekIds) {
                errors += "shoppingItems[$index] refers to missing week '${item.weekId}'."
            }
        }
        payload.meals.forEachIndexed { index, meal ->
            if (meal.name.isBlank()) errors += "meals[$index] has a blank name."
            if (mealWeekIds.isNotEmpty() && meal.weekId !in mealWeekIds) {
                errors += "meals[$index] refers to missing week '${meal.weekId}'."
            }
            if (meal.dayOfWeek !in 1..7) errors += "meals[$index] has an invalid day."
            if (meal.servings != null && meal.servings <= 0) {
                errors += "meals[$index] has invalid servings."
            }
            if (meal.recipeId != null && meal.recipeId !in recipeIds) {
                errors += "meals[$index] refers to missing recipe ${meal.recipeId}."
            }
        }
        payload.recipes.forEachIndexed { index, recipe ->
            if (recipe.title.isBlank()) errors += "recipes[$index] has a blank title."
            if (recipe.servings?.let { !it.isFinite() || it <= 0 } == true) {
                errors += "recipes[$index] has invalid servings."
            }
            if (recipe.rating != null && recipe.rating !in 1..5) {
                errors += "recipes[$index] has a rating outside 1..5."
            }
            if (
                listOf(
                    recipe.prepTimeMinutes,
                    recipe.cookTimeMinutes,
                    recipe.totalTimeMinutes
                ).any { it != null && it < 0 }
            ) {
                errors += "recipes[$index] has a negative cooking time."
            }
        }
        payload.recipeIngredients.forEachIndexed { index, ingredient ->
            if (ingredient.recipeId !in recipeIds) {
                errors += "recipeIngredients[$index] refers to missing recipe ${ingredient.recipeId}."
            }
            if (ingredient.name.isBlank()) {
                errors += "recipeIngredients[$index] has a blank name."
            }
            if (ingredient.quantity?.let { !it.isFinite() || it < 0 } == true) {
                errors += "recipeIngredients[$index] has an invalid quantity."
            }
            if (
                ingredient.linkedPantryItemId != null &&
                ingredient.linkedPantryItemId !in itemIds
            ) {
                warnings += "recipeIngredients[$index] refers to missing optional pantry item ${ingredient.linkedPantryItemId}."
            }
        }
        payload.priceHistory.forEachIndexed { index, price ->
            if (price.itemId != null && price.itemId !in itemIds) {
                warnings += "priceHistory[$index] refers to missing optional item ${price.itemId}."
            }
            if (price.displayName.isBlank() || price.normalizedItemName.isBlank()) {
                errors += "priceHistory[$index] has a blank item name."
            }
            if (price.priceMinor < 0) errors += "priceHistory[$index] has a negative price."
            if (!price.quantity.isFinite() || price.quantity <= 0) {
                errors += "priceHistory[$index] has an invalid quantity."
            }
            validateCurrency(price.currencyCode, "priceHistory[$index]", errors)
        }
        payload.weeklyBudgets.forEachIndexed { index, budget ->
            if (budget.budgetMinor < 0) errors += "weeklyBudgets[$index] has a negative target."
            validateCurrency(budget.currencyCode, "weeklyBudgets[$index]", errors)
        }
        validateCurrency(
            payload.preferences.defaultCurrencyCode,
            "preferences.defaultCurrencyCode",
            errors
        )
        if (payload.preferences.themeMode !in setOf("SYSTEM", "LIGHT", "DARK")) {
            errors += "preferences.themeMode is unsupported."
        }
        if (
            payload.preferences.activeMealWeekId != null &&
            mealWeekIds.isNotEmpty() &&
            payload.preferences.activeMealWeekId !in mealWeekIds
        ) {
            warnings += "The preferred active meal week is not present in the backup."
        }

        return BackupValidationResult(errors = errors.distinct(), warnings = warnings.distinct())
    }

    private fun checkUniqueIds(label: String, ids: List<Long>, errors: MutableList<String>) {
        if (ids.any { it <= 0 }) errors += "Every $label ID must be positive."
        checkUniqueLongs(label, ids, errors)
    }

    private fun checkUniqueLongs(label: String, values: List<Long>, errors: MutableList<String>) {
        if (values.size != values.toSet().size) errors += "Duplicate $label IDs were found."
    }

    private fun checkUniqueStrings(
        label: String,
        values: List<String>,
        errors: MutableList<String>
    ) {
        if (values.any(String::isBlank)) errors += "Every $label must be present."
        if (values.size != values.toSet().size) errors += "Duplicate $label values were found."
    }

    private fun validateCurrency(
        value: String,
        path: String,
        errors: MutableList<String>
    ) {
        if (!Regex("""^[A-Z]{3}$""").matches(value)) {
            errors += "$path must be an uppercase three-letter currency code."
        }
    }
}
