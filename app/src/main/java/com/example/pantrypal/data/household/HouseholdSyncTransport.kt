package com.example.pantrypal.data.household

/**
 * Backend boundary for a future opt-in real-time service.
 *
 * Portable share/import works without an implementation. A backend can later publish the exact
 * same checksummed envelopes without leaking transport concerns into merge or UI code.
 */
interface HouseholdSyncTransport {
    val transportId: String

    suspend fun publish(
        householdId: String,
        encodedSnapshot: String,
        expectedBaseRevision: Long
    ): Result<HouseholdPublishReceipt>

    suspend fun fetchAfter(
        householdId: String,
        afterRevision: Long
    ): Result<List<String>>
}

data class HouseholdPublishReceipt(
    val acceptedRevision: Long,
    val publishedAtEpochMs: Long,
    val remoteReference: String? = null
)
