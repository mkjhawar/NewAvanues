# Implementation Plan: VoiceOS Command Execution Fixes

**Date:** 2026-01-15 | **Version:** V1
**Issue Reference:** `Docs/issues/VoiceOS-Dynamic-Command-Execution-260115.md`
**Branch:** `fix/voice-command-execution`

---

## Overview

This plan addresses 6 critical issues identified in the ToT analysis:

| Priority | Issue | Root Cause |
|----------|-------|------------|
| P0 | Dynamic commands don't click | Stale AccessibilityNodeInfo references |
| P0 | Numeric commands fail | listIndex never populated |
| P1 | Overlay only in Gmail | Package whitelist filtering |
| P1 | Overlay doesn't clear | Missing screen change callback |
| P2 | Click not verified | No gesture completion callback |
| P2 | VUID lookup broken | Bounds-based lookup fails |

---

## Phase 1: Critical Fixes (P0)

### Task 1.1: Add Node Refresh Before Click

**File:** `Modules/VoiceOSCoreNG/src/androidMain/kotlin/com/augmentalis/voiceoscoreng/exploration/ElementClicker.kt`

**Current Code (Lines 42-60):**
```kotlin
override suspend fun clickElement(element: ElementInfo): ClickResult {
    val node = element.node as? AccessibilityNodeInfo
        ?: return ClickResult.Failed(ClickFailure.NODE_STALE, "Node is null")

    return withContext(Dispatchers.Main) {
        if (!node.isVisibleToUser) {
            return@withContext ClickResult.Failed(ClickFailure.NOT_VISIBLE)
        }
        // ... click logic
    }
}
```

**Required Change:**
```kotlin
override suspend fun clickElement(element: ElementInfo): ClickResult {
    // TRY 1: Refresh element to get fresh node
    val freshElement = refreshElement(element)

    if (freshElement != null) {
        val node = freshElement.node as? AccessibilityNodeInfo
        if (node != null) {
            return withContext(Dispatchers.Main) {
                if (!node.isVisibleToUser) {
                    // Fall through to bounds-based click
                } else if (!node.isEnabled) {
                    return@withContext ClickResult.Failed(ClickFailure.NOT_ENABLED)
                } else {
                    // Try action click first
                    val success = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    if (success) {
                        return@withContext ClickResult.Success
                    }
                }
                // Fallback to gesture click
                val bounds = element.bounds
                val gestureSuccess = clickAtCoordinates(bounds.centerX, bounds.centerY)
                if (gestureSuccess) ClickResult.Success
                else ClickResult.Failed(ClickFailure.GESTURE_FAILED)
            }
        }
    }

    // TRY 2: Element not found - use bounds-based gesture click
    val bounds = element.bounds
    return withContext(Dispatchers.Main) {
        val success = clickAtCoordinates(bounds.centerX, bounds.centerY)
        if (success) ClickResult.Success
        else ClickResult.Failed(ClickFailure.ELEMENT_GONE, "Element no longer exists")
    }
}
```

**Acceptance Criteria:**
- [ ] `refreshElement()` called before any node operation
- [ ] Graceful fallback to bounds-based click
- [ ] No exceptions on stale nodes
- [ ] Click success rate improves to >90%

---

### Task 1.2: Populate listIndex During Element Extraction

**File:** `Modules/VoiceOSCoreNG/src/androidMain/kotlin/com/augmentalis/voiceoscoreng/exploration/DFSExplorer.kt`

**Function:** `extractElementsRecursive()` (approximate lines 402-433)

**Required Change:**

