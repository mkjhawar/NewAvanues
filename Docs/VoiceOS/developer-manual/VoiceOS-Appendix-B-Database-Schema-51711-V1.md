# Appendix B: Database Schema Reference
## VOS4 Developer Manual

**Version:** 4.1.0 (VoiceOSAppDatabase v1 - Consolidated)
**Last Updated:** 2025-11-07
**Status:** Living Document
**Framework:** IDEACODE v5.3

---

## CRITICAL UPDATE (2025-11-07)

**Database Consolidation Completed**

As of commit `19e35e0` (2025-11-07), all app metadata is now stored in **VoiceOSAppDatabase** only.

**What This Means for This Schema Reference:**
- This appendix now documents the **unified VoiceOSAppDatabase** schema
- Legacy AppScrapingDatabase (v9) and LearnAppDatabase schemas are deprecated
- The unified schema supports BOTH exploration and scraping modes in a single entity
- See [ADR-005](../planning/architecture/decisions/ADR-005-Database-Consolidation-Activation-2511070830.md) for consolidation details
- See [Chapter 16: Database Design](16-Database-Design.md#database-consolidation) for migration process

---

## Table of Contents

### Part I: Overview
- [B.1 Database Architecture](#b1-database-architecture)
- [B.2 Schema Evolution](#b2-schema-evolution)
- [B.3 Database Statistics](#b3-database-statistics)

### Part II: Complete SQL Schema
- [B.4 Apps Table](#b4-apps-table)
- [B.5 Scraped Elements Table](#b5-scraped-elements-table)
- [B.6 Generated Commands Table](#b6-generated-commands-table)
- [B.7 Screen Contexts Table](#b7-screen-contexts-table)
- [B.8 Screen Transitions Table](#b8-screen-transitions-table)
- [B.9 Element Relationships Table](#b9-element-relationships-table)
- [B.10 User Interactions Table](#b10-user-interactions-table)
- [B.11 Element State History Table](#b11-element-state-history-table)
- [B.12 Scraped Hierarchy Table](#b12-scraped-hierarchy-table)
- [B.13 Exploration Sessions Table](#b13-exploration-sessions-table)
- [B.14 Screens Table](#b14-screens-table)

### Part III: Entity Relationship Diagram
- [B.15 ER Diagram (ASCII)](#b15-er-diagram-ascii)
- [B.16 Relationship Types](#b16-relationship-types)
- [B.17 Foreign Key Constraints](#b17-foreign-key-constraints)

### Part IV: Migrations
- [B.18 Migration 1 to 2](#b18-migration-1-to-2)
- [B.19 Migration 2 to 3](#b19-migration-2-to-3)
- [B.20 Migration 3 to 4](#b20-migration-3-to-4)

### Part V: Query Examples
- [B.21 Common Queries](#b21-common-queries)
- [B.22 Advanced Queries](#b22-advanced-queries)
- [B.23 Performance Tips](#b23-performance-tips)

---

## B.1 Database Architecture

### B.1.1 Overview

VOS4 uses **Room Persistence Library** for database management, which provides:

- Compile-time verification of SQL queries
- Convenient database access with DAOs
- Automatic schema generation
- Built-in migration support
- LiveData/Flow reactive queries

### B.1.2 Database Structure

```
VoiceOSAppDatabase (v4)
├── apps (unified app metadata)
├── screens (screen states)
├── exploration_sessions (learning sessions)
├── scraped_elements (UI elements)
├── scraped_hierarchy (element relationships - tree structure)
├── generated_commands (voice commands)
├── screen_contexts (screen-level context)
├── screen_transitions (navigation flows)
├── element_relationships (semantic relationships)
├── user_interactions (interaction tracking)
└── element_state_history (state changes)
```

### B.1.3 Design Principles

1. **Single Source of Truth:** Unified `apps` table consolidates LearnApp and Scraping data
2. **Hash-Based Lookups:** Element hashes enable O(1) lookups
3. **Cascading Deletes:** FK constraints maintain referential integrity
4. **Nullable Fields:** Mode-specific fields are nullable for semantic correctness
5. **Indexed Foreign Keys:** All FKs have corresponding indices for query performance

---

## B.2 Schema Evolution

### B.2.1 Database Consolidation History

**VoiceOSAppDatabase Timeline:**

| Date | Event | Description |
|------|-------|-------------|
| 2025-10-30 | Phase 3A Complete | VoiceOSAppDatabase created with unified schema |
| 2025-11-06 | Bad Commit (8443c63) | Attempted consolidation by deletion (REVERTED) |
| 2025-11-06 | Revert (8606fee) | Restored LearnApp and AppScraping modules |
| 2025-11-07 | Proper Implementation (19e35e0) | Activated VoiceOSAppDatabase with migration |

**Current Status (v4.1):**
- VoiceOSAppDatabase: ✅ Active (version 1, started with complete schema)
- AppScrapingDatabase: ⚠️ Legacy (version 9, kept as backup)
- LearnAppDatabase: ⚠️ Legacy (kept as backup)

### B.2.2 VoiceOSAppDatabase Schema Versions

VoiceOSAppDatabase started at **version 1** with the complete consolidated schema (no schema migrations needed yet).

| Version | Date | Changes | Migration |
|---------|------|---------|-----------|
| v1 | 2025-10-30 | Initial consolidated schema (Phase 3A) | N/A |

**Future schema changes** will use Room's migration framework (MIGRATION_1_2, etc.) as needed.

### B.2.3 Legacy AppScrapingDatabase Schema History

**Historical Reference Only** - AppScrapingDatabase evolved through 9 versions before consolidation:

| Version | Date | Changes | Migration |
|---------|------|---------|-----------|
| v1 | 2025-10-09 | Initial scraping schema | N/A |
| v2 | 2025-10-15 | Unique element hashes, String FK for commands | MIGRATION_1_2 |
| v3 | 2025-10-20 | LearnApp tracking fields added | MIGRATION_2_3 |
| v4 | 2025-10-22 | UUID integration | MIGRATION_3_4 |
| v5 | 2025-10-24 | AI context fields | MIGRATION_4_5 |
| v6 | 2025-10-26 | Screen contexts, element relationships | MIGRATION_5_6 |
| v7 | 2025-10-28 | Screen transitions | MIGRATION_6_7 |
| v8 | 2025-10-30 | User interactions, state history | MIGRATION_7_8 |
| v9 | 2025-10-31 | AppEntity reference | MIGRATION_8_9 |

**Note:** All schema evolution for v9 was incorporated into VoiceOSAppDatabase v1.

### B.2.4 Data Migration (Database Consolidation)

**One-Time Migration (v4.0 → v4.1):**

This is **different from schema migrations** - it migrates data **between databases**, not schema changes within a single database.

**Implementation:** `DatabaseMigrationHelper.kt`

**Process:**
1. Check if migration already completed (SharedPreferences flag)
2. Migrate LearnAppDatabase → VoiceOSAppDatabase (LEARN_APP fields)
3. Migrate AppScrapingDatabase → VoiceOSAppDatabase (DYNAMIC fields)
4. Mark migration complete

**Idempotent:** Safe to run multiple times (checks completion flag)

**Rollback:** Old databases retained as backup, can revert code changes

See [Chapter 16: Database Consolidation](16-Database-Design.md#database-consolidation) for full details.

### B.2.5 Breaking Changes Log

**v4.1 (2025-11-07) - Database Consolidation:**
- ✅ **NON-BREAKING:** VoiceOSAppDatabase activated as single source of truth
- ✅ **Migration:** Automatic one-time data migration via `DatabaseMigrationHelper.kt`
- ✅ **Backward Compatible:** Legacy databases kept as backup, no code deletion
- ⚠️ **Code Changes Required:**
  - Use `VoiceOSAppDatabase.getInstance()` instead of `AppScrapingDatabase.getInstance()`
  - Use `appDao()` instead of `scrapedAppDao()`
  - Field mappings: `elementCount` → `scrapedElementCount`, `totalElements` → `exploredElementCount`
- See [ADR-005](../planning/architecture/decisions/ADR-005-Database-Consolidation-Activation-2511070830.md) for full details

**Legacy AppScrapingDatabase Breaking Changes (Historical):**

**v4 (2025-10-31):**
- ❌ **BREAKING:** Removed `scraped_apps` table (consolidated into `apps`)
- ✅ **Migration:** Automatic data migration in MIGRATION_3_4
- ⚠️ **Impact:** Code using `ScrapedAppDao` must switch to `AppDao`

**v3 (2025-10-25):**
- ✅ **Non-breaking:** Added feature flags (defaults provided)
- ✅ **Backward Compatible:** No code changes required

**v2 (2025-10-15):**
- ❌ **BREAKING:** Changed `generated_commands.element_id` (Long) → `element_hash` (String)
- ✅ **Migration:** Automatic data migration with FK joins
- ⚠️ **Impact:** Orphaned commands (no matching element) are dropped

---

## B.3 Database Statistics

### B.3.1 Size Estimates

**Typical Deployment (50 apps, moderate usage):**

| Table | Rows | Size per Row | Total Size |
|-------|------|--------------|------------|
| apps | 50 | ~500 bytes | 25 KB |
| screens | 500 | ~300 bytes | 150 KB |
| scraped_elements | 25,000 | ~600 bytes | 15 MB |
| generated_commands | 10,000 | ~200 bytes | 2 MB |
| screen_contexts | 500 | ~400 bytes | 200 KB |
| screen_transitions | 2,000 | ~100 bytes | 200 KB |
| element_relationships | 50,000 | ~150 bytes | 7.5 MB |
| user_interactions | 10,000 | ~150 bytes | 1.5 MB |
| element_state_history | 5,000 | ~200 bytes | 1 MB |
| **TOTAL** | | | **~27.5 MB** |

**Large Deployment (200 apps, heavy usage):**
- Estimated total: **~100-150 MB**

### B.3.2 Performance Characteristics

- **Insert:** O(1) with hash-based primary keys
- **Lookup by Hash:** O(1) with unique indices
- **Lookup by App:** O(log n) with indexed FK
- **Delete Cascade:** Automatic via FK constraints

---

## B.4 Apps Table

### B.4.1 Table Schema

```sql
CREATE TABLE apps (
    package_name TEXT NOT NULL PRIMARY KEY,
    app_id TEXT NOT NULL,
    app_name TEXT NOT NULL,
    version_code INTEGER NOT NULL,
    version_name TEXT NOT NULL,
    app_hash TEXT NOT NULL,

    -- LEARN_APP mode fields
    exploration_status TEXT,
    total_screens INTEGER,
    explored_element_count INTEGER,
    total_edges INTEGER,
    root_screen_hash TEXT,
    first_explored INTEGER,
    last_explored INTEGER,

    -- DYNAMIC mode fields
    scraped_element_count INTEGER,
    command_count INTEGER,
    scrape_count INTEGER,
    first_scraped INTEGER,
    last_scraped INTEGER,

    -- Cross-mode fields
    scraping_mode TEXT,
    is_fully_learned INTEGER,
    learn_completed_at INTEGER,

    -- Feature flags (v3+)
    learn_app_enabled INTEGER NOT NULL DEFAULT 1,
    dynamic_scraping_enabled INTEGER NOT NULL DEFAULT 1,
    max_scrape_depth INTEGER DEFAULT NULL
);

-- Indices
CREATE UNIQUE INDEX index_apps_appId ON apps(app_id);
CREATE UNIQUE INDEX index_apps_packageName ON apps(package_name);
CREATE INDEX index_apps_explorationStatus ON apps(exploration_status);
CREATE INDEX index_apps_scrapingMode ON apps(scraping_mode);
CREATE INDEX index_apps_isFullyLearned ON apps(is_fully_learned);
```

### B.4.2 Field Reference

| Field | Type | Nullable | Description | Example |
|-------|------|----------|-------------|---------|
| `package_name` | TEXT | No | Android package name (PK) | `com.example.app` |
| `app_id` | TEXT | No | UUID identifier | `550e8400-e29b-41d4-a716-446655440000` |
| `app_name` | TEXT | No | Human-readable name | `Example App` |
| `version_code` | INTEGER | No | Numeric version | `42` |
| `version_name` | TEXT | No | Version string | `1.2.3` |
| `app_hash` | TEXT | No | Fingerprint hash | `a3f4d5e6...` |
| `exploration_status` | TEXT | Yes | Learning status | `COMPLETE`, `IN_PROGRESS` |
| `total_screens` | INTEGER | Yes | Discovered screens | `25` |
| `explored_element_count` | INTEGER | Yes | Elements mapped | `1200` |
| `total_edges` | INTEGER | Yes | Navigation edges | `80` |
| `root_screen_hash` | TEXT | Yes | Entry point hash | `b7c8d9...` |
| `first_explored` | INTEGER | Yes | First exploration timestamp | `1698765432000` |
| `last_explored` | INTEGER | Yes | Last exploration timestamp | `1698865432000` |
| `scraped_element_count` | INTEGER | Yes | Dynamic scrape count | `450` |
| `command_count` | INTEGER | Yes | Generated commands | `120` |
| `scrape_count` | INTEGER | Yes | Scrape iterations | `15` |
| `first_scraped` | INTEGER | Yes | First scrape timestamp | `1698665432000` |
| `last_scraped` | INTEGER | Yes | Last scrape timestamp | `1698965432000` |
| `scraping_mode` | TEXT | Yes | Current mode | `DYNAMIC`, `LEARN_APP` |
| `is_fully_learned` | INTEGER | Yes | Learning complete flag | `1` (true), `0` (false) |
| `learn_completed_at` | INTEGER | Yes | Completion timestamp | `1698865432000` |
| `learn_app_enabled` | INTEGER | No | Feature flag | `1` (enabled) |
| `dynamic_scraping_enabled` | INTEGER | No | Feature flag | `1` (enabled) |
| `max_scrape_depth` | INTEGER | Yes | Depth override | `10` |

### B.4.3 Constraints

**Primary Key:** `package_name`
**Unique Constraints:**
- `app_id` (UUID uniqueness)
- `package_name` (implicit via PK)

**Check Constraints:** (implicit in Room)
- `version_code` >= 0
- `exploration_status` IN ('NOT_STARTED', 'IN_PROGRESS', 'COMPLETE', 'FAILED')
- `scraping_mode` IN ('DYNAMIC', 'LEARN_APP')
- `is_fully_learned` IN (0, 1)

---

## B.5 Scraped Elements Table

### B.5.1 Table Schema

```sql
CREATE TABLE scraped_elements (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    element_hash TEXT NOT NULL,
    app_id TEXT NOT NULL,
    uuid TEXT,
    class_name TEXT NOT NULL,
    view_id_resource_name TEXT,
    text TEXT,
    content_description TEXT,
    bounds TEXT NOT NULL,
    is_clickable INTEGER NOT NULL,
    is_long_clickable INTEGER NOT NULL,
    is_editable INTEGER NOT NULL,
    is_scrollable INTEGER NOT NULL,
    is_checkable INTEGER NOT NULL,
    is_focusable INTEGER NOT NULL,
    is_enabled INTEGER NOT NULL,
    depth INTEGER NOT NULL,
    index_in_parent INTEGER NOT NULL,
    scraped_at INTEGER NOT NULL,

    -- AI Context Fields (Phase 1)
    semantic_role TEXT,
    input_type TEXT,
    visual_weight TEXT,
    is_required INTEGER,

    -- Form Context Fields (Phase 2)
    form_group_id TEXT,
    placeholder_text TEXT,
    validation_pattern TEXT,
    background_color TEXT,

    FOREIGN KEY(app_id) REFERENCES apps(app_id) ON DELETE CASCADE
);

-- Indices
CREATE INDEX index_scraped_elements_app_id ON scraped_elements(app_id);
CREATE UNIQUE INDEX index_scraped_elements_element_hash ON scraped_elements(element_hash);
CREATE INDEX index_scraped_elements_view_id_resource_name ON scraped_elements(view_id_resource_name);
CREATE INDEX index_scraped_elements_uuid ON scraped_elements(uuid);
```

### B.5.2 Field Reference

| Field | Type | Nullable | Description | Example |
|-------|------|----------|-------------|---------|
| `id` | INTEGER | No | Auto-increment PK | `1234` |
| `element_hash` | TEXT | No | SHA-256 hash (unique) | `abc123...` |
| `app_id` | TEXT | No | FK to apps.app_id | `550e8400...` |
| `uuid` | TEXT | Yes | Universal identifier | `uuid:android:btn:submit` |
| `class_name` | TEXT | No | Android view class | `android.widget.Button` |
| `view_id_resource_name` | TEXT | Yes | Resource ID | `com.app:id/submit_button` |
| `text` | TEXT | Yes | Visible text | `Submit` |
| `content_description` | TEXT | Yes | Accessibility label | `Submit form` |
| `bounds` | TEXT | No | Screen bounds | `[100,200][400,300]` |
| `is_clickable` | INTEGER | No | Clickable flag | `1` (true) |
| `is_long_clickable` | INTEGER | No | Long-click flag | `0` (false) |
| `is_editable` | INTEGER | No | Text input flag | `0` (false) |
| `is_scrollable` | INTEGER | No | Scrollable flag | `0` (false) |
| `is_checkable` | INTEGER | No | Checkbox/radio flag | `0` (false) |
| `is_focusable` | INTEGER | No | Focusable flag | `1` (true) |
| `is_enabled` | INTEGER | No | Enabled state | `1` (true) |
| `depth` | INTEGER | No | Tree depth | `3` |
| `index_in_parent` | INTEGER | No | Sibling index | `2` |
| `scraped_at` | INTEGER | No | Scrape timestamp | `1698765432000` |
| `semantic_role` | TEXT | Yes | AI-inferred role | `button`, `input`, `label` |
| `input_type` | TEXT | Yes | Input type | `email`, `password`, `text` |
| `visual_weight` | TEXT | Yes | Visual prominence | `primary`, `secondary` |
| `is_required` | INTEGER | Yes | Required field | `1` (required) |
| `form_group_id` | TEXT | Yes | Form group | `login_form` |
| `placeholder_text` | TEXT | Yes | Placeholder | `Enter email` |
| `validation_pattern` | TEXT | Yes | Validation regex | `^[a-z0-9._%+-]+@` |
| `background_color` | TEXT | Yes | Color hex | `#FF5722` |

### B.5.3 Semantic Role Values

**Common Semantic Roles:**
- `button` - Action button
- `input` - Text input field
- `label` - Text label
- `checkbox` - Checkbox/toggle
- `radio` - Radio button
- `dropdown` - Spinner/dropdown
- `image` - Image view
- `link` - Hyperlink
- `container` - Layout container
- `list` - List/RecyclerView
- `unknown` - Cannot determine

---

## B.6 Generated Commands Table

### B.6.1 Table Schema

```sql
CREATE TABLE generated_commands (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    element_hash TEXT NOT NULL,
    command_text TEXT NOT NULL,
    action_type TEXT NOT NULL,
    confidence REAL NOT NULL,
    synonyms TEXT NOT NULL,
    is_user_approved INTEGER NOT NULL DEFAULT 0,
    usage_count INTEGER NOT NULL DEFAULT 0,
    last_used INTEGER,
    generated_at INTEGER NOT NULL,

    FOREIGN KEY(element_hash) REFERENCES scraped_elements(element_hash) ON DELETE CASCADE
);

-- Indices
CREATE INDEX index_generated_commands_element_hash ON generated_commands(element_hash);
CREATE INDEX index_generated_commands_command_text ON generated_commands(command_text);
CREATE INDEX index_generated_commands_action_type ON generated_commands(action_type);
```

### B.6.2 Field Reference

| Field | Type | Nullable | Description | Example |
|-------|------|----------|-------------|---------|
| `id` | INTEGER | No | Auto-increment PK | `5678` |
| `element_hash` | TEXT | No | FK to scraped_elements | `abc123...` |
| `command_text` | TEXT | No | Primary command phrase | `click submit button` |
| `action_type` | TEXT | No | Action type | `click`, `type`, `scroll` |
| `confidence` | REAL | No | AI confidence (0.0-1.0) | `0.92` |
| `synonyms` | TEXT | No | JSON array of alternatives | `["send","post","submit"]` |
| `is_user_approved` | INTEGER | No | User confirmation flag | `1` (approved) |
| `usage_count` | INTEGER | No | Execution count | `15` |
| `last_used` | INTEGER | Yes | Last usage timestamp | `1698865432000` |
| `generated_at` | INTEGER | No | Generation timestamp | `1698765432000` |

### B.6.3 Action Types

**Supported Action Types:**
- `click` - Single tap
- `long_click` - Long press
- `type` - Text input
- `scroll` - Scroll gesture
- `focus` - Focus element
- `check` - Check checkbox
- `uncheck` - Uncheck checkbox
- `select` - Select dropdown item

---

## B.7 Screen Contexts Table

### B.7.1 Table Schema

```sql
CREATE TABLE screen_contexts (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    screen_hash TEXT NOT NULL,
    app_id TEXT NOT NULL,
    package_name TEXT NOT NULL,
    activity_name TEXT,
    window_title TEXT,
    screen_type TEXT,
    form_context TEXT,
    navigation_level INTEGER NOT NULL,
    primary_action TEXT,
    element_count INTEGER NOT NULL,
    has_back_button INTEGER NOT NULL,
    first_scraped INTEGER NOT NULL,
    last_scraped INTEGER NOT NULL,
    visit_count INTEGER NOT NULL,

    FOREIGN KEY(app_id) REFERENCES apps(app_id) ON DELETE CASCADE
);

-- Indices
CREATE UNIQUE INDEX index_screen_contexts_screen_hash ON screen_contexts(screen_hash);
CREATE INDEX index_screen_contexts_app_id ON screen_contexts(app_id);
CREATE INDEX index_screen_contexts_package_name ON screen_contexts(package_name);
CREATE INDEX index_screen_contexts_screen_type ON screen_contexts(screen_type);
```

### B.7.2 Field Reference

| Field | Type | Nullable | Description | Example |
|-------|------|----------|-------------|---------|
| `id` | INTEGER | No | Auto-increment PK | `123` |
| `screen_hash` | TEXT | No | SHA-256 hash (unique) | `def456...` |
| `app_id` | TEXT | No | FK to apps.app_id | `550e8400...` |
| `package_name` | TEXT | No | App package | `com.example.app` |
| `activity_name` | TEXT | Yes | Activity class | `MainActivity` |
| `window_title` | TEXT | Yes | Window title | `Login Screen` |
| `screen_type` | TEXT | Yes | Screen classification | `login`, `list`, `detail` |
| `form_context` | TEXT | Yes | Form type | `login_form`, `signup_form` |
| `navigation_level` | INTEGER | No | Depth in nav stack | `2` |
| `primary_action` | TEXT | Yes | Main CTA | `Submit`, `Login` |
| `element_count` | INTEGER | No | Element count | `25` |
| `has_back_button` | INTEGER | No | Back button present | `1` (true) |
| `first_scraped` | INTEGER | No | First scrape | `1698765432000` |
| `last_scraped` | INTEGER | No | Last scrape | `1698865432000` |
| `visit_count` | INTEGER | No | Visit frequency | `5` |

### B.7.3 Screen Types

**Common Screen Types:**
- `login` - Login/authentication screen
- `signup` - Registration screen
- `list` - List/grid view
- `detail` - Detail/content view
- `settings` - Settings screen
- `profile` - User profile
- `checkout` - Purchase flow
- `confirmation` - Confirmation/success
- `error` - Error screen
- `unknown` - Unclassified

---

## B.8 Screen Transitions Table

### B.8.1 Table Schema

```sql
CREATE TABLE screen_transitions (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    from_screen_hash TEXT NOT NULL,
    to_screen_hash TEXT NOT NULL,
    transition_count INTEGER NOT NULL DEFAULT 1,
    first_transition INTEGER NOT NULL,
    last_transition INTEGER NOT NULL,
    avg_transition_time INTEGER,

    FOREIGN KEY(from_screen_hash) REFERENCES screen_contexts(screen_hash) ON DELETE CASCADE,
    FOREIGN KEY(to_screen_hash) REFERENCES screen_contexts(screen_hash) ON DELETE CASCADE
);

-- Indices
CREATE INDEX index_screen_transitions_from_screen_hash ON screen_transitions(from_screen_hash);
CREATE INDEX index_screen_transitions_to_screen_hash ON screen_transitions(to_screen_hash);
CREATE UNIQUE INDEX index_screen_transitions_unique ON screen_transitions(from_screen_hash, to_screen_hash);
```

### B.8.2 Field Reference

| Field | Type | Nullable | Description | Example |
|-------|------|----------|-------------|---------|
| `id` | INTEGER | No | Auto-increment PK | `789` |
| `from_screen_hash` | TEXT | No | Source screen FK | `def456...` |
| `to_screen_hash` | TEXT | No | Target screen FK | `ghi789...` |
| `transition_count` | INTEGER | No | Frequency | `12` |
| `first_transition` | INTEGER | No | First occurrence | `1698765432000` |
| `last_transition` | INTEGER | No | Last occurrence | `1698865432000` |
| `avg_transition_time` | INTEGER | Yes | Avg time (ms) | `350` |

---

## B.9 Element Relationships Table

### B.9.1 Table Schema

```sql
CREATE TABLE element_relationships (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    source_element_hash TEXT NOT NULL,
    target_element_hash TEXT,
    relationship_type TEXT NOT NULL,
    relationship_data TEXT,
    confidence REAL NOT NULL DEFAULT 1.0,
    inferred_by TEXT NOT NULL DEFAULT 'accessibility_tree',
    created_at INTEGER NOT NULL,

    FOREIGN KEY(source_element_hash) REFERENCES scraped_elements(element_hash) ON DELETE CASCADE,
    FOREIGN KEY(target_element_hash) REFERENCES scraped_elements(element_hash) ON DELETE CASCADE
);

-- Indices
CREATE INDEX index_element_relationships_source_element_hash ON element_relationships(source_element_hash);
CREATE INDEX index_element_relationships_target_element_hash ON element_relationships(target_element_hash);
CREATE INDEX index_element_relationships_relationship_type ON element_relationships(relationship_type);
CREATE UNIQUE INDEX index_element_relationships_unique ON element_relationships(source_element_hash, target_element_hash, relationship_type);
```

### B.9.2 Relationship Types

**Common Relationship Types:**
- `parent_child` - Hierarchical parent-child
- `label_for` - Label describes another element
- `input_for` - Input field for a form
- `button_for` - Button submits a form
- `group_member` - Member of a button group
- `depends_on` - Conditional dependency
- `validates` - Validation relationship

---

## B.10 User Interactions Table

### B.10.1 Table Schema

```sql
CREATE TABLE user_interactions (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    element_hash TEXT NOT NULL,
    screen_hash TEXT NOT NULL,
    interaction_type TEXT NOT NULL,
    interaction_time INTEGER NOT NULL,
    visibility_start INTEGER,
    visibility_duration INTEGER,
    success INTEGER NOT NULL DEFAULT 1,
    created_at INTEGER NOT NULL,

    FOREIGN KEY(element_hash) REFERENCES scraped_elements(element_hash) ON DELETE CASCADE,
    FOREIGN KEY(screen_hash) REFERENCES screen_contexts(screen_hash) ON DELETE CASCADE
);

-- Indices
CREATE INDEX index_user_interactions_element_hash ON user_interactions(element_hash);
CREATE INDEX index_user_interactions_screen_hash ON user_interactions(screen_hash);
CREATE INDEX index_user_interactions_interaction_type ON user_interactions(interaction_type);
CREATE INDEX index_user_interactions_interaction_time ON user_interactions(interaction_time);
```

---

## B.11 Element State History Table

### B.11.1 Table Schema

```sql
CREATE TABLE element_state_history (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    element_hash TEXT NOT NULL,
    screen_hash TEXT NOT NULL,
    state_type TEXT NOT NULL,
    old_value TEXT,
    new_value TEXT,
    changed_at INTEGER NOT NULL,
    triggered_by TEXT,

    FOREIGN KEY(element_hash) REFERENCES scraped_elements(element_hash) ON DELETE CASCADE,
    FOREIGN KEY(screen_hash) REFERENCES screen_contexts(screen_hash) ON DELETE CASCADE
);

-- Indices
CREATE INDEX index_element_state_history_element_hash ON element_state_history(element_hash);
CREATE INDEX index_element_state_history_screen_hash ON element_state_history(screen_hash);
CREATE INDEX index_element_state_history_state_type ON element_state_history(state_type);
CREATE INDEX index_element_state_history_changed_at ON element_state_history(changed_at);
```

---

## B.15 ER Diagram (ASCII)

```
┌──────────────────┐
│   apps (v4)      │ PK: package_name
├──────────────────┤
│ package_name (PK)│◄─────────┐
│ app_id (UQ)      │          │
│ app_name         │          │
│ version_code     │          │
│ version_name     │          │
│ app_hash         │          │
│ ...              │          │
└──────────────────┘          │ FK: app_id
                              │
                              │
┌──────────────────────────┐  │
│  scraped_elements        │  │
├──────────────────────────┤  │
│ id (PK)                  │  │
│ element_hash (UQ)        │  │
│ app_id (FK) ─────────────┼──┘
│ uuid                     │
│ class_name               │
│ text                     │
│ bounds                   │◄─────────┐
│ is_clickable             │          │
│ ...                      │          │
└──────────────────────────┘          │ FK: element_hash
                                      │
                                      │
┌──────────────────────────┐          │
│  generated_commands      │          │
├──────────────────────────┤          │
│ id (PK)                  │          │
│ element_hash (FK) ───────┼──────────┘
│ command_text             │
│ action_type              │
│ confidence               │
│ synonyms                 │
│ usage_count              │
│ ...                      │
└──────────────────────────┘


┌──────────────────────────┐
│  screen_contexts         │
├──────────────────────────┤
│ id (PK)                  │
│ screen_hash (UQ)         │◄─────────┐
│ app_id (FK) ─────────────┼──┐       │
│ package_name             │  │       │
│ screen_type              │  │       │
│ element_count            │  │       │ FK: screen_hash
│ ...                      │  │       │
└──────────────────────────┘  │       │
                              │       │
                              │       │
┌──────────────────────────┐  │       │
│  screen_transitions      │  │       │
├──────────────────────────┤  │       │
│ id (PK)                  │  │       │
│ from_screen_hash (FK) ───┼──┼───────┘
│ to_screen_hash (FK) ─────┼──┤
│ transition_count         │  │
│ avg_transition_time      │  │
│ ...                      │  │
└──────────────────────────┘  │
                              │
                              │
┌──────────────────────────┐  │
│  user_interactions       │  │
├──────────────────────────┤  │
│ id (PK)                  │  │
│ element_hash (FK)        │  │
│ screen_hash (FK) ────────┼──┘
│ interaction_type         │
│ interaction_time         │
│ ...                      │
└──────────────────────────┘
```

---

## B.16 Relationship Types

### B.16.1 One-to-Many Relationships

1. **apps → scraped_elements** (1:N)
   - One app has many elements
   - FK: `scraped_elements.app_id` → `apps.app_id`
   - Cascade: DELETE CASCADE

2. **scraped_elements → generated_commands** (1:N)
   - One element has many commands
   - FK: `generated_commands.element_hash` → `scraped_elements.element_hash`
   - Cascade: DELETE CASCADE

3. **apps → screen_contexts** (1:N)
   - One app has many screens
   - FK: `screen_contexts.app_id` → `apps.app_id`
   - Cascade: DELETE CASCADE

### B.16.2 Many-to-Many Relationships

1. **screen_contexts ↔ screen_contexts** (N:M via screen_transitions)
   - Screens transition to other screens
   - Junction table: `screen_transitions`
   - FK1: `from_screen_hash` → `screen_contexts.screen_hash`
   - FK2: `to_screen_hash` → `screen_contexts.screen_hash`

2. **scraped_elements ↔ scraped_elements** (N:M via element_relationships)
   - Elements relate to other elements
   - Junction table: `element_relationships`
   - FK1: `source_element_hash` → `scraped_elements.element_hash`
   - FK2: `target_element_hash` → `scraped_elements.element_hash`

---

## B.17 Foreign Key Constraints

### B.17.1 Constraint Summary

| Child Table | Parent Table | FK Column | Parent Column | On Delete |
|-------------|--------------|-----------|---------------|-----------|
| scraped_elements | apps | app_id | app_id | CASCADE |
| generated_commands | scraped_elements | element_hash | element_hash | CASCADE |
| screen_contexts | apps | app_id | app_id | CASCADE |
| screen_transitions | screen_contexts | from_screen_hash | screen_hash | CASCADE |
| screen_transitions | screen_contexts | to_screen_hash | screen_hash | CASCADE |
| element_relationships | scraped_elements | source_element_hash | element_hash | CASCADE |
| element_relationships | scraped_elements | target_element_hash | element_hash | CASCADE |
| user_interactions | scraped_elements | element_hash | element_hash | CASCADE |
| user_interactions | screen_contexts | screen_hash | screen_hash | CASCADE |
| element_state_history | scraped_elements | element_hash | element_hash | CASCADE |
| element_state_history | screen_contexts | screen_hash | screen_hash | CASCADE |

### B.17.2 Cascading Behavior

**When an app is deleted:**
```sql
DELETE FROM apps WHERE package_name = 'com.example.app'
```

**Cascading deletes:**
1. All `scraped_elements` for that app (via app_id FK)
2. All `generated_commands` for those elements (via element_hash FK)
3. All `screen_contexts` for that app (via app_id FK)
4. All `screen_transitions` involving those screens (via screen_hash FK)
5. All `element_relationships` involving those elements (via element_hash FK)
6. All `user_interactions` for those elements/screens (via element_hash/screen_hash FK)
7. All `element_state_history` for those elements/screens (via element_hash/screen_hash FK)

**Result:** Complete cleanup of all app-related data with a single delete!

---

## B.18 Migration 1 to 2

### B.18.1 Changes

**Major Changes:**
1. Unified `apps` table (merged LearnApp + Scraping data)
2. Unique constraint on `scraped_elements.element_hash`
3. Changed `generated_commands` FK from `element_id` (Long) to `element_hash` (String)

**Data Migration Strategy:**
- Backup existing tables
- Create new schemas
- Migrate data with FK joins
- Drop old tables

### B.18.2 SQL Migration

```sql
-- Step 1: Backup apps table
ALTER TABLE apps RENAME TO apps_old;

-- Step 2: Backup scraped_apps table
ALTER TABLE scraped_apps RENAME TO scraped_apps_old;

-- Step 3: Create new unified apps table
CREATE TABLE apps (
    package_name TEXT NOT NULL PRIMARY KEY,
    app_id TEXT NOT NULL,
    app_name TEXT NOT NULL,
    version_code INTEGER NOT NULL,
    version_name TEXT NOT NULL,
    app_hash TEXT NOT NULL,
    -- ... (all other fields)
);

-- Step 4: Migrate data from scraped_apps_old (priority)
INSERT OR REPLACE INTO apps (...)
SELECT ... FROM scraped_apps_old;

-- Step 5: Migrate data from apps_old (no conflicts)
INSERT OR REPLACE INTO apps (...)
SELECT ... FROM apps_old
WHERE package_name NOT IN (SELECT package_name FROM apps);

-- Step 6: Update element_hash to unique
DROP INDEX IF EXISTS index_scraped_elements_element_hash;
CREATE UNIQUE INDEX index_scraped_elements_element_hash ON scraped_elements(element_hash);

-- Step 7: Migrate generated_commands
CREATE TABLE generated_commands_new (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    element_hash TEXT NOT NULL,
    command_text TEXT NOT NULL,
    action_type TEXT NOT NULL,
    confidence REAL NOT NULL,
    synonyms TEXT NOT NULL,
    is_user_approved INTEGER NOT NULL DEFAULT 0,
    usage_count INTEGER NOT NULL DEFAULT 0,
    last_used INTEGER,
    generated_at INTEGER NOT NULL,
    FOREIGN KEY(element_hash) REFERENCES scraped_elements(element_hash) ON DELETE CASCADE
);

INSERT INTO generated_commands_new (...)
SELECT gc.id, se.element_hash, gc.command_text, ...
FROM generated_commands gc
INNER JOIN scraped_elements se ON gc.element_id = se.id;

DROP TABLE generated_commands;
ALTER TABLE generated_commands_new RENAME TO generated_commands;

-- Step 8: Clean up
DROP TABLE apps_old;
DROP TABLE scraped_apps_old;
```

---

## B.19 Migration 2 to 3

### B.19.1 Changes

**Added Feature Flags:**
- `learn_app_enabled` (default: 1)
- `dynamic_scraping_enabled` (default: 1)
- `max_scrape_depth` (default: NULL)

### B.19.2 SQL Migration

```sql
-- Add feature flag columns
ALTER TABLE apps ADD COLUMN learn_app_enabled INTEGER NOT NULL DEFAULT 1;
ALTER TABLE apps ADD COLUMN dynamic_scraping_enabled INTEGER NOT NULL DEFAULT 1;
ALTER TABLE apps ADD COLUMN max_scrape_depth INTEGER DEFAULT NULL;
```

---

## B.20 Migration 3 to 4

### B.20.1 Changes

**FK Consolidation:**
- Updated `scraped_elements.app_id` FK to point to `apps.app_id` (was `scraped_apps.app_id`)
- Updated `screen_contexts.app_id` FK to point to `apps.app_id` (was `scraped_apps.app_id`)
- Dropped `scraped_apps` table entirely

**Why:** Eliminate duplication, single source of truth

### B.20.2 SQL Migration

```sql
-- Step 1: Recreate scraped_elements with FK to apps
ALTER TABLE scraped_elements RENAME TO scraped_elements_old;

CREATE TABLE scraped_elements (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    element_hash TEXT NOT NULL,
    app_id TEXT NOT NULL,
    -- ... (all other fields)
    FOREIGN KEY(app_id) REFERENCES apps(app_id) ON DELETE CASCADE
);

INSERT INTO scraped_elements SELECT * FROM scraped_elements_old;
DROP TABLE scraped_elements_old;

-- Step 2: Recreate screen_contexts with FK to apps
ALTER TABLE screen_contexts RENAME TO screen_contexts_old;

CREATE TABLE screen_contexts (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    screen_hash TEXT NOT NULL,
    app_id TEXT NOT NULL,
    -- ... (all other fields)
    FOREIGN KEY(app_id) REFERENCES apps(app_id) ON DELETE CASCADE
);

INSERT INTO screen_contexts SELECT * FROM screen_contexts_old;
DROP TABLE screen_contexts_old;

-- Step 3: Drop scraped_apps table
DROP TABLE IF EXISTS scraped_apps;
```

---

## B.21 Common Queries

### B.21.1 Basic Queries

**Get app by package name:**
```sql
SELECT * FROM apps WHERE package_name = 'com.example.app';
```

**Get all elements for an app:**
```sql
SELECT * FROM scraped_elements WHERE app_id = '550e8400-e29b-41d4-a716-446655440000';
```

**Get commands for a specific element:**
```sql
SELECT * FROM generated_commands WHERE element_hash = 'abc123...';
```

### B.21.2 Aggregate Queries

**Count apps by exploration status:**
```sql
SELECT exploration_status, COUNT(*) as count
FROM apps
GROUP BY exploration_status;
```

**Average elements per app:**
```sql
SELECT AVG(scraped_element_count) as avg_elements
FROM apps
WHERE scraped_element_count IS NOT NULL;
```

**Most popular commands:**
```sql
SELECT command_text, usage_count
FROM generated_commands
ORDER BY usage_count DESC
LIMIT 10;
```

### B.21.3 Join Queries

**Get all commands for an app:**
```sql
SELECT gc.command_text, gc.confidence, se.text, se.class_name
FROM generated_commands gc
INNER JOIN scraped_elements se ON gc.element_hash = se.element_hash
INNER JOIN apps a ON se.app_id = a.app_id
WHERE a.package_name = 'com.example.app';
```

**Get screen transition flow for an app:**
```sql
SELECT
    sc1.window_title as from_screen,
    sc2.window_title as to_screen,
    st.transition_count,
    st.avg_transition_time
FROM screen_transitions st
INNER JOIN screen_contexts sc1 ON st.from_screen_hash = sc1.screen_hash
INNER JOIN screen_contexts sc2 ON st.to_screen_hash = sc2.screen_hash
INNER JOIN apps a ON sc1.app_id = a.app_id
WHERE a.package_name = 'com.example.app'
ORDER BY st.transition_count DESC;
```

---

## B.22 Advanced Queries

### B.22.1 Navigation Flow Analysis

**Find most common user journeys:**
```sql
WITH RECURSIVE journey AS (
    SELECT
        from_screen_hash,
        to_screen_hash,
        transition_count,
        1 as depth,
        from_screen_hash || ' -> ' || to_screen_hash as path
    FROM screen_transitions
    WHERE from_screen_hash IN (
        SELECT screen_hash FROM screen_contexts WHERE navigation_level = 0
    )

    UNION ALL

    SELECT
        st.from_screen_hash,
        st.to_screen_hash,
        st.transition_count,
        j.depth + 1,
        j.path || ' -> ' || st.to_screen_hash
    FROM screen_transitions st
    INNER JOIN journey j ON st.from_screen_hash = j.to_screen_hash
    WHERE j.depth < 5
)
SELECT path, MAX(transition_count) as frequency
FROM journey
GROUP BY path
ORDER BY frequency DESC
LIMIT 20;
```

### B.22.2 Element Interaction Frequency

**Most frequently interacted elements:**
```sql
SELECT
    se.text,
    se.content_description,
    se.class_name,
    COUNT(ui.id) as interaction_count,
    AVG(ui.visibility_duration) as avg_visibility_ms
FROM user_interactions ui
INNER JOIN scraped_elements se ON ui.element_hash = se.element_hash
WHERE ui.interaction_time > (strftime('%s', 'now') - 86400) * 1000  -- Last 24 hours
GROUP BY ui.element_hash
ORDER BY interaction_count DESC
LIMIT 50;
```

---

## B.23 Performance Tips

### B.23.1 Index Usage

**Always use indexed columns in WHERE clauses:**
```sql
-- GOOD (uses index)
SELECT * FROM scraped_elements WHERE element_hash = 'abc123';

-- BAD (no index, full table scan)
SELECT * FROM scraped_elements WHERE text LIKE '%submit%';
```

### B.23.2 Batch Operations

**Insert multiple elements in a transaction:**
```kotlin
database.withTransaction {
    elements.forEach { element ->
        scrapedElementDao.insert(element)
    }
}
```

### B.23.3 Query Optimization

**Use Flow for reactive queries:**
```kotlin
// Reactive query - updates automatically
val appsFlow: Flow<List<AppEntity>> = appDao.getAllAppsFlow()

// One-shot query - manual refresh required
val apps: List<AppEntity> = appDao.getAllApps()
```

---

## Summary

**VOS4 Database Schema v4:**

- **Tables:** 11
- **Entities:** 11 Room entities
- **Indices:** 30+ (optimized for lookup performance)
- **Foreign Keys:** 10 (all with CASCADE delete)
- **Migrations:** 3 (v1→v2, v2→v3, v3→v4)

**Key Features:**
- Unified app metadata (LEARN_APP + DYNAMIC modes)
- Hash-based element lookups (O(1) performance)
- Cascading deletes (referential integrity)
- Feature flags (gradual rollout support)
- Screen transition tracking (navigation flow analysis)
- User interaction history (usage analytics)
- Element state tracking (state-aware commands)

---

**Version:** 4.0.0
**Last Updated:** 2025-11-02
**Status:** Complete
**Next Appendix:** [Appendix C: Troubleshooting Guide](Appendix-C-Troubleshooting.md)
