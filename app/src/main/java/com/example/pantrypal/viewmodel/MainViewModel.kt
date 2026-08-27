package com.example.pantrypal.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.pantrypal.data.dao.ConsumptionWithItem
import com.example.pantrypal.data.dao.InventoryWithItemMap
import com.example.pantrypal.data.entity.ConsumptionEntity
import com.example.pantrypal.data.entity.ConsumptionType
import com.example.pantrypal.data.entity.InventoryEntity
import com.example.pantrypal.data.entity.ItemEntity
import com.example.pantrypal.data.entity.ShoppingItemEntity
import com.example.pantrypal.data.entity.MealEntity
import com.example.pantrypal.data.entity.MealWeekEntity
import com.example.pantrypal.data.entity.ShoppingHistoryEntity
import com.example.pantrypal.data.entity.ShoppingSectionEntity
import com.example.pantrypal.data.repository.KitchenRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import com.example.pantrypal.util.mealsForShopping
import com.example.pantrypal.util.normalizedIngredients
import com.example.pantrypal.util.rotatingWeek
import com.example.pantrypal.util.startOfWeek
import com.example.pantrypal.util.AppPreferences
import com.example.pantrypal.util.AddItemDefaults
import com.example.pantrypal.util.AppSettings
import com.example.pantrypal.util.AppThemeMode
import com.example.pantrypal.util.ExpiryStatus
import com.example.pantrypal.util.ShoppingNeedStatus
import com.example.pantrypal.util.ShoppingReconciliationLine
import com.example.pantrypal.util.ShoppingLocation
import com.example.pantrypal.util.normalizeShoppingName
import com.example.pantrypal.util.ShoppingLocationStore
import com.example.pantrypal.util.ShoppingReminderTiming
import com.example.pantrypal.util.classifyExpiry
import com.example.pantrypal.util.expiryLabel
import com.example.pantrypal.util.expiringCutoffMillis
import com.example.pantrypal.util.reconcileShoppingIngredients

// Helper flow for periodic updates
fun tickerFlow(period: Long, initialDelay: Long = 0) = flow {
    delay(initialDelay)
    while (true) {
        emit(Unit)
        delay(period)
    }
}

class MainViewModel(private val repository: KitchenRepository, application: Application) : AndroidViewModel(application) {

    companion object {
        const val STYLE_RANDOM = "RANDOM"
        const val STYLE_WEEK_AHEAD = "WEEK_AHEAD"
        const val STYLE_TWO_WEEKS = "TWO_WEEKS"
    }

    private val prefs = application.getSharedPreferences(AppPreferences.FILE_NAME, Context.MODE_PRIVATE)
    private val savedWeek = prefs.getString("current_week", MealEntity.WEEK_A) ?: MealEntity.WEEK_A
    private val savedAnchorMonday = if (prefs.contains("meal_week_anchor")) {
        prefs.getLong("meal_week_anchor", startOfWeek(LocalDate.now()).toEpochDay())
    } else {
        startOfWeek(LocalDate.now()).toEpochDay().also { anchor ->
            prefs.edit().putString("current_week", savedWeek).putLong("meal_week_anchor", anchor).apply()
        }
    }
    private val _currentWeek = MutableStateFlow(
        rotatingWeek(savedWeek, savedAnchorMonday, LocalDate.now())
    )
    val currentWeek: StateFlow<String> = _currentWeek.asStateFlow()
    private val _shoppingWeek = MutableStateFlow(_currentWeek.value)
    val shoppingWeek: StateFlow<String> = _shoppingWeek.asStateFlow()

    private val _mealPlanStyle = MutableStateFlow(prefs.getString("meal_plan_style", null))
    val mealPlanStyle: StateFlow<String?> = _mealPlanStyle.asStateFlow()

    private val _hasCompletedOnboarding = MutableStateFlow(
        prefs.getBoolean(AppPreferences.KEY_ONBOARDING_COMPLETE, false)
    )
    val hasCompletedOnboarding: StateFlow<Boolean> = _hasCompletedOnboarding.asStateFlow()

    private val _hasSeenMealPlanIntro = MutableStateFlow(
        prefs.getBoolean(AppPreferences.KEY_MEAL_PLAN_INTRO_SEEN, false)
    )
    val hasSeenMealPlanIntro: StateFlow<Boolean> = _hasSeenMealPlanIntro.asStateFlow()

