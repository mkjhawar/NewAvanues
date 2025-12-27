/**
 * WidgetOverlayHelper.kt - VoiceOS component
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-12-22
 *
 * Helper for widget-based overlays in AccessibilityService context
 */
package com.augmentalis.voiceoscore.learnapp.ui.widgets

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager

object WidgetOverlayHelper {

    /**
     * Create centered dialog layout parameters for overlays
     */
    fun createCenteredDialogParams(): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }
    }

    /**
     * Add overlay to WindowManager
     */
    fun addOverlay(context: Context, view: View, params: WindowManager.LayoutParams) {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        try {
            windowManager.addView(view, params)
        } catch (e: Exception) {
            android.util.Log.e("WidgetOverlayHelper", "Error adding overlay", e)
        }
    }

    /**
     * Remove overlay from WindowManager
     */
    fun removeOverlay(context: Context, view: View) {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        try {
            windowManager.removeView(view)
        } catch (e: Exception) {
            android.util.Log.w("WidgetOverlayHelper", "Error removing overlay", e)
        }
    }

    fun createOverlay(context: Context): View? {
        return null
    }

    fun showOverlay(view: View) {
        // Stub implementation
    }

    fun hideOverlay(view: View) {
        // Stub implementation
    }

    fun updateOverlay(view: View, data: Any) {
        // Stub implementation
    }
}
