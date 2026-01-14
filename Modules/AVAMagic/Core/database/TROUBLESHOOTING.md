# Database Troubleshooting Guide

**Module:** libraries/core/database
**Last Updated:** 2025-12-02
**Version:** 1.1.0

---

## Critical Issues

### SQLITE_BUSY / Database Lock Errors

**Symptoms:**
- `SQLiteDatabaseLockedException: database is locked (code 5 SQLITE_BUSY)`
- Cascade timeout errors across multiple components
- Commands fail with "All tiers failed" messages
- LearnApp buttons not responding / crashing

**Root Cause:**
Multiple `VoiceOSDatabaseManager` instances create separate SQLite connections to the same database file. Even with WAL mode enabled, multiple connections cause lock contention because WAL state isn't properly shared.

**Example Error Log:**
```
JitElementCapture E Failed to persist element: 63062a6272a4
  android.database.sqlite.SQLiteDatabaseLockedException: database is locked (code 5 SQLITE_BUSY)
  at app.cash.sqldelight.driver.android.AndroidSqliteDriver.execute(AndroidSqliteDriver.kt:184)
  at com.augmentalis.database.ScrapedElementQueries.insert(ScrapedElementQueries.kt:1235)

FeatureFlagManager E Error checking dynamic scraping flag
  kotlinx.coroutines.TimeoutCancellationException: Timed out waiting for 10000 ms

VoiceOSService E  ✗ All tiers failed for command: 'four'
```

**Solution:**
`VoiceOSDatabaseManager` MUST be used as a singleton. Always use:

```kotlin
// CORRECT - Singleton instance
val databaseManager = VoiceOSDatabaseManager.getInstance(
    DatabaseDriverFactory(context)
)

// WRONG - Creates new connection (causes SQLITE_BUSY)
val databaseManager = VoiceOSDatabaseManager(
    DatabaseDriverFactory(context)
)
```

**Files Updated (2025-12-01):**
- `VoiceOSDatabaseManager.kt` - Added singleton pattern
- All adapter classes use `.getInstance()` method

**Why Singleton?**
1. Single SQLite connection shared across app
2. WAL mode properly shared
3. busy_timeout (30s) applies to all operations
4. No lock contention between components

---

### SQLITE_BUSY / Transaction Deadlock with 180-Second Timeouts

**Symptoms:**
- `database is locked (code 5 SQLITE_BUSY)` with 180+ second waits
- Multiple threads (11+) waiting for single connection
- Connection pool shows "0 active, 1 idle, 0 available"
- App completely frozen during database operations

**Example Error Log:**
```
15:07:58.335 SQLiteQuery E  exception: database is locked (code 5 SQLITE_BUSY); query: BEGIN IMMEDIATE;

15:09:27.973 SQLiteConnectionPool W  The connection pool for database 'voiceos.db'
has been unable to grant a connection to thread 81 (DefaultDispatcher-worker-1)
with flags 0x1 for 180.005 seconds.
Connections: 0 active, 1 idle, 0 available.
```

**Root Cause:**
DAO abstraction layer wrapped SQLDelight queries in `transaction { runBlocking { } }` pattern:

```kotlin
// DEADLOCK PATTERN (REMOVED)
dao.transaction {  // ← BEGIN IMMEDIATE (locks database)
    runBlocking(Dispatchers.Unconfined) {
        deleteNavigationGraph(packageName)  // ← suspend function
          ↓
        withContext(Dispatchers.IO) {
            databaseManager.queries.delete(...)  // ← Tries DB access
              ↓
            BLOCKED - waiting for transaction lock held by same thread!
              ↓
            180 seconds timeout → SQLITE_BUSY
        }
    }
}
```

