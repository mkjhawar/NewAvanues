package com.augmentalis.commandmanager

/**
 * Platform-agnostic interface for exporting commands from database.
 *
 * This interface defines the contract for exporting learned voice commands
 * and their associated app data. Implementations are responsible for:
 * - Querying the database for commands and apps
 * - Converting internal data models to export format
 * - Creating properly formatted [ExportPackage] objects
 *
 * The export process does not handle file I/O directly - that is delegated
 * to [IExportFileProvider] to maintain platform independence.
 *
 * ## Usage Example
 *
 * ```kotlin
 * // Export all commands
 * val exporter: ICommandExporter = CommandExporter(repository)
 * val package = exporter.exportAll()
 *
 * // Export specific app
 * val spotifyExport = exporter.exportApp("com.spotify.music")
 *
 * // Export multiple selected apps
 * val selectedExport = exporter.exportApps(listOf(
 *     "com.spotify.music",
 *     "com.google.android.apps.maps"
 * ))
 * ```
 *
 * ## Implementation Notes
 *
 * Implementations should:
 * - Handle empty results gracefully (return empty package, not null)
 * - Include all associated screen data for context
 * - Populate the manifest with accurate counts
 * - Use appropriate [ExportType] based on the operation
 *
 * @see ExportPackage
 * @see ICommandImporter
 * @see IExportFileProvider
 */
interface ICommandExporter {

    /**
     * Export all apps and commands from the database.
     *
     * Creates a complete backup of all learned commands across all apps.
     * The resulting package will have [ExportType.FULL] in its manifest.
     *
     * This operation may be expensive for databases with many apps/commands.
     * Consider showing a progress indicator in the UI.
     *
     * @return [ExportPackage] containing all apps and their commands
     *
     * @throws Exception if database access fails
     */
    suspend fun exportAll(): ExportPackage

    /**
     * Export a single app's commands.
     *
     * Creates an export package containing only the specified app's
     * commands and screen data. Useful for sharing app-specific
     * command sets with other users.
     *
     * The resulting package will have [ExportType.SINGLE_APP] in its manifest.
     *
     * @param packageName Android package name (e.g., "com.spotify.music")
     * @return [ExportPackage] containing the app's commands, or empty package
     *         if app not found
     *
     * @throws Exception if database access fails
     */
    suspend fun exportApp(packageName: String): ExportPackage

    /**
     * Export multiple selected apps.
     *
     * Creates an export package containing commands for the specified
     * apps. Apps not found in the database are silently skipped.
     *
     * The resulting package will have [ExportType.MULTI_APP] in its manifest.
     *
     * @param packageNames List of Android package names to export
     * @return [ExportPackage] containing the selected apps' commands
     *
     * @throws Exception if database access fails
     */
    suspend fun exportApps(packageNames: List<String>): ExportPackage

    /**
     * Get list of apps available for export.
     *
     * Returns summary information about all apps that have learned
     * commands in the database. Useful for building app selection UI.
     *
     * The list is typically sorted by app name or last updated time.
     *
     * @return List of [AppExportSummary] for all exportable apps
     *
     * @throws Exception if database access fails
     */
    suspend fun getExportableApps(): List<AppExportSummary>
}

/**
 * Summary of an app available for export.
 *
 * Provides lightweight information about an app for display in
 * export selection UI without loading full command data.
 *
 * @property packageName Android package name (e.g., "com.spotify.music")
 * @property appName Human-readable app name (e.g., "Spotify")
 * @property commandCount Number of learned commands for this app
 * @property lastUpdated Unix timestamp (milliseconds) of last command update
 */
data class AppExportSummary(
    val packageName: String,
    val appName: String,
    val commandCount: Int,
    val lastUpdated: Long
) {
    /**
     * Whether this app has any commands to export.
     */
    val hasCommands: Boolean
        get() = commandCount > 0

    /**
     * Display string for UI showing app name and command count.
     */
    fun displayString(): String = "$appName ($commandCount commands)"
}
