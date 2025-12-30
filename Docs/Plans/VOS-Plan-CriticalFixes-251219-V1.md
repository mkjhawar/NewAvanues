# VoiceOS Critical Fixes - Implementation Plan

**Project:** NewAvanues Monorepo
**Module:** VoiceOS
**Plan Date:** 2025-12-19
**Version:** V1
**Based On:** NAV-Master-Analysis-251219-V1.md (214 critical issues identified)

---

## EXECUTIVE SUMMARY

This plan addresses **214 critical issues** identified across 8 domains by PhD-level expert analysis. The implementation follows an 8-week roadmap prioritized by impact, with Week 1 focused on unblocking builds and fixing P0 runtime crashes.

**Overall System Health:** C- (68/100) → Target: A- (90+)

**Critical Path:** Fix build blockers → Enable foreign keys → Fix memory leaks → Add state machines → Refactor god objects → Complete integration tests

---

## PHASE 1: UNBLOCK BUILDS & CRITICAL FIXES (Week 1)

**Timeline:** Days 1-5 (40 hours)
**Focus:** Fix all build-blocking issues and P0 runtime crashes
**Success Criteria:** All 3 apps compile, no ANR crashes, memory leaks <10KB/hour

### Day 1-2: Build System & Database Core (Priority 1)

#### Task 1.1: Fix SQLDelight Test Failures (BLOCKING BUILD)
**File:** `Modules/VoiceOS/core/database/src/jvmTest/kotlin/com/augmentalis/database/DatabaseTest.kt`
**Issue:** 48 compilation errors - missing 5 columns added in Schema v3
**Time:** 2 hours

**Changes Required:**
```kotlin
// DatabaseTest.kt - Update test data generators

// BEFORE (missing 5 columns)
fun createTestCommand() = GeneratedCommandDTO(
    id = 0,
    elementHash = "test-hash",
    commandText = "test command",
    actionType = "CLICK",
    confidence = 0.95,
    synonyms = null,
    isUserApproved = 0,
    usageCount = 0,
    lastUsed = null,
    createdAt = System.currentTimeMillis()
    // Missing: appId, appVersion, versionCode, lastVerified, isDeprecated
)

// AFTER (add missing columns)
fun createTestCommand() = GeneratedCommandDTO(
    id = 0,
    elementHash = "test-hash",
    commandText = "test command",
    actionType = "CLICK",
    confidence = 0.95,
    synonyms = null,
    isUserApproved = 0,
    usageCount = 0,
    lastUsed = null,
    createdAt = System.currentTimeMillis(),
    appId = "com.test.app",           // NEW
    appVersion = "1.0.0",              // NEW
    versionCode = 1,                   // NEW
    lastVerified = System.currentTimeMillis(), // NEW
    isDeprecated = 0                   // NEW
)
```

**Files to Update:**
- `DatabaseTest.kt` (all 15+ test data generators)
- `RepositoryIntegrationTest.kt` (10+ generators)
- `GeneratedCommandRepositoryTest.kt` (8+ generators)
- `CustomCommandRepositoryTest.kt` (5+ generators)

**Validation:**
```bash
./gradlew :Modules:VoiceOS:core:database:test
# Expected: 0 failures, ~120 tests pass
```

---

#### Task 1.2: Fix Dispatcher.IO Crash in KMP Code
**File:** `Modules/VoiceOS/core/database/src/commonMain/kotlin/com/augmentalis/database/repositories/impl/SQLDelightGeneratedCommandRepository.kt:377`
**Issue:** Using Dispatchers.IO in KMP common code - crashes on iOS/JS
**Time:** 5 minutes

**Fix:**
```kotlin
// WRONG (line 377)
override suspend fun vacuumDatabase() = withContext(Dispatchers.IO) {
    queries.vacuumDatabase()
}

// FIX
override suspend fun vacuumDatabase() = withContext(Dispatchers.Default) {
    queries.vacuumDatabase()
}
```

**Validation:**
```bash
# Run KMP tests on all platforms
./gradlew :Modules:VoiceOS:core:database:allTests
# Expected: iOS/JS simulator tests pass
```

---

#### Task 1.3: Enable Foreign Key Constraints (CRITICAL)
**Location:** Create `Modules/VoiceOS/core/database/src/androidMain/kotlin/com/augmentalis/database/DatabaseDriverFactory.kt`
**Issue:** All 20 FK constraints ignored - silent data corruption risk
**Time:** 30 minutes

