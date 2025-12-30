# VOS-Spec-RoomToSQLDelightMigration-51218-V1

## Specification: LearnApp Room to SQLDelight Database Migration

| Field | Value |
|-------|-------|
| Version | 1.0 |
| Author | Manoj Jhawar |
| Created | 2025-12-18 |
| Status | APPROVED |
| Priority | Strategic |

---

## 1. Overview

### 1.1 Objective

Migrate LearnApp database from Room (Android-only) to SQLDelight (KMP cross-platform), achieving compliance with project database standard while preserving all existing functionality.

### 1.2 Background

The LearnApp module currently uses Room database, which violates the project-wide mandate: **"SQLDelight ONLY (never Room)"**. This specification defines the migration from Room to SQLDelight.

### 1.3 Scope

| In Scope | Out of Scope |
|----------|--------------|
| LearnApp standalone module | VoiceOSCore (already SQLDelight) |
| Room entities, DAOs, database class | core:database module changes |
| Repository pattern implementation | New feature development |
| Integration with VoiceOSCore | UI/UX changes |

---

## 2. Current State Analysis

### 2.1 Room Components

| Component | Location | Lines | Purpose |
|-----------|----------|-------|---------|
| LearnAppDatabase | `database/LearnAppDatabase.kt` | ~50 | Room database with 4 entities |
| LearnAppDao | `database/dao/LearnAppDao.kt` | 154 | 26 query methods |
| LearnedAppEntity | `database/entities/LearnedAppEntity.kt` | ~30 | App metadata |
| ExplorationSessionEntity | `database/entities/ExplorationSessionEntity.kt` | ~25 | Session tracking |
| NavigationEdgeEntity | `database/entities/NavigationEdgeEntity.kt` | ~25 | Graph edges |
| ScreenStateEntity | `database/entities/ScreenStateEntity.kt` | ~25 | Screen fingerprints |
| LearnAppRepository | `database/repository/LearnAppRepository.kt` | 888 | Repository pattern |

### 2.2 Room Schema

```sql
-- learned_apps (PK: package_name)
CREATE TABLE learned_apps (
    package_name TEXT PRIMARY KEY,
    app_name TEXT NOT NULL,
    version_code INTEGER NOT NULL,
    version_name TEXT NOT NULL,
    first_learned_at INTEGER NOT NULL,
    last_updated_at INTEGER NOT NULL,
    total_screens INTEGER NOT NULL,
    total_elements INTEGER NOT NULL,
    app_hash TEXT NOT NULL,
    exploration_status TEXT NOT NULL
);

-- exploration_sessions (FK: package_name → learned_apps)
CREATE TABLE exploration_sessions (
    session_id TEXT PRIMARY KEY,
    package_name TEXT NOT NULL,
    started_at INTEGER NOT NULL,
    completed_at INTEGER,
    status TEXT NOT NULL,
    screens_explored INTEGER NOT NULL DEFAULT 0,
    elements_discovered INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY (package_name) REFERENCES learned_apps(package_name) ON DELETE CASCADE
);

-- navigation_edges (FK: package_name, session_id)
CREATE TABLE navigation_edges (
    edge_id TEXT PRIMARY KEY,
    package_name TEXT NOT NULL,
    session_id TEXT NOT NULL,
    from_screen_hash TEXT NOT NULL,
    clicked_element_uuid TEXT NOT NULL,
    to_screen_hash TEXT NOT NULL,
    timestamp INTEGER NOT NULL,
    FOREIGN KEY (package_name) REFERENCES learned_apps(package_name) ON DELETE CASCADE,
    FOREIGN KEY (session_id) REFERENCES exploration_sessions(session_id) ON DELETE CASCADE
);

-- screen_states (FK: package_name)
CREATE TABLE screen_states (
    screen_hash TEXT PRIMARY KEY,
    package_name TEXT NOT NULL,
    activity_name TEXT,
    element_count INTEGER NOT NULL DEFAULT 0,
    discovered_at INTEGER NOT NULL,
    FOREIGN KEY (package_name) REFERENCES learned_apps(package_name) ON DELETE CASCADE
);
```

