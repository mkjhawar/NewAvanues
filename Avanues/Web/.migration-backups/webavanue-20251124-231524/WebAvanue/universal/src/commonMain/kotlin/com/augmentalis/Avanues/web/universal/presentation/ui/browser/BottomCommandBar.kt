package com.augmentalis.Avanues.web.universal.presentation.ui.browser

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Command bar menu levels - LEGACY STRUCTURE
 *
 * Hierarchy:
 * MAIN → NAVIGATION_COMMANDS | WEB_COMMANDS
 * NAVIGATION_COMMANDS → SCROLL | CURSOR | ZOOM → ZOOM_LEVEL
 * WEB_COMMANDS → TOUCH | FAVORITE
 */
enum class CommandBarLevel {
    MAIN,                   // Step 1: Back, Home, Add Page, Navigation Commands, Web Commands, Prev/Next Tab
    NAVIGATION_COMMANDS,    // Step 1.2: Back, Home, Scroll Commands, Cursor Commands, Zoom Commands
    WEB_COMMANDS,           // Step 1.3: Back, Home, Prev/Next Page, Reload, Desktop Mode, Touch, Favorite, Clear Cache
    SCROLL,                 // Step 1.2.1: Back, Home, Scroll Up/Down/Left/Right, Page Up/Down, Freeze
    CURSOR,                 // Step 1.2.2: Back, Home, Select/Click, Double Click
    ZOOM,                   // Step 1.2.3: Back, Home, Zoom In, Zoom Out, Zoom Level
    ZOOM_LEVEL,             // Step 1.2.3.1: Back, Home, Zoom Level 1-5
    TOUCH,                  // Step 1.3.1: Back, Home, Start/Stop Drag, Rotate, Pinch Open/Close
    MENU                    // Additional: Bookmarks, Downloads, History, Settings
}

