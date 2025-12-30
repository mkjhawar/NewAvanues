package com.augmentalis.webavanue.ui.screen.browser.commandbar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.augmentalis.webavanue.ui.screen.theme.glassBar
import com.augmentalis.webavanue.ui.screen.theme.OceanTheme

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

    // Spatial z-level container - command bar floats at HUD layer
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .graphicsLayer { shadowElevation = 16f }
    ) {
        // Command label - floats above command bar
        AnimatedVisibility(
            visible = currentLabel.isNotEmpty(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Surface(
                modifier = Modifier.glassBar(cornerRadius = 12.dp),
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
                .graphicsLayer { shadowElevation = 12f },
            color = OceanTheme.surface.copy(alpha = 0.92f),
            shape = RoundedCornerShape(12.dp),
            shadowElevation = 12.dp,
            tonalElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
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

                HorizontalCommandBarContent(
                    currentLevel = currentLevel,
                    onLevelChange = { currentLevel = it },
                    onLabelChange = { currentLabel = it },
                    onBack = onBack,
                    onForward = onForward,
                    onHome = onHome,
                    onRefresh = onRefresh,
                    onScrollUp = onScrollUp,
                    onScrollDown = onScrollDown,
                    onScrollTop = onScrollTop,
                    onScrollBottom = onScrollBottom,
                    onFreezePage = onFreezePage,
                    isScrollFrozen = isScrollFrozen,
                    onZoomIn = onZoomIn,
                    onZoomOut = onZoomOut,
                    onZoomLevel = onZoomLevel,
                    onDesktopModeToggle = onDesktopModeToggle,
                    onFavorite = onFavorite,
                    isDesktopMode = isDesktopMode,
                    isHeadlessMode = isHeadlessMode,
                    onBookmarks = onBookmarks,
                    onDownloads = onDownloads,
                    onHistory = onHistory,
                    onSettings = onSettings,
                    onNewTab = onNewTab
                )
            }
        }
    }
}

@Composable
private fun HorizontalCommandBarContent(
    currentLevel: CommandBarLevel,
    onLevelChange: (CommandBarLevel) -> Unit,
    onLabelChange: (String) -> Unit,
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
    isHeadlessMode: Boolean,
    onBookmarks: () -> Unit,
    onDownloads: () -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit,
    onNewTab: () -> Unit
) {
    when (currentLevel) {
        CommandBarLevel.MAIN -> {
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
            CommandButton(
                icon = Icons.Default.Add,
                label = "New",
                onClick = onNewTab,
                onFocus = { onLabelChange("New Tab") },
                onBlur = { onLabelChange("") },
                backgroundColor = OceanTheme.surfaceElevated
            )
            CommandButton(
                icon = Icons.Default.SwapVert,
                label = "Scroll",
                onClick = { onLevelChange(CommandBarLevel.SCROLL) },
                onFocus = { onLabelChange("Scroll") },
                onBlur = { onLabelChange("") },
                backgroundColor = OceanTheme.surfaceElevated
            )
            CommandButton(
                icon = Icons.Default.Web,
                label = "Page",
                onClick = { onLevelChange(CommandBarLevel.PAGE) },
                onFocus = { onLabelChange("Page") },
                onBlur = { onLabelChange("") },
                backgroundColor = OceanTheme.surfaceElevated
            )
            CommandButton(
                icon = Icons.Default.MoreVert,
                label = "Menu",
                onClick = { onLevelChange(CommandBarLevel.MENU) },
                onFocus = { onLabelChange("Menu") },
                onBlur = { onLabelChange("") },
                backgroundColor = OceanTheme.surfaceElevated
            )
        }
        CommandBarLevel.SCROLL -> {
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
                onClick = onBack,
                onFocus = { onLabelChange("Previous Page") },
                onBlur = { onLabelChange("") },
                backgroundColor = OceanTheme.surfaceElevated
            )
            CommandButton(
                icon = Icons.AutoMirrored.Filled.ArrowForward,
                label = "Next",
                onClick = onForward,
                onFocus = { onLabelChange("Next Page") },
                onBlur = { onLabelChange("") },
                backgroundColor = OceanTheme.surfaceElevated
            )
            CommandButton(
                icon = Icons.Default.Refresh,
                label = "Reload",
                onClick = onRefresh,
                onFocus = { onLabelChange("Reload Page") },
                onBlur = { onLabelChange("") },
                backgroundColor = OceanTheme.surfaceElevated
            )
            if (isHeadlessMode) {
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
            CommandButton(
                icon = Icons.Default.Close,
                label = "Close",
                onClick = { onLevelChange(CommandBarLevel.MAIN) },
                onFocus = { onLabelChange("Back to Main") },
                onBlur = { onLabelChange("") },
                backgroundColor = OceanTheme.surfaceElevated
            )
            CommandButton(
                icon = Icons.Default.Bookmarks,
                label = "Bookmarks",
                onClick = onBookmarks,
                onFocus = { onLabelChange("Bookmarks") },
                onBlur = { onLabelChange("") },
                backgroundColor = OceanTheme.surfaceElevated
            )
            CommandButton(
                icon = Icons.Default.FileDownload,
                label = "Downloads",
                onClick = onDownloads,
                onFocus = { onLabelChange("Downloads") },
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
