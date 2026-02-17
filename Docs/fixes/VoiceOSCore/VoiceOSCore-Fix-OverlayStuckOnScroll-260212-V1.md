# VoiceOSCore-Fix-OverlayStuckOnScroll-260212-V1

**Module:** VoiceOSCore / Avanues App
**Type:** Fix
**Date:** 2026-02-12
**Version:** 1
**Branch:** VoiceOSCore-KotlinUpdate
**Status:** Planned

---

## Problem Statement

On scroll, the inbox overlay updates only once. After that, the overlay items appear stuck and do not refresh. The overlay is also not cleared or updated when navigating from the inbox list to the email detail view (Gmail).

## Two-Level Deduplication Architecture

The system has TWO independent deduplication layers that BOTH must pass for overlays to update:

```
KMP Level (VoiceOSAccessibilityService):
  TYPE_WINDOW_CONTENT_CHANGED → handleScreenChange() → extract elements
    → ScreenFingerprinter.calculateFingerprint(elements)    ← CONTENT-based hash (text + descriptions)
    → if hash differs → generateCommands → onCommandsUpdated()

App Level (DynamicCommandGenerator.processScreen()):
  → ScreenCacheManager.generateScreenHash(rootNode)         ← STRUCTURAL-based hash (className, resourceId, depth)
  → if hash differs → generate overlay items
```

## Root Causes (5 identified)

### RC-1: TYPE_VIEW_SCROLLED never triggers processScreen
`handleScrollEvent()` only updates `BoundsResolver.updateScrollOffset()`. It does NOT call `handleScreenChange()`. After scroll, new items may load and `TYPE_WINDOW_CONTENT_CHANGED` fires, but this depends on Android's event pipeline — it's not guaranteed to fire for every scroll position.

### RC-2: ScreenCacheManager structural hash too stable on scroll
`collectElementSignatures()` uses generic `"v"` for scrollable container childCount and limits depth to `+2` inside them. Gmail inbox RecyclerView items are ViewHolder-based — structurally identical. Scrolling produces the exact same hash even when new emails appear.

**Key code** (`ScreenCacheManager.kt:58-69`):
```kotlin
val isScrollableContainer = className in listOf(
    "RecyclerView", "ListView", "GridView", ...
) || node.isScrollable

val childCount = if (isScrollableContainer) "v" else "c${node.childCount}"
// ...
val childDepthLimit = if (isScrollableContainer) depth + 2 else maxDepth
```

### RC-3: Gmail inbox→email is Fragment navigation, not Activity change
Gmail uses single-Activity architecture with Fragment navigation. Going from inbox to email detail is a Fragment transaction. `TYPE_WINDOW_STATE_CHANGED` may NOT fire — only `TYPE_WINDOW_CONTENT_CHANGED` (debounced). The structural hash between inbox and email detail may be similar enough (both have ScrollView/RecyclerView patterns) to not change.

### RC-4: OverlayNumberingExecutor preserves numbers for all target app changes
`handleScreenContext()` only resets on `isAppChange` or `isNewScreen && !isTargetApp`. Gmail IS a target app, so `isNewScreen && !isTargetApp` is always false for Gmail. Even for a major navigation (inbox→email detail), numbers are preserved — which is correct for scroll but wrong for screen navigation.

### RC-5: No content awareness in overlay hash path
The KMP-level `ScreenFingerprinter` includes text content (and DOES detect changes). But even when it passes and calls `onCommandsUpdated()`, the app-level `ScreenCacheManager` gates on structural hash only — blocking the overlay update.

## Event Flow Analysis

| Scenario | Event Type | KMP Hash | App Hash | Overlay Updates? |
|----------|-----------|----------|----------|-----------------|
| Gmail scroll (new items) | TYPE_WINDOW_CONTENT_CHANGED | CHANGES (new text) | SAME (structural) | NO — blocked by RC-2 |
| Gmail scroll only | TYPE_VIEW_SCROLLED | NOT TRIGGERED (RC-1) | NOT TRIGGERED | NO — blocked by RC-1 |
| Gmail inbox→email | TYPE_WINDOW_CONTENT_CHANGED | CHANGES (different content) | MAY NOT CHANGE (RC-3) | NO — blocked by RC-2/RC-3 |
| Gmail→Calculator | TYPE_WINDOW_STATE_CHANGED | CHANGES (different app) | CHANGES (different structure) | YES |

## Solution

### Change 1: ScreenCacheManager — Add Content Digest to Hash

Add a content fingerprint component that captures visible text, making the hash change when new content scrolls into view OR when navigating to a different screen.

**File:** `apps/avanues/.../service/ScreenCacheManager.kt`

Add a content-aware hashing method that supplements the structural hash:
- Count of text-bearing nodes in scrollable containers
- Hash of first 5 and last 5 text snippets (normalized, timestamps removed)
- This makes scroll and inbox→email produce different hashes

```kotlin
fun generateScreenHash(rootNode: AccessibilityNodeInfo): String {
    val elements = mutableListOf<String>()
    val contentDigest = mutableListOf<String>()
    collectElementSignatures(rootNode, elements, contentDigest = contentDigest, maxDepth = 8)

    val displayMetrics = resources.displayMetrics
    val dimensionKey = "${displayMetrics.widthPixels}x${displayMetrics.heightPixels}"

    // Hybrid hash: structural signature + content digest
    val contentKey = if (contentDigest.isNotEmpty()) {
        val first5 = contentDigest.take(5).joinToString(",")
        val last5 = contentDigest.takeLast(5).joinToString(",")
        "|ct${contentDigest.size}:$first5:$last5"
    } else ""

    val signature = "$dimensionKey|${elements.sorted().joinToString("|")}$contentKey"
    return HashUtils.calculateHash(signature).take(16)
}
```

