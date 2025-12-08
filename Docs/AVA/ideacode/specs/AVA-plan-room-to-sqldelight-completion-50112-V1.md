# Implementation Plan: Room to SQLDelight Migration Completion

## Overview
| Property | Value |
|----------|-------|
| Platforms | Android, iOS, Desktop |
| Swarm Recommended | No (sequential dependency chain) |
| Estimated Tasks | 12 tasks |
| Estimated Time | 4-6 hours sequential |

## Phase Ordering (Simplest → Most Complex)

### Phase 1: Test Migration (Simplest - 30 mins)
**Rationale:** Fixes compile errors in test suite, validates SQLDelight migration

| Task | File | Description | Complexity |
|------|------|-------------|------------|
| 1.1 | `DatabaseIntegrationTest.kt` | Replace Room test setup with SQLDelight driver | Low |
| 1.2 | `PerformanceBenchmarkTest.kt` | Replace Room test setup with SQLDelight driver | Low |
| 1.3 | `DatabaseMigrationTest.kt` | Remove Room migration helpers, stub SQLDelight migration | Low |

**Dependencies:** None
**Validation:** `./gradlew :Universal:AVA:Core:Data:testDebugUnitTest`

---

### Phase 2: Crash Reporting Documentation (Simple - 15 mins)
**Rationale:** Document deliberate omission rather than implement Firebase (privacy-first design)

| Task | File | Description | Complexity |
|------|------|-------------|------------|
| 2.1 | `CrashReporter.kt` | Add documentation comments explaining privacy-first approach | Low |
| 2.2 | `docs/AVA-CRASH-REPORTING.md` | Create doc explaining crash reporting architecture | Low |

**Dependencies:** None
**Validation:** Code review

---

### Phase 3: Model Download Implementation (Medium - 2 hours)
**Rationale:** Enables runtime model download, critical for user experience

| Task | File | Description | Complexity |
|------|------|-------------|------------|
| 3.1 | `ModelDownloader.kt` | Implement HTTP download with OkHttp/Ktor | Medium |
| 3.2 | `ModelDownloader.kt` | Add progress tracking and callbacks | Medium |
| 3.3 | `ModelDownloader.kt` | Implement resume/retry logic | Medium |
| 3.4 | `ChecksumHelper.kt` | Generate real SHA256 checksums | Low |
| 3.5 | `DownloadConfig.kt` | Create config for model URLs (HuggingFace) | Low |

**Dependencies:** Network library (OkHttp already in project)
**Validation:** Manual test - download a small model file

---

### Phase 4: Desktop NLU Implementation (Medium - 1 hour)
**Rationale:** Enables desktop platform support

| Task | File | Description | Complexity |
|------|------|-------------|------------|
| 4.1 | `IntentClassifier.kt` (desktop) | Implement ONNX Runtime JVM inference | Medium |
| 4.2 | `BertTokenizer.kt` (desktop) | Implement WordPiece tokenization | Medium |
| 4.3 | `ModelManager.kt` (desktop) | Implement model loading for JVM | Medium |

**Dependencies:** ONNX Runtime JVM dependency
**Validation:** Unit tests with mock model

---

### Phase 5: iOS NLU Implementation (Most Complex - 2 hours)
**Rationale:** Enables iOS platform support with Core ML

| Task | File | Description | Complexity |
|------|------|-------------|------------|
| 5.1 | `IntentClassifier.kt` (iOS) | Implement Core ML model loading | High |
| 5.2 | `IntentClassifier.kt` (iOS) | Implement inference with Core ML | High |
| 5.3 | `ModelManager.kt` (iOS) | Implement model management for iOS | Medium |
| 5.4 | `CoreMLModelManager.kt` | Complete Core ML integration | High |

**Dependencies:** Core ML framework, .mlmodel files
**Validation:** iOS simulator test

---

## Dependency Graph

```
Phase 1 (Tests) ─────────────────────────────────────┐
                                                      │
Phase 2 (Crash Docs) ────────────────────────────────┼──► All Complete
                                                      │
Phase 3 (Model Download) ────────────────────────────┤
                                                      │
Phase 4 (Desktop NLU) ──────► Phase 5 (iOS NLU) ─────┘
```

---

## Time Estimates

| Execution | Time | Notes |
|-----------|------|-------|
| Sequential | 5-6 hours | One developer |
| Parallel (2 devs) | 3-4 hours | Phase 1-3 parallel with 4-5 |

---

## Recommended Execution Order

1. **Phase 1** - Test Migration (immediate, unblocks CI)
2. **Phase 2** - Crash Reporting Docs (quick win)
3. **Phase 3** - Model Download (high user impact)
4. **Phase 4** - Desktop NLU (medium priority)
5. **Phase 5** - iOS NLU (requires iOS dev environment)

---

## Files to Modify

| Phase | Files |
|-------|-------|
| 1 | `DatabaseIntegrationTest.kt`, `PerformanceBenchmarkTest.kt`, `DatabaseMigrationTest.kt` |
| 2 | `CrashReporter.kt`, `docs/AVA-CRASH-REPORTING.md` (new) |
| 3 | `ModelDownloader.kt`, `ChecksumHelper.kt`, `DownloadConfig.kt` (new) |
| 4 | `IntentClassifier.kt` (desktop), `BertTokenizer.kt`, `ModelManager.kt` (desktop) |
| 5 | `IntentClassifier.kt` (iOS), `ModelManager.kt` (iOS), `CoreMLModelManager.kt` |

---

**Plan Created:** 2025-12-01
**Author:** Claude Code
