package com.augmentalis.fileavanue

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.fileavanue.model.FileCategory
import com.augmentalis.fileavanue.model.FileItem
import com.augmentalis.fileavanue.model.StorageInfo
import com.augmentalis.fileavanue.model.formatBytes

/**
 * FileAvanue dashboard — shown when FileAvanue launches with no path.
 *
 * Grid of category cards (Images, Videos, Audio, Documents, Downloads, Recent),
 * storage usage card, and recent files list. All interactive elements have
 * AVID voice identifiers per mandatory rule.
 *
 * Layout follows EmptySessionView pattern from CockpitScreenContent.kt:282.
 */
@Composable
fun FileManagerDashboard(
    controller: FileBrowserController,
    onCategorySelected: (FileCategory) -> Unit,
    onPathSelected: (String) -> Unit,
    onFileSelected: (FileItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AvanueTheme.colors
    var categoryCounts by remember { mutableStateOf<Map<FileCategory, Int>>(emptyMap()) }
    var storageInfo by remember { mutableStateOf<StorageInfo?>(null) }
    var recentFiles by remember { mutableStateOf<List<FileItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val provider = controller.availableProviders.firstOrNull() ?: return@LaunchedEffect
        storageInfo = provider.getStorageInfo()
        categoryCounts = controller.getCategoryCounts()
        recentFiles = provider.getRecentFiles(10)
        isLoading = false
    }

    if (isLoading) {
        Box(modifier.fillMaxSize().background(colors.background), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(Modifier.size(48.dp), color = colors.primary)
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().background(colors.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Storage usage card
        item {
            storageInfo?.let { info ->
                StorageCard(info)
            }
        }

        // Category grid
        item {
            Text(
                "Categories",
                color = colors.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        item {
            CategoryGrid(
                categoryCounts = categoryCounts,
                onCategorySelected = onCategorySelected
            )
        }

        // Browse device button
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surface)
                    .clickable {
                        val root = controller.availableProviders.firstOrNull()?.getRootPath() ?: return@clickable
                        onPathSelected(root)
                    }
                    .padding(16.dp)
                    .semantics { contentDescription = "Voice: click Browse Device" },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Folder,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Browse Device", color = colors.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Text("Navigate filesystem", color = colors.textSecondary, fontSize = 12.sp)
                }
            }
        }

        // Recent files
        if (recentFiles.isNotEmpty()) {
            item {
                Text(
                    "Recent Files",
                    color = colors.textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }
            items(recentFiles, key = { it.uri }) { file ->
                RecentFileRow(file = file, onClick = { onFileSelected(file) })
            }
        }
    }
}

@Composable
private fun StorageCard(info: StorageInfo) {
    val colors = AvanueTheme.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Storage,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text("Storage", color = colors.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(12.dp))
        LinearProgressIndicator(
            progress = { info.usagePercent },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
            color = if (info.usagePercent > 0.85f) colors.error else colors.primary,
            trackColor = colors.border,
        )
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                "${formatBytes(info.usedBytes)} used",
                color = colors.textSecondary,
                fontSize = 12.sp
            )
            Text(
                "${formatBytes(info.availableBytes)} free",
                color = colors.textSecondary,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun CategoryGrid(
    categoryCounts: Map<FileCategory, Int>,
    onCategorySelected: (FileCategory) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.height(200.dp),
        userScrollEnabled = false
    ) {
        items(FileCategory.entries.toList()) { category ->
            CategoryCard(
                category = category,
                count = categoryCounts[category] ?: 0,
                onClick = { onCategorySelected(category) }
            )
        }
    }
}

@Composable
private fun CategoryCard(
    category: FileCategory,
    count: Int,
    onClick: () -> Unit
) {
    val colors = AvanueTheme.colors
    val icon = categoryIcon(category)

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface)
            .clickable(onClick = onClick)
            .padding(12.dp)
            .semantics { contentDescription = "Voice: click ${category.displayName}" },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = colors.primary,
            modifier = Modifier.size(28.dp)
        )
        Spacer(Modifier.height(6.dp))
        Text(
            category.displayName,
            color = colors.textPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
        if (count > 0) {
            Text(
                "$count items",
                color = colors.textSecondary,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun RecentFileRow(file: FileItem, onClick: () -> Unit) {
    val colors = AvanueTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(colors.surface)
            .clickable(onClick = onClick)
            .padding(12.dp)
            .semantics { contentDescription = "Voice: click ${file.name}" },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            fileTypeIcon(file),
            contentDescription = null,
            tint = colors.primary.copy(alpha = 0.7f),
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                file.name,
                color = colors.textPrimary,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                file.formattedSize,
                color = colors.textSecondary,
                fontSize = 12.sp
            )
        }
    }
}

private fun categoryIcon(category: FileCategory): ImageVector = when (category) {
    FileCategory.IMAGES -> Icons.Default.Image
    FileCategory.VIDEOS -> Icons.Default.Videocam
    FileCategory.AUDIO -> Icons.Default.MusicNote
    FileCategory.DOCUMENTS -> Icons.Default.Description
    FileCategory.DOWNLOADS -> Icons.Default.Download
    FileCategory.RECENT -> Icons.Default.History
}

internal fun fileTypeIcon(file: FileItem): ImageVector = when {
    file.isDirectory -> Icons.Default.Folder
    file.isImage -> Icons.Default.Image
    file.isVideo -> Icons.Default.Videocam
    file.isAudio -> Icons.Default.AudioFile
    file.isPdf -> Icons.Default.Description
    file.isArchive -> Icons.Default.Folder
    else -> Icons.Default.Description
}
