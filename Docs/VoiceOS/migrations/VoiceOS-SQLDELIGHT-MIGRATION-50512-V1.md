# VoiceOS Room → SQLDelight Migration Plan (UPDATED)

**Repository:** VoiceOS
**Priority:** HIGH
**Estimated Duration:** 15-20 days
**Depends On:** None (independent of AVA migration)

---

## Executive Summary

VoiceOS has **10 Room databases** with **~57 entities** that need migration to SQLDelight for cross-platform support. There is an existing **disabled SQLDelight module** at `libraries/core/database/` that had schema mismatch errors.

### Migration Strategy

**Approach:** Consolidate 10 databases into a **unified SQLDelight KMP database** with logical groupings:

1. **Core Domain** - Voice data, settings, analytics
2. **App Learning** - App scraping, exploration, commands
3. **Plugin System** - Plugin metadata and permissions
4. **UUID System** - Universal identifiers

---

## Current State: 10 Room Databases

### 1. UUIDCreatorDatabase (LOW complexity)
**Location:** `modules/libraries/UUIDCreator/`
**Entities:** 4 | **Version:** 2

| Entity | Purpose |
|--------|---------|
| UUIDElementEntity | UUID element storage |
| UUIDHierarchyEntity | Parent-child relationships |
| UUIDAnalyticsEntity | Usage analytics |
| UUIDAliasEntity | UUID aliases |

### 2. VoiceOSAppDatabase (HIGH complexity)
**Location:** `modules/apps/VoiceOSCore/database/`
**Entities:** 12 | **Version:** 5 | **Migrations:** 5

| Entity | Purpose |
|--------|---------|
| AppEntity | Unified app metadata |
| ScreenEntity | Screen states |
| ExplorationSessionEntity | Exploration sessions |
| NavigationEdgeEntity | Navigation graph edges |
| ScrapedElementEntity | UI elements |
| ScrapedHierarchyEntity | Element hierarchy |
| GeneratedCommandEntity | Voice commands |
| ScreenContextEntity | Screen context |
| ScreenTransitionEntity | Navigation transitions |
| ElementRelationshipEntity | Element relationships |
| UserInteractionEntity | User interactions |
| ElementStateHistoryEntity | State changes |

### 3. LearnAppDatabase (DEPRECATED)
**Location:** `modules/apps/VoiceOSCore/learnapp/`
**Entities:** 4 | **Version:** 1
**Status:** ⚠️ DEPRECATED - Being consolidated into VoiceOSAppDatabase

| Entity | Purpose |
|--------|---------|
| LearnedAppEntity | Learned apps |
| ExplorationSessionEntity | Sessions |
| NavigationEdgeEntity | Navigation |
| ScreenStateEntity | Screen states |

### 4. AppScrapingDatabase (HIGH complexity)
**Location:** `modules/apps/VoiceOSCore/scraping/`
**Entities:** 10 | **Version:** 10 | **Migrations:** 10

**Note:** Most entities overlap with VoiceOSAppDatabase - consolidation opportunity.

| Entity | Purpose |
|--------|---------|
| AppEntity | App metadata |
| ScrapedAppEntity | Scraped app data |
| ScrapedElementEntity | UI elements |
| ScrapedHierarchyEntity | Hierarchy |
| GeneratedCommandEntity | Commands |
| ScreenContextEntity | Contexts |
| ElementRelationshipEntity | Relationships |
| ScreenTransitionEntity | Transitions |
| UserInteractionEntity | Interactions |
| ElementStateHistoryEntity | History |

### 5. WebScrapingDatabase (LOW complexity)
**Location:** `modules/apps/VoiceOSCore/learnweb/`
**Entities:** 3 | **Version:** 1

| Entity | Purpose |
|--------|---------|
| ScrapedWebsite | Website metadata |
| ScrapedWebElement | DOM elements |
| GeneratedWebCommand | Web commands |

### 6. LocalizationDatabase (LOW complexity)
**Location:** `modules/managers/LocalizationManager/`
**Entities:** 1 | **Version:** 1

| Entity | Purpose |
|--------|---------|
| UserPreference | User language preferences |

