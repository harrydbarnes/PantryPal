package com.example.pantrypal.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shopping_list")
data class ShoppingItemEntity(
    @PrimaryKey(autoGenerate = true) val shoppingId: Long = 0,
    val name: String,
    val quantity: Double = 1.0,
    val unit: String = "pcs",
    val isChecked: Boolean = false,
    val addedAt: Long = System.currentTimeMillis()
)
