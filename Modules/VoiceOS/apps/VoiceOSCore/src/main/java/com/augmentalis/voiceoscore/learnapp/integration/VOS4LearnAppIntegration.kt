/**
 * VOS4LearnAppIntegration.kt - Integration adapter for VOS4 (NOT WIRED)
 * Path: libraries/UUIDCreator/src/main/java/com/augmentalis/learnapp/integration/VOS4LearnAppIntegration.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-08
 *
 * Integration adapter for wiring LearnApp into VOS4
 *
 * IMPORTANT: This file is NOT wired into VOS4. It provides the integration interface only.
 * See VOS4-INTEGRATION-GUIDE.md for wiring instructions.
 */

package com.augmentalis.voiceoscore.learnapp.integration

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.view.accessibility.AccessibilityEvent
import com.augmentalis.voiceoscore.learnapp.database.LearnAppDatabaseAdapter
import com.augmentalis.voiceoscore.learnapp.database.repository.LearnAppRepository
import com.augmentalis.voiceoscore.learnapp.detection.AppLaunchDetector
import com.augmentalis.voiceoscore.learnapp.detection.LearnedAppTracker
import com.augmentalis.voiceoscore.learnapp.exploration.DFSExplorationStrategy
import com.augmentalis.voiceoscore.learnapp.exploration.ExplorationEngine
import com.augmentalis.voiceoscore.learnapp.exploration.ExplorationStrategy
import com.augmentalis.voiceoscore.learnapp.models.ExplorationState
import com.augmentalis.voiceoscore.learnapp.ui.ConsentDialogManager
import com.augmentalis.voiceoscore.learnapp.ui.ProgressOverlayManager
import com.augmentalis.uuidcreator.VUIDCreator
import com.augmentalis.uuidcreator.alias.UuidAliasManager
import com.augmentalis.uuidcreator.thirdparty.ThirdPartyUuidGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * VOS4 LearnApp Integration
 *
 * Central integration adapter for wiring LearnApp into VOS4.
 * Provides unified API for all LearnApp functionality.
 *
 * ## Integration Pattern (NOT YET IMPLEMENTED)
 *
 * ```kotlin
 * // In VOS4 Application.onCreate()
 * class VOS4Application : Application() {
 *     lateinit var learnAppIntegration: VOS4LearnAppIntegration
 *
 *     override fun onCreate() {
 *         super.onCreate()
 *         learnAppIntegration = VOS4LearnAppIntegration.initialize(this)
 *     }
 * }
 *
 * // In VOS4 AccessibilityService
 * class VOS4AccessibilityService : AccessibilityService() {
 *     private val integration by lazy {
 *         (application as VOS4Application).learnAppIntegration
 *     }
 *
 *     override fun onAccessibilityEvent(event: AccessibilityEvent) {
 *         integration.onAccessibilityEvent(event)
 *     }
 * }
 * ```
 *
 * @property context Application context
 * @property accessibilityService Accessibility service instance
 *
 * @since 1.0.0
 */
