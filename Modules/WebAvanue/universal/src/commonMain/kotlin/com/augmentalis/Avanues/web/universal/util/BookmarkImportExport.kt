package com.augmentalis.Avanues.web.universal.util

import com.augmentalis.webavanue.domain.model.Favorite
import com.augmentalis.webavanue.domain.model.FavoriteFolder
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * BookmarkImportExport - Utility for importing and exporting bookmarks in Netscape HTML format.
 *
 * Supports the standard Netscape Bookmark File Format 1 used by:
 * - Google Chrome
 * - Mozilla Firefox
 * - Safari
 * - Microsoft Edge
 * - Opera
 *
 * Format specification:
 * ```html
 * <!DOCTYPE NETSCAPE-Bookmark-file-1>
 * <META HTTP-EQUIV="Content-Type" CONTENT="text/html; charset=UTF-8">
 * <TITLE>Bookmarks</TITLE>
 * <H1>Bookmarks</H1>
 * <DL><p>
 *     <DT><A HREF="url" ADD_DATE="timestamp">Title</A>
 *     <DT><H3 ADD_DATE="timestamp">Folder Name</H3>
 *     <DL><p>
 *         <DT><A HREF="url" ADD_DATE="timestamp">Title</A>
 *     </DL><p>
 * </DL><p>
 * ```
 *
 * Features:
 * - Export all bookmarks with folder structure
 * - Import from standard HTML bookmark files
 * - Duplicate URL detection
 * - Folder hierarchy support
 * - Preserves creation timestamps
 *
 * @since 1.0.0
 */
object BookmarkImportExport {

    /**
     * Result of bookmark import operation
     *
     * @property imported Number of bookmarks successfully imported
     * @property skipped Number of bookmarks skipped (duplicates)
     * @property folders Number of folders created
     * @property errors List of error messages encountered during import
     */
    data class ImportResult(
        val imported: Int,
        val skipped: Int,
        val folders: Int,
        val errors: List<String> = emptyList()
    ) {
        val total: Int get() = imported + skipped
        val hasErrors: Boolean get() = errors.isNotEmpty()
    }

    /**
     * Exports bookmarks to Netscape HTML format.
     *
     * Generates a standard Netscape Bookmark File Format 1 HTML document containing
     * all bookmarks organized by folders. The output is compatible with all major browsers.
     *
     * @param favorites List of favorites to export
     * @param folders List of favorite folders for organization
     * @return HTML string in Netscape bookmark format
     *
     * @sample
     * ```kotlin
     * val html = BookmarkImportExport.exportToHtml(allFavorites, allFolders)
     * // Save html to file...
     * ```
     */
    fun exportToHtml(
        favorites: List<Favorite>,
        folders: List<FavoriteFolder>
    ): String {
        val html = StringBuilder()

        // HTML Header
        html.appendLine("<!DOCTYPE NETSCAPE-Bookmark-file-1>")
        html.appendLine("<META HTTP-EQUIV=\"Content-Type\" CONTENT=\"text/html; charset=UTF-8\">")
        html.appendLine("<TITLE>Bookmarks</TITLE>")
        html.appendLine("<H1>Bookmarks</H1>")
        html.appendLine("<DL><p>")

        // Create folder hierarchy map
        val folderMap = folders.associateBy { it.id }
        val favoritesByFolder = favorites.groupBy { it.folderId }

        // Export root-level bookmarks (no folder)
        val rootBookmarks = favoritesByFolder[null] ?: emptyList()
        rootBookmarks.forEach { favorite ->
            html.appendLine(formatBookmarkEntry(favorite, indent = 1))
        }

        // Export folders and their bookmarks
        folders.filter { it.parentId == null }.forEach { folder ->
            html.append(formatFolderEntry(folder, favoritesByFolder, folderMap, indent = 1))
        }

        // Close root DL
        html.appendLine("</DL><p>")

        return html.toString()
    }

    /**
     * Formats a single bookmark entry as HTML.
     *
     * @param favorite The favorite to format
     * @param indent Indentation level for readability
     * @return Formatted HTML bookmark entry
     */
    private fun formatBookmarkEntry(favorite: Favorite, indent: Int): String {
        val indentStr = "    ".repeat(indent)
        val timestamp = favorite.createdAt.epochSeconds
        val title = escapeHtml(favorite.title)
        val url = escapeHtml(favorite.url)

        return "${indentStr}<DT><A HREF=\"$url\" ADD_DATE=\"$timestamp\">$title</A>"
    }

