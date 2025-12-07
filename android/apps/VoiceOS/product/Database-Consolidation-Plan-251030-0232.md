# IDEACODE Implementation Plan: Database Consolidation

**Created:** 2025-10-30 02:32 PDT
**Feature:** Database Consolidation (LearnApp + Scraping → VoiceOSAppDatabase)
**Branch:** voiceos-database-update
**Spec:** /ideadev/specs/Database-Consolidation-Spec-251030-0232.md

---

## Overview

Consolidate LearnAppDatabase and AppScrapingDatabase into unified VoiceOSAppDatabase using IDEACODE SP(IDE)R methodology.

**Timeline:** 1 implementation session (YOLO mode)
**Risk:** MEDIUM (mitigated by comprehensive testing)
**Impact:** HIGH (eliminates all sync issues)

---

## Phase 1: Create Unified Database Schema

### Subphase 1.1: Create Database File Structure
**Location:** `/modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/database/`

**Files to Create:**
```
database/
├── VoiceOSAppDatabase.kt          # Main database class
├── entities/
│   ├── AppEntity.kt               # Merged learned_apps + scraped_apps
│   ├── ScreenEntity.kt            # Merged screen_states + screen_contexts
│   ├── ScreenTransitionEntity.kt  # From scraping (has timing data)
│   └── ExplorationSessionEntity.kt # From LearnApp (unchanged)
├── dao/
│   ├── AppDao.kt                  # Unified app operations
│   ├── ScreenDao.kt               # Unified screen operations
│   ├── ScreenTransitionDao.kt     # Navigation operations
│   └── ExplorationSessionDao.kt   # Session operations
└── migration/
    └── DatabaseMigrationHelper.kt # Migration orchestrator
```

**Implementation Steps:**

**Step 1.1.1:** Create AppEntity (merged)
```kotlin
@Entity(
    tableName = "apps",
    indices = [
        Index(value = ["appId"], unique = true),
        Index(value = ["packageName"]),
        Index(value = ["explorationStatus"])
    ]
)
data class AppEntity(
    @PrimaryKey val appId: String,
    val packageName: String,
    val appName: String,
    val versionCode: Int,
    val versionName: String,
    val appHash: String,

    // Exploration tracking (from LearnApp)
    val explorationStatus: String,           // "NOT_STARTED", "IN_PROGRESS", "COMPLETE"
    val totalScreens: Int,
    val totalElements: Int,
    val totalEdges: Int,
    val rootScreenHash: String?,
    val firstExplored: Long?,
    val lastExplored: Long?,

    // Scraping metadata (from AppScrapingDatabase)
    val elementCount: Int,
    val commandCount: Int,
    val isFakeable: Int,
    val scrapingMode: String,                // "DYNAMIC" or "STATIC"
    val isFullyLearned: Int,
    val learnCompletedAt: Long?,
    val firstScraped: Long,
    val lastScraped: Long
)
```

**Step 1.1.2:** Create ScreenEntity (merged)
```kotlin
@Entity(
    tableName = "screens",
    foreignKeys = [
        ForeignKey(
            entity = AppEntity::class,
            parentColumns = ["appId"],
            childColumns = ["appId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["screenHash"], unique = true),
        Index(value = ["appId"]),
        Index(value = ["packageName"]),
        Index(value = ["screenType"])
    ]
)
data class ScreenEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val screenHash: String,
    val appId: String,
    val packageName: String,
    val activityName: String?,

    // Screen metadata
    val windowTitle: String?,
    val screenType: String?,
    val formContext: String?,
    val navigationLevel: Int = 0,
    val primaryAction: String?,

    // Element tracking
    val elementCount: Int = 0,
    val hasBackButton: Int = 0,

    // Timestamps
    val firstDiscovered: Long,
    val lastVisited: Long,
    val visitCount: Int = 1
)
```

**Step 1.1.3:** Copy ExplorationSessionEntity from LearnApp (unchanged)

**Step 1.1.4:** Copy ScreenTransitionEntity from AppScrapingDatabase (unchanged)

