# Implementation Plan: VoiceOS Unit Test Coverage

**Created:** 2025-12-18
**Author:** Claude Code Assistant
**Status:** Ready for Implementation
**Mode:** .yolo .swarm

---

## Overview

| Metric | Value |
|--------|-------|
| Target | 90% coverage for critical components |
| Current Coverage | ~9% (68 test files / 761 source files) |
| Priority Files | 10 critical components (15,000+ LOC) |
| Estimated Effort | 4-6 hours |
| Swarm Recommended | YES (5 parallel agents) |

---

## Critical Components Analysis

### Files Needing Tests (By Priority)

| P | File | LOC | Testability | Effort |
|---|------|-----|-------------|--------|
| P0 | `CommandGenerator.kt` | 726 | HIGH | 45 min |
| P0 | `ActionCoordinator.kt` | 621 | HIGH | 45 min |
| P0 | `JustInTimeLearner.kt` | 898 | MEDIUM | 60 min |
| P1 | `LearnAppCore.kt` | 777 | MEDIUM | 45 min |
| P1 | `SpeechEngineManager.kt` | 812 | MEDIUM | 45 min |
| P1 | `UIScrapingEngine.kt` | 926 | MEDIUM | 60 min |
| P2 | `ExplorationEngine.kt` | 3966 | LOW | 90 min |
| P2 | `LearnAppIntegration.kt` | 1843 | LOW | 60 min |
| P3 | `VoiceOSService.kt` | 2405 | VERY LOW | Skip (Android Context) |
| P3 | `AccessibilityScrapingIntegration.kt` | 1747 | LOW | Skip (Android Context) |

### Testability Criteria

| Rating | Criteria |
|--------|----------|
| HIGH | Pure logic, minimal Android dependencies, clear inputs/outputs |
| MEDIUM | Some Android dependencies, can mock with Mockk |
| LOW | Heavy Android dependencies, complex state |
| VERY LOW | Requires AccessibilityService context |

---

## Phase 1: High Testability Components (P0)

### Task 1.1: CommandGenerator Tests
**File:** `scraping/CommandGenerator.kt`
**Test File:** `src/test/java/.../scraping/CommandGeneratorTest.kt`

**Test Cases:**
```kotlin
class CommandGeneratorTest {
    // Core functionality
    @Test fun `generateCommands returns commands for clickable element`()
    @Test fun `generateCommands handles scrollable containers`()
    @Test fun `generateCommands excludes dangerous elements`()

    // Edge cases
    @Test fun `generateCommands handles empty element list`()
    @Test fun `generateCommands handles null text content`()
    @Test fun `generateCommands deduplicates similar commands`()

    // Synonym generation
    @Test fun `generateSynonyms creates alternatives for common actions`()
    @Test fun `generateSynonyms respects max synonym limit`()
}
```

### Task 1.2: ActionCoordinator Tests
**File:** `accessibility/managers/ActionCoordinator.kt`
**Test File:** `src/test/java/.../managers/ActionCoordinatorTest.kt`

**Test Cases:**
```kotlin
class ActionCoordinatorTest {
    // Action execution
    @Test fun `executeAction returns success for valid click`()
    @Test fun `executeAction returns failure for invalid target`()
    @Test fun `executeAction handles timeout gracefully`()

    // Action mapping
    @Test fun `getAllActions returns complete action list`()
    @Test fun `findMatchingAction returns best match for command`()
    @Test fun `findMatchingAction handles fuzzy matching`()

    // State management
    @Test fun `concurrent action requests are serialized`()
    @Test fun `action queue respects priority order`()
}
```

---

## Phase 2: Medium Testability Components (P1)

### Task 2.1: JustInTimeLearner Tests
**File:** `learnapp/jit/JustInTimeLearner.kt`
**Test File:** `src/test/java/.../jit/JustInTimeLearnerTest.kt`

**Test Cases:**
```kotlin
class JustInTimeLearnerTest {
    // Learning flow
    @Test fun `captureElement creates valid JitCapturedElement`()
    @Test fun `processElement generates hash correctly`()
    @Test fun `shouldLearn returns true for new elements`()
    @Test fun `shouldLearn returns false for already learned`()

    // Hash calculation
    @Test fun `calculateElementHash is deterministic`()
    @Test fun `calculateElementHash ignores position changes`()

    // Metrics
    @Test fun `getMetrics returns accurate capture counts`()
}
```

### Task 2.2: LearnAppCore Tests
**File:** `learnapp/core/LearnAppCore.kt`
**Test File:** `src/test/java/.../core/LearnAppCoreTest.kt`

**Test Cases:**
```kotlin
class LearnAppCoreTest {
    // Initialization
    @Test fun `initialize sets up components correctly`()
    @Test fun `initialize handles missing permissions gracefully`()

    // Element processing
    @Test fun `processElements batches correctly`()
    @Test fun `processElements respects batch size limit`()
    @Test fun `processElements filters dangerous elements`()

    // Configuration
    @Test fun `updateConfiguration applies settings immediately`()
}
```

### Task 2.3: SpeechEngineManager Tests
**File:** `accessibility/speech/SpeechEngineManager.kt`
**Test File:** `src/test/java/.../speech/SpeechEngineManagerTest.kt`

