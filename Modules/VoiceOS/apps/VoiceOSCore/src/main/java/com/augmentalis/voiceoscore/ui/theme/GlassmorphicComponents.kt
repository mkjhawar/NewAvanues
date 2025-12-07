/**
 * GlassmorphicComponents.kt - Ocean Theme Glassmorphic Components
 *
 * Reusable glassmorphic UI components with Ocean theme styling.
 *
 * ARCHITECTURE: Migration-Ready Design
 * - All components use consistent API surface
 * - When MagicUI is ready: Simple component name replacement
 *   - GlassSurface -> MagicUI.Surface
 *   - GlassCard -> MagicUI.Card
 *   - GlassBubble -> MagicUI.ChatBubble
 *   - OceanButton -> MagicUI.Button
 *   - GlassChip -> MagicUI.Chip
 *
 * Created: 2025-12-03
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.voiceoscore.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Glass Surface - Base glassmorphic container
 * Future: Replace with MagicUI.Surface
 *
 * @param modifier Modifier to apply
 * @param shape Shape of the surface
 * @param tonalElevation Elevation for depth
 * @param borderWidth Width of glass border
 * @param content Content composable
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    tonalElevation: Dp = 8.dp,
    borderWidth: Dp = 1.dp,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier
            .border(
                width = borderWidth,
                color = OceanGlass.Border,
                shape = shape
            ),
        shape = shape,
        color = OceanGlass.Surface,
        tonalElevation = tonalElevation
    ) {
        Box(
            modifier = Modifier.background(OceanGradients.Glass)
        ) {
            content()
        }
    }
}

/**
 * Glass Card - Glassmorphic card container
 * Future: Replace with MagicUI.Card
 *
 * @param modifier Modifier to apply
 * @param content Content composable
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier
                .border(
                    width = 1.dp,
                    color = OceanGlass.Border,
                    shape = GlassShapes.Card
                ),
            shape = GlassShapes.Card,
            colors = CardDefaults.cardColors(
                containerColor = OceanGlass.Surface
            )
        ) {
            Column(
                modifier = Modifier
                    .background(OceanGradients.Glass)
                    .padding(GlassDefaults.CardPadding),
                content = content
            )
        }
    } else {
        Card(
            modifier = modifier
                .border(
                    width = 1.dp,
                    color = OceanGlass.Border,
                    shape = GlassShapes.Card
                ),
            shape = GlassShapes.Card,
            colors = CardDefaults.cardColors(
                containerColor = OceanGlass.Surface
            )
        ) {
            Column(
                modifier = Modifier
                    .background(OceanGradients.Glass)
                    .padding(GlassDefaults.CardPadding),
                content = content
            )
        }
    }
}

/**
 * Glass Bubble - Glassmorphic chat bubble style container
 * Future: Replace with MagicUI.ChatBubble
 *
 * @param modifier Modifier to apply
 * @param isPrimary Whether this is primary styled (teal) or secondary (glass)
 * @param content Content composable
 */
@Composable
fun GlassBubble(
    modifier: Modifier = Modifier,
    isPrimary: Boolean = false,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier
            .border(
                width = 1.dp,
                color = if (isPrimary) {
                    OceanColors.TealSecondary.copy(alpha = 0.5f)
                } else {
                    OceanGlass.Border
                },
                shape = GlassShapes.Bubble
            ),
        shape = GlassShapes.Bubble,
        color = if (isPrimary) {
            OceanColors.TealPrimary.copy(alpha = 0.15f)
        } else {
            OceanGlass.Surface
        },
        tonalElevation = 2.dp
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = if (isPrimary) {
                        Brush.horizontalGradient(
                            colors = listOf(
                                OceanColors.TealPrimary.copy(alpha = 0.1f),
                                OceanColors.TealGlow.copy(alpha = 0.1f)
                            )
                        )
                    } else {
                        Brush.horizontalGradient(
                            colors = listOf(
                                OceanGlass.Blur,
                                OceanGlass.Surface
                            )
                        )
                    }
                )
                .padding(12.dp)
        ) {
            content()
        }
    }
}

/**
 * Ocean Button - Glassmorphic button with teal gradient
 * Future: Replace with MagicUI.Button
 *
 * @param onClick Click handler
 * @param modifier Modifier to apply
 * @param enabled Whether button is enabled
 * @param style Button style (Primary, Secondary, Tertiary)
 * @param content Button content
 */
@Composable
fun OceanButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: OceanButtonStyle = OceanButtonStyle.Primary,
    content: @Composable RowScope.() -> Unit
) {
    when (style) {
        OceanButtonStyle.Primary -> {
            Button(
                onClick = onClick,
                enabled = enabled,
                modifier = modifier
                    .background(
                        brush = OceanGradients.Teal,
                        shape = GlassShapes.Button
                    ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent
                ),
                shape = GlassShapes.Button,
                content = content
            )
        }

        OceanButtonStyle.Secondary -> {
            OutlinedButton(
                onClick = onClick,
                enabled = enabled,
                modifier = modifier
                    .border(
                        width = 1.dp,
                        color = OceanGlass.Border,
                        shape = GlassShapes.Button
                    ),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = OceanGlass.Surface
                ),
                shape = GlassShapes.Button,
                content = content
            )
        }

        OceanButtonStyle.Tertiary -> {
            TextButton(
                onClick = onClick,
                enabled = enabled,
                modifier = modifier,
                content = content
            )
        }
    }
}

/**
 * Ocean Button Styles
 * Future: Maps to MagicUI.ButtonStyle
 */
enum class OceanButtonStyle {
    Primary,    // Teal gradient
    Secondary,  // Glass outline
    Tertiary    // Text only
}

/**
 * Glass Chip - Small glassmorphic label/tag
 * Future: Replace with MagicUI.Chip
 *
 * @param text Chip text
 * @param modifier Modifier to apply
 * @param isPrimary Whether to use primary (teal) or glass styling
 * @param onDelete Optional delete handler
 */
@Composable
fun GlassChip(
    text: String,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = false,
    onDelete: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier
            .border(
                width = 1.dp,
                color = if (isPrimary) {
                    OceanColors.TealSecondary.copy(alpha = 0.5f)
                } else {
                    OceanGlass.Border
                },
                shape = GlassShapes.Chip
            ),
        shape = GlassShapes.Chip,
        color = if (isPrimary) {
            OceanColors.TealPrimary.copy(alpha = 0.2f)
        } else {
            OceanGlass.Surface
        }
    ) {
        Row(
            modifier = Modifier
                .background(
                    brush = if (isPrimary) {
                        OceanGradients.Teal
                    } else {
                        OceanGradients.Glass
                    }
                )
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = if (isPrimary) OceanColors.TextPrimary else OceanColors.TextSecondary,
                fontWeight = if (isPrimary) FontWeight.Bold else FontWeight.Normal
            )

            if (onDelete != null) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(16.dp)
                ) {
                    // Delete icon would go here
                }
            }
        }
    }
}

/**
 * Ocean Circular Button - Circular button for actions like record
 * Future: Replace with MagicUI.Button(shape = CircleShape)
 *
 * @param onClick Click handler
 * @param modifier Modifier to apply
 * @param size Size of the circular button
 * @param content Button content (usually an Icon)
 */
@Composable
fun OceanCircularButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .size(size)
            .border(
                width = 2.dp,
                brush = OceanGradients.Teal,
                shape = CircleShape
            )
            .background(
                brush = OceanGradients.Teal,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        IconButton(onClick = onClick) {
            content()
        }
    }
}
