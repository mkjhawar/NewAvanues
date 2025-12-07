/**
 * CursorStyleManager.kt - Manages cursor visual styles and animations
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-10-09
 */
package com.augmentalis.voiceoscore.accessibility.cursor

import android.graphics.Color
import android.util.Log
import androidx.compose.ui.graphics.Color as ComposeColor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Cursor style sealed class defining all available cursor styles
 */
sealed class CursorStyle {
    /**
     * Normal cursor - default state
     */
    data class Normal(
        val color: ComposeColor = DEFAULT_COLOR,
        val size: Float = DEFAULT_SIZE,
        val strokeWidth: Float = DEFAULT_STROKE_WIDTH
    ) : CursorStyle()

    /**
     * Selection cursor - when selecting UI elements
     */
    data class Selection(
        val color: ComposeColor = SELECTION_COLOR,
        val size: Float = DEFAULT_SIZE * 1.2f,
        val strokeWidth: Float = DEFAULT_STROKE_WIDTH * 1.5f,
        val pulseEnabled: Boolean = true
    ) : CursorStyle()

    /**
     * Click cursor - during click action
     */
    data class Click(
        val color: ComposeColor = CLICK_COLOR,
        val size: Float = DEFAULT_SIZE * 1.5f,
        val strokeWidth: Float = DEFAULT_STROKE_WIDTH * 2f,
        val pulseSpeed: Float = 1.5f
    ) : CursorStyle()

    /**
     * Loading cursor - during processing/waiting
     */
    data class Loading(
        val color: ComposeColor = LOADING_COLOR,
        val size: Float = DEFAULT_SIZE,
        val strokeWidth: Float = DEFAULT_STROKE_WIDTH,
        val spinSpeed: Float = 1f
    ) : CursorStyle()

    /**
     * Disabled cursor - when cursor cannot interact
     */
    data class Disabled(
        val color: ComposeColor = DISABLED_COLOR,
        val size: Float = DEFAULT_SIZE * 0.8f,
        val strokeWidth: Float = DEFAULT_STROKE_WIDTH * 0.5f,
        val alpha: Float = 0.5f
    ) : CursorStyle()

    /**
     * Custom cursor - for application-specific styles
     */
    data class Custom(
        val color: ComposeColor,
        val size: Float,
        val strokeWidth: Float,
        val animationType: AnimationType = AnimationType.NONE,
        val alpha: Float = 1f
    ) : CursorStyle()

    companion object {
        // Default style parameters
        const val DEFAULT_SIZE = 48f // dp
        const val DEFAULT_STROKE_WIDTH = 3f // dp

        // Default colors
        val DEFAULT_COLOR = ComposeColor(0xFF2196F3) // Blue
        val SELECTION_COLOR = ComposeColor(0xFF4CAF50) // Green
        val CLICK_COLOR = ComposeColor(0xFFFF9800) // Orange
        val LOADING_COLOR = ComposeColor(0xFF9C27B0) // Purple
        val DISABLED_COLOR = ComposeColor(0xFF757575) // Gray
    }

    /**
     * Get color for this style
     */
    fun styleColor(): ComposeColor = when (this) {
        is Normal -> color
        is Selection -> color
        is Click -> color
        is Loading -> color
        is Disabled -> color
        is Custom -> color
    }

    /**
     * Get size for this style
     */
    fun styleSize(): Float = when (this) {
        is Normal -> size
        is Selection -> size
        is Click -> size
        is Loading -> size
        is Disabled -> size
        is Custom -> size
    }

    /**
     * Get stroke width for this style
     */
    fun styleStrokeWidth(): Float = when (this) {
        is Normal -> strokeWidth
        is Selection -> strokeWidth
        is Click -> strokeWidth
        is Loading -> strokeWidth
        is Disabled -> strokeWidth
        is Custom -> strokeWidth
    }

    /**
     * Get alpha for this style
     */
    fun styleAlpha(): Float = when (this) {
        is Disabled -> alpha
        is Custom -> alpha
        else -> 1f
    }

    /**
     * Get animation type for this style
     */
    fun styleAnimationType(): AnimationType = when (this) {
        is Selection -> if (pulseEnabled) AnimationType.PULSE else AnimationType.NONE
        is Click -> AnimationType.PULSE
        is Loading -> AnimationType.SPIN
        is Custom -> animationType
        else -> AnimationType.NONE
    }
}

/**
 * Animation types for cursor
 */
enum class AnimationType {
    NONE,       // No animation
    PULSE,      // Pulsing scale animation
    SPIN,       // Rotating animation
    BOUNCE      // Bouncing animation
}

/**
 * Cursor shape enum
 */
enum class CursorShape {
    CIRCLE,     // Circular cursor
    CROSSHAIR,  // Crosshair cursor
    POINTER,    // Arrow pointer
    HAND,       // Hand icon
    CUSTOM      // Custom drawable
}

/**
 * Style configuration
 *
 * @param shape Base cursor shape
 * @param enableAnimations Whether animations are enabled
 * @param animationDuration Duration of animations in ms
 */
data class StyleConfig(
    val shape: CursorShape = CursorShape.CIRCLE,
    val enableAnimations: Boolean = true,
    val animationDuration: Long = 300L
)

/**
 * Cursor style manager
 *
 * Manages cursor visual appearance with:
 * - Multiple predefined styles (normal, selection, click, loading, disabled)
 * - Custom cursor graphics via Compose
 * - State-based styling
 * - Animation support (pulse, spin, bounce)
 */
