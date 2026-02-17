package com.augmentalis.webavanue

import androidx.compose.runtime.Composable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import com.augmentalis.avanueui.theme.AvanueTheme

/**
 * MainCommandBarFlat - MAIN level (6 buttons max, no scrolling)
 *
 * Buttons: Back, Home, Tabs (3D), Favorites (3D), Page, Menu
 * Design: Direct access to most common actions + spatial views immediately accessible
 */
@Composable
fun MainCommandBarFlat(
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
        backgroundColor = AvanueTheme.colors.surfaceElevated
    )

    // Home
    CommandButton(
        icon = Icons.Default.Home,
        label = "Home",
        onClick = onHome,
        onFocus = { onLabelChange("Go Home") },
        onBlur = { onLabelChange("") },
        backgroundColor = AvanueTheme.colors.surfaceElevated
    )

    // Tabs - 3D Spatial Tab Switcher (directly accessible)
    CommandButton(
        icon = Icons.Default.Layers,
        label = "Tabs",
        onClick = onShowTabs,
        onFocus = { onLabelChange("Show Tabs (3D View)") },
        onBlur = { onLabelChange("") },
        backgroundColor = AvanueTheme.colors.primary.copy(alpha = 0.3f)
    )

    // Favorites - 3D Spatial Favorites Shelf (directly accessible)
    CommandButton(
        icon = Icons.Default.Star,
        label = "Favs",
        onClick = onShowFavorites,
        onFocus = { onLabelChange("Show Favorites (3D View)") },
        onBlur = { onLabelChange("") },
        backgroundColor = AvanueTheme.colors.starActive.copy(alpha = 0.3f)
    )

    // Page Commands (includes scroll and zoom)
    CommandButton(
        icon = Icons.Default.Web,
        label = "Page",
        onClick = onPageClick,
        onFocus = { onLabelChange("Page Controls") },
        onBlur = { onLabelChange("") },
        backgroundColor = AvanueTheme.colors.surfaceElevated
    )

    // Menu (settings, history, etc)
    CommandButton(
        icon = Icons.Default.MoreVert,
        label = "Menu",
        onClick = onMenuClick,
        onFocus = { onLabelChange("Menu") },
        onBlur = { onLabelChange("") },
        backgroundColor = AvanueTheme.colors.surfaceElevated
    )
}

/**
 * ScrollCommandBarFlat - SCROLL level (6 buttons, no scrolling needed)
 *
 * Buttons: Close, Up, Down, Top, Bottom, Freeze
 * Design: Essential scroll actions only
 */