    /**
     * Formats a folder and its contents recursively.
     *
     * @param folder The folder to format
     * @param favoritesByFolder Map of favorites grouped by folder ID
     * @param folderMap Map of folders by ID for hierarchy
     * @param indent Current indentation level
     * @return Formatted HTML folder with nested bookmarks
     */
    private fun formatFolderEntry(
        folder: FavoriteFolder,
        favoritesByFolder: Map<String?, List<Favorite>>,
        folderMap: Map<String, FavoriteFolder>,
        indent: Int
    ): String {
        val html = StringBuilder()
        val indentStr = "    ".repeat(indent)
        val timestamp = folder.createdAt.epochSeconds
        val folderName = escapeHtml(folder.name)

        // Folder header
        html.appendLine("${indentStr}<DT><H3 ADD_DATE=\"$timestamp\">$folderName</H3>")
        html.appendLine("${indentStr}<DL><p>")

        // Bookmarks in this folder
        val bookmarksInFolder = favoritesByFolder[folder.id] ?: emptyList()
        bookmarksInFolder.forEach { favorite ->
            html.appendLine(formatBookmarkEntry(favorite, indent + 1))
        }

        // Nested folders (if any)
        val childFolders = folderMap.values.filter { it.parentId == folder.id }
        childFolders.forEach { childFolder ->
            html.append(formatFolderEntry(childFolder, favoritesByFolder, folderMap, indent + 1))
        }

        // Close folder
        html.appendLine("${indentStr}</DL><p>")

        return html.toString()
    }

