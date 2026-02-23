package com.augmentalis.cockpit.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.cockpit.spatial.PseudoSpatialController

/**
 * PseudoSpatial canvas that wraps content in a 4-layer parallax depth system.
 *
 * Creates a cockpit HUD aesthetic on flat screens (phones/tablets) with:
 * - Scanline grid: subtle 48dp grid lines for a technical/cockpit feel
 * - Background layer: slow-moving ambient content (0.3x parallax)
 * - Mid-ground layer: medium-speed secondary content (0.6x parallax)
 * - Foreground layer: full parallax + 3D card tilt (1.0x parallax, 3-degree max tilt)
 * - HUD layer: locked in place, no parallax (status, command bar)
 * - Corner accents: bright accent-colored L-brackets at the 4 corners
 *
 * The parallax is driven by [PseudoSpatialController] which consumes
 * gyroscope data from [ISpatialOrientationSource].
 *
 * All colors use [AvanueTheme.colors] — works across all 32 theme combinations.
 *
 * @param controller The pseudo-spatial controller providing parallax offsets
 * @param showScanlines Whether to render the background scanline grid
 * @param showCornerAccents Whether to render corner accent L-brackets
 * @param backgroundContent Content for the background layer (0.3x parallax)
 * @param midgroundContent Content for the mid-ground layer (0.6x parallax)
 * @param foregroundContent Content for the foreground layer (1.0x parallax + tilt)
 * @param hudContent Content for the HUD layer (no parallax, locked in place)
 */
@Composable
fun PseudoSpatialCanvas(
    controller: PseudoSpatialController,
    modifier: Modifier = Modifier,
    showScanlines: Boolean = true,
    showCornerAccents: Boolean = true,
    backgroundContent: @Composable () -> Unit = {},
    midgroundContent: @Composable () -> Unit = {},
    foregroundContent: @Composable () -> Unit,
    hudContent: @Composable () -> Unit = {}
) {
    val parallax by controller.state.collectAsState()
    val isEnabled by controller.isEnabled.collectAsState()
    val density = LocalDensity.current
    val accentColor = AvanueTheme.colors.primary

    Box(modifier = modifier.fillMaxSize()) {
        // Layer 0: Scanline grid overlay (background decoration)
        if (showScanlines) {
            ScanlineGrid(
                color = AvanueTheme.colors.onBackground.copy(alpha = 0.05f),
                spacingDp = 48f
            )
        }

        if (isEnabled) {
            // Layer 1: Background (0.3x parallax)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = with(density) { parallax.backgroundOffsetX.dp.toPx() }
                        translationY = with(density) { parallax.backgroundOffsetY.dp.toPx() }
                    }
            ) {
                backgroundContent()
            }

            // Layer 2: Mid-ground (0.6x parallax)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = with(density) { parallax.midgroundOffsetX.dp.toPx() }
                        translationY = with(density) { parallax.midgroundOffsetY.dp.toPx() }
                    }
            ) {
                midgroundContent()
            }

            // Layer 3: Foreground (1.0x parallax + 3D tilt)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = with(density) { parallax.foregroundOffsetX.dp.toPx() }
                        translationY = with(density) { parallax.foregroundOffsetY.dp.toPx() }
                        rotationY = parallax.cardRotationY
                        rotationX = parallax.cardRotationX
                        cameraDistance = 12f * density.density
                    }
            ) {
                foregroundContent()
            }
        } else {
            // Parallax disabled — render all content flat (no movement/tilt)
            backgroundContent()
            midgroundContent()
            foregroundContent()
        }

        // Layer 4: HUD (locked, no parallax — always rendered regardless of enabled state)
        Box(modifier = Modifier.fillMaxSize()) {
            hudContent()
        }

        // Corner accent decorations
        if (showCornerAccents) {
            CornerAccents(color = accentColor)
        }
    }
}

/**
 * Subtle scanline grid overlay for cockpit HUD aesthetic.
 *
 * Draws a regular grid of thin lines (0.5dp stroke, 5% opacity)
 * at [spacingDp] intervals. Creates a technical/engineering feel
 * without being distracting.
 */
@Composable
private fun ScanlineGrid(
    color: Color,
    spacingDp: Float,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    Canvas(modifier = modifier.fillMaxSize()) {
        val spacingPx = with(density) { spacingDp.dp.toPx() }
        val strokeWidth = with(density) { 0.5.dp.toPx() }

        // Vertical lines
        var x = 0f
        while (x < size.width) {
            drawLine(
                color = color,
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = strokeWidth
            )
            x += spacingPx
        }

        // Horizontal lines
        var y = 0f
        while (y < size.height) {
            drawLine(
                color = color,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = strokeWidth
            )
            y += spacingPx
        }
    }
}

/**
 * Corner accent borders — bright accent-colored L-brackets at the 4 corners.
 *
 * Creates a cockpit HUD framing effect. Each corner has two perpendicular
 * lines (12dp long, 2dp stroke, 60% opacity) extending inward from
 * an 8dp margin. Uses the current theme's primary accent color.
 */
@Composable
private fun CornerAccents(
    color: Color,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    Canvas(modifier = modifier.fillMaxSize()) {
        val lineLength = with(density) { 12.dp.toPx() }
        val strokeWidth = with(density) { 2.dp.toPx() }
        val margin = with(density) { 8.dp.toPx() }
        val accentColor = color.copy(alpha = 0.6f)

        // Top-left corner
        drawLine(accentColor, Offset(margin, margin), Offset(margin + lineLength, margin), strokeWidth)
        drawLine(accentColor, Offset(margin, margin), Offset(margin, margin + lineLength), strokeWidth)

        // Top-right corner
        drawLine(accentColor, Offset(size.width - margin, margin), Offset(size.width - margin - lineLength, margin), strokeWidth)
        drawLine(accentColor, Offset(size.width - margin, margin), Offset(size.width - margin, margin + lineLength), strokeWidth)

        // Bottom-left corner
        drawLine(accentColor, Offset(margin, size.height - margin), Offset(margin + lineLength, size.height - margin), strokeWidth)
        drawLine(accentColor, Offset(margin, size.height - margin), Offset(margin, size.height - margin - lineLength), strokeWidth)

        // Bottom-right corner
        drawLine(accentColor, Offset(size.width - margin, size.height - margin), Offset(size.width - margin - lineLength, size.height - margin), strokeWidth)
        drawLine(accentColor, Offset(size.width - margin, size.height - margin), Offset(size.width - margin, size.height - margin - lineLength), strokeWidth)
    }
}