/**
 * BottomCommandBar - Voice-first floating command bar with contextual menus
 *
 * Features:
 * - Floating pill-shaped bar at bottom center
 * - Contextual menu levels (click an icon to see related options)
 * - Back button to return to previous level
 * - Voice input with listening animation
 *
 * @param onBack Callback for back navigation
 * @param onForward Callback for forward navigation
 * @param onHome Callback for home navigation
 * @param onRefresh Callback for refresh
 * @param onScrollUp Callback for scroll up
 * @param onScrollDown Callback for scroll down
 * @param onScrollTop Callback for scroll to top
 * @param onScrollBottom Callback for scroll to bottom
 * @param onSelect Callback for select/click
 * @param onVoice Callback for voice input
 * @param onTextCommand Callback for text command toggle
 * @param onBookmarks Callback for bookmarks
 * @param onDownloads Callback for downloads
 * @param onHistory Callback for history
 * @param onSettings Callback for settings
 * @param isListening Whether voice input is active
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
    onScrollLeft: () -> Unit = {},
    onScrollRight: () -> Unit = {},
    onSelect: () -> Unit = {},
    onDoubleClick: () -> Unit = {},
    onVoice: () -> Unit = {},
    onTextCommand: () -> Unit = {},
    onBookmarks: () -> Unit = {},
    onDownloads: () -> Unit = {},
    onHistory: () -> Unit = {},
    onSettings: () -> Unit = {},
    onDesktopModeToggle: () -> Unit = {},
    onZoomIn: () -> Unit = {},
    onZoomOut: () -> Unit = {},
    onZoomLevel: (Int) -> Unit = {},
    onClearCookies: () -> Unit = {},
    onClearCache: () -> Unit = {},
    onFreezePage: () -> Unit = {},
    onFavorite: () -> Unit = {},
    onDragToggle: () -> Unit = {},
    onRotateImage: () -> Unit = {},
    onPinchOpen: () -> Unit = {},
    onPinchClose: () -> Unit = {},
    onPreviousTab: () -> Unit = {},
    onNextTab: () -> Unit = {},
    onNewTab: () -> Unit = {},
    onCloseTab: () -> Unit = {},
    isListening: Boolean = false,
    isDesktopMode: Boolean = false,
    isScrollFrozen: Boolean = false,
    isDragMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    var currentLevel by remember { mutableStateOf(CommandBarLevel.MAIN) }
    var currentLabel by remember { mutableStateOf("") }

    // Dark 3D theme colors from demo
    val bgCommandBar = Color(0xFF0F3460).copy(alpha = 0.95f)
    val bgSurface = Color(0xFF16213E)
    val accentVoice = Color(0xFFA78BFA)

    Box(
        modifier = modifier
            .fillMaxWidth()
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
                    color = bgCommandBar,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = currentLabel.uppercase(),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFA0A0A0)
                    )
                }
            }

            // Command bar container
            Surface(
                color = bgCommandBar,
                shape = RoundedCornerShape(27.dp),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when (currentLevel) {
                        CommandBarLevel.MAIN -> MainCommandBar(
                            onNavigationCommandsClick = { currentLevel = CommandBarLevel.NAVIGATION_COMMANDS },
                            onWebCommandsClick = { currentLevel = CommandBarLevel.WEB_COMMANDS },
                            onBack = onBack,
                            onHome = onHome,
                            onNewTab = onNewTab,
                            onPreviousTab = onPreviousTab,
                            onNextTab = onNextTab,
                            isListening = isListening,
                            bgSurface = bgSurface,
                            accentVoice = accentVoice,
                            onLabelChange = { currentLabel = it }
                        )

                        CommandBarLevel.NAVIGATION_COMMANDS -> NavigationCommandsBar(
                            onBack = onBack,
                            onHome = onHome,
                            onScrollClick = { currentLevel = CommandBarLevel.SCROLL },
                            onCursorClick = { currentLevel = CommandBarLevel.CURSOR },
                            onZoomClick = { currentLevel = CommandBarLevel.ZOOM },
                            onBackToMain = { currentLevel = CommandBarLevel.MAIN },
                            bgSurface = bgSurface,
                            onLabelChange = { currentLabel = it }
                        )

                        CommandBarLevel.WEB_COMMANDS -> WebCommandsBar(
                            onBack = onBack,
                            onHome = onHome,
                            onPreviousPage = onBack,
                            onNextPage = onForward,
                            onReload = onRefresh,
                            onDesktopModeToggle = onDesktopModeToggle,
                            onTouchClick = { currentLevel = CommandBarLevel.TOUCH },
                            onFavorite = onFavorite,
                            onClearCache = onClearCache,
                            isDesktopMode = isDesktopMode,
                            onBackToMain = { currentLevel = CommandBarLevel.MAIN },
                            bgSurface = bgSurface,
                            accentVoice = accentVoice,
                            onLabelChange = { currentLabel = it }
                        )

                        CommandBarLevel.SCROLL -> ScrollCommandBar(
                            onBack = onBack,
                            onHome = onHome,
                            onScrollUp = onScrollUp,
                            onScrollDown = onScrollDown,
                            onScrollLeft = onScrollLeft,
                            onScrollRight = onScrollRight,
                            onScrollTop = onScrollTop,
                            onScrollBottom = onScrollBottom,
                            onFreezePage = onFreezePage,
                            isScrollFrozen = isScrollFrozen,
                            onBackToParent = { currentLevel = CommandBarLevel.NAVIGATION_COMMANDS },
                            bgSurface = bgSurface,
                            onLabelChange = { currentLabel = it }
                        )

                        CommandBarLevel.CURSOR -> CursorCommandBar(
                            onBack = onBack,
                            onHome = onHome,
                            onSelect = onSelect,
                            onDoubleClick = onDoubleClick,
                            onBackToParent = { currentLevel = CommandBarLevel.NAVIGATION_COMMANDS },
                            bgSurface = bgSurface,
                            onLabelChange = { currentLabel = it }
                        )

                        CommandBarLevel.ZOOM -> ZoomCommandBar(
                            onBack = onBack,
                            onHome = onHome,
                            onZoomIn = onZoomIn,
                            onZoomOut = onZoomOut,
                            onZoomLevelClick = { currentLevel = CommandBarLevel.ZOOM_LEVEL },
                            onBackToParent = { currentLevel = CommandBarLevel.NAVIGATION_COMMANDS },
                            bgSurface = bgSurface,
                            onLabelChange = { currentLabel = it }
                        )

                        CommandBarLevel.ZOOM_LEVEL -> ZoomLevelCommandBar(
                            onBack = onBack,
                            onHome = onHome,
                            onZoomLevel = onZoomLevel,
                            onBackToZoom = { currentLevel = CommandBarLevel.ZOOM },
                            bgSurface = bgSurface,
                            onLabelChange = { currentLabel = it }
                        )

                        CommandBarLevel.TOUCH -> TouchCommandBar(
                            onBack = onBack,
                            onHome = onHome,
                            onDragToggle = onDragToggle,
                            onRotateImage = onRotateImage,
                            onPinchOpen = onPinchOpen,
                            onPinchClose = onPinchClose,
                            isDragMode = isDragMode,
                            onBackToParent = { currentLevel = CommandBarLevel.WEB_COMMANDS },
                            bgSurface = bgSurface,
                            accentVoice = accentVoice,
                            onLabelChange = { currentLabel = it }
                        )

                        CommandBarLevel.MENU -> MenuCommandBar(
                            onBack = onBack,
                            onHome = onHome,
                            onBookmarks = onBookmarks,
                            onDownloads = onDownloads,
                            onHistory = onHistory,
                            onSettings = onSettings,
                            onBackToMain = { currentLevel = CommandBarLevel.MAIN },
                            bgSurface = bgSurface,
                            onLabelChange = { currentLabel = it }
                        )
                    }
                }
            }
        }
    }
}

/**
 * MainCommandBar - Step 1: Main menu
 * Commands: Back, Home, Add Page, Navigation Commands, Web Commands, Prev/Next Tab
 */
