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

package com.augmentalis.learnapp.integration

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.view.accessibility.AccessibilityEvent
import com.augmentalis.learnapp.database.LearnAppDatabase
import com.augmentalis.learnapp.database.repository.LearnAppRepository
import com.augmentalis.learnapp.detection.AppLaunchDetector
import com.augmentalis.learnapp.detection.LearnedAppTracker
import com.augmentalis.learnapp.exploration.DFSExplorationStrategy
import com.augmentalis.learnapp.exploration.ExplorationEngine
import com.augmentalis.learnapp.exploration.ExplorationStrategy
import com.augmentalis.learnapp.models.ExplorationState
import com.augmentalis.learnapp.ui.ConsentDialogManager
import com.augmentalis.learnapp.ui.ProgressOverlayManager
import com.augmentalis.uuidcreator.UUIDCreator
import com.augmentalis.uuidcreator.alias.UuidAliasManager
import com.augmentalis.uuidcreator.database.UUIDCreatorDatabase
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
    private val database: LearnAppDatabase
    private val repository: LearnAppRepository

    /**
     * UUIDCreator components
     */
    private val uuidCreator: UUIDCreator
    private val thirdPartyGenerator: ThirdPartyUuidGenerator
    private val aliasManager: UuidAliasManager

    /**
     * Exploration strategy
     */
    private val explorationStrategy: ExplorationStrategy = DFSExplorationStrategy()

    init {
        // Initialize databases
        database = LearnAppDatabase.getInstance(context)
        repository = LearnAppRepository(database.learnAppDao())

        // Initialize UUIDCreator components
        uuidCreator = UUIDCreator.getInstance()
        thirdPartyGenerator = ThirdPartyUuidGenerator(context)
        val uuidCreatorDatabase = UUIDCreatorDatabase.getInstance(context)
        aliasManager = UuidAliasManager(uuidCreatorDatabase)

        // Initialize LearnApp components
        learnedAppTracker = LearnedAppTracker(context)
        appLaunchDetector = AppLaunchDetector(context, learnedAppTracker)
        consentDialogManager = ConsentDialogManager(context, learnedAppTracker)
        progressOverlayManager = ProgressOverlayManager(context)

        // Initialize exploration engine
        explorationEngine = ExplorationEngine(
            accessibilityService = accessibilityService,
            uuidCreator = uuidCreator,
            thirdPartyGenerator = thirdPartyGenerator,
            aliasManager = aliasManager,
            strategy = explorationStrategy
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
                    is com.augmentalis.learnapp.detection.AppLaunchEvent.NewAppDetected -> {
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
                    is com.augmentalis.learnapp.ui.ConsentResponse.Approved -> {
                        // Start exploration
                        startExploration(response.packageName)
                    }
                    is com.augmentalis.learnapp.ui.ConsentResponse.Declined -> {
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
            repository.createExplorationSession(packageName)

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

            is ExplorationState.Running -> {
                // Show/update progress overlay
                if (!progressOverlayManager.isOverlayShowing()) {
                    progressOverlayManager.showProgressOverlay(
                        progress = state.progress,
                        onPause = { pauseExploration() },
                        onStop = { stopExploration() }
                    )
                } else {
                    progressOverlayManager.updateProgress(state.progress)
                }
            }

            is ExplorationState.PausedForLogin -> {
                // Show login prompt overlay
                // TODO: Implement login prompt overlay manager
            }

            is ExplorationState.PausedByUser -> {
                // Update UI to show paused state
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

            else -> {
                // Handle other states
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
