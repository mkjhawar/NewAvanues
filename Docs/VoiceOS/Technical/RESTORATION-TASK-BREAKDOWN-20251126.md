# VoiceOS Restoration Task Breakdown

**Date:** 2025-11-26 14:45 PST
**Based On:** Current State Assessment 2025-11-26
**Total Estimate:** 41-59 hours (5-7 days)
**Approach:** Phased restoration (MVP → Production → Complete)

---

## Phase 1: Get App Compiling (4-6 hours)

**Goal:** App module builds successfully
**Blocker:** Cannot develop until app compiles

### Task 1.1: Create New DataModule.kt (2-3 hours)

**File:** `app/src/main/java/com/augmentalis/voiceos/di/DataModule.kt`

**Subtasks:**
1. [ ] Remove `.disabled` extension from DataModule.kt.disabled
2. [ ] Replace Room imports with SQLDelight imports
   ```kotlin
   // OLD (remove)
   import com.augmentalis.datamanager.database.VoiceOSDatabase  // Room
   import com.augmentalis.datamanager.dao.*

   // NEW (add)
   import com.augmentalis.database.VoiceOSDatabaseManager
   import com.augmentalis.database.repositories.*
   import com.augmentalis.database.repositories.impl.*
   ```

3. [ ] Replace Room database provider
   ```kotlin
   // OLD
   @Provides @Singleton
   fun provideVoiceOSDatabase(@ApplicationContext context: Context): VoiceOSDatabase {
       return VoiceOSDatabase.getInstance(context)
   }

   // NEW
   @Provides @Singleton
   fun provideDatabaseManager(@ApplicationContext context: Context): VoiceOSDatabaseManager {
       return VoiceOSDatabaseManager.getInstance(context)
   }
   ```

4. [ ] Replace 17 DAO providers with Repository providers
   - [ ] CommandHistoryEntryDao → IVoiceCommandHistoryRepository
   - [ ] CustomCommandDao → IVoiceCommandRepository
   - [ ] ScrappedCommandDao → (check if repository exists)
   - [ ] AnalyticsSettingsDao → (check if repository exists)
   - [ ] DeviceProfileDao → (check if repository exists)
   - [ ] ErrorReportDao → (check if repository exists)
   - [ ] GestureLearningDataDao → (check if repository exists)
   - [ ] LanguageModelDao → (check if repository exists)
   - [ ] RecognitionLearningDao → (check if repository exists)
   - [ ] RetentionSettingsDao → (check if repository exists)
   - [ ] TouchGestureDao → (check if repository exists)
   - [ ] UsageStatisticDao → IVoiceCommandUsageStatRepository
   - [ ] UserPreferenceDao → (check if repository exists)
   - [ ] UserSequenceDao → (check if repository exists)

5. [ ] Check which repositories are missing
   ```bash
   find libraries/core/database/src/commonMain/kotlin/com/augmentalis/database/repositories -name "*.kt"
   ```

6. [ ] Create missing repository interfaces (if any)
   - Template: See existing repositories in `libraries/core/database/repositories/`

7. [ ] Update VoiceOS.kt to use DatabaseManager
   ```kotlin
   // OLD
   import com.augmentalis.datamanager.DatabaseModule

   // NEW
   import com.augmentalis.database.VoiceOSDatabaseManager
   ```

8. [ ] Update ManagerModule.kt references
   - Replace `getInstance()` calls with proper DI

**Verification:**
```bash
./gradlew :app:compileDebugKotlin
# Should: BUILD SUCCESSFUL
```

**Estimated Time:** 2-3 hours
**Dependencies:** None
**Deliverable:** App compiles successfully

---

### Task 1.2: Fix VoiceOS.kt References (1 hour)

**File:** `app/src/main/java/com/augmentalis/voiceos/VoiceOS.kt`

**Errors to Fix (7 total):**
1. [ ] Line 17: `Unresolved reference: datamanager`
   - Replace import with VoiceOSDatabaseManager

2. [ ] Line 52: `Unresolved reference: DatabaseModule`
   - Use @Inject VoiceOSDatabaseManager instead

3. [ ] Line 79: `Unresolved reference: DatabaseModule`
   - Same as above

