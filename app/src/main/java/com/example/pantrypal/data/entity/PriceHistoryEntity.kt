package com.example.pantrypal.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * An observed purchase price. The item reference is deliberately optional so receipt imports
 * can retain useful history before a candidate has been matched to a pantry item.
 */
@Entity(
    tableName = "price_history",
    indices = [
        Index(value = ["normalizedItemName"]),
        Index(value = ["purchasedAt"]),
        Index(value = ["retailer"])
    ]
)
data class PriceHistoryEntity(
    @PrimaryKey(autoGenerate = true) val priceId: Long = 0,
    val itemId: Long? = null,
    val normalizedItemName: String,
    val displayName: String,
    val priceMinor: Long,
    val quantity: Double = 1.0,
    val unit: String = "pcs",
    val retailer: String? = null,
    val purchasedAt: Long = System.currentTimeMillis(),
    val currencyCode: String = "GBP",
    val source: String = SOURCE_MANUAL
) {
    companion object {
        const val SOURCE_MANUAL = "MANUAL"
        const val SOURCE_RECEIPT = "RECEIPT"
        const val SOURCE_IMPORT = "IMPORT"
    }
}
