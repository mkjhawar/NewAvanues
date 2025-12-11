/**
 * FocusIndicator.kt - Visual focus indicator for cursor and element highlighting
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-10-09
 */
package com.augmentalis.voiceoscore.accessibility.cursor

import android.content.Context
import android.graphics.*
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.cos
import kotlin.math.sin

/**
 * Focus state types
 */
enum class FocusState(
    val color: Int,
    val strokeWidth: Float,
    val description: String
) {
    FOCUSED(
        color = Color.parseColor("#4CAF50"),  // Green
        strokeWidth = 8f,
        description = "Element is focused"
    ),
    SELECTED(
        color = Color.parseColor("#2196F3"),  // Blue
        strokeWidth = 10f,
        description = "Element is selected"
    ),
    HOVER(
        color = Color.parseColor("#FFC107"),  // Amber
        strokeWidth = 6f,
        description = "Cursor hovering over element"
    ),
    ERROR(
        color = Color.parseColor("#F44336"),  // Red
        strokeWidth = 8f,
        description = "Error or invalid action"
    ),
    DISABLED(
        color = Color.parseColor("#9E9E9E"),  // Gray
        strokeWidth = 6f,
        description = "Element is disabled"
    );
}

/**
 * Animation style for focus indicator
 */
enum class AnimationStyle {
    STATIC,        // No animation
    PULSE,         // Pulsing size
    ROTATE,        // Rotating ring
    PULSE_ROTATE,  // Both pulsing and rotating
    FADE,          // Fading in/out
    BREATHE        // Smooth breathing effect
}

/**
 * Focus indicator bounds and properties
 */
data class FocusIndicatorConfig(
    val bounds: RectF,
    val state: FocusState = FocusState.FOCUSED,
    val animationStyle: AnimationStyle = AnimationStyle.PULSE,
    val duration: Int = 1000,  // Animation duration in milliseconds
    val cornerRadius: Float = 8f
)

/**
 * Visual focus indicator for highlighting elements
 *
 * Features:
 * - Colored ring highlighting around elements
 * - Multiple animation styles (pulse, rotate, fade)
 * - Multi-state support (focused, selected, error)
 * - Overlay-based rendering (TYPE_ACCESSIBILITY_OVERLAY)
 * - Compose-based rendering for smooth animations
 *
 * Usage:
 * ```
 * val indicator = FocusIndicator(context, windowManager)
 * val config = FocusIndicatorConfig(
 *     bounds = RectF(100f, 100f, 300f, 300f),
 *     state = FocusState.FOCUSED,
 *     animationStyle = AnimationStyle.PULSE
 * )
 * indicator.show(config)
 * indicator.hide()
 * ```
 */
/**
 * Custom LifecycleOwner for FocusIndicator ComposeView
 */
private class FocusIndicatorLifecycleOwner : LifecycleOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    fun onCreate() {
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }
}

class FocusIndicator(
    private val context: Context,
    private val windowManager: WindowManager
) {

    companion object {
        private const val TAG = "FocusIndicator"

        // Default animation parameters
        private const val DEFAULT_DURATION = 1000
        private const val DEFAULT_CORNER_RADIUS = 8f
        const val PULSE_MIN_SCALE = 0.9f
        const val PULSE_MAX_SCALE = 1.1f
    }

    // Overlay view
    private var overlayView: ComposeView? = null

    // Lifecycle owner for ComposeView
    private var lifecycleOwner: FocusIndicatorLifecycleOwner? = null

    // Current configuration
    private var currentConfig: FocusIndicatorConfig? = null

    // Visibility state
    private var isVisible: Boolean = false

    /**
     * Show focus indicator with configuration
     *
     * @param config Focus indicator configuration
     */
    fun show(config: FocusIndicatorConfig) {
        Log.d(TAG, "Showing focus indicator: ${config.state.description}")
        currentConfig = config

        if (overlayView == null) {
            createOverlayView()
        }

        isVisible = true
        updateOverlayContent()
    }

    /**
     * Update focus indicator configuration without recreating overlay
     *
     * @param config New configuration
     */
    fun update(config: FocusIndicatorConfig) {
        if (!isVisible) {
            Log.w(TAG, "Cannot update: indicator not visible")
            return
        }

        currentConfig = config
        updateOverlayContent()
    }

    /**
     * Hide focus indicator
     */
    fun hide() {
        Log.d(TAG, "Hiding focus indicator")
        isVisible = false
        removeOverlayView()
    }

    /**
     * Check if indicator is visible
     */
    fun isVisible(): Boolean = isVisible

    /**
     * Create overlay view for rendering
     */
    private fun createOverlayView() {
        Log.d(TAG, "Creating focus indicator overlay")

        // Create lifecycle owner
        val owner = FocusIndicatorLifecycleOwner().also {
            lifecycleOwner = it
            it.onCreate()
        }

        val composeView = ComposeView(context).apply {
            // Set ViewTreeLifecycleOwner and ViewTreeSavedStateRegistryOwner
            setViewTreeLifecycleOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)

            setContent {
                currentConfig?.let { config ->
                    FocusIndicatorContent(config)
                }
            }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )

        try {
            windowManager.addView(composeView, params)
            overlayView = composeView
            Log.d(TAG, "Focus indicator overlay created successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error creating focus indicator overlay", e)
        }
    }

    /**
     * Update overlay content with current configuration
     */
    private fun updateOverlayContent() {
        overlayView?.setContent {
            currentConfig?.let { config ->
                FocusIndicatorContent(config)
            }
        }
    }

    /**
     * Remove overlay view
     */
    private fun removeOverlayView() {
        overlayView?.let { view ->
            try {
                windowManager.removeView(view)
                Log.d(TAG, "Focus indicator overlay removed")
            } catch (e: Exception) {
                Log.e(TAG, "Error removing focus indicator overlay", e)
            }
        }
        overlayView = null
        currentConfig = null
    }

    /**
     * Dispose and cleanup resources
     */
    fun dispose() {
        hide()
        lifecycleOwner?.onDestroy()
        lifecycleOwner = null
        Log.d(TAG, "FocusIndicator disposed")
    }
}

