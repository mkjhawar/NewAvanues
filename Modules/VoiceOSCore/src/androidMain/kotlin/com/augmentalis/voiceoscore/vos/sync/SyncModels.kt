/**
 * SyncModels.kt - Data models for VOS SFTP sync system
 *
 * Contains sealed classes and data classes for the SFTP sync pipeline:
 * result wrappers, auth modes, remote file info, manifests, and sync status.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-02-11
 */
package com.augmentalis.voiceoscore.vos.sync

/**
 * Result wrapper for SFTP operations.
 * Captures either success with a typed value or an error with message + optional cause.
 */
sealed class SftpResult<out T> {
    data class Success<T>(val data: T) : SftpResult<T>()
    data class Error(val message: String, val cause: Throwable? = null) : SftpResult<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error

    fun getOrNull(): T? = (this as? Success)?.data

    fun <R> map(transform: (T) -> R): SftpResult<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> this
    }
}

/**
 * SFTP authentication mode.
 * Supports SSH key file (preferred) or password (fallback for testing).
 */
sealed class SftpAuthMode {
    data class SshKey(val keyFilePath: String, val passphrase: String = "") : SftpAuthMode()
    data class Password(val password: String) : SftpAuthMode()
}

/**
 * Metadata about a remote file on the SFTP server.
 */
data class RemoteFileInfo(
    val name: String,
    val size: Long,
    val modifiedTime: Long
)

/**
 * Server-side manifest listing all available VOS files.
 * Stored as manifest.json at the SFTP remote root.
 */
data class ServerManifest(
    val version: String = "1.0",
    val files: List<ManifestEntry> = emptyList(),
    val lastUpdated: Long = System.currentTimeMillis()
)

/**
 * A single entry in the server manifest.
 * Maps to one .app.vos or .web.vos file on the server.
 */
data class ManifestEntry(
    val hash: String,
    val filename: String,
    val size: Long,
    val uploadedAt: Long,
    val domain: String? = null,
    val locale: String? = null
)

/**
 * Observable sync status for UI binding.
 */
data class SyncStatus(
    val isConnected: Boolean = false,
    val isSyncing: Boolean = false,
    val lastSyncTime: Long? = null,
    val progress: SyncProgress? = null,
    val error: String? = null
)

/**
 * Progress of an ongoing sync operation.
 */
data class SyncProgress(
    val currentFile: String = "",
    val currentIndex: Int = 0,
    val totalFiles: Int = 0,
    val bytesTransferred: Long = 0,
    val totalBytes: Long = 0
) {
    val fraction: Float
        get() = if (totalFiles > 0) currentIndex.toFloat() / totalFiles else 0f
}

/**
 * Final result of a full sync (upload + download).
 */
data class SyncResult(
    val uploadedCount: Int = 0,
    val downloadedCount: Int = 0,
    val errors: List<String> = emptyList()
) {
    val hasErrors: Boolean get() = errors.isNotEmpty()
    val totalTransferred: Int get() = uploadedCount + downloadedCount
}
