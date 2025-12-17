/**
 * WidgetOverlayHelper.kt - Helper utilities for widget overlays
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-12-16
 *
 * Provides helper utilities for managing WindowManager-based overlay widgets.
 * Handles layout params, window types, and overlay lifecycle management.
 */
package com.augmentalis.voiceoscore.learnapp.ui.widgets

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.WindowManager.LayoutParams

/**
 * Widget Overlay Helper
 *
 * Utility object for creating and managing overlay widgets using WindowManager.
 * Handles Android version compatibility for overlay window types.
 */
object WidgetOverlayHelper {
    private const val TAG = "WidgetOverlayHelper"

    /**
     * Default overlay layout params for accessibility service overlays.
     */
    fun createOverlayLayoutParams(
        width: Int = LayoutParams.WRAP_CONTENT,
        height: Int = LayoutParams.WRAP_CONTENT,
        gravity: Int = Gravity.TOP or Gravity.START,
        x: Int = 0,
        y: Int = 0,
        focusable: Boolean = false,
        touchable: Boolean = true
    ): LayoutParams {
        val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            LayoutParams.TYPE_PHONE
        }

        var flags = LayoutParams.FLAG_NOT_FOCUSABLE or
                LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                LayoutParams.FLAG_LAYOUT_NO_LIMITS

        if (!focusable) {
            flags = flags or LayoutParams.FLAG_NOT_FOCUSABLE
        }

        if (!touchable) {
            flags = flags or LayoutParams.FLAG_NOT_TOUCHABLE
        }

        return LayoutParams(
            width,
            height,
            windowType,
            flags,
            PixelFormat.TRANSLUCENT
        ).apply {
            this.gravity = gravity
            this.x = x
            this.y = y
        }
    }

    /**
     * Create layout params for a full-screen overlay.
     */
    fun createFullScreenOverlayParams(
        focusable: Boolean = false,
        touchable: Boolean = true
    ): LayoutParams {
        return createOverlayLayoutParams(
            width = LayoutParams.MATCH_PARENT,
            height = LayoutParams.MATCH_PARENT,
            gravity = Gravity.TOP or Gravity.START,
            focusable = focusable,
            touchable = touchable
        )
    }

    /**
     * Create layout params for a floating widget overlay.
     */
    fun createFloatingWidgetParams(
        width: Int = LayoutParams.WRAP_CONTENT,
        height: Int = LayoutParams.WRAP_CONTENT,
        x: Int = 0,
        y: Int = 100
    ): LayoutParams {
        return createOverlayLayoutParams(
            width = width,
            height = height,
            gravity = Gravity.TOP or Gravity.START,
            x = x,
            y = y,
            focusable = false,
            touchable = true
        )
    }

    /**
     * Create layout params for a centered dialog overlay.
     */
    fun createCenteredDialogParams(
        width: Int = LayoutParams.WRAP_CONTENT,
        height: Int = LayoutParams.WRAP_CONTENT
    ): LayoutParams {
        return createOverlayLayoutParams(
            width = width,
            height = height,
            gravity = Gravity.CENTER,
            focusable = true,
            touchable = true
        )
    }

    /**
     * Safely add a view to the WindowManager.
     *
     * @param context Context with SYSTEM_ALERT_WINDOW permission
     * @param view View to add
     * @param params Layout parameters
     * @return true if view was added successfully
     */
    fun addOverlay(context: Context, view: View, params: LayoutParams): Boolean {
        return try {
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            windowManager.addView(view, params)
            Log.d(TAG, "Overlay added successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add overlay", e)
            false
        }
    }

    /**
     * Safely remove a view from the WindowManager.
     *
     * @param context Context
     * @param view View to remove
     * @return true if view was removed successfully
     */
    fun removeOverlay(context: Context, view: View?): Boolean {
        if (view == null) return false

        return try {
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            if (view.isAttachedToWindow) {
                windowManager.removeView(view)
                Log.d(TAG, "Overlay removed successfully")
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove overlay", e)
            false
        }
    }

    /**
     * Safely update overlay layout params.
     *
     * @param context Context
     * @param view View to update
     * @param params New layout parameters
     * @return true if update was successful
     */
    fun updateOverlay(context: Context, view: View, params: LayoutParams): Boolean {
        return try {
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            if (view.isAttachedToWindow) {
                windowManager.updateViewLayout(view, params)
                Log.d(TAG, "Overlay updated successfully")
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update overlay", e)
            false
        }
    }

    /**
     * Check if overlay permission is granted.
     *
     * @param context Context
     * @return true if overlay permission is available
     */
    fun hasOverlayPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            android.provider.Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    /**
     * Move an overlay to a new position.
     *
     * @param context Context
     * @param view View to move
     * @param params Current layout params (will be modified)
     * @param x New X position
     * @param y New Y position
     */
    fun moveOverlay(
        context: Context,
        view: View,
        params: LayoutParams,
        x: Int,
        y: Int
    ) {
        params.x = x
        params.y = y
        updateOverlay(context, view, params)
    }

    /**
     * Set overlay visibility.
     *
     * @param view Overlay view
     * @param visible true to show, false to hide
     */
    fun setOverlayVisibility(view: View?, visible: Boolean) {
        view?.visibility = if (visible) View.VISIBLE else View.GONE
    }

    /**
     * Set overlay alpha (transparency).
     *
     * @param view Overlay view
     * @param alpha Alpha value (0.0 - 1.0)
     */
    fun setOverlayAlpha(view: View?, alpha: Float) {
        view?.alpha = alpha.coerceIn(0f, 1f)
    }

    /**
     * Animate overlay fade in.
     *
     * @param view Overlay view
     * @param duration Animation duration in ms
     */
    fun fadeIn(view: View?, duration: Long = 200) {
        view?.apply {
            alpha = 0f
            visibility = View.VISIBLE
            animate()
                .alpha(1f)
                .setDuration(duration)
                .start()
        }
    }

    /**
     * Animate overlay fade out.
     *
     * @param view Overlay view
     * @param duration Animation duration in ms
     * @param onComplete Callback when animation completes
     */
    fun fadeOut(view: View?, duration: Long = 200, onComplete: (() -> Unit)? = null) {
        view?.animate()
            ?.alpha(0f)
            ?.setDuration(duration)
            ?.withEndAction {
                view.visibility = View.GONE
                onComplete?.invoke()
            }
            ?.start()
    }

    /**
     * Get window type for current Android version.
     */
    fun getOverlayWindowType(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            LayoutParams.TYPE_PHONE
        }
    }
}
