# Implementation Plan: Voice Command Element Persistence

**Spec:** `specs/voice-command-element-persistence-spec.md`
**Created:** 2025-12-01
**Framework:** IDEACODE v10.1 `/plan`

---

## Overview

| Attribute | Value |
|-----------|-------|
| **Platforms** | Android only |
| **KMP Required** | No |
| **Swarm Recommended** | Yes (15+ tasks) |
| **Total Tasks** | 24 |
| **Priority** | P0 (Critical) |

---

## Time Estimates

| Mode | Duration | Notes |
|------|----------|-------|
| **Sequential** | ~12 hours | Single developer |
| **Parallel (Swarm)** | ~5 hours | 3-4 agents |
| **Savings** | 7 hours (58%) | Recommended |

---

## Phase Ordering Rationale (.cot)

```
Phase 1: JIT Element Capture ─────────────────┐
         (Foundation - must be first)          │
                                               ▼
Phase 2: Command Generation ──────────────────┤ Can parallelize
         (Depends on Phase 1 elements)         │ Phases 2 & 3
                                               ▼
Phase 3: Tier 3 Implementation ───────────────┤
         (Independent of Phase 2)              │
                                               ▼
Phase 4: Integration & Testing ───────────────┤ Sequential
         (Depends on Phases 1-3)               │
                                               ▼
Phase 5: Performance Optimization ────────────┤
         (After integration works)             │
                                               ▼
Phase 6: AVA Integration Prep ────────────────┘
         (Future-proofing, P2)
```

---

## Phase 1: JIT Element Capture (P0)

**Goal:** Make JIT capture actual UI elements, not just screen metadata.
**Duration:** ~3 hours (sequential) | ~1.5 hours (parallel)
**Dependencies:** None

### Tasks

| ID | Task | File | Est. |
|----|------|------|------|
| 1.1 | Add `IVoiceOSService` reference to JustInTimeLearner constructor | `jit/JustInTimeLearner.kt` | 15m |
| 1.2 | Create `JitElementCapture` class with `captureScreenElements()` | `jit/JitElementCapture.kt` (NEW) | 45m |
| 1.3 | Implement `traverseAndCapture()` with depth limit (max 10) | `jit/JitElementCapture.kt` | 30m |
| 1.4 | Add actionable-only filter (clickable, editable, scrollable) | `jit/JitElementCapture.kt` | 20m |
| 1.5 | Add 50ms timeout guard with early termination | `jit/JitElementCapture.kt` | 15m |
| 1.6 | Integrate capture into `learnCurrentScreen()` | `jit/JustInTimeLearner.kt` | 30m |
| 1.7 | Persist elements to `scraped_element` via DatabaseManager | `jit/JustInTimeLearner.kt` | 30m |
| 1.8 | Update `ScreenState.elementCount` to actual captured count | `jit/JustInTimeLearner.kt` | 15m |

### Code Pattern (from existing AccessibilityScrapingIntegration)

```kotlin
// Reference: AccessibilityScrapingIntegration.kt:875
private suspend fun scrapeNode(
    node: AccessibilityNodeInfo,
    appId: String,
    depth: Int,
    maxDepth: Int
): ScrapedElementEntity? {
    if (depth > maxDepth) return null

    // Only capture actionable elements
    if (!node.isClickable && !node.isEditable && !node.isScrollable) {
        // Still traverse children
        node.forEachChild { child ->
            scrapeNode(child, appId, depth + 1, maxDepth)
        }
        return null
    }

    // Create element entity...
}
```

### Acceptance Criteria
- [ ] `elementCount > 0` in ScreenState for screens with actionable elements
- [ ] Elements appear in `scraped_element` table after JIT learning
- [ ] Capture completes within 50ms per screen
- [ ] No memory leaks (nodes properly recycled)

---

## Phase 2: Command Generation (P0)

**Goal:** Auto-generate voice commands from captured elements.
**Duration:** ~2 hours (sequential) | ~1 hour (parallel)
**Dependencies:** Phase 1 (element capture)