    private val _hasSeenSettingsIntro = MutableStateFlow(
        prefs.getBoolean(AppPreferences.KEY_SETTINGS_INTRO_SEEN, false)
    )
    val hasSeenSettingsIntro: StateFlow<Boolean> = _hasSeenSettingsIntro.asStateFlow()

    private val _appSettings = MutableStateFlow(AppPreferences.readSettings(application))
    val appSettings: StateFlow<AppSettings> = _appSettings.asStateFlow()

    private val _addItemDefaults = MutableStateFlow(AppPreferences.readAddItemDefaults(application))
    val addItemDefaults: StateFlow<AddItemDefaults> = _addItemDefaults.asStateFlow()

    private val _shoppingLocations = MutableStateFlow(ShoppingLocationStore.read(application))
    val shoppingLocations: StateFlow<List<ShoppingLocation>> = _shoppingLocations.asStateFlow()

    fun setCurrentWeek(week: String) {
        _currentWeek.value = week
        prefs.edit()
            .putString("current_week", week)
            .putLong("meal_week_anchor", startOfWeek(LocalDate.now()).toEpochDay())
            .apply()
    }

    fun setMealPlanStyle(style: String) {
        _mealPlanStyle.value = style
        prefs.edit().putString("meal_plan_style", style).apply()
    }

    fun setShoppingWeek(week: String) {
        _shoppingWeek.value = week
    }

    fun completeOnboarding() {
        _hasCompletedOnboarding.value = true
        prefs.edit()
            .putBoolean(AppPreferences.KEY_ONBOARDING_COMPLETE, true)
            .apply()
    }

    fun markMealPlanIntroSeen() {
        if (_hasSeenMealPlanIntro.value) return
        _hasSeenMealPlanIntro.value = true
        prefs.edit().putBoolean(AppPreferences.KEY_MEAL_PLAN_INTRO_SEEN, true).apply()
    }

    fun markSettingsIntroSeen() {
        if (_hasSeenSettingsIntro.value) return
        _hasSeenSettingsIntro.value = true
        prefs.edit().putBoolean(AppPreferences.KEY_SETTINGS_INTRO_SEEN, true).apply()
    }

    fun setThemeMode(themeMode: AppThemeMode) {
        _appSettings.value = _appSettings.value.copy(themeMode = themeMode)
        prefs.edit().putString(AppPreferences.KEY_THEME_MODE, themeMode.name).apply()
    }

    fun setDynamicColorEnabled(enabled: Boolean) {
        _appSettings.value = _appSettings.value.copy(dynamicColorEnabled = enabled)
        prefs.edit().putBoolean(AppPreferences.KEY_DYNAMIC_COLOR, enabled).apply()
    }

    fun setExpiryRemindersEnabled(enabled: Boolean) {
        _appSettings.value = _appSettings.value.copy(expiryRemindersEnabled = enabled)
        prefs.edit().putBoolean(AppPreferences.KEY_EXPIRY_REMINDERS, enabled).apply()
    }

    fun setShoppingRemindersEnabled(enabled: Boolean) {
        _appSettings.value = _appSettings.value.copy(shoppingRemindersEnabled = enabled)
        prefs.edit().putBoolean(AppPreferences.KEY_SHOPPING_REMINDERS, enabled).apply()
    }

    fun setShoppingReminderDay(dayOfWeek: Int) {
        val safeDay = dayOfWeek.coerceIn(1, 7)
        _appSettings.value = _appSettings.value.copy(shoppingDayOfWeek = safeDay)
        prefs.edit().putInt(AppPreferences.KEY_SHOPPING_DAY, safeDay).apply()
    }

    fun setShoppingReminderTime(minutesSinceMidnight: Int) {
        val safeTime = minutesSinceMidnight.coerceIn(0, 23 * 60 + 59)
        _appSettings.value = _appSettings.value.copy(shoppingTimeMinutes = safeTime)
        prefs.edit().putInt(AppPreferences.KEY_SHOPPING_TIME, safeTime).apply()
    }

