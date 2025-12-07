# Chapter 16: Database Design

**VOS4 Developer Manual**
**Version:** 1.1
**Last Updated:** 2025-11-07
**Author:** VOS4 Development Team

---

## CRITICAL UPDATE (2025-11-07)

**Database Consolidation Activated**

As of commit `19e35e0` (2025-11-07), VoiceOS v4 has consolidated all app metadata into **VoiceOSAppDatabase** as the single source of truth.

**What Changed:**
- **VoiceOSAppDatabase** is now the primary and only active database
- **LearnAppDatabase** and **AppScrapingDatabase** are legacy (kept as backup)
- One-time idempotent migration runs on first app launch after update
- All code now references VoiceOSAppDatabase for app metadata queries

**Why This Matters:**
- Single source of truth eliminates data inconsistency
- Simplified codebase (1 database API instead of 3)
- Performance improvement (single transaction for app data)
- Unified schema supports both exploration and scraping modes

**For Developers:**
- Use `VoiceOSAppDatabase.getInstance(context)` for all app metadata operations
- Old databases remain functional for backward compatibility
- See [ADR-005](../../planning/architecture/decisions/ADR-005-Database-Consolidation-Activation-2511070830.md) for full decision context
- See [Testing Guide](../../testing/Database-Consolidation-Testing-Guide.md) for validation procedures

---

## Table of Contents

