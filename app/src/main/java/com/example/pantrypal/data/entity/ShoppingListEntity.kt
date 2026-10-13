package com.example.pantrypal.data.entity

import androidx.room.*
import androidx.room.PrimaryKey

@Entity(tableName = "shopping_list", foreignKeys = [ForeignKey(
    entity = InventoryEntity::class, parentColumns = ["inventoryId"], childColumns = ["inventoryId"],
    onDelete = ForeignKey.SET_NULL
)], indices = [Index("inventoryId"), Index("name")])
data class ShoppingListEntity(
    @PrimaryKey(autoGenerate = true) val shoppingId: Long = 0,
    val name: String,
    val quantity: Double = 1.0,
    val unit: String = "pcs",
    val isChecked: Boolean = false,
    val addedAt: Long = System.currentTimeMillis(),
    val frequency: String = FREQ_ONE_OFF,
    val sectionId: Long = ShoppingSectionEntity.ID_THE_REST,
    val weekId: String? = null,
    @androidx.room.ColumnInfo(defaultValue = "''") val syncId: String = java.util.UUID.randomUUID().toString(),
    @ColumnInfo(defaultValue = "NULL") val inventoryId: Long? = null,
    @ColumnInfo(defaultValue = "'Uncategorised'") val category: String = "Uncategorised",
    @ColumnInfo(defaultValue = "'EITHER'") val storeTag: StoreTag = StoreTag.EITHER,
    @ColumnInfo(defaultValue = "'MANUAL'") val source: String = "MANUAL"
) {
    @get:Ignore val id: Long get() = shoppingId

    companion object {
        const val FREQ_ONE_OFF = "One-Off"
        const val FREQ_ESSENTIAL = "Essential"
        const val FREQ_WEEK_A = "Week A"
        const val FREQ_WEEK_B = "Week B"
    }
}