@Composable
private fun MainCommandBar(
    onNavigationCommandsClick: () -> Unit,
    onWebCommandsClick: () -> Unit,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onNewTab: () -> Unit,
    onPreviousTab: () -> Unit,
    onNextTab: () -> Unit,
    isListening: Boolean,
    bgSurface: Color,
    accentVoice: Color,
    onLabelChange: (String) -> Unit
) {
    // Back
    CommandButton(
        icon = Icons.AutoMirrored.Filled.ArrowBack,
        label = "Back",
        onClick = onBack,
        onFocus = { onLabelChange("Back") },
        onBlur = { onLabelChange("") },
        backgroundColor = bgSurface
    )

    // Home
    CommandButton(
        icon = Icons.Default.Home,
        label = "Home",
        onClick = onHome,
        onFocus = { onLabelChange("Home") },
        onBlur = { onLabelChange("") },
        backgroundColor = bgSurface
    )

    // Add Page
    CommandButton(
        icon = Icons.Default.Add,
        label = "Add",
        onClick = onNewTab,
        onFocus = { onLabelChange("Add Page") },
        onBlur = { onLabelChange("") },
        backgroundColor = bgSurface
    )

    // Navigation Commands
    CommandButton(
        icon = Icons.Default.Menu,
        label = "Nav",
        onClick = onNavigationCommandsClick,
        onFocus = { onLabelChange("Navigation Commands") },
        onBlur = { onLabelChange("") },
        backgroundColor = bgSurface
    )

    // Web Commands
    CommandButton(
        icon = Icons.Default.Settings,
        label = "Web",
        onClick = onWebCommandsClick,
        onFocus = { onLabelChange("Web Commands") },
        onBlur = { onLabelChange("") },
        backgroundColor = bgSurface
    )

    // Previous Tab
    CommandButton(
        icon = Icons.Default.KeyboardArrowLeft,
        label = "Prev",
        onClick = onPreviousTab,
        onFocus = { onLabelChange("Previous Tab") },
        onBlur = { onLabelChange("") },
        backgroundColor = bgSurface
    )

    // Next Tab
    CommandButton(
        icon = Icons.Default.KeyboardArrowRight,
        label = "Next",
        onClick = onNextTab,
        onFocus = { onLabelChange("Next Tab") },
        onBlur = { onLabelChange("") },
        backgroundColor = bgSurface
    )
}

/**
 * NavigationCommandsBar - Step 1.2: Navigation commands menu
 * Commands: Back, Home, Scroll Commands, Cursor Commands, Zoom Commands
 */
@Composable
private fun NavigationCommandsBar(
    onBack: () -> Unit,
    onHome: () -> Unit,
    onScrollClick: () -> Unit,
    onCursorClick: () -> Unit,
    onZoomClick: () -> Unit,
    onBackToMain: () -> Unit,
    bgSurface: Color,
    onLabelChange: (String) -> Unit
) {
    // Close (back to main)
    CommandButton(
        icon = Icons.Default.Close,
        label = "Close",
        onClick = onBackToMain,
        onFocus = { onLabelChange("Back to Main") },
        onBlur = { onLabelChange("") },
        backgroundColor = bgSurface
    )

    Spacer(modifier = Modifier.width(8.dp))

    // Back
    CommandButton(
        icon = Icons.AutoMirrored.Filled.ArrowBack,
        label = "Back",
        onClick = onBack,
        onFocus = { onLabelChange("Back") },
        onBlur = { onLabelChange("") },
        backgroundColor = bgSurface
    )

    // Home
    CommandButton(
        icon = Icons.Default.Home,
        label = "Home",
        onClick = onHome,
        onFocus = { onLabelChange("Home") },
        onBlur = { onLabelChange("") },
        backgroundColor = bgSurface
    )

    // Scroll Commands
    CommandButton(
        icon = Icons.Default.KeyboardArrowDown,
        label = "Scroll",
        onClick = onScrollClick,
        onFocus = { onLabelChange("Scroll Commands") },
        onBlur = { onLabelChange("") },
        backgroundColor = bgSurface
    )

    // Cursor Commands
    CommandButton(
        icon = Icons.Default.LocationOn,
        label = "Cursor",
        onClick = onCursorClick,
        onFocus = { onLabelChange("Cursor Commands") },
        onBlur = { onLabelChange("") },
        backgroundColor = bgSurface
    )

    // Zoom Commands
    CommandButton(
        icon = Icons.Default.Search,
        label = "Zoom",
        onClick = onZoomClick,
        onFocus = { onLabelChange("Zoom Commands") },
        onBlur = { onLabelChange("") },
        backgroundColor = bgSurface
    )
}