### Tasks

| ID | Task | File | Est. |
|----|------|------|------|
| 2.1 | Add `generateCommandsForElements()` to learnapp CommandGenerator | `generation/CommandGenerator.kt` | 30m |
| 2.2 | Extract command text from element text/contentDescription | `generation/CommandGenerator.kt` | 20m |
| 2.3 | Generate action type from element capabilities | `generation/CommandGenerator.kt` | 20m |
| 2.4 | Add basic synonyms (click/tap, hold/long press) | `generation/CommandGenerator.kt` | 15m |
| 2.5 | Persist to `commands_generated` table | `generation/CommandGenerator.kt` | 20m |
| 2.6 | Call command generation after element capture in JIT | `jit/JustInTimeLearner.kt` | 15m |

### Command Generation Logic

```kotlin
fun generateCommandsForElements(elements: List<ScrapedElementEntity>): List<GeneratedCommand> {
    return elements.mapNotNull { element ->
        // Priority: text > contentDescription > viewId
        val label = element.text
            ?: element.contentDescription
            ?: element.viewIdResourceName?.substringAfterLast("/")
            ?: return@mapNotNull null

        // Action based on capabilities
        val action = when {
            element.isClickable -> "click"
            element.isEditable -> "type"
            element.isScrollable -> "scroll"
            else -> return@mapNotNull null
        }

        GeneratedCommand(
            elementHash = element.elementHash,
            commandText = "$action $label",
            actionType = action,
            confidence = 0.9f,
            synonyms = generateSynonyms(action, label)
        )
    }
}
```

### Acceptance Criteria
- [ ] Commands generated for all actionable elements with labels
- [ ] Commands appear in `commands_generated` table
- [ ] Synonyms include common variations (click/tap, etc.)

---

## Phase 3: Tier 3 Implementation (P0)

**Goal:** Create real-time property-based element search (not just text).
**Duration:** ~3 hours (sequential) | ~1.5 hours (parallel)
**Dependencies:** None (can run parallel with Phase 2)

### Tasks

| ID | Task | File | Est. |
|----|------|------|------|
| 3.1 | Create `ElementSearchEngine` class | `scraping/ElementSearchEngine.kt` (NEW) | 30m |
| 3.2 | Create `ElementSearchCriteria` data class | `scraping/ElementSearchCriteria.kt` (NEW) | 15m |
| 3.3 | Implement `findByViewId()` - highest priority | `scraping/ElementSearchEngine.kt` | 30m |
| 3.4 | Implement `findByBoundsAndText()` - medium priority | `scraping/ElementSearchEngine.kt` | 30m |
| 3.5 | Implement `findByClassAndDesc()` - fallback | `scraping/ElementSearchEngine.kt` | 30m |
| 3.6 | Add 20ms timeout with `withTimeoutOrNull` | `scraping/ElementSearchEngine.kt` | 15m |
| 3.7 | Integrate into VoiceCommandProcessor as Tier 3 | `scraping/VoiceCommandProcessor.kt` | 30m |

### ElementSearchCriteria

```kotlin
data class ElementSearchCriteria(
    val viewIdResourceName: String? = null,  // Priority 1
    val bounds: Rect? = null,                // Priority 2 (with text)
    val text: String? = null,                // Priority 2
    val className: String? = null,           // Priority 3 (with desc)
    val contentDescription: String? = null,  // Priority 3
    val elementHash: String? = null          // Optional: exact match
)
```

### Search Priority Order

| Priority | Method | Reliability | Speed |
|----------|--------|-------------|-------|
| 1 | ViewId resource name | 95% | Fast |
| 2 | Bounds + Text combo | 85% | Medium |
| 3 | Class + ContentDesc | 70% | Slow |
| 4 | Text only (existing) | 60% | Fast |

