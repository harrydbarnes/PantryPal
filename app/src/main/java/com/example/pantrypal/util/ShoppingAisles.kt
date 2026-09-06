package com.example.pantrypal.util

import java.util.Locale

object ShoppingAisles {
    const val ORDER_KEY = "aisle-order"
    const val UNASSIGNED = "Unassigned"
    val exampleOrder = listOf("Fruit & veg", "Bakery", "Chilled", "Cupboard", "Frozen", "Household", "Baby")
    fun assignmentKey(name: String) = "aisle:" + name.trim().lowercase(Locale.ROOT)
    fun cleanOrder(names: List<String>) = names.map(String::trim).filter { it.isNotBlank() && it != UNASSIGNED }.distinct()
    fun aisleFor(name: String, order: List<String>, values: Map<String, String>): String =
        values[assignmentKey(name)]?.takeIf { it in order } ?: UNASSIGNED
}
