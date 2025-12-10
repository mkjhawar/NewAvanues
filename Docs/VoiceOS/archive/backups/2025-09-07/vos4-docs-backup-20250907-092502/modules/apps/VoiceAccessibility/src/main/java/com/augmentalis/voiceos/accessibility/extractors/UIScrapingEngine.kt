/**
 * UIScrapingEngine.kt - Advanced UI element extraction and analysis
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-08-28
 * Updated: 2025-09-04
 * 
 * Comprehensive UI scraping engine combining best features from all versions:
 * - Performance optimizations and thread safety (V2)
 * - Legacy Avenue algorithms and text normalization (V3)
 * - Profile management and command replacement (V3)
 * - Intelligent duplicate detection and caching
 */
package com.augmentalis.voiceos.accessibility.extractors

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.Rect
import android.util.Log
import android.util.LruCache
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject
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
        
        // Performance and caching constants
        private const val CACHE_DURATION_MS = 500L
        private const val PROFILE_CACHE_SIZE = 20
        private const val ELEMENT_CACHE_SIZE = 1000
        private const val MAX_DEPTH = 50
        private const val MIN_ELEMENT_SIZE = 10
        private const val MAX_TEXT_LENGTH = 50
        private const val DUPLICATE_DETECTION_EPSILON = 8 // pixels for approximate rect equality
        
        // Legacy Avenue text normalization constants
        private val PARSE_DESCRIPTION_DELIMITERS = listOf(":", "|", ",", ".")
        private val PARSE_DESCRIPTION_CLEANUP_REGEX = Regex("[^\\p{Alnum}\\s]")
        private val NEXT_LINE_REGEX = Regex("[\n\t]")
        private val NUMERIC_PATTERN = Regex("[0-9%/]")
        
        // Forbidden descriptions for filtering
        private val FORBIDDEN_DESCRIPTIONS = setOf(
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
    
    // Properly managed coroutine scope
    private var scope: CoroutineScope? = null
    
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
        val duplicatesFiltered: Long = 0
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
        val confidence: Float = 0.5f // Enhanced: Confidence score
    )
    
    /**
     * Initialize the engine with proper scope management
     */
    fun initialize() {
        scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        Log.d(TAG, "UIScrapingEngine initialized with enhanced features")
    }
    
    /**
     * Load command replacement profile for specific app
     */
    fun loadCommandReplacementProfile(packageName: String, replacements: Map<String, String>) {
        commandReplacementProfiles[packageName] = replacements
        Log.d(TAG, "Loaded ${replacements.size} command replacements for $packageName")
    }
    
    /**
     * Extract all interactive UI elements from the current screen with advanced processing
     */
    fun extractUIElements(): List<UIElement> {
        val currentTime = System.currentTimeMillis()
        
        // Use cache if recent
        if (currentTime - lastScrapeTime.get() < CACHE_DURATION_MS && cachedElements.isNotEmpty()) {
            cacheHits.incrementAndGet()
            updateCacheStats()
            return cachedElements
        }
        
        cacheMisses.incrementAndGet()
        
        val rootNode = service.rootInActiveWindow
        if (rootNode == null) {
            Log.w(TAG, "No root node available")
            return emptyList()
        }
        
        val elements = mutableListOf<UIElement>()
        val currentPackage = getCurrentPackageName()
        val replacements = commandReplacementProfiles[currentPackage]
        
        try {
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
        } finally {
            // rootNode.recycle() // Deprecated - Android handles this automatically
        }
        
        // Apply intelligent duplicate detection
        val filteredElements = applyIntelligentDuplicateDetection(elements)
        
        cachedElements = filteredElements
        lastScrapeTime.set(currentTime)
        scrapeCount.incrementAndGet()
        updateCacheStats()
        
        Log.d(TAG, "Extracted ${elements.size} elements, filtered to ${filteredElements.size} unique elements")
        return filteredElements
    }
    
    /**
     * Async extraction with proper coroutine management
     */
    suspend fun extractUIElementsAsync(): List<UIElement> = withContext(Dispatchers.Default) {
        _extractionState.value = _extractionState.value.copy(isExtracting = true)
        
        try {
            val elements = extractUIElements()
            
            _extractionState.value = _extractionState.value.copy(
                elementCount = elements.size,
                lastExtractionTime = System.currentTimeMillis() - lastScrapeTime.get(),
                duplicatesFiltered = duplicatesFiltered.get()
            )
            
            elements
        } finally {
            _extractionState.value = _extractionState.value.copy(isExtracting = false)
        }
    }
    
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
            Log.w(TAG, "Max depth reached")
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
                        elements.add(element)
                        
                        // Update element cache with thread safety
                        synchronized(elementCache) {
                            elementCache.put(element.hash, CachedElement(element, System.currentTimeMillis()))
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
                    // child?.recycle() // Deprecated - Android handles this automatically
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error extracting from node at depth $depth: ${e.message}")
        }
    }
    
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
            Log.e(TAG, "Text normalization regex error for: '$processedText'", e)
            processedText
        }
    }
    
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
        Log.d(TAG, "Filtered $duplicateCount duplicate elements")
        
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
        
        val hash = generateElementHash(text, description, className, bounds)
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
            confidence = confidence
        )
    }
    
    /**
     * Generate enhanced commands with sophisticated processing
     */
    fun generateCommandsEnhanced(): List<String> {
        val elements = extractUIElements()
        val commands = mutableSetOf<String>()
        
        for (element in elements) {
            // Use normalized text for command generation
            if (element.normalizedText.isNotBlank()) {
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
     * Generate voice commands from current UI elements (backward compatible)
     */
    fun generateCommands(): List<String> {
        val elements = extractUIElements()
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
     * Get current app package name safely
     */
    private fun getCurrentPackageName(): String? {
        return try {
            val windows = service.windows
            for (window in windows) {
                if (window != null && window.type == AccessibilityWindowInfo.TYPE_APPLICATION) {
                    try {
                        val root = window.root
                        root?.packageName?.toString()?.let { return it }
                    } finally {
                        // root?.recycle() // Deprecated - Android handles this automatically
                    }
                }
            }
            null
        } catch (e: Exception) {
            Log.w(TAG, "Error getting package name: ${e.message}")
            null
        }
    }
    
    /**
     * Generate element hash for caching
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
        return builder.toString().hashCode().toString()
    }
    
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
            Log.i(TAG, "Cache stats: Hit rate=${hitRate * 100}%, Total scrapes=${scrapeCount.get()}, Duplicates filtered=${duplicatesFiltered.get()}")
        }
    }
    
    /**
     * Clear all caches
     */
    fun clearCache() {
        nodeCache.clear()
        synchronized(elementCache) {
            elementCache.evictAll()
        }
        synchronized(profileCache) {
            profileCache.evictAll()
        }
        commandReplacementProfiles.clear()
        cachedElements = emptyList()
        lastScrapeTime.set(0)
        cacheHits.set(0)
        cacheMisses.set(0)
        duplicatesFiltered.set(0)
        Log.d(TAG, "All caches cleared")
    }
    
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
        scope?.cancel()
        scope = null
        clearCache()
        Log.d(TAG, "UIScrapingEngine destroyed")
    }
}

// Extension functions from Legacy Avenue

private fun String.extractFirstLine(): String {
    val regex = Regex("[\n\t]")
    return this.split(regex).firstOrNull().orEmpty()
}

private fun String.toLowerCaseTrimmed(): String {
    return this.trim().lowercase()
}

private fun String.isValidDescription(): Boolean {
    if (isEmpty()) return false
    
    val forbiddenDescriptions = setOf(
        "hf_overlay_number",
        "hf_scroll_horizontal", 
        "hf_scroll_none",
        "hf_no_number",
        "hf_keep_help"
    )
    
    val containsPipeAndNotHf = contains("|") && !contains("hf_")
    val containsHashAndPipe = contains("#") && contains("|")
    
    return !(forbiddenDescriptions.contains(this) || containsHashAndPipe || containsPipeAndNotHf)
}

private fun AccessibilityNodeInfo.isEditText(): Boolean {
    return className?.toString()?.contains("edittext", ignoreCase = true) == true
}

private fun AccessibilityNodeInfo.isPerformClickable(): Boolean {
    return isClickable || isEditable || isSelected || isCheckable || isLongClickable || isContextClickable
}

private fun isNumeric(input: String): Boolean {
    val numericPattern = Regex("[0-9%/]")
    return input.contains(numericPattern)
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
    @Suppress("UNUSED_PARAMETER") node: AccessibilityNodeInfo
): Boolean {
    val myFilesPackage = "com.realwear.filebrowser"
    return (isEffectivelyClickable || packageName == myFilesPackage) && 
           (!isNumericAndNotClickable || packageName == myFilesPackage)
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