**The Problem:**
1. `dao.transaction {}` starts `BEGIN IMMEDIATE` (exclusive lock)
2. Transaction uses `runBlocking(Dispatchers.Unconfined)` which blocks thread
3. Inside transaction, code calls suspend functions with `withContext(Dispatchers.IO)`
4. Suspend functions try to access database but transaction already holds lock
5. Same thread is waiting for itself → DEADLOCK
6. 11 worker threads pile up waiting 180 seconds → SQLITE_BUSY

**Solution:**
Remove ALL DAO abstractions and transaction wrappers. Use SQLDelight queries directly:

```kotlin
// BEFORE (DEADLOCK):
suspend fun deleteAppCompletely(packageName: String): RepositoryResult<Boolean> {
    val mutex = getMutexForPackage(packageName)
    return mutex.withLock {
        try {
            dao.transaction {  // ← START TRANSACTION (locks database)
                val app = getLearnedApp(packageName)  // ← suspend call
                deleteNavigationGraph(packageName)  // ← suspend call tries DB access → DEADLOCK
                deleteScreenStatesForPackage(packageName)
                deleteSessionsForPackage(packageName)
                deleteLearnedApp(app)
            }
            RepositoryResult.Success(true)
        } catch (e: Exception) {
            RepositoryResult.Failure(e.message ?: "Unknown error", e)
        }
    }
}

// AFTER (FIXED):
suspend fun deleteAppCompletely(packageName: String): RepositoryResult<Boolean> {
    val mutex = getMutexForPackage(packageName)
    return mutex.withLock {
        try {
            val app = getLearnedApp(packageName)
            if (app == null) {
                return@withLock RepositoryResult.Failure("App not found")
            }

            // NO transaction wrapper - each delete is atomic in SQLDelight
            deleteNavigationGraph(packageName)  // Direct SQLDelight query
            deleteScreenStatesForPackage(packageName)  // Direct SQLDelight query
            deleteSessionsForPackage(packageName)  // Direct SQLDelight query
            deleteLearnedApp(app)  // Direct SQLDelight query

            RepositoryResult.Success(true)
        } catch (e: Exception) {
            RepositoryResult.Failure(e.message ?: "Unknown error", e)
        }
    }
}
```

**Files Updated (2025-12-02):**
- `LearnAppRepository.kt` - Removed ALL DAO abstractions (32 references → 0)
- `LearnAppRepository.kt` - Removed ALL transaction wrappers (6 locations)
- `LearnAppIntegration.kt` - Constructor now takes `VoiceOSDatabaseManager` directly
- Architecture simplified: Repository → SQLDelight (removed DAO layer)

**Why This Fix Works:**
1. **No nested blocking**: Each SQLDelight query uses `withContext(Dispatchers.IO)` without outer `runBlocking`
2. **Atomic queries**: Each SQLDelight query is already atomic by default
3. **Mutex for safety**: Per-package mutex prevents race conditions in multi-query operations
4. **No transaction overhead**: Eliminates unnecessary transaction wrappers
5. **Simple architecture**: Repository → SQLDelight (2 layers instead of 4)

**Architecture Before:**
```
Repository → DAO Interface → DAO Adapter → SQLDelight Queries
             (abstraction)    (transaction wrapper)
```

**Architecture After:**
```
Repository → SQLDelight Queries
             (direct access)
```

**Performance Impact:**
- Before: 180+ second deadlocks, app frozen
- After: < 50ms writes, < 10ms reads, no blocking

---

## Database Configuration

### WAL Mode

**Location:** `DatabaseFactory.android.kt`

```kotlin
override fun onOpen(db: SupportSQLiteDatabase) {
    super.onOpen(db)
    // Set busy timeout to 30 seconds
    db.query("PRAGMA busy_timeout = 30000").close()

    // Enable WAL mode for concurrent read/write
    db.query("PRAGMA journal_mode = WAL").close()
}
```

**Benefits:**
- Readers don't block writers
- Writers don't block readers
- Better concurrent performance
- Reduced lock contention

**Limitations:**
- Only works properly with SINGLE connection
- Multiple connections → lock contention despite WAL

