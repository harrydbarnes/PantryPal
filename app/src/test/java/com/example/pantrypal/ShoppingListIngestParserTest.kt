package com.example.pantrypal

import com.example.pantrypal.domain.shopping.*
import org.junit.Assert.*
import org.junit.Test

class ShoppingListIngestParserTest {
    @Test fun keepsUntickedGroceriesAndSkipsTickedRowsAndHeading() {
        val items = ShoppingListIngestParser.parseKeepSharedText("Weekly shopping list\n[ ] Milk\n- Bread\n* Courgettes\n☐ Yoghurt\n[x] Cheese\n[X] Rice\n☑ Wipes\n✓ Apples")
        assertEquals(listOf("Milk", "Bread", "Courgettes", "Yoghurt"), items.map { it.name })
    }
    @Test fun parsesCountsAndMeasuredAmountsAndRejectsZero() {
        val items = ShoppingListIngestParser.parseKeepSharedText("Milk x2\nBread (3)\n500 g pasta\nRice x0\n\n- [ ] Nappies x2")
        assertEquals(listOf(2.0, 3.0, 500.0, 2.0), items.map { it.quantity })
        assertEquals("g", items[2].unit)
        assertEquals("Nappies", items[3].name)
    }
    @Test fun tickedBulletRowsRemainExcluded() {
        assertTrue(ShoppingListIngestParser.parseKeepSharedText("- [x] Milk\n* ☑ Bread\n• ✓ Eggs").isEmpty())
    }
    @Test(expected = IllegalArgumentException::class) fun boundsSharedPayload() {
        ShoppingListIngestParser.parseKeepSharedText("a".repeat(100001))
    }
}