```kotlin
private fun extractElementsRecursive(
    node: AccessibilityNodeInfo,
    elements: MutableList<ElementInfo>,
    packageName: String,
    parentIsListContainer: Boolean = false,
    indexInParent: Int = -1
) {
    val className = node.className?.toString() ?: ""

    // Detect list containers
    val isListContainer = className.contains("RecyclerView") ||
                          className.contains("ListView") ||
                          className.contains("GridView") ||
                          className.contains("ScrollView")

    val rect = Rect()
    node.getBoundsInScreen(rect)

    // Determine listIndex for this element
    val listIndex = when {
        // Direct child of a list container - use position
        parentIsListContainer && indexInParent >= 0 -> indexInParent
        // Not in a list
        else -> -1
    }

    val element = ElementInfo(
        text = node.text?.toString() ?: "",
        contentDescription = node.contentDescription?.toString() ?: "",
        className = className,
        resourceId = node.viewIdResourceName ?: "",
        bounds = Bounds(rect.left, rect.top, rect.right, rect.bottom),
        isClickable = node.isClickable,
        isEnabled = node.isEnabled,
        isScrollable = node.isScrollable,
        isLongClickable = node.isLongClickable,
        packageName = packageName,
        node = node,
        listIndex = listIndex,  // NOW POPULATED!
        isInDynamicContainer = parentIsListContainer || isListContainer
    )

    if (element.isClickable || element.isLongClickable || element.isScrollable) {
        elements.add(element)
    }

    // Recursively process children
    for (i in 0 until node.childCount) {
        val child = node.getChild(i) ?: continue
        extractElementsRecursive(
            child,
            elements,
            packageName,
            parentIsListContainer = isListContainer,  // Pass list container flag
            indexInParent = i                         // Pass child position
        )
    }
}
```

**Also Update:** `AndroidUIExecutor.kt` if it has similar extraction logic

**Acceptance Criteria:**
- [ ] List items get sequential listIndex (0, 1, 2...)
- [ ] Non-list items have listIndex = -1
- [ ] Nested lists handled correctly
- [ ] "first", "second", "item 11" commands now work

---

### Task 1.3: Verify CommandGenerator Uses listIndex

**File:** `Modules/VoiceOSCoreNG/src/commonMain/kotlin/com/augmentalis/voiceoscoreng/common/CommandGenerator.kt`

**Verify Lines 116-165:**
```kotlin
fun generateListIndexCommands(elements: List<ElementInfo>): List<QuantizedCommand> {
    val listElements = elements
        .filter { it.listIndex >= 0 }  // Should now have data!
        .sortedBy { it.listIndex }

    return listElements.mapIndexed { index, element ->
        val phrase = when (index) {
            0 -> "first"
            1 -> "second"
            2 -> "third"
            3 -> "fourth"
            4 -> "fifth"
            else -> "item ${index + 1}"
        }

        QuantizedCommand(
            phrase = phrase,
            actionType = ActionType.CLICK,
            targetVuid = element.uuid,
            category = ActionCategory.UI,
            confidence = 1.0f
        )
    }
}
```

**Acceptance Criteria:**
- [ ] Ordinal commands generated for list items
- [ ] Commands registered in CommandRegistry
- [ ] Voice engine receives updated grammar

---

## Phase 2: Overlay Fixes (P1)

### Task 2.1: Remove/Expand Package Whitelist

**File:** `android/apps/VoiceOS/app/src/main/java/com/augmentalis/voiceoscore/accessibility/managers/EventRouter.kt`

**Current Code (Lines 244-259):**
```kotlin
if (packageName == null && currentPackage != null) {
    val isWindowChange = isWindowChangeEvent(event)

    if (isWindowChange && !DynamicPackageConfig.shouldMonitorPackage(contextProvider(), currentPackage)) {
        return  // BLOCKS MOST APPS
    }
    // ...
}
```

**Required Change - Option A (Remove filter):**
```kotlin
if (packageName == null && currentPackage != null) {
    val isWindowChange = isWindowChangeEvent(event)

    // REMOVED: Package filtering - monitor all apps
    // Apps can be excluded via settings if needed

    if (isWindowChange) {
        packageName = currentPackage
    } else {
        return
    }
}
```

**Required Change - Option B (Invert to exclusion list):**
```kotlin
if (packageName == null && currentPackage != null) {
    val isWindowChange = isWindowChangeEvent(event)

    // Only exclude known problematic packages
    if (isWindowChange && DynamicPackageConfig.isExcludedPackage(currentPackage)) {
        return
    }

    if (isWindowChange) {
        packageName = currentPackage
    } else {
        return
    }
}
```

