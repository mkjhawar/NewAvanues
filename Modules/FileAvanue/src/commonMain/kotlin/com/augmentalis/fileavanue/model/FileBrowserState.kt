package com.augmentalis.fileavanue.model

import kotlinx.serialization.Serializable

/**
 * Complete state of the file browser UI.
 *
 * Managed by [FileBrowserController] in commonMain — all state transitions
 * (sort, filter, navigate, select) happen in shared code. Platform UI
 * simply observes this state via StateFlow.
 */
@Serializable
data class FileBrowserState(
    val currentPath: String = "",
    val breadcrumbs: List<PathSegment> = emptyList(),
    val items: List<FileItem> = emptyList(),
    val selectedUris: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val sortMode: FileSortMode = FileSortMode.NAME_ASC,
    val viewMode: FileViewMode = FileViewMode.LIST,
    val showHidden: Boolean = false,
    val currentProviderId: String = "local",
    val storageInfo: StorageInfo? = null,
    val searchQuery: String = "",
) {
    val hasSelection: Boolean get() = selectedUris.isNotEmpty()
    val selectionCount: Int get() = selectedUris.size
    val allSelected: Boolean get() = selectedUris.size == items.size && items.isNotEmpty()
}

/**
 * A segment of the breadcrumb navigation path.
 * Each segment is clickable to navigate back to that directory.
 */
@Serializable
data class PathSegment(
    val name: String,
    val uri: String
)

/**
 * Device storage usage information.
 */
@Serializable
data class StorageInfo(
    val totalBytes: Long,
    val usedBytes: Long,
    val availableBytes: Long
) {
    val usagePercent: Float
        get() = if (totalBytes > 0) usedBytes.toFloat() / totalBytes else 0f
}

/**
 * Sort modes for file listing.
 * Directories are always sorted before files regardless of sort mode.
 */
@Serializable
enum class FileSortMode(val displayName: String) {
    NAME_ASC("Name A-Z"),
    NAME_DESC("Name Z-A"),
    DATE_ASC("Oldest first"),
    DATE_DESC("Newest first"),
    SIZE_ASC("Smallest first"),
    SIZE_DESC("Largest first"),
    TYPE_ASC("Type A-Z"),
}

/**
 * View mode for the file listing.
 */
@Serializable
enum class FileViewMode {
    LIST,
    GRID,
}
