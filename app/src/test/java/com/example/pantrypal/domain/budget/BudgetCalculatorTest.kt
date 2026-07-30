package com.example.pantrypal.domain.budget

import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BudgetCalculatorTest {
    @Test
    fun summarize_filtersToMondayWeekAndGroupsRetailers() {
        val week = LocalDate.of(2026, 7, 20)
        val result = BudgetCalculator.summarize(
            weekStart = week.plusDays(2),
            budget = BudgetTarget(week.toEpochDay(), 10_000, "GBP"),
            purchases = listOf(
                purchase(week, 2_000, "Shop B"),
                purchase(week.plusDays(3), 3_500, "Shop A"),
                purchase(week.plusDays(6), 500, "Shop A"),
                purchase(week.minusDays(1), 9_000, "Outside"),
                purchase(week.plusDays(7), 9_000, "Outside")
            ),
            zoneId = ZoneOffset.UTC
        )

        assertEquals(week.toEpochDay(), result.weekStartEpochDay)
        assertEquals(6_000L, result.spentMinor)
        assertEquals(4_000L, result.remainingMinor)
        assertEquals(60.0, result.percentUsed ?: 0.0, 0.001)
        assertEquals(3, result.purchaseCount)
        assertEquals(2_000L, result.averageItemPriceMinor)
        assertFalse(result.isOverBudget)
        assertEquals("Shop A", result.byRetailer.first().label)
        assertEquals(4_000L, result.byRetailer.first().spentMinor)
    }

    @Test
    fun summarize_reportsOverspendAndIgnoresOtherCurrencies() {
        val week = LocalDate.of(2026, 7, 20)
        val result = BudgetCalculator.summarize(
            weekStart = week,
            budget = BudgetTarget(week.toEpochDay(), 1_000, "GBP"),
            purchases = listOf(
                purchase(week, 1_200, null),
                purchase(week, 5_000, "Euro shop", "EUR")
            ),
            zoneId = ZoneOffset.UTC
        )

        assertEquals(1_200L, result.spentMinor)
        assertEquals(-200L, result.remainingMinor)
        assertEquals(120.0, result.percentUsed ?: 0.0, 0.001)
        assertTrue(result.isOverBudget)
        assertEquals("Unknown retailer", result.byRetailer.single().label)
    }

    private fun purchase(
        date: LocalDate,
        priceMinor: Long,
        retailer: String?,
        currency: String = "GBP"
    ) = BudgetPurchase(
        purchasedAt = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        priceMinor = priceMinor,
        currencyCode = currency,
        retailer = retailer
    )
}