### 2.3 Relationship Diagram

```
learned_apps (PK: package_name)
    │
    ├──< exploration_sessions (FK: package_name, CASCADE DELETE)
    │       │
    │       └──< navigation_edges (FK: session_id, CASCADE DELETE)
    │
    ├──< navigation_edges (FK: package_name, CASCADE DELETE)
    │
    └──< screen_states (FK: package_name, CASCADE DELETE)
```

---

## 3. Target State: SQLDelight

### 3.1 Existing SQLDelight Schemas

SQLDelight schemas already exist in `Modules/VoiceOS/core/database/`:

| File | Table | Queries | Status |
|------|-------|---------|--------|
| `LearnedApp.sq` | learned_apps | 19 | COMPLETE |
| `ExplorationSession.sq` | exploration_sessions | 8 | COMPLETE |
| `NavigationEdge.sq` | navigation_edges | 11 | COMPLETE |
| `ScreenState.sq` | screen_states | 8 | COMPLETE |

### 3.2 Schema Comparison

| Field | Room | SQLDelight | Action |
|-------|------|------------|--------|
| package_name | TEXT PK | TEXT PK | Compatible |
| app_name | TEXT | TEXT | Compatible |
| version_code | LONG | INTEGER | Cast Long→Int |
| version_name | TEXT | TEXT | Compatible |
| first_learned_at | LONG | INTEGER | Cast Long→Int |
| last_updated_at | LONG | INTEGER | Cast Long→Int |
| total_screens | INT | INTEGER | Compatible |
| total_elements | INT | INTEGER | Compatible |
| app_hash | TEXT | TEXT | Compatible |
| exploration_status | TEXT | TEXT | Compatible |
| learning_mode | - | TEXT | SQLDelight only |
| status | - | TEXT | SQLDelight only |
| progress | - | INTEGER | SQLDelight only |
| command_count | - | INTEGER | SQLDelight only |
| screens_explored | - | INTEGER | SQLDelight only |
| is_auto_detect_enabled | - | INTEGER | SQLDelight only |
| pause_state | - | TEXT | SQLDelight only |

**Key Finding:** SQLDelight schema is a **superset** of Room schema. No data loss possible.

---

## 4. API Compatibility

### 4.1 DAO Method Mapping

| Room DAO Method | SQLDelight Query | Compatibility |
|-----------------|------------------|---------------|
| `insertLearnedApp()` | `insertLearnedApp` | DIRECT |
| `insertLearnedAppMinimal()` | `insertLearnedAppMinimal` | DIRECT |
| `getLearnedApp()` | `getLearnedApp` | DIRECT |
| `getAllLearnedApps()` | `getAllLearnedApps` | DIRECT |
| `updateAppHash()` | `updateAppHash` | DIRECT |
| `updateAppStats()` | `updateAppStats` | DIRECT |
| `deleteLearnedApp()` | `deleteLearnedApp` | DIRECT |
| `insertExplorationSession()` | `insertExplorationSession` | DIRECT |
| `getExplorationSession()` | `getExplorationSession` | DIRECT |
| `getSessionsForPackage()` | `getSessionsForPackage` | DIRECT |
| `updateSessionStatus()` | `updateSessionStatus` | DIRECT |
| `deleteSessionsForPackage()` | `deleteSessionsForPackage` | DIRECT |
| `insertNavigationEdge()` | `insertNavigationEdge` | DIRECT |
| `insertNavigationEdges()` | `insertNavigationEdges` | LOOP INSERT |
| `getNavigationGraph()` | `getNavigationGraph` | DIRECT |
| `getOutgoingEdges()` | `getOutgoingEdges` | DIRECT |
| `getIncomingEdges()` | `getIncomingEdges` | DIRECT |
| `getEdgesForSession()` | `getEdgesForSession` | DIRECT |
| `deleteNavigationGraph()` | `deleteNavigationGraph` | DIRECT |
| `insertScreenState()` | `insertScreenState` | DIRECT |
| `getScreenState()` | `getScreenState` | DIRECT |
| `getScreenStatesForPackage()` | `getScreenStatesForPackage` | DIRECT |
| `deleteScreenStatesForPackage()` | `deleteScreenStatesForPackage` | DIRECT |
| `getTotalScreensForPackage()` | `getTotalScreensForPackage` | DIRECT |
| `getTotalEdgesForPackage()` | `getTotalEdgesForPackage` | DIRECT |
| `getLastSessionWithStatus()` | `getActiveSessions` | ADAPTER |