    /**
     * Imports bookmarks from Netscape HTML format.
     *
     * Parses a standard Netscape bookmark file and extracts bookmarks and folders.
     * Handles duplicate URLs by skipping them (can be customized via skipDuplicates parameter).
     *
     * @param html HTML content in Netscape bookmark format
     * @param existingUrls Set of URLs already bookmarked (for duplicate detection)
     * @param skipDuplicates If true, skip bookmarks with duplicate URLs; if false, import anyway
     * @return ImportResult containing imported bookmarks, folders, and statistics
     *
     * @throws IllegalArgumentException if HTML is not valid bookmark format
     *
     * @sample
     * ```kotlin
     * val html = readBookmarkFile()
     * val existingUrls = existingFavorites.map { it.url }.toSet()
     * val result = BookmarkImportExport.importFromHtml(html, existingUrls)
     * println("Imported: ${result.imported}, Skipped: ${result.skipped}")
     * ```
     */
    fun importFromHtml(
        html: String,
        existingUrls: Set<String> = emptySet(),
        skipDuplicates: Boolean = true
    ): ImportResult {
        val favorites = mutableListOf<Favorite>()
        val folders = mutableListOf<FavoriteFolder>()
        val errors = mutableListOf<String>()
        var skipped = 0

        try {
            // Parse HTML line by line
            val lines = html.lines()
            var currentFolder: FavoriteFolder? = null
            val folderStack = mutableListOf<FavoriteFolder?>()

            for (line in lines) {
                val trimmed = line.trim()

                when {
                    // Bookmark entry: <DT><A HREF="url" ...>Title</A>
                    trimmed.startsWith("<DT><A HREF=", ignoreCase = true) -> {
                        try {
                            val bookmark = parseBookmarkEntry(trimmed, currentFolder?.id)
                            if (bookmark != null) {
                                if (skipDuplicates && existingUrls.contains(bookmark.url)) {
                                    skipped++
                                } else {
                                    favorites.add(bookmark)
                                }
                            }
                        } catch (e: Exception) {
                            errors.add("Failed to parse bookmark: ${e.message}")
                        }
                    }

                    // Folder entry: <DT><H3 ...>Folder Name</H3>
                    trimmed.startsWith("<DT><H3", ignoreCase = true) -> {
                        try {
                            val folder = parseFolderEntry(trimmed, currentFolder?.id)
                            if (folder != null) {
                                folders.add(folder)
                                folderStack.add(currentFolder)
                                currentFolder = folder
                            }
                        } catch (e: Exception) {
                            errors.add("Failed to parse folder: ${e.message}")
                        }
                    }

                    // Close folder: </DL>
                    trimmed.startsWith("</DL>", ignoreCase = true) -> {
                        if (folderStack.isNotEmpty()) {
                            currentFolder = folderStack.removeLastOrNull()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            errors.add("Import failed: ${e.message}")
        }

        return ImportResult(
            imported = favorites.size,
            skipped = skipped,
            folders = folders.size,
            errors = errors
        )
    }

    /**
     * Parses a bookmark entry from HTML line.
     *
     * Expected format: `<DT><A HREF="url" ADD_DATE="timestamp">Title</A>`
     *
     * @param line HTML line containing bookmark
     * @param folderId ID of the containing folder (null for root)
     * @return Parsed Favorite or null if parsing fails
     */
    private fun parseBookmarkEntry(line: String, folderId: String?): Favorite? {
        try {
            // Extract URL
            val urlMatch = Regex("""HREF="([^"]+)"""", RegexOption.IGNORE_CASE)
                .find(line) ?: return null
            val url = unescapeHtml(urlMatch.groupValues[1])

            // Extract title (between >< tags)
            val titleMatch = Regex(""">([^<]+)</A>""", RegexOption.IGNORE_CASE)
                .find(line) ?: return null
            val title = unescapeHtml(titleMatch.groupValues[1])

            // Extract timestamp (optional)
            val timestamp = Regex("""ADD_DATE="(\d+)"""", RegexOption.IGNORE_CASE)
                .find(line)?.groupValues?.get(1)?.toLongOrNull()

            val createdAt = if (timestamp != null) {
                Instant.fromEpochSeconds(timestamp)
            } else {
                Clock.System.now()
            }

            return Favorite.create(
                url = url,
                title = title,
                folderId = folderId
            ).copy(createdAt = createdAt, lastModifiedAt = createdAt)
        } catch (e: Exception) {
            return null
        }
    }

    /**
     * Parses a folder entry from HTML line.
     *
     * Expected format: `<DT><H3 ADD_DATE="timestamp">Folder Name</H3>`
     *
     * @param line HTML line containing folder
     * @param parentId ID of the parent folder (null for root)
     * @return Parsed FavoriteFolder or null if parsing fails
     */
    private fun parseFolderEntry(line: String, parentId: String?): FavoriteFolder? {
        try {
            // Extract folder name (between >< tags)
            val nameMatch = Regex(""">([^<]+)</H3>""", RegexOption.IGNORE_CASE)
                .find(line) ?: return null
            val name = unescapeHtml(nameMatch.groupValues[1])

            // Extract timestamp (optional)
            val timestamp = Regex("""ADD_DATE="(\d+)"""", RegexOption.IGNORE_CASE)
                .find(line)?.groupValues?.get(1)?.toLongOrNull()

            val createdAt = if (timestamp != null) {
                Instant.fromEpochSeconds(timestamp)
            } else {
                Clock.System.now()
            }

            return FavoriteFolder.create(
                name = name,
                parentId = parentId
            ).copy(createdAt = createdAt)
        } catch (e: Exception) {
            return null
        }
    }

    /**
     * Escapes special HTML characters.
     *
     * @param text Text to escape
     * @return HTML-escaped text
     */
    private fun escapeHtml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }

    /**
     * Unescapes HTML entities.
     *
     * @param text HTML text to unescape
     * @return Unescaped text
     */
    private fun unescapeHtml(text: String): String {
        return text
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&amp;", "&") // Must be last
    }

    /**
     * Generates a timestamped filename for bookmark export.
     *
     * Format: `bookmarks_YYYYMMDD_HHMMSS.html`
     *
     * @return Filename string
     *
     * @sample
     * ```kotlin
     * val filename = BookmarkImportExport.generateExportFilename()
     * // Example: "bookmarks_20231212_143022.html"
     * ```
     */
    fun generateExportFilename(): String {
        val now = Clock.System.now()
        val timestamp = now.toString()
            .replace(":", "")
            .replace("-", "")
            .replace("T", "_")
            .substring(0, 15) // YYYYMMDD_HHMMSS
        return "bookmarks_$timestamp.html"
    }
}

/**
 * Data class containing the results of an import operation with parsed bookmarks and folders.
 *
 * @property favorites List of successfully parsed favorites
 * @property folders List of successfully parsed folders
 * @property result Summary of the import operation (counts and errors)
 */
data class ImportData(
    val favorites: List<Favorite>,
    val folders: List<FavoriteFolder>,
    val result: BookmarkImportExport.ImportResult
)

/**
 * Extension function to parse HTML and return both data and statistics.
 *
 * @param html HTML content to parse
 * @param existingUrls Set of existing bookmark URLs
 * @param skipDuplicates Whether to skip duplicate URLs
 * @return ImportData containing favorites, folders, and import statistics
 */
fun BookmarkImportExport.parseHtmlWithData(
    html: String,
    existingUrls: Set<String> = emptySet(),
    skipDuplicates: Boolean = true
): ImportData {
    val favorites = mutableListOf<Favorite>()
    val folders = mutableListOf<FavoriteFolder>()
    val errors = mutableListOf<String>()
    var skipped = 0

    try {
        val lines = html.lines()
        var currentFolder: FavoriteFolder? = null
        val folderStack = mutableListOf<FavoriteFolder?>()

        for (line in lines) {
            val trimmed = line.trim()

            when {
                trimmed.startsWith("<DT><A HREF=", ignoreCase = true) -> {
                    try {
                        val bookmark = parseBookmarkEntryInternal(trimmed, currentFolder?.id)
                        if (bookmark != null) {
                            if (skipDuplicates && existingUrls.contains(bookmark.url)) {
                                skipped++
                            } else {
                                favorites.add(bookmark)
                            }
                        }
                    } catch (e: Exception) {
                        errors.add("Failed to parse bookmark: ${e.message}")
                    }
                }

                trimmed.startsWith("<DT><H3", ignoreCase = true) -> {
                    try {
                        val folder = parseFolderEntryInternal(trimmed, currentFolder?.id)
                        if (folder != null) {
                            folders.add(folder)
                            folderStack.add(currentFolder)
                            currentFolder = folder
                        }
                    } catch (e: Exception) {
                        errors.add("Failed to parse folder: ${e.message}")
                    }
                }

                trimmed.startsWith("</DL>", ignoreCase = true) -> {
                    if (folderStack.isNotEmpty()) {
                        currentFolder = folderStack.removeLastOrNull()
                    }
                }
            }
        }
    } catch (e: Exception) {
        errors.add("Import failed: ${e.message}")
    }

    return ImportData(
        favorites = favorites,
        folders = folders,
        result = BookmarkImportExport.ImportResult(
            imported = favorites.size,
            skipped = skipped,
            folders = folders.size,
            errors = errors
        )
    )
}

// Internal helper functions for parseHtmlWithData
private fun parseBookmarkEntryInternal(line: String, folderId: String?): Favorite? {
    try {
        val urlMatch = Regex("""HREF="([^"]+)"""", RegexOption.IGNORE_CASE)
            .find(line) ?: return null
        val url = urlMatch.groupValues[1]
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&amp;", "&")

        val titleMatch = Regex(""">([^<]+)</A>""", RegexOption.IGNORE_CASE)
            .find(line) ?: return null
        val title = titleMatch.groupValues[1]
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&amp;", "&")

        val timestamp = Regex("""ADD_DATE="(\d+)"""", RegexOption.IGNORE_CASE)
            .find(line)?.groupValues?.get(1)?.toLongOrNull()

        val createdAt = if (timestamp != null) {
            Instant.fromEpochSeconds(timestamp)
        } else {
            Clock.System.now()
        }

        return Favorite.create(
            url = url,
            title = title,
            folderId = folderId
        ).copy(createdAt = createdAt, lastModifiedAt = createdAt)
    } catch (e: Exception) {
        return null
    }
}

private fun parseFolderEntryInternal(line: String, parentId: String?): FavoriteFolder? {
    try {
        val nameMatch = Regex(""">([^<]+)</H3>""", RegexOption.IGNORE_CASE)
            .find(line) ?: return null
        val name = nameMatch.groupValues[1]
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&amp;", "&")

        val timestamp = Regex("""ADD_DATE="(\d+)"""", RegexOption.IGNORE_CASE)
            .find(line)?.groupValues?.get(1)?.toLongOrNull()

        val createdAt = if (timestamp != null) {
            Instant.fromEpochSeconds(timestamp)
        } else {
            Clock.System.now()
        }

        return FavoriteFolder.create(
            name = name,
            parentId = parentId
        ).copy(createdAt = createdAt)
    } catch (e: Exception) {
        return null
    }
}
