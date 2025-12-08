package com.augmentalis.avaelements.renderers.android.mappers
import androidx.compose.runtime.*
import androidx.compose.animation.core.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.augmentalis.avaelements.core.Theme
import com.augmentalis.avaelements.components.phase3.display.*

// ============================================
// BADGE
// ============================================
@Composable
fun RenderBadge(c: Badge, theme: Theme) {
    val backgroundColor = when (c.variant) {
        "error" -> theme.colorScheme.error.toCompose()
        "warning" -> Color(0xFFFFA726) // Orange
        "success" -> Color(0xFF66BB6A) // Green
        "info" -> theme.colorScheme.primary.toCompose()
        else -> theme.colorScheme.surfaceVariant.toCompose()
    }

    val textColor = when (c.variant) {
        "error", "warning", "success", "info" -> Color.White
        else -> theme.colorScheme.onSurfaceVariant.toCompose()
    }

    Badge(
        containerColor = backgroundColor,
        contentColor = textColor
    ) {
        Text(
            text = c.text,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

// ============================================
// CHIP
// ============================================
@Composable
fun RenderChip(c: Chip, theme: Theme) {
    if (c.onDelete != null) {
        // Input chip with delete action
        InputChip(
            selected = false,
            onClick = {},
            label = { Text(text = c.label) },
            trailingIcon = {
                IconButton(
                    onClick = { c.onDelete?.invoke() },
                    modifier = Modifier.size(18.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Delete",
                        modifier = Modifier.size(14.dp)
                    )
                }
            },
            colors = InputChipDefaults.inputChipColors(
                containerColor = theme.colorScheme.surfaceVariant.toCompose(),
                labelColor = theme.colorScheme.onSurfaceVariant.toCompose()
            )
        )
    } else {
        // Suggestion chip (read-only)
        SuggestionChip(
            onClick = {},
            label = { Text(text = c.label) },
            colors = SuggestionChipDefaults.suggestionChipColors(
                containerColor = theme.colorScheme.surfaceVariant.toCompose(),
                labelColor = theme.colorScheme.onSurfaceVariant.toCompose()
            )
        )
    }
}

// ============================================
// AVATAR
// ============================================
@Composable
fun RenderAvatar(c: Avatar, theme: Theme) {
    Box(
        modifier = Modifier
            .size(c.size.dp)
            .clip(CircleShape)
            .background(
                c.style?.backgroundColor?.toCompose()
                    ?: theme.colorScheme.primaryContainer.toCompose()
            ),
        contentAlignment = Alignment.Center
    ) {
        if (c.imageUrl != null) {
            // TODO: Add AsyncImage support when Coil is added
            Text(
                text = c.initials ?: "?",
                color = theme.colorScheme.onPrimaryContainer.toCompose(),
                fontSize = (c.size / 2.5).sp
            )
        } else {
            Text(
                text = c.initials ?: "?",
                color = theme.colorScheme.onPrimaryContainer.toCompose(),
                fontSize = (c.size / 2.5).sp
            )
        }
    }
}

// ============================================
// DIVIDER
// ============================================
@Composable
fun RenderDivider(c: Divider, theme: Theme) {
    if (c.orientation == "vertical") {
        VerticalDivider(
            thickness = c.thickness.dp,
            color = c.style?.backgroundColor?.toCompose()
                ?: theme.colorScheme.outline.toCompose()
        )
    } else {
        HorizontalDivider(
            thickness = c.thickness.dp,
            color = c.style?.backgroundColor?.toCompose()
                ?: theme.colorScheme.outline.toCompose()
        )
    }
}

// ============================================
// SKELETON (Loading Placeholder)
// ============================================
@Composable
fun RenderSkeleton(c: Skeleton, theme: Theme) {
    val infiniteTransition = rememberInfiniteTransition(label = "skeleton")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeleton-alpha"
    )

    val shape = when (c.variant) {
        "circular" -> CircleShape
        "rounded" -> RoundedCornerShape(8.dp)
        else -> RectangleShape
    }

    Box(
        modifier = Modifier
            .size(c.width.dp, c.height.dp)
            .clip(shape)
            .background(theme.colorScheme.surfaceVariant.toCompose().copy(alpha = alpha))
    )
}

// ============================================
// SPINNER (Circular Progress)
// ============================================
@Composable
fun RenderSpinner(c: Spinner, theme: Theme) {
    CircularProgressIndicator(
        modifier = Modifier.size(c.size.dp),
        color = c.style?.backgroundColor?.toCompose()
            ?: theme.colorScheme.primary.toCompose(),
        strokeWidth = (c.size / 10).dp
    )
}

// ============================================
// PROGRESS BAR
// ============================================
@Composable
fun RenderProgressBar(c: ProgressBar, theme: Theme) {
    Column(modifier = Modifier.fillMaxWidth()) {
        LinearProgressIndicator(
            progress = { c.progress.toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
        )

        if (c.showLabel) {
            Text(
                text = "${(c.progress * 100).toInt()}%",
                fontSize = 12.sp,
                color = theme.colorScheme.onSurface.toCompose(),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

// ============================================
// TOOLTIP
// ============================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenderTooltip(c: Tooltip, theme: Theme) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = {
            PlainTooltip {
                Text(
                    text = c.text,
                    color = theme.colorScheme.onSurface.toCompose()
                )
            }
        },
        state = rememberTooltipState()
    ) {
        // Tooltip content would go here in actual usage
        Text(
            text = "?",
            modifier = Modifier.padding(4.dp),
            color = theme.colorScheme.primary.toCompose()
        )
    }
}
