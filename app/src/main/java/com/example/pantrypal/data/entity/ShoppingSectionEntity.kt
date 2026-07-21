package com.example.pantrypal.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shopping_sections")
data class ShoppingSectionEntity(
    @PrimaryKey(autoGenerate = true) val sectionId: Long = 0,
    val name: String,
    val sortOrder: Int,
    val recursEveryWeek: Boolean,
    val systemKey: String? = null
) {
    companion object {
        const val ID_EVERY_WEEK = 1L
        const val ID_MEAL_PLAN = 2L
        const val ID_THE_REST = 3L
        const val ID_BABY_STUFF = 4L

        const val KEY_EVERY_WEEK = "EVERY_WEEK"
        const val KEY_MEAL_PLAN = "MEAL_PLAN"
        const val KEY_THE_REST = "THE_REST"
        const val KEY_BABY_STUFF = "BABY_STUFF"
    }
}
