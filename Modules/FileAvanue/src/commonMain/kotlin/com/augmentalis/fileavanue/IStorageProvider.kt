package com.augmentalis.fileavanue

import com.augmentalis.fileavanue.model.FileCategory
import com.augmentalis.fileavanue.model.FileItem
import com.augmentalis.fileavanue.model.PathSegment
import com.augmentalis.fileavanue.model.StorageInfo
import kotlinx.serialization.Serializable

/**
 * Storage provider interface — the polymorphic core of FileAvanue.
 *
 * Each implementation represents a genuinely different storage backend:
 * - **Local**: MediaStore (Android), java.nio.file (Desktop), NSFileManager (iOS), File API (Web)
 * - **Cloud**: Google Drive, Dropbox, OneDrive, Box (each with unique REST APIs + OAuth)
 * - **Network**: FTP, FTPS, SFTP (socket-based protocols)
 *
 * This is real polymorphism, not indirection — each backend has fundamentally
 * different I/O patterns, authentication, and capability sets.
 */
interface IStorageProvider {
    /** Unique ID used to tag FileItems and route operations. */
    val providerId: String

    /** Human-readable name for UI display. */
    val displayName: String

    /** Provider category. */
    val type: StorageProviderType

    /** Whether this provider is currently accessible. */
    val isConnected: Boolean

    /** List files/directories at the given path. */
    suspend fun listFiles(path: String, showHidden: Boolean = false): List<FileItem>

    /** Get metadata for a single file. */
    suspend fun getFileInfo(uri: String): FileItem?

    /** Get storage usage info (total/used/available). */
    suspend fun getStorageInfo(): StorageInfo?

    /** Root path for this provider (e.g., "/storage/emulated/0" or "/"). */
    fun getRootPath(): String

    /** Split a path into navigable breadcrumb segments. */
    fun getPathSegments(path: String): List<PathSegment>

    /** Search for files matching a query string. */
    suspend fun searchFiles(path: String, query: String): List<FileItem>

    /** Get recently modified files. */
    suspend fun getRecentFiles(limit: Int = 20): List<FileItem>

    /** Get files matching a category (images, videos, audio, etc.). */
    suspend fun getCategoryFiles(category: FileCategory): List<FileItem>

    /** Delete a file or empty directory. Returns true on success. */
    suspend fun deleteFile(uri: String): Boolean

    /** Rename a file or directory. Returns true on success. */
    suspend fun renameFile(uri: String, newName: String): Boolean

    /** Create a new directory. Returns the created FileItem or null on failure. */
    suspend fun createDirectory(parentPath: String, name: String): FileItem?
}

@Serializable
enum class StorageProviderType {
    LOCAL,
    CLOUD,
    NETWORK,
}
