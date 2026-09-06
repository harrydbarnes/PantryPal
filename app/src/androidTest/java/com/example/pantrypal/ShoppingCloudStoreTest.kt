package com.example.pantrypal

import androidx.test.core.app.ApplicationProvider
import com.example.pantrypal.data.household.*
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.*
import org.junit.Assert.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

/** Runs the production Kotlin transport against local Auth + Firestore and the real rules. */
class ShoppingCloudStoreTest {
    private lateinit var app: FirebaseApp
    private lateinit var db: FirebaseFirestore
    private lateinit var cloud: ShoppingCloudStore
    private lateinit var uid: String
    private val home = UUID.randomUUID().toString()
    @Before fun setup() = runBlocking {
        val existing = sharedApp
        if (existing == null) {
            app = FirebaseApp.initializeApp(ApplicationProvider.getApplicationContext(), FirebaseOptions.Builder()
                .setProjectId("demo-pantrypal").setApplicationId("1:123:android:test").setApiKey("test-key").build(), "cloud-tests")
            val auth = FirebaseAuth.getInstance(app); auth.useEmulator("10.0.2.2", 9099)
            db = FirebaseFirestore.getInstance(app)
            db.firestoreSettings = FirebaseFirestoreSettings.Builder().setPersistenceEnabled(false).build()
            db.useEmulator("10.0.2.2", 8080)
            uid = auth.signInAnonymously().await().user!!.uid
            sharedApp = app
        } else {
            app = existing
            db = FirebaseFirestore.getInstance(app)
            uid = FirebaseAuth.getInstance(app).currentUser!!.uid
        }
        cloud = ShoppingCloudStore(db)
        db.collection("households").document(home).set(mapOf("memberIds" to listOf(uid), "inviteCode" to "apple-basil-copper-dinner-ember-forest", "createdAt" to 1L)).await()
        Unit
    }
    companion object {
        private var sharedApp: FirebaseApp? = null
        @JvmStatic @AfterClass fun close() { runBlocking {
            sharedApp?.let { FirebaseFirestore.getInstance(it).terminate().await(); it.delete() }
            sharedApp = null
        } }
    }

    private fun legacySeed(seed: ShoppingWireState) {
        val connection = URL("http://10.0.2.2:8080/v1/projects/demo-pantrypal/databases/(default)/documents/households/$home/state/current").openConnection() as HttpURLConnection
        connection.requestMethod = "PATCH"
        connection.setRequestProperty("Authorization", "Bearer owner") // Emulator-only admin token.
        connection.setRequestProperty("Content-Type", "application/json")
        connection.doOutput = true
        val body = Gson().toJson(mapOf("fields" to mapOf("protocol" to mapOf("integerValue" to "2"), "shoppingV2" to mapOf("stringValue" to Gson().toJson(seed)))))
        connection.outputStream.use { it.write(body.toByteArray()) }
        check(connection.responseCode in 200..299) { connection.errorStream.bufferedReader().readText() }
        connection.disconnect()
    }
    private fun decode(doc: DocumentSnapshot): ShoppingWireState = doc.getString("shoppingV2")?.let { Gson().fromJson(it, ShoppingWireState::class.java) } ?: ShoppingWireState()

    @Test fun legacyMigrationAndLostAcknowledgementDoNotReplayOldEdits() = runBlocking {
        val seed = ShoppingWireState(records = (1..240).associate { "item:$it" to ShoppingRecord("seed", "item $it") }, receipts = mapOf("phone" to "already-accepted"))
        legacySeed(seed)
        // Simulate a process dying immediately after freezing v2.
        db.collection("households").document(home).collection("state").document("current")
            .update(mapOf("protocol" to 3, "phase" to "migrating", "revision" to 0, "updatedBy" to uid, "updatedAt" to FieldValue.serverTimestamp())).await()
        cloud.ensureReady(home, uid, ::decode)
        cloud.ensureReady(home, uid, ::decode)
        val initial = cloud.exchange(home, uid, "phone", "already-accepted", mapOf("item:1" to ShoppingRecord("old", "stale")))
        assertEquals(240, initial.records.size)
        assertEquals("item 1", initial.records["item:1"]!!.data)
        cloud.exchange(home, uid, "phone", "new", mapOf("item:1" to ShoppingRecord("new", "first edit")))
        cloud.exchange(home, uid, "partner", "other", mapOf("item:1" to ShoppingRecord("partner", "later edit")))
        val retry = cloud.exchange(home, uid, "phone", "new", mapOf("item:1" to ShoppingRecord("new", "first edit")))
        assertEquals("later edit", retry.records["item:1"]!!.data)
    }

    @Test fun largeLegacyBatchAndIndependentEditsSurviveRetry() = runBlocking {
        cloud.ensureReady(home, uid, ::decode)
        val changes = (1..205).associate { "item:$it" to ShoppingRecord("$it", "value $it") }
        assertEquals(205, cloud.exchange(home, uid, "phone", "bulk", changes).records.size)
        cloud.exchange(home, uid, "partner", "edit", mapOf("item:1" to ShoppingRecord("partner", "changed")))
        val retried = cloud.exchange(home, uid, "phone", "bulk", changes)
        assertEquals("changed", retried.records["item:1"]!!.data)
        assertEquals("value 205", retried.records["item:205"]!!.data)
    }
}