/**
 * WebCommandsBar - Step 1.3: Web commands menu
 * Commands: Back, Home, Prev/Next Page, Reload, Desktop Mode, Touch, Favorite, Clear Cache
 */
@Composable
private fun WebCommandsBar(
    onBack: () -> Unit,
    onHome: () -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onReload: () -> Unit,
    onDesktopModeToggle: () -> Unit,
    onTouchClick: () -> Unit,
    onFavorite: () -> Unit,
    onClearCache: () -> Unit,
    isDesktopMode: Boolean,
    onBackToMain: () -> Unit,
    bgSurface: Color,
    accentVoice: Color,
    onLabelChange: (String) -> Unit
) {
    // Close (back to main)
    CommandButton(
        icon = Icons.Default.Close,
        label = "Close",
        onClick = onBackToMain,
        onFocus = { onLabelChange("Back to Main") },
        onBlur = { onLabelChange("") },
        backgroundColor = bgSurface
    )

    Spacer(modifier = Modifier.width(8.dp))

    // Back
    CommandButton(
        icon = Icons.AutoMirrored.Filled.ArrowBack,
        label = "Back",
        onClick = onBack,
        onFocus = { onLabelChange("Back") },
        onBlur = { onLabelChange("") },
        backgroundColor = bgSurface
    )

    // Home
    CommandButton(
        icon = Icons.Default.Home,
        label = "Home",
        onClick = onHome,
        onFocus = { onLabelChange("Home") },
        onBlur = { onLabelChange("") },
        backgroundColor = bgSurface
    )

    // Previous Page
    CommandButton(
        icon = Icons.AutoMirrored.Filled.ArrowBack,
        label = "Prev",
        onClick = onPreviousPage,
        onFocus = { onLabelChange("Previous Page") },
        onBlur = { onLabelChange("") },
        backgroundColor = bgSurface
    )

    // Next Page
    CommandButton(
        icon = Icons.AutoMirrored.Filled.ArrowForward,
        label = "Next",
        onClick = onNextPage,
        onFocus = { onLabelChange("Next Page") },
        onBlur = { onLabelChange("") },
        backgroundColor = bgSurface
    )

    // Reload
    CommandButton(
        icon = Icons.Default.Refresh,
        label = "Reload",
        onClick = onReload,
        onFocus = { onLabelChange("Reload") },
        onBlur = { onLabelChange("") },
        backgroundColor = bgSurface
    )

    // Desktop Mode
    DesktopModeButton(
        isDesktopMode = isDesktopMode,
        onClick = onDesktopModeToggle,
        onFocus = { onLabelChange(if (isDesktopMode) "Mobile Mode" else "Desktop Mode") },
        onBlur = { onLabelChange("") },
        activeColor = accentVoice,
        backgroundColor = bgSurface
    )

    // Touch Commands
    CommandButton(
        icon = Icons.Default.LocationOn,
        label = "Touch",
        onClick = onTouchClick,
        onFocus = { onLabelChange("Touch Commands") },
        onBlur = { onLabelChange("") },
        backgroundColor = bgSurface
    )

    // Favorite
    CommandButton(
        icon = Icons.Default.Star,
        label = "Fav",
        onClick = onFavorite,
        onFocus = { onLabelChange("Add to Favorites") },
        onBlur = { onLabelChange("") },
        backgroundColor = bgSurface
    )

    // Clear Cache
    CommandButton(
        icon = Icons.Default.Delete,
        label = "Clear",
        onClick = onClearCache,
        onFocus = { onLabelChange("Clear Cache") },
        onBlur = { onLabelChange("") },
        backgroundColor = bgSurface
    )
}

