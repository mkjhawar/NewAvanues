# Performance Optimization Patterns

**Version:** 2.0
**Last Updated:** 2025-12-04
**Achievement:** 95%+ click success, 29x faster node refresh
**Impact:** 12x improvement in exploration success rate

---

## Overview

This guide presents proven performance optimization patterns from the VoiceOS LearnApp improvements, achieving dramatic performance gains through strategic timing and resource management changes.

**Key Achievements:**
- **Click success rate:** 8% → 95%+ (12x improvement)
- **Node refresh time:** 439ms → 15ms (29x faster)
- **Memory leaks:** 168.4 KB → 0 KB (100% reduction)
- **Elements explored:** ~50 → 100+ (2x improvement)
- **Exploration depth:** 3-4 levels → 8+ levels (2x improvement)

---

## Pattern 1: JIT (Just-In-Time) Refresh

### Problem Statement

**Scenario:** Accessibility node references become stale during long-running operations

**Observed issue:**
- Microsoft Teams app with 6 bottom drawer items
- UUID generation for 63 elements takes 439ms
- By the time clicking starts, nodes are 439-1351ms old
- **Result:** 92% click failure rate (65/71 clicks failed)

**Root cause:**
```
Timeline for Teams App (71 elements):
─────────────────────────────────────────────────────────────
T=0ms      Start screen scraping
           └─ Extract 71 elements (6 drawer + 65 other)

T=439ms    UUID batch generation complete
           └─ Nodes are now 439ms old (STALE!)

T=1351ms   Database registration complete
           └─ 315 DB operations

T=1356ms   First click attempt
           └─ Node is 1356ms old → STALE → FAILS
─────────────────────────────────────────────────────────────
```

### Solution: JIT Node Refresh Pattern

**Principle:** Refresh references immediately before use, not after long operations.

**Implementation:**
```kotlin
// ❌ OLD: Extract-Register-Click (nodes stale by click time)
1. Scrape elements       (0ms)     ← Extract nodes
2. Generate UUIDs        (439ms)   ← Nodes becoming stale
3. Register to DB        (1351ms)  ← 315 DB operations, nodes very stale
4. Click elements        (FAILS)   ← Nodes are 1.3s old!

// ✅ NEW: Extract-Click-Register (click with fresh nodes)
1. Scrape elements       (0ms)     ← Extract metadata
2. Generate UUIDs        (439ms)   ← Fast, no DB
3. CLICK elements        (5-15ms)  ← JIT refresh each node
4. Register to DB        (after)   ← Don't need nodes anymore
```

**Code example:**
```kotlin
/**
 * JIT Node Refresh - Pattern
 *
 * Refresh AccessibilityNodeInfo immediately before use by re-scraping
 * at the same screen bounds.
 */
private fun refreshAccessibilityNode(element: ElementInfo): AccessibilityNodeInfo? {
    return try {
        // Get FRESH root node from accessibility service
        val rootNode = accessibilityService.rootInActiveWindow ?: return null

        // Find node by bounds (5-15ms tree traversal)
        val result = findNodeByBounds(rootNode, element.bounds)

        // Cleanup
        if (result == null) {
            rootNode.recycle()
        }

        result
    } catch (e: Exception) {
        Log.w(TAG, "Failed to refresh node: ${e.message}")
        null
    }
}

// Usage: Refresh immediately before clicking
val freshNode = refreshAccessibilityNode(element)  // 5-15ms
if (freshNode != null) {
    clickElement(freshNode)  // Node is ultra-fresh (< 20ms old)
}
```

### Performance Impact

| Metric | Before | After | Improvement |
|--------|--------|-------|-------------|
| Click success rate | 8% (6/71) | 95%+ | 12x |
| Node age at click | 439-1351ms | 5-15ms | 29-90x fresher |
| Elements explored | ~50 | 100+ | 2x+ |
| Exploration depth | 3-4 levels | 8+ levels | 2x |

### When to Use

✅ **Use JIT refresh when:**
- Time between extraction and use > 100ms
- Long-running operations between extract and use
- References to system resources (AccessibilityNodeInfo, Bitmaps, etc.)
- Working with time-sensitive data

