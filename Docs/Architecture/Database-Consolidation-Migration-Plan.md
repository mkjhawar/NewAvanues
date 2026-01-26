# Database Consolidation Migration Plan

**Date:** 2026-01-26
**Author:** Claude Code Session
**Status:** Planning
**Priority:** High (Architectural)

---

## Executive Summary

Consolidate two parallel database modules into a unified architecture:
- **From:** `Modules/Database` (7 tables) + `Modules/VoiceOS/core/database` (49 tables)
- **To:** `Modules/Database` (unified master) + app-specific satellites

---

## Current State

### Modules/Database (AvanuesDatabase)
```
Location: /Modules/Database/
Tables: 7 files, 3 domains
Namespace: com.augmentalis.database
Used by: WebAvanue
```

| Domain | Tables |
|--------|--------|
| avid/ | AvidAlias, AvidAnalytics, AvidElement, AvidHierarchy |
| browser/ | BrowserTables (12 tables in 1 file) |
| web/ | ScrapedWebCommand, WebAppWhitelist |

### Modules/VoiceOS/core/database (VoiceOSDatabase)
```
Location: /Modules/VoiceOS/core/database/
Tables: 49 files, 10+ domains
Namespace: com.augmentalis.database (CONFLICT!)
Used by: VoiceOSCore, voiceoscoreng, VoiceOS managers
```

| Domain | Tables |
|--------|--------|
| avid/ | AvidAlias, AvidAnalytics, AvidElement, AvidHierarchy (DUPLICATE) |
| command/ | VoiceCommand, CommandUsage, CommandHistory, ContextPreference, DatabaseVersion |
| scraping/ | ScrappedCommand |
| web/ | GeneratedWebCommand, ScrapedWebElement, ScrapedWebsite |
| element/ | ScrapedElement, ElementRelationship, ScrapedHierarchy |
| app/ | AppVersion, CustomCommand, ErrorReport |
| navigation/ | NavigationEdge, ScreenTransition |
| plugin/ | Plugin, PluginDependency, PluginPermission, SystemCheckpoint |
| settings/ | UserPreference |
| stats/ | UsageStatistic, UserInteraction |
| root | 16 additional tables |

---

## Target State

### Modules/Database (Unified Master)
```
Location: /Modules/Database/
Namespace: com.augmentalis.database
Database: AvanuesDatabase
```

```
src/commonMain/sqldelight/com/augmentalis/database/
├── avid/                    # AVID system (keep existing, enhanced)
│   ├── AvidAlias.sq
│   ├── AvidAnalytics.sq
│   ├── AvidElement.sq
│   └── AvidHierarchy.sq
│
├── command/                 # All command types (from VoiceOS)
│   ├── VoiceCommand.sq      # Static commands
│   ├── GeneratedCommand.sq  # UI-derived commands
│   ├── ScrappedCommand.sq   # Third-party app commands
│   ├── CommandUsage.sq      # Analytics
│   ├── CommandHistory.sq    # History
│   ├── ContextPreference.sq # Learning
│   ├── CustomCommand.sq     # User-defined
│   └── ElementCommand.sq    # Element mappings
│
├── learning/                # App learning system (from VoiceOS)
│   ├── LearnedApp.sq
│   ├── ScrapedApp.sq
│   ├── ScrapedElement.sq
│   ├── ScrapedHierarchy.sq
│   ├── ElementRelationship.sq
│   ├── ElementStateHistory.sq
│   ├── ExplorationSession.sq
│   ├── ScreenContext.sq
│   ├── ScreenState.sq
│   ├── GestureLearning.sq
│   └── RecognitionLearning.sq
│
├── web/                     # Web system (merged)
│   ├── GeneratedWebCommand.sq
│   ├── ScrapedWebElement.sq
│   ├── ScrapedWebsite.sq
│   ├── ScrapedWebCommand.sq  # Keep from original
│   └── WebAppWhitelist.sq    # Keep from original
│
├── navigation/              # Navigation (from VoiceOS)
│   ├── NavigationEdge.sq
│   └── ScreenTransition.sq
│
├── plugin/                  # Plugin system (from VoiceOS)
│   ├── Plugin.sq
│   ├── PluginDependency.sq
│   ├── PluginPermission.sq
│   └── SystemCheckpoint.sq
│
├── app/                     # App metadata (from VoiceOS)
│   ├── AppVersion.sq
│   ├── AppCategoryOverride.sq  # NEW
│   ├── AppPatternGroup.sq      # NEW
│   ├── AppConsentHistory.sq
│   ├── DeviceProfile.sq
│   └── ErrorReport.sq
│
├── settings/                # Settings (from VoiceOS)
│   ├── Settings.sq
│   ├── UserPreference.sq
│   └── LanguageModel.sq
│
├── stats/                   # Statistics (from VoiceOS)
│   ├── UsageStatistic.sq
│   └── UserInteraction.sq
│
├── contracts/               # Shared interfaces (NEW)
│   └── (Kotlin interfaces, not .sq files)
│
└── DatabaseVersion.sq       # Schema versioning

src/commonMain/kotlin/com/augmentalis/database/
├── repositories/
│   ├── IAppCategoryRepository.kt
│   ├── IAppPatternGroupRepository.kt
│   ├── ICommandRepository.kt
│   ├── IScrapedAppRepository.kt
│   ├── IScrapedElementRepository.kt
│   └── ... (all interfaces)
│
├── repositories/impl/
│   ├── SQLDelightAppCategoryRepository.kt
│   ├── SQLDelightAppPatternGroupRepository.kt
│   └── ... (all implementations)
│
├── dto/                     # Data transfer objects
│   └── ... (existing DTOs)
│
└── DatabaseDriverFactory.kt # Platform-specific drivers
```

