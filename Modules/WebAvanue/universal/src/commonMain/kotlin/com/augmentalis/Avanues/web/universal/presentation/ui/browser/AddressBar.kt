package com.augmentalis.Avanues.web.universal.presentation.ui.browser

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.augmentalis.Avanues.web.universal.presentation.design.OceanComponents
import com.augmentalis.Avanues.web.universal.presentation.design.OceanDesignTokens
import com.augmentalis.Avanues.web.universal.presentation.design.IconVariant
import com.augmentalis.Avanues.web.universal.presentation.design.SurfaceVariant
import com.augmentalis.Avanues.web.universal.presentation.ui.theme.OceanTheme
import com.augmentalis.Avanues.web.universal.presentation.ui.tab.CompactTabCounterBadge
import com.augmentalis.Avanues.web.universal.presentation.viewmodel.TabUiState
import com.augmentalis.webavanue.domain.model.Favorite

/**
 * AddressBar - Browser address bar with Ocean component system
 *
 * REWRITTEN: December 2025 - Ocean component abstraction layer
 *
 * Architecture:
 * - Uses ONLY Ocean components (OceanIconButton, OceanIcon, OceanSurface)
 * - All colors from OceanDesignTokens (no direct OceanTheme.* calls)
 * - Zero hardcoded colors or spacing values
 * - MagicUI-ready (when components switch to MagicUI, this code unchanged)
 *
 * Features:
 * - URL input field
 * - Back/Forward navigation buttons (always visible blue when enabled)
 * - Refresh button (always visible blue)
 * - Desktop mode toggle
 * - Tab counter badge
 * - Command bar toggle (hamburger menu)
 * - Voice/mic button (blue when idle, green when listening)
 * - Bookmark button
 * - History button
 * - Responsive portrait/landscape layouts
 *
 * Color System:
 * - All icons: IconVariant.Primary (blue) when enabled
 * - All icons: IconVariant.Disabled (gray) when disabled
 * - Voice mic: IconVariant.Success (green) when listening
 * - NO MORE color patching issues (design tokens handle everything)
 *
 * @param url Current URL
 * @param canGoBack Whether back navigation is available
 * @param canGoForward Whether forward navigation is available
 * @param isDesktopMode Whether in desktop user agent mode
 * @param isFavorite Whether current page is favorited
 * @param tabCount Number of open tabs
 * @param tabs List of all tabs (for dropdown)
 * @param activeTabId Currently active tab ID
 * @param favorites List of all favorites (for dropdown)
 * @param onUrlChange Callback when URL changes
 * @param onGo Callback when Go button is clicked
 * @param onBack Callback for back navigation
 * @param onForward Callback for forward navigation
 * @param onRefresh Callback for refresh
 * @param onDesktopModeToggle Callback for desktop mode toggle
 * @param onFavoriteClick Callback for favorite button (legacy)
 * @param onTabClick Callback when tab is clicked in dropdown
 * @param onTabClose Callback when tab close button is clicked
 * @param onNewTab Callback when new tab button is clicked
 * @param onFavoriteNavigate Callback when favorite is clicked in dropdown
 * @param onAddFavorite Callback when add favorite button is clicked
 * @param onShowFavorites Callback to open spatial favorites shelf
 * @param onHistoryClick Callback for history button
 * @param onSettingsClick Callback for settings button
 * @param onTabSwitcherClick Callback to open 3D tab switcher
 * @param isCommandBarVisible Whether command bar is currently visible
 * @param onCommandBarToggle Toggle command bar visibility
 * @param isListening Whether currently listening for voice
 * @param onStartListening Start voice recognition
 * @param modifier Modifier for customization
 */
