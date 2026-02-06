package com.augmentalis.webavanue

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.augmentalis.webavanue.glassBar
import com.augmentalis.ava.core.theme.OceanTheme

/**
 * CommandBarWrapper - Handles visibility toggle and orientation for the command bar
 *
 * @param isVisible Whether the command bar is visible
 * @param isLandscape Whether the device is in landscape orientation
 * @param onToggleVisibility Callback to toggle command bar visibility
 * @param content The actual command bar content
 */
@Composable
fun CommandBarWrapper(
    isVisible: Boolean,
    isLandscape: Boolean,
    onToggleVisibility: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier) {
        // Command bar content with animation
        AnimatedVisibility(
            visible = isVisible,
            enter = if (isLandscape) slideInHorizontally { it } + fadeIn() else slideInVertically { it } + fadeIn(),
            exit = if (isLandscape) slideOutHorizontally { it } + fadeOut() else slideOutVertically { it } + fadeOut()
        ) {
            content()
        }

        // Show toggle button when bar is hidden
        AnimatedVisibility(
            visible = !isVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = if (isLandscape) {
                Modifier.align(Alignment.CenterEnd).padding(end = 8.dp)
            } else {
                Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)
            }
        ) {
            Surface(
                onClick = onToggleVisibility,
                shape = CircleShape,
                color = OceanTheme.surface,
                border = BorderStroke(1.dp, OceanTheme.border),
                shadowElevation = 4.dp,
                modifier = Modifier.size(48.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = if (isLandscape) Icons.Default.KeyboardArrowLeft else Icons.Default.KeyboardArrowUp,
                        contentDescription = "Show Command Bar",
                        tint = OceanTheme.iconActive,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

/**
 * BottomCommandBar - Floating command bar with flat 2-level hierarchy
 *
 * Features:
 * - Flat hierarchy: MAIN → {SCROLL, ZOOM, PAGE, MENU}
 * - Max 6 buttons per level (no scrolling needed)
 * - Landscape: vertical bar on right side
 * - Portrait: horizontal bar at bottom center
 *
 * @param onBack Browser back navigation
 * @param onForward Browser forward navigation
 * @param onHome Go to home page
 * @param onRefresh Reload current page
 * @param onScrollUp/Down/Top/Bottom Scroll controls
 * @param onBookmarks/Downloads/History/Settings Menu actions
 * @param onDesktopModeToggle Toggle desktop/mobile mode
 * @param onZoomIn/Out/Level Zoom controls
 * @param onFreezePage Freeze/unfreeze scrolling
 * @param onFavorite Add to favorites
 * @param onNewTab/ShowTabs/ShowFavorites Tab management
 * @param onDismissBar Hide command bar
 * @param onToggleHeadlessMode Toggle fullscreen (headless) mode
 * @param isHeadlessMode When true, shows all controls (address bar hidden)
 * @param isLandscape Device orientation
 * @param modifier Modifier for customization
 */
@Composable
fun BottomCommandBar(
    onBack: () -> Unit = {},
    onForward: () -> Unit = {},
    onHome: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onScrollUp: () -> Unit = {},
    onScrollDown: () -> Unit = {},
    onScrollTop: () -> Unit = {},
    onScrollBottom: () -> Unit = {},
    onBookmarks: () -> Unit = {},
    onDownloads: () -> Unit = {},
    onHistory: () -> Unit = {},
    onSettings: () -> Unit = {},
    onDesktopModeToggle: () -> Unit = {},
    onZoomIn: () -> Unit = {},
    onZoomOut: () -> Unit = {},
    onZoomLevel: (Int) -> Unit = {},
    onFreezePage: () -> Unit = {},
    onFavorite: () -> Unit = {},
    onNewTab: () -> Unit = {},
    onShowTabs: () -> Unit = {},
    onShowFavorites: () -> Unit = {},
    onDismissBar: () -> Unit = {},
    tabCount: Int = 0,
    isListening: Boolean = false,
    isDesktopMode: Boolean = false,
    isScrollFrozen: Boolean = false,
    isLandscape: Boolean = false,
    isHeadlessMode: Boolean = false,
    onToggleHeadlessMode: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var currentLevel by remember { mutableStateOf(CommandBarLevel.MAIN) }
    var currentLabel by remember { mutableStateOf("") }

    // Landscape mode: vertical bar on the right side
    if (isLandscape) {
        Box(
            modifier = modifier
                .fillMaxHeight()
                .padding(end = 16.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            VerticalCommandBarInternal(
                currentLevel = currentLevel,
                onLevelChange = { currentLevel = it },
                currentLabel = currentLabel,
                onLabelChange = { currentLabel = it },
                onBack = onBack,
                onHome = onHome,
                onNewTab = onNewTab,
                onDismissBar = onDismissBar,
                tabCount = tabCount,
                isListening = isListening,
                onScrollUp = onScrollUp,
                onScrollDown = onScrollDown,
                onScrollTop = onScrollTop,
                onScrollBottom = onScrollBottom,
                onFreezePage = onFreezePage,
                isScrollFrozen = isScrollFrozen,
                onZoomIn = onZoomIn,
                onZoomOut = onZoomOut,
                onZoomLevel = onZoomLevel,
                onPreviousPage = onBack,
                onNextPage = onForward,
                onReload = onRefresh,
                onDesktopModeToggle = onDesktopModeToggle,
                onFavorite = onFavorite,
                isDesktopMode = isDesktopMode,
                isHeadlessMode = isHeadlessMode,
                onBookmarks = onBookmarks,
                onDownloads = onDownloads,
                onHistory = onHistory,
                onSettings = onSettings,
                onShowTabs = onShowTabs,
                onShowFavorites = onShowFavorites
            )
        }
        return
    }

    // Portrait mode: horizontal bar at the bottom - CENTERED, NO SCROLL
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 0.dp)
            .padding(bottom = 16.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Command title
            AnimatedVisibility(
                visible = currentLabel.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    modifier = Modifier
                        .glassBar(cornerRadius = 4.dp)
                        .background(OceanTheme.surface),
                    color = OceanTheme.surface,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = currentLabel.uppercase(),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = OceanTheme.textPrimary
                    )
                }
            }

            // Command bar container with glassmorphism - CENTERED, NO SCROLL
            Surface(
                modifier = Modifier
                    .glassBar(cornerRadius = 27.dp)
                    .background(OceanTheme.surface),
                color = OceanTheme.surface,
                shape = RoundedCornerShape(27.dp),
                shadowElevation = 0.dp,
                border = BorderStroke(1.dp, OceanTheme.border)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when (currentLevel) {
                        CommandBarLevel.MAIN -> MainCommandBarFlat(
                            onScrollClick = { currentLevel = CommandBarLevel.SCROLL },
                            onZoomClick = { currentLevel = CommandBarLevel.ZOOM },
                            onPageClick = { currentLevel = CommandBarLevel.PAGE },
                            onMenuClick = { currentLevel = CommandBarLevel.MENU },
                            onBack = onBack,
                            onHome = onHome,
                            onNewTab = onNewTab,
                            onShowTabs = onShowTabs,
                            onShowFavorites = onShowFavorites,
                            onDismissBar = onDismissBar,
                            onLabelChange = { currentLabel = it }
                        )

                        CommandBarLevel.SCROLL -> ScrollCommandBarFlat(
                            onScrollUp = onScrollUp,
                            onScrollDown = onScrollDown,
                            onScrollTop = onScrollTop,
                            onScrollBottom = onScrollBottom,
                            onFreezePage = onFreezePage,
                            isScrollFrozen = isScrollFrozen,
                            onBackToMain = { currentLevel = CommandBarLevel.MAIN },
                            onLabelChange = { currentLabel = it }
                        )

                        CommandBarLevel.ZOOM -> ZoomCommandBarFlat(
                            onZoomIn = onZoomIn,
                            onZoomOut = onZoomOut,
                            onZoomLevel = onZoomLevel,
                            onBackToMain = { currentLevel = CommandBarLevel.MAIN },
                            onLabelChange = { currentLabel = it }
                        )

                        CommandBarLevel.PAGE -> PageCommandBarFlat(
                            onPreviousPage = onBack,
                            onNextPage = onForward,
                            onReload = onRefresh,
                            onDesktopModeToggle = onDesktopModeToggle,
                            onFavorite = onFavorite,
                            onZoomIn = onZoomIn,
                            onZoomOut = onZoomOut,
                            isDesktopMode = isDesktopMode,
                            isHeadlessMode = isHeadlessMode,
                            onBackToMain = { currentLevel = CommandBarLevel.MAIN },
                            onLabelChange = { currentLabel = it }
                        )

                        CommandBarLevel.MENU -> MenuCommandBarFlat(
                            onBookmarks = onBookmarks,
                            onDownloads = onDownloads,
                            onHistory = onHistory,
                            onSettings = onSettings,
                            onShowTabs = onShowTabs,
                            onShowFavorites = onShowFavorites,
                            onNewTab = onNewTab,
                            onScrollClick = { currentLevel = CommandBarLevel.SCROLL },
                            onBackToMain = { currentLevel = CommandBarLevel.MAIN },
                            onLabelChange = { currentLabel = it },
                            isHeadlessMode = isHeadlessMode,
                            onToggleHeadlessMode = onToggleHeadlessMode
                        )
                    }
                }
            }
        }
    }
}

