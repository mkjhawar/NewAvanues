# Voice Command Element Persistence Specification

**Version:** 1.0
**Date:** 2025-12-01
**Status:** Draft
**Author:** AI-Generated (IDEACODE /specify .cot .swarm)

---

## Executive Summary

Implement Tier 2 pre-scraped element matching and Tier 3 real-time accessibility tree search for VoiceOS voice command execution. Currently, JIT (Just-In-Time) learning captures screen metadata but **no UI elements**, resulting in an empty `scraped_element` database and broken voice commands for unlearned apps. This spec defines the work to capture elements during JIT learning and enable fast (<100ms) voice command execution for AVA NLU integration.

---

## Problem Statement

### Current State
| Component | Status | Issue |
|-----------|--------|-------|
| JIT Learning | Saves screens | `elementCount=0` - NO elements captured |
| Tier 1 (CommandManager) | Working | Static system commands only |
| Tier 2 (VoiceCommandProcessor) | Partially broken | Relies on empty `scraped_element` table |
| Tier 3 (Real-time tree search) | Does NOT exist | Only text-based fallback exists |

### Pain Points
1. **JIT doesn't capture elements:** `JustInTimeLearner.saveScreenToDatabase()` creates `ScreenState(elementCount=0)`
2. **Tier 2 fails silently:** `VoiceCommandProcessor.processCommand()` returns "App has not been scraped"
3. **No Tier 3:** Current "real-time search" is text-only via `findNodesByText()`, not property-based
4. **AVA integration blocked:** AVA NLU needs structured element data to map intents to actions

### Desired State
```
User: "Click submit button"
  ↓ (< 100ms total)
Tier 1: CommandManager check (static) → NOT FOUND
  ↓
Tier 2: VoiceCommandProcessor (pre-scraped) → FOUND via JIT-captured elements
  ↓ OR
Tier 3: Real-time tree search (by hash/viewId/bounds) → FOUND
  ↓
Execute action on UI element
```

---

## Chain of Thought Analysis (.cot)

### Why JIT Doesn't Capture Elements

**Root Cause:** `JustInTimeLearner.learnCurrentScreen()` only uses `AccessibilityEvent` properties:
```kotlin
// Current implementation (JustInTimeLearner.kt:270-278)
val screenState = ScreenState(
    hash = screenHash,
    packageName = packageName,
    activityName = event.className?.toString(),
    timestamp = System.currentTimeMillis(),
    elementCount = 0,  // ← HARDCODED ZERO
    isVisited = true,
    depth = 0
)
```

**Why:** AccessibilityEvent doesn't contain the full UI tree. You need `AccessibilityService.rootInActiveWindow` to traverse elements.

### Why Text-Based Search Isn't Tier 3

**Current "real-time search"** (`VoiceCommandProcessor.tryRealtimeElementSearch()`):
- Searches by `findAccessibilityNodeInfosByText(searchText)`
- Falls back to recursive text/contentDescription matching
- Does NOT use:
  - Element hash (stable identifier)
  - ViewId resource name (`com.app:id/submit_button`)
  - Bounds coordinates
  - Semantic role

**True Tier 3** should search by:
1. Element hash (primary)
2. ViewId resource name (secondary)
3. Bounds + text combo (tertiary)
4. Class + content description (fallback)

### Performance Budget

| Operation | Target | Current |
|-----------|--------|---------|
| Voice recognition | 300-500ms | N/A (external) |
| Command matching | < 50ms | ~10ms (when DB has data) |
| Element lookup | < 30ms | N/A (no elements) |
| Tree traversal | < 20ms | ~50-200ms (full tree) |
| Action execution | < 10ms | ~5ms |
| **TOTAL** | **< 100ms** | **N/A** |

---

## Functional Requirements

