/**
 * CursorVisibilityManager.kt - Manages cursor visibility with animations
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-10-09
 */
package com.augmentalis.voiceoscore.accessibility.cursor

import android.animation.ValueAnimator
import android.util.Log
import android.view.animation.DecelerateInterpolator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Cursor visibility state
 */
enum class VisibilityState {
    VISIBLE,      // Fully visible (alpha = 1.0)
    HIDDEN,       // Fully hidden (alpha = 0.0)
    FADING_IN,    // Transitioning to visible
    FADING_OUT    // Transitioning to hidden
}

/**
 * Interaction mode determining cursor behavior
 */
enum class InteractionMode {
    VOICE,        // Voice commands only - cursor always visible
    TOUCH,        // Touch input only - cursor auto-hides
    HYBRID        // Voice + touch - cursor visible during voice, auto-hides after
}

/**
 * Visibility configuration
 *
 * @param autoHideDuration Duration before auto-hide (ms), 0 = disabled
 * @param fadeDuration Duration of fade animation (ms)
 * @param interactionMode Current interaction mode
 */
data class VisibilityConfig(
    val autoHideDuration: Long = DEFAULT_AUTO_HIDE_DURATION,
    val fadeDuration: Long = DEFAULT_FADE_DURATION,
    val interactionMode: InteractionMode = InteractionMode.HYBRID
) {
    companion object {
        const val DEFAULT_AUTO_HIDE_DURATION = 5000L // 5 seconds
        const val DEFAULT_FADE_DURATION = 300L // 300ms
        const val NO_AUTO_HIDE = 0L
    }

    /**
     * Check if auto-hide is enabled
     */
    fun isAutoHideEnabled(): Boolean = autoHideDuration > 0

    /**
     * Should cursor always be visible for this mode?
     */
    fun shouldAlwaysShow(): Boolean = interactionMode == InteractionMode.VOICE
}

/**
 * Cursor visibility manager
 *
 * Manages cursor visibility with:
 * - Smooth fade in/out animations (300ms default)
 * - Mode-based visibility control
 * - Auto-hide timer (configurable, default 5s)
 * - State management via StateFlow
 */
