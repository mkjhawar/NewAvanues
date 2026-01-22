# VoiceOSCore Migration Plan
**Date:** 2026-01-16 | **Version:** V1 | **Author:** Claude (ICA)

## Executive Summary

This plan consolidates three legacy Android-only libraries (VoiceOSCore, JITLearning, LearnAppCore) into the KMP-based VoiceOSCoreNG module. The goal is to eliminate duplication, enable cross-platform support, and establish VoiceOSCoreNG as the single source of truth for voice command processing.

---

## Current Architecture

```
CURRENT (Fragmented)                    TARGET (Unified)
┌────────────────────────┐              ┌────────────────────────┐
│ android/apps/VoiceOS   │              │                        │
│ (Standalone App)       │              │                        │
└────────────────────────┘              │                        │
           │                            │   VoiceOSCoreNG        │
           ▼                            │   (KMP Module)         │
┌────────────────────────┐              │                        │
│ VoiceOSCore (Android)  │─────────────►│  ├─ commonMain/        │
│ 100+ files             │              │  ├─ androidMain/       │
│ - VoiceOSService       │              │  ├─ iosMain/           │
│ - Scraping             │              │  └─ desktopMain/       │
│ - Overlays             │              │                        │
│ - Database             │              │  Single source of      │
└────────────────────────┘              │  truth for all         │
           │                            │  platforms             │
           ▼                            │                        │
┌────────────────────────┐              └────────────────────────┘
│ JITLearning (Android)  │
│ 17 files               │
│ - AIDL Service         │
│ - Node Caching         │
└────────────────────────┘
           │
           ▼
┌────────────────────────┐
│ LearnAppCore (Android) │
│ 32 files               │
│ - Safety Managers      │
│ - AVU Exporters        │
│ - Framework Detection  │
└────────────────────────┘
```

---

## What's Already Migrated to VoiceOSCoreNG

| Component | Status | VoiceOSCoreNG File |
|-----------|--------|-------------------|
| JitProcessor | ✅ Basic | `features/JitProcessor.kt` |
| ExplorationEngine | ✅ Basic | `features/ExplorationEngine.kt` |
| LearnAppConfig | ✅ Done | `features/LearnAppConfig.kt` |
| NumberOverlay | ✅ Done | `features/NumberOverlayRenderer.kt` |
| ConfidenceOverlay | ✅ Done | `features/ConfidenceOverlay.kt` |
| CommandHandlers | ✅ Done | `handlers/*.kt` |
| NLU Integration | ✅ Done | `nlu/*.kt` |
| LLM Integration | ✅ Done | `llm/*.kt` |
| Speech Engines | ✅ Done | `features/*Engine*.kt` |
| ActionCoordinator | ✅ Done | `handlers/ActionCoordinator.kt` |
| CommandRegistry | ✅ Done | `common/CommandRegistry.kt` |
| ElementFingerprint | ✅ Done | `common/ElementFingerprint.kt` |

---

## Migration Phases

### Phase M1: Core Safety & Detection (Week 1)

**From LearnAppCore → VoiceOSCoreNG**

| Source File | Target (Flat KMP) | Priority |
|-------------|-------------------|----------|
| `safety/SafetyManager.kt` | `commonMain/SafetyManager.kt` | P0 |
| `safety/DoNotClickList.kt` | `commonMain/DoNotClickListModel.kt` | P0 |
| `safety/DoNotClickReason.kt` | `commonMain/DoNotClickReason.kt` | P0 |
| `safety/LoginScreenDetector.kt` | `commonMain/LoginScreenDetector.kt` | P1 |
| `safety/DynamicContentDetector.kt` | `commonMain/DynamicContentDetector.kt` | P1 |
| `safety/DynamicMenuHandler.kt` | `commonMain/DynamicMenuHandler.kt` | P2 |
| `safety/DynamicRegion.kt` | `commonMain/DynamicRegionModel.kt` | P2 |
| `safety/ScreenFingerprint.kt` | Already exists: `ElementFingerprint.kt` | - |
| `detection/CrossPlatformDetector.kt` | `commonMain/FrameworkDetector.kt` | P1 |

**KMP Considerations:**
- `SafetyManager.kt` - Pure Kotlin, can go directly to commonMain
- Pattern matching for login screens works cross-platform
- "Do Not Click" lists are JSON/config-based, cross-platform

---

### Phase M2: Element Processing (Week 1-2)

**From LearnAppCore → VoiceOSCoreNG**

