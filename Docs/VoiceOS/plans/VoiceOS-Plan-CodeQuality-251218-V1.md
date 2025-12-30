# VoiceOS Code Quality Fixes Implementation Plan

**Module:** VoiceOS/apps/VoiceOSCore
**Created:** 2025-12-18
**Version:** 1.0.0
**Mode:** .yolo .cot .tot .rot .swarm

---

## Executive Summary

| Metric | Value |
|--------|-------|
| Total Tasks | 12 |
| Phases | 4 |
| Estimated Effort | Medium (4-6 hours) |
| Swarm Recommended | YES (12 tasks, 4 phases) |
| Risk Level | MEDIUM (runtime fixes) |

---

## Reasoning Analysis

### Chain-of-Thought (CoT) - Sequential Dependencies

```
1. mutableStateOf fix → independent, no dependencies
2. runBlocking removal → affects callers, but already done in Phase 1
3. AccessibilityNodeInfo recycle → independent
4. Transaction wrappers → already done in Phase 2
5. IVoiceOSContext interface → affects all handlers (13 files)
6. processQueuedEvents → depends on event queue understanding
7. Bounds checking → independent, maps in VoiceOSService
8. Max retry count → depends on LearnApp init flow
9. VoiceOSService split → MAJOR, depends on 5, 6, 7, 8
10. Cache tree searches → independent per handler
11. coroutine delay → independent, simple replacement
12. TTS announcements → independent per overlay
```

**Critical Path:** IVoiceOSContext (5) → VoiceOSService split (9)

### Tree-of-Thought (ToT) - Alternative Approaches

```
                    ┌─────────────────────────────────────┐
                    │     Fix Code Quality Issues         │
                    └─────────────────┬───────────────────┘
                                      │
          ┌───────────────────────────┼───────────────────────────┐
          ▼                           ▼                           ▼
   ┌──────────────┐          ┌──────────────┐          ┌──────────────┐
   │  Approach A  │          │  Approach B  │          │  Approach C  │
   │ By Priority  │          │ By Component │          │ By Proximity │
   └──────┬───────┘          └──────┬───────┘          └──────┬───────┘
          │                         │                         │
  Risk: HIGH              Risk: MEDIUM              Risk: LOW ✓
  Conflicts: Many         Conflicts: Some           Conflicts: Minimal
  Time: Variable          Time: Medium              Time: Shortest
```

**Selected:** Approach C (By Proximity) - Groups related files to minimize merge conflicts

### Recursive-of-Thought (RoT) - Decomposition

```
Fix Code Quality
├── Phase 1: UI/Compose Fixes (Overlays)
│   ├── Task 1.1: Fix mutableStateOf in CommandStatusOverlay
│   ├── Task 1.2: Fix mutableStateOf in ConfidenceOverlay
│   ├── Task 1.3: Fix mutableStateOf in ContextMenuOverlay
│   ├── Task 1.4: Fix mutableStateOf in NumberedSelectionOverlay
│   └── Task 1.5: Add TTS announcements to overlays
├── Phase 2: Threading/Coroutine Fixes
│   ├── Task 2.1: Replace Thread.sleep() with delay() in UIHandler
│   ├── Task 2.2: Replace Thread.sleep() with delay() in GestureHandler
│   └── Task 2.3: Verify executeAction is suspend (already done)
├── Phase 3: Memory/Resource Fixes
│   ├── Task 3.1: Recycle AccessibilityNodeInfo in NavigationHandler
│   ├── Task 3.2: Cache accessibility tree searches in UIHandler
│   ├── Task 3.3: Add bounds checking to eventCounts map
│   └── Task 3.4: Add bounds checking to metrics map
└── Phase 4: Architecture Fixes
    ├── Task 4.1: Implement processQueuedEvents() in LearnAppIntegration
    ├── Task 4.2: Add max retry count for LearnApp initialization
    ├── Task 4.3: Update IVoiceOSContext with missing methods
    └── Task 4.4: Update handlers to use IVoiceOSContext (13 handlers)
```

