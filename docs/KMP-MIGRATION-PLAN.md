# VoiceOS KMP Database Migration Plan

**Version:** 1.0 | **Created:** 2025-11-18 | **Branch:** `kmp/main`

---

## Executive Summary

Migrate VoiceOS from Room (Android-only) to SQLDelight (Kotlin Multiplatform) database.

**Current State:** 10% complete (schemas created, zero integration)
**Target State:** 100% SQLDelight with Android, iOS, JVM support

---

## Phase Overview

| Phase | Description | Effort | Dependencies |
|-------|-------------|--------|--------------|
| 1 | Consolidate Entity Definitions | 2-3 days | None |
| 2 | Create Repository Abstraction | 3-4 days | Phase 1 |
| 3 | Complete SQLDelight Queries | 5-7 days | Phase 2 |
| 4 | Integrate with VoiceDataManager | 4-5 days | Phase 3 |
| 5 | Migrate VoiceOSCore Databases | 3-4 days | Phase 4 |
| 6 | iOS/Desktop Driver Implementation | 2-3 days | Phase 4 |
| 7 | Testing & Validation | 3-4 days | All |

**Total Estimated: 22-30 days**

---

## Phase 1: Consolidate Entity Definitions

### Problem
Three separate entity definitions for same concepts across modules.

### Tasks

#### 1.1 Audit All Entities
- [ ] List all Room entities in VoiceDataManager (14 tables)
- [ ] List all Room entities in VoiceOSCore (10+ tables)
- [ ] List all Room entities in LearnApp (4 tables)
- [ ] Map duplicates to SQLDelight schemas

#### 1.2 Unify AppEntity
**Locations:**
- `LearnApp/entities/LearnedAppEntity.kt`
- `VoiceOSCore/scraping/entities/ScrapedAppEntity.kt`
- `VoiceOSCore/database/entities/AppEntity.kt`

**Target:** `libraries/core/database/.../ScrapedApp.sq`

**Actions:**
- [ ] Compare all field definitions
- [ ] Merge into single schema
- [ ] Document field mappings

#### 1.3 Unify ScreenEntity
**Locations:**
- `LearnApp/entities/ScreenStateEntity.kt`
- `VoiceOSCore/scraping/entities/ScreenContextEntity.kt`
- `VoiceOSCore/database/entities/ScreenEntity.kt`

**Target:** `libraries/core/database/.../ScreenContext.sq`

**Actions:**
- [ ] Compare all field definitions
- [ ] Merge into single schema
- [ ] Document field mappings

#### 1.4 Unify CommandEntity
**Locations:**
- `VoiceDataManager/entities/CustomCommand.kt`
- `VoiceOSCore/scraping/entities/GeneratedCommandEntity.kt`

**Targets:**
- `CustomCommand.sq`
- `GeneratedCommand.sq`

**Actions:**
- [ ] Determine if these should remain separate
- [ ] Update schemas if needed
- [ ] Document relationships

#### 1.5 Verify Type Mappings
- [ ] Room Integer (0/1) → SQLDelight INTEGER
- [ ] Room String → SQLDelight TEXT
- [ ] Room TypeConverters → SQLDelight adapters
- [ ] Create adapter implementations

### Acceptance Criteria
- [ ] Single source of truth for each entity type
- [ ] All SQLDelight schemas match merged definitions
- [ ] Type adapters created for complex types
- [ ] Documentation complete

---

## Phase 2: Create Repository Abstraction

### Problem
All database access is direct DAO calls. Cannot swap Room for SQLDelight.

### Tasks

#### 2.1 Define Repository Interfaces

**Create in `libraries/core/database/src/commonMain/kotlin/.../repositories/`:**

```kotlin
// ICommandRepository.kt
interface ICommandRepository {
    suspend fun insert(command: CustomCommandDTO): Long
    suspend fun getById(id: Long): CustomCommandDTO?
    suspend fun getAll(): List<CustomCommandDTO>
    suspend fun getActive(): List<CustomCommandDTO>
    suspend fun getMostUsed(limit: Int): List<CustomCommandDTO>
    suspend fun update(command: CustomCommandDTO)
    suspend fun delete(id: Long)
    suspend fun incrementUsage(id: Long)
}
```

