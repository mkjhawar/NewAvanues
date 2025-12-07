# Feature Specification: KMP Database Abstraction Layer

**Feature ID:** 001-kmp-database-abstraction
**Version:** 1.0
**Created:** 2025-11-18
**Profile:** android-app
**Complexity:** Tier 3 (Full IDEACODE)

---

## Executive Summary

Create a repository abstraction layer that enables gradual migration from Room (Android-only) to SQLDelight (Kotlin Multiplatform). This includes consolidating duplicate entity definitions across VoiceDataManager, VoiceOSCore, and LearnApp, creating common DTOs, and implementing repository interfaces with both Room and SQLDelight implementations.

---

## Problem Statement

### Current State
- **3 parallel Room databases** with duplicate entity definitions
- **Direct DAO access** throughout codebase (no abstraction)
- **Cannot swap** Room for SQLDelight without rewriting all call sites
- **SQLDelight schemas exist** but database never instantiated (10% complete)

### Pain Points
1. Same concepts defined 3x (AppEntity, ScreenEntity, Commands)
2. No way to test database layer independently
3. Cannot gradually migrate - all or nothing
4. Platform lock-in to Android

### Desired State
- Single source of truth for each entity type
- Repository interfaces abstract database implementation
- Can run Room and SQLDelight in parallel during migration
- Clean separation enables testing and platform portability

---

## Requirements

### Functional Requirements

#### FR-1: Entity Consolidation
- **FR-1.1:** Merge AppEntity definitions (LearnedAppEntity, ScrapedAppEntity, AppEntity) into unified ScrapedApp
- **FR-1.2:** Merge ScreenEntity definitions (ScreenStateEntity, ScreenContextEntity, ScreenEntity) into unified ScreenContext
- **FR-1.3:** Clarify Command entity relationships (CustomCommand vs GeneratedCommand)
- **FR-1.4:** Create field mappings documentation for each merged entity

#### FR-2: DTO Layer
- **FR-2.1:** Create platform-agnostic DTOs in commonMain
- **FR-2.2:** DTOs must have no Room or SQLDelight dependencies
- **FR-2.3:** Provide mapping extensions (toDTO, toRoomEntity, toSQLDelightEntity)
- **FR-2.4:** DTOs for: CustomCommand, CommandHistory, UserPreference, ScrapedApp, GeneratedCommand, UserInteraction, ErrorReport, DeviceProfile

#### FR-3: Repository Interfaces
- **FR-3.1:** Create ICommandRepository with full CRUD operations
- **FR-3.2:** Create ICommandHistoryRepository with time-range queries
- **FR-3.3:** Create IUserPreferenceRepository with key-value access
- **FR-3.4:** Create IAnalyticsRepository for usage statistics
- **FR-3.5:** Create IScrapingRepository for element/screen data
- **FR-3.6:** All repository methods must be suspend functions
- **FR-3.7:** Methods must use DTO types (not Room/SQLDelight entities)

#### FR-4: SQLDelight Repository Implementations
- **FR-4.1:** Implement all repository interfaces using SQLDelight queries
- **FR-4.2:** Add missing queries to .sq files to support repository methods
- **FR-4.3:** Handle type conversions (JSON strings, timestamps)
- **FR-4.4:** Support transactions via database manager

#### FR-5: Room Repository Implementations (Bridge)
- **FR-5.1:** Implement repository interfaces wrapping existing Room DAOs
- **FR-5.2:** Enable parallel operation (both implementations active)
- **FR-5.3:** Allow switching between implementations via configuration

### Non-Functional Requirements

#### NFR-1: Performance
- Query performance must be equal to or better than direct DAO access
- Bulk operations must use transactions
- No N+1 query patterns

#### NFR-2: Testing
- 90%+ code coverage for all repositories
- Unit tests run on JVM (not Android emulator)
- Integration tests verify both Room and SQLDelight implementations

#### NFR-3: Maintainability
- Clear separation between interfaces and implementations
- No leaky abstractions (Room/SQLDelight types in interfaces)
- Consistent naming conventions

#### NFR-4: Backward Compatibility
- Existing VoiceDataManager callers continue to work
- No breaking changes to public APIs during migration
- Room database remains functional

### Success Criteria

- [ ] All duplicate entities consolidated into single definitions
- [ ] 8+ repository interfaces created with full method signatures
- [ ] SQLDelight implementations pass all unit tests
- [ ] Room bridge implementations pass same tests
- [ ] VoiceDataManager compiles with new repository usage
- [ ] 90%+ code coverage achieved
- [ ] Performance benchmarks show no regression

---

## User Stories

### US-1: Developer Migration
**As a** VoiceOS developer
**I want to** use repository interfaces instead of direct DAO access
**So that** I can gradually migrate from Room to SQLDelight

**Acceptance Criteria:**
- Can import ICommandRepository without Room/SQLDelight dependencies
- Can inject either implementation via DI
- IDE autocomplete shows all available methods

### US-2: Cross-Platform Database
**As a** VoiceOS architect
**I want to** share database code between Android and iOS
**So that** we can build iOS version without rewriting database layer

**Acceptance Criteria:**
- DTOs compile in commonMain
- Repository interfaces compile in commonMain
- SQLDelight implementations use expect/actual for drivers

### US-3: Testable Database Layer
**As a** VoiceOS tester
**I want to** run database tests on JVM
**So that** tests are fast and don't require Android emulator

**Acceptance Criteria:**
- All repository tests run with ./gradlew jvmTest
- Tests complete in <30 seconds
- In-memory SQLite used for tests

