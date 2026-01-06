package com.augmentalis.voiceoscoreng.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.voiceoscoreng.common.Bounds
import com.augmentalis.voiceoscoreng.common.ElementInfo
import com.augmentalis.voiceoscoreng.common.VUIDGenerator
import com.augmentalis.voiceoscoreng.common.VUIDTypeCode
import com.augmentalis.voiceoscoreng.functions.HashUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "VoiceOSA11yService"

/**
 * Accessibility Service for VoiceOSCoreNG testing.
 *
 * Provides real-time exploration of apps on the device,
 * extracting UI elements and processing them through the
 * VoiceOSCoreNG library for VUID generation, deduplication,
 * hierarchy tracking, and command generation.
 */
class VoiceOSAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    companion object {
        private var instance: VoiceOSAccessibilityService? = null

        private val _isConnected = MutableStateFlow(false)
        val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

        private val _explorationResults = MutableStateFlow<ExplorationResult?>(null)
        val explorationResults: StateFlow<ExplorationResult?> = _explorationResults.asStateFlow()

        private val _lastError = MutableStateFlow<String?>(null)
        val lastError: StateFlow<String?> = _lastError.asStateFlow()

        fun getInstance(): VoiceOSAccessibilityService? = instance

        fun exploreCurrentApp() {
            instance?.performExploration()
        }

        fun exploreAllApps() {
            instance?.performFullExploration()
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "onServiceConnected() called")
        instance = this
        _isConnected.value = true
        Log.d(TAG, "isConnected set to true")

        try {
            serviceInfo = serviceInfo.apply {
                eventTypes = AccessibilityEvent.TYPES_ALL_MASK
                feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
                flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                        AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                        AccessibilityServiceInfo.FLAG_REQUEST_ENHANCED_WEB_ACCESSIBILITY or
                        AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS  // Required for windows property
                notificationTimeout = 100
            }
            Log.d(TAG, "serviceInfo configured successfully with FLAG_RETRIEVE_INTERACTIVE_WINDOWS")
        } catch (e: Exception) {
            Log.e(TAG, "Error configuring serviceInfo", e)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // We handle events on-demand, not automatically
    }

    override fun onInterrupt() {
        // Required override
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy() called")
        super.onDestroy()
        instance = null
        _isConnected.value = false
        serviceScope.cancel()
    }

    /**
     * Perform exploration of the currently focused app
     */
    fun performExploration() {
        serviceScope.launch {
            try {
                val rootNode = rootInActiveWindow ?: run {
                    _lastError.value = "No active window available"
                    return@launch
                }

                val result = exploreNode(rootNode)
                _explorationResults.value = result
                rootNode.recycle()

            } catch (e: Exception) {
                _lastError.value = "Exploration failed: ${e.message}"
            }
        }
    }

    /**
     * Perform exploration across all windows
     */
    fun performFullExploration() {
        serviceScope.launch {
            try {
                Log.d(TAG, "performFullExploration() started")
                val allResults = mutableListOf<ExplorationResult>()

                val windowList = windows
                Log.d(TAG, "Found ${windowList.size} windows to explore")

                windowList.forEachIndexed { index, window ->
                    Log.d(TAG, "Window $index: type=${window.type}, title=${window.title}, layer=${window.layer}")
                    window.root?.let { rootNode ->
                        Log.d(TAG, "  Exploring root node: pkg=${rootNode.packageName}")
                        val result = exploreNode(rootNode)
                        allResults.add(result)
                        Log.d(TAG, "  Found ${result.totalElements} elements")
                        rootNode.recycle()
                    } ?: Log.d(TAG, "  Window $index has null root")
                }

                // Merge all results
                val merged = mergeResults(allResults)
                _explorationResults.value = merged
                Log.d(TAG, "performFullExploration() complete: ${merged.totalElements} total elements from ${allResults.size} windows")

            } catch (e: Exception) {
                Log.e(TAG, "Full exploration failed", e)
                _lastError.value = "Full exploration failed: ${e.message}"
            }
        }
    }