**Implementation:**
```kotlin
package com.augmentalis.database

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

actual class DatabaseDriverFactory(
    private val context: Context
) {
    actual fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(
            schema = VoiceOSDatabase.Schema,
            context = context,
            name = "voiceos.db",
            callback = object : AndroidSqliteDriver.Callback(VoiceOSDatabase.Schema) {
                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)

                    // CRITICAL: Enable foreign key constraints
                    db.execSQL("PRAGMA foreign_keys = ON")

                    // Performance optimization
                    db.execSQL("PRAGMA journal_mode = WAL")
                    db.execSQL("PRAGMA synchronous = NORMAL")
                }
            }
        )
    }
}
```

**Also create iOS/JVM/JS variants** in respective source sets.

**Validation:**
```kotlin
// Add integration test
@Test
fun `foreign keys are enforced`() {
    val db = createTestDatabase()

    // Try to insert command with non-existent elementHash
    assertThrows<SQLException> {
        db.generatedCommandQueries.insert(
            elementHash = "non-existent-hash",  // No matching scraped_element
            // ... other fields
        )
    }
}
```

---

#### Task 1.4: Add Missing packageManager Implementation
**File:** `Modules/VoiceOS/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/accessibility/VoiceOSService.kt`
**Issue:** Missing IVoiceOSContext.packageManager - causes AbstractMethodError
**Time:** 15 minutes

**Fix:**
```kotlin
// VoiceOSService.kt - Add after line 2216

override val packageManager: PackageManager
    get() = applicationContext.packageManager
```

**Validation:**
```bash
# Build and install
./gradlew :Modules:VoiceOS:apps:VoiceOSCore:assembleDebug
adb install -r app.apk

# Test: Enable service, speak "open settings"
# Expected: No AbstractMethodError crash
```

---

#### Task 1.5: Standardize minSdk Across All Modules
**Issue:** minSdk mismatch (24 vs 26) causes compilation failures
**Time:** 1 hour

**Files to Update:**
```kotlin
// Set ALL modules to minSdk 26 (VoiceOSCore requirement)

// Modules/VoiceOS/apps/VoiceOSCore/build.gradle.kts
android {
    defaultConfig {
        minSdk = 26  // ✅ Already correct
    }
}

// Modules/VoiceOS/apps/LearnApp/build.gradle.kts
android {
    defaultConfig {
        minSdk = 26  // CHANGE from 24 → 26
    }
}

// Modules/VoiceOS/apps/LearnAppDev/build.gradle.kts
android {
    defaultConfig {
        minSdk = 26  // CHANGE from 24 → 26
    }
}

// ALL library modules (9 files)
android {
    defaultConfig {
        minSdk = 26  // Standardize
    }
}
```

**Validation:**
```bash
./gradlew clean
./gradlew :Modules:VoiceOS:apps:VoiceOSCore:assembleDebug
./gradlew :Modules:VoiceOS:apps:LearnApp:assembleDebug
./gradlew :Modules:VoiceOS:apps:LearnAppDev:assembleDebug
# Expected: All build successfully
```

---

### Day 3-5: Critical Concurrency & Memory Leaks

#### Task 1.6: Fix runBlocking on Main Thread (ANR Risk)
**File:** `Modules/VoiceOS/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/accessibility/managers/ActionCoordinator.kt:307-311`
**Issue:** Blocks main thread for up to 5 seconds - causes ANR
**Time:** 4 hours

**Current Code:**
```kotlin
// WRONG (lines 307-311)
fun executeAction(
    action: String,
    params: Map<String, Any> = emptyMap()
): Boolean {
    return runBlocking {  // ← BLOCKS CALLING THREAD
        withTimeoutOrNull(HANDLER_TIMEOUT_MS) {
            handler.execute(category, action, params)
        }
    } ?: false
}
```

**Fix Strategy:**
```kotlin
// OPTION 1: Make suspend function (RECOMMENDED)
suspend fun executeAction(
    action: String,
    params: Map<String, Any> = emptyMap()
): Boolean = withContext(Dispatchers.Default) {
    withTimeoutOrNull(HANDLER_TIMEOUT_MS) {
        handler.execute(category, action, params)
    } ?: false
}

// OPTION 2: Return Deferred (if async needed)
fun executeActionAsync(
    action: String,
    params: Map<String, Any> = emptyMap()
): Deferred<Boolean> = CoroutineScope(Dispatchers.Default).async {
    withTimeoutOrNull(HANDLER_TIMEOUT_MS) {
        handler.execute(category, action, params)
    } ?: false
}

// OPTION 3: Callback-based (if callers can't be suspend)
fun executeAction(
    action: String,
    params: Map<String, Any> = emptyMap(),
    onResult: (Boolean) -> Unit
) {
    CoroutineScope(Dispatchers.Default).launch {
        val result = withTimeoutOrNull(HANDLER_TIMEOUT_MS) {
            handler.execute(category, action, params)
        } ?: false
        withContext(Dispatchers.Main) {
            onResult(result)
        }
    }
}
```

