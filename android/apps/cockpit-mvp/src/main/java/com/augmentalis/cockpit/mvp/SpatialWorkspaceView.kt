package com.augmentalis.cockpit.mvp

import android.graphics.Canvas
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.avanues.cockpit.core.window.AppWindow
import com.avanues.cockpit.core.workspace.Vector3D
import com.avanues.cockpit.layout.LayoutPreset
import com.augmentalis.cockpit.mvp.rendering.SpatialWindowRenderer
import kotlin.math.abs

/**
 * Spatial workspace view with curved 3D rendering and gesture support
 *
 * Features:
 * - 3D curved window projection
 * - Depth sorting and perspective scaling
 * - Atmospheric fading
 * - Support for any LayoutPreset (Linear, Arc, Theater)
 * - Consistent glassmorphic styling from Phase 1
 * - Gesture support: Swipe to cycle layouts, pinch to scale windows
 *
 * Gestures:
 * - Horizontal swipe: Cycle layouts (left = next, right = previous)
 * - Pinch: Scale all windows (min 0.5x, max 2.0x)
 * - Tap close button: Remove window
 *
 * Gesture Priority:
 * 1. Scale gesture (pinch) - highest priority
 * 2. Close button tap - medium priority
 * 3. Fling gesture (swipe) - lowest priority
 *
 * Haptic Feedback:
 * - Light tap: Close button
 * - Gesture feedback: Swipe layout change, pinch scale
 */
@Composable
fun SpatialWorkspaceView(
    windows: List<AppWindow>,
    layoutPreset: LayoutPreset,
    windowColors: Map<String, String> = emptyMap(),
    selectedWindowId: String? = null,
    onRemoveWindow: (String) -> Unit = {},
    onCycleLayoutPreset: (forward: Boolean) -> Unit = {},
    onWindowHover: (String?) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val hapticManager = remember { HapticFeedbackManager(context) }

    // Scale factor state for pinch-to-zoom
    var scaleFactor by remember { mutableStateOf(1.0f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        OceanTheme.backgroundStart,
                        OceanTheme.backgroundEnd
                    )
                )
            )
    ) {
        if (windows.isEmpty()) {
            // Empty state
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(OceanTheme.spacingXLarge),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "No Windows (Spatial Mode)",
                    style = MaterialTheme.typography.headlineMedium,
                    color = OceanTheme.textTertiary
                )
                Spacer(modifier = Modifier.height(OceanTheme.spacingSmall))
                Text(
                    text = "Tap + to add a curved window",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OceanTheme.textDisabled
                )
            }
        } else {
            // Spatial rendering with Canvas
            val renderer = remember(layoutPreset) {
                SpatialWindowRenderer(layoutPreset)
            }

            AndroidView(
                factory = { context ->
                    object : View(context) {
                        // Gesture detectors
                        private val gestureDetector = GestureDetector(
                            context,
                            object : GestureDetector.SimpleOnGestureListener() {
                                override fun onFling(
                                    e1: MotionEvent?,
                                    e2: MotionEvent,
                                    velocityX: Float,
                                    velocityY: Float
                                ): Boolean {
                                    // Check minimum velocity threshold
                                    if (abs(velocityX) > 100 && abs(velocityX) > abs(velocityY)) {
                                        // Haptic feedback for layout cycle
                                        hapticManager.performGesture()

                                        if (velocityX > 0) {
                                            // Swipe right: previous layout
                                            onCycleLayoutPreset(false)
                                        } else {
                                            // Swipe left: next layout
                                            onCycleLayoutPreset(true)
                                        }
                                        return true
                                    }
                                    return false
                                }
                            }
                        )

                        private val scaleGestureDetector = ScaleGestureDetector(
                            context,
                            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                                private var lastScaleFactor = 1.0f

                                override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                                    lastScaleFactor = scaleFactor
                                    return super.onScaleBegin(detector)
                                }

                                override fun onScale(detector: ScaleGestureDetector): Boolean {
                                    // Update scale factor
                                    scaleFactor *= detector.scaleFactor
                                    // Clamp to min/max bounds
                                    scaleFactor = scaleFactor.coerceIn(0.5f, 2.0f)

                                    // Haptic feedback on significant scale change (every 10% change)
                                    if (abs(scaleFactor - lastScaleFactor) >= 0.1f) {
                                        hapticManager.performGesture()
                                        lastScaleFactor = scaleFactor
                                    }

                                    // Trigger redraw
                                    invalidate()
                                    return true
                                }
                            }
                        )

                        override fun onDraw(canvas: Canvas) {
                            super.onDraw(canvas)

                            // Render all windows with curved projection, selected window highlight, and scale factor
                            renderer.render(
                                canvas = canvas,
                                windows = windows,
                                windowColors = windowColors,
                                selectedWindowId = selectedWindowId,
                                centerPoint = Vector3D(0f, 0f, -2f),
                                scaleFactor = scaleFactor
                            )
                        }

                        override fun onTouchEvent(event: MotionEvent): Boolean {
                            // Priority 1: Scale gesture (pinch) - handle first
                            scaleGestureDetector.onTouchEvent(event)
                            if (scaleGestureDetector.isInProgress) {
                                return true
                            }

                            // Check for window hover (for head cursor or touch move)
                            if (event.action == MotionEvent.ACTION_MOVE || event.action == MotionEvent.ACTION_DOWN) {
                                val touchX = event.x
                                val touchY = event.y

                                // Check which window is being hovered
                                val hoveredWindowId = renderer.checkWindowHit(touchX, touchY)
                                onWindowHover(hoveredWindowId)
                            }

                            // Priority 2: Close button tap - check on ACTION_UP
                            if (event.action == MotionEvent.ACTION_UP) {
                                val touchX = event.x
                                val touchY = event.y

                                // Clear hover state
                                onWindowHover(null)

                                // Check if touch hit any window's close button
                                val hitWindowId = renderer.checkCloseButtonHit(
                                    touchX = touchX,
                                    touchY = touchY,
                                    windows = windows,
                                    screenWidth = width,
                                    screenHeight = height
                                )

                                if (hitWindowId != null) {
                                    // Haptic feedback for close button tap
                                    hapticManager.performLightTap()
                                    onRemoveWindow(hitWindowId)
                                    invalidate()
                                    return true
                                }
                            }

                            // Priority 3: Fling gesture (swipe) - handle last
                            gestureDetector.onTouchEvent(event)
                            return true
                        }
                    }.apply {
                        // Enable alpha for transparency
                        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                        // Enable touch events
                        isClickable = true
                    }
                },
                update = { view ->
                    // Trigger redraw when windows or scale factor change
                    view.invalidate()
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * Get window color for visual differentiation
 * Uses color cycling for multiple windows
 */
fun getSpatialWindowColor(windowId: String, index: Int): String {
    val colors = listOf(
        "#FF6B9D", // Pink
        "#4ECDC4", // Teal
        "#95E1D3", // Mint
        "#FFD93D", // Yellow
        "#FF8B94", // Coral
        "#A8E6CF"  // Light green
    )
    return colors[index % colors.size]
}
