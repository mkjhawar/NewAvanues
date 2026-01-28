package com.augmentalis.voiceoscore

import kotlinx.serialization.Serializable

/**
 * Export manifest containing metadata about the export.
 *
 * The manifest serves as a header for export packages, providing
 * version information for compatibility checking, timestamps for
 * tracking export age, and summary statistics.
 *
 * @property version Schema version for backward/forward compatibility
 * @property createdAt Unix timestamp (milliseconds) when export was created
 * @property deviceId Optional device identifier for provenance tracking
 * @property appCount Number of apps included in the export
 * @property totalCommands Total number of commands across all apps
 * @property exportType Classification of the export operation
 */
@Serializable
data class ExportManifest(
    val version: Int = CURRENT_VERSION,
    val createdAt: Long,
    val deviceId: String = "",
    val appCount: Int,
    val totalCommands: Int,
    val exportType: ExportType
) {
    companion object {
        /**
         * Current schema version for export format.
         * Increment when making breaking changes to export structure.
         */
        const val CURRENT_VERSION = 1
    }
}

/**
 * Type of export operation.
 *
 * Used to classify exports for UI display and import validation.
 */
@Serializable
enum class ExportType {
    /**
     * Complete export containing all apps and their commands.
     * Typically used for backup or device migration.
     */
    FULL,

    /**
     * Export containing only one app's commands.
     * Useful for sharing app-specific command sets.
     */
    SINGLE_APP,

    /**
     * Export containing a user-selected subset of apps.
     * Allows selective backup or sharing.
     */
    MULTI_APP
}

/**
 * Complete export package containing all data.
 *
 * This is the root serializable object that gets written to JSON files.
 * It combines the manifest metadata with the actual app/command data.
 *
 * Example JSON structure:
 * ```json
 * {
 *   "manifest": { "version": 1, "createdAt": 1706000000000, ... },
 *   "apps": [
 *     { "packageName": "com.example.app", "commands": [...] }
 *   ]
 * }
 * ```
 *
 * @property manifest Export metadata and summary
 * @property apps List of exported apps with their commands
 */
@Serializable
data class ExportPackage(
    val manifest: ExportManifest,
    val apps: List<AppExportData>
)

/**
 * Exported data for a single app.
 *
 * Contains all information needed to recreate the app's learned
 * commands on another device or after a fresh install.
 *
 * @property packageName Android package name (e.g., "com.spotify.music")
 * @property appName Human-readable app name (e.g., "Spotify")
 * @property versionCode App version code at time of export
 * @property versionName App version name at time of export (e.g., "8.9.0")
 * @property category App category classification name
 * @property commands List of learned voice commands for this app
 * @property screens Screen metadata for command context
 */
@Serializable
data class AppExportData(
    val packageName: String,
    val appName: String,
    val versionCode: Long,
    val versionName: String,
    val category: String,
    val commands: List<CommandExportData>,
    val screens: List<ScreenExportData>
)

/**
 * Exported command data.
 *
 * Represents a single learned voice command with all metadata
 * needed for import and execution.
 *
 * @property avid Unique identifier for the command target element
 * @property phrase Voice phrase that triggers this command
 * @property actionType Type of action (CLICK, SCROLL, etc.) as string
 * @property targetAvid AVID of the target element for the action
 * @property confidence Learning confidence score (0.0-1.0)
 * @property screenHash Hash identifying the screen context
 * @property metadata Additional key-value metadata for the command
 */
@Serializable
data class CommandExportData(
    val avid: String,
    val phrase: String,
    val actionType: String,
    val targetAvid: String,
    val confidence: Float,
    val screenHash: String,
    val metadata: Map<String, String>
)

/**
 * Exported screen data for context.
 *
 * Provides screen-level metadata that helps understand command context
 * and aids in import validation.
 *
 * @property screenHash Unique hash identifying the screen state
 * @property screenType Classification of screen type as string
 * @property elementCount Number of interactive elements on the screen
 * @property staticCommandCount Number of static/built-in commands
 */
