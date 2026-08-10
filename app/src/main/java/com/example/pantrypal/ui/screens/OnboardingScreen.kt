package com.example.pantrypal.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.pantrypal.ui.components.PantryPalSpacing
import com.example.pantrypal.util.ShoppingReminderSchedule
import com.example.pantrypal.util.ShoppingReminderTiming
import java.time.DayOfWeek

private data class SetupStep(
    val eyebrow: String,
    val title: String,
    val supportingText: String,
    val icon: ImageVector
)

private val SetupSteps = listOf(
    SetupStep(
        "Your weekly rhythm",
        "Start with the meals you already know",
        "You do not need a perfect four-week plan. Add this week's familiar meals, then reuse and refine them over time.",
        Icons.Default.RestaurantMenu
    ),
    SetupStep(
        "Your shopping routine",
        "Choose a shop day and a useful nudge",
        "Pick the timing that helps you check the list before you leave. You can fine-tune it in Settings later.",
        Icons.Default.NotificationsActive
    ),
    SetupStep(
        "Your regulars",
        "Put the essentials on the list",
        "Add only the things you buy most weeks. PantryPal will place them in your Every week section.",
        Icons.Default.ShoppingCart
    ),
    SetupStep(
        "Ready for this week",
        "Your routine is ready to grow",
        "Set up this week's meals first. Your shopping list will be there when you are ready to build it.",
        Icons.Default.Eco
    )
)

@Composable
fun OnboardingScreen(
    isReplay: Boolean,
    initialShoppingDay: Int,
    initialShoppingTimeMinutes: Int,
    initialShoppingReminderTiming: ShoppingReminderTiming,
    onSaveShoppingRoutine: (day: Int, timeMinutes: Int, timing: ShoppingReminderTiming) -> Unit,
    onSaveRegulars: (List<String>) -> Unit,
    onSaveShoppingSpot: (String) -> Unit,
    onCompleteToMealPlan: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    var stepIndex by rememberSaveable { mutableIntStateOf(0) }
    var shoppingDay by rememberSaveable { mutableIntStateOf(initialShoppingDay) }
    var shoppingTime by rememberSaveable { mutableIntStateOf(initialShoppingTimeMinutes) }
    var reminderTimingName by rememberSaveable { mutableStateOf(initialShoppingReminderTiming.name) }
    var regularsText by rememberSaveable { mutableStateOf("") }
    var storeName by rememberSaveable { mutableStateOf("") }
    var saveStoreSpot by rememberSaveable { mutableStateOf(false) }

    val step = SetupSteps[stepIndex]
    val isLastStep = stepIndex == SetupSteps.lastIndex
    val reminderTiming = ShoppingReminderTiming.fromStoredValue(reminderTimingName)

    fun finishToMealPlan() {
        onSaveShoppingRoutine(shoppingDay, shoppingTime, reminderTiming)
        val regulars = regularsText.split(',')
            .map(String::trim)
            .filter(String::isNotEmpty)
            .distinctBy(String::lowercase)
        if (regulars.isNotEmpty()) onSaveRegulars(regulars)
        if (saveStoreSpot && storeName.isNotBlank()) onSaveShoppingSpot(storeName.trim())
        onCompleteToMealPlan()
    }

    BackHandler(enabled = stepIndex > 0 || isReplay) {
        if (stepIndex > 0) stepIndex -= 1 else onSkip()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(horizontal = PantryPalSpacing.sm, vertical = PantryPalSpacing.xs)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Surface(
                modifier = Modifier.padding(end = PantryPalSpacing.xs),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Default.Eco, contentDescription = null, modifier = Modifier.padding(10.dp))
            }
            Text("PantryPal", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            TextButton(onClick = onSkip) { Text(if (isReplay) "Close" else "Skip setup") }
        }

        LinearProgressIndicator(
            progress = { (stepIndex + 1).toFloat() / SetupSteps.size },
            modifier = Modifier.fillMaxWidth().padding(vertical = PantryPalSpacing.xs).semantics {
                contentDescription = "Setup step ${stepIndex + 1} of ${SetupSteps.size}"
            }
        )

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(PantryPalSpacing.md)
        ) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(PantryPalSpacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(PantryPalSpacing.sm)
                ) {
                    Icon(step.icon, contentDescription = null)
                    Column {
                        Text("${step.eyebrow} · ${stepIndex + 1} of ${SetupSteps.size}".uppercase(), style = MaterialTheme.typography.labelMedium)
                        Text(step.title, style = MaterialTheme.typography.headlineSmall)
                    }
                }
            }
            Text(step.supportingText, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)

            when (stepIndex) {
                0 -> MealSetupPrompt()
                1 -> ShoppingRoutineSetup(
                    day = shoppingDay,
                    onDayChange = { shoppingDay = it },
                    time = shoppingTime,
                    onTimeChange = { shoppingTime = it },
                    timing = reminderTiming,
                    onTimingChange = { reminderTimingName = it.name }
                )
                2 -> RegularsAndStoreSetup(
                    regularsText = regularsText,
                    onRegularsChange = { regularsText = it },
                    storeName = storeName,
                    onStoreNameChange = { storeName = it },
                    saveStoreSpot = saveStoreSpot,
                    onSaveStoreSpotChange = { saveStoreSpot = it }
                )
                else -> SetupSummary(
                    shoppingDay = shoppingDay,
                    shoppingTime = shoppingTime,
                    timing = reminderTiming,
                    regularsCount = regularsText.split(',').count { it.isNotBlank() },
                    savingStoreSpot = saveStoreSpot && storeName.isNotBlank()
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = PantryPalSpacing.xs),
            horizontalArrangement = Arrangement.spacedBy(PantryPalSpacing.xs)
        ) {
            if (stepIndex > 0) {
                OutlinedButton(onClick = { stepIndex -= 1 }, modifier = Modifier.weight(1f)) { Text("Back") }
            }
            Button(
                onClick = { if (isLastStep) finishToMealPlan() else stepIndex += 1 },
                modifier = Modifier.weight(if (stepIndex > 0) 1f else 2f)
            ) {
                Text(if (isLastStep) "Set up this week's meals" else "Continue")
                Spacer(Modifier.width(PantryPalSpacing.xs))
                Icon(if (isLastStep) Icons.Default.CalendarMonth else Icons.Default.CheckCircle, contentDescription = null)
            }
        }
    }
}

