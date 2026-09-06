package com.example.pantrypal.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Shared recoverable read and mutation feedback. Keeps existing content visible on failure. */
@Composable
fun DataStatus(loading: Boolean, error: String?, onRetry: () -> Unit, actionError: String?, onDismiss: () -> Unit) {
    if (loading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    if (error != null || actionError != null) {
        Surface(color = MaterialTheme.colorScheme.errorContainer) {
            Column(Modifier.fillMaxWidth().padding(12.dp)) {
                Text(actionError ?: error.orEmpty())
                if (actionError != null) TextButton(onClick = onDismiss) { Text("Dismiss") }
                else TextButton(onClick = onRetry) { Text("Retry") }
            }
        }
    }
}