/**
 * ScrollCommandBar - Step 1.2.1: Scroll commands
 * Commands: Back, Home, Scroll Up/Down/Left/Right, Page Up/Down, Freeze
 */
@Composable
private fun ScrollCommandBar(
    onBack: () -> Unit,
    onHome: () -> Unit,
    onScrollUp: () -> Unit,
    onScrollDown: () -> Unit,
    onScrollLeft: () -> Unit,
    onScrollRight: () -> Unit,
    onScrollTop: () -> Unit,
    onScrollBottom: () -> Unit,
    onFreezePage: () -> Unit,
    isScrollFrozen: Boolean,
    onBackToParent: () -> Unit,
    bgSurface: Color,
    onLabelChange: (String) -> Unit
) {
    // Close (back to navigation commands)
    CommandButton(
        icon = Icons.Default.Close,
        label = "Close",
        onClick = onBackToParent,
        onFocus = { onLabelChange("Back") },
        onBlur = { onLabelChange("") },
        backgroundColor = bgSurface
    )

    Spacer(modifier = Modifier.width(8.dp))

    // Back
    CommandButton(
        icon = Icons.AutoMirrored.Filled.ArrowBack,
        label = "Back",
        onClick = onBack,
        onFocus = { onLabelChange("Back") },
        onBlur = { onLabelChange("") },
        backgroundColor = bgSurface
    )

    // Home
    CommandButton(
        icon = Icons.Default.Home,
        label = "Home",
        onClick = onHome,
        onFocus = { onLabelChange("Home") },
        onBlur = { onLabelChange("") },
        backgroundColor = bgSurface
    )

    Spacer(modifier = Modifier.width(4.dp))

    // Scroll Up
    CommandButton(
        icon = Icons.Default.KeyboardArrowUp,
        label = "Up",
        onClick = onScrollUp,
        onFocus = { onLabelChange("Scroll Up") },
        onBlur = { onLabelChange("") },
        backgroundColor = bgSurface
    )

    // Scroll Down
    CommandButton(
        icon = Icons.Default.KeyboardArrowDown,
        label = "Down",
        onClick = onScrollDown,
        onFocus = { onLabelChange("Scroll Down") },
        onBlur = { onLabelChange("") },
        backgroundColor = bgSurface
    )

    // Scroll Left
    CommandButton(
        icon = Icons.Default.KeyboardArrowLeft,
        label = "Left",
        onClick = onScrollLeft,
        onFocus = { onLabelChange("Scroll Left") },
        onBlur = { onLabelChange("") },
        backgroundColor = bgSurface
    )

    // Scroll Right
    CommandButton(
        icon = Icons.Default.KeyboardArrowRight,
        label = "Right",
        onClick = onScrollRight,
        onFocus = { onLabelChange("Scroll Right") },
        onBlur = { onLabelChange("") },
        backgroundColor = bgSurface
    )

    // Scroll Top
    CommandButton(
        icon = Icons.Default.KeyboardArrowUp,
        label = "Top",
        onClick = onScrollTop,
        onFocus = { onLabelChange("Page Up") },
        onBlur = { onLabelChange("") },
        backgroundColor = bgSurface
    )

    // Scroll Bottom
    CommandButton(
        icon = Icons.Default.KeyboardArrowDown,
        label = "Bottom",
        onClick = onScrollBottom,
        onFocus = { onLabelChange("Page Down") },
        onBlur = { onLabelChange("") },
        backgroundColor = bgSurface
    )

    // Freeze Page
    CommandButton(
        icon = if (isScrollFrozen) Icons.Default.Lock else Icons.Default.Lock,
        label = if (isScrollFrozen) "Unfreeze" else "Freeze",
        onClick = onFreezePage,
        onFocus = { onLabelChange(if (isScrollFrozen) "Unfreeze Page" else "Freeze Page") },
        onBlur = { onLabelChange("") },
        backgroundColor = bgSurface
    )
}

/**
 * CursorCommandBar - Step 1.2.2: Cursor commands
 * Commands: Back, Home, Select/Click, Double Click
 */
