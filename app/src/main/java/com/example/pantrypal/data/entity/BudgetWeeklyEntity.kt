package com.example.pantrypal.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A household shopping target for a Monday-anchored ISO week.
 *
 * [weekStartEpochDay] avoids timezone ambiguity and makes a week a stable portable key.
 */
@Entity(tableName = "weekly_budgets")
data class BudgetWeeklyEntity(
    @PrimaryKey val weekStartEpochDay: Long,
    val budgetMinor: Long,
    val currencyCode: String = "GBP",
    val updatedAt: Long = System.currentTimeMillis()
)