### 4.2 Repository Interface

```kotlin
interface ILearnAppRepository {
    // LearnedApp operations
    suspend fun saveLearnedApp(packageName: String, appName: String, versionCode: Long, versionName: String, stats: ExplorationStats)
    suspend fun getLearnedApp(packageName: String): LearnedAppEntity?
    suspend fun isAppLearned(packageName: String): Boolean
    suspend fun getAllLearnedApps(): List<LearnedAppEntity>
    suspend fun updateAppHash(packageName: String, newHash: String)
    suspend fun deleteLearnedApp(packageName: String)
    suspend fun deleteAppCompletely(packageName: String): RepositoryResult<Boolean>
    suspend fun resetAppForRelearning(packageName: String): RepositoryResult<Boolean>
    suspend fun clearExplorationData(packageName: String): RepositoryResult<Boolean>

    // Session operations
    suspend fun createExplorationSessionSafe(packageName: String): SessionCreationResult
    suspend fun createExplorationSessionStrict(packageName: String): String
    suspend fun ensureLearnedAppExists(packageName: String): RepositoryResult<Boolean>
    suspend fun createExplorationSessionUpsert(packageName: String): SessionCreationResult
    suspend fun completeExplorationSession(sessionId: String, stats: ExplorationStats)
    suspend fun getExplorationSession(sessionId: String): ExplorationSessionEntity?
    suspend fun getSessionsForPackage(packageName: String): List<ExplorationSessionEntity>

    // Navigation graph operations
    suspend fun saveNavigationEdge(packageName: String, sessionId: String, fromScreenHash: String, clickedElementUuid: String, toScreenHash: String)
    suspend fun saveNavigationGraph(graph: NavigationGraph, sessionId: String)
    suspend fun getNavigationGraph(packageName: String): NavigationGraph?
    suspend fun deleteNavigationGraph(packageName: String)

    // Screen state operations
    suspend fun saveScreenState(screenState: ScreenState)
    suspend fun getScreenState(hash: String): ScreenStateEntity?

    // Statistics
    suspend fun getAppStatistics(packageName: String): AppStatistics
}
```

---

## 5. Data Migration Strategy

### 5.1 Recommended: Fresh Start

**Rationale:**
- LearnApp data is ephemeral exploration data
- Re-exploration takes minimal time per app
- Migration adds complexity with minimal benefit
- SQLDelight schema is superset of Room schema

**Approach:**
1. On upgrade, clear Room database
2. Users re-learn apps as needed
3. No data corruption risk

### 5.2 Alternative: Data Migration

**Not Recommended** due to:
- Complexity of cross-database migration
- Transient nature of exploration data
- Low user impact of data loss

---

## 6. Success Criteria

| Criterion | Validation Method |
|-----------|-------------------|
| Build passes | `./gradlew assembleDebug` succeeds |
| Zero Room imports | `grep -r "androidx.room" apps/LearnApp/` returns empty |
| All tests pass | `./gradlew test` succeeds |
| Functional parity | All 26 DAO methods work identically |
| No data loss | Exploration works as before |

---

## 7. References

- [SQLDelight Documentation](https://cashapp.github.io/sqldelight/)
- [VoiceOS Database Module](../../../Modules/VoiceOS/core/database/)
- [LearnApp Repository](../../../Modules/VoiceOS/apps/LearnApp/)

---

**Document History:**

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2025-12-18 | Manoj Jhawar | Initial specification |
