package com.example.pantrypal.data.entity

import androidx.room.ColumnInfo
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
    val category: String, // e.g., "Dairy & Fridge", "Fruit & Veg"
    val isVegetarian: Boolean = false,
    val isGlutenFree: Boolean = false,
    val isUsual: Boolean = false,
    @ColumnInfo(defaultValue = "NULL")
    val lowStockThreshold: Double? = null,
    val imageUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "'EITHER'") val preferredStore: StoreTag = StoreTag.EITHER
) {
    companion object {
        const val TEMP_ID = 0L
        val CATEGORIES = listOf(
            "Dairy & Fridge", "Bakery", "Cupboard Staples", "Tinned Goods", "Household & Baby", "Uncategorised",
            "Fruit & Veg", "Fish", "Dairy", "Fresh Snacks", "Fresh Meals",
            "Snacks", "Juice & Drinks", "Baby", "Household", "Bread & Eggs", "Frozen", "General"
        )
    }
}
