package com.example.pantrypal.data.household

import com.example.pantrypal.data.backup.BackupDocument
import com.example.pantrypal.domain.household.HouseholdEventMetadata
import com.example.pantrypal.domain.household.HouseholdMergeSource
import com.example.pantrypal.domain.household.HouseholdRecordVersion

data class HouseholdSnapshotPayload(
    val householdId: String,
    val householdName: String,
    val snapshotId: String,
    val createdAtEpochMs: Long,
    val createdByDeviceId: String,
    val baseRevision: Long,
    val revision: Long,
    val completeBackup: BackupDocument,
    val recordVersions: List<HouseholdRecordVersion> = emptyList(),
    val events: List<HouseholdEventMetadata> = emptyList()
) {
    fun toMergeSource(): HouseholdMergeSource = HouseholdMergeSource(
        householdId = householdId,
        revision = revision,
        recordVersions = recordVersions,
        events = events
    )
}
