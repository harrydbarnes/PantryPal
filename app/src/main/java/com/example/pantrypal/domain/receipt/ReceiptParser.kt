package com.example.pantrypal.domain.receipt

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

data class ReceiptReviewCandidate(
    val candidateId: String,
    val sourceLineNumber: Int,
    val sourceText: String,
    val name: String,
    val normalizedName: String,
    val quantity: Double,
    val unit: String,
    val totalPriceMinor: Long,
    val unitPriceMinor: Long?,
    val currencyCode: String,
    val confidence: Double,
    val needsReview: Boolean,
    val isIncluded: Boolean = true
)

data class ReceiptRejectedLine(
    val lineNumber: Int,
    val text: String,
    val reason: String
)

data class ReceiptParseResult(
    val candidates: List<ReceiptReviewCandidate>,
    val rejectedLines: List<ReceiptRejectedLine>,
    val detectedReceiptTotalMinor: Long?,
    val candidateTotalMinor: Long,
    val currencyCode: String
) {
    val differenceMinor: Long?
        get() = detectedReceiptTotalMinor?.minus(candidateTotalMinor)
}

/**
 * Conservative, deterministic parsing for OCR-like supermarket text.
 *
 * The parser intentionally returns uncertain lines for human review rather than pretending OCR
 * is authoritative. Totals, payment lines, loyalty savings and tax metadata are never candidates.
 */
object ReceiptParser {
    private val trailingPrice = Regex(
        """(?i)(?:GBP|£|\$|€)?\s*(-?\d+[.,]\d{2})\s*[A-Z]?\s*$"""
    )
    private val leadingQuantity = Regex(
        """(?i)^\s*(\d+(?:[.,]\d+)?)\s*[x×]\s+(.+)$"""
    )
    private val nameAndMeasuredQuantity = Regex(
        """(?i)^(.+?)\s+(\d+(?:[.,]\d+)?)\s*(kg|g|l|ml|cl|pcs?|pack|packs|pk|each)\s*(?:@\s*(?:GBP|£|\$|€)?\s*\d+[.,]\d{2}(?:\s*/\s*[a-z]+)?)?\s*$"""
    )
    private val nameAndCountAtPrice = Regex(
        """(?i)^(.+?)\s+(\d+(?:[.,]\d+)?)\s*(?:x|@)\s*(?:GBP|£|\$|€)?\s*\d+[.,]\d{2}\s*$"""
    )
    private val totalLine = Regex("""(?i)^(?:grand\s+)?total(?:\s+to\s+pay)?\b""")
    private val nonItemLine = Regex(
        """(?i)^(?:sub\s*total|subtotal|balance|cash|card|visa|mastercard|amex|change|vat|tax|tender|payment|contactless|receipt|store|till|operator|served\s+by|date|time|items?|saving|savings|discount|coupon|clubcard|nectar)\b"""
    )
    private val dateOrTime = Regex(
        """^\s*(?:\d{1,2}[:/.-]\d{1,2}(?::|[/.-])\d{0,4}.*|\d{4}[/.-]\d{1,2}[/.-]\d{1,2}.*)\s*$"""
    )