### Acceptance Criteria
- [ ] `findByViewId()` finds elements by resource ID
- [ ] `findByBoundsAndText()` finds elements by location + text
- [ ] Total Tier 3 search completes within 20ms
- [ ] Integrated into command pipeline after Tier 2

---

## Phase 4: Integration & Testing (P0)

**Goal:** Connect all components and verify end-to-end flow.
**Duration:** ~2 hours
**Dependencies:** Phases 1, 2, 3

### Tasks

| ID | Task | File | Est. |
|----|------|------|------|
| 4.1 | Wire JitElementCapture into JustInTimeLearner | `jit/JustInTimeLearner.kt` | 20m |
| 4.2 | Wire ElementSearchEngine into VoiceCommandProcessor | `scraping/VoiceCommandProcessor.kt` | 20m |
| 4.3 | Update `executeTieredCommand()` to include Tier 3 | `scraping/VoiceCommandProcessor.kt` | 20m |
| 4.4 | Create unit test for JitElementCapture | `test/.../JitElementCaptureTest.kt` | 30m |
| 4.5 | Create unit test for ElementSearchEngine | `test/.../ElementSearchEngineTest.kt` | 30m |
| 4.6 | Create integration test for full pipeline | `androidTest/.../VoiceCommandPipelineTest.kt` | 30m |

### Test Scenarios

| # | Scenario | Expected Result |
|---|----------|-----------------|
| 1 | JIT learns screen with 10 buttons | `elementCount = 10`, 10 commands generated |
| 2 | Voice command "click submit" | Tier 2 finds element by hash |
| 3 | Voice command on unlearned app | Tier 3 finds element by viewId |
| 4 | Performance benchmark | < 100ms end-to-end |

### Acceptance Criteria
- [ ] All unit tests pass
- [ ] Integration test demonstrates full flow
- [ ] No regressions in existing functionality

---

## Phase 5: Performance Optimization (P1)

**Goal:** Ensure <100ms end-to-end performance target.
**Duration:** ~1.5 hours
**Dependencies:** Phase 4

### Tasks

| ID | Task | File | Est. |
|----|------|------|------|
| 5.1 | Add performance logging with timestamps | Various | 20m |
| 5.2 | Profile element capture timing | `jit/JitElementCapture.kt` | 20m |
| 5.3 | Add LRU element cache (max 100 elements/app) | `jit/JitElementCache.kt` (NEW) | 30m |
| 5.4 | Optimize tree traversal with early termination | `jit/JitElementCapture.kt` | 20m |
| 5.5 | Add depth limit configuration (default: 10) | `jit/JitElementCapture.kt` | 10m |

### Performance Budget Allocation

| Component | Budget | Current | After Opt |
|-----------|--------|---------|-----------|
| Tier 1 (CommandManager) | 5ms | ~5ms | ~5ms |
| Tier 2 (DB query + hash) | 30ms | N/A | ~25ms |
| Tier 3 (tree search) | 20ms | ~50-200ms | ~15ms |
| Action execution | 10ms | ~5ms | ~5ms |
| **Buffer** | 35ms | - | ~50ms |
| **TOTAL** | 100ms | N/A | ~50ms |

### Acceptance Criteria
- [ ] 95th percentile latency < 100ms
- [ ] Element capture < 50ms per screen
- [ ] Memory usage < 5MB cache per app

---

## Phase 6: AVA Integration Prep (P2)

**Goal:** Prepare element data for AVA NLU integration.
**Duration:** ~1.5 hours
**Dependencies:** Phases 1-5 complete

### Tasks

| ID | Task | File | Est. |
|----|------|------|------|
| 6.1 | Add semantic role detection (button, input, nav) | `jit/SemanticRoleDetector.kt` (NEW) | 30m |
| 6.2 | Create AVA-compatible element export format | `export/AvaElementExporter.kt` (NEW) | 30m |
| 6.3 | Add confidence scoring for element matches | `scraping/ElementSearchEngine.kt` | 20m |
| 6.4 | Document AVA integration API | `docs/ava-integration.md` | 20m |