**Step 1.1.5:** Copy all scraping entities (unchanged):
- ScrapedElementEntity
- ScrapedHierarchyEntity
- GeneratedCommandEntity
- ElementRelationshipEntity
- UserInteractionEntity
- ElementStateHistoryEntity

**Step 1.1.6:** Create AppDao
```kotlin
@Dao
interface AppDao {
    @Query("SELECT * FROM apps WHERE packageName = :packageName LIMIT 1")
    suspend fun getApp(packageName: String): AppEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(app: AppEntity)

    @Update
    suspend fun update(app: AppEntity)

    @Query("UPDATE apps SET totalElements = :count WHERE packageName = :packageName")
    suspend fun updateElementCount(packageName: String, count: Int)

    @Query("UPDATE apps SET explorationStatus = :status WHERE packageName = :packageName")
    suspend fun updateExplorationStatus(packageName: String, status: String)

    @Query("SELECT COUNT(*) FROM apps")
    suspend fun getAppCount(): Int

    @Query("DELETE FROM apps WHERE packageName = :packageName")
    suspend fun deleteApp(packageName: String)
}
```

**Step 1.1.7:** Create ScreenDao
```kotlin
@Dao
interface ScreenDao {
    @Query("SELECT * FROM screens WHERE screenHash = :screenHash LIMIT 1")
    suspend fun getScreen(screenHash: String): ScreenEntity?

    @Query("SELECT * FROM screens WHERE packageName = :packageName")
    suspend fun getScreensForApp(packageName: String): List<ScreenEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(screen: ScreenEntity)

    @Update
    suspend fun update(screen: ScreenEntity)

    @Query("SELECT COUNT(*) FROM screens WHERE packageName = :packageName")
    suspend fun getScreenCount(packageName: String): Int

    @Query("DELETE FROM screens WHERE packageName = :packageName")
    suspend fun deleteScreensForApp(packageName: String)
}
```

**Step 1.1.8:** Create VoiceOSAppDatabase
```kotlin
@Database(
    entities = [
        AppEntity::class,
        ScreenEntity::class,
        ScreenTransitionEntity::class,
        ExplorationSessionEntity::class,
        ScrapedElementEntity::class,
        ScrapedHierarchyEntity::class,
        GeneratedCommandEntity::class,
        ElementRelationshipEntity::class,
        UserInteractionEntity::class,
        ElementStateHistoryEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class VoiceOSAppDatabase : RoomDatabase() {

    abstract fun appDao(): AppDao
    abstract fun screenDao(): ScreenDao
    abstract fun screenTransitionDao(): ScreenTransitionDao
    abstract fun explorationSessionDao(): ExplorationSessionDao
    abstract fun scrapedElementDao(): ScrapedElementDao
    abstract fun scrapedHierarchyDao(): ScrapedHierarchyDao
    abstract fun generatedCommandDao(): GeneratedCommandDao
    abstract fun elementRelationshipDao(): ElementRelationshipDao
    abstract fun userInteractionDao(): UserInteractionDao
    abstract fun elementStateHistoryDao(): ElementStateHistoryDao

    companion object {
        private const val DATABASE_NAME = "voiceos_app_database"

        @Volatile
        private var INSTANCE: VoiceOSAppDatabase? = null

        fun getInstance(context: Context): VoiceOSAppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VoiceOSAppDatabase::class.java,
                    DATABASE_NAME
                )
                .addCallback(DatabaseCallback())
                .build()

                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                android.util.Log.i("VoiceOSAppDatabase", "Database created")
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                android.util.Log.i("VoiceOSAppDatabase", "Database opened")
            }
        }
    }
}
```

**Expected Output:**
- ✅ Unified database schema created
- ✅ All entities defined
- ✅ All DAOs created
- ✅ Database compiles successfully

---

## Phase 2: Create Migration Logic

### Subphase 2.1: Create Migration Helper

**File:** `database/migration/DatabaseMigrationHelper.kt`

