package com.example.pantrypal.util

import com.example.pantrypal.data.entity.InventoryEntity
import java.util.Locale

object ShoppingAisles {
    const val ORDER_KEY = "aisle-order"
    const val PROFILES_KEY = "shop-profiles"
    const val DEFAULT_PROFILE = "aldi"
    const val UNASSIGNED = "Unassigned"
    val exampleOrder = listOf("Fruit & veg", "Bakery", "Chilled", "Cupboard", "Frozen", "Household", "Baby")
    fun orderKey(profile: String) = if (profile == DEFAULT_PROFILE) ORDER_KEY else "$profile:aisle-order"
    fun assignmentKey(name: String, profile: String = DEFAULT_PROFILE) =
        (if (profile == DEFAULT_PROFILE) "" else "$profile:") + "aisle:" + name.trim().lowercase(Locale.ROOT)
    fun cleanOrder(names: List<String>) = names.map(String::trim).filter { it.isNotBlank() && it != UNASSIGNED }.distinct()
    fun aisleFor(name: String, order: List<String>, values: Map<String, String>, category: String? = null,
                 location: String? = null, profile: String = DEFAULT_PROFILE): String {
        val assigned = values[assignmentKey(name, profile)]
        if (assigned != null) return assigned.takeIf { it in order } ?: UNASSIGNED
        val suggested = suggestAisle(name, category, location)
        return suggested.takeIf { it in order } ?: UNASSIGNED
    }
    fun suggestAisle(name: String, category: String? = null, location: String? = null): String? {
        val categoryName = category.orEmpty().lowercase(Locale.ROOT)
        return when {
            "baby" in categoryName -> "Baby"
            location == InventoryEntity.LOCATION_HOUSEHOLD || "household" in categoryName -> "Household"
            location == InventoryEntity.LOCATION_FREEZER -> "Frozen"
            categoryName in listOf("fruit", "vegetables", "fruit & veg", "produce") -> "Fruit & veg"
            categoryName in listOf("bakery", "bread") -> "Bakery"
            location == InventoryEntity.LOCATION_FRIDGE || categoryName in listOf("dairy", "meat", "fish", "chilled") -> "Chilled"
            else -> when (normalizeShoppingName(name)) {
                "milk", "cheese", "yoghurt", "yogurt", "butter", "cream", "tofu" -> "Chilled"
                "bread", "bagel", "roll", "croissant" -> "Bakery"
                "apple", "banana", "tomato", "potato", "onion", "carrot", "broccoli", "pepper", "lettuce" -> "Fruit & veg"
                "pasta", "rice", "flour", "sugar", "tea", "coffee", "cereal" -> "Cupboard"
                else -> if (location == InventoryEntity.LOCATION_PANTRY) "Cupboard" else null
            }
        }
    }
    fun suggestStorage(name: String): String = when (suggestAisle(name)) {
        "Chilled" -> InventoryEntity.LOCATION_FRIDGE
        "Frozen" -> InventoryEntity.LOCATION_FREEZER
        "Household", "Baby" -> InventoryEntity.LOCATION_HOUSEHOLD
        else -> InventoryEntity.LOCATION_PANTRY
    }
}
