/*
 * Copyright (c) 2026 Manoj Jhawar, Aman Jhawar
 * Intelligent Devices LLC
 * All rights reserved.
 */

package com.augmentalis.voiceavanue.ui.hub

import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.augmentalis.avanueui.components.AvanueCard
import com.augmentalis.avanueui.tokens.SpacingTokens
import com.augmentalis.avanueui.theme.AvanueTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Spatial Orbit Hub — a radial launcher showing all Avanues modules.
 *
 * Layout:
 *   Center: Pulsing VoiceOS brain icon (hub of the ecosystem)
 *   Inner ring (4 core modules): VoiceTouch, WebAvanue, CursorAvanue, Cockpit
 *   Outer ring (7 content modules): PDF, Image, Video, Notes, Camera, Cast, Annotate
 *   Bottom dock: Settings, About, Developer (easter egg)
 *
 * Canvas draws orbit rings and connection lines behind the positioned composables.
 * Each module node is a clickable circle with icon + label below.
 */
@Composable
fun SpatialOrbitHub(
    onModuleClick: (HubModule) -> Unit,
    onSettingsClick: () -> Unit,
    onAboutClick: () -> Unit,
    onDeveloperClick: () -> Unit
) {
    val coreModules = HubModuleRegistry.coreModules
    val contentModules = HubModuleRegistry.contentModules

    // Infinite pulsing animation for center brain icon
    val infiniteTransition = rememberInfiniteTransition(label = "center_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    // 4-tap Easter egg for developer mode (on center brain icon)
    var tapCount by remember { mutableIntStateOf(0) }
    var firstTapTime by remember { mutableLongStateOf(0L) }
    var devModeActivated by remember { mutableStateOf(false) }

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            AvanueTheme.colors.background,
            AvanueTheme.colors.surface.copy(alpha = 0.6f),
            AvanueTheme.colors.background
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient)
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── Header ──
        OrbitHeader(onSettingsClick = onSettingsClick)

        // ── Orbital Area ──
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            val density = LocalDensity.current
            val availableWidthPx = with(density) { maxWidth.toPx() }
            val availableHeightPx = with(density) { maxHeight.toPx() }
            val minDimensionPx = minOf(availableWidthPx, availableHeightPx)

            // Orbit radii scaled to available space
            val innerRadiusPx = minDimensionPx * 0.22f
            val outerRadiusPx = minDimensionPx * 0.38f

            // Resolve accent colors outside Canvas (they're @Composable)
            val primaryColor = AvanueTheme.colors.primary
            val lineColor = AvanueTheme.colors.textDisabled
            val surfaceColor = AvanueTheme.colors.surface
            val coreAccents = remember(coreModules) {
                coreModules.map { it.id }
            }.map { moduleAccentColor(it) }
            val contentAccents = remember(contentModules) {
                contentModules.map { it.id }
            }.map { moduleAccentColor(it) }

            // Calculate orbital positions
            // Inner orbit: 4 items starting at top (-PI/2), evenly spaced
            val corePositions = orbitPositions(
                count = coreModules.size,
                radiusPx = innerRadiusPx,
                startAngle = (-PI / 2).toFloat()
            )
            // Outer orbit: 7 items, offset by half-step to avoid alignment with inner
            val contentPositions = orbitPositions(
                count = contentModules.size,
                radiusPx = outerRadiusPx,
                startAngle = (-PI / 2 + PI / contentModules.size).toFloat()
            )

            // Canvas: orbit rings, connection lines, center glow
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)

                // Outer orbit ring
                drawCircle(
                    color = lineColor.copy(alpha = 0.06f),
                    radius = outerRadiusPx,
                    center = center,
                    style = Stroke(width = 1.5f)
                )

                // Inner orbit ring
                drawCircle(
                    color = lineColor.copy(alpha = 0.10f),
                    radius = innerRadiusPx,
                    center = center,
                    style = Stroke(width = 1.5f)
                )

                // Center glow (animated)
                drawCircle(
                    color = primaryColor.copy(alpha = glowAlpha * 0.4f),
                    radius = with(density) { 44.dp.toPx() },
                    center = center
                )

                // Connection lines: center → each inner orbit node
                corePositions.forEachIndexed { index, pos ->
                    drawLine(
                        color = coreAccents[index].copy(alpha = 0.12f),
                        start = center,
                        end = Offset(center.x + pos.x, center.y + pos.y),
                        strokeWidth = 1.5f,
                        cap = StrokeCap.Round
                    )
                }
            }

            // Center VoiceOS brain icon (pulsing)
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = glowAlpha),
                                primaryColor.copy(alpha = 0.05f)
                            )
                        )
                    )
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        val now = System.currentTimeMillis()
                        if (now - firstTapTime > 2000L) {
                            tapCount = 1
                            firstTapTime = now
                        } else {
                            tapCount++
                        }
                        if (tapCount >= 4) {
                            devModeActivated = true
                            tapCount = 0
                        }
                    }
                    .semantics { contentDescription = "Voice: click VoiceOS Hub" },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(surfaceColor.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Hub,
                        contentDescription = "VoiceOS Hub",
                        tint = primaryColor,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Inner orbit module nodes
            coreModules.forEachIndexed { index, module ->
                OrbitNode(
                    module = module,
                    accentColor = coreAccents[index],
                    nodeSize = 52.dp,
                    iconSize = 24.dp,
                    labelSizeSp = 11,
                    positionPx = corePositions[index],
                    onClick = { onModuleClick(module) }
                )
            }

            // Outer orbit module nodes
            contentModules.forEachIndexed { index, module ->
                OrbitNode(
                    module = module,
                    accentColor = contentAccents[index],
                    nodeSize = 42.dp,
                    iconSize = 20.dp,
                    labelSizeSp = 10,
                    positionPx = contentPositions[index],
                    onClick = { onModuleClick(module) }
                )
            }
        }

        // ── Bottom Dock ──
        BottomDock(
            onSettingsClick = onSettingsClick,
            onAboutClick = onAboutClick,
            devModeActivated = devModeActivated,
            onDeveloperClick = onDeveloperClick
        )
    }
}

