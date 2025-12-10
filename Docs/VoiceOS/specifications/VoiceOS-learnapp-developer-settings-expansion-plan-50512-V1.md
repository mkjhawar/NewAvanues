# Implementation Plan: LearnApp Developer Settings Expansion

**Version:** 1.0
**Date:** 2025-12-05
**Author:** CCA (Claude Code Assistant)
**Mode:** .swarm .yolo

---

## Overview

| Attribute | Value |
|-----------|-------|
| Platforms | Android |
| Swarm Recommended | Yes (7 phases, 30+ tasks) |
| Estimated Tasks | 35 |
| Estimated Time (Sequential) | 8 hours |
| Estimated Time (Parallel) | 3 hours |
| Savings | 5 hours (62%) |

---

## Goal

Remove ALL hardcoded values from LearnApp and make them configurable via:
1. `LearnAppDeveloperSettings.kt` - Programmatic access
2. Developer Settings UI Screen - Visual configuration

---

## Current State

**Already Configurable (11 settings):**
- maxExplorationDepth, explorationTimeoutMs, loginTimeoutMs
- permissionCheckIntervalMs, pendingRequestExpiryMs
- maxScrollAttempts, scrollDelayMs
- clickRetryAttempts, clickRetryDelayMs
- verboseLogging, screenshotOnScreen

**To Be Added (38 settings across 9 categories):**
1. Exploration & Navigation (7)
2. UI Element Detection & Sizing (3)
3. Detection & Classification (4)
4. Scrolling & Content Discovery (6)
5. JIT Learning & Capture (3)
6. State Detection & Validation (9)
7. Metadata & Quality Scoring (4)
8. Core Processing (2)
9. UI Overlay & Notifications (2)

---

## Phases

### Phase 1: Extend LearnAppDeveloperSettings.kt (Core)

**Agent:** settings-extension-agent

| Task | File | Description |
|------|------|-------------|
| 1.1 | LearnAppDeveloperSettings.kt | Add Exploration & Navigation settings (7) |
| 1.2 | LearnAppDeveloperSettings.kt | Add UI Element Detection settings (3) |
| 1.3 | LearnAppDeveloperSettings.kt | Add Detection & Classification settings (4) |
| 1.4 | LearnAppDeveloperSettings.kt | Add Scrolling settings (6) |
| 1.5 | LearnAppDeveloperSettings.kt | Add JIT Learning settings (3) |
| 1.6 | LearnAppDeveloperSettings.kt | Add State Detection settings (9) |
| 1.7 | LearnAppDeveloperSettings.kt | Add Metadata Quality settings (4) |
| 1.8 | LearnAppDeveloperSettings.kt | Add Core Processing settings (2) |

**Settings to Add:**

```kotlin
// ========== EXPLORATION & NAVIGATION ==========
KEY_ESTIMATED_INITIAL_SCREEN_COUNT = 20
KEY_COMPLETENESS_THRESHOLD_PERCENT = 95f
KEY_BOUNDS_TOLERANCE_PIXELS = 20
KEY_MAX_CONSECUTIVE_CLICK_FAILURES = 5
KEY_MAX_BACK_NAVIGATION_ATTEMPTS = 3
KEY_SCREEN_HASH_SIMILARITY_THRESHOLD = 0.85f
KEY_MIN_ALIAS_TEXT_LENGTH = 3

// ========== UI ELEMENT DETECTION ==========
KEY_MIN_TOUCH_TARGET_SIZE_PIXELS = 48
KEY_BOTTOM_SCREEN_REGION_THRESHOLD_PIXELS = 1600
KEY_BOTTOM_NAV_THRESHOLD_PIXELS = 1600

// ========== DETECTION & CLASSIFICATION ==========
KEY_EXPANSION_WAIT_DELAY_MS = 500L
KEY_EXPANSION_CONFIDENCE_THRESHOLD = 0.65f
KEY_MAX_APP_LAUNCH_EMIT_RETRIES = 3
KEY_APP_LAUNCH_EMIT_RETRY_DELAY_MS = 100L

// ========== SCROLLING ==========
KEY_MAX_ELEMENTS_PER_SCROLLABLE = 20
KEY_MAX_VERTICAL_SCROLL_ITERATIONS = 50
KEY_MAX_HORIZONTAL_SCROLL_ITERATIONS = 20
KEY_MAX_SCROLLABLE_CONTAINER_DEPTH = 2
KEY_MAX_CHILDREN_PER_SCROLL_CONTAINER = 50
KEY_MAX_CHILDREN_PER_CONTAINER_EXPLORATION = 50

// ========== JIT LEARNING ==========
KEY_JIT_CAPTURE_TIMEOUT_MS = 200L
KEY_JIT_MAX_TRAVERSAL_DEPTH = 10
KEY_JIT_MAX_ELEMENTS_CAPTURED = 100

// ========== STATE DETECTION ==========
KEY_TRANSIENT_STATE_DURATION_MS = 500L
KEY_FLICKER_STATE_INTERVAL_MS = 200L
KEY_STABLE_STATE_DURATION_MS = 2000L
KEY_MIN_FLICKER_OCCURRENCES = 3
KEY_FLICKER_DETECTION_WINDOW_MS = 5000L
KEY_PENALTY_MAJOR_CONTRADICTION = 0.3f
KEY_PENALTY_MODERATE_CONTRADICTION = 0.15f
KEY_PENALTY_MINOR_CONTRADICTION = 0.05f
KEY_SECONDARY_STATE_CONFIDENCE_THRESHOLD = 0.5f

// ========== METADATA QUALITY ==========
KEY_QUALITY_WEIGHT_TEXT = 0.3f
KEY_QUALITY_WEIGHT_CONTENT_DESC = 0.25f
KEY_QUALITY_WEIGHT_RESOURCE_ID = 0.3f
KEY_QUALITY_WEIGHT_ACTIONABLE = 0.15f

// ========== CORE PROCESSING ==========
KEY_MAX_COMMAND_BATCH_SIZE = 200
KEY_MIN_GENERATED_LABEL_LENGTH = 2
```