/**
 * VerticalCommandBarInternal - Landscape mode command bar (vertical layout on right side)
 */
@Composable
private fun VerticalCommandBarInternal(
    currentLevel: CommandBarLevel,
    onLevelChange: (CommandBarLevel) -> Unit,
    currentLabel: String,
    onLabelChange: (String) -> Unit,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onNewTab: () -> Unit,
    onDismissBar: () -> Unit,
    tabCount: Int,
    isListening: Boolean,
    onScrollUp: () -> Unit,
    onScrollDown: () -> Unit,
    onScrollTop: () -> Unit,
    onScrollBottom: () -> Unit,
    onFreezePage: () -> Unit,
    isScrollFrozen: Boolean,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onZoomLevel: (Int) -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onReload: () -> Unit,
    onDesktopModeToggle: () -> Unit,
    onFavorite: () -> Unit,
    isDesktopMode: Boolean,
    isHeadlessMode: Boolean,
    onBookmarks: () -> Unit,
    onDownloads: () -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit,
    onShowTabs: () -> Unit,
    onShowFavorites: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        // Command label (horizontal, left of buttons)
        AnimatedVisibility(
            visible = currentLabel.isNotEmpty(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Surface(
                modifier = Modifier.glassBar(cornerRadius = 8.dp),
                color = OceanTheme.surface,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = currentLabel.uppercase(),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = OceanTheme.textPrimary
                )
            }
        }

        // Vertical command bar container
        Surface(
            modifier = Modifier.glassBar(cornerRadius = 27.dp),
            color = OceanTheme.surface,
            shape = RoundedCornerShape(27.dp),
            shadowElevation = 0.dp,
            border = BorderStroke(1.dp, OceanTheme.border)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                VerticalCommandBarContent(
                    currentLevel = currentLevel,
                    onLevelChange = onLevelChange,
                    onLabelChange = onLabelChange,
                    onBack = onBack,
                    onHome = onHome,
                    onNewTab = onNewTab,
                    onScrollUp = onScrollUp,
                    onScrollDown = onScrollDown,
                    onScrollTop = onScrollTop,
                    onScrollBottom = onScrollBottom,
                    onFreezePage = onFreezePage,
                    isScrollFrozen = isScrollFrozen,
                    onZoomIn = onZoomIn,
                    onZoomOut = onZoomOut,
                    onZoomLevel = onZoomLevel,
                    onPreviousPage = onPreviousPage,
                    onNextPage = onNextPage,
                    onReload = onReload,
                    onDesktopModeToggle = onDesktopModeToggle,
                    onFavorite = onFavorite,
                    isDesktopMode = isDesktopMode,
                    isHeadlessMode = isHeadlessMode,
                    onBookmarks = onBookmarks,
                    onDownloads = onDownloads,
                    onHistory = onHistory,
                    onSettings = onSettings,
                    onShowTabs = onShowTabs,
                    onShowFavorites = onShowFavorites
                )
            }
        }
    }
}

