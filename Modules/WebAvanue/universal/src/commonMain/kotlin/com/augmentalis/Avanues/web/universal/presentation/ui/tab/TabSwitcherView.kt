package com.augmentalis.Avanues.web.universal.presentation.ui.tab

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.augmentalis.Avanues.web.universal.presentation.ui.theme.glassCard
import com.augmentalis.Avanues.web.universal.presentation.ui.theme.OceanTheme
import com.augmentalis.Avanues.web.universal.presentation.viewmodel.TabUiState
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size

/**
 * View mode for tab switcher
 */
enum class TabViewMode {
    GRID,   // Thumbnail grid view
    LIST    // Compact list view
}

/**
 * Tab category for organization
 */
enum class TabCategory(val displayName: String) {
    ALL("All"),
    PINNED("Pinned"),
    RECENT("Recent"),
    GROUPED("Groups")
}

/**
 * TabSwitcherView - Adaptive tab switcher for portrait and landscape
 *
 * Features:
 * - Adaptive grid (more columns in landscape)
 * - List/Grid view toggle
 * - Category tabs (All, Pinned, Recent, Groups)
 * - Compact cards that fit more tabs
 * - Close button on each card
 * - New tab button
 *
 * @param tabs List of all open tabs
 * @param activeTabId Currently active tab ID
 * @param onTabClick Callback when tab is clicked
 * @param onTabClose Callback when tab close button is clicked
 * @param onTabPin Callback when tab is long-pressed to pin/unpin
 * @param onNewTab Callback when new tab button is clicked
 * @param onDismiss Callback to close the tab switcher
 * @param isLandscape Whether in landscape orientation
 * @param modifier Modifier for customization
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TabSwitcherView(
    tabs: List<TabUiState>,
    activeTabId: String?,
    onTabClick: (String) -> Unit,
    onTabClose: (String) -> Unit,
    onTabPin: (String) -> Unit = {},
    onNewTab: () -> Unit,
    onDismiss: () -> Unit,
    isLandscape: Boolean = false,
    modifier: Modifier = Modifier
) {
    var viewMode by rememberSaveable { mutableStateOf(TabViewMode.GRID) }
    var selectedCategory by rememberSaveable { mutableStateOf(TabCategory.ALL) }

    // Filter tabs based on category
    val filteredTabs = remember(tabs, selectedCategory) {
        when (selectedCategory) {
            TabCategory.ALL -> tabs
            TabCategory.PINNED -> tabs.filter { it.tab.isPinned }
            TabCategory.RECENT -> tabs.sortedByDescending { it.tab.lastAccessedAt }.take(10)
            TabCategory.GROUPED -> tabs.filter { it.tab.groupId != null }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(OceanTheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header with tab count, view toggle, and controls
            TabSwitcherHeader(
                tabCount = tabs.size,
                viewMode = viewMode,
                onViewModeChange = { viewMode = it },
                onNewTab = onNewTab,
                onDismiss = onDismiss,
                isLandscape = isLandscape
            )

            // Category tabs
            CategoryTabs(
                selectedCategory = selectedCategory,
                onCategorySelect = { selectedCategory = it },
                pinnedCount = tabs.count { it.tab.isPinned },
                groupedCount = tabs.count { it.tab.groupId != null }
            )

            // Tab content based on view mode
            if (filteredTabs.isEmpty()) {
                EmptyTabsState(
                    category = selectedCategory,
                    onNewTab = onNewTab,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                when (viewMode) {
                    TabViewMode.GRID -> TabGridView(
                        tabs = filteredTabs,
                        activeTabId = activeTabId,
                        onTabClick = onTabClick,
                        onTabClose = onTabClose,
                        onTabPin = onTabPin,
                        onDismiss = onDismiss,
                        isLandscape = isLandscape
                    )
                    TabViewMode.LIST -> TabListView(
                        tabs = filteredTabs,
                        activeTabId = activeTabId,
                        onTabClick = onTabClick,
                        onTabClose = onTabClose,
                        onTabPin = onTabPin,
                        onDismiss = onDismiss
                    )
                }
            }
        }
    }
}

/**
 * Header for tab switcher with count, view toggle, and controls
 */