---

### Phase 2: Wire Settings to ExplorationEngine

**Agent:** exploration-wiring-agent

| Task | File | Line(s) | Setting |
|------|------|---------|---------|
| 2.1 | ExplorationEngine.kt | 316 | estimatedInitialScreenCount |
| 2.2 | ExplorationEngine.kt | 369-370 | completenessThresholdPercent |
| 2.3 | ExplorationEngine.kt | 822 | boundsTolerancePixels |
| 2.4 | ExplorationEngine.kt | 912 | maxConsecutiveClickFailures |
| 2.5 | ExplorationEngine.kt | 1616 | maxBackNavigationAttempts |
| 2.6 | ExplorationEngine.kt | 1723, 1752 | screenHashSimilarityThreshold |
| 2.7 | ExplorationEngine.kt | 2290, 2296, 2302 | minAliasTextLength |

---

### Phase 3: Wire Settings to Element Detection

**Agent:** detection-wiring-agent

| Task | File | Line(s) | Setting |
|------|------|---------|---------|
| 3.1 | ElementClassifier.kt | 344 | minTouchTargetSizePixels |
| 3.2 | ElementClassifier.kt | 399 | bottomScreenRegionThresholdPixels |
| 3.3 | ScreenExplorer.kt | 142 | bottomNavThresholdPixels |
| 3.4 | ExpandableControlDetector.kt | 379 | expansionWaitDelayMs |
| 3.5 | ExpandableControlDetector.kt | 387 | expansionConfidenceThreshold |
| 3.6 | AppLaunchDetector.kt | 334 | maxAppLaunchEmitRetries |
| 3.7 | AppLaunchDetector.kt | 335 | appLaunchEmitRetryDelayMs |

---

### Phase 4: Wire Settings to Scrolling & JIT

**Agent:** scrolling-wiring-agent

| Task | File | Line(s) | Setting |
|------|------|---------|---------|
| 4.1 | ScrollExecutor.kt | 59 | maxElementsPerScrollable |
| 4.2 | ScrollExecutor.kt | 68 | maxVerticalScrollIterations |
| 4.3 | ScrollExecutor.kt | 77 | maxHorizontalScrollIterations |
| 4.4 | ScrollExecutor.kt | 87 | maxScrollableContainerDepth |
| 4.5 | ScrollExecutor.kt | 96 | maxChildrenPerScrollContainer |
| 4.6 | ScreenExplorer.kt | 80 | maxChildrenPerContainerExploration |
| 4.7 | JitElementCapture.kt | 88 | jitCaptureTimeoutMs |
| 4.8 | JitElementCapture.kt | 89 | jitMaxTraversalDepth |
| 4.9 | JitElementCapture.kt | 90 | jitMaxElementsCaptured |

---

### Phase 5: Wire Settings to State Detection

**Agent:** state-wiring-agent

| Task | File | Line(s) | Setting |
|------|------|---------|---------|
| 5.1 | TemporalStateValidator.kt | 69 | transientStateDurationMs |
| 5.2 | TemporalStateValidator.kt | 70 | flickerStateIntervalMs |
| 5.3 | TemporalStateValidator.kt | 71 | stableStateDurationMs |
| 5.4 | TemporalStateValidator.kt | 79 | minFlickerOccurrences |
| 5.5 | TemporalStateValidator.kt | 80 | flickerDetectionWindowMs |
| 5.6 | NegativeIndicatorAnalyzer.kt | 51-53 | penaltyMajor/Moderate/Minor |
| 5.7 | MultiStateDetectionEngine.kt | 84 | secondaryStateConfidenceThreshold |
| 5.8 | MetadataQuality.kt | 78-81 | qualityWeight* (4 settings) |
| 5.9 | LearnAppCore.kt | 62-63 | maxCommandBatchSize, minLabelLength |

---

### Phase 6: Create Developer Settings UI Screen

**Agent:** ui-builder-agent

