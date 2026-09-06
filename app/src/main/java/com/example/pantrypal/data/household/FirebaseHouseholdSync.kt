package com.example.pantrypal.data.household

import android.app.Activity
import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.pantrypal.R
import com.example.pantrypal.data.database.KitchenDatabase
import com.example.pantrypal.data.repository.PantryFeaturesRepository
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.*
import com.google.gson.Gson
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import java.security.SecureRandom
import java.util.UUID

data class FirebaseHouseholdState(
    val signedIn: Boolean = false,
    val accountName: String? = null,
    val householdId: String? = null,
    val invite: String? = null,
    val syncing: Boolean = false,
    val working: Boolean = false,
    val lastSyncedAt: Long? = null,
    val status: String? = null,
    val failed: Boolean = false
)

/** Shopping-only protocol v3. Room and its trigger journal survive offline/process death. */
class FirebaseHouseholdSync(
    private val context: Context,
    private val repository: PantryFeaturesRepository,
    database: KitchenDatabase
) {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val prefs = context.getSharedPreferences("firebase_household", Context.MODE_PRIVATE)
    private val store = ShoppingSyncStore(database)
    private val cloud = ShoppingCloudStore(firestore)
    private val gson = Gson()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private val requests = Channel<Unit>(Channel.CONFLATED)
    private val deviceId = prefs.getString("device_id", null) ?: UUID.randomUUID().toString().also {
        check(prefs.edit().putString("device_id", it).commit())
    }
    private val _state = MutableStateFlow(currentState())
    val state = _state.asStateFlow()
    private var listener: ListenerRegistration? = null

    init {
        scope.launch {
            for (ignored in requests) {
                delay(350)
                var backoff = 1_000L
                while (isActive && auth.currentUser != null && householdId() != null) {
                    try {
                        mutex.withLock { syncOnce() }
                        break
                    } catch (cancelled: CancellationException) { throw cancelled
                    } catch (error: Exception) {
                        _state.update { it.copy(syncing = false, failed = true, status = "Changes saved on this device. Sync will retry: ${error.message ?: "connection unavailable"}") }
                        delay(backoff)
                        backoff = (backoff * 2).coerceAtMost(30_000L)
                    }
                }
            }
        }
        auth.addAuthStateListener { scope.launch { mutex.withLock { attachListener() } } }
    }

    private fun householdId(): String? {
        val uid = auth.currentUser?.uid ?: return null
        val owner = prefs.getString("account_uid", null)
        return if (owner == null || owner == uid) prefs.getString("household_id", null) else null
    }

    suspend fun signIn(activity: Activity): Result<Unit> {
        _state.update { it.copy(working = true, failed = false, status = "Signing in…") }
        return try {
            val option = GetGoogleIdOption.Builder().setServerClientId(context.getString(R.string.default_web_client_id))
                .setFilterByAuthorizedAccounts(false).setAutoSelectEnabled(false).build()
            val result = CredentialManager.create(activity).getCredential(activity, GetCredentialRequest.Builder().addCredentialOption(option).build())
            val credential = GoogleIdTokenCredential.createFrom(result.credential.data)
            auth.signInWithCredential(GoogleAuthProvider.getCredential(credential.idToken, null)).await()
            _state.value = currentState().copy(status = "Signed in. Create or join a shopping household.")
            Result.success(Unit)
        } catch (cancelled: CancellationException) { throw cancelled
        } catch (error: Exception) {
            _state.value = currentState().copy(failed = error !is GetCredentialCancellationException,
                status = if (error is GetCredentialCancellationException) "Sign-in cancelled." else "Google sign-in failed. Check your connection and try again.")
            Result.failure(error)
        }
    }

    private fun operation(message: String, block: suspend () -> Unit) {
        if (_state.value.working) return
        _state.update { it.copy(working = true, failed = false, status = message) }
        scope.launch {
            try { mutex.withLock { block() } }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { _state.update { it.copy(failed = true, status = error.message ?: "Household action failed. Try again.") } }
            finally { _state.update { it.copy(working = false) } }
        }
    }

    fun createHousehold() = operation("Creating shopping household…") {
        val uid = auth.currentUser?.uid ?: error("Sign in with Google first.")
        check(householdId() == null) { "This device already has a household." }
        val id = UUID.randomUUID().toString()
        val code = List(6) { WORDS[SecureRandom().nextInt(WORDS.size)] }.joinToString("-")
        firestore.collection("households").document(id).set(mapOf("memberIds" to listOf(uid), "inviteCode" to code, "createdAt" to System.currentTimeMillis())).await()
        check(prefs.edit().putString("household_id", id).putString("account_uid", uid).putString("invite", "$id|$uid|$code").commit())
        store.resetQueue()
        store.seedQueue()
        attachListener()
    }

    /** UI confirms shopping-only replacement; preserve a full safety copy before joining. */
    fun joinHousehold(invite: String) = operation("Joining shopping household…") {
        val uid = auth.currentUser?.uid ?: error("Sign in with Google first.")
        check(householdId() == null) { "Leave the current household before joining another." }
        val parts = invite.trim().removePrefix("PANTRYPAL-LIVE|").split('|')
        require(parts.size == 3 && parts.all { it.isNotBlank() }) { "Paste the complete invite or scan its QR code." }
        val (id, _, code) = parts
        repository.exportBackupJson().let { json ->
            java.io.File(context.filesDir, "before-household-join.json").writeText(json)
        }
        // Rules must validate the unchanged invite and preserve existing members.
        firestore.collection("households").document(id).update(FieldPath.of("memberIds"), FieldValue.arrayUnion(uid), FieldPath.of("joinProofs", uid), code).await()
        val document = firestore.collection("households").document(id).collection("state").document("current").get(Source.SERVER).await()
        require(document.exists()) { "The owner needs to open PantryPal and finish its first sync, then retry joining." }
        cloud.ensureReady(id, uid, ::decode)
        val remote = cloud.exchange(id, uid, deviceId, null, emptyMap())
        store.replaceShopping(remote)
        check(prefs.edit().putString("household_id", id).putString("account_uid", uid).remove("invite").commit())
        attachListener()
    }

    fun leaveHousehold() = operation("Disconnecting this device…") {
        listener?.remove(); listener = null
        prefs.edit().remove("household_id").remove("invite").remove("account_uid").commit()
        store.resetQueue()
        _state.value = currentState().copy(status = "This device is disconnected. Your local lists are kept.")
    }

    fun signOut() = operation("Signing out…") {
        listener?.remove(); listener = null
        prefs.edit().remove("household_id").remove("invite").remove("account_uid").commit()
        store.resetQueue()
        auth.signOut()
        _state.value = currentState().copy(status = "Signed out. Local data is kept.")
    }

    fun onLocalDataChanged() {
        if (householdId() != null) {
            _state.update { it.copy(syncing = true) }
            requests.trySend(Unit)
        }
    }
    fun retry() { scope.launch { mutex.withLock { attachListener() } } }

    private fun attachListener() {
        listener?.remove(); listener = null
        _state.value = currentState()
        val id = householdId() ?: return
        prefs.edit().putString("account_uid", auth.currentUser!!.uid).apply()
        listener = firestore.collection("households").document(id).collection("state").document("current")
            .addSnapshotListener { _, error ->
                if (error != null) _state.update { it.copy(failed = true, syncing = false, status = "Cannot receive household updates. Retry after checking your connection or membership.") }
                else requests.trySend(Unit)
            }
        requests.trySend(Unit)
    }

    private fun decode(doc: DocumentSnapshot): ShoppingWireState {
        doc.getString("shoppingV2")?.let { return gson.fromJson(it, ShoppingWireState::class.java).also { state -> require(state.protocol == 2) } }
        doc.getString("snapshot")?.let { return store.upgradeLegacy(it) }
        return ShoppingWireState()
    }

    private suspend fun syncOnce() {
        val id = householdId() ?: return
        val uid = auth.currentUser?.uid ?: return
        _state.update { it.copy(syncing = true, failed = false) }
        val batch = store.batch(id)
        val changes = batch?.let(store::changes).orEmpty()
        cloud.ensureReady(id, uid, ::decode)
        cloud.compact(id, uid)
        val remote = cloud.exchange(id, uid, deviceId, batch?.batchId, changes)
        store.accept(remote, batch, authoritative = true)
        _state.value = currentState().copy(lastSyncedAt = System.currentTimeMillis(), status = "Shopping list synced.")
        if (store.batch(id) != null) requests.trySend(Unit)
    }

    private fun currentState() = FirebaseHouseholdState(
        signedIn = auth.currentUser != null, accountName = auth.currentUser?.displayName ?: auth.currentUser?.email,
        householdId = householdId(), invite = if (householdId() != null) prefs.getString("invite", null) else null
    )
    private companion object {
        val WORDS = listOf("apple", "basil", "copper", "dinner", "ember", "forest", "ginger", "harbour", "indigo", "juniper", "kettle", "lemon", "mango", "noodle", "olive", "pepper", "quartz", "rosemary", "saffron", "thyme", "umber", "violet", "willow", "yarrow")
    }
}
