package com.example.pantrypal.util

import java.util.Locale

object MealIngredientSelection {
    fun choices(
        selected: List<String>,
        suggestions: List<String>,
        maxSuggestions: Int = 24
    ): List<String> {
        val selectedChoices = selected
            .map(String::trim)
            .filter(String::isNotEmpty)
            .distinctBy { it.lowercase(Locale.ROOT) }
        val selectedKeys = selectedChoices.map { it.lowercase(Locale.ROOT) }.toSet()
        val suggestedChoices = suggestions
            .map(String::trim)
            .filter(String::isNotEmpty)
            .distinctBy { it.lowercase(Locale.ROOT) }
            .filterNot { it.lowercase(Locale.ROOT) in selectedKeys }
            .take(maxSuggestions)
        return selectedChoices + suggestedChoices
    }

    fun toggle(selected: List<String>, ingredient: String): List<String> {
        val trimmed = ingredient.trim()
        if (trimmed.isEmpty()) return selected
        return if (selected.any { it.equals(trimmed, ignoreCase = true) }) {
            selected.filterNot { it.equals(trimmed, ignoreCase = true) }
        } else {
            selected + trimmed
        }
    }

    fun add(selected: List<String>, ingredient: String): List<String> {
        val trimmed = ingredient.trim()
        return if (
            trimmed.isEmpty() ||
                selected.any { it.equals(trimmed, ignoreCase = true) }
        ) {
            selected
        } else {
            selected + trimmed
        }
    }
}
