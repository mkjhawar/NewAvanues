package com.augmentalis.webavanue.ui.screen.browser

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.augmentalis.webavanue.ui.screen.theme.glassBar
import com.augmentalis.webavanue.ui.screen.theme.OceanTheme
import com.augmentalis.webavanue.ui.design.OceanComponents
import com.augmentalis.webavanue.ui.design.OceanDesignTokens
import com.augmentalis.webavanue.ui.design.IconVariant

/**
 * Command bar menu levels - FLAT HIERARCHY (Max 2 Levels)
 *
 * Design Principles:
 * - Max 2 levels deep (MAIN → sub-level)
 * - Max 6 buttons per level (no scrolling needed)
 * - Context-based grouping
 * - Single Close button to return (no redundant Back/Home)
 *
 * Hierarchy:
 * MAIN → SCROLL | ZOOM | PAGE | MENU
 */
enum class CommandBarLevel {
    MAIN,    // Primary: Back, Home, Add, Scroll, Page, Menu
    SCROLL,  // Scroll: Close, Up, Down, Top, Bottom, Freeze
    ZOOM,    // Zoom: Close, In, Out, 50%, 100%, 150%
    PAGE,    // Page: Close, Prev, Next, Reload, Desktop, Fav
    MENU     // Menu: Close, Bookmarks, Downloads, History, Settings
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
            VerticalCommandBar(
                currentLevel = currentLevel,
                onLevelChange = { currentLevel = it },
                currentLabel = currentLabel,
                onLabelChange = { currentLabel = it },
                onBack = onBack,
                onHome = onHome,
                onNewTab = onNewTab,
                onScrollClick = { currentLevel = CommandBarLevel.SCROLL },
                onPageClick = { currentLevel = CommandBarLevel.PAGE },
                onMenuClick = { currentLevel = CommandBarLevel.MENU },
                onDismissBar = onDismissBar,
                tabCount = tabCount,
                isListening = isListening,
                // Sub-level callbacks
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
                shadowElevation = 0.dp,  // FIX: Remove shadow artifacts
                border = BorderStroke(1.dp, OceanTheme.border)  // FIX: Use border instead
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 10.dp, vertical = 8.dp),  // FIX: Reduced padding, no scroll
                    horizontalArrangement = Arrangement.SpaceEvenly,  // FIX: Center-align buttons, no scrolling
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
 * MainCommandBarFlat - MAIN level (6 buttons max, no scrolling)
 *
 * Buttons: Back, Home, Tabs (3D), Favorites (3D), Page, Menu
 * Design: Direct access to most common actions + spatial views immediately accessible
 */
@Composable
private fun MainCommandBarFlat(
    onScrollClick: () -> Unit,
    onZoomClick: () -> Unit,
    onPageClick: () -> Unit,
    onMenuClick: () -> Unit,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onNewTab: () -> Unit,
    onShowTabs: () -> Unit,
    onShowFavorites: () -> Unit,
    onDismissBar: () -> Unit = {},
    onLabelChange: (String) -> Unit
) {
    // Back (browser back)
    CommandButton(
        icon = Icons.AutoMirrored.Filled.ArrowBack,
        label = "Back",
        onClick = onBack,
        onFocus = { onLabelChange("Go Back") },
        onBlur = { onLabelChange("") },
        backgroundColor = OceanTheme.surfaceElevated
    )

    // Home
    CommandButton(
        icon = Icons.Default.Home,
        label = "Home",
        onClick = onHome,
        onFocus = { onLabelChange("Go Home") },
        onBlur = { onLabelChange("") },
        backgroundColor = OceanTheme.surfaceElevated
    )

    // Tabs - 3D Spatial Tab Switcher (directly accessible)
    CommandButton(
        icon = Icons.Default.Layers,
        label = "Tabs",
        onClick = onShowTabs,
        onFocus = { onLabelChange("Show Tabs (3D View)") },
        onBlur = { onLabelChange("") },
        backgroundColor = OceanTheme.primary.copy(alpha = 0.3f)  // Highlight spatial feature
    )

    // Favorites - 3D Spatial Favorites Shelf (directly accessible)
    CommandButton(
        icon = Icons.Default.Star,
        label = "Favs",
        onClick = onShowFavorites,
        onFocus = { onLabelChange("Show Favorites (3D View)") },
        onBlur = { onLabelChange("") },
        backgroundColor = OceanTheme.starActive.copy(alpha = 0.3f)  // Highlight spatial feature
    )

    // Page Commands (includes scroll and zoom)
    CommandButton(
        icon = Icons.Default.Web,
        label = "Page",
        onClick = onPageClick,
        onFocus = { onLabelChange("Page Controls") },
        onBlur = { onLabelChange("") },
        backgroundColor = OceanTheme.surfaceElevated
    )

    // Menu (settings, history, etc)
    CommandButton(
        icon = Icons.Default.MoreVert,
        label = "Menu",
        onClick = onMenuClick,
        onFocus = { onLabelChange("Menu") },
        onBlur = { onLabelChange("") },
        backgroundColor = OceanTheme.surfaceElevated
    )
}

/**
 * ScrollCommandBarFlat - SCROLL level (6 buttons, no scrolling needed)
 *
 * Buttons: Close, Up, Down, Top, Bottom, Freeze
 * Design: Essential scroll actions only
 */
@Composable
private fun ScrollCommandBarFlat(
    onScrollUp: () -> Unit,
    onScrollDown: () -> Unit,
    onScrollTop: () -> Unit,
    onScrollBottom: () -> Unit,
    onFreezePage: () -> Unit,
    isScrollFrozen: Boolean,
    onBackToMain: () -> Unit,
    onLabelChange: (String) -> Unit
) {
    // Close (return to main)
    CommandButton(
        icon = Icons.Default.Close,
        label = "Close",
        onClick = onBackToMain,
        onFocus = { onLabelChange("Back to Main") },
        onBlur = { onLabelChange("") },
        backgroundColor = OceanTheme.surfaceElevated
    )

    // Scroll Up
    CommandButton(
        icon = Icons.Default.KeyboardArrowUp,
        label = "Up",
        onClick = onScrollUp,
        onFocus = { onLabelChange("Scroll Up") },
        onBlur = { onLabelChange("") },
        backgroundColor = OceanTheme.surfaceElevated
    )

    // Scroll Down
    CommandButton(
        icon = Icons.Default.KeyboardArrowDown,
        label = "Down",
        onClick = onScrollDown,
        onFocus = { onLabelChange("Scroll Down") },
        onBlur = { onLabelChange("") },
        backgroundColor = OceanTheme.surfaceElevated
    )

    // Scroll Top
    CommandButton(
        icon = Icons.Default.VerticalAlignTop,
        label = "Top",
        onClick = onScrollTop,
        onFocus = { onLabelChange("Go to Top") },
        onBlur = { onLabelChange("") },
        backgroundColor = OceanTheme.surfaceElevated
    )

    // Scroll Bottom
    CommandButton(
        icon = Icons.Default.VerticalAlignBottom,
        label = "Bottom",
        onClick = onScrollBottom,
        onFocus = { onLabelChange("Go to Bottom") },
        onBlur = { onLabelChange("") },
        backgroundColor = OceanTheme.surfaceElevated
    )

    // Freeze Page
    CommandButton(
        icon = if (isScrollFrozen) Icons.Default.LockOpen else Icons.Default.Lock,
        label = if (isScrollFrozen) "Unfreeze" else "Freeze",
        onClick = onFreezePage,
        onFocus = { onLabelChange(if (isScrollFrozen) "Unfreeze" else "Freeze") },
        onBlur = { onLabelChange("") },
        backgroundColor = if (isScrollFrozen) OceanTheme.primary else OceanTheme.surfaceElevated,
        isActive = isScrollFrozen
    )
}

/**
 * ZoomCommandBarFlat - ZOOM level (6 buttons, inline zoom levels)
 *
 * Buttons: Close, Zoom Out, Zoom In, 50%, 100%, 150%
 * Design: Direct zoom control without sub-menus
 */
@Composable
private fun ZoomCommandBarFlat(
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onZoomLevel: (Int) -> Unit,
    onBackToMain: () -> Unit,
    onLabelChange: (String) -> Unit
) {
    // Close (return to main)
    CommandButton(
        icon = Icons.Default.Close,
        label = "Close",
        onClick = onBackToMain,
        onFocus = { onLabelChange("Back to Main") },
        onBlur = { onLabelChange("") },
        backgroundColor = OceanTheme.surfaceElevated
    )

    // Zoom Out
    CommandButton(
        icon = Icons.Default.ZoomOut,
        label = "Out",
        onClick = onZoomOut,
        onFocus = { onLabelChange("Zoom Out") },
        onBlur = { onLabelChange("") },
        backgroundColor = OceanTheme.surfaceElevated
    )

    // Zoom In
    CommandButton(
        icon = Icons.Default.ZoomIn,
        label = "In",
        onClick = onZoomIn,
        onFocus = { onLabelChange("Zoom In") },
        onBlur = { onLabelChange("") },
        backgroundColor = OceanTheme.surfaceElevated
    )

    // 50% zoom
    ZoomLevelButton(
        label = "50%",
        onClick = { onZoomLevel(1) },
        onFocus = { onLabelChange("Zoom 50%") },
        onBlur = { onLabelChange("") }
    )

    // 100% zoom
    ZoomLevelButton(
        label = "100%",
        onClick = { onZoomLevel(3) },
        onFocus = { onLabelChange("Zoom 100%") },
        onBlur = { onLabelChange("") }
    )

    // 150% zoom
    ZoomLevelButton(
        label = "150%",
        onClick = { onZoomLevel(5) },
        onFocus = { onLabelChange("Zoom 150%") },
        onBlur = { onLabelChange("") }
    )
}

/**
 * ZoomLevelButton - Compact button showing zoom percentage
 */
@Composable
private fun ZoomLevelButton(
    label: String,
    onClick: () -> Unit,
    onFocus: () -> Unit,
    onBlur: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .width(48.dp)
            .height(48.dp),
        shape = RoundedCornerShape(12.dp),
        color = OceanTheme.surfaceElevated,
        border = BorderStroke(1.dp, OceanTheme.border),
        shadowElevation = 2.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = OceanTheme.textPrimary
            )
        }
    }
}

