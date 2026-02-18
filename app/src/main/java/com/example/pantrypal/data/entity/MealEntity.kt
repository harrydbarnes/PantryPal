package com.example.pantrypal.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meals")
data class MealEntity(
    @PrimaryKey(autoGenerate = true) val mealId: Long = 0,
    val name: String,
    val week: String, // "A" or "B"
    val ingredients: List<String>
) {
    companion object {
        const val WEEK_A = "A"
        const val WEEK_B = "B"
    }
}
