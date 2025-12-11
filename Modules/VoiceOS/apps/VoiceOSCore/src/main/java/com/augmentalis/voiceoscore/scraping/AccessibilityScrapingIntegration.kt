/**
 * AccessibilityScrapingIntegration.kt - Integration layer for scraping system
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-09
 * Modified: 2025-10-18 (Phase 3: User settings + battery optimization for interaction learning)
 */
package com.augmentalis.voiceoscore.scraping

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.BatteryManager
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.voiceoscore.accessibility.utils.ResourceMonitor
import com.augmentalis.voiceos.logging.PIILoggingWrapper
import com.augmentalis.voiceoscore.database.VoiceOSCoreDatabaseAdapter
import com.augmentalis.voiceoscore.learnapp.settings.LearnAppDeveloperSettings
import com.augmentalis.voiceoscore.database.toScrapedElementEntity
import com.augmentalis.voiceoscore.utils.ScrapingAnalytics
import com.augmentalis.voiceoscore.utils.AnalyticsSummary
import com.augmentalis.voiceoscore.database.entities.AppEntity
import com.augmentalis.voiceoscore.scraping.detection.LauncherDetector
import com.augmentalis.voiceoscore.scraping.entities.ScrapedElementEntity
import com.augmentalis.voiceoscore.scraping.entities.ScrapedHierarchyEntity
import com.augmentalis.uuidcreator.UUIDCreator
import com.augmentalis.uuidcreator.alias.UuidAliasManager
import com.augmentalis.uuidcreator.database.UUIDCreatorDatabase
import com.augmentalis.uuidcreator.models.UUIDAccessibility
import com.augmentalis.uuidcreator.models.UUIDElement
import com.augmentalis.voiceos.hash.HashUtils
import com.augmentalis.uuidcreator.models.UUIDMetadata
import com.augmentalis.uuidcreator.thirdparty.AccessibilityFingerprint
import com.augmentalis.uuidcreator.thirdparty.ThirdPartyUuidGenerator
import com.augmentalis.voiceoscore.utils.requireNotNull
import com.augmentalis.voiceos.constants.VoiceOSConstants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.util.UUID

/**
 * Accessibility Scraping Integration
 *
 * This class provides the integration between VoiceAccessibilityService
 * and the scraping database system. It handles:
 * 1. Automatic scraping on app window changes
 * 2. Storage of UI element data
 * 3. Command generation
 * 4. Voice command processing
 *
 * Usage in VoiceAccessibilityService:
 * ```
 * private lateinit var scrapingIntegration: AccessibilityScrapingIntegration
 *
 * override fun onServiceConnected() {
 *     scrapingIntegration = AccessibilityScrapingIntegration(this, this)
 * }
 *
 * override fun onAccessibilityEvent(event: AccessibilityEvent) {
 *     scrapingIntegration.onAccessibilityEvent(event)
 * }
 * ```
 */
