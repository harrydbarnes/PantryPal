package com.example.pantrypal.widget

import androidx.activity.ComponentActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import com.example.pantrypal.PantryPalApplication
import com.example.pantrypal.R
import androidx.lifecycle.lifecycleScope
import com.example.pantrypal.data.entity.ShoppingItemEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WidgetQuickAddActivity : ComponentActivity() {

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
            lifecycleScope.launch(Dispatchers.IO) {
                val app = application as PantryPalApplication
                app.repository.addShoppingItem(ShoppingItemEntity(name = name))
                app.repository.rememberShoppingItem(name)
                PantryPalWidgetProvider.updateWidgets(applicationContext)
                withContext(Dispatchers.Main) { finish() }
            }
        }
    }
}
