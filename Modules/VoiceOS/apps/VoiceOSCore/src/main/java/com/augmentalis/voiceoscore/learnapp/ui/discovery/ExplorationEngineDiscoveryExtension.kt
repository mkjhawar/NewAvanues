/**
 * ExplorationEngineDiscoveryExtension.kt - Integration layer for command discovery
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VoiceOS Development Team
 * Created: 2025-12-08
 *
 * Extension functions and helper to integrate command discovery system
 * with ExplorationEngine completion flow.
 */

package com.augmentalis.voiceoscore.learnapp.ui.discovery

import android.content.Context
import android.util.Log
import com.augmentalis.database.VoiceOSDatabaseManager
import com.augmentalis.voiceoscore.learnapp.exploration.ExplorationEngine
import com.augmentalis.voiceoscore.learnapp.models.ElementInfo
import com.augmentalis.voiceoscore.learnapp.models.ExplorationState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Command Discovery Integration
 *
 * Integrates command discovery system with ExplorationEngine.
 * Automatically triggers discovery flow when exploration completes.
 *
 * ## Usage in VoiceOSService or LearnApp coordinator:
 * ```kotlin
 * val discoveryIntegration = CommandDiscoveryIntegration(context, databaseManager)
 *
 * // Start observing exploration state
 * explorationEngine.explorationState.collect { state ->
 *     when (state) {
 *         is ExplorationState.Completed -> {
 *             // Trigger command discovery
 *             discoveryIntegration.onExplorationCompleted(
 *                 state.packageName,
 *                 state.stats,
 *                 discoveredElements
 *             )
 *         }
 *         // ... other states
 *     }
 * }
 * ```
 */
class CommandDiscoveryIntegration(
    private val context: Context,
    private val databaseManager: VoiceOSDatabaseManager
) {
    companion object {
        private const val TAG = "CommandDiscoveryInt"
    }

    // Command discovery manager
    private val discoveryManager by lazy {
        CommandDiscoveryManager(context, databaseManager)
    }

    // Contextual hints service
    private val hintsService by lazy {
        ContextualHintsService(context, databaseManager)
    }

    /**
     * Called when exploration completes successfully
     *
     * Triggers the complete command discovery flow.
     *
     * @param packageName Target app package name
     * @param stats Exploration statistics
     * @param elements All discovered elements from exploration
     * @param scope Coroutine scope for async operations
     */
    fun onExplorationCompleted(
        packageName: String,
        stats: com.augmentalis.voiceoscore.learnapp.models.ExplorationStats,
        elements: List<ElementInfo>,
        scope: CoroutineScope
    ) {
        Log.i(TAG, "Exploration completed for $packageName - triggering command discovery")

        scope.launch {
            try {
                // Generate session ID from stats
                val sessionId = generateSessionId(packageName, stats)

                // Trigger command discovery
                discoveryManager.onExplorationComplete(
                    packageName = packageName,
                    sessionId = sessionId,
                    elements = elements
                )

                // Start contextual hints monitoring
                hintsService.startMonitoring(packageName)

                Log.d(TAG, "Command discovery triggered successfully")

            } catch (e: Exception) {
                Log.e(TAG, "Failed to trigger command discovery", e)
            }
        }
    }

    /**
     * Handle voice commands for discovery features
     *
     * Routes voice commands to appropriate discovery components.
     *
     * @param command Voice command spoken by user
     * @return true if command was handled, false otherwise
     */
    fun handleVoiceCommand(command: String): Boolean {
        val normalizedCommand = command.trim().lowercase()

        return when {
            // Show/hide overlay
            normalizedCommand.contains("show") && normalizedCommand.contains("commands") -> {
                if (normalizedCommand.contains("screen")) {
                    discoveryManager.toggleOverlay()
                } else {
                    discoveryManager.showCommandList(getCurrentPackageName())
                }
                true
            }

            normalizedCommand.contains("hide") && normalizedCommand.contains("commands") -> {
                discoveryManager.toggleOverlay()
                true
            }

            // Command list
            normalizedCommand.contains("what can i say") ||
                    normalizedCommand.contains("show command list") ||
                    normalizedCommand == "help" -> {
                discoveryManager.showCommandList(getCurrentPackageName())
                true
            }

            // Tutorial
            normalizedCommand.contains("tutorial") ||
                    normalizedCommand.contains("teach me") -> {
                discoveryManager.startTutorial(getCurrentPackageName())
                true
            }

            // Contextual hints
            normalizedCommand.contains("what can i do") ||
                    normalizedCommand.contains("suggestions") -> {
                hintsService.suggestCommands()
                true
            }

            else -> false
        }
    }

    /**
     * Notify of user action (for idle detection)
     */
    fun onUserAction() {
        hintsService.onUserAction()
    }

    /**
     * Notify of screen change (for contextual hints)
     */
    fun onScreenChanged(screenHash: String) {
        hintsService.onScreenChanged(screenHash)
    }

    /**
     * Get current package name being monitored
     */
    private fun getCurrentPackageName(): String {
        // TODO: Get from VoiceOSService or current foreground app
        return "unknown"
    }

    /**
     * Generate session ID from exploration stats
     */
    private fun generateSessionId(
        packageName: String,
        stats: com.augmentalis.voiceoscore.learnapp.models.ExplorationStats
    ): String {
        val timestamp = System.currentTimeMillis()
        return "$packageName-$timestamp-${stats.totalScreens}"
    }

    /**
     * Dispose resources
     */
    fun dispose() {
        discoveryManager.dispose()
        hintsService.dispose()
    }
}