**Also Update DynamicPackageConfig.kt:**
```kotlin
// Change from whitelist to exclusion list
private val EXCLUDED_PACKAGES = setOf(
    "com.android.systemui"  // Only truly problematic packages
)

fun isExcludedPackage(packageName: String): Boolean {
    return packageName in EXCLUDED_PACKAGES
}
```

**Acceptance Criteria:**
- [ ] Overlay appears in non-Gmail apps
- [ ] System UI excluded to prevent issues
- [ ] >95% app coverage achieved

---

### Task 2.2: Clear Overlay on Screen Change

**File:** `android/apps/VoiceOS/app/src/main/java/com/augmentalis/voiceoscore/accessibility/managers/EventRouter.kt`

**Current Code (Lines 310-340):**
```kotlin
private fun handleWindowStateChanged(event: AccessibilityEvent, packageName: String) {
    commandScope.launch {
        try {
            windowStateChangeListener?.invoke(
                event.packageName?.toString(),
                event.className?.toString()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error in window state change listener", e)
        }
    }
    // ... NO OVERLAY CLEARING
}
```

**Required Change:**
```kotlin
private fun handleWindowStateChanged(event: AccessibilityEvent, packageName: String) {
    commandScope.launch {
        try {
            // CLEAR OVERLAYS FIRST on screen change
            screenChangeCallback?.onScreenChanged()

            windowStateChangeListener?.invoke(
                event.packageName?.toString(),
                event.className?.toString()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error in window state change listener", e)
        }
    }
}

// Add callback interface
interface ScreenChangeCallback {
    fun onScreenChanged()
}

// Wire up in initialization
fun setScreenChangeCallback(callback: ScreenChangeCallback) {
    this.screenChangeCallback = callback
}
```

**Also Wire in VoiceOSAccessibilityService or similar:**
```kotlin
eventRouter.setScreenChangeCallback(object : ScreenChangeCallback {
    override fun onScreenChanged() {
        overlayManager.hideAll()  // Clear all badges
    }
})
```

**Acceptance Criteria:**
- [ ] Overlay clears when app changes
- [ ] Overlay clears when activity changes
- [ ] No stale badges visible
- [ ] Fresh badges shown for new screen

---

## Phase 3: Robustness Improvements (P2)

### Task 3.1: Add Gesture Completion Callback

**File:** `Modules/VoiceOSCoreNG/src/androidMain/kotlin/com/augmentalis/voiceoscoreng/exploration/ElementClicker.kt`

**Current Code (Lines 96-116):**
```kotlin
private fun clickAtCoordinates(x: Int, y: Int): Boolean {
    val gesture = createClickGesture(x.toFloat(), y.toFloat())
    return try {
        accessibilityService.dispatchGesture(gesture, null, null)  // NO CALLBACK
    } catch (e: Exception) {
        false
    }
}
```

**Required Change:**
```kotlin
private suspend fun clickAtCoordinates(x: Int, y: Int): Boolean {
    return suspendCancellableCoroutine { continuation ->
        val gesture = createClickGesture(x.toFloat(), y.toFloat())

        val callback = object : AccessibilityService.GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription) {
                if (continuation.isActive) {
                    continuation.resume(true)
                }
            }

            override fun onCancelled(gestureDescription: GestureDescription) {
                if (continuation.isActive) {
                    continuation.resume(false)
                }
            }
        }

        try {
            val dispatched = accessibilityService.dispatchGesture(gesture, callback, null)
            if (!dispatched && continuation.isActive) {
                continuation.resume(false)
            }
        } catch (e: Exception) {
            if (continuation.isActive) {
                continuation.resume(false)
            }
        }

        // Timeout after 2 seconds
        continuation.invokeOnCancellation {
            // Gesture still pending - treat as failure
        }
    }
}
```

**Acceptance Criteria:**
- [ ] Gesture completion tracked
- [ ] Failures properly reported
- [ ] Timeout handling added
- [ ] Click verification accurate

---

### Task 3.2: Implement Proper VUID-Based Click

**File:** `Modules/VoiceOSCoreNG/src/androidMain/kotlin/com/augmentalis/voiceoscoreng/handlers/AndroidUIExecutor.kt`

