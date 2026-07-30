package com.example.pantrypal.domain.household

import com.example.pantrypal.data.backup.BackupDocument
import com.example.pantrypal.data.household.HouseholdSnapshotCodec
import com.example.pantrypal.data.household.HouseholdSnapshotDecodeResult
import com.example.pantrypal.data.household.HouseholdSnapshotPayload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HouseholdMergeEngineTest {
    private val codec = HouseholdSnapshotCodec()

    @Test
    fun descendantRecordWinsWithoutConflictIncludingDeletion() {
        val localRecord = record(
            payload = """{"name":"Milk"}""",
            deleted = false,
            baseRevision = 4,
            revision = 5,
            device = "phone"
        )
        val incomingDelete = record(
            payload = null,
            deleted = true,
            baseRevision = 5,
            revision = 6,
            device = "tablet"
        )

        val result = HouseholdMergeEngine.merge(
            snapshot("local", 5, localRecord).toMergeSource(),
            snapshot("incoming", 6, incomingDelete).toMergeSource()
        )

        assertTrue(result.conflicts.isEmpty())
        assertTrue(result.records.single().isDeleted)
        assertEquals(6L, result.highestRevision)
    }

    @Test
    fun divergentDeviceEditsCreateResolvableConflict() {
        val localRecord = record(
            payload = """{"name":"Semi-skimmed milk"}""",
            deleted = false,
            baseRevision = 4,
            revision = 5,
            modifiedAt = 100,
            device = "phone"
        )
        val incomingRecord = record(
            payload = """{"name":"Whole milk"}""",
            deleted = false,
            baseRevision = 4,
            revision = 5,
            modifiedAt = 200,
            device = "tablet"
        )

        val merged = HouseholdMergeEngine.merge(
            snapshot("local", 5, localRecord).toMergeSource(),
            snapshot("incoming", 5, incomingRecord).toMergeSource()
        )

        assertEquals(1, merged.conflicts.size)
        assertEquals(HouseholdConflictChoice.USE_INCOMING, merged.conflicts.single().suggestedChoice)
        assertEquals(incomingRecord, merged.records.single())

        val resolved = HouseholdMergeEngine.resolve(
            merged,
            mapOf(localRecord.stableKey to HouseholdConflictChoice.KEEP_LOCAL)
        )
        assertTrue(resolved.conflicts.isEmpty())
        assertEquals(localRecord, resolved.records.single())
    }

    @Test
    fun snapshotChecksumRejectsTamperingAndRoundTripsUntouchedData() {
        val payload = snapshot(
            "share",
            1,
            record(
                payload = """{"name":"Milk"}""",
                deleted = false,
                baseRevision = 0,
                revision = 1,
                device = "phone"
            )
        )
        val encoded = codec.encode(payload)

        val decoded = codec.decode(encoded)
        assertTrue(decoded is HouseholdSnapshotDecodeResult.Success)
        assertEquals(payload, (decoded as HouseholdSnapshotDecodeResult.Success).payload)

        val tampered = encoded.replace("Milk", "Bread")
        val rejected = codec.decode(tampered)
        assertTrue(rejected is HouseholdSnapshotDecodeResult.Failure)
        assertTrue(
            (rejected as HouseholdSnapshotDecodeResult.Failure).errors.any {
                "checksum does not match" in it
            }
        )
    }

    private fun record(
        payload: String?,
        deleted: Boolean,
        baseRevision: Long,
        revision: Long,
        modifiedAt: Long = revision * 100,
        device: String
    ) = codec.createRecord(
        collection = HouseholdCollection.ITEMS,
        entityId = "1",
        payloadJson = payload,
        isDeleted = deleted,
        baseRevision = baseRevision,
        revision = revision,
        modifiedAtEpochMs = modifiedAt,
        modifiedByDeviceId = device
    )

    private fun snapshot(
        id: String,
        revision: Long,
        record: HouseholdRecordVersion
    ) = HouseholdSnapshotPayload(
        householdId = "household-1",
        householdName = "Home",
        snapshotId = id,
        createdAtEpochMs = revision * 1_000,
        createdByDeviceId = record.modifiedByDeviceId,
        baseRevision = record.baseRevision,
        revision = revision,
        completeBackup = BackupDocument(
            exportId = "export-$id",
            exportedAtEpochMs = 1_000
        ),
        recordVersions = listOf(record)
    )
}
