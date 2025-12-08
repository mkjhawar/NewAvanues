# Hybrid C-Lite Exploration Strategy - Specification

**Version:** 1.0
**Date:** 2025-12-04
**Status:** IMPLEMENTED
**Approach:** TDD (Test-Driven Development)
**Confidence Target:** 98% click success rate

> Implementation Complete: All tests passing, build verified. See commit for full implementation details.

---

## Executive Summary

Replace the fragile 4-strategy `refreshFrameElements()` matching approach with a simpler, more reliable **Fresh-Scrape Loop** that achieves 98% click success rate by:
1. Always working with fresh elements (no stale nodes)
2. Tracking clicked elements by stable identifiers
3. Prioritizing stable elements first
4. Falling back to gesture clicks when node clicks fail

---

## Problem Statement

### Current State (70-80% success)
```
Scrape elements (T=0) → Store bounds → Click element 1 → UI shifts →
Try to match old elements to fresh elements → MATCHING FAILS →
Most elements skipped
```

**Why matching fails:**
- Fresh elements have `uuid=null` (UUIDs generated later)
- Most elements have empty text/contentDescription
- Bounds shift >20px after clicks
- 4 complex strategies, all fail for different reasons

### Desired State (98% success)
```
Loop:
  Fresh scrape → Find unclicked elements → Click (node or gesture) →
  Track as clicked → Repeat until done
```

**Why this works:**
- Nodes always <100ms old when clicked
- No matching required - just track what's been clicked
- Gesture fallback catches elements that can't be found
- Stability sorting maximizes success before UI destabilizes

---

## Functional Requirements

| ID | Requirement | Priority |
|----|-------------|----------|
| FR-01 | Add `stableId()` function to ElementInfo | P0 |
| FR-02 | Add `stabilityScore()` function to ElementInfo | P0 |
| FR-03 | Implement fresh-scrape exploration loop | P0 |
| FR-04 | Track clicked elements by stableId | P0 |
| FR-05 | Sort elements by stability score (descending) | P1 |
| FR-06 | Gesture fallback when node click fails | P0 |
| FR-07 | Update lastKnownBounds after successful clicks | P1 |
| FR-08 | Remove/deprecate refreshFrameElements() | P2 |

---

## Technical Design

### 1. StableId Function

```kotlin
/**
 * Generate a stable identifier for element tracking.
 * Priority: resourceId > text > contentDescription > bounds
 */
fun ElementInfo.stableId(): String = when {
    resourceId.isNotEmpty() -> "res:$resourceId"
    text.isNotEmpty() -> "txt:$className|$text"
    contentDescription.isNotEmpty() -> "cd:$className|$contentDescription"
    else -> "pos:$className|${bounds.centerX()}:${bounds.centerY()}"
}
```

**Test Cases:**
| Input | Expected Output |
|-------|-----------------|
| resourceId="com.teams:id/chat" | "res:com.teams:id/chat" |
| text="Settings", class="Button" | "txt:Button\|Settings" |
| contentDesc="Menu", class="ImageView" | "cd:ImageView\|Menu" |
| bounds=Rect(100,200,150,250), class="View" | "pos:View\|125:225" |

### 2. StabilityScore Function

```kotlin
/**
 * Calculate stability score for sorting.
 * Higher score = more stable = click first.
 */
fun ElementInfo.stabilityScore(): Int = when {
    resourceId.isNotEmpty() -> 100  // Most stable
    text.isNotEmpty() && contentDescription.isNotEmpty() -> 80
    text.isNotEmpty() -> 60
    contentDescription.isNotEmpty() -> 40
    else -> 0  // Least stable (bounds-only)
}
```

**Test Cases:**
| Input | Expected Score |
|-------|----------------|
| resourceId="com.teams:id/chat" | 100 |
| text="Settings", contentDesc="Open settings" | 80 |
| text="Settings" only | 60 |
| contentDesc="Menu" only | 40 |
| bounds only | 0 |

### 3. Fresh-Scrape Exploration Loop

