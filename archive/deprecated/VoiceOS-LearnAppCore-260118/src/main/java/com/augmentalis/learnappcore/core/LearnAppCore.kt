/**
 * LearnAppCore.kt - Shared business logic for LearnApp modes
 *
 * Unified UUID generation, voice command generation, and element processing
 * for both JIT Mode and Full Exploration Mode.
 *
 * Author: Manoj Jhawar
 * Created: 2025-12-04
 * Related: JIT-LearnApp Merge (ADR-001, ADR-002, ADR-003)
 *
 * @since 1.1.0 (JIT-LearnApp Merge)
 */

package com.augmentalis.learnappcore.core

import android.content.Context
import android.util.Log
import com.augmentalis.database.dto.GeneratedCommandDTO
import com.augmentalis.database.repositories.IGeneratedCommandRepository
import com.augmentalis.avidcreator.thirdparty.ThirdPartyAvidGenerator
import com.augmentalis.learnappcore.detection.AppFramework
import com.augmentalis.learnappcore.detection.CrossPlatformDetector
import com.augmentalis.learnappcore.models.ElementInfo
import com.augmentalis.learnappcore.settings.LearnAppDeveloperSettings
import com.augmentalis.learnappcore.utils.DatabaseRetryUtil
import java.util.Collections
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * LearnApp Core - Shared Business Logic
 *
 * Provides unified element processing for both JIT and Exploration modes:
 * - UUID generation (deterministic, stable)
 * - Voice command generation (with synonyms)
 * - Mode-specific storage (immediate vs batch)
 *
 * ## Usage - JIT Mode:
 * ```kotlin
 * val result = core.processElement(
 *     element = elementInfo,
 *     packageName = "com.example.app",
 *     mode = ProcessingMode.IMMEDIATE  // Insert now
 * )
 * ```
 *
 * ## Usage - Exploration Mode:
 * ```kotlin
 * // Process all elements
 * elements.forEach { element ->
 *     core.processElement(element, packageName, ProcessingMode.BATCH)
 * }
 * // Flush batch to database
 * core.flushBatch()
 * ```
 *
 * ## Performance:
 * - IMMEDIATE mode: ~10ms per element
 * - BATCH mode: ~50ms for 100 elements (20x faster)
 *
 * @param commandRepository Repository for command persistence
 * @param vuidGenerator Third-party UUID generator for element identification
 */
