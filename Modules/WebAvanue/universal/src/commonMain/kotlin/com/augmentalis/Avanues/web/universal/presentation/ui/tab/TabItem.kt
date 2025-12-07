package com.augmentalis.Avanues.web.universal.presentation.ui.tab

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.augmentalis.Avanues.web.universal.presentation.viewmodel.TabUiState

/**
 * TabItem - Individual tab in the TabBar
 *
 * Features:
 * - Shows tab title (truncated if too long)
 * - Active state highlighting
 * - Close button
 * - Click to activate
 * - Loading indicator (optional)
 *
 * @param tabState Tab UI state with data and loading/navigation states
 * @param isActive Whether this tab is currently active
 * @param onClick Callback when tab is clicked
 * @param onClose Callback when close button is clicked
 * @param modifier Modifier for customization
 */
@Composable
fun TabItem(
    tabState: TabUiState,
    isActive: Boolean,
    onClick: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Dark 3D theme colors
    val bgSurface = Color(0xFF0F3460)
    val bgSecondary = Color(0xFF16213E)
    val textPrimary = Color(0xFFE8E8E8)
    val accentVoice = Color(0xFFA78BFA)

    Surface(
        modifier = modifier
            .widthIn(min = 100.dp, max = 200.dp)
            .height(36.dp)
            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
            .clickable { onClick() },
        color = if (isActive) {
            bgSurface
        } else {
            bgSecondary
        },
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Tab title (with loading indicator if applicable)
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Loading indicator
                if (tabState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        strokeWidth = 2.dp,
                        color = accentVoice
                    )
                }

                // Tab title
                Text(
                    text = tabState.tab.title.ifBlank { "New Tab" },
                    style = MaterialTheme.typography.labelSmall,
                    color = textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Close button
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Tab (Voice: close tab)",
                    tint = textPrimary.copy(alpha = 0.7f),
                    modifier = Modifier.size(12.dp)
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
    onClick: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Dark 3D theme colors
    val bgSurface = Color(0xFF0F3460)
    val bgSecondary = Color(0xFF16213E)
    val textPrimary = Color(0xFFE8E8E8)
    val accentVoice = Color(0xFFA78BFA)

    Surface(
        modifier = modifier
            .widthIn(min = 100.dp, max = 200.dp)
            .height(36.dp)
            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
            .clickable { onClick() },
        color = if (isActive) {
            bgSurface
        } else {
            bgSecondary
        },
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Tab title
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        strokeWidth = 2.dp,
                        color = accentVoice
                    )
                }

                Text(
                    text = title.ifBlank { "New Tab" },
                    style = MaterialTheme.typography.labelSmall,
                    color = textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Close button
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Tab (Voice: close tab)",
                    tint = textPrimary.copy(alpha = 0.7f),
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}
