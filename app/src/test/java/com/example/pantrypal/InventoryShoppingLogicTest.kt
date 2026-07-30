package com.example.pantrypal

import com.example.pantrypal.data.dao.InventoryWithItemMap
import com.example.pantrypal.data.entity.InventoryEntity
import com.example.pantrypal.util.ExpiryStatus
import com.example.pantrypal.util.InventoryFilter
import com.example.pantrypal.util.InventorySort
import com.example.pantrypal.util.ShoppingNeedStatus
import com.example.pantrypal.util.classifyExpiry
import com.example.pantrypal.util.filterAndSortInventory
import com.example.pantrypal.util.reconcileShoppingIngredients
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test

class InventoryShoppingLogicTest {
    private val today = LocalDate.of(2026, 7, 26)
    private val now = today.atTime(12, 0).toInstant(ZoneOffset.UTC).toEpochMilli()

    @Test
    fun `expiry classification distinguishes expired today soon and later`() {
        assertEquals(ExpiryStatus.EXPIRED, classifyExpiry(millis(today.minusDays(1)), now, zoneId = ZoneOffset.UTC))
        assertEquals(ExpiryStatus.TODAY, classifyExpiry(millis(today), now, zoneId = ZoneOffset.UTC))
        assertEquals(ExpiryStatus.DUE_SOON, classifyExpiry(millis(today.plusDays(7)), now, zoneId = ZoneOffset.UTC))
        assertEquals(ExpiryStatus.LATER, classifyExpiry(millis(today.plusDays(8)), now, zoneId = ZoneOffset.UTC))
        assertEquals(ExpiryStatus.NO_DATE, classifyExpiry(null, now, zoneId = ZoneOffset.UTC))
    }

    @Test
    fun `inventory filtering combines query location low stock and sorting`() {
        val items = listOf(
            inventory(name = "Milk", category = "Dairy", location = InventoryEntity.LOCATION_FRIDGE, quantity = 1.0, threshold = 1.0),
            inventory(name = "Pasta", category = "General", location = InventoryEntity.LOCATION_PANTRY, quantity = 4.0, threshold = 1.0),
            inventory(name = "Yoghurt", category = "Dairy", location = InventoryEntity.LOCATION_FRIDGE, quantity = 3.0, threshold = 1.0)
        )

        val result = filterAndSortInventory(
            items,
            InventoryFilter(
                category = "Dairy",
                location = InventoryEntity.LOCATION_FRIDGE,
                lowStockOnly = true,
                sort = InventorySort.NAME
            ),
            now
        )

        assertEquals(listOf("Milk"), result.map { it.name })
    }

    @Test
    fun `expiry sort keeps undated stock last`() {
        val items = listOf(
            inventory(name = "Undated", expiration = null),
            inventory(name = "Later", expiration = millis(today.plusDays(5))),
            inventory(name = "Sooner", expiration = millis(today.plusDays(1)))
        )

        val result = filterAndSortInventory(items, InventoryFilter(sort = InventorySort.EXPIRY), now)

        assertEquals(listOf("Sooner", "Later", "Undated"), result.map { it.name })
    }

    @Test
    fun `shopping reconciliation separates missing available and uncertain stock`() {
        val inventory = listOf(
            inventory(name = "Milk", quantity = 2.0),
            inventory(name = "Onion", quantity = 0.5)
        )

        val result = reconcileShoppingIngredients(
            listOf("Milk", "2 onions", "Bread", "milk"),
            inventory
        ).associateBy { it.normalizedName }

        assertEquals(3, result.size)
        assertEquals(ShoppingNeedStatus.ALREADY_AT_HOME, result.getValue("milk").status)
        assertEquals(ShoppingNeedStatus.CHECK_STOCK, result.getValue("onion").status)
        assertEquals(ShoppingNeedStatus.NEED_TO_BUY, result.getValue("bread").status)
    }

    @Test
    fun `shopping reconciliation converts units and understands trailing counts`() {
        val inventory = listOf(
            inventory(name = "Milk", quantity = 1.0, unit = "l"),
            inventory(name = "Flour", quantity = 1_000.0, unit = "g"),
            inventory(name = "Pizzas", quantity = 2.0)
        )

        val result = reconcileShoppingIngredients(
            listOf("500 ml milk", "2 kg flour", "Pizzas x2"),
            inventory
        ).associateBy { it.normalizedName }

        assertEquals(ShoppingNeedStatus.ALREADY_AT_HOME, result.getValue("milk").status)
        assertEquals(1_000.0, result.getValue("milk").availableQuantity, 0.0)
        assertEquals(ShoppingNeedStatus.CHECK_STOCK, result.getValue("flour").status)
        assertEquals(ShoppingNeedStatus.ALREADY_AT_HOME, result.getValue("pizza").status)
        assertEquals(2.0, result.getValue("pizza").requiredQuantity, 0.0)
    }

    private fun millis(date: LocalDate): Long =
        date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()

    private fun inventory(
        name: String,
        category: String = "General",
        location: String = InventoryEntity.LOCATION_PANTRY,
        quantity: Double = 1.0,
        unit: String = "pcs",
        threshold: Double? = null,
        expiration: Long? = null
    ) = InventoryWithItemMap(
        inventoryId = name.hashCode().toLong(),
        itemId = name.hashCode().toLong(),
        quantity = quantity,
        unit = unit,
        addedDate = now,
        expirationDate = expiration,
        storageLocation = location,
        isOpened = false,
        name = name,
        barcode = null,
        defaultUnit = unit,
        category = category,
        isVegetarian = false,
        isGlutenFree = false,
        isUsual = threshold != null,
        lowStockThreshold = threshold,
        imageUrl = null,
        createdAt = now
    )
}
