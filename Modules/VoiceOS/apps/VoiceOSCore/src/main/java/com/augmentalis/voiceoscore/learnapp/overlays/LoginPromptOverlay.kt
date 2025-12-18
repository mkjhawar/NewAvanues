/**
 * LoginPromptOverlay.kt - Overlay for login screen prompts during exploration
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-12-17
 *
 * Displays login prompt overlay when login screen is detected during exploration.
 */
package com.augmentalis.voiceoscore.learnapp.overlays

import android.accessibilityservice.AccessibilityService
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView

/**
 * Login Prompt Configuration
 */
data class LoginPromptConfig(
    val packageName: String,
    val appName: String,
    val title: String = "Login Required",
    val message: String = "Please log in to continue exploration",
    val resumeButtonText: String = "I've Logged In",
    val skipButtonText: String = "Skip Login"
)

// Note: LoginPromptAction is defined in LoginPromptAction.kt

/**
 * Login Prompt Overlay
 *
 * Displays an overlay when login screen is detected during app exploration.
 * Allows user to resume after logging in or skip the login requirement.
 */
class LoginPromptOverlay(
    private val accessibilityService: AccessibilityService,
    private val config: LoginPromptConfig,
    private val onAction: (LoginPromptAction) -> Unit
) {
    private var overlayView: android.view.View? = null
    private var messageTextView: TextView? = null
    private val windowManager: WindowManager =
        accessibilityService.getSystemService(AccessibilityService.WINDOW_SERVICE) as WindowManager

    init {
        show()
    }

    /**
     * Show the login prompt overlay
     */
    fun show() {
        if (overlayView != null) return

        try {
            val layoutParams = createLayoutParams()
            overlayView = createOverlayView()
            windowManager.addView(overlayView, layoutParams)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to show login prompt overlay", e)
        }
    }

    /**
     * Hide the login prompt overlay
     */
    fun hide() {
        overlayView?.let { view ->
            try {
                windowManager.removeView(view)
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Failed to remove login prompt overlay", e)
            }
            overlayView = null
        }
    }

    /**
     * Update the overlay message
     */
    fun updateMessage(message: String) {
        messageTextView?.text = message
    }

    private fun createLayoutParams(): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }
    }

    private fun createOverlayView(): android.view.View {
        val inflater = LayoutInflater.from(accessibilityService)

        // Create a simple view programmatically if layout doesn't exist
        val view = android.widget.LinearLayout(accessibilityService).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setBackgroundColor(0xE0303030.toInt())
            setPadding(48, 32, 48, 32)

            // Title
            addView(TextView(context).apply {
                text = config.title
                textSize = 18f
                setTextColor(0xFFFFFFFF.toInt())
                gravity = Gravity.CENTER
            })

            // Message
            val msgView = TextView(context).apply {
                text = config.message
                textSize = 14f
                setTextColor(0xCCFFFFFF.toInt())
                gravity = Gravity.CENTER
                setPadding(0, 16, 0, 24)
            }
            messageTextView = msgView
            addView(msgView)

            // Buttons container
            addView(android.widget.LinearLayout(context).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER

                // Resume button
                addView(Button(context).apply {
                    text = config.resumeButtonText
                    setOnClickListener {
                        onAction(LoginPromptAction.Continue)
                        hide()
                    }
                })

                // Skip button
                addView(Button(context).apply {
                    text = config.skipButtonText
                    setOnClickListener {
                        onAction(LoginPromptAction.Skip)
                        hide()
                    }
                })
            })
        }

        return view
    }

    companion object {
        private const val TAG = "LoginPromptOverlay"
    }
}
