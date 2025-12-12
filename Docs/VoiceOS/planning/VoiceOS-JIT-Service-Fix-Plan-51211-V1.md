# Implementation Plan: JIT Service Integration Fix

## Overview

**Feature:** Wire JITLearningService to JustInTimeLearner
**Platforms:** Android only
**Swarm Recommended:** No (single module focus)
**Estimated:** 12 tasks, 4-5 hours

---

## Chain of Thought Reasoning

### Problem Statement
JITLearningService AIDL interface exists but returns stub values. LearnAppLite/Pro can bind but get no real data.

### Root Cause
```
JITLearningService.onCreate() → // TODO: Initialize JustInTimeLearner
                              → Never creates learner instance
```

### Solution Approach

**Selected: Wire existing JustInTimeLearner architecture**

Reasoning:
1. JustInTimeLearner already works (tested via LearnAppIntegration)
2. Database, repository, and LearnAppCore already initialized in VoiceOSCore
3. Service runs in VoiceOSCore process, can access same instances
4. Avoids code duplication

### Dependency Analysis

```
JITLearningService needs:
├─ Context ✓ (from Service)
├─ DatabaseManager ✓ (singleton in VoiceOSCore)
├─ LearnAppRepository ✓ (can create or get from integration)
├─ LearnAppCore ✓ (can create or get from integration)
└─ AccessibilityService ✗ (needs bridge from VoiceOSService)
```

### Key Insight
Instead of creating new JustInTimeLearner in service, **share the instance from LearnAppIntegration**. This ensures:
- Single source of truth for JIT state
- No race conditions
- Consistent database access

---

## Phases

### Phase 1: Service Initialization (P0) - 1.5 hours

**Goal:** Connect JITLearningService to LearnAppIntegration's JustInTimeLearner

#### Task 1.1: Add LearnAppIntegration reference
**File:** `JITLearningService.kt`
**Lines:** 479-480

```kotlin
// Add field
private var learnAppIntegration: LearnAppIntegration? = null

// In onCreate
override fun onCreate() {
    super.onCreate()
    try {
        learnAppIntegration = LearnAppIntegration.getInstance()
        Log.i(TAG, "Connected to LearnAppIntegration")
    } catch (e: IllegalStateException) {
        Log.w(TAG, "LearnAppIntegration not initialized yet")
    }
}
```

#### Task 1.2: Add lazy initialization for late binding
**File:** `JITLearningService.kt`

```kotlin
private fun getIntegration(): LearnAppIntegration? {
    if (learnAppIntegration == null) {
        try {
            learnAppIntegration = LearnAppIntegration.getInstance()
        } catch (e: IllegalStateException) {
            // Not ready yet
        }
    }
    return learnAppIntegration
}
```

#### Task 1.3: Expose JustInTimeLearner from LearnAppIntegration
**File:** `LearnAppIntegration.kt`

```kotlin
// Add getter for JIT learner
fun getJustInTimeLearner(): JustInTimeLearner = justInTimeLearner
```

---

### Phase 2: State Query Methods (P0) - 1 hour

**Goal:** Return real JIT statistics

#### Task 2.1: Implement queryState()
**File:** `JITLearningService.kt`
**Lines:** 139-149

```kotlin
override fun queryState(): JITState {
    val integration = getIntegration()
    val learner = integration?.getJustInTimeLearner()

    return if (learner != null) {
        val stats = learner.getStats()
        JITState(
            isActive = learner.isActive() && !isPaused,
            screensLearned = stats.screensLearned,
            elementsDiscovered = stats.elementsDiscovered,
            currentPackage = stats.currentPackage
        )
    } else {
        JITState(
            isActive = false,
            screensLearned = 0,
            elementsDiscovered = 0,
            currentPackage = null
        )
    }
}
```

#### Task 2.2: Add getStats() to JustInTimeLearner
**File:** `JustInTimeLearner.kt`

```kotlin
data class JITStats(
    val screensLearned: Int,
    val elementsDiscovered: Int,
    val currentPackage: String?
)

fun getStats(): JITStats {
    return JITStats(
        screensLearned = learnedScreens.size,
        elementsDiscovered = totalElementsDiscovered,
        currentPackage = currentPackage
    )
}

fun isActive(): Boolean = isLearning
```

#### Task 2.3: Implement isScreenLearned()
**File:** `JITLearningService.kt`
**Lines:** 148-149

```kotlin
override fun isScreenLearned(screenHash: String): Boolean {
    return getIntegration()?.getJustInTimeLearner()?.hasScreen(screenHash) ?: false
}
```

---

### Phase 3: Pause/Resume Methods (P0) - 30 min

**Goal:** Control JIT learning from LearnApp

#### Task 3.1: Implement pauseCapture()
**File:** `JITLearningService.kt`
**Lines:** 126-127

```kotlin
override fun pauseCapture() {
    isPaused = true
    getIntegration()?.getJustInTimeLearner()?.pause()
    dispatchStateChanged()
}
```

#### Task 3.2: Implement resumeCapture()
**File:** `JITLearningService.kt`
**Lines:** 132-133

```kotlin
override fun resumeCapture() {
    isPaused = false
    getIntegration()?.getJustInTimeLearner()?.resume()
    dispatchStateChanged()
}
```

#### Task 3.3: Add pause/resume to JustInTimeLearner
**File:** `JustInTimeLearner.kt`

