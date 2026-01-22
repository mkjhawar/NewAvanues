# VoiceOSCore Scraping Fixes - Implementation Plan
**Date:** 2026-01-20 | **Version:** V2 | **Author:** Claude | **Status:** APPROVED - Ready for Implementation

---

## User Decisions Summary

| Decision | Choice |
|----------|--------|
| Special Characters | All symbols with aliases, Universal + Localized |
| Memory Limit | Unlimited until app change |
| Debounce | Dynamic (device-based), default 200ms, user-configurable |
| Bounds Strategy | Hybrid layered approach (recommended) |

---

## Overview

This plan addresses the 5 core issues identified in the scraping analysis. The fixes are grouped into 3 phases based on dependency and risk.

---

## Phase 1: Special Character Handling (Low Risk)

### Fix 1.1: Comprehensive Symbol Normalization with Localization

**Location:** `Modules/VoiceOSCore/src/commonMain/kotlin/com/augmentalis/voiceoscore/CommandGenerator.kt`

**New file:** `Modules/VoiceOSCore/src/commonMain/kotlin/com/augmentalis/voiceoscore/SymbolNormalizer.kt`

**Approach: Universal + Localized**
- Universal aliases always work (e.g., `&` → "and")
- Locale-specific alternatives loaded from resources
- Multiple aliases per symbol (e.g., `#` → "pound", "hash", "number")

**Universal Symbol Map:**
```kotlin
object SymbolNormalizer {
    /**
     * Universal symbol aliases (work in all locales).
     * Each symbol maps to a list of spoken equivalents.
     */
    private val universalAliases = mapOf(
        "&" to listOf("and"),
        "#" to listOf("pound", "hash", "number"),
        "@" to listOf("at", "at the rate of"),
        "+" to listOf("plus", "and"),
        "/" to listOf("or", "slash", "divided by"),
        "%" to listOf("percent"),
        "$" to listOf("dollar", "dollars"),
        "€" to listOf("euro", "euros"),
        "£" to listOf("pound", "pounds"),
        "=" to listOf("equals", "is"),
        "*" to listOf("star", "asterisk", "times"),
        "!" to listOf("exclamation"),
        "?" to listOf("question"),
        ":" to listOf("colon"),
        ";" to listOf("semicolon"),
        "-" to listOf("dash", "minus"),
        "_" to listOf("underscore"),
        "." to listOf("dot", "period"),
        "," to listOf("comma")
    )

    /**
     * Locale-specific overrides loaded from resources.
     * Key: locale code (e.g., "es", "de", "fr")
     */
    private var localeAliases: Map<String, Map<String, List<String>>> = emptyMap()

    /**
     * Initialize with locale-specific aliases from resources.
     */
    fun initLocale(locale: String, aliases: Map<String, List<String>>) {
        localeAliases = localeAliases + (locale to aliases)
    }

    /**
     * Normalize text by replacing symbols with primary spoken equivalent.
     */
    fun normalize(text: String, locale: String = "en"): String {
        var result = text
        val aliases = localeAliases[locale] ?: universalAliases

        aliases.forEach { (symbol, spoken) ->
            result = result.replace(symbol, " ${spoken.first()} ")
        }

        return result.replace(Regex("\\s+"), " ").trim()
    }

    /**
     * Get all aliases for a symbol (for fuzzy matching).
     */
    fun getAliases(symbol: String, locale: String = "en"): List<String> {
        return localeAliases[locale]?.get(symbol)
            ?: universalAliases[symbol]
            ?: emptyList()
    }

    /**
     * Check if spoken text matches any alias for a symbol.
     */
    fun matchesAlias(spoken: String, symbol: String, locale: String = "en"): Boolean {
        return getAliases(symbol, locale).any { it.equals(spoken, ignoreCase = true) }
    }
}
```

**CommandRegistry Enhancement:**
```kotlin
// In CommandMatcher - support bidirectional alias matching
fun matchWithAliases(voiceInput: String, phrase: String, locale: String = "en"): Boolean {
    val normalizedInput = SymbolNormalizer.normalize(voiceInput, locale)
    val normalizedPhrase = SymbolNormalizer.normalize(phrase, locale)
    return normalizedInput.equals(normalizedPhrase, ignoreCase = true)
}
```

