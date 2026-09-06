package com.example.pantrypal

import com.example.pantrypal.data.household.*
import com.example.pantrypal.util.ShoppingAisles
import org.junit.Assert.*
import org.junit.Test

class ShoppingRecordProtocolTest {
    @Test fun independentEditsSurvive() {
        val a = ShoppingRecordProtocol.commit(ShoppingWireState(), "phone", "a", mapOf("milk" to ShoppingRecord("1", "checked")))
        val b = ShoppingRecordProtocol.commit(a, "partner", "b", mapOf("bread" to ShoppingRecord("2", "checked")))
        assertEquals(setOf("milk", "bread"), b.records.keys)
    }
    @Test fun lostAcknowledgementCannotReplayAnOldValueOverANewerEdit() {
        val a = ShoppingRecordProtocol.commit(ShoppingWireState(), "phone", "a", mapOf("milk" to ShoppingRecord("1", "unchecked")))
        val b = ShoppingRecordProtocol.commit(a, "partner", "b", mapOf("milk" to ShoppingRecord("2", "checked")))
        assertEquals(b, ShoppingRecordProtocol.commit(b, "phone", "a", mapOf("milk" to ShoppingRecord("1", "unchecked"))))
    }
    @Test fun deletionIsRetainedAsATombstone() {
        val old = ShoppingWireState(records = mapOf("milk" to ShoppingRecord("1", "milk")))
        val deleted = ShoppingRecordProtocol.commit(old, "a", "b", mapOf("milk" to ShoppingRecord("2", null)))
        assertTrue(deleted.records.containsKey("milk")); assertNull(deleted.records["milk"]!!.data)
    }
    @Test fun sameRecordUsesLastAcceptedWrite() {
        val a = ShoppingRecordProtocol.commit(ShoppingWireState(), "a", "1", mapOf("milk" to ShoppingRecord("1", "a")))
        val b = ShoppingRecordProtocol.commit(a, "b", "2", mapOf("milk" to ShoppingRecord("2", "b")))
        assertEquals("b", b.records.getValue("milk").data)
    }
    @Test fun aislesRememberNamesAndRemovedAislesBecomeUnassigned() {
        val map = mapOf(ShoppingAisles.assignmentKey(" MILK ") to "Chilled")
        assertEquals("Chilled", ShoppingAisles.aisleFor("milk", listOf("Chilled"), map))
        assertEquals("Unassigned", ShoppingAisles.aisleFor("milk", listOf("Bakery"), map))
        assertEquals(listOf("Bakery", "Chilled"), ShoppingAisles.cleanOrder(listOf(" Bakery ", "Chilled", "Bakery", "", "Unassigned")))
    }
}