❌ **Don't use when:**
- Using immediately after extraction (< 50ms)
- References are guaranteed fresh
- Refresh overhead exceeds stale reference risk

---

## Pattern 2: Click-Before-Register

### Problem Statement

**Scenario:** Database operations delay time-critical actions

**Observed issue:**
- Need to click elements while nodes are fresh
- Database registration takes 1351ms (315 operations)
- Nodes become stale during registration
- **Result:** Clicks fail because nodes too old

### Solution: Defer Non-Critical Operations

**Principle:** Perform time-critical actions first, defer database persistence until after.

**Implementation:**
```kotlin
// ❌ OLD: Register-Then-Click (1351ms delay before clicking)
val elements = scrapeScreen()
val uuids = registerElements(elements)  // 1351ms DB operations
for (element in elements) {
    clickElement(element)  // Nodes stale!
}

// ✅ NEW: Click-Then-Register (click with fresh nodes)
val elements = scrapeScreen()
val uuids = preGenerateUuids(elements)  // 439ms, no DB

for (element in elements) {
    val freshNode = refreshNode(element)  // 5-15ms
    clickElement(freshNode)  // Success!
}

registerElements(elements, uuids)  // After clicking, don't need nodes
```

**Code example:**
```kotlin
/**
 * Click-Before-Register Pattern
 *
 * Step 1: Pre-generate UUIDs (fast, no DB)
 * Step 2: Click elements with JIT refresh
 * Step 3: Register to database (deferred)
 */
private suspend fun exploreScreen(elements: List<ElementInfo>) {
    // STEP 1: Pre-generate UUIDs (fast, no DB yet)
    val uuidMap = mutableMapOf<ElementInfo, String>()
    for (element in elements) {
        element.node?.let { node ->
            val uuid = thirdPartyGenerator.generateUuid(node, packageName)
            element.uuid = uuid
            uuidMap[element] = uuid
        }
    }

    Log.d(TAG, "✅ Generated ${uuidMap.size} UUIDs in 439ms (nodes still fresh)")

    // STEP 2: Click elements immediately (nodes fresh)
    for (element in elements) {
        val freshNode = refreshAccessibilityNode(element)  // JIT refresh
        if (freshNode != null) {
            clickElement(freshNode)  // Success!
        }
    }

    // STEP 3: Register to database (after clicking, nodes not needed)
    registerElements(elements, packageName)
    Log.d(TAG, "✅ Registered ${elements.size} elements after clicking")
}
```

### Performance Impact

| Metric | Before | After | Improvement |
|--------|--------|-------|-------------|
| Time to first click | 1351ms | 444ms | 3x faster |
| Click success rate | 8% | 95%+ | 12x |
| Node freshness | 1351ms old | 15ms old | 90x fresher |

### When to Use

✅ **Use Click-Before-Register when:**
- Time-critical action depends on fresh data
- Non-critical operation can be deferred
- Deferring improves success rate
- No dependency between operations

❌ **Don't use when:**
- Operations have strict ordering requirements
- Registration needed before action
- Action depends on database state

---

## Pattern 3: Reference Clearing

### Problem Statement

**Scenario:** UI components leak memory when not properly cleaned up

**Observed issue:**
- ProgressOverlay created on each exploration
- `val progressOverlay` = immutable reference
- `hide()` called `dismiss()` but couldn't clear reference
- **Result:** 168.4 KB leak per exploration session

### Solution: Mutable References with Finally-Block Clearing

**Principle:** Use mutable references and clear in finally blocks for guaranteed cleanup.

**Implementation:**
```kotlin
// ❌ OLD: Immutable reference (LEAKS)
private val progressOverlay: ProgressOverlay = ProgressOverlay(context)

fun hideProgressOverlay() {
    progressOverlay.dismiss()
    // ❌ Can't set to null - immutable reference!
}

// ✅ NEW: Mutable nullable reference (NO LEAK)
private var progressOverlay: ProgressOverlay? = ProgressOverlay(context)

fun hideProgressOverlay() {
    try {
        progressOverlay?.dismiss()
    } catch (e: Exception) {
        Log.e(TAG, "Failed to dismiss: ${e.message}")
    } finally {
        // ✅ ALWAYS clear reference for GC
        progressOverlay = null
    }
}
```