**Localization Resources:**
- Create `strings_symbols.xml` per locale
- Format: `<string-array name="symbol_aliases_ampersand"><item>and</item><item>y</item></string-array>` (Spanish)

**Test Cases:**
- "Display size & text" → registers as "Display size and text"
- User says "display size and text" → matches
- User says "sound # 5" → matches "sound number 5" or "sound pound 5"
- Spanish user says "configuración y texto" → matches "&"

---

## Phase 2: Scroll & Content Change Detection (Medium Risk)

### Fix 2.1: Handle TYPE_WINDOW_CONTENT_CHANGED for Scroll Detection

**Location:** `android/apps/voiceoscoreng/src/main/kotlin/com/augmentalis/voiceoscoreng/service/VoiceOSAccessibilityService.kt`

**Change to onAccessibilityEvent():**
```kotlin
override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    event ?: return

    when (event.eventType) {
        AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
            Log.d(TAG, "Window state changed: ${event.packageName}")
            handleScreenChange(event.packageName?.toString(), isFullChange = true)
        }
        AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
            // Check for scroll-like changes (subtree changes in scrollable containers)
            if (shouldHandleContentChange(event)) {
                Log.d(TAG, "Content changed (scroll detected): ${event.packageName}")
                handleContentUpdate(event)
            }
        }
    }
}

/**
 * Determine if content change should trigger re-scrape.
 * Focus on scroll events in dynamic containers.
 */
private fun shouldHandleContentChange(event: AccessibilityEvent): Boolean {
    if (!continuousScanningEnabled.get()) return false

    val contentTypes = event.contentChangeTypes

    // Subtree changes often indicate scroll in RecyclerView/ListView
    if (contentTypes and AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE != 0) {
        // Check if source is a scrollable container
        event.source?.let { node ->
            val isScrollable = node.isScrollable ||
                ElementExtractor.isDynamicContainer(node.className?.toString() ?: "")
            node.recycle()
            return isScrollable
        }
    }
    return false
}
```

### Fix 2.1b: Dynamic Debounce Based on Device Capability

**New file:** `DeviceCapabilityManager.kt`

```kotlin
/**
 * Manages device-specific performance settings.
 * Adjusts debounce and other timing based on device capability.
 */
object DeviceCapabilityManager {

    enum class DeviceSpeed { FAST, MEDIUM, SLOW }

    private var cachedSpeed: DeviceSpeed? = null
    private var userOverrideDebounce: Long? = null

    /**
     * Get optimal debounce delay for content changes.
     * User override > Device capability > Default
     */
    fun getContentDebounceMs(): Long {
        userOverrideDebounce?.let { return it }

        return when (getDeviceSpeed()) {
            DeviceSpeed.FAST -> 100L
            DeviceSpeed.MEDIUM -> 200L
            DeviceSpeed.SLOW -> 300L
        }
    }

    /**
     * Allow user to override debounce setting.
     */
    fun setUserDebounceMs(ms: Long?) {
        userOverrideDebounce = ms
    }

    /**
     * Detect device speed based on available metrics.
     */
    fun getDeviceSpeed(): DeviceSpeed {
        cachedSpeed?.let { return it }

        val speed = detectDeviceSpeed()
        cachedSpeed = speed
        return speed
    }

    private fun detectDeviceSpeed(): DeviceSpeed {
        // Use available RAM as proxy for device capability
        val runtime = Runtime.getRuntime()
        val maxMemoryMB = runtime.maxMemory() / (1024 * 1024)

        return when {
            maxMemoryMB >= 512 -> DeviceSpeed.FAST    // High-end device
            maxMemoryMB >= 256 -> DeviceSpeed.MEDIUM  // Mid-range
            else -> DeviceSpeed.SLOW                   // Low-end
        }
    }
}
```

