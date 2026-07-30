package com.example.pantrypal.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.example.pantrypal.data.entity.PriceHistoryEntity
import com.example.pantrypal.domain.budget.BudgetWeeklySummary
import com.example.pantrypal.domain.price.PriceItemSummary
import com.example.pantrypal.ui.components.PantryPalSpacing
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Currency
import java.util.Date
import java.util.Locale
import kotlin.math.roundToLong

data class ShoppingToolsUiState(
    val summary: BudgetWeeklySummary? = null,
    val priceSummaries: List<PriceItemSummary> = emptyList(),
    val recentPrices: List<PriceHistoryEntity> = emptyList(),
    val message: String? = null
)

@Composable
fun ShoppingToolsScreen(
    state: ShoppingToolsUiState,
    onSetBudgetMinor: (Long) -> Unit,
    onScanReceipt: () -> Unit,
    modifier: Modifier = Modifier
) {
    var budgetText by remember(state.summary?.budgetMinor) {
        mutableStateOf(
            state.summary?.budgetMinor?.let { minor ->
                "%.2f".format(Locale.ROOT, minor / 100.0)
            }.orEmpty()
        )
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = PantryPalSpacing.sm,
            end = PantryPalSpacing.sm,
            top = PantryPalSpacing.xs,
            bottom = PantryPalSpacing.xl
        ),
        verticalArrangement = Arrangement.spacedBy(PantryPalSpacing.md)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = MaterialTheme.shapes.extraLarge
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
                            Icons.Outlined.AccountBalanceWallet,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Column {
                            Text(
                                "This week's food budget",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                "Receipt imports build a simple local price history.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    state.summary?.let { summary ->
                        Text(
                            "${money(summary.spentMinor, summary.currencyCode)} spent" +
                                summary.remainingMinor?.let {
                                    " · ${money(kotlin.math.abs(it), summary.currencyCode)} " +
                                        if (it < 0) "over" else "left"
                                }.orEmpty(),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        summary.percentUsed?.let { percent ->
                            LinearProgressIndicator(
                                progress = { (percent / 100.0).toFloat().coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    OutlinedTextField(
                        value = budgetText,
                        onValueChange = { budgetText = it },
                        label = { Text("Weekly target (£)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(PantryPalSpacing.sm)
                    ) {
                        Button(
                            onClick = {
                                budgetText.replace(',', '.')
                                    .toDoubleOrNull()
                                    ?.takeIf { it.isFinite() && it >= 0.0 }
                                    ?.let {
                                        onSetBudgetMinor((it * 100).roundToLong())
                                    }
                            },
                            enabled = budgetText.replace(',', '.')
                                .toDoubleOrNull()
                                ?.let { it.isFinite() && it >= 0.0 } == true,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Save target")
                        }
                        Button(
                            onClick = onScanReceipt,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Outlined.ReceiptLong, contentDescription = null)
                            Text(" Scan receipt")
                        }
                    }
                }
            }
        }

        state.message?.let { message ->
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Text(
                        message,
                        modifier = Modifier.padding(PantryPalSpacing.md),
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        if (state.priceSummaries.isNotEmpty()) {
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(PantryPalSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.TrendingUp, contentDescription = null)
                    Text("Price changes", style = MaterialTheme.typography.titleLarge)
                }
            }
            items(
                state.priceSummaries,
                key = { "${it.normalizedItemName}-${it.unit}-${it.currencyCode}" }
            ) { price ->
                PriceSummaryCard(price)
            }
        }

        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(PantryPalSpacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.TrendingUp, contentDescription = null)
                Text("Recent prices", style = MaterialTheme.typography.titleLarge)
            }
        }
        if (state.recentPrices.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Text(
                        "Scan or paste a receipt to start comparing what you paid.",
                        modifier = Modifier.padding(PantryPalSpacing.md)
                    )
                }
            }
        } else {
            items(state.recentPrices, key = PriceHistoryEntity::priceId) { price ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(PantryPalSpacing.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(price.displayName, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${price.quantity.toReadable()} ${price.unit} · " +
                                    DateFormat.getDateInstance(DateFormat.MEDIUM)
                                        .format(Date(price.purchasedAt)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            money(price.priceMinor, price.currencyCode),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PriceSummaryCard(price: PriceItemSummary) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PantryPalSpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(price.displayName, style = MaterialTheme.typography.titleMedium)
                Text(
                    buildString {
                        append("${price.observationCount} shop")
                        if (price.observationCount != 1) append("s")
                        price.changePercent?.let { change ->
                            append(
                                " · ${
                                    "%+.1f".format(Locale.ROOT, change)
                                }% vs previous"
                            )
                        }
                        price.bestRetailer?.let { append(" · lowest at $it") }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                "${money(price.latestUnitPriceMinor, price.currencyCode)}/${price.unit}",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

private fun money(minor: Long, currencyCode: String): String = runCatching {
    NumberFormat.getCurrencyInstance().apply {
        currency = Currency.getInstance(currencyCode)
    }.format(minor / 100.0)
}.getOrElse { "%.2f %s".format(minor / 100.0, currencyCode) }

private fun Double.toReadable(): String =
    if (this % 1.0 == 0.0) toLong().toString() else toString()
