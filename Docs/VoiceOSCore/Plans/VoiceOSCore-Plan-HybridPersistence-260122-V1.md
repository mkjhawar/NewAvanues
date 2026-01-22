# Implementation Plan: Hybrid Persistence Strategy

**Feature:** ScrollView Commands Not Persisted - 4-Layer Hybrid Fix + Import/Export
**Date:** 2026-01-22 | **Version:** V2 | **Author:** Claude
**Module:** VoiceOSCore + voiceoscoreng
**Branch:** `VoiceOSCore-ScrapingUpdate`

---

## Overview

| Attribute | Value |
|-----------|-------|
| **Platforms** | KMP (commonMain) + Android |
| **Estimated Tasks** | 28 tasks (18 hybrid + 10 import/export) |
| **Estimated Time** | 10-14 hours (sequential) |
| **Swarm Recommended** | No (single platform focus) |
| **Risk Level** | Medium |
| **Dependencies** | Phase 5 depends on Phase 4 completion |

---

## Chain-of-Thought: Why This Order

```
Phase 1: Core Infrastructure (KMP)
   │
   │  WHY FIRST: All other phases depend on these enums/classes
   │  LOCATION: Modules/VoiceOSCore/src/commonMain/
   │
   ▼
Phase 2: Android Platform Integration
   │
   │  WHY SECOND: PackageManager is Android-specific
   │  LOCATION: android/apps/voiceoscoreng/src/.../service/
   │
   ▼
Phase 3: Integration & Wiring
   │
   │  WHY THIRD: Connects new components to existing flow
   │  MODIFIES: ElementInfo.kt, CommandGenerator.kt, ElementExtractor.kt
   │
   ▼
Phase 4: Testing & Validation
   │
   │  WHY FOURTH: Verify hybrid persistence works correctly
   │  TESTS: Unit tests + manual verification
   │
   ▼
Phase 5: Database Import/Export
   │
   │  WHY LAST: Requires understanding final DB structure
   │  LOCATION: Database module + Settings UI
   │  FEATURES: Export all/partial, Import, Settings UI
   │
   ▼
DONE
```

---

## Tree-of-Thought: Approach Selection

```
┌─────────────────────────────────────────────────────────────────┐
│                    DECISION: Implementation Approach            │
└─────────────────────────────────────────────────────────────────┘

Option A: Big Bang (All at once)
├── Pros: Single commit, atomic change
├── Cons: High risk, hard to debug, no incremental value
└── Verdict: ❌ REJECTED

Option B: Phased Rollout ◄── SELECTED
├── Pros: Incremental value, easy rollback, testable at each phase
├── Cons: Slightly longer total time
└── Verdict: ✅ BEST BALANCE

Option C: Quick Fix First
├── Pros: Immediate unblock for Settings screens
├── Cons: Tech debt, will need refactor later
└── Verdict: 🔄 BACKUP (if time-critical)
```

---

## Phases

### Phase 1: Core Infrastructure (KMP commonMain)

**Goal:** Create the 4-layer classification system in shared KMP code.

**Location:** `Modules/VoiceOSCore/src/commonMain/kotlin/com/augmentalis/voiceoscore/`

| # | Task | File | Description |
|---|------|------|-------------|
| 1.1 | Create AppCategory enum | `AppCategoryClassifier.kt` | Define EMAIL, MESSAGING, SETTINGS, etc. with DynamicBehavior |
| 1.2 | Create package pattern matcher | `AppCategoryClassifier.kt` | Pattern-based classification (gmail, whatsapp, settings) |
| 1.3 | Create ContainerBehavior enum | `ContainerClassifier.kt` | ALWAYS_DYNAMIC, CONDITIONALLY_DYNAMIC, STATIC |
| 1.4 | Create container classification logic | `ContainerClassifier.kt` | Split RecyclerView vs ScrollView logic |
| 1.5 | Create ContentSignal data class | `ContentAnalyzer.kt` | TextLength, hasResourceId, hasDynamicPatterns, stabilityScore |
| 1.6 | Create content analysis logic | `ContentAnalyzer.kt` | Dynamic/static pattern matching, stability scoring |
| 1.7 | Create ScreenType enum | `ScreenClassifier.kt` | SETTINGS_SCREEN, LIST_SCREEN, FORM_SCREEN, etc. |
| 1.8 | Create screen classification logic | `ScreenClassifier.kt` | Analyze element stats to determine screen type |
| 1.9 | Create PersistenceDecisionEngine | `PersistenceDecisionEngine.kt` | 6-rule decision matrix combining all layers |

