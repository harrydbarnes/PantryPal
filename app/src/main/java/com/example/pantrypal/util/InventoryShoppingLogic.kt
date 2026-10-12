package com.example.pantrypal.util

import com.example.pantrypal.data.dao.InventoryWithItemMap
import com.example.pantrypal.domain.recipe.RecipeIngredientNormalizer
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

const val EXPIRING_WINDOW_DAYS = 7L
private const val MILLIS_PER_DAY = 24L * 60L * 60L * 1_000L

fun expiringCutoffMillis(nowMillis: Long = System.currentTimeMillis()): Long =
    nowMillis + EXPIRING_WINDOW_DAYS * MILLIS_PER_DAY

enum class ExpiryStatus {
    EXPIRED,
    TODAY,
    DUE_SOON,
    LATER,
    NO_DATE
}

enum class InventorySort {
    NAME,
    EXPIRY,
    RECENTLY_ADDED,
    CATEGORY
}

data class InventoryFilter(
    val query: String = "",
    val category: String? = null,
    val location: String? = null,
    val expiryStatus: ExpiryStatus? = null,
    val lowStockOnly: Boolean = false,
    val openedOnly: Boolean = false,
    val sort: InventorySort = InventorySort.NAME
)

enum class ShoppingNeedStatus {
    NEED_TO_BUY,
    ALREADY_AT_HOME,
    CHECK_STOCK
}

data class ShoppingReconciliationLine(
    val name: String,
    val normalizedName: String,
    val requiredQuantity: Double,
    val availableQuantity: Double,
    val unit: String,
    val status: ShoppingNeedStatus,
    val buyQuantity: Double = requiredQuantity,
    val stockNote: String? = null
)

fun classifyExpiry(
    expirationDateMillis: Long?,
    nowMillis: Long = System.currentTimeMillis(),
    dueSoonDays: Long = EXPIRING_WINDOW_DAYS,
    zoneId: ZoneId = ZoneId.systemDefault()
): ExpiryStatus {
    if (expirationDateMillis == null) return ExpiryStatus.NO_DATE
    val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
    val expirationDate = Instant.ofEpochMilli(expirationDateMillis).atZone(zoneId).toLocalDate()
    return when {
        expirationDate.isBefore(today) -> ExpiryStatus.EXPIRED
        expirationDate == today -> ExpiryStatus.TODAY
        !expirationDate.isAfter(today.plusDays(dueSoonDays)) -> ExpiryStatus.DUE_SOON
        else -> ExpiryStatus.LATER
    }
}

fun expiryLabel(status: ExpiryStatus, expirationDateMillis: Long?, zoneId: ZoneId = ZoneId.systemDefault()): String {
    val date = expirationDateMillis?.let { Instant.ofEpochMilli(it).atZone(zoneId).toLocalDate() }
    return when (status) {
        ExpiryStatus.EXPIRED -> "Expired"
        ExpiryStatus.TODAY -> "Use today"
        ExpiryStatus.DUE_SOON -> date?.let { "Use by $it" } ?: "Due soon"
        ExpiryStatus.LATER -> date?.let { "Use by $it" } ?: "Later"
        ExpiryStatus.NO_DATE -> "No expiry date"
    }
}

fun InventoryWithItemMap.isLowStock(): Boolean =
    lowStockThreshold?.let { threshold -> quantity <= threshold } == true

fun filterAndSortInventory(
    items: List<InventoryWithItemMap>,
    filter: InventoryFilter,
    nowMillis: Long = System.currentTimeMillis()
): List<InventoryWithItemMap> {
    val query = filter.query.trim()
    val filtered = items.asSequence()
        .filter { query.isEmpty() || it.name.contains(query, ignoreCase = true) || it.category.contains(query, ignoreCase = true) }
        .filter { filter.category == null || it.category == filter.category }
        .filter { filter.location == null || it.storageLocation == filter.location }
        .filter { filter.expiryStatus == null || classifyExpiry(it.expirationDate, nowMillis) == filter.expiryStatus }
        .filter { !filter.lowStockOnly || it.isLowStock() }
        .filter { !filter.openedOnly || it.isOpened }

    return when (filter.sort) {
        InventorySort.NAME -> filtered.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }).toList()
        InventorySort.EXPIRY -> filtered.sortedWith(
            compareBy<InventoryWithItemMap> { it.expirationDate == null }
                .thenBy { it.expirationDate ?: Long.MAX_VALUE }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
        ).toList()
        InventorySort.RECENTLY_ADDED -> filtered.sortedByDescending { it.addedDate }.toList()
        InventorySort.CATEGORY -> filtered.sortedWith(
            compareBy<InventoryWithItemMap, String>(String.CASE_INSENSITIVE_ORDER) { it.category }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
        ).toList()
    }
}

fun normalizeShoppingName(value: String): String {
    val withoutTrailingCount = Regex(
        """^(.+?)\s+[x×]\s*\d+(?:[.,]\d+)?$""",
        RegexOption.IGNORE_CASE
    ).matchEntire(value.trim())?.groupValues?.get(1) ?: value
    return RecipeIngredientNormalizer.normalizeName(withoutTrailingCount)
}

