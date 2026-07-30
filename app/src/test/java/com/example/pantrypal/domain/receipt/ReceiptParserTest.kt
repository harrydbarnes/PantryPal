package com.example.pantrypal.domain.receipt

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceiptParserTest {
    @Test
    fun parse_extractsCountWeightAndPlainProductsWhileIgnoringTotals() {
        val result = ReceiptParser.parse(
            """
            MY SUPERMARKET
            2 x BANANAS £1.50
            WHOLE MILK £2.40
            APPLES 0.735 kg @ £2.20/kg £1.62
            SUBTOTAL £5.52
            TOTAL TO PAY £5.52
            VISA £5.52
            """.trimIndent()
        )

        assertEquals(3, result.candidates.size)
        assertEquals(552L, result.candidateTotalMinor)
        assertEquals(552L, result.detectedReceiptTotalMinor)
        assertEquals(0L, result.differenceMinor)
        assertEquals("GBP", result.currencyCode)

        val bananas = result.candidates[0]
        assertEquals("Bananas", bananas.name)
        assertEquals(2.0, bananas.quantity, 0.0)
        assertEquals("pcs", bananas.unit)
        assertEquals(150L, bananas.totalPriceMinor)
        assertEquals(75L, bananas.unitPriceMinor)

        val apples = result.candidates[2]
        assertEquals(0.735, apples.quantity, 0.0)
        assertEquals("kg", apples.unit)
        assertFalse(apples.needsReview)
        assertTrue(result.rejectedLines.any { it.reason == "Receipt total" })
    }

    @Test
    fun parse_acceptsDecimalCommasAndFlagsShortUnstructuredNames() {
        val result = ReceiptParser.parse(
            """
            AB 1,20
            3 x YOGHURT 2,10
            TOTAL 3,30
            """.trimIndent()
        )

        assertEquals(2, result.candidates.size)
        assertEquals(120L, result.candidates[0].totalPriceMinor)
        assertTrue(result.candidates[0].needsReview)
        assertEquals(3.0, result.candidates[1].quantity, 0.0)
        assertEquals("yoghurt", result.candidates[1].normalizedName)
    }

    @Test
    fun parse_rejectsNegativeDiscountAndMetadataLines() {
        val result = ReceiptParser.parse(
            """
            CLUBCARD DISCOUNT -0.50
            26/07/2026 12:34
            CHANGE £1.20
            """.trimIndent()
        )

        assertTrue(result.candidates.isEmpty())
        assertEquals(3, result.rejectedLines.size)
    }
}
