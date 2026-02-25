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
import com.augmentalis.cockpit.model.IslandDepthTier
import com.augmentalis.cockpit.model.ModuleUsageTracker
import com.augmentalis.cockpit.model.RankedModule

/**
 * SpaceAvanue — Spatial Zen shell with usage-based island sizing.
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
 * Island layout is **usage-driven**: the most-launched modules get the largest
 * islands near the canvas center (NEAR tier), moderately-used modules occupy
 * the first ring (MID tier), and rarely-used modules drift to the outer ring
 * (FAR tier). This creates a self-organizing spatial memory model — users
 * remember WHERE things are because frequently-used modules are always
 * in the same prominent position.
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
    moduleUsageScores: Map<String, Float> = emptyMap(),
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

    // Rank modules by usage and compute island positions with depth tiers
    val rankedModules = remember(modules, moduleUsageScores) {
        val sorted = modules.sortedByDescending { moduleUsageScores[it.id] ?: 0f }
        sorted.mapIndexed { index, module ->
            RankedModule(
                module = module,
                usageScore = moduleUsageScores[module.id] ?: 0f,
                tier = ModuleUsageTracker.tierForIndex(index),
            )
        }
    }

    // Compute island positions based on ranked modules and display profile
    val islandPositions = remember(rankedModules.size, displayProfile) {
        computeIslandPositions(rankedModules, displayProfile)
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
            // Module islands — sized by usage tier (NEAR=large, MID=medium, FAR=small)
            islandPositions.forEachIndexed { index, position ->
                if (index < rankedModules.size) {
                    val ranked = rankedModules[index]
                    ModuleIsland(
                        module = ranked.module,
                        tier = ranked.tier,
                        position = position,
                        zoomLevel = zoomLevel,
                        displayProfile = displayProfile,
                        onClick = { onModuleClick(ranked.module.id) },
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
                moduleCount = rankedModules.size,
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
 * Computes usage-aware positions for module islands.
 *
 * Modules are pre-sorted by usage (highest first). Positioning follows
 * a 3-ring honeycomb layout mapped to depth tiers:
 *
 * - **NEAR tier** (indices 0-2): Center cluster — the 3 most-used modules
 *   occupy the center and its two closest hexagonal neighbors.
 * - **MID tier** (indices 3-6): First ring — 4 moderately-used modules
 *   in the remaining ring-1 positions.
 * - **FAR tier** (indices 7+): Outer ring — rarely-used modules at 2x distance.
 *
 * This creates a gravitational spatial model: heavy-use modules are close
 * and large, light-use modules are distant and small.
 */
private fun computeIslandPositions(
    rankedModules: List<RankedModule>,
    profile: DisplayProfile,
): List<IslandPosition> {
    val baseSpacing = when (profile) {
        DisplayProfile.GLASS_MICRO -> 80f
        DisplayProfile.GLASS_COMPACT -> 90f
        DisplayProfile.GLASS_STANDARD -> 100f
        DisplayProfile.PHONE -> 130f
        DisplayProfile.TABLET -> 150f
        DisplayProfile.GLASS_HD -> 110f
    }

    val positions = mutableListOf<IslandPosition>()
    if (rankedModules.isEmpty()) return positions

    // ── NEAR tier: center cluster (up to 3 modules) ──
    // Module 0: dead center
    positions.add(IslandPosition(0f, 0f))
    // Module 1-2: close neighbors at reduced spacing
    val nearAngles = listOf(-60f, 60f)
    nearAngles.forEach { angleDeg ->
        val rad = angleDeg * (kotlin.math.PI.toFloat() / 180f)
        positions.add(
            IslandPosition(
                xDp = kotlin.math.cos(rad) * baseSpacing * 0.8f,
                yDp = kotlin.math.sin(rad) * baseSpacing * 0.8f,
            )
        )
    }

    // ── MID tier: first ring remaining slots (up to 4 modules) ──
    val midAngles = listOf(150f, 210f, 270f, 330f)
    midAngles.forEach { angleDeg ->
        val rad = angleDeg * (kotlin.math.PI.toFloat() / 180f)
        positions.add(
            IslandPosition(
                xDp = kotlin.math.cos(rad) * baseSpacing,
                yDp = kotlin.math.sin(rad) * baseSpacing,
            )
        )
    }

    // ── FAR tier: outer ring (remaining modules) ──
    val farAngles = listOf(0f, 45f, 90f, 135f, 180f, 225f, 270f, 315f)
    farAngles.forEach { angleDeg ->
        val rad = angleDeg * (kotlin.math.PI.toFloat() / 180f)
        positions.add(
            IslandPosition(
                xDp = kotlin.math.cos(rad) * baseSpacing * 1.9f,
                yDp = kotlin.math.sin(rad) * baseSpacing * 1.9f,
            )
        )
    }

    return positions.take(rankedModules.size)
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
 * A single module island on the canvas, sized by its usage depth tier.
 *
 * - **NEAR**: Large island (100dp / 72dp glass) — most-used modules
 * - **MID**: Medium island (80dp / 60dp glass) — moderate usage
 * - **FAR**: Small island (64dp / 48dp glass) — rarely used
 */
@Composable
private fun ModuleIsland(
    module: DashboardModule,
    tier: IslandDepthTier,
    position: IslandPosition,
    zoomLevel: Float,
    displayProfile: DisplayProfile,
    onClick: () -> Unit,
) {
    val colors = AvanueTheme.colors
    val accentColor = Color(module.accentColorHex)

    // Tier-based island sizing
    val islandSize = when {
        displayProfile.isGlass -> tier.glassSizeDp.dp
        else -> tier.sizeDp.dp
    }
    val iconSize = when (tier) {
        IslandDepthTier.NEAR -> if (displayProfile.isGlass) 24.dp else 32.dp
        IslandDepthTier.MID -> if (displayProfile.isGlass) 20.dp else 28.dp
        IslandDepthTier.FAR -> if (displayProfile.isGlass) 16.dp else 22.dp
    }
    val labelFontSize = when (tier) {
        IslandDepthTier.NEAR -> if (displayProfile.isGlass) 10.sp else 12.sp
        IslandDepthTier.MID -> if (displayProfile.isGlass) 9.sp else 11.sp
        IslandDepthTier.FAR -> if (displayProfile.isGlass) 8.sp else 10.sp
    }
    // FAR tier islands are slightly translucent to reinforce depth perception
    val cardAlpha = when (tier) {
        IslandDepthTier.NEAR -> 1f
        IslandDepthTier.MID -> 0.9f
        IslandDepthTier.FAR -> 0.7f
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
            .graphicsLayer { alpha = cardAlpha }
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
                    .padding(if (tier == IslandDepthTier.FAR) 4.dp else 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                // Module icon with accent tint
                Icon(
                    moduleIconForCanvas(module.iconName),
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(iconSize)
                )

                // Show label if not glass or zoomed in enough
                if (!displayProfile.isGlass || zoomLevel > 1.5f || tier == IslandDepthTier.NEAR) {
                    Spacer(modifier = Modifier.height(if (tier == IslandDepthTier.FAR) 2.dp else 4.dp))
                    Text(
                        text = module.displayName,
                        color = colors.textPrimary,
                        fontSize = labelFontSize,
                        fontWeight = if (tier == IslandDepthTier.NEAR) FontWeight.SemiBold else FontWeight.Medium,
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
