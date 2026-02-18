package com.example.pantrypal.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.example.pantrypal.data.entity.MealEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MealDao {
    @Insert
    suspend fun insertMeal(meal: MealEntity)

    @Delete
    suspend fun deleteMeal(meal: MealEntity)

    @Query("SELECT * FROM meals WHERE week = :week")
    fun getMealsByWeek(week: String): Flow<List<MealEntity>>

    @Query("SELECT * FROM meals")
    fun getAllMeals(): Flow<List<MealEntity>>
}
