package com.augmentalis.commandmanager

import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

/**
 * Serializer for command export/import operations.
 *
 * Handles JSON serialization with:
 * - Version checking for backward compatibility
 * - Pretty printing for human-readable exports
 * - Error handling for corrupt/invalid files
 */
object ExportSerializer {

    /**
     * Current export format version.
     * Increment when making breaking changes to export format.
     */
    const val CURRENT_VERSION = 1

    /**
     * Minimum supported import version.
     */
    const val MIN_SUPPORTED_VERSION = 1

    /**
     * JSON configuration for export.
     * Uses pretty printing for readability.
     */
    private val exportJson = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = false
    }

    /**
     * JSON configuration for import.
     * More lenient - ignores unknown keys for forward compatibility.
     */
    private val importJson = Json {
        prettyPrint = false
        encodeDefaults = true
        ignoreUnknownKeys = true  // Allow newer exports with extra fields
        isLenient = true
    }

    /**
     * Serialize export package to JSON string.
     *
     * @param exportPackage Data to export
     * @return JSON string
     */
    fun serialize(exportPackage: ExportPackage): String {
        return exportJson.encodeToString(exportPackage)
    }

    /**
     * Deserialize JSON string to export package.
     *
     * @param json JSON string to parse
     * @return Result containing ExportPackage or error
     */
    fun deserialize(json: String): Result<ExportPackage> {
        return try {
            val pkg = importJson.decodeFromString<ExportPackage>(json)

            // Version check
            if (pkg.manifest.version < MIN_SUPPORTED_VERSION) {
                return Result.failure(
                    ExportVersionException(
                        "Export version ${pkg.manifest.version} is too old. " +
                        "Minimum supported: $MIN_SUPPORTED_VERSION"
                    )
                )
            }

            if (pkg.manifest.version > CURRENT_VERSION) {
                // Log warning but allow import (forward compatibility)
                // Logger.w("Importing from newer version ${pkg.manifest.version}")
            }

            Result.success(pkg)
        } catch (e: Exception) {
            Result.failure(ExportParseException("Failed to parse export file: ${e.message}", e))
        }
    }

    /**
     * Quick validation without full deserialization.
     * Checks if JSON is valid and has expected structure.
     *
     * @param json JSON string to validate
     * @return true if appears valid, false otherwise
     */
    fun isValidExportFormat(json: String): Boolean {
        return try {
            val pkg = importJson.decodeFromString<ExportPackage>(json)
            pkg.manifest.version >= MIN_SUPPORTED_VERSION
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get export metadata without full parsing.
     * Useful for preview before import.
     *
     * @param json JSON string
     * @return ExportManifest or null if invalid (error is logged)
     */
    fun getManifest(json: String): ExportManifest? {
        return try {
            val pkg = importJson.decodeFromString<ExportPackage>(json)
            pkg.manifest
        } catch (e: Exception) {
            // Log the error to aid debugging - silent failures are hard to diagnose
            LoggingUtils.w("Failed to parse export manifest: ${e.message}", "ExportSerializer", e)
            null
        }
    }

    /**
     * Generate default export filename with timestamp.
     */
    fun generateExportFilename(type: ExportType, appName: String? = null): String {
        val timestamp = currentTimeMillis()
        val dateStr = formatTimestamp(timestamp)

        return when (type) {
            ExportType.FULL -> "voiceos_backup_$dateStr.json"
            ExportType.SINGLE_APP -> {
                val safeName = appName?.replace("[^a-zA-Z0-9]".toRegex(), "_") ?: "app"
                "voiceos_${safeName}_$dateStr.json"
            }
            ExportType.MULTI_APP -> "voiceos_export_$dateStr.json"
        }
    }

    private fun formatTimestamp(millis: Long): String {
        // Simple date format: YYYYMMDD_HHMMSS
        // In real KMP, would use kotlinx-datetime
        return millis.toString().takeLast(12)
    }
}

/**
 * Exception for version compatibility issues.
 */
class ExportVersionException(message: String) : Exception(message)

/**
 * Exception for JSON parsing errors.
 */
class ExportParseException(message: String, cause: Throwable? = null) : Exception(message, cause)
