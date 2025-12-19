/**
 * ExplorationEngine.kt - Main exploration engine (DFS orchestration)
 * Path: libraries/UUIDCreator/src/main/java/com/augmentalis/learnapp/exploration/ExplorationEngine.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-08
 *
 * Main engine orchestrating DFS exploration of entire app
 */

package com.augmentalis.learnapp.exploration

import android.accessibilityservice.AccessibilityService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.ToneGenerator
import android.media.AudioManager
import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.app.NotificationCompat
import com.augmentalis.learnapp.elements.ElementClassifier
import com.augmentalis.learnapp.fingerprinting.ScreenStateManager
import com.augmentalis.learnapp.models.ExplorationProgress
import com.augmentalis.learnapp.models.ExplorationState
import com.augmentalis.learnapp.models.ExplorationStats
import com.augmentalis.learnapp.navigation.NavigationGraphBuilder
import com.augmentalis.learnapp.scrolling.ScrollDetector
import com.augmentalis.learnapp.scrolling.ScrollExecutor
import com.augmentalis.uuidcreator.UUIDCreator
import com.augmentalis.uuidcreator.alias.UuidAliasManager
import com.augmentalis.uuidcreator.models.UUIDElement
import com.augmentalis.uuidcreator.models.UUIDMetadata
import com.augmentalis.uuidcreator.models.UUIDAccessibility
import com.augmentalis.uuidcreator.thirdparty.ThirdPartyUuidGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Exploration Engine
 *
 * Main engine orchestrating DFS exploration of entire app.
 * Coordinates all components: screen exploration, element classification,
 * UUID generation, navigation graph building, etc.
 *
 * ## Usage Example
 *
 * ```kotlin
 * val engine = ExplorationEngine(...)
 *
 * // Start exploration
 * engine.startExploration("com.instagram.android")
 *
 * // Observe state changes
 * engine.explorationState.collect { state ->
 *     when (state) {
 *         is ExplorationState.Running -> {
 *             println("Progress: ${state.progress}")
 *         }
 *         is ExplorationState.Completed -> {
 *             println("Done! ${state.stats}")
 *         }
 *     }
 * }
 *
 * // Pause/Resume
 * engine.pauseExploration()
 * engine.resumeExploration()
 *
 * // Stop
 * engine.stopExploration()
 * ```
 *
 * @property accessibilityService Accessibility service for UI actions
 * @property uuidCreator UUID creator
 * @property thirdPartyGenerator Third-party UUID generator
 * @property aliasManager Alias manager
 * @property strategy Exploration strategy (default: DFS)
 *
 * @since 1.0.0
 */
