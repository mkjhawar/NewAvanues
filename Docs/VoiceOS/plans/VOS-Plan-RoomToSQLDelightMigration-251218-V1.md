# VOS-Plan-RoomToSQLDelightMigration-51218-V1

## Implementation Plan: LearnApp Room to SQLDelight Migration

| Field | Value |
|-------|-------|
| Version | 1.0 |
| Author | Manoj Jhawar |
| Created | 2025-12-18 |
| Spec Reference | VOS-Spec-RoomToSQLDelightMigration-51218-V1.md |
| Status | ✅ COMPLETE |

---

## 1. Executive Summary

Migrate LearnApp database access from Room to SQLDelight, eliminating the project-wide violation: "SQLDelight ONLY (never Room)". SQLDelight schemas already exist in `core:database` module.

**Key Finding:** SQLDelight schemas are a superset of Room schemas - no data model changes needed.

---

## 2. Migration Strategy

### 2.1 Approach: Adapter Pattern + Entity Rewrite

| Step | Action | Impact |
|------|--------|--------|
| 1 | Create `ILearnAppRepository` interface | None - new file |
| 2 | Create `SQLDelightLearnAppRepository` implementation | None - new file |
| 3 | Convert Room entities to data classes | Compile-time only |
| 4 | Replace Room DAO references with SQLDelight queries | Internal changes |
| 5 | Remove Room dependencies from build.gradle.kts | Build config |

### 2.2 What's NOT Changing

- Repository public API (all 26 methods)
- ExplorationEngine integration
- LearnAppIntegration usage
- Model classes (NavigationGraph, ScreenState, etc.)

---

## 3. Implementation Phases

### Phase 1: Create SQLDelight Repository Adapter (15 min)

**Files to Create:**

| File | Purpose |
|------|---------|
| `database/repository/ILearnAppRepository.kt` | Interface matching current API |
| `database/repository/SQLDelightLearnAppRepository.kt` | SQLDelight implementation |

**Implementation Details:**

The SQLDelightLearnAppRepository will:
1. Take `VoiceOSDatabase` as dependency (from core:database module)
2. Use existing `.sq` queries: `learnedAppQueries`, `explorationSessionQueries`, `navigationEdgeQueries`, `screenStateQueries`
3. Map SQLDelight-generated types to existing entity data classes

---

### Phase 2: Convert Room Entities to Data Classes (10 min)

**Files to Modify:**

| File | Change |
|------|--------|
| `LearnedAppEntity.kt` | Remove Room annotations, keep data class |
| `ExplorationSessionEntity.kt` | Remove Room annotations, keep data class |
| `NavigationEdgeEntity.kt` | Remove Room annotations, keep data class |
| `ScreenStateEntity.kt` | Remove Room annotations, keep data class |

**Before:**
```kotlin
@Entity(tableName = "learned_apps")
data class LearnedAppEntity(
    @PrimaryKey
    @ColumnInfo(name = "package_name")
    val packageName: String,
    ...
)
```

**After:**
```kotlin
data class LearnedAppEntity(
    val packageName: String,
    ...
)
```

---

### Phase 3: Delete Room Files (5 min)

**Files to Delete:**

| File | Reason |
|------|--------|
| `LearnAppDatabase.kt` | Room database class no longer needed |
| `dao/LearnAppDao.kt` | Room DAO no longer needed |

---

### Phase 4: Update Repository References (10 min)

**Files to Modify:**

| File | Change |
|------|--------|
| `LearnAppRepository.kt` | Refactor to implement ILearnAppRepository, use SQLDelight |

**Option A (Recommended):** Rename existing repository to `SQLDelightLearnAppRepository`
**Option B:** Create new implementation, deprecate old

---

### Phase 5: Update Build Configuration (5 min)

**Files to Modify:**

| File | Change |
|------|--------|
| `apps/LearnApp/build.gradle.kts` | Remove Room dependencies, add core:database |

**Dependencies to Remove:**
```kotlin
implementation("androidx.room:room-runtime:...")
implementation("androidx.room:room-ktx:...")
kapt("androidx.room:room-compiler:...")
```

**Dependencies to Add:**
```kotlin
implementation(project(":Modules:VoiceOS:core:database"))
```

---

### Phase 6: Verify and Test (10 min)

1. Run `./gradlew :Modules:VoiceOS:apps:LearnApp:assembleDebug`
2. Verify zero Room imports: `grep -r "androidx.room" apps/LearnApp/`
3. Run unit tests: `./gradlew :Modules:VoiceOS:apps:LearnApp:test`

---

## 4. DAO Method to SQLDelight Query Mapping