@Composable
fun ScrollCommandBarFlat(
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
        backgroundColor = AvanueTheme.colors.surfaceElevated
    )

    // Scroll Up
    CommandButton(
        icon = Icons.Default.KeyboardArrowUp,
        label = "Up",
        onClick = onScrollUp,
        onFocus = { onLabelChange("Scroll Up") },
        onBlur = { onLabelChange("") },
        backgroundColor = AvanueTheme.colors.surfaceElevated
    )

    // Scroll Down
    CommandButton(
        icon = Icons.Default.KeyboardArrowDown,
        label = "Down",
        onClick = onScrollDown,
        onFocus = { onLabelChange("Scroll Down") },
        onBlur = { onLabelChange("") },
        backgroundColor = AvanueTheme.colors.surfaceElevated
    )

    // Scroll Top
    CommandButton(
        icon = Icons.Default.VerticalAlignTop,
        label = "Top",
        onClick = onScrollTop,
        onFocus = { onLabelChange("Go to Top") },
        onBlur = { onLabelChange("") },
        backgroundColor = AvanueTheme.colors.surfaceElevated
    )

    // Scroll Bottom
    CommandButton(
        icon = Icons.Default.VerticalAlignBottom,
        label = "Bottom",
        onClick = onScrollBottom,
        onFocus = { onLabelChange("Go to Bottom") },
        onBlur = { onLabelChange("") },
        backgroundColor = AvanueTheme.colors.surfaceElevated
    )

    // Freeze Page
    CommandButton(
        icon = if (isScrollFrozen) Icons.Default.LockOpen else Icons.Default.Lock,
        label = if (isScrollFrozen) "Unfreeze" else "Freeze",
        onClick = onFreezePage,
        onFocus = { onLabelChange(if (isScrollFrozen) "Unfreeze" else "Freeze") },
        onBlur = { onLabelChange("") },
        backgroundColor = if (isScrollFrozen) AvanueTheme.colors.primary else AvanueTheme.colors.surfaceElevated,
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
fun ZoomCommandBarFlat(
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
        backgroundColor = AvanueTheme.colors.surfaceElevated
    )

    // Zoom Out
    CommandButton(
        icon = Icons.Default.ZoomOut,
        label = "Out",
        onClick = onZoomOut,
        onFocus = { onLabelChange("Zoom Out") },
        onBlur = { onLabelChange("") },
        backgroundColor = AvanueTheme.colors.surfaceElevated
    )

    // Zoom In
    CommandButton(
        icon = Icons.Default.ZoomIn,
        label = "In",
        onClick = onZoomIn,
        onFocus = { onLabelChange("Zoom In") },
        onBlur = { onLabelChange("") },
        backgroundColor = AvanueTheme.colors.surfaceElevated
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
 * PageCommandBarFlat - PAGE level (variable buttons based on headless mode)
 *
 * Normal mode (6 buttons): Close, Prev, Next, Reload, Desktop, Zoom+
 * Headless mode (6 buttons): Close, Prev, Next, Reload, Desktop, Favorite
 */
@Composable
fun PageCommandBarFlat(
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
        backgroundColor = AvanueTheme.colors.surfaceElevated
    )

    // Previous Page
    CommandButton(
        icon = Icons.AutoMirrored.Filled.ArrowBack,
        label = "Prev",
        onClick = onPreviousPage,
        onFocus = { onLabelChange("Previous Page") },
        onBlur = { onLabelChange("") },
        backgroundColor = AvanueTheme.colors.surfaceElevated
    )

    // Next Page
    CommandButton(
        icon = Icons.AutoMirrored.Filled.ArrowForward,
        label = "Next",
        onClick = onNextPage,
        onFocus = { onLabelChange("Next Page") },
        onBlur = { onLabelChange("") },
        backgroundColor = AvanueTheme.colors.surfaceElevated
    )

    // Reload
    CommandButton(
        icon = Icons.Default.Refresh,
        label = "Reload",
        onClick = onReload,
        onFocus = { onLabelChange("Reload Page") },
        onBlur = { onLabelChange("") },
        backgroundColor = AvanueTheme.colors.surfaceElevated
    )

    if (isHeadlessMode) {
        // Headless mode: Show Desktop Mode and Favorite (not in address bar)
        CommandButton(
            icon = if (isDesktopMode) Icons.Default.PhoneAndroid else Icons.Default.Laptop,
            label = if (isDesktopMode) "Mobile" else "Advanced",
            onClick = onDesktopModeToggle,
            onFocus = { onLabelChange(if (isDesktopMode) "Switch to Mobile" else "Switch to Advanced") },
            onBlur = { onLabelChange("") },
            backgroundColor = if (isDesktopMode) AvanueTheme.colors.primary else AvanueTheme.colors.surfaceElevated,
            isActive = isDesktopMode
        )

        CommandButton(
            icon = Icons.Default.Star,
            label = "Favorite",
            onClick = onFavorite,
            onFocus = { onLabelChange("Add to Favorites") },
            onBlur = { onLabelChange("") },
            backgroundColor = AvanueTheme.colors.surfaceElevated
        )
    } else {
        // Normal mode: Desktop toggle (moved from AddressBar) + Zoom
        CommandButton(
            icon = if (isDesktopMode) Icons.Default.PhoneAndroid else Icons.Default.Laptop,
            label = if (isDesktopMode) "Mobile" else "Desktop",
            onClick = onDesktopModeToggle,
            onFocus = { onLabelChange(if (isDesktopMode) "Switch to Mobile" else "Switch to Desktop") },
            onBlur = { onLabelChange("") },
            backgroundColor = if (isDesktopMode) AvanueTheme.colors.primary else AvanueTheme.colors.surfaceElevated,
            isActive = isDesktopMode
        )

        CommandButton(
            icon = Icons.Default.ZoomIn,
            label = "Zoom+",
            onClick = onZoomIn,
            onFocus = { onLabelChange("Zoom In") },
            onBlur = { onLabelChange("") },
            backgroundColor = AvanueTheme.colors.surfaceElevated
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
fun MenuCommandBarFlat(
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
        backgroundColor = AvanueTheme.colors.surfaceElevated
    )

    // New Tab
    CommandButton(
        icon = Icons.Default.Add,
        label = "New Tab",
        onClick = onNewTab,
        onFocus = { onLabelChange("Create New Tab") },
        onBlur = { onLabelChange("") },
        backgroundColor = AvanueTheme.colors.surfaceElevated
    )

    // Scroll Controls
    CommandButton(
        icon = Icons.Default.SwapVert,
        label = "Scroll",
        onClick = onScrollClick,
        onFocus = { onLabelChange("Scroll Controls") },
        onBlur = { onLabelChange("") },
        backgroundColor = AvanueTheme.colors.surfaceElevated
    )

    // History
    CommandButton(
        icon = Icons.Default.History,
        label = "History",
        onClick = onHistory,
        onFocus = { onLabelChange("History") },
        onBlur = { onLabelChange("") },
        backgroundColor = AvanueTheme.colors.surfaceElevated
    )

    // Files (download manager)
    CommandButton(
        icon = Icons.Default.Folder,
        label = "Files",
        onClick = onDownloads,
        onFocus = { onLabelChange("Files") },
        onBlur = { onLabelChange("") },
        backgroundColor = AvanueTheme.colors.surfaceElevated
    )

    // Settings
    CommandButton(
        icon = Icons.Default.Settings,
        label = "Settings",
        onClick = onSettings,
        onFocus = { onLabelChange("Settings") },
        onBlur = { onLabelChange("") },
        backgroundColor = AvanueTheme.colors.surfaceElevated
    )

    // Fullscreen toggle (Headless mode)
    CommandButton(
        icon = if (isHeadlessMode) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
        label = if (isHeadlessMode) "Exit Full" else "Fullscreen",
        onClick = onToggleHeadlessMode,
        onFocus = { onLabelChange(if (isHeadlessMode) "Exit Fullscreen Mode" else "Enter Fullscreen Mode") },
        onBlur = { onLabelChange("") },
        backgroundColor = if (isHeadlessMode) AvanueTheme.colors.primary else AvanueTheme.colors.surfaceElevated,
        isActive = isHeadlessMode
    )
}