**Step 2.1.1:** Create migration coordinator
```kotlin
class DatabaseMigrationHelper(
    private val context: Context,
    private val uuidCreator: UUIDCreator
) {
    private val prefs = context.getSharedPreferences("database_migration", Context.MODE_PRIVATE)
    private val newDb = VoiceOSAppDatabase.getInstance(context)

    suspend fun migrateIfNeeded(): Boolean {
        if (isMigrationComplete()) {
            android.util.Log.i("Migration", "Migration already completed")
            return true
        }

        android.util.Log.i("Migration", "Starting database migration...")

        return try {
            withContext(Dispatchers.IO) {
                // Step 1: Migrate from LearnAppDatabase
                migrateFromLearnApp()

                // Step 2: Migrate from AppScrapingDatabase
                migrateFromScraping()

                // Step 3: Validate against UUIDCreator
                validateElementCounts()

                // Step 4: Mark migration complete
                markMigrationComplete()

                android.util.Log.i("Migration", "✅ Migration completed successfully")
                true
            }
        } catch (e: Exception) {
            android.util.Log.e("Migration", "❌ Migration failed", e)
            handleMigrationFailure(e)
            false
        }
    }

    private fun isMigrationComplete(): Boolean {
        return prefs.getBoolean("migration_complete", false)
    }

    private fun markMigrationComplete() {
        prefs.edit().putBoolean("migration_complete", true).apply()
    }
}
```

**Step 2.1.2:** Implement LearnApp migration
```kotlin
private suspend fun migrateFromLearnApp() {
    android.util.Log.i("Migration", "Migrating from LearnAppDatabase...")

    val oldDb = LearnAppDatabase.getInstance(context)

    // Migrate learned_apps → apps
    val learnedApps = oldDb.learnAppDao().getAllLearnedApps()
    android.util.Log.i("Migration", "Found ${learnedApps.size} learned apps")

    learnedApps.forEach { old ->
        val appEntity = AppEntity(
            appId = old.appId,
            packageName = old.packageName,
            appName = old.packageName, // Will be updated by scraping data
            versionCode = 0,
            versionName = "",
            appHash = "",

            // From LearnApp
            explorationStatus = old.explorationStatus,
            totalScreens = old.totalScreens,
            totalElements = old.totalElements,
            totalEdges = old.totalEdges,
            rootScreenHash = old.rootScreenHash,
            firstExplored = old.firstExplored,
            lastExplored = old.lastExplored,

            // Defaults (will be merged with scraping data)
            elementCount = 0,
            commandCount = 0,
            isFakeable = 0,
            scrapingMode = "DYNAMIC",
            isFullyLearned = 0,
            learnCompletedAt = null,
            firstScraped = System.currentTimeMillis(),
            lastScraped = System.currentTimeMillis()
        )
        newDb.appDao().insert(appEntity)
    }

    // Migrate screen_states → screens
    val screenStates = oldDb.learnAppDao().getAllScreenStates()
    android.util.Log.i("Migration", "Found ${screenStates.size} screen states")

    screenStates.forEach { old ->
        val screenEntity = ScreenEntity(
            screenHash = old.hash,
            appId = old.appId,
            packageName = old.packageName,
            activityName = old.activityName,
            windowTitle = null,
            screenType = null,
            formContext = null,
            navigationLevel = 0,
            primaryAction = null,
            elementCount = old.elementCount,
            hasBackButton = 0,
            firstDiscovered = old.timestamp,
            lastVisited = old.timestamp,
            visitCount = 1
        )
        newDb.screenDao().insert(screenEntity)
    }

    // Migrate exploration_sessions (unchanged)
    val sessions = oldDb.learnAppDao().getAllSessions()
    sessions.forEach { session ->
        newDb.explorationSessionDao().insert(session)
    }

    android.util.Log.i("Migration", "✅ LearnApp migration complete")
}
```