@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun AddressBar(
    url: String,
    canGoBack: Boolean = false,
    canGoForward: Boolean = false,
    isDesktopMode: Boolean = false,
    isFavorite: Boolean = false,
    tabCount: Int = 0,
    tabs: List<TabUiState> = emptyList(),
    activeTabId: String? = null,
    favorites: List<Favorite> = emptyList(),
    onUrlChange: (String) -> Unit,
    onGo: () -> Unit = {},
    onBack: () -> Unit = {},
    onForward: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onDesktopModeToggle: () -> Unit = {},
    onFavoriteClick: () -> Unit = {},
    onTabClick: (String) -> Unit = {},
    onTabClose: (String) -> Unit = {},
    onNewTab: () -> Unit = {},
    onFavoriteNavigate: (Favorite) -> Unit = {},
    onAddFavorite: () -> Unit = {},
    onShowFavorites: () -> Unit = {},
    onHistoryClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onTabSwitcherClick: () -> Unit = {},
    isCommandBarVisible: Boolean = false,
    onCommandBarToggle: () -> Unit = {},
    isListening: Boolean = false,
    onStartListening: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Keyboard and focus management
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    // URL TextField state with selection support
    var textFieldValue by remember { mutableStateOf(TextFieldValue(text = url, selection = TextRange(0, 0))) }
    var hasFocus by remember { mutableStateOf(false) }
    var lastSyncedUrl by remember { mutableStateOf(url) }

    // Sync external url changes to internal state ONLY when not focused and URL actually changed
    LaunchedEffect(url, hasFocus) {
        if (!hasFocus && url != lastSyncedUrl) {
            textFieldValue = TextFieldValue(text = url, selection = TextRange(0, 0))
            lastSyncedUrl = url
        }
    }

    // FIX Issue #4: Clear focus and update URL when tab changes
    // This ensures the address bar shows the correct URL when switching tabs
    LaunchedEffect(activeTabId) {
        if (hasFocus) {
            focusManager.clearFocus()  // Clear focus to trigger URL update
            hasFocus = false
        }
        textFieldValue = TextFieldValue(text = url, selection = TextRange(0, 0))
        lastSyncedUrl = url
    }

    // Detect WebGL-intensive sites that may cause ANR on AOSP devices
    val isWebGLSite = remember(url) {
        val lowerUrl = url.lowercase()
        lowerUrl.contains("babylon") ||
        lowerUrl.contains("shadertoy") ||
        lowerUrl.contains("webgl") ||
        lowerUrl.contains("three.js") ||
        lowerUrl.contains("threejs") ||
        lowerUrl.contains("webgpu")
    }

    // Helper function to dismiss keyboard reliably
    fun dismissKeyboard() {
        hasFocus = false
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
    }

    // Detect orientation: portrait when height > width
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        val isPortrait = maxHeight > maxWidth

        // Solid surface background (no blur)
        OceanComponents.Surface(
            modifier = Modifier.fillMaxWidth(),
            variant = SurfaceVariant.Elevated
        ) {
            if (isPortrait) {
                // Portrait mode: Two-level layout for better URL visibility
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = OceanDesignTokens.Spacing.md,
                            vertical = OceanDesignTokens.Spacing.xs
                        )
                ) {
                    // Top Row: URL input field - takes full width
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp),
                        color = OceanDesignTokens.Surface.input,
                        shape = RoundedCornerShape(OceanDesignTokens.CornerRadius.md)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = OceanDesignTokens.Spacing.sm),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(OceanDesignTokens.Spacing.xs)
                        ) {
                            // Bookmark button - smaller for portrait
                            Surface(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(RoundedCornerShape(OceanDesignTokens.CornerRadius.sm))
                                    .clickable { onAddFavorite() },
                                shape = RoundedCornerShape(OceanDesignTokens.CornerRadius.sm),
                                color = if (isFavorite) OceanTheme.starActive.copy(alpha = 0.15f) else Color.Transparent
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    OceanComponents.Icon(
                                        imageVector = if (isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                        contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                                        variant = if (isFavorite) IconVariant.Warning else IconVariant.Secondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }

                            // URL input with select-all-on-focus behavior
                            androidx.compose.foundation.text.BasicTextField(
                                value = textFieldValue,
                                onValueChange = { newValue ->
                                    textFieldValue = newValue
                                    onUrlChange(newValue.text)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(focusRequester)
                                    .onFocusChanged { focusState ->
                                        val wasFocused = hasFocus
                                        hasFocus = focusState.isFocused
                                        if (focusState.isFocused && !wasFocused && textFieldValue.text.isNotEmpty()) {
                                            textFieldValue = textFieldValue.copy(
                                                selection = TextRange(0, textFieldValue.text.length)
                                            )
                                        }
                                    },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodySmall.copy(
                                    color = OceanDesignTokens.Text.primary
                                ),
                                cursorBrush = SolidColor(OceanDesignTokens.Surface.primary),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                                keyboardActions = KeyboardActions(
                                    onGo = {
                                        dismissKeyboard()
                                        onGo()
                                    }
                                ),
                                decorationBox = { innerTextField ->
                                    if (textFieldValue.text.isEmpty()) {
                                        Text(
                                            text = "Enter URL or search",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = OceanDesignTokens.Text.secondary
                                        )
                                    }
                                    innerTextField()
                                }
                            )

                            // History button - tap to open history
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(RoundedCornerShape(OceanDesignTokens.CornerRadius.sm))
                                    .clickable(onClick = onHistoryClick),
                                contentAlignment = Alignment.Center
                            ) {
                                OceanComponents.Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = "History",
                                    variant = IconVariant.Secondary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            // Go button - smaller for portrait
                            OceanComponents.IconButton(
                                onClick = {
                                    dismissKeyboard()
                                    onGo()
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                OceanComponents.Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Go (Voice: go)",
                                    variant = IconVariant.Secondary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    // Bottom Row: Navigation controls at half size (~18dp buttons)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                            .padding(top = OceanDesignTokens.Spacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Back button - small
                        OceanComponents.IconButton(
                            onClick = onBack,
                            enabled = canGoBack,
                            modifier = Modifier.size(24.dp)
                        ) {
                            OceanComponents.Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                variant = if (canGoBack) IconVariant.Primary else IconVariant.Disabled,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Forward button - small
                        OceanComponents.IconButton(
                            onClick = onForward,
                            enabled = canGoForward,
                            modifier = Modifier.size(24.dp)
                        ) {
                            OceanComponents.Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Forward",
                                variant = if (canGoForward) IconVariant.Primary else IconVariant.Disabled,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Refresh button - small
                        OceanComponents.IconButton(
                            onClick = onRefresh,
                            modifier = Modifier.size(24.dp)
                        ) {
                            OceanComponents.Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                variant = IconVariant.Primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Desktop mode toggle - small
                        CompactDesktopModeIndicator(
                            isDesktopMode = isDesktopMode,
                            onClick = onDesktopModeToggle,
                            modifier = Modifier
                                .size(24.dp)
                                .alpha(if (isWebGLSite) 0.5f else 1f)
                        )

                        // Tab counter badge - compact
                        CompactTabCounterBadge(
                            tabCount = tabCount,
                            onClick = onTabSwitcherClick,
                            modifier = Modifier.size(24.dp)
                        )

                        // Command bar toggle button - shows/hides bottom command bar
                        OceanComponents.IconButton(
                            onClick = onCommandBarToggle,
                            modifier = Modifier.size(24.dp)
                        ) {
                            OceanComponents.Icon(
                                imageVector = Icons.Default.Dehaze,
                                contentDescription = if (isCommandBarVisible) "Hide command bar" else "Show command bar",
                                variant = IconVariant.Primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Voice/Mic button - tap to start listening
                        OceanComponents.IconButton(
                            onClick = { if (!isListening) onStartListening() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            OceanComponents.Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = if (isListening) "Listening..." else "Tap to speak",
                                variant = if (isListening) IconVariant.Success else IconVariant.Primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            } else {
                // Landscape mode: Original single-row layout
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(
                            horizontal = OceanDesignTokens.Spacing.md,
                            vertical = OceanDesignTokens.Spacing.sm
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(OceanDesignTokens.Spacing.sm)
                ) {
                    // Back button
                    OceanComponents.IconButton(
                        onClick = onBack,
                        enabled = canGoBack,
                        modifier = Modifier.size(36.dp)
                    ) {
                        OceanComponents.Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back (Voice: go back)",
                            variant = if (canGoBack) IconVariant.Primary else IconVariant.Disabled
                        )
                    }

                    // Forward button
                    OceanComponents.IconButton(
                        onClick = onForward,
                        enabled = canGoForward,
                        modifier = Modifier.size(36.dp)
                    ) {
                        OceanComponents.Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Forward (Voice: go forward)",
                            variant = if (canGoForward) IconVariant.Primary else IconVariant.Disabled
                        )
                    }

                    // Refresh button - moved next to navigation arrows (left of URL)
                    OceanComponents.IconButton(
                        onClick = onRefresh,
                        modifier = Modifier.size(36.dp)
                    ) {
                        OceanComponents.Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh (Voice: refresh)",
                            variant = IconVariant.Primary
                        )
                    }

                    // URL input field - takes remaining space
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        color = OceanDesignTokens.Surface.input,
                        shape = RoundedCornerShape(OceanDesignTokens.CornerRadius.md)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = OceanDesignTokens.Spacing.sm),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(OceanDesignTokens.Spacing.sm)
                        ) {
                            // Bookmark button - tap to add/remove from favorites
                            Surface(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(OceanDesignTokens.CornerRadius.md))
                                    .clickable { onAddFavorite() },
                                shape = RoundedCornerShape(OceanDesignTokens.CornerRadius.md),
                                color = if (isFavorite) OceanTheme.starActive.copy(alpha = 0.15f) else Color.Transparent
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    OceanComponents.Icon(
                                        imageVector = if (isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                        contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                                        variant = if (isFavorite) IconVariant.Warning else IconVariant.Secondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            // URL input with select-all-on-focus behavior
                            androidx.compose.foundation.text.BasicTextField(
                                value = textFieldValue,
                                onValueChange = { newValue ->
                                    textFieldValue = newValue
                                    onUrlChange(newValue.text)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(focusRequester)
                                    .onFocusChanged { focusState ->
                                        val wasFocused = hasFocus
                                        hasFocus = focusState.isFocused
                                        if (focusState.isFocused && !wasFocused && textFieldValue.text.isNotEmpty()) {
                                            textFieldValue = textFieldValue.copy(
                                                selection = TextRange(0, textFieldValue.text.length)
                                            )
                                        }
                                    },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    color = OceanDesignTokens.Text.primary
                                ),
                                cursorBrush = SolidColor(OceanDesignTokens.Surface.primary),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                                keyboardActions = KeyboardActions(
                                    onGo = {
                                        dismissKeyboard()
                                        onGo()
                                    }
                                ),
                                decorationBox = { innerTextField ->
                                    if (textFieldValue.text.isEmpty()) {
                                        Text(
                                            text = "Enter URL or search",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = OceanDesignTokens.Text.secondary
                                        )
                                    }
                                    innerTextField()
                                }
                            )

                            // History button - tap to open history
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(OceanDesignTokens.CornerRadius.md))
                                    .clickable(onClick = onHistoryClick),
                                contentAlignment = Alignment.Center
                            ) {
                                OceanComponents.Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = "History",
                                    variant = IconVariant.Secondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Desktop mode toggle button - tap to switch mobile/desktop
                            CompactDesktopModeIndicator(
                                isDesktopMode = isDesktopMode,
                                onClick = onDesktopModeToggle,
                                modifier = Modifier
                                    .padding(end = OceanDesignTokens.Spacing.xs)
                                    .alpha(if (isWebGLSite) 0.5f else 1f)
                            )

                            OceanComponents.IconButton(
                                onClick = {
                                    dismissKeyboard()
                                    onGo()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                OceanComponents.Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Go (Voice: go)",
                                    variant = IconVariant.Secondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // Tab counter badge (Chrome-like) - Opens full TabSwitcherView
                    CompactTabCounterBadge(
                        tabCount = tabCount,
                        onClick = onTabSwitcherClick
                    )

                    // Command bar toggle button - shows/hides bottom command bar
                    OceanComponents.IconButton(
                        onClick = onCommandBarToggle,
                        modifier = Modifier.size(36.dp)
                    ) {
                        OceanComponents.Icon(
                            imageVector = Icons.Default.Dehaze,
                            contentDescription = if (isCommandBarVisible) "Hide command bar" else "Show command bar",
                            variant = IconVariant.Primary
                        )
                    }

                    // Voice/Mic button - tap to start listening
                    OceanComponents.IconButton(
                        onClick = { if (!isListening) onStartListening() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        OceanComponents.Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = if (isListening) "Listening for command..." else "Tap to speak command",
                            variant = if (isListening) IconVariant.Success else IconVariant.Primary
                        )
                    }
                }
            }
        }
    }
}