### AVA Export Format

```kotlin
data class AvaElementData(
    val elementId: String,           // Unique identifier
    val label: String,               // Human-readable label
    val semanticRole: String,        // button, input, link, nav
    val actions: List<String>,       // click, type, scroll
    val confidence: Float,           // Match confidence 0.0-1.0
    val bounds: Rect,                // Screen coordinates
    val contextHints: List<String>   // Parent labels, screen context
)
```

### Acceptance Criteria
- [ ] Elements have semantic role classification
- [ ] Export format documented for AVA team
- [ ] Confidence scores reflect match quality

---

## Swarm Agent Assignment

| Agent | Phases | Tasks | Parallel With |
|-------|--------|-------|---------------|
| `kotlin-expert` | 1, 2 | 1.2-1.5, 2.1-2.5 | Agent 2 |
| `android-expert` | 3 | 3.1-3.7 | Agent 1 |
| `test-specialist` | 4 | 4.4-4.6 | After 1-3 |
| `performance-analyzer` | 5 | 5.1-5.5 | After 4 |
| `doc-writer` | 6 | 6.4 | After 5 |

### Parallel Execution Timeline

```
Hour 0-2: ┌── kotlin-expert: Phase 1 (JIT capture)
          └── android-expert: Phase 3 (Tier 3)

Hour 2-3: ┌── kotlin-expert: Phase 2 (commands)
          └── android-expert: Phase 3 (cont.)

Hour 3-4: └── test-specialist: Phase 4 (testing)

Hour 4-5: └── performance-analyzer: Phase 5 (optimization)

Hour 5+:  └── doc-writer: Phase 6 (AVA prep)
```

---

## Files to Create/Modify

### New Files (5)

| File | Purpose |
|------|---------|
| `jit/JitElementCapture.kt` | Element capture from accessibility tree |
| `scraping/ElementSearchEngine.kt` | Tier 3 property-based search |
| `scraping/ElementSearchCriteria.kt` | Search criteria data class |
| `jit/JitElementCache.kt` | LRU cache for elements |
| `jit/SemanticRoleDetector.kt` | Element role classification |

### Modified Files (4)

| File | Changes |
|------|---------|
| `jit/JustInTimeLearner.kt` | Add element capture, command generation |
| `generation/CommandGenerator.kt` | Add `generateCommandsForElements()` |
| `scraping/VoiceCommandProcessor.kt` | Add Tier 3 integration |
| `models/ScreenState.kt` | Ensure `elementCount` is populated |

### Test Files (3)

| File | Purpose |
|------|---------|
| `test/JitElementCaptureTest.kt` | Unit tests for capture |
| `test/ElementSearchEngineTest.kt` | Unit tests for Tier 3 |
| `androidTest/VoiceCommandPipelineTest.kt` | Integration tests |

---

## Risk Mitigation

| Risk | Mitigation | Owner |
|------|------------|-------|
| Element capture > 50ms | Depth limit, actionable filter | kotlin-expert |
| Hash instability | Multi-property fallback search | android-expert |
| Memory pressure | LRU cache with limits | performance-analyzer |
| Battery drain | Debounce, idle detection | kotlin-expert |

---

## Definition of Done

- [ ] All P0 tasks completed (Phases 1-4)
- [ ] All unit tests pass
- [ ] Integration test demonstrates end-to-end flow
- [ ] Performance benchmark shows < 100ms
- [ ] Code reviewed and merged
- [ ] Developer manual updated

---

## Next Steps

| Option | Command | Description |
|--------|---------|-------------|
| **Generate Tasks** | `/tasks` | Create task list for tracking |
| **Implement** | `/implement .yolo .swarm` | Start implementation with swarm |
| **Stop** | (none) | Review plan first |

---

**Plan Generated:** 2025-12-01
**Framework:** IDEACODE v10.1
**Command:** `/plan specs/voice-command-element-persistence-spec.md`
