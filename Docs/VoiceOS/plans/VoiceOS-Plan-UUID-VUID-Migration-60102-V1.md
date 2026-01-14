# Implementation Plan: Complete UUID to VUID Migration

## Overview

| Attribute | Value |
|-----------|-------|
| Platforms | Android (VoiceOS) |
| Swarm Recommended | No (single platform, sequential tasks) |
| Estimated Tasks | 12 tasks |
| Estimated Time | 2-3 hours |
| Priority | High (removes redundancy, unblocks testing) |

## Problem Statement

The codebase has **duplicate database tables and code paths**:
- `uuid/` folder: UUIDAlias.sq, UUIDAnalytics.sq, UUIDElement.sq, UUIDHierarchy.sq
- `vuid/` folder: VUIDAlias.sq, VUIDAnalytics.sq, VUIDElement.sq, VUIDHierarchy.sq

Additionally:
- 108 UUID* imports across 24 files
- 48 VUID* imports across 17 files
- Library named `UUIDCreator` should be `VUIDCreator`
- Repository interfaces use both patterns

## Migration Strategy

**Approach:** Rename/update in place with typealiases for backward compatibility during transition.

## Phases

### Phase 1: Database Schema Cleanup (SQLDelight)

**Goal:** Remove duplicate uuid/ tables, keep only vuid/ tables.

| Task | File/Action | Status |
|------|-------------|--------|
| 1.1 | Delete `uuid/UUIDAlias.sq` | Pending |
| 1.2 | Delete `uuid/UUIDAnalytics.sq` | Pending |
| 1.3 | Delete `uuid/UUIDElement.sq` | Pending |
| 1.4 | Delete `uuid/UUIDHierarchy.sq` | Pending |
| 1.5 | Delete `uuid/` folder | Pending |
| 1.6 | Regenerate SQLDelight code | Pending |

**Files:**
- `Modules/VoiceOS/core/database/src/commonMain/sqldelight/com/augmentalis/database/uuid/`

### Phase 2: Repository Interface Migration

**Goal:** Update repository interfaces from UUID* to VUID*.

| Task | File | Action |
|------|------|--------|
| 2.1 | `IUUIDRepository.kt` | Delete (use IVUIDRepository) |
| 2.2 | `SQLDelightUUIDRepository.kt` | Delete (use SQLDelightVUIDRepository) |
| 2.3 | Update all imports | Replace UUID* with VUID* |

**Files:**
- `Modules/VoiceOS/core/database/src/commonMain/kotlin/com/augmentalis/database/repositories/IUUIDRepository.kt`
- `Modules/VoiceOS/core/database/src/commonMain/kotlin/com/augmentalis/database/repositories/impl/SQLDelightUUIDRepository.kt`

### Phase 3: UUIDCreator Library Refactor

**Goal:** Rename library package and classes to VUIDCreator.

| Task | Current | New |
|------|---------|-----|
| 3.1 | `UUIDCreator.kt` class | Keep as typealias to VUIDCreator |
| 3.2 | `UuidAliasManager.kt` | Rename to `VuidAliasManager.kt` |
| 3.3 | Library folder `UUIDCreator/` | Keep path, update internal names |
| 3.4 | Package `com.augmentalis.uuidcreator` | Add deprecation annotations |

**Strategy:** Add typealiases + deprecation warnings rather than breaking rename.

**Files:**
- `Modules/VoiceOS/libraries/UUIDCreator/src/main/java/com/augmentalis/uuidcreator/`

### Phase 4: VoiceOSCore Integration Updates

**Goal:** Update 24 files with UUID* imports to use VUID* equivalents.

| Task | File | Import Changes |
|------|------|----------------|
| 4.1 | `AccessibilityScrapingIntegration.kt` | 5 imports → VUID* |
| 4.2 | `VoiceOSService.kt` | 1 import → VUID* |
| 4.3 | `LearnAppIntegration.kt` | 2 imports → VUID* |
| 4.4 | `ExplorationEngine.kt` | 4 imports → VUID* |
| 4.5 | Other 20 files | Update as needed |

