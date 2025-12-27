# Implementation Plan: LearnApp Code Fixes

**Source:** `learnapp-code-analysis-251205.md`
**Date:** 2025-12-05
**Mode:** `.cot .yolo .swarm`

---

## Overview

| Metric | Value |
|--------|-------|
| Platforms | Android only |
| Total Tasks | 18 |
| Phases | 3 |
| Swarm Agents | 4 |
| Sequential Est. | 16 hours |
| Parallel Est. | 6 hours |
| Time Savings | 62% |

---

## Phase 1: BLOCKING Issues (P0)

**Goal:** Unblock core functionality

### Agent 1: Developer Settings Navigation

| Task | File | Description |
|------|------|-------------|
| 1.1 | NEW: DeveloperSettingsActivity.kt | Create activity to host fragment |
| 1.2 | AndroidManifest.xml | Register new activity |
| 1.3 | LearnAppIntegration.kt | Add method to launch settings |
| 1.4 | Menu integration | Add developer settings menu option |

**Deliverable:** DeveloperSettingsFragment accessible at runtime

### Agent 2: AIContextSerializer Implementation

| Task | File | Description |
|------|------|-------------|
| 2.1 | AIContextSerializer.kt | Implement deserializeContext() JSON parsing |
| 2.2 | AIContextSerializer.kt | Add unit tests for serialization round-trip |

**Deliverable:** AI context saves and restores correctly

### Agent 3: MetadataNotificationExample Unblock

| Task | File | Description |
|------|------|-------------|
| 3.1 | MetadataNotificationExample.kt:139-145 | Uncomment database save |
| 3.2 | MetadataNotificationExample.kt:180 | Uncomment exploration resume |
| 3.3 | MetadataNotificationExample.kt | Verify dependencies are wired |

**Deliverable:** Exploration metadata persists and resumes work

### Agent 4: Constructor Signature Fixes

| Task | File | Description |
|------|------|-------------|
| 4.1 | ExplorationEngine.kt:168 | Fix ExpandableControlDetector(context) |
| 4.2 | ExplorationEngine.kt:142 | Fix ScrollExecutor(context) |
| 4.3 | ExplorationEngine.kt:137-138 | Fix ScreenExplorer(context) |

**Deliverable:** Clean dependency injection pattern

---

## Phase 2: CRITICAL Issues (P1)

**Goal:** Wire settings to actual code

### Agent 1: HybridCLiteExplorationStrategy Settings Wiring

| Task | File | Description |
|------|------|-------------|
| 5.1 | HybridCLiteExplorationStrategy.kt | Inject LearnAppDeveloperSettings |
| 5.2 | HybridCLiteExplorationStrategy.kt | Replace MAX_CLICK_ATTEMPTS with settings |
| 5.3 | HybridCLiteExplorationStrategy.kt | Replace CLICK_DELAY_MS with settings |
| 5.4 | HybridCLiteExplorationStrategy.kt | Replace SCROLL_DELAY_MS with settings |
| 5.5 | HybridCLiteExplorationStrategy.kt | Remove hardcoded companion object constants |

**Deliverable:** Strategy respects Developer Settings UI values

### Agent 2: ExplorationEngine delay() Fixes

| Task | File | Description |
|------|------|-------------|
| 6.1 | ExplorationEngine.kt:234 | Replace delay(300) with settings.getClickDelayMs() |
| 6.2 | ExplorationEngine.kt:267 | Replace delay(500) with settings.getScrollDelayMs() |
| 6.3 | ExplorationEngine.kt:312 | Replace delay(1000) with settings.getScreenProcessingDelayMs() |

**Deliverable:** ExplorationEngine uses configurable delays

### Agent 3: ScreenExplorer/ScrollExecutor delay() Fixes

| Task | File | Description |
|------|------|-------------|
| 7.1 | ScreenExplorer.kt:145 | Replace delay(200) with settings |
| 7.2 | ScreenExplorer.kt:178 | Replace delay(500) with settings |
| 7.3 | ScrollExecutor.kt:89 | Replace delay(300) with settings |
| 7.4 | ScrollExecutor.kt:134 | Replace delay(500) with settings |
| 7.5 | JitElementCapture.kt:67 | Replace delay(100) with settings |

**Deliverable:** All delay() calls use configurable settings

### Agent 4: Debug Settings Wiring

| Task | File | Description |
|------|------|-------------|
| 8.1 | ExplorationEngine.kt | Add isVerboseLoggingEnabled() checks |
| 8.2 | ScreenExplorer.kt | Add isVerboseLoggingEnabled() checks |
| 8.3 | LearnAppCore.kt | Add isScreenshotOnScreenEnabled() usage |
| 8.4 | LearnAppIntegration.kt | Add isDebugOverlayEnabled() check |

**Deliverable:** Debug settings are functional

---

## Phase 3: HIGH Priority (P2)

**Goal:** Complete configurability

### Sequential Tasks

| Task | File | Description |
|------|------|-------------|
| 9.1 | LearnAppDeveloperSettings.kt | Add 6 missing settings (dialog dimensions, etc.) |
| 9.2 | DeveloperSettingsViewModel.kt | Wire new settings to UI |
| 9.3 | LearnAppCore.kt:330 | Implement batch insert for performance |

---

## Verification Checklist

After implementation:

- [ ] DeveloperSettingsFragment accessible from app
- [ ] All 49+ settings visible and editable
- [ ] Settings changes immediately affect exploration behavior
- [ ] AI context round-trips correctly (serialize/deserialize)
- [ ] Exploration metadata persists to database
- [ ] Exploration resumes from saved state
- [ ] Verbose logging toggleable at runtime
- [ ] Screenshot setting functional
- [ ] Debug overlay setting functional
- [ ] No hardcoded delay() values remain
- [ ] Constructor signatures use Context (not AccessibilityService)
- [ ] HybridCLiteExplorationStrategy reads from settings

---

## Swarm Execution Summary

| Phase | Agents | Tasks | Est. Time |
|-------|--------|-------|-----------|
| Phase 1 (P0) | 4 parallel | 10 | 2 hours |
| Phase 2 (P1) | 4 parallel | 14 | 3 hours |
| Phase 3 (P2) | 1 sequential | 3 | 1 hour |
| **Total** | - | **27** | **6 hours** |

**Sequential equivalent:** 16 hours
**Swarm savings:** 10 hours (62%)

---

## File Change Summary

| File | Phase | Changes |
|------|-------|---------|
| NEW: DeveloperSettingsActivity.kt | P1 | Create |
| AndroidManifest.xml | P1 | Add activity |
| ExplorationEngine.kt | P1+P2 | 6 fixes |
| HybridCLiteExplorationStrategy.kt | P2 | 5 fixes |
| AIContextSerializer.kt | P1 | Implement method |
| MetadataNotificationExample.kt | P1 | Uncomment 2 sections |
| ScreenExplorer.kt | P2 | 3 fixes |
| ScrollExecutor.kt | P2 | 2 fixes |
| JitElementCapture.kt | P2 | 1 fix |
| LearnAppCore.kt | P2+P3 | 2 fixes |
| LearnAppIntegration.kt | P1+P2 | 2 additions |
| LearnAppDeveloperSettings.kt | P3 | Add 6 settings |
| DeveloperSettingsViewModel.kt | P3 | Wire settings |

---

**Plan Version:** 1.0
**Generated:** 2025-12-05
**Next:** Auto-chain to implementation (YOLO mode)
