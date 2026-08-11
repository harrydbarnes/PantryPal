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
import androidx.compose.runtime.saveable.rememberSaveable
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

private data class SetupStep(val eyebrow: String, val title: String, val supportingText: String, val icon: ImageVector)

private val setupSteps = listOf(
    SetupStep("Your shopping routine", "Choose a shop day and a useful nudge", "Start with the rhythm that makes the rest of PantryPal useful. You can fine-tune it in Settings later.", Icons.Default.NotificationsActive),
    SetupStep("Your regulars", "Put the essentials on the list", "Add only the things you buy most weeks. PantryPal will place them in your Every week section.", Icons.Default.ShoppingCart),
    SetupStep("This week's meals", "Start with the meals you already know", "Add a few familiar dinners. You can move, edit and expand them in your meal plan later.", Icons.Default.RestaurantMenu),
    SetupStep("Your usual shop", "Save a place for a useful nudge", "Optional: save the shop you are standing in so PantryPal can remind you when you are nearby.", Icons.Default.LocationOn),
    SetupStep("Ready for this week", "Your routine is ready", "Your shopping rhythm, essentials and meals are in place. Build a list whenever you are ready.", Icons.Default.Eco)
)

@Composable
fun OnboardingScreen(
    isReplay: Boolean,
    initialShoppingDay: Int,
    initialShoppingTimeMinutes: Int,
    initialShoppingReminderTiming: ShoppingReminderTiming,
    onSaveShoppingRoutine: (day: Int, timeMinutes: Int, timing: ShoppingReminderTiming) -> Unit,
    onSaveRegulars: (List<String>) -> Unit,
    onSaveMeals: (List<String>) -> Unit,
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
    var firstMeal by rememberSaveable { mutableStateOf("") }
    var secondMeal by rememberSaveable { mutableStateOf("") }
    var thirdMeal by rememberSaveable { mutableStateOf("") }
    var storeName by rememberSaveable { mutableStateOf("") }
    var saveStoreSpot by rememberSaveable { mutableStateOf(false) }
    val step = setupSteps[stepIndex]
    val reminderTiming = ShoppingReminderTiming.fromStoredValue(reminderTimingName)

    fun finish() {
        onSaveShoppingRoutine(shoppingDay, shoppingTime, reminderTiming)
        regularsText.split(',').map(String::trim).filter(String::isNotEmpty).distinctBy(String::lowercase).takeIf(List<String>::isNotEmpty)?.let(onSaveRegulars)
        listOf(firstMeal, secondMeal, thirdMeal).map(String::trim).filter(String::isNotEmpty).distinctBy(String::lowercase).takeIf(List<String>::isNotEmpty)?.let(onSaveMeals)
        if (saveStoreSpot && storeName.isNotBlank()) onSaveShoppingSpot(storeName.trim())
        onCompleteToMealPlan()
    }

    BackHandler(enabled = stepIndex > 0 || isReplay) { if (stepIndex > 0) stepIndex -= 1 else onSkip() }
    Column(modifier = modifier.fillMaxSize().systemBarsPadding().padding(horizontal = PantryPalSpacing.sm, vertical = PantryPalSpacing.xs)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer) { Icon(Icons.Default.Eco, contentDescription = null, modifier = Modifier.padding(10.dp)) }
            Text("PantryPal", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = PantryPalSpacing.xs).weight(1f))
            TextButton(onClick = onSkip) { Text(if (isReplay) "Close" else "Skip setup") }
        }
        LinearProgressIndicator(progress = { (stepIndex + 1).toFloat() / setupSteps.size }, modifier = Modifier.fillMaxWidth().padding(vertical = PantryPalSpacing.xs).semantics { contentDescription = "Setup step ${stepIndex + 1} of ${setupSteps.size}" })
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(PantryPalSpacing.md)) {
            Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(PantryPalSpacing.md), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(PantryPalSpacing.sm)) {
                    Icon(step.icon, contentDescription = null)
                    Column { Text("${step.eyebrow} · ${stepIndex + 1} of ${setupSteps.size}".uppercase(), style = MaterialTheme.typography.labelMedium); Text(step.title, style = MaterialTheme.typography.headlineSmall) }
                }
            }
            Text(step.supportingText, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            when (stepIndex) {
                0 -> ShoppingRoutineSetup(shoppingDay, { shoppingDay = it }, shoppingTime, { shoppingTime = it }, reminderTiming, { reminderTimingName = it.name })
                1 -> RegularsSetup(regularsText, { regularsText = it })
                2 -> MealSetup(firstMeal, { firstMeal = it }, secondMeal, { secondMeal = it }, thirdMeal, { thirdMeal = it })
                3 -> ShoppingSpotSetup(storeName, { storeName = it }, saveStoreSpot, { saveStoreSpot = it })
                else -> SetupSummary(shoppingDay, shoppingTime, reminderTiming, regularsText.split(',').count { it.isNotBlank() }, listOf(firstMeal, secondMeal, thirdMeal).count { it.isNotBlank() }, saveStoreSpot && storeName.isNotBlank())
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(top = PantryPalSpacing.xs), horizontalArrangement = Arrangement.spacedBy(PantryPalSpacing.xs)) {
            if (stepIndex > 0) OutlinedButton(onClick = { stepIndex -= 1 }, modifier = Modifier.weight(1f)) { Text("Back") }
            Button(onClick = { if (stepIndex == setupSteps.lastIndex) finish() else stepIndex += 1 }, modifier = Modifier.weight(if (stepIndex > 0) 1f else 2f)) {
                Text(if (stepIndex == setupSteps.lastIndex) "Open this week's meal plan" else "Continue")
                Spacer(Modifier.width(PantryPalSpacing.xs)); Icon(if (stepIndex == setupSteps.lastIndex) Icons.Default.CalendarMonth else Icons.Default.CheckCircle, contentDescription = null)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShoppingRoutineSetup(day: Int, onDayChange: (Int) -> Unit, time: Int, onTimeChange: (Int) -> Unit, timing: ShoppingReminderTiming, onTimingChange: (ShoppingReminderTiming) -> Unit) {
    Text("Usual shop day", style = MaterialTheme.typography.titleMedium)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { DayOfWeek.entries.forEach { value -> FilterChip(selected = day == value.value, onClick = { onDayChange(value.value) }, label = { Text(value.name.lowercase().replaceFirstChar(Char::titlecase).take(3)) }) } }
    Text("Around what time?", style = MaterialTheme.typography.titleMedium)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf(10 * 60 to "Morning", 14 * 60 to "Afternoon", 18 * 60 to "Evening").forEach { (value, label) -> FilterChip(selected = time == value, onClick = { onTimeChange(value) }, label = { Text(label) }) } }
    Text("When should we remind you?", style = MaterialTheme.typography.titleMedium)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { ShoppingReminderTiming.entries.forEach { value -> FilterChip(selected = timing == value, onClick = { onTimingChange(value) }, label = { Text(when (value) { ShoppingReminderTiming.NIGHT_BEFORE -> "Night before"; ShoppingReminderTiming.MORNING_OF -> "Morning of the shop"; ShoppingReminderTiming.HOUR_BEFORE -> "One hour before" }) }, modifier = Modifier.fillMaxWidth()) } }
}

@Composable private fun RegularsSetup(value: String, onChange: (String) -> Unit) { OutlinedTextField(value = value, onValueChange = onChange, label = { Text("Regulars") }, supportingText = { Text("Separate items with commas, e.g. milk, bread, coffee") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }

@Composable private fun MealSetup(first: String, onFirstChange: (String) -> Unit, second: String, onSecondChange: (String) -> Unit, third: String, onThirdChange: (String) -> Unit) {
    OutlinedTextField(value = first, onValueChange = onFirstChange, label = { Text("A dinner you plan to make") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
    OutlinedTextField(value = second, onValueChange = onSecondChange, label = { Text("Another dinner (optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
    OutlinedTextField(value = third, onValueChange = onThirdChange, label = { Text("One more dinner (optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
}

@Composable private fun ShoppingSpotSetup(storeName: String, onStoreNameChange: (String) -> Unit, saveStoreSpot: Boolean, onSaveStoreSpotChange: (Boolean) -> Unit) {
    OutlinedTextField(value = storeName, onValueChange = onStoreNameChange, label = { Text("Store name (optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
    FilterChip(selected = saveStoreSpot, onClick = { onSaveStoreSpotChange(!saveStoreSpot) }, enabled = storeName.isNotBlank(), leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) }, label = { Text("Remind me when I'm near this store") }, modifier = Modifier.fillMaxWidth())
    if (saveStoreSpot) SetupCard("Save the spot you are standing in", "Allow location only if you are at this store now. PantryPal uses Android geofencing, not continuous tracking.")
}

@Composable private fun SetupSummary(shoppingDay: Int, shoppingTime: Int, timing: ShoppingReminderTiming, regularsCount: Int, mealCount: Int, savingStoreSpot: Boolean) {
    val timingLabel = when (timing) { ShoppingReminderTiming.NIGHT_BEFORE -> "the night before"; ShoppingReminderTiming.MORNING_OF -> "the morning of"; ShoppingReminderTiming.HOUR_BEFORE -> "one hour before" }
    SetupCard("Your weekly routine", "We'll nudge you $timingLabel your ${DayOfWeek.of(shoppingDay).name.lowercase().replaceFirstChar(Char::titlecase)} shop around ${ShoppingReminderSchedule.formatShoppingTime(shoppingTime)}.")
    if (regularsCount > 0) SetupCard("Every week", "$regularsCount regular ${if (regularsCount == 1) "item" else "items"} will be added to your shopping list.")
    if (mealCount > 0) SetupCard("This week's dinners", "$mealCount ${if (mealCount == 1) "meal is" else "meals are"} ready in your meal plan.")
    if (savingStoreSpot) SetupCard("Near-store reminder", "We'll ask for location so the shopping-list nudge can work when you are nearby.")
}

@Composable private fun SetupCard(title: String, text: String) { Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth()) { Column(modifier = Modifier.padding(PantryPalSpacing.sm), verticalArrangement = Arrangement.spacedBy(4.dp)) { Text(title, style = MaterialTheme.typography.titleMedium); Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
