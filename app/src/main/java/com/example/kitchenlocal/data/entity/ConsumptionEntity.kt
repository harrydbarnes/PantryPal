package com.example.kitchenlocal.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "consumption_history",
    foreignKeys = [
        ForeignKey(
            entity = ItemEntity::class,
            parentColumns = ["itemId"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ConsumptionEntity(
    @PrimaryKey(autoGenerate = true) val eventId: Long = 0,
    val itemId: Long,
    val date: Long = System.currentTimeMillis(),
    val quantity: Double,
    val type: ConsumptionType, // "FINISHED" or "WASTED"
    val wasteReason: String? = null
)

enum class ConsumptionType {
    FINISHED,
    WASTED
}
