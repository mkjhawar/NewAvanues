package com.augmentalis.fileavanue

import com.augmentalis.fileavanue.model.FileCategory
import com.augmentalis.fileavanue.model.FileItem
import com.augmentalis.fileavanue.model.PathSegment
import com.augmentalis.fileavanue.model.StorageInfo
import platform.Foundation.NSFileManager
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSUserDomainMask
import platform.Foundation.NSFileSize
import platform.Foundation.NSFileModificationDate
import platform.Foundation.NSFileCreationDate
import platform.Foundation.NSDate
import platform.Foundation.NSFileSystemSize
import platform.Foundation.NSFileSystemFreeSize
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.timeIntervalSince1970

/**
 * Darwin (iOS + macOS) local storage provider using NSFileManager.
 *
 * On iOS: scoped to app sandbox + Documents directory.
 * On macOS: has broader filesystem access depending on sandbox entitlements.
 * Same pattern as Foundation's IosFileSystem.kt.
 */
class DarwinLocalStorageProvider : IStorageProvider {

    override val providerId: String = "local"
    override val displayName: String = "Device Storage"
    override val type: StorageProviderType = StorageProviderType.LOCAL
    override val isConnected: Boolean = true

    private val fileManager = NSFileManager.defaultManager

    override fun getRootPath(): String {
        @Suppress("UNCHECKED_CAST")
        val paths = NSSearchPathForDirectoriesInDomains(
            NSDocumentDirectory, NSUserDomainMask, true
        ) as? List<String>
        return paths?.firstOrNull() ?: "/"
    }

    override fun getPathSegments(path: String): List<PathSegment> {
        if (path.startsWith("category:")) {
            val categoryName = path.removePrefix("category:")
            return listOf(
                PathSegment("Documents", getRootPath()),
                PathSegment(categoryName.lowercase().replaceFirstChar { it.uppercase() }, path)
            )
        }

        val root = getRootPath()
        val segments = mutableListOf(PathSegment("Documents", root))
        if (path == root || path.isBlank()) return segments

        val relativePath = path.removePrefix(root).trimStart('/')
        var currentPath = root
        for (part in relativePath.split('/')) {
            if (part.isNotEmpty()) {
                currentPath = "$currentPath/$part"
                segments.add(PathSegment(part, currentPath))
            }
        }
        return segments
    }

    override suspend fun listFiles(path: String, showHidden: Boolean): List<FileItem> {
        val error: kotlinx.cinterop.ObjCObjectVar<platform.Foundation.NSError?>? = null
        val contents = fileManager.contentsOfDirectoryAtPath(path, error) ?: return emptyList()
        @Suppress("UNCHECKED_CAST")
        val names = contents as? List<String> ?: return emptyList()

        return names.mapNotNull { name ->
            if (!showHidden && name.startsWith(".")) return@mapNotNull null
            val fullPath = "$path/$name"
            pathToItem(fullPath, name)
        }
    }

    override suspend fun getFileInfo(uri: String): FileItem? {
        val name = uri.substringAfterLast('/')
        return pathToItem(uri, name)
    }

