package com.example.pantrypal.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import android.app.RemoteInput
import com.example.pantrypal.MainActivity
import com.example.pantrypal.PantryPalApplication
import com.example.pantrypal.R
import com.example.pantrypal.data.entity.ShoppingItemEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class PantryPalWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        updateWidgets(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_QUICK_ADD) {
            val name = RemoteInput.getResultsFromIntent(intent)
                ?.getCharSequence(QUICK_ADD_RESULT_KEY)
                ?.toString()
                ?.trim()
                .orEmpty()
            if (name.isNotEmpty()) {
                CoroutineScope(Dispatchers.IO).launch {
                    val app = context.applicationContext as PantryPalApplication
                    app.database.shoppingDao().insertShoppingItem(ShoppingItemEntity(name = name))
                    updateWidgets(context)
                }
            }
        }
    }

    companion object {
        private const val ACTION_QUICK_ADD = "com.example.pantrypal.widget.QUICK_ADD"
        private const val QUICK_ADD_RESULT_KEY = "quick_add_item"
        private const val EXPIRING_WINDOW_DAYS = 7L

        fun updateWidgets(context: Context) {
            CoroutineScope(Dispatchers.IO).launch {
                val app = context.applicationContext as PantryPalApplication
                val database = app.database
                val openCount = database.shoppingDao().countOpenShoppingItems()
                val expiring = database.inventoryDao().getExpiringItemsSnapshot(
                    System.currentTimeMillis() + TimeUnit.DAYS.toMillis(EXPIRING_WINDOW_DAYS),
                    3
                )
                val manager = AppWidgetManager.getInstance(context)
                val component = ComponentName(context, PantryPalWidgetProvider::class.java)
                manager.getAppWidgetIds(component).forEach { widgetId ->
                    manager.updateAppWidget(widgetId, createViews(context, openCount, expiring.map { it.name }))
                }
            }
        }

        private fun createViews(context: Context, openCount: Int, expiringNames: List<String>): RemoteViews {
            return RemoteViews(context.packageName, R.layout.pantry_pal_widget).apply {
                setTextViewText(
                    R.id.widget_shopping_count,
                    "$openCount item${if (openCount == 1) "" else "s"} to buy"
                )
                setTextViewText(
                    R.id.widget_expiring,
                    if (expiringNames.isEmpty()) "Nothing expiring in the next 7 days" else
                        "Expiring soon: ${expiringNames.joinToString(", ")}"
                )
                setOnClickPendingIntent(R.id.widget_title, openAppIntent(context))
                setOnClickPendingIntent(R.id.widget_shopping_count, openAppIntent(context))
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    setOnClickPendingIntent(R.id.widget_quick_add, quickAddIntent(context))
                    setRemoteInputs(
                        R.id.widget_quick_add,
                        arrayOf(
                            RemoteInput.Builder(QUICK_ADD_RESULT_KEY)
                                .setLabel(context.getString(R.string.widget_quick_add_hint))
                                .build()
                        )
                    )
                } else {
                    setOnClickPendingIntent(R.id.widget_quick_add, openAppIntent(context))
                }
            }
        }

        private fun openAppIntent(context: Context): PendingIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        private fun quickAddIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
                context,
                2,
                Intent(context, PantryPalWidgetProvider::class.java).setAction(ACTION_QUICK_ADD),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
    }
}
