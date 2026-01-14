# VoiceOS Room → SQLDelight Migration Plan

**Module:** VoiceOS/apps/VoiceOSCore
**Created:** 2025-12-18
**Version:** 1.0.0
**Mode:** .yolo .cot .tot .rot .swarm

---

## Executive Summary

| Metric | Value |
|--------|-------|
| Total Files to Migrate | 27 |
| Room Entities | 13 |
| Room DAOs | 13 |
| Room Databases | 2 |
| SQLDelight Schemas (existing) | 41 |
| Phases | 4 |
| Swarm Agents | 6 |
| Risk Level | MEDIUM |

---

## Reasoning Analysis

### Chain-of-Thought (CoT) - Sequential Dependencies

```
1. SQLDelight schemas already exist (41 .sq files) ✓
2. Room entities/DAOs in VoiceOSCore → must redirect to SQLDelight repositories
3. Scraping entities depend on ScrapedApp (root) → migrate first
4. LearnApp entities depend on LearnedApp (root) → can parallel with scraping
5. LearnWeb entities depend on ScrapedWebsite (root) → can parallel
6. DAOs must be replaced with repository adapters calling SQLDelight queries
7. Database classes (AppScrapingDatabase, WebScrapingDatabase) → DELETE after migration
8. VoiceOSCoreDatabaseAdapter already exists → extend to cover all entities
```

**Critical Path:** ScrapedApp → ScrapedElement → DependentEntities → DAOs → Database deletion

### Tree-of-Thought (ToT) - Alternative Approaches

```
                    ┌─────────────────────────────────────┐
                    │     Room → SQLDelight Migration     │
                    └─────────────────┬───────────────────┘
                                      │
          ┌───────────────────────────┼───────────────────────────┐
          ▼                           ▼                           ▼
   ┌──────────────┐          ┌──────────────┐          ┌──────────────┐
   │  Approach A  │          │  Approach B  │          │  Approach C  │
   │ Delete Room  │          │ Dual-Write   │          │ Adapter Only │
   │ Use existing │          │ Parallel     │          │ Keep Room    │
   └──────┬───────┘          └──────┬───────┘          └──────┬───────┘
          │                         │                         │
  Risk: LOW ✓              Risk: HIGH               Risk: NONE
  Work: Medium              Work: Very High          Work: Minimal
  Result: Clean             Result: Complex          Result: Tech debt
```

**Selected:** Approach A - Delete Room entities/DAOs, use existing SQLDelight schemas via repository adapters

### Recursive-of-Thought (RoT) - Decomposition

```
Room → SQLDelight Migration
├── Phase 1: Scraping Database (17 files)
│   ├── Subproblem 1.1: Create SQLDelightScrapingAdapter
│   │   ├── Leaf: Map ScrapedAppEntity → ScrapedApp.sq
│   │   ├── Leaf: Map ScrapedElementEntity → ScrapedElement.sq
│   │   ├── Leaf: Map GeneratedCommandEntity → GeneratedCommand.sq
│   │   └── Leaf: Map remaining 6 entities
│   ├── Subproblem 1.2: Replace Room DAOs (9 files)
│   │   └── Leaf: Redirect each DAO to adapter
│   └── Subproblem 1.3: Delete Room database
│       └── Leaf: Delete AppScrapingDatabase.kt
├── Phase 2: LearnApp Database (5 files)
│   ├── Subproblem 2.1: Extend LearnAppDatabaseAdapter
│   │   ├── Leaf: Map LearnedAppEntity → LearnedApp.sq
│   │   ├── Leaf: Map ExplorationSessionEntity → ExplorationSession.sq
│   │   ├── Leaf: Map ScreenStateEntity → ScreenState.sq
│   │   └── Leaf: Map NavigationEdgeEntity → NavigationEdge.sq
│   └── Subproblem 2.2: Update LearnAppDao
│       └── Leaf: Replace with SQLDelight calls
├── Phase 3: LearnWeb Database (4 files)
│   ├── Subproblem 3.1: Create SQLDelightWebScrapingAdapter
│   │   ├── Leaf: Map ScrapedWebsite → (new .sq)
│   │   ├── Leaf: Map ScrapedWebElement → (new .sq)
│   │   └── Leaf: Map GeneratedWebCommand → (new .sq)
│   ├── Subproblem 3.2: Replace Room DAOs (3 files)
│   └── Subproblem 3.3: Delete Room database
│       └── Leaf: Delete WebScrapingDatabase.kt
└── Phase 4: Cleanup & Verification
    ├── Subproblem 4.1: Update all callers
    ├── Subproblem 4.2: Remove Room dependencies from build.gradle
    └── Subproblem 4.3: Verify build passes
```

