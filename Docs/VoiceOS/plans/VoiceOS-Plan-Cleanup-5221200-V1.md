# VoiceOS-Plan-Cleanup-Restore-251222-V1.md

## Executive Summary

This plan outlines the restoration and migration of VoiceOS Cleanup functionality (P3 feature) from commit 18cfa4a7d. The original implementation used Hilt DI, Room database, and Compose UI. Current architecture uses SQLDelight (KMP), Hilt DI, and Compose Material3. Migration focuses on database layer adaptation while preserving business logic and UI patterns.

## 1. Original Implementation Analysis

### 1.1 Deleted Files (Commit 236225df7)
```
cleanup/
├── CleanupManager.kt              (Business logic, safety checks)
├── CleanupWorker.kt               (WorkManager background job)
├── di/CleanupModule.kt            (Hilt DI module)
└── ui/
    ├── CleanupPreviewActivity.kt  (Activity wrapper)
    ├── CleanupPreviewScreen.kt    (Compose UI - Material3)
    ├── CleanupPreviewUiState.kt   (UI state models)
    └── CleanupPreviewViewModel.kt (Hilt ViewModel)
```

### 1.2 Key Dependencies (Original)
- **DI**: Hilt with `@HiltViewModel`, `@AndroidEntryPoint`, `@Module`
- **Database**: Room with `IGeneratedCommandRepository` interface
- **UI**: Compose Material3 with state-based design
- **Background**: WorkManager for periodic cleanup (weekly)
- **Safety**: 90% deletion limit, grace period (1-365 days), user-approved protection

### 1.3 Critical Features
1. **Preview Mode**: Calculate statistics before deletion (dry-run)
2. **Safety Checks**: Refuse to delete >90% of commands, grace period validation
3. **Progress Tracking**: Batch deletion (1000 commands/batch) with progress callbacks
4. **VACUUM Support**: Auto-compact database after >10% deletion
5. **User Protection**: Preserve user-approved commands optionally

## 2. Current Architecture Analysis

### 2.1 Database Layer (SQLDelight)

**Current State**: All cleanup methods already exist in `IGeneratedCommandRepository`:

```kotlin
// Cleanup-specific methods (already implemented)
suspend fun deleteDeprecatedCommands(olderThan: Long, keepUserApproved: Boolean): Int
suspend fun getDeprecatedCommands(packageName: String): List<GeneratedCommandDTO>
suspend fun getDeprecatedCommandsForCleanup(packageName: String, olderThan: Long,
    keepUserApproved: Boolean, limit: Int): List<GeneratedCommandDTO>
suspend fun getAllDeprecatedCommandsByApp(): Map<String, List<GeneratedCommandDTO>>
suspend fun vacuumDatabase()
suspend fun count(): Long
```

**Migration Impact**: ✅ **ZERO CHANGES NEEDED** - All repository methods exist, only interface usage required.

### 2.2 DI Layer (Hilt)

**Current State**: Hilt configured in `build.gradle.kts`:
```kotlin
implementation(libs.hilt.android)
ksp(libs.hilt.compiler)
```

**Migration Impact**: ✅ **MINIMAL CHANGES** - CleanupModule can be reused as-is, just update import from Room to SQLDelight repository.

### 2.3 UI Layer (Compose)

**Current State**:
- Compose BOM 2024.06.00
- Material3 theme (AccessibilityTheme)
- Existing pattern: `LearnAppActivity.kt` shows ComponentActivity + setContent + Compose

**Migration Impact**: ✅ **DIRECT RESTORATION** - Original Compose code compatible, just verify theme integration.

### 2.4 WorkManager

**Current State**: WorkManager dependency present:
```kotlin
implementation(libs.androidx.work.runtime.ktx)
```

**Migration Impact**: ✅ **MINIMAL CHANGES** - Update repository initialization in CleanupWorker.

## 3. Migration Strategy

### 3.1 Phase 1: Core Business Logic (CleanupManager)

**Complexity**: LOW
**Estimated Effort**: 2 hours
**Risk**: LOW

