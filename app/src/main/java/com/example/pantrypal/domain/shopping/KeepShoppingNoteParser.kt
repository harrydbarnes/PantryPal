package com.example.pantrypal.domain.shopping

import com.google.gson.JsonArray
import java.util.Locale

data class KeepShoppingLine(val item: ShoppingIngestItem, val checked: Boolean) {
    val identity: String get() = item.name.trim().lowercase(Locale.UK) + "|" + item.unit.orEmpty()
}

/** Keep has no list-item IDs. Aggregate equal entries so moving ticked rows cannot create new imports. */
object KeepShoppingNoteParser {
    fun parse(listItems: JsonArray): List<KeepShoppingLine> {
        require(listItems.size() <= 1000) { "This Keep list has too many entries." }
        val lines = mutableListOf<KeepShoppingLine>()
        fun add(value: com.google.gson.JsonObject) {
            val text = value.getAsJsonObject("text")?.get("text")?.asString.orEmpty()
            val checked = value.get("checked")?.asBoolean == true
            ShoppingListIngestParser.parseKeepSharedText(text).forEach { lines += KeepShoppingLine(it, checked) }
        }
        listItems.forEach { element ->
            val line = element.asJsonObject
            add(line)
            line.getAsJsonArray("childListItems")?.forEach { add(it.asJsonObject) }
            require(lines.size <= 1000) { "This Keep list has too many entries." }
        }
        return lines.groupBy { it.identity }.values.map { group ->
            val quantity = group.filterNot { it.checked }.sumOf { it.item.quantity }
            KeepShoppingLine(group.first().item.copy(quantity = quantity), checked = quantity == 0.0)
        }
    }
}