**Files to create:**
- [ ] `ICommandRepository.kt`
- [ ] `ICommandHistoryRepository.kt`
- [ ] `IUserPreferenceRepository.kt`
- [ ] `IAnalyticsRepository.kt`
- [ ] `IScrapingRepository.kt`
- [ ] `IExplorationRepository.kt`
- [ ] `IErrorReportRepository.kt`
- [ ] `IDeviceProfileRepository.kt`

#### 2.2 Create DTO Classes

**Create in `libraries/core/database/src/commonMain/kotlin/.../dto/`:**

DTOs are platform-agnostic data classes:

```kotlin
// CustomCommandDTO.kt
data class CustomCommandDTO(
    val id: Long = 0,
    val name: String,
    val description: String?,
    val phrases: List<String>,
    val action: String,
    val parameters: String?,
    val language: String,
    val isActive: Boolean,
    val usageCount: Long,
    val lastUsed: Long?,
    val createdAt: Long,
    val updatedAt: Long
)
```

**Files to create:**
- [ ] `CustomCommandDTO.kt`
- [ ] `CommandHistoryDTO.kt`
- [ ] `UserPreferenceDTO.kt`
- [ ] `ScrapedAppDTO.kt`
- [ ] `GeneratedCommandDTO.kt`
- [ ] `UserInteractionDTO.kt`
- [ ] `ErrorReportDTO.kt`
- [ ] (and others as needed)

#### 2.3 Implement SQLDelight Repositories

**Create in `libraries/core/database/src/commonMain/kotlin/.../repositories/impl/`:**

```kotlin
// SQLDelightCommandRepository.kt
class SQLDelightCommandRepository(
    private val database: VoiceOSDatabase
) : ICommandRepository {

    private val queries = database.customCommandQueries

    override suspend fun insert(command: CustomCommandDTO): Long {
        queries.insert(
            name = command.name,
            description = command.description,
            // ... map all fields
        )
        return queries.lastInsertRowId().executeAsOne()
    }

    override suspend fun getById(id: Long): CustomCommandDTO? {
        return queries.getById(id).executeAsOneOrNull()?.toDTO()
    }

    // ... implement all methods
}
```

**Files to create:**
- [ ] `SQLDelightCommandRepository.kt`
- [ ] `SQLDelightCommandHistoryRepository.kt`
- [ ] `SQLDelightUserPreferenceRepository.kt`
- [ ] `SQLDelightAnalyticsRepository.kt`
- [ ] `SQLDelightScrapingRepository.kt`
- [ ] (and others)

#### 2.4 Implement Room Repositories (Bridge)

**Create in `libraries/core/database/src/androidMain/kotlin/.../repositories/impl/`:**

```kotlin
// RoomCommandRepository.kt
class RoomCommandRepository(
    private val dao: CustomCommandDao
) : ICommandRepository {

    override suspend fun insert(command: CustomCommandDTO): Long {
        return dao.insert(command.toRoomEntity())
    }

    override suspend fun getById(id: Long): CustomCommandDTO? {
        return dao.getById(id)?.toDTO()
    }

    // ... implement all methods
}
```

This allows gradual migration from Room to SQLDelight.

### Acceptance Criteria
- [ ] All repository interfaces defined
- [ ] All DTOs created
- [ ] SQLDelight implementations complete
- [ ] Room bridge implementations complete
- [ ] Unit tests for all repositories
- [ ] 90%+ code coverage

---

## Phase 3: Complete SQLDelight Queries

### Problem
SQLDelight schemas exist but most queries are missing.

### Tasks

#### 3.1 Audit Existing Room DAOs

**VoiceDataManager DAOs to map:**
- [ ] `CustomCommandDao.kt`
- [ ] `CommandHistoryDao.kt`
- [ ] `UserPreferenceDao.kt`
- [ ] `DeviceProfileDao.kt`
- [ ] `TouchGestureDao.kt`
- [ ] `LanguageModelDao.kt`
- [ ] `ErrorReportDao.kt`
- [ ] `RecognitionLearningDao.kt`
- [ ] `GestureLearningDao.kt`
- [ ] `UsageStatisticDao.kt`
- [ ] `ScrappedCommandDao.kt`
- [ ] `AnalyticsSettingsDao.kt`
- [ ] `RetentionSettingsDao.kt`

**VoiceOSCore DAOs to map:**
- [ ] `GeneratedCommandDao.kt`
- [ ] `ScrapedAppDao.kt`
- [ ] `ScrapedElementDao.kt`
- [ ] `ScreenContextDao.kt`
- [ ] `UserInteractionDao.kt`
- [ ] `ElementStateHistoryDao.kt`
- [ ] `ScreenTransitionDao.kt`