class CursorStyleManager(
    private val config: StyleConfig = StyleConfig()
) {
    companion object {
        private const val TAG = "CursorStyleManager"
    }

    // Current style state flow
    private val _styleFlow = MutableStateFlow<CursorStyle>(CursorStyle.Normal())
    val styleFlow: StateFlow<CursorStyle> = _styleFlow.asStateFlow()

    // Animation state flow
    private val _animationProgress = MutableStateFlow(0f)
    val animationProgress: StateFlow<Float> = _animationProgress.asStateFlow()

    // Style change callbacks
    private val styleCallbacks = mutableListOf<(CursorStyle) -> Unit>()

    init {
        Log.d(TAG, "CursorStyleManager initialized with config: $config")
    }

    /**
     * Get current cursor style
     */
    fun getCurrentStyle(): CursorStyle = _styleFlow.value

    /**
     * Set cursor style
     *
     * @param style New cursor style
     */
    fun setStyle(style: CursorStyle) {
        val oldStyle = _styleFlow.value

        if (oldStyle != style) {
            _styleFlow.value = style

            Log.d(TAG, "Cursor style changed: ${oldStyle::class.simpleName} -> ${style::class.simpleName}")

            // Notify callbacks
            styleCallbacks.forEach { callback ->
                try {
                    callback(style)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in style callback", e)
                }
            }
        }
    }

    /**
     * Set normal style
     */
    fun setNormal() {
        setStyle(CursorStyle.Normal())
    }

    /**
     * Set selection style
     */
    fun setSelection() {
        setStyle(CursorStyle.Selection())
    }

    /**
     * Set click style
     */
    fun setClick() {
        setStyle(CursorStyle.Click())
    }

    /**
     * Set loading style
     */
    fun setLoading() {
        setStyle(CursorStyle.Loading())
    }

    /**
     * Set disabled style
     */
    fun setDisabled() {
        setStyle(CursorStyle.Disabled())
    }

    /**
     * Set custom style
     *
     * @param color Cursor color
     * @param size Cursor size in dp
     * @param strokeWidth Stroke width in dp
     * @param animationType Animation type
     * @param alpha Alpha value (0-1)
     */
    fun setCustom(
        color: ComposeColor,
        size: Float = CursorStyle.DEFAULT_SIZE,
        strokeWidth: Float = CursorStyle.DEFAULT_STROKE_WIDTH,
        animationType: AnimationType = AnimationType.NONE,
        alpha: Float = 1f
    ) {
        setStyle(
            CursorStyle.Custom(
                color = color,
                size = size,
                strokeWidth = strokeWidth,
                animationType = animationType,
                alpha = alpha
            )
        )
    }

    /**
     * Get current cursor color
     */
    fun getCurrentColor(): ComposeColor = _styleFlow.value.styleColor()

    /**
     * Get current cursor size
     */
    fun getCurrentSize(): Float = _styleFlow.value.styleSize()

    /**
     * Get current stroke width
     */
    fun getCurrentStrokeWidth(): Float = _styleFlow.value.styleStrokeWidth()

    /**
     * Get current alpha
     */
    fun getCurrentAlpha(): Float = _styleFlow.value.styleAlpha()

    /**
     * Get current animation type
     */
    fun getCurrentAnimationType(): AnimationType = _styleFlow.value.styleAnimationType()

    /**
     * Check if animations are enabled
     */
    fun areAnimationsEnabled(): Boolean = config.enableAnimations

    /**
     * Get animation duration
     */
    fun getAnimationDuration(): Long = config.animationDuration

    /**
     * Update animation progress (0-1)
     *
     * @param progress Animation progress value
     */
    fun updateAnimationProgress(progress: Float) {
        _animationProgress.value = progress.coerceIn(0f, 1f)
    }

    /**
     * Get animation progress
     */
    fun getAnimationProgress(): Float = _animationProgress.value

    /**
     * Add style change callback
     *
     * @param callback Function to call when style changes
     */
    fun addStyleCallback(callback: (CursorStyle) -> Unit) {
        styleCallbacks.add(callback)
        Log.d(TAG, "Style callback registered (total: ${styleCallbacks.size})")
    }

    /**
     * Remove style change callback
     */
    fun removeStyleCallback(callback: (CursorStyle) -> Unit) {
        styleCallbacks.remove(callback)
        Log.d(TAG, "Style callback unregistered (total: ${styleCallbacks.size})")
    }

    /**
     * Clear all callbacks
     */
    fun clearCallbacks() {
        styleCallbacks.clear()
        Log.d(TAG, "All style callbacks cleared")
    }

    /**
     * Get cursor shape
     */
    fun getCursorShape(): CursorShape = config.shape

    /**
     * Helper: Convert Android Color to Compose Color
     */
    fun colorFromInt(colorInt: Int): ComposeColor {
        return ComposeColor(
            red = Color.red(colorInt) / 255f,
            green = Color.green(colorInt) / 255f,
            blue = Color.blue(colorInt) / 255f,
            alpha = Color.alpha(colorInt) / 255f
        )
    }

    /**
     * Helper: Convert Compose Color to Android Color Int
     */
    fun colorToInt(color: ComposeColor): Int {
        return Color.argb(
            (color.alpha * 255).toInt(),
            (color.red * 255).toInt(),
            (color.green * 255).toInt(),
            (color.blue * 255).toInt()
        )
    }

    /**
     * Cleanup resources
     */
    fun dispose() {
        clearCallbacks()
        Log.d(TAG, "CursorStyleManager disposed")
    }
}
