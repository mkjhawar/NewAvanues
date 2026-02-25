package com.augmentalis.fileavanue

import android.content.ContentUris
import android.content.Context
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import com.augmentalis.fileavanue.model.FileCategory
import com.augmentalis.fileavanue.model.FileItem
import com.augmentalis.fileavanue.model.PathSegment
import com.augmentalis.fileavanue.model.StorageInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Android local storage provider with dual browsing modes:
 *
 * 1. **Category mode** (`path = "category:IMAGES"`): Queries MediaStore for media files.
 *    Same pattern as ImageGalleryScreen.queryImages() and VideoGalleryScreen.queryVideos().
 *
 * 2. **Directory mode** (`path = "/storage/emulated/0/Documents"`): Direct java.io.File
 *    listing for arbitrary directory browsing.
 *
 * Storage info via [StatFs] (same as Utilities/FileSystem.android.kt:getAvailableSpace()).
 */
class AndroidLocalStorageProvider(
    private val context: Context
) : IStorageProvider {

    override val providerId: String = "local"
    override val displayName: String = "Device Storage"
    override val type: StorageProviderType = StorageProviderType.LOCAL
    override val isConnected: Boolean = true

    override fun getRootPath(): String =
        Environment.getExternalStorageDirectory().absolutePath

    override fun getPathSegments(path: String): List<PathSegment> {
        if (path.startsWith("category:")) {
            val categoryName = path.removePrefix("category:")
            return listOf(
                PathSegment("Home", ""),
                PathSegment(categoryName.lowercase().replaceFirstChar { it.uppercase() }, path)
            )
        }

        val root = getRootPath()
        val segments = mutableListOf(PathSegment("Storage", root))
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

    override suspend fun listFiles(path: String, showHidden: Boolean): List<FileItem> =
        withContext(Dispatchers.IO) {
            val dir = File(path)
            if (!dir.exists() || !dir.isDirectory) return@withContext emptyList()

            dir.listFiles()?.mapNotNull { file ->
                if (!showHidden && file.isHidden) return@mapNotNull null
                fileToItem(file)
            } ?: emptyList()
        }

    override suspend fun getFileInfo(uri: String): FileItem? = withContext(Dispatchers.IO) {
        val file = File(uri)
        if (file.exists()) fileToItem(file) else null
    }

    override suspend fun getStorageInfo(): StorageInfo? = withContext(Dispatchers.IO) {
        try {
            val stat = StatFs(Environment.getExternalStorageDirectory().absolutePath)
            val total = stat.totalBytes
            val available = stat.availableBytes
            StorageInfo(
                totalBytes = total,
                usedBytes = total - available,
                availableBytes = available
            )
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun searchFiles(path: String, query: String): List<FileItem> =
        withContext(Dispatchers.IO) {
            val results = mutableListOf<FileItem>()
            val lowerQuery = query.lowercase()
            searchRecursive(File(path), lowerQuery, results, maxDepth = 5, currentDepth = 0)
            results
        }

    private fun searchRecursive(
        dir: File, query: String, results: MutableList<FileItem>,
        maxDepth: Int, currentDepth: Int
    ) {
        if (currentDepth > maxDepth || results.size >= 200) return
        val files = dir.listFiles() ?: return
        for (file in files) {
            if (file.name.lowercase().contains(query)) {
                results.add(fileToItem(file))
            }
            if (file.isDirectory && !file.isHidden) {
                searchRecursive(file, query, results, maxDepth, currentDepth + 1)
            }
        }
    }

    override suspend fun getRecentFiles(limit: Int): List<FileItem> = withContext(Dispatchers.IO) {
        queryMediaStore(
            uri = MediaStore.Files.getContentUri("external"),
            projection = MEDIA_PROJECTION,
            sortOrder = "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC",
            limit = limit
        )
    }

    override suspend fun getCategoryFiles(category: FileCategory): List<FileItem> =
        withContext(Dispatchers.IO) {
            when (category) {
                FileCategory.IMAGES -> queryMediaStore(
                    uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection = MEDIA_PROJECTION,
                    sortOrder = "${MediaStore.Images.Media.DATE_MODIFIED} DESC"
                )
                FileCategory.VIDEOS -> queryMediaStore(
                    uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    projection = MEDIA_PROJECTION,
                    sortOrder = "${MediaStore.Video.Media.DATE_MODIFIED} DESC"
                )
                FileCategory.AUDIO -> queryMediaStore(
                    uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    projection = MEDIA_PROJECTION,
                    sortOrder = "${MediaStore.Audio.Media.DATE_MODIFIED} DESC"
                )
                FileCategory.DOCUMENTS -> queryMediaStore(
                    uri = MediaStore.Files.getContentUri("external"),
                    projection = MEDIA_PROJECTION,
                    selection = "${MediaStore.Files.FileColumns.MIME_TYPE} IN (?, ?, ?, ?, ?, ?, ?, ?)",
                    selectionArgs = arrayOf(
                        "application/pdf",
                        "application/msword",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        "application/vnd.ms-excel",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        "application/vnd.ms-powerpoint",
                        "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                        "text/plain"
                    ),
                    sortOrder = "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"
                )
                FileCategory.DOWNLOADS -> {
                    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    listFiles(downloadsDir.absolutePath, showHidden = false)
                }
                FileCategory.RECENT -> getRecentFiles(30)
            }
        }

    override suspend fun deleteFile(uri: String): Boolean = withContext(Dispatchers.IO) {
        try {
            File(uri).delete()
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun renameFile(uri: String, newName: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val file = File(uri)
                val target = File(file.parentFile, newName)
                file.renameTo(target)
            } catch (_: Exception) {
                false
            }
        }

    override suspend fun createDirectory(parentPath: String, name: String): FileItem? =
        withContext(Dispatchers.IO) {
            try {
                val dir = File(parentPath, name)
                if (dir.mkdirs()) fileToItem(dir) else null
            } catch (_: Exception) {
                null
            }
        }

    /**
     * Query MediaStore with standard projection, returning FileItem list.
     * Same pattern as ImageGalleryScreen.queryImages() and VideoGalleryScreen.queryVideos().
     */
    private fun queryMediaStore(
        uri: android.net.Uri,
        projection: Array<String>,
        selection: String? = null,
        selectionArgs: Array<String>? = null,
        sortOrder: String? = null,
        limit: Int = 500
    ): List<FileItem> {
        val items = mutableListOf<FileItem>()

        context.contentResolver.query(
            uri, projection, selection, selectionArgs, sortOrder
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val mimeCol = cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)
            val sizeCol = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
            val dateCol = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)

            var count = 0
            while (cursor.moveToNext() && count < limit) {
                val id = cursor.getLong(idCol)
                val name = cursor.getString(nameCol) ?: continue
                val mime = if (mimeCol >= 0) cursor.getString(mimeCol) ?: "*/*" else "*/*"
                val size = if (sizeCol >= 0) cursor.getLong(sizeCol) else 0L
                val date = if (dateCol >= 0) cursor.getLong(dateCol) * 1000 else 0L

                val contentUri = ContentUris.withAppendedId(uri, id)
                items.add(
                    FileItem(
                        uri = contentUri.toString(),
                        name = name,
                        mimeType = mime,
                        fileSizeBytes = size,
                        dateModified = date,
                        thumbnailUri = contentUri.toString(),
                        providerId = providerId
                    )
                )
                count++
            }
        }
        return items
    }

    private fun fileToItem(file: File): FileItem = FileItem(
        uri = file.absolutePath,
        name = file.name,
        mimeType = if (file.isDirectory) "inode/directory" else MimeTypes.fromFilename(file.name),
        fileSizeBytes = if (file.isFile) file.length() else 0,
        isDirectory = file.isDirectory,
        dateCreated = file.lastModified(),
        dateModified = file.lastModified(),
        parentUri = file.parent ?: "",
        childCount = if (file.isDirectory) (file.listFiles()?.size ?: 0) else -1,
        isHidden = file.isHidden,
        providerId = providerId
    )

    companion object {
        private val MEDIA_PROJECTION = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_MODIFIED,
        )
    }
}

/** Android actual — needs Context, so the factory takes it as a lateinit. */
private var appContext: Context? = null

/**
 * Must be called once (e.g., in Application.onCreate or activity) before using FileAvanue.
 */
fun initFileAvanue(context: Context) {
    appContext = context.applicationContext
}

actual fun createLocalStorageProvider(): IStorageProvider {
    val ctx = appContext ?: throw IllegalStateException(
        "FileAvanue not initialized. Call initFileAvanue(context) first."
    )
    return AndroidLocalStorageProvider(ctx)
}