| Task | File | Description |
|------|------|-------------|
| 6.1 | DeveloperSettingsFragment.kt | Create Fragment with category tabs |
| 6.2 | DeveloperSettingsViewModel.kt | Create ViewModel with LiveData |
| 6.3 | fragment_developer_settings.xml | Create layout with RecyclerView |
| 6.4 | item_setting_number.xml | Number input item layout |
| 6.5 | item_setting_toggle.xml | Toggle switch item layout |
| 6.6 | item_setting_slider.xml | Slider item layout for percentages |
| 6.7 | SettingsAdapter.kt | RecyclerView adapter for settings |
| 6.8 | SettingItem.kt | Data class for setting items |
| 6.9 | strings.xml | Add setting labels and descriptions |
| 6.10 | Navigation integration | Add to settings menu |

**UI Categories (Tabs):**
1. Exploration
2. Scrolling
3. Detection
4. State Analysis
5. UI & Timing
6. Debug

---

### Phase 7: Testing & Documentation

**Agent:** testing-agent

| Task | File | Description |
|------|------|-------------|
| 7.1 | LearnAppDeveloperSettingsTest.kt | Unit tests for all getters/setters |
| 7.2 | DeveloperSettingsViewModelTest.kt | ViewModel tests |
| 7.3 | SettingsWiringIntegrationTest.kt | Verify settings affect behavior |
| 7.4 | developer-manual.md | Update with all new settings |
| 7.5 | user-manual.md | Add Developer Settings UI section |

---

## New Settings Summary (38 Total)

| Category | Count | Settings |
|----------|-------|----------|
| Exploration & Navigation | 7 | initialScreenCount, completenessThreshold, boundsTolerance, maxClickFailures, maxBackAttempts, hashSimilarity, minAliasLength |
| UI Element Detection | 3 | minTouchTarget, bottomScreenRegion, bottomNavThreshold |
| Detection & Classification | 4 | expansionDelay, expansionConfidence, launchRetries, launchRetryDelay |
| Scrolling | 6 | maxElementsPerScrollable, maxVertical/HorizontalIterations, maxDepth, maxChildren (2) |
| JIT Learning | 3 | captureTimeout, maxDepth, maxElements |
| State Detection | 9 | transient/flicker/stableDuration, flickerOccurrences/Window, penalties (3), secondaryThreshold |
| Metadata Quality | 4 | weights for text/contentDesc/resourceId/actionable |
| Core Processing | 2 | maxBatchSize, minLabelLength |

---

## Swarm Agent Assignments

| Phase | Agent | Parallelizable With |
|-------|-------|---------------------|
| 1 | settings-extension-agent | - |
| 2 | exploration-wiring-agent | 3, 4, 5 |
| 3 | detection-wiring-agent | 2, 4, 5 |
| 4 | scrolling-wiring-agent | 2, 3, 5 |
| 5 | state-wiring-agent | 2, 3, 4 |
| 6 | ui-builder-agent | After 1 |
| 7 | testing-agent | After 2-6 |

**Execution Order:**
1. Phase 1 (sequential - foundation)
2. Phases 2-5 (parallel - wiring)
3. Phase 6 (parallel with 2-5 after Phase 1)
4. Phase 7 (sequential - final)

---

## Risk Assessment

| Risk | Mitigation |
|------|------------|
| Breaking existing functionality | All defaults match current hardcoded values |
| Performance impact of SharedPreferences | Cache values in memory, read once per session |
| UI complexity | Group by category, use tabs for organization |
| Test coverage | Create comprehensive unit tests before wiring |

---

## Success Criteria

- [ ] All 38 new settings added to LearnAppDeveloperSettings.kt
- [ ] All hardcoded values replaced with settings getters
- [ ] Developer Settings UI accessible from app settings
- [ ] All settings have validation bounds
- [ ] Unit tests pass for all settings
- [ ] Documentation updated with all settings
- [ ] Build successful with no regressions

---

## Files Modified/Created

**Modified:**
- LearnAppDeveloperSettings.kt (38 new settings)
- ExplorationEngine.kt (7 wiring points)
- ElementClassifier.kt (2 wiring points)
- ScreenExplorer.kt (2 wiring points)
- ExpandableControlDetector.kt (2 wiring points)
- AppLaunchDetector.kt (2 wiring points)
- ScrollExecutor.kt (5 wiring points)
- JitElementCapture.kt (3 wiring points)
- TemporalStateValidator.kt (5 wiring points)
- NegativeIndicatorAnalyzer.kt (3 wiring points)
- MultiStateDetectionEngine.kt (1 wiring point)
- MetadataQuality.kt (4 wiring points)
- LearnAppCore.kt (2 wiring points)
- developer-manual.md
- user-manual.md

**Created:**
- DeveloperSettingsFragment.kt
- DeveloperSettingsViewModel.kt
- fragment_developer_settings.xml
- item_setting_number.xml
- item_setting_toggle.xml
- item_setting_slider.xml
- SettingsAdapter.kt
- SettingItem.kt
- LearnAppDeveloperSettingsTest.kt
- DeveloperSettingsViewModelTest.kt