@Composable
private fun CursorCommandBar(
    onBack: () -> Unit,
    onHome: () -> Unit,
    onSelect: () -> Unit,
    onDoubleClick: () -> Unit,
    onBackToParent: () -> Unit,
    bgSurface: Color,
    onLabelChange: (String) -> Unit
) {
    // Close (back to navigation commands)
    CommandButton(
        icon = Icons.Default.Close,
        label = "Close",
        onClick = onBackToParent,
        onFocus = { onLabelChange("Back") },
        onBlur = { onLabelChange("") },
        backgroundColor = bgSurface
    )

    Spacer(modifier = Modifier.width(8.dp))

    // Back
    CommandButton(
        icon = Icons.AutoMirrored.Filled.ArrowBack,
        label = "Back",
        onClick = onBack,
        onFocus = { onLabelChange("Back") },
        onBlur = { onLabelChange("") },
        backgroundColor = bgSurface
    )

    // Home
    CommandButton(
        icon = Icons.Default.Home,
        label = "Home",
        onClick = onHome,
        onFocus = { onLabelChange("Home") },
        onBlur = { onLabelChange("") },
        backgroundColor = bgSurface
    )

    Spacer(modifier = Modifier.width(4.dp))

    // Select/Click
    CommandButton(
        icon = Icons.Default.LocationOn,
        label = "Click",
        onClick = onSelect,
        onFocus = { onLabelChange("Select/Click") },
        onBlur = { onLabelChange("") },
        backgroundColor = bgSurface
    )

    // Double Click
    CommandButton(
        icon = Icons.Default.Refresh,
        label = "Double",
        onClick = onDoubleClick,
        onFocus = { onLabelChange("Double Click") },
        onBlur = { onLabelChange("") },
        backgroundColor = bgSurface
    )
}

/**
 * ZoomCommandBar - Step 1.2.3: Zoom commands
 * Commands: Back, Home, Zoom In, Zoom Out, Zoom Level
 */
@Composable
private fun ZoomCommandBar(
    onBack: () -> Unit,
    onHome: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onZoomLevelClick: () -> Unit,
    onBackToParent: () -> Unit,
    bgSurface: Color,
    onLabelChange: (String) -> Unit
) {
    // Close (back to navigation commands)
    CommandButton(
        icon = Icons.Default.Close,
        label = "Close",
        onClick = onBackToParent,
        onFocus = { onLabelChange("Back") },
        onBlur = { onLabelChange("") },
        backgroundColor = bgSurface
    )

    Spacer(modifier = Modifier.width(8.dp))

    // Back
    CommandButton(
        icon = Icons.AutoMirrored.Filled.ArrowBack,
        label = "Back",
        onClick = onBack,
        onFocus = { onLabelChange("Back") },
        onBlur = { onLabelChange("") },
        backgroundColor = bgSurface
    )

    // Home
    CommandButton(
        icon = Icons.Default.Home,
        label = "Home",
        onClick = onHome,
        onFocus = { onLabelChange("Home") },
        onBlur = { onLabelChange("") },
        backgroundColor = bgSurface
    )

    Spacer(modifier = Modifier.width(4.dp))

    // Zoom In
    CommandButton(
        icon = Icons.Default.Add,
        label = "Zoom In",
        onClick = onZoomIn,
        onFocus = { onLabelChange("Zoom In") },
        onBlur = { onLabelChange("") },
        backgroundColor = bgSurface
    )

    // Zoom Out
    CommandButton(
        icon = Icons.Default.KeyboardArrowDown,
        label = "Zoom Out",
        onClick = onZoomOut,
        onFocus = { onLabelChange("Zoom Out") },
        onBlur = { onLabelChange("") },
        backgroundColor = bgSurface
    )

    // Zoom Level
    CommandButton(
        icon = Icons.Default.Search,
        label = "Level",
        onClick = onZoomLevelClick,
        onFocus = { onLabelChange("Zoom Level") },
        onBlur = { onLabelChange("") },
        backgroundColor = bgSurface
    )
}

/**
 * ZoomLevelCommandBar - Step 1.2.3.1: Zoom level selection
 * Commands: Back, Home, Zoom Level 1-5
 */
