package com.example.pantrypal.data.household

import androidx.room.withTransaction
import com.example.pantrypal.data.database.KitchenDatabase
import com.example.pantrypal.data.entity.*
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import java.util.UUID

/** The queue is written by SQLite triggers, including worker, bulk and restore mutations. */
class ShoppingSyncStore(private val db: KitchenDatabase) {
    private val dao = db.shoppingSyncDao()
    private val gson = Gson()
    private val recordMapType = object : TypeToken<Map<String, ShoppingRecord>>() {}.type

    suspend fun seedQueue() = db.withTransaction {
        currentRecords().keys.forEach { dao.enqueue(ShoppingSyncQueueEntity(it, UUID.randomUUID().toString())) }
    }

    suspend fun batch(household: String): ShoppingSyncBatchEntity? = db.withTransaction {
        val existing = dao.batch()
        if (existing != null) {
            require(existing.householdId == household) { "Finish or leave the previous household before switching." }
            return@withTransaction existing
        }
        val pending = dao.pending()
        if (pending.isEmpty()) return@withTransaction null
        val current = currentRecords()
        ShoppingSyncBatchEntity(householdId = household, batchId = UUID.randomUUID().toString(),
            payload = gson.toJson(pending.associate { it.recordKey to ShoppingRecord(it.token, current[it.recordKey]) })
        ).also { dao.saveBatch(it) }
    }

    fun changes(batch: ShoppingSyncBatchEntity): Map<String, ShoppingRecord> = gson.fromJson(batch.payload, recordMapType)

    suspend fun resetQueue() = db.withTransaction { dao.clearQueue(); dao.clearBatch() }

    /** Accept server state while retaining local edits made during the network request. */
    suspend fun accept(remote: ShoppingWireState, batch: ShoppingSyncBatchEntity?) = db.withTransaction {
        if (batch != null) {
            changes(batch).forEach { (key, record) -> dao.acknowledge(key, record.token) }
            dao.clearBatch()
        }
        val pending = dao.pending().map { it.recordKey }.toSet()
        val current = currentRecords()
        // Parents before children. Section IDs are local; only sync IDs cross devices.
        remote.records.entries.sortedBy { if (it.key.startsWith("section:")) 0 else 1 }.forEach { (key, record) ->
            if (key in pending || current[key] == record.data) return@forEach
            apply(key, record.data)
            // Suppress only this remote write, in this transaction. Never drop user edits.
            dao.pending().firstOrNull { it.recordKey == key }?.let { dao.acknowledge(key, it.token) }
        }
    }

    suspend fun replaceShopping(remote: ShoppingWireState) = db.withTransaction {
        db.shoppingDao().getAllShoppingItemsSnapshot().forEach { db.shoppingDao().deleteShoppingItem(it) }
        dao.sections().filter { it.systemKey == null }.forEach { dao.deleteSection(it.syncId) }
        dao.layout().forEach { dao.deleteLayout(it.layoutKey) }
        dao.clearQueue(); dao.clearBatch()
        accept(remote, null)
    }

    private suspend fun currentRecords(): Map<String, String> {
        val sections = dao.sections()
        val ids = sections.associate { it.sectionId to it.syncId }
        val result = mutableMapOf<String, String>()
        sections.forEach { result["section:" + it.syncId] = gson.toJson(it.copy(sectionId = 0)) }
        db.shoppingDao().getAllShoppingItemsSnapshot().forEach {
            val json = gson.toJsonTree(it.copy(shoppingId = 0, sectionId = 0)).asJsonObject
            json.addProperty("sectionSyncId", ids[it.sectionId] ?: ShoppingSectionEntity.KEY_THE_REST)
            result["item:" + it.syncId] = json.toString()
        }
        dao.layout().forEach { result["layout:" + it.layoutKey] = it.value }
        return result
    }

    private suspend fun apply(key: String, data: String?) {
        val id = key.substringAfter(':')
        when {
            key.startsWith("section:") -> if (data == null) dao.deleteSection(id) else {
                val row = gson.fromJson(data, ShoppingSectionEntity::class.java)
                require(row.name.isNotBlank() && row.sortOrder >= 0)
                val existing = dao.section(id)
                val value = row.copy(sectionId = existing?.sectionId ?: 0, syncId = id)
                if (existing == null) db.shoppingSectionDao().insertSection(value) else db.shoppingSectionDao().updateSection(value)
            }
            key.startsWith("item:") -> if (data == null) dao.deleteItem(id) else {
                val json = JsonParser.parseString(data).asJsonObject
                val row = gson.fromJson(json, ShoppingItemEntity::class.java)
                require(row.name.isNotBlank() && row.quantity.isFinite() && row.quantity > 0 && row.unit.isNotBlank())
                val section = dao.section(json.get("sectionSyncId").asString)
                    ?: dao.section(ShoppingSectionEntity.KEY_THE_REST)
                    ?: error("Shopping section missing; retry the household update.")
                val existing = dao.item(id)
                val value = row.copy(shoppingId = existing?.shoppingId ?: 0, sectionId = section.sectionId, syncId = id)
                if (existing == null) db.shoppingDao().insertShoppingItem(value) else db.shoppingDao().updateShoppingItem(value)
            }
            key.startsWith("layout:") -> if (data == null) dao.deleteLayout(id) else dao.putLayout(ShoppingLayoutEntity(id, data))
            else -> error("Unsupported shopping record.")
        }
    }

    /** One-time upgrade reads only shopping fields from the legacy snapshot. */
    fun upgradeLegacy(snapshot: String): ShoppingWireState {
        val decoded = HouseholdSnapshotCodec().decode(snapshot)
        require(decoded is HouseholdSnapshotDecodeResult.Success) { "The legacy household could not be read." }
        val payload = decoded.payload.completeBackup.payload
        val records = mutableMapOf<String, ShoppingRecord>()
        val ids = payload.shoppingSections.associate { it.sectionId to (it.systemKey ?: "legacy-section-${it.sectionId}") }
        payload.shoppingSections.forEach {
            val id = ids.getValue(it.sectionId)
            records["section:$id"] = ShoppingRecord("legacy", gson.toJson(ShoppingSectionEntity(0, it.name, it.sortOrder, it.recursEveryWeek, it.systemKey, id)))
        }
        payload.shoppingItems.forEach {
            val id = "legacy-item-${it.shoppingId}"
            val json = gson.toJsonTree(ShoppingItemEntity(0, it.name, it.quantity, it.unit, it.isChecked, it.addedAt, it.frequency, 0, it.weekId, id)).asJsonObject
            json.addProperty("sectionSyncId", ids[it.sectionId] ?: ShoppingSectionEntity.KEY_THE_REST)
            records["item:$id"] = ShoppingRecord("legacy", json.toString())
        }
        return ShoppingWireState(records = records)
    }
}