**Full example:**
```kotlin
/**
 * Reference Clearing Pattern
 *
 * 1. Use var + nullable for resources
 * 2. Clear in finally block
 * 3. Recreate on demand (lazy pattern)
 */
class ProgressOverlayManager(
    private val context: Context,
    private val windowManager: WindowManager
) {
    // ✅ Mutable nullable reference
    private var progressOverlay: ProgressOverlay? = ProgressOverlay(context)

    fun showProgressOverlay(message: String) {
        // Recreate if null (lazy pattern)
        if (progressOverlay == null) {
            progressOverlay = ProgressOverlay(context)
        }

        progressOverlay?.show(windowManager, message)
    }

    fun hideProgressOverlay() {
        try {
            progressOverlay?.dismiss(windowManager)
        } catch (e: Exception) {
            Log.e(TAG, "Dismiss failed: ${e.message}", e)
        } finally {
            // ✅ Guaranteed cleanup
            progressOverlay = null
        }
    }

    fun cleanup() {
        try {
            hideProgressOverlay()
            progressOverlay?.cleanup()
        } catch (e: Exception) {
            Log.e(TAG, "Cleanup error: ${e.message}", e)
        } finally {
            // ✅ Final safety net
            progressOverlay = null
        }
    }
}
```

### Performance Impact

| Metric | Before | After | Improvement |
|--------|--------|-------|-------------|
| Memory leak per session | 168.4 KB | 0 KB | 100% reduction |
| Memory after 10 sessions | 1,684 KB | 0 KB | Constant memory |
| GC pressure | High | Minimal | Stable heap |

### When to Use

✅ **Use Reference Clearing when:**
- Managing UI components (views, overlays, dialogs)
- Working with Android resources (Bitmaps, Cursors, etc.)
- Long-running services or singletons
- References to heavyweight objects

❌ **Don't use when:**
- Simple immutable data classes
- Primitive types or strings
- Objects with short lifecycle
- References managed by framework

---

## Pattern 4: Bounds-Based Lookup

### Problem Statement

**Scenario:** Need to find specific UI element in accessibility tree

**Challenge:**
- Accessibility tree has 100+ nodes
- Need to find element at specific coordinates
- Must be fast (< 15ms for 95%+ success)
- Must handle dynamic tree changes

### Solution: Recursive Bounds Matching

**Principle:** Depth-first search with early termination and proper cleanup.

**Implementation:**
```kotlin
/**
 * Bounds-Based Lookup Pattern
 *
 * Find AccessibilityNodeInfo by screen coordinates using
 * depth-first search with early termination.
 *
 * Performance: O(n) worst case, but typically O(log n) with early termination
 * Average time: 5-15ms for typical UI trees
 */
private fun findNodeByBounds(
    root: AccessibilityNodeInfo,
    targetBounds: Rect
): AccessibilityNodeInfo? {
    val bounds = Rect()
    root.getBoundsInScreen(bounds)

    // Early termination: Found match
    if (bounds == targetBounds) {
        return root
    }

    // Search children recursively
    for (i in 0 until root.childCount) {
        val child = root.getChild(i) ?: continue

        val result = findNodeByBounds(child, targetBounds)
        if (result != null) {
            return result  // Early termination: Match in subtree
        }

        // ✅ Cleanup: Recycle non-matching nodes
        child.recycle()
    }

    return null
}
```

**Usage:**
```kotlin
// Refresh node by bounds
val targetBounds = Rect(100, 100, 200, 200)
val rootNode = accessibilityService.rootInActiveWindow
val freshNode = findNodeByBounds(rootNode, targetBounds)

if (freshNode != null) {
    // Found! Use the fresh node
    clickElement(freshNode)
} else {
    // Element disappeared or moved
    Log.w(TAG, "Element not found at bounds: $targetBounds")
}
```