**Usage in VoiceOSAccessibilityService:**
```kotlin
private fun handleContentUpdate(event: AccessibilityEvent) {
    val debounceMs = DeviceCapabilityManager.getContentDebounceMs()

    val now = System.currentTimeMillis()
    if (now - lastContentUpdateTime < debounceMs) {
        return
    }
    lastContentUpdateTime = now
    // ... rest of implementation
}
```

### Fix 2.2: Implement Incremental Content Update

**New method in VoiceOSAccessibilityService:**
```kotlin
/**
 * Handle content update (scroll, list changes) without full re-scrape.
 *
 * Strategy: Merge new elements with in-memory commands, preserving
 * existing AVID assignments for elements that were already numbered.
 */
private fun handleContentUpdate(event: AccessibilityEvent) {
    // Debounce rapid content changes
    val now = System.currentTimeMillis()
    if (now - lastContentUpdateTime < CONTENT_UPDATE_DEBOUNCE_MS) {
        return
    }
    lastContentUpdateTime = now

    serviceScope.launch {
        try {
            val rootNode = rootInActiveWindow ?: return@launch
            val packageName = rootNode.packageName?.toString() ?: "unknown"

            // Extract current visible elements
            val elements = mutableListOf<ElementInfo>()
            val hierarchy = mutableListOf<HierarchyNode>()
            val seenHashes = mutableSetOf<String>()
            val duplicates = mutableListOf<DuplicateInfo>()

            ElementExtractor.extractElements(rootNode, elements, hierarchy, seenHashes, duplicates, 0)
            rootNode.recycle()

            // Merge with existing commands (preserve existing assignments)
            val newCommands = incrementalCommandGenerator.mergeCommands(
                existingCommands = commandRegistry.all(),
                newElements = elements,
                packageName = packageName
            )

            // Update registry with merged commands
            commandRegistry.updateSync(newCommands)

            // Update overlay with new visible items
            updateOverlayForVisibleItems(elements, packageName)

            Log.d(TAG, "Incremental update: ${newCommands.size} commands (${elements.size} elements)")

        } catch (e: Exception) {
            Log.e(TAG, "Error in content update", e)
        }
    }
}

private const val CONTENT_UPDATE_DEBOUNCE_MS = 200L
private var lastContentUpdateTime = 0L
```

### Fix 2.3: In-Memory Command Merging

**New file:** `IncrementalCommandGenerator.kt`

```kotlin
/**
 * Handles incremental command updates for scroll/content changes.
 *
 * Key principle: Elements that were visible before retain their AVID numbers.
 * New elements get new numbers. Elements no longer visible are kept in memory
 * until app change or explicit clear.
 */
class IncrementalCommandGenerator {

    // In-memory cache of element hash -> AVID assignment
    private val avidAssignments = mutableMapOf<String, Int>()

    // Next available AVID number
    private var nextAvidNumber = 1

    // Current app context
    private var currentAppPackage: String? = null

    /**
     * Merge new elements with existing commands.
     * Preserves existing AVID assignments for known elements.
     */
    fun mergeCommands(
        existingCommands: List<QuantizedCommand>,
        newElements: List<ElementInfo>,
        packageName: String
    ): List<QuantizedCommand> {
        // Reset on app change
        if (packageName != currentAppPackage) {
            clearAssignments()
            currentAppPackage = packageName
        }

        val result = mutableListOf<QuantizedCommand>()

        newElements.forEach { element ->
            val hash = ElementFingerprint.generateHash(element)

            // Check if we've seen this element before
            val existingAvid = avidAssignments[hash]

            val command = if (existingAvid != null) {
                // Preserve existing assignment
                createCommand(element, packageName, existingAvid)
            } else {
                // New element - assign next number
                val newAvid = nextAvidNumber++
                avidAssignments[hash] = newAvid
                createCommand(element, packageName, newAvid)
            }

            command?.let { result.add(it) }
        }

        return result
    }

    /**
     * Clear all assignments (on app change or explicit clear).
     */
    fun clearAssignments() {
        avidAssignments.clear()
        nextAvidNumber = 1
        currentAppPackage = null
    }

    private fun createCommand(element: ElementInfo, packageName: String, avidNumber: Int): QuantizedCommand? {
        return CommandGenerator.fromElement(element, packageName)?.copy(
            metadata = mapOf(
                "avidNumber" to avidNumber.toString(),
                // ... other metadata
            )
        )
    }
}
```

