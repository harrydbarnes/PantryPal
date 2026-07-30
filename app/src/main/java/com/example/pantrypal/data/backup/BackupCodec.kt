package com.example.pantrypal.data.backup

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser

sealed interface BackupDecodeResult {
    data class Success(
        val document: BackupDocument,
        val warnings: List<String>
    ) : BackupDecodeResult

    data class Failure(val errors: List<String>) : BackupDecodeResult
}

class BackupCodec(
    private val gson: Gson = GsonBuilder()
        .disableHtmlEscaping()
        .create()
) {
    fun encode(document: BackupDocument, pretty: Boolean = false): String {
        val validation = BackupValidator.validate(document)
        require(validation.isValid) {
            "Cannot encode an invalid PantryPal backup: ${validation.errors.joinToString()}"
        }
        return if (pretty) {
            GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create().toJson(document)
        } else {
            gson.toJson(document)
        }
    }

    fun decode(json: String): BackupDecodeResult {
        if (json.isBlank()) return BackupDecodeResult.Failure(listOf("Backup is empty."))
        return runCatching {
            val root = JsonParser.parseString(json)
            if (!root.isJsonObject) {
                return BackupDecodeResult.Failure(listOf("Backup root must be a JSON object."))
            }
            val rootObject = root.asJsonObject
            if (!rootObject.has("format")) {
                return BackupDecodeResult.Failure(listOf("Backup format marker is missing."))
            }
            if (!rootObject.has("schemaVersion")) {
                return BackupDecodeResult.Failure(listOf("Backup schema version is missing."))
            }
            if (!rootObject.has("payload") || !rootObject.get("payload").isJsonObject) {
                return BackupDecodeResult.Failure(listOf("Backup payload is missing."))
            }
            val document = gson.fromJson(root, BackupDocument::class.java)
            val validation = BackupValidator.validate(document)
            if (validation.isValid) {
                BackupDecodeResult.Success(document, validation.warnings)
            } else {
                BackupDecodeResult.Failure(validation.errors)
            }
        }.getOrElse { error ->
            BackupDecodeResult.Failure(
                listOf("Backup could not be read: ${error.message ?: error::class.java.simpleName}.")
            )
        }
    }
}
