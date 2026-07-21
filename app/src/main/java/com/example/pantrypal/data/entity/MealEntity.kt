package com.example.pantrypal.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meals")
data class MealEntity(
    @PrimaryKey(autoGenerate = true) val mealId: Long = 0,
    val name: String,
    val week: String,
    val ingredients: List<String>,
    val dayOfWeek: Int = 1,
    val mealSlot: String = SLOT_DINNER
) {
    companion object {
        const val WEEK_A = "A"
        const val WEEK_B = "B"
        const val WEEK_C = "C"
        const val WEEK_D = "D"
        const val SLOT_BREAKFAST = "Breakfast"
        const val SLOT_LUNCH = "Lunch"
        const val SLOT_DINNER = "Dinner"
        const val SLOT_OTHER = "Other"

        val WEEKS = listOf(WEEK_A, WEEK_B, WEEK_C, WEEK_D)
        val SLOTS = listOf(SLOT_BREAKFAST, SLOT_LUNCH, SLOT_DINNER, SLOT_OTHER)
    }
}