    private suspend fun exploreNode(rootNode: AccessibilityNodeInfo): ExplorationResult {
        val startTime = System.currentTimeMillis()

        // Extract all elements from the tree
        val elements = mutableListOf<ElementInfo>()
        val hierarchy = mutableListOf<HierarchyNode>()
        val seenHashes = mutableSetOf<String>()
        val duplicates = mutableListOf<DuplicateInfo>()

        extractElements(rootNode, elements, hierarchy, seenHashes, duplicates, depth = 0)

        // Generate VUIDs for all elements
        val packageName = rootNode.packageName?.toString() ?: "unknown"
        val vuids = elements.map { element ->
            val typeCode = VUIDGenerator.getTypeCode(element.className)
            val elementIdentifier = buildString {
                append(element.className)
                if (element.resourceId.isNotBlank()) append(":${element.resourceId}")
                if (element.text.isNotBlank()) append(":${element.text.take(20)}")
            }
            val elemHash = HashUtils.generateHash(elementIdentifier, 8)
            val vuid = VUIDGenerator.generate(packageName, typeCode, elemHash)

            VUIDInfo(
                element = element,
                vuid = vuid,
                hash = elemHash
            )
        }

        // Derive labels for ALL elements (looking at children for empty parents)
        val elementLabels = deriveElementLabels(elements, hierarchy)

        // Generate commands (pass hierarchy to find child labels)
        val commands = generateCommands(elements, hierarchy, elementLabels, packageName)

        // Generate AVU output with derived labels
        val avuOutput = generateAVU(packageName, elements, elementLabels, commands)

        val duration = System.currentTimeMillis() - startTime

        Log.d(TAG, "=== DEDUPLICATION RESULTS ===")
        Log.d(TAG, "Total elements: ${elements.size}")
        Log.d(TAG, "Unique hashes: ${seenHashes.size}")
        Log.d(TAG, "Duplicates found: ${duplicates.size}")
        duplicates.take(5).forEach { dup ->
            Log.d(TAG, "  DUP: ${dup.element.className.substringAfterLast(".")} '${dup.element.text.take(20)}' first@${dup.firstSeenIndex}")
        }
        Log.d(TAG, "=============================")

        return ExplorationResult(
            packageName = packageName,
            timestamp = System.currentTimeMillis(),
            duration = duration,
            totalElements = elements.size,
            clickableElements = elements.count { it.isClickable },
            scrollableElements = elements.count { it.isScrollable },
            elements = elements,
            vuids = vuids,
            hierarchy = hierarchy,
            duplicates = duplicates,
            deduplicationStats = DeduplicationStats(
                totalHashes = elements.size,  // Total elements processed
                uniqueHashes = seenHashes.size,  // Unique hash count
                duplicateCount = duplicates.size,  // Number of duplicate occurrences
                duplicateElements = duplicates
            ),
            commands = commands,
            avuOutput = avuOutput,
            elementLabels = elementLabels  // Map of index -> derived label
        )
    }