**Impact Analysis:**
- Find all callers of `executeAction()`
- Update to use suspend or callback pattern
- Estimated 20+ call sites to update

**Validation:**
```bash
# Stress test: Rapid command execution
# Monitor ANR via: adb shell dumpsys activity processes
# Expected: No ANR traces
```

---

#### Task 1.7: Fix AccessibilityNodeInfo Memory Leak
**File:** `Modules/VoiceOS/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/accessibility/VoiceOSService.kt:1350`
**Issue:** AccessibilityNodeInfo not recycled - 100-250KB leak per cycle
**Time:** 1 hour

**Fix:**
```kotlin
// Event queue processing (line 1350)

// WRONG
queuedEvent.recycle()  // Only recycles event, not contained nodes

// FIX
try {
    learnAppIntegration?.onAccessibilityEvent(queuedEvent)
} finally {
    val source = queuedEvent.source
    source?.recycle()  // ← CRITICAL: Recycle node first
    queuedEvent.recycle()
}
```

**Additional Leaks to Fix:**
```kotlin
// UIScrapingEngine.kt:226, 357 - Uncomment recycle() calls
// WRONG
// rootNode.recycle() // Deprecated - Android handles this automatically

// FIX (Android does NOT auto-recycle)
rootNode.recycle()
child?.recycle()
```

**Validation:**
```bash
# Use LeakCanary or Memory Profiler
# Scrape 10 screens, observe memory growth
# Expected: <10KB leak (down from 250KB)
```

---

#### Task 1.8: Fix Nested Transaction Deadlock
**File:** `Modules/VoiceOS/core/database/src/commonMain/kotlin/com/augmentalis/database/repositories/impl/SQLDelightCustomCommandRepository.kt:23-40`
**Issue:** Nested transactions cause deadlock
**Time:** 2 hours

**Fix:**
```kotlin
// WRONG
override suspend fun insert(command: CustomCommandDTO): Long = withContext(Dispatchers.Default) {
    queries.insert(...)
    queries.transactionWithResult {  // ← NESTED TRANSACTION
        queries.lastInsertRowId().executeAsOne()
    }
}

// FIX
override suspend fun insert(command: CustomCommandDTO): Long = withContext(Dispatchers.Default) {
    database.transactionWithResult {  // Single transaction
        queries.insert(...)
        queries.lastInsertRowId().executeAsOne()
    }
}
```

**Apply to All Repositories:**
- SQLDelightCustomCommandRepository
- SQLDelightGeneratedCommandRepository
- SQLDelightElementDatabaseRepository
- (All 20+ repositories)

---

#### Task 1.9: Add Command Execution State Machine
**Location:** Create `Modules/VoiceOS/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/learnapp/CommandExecutionStateMachine.kt`
**Issue:** No tracking of execution lifecycle - silent failures
**Time:** 8 hours

**State Machine Design:**
```kotlin
package com.augmentalis.voiceoscore.learnapp

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

sealed class CommandExecutionState {
    object Idle : CommandExecutionState()
    data class Pending(val commandId: Long, val timestamp: Long) : CommandExecutionState()
    data class Executing(val commandId: Long, val startTime: Long) : CommandExecutionState()
    data class Completed(val commandId: Long, val duration: Long) : CommandExecutionState()
    data class Failed(val commandId: Long, val error: String, val retryCount: Int) : CommandExecutionState()
}

class CommandExecutionStateMachine {
    private val _state = MutableStateFlow<CommandExecutionState>(CommandExecutionState.Idle)
    val state: StateFlow<CommandExecutionState> = _state

    private val executionHistory = mutableListOf<CommandExecutionState>()
    private val maxRetries = 3

    suspend fun startExecution(commandId: Long) {
        val currentState = _state.value

        // Validate state transition
        require(currentState is CommandExecutionState.Idle ||
                currentState is CommandExecutionState.Completed ||
                currentState is CommandExecutionState.Failed) {
            "Cannot start execution from state: $currentState"
        }

        val newState = CommandExecutionState.Pending(commandId, System.currentTimeMillis())
        _state.emit(newState)
        executionHistory.add(newState)
    }

    suspend fun markExecuting(commandId: Long) {
        val currentState = _state.value
        require(currentState is CommandExecutionState.Pending) {
            "Cannot mark executing from state: $currentState"
        }

        val newState = CommandExecutionState.Executing(commandId, System.currentTimeMillis())
        _state.emit(newState)
        executionHistory.add(newState)
    }

    suspend fun markCompleted(commandId: Long) {
        val currentState = _state.value
        require(currentState is CommandExecutionState.Executing) {
            "Cannot mark completed from state: $currentState"
        }

        val duration = System.currentTimeMillis() - currentState.startTime
        val newState = CommandExecutionState.Completed(commandId, duration)
        _state.emit(newState)
        executionHistory.add(newState)

        // Auto-transition to Idle after 2 seconds
        kotlinx.coroutines.delay(2000)
        _state.emit(CommandExecutionState.Idle)
    }

    suspend fun markFailed(commandId: Long, error: String) {
        val currentState = _state.value
        require(currentState is CommandExecutionState.Pending ||
                currentState is CommandExecutionState.Executing) {
            "Cannot mark failed from state: $currentState"
        }

        val retryCount = executionHistory
            .filterIsInstance<CommandExecutionState.Failed>()
            .count { it.commandId == commandId }

        val newState = CommandExecutionState.Failed(commandId, error, retryCount)
        _state.emit(newState)
        executionHistory.add(newState)

        // Auto-retry if under limit
        if (retryCount < maxRetries) {
            kotlinx.coroutines.delay(1000L * (retryCount + 1))  // Exponential backoff
            startExecution(commandId)
        } else {
            // Max retries exceeded, transition to Idle
            kotlinx.coroutines.delay(5000)
            _state.emit(CommandExecutionState.Idle)
        }
    }

    fun getExecutionHistory(): List<CommandExecutionState> = executionHistory.toList()

    fun clearHistory() {
        executionHistory.clear()
    }
}
```

