/**
 * CommandExporter.kt - Implementation of ICommandExporter for Phase 5
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-22
 *
 * Exports commands from the database for backup, sharing, or device migration.
 * Works with ICommandPersistence for data access and delegates file I/O to
 * platform-specific IExportFileProvider implementations.
 */

package com.augmentalis.voiceoscore

/**
 * Implementation of [ICommandExporter] for exporting commands from database.
 *
 * Works with the database repositories to extract command data for export.
 * The actual file I/O is handled by platform-specific [IExportFileProvider].
 *
 * ## Architecture
 *
 * This exporter follows a KMP-first approach:
 * - **Data access**: Via [ICommandPersistence] interface
 * - **Package listing**: Via [getPackageNames] function (injected)
 * - **App metadata**: Via [getAppInfo] function (injected)
 * - **File I/O**: Delegated to platform (not handled here)
 *
 * ## Usage Example
 *
 * ```kotlin
 * // Android implementation
 * val exporter = CommandExporter(
 *     commandPersistence = AndroidCommandPersistence(repository),
 *     getPackageNames = { scrapedAppRepository.getAll().map { it.packageName } },
 *     getAppInfo = { packageName -> getAppInfoFromPackageManager(packageName) }
 * )
 *
 * // Export all commands
 * val exportPackage = exporter.exportAll()
 *
 * // Export specific app
 * val spotifyExport = exporter.exportApp("com.spotify.music")
 * ```
 *
 * @property commandPersistence Access to persisted commands
 * @property getPackageNames Function to retrieve all package names with commands
 * @property getAppInfo Function to get app metadata (name, version) for a package
 * @property getAppCategory Function to get app category classification
 */
class CommandExporter(
    private val commandPersistence: ICommandPersistence,
    private val getPackageNames: suspend () -> List<String>,
    private val getAppInfo: suspend (String) -> AppMetadata = { AppMetadata.fromPackageName(it) },
    private val getAppCategory: (String) -> AppCategory = { AppCategoryClassifier.classifyPackage(it) }
) : ICommandExporter {

    override suspend fun exportAll(): ExportPackage {
        val packageNames = getPackageNames()
        val appExports = packageNames.mapNotNull { packageName ->
            exportAppDataSafe(packageName)
        }

        return ExportPackage(
            manifest = ExportManifest(
                createdAt = currentTimeMillis(),
                appCount = appExports.size,
                totalCommands = appExports.sumOf { it.commands.size },
                exportType = ExportType.FULL
            ),
            apps = appExports
        )
    }

    override suspend fun exportApp(packageName: String): ExportPackage {
        val appData = exportAppDataSafe(packageName)

        return ExportPackage(
            manifest = ExportManifest(
                createdAt = currentTimeMillis(),
                appCount = if (appData != null) 1 else 0,
                totalCommands = appData?.commands?.size ?: 0,
                exportType = ExportType.SINGLE_APP
            ),
            apps = listOfNotNull(appData)
        )
    }

    override suspend fun exportApps(packageNames: List<String>): ExportPackage {
        val appExports = packageNames.mapNotNull { packageName ->
            exportAppDataSafe(packageName)
        }

        return ExportPackage(
            manifest = ExportManifest(
                createdAt = currentTimeMillis(),
                appCount = appExports.size,
                totalCommands = appExports.sumOf { it.commands.size },
                exportType = ExportType.MULTI_APP
            ),
            apps = appExports
        )
    }

    override suspend fun getExportableApps(): List<AppExportSummary> {
        val packageNames = getPackageNames()

        return packageNames.mapNotNull { packageName ->
            val commandCount = commandPersistence.countByPackage(packageName)
            if (commandCount <= 0) return@mapNotNull null

            val appInfo = getAppInfo(packageName)

            AppExportSummary(
                packageName = packageName,
                appName = appInfo.appName,
                commandCount = commandCount.toInt(),
                lastUpdated = appInfo.lastUpdated
            )
        }.sortedByDescending { it.commandCount }
    }

    /**
     * Exports app data with error handling.
     * Returns null if the app has no commands or an error occurs.
     */
    private suspend fun exportAppDataSafe(packageName: String): AppExportData? {
        return try {
            val commands = commandPersistence.getByPackage(packageName)
            if (commands.isEmpty()) return null

            exportAppData(packageName, commands)
        } catch (e: Exception) {
            // Log error but don't fail the entire export
            null
        }
    }

    /**
     * Converts a list of QuantizedCommands to AppExportData.
     */
    private suspend fun exportAppData(
        packageName: String,
        commands: List<QuantizedCommand>
    ): AppExportData {
        val appInfo = getAppInfo(packageName)
        val category = getAppCategory(packageName)

        // Convert commands to export format
        val commandExports = commands.map { cmd ->
            CommandExportData(
                avid = cmd.avid,
                phrase = cmd.phrase,
                actionType = cmd.actionType.name,
                targetAvid = cmd.targetAvid ?: "",
                confidence = cmd.confidence,
                screenHash = cmd.metadata["screenHash"] ?: cmd.metadata["screenId"] ?: "",
                metadata = cmd.metadata
            )
        }

        // Group commands by screen to create screen data
        val screens = commands
            .groupBy { it.metadata["screenHash"] ?: it.metadata["screenId"] ?: "unknown" }
            .map { (hash, cmds) ->
                ScreenExportData(
                    screenHash = hash,
                    screenType = cmds.firstOrNull()?.metadata?.get("screenType") ?: ScreenType.UNKNOWN.name,
                    elementCount = cmds.distinctBy { it.targetAvid }.size,
                    staticCommandCount = cmds.size
                )
            }

        return AppExportData(
            packageName = packageName,
            appName = appInfo.appName,
            versionCode = appInfo.versionCode,
            versionName = appInfo.versionName,
            category = category.name,
            commands = commandExports,
            screens = screens
        )
    }
}

/**
 * App metadata used during export.
 *
 * Platform implementations should provide this data from PackageManager (Android)
 * or equivalent APIs on other platforms.
 *
 * @property appName Human-readable app name
 * @property versionCode Numeric version code
 * @property versionName Display version string (e.g., "1.2.3")
 * @property lastUpdated Timestamp of last update/modification
 */
data class AppMetadata(
    val appName: String,
    val versionCode: Long,
    val versionName: String,
    val lastUpdated: Long
) {
    companion object {
        /**
         * Creates AppMetadata from package name using default values.
         * Platform implementations should override [CommandExporter.getAppInfo]
         * to provide actual values from PackageManager.
         */
        fun fromPackageName(packageName: String): AppMetadata {
            return AppMetadata(
                appName = packageName.substringAfterLast(".").replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase() else it.toString()
                },
                versionCode = 0L,
                versionName = "unknown",
                lastUpdated = currentTimeMillis()
            )
        }
    }
}
