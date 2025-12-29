package com.example.pantrypal

import com.example.pantrypal.data.converter.Converters
import com.example.pantrypal.data.entity.ConsumptionType
import org.junit.Assert.assertEquals
import org.junit.Test

class ConvertersTest {

    private val converters = Converters()

    @Test
    fun fromConsumptionType() {
        val type = ConsumptionType.WASTED
        val result = converters.fromConsumptionType(type)
        assertEquals("WASTED", result)
    }

    @Test
    fun toConsumptionType() {
        val typeString = "FINISHED"
        val result = converters.toConsumptionType(typeString)
        assertEquals(ConsumptionType.FINISHED, result)
    }

    @Test
    fun toConsumptionType_Invalid() {
        val typeString = "UNKNOWN"
        val result = converters.toConsumptionType(typeString)
        assertEquals(ConsumptionType.FINISHED, result) // Fallback
    }
}
