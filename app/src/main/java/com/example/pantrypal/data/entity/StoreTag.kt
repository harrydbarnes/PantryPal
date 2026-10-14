package com.example.pantrypal.data.entity

/** The preferred shop is remembered even when a stock batch runs out. */
enum class StoreTag(val label: String) {
    TESCO("Tesco"), ALDI("Aldi"), EITHER("Either");
    fun next(): StoreTag = when (this) { TESCO -> ALDI; ALDI -> EITHER; EITHER -> TESCO }
}
