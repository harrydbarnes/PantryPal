package com.example.pantrypal.util

import com.example.pantrypal.data.entity.MealEntity
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.util.Locale

fun startOfWeek(date: LocalDate): LocalDate =
    date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

fun rotatingWeek(anchorWeek: String, anchorMondayEpochDay: Long, date: LocalDate): String {
    val elapsedWeeks = (startOfWeek(date).toEpochDay() - anchorMondayEpochDay) / 7
    return if (elapsedWeeks.mod(2L) == 0L) anchorWeek else otherWeek(anchorWeek)
}

fun otherWeek(week: String): String =
    if (week == MealEntity.WEEK_A) MealEntity.WEEK_B else MealEntity.WEEK_A

fun dayLabel(dayOfWeek: Int): String =
    DayOfWeek.of(dayOfWeek.coerceIn(1, 7)).getDisplayName(
        java.time.format.TextStyle.FULL,
        Locale.getDefault()
    )

fun normalizedIngredients(raw: List<String>): List<String> = raw
    .map { it.trim() }
    .filter { it.isNotEmpty() }
    .distinctBy { it.lowercase(Locale.getDefault()) }

fun mealsForShopping(meals: List<MealEntity>, week: String): List<String> =
    normalizedIngredients(meals.filter { it.week == week }.flatMap { it.ingredients })