---

## Phase 1: UI/Compose Fixes (Overlays)

**Priority:** HIGH
**Risk:** Memory leaks, broken reactivity
**Swarm Agents:** 2

### Task Group 1.1: Fix mutableStateOf Antipattern

| Task | File | Line | Change |
|------|------|------|--------|
| 1.1.1 | CommandStatusOverlay.kt | 71-73 | `= mutableStateOf` → `by mutableStateOf` |
| 1.1.2 | ConfidenceOverlay.kt | 52-54 | `= mutableStateOf` → `by mutableStateOf` |
| 1.1.3 | ContextMenuOverlay.kt | 73-75 | `= mutableStateOf` → `by mutableStateOf` |
| 1.1.4 | NumberedSelectionOverlay.kt | 72 | `= mutableStateOf` → `by mutableStateOf` |

**Before:**
```kotlin
private var commandState = mutableStateOf("")
private var stateState = mutableStateOf(CommandState.LISTENING)
```

**After:**
```kotlin
private var commandState by mutableStateOf("")
private var stateState by mutableStateOf(CommandState.LISTENING)
```

**Also remove `.value` accessors where used.**

### Task Group 1.2: Add TTS Announcements

| Task | File | Change |
|------|------|--------|
| 1.2.1 | CommandStatusOverlay.kt | Add TTS on state change |
| 1.2.2 | NumberedSelectionOverlay.kt | Announce selection count |
| 1.2.3 | ContextMenuOverlay.kt | Announce menu options |

**Pattern:**
```kotlin
private fun announceForAccessibility(message: String) {
    val tts = context.getSystemService(Context.TEXT_TO_SPEECH_SERVICE) as? TextToSpeech
    tts?.speak(message, TextToSpeech.QUEUE_ADD, null, null)
}
```

---

## Phase 2: Threading/Coroutine Fixes

**Priority:** HIGH
**Risk:** ANR, thread blocking
**Swarm Agents:** 1

### Task Group 2.1: Replace Thread.sleep() with delay()

| Task | File | Line | Change |
|------|------|------|--------|
| 2.1.1 | UIHandler.kt | 156 | `Thread.sleep(50)` → `delay(50)` |
| 2.1.2 | GestureHandler.kt | (if any) | Same pattern |

**Before:**
```kotlin
fun someFunction() {
    Thread.sleep(50)  // Blocks thread!
}
```

**After:**
```kotlin
suspend fun someFunction() {
    delay(50)  // Non-blocking
}
```

### Task Group 2.2: Verify runBlocking Removal

| Task | File | Status |
|------|------|--------|
| 2.2.1 | ActionCoordinator.kt | ✅ Already fixed (executeAction is suspend) |
| 2.2.2 | LearnAppDatabaseAdapter.kt | ✅ Already uses Dispatchers.Default |
| 2.2.3 | AccessibilityScrapingIntegration.kt | ✅ Already fixed |

---

## Phase 3: Memory/Resource Fixes

**Priority:** HIGH
**Risk:** Memory leaks, OOM
**Swarm Agents:** 2

### Task Group 3.1: Recycle AccessibilityNodeInfo

| Task | File | Line | Change |
|------|------|------|--------|
| 3.1.1 | NavigationHandler.kt | 117-133 | Add `recycle()` calls |
| 3.1.2 | UIHandler.kt | 218-238 | Add `recycle()` calls |

**Before:**
```kotlin
fun findScrollableNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
    for (i in 0 until node.childCount) {
        val child = node.getChild(i)  // Leaked!
        if (child.isScrollable) return child
        val result = findScrollableNode(child)
        if (result != null) return result
    }
    return null
}
```

**After:**
```kotlin
fun findScrollableNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
    for (i in 0 until node.childCount) {
        val child = node.getChild(i) ?: continue
        try {
            if (child.isScrollable) return child  // Caller responsible for recycling
            val result = findScrollableNode(child)
            if (result != null) {
                child.recycle()  // Recycle since not returning this node
                return result
            }
        } finally {
            if (!child.isScrollable) child.recycle()
        }
    }
    return null
}
```

