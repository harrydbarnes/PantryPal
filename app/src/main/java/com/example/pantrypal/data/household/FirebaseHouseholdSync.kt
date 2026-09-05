package com.example.pantrypal.data.household

import android.app.Activity
import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.example.pantrypal.R
import com.example.pantrypal.data.repository.PantryFeaturesRepository
import com.google.android.gms.tasks.Tasks
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.security.SecureRandom
import java.util.UUID

data class FirebaseHouseholdState(
    val signedIn: Boolean = false,
    val accountName: String? = null,
    val householdId: String? = null,
    val invite: String? = null,
    val syncing: Boolean = false,
    val status: String? = null
)

/**
 * Keeps Room as the source used by the UI, and mirrors an encrypted-in-transit
 * snapshot through Firestore while both household devices are open.
 */
class FirebaseHouseholdSync(
    private val context: Context,
    private val repository: PantryFeaturesRepository
) {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val prefs = context.getSharedPreferences("firebase_household", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow(currentState())
    val state: StateFlow<FirebaseHouseholdState> = _state.asStateFlow()
    private var listener: ListenerRegistration? = null
    private var importing = false
    private var publishQueued = false

    init {
        auth.addAuthStateListener {
            _state.value = currentState()
            attachListener()
        }
        attachListener()
    }

    suspend fun signIn(activity: Activity): Result<Unit> = runCatching {
        val credentialManager = CredentialManager.create(activity)
        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(context.getString(R.string.default_web_client_id))
            .setFilterByAuthorizedAccounts(false)
            .setAutoSelectEnabled(false)
            .build()
        val result = credentialManager.getCredential(
            activity,
            GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()
        )
        val googleCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
        auth.signInWithCredential(GoogleAuthProvider.getCredential(googleCredential.idToken, null)).await()
        Unit
    }.onFailure { error ->
        _state.value = currentState().copy(status = if (error is GetCredentialException) "Google sign-in was cancelled." else "Google sign-in failed.")
    }

    fun createHousehold() {
        val user = auth.currentUser ?: run {
            _state.value = currentState().copy(status = "Sign in with Google first.")
            return
        }
        scope.launch {
            val householdId = UUID.randomUUID().toString()
            val inviteCode = List(6) { INVITE_WORDS[random.nextInt(INVITE_WORDS.size)] }.joinToString("-")
            firestore.collection("households").document(householdId).set(
                mapOf("memberIds" to listOf(user.uid), "inviteCode" to inviteCode, "createdAt" to System.currentTimeMillis())
            ).await()
            prefs.edit().putString(KEY_HOUSEHOLD_ID, householdId).putString(KEY_INVITE, "$householdId|${user.uid}|$inviteCode").apply()
            _state.value = currentState().copy(status = "Household created. Share the QR code or six words.")
            attachListener()
            publishNow()
        }
    }

    fun joinHousehold(invite: String) {
        val user = auth.currentUser ?: run {
            _state.value = currentState().copy(status = "Sign in with Google first.")
            return
        }
        val parts = invite.trim().split("|")
        if (parts.size != 3) {
            _state.value = currentState().copy(status = "That household invite is not valid.")
            return
        }
        scope.launch {
            val (householdId, ownerId, code) = parts
            firestore.collection("households").document(householdId).update(
                mapOf("memberIds" to listOf(ownerId, user.uid), "inviteCode" to code)
            ).await()
            prefs.edit().putString(KEY_HOUSEHOLD_ID, householdId).remove(KEY_INVITE).apply()
            _state.value = currentState().copy(status = "Joined household. Syncing the latest kitchen data…")
            attachListener()
        }
    }

    fun onLocalDataChanged() {
        if (importing || auth.currentUser == null || prefs.getString(KEY_HOUSEHOLD_ID, null) == null || publishQueued) return
        publishQueued = true
        scope.launch {
            delay(650)
            publishQueued = false
            publishNow()
        }
    }

    private fun publishNow() {
        val householdId = prefs.getString(KEY_HOUSEHOLD_ID, null) ?: return
        val user = auth.currentUser ?: return
        scope.launch {
            runCatching {
                _state.value = currentState().copy(syncing = true)
                val snapshot = repository.exportHouseholdSnapshot()
                firestore.collection("households").document(householdId).collection("state").document("current")
                    .set(mapOf("snapshot" to snapshot, "updatedBy" to user.uid, "updatedAt" to System.currentTimeMillis()))
                    .await()
            }.onFailure { _state.value = currentState().copy(status = "Could not sync household changes.") }
                .onSuccess { _state.value = currentState().copy(status = "Synced just now.") }
        }
    }

    private fun attachListener() {
        listener?.remove()
        val householdId = prefs.getString(KEY_HOUSEHOLD_ID, null) ?: return
        val uid = auth.currentUser?.uid ?: return
        listener = firestore.collection("households").document(householdId).collection("state").document("current")
            .addSnapshotListener { document, error ->
                if (error != null || document == null || document.getString("updatedBy") == uid) return@addSnapshotListener
                importRemote(document)
            }
    }

    private fun importRemote(document: DocumentSnapshot) {
        val snapshot = document.getString("snapshot") ?: return
        scope.launch {
            importing = true
            _state.value = currentState().copy(syncing = true, status = "Applying household update…")
            repository.importHouseholdSnapshot(snapshot)
                .onSuccess { _state.value = currentState().copy(status = "Updated from your household.") }
                .onFailure { _state.value = currentState().copy(status = "Household update could not be applied.") }
            delay(1_000)
            importing = false
        }
    }

    private fun currentState() = FirebaseHouseholdState(
        signedIn = auth.currentUser != null,
        accountName = auth.currentUser?.displayName ?: auth.currentUser?.email,
        householdId = prefs.getString(KEY_HOUSEHOLD_ID, null),
        invite = prefs.getString(KEY_INVITE, null)
    )

    private companion object {
        const val KEY_HOUSEHOLD_ID = "household_id"
        const val KEY_INVITE = "invite"
        val random = SecureRandom()
        val INVITE_WORDS = listOf("apple", "basil", "copper", "dinner", "ember", "forest", "ginger", "harbour", "indigo", "juniper", "kettle", "lemon", "mango", "noodle", "olive", "pepper", "quartz", "rosemary", "saffron", "thyme", "umber", "violet", "willow", "yarrow")
    }
}
