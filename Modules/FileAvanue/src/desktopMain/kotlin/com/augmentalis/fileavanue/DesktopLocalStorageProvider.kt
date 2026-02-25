package com.augmentalis.fileavanue

import com.augmentalis.fileavanue.model.FileCategory
import com.augmentalis.fileavanue.model.FileItem
import com.augmentalis.fileavanue.model.PathSegment
import com.augmentalis.fileavanue.model.StorageInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes
import kotlin.streams.toList

/**
 * Desktop local storage provider using java.nio.file.
 *
 * Same pattern as DesktopImageController.loadGallery() (lines 55-77)
 * which walks ~/Pictures with Files.list(). Uses BasicFileAttributes
 * for metadata and Files.probeContentType() for MIME detection with
 * fallback to MimeTypes registry.
 */
class DesktopLocalStorageProvider : IStorageProvider {

    override val providerId: String = "local"
    override val displayName: String = "Local Filesystem"
    override val type: StorageProviderType = StorageProviderType.LOCAL
    override val isConnected: Boolean = true

    override fun getRootPath(): String =
        System.getProperty("user.home") ?: "/"

    override fun getPathSegments(path: String): List<PathSegment> {
        if (path.startsWith("category:")) {
            val categoryName = path.removePrefix("category:")
            return listOf(
                PathSegment("Home", getRootPath()),
                PathSegment(categoryName.lowercase().replaceFirstChar { it.uppercase() }, path)
            )
        }

        val home = getRootPath()
        val segments = mutableListOf(PathSegment("Home", home))
        if (path == home || path.isBlank()) return segments

        val relativePath = if (path.startsWith(home)) {
            path.removePrefix(home).trimStart(File.separatorChar)
        } else {
            // Absolute path outside home — show from root
            segments.clear()
            segments.add(PathSegment("/", "/"))
            path.trimStart(File.separatorChar)
        }

        var currentPath = if (segments.first().uri == "/") "" else home
        for (part in relativePath.split(File.separatorChar)) {
            if (part.isNotEmpty()) {
                currentPath = "$currentPath${File.separator}$part"
                segments.add(PathSegment(part, currentPath))
            }
        }
        return segments
    }

    override suspend fun listFiles(path: String, showHidden: Boolean): List<FileItem> =
        withContext(Dispatchers.IO) {
            try {
                val dirPath = Paths.get(path)
                if (!Files.exists(dirPath) || !Files.isDirectory(dirPath)) {
                    return@withContext emptyList()
                }

                Files.list(dirPath).use { stream ->
                    stream.toList().mapNotNull { p ->
                        if (!showHidden && p.fileName.toString().startsWith(".")) return@mapNotNull null
                        pathToItem(p)
                    }
                }
            } catch (_: Exception) {
                emptyList()
            }
        }

