package com.example.kitchenlocal.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.kitchenlocal.data.dao.InventoryWithItemMap
import com.example.kitchenlocal.data.entity.ConsumptionEntity
import com.example.kitchenlocal.data.entity.ConsumptionType
import com.example.kitchenlocal.data.entity.InventoryEntity
import com.example.kitchenlocal.data.entity.ItemEntity
import com.example.kitchenlocal.data.repository.KitchenRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(private val repository: KitchenRepository) : ViewModel() {

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

    fun addItem(name: String, quantity: Double, unit: String, category: String, isVeg: Boolean, isGlutenFree: Boolean) {
        viewModelScope.launch {
            // Check if item exists (simple check by name for now, in real app, better check)
            // For now, assume new item creation every time or user selects existing.
            // Simplified: Create Item then Inventory
            val item = ItemEntity(
                name = name,
                defaultUnit = unit,
                category = category,
                isVegetarian = isVeg,
                isGlutenFree = isGlutenFree
            )
            var itemId = repository.insertItem(item)
            if (itemId == -1L) {
                // Item exists (assuming barcode conflict, though simplified logic here uses name)
                // In real app we would check barcode. Here we might need to fetch by name or barcode.
                // Since our simplified 'addItem' doesn't take barcode, we assume unique name/category for now or just fetch valid one.
                // Wait, name is NOT unique in DB schema. Only barcode is unique.
                // If insert returns -1, it means unique constraint violation.
                // But without barcode in input, what violated it? nothing.
                // Ah, unless we add unique constraint on name? No.
                // So insert will always succeed for non-barcode items.
                // But if we extend this to use barcode, we need this check.
                // For safety, let's keep it robust.
            }

            // NOTE: Ideally we should fetch the ID if it exists.
            // Since we don't have barcode in this specific function signature, we assume new item.
            // But if we did have barcode...

            val inventory = InventoryEntity(
                itemId = itemId,
                quantity = quantity,
                unit = unit
            )
            repository.addInventory(inventory)
        }
    }

    fun consumeItem(inventoryId: Long, itemId: Long, quantity: Double, type: ConsumptionType, reason: String? = null) {
        viewModelScope.launch {
            // Log consumption
            val consumption = ConsumptionEntity(
                itemId = itemId,
                quantity = quantity,
                type = type,
                wasteReason = reason
            )
            repository.logConsumption(consumption)

            // Update inventory (Assuming full consumption of that specific entry for simplicity)
            // In a real app, we'd decrement quantity and delete if 0.
            // Here, let's assume the UI passes the specific InventoryEntity to remove.
            // But we only have IDs here.

            // Note: Ideally, we fetch the inventory item first to check quantity.
            // For this exercise, I'll assume we just delete the row (consumed all).
             val inv = InventoryEntity(inventoryId = inventoryId, itemId = itemId, quantity = quantity, unit = "") // Dummy unit/qty for delete
             repository.removeInventory(inv)
        }
    }

    // Export
    fun exportData() {
        viewModelScope.launch {
            val data = repository.getAllDataForExport()
            // In a real app, we would write this to a file using ContentResolver/Storage Access Framework
            // Here we just simulate the data gathering.
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
    val isRestockNeeded: Boolean = false // Placeholder for logic
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
        tags = tags
    )
}

class MainViewModelFactory(private val repository: KitchenRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