**Deliverable:** 5 new files in VoiceOSCore with complete classification logic.

---

### Phase 2: Android Platform Integration

**Goal:** Add Android-specific PackageManager integration for app category detection.

**Location:** `android/apps/voiceoscoreng/src/main/kotlin/.../service/`

| # | Task | File | Description |
|---|------|------|-------------|
| 2.1 | Create IAppCategoryProvider interface | `IAppCategoryProvider.kt` (KMP) | Platform-agnostic interface |
| 2.2 | Create AndroidAppCategoryProvider | `AndroidAppCategoryProvider.kt` | PackageManager.getApplicationInfo().category |
| 2.3 | Inject provider into DynamicCommandGenerator | `DynamicCommandGenerator.kt` | Add constructor parameter |

**Deliverable:** Android can query app category via PackageManager API.

---

### Phase 3: Integration & Wiring

**Goal:** Connect new classification system to existing persistence flow.

| # | Task | File | Description |
|---|------|------|-------------|
| 3.1 | Add appCategory to extraction context | `ElementExtractor.kt` | Pass category to extractElements() |
| 3.2 | Replace isDynamicContent logic | `ElementInfo.kt` | Deprecate getter, add new decision method |
| 3.3 | Update CommandGenerator | `CommandGenerator.kt` | Use PersistenceDecisionEngine.shouldPersist() |
| 3.4 | Update CommandOrchestrator | `CommandOrchestrator.kt` | Pass screen context to decision engine |
| 3.5 | Wire up in VoiceOSAccessibilityService | `VoiceOSAccessibilityService.kt` | Create provider, pass to generator |

**Deliverable:** Full pipeline using new 4-layer decision system.

---

### Phase 4: Testing & Validation

**Goal:** Verify correct behavior across all scenarios.

| # | Task | File | Description |
|---|------|------|-------------|
| 4.1 | Unit test AppCategoryClassifier | `AppCategoryClassifierTest.kt` | Test pattern matching |
| 4.2 | Unit test ContainerClassifier | `ContainerClassifierTest.kt` | Test container detection |
| 4.3 | Unit test ContentAnalyzer | `ContentAnalyzerTest.kt` | Test stability scoring |
| 4.4 | Unit test PersistenceDecisionEngine | `PersistenceDecisionEngineTest.kt` | Test all 6 decision rules |
| 4.5 | Manual test: RealWear Settings | - | Verify commands persisted |
| 4.6 | Manual test: Gmail inbox | - | Verify emails NOT persisted |
| 4.7 | Manual test: Gmail menu | - | Verify menu items persisted |

**Deliverable:** Passing tests + verified manual scenarios.

---

### Phase 5: Database Import/Export System

**Goal:** Create a complete import/export system for command database with settings UI.

**Prerequisites:** Phase 4 must be complete - requires understanding of final database structure.

**Architecture (KMP-First):**
- `Modules/VoiceOSCore/src/commonMain/` - **ALL logic** (export/import/format)
- `android/apps/voiceoscoreng/` - **UI only** (Activities, wiring)

**Location - KMP Core Logic:**
`Modules/VoiceOSCore/src/commonMain/kotlin/com/augmentalis/voiceoscore/`