### FR-1: JIT Element Capture
| ID | Requirement | Priority |
|----|-------------|----------|
| FR-1.1 | JIT must capture UI elements from accessibility tree on screen change | P0 |
| FR-1.2 | Elements must be persisted to `scraped_element` table | P0 |
| FR-1.3 | Only actionable elements (clickable, editable, scrollable) should be captured | P1 |
| FR-1.4 | Element capture must complete within 50ms | P0 |
| FR-1.5 | Duplicate elements (same hash) must be skipped | P1 |

### FR-2: Command Generation
| ID | Requirement | Priority |
|----|-------------|----------|
| FR-2.1 | Auto-generate commands from captured elements | P0 |
| FR-2.2 | Commands must be persisted to `commands_generated` table | P0 |
| FR-2.3 | Command text derived from element text/contentDescription | P0 |
| FR-2.4 | Synonyms generated for common actions (click/tap, press/hold) | P2 |

### FR-3: Tier 2 Enhancement
| ID | Requirement | Priority |
|----|-------------|----------|
| FR-3.1 | VoiceCommandProcessor must query JIT-captured elements | P0 |
| FR-3.2 | Hash-based element lookup must work for JIT apps | P0 |
| FR-3.3 | Fallback to text-based search if hash not found | P1 |

### FR-4: Tier 3 Real-Time Search
| ID | Requirement | Priority |
|----|-------------|----------|
| FR-4.1 | Implement property-based tree search (not just text) | P0 |
| FR-4.2 | Search by viewIdResourceName (most reliable) | P0 |
| FR-4.3 | Search by bounds + text combination | P1 |
| FR-4.4 | Search by className + contentDescription | P2 |
| FR-4.5 | Tier 3 must complete within 20ms | P0 |

### FR-5: AVA NLU Integration Prep
| ID | Requirement | Priority |
|----|-------------|----------|
| FR-5.1 | Element data must include semantic labels for NLU | P1 |
| FR-5.2 | Export format compatible with AVA intent mapping | P2 |
| FR-5.3 | Confidence scoring for element matches | P2 |

---

## Non-Functional Requirements

| Category | Requirement | Metric |
|----------|-------------|--------|
| **Performance** | Total voice command execution | < 100ms |
| **Performance** | JIT element capture per screen | < 50ms |
| **Performance** | Tier 3 tree search | < 20ms |
| **Reliability** | Command success rate | > 95% for scraped apps |
| **Memory** | Element cache size | < 5MB per app |
| **Storage** | Database growth per app | < 1MB for 100 screens |
| **Battery** | JIT impact on battery | < 2% additional drain |

---

## Technical Design

### Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                    Voice Command Pipeline                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  User Voice ──► Speech Recognition ──► Normalized Text           │
│                                              │                   │
│                                              ▼                   │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                    Tier 1: CommandManager                 │   │
│  │  Static system commands (go back, scroll, volume)         │   │
│  │  Time: ~5ms                                               │   │
│  └──────────────────────────────────────────────────────────┘   │
│                              │ NOT FOUND                        │
│                              ▼                                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                Tier 2: VoiceCommandProcessor              │   │
│  │  Pre-scraped elements from database                       │   │
│  │  Sources: Full exploration OR JIT learning                │   │
│  │  Time: ~30ms (DB query + hash lookup)                     │   │
│  └──────────────────────────────────────────────────────────┘   │
│                              │ NOT FOUND                        │
│                              ▼                                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │              Tier 3: Real-Time Element Search             │   │  ◄── NEW
│  │  Live accessibility tree search by properties             │   │
│  │  Search order: viewId > bounds+text > class+desc          │   │
│  │  Time: ~20ms                                               │   │
│  └──────────────────────────────────────────────────────────┘   │
│                              │                                  │
│                              ▼                                  │
│                    Execute Action on Node                       │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### Component Changes

#### 1. JustInTimeLearner Enhancement

**File:** `modules/apps/VoiceOSCore/.../learnapp/jit/JustInTimeLearner.kt`

