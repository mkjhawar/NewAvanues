package com.augmentalis.voiceoscore

/**
 * Implementation of [ICommandImporter] for importing commands to database.
 *
 * Supports three import strategies:
 * - MERGE: Add new commands, keep existing
 * - REPLACE: Overwrite existing commands for imported apps
 * - SKIP_EXISTING: Only import apps not in database
 *
 * This implementation uses [ICommandPersistence] for database operations,
 * making it platform-agnostic and testable.
 *
 * ## Usage
 *
 * ```kotlin
 * val importer = CommandImporter(persistence)
 *
 * // Preview before import
 * val preview = importer.preview(exportPackage)
 *
 * // Import with merge strategy (default)
 * val result = importer.import(exportPackage, ImportStrategy.MERGE)
 *
 * // Import specific apps only
 * val selectiveResult = importer.importApps(
 *     exportPackage,
 *     listOf("com.spotify.music"),
 *     ImportStrategy.REPLACE
 * )
 * ```
 *
 * @property commandPersistence Access to command persistence layer
 */
class CommandImporter(
    private val commandPersistence: ICommandPersistence
) : ICommandImporter {

    override suspend fun preview(exportPackage: ExportPackage): ImportPreview {
        val appPreviews = exportPackage.apps.map { app ->
            val existingCount = commandPersistence.countByPackage(app.packageName)
            AppImportPreview(
                packageName = app.packageName,
                appName = app.appName,
                commandCount = app.commands.size,
                existsInDatabase = existingCount > 0,
                existingCommandCount = existingCount.toInt()
            )
        }

        return ImportPreview(
            manifest = exportPackage.manifest,
            apps = appPreviews
        )
    }

    override suspend fun import(
        exportPackage: ExportPackage,
        strategy: ImportStrategy
    ): ImportResult {
        return importApps(
            exportPackage,
            exportPackage.apps.map { it.packageName },
            strategy
        )
    }

    override suspend fun importApps(
        exportPackage: ExportPackage,
        packageNames: List<String>,
        strategy: ImportStrategy
    ): ImportResult {
        val errors = mutableListOf<String>()
        var appsImported = 0
        var commandsImported = 0
        var commandsSkipped = 0
        var commandsReplaced = 0

        val appsToImport = exportPackage.apps.filter { it.packageName in packageNames }

        for (app in appsToImport) {
            try {
                val existingCount = commandPersistence.countByPackage(app.packageName)
                val existingCommands = if (strategy == ImportStrategy.MERGE) {
                    commandPersistence.getByPackage(app.packageName)
                } else {
                    emptyList()
                }
                val existingAvids = existingCommands.map { it.avid }.toSet()

                when (strategy) {
                    ImportStrategy.SKIP_EXISTING -> {
                        if (existingCount > 0) {
                            commandsSkipped += app.commands.size
                            continue
                        }
                    }
                    ImportStrategy.REPLACE -> {
                        // Delete existing commands for this app
                        val deletedCount = commandPersistence.deleteByPackage(app.packageName)
                        commandsReplaced += deletedCount
                    }
                    ImportStrategy.MERGE -> {
                        // Will handle individually below
                    }
                }

                val commandsToInsert = mutableListOf<QuantizedCommand>()

                for (cmdData in app.commands) {
                    val command = convertToQuantizedCommand(cmdData, app.packageName)

                    if (strategy == ImportStrategy.MERGE && cmdData.avid in existingAvids) {
                        commandsSkipped++
                        continue
                    }

                    commandsToInsert.add(command)
                    commandsImported++
                }

                // Batch insert for efficiency
                if (commandsToInsert.isNotEmpty()) {
                    commandPersistence.insertBatch(commandsToInsert)
                }

                appsImported++

            } catch (e: Exception) {
                errors.add("Failed to import ${app.packageName}: ${e.message}")
            }
        }

        return ImportResult(
            success = errors.isEmpty(),
            appsImported = appsImported,
            commandsImported = commandsImported,
            commandsSkipped = commandsSkipped,
            commandsReplaced = commandsReplaced,
            errors = errors
        )
    }

    /**
     * Convert export data to QuantizedCommand.
     *
     * Handles action type parsing and adds import-specific metadata.
     *
     * @param data Command export data to convert
     * @param packageName Package name for the command
     * @return QuantizedCommand ready for persistence
     */
    private fun convertToQuantizedCommand(
        data: CommandExportData,
        packageName: String
    ): QuantizedCommand {
        val actionType = CommandActionType.fromString(data.actionType)

        return QuantizedCommand(
            avid = data.avid,
            phrase = data.phrase,
            actionType = actionType,
            targetAvid = data.targetAvid.takeIf { it.isNotBlank() },
            confidence = data.confidence,
            metadata = data.metadata + mapOf(
                "packageName" to packageName,
                "imported" to "true",
                "importedAt" to currentTimeMillis().toString(),
                "screenHash" to data.screenHash
            )
        )
    }
}
