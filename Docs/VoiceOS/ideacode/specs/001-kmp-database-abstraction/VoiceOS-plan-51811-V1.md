# KMP Database Abstraction - Implementation Plan

**Feature ID:** 001
**Created:** 2025-11-18
**Profile:** android-app
**Estimated Effort:** 60-86 hours (2-3 weeks)
**Complexity Tier:** 3

---

## Executive Summary

Implement repository abstraction layer for gradual Room to SQLDelight migration. This includes DTOs, repository interfaces, and implementations for both database systems.

---

## Implementation Phases

### Phase 1: Entity Audit & Consolidation (8-12 hours)

**Tasks:**

1. **Audit all Room entities**
   - [ ] List VoiceDataManager entities (14 tables)
   - [ ] List VoiceOSCore entities (10+ tables)
   - [ ] List LearnApp entities (4 tables)
   - [ ] Document field differences

2. **Create entity mapping document**
   - [ ] Map Room entities to SQLDelight schemas
   - [ ] Document type conversions
   - [ ] Identify missing fields in SQLDelight

3. **Update SQLDelight schemas if needed**
   - [ ] Add missing columns
   - [ ] Verify type mappings
   - [ ] Add required queries

**Deliverables:**
- `docs/entity-mapping.md`
- Updated .sq files (if needed)

---

### Phase 2: DTO Layer (4-6 hours)

**Tasks:**

Create DTOs in `libraries/core/database/src/commonMain/kotlin/com/augmentalis/database/dto/`:

