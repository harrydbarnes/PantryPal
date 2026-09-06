package com.example.pantrypal.data.household

import com.google.firebase.Timestamp
import com.google.firebase.firestore.*
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.util.Date

/** v3 stores each record separately. The small control document serialises snapshot revisions.
 * Downloads are authoritative snapshots, so an offline device can reconcile compacted deletes.
 */
class ShoppingCloudStore(private val firestore: FirebaseFirestore) {
    private fun root(id: String) = firestore.collection("households").document(id)
    private fun control(id: String) = root(id).collection("state").document("current")
    private fun records(id: String) = root(id).collection("shoppingRecords")
    private fun receipts(id: String) = root(id).collection("shoppingDevices")
    private fun documentId(key: String) = MessageDigest.getInstance("SHA-256").digest(key.toByteArray())
        .joinToString("") { "%02x".format(it) }
    private fun stamp(uid: String) = mapOf("updatedBy" to uid, "updatedAt" to FieldValue.serverTimestamp())
    private fun recordData(key: String, record: ShoppingRecord, uid: String): Map<String, Any?> =
        stamp(uid) + mapOf("key" to key, "token" to record.token, "data" to record.data, "deleted" to (record.data == null))

    /** Freeze v2 once, then replay identical chunks until ready. Other migrators may help safely. */
    suspend fun ensureReady(id: String, uid: String, decode: (DocumentSnapshot) -> ShoppingWireState) {
        val ref = control(id)
        val seed = firestore.runTransaction { tx ->
            val doc = tx.get(ref)
            if (doc.getLong("protocol") == 3L) {
                if (doc.getString("phase") == "ready") null else decode(doc)
            } else {
                val old = decode(doc)
                tx.set(ref, (doc.data.orEmpty() + stamp(uid)) + mapOf("protocol" to 3, "phase" to "migrating", "revision" to 0))
                old
            }
        }.await() ?: return
        // Include old acknowledgements so lost v2 acknowledgements cannot replay stale edits.
        val seeds = seed.records.map { (key, value) -> records(id).document(documentId(key)) to recordData(key, value, uid) } +
            seed.receipts.map { (device, batch) -> receipts(id).document(device) to (stamp(uid) + mapOf("batch" to batch)) }
        seeds.chunked(100).forEach { chunk ->
            firestore.runTransaction { tx ->
                if (tx.get(ref).getString("phase") == "migrating") chunk.forEach { (doc, data) -> tx.set(doc, data) }
            }.await()
        }
        firestore.runTransaction { tx ->
            if (tx.get(ref).getString("phase") == "migrating")
                tx.set(ref, stamp(uid) + mapOf("protocol" to 3, "phase" to "ready", "revision" to 0))
        }.await()
    }

    suspend fun exchange(id: String, uid: String, device: String, batch: String?, changes: Map<String, ShoppingRecord>): ShoppingWireState {
        // A v2 app may have persisted an oversized in-flight batch before upgrading.
        if (batch != null && changes.size > 100) {
            if (receipts(id).document(device).get(Source.SERVER).await().getString("batch") != batch) {
                changes.entries.chunked(100).forEachIndexed { index, chunk ->
                    exchange(id, uid, "$device-part-$index", batch, chunk.associate { it.toPair() })
                }
                return exchange(id, uid, device, batch, emptyMap())
            }
            return exchange(id, uid, device, null, emptyMap())
        }
        val ref = control(id)
        val before = ref.get(Source.SERVER).await()
        check(before.getString("phase") == "ready") { "Household upgrade is still finishing. Retry shortly." }
        val snapshot = records(id).get(Source.SERVER).await()
        val current = snapshot.documents.associate { doc ->
            requireNotNull(doc.getString("key")) to ShoppingRecord(requireNotNull(doc.getString("token")), doc.getString("data"))
        }
        return firestore.runTransaction { tx ->
            val latest = tx.get(ref)
            check(latest.getLong("revision") == before.getLong("revision")) { "Another device updated the list. Retrying with its changes." }
            val receiptRef = receipts(id).document(device)
            val receipt = if (batch != null) tx.get(receiptRef) else null
            val replay = batch != null && receipt?.getString("batch") == batch
            if (batch != null && !replay) {
                changes.forEach { (key, value) ->
                    require((value.data?.toByteArray()?.size ?: 0) < 100_000) { "A shopping record is too large to sync." }
                    tx.set(records(id).document(documentId(key)), recordData(key, value, uid))
                }
                tx.set(receiptRef, stamp(uid) + mapOf("batch" to batch))
                tx.update(ref, stamp(uid) + mapOf("revision" to (latest.getLong("revision")!! + 1)))
            }
            ShoppingWireState(records = if (batch == null || replay) current else current + changes)
        }.await()
    }

    /** Bound deletion history to 30 days, removing at most 100 per pass.
     * Read each candidate again transactionally so a restored record is never deleted.
     */
    suspend fun compact(id: String, uid: String) {
        val cutoff = Timestamp(Date(System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000))
        val candidates = records(id).whereEqualTo("deleted", true).get(Source.SERVER).await().documents
            .filter { it.getTimestamp("updatedAt")?.let { time -> time < cutoff } == true }.take(100)
        if (candidates.isEmpty()) return
        firestore.runTransaction { tx ->
            val meta = tx.get(control(id))
            val expired = candidates.map { tx.get(it.reference) }.filter {
                it.getBoolean("deleted") == true && it.getTimestamp("updatedAt")?.let { time -> time < cutoff } == true
            }
            if (expired.isNotEmpty()) {
                expired.forEach { tx.delete(it.reference) }
                tx.update(control(id), stamp(uid) + mapOf("revision" to (meta.getLong("revision")!! + 1)))
            }
        }.await()
    }
}
