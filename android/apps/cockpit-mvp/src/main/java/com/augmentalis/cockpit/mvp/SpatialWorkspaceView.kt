package com.augmentalis.cockpit.mvp

import android.graphics.Canvas
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.avanues.cockpit.core.window.AppWindow
import com.avanues.cockpit.core.workspace.Vector3D
import com.avanues.cockpit.layout.presets.LayoutPreset
import com.augmentalis.cockpit.mvp.rendering.SpatialWindowRenderer
import com.augmentalis.cockpit.mvp.rendering.SpatialWebViewManager
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

            // WebView manager for off-screen content rendering
            val webViewManager = remember { SpatialWebViewManager(context) }

            // Update WebViews when windows change
            LaunchedEffect(windows, windowColors) {
                webViewManager.updateWindows(windows, windowColors)
            }

            // Cleanup on dispose
            DisposableEffect(Unit) {
                onDispose {
                    webViewManager.destroy()
                }
            }

            AndroidView(
                factory = { context ->
                    // Create container for hidden WebViews + Canvas view
                    android.widget.FrameLayout(context).apply {
                        // Add hidden WebView container (invisible but measured)
                        addView(webViewManager.getContainer())

                        // Add Canvas view for spatial rendering on top
                        addView(object : View(context) {
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

                            // Render all windows with curved projection, WebView bitmaps, and scale factor
                            renderer.render(
                                canvas = canvas,
                                windows = windows,
                                windowColors = windowColors,
                                selectedWindowId = selectedWindowId,
                                centerPoint = Vector3D(0f, 0f, -2f),
                                scaleFactor = scaleFactor,
                                bitmapProvider = { windowId -> webViewManager.getBitmap(windowId) }
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
                        })
                    }
                },
                update = { view ->
                    // Trigger redraw when windows or scale factor change
                    (view as? android.widget.FrameLayout)?.getChildAt(1)?.invalidate()
                },
                modifier = Modifier.fillMaxSize()
            )

            // Subtle caustic water effect overlay (XR atmosphere)
            CausticOverlay(
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.Center)
            )
        }
    }
}

/**
 * Get window color for visual differentiation
 * Uses color cycling for multiple windows
 */
fun getSpatialWindowColor(windowId: String, index: Int): String {
    // OCEAN BLUE PROFESSIONAL PALETTE (replaces cartoonish bright colors)
    // Deep ocean depths for professional XR appearance
    val colors = listOf(
        "#2D5F7F", // Deep bioluminescent blue - calm authority
        "#2A6B6A", // Muted teal glow - ocean depth
        "#3A7B8A", // Subdued aqua - water reflection
        "#4A90B8", // Frosted blue - ice clarity
        "#8A9BA8", // Chrome mid - metallic professionalism
        "#4A5A6A"  // Chrome dark - steel authority
    )
    return colors[index % colors.size]
}

/**
 * Subtle caustic water effect overlay
 *
 * Creates "underwater" atmosphere with slow animated ripples
 * Uses compose animation for performance (60 FPS)
 */
@Composable
fun CausticOverlay(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "caustic_transition")

    val causticPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 8000,  // 8 second cycle
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "caustic_phase"
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        // Draw subtle animated caustic pattern
        // Simple implementation: Sinusoidal wave overlay
        val paint = Paint().asFrameworkPaint().apply {
            color = android.graphics.Color.parseColor("#0A60A5FA")  // 4% ocean blue
            style = android.graphics.Paint.Style.FILL
        }

        // Draw 3 horizontal wave bands at different phases
        for (i in 0..2) {
            val yOffset = (height / 3f) * i
            val phaseShift = (causticPhase + (i * 0.33f)) % 1f

            val path = android.graphics.Path().apply {
                moveTo(0f, yOffset)

                // Sinusoidal wave
                val wavelength = width / 4f
                for (x in 0..width.toInt() step 10) {
                    val y = yOffset +
                            kotlin.math.sin((x / wavelength + phaseShift) *
                            2f * kotlin.math.PI.toFloat()) * 20f
                    lineTo(x.toFloat(), y)
                }

                lineTo(width, yOffset + height)
                lineTo(0f, yOffset + height)
                close()
            }

            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawPath(path, paint)
            }
        }
    }
}