4. [ ] Line 89: `Unresolved reference: getInstance`
   - Use injected DatabaseManager

5. [ ] Line 112: `Unresolved reference: initialize`
   - Call DatabaseManager.initialize(context)

6. [ ] Line 155: `Unresolved reference: cleanup`
   - Call DatabaseManager.cleanup()

**Verification:**
```bash
./gradlew :app:compileDebugKotlin
# Should: BUILD SUCCESSFUL (all 7 errors gone)
```

**Estimated Time:** 1 hour
**Dependencies:** Task 1.1 complete
**Deliverable:** VoiceOS.kt compiles

---

### Task 1.3: Fix ManagerModule.kt (30 min)

**File:** `app/src/main/java/com/augmentalis/voiceos/di/ManagerModule.kt`

**Error to Fix:**
1. [ ] Line 76: `Unresolved reference: getInstance`
   - Inject DatabaseManager instead of calling getInstance()

**Verification:**
```bash
./gradlew :app:assembleDebug
# Should: BUILD SUCCESSFUL
```

**Estimated Time:** 30 minutes
**Dependencies:** Task 1.1 complete
**Deliverable:** Full app builds

---

### Phase 1 Checkpoint

**Deliverable:** ✅ App compiles and builds
**Time:** 4-6 hours
**Next:** Can now develop and test code changes

---

## Phase 2: Restore Core Voice Functionality (12-20 hours)

**Goal:** Basic voice commands work
**Allows:** User testing, demos, alpha release

### Task 2.1: Re-enable CommandManager Module (2 hours)

**Files to Modify:**
1. `settings.gradle.kts`
2. `app/build.gradle.kts`
3. `modules/apps/VoiceOSCore/build.gradle.kts`

**Subtasks:**

1. [ ] Uncomment CommandManager in settings.gradle.kts
   ```kotlin
   // BEFORE
   // include(":modules:managers:CommandManager")  // DISABLED

   // AFTER
   include(":modules:managers:CommandManager")
   ```

2. [ ] Uncomment dependency in app/build.gradle.kts
   ```kotlin
   // BEFORE
   // implementation(project(":modules:managers:CommandManager"))

   // AFTER
   implementation(project(":modules:managers:CommandManager"))
   ```

3. [ ] Uncomment dependency in VoiceOSCore/build.gradle.kts
   ```kotlin
   // BEFORE
   // implementation(project(":modules:managers:CommandManager"))

   // AFTER
   implementation(project(":modules:managers:CommandManager"))
   ```

4. [ ] Try to build CommandManager
   ```bash
   ./gradlew :modules:managers:CommandManager:compileDebugKotlin
   ```

5. [ ] Fix KSP errors (if any)
   - Enable verbose KSP logging: `--info`
   - Check Hilt annotation processing errors
   - Verify all dependencies available

6. [ ] Update CommandManager DI bindings
   - Replace Room DAO injections with Repository injections

**Verification:**
```bash
./gradlew :modules:managers:CommandManager:assembleDebug
# Should: BUILD SUCCESSFUL
```

**Estimated Time:** 2 hours
**Dependencies:** Phase 1 complete (DataModule migrated)
**Deliverable:** CommandManager module builds

---

### Task 2.2: Restore PreferenceLearner (3-4 hours)

**File:** `modules/managers/CommandManager/src/main/java/com/augmentalis/commandmanager/context/PreferenceLearner.kt.disabled`

**Subtasks:**

1. [ ] Rename file: Remove `.disabled` extension
   ```bash
   mv PreferenceLearner.kt.disabled PreferenceLearner.kt
   ```

