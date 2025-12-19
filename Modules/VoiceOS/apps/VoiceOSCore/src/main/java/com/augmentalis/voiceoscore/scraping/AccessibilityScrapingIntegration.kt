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
import com.augmentalis.voiceoscore.scraping.database.AppScrapingDatabase
import com.augmentalis.voiceoscore.scraping.entities.ScrapedAppEntity
import com.augmentalis.voiceoscore.scraping.entities.ScrapedElementEntity
import com.augmentalis.voiceoscore.scraping.entities.ScrapedHierarchyEntity
import com.augmentalis.uuidcreator.UUIDCreator
import com.augmentalis.uuidcreator.alias.UuidAliasManager
import com.augmentalis.uuidcreator.database.UUIDCreatorDatabase
import com.augmentalis.uuidcreator.models.UUIDAccessibility
import com.augmentalis.uuidcreator.models.UUIDElement
import com.augmentalis.uuidcreator.models.UUIDMetadata
import com.augmentalis.uuidcreator.thirdparty.AccessibilityFingerprint
import com.augmentalis.uuidcreator.thirdparty.ThirdPartyUuidGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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
        private const val MAX_DEPTH = 50 // Prevent stack overflow on deeply nested UIs

        // Phase 3: Interaction learning preferences
        private const val PREF_NAME = "voiceos_interaction_learning"
        private const val PREF_INTERACTION_LEARNING_ENABLED = "interaction_learning_enabled"
        private const val MIN_BATTERY_LEVEL_FOR_LEARNING = 20 // Only learn when battery > 20%

        // Packages to exclude from scraping (system apps, launchers)
        private val EXCLUDED_PACKAGES = setOf(
            "com.android.systemui",
            "com.android.launcher",
            "com.android.launcher3",
            "com.google.android.apps.nexuslauncher"
        )
    }

    private val database: AppScrapingDatabase = AppScrapingDatabase.getInstance(context)
    private val packageManager: PackageManager = context.packageManager
    private val commandGenerator: CommandGenerator = CommandGenerator(context)
    private val voiceCommandProcessor: VoiceCommandProcessor = VoiceCommandProcessor(context, accessibilityService)

    // Phase 3: Interaction learning preferences
    private val preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    // UUID Creator components for universal element identification
    private val uuidCreator: UUIDCreator = UUIDCreator.initialize(context)
    private val uuidCreatorDatabase: UUIDCreatorDatabase = UUIDCreatorDatabase.getInstance(context)
    private val thirdPartyGenerator: ThirdPartyUuidGenerator = ThirdPartyUuidGenerator(context)

    private val aliasManager: UuidAliasManager = UuidAliasManager(uuidCreatorDatabase)

    // AI Context Inference (Phase 1 & Phase 2)
    private val semanticInferenceHelper: SemanticInferenceHelper = SemanticInferenceHelper()
    private val screenContextHelper: ScreenContextInferenceHelper = ScreenContextInferenceHelper()

    private val integrationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Track last scraped app to avoid duplicate scraping
    private var lastScrapedAppHash: String? = null

    // Track last screen for transition detection (Phase 2.5)
    private var lastScrapedScreenHash: String? = null
    private var lastScreenTime: Long = 0L

    // Phase 3: Element visibility tracking for interaction recording
    private val elementVisibilityTracker = java.util.concurrent.ConcurrentHashMap<String, Long>()
    private val elementStateTracker = java.util.concurrent.ConcurrentHashMap<String, MutableMap<String, String?>>()

    // Package info cache for performance optimization
    // Caches packageName -> (packageName, versionCode) to avoid repeated PackageManager lookups
    // Invalidated on window state changes (app might have updated)
    private val packageInfoCache = java.util.concurrent.ConcurrentHashMap<String, Pair<String, Int>>()

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
                Log.d(TAG, "Window state changed: ${event.packageName}")
                // Invalidate package info cache on window changes (app might have updated)
                packageInfoCache.clear()
                integrationScope.launch {
                    scrapeCurrentWindow(event)
                }
            }

            // Phase 3: Track user interactions
            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                Log.d(TAG, "View clicked")
                integrationScope.launch {
                    recordInteraction(event, com.augmentalis.voiceoscore.scraping.entities.InteractionType.CLICK)
                }
            }

            AccessibilityEvent.TYPE_VIEW_LONG_CLICKED -> {
                Log.d(TAG, "View long clicked")
                integrationScope.launch {
                    recordInteraction(event, com.augmentalis.voiceoscore.scraping.entities.InteractionType.LONG_PRESS)
                }
            }

            AccessibilityEvent.TYPE_VIEW_FOCUSED -> {
                Log.d(TAG, "View focused")
                integrationScope.launch {
                    recordInteraction(event, com.augmentalis.voiceoscore.scraping.entities.InteractionType.FOCUS)
                }
            }

            AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                Log.d(TAG, "View scrolled")
                integrationScope.launch {
                    recordInteraction(event, com.augmentalis.voiceoscore.scraping.entities.InteractionType.SCROLL)
                }
            }

            // Phase 3: Track state changes
            AccessibilityEvent.TYPE_VIEW_SELECTED -> {
                Log.d(TAG, "View selected")
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
     * @param event Accessibility event triggering the scrape
     * @param filterNonActionable If true, only scrape actionable elements (default: false)
     */
    private suspend fun scrapeCurrentWindow(
        event: AccessibilityEvent,
        filterNonActionable: Boolean = false
    ) {
        try {
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

            // Check if package should be excluded
            if (EXCLUDED_PACKAGES.contains(packageName)) {
                Log.d(TAG, "Skipping excluded package: $packageName")
                rootNode.recycle()
                return
            }

            Log.d(TAG, "Scraping package: $packageName")

            // Get app info
            val appInfo = try {
                packageManager.getPackageInfo(packageName, 0)
            } catch (e: Exception) {
                Log.e(TAG, "Error getting package info", e)
                rootNode.recycle()
                return
            }

            // Calculate app hash
            val appHash = HashUtils.calculateAppHash(packageName, appInfo.versionCode)
            Log.d(TAG, "App hash: $appHash")

            // Check if already scraped
            if (appHash == lastScrapedAppHash) {
                Log.d(TAG, "App already scraped recently, skipping")
                rootNode.recycle()
                return
            }

            // ===== PHASE 1: Hash Deduplication - Check if app exists =====
            val metrics = ScrapingMetrics()
            val scrapeStartTime = System.currentTimeMillis()

            val existingApp = database.scrapedAppDao().getAppByHash(appHash)
            val appId: String
            val isNewApp = existingApp == null

            if (existingApp != null) {
                Log.d(TAG, "App already in database (appId=${existingApp.appId}), using incremental scraping")
                database.scrapedAppDao().incrementScrapeCount(existingApp.appId)
                appId = existingApp.appId
            } else {
                Log.i(TAG, "New app detected, performing full scrape")

                val currentTime = System.currentTimeMillis()

                // Create app entity
                appId = UUID.randomUUID().toString()
                val scrapedApp = ScrapedAppEntity(
                    appId = appId,
                    packageName = packageName,
                    appName = appInfo.applicationInfo.loadLabel(packageManager).toString(),
                    versionCode = appInfo.versionCode,
                    versionName = appInfo.versionName ?: "unknown",
                    appHash = appHash,
                    firstScraped = currentTime,
                    lastScraped = currentTime
                )

                // Insert app
                database.scrapedAppDao().insert(scrapedApp)
                Log.d(TAG, "Inserted app: ${scrapedApp.appName}")
            }

            // ===== PHASE 1: Scrape element tree with hash deduplication =====
            val elements = mutableListOf<ScrapedElementEntity>()
            val hierarchyBuildInfo = mutableListOf<HierarchyBuildInfo>()

            scrapeNode(rootNode, appId, null, 0, 0, elements, hierarchyBuildInfo, filterNonActionable, metrics)

            // Calculate scraping metrics
            metrics.timeMs = System.currentTimeMillis() - scrapeStartTime

            // Log scraping results with deduplication metrics
            if (filterNonActionable) {
                Log.i(TAG, "Filtered scraping: ${elements.size} actionable elements scraped")
            } else {
                Log.i(TAG, "Full scraping: ${elements.size} total elements scraped")
            }
            Log.i(TAG, "${hierarchyBuildInfo.size} hierarchy relationships tracked")
            Log.i(TAG, "📊 METRICS: Found=${metrics.elementsFound}, Cached=${metrics.elementsCached}, " +
                    "Scraped=${metrics.elementsScraped}, Time=${metrics.timeMs}ms")
            if (metrics.elementsFound > 0) {
                val cacheHitRate = (metrics.elementsCached.toFloat() / metrics.elementsFound * 100).toInt()
                Log.i(TAG, "📈 Cache hit rate: $cacheHitRate% (${metrics.elementsCached}/${metrics.elementsFound})")
            }

            // ===== PHASE 2: Insert elements and capture database-assigned IDs =====
            val assignedIds: List<Long> = database.scrapedElementDao().insertBatchWithIds(elements)

            Log.i(TAG, "Inserted ${assignedIds.size} elements, captured database IDs")
            Log.d(TAG, "Sample ID mapping (first 5): ${assignedIds.take(5)}")

            // Validate we got the expected number of IDs
            if (assignedIds.size != elements.size) {
                Log.e(TAG, "ID count mismatch! Expected ${elements.size}, got ${assignedIds.size}")
                throw IllegalStateException("Failed to retrieve all element IDs from database")
            }

            // ===== PHASE 2.5: Register UUIDs with UUIDCreator =====
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
            Log.i(TAG, "Registered $registeredCount UUIDs with UUIDCreator (${elements.size} total elements)")

            // ===== PHASE 3: Build hierarchy entities using real database IDs =====
            val hierarchy = hierarchyBuildInfo.map { buildInfo ->
                // Map list indices to real database IDs
                val childId = assignedIds[buildInfo.childListIndex]
                val parentId = assignedIds[buildInfo.parentListIndex]

                ScrapedHierarchyEntity(
                    parentElementId = parentId,   // ✅ Real database ID
                    childElementId = childId,      // ✅ Real database ID
                    childOrder = buildInfo.childOrder,
                    depth = buildInfo.depth
                )
            }

            Log.d(TAG, "Built ${hierarchy.size} hierarchy entities with valid foreign keys")

            // ===== PHASE 4: Insert hierarchy with valid foreign key references =====
            database.scrapedHierarchyDao().insertBatch(hierarchy)

            // Update element count
            database.scrapedAppDao().updateElementCount(appId, elements.size)

            // Generate commands (need to update elements with real database IDs first)
            Log.d(TAG, "Generating voice commands...")

            // Update elements with real database IDs from assignedIds
            val elementsWithIds = elements.mapIndexed { index, element ->
                element.copy(id = assignedIds[index])
            }

            val commands = commandGenerator.generateCommandsForElements(elementsWithIds)

            // Validation: Ensure all commands have valid element hashes
            require(commands.all { it.elementHash.isNotBlank() }) {
                "All generated commands must have valid element hashes"
            }

            Log.d(TAG, "Generated ${commands.size} commands with valid element hashes")

            // Insert commands
            database.generatedCommandDao().insertBatch(commands)

            // Update command count
            database.scrapedAppDao().updateCommandCount(appId, commands.size)

            // ===== PHASE 5: Create/Update Screen Context (Phase 2) =====
            val screenHash = java.security.MessageDigest.getInstance("MD5")
                .digest("$packageName${event.className}${rootNode.windowId}".toByteArray())
                .joinToString("") { "%02x".format(it) }

            val existingScreenContext = database.screenContextDao().getByScreenHash(screenHash)

            if (existingScreenContext != null) {
                // Update existing screen context
                database.screenContextDao().incrementVisitCount(screenHash, System.currentTimeMillis())
                Log.d(TAG, "Updated screen context (visit count: ${existingScreenContext.visitCount + 1})")
            } else {
                // Create new screen context
                val screenType = screenContextHelper.inferScreenType(
                    windowTitle = rootNode.text?.toString(),
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
                    windowTitle = rootNode.text?.toString()
                )

                val screenContext = com.augmentalis.voiceoscore.scraping.entities.ScreenContextEntity(
                    screenHash = screenHash,
                    appId = appId,
                    packageName = packageName,
                    activityName = event.className?.toString(),
                    windowTitle = rootNode.text?.toString(),
                    screenType = screenType,
                    formContext = formContext,
                    navigationLevel = navigationLevel,
                    primaryAction = primaryAction,
                    elementCount = elements.size,
                    hasBackButton = hasBackButton
                )

                database.screenContextDao().insert(screenContext)
                Log.d(TAG, "Created screen context: type=$screenType, formContext=$formContext, primaryAction=$primaryAction")

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
                        database.scrapedElementDao().updateFormGroupIdBatch(elementHashes, groupId)

                        Log.d(TAG, "Assigned formGroupId '$groupId' to ${formElements.size} form elements")
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
                                    confidence = 0.8f,
                                    inferredBy = "heuristic_proximity"
                                )
                            )
                        }
                    }

                    if (relationships.isNotEmpty()) {
                        database.elementRelationshipDao().insertAll(relationships)
                        Log.d(TAG, "Created ${relationships.size} button→form relationships")
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
                                    confidence = 0.9f,  // High confidence for adjacent elements
                                    inferredBy = "heuristic_sequence"
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
                                        confidence = 0.7f,  // Lower confidence for parent-level labels
                                        inferredBy = "heuristic_parent_container"
                                    )
                                )
                            }
                        }
                    }

                    if (labelRelationships.isNotEmpty()) {
                        database.elementRelationshipDao().insertAll(labelRelationships)
                        Log.d(TAG, "Created ${labelRelationships.size} label→input relationships")
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
                    database.screenTransitionDao().recordTransition(
                        fromHash = lastScrapedScreenHash!!,
                        toHash = screenHash,
                        transitionTime = transitionTime
                    )

                    Log.d(TAG, "Recorded screen transition: ${lastScrapedScreenHash?.take(8)} → ${screenHash.take(8)}" +
                          if (transitionTime != null) " (${transitionTime}ms)" else "")
                }

                // Update last screen tracking
                lastScrapedScreenHash = screenHash
                lastScreenTime = System.currentTimeMillis()
            }

            // Update last scraped
            lastScrapedAppHash = appHash

            Log.i(TAG, "=== Scrape Complete: ${elements.size} elements, ${commands.size} commands ===")

            // Cleanup
            rootNode.recycle()

        } catch (e: Exception) {
            Log.e(TAG, "Error scraping window", e)
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
    private fun scrapeNode(
        node: AccessibilityNodeInfo,
        appId: String,
        parentIndex: Int?,
        depth: Int,
        indexInParent: Int,
        elements: MutableList<ScrapedElementEntity>,
        hierarchyBuildInfo: MutableList<HierarchyBuildInfo>,
        filterNonActionable: Boolean = false,
        metrics: ScrapingMetrics? = null
    ): Int {
        // Prevent stack overflow on pathological UI trees
        if (depth > MAX_DEPTH) {
            Log.w(TAG, "Max depth ($MAX_DEPTH) reached at element count ${elements.size}, stopping traversal")
            return -1
        }

        // Optional filtering: skip non-actionable elements
        if (filterNonActionable && !isActionable(node)) {
            Log.v(TAG, "Skipping non-actionable element at depth $depth: ${node.className}")

            // Still traverse children to find actionable descendants
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                try {
                    scrapeNode(child, appId, parentIndex, depth + 1, i, elements, hierarchyBuildInfo, filterNonActionable, metrics)
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

            // FIX (2025-12-17): Use withContext instead of runBlocking to avoid blocking threads
            val existsInDb = kotlinx.coroutines.withContext(Dispatchers.IO) {
                database.scrapedElementDao().getElementByHash(elementHash) != null
            }
            if (existsInDb) {
                metrics?.elementsCached = (metrics?.elementsCached ?: 0) + 1
                Log.v(TAG, "✓ CACHED (hash=$elementHash): ${node.className}")

                // Element already in database - skip scraping but still traverse children
                for (i in 0 until node.childCount) {
                    val child = node.getChild(i) ?: continue
                    try {
                        scrapeNode(child, appId, parentIndex, depth + 1, i, elements, hierarchyBuildInfo, filterNonActionable, metrics)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error scraping cached element's child", e)
                    } finally {
                        child.recycle()
                    }
                }
                return -1  // Indicate this node was skipped
            }

            // Element is NEW - proceed with scraping
            metrics?.elementsScraped = (metrics?.elementsScraped ?: 0) + 1
            Log.v(TAG, "⊕ SCRAPE (hash=$elementHash): ${node.className}")

            // Log unstable elements (optional - helps with debugging)
            if (stabilityScore < 0.7f) {
                Log.d(TAG, "Unstable element detected (score=$stabilityScore): ${node.className}, text=${node.text}")
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
            )

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
                val metadataValidator = com.augmentalis.voiceoscore.learnapp.validation.MetadataValidator()
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
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                val indent = "  ".repeat(depth)
                Log.d(TAG, "${indent}[${currentIndex}] ${element.className}")
                if (!element.text.isNullOrBlank()) {
                    Log.d(TAG, "${indent}  text: ${element.text}")
                }
                if (!element.contentDescription.isNullOrBlank()) {
                    Log.d(TAG, "${indent}  desc: ${element.contentDescription}")
                }
                if (!element.viewIdResourceName.isNullOrBlank()) {
                    Log.d(TAG, "${indent}  id: ${element.viewIdResourceName}")
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
                    scrapeNode(child, appId, currentIndex, depth + 1, i, elements, hierarchyBuildInfo, filterNonActionable, metrics)
                } catch (e: Exception) {
                    Log.e(TAG, "Error scraping child node", e)
                } finally {
                    child.recycle()
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
        return JSONObject().apply {
            put("left", bounds.left)
            put("top", bounds.top)
            put("right", bounds.right)
            put("bottom", bounds.bottom)
        }.toString()
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
                val appHash = HashUtils.calculateAppHash(packageName, appInfo.versionCode)
                Log.d(TAG, "App hash: $appHash")

                // Get or create app entity
                var app = database.scrapedAppDao().getAppByHash(appHash)
                val appId = if (app != null) {
                    Log.d(TAG, "App exists in database: ${app.appName}")
                    app.appId
                } else {
                    // Create new app entity
                    val newAppId = UUID.randomUUID().toString()
                    val currentTime = System.currentTimeMillis()
                    app = ScrapedAppEntity(
                        appId = newAppId,
                        packageName = packageName,
                        appName = appInfo.applicationInfo.loadLabel(packageManager).toString(),
                        versionCode = appInfo.versionCode,
                        versionName = appInfo.versionName ?: "unknown",
                        appHash = appHash,
                        firstScraped = currentTime,
                        lastScraped = currentTime,
                        scrapingMode = ScrapingMode.LEARN_APP.name
                    )
                    database.scrapedAppDao().insert(app)
                    Log.d(TAG, "Created new app entity: ${app.appName}")
                    newAppId
                }

                // Update mode to LEARN_APP
                database.scrapedAppDao().updateScrapingMode(appId, ScrapingMode.LEARN_APP.name)

                // Get root node
                val rootNode = accessibilityService.rootInActiveWindow
                if (rootNode == null) {
                    Log.e(TAG, "Cannot start LearnApp - no root node")
                    database.scrapedAppDao().updateScrapingMode(appId, ScrapingMode.DYNAMIC.name)
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
                    database.scrapedAppDao().updateScrapingMode(appId, ScrapingMode.DYNAMIC.name)
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
                    val existing = database.scrapedElementDao().getElementByHash(element.elementHash)
                    if (existing != null) {
                        updatedCount++
                    } else {
                        newCount++
                    }
                    database.scrapedElementDao().upsertElement(element)
                }

                Log.i(TAG, "Merge complete: $newCount new, $updatedCount updated")

                // Mark app as fully learned
                val completionTime = System.currentTimeMillis()
                database.scrapedAppDao().markAsFullyLearned(appId, completionTime)
                database.scrapedAppDao().updateElementCount(appId, elements.size)

                // Update scraping mode back to DYNAMIC
                database.scrapedAppDao().updateScrapingMode(appId, ScrapingMode.DYNAMIC.name)

                // Generate commands for new elements
                if (newCount > 0) {
                    Log.d(TAG, "Generating commands for $newCount new elements...")
                    // Get all elements with real database IDs
                    val allElements = database.scrapedElementDao().getElementsByAppId(appId)
                    val commands = commandGenerator.generateCommandsForElements(allElements)
                    database.generatedCommandDao().insertBatch(commands)
                    database.scrapedAppDao().updateCommandCount(appId, commands.size)
                    Log.d(TAG, "Generated ${commands.size} total commands")
                }

                Log.i(TAG, "=== LearnApp Complete: ${elements.size} total, $newCount new, $updatedCount updated ===")

                LearnAppResult(
                    success = true,
                    message = "Successfully learned ${app!!.appName}",
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

            // Create auto-generated alias for easier voice commands
            val aliasBase = (uuidElement.name ?: "element")
                .lowercase()
                .replace(Regex("[^a-z0-9]+"), "-")
                .trim('-')
            aliasManager.setAliasWithDeduplication(uuid, aliasBase)

            Log.d(TAG, "Registered UUID for element: ${element.text} → $uuid")
            uuid
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register UUID for element: ${element.text}", e)
            null
        }
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        // No explicit cleanup needed - coroutine scope will be cancelled by service
        Log.d(TAG, "Scraping integration cleaned up")
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
    private fun getPackageInfoCached(packageName: String): Pair<String, Int> {
        // Try cache first
        packageInfoCache[packageName]?.let { return it }

        // Cache miss - lookup from PackageManager
        return try {
            val info = packageManager.getPackageInfo(packageName, 0)
            val result = Pair(packageName, info.versionCode)
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
            val elementExists = database.scrapedElementDao().getElementByHash(elementHash) != null
            if (!elementExists) {
                Log.v(TAG, "Skipping $interactionType interaction - element not scraped yet: $elementHash")
                node.recycle()
                return
            }

            // Verify screen exists in database (FK: screen_hash -> screen_contexts.screen_hash)
            val screenExists = database.screenContextDao().getScreenByHash(screenHash) != null
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
                visibilityDuration = visibilityDuration,
                success = true  // Assume success unless we detect failure
            )

            // Save to database (safe - both FK parents verified to exist)
            database.userInteractionDao().insert(interaction)
            Log.d(TAG, "Recorded $interactionType interaction for element $elementHash (visibility: ${visibilityDuration}ms)")

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

                database.elementStateHistoryDao().insert(stateChange)
                previousStates[stateType] = newValue
                Log.d(TAG, "Recorded state change: $stateType from $oldValue to $newValue")
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
                trackStateIfChanged(node, elementHash, screenHash,
                    com.augmentalis.voiceoscore.scraping.entities.StateType.CHECKED,
                    node.isChecked.toString(), event)

                // Track enabled state
                trackStateIfChanged(node, elementHash, screenHash,
                    com.augmentalis.voiceoscore.scraping.entities.StateType.ENABLED,
                    node.isEnabled.toString(), event)

                // Track visible state
                trackStateIfChanged(node, elementHash, screenHash,
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
     * Verifies element and screen exist in database before inserting state change
     * to prevent FK constraint violations. Elements must be scraped before their
     * state changes can be tracked.
     */
    private suspend fun trackStateIfChanged(
        node: AccessibilityNodeInfo,
        elementHash: String,
        screenHash: String,
        stateType: String,
        newValue: String,
        event: AccessibilityEvent
    ) {
        // Verify element exists in database (FK constraint requirement)
        val elementExists = database.scrapedElementDao().getElementByHash(elementHash) != null
        if (!elementExists) {
            // Element not scraped yet - skip state tracking
            // State will be captured when element is eventually scraped
            return
        }

        // Verify screen exists in database (FK constraint requirement)
        val screenExists = database.screenContextDao().getScreenByHash(screenHash) != null
        if (!screenExists) {
            // Screen not scraped yet - skip state tracking
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

            database.elementStateHistoryDao().insert(stateChange)
            previousStates[stateType] = newValue
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
