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
import androidx.compose.foundation.layout.imePadding
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
import java.time.LocalDate
import java.time.ZoneOffset

private data class SetupStep(val eyebrow: String, val title: String, val supportingText: String, val icon: ImageVector)

private val setupSteps = listOf(
    SetupStep("Your shopping routine", "Choose a shop day and an optional nudge", "Start with the rhythm that makes the rest of PantryPal useful. You can fine-tune it in Settings later.", Icons.Default.NotificationsActive),
    SetupStep("Your regulars", "Put the essentials on the list", "Add only the things you buy most weeks. PantryPal will place them in your Every week section.", Icons.Default.ShoppingCart),
    SetupStep("This week's meals", "Start with the meals you already know", "Add ingredients with amounts so your first dinner can produce a useful shopping list. Leave unused dinners blank.", Icons.Default.RestaurantMenu),
    SetupStep("Your cupboard", "Start with something already at home", "Optional: add one item and its expiry date. Shopping review will account for it. Add the rest by scanning or typing in Pantry.", Icons.Default.Eco),
    SetupStep("Ready for this week", "Your routine is ready", "Save your choices, then review ingredients in this week’s meal plan. Nothing is saved until you finish.", Icons.Default.Eco)
)

@Composable
fun OnboardingScreen(
    isReplay: Boolean,
    initialShoppingDay: Int,
    initialShoppingTimeMinutes: Int,
    initialShoppingReminderTiming: ShoppingReminderTiming,
    onSaveShoppingRoutine: (day: Int, timeMinutes: Int, timing: ShoppingReminderTiming, remindersEnabled: Boolean) -> Unit,
    onSaveKitchen: (List<String>, List<OnboardingMeal>, OnboardingStock?, () -> Unit) -> Unit,
    initialShoppingRemindersEnabled: Boolean = false,
    saving: Boolean = false,
    error: String? = null,
    onCompleteToMealPlan: (requestNotificationPermission: Boolean) -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
    householdContent: (@Composable () -> Unit)? = null
) {
    var joiningHousehold by rememberSaveable { mutableStateOf(false) }
    var stepIndex by rememberSaveable { mutableIntStateOf(0) }
    var shoppingDay by rememberSaveable { mutableIntStateOf(initialShoppingDay) }
    var shoppingTime by rememberSaveable { mutableIntStateOf(initialShoppingTimeMinutes) }
    var reminderTimingName by rememberSaveable { mutableStateOf(initialShoppingReminderTiming.name) }
    var shoppingRemindersEnabled by rememberSaveable { mutableStateOf(initialShoppingRemindersEnabled) }
    var regularsText by rememberSaveable { mutableStateOf("") }
    var firstMeal by rememberSaveable { mutableStateOf("") }
    var secondMeal by rememberSaveable { mutableStateOf("") }
    var thirdMeal by rememberSaveable { mutableStateOf("") }
    var firstMealDay by rememberSaveable { mutableIntStateOf(DayOfWeek.MONDAY.value) }
    var secondMealDay by rememberSaveable { mutableIntStateOf(DayOfWeek.WEDNESDAY.value) }
    var thirdMealDay by rememberSaveable { mutableIntStateOf(DayOfWeek.FRIDAY.value) }
    var firstIngredients by rememberSaveable { mutableStateOf("") }
    var secondIngredients by rememberSaveable { mutableStateOf("") }
    var thirdIngredients by rememberSaveable { mutableStateOf("") }
    var stockName by rememberSaveable { mutableStateOf("") }
    var stockAmount by rememberSaveable { mutableStateOf("1") }
    var stockUnit by rememberSaveable { mutableStateOf("pcs") }
    var stockExpiry by rememberSaveable { mutableStateOf("") }
    val expiry = runCatching { LocalDate.parse(stockExpiry).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() }.getOrNull()
    val stockValid = stockName.isBlank() || (stockAmount.toDoubleOrNull()?.let { it.isFinite() && it > 0 } == true && stockUnit.isNotBlank() && (stockExpiry.isBlank() || expiry != null))
    val mealsValid = listOf(firstMeal to firstIngredients, secondMeal to secondIngredients, thirdMeal to thirdIngredients).all { (name, ingredients) -> name.isBlank() || onboardingRegulars(ingredients).isNotEmpty() }
    fun meals() = onboardingMeals(listOf(OnboardingMeal(firstMeal, firstMealDay, onboardingRegulars(firstIngredients)), OnboardingMeal(secondMeal, secondMealDay, onboardingRegulars(secondIngredients)), OnboardingMeal(thirdMeal, thirdMealDay, onboardingRegulars(thirdIngredients))))
    val step = setupSteps[stepIndex]
    val reminderTiming = ShoppingReminderTiming.fromStoredValue(reminderTimingName)

    fun finish() {
        onSaveKitchen(onboardingRegulars(regularsText), meals(),
            stockName.trim().takeIf { it.isNotEmpty() }?.let { OnboardingStock(it, stockAmount.toDouble(), stockUnit.trim(), expiry) }) {
            onSaveShoppingRoutine(shoppingDay, shoppingTime, reminderTiming, shoppingRemindersEnabled)
            onCompleteToMealPlan(shoppingRemindersEnabled)
        }
    }

    if (saving) {
        BackHandler { }
        Column(Modifier.fillMaxSize().systemBarsPadding().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            androidx.compose.material3.CircularProgressIndicator()
            Text("Saving your kitchen…", modifier = Modifier.padding(top = 16.dp))
        }
        return
    }

    if (joiningHousehold && householdContent != null) {
        BackHandler { joiningHousehold = false }
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            TextButton(onClick = { joiningHousehold = false }) {
                Text(androidx.compose.ui.res.stringResource(com.example.pantrypal.R.string.household_back_to_setup))
            }
            androidx.compose.foundation.layout.Box(Modifier.weight(1f)) { householdContent() }
        }
        return
    }

    BackHandler(enabled = !saving && (stepIndex > 0 || isReplay)) { if (stepIndex > 0) stepIndex -= 1 else onSkip() }
    Column(modifier = modifier.fillMaxSize().systemBarsPadding().imePadding().padding(horizontal = PantryPalSpacing.sm, vertical = PantryPalSpacing.xs)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer) { Icon(Icons.Default.Eco, contentDescription = null, modifier = Modifier.padding(10.dp)) }
            Text("PantryPal", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = PantryPalSpacing.xs).weight(1f))
            TextButton(enabled = !saving, onClick = onSkip) { Text(if (isReplay) "Close" else "Skip setup") }
        }
        LinearProgressIndicator(progress = { (stepIndex + 1).toFloat() / setupSteps.size }, modifier = Modifier.fillMaxWidth().padding(vertical = PantryPalSpacing.xs).semantics { contentDescription = "Setup step ${stepIndex + 1} of ${setupSteps.size}" })
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(PantryPalSpacing.md)) {
            Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(PantryPalSpacing.md), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(PantryPalSpacing.sm)) {
                    Icon(step.icon, contentDescription = null)
                    Column { Text("${step.eyebrow} - ${stepIndex + 1} of ${setupSteps.size}".uppercase(), style = MaterialTheme.typography.labelMedium); Text(step.title, style = MaterialTheme.typography.headlineSmall) }
                }
            }
            if (stepIndex == 0 && householdContent != null) {
                OutlinedButton(onClick = { joiningHousehold = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(androidx.compose.ui.res.stringResource(com.example.pantrypal.R.string.household_join_onboarding))
                }
            }
            Text(step.supportingText, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            when (stepIndex) {
                0 -> ShoppingRoutineSetup(shoppingDay, { shoppingDay = it }, shoppingTime, { shoppingTime = it }, reminderTiming, { reminderTimingName = it.name }, shoppingRemindersEnabled, { shoppingRemindersEnabled = it })
                1 -> RegularsSetup(regularsText, { regularsText = it })
                2 -> {
                    MealEntry("A dinner you plan to make", firstMeal, { firstMeal = it }, firstMealDay, { firstMealDay = it })
                    if (firstMeal.isNotBlank()) IngredientsEntry(firstIngredients, { firstIngredients = it })
                    MealEntry("Another dinner (optional)", secondMeal, { secondMeal = it }, secondMealDay, { secondMealDay = it })
                    if (secondMeal.isNotBlank()) IngredientsEntry(secondIngredients, { secondIngredients = it })
                    MealEntry("One more dinner (optional)", thirdMeal, { thirdMeal = it }, thirdMealDay, { thirdMealDay = it })
                    if (thirdMeal.isNotBlank()) IngredientsEntry(thirdIngredients, { thirdIngredients = it })
                }
                3 -> {
                    OutlinedTextField(stockName, { stockName = it }, label = { Text("Item at home (optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    if (stockName.isNotBlank()) {
                        OutlinedTextField(stockAmount, { stockAmount = it }, label = { Text("Amount") }, singleLine = true, modifier = Modifier.fillMaxWidth(), keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal))
                        OutlinedTextField(stockUnit, { stockUnit = it }, label = { Text("Unit, e.g. g, kg, ml or pcs") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(stockExpiry, { stockExpiry = it }, label = { Text("Expiry date (optional)") }, supportingText = { Text("YYYY-MM-DD, e.g. ${LocalDate.now().plusDays(3)}") }, isError = stockExpiry.isNotBlank() && expiry == null, singleLine = true, modifier = Modifier.fillMaxWidth())
                    }
                    Text("Near-store reminders can be set up later in Settings while you are at the shop.")
                }
                else -> {
                    SetupSummary(shoppingDay, shoppingTime, reminderTiming, shoppingRemindersEnabled, onboardingRegulars(regularsText).size, meals().size)
                    SetupCard("Your pantry", if (stockName.isBlank()) "No stock added yet. Open Pantry to add or scan your first item; recipe suggestions improve as you add stock." else "$stockName will be added${if (expiry == null) " without an expiry date" else " with its expiry date"}.")
                    if (meals().isEmpty()) SetupCard("Next: your first meal", "No dinners added yet. In Plan, add a dinner and its ingredients before reviewing shopping.")
                }
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Row(modifier = Modifier.fillMaxWidth().padding(top = PantryPalSpacing.xs), horizontalArrangement = Arrangement.spacedBy(PantryPalSpacing.xs)) {
            if (stepIndex > 0) OutlinedButton(enabled = !saving, onClick = { stepIndex -= 1 }, modifier = Modifier.weight(1f)) { Text("Back") }
            Button(enabled = !saving && (stepIndex != 2 || mealsValid) && (stepIndex != 3 || stockValid) && (stepIndex != setupSteps.lastIndex || (mealsValid && stockValid)), onClick = { if (stepIndex == setupSteps.lastIndex) finish() else stepIndex += 1 }, modifier = Modifier.weight(if (stepIndex > 0) 1f else 2f)) {
                Text(if (saving) "Saving…" else if (stepIndex == setupSteps.lastIndex) "Save and open meal plan" else "Continue")
                Spacer(Modifier.width(PantryPalSpacing.xs)); Icon(if (stepIndex == setupSteps.lastIndex) Icons.Default.CalendarMonth else Icons.Default.CheckCircle, contentDescription = null)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShoppingRoutineSetup(day: Int, onDayChange: (Int) -> Unit, time: Int, onTimeChange: (Int) -> Unit, timing: ShoppingReminderTiming, onTimingChange: (ShoppingReminderTiming) -> Unit, remindersEnabled: Boolean, onRemindersEnabledChange: (Boolean) -> Unit) {
    Text("Usual shop day", style = MaterialTheme.typography.titleMedium)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { DayOfWeek.entries.forEach { value -> FilterChip(selected = day == value.value, onClick = { onDayChange(value.value) }, label = { Text(value.name.lowercase().replaceFirstChar(Char::titlecase).take(3)) }) } }
    Text("Around what time?", style = MaterialTheme.typography.titleMedium)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf(10 * 60 to "Morning", 14 * 60 to "Afternoon", 18 * 60 to "Evening").forEach { (value, label) -> FilterChip(selected = time == value, onClick = { onTimeChange(value) }, label = { Text(label) }) } }
    FilterChip(selected = remindersEnabled, onClick = { onRemindersEnabledChange(!remindersEnabled) }, label = { Text("Send me a shopping reminder") }, leadingIcon = { Icon(Icons.Default.NotificationsActive, contentDescription = null) }, modifier = Modifier.fillMaxWidth())
    if (remindersEnabled) {
        Text("When should we remind you?", style = MaterialTheme.typography.titleMedium)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { ShoppingReminderTiming.entries.forEach { value -> FilterChip(selected = timing == value, onClick = { onTimingChange(value) }, label = { Text(when (value) { ShoppingReminderTiming.NIGHT_BEFORE -> "Night before"; ShoppingReminderTiming.MORNING_OF -> "Morning of the shop"; ShoppingReminderTiming.HOUR_BEFORE -> "One hour before" }) }, modifier = Modifier.fillMaxWidth()) } }
    }
}

@Composable private fun RegularsSetup(value: String, onChange: (String) -> Unit) { OutlinedTextField(value = value, onValueChange = onChange, label = { Text("Regulars") }, supportingText = { Text("Separate items with commas, e.g. milk, bread, coffee") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }

@OptIn(ExperimentalLayoutApi::class)
@Composable private fun MealEntry(label: String, value: String, onValueChange: (String) -> Unit, day: Int, onDayChange: (Int) -> Unit) {
    OutlinedTextField(value = value, onValueChange = onValueChange, label = { Text(label) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
    if (value.isNotBlank()) {
        Text("Plan it for", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { DayOfWeek.entries.forEach { value -> FilterChip(selected = day == value.value, onClick = { onDayChange(value.value) }, label = { Text(value.name.lowercase().replaceFirstChar(Char::titlecase).take(3)) }) } }
    }
}

@Composable private fun SetupSummary(shoppingDay: Int, shoppingTime: Int, timing: ShoppingReminderTiming, remindersEnabled: Boolean, regularsCount: Int, mealCount: Int) {
    val timingLabel = when (timing) { ShoppingReminderTiming.NIGHT_BEFORE -> "the night before"; ShoppingReminderTiming.MORNING_OF -> "the morning of"; ShoppingReminderTiming.HOUR_BEFORE -> "one hour before" }
    SetupCard("Your weekly routine", if (remindersEnabled) "We'll nudge you $timingLabel your ${DayOfWeek.of(shoppingDay).name.lowercase().replaceFirstChar(Char::titlecase)} shop around ${ShoppingReminderSchedule.formatShoppingTime(shoppingTime)}." else "Your usual shop day is ${DayOfWeek.of(shoppingDay).name.lowercase().replaceFirstChar(Char::titlecase)}. You can turn reminders on later in Settings.")
    if (regularsCount > 0) SetupCard("Every week", "$regularsCount regular ${if (regularsCount == 1) "item" else "items"} will be added to your shopping list.")
    if (mealCount > 0) SetupCard("This week's dinners", "$mealCount ${if (mealCount == 1) "meal is" else "meals are"} ready in your meal plan.")
}

@Composable private fun SetupCard(title: String, text: String) { Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth()) { Column(modifier = Modifier.padding(PantryPalSpacing.sm), verticalArrangement = Arrangement.spacedBy(4.dp)) { Text(title, style = MaterialTheme.typography.titleMedium); Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }

@Composable private fun IngredientsEntry(value: String, onChange: (String) -> Unit) {
    OutlinedTextField(value, onChange, label = { Text("Shopping ingredients") }, supportingText = { Text("Separate with commas, e.g. 300 g rice, 2 peppers. Use the same name as your pantry item.") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
}