```kotlin
// NEW: Element capture during JIT learning
private suspend fun captureScreenElements(
    packageName: String,
    screenHash: String
): List<ScrapedElementEntity> {
    return withContext(Dispatchers.Main) {
        val elements = mutableListOf<ScrapedElementEntity>()
        val startTime = System.currentTimeMillis()

        voiceOSService?.rootInActiveWindow?.let { rootNode ->
            try {
                traverseAndCapture(rootNode, packageName, elements, depth = 0)
            } finally {
                rootNode.recycle()
            }
        }

        val elapsed = System.currentTimeMillis() - startTime
        Log.d(TAG, "Captured ${elements.size} elements in ${elapsed}ms")

        // Performance guard: abort if too slow
        if (elapsed > ELEMENT_CAPTURE_TIMEOUT_MS) {
            Log.w(TAG, "Element capture exceeded ${ELEMENT_CAPTURE_TIMEOUT_MS}ms")
        }

        elements
    }
}
```

#### 2. New: ElementSearchEngine (Tier 3)

**File:** `modules/apps/VoiceOSCore/.../scraping/ElementSearchEngine.kt`

```kotlin
/**
 * Tier 3: Real-time element search by multiple properties
 * Searches live accessibility tree without database dependency
 */
class ElementSearchEngine(
    private val accessibilityService: AccessibilityService
) {
    companion object {
        private const val TAG = "ElementSearchEngine"
        private const val SEARCH_TIMEOUT_MS = 20L
    }

    /**
     * Search for element by multiple criteria (priority order)
     */
    suspend fun findElement(criteria: ElementSearchCriteria): AccessibilityNodeInfo? {
        return withTimeoutOrNull(SEARCH_TIMEOUT_MS) {
            val root = accessibilityService.rootInActiveWindow ?: return@withTimeoutOrNull null

            try {
                // Priority 1: ViewId (most reliable)
                criteria.viewIdResourceName?.let { viewId ->
                    findByViewId(root, viewId)?.let { return@withTimeoutOrNull it }
                }

                // Priority 2: Bounds + Text combination
                criteria.bounds?.let { bounds ->
                    criteria.text?.let { text ->
                        findByBoundsAndText(root, bounds, text)?.let { return@withTimeoutOrNull it }
                    }
                }

                // Priority 3: Class + ContentDescription
                criteria.className?.let { className ->
                    criteria.contentDescription?.let { desc ->
                        findByClassAndDesc(root, className, desc)?.let { return@withTimeoutOrNull it }
                    }
                }

                null
            } finally {
                root.recycle()
            }
        }
    }
}
```

#### 3. VoiceCommandProcessor Integration

**File:** `modules/apps/VoiceOSCore/.../scraping/VoiceCommandProcessor.kt`

```kotlin
// ENHANCED: Add Tier 3 after existing text-based fallback
private suspend fun executeTieredCommand(voiceInput: String): CommandResult {
    // Tier 1: Static commands (existing)
    val tier1Result = tryStaticCommand(voiceInput)
    if (tier1Result.success) return tier1Result

    // Tier 2: Pre-scraped elements (existing, now includes JIT)
    val tier2Result = tryDatabaseCommand(voiceInput)
    if (tier2Result.success) return tier2Result

    // Tier 3: Real-time property search (NEW)
    val tier3Result = tryPropertyBasedSearch(voiceInput)
    if (tier3Result.success) return tier3Result

    return CommandResult.failure("Command not recognized")
}
```

### Database Changes

**No schema changes required.** Existing tables support this:

| Table | Usage |
|-------|-------|
| `scraped_element` | JIT-captured elements (same schema as exploration) |
| `commands_generated` | Auto-generated commands from JIT elements |
| `screen_state` | Update `elementCount` from 0 to actual count |

---

## Swarm Assessment (.swarm)

### Recommended Agents