@Composable
private fun ZoomLevelCommandBar(
    onBack: () -> Unit,
    onHome: () -> Unit,
    onZoomLevel: (Int) -> Unit,
    onBackToZoom: () -> Unit,
    bgSurface: Color,
    onLabelChange: (String) -> Unit
) {
    // Back to zoom
    CommandButton(
        icon = Icons.Default.Close,
        label = "Close",
        onClick = onBackToZoom,
        onFocus = { onLabelChange("Back to Zoom") },
        onBlur = { onLabelChange("") },
        backgroundColor = bgSurface
    )

    Spacer(modifier = Modifier.width(8.dp))

    // Back
    CommandButton(
        icon = Icons.AutoMirrored.Filled.ArrowBack,
        label = "Back",
        onClick = onBack,
        onFocus = { onLabelChange("Back") },
        onBlur = { onLabelChange("") },
        backgroundColor = bgSurface
    )

    // Home
    CommandButton(
        icon = Icons.Default.Home,
        label = "Home",
        onClick = onHome,
        onFocus = { onLabelChange("Home") },
        onBlur = { onLabelChange("") },
        backgroundColor = bgSurface
    )

    Spacer(modifier = Modifier.width(4.dp))

    // Level 1 (50%)
    CommandButton(
        icon = Icons.Default.Star,
        label = "1",
        onClick = { onZoomLevel(1) },
        onFocus = { onLabelChange("50%") },
        onBlur = { onLabelChange("") },
        backgroundColor = bgSurface
    )

    // Level 2 (75%)
    CommandButton(
        icon = Icons.Default.Star,
        label = "2",
        onClick = { onZoomLevel(2) },
        onFocus = { onLabelChange("75%") },
        onBlur = { onLabelChange("") },
        backgroundColor = bgSurface
    )

    // Level 3 (100%)
    CommandButton(
        icon = Icons.Default.Star,
        label = "3",
        onClick = { onZoomLevel(3) },
        onFocus = { onLabelChange("100%") },
        onBlur = { onLabelChange("") },
        backgroundColor = bgSurface
    )

    // Level 4 (125%)
    CommandButton(
        icon = Icons.Default.Star,
        label = "4",
        onClick = { onZoomLevel(4) },
        onFocus = { onLabelChange("125%") },
        onBlur = { onLabelChange("") },
        backgroundColor = bgSurface
    )

    // Level 5 (150%)
    CommandButton(
        icon = Icons.Default.Star,
        label = "5",
        onClick = { onZoomLevel(5) },
        onFocus = { onLabelChange("150%") },
        onBlur = { onLabelChange("") },
        backgroundColor = bgSurface
    )
}

/**
 * TouchCommandBar - Step 1.3.1: Touch commands
 * Commands: Back, Home, Start/Stop Drag, Rotate Image, Pinch Open, Pinch Close
 */
@Composable
private fun TouchCommandBar(
    onBack: () -> Unit,
    onHome: () -> Unit,
    onDragToggle: () -> Unit,
    onRotateImage: () -> Unit,
    onPinchOpen: () -> Unit,
    onPinchClose: () -> Unit,
    isDragMode: Boolean,
    onBackToParent: () -> Unit,
    bgSurface: Color,
    accentVoice: Color,
    onLabelChange: (String) -> Unit
) {
    // Close (back to web commands)
    CommandButton(
        icon = Icons.Default.Close,
        label = "Close",
        onClick = onBackToParent,
        onFocus = { onLabelChange("Back to Web") },
        onBlur = { onLabelChange("") },
        backgroundColor = bgSurface
    )

    Spacer(modifier = Modifier.width(8.dp))

    // Back
    CommandButton(
        icon = Icons.AutoMirrored.Filled.ArrowBack,
        label = "Back",
        onClick = onBack,
        onFocus = { onLabelChange("Back") },
        onBlur = { onLabelChange("") },
        backgroundColor = bgSurface
    )

    // Home
    CommandButton(
        icon = Icons.Default.Home,
        label = "Home",
        onClick = onHome,
        onFocus = { onLabelChange("Home") },
        onBlur = { onLabelChange("") },
        backgroundColor = bgSurface
    )

    Spacer(modifier = Modifier.width(4.dp))

    // Start/Stop Drag
    CommandButton(
        icon = Icons.Default.Call,
        label = if (isDragMode) "Stop" else "Drag",
        onClick = onDragToggle,
        onFocus = { onLabelChange(if (isDragMode) "Stop Drag" else "Start Drag") },
        onBlur = { onLabelChange("") },
        backgroundColor = if (isDragMode) accentVoice else bgSurface
    )

    // Rotate Image
    CommandButton(
        icon = Icons.Default.Refresh,
        label = "Rotate",
        onClick = onRotateImage,
        onFocus = { onLabelChange("Rotate Image") },
        onBlur = { onLabelChange("") },
        backgroundColor = bgSurface
    )

    // Pinch Open (Zoom In)
    CommandButton(
        icon = Icons.Default.Add,
        label = "Open",
        onClick = onPinchOpen,
        onFocus = { onLabelChange("Pinch Open") },
        onBlur = { onLabelChange("") },
        backgroundColor = bgSurface
    )

    // Pinch Close (Zoom Out)
    CommandButton(
        icon = Icons.Default.Clear,
        label = "Close",
        onClick = onPinchClose,
        onFocus = { onLabelChange("Pinch Close") },
        onBlur = { onLabelChange("") },
        backgroundColor = bgSurface
    )
}

