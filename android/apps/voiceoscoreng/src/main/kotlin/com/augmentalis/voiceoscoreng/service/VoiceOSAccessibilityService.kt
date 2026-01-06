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

        // Generate simple AVU output
        val avuOutput = generateSimpleAVU(packageName, elements)

        // Generate commands
        val commands = generateCommands(elements, packageName)

        val duration = System.currentTimeMillis() - startTime

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
                totalHashes = seenHashes.size,
                uniqueHashes = seenHashes.size - duplicates.size,
                duplicateCount = duplicates.size,
                duplicateElements = duplicates
            ),
            commands = commands,
            avuOutput = avuOutput
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

        // Generate hash for deduplication
        val hashInput = "${element.className}|${element.resourceId}|${element.text}|${element.bounds}"
        val hash = HashUtils.generateHash(hashInput, 16)

        if (seenHashes.contains(hash)) {
            duplicates.add(DuplicateInfo(
                hash = hash,
                element = element,
                firstSeenIndex = elements.indexOfFirst { e ->
                    val h = HashUtils.generateHash("${e.className}|${e.resourceId}|${e.text}|${e.bounds}", 16)
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

    private fun generateSimpleAVU(packageName: String, elements: List<ElementInfo>): String {
        return buildString {
            appendLine("# Avanues Universal Format v1.0")
            appendLine("# Package: $packageName")
            appendLine("# Elements: ${elements.size}")
            appendLine()
            appendLine("schema: avu-1.0")
            appendLine("version: 1.0.0")
            appendLine("package: $packageName")
            appendLine()
            appendLine("@elements:")

            elements.forEachIndexed { index, element ->
                val typeCode = VUIDGenerator.getTypeCode(element.className)
                val label = element.voiceLabel.take(30)
                val actionable = if (element.isClickable) "T" else "F"

                appendLine("  - idx:$index type:${typeCode.abbrev} label:\"$label\" act:$actionable")
            }

            appendLine()
            appendLine("@commands:")
            elements.filter { it.isClickable }.take(20).forEach { element ->
                val label = element.voiceLabel
                if (label.isNotBlank()) {
                    appendLine("  - \"tap $label\" -> click")
                }
            }
        }
    }

    private fun generateCommands(elements: List<ElementInfo>, packageName: String): List<GeneratedCommand> {
        return elements
            .filter { it.isClickable || it.isScrollable }
            .mapNotNull { element ->
                val label = element.voiceLabel
                if (label.isBlank()) return@mapNotNull null

                val actionType = when {
                    element.isClickable && element.className.contains("Button") -> "tap"
                    element.isClickable && element.className.contains("EditText") -> "focus"
                    element.isClickable -> "click"
                    element.isScrollable -> "scroll"
                    else -> "interact"
                }

                val typeCode = VUIDGenerator.getTypeCode(element.className)
                val elemHash = HashUtils.generateHash(element.resourceId.ifEmpty { element.text }, 8)
                val vuid = VUIDGenerator.generate(packageName, typeCode, elemHash)

                GeneratedCommand(
                    phrase = "$actionType $label",
                    alternates = listOf(
                        "press $label",
                        "select $label",
                        label
                    ),
                    targetVuid = vuid,
                    action = actionType,
                    element = element
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
                avuOutput = ""
            )
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
            avuOutput = results.joinToString("\n---\n") { it.avuOutput }
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
    val avuOutput: String
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
    val element: ElementInfo
)
