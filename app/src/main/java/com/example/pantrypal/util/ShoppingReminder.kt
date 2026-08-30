package com.example.pantrypal.util

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

data class ShoppingReminderCopy(
    val title: String,
    val message: String
)

object ShoppingReminderCopybook {
    private val variations = listOf(
        "Shopping side quest?" to
            "Still shopping tomorrow around %s? Give your list a little refresh before the trolley rolls out.",
        "Trolley check-in" to
            "Is tomorrow a shopping day around %s? Want to tidy the list while the kettle is on?",
        "Cart captain, assemble!" to
            "Still heading out tomorrow around %s? Shall we freshen your list?",
        "A tiny list nudge" to
            "Shopping tomorrow around %s? Your list is ready for a quick pre-shop spritz."
    )

    fun forDate(
        date: LocalDate,
        shoppingTime: String,
        outstandingItemCount: Int? = null
    ): ShoppingReminderCopy {
        val index = Math.floorMod(date.toEpochDay(), variations.size.toLong()).toInt()
        val (title, messageTemplate) = variations[index]
        return ShoppingReminderCopy(
            title,
            messageTemplate.format(shoppingTime) + outstandingSummary(outstandingItemCount)
        )
    }

    fun forTiming(
        date: LocalDate,
        shoppingTime: String,
        timing: ShoppingReminderTiming,
        outstandingItemCount: Int? = null
    ): ShoppingReminderCopy = when (timing) {
        ShoppingReminderTiming.NIGHT_BEFORE ->
            forDate(date, shoppingTime, outstandingItemCount)
        ShoppingReminderTiming.MORNING_OF -> ShoppingReminderCopy(
            "Shopping day check-in",
            "Shopping around $shoppingTime today? Your list is ready for one last look." +
                outstandingSummary(outstandingItemCount)
        )
        ShoppingReminderTiming.HOUR_BEFORE -> ShoppingReminderCopy(
            "Nearly shopping time",
            "Your shop is around $shoppingTime. Open your list for a quick trolley check." +
                outstandingSummary(outstandingItemCount)
        )
    }

    private fun outstandingSummary(count: Int?): String =
        count?.takeIf { it > 0 }?.let {
            " You have $it item${if (it == 1) "" else "s"} still to gather."
        }.orEmpty()
}

object ShoppingReminderSchedule {
    const val REMINDER_HOUR = 20
    const val MORNING_REMINDER_HOUR = 8

    fun shouldNotify(outstandingItemCount: Int): Boolean = outstandingItemCount > 0

    fun nextReminderAt(
        now: ZonedDateTime,
        shoppingDayOfWeek: Int,
        shoppingTimeMinutes: Int,
        timing: ShoppingReminderTiming = ShoppingReminderTiming.NIGHT_BEFORE
    ): ZonedDateTime {
        val shoppingDay = DayOfWeek.of(shoppingDayOfWeek.coerceIn(1, 7))

        for (daysAhead in 0..7) {
            val shoppingDate = now.toLocalDate().plusDays(daysAhead.toLong())
            if (shoppingDate.dayOfWeek != shoppingDay) continue

            val shoppingTime = LocalTime.of(
                shoppingTimeMinutes.coerceIn(0, 23 * 60 + 59) / 60,
                shoppingTimeMinutes.coerceIn(0, 23 * 60 + 59) % 60
            )
            val candidate = when (timing) {
                ShoppingReminderTiming.NIGHT_BEFORE -> ZonedDateTime.of(
                    shoppingDate.minusDays(1), LocalTime.of(REMINDER_HOUR, 0), now.zone
                )
                ShoppingReminderTiming.MORNING_OF -> ZonedDateTime.of(
                    shoppingDate, LocalTime.of(MORNING_REMINDER_HOUR, 0), now.zone
                )
                ShoppingReminderTiming.HOUR_BEFORE -> ZonedDateTime.of(
                    shoppingDate, shoppingTime, now.zone
                ).minusHours(1)
            }
            if (candidate.isAfter(now)) return candidate
        }

        // The loop always finds the next weekly occurrence. This fallback keeps the
        // function total if the calendar rules ever change unexpectedly.
        return now.plusDays(7).withHour(REMINDER_HOUR).withMinute(0).withSecond(0).withNano(0)
    }

    fun formatShoppingTime(minutesSinceMidnight: Int, locale: Locale = Locale.getDefault()): String {
        val safeMinutes = minutesSinceMidnight.coerceIn(0, 23 * 60 + 59)
        return LocalTime.of(safeMinutes / 60, safeMinutes % 60)
            .format(DateTimeFormatter.ofPattern("h:mm a", locale))
    }

    fun formatShoppingDay(dayOfWeek: Int, locale: Locale = Locale.getDefault()): String =
        DayOfWeek.of(dayOfWeek.coerceIn(1, 7)).getDisplayName(TextStyle.FULL, locale)
}
