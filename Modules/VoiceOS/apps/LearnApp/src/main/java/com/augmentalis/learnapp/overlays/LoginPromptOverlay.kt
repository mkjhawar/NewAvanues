/**
 * LoginPromptOverlay.kt - Widget-based overlay for login prompts during app exploration
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-10-09
 * Updated: 2025-10-24 (Migrated from Compose to widgets)
 *
 * Displays a Material 3 overlay to guide users when LearnApp encounters a login screen
 * during automated exploration. Migrated to widgets for AccessibilityService compatibility.
 */
package com.augmentalis.learnapp.overlays

import android.app.AlertDialog
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import com.augmentalis.learnapp.R

/**
 * User action in response to login prompt
 */
sealed class LoginPromptAction {
    object Skip : LoginPromptAction()
    object Continue : LoginPromptAction()
    object Dismiss : LoginPromptAction()
}

/**
 * Configuration for login prompt overlay
 */
data class LoginPromptConfig(
    val appName: String,
    val packageName: String,
    val message: String = "LearnApp detected a login screen",
    val showVoiceHints: Boolean = true
)

/**
 * Material 3 overlay that prompts user during login screen encounters
 *
 * This overlay appears when LearnApp is exploring an app and encounters
 * a login screen. It provides clear guidance and options to skip or continue.
 *
 * ## Migration Notes
 *
 * Previously used Jetpack Compose with ComposeView.
 * Now uses AlertDialog with custom XML layout for AccessibilityService compatibility.
 * All Compose dependencies removed.
 *
 * ## Thread Safety
 *
 * All UI operations automatically executed on main thread via Handler.post().
 * Safe to call from any thread.
 *
 * @param context Android context for window management
 * @param config Configuration for the prompt
 * @param onAction Callback invoked when user takes an action
 */
class LoginPromptOverlay(
    private val context: Context,
    private val config: LoginPromptConfig,
    private val onAction: (LoginPromptAction) -> Unit
) {

    companion object {
        private const val TAG = "LoginPromptOverlay"
    }

    /**
     * Main thread handler for UI operations
     */
    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * Currently displayed dialog (if any)
     */
    private var currentDialog: AlertDialog? = null

    /**
     * Show the overlay
     *
     * @return true if overlay was shown successfully
     */
    fun show(): Boolean {
        if (isVisible()) {
            Log.w(TAG, "Overlay already visible")
            return true
        }

        mainHandler.post {
            try {
                Log.d(TAG, "Showing login prompt overlay for ${config.appName}")

                // Inflate custom view
                val customView = LayoutInflater.from(context)
                    .inflate(R.layout.layout_login_prompt, null)

                // Configure app name
                customView.findViewById<TextView>(R.id.app_name_text).text = config.appName

                // Configure message
                customView.findViewById<TextView>(R.id.message_text).text = config.message

                // Create AlertDialog with Material Design 3 theme
                val dialog = AlertDialog.Builder(context, R.style.Theme_LearnApp_Dialog)
                    .setView(customView)
                    .setCancelable(false)
                    .create()

                // Set window type for AccessibilityService
                dialog.window?.setType(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
                    } else {
                        @Suppress("DEPRECATION")
                        WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
                    }
                )

                // Apply Material Design 3 animations
                dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation

                // Wire up Skip button
                customView.findViewById<Button>(R.id.btn_skip).setOnClickListener {
                    handleAction(LoginPromptAction.Skip)
                }

                // Wire up Continue button
                customView.findViewById<Button>(R.id.btn_continue).setOnClickListener {
                    handleAction(LoginPromptAction.Continue)
                }

                // Wire up Dismiss button
                customView.findViewById<Button>(R.id.btn_dismiss).setOnClickListener {
                    handleAction(LoginPromptAction.Dismiss)
                }

                // Show dialog
                currentDialog = dialog
                dialog.show()

                Log.d(TAG, "Login prompt overlay shown successfully")

            } catch (e: Exception) {
                Log.e(TAG, "Failed to show login prompt overlay", e)
            }
        }

        return true
    }

    /**
     * Hide the overlay
     *
     * @return true if overlay was hidden successfully
     */
    fun hide(): Boolean {
        if (!isVisible()) {
            return true
        }

        mainHandler.post {
            try {
                Log.d(TAG, "Hiding login prompt overlay")

                currentDialog?.dismiss()
                currentDialog = null

                Log.d(TAG, "Login prompt overlay hidden successfully")

            } catch (e: Exception) {
                Log.e(TAG, "Failed to hide login prompt overlay", e)
            }
        }

        return true
    }

    /**
     * Toggle overlay visibility
     */
    fun toggle(): Boolean {
        return if (isVisible()) hide() else show()
    }

    /**
     * Check if overlay is currently visible
     */
    fun isVisible(): Boolean {
        return currentDialog?.isShowing == true
    }

    /**
     * Handle user action
     */
    private fun handleAction(action: LoginPromptAction) {
        Log.d(TAG, "User action: $action")
        hide()
        onAction(action)
    }
}
