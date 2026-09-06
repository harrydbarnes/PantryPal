package com.example.pantrypal.data.household

/** Payloads are individual records, never a complete database restore. Null is a tombstone. */
data class ShoppingRecord(val token: String = "", val data: String? = null)
data class ShoppingWireState(
    val protocol: Int = 2,
    val records: Map<String, ShoppingRecord> = emptyMap(),
    val receipts: Map<String, String> = emptyMap()
)

object ShoppingRecordProtocol {
    /** Receipt makes retry after a lost acknowledgement idempotent, even after another edit. */
    fun commit(remote: ShoppingWireState, device: String, batch: String, changes: Map<String, ShoppingRecord>): ShoppingWireState {
        require(remote.protocol == 2) { "Update PantryPal before syncing this household." }
        if (remote.receipts[device] == batch) return remote
        return remote.copy(records = remote.records + changes, receipts = remote.receipts + (device to batch))
    }
}
