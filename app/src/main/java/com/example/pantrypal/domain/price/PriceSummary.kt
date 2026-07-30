package com.example.pantrypal.domain.price

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

data class PriceObservation(
    val itemName: String,
    val normalizedItemName: String,
    val priceMinor: Long,
    val quantity: Double,
    val unit: String,
    val retailer: String?,
    val purchasedAt: Long,
    val currencyCode: String
)

data class PriceItemSummary(
    val normalizedItemName: String,
    val displayName: String,
    val unit: String,
    val currencyCode: String,
    val observationCount: Int,
    val latestUnitPriceMinor: Long,
    val previousUnitPriceMinor: Long?,
    val lowestUnitPriceMinor: Long,
    val highestUnitPriceMinor: Long,
    val changePercent: Double?,
    val bestRetailer: String?,
    val latestPurchasedAt: Long
)

object PriceCalculator {
    fun summarize(
        observations: List<PriceObservation>,
        currencyCode: String
    ): List<PriceItemSummary> = observations
        .asSequence()
        .filter {
            it.currencyCode == currencyCode &&
                it.normalizedItemName.isNotBlank() &&
                it.quantity.isFinite() &&
                it.quantity > 0 &&
                it.priceMinor >= 0
        }
        .groupBy { "${it.normalizedItemName}|${it.unit.lowercase(Locale.ROOT)}" }
        .values
        .map { matching ->
            val ordered = matching.sortedByDescending(PriceObservation::purchasedAt)
            val priced = ordered.map { it to unitPriceMinor(it) }
            val latest = priced.first()
            val previous = priced.getOrNull(1)
            val cheapest = priced.minWithOrNull(
                compareBy<Pair<PriceObservation, Long>> { it.second }
                    .thenByDescending { it.first.purchasedAt }
            ) ?: latest
            val previousMinor = previous?.second
            PriceItemSummary(
                normalizedItemName = latest.first.normalizedItemName,
                displayName = latest.first.itemName,
                unit = latest.first.unit,
                currencyCode = currencyCode,
                observationCount = priced.size,
                latestUnitPriceMinor = latest.second,
                previousUnitPriceMinor = previousMinor,
                lowestUnitPriceMinor = priced.minOf { it.second },
                highestUnitPriceMinor = priced.maxOf { it.second },
                changePercent = previousMinor
                    ?.takeIf { it > 0 }
                    ?.let { prior ->
                        BigDecimal(latest.second - prior)
                            .multiply(BigDecimal(100))
                            .divide(BigDecimal(prior), 2, RoundingMode.HALF_UP)
                            .toDouble()
                    },
                bestRetailer = cheapest.first.retailer,
                latestPurchasedAt = latest.first.purchasedAt
            )
        }
        .sortedWith(
            compareByDescending<PriceItemSummary> { it.latestPurchasedAt }
                .thenBy { it.displayName }
        )

    private fun unitPriceMinor(observation: PriceObservation): Long =
        BigDecimal(observation.priceMinor)
            .divide(
                BigDecimal.valueOf(observation.quantity),
                0,
                RoundingMode.HALF_UP
            )
            .longValueExact()
}
