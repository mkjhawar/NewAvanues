# Implementation Plan: LearnApp Dual-Edition System

**Spec**: `VoiceOS-LearnApp-DualEdition-Spec-51211-V1.md`
**Created**: 2025-12-11
**Author**: Manoj Jhawar
**Status**: Ready for Implementation

---

## Overview

| Attribute | Value |
|-----------|-------|
| **Platform** | Android |
| **Modules** | LearnAppCore, JITLearning, LearnApp (User), LearnApp (Dev) |
| **Swarm Recommended** | YES (40+ tasks, complex integration) |
| **Total Tasks** | 47 |
| **Phases** | 6 |

---

## Chain of Thought: Phase Ordering

```
LearnAppCore (shared library)     ← FIRST: All apps depend on this
       │
       ▼
JITLearning (AIDL extension)      ← SECOND: IPC layer for event streaming
       │
       ├─────────────┬────────────┐
       ▼             ▼            │
LearnApp User    LearnApp Dev    │← CAN PARALLELIZE (Phases 3-4)
       │             │            │
       ▼             ▼            │
External Ingestion ◄─────────────┘← FIFTH: Needs database + parser
       │
       ▼
Contact Extraction                ← SIXTH: Future feature (deferred)
```

**Reasoning**:
1. Safety systems (Do Not Click, Dynamic Detection) are core business logic → LearnAppCore
2. AIDL event streaming extends existing JITLearning module
3. User Edition validates core flow before Developer Edition complexity
4. Developer Edition can parallel with User Edition after Phase 2
5. External Ingestion integrates with CommandManager (cross-module)
6. Contact Extraction is future feature, can be deferred or paralleled

---

## Phase 1: Core Safety Systems (LearnAppCore)

**Module**: `Modules/VoiceOS/libraries/LearnAppCore`
**Duration**: Tasks 1-10
**Dependencies**: None (foundation phase)

### Tasks

| # | Task | File | Priority |
|---|------|------|----------|
| 1.1 | Create DoNotClickList object with keyword sets | `safety/DoNotClickList.kt` | P0 |
| 1.2 | Implement shouldNotClick() with all 5 categories | `safety/DoNotClickList.kt` | P0 |
| 1.3 | Add dangerous resource ID regex patterns | `safety/DoNotClickList.kt` | P0 |
| 1.4 | Create DoNotClickReason enum | `safety/DoNotClickList.kt` | P0 |
| 1.5 | Create DynamicContentDetector object | `safety/DynamicContentDetector.kt` | P0 |
| 1.6 | Implement ContentFingerprint data class | `safety/DynamicContentDetector.kt` | P0 |
| 1.7 | Implement detectDynamicRegion() with change tracking | `safety/DynamicContentDetector.kt` | P0 |
| 1.8 | Create DynamicChangeType enum (INFINITE_SCROLL, etc.) | `safety/DynamicContentDetector.kt` | P0 |
| 1.9 | Create LoginScreenDetector object | `safety/LoginScreenDetector.kt` | P1 |
| 1.10 | Implement isLoginScreen() with score-based detection | `safety/LoginScreenDetector.kt` | P1 |
| 1.11 | Create LoginType enum (STANDARD, SSO, 2FA) | `safety/LoginScreenDetector.kt` | P1 |
| 1.12 | Create MenuDiscovery object | `menu/MenuDiscovery.kt` | P1 |
| 1.13 | Implement discoverFullMenu() with scroll-based collection | `menu/MenuDiscovery.kt` | P1 |
| 1.14 | Create MenuContent and MenuItem data classes | `menu/MenuDiscovery.kt` | P1 |
| 1.15 | Unit tests for DoNotClickList | `test/.../DoNotClickListTest.kt` | P1 |
| 1.16 | Unit tests for DynamicContentDetector | `test/.../DynamicContentDetectorTest.kt` | P1 |

### Deliverables
- [ ] DoNotClickList with 5 categories (CALL, POST, EXIT, AUTH, PAYMENT)
- [ ] DynamicContentDetector with 3+ change threshold
- [ ] LoginScreenDetector with 40+ score threshold
- [ ] MenuDiscovery with scroll-based enumeration
- [ ] Unit tests passing