---

## Common Patterns

### Correct Usage

```kotlin
class MyAdapter(context: Context) {
    private val databaseManager: VoiceOSDatabaseManager by lazy {
        VoiceOSDatabaseManager.getInstance(
            DatabaseDriverFactory(context.applicationContext)
        )
    }

    fun doWork() {
        databaseManager.scrapedElements.insert(element)
    }
}
```

### Hilt/Dagger Injection

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides
    @Singleton
    fun provideDatabaseManager(
        @ApplicationContext context: Context
    ): VoiceOSDatabaseManager {
        return VoiceOSDatabaseManager.getInstance(
            DatabaseDriverFactory(context)
        )
    }
}
```

---

## Debugging Tips

### Check for Multiple Instances

Search codebase for direct constructor calls:
```bash
grep -r "VoiceOSDatabaseManager(" --include="*.kt" | grep -v ".getInstance"
```

Should return NO results except in VoiceOSDatabaseManager.kt itself.

### Monitor Database Locks

```kotlin
// Add logging in DatabaseDriverFactory callback
override fun onOpen(db: SupportSQLiteDatabase) {
    super.onOpen(db)
    Log.d("Database", "Connection opened: ${db.hashCode()}")
    // Should only see ONE unique hashCode during app lifetime
}
```

### Test Concurrent Access

```kotlin
@Test
fun testConcurrentDatabaseAccess() = runTest {
    val jobs = (1..100).map {
        launch {
            databaseManager.scrapedElements.insert(testElement)
        }
    }
    jobs.joinAll()
    // Should complete without SQLITE_BUSY errors
}
```

---

## Migration Notes

### From Room to SQLDelight

When migrating adapters:

**Before (Room - Multiple Connections):**
```kotlin
val database = Room.databaseBuilder(context, MyDatabase::class.java, "db.db").build()
val dao = database.myDao()
```

**After (SQLDelight - Singleton):**
```kotlin
val databaseManager = VoiceOSDatabaseManager.getInstance(
    DatabaseDriverFactory(context)
)
val repo = databaseManager.myRepository
```

**Key Difference:**
Room managed connection pooling internally. SQLDelight requires explicit singleton pattern to prevent multiple connections.

---

## Performance

### Expected Behavior

With singleton pattern:
- Write operations: < 50ms (WAL mode)
- Read operations: < 10ms (no blocking)
- Concurrent access: No SQLITE_BUSY errors
- Timeout errors: None under normal load

### Red Flags

- Any SQLITE_BUSY errors → Multiple instances
- Cascade timeouts → Database locked
- Slow writes (>100ms) → Lock contention

---

## Related Issues

- **Issue:** LearnApp button clicks not working (2025-12-01)
  - **Cause:** Multiple VoiceOSDatabaseManager instances
  - **Fix:** Singleton pattern implementation

- **Issue:** AccessibilityScrapingIntegration timeouts (2025-12-01)
  - **Cause:** Same as above
  - **Fix:** Same as above

- **Issue:** 180-second SQLITE_BUSY deadlock with frozen app (2025-12-02)
  - **Cause:** DAO transaction wrapper with runBlocking + suspend functions
  - **Fix:** Removed DAO layer, use SQLDelight directly
  - **Files:** LearnAppRepository.kt (912 lines → 836 lines, 0 dao references)

---

## See Also

- [DatabaseFactory.android.kt](src/androidMain/kotlin/com/augmentalis/database/DatabaseFactory.android.kt)
- [VoiceOSDatabaseManager.kt](src/commonMain/kotlin/com/augmentalis/database/VoiceOSDatabaseManager.kt)
- [SQLDelight Documentation](https://cashapp.github.io/sqldelight/)

---

**Version History:**
- 1.1.0 (2025-12-02): Added transaction deadlock troubleshooting section
- 1.0.0 (2025-12-01): Initial troubleshooting guide for SQLITE_BUSY singleton fix
