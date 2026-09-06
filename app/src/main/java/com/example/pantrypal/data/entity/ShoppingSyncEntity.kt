package com.example.pantrypal.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Stable keys also let layout preferences sync without coupling aisles to recurrence. */
@Entity(tableName = "shopping_layout")
data class ShoppingLayoutEntity(@PrimaryKey val layoutKey: String, val value: String)

/** Triggers write this in the same SQLite transaction as every shopping mutation. */
@Entity(tableName = "shopping_sync_queue")
data class ShoppingSyncQueueEntity(@PrimaryKey val recordKey: String, val token: String)

@Entity(tableName = "shopping_sync_batch")
data class ShoppingSyncBatchEntity(
    @PrimaryKey val id: Int = 1,
    val householdId: String,
    val batchId: String,
    val payload: String
)