class ExplorationEngine(
    private val accessibilityService: AccessibilityService,
    private val uuidCreator: UUIDCreator,
    private val thirdPartyGenerator: ThirdPartyUuidGenerator,
    private val aliasManager: UuidAliasManager,
    private val repository: com.augmentalis.learnapp.database.repository.LearnAppRepository,
    private val strategy: ExplorationStrategy = DFSExplorationStrategy()
) {

    /**
     * Screen state manager
     */
    private val screenStateManager = ScreenStateManager()

    /**
     * Element classifier
     */
    private val elementClassifier = ElementClassifier()

    /**
     * Screen explorer
     */
    private val screenExplorer = ScreenExplorer(
        screenStateManager = screenStateManager,
        elementClassifier = elementClassifier,
        scrollDetector = ScrollDetector(),
        scrollExecutor = ScrollExecutor()
    )

    /**
     * Navigation graph builder
     */
    private lateinit var navigationGraphBuilder: NavigationGraphBuilder

    /**
     * Track generic alias counters per element type
     */
    private val genericAliasCounters = mutableMapOf<String, Int>()

    /**
     * Exploration state flow
     */
    private val _explorationState = MutableStateFlow<ExplorationState>(ExplorationState.Idle)
    val explorationState: StateFlow<ExplorationState> = _explorationState.asStateFlow()

    /**
     * Coroutine scope
     */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * Start timestamp
     */
    private var startTimestamp: Long = 0L

    /**
     * Dangerous elements skipped count
     */
    private var dangerousElementsSkipped = 0

    /**
     * Login screens detected count
     */
    private var loginScreensDetected = 0

    /**
     * Scrollable containers found count
     */
    private var scrollableContainersFound = 0

    /**
     * Current session ID for database persistence
     */
    private var currentSessionId: String? = null

    /**
     * Start exploration
     *
     * Begins DFS exploration of app.
     *
     * @param packageName Package name to explore
     * @param sessionId Session ID for database persistence
     */
    fun startExploration(packageName: String, sessionId: String? = null) {
        this.currentSessionId = sessionId
        scope.launch {
            try {
                // Initialize
                navigationGraphBuilder = NavigationGraphBuilder(packageName)
                screenStateManager.clear()
                startTimestamp = System.currentTimeMillis()
                dangerousElementsSkipped = 0
                loginScreensDetected = 0

                _explorationState.value = ExplorationState.Running(
                    packageName = packageName,
                    progress = ExplorationProgress(
                        appName = packageName,
                        screensExplored = 0,
                        estimatedTotalScreens = 20,
                        elementsDiscovered = 0,
                        currentDepth = 0,
                        currentScreen = "Starting...",
                        elapsedTimeMs = 0L
                    )
                )

                // Get root node
                val rootNode = accessibilityService.rootInActiveWindow
                if (rootNode == null) {
                    _explorationState.value = ExplorationState.Failed(
                        packageName = packageName,
                        error = IllegalStateException("Cannot get root node"),
                        partialProgress = null
                    )
                    return@launch
                }

                // Start DFS exploration
                exploreScreenRecursive(rootNode, packageName, depth = 0)

                // Exploration completed
                val stats = createExplorationStats(packageName)
                _explorationState.value = ExplorationState.Completed(
                    packageName = packageName,
                    stats = stats
                )

            } catch (e: Exception) {
                val currentState = _explorationState.value
                val partialProgress = if (currentState is ExplorationState.Running) {
                    currentState.progress
                } else null

                _explorationState.value = ExplorationState.Failed(
                    packageName = packageName,
                    error = e,
                    partialProgress = partialProgress
                )
            }
        }
    }

    /**
     * Explore screen recursively (DFS)
     *
     * Core DFS algorithm.
     *
     * @param rootNode Root node of current screen
     * @param packageName Package name
     * @param depth Current depth
     */
    private suspend fun exploreScreenRecursive(
        rootNode: AccessibilityNodeInfo,
        packageName: String,
        depth: Int
    ) {
        // PACKAGE NAME VALIDATION: Verify rootNode belongs to target app
        val actualPackageName = rootNode.packageName?.toString()
        if (actualPackageName == null || actualPackageName != packageName) {
            android.util.Log.w("ExplorationEngine",
                "exploreScreenRecursive called with wrong package: $actualPackageName (expected: $packageName). " +
                "Skipping exploration to prevent registering foreign app elements.")
            return
        }

        // Check depth limit
        if (depth > strategy.getMaxDepth()) {
            return
        }

        // Check time limit
        val elapsed = System.currentTimeMillis() - startTimestamp
        if (elapsed > strategy.getMaxExplorationTime()) {
            return
        }

        // Check if paused
        val state = _explorationState.value
        if (state is ExplorationState.PausedByUser) {
            // Wait for resume
            waitForResume()
        }

        // 1. Explore current screen
        val explorationResult = screenExplorer.exploreScreen(rootNode, packageName, depth)

        when (explorationResult) {
            is ScreenExplorationResult.AlreadyVisited -> {
                // Already explored this screen, backtrack
                return
            }

            is ScreenExplorationResult.LoginScreen -> {
                // STEP 1: Mark screen as visited (prevent re-exploration)
                screenStateManager.markAsVisited(explorationResult.screenState.hash)

                // STEP 2: Register ALL elements on login screen (including login fields)
                // This ensures the navigation matrix is complete and voice commands can reference them
                // NOTE: We register element STRUCTURE only - credential values are NOT captured
                val allElementsToRegister = explorationResult.allElements
                val elementUuids = registerElements(
                    elements = allElementsToRegister,
                    packageName = packageName
                )

                android.util.Log.d("ExplorationEngine",
                    "Login screen detected. Registered ${elementUuids.size} elements " +
                    "(${explorationResult.loginElements.size} login fields) before pausing. " +
                    "NOTE: Element structures registered - credential values NOT captured.")

                // STEP 3: Add screen to navigation graph with ALL elements
                navigationGraphBuilder.addScreen(
                    screenState = explorationResult.screenState,
                    elementUuids = elementUuids
                )

                // STEP 4: Persist screen state to database
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        repository.saveScreenState(explorationResult.screenState)
                    } catch (e: Exception) {
                        android.util.Log.e("ExplorationEngine",
                            "Failed to persist login screen state: ${explorationResult.screenState.hash}", e)
                    }
                }

                // STEP 5: Update progress
                updateProgress(packageName, depth, explorationResult.screenState.hash)

                // STEP 6: Notify user to enter credentials (notification + sound)
                notifyUserForLoginScreen(packageName)

                // STEP 7: NOW pause for user login (elements already saved)
                loginScreensDetected++
                _explorationState.value = ExplorationState.PausedForLogin(
                    packageName = packageName,
                    progress = getCurrentProgress(packageName, depth)
                )

                // Wait for user to login (screen change)
                waitForScreenChange(explorationResult.screenState.hash)

                // STEP 8: Resume exploration from new screen after login
                val newRootNode = accessibilityService.rootInActiveWindow ?: return
                exploreScreenRecursive(newRootNode, packageName, depth)
                return
            }

            is ScreenExplorationResult.Error -> {
                // Skip this screen
                return
            }

            is ScreenExplorationResult.Success -> {
                // Mark screen as visited
                screenStateManager.markAsVisited(explorationResult.screenState.hash)

                // Register ALL elements (safe + dangerous + disabled + non-clickable)
                // This ensures complete element inventory for voice commands
                val allElementsToRegister = explorationResult.allElements
                val elementUuids = registerElements(
                    elements = allElementsToRegister,
                    packageName = packageName
                )

                // Calculate element type counts for logging
                val safeCount = explorationResult.safeClickableElements.size
                val dangerousCount = explorationResult.dangerousElements.size
                val otherCount = allElementsToRegister.size - safeCount - dangerousCount

                android.util.Log.d("ExplorationEngine",
                    "Registered ${elementUuids.size} total elements: " +
                    "$safeCount safe clickable, " +
                    "$dangerousCount dangerous (not clicked), " +
                    "$otherCount other (disabled/non-clickable)")

                // Count and log dangerous elements (registered but NOT clicked)
                dangerousElementsSkipped += explorationResult.dangerousElements.size
                explorationResult.dangerousElements.forEach { (element, reason) ->
                    android.util.Log.w("ExplorationEngine",
                        "Registered but NOT clicking dangerous element: '${element.text}' " +
                        "(UUID: ${element.uuid}) - Reason: $reason")
                }

                // Track scrollable containers found
                scrollableContainersFound += explorationResult.scrollableContainersCount

                // Add screen to navigation graph (with ALL elements)
                navigationGraphBuilder.addScreen(
                    screenState = explorationResult.screenState,
                    elementUuids = elementUuids
                )

                // Persist screen state to database
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        repository.saveScreenState(explorationResult.screenState)
                    } catch (e: Exception) {
                        android.util.Log.e("ExplorationEngine",
                            "Failed to persist screen state: ${explorationResult.screenState.hash}", e)
                    }
                }

                // Update progress
                updateProgress(packageName, depth, explorationResult.screenState.hash)

                // Enhanced visual logging - show what we're seeing
                android.util.Log.d("ExplorationEngine-Visual",
                    buildString {
                        appendLine("╔═══════════════════════════════════════════════════════════╗")
                        appendLine("║ SCREEN STATE")
                        appendLine("╠═══════════════════════════════════════════════════════════╣")
                        appendLine("║ Hash: ${explorationResult.screenState.hash.take(16)}...")
                        appendLine("║ Package: ${explorationResult.screenState.packageName}")
                        appendLine("║ Depth: $depth")
                        appendLine("║ Total Elements: ${explorationResult.allElements.size}")
                        appendLine("║ Safe Clickable: ${explorationResult.safeClickableElements.size}")
                        appendLine("║ Dangerous: ${explorationResult.dangerousElements.size}")
                        appendLine("╠═══════════════════════════════════════════════════════════╣")
                        appendLine("║ ELEMENTS DETAIL")
                        appendLine("╠═══════════════════════════════════════════════════════════╣")

                        explorationResult.allElements.take(20).forEachIndexed { i, elem ->
                            val classification = when {
                                explorationResult.safeClickableElements.contains(elem) -> "✓ SAFE"
                                explorationResult.dangerousElements.any { it.first == elem } -> "✗ DANGEROUS"
                                !elem.isClickable -> "○ NON-CLICKABLE"
                                !elem.isEnabled -> "○ DISABLED"
                                else -> "○ OTHER"
                            }

                            appendLine("║")
                            appendLine("║ [$i] $classification")
                            appendLine("║     Type: ${elem.className.substringAfterLast('.')}")
                            appendLine("║     Text: \"${elem.text?.take(30) ?: "(none)"}\"")
                            appendLine("║     ContentDesc: \"${elem.contentDescription?.take(30) ?: "(none)"}\"")
                            appendLine("║     Bounds: ${elem.bounds}")
                            appendLine("║     Clickable: ${elem.isClickable}, Enabled: ${elem.isEnabled}")
                            if (elem.uuid != null) {
                                appendLine("║     UUID: ${elem.uuid?.take(32)}...")
                            }
                        }

                        if (explorationResult.allElements.size > 20) {
                            appendLine("║")
                            appendLine("║ ... and ${explorationResult.allElements.size - 20} more elements")
                        }

                        appendLine("╚═══════════════════════════════════════════════════════════╝")
                    }
                )

                // 2. Order elements by strategy
                val orderedElements = strategy.orderElements(explorationResult.safeClickableElements)

                // 3. Explore each element (DFS)
                // Capture original screen hash for BACK navigation verification
                val originalScreenHash = explorationResult.screenState.hash

                for (element in orderedElements) {
                    // Check if should explore
                    if (!strategy.shouldExplore(element)) {
                        continue
                    }

                    // Log which element we're about to click
                    android.util.Log.d("ExplorationEngine-Visual",
                        ">>> CLICKING ELEMENT: \"${element.text ?: element.contentDescription ?: "unknown"}\" " +
                        "(${element.className.substringAfterLast('.')})")

                    // Click element
                    val clicked = clickElement(element.node)
                    if (!clicked) {
                        continue
                    }

                    // Wait for screen transition
                    delay(1000)

                    // Get new screen
                    val newRootNode = accessibilityService.rootInActiveWindow
                    if (newRootNode == null) {
                        // Backtrack
                        pressBack()
                        delay(1000)
                        continue
                    }

                    // PACKAGE NAME VALIDATION: Check if navigation led to foreign app
                    val actualPackageName = newRootNode.packageName?.toString()
                    if (actualPackageName == null || actualPackageName != packageName) {
                        android.util.Log.w("ExplorationEngine",
                            "Navigation led to different package: $actualPackageName (expected: $packageName). " +
                            "This is likely BACK to launcher or external app. " +
                            "Recording special navigation edge and attempting BACK to recover.")

                        // Record navigation edge with null destination (indicates "exited app")
                        element.uuid?.let { uuid ->
                            navigationGraphBuilder.addEdge(
                                fromScreenHash = explorationResult.screenState.hash,
                                clickedElementUuid = uuid,
                                toScreenHash = "EXTERNAL_APP"  // Special marker
                            )
                        }

                        // Attempt to navigate back to target app
                        var backAttempts = 0
                        val maxBackAttempts = 3
                        var recovered = false

                        while (backAttempts < maxBackAttempts) {
                            pressBack()
                            delay(1000)

                            val currentRootNode = accessibilityService.rootInActiveWindow
                            val currentPackage = currentRootNode?.packageName?.toString()

                            if (currentPackage == packageName) {
                                android.util.Log.d("ExplorationEngine",
                                    "Successfully recovered to $packageName after ${backAttempts + 1} BACK attempts from $actualPackageName")
                                recovered = true
                                break
                            }

                            backAttempts++
                        }

                        if (!recovered) {
                            android.util.Log.e("ExplorationEngine",
                                "Unable to recover to target package $packageName after $maxBackAttempts BACK attempts. " +
                                "Currently at: ${accessibilityService.rootInActiveWindow?.packageName}. " +
                                "Stopping exploration to prevent registering foreign app elements.")
                            break
                        }

                        // Successfully recovered - continue with next element
                        continue
                    }

                    // Package name matches - proceed with normal exploration
                    android.util.Log.d("ExplorationEngine",
                        "Package name validated: $actualPackageName matches target $packageName")

                    // Capture new screen state
                    val newScreenState = screenStateManager.captureScreenState(
                        newRootNode,
                        packageName,
                        depth + 1
                    )

                    // Record navigation edge
                    element.uuid?.let { uuid ->
                        navigationGraphBuilder.addEdge(
                            fromScreenHash = explorationResult.screenState.hash,
                            clickedElementUuid = uuid,
                            toScreenHash = newScreenState.hash
                        )

                        // Persist navigation edge to database
                        currentSessionId?.let { sessionId ->
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                try {
                                    repository.saveNavigationEdge(
                                        packageName = packageName,
                                        sessionId = sessionId,
                                        fromScreenHash = explorationResult.screenState.hash,
                                        clickedElementUuid = uuid,
                                        toScreenHash = newScreenState.hash
                                    )
                                } catch (e: Exception) {
                                    android.util.Log.e("ExplorationEngine",
                                        "Failed to persist navigation edge: $uuid", e)
                                }
                            }
                        }
                    }

                    // Check if screen already visited before recursing
                    if (!screenStateManager.isVisited(newScreenState.hash)) {
                        // Not visited yet - explore it
                        android.util.Log.d("ExplorationEngine",
                            "Exploring new screen: ${newScreenState.hash} (from element: ${element.text})")
                        exploreScreenRecursive(newRootNode, packageName, depth + 1)
                    } else {
                        // Already visited - skip content re-exploration
                        android.util.Log.d("ExplorationEngine",
                            "Screen already explored: ${newScreenState.hash}, " +
                            "skipping re-exploration but recorded navigation edge (element: ${element.text})")
                    }

                    // Backtrack
                    pressBack()
                    delay(1000)

                    // VERIFY BACK navigation (check if we returned to original screen)
                    val currentRootNode = accessibilityService.rootInActiveWindow
                    if (currentRootNode == null) {
                        android.util.Log.w("ExplorationEngine", "Root node null after BACK press")
                        continue
                    }

                    val currentScreenState = screenStateManager.captureScreenState(
                        currentRootNode, packageName, depth
                    )

                    // Check if app is still running
                    if (currentScreenState.packageName != packageName) {
                        android.util.Log.e("ExplorationEngine",
                            "App closed or switched (expected $packageName, got ${currentScreenState.packageName}). Stopping exploration.")
                        break
                    }

                    // Check if screen STRUCTURE is similar (tolerate dynamic content)
                    val isSimilarScreen = screenStateManager.areScreensSimilar(
                        originalScreenHash,
                        currentScreenState.hash,
                        similarityThreshold = 0.85
                    )

                    if (!isSimilarScreen) {
                        android.util.Log.w("ExplorationEngine",
                            "BACK navigation anomaly! Expected similar to $originalScreenHash, " +
                            "got ${currentScreenState.hash} (similarity below 85%). " +
                            "This may indicate navigation to unexpected screen.")

                        // Try ONE more BACK press (not two)
                        android.util.Log.d("ExplorationEngine", "Attempting recovery with single BACK press")
                        pressBack()
                        delay(1000)

                        // Check if we recovered
                        val retryRootNode = accessibilityService.rootInActiveWindow
                        if (retryRootNode != null) {
                            val retryScreenState = screenStateManager.captureScreenState(
                                retryRootNode, packageName, depth
                            )

                            if (retryScreenState.packageName != packageName) {
                                android.util.Log.e("ExplorationEngine", "App closed during recovery. Stopping.")
                                break
                            }

                            val retryIsSimilar = screenStateManager.areScreensSimilar(
                                originalScreenHash,
                                retryScreenState.hash,
                                similarityThreshold = 0.85
                            )

                            if (!retryIsSimilar) {
                                android.util.Log.w("ExplorationEngine",
                                    "Unable to recover original screen structure after retry. " +
                                    "Current: ${retryScreenState.hash}. Continuing exploration from current position.")
                                // DON'T break - just log and continue
                                // The navigation graph will track the actual path taken
                            } else {
                                android.util.Log.d("ExplorationEngine", "Successfully recovered original screen")
                            }
                        }
                    } else {
                        android.util.Log.d("ExplorationEngine", "BACK navigation successful - screen similar to original")
                    }
                }
            }
        }
    }

    /**
     * Register elements with UUIDCreator
     *
     * @param elements List of elements
     * @param packageName Package name
     * @return List of UUIDs
     */
    private suspend fun registerElements(
        elements: List<com.augmentalis.learnapp.models.ElementInfo>,
        packageName: String
    ): List<String> {
        return elements.mapNotNull { element ->
            element.node?.let { node ->
                // Generate UUID
                val uuid = thirdPartyGenerator.generateUuid(node, packageName)

                // Create UUIDElement
                val uuidElement = UUIDElement(
                    uuid = uuid,
                    name = element.getDisplayName(),
                    type = element.extractElementType(),
                    metadata = UUIDMetadata(
                        attributes = mapOf(
                            "thirdPartyApp" to "true",
                            "packageName" to packageName,
                            "className" to element.className,
                            "resourceId" to element.resourceId
                        ),
                        accessibility = UUIDAccessibility(
                            isClickable = element.isClickable,
                            isFocusable = element.isEnabled
                        )
                    )
                )

                // Register with UUIDCreator
                uuidCreator.registerElement(uuidElement)

                // Create alias with error handling and automatic deduplication
                try {
                    val baseAlias = generateAliasFromElement(element)

                    // Check if element has no metadata
                    val hasNoMetadata = (element.text.isNullOrBlank() &&
                                        element.contentDescription.isNullOrBlank() &&
                                        element.resourceId.isNullOrBlank())

                    if (baseAlias.length in 3..50) {
                        // Use setAliasWithDeduplication to handle duplicate aliases automatically
                        // (e.g., "calls" → "calls_2", "calls_3" for RecyclerView items)
                        val actualAlias = aliasManager.setAliasWithDeduplication(uuid, baseAlias)

                        if (actualAlias != baseAlias) {
                            android.util.Log.d("ExplorationEngine",
                                "Deduplicated alias for $uuid: '$baseAlias' → '$actualAlias'")
                        } else {
                            android.util.Log.d("ExplorationEngine", "Added alias for $uuid: $actualAlias")
                        }

                        // Notify user if generic alias was used
                        if (hasNoMetadata) {
                            notifyUserOfGenericAlias(uuid, actualAlias, element)
                        }
                    } else {
                        android.util.Log.w("ExplorationEngine", "Alias invalid for $uuid: '$baseAlias' (${baseAlias.length} chars)")
                    }
                } catch (aliasError: Exception) {
                    android.util.Log.w("ExplorationEngine", "Failed to add alias for $uuid: ${aliasError.message}")
                }

                // Store UUID in element
                element.uuid = uuid

                uuid
            }
        }
    }

    /**
     * Generate alias from element with fallbacks
     *
     * Tries multiple sources to create a valid alias:
     * 1. Element text
     * 2. Content description
     * 3. Resource ID (last component)
     * 4. Fallback: "element_${className}"
     *
     * Sanitizes the result and ensures 3-50 character length.
     *
     * @param element Element to generate alias for
     * @return Generated alias (3-50 characters)
     */
    private fun generateAliasFromElement(element: com.augmentalis.learnapp.models.ElementInfo): String {
        // Try text first
        val textAlias = element.text?.trim()?.takeIf { it.isNotEmpty() }
        if (textAlias != null && textAlias.length >= 3) {
            return sanitizeAlias(textAlias)
        }

        // Try content description
        val contentDesc = element.contentDescription?.trim()?.takeIf { it.isNotEmpty() }
        if (contentDesc != null && contentDesc.length >= 3) {
            return sanitizeAlias(contentDesc)
        }

        // Try resource ID (last component)
        val resourceId = element.resourceId?.substringAfterLast('/')?.trim()?.takeIf { it.isNotEmpty() }
        if (resourceId != null && resourceId.length >= 3) {
            return sanitizeAlias(resourceId)
        }

        // Fallback: use className with number
        val className = element.className.substringAfterLast('.').takeIf { it.isNotEmpty() } ?: "unknown"
        val elementType = className.lowercase().replace("view", "").replace("widget", "")

        // Get next number for this element type
        val counter = genericAliasCounters.getOrPut(elementType) { 0 } + 1
        genericAliasCounters[elementType] = counter

        // Create numbered alias
        val numberedAlias = "${elementType}_$counter"

        android.util.Log.w("ExplorationEngine",
            "Element has no metadata (text/contentDesc/resourceId). " +
            "Assigned generic alias: $numberedAlias for ${element.className}")

        return sanitizeAlias(numberedAlias)
    }

    /**
     * Sanitize alias to valid format
     *
     * AliasManager Requirements:
     * - Must start with a letter (a-z)
     * - Must contain only lowercase alphanumeric + underscores
     * - No hyphens allowed
     * - Length 3-50 characters
     *
     * @param alias Raw alias string
     * @return Sanitized alias meeting all AliasManager requirements
     */
    private fun sanitizeAlias(alias: String): String {
        // 1. Convert to lowercase
        var sanitized = alias.lowercase()

        // 2. Replace invalid characters (including hyphens) with underscores
        sanitized = sanitized.replace(Regex("[^a-z0-9_]"), "_")

        // 3. Collapse multiple underscores
        sanitized = sanitized.replace(Regex("_+"), "_")

        // 4. Remove leading/trailing underscores
        sanitized = sanitized.trim('_')

        // 5. Ensure starts with letter (prepend "elem_" if needed)
        if (sanitized.isEmpty() || !sanitized[0].isLetter()) {
            sanitized = "elem_$sanitized"
        }

        // 6. Ensure minimum 3 characters
        if (sanitized.length < 3) {
            sanitized = sanitized.padEnd(3, 'x')
        }

        // 7. Truncate to 50 characters
        if (sanitized.length > 50) {
            sanitized = sanitized.substring(0, 50)
        }

        // 8. Final validation - ensure still starts with letter after truncation
        if (!sanitized[0].isLetter()) {
            sanitized = "elem" + sanitized.substring(4)
        }

        return sanitized
    }

    /**
     * Click element
     *
     * @param node Node to click
     * @return true if clicked successfully
     */
    private suspend fun clickElement(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false

        return try {
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Press back button
     */
    private suspend fun pressBack() {
        accessibilityService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
    }

    /**
     * Wait for screen change
     *
     * @param previousHash Previous screen hash
     */
    private suspend fun waitForScreenChange(previousHash: String) {
        val timeout = 60000L  // 1 minute
        val startTime = System.currentTimeMillis()

        while (System.currentTimeMillis() - startTime < timeout) {
            val currentHash = screenStateManager.getCurrentScreenHash()
            if (currentHash != null && currentHash != previousHash) {
                return
            }

            delay(500)
        }
    }

    /**
     * Wait for resume (when paused)
     */
    private suspend fun waitForResume() {
        while (_explorationState.value is ExplorationState.PausedByUser) {
            delay(100)
        }
    }

    /**
     * Update progress
     *
     * @param packageName Package name
     * @param depth Current depth
     * @param currentScreenHash Current screen hash
     */
    private suspend fun updateProgress(packageName: String, depth: Int, @Suppress("UNUSED_PARAMETER") currentScreenHash: String) {
        val stats = screenStateManager.getStats()
        val elapsed = System.currentTimeMillis() - startTimestamp

        val progress = ExplorationProgress(
            appName = packageName,
            screensExplored = stats.totalScreensVisited,
            estimatedTotalScreens = maxOf(20, stats.totalScreensDiscovered + 10),
            elementsDiscovered = navigationGraphBuilder.getNodeCount(),
            currentDepth = depth,
            currentScreen = "Screen ${stats.totalScreensVisited}",
            elapsedTimeMs = elapsed
        )

        _explorationState.value = ExplorationState.Running(
            packageName = packageName,
            progress = progress
        )
    }

    /**
     * Get current progress
     *
     * @param packageName Package name
     * @param depth Depth
     * @return Progress
     */
    private suspend fun getCurrentProgress(packageName: String, depth: Int): ExplorationProgress {
        val stats = screenStateManager.getStats()
        val elapsed = System.currentTimeMillis() - startTimestamp

        return ExplorationProgress(
            appName = packageName,
            screensExplored = stats.totalScreensVisited,
            estimatedTotalScreens = maxOf(20, stats.totalScreensDiscovered + 10),
            elementsDiscovered = navigationGraphBuilder.getNodeCount(),
            currentDepth = depth,
            currentScreen = "Screen ${stats.totalScreensVisited}",
            elapsedTimeMs = elapsed
        )
    }

    /**
     * Create exploration stats
     *
     * @param packageName Package name
     * @return Stats
     */
    private suspend fun createExplorationStats(packageName: String): ExplorationStats {
        val stats = screenStateManager.getStats()
        val graph = navigationGraphBuilder.build()
        val graphStats = graph.getStats()
        val elapsed = System.currentTimeMillis() - startTimestamp

        return ExplorationStats(
            packageName = packageName,
            appName = packageName,
            totalScreens = stats.totalScreensDiscovered,
            totalElements = graphStats.totalElements,  // FIXED: Use totalElements instead of totalScreens
            totalEdges = graphStats.totalEdges,
            durationMs = elapsed,
            maxDepth = graphStats.maxDepth,
            dangerousElementsSkipped = dangerousElementsSkipped,
            loginScreensDetected = loginScreensDetected,
            scrollableContainersFound = scrollableContainersFound
        )
    }

    /**
     * Pause exploration
     */
    fun pauseExploration() {
        val currentState = _explorationState.value
        if (currentState is ExplorationState.Running) {
            _explorationState.value = ExplorationState.PausedByUser(
                packageName = currentState.packageName,
                progress = currentState.progress
            )
        }
    }

    /**
     * Resume exploration
     */
    fun resumeExploration() {
        val currentState = _explorationState.value
        if (currentState is ExplorationState.PausedByUser) {
            _explorationState.value = ExplorationState.Running(
                packageName = currentState.packageName,
                progress = currentState.progress
            )
        }
    }

    /**
     * Stop exploration
     *
     * Launches coroutine to create final stats and update state.
     */
    fun stopExploration() {
        val currentState = _explorationState.value
        if (currentState is ExplorationState.Running) {
            scope.launch {
                val stats = createExplorationStats(currentState.packageName)
                _explorationState.value = ExplorationState.Completed(
                    packageName = currentState.packageName,
                    stats = stats
                )
            }
        }
    }

    /**
     * Notify user to enter credentials on login screen
     *
     * Creates a notification and plays a sound to alert the user that
     * manual credential input is required. This is a privacy-preserving
     * approach - we register element structures but DO NOT capture
     * actual passwords or email values entered by the user.
     *
     * @param packageName Package name of the app with login screen
     */
    private fun notifyUserForLoginScreen(packageName: String) {
        try {
            val notificationManager = accessibilityService.getSystemService(Context.NOTIFICATION_SERVICE)
                as? NotificationManager ?: return

            // Create notification channel (Android 8.0+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "LearnApp Exploration",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifications for app exploration events"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 500, 250, 500)
                }
                notificationManager.createNotificationChannel(channel)
            }

            // Create notification
            val notification = NotificationCompat.Builder(accessibilityService, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Login Screen Detected")
                .setContentText("Please enter credentials for $packageName. Exploration will resume after login.")
                .setStyle(NotificationCompat.BigTextStyle()
                    .bigText("LearnApp has detected a login screen in $packageName. " +
                            "Please manually enter your credentials. " +
                            "NOTE: Only element structures are saved - your password and email values are NOT captured. " +
                            "Exploration will automatically resume when the screen changes."))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setVibrate(longArrayOf(0, 500, 250, 500))
                .build()

            notificationManager.notify(LOGIN_NOTIFICATION_ID, notification)

            // Play notification sound
            try {
                val toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
                toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
                scope.launch {
                    delay(200)
                    toneGenerator.release()
                }
            } catch (soundError: Exception) {
                android.util.Log.w("ExplorationEngine",
                    "Failed to play notification sound: ${soundError.message}")
            }

            android.util.Log.i("ExplorationEngine",
                "User notified for login screen: $packageName (notification + sound)")

        } catch (e: Exception) {
            android.util.Log.e("ExplorationEngine",
                "Failed to notify user for login screen: ${e.message}", e)
        }
    }

    /**
     * Notify user that element has no metadata and generic alias was assigned
     *
     * Shows notification allowing user to customize the alias via voice command.
     */
    private fun notifyUserOfGenericAlias(uuid: String, genericAlias: String, element: com.augmentalis.learnapp.models.ElementInfo) {
        try {
            val notificationManager = accessibilityService.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            // Create notification channel if needed
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "LearnApp Exploration",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Notifications about app exploration and element learning"
                    enableVibration(false)
                    setSound(null, null)
                }
                notificationManager.createNotificationChannel(channel)
            }

            val notification = NotificationCompat.Builder(accessibilityService, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Unnamed Element Found")
                .setContentText("${element.className.substringAfterLast('.')} has no label. Voice command: \"$genericAlias\"")
                .setStyle(NotificationCompat.BigTextStyle()
                    .bigText("Element Type: ${element.className.substringAfterLast('.')}\n" +
                            "Assigned Name: \"$genericAlias\"\n" +
                            "Position: ${element.bounds}\n\n" +
                            "You can customize this later in Settings."))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()

            // Use UUID hash as notification ID to avoid duplicates
            notificationManager.notify(uuid.hashCode(), notification)

            android.util.Log.d("ExplorationEngine",
                "Notified user about generic alias: $genericAlias for element at ${element.bounds}")

        } catch (e: Exception) {
            android.util.Log.e("ExplorationEngine", "Failed to send generic alias notification", e)
        }
    }

    /**
     * Cleanup resources
     *
     * Cancels all coroutines. Call when done with exploration.
     */
    fun cleanup() {
        scope.cancel()
    }

    companion object {
        /**
         * Notification channel ID for exploration events
         */
        private const val NOTIFICATION_CHANNEL_ID = "learnapp_exploration"

        /**
         * Notification ID for login screen alerts
         */
        private const val LOGIN_NOTIFICATION_ID = 1001
    }
}
