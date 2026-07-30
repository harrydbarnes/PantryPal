package com.example.pantrypal.domain.recipe

data class Recipe(
    val id: Long = 0,
    val title: String,
    val ingredients: List<RecipeIngredient>,
    val instructions: List<String> = emptyList(),
    val source: RecipeSource? = null,
    val externalId: String? = null,
    val imageUrl: String? = null,
    val yieldText: String? = null,
    val servings: Double? = null,
    val prepTimeMinutes: Int? = null,
    val cookTimeMinutes: Int? = null,
    val totalTimeMinutes: Int? = null,
    val tags: List<String> = emptyList(),
    val rating: Int? = null,
    val isFavourite: Boolean = false,
    val lastCookedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class RecipeIngredient(
    val id: Long = 0,
    val rawText: String,
    val name: String,
    val normalizedName: String,
    val quantity: Double? = null,
    val unit: String? = null,
    val isOptional: Boolean = false,
    val linkedPantryItemId: Long? = null,
    val sortOrder: Int = 0
)

data class RecipeSource(
    val name: String,
    val url: String? = null,
    val attribution: String = name
)

data class RecipePantryIngredient(
    val itemId: Long,
    val name: String,
    val quantity: Double,
    val unit: String? = null,
    val expirationDate: Long? = null
)

data class RecipeMatch(
    val recipe: Recipe,
    val availableIngredientCount: Int,
    val missingIngredients: List<RecipeIngredient>,
    val expiringIngredients: List<RecipeIngredient>,
    val matchScore: Double
) {
    val essentialIngredientCount: Int
        get() = recipe.ingredients.count { !it.isOptional }

    val canCookNow: Boolean
        get() = essentialIngredientCount > 0 && missingIngredients.isEmpty()
}

data class RecipeIdeaShelves(
    val cookNow: List<RecipeMatch> = emptyList(),
    val useSoon: List<RecipeMatch> = emptyList(),
    val missingOneOrTwo: List<RecipeMatch> = emptyList(),
    val forgottenFavourites: List<RecipeMatch> = emptyList()
)

enum class RecipeExternalSearchMode {
    NAME,
    INGREDIENT
}