/**
 * MenuCommandBar - Additional menu for navigation
 * Commands: Back, Home, Bookmarks, Downloads, History, Settings
 */
@Composable
private fun MenuCommandBar(
    onBack: () -> Unit,
    onHome: () -> Unit,
    onBookmarks: () -> Unit,
    onDownloads: () -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit,
    onBackToMain: () -> Unit,
    bgSurface: Color,
    onLabelChange: (String) -> Unit
) {
    // Back to main
    CommandButton(
        icon = Icons.Default.Close,
        label = "Close",
        onClick = onBackToMain,
        onFocus = { onLabelChange("Back to Main") },
        onBlur = { onLabelChange("") },
        backgroundColor = bgSurface
    )

    Spacer(modifier = Modifier.width(8.dp))

    // Back
    CommandButton(
        icon = Icons.AutoMirrored.Filled.ArrowBack,
        label = "Back",
        onClick = onBack,
        onFocus = { onLabelChange("Back") },
        onBlur = { onLabelChange("") },
        backgroundColor = bgSurface
    )

    // Home
    CommandButton(
        icon = Icons.Default.Home,
        label = "Home",
        onClick = onHome,
        onFocus = { onLabelChange("Home") },
        onBlur = { onLabelChange("") },
        backgroundColor = bgSurface
    )

    Spacer(modifier = Modifier.width(4.dp))

    // Bookmarks
    CommandButton(
        icon = Icons.Default.Star,
        label = "Bookmarks",
        onClick = onBookmarks,
        onFocus = { onLabelChange("Bookmarks") },
        onBlur = { onLabelChange("") },
        backgroundColor = bgSurface
    )

    // Downloads
    CommandButton(
        icon = Icons.Default.Info,
        label = "Downloads",
        onClick = onDownloads,
        onFocus = { onLabelChange("Downloads") },
        onBlur = { onLabelChange("") },
        backgroundColor = bgSurface
    )

    // History
    CommandButton(
        icon = Icons.Default.DateRange,
        label = "History",
        onClick = onHistory,
        onFocus = { onLabelChange("History") },
        onBlur = { onLabelChange("") },
        backgroundColor = bgSurface
    )

    // Settings
    CommandButton(
        icon = Icons.Default.Settings,
        label = "Settings",
        onClick = onSettings,
        onFocus = { onLabelChange("Settings") },
        onBlur = { onLabelChange("") },
        backgroundColor = bgSurface
    )
}

@Composable
private fun CommandButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    onFocus: () -> Unit,
    onBlur: () -> Unit,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(backgroundColor)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color(0xFFE8E8E8),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun VoiceButton(
    isListening: Boolean,
    onClick: () -> Unit,
    onFocus: () -> Unit,
    onBlur: () -> Unit,
    activeColor: Color,
    backgroundColor: Color,
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
                imageVector = Icons.Default.Phone,
                contentDescription = if (isListening) "Stop listening" else "Voice input",
                tint = if (isListening) Color.White else Color(0xFFE8E8E8),
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
    activeColor: Color,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(36.dp),
        contentAlignment = Alignment.Center
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (isDesktopMode) activeColor else backgroundColor)
        ) {
            Icon(
                imageVector = Icons.Default.Phone,
                contentDescription = if (isDesktopMode) "Exit Advanced Mode" else "Enter Advanced Mode",
                tint = if (isDesktopMode) Color.White else Color(0xFFE8E8E8),
                modifier = Modifier.size(18.dp)
            )
        }

        // Indicator dot when active
        if (isDesktopMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color.White)
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
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFFE8E8E8)),
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
        "go to [url]" to "Navigate to URL"
    )

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Surface(
            color = Color(0xFF0F3460).copy(alpha = 0.95f),
            shape = RoundedCornerShape(12.dp),
            shadowElevation = 8.dp,
            modifier = modifier
                .width(260.dp)
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Voice Commands",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color(0xFFE8E8E8)
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFFA0A0A0),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFF2D4A6F))

                commands.forEach { (command, description) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF16213E), RoundedCornerShape(6.dp))
                            .padding(6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = Color(0xFF60A5FA),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = command,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFA0A0A0)
                        )
                    }
                }
            }
        }
    }
}
