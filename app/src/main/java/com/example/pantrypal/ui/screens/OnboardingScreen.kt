package com.example.pantrypal.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.example.pantrypal.ui.components.PantryPalSpacing
import com.example.pantrypal.util.OnboardingGoal

private data class GoalOption(
    val goal: OnboardingGoal,
    val title: String,
    val description: String,
    val firstAction: String,
    val icon: ImageVector
)

private val goalOptions = listOf(
    GoalOption(OnboardingGoal.PANTRY_EXPIRY, "Track pantry and expiry", "Know what you have and what needs using first.", "Add your first pantry item", Icons.Default.Inventory2),
    GoalOption(OnboardingGoal.MEAL_PLANNING, "Plan meals", "Start with one meal for this week, not a whole rotation.", "Plan one meal", Icons.Default.RestaurantMenu),
    GoalOption(OnboardingGoal.SHOPPING_LIST, "Create a shopping list", "Build a useful list without setting up a meal plan.", "Open your shopping list", Icons.Default.ShoppingCart),
    GoalOption(OnboardingGoal.REDUCE_WASTE, "Reduce waste", "Spot food to use soon and record what you use.", "Add an item to use first", Icons.Default.Eco)
)

@Composable
fun OnboardingScreen(
    isReplay: Boolean,
    onComplete: (OnboardingGoal) -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedGoalName by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedGoal = OnboardingGoal.fromStoredValue(selectedGoalName)

    BackHandler(enabled = selectedGoal != null || isReplay) {
        if (selectedGoal != null) selectedGoalName = null else onSkip()
    }

    Column(
        modifier = modifier.fillMaxSize().systemBarsPadding()
            .padding(horizontal = PantryPalSpacing.sm, vertical = PantryPalSpacing.xs)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer) {
                Icon(Icons.Default.Eco, contentDescription = null, modifier = Modifier.padding(PantryPalSpacing.sm))
            }
            Text("PantryPal", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = PantryPalSpacing.xs).weight(1f))
            TextButton(onClick = onSkip) { Text(if (isReplay) "Close" else "Skip") }
        }

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(PantryPalSpacing.sm)
        ) {
            if (selectedGoal == null) GoalSelection(onGoalSelected = { selectedGoalName = it.name })
            else GoalFirstSuccess(goalOptions.first { it.goal == selectedGoal })
        }

        if (selectedGoal != null) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(PantryPalSpacing.xs)) {
                OutlinedButton(onClick = { selectedGoalName = null }, modifier = Modifier.weight(1f)) { Text("Choose another goal") }
                Button(onClick = { onComplete(selectedGoal) }, modifier = Modifier.weight(1f)) { Text(goalOptions.first { it.goal == selectedGoal }.firstAction) }
            }
        }
    }
}

@Composable
private fun GoalSelection(onGoalSelected: (OnboardingGoal) -> Unit) {
    Text("What would you like help with first?", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = PantryPalSpacing.md))
    Text("Choose one. You can use every PantryPal feature later.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    goalOptions.forEach { option ->
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = option.title }.clickable(role = Role.Button) { onGoalSelected(option.goal) }
        ) {
            Row(modifier = Modifier.padding(PantryPalSpacing.sm), horizontalArrangement = Arrangement.spacedBy(PantryPalSpacing.sm), verticalAlignment = Alignment.CenterVertically) {
                Icon(option.icon, contentDescription = null)
                Column {
                    Text(option.title, style = MaterialTheme.typography.titleMedium)
                    Text(option.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun GoalFirstSuccess(option: GoalOption) {
    Text("A quick first win", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = PantryPalSpacing.md))
    Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(PantryPalSpacing.md), verticalArrangement = Arrangement.spacedBy(PantryPalSpacing.xs)) {
            Icon(option.icon, contentDescription = null)
            Text(option.firstAction, style = MaterialTheme.typography.headlineSmall)
            Text(firstSuccessSupportingText(option.goal), style = MaterialTheme.typography.bodyLarge)
        }
    }
    Text("No reminder or location permission is needed for this step. Enable those only when they are useful to you in Settings.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

internal fun firstSuccessSupportingText(goal: OnboardingGoal): String = when (goal) {
    OnboardingGoal.PANTRY_EXPIRY -> "Add one item and, if you know it, its use-by date. PantryPal will keep the rest lightweight."
    OnboardingGoal.MEAL_PLANNING -> "Add just one meal to this week. The four-week rotation can wait until it earns its place."
    OnboardingGoal.SHOPPING_LIST -> "Start with the next thing you need. You can add regulars or build from meals whenever you want."
    OnboardingGoal.REDUCE_WASTE -> "Check what is already in your pantry, then add an expiry date for anything you want to use first."
}