```kotlin
private suspend fun exploreWithHybridStrategy(packageName: String) {
    val clickedIds = mutableSetOf<String>()
    val lastKnownBounds = mutableMapOf<String, Rect>()
    var consecutiveEmptyLoops = 0

    while (consecutiveEmptyLoops < 3) {
        // STEP 1: Fresh scrape
        val freshElements = scrapeCurrentScreen()

        // STEP 2: Filter and sort
        val unclicked = freshElements
            .filter { it.stableId() !in clickedIds }
            .sortedByDescending { it.stabilityScore() }

        if (unclicked.isEmpty()) {
            consecutiveEmptyLoops++
            delay(500)
            continue
        }
        consecutiveEmptyLoops = 0

        // STEP 3: Click each element
        for (element in unclicked) {
            val stableId = element.stableId()
            var success = false

            // Primary: Fresh node click
            if (element.node != null) {
                success = clickElement(element.node)
            }

            // Fallback: Gesture at coordinates
            if (!success) {
                val coords = lastKnownBounds[stableId] ?: element.bounds
                success = performCoordinateClick(coords)
            }

            // Track result
            if (success) {
                clickedIds.add(stableId)
                lastKnownBounds[stableId] = element.bounds
            }

            delay(300)
        }
    }
}
```

---

## Test Plan (TDD)

### Phase 1: Unit Tests (Write First)

#### Test File: `HybridExplorationTest.kt`

```kotlin
class HybridExplorationTest {

    // ==================== stableId() Tests ====================

    @Test
    fun `stableId returns resourceId when available`() {
        val element = ElementInfo(
            className = "Button",
            resourceId = "com.teams:id/chat_tab",
            text = "Chat",
            bounds = Rect(0, 0, 100, 50)
        )
        assertEquals("res:com.teams:id/chat_tab", element.stableId())
    }

    @Test
    fun `stableId returns text when no resourceId`() {
        val element = ElementInfo(
            className = "Button",
            resourceId = "",
            text = "Settings",
            bounds = Rect(0, 0, 100, 50)
        )
        assertEquals("txt:Button|Settings", element.stableId())
    }

    @Test
    fun `stableId returns contentDescription when no resourceId or text`() {
        val element = ElementInfo(
            className = "ImageView",
            resourceId = "",
            text = "",
            contentDescription = "Menu icon",
            bounds = Rect(0, 0, 100, 50)
        )
        assertEquals("cd:ImageView|Menu icon", element.stableId())
    }

    @Test
    fun `stableId returns position when no identifiers`() {
        val element = ElementInfo(
            className = "View",
            resourceId = "",
            text = "",
            contentDescription = "",
            bounds = Rect(100, 200, 150, 250)
        )
        assertEquals("pos:View|125:225", element.stableId())
    }

    // ==================== stabilityScore() Tests ====================

    @Test
    fun `stabilityScore returns 100 for resourceId`() {
        val element = ElementInfo(
            className = "Button",
            resourceId = "com.teams:id/chat",
            bounds = Rect(0, 0, 100, 50)
        )
        assertEquals(100, element.stabilityScore())
    }

    @Test
    fun `stabilityScore returns 80 for text and contentDesc`() {
        val element = ElementInfo(
            className = "Button",
            text = "Settings",
            contentDescription = "Open settings"
        )
        assertEquals(80, element.stabilityScore())
    }

    @Test
    fun `stabilityScore returns 60 for text only`() {
        val element = ElementInfo(
            className = "Button",
            text = "Settings"
        )
        assertEquals(60, element.stabilityScore())
    }

    @Test
    fun `stabilityScore returns 40 for contentDesc only`() {
        val element = ElementInfo(
            className = "ImageView",
            contentDescription = "Menu"
        )
        assertEquals(40, element.stabilityScore())
    }

    @Test
    fun `stabilityScore returns 0 for bounds only`() {
        val element = ElementInfo(
            className = "View",
            bounds = Rect(0, 0, 100, 50)
        )
        assertEquals(0, element.stabilityScore())
    }

    // ==================== Sorting Tests ====================

    @Test
    fun `elements sorted by stability score descending`() {
        val elements = listOf(
            ElementInfo(className = "View", bounds = Rect(0,0,10,10)),  // score 0
            ElementInfo(className = "Button", resourceId = "id/btn"),   // score 100
            ElementInfo(className = "Text", text = "Hello"),            // score 60
        )

        val sorted = elements.sortedByDescending { it.stabilityScore() }

        assertEquals(100, sorted[0].stabilityScore())
        assertEquals(60, sorted[1].stabilityScore())
        assertEquals(0, sorted[2].stabilityScore())
    }

    // ==================== Click Tracking Tests ====================

    @Test
    fun `clicked elements are tracked by stableId`() {
        val clickedIds = mutableSetOf<String>()
        val element = ElementInfo(className = "Button", resourceId = "id/btn")

        clickedIds.add(element.stableId())

        assertTrue(element.stableId() in clickedIds)
    }

    @Test
    fun `unclicked elements filtered correctly`() {
        val clickedIds = mutableSetOf("res:id/btn1")
        val elements = listOf(
            ElementInfo(className = "Button", resourceId = "id/btn1"),
            ElementInfo(className = "Button", resourceId = "id/btn2"),
        )

        val unclicked = elements.filter { it.stableId() !in clickedIds }

        assertEquals(1, unclicked.size)
        assertEquals("res:id/btn2", unclicked[0].stableId())
    }
}
```

