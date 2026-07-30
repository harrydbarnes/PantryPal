package com.example.pantrypal.data.mapper

import com.example.pantrypal.data.entity.BudgetWeeklyEntity
import com.example.pantrypal.data.entity.PriceHistoryEntity
import com.example.pantrypal.domain.budget.BudgetPurchase
import com.example.pantrypal.domain.budget.BudgetTarget

fun BudgetWeeklyEntity.toBudgetTarget(): BudgetTarget = BudgetTarget(
    weekStartEpochDay = weekStartEpochDay,
    budgetMinor = budgetMinor,
    currencyCode = currencyCode
)

fun PriceHistoryEntity.toBudgetPurchase(): BudgetPurchase = BudgetPurchase(
    purchasedAt = purchasedAt,
    priceMinor = priceMinor,
    currencyCode = currencyCode,
    retailer = retailer
)