**Integration Points:**
- Inject into `LearnAppIntegration`
- Use in `ActionCoordinator.executeAction()`
- Add UI feedback in overlays

---

#### Task 1.10: Add Database Initialization Validation
**File:** `Modules/VoiceOS/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/accessibility/VoiceOSService.kt`
**Issue:** Database accessed before initialization complete
**Time:** 4 hours

**Implementation:**
```kotlin
class VoiceOSService : AccessibilityService() {

    private sealed class InitializationState {
        object NotStarted : InitializationState()
        object InProgress : InitializationState()
        data class Completed(val timestamp: Long) : InitializationState()
        data class Failed(val error: String) : InitializationState()
    }

    private val initState = MutableStateFlow<InitializationState>(InitializationState.NotStarted)

    override fun onServiceConnected() {
        super.onServiceConnected()

        lifecycleScope.launch {
            initState.emit(InitializationState.InProgress)

            try {
                // Wait for database initialization
                withTimeout(10_000) {
                    databaseAdapter.waitForInitialization()
                }

                // Verify foreign keys enabled
                val fkEnabled = databaseAdapter.database
                    .rawQuery("PRAGMA foreign_keys")
                    .use { cursor ->
                        cursor.moveToFirst() && cursor.getInt(0) == 1
                    }

                if (!fkEnabled) {
                    throw IllegalStateException("Foreign keys not enabled!")
                }

                initState.emit(InitializationState.Completed(System.currentTimeMillis()))

            } catch (e: Exception) {
                initState.emit(InitializationState.Failed(e.message ?: "Unknown error"))
                // Show error to user
                showErrorNotification("VoiceOS initialization failed: ${e.message}")
            }
        }
    }

    // Guard all database access
    private suspend fun <T> withDatabaseReady(block: suspend () -> T): T {
        val state = initState.first { it is InitializationState.Completed || it is InitializationState.Failed }

        return when (state) {
            is InitializationState.Completed -> block()
            is InitializationState.Failed -> throw IllegalStateException("Database not initialized: ${state.error}")
            else -> throw IllegalStateException("Unexpected state: $state")
        }
    }
}
```

---

### Week 1 Deliverables Checklist

- [ ] **Build System:** All 3 apps compile without errors
- [ ] **Tests:** 120+ database tests pass
- [ ] **Foreign Keys:** Enabled and enforced
- [ ] **Memory Leaks:** Reduced from 250KB/cycle to <10KB/cycle
- [ ] **ANR:** No runBlocking on main thread
- [ ] **Crashes:** No Dispatcher.IO crash, no AbstractMethodError
- [ ] **State Machine:** Command execution fully tracked
- [ ] **Database:** Initialization validated before access

**Validation Command:**
```bash
# Build all modules
./gradlew :Modules:VoiceOS:apps:VoiceOSCore:assembleDebug
./gradlew :Modules:VoiceOS:apps:LearnApp:assembleDebug
./gradlew :Modules:VoiceOS:apps:LearnAppDev:assembleDebug

# Run all tests
./gradlew :Modules:VoiceOS:core:database:test
./gradlew :Modules:VoiceOS:apps:VoiceOSCore:testDebugUnitTest

# Install and test
adb install -r VoiceOSCore.apk
# Expected: Service starts, commands work, no crashes
```

