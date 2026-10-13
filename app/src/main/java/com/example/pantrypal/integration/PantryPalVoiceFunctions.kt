package com.example.pantrypal.integration

import android.content.Context
import androidx.annotation.RequiresApi
import androidx.appfunctions.AppFunction
import androidx.appfunctions.AppFunctionInvalidArgumentException
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint
import com.example.pantrypal.PantryPalApplication
import com.example.pantrypal.data.entity.StoreTag
import com.example.pantrypal.domain.shopping.ShoppingListIngestParser
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ShoppingVoiceEntry {
    suspend fun addShoppingItems(context: Context, items: List<String>, store: String? = null): Int = withContext(Dispatchers.IO) {
        val tag = store?.let { value -> StoreTag.entries.firstOrNull { it.name.equals(value.trim(), true) }
            ?: throw IllegalArgumentException("Choose Tesco, Aldi or Either.") }
        require(items.isNotEmpty() && items.size <= 1000) { "Provide between 1 and 1,000 shopping items." }
        val parsed = ShoppingListIngestParser.parseKeepSharedText(items.joinToString("\n"))
        require(parsed.isNotEmpty()) { "No shopping items were recognised." }
        val app = context.applicationContext as PantryPalApplication
        val prefs = context.getSharedPreferences(com.example.pantrypal.util.AppPreferences.FILE_NAME, Context.MODE_PRIVATE)
        val week = prefs.getString("active_shopping_week", null) ?: prefs.getString("current_week", "A") ?: "A"
        app.database.withTransaction {
            parsed.forEach { app.database.shoppingListDao().upsertShoppingItem(it.name, it.quantity, "VOICE", week, tag, it.unit) }
        }
        parsed.size
    }
}

/** Only the authorised Android system can bind to the generated agent service. */
@RequiresApi(36)
@AppFunctionServiceEntryPoint(serviceName = "PantryPalAgentService", appFunctionXmlFileName = "pantry_pal_voice_functions")
abstract class PantryPalVoiceFunctions : AppFunctionService() {
    /**
     * Adds groceries to the user's currently selected weekly shopping list.
     * Existing unticked entries with compatible measures gain the requested quantity;
     * ticked purchases and entries in other weeks are preserved. Product store preferences
     * and categories are inherited, with UK grocery categories inferred for new products.
     *
     * @param items Grocery names, optionally with quantities such as "Milk x2" or "500 g pasta".
     * @param store Optional Tesco, Aldi or Either preference. Omit this to keep remembered shops.
     * @return The number of shopping entries accepted.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun addShoppingItems(items: List<String>, store: String? = null): Int {
        return try { ShoppingVoiceEntry.addShoppingItems(applicationContext, items, store) }
        catch (error: IllegalArgumentException) { throw AppFunctionInvalidArgumentException(error.message ?: "Check the shopping items.") }
    }
}
