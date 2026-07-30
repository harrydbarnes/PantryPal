package com.example.pantrypal.domain.household

/**
 * Deterministic record merge rules:
 * 1. Identical values collapse to the newest metadata.
 * 2. A version whose base includes the other version is its descendant and wins.
 * 3. Sequential edits from one device use the higher revision.
 * 4. Divergent edits are retained as a conflict; the newest timestamp is only a suggestion.
 */
object HouseholdMergeEngine {
    fun merge(
        local: HouseholdMergeSource,
        incoming: HouseholdMergeSource
    ): HouseholdMergeResult {
        require(local.householdId == incoming.householdId) {
            "Snapshots belong to different households."
        }
        val localByKey = local.recordVersions.associateBy(HouseholdRecordVersion::stableKey)
        val incomingByKey = incoming.recordVersions.associateBy(HouseholdRecordVersion::stableKey)
        val allKeys = (localByKey.keys + incomingByKey.keys).toSortedSet()
        val records = mutableListOf<HouseholdRecordVersion>()
        val conflicts = mutableListOf<HouseholdConflict>()

        allKeys.forEach { key ->
            val localRecord = localByKey[key]
            val incomingRecord = incomingByKey[key]
            when {
                localRecord == null -> records += requireNotNull(incomingRecord)
                incomingRecord == null -> records += localRecord
                sameValue(localRecord, incomingRecord) -> {
                    records += newerMetadata(localRecord, incomingRecord)
                }
                incomingRecord.baseRevision >= localRecord.revision -> {
                    records += incomingRecord
                }
                localRecord.baseRevision >= incomingRecord.revision -> {
                    records += localRecord
                }
                localRecord.modifiedByDeviceId == incomingRecord.modifiedByDeviceId -> {
                    records += if (incomingRecord.revision >= localRecord.revision) {
                        incomingRecord
                    } else {
                        localRecord
                    }
                }
                else -> {
                    val suggestion = if (compareFreshness(incomingRecord, localRecord) >= 0) {
                        HouseholdConflictChoice.USE_INCOMING
                    } else {
                        HouseholdConflictChoice.KEEP_LOCAL
                    }
                    val conflict = HouseholdConflict(
                        stableKey = key,
                        local = localRecord,
                        incoming = incomingRecord,
                        suggestedChoice = suggestion,
                        reason = "Both devices changed this record from different base revisions."
                    )
                    conflicts += conflict
                    records += when (suggestion) {
                        HouseholdConflictChoice.KEEP_LOCAL -> localRecord
                        HouseholdConflictChoice.USE_INCOMING -> incomingRecord
                    }
                }
            }
        }

        val events = (local.events + incoming.events)
            .associateBy(HouseholdEventMetadata::eventId)
            .values
            .sortedWith(
                compareBy<HouseholdEventMetadata> { it.occurredAtEpochMs }
                    .thenBy { it.eventId }
            )
        return HouseholdMergeResult(
            records = records.sortedBy(HouseholdRecordVersion::stableKey),
            conflicts = conflicts,
            events = events,
            highestRevision = maxOf(
                local.revision,
                incoming.revision,
                records.maxOfOrNull(HouseholdRecordVersion::revision) ?: 0
            )
        )
    }

    fun resolve(
        result: HouseholdMergeResult,
        choices: Map<String, HouseholdConflictChoice>
    ): HouseholdMergeResult {
        val conflictsByKey = result.conflicts.associateBy(HouseholdConflict::stableKey)
        val resolvedRecords = result.records.associateBy(HouseholdRecordVersion::stableKey).toMutableMap()
        choices.forEach { (key, choice) ->
            val conflict = conflictsByKey[key] ?: return@forEach
            resolvedRecords[key] = when (choice) {
                HouseholdConflictChoice.KEEP_LOCAL -> conflict.local
                HouseholdConflictChoice.USE_INCOMING -> conflict.incoming
            }
        }
        val unresolved = result.conflicts.filter { it.stableKey !in choices }
        return result.copy(
            records = resolvedRecords.values.sortedBy(HouseholdRecordVersion::stableKey),
            conflicts = unresolved
        )
    }

    private fun sameValue(
        first: HouseholdRecordVersion,
        second: HouseholdRecordVersion
    ): Boolean = first.isDeleted == second.isDeleted &&
        first.payloadChecksum == second.payloadChecksum

    private fun newerMetadata(
        first: HouseholdRecordVersion,
        second: HouseholdRecordVersion
    ): HouseholdRecordVersion =
        if (compareFreshness(first, second) >= 0) first else second

    private fun compareFreshness(
        first: HouseholdRecordVersion,
        second: HouseholdRecordVersion
    ): Int = compareValuesBy(
        first,
        second,
        HouseholdRecordVersion::revision,
        HouseholdRecordVersion::modifiedAtEpochMs,
        HouseholdRecordVersion::modifiedByDeviceId
    )
}
