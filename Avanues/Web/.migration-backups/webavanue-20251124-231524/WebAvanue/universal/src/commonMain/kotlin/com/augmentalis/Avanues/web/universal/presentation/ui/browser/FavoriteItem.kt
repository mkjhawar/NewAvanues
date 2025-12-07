package com.augmentalis.Avanues.web.universal.presentation.ui.browser

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.augmentalis.webavanue.domain.model.Favorite

/**
 * FavoriteItem - Individual favorite item in the favorites bar
 *
 * Features:
 * - Shows favicon (or placeholder) and title
 * - Click to navigate to URL
 * - Long-press to edit/delete
 * - Compact size for horizontal scrolling
 *
 * @param favorite The favorite data
 * @param onClick Callback when clicked
 * @param onLongPress Callback when long-pressed (for editing)
 * @param backgroundColor Background color for the item
 * @param modifier Modifier for customization
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FavoriteItem(
    favorite: Favorite,
    onClick: () -> Unit,
    onLongPress: () -> Unit = {},
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            ),
        color = backgroundColor,
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Favicon (placeholder for now - real favicon loading in future task)
            FaviconPlaceholder(
                url = favorite.url,
                modifier = Modifier.size(20.dp)
            )

            // Title
            Text(
                text = favorite.title.ifBlank { favorite.url },
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFFE8E8E8),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 120.dp)
            )
        }
    }
}

/**
 * FaviconPlaceholder - Shows a placeholder icon for favorite items
 *
 * TODO: Replace with actual favicon loading from URL
 */
@Composable
private fun FaviconPlaceholder(
    url: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Color(0xFF60A5FA).copy(alpha = 0.3f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            tint = Color(0xFF60A5FA),
            modifier = Modifier.size(12.dp)
        )
    }
}
