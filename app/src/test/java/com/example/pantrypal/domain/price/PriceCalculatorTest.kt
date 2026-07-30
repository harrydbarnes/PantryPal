package com.example.pantrypal.domain.price

import org.junit.Assert.assertEquals
import org.junit.Test

class PriceCalculatorTest {
    @Test
    fun summarize_comparesUnitPricesAndFindsBestRetailer() {
        val summaries = PriceCalculator.summarize(
            observations = listOf(
                observation(price = 180, quantity = 2.0, retailer = "Shop A", at = 100),
                observation(price = 110, quantity = 1.0, retailer = "Shop B", at = 200),
                observation(price = 240, quantity = 2.0, retailer = "Shop C", at = 300),
                observation(price = 50, quantity = 1.0, retailer = "Euro", at = 400, currency = "EUR")
            ),
            currencyCode = "GBP"
        )

        val summary = summaries.single()
        assertEquals(3, summary.observationCount)
        assertEquals(120L, summary.latestUnitPriceMinor)
        assertEquals(110L, summary.previousUnitPriceMinor)
        assertEquals(90L, summary.lowestUnitPriceMinor)
        assertEquals(120L, summary.highestUnitPriceMinor)
        assertEquals(9.09, summary.changePercent ?: 0.0, 0.001)
        assertEquals("Shop A", summary.bestRetailer)
    }

    private fun observation(
        price: Long,
        quantity: Double,
        retailer: String,
        at: Long,
        currency: String = "GBP"
    ) = PriceObservation(
        itemName = "Milk",
        normalizedItemName = "milk",
        priceMinor = price,
        quantity = quantity,
        unit = "l",
        retailer = retailer,
        purchasedAt = at,
        currencyCode = currency
    )
}