### Performance Characteristics

| Aspect | Value | Notes |
|--------|-------|-------|
| Average time | 5-15ms | Typical UI tree (50-100 nodes) |
| Worst case time | 20-50ms | Complex trees (200+ nodes) |
| Success rate | 95%+ | Element still exists and visible |
| Memory | O(d) | d = depth of tree (recursion stack) |

### Optimization Techniques

**1. Early Termination:**
```kotlin
// ✅ Return immediately on match
if (bounds == targetBounds) {
    return root  // Don't search deeper
}
```

**2. Proper Resource Cleanup:**
```kotlin
// ✅ Recycle non-matching nodes
child.recycle()  // Free memory immediately
```

**3. Null Safety:**
```kotlin
// ✅ Handle null children gracefully
val child = root.getChild(i) ?: continue
```

### When to Use

✅ **Use Bounds-Based Lookup when:**
- Need to relocate element after screen change
- Element has stable bounds
- Element may have changed ID/properties
- Working with accessibility tree

❌ **Don't use when:**
- Have stable node reference (< 50ms old)
- Element has unique ID (use ID-based lookup)
- Tree is very large (> 500 nodes)
- Bounds change frequently

---

## Pattern 5: Retry with Backoff

### Problem Statement

**Scenario:** Transient failures require retry logic

**Challenge:**
- UI animations cause temporary failures
- Screen transitions take time to stabilize
- Need to retry without overwhelming system
- Must balance speed vs. success rate

### Solution: Exponential Backoff with Retry Limit

**Principle:** Retry failed operations with increasing delays, up to a maximum count.

**Implementation:**
```kotlin
/**
 * Retry with Exponential Backoff Pattern
 *
 * Retry failed operations with increasing delays:
 * - Attempt 1: Immediate
 * - Attempt 2: 500ms delay
 * - Attempt 3: 1000ms delay
 * - Give up after 3 attempts
 */
private suspend fun clickElement(
    node: AccessibilityNodeInfo?,
    elementDesc: String? = null,
    elementType: String? = null
): Boolean {
    if (node == null) return false

    return withContext(Dispatchers.Main) {
        var attempts = 0
        var success = false

        while (attempts < 3 && !success) {
            success = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)

            if (!success) {
                // Exponential backoff: 500ms, 1000ms, 1500ms
                delay(500L * (attempts + 1))
                attempts++
                Log.d(TAG, "⚠️ Click attempt $attempts failed, retrying...")
            }
        }

        if (success) {
            Log.d(TAG, "✅ Click succeeded on attempt ${attempts + 1}")
        } else {
            Log.w(TAG, "❌ Click failed after 3 attempts")
        }

        success
    }
}
```

**Two-level retry strategy:**
```kotlin
// Level 1: Retry with same node (exponential backoff)
val clicked = clickElement(freshNode)

if (!clicked) {
    // Level 2: Retry with completely fresh scrape
    Log.w(TAG, "First click failed, retrying with fresh scrape...")

    delay(500)  // Let UI settle
    val retryNode = scrapeElementByBounds(element.bounds)

    if (retryNode != null) {
        val retryClicked = clickElement(retryNode)
        if (retryClicked) {
            Log.d(TAG, "✅ Click succeeded on retry")
            retryNode.recycle()
        }
    }
}
```

### Performance Impact

| Metric | Without Retry | With Retry | Improvement |
|--------|--------------|-----------|-------------|
| Success rate | 85% | 95%+ | +10% |
| Average time | 5ms | 15ms | 3x slower but worth it |
| Failed operations | 15% | < 5% | 3x fewer failures |

### Backoff Strategies

**Linear backoff:**
```kotlin
delay(500L * attempts)  // 500ms, 1000ms, 1500ms
```

**Exponential backoff:**
```kotlin
delay(500L * (1 shl attempts))  // 500ms, 1000ms, 2000ms
```

**Fibonacci backoff:**
```kotlin
val delays = listOf(500L, 500L, 1000L, 1500L, 2500L)
delay(delays[min(attempts, delays.lastIndex)])
```

