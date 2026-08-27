package com.example.pantrypal.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.pantrypal.domain.receipt.ReceiptParseResult
import com.example.pantrypal.domain.receipt.ReceiptReviewCandidate
import com.example.pantrypal.ui.components.PantryPalSpacing
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import kotlin.math.roundToLong

/**
 * Stateless receipt confirmation surface. Camera/gallery selection and OCR are supplied by the
 * host so this screen stays usable with any future acquisition provider.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptReviewScreen(
    result: ReceiptParseResult?,
    isProcessing: Boolean,
    onCaptureReceipt: () -> Unit,
    onPasteReceiptText: () -> Unit,
    onCandidateChange: (ReceiptReviewCandidate) -> Unit,
    onImportSelected: (List<ReceiptReviewCandidate>) -> Unit,
    errorMessage: String? = null,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    showTopBar: Boolean = false
) {
    val candidates = result?.candidates.orEmpty()
    val selected = candidates.filter { it.isIncluded }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            if (showTopBar) {
                TopAppBar(
                    title = { Text("Review receipt") },
                    navigationIcon = {
                        if (onBack != null) {
                            androidx.compose.material3.TextButton(onClick = onBack) {
                                Text("Back")
                            }
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (candidates.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(PantryPalSpacing.sm),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(PantryPalSpacing.md),
                        horizontalArrangement = Arrangement.spacedBy(PantryPalSpacing.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "${selected.size} items selected",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                money(selected.sumOf { it.totalPriceMinor }, result?.currencyCode ?: "GBP"),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = { onImportSelected(selected) },
                            enabled = selected.isNotEmpty() && !isProcessing
                        ) {
                            Text("Add purchases")
                        }
                    }
                }
            }
        }
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = PantryPalSpacing.sm,
                end = PantryPalSpacing.sm,
                top = PantryPalSpacing.xs,
                bottom = 112.dp
            ),
            verticalArrangement = Arrangement.spacedBy(PantryPalSpacing.sm)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(PantryPalSpacing.md),
                        verticalArrangement = Arrangement.spacedBy(PantryPalSpacing.sm)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(PantryPalSpacing.sm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.ReceiptLong,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Column {
                                Text(
                                    "Check before adding",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    "Receipt scanning can misread names, quantities, and prices.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(PantryPalSpacing.sm)
                        ) {
                            Button(
                                onClick = onCaptureReceipt,
                                enabled = !isProcessing,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Outlined.AddAPhoto,
                                    contentDescription = null
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(if (isProcessing) "Reading…" else "Choose photo")
                            }
                            OutlinedButton(
                                onClick = onPasteReceiptText,
                                enabled = !isProcessing,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Outlined.ContentPaste,
                                    contentDescription = null
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Paste text")
                            }
                        }
                    }
                }
            }

            if (!errorMessage.isNullOrBlank()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            errorMessage,
                            modifier = Modifier.padding(PantryPalSpacing.md),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            if (result != null) {
                item {
                    ReceiptTotalsCard(result)
                }
            }

            if (result != null && candidates.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Text(
                            "No purchasable lines were found. Try a clearer image or paste the receipt text.",
                            modifier = Modifier.padding(PantryPalSpacing.md),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            items(candidates, key = ReceiptReviewCandidate::candidateId) { candidate ->
                ReceiptCandidateCard(
                    candidate = candidate,
                    onCandidateChange = onCandidateChange
                )
            }
        }
    }
}

@Composable
private fun ReceiptTotalsCard(result: ReceiptParseResult) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (result.differenceMinor == null || result.differenceMinor == 0L) {
                MaterialTheme.colorScheme.surfaceContainerLow
            } else {
                MaterialTheme.colorScheme.tertiaryContainer
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(PantryPalSpacing.md),
            verticalArrangement = Arrangement.spacedBy(PantryPalSpacing.xs)
        ) {
            SummaryRow("Parsed items", money(result.candidateTotalMinor, result.currencyCode))
            result.detectedReceiptTotalMinor?.let { total ->
                SummaryRow("Receipt total", money(total, result.currencyCode))
                HorizontalDivider()
                SummaryRow(
                    "Difference",
                    money(result.differenceMinor ?: 0L, result.currencyCode)
                )
            }
            if (result.rejectedLines.isNotEmpty()) {
                Text(
                    "${result.rejectedLines.size} non-item lines ignored",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun ReceiptCandidateCard(
    candidate: ReceiptReviewCandidate,
    onCandidateChange: (ReceiptReviewCandidate) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (candidate.needsReview) {
                MaterialTheme.colorScheme.tertiaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(PantryPalSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(PantryPalSpacing.sm)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = candidate.isIncluded,
                    onCheckedChange = {
                        onCandidateChange(candidate.copy(isIncluded = it))
                    },
                    modifier = Modifier.semantics {
                        contentDescription = "Include ${candidate.name}"
                    }
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (candidate.needsReview) "Check this line" else "Ready to add",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        candidate.sourceText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    money(candidate.totalPriceMinor, candidate.currencyCode),
                    style = MaterialTheme.typography.titleMedium
                )
            }
            OutlinedTextField(
                value = candidate.name,
                onValueChange = { value ->
                    onCandidateChange(
                        candidate.copy(
                            name = value,
                            normalizedName = com.example.pantrypal.domain.receipt.ReceiptParser
                                .normalizeName(value)
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Item name") },
                enabled = candidate.isIncluded,
                singleLine = true
            )
            Row(horizontalArrangement = Arrangement.spacedBy(PantryPalSpacing.sm)) {
                OutlinedTextField(
                    value = candidate.quantity.toEditableNumber(),
                    onValueChange = { value ->
                        value.toDoubleOrNull()?.takeIf { it > 0 }?.let { quantity ->
                            onCandidateChange(
                                candidate.copy(
                                    quantity = quantity,
                                    unitPriceMinor = candidate.totalPriceMinor
                                        .dividedBy(quantity)
                                )
                            )
                        }
                    },
                    modifier = Modifier.weight(1f),
                    label = { Text("Quantity") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    enabled = candidate.isIncluded,
                    singleLine = true
                )
                OutlinedTextField(
                    value = candidate.unit,
                    onValueChange = { onCandidateChange(candidate.copy(unit = it)) },
                    modifier = Modifier.weight(1f),
                    label = { Text("Unit") },
                    enabled = candidate.isIncluded,
                    singleLine = true
                )
            }
            OutlinedTextField(
                value = "%.2f".format(
                    Locale.ROOT,
                    candidate.totalPriceMinor / 100.0
                ),
                onValueChange = { value ->
                    value.replace(',', '.')
                        .toDoubleOrNull()
                        ?.takeIf { it.isFinite() && it >= 0.0 }
                        ?.let { total ->
                            val totalMinor = (total * 100).roundToLong()
                            onCandidateChange(
                                candidate.copy(
                                    totalPriceMinor = totalMinor,
                                    unitPriceMinor = totalMinor.dividedBy(candidate.quantity)
                                )
                            )
                        }
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Line total (${candidate.currencyCode})") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                enabled = candidate.isIncluded,
                singleLine = true
            )
        }
    }
}

private fun Double.toEditableNumber(): String =
    if (this % 1.0 == 0.0) toLong().toString() else toString()

private fun Long.dividedBy(quantity: Double): Long? =
    quantity.takeIf { it.isFinite() && it > 0.0 }
        ?.let { (this / it).roundToLong() }

private fun money(minor: Long, currencyCode: String): String = runCatching {
    NumberFormat.getCurrencyInstance().apply {
        currency = Currency.getInstance(currencyCode)
    }.format(minor / 100.0)
}.getOrElse { "%.2f %s".format(minor / 100.0, currencyCode) }
