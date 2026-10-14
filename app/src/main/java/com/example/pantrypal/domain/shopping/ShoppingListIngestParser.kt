package com.example.pantrypal.domain.shopping

import com.example.pantrypal.util.parseQuickShoppingEntry

data class ShoppingIngestItem(val name: String, val quantity: Double = 1.0, val unit: String? = null)

/** Shared Keep text is data, not an instruction. Ticked lines never enter the shopping list. */
object ShoppingListIngestParser {
    private val ticked = Regex("^(?:[-*•]\\s*)?(?:\\[[xX✓]\\]|☑|✓|✔)\\s*.*$")
    private val bullet = Regex("^(?:(?:[-*•]|\\[\\s*\\]|☐)\\s*)+")
    private val count = Regex("^(.*?)\\s*(?:[x×]\\s*(\\d+(?:[.,]\\d+)?)|\\((\\d+(?:[.,]\\d+)?)\\))$", RegexOption.IGNORE_CASE)
    fun parseKeepSharedText(rawText: String): List<ShoppingIngestItem> {
        require(rawText.length <= 100_000) { "This shopping list is too long to import." }
        val lines = rawText.lines()
        require(lines.size <= 1000) { "Import up to 1,000 shopping items at a time." }
        return lines.mapNotNull { raw ->
            val line = raw.trim().removePrefix("\uFEFF")
            if (line.isBlank() || ticked.matches(line)) return@mapNotNull null
            val clean = line.replace(bullet, "").trim()
            if (clean.isBlank() || clean.equals("Shopping", true) || clean.equals("Shopping list", true) || clean.equals("Weekly shopping list", true)) return@mapNotNull null
            require(clean.length <= 200) { "A shopping item is too long. Shorten its name and try again." }
            val match = count.matchEntire(clean)
            if (match != null) {
                val quantity = (match.groupValues[2].ifEmpty { match.groupValues[3] }).replace(',', '.').toDoubleOrNull()
                if (quantity == null || !quantity.isFinite() || quantity <= 0) return@mapNotNull null
                val name = match.groupValues[1].trim()
                if (name.isBlank()) return@mapNotNull null
                ShoppingIngestItem(name, quantity)
            } else {
                val parsed = parseQuickShoppingEntry(clean) ?: return@mapNotNull null
                ShoppingIngestItem(parsed.name, parsed.quantity, parsed.unit.takeUnless { it == "pcs" })
            }
        }
    }
}