```kotlin
private var isPaused = false

fun pause() {
    isPaused = true
    Log.i(TAG, "JIT learning paused")
}

fun resume() {
    isPaused = false
    Log.i(TAG, "JIT learning resumed")
}

// Update processAccessibilityEvent to check isPaused
fun onAccessibilityEvent(event: AccessibilityEvent) {
    if (isPaused) return
    // ... existing code
}
```

---

### Phase 4: Event Dispatch (P1) - 1.5 hours

**Goal:** Send real-time events to registered listeners

#### Task 4.1: Add dispatchScreenChanged()
**File:** `JITLearningService.kt`

```kotlin
private fun dispatchScreenChanged(event: ScreenChangeEvent) {
    val deadListeners = mutableListOf<IAccessibilityEventListener>()

    eventListeners.forEach { listener ->
        try {
            listener.onScreenChanged(event)
        } catch (e: RemoteException) {
            Log.w(TAG, "Listener disconnected", e)
            deadListeners.add(listener)
        }
    }

    eventListeners.removeAll(deadListeners)
}
```

#### Task 4.2: Add dispatchStateChanged()
**File:** `JITLearningService.kt`

```kotlin
private fun dispatchStateChanged() {
    val state = queryState()
    // Listeners can poll queryState() or we can add onStateChanged()
}
```

#### Task 4.3: Hook event dispatch to JustInTimeLearner
**File:** `JustInTimeLearner.kt`

```kotlin
interface JITEventCallback {
    fun onScreenLearned(screenHash: String, elementCount: Int)
    fun onElementDiscovered(stableId: String, vuid: String?)
}

private var eventCallback: JITEventCallback? = null

fun setEventCallback(callback: JITEventCallback?) {
    eventCallback = callback
}

// In screen processing:
eventCallback?.onScreenLearned(screenHash, elements.size)
```

#### Task 4.4: Wire callback in JITLearningService
**File:** `JITLearningService.kt`

```kotlin
override fun onCreate() {
    super.onCreate()
    setupEventCallback()
}

private fun setupEventCallback() {
    getIntegration()?.getJustInTimeLearner()?.setEventCallback(object : JITEventCallback {
        override fun onScreenLearned(screenHash: String, elementCount: Int) {
            val event = ScreenChangeEvent().apply {
                this.screenHash = screenHash
                this.elementCount = elementCount
            }
            dispatchScreenChanged(event)
        }

        override fun onElementDiscovered(stableId: String, vuid: String?) {
            // Dispatch element event
        }
    })
}
```

---

### Phase 5: Element Query Methods (P2) - 1 hour

**Goal:** Allow LearnAppPro to inspect current screen

#### Task 5.1: Implement getCurrentScreenInfo()
**File:** `JITLearningService.kt`
**Lines:** 371-375

```kotlin
override fun getCurrentScreenInfo(): ParcelableNodeInfo? {
    val integration = getIntegration()
    val rootNode = integration?.getCurrentRootNode() ?: return null
    return ParcelableNodeInfo.fromAccessibilityNode(rootNode)
}
```

#### Task 5.2: Add getCurrentRootNode() to LearnAppIntegration
**File:** `LearnAppIntegration.kt`

```kotlin
fun getCurrentRootNode(): AccessibilityNodeInfo? {
    return accessibilityService.rootInActiveWindow
}
```

#### Task 5.3: Implement getMenuItems()
**File:** `JITLearningService.kt`
**Lines:** 186-188

```kotlin
override fun getMenuItems(menuId: String): List<ParcelableNodeInfo> {
    // Query from database or current screen
    return getIntegration()?.getJustInTimeLearner()?.getMenuItems(menuId)
        ?.map { ParcelableNodeInfo.fromElementInfo(it) }
        ?: emptyList()
}
```

---

### Phase 6: Testing & Verification (P0) - 30 min

#### Task 6.1: Build and verify no compilation errors

#### Task 6.2: Test LearnAppLite binding
- Connect to service
- Verify queryState() returns real values
- Test pause/resume

#### Task 6.3: Test LearnAppPro event stream
- Open Logs tab
- Navigate apps
- Verify events appear

---

## Time Estimates

| Phase | Tasks | Hours |
|-------|-------|-------|
| Phase 1: Service Init | 3 | 1.5 |
| Phase 2: State Query | 3 | 1.0 |
| Phase 3: Pause/Resume | 3 | 0.5 |
| Phase 4: Event Dispatch | 4 | 1.5 |
| Phase 5: Element Query | 3 | 1.0 |
| Phase 6: Testing | 3 | 0.5 |
| **Total** | **19** | **6.0** |

---

## Success Criteria

| Test | Expected |
|------|----------|
| LearnAppLite connects | Service connected = true |
| queryState().screensLearned | > 0 (if screens learned) |
| pauseCapture() | JIT stops learning |
| resumeCapture() | JIT resumes learning |
| LearnAppPro event log | Shows screen change events |
| getCurrentScreenInfo() | Returns ParcelableNodeInfo |

---

## Risk Mitigation

| Risk | Mitigation |
|------|------------|
| LearnAppIntegration not initialized | Lazy init with null checks |
| Service started before VoiceOS | Retry getIntegration() on each call |
| Dead listener references | Remove on RemoteException |
| Binder thread issues | Use Handler for main thread work |

---

**Plan Version:** 1.0
**Created:** 2025-12-11
**Status:** Ready for implementation
