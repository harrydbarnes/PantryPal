package com.example.pantrypal.domain.budget

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

data class BudgetCategorySpend(
    val label: String,
    val spentMinor: Long,
    val purchaseCount: Int
)

data class BudgetTarget(
    val weekStartEpochDay: Long,
    val budgetMinor: Long,
    val currencyCode: String
)

data class BudgetPurchase(
    val purchasedAt: Long,
    val priceMinor: Long,
    val currencyCode: String,
    val retailer: String?
)

data class BudgetWeeklySummary(
    val weekStartEpochDay: Long,
    val budgetMinor: Long?,
    val spentMinor: Long,
    val remainingMinor: Long?,
    val percentUsed: Double?,
    val isOverBudget: Boolean,
    val currencyCode: String,
    val purchaseCount: Int,
    val averageItemPriceMinor: Long,
    val byRetailer: List<BudgetCategorySpend>
)

object BudgetCalculator {
    fun mondayFor(date: LocalDate): LocalDate =
        date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    fun summarize(
        weekStart: LocalDate,
        budget: BudgetTarget?,
        purchases: List<BudgetPurchase>,
        zoneId: ZoneId = ZoneId.systemDefault(),
        fallbackCurrencyCode: String = "GBP"
    ): BudgetWeeklySummary {
        val start = mondayFor(weekStart)
        val endExclusive = start.plusDays(7)
        val inWeek = purchases.filter { purchase ->
            val purchaseDate = Instant.ofEpochMilli(purchase.purchasedAt).atZone(zoneId).toLocalDate()
            !purchaseDate.isBefore(start) && purchaseDate.isBefore(endExclusive)
        }
        val currencyCode = budget?.currencyCode
            ?: inWeek.firstOrNull()?.currencyCode
            ?: fallbackCurrencyCode
        val sameCurrency = inWeek.filter { it.currencyCode == currencyCode }
        val spent = sameCurrency.sumOf { it.priceMinor }
        val budgetMinor = budget?.budgetMinor
        val remaining = budgetMinor?.minus(spent)
        val percentUsed = budgetMinor
            ?.takeIf { it > 0 }
            ?.let { target ->
                BigDecimal(spent)
                    .multiply(BigDecimal(100))
                    .divide(BigDecimal(target), 2, RoundingMode.HALF_UP)
                    .toDouble()
            }
        val byRetailer = sameCurrency
            .groupBy { it.retailer?.trim()?.takeIf(String::isNotEmpty) ?: "Unknown retailer" }
            .map { (retailer, entries) ->
                BudgetCategorySpend(
                    label = retailer,
                    spentMinor = entries.sumOf { it.priceMinor },
                    purchaseCount = entries.size
                )
            }
            .sortedWith(compareByDescending<BudgetCategorySpend> { it.spentMinor }.thenBy { it.label })

        return BudgetWeeklySummary(
            weekStartEpochDay = start.toEpochDay(),
            budgetMinor = budgetMinor,
            spentMinor = spent,
            remainingMinor = remaining,
            percentUsed = percentUsed,
            isOverBudget = remaining?.let { it < 0 } ?: false,
            currencyCode = currencyCode,
            purchaseCount = sameCurrency.size,
            averageItemPriceMinor = if (sameCurrency.isEmpty()) 0 else spent / sameCurrency.size,
            byRetailer = byRetailer
        )
    }
}