/**
 * PageCommandBarFlat - PAGE level (variable buttons based on headless mode)
 *
 * Normal mode (6 buttons): Close, Prev, Next, Reload, Zoom+, Zoom-
 * Headless mode (6 buttons): Close, Prev, Next, Reload, Desktop, Favorite
 *
 * Desktop mode and Favorite are available in AddressBar when not headless,
 * so they are only shown here in headless mode.
 */
@Composable
private fun PageCommandBarFlat(
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onReload: () -> Unit,
    onDesktopModeToggle: () -> Unit,
    onFavorite: () -> Unit,
    onZoomIn: () -> Unit = {},
    onZoomOut: () -> Unit = {},
    isDesktopMode: Boolean,
    isHeadlessMode: Boolean = false,
    onBackToMain: () -> Unit,
    onLabelChange: (String) -> Unit
) {
    // Close (return to main)
    CommandButton(
        icon = Icons.Default.Close,
        label = "Close",
        onClick = onBackToMain,
        onFocus = { onLabelChange("Back to Main") },
        onBlur = { onLabelChange("") },
        backgroundColor = OceanTheme.surfaceElevated
    )

    // Previous Page
    CommandButton(
        icon = Icons.AutoMirrored.Filled.ArrowBack,
        label = "Prev",
        onClick = onPreviousPage,
        onFocus = { onLabelChange("Previous Page") },
        onBlur = { onLabelChange("") },
        backgroundColor = OceanTheme.surfaceElevated
    )

    // Next Page
    CommandButton(
        icon = Icons.AutoMirrored.Filled.ArrowForward,
        label = "Next",
        onClick = onNextPage,
        onFocus = { onLabelChange("Next Page") },
        onBlur = { onLabelChange("") },
        backgroundColor = OceanTheme.surfaceElevated
    )

    // Reload
    CommandButton(
        icon = Icons.Default.Refresh,
        label = "Reload",
        onClick = onReload,
        onFocus = { onLabelChange("Reload Page") },
        onBlur = { onLabelChange("") },
        backgroundColor = OceanTheme.surfaceElevated
    )

    if (isHeadlessMode) {
        // Headless mode: Show Desktop Mode and Favorite (not in address bar)
        // Desktop Mode
        CommandButton(
            icon = if (isDesktopMode) Icons.Default.PhoneAndroid else Icons.Default.Laptop,
            label = if (isDesktopMode) "Mobile" else "Desktop",
            onClick = onDesktopModeToggle,
            onFocus = { onLabelChange(if (isDesktopMode) "Switch to Mobile" else "Switch to Desktop") },
            onBlur = { onLabelChange("") },
            backgroundColor = if (isDesktopMode) OceanTheme.primary else OceanTheme.surfaceElevated,
            isActive = isDesktopMode
        )

        // Favorite
        CommandButton(
            icon = Icons.Default.Star,
            label = "Favorite",
            onClick = onFavorite,
            onFocus = { onLabelChange("Add to Favorites") },
            onBlur = { onLabelChange("") },
            backgroundColor = OceanTheme.surfaceElevated
        )
    } else {
        // Normal mode: Show Zoom controls (Desktop/Favorite are in AddressBar)
        // Zoom In
        CommandButton(
            icon = Icons.Default.ZoomIn,
            label = "Zoom+",
            onClick = onZoomIn,
            onFocus = { onLabelChange("Zoom In") },
            onBlur = { onLabelChange("") },
            backgroundColor = OceanTheme.surfaceElevated
        )

        // Zoom Out
        CommandButton(
            icon = Icons.Default.ZoomOut,
            label = "Zoom-",
            onClick = onZoomOut,
            onFocus = { onLabelChange("Zoom Out") },
            onBlur = { onLabelChange("") },
            backgroundColor = OceanTheme.surfaceElevated
        )
    }
}

/**
 * MenuCommandBarFlat - MENU level (6 buttons)
 *
 * Buttons: Close, New Tab, Scroll, Downloads, History, Settings
 * Design: App navigation and utilities (Tabs/Favorites moved to MAIN level)
 */