#### Changes Required:
1. **Repository Interface**: Change from Room to SQLDelight repository
   - Original: `IGeneratedCommandRepository` (Room interface)
   - New: `IGeneratedCommandRepository` (SQLDelight interface - **same name, different implementation**)

2. **No Algorithm Changes**: Business logic remains identical
   - Safety checks (90% limit) - unchanged
   - Grace period validation - unchanged
   - Batch deletion logic - unchanged
   - VACUUM threshold - unchanged

#### File Restoration:
```
/Modules/VoiceOS/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/cleanup/
└── CleanupManager.kt  (restore from 236225df7^, update imports only)
```

#### Code Changes:
```kotlin
// OLD (Room):
import com.augmentalis.voiceoscore.database.room.repositories.IGeneratedCommandRepository

// NEW (SQLDelight):
import com.augmentalis.database.repositories.IGeneratedCommandRepository  // KMP module
```

**No other changes needed** - method signatures identical.

### 3.2 Phase 2: Dependency Injection (CleanupModule)

**Complexity**: LOW
**Estimated Effort**: 1 hour
**Risk**: LOW

#### Changes Required:
1. Update repository import (Room → SQLDelight)
2. Get repository from database singleton instead of Hilt injection

#### Original Code:
```kotlin
@Module
@InstallIn(ViewModelComponent::class)
object CleanupModule {
    @Provides
    @ViewModelScoped
    fun provideCleanupManager(
        commandRepo: IGeneratedCommandRepository  // Room injection
    ): CleanupManager {
        return CleanupManager(commandRepo)
    }
}
```

#### Migrated Code:
```kotlin
@Module
@InstallIn(ViewModelComponent::class)
object CleanupModule {
    @Provides
    @ViewModelScoped
    fun provideCleanupManager(): CleanupManager {
        // Get database singleton (KMP pattern)
        val database = VoiceOSDatabaseManager.getInstance(/* driver factory */)
        return CleanupManager(database.generatedCommands)
    }
}
```

**Issue**: DatabaseDriverFactory needs Android Context, not available in ViewModel scope.

**Solution**: Inject via Application-level module or use service pattern (see Phase 2B).

### 3.2B Alternative: Direct Repository Injection