Modify `collectElementSignatures()` to also collect text snippets from scrollable container children:
- Extract `node.text?.toString()?.take(20)` (first 20 chars)
- Normalize (strip timestamps, counts) using simple regex
- Add to `contentDigest` list

### Change 2: VoiceOSAccessibilityService — Trigger Overlay Refresh on Scroll

Add a debounced screen change trigger after scroll events. RecyclerView scroll → new items load → existing `TYPE_WINDOW_CONTENT_CHANGED` should handle this, but as a safety net, schedule a delayed processScreen after scroll settles.

**File:** `Modules/VoiceOSCore/.../VoiceOSAccessibilityService.kt`

In `handleScrollEvent()`, after updating BoundsResolver, add:
```kotlin
// Schedule debounced overlay refresh after scroll settles
pendingScrollRefreshJob?.cancel()
pendingScrollRefreshJob = serviceScope.launch {
    kotlinx.coroutines.delay(scrollRefreshDebounceMs)  // 300ms
    handleScreenChange(event)
}
```

This ensures that after scroll settles, the screen is re-processed even if `TYPE_WINDOW_CONTENT_CHANGED` was debounced away.

### Change 3: OverlayNumberingExecutor — Detect Major Navigation in Target Apps

Add a "screen content signature" check to distinguish scroll from major navigation:
- Track the set of visible resource IDs or element types
- When >60% of top-level structure changes, treat as major navigation (reset numbers)
- When <60% changes, treat as scroll (preserve numbers)

**File:** `apps/avanues/.../service/OverlayNumberingExecutor.kt`

Add `isSignificantStructuralChange` parameter to `handleScreenContext()`:
```kotlin
fun handleScreenContext(
    packageName: String,
    isTargetApp: Boolean,
    isNewScreen: Boolean,
    structuralChangeRatio: Float = 0f  // NEW: 0.0-1.0, how much structure changed
): Boolean {
    val isAppChange = packageName != lastPackageName
    var didReset = false

    if (isAppChange) {
        lastPackageName = packageName
        clearAllAssignmentsInternal()
        didReset = true
    } else if (isNewScreen && !isTargetApp) {
        clearAllAssignmentsInternal()
        didReset = true
    } else if (isNewScreen && isTargetApp && structuralChangeRatio > 0.6f) {
        // NEW: Major navigation within target app (e.g., inbox → email detail)
        clearAllAssignmentsInternal()
        didReset = true
    }

    lastIsTargetApp = isTargetApp
    return didReset
}
```

### Change 4: DynamicCommandGenerator — Pass Structural Change Ratio

**File:** `apps/avanues/.../service/DynamicCommandGenerator.kt`

Calculate structural change ratio by comparing previous and current top-level element set:
```kotlin
private var lastTopLevelElements: Set<String> = emptySet()

// In processScreen(), after extracting elements:
val currentTopLevel = elements.filter { it.depth <= 2 }
    .map { "${it.className}:${it.resourceId}" }
    .toSet()

val structuralChangeRatio = if (lastTopLevelElements.isEmpty()) 0f
    else 1f - (lastTopLevelElements.intersect(currentTopLevel).size.toFloat() /
               maxOf(lastTopLevelElements.size, currentTopLevel.size).toFloat())

lastTopLevelElements = currentTopLevel

val didReset = numberingExecutor.handleScreenContext(
    packageName, isTargetApp, isNewScreen, structuralChangeRatio
)
```

## Files Changed

| # | File | Action | Description |
|---|------|--------|-------------|
| 1 | `apps/avanues/.../service/ScreenCacheManager.kt` | MODIFY | Add content digest to hash (hybrid structural+content) |
| 2 | `Modules/VoiceOSCore/.../VoiceOSAccessibilityService.kt` | MODIFY | Add scroll-triggered debounced screen refresh |
| 3 | `apps/avanues/.../service/OverlayNumberingExecutor.kt` | MODIFY | Add structuralChangeRatio parameter for target app major navigation |
| 4 | `apps/avanues/.../service/DynamicCommandGenerator.kt` | MODIFY | Calculate structural change ratio, pass to executor |

## Verification Matrix

| Scenario | Expected After Fix | Verifies |
|----------|-------------------|----------|
| Gmail inbox scroll down | New emails get numbers 6,7,8... | RC-1, RC-2 fixed |
| Gmail inbox scroll back up | Original emails show 1,2,3 (preserved) | Numbering stability |
| Gmail inbox → email detail | Inbox badges clear, email buttons get new numbers from 1 | RC-3, RC-4 fixed |
| Gmail email → back to inbox | Inbox list re-numbered from 1 | RC-4 fixed |
| Calculator → Home | Numbers reset to 1 | Existing behavior preserved |
| Non-target app screen change | Numbers reset to 1 | Existing behavior preserved |
| Rapid scroll (debounce) | Updates at ~300ms intervals, not every frame | Performance |
| Static screen (no scroll) | No unnecessary reprocessing | Efficiency |

## Risk Assessment

| Risk | Mitigation |
|------|-----------|
| Performance: content-aware hash slower | Only collect first/last 5 text snippets, not all text |
| Scroll debounce adds events | 300ms delay prevents cascade; cancel-and-restart pattern |
| Structural change ratio threshold | 0.6 is conservative; Gmail inbox→email should be >0.8 change |
| RecyclerView childCount changes | Already handled by "v" marker — content digest handles this instead |