---

## Phase 2: AIDL Extension (JITLearning)

**Module**: `Modules/VoiceOS/libraries/JITLearning`
**Duration**: Tasks 2.1-2.12
**Dependencies**: Phase 1 (uses safety systems)

### Tasks

| # | Task | File | Priority |
|---|------|------|----------|
| 2.1 | Create IAccessibilityEventListener.aidl | `aidl/.../IAccessibilityEventListener.aidl` | P0 |
| 2.2 | Create ScreenChangeEvent.aidl parcelable | `aidl/.../ScreenChangeEvent.aidl` | P0 |
| 2.3 | Create ParcelableNodeInfo.aidl parcelable | `aidl/.../ParcelableNodeInfo.aidl` | P0 |
| 2.4 | Create ExplorationCommand.aidl parcelable | `aidl/.../ExplorationCommand.aidl` | P0 |
| 2.5 | Extend IElementCaptureService with registerEventListener() | `aidl/.../IElementCaptureService.aidl` | P0 |
| 2.6 | Add getCurrentScreenInfo() to IElementCaptureService | `aidl/.../IElementCaptureService.aidl` | P0 |
| 2.7 | Add getFullMenuContent() to IElementCaptureService | `aidl/.../IElementCaptureService.aidl` | P0 |
| 2.8 | Add performClick/performScroll/performAction methods | `aidl/.../IElementCaptureService.aidl` | P0 |
| 2.9 | Implement ParcelableNodeInfo.kt with Parcelable | `model/ParcelableNodeInfo.kt` | P0 |
| 2.10 | Implement ScreenChangeEvent.kt with Parcelable | `model/ScreenChangeEvent.kt` | P0 |
| 2.11 | Implement ExplorationCommand.kt sealed class | `model/ExplorationCommand.kt` | P0 |
| 2.12 | Update JITLearningService with listener management | `JITLearningService.kt` | P0 |
| 2.13 | Implement event dispatching to listeners | `JITLearningService.kt` | P0 |
| 2.14 | Implement performClick/Scroll/Action handlers | `JITLearningService.kt` | P1 |
| 2.15 | Integration tests for AIDL round-trip | `test/.../AIDLIntegrationTest.kt` | P1 |

### Deliverables
- [ ] IAccessibilityEventListener.aidl with 6 callback methods
- [ ] Extended IElementCaptureService with 6 new methods
- [ ] ParcelableNodeInfo with full element properties
- [ ] JITLearningService dispatching events to registered listeners
- [ ] Integration tests passing

---

## Phase 3: LearnApp User Edition

**Module**: `Modules/VoiceOS/apps/LearnApp`
**Duration**: Tasks 3.1-3.15
**Dependencies**: Phase 2 (AIDL interface)
**Parallel**: Can run with Phase 4 after 3.5 complete

### Tasks

| # | Task | File | Priority |
|---|------|------|----------|
| 3.1 | Implement event listener binding in LearnAppActivity | `LearnAppActivity.kt` | P0 |
| 3.2 | Create ExplorationCoordinator class | `exploration/ExplorationCoordinator.kt` | P0 |
| 3.3 | Implement automated DFS exploration algorithm | `exploration/ExplorationCoordinator.kt` | P0 |
| 3.4 | Integrate DoNotClickList into exploration | `exploration/ExplorationCoordinator.kt` | P0 |
| 3.5 | Integrate DynamicContentDetector into exploration | `exploration/ExplorationCoordinator.kt` | P0 |
| 3.6 | Create ScreenGraphBuilder for navigation tracking | `graph/ScreenGraphBuilder.kt` | P1 |
| 3.7 | Implement SQLDelight hybrid storage (in-memory + persist) | `storage/HybridStorage.kt` | P1 |
| 3.8 | Create exploration progress notification | `ui/ExplorationNotification.kt` | P1 |
| 3.9 | Create floating progress overlay | `ui/FloatingProgressOverlay.kt` | P1 |
| 3.10 | Implement AVU serializer for export | `export/AVUSerializer.kt` | P1 |
| 3.11 | Implement encrypted AVU export | `export/EncryptedAVUExport.kt` | P2 |
| 3.12 | Create app selection UI with search/filter | `ui/AppSelectionScreen.kt` | P1 |
| 3.13 | Add exploration history tracking | `ui/ExplorationHistoryScreen.kt` | P2 |
| 3.14 | Implement login screen handling (prompt user) | `exploration/LoginHandler.kt` | P1 |
| 3.15 | Integration tests for exploration flow | `test/.../ExplorationFlowTest.kt` | P1 |

