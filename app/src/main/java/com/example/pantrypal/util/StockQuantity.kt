package com.example.pantrypal.util

import com.example.pantrypal.data.entity.InventoryEntity

/** Null means remaining stock includes an incompatible measure: never pretend it is zero. */
fun stockQuantityInUnit(
    batches: List<InventoryEntity>,
    unit: String,
    nowMillis: Long = System.currentTimeMillis()
): Double? {
    var total = 0.0
    for (batch in batches) {
        if (batch.quantity <= 0 || classifyExpiry(batch.expirationDate, nowMillis) == ExpiryStatus.EXPIRED) continue
        total += convertShoppingQuantity(batch.quantity, batch.unit, unit) ?: return null
    }
    return total
}
