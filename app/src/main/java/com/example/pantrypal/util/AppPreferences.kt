package com.example.pantrypal.util

import android.content.Context

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK;

    companion object {
        fun fromStoredValue(value: String?): AppThemeMode =
            entries.firstOrNull { it.name == value } ?: SYSTEM
    }
}

enum class ShoppingReminderTiming {
    NIGHT_BEFORE,
    MORNING_OF,
    HOUR_BEFORE;

    companion object {
        fun fromStoredValue(value: String?): ShoppingReminderTiming =
            entries.firstOrNull { it.name == value } ?: NIGHT_BEFORE
    }
}

data class AppSettings(
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val dynamicColorEnabled: Boolean = true,
    val expiryRemindersEnabled: Boolean = true,
    val shoppingRemindersEnabled: Boolean = false,
    val shoppingDayOfWeek: Int = AppPreferences.DEFAULT_SHOPPING_DAY,
    val shoppingTimeMinutes: Int = AppPreferences.DEFAULT_SHOPPING_TIME_MINUTES,
    val shoppingReminderTiming: ShoppingReminderTiming = ShoppingReminderTiming.NIGHT_BEFORE,
    val nearbyShoppingRemindersEnabled: Boolean = false
)

object AppPreferences {
    const val FILE_NAME = "pantry_prefs"
    const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"
    const val KEY_MEAL_PLAN_INTRO_SEEN = "meal_plan_intro_seen"
    const val KEY_SETTINGS_INTRO_SEEN = "settings_intro_seen"
    const val KEY_THEME_MODE = "theme_mode"
    const val KEY_DYNAMIC_COLOR = "dynamic_color"
    const val KEY_EXPIRY_REMINDERS = "expiry_reminders"
    const val KEY_SHOPPING_REMINDERS = "shopping_reminders"
    const val KEY_SHOPPING_DAY = "shopping_day"
    const val KEY_SHOPPING_TIME = "shopping_time"
    const val KEY_SHOPPING_REMINDER_TIMING = "shopping_reminder_timing"
    const val KEY_NEARBY_SHOPPING_REMINDERS = "nearby_shopping_reminders"

    const val DEFAULT_SHOPPING_DAY = 6 // Saturday
    const val DEFAULT_SHOPPING_TIME_MINUTES = 10 * 60

    fun readSettings(context: Context): AppSettings {
        val preferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
        return AppSettings(
            themeMode = AppThemeMode.fromStoredValue(
                preferences.getString(KEY_THEME_MODE, AppThemeMode.SYSTEM.name)
            ),
            dynamicColorEnabled = preferences.getBoolean(KEY_DYNAMIC_COLOR, true),
            expiryRemindersEnabled = preferences.getBoolean(KEY_EXPIRY_REMINDERS, true),
            shoppingRemindersEnabled = preferences.getBoolean(KEY_SHOPPING_REMINDERS, false),
            shoppingDayOfWeek = preferences.getInt(
                KEY_SHOPPING_DAY,
                DEFAULT_SHOPPING_DAY
            ).coerceIn(1, 7),
            shoppingTimeMinutes = preferences.getInt(
                KEY_SHOPPING_TIME,
                DEFAULT_SHOPPING_TIME_MINUTES
            ).coerceIn(0, 23 * 60 + 59),
            shoppingReminderTiming = ShoppingReminderTiming.fromStoredValue(
                preferences.getString(KEY_SHOPPING_REMINDER_TIMING, null)
            ),
            nearbyShoppingRemindersEnabled = preferences.getBoolean(
                KEY_NEARBY_SHOPPING_REMINDERS,
                false
            )
        )
    }
}
