/**
 * RenameHintOverlay.kt - XML-based rename hint overlay
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-10-09
 * Updated: 2025-12-11 (Converted from Compose to XML)
 *
 * Displays a hint overlay showing users how to rename buttons via voice commands.
 *
 * FIX HISTORY:
 * - v2.0.0 (2025-12-11): Converted from Compose to XML-based layout
 *   Root cause: ViewTreeLifecycleOwner crashes in Compose when used in AccessibilityService
 *   Solution: Use native Android XML layout with MaterialThemeHelper, no lifecycle required
 *   Pattern: Follows FloatingProgressWidget.kt and ProgressOverlayManager.kt
 */
package com.augmentalis.voiceoscore.learnapp.ui

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import com.augmentalis.voiceoscore.R
import com.augmentalis.voiceoscore.utils.MaterialThemeHelper
import com.augmentalis.database.dto.GeneratedCommandDTO

/**
 * RenameHintOverlay - Manages hint overlay lifecycle
 *
 * Shows contextual hint when screen has generated labels.
 * Uses XML layout with native Android widgets to avoid Compose lifecycle issues.
 *
 * ## Usage Example
 *
 * ```kotlin
 * val overlay = RenameHintOverlay(
 *     context = accessibilityService,
 *     tts = textToSpeech
 * )
 *
 * // When screen changes
 * overlay.showIfNeeded(
 *     packageName = "com.instagram.android",
 *     activityName = "MainActivity",
 *     generatedCommands = commands
 * )
 *
 * // Reset session (e.g., after app restart)
 * overlay.reset()
 * ```
 *
 * ## Thread Safety
 *
 * All WindowManager operations run on main thread.
 * Safe to call from any thread.
 *
 * @param context AccessibilityService context
 * @param tts TextToSpeech instance for accessibility announcements
 */
