package com.example.pantrypal.data.entity

import com.example.pantrypal.domain.recipe.Recipe
import com.example.pantrypal.domain.recipe.RecipeIngredient
import com.example.pantrypal.domain.recipe.RecipeSource
import com.example.pantrypal.domain.recipe.normalizeRecipeTitle

fun RecipeWithIngredients.toDomain(): Recipe = recipe.toDomain(
    ingredients = ingredients
        .sortedBy(RecipeIngredientEntity::sortOrder)
        .map(RecipeIngredientEntity::toDomain)
)

fun RecipeEntity.toDomain(
    ingredients: List<RecipeIngredient> = emptyList()
): Recipe = Recipe(
    id = recipeId,
    title = title,
    ingredients = ingredients,
    instructions = instructions,
    source = sourceName?.let {
        RecipeSource(
            name = it,
            url = sourceUrl,
            attribution = attribution ?: it
        )
    },
    externalId = externalId,
    imageUrl = imageUrl,
    yieldText = yieldText,
    servings = servings,
    prepTimeMinutes = prepTimeMinutes,
    cookTimeMinutes = cookTimeMinutes,
    totalTimeMinutes = totalTimeMinutes,
    tags = tags,
    rating = rating,
    isFavourite = isFavourite,
    lastCookedAt = lastCookedAt,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun RecipeIngredientEntity.toDomain(): RecipeIngredient = RecipeIngredient(
    id = ingredientId,
    rawText = rawText,
    name = name,
    normalizedName = normalizedName,
    quantity = quantity,
    unit = unit,
    isOptional = isOptional,
    linkedPantryItemId = linkedPantryItemId,
    sortOrder = sortOrder
)

fun Recipe.toEntity(): RecipeEntity = RecipeEntity(
    recipeId = id,
    title = title.trim(),
    normalizedTitle = normalizeRecipeTitle(title),
    sourceUrl = source?.url,
    sourceName = source?.name,
    attribution = source?.attribution,
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

fun RecipeIngredient.toEntity(recipeId: Long): RecipeIngredientEntity =
    RecipeIngredientEntity(
        ingredientId = id,
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
