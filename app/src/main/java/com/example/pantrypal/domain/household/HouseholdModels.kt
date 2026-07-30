package com.example.pantrypal.domain.household

enum class HouseholdCollection {
    ITEMS,
    INVENTORY,
    CONSUMPTION,
    SHOPPING_SECTIONS,
    SHOPPING_ITEMS,
    SHOPPING_HISTORY,
    MEAL_WEEKS,
    MEALS,
    RECIPES,
    RECIPE_INGREDIENTS,
    PRICE_HISTORY,
    WEEKLY_BUDGETS,
    PREFERENCES
}

enum class HouseholdOperation {
    UPSERT,
    DELETE,
    SNAPSHOT_SHARED,
    SNAPSHOT_IMPORTED
}

data class HouseholdEventMetadata(
    val eventId: String,
    val householdId: String,
    val collection: HouseholdCollection,
    val entityId: String,
    val operation: HouseholdOperation,
    val actorDeviceId: String,
    val actorDisplayName: String? = null,
    val occurredAtEpochMs: Long,
    val baseRevision: Long,
    val revision: Long
)

/**
 * A portable last-writer candidate. [payloadJson] is an individual backup DTO encoded as JSON.
 * Deletions are durable tombstones so an older snapshot cannot resurrect a removed record.
 */
data class HouseholdRecordVersion(
    val collection: HouseholdCollection,
    val entityId: String,
    val payloadJson: String? = null,
    val payloadChecksum: String,
    val isDeleted: Boolean = false,
    val baseRevision: Long,
    val revision: Long,
    val modifiedAtEpochMs: Long,
    val modifiedByDeviceId: String
) {
    val stableKey: String
        get() = "${collection.name}:$entityId"
}

data class HouseholdMergeSource(
    val householdId: String,
    val revision: Long,
    val recordVersions: List<HouseholdRecordVersion> = emptyList(),
    val events: List<HouseholdEventMetadata> = emptyList()
)

enum class HouseholdConflictChoice {
    KEEP_LOCAL,
    USE_INCOMING
}

data class HouseholdConflict(
    val stableKey: String,
    val local: HouseholdRecordVersion,
    val incoming: HouseholdRecordVersion,
    val suggestedChoice: HouseholdConflictChoice,
    val reason: String
)

data class HouseholdMergeResult(
    val records: List<HouseholdRecordVersion>,
    val conflicts: List<HouseholdConflict>,
    val events: List<HouseholdEventMetadata>,
    val highestRevision: Long
)