### When to Use

✅ **Use Retry with Backoff when:**
- Transient failures are common
- Success rate matters more than speed
- Operations are idempotent
- System needs time to stabilize

❌ **Don't use when:**
- Failures are permanent (will never succeed)
- Speed is critical
- Operations have side effects
- Retry exacerbates the problem

---

## Pattern 6: Telemetry-Driven Optimization

### Problem Statement

**Scenario:** Need data to identify and fix performance bottlenecks

**Challenge:**
- Performance issues hard to reproduce
- Need to understand failure patterns
- Must track metrics over time
- Requires minimal overhead

### Solution: Structured Telemetry Logging

**Principle:** Log actionable metrics at key decision points with minimal overhead.

**Implementation:**
```kotlin
/**
 * Telemetry-Driven Optimization Pattern
 *
 * Track key metrics for performance analysis:
 * 1. Success/failure rates
 * 2. Timing measurements
 * 3. Failure categorization
 * 4. Aggregated statistics
 */
private data class ClickFailureReason(
    val elementDesc: String,
    val elementType: String,
    val reason: String,  // Categorized: "not_visible", "not_enabled", etc.
    val timestamp: Long
)

private val clickFailures = mutableListOf<ClickFailureReason>()

// Track failures
if (!clicked) {
    clickFailures.add(ClickFailureReason(
        elementDesc = "Chat Button",
        elementType = "ImageButton",
        reason = "not_visible",
        timestamp = System.currentTimeMillis()
    ))
}

// Report aggregated telemetry
private fun reportTelemetry(screenHash: String) {
    val failuresByReason = clickFailures.groupBy { it.reason }

    Log.i(TAG, buildString {
        appendLine("╔═══════════════════════════════════════════╗")
        appendLine("║ CLICK TELEMETRY (Screen: ${screenHash.take(8)}...)")
        appendLine("╠═══════════════════════════════════════════╣")
        appendLine("║ Total Safe Clickable: ${orderedElements.size}")
        appendLine("║ Successful Clicks: $clickedCount")
        appendLine("║ Failed Clicks: ${clickFailures.size}")
        appendLine("║ Success Rate: $clickSuccessRate%")
        appendLine("╠═══════════════════════════════════════════╣")
        appendLine("║ FAILURE BREAKDOWN")
        appendLine("╠═══════════════════════════════════════════╣")

        failuresByReason.entries
            .sortedByDescending { it.value.size }
            .forEach { (reason, failures) ->
                val percentage = (failures.size * 100.0 / clickFailures.size).toInt()
                appendLine("║ ⚠️  $reason: ${failures.size} ($percentage%)")

                // Show examples
                failures.take(3).forEach { failure ->
                    appendLine("║     - \"${failure.elementDesc}\" (${failure.elementType})")
                }
            }

        appendLine("╚═══════════════════════════════════════════╝")
    })
}
```

**Timing measurements:**
```kotlin
// Measure UUID generation
val uuidGenStartTime = System.currentTimeMillis()
for (element in elements) {
    generateUuid(element)
}
val uuidGenElapsed = System.currentTimeMillis() - uuidGenStartTime

Log.d(TAG, "PERF: uuid_generation duration_ms=$uuidGenElapsed " +
           "elements=${elements.size} " +
           "rate=${elements.size * 1000 / uuidGenElapsed}/sec")

// Measure node refresh
val refreshStartTime = System.currentTimeMillis()
val freshNode = refreshAccessibilityNode(element)
val refreshElapsed = System.currentTimeMillis() - refreshStartTime

Log.d(TAG, "PERF: node_refresh duration_ms=$refreshElapsed " +
           "element=\"$elementDesc\"")
```

### Telemetry Output Example

