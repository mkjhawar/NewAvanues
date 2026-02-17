package com.augmentalis.webavanue

import android.annotation.SuppressLint
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.avanueui.tokens.ShapeTokens
import com.augmentalis.avanueui.tokens.SpacingTokens
import com.augmentalis.webavanue.IconVariant
import com.augmentalis.webavanue.toColor
import com.augmentalis.webavanue.CompactTabCounterBadge
import com.augmentalis.webavanue.TabUiState
import com.augmentalis.webavanue.Favorite

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
@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun AddressBar(
    url: String,
    canGoBack: Boolean = false,
    canGoForward: Boolean = false,
    isDesktopMode: Boolean = false,
    isReadingMode: Boolean = false,            // Phase 4: Reading Mode
    isArticleAvailable: Boolean = false,        // Phase 4: Reading Mode
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
    onReadingModeToggle: () -> Unit = {},       // Phase 4: Reading Mode
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
    onExitBrowser: (() -> Unit)? = null,
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
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = AvanueTheme.colors.surfaceElevated,
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isPortrait) {
                // Portrait mode: Two-level layout for better URL visibility
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 12.dp,
                            vertical = SpacingTokens.xs
                        )
                ) {
                    // Top Row: URL input field - takes full width
                    // Long-press selects entire URL for easy editing
                    @OptIn(ExperimentalFoundationApi::class)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .combinedClickable(
                                onClick = { focusRequester.requestFocus() },
                                onLongClick = {
                                    focusRequester.requestFocus()
                                    if (textFieldValue.text.isNotEmpty()) {
                                        textFieldValue = textFieldValue.copy(
                                            selection = TextRange(0, textFieldValue.text.length)
                                        )
                                    }
                                }
                            ),
                        color = AvanueTheme.colors.surfaceInput,
                        shape = RoundedCornerShape(ShapeTokens.sm)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = SpacingTokens.sm),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(SpacingTokens.xs)
                        ) {
                            // Bookmark button
                            IconButton(
                                onClick = onAddFavorite,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = if (isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                    contentDescription = if (isFavorite) "Remove bookmark (Voice: remove bookmark)" else "Add bookmark (Voice: add bookmark)",
                                    tint = if (isFavorite) IconVariant.Warning.toColor() else IconVariant.Secondary.toColor(),
                                    modifier = Modifier.size(20.dp)
                                )
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
                                    color = AvanueTheme.colors.textPrimary
                                ),
                                cursorBrush = SolidColor(AvanueTheme.colors.primary),
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
                                            color = AvanueTheme.colors.textSecondary
                                        )
                                    }
                                    innerTextField()
                                }
                            )

                            // Settings button - one-tap access to settings
                            IconButton(
                                onClick = onSettingsClick,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Settings (Voice: open settings)",
                                    tint = IconVariant.Secondary.toColor(),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // Bottom Row: Navigation controls with text labels
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = SpacingTokens.xs),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        LabeledNavButton(
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            label = "Back",
                            onClick = onBack,
                            enabled = canGoBack,
                            tint = if (canGoBack) IconVariant.Primary.toColor() else IconVariant.Disabled.toColor(),
                            voiceHint = "go back"
                        )

                        LabeledNavButton(
                            icon = Icons.AutoMirrored.Filled.ArrowForward,
                            label = "Fwd",
                            onClick = onForward,
                            enabled = canGoForward,
                            tint = if (canGoForward) IconVariant.Primary.toColor() else IconVariant.Disabled.toColor(),
                            voiceHint = "go forward"
                        )

                        LabeledNavButton(
                            icon = Icons.Default.Refresh,
                            label = "Reload",
                            onClick = onRefresh,
                            tint = IconVariant.Primary.toColor(),
                            voiceHint = "refresh"
                        )

                        LabeledNavButton(
                            icon = Icons.Default.History,
                            label = "History",
                            onClick = onHistoryClick,
                            tint = IconVariant.Primary.toColor(),
                            voiceHint = "open history"
                        )

                        if (isArticleAvailable) {
                            LabeledNavButton(
                                icon = Icons.AutoMirrored.Filled.MenuBook,
                                label = "Read",
                                onClick = onReadingModeToggle,
                                tint = if (isReadingMode) IconVariant.Primary.toColor() else IconVariant.Secondary.toColor(),
                                voiceHint = "reading mode"
                            )
                        }

                        // Tab counter badge
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CompactTabCounterBadge(
                                tabCount = tabCount,
                                onClick = onTabSwitcherClick,
                                modifier = Modifier.size(28.dp)
                            )
                            Text(
                                text = "Tabs",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 8.sp,
                                color = AvanueTheme.colors.textSecondary,
                                textAlign = TextAlign.Center
                            )
                        }

                        LabeledNavButton(
                            icon = Icons.Default.GraphicEq,
                            label = "Voice",
                            onClick = onCommandBarToggle,
                            tint = IconVariant.Primary.toColor(),
                            voiceHint = "show command bar"
                        )

                        LabeledNavButton(
                            icon = Icons.Default.Mic,
                            label = "Mic",
                            onClick = { if (!isListening) onStartListening() },
                            tint = if (isListening) IconVariant.Success.toColor() else IconVariant.Primary.toColor(),
                            voiceHint = "start listening"
                        )
                    }
                }
            } else {
                // Landscape mode: Original single-row layout
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(
                            horizontal = 12.dp,
                            vertical = SpacingTokens.sm
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(SpacingTokens.sm)
                ) {
                    // Back button
                    IconButton(
                        onClick = onBack,
                        enabled = canGoBack,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back (Voice: go back)",
                            tint = if (canGoBack) IconVariant.Primary.toColor() else IconVariant.Disabled.toColor(),
                            modifier = Modifier
                        )
                    }

                    // Forward button
                    IconButton(
                        onClick = onForward,
                        enabled = canGoForward,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Forward (Voice: go forward)",
                            tint = if (canGoForward) IconVariant.Primary.toColor() else IconVariant.Disabled.toColor(),
                            modifier = Modifier
                        )
                    }

                    // Refresh button - moved next to navigation arrows (left of URL)
                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier.size(36.dp),
                        enabled = true
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh (Voice: refresh)",
                            tint = IconVariant.Primary.toColor(),
                            modifier = Modifier
                        )
                    }

                    // URL input field - takes remaining space
                    // Long-press selects entire URL for easy editing
                    @OptIn(ExperimentalFoundationApi::class)
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .combinedClickable(
                                onClick = { focusRequester.requestFocus() },
                                onLongClick = {
                                    focusRequester.requestFocus()
                                    if (textFieldValue.text.isNotEmpty()) {
                                        textFieldValue = textFieldValue.copy(
                                            selection = TextRange(0, textFieldValue.text.length)
                                        )
                                    }
                                }
                            ),
                        color = AvanueTheme.colors.surfaceInput,
                        shape = RoundedCornerShape(ShapeTokens.sm)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = SpacingTokens.sm),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(SpacingTokens.sm)
                        ) {
                            // Bookmark button - tap to add/remove from favorites
                            IconButton(
                                onClick = onAddFavorite,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                    contentDescription = if (isFavorite) "Remove bookmark (Voice: remove bookmark)" else "Add bookmark (Voice: add bookmark)",
                                    tint = if (isFavorite) IconVariant.Warning.toColor() else IconVariant.Secondary.toColor(),
                                    modifier = Modifier.size(24.dp)
                                )
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
                                    color = AvanueTheme.colors.textPrimary
                                ),
                                cursorBrush = SolidColor(AvanueTheme.colors.primary),
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
                                            color = AvanueTheme.colors.textSecondary
                                        )
                                    }
                                    innerTextField()
                                }
                            )

                            // History button - tap to open history
                            IconButton(
                                onClick = onHistoryClick,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = "History (Voice: open history)",
                                    tint = IconVariant.Secondary.toColor(),
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            // Settings button - one-tap access to settings
                            IconButton(
                                onClick = onSettingsClick,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Settings (Voice: open settings)",
                                    tint = IconVariant.Secondary.toColor(),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    // Tab counter badge (Chrome-like) - Opens full TabSwitcherView
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CompactTabCounterBadge(
                            tabCount = tabCount,
                            onClick = onTabSwitcherClick,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "Tabs",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 8.sp,
                            color = AvanueTheme.colors.textSecondary,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Command bar toggle button - shows/hides bottom command bar
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = onCommandBarToggle,
                            modifier = Modifier.size(36.dp),
                            enabled = true
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = if (isCommandBarVisible) "Hide command bar (Voice: hide command bar)" else "Show command bar (Voice: show command bar)",
                                tint = IconVariant.Primary.toColor(),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Text(
                            text = "Voice",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 8.sp,
                            color = AvanueTheme.colors.textSecondary,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Voice/Mic button - tap to start listening
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = { if (!isListening) onStartListening() },
                            modifier = Modifier.size(36.dp),
                            enabled = true
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = if (isListening) "Listening (Voice: stop listening)" else "Microphone (Voice: start listening)",
                                tint = if (isListening) IconVariant.Success.toColor() else IconVariant.Primary.toColor(),
                                modifier = Modifier
                            )
                        }
                        Text(
                            text = "Mic",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 8.sp,
                            color = AvanueTheme.colors.textSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

/**
 * Compact labeled icon button for the address bar navigation row.
 * Shows an icon with a tiny text label below.
 *
 * @param icon Icon to display
 * @param label Visible text label below the icon
 * @param onClick Click handler
 * @param enabled Whether the button is enabled
 * @param tint Icon tint color
 * @param voiceHint Explicit voice command phrase. When provided, contentDescription
 *   becomes "$label (Voice: $voiceHint)" for the universal voice hint convention.
 *   Any Android accessibility framework (Compose, Flutter, RN, Unity) can use this.
 */
@Composable
private fun LabeledNavButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    tint: Color = IconVariant.Primary.toColor(),
    voiceHint: String? = null
) {
    val description = if (voiceHint != null) "$label (Voice: $voiceHint)" else label
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(enabled = enabled, onClick = onClick)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 8.sp,
            color = if (enabled) AvanueTheme.colors.textSecondary else AvanueTheme.colors.textDisabled,
            textAlign = TextAlign.Center
        )
    }
}
