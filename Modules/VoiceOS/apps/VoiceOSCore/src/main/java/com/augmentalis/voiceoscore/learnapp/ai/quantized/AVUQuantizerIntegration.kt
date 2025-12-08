/**
 * AVUQuantizerIntegration.kt - Integration layer for real-time quantization
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-12-08
 *
 * Connects AVUQuantizer to ExplorationEngine via debug callbacks:
 * - Receives real-time exploration events
 * - Performs incremental quantization during exploration
 * - Saves quantized context when exploration completes
 * - Provides quantized context for NLU/LLM queries
 *
 * Part of LearnApp NLU Integration feature
 */
package com.augmentalis.voiceoscore.learnapp.ai.quantized

import android.content.Context
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import com.augmentalis.voiceoscore.learnapp.exploration.ExplorationDebugCallback
import com.augmentalis.voiceoscore.learnapp.models.ElementInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * AVU Quantizer Integration
 *
 * Bridges the ExplorationEngine and AVUQuantizer, enabling real-time
 * quantization of UI elements as they are discovered during exploration.
 *
 * ## Features:
 * - Implements ExplorationDebugCallback for event capture
 * - Performs real-time element quantization
 * - Tracks navigation paths
 * - Auto-saves quantized context on exploration completion
 * - Provides query interface for NLU/LLM consumers
 *
 * ## Usage:
 * ```kotlin
 * val integration = AVUQuantizerIntegration(context)
 *
 * // Connect to exploration engine
 * explorationEngine.setDebugCallback(integration)
 *
 * // Start exploration (integration captures events automatically)
 * explorationEngine.startExploration(packageName)
 *
 * // After exploration, get quantized context
 * val quantized = integration.getQuantizedContext()
 * val prompt = integration.generateLLMPrompt(quantized, "open settings")
 * ```
 */