### Deliverables
- [ ] ExplorationCoordinator with safety-aware DFS
- [ ] HybridStorage with SQLDelight + in-memory graph
- [ ] Progress notification + floating overlay
- [ ] Encrypted AVU export
- [ ] App selection UI with search
- [ ] Integration tests passing

---

## Phase 4: LearnApp Developer Edition

**Module**: `Modules/VoiceOS/apps/LearnAppDev` (new module)
**Duration**: Tasks 4.1-4.16
**Dependencies**: Phase 2 (AIDL interface), shares code with Phase 3
**Parallel**: Can run with Phase 3 after 3.5 complete

### Tasks

| # | Task | File | Priority |
|---|------|------|----------|
| 4.1 | Create LearnAppDev module (copy LearnApp, add dev deps) | `build.gradle.kts` | P0 |
| 4.2 | Add Neo4j embedded dependency | `build.gradle.kts` | P0 |
| 4.3 | Create DeveloperSettingsManager | `dev/DeveloperSettingsManager.kt` | P0 |
| 4.4 | Implement tap sequence activation (7x tap) | `dev/DeveloperSettingsManager.kt` | P0 |
| 4.5 | Implement secret code activation (long-press) | `dev/DeveloperSettingsManager.kt` | P1 |
| 4.6 | Stub remote flag activation (API check) | `dev/DeveloperSettingsManager.kt` | P2 |
| 4.7 | Create LiveElementInspector | `dev/LiveElementInspector.kt` | P0 |
| 4.8 | Implement inspection UI overlay | `dev/ui/InspectorOverlay.kt` | P0 |
| 4.9 | Create ManualClickInjection handler | `dev/ManualClickInjection.kt` | P1 |
| 4.10 | Implement screenshot-to-click mapping | `dev/ManualClickInjection.kt` | P1 |
| 4.11 | Create CommandTestHarness | `dev/CommandTestHarness.kt` | P1 |
| 4.12 | Implement test harness UI with toggle | `dev/ui/TestHarnessScreen.kt` | P1 |
| 4.13 | Create ScreenDiffViewer | `dev/ScreenDiffViewer.kt` | P2 |
| 4.14 | Implement Neo4j graph storage adapter | `dev/graph/Neo4jAdapter.kt` | P1 |
| 4.15 | Create Cypher query console UI | `dev/ui/CypherConsoleScreen.kt` | P2 |
| 4.16 | Implement unencrypted AVU + Neo4j export | `dev/export/DeveloperExport.kt` | P1 |

### Deliverables
- [ ] LearnAppDev module with Neo4j embedded
- [ ] 4 developer activation methods (tap, code, build, remote)
- [ ] LiveElementInspector with pause/inspect
- [ ] ManualClickInjection with screenshot mapping
- [ ] CommandTestHarness with 3x validation
- [ ] Neo4j graph storage + Cypher console
- [ ] Developer export (AVU + JSON + Cypher)

---

## Phase 5: External Folder Ingestion

**Module**: `Modules/VoiceOS/managers/CommandManager`
**Duration**: Tasks 5.1-5.8
**Dependencies**: Phase 3 (AVU serializer)

### Tasks

