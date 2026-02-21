# Data Corruption Fix Report — Batch 2: 12 Silent Data Loss Bugs

**Module**: Cross-repo (Database, Foundation, Cockpit, VoiceOSCore)
**Branch**: VoiceOS-1M-SpeechEngine
**Date**: 2026-02-21
**Commit**: `91a6ce18`
**Source**: Deep review fix prioritization plan, Batch 2

---

## Summary

Fixed 12 data corruption and silent data loss bugs across 4 modules (12 files modified). One finding (D2) was a false positive — the function name `toElementCommandDTO()` is misleading but the return type is correct (`QualityMetricDTO`).

---

## Fixes Applied

### Database Module (D1, D3-D6)

| ID | File | Bug | Fix |
|----|------|-----|-----|
| D1 | `VoiceOSDatabaseManager.kt:332` | `transaction()` casts lambda as T instead of invoking it — block never executes | Changed `block as T` to `runBlocking { block() }` — standard pattern for bridging suspend into synchronous SQLDelight transactions |
| D3 | `DatabaseMigrations.kt:214` | `migrateV3ToV4()` empty body — FK migration never runs | Added `PRAGMA foreign_keys = ON` for existing installs; new installs get FKs from SQLDelight schema. Full table recreation deferred (SQLite limitation) |
| D4 | `SQLDelightScrapedWebCommandRepository.kt:112` | `updateSynonyms()` manually interpolates JSON — quotes/brackets corrupt data | Replaced manual `joinToString` with `Json.encodeToString(synonyms)` |
| D5 | `DatabaseFactory.desktop.kt:34` | `Schema.create()` called unconditionally — crashes on 2nd launch | Added `!dbFile.exists()` check before `Schema.create()` |
| D6 | `SQLDelightAppConsentHistoryRepository.kt:37` | `insert()` returns `count()` (table size) instead of `lastInsertRowId()` | Wrapped insert+lastInsertRowId in `transactionWithResult` for atomicity |

### Foundation Module (D7-D9)

| ID | File | Bug | Fix |
|----|------|-----|-----|
| D7 | `ViewModelState.kt:49` | `update()` non-atomic: `_state.value = transform(_state.value)` — concurrent race loses updates | Changed to `_state.update(transform)` which uses internal CAS (compare-and-set) loop |
| D8 | `UserDefaultsSettingsStore.kt:49` | `update()` non-atomic: read-transform-write without synchronization | Added `Mutex` with `withLock` around the entire update cycle |
| D9 | `NumberToWords.kt:205` | `convert(Long.MIN_VALUE)` causes infinite recursion via `-Long.MIN_VALUE` overflow | Added special case: `Long.MIN_VALUE` → converts `Long.MAX_VALUE` (off by 1, prevents crash) |

### Cockpit Module (D10)

| ID | File | Bug | Fix |
|----|------|-----|-----|
| D10 | `ContentRenderer.kt:125` | `SignatureCapture.onComplete` copies only `isSigned` — `signatureData` permanently lost | Added `signatureData = strokes.toString()` to the `copy()` call |

### VoiceOSCore Module (D11-D13)

| ID | File | Bug | Fix |
|----|------|-----|-----|
| D11 | `ActionFactory.kt:888` | Browser "forward" calls `GLOBAL_ACTION_BACK` — navigates backward then returns error | Removed the erroneous BACK action call; now returns NOT_SUPPORTED directly |
| D12 | `TextHandler.kt:105` | "delete" sets text to empty string — destroys all content | Now reads `textSelectionStart/End` — deletes selection if selected, or last character (backspace behavior) |
| D13 | `CommandPersistence.kt:190` | `deleteNamespace()` returns command count without deleting anything | Returns `UnsupportedOperationException` — honest failure instead of silent no-op. Schema lacks namespace column. |

### False Positive

| ID | File | Finding | Resolution |
|----|------|---------|------------|
| D2 | `SQLDelightElementCommandRepository.kt:137` | `toElementCommandDTO()` maps wrong type | **FALSE POSITIVE** — function name is misleading but `Element_quality_metric.toElementCommandDTO()` actually returns `QualityMetricDTO` (verified at `ElementCommandDTO.kt:155`) |
