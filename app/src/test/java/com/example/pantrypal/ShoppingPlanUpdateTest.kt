package com.example.pantrypal

import com.example.pantrypal.data.entity.ShoppingItemEntity
import com.example.pantrypal.data.entity.ShoppingSectionEntity
import com.example.pantrypal.util.*
import org.junit.Assert.*
import org.junit.Test

class ShoppingPlanUpdateTest {
    private fun line(quantity: Double = 600.0, unit: String = "g") = ShoppingReconciliationLine(
        "Tomatoes", "tomato", quantity, 0.0, unit, ShoppingNeedStatus.NEED_TO_BUY, quantity
    )
    private fun item(quantity: Double = 600.0, checked: Boolean = false) = ShoppingItemEntity(
        name = "Tomatoes", quantity = quantity, unit = "g", isChecked = checked,
        sectionId = ShoppingSectionEntity.ID_MEAL_PLAN, weekId = "A"
    )
    private fun baseline(item: ShoppingItemEntity) = mapOf(ShoppingMetadata.baseline(item.syncId) to ShoppingMetadata.fingerprint(item))

    @Test fun `rebuild preserves bought identity and only adds remaining quantity`() {
        val bought = item(200.0, true)
        val changes = planShoppingUpdate(listOf(line()), listOf(bought), emptyList(), "A", baseline(bought))
        assertEquals(400.0, changes.single { it.kind == ShoppingChangeKind.ADD }.item.quantity, 0.0)
        assertEquals(bought, changes.single { it.kind == ShoppingChangeKind.KEEP }.item)
    }
    @Test fun `rebuild updates generated item without incrementing or changing identity`() {
        val original = item(200.0)
        val update = planShoppingUpdate(listOf(line()), listOf(original), emptyList(), "A", baseline(original)).single()
        assertEquals(ShoppingChangeKind.UPDATE, update.kind)
        assertEquals(original.syncId, update.item.syncId)
        assertEquals(600.0, update.item.quantity, 0.0)
        val repeated = planShoppingUpdate(listOf(line()), listOf(update.item), emptyList(), "A", baseline(update.item)).single()
        assertEquals(ShoppingChangeKind.KEEP, repeated.kind)
    }
    @Test fun `manual edits remain unless replacement was explicitly chosen`() {
        val original = item(200.0)
        val edited = original.copy(quantity = 800.0)
        assertEquals(edited, planShoppingUpdate(listOf(line()), listOf(edited), emptyList(), "A", baseline(original)).single().item)
        assertEquals(ShoppingChangeKind.UPDATE, planShoppingUpdate(listOf(line()), listOf(edited), emptyList(), "A", baseline(original), true).single().kind)
    }
    @Test fun `legacy list entries remain protected without a baseline`() {
        val original = item(800.0)
        assertEquals(ShoppingChangeKind.KEEP, planShoppingUpdate(emptyList(), listOf(original), emptyList(), "A", emptyMap()).single().kind)
    }
    @Test fun `obsolete unedited entries are removed but purchased items remain`() {
        val original = item()
        assertEquals(ShoppingChangeKind.REMOVE, planShoppingUpdate(emptyList(), listOf(original), emptyList(), "A", baseline(original)).single().kind)
        val bought = original.copy(isChecked = true)
        assertEquals(ShoppingChangeKind.KEEP, planShoppingUpdate(emptyList(), listOf(bought), emptyList(), "A", baseline(original), true).single().kind)
    }
    @Test fun `manual coverage is converted instead of suppressing a whole requirement`() {
        val manual = item(0.2).copy(unit = "kg", sectionId = ShoppingSectionEntity.ID_THE_REST)
        val addition = planShoppingUpdate(listOf(line()), listOf(manual), emptyList(), "A", emptyMap()).single()
        assertEquals(400.0, addition.item.quantity, 0.0)
        val incompatible = manual.copy(unit = "tin", quantity = 1.0)
        assertEquals(600.0, planShoppingUpdate(listOf(line()), listOf(incompatible), emptyList(), "A", emptyMap()).single().item.quantity, 0.0)
    }
    @Test fun `other weeks never cover this weeks requirement`() {
        val otherWeek = item().copy(weekId = "B", sectionId = ShoppingSectionEntity.ID_THE_REST)
        assertEquals(ShoppingChangeKind.ADD, planShoppingUpdate(listOf(line()), listOf(otherWeek), emptyList(), "A", emptyMap()).single().kind)
    }
    @Test fun `quick entry parses count weight decimals and rejects zero`() {
        assertEquals(QuickShoppingEntry("Milk", 2.0, "pcs"), parseQuickShoppingEntry("Milk x2"))
        assertEquals(QuickShoppingEntry("pasta", 500.0, "g"), parseQuickShoppingEntry("500 g pasta"))
        assertEquals(QuickShoppingEntry("milk", 1.5, "l"), parseQuickShoppingEntry("1,5 litres milk"))
        assertNull(parseQuickShoppingEntry("Milk x0"))
        assertNull(parseQuickShoppingEntry(""))
    }
    @Test fun `aisles suggest category and location but respect explicit unassigned`() {
        assertEquals("Chilled", ShoppingAisles.aisleFor("Milk", ShoppingAisles.exampleOrder, emptyMap()))
        assertEquals("Frozen", ShoppingAisles.aisleFor("Peas", ShoppingAisles.exampleOrder, emptyMap(), location = "Freezer"))
        assertEquals("Fruit & veg", ShoppingAisles.aisleFor("Kiwi", ShoppingAisles.exampleOrder, emptyMap(), category = "Fruit"))
        assertEquals(ShoppingAisles.UNASSIGNED, ShoppingAisles.aisleFor("Milk", ShoppingAisles.exampleOrder, mapOf(ShoppingAisles.assignmentKey("Milk") to ShoppingAisles.UNASSIGNED)))
        assertNotEquals(ShoppingAisles.assignmentKey("Milk"), ShoppingAisles.assignmentKey("Milk", "tesco"))
    }
    @Test fun `explicit have it removes a manually edited meal row without replacing other edits`() {
        val original = item(800.0)
        val other = item().copy(name = "Pasta")
        val changes = planShoppingUpdate(listOf(line().copy(buyQuantity = 0.0, explicitDecision = true)),
            listOf(original, other), emptyList(), "A", emptyMap())
        assertEquals(ShoppingChangeKind.REMOVE, changes.first { it.item.syncId == original.syncId }.kind)
        assertEquals(ShoppingChangeKind.KEEP, changes.first { it.item.syncId == other.syncId }.kind)
    }
    @Test fun `explicit reduced quantity updates a manual row and preserves ticks`() {
        val manual = item(800.0)
        val bought = item(100.0, true)
        val changes = planShoppingUpdate(listOf(line().copy(buyQuantity = 300.0, explicitDecision = true)),
            listOf(manual, bought), emptyList(), "A", emptyMap())
        assertEquals(200.0, changes.first { it.item.syncId == manual.syncId }.item.quantity, 0.0)
        assertEquals(bought, changes.first { it.item.syncId == bought.syncId }.item)
    }
}
