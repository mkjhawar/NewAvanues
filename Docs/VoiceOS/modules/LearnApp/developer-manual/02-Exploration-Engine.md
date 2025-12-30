# Chapter 2: Exploration Engine

**Module**: LearnApp
**Package**: `com.augmentalis.voiceoscore.learnapp.exploration`
**Last Updated**: 2025-12-08

---

## Hybrid C-Lite Exploration Strategy (2025-12-05) ⭐ LATEST

### Overview

Hybrid C-Lite replaces the fragile `refreshFrameElements()` approach with a **fresh-scrape loop** that achieves 98% click success rate by always working with fresh accessibility nodes.

**Spec:** `docs/specifications/learnapp-hybrid-c-lite-spec-251204.md`
**Commit:** `77d1aae3`

**Key Improvements:**
| Metric | Before | After |
|--------|--------|-------|
| Click Success Rate | 70-80% | 98% |
| Stale Node Issues | Frequent | None |
| Matching Strategies | 4 complex | 1 simple |
| Code Complexity | High | Low |

### Problem Statement

The previous `refreshFrameElements()` approach used 4 matching strategies to find fresh nodes for stale elements:
1. UUID match (fails: fresh elements have uuid=null)
2. ResourceId match (works but not all elements have IDs)
3. Text+ContentDesc match (fails: many elements empty)
4. Fuzzy bounds match (fails: UI shifts >20px after clicks)

**Result:** 93% of elements failed to refresh, causing 70-80% click success.

### Solution: Fresh-Scrape Loop

Instead of refreshing stale elements, we:
1. Fresh scrape the screen before each click
2. Track clicked elements by `stableId()` (survives UI shifts)
3. Sort unclicked elements by `stabilityScore()` (click most reliable first)
4. Use node-first click with gesture fallback

**Algorithm:**
```kotlin
val clickedIds = mutableSetOf<String>()

while (true) {
    // Always fresh nodes
    val fresh = scrapeScreen()

    // Filter and sort
    val unclicked = fresh
        .filter { it.stableId() !in clickedIds }
        .sortedByDescending { it.stabilityScore() }

    if (unclicked.isEmpty()) break

    // Click with fallback
    val element = unclicked.first()
    click(element.node) || performGesture(element.bounds)

    clickedIds.add(element.stableId())
}
```

### New Functions in ElementInfo.kt

**stableId()** - Unique identifier that survives UI shifts:
```kotlin
fun stableId(): String = when {
    resourceId.isNotEmpty() -> "res:$resourceId"
    text.isNotEmpty() -> "txt:$className|$text"
    contentDescription.isNotEmpty() -> "cd:$className|$contentDescription"
    else -> "pos:$className|${bounds.centerX()}:${bounds.centerY()}"
}
```

**stabilityScore()** - Prioritization score (higher = more stable):
```kotlin
fun stabilityScore(): Int = when {
    resourceId.isNotEmpty() -> 100   // Most stable
    text.isNotEmpty() && contentDescription.isNotEmpty() -> 80
    text.isNotEmpty() -> 60
    contentDescription.isNotEmpty() -> 40
    else -> 0  // Bounds-only, least stable
}
```

### Implementation Details

**ExplorationEngine.kt Changes:**

1. **New class-level tracking:**
```kotlin
private val clickedStableIds = mutableSetOf<String>()
```

2. **New method `exploreScreenWithFreshScrape()`:**
- Fresh scrapes before each click
- Filters by clickedStableIds
- Sorts by stabilityScore (descending)
- Node-first click with gesture fallback
- Handles screen navigation detection

3. **Modified `exploreAppIterative()`:**
- Replaced element-by-element loop with `exploreScreenWithFreshScrape()`
- Removed `refreshFrameElements()` calls
- Simplified navigation handling

### Unit Tests

**File:** `HybridExplorationTest.kt`
**Tests:** 22 (all passing)

| Category | Tests |
|----------|-------|
| stableId() | 8 tests |
| stabilityScore() | 6 tests |
| Sorting | 2 tests |
| Click Tracking | 4 tests |
| Edge Cases | 2 tests |