@Serializable
data class ScreenExportData(
    val screenHash: String,
    val screenType: String,
    val elementCount: Int,
    val staticCommandCount: Int
)

/**
 * Import strategy options.
 *
 * Defines how to handle conflicts when importing commands that
 * may already exist in the target database.
 */
enum class ImportStrategy {
    /**
     * Add new commands, keep existing ones unchanged.
     * This is the safest and default strategy.
     * - New commands are added
     * - Existing commands remain untouched
     * - Commands with same AVID but different data are skipped
     */
    MERGE,

    /**
     * Replace existing commands for imported apps.
     * Use when you want to fully restore from backup.
     * - Existing commands for imported apps are deleted first
     * - All imported commands are added fresh
     * - Commands for non-imported apps are untouched
     */
    REPLACE,

    /**
     * Only import apps that don't exist in database.
     * Use when adding commands from another device without overwriting.
     * - Apps already in database are completely skipped
     * - Only new apps and their commands are imported
     */
    SKIP_EXISTING
}

/**
 * Result of import operation.
 *
 * Provides detailed statistics about what was imported, skipped,
 * or replaced during the import process.
 *
 * @property success Whether the import completed without critical errors
 * @property appsImported Number of apps processed during import
 * @property commandsImported Number of new commands added
 * @property commandsSkipped Number of commands skipped (duplicates, conflicts)
 * @property commandsReplaced Number of existing commands that were replaced
 * @property errors List of error messages for any failures
 */
data class ImportResult(
    val success: Boolean,
    val appsImported: Int,
    val commandsImported: Int,
    val commandsSkipped: Int,
    val commandsReplaced: Int,
    val errors: List<String>
) {
    /**
     * Total number of commands that were in the import package.
     */
    val totalCommandsProcessed: Int
        get() = commandsImported + commandsSkipped + commandsReplaced

    /**
     * Whether any errors occurred during import.
     */
    val hasErrors: Boolean
        get() = errors.isNotEmpty()

    /**
     * Human-readable summary of the import result.
     */
    fun summary(): String = buildString {
        append(if (success) "Import successful" else "Import failed")
        append(": $appsImported apps, ")
        append("$commandsImported imported, ")
        append("$commandsSkipped skipped, ")
        append("$commandsReplaced replaced")
        if (hasErrors) {
            append(" (${errors.size} errors)")
        }
    }
}

/**
 * Preview of what will be imported before confirmation.
 *
 * Allows users to review the import contents and make informed
 * decisions before committing to the import operation.
 *
 * @property manifest Export package metadata
 * @property apps List of app-level import previews
 */
data class ImportPreview(
    val manifest: ExportManifest,
    val apps: List<AppImportPreview>
) {
    /**
     * Total number of commands that would be imported.
     */
    val totalCommands: Int
        get() = apps.sumOf { it.commandCount }

    /**
     * Number of apps that already exist in the database.
     */
    val existingAppsCount: Int
        get() = apps.count { it.existsInDatabase }

    /**
     * Number of new apps (not in database).
     */
    val newAppsCount: Int
        get() = apps.count { !it.existsInDatabase }
}

/**
 * Preview for a single app import.
 *
 * Shows what will happen to each app during import, including
 * whether it already exists and how many commands it has.
 *
 * @property packageName Android package name of the app
 * @property appName Human-readable app name
 * @property commandCount Number of commands in the export for this app
 * @property existsInDatabase Whether this app already has commands in database
 * @property existingCommandCount Number of commands already in database for this app
 */
data class AppImportPreview(
    val packageName: String,
    val appName: String,
    val commandCount: Int,
    val existsInDatabase: Boolean,
    val existingCommandCount: Int
) {
    /**
     * Whether importing this app would add new commands.
     */
    val hasNewCommands: Boolean
        get() = commandCount > 0 && (!existsInDatabase || commandCount > existingCommandCount)

    /**
     * Potential command count after import with MERGE strategy.
     */
    val potentialTotalCommands: Int
        get() = if (existsInDatabase) {
            maxOf(existingCommandCount, commandCount)
        } else {
            commandCount
        }
}
