/**
 * RenameHintOverlay.kt - Contextual hint for command renaming
 * Path: Modules/VoiceOS/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/learnapp/ui/RenameHintOverlay.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: Claude Code (IDEACODE v10.3)
 * Created: 2025-12-08
 * Updated: 2025-12-08 (v1.0.1 - Removed Compose to fix lifecycle crash)
 * Related: LearnApp-Rename-Hint-Overlay-Mockups-5081220-V1.md
 *          LearnApp-On-Demand-Command-Renaming-5081220-V2.md
 *
 * Contextual hint overlay that shows when screen has generated fallback labels.
 * Non-intrusive overlay at top of screen with 3-second auto-dismiss.
 * Only shows once per screen per session.
 *
 * Features:
 * - Detects screens with generated labels (Button 1, Tab 2, etc.)
 * - Shows Material Design hint card with example command
 * - 3-second auto-dismiss with fade animation
 * - TTS announcement for accessibility
 * - Session-based tracking (doesn't repeat)
 * - Native Android widgets (no Compose)
 *
 * ## Fix History
 *
 * - v1.0.1 (2025-12-08): Fixed Compose lifecycle crash - Removed all Compose components
 *   - Replaced ComposeView + RenameHintCard with native XML layout
 *   - Replaced Compose animations with native View animations (alpha fade)
 *   - Eliminated Compose dependency that caused ViewTreeLifecycleOwner crash
 *   - ComposeView internally requires LifecycleOwner not available in AccessibilityService
 */

package com.augmentalis.voiceoscore.learnapp.ui

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.TextView
import com.augmentalis.voiceoscore.R
import com.augmentalis.database.dto.GeneratedCommandDTO

/**
 * RenameHintOverlay - Manages hint overlay lifecycle
 *
 * Shows contextual hint when screen has generated labels.
 * Uses WindowManager overlay pattern (same as ConsentDialog).
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
    /**
     * WindowManager for adding overlay views
     */
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    /**
     * Currently displayed view (if any)
     */
    private var currentView: View? = null

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

        // Show hint
        Log.i(TAG, "Showing rename hint for $screenKey (example: ${generatedCommand.commandText})")
        show(generatedCommand)
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
     * Show hint overlay
     *
     * Inflates XML layout with native Android widgets and adds to WindowManager.
     * Auto-dismisses after 3 seconds with fade animation.
     * Announces via TTS for accessibility.
     *
     * @param exampleCommand Command to use in example text
     */
    private fun show(exampleCommand: GeneratedCommandDTO) {
        // Remove any existing view first
        currentView?.let { view: View ->
            try {
                windowManager.removeView(view)
            } catch (e: Exception) {
                // View already removed, ignore
                Log.d(TAG, "Exception removing existing view: ${e.message}")
            }
        }

        // Extract button name from command
        val buttonName = extractButtonName(exampleCommand.commandText)

        // TTS announcement for accessibility
        tts?.speak(
            "Hint: You can rename buttons by saying: Rename $buttonName to Save. This message will auto-dismiss in 3 seconds.",
            TextToSpeech.QUEUE_FLUSH,
            null,
            "rename_hint"
        )

        // Inflate XML layout
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.learnapp_layout_rename_hint, null)

        // Update example text
        val textExample = view.findViewById<TextView>(R.id.text_example)
        textExample.text = "\"Rename $buttonName to Save\""

        // Get the card container for animation
        val hintCard = view.findViewById<View>(R.id.hint_card)

        // Window layout parameters
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        }

        // Add to WindowManager
        try {
            windowManager.addView(view, params)
            currentView = view
            Log.i(TAG, "Rename hint overlay displayed")

            // Fade-in animation (200ms)
            val fadeIn = AlphaAnimation(0.0f, 1.0f).apply {
                duration = 200
                fillAfter = true
            }
            hintCard.startAnimation(fadeIn)

            // Auto-dismiss after 3 seconds with fade-out
            Handler(Looper.getMainLooper()).postDelayed({
                val fadeOut = AlphaAnimation(1.0f, 0.0f).apply {
                    duration = 200
                    fillAfter = true
                    setAnimationListener(object : Animation.AnimationListener {
                        override fun onAnimationStart(animation: Animation?) {}
                        override fun onAnimationRepeat(animation: Animation?) {}
                        override fun onAnimationEnd(animation: Animation?) {
                            hide()
                        }
                    })
                }
                hintCard.startAnimation(fadeOut)
            }, 3000)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to add rename hint overlay", e)
        }
    }

    /**
     * Hide overlay
     *
     * Removes view from WindowManager if currently displayed.
     */
    private fun hide() {
        currentView?.let { view: View ->
            try {
                windowManager.removeView(view)
                Log.d(TAG, "Rename hint overlay removed")
            } catch (e: Exception) {
                // View already removed, ignore
                Log.d(TAG, "Exception removing overlay: ${e.message}")
            }
            currentView = null
        }
    }

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

    /**
     * Convert dp to pixels
     *
     * @param dp Density-independent pixels
     * @return Pixels
     */
    private fun dpToPx(dp: Int): Int {
        val density = context.resources.displayMetrics.density
        return (dp * density).toInt()
    }

    companion object {
        private const val TAG = "RenameHintOverlay"
    }
}