### 7. VoiceOSDatabase (MEDIUM complexity)
**Location:** `modules/managers/VoiceDataManager/`
**Entities:** 14 | **Version:** 1

| Entity | Purpose |
|--------|---------|
| AnalyticsSettings | Analytics config |
| CommandHistoryEntry | Command history |
| CustomCommand | Custom commands |
| DeviceProfile | Device info |
| ErrorReport | Error reports |
| GestureLearningData | Gesture learning |
| LanguageModel | Language models |
| RecognitionLearning | Recognition data |
| RetentionSettings | Retention config |
| ScrappedCommand | Scraped commands |
| TouchGesture | Touch gestures |
| UsageStatistic | Usage stats |
| UserPreference | User prefs |
| UserSequence | User sequences |

### 8. CommandDatabase (LOW complexity)
**Location:** `modules/managers/CommandManager/database/`
**Entities:** 3 | **Version:** 3

| Entity | Purpose |
|--------|---------|
| VoiceCommandEntity | Voice commands |
| DatabaseVersionEntity | Version tracking |
| CommandUsageEntity | Usage analytics |

### 9. LearningDatabase (LOW complexity)
**Location:** `modules/managers/CommandManager/context/`
**Entities:** 2 | **Version:** 1

| Entity | Purpose |
|--------|---------|
| CommandUsageEntity | Command usage |
| ContextPreferenceEntity | Context preferences |

### 10. PluginDatabase (LOW complexity)
**Location:** `modules/libraries/PluginSystem/`
**Entities:** 4 | **Version:** 1

| Entity | Purpose |
|--------|---------|
| PluginEntity | Plugin metadata |
| DependencyEntity | Dependencies |
| PermissionEntity | Permissions |
| CheckpointEntity | System checkpoints |

---

## Existing SQLDelight Module (DISABLED)

**Location:** `libraries/core/database/`
**Status:** ❌ DISABLED - Schema mismatch errors

```kotlin
// settings.gradle.kts
// include(":libraries:core:database") // DISABLED: Schema mismatch errors
```

**Configuration:**
- SQLDelight 2.0.1
- Android, iOS, JVM targets
- Database: `VoiceOSDatabase`

---

## Migration Phases

### Phase 1: Fix & Enable SQLDelight Module (2-3 days)

**Tasks:**
1. Re-enable `libraries/core/database` module
2. Fix schema mismatch errors
3. Create consolidated schema design
4. Test basic CRUD operations

### Phase 2: Migrate UUIDCreator (2-3 days)

**Current:** `modules/libraries/UUIDCreator/` (4 entities)

**Tasks:**
1. Create SQLDelight schema files:
   - `UUIDElement.sq`
   - `UUIDHierarchy.sq`
   - `UUIDAnalytics.sq`
   - `UUIDAliases.sq`
2. Update UUIDCreator to use shared database
3. Add data migration utility
4. Remove Room dependencies
5. Test UUID operations

### Phase 3: Migrate VoiceOSCore Databases (5-7 days)

**Current:** 3 databases (VoiceOSAppDatabase, AppScrapingDatabase, LearnAppDatabase)

**Strategy:** Consolidate into single "AppLearning" schema group

**Tasks:**
1. Create consolidated schema:
   - `App.sq` (unified app metadata)
   - `Screen.sq` (screen states)
   - `Element.sq` (scraped elements)
   - `Command.sq` (generated commands)
   - `Navigation.sq` (transitions, edges)
   - `Interaction.sq` (user interactions)
   - `WebScraping.sq` (web elements)
2. Create data migration from 3 Room databases
3. Update VoiceOSCore to use SQLDelight
4. Update scraping module
5. Remove deprecated LearnAppDatabase
6. Test all app learning flows

### Phase 4: Migrate VoiceDataManager (3-4 days)

**Current:** `modules/managers/VoiceDataManager/` (14 entities)

**Tasks:**
1. Create SQLDelight schemas:
   - `Settings.sq` (analytics, retention)
   - `CommandHistory.sq` (history, custom commands)
   - `DeviceProfile.sq` (device, errors)
   - `Learning.sq` (gesture, recognition)
   - `Usage.sq` (statistics, sequences)
