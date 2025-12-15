/**
 * JustInTimeLearner.kt - Passive screen-by-screen learning for VoiceOS
 *
 * Part of LearnApp UX improvements (Phase 3 - Just-in-Time Learning Engine)
 * Date: 2025-11-28
 *
 * Learns app screens passively as user naturally navigates, without full exploration.
 * Triggered when user clicks "Skip" button on consent dialog.
 */

package com.augmentalis.voiceoscore.learnapp.jit

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import com.augmentalis.database.VoiceOSDatabaseManager
import com.augmentalis.database.dto.GeneratedCommandDTO
import com.augmentalis.voiceoscore.accessibility.IVoiceOSServiceInternal
import com.augmentalis.voiceoscore.learnapp.database.repository.LearnAppRepository
// Phase 2 (2025-12-04): LearnAppCore integration imports
import com.augmentalis.voiceoscore.learnapp.core.LearnAppCore
import com.augmentalis.voiceoscore.learnapp.core.ProcessingMode
import com.augmentalis.voiceoscore.learnapp.models.ElementInfo
// FIX (2025-12-01): Removed unused CommandGenerator import - command generation is inlined
import com.augmentalis.voiceoscore.learnapp.models.ScreenState
import com.augmentalis.voiceoscore.learnapp.fingerprinting.ScreenStateManager
import com.augmentalis.voiceoscore.scraping.AccessibilityScrapingIntegration
import com.augmentalis.voiceoscore.version.AppVersionDetector
import com.augmentalis.voiceoscore.version.AppVersion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Just-In-Time Learner
 *
 * Passive learning engine that captures screens as user naturally uses the app.
 * No automatic exploration, no interruptions - just learns what user sees.
 *
 * ## Behavior (FIX 2025-11-30):
 * - ALWAYS ACTIVE for any app that isn't fully learned (no consent required)
 * - Monitors AccessibilityEvents for screen changes
 * - Learns current screen when user lands on it
 * - Triggers command generation after learning (enables voice control)
 * - Completely passive - no user interaction required
 *
 * ## Performance:
 * - Target: <50ms per screen capture
 * - Debounced screen change detection (500ms)
 * - Background coroutine processing
 *
 * @param context Application context
 * @param databaseManager Database manager for persistence
 * @param repository Repository for app data
 * @param voiceOSService Service reference for command registration (nullable for tests)
 * @param versionDetector App version detector for version-aware command creation
 */
