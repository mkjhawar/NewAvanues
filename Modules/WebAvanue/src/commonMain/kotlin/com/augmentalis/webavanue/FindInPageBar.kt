package com.augmentalis.webavanue

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.avanueui.tokens.SpacingTokens
import com.augmentalis.webavanue.AppIcon
import com.augmentalis.webavanue.AppIconButton
import com.augmentalis.webavanue.AppSurface
import com.augmentalis.webavanue.IconVariant
import com.augmentalis.webavanue.SurfaceVariant

/**
 * FindInPageBar - Text search UI for browser
 *
 * Features:
 * - Search input field with real-time search
 * - Match counter ("3 of 15")
 * - Previous/Next navigation buttons
 * - Case sensitive toggle (note: not supported on Android WebView)
 * - Close button
 * - Keyboard shortcuts (Enter/Shift+Enter)
 *
 * Design:
 * - Uses Ocean design system for consistent styling
 * - Compact overlay bar at bottom of browser
 * - Auto-focuses search field on open
 * - Highlights match count
 *
 * @param query Current search query
 * @param currentMatch Current match index (0-based)
 * @param totalMatches Total number of matches found
 * @param caseSensitive Whether search is case-sensitive
 * @param onQueryChange Callback when query changes
 * @param onNext Navigate to next match
 * @param onPrevious Navigate to previous match
 * @param onCaseSensitiveToggle Toggle case sensitivity
 * @param onClose Close find bar
 * @param modifier Modifier for customization
 */
@Composable
fun FindInPageBar(
    query: String,
    currentMatch: Int,
    totalMatches: Int,
    caseSensitive: Boolean,
    onQueryChange: (String) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onCaseSensitiveToggle: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    // Auto-focus search field when bar opens
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    AppSurface(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        variant = SurfaceVariant.Elevated,
        shape = null,
        onClick = null
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SpacingTokens.sm)
        ) {
            // Search input field
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp),
                color = AvanueTheme.colors.surfaceInput,
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(SpacingTokens.sm)
                ) {
                    // Search input
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = AvanueTheme.colors.textPrimary
                        ),
                        cursorBrush = SolidColor(AvanueTheme.colors.primary),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(
                            onNext = { onNext() }
                        ),
                        decorationBox = { innerTextField ->
                            if (query.isEmpty()) {
                                Text(
                                    text = "Find in page",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = AvanueTheme.colors.textSecondary
                                )
                            }
                            innerTextField()
                        }
                    )

                    // Match counter
                    if (query.isNotEmpty()) {
                        Text(
                            text = if (totalMatches > 0) {
                                "${currentMatch + 1} of $totalMatches"
                            } else if (query.isNotEmpty()) {
                                "0 of 0"
                            } else {
                                ""
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = AvanueTheme.colors.textSecondary
                        )
                    }
                }
            }

            // Previous button
            AppIconButton(
                onClick = onPrevious,
                enabled = totalMatches > 0,
                modifier = Modifier.size(36.dp)
            ) {
                AppIcon(
                    imageVector = Icons.Default.ArrowUpward,
                    contentDescription = "Previous match (Shift+Enter)",
                    variant = if (totalMatches > 0) IconVariant.Primary else IconVariant.Disabled,
                    modifier = Modifier
                )
            }

            // Next button
            AppIconButton(
                onClick = onNext,
                enabled = totalMatches > 0,
                modifier = Modifier.size(36.dp)
            ) {
                AppIcon(
                    imageVector = Icons.Default.ArrowDownward,
                    contentDescription = "Next match (Enter)",
                    variant = if (totalMatches > 0) IconVariant.Primary else IconVariant.Disabled,
                    modifier = Modifier
                )
            }

            // Case sensitive toggle
            // Note: Disabled for now as Android WebView doesn't support it
            // Kept in UI for future iOS/Desktop support
            /*
            AppIconButton(
                onClick = onCaseSensitiveToggle,
                modifier = Modifier.size(36.dp)
            ) {
                AppIcon(
                    imageVector = Icons.Default.FormatSize,
                    contentDescription = if (caseSensitive) "Case sensitive" else "Case insensitive",
                    variant = if (caseSensitive) IconVariant.Primary else IconVariant.Secondary
                )
            }
            */

            // Close button
            AppIconButton(
                onClick = {
                    focusManager.clearFocus()
                    onClose()
                },
                modifier = Modifier.size(36.dp),
                enabled = true
            ) {
                AppIcon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close find bar (Escape)",
                    variant = IconVariant.Secondary,
                    modifier = Modifier
                )
            }
        }
    }
}