    override suspend fun getStorageInfo(): StorageInfo? {
        return try {
            val attrs = fileManager.attributesOfFileSystemForPath(getRootPath(), null)
            val total = (attrs?.get(NSFileSystemSize) as? Number)?.toLong() ?: return null
            val free = (attrs[NSFileSystemFreeSize] as? Number)?.toLong() ?: return null
            StorageInfo(totalBytes = total, usedBytes = total - free, availableBytes = free)
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun searchFiles(path: String, query: String): List<FileItem> {
        val results = mutableListOf<FileItem>()
        val lowerQuery = query.lowercase()
        searchRecursive(path, lowerQuery, results, maxDepth = 4, currentDepth = 0)
        return results
    }

    private fun searchRecursive(
        dirPath: String, query: String, results: MutableList<FileItem>,
        maxDepth: Int, currentDepth: Int
    ) {
        if (currentDepth > maxDepth || results.size >= 200) return
        val contents = fileManager.contentsOfDirectoryAtPath(dirPath, null) ?: return
        @Suppress("UNCHECKED_CAST")
        val names = contents as? List<String> ?: return

        for (name in names) {
            if (name.startsWith(".")) continue
            val fullPath = "$dirPath/$name"
            if (name.lowercase().contains(query)) {
                pathToItem(fullPath, name)?.let { results.add(it) }
            }
            // Check if directory for recursion
            val attrs = fileManager.attributesOfItemAtPath(fullPath, null)
            val type = attrs?.get("NSFileType") as? String
            if (type == "NSFileTypeDirectory") {
                searchRecursive(fullPath, query, results, maxDepth, currentDepth + 1)
            }
        }
    }

    override suspend fun getRecentFiles(limit: Int): List<FileItem> {
        val all = mutableListOf<FileItem>()
        collectAllFiles(getRootPath(), all, maxDepth = 3, currentDepth = 0)
        return all.sortedByDescending { it.dateModified }.take(limit)
    }

    private fun collectAllFiles(
        dirPath: String, results: MutableList<FileItem>,
        maxDepth: Int, currentDepth: Int
    ) {
        if (currentDepth > maxDepth || results.size >= 500) return
        val contents = fileManager.contentsOfDirectoryAtPath(dirPath, null) ?: return
        @Suppress("UNCHECKED_CAST")
        val names = contents as? List<String> ?: return

        for (name in names) {
            if (name.startsWith(".")) continue
            val fullPath = "$dirPath/$name"
            val item = pathToItem(fullPath, name) ?: continue
            if (item.isDirectory) {
                collectAllFiles(fullPath, results, maxDepth, currentDepth + 1)
            } else {
                results.add(item)
            }
        }
    }

    override suspend fun getCategoryFiles(category: FileCategory): List<FileItem> {
        val all = mutableListOf<FileItem>()
        collectAllFiles(getRootPath(), all, maxDepth = 3, currentDepth = 0)

        return when (category) {
            FileCategory.IMAGES -> all.filter { it.isImage }
            FileCategory.VIDEOS -> all.filter { it.isVideo }
            FileCategory.AUDIO -> all.filter { it.isAudio }
            FileCategory.DOCUMENTS -> all.filter { it.isDocument || it.isPdf }
            FileCategory.DOWNLOADS -> all
            FileCategory.RECENT -> all.sortedByDescending { it.dateModified }.take(30)
        }
    }

    override suspend fun deleteFile(uri: String): Boolean {
        return try {
            fileManager.removeItemAtPath(uri, null)
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun renameFile(uri: String, newName: String): Boolean {
        return try {
            val parent = uri.substringBeforeLast('/')
            val newPath = "$parent/$newName"
            fileManager.moveItemAtPath(uri, newPath, null)
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun createDirectory(parentPath: String, name: String): FileItem? {
        return try {
            val dirPath = "$parentPath/$name"
            fileManager.createDirectoryAtPath(dirPath, true, null, null)
            pathToItem(dirPath, name)
        } catch (_: Exception) {
            null
        }
    }

    private fun pathToItem(fullPath: String, name: String): FileItem? {
        return try {
            val attrs = fileManager.attributesOfItemAtPath(fullPath, null) ?: return null
            val type = attrs["NSFileType"] as? String
            val isDir = type == "NSFileTypeDirectory"
            val size = (attrs[NSFileSize] as? Number)?.toLong() ?: 0L
            val modDate = (attrs[NSFileModificationDate] as? NSDate)
                ?.timeIntervalSince1970?.toLong()?.times(1000) ?: 0L
            val createDate = (attrs[NSFileCreationDate] as? NSDate)
                ?.timeIntervalSince1970?.toLong()?.times(1000) ?: 0L

            FileItem(
                uri = fullPath,
                name = name,
                mimeType = if (isDir) "inode/directory" else MimeTypes.fromFilename(name),
                fileSizeBytes = if (isDir) 0 else size,
                isDirectory = isDir,
                dateCreated = createDate,
                dateModified = modDate,
                parentUri = fullPath.substringBeforeLast('/'),
                isHidden = name.startsWith("."),
                providerId = providerId
            )
        } catch (_: Exception) {
            null
        }
    }
}

actual fun createLocalStorageProvider(): IStorageProvider = DarwinLocalStorageProvider()