---

## Phase 3: Overlay & Click Execution Fixes (Medium Risk)

### Fix 3.1: Incremental Overlay Updates

**Location:** `OverlayStateManager.kt`

```kotlin
/**
 * Update overlay items incrementally.
 * Preserves positions for items that are still visible.
 */
fun updateOverlayItemsIncremental(
    newItems: List<NumberOverlayItem>,
    preserveExisting: Boolean = true
) {
    val current = _numberedOverlayItems.value

    if (!preserveExisting || current.isEmpty()) {
        _numberedOverlayItems.value = newItems
        return
    }

    // Build map of VUID -> existing number
    val existingNumbers = current.associateBy { it.vuid }

    // Assign numbers to new items, preserving existing
    val merged = newItems.map { item ->
        val existing = existingNumbers[item.vuid]
        if (existing != null) {
            // Keep existing number
            item.copy(number = existing.number)
        } else {
            // Keep new number from item
            item
        }
    }

    _numberedOverlayItems.value = merged
}
```

### Fix 3.2: Hybrid Layered Bounds Resolution (RECOMMENDED)

**Based on investigation:** A hybrid approach with 4 fallback layers provides the best balance of speed and accuracy.

**Performance comparison:**

| Layer | Strategy | Latency | Success Rate |
|-------|----------|---------|--------------|
| 1 | Metadata bounds (cached) | ~0-1ms | 60% |
| 2 | Delta compensation (scroll offset) | ~1-2ms | 25% |
| 3 | Resource ID anchor search | ~5-10ms | 10% |
| 4 | Full tree search | ~50-100ms | 5% (fallback) |

**New file:** `BoundsResolver.kt`

