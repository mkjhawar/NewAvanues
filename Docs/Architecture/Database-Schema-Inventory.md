# Database Schema Inventory

**Document Version:** 1.0
**Created:** 2026-01-26
**Purpose:** Phase 1 Preparation for Database Consolidation

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [Module Locations](#module-locations)
3. [Schema Inventory: Modules/Database](#schema-inventory-modulesdatabase)
4. [Schema Inventory: Modules/VoiceOS/core/database](#schema-inventory-modulesvoiceoscoredatabase)
5. [Schema Comparison Analysis](#schema-comparison-analysis)
6. [Consumer Analysis](#consumer-analysis)
7. [Foreign Key Relationships](#foreign-key-relationships)
8. [Consolidation Recommendations](#consolidation-recommendations)

---

## Executive Summary

This document provides a comprehensive inventory of two database module structures in the NewAvanues codebase:

| Metric | Modules/Database | VoiceOS/core/database |
|--------|------------------|----------------------|
| Total .sq files | 7 | 44 |
| Tables defined | 14 | ~60 |
| Duplicate schemas | 4 (AVID tables) | 4 (AVID tables) |
| Unique tables | 3 | ~40 |

**Key Finding:** The Modules/Database module contains a subset of schemas focused on AVID (Avanues Voice ID), browser, and web scraping. The VoiceOS/core/database is the comprehensive voice OS database with all learning, command, and UI element schemas.

---

## Module Locations

### Primary Database Module
```
/Volumes/M-Drive/Coding/NewAvanues/Modules/Database/
  src/commonMain/sqldelight/com/augmentalis/database/
    avid/
      AvidAlias.sq
      AvidAnalytics.sq
      AvidElement.sq
      AvidHierarchy.sq
    browser/
      BrowserTables.sq
    web/
      ScrapedWebCommand.sq
      WebAppWhitelist.sq
```

### VoiceOS Core Database Module
```
/Volumes/M-Drive/Coding/NewAvanues/Modules/VoiceOS/core/database/
  src/commonMain/sqldelight/com/augmentalis/database/
    (root level files)
    app/
    avid/
    command/
    element/
    navigation/
    plugin/
    scraping/
    settings/
    stats/
    web/
```

---

## Schema Inventory: Modules/Database

### 1. AVID Tables (avid/)

#### avid_alias
**File:** `AvidAlias.sq`
**Purpose:** AVID alias/synonym storage for voice command variations
**Table Name:** `avid_alias`

| Column | Type | Constraints |
|--------|------|-------------|
| id | INTEGER | PRIMARY KEY AUTOINCREMENT |
| avid | TEXT | NOT NULL |
| package_name | TEXT | NOT NULL |
| alias | TEXT | NOT NULL |
| canonical | TEXT | NOT NULL |
| alias_type | TEXT | NOT NULL DEFAULT 'voice' |
| usage_count | INTEGER | NOT NULL DEFAULT 0 |
| is_active | INTEGER | NOT NULL DEFAULT 1 |
| source | TEXT | NOT NULL DEFAULT 'system' |
| created_at | INTEGER | NOT NULL |
| last_used | INTEGER | |

**FK:** `avid` REFERENCES `avid_element(avid)` ON DELETE CASCADE

---

#### avid_analytics
**File:** `AvidAnalytics.sq`
**Purpose:** AVID usage analytics and statistics
**Table Name:** `avid_analytics`

| Column | Type | Constraints |
|--------|------|-------------|
| id | INTEGER | PRIMARY KEY AUTOINCREMENT |
| avid | TEXT | NOT NULL UNIQUE |
| package_name | TEXT | NOT NULL |
| total_uses | INTEGER | NOT NULL DEFAULT 0 |
| successful_uses | INTEGER | NOT NULL DEFAULT 0 |
| failed_uses | INTEGER | NOT NULL DEFAULT 0 |
| voice_activations | INTEGER | NOT NULL DEFAULT 0 |
| touch_activations | INTEGER | NOT NULL DEFAULT 0 |
| avg_response_ms | INTEGER | |
| last_success_at | INTEGER | |
| last_failure_at | INTEGER | |
| sessions_used | INTEGER | NOT NULL DEFAULT 0 |
| unique_days | INTEGER | NOT NULL DEFAULT 0 |
| most_common_context | TEXT | |
| most_common_action | TEXT | |
| first_used | INTEGER | NOT NULL |
| last_used | INTEGER | NOT NULL |
| updated_at | INTEGER | NOT NULL |

**FK:** `avid` REFERENCES `avid_element(avid)` ON DELETE CASCADE

---

#### avid_element
**File:** `AvidElement.sq`
**Purpose:** AVID (Avanues Voice ID) Element persistence - unified identifier for UI elements
**Table Name:** `avid_element`

| Column | Type | Constraints |
|--------|------|-------------|
| id | INTEGER | PRIMARY KEY AUTOINCREMENT |
| avid | TEXT | NOT NULL UNIQUE |
| platform | TEXT | NOT NULL |
| sequence | INTEGER | NOT NULL DEFAULT 0 |
| package_name | TEXT | NOT NULL |
| element_hash | TEXT | NOT NULL |
| element_type | TEXT | NOT NULL |
| resource_id | TEXT | |
| display_name | TEXT | |
| content_desc | TEXT | |
| bounds_left | REAL | |
| bounds_top | REAL | |
| bounds_right | REAL | |
| bounds_bottom | REAL | |
| app_version | TEXT | |
| os_version | TEXT | |
| screen_id | INTEGER | |
| parent_avid | TEXT | |
| is_active | INTEGER | NOT NULL DEFAULT 1 |
| confidence | REAL | NOT NULL DEFAULT 1.0 |
| created_at | INTEGER | NOT NULL |
| updated_at | INTEGER | NOT NULL |
| last_verified | INTEGER | |

**Unique:** `(package_name, element_hash)`

---

#### avid_hierarchy
**File:** `AvidHierarchy.sq`
**Purpose:** AVID hierarchy relationships (parent-child)
**Table Name:** `avid_hierarchy`

| Column | Type | Constraints |
|--------|------|-------------|
| id | INTEGER | PRIMARY KEY AUTOINCREMENT |
| parent_avid | TEXT | NOT NULL |
| child_avid | TEXT | NOT NULL |
| package_name | TEXT | NOT NULL |
| screen_id | INTEGER | |
| depth | INTEGER | NOT NULL DEFAULT 1 |
| child_index | INTEGER | NOT NULL DEFAULT 0 |
| created_at | INTEGER | NOT NULL |
| updated_at | INTEGER | NOT NULL |

**FK:** `parent_avid` REFERENCES `avid_element(avid)` ON DELETE CASCADE
**FK:** `child_avid` REFERENCES `avid_element(avid)` ON DELETE CASCADE

---

### 2. Browser Tables (browser/)

#### BrowserTables.sq
**File:** `BrowserTables.sq`
**Purpose:** Complete browser data model for WebAvanue

**Tables:**
- `tab_group` - Chrome-like tab grouping
- `tab` - Browser tabs with session data
- `favorite` - Bookmarks
- `favorite_folder` - Bookmark folders
- `favorite_tag` - Bookmark tags
- `history_entry` - Browser history
- `download` - Download management
- `browser_settings` - Single-row settings
- `site_permission` - Per-site permissions
- `session` - Crash recovery
- `session_tab` - Session tab state

---

### 3. Web Tables (web/)

#### scraped_web_command
**File:** `ScrapedWebCommand.sq`
**Purpose:** Web element voice commands for whitelisted domains
**Table Name:** `scraped_web_command`

| Column | Type | Constraints |
|--------|------|-------------|
| id | INTEGER | PRIMARY KEY AUTOINCREMENT |
| element_hash | TEXT | NOT NULL |
| domain_id | TEXT | NOT NULL |
| url_pattern | TEXT | |
| css_selector | TEXT | NOT NULL |
| xpath | TEXT | |
| command_text | TEXT | NOT NULL |
| element_text | TEXT | |
| element_tag | TEXT | NOT NULL |
| element_type | TEXT | NOT NULL |
| allowed_actions | TEXT | NOT NULL DEFAULT '["click"]' |
| primary_action | TEXT | NOT NULL DEFAULT 'click' |
| confidence | REAL | NOT NULL DEFAULT 0.5 |
| is_user_approved | INTEGER | NOT NULL DEFAULT 0 |
| user_approved_at | INTEGER | |
| synonyms | TEXT | |
| usage_count | INTEGER | NOT NULL DEFAULT 0 |
| last_used | INTEGER | |
| created_at | INTEGER | NOT NULL |
| last_verified | INTEGER | |
| is_deprecated | INTEGER | NOT NULL DEFAULT 0 |
| bound_left | INTEGER | |
| bound_top | INTEGER | |
| bound_width | INTEGER | |
| bound_height | INTEGER | |

**Unique:** `(element_hash, domain_id, primary_action)`

---

#### web_app_whitelist
**File:** `WebAppWhitelist.sq`
**Purpose:** User-designated web apps for persistent command storage
**Table Name:** `web_app_whitelist`

| Column | Type | Constraints |
|--------|------|-------------|
| id | INTEGER | PRIMARY KEY AUTOINCREMENT |
| domain_id | TEXT | NOT NULL UNIQUE |
| display_name | TEXT | NOT NULL |
| base_url | TEXT | |
| category | TEXT | |
| is_enabled | INTEGER | NOT NULL DEFAULT 1 |
| auto_scan | INTEGER | NOT NULL DEFAULT 1 |
| save_commands | INTEGER | NOT NULL DEFAULT 1 |
| command_count | INTEGER | NOT NULL DEFAULT 0 |
| last_visited | INTEGER | |
| visit_count | INTEGER | NOT NULL DEFAULT 0 |
| created_at | INTEGER | NOT NULL |
| updated_at | INTEGER | NOT NULL |

---

## Schema Inventory: Modules/VoiceOS/core/database

### Root Level Tables

| File | Table Name | Purpose |
|------|------------|---------|
| AppConsentHistory.sq | app_consent_history | User consent decisions tracking |
| AppCategoryOverride.sq | app_category_override | App category classifications |
| AppPatternGroup.sq | app_pattern_group | Pattern groups for category matching |
| CommandHistory.sq | command_history_entry | Voice command execution history |
| DeviceProfile.sq | device_profile | Device-specific configuration |
| ElementCommand.sq | element_command, element_quality_metric | User-assigned element commands |
| ElementStateHistory.sq | element_state_history | Element state change tracking |
| ExplorationSession.sq | exploration_sessions | App exploration sessions |
| GeneratedCommand.sq | commands_generated | Auto-generated voice commands |
| GestureLearning.sq | gesture_learning | Gesture recognition learning |
| LanguageModel.sq | language_model | Downloaded language model tracking |
| LearnedApp.sq | learned_apps | Apps with learned commands |
| RecognitionLearning.sq | recognition_learning | Speech recognition learning data |
| ScrapedApp.sq | scraped_app | App metadata for UI scraping |
| ScreenContext.sq | screen_context | Screen metadata and context |
| ScreenState.sq | screen_states | Screen state snapshots |
| Settings.sq | analytics_settings, retention_settings | System settings |
| TouchGesture.sq | touch_gesture | Touch gesture definitions |
| UserSequence.sq | user_sequence | User-defined command sequences |

### Subdirectory Tables

#### app/
| File | Table Name | Purpose |
|------|------------|---------|
| AppVersion.sq | app_version | App version tracking |
| CustomCommand.sq | custom_command | User-defined custom commands |
| ErrorReport.sq | error_report | Error tracking with privacy |

#### avid/
| File | Table Name | Purpose |
|------|------------|---------|
| AvidAlias.sq | avid_aliases | AVID aliases (DIFFERENT SCHEMA) |
| AvidAnalytics.sq | avid_analytics | AVID analytics (DIFFERENT SCHEMA) |
| AvidElement.sq | avid_elements | AVID elements (DIFFERENT SCHEMA) |
| AvidHierarchy.sq | avid_hierarchy | AVID hierarchy (SIMILAR SCHEMA) |

#### command/
| File | Table Name | Purpose |
|------|------------|---------|
| CommandUsage.sq | command_usage | Command usage tracking |
| ContextPreference.sq | context_preference | Context preferences |
| DatabaseVersion.sq | database_version | Database version tracking |
| VoiceCommand.sq | commands_static | Static voice commands |

#### element/
| File | Table Name | Purpose |
|------|------------|---------|
| ElementRelationship.sq | element_relationship | Semantic element relationships |
| ScrapedElement.sq | scraped_element | UI elements scraped from apps |
| ScrapedHierarchy.sq | scraped_hierarchy | Parent-child UI element relationships |

#### navigation/
| File | Table Name | Purpose |
|------|------------|---------|
| NavigationEdge.sq | navigation_edges | Navigation graph edges |
| ScreenTransition.sq | screen_transition | Screen navigation patterns |

#### plugin/
| File | Table Name | Purpose |
|------|------------|---------|
| Plugin.sq | plugins | Plugin metadata |
| PluginDependency.sq | plugin_dependencies | Plugin dependencies |
| PluginPermission.sq | plugin_permissions | Plugin permissions |
| SystemCheckpoint.sq | system_checkpoints | System state checkpoints |

#### scraping/
| File | Table Name | Purpose |
|------|------------|---------|
| ScrappedCommand.sq | commands_scraped | Commands scraped from apps |

#### settings/
| File | Table Name | Purpose |
|------|------------|---------|
| UserPreference.sq | user_preference | User preferences key-value store |

#### stats/
| File | Table Name | Purpose |
|------|------------|---------|
| UsageStatistic.sq | usage_statistic | General usage metrics |
| UserInteraction.sq | user_interaction | User interaction tracking |

#### web/
| File | Table Name | Purpose |
|------|------------|---------|
| GeneratedWebCommand.sq | generated_web_commands | Generated web commands |
| ScrapedWebElement.sq | scraped_web_elements | Scraped web page elements |
| ScrapedWebsite.sq | scraped_websites | Scraped website metadata |

---

## Schema Comparison Analysis

### DUPLICATE/OVERLAPPING Tables (Require Consolidation)

#### 1. AVID Alias Tables

| Aspect | Modules/Database | VoiceOS/core/database |
|--------|------------------|----------------------|
| Table Name | `avid_alias` | `avid_aliases` |
| Primary Key | id (autoincrement) | id (autoincrement) |
| AVID Reference | `avid TEXT NOT NULL` | `avid TEXT NOT NULL` |
| Package Name | Yes | No |
| Alias | Yes | Yes |
| Canonical | Yes | No |
| Alias Type | Yes (voice/accessibility/user/system) | No |
| Usage Count | Yes | No |
| Is Primary | No | Yes |
| Is Active | Yes | No |
| Source | Yes (system/user/learned/imported) | No |
| Created At | Yes | Yes |
| Last Used | Yes | No |
| FK Constraint | avid_element(avid) | avid_elements(avid) |

**Recommendation:** Modules/Database version is more feature-rich. Consolidate to the richer schema.

---

#### 2. AVID Analytics Tables

| Aspect | Modules/Database | VoiceOS/core/database |
|--------|------------------|----------------------|
| Table Name | `avid_analytics` | `avid_analytics` |
| Primary Key | id | avid (TEXT PK) |
| Package Name | Yes | No |
| Total/Access Count | total_uses | access_count |
| Success/Failure | separate fields | separate fields |
| Voice/Touch Activations | Yes | No |
| Avg Response MS | Yes | No |
| Session Metrics | Yes | No |
| Lifecycle State | No | Yes |
| First/Last Timestamps | Yes | Yes |
| FK Constraint | avid_element(avid) | avid_elements(avid) |

**Recommendation:** Merge schemas - Modules/Database has more analytics metrics, VoiceOS has lifecycle state.

---

#### 3. AVID Element Tables

| Aspect | Modules/Database | VoiceOS/core/database |
|--------|------------------|----------------------|
| Table Name | `avid_element` | `avid_elements` |
| Primary Key | id + UNIQUE(avid) | avid (TEXT PK) |
| Platform/Sequence | Yes | No |
| Element Hash | Yes | No |
| Element Type | Yes (codes: BTN, INP...) | Yes (generic) |
| Bounds (4 fields) | Yes (REAL, relative) | No (position_json) |
| App/OS Version | Yes | No |
| Screen ID | Yes | No |
| Parent AVID | Yes | Yes |
| Is Active/Enabled | is_active | is_enabled |
| Confidence/Priority | confidence | priority |
| Metadata | Structured | metadata_json, position_json |

**Recommendation:** Modules/Database version is significantly more comprehensive. Use it as the consolidated schema.

---

#### 4. AVID Hierarchy Tables

| Aspect | Modules/Database | VoiceOS/core/database |
|--------|------------------|----------------------|
| Table Name | `avid_hierarchy` | `avid_hierarchy` |
| Depth | Yes (1 = direct child) | Yes (0-based) |
| Child Index | Yes | order_index |
| Path | No | Yes |
| Package Name | Yes | No |
| Screen ID | Yes | No |

**Recommendation:** Merge - Modules/Database has package/screen context, VoiceOS has path support.

---

### UNIQUE to Modules/Database

1. **Browser Tables** (`BrowserTables.sq`)
   - tab_group, tab, favorite, favorite_folder, favorite_tag
   - history_entry, download, browser_settings
   - site_permission, session, session_tab

2. **Web Command Tables** (`ScrapedWebCommand.sq`, `WebAppWhitelist.sq`)
   - scraped_web_command
   - web_app_whitelist

---

### UNIQUE to VoiceOS/core/database

| Category | Tables |
|----------|--------|
| **App Learning** | learned_apps, exploration_sessions, app_consent_history, scraped_app |
| **Commands** | commands_static, commands_scraped, commands_generated, command_usage, context_preference |
| **Elements** | scraped_element, scraped_hierarchy, element_relationship, element_command, element_quality_metric |
| **Navigation** | navigation_edges, screen_transition, screen_states, screen_context |
| **Gestures** | gesture_learning, touch_gesture |
| **Recognition** | recognition_learning, language_model, command_history_entry |
| **Plugins** | plugins, plugin_dependencies, plugin_permissions, system_checkpoints |
| **Settings** | user_preference, analytics_settings, retention_settings |
| **Stats** | usage_statistic, user_interaction |
| **Web** | scraped_websites, scraped_web_elements, generated_web_commands |
| **App** | app_version, custom_command, error_report, device_profile, user_sequence |
| **Classification** | app_category_override, app_pattern_group |
| **Meta** | database_version, element_state_history |

---

## Consumer Analysis

### Active Consumers (Non-archived)

#### VoiceOS/core/database Consumers

| Module/Location | Files Using Database |
|-----------------|---------------------|
| **android/apps/voiceoscoreng/** | VoiceOSCoreNGApplication.kt, VoiceOSAccessibilityService.kt, SQLDelightAppCategoryOverrideRepository.kt, SQLDelightAppPatternGroupRepository.kt, AndroidCommandPersistence.kt, CommandPersistenceManager.kt, DynamicCommandGenerator.kt |
| **Modules/VoiceOS/managers/VoiceDataManager/** | DatabaseManager.kt, RecognitionLearningRepository.kt, VosDataManagerActivity.kt, VosDataViewModel.kt |
| **Modules/VoiceOS/managers/CommandManager/** | CommandDatabase.kt, DatabaseCommandResolver.kt, PreferenceLearner.kt, CommandUsageDaoAdapter.kt, DatabaseVersionDaoAdapter.kt, VoiceCommandDaoAdapter.kt |
| **Modules/VoiceOS/managers/LocalizationManager/** | PreferencesDaoAdapter.kt |
| **Modules/WebAvanue/** | BrowserVoiceOSCallback.kt |
| **Modules/PluginSystem/** | AndroidAccessibilityDataProvider.kt, RepositoryAdapter.kt, AccessibilityDataProviderFactory.kt |
| **Modules/AvidCreator/** | SQLDelightAvidRepositoryAdapter.kt, AvidAliasManager.kt, AvidCreatorDatabase.kt |

#### Modules/Database Consumers

| Module/Location | Files Using Database |
|-----------------|---------------------|
| **Modules/Database/** | IWebAppWhitelistRepository.kt, IScrapedWebCommandRepository.kt |
| **Common/VoiceOS/database/** | SQLDelightScrapedWebCommandRepository.kt, SQLDelightWebAppWhitelistRepository.kt |
| **Common/Database/** | SQLDelightGeneratedCommandRepository.kt, SQLDelightScreenContextRepository.kt, SQLDelightAppVersionRepository.kt |

#### AvanuesDatabase Consumers (BrowserAvanue)

| Location | Files |
|----------|-------|
| **android/apps/browseravanue/** | BrowserFavoriteDao.kt, BrowserSettingsDao.kt, BrowserTabDao.kt |

### Archived/Deprecated Consumers (for reference)

- `archive/deprecated/VoiceOS-LegacyApp-260121/`
- `archive/deprecated/VoiceOS-LearnAppCore-260118/`
- `archive/deprecated/VoiceOS-JITLearning-260118/`
- `archive/VoiceOS-Legacy-260119/VoiceOSCore/`
- `Modules/AvaMagic/` (duplicate of VoiceOS structure)

---

## Foreign Key Relationships

### VoiceOS/core/database FK Graph

```
learned_apps (package_name PK)
    |
    +-- app_consent_history (FK: package_name)
    +-- exploration_sessions (FK: package_name)
    +-- navigation_edges (FK: package_name, session_id)
    +-- screen_states (FK: package_name)

scraped_app (appId PK)
    |
    +-- scraped_element (FK: appId)
    +-- screen_context (FK: appId)

scraped_element (elementHash)
    |
    +-- scraped_hierarchy (FK: parentElementHash, childElementHash) [COMMENTED OUT]
    +-- element_relationship (FK: sourceElementHash, targetElementHash) [COMMENTED OUT]
    +-- commands_generated (FK: elementHash) [COMMENTED OUT]

avid_elements (avid PK)
    |
    +-- avid_aliases (FK: avid)
    +-- avid_analytics (FK: avid)
    +-- avid_hierarchy (FK: parent_avid, child_avid)

plugins (id PK)
    |
    +-- plugin_dependencies (FK: plugin_id, depends_on_plugin_id)
    +-- plugin_permissions (FK: plugin_id)

scraped_websites (url_hash PK)
    |
    +-- scraped_web_elements (FK: website_url_hash)
    +-- generated_web_commands (FK: website_url_hash)
    +-- scraped_websites (self-reference: parent_url_hash)
```

### Modules/Database FK Graph

```
avid_element (avid PK)
    |
    +-- avid_alias (FK: avid)
    +-- avid_analytics (FK: avid)
    +-- avid_hierarchy (FK: parent_avid, child_avid)

tab_group (id PK)
    |
    +-- tab (FK: group_id)

session (id PK)
    |
    +-- session_tab (FK: session_id)

favorite (id PK)
    |
    +-- favorite_tag (FK: favorite_id)
```

---

## Consolidation Recommendations

### Phase 1: Schema Alignment

1. **Unify AVID Tables**
   - Consolidate to the richer Modules/Database schema
   - Add missing fields from VoiceOS version (lifecycle_state, path)
   - Standardize table naming: `avid_element` (singular), `avid_alias`, `avid_analytics`, `avid_hierarchy`

2. **Resolve Naming Conflicts**
   - `avid_element` vs `avid_elements`
   - `avid_alias` vs `avid_aliases`

3. **Merge Analytics Fields**
   - Combine Modules/Database detailed metrics with VoiceOS lifecycle_state

### Phase 2: Physical Consolidation

**Option A: Single Database Module** (Recommended)
- Move all schemas to Modules/VoiceOS/core/database
- Add browser/ and web/ subfolders from Modules/Database
- Deprecate Modules/Database module

**Option B: Split by Domain**
- Keep VoiceOS/core/database for voice/accessibility
- Keep Modules/Database for AVID and browser
- Share common types via a core-types module

### Phase 3: Consumer Migration

1. Update all imports from `Modules/Database` to `VoiceOS/core/database`
2. Update SQLDelight generation configuration
3. Create migration scripts for runtime database schema changes
4. Update DI modules to provide repositories from single source

### Risk Mitigation

- Create database backup before migration
- Implement version-aware migrations
- Add comprehensive repository interface tests
- Phase rollout: test environments first

---

## Appendix: File Count Summary

```
Modules/Database:           7 .sq files, ~14 tables
VoiceOS/core/database:     44 .sq files, ~60 tables
Total unique tables:       ~70 tables (after consolidation ~66)
Duplicate tables:           4 AVID tables requiring merge
```

---

*Document generated for Phase 1 of Database Consolidation Project*