**Step 2.1.3:** Implement Scraping migration
```kotlin
private suspend fun migrateFromScraping() {
    android.util.Log.i("Migration", "Migrating from AppScrapingDatabase...")

    val oldDb = AppScrapingDatabase.getInstance(context)

    // Merge scraped_apps data into existing apps
    val scrapedApps = oldDb.scrapedAppDao().getAllApps()
    android.util.Log.i("Migration", "Found ${scrapedApps.size} scraped apps")

    scrapedApps.forEach { scraped ->
        val existing = newDb.appDao().getApp(scraped.packageName)

        if (existing != null) {
            // Merge with existing LearnApp data
            val merged = existing.copy(
                appName = scraped.appName,
                versionCode = scraped.versionCode,
                versionName = scraped.versionName,
                appHash = scraped.appHash,
                elementCount = scraped.elementCount,
                commandCount = scraped.commandCount,
                isFakeable = scraped.isFakeable,
                scrapingMode = scraped.scrapingMode,
                isFullyLearned = scraped.isFullyLearned,
                learnCompletedAt = scraped.learnCompletedAt,
                firstScraped = scraped.firstScraped,
                lastScraped = scraped.lastScraped
            )
            newDb.appDao().update(merged)
        } else {
            // No LearnApp data, insert as new
            val appEntity = AppEntity(
                appId = scraped.appId,
                packageName = scraped.packageName,
                appName = scraped.appName,
                versionCode = scraped.versionCode,
                versionName = scraped.versionName,
                appHash = scraped.appHash,
                explorationStatus = "NOT_STARTED",
                totalScreens = 0,
                totalElements = scraped.elementCount,
                totalEdges = 0,
                rootScreenHash = null,
                firstExplored = null,
                lastExplored = null,
                elementCount = scraped.elementCount,
                commandCount = scraped.commandCount,
                isFakeable = scraped.isFakeable,
                scrapingMode = scraped.scrapingMode,
                isFullyLearned = scraped.isFullyLearned,
                learnCompletedAt = scraped.learnCompletedAt,
                firstScraped = scraped.firstScraped,
                lastScraped = scraped.lastScraped
            )
            newDb.appDao().insert(appEntity)
        }
    }

    // Migrate screen_contexts → merge with screens
    val screenContexts = oldDb.screenContextDao().getAllScreens()
    android.util.Log.i("Migration", "Found ${screenContexts.size} screen contexts")

    screenContexts.forEach { context ->
        val existing = newDb.screenDao().getScreen(context.screenHash)

        if (existing != null) {
            // Merge with existing screen_state data
            val merged = existing.copy(
                windowTitle = context.windowTitle,
                screenType = context.screenType,
                formContext = context.formContext,
                navigationLevel = context.navigationLevel,
                primaryAction = context.primaryAction,
                hasBackButton = context.hasBackButton,
                visitCount = context.visitCount
            )
            newDb.screenDao().update(merged)
        } else {
            // No screen_state data, insert as new
            val screenEntity = ScreenEntity(
                screenHash = context.screenHash,
                appId = context.appId,
                packageName = context.packageName,
                activityName = context.activityName,
                windowTitle = context.windowTitle,
                screenType = context.screenType,
                formContext = context.formContext,
                navigationLevel = context.navigationLevel,
                primaryAction = context.primaryAction,
                elementCount = context.elementCount,
                hasBackButton = context.hasBackButton,
                firstDiscovered = context.firstScraped,
                lastVisited = context.lastScraped,
                visitCount = context.visitCount
            )
            newDb.screenDao().insert(screenEntity)
        }
    }

    // Copy all scraping tables unchanged
    val elements = oldDb.scrapedElementDao().getAllElements()
    elements.forEach { newDb.scrapedElementDao().insert(it) }

    val hierarchy = oldDb.scrapedHierarchyDao().getAllRelationships()
    hierarchy.forEach { newDb.scrapedHierarchyDao().insert(it) }

    val commands = oldDb.generatedCommandDao().getAllCommands()
    commands.forEach { newDb.generatedCommandDao().insert(it) }

    android.util.Log.i("Migration", "✅ Scraping migration complete")
}
```

**Step 2.1.4:** Implement validation
```kotlin
private suspend fun validateElementCounts() {
    android.util.Log.i("Migration", "Validating element counts against UUIDCreator...")

    val allApps = newDb.appDao().getAllApps()

    allApps.forEach { app ->
        val actualCount = uuidCreator.getElementCountForPackage(app.packageName)

        if (app.totalElements != actualCount) {
            android.util.Log.w("Migration",
                "Element count mismatch for ${app.packageName}: " +
                "DB says ${app.totalElements}, UUIDCreator says $actualCount. " +
                "Correcting to $actualCount")

            newDb.appDao().updateElementCount(app.packageName, actualCount)
        }
    }

    android.util.Log.i("Migration", "✅ Validation complete")
}
```

