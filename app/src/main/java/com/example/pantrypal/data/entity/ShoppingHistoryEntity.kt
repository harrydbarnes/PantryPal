package com.example.pantrypal.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shopping_history")
data class ShoppingHistoryEntity(
    @PrimaryKey val normalizedName: String,
    val displayName: String,
    val lastUsedAt: Long = System.currentTimeMillis()
)