class RenameHintOverlay(
    private val context: AccessibilityService,
    private val tts: TextToSpeech?
) {
    companion object {
        private const val TAG = "RenameHintOverlay"
        private const val AUTO_DISMISS_DELAY = 3000L  // 3 seconds
    }

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private var overlayView: View? = null
    private var isShowing = false
    private var autoDismissRunnable: Runnable? = null

    // UI elements
    private var titleText: TextView? = null
    private var exampleText: TextView? = null
    private var hintCard: View? = null

    /**
     * Set of screen identifiers where hint has been shown this session
     * Format: "packageName/activityName"
     */
    private val shownScreens = mutableSetOf<String>()

    /**
     * Show rename hint if screen has generated labels
     *
     * Checks if:
     * 1. Hint hasn't been shown for this screen this session
     * 2. Screen has generated labels (Button 1, Tab 2, etc.)
     *
     * If both conditions are true, shows hint overlay with example command.
     *
     * @param packageName Current app package name
     * @param activityName Current activity name
     * @param generatedCommands List of commands with potential generated labels
     */
    fun showIfNeeded(
        packageName: String,
        activityName: String,
        generatedCommands: List<GeneratedCommandDTO>
    ) {
        // Screen identifier
        val screenKey = "$packageName/$activityName"

        // Already shown this session?
        if (shownScreens.contains(screenKey)) {
            Log.d(TAG, "Hint already shown for $screenKey")
            return
        }

        // Any generated labels on this screen?
        val generatedCommand = generatedCommands.firstOrNull { command ->
            isGeneratedLabel(command.commandText)
        }

        if (generatedCommand == null) {
            Log.d(TAG, "No generated labels on $screenKey")
            return
        }

        // Extract button name and show hint
        val buttonName = extractButtonName(generatedCommand.commandText)
        Log.i(TAG, "Showing rename hint for $screenKey (example: $buttonName)")
        show(buttonName)
        shownScreens.add(screenKey)
    }

    /**
     * Check if command text is a generated label
     *
     * Detects patterns like:
     * - "click button 1", "click tab 2"
     * - "click top button", "click bottom card"
     * - "click top left button" (Unity 3x3 grid)
     * - "click corner top far left button" (Unreal 4x4 grid)
     *
     * @param commandText Command text to check
     * @return true if text matches generated label pattern
     */
    internal fun isGeneratedLabel(commandText: String): Boolean {
        val patterns = listOf(
            // Position-based (Button 1, Tab 2, etc.)
            Regex("click (button|tab|card|option) \\d+"),
            // Context-aware (top button, bottom card, etc.)
            Regex("click (top|bottom|center|middle) .+"),
            // Unity 3x3 grid (top left button, middle center card, etc.)
            Regex("click (top|middle|bottom) (left|center|right) .+"),
            // Unreal corner labels
            Regex("click corner .+"),
            // Unreal 4x4 grid (upper left button, lower far right card, etc.)
            Regex("click (upper|lower) .+")
        )

        return patterns.any { it.matches(commandText) }
    }

    /**
     * Show hint overlay with button name
     *
     * @param buttonName Name of the button to show in example (e.g., "Button 1", "Save")
     */
    private fun show(buttonName: String) {
        mainHandler.post {
            try {
                if (isShowing) {
                    Log.d(TAG, "Overlay already showing, updating content")
                    updateContent(buttonName)
                    return@post
                }

                // Create view if needed
                if (overlayView == null) {
                    createOverlayView()
                }

                // Update content
                updateContent(buttonName)

                // TTS announcement for accessibility
                tts?.speak(
                    "Hint: You can rename buttons by saying: Rename $buttonName to Save. This message will auto-dismiss in 3 seconds.",
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "rename_hint"
                )

                // Add to window manager
                val params = createLayoutParams()
                windowManager.addView(overlayView, params)
                isShowing = true

                // Animate in
                animateIn()

                // Schedule auto-dismiss
                scheduleAutoDismiss()

                Log.i(TAG, "Rename hint overlay shown for: $buttonName")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to show overlay", e)
            }
        }
    }

    /**
     * Hide the overlay
     */
    fun hide() {
        mainHandler.post {
            try {
                if (!isShowing) {
                    Log.d(TAG, "Overlay not showing, nothing to hide")
                    return@post
                }

                // Cancel auto-dismiss
                cancelAutoDismiss()

                // Animate out and remove
                animateOut {
                    try {
                        overlayView?.let { view ->
                            windowManager.removeView(view)
                        }
                        isShowing = false
                        Log.i(TAG, "Rename hint overlay hidden")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to remove view from window", e)
                        isShowing = false
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to hide overlay", e)
                isShowing = false
            }
        }
    }

    /**
     * Check if overlay is currently visible
     */
    fun isVisible(): Boolean = isShowing

    /**
     * Reset shown screens
     *
     * Clears session tracking so hints can be shown again.
     * Useful for testing or when starting new session.
     */
    fun reset() {
        shownScreens.clear()
        Log.d(TAG, "Reset shown screens")
    }

    /**
     * Dispose and clean up resources
     */
    fun dispose() {
        mainHandler.post {
            try {
                hide()
                overlayView = null
                titleText = null
                exampleText = null
                hintCard = null
                autoDismissRunnable = null
            } catch (e: Exception) {
                Log.e(TAG, "Error during disposal", e)
            }
        }
    }

    // ========== Private Methods ==========

    /**
     * Create the overlay view from XML layout
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun createOverlayView() {
        // Use MaterialThemeHelper to inflate with proper theme
        overlayView = MaterialThemeHelper.inflateOverlay(context, R.layout.learnapp_layout_rename_hint)

        overlayView?.let { view ->
            // Get UI element references
            hintCard = view.findViewById(R.id.hint_card)
            titleText = view.findViewById(R.id.text_title)
            exampleText = view.findViewById(R.id.text_example)

            // Set up click to dismiss
            hintCard?.setOnClickListener {
                hide()
            }

            // Initial state: hidden (for animation)
            hintCard?.alpha = 0f
        }
    }

    /**
     * Update overlay content with button name
     */
    private fun updateContent(buttonName: String) {
        exampleText?.text = "\"Rename $buttonName to Save\""
    }

    /**
     * Create window layout parameters for overlay
     */
    private fun createLayoutParams(): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        }
    }

    /**
     * Animate overlay in (fade in + slide down)
     */
    private fun animateIn() {
        hintCard?.let { card ->
            card.translationY = -50f  // Start slightly above
            card.alpha = 0f

            card.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(300)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
    }

    /**
     * Animate overlay out (fade out + slide up)
     */
    private fun animateOut(onComplete: () -> Unit) {
        hintCard?.let { card ->
            card.animate()
                .translationY(-50f)
                .alpha(0f)
                .setDuration(200)
                .setInterpolator(DecelerateInterpolator())
                .withEndAction(onComplete)
                .start()
        } ?: onComplete()
    }

    /**
     * Schedule auto-dismiss after delay
     */
    private fun scheduleAutoDismiss() {
        cancelAutoDismiss()
        autoDismissRunnable = Runnable {
            hide()
        }
        mainHandler.postDelayed(autoDismissRunnable!!, AUTO_DISMISS_DELAY)
    }

    /**
     * Cancel scheduled auto-dismiss
     */
    private fun cancelAutoDismiss() {
        autoDismissRunnable?.let { runnable ->
            mainHandler.removeCallbacks(runnable)
        }
        autoDismissRunnable = null
    }

    /**
     * Extract button name from command text
     *
     * Converts "click button 1" → "Button 1"
     * Converts "click top left button" → "Top Left Button"
     *
     * @param commandText Full command text (e.g., "click button 1")
     * @return Capitalized button name (e.g., "Button 1")
     */
    private fun extractButtonName(commandText: String): String {
        return commandText
            .removePrefix("click ")
            .removePrefix("type ")
            .removePrefix("scroll ")
            .split(" ")
            .joinToString(" ") { word ->
                word.replaceFirstChar { char -> char.uppercase() }
            }
    }
}
