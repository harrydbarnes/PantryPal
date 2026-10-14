package com.example.pantrypal.data.household

import kotlinx.coroutines.sync.Mutex

/** Serialises local restoration with membership changes, including an in-flight join. */
internal object HouseholdMutationGate {
    val mutex = Mutex()
}
