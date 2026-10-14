package com.example.pantrypal.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Durable import receipts prevent retries, rotation and periodic polling from increasing quantities twice. */
@Entity(tableName = "shopping_ingest_state")
data class ShoppingIngestEntity(@PrimaryKey val ingestKey: String, val quantity: Double = 0.0,
    val unit: String = "", val shoppingSyncId: String? = null, val updatedAt: Long = System.currentTimeMillis())
