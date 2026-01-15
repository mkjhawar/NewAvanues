package com.augmentalis.voiceoscoreng.service

import android.content.pm.PackageManager
import android.util.Log
import com.augmentalis.voiceoscoreng.common.CommandGenerator
import com.augmentalis.voiceoscoreng.common.CommandRegistry
import com.augmentalis.voiceoscoreng.common.ElementFingerprint
import com.augmentalis.voiceoscoreng.common.ElementInfo
import com.augmentalis.voiceoscoreng.common.QuantizedCommand
import com.augmentalis.voiceoscoreng.common.StaticCommandRegistry
import com.augmentalis.voiceoscoreng.functions.HashUtils
import com.augmentalis.voiceoscoreng.persistence.ICommandPersistence
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
        val staticPhrases = StaticCommandRegistry.allPhrases()
        val dynamicPhrases = allCommands.map { it.phrase } +
            indexCommands.map { it.phrase } +
            labelCommands.map { it.phrase }
        val commandPhrases = staticPhrases + dynamicPhrases

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
                    appHash = HashUtils.generateHash(packageName + appInfo.versionCode, 8),
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
                val insertedHashes = mutableSetOf<String>()
                var insertedCount = 0

                staticQuantizedCommands.forEach { cmd ->
                    val elementHash = cmd.metadata["elementHash"] ?: return@forEach

                    // Skip if already inserted
                    if (elementHash in insertedHashes) return@forEach

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
                        insertedHashes.add(elementHash)
                        insertedCount++
                    } catch (e: Exception) {
                        Log.v(TAG, "Element hash $elementHash may already exist: ${e.message}")
                        insertedHashes.add(elementHash)
                    }
                }
                Log.d(TAG, "Step 2/3: Inserted $insertedCount scraped_elements for ${insertedHashes.size} unique hashes (static only)")

                // Step 3: Insert static commands
                commandPersistence.insertBatch(staticQuantizedCommands)
                Log.d(TAG, "Step 3/3: Persisted ${staticQuantizedCommands.size} STATIC commands to voiceos.db (skipped $dynamicCount dynamic)")

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
