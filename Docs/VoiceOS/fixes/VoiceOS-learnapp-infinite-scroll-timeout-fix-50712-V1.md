# LearnApp Infinite Scroll Timeout Fix

**Document:** VoiceOS-learnapp-infinite-scroll-timeout-fix-50712-V1.md
**Date:** 2025-12-07
**Version:** 1.0
**Status:** Implemented
**Severity:** HIGH

---

## Summary

LearnApp exploration was getting stuck (infinite hang) on screens with both horizontal and vertical scrolling, particularly on apps with dynamic/infinite-loading content like Microsoft Teams channels, Instagram feeds, and similar social media apps.

---

## Root Cause Analysis

### Issue 1: No Timeout on Scrollable Containers

**Location:** `ScrollExecutor.scrollAndCollectAll()`

When a scrollable container has `ScrollDirection.BOTH`, the executor would:
1. Scroll vertically (50 iterations x 500ms = 25 seconds max)
2. Then scroll horizontally (20 iterations x 500ms = 10 seconds max)
3. Total: **35+ seconds per container** without any timeout

For dynamic content where new elements keep appearing (Teams messages, social feeds), the hash changes on every scroll, preventing the "unchanged count" termination condition from triggering.

### Issue 2: No Per-Screen Timeout

**Location:** `ExplorationEngine.exploreScreenWithFreshScrape()`

The fresh-scrape exploration loop had no upper bound on time. If a screen had problematic scrollables, exploration could run indefinitely.

### Issue 3: No Timeout on exploreScreen() Call

**Location:** `ExplorationEngine` calling `screenExplorer.exploreScreen()`

The screen exploration call itself had no timeout wrapper, meaning if `collectAllElements()` hung, the entire exploration would freeze.

---

## Solution

### Multi-Layer Timeout Protection

#### Layer 1: Per-Container Timeout (ScreenExplorer.kt)

```kotlin
// 30s total for all scrollables, 10s per container
val totalTimeout = 30_000L
val perContainerTimeout = 10_000L

for ((index, scrollable) in scrollables.withIndex()) {
    // Check total timeout
    if (System.currentTimeMillis() - totalStartTime > totalTimeout) {
        Log.w("ScreenExplorer", "Total collection timeout reached")
        break
    }

    // Wrap each container in individual timeout
    val scrolledElements = withTimeoutOrNull(perContainerTimeout) {
        scrollExecutor.scrollAndCollectAll(scrollable)
    }

    if (scrolledElements == null) {
        Log.w("ScreenExplorer", "Scroll timeout on container - likely infinite content")
    }
}
```

#### Layer 2: Limited Scroll for BOTH Direction (ScrollExecutor.kt)

```kotlin
ScrollDirection.BOTH -> {
    // Use reduced iterations for complex scrollables
    val vertical = scrollVerticallyAndCollectLimited(scrollable, maxIterations = 10)
    val horizontal = scrollHorizontallyAndCollectLimited(scrollable, maxIterations = 5)
    (vertical + horizontal).distinctBy { it.hashCode() }
}
```

#### Layer 3: Per-Screen Exploration Timeout (ExplorationEngine.kt)

```kotlin
val screenExplorationTimeout = 120_000L  // 2 minutes max per screen
val screenStartTime = System.currentTimeMillis()

while (consecutiveFailures < maxConsecutiveFailures) {
    if (System.currentTimeMillis() - screenStartTime > screenExplorationTimeout) {
        Log.w("ExplorationEngine", "Per-screen exploration timeout reached")
        break
    }
    // ... exploration loop
}
```

#### Layer 4: exploreScreen() Call Timeout (ExplorationEngine.kt)

```kotlin
val freshExploration = withTimeoutOrNull(45_000L) {
    screenExplorer.exploreScreen(rootNode, packageName, frame.depth)
}

if (freshExploration == null) {
    Log.w("ExplorationEngine", "Fresh scrape timeout - complex scrollables")
    consecutiveFailures++
    continue
}
```

---

## Files Changed

| File | Changes |
|------|---------|
| `ScreenExplorer.kt` | Added 30s total / 10s per-container timeout in `collectAllElements()` |
| `ScrollExecutor.kt` | Added `scrollVerticallyAndCollectLimited()` and `scrollHorizontallyAndCollectLimited()` for BOTH direction |
| `ExplorationEngine.kt` | Added 2-minute per-screen timeout and 45s exploreScreen() timeout |

---

## Timeout Configuration

| Layer | Timeout | Purpose |
|-------|---------|---------|
| Total scrollable collection | 30 seconds | Cap all scrollable processing per screen |
| Per-container | 10 seconds | Prevent single container from blocking |
| Per-screen exploration | 2 minutes | Prevent infinite exploration loops |
| exploreScreen() call | 45 seconds | Prevent hang in screen exploration |
| BOTH vertical scroll | 10 iterations | Reduced from 50 for complex scrollables |
| BOTH horizontal scroll | 5 iterations | Reduced from 20 for complex scrollables |

---

## Testing

### Test Cases

1. **Microsoft Teams Channel** - Has both horizontal tabs and vertical message list
2. **Instagram Feed** - Infinite scrolling vertical feed
3. **Twitter/X Timeline** - Dynamic loading content
4. **Slack Channels** - Multiple scrollable areas

### Expected Behavior

- Exploration should complete within 2-3 minutes per screen maximum
- Timeout warnings logged when hit
- Exploration continues to next screen instead of hanging
- Visible elements are still collected even if scrolling times out

---

## User-Facing Changes

### New Floating Progress Widget

Along with the timeout fixes, a new **FloatingProgressWidget** was added:

- **Draggable**: User can move the widget anywhere on screen
- **Semi-transparent**: 92% opacity with dark background
- **Always visible**: Uses `TYPE_ACCESSIBILITY_OVERLAY` to stay above all content
- **Controls**:
  - Pause/Resume button
  - STOP button (immediately terminates exploration)
- **Progress display**: Shows percentage, status message, and stats

### How to Use

1. Start "Learn App" as usual
2. The floating widget appears in the top-right corner
3. Drag the widget to move it if blocking content
4. Press **Pause** to temporarily stop exploration
5. Press **STOP** to immediately terminate and save progress

---

## Related Documentation

- [LearnApp Scrollable Content Fix Plan](../specifications/VoiceOS-learnapp-scrollable-content-fix-plan-50412-V1.md)
- [LearnApp Hybrid C-Lite Spec](../specifications/VoiceOS-learnapp-hybrid-c-lite-spec-50412-V1.md)
- [LearnApp Developer Settings](../specifications/VoiceOS-learnapp-developer-settings-expansion-plan-50512-V1.md)

---

## Commit Reference

```
fix(learnapp): prevent exploration hanging on infinite scrollable content
Commit: cbc8d5e6
Branch: VoiceOS-Development
```

---

**Author:** VOS4 Development Team
**Reviewed:** 2025-12-07