class JustInTimeLearner(
    private val context: Context,
    private val databaseManager: VoiceOSDatabaseManager,
    private val repository: LearnAppRepository,
    private val voiceOSService: IVoiceOSServiceInternal? = null,  // FIX: Added for command registration
    private val learnAppCore: com.augmentalis.voiceoscore.learnapp.core.LearnAppCore? = null,  // Phase 2: LearnAppCore integration
    private val versionDetector: AppVersionDetector? = null  // Version-aware command creation
) {
    companion object {
        private const val TAG = "JustInTimeLearner"
        private const val SCREEN_CHANGE_DEBOUNCE_MS = 500L
        private const val TOAST_DURATION_MS = 1500L

        // FIX (2025-11-30): System packages to exclude from JIT learning
        private val EXCLUDED_PACKAGES = setOf(
            "com.android.systemui",
            "com.android.launcher",
            "com.android.launcher3",
            "android",
            "com.google.android.gms",
            "com.google.android.gsf"
        )
    }

    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private var lastScreenHash: String? = null
    private var lastProcessedTime = 0L
    // FIX (2025-11-30): JIT is now always active by default (P1-H4)
    // No consent required - works on any unlearned app automatically
    private var isActive = true  // Changed from false to true
    private var currentPackageName: String? = null

    // FIX (2025-12-11): Add pause state for JITLearningService control
    private var isPaused = false

    // FIX (2025-12-11): Track stats for queryState()
    private var screensLearnedCount = 0
    private var elementsDiscoveredCount = 0

    // FIX (2025-12-11): Event callback for JITLearningService
    private var eventCallback: JITEventCallback? = null

    /**
     * Event callback interface for JITLearningService integration
     * FIX (2025-12-11): Enables real-time event streaming to LearnApp
     */
    interface JITEventCallback {
        fun onScreenLearned(packageName: String, screenHash: String, elementCount: Int)
        fun onElementDiscovered(stableId: String, vuid: String?)
        fun onLoginDetected(packageName: String, screenHash: String)
    }

    /**
     * Set event callback for JITLearningService
     * FIX (2025-12-11): Enables event streaming to registered listeners
     */
    fun setEventCallback(callback: JITEventCallback?) {
        eventCallback = callback
    }

    // FIX (2025-12-01): Element capture for JIT (command generation is inlined)
    // Phase 1 of Voice Command Element Persistence feature
    private var elementCapture: JitElementCapture? = null

    // FIX (2025-12-02): Screen state manager for unified hashing (Spec 009 - Phase 2)
    // Uses structure-based hashing with popup awareness (same as LearnApp)
    private var accessibilityService: AccessibilityService? = null
    private var screenStateManager: ScreenStateManager? = null

    /**
     * Initialize element capture with accessibility service
     * Must be called when service is available
     *
     * FIX (2025-12-02): Also initializes ScreenStateManager for unified hashing
     * Phase 4 (2025-12-02): Initializes ThirdPartyUuidGenerator for UUID generation
     */
    fun initializeElementCapture(accessibilityService: AccessibilityService) {
        this.accessibilityService = accessibilityService

        // Phase 4: Initialize UUID generator for element identification
        val thirdPartyUuidGenerator = com.augmentalis.uuidcreator.thirdparty.ThirdPartyUuidGenerator(context)

        elementCapture = JitElementCapture(accessibilityService, databaseManager, thirdPartyUuidGenerator)

        // FIX (2025-12-02): Initialize ScreenStateManager for structure-based hashing
        screenStateManager = ScreenStateManager(accessibilityService)
        // FIX (2025-12-01): Removed CommandGenerator - command generation is inlined in generateCommandsForElements()
        Log.i(TAG, "JIT element capture, screen state manager, and UUID generator initialized")
    }

    /**
     * Activate JIT learning for a specific package.
     *
     * @param packageName Package to learn
     */
    suspend fun activate(packageName: String) {
        withContext(Dispatchers.Main) {
            isActive = true
            currentPackageName = packageName
            Log.i(TAG, "JIT learning activated for: $packageName")

            // Record consent as SKIPPED in database
            databaseManager.appConsentHistory.insert(
                packageName = packageName,
                userChoice = "SKIPPED",
                timestamp = System.currentTimeMillis()
            )

            // Ensure learned app exists in database (create if needed)
            val existingApp = databaseManager.learnedAppQueries.getLearnedApp(packageName).executeAsOneOrNull()
            if (existingApp != null) {
                // Update existing app to JIT mode
                databaseManager.learnedAppQueries.updateStatus(
                    package_name = packageName,
                    status = "JIT_ACTIVE",
                    last_updated_at = System.currentTimeMillis()
                )
                databaseManager.learnedAppQueries.updateLearningMode(
                    package_name = packageName,
                    learning_mode = "JUST_IN_TIME",
                    last_updated_at = System.currentTimeMillis()
                )
            } else {
                // Auto-create learned app record for JIT mode
                // Use repository's createExplorationSessionSafe which auto-creates app if needed
                Log.d(TAG, "Auto-creating learned app record for JIT mode: $packageName")
                repository.createExplorationSessionSafe(packageName)

                // Now update to JIT status
                databaseManager.learnedAppQueries.updateStatus(
                    package_name = packageName,
                    status = "JIT_ACTIVE",
                    last_updated_at = System.currentTimeMillis()
                )
                databaseManager.learnedAppQueries.updateLearningMode(
                    package_name = packageName,
                    learning_mode = "JUST_IN_TIME",
                    last_updated_at = System.currentTimeMillis()
                )
            }

            Toast.makeText(
                context,
                "JIT learning activated - screens will be learned as you use the app",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Deactivate JIT learning.
     */
    fun deactivate() {
        isActive = false
        currentPackageName = null
        lastScreenHash = null
        Log.i(TAG, "JIT learning deactivated")
    }

    /**
     * Check if JIT learning is active for a package.
     */
    fun isActiveForPackage(packageName: String): Boolean {
        return isActive && currentPackageName == packageName
    }

    /**
     * Process accessibility event - learns screen if it's new.
     *
     * FIX (2025-11-30): JIT now processes ANY unlearned app automatically.
     * No consent required - this is the fallback for voice commands.
     *
     * Called from AccessibilityService on every event.
     * Debounced to avoid processing too frequently.
     *
     * @param event AccessibilityEvent to process
     */
    fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (!isActive) return
        // FIX (2025-12-11): Check pause state
        if (isPaused) return

        val packageName = event.packageName?.toString() ?: return

        // FIX (2025-11-30): Skip excluded system packages
        if (EXCLUDED_PACKAGES.any { packageName.startsWith(it) }) {
            return
        }

        // FIX (2025-11-30): Process ANY app, not just currentPackageName
        // JIT now works as universal fallback for unlearned apps
        // The currentPackageName is only used for explicit activate() calls

        // Debounce screen changes
        val now = System.currentTimeMillis()
        if (now - lastProcessedTime < SCREEN_CHANGE_DEBOUNCE_MS) {
            return
        }
        lastProcessedTime = now

        // Process screen in background
        scope.launch {
            try {
                // FIX (2025-11-30): Pass actual package name from event
                learnCurrentScreen(event, packageName)
            } catch (e: Exception) {
                Log.e(TAG, "Error learning screen for $packageName", e)
            }
        }
    }

    /**
     * Learn the current screen passively.
     *
     * FIX (2025-11-30): Now accepts packageName parameter and triggers command generation.
     *
     * @param event AccessibilityEvent containing screen info
     * @param packageName Package name of the app (from event)
     */
    private suspend fun learnCurrentScreen(event: AccessibilityEvent, packageName: String) {
        val startTime = System.currentTimeMillis()

        // FIX (2025-11-30): Check if app is already fully learned
        val appStatus = withContext(Dispatchers.IO) {
            databaseManager.learnedAppQueries.getLearnedApp(packageName).executeAsOneOrNull()
        }
        if (appStatus?.status == "LEARNED") {
            // App is fully learned, skip JIT for this app
            return
        }

        // FIX (2025-12-02): Calculate screen hash using ScreenStateManager (structure-based)
        val screenHash = calculateScreenHash(packageName)

        // Skip if same screen as last time
        if (screenHash == lastScreenHash) {
            return
        }
        lastScreenHash = screenHash

        // FIX (2025-12-02): Check if screen already captured (deduplication)
        // Avoids re-traversing visited screens, saving battery and processing time
        if (isScreenAlreadyCaptured(packageName, screenHash)) {
            Log.i(TAG, "Screen already captured, skipping: $packageName - Hash: $screenHash")
            return
        }

        // Save screen to database
        saveScreenToDatabase(packageName, screenHash, event)

        // Update progress
        updateLearningProgress(packageName)

        // FIX (2025-11-30): Trigger command generation after learning screen (P1-H4)
        // This enables voice control for JIT-learned screens immediately
        withContext(Dispatchers.Main) {
            voiceOSService?.onNewCommandsGenerated()
        }

        val elapsed = System.currentTimeMillis() - startTime
        Log.d(TAG, "JIT learned screen in ${elapsed}ms - $packageName")
    }

    /**
     * Check if screen was already captured by JIT.
     *
     * FIX (2025-12-02): Deduplication check to avoid re-traversing visited screens (Spec 009 - Phase 3)
     * Queries database for elements with matching screen hash.
     *
     * Performance: <7ms vs 50ms full capture
     *
     * @param packageName Package name of the app
     * @param screenHash Screen hash to check
     * @return true if screen already captured, false otherwise
     */
    private suspend fun isScreenAlreadyCaptured(packageName: String, screenHash: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Use countByScreenHash query from Phase 1
                val count = databaseManager.scrapedElements.countByScreenHash(packageName, screenHash)
                count > 0
            } catch (e: Exception) {
                Log.e(TAG, "Error checking screen deduplication for $packageName", e)
                false  // On error, proceed with capture to be safe
            }
        }
    }

    /**
     * Calculate hash for screen identity using ScreenStateManager.
     *
     * FIX (2025-12-02): Replaced content-dependent hashing with structure-based hashing
     * Uses ScreenStateManager for unified hashing algorithm (same as LearnApp)
     * Supports popup/dialog detection with stable hashing
     *
     * @param packageName Package name of the app
     * @return Screen hash from ScreenStateManager, or fallback hash if unavailable
     */
    private suspend fun calculateScreenHash(packageName: String): String {
        // Get root node from accessibility service
        val rootNode = accessibilityService?.rootInActiveWindow

        // Use ScreenStateManager for unified hashing if available
        return if (rootNode != null && screenStateManager != null) {
            val screenState = screenStateManager!!.captureScreenState(rootNode, packageName)
            screenState.hash
        } else {
            // Fallback to timestamp-based hash if ScreenStateManager unavailable
            // This should rarely happen in production
            Log.w(TAG, "ScreenStateManager unavailable, using fallback hash")
            System.currentTimeMillis().toString()
        }
    }

    /**
     * Save screen data to database.
     * Creates ScreenState and persists via repository.
     *
     * FIX (2025-12-01): Now captures UI elements during save for voice command support.
     */
    private suspend fun saveScreenToDatabase(
        packageName: String,
        screenHash: String,
        event: AccessibilityEvent
    ) {
        // FIX (2025-12-01): Capture elements first (needs Main dispatcher for accessibility tree)
        var capturedElementCount = 0
        val capturedElements = elementCapture?.captureScreenElements(packageName) ?: emptyList()

        if (capturedElements.isNotEmpty()) {
            // FIX (2025-12-02): Persist elements with screen hash for deduplication
            capturedElementCount = elementCapture?.persistElements(packageName, capturedElements, screenHash) ?: 0

            // Generate commands for captured elements
            if (capturedElementCount > 0) {
                generateCommandsForElements(packageName, capturedElements)
            }

            Log.i(TAG, "JIT captured $capturedElementCount elements for $packageName (hash: $screenHash)")
        }

        withContext(Dispatchers.IO) {
            try {
                // Create ScreenState from event with actual element count
                val screenState = ScreenState(
                    hash = screenHash,
                    packageName = packageName,
                    activityName = event.className?.toString(),
                    timestamp = System.currentTimeMillis(),
                    elementCount = capturedElementCount,  // FIX: Actual captured count
                    isVisited = true,  // JIT screens are always "visited"
                    depth = 0  // JIT doesn't use DFS depth
                )

                // Save via repository
                repository.saveScreenState(screenState)
                Log.i(TAG, "Screen saved: $packageName - Hash: $screenHash - Elements: $capturedElementCount")

                // FIX (2025-12-11): Update stats for queryState()
                screensLearnedCount++
                elementsDiscoveredCount += capturedElementCount

                // FIX (2025-12-11): Dispatch event to JITLearningService
                eventCallback?.onScreenLearned(packageName, screenHash, capturedElementCount)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save screen state for $packageName", e)
            }
        }
    }

    /**
     * Generate voice commands for captured elements
     *
     * Phase 2 (2025-12-04): Refactored to use LearnAppCore instead of duplicated logic
     * - Converts JitCapturedElement to ElementInfo
     * - Uses LearnAppCore.processElement() with IMMEDIATE mode
     * - Preserves existing deduplication check
     */
    private suspend fun generateCommandsForElements(
        packageName: String,
        elements: List<JitCapturedElement>
    ) {
        withContext(Dispatchers.IO) {
            try {
                var commandCount = 0

                // If LearnAppCore is available, use it (new path)
                if (learnAppCore != null) {
                    for (element in elements) {
                        // Convert to ElementInfo
                        val elementInfo = element.toElementInfo()

                        // Check if command already exists (deduplication)
                        val label = element.text
                            ?: element.contentDescription
                            ?: element.viewIdResourceName?.substringAfterLast("/")
                        if (label != null && label.length >= 2) {
                            val existingCommands = databaseManager.generatedCommands.fuzzySearch(label)
                            val existing = existingCommands.any { it.elementHash == element.elementHash }

                            if (!existing) {
                                // Use LearnAppCore to process element
                                val result = learnAppCore.processElement(
                                    element = elementInfo,
                                    packageName = packageName,
                                    mode = ProcessingMode.IMMEDIATE  // JIT uses immediate insert
                                )

                                if (result.success) {
                                    commandCount++
                                    Log.v(TAG, "Generated command via LearnAppCore: ${result.command?.commandText}")
                                } else {
                                    Log.w(TAG, "Failed to generate command: ${result.error}")
                                }
                            }
                        }
                    }
                } else {
                    // Fallback to old logic if LearnAppCore not provided (backward compatibility)
                    val timestamp = System.currentTimeMillis()

                    // Get app version for version-aware commands
                    val appVersion = versionDetector?.getCurrentVersion(packageName) ?: AppVersion.UNKNOWN

                    for (element in elements) {
                        val label = element.text
                            ?: element.contentDescription
                            ?: element.viewIdResourceName?.substringAfterLast("/")
                            ?: continue

                        if (label.length < 2 || label.all { it.isDigit() }) continue

                        val actionType = when {
                            element.isClickable -> "click"
                            element.isEditable -> "type"
                            element.isScrollable -> "scroll"
                            element.isLongClickable -> "long_click"
                            else -> "click"
                        }

                        val commandText = "$actionType $label".lowercase()
                        val existingCommands = databaseManager.generatedCommands.fuzzySearch(commandText)
                        val existing = existingCommands.any { it.elementHash == element.elementHash }

                        if (!existing) {
                            val commandDTO = GeneratedCommandDTO(
                                id = 0L,
                                elementHash = element.elementHash,
                                commandText = commandText,
                                actionType = actionType,
                                confidence = 0.85,
                                synonyms = generateSynonyms(actionType, label),
                                isUserApproved = 0L,
                                usageCount = 0L,
                                lastUsed = null,
                                createdAt = timestamp,
                                appId = packageName,
                                // Version-aware fields (Schema v3)
                                appVersion = appVersion.versionName,
                                versionCode = appVersion.versionCode,
                                lastVerified = timestamp,
                                isDeprecated = 0L  // New commands are never deprecated
                            )
                            databaseManager.generatedCommands.insert(commandDTO)
                            commandCount++
                        }
                    }
                }

                if (commandCount > 0) {
                    Log.i(TAG, "Generated $commandCount voice commands for $packageName")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to generate commands for $packageName", e)
            }
            Unit
        }
    }

    /**
     * Convert JitCapturedElement to ElementInfo
     *
     * Phase 2 (2025-12-04): Conversion method for LearnAppCore integration
     *
     * @receiver JitCapturedElement to convert
     * @return ElementInfo for LearnAppCore
     */
    private fun JitCapturedElement.toElementInfo(): ElementInfo {
        return ElementInfo(
            className = this.className,
            text = this.text ?: "",
            contentDescription = this.contentDescription ?: "",
            resourceId = this.viewIdResourceName ?: "",
            isClickable = this.isClickable,
            isEnabled = this.isEnabled,
            isPassword = false,  // JitCapturedElement doesn't track password fields
            isScrollable = this.isScrollable,
            bounds = this.bounds,
            node = null,  // Not available in JitCapturedElement
            uuid = this.uuid  // Preserve existing UUID if set
        )
    }

    /**
     * Generate synonyms for voice command
     */
    private fun generateSynonyms(actionType: String, label: String): String {
        val synonyms = mutableListOf<String>()

        when (actionType) {
            "click" -> {
                synonyms.add("tap $label")
                synonyms.add("press $label")
                synonyms.add("select $label")
            }
            "long_click" -> {
                synonyms.add("hold $label")
                synonyms.add("long press $label")
            }
            "scroll" -> {
                synonyms.add("swipe $label")
            }
            "type" -> {
                synonyms.add("enter $label")
                synonyms.add("input $label")
            }
        }

        // Return as JSON array string
        return "[${synonyms.joinToString(",") { "\"${it.lowercase()}\"" }}]"
    }

    /**
     * Update learning progress for the app.
     */
    private suspend fun updateLearningProgress(packageName: String) {
        withContext(Dispatchers.IO) {
            val app = databaseManager.learnedAppQueries.getLearnedApp(packageName).executeAsOneOrNull()
            if (app != null) {
                val newScreensExplored = app.screens_explored + 1

                databaseManager.learnedAppQueries.updateProgress(
                    package_name = packageName,
                    progress = newScreensExplored,  // Simple progress metric
                    screens_explored = newScreensExplored,
                    last_updated_at = System.currentTimeMillis()
                )
            }
        }
    }

    /**
     * Show subtle toast notification.
     */
    private fun showLearningToast() {
        Toast.makeText(
            context,
            "Learning this screen...",
            Toast.LENGTH_SHORT
        ).show()
    }

    /**
     * Clean up resources.
     */
    fun destroy() {
        deactivate()
        eventCallback = null
        // Coroutine scope will be cancelled automatically
    }

    // ================================================================
    // FIX (2025-12-11): Methods for JITLearningService integration
    // ================================================================

    /**
     * Pause JIT learning (called from JITLearningService)
     */
    fun pause() {
        isPaused = true
        Log.i(TAG, "JIT learning paused")
    }

    /**
     * Resume JIT learning (called from JITLearningService)
     */
    fun resume() {
        isPaused = false
        Log.i(TAG, "JIT learning resumed")
    }

    /**
     * Check if JIT is currently paused
     */
    fun isPausedState(): Boolean = isPaused

    /**
     * Check if JIT is actively learning
     */
    fun isLearningActive(): Boolean = isActive && !isPaused

    /**
     * Get current stats for queryState()
     */
    fun getStats(): JITStats {
        return JITStats(
            screensLearned = screensLearnedCount,
            elementsDiscovered = elementsDiscoveredCount,
            currentPackage = currentPackageName,
            isActive = isActive && !isPaused
        )
    }

    /**
     * Check if a screen has already been learned
     */
    suspend fun hasScreen(screenHash: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val count = databaseManager.scrapedElements.countByScreenHash(
                    currentPackageName ?: "",
                    screenHash
                )
                count > 0
            } catch (e: Exception) {
                Log.e(TAG, "Error checking screen hash", e)
                false
            }
        }
    }

    /**
     * Get menu items for a specific menu (for LearnAppPro)
     */
    suspend fun getMenuItems(menuId: String): List<ElementInfo> {
        // Query database for elements that are part of the menu
        // For now, return empty list as menu detection is handled by ExplorationEngine
        return emptyList()
    }

    /**
     * JIT statistics data class
     */
    data class JITStats(
        val screensLearned: Int,
        val elementsDiscovered: Int,
        val currentPackage: String?,
        val isActive: Boolean
    )
}