@Composable
private fun MenuCommandBarFlat(
    onBookmarks: () -> Unit,
    onDownloads: () -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit,
    onShowTabs: () -> Unit,
    onShowFavorites: () -> Unit,
    onNewTab: () -> Unit,
    onScrollClick: () -> Unit,
    onBackToMain: () -> Unit,
    onLabelChange: (String) -> Unit,
    isHeadlessMode: Boolean = false,
    onToggleHeadlessMode: () -> Unit = {}
) {
    // Close (return to main)
    CommandButton(
        icon = Icons.Default.Close,
        label = "Close",
        onClick = onBackToMain,
        onFocus = { onLabelChange("Back to Main") },
        onBlur = { onLabelChange("") },
        backgroundColor = OceanTheme.surfaceElevated
    )

    // New Tab
    CommandButton(
        icon = Icons.Default.Add,
        label = "New Tab",
        onClick = onNewTab,
        onFocus = { onLabelChange("Create New Tab") },
        onBlur = { onLabelChange("") },
        backgroundColor = OceanTheme.surfaceElevated
    )

    // Scroll Controls
    CommandButton(
        icon = Icons.Default.SwapVert,
        label = "Scroll",
        onClick = onScrollClick,
        onFocus = { onLabelChange("Scroll Controls") },
        onBlur = { onLabelChange("") },
        backgroundColor = OceanTheme.surfaceElevated
    )

    // History
    CommandButton(
        icon = Icons.Default.History,
        label = "History",
        onClick = onHistory,
        onFocus = { onLabelChange("History") },
        onBlur = { onLabelChange("") },
        backgroundColor = OceanTheme.surfaceElevated
    )

    // Downloads
    CommandButton(
        icon = Icons.Default.Download,
        label = "Downloads",
        onClick = onDownloads,
        onFocus = { onLabelChange("Downloads") },
        onBlur = { onLabelChange("") },
        backgroundColor = OceanTheme.surfaceElevated
    )

    // Settings
    CommandButton(
        icon = Icons.Default.Settings,
        label = "Settings",
        onClick = onSettings,
        onFocus = { onLabelChange("Settings") },
        onBlur = { onLabelChange("") },
        backgroundColor = OceanTheme.surfaceElevated
    )

    // Fullscreen toggle (Headless mode)
    CommandButton(
        icon = if (isHeadlessMode) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
        label = if (isHeadlessMode) "Exit Full" else "Fullscreen",
        onClick = onToggleHeadlessMode,
        onFocus = { onLabelChange(if (isHeadlessMode) "Exit Fullscreen Mode" else "Enter Fullscreen Mode") },
        onBlur = { onLabelChange("") },
        backgroundColor = if (isHeadlessMode) OceanTheme.primary else OceanTheme.surfaceElevated,
        isActive = isHeadlessMode
    )
}

// ============================================================================
// UTILITY COMPONENTS
// ============================================================================

/**
 * CommandButton - Standard command button with icon and label
 *
 * REWRITTEN: December 2025 - Uses Ocean component system
 *
 * Design:
 * - 46dp size optimized for 6 buttons in portrait mode
 * - Glassmorphic surface with border
 * - Icon + label layout
 * - Uses OceanDesignTokens for all colors and spacing
 * - MagicUI-ready architecture
 */
@Composable
private fun CommandButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    onFocus: () -> Unit,
    onBlur: () -> Unit,
    backgroundColor: Color = OceanDesignTokens.Surface.elevated,
    isActive: Boolean = false,
    modifier: Modifier = Modifier
) {
    // FIX: Reduced button size from 48dp to 46dp to fit 6 buttons in portrait without scrolling
    // 6 buttons × 46dp + 5 gaps × 4dp + 2 padding × 10dp = 276 + 20 + 20 = 316dp (fits 360dp screens)
    Surface(
        onClick = onClick,
        modifier = modifier
            .width(46.dp)
            .height(46.dp),
        shape = RoundedCornerShape(OceanDesignTokens.CornerRadius.md),
        color = if (isActive) OceanDesignTokens.Surface.primary else backgroundColor,
        border = BorderStroke(1.dp, OceanDesignTokens.Border.default),
        shadowElevation = OceanDesignTokens.Elevation.sm
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(OceanDesignTokens.Spacing.xs),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            OceanComponents.Icon(
                imageVector = icon,
                contentDescription = label,
                variant = if (isActive) IconVariant.OnPrimary else IconVariant.Primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (isActive) OceanDesignTokens.Text.onPrimary else OceanDesignTokens.Text.secondary,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun VoiceButton(
    isListening: Boolean,
    onClick: () -> Unit,
    onFocus: () -> Unit,
    onBlur: () -> Unit,
    activeColor: Color = OceanTheme.primary,
    backgroundColor: Color = OceanTheme.surfaceElevated,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "voice_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_alpha"
    )

    Box(
        modifier = modifier.size(36.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isListening) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(activeColor.copy(alpha = pulseAlpha))
            )
        }

        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (isListening) activeColor else backgroundColor)
        ) {
            Icon(
                imageVector = Icons.Default.Mic,  // FIX Issue #5: Changed from Phone to Mic (voice input)
                contentDescription = if (isListening) "Stop listening" else "Voice input",
                tint = OceanTheme.textPrimary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun DesktopModeButton(
    isDesktopMode: Boolean,
    onClick: () -> Unit,
    onFocus: () -> Unit,
    onBlur: () -> Unit,
    activeColor: Color = OceanTheme.primary,
    backgroundColor: Color = OceanTheme.surfaceElevated,
    modifier: Modifier = Modifier
) {
    // Use same style as CommandButton to show label
    Surface(
        onClick = onClick,
        modifier = modifier
            .width(48.dp)
            .height(48.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (isDesktopMode) activeColor else backgroundColor,
        border = BorderStroke(1.dp, OceanTheme.border),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (isDesktopMode) Icons.Default.PhoneAndroid else Icons.Default.Laptop,
                contentDescription = if (isDesktopMode) "Exit Desktop Mode" else "Enter Desktop Mode",
                tint = if (isDesktopMode) OceanTheme.textOnPrimary else OceanTheme.textPrimary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (isDesktopMode) "Mobile" else "Desktop",
                style = MaterialTheme.typography.labelSmall,
                color = if (isDesktopMode) OceanTheme.textOnPrimary else OceanTheme.textSecondary,
                maxLines = 1
            )
        }
    }
}

/**
 * TextCommandInput - Text command input field
 */
@Composable
fun TextCommandInput(
    visible: Boolean,
    onCommand: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var textValue by remember { mutableStateOf("") }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically { it },
        exit = fadeOut() + slideOutVertically { it }
    ) {
        Surface(
            color = Color(0xFF0F3460).copy(alpha = 0.95f),
            shape = RoundedCornerShape(8.dp),
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            OutlinedTextField(
                value = textValue,
                onValueChange = { textValue = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        "Type: back, forward, go to...",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF6C6C6C)
                    )
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF60A5FA),
                    unfocusedBorderColor = Color(0xFF2D4A6F),
                    cursorColor = Color(0xFF60A5FA)
                )
            )
        }
    }
}

/**
 * ListeningIndicator - Overlay shown when voice input is active
 */
@Composable
fun ListeningIndicator(
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                color = Color(0xFFA78BFA).copy(alpha = 0.95f),
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 16.dp
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 48.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(5) { index ->
                            WaveBar(delayMillis = index * 100)
                        }
                    }
                    Text(
                        text = "Listening...",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun WaveBar(
    delayMillis: Int,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val height by infiniteTransition.animateFloat(
        initialValue = 8f,
        targetValue = 24f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, delayMillis = delayMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave_height"
    )

    Box(
        modifier = modifier
            .width(4.dp)
            .height(height.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(Color.White)
    )
}

/**
 * VoiceCommandsPanel - Help panel showing available voice commands
 * AR/XR optimized: larger touch targets, better spacing, muted radius (12dp)
 */
@Composable
fun VoiceCommandsPanel(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val commands = listOf(
        "go back" to "Navigate back",
        "go forward" to "Navigate forward",
        "go home" to "Go to home page",
        "refresh" to "Reload page",
        "scroll up" to "Scroll page up",
        "scroll down" to "Scroll page down",
        "new tab" to "Open new tab",
        "close tab" to "Close current tab",
        "show tabs" to "Show 3D tabs view",
        "show favorites" to "Show favorites shelf",
        "go to [url]" to "Navigate to URL"
    )

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Surface(
            color = OceanTheme.surface.copy(alpha = 0.95f),
            shape = RoundedCornerShape(12.dp),
            shadowElevation = 12.dp,
            modifier = modifier
                .width(320.dp)  // Wider for better readability
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),  // Larger padding
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Voice Commands",
                        style = MaterialTheme.typography.titleMedium,  // Larger title
                        color = OceanTheme.textPrimary
                    )
                    // AR/XR: Larger close button (48dp minimum touch target)
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Surface(
                            color = OceanTheme.surfaceElevated,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = OceanTheme.textSecondary,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(24.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(color = OceanTheme.border.copy(alpha = 0.5f))

                commands.forEach { (command, description) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(OceanTheme.surfaceElevated, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),  // Larger padding for touch
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = OceanTheme.primary,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = command,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium,  // Larger text
                                color = Color.White
                            )
                        }
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyMedium,  // Larger text
                            color = OceanTheme.textSecondary
                        )
                    }
                }
            }
        }
    }
}