class CursorVisibilityManager(
    private val config: VisibilityConfig = VisibilityConfig()
) {
    companion object {
        private const val TAG = "CursorVisibilityManager"
        private const val MIN_ALPHA = 0f
        private const val MAX_ALPHA = 1f
    }

    // Coroutine scope for async operations
    private val scope = CoroutineScope(Dispatchers.Main)

    // Visibility state flow
    private val _stateFlow = MutableStateFlow(VisibilityState.HIDDEN)
    val stateFlow: StateFlow<VisibilityState> = _stateFlow.asStateFlow()

    // Alpha value flow (for smooth animations)
    private val _alphaFlow = MutableStateFlow(MIN_ALPHA)
    val alphaFlow: StateFlow<Float> = _alphaFlow.asStateFlow()

    // Auto-hide timer job
    private var autoHideJob: Job? = null

    // Current fade animator
    private var fadeAnimator: ValueAnimator? = null

    // Visibility change callbacks
    private val visibilityCallbacks = mutableListOf<(VisibilityState) -> Unit>()
    private val alphaCallbacks = mutableListOf<(Float) -> Unit>()

    init {
        Log.d(TAG, "CursorVisibilityManager initialized with config: $config")

        // If voice mode, show cursor immediately
        if (config.shouldAlwaysShow()) {
            show(animated = false)
        }
    }

    /**
     * Get current visibility state
     */
    fun getState(): VisibilityState = _stateFlow.value

    /**
     * Get current alpha value
     */
    fun getAlpha(): Float = _alphaFlow.value

    /**
     * Check if cursor is visible
     */
    fun isVisible(): Boolean = _stateFlow.value == VisibilityState.VISIBLE ||
            _stateFlow.value == VisibilityState.FADING_IN

    /**
     * Check if cursor is hidden
     */
    fun isHidden(): Boolean = _stateFlow.value == VisibilityState.HIDDEN ||
            _stateFlow.value == VisibilityState.FADING_OUT

    /**
     * Show cursor with optional animation
     *
     * @param animated Whether to animate the transition (default: true)
     */
    fun show(animated: Boolean = true) {
        // Cancel any pending auto-hide
        cancelAutoHide()

        // Cancel any ongoing fade animation
        fadeAnimator?.cancel()

        val currentAlpha = _alphaFlow.value

        if (animated && currentAlpha < MAX_ALPHA) {
            // Start fade in animation
            updateState(VisibilityState.FADING_IN)
            fadeIn(from = currentAlpha)
        } else {
            // Immediate show
            updateState(VisibilityState.VISIBLE)
            updateAlpha(MAX_ALPHA)
        }

        Log.d(TAG, "Cursor shown (animated=$animated)")

        // Start auto-hide timer if enabled and not in voice-only mode
        if (config.isAutoHideEnabled() && !config.shouldAlwaysShow()) {
            startAutoHideTimer()
        }
    }

    /**
     * Hide cursor with optional animation
     *
     * @param animated Whether to animate the transition (default: true)
     */
    fun hide(animated: Boolean = true) {
        // Don't hide if in voice-only mode
        if (config.shouldAlwaysShow()) {
            Log.d(TAG, "Hide ignored - voice-only mode")
            return
        }

        // Cancel any pending auto-hide
        cancelAutoHide()

        // Cancel any ongoing fade animation
        fadeAnimator?.cancel()

        val currentAlpha = _alphaFlow.value

        if (animated && currentAlpha > MIN_ALPHA) {
            // Start fade out animation
            updateState(VisibilityState.FADING_OUT)
            fadeOut(from = currentAlpha)
        } else {
            // Immediate hide
            updateState(VisibilityState.HIDDEN)
            updateAlpha(MIN_ALPHA)
        }

        Log.d(TAG, "Cursor hidden (animated=$animated)")
    }

    /**
     * Toggle cursor visibility
     *
     * @param animated Whether to animate the transition (default: true)
     */
    fun toggle(animated: Boolean = true) {
        if (isVisible()) {
            hide(animated)
        } else {
            show(animated)
        }
    }

    /**
     * Reset auto-hide timer (extends cursor visibility)
     */
    fun resetAutoHideTimer() {
        if (config.isAutoHideEnabled() && !config.shouldAlwaysShow()) {
            cancelAutoHide()
            startAutoHideTimer()
            Log.v(TAG, "Auto-hide timer reset")
        }
    }

    /**
     * Add visibility state change callback
     *
     * @param callback Function to call when state changes
     */
    fun addVisibilityCallback(callback: (VisibilityState) -> Unit) {
        visibilityCallbacks.add(callback)
        Log.d(TAG, "Visibility callback registered (total: ${visibilityCallbacks.size})")
    }

    /**
     * Remove visibility state change callback
     */
    fun removeVisibilityCallback(callback: (VisibilityState) -> Unit) {
        visibilityCallbacks.remove(callback)
        Log.d(TAG, "Visibility callback unregistered (total: ${visibilityCallbacks.size})")
    }

    /**
     * Add alpha change callback
     *
     * @param callback Function to call when alpha changes
     */
    fun addAlphaCallback(callback: (Float) -> Unit) {
        alphaCallbacks.add(callback)
        Log.d(TAG, "Alpha callback registered (total: ${alphaCallbacks.size})")
    }

    /**
     * Remove alpha change callback
     */
    fun removeAlphaCallback(callback: (Float) -> Unit) {
        alphaCallbacks.remove(callback)
        Log.d(TAG, "Alpha callback unregistered (total: ${alphaCallbacks.size})")
    }

    /**
     * Clear all callbacks
     */
    fun clearCallbacks() {
        visibilityCallbacks.clear()
        alphaCallbacks.clear()
        Log.d(TAG, "All callbacks cleared")
    }

    /**
     * Update interaction mode
     *
     * @param mode New interaction mode
     */
    fun setInteractionMode(mode: InteractionMode) {
        Log.d(TAG, "Interaction mode changed: ${config.interactionMode} -> $mode")

        val newConfig = config.copy(interactionMode = mode)

        // If switching to voice-only mode, show cursor
        if (mode == InteractionMode.VOICE) {
            show(animated = true)
            cancelAutoHide()
        }

        // Update config would go here if it's mutable
        // For now, this is a placeholder for future implementation
    }

    /**
     * Start auto-hide timer
     */
    private fun startAutoHideTimer() {
        autoHideJob = scope.launch {
            delay(config.autoHideDuration)
            hide(animated = true)
            Log.d(TAG, "Auto-hide triggered after ${config.autoHideDuration}ms")
        }
    }

    /**
     * Cancel auto-hide timer
     */
    private fun cancelAutoHide() {
        autoHideJob?.cancel()
        autoHideJob = null
    }

    /**
     * Fade in animation
     *
     * @param from Starting alpha value
     */
    private fun fadeIn(from: Float) {
        fadeAnimator = ValueAnimator.ofFloat(from, MAX_ALPHA).apply {
            duration = config.fadeDuration
            interpolator = DecelerateInterpolator()

            addUpdateListener { animator ->
                val alpha = animator.animatedValue as Float
                updateAlpha(alpha)
            }

            doOnEnd {
                updateState(VisibilityState.VISIBLE)
                updateAlpha(MAX_ALPHA)
                Log.v(TAG, "Fade in complete")
            }

            start()
        }
    }

    /**
     * Fade out animation
     *
     * @param from Starting alpha value
     */
    private fun fadeOut(from: Float) {
        fadeAnimator = ValueAnimator.ofFloat(from, MIN_ALPHA).apply {
            duration = config.fadeDuration
            interpolator = DecelerateInterpolator()

            addUpdateListener { animator ->
                val alpha = animator.animatedValue as Float
                updateAlpha(alpha)
            }

            doOnEnd {
                updateState(VisibilityState.HIDDEN)
                updateAlpha(MIN_ALPHA)
                Log.v(TAG, "Fade out complete")
            }

            start()
        }
    }

    /**
     * Update visibility state and notify callbacks
     */
    private fun updateState(newState: VisibilityState) {
        val oldState = _stateFlow.value

        if (oldState != newState) {
            _stateFlow.value = newState

            // Notify callbacks
            visibilityCallbacks.forEach { callback ->
                try {
                    callback(newState)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in visibility callback", e)
                }
            }

            Log.v(TAG, "State updated: $oldState -> $newState")
        }
    }

    /**
     * Update alpha value and notify callbacks
     */
    private fun updateAlpha(alpha: Float) {
        val oldAlpha = _alphaFlow.value

        if (oldAlpha != alpha) {
            _alphaFlow.value = alpha

            // Notify callbacks
            alphaCallbacks.forEach { callback ->
                try {
                    callback(alpha)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in alpha callback", e)
                }
            }
        }
    }

    /**
     * Cleanup resources
     */
    fun dispose() {
        cancelAutoHide()
        fadeAnimator?.cancel()
        fadeAnimator = null
        clearCallbacks()
        Log.d(TAG, "CursorVisibilityManager disposed")
    }
}

/**
 * Extension function for ValueAnimator to add end listener
 */
private fun ValueAnimator.doOnEnd(action: () -> Unit) {
    addListener(object : android.animation.Animator.AnimatorListener {
        override fun onAnimationStart(animation: android.animation.Animator) {}
        override fun onAnimationEnd(animation: android.animation.Animator) {
            action()
        }
        override fun onAnimationCancel(animation: android.animation.Animator) {}
        override fun onAnimationRepeat(animation: android.animation.Animator) {}
    })
}
