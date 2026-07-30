package com.example.pantrypal.data.mapper

import com.example.pantrypal.data.entity.PriceHistoryEntity
import com.example.pantrypal.domain.price.PriceObservation

fun PriceHistoryEntity.toPriceObservation(): PriceObservation = PriceObservation(
    itemName = displayName,
    normalizedItemName = normalizedItemName,
    priceMinor = priceMinor,
    quantity = quantity,
    unit = unit,
    retailer = retailer,
    purchasedAt = purchasedAt,
    currencyCode = currencyCode
)