---

## SQLDelight Schema Status

### Existing SQLDelight Schemas (Already Defined)

| Room Entity | SQLDelight File | Status |
|-------------|-----------------|--------|
| ScrapedAppEntity | ScrapedApp.sq | ✅ EXISTS |
| ScrapedElementEntity | ScrapedElement.sq | ✅ EXISTS |
| ScrapedHierarchyEntity | ScrapedHierarchy.sq | ✅ EXISTS |
| GeneratedCommandEntity | GeneratedCommand.sq | ✅ EXISTS |
| ScreenContextEntity | ScreenContext.sq | ✅ EXISTS |
| ElementRelationshipEntity | ElementRelationship.sq | ✅ EXISTS |
| ScreenTransitionEntity | ScreenTransition.sq | ✅ EXISTS |
| UserInteractionEntity | UserInteraction.sq | ✅ EXISTS |
| ElementStateHistoryEntity | ElementStateHistory.sq | ✅ EXISTS |
| LearnedAppEntity | LearnedApp.sq | ✅ EXISTS |
| ExplorationSessionEntity | ExplorationSession.sq | ✅ EXISTS |
| ScreenStateEntity | ScreenState.sq | ✅ EXISTS |
| NavigationEdgeEntity | NavigationEdge.sq | ✅ EXISTS |

### Missing SQLDelight Schemas (Need Creation)

| Room Entity | Required .sq File | Priority |
|-------------|-------------------|----------|
| ScrapedWebsite | ScrapedWebsite.sq | HIGH |
| ScrapedWebElement | ScrapedWebElement.sq | HIGH |
| GeneratedWebCommand | GeneratedWebCommand.sq | HIGH |

---

## Phase 1: Scraping Database Migration

**Priority:** HIGH
**Files:** 17 (9 entities + 9 DAOs + 1 database)
**Swarm Agents:** 2

### Task Group 1.1: Create SQLDelightScrapingAdapter

| Task | Action | File |
|------|--------|------|
| 1.1.1 | CREATE | `scraping/adapter/SQLDelightScrapingAdapter.kt` |

**Adapter Pattern:**
```kotlin
class SQLDelightScrapingAdapter(
    private val database: VoiceOSDatabase
) {
    // ScrapedApp operations
    suspend fun insertScrapedApp(entity: ScrapedAppEntity) {
        database.scrapedAppQueries.insert(
            app_id = entity.appId,
            package_name = entity.packageName,
            app_name = entity.appName,
            // ... map all fields
        )
    }

    suspend fun getScrapedApp(appId: String): ScrapedAppEntity? {
        return database.scrapedAppQueries.getById(appId)
            .executeAsOneOrNull()
            ?.toEntity()
    }

    // ... all DAO methods mapped
}
```

### Task Group 1.2: Replace Room DAOs with Adapter Calls

| Task | File | Action |
|------|------|--------|
| 1.2.1 | ScrapedAppDao.kt | REWRITE → delegate to adapter |
| 1.2.2 | ScrapedElementDao.kt | REWRITE → delegate to adapter |
| 1.2.3 | ScrapedHierarchyDao.kt | REWRITE → delegate to adapter |
| 1.2.4 | GeneratedCommandDao.kt | REWRITE → delegate to adapter |
| 1.2.5 | ScreenContextDao.kt | REWRITE → delegate to adapter |
| 1.2.6 | ElementRelationshipDao.kt | REWRITE → delegate to adapter |
| 1.2.7 | ScreenTransitionDao.kt | REWRITE → delegate to adapter |
| 1.2.8 | UserInteractionDao.kt | REWRITE → delegate to adapter |
| 1.2.9 | ElementStateHistoryDao.kt | REWRITE → delegate to adapter |