1. **CustomCommandDTO.kt**
   ```kotlin
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

2. **CommandHistoryDTO.kt** - Command execution records
3. **UserPreferenceDTO.kt** - Key-value preferences
4. **ScrapedAppDTO.kt** - App metadata for scraping
5. **GeneratedCommandDTO.kt** - AI-generated commands
6. **UserInteractionDTO.kt** - User interaction logs
7. **ErrorReportDTO.kt** - Error reports
8. **DeviceProfileDTO.kt** - Device information

**Mapping Extensions:**
- `fun Custom_command.toDTO(): CustomCommandDTO`
- `fun CustomCommandDTO.toSQLDelight(): Custom_command`

**Deliverables:**
- 8 DTO files
- Mapping extensions in each file

---

### Phase 3: Repository Interfaces (4-6 hours)

**Tasks:**

Create interfaces in `libraries/core/database/src/commonMain/kotlin/com/augmentalis/database/repositories/`:

1. **ICommandRepository.kt**
   ```kotlin
   interface ICommandRepository {
       suspend fun insert(command: CustomCommandDTO): Long
       suspend fun getById(id: Long): CustomCommandDTO?
       suspend fun getAll(): List<CustomCommandDTO>
       suspend fun getActive(): List<CustomCommandDTO>
       suspend fun getMostUsed(limit: Int): List<CustomCommandDTO>
       suspend fun getByLanguage(language: String): List<CustomCommandDTO>
       suspend fun searchByPhrase(phrase: String): List<CustomCommandDTO>
       suspend fun update(command: CustomCommandDTO)
       suspend fun delete(id: Long)
       suspend fun incrementUsage(id: Long)
       suspend fun count(): Long
   }
   ```

2. **ICommandHistoryRepository.kt**
   - getAll, getByTimeRange, getSuccessful, getByEngine
   - getSuccessRate, cleanupOldEntries, count

3. **IUserPreferenceRepository.kt**
   - getValue, setValue, exists, delete, getAll

4. **IAnalyticsRepository.kt**
   - recordUsage, getStats, getByPeriod, aggregate

5. **IScrapingRepository.kt**
   - Apps: getByPackage, getFullyLearned, updateLearningStatus
   - Elements: getByScreen, getClickable
   - Commands: getGenerated, getUserApproved

6. **IErrorReportRepository.kt**
   - insert, getUnsent, markSent, count

**Deliverables:**
- 6 interface files
- Full method signatures with KDoc

---

### Phase 4: SQLDelight Query Additions (8-12 hours)

**Tasks:**

Add missing queries to .sq files to support repository methods:

1. **CustomCommand.sq**
   ```sql
   getById:
   SELECT * FROM custom_command WHERE id = ?;

   getActive:
   SELECT * FROM custom_command WHERE isActive = 1;

   getMostUsed:
   SELECT * FROM custom_command WHERE isActive = 1 ORDER BY usageCount DESC LIMIT ?;

   getByLanguage:
   SELECT * FROM custom_command WHERE language = ?;

   searchByPhrase:
   SELECT * FROM custom_command WHERE phrases LIKE '%' || ? || '%';

   incrementUsage:
   UPDATE custom_command SET usageCount = usageCount + 1, lastUsed = ?, updatedAt = ? WHERE id = ?;

   deleteById:
   DELETE FROM custom_command WHERE id = ?;

   count:
   SELECT COUNT(*) FROM custom_command;
   ```

2. **CommandHistory.sq** - getByTimeRange, getSuccessful, getByEngine, getSuccessRate
3. **UserPreference.sq** - getValue, exists, setValue, deleteByKey
4. **GeneratedCommand.sq** - fuzzySearch, getUserApproved, getByApp
5. **ScrapedApp.sq** - getByPackage, getFullyLearned, updateLearningStatus
6. **ScrapedElement.sq** - getByScreen, getClickable, getInteractive
7. **UserInteraction.sq** - getByScreen, getByElement, getRecent
8. **ErrorReport.sq** - getUnsent, markSent
9. **UsageStatistic.sq** - getByPeriod, aggregate

**Deliverables:**
- Updated .sq files with all required queries
- Verify compilation with `./gradlew :libraries:core:database:generateSqlDelightInterface`

---

### Phase 5: SQLDelight Repository Implementations (12-16 hours)

**Tasks:**

Create implementations in `libraries/core/database/src/commonMain/kotlin/com/augmentalis/database/repositories/impl/`:

1. **SQLDelightCommandRepository.kt**
   ```kotlin
   class SQLDelightCommandRepository(
       private val database: VoiceOSDatabase
   ) : ICommandRepository {

       private val queries = database.customCommandQueries

       override suspend fun insert(command: CustomCommandDTO): Long =
           withContext(Dispatchers.Default) {
               queries.insert(
                   name = command.name,
                   description = command.description,
                   phrases = command.phrases.joinToString(","),
                   action = command.action,
                   parameters = command.parameters,
                   language = command.language,
                   isActive = if (command.isActive) 1L else 0L,
                   usageCount = command.usageCount,
                   lastUsed = command.lastUsed,
                   createdAt = command.createdAt,
                   updatedAt = command.updatedAt
               )
               queries.lastInsertRowId().executeAsOne()
           }

       override suspend fun getById(id: Long): CustomCommandDTO? =
           withContext(Dispatchers.Default) {
               queries.getById(id).executeAsOneOrNull()?.toDTO()
           }

       // ... implement all methods
   }
   ```

2. **SQLDelightCommandHistoryRepository.kt**
3. **SQLDelightUserPreferenceRepository.kt**
4. **SQLDelightAnalyticsRepository.kt**
5. **SQLDelightScrapingRepository.kt**
6. **SQLDelightErrorReportRepository.kt**

**Deliverables:**
- 6 implementation files
- All interface methods implemented
- Type conversions handled

---

### Phase 6: Room Bridge Implementations (8-12 hours)

**Tasks:**

Create Room bridges in `libraries/core/database/src/androidMain/kotlin/com/augmentalis/database/repositories/impl/`:

1. **RoomCommandRepository.kt**
   ```kotlin
   class RoomCommandRepository(
       private val dao: CustomCommandDao
   ) : ICommandRepository {

       override suspend fun insert(command: CustomCommandDTO): Long =
           dao.insert(command.toRoomEntity())

       override suspend fun getById(id: Long): CustomCommandDTO? =
           dao.getById(id)?.toDTO()

       // ... wrap all DAO methods
   }
   ```

2. **RoomCommandHistoryRepository.kt**
3. **RoomUserPreferenceRepository.kt**
4. **RoomAnalyticsRepository.kt**
5. **RoomScrapingRepository.kt**
6. **RoomErrorReportRepository.kt**

**Deliverables:**
- 6 Room bridge implementations
- Room entity mapping extensions

---

### Phase 7: Unit Tests (12-16 hours)

**Tasks:**

Create tests in `libraries/core/database/src/jvmTest/kotlin/com/augmentalis/database/repositories/`:

1. **CommandRepositoryTest.kt**
   ```kotlin
   class CommandRepositoryTest {
       private lateinit var database: VoiceOSDatabase
       private lateinit var repository: ICommandRepository

       @BeforeTest
       fun setup() {
           val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
           VoiceOSDatabase.Schema.create(driver)
           database = VoiceOSDatabase(driver)
           repository = SQLDelightCommandRepository(database)
       }

       @Test
       fun `insert and retrieve command`() = runTest {
           val command = CustomCommandDTO(
               name = "test",
               // ...
           )
           val id = repository.insert(command)
           val retrieved = repository.getById(id)
           assertEquals(command.name, retrieved?.name)
       }

       @Test
       fun `search by phrase`() = runTest {
           // ...
       }

       // Test all methods
   }
   ```

2. **CommandHistoryRepositoryTest.kt**
3. **UserPreferenceRepositoryTest.kt**
4. **AnalyticsRepositoryTest.kt**
5. **ScrapingRepositoryTest.kt**
6. **ErrorReportRepositoryTest.kt**

**Test Coverage Requirements:**
- 90%+ line coverage
- All CRUD operations
- Edge cases (null, empty, large data)
- Transactions
- Concurrent access

**Deliverables:**
- 6 test files
- Test report showing 90%+ coverage

---

### Phase 8: Documentation (4-6 hours)

**Tasks:**

1. **Update VoiceOSDatabaseManager**
   - Expose all repositories
   - Add factory methods

2. **Create README.md** for database module
   - Architecture overview
   - Usage examples
   - Migration guide

3. **Update KMP-MIGRATION-PLAN.md**
   - Mark Phase 1 & 2 complete
   - Update status

4. **API documentation**
   - KDoc for all public methods
   - Usage examples

**Deliverables:**
- Updated VoiceOSDatabaseManager.kt
- libraries/core/database/README.md
- Updated migration plan

---

## Quality Gates

### Per-Phase Gates
- [ ] All tasks complete
- [ ] Code compiles without warnings
- [ ] Tests pass
- [ ] 90%+ coverage for new code

### Final Gates
- [ ] All 6 repository interfaces complete
- [ ] All 6 SQLDelight implementations complete
- [ ] All 6 Room bridges complete
- [ ] All 6 test suites pass
- [ ] 90%+ overall coverage
- [ ] Documentation complete

---

## Dependencies

### Required Before Starting
- SQLDelight database module (COMPLETE)
- 18 .sq schema files (COMPLETE)
- VoiceOSDatabaseManager (COMPLETE)

### External Libraries
- kotlinx-coroutines-core
- kotlinx-serialization-json
- kotlin-test

---

## Risk Mitigation

| Risk | Mitigation |
|------|------------|
| Type conversion errors | Comprehensive unit tests for each mapping |
| Missing Room DAO methods | Audit all DAOs before starting Room bridges |
| Performance issues | Benchmark critical queries |
| Scope creep | Strict adherence to interface definitions |

---

## Execution Order

```
Day 1-2:   Phase 1 (Entity Audit)
Day 2-3:   Phase 2 (DTOs)
Day 3:     Phase 3 (Interfaces)
Day 4-5:   Phase 4 (SQLDelight Queries)
Day 6-8:   Phase 5 (SQLDelight Implementations)
Day 8-10:  Phase 6 (Room Bridges)
Day 10-12: Phase 7 (Unit Tests)
Day 12-13: Phase 8 (Documentation)
```

---

## Commit Strategy

Commit after each phase:
1. `feat(database): Add entity mapping documentation`
2. `feat(database): Add DTO classes for repository layer`
3. `feat(database): Add repository interfaces`
4. `feat(database): Add SQLDelight queries for repositories`
5. `feat(database): Implement SQLDelight repositories`
6. `feat(database): Add Room bridge implementations`
7. `test(database): Add repository unit tests`
8. `docs(database): Add README and update migration plan`

---

## Next Steps

1. Begin Phase 1: Entity Audit
2. Run `ideacode_implement` when ready to start coding
3. Follow IDE Loop: Implement → Defend → Evaluate → Commit

---

**Template Version:** Custom
**Last Updated:** 2025-11-18