**Better Approach**: Provide repository directly in Application-level module:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideVoiceOSDatabase(
        @ApplicationContext context: Context
    ): VoiceOSDatabase {
        val driverFactory = DatabaseDriverFactory(context)
        return VoiceOSDatabaseManager.getInstance(driverFactory)
    }

    @Provides
    @Singleton
    fun provideGeneratedCommandRepository(
        database: VoiceOSDatabase
    ): IGeneratedCommandRepository {
        return database.generatedCommands
    }
}
```

Then CleanupModule simplifies to:
```kotlin
@Module
@InstallIn(ViewModelComponent::class)
object CleanupModule {
    @Provides
    @ViewModelScoped
    fun provideCleanupManager(
        commandRepo: IGeneratedCommandRepository
    ): CleanupManager {
        return CleanupManager(commandRepo)
    }
}
```

### 3.3 Phase 3: UI Layer (Compose)

**Complexity**: LOW
**Estimated Effort**: 3 hours
**Risk**: LOW

#### 3.3A CleanupPreviewUiState.kt
**No changes needed** - Pure Kotlin data classes, no dependencies.

#### 3.3B CleanupPreviewViewModel.kt
**Changes Required**:
1. Update repository import (Room → SQLDelight)
2. Change `Application` to `Context` (lighter dependency)

#### 3.3C CleanupPreviewScreen.kt
**No changes needed** - Pure Compose UI, state-driven, no database dependencies.

#### 3.3D CleanupPreviewActivity.kt
**Changes Required**:
1. Update theme import: `MaterialTheme` → `AccessibilityTheme` (existing VoiceOS theme)
2. Add result handling for navigation back to settings

#### File Structure:
```
/Modules/VoiceOS/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/cleanup/ui/
├── CleanupPreviewActivity.kt  (restore + theme update)
├── CleanupPreviewScreen.kt    (restore as-is)
├── CleanupPreviewUiState.kt   (restore as-is)
└── CleanupPreviewViewModel.kt (restore + import update)
```

### 3.4 Phase 4: Background Job (CleanupWorker)

**Complexity**: LOW
**Estimated Effort**: 1 hour
**Risk**: LOW

#### Changes Required:
1. Update database initialization (Room → SQLDelight)

#### Original Code:
```kotlin
override suspend fun doWork(): Result {
    val database = VoiceOSDatabase.getInstance(applicationContext)  // Room
    val commandRepo = database.generatedCommandDao()
    val cleanupManager = CleanupManager(commandRepo)
    // ...
}
```

#### Migrated Code:
```kotlin
override suspend fun doWork(): Result {
    val driverFactory = DatabaseDriverFactory(applicationContext)
    val database = VoiceOSDatabaseManager.getInstance(driverFactory)
    val commandRepo = database.generatedCommands
    val cleanupManager = CleanupManager(commandRepo)
    // ...
}
```

**Note**: ListenableFuture dependency issue in `isCleanupScheduled()` - keep disabled, document as known issue.

### 3.5 Phase 5: Testing

**Complexity**: MEDIUM
**Estimated Effort**: 4 hours
**Risk**: MEDIUM

#### 5.5A Unit Tests (CleanupManager)

**Test File**: `CleanupManagerTest.kt` (new file)

**Dependencies**:
```kotlin
testImplementation(libs.junit)
testImplementation(libs.kotlinx.coroutines.test)
testImplementation(libs.mockk)
testImplementation(libs.sqldelight.sqlite.driver)  // JVM SQLite driver
```

**Test Strategy**:
1. **Mock-based**: Mock `IGeneratedCommandRepository` with MockK
2. **In-memory**: Use SQLite in-memory database for integration tests

**Test Cases**:
- ✅ Preview calculation accuracy
- ✅ Safety limit enforcement (>90% rejection)
- ✅ Grace period validation
- ✅ User-approved preservation
- ✅ Batch deletion progress
- ✅ VACUUM threshold trigger

#### 5.5B Integration Tests (CleanupManager + Repository)

**Test File**: `CleanupIntegrationTest.kt` (androidTest)

**Dependencies**:
```kotlin
androidTestImplementation(project(":Modules:VoiceOS:core:database"))
androidTestImplementation(libs.junit)
androidTestImplementation(libs.androidx.test.runner)
androidTestImplementation(libs.kotlinx.coroutines.test)
```

**Test Strategy**: Real SQLDelight database with in-memory driver

**Test Cases**:
- ✅ End-to-end cleanup workflow (preview → execute → verify)
- ✅ Database state verification
- ✅ VACUUM execution and space reclamation
- ✅ Multi-app cleanup scenarios
- ✅ Concurrent access safety

#### 5.5C UI Tests (Compose)

**Test File**: `CleanupPreviewScreenTest.kt` (androidTest)

**Dependencies**:
```kotlin
androidTestImplementation(libs.bundles.testing.compose)
androidTestImplementation(libs.bundles.compose.debug)
androidTestImplementation("com.google.dagger:hilt-android-testing:2.51.1")
```

**Test Strategy**: Compose UI testing with fake ViewModel

**Test Cases**:
- ✅ Loading state display
- ✅ Preview state statistics rendering
- ✅ Safety level indicators (Safe/Moderate/High Risk)
- ✅ Affected apps list scrolling
- ✅ Execute button interaction
- ✅ Success state display
- ✅ Error state with retry

## 4. Implementation Plan (Phased Approach)

### Phase 1: Foundation (Day 1, 4 hours)

**Goal**: Restore core business logic with SQLDelight integration

**Tasks**:
1. Create `cleanup/` package structure
2. Restore `CleanupManager.kt` with updated imports
3. Create Application-level `DatabaseModule` for repository injection
4. Restore `CleanupModule.kt` (DI)
5. Write unit tests for CleanupManager

**Deliverables**:
- ✅ CleanupManager.kt (compiled, tested)
- ✅ DatabaseModule.kt (Hilt module)
- ✅ CleanupModule.kt (Hilt module)
- ✅ CleanupManagerTest.kt (90%+ coverage)

**Validation**:
```bash
./gradlew :Modules:VoiceOS:apps:VoiceOSCore:testDebugUnitTest --tests CleanupManagerTest
```

### Phase 2: UI Layer (Day 2, 4 hours)

**Goal**: Restore Compose UI with state-based architecture

**Tasks**:
1. Restore `cleanup/ui/CleanupPreviewUiState.kt`
2. Restore `cleanup/ui/CleanupPreviewViewModel.kt` with repository injection
3. Restore `cleanup/ui/CleanupPreviewScreen.kt`
4. Restore `cleanup/ui/CleanupPreviewActivity.kt` with AccessibilityTheme
5. Add UI tests

**Deliverables**:
- ✅ Complete UI stack (Activity → Screen → ViewModel → State)
- ✅ Material3 integration with AccessibilityTheme
- ✅ CleanupPreviewScreenTest.kt

**Validation**:
```bash
./gradlew :Modules:VoiceOS:apps:VoiceOSCore:connectedDebugAndroidTest --tests CleanupPreviewScreenTest
```

### Phase 3: Background Jobs (Day 3, 2 hours)

**Goal**: Enable periodic background cleanup

**Tasks**:
1. Restore `CleanupWorker.kt` with SQLDelight database init
2. Add WorkManager scheduling logic
3. Test background execution

**Deliverables**:
- ✅ CleanupWorker.kt (weekly periodic work)
- ✅ Worker unit tests

**Validation**:
```bash
adb shell am broadcast -a androidx.work.impl.background.systemalarm.RescheduleReceiver
```

### Phase 4: Integration & Testing (Day 3-4, 4 hours)

**Goal**: End-to-end validation and integration tests

**Tasks**:
1. Create `CleanupIntegrationTest.kt` (androidTest)
2. Test cleanup workflow with real database
3. Performance benchmarks (10k+ commands)
4. Memory leak testing (LeakCanary)

**Deliverables**:
- ✅ CleanupIntegrationTest.kt (full workflow coverage)
- ✅ Performance benchmarks documented
- ✅ Memory usage validation

**Validation**:
```bash
./gradlew :Modules:VoiceOS:apps:VoiceOSCore:connectedDebugAndroidTest --tests CleanupIntegrationTest
```

### Phase 5: Documentation & Polish (Day 4, 2 hours)

**Goal**: Update documentation and user-facing features

**Tasks**:
1. Add KDoc comments for public APIs
2. Create user guide for cleanup feature
3. Add settings UI integration point
4. Update CHANGELOG.md

**Deliverables**:
- ✅ API documentation (KDoc)
- ✅ User guide (Markdown)
- ✅ Settings integration documented

## 5. Dependency Matrix

### 5.1 New Dependencies Required
**None** - All dependencies already present in `build.gradle.kts`

### 5.2 Existing Dependencies Used
```kotlin
// Database (KMP)
implementation(project(":Modules:VoiceOS:core:database"))