@Composable
private fun VerticalCommandBarContent(
    currentLevel: CommandBarLevel,
    onLevelChange: (CommandBarLevel) -> Unit,
    onLabelChange: (String) -> Unit,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onNewTab: () -> Unit,
    onScrollUp: () -> Unit,
    onScrollDown: () -> Unit,
    onScrollTop: () -> Unit,
    onScrollBottom: () -> Unit,
    onFreezePage: () -> Unit,
    isScrollFrozen: Boolean,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onZoomLevel: (Int) -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onReload: () -> Unit,
    onDesktopModeToggle: () -> Unit,
    onFavorite: () -> Unit,
    isDesktopMode: Boolean,
    isHeadlessMode: Boolean,
    onBookmarks: () -> Unit,
    onDownloads: () -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit,
    onShowTabs: () -> Unit,
    onShowFavorites: () -> Unit
) {
    when (currentLevel) {
        CommandBarLevel.MAIN -> {
            CommandButton(
                icon = androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack,
                label = "Back",
                onClick = onBack,
                onFocus = { onLabelChange("Go Back") },
                onBlur = { onLabelChange("") },
                backgroundColor = OceanTheme.surfaceElevated
            )
            CommandButton(
                icon = androidx.compose.material.icons.Icons.Default.Home,
                label = "Home",
                onClick = onHome,
                onFocus = { onLabelChange("Go Home") },
                onBlur = { onLabelChange("") },
                backgroundColor = OceanTheme.surfaceElevated
            )
            CommandButton(
                icon = androidx.compose.material.icons.Icons.Default.Layers,
                label = "Tabs",
                onClick = onShowTabs,
                onFocus = { onLabelChange("Show Tabs (3D View)") },
                onBlur = { onLabelChange("") },
                backgroundColor = OceanTheme.primary.copy(alpha = 0.3f)
            )
            CommandButton(
                icon = androidx.compose.material.icons.Icons.Default.Star,
                label = "Favs",
                onClick = onShowFavorites,
                onFocus = { onLabelChange("Show Favorites (3D View)") },
                onBlur = { onLabelChange("") },
                backgroundColor = OceanTheme.starActive.copy(alpha = 0.3f)
            )
            CommandButton(
                icon = androidx.compose.material.icons.Icons.Default.Web,
                label = "Page",
                onClick = { onLevelChange(CommandBarLevel.PAGE) },
                onFocus = { onLabelChange("Page Controls") },
                onBlur = { onLabelChange("") },
                backgroundColor = OceanTheme.surfaceElevated
            )
            CommandButton(
                icon = androidx.compose.material.icons.Icons.Default.MoreVert,
                label = "Menu",
                onClick = { onLevelChange(CommandBarLevel.MENU) },
                onFocus = { onLabelChange("Menu") },
                onBlur = { onLabelChange("") },
                backgroundColor = OceanTheme.surfaceElevated
            )
        }
        CommandBarLevel.SCROLL -> {
            CommandButton(
                icon = androidx.compose.material.icons.Icons.Default.Close,
                label = "Close",
                onClick = { onLevelChange(CommandBarLevel.MAIN) },
                onFocus = { onLabelChange("Back to Main") },
                onBlur = { onLabelChange("") },
                backgroundColor = OceanTheme.surfaceElevated
            )
            CommandButton(
                icon = androidx.compose.material.icons.Icons.Default.KeyboardArrowUp,
                label = "Up",
                onClick = onScrollUp,
                onFocus = { onLabelChange("Scroll Up") },
                onBlur = { onLabelChange("") },
                backgroundColor = OceanTheme.surfaceElevated
            )
            CommandButton(
                icon = androidx.compose.material.icons.Icons.Default.KeyboardArrowDown,
                label = "Down",
                onClick = onScrollDown,
                onFocus = { onLabelChange("Scroll Down") },
                onBlur = { onLabelChange("") },
                backgroundColor = OceanTheme.surfaceElevated
            )
            CommandButton(
                icon = androidx.compose.material.icons.Icons.Default.VerticalAlignTop,
                label = "Top",
                onClick = onScrollTop,
                onFocus = { onLabelChange("Go to Top") },
                onBlur = { onLabelChange("") },
                backgroundColor = OceanTheme.surfaceElevated
            )
            CommandButton(
                icon = androidx.compose.material.icons.Icons.Default.VerticalAlignBottom,
                label = "Bottom",
                onClick = onScrollBottom,
                onFocus = { onLabelChange("Go to Bottom") },
                onBlur = { onLabelChange("") },
                backgroundColor = OceanTheme.surfaceElevated
            )
            CommandButton(
                icon = if (isScrollFrozen) androidx.compose.material.icons.Icons.Default.LockOpen else androidx.compose.material.icons.Icons.Default.Lock,
                label = if (isScrollFrozen) "Unfreeze" else "Freeze",
                onClick = onFreezePage,
                onFocus = { onLabelChange(if (isScrollFrozen) "Unfreeze" else "Freeze") },
                onBlur = { onLabelChange("") },
                backgroundColor = if (isScrollFrozen) OceanTheme.primary else OceanTheme.surfaceElevated,
                isActive = isScrollFrozen
            )
        }
        CommandBarLevel.ZOOM -> {
            CommandButton(
                icon = androidx.compose.material.icons.Icons.Default.Close,
                label = "Close",
                onClick = { onLevelChange(CommandBarLevel.MAIN) },
                onFocus = { onLabelChange("Back to Main") },
                onBlur = { onLabelChange("") },
                backgroundColor = OceanTheme.surfaceElevated
            )
            CommandButton(
                icon = androidx.compose.material.icons.Icons.Default.ZoomOut,
                label = "Out",
                onClick = onZoomOut,
                onFocus = { onLabelChange("Zoom Out") },
                onBlur = { onLabelChange("") },
                backgroundColor = OceanTheme.surfaceElevated
            )
            CommandButton(
                icon = androidx.compose.material.icons.Icons.Default.ZoomIn,
                label = "In",
                onClick = onZoomIn,
                onFocus = { onLabelChange("Zoom In") },
                onBlur = { onLabelChange("") },
                backgroundColor = OceanTheme.surfaceElevated
            )
            ZoomLevelButton(label = "50%", onClick = { onZoomLevel(1) }, onFocus = { onLabelChange("Zoom 50%") }, onBlur = { onLabelChange("") })
            ZoomLevelButton(label = "100%", onClick = { onZoomLevel(3) }, onFocus = { onLabelChange("Zoom 100%") }, onBlur = { onLabelChange("") })
            ZoomLevelButton(label = "150%", onClick = { onZoomLevel(5) }, onFocus = { onLabelChange("Zoom 150%") }, onBlur = { onLabelChange("") })
        }
        CommandBarLevel.PAGE -> {
            CommandButton(
                icon = androidx.compose.material.icons.Icons.Default.Close,
                label = "Close",
                onClick = { onLevelChange(CommandBarLevel.MAIN) },
                onFocus = { onLabelChange("Back to Main") },
                onBlur = { onLabelChange("") },
                backgroundColor = OceanTheme.surfaceElevated
            )
            CommandButton(
                icon = androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack,
                label = "Prev",
                onClick = onPreviousPage,
                onFocus = { onLabelChange("Previous Page") },
                onBlur = { onLabelChange("") },
                backgroundColor = OceanTheme.surfaceElevated
            )
            CommandButton(
                icon = androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowForward,
                label = "Next",
                onClick = onNextPage,
                onFocus = { onLabelChange("Next Page") },
                onBlur = { onLabelChange("") },
                backgroundColor = OceanTheme.surfaceElevated
            )
            CommandButton(
                icon = androidx.compose.material.icons.Icons.Default.Refresh,
                label = "Reload",
                onClick = onReload,
                onFocus = { onLabelChange("Reload Page") },
                onBlur = { onLabelChange("") },
                backgroundColor = OceanTheme.surfaceElevated
            )
            if (isHeadlessMode) {
                CommandButton(
                    icon = if (isDesktopMode) androidx.compose.material.icons.Icons.Default.PhoneAndroid else androidx.compose.material.icons.Icons.Default.Laptop,
                    label = if (isDesktopMode) "Mobile" else "Desktop",
                    onClick = onDesktopModeToggle,
                    onFocus = { onLabelChange(if (isDesktopMode) "Switch to Mobile" else "Switch to Desktop") },
                    onBlur = { onLabelChange("") },
                    backgroundColor = if (isDesktopMode) OceanTheme.primary else OceanTheme.surfaceElevated,
                    isActive = isDesktopMode
                )
                CommandButton(
                    icon = androidx.compose.material.icons.Icons.Default.Star,
                    label = "Favorite",
                    onClick = onFavorite,
                    onFocus = { onLabelChange("Add to Favorites") },
                    onBlur = { onLabelChange("") },
                    backgroundColor = OceanTheme.surfaceElevated
                )
            } else {
                CommandButton(
                    icon = androidx.compose.material.icons.Icons.Default.ZoomIn,
                    label = "Zoom+",
                    onClick = onZoomIn,
                    onFocus = { onLabelChange("Zoom In") },
                    onBlur = { onLabelChange("") },
                    backgroundColor = OceanTheme.surfaceElevated
                )
                CommandButton(
                    icon = androidx.compose.material.icons.Icons.Default.ZoomOut,
                    label = "Zoom-",
                    onClick = onZoomOut,
                    onFocus = { onLabelChange("Zoom Out") },
                    onBlur = { onLabelChange("") },
                    backgroundColor = OceanTheme.surfaceElevated
                )
            }
        }
        CommandBarLevel.MENU -> {
            CommandButton(
                icon = androidx.compose.material.icons.Icons.Default.Close,
                label = "Close",
                onClick = { onLevelChange(CommandBarLevel.MAIN) },
                onFocus = { onLabelChange("Back to Main") },
                onBlur = { onLabelChange("") },
                backgroundColor = OceanTheme.surfaceElevated
            )
            CommandButton(
                icon = androidx.compose.material.icons.Icons.Default.Add,
                label = "New Tab",
                onClick = onNewTab,
                onFocus = { onLabelChange("Create New Tab") },
                onBlur = { onLabelChange("") },
                backgroundColor = OceanTheme.surfaceElevated
            )
            CommandButton(
                icon = androidx.compose.material.icons.Icons.Default.SwapVert,
                label = "Scroll",
                onClick = { onLevelChange(CommandBarLevel.SCROLL) },
                onFocus = { onLabelChange("Scroll Controls") },
                onBlur = { onLabelChange("") },
                backgroundColor = OceanTheme.surfaceElevated
            )
            CommandButton(
                icon = androidx.compose.material.icons.Icons.Default.History,
                label = "History",
                onClick = onHistory,
                onFocus = { onLabelChange("History") },
                onBlur = { onLabelChange("") },
                backgroundColor = OceanTheme.surfaceElevated
            )
            CommandButton(
                icon = androidx.compose.material.icons.Icons.Default.Download,
                label = "Downloads",
                onClick = onDownloads,
                onFocus = { onLabelChange("Downloads") },
                onBlur = { onLabelChange("") },
                backgroundColor = OceanTheme.surfaceElevated
            )
            CommandButton(
                icon = androidx.compose.material.icons.Icons.Default.Settings,
                label = "Settings",
                onClick = onSettings,
                onFocus = { onLabelChange("Settings") },
                onBlur = { onLabelChange("") },
                backgroundColor = OceanTheme.surfaceElevated
            )
        }
    }
}
