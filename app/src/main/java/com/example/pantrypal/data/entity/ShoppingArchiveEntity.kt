package com.example.pantrypal.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "shopping_archive",
    indices = [
        Index(value = ["tripId"]),
        Index(value = ["completedAt"])
    ]
)
data class ShoppingArchiveEntity(
    @PrimaryKey(autoGenerate = true) val archiveId: Long = 0,
    val tripId: String,
    val weekId: String,
    val name: String,
    val quantity: Double,
    val unit: String,
    val sectionName: String,
    val completedAt: Long = System.currentTimeMillis(),
    val storageLocation: String? = null
)