| # | Task | File | Priority |
|---|------|------|----------|
| 5.1 | Create ExternalFolderWatcher class | `loader/ExternalFolderWatcher.kt` | P0 |
| 5.2 | Implement FileObserver for /VoiceOS/import/ | `loader/ExternalFolderWatcher.kt` | P0 |
| 5.3 | Create AVUParser for .vos files | `loader/AVUParser.kt` | P0 |
| 5.4 | Implement IPC code parsing (APP, SCR, ELM, etc.) | `loader/AVUParser.kt` | P0 |
| 5.5 | Extend VOSCommandIngestion with AVU support | `loader/VOSCommandIngestion.kt` | P1 |
| 5.6 | Add ingestAVUFile() method | `loader/VOSCommandIngestion.kt` | P1 |
| 5.7 | Integrate with AVUQuantizer for NLU | `loader/AVUQuantizerIntegration.kt` | P2 |
| 5.8 | Unit tests for AVU parsing and ingestion | `test/.../AVUParserTest.kt` | P1 |

### Deliverables
- [ ] ExternalFolderWatcher auto-detecting .vos files
- [ ] AVUParser handling all 11 IPC codes
- [ ] VOSCommandIngestion extended for AVU
- [ ] NLU quantization integration
- [ ] Unit tests passing

---

## Phase 6: Contact Extraction (Future)

**Module**: `Modules/VoiceOS/libraries/LearnAppCore`
**Duration**: Tasks 6.1-6.10
**Dependencies**: Phase 3 (exploration engine)
**Status**: DEFERRED (implement after phases 1-5 validated)

### Tasks

| # | Task | File | Priority |
|---|------|------|----------|
| 6.1 | Create ContactScreenDetector | `contacts/ContactScreenDetector.kt` | P2 |
| 6.2 | Implement isContactListScreen() heuristics | `contacts/ContactScreenDetector.kt` | P2 |
| 6.3 | Create TeamsContactDetector (MS Teams specific) | `contacts/TeamsContactDetector.kt` | P2 |
| 6.4 | Create ContactExtractor with scroll-based collection | `contacts/ContactExtractor.kt` | P2 |
| 6.5 | Implement contact deduplication algorithm | `contacts/ContactDeduplicator.kt` | P2 |
| 6.6 | Create ExtractedContact data model | `contacts/ExtractedContact.kt` | P2 |
| 6.7 | Add unified_contacts table to database | `database/.../UnifiedContact.sq` | P2 |
| 6.8 | Create privacy consent dialog | `ui/ContactConsentDialog.kt` | P2 |
| 6.9 | Implement contact AVU export (CNT code) | `export/ContactExporter.kt` | P2 |
| 6.10 | Integration tests for contact extraction | `test/.../ContactExtractionTest.kt` | P2 |

### Deliverables
- [ ] ContactScreenDetector with heuristics
- [ ] MS Teams-specific detection
- [ ] Scroll-based contact extraction
- [ ] Deduplication with normalized names
- [ ] Privacy consent flow
- [ ] Database integration

---

## Parallel Execution Strategy (Swarm)

```
Week 1-2:  [Phase 1: Core Safety] ─────────────────────────────►

Week 3-4:  [Phase 2: AIDL Extension] ─────────────────────────►

Week 5-7:  [Phase 3: User Edition] ───────────────────────────►
           [Phase 4: Dev Edition] ────────────────────────────► (PARALLEL)

Week 8:    [Phase 5: External Ingestion] ─────────────────────►

Week 9+:   [Phase 6: Contact Extraction] ─────────────────────► (DEFERRED)
```

**Swarm Agents (Phases 3-4 parallel):**
- Agent 1: User Edition core (Tasks 3.1-3.5)
- Agent 2: User Edition UI (Tasks 3.8-3.13)
- Agent 3: Developer Edition core (Tasks 4.3-4.11)
- Agent 4: Developer Edition UI (Tasks 4.8, 4.12, 4.15)

---

## Quality Gates

| Phase | Gate | Criteria |
|-------|------|----------|
| 1 | Safety Validation | 100% dangerous elements blocked in test suite |
| 2 | AIDL Contract | All 12 methods callable across process boundary |
| 3 | User E2E | Complete exploration of Settings app with AVU export |
| 4 | Developer E2E | Inspect + inject click on MS Teams element |
| 5 | Ingestion | Import 100 commands from external .vos file |
| 6 | Contact Accuracy | 95%+ contact names correctly extracted |

---

## Risk Mitigation