### Modules/WebAvanue/database (Browser-Specific Satellite)
```
Location: /Modules/WebAvanue/database/
Namespace: com.augmentalis.webavanue.database
Database: WebAvanueDatabase
```

```
src/commonMain/sqldelight/com/augmentalis/webavanue/database/
└── browser/
    └── BrowserTables.sq     # Tabs, history, bookmarks, downloads
```

**Rationale:** Browser data (tabs, history, bookmarks) is purely WebAvanue-specific and doesn't need to be in the master database.

### Modules/VoiceOS/database (REMOVED)
The `/Modules/VoiceOS/core/database/` folder will be **deleted** after migration.
- All schemas moved to `Modules/Database`
- `/core/` folder removed (flatten structure)
- VoiceOS modules will depend on `Modules/Database` directly

---

## Migration Phases

### Phase 1: Preparation (No Breaking Changes)
**Duration:** 1 session
**Risk:** Low

1. [ ] Create backup of both database modules
2. [ ] Document all current consumers and their imports
3. [ ] Create database schema comparison report
4. [ ] Identify and resolve AVID schema differences
5. [ ] Plan namespace changes

**Deliverables:**
- Schema comparison document
- Consumer dependency map
- Namespace migration guide

### Phase 2: Namespace Fix
**Duration:** 1 session
**Risk:** Medium

1. [ ] Update `Modules/Database` namespace to `com.augmentalis.database` (keep)
2. [ ] Update `VoiceOS/core/database` namespace to `com.augmentalis.database.voiceos` (temporary)
3. [ ] Update all imports in consuming modules
4. [ ] Verify build succeeds
5. [ ] Test on device

**Breaking Changes:**
- Import statements change
- Generated code paths change

### Phase 3: Schema Migration
**Duration:** 2-3 sessions
**Risk:** High

