# VoiceOSCoreNG Module Consolidation Plan

## Overview

Consolidate VoiceOSCore/learnapp, LearnAppCore library, and JITLearning into a single unified `VoiceOSCoreNG` module with cross-platform KMP support.

**Branch:** `VoiceOSCoreNG` (derived from `Avanues-Main`)
**Module Path:** `Modules/VoiceOSCoreNG/`
**Workflow:** TDD with swarm agents (.swarm .yolo .cot .tot .tdd)

---

## Requirements Summary

| # | Requirement | Status |
|---|-------------|--------|
| 1 | New branch `VoiceOSCoreNG` from `Avanues-Main` | Pending |
| 2 | TDD workflow with swarm agents | Pending |
| 3 | Module at `Modules/VoiceOSCoreNG/` | Pending |
| 4 | Dev features via system setting + paywall prep | Pending |
| 5 | Migrate functionality (not deprecated code) | Pending |
| 6 | KMP for cross-platform | Pending |
| 7 | Folder structure: Common + Platform/src | Pending |
| 8 | Detailed migration document | Pending |
| 9 | UUID → VUID naming | Pending |

---

## Folder Structure

```
Modules/VoiceOSCoreNG/
├── Common/                           # Shared code (any tech stack)
│   ├── Classes/
│   │   ├── ElementInfo.kt
│   │   ├── ProcessingMode.kt
│   │   ├── ElementProcessingResult.kt
│   │   ├── FrameworkInfo.kt
│   │   └── VUIDGenerator.kt
│   ├── Functions/
│   │   ├── ElementHashing.kt
│   │   ├── FrameworkDetection.kt
│   │   └── VUIDOperations.kt
│   ├── Features/
│   │   ├── LearnAppLite/
│   │   │   ├── LiteCore.kt
│   │   │   └── LiteConfig.kt
│   │   └── LearnAppDev/
│   │       ├── DevCore.kt
│   │       ├── DevConfig.kt
│   │       └── FeatureGate.kt         # Paywall/code access prep
│   └── UI/                            # Cross-platform UI using AVAUI
│       ├── Components/
│       │   ├── ElementOverlay.yaml
│       │   ├── CommandStatus.yaml
│       │   └── DebugPanel.yaml
│       ├── Screens/
│       │   ├── LearnAppMain.yaml
│       │   └── SettingsScreen.yaml
│       └── Theme/
│           └── VoiceOSCoreNGTheme.yaml
├── Android/
│   └── src/
│       └── main/
│           └── java/com/augmentalis/voiceoscoreng/
│               ├── core/
│               │   └── VoiceOSCoreNG.kt
│               ├── database/
│               │   └── VoiceOSCoreNGDatabase.kt
│               ├── jit/
│               │   └── JitProcessor.kt
│               ├── exploration/
│               │   └── ExplorationEngine.kt
│               └── frameworks/
│                   ├── FlutterHandler.kt
│                   ├── UnityHandler.kt
│                   ├── UnrealHandler.kt
│                   ├── ReactNativeHandler.kt
│                   └── WebViewHandler.kt
├── iOS/
│   └── src/
├── MacOS/
│   └── src/
├── Windows/
│   └── src/
├── Web/
│   └── src/
├── build.gradle.kts
└── README.md
```

---

## Why Migrate Functionality (Not Deprecated Code)

**Question:** If `VoiceOSCore/learnapp/` is deprecated, why migrate?

**Answer:** We migrate the **functionality**, not the deprecated code itself:

| What We Take | What We Leave Behind |
|--------------|---------------------|
| Working algorithms | Duplicate class definitions |
| Business logic | Legacy patterns |
| Framework detection | Hardcoded values |
| Processing modes | Test workarounds |
| FK constraint handling | Mock/stub helpers |

The deprecated code served as a prototype. The functionality it provides (element processing, command generation, framework detection) is production-ready and tested. We consolidate this into clean, single-source-of-truth code.

---

## VUID Naming Convention

All UUID references become VUID (Voice Unique Identifier):