2. Update VoiceDataManager
3. Add data migration
4. Remove Room dependencies

### Phase 5: Migrate CommandManager (2-3 days)

**Current:** 2 databases (CommandDatabase, LearningDatabase)

**Tasks:**
1. Create SQLDelight schemas:
   - `VoiceCommand.sq`
   - `CommandUsage.sq`
   - `ContextPreference.sq`
2. Consolidate into single schema
3. Update CommandManager
4. Remove Room dependencies

### Phase 6: Migrate Remaining Modules (2-3 days)

**Modules:**
- LocalizationManager (1 entity)
- PluginSystem (4 entities)

**Tasks:**
1. Create SQLDelight schemas
2. Update modules
3. Final Room cleanup

---

## Consolidated SQLDelight Schema Design

```
libraries/core/database/src/commonMain/sqldelight/
└── com/augmentalis/database/
    ├── uuid/
    │   ├── UUIDElement.sq
    │   ├── UUIDHierarchy.sq
    │   ├── UUIDAnalytics.sq
    │   └── UUIDAlias.sq
    ├── app/
    │   ├── App.sq
    │   ├── Screen.sq
    │   ├── Element.sq
    │   ├── Command.sq
    │   ├── Navigation.sq
    │   └── Interaction.sq
    ├── web/
    │   ├── Website.sq
    │   ├── WebElement.sq
    │   └── WebCommand.sq
    ├── voice/
    │   ├── VoiceCommand.sq
    │   ├── CommandHistory.sq
    │   ├── CommandUsage.sq
    │   └── ContextPreference.sq
    ├── settings/
    │   ├── AnalyticsSettings.sq
    │   ├── RetentionSettings.sq
    │   ├── UserPreference.sq
    │   └── DeviceProfile.sq
    ├── learning/
    │   ├── GestureLearning.sq
    │   ├── RecognitionLearning.sq
    │   ├── UsageStatistic.sq
    │   └── UserSequence.sq
    ├── plugin/
    │   ├── Plugin.sq
    │   ├── Dependency.sq
    │   ├── Permission.sq
    │   └── Checkpoint.sq
    └── error/
        └── ErrorReport.sq
```

---

## Build Configuration

```kotlin
// libraries/core/database/build.gradle.kts
sqldelight {
    databases {
        create("VoiceOSDatabase") {
            packageName.set("com.augmentalis.database")
            dialect("app.cash.sqldelight:sqlite-3-38-dialect:2.0.1")
            deriveSchemaFromMigrations.set(true)
            verifyMigrations.set(true)
        }
    }
}
```

---

## Timeline

| Phase | Duration | Dependencies |
|-------|----------|--------------|
| Phase 1: Fix SQLDelight Module | 2-3 days | None |
| Phase 2: UUIDCreator | 2-3 days | Phase 1 |
| Phase 3: VoiceOSCore | 5-7 days | Phase 1, 2 |
| Phase 4: VoiceDataManager | 3-4 days | Phase 1 |
| Phase 5: CommandManager | 2-3 days | Phase 1 |
| Phase 6: Remaining | 2-3 days | Phase 1-5 |
| **Total** | **15-20 days** | |

---

## Verification Checklist

### Per-Phase Verification
- [ ] All entities migrated
- [ ] Data migration works
- [ ] Room dependencies removed
- [ ] Unit tests pass
- [ ] Integration tests pass

### Final Verification
- [ ] No Room dependencies in any module
- [ ] All databases consolidated
- [ ] iOS build works
- [ ] Desktop build works
- [ ] Android app runs correctly
- [ ] All features functional

---

## Rollback Strategy

1. Keep Room code in separate branch
2. Database backup before migration
3. Feature flags for gradual rollout
4. Revert commits if critical issues

---

## Git Commits (Per Phase)