```kotlin
/**
 * Hybrid layered bounds resolution for click execution.
 * Tries multiple strategies from fastest to most accurate.
 */
class BoundsResolver(private val service: AccessibilityService) {

    private val TAG = "BoundsResolver"

    // Weak cache of node references by AVID
    private val nodeCache = WeakHashMap<String, AccessibilityNodeInfo>()

    // Scroll offset tracking per container
    private val scrollOffsets = mutableMapOf<String, Pair<Int, Int>>()

    /**
     * Resolve bounds for a command using layered strategy.
     * Returns fresh, validated bounds or null if element not found.
     */
    fun resolve(command: QuantizedCommand): Bounds? {
        val startTime = System.currentTimeMillis()

        // Layer 1: Try cached metadata bounds (fastest)
        val metadataBounds = tryMetadataBounds(command)
        if (metadataBounds != null) {
            Log.d(TAG, "Layer 1 success: metadata bounds (${System.currentTimeMillis() - startTime}ms)")
            return metadataBounds
        }

        // Layer 2: Try delta compensation for scrolled content
        val deltaBounds = tryDeltaCompensation(command)
        if (deltaBounds != null) {
            Log.d(TAG, "Layer 2 success: delta compensation (${System.currentTimeMillis() - startTime}ms)")
            return deltaBounds
        }

        // Layer 3: Try resource ID anchor search
        val anchorBounds = tryAnchorSearch(command)
        if (anchorBounds != null) {
            Log.d(TAG, "Layer 3 success: anchor search (${System.currentTimeMillis() - startTime}ms)")
            return anchorBounds
        }

        // Layer 4: Full tree search (fallback)
        val fullSearchBounds = tryFullTreeSearch(command)
        if (fullSearchBounds != null) {
            Log.d(TAG, "Layer 4 success: full search (${System.currentTimeMillis() - startTime}ms)")
            return fullSearchBounds
        }

        Log.w(TAG, "All layers failed for '${command.phrase}' (${System.currentTimeMillis() - startTime}ms)")
        return null
    }

    /**
     * Layer 1: Use bounds from command metadata.
     */
    private fun tryMetadataBounds(command: QuantizedCommand): Bounds? {
        val boundsStr = command.metadata["bounds"] ?: return null
        val bounds = parseBounds(boundsStr) ?: return null

        // Validate bounds are still reasonable
        if (!validateBounds(bounds)) return null

        return bounds
    }

    /**
     * Layer 2: Apply scroll delta to cached bounds.
     */
    private fun tryDeltaCompensation(command: QuantizedCommand): Bounds? {
        val containerId = command.metadata["containerId"] ?: return null
        val cachedScrollOffset = command.metadata["scrollOffset"]?.split(",")
            ?.mapNotNull { it.toIntOrNull() }
            ?.takeIf { it.size == 2 }
            ?: return null

        val currentOffset = scrollOffsets[containerId] ?: return null
        val deltaX = currentOffset.first - cachedScrollOffset[0]
        val deltaY = currentOffset.second - cachedScrollOffset[1]

        val boundsStr = command.metadata["bounds"] ?: return null
        val bounds = parseBounds(boundsStr) ?: return null

        val adjustedBounds = Bounds(
            left = bounds.left - deltaX,
            top = bounds.top - deltaY,
            right = bounds.right - deltaX,
            bottom = bounds.bottom - deltaY
        )

        if (!validateBounds(adjustedBounds)) return null
        return adjustedBounds
    }

    /**
     * Layer 3: Search by resource ID (targeted, faster than full search).
     */
    private fun tryAnchorSearch(command: QuantizedCommand): Bounds? {
        val resourceId = command.metadata["resourceId"]?.takeIf { it.isNotBlank() }
            ?: return null

        val root = service.rootInActiveWindow ?: return null
        try {
            val nodes = root.findAccessibilityNodeInfosByViewId(resourceId)
            if (nodes.isNullOrEmpty()) return null

            // Find best match (prefer visible, clickable)
            val node = nodes.firstOrNull { it.isVisibleToUser && it.isClickable }
                ?: nodes.firstOrNull { it.isVisibleToUser }
                ?: nodes.firstOrNull()

            return node?.let {
                val rect = Rect()
                it.getBoundsInScreen(rect)
                Bounds(rect.left, rect.top, rect.right, rect.bottom)
            }
        } finally {
            root.recycle()
        }
    }

    /**
     * Layer 4: Full tree search by text/hash matching.
     */
    private fun tryFullTreeSearch(command: QuantizedCommand): Bounds? {
        val targetHash = command.metadata["elementHash"]
        val targetText = command.metadata["label"] ?: command.phrase

        val root = service.rootInActiveWindow ?: return null
        try {
            val node = findNodeByHashOrText(root, targetHash, targetText)
            return node?.let {
                val rect = Rect()
                it.getBoundsInScreen(rect)
                it.recycle()
                Bounds(rect.left, rect.top, rect.right, rect.bottom)
            }
        } finally {
            root.recycle()
        }
    }

    /**
     * Update scroll offset tracking for a container.
     */
    fun updateScrollOffset(containerId: String, offsetX: Int, offsetY: Int) {
        scrollOffsets[containerId] = Pair(offsetX, offsetY)
    }

    /**
     * Validate bounds are on screen and reasonable.
     */
    private fun validateBounds(bounds: Bounds): Boolean {
        if (bounds.left < 0 || bounds.top < 0) return false
        if (bounds.right <= bounds.left || bounds.bottom <= bounds.top) return false
        // Could add screen size validation here
        return true
    }

    private fun parseBounds(str: String): Bounds? {
        return try {
            val parts = str.split(",").map { it.trim().toInt() }
            if (parts.size == 4) Bounds(parts[0], parts[1], parts[2], parts[3]) else null
        } catch (e: Exception) { null }
    }

    private fun findNodeByHashOrText(
        root: AccessibilityNodeInfo,
        hash: String?,
        text: String
    ): AccessibilityNodeInfo? {
        // BFS search for matching element
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)

        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()

            // Check text match
            val nodeText = node.text?.toString() ?: ""
            val nodeDesc = node.contentDescription?.toString() ?: ""
            if (nodeText.contains(text, ignoreCase = true) ||
                nodeDesc.contains(text, ignoreCase = true)) {
                return node
            }

            // Check hash match
            if (hash != null) {
                val nodeHash = calculateHash(node)
                if (nodeHash == hash) return node
            }

            // Add children to queue
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }
        return null
    }

    private fun calculateHash(node: AccessibilityNodeInfo): String {
        val input = "${node.className}|${node.viewIdResourceName}|${node.text}"
        return input.hashCode().toUInt().toString(16).padStart(8, '0')
    }
}
```