**Expected Output:**
- ✅ All LearnApp data migrated
- ✅ All Scraping data migrated
- ✅ Data merged correctly
- ✅ Element counts validated against UUIDCreator
- ✅ No data loss

---

## Phase 3: Update Code References

### Subphase 3.1: Update ExplorationEngine

**File:** `/modules/apps/LearnApp/src/main/java/com/augmentalis/learnapp/exploration/ExplorationEngine.kt`

**Changes:**
1. Replace `LearnAppDatabase` with `VoiceOSAppDatabase`
2. Update DAO calls to use new unified DAOs
3. Add validation using UUIDCreator

**Step 3.1.1:** Update database instance
```kotlin
// OLD
private val database = LearnAppDatabase.getInstance(context)
private val learnAppDao = database.learnAppDao()

// NEW
private val database = VoiceOSAppDatabase.getInstance(context)
private val appDao = database.appDao()
private val screenDao = database.screenDao()
```

**Step 3.1.2:** Update stats creation
```kotlin
private fun createExplorationStats(packageName: String): ExplorationStats {
    val stats = screenStateManager.getStats()
    val graph = navigationGraphBuilder.build()
    val graphStats = graph.getStats()

    // NEW: Query actual registered elements from UUIDCreator
    val actualElementCount = uuidCreator.getElementCountForPackage(packageName)

    // VALIDATION: Warn if mismatch
    if (actualElementCount != graphStats.totalElements) {
        android.util.Log.w("ExplorationEngine",
            "Stats mismatch! Graph says ${graphStats.totalElements} elements, " +
            "but UUIDCreator has $actualElementCount elements for $packageName. " +
            "Using actual count from UUIDCreator.")
    }

    return ExplorationStats(
        packageName = packageName,
        appName = packageName,
        totalScreens = stats.totalScreensDiscovered,
        totalElements = actualElementCount,  // Use actual count from UUIDCreator
        totalEdges = graphStats.totalEdges,
        durationMs = elapsed,
        maxDepth = graphStats.maxDepth,
        dangerousElementsSkipped = dangerousElementsSkipped,
        loginScreensDetected = loginScreensDetected,
        scrollableContainersFound = 0
    )
}
```

**Step 3.1.3:** Update app status updates
```kotlin
// OLD
learnAppDao.updateExplorationStatus(packageName, "COMPLETE")

// NEW
appDao.updateExplorationStatus(packageName, "COMPLETE")
```

### Subphase 3.2: Update VoiceCommandProcessor

**File:** `/modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/scraping/VoiceCommandProcessor.kt`

**Changes:**
1. Replace `AppScrapingDatabase` with `VoiceOSAppDatabase`
2. Update DAO references

**Step 3.2.1:** Update database instance
```kotlin
// OLD
private val database = AppScrapingDatabase.getInstance(context)
private val scrapedAppDao = database.scrapedAppDao()

// NEW
private val database = VoiceOSAppDatabase.getInstance(context)
private val appDao = database.appDao()
```

### Subphase 3.3: Update LearnAppIntegration

**File:** `/modules/apps/LearnApp/src/main/java/com/augmentalis/learnapp/integration/LearnAppIntegration.kt`

**Changes:**
1. Replace `LearnAppDatabase` with `VoiceOSAppDatabase`
2. Update all DAO calls

### Subphase 3.4: Update All Test Files

**Files to Update:**
- LearnAppForeignKeyConstraintTest.kt
- LearnAppIntegrationTest.kt
- ForeignKeyConstraintTest.kt
- Migration1To2Test.kt
- LearnAppMergeTest.kt
- VoiceCommandPersistenceTest.kt

**Changes:**
- Update database instances
- Update expected table names
- Update entity references

**Expected Output:**
- ✅ All code references updated
- ✅ No compilation errors
- ✅ All imports updated

---

## Phase 4: Create Tests

### Subphase 4.1: Create Unit Tests

**File:** `VoiceOSAppDatabaseTest.kt`