| Source File | Target (Flat KMP) | Notes |
|-------------|-------------------|-------|
| `core/LearnAppCore.kt` | Already exists: `JitProcessor.kt` - ENHANCE | Merge logic |
| `core/ProcessingMode.kt` | Already exists: `ProcessingMode.kt` | ✅ |
| `core/ElementProcessingResult.kt` | Already exists: `ElementProcessingResult.kt` | ✅ |
| `core/IElementProcessorInterface.kt` | Merge into `IJitProcessor.kt` | P1 |
| `core/IBatchManagerInterface.kt` | Add to `JitProcessor.kt` | P1 |
| `models/ElementInfo.kt` | Already exists: `ElementInfo.kt` | ✅ |
| `processors/IUuidGenerator.kt` | Use AVID module | - |
| `processors/ICommandGenerator.kt` | `commonMain/ICommandGenerator.kt` | P1 |
| `processors/IFrameworkDetector.kt` | `commonMain/IFrameworkDetector.kt` | P1 |
| `processors/IBatchManager.kt` | Merge into `JitProcessor.kt` | P1 |
| `export/AVUExporter.kt` | `commonMain/AvuExporter.kt` | P2 |
| `export/CommandGenerator.kt` | Merge with existing CommandGenerator | P2 |

---

### Phase M3: JIT Service Migration (Week 2)

**From JITLearning → VoiceOSCoreNG**

| Source File | Target (Flat KMP) | Notes |
|-------------|-------------------|-------|
| `JITLearningService.kt` | `androidMain/JitLearningService.kt` | Android-only service |
| `JITState.kt` | `commonMain/JitStateModel.kt` | Cross-platform state |
| `JITLearnerProvider` interface | `commonMain/IJitLearner.kt` | Cross-platform interface |
| `ParcelableNodeInfo.kt` | `androidMain/ParcelableNodeInfo.kt` | Android-specific |
| `ScreenChangeEvent.kt` | `commonMain/ScreenChangeEvent.kt` | Cross-platform event |
| `ExplorationCommand.kt` | `commonMain/ExplorationCommand.kt` | Cross-platform |
| `ExplorationProgress.kt` | Already similar: `ExplorationStats.kt` | Merge |
| `handlers/NodeCacheManager.kt` | `androidMain/NodeCacheManager.kt` | Android-specific |
| `handlers/ElementInteractionHandler.kt` | `androidMain/ElementInteractionHandler.kt` | Android-specific |
| `handlers/ExplorationController.kt` | `androidMain/ExplorationController.kt` | Merge with ExplorationEngine |
| `handlers/ScreenCaptureHandler.kt` | `androidMain/ScreenCaptureHandler.kt` | Android-specific |
| `SecurityValidator.kt` | `commonMain/SecurityValidator.kt` | Cross-platform |

---

### Phase M4: VoiceOSCore Accessibility (Week 2-3)

**From VoiceOSCore → VoiceOSCoreNG**

| Source File | Target (Flat KMP) | Notes |
|-------------|-------------------|-------|
| `VoiceOSService.kt` | Keep in VoiceOS app (NOT in module) | App-level service |
| `scraping/ElementHasher.kt` | Already exists: `functions/HashUtils.kt` | ✅ |
| `scraping/ElementSearchEngine.kt` | `commonMain/ElementSearchEngine.kt` | P1 |
| `scraping/VoiceCommandProcessor.kt` | Already: `ActionCoordinator.kt` | ✅ |
| `scraping/ElementMatcher.kt` | `commonMain/ElementMatcher.kt` | P1 |
| `scraping/SemanticInferenceHelper.kt` | `commonMain/SemanticInferenceHelper.kt` | P2 |
| `scraping/CommandGenerator.kt` | Merge with existing | - |
| `lifecycle/AccessibilityNodeManager.kt` | `androidMain/AccessibilityNodeManager.kt` | Android-only |
| `lifecycle/SafeNodeTraverser.kt` | `androidMain/SafeNodeTraverser.kt` | Android-only |
| `lifecycle/AsyncQueryManager.kt` | `androidMain/AsyncQueryManager.kt` | Android-only |
| `utils/NodeRecyclingUtils.kt` | `androidMain/NodeRecyclingUtils.kt` | Android-only |

---

### Phase M5: UI Components (Week 3)

**From VoiceOSCore → VoiceOSCoreNG**