    fun parse(
        receiptText: String,
        defaultCurrencyCode: String = "GBP"
    ): ReceiptParseResult {
        var detectedTotal: Long? = null
        var detectedCurrency = defaultCurrencyCode.uppercase(Locale.ROOT)
        val candidates = mutableListOf<ReceiptReviewCandidate>()
        val rejected = mutableListOf<ReceiptRejectedLine>()

        receiptText.lineSequence().forEachIndexed { zeroBasedIndex, rawLine ->
            val lineNumber = zeroBasedIndex + 1
            val line = rawLine
                .replace('\u00a0', ' ')
                .replace(Regex("""\s+"""), " ")
                .trim()
            if (line.isEmpty()) return@forEachIndexed

            detectedCurrency = currencyFor(line) ?: detectedCurrency
            val priceMatch = trailingPrice.find(line)
            val parsedPrice = priceMatch?.groupValues?.getOrNull(1)?.toMinorUnits()

            if (totalLine.containsMatchIn(line)) {
                if (parsedPrice != null && parsedPrice >= 0) detectedTotal = parsedPrice
                rejected += ReceiptRejectedLine(lineNumber, line, "Receipt total")
                return@forEachIndexed
            }
            if (
                nonItemLine.containsMatchIn(line) ||
                dateOrTime.matches(line) ||
                line.none(Char::isLetter)
            ) {
                rejected += ReceiptRejectedLine(lineNumber, line, "Receipt metadata")
                return@forEachIndexed
            }
            if (priceMatch == null || parsedPrice == null || parsedPrice < 0) {
                rejected += ReceiptRejectedLine(lineNumber, line, "No positive trailing price")
                return@forEachIndexed
            }

            val description = line.removeRange(priceMatch.range).trim().trimEnd('-', ':')
            val parsedDescription = parseDescription(description)
            if (parsedDescription.name.length < 2 || parsedDescription.name.none(Char::isLetter)) {
                rejected += ReceiptRejectedLine(lineNumber, line, "No product name")
                return@forEachIndexed
            }
            val cleanedName = cleanName(parsedDescription.name)
            val unitPrice = if (parsedDescription.quantity > 0) {
                BigDecimal(parsedPrice)
                    .divide(
                        BigDecimal.valueOf(parsedDescription.quantity),
                        0,
                        RoundingMode.HALF_UP
                    )
                    .longValueExact()
            } else {
                null
            }
            val confidence = when {
                parsedDescription.wasStructured -> 0.96
                cleanedName.length >= 4 -> 0.88
                else -> 0.68
            }
            candidates += ReceiptReviewCandidate(
                candidateId = "line-$lineNumber",
                sourceLineNumber = lineNumber,
                sourceText = line,
                name = cleanedName,
                normalizedName = normalizeName(cleanedName),
                quantity = parsedDescription.quantity,
                unit = parsedDescription.unit,
                totalPriceMinor = parsedPrice,
                unitPriceMinor = unitPrice,
                currencyCode = detectedCurrency,
                confidence = confidence,
                needsReview = confidence < 0.8
            )
        }

        return ReceiptParseResult(
            candidates = candidates,
            rejectedLines = rejected,
            detectedReceiptTotalMinor = detectedTotal,
            candidateTotalMinor = candidates.sumOf { it.totalPriceMinor },
            currencyCode = detectedCurrency
        )
    }

    fun normalizeName(name: String): String = name
        .trim()
        .lowercase(Locale.ROOT)
        .replace(Regex("""[^a-z0-9]+"""), " ")
        .trim()

    private fun parseDescription(description: String): ParsedDescription {
        leadingQuantity.matchEntire(description)?.let { match ->
            return ParsedDescription(
                name = match.groupValues[2],
                quantity = match.groupValues[1].toDecimalDouble(),
                unit = "pcs",
                wasStructured = true
            )
        }
        nameAndMeasuredQuantity.matchEntire(description)?.let { match ->
            return ParsedDescription(
                name = match.groupValues[1],
                quantity = match.groupValues[2].toDecimalDouble(),
                unit = normalizeUnit(match.groupValues[3]),
                wasStructured = true
            )
        }
        nameAndCountAtPrice.matchEntire(description)?.let { match ->
            return ParsedDescription(
                name = match.groupValues[1],
                quantity = match.groupValues[2].toDecimalDouble(),
                unit = "pcs",
                wasStructured = true
            )
        }
        return ParsedDescription(
            name = description,
            quantity = 1.0,
            unit = "pcs",
            wasStructured = false
        )
    }

    private fun cleanName(value: String): String {
        val withoutSku = value
            .replace(Regex("""^\d{5,}\s+"""), "")
            .replace(Regex("""\s+"""), " ")
            .trim(' ', '-', ':')
        return if (withoutSku.any(Char::isLowerCase)) {
            withoutSku
        } else {
            withoutSku
                .lowercase(Locale.ROOT)
                .replaceFirstChar { char -> char.titlecase(Locale.ROOT) }
        }
    }

    private fun normalizeUnit(unit: String): String = when (unit.lowercase(Locale.ROOT)) {
        "pc", "pcs", "each" -> "pcs"
        "pk", "pack", "packs" -> "pack"
        else -> unit.lowercase(Locale.ROOT)
    }

    private fun currencyFor(line: String): String? = when {
        "£" in line || Regex("""(?i)\bGBP\b""").containsMatchIn(line) -> "GBP"
        "€" in line -> "EUR"
        "$" in line -> "USD"
        else -> null
    }

    private fun String.toMinorUnits(): Long? = runCatching {
        replace(',', '.')
            .toBigDecimal()
            .movePointRight(2)
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact()
    }.getOrNull()

    private fun String.toDecimalDouble(): Double =
        replace(',', '.').toDoubleOrNull()?.takeIf { it > 0 } ?: 1.0

    private data class ParsedDescription(
        val name: String,
        val quantity: Double,
        val unit: String,
        val wasStructured: Boolean
    )
}