    fun setShoppingReminderTiming(timing: ShoppingReminderTiming) {
        _appSettings.value = _appSettings.value.copy(shoppingReminderTiming = timing)
        prefs.edit().putString(AppPreferences.KEY_SHOPPING_REMINDER_TIMING, timing.name).apply()
    }

    fun setNearbyShoppingRemindersEnabled(enabled: Boolean) {
        _appSettings.value = _appSettings.value.copy(nearbyShoppingRemindersEnabled = enabled)
        prefs.edit().putBoolean(AppPreferences.KEY_NEARBY_SHOPPING_REMINDERS, enabled).apply()
    }

    fun addShoppingLocation(name: String, latitude: Double, longitude: Double) {
        val location = ShoppingLocation(
            name = name.trim(),
            latitude = latitude,
            longitude = longitude
        )
        if (!location.isValid()) return
        val updated = (_shoppingLocations.value + location)
            .take(ShoppingLocationStore.MAX_LOCATIONS)
        _shoppingLocations.value = updated
        ShoppingLocationStore.write(getApplication(), updated)
    }

    fun deleteShoppingLocation(location: ShoppingLocation) {
        val updated = _shoppingLocations.value.filterNot { it.id == location.id }
        _shoppingLocations.value = updated
        ShoppingLocationStore.write(getApplication(), updated)
    }

