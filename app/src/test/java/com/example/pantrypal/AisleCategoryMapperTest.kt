package com.example.pantrypal

import com.example.pantrypal.domain.shopping.AisleCategoryMapper
import org.junit.Assert.*
import org.junit.Test

class AisleCategoryMapperTest {
    @Test fun mapsCommonUkGroceriesWithoutSubstringMistakes() {
        mapOf("Whole milk" to "Dairy & Fridge", "Greek yoghurt" to "Dairy & Fridge", "Courgettes" to "Fruit & Veg",
            "Apples" to "Fruit & Veg", "Wholemeal wraps" to "Bakery", "Tinned tomatoes" to "Cupboard Staples",
            "Sensitive wipes" to "Household & Baby", "Bleach" to "Household & Baby", "Olive oil" to "Cupboard Staples",
            "Breadboard" to "Uncategorised", "Notebook" to "Uncategorised").forEach { (name, category) ->
            assertEquals(name, category, AisleCategoryMapper.categoryFor(name))
        }
    }
}