| Agent | Role | Tasks |
|-------|------|-------|
| `kotlin-expert` | Core implementation | ElementSearchEngine, JIT enhancement |
| `android-expert` | Accessibility APIs | AccessibilityNodeInfo traversal |
| `database-expert` | SQLDelight queries | Element persistence optimization |
| `test-specialist` | Testing | Unit tests, performance benchmarks |
| `performance-analyzer` | Optimization | Profile and optimize <100ms target |

### Parallelization Strategy

```
Phase 1 (Parallel):
├── Agent 1: JIT element capture (JustInTimeLearner)
├── Agent 2: ElementSearchEngine (Tier 3)
└── Agent 3: Unit test scaffolding

Phase 2 (Sequential):
└── Integration + Performance tuning

Phase 3 (Parallel):
├── Agent 4: AVA export format
└── Agent 5: Documentation
```

---

## Success Criteria

| # | Criterion | Metric | Verification |
|---|-----------|--------|--------------|
| 1 | JIT captures elements | `elementCount > 0` in ScreenState | DB query |
| 2 | Elements persist to database | `scraped_element` table populated | DB count |
| 3 | Commands auto-generated | `commands_generated` table populated | DB count |
| 4 | Tier 2 works for JIT apps | Hash-based lookup succeeds | Unit test |
| 5 | Tier 3 implemented | Property-based search works | Integration test |
| 6 | Performance < 100ms | End-to-end command execution | Benchmark |
| 7 | Battery impact < 2% | JIT background processing | Power profiler |

---

## Implementation Plan

### Phase 1: JIT Element Capture (P0)
1. Enhance `JustInTimeLearner.learnCurrentScreen()` to call `captureScreenElements()`
2. Implement `traverseAndCapture()` with 50ms timeout guard
3. Persist to `scraped_element` table via existing queries
4. Update `ScreenState.elementCount` to actual count

### Phase 2: Command Generation (P0)
1. Add `CommandGenerator.generateFromElements()` call after element capture
2. Generate commands using element text/contentDescription
3. Persist to `commands_generated` table

### Phase 3: Tier 3 Implementation (P0)
1. Create `ElementSearchEngine` class
2. Implement `findByViewId()`, `findByBoundsAndText()`, `findByClassAndDesc()`
3. Integrate into `VoiceCommandProcessor` after Tier 2 fallback

### Phase 4: Performance Optimization (P1)
1. Profile end-to-end timing
2. Add element caching for frequently accessed screens
3. Optimize tree traversal (early termination, depth limits)

### Phase 5: AVA Integration Prep (P2)
1. Add semantic role detection
2. Export element data in AVA-compatible format
3. Add confidence scoring for element matches

---

## Risks and Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Element capture too slow | > 50ms | Depth limit, actionable-only filter |
| Memory pressure from caching | OOM on low-end devices | LRU cache with size limits |
| Hash instability | Elements not found | Multi-property search fallback |
| Battery drain from JIT | User complaints | Debounce, idle detection |

---

## Dependencies

| Dependency | Type | Status |
|------------|------|--------|
| SQLDelight database | Internal | Available |
| AccessibilityService | Android | Available |
| UUIDCreator fingerprinting | Internal | Available |
| CommandGenerator | Internal | Available |

---

## Related Documents

- [Developer Manual - LearnApp](/docs/modules/learnapp/developer-manual.md)
- [Monorepo Merge Plan](/Volumes/M-Drive/Coding/NewAvanues/specs/monorepo-merge-plan-20251201.md)
- [VoiceCommandProcessor.kt](/modules/apps/VoiceOSCore/.../scraping/VoiceCommandProcessor.kt)
- [JustInTimeLearner.kt](/modules/apps/VoiceOSCore/.../learnapp/jit/JustInTimeLearner.kt)

---

**Spec Generated:** 2025-12-01
**Framework:** IDEACODE v10.1
**Command:** `/specify .cot .swarm`
