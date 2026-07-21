package com.example.pantrypal.util

import com.example.pantrypal.data.entity.MealEntity
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.util.Locale

fun startOfWeek(date: LocalDate): LocalDate =
    date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

fun rotatingWeek(anchorWeek: String, anchorMondayEpochDay: Long, date: LocalDate): String {
    return rotatingWeek(anchorWeek, anchorMondayEpochDay, date, MealEntity.WEEKS)
}

fun rotatingWeek(
    anchorWeek: String,
    anchorMondayEpochDay: Long,
    date: LocalDate,
    weekOrder: List<String>
): String {
    if (weekOrder.isEmpty()) return anchorWeek
    val elapsedWeeks = (startOfWeek(date).toEpochDay() - anchorMondayEpochDay) / 7
    val anchorIndex = weekOrder.indexOf(anchorWeek).takeIf { it >= 0 } ?: 0
    return weekOrder[(anchorIndex + elapsedWeeks).mod(weekOrder.size.toLong()).toInt()]
}

fun otherWeek(week: String): String =
    if (week == MealEntity.WEEK_A) MealEntity.WEEK_B else MealEntity.WEEK_A

fun nextWeek(week: String, weekOrder: List<String>): String {
    if (weekOrder.isEmpty()) return week
    val currentIndex = weekOrder.indexOf(week).takeIf { it >= 0 } ?: 0
    return weekOrder[(currentIndex + 1) % weekOrder.size]
}

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