/**
 * VerticalCommandBarLayout - Public composable for landscape mode command bar
 * Used by BrowserScreen.kt for fixed side command bar layout
 *
 * Features:
 * - Vertical column of buttons
 * - Fixed position on left/right side (page resizes)
 * - Includes hide and switch-side controls
 */
@Composable
fun VerticalCommandBarLayout(
    onBack: () -> Unit,
    onForward: () -> Unit,
    onHome: () -> Unit,
    onRefresh: () -> Unit,
    onScrollUp: () -> Unit,
    onScrollDown: () -> Unit,
    onScrollTop: () -> Unit,
    onScrollBottom: () -> Unit,
    onFreezePage: () -> Unit,
    isScrollFrozen: Boolean,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onZoomLevel: (Int) -> Unit,
    onDesktopModeToggle: () -> Unit,
    onFavorite: () -> Unit,
    isDesktopMode: Boolean,
    isHeadlessMode: Boolean = false,
    onBookmarks: () -> Unit,
    onDownloads: () -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit,
    onNewTab: () -> Unit,
    onHide: () -> Unit,
    onSwitchSide: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentLevel by remember { mutableStateOf(CommandBarLevel.MAIN) }
    var currentLabel by remember { mutableStateOf("") }

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(horizontal = 8.dp)
    ) {
        // Command label
        AnimatedVisibility(
            visible = currentLabel.isNotEmpty(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Surface(
                modifier = Modifier
                    .glassBar(cornerRadius = 8.dp),
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

        // Vertical command bar container - radius matches URL bar (8.dp)
        Surface(
            modifier = Modifier
                .glassBar(cornerRadius = 8.dp),
            color = OceanTheme.surface,
            shape = RoundedCornerShape(8.dp),
            shadowElevation = 0.dp,  // FIX: Remove shadow artifacts in landscape mode
            border = BorderStroke(1.dp, OceanTheme.border)  // FIX: Use border instead
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Hide button at top
                CommandButton(
                    icon = Icons.Default.VisibilityOff,
                    label = "Hide",
                    onClick = onHide,
                    onFocus = { currentLabel = "Hide Bar" },
                    onBlur = { currentLabel = "" },
                    backgroundColor = OceanTheme.surfaceElevated
                )

                // Switch side button
                CommandButton(
                    icon = Icons.Default.SwapHoriz,
                    label = "Side",
                    onClick = onSwitchSide,
                    onFocus = { currentLabel = "Switch Side" },
                    onBlur = { currentLabel = "" },
                    backgroundColor = OceanTheme.surfaceElevated
                )

                HorizontalDivider(
                    modifier = Modifier.width(40.dp).padding(vertical = 4.dp),
                    color = OceanTheme.border
                )

                when (currentLevel) {
                    CommandBarLevel.MAIN -> {
                        CommandButton(
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            label = "Back",
                            onClick = onBack,
                            onFocus = { currentLabel = "Go Back" },
                            onBlur = { currentLabel = "" },
                            backgroundColor = OceanTheme.surfaceElevated
                        )
                        CommandButton(
                            icon = Icons.Default.Home,
                            label = "Home",
                            onClick = onHome,
                            onFocus = { currentLabel = "Go Home" },
                            onBlur = { currentLabel = "" },
                            backgroundColor = OceanTheme.surfaceElevated
                        )
                        CommandButton(
                            icon = Icons.Default.Add,
                            label = "New",
                            onClick = onNewTab,
                            onFocus = { currentLabel = "New Tab" },
                            onBlur = { currentLabel = "" },
                            backgroundColor = OceanTheme.surfaceElevated
                        )
                        CommandButton(
                            icon = Icons.Default.SwapVert,
                            label = "Scroll",
                            onClick = { currentLevel = CommandBarLevel.SCROLL },
                            onFocus = { currentLabel = "Scroll" },
                            onBlur = { currentLabel = "" },
                            backgroundColor = OceanTheme.surfaceElevated
                        )
                        CommandButton(
                            icon = Icons.Default.Web,
                            label = "Page",
                            onClick = { currentLevel = CommandBarLevel.PAGE },
                            onFocus = { currentLabel = "Page" },
                            onBlur = { currentLabel = "" },
                            backgroundColor = OceanTheme.surfaceElevated
                        )
                        CommandButton(
                            icon = Icons.Default.MoreVert,
                            label = "Menu",
                            onClick = { currentLevel = CommandBarLevel.MENU },
                            onFocus = { currentLabel = "Menu" },
                            onBlur = { currentLabel = "" },
                            backgroundColor = OceanTheme.surfaceElevated
                        )
                    }
                    CommandBarLevel.SCROLL -> {
                        CommandButton(
                            icon = Icons.Default.Close,
                            label = "Close",
                            onClick = { currentLevel = CommandBarLevel.MAIN },
                            onFocus = { currentLabel = "Back to Main" },
                            onBlur = { currentLabel = "" },
                            backgroundColor = OceanTheme.surfaceElevated
                        )
                        CommandButton(
                            icon = Icons.Default.KeyboardArrowUp,
                            label = "Up",
                            onClick = onScrollUp,
                            onFocus = { currentLabel = "Scroll Up" },
                            onBlur = { currentLabel = "" },
                            backgroundColor = OceanTheme.surfaceElevated
                        )
                        CommandButton(
                            icon = Icons.Default.KeyboardArrowDown,
                            label = "Down",
                            onClick = onScrollDown,
                            onFocus = { currentLabel = "Scroll Down" },
                            onBlur = { currentLabel = "" },
                            backgroundColor = OceanTheme.surfaceElevated
                        )
                        CommandButton(
                            icon = Icons.Default.VerticalAlignTop,
                            label = "Top",
                            onClick = onScrollTop,
                            onFocus = { currentLabel = "Go to Top" },
                            onBlur = { currentLabel = "" },
                            backgroundColor = OceanTheme.surfaceElevated
                        )
                        CommandButton(
                            icon = Icons.Default.VerticalAlignBottom,
                            label = "Bottom",
                            onClick = onScrollBottom,
                            onFocus = { currentLabel = "Go to Bottom" },
                            onBlur = { currentLabel = "" },
                            backgroundColor = OceanTheme.surfaceElevated
                        )
                        CommandButton(
                            icon = if (isScrollFrozen) Icons.Default.LockOpen else Icons.Default.Lock,
                            label = if (isScrollFrozen) "Unfreeze" else "Freeze",
                            onClick = onFreezePage,
                            onFocus = { currentLabel = if (isScrollFrozen) "Unfreeze" else "Freeze" },
                            onBlur = { currentLabel = "" },
                            backgroundColor = if (isScrollFrozen) OceanTheme.primary else OceanTheme.surfaceElevated,
                            isActive = isScrollFrozen
                        )
                    }
                    CommandBarLevel.ZOOM -> {
                        CommandButton(
                            icon = Icons.Default.Close,
                            label = "Close",
                            onClick = { currentLevel = CommandBarLevel.MAIN },
                            onFocus = { currentLabel = "Back to Main" },
                            onBlur = { currentLabel = "" },
                            backgroundColor = OceanTheme.surfaceElevated
                        )
                        CommandButton(
                            icon = Icons.Default.ZoomOut,
                            label = "Out",
                            onClick = onZoomOut,
                            onFocus = { currentLabel = "Zoom Out" },
                            onBlur = { currentLabel = "" },
                            backgroundColor = OceanTheme.surfaceElevated
                        )
                        CommandButton(
                            icon = Icons.Default.ZoomIn,
                            label = "In",
                            onClick = onZoomIn,
                            onFocus = { currentLabel = "Zoom In" },
                            onBlur = { currentLabel = "" },
                            backgroundColor = OceanTheme.surfaceElevated
                        )
                        ZoomLevelButton(label = "50%", onClick = { onZoomLevel(1) }, onFocus = { currentLabel = "Zoom 50%" }, onBlur = { currentLabel = "" })
                        ZoomLevelButton(label = "100%", onClick = { onZoomLevel(3) }, onFocus = { currentLabel = "Zoom 100%" }, onBlur = { currentLabel = "" })
                        ZoomLevelButton(label = "150%", onClick = { onZoomLevel(5) }, onFocus = { currentLabel = "Zoom 150%" }, onBlur = { currentLabel = "" })
                    }
                    CommandBarLevel.PAGE -> {
                        // PAGE level - content depends on headless mode
                        CommandButton(
                            icon = Icons.Default.Close,
                            label = "Close",
                            onClick = { currentLevel = CommandBarLevel.MAIN },
                            onFocus = { currentLabel = "Back to Main" },
                            onBlur = { currentLabel = "" },
                            backgroundColor = OceanTheme.surfaceElevated
                        )
                        CommandButton(
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            label = "Prev",
                            onClick = onBack,
                            onFocus = { currentLabel = "Previous Page" },
                            onBlur = { currentLabel = "" },
                            backgroundColor = OceanTheme.surfaceElevated
                        )
                        CommandButton(
                            icon = Icons.AutoMirrored.Filled.ArrowForward,
                            label = "Next",
                            onClick = onForward,
                            onFocus = { currentLabel = "Next Page" },
                            onBlur = { currentLabel = "" },
                            backgroundColor = OceanTheme.surfaceElevated
                        )
                        CommandButton(
                            icon = Icons.Default.Refresh,
                            label = "Reload",
                            onClick = onRefresh,
                            onFocus = { currentLabel = "Reload Page" },
                            onBlur = { currentLabel = "" },
                            backgroundColor = OceanTheme.surfaceElevated
                        )
                        if (isHeadlessMode) {
                            // Headless: Desktop and Favorite (not in AddressBar)
                            CommandButton(
                                icon = if (isDesktopMode) Icons.Default.PhoneAndroid else Icons.Default.Laptop,
                                label = if (isDesktopMode) "Mobile" else "Desktop",
                                onClick = onDesktopModeToggle,
                                onFocus = { currentLabel = if (isDesktopMode) "Switch to Mobile" else "Switch to Desktop" },
                                onBlur = { currentLabel = "" },
                                backgroundColor = if (isDesktopMode) OceanTheme.primary else OceanTheme.surfaceElevated,
                                isActive = isDesktopMode
                            )
                            CommandButton(
                                icon = Icons.Default.Star,
                                label = "Favorite",
                                onClick = onFavorite,
                                onFocus = { currentLabel = "Add to Favorites" },
                                onBlur = { currentLabel = "" },
                                backgroundColor = OceanTheme.surfaceElevated
                            )
                        } else {
                            // Normal: Zoom controls (Desktop/Favorite in AddressBar)
                            CommandButton(
                                icon = Icons.Default.ZoomIn,
                                label = "Zoom+",
                                onClick = onZoomIn,
                                onFocus = { currentLabel = "Zoom In" },
                                onBlur = { currentLabel = "" },
                                backgroundColor = OceanTheme.surfaceElevated
                            )
                            CommandButton(
                                icon = Icons.Default.ZoomOut,
                                label = "Zoom-",
                                onClick = onZoomOut,
                                onFocus = { currentLabel = "Zoom Out" },
                                onBlur = { currentLabel = "" },
                                backgroundColor = OceanTheme.surfaceElevated
                            )
                        }
                    }
                    CommandBarLevel.MENU -> {
                        CommandButton(
                            icon = Icons.Default.Close,
                            label = "Close",
                            onClick = { currentLevel = CommandBarLevel.MAIN },
                            onFocus = { currentLabel = "Back to Main" },
                            onBlur = { currentLabel = "" },
                            backgroundColor = OceanTheme.surfaceElevated
                        )
                        CommandButton(
                            icon = Icons.Default.Bookmarks,
                            label = "Bookmarks",
                            onClick = onBookmarks,
                            onFocus = { currentLabel = "Bookmarks" },
                            onBlur = { currentLabel = "" },
                            backgroundColor = OceanTheme.surfaceElevated
                        )
                        CommandButton(
                            icon = Icons.Default.FileDownload,
                            label = "Downloads",
                            onClick = onDownloads,
                            onFocus = { currentLabel = "Downloads" },
                            onBlur = { currentLabel = "" },
                            backgroundColor = OceanTheme.surfaceElevated
                        )
                        CommandButton(
                            icon = Icons.Default.History,
                            label = "History",
                            onClick = onHistory,
                            onFocus = { currentLabel = "History" },
                            onBlur = { currentLabel = "" },
                            backgroundColor = OceanTheme.surfaceElevated
                        )
                        CommandButton(
                            icon = Icons.Default.Settings,
                            label = "Settings",
                            onClick = onSettings,
                            onFocus = { currentLabel = "Settings" },
                            onBlur = { currentLabel = "" },
                            backgroundColor = OceanTheme.surfaceElevated
                        )
                    }
                }
            }
        }
    }
}

/**
 * HorizontalCommandBarLayout - Public composable for portrait mode command bar
 * Used by BrowserScreen.kt for bottom command bar layout (not overlay)
 *
 * Features:
 * - Horizontal row of buttons
 * - Fixed position at bottom (page resizes)
 * - Includes hide control
 */
@Composable
fun HorizontalCommandBarLayout(
    onBack: () -> Unit,
    onForward: () -> Unit,
    onHome: () -> Unit,
    onRefresh: () -> Unit,
    onScrollUp: () -> Unit,
    onScrollDown: () -> Unit,
    onScrollTop: () -> Unit,
    onScrollBottom: () -> Unit,
    onFreezePage: () -> Unit,
    isScrollFrozen: Boolean,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onZoomLevel: (Int) -> Unit,
    onDesktopModeToggle: () -> Unit,
    onFavorite: () -> Unit,
    isDesktopMode: Boolean,
    onBookmarks: () -> Unit,
    onDownloads: () -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit,
    onNewTab: () -> Unit,
    onShowTabs: () -> Unit,
    onShowFavorites: () -> Unit,
    onHide: () -> Unit,
    isHeadlessMode: Boolean = false,
    onToggleHeadlessMode: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var currentLevel by remember { mutableStateOf(CommandBarLevel.MAIN) }
    var currentLabel by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    LaunchedEffect(currentLevel) {
        scrollState.scrollTo(0)
    }

    // Spatial z-level container - command bar floats at HUD layer (0.5m equivalent)
    // AR/XR standard: muted radius (12.dp) for aesthetic continuity
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .graphicsLayer {
                // Spatial depth effect - slight elevation for floating feel
                shadowElevation = 16f
            }
    ) {
        // Command label - floats above command bar
        AnimatedVisibility(
            visible = currentLabel.isNotEmpty(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Surface(
                modifier = Modifier
                    .glassBar(cornerRadius = 12.dp),
                color = OceanTheme.surface.copy(alpha = 0.92f),
                shape = RoundedCornerShape(12.dp),
                shadowElevation = 8.dp,
                tonalElevation = 4.dp
            ) {
                Text(
                    text = currentLabel.uppercase(),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = OceanTheme.textPrimary
                )
            }
        }

        // Command bar container - Rounded square with muted radius (12.dp) for XR aesthetic
        Surface(
            modifier = Modifier
                .glassBar(cornerRadius = 12.dp)
                .graphicsLayer {
                    // 3D depth shadow
                    shadowElevation = 12f
                },
            color = OceanTheme.surface.copy(alpha = 0.92f),
            shape = RoundedCornerShape(12.dp),
            shadowElevation = 12.dp,
            tonalElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Hide button
                CommandButton(
                    icon = Icons.Default.KeyboardArrowDown,
                    label = "Hide",
                    onClick = onHide,
                    onFocus = { currentLabel = "Hide Bar" },
                    onBlur = { currentLabel = "" },
                    backgroundColor = OceanTheme.surfaceElevated
                )

                VerticalDivider(
                    modifier = Modifier.height(40.dp).padding(horizontal = 4.dp),
                    color = OceanTheme.border
                )

                when (currentLevel) {
                    CommandBarLevel.MAIN -> {
                        CommandButton(
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            label = "Back",
                            onClick = onBack,
                            onFocus = { currentLabel = "Go Back" },
                            onBlur = { currentLabel = "" },
                            backgroundColor = OceanTheme.surfaceElevated
                        )
                        CommandButton(
                            icon = Icons.Default.Home,
                            label = "Home",
                            onClick = onHome,
                            onFocus = { currentLabel = "Go Home" },
                            onBlur = { currentLabel = "" },
                            backgroundColor = OceanTheme.surfaceElevated
                        )
                        CommandButton(
                            icon = Icons.Default.Add,
                            label = "New",
                            onClick = onNewTab,
                            onFocus = { currentLabel = "New Tab" },
                            onBlur = { currentLabel = "" },
                            backgroundColor = OceanTheme.surfaceElevated
                        )
                        CommandButton(
                            icon = Icons.Default.SwapVert,
                            label = "Scroll",
                            onClick = { currentLevel = CommandBarLevel.SCROLL },
                            onFocus = { currentLabel = "Scroll" },
                            onBlur = { currentLabel = "" },
                            backgroundColor = OceanTheme.surfaceElevated
                        )
                        CommandButton(
                            icon = Icons.Default.Web,
                            label = "Page",
                            onClick = { currentLevel = CommandBarLevel.PAGE },
                            onFocus = { currentLabel = "Page" },
                            onBlur = { currentLabel = "" },
                            backgroundColor = OceanTheme.surfaceElevated
                        )
                        CommandButton(
                            icon = Icons.Default.MoreVert,
                            label = "Menu",
                            onClick = { currentLevel = CommandBarLevel.MENU },
                            onFocus = { currentLabel = "Menu" },
                            onBlur = { currentLabel = "" },
                            backgroundColor = OceanTheme.surfaceElevated
                        )
                    }
                    CommandBarLevel.SCROLL -> {
                        CommandButton(
                            icon = Icons.Default.Close,
                            label = "Close",
                            onClick = { currentLevel = CommandBarLevel.MAIN },
                            onFocus = { currentLabel = "Back to Main" },
                            onBlur = { currentLabel = "" },
                            backgroundColor = OceanTheme.surfaceElevated
                        )
                        CommandButton(
                            icon = Icons.Default.KeyboardArrowUp,
                            label = "Up",
                            onClick = onScrollUp,
                            onFocus = { currentLabel = "Scroll Up" },
                            onBlur = { currentLabel = "" },
                            backgroundColor = OceanTheme.surfaceElevated
                        )
                        CommandButton(
                            icon = Icons.Default.KeyboardArrowDown,
                            label = "Down",
                            onClick = onScrollDown,
                            onFocus = { currentLabel = "Scroll Down" },
                            onBlur = { currentLabel = "" },
                            backgroundColor = OceanTheme.surfaceElevated
                        )
                        CommandButton(
                            icon = Icons.Default.VerticalAlignTop,
                            label = "Top",
                            onClick = onScrollTop,
                            onFocus = { currentLabel = "Go to Top" },
                            onBlur = { currentLabel = "" },
                            backgroundColor = OceanTheme.surfaceElevated
                        )
                        CommandButton(
                            icon = Icons.Default.VerticalAlignBottom,
                            label = "Bottom",
                            onClick = onScrollBottom,
                            onFocus = { currentLabel = "Go to Bottom" },
                            onBlur = { currentLabel = "" },
                            backgroundColor = OceanTheme.surfaceElevated
                        )
                        CommandButton(
                            icon = if (isScrollFrozen) Icons.Default.LockOpen else Icons.Default.Lock,
                            label = if (isScrollFrozen) "Unfreeze" else "Freeze",
                            onClick = onFreezePage,
                            onFocus = { currentLabel = if (isScrollFrozen) "Unfreeze" else "Freeze" },
                            onBlur = { currentLabel = "" },
                            backgroundColor = if (isScrollFrozen) OceanTheme.primary else OceanTheme.surfaceElevated,
                            isActive = isScrollFrozen
                        )
                    }
                    CommandBarLevel.ZOOM -> {
                        CommandButton(
                            icon = Icons.Default.Close,
                            label = "Close",
                            onClick = { currentLevel = CommandBarLevel.MAIN },
                            onFocus = { currentLabel = "Back to Main" },
                            onBlur = { currentLabel = "" },
                            backgroundColor = OceanTheme.surfaceElevated
                        )
                        CommandButton(
                            icon = Icons.Default.ZoomOut,
                            label = "Out",
                            onClick = onZoomOut,
                            onFocus = { currentLabel = "Zoom Out" },
                            onBlur = { currentLabel = "" },
                            backgroundColor = OceanTheme.surfaceElevated
                        )
                        CommandButton(
                            icon = Icons.Default.ZoomIn,
                            label = "In",
                            onClick = onZoomIn,
                            onFocus = { currentLabel = "Zoom In" },
                            onBlur = { currentLabel = "" },
                            backgroundColor = OceanTheme.surfaceElevated
                        )
                        ZoomLevelButton(label = "50%", onClick = { onZoomLevel(1) }, onFocus = { currentLabel = "Zoom 50%" }, onBlur = { currentLabel = "" })
                        ZoomLevelButton(label = "100%", onClick = { onZoomLevel(3) }, onFocus = { currentLabel = "Zoom 100%" }, onBlur = { currentLabel = "" })
                        ZoomLevelButton(label = "150%", onClick = { onZoomLevel(5) }, onFocus = { currentLabel = "Zoom 150%" }, onBlur = { currentLabel = "" })
                    }
                    CommandBarLevel.PAGE -> {
                        // PAGE level - content depends on headless mode
                        CommandButton(
                            icon = Icons.Default.Close,
                            label = "Close",
                            onClick = { currentLevel = CommandBarLevel.MAIN },
                            onFocus = { currentLabel = "Back to Main" },
                            onBlur = { currentLabel = "" },
                            backgroundColor = OceanTheme.surfaceElevated
                        )
                        CommandButton(
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            label = "Prev",
                            onClick = onBack,
                            onFocus = { currentLabel = "Previous Page" },
                            onBlur = { currentLabel = "" },
                            backgroundColor = OceanTheme.surfaceElevated
                        )
                        CommandButton(
                            icon = Icons.AutoMirrored.Filled.ArrowForward,
                            label = "Next",
                            onClick = onForward,
                            onFocus = { currentLabel = "Next Page" },
                            onBlur = { currentLabel = "" },
                            backgroundColor = OceanTheme.surfaceElevated
                        )
                        CommandButton(
                            icon = Icons.Default.Refresh,
                            label = "Reload",
                            onClick = onRefresh,
                            onFocus = { currentLabel = "Reload Page" },
                            onBlur = { currentLabel = "" },
                            backgroundColor = OceanTheme.surfaceElevated
                        )
                        if (isHeadlessMode) {
                            // Headless: Desktop and Favorite (not in AddressBar)
                            CommandButton(
                                icon = if (isDesktopMode) Icons.Default.PhoneAndroid else Icons.Default.Laptop,
                                label = if (isDesktopMode) "Mobile" else "Desktop",
                                onClick = onDesktopModeToggle,
                                onFocus = { currentLabel = if (isDesktopMode) "Switch to Mobile" else "Switch to Desktop" },
                                onBlur = { currentLabel = "" },
                                backgroundColor = if (isDesktopMode) OceanTheme.primary else OceanTheme.surfaceElevated,
                                isActive = isDesktopMode
                            )
                            CommandButton(
                                icon = Icons.Default.Star,
                                label = "Favorite",
                                onClick = onFavorite,
                                onFocus = { currentLabel = "Add to Favorites" },
                                onBlur = { currentLabel = "" },
                                backgroundColor = OceanTheme.surfaceElevated
                            )
                        } else {
                            // Normal: Zoom controls (Desktop/Favorite in AddressBar)
                            CommandButton(
                                icon = Icons.Default.ZoomIn,
                                label = "Zoom+",
                                onClick = onZoomIn,
                                onFocus = { currentLabel = "Zoom In" },
                                onBlur = { currentLabel = "" },
                                backgroundColor = OceanTheme.surfaceElevated
                            )
                            CommandButton(
                                icon = Icons.Default.ZoomOut,
                                label = "Zoom-",
                                onClick = onZoomOut,
                                onFocus = { currentLabel = "Zoom Out" },
                                onBlur = { currentLabel = "" },
                                backgroundColor = OceanTheme.surfaceElevated
                            )
                        }
                    }
                    CommandBarLevel.MENU -> {
                        CommandButton(
                            icon = Icons.Default.Close,
                            label = "Close",
                            onClick = { currentLevel = CommandBarLevel.MAIN },
                            onFocus = { currentLabel = "Back to Main" },
                            onBlur = { currentLabel = "" },
                            backgroundColor = OceanTheme.surfaceElevated
                        )
                        CommandButton(
                            icon = Icons.Default.Bookmarks,
                            label = "Bookmarks",
                            onClick = onBookmarks,
                            onFocus = { currentLabel = "Bookmarks" },
                            onBlur = { currentLabel = "" },
                            backgroundColor = OceanTheme.surfaceElevated
                        )
                        CommandButton(
                            icon = Icons.Default.FileDownload,
                            label = "Downloads",
                            onClick = onDownloads,
                            onFocus = { currentLabel = "Downloads" },
                            onBlur = { currentLabel = "" },
                            backgroundColor = OceanTheme.surfaceElevated
                        )
                        CommandButton(
                            icon = Icons.Default.History,
                            label = "History",
                            onClick = onHistory,
                            onFocus = { currentLabel = "History" },
                            onBlur = { currentLabel = "" },
                            backgroundColor = OceanTheme.surfaceElevated
                        )
                        CommandButton(
                            icon = Icons.Default.Settings,
                            label = "Settings",
                            onClick = onSettings,
                            onFocus = { currentLabel = "Settings" },
                            onBlur = { currentLabel = "" },
                            backgroundColor = OceanTheme.surfaceElevated
                        )
                    }
                }
            }
        }
    }
}