2. [ ] Map 18 database calls to repository calls

   **Database Call Mapping:**

   | Line | Old Call | New Repository | Method |
   |------|----------|----------------|--------|
   | 89 | `database.recordUsage(...)` | IVoiceCommandHistoryRepository | `insert(...)` |
   | 128 | `database.getCommandHistory(...)` | IVoiceCommandHistoryRepository | `getAll()` |
   | 325 | `database.getUserPreferences(...)` | (TBD - check if exists) | |
   | 341 | `database.getCommandFrequency(...)` | IVoiceCommandUsageStatRepository | `getFrequency(...)` |
   | 376 | `database.getSequentialPatterns(...)` | (TBD - may need new query) | |
   | 377 | `database.getContextualPatterns(...)` | (TBD - may need new query) | |
   | 393 | `database.insertPattern(...)` | (TBD) | |
   | 394 | `database.updatePattern(...)` | (TBD) | |
   | 410 | `database.getStaleRecords(...)` | (TBD) | |
   | 411 | `database.deleteStaleRecords(...)` | (TBD) | |
   | 447 | `database.getUsageStatistics(...)` | IVoiceCommandUsageStatRepository | `getAll()` |
   | 466 | `database.getMostUsedCommands(...)` | IVoiceCommandUsageStatRepository | `getMostUsed(...)` |
   | 484 | `database.getCommandsByCategory(...)` | IVoiceCommandRepository | `getByCategory(...)` |
   | 485 | `database.getCommandsByContext(...)` | (TBD) | |
   | 486 | `database.getCommandsByTime(...)` | (TBD) | |
   | 487 | `database.getRecentCommands(...)` | IVoiceCommandHistoryRepository | `getRecent(...)` |
   | 488 | `database.getCommandSuccessRate(...)` | (TBD) | |
   | 489 | `database.getCommandErrorRate(...)` | (TBD) | |

3. [ ] Identify missing repository methods (marked TBD above)

4. [ ] Add missing methods to repository interfaces
   - Location: `libraries/core/database/src/commonMain/kotlin/com/augmentalis/database/repositories/`
   - Add to appropriate repository interface
   - Implement in `impl/` folder

5. [ ] Update PreferenceLearner constructor
   ```kotlin
   // OLD
   class PreferenceLearner(
       private val database: CommandDatabase
   )

   // NEW
   class PreferenceLearner(
       private val commandHistoryRepo: IVoiceCommandHistoryRepository,
       private val commandRepo: IVoiceCommandRepository,
       private val usageStatRepo: IVoiceCommandUsageStatRepository
   )
   ```

6. [ ] Update Hilt injection
   ```kotlin
   @Inject constructor(
       commandHistoryRepo: IVoiceCommandHistoryRepository,
       commandRepo: IVoiceCommandRepository,
       usageStatRepo: IVoiceCommandUsageStatRepository
   )
   ```

7. [ ] Replace all 18 database calls

8. [ ] Test compilation
   ```bash
   ./gradlew :modules:managers:CommandManager:compileDebugKotlin
   ```

**Verification:**
- [ ] File compiles without errors
- [ ] All database calls resolved
- [ ] Hilt DI works

**Estimated Time:** 3-4 hours
**Dependencies:** Task 2.1 complete
**Deliverable:** PreferenceLearner restored and functional

---

### Task 2.3: Restore Handler Files (4-6 hours)

**Location:** `modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/accessibility/handlers/`

**Files to Restore (11 total):**

**Subtasks:**

1. [ ] Find commit where handlers were deleted
   ```bash
   git log --all --full-history --oneline -- "**/handlers/ActionHandler.kt" | head -5
   ```

2. [ ] Restore all handler files from git
   ```bash
   # Find the last good commit (before YOLO migration)
   GOOD_COMMIT=$(git log --oneline --all -- "**/handlers/ActionHandler.kt" | head -1 | cut -d' ' -f1)

   # Restore all handlers
   git checkout $GOOD_COMMIT -- modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/accessibility/handlers/ActionHandler.kt
   git checkout $GOOD_COMMIT -- modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/accessibility/handlers/AppHandler.kt
   git checkout $GOOD_COMMIT -- modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/accessibility/handlers/BluetoothHandler.kt
   git checkout $GOOD_COMMIT -- modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/accessibility/handlers/DeviceHandler.kt
   git checkout $GOOD_COMMIT -- modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/accessibility/handlers/DragHandler.kt
   git checkout $GOOD_COMMIT -- modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/accessibility/handlers/GestureHandler.kt
   git checkout $GOOD_COMMIT -- modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/accessibility/handlers/HelpMenuHandler.kt
   git checkout $GOOD_COMMIT -- modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/accessibility/handlers/InputHandler.kt
   git checkout $GOOD_COMMIT -- modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/accessibility/handlers/NavigationHandler.kt
   git checkout $GOOD_COMMIT -- modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/accessibility/handlers/SelectHandler.kt
   git checkout $GOOD_COMMIT -- modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/accessibility/handlers/SystemHandler.kt
   git checkout $GOOD_COMMIT -- modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/accessibility/handlers/UIHandler.kt
   ```

