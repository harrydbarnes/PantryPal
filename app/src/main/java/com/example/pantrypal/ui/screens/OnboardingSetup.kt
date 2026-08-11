package com.example.pantrypal.ui.screens

data class OnboardingMeal(val name: String, val dayOfWeek: Int)

fun onboardingRegulars(value: String): List<String> =
    value.split(',')
        .map(String::trim)
        .filter(String::isNotEmpty)
        .distinctBy(String::lowercase)

fun onboardingMeals(meals: List<OnboardingMeal>): List<OnboardingMeal> =
    meals.map { it.copy(name = it.name.trim()) }
        .filter { it.name.isNotEmpty() }
        .distinctBy { it.name.lowercase() }