### US-4: Data Consistency
**As a** VoiceOS user
**I want** my data to remain intact during app updates
**So that** I don't lose commands, history, or preferences

**Acceptance Criteria:**
- Migration preserves all existing Room data
- No data loss or corruption
- Rollback possible if issues detected

---

## Technical Constraints

### Android-Specific (from profile: android-app)
- Minimum API level: 24 (Android 7.0)
- Target API level: 34 (Android 14)
- Must support Room 2.6.1 during transition
- SQLDelight 2.0.1 for KMP
- Coroutines for async operations (Dispatchers.IO)

### KMP Structure
```
libraries/core/database/
├── src/
│   ├── commonMain/kotlin/com/augmentalis/database/
│   │   ├── dto/                    # Platform-agnostic DTOs
│   │   ├── repositories/           # Interfaces
│   │   └── repositories/impl/      # SQLDelight implementations
│   ├── androidMain/kotlin/com/augmentalis/database/
│   │   └── repositories/impl/      # Room bridge implementations
│   ├── iosMain/                    # iOS driver
│   └── jvmMain/                    # JVM test driver
```

### Dependencies
- SQLDelight 2.0.1
- Kotlinx.coroutines 1.7.3
- Kotlinx.serialization 1.6.0 (for JSON adapters)

---

## Dependencies

### Internal Dependencies
- SQLDelight database module (libraries/core/database) - COMPLETE
- 18 .sq schema files - COMPLETE
- VoiceOSDatabaseManager - COMPLETE
- CommandHistoryRepository (basic version) - COMPLETE

### External Dependencies
- SQLDelight runtime
- SQLite drivers (Android, iOS, JVM)
- Kotlinx.coroutines

### Blocked By
- None (Phase 1, no prior phases)

### Blocks
- Phase 4: VoiceDataManager integration
- Phase 5: VoiceOSCore migration
- Phase 6: iOS/Desktop drivers

---

## Out of Scope

1. **VoiceDataManager refactoring** - Separate phase (Phase 4)
2. **VoiceOSCore database consolidation** - Separate phase (Phase 5)
3. **iOS driver implementation** - Separate phase (Phase 6)
4. **Data migration scripts** - Will be part of integration phases
5. **UI changes** - No user-facing changes in this phase
6. **Room removal** - Room remains for bridge implementations

---

## Risks and Mitigations

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| Type conversion errors | Medium | High | Comprehensive unit tests for all type mappings |
| Performance regression | Low | High | Benchmark critical queries before/after |
| Breaking changes | Medium | High | Keep Room bridge as fallback |
| Scope creep | High | Medium | Strict adherence to "Out of Scope" list |

---

## Estimated Effort

| Task | Hours |
|------|-------|
| Entity consolidation & documentation | 8-12 |
| DTO creation (8 classes) | 4-6 |
| Repository interfaces (6 interfaces) | 4-6 |
| SQLDelight query additions | 8-12 |
| SQLDelight implementations | 12-16 |
| Room bridge implementations | 8-12 |
| Unit tests (90% coverage) | 12-16 |
| Documentation | 4-6 |

**Total: 60-86 hours (2-3 weeks)**

---

## Files to Create

### DTOs (commonMain)
- `dto/CustomCommandDTO.kt`
- `dto/CommandHistoryDTO.kt`
- `dto/UserPreferenceDTO.kt`
- `dto/ScrapedAppDTO.kt`
- `dto/GeneratedCommandDTO.kt`
- `dto/UserInteractionDTO.kt`
- `dto/ErrorReportDTO.kt`
- `dto/DeviceProfileDTO.kt`

### Interfaces (commonMain)
- `repositories/ICommandRepository.kt`
- `repositories/ICommandHistoryRepository.kt`
- `repositories/IUserPreferenceRepository.kt`
- `repositories/IAnalyticsRepository.kt`
- `repositories/IScrapingRepository.kt`
- `repositories/IErrorReportRepository.kt`

### SQLDelight Implementations (commonMain)
- `repositories/impl/SQLDelightCommandRepository.kt`
- `repositories/impl/SQLDelightCommandHistoryRepository.kt`
- `repositories/impl/SQLDelightUserPreferenceRepository.kt`
- `repositories/impl/SQLDelightAnalyticsRepository.kt`
- `repositories/impl/SQLDelightScrapingRepository.kt`
- `repositories/impl/SQLDelightErrorReportRepository.kt`

### Room Bridge Implementations (androidMain)
- `repositories/impl/RoomCommandRepository.kt`
- `repositories/impl/RoomCommandHistoryRepository.kt`
- `repositories/impl/RoomUserPreferenceRepository.kt`
- `repositories/impl/RoomAnalyticsRepository.kt`
- `repositories/impl/RoomScrapingRepository.kt`
- `repositories/impl/RoomErrorReportRepository.kt`

### Tests (jvmTest)
- `repositories/CommandRepositoryTest.kt`
- `repositories/CommandHistoryRepositoryTest.kt`
- `repositories/UserPreferenceRepositoryTest.kt`
- `repositories/AnalyticsRepositoryTest.kt`
- `repositories/ScrapingRepositoryTest.kt`
- `repositories/ErrorReportRepositoryTest.kt`

---

## Approval

- [ ] Specification reviewed and approved
- [ ] Technical approach validated
- [ ] Effort estimate accepted
- [ ] Ready for `/ideacode.plan`

---

**Author:** IDEACODE v8.4
**Project:** VoiceOS
**Branch:** kmp/main