fun reconcileShoppingIngredients(
    ingredients: List<String>,
    inventory: List<InventoryWithItemMap>,
    nowMillis: Long = System.currentTimeMillis()
): List<ShoppingReconciliationLine> {
    // Keep unlike measures separate: a tin is not interchangeable with grams.
    val requirements = mutableListOf<ShoppingRequirement>()
    ingredients.map(String::trim).filter(String::isNotEmpty).map(::parseShoppingRequirement).forEach { next ->
        val index = requirements.indexOfFirst {
            it.normalizedName == next.normalizedName &&
                (it.unit == next.unit || (it.unit != null && next.unit != null &&
                    convertShoppingQuantity(next.quantity, next.unit, it.unit) != null))
        }
        if (index < 0) requirements += next
        else {
            val previous = requirements[index]
            val amount = if (previous.unit != null && next.unit != null)
                convertShoppingQuantity(next.quantity, next.unit, previous.unit)!! else next.quantity
            requirements[index] = previous.copy(quantity = previous.quantity + amount)
        }
    }
    return requirements.map { ingredient ->
        val matching = inventory.filter { normalizeShoppingName(it.name) == ingredient.normalizedName }
        val batches = matching.filter { classifyExpiry(it.expirationDate, nowMillis) != ExpiryStatus.EXPIRED }
        val unit = ingredient.unit ?: "pcs"
        val available = batches.sumOf { convertShoppingQuantity(it.quantity, it.unit, unit) ?: 0.0 }
        val incompatible = batches.any { it.quantity > 0 && convertShoppingQuantity(it.quantity, it.unit, unit) == null }
        val hasUnspecifiedStock = ingredient.unit == null && batches.any { it.quantity > 0 }
        val uncertain = incompatible || hasUnspecifiedStock
        val status = when {
            uncertain -> ShoppingNeedStatus.CHECK_STOCK
            available >= ingredient.quantity -> ShoppingNeedStatus.ALREADY_AT_HOME
            else -> ShoppingNeedStatus.NEED_TO_BUY
        }
        ShoppingReconciliationLine(
            name = ingredient.displayName,
            normalizedName = ingredient.normalizedName,
            requiredQuantity = ingredient.quantity,
            availableQuantity = available,
            unit = unit,
            status = status,
            buyQuantity = if (uncertain) ingredient.quantity else (ingredient.quantity - available).coerceAtLeast(0.0),
            stockNote = when {
                uncertain -> "Confirm the amount at home; these measures may not be comparable."
                matching.any { classifyExpiry(it.expirationDate, nowMillis) == ExpiryStatus.EXPIRED } -> "Expired stock excluded."
                else -> null
            }
        )
    }
}

private data class ShoppingRequirement(
    val displayName: String,
    val normalizedName: String,
    val quantity: Double,
    val unit: String?
)

private fun parseShoppingRequirement(raw: String): ShoppingRequirement {
    val trailingCount = Regex(
        """^(.+?)\s+[x×]\s*(\d+(?:[.,]\d+)?)$""",
        RegexOption.IGNORE_CASE
    ).matchEntire(raw.trim())
    if (trailingCount != null) {
        val name = trailingCount.groupValues[1].trim()
        return ShoppingRequirement(
            displayName = name,
            normalizedName = normalizeShoppingName(name),
            quantity = trailingCount.groupValues[2].replace(',', '.').toDoubleOrNull() ?: 1.0,
            unit = "pcs"
        )
    }

    val parsed = RecipeIngredientNormalizer.parse(raw)
    return ShoppingRequirement(
        displayName = parsed.name,
        normalizedName = parsed.normalizedName.ifBlank {
            normalizeShoppingName(parsed.name)
        },
        quantity = parsed.quantity ?: 1.0,
        unit = parsed.unit?.let(::canonicalShoppingUnit) ?: if (parsed.quantity != null) "pcs" else null
    )
}

fun convertShoppingQuantity(quantity: Double, fromUnit: String, toUnit: String): Double? {
    val from = canonicalShoppingUnit(fromUnit)
    val to = canonicalShoppingUnit(toUnit)
    if (from == to) return quantity
    return when (from to to) {
        "kg" to "g" -> quantity * 1_000
        "g" to "kg" -> quantity / 1_000
        "l" to "ml" -> quantity * 1_000
        "ml" to "l" -> quantity / 1_000
        "l" to "cl" -> quantity * 100
        "cl" to "l" -> quantity / 100
        "cl" to "ml" -> quantity * 10
        "ml" to "cl" -> quantity / 10
        else -> null
    }
}

fun canonicalShoppingUnit(value: String): String = when (
    value.trim().lowercase(Locale.ROOT)
) {
    "pc", "piece", "pieces", "each" -> "pcs"
    "packs", "packet", "packets" -> "pack"
    "litre", "litres", "liter", "liters" -> "l"
    "millilitre", "millilitres", "milliliter", "milliliters" -> "ml"
    "kilogram", "kilograms" -> "kg"
    "gram", "grams" -> "g"
    else -> value.trim().lowercase(Locale.ROOT)
}

data class QuickShoppingEntry(val name: String, val quantity: Double, val unit: String)

fun parseQuickShoppingEntry(text: String): QuickShoppingEntry? {
    val requirement = parseShoppingRequirement(text.trim())
    if (requirement.displayName.isBlank() || requirement.normalizedName.isBlank() ||
        !requirement.quantity.isFinite() || requirement.quantity <= 0) return null
    return QuickShoppingEntry(requirement.displayName, requirement.quantity, requirement.unit ?: "pcs")
}