/**
 * Extension function to add command discovery to ExplorationEngine
 *
 * Provides convenient integration point.
 *
 * @param context Android context
 * @param databaseManager Database manager
 * @param onCompleted Callback when exploration completes with discovery
 */
fun ExplorationEngine.setupCommandDiscovery(
    context: Context,
    databaseManager: VoiceOSDatabaseManager,
    scope: CoroutineScope,
    onCompleted: ((String, List<ElementInfo>) -> Unit)? = null
) {
    val integration = CommandDiscoveryIntegration(context, databaseManager)

    // Observe exploration state
    scope.launch {
        explorationState.collect { state ->
            when (state) {
                is ExplorationState.Completed -> {
                    Log.d("CommandDiscovery", "Exploration completed: ${state.packageName}")

                    // TODO: Get discovered elements from ExplorationEngine
                    // For now, we'll load from database
                    val elements = loadDiscoveredElements(state.packageName, databaseManager)

                    // Trigger discovery
                    integration.onExplorationCompleted(
                        packageName = state.packageName,
                        stats = state.stats,
                        elements = elements,
                        scope = scope
                    )

                    // Callback
                    onCompleted?.invoke(state.packageName, elements)
                }

                is ExplorationState.Running -> {
                    // User is active
                    integration.onUserAction()
                }

                else -> {
                    // Other states - no action needed
                }
            }
        }
    }
}

/**
 * Load discovered elements from database
 *
 * Temporary helper until ExplorationEngine exposes elements directly.
 *
 * @param packageName Target app package
 * @param databaseManager Database manager
 * @return List of discovered elements
 */
private suspend fun loadDiscoveredElements(
    packageName: String,
    databaseManager: VoiceOSDatabaseManager
): List<ElementInfo> {
    // Load commands from database and reconstruct basic element info
    val commands = databaseManager.generatedCommands.getAllCommands()

    // Convert commands to minimal ElementInfo
    // Note: This is a simplified version - real implementation should
    // store and retrieve full ElementInfo from exploration
    return commands.map { command ->
        ElementInfo(
            className = "android.view.View",
            text = command.commandText,
            contentDescription = "",
            resourceId = "",
            isClickable = command.actionType == "click",
            isEnabled = true,
            isPassword = false,
            isScrollable = command.actionType == "scroll",
            bounds = android.graphics.Rect(),  // TODO: Store bounds
            node = null,
            uuid = null,
            explorationBehavior = if (command.actionType == "click")
                com.augmentalis.voiceoscore.learnapp.models.ExplorationBehavior.CLICKABLE
            else
                com.augmentalis.voiceoscore.learnapp.models.ExplorationBehavior.SCROLLABLE,
            screenWidth = 0,
            screenHeight = 0,
            parent = null,
            children = null,
            index = 0
        )
    }
}
