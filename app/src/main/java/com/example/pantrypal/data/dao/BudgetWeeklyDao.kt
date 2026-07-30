package com.example.pantrypal.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.pantrypal.data.entity.BudgetWeeklyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetWeeklyDao {
    @Upsert
    suspend fun upsert(budget: BudgetWeeklyEntity)

    @Query("SELECT * FROM weekly_budgets WHERE weekStartEpochDay = :weekStartEpochDay")
    fun observeForWeek(weekStartEpochDay: Long): Flow<BudgetWeeklyEntity?>

    @Query("SELECT * FROM weekly_budgets ORDER BY weekStartEpochDay DESC")
    fun observeAll(): Flow<List<BudgetWeeklyEntity>>

    @Query("SELECT * FROM weekly_budgets")
    suspend fun getAllSnapshot(): List<BudgetWeeklyEntity>
}