**Files in scope:**
```
Modules/VoiceOS/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/scraping/AccessibilityScrapingIntegration.kt
Modules/VoiceOS/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/accessibility/VoiceOSService.kt
Modules/VoiceOS/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/learnapp/integration/LearnAppIntegration.kt
Modules/VoiceOS/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/learnapp/exploration/ExplorationEngine.kt
```

### Phase 5: Test File Updates

**Goal:** Update test files to use VUID* patterns.

| Task | File | Action |
|------|------|--------|
| 5.1 | `UUIDRepositoryIntegrationTest.kt` | Delete or rename to VUIDRepositoryIntegrationTest |
| 5.2 | `UUIDCreatorIntegrationTest.kt` | Update imports |
| 5.3 | Other test files | Update as needed |

### Phase 6: Build Verification

**Goal:** Verify migration doesn't break build.

| Task | Command | Expected |
|------|---------|----------|
| 6.1 | `./gradlew :Modules:VoiceOS:core:database:generateCommonMainVoiceOSDatabaseInterface` | Success |
| 6.2 | `./gradlew :Modules:VoiceOS:apps:VoiceOSCore:assembleDebug` | Success |
| 6.3 | `./gradlew :Modules:VoiceOS:core:database:test` | Pass |
| 6.4 | Install and test on emulator | No VUID errors |

## Typealias Strategy (Backward Compatibility)

To avoid breaking external code, add typealiases in `UUIDCreator.kt`:

```kotlin
// Deprecated typealiases for backward compatibility
@Deprecated("Use VUIDCreator instead", ReplaceWith("VUIDCreator"))
typealias UUIDCreator = VUIDCreator

@Deprecated("Use VUIDElement instead", ReplaceWith("VUIDElement"))
typealias UUIDElement = VUIDElement

@Deprecated("Use VUIDAccessibility instead", ReplaceWith("VUIDAccessibility"))
typealias UUIDAccessibility = VUIDAccessibility

@Deprecated("Use VUIDMetadata instead", ReplaceWith("VUIDMetadata"))
typealias UUIDMetadata = VUIDMetadata
```

## Risk Assessment

| Risk | Mitigation |
|------|------------|
| Breaking existing database | VUIDAlias/VUIDElement tables already exist |
| Breaking external imports | Typealiases preserve old names |
| Test failures | Run full test suite before commit |

## Files to Delete

```
Modules/VoiceOS/core/database/src/commonMain/sqldelight/com/augmentalis/database/uuid/UUIDAlias.sq
Modules/VoiceOS/core/database/src/commonMain/sqldelight/com/augmentalis/database/uuid/UUIDAnalytics.sq
Modules/VoiceOS/core/database/src/commonMain/sqldelight/com/augmentalis/database/uuid/UUIDElement.sq
Modules/VoiceOS/core/database/src/commonMain/sqldelight/com/augmentalis/database/uuid/UUIDHierarchy.sq
Modules/VoiceOS/core/database/src/commonMain/kotlin/com/augmentalis/database/repositories/IUUIDRepository.kt
Modules/VoiceOS/core/database/src/commonMain/kotlin/com/augmentalis/database/repositories/impl/SQLDelightUUIDRepository.kt
Modules/VoiceOS/core/database/src/jvmTest/kotlin/com/augmentalis/database/repositories/UUIDRepositoryIntegrationTest.kt
```

## Success Criteria

1. No `uuid/` folder in SQLDelight
2. No `IUUIDRepository` or `SQLDelightUUIDRepository`
3. All imports use VUID* (with deprecated typealiases for UUID*)
4. Build succeeds
5. Tests pass
6. App runs without VUID validation errors

---

**Created:** 2026-01-02
**Author:** Claude Code Assistant
**Version:** 1.0
