# VoiceOS Module Consolidation Specification

**Version:** 1.0
**Date:** 2026-01-17
**Author:** Claude AI / Manoj Jhawar
**Status:** Ready for Implementation

---

## Overview

Consolidate all fragmented VoiceOS modules into a single unified KMP module at `/Modules/VoiceOSCore`. This eliminates the redundant `Modules/Voice` and `Modules/VoiceOS` folder structure and creates a clean architecture where the VoiceOS app becomes a thin UI shell.

## Current State (Fragmented)

```
Modules/
├── Voice/
│   ├── Core/                    # KMP module (VoiceOSCoreNG) - 221 files
│   └── WakeWord/                # Android wake word detection
├── VoiceOS/
│   ├── VoiceOSCore/             # Android-only library - 490 files
│   ├── core/                    # 11 utility modules
│   │   ├── database/            # SQLDelight database
│   │   ├── result/              # Result type
│   │   ├── constants/           # Constants
│   │   ├── command-models/      # Command DTOs
│   │   ├── accessibility-types/ # Accessibility enums
│   │   ├── hash/                # Hashing utilities
│   │   ├── text-utils/          # Text processing
│   │   ├── json-utils/          # JSON utilities
│   │   ├── validation/          # Validation
│   │   ├── exceptions/          # Exception hierarchy
│   │   └── voiceos-logging/     # PII-safe logging
│   ├── libraries/
│   │   ├── AvidCreator/         # AVID generation
│   │   ├── JITLearning/         # JIT learning system
│   │   ├── LearnAppCore/        # DEPRECATED
│   │   ├── PluginSystem/        # Plugin architecture
│   │   ├── UniversalIPC/        # IPC protocol
│   │   └── VoiceOsLogging/      # Logging (duplicate)
│   └── managers/
│       ├── CommandManager/      # Command coordination
│       ├── HUDManager/          # HUD/gaze tracking
│       ├── LicenseManager/      # License validation
│       ├── LocalizationManager/ # i18n
│       └── VoiceDataManager/    # Data persistence
```

**Problems:**
1. 15+ separate Gradle modules for what should be one cohesive library
2. Duplicate code between Voice/Core and VoiceOS/VoiceOSCore
3. Confusing dependency graph (modules depending on each other in circles)
4. Android-specific code mixed with cross-platform code
5. No clear separation of platform concerns

## Target State (Unified)

```
Modules/
├── VoiceOSCore/                 # Single unified KMP module
│   ├── src/
│   │   ├── commonMain/          # All cross-platform shared logic
│   │   │   └── kotlin/com/augmentalis/voiceoscore/
│   │   │       ├── result/      # Result<T> utilities
│   │   │       ├── hash/        # Platform-agnostic hashing
│   │   │       ├── constants/   # Configuration constants
│   │   │       ├── command/     # Command models
│   │   │       ├── accessibility/ # Accessibility types
│   │   │       ├── text/        # Text utilities
│   │   │       ├── json/        # JSON utilities
│   │   │       ├── validation/  # Validation
│   │   │       ├── exceptions/  # Exception hierarchy
│   │   │       ├── logging/     # Cross-platform logging
│   │   │       ├── database/    # SQLDelight database
│   │   │       ├── synonym/     # Synonym management
│   │   │       ├── llm/         # LLM interface
│   │   │       ├── nlu/         # NLU interface
│   │   │       ├── cursor/      # Cursor control
│   │   │       ├── exploration/ # UI exploration
│   │   │       ├── jit/         # JIT learning
│   │   │       └── e2e/         # E2E command flow
│   │   ├── androidMain/         # Android-specific implementations
│   │   │   ├── kotlin/com/augmentalis/voiceoscore/
│   │   │   │   ├── service/     # AccessibilityService
│   │   │   │   ├── ui/          # Compose overlays
│   │   │   │   ├── scraping/    # Element extraction
│   │   │   │   ├── speech/      # Vivoka, Vosk engines
│   │   │   │   ├── llm/         # Android LLM impl
│   │   │   │   ├── nlu/         # Android NLU impl
│   │   │   │   └── ipc/         # AIDL bindings
│   │   │   ├── res/             # Android resources
│   │   │   └── aidl/            # AIDL interfaces
│   │   ├── iosMain/             # iOS implementations
│   │   │   └── kotlin/com/augmentalis/voiceoscore/
│   │   │       ├── speech/      # Apple speech engine
│   │   │       ├── llm/         # iOS LLM impl
│   │   │       └── nlu/         # iOS NLU impl
│   │   └── desktopMain/         # Desktop implementations
│   │       └── kotlin/com/augmentalis/voiceoscore/
│   │           └── ...
│   └── build.gradle.kts         # Single unified build
│
└── VoiceOSFeatures/             # Optional: Feature modules (thin wrappers)
    ├── WakeWord/                # Wake word detection
    ├── AvidCreator/             # AVID utilities (if needed separate)
    └── PluginSystem/            # Plugin architecture (if needed separate)
```