#### 3.2 Add Missing Queries

**Example - CustomCommand.sq additions:**

```sql
-- Get by ID
getById:
SELECT * FROM custom_command WHERE id = ?;

-- Get active commands
getActive:
SELECT * FROM custom_command WHERE isActive = 1;

-- Get most used
getMostUsed:
SELECT * FROM custom_command
WHERE isActive = 1
ORDER BY usageCount DESC
LIMIT ?;

-- Search by phrase
searchByPhrase:
SELECT * FROM custom_command
WHERE phrases LIKE '%' || ? || '%';

-- Get by language
getByLanguage:
SELECT * FROM custom_command WHERE language = ?;

-- Update usage
incrementUsage:
UPDATE custom_command
SET usageCount = usageCount + 1, lastUsed = ?, updatedAt = ?
WHERE id = ?;

-- Delete by ID
deleteById:
DELETE FROM custom_command WHERE id = ?;

-- Get count
count:
SELECT COUNT(*) FROM custom_command;

-- Last insert ID
lastInsertRowId:
SELECT last_insert_rowid();
```

#### 3.3 Files to Update

- [ ] `CommandHistory.sq` - Add getByTimeRange, getSuccessful, getByEngine, etc.
- [ ] `CustomCommand.sq` - Add getById, getActive, getMostUsed, etc.
- [ ] `RecognitionLearning.sq` - Add getByEngine, getStats, etc.
- [ ] `GeneratedCommand.sq` - Add fuzzySearch, getUserApproved, etc.
- [ ] `ScrapedApp.sq` - Add getByPackage, getFullyLearned, etc.
- [ ] `UserInteraction.sq` - Add getByScreen, getByElement, etc.
- [ ] `ElementStateHistory.sq` - Add getByElement, getRecent, etc.
- [ ] `Settings.sq` - Add getAnalytics, getRetention, etc.
- [ ] `DeviceProfile.sq` - Add getActive, getByType, etc.
- [ ] `TouchGesture.sq` - Add getByType, getRecent, etc.
- [ ] `UserPreference.sq` - Add getValue, exists, etc.
- [ ] `ErrorReport.sq` - Add getUnsent, markSent, etc.
- [ ] `LanguageModel.sq` - Add getActive, getByLanguage, etc.
- [ ] `GestureLearning.sq` - Add getByGesture, getStats, etc.
- [ ] `UsageStatistic.sq` - Add getByPeriod, aggregate, etc.
- [ ] `ScrappedCommand.sq` - Add getByApp, getByScreen, etc.
- [ ] `ScrapedElement.sq` - Add getByScreen, getClickable, etc.
- [ ] `ScreenContext.sq` - Add getByApp, getRecent, etc.
- [ ] `ScreenTransition.sq` - Add getFromScreen, getToScreen, etc.

### Acceptance Criteria
- [ ] All DAO methods have corresponding SQLDelight queries
- [ ] Complex queries (JOINs, aggregations) working
- [ ] Unit tests for all queries
- [ ] Performance comparable to Room

---

## Phase 4: Integrate with VoiceDataManager

### Problem
VoiceDataManager uses Room directly. Need to use repository abstraction.

### Tasks

#### 4.1 Update DatabaseModule

**File:** `modules/managers/VoiceDataManager/.../core/DatabaseModule.kt`

```kotlin
class DatabaseModule(private val context: Context) {

    // Legacy Room (for gradual migration)
    private lateinit var roomDatabase: VoiceOSDatabase

    // New SQLDelight
    private lateinit var sqlDelightDatabase: com.augmentalis.database.VoiceOSDatabase
    private lateinit var databaseManager: VoiceOSDatabaseManager

    // Repositories (use interface types)
    lateinit var commandRepository: ICommandRepository
    lateinit var commandHistoryRepository: ICommandHistoryRepository
    lateinit var userPreferenceRepository: IUserPreferenceRepository
    // ... other repositories

    fun init() {
        // Initialize Room (legacy)
        roomDatabase = VoiceOSDatabase.getInstance(context)

        // Initialize SQLDelight
        val driverFactory = DatabaseDriverFactory(context)
        databaseManager = VoiceOSDatabaseManager(driverFactory)

        // Use SQLDelight repositories
        commandRepository = SQLDelightCommandRepository(databaseManager)
        commandHistoryRepository = databaseManager.commandHistory
        userPreferenceRepository = SQLDelightUserPreferenceRepository(databaseManager)

        // Or use Room repositories during transition:
        // commandRepository = RoomCommandRepository(roomDatabase.customCommandDao())
    }
}
```

