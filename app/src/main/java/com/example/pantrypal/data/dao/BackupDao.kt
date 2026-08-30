package com.example.pantrypal.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.pantrypal.data.entity.BudgetWeeklyEntity
import com.example.pantrypal.data.entity.ShoppingArchiveEntity
import com.example.pantrypal.data.entity.ConsumptionEntity
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

/**
 * Complete, dependency-aware database access used only by portable backup and restore.
 *
 * Keeping this separate from the feature DAOs makes it difficult for ordinary UI code to
 * accidentally replace the user's whole kitchen.
 */
@Dao
interface BackupDao {
    @Query("SELECT * FROM items")
    suspend fun items(): List<ItemEntity>

    @Query("SELECT * FROM inventory")
    suspend fun inventory(): List<InventoryEntity>

    @Query("SELECT * FROM consumption_history")
    suspend fun consumption(): List<ConsumptionEntity>

    @Query("SELECT * FROM shopping_sections")
    suspend fun shoppingSections(): List<ShoppingSectionEntity>

    @Query("SELECT * FROM shopping_list")
    suspend fun shoppingItems(): List<ShoppingItemEntity>

    @Query("SELECT * FROM shopping_archive")
    suspend fun shoppingArchive(): List<ShoppingArchiveEntity>

    @Query("SELECT * FROM shopping_history")
    suspend fun shoppingHistory(): List<ShoppingHistoryEntity>

    @Query("SELECT * FROM meal_weeks")
    suspend fun mealWeeks(): List<MealWeekEntity>

    @Query("SELECT * FROM meals")
    suspend fun meals(): List<MealEntity>

    @Query("SELECT * FROM recipes")
    suspend fun recipes(): List<RecipeEntity>

    @Query("SELECT * FROM recipe_ingredients")
    suspend fun recipeIngredients(): List<RecipeIngredientEntity>

    @Query("SELECT * FROM price_history")
    suspend fun priceHistory(): List<PriceHistoryEntity>

    @Query("SELECT * FROM weekly_budgets")
    suspend fun weeklyBudgets(): List<BudgetWeeklyEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(values: List<ItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventory(values: List<InventoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConsumption(values: List<ConsumptionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShoppingSections(values: List<ShoppingSectionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShoppingItems(values: List<ShoppingItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShoppingArchive(values: List<ShoppingArchiveEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShoppingHistory(values: List<ShoppingHistoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealWeeks(values: List<MealWeekEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeals(values: List<MealEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipes(values: List<RecipeEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipeIngredients(values: List<RecipeIngredientEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPriceHistory(values: List<PriceHistoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeeklyBudgets(values: List<BudgetWeeklyEntity>)

    @Query("DELETE FROM recipe_ingredients")
    suspend fun clearRecipeIngredients()

    @Query("DELETE FROM inventory")
    suspend fun clearInventory()

    @Query("DELETE FROM consumption_history")
    suspend fun clearConsumption()

    @Query("DELETE FROM shopping_list")
    suspend fun clearShoppingItems()

    @Query("DELETE FROM shopping_archive")
    suspend fun clearShoppingArchive()

    @Query("DELETE FROM shopping_history")
    suspend fun clearShoppingHistory()

    @Query("DELETE FROM meals")
    suspend fun clearMeals()

    @Query("DELETE FROM price_history")
    suspend fun clearPriceHistory()

    @Query("DELETE FROM weekly_budgets")
    suspend fun clearWeeklyBudgets()

    @Query("DELETE FROM recipes")
    suspend fun clearRecipes()

    @Query("DELETE FROM items")
    suspend fun clearItems()

    @Query("DELETE FROM shopping_sections")
    suspend fun clearShoppingSections()

    @Query("DELETE FROM meal_weeks")
    suspend fun clearMealWeeks()
}
