package com.example.pantrypal.domain.recipe

import java.text.Normalizer
import java.util.Locale
import kotlin.math.abs

object RecipeIngredientNormalizer {
    private val quantityPattern = Regex(
        """^((?:\d+\s+\d+/\d+)|(?:\d+/\d+)|(?:\d+(?:[.,]\d+)?))\s*"""
    )
    private val unitPattern = Regex(
        """^(tablespoons?|tbsp|teaspoons?|tsp|kilograms?|kg|grams?|g|millilit(?:er|re)s?|ml|lit(?:er|re)s?|l|cups?|ounces?|oz|pounds?|lbs?|cloves?|cans?|tins?|packs?|packets?|pieces?|pcs?|bunch(?:es)?|handfuls?|pinch(?:es)?|slices?)\b\s*""",
        RegexOption.IGNORE_CASE
    )
    private val optionalPattern = Regex(
        """(?:\(\s*optional\s*\)|,\s*optional\b|\boptional\s*$)""",
        RegexOption.IGNORE_CASE
    )
    private val preparationWords = setOf(
        "a", "an", "of", "fresh", "large", "medium", "small", "finely", "roughly",
        "chopped", "diced", "sliced", "grated", "minced", "crushed", "peeled",
        "drained", "rinsed", "cooked", "frozen", "optional", "to", "taste"
    )
    private val unitAliases = mapOf(
        "tablespoon" to "tbsp",
        "tablespoons" to "tbsp",
        "teaspoon" to "tsp",
        "teaspoons" to "tsp",
        "kilogram" to "kg",
        "kilograms" to "kg",
        "gram" to "g",
        "grams" to "g",
        "milliliter" to "ml",
        "milliliters" to "ml",
        "millilitre" to "ml",
        "millilitres" to "ml",
        "liter" to "l",
        "liters" to "l",
        "litre" to "l",
        "litres" to "l",
        "ounce" to "oz",
        "ounces" to "oz",
        "pound" to "lb",
        "pounds" to "lb",
        "lbs" to "lb",
        "cups" to "cup",
        "cloves" to "clove",
        "cans" to "can",
        "tins" to "tin",
        "packs" to "pack",
        "packets" to "packet",
        "pieces" to "piece",
        "pcs" to "piece",
        "bunches" to "bunch",
        "handfuls" to "handful",
        "pinches" to "pinch",
        "slices" to "slice"
    )

    fun parse(rawText: String, sortOrder: Int = 0): RecipeIngredient {
        val trimmed = replaceUnicodeFractions(rawText).trim()
        val isOptional = optionalPattern.containsMatchIn(trimmed)
        var remainder = trimmed.replace(optionalPattern, "").trim().trim(',', ';')

        val quantityMatch = quantityPattern.find(remainder)
        val quantity = quantityMatch?.groupValues?.getOrNull(1)?.let(::parseQuantity)
        if (quantityMatch != null) remainder = remainder.removeRange(quantityMatch.range).trim()

        val unitMatch = unitPattern.find(remainder)
        val unit = unitMatch?.value?.trim()?.lowercase(Locale.ROOT)?.let(::canonicalUnit)
        if (unitMatch != null) remainder = remainder.removeRange(unitMatch.range).trim()

        remainder = remainder
            .removePrefix("of ")
            .substringBefore(',')
            .trim()
            .ifBlank { trimmed }

        return RecipeIngredient(
            rawText = rawText.trim(),
            name = remainder,
            normalizedName = normalizeName(remainder),
            quantity = quantity,
            unit = unit,
            isOptional = isOptional,
            sortOrder = sortOrder
        )
    }

    fun normalizeName(value: String): String {
        val parsedValue = stripQuantityAndUnit(replaceUnicodeFractions(value))
        val ascii = Normalizer.normalize(parsedValue, Normalizer.Form.NFD)
            .replace(Regex("""\p{Mn}+"""), "")
            .lowercase(Locale.ROOT)
            .replace("&", " and ")
            .replace(Regex("""\([^)]*\)"""), " ")
            .replace(Regex("""[^a-z0-9\s-]"""), " ")
            .replace('-', ' ')

        return ascii
            .split(Regex("""\s+"""))
            .filter { it.isNotBlank() && it !in preparationWords }
            .map(::singularize)
            .joinToString(" ")
            .trim()
    }

    private fun stripQuantityAndUnit(value: String): String {
        var result = value.trim()
        quantityPattern.find(result)?.let { result = result.removeRange(it.range).trim() }
        unitPattern.find(result)?.let { result = result.removeRange(it.range).trim() }
        return result.removePrefix("of ").trim()
    }

    private fun parseQuantity(value: String): Double? {
        val normalized = value.replace(',', '.').trim()
        return when {
            " " in normalized -> {
                val parts = normalized.split(Regex("""\s+"""), limit = 2)
                parts.firstOrNull()?.toDoubleOrNull()?.plus(parseFraction(parts.getOrNull(1)))
            }
            "/" in normalized -> parseFraction(normalized)
            else -> normalized.toDoubleOrNull()
        }
    }

    private fun parseFraction(value: String?): Double {
        val parts = value.orEmpty().split('/', limit = 2)
        val numerator = parts.getOrNull(0)?.toDoubleOrNull() ?: return 0.0
        val denominator = parts.getOrNull(1)?.toDoubleOrNull() ?: return 0.0
        return if (abs(denominator) < 0.000001) 0.0 else numerator / denominator
    }

    private fun replaceUnicodeFractions(value: String): String = value
        .replace("¼", " 1/4")
        .replace("½", " 1/2")
        .replace("¾", " 3/4")
        .replace("⅓", " 1/3")
        .replace("⅔", " 2/3")
        .replace(Regex("""(?<=\d)\s+(?=\d/\d)"""), " ")
        .trim()

    private fun canonicalUnit(value: String): String {
        val clean = value.lowercase(Locale.ROOT).trim()
        return unitAliases[clean] ?: clean
    }

    private fun singularize(token: String): String = when {
        token == "tomatoes" -> "tomato"
        token == "potatoes" -> "potato"
        token.endsWith("berries") -> token.removeSuffix("ies") + "y"
        token.endsWith("ies") && token.length > 4 -> token.dropLast(3) + "y"
        token.endsWith("ses") || token.endsWith("ss") || token.endsWith("us") -> token
        token.endsWith("s") && token.length > 3 -> token.dropLast(1)
        else -> token
    }
}

fun normalizeRecipeTitle(value: String): String = Normalizer
    .normalize(value, Normalizer.Form.NFD)
    .replace(Regex("""\p{Mn}+"""), "")
    .lowercase(Locale.ROOT)
    .replace("&", " and ")
    .replace(Regex("""[^a-z0-9\s]"""), " ")
    .replace(Regex("""\s+"""), " ")
    .trim()
