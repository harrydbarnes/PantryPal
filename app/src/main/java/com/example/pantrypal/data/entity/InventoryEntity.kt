package com.example.pantrypal.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "inventory",
    foreignKeys = [
        ForeignKey(
            entity = ItemEntity::class,
            parentColumns = ["itemId"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["itemId"])]
)
data class InventoryEntity(
    @PrimaryKey(autoGenerate = true) val inventoryId: Long = 0,
    val itemId: Long,
    val quantity: Double,
    val unit: String,
    val addedDate: Long = System.currentTimeMillis(),
    val expirationDate: Long? = null
)
