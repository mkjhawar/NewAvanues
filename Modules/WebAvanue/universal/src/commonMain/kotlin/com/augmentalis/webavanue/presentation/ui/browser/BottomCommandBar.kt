@file:Suppress("DEPRECATION_ERROR")
package com.augmentalis.webavanue.ui.screen.browser

/**
 * DEPRECATED: This file is maintained for backward compatibility.
 * All command bar components have been refactored into the commandbar subpackage.
 *
 * New imports should use:
 * - com.augmentalis.webavanue.ui.screen.browser.commandbar.BottomCommandBar
 * - com.augmentalis.webavanue.ui.screen.browser.commandbar.CommandBarWrapper
 * - com.augmentalis.webavanue.ui.screen.browser.commandbar.VerticalCommandBarLayout
 * - com.augmentalis.webavanue.ui.screen.browser.commandbar.HorizontalCommandBarLayout
 * - com.augmentalis.webavanue.ui.screen.browser.commandbar.CommandBarLevel
 *
 * This file re-exports components for backward compatibility.
 */

// Re-export from refactored package
import com.augmentalis.webavanue.ui.screen.browser.commandbar.CommandBarLevel as RefactoredCommandBarLevel
import com.augmentalis.webavanue.ui.screen.browser.commandbar.BottomCommandBar as RefactoredBottomCommandBar
import com.augmentalis.webavanue.ui.screen.browser.commandbar.CommandBarWrapper as RefactoredCommandBarWrapper
import com.augmentalis.webavanue.ui.screen.browser.commandbar.VerticalCommandBarLayout as RefactoredVerticalCommandBarLayout
import com.augmentalis.webavanue.ui.screen.browser.commandbar.HorizontalCommandBarLayout as RefactoredHorizontalCommandBarLayout
import com.augmentalis.webavanue.ui.screen.browser.commandbar.TextCommandInput as RefactoredTextCommandInput
import com.augmentalis.webavanue.ui.screen.browser.commandbar.ListeningIndicator as RefactoredListeningIndicator
import com.augmentalis.webavanue.ui.screen.browser.commandbar.VoiceCommandsPanel as RefactoredVoiceCommandsPanel

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * @deprecated Use com.augmentalis.webavanue.ui.screen.browser.commandbar.CommandBarLevel
 */
@Deprecated(
    message = "Use CommandBarLevel from commandbar package",
    replaceWith = ReplaceWith("CommandBarLevel", "com.augmentalis.webavanue.ui.screen.browser.commandbar.CommandBarLevel")
)
typealias CommandBarLevel = RefactoredCommandBarLevel

/**
 * @deprecated Use com.augmentalis.webavanue.ui.screen.browser.commandbar.CommandBarWrapper
 */
@Deprecated(
    message = "Use CommandBarWrapper from commandbar package",
    replaceWith = ReplaceWith("CommandBarWrapper", "com.augmentalis.webavanue.ui.screen.browser.commandbar.CommandBarWrapper")
)
@Composable
fun CommandBarWrapper(
    isVisible: Boolean,
    isLandscape: Boolean,
    onToggleVisibility: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) = RefactoredCommandBarWrapper(isVisible, isLandscape, onToggleVisibility, modifier, content)

/**
 * @deprecated Use com.augmentalis.webavanue.ui.screen.browser.commandbar.BottomCommandBar
 */