---

## PHASE 2: VERSION DEPRECATION & INTEGRATION TESTS (Week 2-3)

**Timeline:** Days 6-15 (80 hours)
**Focus:** Implement missing version tracking system and foundational integration tests

### Day 6-10: Version Deprecation System

#### Task 2.1: Implement App Version Detection
**Location:** Create `Modules/VoiceOS/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/versioning/AppVersionDetector.kt`
**Time:** 4 hours

**Implementation:**
```kotlin
package com.augmentalis.voiceoscore.versioning

import android.content.Context
import android.content.pm.PackageManager

data class AppVersionInfo(
    val packageName: String,
    val versionName: String,
    val versionCode: Long
)

class AppVersionDetector(
    private val context: Context
) {
    fun getInstalledAppVersion(packageName: String): AppVersionInfo? {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(packageName, 0)
            AppVersionInfo(
                packageName = packageName,
                versionName = packageInfo.versionName ?: "unknown",
                versionCode = if (android.os.Build.VERSION.SDK_INT >= 28) {
                    packageInfo.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo.versionCode.toLong()
                }
            )
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    fun getAllInstalledApps(): List<AppVersionInfo> {
        return context.packageManager.getInstalledApplications(0)
            .mapNotNull { appInfo ->
                getInstalledAppVersion(appInfo.packageName)
            }
    }
}
```

---

#### Task 2.2: Build Version Tracking Manager
**Location:** Create `Modules/VoiceOS/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/versioning/VersionTrackingManager.kt`
**Time:** 8 hours

**Implementation:**
```kotlin
package com.augmentalis.voiceoscore.versioning

import com.augmentalis.database.repositories.IGeneratedCommandRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

data class VersionChange(
    val packageName: String,
    val oldVersion: AppVersionInfo?,
    val newVersion: AppVersionInfo?,
    val changeType: ChangeType
) {
    enum class ChangeType {
        INSTALLED,   // New app
        UPDATED,     // Version changed
        UNINSTALLED, // App removed
        UNCHANGED    // No change
    }
}

class VersionTrackingManager(
    private val versionDetector: AppVersionDetector,
    private val commandRepository: IGeneratedCommandRepository
) {
    private val _versionChanges = MutableStateFlow<List<VersionChange>>(emptyList())
    val versionChanges: StateFlow<List<VersionChange>> = _versionChanges

    // Cache of last known versions
    private val versionCache = mutableMapOf<String, AppVersionInfo>()

    suspend fun detectVersionChanges(): List<VersionChange> = withContext(Dispatchers.Default) {
        val currentApps = versionDetector.getAllInstalledApps()
        val currentPackages = currentApps.associateBy { it.packageName }

        val changes = mutableListOf<VersionChange>()

        // Check for updates and new installs
        currentApps.forEach { currentVersion ->
            val cachedVersion = versionCache[currentVersion.packageName]

            val change = when {
                cachedVersion == null -> VersionChange(
                    packageName = currentVersion.packageName,
                    oldVersion = null,
                    newVersion = currentVersion,
                    changeType = VersionChange.ChangeType.INSTALLED
                )

                cachedVersion.versionCode != currentVersion.versionCode -> VersionChange(
                    packageName = currentVersion.packageName,
                    oldVersion = cachedVersion,
                    newVersion = currentVersion,
                    changeType = VersionChange.ChangeType.UPDATED
                )

                else -> VersionChange(
                    packageName = currentVersion.packageName,
                    oldVersion = cachedVersion,
                    newVersion = currentVersion,
                    changeType = VersionChange.ChangeType.UNCHANGED
                )
            }

            if (change.changeType != VersionChange.ChangeType.UNCHANGED) {
                changes.add(change)
            }
        }

        // Check for uninstalls
        versionCache.keys.forEach { packageName ->
            if (packageName !in currentPackages) {
                changes.add(VersionChange(
                    packageName = packageName,
                    oldVersion = versionCache[packageName],
                    newVersion = null,
                    changeType = VersionChange.ChangeType.UNINSTALLED
                ))
            }
        }

        // Update cache
        versionCache.clear()
        currentApps.forEach { versionCache[it.packageName] = it }

        _versionChanges.emit(changes)
        changes
    }

    suspend fun handleVersionChange(change: VersionChange) {
        when (change.changeType) {
            VersionChange.ChangeType.UPDATED -> handleAppUpdate(change)
            VersionChange.ChangeType.UNINSTALLED -> handleAppUninstall(change)
            else -> { /* No action needed */ }
        }
    }

    private suspend fun handleAppUpdate(change: VersionChange) {
        val oldVersion = change.oldVersion?.versionCode ?: return

        // Mark old version commands as deprecated
        val affectedCount = commandRepository.markVersionDeprecated(
            packageName = change.packageName,
            versionCode = oldVersion
        )

        println("VoiceOS: Marked $affectedCount commands as deprecated for ${change.packageName} v$oldVersion")
    }

    private suspend fun handleAppUninstall(change: VersionChange) {
        // Delete all commands for uninstalled app
        val deletedCount = commandRepository.deleteCommandsByPackage(change.packageName)

        println("VoiceOS: Deleted $deletedCount commands for uninstalled app ${change.packageName}")
    }
}
```

