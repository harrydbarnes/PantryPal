package com.example.pantrypal.data.household

import com.example.pantrypal.data.backup.BackupDocument
import com.example.pantrypal.domain.household.HouseholdCollection
import com.example.pantrypal.domain.household.HouseholdEventMetadata
import com.example.pantrypal.domain.household.HouseholdOperation
import com.example.pantrypal.domain.household.HouseholdRecordVersion
import java.util.UUID

object HouseholdSnapshotFactory {
    fun createPortableSnapshot(
        householdId: String,
        householdName: String,
        deviceId: String,
        deviceName: String?,
        baseRevision: Long,
        revision: Long,
        backup: BackupDocument,
        recordVersions: List<HouseholdRecordVersion> = emptyList(),
        createdAtEpochMs: Long = System.currentTimeMillis(),
        snapshotId: String = UUID.randomUUID().toString()
    ): HouseholdSnapshotPayload {
        val shareEvent = HouseholdEventMetadata(
            eventId = UUID.randomUUID().toString(),
            householdId = householdId,
            collection = HouseholdCollection.PREFERENCES,
            entityId = snapshotId,
            operation = HouseholdOperation.SNAPSHOT_SHARED,
            actorDeviceId = deviceId,
            actorDisplayName = deviceName,
            occurredAtEpochMs = createdAtEpochMs,
            baseRevision = baseRevision,
            revision = revision
        )
        return HouseholdSnapshotPayload(
            householdId = householdId,
            householdName = householdName,
            snapshotId = snapshotId,
            createdAtEpochMs = createdAtEpochMs,
            createdByDeviceId = deviceId,
            baseRevision = baseRevision,
            revision = revision,
            completeBackup = backup,
            recordVersions = recordVersions,
            events = listOf(shareEvent)
        )
    }
}