| Old Name | New Name |
|----------|----------|
| `uuid` | `vuid` |
| `UUID_PATTERN` | `VUID_PATTERN` |
| `generateUUID()` | `generateVUID()` |
| `UUIDGenerator` | `VUIDGenerator` |
| `UUIDCreator` | `VUIDCreator` |
| `isValidUUID()` | `isValidVUID()` |

---

## Dev Features & Paywall Prep

### System Setting Toggle
```kotlin
object FeatureGate {
    // Current: System setting toggle
    fun isDevEnabled(): Boolean {
        return Settings.System.getInt(
            context.contentResolver,
            "voiceos_dev_features",
            0
        ) == 1
    }

    // Future: Paywall/license check
    interface LicenseProvider {
        suspend fun hasDevAccess(): Boolean
        suspend fun requestAccess(): AccessResult
    }

    // Future: Special code access
    interface CodeAccessProvider {
        suspend fun validateCode(code: String): Boolean
        suspend fun activateCode(code: String): ActivationResult
    }
}
```

### Dev-Only Features
| Feature | Lite | Dev |
|---------|------|-----|
| Basic element capture | ✓ | ✓ |
| Command generation | ✓ | ✓ |
| Framework detection | ✓ | ✓ |
| Batch exploration | ✗ | ✓ |
| Custom command templates | ✗ | ✓ |
| Analytics export | ✗ | ✓ |
| Debug overlays | ✗ | ✓ |

---

## Phase 1: Branch & Structure Setup

### Tasks
| # | Task | TDD |
|---|------|-----|
| 1.1 | Create branch `VoiceOSCoreNG` from `Avanues-Main` | - |
| 1.2 | Create `Modules/VoiceOSCoreNG/` folder structure | - |
| 1.3 | Create `build.gradle.kts` with KMP configuration | - |
| 1.4 | Add to `settings.gradle.kts` | - |
| 1.5 | Verify project syncs | Test |

---

## Phase 2: Common Classes Migration

### Tasks
| # | Task | Source | TDD |
|---|------|--------|-----|
| 2.1 | Create `Common/Classes/ElementInfo.kt` | LearnAppCore | Test first |
| 2.2 | Create `Common/Classes/ProcessingMode.kt` | LearnAppCore | Test first |
| 2.3 | Create `Common/Classes/ElementProcessingResult.kt` | LearnAppCore | Test first |
| 2.4 | Create `Common/Classes/FrameworkInfo.kt` | VoiceOSCore | Test first |
| 2.5 | Create `Common/Classes/VUIDGenerator.kt` | UUIDCreator | Test first |
| 2.6 | Run all Common/Classes tests | - | Verify |

---

## Phase 3: Common Functions Migration

### Tasks
| # | Task | Source | TDD |
|---|------|--------|-----|
| 3.1 | Create `Common/Functions/ElementHashing.kt` | LearnAppCore | Test first |
| 3.2 | Create `Common/Functions/FrameworkDetection.kt` | VoiceOSCore | Test first |
| 3.3 | Create `Common/Functions/VUIDOperations.kt` | UUIDCreator | Test first |
| 3.4 | Run all Common/Functions tests | - | Verify |

---

## Phase 4: Feature Implementation (Lite)

### Tasks
| # | Task | Source | TDD |
|---|------|--------|-----|
| 4.1 | Create `Common/Features/LearnAppLite/LiteCore.kt` | LearnAppCore | Test first |
| 4.2 | Create `Common/Features/LearnAppLite/LiteConfig.kt` | New | Test first |
| 4.3 | Implement basic element capture | LearnAppCore | Test first |
| 4.4 | Implement command generation | LearnAppCore | Test first |
| 4.5 | Run Lite feature tests | - | Verify |

---

## Phase 5: Feature Implementation (Dev)