#### 4.2 Update All Callers

Replace direct DAO access with repository calls:

**Before:**
```kotlin
DatabaseManager.database.customCommandDao().insert(command)
```

**After:**
```kotlin
DatabaseManager.commandRepository.insert(commandDTO)
```

**Files to update:**
- [ ] All files in `VoiceDataManager/` using DAOs
- [ ] `CommandExecutor.kt`
- [ ] `VoiceProcessor.kt`
- [ ] `AnalyticsManager.kt`
- [ ] Any service classes

#### 4.3 Data Migration

If switching from Room to SQLDelight on existing devices:

```kotlin
suspend fun migrateRoomToSQLDelight() {
    // Read all data from Room
    val commands = roomDatabase.customCommandDao().getAll()
    val history = roomDatabase.commandHistoryDao().getAll()
    // ... other tables

    // Write to SQLDelight
    databaseManager.transaction {
        commands.forEach { commandRepository.insert(it.toDTO()) }
        history.forEach { commandHistoryRepository.insert(it.toDTO()) }
        // ... other tables
    }

    // Optionally: Clear Room database after successful migration
}
```

### Acceptance Criteria
- [ ] VoiceDataManager uses repository interfaces
- [ ] Both Room and SQLDelight repositories work
- [ ] Data migration tested
- [ ] All existing tests pass
- [ ] No regressions in functionality

---

## Phase 5: Migrate VoiceOSCore Databases

### Problem
VoiceOSCore has 3 separate Room databases that need consolidation.

### Tasks

#### 5.1 Audit Existing Databases

**VoiceOSAppDatabase (v4):**
- AppEntity
- ScreenEntity
- ElementEntity
- Migration history: v1→v4

**AppScrapingDatabase (v10):**
- ScrapedAppEntity
- ScreenContextEntity
- ScrapedElementEntity
- GeneratedCommandEntity
- UserInteractionEntity
- ElementStateHistoryEntity
- Migration history: v1→v10 (9 migrations!)

**WebScrapingDatabase:**
- WebElementEntity
- WebPageEntity

#### 5.2 Map to SQLDelight

| Room Database | Room Entity | SQLDelight Table |
|---------------|-------------|------------------|
| VoiceOSAppDatabase | AppEntity | ScrapedApp |
| VoiceOSAppDatabase | ScreenEntity | ScreenContext |
| AppScrapingDatabase | ScrapedElementEntity | ScrapedElement |
| AppScrapingDatabase | GeneratedCommandEntity | GeneratedCommand |
| AppScrapingDatabase | UserInteractionEntity | UserInteraction |
| AppScrapingDatabase | ElementStateHistoryEntity | ElementStateHistory |
| AppScrapingDatabase | ScreenTransitionEntity | ScreenTransition |

#### 5.3 Handle Migrations

The 9 Room migrations must be preserved or converted:

```kotlin
// Room migration v1→v2 (major change)
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Changed from element_id to element_hash
        database.execSQL("ALTER TABLE ...")
    }
}
```

**Options:**
1. **Fresh start** - New installs use SQLDelight, no migration
2. **One-time migration** - Read Room, write SQLDelight, delete Room
3. **Parallel operation** - Both databases during transition

#### 5.4 Update VoiceOSCore

Replace database access in:
- [ ] `AccessibilityScrapingService.kt`
- [ ] `ScreenAnalyzer.kt`
- [ ] `CommandGenerator.kt`
- [ ] `ExplorationManager.kt`
- [ ] All scraping-related files

### Acceptance Criteria
- [ ] Single SQLDelight database replaces 3 Room databases
- [ ] Data migration path defined and tested
- [ ] All VoiceOSCore features work with new database
- [ ] Migration history preserved for existing users

---

## Phase 6: iOS/Desktop Driver Implementation

### Problem
iOS and JVM drivers are stubs. Need actual implementations.

### Tasks

#### 6.1 iOS Driver

**File:** `libraries/core/database/src/iosMain/kotlin/.../DatabaseFactory.ios.kt`