**DAO Replacement Pattern:**
```kotlin
// Before (Room)
@Dao
interface ScrapedAppDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(app: ScrapedAppEntity)

    @Query("SELECT * FROM scraped_apps WHERE app_id = :appId")
    suspend fun getAppById(appId: String): ScrapedAppEntity?
}

// After (SQLDelight Adapter)
class ScrapedAppDao(
    private val adapter: SQLDelightScrapingAdapter
) {
    suspend fun insert(app: ScrapedAppEntity) = adapter.insertScrapedApp(app)
    suspend fun getAppById(appId: String) = adapter.getScrapedApp(appId)
}
```

### Task Group 1.3: Delete Room Entities & Database

| Task | File | Action |
|------|------|--------|
| 1.3.1 | entities/ScrapedAppEntity.kt | CONVERT to data class (remove @Entity) |
| 1.3.2 | entities/ScrapedElementEntity.kt | CONVERT to data class |
| 1.3.3 | entities/ScrapedHierarchyEntity.kt | CONVERT to data class |
| 1.3.4 | entities/GeneratedCommandEntity.kt | CONVERT to data class |
| 1.3.5 | entities/ScreenContextEntity.kt | CONVERT to data class |
| 1.3.6 | entities/ElementRelationshipEntity.kt | CONVERT to data class |
| 1.3.7 | entities/ScreenTransitionEntity.kt | CONVERT to data class |
| 1.3.8 | entities/UserInteractionEntity.kt | CONVERT to data class |
| 1.3.9 | entities/ElementStateHistoryEntity.kt | CONVERT to data class |
| 1.3.10 | database/AppScrapingDatabase.kt | DELETE |

---

## Phase 2: LearnApp Database Migration

**Priority:** HIGH
**Files:** 5 (4 entities + 1 DAO)
**Swarm Agents:** 1

### Task Group 2.1: Extend LearnAppDatabaseAdapter

| Task | Action |
|------|--------|
| 2.1.1 | Add LearnedApp operations to existing adapter |
| 2.1.2 | Add ExplorationSession operations |
| 2.1.3 | Add ScreenState operations |
| 2.1.4 | Add NavigationEdge operations |

### Task Group 2.2: Update LearnAppDao

| Task | File | Action |
|------|------|--------|
| 2.2.1 | LearnAppDao.kt | REWRITE → SQLDelight calls |

### Task Group 2.3: Convert Entities

| Task | File | Action |
|------|------|--------|
| 2.3.1 | LearnedAppEntity.kt | CONVERT to data class |
| 2.3.2 | ExplorationSessionEntity.kt | CONVERT to data class |
| 2.3.3 | ScreenStateEntity.kt | CONVERT to data class |
| 2.3.4 | NavigationEdgeEntity.kt | CONVERT to data class |

---

## Phase 3: LearnWeb Database Migration

**Priority:** MEDIUM
**Files:** 4 (3 entities + 1 database)
**Swarm Agents:** 2

### Task Group 3.1: Create SQLDelight Schemas

| Task | File | Action |
|------|------|--------|
| 3.1.1 | ScrapedWebsite.sq | CREATE in core/database |
| 3.1.2 | ScrapedWebElement.sq | CREATE in core/database |
| 3.1.3 | GeneratedWebCommand.sq | CREATE in core/database |

**ScrapedWebsite.sq:**
```sql
CREATE TABLE scraped_website (
    url_hash TEXT NOT NULL PRIMARY KEY,
    url TEXT NOT NULL,
    domain TEXT NOT NULL,
    title TEXT NOT NULL,
    structure_hash TEXT NOT NULL,
    parent_url_hash TEXT,
    scraped_at INTEGER NOT NULL,
    last_accessed_at INTEGER NOT NULL,
    access_count INTEGER NOT NULL DEFAULT 0,
    is_stale INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_scraped_website_domain ON scraped_website(domain);
CREATE INDEX idx_scraped_website_parent ON scraped_website(parent_url_hash);

insert:
INSERT OR REPLACE INTO scraped_website VALUES ?;

getByUrlHash:
SELECT * FROM scraped_website WHERE url_hash = ?;

getByDomain:
SELECT * FROM scraped_website WHERE domain = ?;

deleteByUrlHash:
DELETE FROM scraped_website WHERE url_hash = ?;
```

### Task Group 3.2: Create SQLDelightWebScrapingAdapter