| Risk | Impact | Mitigation |
|------|--------|------------|
| AIDL callback complexity | High | Start with polling, add callbacks incrementally |
| Neo4j APK size (+50MB) | Medium | Developer Edition only, ProGuard rules |
| Google Play accessibility policy | High | Frame as voice accessibility, clear disclosure |
| Dynamic content false positives | Medium | Configurable threshold, whitelist |
| Login detection false negatives | Medium | Manual override in Developer Edition |

---

## File Structure (New Files)

```
Modules/VoiceOS/libraries/LearnAppCore/
├── src/main/java/com/augmentalis/learnappcore/
│   ├── safety/
│   │   ├── DoNotClickList.kt
│   │   ├── DynamicContentDetector.kt
│   │   └── LoginScreenDetector.kt
│   ├── menu/
│   │   └── MenuDiscovery.kt
│   ├── contacts/
│   │   ├── ContactScreenDetector.kt
│   │   ├── ContactExtractor.kt
│   │   └── TeamsContactDetector.kt
│   └── export/
│       └── AVUSerializer.kt

Modules/VoiceOS/libraries/JITLearning/
├── src/main/aidl/com/augmentalis/jitlearning/
│   ├── IAccessibilityEventListener.aidl (NEW)
│   ├── ScreenChangeEvent.aidl (NEW)
│   ├── ParcelableNodeInfo.aidl (NEW)
│   └── ExplorationCommand.aidl (NEW)
└── src/main/java/com/augmentalis/jitlearning/
    └── model/
        ├── ParcelableNodeInfo.kt
        ├── ScreenChangeEvent.kt
        └── ExplorationCommand.kt

Modules/VoiceOS/apps/LearnApp/
├── src/main/java/com/augmentalis/learnapp/
│   ├── exploration/
│   │   ├── ExplorationCoordinator.kt
│   │   └── LoginHandler.kt
│   ├── graph/
│   │   └── ScreenGraphBuilder.kt
│   ├── storage/
│   │   └── HybridStorage.kt
│   ├── export/
│   │   └── EncryptedAVUExport.kt
│   └── ui/
│       ├── AppSelectionScreen.kt
│       ├── ExplorationNotification.kt
│       └── FloatingProgressOverlay.kt

Modules/VoiceOS/apps/LearnAppDev/ (NEW MODULE)
├── build.gradle.kts
├── src/main/java/com/augmentalis/learnappdev/
│   ├── dev/
│   │   ├── DeveloperSettingsManager.kt
│   │   ├── LiveElementInspector.kt
│   │   ├── ManualClickInjection.kt
│   │   ├── CommandTestHarness.kt
│   │   └── ScreenDiffViewer.kt
│   ├── graph/
│   │   └── Neo4jAdapter.kt
│   ├── export/
│   │   └── DeveloperExport.kt
│   └── ui/
│       ├── InspectorOverlay.kt
│       ├── TestHarnessScreen.kt
│       └── CypherConsoleScreen.kt

Modules/VoiceOS/managers/CommandManager/
└── src/main/java/com/augmentalis/commandmanager/
    └── loader/
        ├── ExternalFolderWatcher.kt (NEW)
        ├── AVUParser.kt (NEW)
        └── AVUQuantizerIntegration.kt (NEW)
```

---

## Summary

| Metric | Value |
|--------|-------|
| **Total Tasks** | 47 |
| **P0 Tasks** | 22 |
| **P1 Tasks** | 19 |
| **P2 Tasks** | 6 |
| **New Files** | 28 |
| **New Modules** | 1 (LearnAppDev) |
| **Sequential Estimate** | 8-10 weeks |
| **Parallel Estimate (Swarm)** | 5-6 weeks |
| **Savings** | 40% time reduction |

---

**Next Steps**:
1. `/i.implement .yolo` to begin Phase 1
2. Create TodoWrite tasks for tracking
3. Begin with DoNotClickList (Task 1.1)

---

**Version History**:
| Version | Date | Changes |
|---------|------|---------|
| 1.0.0 | 2025-12-11 | Initial plan |

**Author**: Manoj Jhawar
**Spec Reference**: `VoiceOS-LearnApp-DualEdition-Spec-51211-V1.md`
