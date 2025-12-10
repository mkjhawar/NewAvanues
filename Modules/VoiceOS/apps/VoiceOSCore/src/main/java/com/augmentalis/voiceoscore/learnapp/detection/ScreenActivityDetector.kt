/**
 * ScreenActivityDetector.kt - Detects screen changes and triggers rename hints
 * Path: modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/learnapp/detection/ScreenActivityDetector.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: Claude Code (IDEACODE v10.3)
 * Created: 2025-12-08
 *
 * Observes accessibility events to detect when user navigates to new screen.
 * Checks if screen has generated labels and shows hint overlay if needed.
 */

package com.augmentalis.voiceoscore.learnapp.detection

import android.content.Context
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.augmentalis.database.VoiceOSDatabaseManager
import com.augmentalis.database.dto.GeneratedCommandDTO
import com.augmentalis.voiceoscore.learnapp.ui.RenameHintOverlay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Screen Activity Detector
 *
 * Detects screen/activity changes and triggers rename hints.
 * Observes accessibility events to detect when user navigates to new screen,
 * then checks if screen has generated labels and shows hint overlay if needed.
 *
 * ## Usage Example
 *
 * ```kotlin
 * val renameHintOverlay = RenameHintOverlay(context, windowManager)
 * val detector = ScreenActivityDetector(
 *     context = context,
 *     database = VoiceOSDatabaseManager.getInstance(driverFactory),
 *     renameHintOverlay = renameHintOverlay
 * )
 *
 * // In AccessibilityService
 * override fun onAccessibilityEvent(event: AccessibilityEvent) {
 *     if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
 *         lifecycleScope.launch {
 *             detector.onWindowStateChanged(event)
 *         }
 *     }
 * }
 * ```
 *
 * ## Event Filtering
 *
 * - Only processes TYPE_WINDOW_STATE_CHANGED events
 * - Tracks current screen to detect changes
 * - Queries database for commands on new screen
 * - Delegates to RenameHintOverlay for UI display
 *
 * ## Threading
 *
 * - Database queries on Dispatchers.IO
 * - UI updates on Dispatchers.Main (via RenameHintOverlay)
 * - Uses SupervisorJob for error isolation
 *
 * @property context Application context
 * @property database Database manager for querying commands
 * @property renameHintOverlay Overlay component for showing hints
 *
 * @since Phase 2 (On-Demand Command Renaming)
 */
class ScreenActivityDetector(
    private val context: Context,
    private val database: VoiceOSDatabaseManager,
    private val renameHintOverlay: RenameHintOverlay
) {

    /**
     * Coroutine scope for async operations
     * Uses SupervisorJob to prevent child failures from cascading
     */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /**
     * Current screen identifier (packageName/className)
     * Used to detect screen changes
     */
    private var currentScreen: String = ""

    /**
     * Called when accessibility event TYPE_WINDOW_STATE_CHANGED fires
     *
     * Detects screen changes by comparing packageName/className with current screen.
     * When screen changes, queries database for commands and triggers hint overlay.
     *
     * @param event Accessibility event with window state change info
     */
    suspend fun onWindowStateChanged(event: AccessibilityEvent) {
        try {
            // Extract package and class names
            val packageName = event.packageName?.toString()
            val className = event.className?.toString()

            // Validate event data
            if (packageName.isNullOrBlank() || className.isNullOrBlank()) {
                Log.d(TAG, "Skipping event with missing package/class: pkg=$packageName, cls=$className")
                return
            }

            // Build screen identifier
            val screenKey = "$packageName/$className"

            // Check if screen changed
            if (screenKey != currentScreen) {
                Log.d(TAG, "Screen changed: $currentScreen → $screenKey")
                currentScreen = screenKey

                // Get commands for this screen
                val commands = getCommandsForScreen(packageName, className)

                Log.d(TAG, "Found ${commands.size} commands for $screenKey")

                // Show hint if needed (RenameHintOverlay handles UI thread)
                withContext(Dispatchers.Main) {
                    renameHintOverlay.showIfNeeded(packageName, className, commands)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing window state change", e)
        }
    }

    /**
     * Get commands for current screen
     *
     * Queries database for all commands associated with the given package.
     * Currently returns all commands for package; future enhancement will
     * filter by specific screen/activity when screen metadata is available.
     *
     * Database query executed on IO dispatcher for performance.
     *
     * @param packageName App package name (e.g., "com.example.app")
     * @param className Activity class name (e.g., "com.example.app.MainActivity")
     * @return List of commands for the screen (may be empty)
     */
    private suspend fun getCommandsForScreen(
        packageName: String,
        className: String
    ): List<GeneratedCommandDTO> = withContext(Dispatchers.IO) {
        try {
            // Get all commands for package
            val allCommands = database.generatedCommands.getByPackage(packageName)

            Log.d(TAG, "Retrieved ${allCommands.size} commands for package: $packageName")

            // TODO: Filter by screen when screen metadata is available in database
            // For now, return all commands for package
            // Future enhancement: Filter by className or screen context

            allCommands
        } catch (e: Exception) {
            Log.e(TAG, "Error querying commands for screen: $packageName/$className", e)
            emptyList()
        }
    }

    /**
     * Reset current screen tracking
     *
     * Clears the current screen identifier, forcing next event to be
     * treated as a new screen change. Useful for testing or when
     * re-initializing the detector.
     */
    fun resetCurrentScreen() {
        currentScreen = ""
        Log.d(TAG, "Reset current screen tracking")
    }

    /**
     * Get current screen identifier
     *
     * @return Current screen as "packageName/className", or empty if not set
     */
    fun getCurrentScreen(): String = currentScreen

    companion object {
        private const val TAG = "ScreenActivityDetector"
    }
}