**Benefits:**
1. Single Gradle module = faster builds, simpler dependency management
2. Clear platform separation (commonMain vs androidMain vs iosMain)
3. No circular dependencies
4. VoiceOS app becomes thin UI shell
5. Easier to maintain and test

## Scope

### In Scope

1. **Merge Voice/Core into VoiceOSCore** - All 221 KMP files
2. **Merge VoiceOS/VoiceOSCore into androidMain** - All 490 Android files
3. **Merge all 11 core/* modules into commonMain** - ~100 files
4. **Update all consumers** - VoiceOS app, VoiceUI, VoiceCursor, etc.
5. **Update all Gradle configurations** - settings.gradle.kts across repo
6. **Delete deprecated modules** - LearnAppCore, duplicates
7. **Comprehensive testing** - Unit tests, integration tests

### Out of Scope

1. iOS/Desktop implementations (stubs only, not full implementation)
2. WakeWord migration (separate feature, can be done later)
3. Plugin system redesign (keep as separate module)
4. New feature development

## Requirements

### REQ-001: Single Module Structure
- All VoiceOS core logic MUST be in `/Modules/VoiceOSCore`
- Module MUST support KMP targets: Android, iOS, Desktop
- Package name: `com.augmentalis.voiceoscore`

### REQ-002: Source Set Organization
- Common code (platform-agnostic) → `commonMain`
- Android implementations → `androidMain`
- iOS implementations → `iosMain`
- Desktop implementations → `desktopMain`

### REQ-003: Backward Compatibility
- All existing public APIs MUST be preserved
- Deprecation warnings for renamed/moved classes
- Migration period of at least 2 weeks

### REQ-004: Build Configuration
- Single `build.gradle.kts` with all targets
- SQLDelight configured for all platforms
- Proper dependency management (api vs implementation)

### REQ-005: Testing
- All existing tests MUST pass after migration
- New tests for critical migration paths
- Build verification on CI

## Success Criteria

1. ✅ `./gradlew :Modules:VoiceOSCore:build` succeeds
2. ✅ `./gradlew :android:apps:VoiceOS:app:assembleDebug` succeeds
3. ✅ All unit tests pass
4. ✅ No duplicate code between modules
5. ✅ Clear separation in source sets
6. ✅ Documentation updated

## Migration Strategy

**Phase 1: Foundation** - Create VoiceOSCore module structure
**Phase 2: Common Code** - Migrate all platform-agnostic utilities
**Phase 3: Android Code** - Migrate Android-specific implementations
**Phase 4: Dependencies** - Update all consumers
**Phase 5: Cleanup** - Remove old modules, update docs
**Phase 6: Verification** - Full test suite, CI validation

## Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Circular dependencies | Medium | High | Careful dependency ordering during migration |
| Breaking changes | Low | High | Preserve all public APIs, use typealias for renames |
| Build failures | Medium | Medium | Incremental migration with CI checks |
| Missing platform code | Low | Medium | Stub implementations for non-Android platforms |

---

**Next:** See `plan.md` for detailed implementation plan.
