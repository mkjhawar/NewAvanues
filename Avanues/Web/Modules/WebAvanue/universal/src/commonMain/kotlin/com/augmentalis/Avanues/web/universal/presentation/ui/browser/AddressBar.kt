package com.augmentalis.Avanues.web.universal.presentation.ui.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentHeight
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

/**
 * AddressBar - Browser address bar with navigation and menu buttons
 *
 * Features:
 * - URL input field
 * - Back/Forward navigation buttons
 * - Refresh button
 * - Bookmark/Downloads/History/Settings menu buttons
 * - Go button to navigate
 *
 * @param url Current URL
 * @param canGoBack Whether back navigation is available
 * @param canGoForward Whether forward navigation is available
 * @param onUrlChange Callback when URL changes
 * @param onGo Callback when Go button is clicked
 * @param onBack Callback for back navigation
 * @param onForward Callback for forward navigation
 * @param onRefresh Callback for refresh
 * @param onBookmarkClick Callback for bookmarks button
 * @param onDownloadClick Callback for downloads button
 * @param onHistoryClick Callback for history button
 * @param onSettingsClick Callback for settings button
 * @param modifier Modifier for customization
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AddressBar(
    url: String,
    canGoBack: Boolean = false,
    canGoForward: Boolean = false,
    isDesktopMode: Boolean = false,
    isFavorite: Boolean = false,
    onUrlChange: (String) -> Unit,
    onGo: () -> Unit = {},
    onBack: () -> Unit = {},
    onForward: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onDesktopModeToggle: () -> Unit = {},
    onFavoriteClick: () -> Unit = {},
    onBookmarkClick: () -> Unit = {},
    onDownloadClick: () -> Unit = {},
    onHistoryClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // FIX Issue #9: Use both keyboard controller and focus manager for reliable keyboard dismissal
    // LocalSoftwareKeyboardController can be null in some contexts, so we use focus manager as backup
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    // Helper function to dismiss keyboard reliably
    fun dismissKeyboard() {
        keyboardController?.hide()
        focusManager.clearFocus()  // This also dismisses keyboard by clearing focus
    }

    // Dark 3D theme colors
    val bgSecondary = Color(0xFF16213E)
    val bgSurface = Color(0xFF0F3460)
    val textPrimary = Color(0xFFE8E8E8)
    val textSecondary = Color(0xFFA0A0A0)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        color = bgSecondary,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                    tint = if (canGoBack) {
                        textPrimary
                    } else {
                        textSecondary.copy(alpha = 0.3f)
                    }
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
                    tint = if (canGoForward) {
                        textPrimary
                    } else {
                        textSecondary.copy(alpha = 0.3f)
                    }
                )
            }

            // URL input field - takes remaining space
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp),
                color = bgSurface,
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = url,
                        onValueChange = onUrlChange,
                        onGo = {
                            dismissKeyboard()  // FIX Issue #9: Use reliable keyboard dismissal
                            onGo()
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = textPrimary
                        ),
                        decorationBox = { innerTextField ->
                            if (url.isEmpty()) {
                                Text(
                                    text = "Enter URL or say 'go to [website]'",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = textSecondary
                                )
                            }
                            innerTextField()
                        }
                    )

                    // Favorite star icon
                    IconButton(
                        onClick = onFavoriteClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.Star,
                            contentDescription = if (isFavorite) "Remove from Favorites" else "Add to Favorites",
                            tint = if (isFavorite) Color(0xFFFFC107) else textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Desktop mode indicator
                    CompactDesktopModeIndicator(
                        isDesktopMode = isDesktopMode,
                        modifier = Modifier.padding(end = 4.dp)
                    )

                    IconButton(
                        onClick = {
                            dismissKeyboard()  // FIX Issue #9: Use reliable keyboard dismissal
                            onGo()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Go (Voice: go)",
                            tint = textPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Refresh button
            IconButton(
                onClick = onRefresh,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh (Voice: refresh)",
                    tint = textPrimary
                )
            }
        }
    }
}

/**
 * BasicTextField wrapper for URL input with keyboard action support
 */
@Composable
private fun BasicTextField(
    value: String,
    onValueChange: (String) -> Unit,
    onGo: () -> Unit = {},
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    textStyle: androidx.compose.ui.text.TextStyle = LocalTextStyle.current,
    decorationBox: @Composable (innerTextField: @Composable () -> Unit) -> Unit
) {
    // FIX: Add keyboard action to support Return/Enter key for URL navigation
    androidx.compose.foundation.text.BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        singleLine = singleLine,
        textStyle = textStyle,
        decorationBox = decorationBox,
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Go
        ),
        keyboardActions = KeyboardActions(
            onGo = {
                onGo()
            }
        )
    )
}
