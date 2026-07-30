package com.example.pantrypal.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.pantrypal.data.entity.RecipeEntity
import com.example.pantrypal.data.entity.RecipeIngredientEntity
import com.example.pantrypal.data.entity.RecipeWithIngredients
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeDao {
    @Transaction
    @Query(
        """
        SELECT * FROM recipes
        ORDER BY isFavourite DESC, rating DESC, title COLLATE NOCASE
        """
    )
    fun observeRecipes(): Flow<List<RecipeWithIngredients>>

    @Transaction
    @Query(
        """
        SELECT * FROM recipes
        WHERE title LIKE '%' || :query || '%'
           OR normalizedTitle LIKE '%' || :query || '%'
           OR sourceName LIKE '%' || :query || '%'
           OR tags LIKE '%' || :query || '%'
        ORDER BY isFavourite DESC, rating DESC, title COLLATE NOCASE
        """
    )
    fun searchRecipes(query: String): Flow<List<RecipeWithIngredients>>

    @Transaction
    @Query("SELECT * FROM recipes WHERE recipeId = :recipeId LIMIT 1")
    suspend fun getRecipe(recipeId: Long): RecipeWithIngredients?

    @Query("SELECT * FROM recipes WHERE recipeId = :recipeId LIMIT 1")
    suspend fun getRecipeEntity(recipeId: Long): RecipeEntity?

    @Query("SELECT * FROM recipes WHERE normalizedTitle = :normalizedTitle LIMIT 1")
    suspend fun getRecipeByNormalizedTitle(normalizedTitle: String): RecipeEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRecipe(recipe: RecipeEntity): Long

    @Insert
    suspend fun insertIngredients(ingredients: List<RecipeIngredientEntity>)

    @Update
    suspend fun updateRecipe(recipe: RecipeEntity)

    @Delete
    suspend fun deleteRecipe(recipe: RecipeEntity)

    @Query("DELETE FROM recipe_ingredients WHERE recipeId = :recipeId")
    suspend fun deleteIngredients(recipeId: Long)

    @Transaction
    suspend fun saveRecipe(
        recipe: RecipeEntity,
        ingredients: List<RecipeIngredientEntity>
    ): Long {
        val existing = when {
            recipe.recipeId > 0 -> getRecipeEntity(recipe.recipeId)
            else -> getRecipeByNormalizedTitle(recipe.normalizedTitle)
        }
        val recipeId = if (existing == null) {
            insertRecipe(recipe).takeIf { it > 0 }
                ?: requireNotNull(getRecipeByNormalizedTitle(recipe.normalizedTitle)).recipeId
        } else {
            updateRecipe(
                recipe.copy(
                    recipeId = existing.recipeId,
                    createdAt = existing.createdAt
                )
            )
            existing.recipeId
        }

        deleteIngredients(recipeId)
        if (ingredients.isNotEmpty()) {
            insertIngredients(
                ingredients.mapIndexed { index, ingredient ->
                    ingredient.copy(
                        ingredientId = 0,
                        recipeId = recipeId,
                        sortOrder = index
                    )
                }
            )
        }
        return recipeId
    }
}