### Task Group 3.2: Cache Accessibility Tree Searches

| Task | File | Change |
|------|------|--------|
| 3.2.1 | UIHandler.kt | Add LRU cache for findNodeByText results |
| 3.2.2 | NavigationHandler.kt | Cache scrollable nodes per screen |

**Pattern:**
```kotlin
private val nodeCache = object : LruCache<String, WeakReference<AccessibilityNodeInfo>>(50) {
    override fun entryRemoved(evicted: Boolean, key: String, oldValue: WeakReference<AccessibilityNodeInfo>, newValue: WeakReference<AccessibilityNodeInfo>?) {
        oldValue.get()?.recycle()
    }
}

fun clearCacheOnScreenChange() {
    nodeCache.evictAll()
}
```

### Task Group 3.3: Add Bounds Checking to Maps

| Task | File | Line | Change |
|------|------|------|--------|
| 3.3.1 | VoiceOSService.kt | eventCounts | Add size limit (1000 entries) |
| 3.3.2 | ActionCoordinator.kt | metrics | Add size limit (500 entries) |

**Pattern:**
```kotlin
private val eventCounts = object : LinkedHashMap<String, Int>(100, 0.75f, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Int>): Boolean {
        return size > MAX_EVENT_COUNTS  // e.g., 1000
    }
}
```

---

## Phase 4: Architecture Fixes

**Priority:** MEDIUM
**Risk:** API changes, regression
**Swarm Agents:** 2

### Task Group 4.1: Implement processQueuedEvents()

| Task | File | Change |
|------|------|--------|
| 4.1.1 | LearnAppIntegration.kt | Add processQueuedEvents() implementation |

**Implementation:**
```kotlin
private fun processQueuedEvents() {
    if (pendingEvents.isEmpty()) return

    val eventsToProcess = mutableListOf<AccessibilityEvent>()
    while (pendingEvents.isNotEmpty() && eventsToProcess.size < 10) {
        pendingEvents.poll()?.let { eventsToProcess.add(it) }
    }

    eventsToProcess.forEach { event ->
        try {
            onAccessibilityEvent(event)
        } catch (e: Exception) {
            Log.e(TAG, "Error processing queued event", e)
        }
    }
}
```

### Task Group 4.2: Add Max Retry Count

| Task | File | Change |
|------|------|--------|
| 4.2.1 | VoiceOSService.kt | Add retry counter for LearnApp init |

**Pattern:**
```kotlin
private val learnAppInitRetries = AtomicInteger(0)
private const val MAX_LEARNAPP_INIT_RETRIES = 3

private fun initializeLearnAppIntegration() {
    if (learnAppInitRetries.get() >= MAX_LEARNAPP_INIT_RETRIES) {
        Log.e(TAG, "LearnApp initialization failed after $MAX_LEARNAPP_INIT_RETRIES attempts")
        return
    }

    try {
        // existing init code
        learnAppInitRetries.set(0)  // Reset on success
    } catch (e: Exception) {
        learnAppInitRetries.incrementAndGet()
        learnAppInitState.set(0)  // Allow retry
    }
}
```

### Task Group 4.3: Update IVoiceOSContext Interface

| Task | File | Change |
|------|------|--------|
| 4.3.1 | IVoiceOSContext.kt | Add missing methods used by handlers |

**Methods to add:**
```kotlin
interface IVoiceOSContext {
    // Existing
    val context: Context
    val accessibilityService: AccessibilityService
    val windowManager: WindowManager
    val packageManager: PackageManager
    val rootInActiveWindow: AccessibilityNodeInfo?
    fun performGlobalAction(action: Int): Boolean
    fun getAppCommands(): Map<String, String>
    fun getSystemService(name: String): Any?
    fun startActivity(intent: Intent)
    fun showToast(message: String)
    fun vibrate(duration: Long)

    // NEW - Add these
    fun dispatchGesture(gesture: GestureDescription, callback: GestureResultCallback?, handler: Handler?): Boolean
    fun isCursorVisible(): Boolean
    fun getCursorPosition(): Pair<Float, Float>?
    fun getAppCommands(): Map<String, String>
    val serviceInfo: AccessibilityServiceInfo
}
```

