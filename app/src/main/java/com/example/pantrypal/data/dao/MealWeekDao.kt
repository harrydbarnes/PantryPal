package com.example.pantrypal.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Update
import com.example.pantrypal.data.entity.MealWeekEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MealWeekDao {
    @Query("SELECT * FROM meal_weeks ORDER BY sortOrder")
    fun getAllWeeks(): Flow<List<MealWeekEntity>>

    @Update
    suspend fun updateWeek(week: MealWeekEntity)
}