@Deprecated(
    message = "Use BottomCommandBar from commandbar package",
    replaceWith = ReplaceWith("BottomCommandBar", "com.augmentalis.webavanue.ui.screen.browser.commandbar.BottomCommandBar")
)
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
) = RefactoredBottomCommandBar(
    onBack = onBack,
    onForward = onForward,
    onHome = onHome,
    onRefresh = onRefresh,
    onScrollUp = onScrollUp,
    onScrollDown = onScrollDown,
    onScrollTop = onScrollTop,
    onScrollBottom = onScrollBottom,
    onBookmarks = onBookmarks,
    onDownloads = onDownloads,
    onHistory = onHistory,
    onSettings = onSettings,
    onDesktopModeToggle = onDesktopModeToggle,
    onZoomIn = onZoomIn,
    onZoomOut = onZoomOut,
    onZoomLevel = onZoomLevel,
    onFreezePage = onFreezePage,
    onFavorite = onFavorite,
    onNewTab = onNewTab,
    onShowTabs = onShowTabs,
    onShowFavorites = onShowFavorites,
    onDismissBar = onDismissBar,
    tabCount = tabCount,
    isListening = isListening,
    isDesktopMode = isDesktopMode,
    isScrollFrozen = isScrollFrozen,
    isLandscape = isLandscape,
    isHeadlessMode = isHeadlessMode,
    onToggleHeadlessMode = onToggleHeadlessMode,
    modifier = modifier
)

/**
 * @deprecated Use com.augmentalis.webavanue.ui.screen.browser.commandbar.VerticalCommandBarLayout
 */
@Deprecated(
    message = "Use VerticalCommandBarLayout from commandbar package",
    replaceWith = ReplaceWith("VerticalCommandBarLayout", "com.augmentalis.webavanue.ui.screen.browser.commandbar.VerticalCommandBarLayout")
)
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
) = RefactoredVerticalCommandBarLayout(
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
    onNewTab = onNewTab,
    onHide = onHide,
    onSwitchSide = onSwitchSide,
    modifier = modifier
)

/**
 * @deprecated Use com.augmentalis.webavanue.ui.screen.browser.commandbar.HorizontalCommandBarLayout
 */
@Deprecated(
    message = "Use HorizontalCommandBarLayout from commandbar package",
    replaceWith = ReplaceWith("HorizontalCommandBarLayout", "com.augmentalis.webavanue.ui.screen.browser.commandbar.HorizontalCommandBarLayout")
)
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
) = RefactoredHorizontalCommandBarLayout(
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
    onBookmarks = onBookmarks,
    onDownloads = onDownloads,
    onHistory = onHistory,
    onSettings = onSettings,
    onNewTab = onNewTab,
    onShowTabs = onShowTabs,
    onShowFavorites = onShowFavorites,
    onHide = onHide,
    isHeadlessMode = isHeadlessMode,
    onToggleHeadlessMode = onToggleHeadlessMode,
    modifier = modifier
)

/**
 * @deprecated Use com.augmentalis.webavanue.ui.screen.browser.commandbar.TextCommandInput
 */
@Deprecated(
    message = "Use TextCommandInput from commandbar package",
    replaceWith = ReplaceWith("TextCommandInput", "com.augmentalis.webavanue.ui.screen.browser.commandbar.TextCommandInput")
)
@Composable
fun TextCommandInput(
    visible: Boolean,
    onCommand: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) = RefactoredTextCommandInput(visible, onCommand, onDismiss, modifier)

/**
 * @deprecated Use com.augmentalis.webavanue.ui.screen.browser.commandbar.ListeningIndicator
 */
@Deprecated(
    message = "Use ListeningIndicator from commandbar package",
    replaceWith = ReplaceWith("ListeningIndicator", "com.augmentalis.webavanue.ui.screen.browser.commandbar.ListeningIndicator")
)
@Composable
fun ListeningIndicator(
    visible: Boolean,
    modifier: Modifier = Modifier
) = RefactoredListeningIndicator(visible, modifier)

/**
 * @deprecated Use com.augmentalis.webavanue.ui.screen.browser.commandbar.VoiceCommandsPanel
 */
@Deprecated(
    message = "Use VoiceCommandsPanel from commandbar package",
    replaceWith = ReplaceWith("VoiceCommandsPanel", "com.augmentalis.webavanue.ui.screen.browser.commandbar.VoiceCommandsPanel")
)
@Composable
fun VoiceCommandsPanel(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) = RefactoredVoiceCommandsPanel(visible, onDismiss, modifier)