---

#### Task 2.3: Build Cleanup Worker
**Location:** Create `Modules/VoiceOS/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/versioning/DeprecatedCommandCleanupWorker.kt`
**Time:** 4 hours

**Implementation:**
```kotlin
package com.augmentalis.voiceoscore.versioning

import android.content.Context
import androidx.work.*
import com.augmentalis.database.repositories.IGeneratedCommandRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class DeprecatedCommandCleanupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val WORK_NAME = "deprecated_command_cleanup"
        private const val GRACE_PERIOD_DAYS = 30L

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .setRequiresDeviceIdle(true)
                .build()

            val request = PeriodicWorkRequestBuilder<DeprecatedCommandCleanupWorker>(
                repeatInterval = 7,  // Run weekly
                repeatIntervalTimeUnit = TimeUnit.DAYS
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.Default) {
        try {
            // Inject repository (use Hilt in production)
            val repository: IGeneratedCommandRepository = getRepository()

            val gracePeriodMs = System.currentTimeMillis() -
                (GRACE_PERIOD_DAYS * 24 * 60 * 60 * 1000)

            // Delete deprecated commands older than grace period
            // Keep user-approved commands
            val deletedCount = repository.deleteDeprecatedCommands(
                olderThan = gracePeriodMs,
                keepUserApproved = true
            )

            println("VoiceOS: Cleanup deleted $deletedCount deprecated commands")

            // Vacuum database if significant deletions
            if (deletedCount > 100) {
                repository.vacuumDatabase()
                println("VoiceOS: Database vacuumed after cleanup")
            }

            Result.success()

        } catch (e: Exception) {
            println("VoiceOS: Cleanup failed: ${e.message}")
            Result.retry()
        }
    }

    private fun getRepository(): IGeneratedCommandRepository {
        // TODO: Use Hilt injection
        throw NotImplementedError("Inject via Hilt")
    }
}
```

---

### Day 11-15: Integration Testing Foundation

#### Task 2.4: Create Integration Test Framework
**Location:** `Modules/VoiceOS/apps/VoiceOSCore/src/androidTest/kotlin/com/augmentalis/voiceoscore/integration/IntegrationTestBase.kt`
**Time:** 16 hours

**Framework Design:**
```kotlin
package com.augmentalis.voiceoscore.integration

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.augmentalis.database.VoiceOSDatabase
import com.augmentalis.database.repositories.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
abstract class IntegrationTestBase {

    protected lateinit var context: Context
    protected lateinit var database: VoiceOSDatabase
    protected lateinit var commandRepository: IGeneratedCommandRepository
    protected lateinit var elementRepository: IScrapedElementRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()

        // Create test database
        database = createTestDatabase()

        // Create repositories
        commandRepository = SQLDelightGeneratedCommandRepository(database)
        elementRepository = SQLDelightScrapedElementRepository(database)
    }

    @After
    fun teardown() = runBlocking {
        // Clean up database
        database.close()
    }

    private fun createTestDatabase(): VoiceOSDatabase {
        // Create in-memory database for tests
        val driver = createInMemoryDriver(context)
        return VoiceOSDatabase(driver)
    }

    // Test helpers
    protected fun runTest(block: suspend () -> Unit) = runBlocking {
        block()
    }

    protected suspend fun insertTestElement(packageName: String): String {
        val element = createTestScrapedElement(packageName)
        elementRepository.insert(element)
        return element.elementHash
    }

    protected suspend fun insertTestCommand(
        elementHash: String,
        packageName: String
    ): Long {
        val command = createTestGeneratedCommand(elementHash, packageName)
        return commandRepository.insert(command)
    }
}
```

---

#### Task 2.5: Write Workflow 1 Integration Tests (New User Onboarding)
**Time:** 8 hours

