/**
 * External Storage Migration Utility
 *
 * Handles migration from old folder names to new .AVAVoiceAvanues structure
 * Preserves hidden/visible state from original folders
 *
 * Created: 2025-11-27
 */

package com.augmentalis.ava.core.common.storage

import android.os.Environment
import timber.log.Timber
import java.io.File

object ExternalStorageMigration {

    // New standard folder name
    private const val NEW_FOLDER_NAME = "AVAVoiceAvanues"
    private const val NEW_FOLDER_HIDDEN = ".AVAVoiceAvanues"

    // Legacy folder names to migrate from
    private val LEGACY_FOLDER_NAMES = listOf(
        "ava-ai-models",      // Old visible
        ".ava-ai-models",     // Old hidden
        "ava-models",         // Potential variant
        ".ava-models"         // Potential variant hidden
    )

    /**
     * Check and migrate legacy folders to new structure
     *
     * @return Pair<File, Boolean> - (new folder path, was migrated)
     */
    fun migrateIfNeeded(): Pair<File, Boolean> {
        val externalStorageRoot = Environment.getExternalStorageDirectory()

        // Check for legacy folders
        for (legacyName in LEGACY_FOLDER_NAMES) {
            val legacyFolder = File(externalStorageRoot, legacyName)

            if (legacyFolder.exists() && legacyFolder.isDirectory) {
                Timber.i("Found legacy folder: ${legacyFolder.absolutePath}")
                return migrateLegacyFolder(legacyFolder, legacyName)
            }
        }

        // No legacy folder found, return new folder (hidden by default)
        val newFolder = File(externalStorageRoot, NEW_FOLDER_HIDDEN)
        return Pair(newFolder, false)
    }

    /**
     * Migrate legacy folder to new name, preserving hidden/visible state
     */
    private fun migrateLegacyFolder(legacyFolder: File, legacyName: String): Pair<File, Boolean> {
        val externalStorageRoot = legacyFolder.parentFile ?: return Pair(legacyFolder, false)

        // Determine if legacy folder was hidden
        val wasHidden = legacyName.startsWith(".")

        // Create new folder name (preserve hidden state)
        val newFolderName = if (wasHidden) NEW_FOLDER_HIDDEN else NEW_FOLDER_NAME
        val newFolder = File(externalStorageRoot, newFolderName)

        // If new folder already exists, don't migrate
        if (newFolder.exists()) {
            Timber.w("New folder already exists, skipping migration: ${newFolder.absolutePath}")
            return Pair(newFolder, false)
        }

        try {
            // Rename folder
            val renamed = legacyFolder.renameTo(newFolder)

            if (renamed) {
                Timber.i("Successfully migrated: $legacyName → $newFolderName")
                Timber.i("Hidden state preserved: $wasHidden")
                return Pair(newFolder, true)
            } else {
                Timber.e("Failed to rename folder: $legacyName")

                // Fallback: copy and delete
                return copyAndDeleteLegacy(legacyFolder, newFolder, wasHidden)
            }

        } catch (e: Exception) {
            Timber.e(e, "Error during migration: $legacyName")
            return Pair(legacyFolder, false)  // Return legacy folder on error
        }
    }

    /**
     * Fallback migration: copy folder contents and delete old folder
     */
    private fun copyAndDeleteLegacy(
        legacyFolder: File,
        newFolder: File,
        wasHidden: Boolean
    ): Pair<File, Boolean> {
        try {
            Timber.i("Attempting copy-and-delete migration")

            // Create new folder
            if (!newFolder.mkdirs()) {
                Timber.e("Failed to create new folder: ${newFolder.absolutePath}")
                return Pair(legacyFolder, false)
            }

            // Copy all contents
            legacyFolder.copyRecursively(newFolder, overwrite = false)

            // Verify copy
            val legacyFiles = legacyFolder.walk().count()
            val newFiles = newFolder.walk().count()

            if (legacyFiles != newFiles) {
                Timber.e("File count mismatch after copy: $legacyFiles → $newFiles")
                return Pair(legacyFolder, false)
            }

            // Delete legacy folder
            val deleted = legacyFolder.deleteRecursively()

            if (deleted) {
                Timber.i("Successfully copied and deleted legacy folder")
                Timber.i("Hidden state preserved: $wasHidden")
                return Pair(newFolder, true)
            } else {
                Timber.w("Copied successfully but failed to delete legacy folder")
                // Return new folder anyway (migration successful, cleanup failed)
                return Pair(newFolder, true)
            }

        } catch (e: Exception) {
            Timber.e(e, "Error during copy-and-delete migration")
            return Pair(legacyFolder, false)
        }
    }

    /**
     * Get the active external storage folder (after migration if needed)
     */
    fun getExternalStorageFolder(): File {
        val (folder, wasMigrated) = migrateIfNeeded()

        if (wasMigrated) {
            Timber.i("Using migrated folder: ${folder.absolutePath}")
        } else {
            Timber.d("Using folder: ${folder.absolutePath}")
        }

        // Ensure folder exists
        if (!folder.exists()) {
            folder.mkdirs()
            Timber.i("Created folder: ${folder.absolutePath}")
        }

        return folder
    }

    /**
     * Get subfolder paths (always hidden)
     */
    fun getEmbeddingsFolder(): File {
        val root = getExternalStorageFolder()
        return File(root, ".embeddings").also {
            if (!it.exists()) it.mkdirs()
        }
    }

    fun getLLMFolder(): File {
        val root = getExternalStorageFolder()
        return File(root, ".llm").also {
            if (!it.exists()) it.mkdirs()
        }
    }

    fun getWakeWordFolder(): File {
        val root = getExternalStorageFolder()
        return File(root, ".wakeword").also {
            if (!it.exists()) it.mkdirs()
        }
    }

    /**
     * Get migration status for logging/debugging
     */
    fun getMigrationStatus(): MigrationStatus {
        val externalStorageRoot = Environment.getExternalStorageDirectory()

        // Check for legacy folders
        val legacyFolders = LEGACY_FOLDER_NAMES.mapNotNull { name ->
            val folder = File(externalStorageRoot, name)
            if (folder.exists()) name to folder.absolutePath else null
        }.toMap()

        // Check for new folders
        val hiddenExists = File(externalStorageRoot, NEW_FOLDER_HIDDEN).exists()
        val visibleExists = File(externalStorageRoot, NEW_FOLDER_NAME).exists()

        return MigrationStatus(
            legacyFolders = legacyFolders,
            newFolderHidden = if (hiddenExists) File(externalStorageRoot, NEW_FOLDER_HIDDEN).absolutePath else null,
            newFolderVisible = if (visibleExists) File(externalStorageRoot, NEW_FOLDER_NAME).absolutePath else null,
            needsMigration = legacyFolders.isNotEmpty()
        )
    }

    data class MigrationStatus(
        val legacyFolders: Map<String, String>,
        val newFolderHidden: String?,
        val newFolderVisible: String?,
        val needsMigration: Boolean
    )
}