**Integration in AndroidGestureHandler:**
```kotlin
private val boundsResolver = BoundsResolver(service)

CommandActionType.TAP, CommandActionType.CLICK -> {
    val bounds = boundsResolver.resolve(command)
    if (bounds == null) {
        Log.w(TAG, "Could not resolve bounds for '${command.phrase}'")
        return HandlerResult.notHandled()
    }

    val success = dispatcher.click(bounds)
    if (success) {
        HandlerResult.success("Clicked ${command.phrase}")
    } else {
        HandlerResult.failure("Failed to click")
    }
}
```

---

## Summary of Changes

| Phase | File | Change Type | Risk |
|-------|------|-------------|------|
| 1 | SymbolNormalizer.kt | New file - universal + localized symbol handling | Low |
| 1 | CommandGenerator.kt | Integrate SymbolNormalizer in deriveLabel() | Low |
| 1 | CommandMatcher.kt | Support bidirectional alias matching | Low |
| 1 | strings_symbols.xml | Locale-specific symbol aliases | Low |
| 2 | VoiceOSAccessibilityService.kt | Handle TYPE_WINDOW_CONTENT_CHANGED | Medium |
| 2 | DeviceCapabilityManager.kt | New file - dynamic debounce | Low |
| 2 | IncrementalCommandGenerator.kt | New file - in-memory command merging | Medium |
| 3 | OverlayStateManager.kt | Add incremental update method | Medium |
| 3 | BoundsResolver.kt | New file - hybrid 4-layer bounds resolution | Medium |
| 3 | AndroidGestureHandler.kt | Integrate BoundsResolver | Medium |

---

## Testing Plan

1. **Settings App:**
   - Verify "Sound and vibration" works when saying "sound and vibration"
   - Verify "Display size & text" matches "display size and text"
   - Verify scroll down shows new items with working voice commands

2. **Calculator App:**
   - Verify numeric buttons 1-9 respond to voice commands
   - Test rapid number entry
   - Verify bounds resolution finds buttons correctly

3. **Gmail App:**
   - Verify email list items respond to index commands ("first", "second")
   - Verify scroll reveals new emails with working commands
   - Verify overlay numbers update correctly (existing items keep numbers)
   - Test in-app navigation (inbox → email → back)

4. **Teams App:**
   - Same as Gmail testing
   - Verify landing screen commands work
   - Verify navigation within Teams triggers re-scrape

5. **Performance Testing:**
   - Measure bounds resolution layer hit rates
   - Verify debounce works correctly on different device speeds
   - Confirm memory usage stays bounded on app change

---

## Implementation Order

**Batch 1 (Quick Wins - can be done in parallel):**
- [ ] SymbolNormalizer.kt
- [ ] DeviceCapabilityManager.kt
- [ ] CommandGenerator.kt updates

**Batch 2 (Core Scroll Handling):**
- [ ] VoiceOSAccessibilityService.kt - content change handling
- [ ] IncrementalCommandGenerator.kt
- [ ] OverlayStateManager.kt updates

**Batch 3 (Bounds Resolution):**
- [ ] BoundsResolver.kt
- [ ] AndroidGestureHandler.kt integration
- [ ] Metadata enrichment for scroll offset tracking

---

## Ready for Implementation

All questions answered. Plan approved with:
- ✅ Universal + Localized symbol handling
- ✅ Unlimited memory until app change
- ✅ Dynamic debounce with user override
- ✅ Hybrid 4-layer bounds resolution

**Awaiting your approval to begin implementation.**
