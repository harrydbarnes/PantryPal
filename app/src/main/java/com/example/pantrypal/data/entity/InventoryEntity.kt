package com.example.pantrypal.data.entity

import androidx.room.ColumnInfo
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
    val expirationDate: Long? = null,
    @ColumnInfo(defaultValue = "'Pantry'")
    val storageLocation: String = LOCATION_PANTRY,
    @ColumnInfo(defaultValue = "0")
    val isOpened: Boolean = false
) {
    companion object {
        const val LOCATION_PANTRY = "Pantry"
        const val LOCATION_FRIDGE = "Fridge"
        const val LOCATION_FREEZER = "Freezer"
        const val LOCATION_HOUSEHOLD = "Household"

        val STORAGE_LOCATIONS = listOf(
            LOCATION_PANTRY,
            LOCATION_FRIDGE,
            LOCATION_FREEZER,
            LOCATION_HOUSEHOLD
        )
    }
}