**Required Change:**
```kotlin
override suspend fun clickByVuid(vuid: String): Boolean {
    // Strategy 1: Check element cache
    val cachedElement = elementCache[vuid]
    if (cachedElement != null) {
        val result = clickElement(cachedElement)
        if (result.isSuccess) return true
    }

    // Strategy 2: Fresh scan of current screen
    val rootNode = accessibilityService.rootInActiveWindow ?: return false
    val packageName = rootNode.packageName?.toString() ?: return false

    try {
        val freshElement = findElementByVuidInTree(rootNode, vuid, packageName)
        if (freshElement != null) {
            val result = clickElement(freshElement)
            return result.isSuccess
        }
    } finally {
        rootNode.recycle()
    }

    // Strategy 3: VUID not found - element removed
    Log.w(TAG, "Element with VUID $vuid not found on current screen")
    return false
}

private fun findElementByVuidInTree(
    node: AccessibilityNodeInfo,
    targetVuid: String,
    packageName: String
): ElementInfo? {
    // Generate VUID for this node and check
    val element = nodeToElementInfo(node, packageName)
    val nodeVuid = generateVuid(element)

    if (nodeVuid == targetVuid) {
        return element
    }

    // Recurse into children
    for (i in 0 until node.childCount) {
        val child = node.getChild(i) ?: continue
        val found = findElementByVuidInTree(child, targetVuid, packageName)
        if (found != null) {
            return found
        }
        child.recycle()
    }

    return null
}
```

**Acceptance Criteria:**
- [ ] VUID lookup works without bounds
- [ ] Cache utilized when available
- [ ] Fresh scan as fallback
- [ ] Proper resource cleanup

---

## Task Summary

| Task | Priority | File | Complexity |
|------|----------|------|------------|
| 1.1 | P0 | ElementClicker.kt | Medium |
| 1.2 | P0 | DFSExplorer.kt | Medium |
| 1.3 | P0 | CommandGenerator.kt | Low (verify) |
| 2.1 | P1 | EventRouter.kt, DynamicPackageConfig.kt | Low |
| 2.2 | P1 | EventRouter.kt, VoiceOSAccessibilityService | Medium |
| 3.1 | P2 | ElementClicker.kt | Medium |
| 3.2 | P2 | AndroidUIExecutor.kt | High |

---

## Testing Checklist

### Phase 1 Tests
- [ ] Open Gmail, say "first" - should click first email
- [ ] Open Gmail, say "second" - should click second email
- [ ] Open Gmail, say "item 11" - should click 11th item (scroll if needed)
- [ ] Open Gmail, wait 30 seconds, say "click Updates" - should still work

### Phase 2 Tests
- [ ] Open Calculator - overlay should appear
- [ ] Open Settings - overlay should appear
- [ ] Gmail -> Calculator - old Gmail badges should disappear
- [ ] Navigate within app - overlay should refresh

### Phase 3 Tests
- [ ] Click execution logs show "completed" or "cancelled"
- [ ] VUID lookup works even if element bounds shifted
- [ ] Multiple rapid commands handled correctly

---

## Rollback Plan

If issues arise:
1. Revert ElementClicker.kt changes (Phase 1.1)
2. Revert extraction changes (Phase 1.2)
3. Re-enable package filtering (Phase 2.1)
4. Remove screen change callback (Phase 2.2)

Each phase can be rolled back independently.

---

---

## Phase 4: AVID Migration Cleanup (P3)

### Background

The codebase uses **two distinct ID systems** that need clarification:

| ID Type | Generator | Format | Storage | Purpose |
|---------|-----------|--------|---------|---------|
| Sequential AVID | `AvidGenerator.generateElementId()` | `AVID-A-000001` | `ElementInfo.uuid` | Unique tracking |
| Element Fingerprint | `ElementFingerprint.fromElementInfo()` | `BTN:a3f2e1c9` | `QuantizedCommand.targetVuid` | Command targeting |

### Task 4.1: Rename `targetVuid` to `targetFingerprint` (Optional - Breaking Change)

**Files to Update:**
- `QuantizedCommand.kt` - Change field name
- `CommandGenerator.kt` - Update references
- `CommandRegistry.kt` - Update key field
- `UIHandler.kt` - Update usage
- `ActionCoordinator.kt` - Update references
- All tests referencing `targetVuid`