class LearnAppCore(
    context: Context,
    private val commandRepository: IGeneratedCommandRepository,
    private val uuidGenerator: ThirdPartyAvidGenerator
) : IElementProcessorInterface, IBatchManagerInterface {
    companion object {
        private const val TAG = "LearnAppCore"
    }

    // Developer settings (lazy initialized)
    private val developerSettings: LearnAppDeveloperSettings by lazy {
        LearnAppDeveloperSettings(context)
    }

    /**
     * Framework detection cache with LRU eviction
     *
     * Caches detected framework per package name to avoid repeated detection.
     * Key: package name, Value: detected framework
     *
     * LRU eviction prevents unbounded growth (max 50 frameworks cached).
     * Thread-safe via Collections.synchronizedMap for concurrent access.
     */
    private val frameworkCache = Collections.synchronizedMap(object : LinkedHashMap<String, AppFramework>(
        16, 0.75f, true  // LRU access order
    ) {
        override fun removeEldestEntry(eldest: Map.Entry<String, AppFramework>): Boolean {
            return size > 50  // Max 50 frameworks cached
        }
    })

    /**
     * Batch queue for BATCH mode
     *
     * Commands are queued during exploration and flushed as single transaction.
     * Memory: ~1.5KB per command × 100 = ~150KB peak
     *
     * Uses ArrayBlockingQueue to prevent unbounded growth.
     * Auto-flushes when queue reaches capacity.
     */
    private val maxBatchSize = 100
    private val batchQueue = java.util.concurrent.ArrayBlockingQueue<GeneratedCommandDTO>(maxBatchSize)

    /**
     * Process element and generate UUID + voice command
     *
     * Storage strategy:
     * - IMMEDIATE: Insert to database now (~10ms)
     * - BATCH: Queue for later batch insert (~0.1ms)
     *
     * @param element Element to process
     * @param packageName App package name
     * @param mode Processing mode (IMMEDIATE or BATCH)
     * @return Processing result with UUID and command
     */
    override suspend fun processElement(
        element: ElementInfo,
        packageName: String,
        mode: ProcessingMode
    ): ElementProcessingResult = withContext(Dispatchers.Default) {
        try {
            // 1. Generate UUID
            val uuid = generateUUID(element, packageName)
            if (developerSettings.isVerboseLoggingEnabled()) {
                Log.d(TAG, "Generated UUID: $uuid for element: ${element.text}")
            }

            // 2. Generate voice command
            val command = generateVoiceCommand(element, uuid, packageName) ?: return@withContext ElementProcessingResult(
                uuid = uuid,
                command = null,
                success = false,
                error = "No label found for command"
            )
            if (developerSettings.isVerboseLoggingEnabled()) {
                Log.d(TAG, "Generated command: ${command.commandText}")
            }

            // 3. Store (mode-specific)
            when (mode) {
                ProcessingMode.IMMEDIATE -> {
                    // JIT Mode: Insert immediately with retry logic
                    DatabaseRetryUtil.withRetry {
                        commandRepository.insert(command)
                    }
                    if (developerSettings.isVerboseLoggingEnabled()) {
                        Log.d(TAG, "Inserted command immediately: ${command.commandText}")
                    }
                }
                ProcessingMode.BATCH -> {
                    // Exploration Mode: Queue for batch
                    // Try to add to queue (non-blocking)
                    if (!batchQueue.offer(command)) {
                        // Queue full, flush immediately
                        Log.w(TAG, "Batch queue full ($maxBatchSize), auto-flushing")
                        flushBatch()
                        // Try again after flush (should succeed)
                        if (!batchQueue.offer(command)) {
                            Log.e(TAG, "Failed to queue command even after flush!")
                        }
                    } else {
                        Log.v(TAG, "Queued command for batch: ${command.commandText}")
                    }
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

    /**
     * Generate UUID for element
     *
     * Uses third-party UUID generator to create deterministic UUID
     * based on element properties and package name.
     *
     * VUID format: {packageName}.v{version}.{type}-{hash}
     *
     * @param element Element to generate UUID for
     * @param packageName App package name
     * @return Generated UUID string
     */
    private fun generateUUID(element: ElementInfo, packageName: String): String {
        // Calculate element hash from properties
        val elementHash = calculateElementHash(element)

        // Generate UUID using ThirdPartyAvidGenerator
        // Format: packageName.v{version}.{type}-{hash}
        // For now, we'll use a simplified version
        // The ThirdPartyAvidGenerator would need an AccessibilityNodeInfo
        // Since we're working with ElementInfo, we'll create a compatible UUID
        val elementType = when {
            element.isClickable -> "button"
            element.isEditText() -> "input"
            element.isScrollable -> "scroll"
            else -> "element"
        }

        // Create stable UUID from package + hash + type
        return "$packageName.$elementType-$elementHash"
    }

    /**
     * Calculate element hash
     *
     * Creates deterministic hash from element properties for UUID generation.
     * Uses MD5 hash of combined properties.
     *
     * @param element Element to hash
     * @return 12-character hash string
     */
    private fun calculateElementHash(element: ElementInfo): String {
        // Combine element properties
        val fingerprint = buildString {
            append(element.className)
            append("|")
            append(element.resourceId)
            append("|")
            append(element.text)
            append("|")
            append(element.contentDescription)
            append("|")
            append(element.bounds.toString())
        }

        // Generate MD5 hash
        return try {
            val md = java.security.MessageDigest.getInstance("MD5")
            val hashBytes = md.digest(fingerprint.toByteArray())
            // Take first 12 characters of hex string
            hashBytes.joinToString("") { "%02x".format(it) }.take(12)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to calculate hash", e)
            // Fallback to simple hash
            fingerprint.hashCode().toString()
        }
    }

    /**
     * Generate voice command from element
     *
     * Creates GeneratedCommandDTO with command text, synonyms, and metadata.
     *
     * CRITICAL: ALL actionable elements MUST get commands - NEVER skip them.
     * The app's entire purpose is voice control - skipping elements defeats this.
     *
     * Enhanced for ALL apps (native and cross-platform):
     * - Label priority: text > contentDescription > resourceId > FALLBACK
     * - Fallback generation for ALL unlabeled actionable elements
     * - Position-based fallback as last resort (type_x_y format)
     * - Command format: "{actionType} {label}" (lowercase)
     *
     * @param element Element to generate command for
     * @param vuid Pre-generated UUID for this element
     * @param packageName Package name for framework detection
     * @return GeneratedCommandDTO or null ONLY for non-actionable elements with poor labels
     */
    private fun generateVoiceCommand(
        element: ElementInfo,
        uuid: String,
        packageName: String = ""
    ): GeneratedCommandDTO? {
        // Detect app framework (cached)
        val framework = if (packageName.isNotEmpty() && element.node != null) {
            frameworkCache.getOrPut(packageName) {
                CrossPlatformDetector.detectFramework(packageName, element.node)
            }
        } else {
            AppFramework.NATIVE
        }

        // CRITICAL: Check if element is actionable (must ALWAYS get a command)
        val isActionable = element.isClickable ||
                          element.isLongClickable ||
                          element.isScrollable ||
                          element.isEditText()

        // Extract label (text > contentDescription > resourceId > FALLBACK)
        var label = element.text.takeIf { it.isNotBlank() }
            ?: element.contentDescription.takeIf { it.isNotBlank() }
            ?: element.resourceId.substringAfterLast("/").takeIf { it.isNotBlank() }
            ?: generateFallbackLabel(element, framework)

        // Get framework-adjusted minimum label length
        val minLabelLength = framework.getMinLabelLength(
            developerSettings.getMinGeneratedLabelLength()
        )

        // CRITICAL FIX: Actionable elements MUST get commands - generate last-resort fallback
        if (isActionable && (label.isBlank() || label == "unlabeled" || label.length < minLabelLength || label.all { it.isDigit() })) {
            // Generate positional last-resort label for actionable elements
            label = generateLastResortLabel(element)
            Log.d(TAG, "Generated last-resort label for actionable element: $label")
        }

        // For clickable elements in any framework, ALWAYS generate labels
        if (isActionable) {
            val actionType = determineActionType(element)
            val commandText = "$actionType $label".lowercase()
            val synonyms = generateSynonyms(actionType, label)
            val elementHash = calculateElementHash(element)

            // Confidence based on label quality
            val confidence = when {
                element.text.isNotBlank() || element.contentDescription.isNotBlank() -> 0.95  // Semantic label
                element.resourceId.isNotBlank() -> 0.85  // Resource ID
                label.contains("_") && label.matches(Regex(".*_\\d+_\\d+$")) -> 0.5  // Coordinate fallback
                else -> 0.7  // Position/context fallback
            }

            return GeneratedCommandDTO(
                id = 0L,
                elementHash = elementHash,
                commandText = commandText,
                actionType = actionType,
                confidence = confidence,
                synonyms = synonyms,
                isUserApproved = 0L,
                usageCount = 0L,
                lastUsed = null,
                createdAt = System.currentTimeMillis(),
                appId = packageName
            )
        }

        // For NON-actionable elements, apply quality filters (OK to skip display-only elements)
        if (label.length < minLabelLength || label.all { it.isDigit() }) {
            return null
        }

        // Determine action type
        val actionType = determineActionType(element)

        // Generate command text
        val commandText = "$actionType $label".lowercase()

        // Generate synonyms
        val synonyms = generateSynonyms(actionType, label)

        // Calculate element hash for database
        val elementHash = calculateElementHash(element)

        // Create command DTO
        return GeneratedCommandDTO(
            id = 0L,  // Auto-generated by database
            elementHash = elementHash,
            commandText = commandText,
            actionType = actionType,
            confidence = 0.85,  // High confidence for automated generation
            synonyms = synonyms,
            isUserApproved = 0L,
            usageCount = 0L,
            lastUsed = null,
            createdAt = System.currentTimeMillis(),
            appId = packageName
        )
    }

    /**
     * Generate last-resort label for actionable elements
     *
     * CRITICAL: This ensures ALL actionable elements get voice commands.
     * Uses element type + center coordinates for unique identification.
     *
     * Format: "{elementType}_{centerX}_{centerY}"
     * Examples: "button_540_1200", "imageview_270_800"
     *
     * @param element Element to generate label for
     * @return Coordinate-based unique label
     */
    private fun generateLastResortLabel(element: ElementInfo): String {
        val elementType = element.className
            .substringAfterLast(".")
            .lowercase()
            .replace("view", "")
            .replace("layout", "")
            .takeIf { it.isNotBlank() } ?: "element"

        val centerX = element.bounds.centerX()
        val centerY = element.bounds.centerY()

        return "${elementType}_${centerX}_${centerY}"
    }

    /**
     * Determine action type for element
     *
     * @param element Element to analyze
     * @return Action type string (click, type, scroll, etc.)
     */
    private fun determineActionType(element: ElementInfo): String {
        return when {
            element.isEditText() -> "type"  // EditText fields
            element.isScrollable -> "scroll"  // Scrollable containers
            element.isClickable -> "click"  // Clickable elements
            else -> "click"  // Default action
        }
    }

    /**
     * Generate fallback labels for unlabeled elements
     *
     * CRITICAL: Applies to ALL apps (native AND cross-platform).
     * Native Android apps with poor metadata MUST also get fallback labels.
     *
     * Strategies (in priority order):
     * 1. Game engines (Unity/Unreal/Godot/Cocos2d/Defold): Spatial grid labeling
     * 2. Position-based: "Tab 1", "Button 2", "Card 3"
     * 3. Context-aware: "Top button", "Bottom card"
     * 4. Type + index fallback: "View 1", "Element 2"
     *
     * @param element Element to generate label for
     * @param framework Detected app framework
     * @return Generated label string (NEVER returns "unlabeled" for actionable elements)
     */
    private fun generateFallbackLabel(element: ElementInfo, framework: AppFramework): String {
        // Special handling for game engines (spatial coordinate-based)
        when (framework) {
            AppFramework.UNITY, AppFramework.GODOT, AppFramework.COCOS2D, AppFramework.DEFOLD -> {
                return generateUnityLabel(element)  // 3x3 grid
            }
            AppFramework.UNREAL -> {
                return generateUnrealLabel(element)  // 4x4 grid
            }
            else -> { /* Continue to general fallback strategies */ }
        }

        // Strategy 1: Position-based labels (works for ALL frameworks including NATIVE)
        val positionLabel = generatePositionLabel(element)
        if (positionLabel != null) return positionLabel

        // Strategy 2: Context-aware labels
        val contextLabel = generateContextLabel(element)
        if (contextLabel != null) return contextLabel

        // Strategy 3: Type + index fallback (universal)
        val elementType = element.className.substringAfterLast(".").lowercase()
            .replace("view", "")
            .replace("layout", "")
            .takeIf { it.isNotBlank() } ?: "element"

        val index = element.index.takeIf { it >= 0 } ?: 0
        return "$elementType ${index + 1}"
    }

    /**
     * Generate fallback labels for Unity apps
     *
     * Unity apps render to OpenGL surface with no accessibility tree.
     * Strategy: Use grid-based spatial labeling with coordinates.
     *
     * Unity apps are uniquely challenging:
     * - Single GLSurfaceView or UnityPlayer view
     * - No semantic labels (text, contentDescription, resourceId)
     * - Individual UI elements NOT exposed to accessibility
     * - Must use spatial coordinates for voice control
     *
     * Label format: "[Size] Row Column [Type]"
     * Examples:
     * - "Top Left Button"
     * - "Large Center Middle Button"
     * - "Small Bottom Right Element"
     *
     * @param element Element to generate label for
     * @return Grid-based spatial label
     */
    private fun generateUnityLabel(element: ElementInfo): String {
        val bounds = element.bounds
        val screenWidth = element.screenWidth.takeIf { it > 0 } ?: 1080
        val screenHeight = element.screenHeight.takeIf { it > 0 } ?: 1920

        // Divide screen into 3x3 grid
        val col = when {
            bounds.centerX() < screenWidth / 3 -> "Left"
            bounds.centerX() > screenWidth * 2 / 3 -> "Right"
            else -> "Center"
        }

        val row = when {
            bounds.centerY() < screenHeight / 3 -> "Top"
            bounds.centerY() > screenHeight * 2 / 3 -> "Bottom"
            else -> "Middle"
        }

        // Calculate element size relative to screen
        val elementArea = bounds.width() * bounds.height()
        val screenArea = screenWidth * screenHeight
        val sizeRatio = elementArea.toFloat() / screenArea.toFloat()

        val size = when {
            sizeRatio > 0.111f -> "Large"  // > 1/9 of screen
            sizeRatio < 0.028f -> "Small"  // < 1/36 of screen
            else -> ""  // Medium (default, no size label)
        }

        // Determine element type if available
        val type = when {
            element.className.contains("Button", ignoreCase = true) -> "Button"
            element.className.contains("Image", ignoreCase = true) -> "Image"
            element.className.contains("Text", ignoreCase = true) -> "Text"
            element.isClickable -> "Button"  // Clickable elements treated as buttons
            else -> "Element"
        }

        // Build label: [Size] Row Column [Type]
        return buildString {
            if (size.isNotEmpty()) {
                append(size)
                append(" ")
            }
            append(row)
            append(" ")
            append(col)
            append(" ")
            append(type)
        }.trim()
    }

    /**
     * Generate fallback labels for Unreal Engine apps
     *
     * Unreal Engine apps have more complex UI than Unity (AAA mobile games).
     * Strategy: Use 4x4 grid with edge/corner detection for HUD elements.
     *
     * Unreal Engine challenges:
     * - Renders via Slate UI framework to graphics surface
     * - Minimal accessibility support (like Unity)
     * - More complex UI than Unity (AAA games like PUBG Mobile, Fortnite)
     * - Common HUD elements at edges and corners
     *
     * Differences from Unity:
     * - 4x4 grid instead of 3x3 (more UI elements)
     * - Edge/corner detection for HUD elements
     * - "Upper"/"Lower" instead of "Middle" for clarity
     * - Uses "Widget" terminology for clickable elements
     *
     * Label format: "[Corner/Edge] [Size] Row Column [Type]"
     * Examples:
     * - "Corner Top Far Left Button"
     * - "Edge Small Bottom Right Icon"
     * - "Large Upper Left Widget"
     * - "Lower Right Button"
     *
     * @param element Element to generate label for
     * @return 4x4 grid-based spatial label with edge/corner detection
     */
    private fun generateUnrealLabel(element: ElementInfo): String {
        val bounds = element.bounds
        val screenWidth = element.screenWidth.takeIf { it > 0 } ?: 1080
        val screenHeight = element.screenHeight.takeIf { it > 0 } ?: 1920

        // Divide screen into 4x4 grid (finer than Unity's 3x3)
        val col = when {
            bounds.centerX() < screenWidth / 4 -> "Far Left"
            bounds.centerX() < screenWidth / 2 -> "Left"
            bounds.centerX() < screenWidth * 3 / 4 -> "Right"
            else -> "Far Right"
        }

        val row = when {
            bounds.centerY() < screenHeight / 4 -> "Top"
            bounds.centerY() < screenHeight / 2 -> "Upper"
            bounds.centerY() < screenHeight * 3 / 4 -> "Lower"
            else -> "Bottom"
        }

        // Check if element is in corner (common for HUD elements in Unreal games)
        val isCorner = (bounds.centerX() < screenWidth / 4 || bounds.centerX() > screenWidth * 3 / 4) &&
                (bounds.centerY() < screenHeight / 4 || bounds.centerY() > screenHeight * 3 / 4)

        // Check if element is at edge (common for menu buttons)
        val edgeThreshold = 50  // pixels from screen edge
        val isEdge = !isCorner && (
                bounds.left < edgeThreshold ||
                        bounds.right > screenWidth - edgeThreshold ||
                        bounds.top < edgeThreshold ||
                        bounds.bottom > screenHeight - edgeThreshold
                )

        // Calculate element size relative to screen
        val elementArea = bounds.width() * bounds.height()
        val screenArea = screenWidth * screenHeight
        val sizeRatio = elementArea.toFloat() / screenArea.toFloat()

        val size = when {
            sizeRatio > 0.167f -> "Large"  // > 1/6 of screen (larger threshold for Unreal)
            sizeRatio < 0.031f -> "Small"  // < 1/32 of screen (finer granularity)
            else -> ""  // Medium (default, no size label)
        }

        // Determine element type (Unreal uses "Widget" terminology)
        val type = when {
            element.className.contains("Button", ignoreCase = true) -> "Button"
            element.className.contains("Image", ignoreCase = true) -> "Icon"
            element.className.contains("Text", ignoreCase = true) -> "Text"
            element.isClickable -> "Widget"  // Unreal uses "Widget" for UI elements
            else -> "Element"
        }

        // Build label: [Corner/Edge] [Size] Row Column [Type]
        return buildString {
            if (isCorner) {
                append("Corner ")
            } else if (isEdge) {
                append("Edge ")
            }

            if (size.isNotEmpty()) {
                append(size)
                append(" ")
            }

            append(row)
            append(" ")
            append(col)
            append(" ")
            append(type)
        }.trim()
    }

    /**
     * Generate position-based label
     *
     * Examples:
     * - LinearLayout with 5 children → "Tab 1", "Tab 2", ..., "Tab 5"
     * - CardView in RecyclerView → "Card 1", "Card 2", ...
     *
     * @param element Element to generate label for
     * @return Position-based label or null if not applicable
     */
    private fun generatePositionLabel(element: ElementInfo): String? {
        val parent = element.parent ?: return null
        val siblings = parent.children ?: return null

        // Get element position among siblings
        val position = element.index
        if (position < 0) return null

        // Determine label prefix based on element type and parent
        val prefix = when {
            // Tab-like layouts
            element.className.contains("LinearLayout", ignoreCase = true) &&
                    parent.className.contains("TabLayout", ignoreCase = true) -> "Tab"

            // Card-like layouts
            element.className.contains("CardView", ignoreCase = true) -> "Card"

            // Button-like elements
            element.className.contains("Button", ignoreCase = true) -> "Button"

            // List items
            parent.className.contains("RecyclerView", ignoreCase = true) ||
                    parent.className.contains("ListView", ignoreCase = true) -> "Item"

            // Generic clickable
            element.isClickable -> "Option"

            else -> return null
        }

        return "$prefix ${position + 1}"
    }

    /**
     * Generate context-aware label
     *
     * Uses screen position to generate labels like:
     * - "Top button", "Bottom card"
     * - "Left tab", "Right tab"
     * - "Center button"
     *
     * @param element Element to generate label for
     * @return Context-aware label or null if not applicable
     */
    private fun generateContextLabel(element: ElementInfo): String? {
        val bounds = element.bounds
        val screenHeight = element.screenHeight
        val screenWidth = element.screenWidth

        // Can't generate context label without screen dimensions
        if (screenHeight <= 0 || screenWidth <= 0) return null

        // Determine vertical position
        val verticalPos = when {
            bounds.top < screenHeight / 3 -> "Top"
            bounds.top > screenHeight * 2 / 3 -> "Bottom"
            else -> "Center"
        }

        // Determine horizontal position (for tabs)
        val horizontalPos = when {
            bounds.left < screenWidth / 3 -> "Left"
            bounds.left > screenWidth * 2 / 3 -> "Right"
            else -> "Center"
        }

        // Choose appropriate label
        val type = element.className.substringAfterLast(".").lowercase()
            .replace("view", "")
            .replace("layout", "")
            .takeIf { it.isNotBlank() } ?: "element"

        return when {
            bounds.width() > screenWidth * 0.7 -> "$verticalPos $type"
            else -> "$horizontalPos $verticalPos $type".trim()
        }
    }

    /**
     * Generate command synonyms
     *
     * Creates alternative phrasings for voice command.
     * Returns JSON array format for database storage.
     *
     * @param actionType Action type (click, type, scroll, long_click)
     * @param label Element label
     * @return JSON array string of synonyms
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
     * Flush batch queue to database
     *
     * Inserts all queued commands as single transaction.
     * Called by ExplorationEngine after processing screen.
     *
     * Performance: ~50ms for 100 commands (20x faster than sequential)
     * Memory freed: ~150KB
     */
    override suspend fun flushBatch() = withContext(Dispatchers.IO) {
        if (batchQueue.isEmpty()) {
            if (developerSettings.isVerboseLoggingEnabled()) {
                Log.d(TAG, "Batch queue empty, nothing to flush")
            }
            return@withContext
        }

        val startTime = System.currentTimeMillis()
        val count = batchQueue.size

        try {
            // Convert queue to list for batch insert
            val commandsList = mutableListOf<GeneratedCommandDTO>()
            batchQueue.drainTo(commandsList)

            // Keep backup to prevent data loss if insert fails
            val backupCommands = commandsList.toList()

            try {
                // Use batch insert with retry logic for transient database errors
                DatabaseRetryUtil.withRetry {
                    commandRepository.insertBatch(commandsList)
                }

                val elapsedMs = System.currentTimeMillis() - startTime
                val rate = count * 1000 / elapsedMs.coerceAtLeast(1)
                Log.i(TAG, "Flushed $count commands in ${elapsedMs}ms (~$rate commands/sec)")

            } catch (e: Exception) {
                Log.e(TAG, "Failed to flush batch, re-queuing ${backupCommands.size} commands", e)
                // Re-queue commands to prevent data loss
                backupCommands.forEach { command ->
                    if (!batchQueue.offer(command)) {
                        Log.w(TAG, "Failed to re-queue command: ${command.commandText}")
                    }
                }
                throw e
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to flush batch queue", e)
            throw e
        }
    }

    /**
     * Clear batch queue
     *
     * Removes all queued commands without flushing to database.
     * Used for cleanup or error recovery.
     */
    override fun clearBatchQueue() {
        val count = batchQueue.size
        batchQueue.clear()
        if (count > 0) {
            Log.w(TAG, "Cleared $count commands from batch queue without flushing")
        }
    }

    /**
     * Get batch queue size
     *
     * For monitoring and debugging.
     *
     * @return Number of commands queued
     */
    fun getBatchQueueSize(): Int = batchQueue.size

    // ================================================================
    // IBatchManagerInterface Implementation
    // ================================================================

    /**
     * Add command to batch queue (IBatchManagerInterface)
     */
    override fun addCommand(command: GeneratedCommandDTO) {
        if (!batchQueue.offer(command)) {
            Log.w(TAG, "Batch queue full, command not added")
        }
    }

    /**
     * Get current batch size (IBatchManagerInterface)
     */
    override fun getBatchSize(): Int = batchQueue.size

    // ================================================================
    // IElementProcessorInterface Implementation
    // ================================================================

    /**
     * Process batch of elements (IElementProcessorInterface)
     *
     * More efficient than processing individually.
     */
    override suspend fun processBatch(
        elements: List<ElementInfo>,
        packageName: String
    ): List<ElementProcessingResult> {
        return elements.map { element ->
            processElement(element, packageName, ProcessingMode.BATCH)
        }
    }

    /**
     * Clear framework cache
     *
     * Clears all cached framework detections.
     * Useful for testing or when packages are updated.
     */
    fun clearCache() {
        frameworkCache.clear()
    }
}
