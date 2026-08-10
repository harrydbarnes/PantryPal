package com.example.pantrypal.widget

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import com.example.pantrypal.PantryPalApplication
import com.example.pantrypal.R
import com.example.pantrypal.data.entity.ShoppingItemEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WidgetQuickAddActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_widget_quick_add)

        val nameInput = findViewById<EditText>(R.id.quick_add_name)
        findViewById<Button>(R.id.quick_add_submit).setOnClickListener {
            val name = nameInput.text.toString().trim()
            if (name.isEmpty()) {
                nameInput.error = getString(R.string.widget_quick_add_hint)
                return@setOnClickListener
            }
            CoroutineScope(Dispatchers.IO).launch {
                val app = application as PantryPalApplication
                app.database.shoppingDao().insertShoppingItem(ShoppingItemEntity(name = name))
                PantryPalWidgetProvider.updateWidgets(applicationContext)
                runOnUiThread(::finish)
            }
        }
    }
}