```kotlin
actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        return NativeSqliteDriver(
            schema = VoiceOSDatabase.Schema,
            name = "VoiceOS.db"
        )
    }
}
```

**Requirements:**
- [ ] Add SQLDelight iOS dependencies
- [ ] Test on iOS simulator
- [ ] Handle iOS file system paths
- [ ] Encryption support (if needed)

#### 6.2 JVM Driver (for tests and desktop)

**File:** `libraries/core/database/src/jvmMain/kotlin/.../DatabaseFactory.jvm.kt`

```kotlin
actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        return JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also {
            VoiceOSDatabase.Schema.create(it)
        }
    }
}
```

**Requirements:**
- [ ] Support both in-memory and file-based
- [ ] Enable for unit testing
- [ ] Desktop app support (if needed)

#### 6.3 Shared Test Suite

Create tests that run on all platforms:

```kotlin
// commonTest
class DatabaseTests {
    @Test
    fun testInsertAndRetrieve() {
        val factory = DatabaseDriverFactory()
        val db = createDatabase(factory)

        db.commandHistoryQueries.insert(...)
        val result = db.commandHistoryQueries.getAll().executeAsList()

        assertEquals(1, result.size)
    }
}
```

### Acceptance Criteria
- [ ] iOS driver working on simulator
- [ ] JVM driver working for tests
- [ ] Shared test suite passes on all platforms
- [ ] No platform-specific bugs

---

## Phase 7: Testing & Validation

### Tasks

#### 7.1 Unit Tests
- [ ] All repositories have 90%+ coverage
- [ ] All queries tested
- [ ] Edge cases covered
- [ ] Performance benchmarks

#### 7.2 Integration Tests
- [ ] End-to-end flows work
- [ ] Data persistence verified
- [ ] Concurrent access tested
- [ ] Transaction rollback tested

#### 7.3 Migration Tests
- [ ] Room to SQLDelight migration works
- [ ] Data integrity verified
- [ ] No data loss
- [ ] Rollback capability

#### 7.4 Platform Tests
- [ ] Android device testing
- [ ] iOS simulator testing
- [ ] JVM test suite
- [ ] Performance comparison

#### 7.5 Regression Tests
- [ ] All existing features work
- [ ] Voice commands work
- [ ] Scraping works
- [ ] LearnApp works

### Acceptance Criteria
- [ ] 90%+ code coverage
- [ ] All tests pass on all platforms
- [ ] Performance equal or better than Room
- [ ] No regressions

---

## Risk Mitigation

### Risk: Data Loss During Migration
**Mitigation:**
- Backup Room database before migration
- Verify data integrity after migration
- Keep Room database until confirmed working

### Risk: Performance Regression
**Mitigation:**
- Benchmark critical queries
- Use indexes appropriately
- Test with production data volumes

### Risk: Breaking Changes
**Mitigation:**
- Parallel worktrees (Room stable, KMP development)
- Feature flags for gradual rollout
- Keep Room version buildable

---

## Success Metrics

| Metric | Target |
|--------|--------|
| Test Coverage | 90%+ |
| Query Performance | ≤ Room performance |
| Build Success | All platforms |
| Data Integrity | 100% |
| Feature Parity | 100% |

---

## Quick Reference: File Locations

### New Files to Create
```
libraries/core/database/src/commonMain/kotlin/com/augmentalis/database/
├── dto/
│   ├── CustomCommandDTO.kt
│   ├── CommandHistoryDTO.kt
│   └── ... (8-10 more)
├── repositories/
│   ├── ICommandRepository.kt
│   ├── ICommandHistoryRepository.kt
│   └── ... (6-8 more)
└── repositories/impl/
    ├── SQLDelightCommandRepository.kt
    └── ... (6-8 more)

libraries/core/database/src/androidMain/kotlin/com/augmentalis/database/
└── repositories/impl/
    ├── RoomCommandRepository.kt
    └── ... (bridge implementations)
```

### Files to Modify
```
modules/managers/VoiceDataManager/src/main/java/com/augmentalis/datamanager/
├── core/DatabaseModule.kt
├── core/DatabaseManager.kt
└── ... (all DAO callers)

modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/
├── scraping/...
├── database/...
└── ... (all database callers)
```

---

**Author:** VoiceOS Team
**Last Updated:** 2025-11-18