```kotlin
@RunWith(AndroidJUnit4::class)
class VoiceOSAppDatabaseTest {

    private lateinit var database: VoiceOSAppDatabase
    private lateinit var appDao: AppDao
    private lateinit var screenDao: ScreenDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            VoiceOSAppDatabase::class.java
        ).build()

        appDao = database.appDao()
        screenDao = database.screenDao()
    }

    @After
    fun cleanup() {
        database.close()
    }

    @Test
    fun testInsertAndRetrieveApp() = runBlocking {
        val app = createTestApp("com.test.app")
        appDao.insert(app)

        val retrieved = appDao.getApp("com.test.app")
        assertNotNull(retrieved)
        assertEquals("com.test.app", retrieved?.packageName)
    }

    @Test
    fun testCascadeDeleteApp() = runBlocking {
        // Insert app with screens
        val app = createTestApp("com.test.app")
        appDao.insert(app)

        val screen = createTestScreen("hash1", app.appId, "com.test.app")
        screenDao.insert(screen)

        // Delete app
        appDao.deleteApp("com.test.app")

        // Verify screens also deleted (cascade)
        val screens = screenDao.getScreensForApp("com.test.app")
        assertTrue(screens.isEmpty())
    }

    @Test
    fun testUpdateElementCount() = runBlocking {
        val app = createTestApp("com.test.app")
        appDao.insert(app)

        appDao.updateElementCount("com.test.app", 254)

        val updated = appDao.getApp("com.test.app")
        assertEquals(254, updated?.totalElements)
    }
}
```

### Subphase 4.2: Create Migration Tests

**File:** `DatabaseMigrationTest.kt`

```kotlin
@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {

    private lateinit var context: Context
    private lateinit var migrationHelper: DatabaseMigrationHelper

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()

        // Clear any existing migration state
        context.getSharedPreferences("database_migration", Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    @Test
    fun testMigrationFromLearnApp() = runBlocking {
        // Populate old LearnApp database
        val oldDb = LearnAppDatabase.getInstance(context)
        val testApp = createTestLearnedApp("com.microsoft.teams")
        oldDb.learnAppDao().insert(testApp)

        // Run migration
        migrationHelper = DatabaseMigrationHelper(context, mockUuidCreator)
        val success = migrationHelper.migrateIfNeeded()

        assertTrue(success)

        // Verify data in new database
        val newDb = VoiceOSAppDatabase.getInstance(context)
        val migratedApp = newDb.appDao().getApp("com.microsoft.teams")

        assertNotNull(migratedApp)
        assertEquals(testApp.packageName, migratedApp?.packageName)
        assertEquals(testApp.explorationStatus, migratedApp?.explorationStatus)
    }

    @Test
    fun testElementCountValidation() = runBlocking {
        // Mock UUIDCreator to return 254 elements
        val mockUuidCreator = mock(UUIDCreator::class.java)
        `when`(mockUuidCreator.getElementCountForPackage("com.microsoft.teams"))
            .thenReturn(254)

        // Populate app with wrong count
        val newDb = VoiceOSAppDatabase.getInstance(context)
        val app = createTestApp("com.microsoft.teams").copy(totalElements = 5)
        newDb.appDao().insert(app)

        // Run validation
        migrationHelper = DatabaseMigrationHelper(context, mockUuidCreator)
        migrationHelper.migrateIfNeeded()

        // Verify count corrected
        val corrected = newDb.appDao().getApp("com.microsoft.teams")
        assertEquals(254, corrected?.totalElements)
    }
}
```

### Subphase 4.3: Create Integration Tests

**File:** `DatabaseConsolidationIntegrationTest.kt`

```kotlin
@RunWith(AndroidJUnit4::class)
class DatabaseConsolidationIntegrationTest {

    @Test
    fun testEndToEndExplorationWithNewDatabase() = runBlocking {
        // Test full exploration flow with new database
        val explorationEngine = ExplorationEngine(context)

        // Start exploration
        explorationEngine.startExploration("com.microsoft.teams")

        // Wait for completion
        delay(30000) // 30 seconds max

        // Verify stats in database
        val db = VoiceOSAppDatabase.getInstance(context)
        val app = db.appDao().getApp("com.microsoft.teams")

        assertNotNull(app)
        assertEquals("COMPLETE", app?.explorationStatus)
        assertTrue(app?.totalElements ?: 0 > 0)

        // Verify stats match UUIDCreator
        val actualCount = uuidCreator.getElementCountForPackage("com.microsoft.teams")
        assertEquals(actualCount, app?.totalElements)
    }
}
```