### Task Group 4.4: Update Handlers to Use Interface

| Task | Handler | Change |
|------|---------|--------|
| 4.4.1 | SystemHandler.kt | Change `service: VoiceOSService` → `context: IVoiceOSContext` |
| 4.4.2 | AppHandler.kt | Same |
| 4.4.3 | NavigationHandler.kt | Same |
| 4.4.4 | UIHandler.kt | Same |
| 4.4.5 | InputHandler.kt | Same |
| 4.4.6 | GestureHandler.kt | Same |
| 4.4.7 | DeviceHandler.kt | Same |
| 4.4.8 | BluetoothHandler.kt | Same |
| 4.4.9 | DragHandler.kt | Same |
| 4.4.10 | HelpMenuHandler.kt | Same |
| 4.4.11 | SelectHandler.kt | Same |
| 4.4.12 | NumberHandler.kt | Same |

**Note:** VoiceOSService already implements IVoiceOSContext, so this is safe.

---

## Swarm Configuration

### Agent Distribution

| Agent | Phase | Tasks | Files |
|-------|-------|-------|-------|
| Agent 1 | 1.1 | mutableStateOf fixes | 4 files |
| Agent 2 | 1.2 | TTS announcements | 3 files |
| Agent 3 | 2.1 | Thread.sleep replacement | 2 files |
| Agent 4 | 3.1 + 3.2 | AccessibilityNodeInfo + caching | 2 files |
| Agent 5 | 3.3 | Bounds checking | 2 files |
| Agent 6 | 4.1 + 4.2 | processQueuedEvents + retry | 2 files |
| Agent 7 | 4.3 + 4.4 | IVoiceOSContext + handlers | 14 files |

### Execution Order

```
Phase 1 (Parallel - UI/Compose)
├── Agent 1: mutableStateOf fixes ────┐
└── Agent 2: TTS announcements ───────┼── Can run in parallel
                                      │
Phase 2 (Quick - Threading)           │
└── Agent 3: Thread.sleep fixes ──────┘

Phase 3 (Parallel - Memory) [After Phase 2]
├── Agent 4: NodeInfo + caching ──────┐
└── Agent 5: Bounds checking ─────────┼── Can run in parallel

Phase 4 (Sequential - Architecture) [After Phase 3]
├── Agent 6: Events + retry ──────────┘
└── Agent 7: IVoiceOSContext + handlers [After Agent 6]
```

---

## Success Criteria

| Criterion | Target |
|-----------|--------|
| Build passes | `./gradlew :Modules:VoiceOS:apps:VoiceOSCore:compileDebugKotlin` |
| Zero Thread.sleep() | All replaced with delay() |
| Zero leaked NodeInfo | All getChild() calls have recycle() |
| Bounded maps | eventCounts, metrics have size limits |
| mutableStateOf fixed | All 4 overlays use `by` delegation |
| processQueuedEvents | Implementation exists and called |
| Max retry | LearnApp init has 3-attempt limit |

---

## Rollback Strategy

Each phase is independently deployable:

1. **Phase 1 rollback:** Revert overlay changes (no functional impact)
2. **Phase 2 rollback:** Restore Thread.sleep (temporary ANR risk)
3. **Phase 3 rollback:** Remove caching (performance regression)
4. **Phase 4 rollback:** Keep direct VoiceOSService dependency

---

## Files Summary

| Category | Count |
|----------|-------|
| Files to Modify | 24 |
| Files to Create | 0 |
| Files to Delete | 0 |
| Total Changes | 24 |

---

**Plan Version:** 1.0.0
**Created:** 2025-12-18
**Author:** VoiceOS Development Team
