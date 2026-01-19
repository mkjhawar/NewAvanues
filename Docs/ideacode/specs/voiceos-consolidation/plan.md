# VoiceOS Consolidation Implementation Plan

**Version:** 1.0
**Date:** 2026-01-17
**Author:** Claude AI / Manoj Jhawar
**Spec:** spec.md

---

## Chain of Thought (CoT) Analysis

### Why This Order?

1. **Foundation First** - Create module skeleton before moving code
2. **Common Before Platform** - Dependencies flow common → platform
3. **Database Early** - Many modules depend on database
4. **Android Last** - Most complex, depends on all common code
5. **Consumers After Source** - Can't update consumers until source is ready

### KMP Considerations

- Android has 90% of platform-specific code
- iOS/Desktop can use stubs initially
- SQLDelight works on all platforms
- Coroutines/Serialization are KMP-ready

### Swarm Assessment

**Recommended: YES** - 3+ platforms, 25+ tasks, complex integration points

Parallel workstreams:
- **Stream A**: Common code migration (Phase 2)
- **Stream B**: Android code migration (Phase 3) - after A completes
- **Stream C**: Consumer updates (Phase 4) - after B completes

---

## Overview

| Metric | Value |
|--------|-------|
| Platforms | Android, iOS, Desktop |
| Total Phases | 6 |
| Total Tasks | 35 |
| Estimated Files | ~900 |
| Swarm Recommended | Yes (3 streams) |

---

## Phase 1: Foundation (Create Module Structure)

**Goal:** Create the VoiceOSCore module skeleton with proper KMP configuration.

### P1T01: Create Module Directory Structure
```bash
mkdir -p Modules/VoiceOSCore/src/{commonMain,commonTest,androidMain,androidTest,iosMain,desktopMain}/kotlin/com/augmentalis/voiceoscore
mkdir -p Modules/VoiceOSCore/src/androidMain/{res,aidl}
mkdir -p Modules/VoiceOSCore/src/commonMain/sqldelight/com/augmentalis/database
```

### P1T02: Create build.gradle.kts
Create unified KMP build configuration with:
- kotlin.multiplatform plugin
- android.library plugin
- kotlin.serialization plugin
- sqldelight plugin
- All targets: androidTarget, iosX64, iosArm64, iosSimulatorArm64, jvm("desktop")

### P1T03: Create AndroidManifest.xml
- Package: `com.augmentalis.voiceoscore`
- Permissions: ACCESSIBILITY_SERVICE, RECORD_AUDIO, INTERNET, etc.
- Services: VoiceOSService with accessibility-service meta-data

### P1T04: Register Module in settings.gradle.kts
Add `include(":Modules:VoiceOSCore")` to root settings.gradle.kts

### P1T05: Verify Foundation Builds
Run `./gradlew :Modules:VoiceOSCore:build` - should succeed with empty module

---

## Phase 2: Common Code Migration (Platform-Agnostic)

**Goal:** Migrate all cross-platform utilities to commonMain.

### P2T01: Migrate core/result
Source: `Modules/VoiceOS/core/result/src/commonMain/kotlin/`
Target: `Modules/VoiceOSCore/src/commonMain/kotlin/com/augmentalis/voiceoscore/result/`
Files: ~5

### P2T02: Migrate core/hash
Source: `Modules/VoiceOS/core/hash/src/commonMain/kotlin/`
Target: `Modules/VoiceOSCore/src/commonMain/kotlin/com/augmentalis/voiceoscore/hash/`
Files: ~5

### P2T03: Migrate core/constants
Source: `Modules/VoiceOS/core/constants/src/commonMain/kotlin/`
Target: `Modules/VoiceOSCore/src/commonMain/kotlin/com/augmentalis/voiceoscore/constants/`
Files: ~10

### P2T04: Migrate core/exceptions
Source: `Modules/VoiceOS/core/exceptions/src/commonMain/kotlin/`
Target: `Modules/VoiceOSCore/src/commonMain/kotlin/com/augmentalis/voiceoscore/exceptions/`
Files: ~8

### P2T05: Migrate core/text-utils
Source: `Modules/VoiceOS/core/text-utils/src/commonMain/kotlin/`
Target: `Modules/VoiceOSCore/src/commonMain/kotlin/com/augmentalis/voiceoscore/text/`
Files: ~12

### P2T06: Migrate core/json-utils
Source: `Modules/VoiceOS/core/json-utils/src/commonMain/kotlin/`
Target: `Modules/VoiceOSCore/src/commonMain/kotlin/com/augmentalis/voiceoscore/json/`
Files: ~8

### P2T07: Migrate core/validation
Source: `Modules/VoiceOS/core/validation/src/commonMain/kotlin/`
Target: `Modules/VoiceOSCore/src/commonMain/kotlin/com/augmentalis/voiceoscore/validation/`
Files: ~10

### P2T08: Migrate core/command-models
Source: `Modules/VoiceOS/core/command-models/src/commonMain/kotlin/`
Target: `Modules/VoiceOSCore/src/commonMain/kotlin/com/augmentalis/voiceoscore/command/`
Files: ~15