**Test Cases:**
```kotlin
@Test
fun `workflow 1 - new user enables service for first time`() = runTest {
    // ARRANGE
    val testService = createTestVoiceOSService()

    // ACT
    testService.simulateServiceConnected()

    // ASSERT
    // 1. Database initialized
    assertTrue(testService.isDatabaseReady())

    // 2. Foreign keys enabled
    assertTrue(testService.areForeignKeysEnabled())

    // 3. Onboarding prompt shown
    assertTrue(testService.isOnboardingShown())

    // 4. No commands exist yet
    assertEquals(0, commandRepository.count())
}

@Test
fun `workflow 1 - user grants permissions and explores first app`() = runTest {
    // ARRANGE
    val testService = createTestVoiceOSService()
    testService.simulateServiceConnected()

    // ACT
    testService.simulateWindowStateChanged("com.android.settings")
    testService.simulateAccessibilityEvent(TYPE_WINDOW_CONTENT_CHANGED)

    // ASSERT
    // 1. Elements scraped
    val elements = elementRepository.getByPackage("com.android.settings")
    assertTrue(elements.isNotEmpty())

    // 2. Commands generated
    val commands = commandRepository.getByPackage("com.android.settings")
    assertTrue(commands.isNotEmpty())

    // 3. Commands have correct version
    commands.forEach { command ->
        assertEquals("com.android.settings", command.appId)
        assertTrue(command.versionCode > 0)
    }
}
```

---

#### Task 2.6: Write Workflow 2 Integration Tests (JIT Learning)
**Time:** 8 hours

**Test Cases:**
```kotlin
@Test
fun `workflow 2 - JIT learning generates commands for new element`() = runTest {
    // ARRANGE
    val elementHash = insertTestElement("com.test.app")

    // ACT
    val learnedCommands = jitLearner.learnFromElement(elementHash)

    // ASSERT
    assertTrue(learnedCommands.isNotEmpty())
    assertEquals(elementHash, learnedCommands.first().elementHash)
}

@Test
fun `workflow 2 - user approves generated command`() = runTest {
    // ARRANGE
    val commandId = insertTestCommand("test-hash", "com.test.app")

    // ACT
    commandRepository.markApproved(commandId)

    // ASSERT
    val approved = commandRepository.getUserApproved()
    assertTrue(approved.any { it.id == commandId })
}

@Test
fun `workflow 2 - command execution increments usage count`() = runTest {
    // ARRANGE
    val commandId = insertTestCommand("test-hash", "com.test.app")
    val initialUsage = commandRepository.getById(commandId)?.usageCount ?: 0

    // ACT
    commandRepository.incrementUsage(commandId, System.currentTimeMillis())

    // ASSERT
    val updatedUsage = commandRepository.getById(commandId)?.usageCount ?: 0
    assertEquals(initialUsage + 1, updatedUsage)
}
```

---

#### Task 2.7: Write Workflow 5 Integration Tests (Database Migration)
**Time:** 8 hours

**Test Cases:**
```kotlin
@Test
fun `workflow 5 - migrate from schema v12 to v15`() = runTest {
    // ARRANGE
    val v12Database = createDatabaseAtVersion(12)
    seedDatabaseWithTestData(v12Database)

    // ACT
    val v15Database = migrateTo(15)

    // ASSERT
    // 1. All data preserved
    val commands = v15Database.generatedCommandQueries.getAll().executeAsList()
    assertTrue(commands.isNotEmpty())

    // 2. New columns have default values
    commands.forEach { command ->
        assertEquals("", command.appId)  // Default for existing commands
        assertEquals(0L, command.versionCode)
        assertEquals(0L, command.isDeprecated)
    }
}

@Test
fun `workflow 5 - foreign keys preserved after migration`() = runTest {
    // ARRANGE
    val database = migrateTo(15)

    // ACT & ASSERT
    assertThrows<SQLException> {
        database.generatedCommandQueries.insert(
            elementHash = "non-existent",
            // ... other fields
        )
    }
}
```

---

### Week 2-3 Deliverables Checklist

- [ ] **Version Detection:** AppVersionDetector works for all installed apps
- [ ] **Version Tracking:** VersionTrackingManager detects updates/uninstalls
- [ ] **Deprecation:** Commands marked deprecated on app update
- [ ] **Cleanup:** Worker deletes deprecated commands after 30 days
- [ ] **Integration Tests:** 31+ tests covering 3 workflows
- [ ] **Foreign Key Tests:** FK constraints validated in integration tests
- [ ] **Test Framework:** Reusable base class for future tests

---

## PHASE 3: SOLID REFACTORING (Week 4-5)

**Timeline:** Days 16-25 (80 hours)
**Focus:** Extract remaining responsibilities from VoiceOSService
**Target:** VoiceOSService <500 lines, SOLID compliance 90%+

