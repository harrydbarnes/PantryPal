package com.example.pantrypal.domain.shopping

import java.util.Locale

/** Small, deterministic UK grocery vocabulary; word boundaries avoid accidental matches. */
object AisleCategoryMapper {
    private val groups = linkedMapOf(
        "Household & Baby" to listOf("nappy", "nappies", "wipe", "wipes", "bleach", "detergent", "washing up", "toilet roll", "formula", "shampoo"),
        "Dairy & Fridge" to listOf("milk", "cheese", "yoghurt", "yogurt", "butter", "cream", "egg", "eggs", "tofu"),
        "Fruit & Veg" to listOf("courgette", "courgettes", "broccoli", "apple", "apples", "banana", "bananas", "tomato", "tomatoes", "potato", "potatoes", "carrot", "carrots", "onion", "onions", "grapes", "berries", "lettuce", "cucumber"),
        "Bakery" to listOf("bread", "wrap", "wraps", "bagel", "bagels", "crumpet", "crumpets", "croissant", "croissants"),
        "Cupboard Staples" to listOf("pasta", "rice", "tin", "tins", "tinned", "can", "cans", "bean", "beans", "lentil", "lentils", "flour", "sugar", "cereal", "tea", "coffee", "oil", "sauce")
    )
    fun categoryFor(rawName: String): String {
        val name = rawName.lowercase(Locale.UK)
        if (Regex("\\b(tin|tins|tinned|can|cans)\\b").containsMatchIn(name)) return "Cupboard Staples"
        return groups.entries.firstOrNull { (_, words) -> words.any {
            Regex("\\b" + Regex.escape(it) + "\\b").containsMatchIn(name)
        } }?.key ?: "Uncategorised"
    }
}
