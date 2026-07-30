package com.example.pantrypal.data.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(
    tableName = "recipes",
    indices = [Index(value = ["normalizedTitle"], unique = true)]
)
data class RecipeEntity(
    @PrimaryKey(autoGenerate = true) val recipeId: Long = 0,
    val title: String,
    val normalizedTitle: String,
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
    val tags: List<String> = emptyList(),
    val rating: Int? = null,
    val isFavourite: Boolean = false,
    val lastCookedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "recipe_ingredients",
    foreignKeys = [
        ForeignKey(
            entity = RecipeEntity::class,
            parentColumns = ["recipeId"],
            childColumns = ["recipeId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ItemEntity::class,
            parentColumns = ["itemId"],
            childColumns = ["linkedPantryItemId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["recipeId"]),
        Index(value = ["linkedPantryItemId"])
    ]
)
data class RecipeIngredientEntity(
    @PrimaryKey(autoGenerate = true) val ingredientId: Long = 0,
    val recipeId: Long,
    val rawText: String,
    val name: String,
    val normalizedName: String,
    val quantity: Double? = null,
    val unit: String? = null,
    val isOptional: Boolean = false,
    val linkedPantryItemId: Long? = null,
    val sortOrder: Int = 0
)

data class RecipeWithIngredients(
    @Embedded val recipe: RecipeEntity,
    @Relation(
        parentColumn = "recipeId",
        entityColumn = "recipeId"
    )
    val ingredients: List<RecipeIngredientEntity>
)
