package com.augmentalis.voiceoscoreng.service

import android.content.pm.PackageManager
import android.util.Log
import com.augmentalis.voiceoscore.CommandGenerator
import com.augmentalis.voiceoscore.CommandRegistry
import com.augmentalis.voiceoscore.ElementFingerprint
import com.augmentalis.voiceoscore.ElementInfo
import com.augmentalis.voiceoscore.QuantizedCommand
import com.augmentalis.voiceoscore.StaticCommandRegistry
import com.augmentalis.voiceoscore.HashUtils
import com.augmentalis.voiceoscore.ICommandPersistence
import com.augmentalis.database.dto.ScrapedAppDTO
import com.augmentalis.database.dto.ScrapedElementDTO
import com.augmentalis.database.repositories.IScrapedAppRepository
import com.augmentalis.database.repositories.IScrapedElementRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "DynamicCommandGen"

/**
 * Generates voice commands for actionable elements.
 *
 * Extracted from VoiceOSAccessibilityService.kt for SOLID compliance.
 * Single Responsibility: Generate and manage voice commands.
 *
 * Static/Dynamic Separation:
 * - Static commands (menus, buttons) are persisted to database
 * - Dynamic commands (list items, emails) are kept in memory only
 */
class DynamicCommandGenerator(
    private val commandRegistry: CommandRegistry,
    private val commandPersistence: ICommandPersistence,
    private val scrapedAppRepository: IScrapedAppRepository,
    private val scrapedElementRepository: IScrapedElementRepository,
    private val scope: CoroutineScope,
    private val getAppInfo: (String) -> AppVersionInfo
) {

    /**
     * App version info holder.
     */
    data class AppVersionInfo(val versionCode: Long, val versionName: String)

    /**
     * Result of command generation containing all generated commands
     * and metadata for UI display and persistence.
     */
    data class CommandGenerationResult(
        val legacyCommands: List<GeneratedCommand>,
        val allQuantizedCommands: List<QuantizedCommand>,
        val staticCommands: List<QuantizedCommand>,
        val dynamicCommands: List<QuantizedCommand>,
        val indexCommands: List<QuantizedCommand>,
        val labelCommands: List<QuantizedCommand>,
        val overlayItems: List<OverlayStateManager.NumberOverlayItem>
    )

    /**
     * Result of incremental command update (for scroll/content changes).
     */
    data class IncrementalUpdateResult(
        val totalCommands: Int,
        val newCommands: Int,
        val preservedCommands: Int,
        val removedCommands: Int
    )

    // =========================================================================
    // In-memory tracking for incremental updates
    // =========================================================================

    /**
     * In-memory cache of element hash -> AVID number assignment.
     * Preserved across scrolls within the same app.
     */
    private val avidAssignments = mutableMapOf<String, Int>()

    /**
     * Next available AVID number for new elements.
     */
    private var nextAvidNumber = 1

    /**
     * Current app package for tracking app changes.
     */
    private var currentAppPackage: String? = null

    /**
     * Generate voice commands for actionable elements using KMP CommandGenerator.
     * Also updates the in-memory CommandRegistry for voice matching.
     *
     * @param elements All extracted UI elements
     * @param hierarchy Element hierarchy information
     * @param elementLabels Pre-derived labels for elements
     * @param packageName App package name
     * @param voiceOSCore Optional: VoiceOS core for updating speech grammar
     * @return CommandGenerationResult with all command types
     */
    fun generateCommands(
        elements: List<ElementInfo>,
        hierarchy: List<HierarchyNode>,
        elementLabels: Map<Int, String>,
        packageName: String,
        updateSpeechEngine: ((List<String>) -> Unit)? = null
    ): CommandGenerationResult {
        // Generate QuantizedCommands with persistence info using KMP CommandGenerator
        val commandResults = elements.mapNotNull { element ->
            CommandGenerator.fromElementWithPersistence(element, packageName)
        }

        // Separate static (persist) and dynamic (memory-only) commands
        val staticCommands = commandResults.filter { it.shouldPersist }
        val dynamicCommands = commandResults.filter { !it.shouldPersist }

        // All commands go to in-memory registry for voice matching
        val allCommands = commandResults.map { it.command }
        commandRegistry.updateSync(allCommands)

        // Also generate index commands for list items ("first", "second", etc.)
        val listItems = elements.filter { it.listIndex >= 0 }
        val indexCommands = CommandGenerator.generateListIndexCommands(listItems, packageName)
        if (indexCommands.isNotEmpty()) {
            commandRegistry.addAll(indexCommands)
        }

        // Generate label-based commands for list items (e.g., "Lifemiles" for email sender)
        val labelCommands = CommandGenerator.generateListLabelCommands(listItems, packageName)
        if (labelCommands.isNotEmpty()) {
            commandRegistry.addAll(labelCommands)
            Log.d(TAG, "Label commands for lists: ${labelCommands.take(5).map { it.phrase }}")
        }

        // Populate numbered overlay items for visual display
        val overlayItems = generateOverlayItems(listItems, elements, packageName)

        // Update overlay state
        OverlayStateManager.updateNumberedOverlayItems(overlayItems)

        // Log the separation
        Log.d(TAG, "Commands: ${allCommands.size} total (${staticCommands.size} static, ${dynamicCommands.size} dynamic)")
        if (dynamicCommands.isNotEmpty()) {
            Log.d(TAG, "Dynamic commands (not persisted): ${dynamicCommands.take(3).map { it.command.phrase.take(30) }}")
        }
        if (indexCommands.isNotEmpty()) {
            Log.d(TAG, "Index commands for lists: ${indexCommands.take(5).map { it.phrase }}")
        }

        // Update speech engine grammar (Vivoka SDK) so it recognizes ALL phrases
        // CRITICAL: Must include static commands EVERY time, as updateCommands() REPLACES the grammar
        // IMPORTANT: Deduplicate phrases to prevent multiple identical commands in grammar
        val staticPhrases = StaticCommandRegistry.allPhrases()
        val dynamicPhrases = allCommands.map { it.phrase } +
            indexCommands.map { it.phrase } +
            labelCommands.map { it.phrase }

        // Deduplicate while preserving order (static commands first)
        val commandPhrases = (staticPhrases + dynamicPhrases).distinct()

        Log.d(TAG, "Updated speech engine with ${commandPhrases.size} command phrases " +
            "(${staticPhrases.size} static, ${allCommands.size} elements, ${indexCommands.size} index, ${labelCommands.size} labels)")

        updateSpeechEngine?.invoke(commandPhrases)

        // Persist STATIC commands to database (async)
        val staticQuantizedCommands = staticCommands.map { it.command }
        if (staticQuantizedCommands.isNotEmpty()) {
            persistStaticCommands(staticQuantizedCommands, elements, packageName, dynamicCommands.size)
        }

        // Generate legacy GeneratedCommand for UI display
        val legacyCommands = generateLegacyCommands(elements, elementLabels, packageName)

        return CommandGenerationResult(
            legacyCommands = legacyCommands,
            allQuantizedCommands = allCommands,
            staticCommands = staticQuantizedCommands,
            dynamicCommands = dynamicCommands.map { it.command },
            indexCommands = indexCommands,
            labelCommands = labelCommands,
            overlayItems = overlayItems
        )
    }

    /**
     * Generate commands incrementally for scroll/content changes.
     * Merges new elements with existing commands, preserving AVID assignments
     * for previously seen elements.
     *
     * Key principle: Elements that were visible before retain their AVID numbers.
     * New elements get new numbers. Memory is cleared only on app change.
     *
     * @param elements Newly extracted UI elements
     * @param hierarchy Element hierarchy information
     * @param elementLabels Pre-derived labels for elements
     * @param packageName App package name
     * @param existingCommands Currently registered commands
     * @param updateSpeechEngine Callback to update speech engine
     * @return IncrementalUpdateResult with counts
     */
    fun generateCommandsIncremental(
        elements: List<ElementInfo>,
        hierarchy: List<HierarchyNode>,
        elementLabels: Map<Int, String>,
        packageName: String,
        existingCommands: List<QuantizedCommand>,
        updateSpeechEngine: ((List<String>) -> Unit)? = null
    ): IncrementalUpdateResult {
        // Reset on app change
        if (packageName != currentAppPackage) {
            avidAssignments.clear()
            nextAvidNumber = 1
            currentAppPackage = packageName
            Log.d(TAG, "App changed to $packageName - reset AVID assignments")
        }

        // Build a map of existing commands by element hash for quick lookup
        val existingByHash = existingCommands.associateBy { it.metadata["elementHash"] }

        // Track results
        var newCount = 0
        var preservedCount = 0
        val mergedCommands = mutableListOf<QuantizedCommand>()

        // Generate commands for current elements
        val commandResults = elements.mapNotNull { element ->
            CommandGenerator.fromElementWithPersistence(element, packageName)
        }

        commandResults.forEach { result ->
            val cmd = result.command
            val hash = cmd.metadata["elementHash"]

            if (hash != null && hash in avidAssignments) {
                // Preserve existing AVID number
                val existingCmd = existingByHash[hash]
                if (existingCmd != null) {
                    mergedCommands.add(existingCmd)
                    preservedCount++
                } else {
                    // Hash known but command not in registry - re-add with same number
                    mergedCommands.add(cmd)
                    preservedCount++
                }
            } else {
                // New element - assign next number
                if (hash != null) {
                    avidAssignments[hash] = nextAvidNumber++
                }
                mergedCommands.add(cmd)
                newCount++
            }
        }

        // Update registry with merged commands
        commandRegistry.updateSync(mergedCommands)

        // Also update list index commands
        val listItems = elements.filter { it.listIndex >= 0 }
        val indexCommands = CommandGenerator.generateListIndexCommands(listItems, packageName)
        if (indexCommands.isNotEmpty()) {
            commandRegistry.addAll(indexCommands)
        }

        // Update label commands
        val labelCommands = CommandGenerator.generateListLabelCommands(listItems, packageName)
        if (labelCommands.isNotEmpty()) {
            commandRegistry.addAll(labelCommands)
        }

        // Update overlay incrementally (preserve positions for existing items)
        val overlayItems = generateOverlayItemsIncremental(listItems, elements, packageName)
        OverlayStateManager.updateNumberedOverlayItemsIncremental(overlayItems)

        // Update speech engine with all phrases
        val staticPhrases = StaticCommandRegistry.allPhrases()
        val dynamicPhrases = mergedCommands.map { it.phrase } +
            indexCommands.map { it.phrase } +
            labelCommands.map { it.phrase }
        val allPhrases = (staticPhrases + dynamicPhrases).distinct()
        updateSpeechEngine?.invoke(allPhrases)

        Log.d(TAG, "Incremental update: ${mergedCommands.size} commands " +
            "($newCount new, $preservedCount preserved)")

        return IncrementalUpdateResult(
            totalCommands = mergedCommands.size + indexCommands.size + labelCommands.size,
            newCommands = newCount,
            preservedCommands = preservedCount,
            removedCommands = existingCommands.size - preservedCount
        )
    }

    /**
     * Generate overlay items incrementally, preserving numbers for existing items.
     */
    private fun generateOverlayItemsIncremental(
        listItems: List<ElementInfo>,
        allElements: List<ElementInfo>,
        packageName: String
    ): List<OverlayStateManager.NumberOverlayItem> {
        val rowElements = ElementExtractor.findTopLevelListItems(listItems, allElements)
            .sortedBy { it.bounds.top }

        return rowElements.mapIndexed { index, element ->
            val label = CommandGenerator.extractShortLabel(element) ?: ""

            val fingerprint = ElementFingerprint.generate(
                className = element.className,
                packageName = packageName,
                resourceId = element.resourceId,
                text = element.text,
                contentDesc = element.contentDescription
            )

            // Use AVID assignment if available, otherwise use sequential index
            val hash = element.hashCode().toString()  // Simple hash for lookup
            val assignedNumber = avidAssignments[hash] ?: (index + 1)

            OverlayStateManager.NumberOverlayItem(
                number = assignedNumber,
                label = label,
                left = element.bounds.left,
                top = element.bounds.top,
                right = element.bounds.right,
                bottom = element.bounds.bottom,
                vuid = fingerprint
            )
        }
    }

    /**
     * Clear all AVID assignments (called on explicit refresh or app change).
     */
    fun clearAvidAssignments() {
        avidAssignments.clear()
        nextAvidNumber = 1
        currentAppPackage = null
        Log.d(TAG, "AVID assignments cleared")
    }

    /**
     * Generate overlay items for numbered badge display.
     */
    private fun generateOverlayItems(
        listItems: List<ElementInfo>,
        allElements: List<ElementInfo>,
        packageName: String
    ): List<OverlayStateManager.NumberOverlayItem> {
        // Find top-level row elements
        val rowElements = ElementExtractor.findTopLevelListItems(listItems, allElements)
            .sortedBy { it.bounds.top }  // Sort by visual position (top to bottom)

        // Assign sequential numbers based on sorted order
        return rowElements.mapIndexed { index, element ->
            val label = CommandGenerator.extractShortLabel(element) ?: ""

            // Use consistent fingerprint generation matching CommandGenerator
            val fingerprint = ElementFingerprint.generate(
                className = element.className,
                packageName = packageName,
                resourceId = element.resourceId,
                text = element.text,
                contentDesc = element.contentDescription
            )

            OverlayStateManager.NumberOverlayItem(
                number = index + 1,  // Sequential 1-based numbering by screen position
                label = label,
                left = element.bounds.left,
                top = element.bounds.top,
                right = element.bounds.right,
                bottom = element.bounds.bottom,
                vuid = fingerprint
            )
        }
    }

    /**
     * Persist static commands to SQLDelight database.
     *
     * FIX (2026-01-19): Resolve FOREIGN KEY constraint failure (code 787)
     * Root cause: Commands were being inserted referencing elementHashes that
     * didn't exist in scraped_element table due to:
     * 1. Silent swallowing of insert failures
     * 2. No verification that elements actually exist before command insert
     *
     * Solution:
     * - Track which elements were SUCCESSFULLY inserted (not just attempted)
     * - Verify element existence before command insert
     * - Only insert commands for elements that actually exist in DB
     */
    private fun persistStaticCommands(
        staticQuantizedCommands: List<QuantizedCommand>,
        elements: List<ElementInfo>,
        packageName: String,
        dynamicCount: Int
    ) {
        scope.launch(Dispatchers.IO) {
            try {
                val currentTime = System.currentTimeMillis()

                // Step 1: Ensure scraped_app exists (FK parent)
                val appInfo = getAppInfo(packageName)
                val scrapedApp = ScrapedAppDTO(
                    appId = packageName,
                    packageName = packageName,
                    versionCode = appInfo.versionCode,
                    versionName = appInfo.versionName,
                    appHash = HashUtils.calculateHash(packageName + appInfo.versionCode).take(8),
                    isFullyLearned = 0,
                    learnCompletedAt = null,
                    scrapingMode = "DYNAMIC",
                    scrapeCount = 1,
                    elementCount = elements.size.toLong(),
                    commandCount = staticQuantizedCommands.size.toLong(),
                    firstScrapedAt = currentTime,
                    lastScrapedAt = currentTime
                )
                scrapedAppRepository.insert(scrapedApp)
                Log.d(TAG, "Step 1/3: Inserted scraped_app for $packageName")

                // Step 2: Insert scraped_elements (FK parent for commands)
                // FIX: Track CONFIRMED existing hashes (either inserted or verified to exist)
                val confirmedHashes = mutableSetOf<String>()
                var insertedCount = 0
                var alreadyExistedCount = 0

                staticQuantizedCommands.forEach { cmd ->
                    val elementHash = cmd.metadata["elementHash"] ?: return@forEach

                    // Skip if already confirmed
                    if (elementHash in confirmedHashes) return@forEach

                    // Find the corresponding element
                    val element = elements.find { el ->
                        val cmdClassName = cmd.metadata["className"] ?: ""
                        val cmdResourceId = cmd.metadata["resourceId"] ?: ""
                        el.className == cmdClassName && el.resourceId == cmdResourceId
                    } ?: elements.firstOrNull { el ->
                        val cmdLabel = cmd.metadata["label"] ?: ""
                        el.text == cmdLabel || el.contentDescription == cmdLabel
                    }

                    val scrapedElement = ScrapedElementDTO(
                        id = 0,
                        elementHash = elementHash,
                        appId = packageName,
                        uuid = null,
                        className = element?.className ?: cmd.metadata["className"] ?: "",
                        viewIdResourceName = element?.resourceId?.ifBlank { null } ?: cmd.metadata["resourceId"]?.ifBlank { null },
                        text = element?.text?.ifBlank { null },
                        contentDescription = element?.contentDescription?.ifBlank { null },
                        bounds = element?.let { "${it.bounds.left},${it.bounds.top},${it.bounds.right},${it.bounds.bottom}" } ?: "0,0,0,0",
                        isClickable = if (element?.isClickable == true) 1L else 0L,
                        isLongClickable = if (element?.isLongClickable == true) 1L else 0L,
                        isEditable = 0L,
                        isScrollable = if (element?.isScrollable == true) 1L else 0L,
                        isCheckable = 0L,
                        isFocusable = 0L,
                        isEnabled = if (element?.isEnabled != false) 1L else 0L,
                        depth = 0L,
                        indexInParent = 0L,
                        scrapedAt = currentTime,
                        semanticRole = null,
                        inputType = null,
                        visualWeight = null,
                        isRequired = null,
                        formGroupId = null,
                        placeholderText = null,
                        validationPattern = null,
                        backgroundColor = null,
                        screen_hash = null
                    )
                    try {
                        scrapedElementRepository.insert(scrapedElement)
                        confirmedHashes.add(elementHash)
                        insertedCount++
                    } catch (e: Exception) {
                        // FIX: Verify element actually exists before assuming it does
                        val existing = scrapedElementRepository.getByHash(elementHash)
                        if (existing != null) {
                            confirmedHashes.add(elementHash)
                            alreadyExistedCount++
                            Log.v(TAG, "Element hash $elementHash already exists in DB")
                        } else {
                            // Element doesn't exist and insert failed - do NOT add to confirmed
                            Log.w(TAG, "Element hash $elementHash insert failed and doesn't exist: ${e.message}")
                        }
                    }
                }
                Log.d(TAG, "Step 2/3: Inserted $insertedCount scraped_elements for ${confirmedHashes.size} unique hashes (static only, $alreadyExistedCount pre-existed)")

                // Step 3: Insert ONLY commands whose elements exist in DB
                // FIX: Filter commands to only those with confirmed element hashes
                val validCommands = staticQuantizedCommands.filter { cmd ->
                    val hash = cmd.metadata["elementHash"]
                    hash != null && hash in confirmedHashes
                }

                val skippedCount = staticQuantizedCommands.size - validCommands.size
                if (skippedCount > 0) {
                    Log.w(TAG, "Step 3/3: Skipping $skippedCount commands with missing element FK references")
                }

                if (validCommands.isNotEmpty()) {
                    commandPersistence.insertBatch(validCommands)
                    Log.d(TAG, "Step 3/3: Persisted ${validCommands.size} STATIC commands to voiceos.db (skipped $dynamicCount dynamic, $skippedCount FK-invalid)")
                } else {
                    Log.w(TAG, "Step 3/3: No valid commands to persist (all had missing FK references)")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Failed to persist commands to database", e)
            }
        }
    }

    /**
     * Generate legacy GeneratedCommand objects for UI display (backwards compatibility).
     */
    private fun generateLegacyCommands(
        elements: List<ElementInfo>,
        elementLabels: Map<Int, String>,
        packageName: String
    ): List<GeneratedCommand> {
        return elements
            .mapIndexedNotNull { index, element ->
                // Only process clickable or scrollable elements
                if (!element.isClickable && !element.isScrollable) return@mapIndexedNotNull null

                // Get the pre-derived label
                val label = elementLabels[index]

                // Skip if label is just the class name (no meaningful content)
                if (label == null || label == element.className.substringAfterLast(".")) {
                    return@mapIndexedNotNull null
                }

                val actionType = when {
                    element.isClickable && element.className.contains("Button") -> "tap"
                    element.isClickable && element.className.contains("EditText") -> "focus"
                    element.isClickable && element.className.contains("ImageView") -> "tap"
                    element.isClickable && element.className.contains("CheckBox") -> "toggle"
                    element.isClickable && element.className.contains("Switch") -> "toggle"
                    element.isClickable -> "tap"
                    element.isScrollable -> "scroll"
                    else -> "interact"
                }

                val fingerprint = ElementFingerprint.generate(
                    className = element.className,
                    packageName = packageName,
                    resourceId = element.resourceId,
                    text = element.text,
                    contentDesc = element.contentDescription
                )

                GeneratedCommand(
                    phrase = "$actionType $label",  // Full voice phrase: "tap Reset"
                    alternates = listOf(
                        "press $label",
                        "select $label",
                        label  // Just the label also works
                    ),
                    targetVuid = fingerprint,
                    action = actionType,
                    element = element,
                    derivedLabel = label
                )
            }
    }

    companion object {
        /**
         * Create AppVersionInfo from PackageManager.
         */
        fun getAppInfoFromPackageManager(
            packageManager: PackageManager,
            packageName: String
        ): AppVersionInfo {
            return try {
                val packageInfo = packageManager.getPackageInfo(packageName, 0)
                AppVersionInfo(
                    versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                        packageInfo.longVersionCode
                    } else {
                        @Suppress("DEPRECATION")
                        packageInfo.versionCode.toLong()
                    },
                    versionName = packageInfo.versionName ?: "unknown"
                )
            } catch (e: PackageManager.NameNotFoundException) {
                Log.w(TAG, "Package not found: $packageName, using defaults")
                AppVersionInfo(versionCode = 0, versionName = "unknown")
            }
        }
    }
}
