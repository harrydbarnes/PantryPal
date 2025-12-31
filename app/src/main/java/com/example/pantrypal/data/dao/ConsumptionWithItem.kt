package com.example.pantrypal.data.dao

data class ConsumptionWithItem(
    val eventId: Long,
    val itemId: Long,
    val date: Long,
    val quantity: Double,
    val type: com.example.pantrypal.data.entity.ConsumptionType,
    val wasteReason: String?,
    // Item fields
    val name: String,
    val category: String
)
