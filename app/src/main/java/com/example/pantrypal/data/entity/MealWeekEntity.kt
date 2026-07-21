package com.example.pantrypal.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meal_weeks")
data class MealWeekEntity(
    @PrimaryKey val weekId: String,
    val name: String,
    val emoji: String,
    val sortOrder: Int
) {
    val displayName: String
        get() = listOf(emoji.trim(), name.trim()).filter { it.isNotEmpty() }.joinToString(" ")

    companion object {
        val DEFAULT_IDS = listOf("A", "B", "C", "D")
    }
}