| Task | File | Action |
|------|------|--------|
| 3.2.1 | learnweb/adapter/SQLDelightWebScrapingAdapter.kt | CREATE |

### Task Group 3.3: Replace Room DAOs

| Task | File | Action |
|------|------|--------|
| 3.3.1 | ScrapedWebsiteDao.kt | REWRITE → adapter |
| 3.3.2 | ScrapedWebElementDao.kt | REWRITE → adapter |
| 3.3.3 | GeneratedWebCommandDao.kt | REWRITE → adapter |

### Task Group 3.4: Delete Room Database

| Task | File | Action |
|------|------|--------|
| 3.4.1 | WebScrapingDatabase.kt | DELETE |

---

## Phase 4: Cleanup & Verification

**Priority:** HIGH
**Swarm Agents:** 1

### Task Group 4.1: Update build.gradle.kts

| Task | File | Change |
|------|------|--------|
| 4.1.1 | VoiceOSCore/build.gradle.kts | Remove Room dependencies |

**Remove:**
```kotlin
// DELETE these
implementation("androidx.room:room-runtime:2.6.1")
implementation("androidx.room:room-ktx:2.6.1")
kapt("androidx.room:room-compiler:2.6.1")
```

### Task Group 4.2: Update Callers

| Task | Component | Change |
|------|-----------|--------|
| 4.2.1 | AccessibilityScrapingIntegration | Use adapter instead of Room DAO |
| 4.2.2 | LearnAppIntegration | Use adapter instead of Room DAO |
| 4.2.3 | VoiceOSService | Update database initialization |

### Task Group 4.3: Verify Build

| Task | Command |
|------|---------|
| 4.3.1 | `./gradlew :Modules:VoiceOS:core:database:compileDebugKotlin` |
| 4.3.2 | `./gradlew :Modules:VoiceOS:apps:VoiceOSCore:compileDebugKotlin` |

---

## Swarm Configuration

### Agent Distribution

| Agent | Phase | Tasks | Files |
|-------|-------|-------|-------|
| Agent 1 | 1.1 | Create SQLDelightScrapingAdapter | 1 file |
| Agent 2 | 1.2 | Rewrite scraping DAOs | 9 files |
| Agent 3 | 1.3 + 2.3 | Convert entities to data classes | 13 files |
| Agent 4 | 2.1 + 2.2 | LearnApp adapter + DAO | 2 files |
| Agent 5 | 3.1 + 3.2 + 3.3 | LearnWeb schemas + adapter + DAOs | 7 files |
| Agent 6 | 4.x | Cleanup + verification | 3 files |

### Execution Order

```
Phase 1 + 2 (Parallel)
├── Agent 1: SQLDelightScrapingAdapter ───────┐
├── Agent 2: Scraping DAOs (depends on A1) ───┼── Sequential
├── Agent 3: Entity conversions ──────────────┤
└── Agent 4: LearnApp adapter + DAO ──────────┘

Phase 3 (After Phase 1)
├── Agent 5: LearnWeb complete ───────────────┘

Phase 4 (After all)
└── Agent 6: Cleanup + verify ────────────────┘
```

---

## Files Summary

| Category | Count | Action |
|----------|-------|--------|
| Room Entities | 13 | CONVERT to data class |
| Room DAOs | 13 | REWRITE to adapter |
| Room Databases | 2 | DELETE |
| SQLDelight .sq | 3 | CREATE (web) |
| Adapters | 2 | CREATE |
| build.gradle.kts | 1 | MODIFY |
| **Total** | **34** | |

---

## Success Criteria

| Criterion | Target |
|-----------|--------|
| Build passes | Both database and VoiceOSCore modules |
| Zero Room imports | No `androidx.room` imports in migrated files |
| Zero @Entity/@Dao | No Room annotations in codebase |
| All queries work | Equivalent functionality via SQLDelight |
| Tests pass | Existing tests still pass |

---

## Rollback Strategy

1. **Phase 1 rollback:** Restore Room DAOs from git
2. **Phase 2 rollback:** Restore LearnApp entities from git
3. **Phase 3 rollback:** Restore LearnWeb database from git
4. **Full rollback:** `git checkout -- Modules/VoiceOS/apps/VoiceOSCore/`

---

**Plan Version:** 1.0.0
**Created:** 2025-12-18
**Author:** VoiceOS Development Team
