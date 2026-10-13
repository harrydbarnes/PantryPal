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
        val pending = dao.pending().take(100)
        if (pending.isEmpty()) return@withTransaction null
        val current = currentRecords()
        ShoppingSyncBatchEntity(householdId = household, batchId = UUID.randomUUID().toString(),
            payload = gson.toJson(pending.associate { it.recordKey to ShoppingRecord(it.token, current[it.recordKey]) })
        ).also { dao.saveBatch(it) }
    }

    fun changes(batch: ShoppingSyncBatchEntity): Map<String, ShoppingRecord> = gson.fromJson(batch.payload, recordMapType)

    suspend fun resetQueue() = db.withTransaction { dao.clearQueue(); dao.clearBatch() }

    /** Accept server state while retaining local edits made during the network request. */
    suspend fun accept(remote: ShoppingWireState, batch: ShoppingSyncBatchEntity?, authoritative: Boolean = false) = db.withTransaction {
        if (batch != null) {
            changes(batch).forEach { (key, record) -> dao.acknowledge(key, record.token) }
            dao.clearBatch()
        }
        val pending = dao.pending().map { it.recordKey }.toSet()
        val current = currentRecords()
        // Authoritative snapshots also remove records whose tombstones have been compacted.
        val incoming = if (authoritative) {
            (current.keys - remote.records.keys - pending).associateWith { ShoppingRecord(data = null) } + remote.records
        } else remote.records
        val fieldPrefix = "layout:item-field:"
        val fields = current.filterKeys { it.startsWith(fieldPrefix) }.toMutableMap()
        incoming.filterKeys { it.startsWith(fieldPrefix) }.forEach { (key, record) ->
            if (key !in pending) {
                if (record.data == null) fields.remove(key) else fields[key] = record.data
            }
        }
        // Base records establish existence. Field records overlay only fields actually edited.
        incoming.entries.filterNot { it.key.startsWith(fieldPrefix) }
            .sortedBy { if (it.key.startsWith("section:")) 0 else 1 }.forEach { (key, record) ->
                if (key !in pending && current[key] != record.data) apply(key, record.data)
            }
        // Base-row triggers may have emitted stale values. Restore the merged field records.
        dao.layout().filter { it.layoutKey.startsWith("item-field:") }.forEach {
            if ("layout:" + it.layoutKey !in fields) dao.deleteLayout(it.layoutKey)
        }
        fields.forEach { (key, value) -> dao.putLayout(ShoppingLayoutEntity(key.removePrefix("layout:"), value)) }
        fields.entries.groupBy { it.key.removePrefix(fieldPrefix).substringBeforeLast(':') }.forEach { (id, entries) ->
            val row = dao.item(id) ?: return@forEach // A field edit must never resurrect a deletion.
            val values = entries.associate { it.key.substringAfterLast(':') to it.value }
            val section = values["sectionSyncId"]?.let { dao.section(it) }
            val updated = row.copy(
                name = values["name"] ?: row.name,
                quantity = values["quantity"]?.toDoubleOrNull() ?: row.quantity,
                unit = values["unit"] ?: row.unit,
                isChecked = values["isChecked"]?.let { it == "1" } ?: row.isChecked,
                sectionId = section?.sectionId ?: row.sectionId,
                weekId = if ("weekId" in values) values["weekId"]?.ifEmpty { null } else row.weekId,
                frequency = values["frequency"] ?: row.frequency,
                category = values["category"] ?: row.category,
                storeTag = values["storeTag"]?.let { runCatching { StoreTag.valueOf(it) }.getOrDefault(StoreTag.EITHER) } ?: row.storeTag,
                source = values["source"] ?: row.source
            )
            require(updated.name.isNotBlank() && updated.quantity.isFinite() && updated.quantity >= 0 && updated.unit.isNotBlank())
            if (updated != row) db.shoppingDao().updateShoppingItem(updated)
            if ("storeTag" in values) db.shoppingListDao().product(updated.name)?.let {
                db.shoppingListDao().rememberProductStore(it.itemId, updated.storeTag)
                db.shoppingListDao().rememberStockStore(it.itemId, updated.storeTag)
            }
        }
        // Suppress generated remote writes while retaining every edit pending before acceptance.
        dao.pending().filter { it.recordKey !in pending }.forEach { dao.acknowledge(it.recordKey, it.token) }
        // A partner may delete a section while this device adds an item to it.
        // Keep the surviving item visible and journal the fallback assignment.
        val sections = dao.sections()
        val sectionIds = sections.map { it.sectionId }.toSet()
        val fallback = sections.firstOrNull { it.systemKey == ShoppingSectionEntity.KEY_THE_REST }
        if (fallback != null) {
            db.shoppingDao().getAllShoppingItemsSnapshot().filter { it.sectionId !in sectionIds }.forEach {
                db.shoppingDao().updateShoppingItem(it.copy(sectionId = fallback.sectionId))
            }
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
            val json = gson.toJsonTree(it.copy(shoppingId = 0, sectionId = 0, inventoryId = null)).asJsonObject
            json.addProperty("sectionSyncId", ids[it.sectionId] ?: ShoppingSectionEntity.KEY_THE_REST)
            result["item:" + it.syncId] = json.toString()
        }
        dao.layout().forEach { result["layout:" + it.layoutKey] = it.value }
        db.backupDao().mealWeeks().forEach { result["week:" + it.weekId] = gson.toJson(it) }
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
                require(row.name.isNotBlank() && row.quantity.isFinite() && row.quantity >= 0 && row.unit.isNotBlank())
                val section = dao.section(json.get("sectionSyncId").asString)
                    ?: dao.section(ShoppingSectionEntity.KEY_THE_REST)
                    ?: error("Shopping section missing; retry the household update.")
                val existing = dao.item(id)
                val localProduct = db.shoppingListDao().product(row.name)
                val localStock = localProduct?.let { db.shoppingListDao().stock(it.itemId).firstOrNull() }
                val value = row.copy(shoppingId = existing?.shoppingId ?: 0, sectionId = section.sectionId, syncId = id,
                    inventoryId = localStock?.inventoryId,
                    category = json.get("category")?.takeUnless { it.isJsonNull }?.asString ?: "Uncategorised",
                    storeTag = json.get("storeTag")?.takeUnless { it.isJsonNull }?.asString?.let { runCatching { StoreTag.valueOf(it) }.getOrDefault(StoreTag.EITHER) } ?: StoreTag.EITHER,
                    source = json.get("source")?.takeUnless { it.isJsonNull }?.asString ?: "MANUAL")
                if (existing == null) db.shoppingDao().insertShoppingItem(value) else db.shoppingDao().updateShoppingItem(value)
            }
            key.startsWith("week:") -> if (data != null) {
                val week = gson.fromJson(data, MealWeekEntity::class.java)
                require(week.name.isNotBlank() && week.sortOrder >= 0)
                db.backupDao().insertMealWeeks(listOf(week.copy(weekId = id)))
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
        payload.mealWeeks.forEach { records["week:" + it.weekId] = ShoppingRecord("legacy", gson.toJson(MealWeekEntity(it.weekId, it.name, it.emoji, it.sortOrder))) }
        return ShoppingWireState(records = records)
    }
}