| # | Task | File | Description |
|---|------|------|-------------|
| 5.1 | Analyze final DB schema | - | Document tables: scraped_app, scraped_element, scraped_command, app_category_cache |
| 5.2 | Create export data models | `CommandExportModels.kt` | ExportManifest, AppExportData, CommandExportData |
| 5.3 | Create ICommandExporter interface | `ICommandExporter.kt` | Platform-agnostic export contract |
| 5.4 | Create ICommandImporter interface | `ICommandImporter.kt` | Platform-agnostic import contract |
| 5.5 | Implement CommandExporter | `CommandExporter.kt` | Export all/selective apps to JSON |
| 5.6 | Implement CommandImporter | `CommandImporter.kt` | Import with merge/replace strategies |
| 5.7 | Create export format serializer | `ExportSerializer.kt` | JSON serialization with versioning |

**Location - Android UI/Wiring:**
`android/apps/voiceoscoreng/src/main/kotlin/.../`

| # | Task | File | Description |
|---|------|------|-------------|
| 5.8 | Create Android file I/O provider | `AndroidExportFileProvider.kt` | File system access (SAF) |
| 5.9 | Create Export Settings UI | `ExportSettingsActivity.kt` | App selection, export options |
| 5.10 | Create Import Settings UI | `ImportSettingsActivity.kt` | File picker, preview, import |

**Export Options:**
- **All Apps:** Complete database backup
- **Single App:** Export one app's commands (e.g., just Gmail)
- **Multiple Apps:** Select multiple apps to export
- **Format:** JSON with metadata (version, timestamp, app count)

**Import Options:**
- **Merge:** Add new commands, keep existing
- **Replace:** Overwrite existing commands for imported apps
- **Preview:** Show what will be imported before confirming

**Deliverable:** KMP export/import logic + Android UI.

---

## File Changes Summary

### New Files (19)

**Phase 1-4: Hybrid Persistence (9 files)**

| Location | File | Purpose |
|----------|------|---------|
| VoiceOSCore/commonMain | `AppCategoryClassifier.kt` | Layer 1: App category |
| VoiceOSCore/commonMain | `ContainerClassifier.kt` | Layer 2: Container type |
| VoiceOSCore/commonMain | `ContentAnalyzer.kt` | Layer 3: Content heuristics |
| VoiceOSCore/commonMain | `ScreenClassifier.kt` | Layer 4: Screen type |
| VoiceOSCore/commonMain | `PersistenceDecisionEngine.kt` | Decision matrix |
| VoiceOSCore/commonMain | `IAppCategoryProvider.kt` | Platform interface |
| voiceoscoreng/service | `AndroidAppCategoryProvider.kt` | Android implementation |
| VoiceOSCore/commonTest | `AppCategoryClassifierTest.kt` | Unit tests |
| VoiceOSCore/commonTest | `PersistenceDecisionEngineTest.kt` | Unit tests |

**Phase 5: Import/Export (10 files)**

*KMP Core Logic (7 files) - `VoiceOSCore/commonMain`*

| Location | File | Purpose |
|----------|------|---------|
| VoiceOSCore/commonMain | `CommandExportModels.kt` | Export data classes |
| VoiceOSCore/commonMain | `ICommandExporter.kt` | Export interface |
| VoiceOSCore/commonMain | `ICommandImporter.kt` | Import interface |
| VoiceOSCore/commonMain | `IExportFileProvider.kt` | Platform file I/O interface |
| VoiceOSCore/commonMain | `CommandExporter.kt` | Export implementation |
| VoiceOSCore/commonMain | `CommandImporter.kt` | Import implementation |
| VoiceOSCore/commonMain | `ExportSerializer.kt` | JSON serialization |

*Android UI/Wiring (3 files) - `voiceoscoreng`*

| Location | File | Purpose |
|----------|------|---------|
| voiceoscoreng/service | `AndroidExportFileProvider.kt` | SAF file access |
| voiceoscoreng | `ExportSettingsActivity.kt` | Export UI |
| voiceoscoreng | `ImportSettingsActivity.kt` | Import UI |

### Modified Files (5)