| Source File | Target (Flat KMP) | Notes |
|-------------|-------------------|-------|
| `ui/overlays/NumberOverlayManager.kt` | Already: `OverlayManager.kt` | ✅ |
| `ui/overlays/NumberBadgeView.kt` | `androidMain/NumberBadgeView.kt` | Android-specific |
| `ui/overlays/NumberOverlayConfig.kt` | Already: `OverlayConfig.kt` | ✅ |
| `ui/GlassmorphismUtils.kt` | `androidMain/GlassmorphismUtils.kt` | Android-specific |
| `ui/theme/GlassmorphicComponents.kt` | `androidMain/GlassmorphicComponents.kt` | Android Compose |
| `ui/theme/OceanThemeExtensions.kt` | `androidMain/OceanThemeExtensions.kt` | Android Compose |
| `ui/components/FloatingEngineSelector.kt` | `androidMain/FloatingEngineSelector.kt` | Android Compose |

---

### Phase M6: Utilities & Security (Week 3)

**From VoiceOSCore → VoiceOSCoreNG**

| Source File | Target (Flat KMP) | Notes |
|-------------|-------------------|-------|
| `utils/HashUtils.kt` | Already: `functions/HashUtils.kt` | ✅ |
| `utils/DisplayUtils.kt` | `androidMain/DisplayUtils.kt` | Android-specific |
| `utils/ConditionalLogger.kt` | `commonMain/ConditionalLogger.kt` | Cross-platform |
| `utils/CommandRateLimiter.kt` | `commonMain/CommandRateLimiter.kt` | Cross-platform |
| `utils/DatabaseIntegrityChecker.kt` | Move to database module | - |
| `security/InputValidator.kt` | `commonMain/InputValidator.kt` | Cross-platform |
| `security/DataEncryptionManager.kt` | `androidMain/DataEncryptionManager.kt` | Android KeyStore |

---

## KMP Flat Folder Structure (MANDATORY)

**Rules:**
1. NO nested folders - all files at package root
2. Use suffix naming: `*Handler.kt`, `*Manager.kt`, `*Model.kt`, `*Service.kt`
3. Platform files: `{Name}.android.kt`, `{Name}.ios.kt`

**Target Structure:**
```
VoiceOSCoreNG/src/
├── commonMain/kotlin/com/augmentalis/voiceoscoreng/
│   ├── VoiceOSCoreNG.kt                 # Main facade
│   ├── ActionCoordinator.kt             # Command routing
│   ├── SafetyManager.kt                 # Safety checks (NEW)
│   ├── DoNotClickListModel.kt           # Safety model (NEW)
│   ├── DoNotClickReason.kt              # Safety enum (NEW)
│   ├── LoginScreenDetector.kt           # Login detection (NEW)
│   ├── DynamicContentDetector.kt        # Dynamic UI (NEW)
│   ├── FrameworkDetector.kt             # Framework detection (NEW)
│   ├── ElementSearchEngine.kt           # Search (NEW)
│   ├── ElementMatcher.kt                # Matching (NEW)
│   ├── SemanticInferenceHelper.kt       # Inference (NEW)
│   ├── SecurityValidator.kt             # Security (NEW)
│   ├── InputValidator.kt                # Input validation (NEW)
│   ├── CommandRateLimiter.kt            # Rate limiting (NEW)
│   ├── ConditionalLogger.kt             # Logging (NEW)
│   ├── JitStateModel.kt                 # JIT state (NEW)
│   ├── IJitLearner.kt                   # JIT interface (NEW)
│   ├── ScreenChangeEvent.kt             # Events (NEW)
│   ├── ExplorationCommand.kt            # Commands (NEW)
│   ├── ICommandGenerator.kt             # Interface (NEW)
│   ├── AvuExporter.kt                   # AVU export (NEW)
│   └── ... (existing files)
│
├── androidMain/kotlin/com/augmentalis/voiceoscoreng/
│   ├── JitLearningService.kt            # Foreground service (NEW)
│   ├── AccessibilityNodeManager.kt      # Node management (NEW)
│   ├── SafeNodeTraverser.kt             # Safe traversal (NEW)
│   ├── NodeCacheManager.kt              # Node caching (NEW)
│   ├── ElementInteractionHandler.kt     # Interactions (NEW)
│   ├── ExplorationController.kt         # Controller (NEW)
│   ├── ScreenCaptureHandler.kt          # Screen capture (NEW)
│   ├── ParcelableNodeInfo.kt            # Parcelable (NEW)
│   ├── NumberBadgeView.kt               # Badge view (NEW)
│   ├── GlassmorphismUtils.kt            # Glass effects (NEW)
│   ├── DisplayUtils.kt                  # Display utils (NEW)
│   ├── NodeRecyclingUtils.kt            # Node recycling (NEW)
│   ├── DataEncryptionManager.kt         # Encryption (NEW)
│   └── ... (existing files)
│
├── iosMain/kotlin/com/augmentalis/voiceoscoreng/
│   └── ... (future iOS implementations)
│
└── desktopMain/kotlin/com/augmentalis/voiceoscoreng/
    └── ... (future desktop implementations)
```