class VOS4LearnAppIntegration private constructor(
    private val context: Context,
    private val accessibilityService: AccessibilityService
) {

    /**
     * Coroutine scope
     */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * Core components
     */
    private val learnedAppTracker: LearnedAppTracker
    private val appLaunchDetector: AppLaunchDetector
    private val consentDialogManager: ConsentDialogManager
    private val progressOverlayManager: ProgressOverlayManager
    private val explorationEngine: ExplorationEngine

    /**
     * Database and repository
     */
    private val databaseAdapter: LearnAppDatabaseAdapter
    private val databaseManager: com.augmentalis.database.VoiceOSDatabaseManager
    private val repository: LearnAppRepository

    /**
     * VUIDCreator components
     */
    private val vuidCreator: VUIDCreator
    private val thirdPartyGenerator: ThirdPartyUuidGenerator
    private val aliasManager: UuidAliasManager

    /**
     * Exploration strategy
     */
    private val explorationStrategy: ExplorationStrategy = DFSExplorationStrategy()

    init {
        // Initialize databases
        databaseAdapter = LearnAppDatabaseAdapter.getInstance(context)
        databaseManager = com.augmentalis.database.VoiceOSDatabaseManager.getInstance(
            com.augmentalis.database.DatabaseDriverFactory(context)
        )
        repository = LearnAppRepository(databaseManager, context)

        // Initialize VUIDCreator components
        vuidCreator = VUIDCreator.getInstance()
        thirdPartyGenerator = ThirdPartyUuidGenerator(context)
        aliasManager = UuidAliasManager(databaseManager.uuids)

        // Initialize LearnApp components
        learnedAppTracker = LearnedAppTracker(context)
        appLaunchDetector = AppLaunchDetector(context, learnedAppTracker)
        consentDialogManager = ConsentDialogManager(accessibilityService, learnedAppTracker)
        progressOverlayManager = ProgressOverlayManager(accessibilityService)

        // Initialize exploration engine
        explorationEngine = ExplorationEngine(
            context = context,
            accessibilityService = accessibilityService,
            uuidCreator = vuidCreator,
            thirdPartyGenerator = thirdPartyGenerator,
            aliasManager = aliasManager,
            repository = repository,
            databaseManager = databaseManager,
            strategy = explorationStrategy,
            learnAppCore = null
        )

        // Set up event listeners
        setupEventListeners()
    }

    /**
     * Setup event listeners
     */
    private fun setupEventListeners() {
        // Listen for app launch events
        scope.launch {
            appLaunchDetector.appLaunchEvents.collect { event ->
                when (event) {
                    is com.augmentalis.voiceoscore.learnapp.detection.AppLaunchEvent.NewAppDetected -> {
                        // Show consent dialog
                        consentDialogManager.showConsentDialog(
                            packageName = event.packageName,
                            appName = event.appName
                        )
                    }
                    else -> {
                        // Handle other events
                    }
                }
            }
        }

        // Listen for consent responses
        scope.launch {
            consentDialogManager.consentResponses.collect { response ->
                when (response) {
                    is com.augmentalis.voiceoscore.learnapp.ui.ConsentResponse.Approved -> {
                        // Start exploration
                        startExploration(response.packageName)
                    }
                    is com.augmentalis.voiceoscore.learnapp.ui.ConsentResponse.Declined -> {
                        // Do nothing
                    }
                    is com.augmentalis.voiceoscore.learnapp.ui.ConsentResponse.Skipped -> {
                        // Do nothing
                    }
                    is com.augmentalis.voiceoscore.learnapp.ui.ConsentResponse.Dismissed -> {
                        // Do nothing
                    }
                    is com.augmentalis.voiceoscore.learnapp.ui.ConsentResponse.Timeout -> {
                        // Do nothing
                    }
                }
            }
        }

        // Listen for exploration state changes
        scope.launch {
            explorationEngine.explorationState.collect { state ->
                handleExplorationStateChange(state)
            }
        }
    }

    /**
     * Handle accessibility event
     *
     * Call this from VOS4 AccessibilityService.onAccessibilityEvent()
     *
     * @param event Accessibility event
     */
    fun onAccessibilityEvent(event: AccessibilityEvent) {
        appLaunchDetector.onAccessibilityEvent(event)
    }

    /**
     * Start exploration of app
     *
     * @param packageName Package name to explore
     */
    private fun startExploration(packageName: String) {
        scope.launch {
            // Create session
            repository.createExplorationSessionSafe(packageName)

            // Start exploration engine
            explorationEngine.startExploration(packageName)
        }
    }

    /**
     * Handle exploration state change
     *
     * @param state New exploration state
     */
    private fun handleExplorationStateChange(state: ExplorationState) {
        when (state) {
            is ExplorationState.Idle -> {
                // Hide progress overlay
                progressOverlayManager.hideProgressOverlay()
            }

            is ExplorationState.ConsentRequested -> {
                // Consent dialog already shown by listener
            }

            is ExplorationState.ConsentCancelled -> {
                // User declined consent
            }

            is ExplorationState.Preparing -> {
                // Preparing to start exploration
            }

            is ExplorationState.Running -> {
                // Show/update progress overlay
                val progressPercent = (state.progress.calculatePercentage() * 100).toInt()
                val message = "Learning ${state.progress.appName}... $progressPercent%"
                if (!progressOverlayManager.isOverlayShowing()) {
                    progressOverlayManager.showProgressOverlay(message)
                } else {
                    progressOverlayManager.updateProgress(progressPercent, message)
                }
            }

            is ExplorationState.PausedForLogin -> {
                // Show login prompt overlay
                // TODO: Implement login prompt overlay manager
            }

            is ExplorationState.PausedByUser -> {
                // Update UI to show paused state
            }

            is ExplorationState.Paused -> {
                // Paused for other reason
            }

            is ExplorationState.Completed -> {
                // Hide progress overlay
                progressOverlayManager.hideProgressOverlay()

                // Save results
                scope.launch {
                    repository.saveLearnedApp(
                        packageName = state.packageName,
                        appName = state.stats.appName,
                        versionCode = 1L,  // TODO: get from PackageManager
                        versionName = "1.0",  // TODO: get from PackageManager
                        stats = state.stats
                    )
                }
            }

            is ExplorationState.Failed -> {
                // Hide progress overlay
                progressOverlayManager.hideProgressOverlay()

                // Show error notification
                // TODO: Implement error notification
            }
        }
    }

    /**
     * Pause exploration
     */
    fun pauseExploration() {
        explorationEngine.pauseExploration()
    }

    /**
     * Resume exploration
     */
    fun resumeExploration() {
        explorationEngine.resumeExploration()
    }

    /**
     * Stop exploration
     */
    fun stopExploration() {
        explorationEngine.stopExploration()
    }

    /**
     * Get exploration state flow
     *
     * @return Exploration state flow
     */
    fun getExplorationState(): StateFlow<ExplorationState> {
        return explorationEngine.explorationState
    }

    /**
     * Cleanup (call in onDestroy)
     */
    fun cleanup() {
        consentDialogManager.cleanup()
        progressOverlayManager.cleanup()
    }

    companion object {
        @Volatile
        private var INSTANCE: VOS4LearnAppIntegration? = null

        /**
         * Initialize integration
         *
         * Call this from VOS4 Application.onCreate()
         *
         * @param context Application context
         * @param accessibilityService Accessibility service instance
         * @return Integration instance
         */
        fun initialize(
            context: Context,
            accessibilityService: AccessibilityService
        ): VOS4LearnAppIntegration {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: VOS4LearnAppIntegration(
                    context.applicationContext,
                    accessibilityService
                ).also {
                    INSTANCE = it
                }
            }
        }

        /**
         * Get integration instance
         *
         * @return Integration instance
         * @throws IllegalStateException if not initialized
         */
        fun getInstance(): VOS4LearnAppIntegration {
            return INSTANCE ?: throw IllegalStateException(
                "VOS4LearnAppIntegration not initialized. Call initialize(context, service) first."
            )
        }
    }
}
