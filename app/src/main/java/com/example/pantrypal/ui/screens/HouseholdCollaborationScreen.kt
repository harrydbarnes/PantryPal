package com.example.pantrypal.ui.screens

import android.Manifest
import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.example.pantrypal.ui.BarcodeScanner
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class HouseholdSyncUiState(
    val householdName: String = "My household",
    val deviceName: String = "This device",
    val lastSharedAtEpochMs: Long? = null,
    val lastImportedAtEpochMs: Long? = null,
    val isWorking: Boolean = false,
    val message: String? = null,
    val signedIn: Boolean = false,
    val accountName: String? = null,
    val liveHouseholdId: String? = null,
    val liveInvite: String? = null,
    val liveSyncing: Boolean = false,
    val liveWorking: Boolean = false,
    val liveFailed: Boolean = false,
    val lastSyncedAt: Long? = null
) {
    val syncLabel: String get() = when {
        liveHouseholdId == null -> "This device only"
        liveFailed -> "Sync needs attention"
        liveSyncing -> "Syncing shopping changes…"
        lastSyncedAt != null -> "Shopping list synced"
        else -> "Connecting to household…"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HouseholdCollaborationScreen(
    state: HouseholdSyncUiState,
    onShareSnapshot: () -> Unit,
    onImportSnapshot: () -> Unit,
    onGoogleSignIn: () -> Unit = {},
    onCreateLiveHousehold: () -> Unit = {},
    onJoinLiveHousehold: (String) -> Unit = {},
    onRetry: () -> Unit = {},
    onDisconnect: () -> Unit = {},
    onSignOut: () -> Unit = {},
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    showTopBar: Boolean = false
) {
    var invite by rememberSaveable { mutableStateOf("") }
    var showQr by rememberSaveable { mutableStateOf(false) }
    var scanning by rememberSaveable { mutableStateOf(false) }
    var confirmJoin by rememberSaveable { mutableStateOf(false) }
    var confirmDisconnect by rememberSaveable { mutableStateOf(false) }
    var cameraMessage by rememberSaveable { mutableStateOf<String?>(null) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        scanning = it
        cameraMessage = if (it) null else "Camera permission was denied. Paste the full invite below instead."
    }
    BackHandler(scanning) { scanning = false }
    if (scanning) {
        Box(modifier.fillMaxSize()) {
            BarcodeScanner(viewfinderAspectRatio = 1f, onBarcodeDetected = {
                if (it.startsWith("PANTRYPAL-LIVE|")) { invite = it; scanning = false }
                else cameraMessage = "This is not a live household invite."
            })
            TextButton(onClick = { scanning = false }) { Text("Back to household") }
        }
        return
    }
    Scaffold(modifier, topBar = {
        if (showTopBar) TopAppBar(title = { Text("Household") }, navigationIcon = {
            TextButton(onClick = { onBack?.invoke() }) { Text("Back") }
        })
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).imePadding(), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Text("My shopping household", style = MaterialTheme.typography.headlineSmall)
                Text(state.syncLabel, style = MaterialTheme.typography.titleMedium)
                Text("Live sync shares shopping items, sections and My Aldi aisle settings. Pantry, recipes, meal plans and device settings remain on each device.")
            }
            item {
                if (state.liveWorking || state.isWorking || state.liveSyncing) LinearProgressIndicator(Modifier.fillMaxWidth())
                state.message?.let { Text(it, color = if (state.liveFailed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant) }
                cameraMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (state.liveFailed && state.liveHouseholdId != null) TextButton(onClick = onRetry) { Text("Retry sync") }
            }
            if (!state.signedIn) {
                item { Button(onClick = onGoogleSignIn, enabled = !state.liveWorking, modifier = Modifier.fillMaxWidth()) { Text("Sign in with Google") } }
            } else {
                item { Text("Signed in as ${state.accountName ?: "Google account"}") }
                if (state.liveHouseholdId == null) {
                    item { Button(onClick = onCreateLiveHousehold, enabled = !state.liveWorking, modifier = Modifier.fillMaxWidth()) { Text("Create a shopping household") } }
                    item {
                        Text("Or join your partner", style = MaterialTheme.typography.titleMedium)
                        OutlinedButton(onClick = { permission.launch(Manifest.permission.CAMERA) }, enabled = !state.liveWorking) { Text("Scan household QR") }
                        OutlinedTextField(invite, { invite = it }, label = { Text("Complete household invite") }, supportingText = { Text("Paste the full invite beginning PANTRYPAL-LIVE|. The six words alone are not enough.") }, modifier = Modifier.fillMaxWidth())
                        Button(onClick = { confirmJoin = true }, enabled = invite.startsWith("PANTRYPAL-LIVE|") && !state.liveWorking) { Text("Review and join") }
                    }
                } else {
                    state.liveInvite?.let { code ->
                        item {
                            OutlinedButton(onClick = { showQr = !showQr }, modifier = Modifier.fillMaxWidth()) { Text(if (showQr) "Hide invite" else "Share household invite") }
                            if (showQr) {
                                PairingQrCode("PANTRYPAL-LIVE|$code")
                                androidx.compose.foundation.text.selection.SelectionContainer { Text("PANTRYPAL-LIVE|$code", style = MaterialTheme.typography.bodySmall) }
                                Text("Ask your partner to sign in, then scan this code. Or select and copy the complete invite above.")
                            }
                        }
                    }
                    item { OutlinedButton(onClick = { confirmDisconnect = true }, enabled = !state.liveWorking) { Text("Disconnect this device") } }
                }
                item { TextButton(onClick = onSignOut, enabled = !state.liveWorking && state.liveHouseholdId == null) { Text("Sign out") } }
            }
            item {
                HorizontalDivider()
                Text("One-off kitchen copy", style = MaterialTheme.typography.titleMedium)
                Text("Export or import the full kitchen separately. Import is reviewed before replacing data. It is not live sync.")
                OutlinedButton(onClick = onShareSnapshot, enabled = !state.isWorking) { Text("Export kitchen copy") }
                OutlinedButton(onClick = onImportSnapshot, enabled = !state.isWorking && state.liveHouseholdId == null) { Text("Import kitchen copy") }
                if (state.liveHouseholdId != null) Text("Disconnect before importing a full kitchen copy.")
            }
        }
    }
    if (confirmJoin) AlertDialog(onDismissRequest = { confirmJoin = false }, title = { Text("Join this shopping household?") }, text = { Text("Your current shopping list, custom sections and aisle settings will be replaced by the household's list. Pantry, meals, recipes and settings stay here. A full safety backup is saved on this device before joining. Both devices must use this updated PantryPal version.") }, confirmButton = { TextButton(onClick = { confirmJoin = false; onJoinLiveHousehold(invite) }) { Text("Join household") } }, dismissButton = { TextButton(onClick = { confirmJoin = false }) { Text("Cancel") } })
    if (confirmDisconnect) AlertDialog(onDismissRequest = { confirmDisconnect = false }, title = { Text("Disconnect this device?") }, text = { Text("Your local data is kept. Unsent changes will no longer be sent to this household. This does not revoke your Google account's membership on the server.") }, confirmButton = { TextButton(onClick = { confirmDisconnect = false; onDisconnect() }) { Text("Disconnect") } }, dismissButton = { TextButton(onClick = { confirmDisconnect = false }) { Text("Cancel") } })
}

@Composable
private fun PairingQrCode(code: String) {
    val bitmap by produceState<Bitmap?>(null, code) {
        value = withContext(Dispatchers.Default) {
            val matrix = QRCodeWriter().encode(code, BarcodeFormat.QR_CODE, 480, 480)
            val pixels = IntArray(480 * 480) { i -> if (matrix[i % 480, i / 480]) android.graphics.Color.BLACK else android.graphics.Color.WHITE }
            Bitmap.createBitmap(pixels, 480, 480, Bitmap.Config.ARGB_8888)
        }
    }
    bitmap?.let { Image(it.asImageBitmap(), "Household invite QR", Modifier.sizeIn(maxWidth = 240.dp, maxHeight = 240.dp).aspectRatio(1f)) }
        ?: CircularProgressIndicator()
}
