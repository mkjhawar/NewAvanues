package com.augmentalis.voiceoscore.commands.integration

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.util.Log
import com.augmentalis.database.VoiceOSDatabaseManager
import com.augmentalis.voiceoscore.commands.ElementCommandManager
import com.augmentalis.voiceoscore.commands.ui.CommandAssignmentDialog
import com.augmentalis.voiceoscore.commands.ui.PostLearningOverlay
import com.augmentalis.voiceoscore.commands.ui.QualityIndicatorOverlay
import com.augmentalis.voiceoscore.learnapp.integration.LearnAppIntegration
import com.augmentalis.voiceoscore.learnapp.models.ExplorationState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Wiring layer connecting manual command assignment UI components to VoiceOSService.
 *
 * Responsibilities:
 * - Initialize ElementCommandManager with database repositories
 * - Create and manage overlay lifecycle
 * - Hook into LearnAppIntegration post-exploration events
 * - Register voice commands for overlay control
 * - Integrate with VoiceOSService command system
 *
 * Integration Points:
 * - VoiceOSService.onCreate(): Initialize this integration
 * - LearnAppIntegration.handleExplorationStateChange(): Trigger post-learning overlay
 */
class ManualCommandIntegration(
    private val context: Context,
    private val accessibilityService: AccessibilityService,
    private val databaseManager: VoiceOSDatabaseManager,
    private val speechEngineManager: com.augmentalis.voiceoscore.accessibility.speech.SpeechEngineManager
) {
    companion object {
        private const val TAG = "ManualCommandIntegration"
    }

    // Coroutine scope for background operations
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Business logic manager
    private lateinit var commandManager: ElementCommandManager

    // UI overlays
    private var postLearningOverlay: PostLearningOverlay? = null
    private var qualityOverlay: QualityIndicatorOverlay? = null

    // State management
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private val _qualityOverlayVisible = MutableStateFlow(false)
    val qualityOverlayVisible: StateFlow<Boolean> = _qualityOverlayVisible.asStateFlow()

    /**
     * Initialize the integration.
     * Must be called from VoiceOSService.onCreate().
     */
    fun initialize() {
        if (_isInitialized.value) {
            Log.w(TAG, "Already initialized")
            return
        }

        try {
            // Initialize command manager with database repositories
            commandManager = ElementCommandManager(
                repository = databaseManager.elementCommands,
                qualityRepository = databaseManager.qualityMetrics
            )

            // Initialize overlays
            postLearningOverlay = PostLearningOverlay(
                context = context,
                accessibilityService = accessibilityService,
                commandManager = commandManager,
                speechEngineManager = speechEngineManager
            )

            qualityOverlay = QualityIndicatorOverlay(
                context = context,
                accessibilityService = accessibilityService,
                commandManager = commandManager
            )

            _isInitialized.value = true
            Log.i(TAG, "ManualCommandIntegration initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize ManualCommandIntegration", e)
        }
    }

    /**
     * Hook to be called when exploration completes.
     * Called from LearnAppIntegration.handleExplorationStateChange().
     *
     * @param state Exploration state (should be ExplorationState.Completed)
     */
    fun onExplorationCompleted(state: ExplorationState) {
        if (!_isInitialized.value) {
            Log.w(TAG, "Not initialized, ignoring exploration completion")
            return
        }

        if (state !is ExplorationState.Completed) {
            Log.w(TAG, "Called with non-completed state: ${state::class.simpleName}")
            return
        }

        scope.launch {
            try {
                // Check for elements needing commands
                val elementsNeedingCommands = commandManager.getElementsNeedingCommands(
                    appId = state.packageName
                )

                if (elementsNeedingCommands.isNotEmpty()) {
                    Log.i(TAG, "Found ${elementsNeedingCommands.size} elements needing commands for ${state.packageName}")

                    // Show post-learning overlay on main thread
                    withContext(Dispatchers.Main) {
                        postLearningOverlay?.show(
                            packageName = state.packageName,
                            elements = elementsNeedingCommands
                        )
                    }
                } else {
                    Log.i(TAG, "No elements needing commands for ${state.packageName}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error showing post-learning overlay", e)
            }
        }
    }

    /**
     * Show post-learning overlay for a specific app.
     * Can be called manually via voice command or UI.
     *
     * @param packageName Application package name
     */
    fun showPostLearningOverlay(packageName: String) {
        if (!_isInitialized.value) {
            Log.w(TAG, "Not initialized, cannot show post-learning overlay")
            return
        }

        scope.launch {
            try {
                val elementsNeedingCommands = commandManager.getElementsNeedingCommands(packageName)

                if (elementsNeedingCommands.isEmpty()) {
                    Log.i(TAG, "No elements needing commands for $packageName")
                    return@launch
                }

                withContext(Dispatchers.Main) {
                    postLearningOverlay?.show(packageName, elementsNeedingCommands)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error showing post-learning overlay", e)
            }
        }
    }

    /**
     * Toggle quality indicator overlay.
     * Called via voice commands: "show quality overlay" / "hide quality overlay"
     */
    fun toggleQualityOverlay() {
        if (!_isInitialized.value) {
            Log.w(TAG, "Not initialized, cannot toggle quality overlay")
            return
        }

        scope.launch(Dispatchers.Main) {
            try {
                if (_qualityOverlayVisible.value) {
                    qualityOverlay?.hide()
                    _qualityOverlayVisible.value = false
                    Log.i(TAG, "Quality overlay hidden")
                } else {
                    qualityOverlay?.show()
                    _qualityOverlayVisible.value = true
                    Log.i(TAG, "Quality overlay shown")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error toggling quality overlay", e)
            }
        }
    }

    /**
     * Show quality indicator overlay.
     * Called via voice command: "show quality overlay"
     */
    fun showQualityOverlay() {
        if (!_isInitialized.value) {
            Log.w(TAG, "Not initialized, cannot show quality overlay")
            return
        }

        scope.launch(Dispatchers.Main) {
            try {
                qualityOverlay?.show()
                _qualityOverlayVisible.value = true
                Log.i(TAG, "Quality overlay shown")
            } catch (e: Exception) {
                Log.e(TAG, "Error showing quality overlay", e)
            }
        }
    }

    /**
     * Hide quality indicator overlay.
     * Called via voice command: "hide quality overlay"
     */
    fun hideQualityOverlay() {
        if (!_isInitialized.value) {
            Log.w(TAG, "Not initialized, cannot hide quality overlay")
            return
        }

        scope.launch(Dispatchers.Main) {
            try {
                qualityOverlay?.hide()
                _qualityOverlayVisible.value = false
                Log.i(TAG, "Quality overlay hidden")
            } catch (e: Exception) {
                Log.e(TAG, "Error hiding quality overlay", e)
            }
        }
    }

    /**
     * Get the command manager instance.
     * Used by other components that need to interact with element commands.
     */
    fun getCommandManager(): ElementCommandManager? {
        return if (_isInitialized.value) commandManager else null
    }

    /**
     * Cleanup resources.
     * Must be called from VoiceOSService.onDestroy().
     */
    fun cleanup() {
        Log.i(TAG, "Cleaning up ManualCommandIntegration")

        try {
            // Hide overlays
            scope.launch(Dispatchers.Main) {
                postLearningOverlay?.hide()
                qualityOverlay?.hide()
            }

            // Clear caches
            scope.launch {
                commandManager.clearCaches()
            }

            // Cancel coroutine scope
            scope.cancel()

            _isInitialized.value = false
            _qualityOverlayVisible.value = false

            Log.i(TAG, "ManualCommandIntegration cleaned up successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }
    }
}
