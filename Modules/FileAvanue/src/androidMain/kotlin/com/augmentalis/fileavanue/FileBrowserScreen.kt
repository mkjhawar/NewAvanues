package com.augmentalis.fileavanue

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.fileavanue.model.FileItem
import com.augmentalis.fileavanue.model.FileSortMode
import com.augmentalis.fileavanue.model.FileViewMode
import kotlinx.coroutines.launch

/**
 * File browser screen with breadcrumb navigation, list/grid view, sort/filter.
 *
 * Observes [FileBrowserController.state] and delegates all mutations back
 * through the controller. Every interactive element has AVID voice identifiers.
 *
 * @param controller Shared controller managing state in commonMain
 * @param onFileOpened Called when user taps a non-directory file
 * @param onFileDetail Called on long-press — shows detail bottom sheet
 */
@Composable
fun FileBrowserScreen(
    controller: FileBrowserController,
    onFileOpened: (FileItem) -> Unit = {},
    onFileDetail: (FileItem) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by controller.state.collectAsState()
    val colors = AvanueTheme.colors
    val scope = rememberCoroutineScope()
    var showSearch by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    Column(modifier.fillMaxSize().background(colors.background)) {
        // Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back / Go Up
            if (state.breadcrumbs.size > 1) {
                IconButton(
                    onClick = { scope.launch { controller.navigateToParent() } },
                    modifier = Modifier.semantics { contentDescription = "Voice: click Go Up" }
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Go up", tint = colors.textPrimary)
                }
            }

            // Breadcrumbs
            BreadcrumbBar(
                breadcrumbs = state.breadcrumbs,
                onNavigate = { segment -> scope.launch { controller.navigateToBreadcrumb(segment) } },
                modifier = Modifier.weight(1f)
            )

            // Search toggle
            IconButton(
                onClick = { showSearch = !showSearch },
                modifier = Modifier.semantics { contentDescription = "Voice: click Search" }
            ) {
                Icon(
                    if (showSearch) Icons.Default.Close else Icons.Default.Search,
                    "Search", tint = colors.textPrimary
                )
            }

            // Sort dropdown
            Box {
                IconButton(
                    onClick = { showSortMenu = true },
                    modifier = Modifier.semantics { contentDescription = "Voice: click Sort" }
                ) {
                    Icon(Icons.AutoMirrored.Filled.Sort, "Sort", tint = colors.textPrimary)
                }
                DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                    FileSortMode.entries.forEach { mode ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    mode.displayName,
                                    fontWeight = if (mode == state.sortMode) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            onClick = {
                                controller.setSortMode(mode)
                                showSortMenu = false
                            },
                            modifier = Modifier.semantics {
                                contentDescription = "Voice: click ${mode.displayName}"
                            }
                        )
                    }
                }
            }

            // View mode toggle
            IconButton(
                onClick = {
                    controller.setViewMode(
                        if (state.viewMode == FileViewMode.LIST) FileViewMode.GRID else FileViewMode.LIST
                    )
                },
                modifier = Modifier.semantics { contentDescription = "Voice: click View Mode" }
            ) {
                Icon(
                    if (state.viewMode == FileViewMode.LIST) Icons.Default.GridView
                    else Icons.AutoMirrored.Filled.ViewList,
                    "View mode", tint = colors.textPrimary
                )
            }
        }

        // Search bar (conditional)
        if (showSearch) {
            var searchText by remember { mutableStateOf(state.searchQuery) }
            TextField(
                value = searchText,
                onValueChange = { searchText = it },
                placeholder = { Text("Search files...") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .semantics { contentDescription = "Voice: type search query" },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = colors.surface,
                    unfocusedContainerColor = colors.surface,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary,
                ),
                trailingIcon = {
                    IconButton(
                        onClick = { scope.launch { controller.searchFiles(searchText) } },
                        modifier = Modifier.semantics { contentDescription = "Voice: click Search" }
                    ) {
                        Icon(Icons.Default.Search, "Search", tint = colors.primary)
                    }
                }
            )
        }

        // Selection bar
        if (state.hasSelection) {
            SelectionBar(
                count = state.selectionCount,
                allSelected = state.allSelected,
                onSelectAll = { controller.toggleSelectAll() },
                onClearSelection = { controller.clearSelection() },
                onDelete = { scope.launch { controller.deleteSelected() } }
            )
        }

        // Content
        when {
            state.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(48.dp), color = colors.primary)
                }
            }
            state.error != null -> {
                Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text(state.error ?: "Unknown error", color = colors.error, fontSize = 14.sp)
                }
            }
            state.items.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("This folder is empty", color = colors.textSecondary, fontSize = 14.sp)
                }
            }
            state.viewMode == FileViewMode.GRID -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(state.items, key = { it.uri }) { item ->
                        FileItemCard(
                            item = item,
                            isSelected = item.uri in state.selectedUris,
                            onClick = {
                                if (state.hasSelection) {
                                    controller.selectItem(item.uri)
                                } else if (item.isDirectory) {
                                    scope.launch { controller.loadDirectory(item.uri) }
                                } else {
                                    onFileOpened(item)
                                }
                            },
                            onLongClick = {
                                if (!state.hasSelection) {
                                    controller.selectItem(item.uri)
                                }
                                onFileDetail(item)
                            }
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(state.items, key = { it.uri }) { item ->
                        FileItemRow(
                            item = item,
                            isSelected = item.uri in state.selectedUris,
                            onClick = {
                                if (state.hasSelection) {
                                    controller.selectItem(item.uri)
                                } else if (item.isDirectory) {
                                    scope.launch { controller.loadDirectory(item.uri) }
                                } else {
                                    onFileOpened(item)
                                }
                            },
                            onLongClick = {
                                if (!state.hasSelection) {
                                    controller.selectItem(item.uri)
                                }
                                onFileDetail(item)
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Horizontal scrollable breadcrumb bar.
 * Each segment is a clickable chip with AVID identifier.
 */
@Composable
private fun BreadcrumbBar(
    breadcrumbs: List<com.augmentalis.fileavanue.model.PathSegment>,
    onNavigate: (com.augmentalis.fileavanue.model.PathSegment) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AvanueTheme.colors

    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        breadcrumbs.forEachIndexed { index, segment ->
            if (index > 0) {
                Text("/", color = colors.textSecondary, fontSize = 12.sp)
            }
            Text(
                text = segment.name,
                color = if (index == breadcrumbs.lastIndex) colors.primary else colors.textSecondary,
                fontSize = 13.sp,
                fontWeight = if (index == breadcrumbs.lastIndex) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { onNavigate(segment) }
                    .padding(horizontal = 4.dp, vertical = 2.dp)
                    .semantics { contentDescription = "Voice: click ${segment.name}" }
            )
        }
    }
}

/**
 * Selection action bar with count, select all, clear, delete.
 */
@Composable
private fun SelectionBar(
    count: Int,
    allSelected: Boolean,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = AvanueTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.primaryContainer)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "$count selected",
            color = colors.onPrimaryContainer,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        IconButton(
            onClick = onSelectAll,
            modifier = Modifier.semantics {
                contentDescription = "Voice: click ${if (allSelected) "Deselect All" else "Select All"}"
            }
        ) {
            Icon(
                if (allSelected) Icons.Default.Deselect else Icons.Default.SelectAll,
                "Select all", tint = colors.onPrimaryContainer
            )
        }
        IconButton(
            onClick = onClearSelection,
            modifier = Modifier.semantics { contentDescription = "Voice: click Clear Selection" }
        ) {
            Icon(Icons.Default.Close, "Clear", tint = colors.onPrimaryContainer)
        }
        IconButton(
            onClick = onDelete,
            modifier = Modifier.semantics { contentDescription = "Voice: click Delete Selected" }
        ) {
            Icon(Icons.Default.Delete, "Delete", tint = colors.error)
        }
    }
}

/**
 * Grid-mode card for a file item.
 * Shows thumbnail for images/videos (via Coil), Material icon for others.
 */
@Composable
private fun FileItemCard(
    item: FileItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val colors = AvanueTheme.colors

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) colors.primaryContainer else colors.surface)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .semantics { contentDescription = "Voice: click ${item.name}" }
    ) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(1f),
            contentAlignment = Alignment.Center
        ) {
            if (item.isImage && item.thumbnailUri.isNotBlank()) {
                SubcomposeAsyncImage(
                    model = item.thumbnailUri.ifBlank { item.uri },
                    contentDescription = item.name,
                    contentScale = ContentScale.Crop,
                    loading = {
                        Box(Modifier.fillMaxSize().background(colors.surface), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(Modifier.size(24.dp), color = colors.primary, strokeWidth = 2.dp)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    fileTypeIcon(item),
                    contentDescription = null,
                    tint = colors.primary.copy(alpha = 0.6f),
                    modifier = Modifier.size(40.dp)
                )
            }

            // Selection indicator
            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    "Selected",
                    tint = colors.primary,
                    modifier = Modifier.size(20.dp).align(Alignment.TopEnd).padding(4.dp)
                )
            }
        }

        Column(Modifier.padding(horizontal = 6.dp, vertical = 4.dp)) {
            Text(
                item.name,
                color = colors.textPrimary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!item.isDirectory) {
                Text(
                    item.formattedSize,
                    color = colors.textSecondary,
                    fontSize = 10.sp
                )
            } else if (item.childCount >= 0) {
                Text(
                    "${item.childCount} items",
                    color = colors.textSecondary,
                    fontSize = 10.sp
                )
            }
        }
    }
}

/**
 * List-mode row for a file item.
 * Icon + name + size + date in a horizontal layout.
 */
@Composable
private fun FileItemRow(
    item: FileItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val colors = AvanueTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) colors.primaryContainer else colors.surface)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(12.dp)
            .semantics { contentDescription = "Voice: click ${item.name}" },
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail or icon
        Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
            if (item.isImage && item.thumbnailUri.isNotBlank()) {
                SubcomposeAsyncImage(
                    model = item.thumbnailUri.ifBlank { item.uri },
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(6.dp))
                )
            } else {
                Icon(
                    fileTypeIcon(item),
                    contentDescription = null,
                    tint = colors.primary.copy(alpha = 0.6f),
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(
                item.name,
                color = colors.textPrimary,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!item.isDirectory) {
                    Text(item.formattedSize, color = colors.textSecondary, fontSize = 12.sp)
                } else if (item.childCount >= 0) {
                    Text("${item.childCount} items", color = colors.textSecondary, fontSize = 12.sp)
                }
            }
        }

        // Selection indicator
        if (isSelected) {
            Icon(
                Icons.Default.CheckCircle,
                "Selected",
                tint = colors.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
