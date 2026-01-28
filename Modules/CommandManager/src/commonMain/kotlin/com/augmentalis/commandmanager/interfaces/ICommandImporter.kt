package com.augmentalis.commandmanager

/**
 * Platform-agnostic interface for importing commands to database.
 *
 * This interface defines the contract for importing voice commands from
 * an [ExportPackage]. Implementations are responsible for:
 * - Validating import data format and version compatibility
 * - Checking for conflicts with existing commands
 * - Applying the specified [ImportStrategy]
 * - Persisting imported commands to the database
 *
 * The import process does not handle file I/O directly - reading the export
 * file is delegated to [IExportFileProvider] to maintain platform independence.
 *
 * ## Usage Example
 *
 * ```kotlin
 * // Preview import before committing
 * val importer: ICommandImporter = CommandImporter(repository)
 * val preview = importer.preview(exportPackage)
 *
 * // Show preview to user, then import
 * val result = importer.import(exportPackage, ImportStrategy.MERGE)
 *
 * // Or import only specific apps
 * val selectiveResult = importer.importApps(
 *     exportPackage,
 *     listOf("com.spotify.music"),
 *     ImportStrategy.REPLACE
 * )
 * ```
 *
 * ## Import Strategies
 *
 * - [ImportStrategy.MERGE]: Default strategy. Adds new commands without
 *   modifying existing ones. Safe for incremental updates.
 *
 * - [ImportStrategy.REPLACE]: Deletes existing commands for imported apps
 *   before adding new ones. Use for full restoration from backup.
 *
 * - [ImportStrategy.SKIP_EXISTING]: Only imports apps that don't exist in
 *   the database. Useful when combining commands from multiple sources.
 *
 * ## Version Compatibility
 *
 * Implementations should check [ExportManifest.version] and handle:
 * - Same version: Direct import
 * - Older version: Migration/upgrade if possible
 * - Newer version: Graceful failure with clear error message
 *
 * @see ExportPackage
 * @see ImportStrategy
 * @see ImportResult
 * @see ICommandExporter
 */
interface ICommandImporter {

    /**
     * Preview what will be imported without making changes.
     *
     * Analyzes the export package against the current database state
     * and returns a preview of what would happen during import. This
     * allows users to make informed decisions before committing.
     *
     * The preview includes:
     * - Which apps would be imported
     * - Which apps already exist in the database
     * - How many commands would be added/affected
     *
     * No database modifications are made during preview.
     *
     * @param exportPackage The package to analyze for import
     * @return [ImportPreview] showing what would be imported
     *
     * @throws IllegalArgumentException if export package is invalid or
     *         version is incompatible
     */
    suspend fun preview(exportPackage: ExportPackage): ImportPreview

    /**
     * Import commands using the specified strategy.
     *
     * Imports all apps and commands from the export package into the
     * database using the specified conflict resolution strategy.
     *
     * This operation is atomic at the app level - if importing an app
     * fails, any partial changes for that app are rolled back, but
     * successfully imported apps are preserved.
     *
     * @param exportPackage The package containing commands to import
     * @param strategy How to handle conflicts with existing commands.
     *                 Defaults to [ImportStrategy.MERGE]
     * @return [ImportResult] with counts and any error messages
     *
     * @throws IllegalArgumentException if export package is invalid or
     *         version is incompatible
     */
    suspend fun import(
        exportPackage: ExportPackage,
        strategy: ImportStrategy = ImportStrategy.MERGE
    ): ImportResult

    /**
     * Import specific apps only.
     *
     * Imports only the specified apps from the export package, ignoring
     * all other apps in the package. Apps not found in the package are
     * silently skipped.
     *
     * Useful when:
     * - User wants to selectively restore certain apps
     * - Importing from a FULL export but only need some apps
     * - Combining commands from multiple export sources
     *
     * @param exportPackage The package containing commands to import
     * @param packageNames List of package names to import
     * @param strategy How to handle conflicts with existing commands.
     *                 Defaults to [ImportStrategy.MERGE]
     * @return [ImportResult] with counts and any error messages
     *
     * @throws IllegalArgumentException if export package is invalid or
     *         version is incompatible
     */
    suspend fun importApps(
        exportPackage: ExportPackage,
        packageNames: List<String>,
        strategy: ImportStrategy = ImportStrategy.MERGE
    ): ImportResult
}
