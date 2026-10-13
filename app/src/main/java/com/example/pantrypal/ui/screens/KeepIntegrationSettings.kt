package com.example.pantrypal.ui.screens

import android.app.Activity
import android.content.SharedPreferences
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.pantrypal.integration.KeepApiSyncWorker
import com.google.android.gms.auth.api.identity.Identity
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun KeepIntegrationSettings() {
    val context = LocalContext.current
    val prefs = remember(context) { KeepApiSyncWorker.preferences(context) }
    var status by remember { mutableStateOf(prefs.getString("status", KeepApiSyncWorker.UNAVAILABLE).orEmpty()) }
    var enabled by remember { mutableStateOf(prefs.getBoolean("enabled", false)) }
    var email by rememberSaveable { mutableStateOf(prefs.getString("account", "").orEmpty()) }
    var pendingEmail by rememberSaveable { mutableStateOf("") }
    var connecting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    DisposableEffect(prefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            status = prefs.getString("status", KeepApiSyncWorker.UNAVAILABLE).orEmpty(); enabled = prefs.getBoolean("enabled", false)
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    val resolution = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        connecting = false
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            runCatching { Identity.getAuthorizationClient(context).getAuthorizationResultFromIntent(result.data) }
                .onSuccess { if (!it.accessToken.isNullOrBlank()) KeepApiSyncWorker.connect(context, pendingEmail) }
                .onFailure { KeepApiSyncWorker.status(context, "Keep authorisation could not finish. Use Keep → Send → PantryPal or try again.") }
        } else KeepApiSyncWorker.status(context, "Keep authorisation cancelled. Share imports remain available.")
    }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Google Keep & voice shopping", style = MaterialTheme.typography.titleLarge)
            Text(status, style = MaterialTheme.typography.bodyMedium)
            Text("For any Google account: open your Keep shopping note, choose Send, then PantryPal. Unticked items are added to the active shopping week.")
            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Workspace account email") }, singleLine = true,
                enabled = !connecting, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(enabled = !connecting && email.contains('@') && email.none { it.isWhitespace() }, onClick = {
                    pendingEmail = email.trim(); connecting = true
                    scope.launch {
                        try {
                            val result = Identity.getAuthorizationClient(context).authorize(KeepApiSyncWorker.authorisationRequest(pendingEmail)).await()
                            if (result.hasResolution()) resolution.launch(IntentSenderRequest.Builder(requireNotNull(result.pendingIntent)).build())
                            else { KeepApiSyncWorker.connect(context, pendingEmail); connecting = false }
                        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                        catch (_: Exception) { connecting = false; KeepApiSyncWorker.status(context, "Google Keep access is unavailable for this account or app configuration. Use Keep → Send → PantryPal instead.") }
                    }
                }) { Text(if (connecting) "Connecting…" else "Connect Workspace Keep") }
            }
            if (enabled) Row {
                TextButton(onClick = { KeepApiSyncWorker.syncNow(context) }) { Text("Sync now") }
                TextButton(onClick = { KeepApiSyncWorker.disconnect(context) }) { Text("Disconnect") }
            }
            Text("Voice commands require Google Assistant App Actions approval. Gemini access to AppFunctions depends on Google's availability for your device and account.", style = MaterialTheme.typography.bodySmall)
        }
    }
}
