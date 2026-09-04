package com.example.pantrypal.util

import java.util.Locale

/** Returns only new names, treating case and repeated whitespace as equivalent. */
fun onboardingNamesMissingFrom(existingNames: Collection<String>, incomingNames: Collection<String>): List<String> {
    val knownNames = existingNames.mapTo(mutableSetOf(), ::normalizeOnboardingName)
    return incomingNames.map(String::trim).filter { name ->
        name.isNotEmpty() && knownNames.add(normalizeOnboardingName(name))
    }
}

fun normalizeOnboardingName(name: String): String =
    name.trim().replace(Regex("\\s+"), " ").lowercase(Locale.ROOT)
