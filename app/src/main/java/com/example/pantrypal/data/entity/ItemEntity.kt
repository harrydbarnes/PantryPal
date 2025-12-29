package com.example.pantrypal.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "items",
    indices = [Index(value = ["barcode"], unique = true)]
)
data class ItemEntity(
    @PrimaryKey(autoGenerate = true) val itemId: Long = 0,
    val name: String,
    val barcode: String? = null,
    val defaultUnit: String, // e.g., "pcs", "L", "kg"
    val category: String, // e.g., "Dairy", "Produce"
    val isVegetarian: Boolean = false,
    val isGlutenFree: Boolean = false,
    val isUsual: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