### P2T09: Migrate core/accessibility-types
Source: `Modules/VoiceOS/core/accessibility-types/src/commonMain/kotlin/`
Target: `Modules/VoiceOSCore/src/commonMain/kotlin/com/augmentalis/voiceoscore/accessibility/`
Files: ~8

### P2T10: Migrate core/voiceos-logging
Source: `Modules/VoiceOS/core/voiceos-logging/src/commonMain/kotlin/`
Target: `Modules/VoiceOSCore/src/commonMain/kotlin/com/augmentalis/voiceoscore/logging/`
Files: ~12

### P2T11: Migrate core/database (SQLDelight)
Source: `Modules/VoiceOS/core/database/src/commonMain/`
Target: `Modules/VoiceOSCore/src/commonMain/`
Includes:
- `sqldelight/` schemas
- `kotlin/` repositories
Files: ~20

### P2T12: Migrate Voice/Core commonMain
Source: `Modules/Voice/Core/src/commonMain/kotlin/`
Target: `Modules/VoiceOSCore/src/commonMain/kotlin/com/augmentalis/voiceoscore/`
Includes:
- `synonym/` - Synonym management
- `llm/` - LLM interfaces
- `nlu/` - NLU interfaces
- `cursor/` - Cursor control
- `exploration/` - UI exploration
- `jit/` - JIT learning
- `e2e/` - E2E command flow
- `extraction/` - Data extraction
- `safety/` - Safety utilities
Files: ~173

### P2T13: Migrate Voice/Core commonMain resources
Source: `Modules/Voice/Core/src/commonMain/resources/`
Target: `Modules/VoiceOSCore/src/commonMain/resources/`
Includes: Synonym dictionaries, extraction data
Files: ~200 resource files

### P2T14: Update Package References (commonMain)
- Update all imports to use `com.augmentalis.voiceoscore.*`
- Create typealias for backward compatibility where needed
- Update internal references

### P2T15: Verify Common Code Builds
Run `./gradlew :Modules:VoiceOSCore:compileKotlinMetadata`

---

## Phase 3: Android Code Migration

**Goal:** Migrate all Android-specific implementations to androidMain.

### P3T01: Migrate Voice/Core androidMain
Source: `Modules/Voice/Core/src/androidMain/kotlin/`
Target: `Modules/VoiceOSCore/src/androidMain/kotlin/com/augmentalis/voiceoscore/`
Includes:
- `llm/` - Android LLM implementations
- `nlu/` - Android NLU implementations
- `speech/` - Vivoka/Vosk engines
- `synonym/` - Android path providers
- `handlers/` - Event handlers
- `functions/` - Platform utilities
Files: ~48

### P3T02: Migrate VoiceOSCore Services
Source: `Modules/VoiceOS/VoiceOSCore/src/main/java/.../service/`
Target: `Modules/VoiceOSCore/src/androidMain/kotlin/com/augmentalis/voiceoscore/service/`
Includes:
- VoiceOSService.kt (AccessibilityService)
- AIDL bindings
- Service lifecycle management
Files: ~50

### P3T03: Migrate VoiceOSCore UI
Source: `Modules/VoiceOS/VoiceOSCore/src/main/java/.../ui/`
Target: `Modules/VoiceOSCore/src/androidMain/kotlin/com/augmentalis/voiceoscore/ui/`
Includes:
- Compose overlays
- Glass-morphism effects
- Number overlay system
- Theme management
Files: ~100

### P3T04: Migrate VoiceOSCore Scraping
Source: `Modules/VoiceOS/VoiceOSCore/src/main/java/.../scraping/`
Target: `Modules/VoiceOSCore/src/androidMain/kotlin/com/augmentalis/voiceoscore/scraping/`
Includes:
- Element hashing
- Semantic inference
- Element search
- Extraction utilities
Files: ~80

### P3T05: Migrate VoiceOSCore AIDL
Source: `Modules/VoiceOS/VoiceOSCore/src/main/aidl/`
Target: `Modules/VoiceOSCore/src/androidMain/aidl/`
Files: ~5

### P3T06: Migrate VoiceOSCore Resources
Source: `Modules/VoiceOS/VoiceOSCore/src/main/res/`
Target: `Modules/VoiceOSCore/src/androidMain/res/`
Includes: anim, drawable, layout, values, xml
Files: ~50

### P3T07: Migrate Remaining VoiceOSCore Code
Source: `Modules/VoiceOS/VoiceOSCore/src/main/java/`
Target: `Modules/VoiceOSCore/src/androidMain/kotlin/com/augmentalis/voiceoscore/`
Includes all remaining Android-specific code not covered above
Files: ~200

### P3T08: Migrate core/database androidMain
Source: `Modules/VoiceOS/core/database/src/androidMain/kotlin/`
Target: `Modules/VoiceOSCore/src/androidMain/kotlin/com/augmentalis/voiceoscore/database/`
Includes: DatabaseDriverFactory.android.kt
Files: ~5