**Run tests:**
```bash
./gradlew :modules:apps:VoiceOSCore:testDebugUnitTest \
  --tests "com.augmentalis.voiceoscore.learnapp.HybridExplorationTest"
```

### Benefits

| Benefit | Explanation |
|---------|-------------|
| 98% Success | Fresh nodes never stale |
| Simple | One tracking mechanism vs 4 matchers |
| Robust | stableId survives UI shifts |
| Optimized | Stable elements clicked first |
| Fallback | Gesture catches node failures |

### Log Tags

| Tag | Purpose |
|-----|---------|
| ExplorationEngine-HybridCLite | Fresh-scrape loop progress |
| ExplorationEngine-Click | Click attempts and fallbacks |

**Example logs:**
```
🔄 Starting fresh-scrape exploration for screen abc123...
📊 Fresh scrape: 15 unclicked elements (top stability: 100)
>>> CLICKING: "Settings" (Button) [stability: 100, id: res:com.app...]
✅ Node click succeeded for "Settings"
```

### Commit

**Hash:** `77d1aae3`
**Message:** feat(LearnApp): Implement Hybrid C-Lite exploration strategy for 98% click success

---
## Cumulative VUID Tracking (2025-12-08) CRITICAL FIX

### Problem

When exploring apps with intent relaunches (e.g., app exits and is relaunched via intent), the learning completion showed 10% when visual observation showed 50-75% of screens explored.

**Root Cause:** The P2 Fix (2025-12-07) called `clickTracker.clear()` on intent relaunch to prevent "inflated totalElements". However, this destroyed all exploration progress:
- 50 screens explored but only 12 tracked in final stats
- 37 screens lost across 2 intent relaunches (15 + 22 screens)
- 496 elements lost (189 + 307 elements)

### Solution: Hybrid Cumulative Tracking

Added class-level cumulative tracking sets that survive intent relaunches:

```kotlin
/**
 * FIX (2025-12-08): Global cumulative tracking - NEVER cleared during exploration
 * These track ALL exploration progress regardless of intent relaunches
 * Used for final completion percentage calculation
 */
private val cumulativeDiscoveredVuids = mutableSetOf<String>()  // All discovered element VUIDs
private val cumulativeClickedVuids = mutableSetOf<String>()      // All clicked element VUIDs
```

**Lifecycle:**
1. Cleared at START of new exploration session (`startExploration()`)
2. Never cleared during exploration (survives intent relaunches)
3. Populated whenever elements are registered/clicked
4. Used for final completion % calculation

**Relationship with clickTracker:**
| Tracker | Purpose | Cleared On |
|---------|---------|------------|
| clickTracker | Per-screen fresh-scrape logic | Intent relaunch |
| cumulativeVuids | Final completion % | New exploration session |

### Log Messages

```
// CUMULATIVE stats (accurate - use this!)
📊 Final Stats (CUMULATIVE): 450/600 VUIDs clicked (75% completeness)

// clickTracker stats (for comparison - may be lower due to intent relaunches)
📊 Final Stats (clickTracker): 149/149 elements clicked (100% completeness per session)
```

### Commit

**Hash:** `43f0c480`
**Message:** fix(learnapp): add cumulative VUID tracking for accurate completion percentage

---
## Aggressive Exploration Mode

**Version:** 1.1 (2025-11-22)
**Status:** Production
**Test Coverage:** 21 unit tests

### Overview

Aggressive Exploration Mode is a major enhancement to LearnApp's element discovery system. Instead of relying solely on Android's `isClickable` flag, it uses intelligent heuristics to identify ALL potentially interactive elements, resulting in 300-400% more screens discovered.

### Problem Solved

