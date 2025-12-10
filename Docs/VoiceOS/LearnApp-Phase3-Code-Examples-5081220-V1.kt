/**
 * LearnApp Phase 3 - Code Integration Examples
 * Path: Docs/VoiceOS/LearnApp-Phase3-Code-Examples-5081220-V1.kt
 *
 * Ready-to-use code snippets for integrating Phase 3 observability features.
 * Copy and adapt these examples to ExplorationEngine and LearnAppCore.
 *
 * Author: Claude Code (IDEACODE v10.3)
 * Created: 2025-12-08
 * Phase: 3 (Observability)
 */

package com.augmentalis.voiceoscore.learnapp.examples

// ========================================
// EXAMPLE 1: ExplorationEngine Integration
// ========================================

/**
 * Add these imports to ExplorationEngine.kt
 */
import com.augmentalis.voiceoscore.learnapp.metrics.VUIDCreationMetricsCollector
import com.augmentalis.voiceoscore.learnapp.metrics.VUIDCreationDebugOverlay
import com.augmentalis.voiceoscore.learnapp.metrics.createVUIDDebugOverlay
import com.augmentalis.voiceoscore.learnapp.database.repository.VUIDMetricsRepository

/**
 * EXAMPLE 1A: Add private fields to ExplorationEngine class
 */
class ExplorationEngineExample(
    private val context: Context,
    private val databaseManager: VoiceOSDatabaseManager,
    private val developerSettings: LearnAppDeveloperSettings
) {

    // ========== PHASE 3: Metrics & Debug Overlay ==========

    /**
     * Metrics collector for VUID creation tracking
     */
    private val metricsCollector = VUIDCreationMetricsCollector()

    /**
     * Debug overlay for real-time metrics display
     */
    private val debugOverlay: VUIDCreationDebugOverlay by lazy {
        context.createVUIDDebugOverlay().apply {
            setMetricsCollector(metricsCollector)
        }
    }

    /**
     * Repository for persisting metrics to database
     */
    private val metricsRepository = VUIDMetricsRepository(databaseManager)

    /**
     * Current package being explored (for metrics)
     */
    private var currentPackageName: String = ""

    init {
        // Initialize metrics database schema
        coroutineScope.launch {
            metricsRepository.initializeSchema()
        }
    }
}

/**
 * EXAMPLE 1B: Modify startExploration() method
 */
suspend fun startExploration(packageName: String) {
    currentPackageName = packageName

    // Reset metrics for new exploration
    metricsCollector.reset()

    // Show debug overlay if enabled
    if (developerSettings.isDebugOverlayEnabled()) {
        debugOverlay.show(packageName)
        Log.i(TAG, "VUID debug overlay enabled")
    }

    // ... rest of existing startExploration code ...
}

/**
 * EXAMPLE 1C: Modify stopExploration() method
 */
suspend fun stopExploration() {
    // ... existing stop logic ...

    // Hide debug overlay
    if (debugOverlay.isShowing()) {
        debugOverlay.hide()
    }

    // Build final metrics
    val metrics = metricsCollector.buildMetrics(currentPackageName)

    // Save to database
    val rowId = metricsRepository.saveMetrics(metrics)
    Log.i(TAG, "Metrics saved to database (row $rowId)")

    // Generate filter report
    val filterReport = metricsCollector.generateFilterReport()

    // Log comprehensive report
    Log.i(TAG, """
        ========================================
        VUID CREATION FINAL REPORT
        ========================================
        ${metrics.toReportString()}

        ========================================
        FILTER ANALYSIS
        ========================================
        Total Filtered: ${filterReport.totalFiltered}
        Errors (isClickable=true): ${filterReport.errorCount}
        Warnings (suspicious): ${filterReport.warningCount}
        Intended (decorative): ${filterReport.intendedCount}

        ${if (filterReport.errorCount > 0) {
            "⚠️  WARNING: ${filterReport.errorCount} elements with isClickable=true were filtered!"
        } else {
            "✅ No clickable elements filtered (Phase 1 fix working)"
        }}
    """.trimIndent())

    // ... rest of existing stop logic ...
}

// ========================================
// EXAMPLE 2: Element Processing Integration
// ========================================

/**
 * EXAMPLE 2A: Track element detection
 *
 * Insert this in element extraction logic (ScreenExplorer.extractElements)
 */