### P3T09: Update Package References (androidMain)
- Update all imports to use `com.augmentalis.voiceoscore.*`
- Update resource references (R.*)
- Update AIDL package references

### P3T10: Verify Android Code Builds
Run `./gradlew :Modules:VoiceOSCore:compileDebugKotlin`

---

## Phase 4: Consumer Updates

**Goal:** Update all modules that depend on the old VoiceOS modules.

### P4T01: Update android/apps/VoiceOS/app
- Change dependency from `:Modules:Voice:Core` to `:Modules:VoiceOSCore`
- Change dependency from `:Modules:VoiceOS:VoiceOSCore` to `:Modules:VoiceOSCore`
- Remove all `:Modules:VoiceOS:core:*` dependencies
- Update imports in source files

### P4T02: Update android/apps/VoiceUI
- Update dependencies to `:Modules:VoiceOSCore`
- Update imports

### P4T03: Update android/apps/VoiceCursor
- Update dependencies to `:Modules:VoiceOSCore`
- Update imports

### P4T04: Update Modules/VoiceOS/libraries/JITLearning
- Update dependency from `:Modules:Voice:Core` to `:Modules:VoiceOSCore`
- Update imports

### P4T05: Update Modules/VoiceOS/libraries/AvidCreator
- Update dependencies
- Update imports (if any reference Voice/Core)

### P4T06: Update Modules/VoiceOS/managers/*
All 5 managers:
- CommandManager
- HUDManager
- LicenseManager
- LocalizationManager
- VoiceDataManager

### P4T07: Update root settings.gradle.kts
- Add `:Modules:VoiceOSCore`
- Comment out old modules (don't remove yet)

### P4T08: Verify All Consumers Build
Run `./gradlew :android:apps:VoiceOS:app:assembleDebug`

---

## Phase 5: Cleanup

**Goal:** Remove deprecated modules and update documentation.

### P5T01: Create Deprecation Aliases
In `Modules/Voice/Core/build.gradle.kts`:
```kotlin
// DEPRECATED: Use :Modules:VoiceOSCore instead
// This module forwards to VoiceOSCore for backward compatibility
```
Create forwarding module if needed for gradual migration

### P5T02: Mark Old Modules as Deprecated
Add DEPRECATED.md to:
- `Modules/Voice/Core/`
- `Modules/VoiceOS/VoiceOSCore/`
- `Modules/VoiceOS/core/*/`

### P5T03: Delete LearnAppCore
Module is already deprecated, safe to remove:
- `Modules/VoiceOS/libraries/LearnAppCore/`

### P5T04: Update MasterDocs
- Update `Docs/MasterDocs/AI/PLATFORM-INDEX.ai.md`
- Update `Docs/MasterDocs/AI/CLASS-INDEX.ai.md`
- Update `Docs/MasterDocs/VoiceOS/README.md`

### P5T05: Update MASTER-INDEX.md
Document the new consolidated structure

---

## Phase 6: Verification

**Goal:** Full test suite and CI validation.

### P6T01: Run All Unit Tests
```bash
./gradlew :Modules:VoiceOSCore:test
```

### P6T02: Run All Android Tests
```bash
./gradlew :Modules:VoiceOSCore:connectedAndroidTest
```

### P6T03: Full Build Verification
```bash
./gradlew build
```

### P6T04: CI Pipeline Update
Update CI configuration to:
- Build new module
- Run tests on new module
- Remove old module builds (after transition period)

---

## Time Estimates

| Phase | Sequential | Parallel (Swarm) |
|-------|------------|------------------|
| Phase 1: Foundation | 2 hours | 2 hours |
| Phase 2: Common Code | 8 hours | 4 hours |
| Phase 3: Android Code | 12 hours | 6 hours |
| Phase 4: Consumer Updates | 4 hours | 2 hours |
| Phase 5: Cleanup | 2 hours | 2 hours |
| Phase 6: Verification | 2 hours | 2 hours |
| **Total** | **30 hours** | **18 hours** |

**Savings with Swarm:** 12 hours (40%)

---

## Swarm Configuration

### Stream A: Common Code (Phase 2)
- Agent: KMP specialist
- Focus: commonMain migration, SQLDelight
- Blocks: Phase 3 (Android needs common code)

### Stream B: Android Code (Phase 3)
- Agent: Android specialist
- Focus: androidMain migration, AIDL, resources
- Requires: Stream A completion
- Blocks: Phase 4

### Stream C: Consumer Updates (Phase 4)
- Agent: Integration specialist
- Focus: Dependency updates, import fixes
- Requires: Stream B completion

---

## Rollback Plan

If migration fails:
1. Keep old modules intact (don't delete until verified)
2. Revert settings.gradle.kts changes
3. Consumers fallback to old module paths

---

## Next Steps

1. **Approve this plan**
2. Run `/i.implement plan.md` or `/i.implement plan.md .swarm`
3. Monitor progress in TodoWrite
4. Run verification in Phase 6

---

**Ready for implementation.**