| Room DAO Method | SQLDelight Query | Notes |
|-----------------|------------------|-------|
| `insertLearnedApp(app)` | `learnedAppQueries.insertLearnedApp(...)` | Map entity to params |
| `insertLearnedAppMinimal(app)` | `learnedAppQueries.insertLearnedAppMinimal(...)` | Map entity to params |
| `getLearnedApp(pkg)` | `learnedAppQueries.getLearnedApp(pkg).executeAsOneOrNull()` | Map result to entity |
| `getAllLearnedApps()` | `learnedAppQueries.getAllLearnedApps().executeAsList()` | Map results to entities |
| `updateAppHash(pkg, hash, ts)` | `learnedAppQueries.updateAppHash(hash, ts, pkg)` | Direct |
| `updateAppStats(pkg, screens, elements)` | `learnedAppQueries.updateAppStats(screens, elements, ts, pkg)` | Add timestamp |
| `deleteLearnedApp(app)` | `learnedAppQueries.deleteLearnedApp(app.packageName)` | By package name |
| `insertExplorationSession(session)` | `explorationSessionQueries.insertExplorationSession(...)` | Map entity |
| `getExplorationSession(id)` | `explorationSessionQueries.getExplorationSession(id).executeAsOneOrNull()` | Map result |
| `getSessionsForPackage(pkg)` | `explorationSessionQueries.getSessionsForPackage(pkg).executeAsList()` | Map results |
| `updateSessionStatus(id, status, completedAt, durationMs)` | `explorationSessionQueries.updateSessionStatus(status, completedAt, durationMs, id)` | Direct |
| `deleteSessionsForPackage(pkg)` | `explorationSessionQueries.deleteSessionsForPackage(pkg)` | Direct |
| `insertNavigationEdge(edge)` | `navigationEdgeQueries.insertNavigationEdge(...)` | Map entity |
| `insertNavigationEdges(edges)` | Loop `navigationEdgeQueries.insertNavigationEdge(...)` | SQLDelight no batch |
| `getNavigationGraph(pkg)` | `navigationEdgeQueries.getNavigationGraph(pkg).executeAsList()` | Map results |
| `getOutgoingEdges(hash)` | `navigationEdgeQueries.getOutgoingEdges(hash).executeAsList()` | Map results |
| `getIncomingEdges(hash)` | `navigationEdgeQueries.getIncomingEdges(hash).executeAsList()` | Map results |
| `getEdgesForSession(id)` | `navigationEdgeQueries.getEdgesForSession(id).executeAsList()` | Map results |
| `deleteNavigationGraph(pkg)` | `navigationEdgeQueries.deleteNavigationGraph(pkg)` | Direct |
| `insertScreenState(state)` | `screenStateQueries.insertScreenState(...)` | Map entity |
| `getScreenState(hash)` | `screenStateQueries.getScreenState(hash).executeAsOneOrNull()` | Map result |
| `getScreenStatesForPackage(pkg)` | `screenStateQueries.getScreenStatesForPackage(pkg).executeAsList()` | Map results |
| `deleteScreenStatesForPackage(pkg)` | `screenStateQueries.deleteScreenStatesForPackage(pkg)` | Direct |
| `getTotalScreensForPackage(pkg)` | `screenStateQueries.getTotalScreensForPackage(pkg).executeAsOne()` | Returns Long |
| `getTotalEdgesForPackage(pkg)` | `navigationEdgeQueries.getTotalEdgesForPackage(pkg).executeAsOne()` | Returns Long |
| `getLastSessionWithStatus(pkg, status)` | `explorationSessionQueries.getActiveSessions().executeAsList().firstOrNull()` | Filter in code |

---

## 5. Risk Assessment

| Risk | Mitigation |
|------|------------|
| Foreign key cascade behavior differs | SQLDelight uses same CASCADE DELETE - verified in .sq files |
| Transaction handling differs | Use `database.transaction { }` wrapper |
| Type differences (Long vs Int) | Cast appropriately in mapper functions |
| Build breaks from missing Room | Verify all usages migrated before removing |

---

## 6. Rollback Plan

If issues arise:
1. Keep Room files in VCS history (soft delete)
2. Can revert by restoring Room files and reverting build.gradle.kts
3. No database migration needed - separate databases

---

## 7. Success Criteria

| Criterion | Validation |
|-----------|------------|
| Zero Room imports | `grep -r "androidx.room" apps/LearnApp/` returns empty |
| Build passes | `./gradlew :Modules:VoiceOS:apps:LearnApp:assembleDebug` |
| All tests pass | `./gradlew :Modules:VoiceOS:apps:LearnApp:test` |
| Functional parity | Manual test: Learn app, verify persistence |

---

## 8. Timeline

| Phase | Duration | Status |
|-------|----------|--------|
| Phase 1: Create Repository | 15 min | ✅ COMPLETE |
| Phase 2: Convert Entities | 10 min | ✅ COMPLETE |
| Phase 3: Delete Room Files | 5 min | ✅ COMPLETE |
| Phase 4: Update References | 10 min | ✅ COMPLETE |
| Phase 5: Build Config | 5 min | ✅ COMPLETE |
| Phase 6: Verify | 10 min | ✅ COMPLETE |
| **Total** | **~55 min** | ✅ COMPLETE |

---

**Document History:**

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2025-12-18 | Manoj Jhawar | Initial plan |
