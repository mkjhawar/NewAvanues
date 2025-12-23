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
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import java.security.MessageDigest
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
import com.augmentalis.voiceoscore.version.ScreenHashCalculator
import com.augmentalis.database.dto.ScrapedElementDTO
// Phase 5 (2025-12-22): JIT→Lite progression imports
import com.augmentalis.voiceoscore.learnapp.subscription.FeatureGateManager
import com.augmentalis.voiceoscore.learnapp.subscription.LearningMode
import com.augmentalis.voiceoscore.learnapp.subscription.FeatureGateResult
import com.augmentalis.voiceoscore.learnapp.ui.DeepScanConsentManager
import com.augmentalis.voiceoscore.learnapp.detection.ExpandableControlDetector
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
 * @param screenHashCalculator Screen hash calculator for rescan optimization (Phase 2 Task 1.1)
 * @param featureGateManager Feature gate manager for subscription checks (Phase 5: JIT→Lite progression)
 * @param deepScanConsentManager Deep scan consent manager for user consent (Phase 5: JIT→Lite progression)
 */
class JustInTimeLearner(
    private val context: Context,
    private val databaseManager: VoiceOSDatabaseManager,
    private val repository: LearnAppRepository,
    private val voiceOSService: IVoiceOSServiceInternal? = null,  // FIX: Added for command registration
    private val learnAppCore: com.augmentalis.voiceoscore.learnapp.core.LearnAppCore? = null,  // Phase 2: LearnAppCore integration
    private val versionDetector: AppVersionDetector? = null,  // Version-aware command creation
    private val screenHashCalculator: ScreenHashCalculator = ScreenHashCalculator,  // P2 Task 1.1: Hash-based rescan optimization
    private val featureGateManager: FeatureGateManager? = null,  // Phase 5: JIT→Lite progression
    private val deepScanConsentManager: DeepScanConsentManager? = null  // Phase 5: JIT→Lite progression
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

    // P2 Task 1.1: Hash-based rescan optimization metrics
    private var totalScreensProcessed = 0
    private var screensSkippedByHash = 0
    private var screensRescanned = 0

    // Phase 5 (2025-12-22): Track deep scanned screens to avoid re-asking
    // Maps "packageName:screenHash" to true when deep scan completed
    private val deepScannedScreens = mutableSetOf<String>()

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
        screenStateManager = ScreenStateManager()
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
     * ENHANCED (2025-12-22): Hash-based deduplication with app version validation
     * - Step 1: Calculate screen hash (cheap operation)
     * - Step 2: Check database for existing screen with matching hash
     * - Step 3: If exists AND version matches: Load from cache, skip scraping
     * - Step 4: If exists BUT version changed: Mark for rescan
     * - Step 5: If new: Full scrape with VUID-based element deduplication
     *
     * Battery savings: ~80% skip rate for unchanged screens
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

        // 1. Calculate screen hash first (cheap operation)
        val currentHash = calculateScreenHash(packageName)

        // Skip if same screen as last time
        if (currentHash == lastScreenHash) {
            return
        }
        lastScreenHash = currentHash

        // 2. Check database for existing screen with matching hash
        val existingScreen = getScreenByHash(currentHash, packageName)

        if (existingScreen != null) {
            // 3. Validate app version
            val currentVersion = versionDetector?.getVersion(packageName)?.versionName

            if (existingScreen.appVersion == currentVersion) {
                // FAST PATH: Load from cache, skip scraping
                val elapsed = System.currentTimeMillis() - startTime
                Log.d(TAG, "Screen $currentHash already learned (v${currentVersion}) - loading from DB in ${elapsed}ms")

                loadCommandsFromCache(existingScreen)

                // Still check for hidden menus (Lite upgrade opportunity)
                checkForHiddenMenus(packageName)

                // Update stats and metrics
                totalScreensProcessed++
                screensSkippedByHash++
                screensLearnedCount++
                eventCallback?.onScreenLearned(packageName, currentHash, existingScreen.elementCount)

                val skipRate = getSkipPercentage()
                Log.i(TAG, "Hash-based skip achieved ${elapsed}ms savings [Skip rate: ${String.format("%.1f", skipRate)}%]")

                return
            } else {
                // Version changed - rescan
                totalScreensProcessed++
                screensRescanned++
                Log.i(TAG, "App version changed: ${existingScreen.appVersion} → $currentVersion - rescanning screen $currentHash")
            }
        }

        // 4. NEW SCREEN: Full scrape with VUID deduplication
        // Capture elements for new/changed screens
        val capturedElements = elementCapture?.captureScreenElements(packageName) ?: emptyList()

        // VUID-based deduplication: Filter out elements already in database
        val newElements = deduplicateByVUID(capturedElements, packageName)
        val deduplicatedCount = capturedElements.size - newElements.size

        if (deduplicatedCount > 0) {
            Log.i(TAG, "VUID deduplication: filtered $deduplicatedCount/${capturedElements.size} existing elements")
        }

        // Screen is new or changed - proceed with full processing
        if (existingScreen == null) {
            totalScreensProcessed++
            screensRescanned++
        }
        Log.i(TAG, "Processing new/changed screen: $packageName - Hash: $currentHash - Elements: ${newElements.size}")

        // Save screen to database with deduplicated elements
        saveScreenToDatabase(packageName, currentHash, event, newElements)

        // Update progress
        updateLearningProgress(packageName)

        // FIX (2025-11-30): Trigger command generation after learning screen (P1-H4)
        // This enables voice control for JIT-learned screens immediately
        withContext(Dispatchers.Main) {
            voiceOSService?.onNewCommandsGenerated()
        }

        // Phase 5 (2025-12-22): Check for hidden menus/drawers after screen learning
        // This enables seamless JIT→Lite progression when user has subscription
        checkForHiddenMenus(packageName, currentHash)

        val elapsed = System.currentTimeMillis() - startTime
        Log.d(TAG, "JIT learned screen in ${elapsed}ms - $packageName [Battery savings from deduplication: ${deduplicatedCount} elements]")
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
     * ENHANCED (2025-12-22): Hybrid hashing - structure + content
     * Combines structure-based hash (layout) with content-based hash (visible text)
     * Enables detection of scrolled content while maintaining popup detection
     *
     * @param packageName Package name of the app
     * @return Hybrid screen hash (structure-content), or fallback hash if unavailable
     */
    private suspend fun calculateScreenHash(packageName: String): String {
        // Get root node from accessibility service
        val rootNode = accessibilityService?.rootInActiveWindow

        // Use ScreenStateManager for unified hashing if available
        return if (rootNode != null && screenStateManager != null) {
            val screenState = screenStateManager!!.captureScreenState(rootNode, packageName)
            val structureHash = screenState.hash

            // ENHANCEMENT: Add content hash for scrollable containers
            val scrollDetector = com.augmentalis.voiceoscore.learnapp.scrolling.ScrollDetector()
            val scrollables = scrollDetector.findScrollableContainers(rootNode)

            if (scrollables.isNotEmpty()) {
                // Hash visible content in scrollable containers
                val contentHashes = scrollables.mapNotNull { scrollable ->
                    try {
                        hashVisibleContent(scrollable)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to hash scrollable content: ${e.message}")
                        null
                    }
                }

                if (contentHashes.isNotEmpty()) {
                    // Combined hash: structure + content
                    "$structureHash-${contentHashes.joinToString("|")}"
                } else {
                    structureHash  // No content hash, use structure only
                }
            } else {
                structureHash  // No scrollables, use structure only
            }
        } else {
            // Fallback to timestamp-based hash if ScreenStateManager unavailable
            // This should rarely happen in production
            Log.w(TAG, "ScreenStateManager unavailable, using fallback hash")
            System.currentTimeMillis().toString()
        }
    }

    /**
     * Hash visible content in a scrollable container.
     *
     * Creates content signature from visible text elements to detect
     * when user scrolls and new content becomes visible.
     *
     * @param scrollableNode Scrollable container node
     * @return Content hash (MD5 of visible texts)
     */
    private fun hashVisibleContent(scrollableNode: AccessibilityNodeInfo): String {
        val visibleTexts = mutableListOf<String>()

        // Collect text from visible children
        traverseForVisibleText(scrollableNode, visibleTexts)

        // Create signature from texts
        if (visibleTexts.isEmpty()) {
            return "empty"
        }

        val signature = visibleTexts.joinToString("|")

        // MD5 hash for compact representation
        return try {
            val digest = MessageDigest.getInstance("MD5")
            val hashBytes = digest.digest(signature.toByteArray())
            hashBytes.joinToString("") { "%02x".format(it) }.take(8)  // First 8 chars
        } catch (e: Exception) {
            signature.hashCode().toString()  // Fallback to hashCode
        }
    }

    /**
     * Traverse tree collecting visible text.
     *
     * @param node Current node
     * @param results Mutable list to collect text
     */
    private fun traverseForVisibleText(
        node: AccessibilityNodeInfo,
        results: MutableList<String>
    ) {
        // Only collect from visible nodes
        if (node.isVisibleToUser) {
            node.text?.toString()?.takeIf { it.isNotBlank() }?.let {
                results.add(it.trim())
            }
            node.contentDescription?.toString()?.takeIf { it.isNotBlank() }?.let {
                results.add(it.trim())
            }
        }

        // Recurse children
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                try {
                    traverseForVisibleText(child, results)
                } finally {
                    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        @Suppress("DEPRECATION")
                        child.recycle()
                    }
                }
            }
        }
    }

    /**
     * Check if screen needs rescanning based on hash comparison.
     *
     * Queries database for existing screen with matching hash to determine
     * if rescan can be skipped (achieves 80% time savings for unchanged screens).
     *
     * Uses dual-hash strategy:
     * 1. Structure hash (from ScreenStateManager) - fast, checks UI structure
     * 2. Element hash (from ScreenHashCalculator) - accurate, checks element details
     *
     * Performance: ~5ms database lookup vs ~500ms full rescan
     *
     * ## P2 Task 1.1 Implementation
     * This method enables hash-based rescan optimization by checking if the
     * current screen hash matches any existing screen in the database.
     *
     * @param packageName Package name of the app
     * @param currentHash Current screen hash from ScreenStateManager
     * @param elements List of scraped elements for fallback hash calculation
     * @return true if rescan needed (new/changed screen), false if can skip (unchanged)
     */
    private suspend fun shouldRescanScreen(
        packageName: String,
        currentHash: String,
        elements: List<com.augmentalis.voiceoscore.learnapp.jit.JitCapturedElement>
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            // First check: Is structure hash already in screen_context?
            val existingScreen = databaseManager.screenContexts.getByHash(currentHash)
            if (existingScreen != null && existingScreen.packageName == packageName) {
                Log.i(TAG, "Screen unchanged (structure hash match), skipping rescan: $currentHash")
                return@withContext false  // Skip rescan
            }

            // Second check: Calculate element-based hash and compare
            // This provides more granular detection of screen changes
            if (elements.isNotEmpty()) {
                // Convert JitCapturedElement to ScrapedElementDTO for hash calculation
                val elementDTOs = elements.mapNotNull { element ->
                    try {
                        ScrapedElementDTO(
                            id = 0L,
                            elementHash = element.elementHash,
                            appId = packageName,
                            uuid = element.uuid,
                            className = element.className ?: "unknown",
                            viewIdResourceName = element.viewIdResourceName,
                            text = element.text,
                            contentDescription = element.contentDescription,
                            bounds = "${element.bounds.left},${element.bounds.top},${element.bounds.right},${element.bounds.bottom}",
                            isClickable = if (element.isClickable) 1L else 0L,
                            isLongClickable = if (element.isLongClickable) 1L else 0L,
                            isEditable = if (element.isEditable) 1L else 0L,
                            isScrollable = if (element.isScrollable) 1L else 0L,
                            isCheckable = if (element.isCheckable) 1L else 0L,
                            isFocusable = if (element.isFocusable) 1L else 0L,
                            isEnabled = if (element.isEnabled) 1L else 0L,
                            depth = element.depth.toLong(),
                            indexInParent = element.indexInParent.toLong(),
                            scrapedAt = System.currentTimeMillis(),
                            semanticRole = null,
                            inputType = null,
                            visualWeight = null,
                            isRequired = null,
                            formGroupId = null,
                            placeholderText = null,
                            validationPattern = null,
                            backgroundColor = null,
                            screen_hash = currentHash
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to convert element for hash: ${e.message}")
                        null  // Skip malformed elements
                    }
                }

                if (elementDTOs.isNotEmpty()) {
                    val elementHash = screenHashCalculator.calculateScreenHash(elementDTOs)
                    if (elementHash.isNotEmpty()) {
                        val existingByElementHash = databaseManager.screenContexts.getByHash(elementHash)
                        if (existingByElementHash != null && existingByElementHash.packageName == packageName) {
                            Log.i(TAG, "Screen unchanged (element hash match), skipping rescan: $elementHash")
                            return@withContext false  // Skip rescan
                        }
                    }
                }
            }

            // No match found - screen is new or changed, rescan needed
            Log.i(TAG, "Screen changed or new, rescan needed: $currentHash")
            return@withContext true
        } catch (e: Exception) {
            // On error, always rescan (safe fallback)
            Log.e(TAG, "Error checking screen hash for $packageName, defaulting to rescan: ${e.message}")
            return@withContext true
        }
    }

    /**
     * Save screen data to database.
     * Creates ScreenState and persists via repository.
     *
     * FIX (2025-12-01): Now captures UI elements during save for voice command support.
     * P2 Task 1.1: Updated to accept capturedElements as parameter to avoid double capture.
     */
    private suspend fun saveScreenToDatabase(
        packageName: String,
        screenHash: String,
        event: AccessibilityEvent,
        capturedElements: List<com.augmentalis.voiceoscore.learnapp.jit.JitCapturedElement>
    ) {
        // P2 Task 1.1: Elements already captured by caller (avoid double capture)
        var capturedElementCount = 0

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
                learnAppCore?.let { core ->
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
                                val result = core.processElement(
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
                } ?: run {
                    // Fallback to old logic if LearnAppCore not provided (backward compatibility)
                    val timestamp = System.currentTimeMillis()

                    // Get app version for version-aware commands
                    val appVersion = versionDetector?.getVersion(packageName) ?: AppVersion("unknown", 0L)

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

    // ========================================================================
    // JIT → LITE PROGRESSION (2025-12-22)
    // ========================================================================

    /**
     * Check for hidden menus/drawers after screen learning.
     *
     * Phase 5 (2025-12-22): Seamless JIT→Lite progression.
     * Detects expandable controls (menus, drawers) and offers deep scan
     * if user has Lite subscription.
     *
     * @param packageName Package name of the app
     * @param screenHash Current screen hash
     */
    private suspend fun checkForHiddenMenus(packageName: String, screenHash: String) {
        // Check if already deep scanned this screen
        if (hasDeepScannedScreen(packageName, screenHash)) {
            return
        }

        // Detect expandable controls
        val hasHiddenMenus = hasHiddenMenuItems()

        if (hasHiddenMenus) {
            Log.d(TAG, "Hidden menus detected on screen: $packageName - $screenHash")

            // Check if user has Lite access
            when (featureGateManager?.canUseMode(LearningMode.LITE)) {
                is FeatureGateResult.Allowed -> {
                    // Has access - check consent
                    if (deepScanConsentManager?.needsConsent(packageName) == true) {
                        val rootNode = accessibilityService?.rootInActiveWindow
                        if (rootNode != null) {
                            val expandables = ExpandableControlDetector.findExpandableControls(rootNode)
                            deepScanConsentManager.showConsentDialog(
                                packageName,
                                getAppName(packageName),
                                expandables.size
                            )
                            Log.i(TAG, "Showing deep scan consent dialog for $packageName (${expandables.size} expandables)")
                        }
                    }
                }
                is FeatureGateResult.Blocked -> {
                    // No access - could show upgrade offer here in the future
                    Log.d(TAG, "Hidden menus detected but no Lite subscription for $packageName")
                }
                null -> {
                    // Feature gate not available (testing without gate)
                    Log.d(TAG, "FeatureGateManager not available, skipping deep scan offer")
                }
            }
        }
    }

    /**
     * Check if screen has already been deep scanned.
     *
     * @param packageName Package name
     * @param screenHash Screen hash
     * @return true if already deep scanned, false otherwise
     */
    private fun hasDeepScannedScreen(packageName: String, screenHash: String): Boolean {
        val key = "$packageName:$screenHash"
        return deepScannedScreens.contains(key)
    }

    /**
     * Mark screen as deep scanned to avoid re-asking.
     *
     * @param packageName Package name
     * @param screenHash Screen hash
     */
    private fun markScreenDeepScanned(packageName: String, screenHash: String) {
        val key = "$packageName:$screenHash"
        deepScannedScreens.add(key)
        Log.d(TAG, "Marked screen as deep scanned: $key")
    }

    /**
     * Get app display name from package name.
     *
     * @param packageName Package name to look up
     * @return App display name or package name if not found
     */
    private fun getAppName(packageName: String): String {
        return try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            Log.w(TAG, "Could not get app name for: $packageName", e)
            packageName.substringAfterLast(".")
        }
    }

    /**
     * Handle deep scan consent granted by user.
     *
     * Called when user approves deep scan via consent dialog.
     * Runs deep scan on current screen and marks it as scanned.
     *
     * @param packageName Package name to deep scan
     */
    suspend fun onDeepScanConsentGranted(packageName: String) {
        Log.i(TAG, "User granted deep scan consent for $packageName")

        // Get current screen hash
        val screenHash = calculateScreenHash(packageName)

        // Run deep scan (builds on JIT data)
        val result = deepScanCurrentScreen(packageName)

        Log.i(TAG, "Deep scan result: $result")

        // Mark screen as deep scanned to avoid re-asking
        markScreenDeepScanned(packageName, screenHash)
    }

    // ========================================================================
    // DEEP SCAN FOR MENUS/DRAWERS (2025-12-22)
    // ========================================================================

    /**
     * Deep scan current screen for hidden menu/drawer items.
     *
     * Detects expandable controls (menus, drawers, dropdowns), expands them,
     * captures hidden elements, and collapses them back.
     *
     * Called when user approves deep scan via popup dialog.
     *
     * @param packageName Package name of the app
     * @return DeepScanResult with statistics
     */
    suspend fun deepScanCurrentScreen(packageName: String): DeepScanResult {
        val startTime = System.currentTimeMillis()
        val rootNode = accessibilityService?.rootInActiveWindow

        if (rootNode == null) {
            return DeepScanResult(
                success = false,
                expandablesFound = 0,
                expandablesScanned = 0,
                newElementsDiscovered = 0,
                duration = System.currentTimeMillis() - startTime,
                error = "Root node unavailable"
            )
        }

        try {
            Log.i(TAG, "Starting deep scan for $packageName")

            // 1. Find all expandable controls (menus, drawers, dropdowns)
            val expandableDetector = com.augmentalis.voiceoscore.learnapp.detection.ExpandableControlDetector
            val expandables = expandableDetector.findExpandableControls(rootNode)

            if (expandables.isEmpty()) {
                Log.i(TAG, "No expandable controls found")
                return DeepScanResult(
                    success = true,
                    expandablesFound = 0,
                    expandablesScanned = 0,
                    newElementsDiscovered = 0,
                    duration = System.currentTimeMillis() - startTime
                )
            }

            Log.i(TAG, "Found ${expandables.size} expandable controls")

            var expandablesScanned = 0
            var totalNewElements = 0
            val allCapturedElements = mutableListOf<JitCapturedElement>()

            // 2. Process each expandable
            for (expandable in expandables) {
                try {
                    val info = expandableDetector.getExpansionInfo(expandable)

                    // Skip if already expanded or low confidence
                    if (info.isExpanded || info.confidence < 0.5f) {
                        Log.d(TAG, "Skipping expandable (expanded=${info.isExpanded}, confidence=${info.confidence})")
                        continue
                    }

                    Log.i(TAG, "Scanning ${info.expansionType}: ${info.reason}")

                    // 3. Expand the control
                    val expanded = expandable.performAction(AccessibilityNodeInfo.ACTION_EXPAND)
                    if (!expanded) {
                        Log.w(TAG, "Failed to expand control")
                        continue
                    }

                    // Wait for animation
                    delay(500)

                    // 4. Capture expanded content
                    val newElements = elementCapture?.captureScreenElements(packageName) ?: emptyList()
                    Log.i(TAG, "Captured ${newElements.size} elements from expanded control")

                    allCapturedElements.addAll(newElements)
                    totalNewElements += newElements.size
                    expandablesScanned++

                    // 5. Collapse back
                    expandable.performAction(AccessibilityNodeInfo.ACTION_COLLAPSE)
                    delay(300)

                } catch (e: Exception) {
                    Log.e(TAG, "Error scanning expandable control", e)
                } finally {
                    // Recycle node if needed
                    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        try {
                            @Suppress("DEPRECATION")
                            expandable.recycle()
                        } catch (e: Exception) {
                            // Ignore recycle errors
                        }
                    }
                }
            }

            // 6. Generate commands for newly discovered elements
            if (allCapturedElements.isNotEmpty()) {
                generateCommandsForElements(packageName, allCapturedElements)

                // Trigger command registration
                withContext(Dispatchers.Main) {
                    voiceOSService?.onNewCommandsGenerated()
                }
            }

            val duration = System.currentTimeMillis() - startTime
            Log.i(TAG, "Deep scan complete: ${expandablesScanned}/${expandables.size} scanned, $totalNewElements new elements, ${duration}ms")

            return DeepScanResult(
                success = true,
                expandablesFound = expandables.size,
                expandablesScanned = expandablesScanned,
                newElementsDiscovered = totalNewElements,
                duration = duration
            )

        } catch (e: Exception) {
            Log.e(TAG, "Deep scan failed", e)
            return DeepScanResult(
                success = false,
                expandablesFound = 0,
                expandablesScanned = 0,
                newElementsDiscovered = 0,
                duration = System.currentTimeMillis() - startTime,
                error = e.message
            )
        }
    }

    /**
     * Check if screen has hidden menu/drawer items that need deep scan.
     *
     * Called automatically during screen learning to detect if we should
     * prompt user for deep scan.
     *
     * @return true if expandable controls found, false otherwise
     */
    suspend fun hasHiddenMenuItems(): Boolean {
        val rootNode = accessibilityService?.rootInActiveWindow ?: return false

        val expandableDetector = com.augmentalis.voiceoscore.learnapp.detection.ExpandableControlDetector
        val expandables = expandableDetector.findExpandableControls(rootNode)

        // Filter for collapsed, high-confidence expandables
        val collapsedExpandables = expandables.count { expandable ->
            try {
                val info = expandableDetector.getExpansionInfo(expandable)
                !info.isExpanded && info.confidence >= 0.5f
            } catch (e: Exception) {
                false
            } finally {
                if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    try {
                        @Suppress("DEPRECATION")
                        expandable.recycle()
                    } catch (e: Exception) {
                        // Ignore
                    }
                }
            }
        }

        Log.d(TAG, "Found $collapsedExpandables collapsed expandable controls")
        return collapsedExpandables > 0
    }

    /**
     * Deep Scan Result
     *
     * Statistics from deep scan operation.
     */
    data class DeepScanResult(
        val success: Boolean,
        val expandablesFound: Int,
        val expandablesScanned: Int,
        val newElementsDiscovered: Int,
        val duration: Long,
        val error: String? = null
    ) {
        override fun toString(): String {
            return if (success) {
                "Deep Scan: $expandablesScanned/$expandablesFound scanned, $newElementsDiscovered elements, ${duration}ms"
            } else {
                "Deep Scan Failed: ${error ?: "Unknown error"}"
            }
        }
    }

    // ================================================================
    // HASH-BASED DEDUPLICATION HELPERS (2025-12-22)
    // ================================================================

    /**
     * Data class representing screen metadata with app version
     *
     * Used for hash-based deduplication to determine if screen needs rescanning
     */
    data class ScreenData(
        val screenHash: String,
        val packageName: String,
        val elementCount: Int,
        val appVersion: String?
    )

    /**
     * Get screen by hash with app version validation
     *
     * Queries database for existing screen with matching hash and package.
     * Returns screen metadata including app version for version-aware deduplication.
     *
     * @param hash Screen hash to lookup
     * @param packageName Package name to match
     * @return ScreenData if exists, null otherwise
     */
    private suspend fun getScreenByHash(hash: String, packageName: String): ScreenData? {
        return withContext(Dispatchers.IO) {
            try {
                // Check screen_context table for matching hash
                val screenContext = databaseManager.screenContexts.getByHash(hash)

                if (screenContext != null && screenContext.packageName == packageName) {
                    // Get app version from app_version table
                    val appVersion = databaseManager.appVersionQueries.getAppVersion(packageName).executeAsOneOrNull()

                    ScreenData(
                        screenHash = screenContext.screenHash,
                        packageName = screenContext.packageName,
                        elementCount = screenContext.elementCount.toInt(),
                        appVersion = appVersion?.version_name
                    )
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting screen by hash: $hash", e)
                null
            }
        }
    }

    /**
     * Load pre-generated commands from cache
     *
     * Loads existing voice commands for cached screen from database
     * and notifies VoiceOSService they're available.
     *
     * @param screen Cached screen data
     */
    private suspend fun loadCommandsFromCache(screen: ScreenData) {
        withContext(Dispatchers.IO) {
            try {
                // Commands are already in database via screen_hash FK
                // Just notify service to reload commands
                withContext(Dispatchers.Main) {
                    voiceOSService?.onNewCommandsGenerated()
                }

                Log.d(TAG, "Loaded ${screen.elementCount} commands from cache for screen ${screen.screenHash}")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading commands from cache", e)
            }
        }
    }

    /**
     * Check for hidden menus (overload for compatibility)
     *
     * Checks if current screen has expandable controls without screen hash parameter.
     *
     * @param packageName Package name of the app
     */
    private suspend fun checkForHiddenMenus(packageName: String) {
        val currentHash = calculateScreenHash(packageName)
        checkForHiddenMenus(packageName, currentHash)
    }

    /**
     * Deduplicate elements by VUID
     *
     * Filters out elements that already exist in database by checking UUID.
     * Prevents duplicate element capture and command generation.
     *
     * @param elements List of captured elements
     * @param packageName Package name of the app
     * @return Filtered list containing only new elements
     */
    private suspend fun deduplicateByVUID(
        elements: List<JitCapturedElement>,
        packageName: String
    ): List<JitCapturedElement> {
        return withContext(Dispatchers.IO) {
            elements.filter { element ->
                try {
                    // Check if element with this UUID already exists
                    val uuid = element.uuid ?: return@filter true  // Keep if no UUID

                    val existing = databaseManager.scrapedElements.getByUuid(packageName, uuid)

                    existing == null  // Keep only if not found
                } catch (e: Exception) {
                    Log.w(TAG, "Error checking VUID for element: ${e.message}")
                    true  // Keep on error (safer than dropping)
                }
            }
        }
    }

    /**
     * Clean up resources.
     */
    fun destroy() {
        deactivate()
        eventCallback = null
        deepScannedScreens.clear()
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

    /**
     * P2 Task 1.1: Get hash-based rescan optimization metrics.
     *
     * Provides statistics on how many screens were skipped vs rescanned,
     * enabling measurement of the 80% time savings goal.
     *
     * @return JITHashMetrics containing skip rate and counts
     */
    fun getHashMetrics(): JITHashMetrics {
        val skipRate = getSkipPercentage()

        return JITHashMetrics(
            totalScreens = totalScreensProcessed,
            skipped = screensSkippedByHash,
            rescanned = screensRescanned,
            skipPercentage = skipRate
        )
    }

    /**
     * Calculate skip percentage for metrics logging.
     *
     * @return Skip percentage (0.0-100.0)
     */
    private fun getSkipPercentage(): Float {
        return if (totalScreensProcessed > 0) {
            (screensSkippedByHash.toFloat() / totalScreensProcessed) * 100
        } else 0f
    }

    /**
     * P2 Task 1.1: Hash-based rescan optimization metrics data class.
     *
     * Tracks performance of hash-based screen deduplication to measure
     * the effectiveness of rescan skipping (target: 80% skip rate).
     */
    data class JITHashMetrics(
        val totalScreens: Int,
        val skipped: Int,
        val rescanned: Int,
        val skipPercentage: Float
    ) {
        /**
         * Check if optimization is working effectively (>= 70% skip rate)
         */
        fun isOptimizationEffective(): Boolean = skipPercentage >= 70f

        /**
         * Get human-readable summary
         */
        fun getSummary(): String {
            return "Hash Metrics: $skipped skipped / $totalScreens total (${String.format("%.1f", skipPercentage)}% skip rate)"
        }
    }
}