private fun extractElementsExample(rootNode: AccessibilityNodeInfo): List<ElementInfo> {
    val elements = mutableListOf<ElementInfo>()

    // ... existing extraction logic ...

    // Phase 3: Track each detected element
    elements.forEach { element ->
        metricsCollector.onElementDetected()
    }

    if (developerSettings.isVerboseLoggingEnabled()) {
        Log.d(TAG, "Detected ${elements.size} elements (total: ${metricsCollector.getCurrentStats().first})")
    }

    return elements
}

/**
 * EXAMPLE 2B: Track VUID creation and filtering
 *
 * Insert this in VUID creation logic
 */
private fun createVUIDForElementExample(
    element: AccessibilityNodeInfo,
    packageName: String
): String? {
    // Check if VUID should be created
    val shouldCreate = shouldCreateVUID(element)

    if (shouldCreate) {
        // Create VUID
        val vuid = uuidCreator.generateVUID(element, packageName)

        // Phase 3: Record successful creation
        metricsCollector.onVUIDCreated()

        if (developerSettings.isVerboseLoggingEnabled()) {
            val className = element.className?.toString()?.substringAfterLast('.') ?: "Unknown"
            val label = element.text?.toString() ?: element.contentDescription?.toString() ?: "no-label"
            Log.d(TAG, "✅ VUID created: $vuid ($className: $label)")
        }

        return vuid
    } else {
        // Determine why element was filtered
        val reason = determineFilterReason(element)

        // Phase 3: Record filtering
        metricsCollector.onElementFiltered(element, reason)

        if (developerSettings.isVerboseLoggingEnabled()) {
            val className = element.className?.toString()?.substringAfterLast('.') ?: "Unknown"
            Log.d(TAG, "❌ Filtered: $className - $reason")
        }

        return null
    }
}

/**
 * EXAMPLE 2C: Determine filter reason
 */
private fun determineFilterReason(element: AccessibilityNodeInfo): String {
    return when {
        element.className == null -> "No className"
        isDecorativeElement(element) -> "Decorative element"
        !element.isClickable -> "Not clickable (isClickable=false)"
        element.text.isNullOrBlank() &&
            element.contentDescription.isNullOrBlank() -> "No label (text/contentDescription)"
        (element.text?.length ?: 0) < 2 -> "Label too short"
        else -> "Below threshold"
    }
}

/**
 * EXAMPLE 2D: Check if element is decorative
 */
private fun isDecorativeElement(element: AccessibilityNodeInfo): Boolean {
    val className = element.className?.toString() ?: return false

    // Decorative images (no text/description)
    if (className == "android.widget.ImageView") {
        val hasText = !element.text.isNullOrBlank()
        val hasDescription = !element.contentDescription.isNullOrBlank()
        if (!hasText && !hasDescription) return true
    }

    // Dividers/spacers
    if (className == "android.view.View") {
        val hasText = !element.text.isNullOrBlank()
        if (!hasText && element.childCount == 0) return true
    }

    return false
}

// ========================================
// EXAMPLE 3: LearnAppCore Integration
// ========================================

/**
 * EXAMPLE 3A: Modified LearnAppCore constructor
 */
class LearnAppCoreExample(
    context: Context,
    private val database: VoiceOSDatabaseManager,
    private val uuidGenerator: ThirdPartyUuidGenerator,
    private val metricsCollector: VUIDCreationMetricsCollector? = null  // Phase 3: Optional metrics
) {

    /**
     * EXAMPLE 3B: Modified processElement with metrics
     */
    suspend fun processElement(
        element: ElementInfo,
        packageName: String,
        mode: ProcessingMode
    ): ElementProcessingResult {
        // Phase 3: Record element detection
        metricsCollector?.onElementDetected()

        return try {
            // 1. Generate UUID
            val uuid = generateUUID(element, packageName)

            // 2. Generate voice command
            val command = generateVoiceCommand(element, uuid)

            if (command == null) {
                // Phase 3: Record filtering (no label)
                metricsCollector?.onElementFiltered(
                    element.toAccessibilityNodeInfo(),  // Convert ElementInfo -> AccessibilityNodeInfo
                    "No label found for voice command"
                )

                return ElementProcessingResult(
                    uuid = uuid,
                    command = null,
                    success = false,
                    error = "No label found for command"
                )
            }

            // Phase 3: Record successful VUID creation
            metricsCollector?.onVUIDCreated()

            // 3. Store (mode-specific)
            when (mode) {
                ProcessingMode.IMMEDIATE -> {
                    database.generatedCommands.insert(command)
                }
                ProcessingMode.BATCH -> {
                    batchQueue.add(command)
                }
            }

            ElementProcessingResult(uuid, command, success = true)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to process element", e)
            ElementProcessingResult(
                uuid = "",
                command = null,
                success = false,
                error = e.message
            )
        }
    }

    // ... rest of LearnAppCore ...
}