### Tasks
| # | Task | Source | TDD |
|---|------|--------|-----|
| 5.1 | Create `Common/Features/LearnAppDev/DevCore.kt` | VoiceOSCore | Test first |
| 5.2 | Create `Common/Features/LearnAppDev/DevConfig.kt` | New | Test first |
| 5.3 | Create `Common/Features/LearnAppDev/FeatureGate.kt` | New | Test first |
| 5.4 | Implement batch exploration | JITLearning | Test first |
| 5.5 | Implement debug overlays | VoiceOSCore | Test first |
| 5.6 | Run Dev feature tests | - | Verify |

---

## Phase 6: Android Platform Implementation

### Tasks
| # | Task | Source | TDD |
|---|------|--------|-----|
| 6.1 | Create `Android/src/.../core/VoiceOSCoreNG.kt` | LearnAppCore | Test first |
| 6.2 | Create `Android/src/.../database/VoiceOSCoreNGDatabase.kt` | VoiceOSCore | Test first |
| 6.3 | Create `Android/src/.../jit/JitProcessor.kt` | JITLearning | Test first |
| 6.4 | Create `Android/src/.../exploration/ExplorationEngine.kt` | VoiceOSCore | Test first |
| 6.5 | Run Android platform tests | - | Verify |

---

## Phase 7: Framework Handlers

### Tasks
| # | Task | Source | TDD |
|---|------|--------|-----|
| 7.1 | Create `FlutterHandler.kt` | VoiceOSCore | Test first |
| 7.2 | Create `UnityHandler.kt` | VoiceOSCore | Test first |
| 7.3 | Create `UnrealHandler.kt` | VoiceOSCore | Test first |
| 7.4 | Create `ReactNativeHandler.kt` | VoiceOSCore | Test first |
| 7.5 | Create `WebViewHandler.kt` | VoiceOSCore | Test first |
| 7.6 | Run framework handler tests | - | Verify |

---

## Phase 8: Cross-Platform UI (AVAUI)

### Tasks
| # | Task | Source | TDD |
|---|------|--------|-----|
| 8.1 | Create `Common/UI/Theme/VoiceOSCoreNGTheme.yaml` | New | Test first |
| 8.2 | Create `Common/UI/Components/ElementOverlay.yaml` | VoiceOSCore overlays | Test first |
| 8.3 | Create `Common/UI/Components/CommandStatus.yaml` | VoiceOSCore overlays | Test first |
| 8.4 | Create `Common/UI/Components/DebugPanel.yaml` | Dev features | Test first |
| 8.5 | Create `Common/UI/Screens/LearnAppMain.yaml` | New | Test first |
| 8.6 | Create `Common/UI/Screens/SettingsScreen.yaml` | New | Test first |
| 8.7 | Verify AVAUI renders on all platforms | - | Verify |

---

## Phase 9: Database Consolidation

### Tasks
| # | Task | Source | TDD |
|---|------|--------|-----|
| 9.1 | Create unified SQLDelight schema | VoiceOS/database | Test first |
| 9.2 | Implement FK constraint handling | LearnAppCore fix | Test first |
| 9.3 | Create `scraped_app` → `scraped_element` → `commands_generated` chain | Existing | Test first |
| 9.4 | Migrate all queries to VUID naming | UUID→VUID | Test first |
| 9.5 | Run database tests | - | Verify |

---

## Phase 10: Integration Testing

### Tasks
| # | Task | TDD |
|---|------|-----|
| 10.1 | Create end-to-end element processing test | Test |
| 10.2 | Create framework detection integration test | Test |
| 10.3 | Create Lite vs Dev feature gate test | Test |
| 10.4 | Create database FK chain test | Test |
| 10.5 | Run full integration suite | Verify |

---

## Phase 11: Migration Bridge

### Tasks
| # | Task |
|---|------|
| 11.1 | Create adapter for VoiceOSCore consumers |
| 11.2 | Create adapter for JITLearning consumers |
| 11.3 | Add deprecation warnings to old modules |
| 11.4 | Document migration path |

---

## Phase 12: Cleanup Old Modules

### Tasks
| # | Task |
|---|------|
| 12.1 | Mark `VoiceOSCore/learnapp/` as deprecated |
| 12.2 | Mark `LearnAppCore` library as deprecated |
| 12.3 | Mark duplicate classes in JITLearning as deprecated |
| 12.4 | Update all imports to VoiceOSCoreNG |