    private fun extractElements(
        node: AccessibilityNodeInfo,
        elements: MutableList<ElementInfo>,
        hierarchy: MutableList<HierarchyNode>,
        seenHashes: MutableSet<String>,
        duplicates: MutableList<DuplicateInfo>,
        depth: Int,
        parentIndex: Int? = null
    ) {
        val bounds = Rect()
        node.getBoundsInScreen(bounds)

        val element = ElementInfo(
            className = node.className?.toString() ?: "",
            resourceId = node.viewIdResourceName ?: "",
            text = node.text?.toString() ?: "",
            contentDescription = node.contentDescription?.toString() ?: "",
            bounds = Bounds(bounds.left, bounds.top, bounds.right, bounds.bottom),
            isClickable = node.isClickable,
            isScrollable = node.isScrollable,
            isEnabled = node.isEnabled,
            packageName = node.packageName?.toString() ?: ""
        )

        // Generate hash for deduplication - use className|resourceId|text (NOT bounds, as bounds make every element unique)
        val hashInput = "${element.className}|${element.resourceId}|${element.text}"
        val hash = HashUtils.generateHash(hashInput, 16)

        if (seenHashes.contains(hash)) {
            Log.d(TAG, "DUPLICATE FOUND: hash=$hash class=${element.className.substringAfterLast(".")} text='${element.text.take(20)}'")
            duplicates.add(DuplicateInfo(
                hash = hash,
                element = element,
                firstSeenIndex = elements.indexOfFirst { e ->
                    val h = HashUtils.generateHash("${e.className}|${e.resourceId}|${e.text}", 16)
                    h == hash
                }
            ))
        } else {
            seenHashes.add(hash)
        }

        val currentIndex = elements.size
        elements.add(element)

        // Track hierarchy
        hierarchy.add(HierarchyNode(
            index = currentIndex,
            depth = depth,
            parentIndex = parentIndex,
            childCount = node.childCount,
            className = element.className.substringAfterLast(".")
        ))

        // Recurse into children
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                extractElements(child, elements, hierarchy, seenHashes, duplicates, depth + 1, currentIndex)
                child.recycle()
            }
        }
    }

    /**
     * Derive labels for ALL elements by looking at child TextViews when parent has no text.
     * Returns a map of elementIndex -> derivedLabel
     */
    private fun deriveElementLabels(
        elements: List<ElementInfo>,
        hierarchy: List<HierarchyNode>
    ): Map<Int, String> {
        val labels = mutableMapOf<Int, String>()

        elements.forEachIndexed { index, element ->
            // First try the element's own content
            var label: String? = when {
                element.text.isNotBlank() -> element.text.take(30)
                element.contentDescription.isNotBlank() -> element.contentDescription.take(30)
                element.resourceId.isNotBlank() -> element.resourceId.substringAfterLast("/").replace("_", " ")
                else -> null
            }

            // If no label, look at children (especially for clickable Views wrapping TextViews)
            if (label == null) {
                val node = hierarchy.getOrNull(index)
                if (node != null && node.childCount > 0) {
                    for (childIdx in (index + 1) until minOf(index + 10, elements.size)) {
                        val childNode = hierarchy.getOrNull(childIdx) ?: continue
                        if (childNode.depth <= node.depth) break
                        if (childNode.depth == node.depth + 1) {
                            val childElement = elements[childIdx]
                            if (childElement.text.isNotBlank()) {
                                label = childElement.text.take(30)
                                break
                            }
                            if (childElement.contentDescription.isNotBlank()) {
                                label = childElement.contentDescription.take(30)
                                break
                            }
                        }
                    }
                }
            }

            // Store the label (or fallback to class name)
            labels[index] = label ?: element.className.substringAfterLast(".")
        }

        return labels
    }

    /**
     * Generate AVU (Avanues Universal) format output with proper command names
     */
    private fun generateAVU(
        packageName: String,
        elements: List<ElementInfo>,
        elementLabels: Map<Int, String>,
        commands: List<GeneratedCommand>
    ): String {
        return buildString {
            appendLine("# Avanues Universal Format v2.0")
            appendLine("# Package: $packageName")
            appendLine("# Elements: ${elements.size}")
            appendLine("# Commands: ${commands.size}")
            appendLine()
            appendLine("schema: avu-2.0")
            appendLine("version: 2.0.0")
            appendLine("package: $packageName")
            appendLine()

            // Elements section with derived labels
            appendLine("@elements:")
            elements.forEachIndexed { index, element ->
                val typeCode = VUIDGenerator.getTypeCode(element.className)
                val label = elementLabels[index] ?: element.className.substringAfterLast(".")
                val clickable = if (element.isClickable) "T" else "F"
                val scrollable = if (element.isScrollable) "T" else "F"
                appendLine("  - idx:$index type:${typeCode.abbrev} label:\"$label\" click:$clickable scroll:$scrollable")
            }
            appendLine()

            // Commands section with voice phrases
            appendLine("@commands:")
            if (commands.isEmpty()) {
                appendLine("  # No actionable elements found")
            } else {
                commands.forEach { cmd ->
                    // The voice command is just the label (e.g., "Accessibility", "Reset")
                    // The action (tap/scroll/toggle) is metadata
                    appendLine("  - voice:\"${cmd.derivedLabel}\" action:${cmd.action} vuid:${cmd.targetVuid}")
                    // Also include alternate phrases
                    appendLine("    alternates: [\"${cmd.phrase}\", \"press ${cmd.derivedLabel}\", \"select ${cmd.derivedLabel}\"]")
                }
            }
            appendLine()

            // Actionable elements summary
            appendLine("@actionable:")
            val actionableElements = elements.mapIndexedNotNull { index, element ->
                if (element.isClickable || element.isScrollable) {
                    val label = elementLabels[index] ?: return@mapIndexedNotNull null
                    val action = when {
                        element.isClickable -> "tap"
                        element.isScrollable -> "scroll"
                        else -> "interact"
                    }
                    "  - \"$label\" -> $action"
                } else null
            }
            actionableElements.forEach { appendLine(it) }
        }
    }

    /**
     * Generate voice commands for actionable elements
     */
    private fun generateCommands(
        elements: List<ElementInfo>,
        hierarchy: List<HierarchyNode>,
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

                val typeCode = VUIDGenerator.getTypeCode(element.className)
                val elemHash = HashUtils.generateHash(element.resourceId.ifEmpty { label }, 8)
                val vuid = VUIDGenerator.generate(packageName, typeCode, elemHash)

                GeneratedCommand(
                    phrase = "$actionType $label",  // Full voice phrase: "tap Reset"
                    alternates = listOf(
                        "press $label",
                        "select $label",
                        label  // Just the label also works
                    ),
                    targetVuid = vuid,
                    action = actionType,  // Action type for execution
                    element = element,
                    derivedLabel = label  // The clean label without action prefix
                )
            }
    }

    private fun mergeResults(results: List<ExplorationResult>): ExplorationResult {
        if (results.isEmpty()) {
            return ExplorationResult(
                packageName = "empty",
                timestamp = System.currentTimeMillis(),
                duration = 0,
                totalElements = 0,
                clickableElements = 0,
                scrollableElements = 0,
                elements = emptyList(),
                vuids = emptyList(),
                hierarchy = emptyList(),
                duplicates = emptyList(),
                deduplicationStats = DeduplicationStats(0, 0, 0, emptyList()),
                commands = emptyList(),
                avuOutput = "",
                elementLabels = emptyMap()
            )
        }

        // Merge elementLabels with re-indexed keys
        val mergedLabels = mutableMapOf<Int, String>()
        var offset = 0
        results.forEach { result ->
            result.elementLabels.forEach { (idx, label) ->
                mergedLabels[idx + offset] = label
            }
            offset += result.elements.size
        }

        return ExplorationResult(
            packageName = results.map { it.packageName }.distinct().joinToString(", "),
            timestamp = System.currentTimeMillis(),
            duration = results.sumOf { it.duration },
            totalElements = results.sumOf { it.totalElements },
            clickableElements = results.sumOf { it.clickableElements },
            scrollableElements = results.sumOf { it.scrollableElements },
            elements = results.flatMap { it.elements },
            vuids = results.flatMap { it.vuids },
            hierarchy = results.flatMap { it.hierarchy },
            duplicates = results.flatMap { it.duplicates },
            deduplicationStats = DeduplicationStats(
                totalHashes = results.sumOf { it.deduplicationStats.totalHashes },
                uniqueHashes = results.sumOf { it.deduplicationStats.uniqueHashes },
                duplicateCount = results.sumOf { it.deduplicationStats.duplicateCount },
                duplicateElements = results.flatMap { it.deduplicationStats.duplicateElements }
            ),
            commands = results.flatMap { it.commands },
            avuOutput = results.joinToString("\n---\n") { it.avuOutput },
            elementLabels = mergedLabels
        )
    }
}

