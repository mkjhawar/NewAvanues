package com.augmentalis.webavanue

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.avanues.themes.OceanTheme

/**
 * CollapsibleSectionHeader - Expandable/collapsible section header for settings
 *
 * @param title Section title
 * @param isExpanded Whether section is currently expanded
 * @param onToggle Callback when header is clicked to toggle expansion
 * @param matchesSearch Whether section matches current search query
 * @param modifier Modifier for customization
 */
@Composable
fun CollapsibleSectionHeader(
    title: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    matchesSearch: Boolean,
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "chevron_rotation"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        color = if (matchesSearch && !isExpanded) {
            OceanTheme.primary.copy(alpha = 0.15f)
        } else {
            Color.Transparent
        },
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = if (matchesSearch) {
                    OceanTheme.primary
                } else {
                    OceanTheme.textPrimary
                }
            )

            Icon(
                imageVector = Icons.Default.ExpandMore,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                modifier = Modifier
                    .size(24.dp)
                    .rotate(rotation),
                tint = if (matchesSearch) {
                    OceanTheme.primary
                } else {
                    OceanTheme.textSecondary
                }
            )
        }
    }
}