class AVUQuantizerIntegration(
    private val context: Context
) : ExplorationDebugCallback {

    companion object {
        private const val TAG = "AVUQuantizerIntegration"
    }

    // Core components
    private val quantizer: AVUQuantizer
    private val serializer: AVUContextSerializer
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Current exploration state
    private var currentPackageName: String? = null
    private var currentAppName: String? = null
    private var explorationStartTime: Long = 0L
    private var isExplorationActive = false

    // Screen tracking for scroll direction detection
    private val screenScrollDirections = mutableMapOf<String, ScrollDirection>()

    // Parent screen tracking
    private var lastScreenHash: String? = null

    // Existing debug callback to chain (for debug overlay)
    private var chainedCallback: ExplorationDebugCallback? = null

    init {
        // Get screen dimensions
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getMetrics(metrics)

        quantizer = AVUQuantizer(
            screenWidth = metrics.widthPixels,
            screenHeight = metrics.heightPixels
        )
        serializer = AVUContextSerializer(context)

        Log.i(TAG, "Initialized with screen: ${metrics.widthPixels}x${metrics.heightPixels}")
    }

    /**
     * Set a chained debug callback
     *
     * Allows both this integration and another callback (e.g., debug overlay)
     * to receive exploration events.
     */
    fun setChainedCallback(callback: ExplorationDebugCallback?) {
        chainedCallback = callback
    }

    /**
     * Start quantization for a new exploration session
     *
     * Call this before starting exploration to reset state.
     */
    fun startQuantization(packageName: String, appName: String? = null) {
        scope.launch {
            Log.i(TAG, "Starting quantization for: $packageName")

            currentPackageName = packageName
            currentAppName = appName ?: packageName.substringAfterLast(".")
            explorationStartTime = System.currentTimeMillis()
            isExplorationActive = true
            lastScreenHash = null
            screenScrollDirections.clear()

            // Reset quantizer state
            quantizer.reset()
        }
    }

    /**
     * Stop quantization and save results
     *
     * Call this when exploration completes to finalize and save the quantized context.
     */
    fun stopQuantization(): QuantizedContext? {
        if (!isExplorationActive) {
            Log.w(TAG, "Quantization not active")
            return null
        }

        isExplorationActive = false
        val packageName = currentPackageName ?: return null
        val appName = currentAppName ?: packageName

        var result: QuantizedContext? = null

        scope.launch {
            try {
                // Build final quantized context
                result = quantizer.buildQuantizedContext(packageName, appName)

                // Save to file
                result?.let { context ->
                    serializer.saveQuantizedContext(context)

                    val stats = quantizer.getStats()
                    Log.i(TAG, "Quantization completed for $packageName:")
                    Log.i(TAG, "  Screens: ${stats.screensProcessed}")
                    Log.i(TAG, "  Elements: ${stats.elementsProcessed}")
                    Log.i(TAG, "  Navigation: ${stats.navigationPaths}")
                    Log.i(TAG, "  Actions: ${stats.actionCandidates}")
                    Log.i(TAG, "  Clusters: ${stats.semanticClusters}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to complete quantization", e)
            }
        }

        return result
    }

    /**
     * Get current quantized context
     *
     * Returns the current quantized context being built, or loads from file
     * if exploration is not active.
     */
    suspend fun getQuantizedContext(packageName: String? = null): QuantizedContext? {
        val pkg = packageName ?: currentPackageName ?: return null

        return if (isExplorationActive && pkg == currentPackageName) {
            // Return in-progress context
            quantizer.buildQuantizedContext(pkg, currentAppName ?: pkg)
        } else {
            // Load from file
            serializer.loadQuantizedContext(pkg)
        }
    }

    /**
     * Generate LLM prompt for a user goal
     */
    suspend fun generateLLMPrompt(
        packageName: String,
        userGoal: String,
        format: LLMPromptFormat = LLMPromptFormat.COMPACT
    ): String? {
        val context = getQuantizedContext(packageName) ?: return null

        return when (format) {
            LLMPromptFormat.COMPACT -> serializer.toCompactPrompt(context, userGoal)
            LLMPromptFormat.HTML -> serializer.toHTMLPrompt(context, userGoal)
            LLMPromptFormat.FULL -> serializer.toFullPrompt(context, userGoal)
        }
    }

    /**
     * Generate action prediction prompt for current screen
     */
    suspend fun generateActionPredictionPrompt(
        packageName: String,
        currentScreenHash: String,
        userIntent: String
    ): String? {
        val context = getQuantizedContext(packageName) ?: return null
        return serializer.toActionPredictionPrompt(context, currentScreenHash.take(8), userIntent)
    }

    // =========================================================================
    // ExplorationDebugCallback Implementation
    // =========================================================================

    override fun onScreenExplored(
        elements: List<ElementInfo>,
        screenHash: String,
        activityName: String,
        packageName: String,
        parentScreenHash: String?
    ) {
        // Chain to other callback first
        chainedCallback?.onScreenExplored(elements, screenHash, activityName, packageName, parentScreenHash)

        if (!isExplorationActive) return

        scope.launch {
            try {
                // Detect scroll direction from elements
                val scrollDirection = detectScrollDirection(elements)
                screenScrollDirections[screenHash.take(8)] = scrollDirection

                // Process screen with quantizer
                quantizer.onScreenExplored(
                    screenHash = screenHash,
                    activityName = activityName,
                    elements = elements,
                    parentScreenHash = parentScreenHash,
                    scrollDirection = scrollDirection
                )

                lastScreenHash = screenHash

                Log.d(TAG, "Quantized screen ${screenHash.take(8)}: ${elements.size} elements")
            } catch (e: Exception) {
                Log.e(TAG, "Error quantizing screen", e)
            }
        }
    }

    override fun onElementNavigated(elementKey: String, destinationScreenHash: String) {
        // Chain to other callback first
        chainedCallback?.onElementNavigated(elementKey, destinationScreenHash)

        if (!isExplorationActive) return

        scope.launch {
            try {
                // Parse element key (format: "screenHash:stableId" or just stableId)
                val parts = elementKey.split(":")
                val sourceScreenHash = if (parts.size > 1) parts[0] else lastScreenHash ?: return@launch
                val elementId = if (parts.size > 1) parts[1] else parts[0]

                // Get element label (would need lookup, using elementId for now)
                val elementLabel = elementId.take(15) // Placeholder

                quantizer.onNavigationDiscovered(
                    fromScreenHash = sourceScreenHash,
                    toScreenHash = destinationScreenHash,
                    triggerElementId = elementId,
                    triggerLabel = elementLabel
                )

                Log.d(TAG, "Quantized navigation: ${sourceScreenHash.take(8)} -> ${destinationScreenHash.take(8)}")
            } catch (e: Exception) {
                Log.e(TAG, "Error quantizing navigation", e)
            }
        }
    }

    override fun onProgressUpdated(progress: Int) {
        // Chain to other callback
        chainedCallback?.onProgressUpdated(progress)

        // Auto-save at certain progress milestones
        if (isExplorationActive && progress % 25 == 0 && progress > 0) {
            scope.launch {
                try {
                    val packageName = currentPackageName ?: return@launch
                    val appName = currentAppName ?: packageName
                    val context = quantizer.buildQuantizedContext(packageName, appName)
                    serializer.saveQuantizedContext(context)
                    Log.d(TAG, "Auto-saved quantized context at $progress%")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to auto-save", e)
                }
            }
        }
    }

    override fun onElementClicked(stableId: String, screenHash: String, vuid: String?) {
        // Chain to other callback
        chainedCallback?.onElementClicked(stableId, screenHash, vuid)
        // Quantizer doesn't need click events specifically
    }

    override fun onElementBlocked(stableId: String, screenHash: String, reason: String) {
        // Chain to other callback
        chainedCallback?.onElementBlocked(stableId, screenHash, reason)
        // Could track blocked elements for statistics
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    /**
     * Detect scroll direction from element properties
     */
    private fun detectScrollDirection(elements: List<ElementInfo>): ScrollDirection {
        val scrollableElements = elements.filter { it.isScrollable }

        if (scrollableElements.isEmpty()) return ScrollDirection.NONE

        var hasVertical = false
        var hasHorizontal = false

        for (element in scrollableElements) {
            val className = element.className.lowercase()
            val resourceId = element.resourceId.lowercase()

            when {
                className.contains("horizontalscroll") ||
                resourceId.contains("horizontal") ||
                className.contains("viewpager") -> hasHorizontal = true

                className.contains("recyclerview") ||
                className.contains("listview") ||
                className.contains("scrollview") -> hasVertical = true
            }
        }

        return when {
            hasVertical && hasHorizontal -> ScrollDirection.BOTH
            hasHorizontal -> ScrollDirection.HORIZONTAL
            hasVertical -> ScrollDirection.VERTICAL
            else -> ScrollDirection.VERTICAL // Default for generic scrollables
        }
    }

    /**
     * Check if quantized context exists for a package
     */
    fun hasQuantizedContext(packageName: String): Boolean {
        return serializer.quantizedContextExists(packageName)
    }

    /**
     * Delete quantized context for a package
     */
    suspend fun deleteQuantizedContext(packageName: String): Boolean {
        return serializer.deleteQuantizedContext(packageName)
    }

    /**
     * Get quantizer statistics
     */
    fun getQuantizerStats(): QuantizerStats {
        return quantizer.getStats()
    }

    /**
     * List all packages with quantized contexts
     */
    suspend fun listQuantizedPackages(): List<String> {
        return serializer.listQuantizedContexts()
    }
}
