package com.augmentalis.avaelements.phase3

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

/**
 * Android Compose implementations for Phase 3 Display Components
 */

/**
 * Badge renderer for Android
 */
@Composable
fun RenderBadge(badge: Badge, content: @Composable () -> Unit, modifier: Modifier = Modifier) {
    if (!badge.visible) {
        content()
        return
    }

    BadgedBox(
        badge = {
            val badgeColor = when (badge.color) {
                BadgeColor.Primary -> MaterialTheme.colorScheme.primary
                BadgeColor.Secondary -> MaterialTheme.colorScheme.secondary
                BadgeColor.Error -> MaterialTheme.colorScheme.error
                BadgeColor.Success -> ComposeColor.Green
                BadgeColor.Warning -> ComposeColor(0xFFFFA726)
                BadgeColor.Info -> ComposeColor(0xFF29B6F6)
            }

            val badgeSize = when (badge.size) {
                BadgeSize.Small -> 12.dp
                BadgeSize.Medium -> 16.dp
                BadgeSize.Large -> 20.dp
            }

            if (badge.content != null) {
                Badge(
                    containerColor = badgeColor,
                    modifier = Modifier.size(badgeSize)
                ) {
                    val count = badge.content.toIntOrNull()
                    val displayText = if (count != null && count > badge.maxCount) {
                        "${badge.maxCount}+"
                    } else {
                        badge.content
                    }
                    Text(displayText, style = MaterialTheme.typography.labelSmall)
                }
            } else {
                // Dot badge
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(badgeColor, CircleShape)
                )
            }
        },
        modifier = modifier
    ) {
        content()
    }
}

/**
 * Chip renderer for Android
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenderChip(chip: Chip, modifier: Modifier = Modifier) {
    when (chip.variant) {
        ChipVariant.Filled -> FilterChip(
            selected = chip.selected,
            onClick = { if (chip.enabled) chip.onSelected?.invoke() },
            label = { Text(chip.label) },
            enabled = chip.enabled,
            trailingIcon = if (chip.closeable) {
                {
                    IconButton(
                        onClick = { chip.onClose?.invoke() },
                        modifier = Modifier.size(18.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Remove",
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            } else null,
            modifier = modifier
        )
        ChipVariant.Outlined -> OutlinedFilterChip(
            selected = chip.selected,
            onClick = { if (chip.enabled) chip.onSelected?.invoke() },
            label = { Text(chip.label) },
            enabled = chip.enabled,
            trailingIcon = if (chip.closeable) {
                {
                    IconButton(
                        onClick = { chip.onClose?.invoke() },
                        modifier = Modifier.size(18.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Remove",
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            } else null,
            modifier = modifier
        )
        ChipVariant.Elevated -> ElevatedFilterChip(
            selected = chip.selected,
            onClick = { if (chip.enabled) chip.onSelected?.invoke() },
            label = { Text(chip.label) },
            enabled = chip.enabled,
            trailingIcon = if (chip.closeable) {
                {
                    IconButton(
                        onClick = { chip.onClose?.invoke() },
                        modifier = Modifier.size(18.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Remove",
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            } else null,
            modifier = modifier
        )
    }
}

/**
 * Avatar renderer for Android
 */