// Data classes for exploration results
data class ExplorationResult(
    val packageName: String,
    val timestamp: Long,
    val duration: Long,
    val totalElements: Int,
    val clickableElements: Int,
    val scrollableElements: Int,
    val elements: List<ElementInfo>,
    val vuids: List<VUIDInfo>,
    val hierarchy: List<HierarchyNode>,
    val duplicates: List<DuplicateInfo>,
    val deduplicationStats: DeduplicationStats,
    val commands: List<GeneratedCommand>,
    val avuOutput: String,
    val elementLabels: Map<Int, String> = emptyMap()  // index -> derived label (from self or child)
)

data class VUIDInfo(
    val element: ElementInfo,
    val vuid: String,
    val hash: String
)

data class HierarchyNode(
    val index: Int,
    val depth: Int,
    val parentIndex: Int?,
    val childCount: Int,
    val className: String
)

data class DuplicateInfo(
    val hash: String,
    val element: ElementInfo,
    val firstSeenIndex: Int
)

data class DeduplicationStats(
    val totalHashes: Int,
    val uniqueHashes: Int,
    val duplicateCount: Int,
    val duplicateElements: List<DuplicateInfo>
)

data class GeneratedCommand(
    val phrase: String,
    val alternates: List<String>,
    val targetVuid: String,
    val action: String,
    val element: ElementInfo,
    val derivedLabel: String = ""  // Label derived from child elements if parent has none
)
