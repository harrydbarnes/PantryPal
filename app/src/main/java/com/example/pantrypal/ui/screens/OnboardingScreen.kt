package com.example.pantrypal.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.pantrypal.ui.components.PantryPalSpacing

private data class OnboardingFeature(
    val icon: ImageVector,
    val title: String,
    val supportingText: String
)

private data class OnboardingPage(
    val eyebrow: String,
    val title: String,
    val supportingText: String,
    val icon: ImageVector,
    val containerRole: OnboardingContainerRole,
    val features: List<OnboardingFeature>
)

private enum class OnboardingContainerRole {
    Primary,
    Secondary,
    Tertiary
}

private val OnboardingPages = listOf(
    OnboardingPage(
        eyebrow = "Welcome to PantryPal",
        title = "Your kitchen, on speaking terms",
        supportingText = "PantryPal connects what you have, what you plan to eat, and what belongs on the next shop.",
        icon = Icons.Default.Eco,
        containerRole = OnboardingContainerRole.Primary,
        features = listOf(
            OnboardingFeature(
                Icons.Default.Inventory2,
                "Know what’s in",
                "Keep pantry quantities, dietary details, and use-by dates together."
            ),
            OnboardingFeature(
                Icons.Default.RestaurantMenu,
                "Know what’s next",
                "Reuse a four-week meal rhythm instead of planning from zero every day."
            ),
            OnboardingFeature(
                Icons.Default.ShoppingCart,
                "Buy with purpose",
                "Turn meal ingredients and weekly essentials into one organised checklist."
            )
        )
    ),
    OnboardingPage(
        eyebrow = "Stock and scan",
        title = "Build a pantry you can trust",
        supportingText = "Start small: scan a barcode or add an item manually. PantryPal gets more useful with every item you track.",
        icon = Icons.Default.QrCodeScanner,
        containerRole = OnboardingContainerRole.Secondary,
        features = listOf(
            OnboardingFeature(
                Icons.Default.AddCircle,
                "Scan in or type it",
                "Unknown barcodes open a pre-filled form so you stay in control of the details."
            ),
            OnboardingFeature(
                Icons.Default.CalendarMonth,
                "Add an optional date",
                "Use expiry dates to surface food that deserves a place in the next meal."
            ),
            OnboardingFeature(
                Icons.Default.CheckCircle,
                "Finish or waste",
                "Recording what left the pantry improves restock suggestions over time."
            )
        )
    ),
    OnboardingPage(
        eyebrow = "Plan once",
        title = "Let four good weeks do the thinking",
        supportingText = "Each week is an editable template. Name it, add meals by day, and borrow favourites from another week.",
        icon = Icons.Default.RestaurantMenu,
        containerRole = OnboardingContainerRole.Tertiary,
        features = listOf(
            OnboardingFeature(
                Icons.Default.CalendarMonth,
                "A gentle rotation",
                "The current template advances automatically each Monday."
            ),
            OnboardingFeature(
                Icons.Default.AutoAwesome,
                "Reuse the wins",
                "Copy one meal or a whole week without creating duplicate matching meals."
            ),
            OnboardingFeature(
                Icons.Default.ShoppingCart,
                "Ingredients stay intentional",
                "Saving a meal never changes your shop until you choose Build list."
            )
        )
    ),
    OnboardingPage(
        eyebrow = "Shop smarter",
        title = "One list, already sorted",
        supportingText = "Recurring essentials, meal-plan ingredients, and one-off extras each have a clear home.",
        icon = Icons.Default.ShoppingCart,
        containerRole = OnboardingContainerRole.Secondary,
        features = listOf(
            OnboardingFeature(
                Icons.Default.RestaurantMenu,
                "Build from the selected week",
                "Meal ingredients are trimmed, deduplicated, and placed in Meal plan for review."
            ),
            OnboardingFeature(
                Icons.Default.AutoAwesome,
                "Remember the regulars",
                "Every week sections keep household staples visible across the rotation."
            ),
            OnboardingFeature(
                Icons.Default.CheckCircle,
                "Tick as you shop",
                "Clear checked items when the trolley is packed and you’re ready to start fresh."
            )
        )
    ),
    OnboardingPage(
        eyebrow = "You’re ready",
        title = "A calmer kitchen starts with one item",
        supportingText = "Scan something in, give your meal weeks names you recognise, then build your first shopping list.",
        icon = Icons.Default.Eco,
        containerRole = OnboardingContainerRole.Primary,
        features = listOf(
            OnboardingFeature(
                Icons.Default.Storage,
                "Local by default",
                "Your pantry, meals, and shopping list are stored on this device."
            ),
            OnboardingFeature(
                Icons.Default.QrCodeScanner,
                "Camera only for scanning",
                "PantryPal asks for camera access when you choose Scan in or Scan out."
            ),
            OnboardingFeature(
                Icons.Default.NotificationsActive,
                "Useful expiry nudges",
                "On supported Android versions, the next prompt enables date reminders."
            )
        )
    )
)

