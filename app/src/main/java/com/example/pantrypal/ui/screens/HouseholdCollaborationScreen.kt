package com.example.pantrypal.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.pantrypal.ui.BarcodeScanner
import com.example.pantrypal.ui.components.PantryPalSpacing
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.text.DateFormat
import java.util.Date

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
    val liveSyncing: Boolean = false
)

private enum class HouseholdShareStep { OVERVIEW, CHOOSE, SHARE, JOIN }

/** A friendly local-first hand-off. The actual data travels through Android's share sheet. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HouseholdCollaborationScreen(
    state: HouseholdSyncUiState,
    onShareSnapshot: () -> Unit,
    onImportSnapshot: () -> Unit,
    onGoogleSignIn: () -> Unit = {},
    onCreateLiveHousehold: () -> Unit = {},
    onJoinLiveHousehold: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    showTopBar: Boolean = false
) {
    var step by remember { mutableStateOf(HouseholdShareStep.OVERVIEW) }
    val pairingCode = remember(state.liveInvite, state.householdName) {
        state.liveInvite?.let { "PANTRYPAL-LIVE|$it" } ?: householdPairingCode(state.householdName)
    }
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            if (showTopBar) {
                TopAppBar(
                    title = { Text(if (step == HouseholdShareStep.OVERVIEW) "Household" else "Set up household") },
                    navigationIcon = {
                        TextButton(onClick = {
                            if (step == HouseholdShareStep.OVERVIEW) onBack?.invoke()
                            else step = HouseholdShareStep.OVERVIEW
                        }) { Text("Back") }
                    }
                )
            }
        }
    ) { contentPadding ->
        when (step) {
            HouseholdShareStep.OVERVIEW -> HouseholdOverview(state, { step = HouseholdShareStep.CHOOSE }, onGoogleSignIn, onCreateLiveHousehold, Modifier.padding(contentPadding))
            HouseholdShareStep.CHOOSE -> SetupChoice({ step = HouseholdShareStep.SHARE }, { step = HouseholdShareStep.JOIN }, Modifier.padding(contentPadding))
            HouseholdShareStep.SHARE -> ShareSetupCode(pairingCode, state.isWorking, onShareSnapshot, Modifier.padding(contentPadding))
            HouseholdShareStep.JOIN -> JoinSetupCode(onImportSnapshot, onJoinLiveHousehold, Modifier.padding(contentPadding))
        }
    }
}

@Composable
private fun HouseholdOverview(
    state: HouseholdSyncUiState,
    onSetUp: () -> Unit,
    onGoogleSignIn: () -> Unit,
    onCreateLiveHousehold: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(PantryPalSpacing.md),
        verticalArrangement = Arrangement.spacedBy(PantryPalSpacing.md)
    ) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Row(Modifier.padding(PantryPalSpacing.md), horizontalArrangement = Arrangement.spacedBy(PantryPalSpacing.sm), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Group, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    Column(Modifier.weight(1f)) {
                        Text(state.householdName, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(
                            when {
                                state.liveHouseholdId != null -> "Live sync is on${if (state.liveSyncing) "…" else ""}"
                                state.signedIn -> "Signed in as ${state.accountName ?: "Google account"}"
                                else -> "This device only"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
        item {
            Text("Keep the kitchen in step", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.size(6.dp))
            Text("Send a setup copy to another phone or tablet. You can review it before anything on that device changes.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (!state.signedIn) {
            item {
                Button(onClick = onGoogleSignIn, modifier = Modifier.fillMaxWidth()) {
                    Text("Sign in with Google for live sync")
                }
            }
        } else if (state.liveHouseholdId == null) {
            item {
                Button(onClick = onCreateLiveHousehold, modifier = Modifier.fillMaxWidth()) {
                    Text("Create live household")
                }
            }
        }
        item {
            Button(onClick = onSetUp, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Group, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Set up household")
            }
        }
        item {
            Text("One-off copy", style = MaterialTheme.typography.titleMedium)
            Text("Changes will not sync automatically. Live household sync can be added later without changing this hand-off.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item { SnapshotTime("Last shared", state.lastSharedAtEpochMs) }
        item { SnapshotTime("Last imported", state.lastImportedAtEpochMs) }
        state.message?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.primary) } }
    }
}

@Composable
private fun SetupChoice(onShare: () -> Unit, onJoin: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize().padding(PantryPalSpacing.md), verticalArrangement = Arrangement.spacedBy(PantryPalSpacing.md)) {
        Text("Share PantryPal with another device", style = MaterialTheme.typography.headlineSmall)
        Text("Choose what you are doing on this device.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Card(modifier = Modifier.fillMaxWidth(), onClick = onShare) {
            Row(Modifier.padding(PantryPalSpacing.md), horizontalArrangement = Arrangement.spacedBy(PantryPalSpacing.sm), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Share, contentDescription = null)
                Column {
                    Text("Share a setup code", fontWeight = FontWeight.SemiBold)
                    Text("Show a QR code and send a setup copy.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        Card(modifier = Modifier.fillMaxWidth(), onClick = onJoin) {
            Row(Modifier.padding(PantryPalSpacing.md), horizontalArrangement = Arrangement.spacedBy(PantryPalSpacing.sm), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.QrCode2, contentDescription = null)
                Column {
                    Text("Join with a code", fontWeight = FontWeight.SemiBold)
                    Text("Scan the QR code, then choose the shared copy.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        Text("One-off copy. Changes will not sync automatically.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ShareSetupCode(code: String, isWorking: Boolean, onShareCopy: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(PantryPalSpacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(PantryPalSpacing.md)
    ) {
        Text("Share a setup code", style = MaterialTheme.typography.headlineSmall)
        Text("On the other device, scan this code or enter the words.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        PairingQrCode(code)
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
            Text(code.removePrefix("PANTRYPAL-"), modifier = Modifier.padding(PantryPalSpacing.md), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        Button(onClick = onShareCopy, enabled = !isWorking, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.UploadFile, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Send setup copy")
        }
        Text("The setup copy contains your pantry, shopping list, meal plan, recipes and preferences.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Send it only through an app or service you trust.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun JoinSetupCode(
    onImportSnapshot: () -> Unit,
    onJoinLiveHousehold: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var scannedCode by remember { mutableStateOf<String?>(null) }
    var manualCode by remember { mutableStateOf("") }
    var showScanner by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> showScanner = granted }
    if (showScanner) {
        Box(modifier.fillMaxSize()) {
            BarcodeScanner(viewfinderAspectRatio = 1f, onBarcodeDetected = { value ->
                if (value.startsWith("PANTRYPAL-")) {
                    scannedCode = value
                    showScanner = false
                }
            })
            TextButton(onClick = { showScanner = false }, modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp)) { Text("Cancel") }
        }
        return
    }
    Column(
        modifier = modifier.fillMaxSize().padding(PantryPalSpacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(PantryPalSpacing.md)
    ) {
        Text("Join with a code", style = MaterialTheme.typography.headlineSmall)
        Text("Scan the code on the sharing device to join live sync, or use a one-off copy below.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedButton(onClick = {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) showScanner = true
            else cameraPermission.launch(Manifest.permission.CAMERA)
        }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.QrCode2, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Scan QR code")
        }
        OutlinedTextField(
            value = manualCode,
            onValueChange = { manualCode = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Or enter the six words") },
            singleLine = true
        )
        TextButton(onClick = { if (manualCode.trim().isNotBlank()) scannedCode = manualCode.trim() }) {
            Text("Use entered code")
        }
        scannedCode?.let { code ->
            if (code.startsWith("PANTRYPAL-LIVE|")) {
                Text("Live household recognised", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                Button(
                    onClick = { onJoinLiveHousehold(code.removePrefix("PANTRYPAL-LIVE|")) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Join live household") }
            } else {
                Text("Setup code recognised", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            }
        }
        HorizontalDivider()
        Button(onClick = onImportSnapshot, enabled = scannedCode != null && !scannedCode!!.startsWith("PANTRYPAL-LIVE|"), modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.FileOpen, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Choose shared copy")
        }
        Text("You will review the copy before it replaces anything on this device.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PairingQrCode(code: String) {
    val bitmap = remember(code) {
        val matrix = QRCodeWriter().encode(code, BarcodeFormat.QR_CODE, 480, 480)
        Bitmap.createBitmap(matrix.width, matrix.height, Bitmap.Config.ARGB_8888).also { bitmap ->
            for (x in 0 until matrix.width) for (y in 0 until matrix.height) bitmap.setPixel(x, y, if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
        }
    }
    Image(bitmap.asImageBitmap(), contentDescription = "Household setup QR code", modifier = Modifier.size(240.dp))
}

private fun householdPairingCode(householdName: String): String {
    val words = listOf("maple", "river", "lemon", "brick", "frost", "note", "orbit", "meadow", "pepper", "harbour", "copper", "willow")
    val seed = (householdName.hashCode().toLong() xor System.currentTimeMillis() / 86_400_000L).toInt()
    return "PANTRYPAL-" + (0 until 6).joinToString("-") { words[kotlin.math.abs(seed + it * 17) % words.size] }
}

@Composable
private fun SnapshotTime(label: String, epochMs: Long?) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(epochMs?.let { DateFormat.getDateTimeInstance().format(Date(it)) } ?: "Not yet", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
