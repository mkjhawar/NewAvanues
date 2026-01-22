package com.augmentalis.webavanue

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.augmentalis.webavanue.glassTab
import com.augmentalis.webavanue.TabUiState

/**
 * TabItem - Individual tab in the TabBar (Chrome-like with glassmorphism)
 *
 * Features:
 * - Chrome-like rounded top corners with glass effect
 * - Tab group color indicator (colored left border)
 * - Shows tab title (truncated if too long)
 * - Active state highlighting with brighter glass
 * - Close button with hover effect
 * - Loading indicator
 *
 * @param tabState Tab UI state with data and loading/navigation states
 * @param isActive Whether this tab is currently active
 * @param groupColor Optional tab group color for left border indicator
 * @param onClick Callback when tab is clicked
 * @param onClose Callback when close button is clicked
 * @param modifier Modifier for customization
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TabItem(
    tabState: TabUiState,
    isActive: Boolean,
    groupColor: Color? = null,
    onClick: () -> Unit,
    onClose: () -> Unit,
    onLongClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .widthIn(min = 120.dp, max = 240.dp)
            .height(36.dp)
            .glassTab(isActive = isActive)
            .background(
                color = if (isActive) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        // Tab group color indicator (left border)
        if (groupColor != null) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(3.dp)
                    .background(groupColor)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = if (groupColor != null) 12.dp else 16.dp,
                    end = 8.dp,
                    top = 6.dp,
                    bottom = 6.dp
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Tab title (with loading indicator if applicable)
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Loading indicator
                if (tabState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Tab title
                Text(
                    text = tabState.tab.title.ifBlank { "New Tab" },
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Close button
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(20.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Tab (Voice: close tab)",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

/**
 * TabItem with custom title (for preview/testing)
 */
@Composable
fun TabItem(
    title: String,
    isActive: Boolean,
    isLoading: Boolean = false,
    groupColor: Color? = null,
    onClick: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .widthIn(min = 120.dp, max = 240.dp)
            .height(36.dp)
            .glassTab(isActive = isActive)
            .background(
                color = if (isActive) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
            )
            .clickable { onClick() }
    ) {
        // Tab group color indicator
        if (groupColor != null) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(3.dp)
                    .background(groupColor)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = if (groupColor != null) 12.dp else 16.dp,
                    end = 8.dp,
                    top = 6.dp,
                    bottom = 6.dp
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Tab title
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = title.ifBlank { "New Tab" },
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Close button
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(20.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Tab (Voice: close tab)",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
