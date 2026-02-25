package com.augmentalis.cockpit.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.augmentalis.avanueui.components.AvanueCard
import com.augmentalis.avanueui.components.AvanueFAB
import com.augmentalis.avanueui.display.DisplayProfile
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.cockpit.model.DashboardModule
import com.augmentalis.cockpit.model.DashboardModuleRegistry
import com.augmentalis.cockpit.model.DashboardState

/**
 * Canvas — Spatial Zen shell.
 *
 * Philosophy: "Your workspace is an infinite calm canvas. Content exists in
 * space. You navigate by looking, not by clicking through menus."
 *
 * The home screen is an infinite 2D plane with module islands floating on it.
 * Users navigate by pinch-zoom (touch), scroll-wheel (desktop), or voice
 * ("zoom in", "focus on notes"). Semantic zoom levels control how much detail
 * is shown:
 *
 * - **Level 1 (1.0x)**: Overview — all module islands visible as small cards
 * - **Level 2 (2.0x)**: Module — selected module fills 60%, neighbors visible
 * - **Level 3 (4.0x)**: Focus — single module fullscreen, zero chrome
 *
 * Island positions are computed in a staggered organic layout (not a rigid grid)
 * to leverage spatial memory — users remember WHERE things are, not what menu
 * they're in.
 *
 * Responsive adaptation:
 * - Glass: Head-tracked viewport, voice zoom, 3 nearest islands visible
 * - Phone: Touch-driven pan/zoom, vertical bias
 * - Tablet: Spacious canvas, pen support
 * - Desktop: Mouse wheel zoom, click-drag pan
 */
@Composable
fun ZenCanvasLayout(
    dashboardState: DashboardState,
    displayProfile: DisplayProfile = DisplayProfile.PHONE,
    onModuleClick: (String) -> Unit,
    onVoiceActivate: () -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AvanueTheme.colors
    val modules = dashboardState.availableModules.ifEmpty { DashboardModuleRegistry.allModules }

    // Canvas state: zoom level and pan offset
    var zoomLevel by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    // Compute island positions based on module count and display profile
    val islandPositions = remember(modules.size, displayProfile) {
        computeIslandPositions(modules, displayProfile)
    }

    Box(modifier = modifier.fillMaxSize()) {
        // ── Dot Grid Background ─────────────────────────────────────────
        DotGridBackground(
            zoomLevel = zoomLevel,
            panOffset = panOffset,
            modifier = Modifier.fillMaxSize()
        )

        // ── Canvas Content (transformable) ──────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        zoomLevel = (zoomLevel * zoom).coerceIn(MIN_ZOOM, MAX_ZOOM)
                        panOffset += pan
                    }
                }
                .graphicsLayer {
                    scaleX = zoomLevel
                    scaleY = zoomLevel
                    translationX = panOffset.x
                    translationY = panOffset.y
                }
        ) {
            // Module islands
            islandPositions.forEachIndexed { index, position ->
                if (index < modules.size) {
                    ModuleIsland(
                        module = modules[index],
                        position = position,
                        zoomLevel = zoomLevel,
                        displayProfile = displayProfile,
                        onClick = { onModuleClick(modules[index].id) },
                    )
                }
            }
        }

        // ── Zoom Level Indicator ────────────────────────────────────────
        AnimatedVisibility(
            visible = !displayProfile.isGlass,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 8.dp, end = 12.dp)
        ) {
            ZoomLevelBadge(zoomLevel = zoomLevel)
        }

        // ── Zoom Rail (bottom) ──────────────────────────────────────────
        if (!displayProfile.isGlass) {
            ZoomRail(
                zoomLevel = zoomLevel,
                onZoomChange = { zoomLevel = it },
                displayProfile = displayProfile,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp, start = 24.dp, end = 80.dp)
                    .fillMaxWidth()
            )
        }

        // ── Bottom-right controls (voice + search) ─────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Search button
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.surface.copy(alpha = 0.7f))
                    .clickable { onSearchClick() }
                    .semantics { contentDescription = "Voice: click Search" },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = colors.textPrimary.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Voice FAB
            AvanueFAB(
                onClick = onVoiceActivate,
                modifier = Modifier
                    .size(56.dp)
                    .semantics { contentDescription = "Voice: click Microphone" },
            ) {
                Icon(
                    Icons.Default.Mic,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Glass: minimal HUD overlay
        if (displayProfile.isGlass) {
            GlassCanvasHud(
                moduleCount = modules.size,
                zoomLevel = zoomLevel,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp)
            )
        }

        // Footer (overview only)
        if (zoomLevel <= 1.2f && !displayProfile.isGlass) {
            Text(
                text = "VoiceOS\u00AE Avanues EcoSystem",
                color = colors.textPrimary.copy(alpha = 0.15f),
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 56.dp)
            )
        }
    }
}