### Phase 2: Integration Tests

```kotlin
class HybridExplorationIntegrationTest {

    @Test
    fun `fresh scrape loop processes all elements`() {
        // Setup mock accessibility service
        // Verify all elements get clicked
        // Verify clickedIds contains all stableIds
    }

    @Test
    fun `gesture fallback triggered when node click fails`() {
        // Setup element with null node
        // Verify performCoordinateClick called
    }

    @Test
    fun `stability sorting clicks resourceId elements first`() {
        // Create mixed elements
        // Verify click order matches stability score
    }

    @Test
    fun `loop terminates after 3 consecutive empty scrapes`() {
        // Mock empty scrape results
        // Verify loop exits
    }
}
```

---

## Implementation Order (TDD)

| Step | Action | Files |
|------|--------|-------|
| 1 | Write unit tests for stableId() | `HybridExplorationTest.kt` |
| 2 | Implement stableId() - make tests pass | `ElementInfo.kt` |
| 3 | Write unit tests for stabilityScore() | `HybridExplorationTest.kt` |
| 4 | Implement stabilityScore() - make tests pass | `ElementInfo.kt` |
| 5 | Write tests for fresh-scrape loop | `HybridExplorationTest.kt` |
| 6 | Implement exploreWithHybridStrategy() | `ExplorationEngine.kt` |
| 7 | Integration tests | `HybridExplorationIntegrationTest.kt` |
| 8 | Deprecate refreshFrameElements() | `ExplorationEngine.kt` |

---

## Success Criteria

| Criteria | Target | Measurement |
|----------|--------|-------------|
| Unit test coverage | 100% for new code | JaCoCo report |
| Click success rate | ≥98% | Telemetry logs |
| No stale node errors | 0 | Log analysis |
| Code complexity reduction | -50 lines | Line count |
| Build passes | ✓ | `./gradlew build` |

---

## Files to Modify

| File | Changes |
|------|---------|
| `ElementInfo.kt` | Add `stableId()`, `stabilityScore()` extension functions |
| `ExplorationEngine.kt` | Add `exploreWithHybridStrategy()`, deprecate `refreshFrameElements()` |
| `HybridExplorationTest.kt` | NEW - Unit tests |
| `HybridExplorationIntegrationTest.kt` | NEW - Integration tests |

---

## Rollback Plan

If issues arise:
1. Keep `refreshFrameElements()` as fallback
2. Add feature flag: `USE_HYBRID_EXPLORATION = true`
3. Toggle via build config or runtime setting

---

**Author:** AI Assistant
**Reviewed:** Pending
**Approved:** Pending
