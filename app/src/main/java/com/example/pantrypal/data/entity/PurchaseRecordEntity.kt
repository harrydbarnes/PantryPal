package com.example.pantrypal.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/** Local purchase identity, independent of shared checklist rows. */
@Entity(tableName = "purchase_records")
data class PurchaseRecordEntity(
    @PrimaryKey val purchaseId: String = UUID.randomUUID().toString(),
    val shoppingSyncId: String? = null,
    val receiptId: String? = null,
    val name: String,
    val quantity: Double,
    val unit: String,
    val stockRecorded: Boolean,
    val completed: Boolean,
    val storageLocation: String? = null,
    val purchasedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "receipt_imports")
data class ReceiptImportEntity(@PrimaryKey val receiptId: String, val importedAt: Long)