3. [ ] Try to compile handlers
   ```bash
   ./gradlew :modules:apps:VoiceOSCore:compileDebugKotlin
   ```

4. [ ] Fix database reference errors in each handler

   **For EACH handler file:**
   - [ ] Find Room DAO references
   - [ ] Replace with Repository injection
   - [ ] Update imports
   - [ ] Test compilation

   **Example (AppHandler.kt):**
   ```kotlin
   // OLD
   @Inject constructor(
       private val appDao: AppDao
   )

   // NEW
   @Inject constructor(
       private val scrapedAppRepo: IScrapedAppRepository
   )
   ```

5. [ ] Create checklist for each handler:
   - [ ] ActionHandler.kt - database refs fixed
   - [ ] AppHandler.kt - database refs fixed
   - [ ] BluetoothHandler.kt - database refs fixed
   - [ ] DeviceHandler.kt - database refs fixed
   - [ ] DragHandler.kt - database refs fixed
   - [ ] GestureHandler.kt - database refs fixed
   - [ ] HelpMenuHandler.kt - database refs fixed
   - [ ] InputHandler.kt - database refs fixed
   - [ ] NavigationHandler.kt - database refs fixed
   - [ ] SelectHandler.kt - database refs fixed
   - [ ] SystemHandler.kt - database refs fixed
   - [ ] UIHandler.kt - database refs fixed

**Verification:**
```bash
./gradlew :modules:apps:VoiceOSCore:compileDebugKotlin
# Should: BUILD SUCCESSFUL with all 12 handlers
```

**Estimated Time:** 4-6 hours (30 min per handler)
**Dependencies:** Phase 1 complete
**Deliverable:** All 12 handlers restored and compiling

---

### Task 2.4: Restore Manager Implementations (2-4 hours)

**Files:**
1. `modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/accessibility/managers/ActionCoordinator.kt`
2. `modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/accessibility/managers/InstalledAppsManager.kt`

**Subtasks:**

1. [ ] **Option A: Restore from git** (recommended if recent deletion)
   ```bash
   git log --oneline --all -- "**/managers/ActionCoordinator.kt" | head -5
   GOOD_COMMIT=$(git log --oneline --all -- "**/managers/ActionCoordinator.kt" | head -1 | cut -d' ' -f1)
   git checkout $GOOD_COMMIT -- modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/accessibility/managers/ActionCoordinator.kt
   git checkout $GOOD_COMMIT -- modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/accessibility/managers/InstalledAppsManager.kt
   ```

2. [ ] **Option B: Implement from scratch** (if git history unclear)
   - Use functionality loss document as spec
   - Implement ActionCoordinator:
     - [ ] Handler registration
     - [ ] Command routing
     - [ ] Fallback handling
   - Implement InstalledAppsManager:
     - [ ] App discovery
     - [ ] App search
     - [ ] App launching

3. [ ] Fix database references in both files
   - ActionCoordinator: May not have any
   - InstalledAppsManager: Replace AppDao with IScrapedAppRepository

4. [ ] Update handler registration in ActionCoordinator
   ```kotlin
   private val handlers = mapOf(
       "action" to actionHandler,
       "app" to appHandler,
       "bluetooth" to bluetoothHandler,
       "device" to deviceHandler,
       "drag" to dragHandler,
       "gesture" to gestureHandler,
       "help" to helpMenuHandler,
       "input" to inputHandler,
       "navigation" to navigationHandler,
       "number" to numberHandler,  // Already exists
       "select" to selectHandler,
       "system" to systemHandler,
       "ui" to uiHandler
   )
   ```

5. [ ] Test compilation
   ```bash
   ./gradlew :modules:apps:VoiceOSCore:compileDebugKotlin
   ```

**Verification:**
- [ ] ActionCoordinator compiles
- [ ] InstalledAppsManager compiles
- [ ] All 12 handlers registered
- [ ] Command routing logic functional