| Location | File | Changes |
|----------|------|---------|
| VoiceOSCore/commonMain | `ElementInfo.kt` | Deprecate `isDynamicContent`, add decision method |
| VoiceOSCore/commonMain | `CommandGenerator.kt` | Use new decision engine |
| VoiceOSCore/commonMain | `CommandOrchestrator.kt` | Pass screen context |
| voiceoscoreng/service | `ElementExtractor.kt` | Pass app category context |
| voiceoscoreng/service | `DynamicCommandGenerator.kt` | Inject category provider |

---

## Decision Rules Reference

```kotlin
// Rule 1: Always-dynamic containers NEVER persist
if (containerBehavior == ALWAYS_DYNAMIC) return false

// Rule 2: Settings/System apps persist (unless dynamic patterns)
if (appCategory in [SETTINGS, SYSTEM]) return !hasDynamicPatterns

// Rule 3: Settings screens persist (any app)
if (screenType == SETTINGS_SCREEN) return !hasDynamicPatterns

// Rule 4: Form screens persist short content
if (screenType == FORM_SCREEN) return textLength != LONG

// Rule 5: Email/Messaging with ScrollView - context dependent
if (appCategory in [EMAIL, MESSAGING, SOCIAL]) {
    return (textLength == SHORT && hasResourceId) || stabilityScore > 70
}

// Rule 6: Unknown apps - stability threshold
return stabilityScore > 60 && !hasDynamicPatterns
```

---

## Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Incorrect classification | Medium | Medium | Extensive test coverage |
| Performance regression | Low | Medium | Early-exit rules for RecyclerView |
| Breaking existing behavior | Medium | High | Deprecate old method, gradual migration |
| PackageManager API changes | Low | Low | Fallback to pattern matching |

---

## Success Criteria

| Scenario | Expected Result | Verification |
|----------|-----------------|--------------|
| RealWear Settings | All commands persisted to DB | `SELECT COUNT(*) > 0` |
| Gmail Inbox | Email rows NOT persisted | `SELECT COUNT(*) = 0` for emails |
| Gmail Menu | Menu items persisted | `SELECT COUNT(*) > 0` for menu |
| Unknown app with ScrollView | Static content persisted | Stability score > 60 passes |
| RecyclerView in any app | Nothing persisted | Rule 1 early exit |

---

## Time Estimates

| Phase | Sequential | Notes |
|-------|------------|-------|
| Phase 1: Core Infrastructure | 3-4 hours | 9 tasks, pure KMP |
| Phase 2: Android Integration | 1 hour | 3 tasks |
| Phase 3: Integration | 1.5-2 hours | 5 tasks, careful wiring |
| Phase 4: Testing | 1-1.5 hours | 7 tasks |
| **Subtotal (Phases 1-4)** | **6.5-8.5 hours** | Hybrid persistence complete |
| Phase 5: Import/Export | 4-6 hours | 10 tasks, DB analysis + UI |
| **Total** | **10.5-14.5 hours** | |

---

## Dependencies

```
Phase 1 ──────────────────────────────┐
                                      │
Phase 2 ────────┐                     │
                │                     │
                ▼                     ▼
           Phase 3 (depends on 1 + 2)
                │
                ▼
           Phase 4 (depends on 3)
                │
                ▼
           Phase 5 (depends on 4)
                │
                │  ◄── REQUIRES understanding of final DB structure
                │      Cannot start until Phase 4 validates schema
                ▼
              DONE
```

**Parallel Opportunity:** Phase 1 and Phase 2.1 (interface) can run in parallel.

**Critical Dependency:** Phase 5 MUST wait for Phase 4 completion because:
- Import/Export needs to work with final table schema
- App category caching may add new tables
- Need to verify FK relationships are correct

---

## Rollback Plan

If issues arise:
1. Revert `ElementInfo.isDynamicContent` to original logic
2. Remove decision engine calls
3. Keep new classifier files (no harm if unused)

---

*Plan by Claude | VoiceOSCore Module | IDEACODE Framework*