**Before (v1.0):**
- Only clicked elements with `isClickable=true`
- Missed bottom navigation tabs (often don't set clickable flag)
- Missed overflow menus (3-dot, hamburger icons)
- Missed toolbar action items
- Result: Incomplete app exploration (1-2 screens vs 6-8 screens)

**After (v1.1 - Aggressive Mode):**
- Clicks ALL potentially interactive elements
- Detects navigation elements by className patterns
- Identifies large touch targets (>= 48dp Material Design minimum)
- Discovers ImageView icons with contentDescription
- Result: Comprehensive app exploration (4-10 screens)

### Technical Implementation

#### 1. Smart Element Classification

**File:** `elements/ElementClassifier.kt`

The classifier now uses `isAggressivelyClickable()` instead of checking `isClickable` directly:

```kotlin
private fun isAggressivelyClickable(element: ElementInfo): Boolean {
    // 1. Already marked clickable
    if (element.isClickable) return true

    // 2. Navigation elements (className patterns)
    val navigationTypes = listOf(
        "bottomnavigationitemview",  // Bottom nav tabs
        "actionmenuitemview",        // Overflow menu items
        "tabview",                   // Tab elements
        "toolbar"                    // Toolbar actions
    )
    if (navigationTypes.any { className.contains(it) }) return true

    // 3. ImageViews (icons) with content description or >= 48dp
    if (iconTypes.any { className.contains(it) }) {
        if (element.contentDescription.isNotBlank()) return true
        if (bounds.width() >= 48 && bounds.height() >= 48) return true
    }

    // 4. Buttons always clickable
    if (buttonTypes.any { className.contains(it) }) return true

    return false
}
```

#### 2. Extended Timeouts

**File:** `exploration/ExplorationStrategy.kt`

| Setting | Old Value | New Value | Benefit |
|---------|-----------|-----------|---------|
| Max Depth | 50 | 100 | 2x deeper navigation |
| Max Exploration Time | 30 min | 60 min | Complex apps don't timeout |
| Login Wait Timeout | 1 min | 10 min | Time for 2FA, captchas |

**Dynamic Timeout Formula:**
```kotlin
fun calculateDynamicTimeout(elementCount: Int): Long {
    val baseTimeout = 60 * 60 * 1000L  // 60 min max
    val dynamicTimeout = elementCount * 2000L  // 2 sec per element
    return minOf(baseTimeout, maxOf(dynamicTimeout, 30 * 60 * 1000L))
}
```

**Examples:**
- Simple app (200 elements): 30 min
- Medium app (1000 elements): 33 min
- Complex app (5000 elements): 60 min (capped)

#### 3. System App Detection

**File:** `exploration/ExplorationEngine.kt`

Detects and partially supports system apps (Settings, Phone, Messages):

```kotlin
private fun isSystemApp(packageName: String): Boolean {
    // Heuristic 1: Package prefix
    val systemPrefixes = listOf("com.android.", "com.google.android.apps.messaging")

    // Heuristic 2: FLAG_SYSTEM check
    val appInfo = packageManager.getApplicationInfo(packageName, 0)
    return (appInfo.flags and FLAG_SYSTEM) != 0
}
```

### Usage

Aggressive mode is **automatic** - no configuration needed. The system intelligently decides which elements to click based on the heuristics above.

#### Element Prioritization

```
Priority 1: Explicitly clickable elements (isClickable=true)
Priority 2: Navigation elements (bottom nav, tabs, overflow)
Priority 3: Large ImageViews (>= 48dp)
Priority 4: ImageViews with contentDescription
Priority 5: Buttons (className contains "button")
```

### Safety Features

Aggressive mode maintains **all existing safety checks**:

✅ **Never clicks CRITICAL dangerous elements** (exit, power off, shutdown, sleep, logout) - UUID'd but not clicked
✅ **Defers non-critical dangerous elements** (submit, send, delete) - clicked last
✅ **Never clicks EditText** (prevents keyboard popup)
✅ **Never clicks disabled elements** (isEnabled=false)
✅ **Respects login screens** (pauses for user input)
✅ **Package filtering** (prevents clicking launcher elements)

#### Critical Dangerous Elements (2025-12-05)

Elements that are **NEVER clicked** during exploration (but still UUID'd and registered):

| Category | Patterns |
|----------|----------|
| System Power | power off, shutdown, restart, reboot, sleep, hibernate, turn off |
| App Termination | exit, quit, force stop, force close |
| Session | sign out, logout |
| Destructive | delete account, deactivate account, factory reset, wipe data |

These elements are detected by `isCriticalDangerousElement()` in `ExplorationEngine.kt`.

#### Recovery Mechanism (2025-12-05)

When exploration navigates to an external app or launcher, recovery attempts:

| Recovery Type | Method | DFS Stack |
|---------------|--------|-----------|
| BACK Recovery | Press BACK 5x | Stack preserved - continue from parent screen |
| Intent Relaunch | Launch app via intent | Stack cleared - restart from app entry point |
| Failed | Could not recover | Pop frame and try parent |

The intent relaunch recovery clears the DFS stack because:
- App starts at its entry point (not previous screen)
- Previous screens are no longer accessible via BACK
- Stale stack would cause repeated launcher exits

### Real-World Performance

**Test Results (2025-11-22):**

| App | Before | After | Improvement |
|-----|--------|-------|-------------|
| Google Calculator | 1 screen | ~4 screens | **+300%** |
| Google Clock | 2 screens | ~8 screens | **+300%** |
| Glovius | Exit on login | Full exploration | **100% fix** |
| System Settings | Not supported | Partial support | **New** |

**Command Success Rate:**
- "world clock" (Google Clock): ❌ Failed → ✅ Success
- "clear history" (Calculator): ❌ Failed → ✅ Success
- Dynamic commands: **95%+ success rate** (up from 40%)

### Testing

**Unit Tests:** 21 tests in 2 files
- `AggressiveExplorationTest.kt` - 11 tests (element classification)
- `ExplorationTimeoutTest.kt` - 10 tests (timeout behavior)

**Key Test Cases:**
```kotlin
@Test
fun testBottomNavigationTabsAreClickable() {
    val tab = createMockElement(
        className = "BottomNavigationItemView",
        isClickable = false  // Often not set!
    )
    assertTrue(classifier.classify(tab) is SafeClickable)
}

@Test
fun testOverflowMenuIconIsClickable() {
    val icon = createMockElement(
        className = "ActionMenuItemView",
        isClickable = false  // Often not set!
    )
    assertTrue(classifier.classify(icon) is SafeClickable)
}
```

### Developer Notes

#### When to Investigate

If exploration still misses screens:
1. Check logs for "Non-clickable" elements that should be clicked
2. Add className pattern to `navigationTypes` list
3. Adjust minimum touch target size (currently 48dp)
4. Check if element has unique contentDescription

#### Configuration Points

**ElementClassifier.kt:**
```kotlin
val minTouchTarget = 48  // Material Design minimum (adjust if needed)

val navigationTypes = listOf(
    "bottomnavigationitemview",
    "actionmenuitemview",
    // Add new patterns here
)
```

**ExplorationStrategy.kt:**
```kotlin
override fun getMaxDepth(): Int = 100  // Increase for very deep apps
override fun getMaxExplorationTime(): Long = 60 * 60 * 1000L  // 60 min
```

### Troubleshooting

**Issue:** Exploration still misses overflow menu

**Solution:**
1. Check element className: `adb shell dumpsys activity top`
2. Add className pattern to `navigationTypes` in `ElementClassifier.kt`
3. Verify `isAggressivelyClickable()` returns true for that element

**Issue:** Exploration takes too long

**Solution:**
- Reduce `maxDepth` from 100 to 75
- Add app-specific timeout in `calculateDynamicTimeout()`

**Issue:** Clicking wrong elements

**Solution:**
- Review dangerous element detector patterns
- Add element to skip list if needed
- Check package filtering is working correctly

### Future Enhancements

Planned for v1.2:
- [ ] Machine learning-based clickability prediction
- [ ] App-specific configuration profiles
- [ ] Real-time element discovery (during voice command usage)
- [ ] Gesture-based navigation (swipe, long-press)

---

---

**Navigation**: [← Previous: Overview](./01-Overview-Architecture.md) | [Index](./00-Index.md) | [Next: Developer Settings →](./03-Developer-Settings.md)