// ──────────────────────────── ORBIT NODE ────────────────────────────

/**
 * A single module node on the orbit. Circular icon with label below.
 * Positioned via offset from the parent Box's center.
 */
@Composable
private fun OrbitNode(
    module: HubModule,
    accentColor: Color,
    nodeSize: Dp,
    iconSize: Dp,
    labelSizeSp: Int,
    positionPx: Offset,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .offset { IntOffset(positionPx.x.roundToInt(), positionPx.y.roundToInt()) }
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            )
            .semantics { contentDescription = "Voice: click Open ${module.displayName}" },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Box(
            modifier = Modifier
                .size(nodeSize)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = module.icon,
                contentDescription = module.displayName,
                tint = accentColor,
                modifier = Modifier.size(iconSize)
            )
        }
        Text(
            text = module.displayName,
            style = MaterialTheme.typography.labelSmall,
            color = AvanueTheme.colors.textSecondary,
            fontSize = labelSizeSp.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.Medium
        )
    }
}

// ──────────────────────────── HEADER ────────────────────────────

@Composable
private fun OrbitHeader(onSettingsClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SpacingTokens.md)
            .padding(top = SpacingTokens.sm, bottom = SpacingTokens.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = buildAnnotatedString {
                    append("Avanues")
                    withStyle(
                        SpanStyle(
                            baselineShift = BaselineShift.Superscript,
                            fontSize = 10.sp
                        )
                    ) {
                        append("\u00AE")
                    }
                },
                style = MaterialTheme.typography.headlineMedium,
                color = AvanueTheme.colors.textPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Your accessibility ecosystem",
                style = MaterialTheme.typography.bodySmall,
                color = AvanueTheme.colors.textSecondary
            )
        }
        IconButton(
            onClick = onSettingsClick,
            modifier = Modifier.semantics { contentDescription = "Voice: click Settings" }
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = AvanueTheme.colors.textSecondary,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

// ──────────────────────────── BOTTOM DOCK ────────────────────────────

@Composable
private fun BottomDock(
    onSettingsClick: () -> Unit,
    onAboutClick: () -> Unit,
    devModeActivated: Boolean,
    onDeveloperClick: () -> Unit
) {
    AvanueCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SpacingTokens.lg, vertical = SpacingTokens.sm)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SpacingTokens.lg, vertical = SpacingTokens.sm),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DockItem(
                icon = Icons.Default.Settings,
                label = "Settings",
                tint = AvanueTheme.colors.textSecondary,
                onClick = onSettingsClick
            )
            DockItem(
                icon = Icons.Default.Info,
                label = "About",
                tint = AvanueTheme.colors.textSecondary,
                onClick = onAboutClick
            )
            if (devModeActivated) {
                DockItem(
                    icon = Icons.Default.BugReport,
                    label = "Developer",
                    tint = AvanueTheme.colors.warning,
                    onClick = onDeveloperClick
                )
            }
        }
    }

    // Branding footer
    Text(
        text = "VoiceOS\u00AE Avanues EcoSystem",
        style = MaterialTheme.typography.labelSmall,
        color = AvanueTheme.colors.textDisabled,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = SpacingTokens.sm)
    )
}

@Composable
private fun DockItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            )
            .semantics { contentDescription = "Voice: click $label" }
            .padding(horizontal = SpacingTokens.md, vertical = SpacingTokens.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            fontSize = 10.sp
        )
    }
}

// ──────────────────────────── MATH HELPERS ────────────────────────────

/**
 * Calculates evenly-spaced positions on a circle.
 *
 * @param count Number of items to place
 * @param radiusPx Radius of the orbit in pixels
 * @param startAngle Starting angle in radians (-PI/2 = top/12 o'clock)
 * @return List of Offset(x, y) relative to center (0, 0)
 */
private fun orbitPositions(
    count: Int,
    radiusPx: Float,
    startAngle: Float = (-PI / 2).toFloat()
): List<Offset> {
    if (count == 0) return emptyList()
    val step = (2 * PI / count).toFloat()
    return (0 until count).map { i ->
        val angle = startAngle + step * i
        Offset(
            x = radiusPx * cos(angle),
            y = radiusPx * sin(angle)
        )
    }
}