// ========================================
// EXAMPLE 4: Debug Overlay Controls
// ========================================

/**
 * EXAMPLE 4A: Toggle overlay visibility
 */
fun toggleDebugOverlay() {
    if (debugOverlay.isShowing()) {
        debugOverlay.hide()
        Log.i(TAG, "Debug overlay hidden")
    } else {
        debugOverlay.show(currentPackageName)
        Log.i(TAG, "Debug overlay shown")
    }
}

/**
 * EXAMPLE 4B: Voice command integration
 */
fun processVoiceCommandExample(command: String) {
    when (command.lowercase()) {
        "show debug overlay", "show vuid stats" -> {
            if (!debugOverlay.isShowing()) {
                debugOverlay.show(currentPackageName)
                speakFeedback("Debug overlay enabled")
            }
        }
        "hide debug overlay", "hide vuid stats" -> {
            if (debugOverlay.isShowing()) {
                debugOverlay.hide()
                speakFeedback("Debug overlay disabled")
            }
        }
        "toggle debug overlay" -> {
            toggleDebugOverlay()
            speakFeedback("Debug overlay ${if (debugOverlay.isShowing()) "enabled" else "disabled"}")
        }
    }
}

// ========================================
// EXAMPLE 5: Real-Time Progress Updates
// ========================================

/**
 * EXAMPLE 5A: Update exploration state with metrics
 */
private suspend fun updateExplorationProgressExample() {
    // Get current stats
    val (detected, created, rate) = metricsCollector.getCurrentStats()

    // Update state flow for UI
    _explorationState.value = ExplorationState.Running(
        progress = calculateProgress(),
        stats = ExplorationStats(
            screensExplored = screensExplored.size,
            elementsDetected = detected,
            vuidsCreated = created,
            creationRate = rate,
            navigationEdges = navigationGraph.edgeCount
        )
    )

    // Optional: Log periodic updates
    if (detected % 100 == 0) {  // Every 100 elements
        Log.i(TAG, "Progress: $detected detected, $created created (${(rate * 100).toInt()}%)")
    }
}

/**
 * EXAMPLE 5B: Periodic metrics update (in exploration loop)
 */
private suspend fun exploreScreenDFSExample() {
    val updateInterval = 5000L  // 5 seconds
    var lastUpdate = System.currentTimeMillis()

    while (hasMoreScreens()) {
        // ... explore screen ...

        // Periodic update
        val now = System.currentTimeMillis()
        if (now - lastUpdate > updateInterval) {
            updateExplorationProgressExample()
            lastUpdate = now
        }
    }
}

// ========================================
// EXAMPLE 6: Database Queries
// ========================================

/**
 * EXAMPLE 6A: Query latest metrics
 */
suspend fun getLatestMetricsExample(packageName: String) {
    val metrics = metricsRepository.getLatestMetrics(packageName)

    if (metrics != null) {
        Log.i(TAG, """
            Latest exploration of $packageName:
            - Detected: ${metrics.elementsDetected}
            - Created: ${metrics.vuidsCreated}
            - Rate: ${(metrics.creationRate * 100).toInt()}%
            - Timestamp: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date(metrics.explorationTimestamp))}
        """.trimIndent())
    } else {
        Log.w(TAG, "No metrics found for $packageName")
    }
}

/**
 * EXAMPLE 6B: Query metrics history
 */
suspend fun getMetricsHistoryExample(packageName: String, limit: Int = 10) {
    val history = metricsRepository.getMetricsHistory(packageName, limit)

    Log.i(TAG, "Metrics history for $packageName (last $limit):")
    history.forEachIndexed { index, metrics ->
        val timestamp = SimpleDateFormat("MM-dd HH:mm").format(Date(metrics.explorationTimestamp))
        val rate = (metrics.creationRate * 100).toInt()
        Log.i(TAG, "  ${index + 1}. $timestamp: ${metrics.vuidsCreated}/${metrics.elementsDetected} ($rate%)")
    }
}

/**
 * EXAMPLE 6C: Query aggregate stats
 */