    override suspend fun getFileInfo(uri: String): FileItem? = withContext(Dispatchers.IO) {
        try {
            val p = Paths.get(uri)
            if (Files.exists(p)) pathToItem(p) else null
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun getStorageInfo(): StorageInfo? = withContext(Dispatchers.IO) {
        try {
            val store = Files.getFileStore(Paths.get(getRootPath()))
            StorageInfo(
                totalBytes = store.totalSpace,
                usedBytes = store.totalSpace - store.usableSpace,
                availableBytes = store.usableSpace
            )
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun searchFiles(path: String, query: String): List<FileItem> =
        withContext(Dispatchers.IO) {
            val results = mutableListOf<FileItem>()
            val lowerQuery = query.lowercase()
            searchRecursive(Paths.get(path), lowerQuery, results, maxDepth = 5, currentDepth = 0)
            results
        }

    private fun searchRecursive(
        dir: Path, query: String, results: MutableList<FileItem>,
        maxDepth: Int, currentDepth: Int
    ) {
        if (currentDepth > maxDepth || results.size >= 200) return
        try {
            Files.list(dir).use { stream ->
                stream.toList().forEach { p ->
                    val name = p.fileName?.toString() ?: return@forEach
                    if (name.lowercase().contains(query)) {
                        pathToItem(p)?.let { results.add(it) }
                    }
                    if (Files.isDirectory(p) && !name.startsWith(".")) {
                        searchRecursive(p, query, results, maxDepth, currentDepth + 1)
                    }
                }
            }
        } catch (_: Exception) { /* Permission denied or other I/O error */ }
    }

    override suspend fun getRecentFiles(limit: Int): List<FileItem> = withContext(Dispatchers.IO) {
        val home = Paths.get(getRootPath())
        val results = mutableListOf<FileItem>()
        collectRecentFiles(home, results, maxDepth = 3, currentDepth = 0)
        results.sortedByDescending { it.dateModified }.take(limit)
    }

    private fun collectRecentFiles(
        dir: Path, results: MutableList<FileItem>,
        maxDepth: Int, currentDepth: Int
    ) {
        if (currentDepth > maxDepth || results.size >= 500) return
        try {
            Files.list(dir).use { stream ->
                stream.toList().forEach { p ->
                    val name = p.fileName?.toString() ?: return@forEach
                    if (name.startsWith(".")) return@forEach
                    if (Files.isRegularFile(p)) {
                        pathToItem(p)?.let { results.add(it) }
                    } else if (Files.isDirectory(p)) {
                        collectRecentFiles(p, results, maxDepth, currentDepth + 1)
                    }
                }
            }
        } catch (_: Exception) { /* Permission denied */ }
    }

    override suspend fun getCategoryFiles(category: FileCategory): List<FileItem> =
        withContext(Dispatchers.IO) {
            val home = getRootPath()
            when (category) {
                FileCategory.IMAGES -> collectByMimePrefix(Paths.get(home, "Pictures"), "image/")
                FileCategory.VIDEOS -> collectByMimePrefix(Paths.get(home, "Videos"), "video/")
                FileCategory.AUDIO -> collectByMimePrefix(Paths.get(home, "Music"), "audio/")
                FileCategory.DOCUMENTS -> collectByMimePrefix(Paths.get(home, "Documents"), "")
                FileCategory.DOWNLOADS -> listFiles("$home${File.separator}Downloads", showHidden = false)
                FileCategory.RECENT -> getRecentFiles(30)
            }
        }

    private fun collectByMimePrefix(dir: Path, prefix: String): List<FileItem> {
        if (!Files.exists(dir)) return emptyList()
        val items = mutableListOf<FileItem>()
        try {
            Files.walk(dir, 3).use { stream ->
                stream.filter { Files.isRegularFile(it) }
                    .toList()
                    .forEach { p ->
                        val item = pathToItem(p) ?: return@forEach
                        if (prefix.isEmpty() || item.mimeType.startsWith(prefix)) {
                            items.add(item)
                        }
                    }
            }
        } catch (_: Exception) { /* Permission denied */ }
        return items.sortedByDescending { it.dateModified }
    }

    override suspend fun deleteFile(uri: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Files.deleteIfExists(Paths.get(uri))
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun renameFile(uri: String, newName: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val source = Paths.get(uri)
                Files.move(source, source.resolveSibling(newName))
                true
            } catch (_: Exception) {
                false
            }
        }

    override suspend fun createDirectory(parentPath: String, name: String): FileItem? =
        withContext(Dispatchers.IO) {
            try {
                val dir = Paths.get(parentPath, name)
                Files.createDirectories(dir)
                pathToItem(dir)
            } catch (_: Exception) {
                null
            }
        }

    private fun pathToItem(p: Path): FileItem? {
        return try {
            val attrs = Files.readAttributes(p, BasicFileAttributes::class.java)
            val name = p.fileName?.toString() ?: return null
            val isDir = attrs.isDirectory
            val mime = if (isDir) {
                "inode/directory"
            } else {
                Files.probeContentType(p) ?: MimeTypes.fromFilename(name)
            }

            FileItem(
                uri = p.toAbsolutePath().toString(),
                name = name,
                mimeType = mime,
                fileSizeBytes = if (isDir) 0 else attrs.size(),
                isDirectory = isDir,
                dateCreated = attrs.creationTime().toMillis(),
                dateModified = attrs.lastModifiedTime().toMillis(),
                parentUri = p.parent?.toAbsolutePath()?.toString() ?: "",
                childCount = if (isDir) {
                    try {
                        Files.list(p).use { it.count().toInt() }
                    } catch (_: Exception) { 0 }
                } else -1,
                isHidden = name.startsWith("."),
                providerId = providerId
            )
        } catch (_: Exception) {
            null
        }
    }
}

actual fun createLocalStorageProvider(): IStorageProvider = DesktopLocalStorageProvider()