@Composable
fun RenderAvatar(avatar: Avatar, modifier: Modifier = Modifier) {
    val size = when (avatar.size) {
        AvatarSize.ExtraSmall -> 24.dp
        AvatarSize.Small -> 32.dp
        AvatarSize.Medium -> 40.dp
        AvatarSize.Large -> 56.dp
        AvatarSize.ExtraLarge -> 72.dp
    }

    val shape = when (avatar.shape) {
        AvatarShape.Circle -> CircleShape
        AvatarShape.Square -> RoundedCornerShape(0.dp)
        AvatarShape.Rounded -> RoundedCornerShape(8.dp)
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(
                avatar.backgroundColor?.let {
                    ComposeColor(it.red, it.green, it.blue, (it.alpha * 255).toInt())
                } ?: MaterialTheme.colorScheme.primaryContainer
            )
            .then(
                if (avatar.onClick != null) {
                    Modifier.clickable { avatar.onClick.invoke() }
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        if (avatar.imageUrl != null) {
            AsyncImage(
                model = avatar.imageUrl,
                contentDescription = "Avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else if (avatar.initials != null) {
            Text(
                text = avatar.initials,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Divider renderer for Android
 */
@Composable
fun RenderDivider(divider: Divider, modifier: Modifier = Modifier) {
    val color = divider.color?.let {
        ComposeColor(it.red, it.green, it.blue, (it.alpha * 255).toInt())
    } ?: MaterialTheme.colorScheme.outlineVariant

    when (divider.orientation) {
        DividerOrientation.Horizontal -> HorizontalDivider(
            thickness = divider.thickness.dp,
            color = color,
            modifier = modifier.padding(
                start = divider.startIndent.dp,
                end = divider.endIndent.dp
            )
        )
        DividerOrientation.Vertical -> VerticalDivider(
            thickness = divider.thickness.dp,
            color = color,
            modifier = modifier.padding(
                top = divider.startIndent.dp,
                bottom = divider.endIndent.dp
            )
        )
    }
}

/**
 * Skeleton renderer for Android
 */
@Composable
fun RenderSkeleton(skeleton: Skeleton, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "skeleton")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    val shimmerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
        alpha = if (skeleton.animated) alpha else 0.3f
    )

    val shape = when (skeleton.variant) {
        SkeletonVariant.Text -> RoundedCornerShape(4.dp)
        SkeletonVariant.Circular -> CircleShape
        SkeletonVariant.Rectangular -> RoundedCornerShape(0.dp)
        SkeletonVariant.Rounded -> RoundedCornerShape(8.dp)
    }

    val defaultWidth = when (skeleton.variant) {
        SkeletonVariant.Text -> 200.dp
        SkeletonVariant.Circular -> 40.dp
        SkeletonVariant.Rectangular -> 300.dp
        SkeletonVariant.Rounded -> 200.dp
    }

    val defaultHeight = when (skeleton.variant) {
        SkeletonVariant.Text -> 16.dp
        SkeletonVariant.Circular -> 40.dp
        SkeletonVariant.Rectangular -> 200.dp
        SkeletonVariant.Rounded -> 48.dp
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        repeat(skeleton.count) {
            Box(
                modifier = Modifier
                    .width(skeleton.width?.dp ?: defaultWidth)
                    .height(skeleton.height?.dp ?: defaultHeight)
                    .background(shimmerColor, shape)
            )
        }
    }
}

/**
 * Spinner renderer for Android
 */
@Composable
fun RenderSpinner(spinner: Spinner, modifier: Modifier = Modifier) {
    val size = when (spinner.size) {
        SpinnerSize.Small -> 16.dp
        SpinnerSize.Medium -> 24.dp
        SpinnerSize.Large -> 32.dp
        SpinnerSize.ExtraLarge -> 48.dp
    }

    val color = spinner.color?.let {
        ComposeColor(it.red, it.green, it.blue, (it.alpha * 255).toInt())
    } ?: MaterialTheme.colorScheme.primary

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(size),
            color = color
        )

        if (spinner.label != null) {
            Text(
                text = spinner.label,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/**
 * ProgressBar renderer for Android
 */
@Composable
fun RenderProgressBar(progressBar: ProgressBar, modifier: Modifier = Modifier) {
    val color = progressBar.color?.let {
        ComposeColor(it.red, it.green, it.blue, (it.alpha * 255).toInt())
    } ?: MaterialTheme.colorScheme.primary

    Column(modifier = modifier) {
        if (progressBar.label != null) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = progressBar.label,
                    style = MaterialTheme.typography.bodyMedium
                )
                if (progressBar.showPercentage && !progressBar.indeterminate) {
                    Text(
                        text = "${(progressBar.progress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        when (progressBar.variant) {
            ProgressVariant.Linear -> {
                if (progressBar.indeterminate) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(progressBar.height.dp),
                        color = color
                    )
                } else {
                    LinearProgressIndicator(
                        progress = progressBar.progress.coerceIn(0f, 1f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(progressBar.height.dp),
                        color = color
                    )
                }
            }
            ProgressVariant.Circular -> {
                if (progressBar.indeterminate) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = color
                    )
                } else {
                    CircularProgressIndicator(
                        progress = progressBar.progress.coerceIn(0f, 1f),
                        modifier = Modifier.size(48.dp),
                        color = color
                    )
                }
            }
        }
    }
}

/**
 * Tooltip renderer for Android
 * Note: Material3 doesn't have built-in Tooltip yet, using a simplified version
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenderTooltip(
    tooltip: Tooltip,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    PlainTooltipBox(
        tooltip = {
            PlainTooltipBox(
                tooltip = { Text(tooltip.content) }
            )
        },
        modifier = modifier
    ) {
        content()
    }
}
