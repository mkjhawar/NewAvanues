package com.augmentalis.cockpit.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.augmentalis.avanueui.theme.AvanueTheme
import kotlinx.serialization.Serializable
import kotlin.math.sin
import kotlin.random.Random

/**
 * Background scenes for the Cockpit layout area.
 *
 * Each scene renders a full-screen backdrop behind the content layer.
 * The active scene is persisted via [CockpitViewModel] and can be
 * toggled at runtime from the command bar or voice commands.
 */
@Serializable
enum class BackgroundScene {
    /** Default gradient from AvanueTheme colors. */
    GRADIENT,

    /** Animated starfield with twinkling dots. */
    STARFIELD,

    /** Scanline grid with subtle glow — sci-fi HUD aesthetic. */
    SCANLINE_GRID,

    /** Fully transparent — for see-through glass displays. */
    TRANSPARENT;

    companion object {
        val DEFAULT = GRADIENT
    }
}

/**
 * Renders the selected [BackgroundScene] as a full-size composable layer.
 *
 * Place this behind the content Column inside a [Box] so the scene
 * fills the entire layout area without affecting content z-order.
 */
@Composable
fun BackgroundSceneRenderer(
    scene: BackgroundScene,
    modifier: Modifier = Modifier
) {
    when (scene) {
        BackgroundScene.GRADIENT -> GradientBackground(modifier)
        BackgroundScene.STARFIELD -> StarfieldBackground(modifier)
        BackgroundScene.SCANLINE_GRID -> ScanlineGridBackground(modifier)
        BackgroundScene.TRANSPARENT -> { /* nothing — see-through */ }
    }
}

// ── GRADIENT ────────────────────────────────────────────────────────────────

/**
 * Vertical gradient matching the original CockpitScreenContent backdrop:
 * background → surface(0.6 alpha) → background.
 */
@Composable
private fun GradientBackground(modifier: Modifier = Modifier) {
    val colors = AvanueTheme.colors
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        colors.background,
                        colors.surface.copy(alpha = 0.6f),
                        colors.background
                    )
                )
            )
    )
}

// ── STARFIELD ───────────────────────────────────────────────────────────────

/**
 * Holds normalized (0..1) position and base size for a single star.
 * Positions are scaled to actual canvas dimensions at draw time.
 */
private data class Star(
    val nx: Float,
    val ny: Float,
    val radius: Float,
    val phase: Float
)

/**
 * Animated starfield: ~80 white dots on a dark background.
 *
 * A continuously-animating float drives a sin-based alpha modulation
 * per star (each star has a random phase offset) so they twinkle
 * asynchronously. Stars range from 1dp to 3dp radius.
 */
@Composable
private fun StarfieldBackground(modifier: Modifier = Modifier) {
    val bgColor = AvanueTheme.colors.background

    val stars = remember {
        List(80) {
            Star(
                nx = Random.nextFloat(),
                ny = Random.nextFloat(),
                radius = 1f + Random.nextFloat() * 2f,     // 1..3 dp
                phase = Random.nextFloat() * 6.2832f         // 0..2PI
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "starfield")
    val tick by transition.animateFloat(
        initialValue = 0f,
        targetValue = 6.2832f, // 2 * PI
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "starTick"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        // Fill background
        drawRect(color = bgColor)

        val w = size.width
        val h = size.height
        val starColor = Color.White

        for (star in stars) {
            // Sin-based alpha: ranges 0.15 .. 0.85 so stars never fully vanish
            val alpha = 0.15f + 0.7f * ((sin(tick + star.phase) + 1f) / 2f)
            drawCircle(
                color = starColor.copy(alpha = alpha),
                radius = star.radius,
                center = Offset(star.nx * w, star.ny * h)
            )
        }
    }
}

// ── SCANLINE GRID ───────────────────────────────────────────────────────────

/**
 * Sci-fi HUD grid: horizontal scanlines every 8dp, vertical lines every
 * 40dp, plus a subtle glow dot at each intersection using the theme
 * primary color.
 *
 * A slow vertical scan bar (semi-transparent horizontal line) sweeps
 * from top to bottom over 4 seconds for a radar/HUD feel.
 */
@Composable
private fun ScanlineGridBackground(modifier: Modifier = Modifier) {
    val bgColor = AvanueTheme.colors.background
    val primaryColor = AvanueTheme.colors.primary

    val transition = rememberInfiniteTransition(label = "scanbar")
    val scanProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scanProgress"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(color = bgColor)

        val w = size.width
        val h = size.height

        val hLineSpacing = 8f   // dp-ish spacing (Canvas uses px but small enough for visual effect)
        val vLineSpacing = 40f
        val lineColor = Color.White

        // Horizontal scanlines
        var y = 0f
        while (y < h) {
            drawLine(
                color = lineColor.copy(alpha = 0.08f),
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = 0.5f
            )
            y += hLineSpacing
        }

        // Vertical grid lines
        var x = 0f
        while (x < w) {
            drawLine(
                color = lineColor.copy(alpha = 0.05f),
                start = Offset(x, 0f),
                end = Offset(x, h),
                strokeWidth = 0.5f
            )
            x += vLineSpacing
        }

        // Glow dots at grid intersections
        val dotColor = primaryColor.copy(alpha = 0.15f)
        y = 0f
        while (y < h) {
            x = 0f
            while (x < w) {
                drawCircle(
                    color = dotColor,
                    radius = 1.5f,
                    center = Offset(x, y)
                )
                x += vLineSpacing
            }
            y += hLineSpacing
        }

        // Animated scan bar — a horizontal bright line sweeping top→bottom
        val scanY = scanProgress * h
        drawLine(
            color = primaryColor.copy(alpha = 0.25f),
            start = Offset(0f, scanY),
            end = Offset(w, scanY),
            strokeWidth = 2f
        )
        // Subtle glow band around the scan bar
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    primaryColor.copy(alpha = 0.08f),
                    primaryColor.copy(alpha = 0.08f),
                    Color.Transparent
                ),
                startY = (scanY - 12f).coerceAtLeast(0f),
                endY = (scanY + 12f).coerceAtMost(h)
            ),
            topLeft = Offset(0f, (scanY - 12f).coerceAtLeast(0f)),
            size = androidx.compose.ui.geometry.Size(
                w,
                ((scanY + 12f).coerceAtMost(h) - (scanY - 12f).coerceAtLeast(0f))
            )
        )
    }
}
