/**
 * DebugOverlayManager.kt - Manager for LearnApp debug overlay
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-12-08
 *
 * Manages the lifecycle and state of the debug overlay during LearnApp exploration.
 * Coordinates between ExplorationEngine, ElementInfo data, and the overlay view.
 *
 * ## Usage
 * ```kotlin
 * val debugManager = DebugOverlayManager(context, windowManager)
 *
 * // Enable during exploration
 * debugManager.show()
 *
 * // Update with current elements
 * debugManager.updateElements(elements, screenHash, activityName)
 *
 * // Toggle verbosity
 * debugManager.cycleVerbosity()
 *
 * // Hide when done
 * debugManager.hide()
 * ```
 */
package com.augmentalis.voiceoscore.learnapp.debugging

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.WindowManager
import com.augmentalis.voiceoscore.learnapp.models.ElementInfo
import com.augmentalis.voiceoscore.utils.MaterialThemeHelper

/**
 * Manager for debug overlay lifecycle and state
 *
 * @property context Application/Service context
 * @property windowManager WindowManager for overlay display
 */
class DebugOverlayManager(
    private val context: Context,
    private val windowManager: WindowManager
) {
    companion object {
        private const val TAG = "DebugOverlayManager"
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var overlayView: LearnAppDebugOverlay? = null
    private var currentState = DebugOverlayState()

    // Tracking data from exploration
    private var visitedScreens = mutableSetOf<String>()
    private var elementToDestination = mutableMapOf<String, String>()
    private var screenToParent = mutableMapOf<String, String>()
    private var totalElementsLearned = 0

    /**
     * Whether debug overlay is currently visible
     */
    val isVisible: Boolean get() = overlayView != null

    /**
     * Current verbosity level
     */
    val verbosity: DebugVerbosity get() = currentState.verbosity

    /**
     * Show debug overlay
     */
    fun show() {
        if (overlayView != null) {
            Log.d(TAG, "Debug overlay already visible")
            return
        }

        mainHandler.post {
            try {
                Log.i(TAG, "📊 Showing debug overlay")

                currentState = currentState.copy(isEnabled = true)

                val themedContext = MaterialThemeHelper.getThemedContext(context)
                overlayView = LearnAppDebugOverlay(themedContext, currentState)

                val params = createLayoutParams()
                windowManager.addView(overlayView, params)

                Log.i(TAG, "✅ Debug overlay shown")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to show debug overlay", e)
            }
        }
    }

    /**
     * Hide debug overlay
     */
    fun hide() {
        mainHandler.post {
            overlayView?.let { view ->
                try {
                    Log.i(TAG, "📊 Hiding debug overlay")
                    windowManager.removeView(view)
                    overlayView = null
                    currentState = currentState.copy(isEnabled = false)
                    Log.i(TAG, "✅ Debug overlay hidden")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to hide debug overlay", e)
                }
            }
        }
    }

    /**
     * Toggle overlay visibility
     */
    fun toggle() {
        if (isVisible) hide() else show()
    }

    /**
     * Cycle through verbosity levels: MINIMAL → STANDARD → VERBOSE → MINIMAL
     */
    fun cycleVerbosity() {
        val newVerbosity = when (currentState.verbosity) {
            DebugVerbosity.MINIMAL -> DebugVerbosity.STANDARD
            DebugVerbosity.STANDARD -> DebugVerbosity.VERBOSE
            DebugVerbosity.VERBOSE -> DebugVerbosity.MINIMAL
        }

        Log.d(TAG, "📝 Verbosity: ${currentState.verbosity} → $newVerbosity")
        currentState = currentState.copy(verbosity = newVerbosity)
        overlayView?.updateState(currentState)
    }

    /**
     * Set specific verbosity level
     */
    fun setVerbosity(verbosity: DebugVerbosity) {
        currentState = currentState.copy(verbosity = verbosity)
        overlayView?.updateState(currentState)
    }

    /**
     * Update overlay with current screen elements
     *
     * @param elements List of ElementInfo from current screen
     * @param screenHash Current screen hash
     * @param activityName Current activity name
     * @param packageName Target app package
     * @param parentScreenHash Screen we navigated from (if any)
     */
    fun updateElements(
        elements: List<ElementInfo>,
        screenHash: String,
        activityName: String,
        packageName: String,
        parentScreenHash: String? = null
    ) {
        // Track screen
        visitedScreens.add(screenHash)
        parentScreenHash?.let { screenToParent[screenHash] = it }

        // Convert ElementInfo to DebugElementState
        val debugElements = elements.map { element ->
            convertToDebugElement(element, screenHash)
        }

        // Count learned elements
        val learnedCount = debugElements.count { it.vuid != null }
        val exploredCount = debugElements.count { it.clickCount > 0 }

        // Build screen state
        val screenState = DebugScreenState(
            screenHash = screenHash,
            activityName = activityName,
            packageName = packageName,
            elements = debugElements,
            totalElements = elements.size,
            learnedElements = learnedCount,
            exploredElements = exploredCount,
            parentScreenHash = parentScreenHash ?: screenToParent[screenHash]
        )

        // Update overall state
        totalElementsLearned = maxOf(totalElementsLearned, learnedCount)

        currentState = currentState.copy(
            currentScreen = screenState,
            totalScreensExplored = visitedScreens.size,
            totalElementsLearned = totalElementsLearned
        )

        // Refresh overlay
        mainHandler.post {
            overlayView?.updateState(currentState)
        }

        Log.d(TAG, "📊 Updated debug overlay: ${elements.size} elements, $learnedCount learned")
    }

    /**
     * Record navigation from element to destination screen
     *
     * @param elementKey Element identifier (VUID or resource ID)
     * @param destinationScreenHash Screen that element navigated to
     */
    fun recordNavigation(elementKey: String, destinationScreenHash: String) {
        elementToDestination[elementKey] = destinationScreenHash
        Log.d(TAG, "📍 Recorded navigation: $elementKey → ${destinationScreenHash.take(8)}...")
    }

    /**
     * Update exploration progress percentage
     */
    fun updateProgress(progress: Int) {
        currentState = currentState.copy(explorationProgress = progress)
        mainHandler.post {
            overlayView?.updateState(currentState)
        }
    }

    /**
     * Mark element as currently being explored
     */
    fun markExploring(elementVuid: String) {
        currentState.currentScreen?.let { screen ->
            val updatedElements = screen.elements.map { element ->
                if (element.vuid == elementVuid) {
                    element.copy(learningSource = LearningSource.EXPLORING)
                } else {
                    element
                }
            }

            val updatedScreen = screen.copy(elements = updatedElements)
            currentState = currentState.copy(currentScreen = updatedScreen)

            mainHandler.post {
                overlayView?.updateState(currentState)
            }
        }
    }

    /**
     * Clear all tracking data
     */
    fun reset() {
        visitedScreens.clear()
        elementToDestination.clear()
        screenToParent.clear()
        totalElementsLearned = 0
        currentState = DebugOverlayState(
            isEnabled = currentState.isEnabled,
            verbosity = currentState.verbosity
        )

        mainHandler.post {
            overlayView?.updateState(currentState)
        }

        Log.i(TAG, "🔄 Debug overlay state reset")
    }

    /**
     * Clean up resources
     */
    fun dispose() {
        hide()
        reset()
    }

    // ========== Private Helpers ==========

    /**
     * Convert ElementInfo to DebugElementState
     */
    private fun convertToDebugElement(element: ElementInfo, screenHash: String): DebugElementState {
        val vuid = element.uuid
        val elementKey = vuid ?: element.resourceId.ifBlank { null }

        // Determine learning source
        val learningSource = when {
            vuid == null -> LearningSource.UNLEARNED
            element.classification == "jit_learned" -> LearningSource.JIT
            else -> LearningSource.LEARNAPP
        }

        // Check for navigation links
        val linksTo = elementKey?.let { elementToDestination[it] }
        val linkedFrom = screenToParent[screenHash]

        return DebugElementState(
            bounds = Rect(element.bounds),
            vuid = vuid,
            displayName = element.getDisplayName(),
            classification = element.classification,
            learningSource = learningSource,
            hashedScreen = screenHash.isNotEmpty(),
            linksToScreen = linksTo,
            linkedFromScreen = if (linksTo != null) linkedFrom else null,
            clickCount = 0, // Would need click tracker integration
            isDangerous = element.classification == "dangerous"
        )
    }

    /**
     * Create window layout params for overlay
     */
    private fun createLayoutParams(): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
    }
}