// ── Constants ───────────────────────────────────────────────────────────────

private const val MIN_ZOOM = 0.5f
private const val MAX_ZOOM = 4f

// ── Island Positioning ──────────────────────────────────────────────────────

/**
 * Position of a module island on the canvas, in dp offsets from center.
 */
data class IslandPosition(
    val xDp: Float,
    val yDp: Float,
)

/**
 * Computes organic staggered positions for module islands.
 *
 * Uses a honeycomb-inspired layout rather than a rigid grid to create
 * a natural, memorable spatial arrangement. Frequently-used modules
 * gravitate toward the center (first in the list = closer to center).
 */
private fun computeIslandPositions(
    modules: List<DashboardModule>,
    profile: DisplayProfile,
): List<IslandPosition> {
    val spacing = when (profile) {
        DisplayProfile.GLASS_MICRO -> 80f
        DisplayProfile.GLASS_COMPACT -> 90f
        DisplayProfile.GLASS_STANDARD -> 100f
        DisplayProfile.PHONE -> 120f
        DisplayProfile.TABLET -> 140f
        DisplayProfile.GLASS_HD -> 110f
    }

    // Organic spiral layout: first module at center, others spiral outward
    val positions = mutableListOf<IslandPosition>()
    if (modules.isEmpty()) return positions

    // Center island
    positions.add(IslandPosition(0f, 0f))

    // Ring 1: 6 positions around center (hexagonal)
    val ring1Angles = listOf(0f, 60f, 120f, 180f, 240f, 300f)
    ring1Angles.forEach { angleDeg ->
        val rad = angleDeg * (Math.PI.toFloat() / 180f)
        positions.add(
            IslandPosition(
                xDp = kotlin.math.cos(rad) * spacing,
                yDp = kotlin.math.sin(rad) * spacing,
            )
        )
    }

    // Ring 2: 6 positions at 2x distance, offset by 30 degrees
    val ring2Angles = listOf(30f, 90f, 150f, 210f, 270f, 330f)
    ring2Angles.forEach { angleDeg ->
        val rad = angleDeg * (Math.PI.toFloat() / 180f)
        positions.add(
            IslandPosition(
                xDp = kotlin.math.cos(rad) * spacing * 2f,
                yDp = kotlin.math.sin(rad) * spacing * 2f,
            )
        )
    }

    return positions.take(modules.size)
}

// ── Composables ─────────────────────────────────────────────────────────────

/**
 * Subtle dot grid that provides spatial context on the canvas.
 */
@Composable
private fun DotGridBackground(
    zoomLevel: Float,
    panOffset: Offset,
    modifier: Modifier = Modifier,
) {
    val colors = AvanueTheme.colors
    val dotColor = colors.textPrimary.copy(alpha = 0.06f)
    val dotSpacing = 40f

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        // Compute grid offset from pan
        val offsetX = (panOffset.x * zoomLevel) % (dotSpacing * zoomLevel)
        val offsetY = (panOffset.y * zoomLevel) % (dotSpacing * zoomLevel)
        val scaledSpacing = dotSpacing * zoomLevel.coerceIn(0.5f, 2f)

        var x = offsetX
        while (x < width) {
            var y = offsetY
            while (y < height) {
                drawCircle(
                    color = dotColor,
                    radius = 1.5f,
                    center = Offset(x, y)
                )
                y += scaledSpacing
            }
            x += scaledSpacing
        }
    }
}

/**
 * A single module island on the canvas.
 */
