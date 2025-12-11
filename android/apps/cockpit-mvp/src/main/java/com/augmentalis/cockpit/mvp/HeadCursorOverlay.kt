package com.augmentalis.cockpit.mvp

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest

/**
 * Head-based cursor overlay using IMU data from DeviceManager
 * Shows a circular cursor that follows head movements
 *
 * NOTE: Temporarily disabled - requires DeviceManager module integration
 */
@Composable
fun HeadCursorOverlay(
    isEnabled: Boolean,
    onCursorPositionChange: (x: Float, y: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isEnabled) return

    val density = LocalDensity.current

    var cursorX by remember { mutableFloatStateOf(0.5f) }
    var cursorY by remember { mutableFloatStateOf(0.5f) }

    // Dwell time for selection (when cursor stays on target)
    var dwellProgress by remember { mutableFloatStateOf(0f) }

    // Pulse animation for cursor
    val infiniteTransition = rememberInfiniteTransition(label = "cursor_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursor_alpha"
    )

    // TODO: Integrate with DeviceManager CursorAdapter when available
    // For now, use center position

    // Draw cursor overlay
    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cursorRadius = with(density) { 24.dp.toPx() }
            val innerRadius = with(density) { 8.dp.toPx() }

            // Outer ring (pulsing)
            drawCircle(
                color = OceanTheme.primary.copy(alpha = pulseAlpha),
                radius = cursorRadius,
                center = Offset(cursorX, cursorY),
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            // Inner dot (solid)
            drawCircle(
                color = Color.White.copy(alpha = 0.9f),
                radius = innerRadius,
                center = Offset(cursorX, cursorY)
            )

            // Dwell progress ring (shows selection progress)
            if (dwellProgress > 0f) {
                drawArc(
                    color = OceanTheme.success,
                    startAngle = -90f,
                    sweepAngle = 360f * dwellProgress,
                    useCenter = false,
                    topLeft = Offset(
                        cursorX - cursorRadius - 8.dp.toPx(),
                        cursorY - cursorRadius - 8.dp.toPx()
                    ),
                    size = androidx.compose.ui.geometry.Size(
                        (cursorRadius + 8.dp.toPx()) * 2,
                        (cursorRadius + 8.dp.toPx()) * 2
                    ),
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }
    }
}

/**
 * Data class representing cursor position from IMU
 */
data class CursorPosition(
    val x: Float,
    val y: Float,
    val timestamp: Long = System.currentTimeMillis()
)
