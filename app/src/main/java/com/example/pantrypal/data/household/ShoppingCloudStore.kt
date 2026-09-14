package com.example.pantrypal.data.household

import com.google.firebase.Timestamp
import com.google.firebase.firestore.*
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.util.Date

data class ShoppingCloudResult(
    val state: ShoppingWireState,
    val revision: Long,
    val authoritative: Boolean,
    /** Number of shopping-record documents returned by Firestore for this exchange. */
    val recordsRead: Int
)

/** v4 adds per-record revisions and a retention watermark to the v3 record store. */
class ShoppingCloudStore(private val firestore: FirebaseFirestore) {
    private fun root(id: String) = firestore.collection("households").document(id)
    private fun control(id: String) = root(id).collection("state").document("current")
    private fun records(id: String) = root(id).collection("shoppingRecords")
    private fun receipts(id: String) = root(id).collection("shoppingDevices")
    private fun documentId(key: String) = MessageDigest.getInstance("SHA-256").digest(key.toByteArray())
        .joinToString("") { "%02x".format(it) }
    private fun stamp(uid: String) = mapOf("updatedBy" to uid, "updatedAt" to FieldValue.serverTimestamp())
    private fun recordData(key: String, record: ShoppingRecord, uid: String, revision: Long): Map<String, Any?> =
        stamp(uid) + mapOf(
            "key" to key,
            "token" to record.token,
            "data" to record.data,
            "deleted" to (record.data == null),
            "revision" to revision
        )

    /** Complete any interrupted v2-to-v3 migration, then backfill v4 revisions once. */
    suspend fun ensureReady(id: String, uid: String, decode: (DocumentSnapshot) -> ShoppingWireState) {
        ensureV3Ready(id, uid, decode)
        val ref = control(id)
        val migrationRevision = firestore.runTransaction { tx ->
            val doc = tx.get(ref)
            when (doc.getLong("protocol")) {
                4L -> if (doc.getString("phase") == "ready") null else requireNotNull(doc.getLong("revision"))
                3L -> {
                    check(doc.getString("phase") == "ready") { "Household upgrade is still finishing. Retry shortly." }
                    val revision = requireNotNull(doc.getLong("revision"))
                    tx.set(ref, (doc.data.orEmpty() + stamp(uid)) + mapOf(
                        "protocol" to 4,
                        "phase" to "migrating",
                        "revision" to revision,
                        "historyFloor" to revision
                    ))
                    revision
                }
                else -> error("Unsupported household shopping protocol.")
            }
        }.await() ?: return

        // v3 records have no revision. Replaying these deterministic updates is safe after interruption.
        records(id).get(Source.SERVER).await().documents.chunked(100).forEach { chunk ->
            firestore.runTransaction { tx ->
                val current = tx.get(ref)
                if (current.getLong("protocol") == 4L && current.getString("phase") == "migrating") {
                    chunk.forEach { record ->
                        tx.update(record.reference, "revision", migrationRevision)
                    }
                }
            }.await()
        }
        firestore.runTransaction { tx ->
            val current = tx.get(ref)
            if (current.getLong("protocol") == 4L && current.getString("phase") == "migrating") {
                tx.set(ref, stamp(uid) + mapOf(
                    "protocol" to 4,
                    "phase" to "ready",
                    "revision" to migrationRevision,
                    "historyFloor" to migrationRevision
                ))
            }
        }.await()
    }

    /** Freeze v2 once, then replay identical chunks until v3 is ready for the v4 backfill. */
    private suspend fun ensureV3Ready(id: String, uid: String, decode: (DocumentSnapshot) -> ShoppingWireState) {
        val ref = control(id)
        val seed = firestore.runTransaction { tx ->
            val doc = tx.get(ref)
            if ((doc.getLong("protocol") ?: 0) >= 3L) {
                if (doc.getLong("protocol") == 3L && doc.getString("phase") != "ready") decode(doc) else null
            } else {
                val old = decode(doc)
                tx.set(ref, (doc.data.orEmpty() + stamp(uid)) + mapOf("protocol" to 3, "phase" to "migrating", "revision" to 0))
                old
            }
        }.await() ?: return
        val seeds = seed.records.map { (key, value) ->
            records(id).document(documentId(key)) to (recordData(key, value, uid, 0) - "revision")
        } + seed.receipts.map { (device, batch) ->
            receipts(id).document(device) to (stamp(uid) + mapOf("batch" to batch))
        }
        seeds.chunked(100).forEach { chunk ->
            firestore.runTransaction { tx ->
                val current = tx.get(ref)
                if (current.getLong("protocol") == 3L && current.getString("phase") == "migrating") {
                    chunk.forEach { (doc, data) -> tx.set(doc, data) }
                }
            }.await()
        }
        firestore.runTransaction { tx ->
            val current = tx.get(ref)
            if (current.getLong("protocol") == 3L && current.getString("phase") == "migrating") {
                tx.set(ref, stamp(uid) + mapOf("protocol" to 3, "phase" to "ready", "revision" to 0))
            }
        }.await()
    }