### Manager Extraction Tasks

#### Task 3.1: Extract DatabaseInitializationManager
**Time:** 8 hours
**Lines Removed:** ~150 from VoiceOSService

#### Task 3.2: Extract LearnAppCoordinator
**Time:** 16 hours
**Lines Removed:** ~400 from VoiceOSService

#### Task 3.3: Extract ScrapingCoordinator
**Time:** 16 hours
**Lines Removed:** ~300 from VoiceOSService

#### Task 3.4: Extract VersionTrackingManager
**Time:** 8 hours
**Lines Removed:** ~200 from VoiceOSService

#### Task 3.5: Refactor VoiceOSCoreDatabaseAdapter
**Time:** 16 hours
**Focus:** Remove god object pattern

---

## PHASE 4: REMAINING FIXES & TESTING (Week 6-8)

**Timeline:** Days 26-40 (120 hours)
**Focus:** Complete all P1/P2 issues, full test coverage

### Testing Tasks

#### Task 4.1: Complete Integration Test Suite
**Time:** 32 hours
**Target:** 70+ integration tests

#### Task 4.2: Performance Testing
**Time:** 16 hours
**Metrics:** Memory, CPU, battery, latency

#### Task 4.3: End-to-End Testing
**Time:** 16 hours
**Scenarios:** 5 critical user flows

---

## SUCCESS METRICS

| Metric | Current | Week 1 | Week 3 | Week 5 | Week 8 |
|--------|---------|--------|--------|--------|--------|
| **Build Success** | 0% | 100% | 100% | 100% | 100% |
| **P0 Issues** | 10 | 0 | 0 | 0 | 0 |
| **Memory Leaks** | 250KB/cycle | <10KB | <5KB | <2KB | <1KB |
| **ANR Crashes** | High | 0 | 0 | 0 | 0 |
| **Test Coverage** | 40% | 50% | 70% | 85% | 90%+ |
| **SOLID Compliance** | 60% | 60% | 65% | 90% | 95% |
| **Integration Tests** | 0 | 10 | 31 | 50 | 70+ |
| **System Grade** | C- (68) | C+ (75) | B- (80) | A- (90) | A (95) |

---

## PRIORITY MATRIX

### P0 - Fix Immediately (Week 1)
1. SQLDelight test failures (BLOCKING)
2. Dispatcher.IO crash
3. Foreign keys not enabled
4. runBlocking ANR
5. Missing packageManager
6. AccessibilityNodeInfo leak
7. Nested transaction deadlock
8. Command execution state machine
9. Database init validation
10. minSdk mismatch

### P1 - Fix Soon (Week 2-3)
- Version deprecation system
- Integration test framework
- Workflow 1-3 integration tests
- State read delegation (Compose)
- UIScrapingEngine recycle() calls

### P2 - Fix Later (Week 4-8)
- SOLID refactoring
- Complete integration tests
- Performance optimization
- Documentation updates

---

## VALIDATION COMMANDS

```bash
# Week 1 Validation
./gradlew clean
./gradlew :Modules:VoiceOS:core:database:test
./gradlew :Modules:VoiceOS:apps:VoiceOSCore:assembleDebug
./gradlew :Modules:VoiceOS:apps:LearnApp:assembleDebug
./gradlew :Modules:VoiceOS:apps:LearnAppDev:assembleDebug

# Install and test
adb install -r VoiceOSCore.apk
# Expected: Service starts, no crashes, commands work

# Week 3 Validation
./gradlew :Modules:VoiceOS:apps:VoiceOSCore:connectedAndroidTest
# Expected: 31+ integration tests pass

# Week 8 Validation
./gradlew test
./gradlew connectedAndroidTest
# Expected: 90%+ coverage, all tests pass
```

---

## RISK MITIGATION

| Risk | Mitigation |
|------|------------|
| Breaking changes | Comprehensive integration tests before each merge |
| Database corruption | Pre-migration backups, foreign keys, validation |
| Performance regression | Benchmark before/after, memory profiler |
| Schedule slippage | 20% buffer in estimates, daily progress tracking |

---

## NEXT STEPS

1. **Review this plan** with stakeholders
2. **Approve Week 1 tasks** (Days 1-5)
3. **Create TodoWrite tasks** for Week 1
4. **Begin implementation** with Task 1.1 (SQLDelight tests)
5. **Daily standup** to track progress
6. **Weekly retrospective** to adjust plan

---

**Plan Created:** 2025-12-19
**Estimated Completion:** 2025-02-13 (8 weeks)
**Total Effort:** 320 hours (40 hours/week)

---

**Ready to proceed with implementation using /i.implement or /i.fix**
