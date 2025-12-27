/**
 * BaseOverlay.kt - VoiceOS component
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-12-22
 *
 * Base class for all overlay implementations
 */
package com.augmentalis.voiceoscore.accessibility.ui.overlays

import android.content.Context
import android.graphics.PixelFormat
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView

/**
 * Base Overlay
 *
 * Abstract base class for overlay implementations using Jetpack Compose.
 * Provides show/hide lifecycle and window management.
 */
abstract class BaseOverlay(
    protected val context: Context,
    private val overlayType: OverlayType
) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: ComposeView? = null
    private var isVisible = false

    /**
     * Show the overlay
     */
    fun show() {
        if (isVisible) return

        overlayView = ComposeView(context).apply {
            setContent {
                OverlayContent()
            }
        }

        windowManager.addView(overlayView, createLayoutParams())
        isVisible = true
    }

    /**
     * Hide the overlay
     */
    fun hide() {
        if (!isVisible) return

        overlayView?.let {
            windowManager.removeView(it)
        }
        overlayView = null
        isVisible = false
    }

    /**
     * Check if overlay is currently visible
     */
    fun isShowing(): Boolean = isVisible

    /**
     * Fade in the overlay
     */
    fun fadeIn() {
        // Stub implementation - subclasses can override for animation
        show()
    }

    /**
     * Fade out the overlay
     */
    fun fadeOut() {
        // Stub implementation - subclasses can override for animation
        hide()
    }

    /**
     * Overlay content - implemented by subclasses
     */
    @Composable
    abstract fun OverlayContent()

    /**
     * Create window layout parameters for the overlay
     */
    private fun createLayoutParams(): WindowManager.LayoutParams {
        return WindowManager.LayoutParams().apply {
            type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            format = PixelFormat.TRANSLUCENT
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL

            when (overlayType) {
                OverlayType.FULLSCREEN -> {
                    width = WindowManager.LayoutParams.MATCH_PARENT
                    height = WindowManager.LayoutParams.MATCH_PARENT
                }
                OverlayType.DIALOG -> {
                    width = WindowManager.LayoutParams.WRAP_CONTENT
                    height = WindowManager.LayoutParams.WRAP_CONTENT
                }
                OverlayType.BADGE -> {
                    width = WindowManager.LayoutParams.WRAP_CONTENT
                    height = WindowManager.LayoutParams.WRAP_CONTENT
                }
                OverlayType.TOAST -> {
                    width = WindowManager.LayoutParams.WRAP_CONTENT
                    height = WindowManager.LayoutParams.WRAP_CONTENT
                }
            }
        }
    }
}