    suspend fun exchange(
        id: String,
        uid: String,
        device: String,
        batch: String?,
        changes: Map<String, ShoppingRecord>,
        sinceRevision: Long?
    ): ShoppingCloudResult {
        // A v2 app may have persisted an oversized in-flight batch before upgrading.
        if (batch != null && changes.size > 100) {
            if (receipts(id).document(device).get(Source.SERVER).await().getString("batch") == batch) {
                return exchange(id, uid, device, null, emptyMap(), sinceRevision)
            }
            var cursor = sinceRevision
            var authoritative = false
            var recordsRead = 0
            val accumulated = linkedMapOf<String, ShoppingRecord>()
            changes.entries.chunked(100).forEachIndexed { index, chunk ->
                val part = exchange(id, uid, "$device-part-$index", batch, chunk.associate { it.toPair() }, cursor)
                if (part.authoritative) accumulated.clear()
                accumulated.putAll(part.state.records)
                authoritative = authoritative || part.authoritative
                recordsRead += part.recordsRead
                cursor = part.revision
            }
            val acknowledgement = exchange(id, uid, device, batch, emptyMap(), cursor)
            accumulated.putAll(acknowledgement.state.records)
            return acknowledgement.copy(
                state = ShoppingWireState(records = accumulated),
                authoritative = authoritative || acknowledgement.authoritative,
                recordsRead = recordsRead + acknowledgement.recordsRead
            )
        }

        val ref = control(id)
        val before = ref.get(Source.SERVER).await()
        check(before.getLong("protocol") == 4L && before.getString("phase") == "ready") {
            "Household upgrade is still finishing. Retry shortly."
        }
        val beforeRevision = requireNotNull(before.getLong("revision"))
        val historyFloor = requireNotNull(before.getLong("historyFloor"))
        val authoritative = sinceRevision == null || sinceRevision < historyFloor || sinceRevision > beforeRevision
        val snapshot = when {
            authoritative -> records(id).get(Source.SERVER).await()
            sinceRevision != null && sinceRevision < beforeRevision ->
                records(id).whereGreaterThan("revision", sinceRevision).get(Source.SERVER).await()
            else -> null
        }
        val current = snapshot?.documents.orEmpty()
            .filter { authoritative || requireNotNull(it.getLong("revision")) <= beforeRevision }
            .associate { doc ->
                requireNotNull(doc.getString("key")) to ShoppingRecord(requireNotNull(doc.getString("token")), doc.getString("data"))
            }
        return firestore.runTransaction { tx ->
            val latest = tx.get(ref)
            check(latest.getLong("protocol") == 4L && latest.getString("phase") == "ready") {
                "Household upgrade is still finishing. Retry shortly."
            }
            check(latest.getLong("revision") == beforeRevision) { "Another device updated the list. Retrying with its changes." }
            val receiptRef = receipts(id).document(device)
            val receipt = if (batch != null) tx.get(receiptRef) else null
            val replay = batch != null && receipt?.getString("batch") == batch
            val resultRevision = if (batch != null && !replay) beforeRevision + 1 else beforeRevision
            if (batch != null && !replay) {
                changes.forEach { (key, value) ->
                    require((value.data?.toByteArray()?.size ?: 0) < 100_000) { "A shopping record is too large to sync." }
                    tx.set(records(id).document(documentId(key)), recordData(key, value, uid, resultRevision))
                }
                tx.set(receiptRef, stamp(uid) + mapOf("batch" to batch))
                tx.update(ref, stamp(uid) + mapOf("revision" to resultRevision))
            }
            ShoppingCloudResult(
                state = ShoppingWireState(records = if (batch == null || replay) current else current + changes),
                revision = resultRevision,
                authoritative = authoritative,
                recordsRead = snapshot?.size() ?: 0
            )
        }.await()
    }

    /** Delete up to 100 expired tombstones and advance the unsafe incremental-history boundary. */
    suspend fun compact(id: String, uid: String) {
        val cutoff = Timestamp(Date(System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000))
        val candidates = records(id)
            .whereEqualTo("deleted", true)
            .whereLessThan("updatedAt", cutoff)
            .limit(100)
            .get(Source.SERVER).await().documents
        if (candidates.isEmpty()) return
        firestore.runTransaction { tx ->
            val meta = tx.get(control(id))
            val expired = candidates.map { tx.get(it.reference) }.filter {
                it.getBoolean("deleted") == true && it.getTimestamp("updatedAt")?.let { time -> time < cutoff } == true
            }
            if (expired.isNotEmpty()) {
                expired.forEach { tx.delete(it.reference) }
                val floor = maxOf(
                    requireNotNull(meta.getLong("historyFloor")),
                    expired.maxOf { requireNotNull(it.getLong("revision")) }
                )
                tx.update(control(id), stamp(uid) + mapOf(
                    "revision" to (requireNotNull(meta.getLong("revision")) + 1),
                    "historyFloor" to floor
                ))
            }
        }.await()
    }
}
