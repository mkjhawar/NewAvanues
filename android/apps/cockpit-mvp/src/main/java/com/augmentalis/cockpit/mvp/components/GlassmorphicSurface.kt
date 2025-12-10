package com.augmentalis.cockpit.mvp.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.augmentalis.cockpit.mvp.OceanTheme

/**
 * Glassmorphic surface wrapper component following NewAvanues UI Guidelines
 *
 * Pre-MagicUI Implementation Pattern:
 * - Today: Uses Material3 Surface with Ocean theme tokens
 * - Future: Replace with single line: MagicSurface(modifier) { content() }
 *
 * Features:
 * - Glassmorphic appearance (translucent with blur effect)
 * - Interactive states (hover, pressed, focus)
 * - Consistent Ocean theme styling
 * - MagicUI migration-ready (semantic wrapper)
 *
 * @param modifier Modifier for the surface
 * @param shape Shape of the surface (default: 16.dp rounded corners)
 * @param color Base surface color (default: OceanTheme.glassSurface)
 * @param border Border stroke (default: 1.dp glassBorder)
 * @param tonalElevation Tonal elevation (default: 4.dp)
 * @param shadowElevation Shadow elevation (default: 8.dp)
 * @param interactionSource Interaction source for hover/press states
 * @param content Content to display inside the surface
 */
@Composable
fun GlassmorphicSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    color: Color = OceanTheme.glassSurface,
    border: BorderStroke? = BorderStroke(1.dp, OceanTheme.glassBorder),
    tonalElevation: Dp = 4.dp,
    shadowElevation: Dp = 8.dp,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable () -> Unit
) {
    // Collect interaction states
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()

    // Determine surface color based on interaction state
    val surfaceColor = when {
        isPressed -> OceanTheme.glassSurfacePressed
        isHovered -> OceanTheme.glassSurfaceHover
        else -> color
    }

    Surface(
        modifier = modifier,
        color = surfaceColor,
        shape = shape,
        border = border,
        tonalElevation = tonalElevation,
        shadowElevation = shadowElevation
    ) {
        content()
    }
}

/**
 * Glassmorphic surface with focus state support
 *
 * Extended variant that supports focus border highlighting
 *
 * @param isFocused Whether the surface is focused
 * @param focusBorder Border to show when focused (default: glassBorderFocus)
 */
@Composable
fun GlassmorphicSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    color: Color = OceanTheme.glassSurface,
    tonalElevation: Dp = 4.dp,
    shadowElevation: Dp = 8.dp,
    isFocused: Boolean = false,
    focusBorder: BorderStroke = BorderStroke(2.dp, OceanTheme.glassBorderFocus),
    defaultBorder: BorderStroke = BorderStroke(1.dp, OceanTheme.glassBorder),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable () -> Unit
) {
    // Collect interaction states
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()

    // Determine surface color based on interaction state
    val surfaceColor = when {
        isPressed -> OceanTheme.glassSurfacePressed
        isHovered -> OceanTheme.glassSurfaceHover
        else -> color
    }

    // Determine border based on focus state
    val border = if (isFocused) focusBorder else defaultBorder

    Surface(
        modifier = modifier,
        color = surfaceColor,
        shape = shape,
        border = border,
        tonalElevation = tonalElevation,
        shadowElevation = shadowElevation
    ) {
        content()
    }
}

/**
 * Glassmorphic card wrapper (pre-configured for window/card use cases)
 *
 * Preset configuration for card-like components:
 * - 16.dp rounded corners
 * - 2.dp border width (more prominent than surface)
 * - Higher shadow elevation for depth
 */
@Composable
fun GlassmorphicCard(
    modifier: Modifier = Modifier,
    isFocused: Boolean = false,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable () -> Unit
) {
    GlassmorphicSurface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 4.dp,
        shadowElevation = 12.dp,
        isFocused = isFocused,
        focusBorder = BorderStroke(2.dp, OceanTheme.glassBorderFocus),
        defaultBorder = BorderStroke(2.dp, OceanTheme.glassBorder),
        interactionSource = interactionSource,
        content = content
    )
}