// DI
implementation(libs.hilt.android)
ksp(libs.hilt.compiler)

// Compose
implementation(platform(libs.compose.bom))
implementation(libs.bundles.compose)

// WorkManager
implementation(libs.androidx.work.runtime.ktx)

// Coroutines
implementation(libs.kotlinx.coroutines.android)
implementation(libs.kotlinx.coroutines.core)

// Testing
testImplementation(libs.sqldelight.sqlite.driver)
testImplementation(libs.mockk)
androidTestImplementation(libs.bundles.testing.compose)
```

## 6. Risk Assessment

### 6.1 Technical Risks

| Risk | Severity | Mitigation |
|------|----------|------------|
| Database driver initialization in DI | MEDIUM | Use Application-level singleton module |
| ListenableFuture dependency in WorkManager | LOW | Document as known issue, disable `isCleanupScheduled()` |
| Hilt ViewModel scope conflicts | LOW | Follow existing VoiceOSCore patterns |
| SQLDelight transaction differences | LOW | Repository interface abstracts implementation |
| Compose theme compatibility | LOW | Use existing AccessibilityTheme |

### 6.2 Testing Risks

| Risk | Severity | Mitigation |
|------|----------|------------|
| In-memory database state management | LOW | Use `@Before`/`@After` for cleanup |
| Coroutine test delays | LOW | Use `runTest` and virtual time |
| UI test flakiness | MEDIUM | Add explicit wait conditions |
| Integration test performance | LOW | Use smaller datasets for tests |

## 7. Breaking Changes

**None** - This is a feature restoration, not a breaking change. No existing APIs affected.

## 8. Rollback Plan

If critical issues arise:
1. **Immediate**: Comment out cleanup package in git
2. **Short-term**: Disable WorkManager scheduling
3. **Long-term**: Revert to commit before restoration (current HEAD)

**Rollback Command**:
```bash
git checkout HEAD~1 -- Modules/VoiceOS/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/cleanup/
```

## 9. Success Criteria

### 9.1 Functional Requirements
- ✅ Preview cleanup statistics (commands, apps, size)
- ✅ Execute cleanup with safety checks
- ✅ Preserve user-approved commands
- ✅ VACUUM database after cleanup
- ✅ Weekly background scheduling
- ✅ Grace period configuration (1-365 days)

### 9.2 Quality Requirements
- ✅ 90%+ test coverage for CleanupManager
- ✅ Zero compilation errors
- ✅ Zero memory leaks (LeakCanary)
- ✅ <500ms cleanup for 1000 commands
- ✅ <100ms preview calculation

### 9.3 Documentation Requirements
- ✅ KDoc for all public APIs
- ✅ User guide with screenshots
- ✅ Settings integration documented
- ✅ Migration guide from P2 to P3

## 10. Post-Implementation Tasks

1. **Performance Optimization**: Profile large database cleanups (>10k commands)
2. **UX Enhancement**: Add undo functionality (30-second window)
3. **Analytics**: Track cleanup frequency and patterns
4. **Settings UI**: Add cleanup configuration screen
5. **Notifications**: Show cleanup results to user

---

## Critical Files for Implementation

Based on this analysis, here are the 5 most critical files for implementing this plan:

- **/Volumes/M-Drive/Coding/NewAvanues/Modules/VoiceOS/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/cleanup/CleanupManager.kt** - Core business logic with safety checks, 90% deletion limit, batch processing, and VACUUM integration. Restore from git with minimal import changes (Room → SQLDelight).

- **/Volumes/M-Drive/Coding/NewAvanues/Modules/VoiceOS/core/database/src/commonMain/kotlin/com/augmentalis/database/repositories/IGeneratedCommandRepository.kt** - Repository interface with all cleanup methods already implemented (deleteDeprecatedCommands, getDeprecatedCommandsForCleanup, vacuumDatabase). **No changes needed** - validates migration feasibility.

- **/Volumes/M-Drive/Coding/NewAvanues/Modules/VoiceOS/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/cleanup/ui/CleanupPreviewViewModel.kt** - Hilt ViewModel orchestrating preview → execute workflow. Update repository import and add error handling for database initialization.

- **/Volumes/M-Drive/Coding/NewAvanues/Modules/VoiceOS/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/cleanup/ui/CleanupPreviewScreen.kt** - Material3 Compose UI with state-based rendering (Loading, Preview, Executing, Success, Error). Restore as-is, verify AccessibilityTheme integration.

- **/Volumes/M-Drive/Coding/NewAvanues/Modules/VoiceOS/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/cleanup/di/CleanupModule.kt** - Hilt DI module providing CleanupManager. Create companion Application-level DatabaseModule for repository injection pattern.