/**
 * Composable content for focus indicator
 */
@Composable
private fun FocusIndicatorContent(config: FocusIndicatorConfig) {
    when (config.animationStyle) {
        AnimationStyle.STATIC -> StaticFocusRing(config)
        AnimationStyle.PULSE -> PulsingFocusRing(config)
        AnimationStyle.ROTATE -> RotatingFocusRing(config)
        AnimationStyle.PULSE_ROTATE -> PulseRotateFocusRing(config)
        AnimationStyle.FADE -> FadingFocusRing(config)
        AnimationStyle.BREATHE -> BreathingFocusRing(config)
    }
}

/**
 * Static focus ring (no animation)
 */
@Composable
private fun StaticFocusRing(config: FocusIndicatorConfig) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRoundRect(
            color = androidx.compose.ui.graphics.Color(config.state.color),
            topLeft = androidx.compose.ui.geometry.Offset(config.bounds.left, config.bounds.top),
            size = androidx.compose.ui.geometry.Size(config.bounds.width(), config.bounds.height()),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(config.cornerRadius),
            style = Stroke(width = config.state.strokeWidth)
        )
    }
}

/**
 * Pulsing focus ring
 */
@Composable
private fun PulsingFocusRing(config: FocusIndicatorConfig) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = FocusIndicator.PULSE_MIN_SCALE,
        targetValue = FocusIndicator.PULSE_MAX_SCALE,
        animationSpec = infiniteRepeatable(
            animation = tween(config.duration / 2, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val centerX = config.bounds.centerX()
        val centerY = config.bounds.centerY()
        val width = config.bounds.width() * scale
        val height = config.bounds.height() * scale

        drawRoundRect(
            color = androidx.compose.ui.graphics.Color(config.state.color),
            topLeft = androidx.compose.ui.geometry.Offset(
                centerX - width / 2,
                centerY - height / 2
            ),
            size = androidx.compose.ui.geometry.Size(width, height),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(config.cornerRadius),
            style = Stroke(width = config.state.strokeWidth)
        )
    }
}

/**
 * Rotating focus ring (uses pulsing instead of rotation for simplicity)
 */
@Composable
private fun RotatingFocusRing(config: FocusIndicatorConfig) {
    // Use pulsing animation for now (rotation requires additional graphics transforms)
    PulsingFocusRing(config)
}

/**
 * Pulsing and rotating focus ring (uses pulsing only for simplicity)
 */
@Composable
private fun PulseRotateFocusRing(config: FocusIndicatorConfig) {
    // Use pulsing animation for now (rotation requires additional graphics transforms)
    PulsingFocusRing(config)
}

/**
 * Fading focus ring
 */
@Composable
private fun FadingFocusRing(config: FocusIndicatorConfig) {
    val infiniteTransition = rememberInfiniteTransition(label = "fade")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(config.duration / 2, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRoundRect(
            color = androidx.compose.ui.graphics.Color(config.state.color).copy(alpha = alpha),
            topLeft = androidx.compose.ui.geometry.Offset(config.bounds.left, config.bounds.top),
            size = androidx.compose.ui.geometry.Size(config.bounds.width(), config.bounds.height()),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(config.cornerRadius),
            style = Stroke(width = config.state.strokeWidth)
        )
    }
}

/**
 * Breathing focus ring (smooth sine wave scale)
 */
@Composable
private fun BreathingFocusRing(config: FocusIndicatorConfig) {
    val infiniteTransition = rememberInfiniteTransition(label = "breathe")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(config.duration, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )

    // Apply sine wave for smooth breathing
    val scale = FocusIndicator.PULSE_MIN_SCALE +
        (FocusIndicator.PULSE_MAX_SCALE - FocusIndicator.PULSE_MIN_SCALE) *
        (0.5f + 0.5f * sin(progress * 2 * Math.PI).toFloat())

    Canvas(modifier = Modifier.fillMaxSize()) {
        val centerX = config.bounds.centerX()
        val centerY = config.bounds.centerY()
        val width = config.bounds.width() * scale
        val height = config.bounds.height() * scale

        drawRoundRect(
            color = androidx.compose.ui.graphics.Color(config.state.color),
            topLeft = androidx.compose.ui.geometry.Offset(
                centerX - width / 2,
                centerY - height / 2
            ),
            size = androidx.compose.ui.geometry.Size(width, height),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(config.cornerRadius),
            style = Stroke(width = config.state.strokeWidth)
        )
    }
}
