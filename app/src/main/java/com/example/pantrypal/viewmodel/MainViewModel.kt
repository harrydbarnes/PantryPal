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
import com.example.pantrypal.data.repository.KitchenRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
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
        const val STYLE_RANDOM = "Random"
        const val STYLE_WEEK_AHEAD = "Week ahead"
        const val STYLE_TWO_WEEKS = "Two week schedule"
    }

    private val prefs = application.getSharedPreferences("pantry_prefs", Context.MODE_PRIVATE)
    private val _currentWeek = MutableStateFlow(prefs.getString("current_week", "A") ?: "A")
    val currentWeek: StateFlow<String> = _currentWeek.asStateFlow()

    private val _mealPlanStyle = MutableStateFlow(prefs.getString("meal_plan_style", null))
    val mealPlanStyle: StateFlow<String?> = _mealPlanStyle.asStateFlow()

    fun setCurrentWeek(week: String) {
        _currentWeek.value = week
        prefs.edit().putString("current_week", week).apply()
    }

    fun setMealPlanStyle(style: String) {
        _mealPlanStyle.value = style
        prefs.edit().putString("meal_plan_style", style).apply()
    }

    // UI State for Inventory
    val inventoryState: StateFlow<List<InventoryUiModel>> = repository.currentInventory
        .map { list ->
            list.map { it.toUiModel() }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // UI State for Expiring Items
    val expiringItemsState: StateFlow<List<InventoryUiModel>> = tickerFlow(60_000L) // Check every minute
        .flatMapLatest { repository.getExpiringItems(System.currentTimeMillis()) }
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
    val restockSuggestionsState: StateFlow<List<ItemEntity>> = tickerFlow(3_600_000L) // Check every hour
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

    val mealsState: StateFlow<List<MealEntity>> = repository.allMeals
         .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addMeal(name: String, week: String, ingredients: List<String>) {
        viewModelScope.launch {
            repository.insertMeal(MealEntity(name = name, week = week, ingredients = ingredients))
            // Auto add ingredients to shopping list logic
            val currentShoppingList = repository.shoppingList.first()
            ingredients.distinctBy { it.lowercase() }.forEach { ingredient ->
                 val freq = if (week == "A") ShoppingItemEntity.FREQ_WEEK_A else ShoppingItemEntity.FREQ_WEEK_B
                 val alreadyInList = currentShoppingList.any {
                     it.name.equals(ingredient, ignoreCase = true) && it.frequency == freq
                 }
                 if (!alreadyInList) {
                     repository.addShoppingItem(
                         ShoppingItemEntity(
                            name = ingredient,
                            frequency = freq
                         )
                     )
                 }
            }
        }
    }

    fun deleteMeal(meal: MealEntity) {
        viewModelScope.launch {
            repository.deleteMeal(meal)
        }
    }

    fun addItem(name: String, quantity: Double, unit: String, category: String, isVeg: Boolean, isGlutenFree: Boolean, barcode: String? = null, expirationDate: Long? = null, imageUrl: String? = null) {
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
                    expirationDate = expirationDate
                )
                repository.addInventory(inventory)
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
        // Log consumption
        val consumption = ConsumptionEntity(
            itemId = itemId,
            quantity = quantity,
            type = type,
            wasteReason = reason
        )
        repository.logConsumption(consumption)

        // For this exercise, I'll assume we just delete the row (consumed all).
        val inv = InventoryEntity(inventoryId = inventoryId, itemId = itemId, quantity = quantity, unit = "") // Dummy unit/qty for delete
        repository.removeInventory(inv)

        // Auto-add to shopping list if "Usual"
        if (type == ConsumptionType.FINISHED) {
            val item = repository.getItemById(itemId)
            if (item != null && item.isUsual) {
                val shoppingItem = ShoppingItemEntity(
                    name = item.name,
                    quantity = 1.0, // Default to 1
                    unit = item.defaultUnit
                )
                repository.addShoppingItem(shoppingItem)
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
            items.map { item ->
                async { consumeItemSuspend(item.inventoryId, item.itemId, 1.0, type) }
            }.awaitAll()
        }
    }

    fun addShoppingItem(name: String, quantity: Double, unit: String, frequency: String = ShoppingItemEntity.FREQ_ONE_OFF) {
        viewModelScope.launch {
             val item = ShoppingItemEntity(
                 name = name,
                 quantity = quantity,
                 unit = unit,
                 frequency = frequency
             )
             repository.addShoppingItem(item)
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

    fun clearCheckedShoppingItems() {
        viewModelScope.launch {
            repository.deleteCheckedShoppingItems()
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
    val tags: List<String>,
    val imageUrl: String? = null,
    val isRestockNeeded: Boolean = false
)

fun InventoryWithItemMap.toUiModel(): InventoryUiModel {
    val tags = mutableListOf<String>()
    if (isVegetarian) tags.add("Veg")
    if (isGlutenFree) tags.add("GF")

    return InventoryUiModel(
        inventoryId = inventoryId,
        itemId = itemId,
        name = name,
        quantity = "$quantity $unit",
        tags = tags,
        imageUrl = imageUrl
    )
}

class MainViewModelFactory(private val repository: KitchenRepository, private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