@Composable
private fun ModuleIsland(
    module: DashboardModule,
    position: IslandPosition,
    zoomLevel: Float,
    displayProfile: DisplayProfile,
    onClick: () -> Unit,
) {
    val colors = AvanueTheme.colors
    val accentColor = Color(module.accentColorHex)
    val islandSize = when {
        displayProfile.isGlass -> 64.dp
        else -> 80.dp
    }

    // Convert dp position to pixel offset
    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = position.xDp.dp.roundToPx(),
                    y = position.yDp.dp.roundToPx(),
                )
            }
    ) {
        AvanueCard(
            onClick = onClick,
            modifier = Modifier
                .size(islandSize)
                .semantics { contentDescription = "Voice: click ${module.displayName}" },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                // Module icon with accent tint
                Icon(
                    moduleIconForCanvas(module.iconName),
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(if (displayProfile.isGlass) 20.dp else 28.dp)
                )

                if (!displayProfile.isGlass || zoomLevel > 1.5f) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = module.displayName,
                        color = colors.textPrimary,
                        fontSize = if (displayProfile.isGlass) 9.sp else 11.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

/**
 * Maps module icon names to Material icons for the Canvas shell.
 * Reuses the same mapping as DashboardLayout.
 */
private fun moduleIconForCanvas(iconName: String): androidx.compose.ui.graphics.vector.ImageVector = when (iconName) {
    "mic" -> Icons.Default.Mic
    "language" -> Icons.Default.Language
    "mouse" -> Icons.Default.TouchApp
    "picture_as_pdf" -> Icons.Default.PictureAsPdf
    "image" -> Icons.Default.Image
    "videocam" -> Icons.Default.Videocam
    "edit_note" -> Icons.Default.EditNote
    "photo_camera" -> Icons.Default.PhotoCamera
    "cast" -> Icons.Default.Cast
    "draw" -> Icons.Default.Brush
    else -> Icons.Default.Add
}

/**
 * Zoom level badge shown at top-right.
 */
@Composable
private fun ZoomLevelBadge(zoomLevel: Float) {
    val colors = AvanueTheme.colors
    val levelLabel = when {
        zoomLevel <= 0.8f -> "Overview"
        zoomLevel <= 1.5f -> "Level 1"
        zoomLevel <= 2.5f -> "Level 2"
        else -> "Focus"
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface.copy(alpha = 0.6f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = "$levelLabel \u00B7 ${(zoomLevel * 100).toInt()}%",
            color = colors.textPrimary.copy(alpha = 0.5f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

/**
 * Zoom rail at the bottom of the canvas — slider with +/- buttons.
 */
@Composable
private fun ZoomRail(
    zoomLevel: Float,
    onZoomChange: (Float) -> Unit,
    displayProfile: DisplayProfile,
    modifier: Modifier = Modifier,
) {
    val colors = AvanueTheme.colors

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Zoom out button
        IconButton(
            onClick = { onZoomChange((zoomLevel - 0.25f).coerceAtLeast(MIN_ZOOM)) },
            modifier = Modifier
                .size(32.dp)
                .semantics { contentDescription = "Voice: click Zoom Out" }
        ) {
            Icon(
                Icons.Default.Remove,
                contentDescription = null,
                tint = colors.textPrimary.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp)
            )
        }

        // Zoom slider
        Slider(
            value = zoomLevel,
            onValueChange = onZoomChange,
            valueRange = MIN_ZOOM..MAX_ZOOM,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = colors.primary,
                activeTrackColor = colors.primary.copy(alpha = 0.5f),
                inactiveTrackColor = colors.border.copy(alpha = 0.3f),
            )
        )

        // Zoom in button
        IconButton(
            onClick = { onZoomChange((zoomLevel + 0.25f).coerceAtMost(MAX_ZOOM)) },
            modifier = Modifier
                .size(32.dp)
                .semantics { contentDescription = "Voice: click Zoom In" }
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = null,
                tint = colors.textPrimary.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Minimal HUD for smart glasses — shows module count and zoom level.
 */
@Composable
private fun GlassCanvasHud(
    moduleCount: Int,
    zoomLevel: Float,
    modifier: Modifier = Modifier,
) {
    val colors = AvanueTheme.colors
    val levelLabel = when {
        zoomLevel <= 1.5f -> "Level 1"
        zoomLevel <= 2.5f -> "Level 2"
        else -> "Focus"
    }

    Text(
        text = "$levelLabel \u00B7 $moduleCount modules",
        color = colors.textPrimary.copy(alpha = 0.4f),
        fontSize = 10.sp,
        modifier = modifier,
    )
}