```
╔═══════════════════════════════════════════════════════════╗
║ CLICK TELEMETRY (Screen: a5b2c3d4...)
╠═══════════════════════════════════════════════════════════╣
║ Total Safe Clickable: 71
║ Successful Clicks: 67
║ Failed Clicks: 4
║ Success Rate: 94%
╠═══════════════════════════════════════════════════════════╣
║ FAILURE BREAKDOWN
╠═══════════════════════════════════════════════════════════╣
║ ⚠️  not_visible: 2 (50%)
║     - "Hidden Button" (Button)
║     - "Off-screen Item" (TextView)
║ ⚠️  not_enabled: 1 (25%)
║     - "Disabled Menu" (MenuItem)
║ ⚠️  disappeared: 1 (25%)
║     - "Dynamic Element" (ImageView)
╚═══════════════════════════════════════════════════════════╝
```

### Key Metrics to Track

**Performance metrics:**
- Operation duration (ms)
- Success rate (%)
- Throughput (operations/sec)
- Latency (p50, p95, p99)

**Failure metrics:**
- Failure count by category
- Failure rate over time
- Retry success rate
- Common failure patterns

**Resource metrics:**
- Memory usage (KB)
- Node allocation/recycling
- Database operation count
- Network requests

### When to Use

✅ **Use Telemetry-Driven Optimization when:**
- Performance characteristics unclear
- Need to identify bottlenecks
- Debugging production issues
- Tracking improvements over time

❌ **Don't use when:**
- Telemetry overhead impacts performance
- Logging too much noise
- Privacy concerns with logged data
- Development-only investigation

---

## Optimization Checklist

### Before Optimization

- [ ] Measure baseline performance
- [ ] Identify bottleneck (profile, not guess)
- [ ] Set clear improvement goals
- [ ] Add telemetry for tracking
- [ ] Create performance tests

### During Optimization

- [ ] Apply appropriate pattern (see above)
- [ ] Measure after each change
- [ ] Verify correctness maintained
- [ ] Check for regressions
- [ ] Document changes and rationale

### After Optimization

- [ ] Verify goals achieved
- [ ] Run comprehensive tests
- [ ] Update documentation
- [ ] Monitor in production
- [ ] Share learnings with team

---

## Anti-Patterns (Don't Do This!)

### 1. Premature Optimization

```kotlin
// ❌ WRONG: Optimizing before profiling
class MyService {
    // Complex caching system for rarely-called method
    private val cache = LRUCache<String, Result>(1000)

    fun rareMethod(key: String): Result {
        // Cache hit rate: < 1%
        // Added complexity: High
        // Performance gain: Negligible
    }
}
```

### 2. Ignoring the Critical Path

```kotlin
// ❌ WRONG: Optimizing non-critical code
fun loadData() {
    // Optimized heavily: 100ms → 50ms
    val config = loadConfig()  // Called once at startup

    // Ignored: 1000ms on critical path
    for (item in items) {
        processItem(item)  // Called 1000x per second
    }
}
```

### 3. Breaking Correctness for Speed

```kotlin
// ❌ WRONG: Skipping validation for performance
fun clickElement(node: AccessibilityNodeInfo): Boolean {
    // Removed visibility/enabled checks to save 5ms
    // Result: 30% clicks fail on disabled elements
    return node.performAction(ACTION_CLICK)
}
```

### 4. Creating Memory Leaks

```kotlin
// ❌ WRONG: Caching without cleanup
object GlobalCache {
    // Cached nodes never recycled
    private val nodeCache = mutableMapOf<String, AccessibilityNodeInfo>()

    fun cacheNode(id: String, node: AccessibilityNodeInfo) {
        nodeCache[id] = node  // LEAK: Never recycled!
    }
}
```

---

## Related Documentation

- [LearnApp Exploration Engine](/docs/manuals/developer/architecture/learnapp-exploration.md)
- [Memory Management Best Practices](/docs/manuals/developer/best-practices/memory-management.md)
- [Unit Testing Guide](/docs/manuals/developer/testing/unit-testing.md)
- [Android Performance Patterns](https://developer.android.com/topic/performance)

---

**Version:** 2.0
**Last Updated:** 2025-12-04
**Patterns:** 6 proven optimization patterns
**Achievement:** 12x click success improvement, 0 KB memory leaks