**Test Cases:**
```kotlin
class SpeechEngineManagerTest {
    // Engine lifecycle
    @Test fun `initializeEngine starts selected engine`()
    @Test fun `switchEngine stops current and starts new`()

    // Command handling
    @Test fun `updateCommands refreshes command list`()
    @Test fun `matchCommand returns best match above threshold`()
    @Test fun `matchCommand returns null below threshold`()

    // State management
    @Test fun `speechState emits correct states`()
    @Test fun `startListening transitions to listening state`()
    @Test fun `stopListening transitions to idle state`()
}
```

### Task 2.4: UIScrapingEngine Tests
**File:** `accessibility/extractors/UIScrapingEngine.kt`
**Test File:** `src/test/java/.../extractors/UIScrapingEngineTest.kt`

**Test Cases:**
```kotlin
class UIScrapingEngineTest {
    // Element extraction
    @Test fun `scrapeCurrentScreen extracts all visible elements`()
    @Test fun `scrapeCurrentScreen handles nested hierarchies`()

    // Element classification
    @Test fun `classifyElement identifies buttons correctly`()
    @Test fun `classifyElement identifies text fields correctly`()
    @Test fun `classifyElement identifies scrollable containers`()

    // Filtering
    @Test fun `filterClickableElements excludes decorative views`()
    @Test fun `filterClickableElements includes actionable items`()
}
```

---

## Phase 3: Low Testability Components (P2)

### Task 3.1: ExplorationEngine Tests (Core Logic Only)
**File:** `learnapp/exploration/ExplorationEngine.kt`
**Test File:** `src/test/java/.../exploration/ExplorationEngineTest.kt`

**Test Cases:**
```kotlin
class ExplorationEngineTest {
    // Exploration strategy
    @Test fun `calculateNextAction prioritizes unexplored areas`()
    @Test fun `calculateNextAction avoids dangerous actions`()

    // State tracking
    @Test fun `markScreenExplored updates exploration state`()
    @Test fun `getExplorationProgress returns accurate percentage`()

    // Graph building
    @Test fun `buildNavigationGraph creates valid edges`()
    @Test fun `detectCycles identifies circular navigation`()
}
```

---

## Test Infrastructure

### Mock Setup (Shared)

```kotlin
// TestFixtures.kt
object TestFixtures {
    fun createMockAccessibilityNodeInfo(
        className: String = "android.widget.Button",
        text: String? = "Click me",
        isClickable: Boolean = true
    ): AccessibilityNodeInfo = mockk {
        every { this@mockk.className } returns className
        every { this@mockk.text } returns text
        every { this@mockk.isClickable } returns isClickable
    }

    fun createMockElementInfo(
        id: String = "element_1",
        text: String = "Button",
        type: ElementType = ElementType.BUTTON
    ): ElementInfo = ElementInfo(id, text, type, Rect(0, 0, 100, 50))
}
```

### Dependencies Required

```kotlin
// build.gradle.kts additions
testImplementation("io.mockk:mockk:1.13.8")
testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
testImplementation("app.cash.turbine:turbine:1.0.0") // Flow testing
```

---

## Swarm Assignment

| Agent | Tasks | Files | Effort |
|-------|-------|-------|--------|
| Agent 1 | 1.1 | CommandGeneratorTest | 45 min |
| Agent 2 | 1.2 | ActionCoordinatorTest | 45 min |
| Agent 3 | 2.1-2.2 | JustInTimeLearnerTest, LearnAppCoreTest | 90 min |
| Agent 4 | 2.3-2.4 | SpeechEngineManagerTest, UIScrapingEngineTest | 90 min |
| Agent 5 | 3.1 | ExplorationEngineTest (partial) | 60 min |

---

## Execution Order

```
Phase 1 (P0) ──┬── Agent 1: CommandGeneratorTest
               └── Agent 2: ActionCoordinatorTest
                        │
                        ▼
Phase 2 (P1) ──┬── Agent 3: JIT + LearnAppCore
               └── Agent 4: Speech + UIScraping
                        │
                        ▼
Phase 3 (P2) ───── Agent 5: ExplorationEngine
                        │
                        ▼
               Run All Tests
                        │
                        ▼
               Commit & Push
```

---

## Success Criteria

| Criterion | Target |
|-----------|--------|
| New test files created | 8 |
| Test cases added | 50+ |
| All tests pass | 100% |
| No mocking leaks | Verified |
| Build passes | `./gradlew test` |

---

## Files to Create

| # | File Path |
|---|-----------|
| 1 | `src/test/java/.../scraping/CommandGeneratorTest.kt` |
| 2 | `src/test/java/.../managers/ActionCoordinatorTest.kt` |
| 3 | `src/test/java/.../jit/JustInTimeLearnerTest.kt` |
| 4 | `src/test/java/.../core/LearnAppCoreTest.kt` |
| 5 | `src/test/java/.../speech/SpeechEngineManagerTest.kt` |
| 6 | `src/test/java/.../extractors/UIScrapingEngineTest.kt` |
| 7 | `src/test/java/.../exploration/ExplorationEngineTest.kt` |
| 8 | `src/test/java/.../TestFixtures.kt` |

---

## Risk Assessment

| Risk | Mitigation |
|------|------------|
| Mockk complexity | Use simple mocks, avoid deep nesting |
| Coroutine testing | Use runTest and Turbine for Flows |
| Android dependencies | Extract testable logic to pure functions |
| Flaky tests | Avoid time-dependent assertions |

---

**Ready for Implementation**