1. [Overview](#overview)
2. [Database Consolidation](#database-consolidation)
3. [Database Architecture](#database-architecture)
4. [Entity Definitions](#entity-definitions)
5. [Database Schema](#database-schema)
6. [DAO Layer](#dao-layer)
7. [Foreign Key Relationships](#foreign-key-relationships)
8. [Migration Strategy](#migration-strategy)
9. [Recent Critical Fixes](#recent-critical-fixes)
10. [Performance Optimizations](#performance-optimizations)
11. [Data Retention and Cleanup](#data-retention-and-cleanup)
12. [Testing Strategy](#testing-strategy)
13. [Best Practices](#best-practices)

---

## Overview

### Purpose

The VOS4 database system is built on **Room Persistence Library** and serves as the foundation for:

1. **Accessibility Scraping**: Storing UI element data from the accessibility service
2. **Voice Command Generation**: Mapping voice commands to UI elements
3. **App Learning**: Tracking app structure and user interactions
4. **Context Awareness**: Understanding screen contexts and navigation flows
5. **User Analytics**: Recording interaction patterns for optimization

### Database Files

VOS4 uses **VoiceOSAppDatabase** as the unified database for all app metadata:

| Database | Status | Purpose | Version |
|----------|--------|---------|---------|
| **VoiceOSAppDatabase** | ✅ Active | Unified app metadata, scraping data, voice commands | 1 |
| **AppScrapingDatabase** | ⚠️ Legacy | Accessibility scraping (migrated to VoiceOS) | 9 |
| **LearnAppDatabase** | ⚠️ Legacy | App exploration metadata (migrated to VoiceOS) | - |
| **WebScrapingDatabase** | ✅ Active | Web content scraping (LearnWeb feature) | 1 |

**Migration Status (2025-11-07):**
- VoiceOSAppDatabase activated as single source of truth
- Legacy databases retained for backward compatibility
- One-time migration copies data from legacy → unified database
- See [Database Consolidation](#database-consolidation) section below

This chapter focuses primarily on **VoiceOSAppDatabase**, now the central database for all VoiceOS functionality.

### Key Design Principles

1. **Hash-Based Lookups**: Element identification via MD5 hashes for O(1) performance
2. **Foreign Key Cascading**: Automatic cleanup when parent entities are deleted
3. **Unique Constraints**: Prevent duplicate elements and relationships
4. **Indexed Columns**: Fast queries on frequently-accessed fields
5. **Nullable AI Fields**: Backward compatibility for incremental AI feature rollout
6. **Automatic Migrations**: Schema evolution without data loss

---

## Database Consolidation

### Overview

**Date:** 2025-11-07
**Commit:** 19e35e0
**ADR:** [ADR-005](../../planning/architecture/decisions/ADR-005-Database-Consolidation-Activation-2511070830.md)

VoiceOS v4.1 consolidated three separate databases into **VoiceOSAppDatabase** as the single source of truth for all app metadata.

### Pre-Consolidation Architecture (v4.0)

**Problem:** Three databases with overlapping concerns

```
┌──────────────────────┐
│  LearnAppDatabase    │  ← Full app exploration (screens, edges, elements)
│  - LearnedAppEntity  │     Generated from LearnApp LEARN_APP mode
│  - 15+ exploration   │
│    metadata fields   │
└──────────────────────┘

┌──────────────────────┐
│ AppScrapingDatabase  │  ← Dynamic scraping (elements, commands)
│  - ScrapedAppEntity  │     Generated from DYNAMIC scraping mode
│  - 8+ scraping       │
│    metadata fields   │
└──────────────────────┘

┌──────────────────────┐
│ VoiceOSAppDatabase   │  ← Unified schema (created but never used!)
│  - AppEntity         │     Designed to replace both above
│  - 25+ total fields  │     Supports BOTH modes
└──────────────────────┘
```

**Issues:**
- Data duplication (same app in multiple databases)
- Inconsistency risk (LearnApp vs Scraping metadata out of sync)
- Complex codebase (3 different DAO interfaces)
- Performance cost (multiple database transactions)

### Post-Consolidation Architecture (v4.1)

**Solution:** Activate VoiceOSAppDatabase with idempotent migration

```
┌──────────────────────────────────────────────────┐
│           VoiceOSAppDatabase (ACTIVE)            │
│                                                  │
│  AppEntity (Unified Schema)                      │
│  ┌────────────────────────────────────────────┐  │
│  │ Core Fields (all apps)                     │  │
│  │  - packageName, appId, appHash, version   │  │
│  │                                            │  │
│  │ LEARN_APP Mode Fields (exploration)       │  │
│  │  - exploredElementCount, totalScreens     │  │
│  │  - firstExplored, lastExplored            │  │
│  │  - totalEdges, rootScreenHash             │  │
│  │                                            │  │
│  │ DYNAMIC Mode Fields (scraping)            │  │
│  │  - scrapedElementCount, commandCount      │  │
│  │  - firstScraped, lastScraped              │  │
│  │  - scrapeCount, scrapingMode              │  │
│  │                                            │  │
│  │ Cross-Mode Fields                         │  │
│  │  - isFullyLearned, learnCompletedAt       │  │
│  │  - learnAppEnabled, dynamicScrapingEnabled│  │
│  └────────────────────────────────────────────┘  │
│                                                  │
│  Related Entities:                               │
│  - ScrapedElementEntity                          │
│  - GeneratedCommandEntity                        │
│  - ScreenContextEntity                           │
│  - (8 more entities for scraping data)           │
└──────────────────────────────────────────────────┘

┌──────────────────────┐  ┌──────────────────────┐
│  LearnAppDatabase    │  │ AppScrapingDatabase  │
│    (LEGACY BACKUP)   │  │   (LEGACY BACKUP)    │
│  - Retained for      │  │ - Retained for       │
│    backward compat   │  │   backward compat    │
│  - Not actively used │  │ - Not actively used  │
└──────────────────────┘  └──────────────────────┘
```

### Migration Process

**Implementation:** `DatabaseMigrationHelper.kt`

**Strategy:** One-time idempotent migration on first app launch

```kotlin
class DatabaseMigrationHelper(context: Context) {

    suspend fun migrateIfNeeded() {
        // CoT: Check if already migrated (idempotent)
        if (isMigrationComplete()) {
            Log.i(TAG, "Migration already complete, skipping")
            return
        }

        try {
            // Step 1: Migrate LearnApp data (exploration metadata)
            val learnedAppsCount = migrateLearnAppData(learnAppDb, unifiedDb)

            // Step 2: Migrate Scraping data (merge if exists)
            val scrapedAppsCount = migrateScrapingData(scrapingDb, unifiedDb)

            // Step 3: Mark complete (prevents re-migration)
            prefs.edit().putBoolean(MIGRATION_V1_COMPLETE, true).apply()

        } catch (e: Exception) {
            // CoT: Don't mark complete if failed - will retry next launch
            Log.e(TAG, "Migration failed: ${e.message}", e)
            throw e
        }
    }
}
```

**Chain of Thought - Merge Priority:**

```
ToT Analysis: Which database has priority when app exists in both?

Option A: LearnApp first, then merge Scraping
  ✅ Exploration data more complete (full app graph)
  ✅ Clear priority, easier to debug

Option B: Scraping first, then merge LearnApp
  ❌ Less complete data takes precedence

Decision: Option A - LearnApp data has priority
```

**Execution:**
1. LearnApp data migrated first (fills LEARN_APP fields)
2. Scraping data merged second (fills DYNAMIC fields)
3. If app exists in both: combine fields intelligently

### Field Mapping

**LearnedAppEntity → AppEntity:**

| LearnedApp Field | AppEntity Field | Notes |
|-----------------|-----------------|-------|
| `totalElements` | `exploredElementCount` | Renamed for clarity |
| `firstLearnedAt` | `firstExplored` | Consistency |
| `lastUpdatedAt` | `lastExplored` | Consistency |
| `N/A` (missing) | `appId` | Generated UUID |
| `versionCode` (Long) | `versionCode` (Long) | Type match ✓ |

**ScrapedAppEntity → AppEntity:**

| ScrapedApp Field | AppEntity Field | Notes |
|-----------------|-----------------|-------|
| `elementCount` | `scrapedElementCount` | Renamed for clarity |
| `versionCode` (Int) | `versionCode` (Long) | Type conversion required |
| `isFullyLearned` (Boolean) | `isFullyLearned` (Boolean?) | Nullable conversion |
| `appId` (exists) | `appId` | Direct copy ✓ |

### Usage Examples

**Before (v4.0):**

```kotlin
// Complex: need to check BOTH databases
val learnAppDb = LearnAppDatabase.getInstance(context)
val scrapingDb = AppScrapingDatabase.getInstance(context)

val learnedApp = learnAppDb.learnAppDao().getApp(packageName)
val scrapedApp = scrapingDb.scrapedAppDao().getApp(packageName)

// Manually merge data
val isLearned = learnedApp?.isFullyLearned ?: false
val elementCount = scrapedApp?.elementCount ?: 0
```

**After (v4.1):**

```kotlin
// Simple: single source of truth
val database = VoiceOSAppDatabase.getInstance(context)
val app = database.appDao().getApp(packageName)

// All data in one entity
val isLearned = app?.isFullyLearned ?: false
val exploredCount = app?.exploredElementCount ?: 0
val scrapedCount = app?.scrapedElementCount ?: 0
```

### Benefits

**1. Data Consistency**
- Single source of truth eliminates sync issues
- Atomic transactions across all app metadata
- No risk of LearnApp/Scraping disagreeing

**2. Performance**
- Single database query vs multiple database joins
- Estimated 20-30% query performance improvement
- Smaller transaction overhead

**3. Simplified Codebase**
- 1 DAO interface instead of 3
- 1 entity model instead of 3
- Easier onboarding for new developers

**4. Migration Safety**
- Idempotent (safe to retry on failure)
- Non-destructive (old databases kept as backup)
- Zero data loss risk

### Backward Compatibility

**Old Databases Retained:**
- LearnAppDatabase still exists on device
- AppScrapingDatabase still exists on device
- Can rollback by reverting code changes
- Old code that imports legacy databases still compiles

**Migration Flag:**
- Stored in SharedPreferences: `voiceos_db_migration`
- Key: `migration_v1_to_unified_complete`
- Prevents duplicate migration attempts

**Rollback Plan:**
```bash
# If issues found, revert is simple:
git revert 19e35e0
adb shell pm clear com.augmentalis.voiceos  # Clear unified DB
# Old databases resume being used automatically
```

### Testing

**Comprehensive testing guide:** [Database-Consolidation-Testing-Guide.md](../../testing/Database-Consolidation-Testing-Guide.md)

**Test Coverage:**
- Unit tests for migration logic
- Device tests for end-to-end migration
- SQL verification queries
- Performance benchmarks
- Edge case handling (empty databases, partial data, etc.)

---

## Database Architecture

### VoiceOSAppDatabase Overview

**File:** `modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/database/VoiceOSAppDatabase.kt`

**Current Version:** 1

**Status:** ✅ Active (as of v4.1, 2025-11-07)

**Purpose:** Unified database for all app metadata, supporting both exploration (LEARN_APP mode) and dynamic scraping (DYNAMIC mode)

**Entity Relationships:**

```
┌────────────────────────────────────────────────────────────────────┐
│                      AppScrapingDatabase (v9)                      │
└────────────────────────────────────────────────────────────────────┘

┌──────────────────┐
│    AppEntity     │  ← Unified app registry (v9 addition)
│  (VoiceOSApp DB) │
└────────┬─────────┘
         │
         │ Foreign Key: app_id
         │
         ├──────────────────────┬──────────────────────┐
         ▼                      ▼                      ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│ ScrapedAppEntity│    │ScrapedElementEnt│    │ScreenContextEnt │
│  (deprecated)   │    │                 │    │                 │
└─────────────────┘    └────────┬────────┘    └────────┬────────┘
                                │                       │
         ┌──────────────────────┼───────────────────────┘
         │                      │
         │                      │ Foreign Key: element_hash
         │                      │ & screen_hash
         │                      │
         ├────────┬─────────────┼──────────┬──────────────┬──────────┐
         ▼        ▼             ▼          ▼              ▼          ▼
┌───────────┐ ┌─────────┐ ┌──────────┐ ┌────────┐ ┌──────────┐ ┌─────────┐
│ Scraped   │ │Generated│ │ Element  │ │Screen  │ │  User    │ │ Element │
│ Hierarchy │ │ Command │ │Relationsh│ │Transiti│ │Interacti │ │  State  │
│           │ │         │ │  ip      │ │  on    │ │   on     │ │ History │
└───────────┘ └─────────┘ └──────────┘ └────────┘ └──────────┘ └─────────┘
```

### Database Statistics

```kotlin
data class DatabaseStats(
    val appCount: Int,              // Total learned apps
    val totalElements: Int,         // All UI elements across apps
    val totalCommands: Int,         // Generated voice commands
    val totalRelationships: Int     // Hierarchy + semantic relationships
)
```

### Singleton Pattern

```kotlin
@Volatile
private var INSTANCE: AppScrapingDatabase? = null

fun getInstance(context: Context): AppScrapingDatabase {
    return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
            context.applicationContext,
            AppScrapingDatabase::class.java,
            DATABASE_NAME
        )
            .addCallback(DatabaseCallback())
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, ..., MIGRATION_8_9)
            .build()

        INSTANCE = instance
        instance
    }
}
```

**Why Singleton?**
- Single database connection across app
- Reduced memory overhead
- Consistent transaction handling
- Simplified testing

---

## Entity Definitions

### 1. ScrapedElementEntity

**Purpose:** Stores individual UI elements discovered through accessibility service.

**File:** `entities/ScrapedElementEntity.kt`

**Table Name:** `scraped_elements`

```kotlin
@Entity(
    tableName = "scraped_elements",
    foreignKeys = [
        ForeignKey(
            entity = AppEntity::class,
            parentColumns = ["app_id"],
            childColumns = ["app_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("app_id"),
        Index(value = ["element_hash"], unique = true),
        Index("view_id_resource_name"),
        Index("uuid")
    ]
)
data class ScrapedElementEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val elementHash: String,        // MD5 hash: className + viewId + text + contentDesc
    val appId: String,              // FK to AppEntity
    val uuid: String? = null,       // Universal UUID from UUIDCreator

    // Accessibility Properties
    val className: String,          // e.g., "android.widget.Button"
    val viewIdResourceName: String?, // e.g., "com.example:id/submit_button"
    val text: String?,
    val contentDescription: String?,
    val bounds: String,             // JSON: {"left":0,"top":0,"right":100,"bottom":50}

    // Action Capabilities
    val isClickable: Boolean,
    val isLongClickable: Boolean,
    val isEditable: Boolean,
    val isScrollable: Boolean,
    val isCheckable: Boolean,
    val isFocusable: Boolean,
    val isEnabled: Boolean,

    // Hierarchy Position
    val depth: Int,
    val indexInParent: Int,

    // Metadata
    val scrapedAt: Long = System.currentTimeMillis(),

    // AI Context Inference (Phase 1)
    val semanticRole: String? = null,    // "submit_login", "input_email", "navigate_back"
    val inputType: String? = null,        // "email", "password", "phone", "text"
    val visualWeight: String? = null,     // "primary", "secondary", "tertiary", "danger"
    val isRequired: Boolean? = null,

    // AI Context Inference (Phase 2)
    val formGroupId: String? = null,      // Links related form fields
    val placeholderText: String? = null,
    val validationPattern: String? = null,
    val backgroundColor: String? = null   // Hex color for prominence detection
)
```

**Key Fields:**

- **elementHash**: Unique identifier combining className + viewId + text + contentDescription
- **uuid**: Cross-system element identification (UUIDCreator integration)
- **semanticRole**: AI-inferred purpose (e.g., "submit_login", "input_email")
- **formGroupId**: Groups related form fields together
- **bounds**: JSON string representing element coordinates

**Unique Constraint:**
- `element_hash` has a UNIQUE index to prevent duplicate elements

**Foreign Keys:**
- `app_id` → `AppEntity.app_id` (CASCADE on delete)

### 2. ScrapedHierarchyEntity

**Purpose:** Captures parent-child relationships in the accessibility tree.

**File:** `entities/ScrapedHierarchyEntity.kt`

**Table Name:** `scraped_hierarchy`

```kotlin
@Entity(
    tableName = "scraped_hierarchy",
    foreignKeys = [
        ForeignKey(
            entity = ScrapedElementEntity::class,
            parentColumns = ["id"],
            childColumns = ["parent_element_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ScrapedElementEntity::class,
            parentColumns = ["id"],
            childColumns = ["child_element_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("parent_element_id"),
        Index("child_element_id")
    ]
)
data class ScrapedHierarchyEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val parentElementId: Long,   // FK to ScrapedElementEntity.id
    val childElementId: Long,    // FK to ScrapedElementEntity.id
    val childOrder: Int,         // Order among siblings
    val depth: Int = 1           // Depth difference (usually 1)
)
```

**Why Long IDs Instead of element_hash?**

See: `/coding/DECISIONS/ScrapedHierarchy-Migration-Analysis-251010-0220.md`

**Reasons:**
1. **Performance**: Integer FK lookups are ~30% faster than string comparisons
2. **Storage**: Long uses 8 bytes vs ~32 bytes for MD5 hash string
3. **Hierarchy Queries**: Tree traversals benefit from integer indices
4. **Memory**: Reduced memory footprint for large hierarchy datasets

**Trade-off:** Requires deleting hierarchy before replacing elements (see recent fixes).

### 3. ScreenContextEntity

**Purpose:** Screen-level context for AI understanding of navigation flows.

**File:** `entities/ScreenContextEntity.kt`

**Table Name:** `screen_contexts`

```kotlin
@Entity(
    tableName = "screen_contexts",
    foreignKeys = [
        ForeignKey(
            entity = AppEntity::class,
            parentColumns = ["app_id"],
            childColumns = ["app_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["screen_hash"], unique = true),
        Index("app_id"),
        Index("package_name"),
        Index("screen_type")
    ]
)
data class ScreenContextEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val screenHash: String,          // UNIQUE: MD5(package + class + title + contentFingerprint)
    val appId: String,               // FK to AppEntity
    val packageName: String,
    val activityName: String?,
    val windowTitle: String?,

    // AI Context (Phase 2)
    val screenType: String?,         // "login", "checkout", "settings", "home", "form"
    val formContext: String?,        // "registration", "payment", "search"
    val navigationLevel: Int = 0,    // Depth in navigation (0 = main, 1+ = nested)
    val primaryAction: String?,      // "submit", "search", "browse"

    // Screen Metrics
    val elementCount: Int = 0,
    val hasBackButton: Boolean = false,

    // Timestamps
    val firstScraped: Long = System.currentTimeMillis(),
    val lastScraped: Long = System.currentTimeMillis(),
    val visitCount: Int = 1
)
```

**Screen Hash Algorithm (FIXED in v9):**

**Old (Broken):**
```kotlin
screenHash = MD5(packageName + className + windowTitle)
// Problem: Empty windowTitle caused duplicate hashes
```

**New (Fixed):**
```kotlin
val contentFingerprint = elements
    .filter { !it.className.contains("DecorView") && !it.className.contains("Layout") }
    .sortedBy { it.depth }
    .take(10)  // Top 10 significant elements
    .joinToString("|") { e ->
        "${e.className}:${e.text ?: ""}:${e.contentDescription ?: ""}:${e.isClickable}"
    }

screenHash = MD5(packageName + className + windowTitle + contentFingerprint)
```

**Why Content Fingerprint?**
- Most Android windows have **empty windowTitle**
- Different screens in same activity now have unique hashes
- Revisiting same screen correctly increments `visitCount`

See: `docs/modules/VoiceOSCore/fixes/Fix-FK-Constraint-And-Screen-Duplication-251031.md`

### 4. GeneratedCommandEntity

**Purpose:** Voice commands automatically generated from UI elements.

**File:** `entities/GeneratedCommandEntity.kt`

**Table Name:** `generated_commands`

```kotlin
@Entity(
    tableName = "generated_commands",
    foreignKeys = [
        ForeignKey(
            entity = ScrapedElementEntity::class,
            parentColumns = ["element_hash"],  // String FK (changed in v2)
            childColumns = ["element_hash"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("element_hash"),
        Index("command_text"),
        Index("action_type")
    ]
)
data class GeneratedCommandEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val elementHash: String,        // FK to ScrapedElementEntity.element_hash
    val commandText: String,        // "click submit button"
    val actionType: String,         // "click", "long_click", "type", "scroll", "focus"
    val confidence: Float,          // 0.0-1.0
    val synonyms: String,           // JSON: ["send", "post", "submit"]

    val isUserApproved: Boolean = false,
    val usageCount: Int = 0,
    val lastUsed: Long? = null,
    val generatedAt: Long = System.currentTimeMillis()
)
```

**Migration Note (v1 → v2):**

Originally used `element_id: Long` FK. Migrated to `element_hash: String` for consistency.

**Why element_hash FK?**
- Commands reference elements semantically, not by temporary ID
- Element IDs change when replaced (OnConflictStrategy.REPLACE)
- Hash-based FK survives element updates

### 5. ScrapedAppEntity (Deprecated)

**Purpose:** App metadata (being phased out in favor of unified AppEntity).

**File:** `entities/ScrapedAppEntity.kt`

**Table Name:** `scraped_apps`

```kotlin
@Entity(tableName = "scraped_apps")
data class ScrapedAppEntity(
    @PrimaryKey
    val appId: String,              // UUID for this scraping session

    val packageName: String,
    val appName: String,
    val versionCode: Int,
    val versionName: String,
    val appHash: String,            // MD5(packageName + versionCode)

    val firstScraped: Long,
    val lastScraped: Long,
    val scrapeCount: Int = 1,
    val elementCount: Int = 0,
    val commandCount: Int = 0,

    // LearnApp Mode Tracking (v3 addition)
    val isFullyLearned: Boolean = false,
    val learnCompletedAt: Long? = null,
    val scrapingMode: String = "DYNAMIC"  // "DYNAMIC" or "LEARN_APP"
)
```

**Deprecation Plan:**

v9 introduced unified **AppEntity** in VoiceOSAppDatabase. ScrapedAppEntity kept for backward compatibility during migration.

**Future:** All FK references will use AppEntity.app_id.

### 6. ElementRelationshipEntity

**Purpose:** Semantic relationships between elements (e.g., "button submits form").

**File:** `entities/ElementRelationshipEntity.kt`

**Table Name:** `element_relationships`

```kotlin
@Entity(
    tableName = "element_relationships",
    foreignKeys = [
        ForeignKey(
            entity = ScrapedElementEntity::class,
            parentColumns = ["element_hash"],
            childColumns = ["source_element_hash"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ScrapedElementEntity::class,
            parentColumns = ["element_hash"],
            childColumns = ["target_element_hash"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("source_element_hash"),
        Index("target_element_hash"),
        Index("relationship_type"),
        Index(value = ["source_element_hash", "target_element_hash", "relationship_type"],
              unique = true)
    ]
)
data class ElementRelationshipEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val sourceElementHash: String,
    val targetElementHash: String?,
    val relationshipType: String,    // See RelationshipType constants
    val relationshipData: String? = null,  // Additional JSON data
    val confidence: Float = 1.0f,
    val inferredBy: String = "accessibility_tree",
    val createdAt: Long = System.currentTimeMillis()
)
```

**Relationship Types:**

```kotlin
object RelationshipType {
    // Form relationships
    const val FORM_GROUP_MEMBER = "form_group_member"
    const val BUTTON_SUBMITS_FORM = "button_submits_form"
    const val LABEL_FOR = "label_for"
    const val ERROR_FOR = "error_for"

    // Navigation relationships
    const val NAVIGATES_TO = "navigates_to"
    const val BACK_TO = "back_to"

    // Content relationships
    const val DESCRIBES = "describes"
    const val CONTAINS = "contains"
    const val EXPANDS_TO = "expands_to"

    // Action relationships
    const val TRIGGERS = "triggers"
    const val TOGGLES = "toggles"
    const val FILTERS = "filters"
}
```

**Unique Constraint:**
Prevents duplicate relationships: (source, target, type) must be unique.

### 7. ScreenTransitionEntity

**Purpose:** Navigation flow tracking for user journey analysis.

**File:** `entities/ScreenTransitionEntity.kt`

**Table Name:** `screen_transitions`

```kotlin
@Entity(
    tableName = "screen_transitions",
    foreignKeys = [
        ForeignKey(
            entity = ScreenContextEntity::class,
            parentColumns = ["screen_hash"],
            childColumns = ["from_screen_hash"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ScreenContextEntity::class,
            parentColumns = ["screen_hash"],
            childColumns = ["to_screen_hash"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("from_screen_hash"),
        Index("to_screen_hash"),
        Index(value = ["from_screen_hash", "to_screen_hash"], unique = true)
    ]
)
data class ScreenTransitionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val fromScreenHash: String,      // FK to ScreenContextEntity.screen_hash
    val toScreenHash: String,        // FK to ScreenContextEntity.screen_hash
    val transitionCount: Int = 1,
    val firstTransition: Long = System.currentTimeMillis(),
    val lastTransition: Long = System.currentTimeMillis(),
    val avgTransitionTime: Long? = null
)
```

**Use Cases:**
- User journey visualization
- Common navigation path detection
- App flow understanding for voice navigation

### 8. UserInteractionEntity

**Purpose:** Track user interactions for confidence scoring and analytics.

**File:** `entities/UserInteractionEntity.kt`

**Table Name:** `user_interactions`

```kotlin
@Entity(
    tableName = "user_interactions",
    foreignKeys = [
        ForeignKey(
            entity = ScrapedElementEntity::class,
            parentColumns = ["element_hash"],
            childColumns = ["element_hash"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ScreenContextEntity::class,
            parentColumns = ["screen_hash"],
            childColumns = ["screen_hash"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("element_hash"),
        Index("screen_hash"),
        Index("interaction_type"),
        Index("interaction_time")
    ]
)
data class UserInteractionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val elementHash: String,
    val screenHash: String,
    val interactionType: String,     // See InteractionType constants
    val interactionTime: Long = System.currentTimeMillis(),
    val visibilityStart: Long? = null,
    val visibilityDuration: Long? = null,
    val success: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
```

**Interaction Types:**

```kotlin
object InteractionType {
    const val CLICK = "click"
    const val LONG_PRESS = "long_press"
    const val SWIPE = "swipe"
    const val FOCUS = "focus"
    const val SCROLL = "scroll"
    const val DOUBLE_TAP = "double_tap"
    const val VOICE_COMMAND = "voice_command"
}
```

### 9. ElementStateHistoryEntity

**Purpose:** Track element state changes for state-aware voice commands.

**File:** `entities/ElementStateHistoryEntity.kt`

**Table Name:** `element_state_history`

```kotlin
@Entity(
    tableName = "element_state_history",
    foreignKeys = [
        ForeignKey(
            entity = ScrapedElementEntity::class,
            parentColumns = ["element_hash"],
            childColumns = ["element_hash"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ScreenContextEntity::class,
            parentColumns = ["screen_hash"],
            childColumns = ["screen_hash"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("element_hash"),
        Index("screen_hash"),
        Index("state_type"),
        Index("changed_at")
    ]
)
data class ElementStateHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val elementHash: String,
    val screenHash: String,
    val stateType: String,           // "checked", "enabled", "focused", "text"
    val oldValue: String?,
    val newValue: String?,
    val changedAt: Long = System.currentTimeMillis(),
    val triggeredBy: String?         // What caused the change
)
```

**Use Cases:**
- State-aware commands ("check" vs "uncheck")
- Element behavior learning
- Interaction pattern analysis

---

## Database Schema

### Complete Schema Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          VOS4 Database Schema                           │
└─────────────────────────────────────────────────────────────────────────┘

┌───────────────────────────────────────────────────────────────────────┐
│ AppEntity (VoiceOSAppDatabase)                                        │
├───────────────────────────────────────────────────────────────────────┤
│ PK: app_id (String - UUID)                                            │
│     package_name (String)                                             │
│     app_name (String)                                                 │
│     version_code (Int)                                                │
└─────────────────┬─────────────────────────────────────────────────────┘
                  │
                  │ CASCADE DELETE
                  │
    ┌─────────────┼─────────────┐
    │             │             │
    ▼             ▼             ▼
┌────────────┐ ┌────────────┐ ┌─────────────────┐
│ Scraped    │ │  Scraped   │ │ Screen          │
│ Element    │ │  App       │ │ Context         │
│ (Main)     │ │ (Deprecate)│ │                 │
└──┬─────────┘ └────────────┘ └────┬────────────┘
   │                               │
   │ element_hash (UNIQUE)         │ screen_hash (UNIQUE)
   │                               │
   ├───────────┬──────────┬────────┼──────────┬──────────┬──────────┐
   │           │          │        │          │          │          │
   ▼           ▼          ▼        ▼          ▼          ▼          ▼
┌──────┐  ┌────────┐ ┌────────┐ ┌────────┐ ┌──────┐ ┌──────┐ ┌──────┐
│Hierar│  │Command │ │Element │ │Screen  │ │User  │ │Eleme │ │      │
│chy   │  │        │ │Relatio │ │Transit │ │Intera│ │State │ │ ...  │
│(Long │  │        │ │nship   │ │ion     │ │ction │ │Histor│ │      │
│  FK) │  │        │ │        │ │        │ │      │ │y     │ │      │
└──────┘  └────────┘ └────────┘ └────────┘ └──────┘ └──────┘ └──────┘
```

### Foreign Key Cascade Behavior

```
DELETE AppEntity
    ├─→ CASCADE DELETE ScrapedElementEntity
    │       ├─→ CASCADE DELETE ScrapedHierarchyEntity
    │       ├─→ CASCADE DELETE GeneratedCommandEntity
    │       ├─→ CASCADE DELETE ElementRelationshipEntity
    │       ├─→ CASCADE DELETE UserInteractionEntity
    │       └─→ CASCADE DELETE ElementStateHistoryEntity
    │
    └─→ CASCADE DELETE ScreenContextEntity
            ├─→ CASCADE DELETE ScreenTransitionEntity
            ├─→ CASCADE DELETE UserInteractionEntity
            └─→ CASCADE DELETE ElementStateHistoryEntity
```

**Benefit:** Deleting an app automatically cleans up all related data.

### Index Strategy

**Why Indexes?**
- Speed up query performance (O(log n) vs O(n) for unindexed)
- Enable unique constraints
- Optimize JOIN operations

**Index Rules:**
1. **Primary Keys**: Auto-indexed
2. **Foreign Keys**: Always indexed for JOIN performance
3. **Unique Constraints**: Require unique index
4. **Query Columns**: Index frequently-queried columns
5. **Avoid Over-Indexing**: Slows down INSERT/UPDATE operations

**Current Indexes:**

| Table | Index | Type | Purpose |
|-------|-------|------|---------|
| scraped_elements | element_hash | UNIQUE | Prevent duplicate elements |
| scraped_elements | app_id | INDEX | Fast app-based queries |
| scraped_elements | view_id_resource_name | INDEX | Resource ID lookups |
| scraped_elements | uuid | INDEX | Cross-system identification |
| scraped_hierarchy | parent_element_id | INDEX | Parent-child queries |
| scraped_hierarchy | child_element_id | INDEX | Reverse hierarchy traversal |
| generated_commands | element_hash | INDEX | Command-element lookups |
| generated_commands | command_text | INDEX | Voice command matching |
| generated_commands | action_type | INDEX | Action filtering |
| screen_contexts | screen_hash | UNIQUE | Prevent duplicate screens |
| screen_contexts | app_id | INDEX | App-screen queries |
| screen_contexts | package_name | INDEX | Package-based filtering |
| screen_contexts | screen_type | INDEX | Screen type queries |
| element_relationships | source_element_hash | INDEX | Relationship queries |
| element_relationships | target_element_hash | INDEX | Reverse relationships |
| element_relationships | relationship_type | INDEX | Type filtering |
| element_relationships | (source, target, type) | UNIQUE | Prevent duplicates |
| screen_transitions | from_screen_hash | INDEX | Navigation flow queries |
| screen_transitions | to_screen_hash | INDEX | Reverse navigation |
| screen_transitions | (from, to) | UNIQUE | Prevent duplicate transitions |
| user_interactions | element_hash | INDEX | Element interaction history |
| user_interactions | screen_hash | INDEX | Screen interaction queries |
| user_interactions | interaction_type | INDEX | Interaction filtering |
| user_interactions | interaction_time | INDEX | Time-based queries |
| element_state_history | element_hash | INDEX | State history lookups |
| element_state_history | screen_hash | INDEX | Screen state queries |
| element_state_history | state_type | INDEX | State type filtering |
| element_state_history | changed_at | INDEX | Time-based queries |

---

## DAO Layer

### DAO Architecture

**DAO (Data Access Object)** pattern provides abstraction layer between business logic and database.

**Benefits:**
- Clean separation of concerns
- Testable database operations
- Type-safe queries
- Suspend function support for coroutines

### ScrapedElementDao

**File:** `dao/ScrapedElementDao.kt`

```kotlin
@Dao
interface ScrapedElementDao {

    // INSERT operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatchWithIds(elements: List<ScrapedElementEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(element: ScrapedElementEntity): Long

    // QUERY operations
    @Query("SELECT * FROM scraped_elements WHERE app_id = :appId")
    suspend fun getElementsByAppId(appId: String): List<ScrapedElementEntity>

    @Query("SELECT * FROM scraped_elements WHERE element_hash = :elementHash LIMIT 1")
    suspend fun getElementByHash(elementHash: String): ScrapedElementEntity?

    @Query("SELECT * FROM scraped_elements WHERE uuid = :uuid LIMIT 1")
    suspend fun getElementByUuid(uuid: String): ScrapedElementEntity?

    @Query("SELECT * FROM scraped_elements WHERE is_clickable = 1 AND app_id = :appId")
    suspend fun getClickableElements(appId: String): List<ScrapedElementEntity>

    @Query("SELECT * FROM scraped_elements WHERE is_editable = 1 AND app_id = :appId")
    suspend fun getEditableElements(appId: String): List<ScrapedElementEntity>

    @Query("""
        SELECT * FROM scraped_elements
        WHERE semantic_role IS NOT NULL
        AND app_id = :appId
    """)
    suspend fun getElementsWithSemanticRole(appId: String): List<ScrapedElementEntity>

    // UPDATE operations
    @Update
    suspend fun update(element: ScrapedElementEntity)

    @Query("""
        UPDATE scraped_elements
        SET semantic_role = :semanticRole
        WHERE element_hash = :elementHash
    """)
    suspend fun updateSemanticRole(elementHash: String, semanticRole: String)

    // DELETE operations
    @Delete
    suspend fun delete(element: ScrapedElementEntity)

    @Query("DELETE FROM scraped_elements WHERE app_id = :appId")
    suspend fun deleteByAppId(appId: String): Int

    // COUNT operations
    @Query("SELECT COUNT(*) FROM scraped_elements WHERE app_id = :appId")
    suspend fun getElementCount(appId: String): Int
}
```

**OnConflictStrategy.REPLACE:**

When inserting element with existing `element_hash`:
1. Delete existing row
2. Insert new row with **NEW auto-generated ID**
3. Old ID becomes invalid

**Critical:** This behavior caused FK constraint violations (see fixes section).

### ScrapedHierarchyDao

**File:** `dao/ScrapedHierarchyDao.kt`

```kotlin
@Dao
interface ScrapedHierarchyDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatch(hierarchies: List<ScrapedHierarchyEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(hierarchy: ScrapedHierarchyEntity): Long

    // Get children of a parent
    @Query("""
        SELECT se.* FROM scraped_elements se
        INNER JOIN scraped_hierarchy sh ON se.id = sh.child_element_id
        WHERE sh.parent_element_id = :parentElementId
        ORDER BY sh.child_order
    """)
    suspend fun getChildElements(parentElementId: Long): List<ScrapedElementEntity>

    // Get parent of a child
    @Query("""
        SELECT se.* FROM scraped_elements se
        INNER JOIN scraped_hierarchy sh ON se.id = sh.parent_element_id
        WHERE sh.child_element_id = :childElementId
        LIMIT 1
    """)
    suspend fun getParentElement(childElementId: Long): ScrapedElementEntity?

    // CRITICAL: Delete hierarchy for app before replacing elements
    @Query("""
        DELETE FROM scraped_hierarchy
        WHERE parent_element_id IN (
            SELECT id FROM scraped_elements WHERE app_id = :appId
        )
    """)
    suspend fun deleteHierarchyForApp(appId: String): Int

    @Query("SELECT COUNT(*) FROM scraped_hierarchy")
    suspend fun getRelationshipCount(): Int

    @Query("DELETE FROM scraped_hierarchy")
    suspend fun deleteAll()
}
```

**CRITICAL METHOD:** `deleteHierarchyForApp()`

**Purpose:** Delete all hierarchy records for an app BEFORE replacing elements.

**Why?** When elements are replaced (REPLACE strategy), they get new IDs, orphaning old hierarchy records and causing FK constraint violations.

**See:** `docs/modules/VoiceOSCore/fixes/Fix-FK-Constraint-And-Screen-Duplication-251031.md`

### GeneratedCommandDao

**File:** `dao/GeneratedCommandDao.kt`

```kotlin
@Dao
interface GeneratedCommandDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(command: GeneratedCommandEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatch(commands: List<GeneratedCommandEntity>): List<Long>

    // Query commands by element
    @Query("SELECT * FROM generated_commands WHERE element_hash = :elementHash")
    suspend fun getCommandsByElementHash(elementHash: String): List<GeneratedCommandEntity>

    // Query commands by text
    @Query("""
        SELECT * FROM generated_commands
        WHERE command_text LIKE :commandText || '%'
        ORDER BY confidence DESC, usage_count DESC
    """)
    suspend fun findCommandsByText(commandText: String): List<GeneratedCommandEntity>

    // Get all commands with package name (JOIN)
    @Query("""
        SELECT gc.*, se.app_id as packageName
        FROM generated_commands gc
        INNER JOIN scraped_elements se ON gc.element_hash = se.element_hash
        WHERE se.app_id = :appId
    """)
    suspend fun getCommandsWithPackageName(appId: String): List<GeneratedCommandWithPackageName>

    // Update usage statistics
    @Query("""
        UPDATE generated_commands
        SET usage_count = usage_count + 1, last_used = :timestamp
        WHERE id = :commandId
    """)
    suspend fun incrementUsageCount(commandId: Long, timestamp: Long = System.currentTimeMillis())

    // Delete low-quality commands (cleanup)
    @Query("""
        DELETE FROM generated_commands
        WHERE usage_count = 0
        AND confidence < :threshold
        AND is_user_approved = 0
    """)
    suspend fun deleteLowQualityCommands(threshold: Float): Int

    @Delete
    suspend fun delete(command: GeneratedCommandEntity)
}
```

**Data Class for JOIN Query:**

```kotlin
data class GeneratedCommandWithPackageName(
    @Embedded val command: GeneratedCommandEntity,
    val packageName: String
)
```

### ScreenContextDao

**File:** `dao/ScreenContextDao.kt`

```kotlin
@Dao
interface ScreenContextDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(screenContext: ScreenContextEntity): Long

    @Query("SELECT * FROM screen_contexts WHERE screen_hash = :screenHash LIMIT 1")
    suspend fun getByScreenHash(screenHash: String): ScreenContextEntity?

    @Query("SELECT * FROM screen_contexts WHERE app_id = :appId")
    suspend fun getScreensByApp(appId: String): List<ScreenContextEntity>

    // Increment visit count
    @Query("""
        UPDATE screen_contexts
        SET visit_count = visit_count + 1,
            last_scraped = :timestamp
        WHERE screen_hash = :screenHash
    """)
    suspend fun incrementVisitCount(screenHash: String, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM screen_contexts WHERE app_id = :appId")
    suspend fun getScreenCount(appId: String): Int

    @Delete
    suspend fun delete(screenContext: ScreenContextEntity)
}
```

### Other DAOs

**ElementRelationshipDao**, **ScreenTransitionDao**, **UserInteractionDao**, **ElementStateHistoryDao** follow similar patterns with:
- Insert/query/update/delete methods
- Suspend functions for coroutines
- Type-safe SQL queries
- Appropriate conflict strategies

---

## Foreign Key Relationships

### Relationship Graph

```
AppEntity (app_id - String UUID)
    │
    ├─→ ScrapedElementEntity (app_id FK)
    │       │
    │       ├─→ ScrapedHierarchyEntity (parent_element_id, child_element_id FK - LONG IDs)
    │       ├─→ GeneratedCommandEntity (element_hash FK - STRING)
    │       ├─→ ElementRelationshipEntity (source/target_element_hash FK - STRING)
    │       ├─→ UserInteractionEntity (element_hash FK - STRING)
    │       └─→ ElementStateHistoryEntity (element_hash FK - STRING)
    │
    └─→ ScreenContextEntity (app_id FK)
            │
            ├─→ ScreenTransitionEntity (from/to_screen_hash FK - STRING)
            ├─→ UserInteractionEntity (screen_hash FK - STRING)
            └─→ ElementStateHistoryEntity (screen_hash FK - STRING)
```

### FK Design Decisions

**1. Why element_hash (String) FK for Commands?**

Commands reference elements **semantically**, not by temporary ID.

**Problem with Long ID FK:**
```kotlin
// First scrape: Element inserted with id=100
val element = ScrapedElementEntity(id=100, elementHash="abc123", ...)
database.insert(element)

// Command created with element_id=100
val command = GeneratedCommandEntity(elementId=100, ...)
database.insert(command)

// Second scrape: Same element, but REPLACE strategy assigns NEW id=200
val updatedElement = ScrapedElementEntity(id=200, elementHash="abc123", ...)
database.insert(updatedElement)  // Old id=100 deleted, new id=200 created

// Command still references old id=100 → FK CONSTRAINT VIOLATION
```

**Solution:** Use `element_hash` FK instead of ID.
- Hash remains constant across scrapes
- Commands survive element updates

**2. Why Long ID FK for Hierarchy?**

Hierarchy represents **tree structure**, optimized with integer IDs.

**Benefits:**
- 30% faster JOIN queries
- Smaller storage (8 bytes vs 32 bytes)
- Better cache locality

**Trade-off:** Must delete hierarchy before replacing elements.

```kotlin
// CORRECT ORDER (post-fix):
database.scrapedHierarchyDao().deleteHierarchyForApp(appId)  // Step 1
val ids = database.scrapedElementDao().insertBatchWithIds(elements)  // Step 2
database.scrapedHierarchyDao().insertBatch(hierarchy)  // Step 3
```

---

## Migration Strategy

### Database Consolidation Migration (v4.1)

**CRITICAL:** As of v4.1 (2025-11-07), there are TWO types of migrations:

1. **Schema Migrations** (Room migrations v1→v2→...→v9)
   - Handled automatically by Room framework
   - Modifies table structure within a single database
   - See "Schema Migration History" below

2. **Database Consolidation Migration** (One-time, v4.0 → v4.1)
   - Migrates data FROM LearnAppDatabase and AppScrapingDatabase TO VoiceOSAppDatabase
   - Handled by `DatabaseMigrationHelper.kt`
   - Runs once on first app launch after v4.1 update
   - Idempotent (safe to retry on failure)
   - See [Database Consolidation](#database-consolidation) section above

**Implementation:**

```kotlin
// In VoiceOSService.onCreate()
serviceScope.launch {
    try {
        val migrationHelper = DatabaseMigrationHelper(this@VoiceOSService)
        migrationHelper.migrateIfNeeded()  // Idempotent check inside
    } catch (e: Exception) {
        Log.e(TAG, "Database consolidation migration failed (will retry): ${e.message}", e)
    }
}
```

**Migration Status Check:**

```kotlin
val helper = DatabaseMigrationHelper(context)
if (helper.isMigrationComplete()) {
    // VoiceOSAppDatabase is populated and ready
} else {
    // Migration pending or failed, will retry on next launch
}
```

### Schema Migration History (AppScrapingDatabase Legacy)

**Note:** These schema migrations apply to the **legacy AppScrapingDatabase** (v1→v9). VoiceOSAppDatabase started at v1 with the consolidated schema.

| Version | Changes | Migration Script |
|---------|---------|------------------|
| 1 → 2 | Added unique constraint to element_hash, migrated commands from Long to String FK | MIGRATION_1_2 |
| 2 → 3 | Added LearnApp tracking fields (isFullyLearned, learnCompletedAt, scrapingMode) | MIGRATION_2_3 |
| 3 → 4 | Added uuid column to scraped_elements (UUIDCreator integration) | MIGRATION_3_4 |
| 4 → 5 | Added AI context fields (semanticRole, inputType, visualWeight, isRequired) | MIGRATION_4_5 |
| 5 → 6 | Added Phase 2 AI fields, screen_contexts, element_relationships tables | MIGRATION_5_6 |
| 6 → 7 | Added screen_transitions table | MIGRATION_6_7 |
| 7 → 8 | Added user_interactions and element_state_history tables | MIGRATION_7_8 |
| 8 → 9 | Added AppEntity reference (unified app registry) | MIGRATION_8_9 |

**VoiceOSAppDatabase Schema:** Version 1 (started with consolidated schema from Phase 3A, October 2025)

### Migration Example: v1 → v2

**Goal:** Change generated_commands FK from `element_id: Long` to `element_hash: String`

```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        android.util.Log.i("AppScrapingDatabase", "Starting migration 1 → 2")

        try {
            // STEP 1: Add unique constraint to element_hash
            db.execSQL("DROP INDEX IF EXISTS index_scraped_elements_element_hash")
            db.execSQL(
                "CREATE UNIQUE INDEX index_scraped_elements_element_hash " +
                "ON scraped_elements(element_hash)"
            )

            // STEP 2: Create new generated_commands table with element_hash FK
            db.execSQL("""
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
                    FOREIGN KEY(element_hash)
                        REFERENCES scraped_elements(element_hash)
                        ON DELETE CASCADE
                )
            """)

            // STEP 3: Migrate existing data (join to get element_hash)
            db.execSQL("""
                INSERT INTO generated_commands_new
                (id, element_hash, command_text, action_type, confidence, synonyms,
                 is_user_approved, usage_count, last_used, generated_at)
                SELECT
                    gc.id,
                    se.element_hash,      ← JOIN to get hash from ID
                    gc.command_text,
                    gc.action_type,
                    gc.confidence,
                    gc.synonyms,
                    gc.is_user_approved,
                    gc.usage_count,
                    gc.last_used,
                    gc.generated_at
                FROM generated_commands gc
                INNER JOIN scraped_elements se ON gc.element_id = se.id
            """)

            // STEP 4: Drop old table and rename new table
            db.execSQL("DROP TABLE generated_commands")
            db.execSQL("ALTER TABLE generated_commands_new RENAME TO generated_commands")

            // STEP 5: Create indexes on new table
            db.execSQL(
                "CREATE INDEX index_generated_commands_element_hash " +
                "ON generated_commands(element_hash)"
            )
            db.execSQL(
                "CREATE INDEX index_generated_commands_command_text " +
                "ON generated_commands(command_text)"
            )
            db.execSQL(
                "CREATE INDEX index_generated_commands_action_type " +
                "ON generated_commands(action_type)"
            )

            android.util.Log.i("AppScrapingDatabase", "Migration 1 → 2 completed successfully")

        } catch (e: Exception) {
            android.util.Log.e("AppScrapingDatabase", "Migration 1 → 2 failed", e)
            throw e
        }
    }
}
```

**Migration Safety:**
- Wrapped in try-catch
- Logs every step
- Uses transactions (implicit in Room)
- Verifies data migration count

### Adding New Columns (Simple Migration)

**Example:** v3 → v4 (Add uuid column)

```kotlin
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        android.util.Log.i("AppScrapingDatabase", "Starting migration 3 → 4")

        try {
            // Add uuid column (nullable for backward compatibility)
            db.execSQL(
                "ALTER TABLE scraped_elements ADD COLUMN uuid TEXT"
            )

            // Create index on uuid
            db.execSQL(
                "CREATE INDEX index_scraped_elements_uuid ON scraped_elements(uuid)"
            )

            android.util.Log.i("AppScrapingDatabase", "Migration 3 → 4 completed")

        } catch (e: Exception) {
            android.util.Log.e("AppScrapingDatabase", "Migration 3 → 4 failed", e)
            throw e
        }
    }
}
```

**Why Nullable?**
- Existing elements won't have UUIDs yet
- New elements will populate UUID field on next scrape
- No need to backfill existing data

### Creating New Tables (Complex Migration)

**Example:** v5 → v6 (Add screen_contexts and element_relationships tables)

```kotlin
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        android.util.Log.i("AppScrapingDatabase", "Starting migration 5 → 6")

        try {
            // STEP 1: Add Phase 2 fields to scraped_elements
            db.execSQL("ALTER TABLE scraped_elements ADD COLUMN form_group_id TEXT")
            db.execSQL("ALTER TABLE scraped_elements ADD COLUMN placeholder_text TEXT")
            db.execSQL("ALTER TABLE scraped_elements ADD COLUMN validation_pattern TEXT")
            db.execSQL("ALTER TABLE scraped_elements ADD COLUMN background_color TEXT")

            // STEP 2: Create screen_contexts table
            db.execSQL("""
                CREATE TABLE screen_contexts (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    screen_hash TEXT NOT NULL,
                    app_id TEXT NOT NULL,
                    package_name TEXT NOT NULL,
                    activity_name TEXT,
                    window_title TEXT,
                    screen_type TEXT,
                    form_context TEXT,
                    navigation_level INTEGER NOT NULL DEFAULT 0,
                    primary_action TEXT,
                    element_count INTEGER NOT NULL DEFAULT 0,
                    has_back_button INTEGER NOT NULL DEFAULT 0,
                    first_scraped INTEGER NOT NULL,
                    last_scraped INTEGER NOT NULL,
                    visit_count INTEGER NOT NULL DEFAULT 1,
                    FOREIGN KEY(app_id) REFERENCES scraped_apps(app_id) ON DELETE CASCADE
                )
            """)

            // STEP 3: Create indices for screen_contexts
            db.execSQL("CREATE UNIQUE INDEX index_screen_contexts_screen_hash ON screen_contexts(screen_hash)")
            db.execSQL("CREATE INDEX index_screen_contexts_app_id ON screen_contexts(app_id)")
            db.execSQL("CREATE INDEX index_screen_contexts_package_name ON screen_contexts(package_name)")
            db.execSQL("CREATE INDEX index_screen_contexts_screen_type ON screen_contexts(screen_type)")

            // STEP 4: Create element_relationships table
            db.execSQL("""
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
                )
            """)

            // STEP 5: Create indices for element_relationships
            db.execSQL("CREATE INDEX index_element_relationships_source_element_hash ON element_relationships(source_element_hash)")
            db.execSQL("CREATE INDEX index_element_relationships_target_element_hash ON element_relationships(target_element_hash)")
            db.execSQL("CREATE INDEX index_element_relationships_relationship_type ON element_relationships(relationship_type)")
            db.execSQL("CREATE UNIQUE INDEX index_element_relationships_unique ON element_relationships(source_element_hash, target_element_hash, relationship_type)")

            android.util.Log.i("AppScrapingDatabase", "Migration 5 → 6 completed")

        } catch (e: Exception) {
            android.util.Log.e("AppScrapingDatabase", "Migration 5 → 6 failed", e)
            throw e
        }
    }
}
```

### Migration Best Practices

1. **Always increment version number** in @Database annotation
2. **Add migration to database builder** via `.addMigrations()`
3. **Test migrations** with existing data
4. **Use nullable columns** for backward compatibility
5. **Log every step** for debugging
6. **Wrap in try-catch** to prevent app crashes
7. **Verify data integrity** after migration
8. **Export schema** for migration testing

**Schema Export:**

```kotlin
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}
```

Generated schemas allow Room to validate migrations.

---

## Recent Critical Fixes

### Fix 1: FK Constraint Violation (October 31, 2025)

**File:** `docs/modules/VoiceOSCore/fixes/Fix-FK-Constraint-And-Screen-Duplication-251031.md`

#### Problem

Application crashed with:
```
android.database.sqlite.SQLiteConstraintException:
FOREIGN KEY constraint failed (code 787 SQLITE_CONSTRAINT_FOREIGNKEY)
at ScrapedHierarchyDao.insertBatch()
```

#### Root Cause

**Flow (BROKEN):**

```
1. First Scrape:
   - Insert elements → IDs: [100, 101, 102]
   - Insert hierarchy → Parent:100, Children:[101, 102]
   ✅ Success

2. Second Scrape (same app):
   - Insert elements with REPLACE strategy
     → Old IDs [100, 101, 102] DELETED
     → New IDs [200, 201, 202] CREATED

   - Old hierarchy still exists:
     Parent: 100 (DELETED), Children: [101, 102] (DELETED)

   - Try to insert new hierarchy:
     Parent: 200, Children: [201, 202]

   ❌ FK CONSTRAINT VIOLATION
   (Old hierarchy references deleted element IDs)
```

#### Solution

**Delete hierarchy BEFORE replacing elements:**

```kotlin
// File: AccessibilityScrapingIntegration.kt (Lines 363-371)

// ===== PHASE 2: Clean up old hierarchy and insert elements =====
// CRITICAL: Delete old hierarchy records BEFORE inserting elements
// When elements are replaced (same hash), they get new IDs, orphaning old hierarchy records
// This causes FK constraint violations when inserting new hierarchy
database.scrapedHierarchyDao().deleteHierarchyForApp(appId)
Log.d(TAG, "Cleared old hierarchy records for app: $appId")

// Insert elements and capture database-assigned IDs
val assignedIds: List<Long> = database.scrapedElementDao().insertBatchWithIds(elements)
```

**Flow (FIXED):**

```
1. First Scrape:
   - Insert elements → IDs: [100, 101, 102]
   - Insert hierarchy → Parent:100, Children:[101, 102]
   ✅ Success

2. Second Scrape (same app):
   - DELETE old hierarchy (all relationships cleared)

   - Insert elements with REPLACE strategy
     → Old IDs [100, 101, 102] DELETED
     → New IDs [200, 201, 202] CREATED

   - Insert new hierarchy:
     Parent: 200, Children: [201, 202]

   ✅ Success (no orphaned hierarchy)
```

#### Impact

- **Before Fix:** Random crashes during re-scraping
- **After Fix:** Stable scraping with no FK violations
- **Performance:** Minimal impact (<5ms for DELETE query)

### Fix 2: Screen Duplication (October 31, 2025)

#### Problem

Sample app with 1 screen reported: **"Learned 4 screens, 11 elements"**

#### Root Cause

**Old Screen Hash:**
```kotlin
val windowTitle = rootNode.text?.toString() ?: ""
val screenHash = MD5(packageName + className + windowTitle)
```

**Problem:** Most Android windows have **empty windowTitle**

```
Screen 1: Welcome → Hash: MD5("com.example.app" + "MainActivity" + "")
Screen 2: Loading → Hash: MD5("com.example.app" + "MainActivity" + "")
Screen 3: Form    → Hash: MD5("com.example.app" + "MainActivity" + "")
Screen 4: Results → Hash: MD5("com.example.app" + "MainActivity" + "")

ALL HASHES IDENTICAL!
```

#### Solution

**Add content fingerprint to screen hash:**

```kotlin
// File: AccessibilityScrapingIntegration.kt (Lines 463-483)

// Create content-based screen hash for stable identification
// Using element structure fingerprint prevents duplicate screens
val windowTitle = rootNode.text?.toString() ?: ""

// Build a content fingerprint from visible elements to uniquely identify screen
// This prevents counting the same screen multiple times even if windowTitle is empty
val contentFingerprint = elements
    .filter { !it.className.contains("DecorView") && !it.className.contains("Layout") }
    .sortedBy { it.depth }
    .take(10)  // Use top 10 significant elements
    .joinToString("|") { e ->
        "${e.className}:${e.text ?: ""}:${e.contentDescription ?: ""}:${e.isClickable}"
    }

val screenHash = java.security.MessageDigest.getInstance("MD5")
    .digest("$packageName${event.className}$windowTitle$contentFingerprint".toByteArray())
    .joinToString("") { "%02x".format(it) }
```

**New Screen Hash:**

```
Screen 1: Welcome
  Fingerprint: "TextView:Welcome to App::false|Button:START::true|..."
  Hash: a3f7b92c... ← UNIQUE

Screen 2: Loading
  Fingerprint: "ProgressBar:::false|TextView:Loading...::false|..."
  Hash: 7d4e8f1a... ← UNIQUE

Screen 3: Form
  Fingerprint: "EditText::Email:false|EditText::Password:false|..."
  Hash: 2c9b5e3f... ← UNIQUE

Screen 4: Results
  Fingerprint: "ListView::Results:false|Button:BACK::true|..."
  Hash: 8a1f4d7c... ← UNIQUE
```

#### Stability Test

Revisiting same screen produces **identical hash**:

```
Visit 1: Screen 1 (Welcome) → Hash: a3f7b92c...
Visit 2: Screen 1 (Welcome) → Hash: a3f7b92c... ← MATCHES
Action: Increment visit_count (1 → 2)
```

#### Impact

- **Before Fix:** Inaccurate screen counts (1 screen reported as 4)
- **After Fix:** Accurate screen detection and tracking
- **Performance:** Minimal impact (<2ms for fingerprint generation)

---

## Performance Optimizations

### Hash-Based Element Lookup

**Why MD5 Hash?**

```kotlin
// Element identification
val elementHash = MessageDigest.getInstance("MD5")
    .digest("$className$viewIdResourceName$text$contentDescription".toByteArray())
    .joinToString("") { "%02x".format(it) }
```

**Benefits:**
1. **O(1) Lookup**: Hash index provides constant-time lookups
2. **Deduplication**: Same element detected multiple times → single DB entry
3. **Version Stability**: Hash unchanged across app versions (if UI unchanged)
4. **Collision Resistance**: MD5 collision probability negligible for UI elements

**Trade-off:**
- Hash computation cost (~0.1ms per element)
- Acceptable for accessibility tree traversal (typically <100 elements)

### Index Strategy

**Query Performance:**

| Query Type | Without Index | With Index | Speedup |
|------------|---------------|------------|---------|
| Element by hash | O(n) scan | O(log n) | 100-1000x |
| FK JOIN (Long) | O(n * m) | O(m log n) | 10-100x |
| FK JOIN (String) | O(n * m) | O(m log n) | 10-100x |

**Index Overhead:**

| Operation | Without Index | With Index | Slowdown |
|-----------|---------------|------------|----------|
| INSERT | Fast | Slower | ~10-20% |
| UPDATE | Fast | Slower | ~10-20% |
| DELETE | Fast | Slower | ~5-10% |

**Rule:** Index columns used in WHERE, JOIN, ORDER BY clauses.

### Batch Operations

**Why Batch?**

```kotlin
// BAD: Insert one-by-one (slow)
for (element in elements) {
    database.scrapedElementDao().insert(element)  // N round trips
}

// GOOD: Batch insert (fast)
database.scrapedElementDao().insertBatchWithIds(elements)  // 1 round trip
```

**Performance:**
- Single insert: ~1ms per element
- Batch insert: ~0.1ms per element (10x faster)
- Batch size: Recommended 100-1000 items

### Foreign Key Cascade Deletes

**Why Cascade?**

```kotlin
// Manual deletion (slow, error-prone)
val app = database.scrapedAppDao().getAppById(appId)
val elements = database.scrapedElementDao().getElementsByAppId(appId)
for (element in elements) {
    database.generatedCommandDao().deleteByElementHash(element.elementHash)
    database.elementRelationshipDao().deleteByElementHash(element.elementHash)
    // ... many more deletions
}
database.scrapedElementDao().deleteByAppId(appId)
database.scrapedAppDao().delete(app)

// CASCADE deletion (fast, automatic)
database.scrapedAppDao().delete(app)  // All related data deleted automatically
```

**Performance:**
- Manual: O(n) round trips
- Cascade: O(1) round trip + database-level cascade

### Query Optimization

**Use Indexed Columns in WHERE:**

```kotlin
// SLOW: Non-indexed column
@Query("SELECT * FROM scraped_elements WHERE text = :text")

// FAST: Indexed column
@Query("SELECT * FROM scraped_elements WHERE element_hash = :hash")
```

**Use LIMIT When Appropriate:**

```kotlin
// BAD: Fetch all, take first
val allElements = database.scrapedElementDao().getAllElements()
val firstElement = allElements.firstOrNull()

// GOOD: Fetch only what's needed
@Query("SELECT * FROM scraped_elements WHERE app_id = :appId LIMIT 1")
suspend fun getFirstElement(appId: String): ScrapedElementEntity?
```

**Use Projection (SELECT Specific Columns):**

```kotlin
// BAD: Fetch entire entity
@Query("SELECT * FROM scraped_elements WHERE app_id = :appId")
suspend fun getElements(appId: String): List<ScrapedElementEntity>

// GOOD: Fetch only needed columns
@Query("SELECT element_hash, text FROM scraped_elements WHERE app_id = :appId")
suspend fun getElementHashesAndText(appId: String): List<ElementHashAndText>

data class ElementHashAndText(val elementHash: String, val text: String?)
```

### Memory Management

**Pagination for Large Datasets:**

```kotlin
// BAD: Load all elements into memory
val allElements = database.scrapedElementDao().getAllElements()

// GOOD: Paginate
@Query("""
    SELECT * FROM scraped_elements
    WHERE app_id = :appId
    LIMIT :limit OFFSET :offset
""")
suspend fun getElementsPage(appId: String, limit: Int, offset: Int): List<ScrapedElementEntity>
```

**Use Flow for Observing Changes:**

```kotlin
// Live updates without polling
@Query("SELECT * FROM scraped_elements WHERE app_id = :appId")
fun observeElements(appId: String): Flow<List<ScrapedElementEntity>>
```

---

## Data Retention and Cleanup

### Automatic Cleanup Strategy

**File:** `AppScrapingDatabase.kt`

```kotlin
private const val RETENTION_DAYS = 7L

private suspend fun cleanupOldData(database: AppScrapingDatabase) {
    try {
        val retentionTimestamp = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(RETENTION_DAYS)

        // Delete old apps (cascades to elements, hierarchy, and commands via foreign keys)
        val deletedApps = database.scrapedAppDao().deleteAppsOlderThan(retentionTimestamp)

        // Delete low-quality commands (unused and low confidence < 0.3)
        val deletedCommands = database.generatedCommandDao().deleteLowQualityCommands(threshold = 0.3f)

        android.util.Log.d(
            "AppScrapingDatabase",
            "Cleanup complete: $deletedApps apps deleted, $deletedCommands low-quality commands removed"
        )
    } catch (e: Exception) {
        android.util.Log.e("AppScrapingDatabase", "Error during cleanup", e)
    }
}
```

**Cleanup Triggers:**

1. **On Database Open**: Automatic cleanup when database is first accessed
2. **Manual Trigger**: User can invoke from settings

```kotlin
// Manual cleanup
suspend fun performCleanup(context: Context) {
    val database = getInstance(context)
    cleanupOldData(database)
}
```

### Cleanup Rules

**Apps:**
- Delete apps not scraped in 7 days
- CASCADE deletes all related elements, commands, hierarchy

**Commands:**
- Delete commands with:
  - usageCount == 0
  - confidence < 0.3
  - isUserApproved == false

**Rationale:**
- Keep frequently-used data
- Remove low-quality, unused data
- User-approved commands never deleted

### Database Size Management

**Expected Growth:**

| Data Type | Size per Item | Items per App | Total (100 apps) |
|-----------|---------------|---------------|------------------|
| ScrapedElementEntity | ~500 bytes | 50-200 | 2.5-10 MB |
| ScrapedHierarchyEntity | ~50 bytes | 50-200 | 250 KB - 1 MB |
| GeneratedCommandEntity | ~200 bytes | 20-100 | 400 KB - 2 MB |
| ScreenContextEntity | ~300 bytes | 5-20 | 150 KB - 600 KB |
| **Total** | - | - | **~5-15 MB** |

**With 7-day retention:** Database stays under 20 MB.

### Manual Database Reset

```kotlin
suspend fun clearAllData(context: Context) {
    val database = getInstance(context)
    database.clearAllTables()
    android.util.Log.d("AppScrapingDatabase", "All data cleared")
}
```

**Use Cases:**
- Testing
- User-requested data reset
- Corrupted database recovery

---

## Testing Strategy

### Unit Tests (Robolectric)

**File:** `src/test/java/com/augmentalis/voiceoscore/scraping/database/AppScrapingDatabaseTest.kt`

```kotlin
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class AppScrapingDatabaseTest {

    private lateinit var database: AppScrapingDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            AppScrapingDatabase::class.java
        )
            .allowMainThreadQueries()  // For testing only
            .build()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun `insert element and retrieve by hash`() = runBlocking {
        // Arrange
        val element = ScrapedElementEntity(
            elementHash = "test-hash-123",
            appId = "test-app",
            className = "Button",
            viewIdResourceName = "submit",
            text = "Submit",
            contentDescription = "Submit button",
            bounds = "{\"left\":0,\"top\":0,\"right\":100,\"bottom\":50}",
            isClickable = true,
            isLongClickable = false,
            isEditable = false,
            isScrollable = false,
            isCheckable = false,
            isFocusable = true,
            isEnabled = true,
            depth = 2,
            indexInParent = 0
        )

        // Act
        database.scrapedElementDao().insert(element)
        val retrieved = database.scrapedElementDao().getElementByHash("test-hash-123")

        // Assert
        assertNotNull(retrieved)
        assertEquals("test-hash-123", retrieved?.elementHash)
        assertEquals("Submit", retrieved?.text)
    }

    @Test
    fun `cascade delete removes hierarchy when element deleted`() = runBlocking {
        // Arrange
        val parent = ScrapedElementEntity(/* ... */)
        val child = ScrapedElementEntity(/* ... */)

        val parentId = database.scrapedElementDao().insert(parent)
        val childId = database.scrapedElementDao().insert(child)

        val hierarchy = ScrapedHierarchyEntity(
            parentElementId = parentId,
            childElementId = childId,
            childOrder = 0,
            depth = 1
        )
        database.scrapedHierarchyDao().insert(hierarchy)

        // Act
        database.scrapedElementDao().delete(parent)

        // Assert
        val remainingHierarchy = database.scrapedHierarchyDao().getRelationshipCount()
        assertEquals(0, remainingHierarchy)  // Cascade delete worked
    }

    @Test
    fun `unique constraint prevents duplicate element hashes`() = runBlocking {
        // Arrange
        val element1 = ScrapedElementEntity(elementHash = "duplicate-hash", /* ... */)
        val element2 = ScrapedElementEntity(elementHash = "duplicate-hash", /* ... */)

        // Act
        database.scrapedElementDao().insert(element1)
        database.scrapedElementDao().insert(element2)  // REPLACE strategy

        // Assert
        val count = database.scrapedElementDao().getElementCount("test-app")
        assertEquals(1, count)  // Only one element with this hash
    }
}
```

### Integration Tests (Device/Emulator)

**File:** `src/androidTest/java/com/augmentalis/voiceoscore/scraping/database/AppScrapingDatabaseIntegrationTest.kt`

```kotlin
@RunWith(AndroidJUnit4::class)
class AppScrapingDatabaseIntegrationTest {

    @get:Rule
    val context = ApplicationProvider.getApplicationContext<Context>()

    private lateinit var database: AppScrapingDatabase

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            context,
            AppScrapingDatabase::class.java
        ).build()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun performanceTest_batchInsert1000Elements() = runBlocking {
        // Arrange
        val elements = (1..1000).map { i ->
            ScrapedElementEntity(
                elementHash = "hash-$i",
                appId = "test-app",
                className = "Button",
                viewIdResourceName = "button-$i",
                text = "Button $i",
                contentDescription = null,
                bounds = "{}",
                isClickable = true,
                isLongClickable = false,
                isEditable = false,
                isScrollable = false,
                isCheckable = false,
                isFocusable = false,
                isEnabled = true,
                depth = 1,
                indexInParent = i
            )
        }

        // Act
        val startTime = System.currentTimeMillis()
        database.scrapedElementDao().insertBatchWithIds(elements)
        val duration = System.currentTimeMillis() - startTime

        // Assert
        assertTrue("Batch insert should take <500ms", duration < 500)
        val count = database.scrapedElementDao().getElementCount("test-app")
        assertEquals(1000, count)
    }
}
```

### Migration Tests

**File:** `src/androidTest/java/com/augmentalis/voiceoscore/scraping/database/AppScrapingDatabaseMigrationTest.kt`

```kotlin
@RunWith(AndroidJUnit4::class)
class AppScrapingDatabaseMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppScrapingDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migrate1To2() {
        // Create database at version 1
        val db = helper.createDatabase(TEST_DB, 1).apply {
            // Insert test data in v1 schema
            execSQL("""
                INSERT INTO scraped_elements (element_hash, app_id, class_name, /* ... */)
                VALUES ('hash1', 'app1', 'Button', /* ... */)
            """)

            execSQL("""
                INSERT INTO generated_commands (element_id, command_text, /* ... */)
                VALUES (1, 'click submit', /* ... */)
            """)

            close()
        }

        // Migrate to version 2
        val migratedDb = helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)

        // Verify migration
        migratedDb.query("SELECT * FROM generated_commands").use { cursor ->
            assertTrue(cursor.moveToFirst())
            val elementHashIndex = cursor.getColumnIndex("element_hash")
            assertTrue(elementHashIndex >= 0)  // New column exists
            assertEquals("hash1", cursor.getString(elementHashIndex))
        }
    }

    companion object {
        private const val TEST_DB = "migration-test"
    }
}
```

### Test Coverage Goals

| Component | Target Coverage |
|-----------|----------------|
| Entity classes | 100% |
| DAO interfaces | 90% |
| Migration scripts | 100% |
| Database callbacks | 80% |

---

## Best Practices

### 1. Always Use Suspend Functions

```kotlin
// GOOD
@Dao
interface ScrapedElementDao {
    @Insert
    suspend fun insert(element: ScrapedElementEntity): Long
}

// BAD (blocks main thread)
@Dao
interface ScrapedElementDao {
    @Insert
    fun insert(element: ScrapedElementEntity): Long
}
```

### 2. Use Transactions for Multi-Step Operations

```kotlin
@Transaction
suspend fun insertElementWithCommands(
    element: ScrapedElementEntity,
    commands: List<GeneratedCommandEntity>
) {
    val elementId = scrapedElementDao().insert(element)
    commands.forEach { command ->
        generatedCommandDao().insert(command.copy(elementHash = element.elementHash))
    }
}
```

### 3. Prefer Hash-Based FKs for Semantic Relationships

```kotlin
// GOOD: Commands reference element by hash (survives updates)
@ForeignKey(
    entity = ScrapedElementEntity::class,
    parentColumns = ["element_hash"],
    childColumns = ["element_hash"]
)

// BAD: Commands reference element by ID (breaks on REPLACE)
@ForeignKey(
    entity = ScrapedElementEntity::class,
    parentColumns = ["id"],
    childColumns = ["element_id"]
)
```

**Exception:** Hierarchy uses Long IDs for performance (with explicit cleanup).

### 4. Use Unique Constraints to Prevent Duplicates

```kotlin
@Entity(
    tableName = "scraped_elements",
    indices = [
        Index(value = ["element_hash"], unique = true)  ← Prevents duplicates
    ]
)
```

### 5. Make AI Fields Nullable for Backward Compatibility

```kotlin
val semanticRole: String? = null,     // ← Nullable
val inputType: String? = null,        // ← Nullable
val visualWeight: String? = null,     // ← Nullable
```

**Why?** Existing elements won't have AI-inferred values yet.

### 6. Export Schema for Migration Testing

```gradle
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}
```

### 7. Use Cascade Deletes for Related Data

```kotlin
@ForeignKey(
    entity = AppEntity::class,
    parentColumns = ["app_id"],
    childColumns = ["app_id"],
    onDelete = ForeignKey.CASCADE  ← Automatic cleanup
)
```

### 8. Index Foreign Keys

```kotlin
@Entity(
    tableName = "scraped_elements",
    foreignKeys = [ /* ... */ ],
    indices = [
        Index("app_id")  ← Index FK column for JOIN performance
    ]
)
```

### 9. Use Batch Operations for Performance

```kotlin
// Insert 100 elements in one transaction
database.scrapedElementDao().insertBatchWithIds(elements)
```

### 10. Log Migration Steps for Debugging

```kotlin
val MIGRATION_X_Y = object : Migration(X, Y) {
    override fun migrate(db: SupportSQLiteDatabase) {
        Log.i("DB", "Starting migration $X → $Y")

        try {
            // Migration steps
            Log.d("DB", "Step 1 complete")
            Log.d("DB", "Step 2 complete")

            Log.i("DB", "Migration $X → $Y successful")
        } catch (e: Exception) {
            Log.e("DB", "Migration $X → $Y failed", e)
            throw e
        }
    }
}
```

---

## Summary

The VOS4 database system is a sophisticated, well-architected Room-based solution that:

1. **Scales Efficiently**: Hash-based lookups, indexed FKs, batch operations
2. **Maintains Integrity**: Foreign key cascades, unique constraints
3. **Evolves Gracefully**: 9 successful migrations without data loss
4. **Performs Reliably**: Recent critical fixes ensure stability
5. **Stays Clean**: Automatic retention policies prevent bloat

**Key Takeaways:**

- **Element Hash**: MD5-based unique identifier for deduplication
- **Hierarchy**: Uses Long IDs for performance (with explicit cleanup)
- **Commands**: Use String hash FKs for semantic persistence
- **Screen Contexts**: Content fingerprint prevents duplicate screens
- **Migrations**: Well-tested, logged, incremental schema evolution
- **Cleanup**: 7-day retention, low-quality command removal
- **Performance**: O(1) lookups, batch operations, cascade deletes

**Next Steps:**

- Chapter 17: Architectural Decisions (Why these design choices?)
- Chapter 18: Performance Design (Memory, battery, rendering optimizations)
- Chapter 19: Security Design (Permissions, encryption, privacy)
- Chapter 20: Current State Analysis (What's completed, what's pending?)
- Chapter 21: Expansion Roadmap (Future plans for VOS4)

---

**End of Chapter 16**
