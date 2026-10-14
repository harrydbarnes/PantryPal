package com.example.pantrypal.integration

import android.accounts.Account
import android.content.Context
import androidx.work.*
import androidx.room.withTransaction
import com.example.pantrypal.PantryPalApplication
import com.example.pantrypal.data.entity.ShoppingIngestEntity
import com.example.pantrypal.domain.shopping.ShoppingListIngestParser
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/** One-way, read-only Keep import for accounts authorised by their Workspace administrator. */
class KeepApiSyncWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val prefs = preferences(applicationContext)
        val email = prefs.getString("account", null)
        if (!prefs.getBoolean("enabled", false) || email.isNullOrBlank()) return@withContext Result.success()
        try {
            val authorisation = Identity.getAuthorizationClient(applicationContext).authorize(authorisationRequest(email)).await()
            if (authorisation.hasResolution() || authorisation.accessToken.isNullOrBlank()) {
                status(applicationContext, "Reconnect your Workspace account to continue importing from Keep.", disable = true)
                return@withContext Result.success()
            }
            val db = (applicationContext as PantryPalApplication).database
            val appPrefs = applicationContext.getSharedPreferences(com.example.pantrypal.util.AppPreferences.FILE_NAME, Context.MODE_PRIVATE)
            val week = appPrefs.getString("active_shopping_week", null) ?: appPrefs.getString("current_week", "A") ?: "A"
            var pageToken: String? = null
            var imported = 0
            var pages = 0
            do {
                require(++pages <= 100) { "Too many Keep notes. Narrow the shopping lists before syncing." }
                val query = "https://keep.googleapis.com/v1/notes?filter=trashed%3Dfalse&pageSize=100" +
                    (pageToken?.let { "&pageToken=" + URLEncoder.encode(it, "UTF-8") } ?: "")
                val connection = URL(query).openConnection() as HttpURLConnection
                val response = try {
                    connection.connectTimeout = 15000; connection.readTimeout = 15000
                    connection.instanceFollowRedirects = false
                    connection.setRequestProperty("Authorization", "Bearer ${authorisation.accessToken}")
                    when (val code = connection.responseCode) {
                        400, 403 -> {
                            status(applicationContext, UNAVAILABLE, disable = true)
                            return@withContext Result.success()
                        }
                        401 -> {
                            status(applicationContext, "Keep access expired. Reconnect your Workspace account, or use Keep → Send → PantryPal.", disable = true)
                            return@withContext Result.success()
                        }
                        429, in 500..599 -> return@withContext Result.retry()
                        in 200..299 -> connection.inputStream.bufferedReader().use { reader ->
                            val text = StringBuilder()
                            val buffer = CharArray(8192)
                            while (true) {
                                val read = reader.read(buffer); if (read < 0) break
                                require(text.length + read <= 2_000_000) { "This Keep response is too large." }
                                text.append(buffer, 0, read)
                            }
                            JsonParser.parseString(text.toString()).asJsonObject
                        }
                        else -> { status(applicationContext, "Keep could not be imported (HTTP $code). Try again or use Keep → Send → PantryPal."); return@withContext Result.failure() }
                    }
                } finally { connection.disconnect() }
                db.withTransaction {
                    response.getAsJsonArray("notes")?.forEach { element ->
                        val note = element.asJsonObject
                        if (note.get("trashed")?.asBoolean == true || note.get("title")?.asString?.contains("Shopping", true) != true) return@forEach
                        val name = note.get("name")?.asString ?: return@forEach
                        val lines = note.getAsJsonObject("body")?.getAsJsonObject("list")?.getAsJsonArray("listItems") ?: return@forEach
                        val prefix = "keep:" + hash(email) + ":" + hash("$name|$week") + ":"
                        val entries = com.example.pantrypal.domain.shopping.KeepShoppingNoteParser.parse(lines)
                        val seen = mutableSetOf<String>()
                        for (line in entries) {
                            val key = prefix + hash(line.identity)
                            seen += key
                            if (line.checked) {
                                val previous = db.shoppingListDao().ingestState(key)
                                if (previous == null || previous.quantity != 0.0)
                                    db.shoppingListDao().saveIngestState(ShoppingIngestEntity(key, unit = line.item.unit.orEmpty()))
                            } else if (db.shoppingListDao().importKeepItem(key, line.item, week)) imported++
                        }
                        // Removing and later re-adding a Keep entry represents a new request.
                        db.shoppingListDao().ingestStates().filter { it.ingestKey.startsWith(prefix) && it.ingestKey !in seen && it.quantity != 0.0 }
                            .forEach { db.shoppingListDao().saveIngestState(it.copy(quantity = 0.0)) }

                    }
                }
                pageToken = response.get("nextPageToken")?.asString?.takeIf { it.isNotBlank() }
            } while (pageToken != null)
            status(applicationContext, "Keep checked: $imported item${if (imported == 1) "" else "s"} imported. Your Keep notes are unchanged.")
            Result.success()
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: java.io.IOException) { if (runAttemptCount < 3) Result.retry() else { status(applicationContext, "Keep could not connect. Try again or use Keep → Send → PantryPal."); Result.failure() } }
        catch (_: Exception) { status(applicationContext, "Keep access needs attention. Reconnect your Workspace account or use Keep → Send → PantryPal."); Result.failure() }
    }

    companion object {
        const val SCOPE = "https://www.googleapis.com/auth/keep.readonly"
        const val UNAVAILABLE = "Google Keep's official API requires eligible Google Workspace access. Personal Gmail accounts can use Keep → Send → PantryPal instead."
        private const val PERIODIC = "keep-shopping-sync"
        fun preferences(context: Context) = context.getSharedPreferences("keep_shopping_sync", Context.MODE_PRIVATE)
        fun authorisationRequest(email: String): AuthorizationRequest = AuthorizationRequest.builder()
            .setAccount(Account(email, "com.google")).setRequestedScopes(listOf(Scope(SCOPE))).build()
        fun status(context: Context, message: String, disable: Boolean = false) {
            preferences(context).edit().putString("status", message).apply { if (disable) putBoolean("enabled", false) }.apply()
        }
        fun connect(context: Context, email: String) {
            preferences(context).edit().putString("account", email).putBoolean("enabled", true).putString("status", "Keep import queued.").apply()
            val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            val manager = WorkManager.getInstance(context)
            manager.enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.UPDATE,
                PeriodicWorkRequestBuilder<KeepApiSyncWorker>(1, TimeUnit.HOURS).setConstraints(constraints).build())
            syncNow(context)
        }
        fun syncNow(context: Context) = WorkManager.getInstance(context).enqueueUniqueWork("keep-shopping-import-now", ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<KeepApiSyncWorker>().setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).build())
        fun disconnect(context: Context) {
            preferences(context).edit().clear().putString("status", "Keep import disconnected. You can still use Keep → Send → PantryPal.").apply()
            WorkManager.getInstance(context).cancelUniqueWork(PERIODIC)
            WorkManager.getInstance(context).cancelUniqueWork("keep-shopping-import-now")
        }
        private fun hash(value: String) = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }
}