/**
 * VerticalCommandBar - Landscape mode command bar (vertical layout on right side)
 * @deprecated Use VerticalCommandBarLayout instead for fixed layout
 *
 * Features:
 * - Vertical column of buttons (max 6)
 * - Same flat hierarchy as horizontal bar
 * - Switches content based on currentLevel
 * - Positioned on the right side of the screen
 */
@Composable
private fun VerticalCommandBar(
    currentLevel: CommandBarLevel,
    onLevelChange: (CommandBarLevel) -> Unit,
    currentLabel: String,
    onLabelChange: (String) -> Unit,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onNewTab: () -> Unit,
    onScrollClick: () -> Unit,
    onPageClick: () -> Unit,
    onMenuClick: () -> Unit,
    onDismissBar: () -> Unit,
    tabCount: Int,
    isListening: Boolean,
    // Additional callbacks for sub-levels
    onScrollUp: () -> Unit = {},
    onScrollDown: () -> Unit = {},
    onScrollTop: () -> Unit = {},
    onScrollBottom: () -> Unit = {},
    onFreezePage: () -> Unit = {},
    isScrollFrozen: Boolean = false,
    onZoomIn: () -> Unit = {},
    onZoomOut: () -> Unit = {},
    onZoomLevel: (Int) -> Unit = {},
    onPreviousPage: () -> Unit = {},
    onNextPage: () -> Unit = {},
    onReload: () -> Unit = {},
    onDesktopModeToggle: () -> Unit = {},
    onFavorite: () -> Unit = {},
    isDesktopMode: Boolean = false,
    isHeadlessMode: Boolean = false,
    onBookmarks: () -> Unit = {},
    onDownloads: () -> Unit = {},
    onHistory: () -> Unit = {},
    onSettings: () -> Unit = {},
    onShowTabs: () -> Unit = {},
    onShowFavorites: () -> Unit = {},
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
                modifier = Modifier
                    .glassBar(cornerRadius = 8.dp),
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
            modifier = Modifier
                .glassBar(cornerRadius = 27.dp),
            color = OceanTheme.surface,
            shape = RoundedCornerShape(27.dp),
            shadowElevation = 0.dp,  // FIX: Remove shadow artifacts in landscape mode
            border = BorderStroke(1.dp, OceanTheme.border)  // FIX: Use border instead
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // FIX: Switch content based on currentLevel
                when (currentLevel) {
                    CommandBarLevel.MAIN -> {
                        // MAIN: Back, Home, Tabs (3D), Favorites (3D), Page, Menu
                        CommandButton(
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            label = "Back",
                            onClick = onBack,
                            onFocus = { onLabelChange("Go Back") },
                            onBlur = { onLabelChange("") },
                            backgroundColor = OceanTheme.surfaceElevated
                        )
                        CommandButton(
                            icon = Icons.Default.Home,
                            label = "Home",
                            onClick = onHome,
                            onFocus = { onLabelChange("Go Home") },
                            onBlur = { onLabelChange("") },
                            backgroundColor = OceanTheme.surfaceElevated
                        )
                        // Tabs - 3D Spatial Tab Switcher (directly accessible)
                        CommandButton(
                            icon = Icons.Default.Layers,
                            label = "Tabs",
                            onClick = onShowTabs,
                            onFocus = { onLabelChange("Show Tabs (3D View)") },
                            onBlur = { onLabelChange("") },
                            backgroundColor = OceanTheme.primary.copy(alpha = 0.3f)
                        )
                        // Favorites - 3D Spatial Favorites Shelf (directly accessible)
                        CommandButton(
                            icon = Icons.Default.Star,
                            label = "Favs",
                            onClick = onShowFavorites,
                            onFocus = { onLabelChange("Show Favorites (3D View)") },
                            onBlur = { onLabelChange("") },
                            backgroundColor = OceanTheme.starActive.copy(alpha = 0.3f)
                        )
                        CommandButton(
                            icon = Icons.Default.Web,
                            label = "Page",
                            onClick = onPageClick,
                            onFocus = { onLabelChange("Page Controls") },
                            onBlur = { onLabelChange("") },
                            backgroundColor = OceanTheme.surfaceElevated
                        )
                        CommandButton(
                            icon = Icons.Default.MoreVert,
                            label = "Menu",
                            onClick = onMenuClick,
                            onFocus = { onLabelChange("Menu") },
                            onBlur = { onLabelChange("") },
                            backgroundColor = OceanTheme.surfaceElevated
                        )
                    }
                    CommandBarLevel.SCROLL -> {
                        // SCROLL: Close, Up, Down, Top, Bottom, Freeze
                        CommandButton(
                            icon = Icons.Default.Close,
                            label = "Close",
                            onClick = { onLevelChange(CommandBarLevel.MAIN) },
                            onFocus = { onLabelChange("Back to Main") },
                            onBlur = { onLabelChange("") },
                            backgroundColor = OceanTheme.surfaceElevated
                        )
                        CommandButton(
                            icon = Icons.Default.KeyboardArrowUp,
                            label = "Up",
                            onClick = onScrollUp,
                            onFocus = { onLabelChange("Scroll Up") },
                            onBlur = { onLabelChange("") },
                            backgroundColor = OceanTheme.surfaceElevated
                        )
                        CommandButton(
                            icon = Icons.Default.KeyboardArrowDown,
                            label = "Down",
                            onClick = onScrollDown,
                            onFocus = { onLabelChange("Scroll Down") },
                            onBlur = { onLabelChange("") },
                            backgroundColor = OceanTheme.surfaceElevated
                        )
                        CommandButton(
                            icon = Icons.Default.VerticalAlignTop,
                            label = "Top",
                            onClick = onScrollTop,
                            onFocus = { onLabelChange("Go to Top") },
                            onBlur = { onLabelChange("") },
                            backgroundColor = OceanTheme.surfaceElevated
                        )
                        CommandButton(
                            icon = Icons.Default.VerticalAlignBottom,
                            label = "Bottom",
                            onClick = onScrollBottom,
                            onFocus = { onLabelChange("Go to Bottom") },
                            onBlur = { onLabelChange("") },
                            backgroundColor = OceanTheme.surfaceElevated
                        )
                        CommandButton(
                            icon = if (isScrollFrozen) Icons.Default.LockOpen else Icons.Default.Lock,
                            label = if (isScrollFrozen) "Unfreeze" else "Freeze",
                            onClick = onFreezePage,
                            onFocus = { onLabelChange(if (isScrollFrozen) "Unfreeze" else "Freeze") },
                            onBlur = { onLabelChange("") },
                            backgroundColor = if (isScrollFrozen) OceanTheme.primary else OceanTheme.surfaceElevated,
                            isActive = isScrollFrozen
                        )
                    }
                    CommandBarLevel.ZOOM -> {
                        // ZOOM: Close, Out, In, 50%, 100%, 150%
                        CommandButton(
                            icon = Icons.Default.Close,
                            label = "Close",
                            onClick = { onLevelChange(CommandBarLevel.MAIN) },
                            onFocus = { onLabelChange("Back to Main") },
                            onBlur = { onLabelChange("") },
                            backgroundColor = OceanTheme.surfaceElevated
                        )
                        CommandButton(
                            icon = Icons.Default.ZoomOut,
                            label = "Out",
                            onClick = onZoomOut,
                            onFocus = { onLabelChange("Zoom Out") },
                            onBlur = { onLabelChange("") },
                            backgroundColor = OceanTheme.surfaceElevated
                        )
                        CommandButton(
                            icon = Icons.Default.ZoomIn,
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
                        // PAGE level - content depends on headless mode
                        CommandButton(
                            icon = Icons.Default.Close,
                            label = "Close",
                            onClick = { onLevelChange(CommandBarLevel.MAIN) },
                            onFocus = { onLabelChange("Back to Main") },
                            onBlur = { onLabelChange("") },
                            backgroundColor = OceanTheme.surfaceElevated
                        )
                        CommandButton(
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            label = "Prev",
                            onClick = onPreviousPage,
                            onFocus = { onLabelChange("Previous Page") },
                            onBlur = { onLabelChange("") },
                            backgroundColor = OceanTheme.surfaceElevated
                        )
                        CommandButton(
                            icon = Icons.AutoMirrored.Filled.ArrowForward,
                            label = "Next",
                            onClick = onNextPage,
                            onFocus = { onLabelChange("Next Page") },
                            onBlur = { onLabelChange("") },
                            backgroundColor = OceanTheme.surfaceElevated
                        )
                        CommandButton(
                            icon = Icons.Default.Refresh,
                            label = "Reload",
                            onClick = onReload,
                            onFocus = { onLabelChange("Reload Page") },
                            onBlur = { onLabelChange("") },
                            backgroundColor = OceanTheme.surfaceElevated
                        )
                        if (isHeadlessMode) {
                            // Headless: Show Desktop/Favorite (not in AddressBar)
                            CommandButton(
                                icon = if (isDesktopMode) Icons.Default.PhoneAndroid else Icons.Default.Laptop,
                                label = if (isDesktopMode) "Mobile" else "Desktop",
                                onClick = onDesktopModeToggle,
                                onFocus = { onLabelChange(if (isDesktopMode) "Switch to Mobile" else "Switch to Desktop") },
                                onBlur = { onLabelChange("") },
                                backgroundColor = if (isDesktopMode) OceanTheme.primary else OceanTheme.surfaceElevated,
                                isActive = isDesktopMode
                            )
                            CommandButton(
                                icon = Icons.Default.Star,
                                label = "Favorite",
                                onClick = onFavorite,
                                onFocus = { onLabelChange("Add to Favorites") },
                                onBlur = { onLabelChange("") },
                                backgroundColor = OceanTheme.surfaceElevated
                            )
                        } else {
                            // Normal: Show Zoom controls (Desktop/Favorite in AddressBar)
                            CommandButton(
                                icon = Icons.Default.ZoomIn,
                                label = "Zoom+",
                                onClick = onZoomIn,
                                onFocus = { onLabelChange("Zoom In") },
                                onBlur = { onLabelChange("") },
                                backgroundColor = OceanTheme.surfaceElevated
                            )
                            CommandButton(
                                icon = Icons.Default.ZoomOut,
                                label = "Zoom-",
                                onClick = onZoomOut,
                                onFocus = { onLabelChange("Zoom Out") },
                                onBlur = { onLabelChange("") },
                                backgroundColor = OceanTheme.surfaceElevated
                            )
                        }
                    }
                    CommandBarLevel.MENU -> {
                        // MENU: Close, New Tab, Scroll, History, Downloads, Settings
                        CommandButton(
                            icon = Icons.Default.Close,
                            label = "Close",
                            onClick = { onLevelChange(CommandBarLevel.MAIN) },
                            onFocus = { onLabelChange("Back to Main") },
                            onBlur = { onLabelChange("") },
                            backgroundColor = OceanTheme.surfaceElevated
                        )
                        CommandButton(
                            icon = Icons.Default.Add,
                            label = "New Tab",
                            onClick = onNewTab,
                            onFocus = { onLabelChange("Create New Tab") },
                            onBlur = { onLabelChange("") },
                            backgroundColor = OceanTheme.surfaceElevated
                        )
                        CommandButton(
                            icon = Icons.Default.SwapVert,
                            label = "Scroll",
                            onClick = { onLevelChange(CommandBarLevel.SCROLL) },
                            onFocus = { onLabelChange("Scroll Controls") },
                            onBlur = { onLabelChange("") },
                            backgroundColor = OceanTheme.surfaceElevated
                        )
                        CommandButton(
                            icon = Icons.Default.History,
                            label = "History",
                            onClick = onHistory,
                            onFocus = { onLabelChange("History") },
                            onBlur = { onLabelChange("") },
                            backgroundColor = OceanTheme.surfaceElevated
                        )
                        CommandButton(
                            icon = Icons.Default.Download,
                            label = "Downloads",
                            onClick = onDownloads,
                            onFocus = { onLabelChange("Downloads") },
                            onBlur = { onLabelChange("") },
                            backgroundColor = OceanTheme.surfaceElevated
                        )
                        CommandButton(
                            icon = Icons.Default.Settings,
                            label = "Settings",
                            onClick = onSettings,
                            onFocus = { onLabelChange("Settings") },
                            onBlur = { onLabelChange("") },
                            backgroundColor = OceanTheme.surfaceElevated
                        )
                    }
                }
            }
        }
    }
}