@Composable
fun OnboardingScreen(
    isReplay: Boolean,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var pageIndex by rememberSaveable { mutableIntStateOf(0) }
    val pageCount = OnboardingPages.size
    val isLastPage = pageIndex == pageCount - 1

    BackHandler(enabled = pageIndex > 0 || isReplay) {
        if (pageIndex > 0) pageIndex -= 1 else onComplete()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(horizontal = PantryPalSpacing.sm, vertical = PantryPalSpacing.xs)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(PantryPalSpacing.xs)
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(
                        Icons.Default.Eco,
                        contentDescription = null,
                        modifier = Modifier.padding(9.dp)
                    )
                }
                Text("PantryPal", style = MaterialTheme.typography.titleLarge)
            }
            TextButton(onClick = onComplete) {
                Text(if (isReplay) "Close" else "Skip")
            }
        }

        LinearProgressIndicator(
            progress = { (pageIndex + 1).toFloat() / pageCount.toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = PantryPalSpacing.xs)
                .semantics {
                    contentDescription = "Onboarding step ${pageIndex + 1} of $pageCount"
                }
        )

        Crossfade(
            targetState = pageIndex,
            modifier = Modifier.weight(1f),
            label = "Onboarding page"
        ) { selectedPage ->
            OnboardingPageContent(
                page = OnboardingPages[selectedPage],
                stepLabel = "Step ${selectedPage + 1} of $pageCount"
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = PantryPalSpacing.xs),
            horizontalArrangement = Arrangement.spacedBy(PantryPalSpacing.xs)
        ) {
            if (pageIndex > 0) {
                OutlinedButton(
                    onClick = { pageIndex -= 1 },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null)
                    Spacer(Modifier.size(PantryPalSpacing.xs))
                    Text("Back")
                }
            }
            Button(
                onClick = {
                    if (isLastPage) onComplete() else pageIndex += 1
                },
                modifier = Modifier.weight(if (pageIndex > 0) 1f else 2f)
            ) {
                Text(if (isLastPage) "Start with the dashboard" else "Next")
                Spacer(Modifier.size(PantryPalSpacing.xs))
                Icon(
                    if (isLastPage) Icons.Default.CheckCircle else Icons.Default.ArrowForward,
                    contentDescription = null
                )
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(
    page: OnboardingPage,
    stepLabel: String
) {
    val colors = onboardingColors(page.containerRole)
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        if (maxWidth >= 700.dp) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(PantryPalSpacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OnboardingVisual(
                    page = page,
                    containerColor = colors.first,
                    contentColor = colors.second,
                    modifier = Modifier.weight(0.9f).fillMaxHeight(0.82f)
                )
                OnboardingDetails(
                    page = page,
                    stepLabel = stepLabel,
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(PantryPalSpacing.sm)
            ) {
                OnboardingVisual(
                    page = page,
                    containerColor = colors.first,
                    contentColor = colors.second,
                    modifier = Modifier.fillMaxWidth().height(196.dp)
                )
                OnboardingDetails(page = page, stepLabel = stepLabel)
            }
        }
    }
}

@Composable
private fun OnboardingVisual(
    page: OnboardingPage,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        color = containerColor,
        contentColor = contentColor
    ) {
        Box(contentAlignment = Alignment.Center) {
            Surface(
                modifier = Modifier.size(126.dp),
                shape = CircleShape,
                color = contentColor.copy(alpha = 0.12f),
                contentColor = contentColor
            ) {
                Icon(
                    page.icon,
                    contentDescription = null,
                    modifier = Modifier.padding(30.dp)
                )
            }
        }
    }
}

@Composable
private fun OnboardingDetails(
    page: OnboardingPage,
    stepLabel: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(PantryPalSpacing.sm)
    ) {
        Text(
            "${page.eyebrow} • $stepLabel".uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(page.title, style = MaterialTheme.typography.headlineMedium)
        Text(
            page.supportingText,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Column(verticalArrangement = Arrangement.spacedBy(PantryPalSpacing.xs)) {
            page.features.forEach { feature ->
                OnboardingFeatureRow(feature)
            }
        }
    }
}

@Composable
private fun OnboardingFeatureRow(feature: OnboardingFeature) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ) {
                Icon(
                    feature.icon,
                    contentDescription = null,
                    modifier = Modifier.padding(9.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(feature.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    feature.supportingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun onboardingColors(role: OnboardingContainerRole): Pair<Color, Color> = when (role) {
    OnboardingContainerRole.Primary ->
        MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
    OnboardingContainerRole.Secondary ->
        MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
    OnboardingContainerRole.Tertiary ->
        MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
}