---

## Phase 13: Documentation

### Tasks
| # | Task |
|---|------|
| 13.1 | Create `Modules/VoiceOSCoreNG/README.md` |
| 13.2 | Create migration guide document |
| 13.3 | Update developer manual |
| 13.4 | Create API reference |

---

## Files to Migrate

### From VoiceOSCore/learnapp/ (149 files - FUNCTIONALITY ONLY)
- `core/LearnAppCore.kt` → `Android/src/.../core/VoiceOSCoreNG.kt`
- `jit/JitElementCapture.kt` → `Android/src/.../jit/JitProcessor.kt`
- Framework detection logic → `Common/Functions/FrameworkDetection.kt`

### From LearnAppCore Library (32 files)
- `ElementInfo.kt` → `Common/Classes/ElementInfo.kt`
- `ProcessingMode.kt` → `Common/Classes/ProcessingMode.kt`
- `ElementProcessingResult.kt` → `Common/Classes/ElementProcessingResult.kt`

### From JITLearning (28 files)
- `JITLearningService.kt` → `Android/src/.../jit/JitProcessor.kt`
- `SecurityValidator.kt` → `Common/Functions/` (VUID validation)
- `ExplorationCommand.kt` → `Common/Classes/`

### From UUIDCreator (40 files - rename to VUID)
- `ThirdPartyUuidGenerator.kt` → `Common/Classes/VUIDGenerator.kt`
- `AccessibilityFingerprint.kt` → `Common/Functions/ElementHashing.kt`

---

## Duplicate Classes Resolution

| Class | Locations | Resolution |
|-------|-----------|------------|
| `ElementInfo` | LearnAppCore, VoiceOSCore | Single in `Common/Classes/` |
| `ProcessingMode` | LearnAppCore, VoiceOSCore | Single in `Common/Classes/` |
| `ElementProcessingResult` | LearnAppCore, JITLearning | Single in `Common/Classes/` |
| `LearnAppCore` | Library, VoiceOSCore | Single `VoiceOSCoreNG.kt` |

---

## Test Strategy

### TDD Workflow Per Phase
1. Write test for new class/function
2. Run test (expect fail)
3. Implement code
4. Run test (expect pass)
5. Refactor if needed
6. Move to next task

### Test Categories
| Category | Location | Count |
|----------|----------|-------|
| Unit Tests | `Common/*/test/` | ~50 |
| Android Tests | `Android/src/test/` | ~30 |
| Integration Tests | `Android/src/androidTest/` | ~20 |
| Total | | ~100 |

---

## Swarm Agent Assignments

| Phase | Agents | Focus |
|-------|--------|-------|
| 1-3 | architecture, code-review | Structure & common code |
| 4-5 | testing, code-review | Feature implementation |
| 6-7 | android, testing | Platform & frameworks |
| 8 | design-system, testing | AVAUI cross-platform UI |
| 9-10 | testing, security | Database & integration |
| 11-13 | documentation, code-review | Migration & docs |

---

## Success Criteria

| Metric | Target |
|--------|--------|
| Test coverage | ≥90% |
| Duplicate classes | 0 |
| FK constraint violations | 0 |
| Build time | <60s |
| All platforms compile | ✓ |

---

## Timeline Estimate

| Phase | Tasks |
|-------|-------|
| Phase 1 | 5 tasks |
| Phase 2-3 | 10 tasks |
| Phase 4-5 | 11 tasks |
| Phase 6-7 | 11 tasks |
| Phase 8 (UI) | 7 tasks |
| Phase 9-10 | 10 tasks |
| Phase 11-13 | 12 tasks |
| **Total** | **66 tasks** |

---

## Next Steps

1. Create branch `VoiceOSCoreNG` from `Avanues-Main`
2. Set up folder structure
3. Begin Phase 1 with TDD workflow
4. Use swarm agents for parallel work

---

**Created:** 2025-12-31
**Version:** 1.0
**Author:** Claude (IDEACODE)