**Estimated Time:** 2-4 hours
**Dependencies:** Task 2.3 complete (handlers restored)
**Deliverable:** Manager layer functional

---

### Phase 2 Checkpoint

**Deliverable:** ✅ Basic voice commands work
**Time:** 16-26 hours total (Phase 1 + Phase 2)
**User Impact:** Can use app with voice commands
**Ready For:** Alpha testing, demos

**What Works:**
- ✅ App compiles
- ✅ Voice commands processed
- ✅ Handlers execute actions
- ✅ Command history tracked
- ✅ AI suggestions (PreferenceLearner)

**What Doesn't Work:**
- ❌ No accessibility events (VoiceOSService disabled)
- ❌ No IPC (external apps can't use VoiceOS)
- ❌ No tests (manual QA only)

---

## Phase 3: Production Readiness (25-33 hours)

**Goal:** Production-quality app with full testing
**Allows:** Beta release, production deployment

### Task 3.1: Restore Service Layer (2-3 hours)

**Files to Restore:**
1. `VoiceOSService.kt.disabled`
2. `VoiceOSIPCService.java.disabled`
3. `VoiceOSServiceBinder.java.disabled`

**Subtasks:**

1. [ ] Remove `.disabled` extensions
   ```bash
   mv modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/accessibility/VoiceOSService.kt.disabled \
      modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/accessibility/VoiceOSService.kt

   mv modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/accessibility/VoiceOSIPCService.java.disabled \
      modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/accessibility/VoiceOSIPCService.java

   mv modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/accessibility/VoiceOSServiceBinder.java.disabled \
      modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/accessibility/VoiceOSServiceBinder.java
   ```

2. [ ] Fix VoiceOSService.kt database references
   - Replace Room event logging with repository calls
   - Update CommandManager integration
   - Verify ActionCoordinator injection

3. [ ] Test IPC functionality
   - Create simple test client app
   - Verify binder works
   - Test cross-process command execution

4. [ ] Update AndroidManifest.xml (if needed)
   - Verify service declarations
   - Check permissions

**Verification:**
```bash
./gradlew :modules:apps:VoiceOSCore:assembleDebug
# Should: BUILD SUCCESSFUL
```

**Estimated Time:** 2-3 hours
**Dependencies:** Phase 2 complete
**Deliverable:** Full accessibility service functional

---

### Task 3.2: Rewrite Test Suite (20-30 hours)

**Location:** `modules/apps/VoiceOSCore/src/test/`

**Strategy:** Rewrite for SQLDelight (cannot just re-enable)

**Subtasks:**

#### 3.2.1: Setup Test Infrastructure (4 hours)

1. [ ] Create SQLDelight test database factory
   ```kotlin
   // TestDatabaseFactory.kt
   object TestDatabaseFactory {
       fun createInMemoryDatabase(): VoiceOSDatabase {
           val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
           VoiceOSDatabase.Schema.create(driver)
           return VoiceOSDatabase(driver)
       }
   }
   ```

2. [ ] Setup coroutine test dispatcher
   ```kotlin
   // CoroutineTestRule.kt
   @ExperimentalCoroutinesApi
   class CoroutineTestRule : TestWatcher() {
       val testDispatcher = StandardTestDispatcher()
       override fun starting(description: Description) {
           Dispatchers.setMain(testDispatcher)
       }
       override fun finished(description: Description) {
           Dispatchers.resetMain()
       }
   }
   ```

3. [ ] Create base test class
   ```kotlin
   // BaseRepositoryTest.kt
   abstract class BaseRepositoryTest {
       lateinit var database: VoiceOSDatabase
       lateinit var databaseManager: VoiceOSDatabaseManager

       @Before
       fun setup() {
           database = TestDatabaseFactory.createInMemoryDatabase()
           databaseManager = VoiceOSDatabaseManager(database)
       }

       @After
       fun teardown() {
           database.close()
       }
   }
   ```

4. [ ] Add test dependencies to build.gradle.kts
   ```kotlin
   testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
   testImplementation("app.cash.turbine:turbine:1.0.0")
   testImplementation("com.squareup.sqldelight:sqlite-driver:2.0.0")
   ```

**Estimated Time:** 4 hours
**Deliverable:** Test infrastructure ready

#### 3.2.2: Rewrite Database Tests (3 hours)

**Files to Rewrite (2):**
1. [ ] BatchTransactionManagerTest.kt → SQLDelight transaction tests
2. [ ] SafeCursorManagerTest.kt → Repository query tests

**Template:**
```kotlin
class VoiceCommandRepositoryTest : BaseRepositoryTest() {
    private lateinit var repository: IVoiceCommandRepository

    @Before
    override fun setup() {
        super.setup()
        repository = databaseManager.voiceCommands
    }

    @Test
    fun `insert command creates record`() = runTest {
        val command = VoiceCommandDTO(...)
        repository.insert(command)
        val result = repository.getById(command.id)
        assertEquals(command, result)
    }
}
```

**Estimated Time:** 3 hours

#### 3.2.3: Rewrite Accessibility Tests (10 hours)

**Files to Rewrite (14):**
1. [ ] MockVoiceAccessibilityService.kt
2. [ ] MockVoiceRecognitionManager.kt
3. [ ] AccessibilityTreeProcessorTest.kt
4. [ ] VoiceCommandTestScenarios.kt
5. [ ] CommandExecutionVerifier.kt
6. [ ] ConfidenceOverlayTest.kt
7. [ ] OverlayManagerTest.kt
8. [ ] UUIDCreatorIntegrationTest.kt
9. [ ] GestureHandlerTest.kt
10. [ ] DragHandlerTest.kt
11. [ ] GazeHandlerTest.kt
12. [ ] EndToEndVoiceTest.kt
13. [ ] PerformanceTest.kt
14. [ ] EventPriorityManagerTest.kt

**Approach:**
- [ ] Rewrite mocks for SQLDelight repositories
- [ ] Update handler tests to use new repositories
- [ ] Convert runBlocking to runTest
- [ ] Update assertions for new data models

**Estimated Time:** 10 hours (~45 min per test)

#### 3.2.4: Rewrite Lifecycle Tests (4 hours)

**Files to Rewrite (4):**
1. [ ] AccessibilityNodeManagerSimpleTest.kt
2. [ ] AccessibilityNodeManagerTest.kt
3. [ ] AsyncQueryManagerTest.kt
4. [ ] SafeNodeTraverserTest.kt

**Estimated Time:** 4 hours (1 hour per test)

#### 3.2.5: Rewrite Scraping/Validation Tests (5 hours)

**Files to Rewrite (5):**
1. [ ] CachedElementHierarchyTest.kt
2. [ ] DataFlowValidationTest.kt
3. [ ] HierarchyIntegrityTest.kt
4. [ ] ScrapingDatabaseSyncTest.kt
5. [ ] UUIDIntegrationTest.kt

**Estimated Time:** 5 hours (1 hour per test)

#### 3.2.6: Rewrite Utility Tests (1 hour)

**Files to Rewrite (1):**
1. [ ] SafeNullHandlerTest.kt

**Estimated Time:** 1 hour

#### 3.2.7: Add Performance Benchmarks (3 hours)

**New Tests:**
1. [ ] SQLDelight query performance
2. [ ] Repository operation benchmarks
3. [ ] Comparison with Room baseline (if data exists)

**Estimated Time:** 3 hours

**Total Test Rewrite Time:** 20-30 hours

---

### Phase 3 Checkpoint

**Deliverable:** ✅ Production-ready app
**Time:** 41-59 hours total (Phase 1 + 2 + 3)
**User Impact:** Fully functional with quality assurance
**Ready For:** Production deployment

**What Works:**
- ✅ Everything from Phase 2
- ✅ Accessibility events processed
- ✅ IPC functional
- ✅ 90%+ test coverage
- ✅ Automated regression protection

---

## Optional: Advanced Features (10-15 hours)

**Not required for MVP or production, but restores full feature parity**

### Task 4.1: Restore LearnApp Features (6 hours)

**Files to Restore (~15):**
- LearnAppIntegration.kt
- ExplorationEngine.kt
- ScreenExplorer.kt
- ElementClassifier.kt
- CommandGenerator.kt
- NavigationGraph.kt
- StateDetector.kt
- ConsentDialog.kt
- ProgressOverlay.kt
- etc.

**Approach:**
1. [ ] Git checkout all LearnApp files
2. [ ] Migrate database references to SQLDelight
3. [ ] Test UI exploration
4. [ ] Test command generation

**Estimated Time:** 6 hours

### Task 4.2: Restore LearnWeb Features (4 hours)

**Files to Restore (~8):**
- WebScrapingDatabase.kt
- WebCommandGenerator.kt
- WebViewScrapingEngine.kt
- ScrapedWebsiteDao.kt
- GeneratedWebCommandDao.kt
- etc.

**Approach:**
1. [ ] Git checkout all LearnWeb files
2. [ ] Migrate web scraping database to SQLDelight
3. [ ] Test WebView integration

**Estimated Time:** 4 hours

### Task 4.3: Restore Database Utilities (2-4 hours)

**Files to Evaluate:**
1. CircuitBreaker.kt - Still useful
2. DataRetentionPolicy.kt - Still useful
3. BatchTransactionManager.kt - Not needed (SQLDelight native)
4. MigrationRollbackManager.kt - Not needed
5. SafeCursorManager.kt - Not needed
6. SafeTransactionManager.kt - Not needed (SQLDelight native)

**Approach:**
1. [ ] Restore CircuitBreaker.kt (no database deps)
2. [ ] Restore DataRetentionPolicy.kt + migrate to repositories
3. [ ] Document deprecated utilities

**Estimated Time:** 2-4 hours

---

## Summary: Task Dependencies

```
Phase 1: Get App Compiling (4-6h)
├─ 1.1: Create DataModule.kt (2-3h)
├─ 1.2: Fix VoiceOS.kt (1h) [depends on 1.1]
└─ 1.3: Fix ManagerModule.kt (30m) [depends on 1.1]

Phase 2: Restore Core Functionality (12-20h)
├─ 2.1: Re-enable CommandManager (2h) [depends on Phase 1]
├─ 2.2: Restore PreferenceLearner (3-4h) [depends on 2.1]
├─ 2.3: Restore Handlers (4-6h) [depends on Phase 1]
└─ 2.4: Restore Managers (2-4h) [depends on 2.3]

Phase 3: Production Ready (25-33h)
├─ 3.1: Restore Service Layer (2-3h) [depends on Phase 2]
└─ 3.2: Rewrite Tests (20-30h) [depends on Phase 2]
    ├─ 3.2.1: Setup infrastructure (4h)
    ├─ 3.2.2: Database tests (3h)
    ├─ 3.2.3: Accessibility tests (10h)
    ├─ 3.2.4: Lifecycle tests (4h)
    ├─ 3.2.5: Scraping tests (5h)
    ├─ 3.2.6: Utility tests (1h)
    └─ 3.2.7: Performance tests (3h)

Phase 4: Advanced Features (10-15h) [OPTIONAL]
├─ 4.1: LearnApp (6h)
├─ 4.2: LearnWeb (4h)
└─ 4.3: DB Utilities (2-4h)
```

---

## Milestones

| Milestone | Hours | Days | Deliverable |
|-----------|-------|------|-------------|
| **M1: Compilation** | 4-6 | 1 | App builds |
| **M2: MVP** | 16-26 | 2-3 | Basic voice works |
| **M3: Production** | 41-59 | 5-7 | Fully tested |
| **M4: Complete** | 51-74 | 6-9 | All features restored |

---

## Quick Start: Execute Phase 1

**To begin restoration RIGHT NOW:**

```bash
# 1. Create DataModule.kt
cd /Volumes/M-Drive/Coding/VoiceOS/app/src/main/java/com/augmentalis/voiceos/di
mv DataModule.kt.disabled DataModule.kt

# 2. Open in editor and start Task 1.1
# Follow the checklist in Task 1.1 above

# 3. Verify progress
./gradlew :app:compileDebugKotlin
```

**Time to first milestone:** 4-6 hours

---

**Document Created:** 2025-11-26 14:45 PST
**Based On:** Current State Assessment 2025-11-26
**Total Tasks:** 60+ individual tasks across 4 phases
**Format:** Ready for TodoWrite tool or manual execution
