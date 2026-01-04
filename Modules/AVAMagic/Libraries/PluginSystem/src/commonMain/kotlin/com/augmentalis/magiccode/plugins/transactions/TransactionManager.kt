package com.augmentalis.magiccode.plugins.transactions

import com.augmentalis.magiccode.plugins.core.*
import com.augmentalis.magiccode.plugins.platform.FileIO
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Transaction manager for atomic plugin operations.
 *
 * Provides checkpoint-based rollback for install/update/uninstall operations.
 */
class TransactionManager(
    private val fileIO: FileIO,
    private val registry: PluginRegistry,
    private val pluginDirectory: String,
    private val backupDirectory: String
) {
    private val mutex = Mutex()
    private val checkpoints = mutableMapOf<String, Checkpoint>()

    companion object {
        private const val TAG = "TransactionManager"
        private const val MAX_BACKUP_SIZE_MB = 500L // Maximum backup size per transaction
        private const val BACKUP_CLEANUP_DELAY_MS = 60000L // Keep backups for 1 minute after commit/rollback
    }

    /**
     * File metadata for snapshot.
     */
    data class FileMetadata(
        val relativePath: String,
        val size: Long,
        val lastModified: Long,
        val isDirectory: Boolean
    )

    /**
     * Filesystem snapshot capturing state at checkpoint time.
     */
    data class FilesystemSnapshot(
        val pluginId: String,
        val rootPath: String,
        val files: List<FileMetadata>,
        val backupPath: String,
        val totalSize: Long
    )

    /**
     * Registry snapshot capturing plugin registry state.
     */
    data class RegistrySnapshot(
        val pluginInfo: PluginRegistry.PluginInfo?,
        val wasRegistered: Boolean
    )

    /**
     * Checkpoint data combining filesystem and registry snapshots.
     */
    data class Checkpoint(
        val transactionId: String,
        val pluginId: String,
        val type: TransactionType,
        val timestamp: Long,
        val state: PluginState,
        val filesystemSnapshot: FilesystemSnapshot?,
        val registrySnapshot: RegistrySnapshot,
        val pluginPath: String
    )

    /**
     * Transaction result.
     */
    sealed class TransactionResult {
        data class Success(val transactionId: String) : TransactionResult()
        data class Failure(val reason: String) : TransactionResult()
    }

    /**
     * Create checkpoint before operation.
     *
     * @param pluginId Plugin identifier
     * @param type Transaction type
     * @param currentState Current plugin state
     * @return Transaction ID
     * @throws InsufficientDiskSpaceException if backup cannot be created due to disk space
     */
    suspend fun createCheckpoint(
        pluginId: String,
        type: TransactionType,
        currentState: PluginState
    ): String {
        val transactionId = generateTransactionId(pluginId, type)
        val pluginPath = "$pluginDirectory/$pluginId"

        PluginLog.i(TAG, "Creating checkpoint: $transactionId for $pluginId")

        // Capture registry snapshot
        val registrySnapshot = captureRegistrySnapshot(pluginId)

        // Capture filesystem snapshot (if plugin directory exists)
        val filesystemSnapshot = if (fileIO.directoryExists(pluginPath)) {
            captureFilesystemSnapshot(pluginId, pluginPath, transactionId)
        } else {
            null
        }

        mutex.withLock {
            checkpoints[transactionId] = Checkpoint(
                transactionId = transactionId,
                pluginId = pluginId,
                type = type,
                timestamp = System.currentTimeMillis(),
                state = currentState,
                filesystemSnapshot = filesystemSnapshot,
                registrySnapshot = registrySnapshot,
                pluginPath = pluginPath
            )
        }

        PluginLog.i(TAG, "Created checkpoint: $transactionId for $pluginId (backup: ${filesystemSnapshot?.backupPath ?: "none"})")
        return transactionId
    }

    /**
     * Capture current registry state for a plugin.
     */
    private suspend fun captureRegistrySnapshot(pluginId: String): RegistrySnapshot {
        val pluginInfo = registry.getPlugin(pluginId)
        return RegistrySnapshot(
            pluginInfo = pluginInfo,
            wasRegistered = pluginInfo != null
        )
    }

    /**
     * Capture filesystem snapshot by backing up plugin directory.
     *
     * @throws InsufficientDiskSpaceException if insufficient disk space for backup
     */
    private fun captureFilesystemSnapshot(
        pluginId: String,
        pluginPath: String,
        transactionId: String
    ): FilesystemSnapshot {
        // List all files in plugin directory
        val relativeFiles = fileIO.listFilesRecursive(pluginPath)

        // Calculate total size
        var totalSize = 0L
        val fileMetadata = mutableListOf<FileMetadata>()

        for (relativePath in relativeFiles) {
            val fullPath = "$pluginPath/$relativePath"
            val size = fileIO.getFileSize(fullPath)
            val lastModified = fileIO.getLastModified(fullPath)

            fileMetadata.add(FileMetadata(
                relativePath = relativePath,
                size = size,
                lastModified = lastModified,
                isDirectory = false
            ))

            totalSize += size
        }

        // Check if backup size is reasonable
        val maxBackupBytes = MAX_BACKUP_SIZE_MB * 1024 * 1024
        if (totalSize > maxBackupBytes) {
            PluginLog.w(TAG, "Backup size ${totalSize / 1024 / 1024}MB exceeds limit ${MAX_BACKUP_SIZE_MB}MB")
        }

        // Check available disk space (need at least 2x the backup size for safety)
        val availableSpace = fileIO.getAvailableDiskSpace(backupDirectory)
        if (availableSpace != -1L && availableSpace < totalSize * 2) {
            throw InsufficientDiskSpaceException(
                "Insufficient disk space for backup. Required: ${totalSize * 2 / 1024 / 1024}MB, Available: ${availableSpace / 1024 / 1024}MB"
            )
        }

        // Create backup directory
        val backupPath = "$backupDirectory/$transactionId"
        if (!fileIO.createDirectory(backupPath)) {
            throw BackupCreationException("Failed to create backup directory: $backupPath")
        }

        // Copy plugin directory to backup
        val success = fileIO.copy(pluginPath, backupPath)
        if (!success) {
            // Clean up failed backup
            fileIO.delete(backupPath)
            throw BackupCreationException("Failed to copy plugin directory to backup")
        }

        PluginLog.d(TAG, "Created filesystem backup: $backupPath (${totalSize / 1024}KB, ${fileMetadata.size} files)")

        return FilesystemSnapshot(
            pluginId = pluginId,
            rootPath = pluginPath,
            files = fileMetadata,
            backupPath = backupPath,
            totalSize = totalSize
        )
    }

    /**
     * Commit transaction (remove checkpoint and clean up backup).
     *
     * @param transactionId Transaction identifier
     */
    suspend fun commit(transactionId: String) {
        val checkpoint = mutex.withLock {
            checkpoints.remove(transactionId)
        }

        if (checkpoint != null) {
            // Clean up backup directory
            checkpoint.filesystemSnapshot?.let { snapshot ->
                if (fileIO.directoryExists(snapshot.backupPath)) {
                    val deleted = fileIO.delete(snapshot.backupPath)
                    if (deleted) {
                        PluginLog.d(TAG, "Cleaned up backup: ${snapshot.backupPath}")
                    } else {
                        PluginLog.w(TAG, "Failed to clean up backup: ${snapshot.backupPath}")
                    }
                }
            }
            PluginLog.i(TAG, "Committed transaction: $transactionId")
        } else {
            PluginLog.w(TAG, "Cannot commit - checkpoint not found: $transactionId")
        }
    }

    /**
     * Rollback transaction to checkpoint.
     *
     * Restores both filesystem and registry state to the checkpoint.
     * Implements partial rollback recovery if rollback fails midway.
     *
     * @param transactionId Transaction identifier
     * @return TransactionResult
     */
    suspend fun rollback(transactionId: String): TransactionResult {
        val checkpoint = mutex.withLock {
            checkpoints.remove(transactionId)
        }

        if (checkpoint == null) {
            return TransactionResult.Failure("Checkpoint not found: $transactionId")
        }

        PluginLog.i(TAG, "Rolling back transaction: $transactionId")

        val errors = mutableListOf<String>()

        try {
            // Step 1: Restore filesystem state
            val filesystemRestored = restoreFilesystemState(checkpoint, errors)

            // Step 2: Restore registry state
            val registryRestored = restoreRegistryState(checkpoint, errors)

            // Step 3: Clean up backup
            cleanupBackup(checkpoint)

            // Determine result
            return when {
                filesystemRestored && registryRestored -> {
                    PluginLog.i(TAG, "Successfully rolled back transaction: $transactionId")
                    TransactionResult.Success(transactionId)
                }
                errors.isNotEmpty() -> {
                    val errorMsg = "Rollback completed with errors: ${errors.joinToString("; ")}"
                    PluginLog.e(TAG, errorMsg)
                    TransactionResult.Failure(errorMsg)
                }
                else -> {
                    val errorMsg = "Rollback failed for unknown reason"
                    PluginLog.e(TAG, errorMsg)
                    TransactionResult.Failure(errorMsg)
                }
            }

        } catch (e: Exception) {
            val errorMsg = "Rollback failed with exception: ${e.message}"
            PluginLog.e(TAG, errorMsg, e)

            // Attempt emergency cleanup
            try {
                cleanupBackup(checkpoint)
            } catch (cleanupEx: Exception) {
                PluginLog.e(TAG, "Emergency cleanup also failed: ${cleanupEx.message}", cleanupEx)
            }

            return TransactionResult.Failure(errorMsg)
        }
    }

    /**
     * Restore filesystem to checkpoint state.
     *
     * Strategy:
     * - Delete current plugin directory
     * - Restore from backup (if backup exists)
     * - If no backup, plugin directory remains deleted (for new installs)
     *
     * @return true if restoration succeeded (or was not needed), false if errors occurred
     */
    private fun restoreFilesystemState(checkpoint: Checkpoint, errors: MutableList<String>): Boolean {
        val pluginPath = checkpoint.pluginPath
        val snapshot = checkpoint.filesystemSnapshot

        try {
            // Delete current plugin directory if it exists
            if (fileIO.directoryExists(pluginPath) || fileIO.fileExists(pluginPath)) {
                PluginLog.d(TAG, "Deleting current plugin directory: $pluginPath")
                val deleted = fileIO.delete(pluginPath)
                if (!deleted) {
                    errors.add("Failed to delete current plugin directory: $pluginPath")
                    PluginLog.w(TAG, "Failed to delete plugin directory during rollback: $pluginPath")
                    // Continue anyway - try to restore
                }
            }

            // Restore from backup if snapshot exists
            if (snapshot != null) {
                if (!fileIO.directoryExists(snapshot.backupPath)) {
                    errors.add("Backup directory not found: ${snapshot.backupPath}")
                    PluginLog.e(TAG, "Cannot restore - backup missing: ${snapshot.backupPath}")
                    return false
                }

                PluginLog.d(TAG, "Restoring from backup: ${snapshot.backupPath} -> $pluginPath")
                val restored = fileIO.copy(snapshot.backupPath, pluginPath)

                if (!restored) {
                    errors.add("Failed to restore from backup: ${snapshot.backupPath}")
                    PluginLog.e(TAG, "Failed to restore from backup")
                    return false
                }

                // Verify restoration
                val restoredFiles = fileIO.listFilesRecursive(pluginPath)
                val expectedFileCount = snapshot.files.size

                if (restoredFiles.size != expectedFileCount) {
                    val warning = "File count mismatch after restore. Expected: $expectedFileCount, Got: ${restoredFiles.size}"
                    errors.add(warning)
                    PluginLog.w(TAG, warning)
                    // Don't fail - partial restoration is better than nothing
                }

                PluginLog.i(TAG, "Restored ${restoredFiles.size} files from backup")
                return true

            } else {
                // No backup means plugin didn't exist before - directory deletion is the correct rollback
                PluginLog.d(TAG, "No backup to restore - plugin was newly created")
                return true
            }

        } catch (e: Exception) {
            val error = "Filesystem restoration failed: ${e.message}"
            errors.add(error)
            PluginLog.e(TAG, error, e)
            return false
        }
    }

    /**
     * Restore registry to checkpoint state.
     *
     * Strategy:
     * - If plugin was registered: restore to previous state
     * - If plugin was not registered: unregister it
     *
     * @return true if restoration succeeded, false if errors occurred
     */
    private suspend fun restoreRegistryState(checkpoint: Checkpoint, errors: MutableList<String>): Boolean {
        val snapshot = checkpoint.registrySnapshot

        try {
            if (snapshot.wasRegistered) {
                // Plugin was registered - restore it
                val previousInfo = snapshot.pluginInfo
                if (previousInfo != null) {
                    PluginLog.d(TAG, "Restoring plugin to registry: ${checkpoint.pluginId}")

                    // Re-register with previous state
                    val registered = registry.register(previousInfo.manifest, previousInfo.namespace)

                    if (registered) {
                        // Update to previous state
                        registry.updateState(checkpoint.pluginId, previousInfo.state)
                        PluginLog.i(TAG, "Restored plugin registry state: ${checkpoint.pluginId} -> ${previousInfo.state}")
                        return true
                    } else {
                        // Plugin already exists - just update state
                        val updated = registry.updateState(checkpoint.pluginId, previousInfo.state)
                        if (updated) {
                            PluginLog.i(TAG, "Updated plugin registry state: ${checkpoint.pluginId} -> ${previousInfo.state}")
                            return true
                        } else {
                            errors.add("Failed to update plugin state in registry")
                            PluginLog.w(TAG, "Failed to update plugin state during rollback")
                            return false
                        }
                    }
                } else {
                    errors.add("Plugin was registered but info is null - inconsistent state")
                    PluginLog.e(TAG, "Inconsistent registry snapshot - wasRegistered=true but info=null")
                    return false
                }
            } else {
                // Plugin was not registered - unregister it
                PluginLog.d(TAG, "Unregistering plugin from registry: ${checkpoint.pluginId}")
                val unregistered = registry.unregister(checkpoint.pluginId)

                if (!unregistered) {
                    // Plugin might not be registered - that's okay
                    PluginLog.d(TAG, "Plugin was not in registry (expected for rollback)")
                }

                return true
            }

        } catch (e: Exception) {
            val error = "Registry restoration failed: ${e.message}"
            errors.add(error)
            PluginLog.e(TAG, error, e)
            return false
        }
    }

    /**
     * Clean up backup directory after rollback.
     *
     * Non-critical operation - failures are logged but don't affect rollback result.
     */
    private fun cleanupBackup(checkpoint: Checkpoint) {
        checkpoint.filesystemSnapshot?.let { snapshot ->
            try {
                if (fileIO.directoryExists(snapshot.backupPath)) {
                    val deleted = fileIO.delete(snapshot.backupPath)
                    if (deleted) {
                        PluginLog.d(TAG, "Cleaned up backup after rollback: ${snapshot.backupPath}")
                    } else {
                        PluginLog.w(TAG, "Failed to clean up backup (non-critical): ${snapshot.backupPath}")
                    }
                }
            } catch (e: Exception) {
                PluginLog.w(TAG, "Exception during backup cleanup (non-critical): ${e.message}", e)
            }
        }
    }

    /**
     * Generate unique transaction ID.
     */
    private fun generateTransactionId(pluginId: String, type: TransactionType): String {
        return "${pluginId}_${type.name}_${System.currentTimeMillis()}"
    }

    /**
     * Get all active checkpoints (for debugging/monitoring).
     */
    suspend fun getActiveCheckpoints(): List<Checkpoint> {
        return mutex.withLock {
            checkpoints.values.toList()
        }
    }

    /**
     * Check if a transaction exists.
     */
    suspend fun hasCheckpoint(transactionId: String): Boolean {
        return mutex.withLock {
            checkpoints.containsKey(transactionId)
        }
    }

    /**
     * Clean up old checkpoints that exceed a time threshold.
     *
     * Useful for recovering from crashes where commit/rollback wasn't called.
     *
     * @param maxAgeMs Maximum age in milliseconds (default: 1 hour)
     * @return Number of checkpoints cleaned up
     */
    suspend fun cleanupStaleCheckpoints(maxAgeMs: Long = 3600000L): Int {
        val now = System.currentTimeMillis()
        val staleCheckpoints = mutableListOf<Checkpoint>()

        mutex.withLock {
            val iterator = checkpoints.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                val checkpoint = entry.value
                if (now - checkpoint.timestamp > maxAgeMs) {
                    staleCheckpoints.add(checkpoint)
                    iterator.remove()
                }
            }
        }

        // Clean up backups outside the lock
        var cleanedCount = 0
        for (checkpoint in staleCheckpoints) {
            try {
                cleanupBackup(checkpoint)
                cleanedCount++
                PluginLog.i(TAG, "Cleaned up stale checkpoint: ${checkpoint.transactionId}")
            } catch (e: Exception) {
                PluginLog.w(TAG, "Failed to clean up stale checkpoint: ${checkpoint.transactionId}", e)
            }
        }

        if (cleanedCount > 0) {
            PluginLog.i(TAG, "Cleaned up $cleanedCount stale checkpoints")
        }

        return cleanedCount
    }
}

/**
 * Exception thrown when there's insufficient disk space for backup.
 */
class InsufficientDiskSpaceException(message: String) : Exception(message)

/**
 * Exception thrown when backup creation fails.
 */
class BackupCreationException(message: String) : Exception(message)