class AccessibilityScrapingIntegration(
    private val context: Context,
    private val accessibilityService: AccessibilityService
) {

    companion object {
        private const val TAG = "AccessibilityScrapingIntegration"
        // FIX (2025-12-11): Reduced from 50 → 15 to prevent excessive memory allocation (14MB → 4-5MB)
        // Root cause: Deep recursion caused 14MB+ allocations triggering GC pressure
        private const val MAX_DEPTH = 15 // Prevent stack overflow on deeply nested UIs

        // FIX (2025-12-11): Launcher screen command throttling
        // Root cause: Launcher screens with 50-100+ app icons generated 300+ commands causing ANR
        private const val MAX_COMMANDS_LAUNCHER = 30  // Limit for launcher/home screens
        private const val MAX_COMMANDS_NORMAL = 100   // Limit for regular apps

        // App scraping mode constants
        const val MODE_DYNAMIC = "DYNAMIC"
        const val MODE_LEARN_APP = "LEARN_APP"

        // System UI packages to filter out
        val SYSTEM_UI_PACKAGES = setOf(
            "com.android.systemui",
            "android"
        )

        // Phase 3: Interaction learning preferences
        private const val PREF_NAME = "voiceos_interaction_learning"
        private const val PREF_INTERACTION_LEARNING_ENABLED = "interaction_learning_enabled"
        private val MIN_BATTERY_LEVEL_FOR_LEARNING = VoiceOSConstants.Battery.MIN_BATTERY_LEVEL_FOR_LEARNING

        // Phase 3D: Resource throttling
        private const val THROTTLE_DELAY_MS = 500L  // Delay between operations when throttling

        // P2 Fix (2025-11-30): Scraping timeout to prevent unbounded operations
        private const val SCRAPING_TIMEOUT_MS = 10_000L  // 10 second timeout for scraping

        // P2 Fix (2025-11-30): Scroll-to-load for RecyclerView scraping
        private const val SCROLL_TO_LOAD_ENABLED = true
        private const val MAX_SCROLL_ATTEMPTS = 100      // Max scrolls per RecyclerView (comprehensive scraping)
        private const val SCROLL_DELAY_MS = 300L          // Wait for content to load after scroll

        // Dynamic content wait configuration
        private const val SCREEN_STABLE_TIMEOUT_MS = 3000L  // Max wait for screen to stabilize
        private const val STABLE_CHECK_INTERVAL_MS = 200L   // How often to check stability
        private const val STABLE_THRESHOLD = 3              // Consecutive stable counts required
        private val SCROLLABLE_VIEW_CLASSES = setOf(
            "androidx.recyclerview.widget.RecyclerView",
            "android.widget.ListView",
            "android.widget.ScrollView",
            "androidx.core.widget.NestedScrollView",
            "android.widget.HorizontalScrollView"
        )
    }

    private val database: VoiceOSCoreDatabaseAdapter = VoiceOSCoreDatabaseAdapter.getInstance(context)
    private val packageManager: PackageManager = context.packageManager
    private val commandGenerator: CommandGenerator = CommandGenerator(context)
    private val voiceCommandProcessor: VoiceCommandProcessor = VoiceCommandProcessor(context, accessibilityService)

    // Phase 1: Device-agnostic launcher detection (replaces hardcoded EXCLUDED_PACKAGES)
    private val launcherDetector: LauncherDetector = LauncherDetector(context)

    // Phase 3: Interaction learning preferences
    private val preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    // Recovery mode flag - suppress scraping during BACK navigation recovery
    @Volatile
    private var isInRecoveryMode = false

    // UUID Creator components for universal element identification
    private val uuidCreator: UUIDCreator = UUIDCreator.initialize(context)
    private val uuidCreatorDatabase: UUIDCreatorDatabase = UUIDCreatorDatabase.getInstance(context)
    private val thirdPartyGenerator: ThirdPartyUuidGenerator = ThirdPartyUuidGenerator(context)

    private val aliasManager: UuidAliasManager = UuidAliasManager(database.databaseManager.uuids)

    // AI Context Inference (Phase 1 & Phase 2)
    private val semanticInferenceHelper: SemanticInferenceHelper = SemanticInferenceHelper()
    private val screenContextHelper: ScreenContextInferenceHelper = ScreenContextInferenceHelper()

    // Phase 3D: Resource monitoring for intelligent throttling
    private val resourceMonitor: ResourceMonitor = ResourceMonitor(context)

    // Developer settings for verbose logging
    private val developerSettings by lazy { LearnAppDeveloperSettings(context) }

    // Phase 3E: Feature flag management for gradual rollout
    private val featureFlagManager: com.augmentalis.voiceoscore.accessibility.utils.FeatureFlagManager =
        com.augmentalis.voiceoscore.accessibility.utils.FeatureFlagManager(context)

    // Phase 3: Scraping analytics for observability
    private val scrapingAnalytics = ScrapingAnalytics()

    private val integrationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Track last scraped app to avoid duplicate scraping
    private var lastScrapedAppHash: String? = null

    // Track last screen for transition detection (Phase 2.5)
    private var lastScrapedScreenHash: String? = null
    private var lastScreenTime: Long = 0L

    // Phase 3: Element visibility tracking for interaction recording
    private val elementVisibilityTracker = java.util.concurrent.ConcurrentHashMap<String, Long>()
    private val elementStateTracker = java.util.concurrent.ConcurrentHashMap<String, MutableMap<String, String?>>()

    // YOLO Phase 2 - Issue #20: Retry queue for failed state changes
    // When state changes occur before elements are scraped, queue them for retry
    private val stateChangeRetryQueue = java.util.concurrent.ConcurrentLinkedQueue<PendingStateChange>()
    private val MAX_RETRY_ATTEMPTS = 5
    private val RETRY_CLEANUP_THRESHOLD = 100 // Clean queue when it exceeds this size

    // Package info cache for performance optimization
    // Caches packageName -> (packageName, versionCode) to avoid repeated PackageManager lookups
    // Invalidated on window state changes (app might have updated)
    private val packageInfoCache = java.util.concurrent.ConcurrentHashMap<String, Pair<String, Long>>()

    // Scraping metrics for performance monitoring
    private data class ScrapingMetrics(
        var elementsFound: Int = 0,
        var elementsCached: Int = 0,
        var elementsScraped: Int = 0,
        var timeMs: Long = 0
    )

    /**
     * Handle accessibility events
     *
     * Call this from VoiceOSService.onAccessibilityEvent()
     */
    fun onAccessibilityEvent(event: AccessibilityEvent) {
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                if (developerSettings.isVerboseLoggingEnabled()) {
                    Log.d(TAG, "Window state changed: ${event.packageName}")
                }
                // Invalidate package info cache on window changes (app might have updated)
                packageInfoCache.clear()
                integrationScope.launch {
                    scrapeCurrentWindow(event)
                }
            }

            // Phase 3: Track user interactions
            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                if (developerSettings.isVerboseLoggingEnabled()) {
                    Log.d(TAG, "View clicked")
                }
                integrationScope.launch {
                    recordInteraction(event, com.augmentalis.voiceoscore.scraping.entities.InteractionType.CLICK)
                }
            }

            AccessibilityEvent.TYPE_VIEW_LONG_CLICKED -> {
                if (developerSettings.isVerboseLoggingEnabled()) {
                    Log.d(TAG, "View long clicked")
                }
                integrationScope.launch {
                    recordInteraction(event, com.augmentalis.voiceoscore.scraping.entities.InteractionType.LONG_PRESS)
                }
            }

            AccessibilityEvent.TYPE_VIEW_FOCUSED -> {
                if (developerSettings.isVerboseLoggingEnabled()) {
                    Log.d(TAG, "View focused")
                }
                integrationScope.launch {
                    recordInteraction(event, com.augmentalis.voiceoscore.scraping.entities.InteractionType.FOCUS)
                }
            }

            AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                if (developerSettings.isVerboseLoggingEnabled()) {
                    Log.d(TAG, "View scrolled")
                }
                integrationScope.launch {
                    recordInteraction(event, com.augmentalis.voiceoscore.scraping.entities.InteractionType.SCROLL)
                }
            }

            // Phase 3: Track state changes
            AccessibilityEvent.TYPE_VIEW_SELECTED -> {
                if (developerSettings.isVerboseLoggingEnabled()) {
                    Log.d(TAG, "View selected")
                }
                integrationScope.launch {
                    recordStateChange(event, com.augmentalis.voiceoscore.scraping.entities.StateType.SELECTED)
                }
            }

            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                // Track element state changes (checked, enabled, etc.)
                integrationScope.launch {
                    trackContentChanges(event)
                }
            }
        }
    }

    /**
     * Scrape current window and store data
     *
     * P2 Fix (2025-11-30): Added timeout to prevent unbounded scraping operations.
     * Previous implementation could hang indefinitely on complex UI trees.
     *
     * @param event Accessibility event triggering the scrape
     * @param filterNonActionable If true, only scrape actionable elements (default: false)
     */
    private suspend fun scrapeCurrentWindow(
        event: AccessibilityEvent,
        filterNonActionable: Boolean = false
    ) {
        try {
            // P2 Fix: Wrap scraping in timeout to prevent unbounded operations
            withTimeout(SCRAPING_TIMEOUT_MS) {
                scrapeCurrentWindowImpl(event, filterNonActionable)
            }
        } catch (e: TimeoutCancellationException) {
            Log.w(TAG, "Scraping timed out after ${SCRAPING_TIMEOUT_MS}ms - UI tree too complex or app unresponsive")
            // Record timeout for analytics
            try {
                val rootNode = accessibilityService.rootInActiveWindow
                val packageName = rootNode?.packageName?.toString() ?: "unknown"
                rootNode?.recycle()

                scrapingAnalytics.recordScrape(
                    packageName = packageName,
                    elementCount = 0,
                    durationMs = SCRAPING_TIMEOUT_MS,
                    success = false,
                    cacheHit = false,
                    treeDepth = 0,
                    errorType = "TIMEOUT"
                )
            } catch (analyticsError: Exception) {
                Log.w(TAG, "Failed to record timeout analytics", analyticsError)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in scrapeCurrentWindow wrapper", e)
        }
    }

    /**
     * Internal scraping implementation (wrapped by scrapeCurrentWindow with timeout)
     */
    private suspend fun scrapeCurrentWindowImpl(
        event: AccessibilityEvent,
        filterNonActionable: Boolean = false
    ) {
        try {
            // Phase 3D: Check if should throttle due to memory pressure
            val throttleLevel = resourceMonitor.getThrottleRecommendation()
            if (throttleLevel == ResourceMonitor.ThrottleLevel.HIGH) {
                Log.w(TAG, "⏸️ Skipping scraping - HIGH memory pressure detected")
                return
            }

            // Add delay if medium throttling
            if (throttleLevel == ResourceMonitor.ThrottleLevel.MEDIUM) {
                Log.i(TAG, "⏳ MEDIUM throttling - adding ${THROTTLE_DELAY_MS}ms delay")
                kotlinx.coroutines.delay(THROTTLE_DELAY_MS)
            }

            Log.i(TAG, "=== Starting Window Scrape ===")

            // Get root node
            val rootNode = event.source ?: accessibilityService.rootInActiveWindow
            if (rootNode == null) {
                Log.w(TAG, "Root node is null, cannot scrape")
                return
            }

            val packageName = rootNode.packageName?.toString()
            if (packageName == null) {
                Log.w(TAG, "Package name is null, skipping scrape")
                rootNode.recycle()
                return
            }

            // Phase 3E: Check if dynamic scraping enabled for this app
            if (!featureFlagManager.isDynamicScrapingEnabled(packageName)) {
                Log.i(TAG, "🚫 Dynamic scraping disabled for $packageName - skipping")
                rootNode.recycle()
                return
            }

            // Check if in recovery mode (suppress scraping during BACK navigation)
            if (isInRecoveryMode) {
                Log.v(TAG, "⏸️ Recovery mode active - suppressing scraping for: $packageName")
                rootNode.recycle()
                return
            }

            // FIX (2025-12-11): Allow launcher scraping but with LIMITED commands
            // Root cause: Previously skipped launchers entirely, but user reported learning Teams app launcher
            // Solution: Detect launcher and apply command limit (30 instead of 100) to prevent ANR
            val isLauncher = launcherDetector.isLauncher(packageName)
            if (isLauncher) {
                Log.i(TAG, "🏠 Launcher detected: $packageName - applying command limit ($MAX_COMMANDS_LAUNCHER)")
            }

            // Check if system UI
            if (SYSTEM_UI_PACKAGES.contains(packageName)) {
                if (developerSettings.isVerboseLoggingEnabled()) {
                    Log.d(TAG, "⚙️ Skipping system UI package: $packageName")
                }
                rootNode.recycle()
                return
            }

            if (developerSettings.isVerboseLoggingEnabled()) {
                Log.d(TAG, "✅ Scraping package: $packageName")
            }

            // Get app info
            val appInfo = try {
                packageManager.getPackageInfo(packageName, 0)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to retrieve package metadata for $packageName (cannot obtain version code for hash calculation). Package will be skipped.", e)
                rootNode.recycle()
                return
            }

            // Calculate app hash
            val appHash = HashUtils.calculateAppHash(packageName, appInfo.longVersionCode.toInt())
            if (developerSettings.isVerboseLoggingEnabled()) {
                Log.d(TAG, "App hash: $appHash")
            }

            // Check if already scraped
            if (appHash == lastScrapedAppHash) {
                if (developerSettings.isVerboseLoggingEnabled()) {
                    Log.d(TAG, "App already scraped recently, skipping")
                }
                rootNode.recycle()
                return
            }

            // ===== PHASE 1: Hash Deduplication - Check if app exists =====
            val metrics = ScrapingMetrics()
            val scrapeStartTime = System.currentTimeMillis()

            val existingApp = database.getAppByHash(appHash)
            val appId: String
            val isNewApp = existingApp == null

            if (existingApp != null) {
                if (developerSettings.isVerboseLoggingEnabled()) {
                    Log.d(TAG, "App already in database (appId=${existingApp.packageName}), using incremental scraping")
                }
                database.incrementScrapeCount(packageName)
                appId = existingApp.packageName
            } else {
                Log.i(TAG, "New app detected, performing full scrape")

                val currentTime = System.currentTimeMillis()

                // Create app entity
                appId = packageName // Use packageName as appId
                val app = AppEntity(
                    packageName = packageName,
                    appName = appInfo.applicationInfo.loadLabel(packageManager).toString(),
                    versionCode = appInfo.longVersionCode,
                    versionName = appInfo.versionName ?: "unknown",
                    lastScraped = currentTime,
                    dynamicScrapingEnabled = true  // MODE_DYNAMIC equivalent
                )

                // Insert app into unified apps table
                database.insertApp(app)
                if (developerSettings.isVerboseLoggingEnabled()) {
                    Log.d(TAG, "Inserted app: ${app.appName}")
                }
            }

            // ===== DYNAMIC CONTENT WAIT: Wait for screen to stabilize =====
            // This addresses async-loaded content (AJAX, lazy loading) that appears
            // 500ms-2s after screen loads (social media feeds, search results, etc.)
            val screenStable = waitForScreenStable(rootNode, SCREEN_STABLE_TIMEOUT_MS)
            if (!screenStable) {
                Log.w(TAG, "⚠️ Screen did not stabilize - scraping may miss async content")
            }

            // ===== PHASE 1: Scrape element tree with hash deduplication =====
            val elements = mutableListOf<ScrapedElementEntity>()
            val hierarchyBuildInfo = mutableListOf<HierarchyBuildInfo>()

            // Phase 3E: Get custom max depth from feature flags (if set)
            val customMaxDepth = featureFlagManager.getMaxScrapeDepth(packageName)
            if (customMaxDepth != MAX_DEPTH) {
                if (developerSettings.isVerboseLoggingEnabled()) {
                    Log.d(TAG, "Using custom max scrape depth for $packageName: $customMaxDepth")
                }
            }

            scrapeNode(rootNode, appId, null, 0, 0, elements, hierarchyBuildInfo, filterNonActionable, metrics, customMaxDepth)

            // Calculate scraping metrics (before database insertion)
            metrics.timeMs = System.currentTimeMillis() - scrapeStartTime

            // Calculate max tree depth for analytics
            val maxTreeDepth = elements.maxOfOrNull { it.depth } ?: 0

            // Determine if cache hit (app already scraped recently)
            val cacheHit = !isNewApp && (appHash == lastScrapedAppHash)

            // FIX (2025-12-11): Apply command limit to prevent ANR on launcher screens
            val maxCommands = if (isLauncher) MAX_COMMANDS_LAUNCHER else MAX_COMMANDS_NORMAL
            if (elements.size > maxCommands) {
                Log.w(TAG, "⚠️ Command limit exceeded: ${elements.size} > $maxCommands, trimming to most important elements")
                // Sort by importance (clickable + shallow depth + has text = more important)
                elements.sortWith(compareByDescending<ScrapedElementEntity> { it.isClickable }
                    .thenBy { it.depth }  // Shallower depth = more prominent
                    .thenByDescending { !it.text.isNullOrEmpty() || !it.contentDescription.isNullOrEmpty() })
                // Remove elements beyond limit
                while (elements.size > maxCommands) {
                    elements.removeAt(elements.size - 1)
                }
                Log.i(TAG, "✂️ Trimmed to $maxCommands elements (prioritized clickable, shallow, labeled)")
            }

            // ===== PHASE 2: Clean up old hierarchy and insert elements =====
            // CRITICAL: Delete old hierarchy records BEFORE inserting elements
            // When elements are replaced (same hash), they get new IDs, orphaning old hierarchy records
            // This causes FK constraint violations when inserting new hierarchy
            database.deleteHierarchyByApp(appId)
            if (developerSettings.isVerboseLoggingEnabled()) {
                Log.d(TAG, "Cleared old hierarchy records for app: $appId")
            }

            // Insert elements and capture database-assigned IDs
            val assignedIds: List<Long> = database.insertElementBatch(elements)

            Log.i(TAG, "Inserted ${assignedIds.size} elements, captured database IDs")
            if (developerSettings.isVerboseLoggingEnabled()) {
                Log.d(TAG, "Sample ID mapping (first 5): ${assignedIds.take(5)}")
            }

            // Validate we got the expected number of IDs
            if (assignedIds.size != elements.size) {
                Log.e(TAG, "ID count mismatch! Expected ${elements.size}, got ${assignedIds.size}")
                throw IllegalStateException(
                    "Batch insert partially failed: inserted ${assignedIds.size} elements but expected ${elements.size}. " +
                    "This violates the all-or-nothing insertion contract. " +
                    "Possible causes: (1) database constraint violation on some records, " +
                    "(2) insufficient storage space, (3) transaction rolled back partially. " +
                    "Check database logs and re-scrape the application to retry."
                )
            }

            // P1-1: Validate database actually contains all scraped elements
            val dbCount = database.countElementsByApp(appId).toInt()
            if (dbCount < elements.size) {
                Log.e(TAG, "❌ Database count mismatch! Expected ${elements.size}, got $dbCount")
                throw IllegalStateException(
                    "Database verification failed after insertion: stored $dbCount elements but scraped ${elements.size}. " +
                    "Expected count: ${elements.size}, Actual count: $dbCount, Gap: ${elements.size - dbCount} missing. " +
                    "This indicates: (1) some records failed to persist despite insertion success, " +
                    "(2) concurrent deletion by another process, or (3) DAO count method has a bug. " +
                    "Verify database integrity and re-scrape the application."
                )
            }
            if (developerSettings.isVerboseLoggingEnabled()) {
                Log.d(TAG, "✅ Database count validated: $dbCount elements for app $appId")
            }

            // P1-5: Log detailed scraping metrics (after database validation)
            if (filterNonActionable) {
                Log.i(TAG, "Filtered scraping: ${elements.size} actionable elements scraped")
            } else {
                Log.i(TAG, "Full scraping: ${elements.size} total elements scraped")
            }
            Log.i(TAG, "${hierarchyBuildInfo.size} hierarchy relationships tracked")
            Log.i(TAG, "📊 METRICS: Found=${metrics.elementsFound}, Cached=${metrics.elementsCached}, " +
                    "Scraped=${metrics.elementsScraped}, Persisted=$dbCount, Time=${metrics.timeMs}ms")
            if (metrics.elementsFound > 0) {
                val cacheHitRate = (metrics.elementsCached.toFloat() / metrics.elementsFound * 100).toInt()
                Log.i(TAG, "📈 Cache hit rate: $cacheHitRate% (${metrics.elementsCached}/${metrics.elementsFound})")
            }

            // ===== PHASE 2.5: Register UUIDs with UUIDCreator =====
            // P1-3: Track UUID generation and registration metrics
            val elementsWithUuid = elements.count { it.uuid != null }
            val elementsWithoutUuid = elements.size - elementsWithUuid

            // Register elements that have UUIDs with the UUIDCreator system
            val registeredCount = elements.count { element ->
                element.uuid != null && try {
                    val uuidElement = UUIDElement(
                        uuid = element.uuid,
                        name = element.text ?: element.contentDescription ?: "Unknown",
                        type = element.className?.substringAfterLast('.') ?: "unknown",
                        description = element.contentDescription,
                        metadata = UUIDMetadata(
                            label = element.text,
                            hint = element.contentDescription,
                            attributes = mapOf(
                                "thirdPartyApp" to "true",
                                "packageName" to packageName,
                                "className" to (element.className ?: ""),
                                "resourceId" to (element.viewIdResourceName ?: ""),
                                "elementHash" to element.elementHash
                            ),
                            accessibility = UUIDAccessibility(
                                contentDescription = element.contentDescription,
                                isClickable = element.isClickable,
                                isFocusable = element.isFocusable,
                                isScrollable = element.isScrollable
                            )
                        )
                    )
                    uuidCreator.registerElement(uuidElement)
                    true
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to register UUID ${element.uuid}", e)
                    false
                }
            }

            // P1-3: Calculate and log UUID generation/registration rates
            val uuidGenerationRate = if (elements.size > 0) (elementsWithUuid * 100 / elements.size) else 100
            val uuidRegistrationRate = if (elementsWithUuid > 0) (registeredCount * 100 / elementsWithUuid) else 100

            Log.i(TAG, "UUID Generation: $elementsWithUuid/${elements.size} ($uuidGenerationRate%)")
            Log.i(TAG, "UUID Registration: $registeredCount/$elementsWithUuid ($uuidRegistrationRate%)")

            // P1-3: Warn if UUID generation rate is below 90%
            if (uuidGenerationRate < 90) {
                Log.w(TAG, "⚠️ LOW UUID generation rate: $uuidGenerationRate% ($elementsWithoutUuid elements without UUIDs)")
            }

            // P1-3: Warn if UUID registration rate is below 90%
            if (elementsWithUuid > 0 && uuidRegistrationRate < 90) {
                Log.w(TAG, "⚠️ LOW UUID registration rate: $uuidRegistrationRate%")
            }

            // ===== PHASE 3: Build hierarchy entities using element hashes =====
            val hierarchy = hierarchyBuildInfo.map { buildInfo ->
                // Map list indices to element hashes
                val childHash = elements[buildInfo.childListIndex].elementHash
                val parentHash = elements[buildInfo.parentListIndex].elementHash

                ScrapedHierarchyEntity(
                    parentElementHash = parentHash,   // ✅ Element hash
                    childElementHash = childHash,      // ✅ Element hash
                    depth = buildInfo.depth
                )
            }

            if (developerSettings.isVerboseLoggingEnabled()) {
                Log.d(TAG, "Built ${hierarchy.size} hierarchy entities with valid foreign keys")
            }

            // ===== PHASE 4: Insert hierarchy with valid foreign key references =====
            database.insertHierarchyBatch(hierarchy)

            // Generate commands (need to update elements with real database IDs first)
            if (developerSettings.isVerboseLoggingEnabled()) {
                Log.d(TAG, "Generating voice commands...")
            }

            // Update elements with real database IDs from assignedIds
            val elementsWithIds = elements.mapIndexed { index, element ->
                element.copy(id = assignedIds[index])
            }

            val commands = commandGenerator.generateCommandsForElements(elementsWithIds)

            // Validation: Ensure all commands have valid element hashes
            require(commands.all { it.elementHash.isNotBlank() }) {
                "All generated commands must have valid element hashes"
            }

            if (developerSettings.isVerboseLoggingEnabled()) {
                Log.d(TAG, "Generated ${commands.size} commands with valid element hashes")
            }

            // Insert commands
            database.insertCommandBatch(commands)

            // ===== PHASE 5: Create/Update Screen Context (Phase 2) =====
            // Create content-based screen hash for stable identification
            // Using element structure fingerprint prevents duplicate screens
            val windowTitle = rootNode.text?.toString() ?: ""

            // Build a content fingerprint from visible elements to uniquely identify screen
            // This prevents counting the same screen multiple times even if windowTitle is empty
            val contentFingerprint = elements
                .filter { !it.className.contains("DecorView") && !it.className.contains("Layout") }
                .sortedBy { it.depth }
                .take(10)  // Use top 10 significant elements
                .joinToString("|") { e ->
                    "${e.className}:${e.text ?: ""}:${e.contentDescription ?: ""}:${e.isClickable}"
                }

            val screenHash = java.security.MessageDigest.getInstance("MD5")
                .digest("$packageName${event.className}$windowTitle$contentFingerprint".toByteArray())
                .joinToString("") { "%02x".format(it) }

            if (developerSettings.isVerboseLoggingEnabled()) {
                Log.d(TAG, "Screen identification: package=$packageName, activity=${event.className}, " +
                        "title='$windowTitle', elements=${elements.size}, hash=${screenHash.take(8)}...")
            }

            val existingScreenContext = database.databaseManager.screenContexts.getByHash(screenHash)

            if (existingScreenContext != null) {
                // Update existing screen context
                database.incrementVisitCount(screenHash, System.currentTimeMillis())
                if (developerSettings.isVerboseLoggingEnabled()) {
                    Log.d(TAG, "Updated screen context (visit count: ${existingScreenContext.visitCount + 1})")
                }
            } else {
                // Create new screen context
                val screenType = screenContextHelper.inferScreenType(
                    windowTitle = windowTitle,
                    activityName = event.className?.toString(),
                    elements = elements
                )

                val formContext = screenContextHelper.inferFormContext(elements)

                val primaryAction = screenContextHelper.inferPrimaryAction(elements)

                val hasBackButton = elements.any {
                    it.contentDescription?.contains("back", ignoreCase = true) == true ||
                    it.contentDescription?.contains("navigate up", ignoreCase = true) == true
                }

                val navigationLevel = screenContextHelper.inferNavigationLevel(
                    hasBackButton = hasBackButton,
                    windowTitle = windowTitle
                )

                val screenContext = com.augmentalis.voiceoscore.scraping.entities.ScreenContextEntity(
                    screenHash = screenHash,
                    appId = appId,
                    packageName = packageName,
                    activityName = event.className?.toString(),
                    windowTitle = windowTitle,
                    screenType = screenType,
                    formContext = formContext,
                    navigationLevel = navigationLevel,
                    primaryAction = primaryAction,
                    elementCount = elements.size,
                    hasBackButton = hasBackButton
                )

                database.insertScreenContext(screenContext)
                if (developerSettings.isVerboseLoggingEnabled()) {
                    Log.d(TAG, "Created screen context: type=$screenType, formContext=$formContext, primaryAction=$primaryAction")
                }

                // ===== PHASE 2.5: Assign Form Group IDs =====
                if (formContext != null) {
                    // Find all form-related elements (editable fields and form inputs)
                    val formElements = elements.filter { element ->
                        element.isEditable ||
                        element.semanticRole?.startsWith("input_") == true ||
                        element.className.contains("EditText", ignoreCase = true)
                    }

                    if (formElements.isNotEmpty()) {
                        // Generate stable group ID for this form
                        val groupId = screenContextHelper.generateFormGroupId(
                            packageName = packageName,
                            screenHash = screenHash,
                            elementDepth = formElements.firstOrNull()?.depth ?: 0,
                            formContext = formContext
                        )

                        // Update all form elements with the group ID
                        val elementHashes = formElements.map { it.elementHash }
                        database.updateFormGroupIdBatch(elementHashes, groupId)

                        if (developerSettings.isVerboseLoggingEnabled()) {
                            Log.d(TAG, "Assigned formGroupId '$groupId' to ${formElements.size} form elements")
                        }
                    }
                }

                // ===== PHASE 2.5: Infer Button→Form Relationships =====
                // Find submit buttons
                val submitButtons = elements.filter { element ->
                    element.semanticRole in listOf("submit_form", "submit_login", "submit_signup", "submit_payment") ||
                    (element.isClickable && element.className.contains("Button", ignoreCase = true) &&
                     element.text?.lowercase()?.let { text ->
                         text.contains("submit") || text.contains("login") || text.contains("sign in") ||
                         text.contains("continue") || text.contains("next") || text.contains("done") ||
                         text.contains("save") || text.contains("send")
                     } == true)
                }

                // Find form input fields
                val formInputs = elements.filter { it.isEditable || it.semanticRole?.startsWith("input_") == true }

                if (submitButtons.isNotEmpty() && formInputs.isNotEmpty()) {
                    val relationships = mutableListOf<com.augmentalis.voiceoscore.scraping.entities.ElementRelationshipEntity>()

                    submitButtons.forEach { button ->
                        // Find form inputs that precede this button (heuristic: same or shallower depth, earlier in tree)
                        val candidateInputs = formInputs.filter { input ->
                            // Input must come before button in traversal order
                            input.indexInParent < button.indexInParent &&
                            // Input should be at same depth or shallower (likely in same container)
                            kotlin.math.abs(input.depth - button.depth) <= 1
                        }

                        // Create relationship for each candidate input
                        candidateInputs.forEach { input ->
                            relationships.add(
                                com.augmentalis.voiceoscore.scraping.entities.ElementRelationshipEntity(
                                    sourceElementHash = button.elementHash,
                                    targetElementHash = input.elementHash,
                                    relationshipType = com.augmentalis.voiceoscore.scraping.entities.RelationshipType.BUTTON_SUBMITS_FORM,
                                    confidence = 0.8f
                                )
                            )
                        }
                    }

                    if (relationships.isNotEmpty()) {
                        database.insertRelationshipBatch(relationships)
                        if (developerSettings.isVerboseLoggingEnabled()) {
                            Log.d(TAG, "Created ${relationships.size} button→form relationships")
                        }
                    }
                }

                // ===== PHASE 2.5: Infer Label→Input Relationships =====
                // Find text labels (TextViews with content)
                val labels = elements.filter { element ->
                    element.className.contains("TextView", ignoreCase = true) &&
                    !element.className.contains("EditText", ignoreCase = true) &&
                    !element.className.contains("Button", ignoreCase = true) &&
                    element.text?.isNotBlank() == true
                }

                // Find input fields
                val inputs = elements.filter { it.isEditable || it.semanticRole?.startsWith("input_") == true }

                if (labels.isNotEmpty() && inputs.isNotEmpty()) {
                    val labelRelationships = mutableListOf<com.augmentalis.voiceoscore.scraping.entities.ElementRelationshipEntity>()

                    inputs.forEach { input ->
                        // Strategy 1: Find label immediately before input (same depth, previous index)
                        val adjacentLabel = labels.find { label ->
                            label.depth == input.depth &&
                            label.indexInParent == input.indexInParent - 1
                        }

                        if (adjacentLabel != null) {
                            labelRelationships.add(
                                com.augmentalis.voiceoscore.scraping.entities.ElementRelationshipEntity(
                                    sourceElementHash = adjacentLabel.elementHash,
                                    targetElementHash = input.elementHash,
                                    relationshipType = com.augmentalis.voiceoscore.scraping.entities.RelationshipType.LABEL_FOR,
                                    confidence = 0.9f  // High confidence for adjacent elements
                                )
                            )
                        } else {
                            // Strategy 2: Find label in parent container before input (one level shallower)
                            val parentLabel = labels.find { label ->
                                label.depth == input.depth - 1 &&
                                label.indexInParent < input.indexInParent
                            }

                            if (parentLabel != null) {
                                labelRelationships.add(
                                    com.augmentalis.voiceoscore.scraping.entities.ElementRelationshipEntity(
                                        sourceElementHash = parentLabel.elementHash,
                                        targetElementHash = input.elementHash,
                                        relationshipType = com.augmentalis.voiceoscore.scraping.entities.RelationshipType.LABEL_FOR,
                                        confidence = 0.7f  // Lower confidence for parent-level labels
                                    )
                                )
                            }
                        }
                    }

                    if (labelRelationships.isNotEmpty()) {
                        database.insertRelationshipBatch(labelRelationships)
                        if (developerSettings.isVerboseLoggingEnabled()) {
                            Log.d(TAG, "Created ${labelRelationships.size} label→input relationships")
                        }
                    }
                }

                // ===== PHASE 2.5: Track Screen Transitions =====
                if (lastScrapedScreenHash != null && lastScrapedScreenHash != screenHash) {
                    // Calculate transition time
                    val currentTime = System.currentTimeMillis()
                    val transitionTime = if (lastScreenTime > 0) {
                        currentTime - lastScreenTime
                    } else null

                    // Record the transition
                    val transition = com.augmentalis.voiceoscore.scraping.entities.ScreenTransitionEntity(
                        fromScreenHash = lastScrapedScreenHash.requireNotNull(
                            "lastScrapedScreenHash",
                            "Null after null check - this should not happen"
                        ),
                        toScreenHash = screenHash,
                        lastTransitionAt = System.currentTimeMillis(),
                        avgDurationMs = transitionTime ?: 0L
                    )
                    database.insertScreenTransition(transition)

                    if (developerSettings.isVerboseLoggingEnabled()) {
                        Log.d(TAG, "Recorded screen transition: ${lastScrapedScreenHash?.take(8)} → ${screenHash.take(8)}" +
                              if (transitionTime != null) " (${transitionTime}ms)" else "")
                    }
                }

                // Update last screen tracking
                lastScrapedScreenHash = screenHash
                lastScreenTime = System.currentTimeMillis()
            }

            // Update last scraped
            lastScrapedAppHash = appHash

            // P2-1: Update element and command counts AFTER all database operations complete
            database.updateElementCount(packageName, dbCount)
            database.updateCommandCount(packageName, commands.size)

            // YOLO Phase 2 - Issue #20: Process retry queue after scraping completes
            // Now that elements and screens are in database, retry any pending state changes
            processRetryQueue()

            // Phase 3: Record scraping analytics for observability
            scrapingAnalytics.recordScrape(
                packageName = packageName,
                elementCount = elements.size,
                durationMs = metrics.timeMs,
                success = true,
                cacheHit = cacheHit,
                treeDepth = maxTreeDepth
            )

            Log.i(TAG, "=== Scrape Complete: ${elements.size} elements, ${commands.size} commands ===")

            // Cleanup
            rootNode.recycle()

        } catch (e: Exception) {
            Log.e(TAG, "Error scraping window", e)

            // Phase 3: Record failed scrape for analytics
            try {
                val rootNode = accessibilityService.rootInActiveWindow
                val packageName = rootNode?.packageName?.toString() ?: "unknown"
                rootNode?.recycle()

                scrapingAnalytics.recordScrape(
                    packageName = packageName,
                    elementCount = 0,
                    durationMs = 0,
                    success = false,
                    cacheHit = false,
                    treeDepth = 0,
                    errorType = e.javaClass.simpleName
                )
            } catch (analyticsError: Exception) {
                Log.w(TAG, "Failed to record scraping error analytics", analyticsError)
            }
        }
    }

    /**
     * Recursively scrape accessibility tree with hash deduplication
     *
     * **Phase 1: Hash Deduplication** - Skips elements that already exist in database
     *
     * NOTE: This method tracks list indices (not database IDs) for hierarchy relationships.
     * Database IDs are assigned later during insertion.
     *
     * @param node Current accessibility node to scrape
     * @param appId App identifier
     * @param parentIndex Index of parent in elements list (null for root)
     * @param depth Depth in tree (0 for root)
     * @param indexInParent Index among siblings
     * @param elements List to collect scraped elements
     * @param hierarchyBuildInfo List to collect hierarchy relationships by index
     * @param filterNonActionable If true, skip non-actionable elements (default: false)
     * @param metrics Scraping metrics tracker
     * @return Index of this element in elements list, or -1 if skipped
     */
    private suspend fun scrapeNode(
        node: AccessibilityNodeInfo,
        appId: String,
        parentIndex: Int?,
        depth: Int,
        indexInParent: Int,
        elements: MutableList<ScrapedElementEntity>,
        hierarchyBuildInfo: MutableList<HierarchyBuildInfo>,
        filterNonActionable: Boolean = false,
        metrics: ScrapingMetrics? = null,
        customMaxDepth: Int = MAX_DEPTH  // Phase 3E: Feature flag override
    ): Int {
        // YOLO Phase 2 - High Priority Issue #16: Absolute maximum depth enforcement
        // FIX (2025-12-11): Reduced from 100 → 20 to prevent excessive memory allocation
        // Enforce absolute hard limit FIRST, regardless of memory pressure
        val ABSOLUTE_MAX_DEPTH = 20  // Hard limit to prevent stack overflow on malicious apps
        if (depth > ABSOLUTE_MAX_DEPTH) {
            Log.w(TAG, "ABSOLUTE max depth ($ABSOLUTE_MAX_DEPTH) exceeded at depth $depth, stopping traversal immediately")
            return -1
        }

        // Phase 3D: Adaptive depth limiting based on memory pressure
        val throttleLevel = resourceMonitor.getThrottleRecommendation()

        // Phase 3E: Use custom max depth if set, otherwise use default
        val baseMaxDepth = customMaxDepth

        val effectiveMaxDepth = when (throttleLevel) {
            ResourceMonitor.ThrottleLevel.HIGH -> baseMaxDepth / 4      // 25% depth when high pressure
            ResourceMonitor.ThrottleLevel.MEDIUM -> baseMaxDepth / 2     // 50% depth when medium pressure
            ResourceMonitor.ThrottleLevel.LOW -> (baseMaxDepth * 0.75).toInt()  // 75% depth when low pressure
            ResourceMonitor.ThrottleLevel.NONE -> baseMaxDepth           // Full depth when no pressure
        }.coerceAtMost(ABSOLUTE_MAX_DEPTH)  // Never exceed absolute maximum

        // Prevent stack overflow on pathological UI trees
        if (depth > effectiveMaxDepth) {
            if (throttleLevel != ResourceMonitor.ThrottleLevel.NONE) {
                if (developerSettings.isVerboseLoggingEnabled()) {
                    Log.d(TAG, "Throttled max depth ($effectiveMaxDepth) reached at element count ${elements.size} (throttle: $throttleLevel)")
                }
            } else {
                Log.w(TAG, "Max depth ($effectiveMaxDepth) reached at element count ${elements.size}, stopping traversal")
            }
            return -1
        }

        // Optional filtering: skip non-actionable elements
        if (filterNonActionable && !isActionable(node)) {
            Log.v(TAG, "Skipping non-actionable element at depth $depth: ${node.className}")

            // Still traverse children to find actionable descendants
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                try {
                    scrapeNode(child, appId, parentIndex, depth + 1, i, elements, hierarchyBuildInfo, filterNonActionable, metrics, customMaxDepth)
                } catch (e: Exception) {
                    Log.e(TAG, "Error scraping filtered child node", e)
                } finally {
                    child.recycle()
                }
            }

            return -1  // Indicate this node was skipped
        }

        try {
            // Get bounds
            val bounds = Rect()
            node.getBoundsInScreen(bounds)

            // Calculate element hash using AccessibilityFingerprint (hierarchy-aware, version-scoped)
            val packageName = node.packageName?.toString() ?: "unknown"
            val fingerprint = AccessibilityFingerprint.fromNode(
                node = node,
                packageName = packageName,
                appVersion = getAppVersion(packageName),
                calculateHierarchyPath = { calculateNodePath(it) }
            )
            val elementHash = fingerprint.generateHash()
            val stabilityScore = fingerprint.calculateStabilityScore()

            // ===== PHASE 1: Hash Deduplication - Check if element already exists =====
            metrics?.elementsFound = (metrics?.elementsFound ?: 0) + 1

            // P1-2: Check database for existing element and retrieve full entity (not just boolean)
            // This allows us to use the cached element's database ID for hierarchy building
            // PHASE 2 Issue #21: Removed runBlocking, using proper suspend function
            val cachedElement = database.databaseManager.scrapedElements.getByHash(elementHash)
            if (cachedElement != null) {
                metrics?.elementsCached = (metrics?.elementsCached ?: 0) + 1
                Log.v(TAG, "✓ CACHED (hash=$elementHash, id=${cachedElement.id}): ${node.className}")

                // P1-2: Element already in database - skip scraping but still traverse children
                // IMPORTANT: Pass cachedElement.id as parentIndex so children can build hierarchy correctly
                for (i in 0 until node.childCount) {
                    val child = node.getChild(i) ?: continue
                    try {
                        scrapeNode(child, appId, cachedElement.id.toInt(), depth + 1, i, elements, hierarchyBuildInfo, filterNonActionable, metrics, customMaxDepth)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error scraping cached element's child", e)
                    } finally {
                        child.recycle()
                    }
                }

                // P1-2: Return cached element's database ID (not -1) so hierarchy can be built
                return cachedElement.id.toInt()
            }

            // Element is NEW - proceed with scraping
            metrics?.elementsScraped = (metrics?.elementsScraped ?: 0) + 1
            Log.v(TAG, "⊕ SCRAPE (hash=$elementHash): ${node.className}")

            // Log unstable elements (optional - helps with debugging)
            // PII Redaction: Sanitize node text before logging
            if (stabilityScore < 0.7f) {
                PIILoggingWrapper.d(TAG, "Unstable element detected (score=$stabilityScore): ${node.className}, text=${node.text?.toString()}")
            }

            // Generate UUID for element using ThirdPartyUuidGenerator (synchronous to avoid blocking)
            val elementUuid = try {
                thirdPartyGenerator.generateUuidFromFingerprint(fingerprint)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to generate UUID for element, continuing without UUID", e)
                null
            }

            // ===== AI CONTEXT INFERENCE (PHASE 1) =====
            val resourceId = node.viewIdResourceName?.toString()
            val text = node.text?.toString()
            val contentDesc = node.contentDescription?.toString()
            val className = node.className?.toString() ?: "unknown"

            val semanticRole = semanticInferenceHelper.inferSemanticRole(
                node = node,
                resourceId = resourceId,
                text = text,
                contentDescription = contentDesc,
                className = className
            )

            val inputType = semanticInferenceHelper.inferInputType(
                node = node,
                resourceId = resourceId,
                text = text,
                contentDescription = contentDesc
            )

            val visualWeight = semanticInferenceHelper.inferVisualWeight(
                resourceId = resourceId,
                text = text,
                className = className
            )

            val isRequired = semanticInferenceHelper.inferIsRequired(
                contentDescription = contentDesc,
                text = text,
                resourceId = resourceId
            ) ?: false

            // ===== AI CONTEXT INFERENCE (PHASE 2) =====
            val placeholderText = screenContextHelper.extractPlaceholderText(node)

            val validationPattern = screenContextHelper.inferValidationPattern(
                node = node,
                resourceId = resourceId,
                inputType = inputType,
                className = className
            )

            val backgroundColor = screenContextHelper.extractBackgroundColor(node)

            // formGroupId will be set at screen level after all elements are collected

            // Create element entity (ID will be auto-generated by database)
            val element = ScrapedElementEntity(
                elementHash = elementHash,
                appId = appId,
                uuid = elementUuid,
                className = className,
                viewIdResourceName = resourceId,
                text = text,
                contentDescription = contentDesc,
                bounds = boundsToJson(bounds),
                isClickable = node.isClickable,
                isLongClickable = node.isLongClickable,
                isEditable = node.isEditable,
                isScrollable = node.isScrollable,
                isCheckable = node.isCheckable,
                isFocusable = node.isFocusable,
                isEnabled = node.isEnabled,
                depth = depth,
                indexInParent = indexInParent,
                // AI Context (Phase 1)
                semanticRole = semanticRole,
                inputType = inputType,
                visualWeight = visualWeight,
                isRequired = isRequired,
                // AI Context (Phase 2)
                formGroupId = null,  // Set later at screen level
                placeholderText = placeholderText,
                validationPattern = validationPattern,
                backgroundColor = backgroundColor
            )

            // Get current list index BEFORE adding element
            val currentIndex = elements.size

            // Add element to list
            elements.add(element)

            // ===== METADATA QUALITY VALIDATION (PHASE 1 INTEGRATION) =====
            try {
                val metadataValidator = com.augmentalis.voiceoscore.learnapp.validation.MetadataValidator(context)
                val qualityScore = metadataValidator.validateElement(node)

                if (!qualityScore.isSufficient()) {
                    Log.w(TAG, "Poor metadata quality at depth $depth:")
                    Log.w(TAG, "  Class: ${qualityScore.className}")
                    Log.w(TAG, "  Score: ${String.format("%.2f", qualityScore.score)}")
                    qualityScore.getPrioritySuggestion()?.let { suggestion ->
                        Log.w(TAG, "  Priority: $suggestion")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error validating metadata", e)
            }
            // ===== END METADATA VALIDATION =====

            // ===== ENHANCED DEBUG LOGGING =====
            // PII Redaction: Sanitize text and content description before logging
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                val indent = "  ".repeat(depth)
                PIILoggingWrapper.d(TAG, "${indent}[${currentIndex}] ${element.className}")
                if (!element.text.isNullOrBlank()) {
                    PIILoggingWrapper.d(TAG, "${indent}  text: ${element.text}")
                }
                if (!element.contentDescription.isNullOrBlank()) {
                    PIILoggingWrapper.d(TAG, "${indent}  desc: ${element.contentDescription}")
                }
                if (!element.viewIdResourceName.isNullOrBlank()) {
                    // Resource IDs are OK - they're developer-defined identifiers, not user content
                    if (developerSettings.isVerboseLoggingEnabled()) {
                        Log.d(TAG, "${indent}  id: ${element.viewIdResourceName}")
                    }
                }
            }
            // ===== END ENHANCED LOGGING =====

            // Track hierarchy relationship using list indices (not IDs)
            if (parentIndex != null) {
                // Validate parent index is within bounds
                if (parentIndex >= 0 && parentIndex < elements.size) {
                    hierarchyBuildInfo.add(
                        HierarchyBuildInfo(
                            childListIndex = currentIndex,
                            parentListIndex = parentIndex,
                            childOrder = indexInParent,
                            depth = 1
                        )
                    )
                } else {
                    Log.w(TAG, "Invalid parent index $parentIndex for element $currentIndex (total: ${elements.size})")
                }
            }

            // Recurse for children
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                try {
                    scrapeNode(child, appId, currentIndex, depth + 1, i, elements, hierarchyBuildInfo, filterNonActionable, metrics, customMaxDepth)
                } catch (e: Exception) {
                    Log.e(TAG, "Error scraping child node", e)
                } finally {
                    child.recycle()
                }
            }

            // FIX (2025-11-30): P2 - Scroll-to-load for RecyclerView off-screen items
            // After processing visible children, scroll to load and scrape more items
            if (SCROLL_TO_LOAD_ENABLED && isScrollableContainer(node)) {
                try {
                    val additionalElements = scrollAndScrapeMore(
                        node, appId, currentIndex, depth,
                        elements, hierarchyBuildInfo, filterNonActionable, metrics, customMaxDepth
                    )
                    if (additionalElements > 0) {
                        if (developerSettings.isVerboseLoggingEnabled()) {
                            Log.d(TAG, "Scroll-to-load added $additionalElements elements from ${node.className}")
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error during scroll-to-load for ${node.className}", e)
                }
            }

            return currentIndex

        } catch (e: Exception) {
            Log.e(TAG, "Error in scrapeNode", e)
            return -1
        }
    }

    /**
     * Check if node is actionable (worth scraping for voice commands)
     *
     * A node is actionable if it has any interactive capability
     * or contains meaningful text/description that could be a voice target.
     *
     * @param node AccessibilityNodeInfo to check
     * @return true if node should be scraped for commands
     */
    private fun isActionable(node: AccessibilityNodeInfo): Boolean {
        return node.isClickable ||
            node.isLongClickable ||
            node.isEditable ||
            node.isScrollable ||
            node.isCheckable ||
            !node.text.isNullOrBlank() ||
            !node.contentDescription.isNullOrBlank()
    }

    /**
     * Check if node is a scrollable container that supports scroll-to-load
     *
     * FIX (2025-11-30): P2 - Hierarchy gap fix for RecyclerView off-screen items.
     *
     * @param node AccessibilityNodeInfo to check
     * @return true if node is a scrollable RecyclerView/ListView
     */
    private fun isScrollableContainer(node: AccessibilityNodeInfo): Boolean {
        if (!SCROLL_TO_LOAD_ENABLED) return false
        if (!node.isScrollable) return false

        val className = node.className?.toString() ?: return false
        return SCROLLABLE_VIEW_CLASSES.any { className.contains(it) || className == it }
    }

    /**
     * Perform scroll-to-load on a scrollable container to reveal off-screen items
     *
     * FIX (2025-11-30): P2 - Enables scraping of RecyclerView items that are off-screen.
     * Scrolls the container and scrapes newly visible items.
     *
     * @param node Scrollable container node
     * @param appId App identifier
     * @param parentIndex Parent index for hierarchy building
     * @param depth Current depth in tree
     * @param elements List to collect scraped elements
     * @param hierarchyBuildInfo List to collect hierarchy relationships
     * @param filterNonActionable If true, skip non-actionable elements
     * @param metrics Scraping metrics tracker
     * @param customMaxDepth Custom max depth limit
     * @return Number of additional elements scraped via scrolling
     */
    private suspend fun scrollAndScrapeMore(
        node: AccessibilityNodeInfo,
        appId: String,
        parentIndex: Int,
        depth: Int,
        elements: MutableList<ScrapedElementEntity>,
        hierarchyBuildInfo: MutableList<HierarchyBuildInfo>,
        filterNonActionable: Boolean,
        metrics: ScrapingMetrics?,
        customMaxDepth: Int
    ): Int {
        if (!isScrollableContainer(node)) return 0

        var totalNewElements = 0
        var scrollAttempts = 0
        val seenChildHashes = mutableSetOf<String>()

        // Record initial children hashes to detect new items
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            try {
                val hash = "${child.className}:${child.text}:${child.contentDescription}:${child.viewIdResourceName}"
                seenChildHashes.add(hash)
            } finally {
                child.recycle()
            }
        }

        if (developerSettings.isVerboseLoggingEnabled()) {
            Log.d(TAG, "Starting scroll-to-load for ${node.className} with ${seenChildHashes.size} initial children")
        }

        while (scrollAttempts < MAX_SCROLL_ATTEMPTS) {
            // Perform scroll
            val scrolled = node.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
            if (!scrolled) {
                if (developerSettings.isVerboseLoggingEnabled()) {
                    Log.d(TAG, "Scroll-to-load: No more scrolling possible after $scrollAttempts attempts")
                }
                break
            }

            scrollAttempts++
            delay(SCROLL_DELAY_MS)

            // Check for new children
            var newChildrenFound = 0
            val currentChildCount = node.childCount

            for (i in 0 until currentChildCount) {
                val child = node.getChild(i) ?: continue
                try {
                    val hash = "${child.className}:${child.text}:${child.contentDescription}:${child.viewIdResourceName}"

                    if (!seenChildHashes.contains(hash)) {
                        seenChildHashes.add(hash)
                        newChildrenFound++

                        // Scrape this newly visible child
                        val result = scrapeNode(
                            child, appId, parentIndex, depth + 1, i,
                            elements, hierarchyBuildInfo, filterNonActionable, metrics, customMaxDepth
                        )
                        if (result >= 0) {
                            totalNewElements++
                        }
                    }
                } finally {
                    child.recycle()
                }
            }

            if (developerSettings.isVerboseLoggingEnabled()) {
                Log.d(TAG, "Scroll-to-load attempt $scrollAttempts: found $newChildrenFound new children")
            }

            if (newChildrenFound == 0) {
                if (developerSettings.isVerboseLoggingEnabled()) {
                    Log.d(TAG, "Scroll-to-load: No new children after scroll, stopping")
                }
                break
            }
        }

        Log.i(TAG, "Scroll-to-load complete: scraped $totalNewElements additional elements in $scrollAttempts scrolls")
        return totalNewElements
    }

    /**
     * Wait for screen to stabilize before scraping.
     * Detects when element count stops changing (idle state).
     *
     * This addresses the dynamic async-loaded content gap where elements appear
     * 500ms-2s after screen loads (e.g., social media feeds, search results).
     *
     * @param rootNode The root node to monitor
     * @param timeoutMs Maximum time to wait for stability (default: 3000ms)
     * @return true if screen stabilized, false if timeout
     */
    private suspend fun waitForScreenStable(
        rootNode: AccessibilityNodeInfo,
        timeoutMs: Long = SCREEN_STABLE_TIMEOUT_MS
    ): Boolean {
        val startTime = System.currentTimeMillis()
        var previousCount = 0
        var stableCount = 0

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            rootNode.refresh()
            val currentCount = countAllNodes(rootNode)

            if (currentCount == previousCount) {
                stableCount++
                if (stableCount >= STABLE_THRESHOLD) {
                    val elapsedMs = System.currentTimeMillis() - startTime
                    if (developerSettings.isVerboseLoggingEnabled()) {
                        Log.d(TAG, "Screen stable after ${elapsedMs}ms (${currentCount} nodes)")
                    }
                    return true
                }
            } else {
                stableCount = 0  // Reset counter when count changes
                if (developerSettings.isVerboseLoggingEnabled()) {
                    Log.d(TAG, "Screen unstable: $previousCount -> $currentCount nodes")
                }
            }

            previousCount = currentCount
            delay(STABLE_CHECK_INTERVAL_MS)
        }

        Log.w(TAG, "Screen did not stabilize within ${timeoutMs}ms")
        return false
    }

    /**
     * Recursively count all nodes in the accessibility tree.
     *
     * Used by waitForScreenStable() to detect when async content has finished loading.
     *
     * @param node The root node to count from
     * @return Total number of nodes in tree
     */
    private fun countAllNodes(node: AccessibilityNodeInfo): Int {
        var count = 1
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                try {
                    count += countAllNodes(child)
                } finally {
                    child.recycle()
                }
            }
        }
        return count
    }

    /**
     * Calculates the hierarchy path from root to the given node.
     * Path format: "/0/1/3" means root → 1st child → 2nd child → 4th child
     *
     * IMPORTANT: Properly recycles AccessibilityNodeInfo to prevent memory leaks.
     *
     * @param node The node to calculate path for
     * @return Hierarchy path string (e.g., "/0/1/3") or "/" for root
     */
    private fun calculateNodePath(node: AccessibilityNodeInfo): String {
        val path = mutableListOf<Int>()
        var current: AccessibilityNodeInfo? = node
        val nodesToRecycle = mutableListOf<AccessibilityNodeInfo>()

        try {
            while (current != null) {
                val parent = current.parent
                if (parent != null) {
                    val index = findChildIndex(parent, current)
                    if (index >= 0) {
                        path.add(0, index)  // Prepend to front
                    }

                    // Don't recycle the original node
                    if (current != node) {
                        nodesToRecycle.add(current)
                    }

                    current = parent
                    nodesToRecycle.add(parent)
                } else {
                    break
                }
            }

            return if (path.isEmpty()) "/" else "/" + path.joinToString("/")
        } finally {
            // Always recycle nodes to prevent memory leaks
            nodesToRecycle.forEach { it.recycle() }
        }
    }

    /**
     * Finds the index of a child node within its parent.
     *
     * @param parent The parent node
     * @param child The child node to find
     * @return Child index (0-based) or -1 if not found
     */
    private fun findChildIndex(parent: AccessibilityNodeInfo, child: AccessibilityNodeInfo): Int {
        for (i in 0 until parent.childCount) {
            val c = parent.getChild(i)
            val isMatch = c == child
            c?.recycle()  // Recycle immediately after comparison

            if (isMatch) {
                return i
            }
        }
        return -1
    }

    /**
     * Get app version name for a given package.
     *
     * @param packageName The package name to query
     * @return Version name string, or "unknown" if not found
     */
    private fun getAppVersion(packageName: String): String {
        return try {
            context.packageManager
                .getPackageInfo(packageName, 0)
                .versionName ?: "unknown"
        } catch (e: Exception) {
            Log.w(TAG, "Could not get app version for $packageName", e)
            "unknown"
        }
    }

    /**
     * Convert bounds to JSON string
     */
    private fun boundsToJson(bounds: Rect): String {
        return com.augmentalis.voiceos.json.JsonConverters.boundsToJson(
            bounds.left, bounds.top, bounds.right, bounds.bottom
        )
    }

    /**
     * Process voice command
     *
     * Call this when speech recognition detects a command
     *
     * @param voiceInput Voice command text
     * @return CommandResult with success status and message
     */
    suspend fun processVoiceCommand(voiceInput: String): CommandResult {
        return voiceCommandProcessor.processCommand(voiceInput)
    }

    /**
     * Process text input command
     *
     * @param targetCommand Command to identify input field (e.g., "type in search")
     * @param text Text to input
     */
    suspend fun processTextInput(targetCommand: String, text: String): CommandResult {
        return voiceCommandProcessor.executeTextInput(targetCommand, text)
    }

    /**
     * Learn entire app by traversing all screens
     *
     * This performs a comprehensive UI traversal, attempting to visit
     * all screens and discover all elements. It merges with existing
     * dynamic data using hash-based matching (UPSERT).
     *
     * Workflow:
     * 1. Get or create app entity
     * 2. Set scraping mode to LEARN_APP
     * 3. Scrape all visible elements (full tree traversal)
     * 4. Merge elements using upsertElement() (hash-based)
     * 5. Mark app as fully learned
     * 6. Restore scraping mode to DYNAMIC
     *
     * @param packageName Package to learn
     * @return LearnAppResult with statistics
     */
    suspend fun learnApp(packageName: String): LearnAppResult {
        Log.i(TAG, "=== Starting LearnApp Mode for $packageName ===")

        return kotlinx.coroutines.withContext(Dispatchers.IO) {
            try {
                // Get app info
                val appInfo = try {
                    packageManager.getPackageInfo(packageName, 0)
                } catch (e: Exception) {
                    Log.e(TAG, "Cannot get package info for $packageName", e)
                    return@withContext LearnAppResult(
                        success = false,
                        message = "Cannot access app: ${e.message}",
                        elementsDiscovered = 0,
                        newElements = 0,
                        updatedElements = 0
                    )
                }

                // Calculate app hash
                val appHash = HashUtils.calculateAppHash(packageName, appInfo.longVersionCode.toInt())
                if (developerSettings.isVerboseLoggingEnabled()) {
                    Log.d(TAG, "App hash: $appHash")
                }

                // Get or create app entity
                var app = database.getAppByHash(appHash)
                val appId = if (app != null) {
                    if (developerSettings.isVerboseLoggingEnabled()) {
                        Log.d(TAG, "App exists in database: ${app.appName}")
                    }
                    app.packageName
                } else {
                    // Create new app entity
                    val newAppId = UUID.randomUUID().toString()
                    val currentTime = System.currentTimeMillis()
                    app = AppEntity(
                        packageName = packageName,
                        appName = appInfo.applicationInfo.loadLabel(packageManager).toString(),
                        versionCode = appInfo.longVersionCode,
                        versionName = appInfo.versionName ?: "unknown",
                        lastScraped = currentTime,
                        dynamicScrapingEnabled = false  // MODE_LEARN_APP equivalent (learning mode, not dynamic)
                    )
                    database.insertApp(app)
                    if (developerSettings.isVerboseLoggingEnabled()) {
                        Log.d(TAG, "Created new app entity: ${app.appName}")
                    }
                    newAppId
                }

                // Update mode to LEARN_APP
                database.updateScrapingMode(packageName, MODE_LEARN_APP)

                // Get root node
                val rootNode = accessibilityService.rootInActiveWindow
                if (rootNode == null) {
                    Log.e(TAG, "Cannot start LearnApp - no root node")
                    database.updateScrapingMode(packageName, MODE_DYNAMIC)
                    return@withContext LearnAppResult(
                        success = false,
                        message = "No accessibility access - ensure service is enabled",
                        elementsDiscovered = 0,
                        newElements = 0,
                        updatedElements = 0
                    )
                }

                // Verify we're on the correct app
                val currentPackage = rootNode.packageName?.toString()
                if (currentPackage != packageName) {
                    Log.w(TAG, "Current app ($currentPackage) doesn't match target ($packageName)")
                    rootNode.recycle()
                    database.updateScrapingMode(packageName, MODE_DYNAMIC)
                    return@withContext LearnAppResult(
                        success = false,
                        message = "Please navigate to $packageName before learning",
                        elementsDiscovered = 0,
                        newElements = 0,
                        updatedElements = 0
                    )
                }

                Log.i(TAG, "Starting comprehensive UI traversal...")

                // Scrape all elements (similar to dynamic, but using LearnApp mode)
                val elements = mutableListOf<ScrapedElementEntity>()
                val hierarchyBuildInfo = mutableListOf<HierarchyBuildInfo>()

                scrapeNode(rootNode, appId, null, 0, 0, elements, hierarchyBuildInfo)

                rootNode.recycle()

                Log.i(TAG, "Scraped ${elements.size} elements")

                // Merge elements using upsert (hash-based deduplication)
                var newCount = 0
                var updatedCount = 0

                for (element in elements) {
                    val existing = database.databaseManager.scrapedElements.getByHash(element.elementHash)
                    if (existing != null) {
                        updatedCount++
                    } else {
                        newCount++
                    }
                    database.upsertElement(element)
                }

                Log.i(TAG, "Merge complete: $newCount new, $updatedCount updated")

                // Mark app as fully learned
                val completionTime = System.currentTimeMillis()
                database.markAsFullyLearned(packageName, completionTime)
                database.updateElementCount(packageName, elements.size)

                // Update scraping mode back to DYNAMIC
                database.updateScrapingMode(packageName, MODE_DYNAMIC)

                // Generate commands for new elements
                if (newCount > 0) {
                    if (developerSettings.isVerboseLoggingEnabled()) {
                        Log.d(TAG, "Generating commands for $newCount new elements...")
                    }
                    // Get all elements with real database IDs
                    val allElementDtos = database.databaseManager.scrapedElements.getByApp(appId)
                    val allElements = allElementDtos.map { it.toScrapedElementEntity() }
                    val commands = commandGenerator.generateCommandsForElements(allElements)
                    database.insertCommandBatch(commands)
                    database.updateCommandCount(packageName, commands.size)
                    if (developerSettings.isVerboseLoggingEnabled()) {
                        Log.d(TAG, "Generated ${commands.size} total commands")
                    }
                }

                Log.i(TAG, "=== LearnApp Complete: ${elements.size} total, $newCount new, $updatedCount updated ===")

                val appName = app?.appName ?: "unknown app"
                LearnAppResult(
                    success = true,
                    message = "Successfully learned $appName",
                    elementsDiscovered = elements.size,
                    newElements = newCount,
                    updatedElements = updatedCount
                )

            } catch (e: Exception) {
                Log.e(TAG, "Error in LearnApp mode", e)
                LearnAppResult(
                    success = false,
                    message = "Error: ${e.message}",
                    elementsDiscovered = 0,
                    newElements = 0,
                    updatedElements = 0
                )
            }
        }
    }

    /**
     * Register element with UUID Creator
     *
     * Generates and registers a UUID for a scraped element, enabling universal
     * voice command identification across the system.
     *
     * @param element ScrapedElementEntity to register
     * @param node AccessibilityNodeInfo for UUID generation
     * @param packageName App package name
     * @return Generated UUID string, or null if registration failed
     */
    private suspend fun registerElementWithUUID(
        element: ScrapedElementEntity,
        node: AccessibilityNodeInfo,
        packageName: String
    ): String? {
        return try {
            // Generate UUID from accessibility node
            val uuid = thirdPartyGenerator.generateUuid(node, packageName)

            // Create UUIDElement with metadata
            val uuidElement = UUIDElement(
                uuid = uuid,
                name = element.text ?: element.contentDescription ?: "Unknown",
                type = element.className?.substringAfterLast('.') ?: "unknown",
                description = element.contentDescription,
                metadata = UUIDMetadata(
                    label = element.text,
                    hint = element.contentDescription,
                    attributes = mapOf(
                        "thirdPartyApp" to "true",
                        "packageName" to packageName,
                        "className" to (element.className ?: ""),
                        "resourceId" to (element.viewIdResourceName ?: ""),
                        "elementHash" to element.elementHash
                    ),
                    accessibility = UUIDAccessibility(
                        contentDescription = element.contentDescription,
                        isClickable = element.isClickable,
                        isFocusable = element.isFocusable,
                        isScrollable = element.isScrollable
                    )
                )
            )

            // Register with UUIDCreator
            uuidCreator.registerElement(uuidElement)

            // TODO: Re-enable when createAutoAlias is implemented
            // Create auto-generated alias for easier voice commands
            // aliasManager.createAutoAlias(
            //     uuid = uuid,
            //     elementName = uuidElement.name ?: "element",
            //     elementType = uuidElement.type
            // )

            // PII Redaction: Sanitize element text before logging
            PIILoggingWrapper.d(TAG, "Registered UUID for element: ${element.text} → $uuid")
            uuid
        } catch (e: Exception) {
            // PII Redaction: Sanitize element text before logging
            PIILoggingWrapper.e(TAG, "Failed to register UUID for element: ${element.text}", e)
            null
        }
    }

    /**
     * Get comprehensive scraping analytics summary (Phase 3)
     *
     * Provides detailed analytics including:
     * - Success/failure rates
     * - Scraping time statistics (min, max, avg, p95)
     * - Cache hit/miss ratios
     * - Top 10 most scraped apps
     * - Tree traversal statistics
     * - Error pattern analysis
     *
     * @return AnalyticsSummary with comprehensive metrics
     */
    fun getScrapingAnalyticsSummary(): AnalyticsSummary {
        return scrapingAnalytics.getSummary()
    }

    /**
     * Get scraping analytics for a specific app
     *
     * @param packageName Package name to get analytics for
     * @return AppAnalytics for the specific app, or null if not tracked
     */
    fun getAppScrapingAnalytics(packageName: String): com.augmentalis.voiceoscore.utils.AppAnalytics? {
        return scrapingAnalytics.getAppAnalytics(packageName)
    }

    /**
     * Reset scraping analytics
     *
     * Clears all collected analytics data
     */
    fun resetScrapingAnalytics() {
        scrapingAnalytics.reset()
    }

    /**
     * Cleanup resources
     *
     * YOLO Phase 2 - Issue #10: Clear all ConcurrentHashMap caches to prevent memory leaks
     * YOLO Phase 2 - Issue #20: Clear retry queue on cleanup
     */
    fun cleanup() {
        if (developerSettings.isVerboseLoggingEnabled()) {
            Log.d(TAG, "Cleaning up scraping integration...")
        }

        // Clear all ConcurrentHashMap caches to prevent memory leaks
        try {
            val visibilitySize = elementVisibilityTracker.size
            val stateSize = elementStateTracker.size
            val packageCacheSize = packageInfoCache.size
            val retryQueueSize = stateChangeRetryQueue.size

            if (developerSettings.isVerboseLoggingEnabled()) {
                Log.d(TAG, "Clearing caches:")
                Log.d(TAG, "  - elementVisibilityTracker: $visibilitySize entries")
                Log.d(TAG, "  - elementStateTracker: $stateSize entries")
                Log.d(TAG, "  - packageInfoCache: $packageCacheSize entries")
                Log.d(TAG, "  - stateChangeRetryQueue: $retryQueueSize entries")
            }

            elementVisibilityTracker.clear()
            elementStateTracker.clear()
            packageInfoCache.clear()
            stateChangeRetryQueue.clear()

            Log.i(TAG, "✓ All caches cleared successfully")
            Log.i(TAG, "  - Total entries cleared: ${visibilitySize + stateSize + packageCacheSize + retryQueueSize}")

        } catch (e: Exception) {
            Log.e(TAG, "✗ Error clearing caches during cleanup", e)
            Log.e(TAG, "  Error type: ${e.javaClass.simpleName}")
            Log.e(TAG, "  Error message: ${e.message}")
        }

        // Coroutine scope will be cancelled by service
        if (developerSettings.isVerboseLoggingEnabled()) {
            Log.d(TAG, "Scraping integration cleaned up")
        }
    }

    /**
     * Helper data class to track hierarchy relationships during build phase.
     * Uses list indices (not database IDs) because IDs aren't assigned yet.
     *
     * @property childListIndex Index in elements list (will be mapped to DB ID)
     * @property parentListIndex Index in elements list (will be mapped to DB ID)
     * @property childOrder Order of child among siblings
     * @property depth Depth in hierarchy tree
     */
    private data class HierarchyBuildInfo(
        val childListIndex: Int,
        val parentListIndex: Int,
        val childOrder: Int,
        val depth: Int
    )

    /**
     * YOLO Phase 2 - Issue #20: Pending state change for retry queue
     *
     * Tracks state changes that failed because element/screen wasn't scraped yet.
     * These are retried on next scrape cycle to ensure no state changes are lost.
     *
     * @property elementHash Hash of element whose state changed
     * @property screenHash Hash of screen where change occurred
     * @property stateType Type of state (CHECKED, ENABLED, VISIBLE, etc.)
     * @property oldValue Previous state value
     * @property newValue New state value
     * @property triggeredBy What triggered the change (USER_CLICK, SYSTEM, etc.)
     * @property attemptCount Number of retry attempts (max 5)
     * @property timestamp When state change was first detected
     */
    private data class PendingStateChange(
        val elementHash: String,
        val screenHash: String,
        val stateType: String,
        val oldValue: String?,
        val newValue: String?,
        val triggeredBy: String,
        val attemptCount: Int = 0,
        val timestamp: Long = System.currentTimeMillis()
    )

    // ========== Phase 3: User Interaction Tracking ==========

    /**
     * Check if interaction learning is enabled
     * Respects user settings and battery level
     *
     * @return true if learning should happen, false otherwise
     */
    private fun isInteractionLearningEnabled(): Boolean {
        // Check user preference (default: true)
        val userEnabled = preferences.getBoolean(PREF_INTERACTION_LEARNING_ENABLED, true)
        if (!userEnabled) {
            return false
        }

        // Check battery level
        val batteryLevel = getBatteryLevel()
        return batteryLevel > MIN_BATTERY_LEVEL_FOR_LEARNING
    }

    /**
     * Get current battery level as percentage
     *
     * @return Battery level 0-100, or 100 if unable to determine
     */
    private fun getBatteryLevel(): Int {
        return try {
            val batteryStatus: Intent? = context.registerReceiver(
                null,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )
            val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1

            if (level == -1 || scale == -1) {
                100 // Assume full battery if unable to determine
            } else {
                (level * 100 / scale.toFloat()).toInt()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Unable to get battery level", e)
            100 // Assume full battery on error
        }
    }

    /**
     * Enable or disable interaction learning
     * Called from VoiceOS settings UI
     *
     * @param enabled Whether to enable learning
     */
    fun setInteractionLearningEnabled(enabled: Boolean) {
        preferences.edit()
            .putBoolean(PREF_INTERACTION_LEARNING_ENABLED, enabled)
            .apply()
        Log.i(TAG, "Interaction learning ${if (enabled) "enabled" else "disabled"}")
    }

    /**
     * Check if interaction learning is currently enabled
     *
     * @return Current setting value
     */
    fun isInteractionLearningUserEnabled(): Boolean {
        return preferences.getBoolean(PREF_INTERACTION_LEARNING_ENABLED, true)
    }

    /**
     * Get package version with caching for performance
     *
     * Retrieves package version code from cache or PackageManager.
     * Cache is invalidated on window state changes to detect app updates.
     *
     * @param packageName Package name to look up
     * @return Pair of (packageName, versionCode), or ("unknown", 0) on error
     */
    private fun getPackageInfoCached(packageName: String): Pair<String, Long> {
        // Try cache first
        packageInfoCache[packageName]?.let { return it }

        // Cache miss - lookup from PackageManager
        return try {
            val info = packageManager.getPackageInfo(packageName, 0)
            val result = Pair(packageName, info.longVersionCode)
            packageInfoCache[packageName] = result
            result
        } catch (e: Exception) {
            Log.w(TAG, "Error getting package info for $packageName", e)
            Pair("unknown", 0)
        }
    }

    /**
     * Record user interaction with an element
     *
     * @param event Accessibility event containing interaction data
     * @param interactionType Type of interaction (click, long_press, etc.)
     */
    private suspend fun recordInteraction(
        event: AccessibilityEvent,
        interactionType: String
    ) {
        // Check if learning is enabled (user setting + battery level)
        if (!isInteractionLearningEnabled()) {
            return
        }

        try {
            val node = event.source ?: return

            // Extract package info for fingerprinting (with caching for performance)
            val nodePkgName = node.packageName?.toString() ?: "unknown"
            val (packageName, versionCode) = getPackageInfoCached(nodePkgName)
            val appVersion = versionCode.toString()

            val elementHash = AccessibilityFingerprint.fromNode(node, packageName, appVersion).generateHash()
            val screenHash = lastScrapedScreenHash ?: return

            // FOREIGN KEY VALIDATION: Verify parent records exist before inserting interaction
            // This prevents SQLiteConstraintException when elements/screens haven't been scraped yet
            // (race condition: user interactions can occur before window scraping completes)

            // Verify element exists in database (FK: element_hash -> scraped_elements.element_hash)
            val elementExists = database.databaseManager.scrapedElements.getByHash(elementHash) != null
            if (!elementExists) {
                Log.v(TAG, "Skipping $interactionType interaction - element not scraped yet: $elementHash")
                node.recycle()
                return
            }

            // Verify screen exists in database (FK: screen_hash -> screen_contexts.screen_hash)
            val screenExists = database.databaseManager.screenContexts.getByHash(screenHash) != null
            if (!screenExists) {
                Log.v(TAG, "Skipping $interactionType interaction - screen not scraped yet: $screenHash")
                node.recycle()
                return
            }

            // Get visibility start time (if tracked)
            val visibilityStart = elementVisibilityTracker[elementHash]
            val visibilityDuration = visibilityStart?.let {
                System.currentTimeMillis() - it
            }

            // Create interaction entity
            val interaction = com.augmentalis.voiceoscore.scraping.entities.UserInteractionEntity(
                elementHash = elementHash,
                screenHash = screenHash,
                interactionType = interactionType,
                visibilityStart = visibilityStart,
                visibilityDuration = visibilityDuration
            )

            // Save to database (safe - both FK parents verified to exist)
            database.insertUserInteraction(interaction)
            if (developerSettings.isVerboseLoggingEnabled()) {
                Log.d(TAG, "Recorded $interactionType interaction for element $elementHash (visibility: ${visibilityDuration}ms)")
            }

            node.recycle()
        } catch (e: Exception) {
            Log.e(TAG, "Error recording interaction", e)
        }
    }

    /**
     * Record element state change
     *
     * @param event Accessibility event containing state change data
     * @param stateType Type of state that changed
     */
    private suspend fun recordStateChange(
        event: AccessibilityEvent,
        stateType: String
    ) {
        // Check if learning is enabled (user setting + battery level)
        if (!isInteractionLearningEnabled()) {
            return
        }

        try {
            val node = event.source ?: return

            // Extract package info for fingerprinting (with caching for performance)
            val nodePkgName = node.packageName?.toString() ?: "unknown"
            val (packageName, versionCode) = getPackageInfoCached(nodePkgName)
            val appVersion = versionCode.toString()

            val elementHash = AccessibilityFingerprint.fromNode(node, packageName, appVersion).generateHash()
            val screenHash = lastScrapedScreenHash ?: return

            // Get previous state
            val previousStates = elementStateTracker.getOrPut(elementHash) { mutableMapOf() }
            val oldValue = previousStates[stateType]

            // Determine new value based on state type
            val newValue = when (stateType) {
                com.augmentalis.voiceoscore.scraping.entities.StateType.CHECKED -> node.isChecked.toString()
                com.augmentalis.voiceoscore.scraping.entities.StateType.SELECTED -> node.isSelected.toString()
                com.augmentalis.voiceoscore.scraping.entities.StateType.ENABLED -> node.isEnabled.toString()
                com.augmentalis.voiceoscore.scraping.entities.StateType.FOCUSED -> node.isFocused.toString()
                com.augmentalis.voiceoscore.scraping.entities.StateType.VISIBLE -> node.isVisibleToUser.toString()
                else -> null
            }

            // Only record if state actually changed
            if (oldValue != newValue) {
                val stateChange = com.augmentalis.voiceoscore.scraping.entities.ElementStateHistoryEntity(
                    elementHash = elementHash,
                    screenHash = screenHash,
                    stateType = stateType,
                    oldValue = oldValue,
                    newValue = newValue,
                    triggeredBy = determineTrigerSource(event)
                )

                // YOLO FIX: Use insertOrIgnore to prevent FK constraint violations
                // If element/screen not in DB yet, insert is silently ignored
                val insertedId = database.insertElementStateHistory(stateChange)
                if (insertedId > 0) {
                    previousStates[stateType] = newValue
                    if (developerSettings.isVerboseLoggingEnabled()) {
                        Log.d(TAG, "Recorded state change: $stateType from $oldValue to $newValue")
                    }
                } else {
                    if (developerSettings.isVerboseLoggingEnabled()) {
                        Log.d(TAG, "Skipped state change for unscraped element/screen (FK constraint)")
                    }
                }
            }

            node.recycle()
        } catch (e: Exception) {
            Log.e(TAG, "Error recording state change", e)
        }
    }

    /**
     * Track content changes to detect state updates
     *
     * @param event Accessibility event for content change
     */
    private suspend fun trackContentChanges(event: AccessibilityEvent) {
        // Check if learning is enabled (user setting + battery level)
        if (!isInteractionLearningEnabled()) {
            return
        }

        try {
            val node = event.source ?: return

            // Extract package info for fingerprinting (with caching for performance)
            val nodePkgName = node.packageName?.toString() ?: "unknown"
            val (packageName, versionCode) = getPackageInfoCached(nodePkgName)
            val appVersion = versionCode.toString()

            val elementHash = AccessibilityFingerprint.fromNode(node, packageName, appVersion).generateHash()

            // Track visibility for interaction timing
            if (node.isVisibleToUser && !elementVisibilityTracker.containsKey(elementHash)) {
                elementVisibilityTracker[elementHash] = System.currentTimeMillis()
            }

            // Check for state changes
            val screenHash = lastScrapedScreenHash
            if (screenHash != null) {
                // Track checked state
                trackStateIfChanged(elementHash, screenHash,
                    com.augmentalis.voiceoscore.scraping.entities.StateType.CHECKED,
                    node.isChecked.toString(), event)

                // Track enabled state
                trackStateIfChanged(elementHash, screenHash,
                    com.augmentalis.voiceoscore.scraping.entities.StateType.ENABLED,
                    node.isEnabled.toString(), event)

                // Track visible state
                trackStateIfChanged(elementHash, screenHash,
                    com.augmentalis.voiceoscore.scraping.entities.StateType.VISIBLE,
                    node.isVisibleToUser.toString(), event)
            }

            node.recycle()
        } catch (e: Exception) {
            Log.e(TAG, "Error tracking content changes", e)
        }
    }

    /**
     * Track state if it has changed from previous value
     *
     * YOLO Phase 2 - Issue #20: Retry queue for failed state changes
     * Verifies element and screen exist in database before inserting state change
     * to prevent FK constraint violations. If they don't exist, queues for retry.
     */
    private suspend fun trackStateIfChanged(
        elementHash: String,
        screenHash: String,
        stateType: String,
        newValue: String,
        event: AccessibilityEvent
    ) {
        // Verify element exists in database (FK constraint requirement)
        val elementExists = database.databaseManager.scrapedElements.getByHash(elementHash) != null
        if (!elementExists) {
            // Element not scraped yet - queue state change for retry
            queueStateChangeForRetry(elementHash, screenHash, stateType, null, newValue, event)
            return
        }

        // Verify screen exists in database (FK constraint requirement)
        val screenExists = database.databaseManager.screenContexts.getByHash(screenHash) != null
        if (!screenExists) {
            // Screen not scraped yet - queue state change for retry
            queueStateChangeForRetry(elementHash, screenHash, stateType, null, newValue, event)
            return
        }

        val previousStates = elementStateTracker.getOrPut(elementHash) { mutableMapOf() }
        val oldValue = previousStates[stateType]

        if (oldValue != newValue) {
            val stateChange = com.augmentalis.voiceoscore.scraping.entities.ElementStateHistoryEntity(
                elementHash = elementHash,
                screenHash = screenHash,
                stateType = stateType,
                oldValue = oldValue,
                newValue = newValue,
                triggeredBy = determineTrigerSource(event)
            )

            // YOLO FIX: Use insertOrIgnore to prevent FK constraint violations
            val insertedId = database.insertElementStateHistory(stateChange)
            if (insertedId > 0) {
                previousStates[stateType] = newValue
            }
        }
    }

    /**
     * YOLO Phase 2 - Issue #20: Queue state change for retry
     *
     * When element/screen doesn't exist yet, queue the state change for retry
     * on the next scrape cycle. This prevents losing state changes due to timing.
     */
    private fun queueStateChangeForRetry(
        elementHash: String,
        screenHash: String,
        stateType: String,
        oldValue: String?,
        newValue: String?,
        event: AccessibilityEvent
    ) {
        val pendingChange = PendingStateChange(
            elementHash = elementHash,
            screenHash = screenHash,
            stateType = stateType,
            oldValue = oldValue,
            newValue = newValue,
            triggeredBy = determineTrigerSource(event),
            attemptCount = 0
        )

        stateChangeRetryQueue.offer(pendingChange)
        Log.v(TAG, "Queued state change for retry: $stateType on element $elementHash (queue size: ${stateChangeRetryQueue.size})")

        // Clean up old entries if queue gets too large
        if (stateChangeRetryQueue.size > RETRY_CLEANUP_THRESHOLD) {
            cleanupRetryQueue()
        }
    }

    /**
     * YOLO Phase 2 - Issue #20: Process retry queue
     *
     * Attempts to process all pending state changes in the retry queue.
     * Called after each scrape cycle to retry failed state changes.
     */
    private suspend fun processRetryQueue() {
        if (stateChangeRetryQueue.isEmpty()) {
            return
        }

        val queueSize = stateChangeRetryQueue.size
        if (developerSettings.isVerboseLoggingEnabled()) {
            Log.d(TAG, "Processing retry queue: $queueSize pending state changes")
        }

        val toRetry = mutableListOf<PendingStateChange>()
        val toRemove = mutableListOf<PendingStateChange>()

        // Drain queue into lists for processing
        while (true) {
            val pending = stateChangeRetryQueue.poll() ?: break

            // Check if max retries exceeded
            if (pending.attemptCount >= MAX_RETRY_ATTEMPTS) {
                Log.w(TAG, "Max retries exceeded for state change: ${pending.stateType} on ${pending.elementHash}")
                toRemove.add(pending)
                continue
            }

            // Check if element and screen now exist
            val elementExists = database.databaseManager.scrapedElements.getByHash(pending.elementHash) != null
            val screenExists = database.databaseManager.screenContexts.getByHash(pending.screenHash) != null

            if (elementExists && screenExists) {
                // Can now insert the state change
                try {
                    val previousStates = elementStateTracker.getOrPut(pending.elementHash) { mutableMapOf() }
                    val currentOldValue = previousStates[pending.stateType]

                    // Only insert if state actually changed
                    if (currentOldValue != pending.newValue) {
                        val stateChange = com.augmentalis.voiceoscore.scraping.entities.ElementStateHistoryEntity(
                            elementHash = pending.elementHash,
                            screenHash = pending.screenHash,
                            stateType = pending.stateType,
                            oldValue = currentOldValue,
                            newValue = pending.newValue,
                            triggeredBy = pending.triggeredBy
                        )

                        // YOLO FIX: Use insertOrIgnore to prevent FK constraint violations
                        val insertedId = database.insertElementStateHistory(stateChange)
                        if (insertedId > 0) {
                            previousStates[pending.stateType] = pending.newValue
                            if (developerSettings.isVerboseLoggingEnabled()) {
                                Log.d(TAG, "Successfully retried state change: ${pending.stateType} on ${pending.elementHash}")
                            }
                        } else {
                            if (developerSettings.isVerboseLoggingEnabled()) {
                                Log.d(TAG, "Failed retry - element/screen still not in DB: ${pending.elementHash}")
                            }
                        }
                    }
                    toRemove.add(pending)
                } catch (e: Exception) {
                    Log.e(TAG, "Error inserting retried state change", e)
                    // Re-queue with incremented attempt count
                    toRetry.add(pending.copy(attemptCount = pending.attemptCount + 1))
                }
            } else {
                // Still can't insert - re-queue with incremented attempt count
                toRetry.add(pending.copy(attemptCount = pending.attemptCount + 1))
            }
        }

        // Re-add items that need more retries
        toRetry.forEach { stateChangeRetryQueue.offer(it) }

        val successCount = toRemove.size
        val requeuedCount = toRetry.size
        if (successCount > 0 || requeuedCount > 0) {
            Log.i(TAG, "Retry queue processed: $successCount succeeded, $requeuedCount requeued")
        }
    }

    /**
     * YOLO Phase 2 - Issue #20: Clean up retry queue
     *
     * Removes old entries that have exceeded max retries or are too old.
     * Prevents unbounded queue growth.
     */
    private fun cleanupRetryQueue() {
        val maxAge = 5 * 60 * 1000L // 5 minutes
        val currentTime = System.currentTimeMillis()
        val toRemove = mutableListOf<PendingStateChange>()

        val iterator = stateChangeRetryQueue.iterator()
        while (iterator.hasNext()) {
            val pending = iterator.next()

            // Remove if max retries exceeded or too old
            if (pending.attemptCount >= MAX_RETRY_ATTEMPTS ||
                (currentTime - pending.timestamp) > maxAge) {
                iterator.remove()
                toRemove.add(pending)
            }
        }

        if (toRemove.isNotEmpty()) {
            Log.i(TAG, "Cleaned up ${toRemove.size} old entries from retry queue")
        }
    }

    /**
     * Determine what triggered a state change
     *
     * @param event Accessibility event
     * @return Trigger source constant
     */
    private fun determineTrigerSource(event: AccessibilityEvent): String {
        return when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_CLICKED,
            AccessibilityEvent.TYPE_VIEW_LONG_CLICKED -> com.augmentalis.voiceoscore.scraping.entities.TriggerSource.USER_CLICK

            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> com.augmentalis.voiceoscore.scraping.entities.TriggerSource.USER_KEYBOARD

            AccessibilityEvent.TYPE_GESTURE_DETECTION_START,
            AccessibilityEvent.TYPE_GESTURE_DETECTION_END -> com.augmentalis.voiceoscore.scraping.entities.TriggerSource.USER_GESTURE

            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> com.augmentalis.voiceoscore.scraping.entities.TriggerSource.SYSTEM

            else -> com.augmentalis.voiceoscore.scraping.entities.TriggerSource.UNKNOWN
        }
    }

    /**
     * Sets recovery mode to suppress scraping during BACK navigation attempts.
     *
     * When LearnApp attempts BACK navigation recovery (after clicking an element that leads
     * to an unexpected screen), it needs to suppress dynamic scraping to avoid capturing
     * launcher or transition screens.
     *
     * ## Usage in ExplorationEngine:
     * ```kotlin
     * try {
     *     accessibilityIntegration.setRecoveryMode(true)
     *     performBackNavigation()
     *     delay(1000)
     *     // ... recovery logic
     * } finally {
     *     accessibilityIntegration.setRecoveryMode(false) // Always re-enable
     * }
     * ```
     *
     * @param enabled true to suppress scraping, false to resume normal scraping
     */
    fun setRecoveryMode(enabled: Boolean) {
        isInRecoveryMode = enabled
        if (enabled) {
            if (developerSettings.isVerboseLoggingEnabled()) {
                Log.d(TAG, "⏸️ Recovery mode ENABLED - scraping suppressed")
            }
        } else {
            if (developerSettings.isVerboseLoggingEnabled()) {
                Log.d(TAG, "▶️ Recovery mode DISABLED - scraping resumed")
            }
        }
    }

    /**
     * Checks if recovery mode is currently active.
     *
     * @return true if scraping is suppressed, false if normal scraping is active
     */
    fun isRecoveryModeActive(): Boolean {
        return isInRecoveryMode
    }
}

/**
 * Result of LearnApp operation
 *
 * @property success Whether the operation completed successfully
 * @property message Human-readable result message
 * @property elementsDiscovered Total number of elements discovered during scan
 * @property newElements Number of new elements inserted (not previously in database)
 * @property updatedElements Number of existing elements updated (already in database)
 */
data class LearnAppResult(
    val success: Boolean,
    val message: String,
    val elementsDiscovered: Int,
    val newElements: Int,
    val updatedElements: Int
)
