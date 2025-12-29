/**
 * UIScrapingEngine.kt - Advanced UI element extraction and analysis
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-08-28
 */
package com.augmentalis.voiceoscore.accessibility.extractors

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.Rect
import android.util.Log
import android.util.LruCache
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.uuidcreator.flutter.FlutterIdentifier
import com.augmentalis.uuidcreator.flutter.FlutterIdentifierExtractor
import com.augmentalis.voiceoscore.utils.ConditionalLogger
import com.augmentalis.voiceos.logging.PIILoggingWrapper
import com.augmentalis.voiceoscore.accessibility.extractors.UIScrapingEngine.Companion.FORBIDDEN_DESCRIPTIONS
import com.augmentalis.voiceoscore.accessibility.extractors.UIScrapingEngine.Companion.NEXT_LINE_REGEX
import com.augmentalis.voiceoscore.accessibility.extractors.UIScrapingEngine.Companion.NUMERIC_PATTERN
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.absoluteValue

/**
 * Advanced engine for scraping and analyzing UI elements from the screen
 * Combines performance optimizations, Legacy Avenue algorithms, and intelligent caching
 */
class UIScrapingEngine(
    private val service: AccessibilityService,
    private val context: Context = service.applicationContext
) {

    companion object {
        private const val TAG = "UIScrapingEngine"
        private const val TAG_EVENT = "EventScrapingEngine"

        // Performance and caching constants
        private const val CACHE_DURATION_MS = 1000L
        private const val PROFILE_CACHE_SIZE = 20
        private const val ELEMENT_CACHE_SIZE = 1000
        private const val MAX_DEPTH = 50
        private const val MIN_ELEMENT_SIZE = 10
        private const val MAX_TEXT_LENGTH = 50
        private const val DUPLICATE_DETECTION_EPSILON = 8 // pixels for approximate rect equality

        // Legacy Avenue text normalization constants
        private val PARSE_DESCRIPTION_DELIMITERS = listOf(":", "|", ",", ".")
        private val PARSE_DESCRIPTION_CLEANUP_REGEX = Regex("[^\\p{Alnum}\\s]")
        val NEXT_LINE_REGEX = Regex("[\n\t]")
        val NUMERIC_PATTERN = Regex("[0-9%/]")

        // Forbidden descriptions for filtering
        val FORBIDDEN_DESCRIPTIONS = setOf(
            "hf_overlay_number",
            "hf_scroll_horizontal",
            "hf_scroll_none",
            "hf_no_number",
            "hf_keep_help"
        )

        // Generic labels to filter out
        private val GENERIC_LABELS = setOf(
            "button", "image", "icon", "view", "text", "label",
            "item", "cell", "row", "column", "container"
        )

        // Special package names for context-aware processing
        private const val MY_FILES_PACKAGE = "com.realwear.filebrowser"

        // Special package names for context-aware processing

        // Apps that support numeric commands and level controls
        val NUMERIC_SUPPORT_PACKAGES = setOf(
            "com.realwear.filebrowser",      // My Files - numbered files/folders
            "com.realwear.devicecontrol",    // Device Control - volume/brightness levels
            "com.realwear.camera",           // Camera - zoom/exposure levels
            "com.realwear.sysinfo",          // SysInfo - system metrics
            "com.realwear.deviceinfo",       // Device Info - device details
            "com.android.camera2",           // Standard camera apps
            "com.android.settings"           // Settings with level controls
        )
    }

    // Thread-safe caches with LRU eviction
    private val profileCache = LruCache<String, UIProfile>(PROFILE_CACHE_SIZE)
    private val elementCache = LruCache<String, CachedElement>(ELEMENT_CACHE_SIZE)
    private val nodeCache = ConcurrentHashMap<String, WeakReference<AccessibilityNodeInfo>>()

    // Command replacement profiles for app-specific configurations
    private val commandReplacementProfiles = ConcurrentHashMap<String, Map<String, String>>()

    // Performance tracking with atomic operations
    private val lastScrapeTime = AtomicLong(0)
    private val scrapeCount = AtomicLong(0)
    private val cacheHits = AtomicLong(0)
    private val cacheMisses = AtomicLong(0)
    private val duplicatesFiltered = AtomicLong(0)

    // State flow for real-time monitoring
    private val _extractionState = MutableStateFlow(ExtractionState())
    val extractionState: StateFlow<ExtractionState> = _extractionState

    // Cached elements for synchronous access
    @Volatile
    private var cachedElements: List<UIElement> = emptyList()

    /**
     * State class for real-time extraction monitoring
     */
    data class ExtractionState(
        val isExtracting: Boolean = false,
        val elementCount: Int = 0,
        val cacheHitRate: Float = 0f,
        val lastExtractionTime: Long = 0,
        val duplicatesFiltered: Long = 0,
        val uiElements: List<String> = emptyList()
    )

    /**
     * Profile class for app-specific configurations
     */
    data class UIProfile(
        val packageName: String = "unknown",
        val timestamp: Long = 0,
        val commonElements: Set<String> = emptySet(),
        val staticCommands: List<String> = emptyList(),
        val layoutSignature: String = "",
        val commandReplacements: Map<String, String> = emptyMap(),
        val elementCount: Int = 0,
        val clickableCount: Int = 0,
        val textElements: List<String> = emptyList(),
        val processingTimeMs: Long = 0,
        val confidence: Float = 0f,
        val hasDuplicates: Boolean = false
    )

    /**
     * Cached element wrapper with metadata
     */
    data class CachedElement(
        val element: UIElement,
        val timestamp: Long,
        val accessCount: Int = 0
    )

    /**
     * Enhanced data class representing a UI element with advanced features
     */
    data class UIElement(
        val text: String,
        val contentDescription: String?,
        val className: String?,
        val isClickable: Boolean,
        val bounds: Rect,
        val nodeInfo: WeakReference<AccessibilityNodeInfo>,
        val depth: Int = 0,
        val parentClass: String? = null,
        val siblingIndex: Int = 0,
        val hash: String = "",
        val normalizedText: String = "", // Enhanced: Normalized text for command matching
        val isInheritedClickable: Boolean = false, // Enhanced: Inherited clickability
        val targetNodeRef: WeakReference<AccessibilityNodeInfo>? = null, // Enhanced: Target node for action
        val confidence: Float = 0.5f, // Enhanced: Confidence score
        val flutterIdentifier: FlutterIdentifier? = null, // Flutter 3.19+ stable identifier
        val resourceId: String? = null // Android resource ID (viewIdResourceName)
    )


    // ============================================================================
    // SECTION: Public API - Configuration
    // ============================================================================

    /**
     * Load command replacement profile for specific app
     */
    fun loadCommandReplacementProfile(packageName: String, replacements: Map<String, String>) {
        commandReplacementProfiles[packageName] = replacements
        ConditionalLogger.d(TAG) { "Loaded ${replacements.size} command replacements for $packageName" }
    }

    // ============================================================================
    // SECTION: Public API - Main Entry Points
    // ============================================================================

    /**
     * Extract all interactive UI elements from the current screen with advanced processing
     */
    fun extractUIElements(@Suppress("UNUSED_PARAMETER") event: AccessibilityEvent? = null): List<UIElement> {
        val currentTime = System.currentTimeMillis()

        // Use cache if recent
        if (currentTime - lastScrapeTime.get() < CACHE_DURATION_MS && cachedElements.isNotEmpty()) {
            cacheHits.incrementAndGet()
            updateCacheStats()
            return cachedElements
        }

        cacheMisses.incrementAndGet()

        // MEMORY FIX: Get rootNode and ensure it's recycled after use
        var rootNode: AccessibilityNodeInfo? = null
        try {
            rootNode = service.rootInActiveWindow
            if (rootNode == null) {
                ConditionalLogger.w(TAG) { "No root node available" }
                return emptyList()
            }

            val elements = mutableListOf<UIElement>()
            val currentPackage = getCurrentPackageNameInternal(rootNode)

            var replacements: Map<String, String>? = emptyMap()
            currentPackage?.let {
                replacements = commandReplacementProfiles[currentPackage]
            }

            // Enhanced recursive extraction with context
            extractElementsRecursiveEnhanced(
                rootNode,
                elements,
                0,
                null,
                0,
                replacements,
                currentPackage,
                isParentClickable = false
            )

            // Apply intelligent duplicate detection
            val filteredElements = applyIntelligentDuplicateDetection(elements)

            cachedElements = filteredElements
            lastScrapeTime.set(currentTime)
            scrapeCount.incrementAndGet()
            updateCacheStats()

            ConditionalLogger.d(TAG) { "Extracted ${elements.size} elements, filtered to ${filteredElements.size} unique elements" }
            return filteredElements
        } finally {
            // MEMORY FIX: Always recycle rootNode
            @Suppress("DEPRECATION")
            rootNode?.recycle()
        }
    }

    /**
     * Async extraction with proper coroutine management
     */
    suspend fun extractUIElementsAsync(event: AccessibilityEvent? = null): List<UIElement> = withContext(Dispatchers.Default) {
        _extractionState.value = _extractionState.value.copy(isExtracting = true, uiElements = emptyList())

        try {
            val elements = extractUIElements(event)
            val commands = elements.map { it.normalizedText }
            _extractionState.value = _extractionState.value.copy(
                elementCount = elements.size,
                lastExtractionTime = System.currentTimeMillis() - lastScrapeTime.get(),
                duplicatesFiltered = duplicatesFiltered.get(),
                uiElements = commands
            )

            elements
        } finally {
            _extractionState.value = _extractionState.value.copy(isExtracting = false)
        }
    }

    // ============================================================================
    // SECTION: UI Tree Traversal
    // ============================================================================

    /**
     * Enhanced recursive extraction with Legacy Avenue algorithms
     */
    private fun extractElementsRecursiveEnhanced(
        node: AccessibilityNodeInfo,
        elements: MutableList<UIElement>,
        depth: Int,
        parentNode: AccessibilityNodeInfo?,
        siblingIndex: Int,
        commandReplacements: Map<String, String>?,
        packageName: String?,
        isParentClickable: Boolean
    ) {
        if (depth > MAX_DEPTH) {
            ConditionalLogger.w(TAG) { "Max depth reached" }
            return
        }

        try {
            // Quick visibility check
            if (!node.isVisibleToUser) {
                return
            }

            // Extract text from node using Legacy Avenue logic
            val rawTextFromNode = extractRawTextFromNode(node)

            if (!rawTextFromNode.isNullOrBlank()) {
                // Apply sophisticated text normalization
                val normalizedText = normalizeTextAdvanced(rawTextFromNode, commandReplacements)

                if (normalizedText.isNotBlank()) {
                    // Determine target node intelligently
                    val targetNode = determineTargetNode(node, parentNode, isParentClickable)
                    val isEffectivelyClickable = isNodeEffectivelyClickable(node, isParentClickable)
                    val isNumericAndNotClickable = isNumeric(normalizedText) && !node.isPerformClickable()

                    // Apply package-specific filtering rules
                    val shouldInclude = shouldIncludeElement(
                        isEffectivelyClickable,
                        isNumericAndNotClickable,
                        packageName,
                        node
                    )

                    if (shouldInclude && isUsefulNodeEnhanced(node, normalizedText)) {
                        val element = createEnhancedUIElement(
                            node,
                            depth,
                            node.className?.toString(),
                            siblingIndex,
                            normalizedText,
                            isParentClickable,
                            targetNode
                        )
                        element.let { cmd ->
                            // Add to list only if no existing command has an approximately equal rect
                            if (elements.none { existingCmd ->
                                    cmd.bounds.let { newRect ->
                                        existingCmd.bounds.approximatelyEquals(newRect)
                                    }
                                }) {
                                elements.add(element)
                                // Update element cache - LruCache is thread-safe internally
                                // YOLO Phase 2 - Issue #13: Removed redundant synchronized block
                                elementCache.put(element.hash, CachedElement(element, System.currentTimeMillis()))
                            }
                        }

                    }
                }
            }

            // Process children with enhanced clickability inheritance
            val canChildrenInheritClickability = isParentClickable || node.isPerformClickable()
            val childCount = node.childCount

            for (i in 0 until childCount) {
                try {
                    val child = node.getChild(i)
                    if (child != null) {
                        extractElementsRecursiveEnhanced(
                            child,
                            elements,
                            depth + 1,
                            node,
                            i,
                            commandReplacements,
                            packageName,
                            canChildrenInheritClickability
                        )
                    }
                } finally {
                    // Child node recycling handled by AccessibilityNodeManager
                }
            }
        } catch (e: Exception) {
            ConditionalLogger.w(TAG) { "Error extracting from node at depth $depth: ${e.message}" }
        }
    }

    // ============================================================================
    // SECTION: Text Normalization & Parsing
    // ============================================================================

    /**
     * Extract raw text from node using Legacy Avenue preference order
     */
    private fun extractRawTextFromNode(node: AccessibilityNodeInfo): String? {
        // Prefer content description if valid, then text, then hint text for EditText
        return node.contentDescription?.toString()?.takeIf { it.isValidDescription() }
            ?: node.text?.toString()
            ?: node.hintText?.takeIf { node.isEditText() }?.toString()
    }

    /**
     * Advanced text normalization from Legacy Avenue
     */
    private fun normalizeTextAdvanced(
        rawText: String,
        commandReplacements: Map<String, String>?
    ): String {
        // Extract first line and normalize
        val firstLine = rawText.extractFirstLine().toLowerCaseTrimmed()

        // Apply sophisticated text parsing
        val parsedText = parseDescriptionAdvanced(firstLine)

        // Apply command replacements if available
        val finalText = if (!commandReplacements.isNullOrEmpty()) {
            parsedText.findScrapingItems(commandReplacements)
        } else {
            parsedText
        }

        return finalText
    }

    /**
     * Advanced description parsing from Legacy Avenue
     */
    private fun parseDescriptionAdvanced(text: String): String {
        var processedText = text
            .replace("&", "and")
            .replace("_", " ")

        // Find the first delimiter from our list that exists in the text
        val foundDelimiter = PARSE_DESCRIPTION_DELIMITERS.firstOrNull { processedText.contains(it) }
        if (foundDelimiter != null) {

            val parts = processedText.split(foundDelimiter, limit = 2)
            processedText = if ("hf_" in text) {
                // If "hf_" is present, take the part after the first delimiter
                if (parts.size > 1) parts[1] else parts[0]
            } else {
                // If "hf_" is NOT present, take the part before the first delimiter
                parts[0]
            }
        }
        // Final cleaning: remove non-alphanumeric characters (except space)
        return try {
            PARSE_DESCRIPTION_CLEANUP_REGEX.replace(processedText, "").trim()
        } catch (e: Exception) {
            // Text could be user data from UI elements, use PIILoggingWrapper
            PIILoggingWrapper.e(TAG, "Text normalization regex error for: '$processedText' - ${e.message}")
            processedText
        }
    }

    // ============================================================================
    // SECTION: Node Filtering & Validation
    // ============================================================================

    /**
     * Enhanced node usefulness check
     */
    private fun isUsefulNodeEnhanced(node: AccessibilityNodeInfo, normalizedText: String): Boolean {
        // Check bounds
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        if (bounds.width() < MIN_ELEMENT_SIZE || bounds.height() < MIN_ELEMENT_SIZE) {
            return false
        }

        // Must have meaningful text after normalization
        if (normalizedText.isBlank()) {
            return false
        }

        // Check if text length is reasonable
        if (normalizedText.length > MAX_TEXT_LENGTH) {
            return false
        }

        return true
    }

    // ============================================================================
    // SECTION: Duplicate Detection
    // ============================================================================

    /**
     * Intelligent duplicate detection using approximate rect equality
     */
    private fun applyIntelligentDuplicateDetection(elements: List<UIElement>): List<UIElement> {
        val uniqueElements = mutableListOf<UIElement>()
        var duplicateCount = 0

        for (element in elements) {
            val hasDuplicate = uniqueElements.any { existing ->
                existing.bounds.approximatelyEquals(element.bounds)
            }

            if (!hasDuplicate) {
                uniqueElements.add(element)
            } else {
                duplicateCount++
            }
        }

        duplicatesFiltered.set(duplicateCount.toLong())
        ConditionalLogger.d(TAG) { "Filtered $duplicateCount duplicate elements" }

        return uniqueElements
    }

    /**
     * Determine target node intelligently
     */
    private fun determineTargetNode(
        currentNode: AccessibilityNodeInfo,
        parentNode: AccessibilityNodeInfo?,
        isParentClickable: Boolean
    ): AccessibilityNodeInfo {
        // If parent is clickable AND current node isn't inherently perform-clickable,
        // and parent exists, target the parent. Otherwise, target current node.
        return if (isParentClickable && parentNode != null && !currentNode.isPerformClickable()) {
            parentNode
        } else {
            currentNode
        }
    }

    /**
     * Create enhanced UI element with additional metadata
     *
     * Flutter 3.19+ Enhancement: Extracts stable identifiers from Flutter apps
     * for improved VUID stability across sessions.
     */
    private fun createEnhancedUIElement(
        node: AccessibilityNodeInfo,
        depth: Int,
        parentClass: String?,
        siblingIndex: Int,
        normalizedText: String,
        isInheritedClickable: Boolean,
        targetNode: AccessibilityNodeInfo
    ): UIElement {
        val bounds = Rect()
        node.getBoundsInScreen(bounds)

        val text = node.text?.toString() ?: ""
        val description = node.contentDescription?.toString()
        val className = node.className?.toString()
        val resourceId = node.viewIdResourceName

        // Extract Flutter 3.19+ identifier if available
        val flutterIdentifier = FlutterIdentifierExtractor.extract(node)

        // Generate hash - prefer Flutter identifier for stability
        val hash = if (flutterIdentifier?.isStable == true) {
            // Use Flutter 3.19+ stable identifier for hash
            "flutter-${flutterIdentifier.toStableHash()}"
        } else {
            // Fall back to content-based hash
            generateElementHash(text, description, className, bounds)
        }

        val confidence = calculateConfidence(node, text, normalizedText)

        return UIElement(
            text = text,
            contentDescription = description,
            className = className,
            isClickable = node.isClickable,
            bounds = bounds,
            nodeInfo = WeakReference(node),
            depth = depth,
            parentClass = parentClass,
            siblingIndex = siblingIndex,
            hash = hash,
            normalizedText = normalizedText,
            isInheritedClickable = isInheritedClickable,
            targetNodeRef = WeakReference(targetNode),
            confidence = confidence,
            flutterIdentifier = flutterIdentifier,
            resourceId = resourceId
        )
    }

    // ============================================================================
    // SECTION: Command Generation
    // ============================================================================

    /**
     * Generate enhanced commands with sophisticated processing and app-specific logic
     */
    fun generateCommandsEnhanced(event: AccessibilityEvent): List<String> {
        val elements = extractUIElements(event)
        val commands = mutableSetOf<String>()
        val currentPackage = getCurrentPackageName()

        for (element in elements) {
            // Use normalized text for command generation
            if (element.normalizedText.isNotBlank()) {
                // Generate app-specific commands based on package
                generateAppSpecificCommands(element, currentPackage, commands)

                // Standard commands
                commands.add("click ${element.normalizedText}")
                commands.add("tap ${element.normalizedText}")

                if (element.isClickable || element.isInheritedClickable) {
                    commands.add("select ${element.normalizedText}")
                    commands.add("activate ${element.normalizedText}")
                }
            }

            // Add original text-based commands for compatibility
            if (element.text.isNotBlank() && element.text.length < MAX_TEXT_LENGTH) {
                val normalizedText = element.text.lowercase().trim()
                commands.add("click $normalizedText")
                commands.add("tap $normalizedText")
            }
        }

        return commands.sorted()
    }

    /**
     * Generate app-specific commands for better user experience
     */
    private fun generateAppSpecificCommands(
        element: UIElement,
        packageName: String?,
        commands: MutableSet<String>
    ) {
        val text = element.normalizedText.lowercase()

        when (packageName) {
            "com.realwear.devicecontrol" -> {
                // Device Control specific commands
                if (text.matches(Regex("\\d+"))) {
                    commands.add("set level $text")
                    commands.add("level $text")
                }
                if (text.contains("%")) {
                    val number = text.replace("%", "")
                    if (number.isNotBlank()) {
                        commands.add("set $number percent")
                        commands.add("$number percent")
                    }
                }
            }

            "com.realwear.camera" -> {
                // Camera specific commands
                if (text.matches(Regex("\\d+"))) {
                    commands.add("zoom level $text")
                    commands.add("exposure level $text")
                    commands.add("set zoom $text")
                    commands.add("set exposure $text")
                }
                if (text.contains("x")) {
                    commands.add("zoom $text")
                    commands.add("set zoom $text")
                }
            }

            "com.realwear.filebrowser" -> {
                // File browser specific commands
                if (text.matches(Regex("\\d+"))) {
                    commands.add("item $text")
                    commands.add("file $text")
                    commands.add("folder $text")
                    commands.add("number $text")
                }
            }

            in NUMERIC_SUPPORT_PACKAGES -> {
                // Generic numeric commands for other supported apps
                if (text.matches(Regex("\\d+"))) {
                    commands.add("number $text")
                    commands.add("item $text")
                }
            }
        }
    }

    /**
     * Generate voice commands from current UI elements (backward compatible)
     */
    fun generateCommands(event: AccessibilityEvent): List<String> {
        val elements = extractUIElements(event)
        val commands = mutableSetOf<String>()

        for (element in elements) {
            // Add text-based commands
            if (element.text.isNotBlank() && element.text.length < MAX_TEXT_LENGTH) {
                val normalizedText = element.text.lowercase().trim()
                commands.add("click $normalizedText")
                commands.add("tap $normalizedText")
                if (element.isClickable) {
                    commands.add("select $normalizedText")
                }
            }

            // Add description-based commands
            element.contentDescription?.let { desc ->
                if (desc.isNotBlank() && desc.length < MAX_TEXT_LENGTH) {
                    val normalizedDesc = desc.lowercase().trim()
                    commands.add("click $normalizedDesc")
                    commands.add("tap $normalizedDesc")
                }
            }
        }

        return commands.sorted()
    }

    // ============================================================================
    // SECTION: Confidence & Scoring
    // ============================================================================

    /**
     * Calculate confidence score for command
     */
    private fun calculateConfidence(
        node: AccessibilityNodeInfo,
        @Suppress("UNUSED_PARAMETER") originalText: String,
        normalizedText: String
    ): Float {
        var confidence = 0.5f

        // Increase for clickable elements
        if (node.isClickable) confidence += 0.2f

        // Increase for focused elements
        if (node.isFocused) confidence += 0.1f

        // Decrease for generic text
        if (normalizedText in GENERIC_LABELS) confidence -= 0.3f

        // Increase for content descriptions (usually more accurate)
        if (node.contentDescription != null) confidence += 0.1f

        // Increase for proper length
        if (normalizedText.length in 3..20) confidence += 0.1f

        return confidence.coerceIn(0f, 1f)
    }

    /**
     * Get current app package name from an already-obtained root node (no new allocation)
     * MEMORY FIX: Extracted to avoid double rootInActiveWindow allocation and leak
     */
    private fun getCurrentPackageNameInternal(rootNode: AccessibilityNodeInfo?): String? {
        return try {
            rootNode?.packageName?.toString()
        } catch (e: Exception) {
            ConditionalLogger.w(TAG) { "Error getting package name: ${e.message}" }
            null
        }
    }

    /**
     * Get current app package name safely
     * MEMORY FIX: Recycles rootNode after use
     */
    private fun getCurrentPackageName(): String? {
        var rootNode: AccessibilityNodeInfo? = null
        return try {
            rootNode = service.rootInActiveWindow
            rootNode?.packageName?.toString()
        } catch (e: Exception) {
            ConditionalLogger.w(TAG) { "Error getting package name: ${e.message}" }
            null
        } finally {
            @Suppress("DEPRECATION")
            rootNode?.recycle()
        }
    }

    /**
     * Generate element hash for caching
     *
     * YOLO Phase 2 - Issue #19: Improved hash collision handling using SHA-256
     * Previous implementation used Java hashCode() (32-bit), high collision risk
     * Now using SHA-256 for cryptographically secure, collision-resistant hashing
     */
    private fun generateElementHash(
        text: String,
        description: String?,
        className: String?,
        bounds: Rect
    ): String {
        val builder = StringBuilder()
        builder.append(text).append('_')
        builder.append(description ?: "").append('_')
        builder.append(className ?: "").append('_')
        builder.append(bounds.toShortString())

        // Use SHA-256 instead of hashCode() for collision resistance
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(builder.toString().toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    // ============================================================================
    // SECTION: Cache Management
    // ============================================================================

    /**
     * Update cache statistics
     */
    private fun updateCacheStats() {
        val hits = cacheHits.get().toFloat()
        val misses = cacheMisses.get().toFloat()
        val total = hits + misses
        val hitRate = if (total > 0) hits / total else 0f

        _extractionState.value = _extractionState.value.copy(
            cacheHitRate = hitRate,
            duplicatesFiltered = duplicatesFiltered.get()
        )

        if (scrapeCount.get() % 100 == 0L) {
            ConditionalLogger.i(TAG) {
                "Cache stats: Hit rate=${hitRate * 100}%, Total scrapes=${scrapeCount.get()}, Duplicates filtered=${duplicatesFiltered.get()}"
            }
        }
    }

    /**
     * Clear all caches
     *
     * YOLO Phase 2 - Issue #13: Removed redundant synchronized blocks
     * LruCache is thread-safe internally, no need for explicit synchronization
     */
    fun clearCache() {
        nodeCache.clear()
        // LruCache.evictAll() is already thread-safe
        elementCache.evictAll()
        // LruCache.evictAll() is already thread-safe
        profileCache.evictAll()
        commandReplacementProfiles.clear()
        cachedElements = emptyList()
        lastScrapeTime.set(0)
        cacheHits.set(0)
        cacheMisses.set(0)
        duplicatesFiltered.set(0)
        ConditionalLogger.d(TAG) { "All caches cleared" }
    }

    // ============================================================================
    // SECTION: Metrics & Monitoring
    // ============================================================================

    /**
     * Get performance metrics
     */
    fun getPerformanceMetrics(): Map<String, Any> {
        return mapOf(
            "scrapeCount" to scrapeCount.get(),
            "cacheHitRate" to _extractionState.value.cacheHitRate,
            "elementCacheSize" to elementCache.size(),
            "profileCacheSize" to profileCache.size(),
            "lastExtractionTime" to _extractionState.value.lastExtractionTime,
            "nodeCacheSize" to nodeCache.size,
            "duplicatesFiltered" to duplicatesFiltered.get(),
            "commandReplacementProfiles" to commandReplacementProfiles.size
        )
    }

    /**
     * Get app-specific profile information
     */
    fun getProfileInfo(packageName: String): Map<String, Any> {
        val replacements = commandReplacementProfiles[packageName]
        return mapOf(
            "hasProfile" to (replacements != null),
            "replacementCount" to (replacements?.size ?: 0),
            "profileKeys" to (replacements?.keys?.toList() ?: emptyList<String>())
        )
    }

    /**
     * Cleanup resources properly
     */
    fun destroy() {
        clearCache()
        ConditionalLogger.d(TAG) { "UIScrapingEngine destroyed" }
    }
}

// ============================================================================
// SECTION: Extension Functions
// ============================================================================

private fun String.extractFirstLine(): String {
    return this.split(NEXT_LINE_REGEX).firstOrNull().orEmpty()
}

private fun String.toLowerCaseTrimmed(): String {
    return this.trim().lowercase()
}

private fun String.isValidDescription(): Boolean {
    if (isEmpty()) return false

    val containsPipeAndNotHf = contains("|") && !contains("hf_")
    val containsHashAndPipe = contains("#") && contains("|")

    return !(FORBIDDEN_DESCRIPTIONS.contains(this) || containsHashAndPipe || containsPipeAndNotHf)
}

private fun AccessibilityNodeInfo.isEditText(): Boolean {
    return className?.toString()?.contains("edittext", ignoreCase = true) == true
}

private fun AccessibilityNodeInfo.isPerformClickable(): Boolean {
    return isClickable || isEditable || isSelected || isCheckable || isLongClickable || isContextClickable
}

private fun isNumeric(input: String): Boolean {
    return input.contains(NUMERIC_PATTERN)
}

private fun String.findScrapingItems(replacements: Map<String, String>): String {
    for ((key, value) in replacements) {
        if (this.contains(key)) {
            return this.replace(key, value)
        }
    }
    return this
}

private fun isNodeEffectivelyClickable(node: AccessibilityNodeInfo, isParentClickable: Boolean): Boolean {
    return node.isClickable || node.isPerformClickable() || isParentClickable
}

private fun shouldIncludeElement(
    isEffectivelyClickable: Boolean,
    isNumericAndNotClickable: Boolean,
    packageName: String?,
    node: AccessibilityNodeInfo
): Boolean {
    // Check if this app supports numeric content
    val supportsNumericContent = packageName in UIScrapingEngine.NUMERIC_SUPPORT_PACKAGES

    // Enhanced logic for numeric content detection
    val shouldIncludeNumeric = if (supportsNumericContent && isNumericAndNotClickable) {
        // For apps that support numeric content, include numeric elements even if not clickable
        // But add additional validation to ensure they're meaningful
        isValidNumericElement(node, packageName)
    } else {
        // Standard logic: exclude numeric non-clickable elements
        !isNumericAndNotClickable
    }

    // Element should be included if:
    // 1. It's effectively clickable, OR
    // 2. It's in an app that supports numeric content
    // AND it passes the numeric validation
    return (isEffectivelyClickable || supportsNumericContent) && shouldIncludeNumeric
}

/**
 * Enhanced validation for numeric elements to ensure they're meaningful commands
 */
private fun isValidNumericElement(node: AccessibilityNodeInfo, packageName: String?): Boolean {
    val bounds = Rect()
    node.getBoundsInScreen(bounds)

    // Must have reasonable size (not too small)
    if (bounds.width() < 20 || bounds.height() < 20) {
        return false
    }

    // Get the text content
    val text = node.text?.toString() ?: node.contentDescription?.toString() ?: ""
    if (text.isBlank()) return false
    // Package-specific validation rules
    return when (packageName) {
        "com.realwear.filebrowser" -> {
            // For file browser: accept numbered items, file names with numbers
            text.matches(Regex("\\d+")) || text.contains(Regex("\\d+"))
        }

        "com.realwear.devicecontrol" -> {
            // For device control: accept level indicators, percentage, volume levels
            text.matches(Regex("(?i).*level\\s*\\d+.*")) ||
                    text.matches(Regex("\\d+%?")) ||
                    text.contains(Regex("(?i)(set|level|volume|brightness)\\s*\\d+"))
        }

        "com.realwear.camera" -> {
            // For camera: accept zoom levels, exposure settings
            text.matches(Regex("(?i).*(zoom|exposure)\\s*\\d+.*")) ||
                    text.matches(Regex("\\d+x")) ||
                    text.contains(Regex("(?i)(zoom|exposure|level)\\s*\\d+"))
        }

        in UIScrapingEngine.NUMERIC_SUPPORT_PACKAGES -> {
            // Generic numeric validation for other supported apps
            text.matches(Regex("\\d+")) || text.contains(Regex("\\d+"))
        }

        else -> false
    }
}

/**
 * Approximate rect equality for duplicate detection
 */
private fun Rect.approximatelyEquals(other: Rect): Boolean {
    val epsilon = 8 // pixels for approximate rect equality
    return (left - other.left).absoluteValue <= epsilon &&
            (right - other.right).absoluteValue <= epsilon &&
            (top - other.top).absoluteValue <= epsilon &&
            (bottom - other.bottom).absoluteValue <= epsilon
}