#### Phase 3a: Move Command System
1. [ ] Copy command/*.sq files to Modules/Database/command/
2. [ ] Update foreign key references
3. [ ] Update VoiceOSCore to use new location
4. [ ] Delete old files from VoiceOS/core/database
5. [ ] Verify build and tests

#### Phase 3b: Move Learning System
1. [ ] Copy learning-related .sq files to Modules/Database/learning/
2. [ ] Update foreign key references
3. [ ] Update VoiceOSCore to use new location
4. [ ] Delete old files
5. [ ] Verify build and tests

#### Phase 3c: Move Web System
1. [ ] Merge web schemas (resolve GeneratedWebCommand vs ScrapedWebCommand)
2. [ ] Move to Modules/Database/web/
3. [ ] Update consumers
4. [ ] Delete old files
5. [ ] Verify

#### Phase 3d: Move Remaining Tables
1. [ ] Move navigation/, plugin/, app/, settings/, stats/
2. [ ] Move root-level tables
3. [ ] Update all consumers
4. [ ] Delete VoiceOS/core/database entirely

### Phase 4: Repository Consolidation
**Duration:** 1 session
**Risk:** Medium

1. [ ] Move all repository interfaces to Modules/Database/repositories/
2. [ ] Move all implementations to Modules/Database/repositories/impl/
3. [ ] Update VoiceOSDatabaseManager → AvanuesDatabaseManager
4. [ ] Update all consumers to use unified manager
5. [ ] Delete redundant code

### Phase 5: Satellite Extraction
**Duration:** 1 session
**Risk:** Low

1. [ ] Create Modules/WebAvanue/database/ module
2. [ ] Move BrowserTables.sq to satellite
3. [ ] Update WebAvanue to use satellite
4. [ ] Verify browser functionality

### Phase 6: Cleanup & Documentation
**Duration:** 1 session
**Risk:** Low

1. [ ] Delete /Modules/VoiceOS/core/ folder entirely
2. [ ] Flatten VoiceOS structure: /Modules/VoiceOS/managers/, /Modules/VoiceOS/...
3. [ ] Update all documentation
4. [ ] Update CLAUDE.md with new structure
5. [ ] Create architecture diagram

---

## Risk Mitigation

### Data Migration (Existing Installs)
```kotlin
// In app startup, check and migrate
class DatabaseMigrator {
    fun migrateIfNeeded(context: Context) {
        val oldDb = context.getDatabasePath("voiceos.db")
        val newDb = context.getDatabasePath("avanues.db")

        if (oldDb.exists() && !newDb.exists()) {
            // Copy data from old to new schema
            migrateData(oldDb, newDb)
        }
    }
}
```

### Rollback Plan
- Keep VoiceOS/core/database as deprecated but functional for 1 release
- Add feature flag to switch between old/new database
- Monitor crash reports for database-related issues

### Testing Strategy
1. Unit tests for all repository implementations
2. Integration tests for cross-table queries
3. Migration tests with sample data
4. Manual testing on RealWear HMT-1

---

## Dependency Changes

### Before
```
voiceoscoreng
├── VoiceOSCore
│   └── VoiceOS/core/database
└── (no direct Database dependency)

WebAvanue
└── Modules/Database
```

### After
```
voiceoscoreng
├── VoiceOSCore
│   └── Modules/Database (unified)
└── Modules/Database (unified)

WebAvanue
├── Modules/Database (unified)
└── Modules/WebAvanue/database (browser satellite)
```

---

## Files to Delete (After Migration)

```
DELETE: Modules/VoiceOS/core/database/  (entire folder - 49 .sq files)
DELETE: Modules/VoiceOS/core/           (remove /core/ level)
DELETE: Modules/AvaMagic/Core/database/src/commonMain/sqldelight/com/augmentalis/database/AppCategoryOverride.sq
DELETE: Modules/AvaMagic/Core/database/src/commonMain/sqldelight/com/augmentalis/database/AppPatternGroup.sq
```

---

## Files to Create

```
CREATE: Modules/Database/src/commonMain/sqldelight/com/augmentalis/database/command/*.sq
CREATE: Modules/Database/src/commonMain/sqldelight/com/augmentalis/database/learning/*.sq
CREATE: Modules/Database/src/commonMain/sqldelight/com/augmentalis/database/navigation/*.sq
CREATE: Modules/Database/src/commonMain/sqldelight/com/augmentalis/database/plugin/*.sq
CREATE: Modules/Database/src/commonMain/sqldelight/com/augmentalis/database/app/*.sq
CREATE: Modules/Database/src/commonMain/sqldelight/com/augmentalis/database/settings/*.sq
CREATE: Modules/Database/src/commonMain/sqldelight/com/augmentalis/database/stats/*.sq
CREATE: Modules/Database/src/commonMain/kotlin/com/augmentalis/database/repositories/*.kt
CREATE: Modules/Database/src/commonMain/kotlin/com/augmentalis/database/repositories/impl/*.kt
CREATE: Modules/WebAvanue/database/ (new module)
```

---

## Success Criteria

1. [ ] Single master database module (`Modules/Database`)
2. [ ] No duplicate schemas
3. [ ] All apps build successfully
4. [ ] All tests pass
5. [ ] No `/core/` folders (flat structure)
6. [ ] Clear namespace separation
7. [ ] Shared interfaces accessible to all modules
8. [ ] Browser data in separate satellite module
9. [ ] Documentation updated

---

## Timeline Estimate

| Phase | Sessions | Risk |
|-------|----------|------|
| Phase 1: Preparation | 1 | Low |
| Phase 2: Namespace Fix | 1 | Medium |
| Phase 3: Schema Migration | 3 | High |
| Phase 4: Repository Consolidation | 1 | Medium |
| Phase 5: Satellite Extraction | 1 | Low |
| Phase 6: Cleanup | 1 | Low |
| **Total** | **8 sessions** | |

---

## Next Steps

1. Review and approve this plan
2. Start Phase 1 (Preparation)
3. Execute phases sequentially with verification at each step

---

**Approval:**
- [ ] Architecture approved
- [ ] Timeline approved
- [ ] Risk mitigation approved