suspend fun getAggregateStatsExample() {
    val stats = metricsRepository.getAggregateStats()

    Log.i(TAG, """
        ========================================
        AGGREGATE STATISTICS (ALL APPS)
        ========================================
        Total Explorations: ${stats.totalExplorations}
        Total Elements: ${stats.totalElements}
        Total VUIDs: ${stats.totalVuids}
        Average Rate: ${(stats.avgCreationRate * 100).toInt()}%
        Min Rate: ${(stats.minCreationRate * 100).toInt()}%
        Max Rate: ${(stats.maxCreationRate * 100).toInt()}%
    """.trimIndent())
}

/**
 * EXAMPLE 6D: Cleanup old metrics
 */
suspend fun cleanupOldMetricsExample(daysToKeep: Int = 30) {
    val deletedCount = metricsRepository.deleteOldMetrics(daysToKeep)
    Log.i(TAG, "Deleted $deletedCount old metric records (older than $daysToKeep days)")
}

// ========================================
// EXAMPLE 7: Settings UI Integration
// ========================================

/**
 * EXAMPLE 7A: Enable/disable overlay via settings
 */
fun updateDebugOverlaySettingExample(enabled: Boolean) {
    developerSettings.setDebugOverlayEnabled(enabled)

    // If currently exploring, update overlay immediately
    if (isExplorationRunning()) {
        if (enabled) {
            debugOverlay.show(currentPackageName)
        } else {
            debugOverlay.hide()
        }
    }

    Log.i(TAG, "Debug overlay ${if (enabled) "enabled" else "disabled"}")
}

/**
 * EXAMPLE 7B: Settings activity switch listener
 */
fun setupDebugOverlaySwitchExample(switch: SwitchCompat) {
    // Load current state
    switch.isChecked = developerSettings.isDebugOverlayEnabled()

    // Listen for changes
    switch.setOnCheckedChangeListener { _, isChecked ->
        updateDebugOverlaySettingExample(isChecked)
    }
}

// ========================================
// EXAMPLE 8: Error Handling
// ========================================

/**
 * EXAMPLE 8A: Handle overlay permission issues
 */
fun showDebugOverlaySafeExample(packageName: String) {
    try {
        debugOverlay.show(packageName)
    } catch (e: SecurityException) {
        Log.e(TAG, "Overlay permission denied", e)
        speakFeedback("Overlay permission required. Please enable in settings.")
    } catch (e: Exception) {
        Log.e(TAG, "Failed to show debug overlay", e)
    }
}

/**
 * EXAMPLE 8B: Handle database errors
 */
suspend fun saveMetricsSafeExample(packageName: String) {
    try {
        val metrics = metricsCollector.buildMetrics(packageName)
        val rowId = metricsRepository.saveMetrics(metrics)

        if (rowId > 0) {
            Log.i(TAG, "Metrics saved successfully (row $rowId)")
        } else {
            Log.w(TAG, "Failed to save metrics (invalid row ID)")
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to save metrics to database", e)
        // Metrics still logged to console, just not persisted
    }
}

/**
 * EXAMPLE 8C: Graceful overlay cleanup
 */
fun cleanupDebugOverlayExample() {
    try {
        if (debugOverlay.isShowing()) {
            debugOverlay.hide()
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to hide debug overlay", e)
        // Ignore - overlay will be cleaned up by window manager
    }
}

// ========================================
// USAGE NOTES
// ========================================

/*
INTEGRATION STEPS:

1. Copy relevant imports to ExplorationEngine.kt
2. Add private fields (metricsCollector, debugOverlay, metricsRepository)
3. Initialize schema in init block
4. Call reset() in startExploration()
5. Call show() in startExploration() if enabled
6. Call onElementDetected() when extracting elements
7. Call onVUIDCreated() when creating VUIDs
8. Call onElementFiltered() when filtering elements
9. Call hide() in stopExploration()
10. Call saveMetrics() in stopExploration()
11. Log final report

TESTING:

# Enable overlay
adb shell am broadcast -a com.augmentalis.voiceos.SET_SETTING \
    --es key debug_overlay_enabled --ez value true

# Run exploration
adb shell am start -n com.augmentalis.voiceos/.learnapp.LearnAppService \
    --es target_package com.ytheekshana.deviceinfo

# Check logs
adb logcat -s ExplorationEngine:I

# Verify database
adb shell "sqlite3 /data/data/com.augmentalis.voiceos/databases/voiceos.db \
    'SELECT * FROM vuid_creation_metrics ORDER BY exploration_timestamp DESC LIMIT 1;'"

EXPECTED RESULTS (DeviceInfo):
- Elements detected: 117
- VUIDs created: 117
- Creation rate: 100%
- Filtered count: 0
- Overlay visible during exploration
- Metrics saved to database

*/