---

## Files to DELETE After Migration

### From JITLearning (Move to Legacy Development)

After migration complete:
```
Legacy Development/Modules/VoiceOS/libraries/JITLearning/
├── All 17 source files
└── Keep only README explaining migration
```

### From LearnAppCore (Move to Legacy Development)

After migration complete:
```
Legacy Development/Modules/VoiceOS/libraries/LearnAppCore/
├── All 32 source files
└── Keep only README explaining migration
```

### From VoiceOSCore

**DO NOT DELETE YET** - VoiceOSCore serves as the Android app wrapper. However:
- Remove duplicated logic that's now in VoiceOSCoreNG
- Update imports to use VoiceOSCoreNG classes
- Keep only Android-specific service bindings

---

## Dependency Updates

### settings.gradle.kts Changes

```kotlin
// BEFORE
include(":Modules:VoiceOS:libraries:JITLearning")    // DEPRECATED
include(":Modules:VoiceOS:libraries:LearnAppCore")   // DEPRECATED
include(":Modules:VoiceOS:VoiceOSCore")              // Keep for now

// AFTER (when migration complete)
// include(":Modules:VoiceOS:libraries:JITLearning")    // REMOVED
// include(":Modules:VoiceOS:libraries:LearnAppCore")   // REMOVED
include(":Modules:VoiceOS:VoiceOSCore")              // Thin wrapper only
```

### VoiceOSCore build.gradle.kts Changes

```kotlin
// BEFORE
dependencies {
    implementation(project(":Modules:VoiceOSCoreNG"))
    implementation(project(":Modules:VoiceOS:libraries:JITLearning"))
    implementation(project(":Modules:VoiceOS:libraries:LearnAppCore"))
}

// AFTER
dependencies {
    implementation(project(":Modules:VoiceOSCoreNG"))  // All logic here
    // JITLearning - REMOVED (merged into VoiceOSCoreNG)
    // LearnAppCore - REMOVED (merged into VoiceOSCoreNG)
}
```

---

## Migration Verification Checklist

### Per-Phase Checklist

- [ ] All source files migrated to correct KMP source set
- [ ] Flat folder structure enforced (no nested packages)
- [ ] Proper file naming with suffixes
- [ ] Unit tests migrated/created
- [ ] Build passes on all platforms
- [ ] No import errors from old packages

### Final Migration Checklist

- [ ] VoiceOSCoreNG builds successfully
- [ ] All tests pass
- [ ] JITLearning module removed from settings.gradle.kts
- [ ] LearnAppCore module removed from settings.gradle.kts
- [ ] VoiceOSCore updated to only wrap VoiceOSCoreNG
- [ ] Legacy Development folder contains old modules
- [ ] Documentation updated

---

## Risk Assessment

| Risk | Mitigation |
|------|------------|
| Breaking AIDL IPC | Keep AIDL service in VoiceOSCore app, not module |
| Android-specific APIs in commonMain | Use expect/actual pattern |
| Test coverage gaps | Write tests before removing old code |
| Build failures | Incremental migration, verify each phase |

---

## Timeline

| Phase | Duration | Deliverable |
|-------|----------|-------------|
| M1 | 2-3 days | Safety & Detection migrated |
| M2 | 2-3 days | Element Processing enhanced |
| M3 | 3-4 days | JIT Service migrated |
| M4 | 3-4 days | Accessibility components migrated |
| M5 | 2-3 days | UI components migrated |
| M6 | 2-3 days | Utilities migrated |
| **Total** | **~3 weeks** | Full consolidation |

---

## Post-Migration Cleanup

1. Move `JITLearning` folder to `Legacy Development/Modules/VoiceOS/libraries/`
2. Move `LearnAppCore` folder to `Legacy Development/Modules/VoiceOS/libraries/`
3. Update `android/apps/VoiceOS` to use VoiceOSCoreNG directly
4. Remove deprecated imports from VoiceOSCore
5. Update MasterDocs to reflect new architecture

---

**Author:** Claude (ICA) | **Version:** 1.0 | **Date:** 2026-01-16
