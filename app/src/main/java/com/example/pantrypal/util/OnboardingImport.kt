package com.example.pantrypal.util

/**
 * Returns only new names. Names use the same normalisation as shopping ingredients, so
 * capitals, punctuation, spacing and common singular/plural variants do not create copies.
 */
fun onboardingNamesMissingFrom(existingNames: Collection<String>, incomingNames: Collection<String>): List<String> {
    val knownNames = existingNames.mapTo(mutableSetOf(), ::normalizeOnboardingName)
    return incomingNames.map(String::trim).filter { name ->
        name.isNotEmpty() && knownNames.add(normalizeOnboardingName(name))
    }
}

fun normalizeOnboardingName(name: String): String = normalizeShoppingName(name)