@Composable
private fun TabSwitcherHeader(
    tabCount: Int,
    viewMode: TabViewMode,
    onViewModeChange: (TabViewMode) -> Unit,
    onNewTab: () -> Unit,
    onDismiss: () -> Unit,
    isLandscape: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .glassCard(cornerRadius = 0.dp)
            .background(OceanTheme.surface),
        color = OceanTheme.surface,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = if (isLandscape) 8.dp else 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side: Back button + Tab count
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back button - replaces X close button
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = OceanTheme.textPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Tab count
                Text(
                    text = "$tabCount ${if (tabCount == 1) "tab" else "tabs"}",
                    style = if (isLandscape) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                    color = OceanTheme.textPrimary
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // View mode toggle
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = OceanTheme.surfaceElevated,
                    modifier = Modifier.height(36.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        // Grid view button - custom 2x2 grid icon
                        IconButton(
                            onClick = { onViewModeChange(TabViewMode.GRID) },
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (viewMode == TabViewMode.GRID) OceanTheme.primary
                                    else Color.Transparent
                                )
                        ) {
                            CustomGridViewIcon(
                                tint = if (viewMode == TabViewMode.GRID) OceanTheme.textOnPrimary else OceanTheme.textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // List view button - custom 3-line list icon
                        IconButton(
                            onClick = { onViewModeChange(TabViewMode.LIST) },
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (viewMode == TabViewMode.LIST) OceanTheme.primary
                                    else Color.Transparent
                                )
                        ) {
                            CustomListViewIcon(
                                tint = if (viewMode == TabViewMode.LIST) OceanTheme.textOnPrimary else OceanTheme.textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // New tab button - AR/XR: 48dp touch target
                // Note: X close button removed - Back button on left serves this purpose
                IconButton(
                    onClick = onNewTab,
                    modifier = Modifier.size(48.dp)
                ) {
                    Surface(
                        color = OceanTheme.primary,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New Tab",
                            tint = Color.White,
                            modifier = Modifier.padding(8.dp).size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Category tabs for filtering
 */
@Composable
private fun CategoryTabs(
    selectedCategory: TabCategory,
    onCategorySelect: (TabCategory) -> Unit,
    pinnedCount: Int,
    groupedCount: Int,
    modifier: Modifier = Modifier
) {
    ScrollableTabRow(
        selectedTabIndex = TabCategory.entries.indexOf(selectedCategory),
        modifier = modifier
            .fillMaxWidth()
            .background(OceanTheme.surface),
        containerColor = OceanTheme.surface,
        contentColor = OceanTheme.textPrimary,
        edgePadding = 16.dp,
        divider = {}
    ) {
        TabCategory.entries.forEach { category ->
            val count = when (category) {
                TabCategory.PINNED -> pinnedCount
                TabCategory.GROUPED -> groupedCount
                else -> null
            }
            Tab(
                selected = selectedCategory == category,
                onClick = { onCategorySelect(category) },
                modifier = Modifier.height(40.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = category.displayName,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selectedCategory == category) OceanTheme.primary else OceanTheme.textSecondary
                    )
                    if (count != null && count > 0) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (selectedCategory == category) OceanTheme.primary else OceanTheme.surfaceElevated,
                            modifier = Modifier.size(20.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = count.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selectedCategory == category) OceanTheme.textOnPrimary else OceanTheme.textTertiary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Grid view for tabs - adaptive columns based on orientation
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TabGridView(
    tabs: List<TabUiState>,
    activeTabId: String?,
    onTabClick: (String) -> Unit,
    onTabClose: (String) -> Unit,
    onTabPin: (String) -> Unit,
    onDismiss: () -> Unit,
    isLandscape: Boolean
) {
    // Adaptive columns: 2 in portrait, 4 in landscape
    val columns = if (isLandscape) 4 else 2
    // Smaller card height in landscape
    val cardHeight = if (isLandscape) 100.dp else 140.dp

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(tabs, key = { it.tab.id }) { tabState ->
            TabGridCard(
                tabState = tabState,
                isActive = tabState.tab.id == activeTabId,
                cardHeight = cardHeight,
                isCompact = isLandscape,
                onClick = {
                    onTabClick(tabState.tab.id)
                    onDismiss()
                },
                onLongClick = { onTabPin(tabState.tab.id) },
                onClose = { onTabClose(tabState.tab.id) }
            )
        }
    }
}

/**
 * List view for tabs - compact single-line items
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TabListView(
    tabs: List<TabUiState>,
    activeTabId: String?,
    onTabClick: (String) -> Unit,
    onTabClose: (String) -> Unit,
    onTabPin: (String) -> Unit,
    onDismiss: () -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(tabs, key = { it.tab.id }) { tabState ->
            TabListItem(
                tabState = tabState,
                isActive = tabState.tab.id == activeTabId,
                onClick = {
                    onTabClick(tabState.tab.id)
                    onDismiss()
                },
                onLongClick = { onTabPin(tabState.tab.id) },
                onClose = { onTabClose(tabState.tab.id) }
            )
        }
    }
}

/**
 * Grid card for individual tab - adaptive size
 * Long-press to pin/unpin tab
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TabGridCard(
    tabState: TabUiState,
    isActive: Boolean,
    cardHeight: androidx.compose.ui.unit.Dp,
    isCompact: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(cardHeight)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) OceanTheme.surfaceElevated else OceanTheme.surface
        ),
        border = if (isActive) BorderStroke(2.dp, OceanTheme.primary) else BorderStroke(1.dp, OceanTheme.border)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (isCompact) 8.dp else 12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Tab content
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    // Pinned indicator
                    if (tabState.tab.isPinned) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Pinned",
                            tint = OceanTheme.primary,
                            modifier = Modifier.size(if (isCompact) 12.dp else 14.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    Text(
                        text = tabState.tab.title.ifBlank { "New Tab" },
                        style = if (isCompact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.titleSmall,
                        color = OceanTheme.textPrimary,
                        maxLines = if (isCompact) 1 else 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (!isCompact) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = tabState.tab.url.ifBlank { "about:blank" },
                            style = MaterialTheme.typography.labelSmall,
                            color = OceanTheme.textTertiary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Active indicator line
                if (isActive) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(OceanTheme.primary, RoundedCornerShape(1.dp))
                    )
                }
            }

            // Close button (top right)
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(2.dp)
                    .size(if (isCompact) 24.dp else 28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Tab",
                    tint = OceanTheme.iconActive,
                    modifier = Modifier.size(if (isCompact) 14.dp else 16.dp)
                )
            }
        }
    }
}

/**
 * List item for individual tab - compact single row
 * Long-press to pin/unpin tab
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TabListItem(
    tabState: TabUiState,
    isActive: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(8.dp),
        color = if (isActive) OceanTheme.surfaceElevated else OceanTheme.surface,
        border = if (isActive) BorderStroke(1.dp, OceanTheme.primary) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Active indicator dot
            if (isActive) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(OceanTheme.primary, RoundedCornerShape(4.dp))
                )
            }

            // Pinned indicator
            if (tabState.tab.isPinned) {
                Icon(
                    imageVector = Icons.Default.PushPin,
                    contentDescription = "Pinned",
                    tint = OceanTheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Title and URL
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = tabState.tab.title.ifBlank { "New Tab" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = OceanTheme.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = tabState.tab.url.ifBlank { "about:blank" },
                    style = MaterialTheme.typography.labelSmall,
                    color = OceanTheme.textTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Close button
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Tab",
                    tint = OceanTheme.iconActive,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Empty state when no tabs match filter
 */
@Composable
private fun EmptyTabsState(
    category: TabCategory,
    onNewTab: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = when (category) {
                    TabCategory.ALL -> Icons.Default.Tab
                    TabCategory.PINNED -> Icons.Default.PushPin
                    TabCategory.RECENT -> Icons.Default.History
                    TabCategory.GROUPED -> Icons.Default.Folder
                },
                contentDescription = null,
                tint = OceanTheme.textTertiary,
                modifier = Modifier.size(48.dp)
            )

            Text(
                text = when (category) {
                    TabCategory.ALL -> "No tabs open"
                    TabCategory.PINNED -> "No pinned tabs"
                    TabCategory.RECENT -> "No recent tabs"
                    TabCategory.GROUPED -> "No tab groups"
                },
                style = MaterialTheme.typography.titleMedium,
                color = OceanTheme.textPrimary,
                textAlign = TextAlign.Center
            )

            Text(
                text = when (category) {
                    TabCategory.ALL -> "Tap + to create a new tab"
                    TabCategory.PINNED -> "Long-press a tab to pin it"
                    TabCategory.RECENT -> "Your recent tabs will appear here"
                    TabCategory.GROUPED -> "Group tabs to organize your browsing"
                },
                style = MaterialTheme.typography.bodySmall,
                color = OceanTheme.textSecondary,
                textAlign = TextAlign.Center
            )

            if (category == TabCategory.ALL) {
                Button(
                    onClick = onNewTab,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OceanTheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("New Tab")
                }
            }
        }
    }
}

/**
 * Custom Grid View Icon - 2x2 grid of rounded squares
 * More visually distinct than Material's GridView icon
 */
@Composable
private fun CustomGridViewIcon(
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val boxSize = size.width * 0.4f
        val gap = size.width * 0.2f

        // Top-left square
        drawRoundRect(
            color = tint,
            topLeft = Offset(0f, 0f),
            size = Size(boxSize, boxSize),
            cornerRadius = CornerRadius(2.dp.toPx())
        )
        // Top-right square
        drawRoundRect(
            color = tint,
            topLeft = Offset(boxSize + gap, 0f),
            size = Size(boxSize, boxSize),
            cornerRadius = CornerRadius(2.dp.toPx())
        )
        // Bottom-left square
        drawRoundRect(
            color = tint,
            topLeft = Offset(0f, boxSize + gap),
            size = Size(boxSize, boxSize),
            cornerRadius = CornerRadius(2.dp.toPx())
        )
        // Bottom-right square
        drawRoundRect(
            color = tint,
            topLeft = Offset(boxSize + gap, boxSize + gap),
            size = Size(boxSize, boxSize),
            cornerRadius = CornerRadius(2.dp.toPx())
        )
    }
}

/**
 * Custom List View Icon - 3 horizontal lines with varying lengths
 * More visually distinct than Material's ViewList icon
 */
@Composable
private fun CustomListViewIcon(
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val lineHeight = size.height * 0.15f
        val gap = (size.height - 3 * lineHeight) / 4

        // Line 1 - full width
        drawRoundRect(
            color = tint,
            topLeft = Offset(0f, gap),
            size = Size(size.width, lineHeight),
            cornerRadius = CornerRadius(1.dp.toPx())
        )
        // Line 2 - 80% width
        drawRoundRect(
            color = tint,
            topLeft = Offset(0f, gap * 2 + lineHeight),
            size = Size(size.width * 0.8f, lineHeight),
            cornerRadius = CornerRadius(1.dp.toPx())
        )
        // Line 3 - 60% width
        drawRoundRect(
            color = tint,
            topLeft = Offset(0f, gap * 3 + lineHeight * 2),
            size = Size(size.width * 0.6f, lineHeight),
            cornerRadius = CornerRadius(1.dp.toPx())
        )
    }
}