**Risk:** High (breaking change to API)
**Recommendation:** Document alias for backwards compatibility

```kotlin
data class QuantizedCommand(
    ...
    val targetFingerprint: String?,   // NEW: Clear name
    ...
) {
    @Deprecated("Use targetFingerprint", ReplaceWith("targetFingerprint"))
    val targetVuid: String? get() = targetFingerprint  // Alias for compat
}
```

### Task 4.2: Consolidate VUID Generation to ElementFingerprint

**Problem:** JITLearner uses old format `vuid_pkg_id`

**File:** `JITLearner.kt`

**Current (line ~391):**
```kotlin
private fun generateVuid(element: ElementInfo): String {
    return "vuid_${element.packageName}_$identifier"  // OLD FORMAT
}
```

**Required:**
```kotlin
private fun generateVuid(element: ElementInfo, packageName: String): String {
    return ElementFingerprint.fromElementInfo(element, packageName)  // NEW FORMAT
}
```

### Task 4.3: Remove or Repurpose ElementInfo.uuid

**Options:**

**Option A: Remove Field**
- Delete `uuid: String? = null` from ElementInfo
- Update ElementRegistrar to not call `preGenerateUuids()`
- Simplest approach

**Option B: Use for Debug/Tracking Only**
- Keep field but document it's for debugging
- Not used in command execution flow

**Recommendation:** Option B (keep for debugging/analytics)

### Task 4.4: Update Documentation

**Files to Update:**
- `README.md` - Replace "VUID Generation" with "AVID Fingerprint"
- `docs/analysis/VUID-Unified-System-Analysis-260113-V1.md` - Mark as superseded
- MasterDocs AI index - Update class references

---

## Task Summary (All Phases)

| Phase | Task | Priority | Complexity | Breaking |
|-------|------|----------|------------|----------|
| 1 | 1.1 Node refresh | P0 | Medium | No |
| 1 | 1.2 listIndex population | P0 | Medium | No |
| 1 | 1.3 Verify CommandGenerator | P0 | Low | No |
| 2 | 2.1 Remove package whitelist | P1 | Low | No |
| 2 | 2.2 Clear overlay on screen change | P1 | Medium | No |
| 3 | 3.1 Gesture completion callback | P2 | Medium | No |
| 3 | 3.2 AVID fingerprint lookup | P2 | High | No |
| 4 | 4.1 Rename targetVuid (optional) | P3 | High | Yes |
| 4 | 4.2 Consolidate VUID generation | P3 | Low | No |
| 4 | 4.3 Cleanup ElementInfo.uuid | P3 | Low | No |
| 4 | 4.4 Update documentation | P3 | Low | No |

---

## AVID System Reference

### Element Fingerprint Generation
```kotlin
// Use this for command targeting (deterministic)
val fingerprint = ElementFingerprint.fromElementInfo(element, packageName)
// Result: "BTN:a3f2e1c9"
```

### Sequential AVID Generation
```kotlin
// Use this for unique tracking (sequential)
val avid = AvidGenerator.generateElementId()
// Result: "AVID-A-000001"
```

### Fingerprint Lookup Flow
```kotlin
// When executing command:
val command = registry.findByPhrase("Settings")
val targetFingerprint = command.targetVuid  // "BTN:a3f2e1c9"

// Find element on current screen with matching fingerprint
val rootNode = accessibilityService.rootInActiveWindow
val element = findElementByFingerprint(rootNode, targetFingerprint, packageName)

// findElementByFingerprint should:
// 1. Traverse accessibility tree
// 2. For each element, compute ElementFingerprint.fromElementInfo()
// 3. Compare with targetFingerprint
// 4. Return matching element or null
```

---

## Definition of Done

- [ ] All P0 tasks completed and tested
- [ ] All P1 tasks completed and tested
- [ ] Dynamic command success rate >95%
- [ ] Numeric command success rate >90%
- [ ] Overlay visible in >95% of apps
- [ ] No regression in static commands
- [ ] AVID fingerprint lookup working
- [ ] Code reviewed
- [ ] MasterDocs updated
