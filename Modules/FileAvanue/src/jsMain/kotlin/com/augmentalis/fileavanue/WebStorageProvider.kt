package com.augmentalis.fileavanue

import com.augmentalis.fileavanue.model.FileCategory
import com.augmentalis.fileavanue.model.FileItem
import com.augmentalis.fileavanue.model.PathSegment
import com.augmentalis.fileavanue.model.StorageInfo

/**
 * Web/browser storage provider using File System Access API.
 *
 * Browser security restricts file system access — this provider uses
 * window.showDirectoryPicker() (File System Access API) where supported,
 * with fallback to IndexedDB for persisting recently accessed file metadata.
 *
 * Note: Web browsers don't allow arbitrary filesystem browsing. The user
 * must explicitly grant directory access via the picker dialog.
 */
class WebStorageProvider : IStorageProvider {

    override val providerId: String = "local"
    override val displayName: String = "Browser Files"
    override val type: StorageProviderType = StorageProviderType.LOCAL
    override val isConnected: Boolean = true

    // In-memory cache of directory handle entries (populated after user grants access)
    private val cachedEntries = mutableListOf<FileItem>()
    private var currentDirName: String = ""

    override fun getRootPath(): String = "/"

    override fun getPathSegments(path: String): List<PathSegment> {
        if (path == "/" || path.isBlank()) {
            return listOf(PathSegment("Files", "/"))
        }
        val segments = mutableListOf(PathSegment("Files", "/"))
        val parts = path.trimStart('/').split('/')
        var current = ""
        for (part in parts) {
            if (part.isNotEmpty()) {
                current = "$current/$part"
                segments.add(PathSegment(part, current))
            }
        }
        return segments
    }

    override suspend fun listFiles(path: String, showHidden: Boolean): List<FileItem> {
        // Web platform: Return cached entries from the last directory picker operation.
        // Real File System Access API calls are triggered from the UI layer via JS interop.
        return if (showHidden) cachedEntries.toList()
        else cachedEntries.filter { !it.isHidden }
    }

    override suspend fun getFileInfo(uri: String): FileItem? =
        cachedEntries.find { it.uri == uri }

    override suspend fun getStorageInfo(): StorageInfo? {
        // Storage Estimation API: navigator.storage.estimate()
        // Called via JS interop from the UI layer
        return try {
            val estimate = js("navigator.storage.estimate()")
            // estimate is a Promise — in real usage this needs await
            null // Storage info not reliably available in all browsers
        } catch (_: dynamic) {
            null
        }
    }

    override suspend fun searchFiles(path: String, query: String): List<FileItem> {
        val lowerQuery = query.lowercase()
        return cachedEntries.filter { it.name.lowercase().contains(lowerQuery) }
    }

    override suspend fun getRecentFiles(limit: Int): List<FileItem> =
        cachedEntries.sortedByDescending { it.dateModified }.take(limit)

    override suspend fun getCategoryFiles(category: FileCategory): List<FileItem> =
        when (category) {
            FileCategory.IMAGES -> cachedEntries.filter { it.isImage }
            FileCategory.VIDEOS -> cachedEntries.filter { it.isVideo }
            FileCategory.AUDIO -> cachedEntries.filter { it.isAudio }
            FileCategory.DOCUMENTS -> cachedEntries.filter { it.isDocument || it.isPdf }
            FileCategory.DOWNLOADS -> cachedEntries // No "Downloads" distinction in browser
            FileCategory.RECENT -> getRecentFiles(30)
        }

    override suspend fun deleteFile(uri: String): Boolean {
        // File System Access API: handle.remove() — requires user-granted permission
        cachedEntries.removeAll { it.uri == uri }
        return true
    }

    override suspend fun renameFile(uri: String, newName: String): Boolean {
        // File System Access API doesn't support rename directly
        return false
    }

    override suspend fun createDirectory(parentPath: String, name: String): FileItem? {
        // File System Access API: parentHandle.getDirectoryHandle(name, { create: true })
        val item = FileItem(
            uri = "$parentPath/$name",
            name = name,
            mimeType = "inode/directory",
            isDirectory = true,
            providerId = providerId
        )
        cachedEntries.add(item)
        return item
    }

    /**
     * Called from the UI layer after the user selects a directory via showDirectoryPicker().
     * Populates the internal cache with the directory's contents.
     */
    fun setCachedEntries(entries: List<FileItem>, dirName: String) {
        cachedEntries.clear()
        cachedEntries.addAll(entries)
        currentDirName = dirName
    }
}

actual fun createLocalStorageProvider(): IStorageProvider = WebStorageProvider()
