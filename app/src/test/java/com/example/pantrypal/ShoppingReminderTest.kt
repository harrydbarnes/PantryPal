package com.example.pantrypal

import com.example.pantrypal.util.ShoppingReminderCopybook
import com.example.pantrypal.util.ShoppingReminderSchedule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.ZonedDateTime

class ShoppingReminderTest {
    private val london = ZoneId.of("Europe/London")

    @Test
    fun schedulesAtEightPmTheNightBeforeTheUsualShop() {
        val now = ZonedDateTime.of(2026, 8, 10, 12, 0, 0, 0, london)

        val result = ShoppingReminderSchedule.nextReminderAt(
            now = now,
            shoppingDayOfWeek = DayOfWeek.SATURDAY.value,
            shoppingTimeMinutes = 10 * 60
        )

        assertEquals(
            ZonedDateTime.of(2026, 8, 14, 20, 0, 0, 0, london),
            result
        )
    }

    @Test
    fun rollsToTheFollowingWeekAfterThisWeeksNudgeHasPassed() {
        val now = ZonedDateTime.of(2026, 8, 14, 20, 1, 0, 0, london)

        val result = ShoppingReminderSchedule.nextReminderAt(
            now = now,
            shoppingDayOfWeek = DayOfWeek.SATURDAY.value,
            shoppingTimeMinutes = 10 * 60
        )

        assertEquals(
            ZonedDateTime.of(2026, 8, 21, 20, 0, 0, 0, london),
            result
        )
    }

    @Test
    fun reminderCopybookVariesByDateButIsStableForRetries() {
        val first = ShoppingReminderCopybook.forDate(
            date = java.time.LocalDate.of(2026, 8, 10),
            shoppingTime = "10:00 AM"
        )
        val sameDateRetry = ShoppingReminderCopybook.forDate(
            date = java.time.LocalDate.of(2026, 8, 10),
            shoppingTime = "10:00 AM"
        )
        val nextWeek = ShoppingReminderCopybook.forDate(
            date = java.time.LocalDate.of(2026, 8, 17),
            shoppingTime = "10:00 AM"
        )

        assertEquals(first, sameDateRetry)
        assertNotEquals(first, nextWeek)
        assertTrue(first.message.contains("10:00 AM"))
    }
}