@Composable
private fun MealSetupPrompt() {
    SetupCard("A good first plan", "Add the meals you expect to eat this week. You can copy the useful ones into later weeks when they have earned their place.")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShoppingRoutineSetup(
    day: Int,
    onDayChange: (Int) -> Unit,
    time: Int,
    onTimeChange: (Int) -> Unit,
    timing: ShoppingReminderTiming,
    onTimingChange: (ShoppingReminderTiming) -> Unit
) {
    Text("Usual shop day", style = MaterialTheme.typography.titleMedium)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DayOfWeek.entries.forEach { value ->
            FilterChip(selected = day == value.value, onClick = { onDayChange(value.value) }, label = { Text(value.name.lowercase().replaceFirstChar(Char::titlecase).take(3)) })
        }
    }
    Text("Around what time?", style = MaterialTheme.typography.titleMedium)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(10 * 60 to "Morning", 14 * 60 to "Afternoon", 18 * 60 to "Evening").forEach { (value, label) ->
            FilterChip(selected = time == value, onClick = { onTimeChange(value) }, label = { Text(label) })
        }
    }
    Text("When should we remind you?", style = MaterialTheme.typography.titleMedium)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ReminderTimingChip(ShoppingReminderTiming.NIGHT_BEFORE, "Night before", timing, onTimingChange)
        ReminderTimingChip(ShoppingReminderTiming.MORNING_OF, "Morning of the shop", timing, onTimingChange)
        ReminderTimingChip(ShoppingReminderTiming.HOUR_BEFORE, "One hour before", timing, onTimingChange)
    }
}

@Composable
private fun ReminderTimingChip(value: ShoppingReminderTiming, label: String, selected: ShoppingReminderTiming, onChange: (ShoppingReminderTiming) -> Unit) {
    FilterChip(selected = selected == value, onClick = { onChange(value) }, label = { Text(label) }, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun RegularsAndStoreSetup(
    regularsText: String,
    onRegularsChange: (String) -> Unit,
    storeName: String,
    onStoreNameChange: (String) -> Unit,
    saveStoreSpot: Boolean,
    onSaveStoreSpotChange: (Boolean) -> Unit
) {
    OutlinedTextField(value = regularsText, onValueChange = onRegularsChange, label = { Text("Regulars") }, supportingText = { Text("Separate items with commas, e.g. milk, bread, coffee") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
    Text("Where do you shop?", style = MaterialTheme.typography.titleMedium)
    OutlinedTextField(value = storeName, onValueChange = onStoreNameChange, label = { Text("Store name (optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
    FilterChip(
        selected = saveStoreSpot,
        onClick = { onSaveStoreSpotChange(!saveStoreSpot) },
        enabled = storeName.isNotBlank(),
        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
        label = { Text("Remind me when I'm near this store") },
        modifier = Modifier.fillMaxWidth()
    )
    if (saveStoreSpot) {
        SetupCard("Save the spot you are standing in", "Allow location only if you are at this store now. PantryPal uses Android geofencing, not continuous tracking.")
    }
}

@Composable
private fun SetupSummary(shoppingDay: Int, shoppingTime: Int, timing: ShoppingReminderTiming, regularsCount: Int, savingStoreSpot: Boolean) {
    val timingLabel = when (timing) {
        ShoppingReminderTiming.NIGHT_BEFORE -> "the night before"
        ShoppingReminderTiming.MORNING_OF -> "the morning of"
        ShoppingReminderTiming.HOUR_BEFORE -> "one hour before"
    }
    SetupCard("Your weekly routine", "We'll nudge you $timingLabel your ${DayOfWeek.of(shoppingDay).name.lowercase().replaceFirstChar(Char::titlecase)} shop around ${ShoppingReminderSchedule.formatShoppingTime(shoppingTime)}.")
    if (regularsCount > 0) SetupCard("Every week", "$regularsCount regular ${if (regularsCount == 1) "item" else "items"} will be added to your shopping list.")
    if (savingStoreSpot) SetupCard("Near-store reminder", "We'll ask for location so the shopping-list nudge can work when you are nearby.")
}

@Composable
private fun SetupCard(title: String, text: String) {
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(PantryPalSpacing.sm), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