```bash
# Phase 1
git commit -m "fix(db): re-enable SQLDelight database module

- Fix schema mismatch errors
- Create consolidated schema design
- Verify KMP targets (Android, iOS, JVM)

Part of: VoiceOS Room→SQLDelight Migration Phase 1"

# Phase 2
git commit -m "refactor(db): migrate UUIDCreator to SQLDelight

- Create UUID schema files
- Update UUIDCreator module
- Add data migration utility
- Remove Room dependencies

Part of: VoiceOS Room→SQLDelight Migration Phase 2"

# Phase 3
git commit -m "refactor(db): migrate VoiceOSCore to SQLDelight

- Consolidate 3 databases into unified schema
- Create app learning schema files
- Update scraping and exploration modules
- Remove deprecated LearnAppDatabase
- Remove Room dependencies

BREAKING CHANGE: Database format changed
Part of: VoiceOS Room→SQLDelight Migration Phase 3"

# Phase 4
git commit -m "refactor(db): migrate VoiceDataManager to SQLDelight

- Create voice data schema files
- Update VoiceDataManager module
- Add data migration
- Remove Room dependencies

Part of: VoiceOS Room→SQLDelight Migration Phase 4"

# Phase 5
git commit -m "refactor(db): migrate CommandManager to SQLDelight

- Consolidate CommandDatabase and LearningDatabase
- Create command schema files
- Update CommandManager module
- Remove Room dependencies

Part of: VoiceOS Room→SQLDelight Migration Phase 5"

# Phase 6
git commit -m "refactor(db): complete SQLDelight migration

- Migrate LocalizationManager
- Migrate PluginSystem
- Remove all Room dependencies
- Final cleanup

Part of: VoiceOS Room→SQLDelight Migration Phase 6 (COMPLETE)"
```

---

**Parent Plan:** [ROOM-TO-SQLDELIGHT-MIGRATION-PLAN.md](/Volumes/M-Drive/Coding/AVA/docs/migrations/ROOM-TO-SQLDELIGHT-MIGRATION-PLAN.md)

---

## Appendix: Entity Mapping Reference

### UUIDCreator → SQLDelight
| Room Entity | SQLDelight Table |
|-------------|------------------|
| UUIDElementEntity | uuid_elements |
| UUIDHierarchyEntity | uuid_hierarchy |
| UUIDAnalyticsEntity | uuid_analytics |
| UUIDAliasEntity | uuid_aliases |

### VoiceOSCore → SQLDelight (Consolidated)
| Room Entity | SQLDelight Table |
|-------------|------------------|
| AppEntity | apps |
| ScreenEntity | screens |
| ScrapedElementEntity | elements |
| GeneratedCommandEntity | commands_generated | (RENAMED 2025-12-05)
| NavigationEdgeEntity | navigation_edges |
| ScreenTransitionEntity | screen_transitions |
| UserInteractionEntity | user_interactions |
| ElementStateHistoryEntity | element_state_history |
| ScrapedWebsite | websites |
| ScrapedWebElement | web_elements |
| GeneratedWebCommand | web_commands |

### VoiceDataManager → SQLDelight
| Room Entity | SQLDelight Table |
|-------------|------------------|
| AnalyticsSettings | analytics_settings |
| CommandHistoryEntry | command_history |
| CustomCommand | custom_commands |
| DeviceProfile | device_profiles |
| ErrorReport | error_reports |
| GestureLearningData | gesture_learning |
| LanguageModel | language_models |
| RecognitionLearning | recognition_learning |
| RetentionSettings | retention_settings |
| ScrappedCommand | commands_scraped | (RENAMED 2025-12-05)
| TouchGesture | touch_gestures |
| UsageStatistic | usage_statistics |
| UserPreference | user_preferences |
| UserSequence | user_sequences |

### CommandManager → SQLDelight
| Room Entity | SQLDelight Table |
|-------------|------------------|
| VoiceCommandEntity | commands_static | (RENAMED 2025-12-05)
| DatabaseVersionEntity | database_versions |
| CommandUsageEntity | command_usage |
| ContextPreferenceEntity | context_preferences |

### PluginSystem → SQLDelight
| Room Entity | SQLDelight Table |
|-------------|------------------|
| PluginEntity | plugins |
| DependencyEntity | plugin_dependencies |
| PermissionEntity | plugin_permissions |
| CheckpointEntity | system_checkpoints |

### LocalizationManager → SQLDelight
| Room Entity | SQLDelight Table |
|-------------|------------------|
| UserPreference | localization_preferences |