**Expected Output:**
- ✅ All unit tests passing
- ✅ Migration tests passing
- ✅ Integration tests passing
- ✅ Teams app shows 254 elements
- ✅ No data loss verified

---

## Phase 5: Build and Validate

### Subphase 5.1: Build Project

**Step 5.1.1:** Run Gradle build
```bash
./gradlew clean build
```

**Expected:** BUILD SUCCESSFUL

### Subphase 5.2: Run Tests

**Step 5.2.1:** Run all tests
```bash
./gradlew test
./gradlew connectedAndroidTest
```

**Expected:** All tests passing

### Subphase 5.3: Manual Testing

**Step 5.3.1:** Test Teams app
- Reset Teams app
- Let exploration run
- Verify stats show 254 elements (not 5)

**Step 5.3.2:** Test RealWear app
- Reset RealWear app
- Let exploration run
- Verify stats correct

---

## Phase 6: Commit and Push

### Subphase 6.1: Stage Changes

```bash
git add .
```

### Subphase 6.2: Commit

```bash
git commit -m "feat(database): Consolidate LearnApp and Scraping databases

Merge LearnAppDatabase and AppScrapingDatabase into unified VoiceOSAppDatabase
to eliminate data synchronization issues and simplify data model.

Changes:
- Created VoiceOSAppDatabase with unified schema
- Merged AppEntity (learned_apps + scraped_apps)
- Merged ScreenEntity (screen_states + screen_contexts)
- Implemented DatabaseMigrationHelper for data migration
- Updated ExplorationEngine to use new database
- Updated VoiceCommandProcessor to use new database
- Updated LearnAppIntegration to use new database
- Added validation using UUIDCreator as source of truth
- Created comprehensive unit and integration tests
- All tests passing

Benefits:
- Eliminates stats mismatches (254 vs 85 vs 5 elements)
- Single source of truth for app data
- Atomic transactions across exploration and scraping
- Simpler queries (no cross-database logic)
- UUIDCreator stays independent (correct architecture)

Files Changed:
- Created: VoiceOSAppDatabase.kt and all entities/DAOs
- Created: DatabaseMigrationHelper.kt
- Updated: ExplorationEngine.kt
- Updated: VoiceCommandProcessor.kt
- Updated: LearnAppIntegration.kt
- Updated: All test files
- Added: Comprehensive test suite

Build Status: BUILD SUCCESSFUL
Tests: ALL PASSING
Migration: Validated with Teams and RealWear apps"
```

### Subphase 6.3: Push to Branch

```bash
git push -u origin voiceos-database-update
```

---

## Success Criteria Checklist

- [ ] VoiceOSAppDatabase created with all entities
- [ ] AppEntity and ScreenEntity properly merged
- [ ] All DAOs created and functional
- [ ] DatabaseMigrationHelper implemented
- [ ] Migration from LearnAppDatabase working
- [ ] Migration from AppScrapingDatabase working
- [ ] Element count validation using UUIDCreator
- [ ] ExplorationEngine updated
- [ ] VoiceCommandProcessor updated
- [ ] LearnAppIntegration updated
- [ ] All test files updated
- [ ] Unit tests created and passing
- [ ] Migration tests created and passing
- [ ] Integration tests created and passing
- [ ] Build successful
- [ ] Teams app shows 254 elements (not 5)
- [ ] RealWear app stats correct
- [ ] No data loss verified
- [ ] Code committed to voiceos-database-update branch
- [ ] Code pushed to remote

---

## Rollback Plan

If anything fails:
1. Switch back to voiceos-development branch
2. Old databases still intact
3. No data loss
4. Fix issues and retry

---

**Plan Version:** 1.0
**Status:** Ready for Implementation (YOLO Mode)
**Start Time:** 2025-10-30 02:32 PDT