    // UI State for Inventory
    val inventoryState: StateFlow<List<InventoryUiModel>> = repository.currentInventory
        .map { list ->
            val totals = list.groupBy { it.itemId }.mapValues { (_, batches) -> batches.sumOf { it.quantity } }
            list.map { it.toUiModel(totalQuantity = totals[it.itemId] ?: it.quantity) }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // UI State for Expiring Items
    val expiringItemsState: StateFlow<List<InventoryUiModel>> = tickerFlow(60_000L) // Check every minute
        .flatMapLatest {
            repository.getExpiringItems(expiringCutoffMillis())
        }
        .map { list ->
            list.map { it.toUiModel() }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val pastItemsState: StateFlow<List<ConsumptionWithItem>> = repository.allConsumptionHistory
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // UI State for Restock Suggestions
    val restockSuggestionsState: StateFlow<List<ItemEntity>> = repository.currentInventory
        .flatMapLatest { flow { emit(repository.getRestockSuggestions(System.currentTimeMillis())) } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // UI State for Shopping List
    val shoppingListState: StateFlow<List<ShoppingItemEntity>> = repository.shoppingList
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val shoppingSectionsState: StateFlow<List<ShoppingSectionEntity>> = repository.shoppingSections
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val shoppingHistoryState: StateFlow<List<ShoppingHistoryEntity>> = repository.shoppingHistory
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _shoppingBuildPreview = MutableStateFlow<ShoppingBuildPreview?>(null)
    val shoppingBuildPreview: StateFlow<ShoppingBuildPreview?> = _shoppingBuildPreview.asStateFlow()

    val mealsState: StateFlow<List<MealEntity>> = repository.allMeals
         .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
         )

    val mealWeeksState: StateFlow<List<MealWeekEntity>> = repository.mealWeeks
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = listOf(
                MealWeekEntity("A", "Week A", "🥔", 0),
                MealWeekEntity("B", "Week B", "🐟", 1),
                MealWeekEntity("C", "Week C", "🍕", 2),
                MealWeekEntity("D", "Week D", "🍝", 3)
            )
        )

    fun addMeal(name: String, week: String, dayOfWeek: Int, mealSlot: String, ingredients: List<String>) {
        viewModelScope.launch {
            repository.insertMeal(
                MealEntity(
                    name = name.trim(),
                    week = week,
                    ingredients = normalizedIngredients(ingredients),
                    dayOfWeek = dayOfWeek.coerceIn(1, 7),
                    mealSlot = mealSlot
                )
            )
        }
    }

    fun updateMeal(meal: MealEntity, name: String, dayOfWeek: Int, mealSlot: String, ingredients: List<String>) {
        viewModelScope.launch {
            repository.updateMeal(
                meal.copy(
                    name = name.trim(),
                    ingredients = normalizedIngredients(ingredients),
                    dayOfWeek = dayOfWeek.coerceIn(1, 7),
                    mealSlot = mealSlot
                )
            )
        }
    }

    fun copyMealToWeek(meal: MealEntity, targetWeek: String) {
        viewModelScope.launch {
            if (targetWeek == meal.week) return@launch
            val alreadyExists = repository.allMeals.first().any {
                it.week == targetWeek &&
                    it.dayOfWeek == meal.dayOfWeek &&
                    it.mealSlot == meal.mealSlot &&
                    it.name.equals(meal.name, ignoreCase = true)
            }
            if (!alreadyExists) {
                repository.insertMeal(meal.copy(mealId = 0, week = targetWeek))
            }
        }
    }

    fun copyWeek(sourceWeek: String, targetWeek: String) {
        viewModelScope.launch {
            val meals = repository.allMeals.first()
            val targetKeys = meals.filter { it.week == targetWeek }
                .map { Triple(it.name.lowercase(), it.dayOfWeek, it.mealSlot) }
                .toSet()
            meals.filter { it.week == sourceWeek }
                .distinctBy { Triple(it.name.lowercase(), it.dayOfWeek, it.mealSlot) }
                .forEach { meal ->
                val key = Triple(meal.name.lowercase(), meal.dayOfWeek, meal.mealSlot)
                if (key !in targetKeys) {
                    repository.insertMeal(meal.copy(mealId = 0, week = targetWeek))
                }
            }
        }
    }

    fun buildShoppingListForWeek(week: String) {
        _shoppingWeek.value = week
        viewModelScope.launch {
            val ingredients = mealsForShopping(repository.allMeals.first(), week)
            val inventory = repository.currentInventory.first()
            commitShoppingPreview(
                ShoppingBuildPreview(
                    weekId = week,
                    lines = reconcileShoppingIngredients(ingredients, inventory)
                )
            )
        }
    }

    fun previewShoppingListForWeek(week: String) {
        _shoppingWeek.value = week
        viewModelScope.launch {
            val ingredients = mealsForShopping(repository.allMeals.first(), week)
            _shoppingBuildPreview.value = ShoppingBuildPreview(
                weekId = week,
                lines = reconcileShoppingIngredients(ingredients, repository.currentInventory.first())
            )
        }
    }

    fun dismissShoppingBuildPreview() {
        _shoppingBuildPreview.value = null
    }

    fun commitShoppingBuildPreview(includeCheckStock: Boolean = true) {
        val preview = _shoppingBuildPreview.value ?: return
        viewModelScope.launch {
            commitShoppingPreview(preview, includeCheckStock)
            _shoppingBuildPreview.value = null
        }
    }

    private suspend fun commitShoppingPreview(
        preview: ShoppingBuildPreview,
        includeCheckStock: Boolean = true
    ) {
            val week = preview.weekId
            val existing = repository.shoppingList.first()
            val sections = repository.shoppingSections.first()
            val recurringSectionIds = sections.filter { it.recursEveryWeek }.map { it.sectionId }.toSet()
            val namesAlreadyCovered = existing.filter {
                it.sectionId in recurringSectionIds ||
                    (it.weekId == week && it.sectionId != ShoppingSectionEntity.ID_MEAL_PLAN)
            }.map { normalizeShoppingName(it.name) }.toSet()

            repository.deleteShoppingItemsInSectionForWeek(ShoppingSectionEntity.ID_MEAL_PLAN, week)
            preview.lines
                .filter {
                    it.status == ShoppingNeedStatus.NEED_TO_BUY ||
                        (includeCheckStock && it.status == ShoppingNeedStatus.CHECK_STOCK)
                }
                .forEach { line ->
                repository.rememberShoppingItem(line.name)
                if (normalizeShoppingName(line.name) !in namesAlreadyCovered) {
                    repository.addShoppingItem(
                        ShoppingItemEntity(
                            name = line.name,
                            quantity = line.requiredQuantity,
                            unit = line.unit,
                            frequency = ShoppingItemEntity.FREQ_ONE_OFF,
                            sectionId = ShoppingSectionEntity.ID_MEAL_PLAN,
                            weekId = week
                        )
                    )
                }
            }
    }

    fun updateMealWeek(week: MealWeekEntity, name: String, emoji: String) {
        viewModelScope.launch {
            repository.updateMealWeek(week.copy(name = name.trim(), emoji = emoji.trim()))
        }
    }

    fun deleteMeal(meal: MealEntity) {
        viewModelScope.launch {
            repository.deleteMeal(meal)
        }
    }

    fun addItem(
        name: String,
        quantity: Double,
        unit: String,
        category: String,
        isVeg: Boolean,
        isGlutenFree: Boolean,
        barcode: String? = null,
        expirationDate: Long? = null,
        imageUrl: String? = null,
        isUsual: Boolean = false,
        lowStockThreshold: Double? = null,
        storageLocation: String = InventoryEntity.LOCATION_PANTRY,
        isOpened: Boolean = false
    ) {
        viewModelScope.launch {
            var itemId: Long = -1

            if (!barcode.isNullOrEmpty()) {
                 val existingItem = repository.getItemByBarcode(barcode)
                 if (existingItem != null) {
                     itemId = existingItem.itemId
                 }
            }

            if (itemId == -1L) {
                val item = ItemEntity(
                    name = name,
                    defaultUnit = unit,
                    category = category,
                    isVegetarian = isVeg,
                    isGlutenFree = isGlutenFree,
                    isUsual = isUsual,
                    lowStockThreshold = lowStockThreshold,
                    barcode = barcode,
                    imageUrl = imageUrl
                )
                itemId = repository.insertItem(item)
            }

            if (itemId != -1L) {
                val inventory = InventoryEntity(
                    itemId = itemId,
                    quantity = quantity,
                    unit = unit,
                    expirationDate = expirationDate,
                    storageLocation = storageLocation,
                    isOpened = isOpened
                )
                repository.addInventory(inventory)
                if (isUsual || lowStockThreshold != null) {
                    repository.updateStockSettings(itemId, isUsual, lowStockThreshold)
                }
                val defaults = AddItemDefaults(
                    unit = unit.trim().ifBlank { "pcs" },
                    category = category.trim().ifBlank { "General" },
                    storageLocation = storageLocation.trim().ifBlank { InventoryEntity.LOCATION_PANTRY }
                )
                _addItemDefaults.value = defaults
                prefs.edit()
                    .putString(AppPreferences.KEY_ADD_ITEM_UNIT, defaults.unit)
                    .putString(AppPreferences.KEY_ADD_ITEM_CATEGORY, defaults.category)
                    .putString(AppPreferences.KEY_ADD_ITEM_LOCATION, defaults.storageLocation)
                    .apply()
            }
        }
    }

    suspend fun getItemByBarcode(barcode: String): ItemEntity? {
        return repository.getItemByBarcode(barcode) ?: repository.getItemByBarcodeFromApi(barcode)
    }

    suspend fun getInventoryByBarcode(barcode: String): List<InventoryWithItemMap> {
        return repository.getInventoryByBarcode(barcode)
    }

    private suspend fun consumeItemSuspend(inventoryId: Long, itemId: Long, quantity: Double, type: ConsumptionType, reason: String? = null) {
        val inventory = repository.getInventorySnapshot().firstOrNull { it.inventoryId == inventoryId } ?: return
        val consumedQuantity = quantity.coerceAtLeast(0.0).coerceAtMost(inventory.quantity)
        if (consumedQuantity <= 0.0) return

        // Log consumption
        val consumption = ConsumptionEntity(
            itemId = itemId,
            quantity = consumedQuantity,
            type = type,
            wasteReason = reason
        )
        repository.logConsumption(consumption)

        val remaining = inventory.quantity - consumedQuantity
        if (remaining <= 0.0) {
            repository.removeInventory(inventory)
        } else {
            repository.updateInventory(inventory.copy(quantity = remaining))
        }

        // Auto-add to shopping list if "Usual"
        if (type == ConsumptionType.FINISHED) {
            val item = repository.getItemById(itemId)
            val restockBoundary = item?.lowStockThreshold ?: 0.0
            val totalRemaining = repository.getInventorySnapshot()
                .filter { it.itemId == itemId }
                .sumOf { it.quantity }
            val alreadyListed = item?.let { stockedItem ->
                repository.shoppingList.first().any {
                    !it.isChecked &&
                        normalizeShoppingName(it.name) == normalizeShoppingName(stockedItem.name)
                }
            } ?: false
            if (item != null && item.isUsual && totalRemaining <= restockBoundary && !alreadyListed) {
                repository.addShoppingItem(
                    ShoppingItemEntity(
                        name = item.name,
                        quantity = restockBoundary.coerceAtLeast(1.0),
                        unit = item.defaultUnit,
                        sectionId = ShoppingSectionEntity.ID_THE_REST,
                        weekId = _shoppingWeek.value
                    )
                )
            }
        }
    }

    fun consumeItem(inventoryId: Long, itemId: Long, quantity: Double, type: ConsumptionType, reason: String? = null) {
        viewModelScope.launch {
            consumeItemSuspend(inventoryId, itemId, quantity, type, reason)
        }
    }

    fun consumeItems(items: List<InventoryWithItemMap>, type: ConsumptionType) {
        viewModelScope.launch {
            items.forEach { item ->
                consumeItemSuspend(item.inventoryId, item.itemId, 1.0.coerceAtMost(item.quantity), type)
            }
        }
    }

    fun consumeItemAmounts(items: List<Pair<InventoryWithItemMap, Double>>, type: ConsumptionType) {
        viewModelScope.launch {
            items.forEach { (item, amount) ->
                consumeItemSuspend(item.inventoryId, item.itemId, amount, type)
            }
        }
    }

    fun adjustInventoryQuantity(inventoryId: Long, delta: Double) {
        viewModelScope.launch {
            val inventory = repository.getInventorySnapshot().firstOrNull { it.inventoryId == inventoryId } ?: return@launch
            val nextQuantity = inventory.quantity + delta
            if (nextQuantity <= 0.0) repository.removeInventory(inventory)
            else repository.updateInventory(inventory.copy(quantity = nextQuantity))
        }
    }

    fun toggleInventoryOpened(item: InventoryUiModel) {
        viewModelScope.launch {
            val inventory = repository.getInventorySnapshot().firstOrNull { it.inventoryId == item.inventoryId } ?: return@launch
            repository.updateInventory(inventory.copy(isOpened = !inventory.isOpened))
        }
    }

    fun updateInventoryLocation(item: InventoryUiModel, storageLocation: String) {
        viewModelScope.launch {
            val inventory = repository.getInventorySnapshot().firstOrNull { it.inventoryId == item.inventoryId } ?: return@launch
            repository.updateInventory(inventory.copy(storageLocation = storageLocation.trim().ifEmpty { InventoryEntity.LOCATION_PANTRY }))
        }
    }

    fun updateStockSettings(itemId: Long, isUsual: Boolean, lowStockThreshold: Double?) {
        viewModelScope.launch {
            repository.updateStockSettings(itemId, isUsual, lowStockThreshold)
        }
    }

    fun addRestockToShopping(item: ItemEntity, weekId: String = _currentWeek.value) {
        viewModelScope.launch {
            val alreadyListed = repository.shoppingList.first().any {
                !it.isChecked &&
                    normalizeShoppingName(it.name) == normalizeShoppingName(item.name) &&
                    (it.weekId == null || it.weekId == weekId)
            }
            if (!alreadyListed) {
                repository.addShoppingItem(
                    ShoppingItemEntity(
                        name = item.name,
                        quantity = item.lowStockThreshold?.coerceAtLeast(1.0) ?: 1.0,
                        unit = item.defaultUnit,
                        sectionId = ShoppingSectionEntity.ID_THE_REST,
                        weekId = weekId
                    )
                )
                repository.rememberShoppingItem(item.name)
            }
        }
    }

    fun addShoppingItem(
        name: String,
        quantity: Double,
        unit: String,
        sectionId: Long = ShoppingSectionEntity.ID_THE_REST,
        weekId: String? = _currentWeek.value
    ) {
        viewModelScope.launch {
             val trimmedName = name.trim()
             val item = ShoppingItemEntity(
                 name = trimmedName,
                 quantity = quantity,
                 unit = unit.trim().ifEmpty { "pcs" },
                 sectionId = sectionId,
                 weekId = weekId
             )
             repository.addShoppingItem(item)
             repository.rememberShoppingItem(trimmedName)
        }
    }

    fun updateShoppingItem(item: ShoppingItemEntity, name: String, quantity: Double, unit: String) {
        viewModelScope.launch {
            val trimmedName = name.trim()
            repository.updateShoppingItem(
                item.copy(name = trimmedName, quantity = quantity, unit = unit.trim().ifEmpty { "pcs" })
            )
            repository.rememberShoppingItem(trimmedName)
        }
    }

    fun addShoppingSection(name: String, recursEveryWeek: Boolean) {
        viewModelScope.launch {
            val nextOrder = (repository.shoppingSections.first().maxOfOrNull { it.sortOrder } ?: 0) + 1
            repository.insertShoppingSection(
                ShoppingSectionEntity(
                    name = name.trim(),
                    sortOrder = nextOrder,
                    recursEveryWeek = recursEveryWeek
                )
            )
        }
    }

    fun updateShoppingSection(section: ShoppingSectionEntity, name: String, recursEveryWeek: Boolean) {
        viewModelScope.launch {
            repository.updateShoppingSection(
                section.copy(name = name.trim(), recursEveryWeek = recursEveryWeek)
            )
        }
    }

    fun deleteShoppingSection(section: ShoppingSectionEntity) {
        if (section.systemKey != null) return
        viewModelScope.launch {
            repository.deleteShoppingItemsInSection(section.sectionId)
            repository.deleteShoppingSection(section)
        }
    }

    fun toggleShoppingItem(item: ShoppingItemEntity) {
        viewModelScope.launch {
            repository.updateShoppingItem(item.copy(isChecked = !item.isChecked))
        }
    }

    fun deleteShoppingItem(item: ShoppingItemEntity) {
        viewModelScope.launch {
            repository.deleteShoppingItem(item)
        }
    }

    fun restoreShoppingItem(item: ShoppingItemEntity) {
        viewModelScope.launch {
            repository.addShoppingItem(item)
        }
    }

    fun clearCheckedShoppingItems(weekId: String = _currentWeek.value) {
        viewModelScope.launch {
            repository.deleteCheckedShoppingItems(weekId)
        }
    }

    fun finishShopping(weekId: String, storageLocation: String) {
        viewModelScope.launch {
            val sections = repository.shoppingSections.first()
            val recurringIds = sections.filter { it.recursEveryWeek }.map { it.sectionId }.toSet()
            val checked = repository.shoppingList.first().filter {
                it.isChecked && (it.sectionId in recurringIds || it.weekId == null || it.weekId == weekId)
            }
            repository.putAwayShoppingItems(checked, storageLocation)
            repository.deleteCheckedShoppingItems(weekId)
        }
    }

    // Export
    fun exportData() {
        viewModelScope.launch {
            val data = repository.getAllDataForExport()
            println("Exporting: $data")
        }
    }
}

data class InventoryUiModel(
    val inventoryId: Long,
    val itemId: Long,
    val name: String,
    val quantity: String,
    val quantityValue: Double,
    val unit: String,
    val category: String,
    val storageLocation: String,
    val expirationDate: Long?,
    val expiryStatus: ExpiryStatus,
    val expiryLabel: String,
    val isOpened: Boolean,
    val isUsual: Boolean,
    val lowStockThreshold: Double?,
    val tags: List<String>,
    val imageUrl: String? = null,
    val isRestockNeeded: Boolean = false,
    val addedDate: Long
)

fun InventoryWithItemMap.toUiModel(totalQuantity: Double = quantity): InventoryUiModel {
    val tags = mutableListOf<String>()
    if (isVegetarian) tags.add("Veg")
    if (isGlutenFree) tags.add("GF")

    val status = classifyExpiry(expirationDate)
    return InventoryUiModel(
        inventoryId = inventoryId,
        itemId = itemId,
        name = name,
        quantity = "$quantity $unit",
        quantityValue = quantity,
        unit = unit,
        category = category,
        storageLocation = storageLocation,
        expirationDate = expirationDate,
        expiryStatus = status,
        expiryLabel = expiryLabel(status, expirationDate),
        isOpened = isOpened,
        isUsual = isUsual,
        lowStockThreshold = lowStockThreshold,
        tags = tags,
        imageUrl = imageUrl,
        isRestockNeeded = lowStockThreshold?.let { totalQuantity <= it } == true,
        addedDate = addedDate
    )
}

data class ShoppingBuildPreview(
    val weekId: String,
    val lines: List<ShoppingReconciliationLine>
)

class MainViewModelFactory(private val repository: KitchenRepository, private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
