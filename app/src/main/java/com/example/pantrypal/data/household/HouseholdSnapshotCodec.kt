package com.example.pantrypal.data.household

import com.example.pantrypal.data.backup.BackupValidator
import com.example.pantrypal.domain.household.HouseholdRecordVersion
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

data class HouseholdSnapshotEnvelope(
    val format: String = FORMAT,
    val envelopeVersion: Int = CURRENT_VERSION,
    val checksumAlgorithm: String = ALGORITHM,
    val payloadJson: String,
    val integritySignature: String,
    val securityNotice: String = INTEGRITY_NOTICE
) {
    companion object {
        const val FORMAT = "pantrypal-household-snapshot"
        const val CURRENT_VERSION = 1
        const val ALGORITHM = "SHA-256"
        const val INTEGRITY_NOTICE =
            "Checksum detects accidental changes; this snapshot is not encrypted or authenticated."
    }
}

sealed interface HouseholdSnapshotDecodeResult {
    data class Success(
        val payload: HouseholdSnapshotPayload,
        val warnings: List<String>
    ) : HouseholdSnapshotDecodeResult

    data class Failure(val errors: List<String>) : HouseholdSnapshotDecodeResult
}

class HouseholdSnapshotCodec(
    private val gson: Gson = GsonBuilder().disableHtmlEscaping().create()
) {
    fun encode(payload: HouseholdSnapshotPayload, pretty: Boolean = false): String {
        val errors = validate(payload)
        require(errors.isEmpty()) {
            "Cannot encode invalid household snapshot: ${errors.joinToString()}"
        }
        val payloadJson = gson.toJson(payload)
        val envelope = HouseholdSnapshotEnvelope(
            payloadJson = payloadJson,
            integritySignature = checksum(envelopeSignableText(payloadJson))
        )
        return if (pretty) {
            GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create().toJson(envelope)
        } else {
            gson.toJson(envelope)
        }
    }

    fun decode(encoded: String): HouseholdSnapshotDecodeResult {
        if (encoded.isBlank()) {
            return HouseholdSnapshotDecodeResult.Failure(listOf("Household snapshot is empty."))
        }
        return runCatching {
            val root = JsonParser.parseString(encoded)
            if (!root.isJsonObject) {
                return HouseholdSnapshotDecodeResult.Failure(
                    listOf("Household snapshot root must be an object.")
                )
            }
            val envelope = gson.fromJson(root, HouseholdSnapshotEnvelope::class.java)
            if (envelope.format != HouseholdSnapshotEnvelope.FORMAT) {
                return HouseholdSnapshotDecodeResult.Failure(
                    listOf("Unsupported household snapshot format.")
                )
            }
            if (envelope.envelopeVersion != HouseholdSnapshotEnvelope.CURRENT_VERSION) {
                return HouseholdSnapshotDecodeResult.Failure(
                    listOf("Unsupported household snapshot version ${envelope.envelopeVersion}.")
                )
            }
            if (envelope.checksumAlgorithm != HouseholdSnapshotEnvelope.ALGORITHM) {
                return HouseholdSnapshotDecodeResult.Failure(
                    listOf("Unsupported checksum algorithm '${envelope.checksumAlgorithm}'.")
                )
            }
            val expected = checksum(envelopeSignableText(envelope.payloadJson))
            if (!constantTimeEquals(expected, envelope.integritySignature)) {
                return HouseholdSnapshotDecodeResult.Failure(
                    listOf("Snapshot checksum does not match; the file may be damaged or changed.")
                )
            }
            val payload = gson.fromJson(
                envelope.payloadJson,
                HouseholdSnapshotPayload::class.java
            )
            val validationErrors = validate(payload)
            if (validationErrors.isNotEmpty()) {
                HouseholdSnapshotDecodeResult.Failure(validationErrors)
            } else {
                val warnings = BackupValidator.validate(payload.completeBackup).warnings +
                    HouseholdSnapshotEnvelope.INTEGRITY_NOTICE
                HouseholdSnapshotDecodeResult.Success(payload, warnings.distinct())
            }
        }.getOrElse { error ->
            HouseholdSnapshotDecodeResult.Failure(
                listOf(
                    "Household snapshot could not be read: " +
                        "${error.message ?: error::class.java.simpleName}."
                )
            )
        }
    }

    fun createRecord(
        collection: com.example.pantrypal.domain.household.HouseholdCollection,
        entityId: String,
        payloadJson: String?,
        isDeleted: Boolean,
        baseRevision: Long,
        revision: Long,
        modifiedAtEpochMs: Long,
        modifiedByDeviceId: String
    ): HouseholdRecordVersion {
        require(isDeleted == (payloadJson == null)) {
            "Deleted records must have no payload; active records must have a payload."
        }
        return HouseholdRecordVersion(
            collection = collection,
            entityId = entityId,
            payloadJson = payloadJson,
            payloadChecksum = recordChecksum(payloadJson, isDeleted),
            isDeleted = isDeleted,
            baseRevision = baseRevision,
            revision = revision,
            modifiedAtEpochMs = modifiedAtEpochMs,
            modifiedByDeviceId = modifiedByDeviceId
        )
    }

    private fun validate(payload: HouseholdSnapshotPayload): List<String> {
        val errors = mutableListOf<String>()
        if (payload.householdId.isBlank()) errors += "Household ID is missing."
        if (payload.householdName.isBlank()) errors += "Household name is missing."
        if (payload.snapshotId.isBlank()) errors += "Snapshot ID is missing."
        if (payload.createdByDeviceId.isBlank()) errors += "Creating device ID is missing."
        if (payload.createdAtEpochMs <= 0) errors += "Snapshot timestamp is invalid."
        if (payload.revision < payload.baseRevision || payload.baseRevision < 0) {
            errors += "Snapshot revisions are invalid."
        }
        payload.completeBackup.payload.preferences.householdId?.let { backupHouseholdId ->
            if (backupHouseholdId != payload.householdId) {
                errors += "Backup and snapshot household IDs do not match."
            }
        }
        val duplicateRecordKeys = payload.recordVersions
            .groupBy(HouseholdRecordVersion::stableKey)
            .filterValues { it.size > 1 }
            .keys
        if (duplicateRecordKeys.isNotEmpty()) {
            errors += "Snapshot contains duplicate record keys."
        }
        payload.recordVersions.forEachIndexed { index, record ->
            if (record.entityId.isBlank()) errors += "recordVersions[$index] has no entity ID."
            if (record.modifiedByDeviceId.isBlank()) {
                errors += "recordVersions[$index] has no device ID."
            }
            if (
                record.baseRevision < 0 ||
                record.revision < record.baseRevision ||
                record.revision > payload.revision
            ) {
                errors += "recordVersions[$index] has invalid revisions."
            }
            if (record.isDeleted != (record.payloadJson == null)) {
                errors += "recordVersions[$index] has inconsistent deletion data."
            }
            if (
                record.payloadChecksum != recordChecksum(record.payloadJson, record.isDeleted)
            ) {
                errors += "recordVersions[$index] checksum does not match."
            }
        }
        val duplicateEventIds = payload.events
            .groupBy { it.eventId }
            .filterValues { it.size > 1 }
            .keys
        if (duplicateEventIds.isNotEmpty()) errors += "Snapshot contains duplicate event IDs."
        payload.events.forEachIndexed { index, event ->
            if (event.eventId.isBlank()) errors += "events[$index] has no event ID."
            if (event.householdId != payload.householdId) {
                errors += "events[$index] belongs to another household."
            }
            if (event.entityId.isBlank()) errors += "events[$index] has no entity ID."
            if (event.actorDeviceId.isBlank()) errors += "events[$index] has no device ID."
            if (
                event.baseRevision < 0 ||
                event.revision < event.baseRevision ||
                event.revision > payload.revision
            ) {
                errors += "events[$index] has invalid revisions."
            }
        }
        val backupValidation = BackupValidator.validate(payload.completeBackup)
        errors += backupValidation.errors
        return errors.distinct()
    }

    private fun recordChecksum(payloadJson: String?, deleted: Boolean): String =
        checksum(if (deleted) "TOMBSTONE" else "ACTIVE:${payloadJson.orEmpty()}")

    private fun envelopeSignableText(payloadJson: String): String =
        "${HouseholdSnapshotEnvelope.FORMAT}:${HouseholdSnapshotEnvelope.CURRENT_VERSION}:$payloadJson"

    private fun checksum(value: String): String = MessageDigest
        .getInstance(HouseholdSnapshotEnvelope.ALGORITHM)
        .digest(value.toByteArray(StandardCharsets.UTF_8))
        .joinToString(separator = "") { byte -> "%02x".format(byte.toInt() and 0xff) }

    private fun constantTimeEquals(expected: String, actual: String): Boolean =
        MessageDigest.isEqual(
            expected.toByteArray(StandardCharsets.US_ASCII),
            actual.toByteArray(StandardCharsets.US_ASCII)
        )
}
