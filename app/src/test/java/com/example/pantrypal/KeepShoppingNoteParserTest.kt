package com.example.pantrypal

import com.example.pantrypal.domain.shopping.KeepShoppingNoteParser
import com.google.gson.JsonParser
import org.junit.Assert.*
import org.junit.Test

class KeepShoppingNoteParserTest {
    @Test fun reorderedEntriesKeepTheirIdentityAndDuplicateCountsAreAggregated() {
        val first = JsonParser.parseString("""[{"text":{"text":"Milk x2"}}, {"text":{"text":"Bread"}}, {"text":{"text":"milk"}}]""").asJsonArray
        val second = JsonParser.parseString("""[{"text":{"text":"Bread"}}, {"text":{"text":"Milk x2"}}, {"text":{"text":"milk"}}]""").asJsonArray
        val a = KeepShoppingNoteParser.parse(first).associate { it.identity to it.item.quantity }
        val b = KeepShoppingNoteParser.parse(second).associate { it.identity to it.item.quantity }
        assertEquals(a, b); assertEquals(3.0, a.getValue("milk|"), 0.0)
    }
    @Test fun checkedGroupsResetWhileUntickedChildrenRemainAvailable() {
        val json = JsonParser.parseString("""[{"text":{"text":"Milk"},"checked":true}, {"text":{"text":"Bread"},"checked":true,"childListItems":[{"text":{"text":"Wraps (2)"},"checked":false}]}]""").asJsonArray
        val rows = KeepShoppingNoteParser.parse(json)
        assertTrue(rows.first { it.item.name == "Milk" }.checked)
        assertEquals(2.0, rows.first { it.item.name == "Wraps" }.item.quantity, 0.0)
    }
}
