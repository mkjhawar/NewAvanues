# Database IPC Architecture - Developer Manual

**Version**: 1.0.0
**Last Updated**: 2025-11-04
**Author**: Manoj Jhawar, manoj@ideahq.net
**Status**: Production Ready (Pending Beta Deployment)

---

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Getting Started](#getting-started)
4. [Usage Guide](#usage-guide)
5. [API Reference](#api-reference)
6. [ContentProvider Reference](#contentprovider-reference)
7. [Migration Guide](#migration-guide)
8. [Testing](#testing)
9. [Performance](#performance)
10. [Troubleshooting](#troubleshooting)
11. [Best Practices](#best-practices)
12. [FAQ](#faq)

---

## Overview

### What is Database IPC Architecture?

The Database IPC (Inter-Process Communication) Architecture is a hybrid system that provides:

1. **Internal IPC Layer** (AIDL) - Fast, process-isolated database access for in-app components
2. **External Sharing Layer** (ContentProvider) - Secure cross-app data sharing with AVA ecosystem apps

### Key Benefits

✅ **Process Isolation** - Database runs in separate `:database` process (crashes don't affect main app)
✅ **Memory Optimization** - Expected 20 MB freed from main process
✅ **Crash Protection** - Database failures are isolated and recoverable
✅ **Auto-Reconnect** - Transparent recovery from service crashes
✅ **Cross-App Sharing** - AVA AI, AVAConnect, BrowserAvanue can access data securely
✅ **Gradual Migration** - Feature flag enables safe rollout (off by default)

### Architecture Components

```
┌─────────────────────────────────────────────────────────────┐
│                        Main Process                          │
│  ┌────────────────────────────────────────────────────────┐ │
│  │          DatabaseAccessFactory                         │ │
│  │  (Feature flag: USE_IPC_DATABASE = false/true)        │ │
│  └─────────────┬──────────────────────┬───────────────────┘ │
│                │                      │                      │
│                ▼                      ▼                      │
│  ┌─────────────────────┐  ┌─────────────────────────────┐  │
│  │ DatabaseDirectAdapter│  │  DatabaseClientAdapter      │  │
│  │  (Legacy/Direct)     │  │  (IPC via AIDL)             │  │
│  └──────────────────────┘  └─────────┬───────────────────┘  │
│                                      │                       │
│                                      │ Binder IPC            │
└──────────────────────────────────────┼───────────────────────┘
                                       │
┌──────────────────────────────────────┼───────────────────────┐
│                    :database Process  │                       │
│                                      ▼                       │
│  ┌─────────────────────────────────────────────────────┐    │
│  │            DatabaseService (AIDL)                   │    │
│  │  - 22 Database Operations                           │    │
│  │  - Collection Management (users, commands, settings)│    │
│  │  - Health Monitoring                                │    │
│  └──────────────────────┬──────────────────────────────┘    │
│                         │                                    │
│                         ▼                                    │
│  ┌─────────────────────────────────────────────────────┐    │
│  │     Database (Collection-based Document Storage)    │    │
│  │  - Collections: users, voice_commands, settings     │    │
│  │  - Documents: Map<String, String>                   │    │
│  └─────────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────────┘

External Apps (AVA AI, AVAConnect, BrowserAvanue)
       │
       │ ContentResolver
       ▼
┌─────────────────────────────────────────┐
│   DatabaseContentProvider               │
│  URI: content://...database/users       │
│       content://...database/commands    │
│       content://...database/settings    │
└─────────────────┬───────────────────────┘
                  │
                  ▼
          DatabaseClient (AIDL)
```

---

## Architecture

### Hybrid IPC Design

The architecture uses TWO communication layers:

#### 1. Internal Layer (AIDL)
**Purpose**: Fast IPC for in-app components
**Technology**: Android AIDL (Android Interface Definition Language)
**Process**: Main app → `:database` process via Binder IPC
**Latency**: <50ms per operation (target)
**Usage**: Primary method for internal database access

#### 2. External Layer (ContentProvider)
**Purpose**: Cross-app data sharing
**Technology**: Android ContentProvider
**Security**: Signature-level permissions (same certificate only)
**Clients**: AVA AI, AVAConnect, BrowserAvanue
**Usage**: External apps query via ContentResolver

### Process Isolation

The database service runs in a **separate process** (`:database`):

**AndroidManifest.xml**:
```xml
<service
    android:name="com.augmentalis.avanues.service.DatabaseService"
    android:process=":database"
    android:exported="false"
    android:permission="com.augmentalis.avanues.permission.BIND_DATABASE_SERVICE" />
```

**Benefits**:
- Main app memory: 20 MB lighter
- Database crashes don't crash app
- Process can be killed when idle (5 min timeout)
- Clear resource boundaries

### Collection-Based Storage

The database uses a **document-oriented storage model**:

**Collections**:
1. `users` - User profiles and authentication
2. `voice_commands` - Voice command definitions
3. `settings` - App configuration and preferences

**Documents**: `Map<String, String>` key-value pairs
- All fields stored as strings
- Type conversion on read/write (Int, Long, Boolean → String)

**Example**:
```kotlin
// User document
Document(
    id = "1",
    data = mapOf(
        "id" to "1",
        "name" to "Alice",
        "email" to "alice@example.com",
        "createdAt" to "1699123456789",
        "lastLoginAt" to "1699234567890"
    )
)
```

### Feature Flag Migration

**Configuration**: `DatabaseConfig.kt`

```kotlin
object DatabaseConfig {
    const val USE_IPC_DATABASE = false  // Safe default: direct access
}
```

**Migration Stages**:
1. **Development** - Test with `createIpc()` explicitly
2. **Beta** - Enable for 10% of users
3. **Staged Rollout** - 10% → 25% → 50% → 100%
4. **Full Migration** - Remove legacy DatabaseDirectAdapter

---

## Getting Started

### Quick Start (5 minutes)

#### 1. Add Dependency

The Database module is already included in the project:

```gradle
implementation(project(":Universal:IDEAMagic:Database"))
```

#### 2. Initialize Database Access

```kotlin
import com.augmentalis.avanues.access.DatabaseAccessFactory
import kotlinx.coroutines.runBlocking

class MyActivity : AppCompatActivity() {
    private lateinit var database: DatabaseAccess

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create database instance (respects feature flag)
        database = DatabaseAccessFactory.create(this)

        // Connect to database
        lifecycleScope.launch {
            val connected = database.connect()
            if (connected) {
                Log.d("DB", "Connected successfully")
            } else {
                Log.e("DB", "Connection failed")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        database.disconnect()
    }
}
```

#### 3. Perform Database Operations

```kotlin
lifecycleScope.launch {
    // Insert a user
    val user = User(
        id = 1,
        name = "Alice",
        email = "alice@example.com",
        createdAt = System.currentTimeMillis(),
        lastLoginAt = null
    )
    database.insertUser(user)

    // Retrieve the user
    val retrievedUser = database.getUserById(1)
    Log.d("DB", "User: ${retrievedUser?.name}")

    // Get all users
    val allUsers = database.getAllUsers()
    Log.d("DB", "Total users: ${allUsers.size}")
}
```

---

## Usage Guide

### Choosing an Implementation

The `DatabaseAccessFactory` provides three methods:

#### 1. Automatic Selection (Recommended)

```kotlin
val database = DatabaseAccessFactory.create(context)
```

Selects implementation based on `DatabaseConfig.USE_IPC_DATABASE`:
- `false` → DatabaseDirectAdapter (legacy, direct access)
- `true` → DatabaseClientAdapter (IPC, process-isolated)

#### 2. Explicit IPC (Testing/Development)

```kotlin
val database = DatabaseAccessFactory.createIpc(context)
```

Always uses IPC implementation, ignoring feature flag.

**Use cases**:
- Testing IPC layer before full rollout
- Development/debugging IPC issues
- Performance comparison

#### 3. Explicit Direct (Legacy/Fallback)

```kotlin
val database = DatabaseAccessFactory.createDirect(context)
```

Always uses direct implementation, ignoring feature flag.

**Use cases**:
- Performance comparison
- Fallback during migration issues
- Legacy code compatibility

### Connection Management

All database operations require an active connection:

```kotlin
// Connect
val connected = database.connect()
if (!connected) {
    // Handle connection failure
    return
}

// Check connection status
if (database.isConnected()) {
    // Perform operations
}

// Disconnect when done
database.disconnect()
```

**Best practices**:
- Connect in `onCreate()` or when database is needed
- Disconnect in `onDestroy()` or when done
- Check `isConnected()` before critical operations
- Handle connection failures gracefully

### Error Handling

All database operations return **safe defaults** on error:

```kotlin
// Returns empty list on error (never null)
val users = database.getAllUsers()

// Returns null on error (not found or exception)
val user = database.getUserById(999)

// Returns 0 on error
val count = database.getUserCount()

// Void operations log errors but don't throw
database.insertUser(user)  // Logs error if fails
```

**Exception handling**:
- Operations catch `RemoteException` internally
- Auto-reconnect attempted on IPC failures
- Errors logged with tag "DatabaseClient" or "DatabaseService"

---

## API Reference

### DatabaseAccess Interface

All 22 database operations are accessible through the `DatabaseAccess` interface:

#### Connection Management

```kotlin
suspend fun connect(): Boolean
```
Connects to database service. Returns `true` on success.

```kotlin
fun disconnect()
```
Disconnects from database service and releases resources.

```kotlin
fun isConnected(): Boolean
```
Returns `true` if currently connected to database service.

---

### User Operations (6 methods)

#### getAllUsers()
```kotlin
suspend fun getAllUsers(): List<User>
```
Retrieves all users from the database.

**Returns**: List of users (empty list if none exist or error occurs)

**Example**:
```kotlin
val users = database.getAllUsers()
users.forEach { user ->
    println("${user.name} (${user.email})")
}
```

---

#### getUserById()
```kotlin
suspend fun getUserById(userId: Int): User?
```
Retrieves a specific user by ID.

**Parameters**:
- `userId` - Unique user identifier

**Returns**: User object or `null` if not found

**Example**:
```kotlin
val user = database.getUserById(1)
if (user != null) {
    println("Found user: ${user.name}")
} else {
    println("User not found")
}
```

---

#### insertUser()
```kotlin
suspend fun insertUser(user: User)
```
Inserts a new user into the database.

**Parameters**:
- `user` - User object to insert

**Example**:
```kotlin
val newUser = User(
    id = 1,
    name = "Bob",
    email = "bob@example.com",
    createdAt = System.currentTimeMillis(),
    lastLoginAt = null
)
database.insertUser(newUser)
```

---

#### updateUser()
```kotlin
suspend fun updateUser(user: User)
```
Updates an existing user.

**Parameters**:
- `user` - User object with updated fields (matched by ID)

**Example**:
```kotlin
val existingUser = database.getUserById(1)
val updated = existingUser?.copy(
    name = "Robert",
    lastLoginAt = System.currentTimeMillis()
)
if (updated != null) {
    database.updateUser(updated)
}
```

---

#### deleteUser()
```kotlin
suspend fun deleteUser(userId: Int)
```
Deletes a user by ID.

**Parameters**:
- `userId` - ID of user to delete

**Example**:
```kotlin
database.deleteUser(1)
```

---

#### getUserCount()
```kotlin
suspend fun getUserCount(): Int
```
Returns the total number of users.

**Returns**: User count (0 if none exist or error occurs)

**Example**:
```kotlin
val count = database.getUserCount()
println("Total users: $count")
```

---

### Voice Command Operations (6 methods)

#### getAllVoiceCommands()
```kotlin
suspend fun getAllVoiceCommands(): List<VoiceCommand>
```
Retrieves all voice commands.

**Returns**: List of voice commands (empty if none exist)

**Example**:
```kotlin
val commands = database.getAllVoiceCommands()
commands.filter { it.enabled }.forEach { cmd ->
    println("${cmd.command} → ${cmd.action}")
}
```

---

#### getVoiceCommandById()
```kotlin
suspend fun getVoiceCommandById(commandId: Int): VoiceCommand?
```
Retrieves a specific voice command by ID.

**Parameters**:
- `commandId` - Unique command identifier

**Returns**: VoiceCommand object or `null` if not found

---

#### getVoiceCommandsByCategory()
```kotlin
suspend fun getVoiceCommandsByCategory(category: String): List<VoiceCommand>
```
Retrieves voice commands filtered by category.

**Parameters**:
- `category` - Category name (e.g., "navigation", "media", "communication")

**Returns**: List of commands in specified category

**Example**:
```kotlin
val navigationCommands = database.getVoiceCommandsByCategory("navigation")
println("Navigation commands: ${navigationCommands.size}")
```

---

#### insertVoiceCommand()
```kotlin
suspend fun insertVoiceCommand(command: VoiceCommand)
```
Inserts a new voice command.

**Parameters**:
- `command` - VoiceCommand object to insert

**Example**:
```kotlin
val command = VoiceCommand(
    id = 1,
    command = "open browser",
    action = "ACTION_OPEN_BROWSER",
    category = "navigation",
    enabled = true,
    usageCount = 0
)
database.insertVoiceCommand(command)
```

---

#### updateVoiceCommand()
```kotlin
suspend fun updateVoiceCommand(command: VoiceCommand)
```
Updates an existing voice command.

**Parameters**:
- `command` - VoiceCommand object with updated fields

**Example**:
```kotlin
val cmd = database.getVoiceCommandById(1)
val updated = cmd?.copy(usageCount = cmd.usageCount + 1)
if (updated != null) {
    database.updateVoiceCommand(updated)
}
```

---

#### deleteVoiceCommand()
```kotlin
suspend fun deleteVoiceCommand(commandId: Int)
```
Deletes a voice command by ID.

**Parameters**:
- `commandId` - ID of command to delete

---

### Settings Operations (4 methods)

#### getSettings()
```kotlin
suspend fun getSettings(): AppSettings?
```
Retrieves app settings.

**Returns**: AppSettings object or default settings if none exist

**Example**:
```kotlin
val settings = database.getSettings()
if (settings != null) {
    println("Theme: ${settings.theme}")
    println("Language: ${settings.language}")
}
```

---

#### updateSettings()
```kotlin
suspend fun updateSettings(settings: AppSettings)
```
Updates app settings.

**Parameters**:
- `settings` - AppSettings object with updated values

**Example**:
```kotlin
val settings = database.getSettings()
val updated = settings?.copy(
    theme = "dark",
    notificationsEnabled = false
)
if (updated != null) {
    database.updateSettings(updated)
}
```

---

#### getSettingValue()
```kotlin
suspend fun getSettingValue(key: String): String?
```
Retrieves a specific setting value by key.

**Parameters**:
- `key` - Setting key name

**Returns**: Setting value as string, or `null` if not found

**Example**:
```kotlin
val theme = database.getSettingValue("theme")
println("Current theme: $theme")
```

---

#### setSettingValue()
```kotlin
suspend fun setSettingValue(key: String, value: String)
```
Updates a specific setting value.

**Parameters**:
- `key` - Setting key name
- `value` - New value as string

**Example**:
```kotlin
database.setSettingValue("theme", "dark")
database.setSettingValue("language", "es")
```

---

### Maintenance Operations (4 methods)

#### clearAllData()
```kotlin
suspend fun clearAllData()
```
Clears all data from all collections. **Use with caution!**

**Example**:
```kotlin
// Confirm before clearing
if (userConfirmed) {
    database.clearAllData()
    println("All data cleared")
}
```

---

#### getDatabaseSize()
```kotlin
suspend fun getDatabaseSize(): Long
```
Returns the database file size in bytes.

**Returns**: Size in bytes (0 on error)

**Example**:
```kotlin
val sizeBytes = database.getDatabaseSize()
val sizeMB = sizeBytes / (1024.0 * 1024.0)
println("Database size: %.2f MB".format(sizeMB))
```

---

#### vacuum()
```kotlin
suspend fun vacuum()
```
Flushes pending database operations to disk.

**Example**:
```kotlin
database.vacuum()  // Ensure all writes are persisted
```

---

#### getDatabaseVersion()
```kotlin
suspend fun getDatabaseVersion(): String?
```
Returns the database version string.

**Returns**: Version string (e.g., "1.0.0") or `null` on error

---

### Health & Utility (2 methods)

#### isHealthy()
```kotlin
suspend fun isHealthy(): Boolean
```
Checks if the database service is healthy (all collections exist).

**Returns**: `true` if healthy, `false` otherwise

**Example**:
```kotlin
if (!database.isHealthy()) {
    Log.w("DB", "Database service is unhealthy")
    // Attempt reconnection
    database.disconnect()
    database.connect()
}
```

---

#### getLastAccessTime()
```kotlin
suspend fun getLastAccessTime(): Long
```
Returns the timestamp (milliseconds since epoch) of the last database access.

**Returns**: Timestamp in milliseconds

**Example**:
```kotlin
val lastAccess = database.getLastAccessTime()
val idleTime = System.currentTimeMillis() - lastAccess
println("Idle for ${idleTime / 1000} seconds")
```

---

## ContentProvider Reference

### Overview

External apps (AVA AI, AVAConnect, BrowserAvanue) can access database data via `DatabaseContentProvider`.

**Authority**: `com.augmentalis.avanues.database`

**Permissions Required**:
```xml
<uses-permission android:name="com.augmentalis.avanues.permission.ACCESS_DATABASE_DATA" />
```

### URI Patterns

#### Users
```
content://com.augmentalis.avanues.database/users          # All users
content://com.augmentalis.avanues.database/users/1        # User with ID=1
```

#### Voice Commands
```
content://com.augmentalis.avanues.database/commands       # All commands
content://com.augmentalis.avanues.database/commands/1     # Command with ID=1
```

#### Settings
```
content://com.augmentalis.avanues.database/settings       # App settings
```

### MIME Types

```kotlin
// Collection MIME types
"vnd.android.cursor.dir/vnd.avanues.user"
"vnd.android.cursor.dir/vnd.avanues.command"
"vnd.android.cursor.dir/vnd.avanues.settings"

// Item MIME types
"vnd.android.cursor.item/vnd.avanues.user"
"vnd.android.cursor.item/vnd.avanues.command"
"vnd.android.cursor.item/vnd.avanues.settings"
```

### Query Examples

#### Query All Users

```kotlin
val uri = Uri.parse("content://com.augmentalis.avanues.database/users")
val cursor = contentResolver.query(uri, null, null, null, null)

cursor?.use {
    val idCol = it.getColumnIndex("id")
    val nameCol = it.getColumnIndex("name")
    val emailCol = it.getColumnIndex("email")

    while (it.moveToNext()) {
        val id = it.getInt(idCol)
        val name = it.getString(nameCol)
        val email = it.getString(emailCol)
        println("User: $name ($email)")
    }
}
```

#### Query Specific User

```kotlin
val uri = Uri.parse("content://com.augmentalis.avanues.database/users/1")
val cursor = contentResolver.query(uri, null, null, null, null)

cursor?.use {
    if (it.moveToFirst()) {
        val name = it.getString(it.getColumnIndex("name"))
        println("User name: $name")
    }
}
```

#### Query Commands by Category

```kotlin
val uri = Uri.parse("content://com.augmentalis.avanues.database/commands")
val selection = "category = ?"
val selectionArgs = arrayOf("navigation")
val cursor = contentResolver.query(uri, null, selection, selectionArgs, null)

cursor?.use {
    while (it.moveToNext()) {
        val command = it.getString(it.getColumnIndex("command"))
        println("Command: $command")
    }
}
```

### Insert Examples

#### Insert User

```kotlin
val uri = Uri.parse("content://com.augmentalis.avanues.database/users")
val values = ContentValues().apply {
    put("id", 1)
    put("name", "Alice")
    put("email", "alice@example.com")
    put("createdAt", System.currentTimeMillis())
}

val resultUri = contentResolver.insert(uri, values)
println("Inserted user: $resultUri")
```

#### Insert Voice Command

```kotlin
val uri = Uri.parse("content://com.augmentalis.avanues.database/commands")
val values = ContentValues().apply {
    put("id", 1)
    put("command", "open browser")
    put("action", "ACTION_OPEN_BROWSER")
    put("category", "navigation")
    put("enabled", 1)  // Boolean: 1=true, 0=false
    put("usageCount", 0)
}

val resultUri = contentResolver.insert(uri, values)
```

### Update Examples

#### Update User

```kotlin
val uri = Uri.parse("content://com.augmentalis.avanues.database/users/1")
val values = ContentValues().apply {
    put("name", "Alice Updated")
    put("lastLoginAt", System.currentTimeMillis())
}

val rowsUpdated = contentResolver.update(uri, values, null, null)
println("Updated $rowsUpdated rows")
```

#### Update Settings

```kotlin
val uri = Uri.parse("content://com.augmentalis.avanues.database/settings")
val values = ContentValues().apply {
    put("theme", "dark")
    put("language", "es")
}

val rowsUpdated = contentResolver.update(uri, values, null, null)
```

### Delete Examples

#### Delete User

```kotlin
val uri = Uri.parse("content://com.augmentalis.avanues.database/users/1")
val rowsDeleted = contentResolver.delete(uri, null, null)
println("Deleted $rowsDeleted rows")
```

#### Delete Command

```kotlin
val uri = Uri.parse("content://com.augmentalis.avanues.database/commands/5")
val rowsDeleted = contentResolver.delete(uri, null, null)
```

### Change Notifications

Register a ContentObserver to receive change notifications:

```kotlin
val uri = Uri.parse("content://com.augmentalis.avanues.database/users")

val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
    override fun onChange(selfChange: Boolean) {
        println("Users data changed, refresh UI")
        // Reload data
    }
}

contentResolver.registerContentObserver(uri, true, observer)

// Unregister when done
contentResolver.unregisterContentObserver(observer)
```

---

## Migration Guide

### Phase 1: Development Testing (Current)

**Status**: `USE_IPC_DATABASE = false` (direct access)

**Goal**: Test IPC implementation without affecting production

**Steps**:
1. Use `DatabaseAccessFactory.createIpc()` explicitly in test code
2. Run end-to-end tests
3. Measure IPC latency (<50ms target)
4. Verify process isolation works
5. Test auto-reconnect after service crash

**Example**:
```kotlin
// In development/test builds only
val database = if (BuildConfig.DEBUG) {
    DatabaseAccessFactory.createIpc(context)  // Test IPC
} else {
    DatabaseAccessFactory.create(context)     // Production (direct)
}
```

### Phase 2: Beta Deployment (Week 1)

**Status**: Enable for internal beta testers

**Steps**:
1. Set `USE_IPC_DATABASE = true` for beta builds
2. Deploy to internal testers (10-20 users)
3. Monitor for 48 hours:
   - Crash reports (Firebase Crashlytics)
   - ANR reports
   - IPC latency metrics
   - Memory usage metrics
4. Collect user feedback

**Metrics to Monitor**:
- Crash rate (should not increase)
- ANR rate (should not increase)
- Average IPC latency (<50ms)
- Memory usage (main process should decrease ~20 MB)

### Phase 3: Staged Rollout (Weeks 2-4)

**Canary Deployment**:
```kotlin
// Gradual rollout based on user ID
object DatabaseConfig {
    fun shouldUseIPC(userId: Int): Boolean {
        // Week 2: 10% of users
        val rolloutPercent = 10
        return (userId % 100) < rolloutPercent
    }
}

// Usage
val useIPC = DatabaseConfig.shouldUseIPC(currentUser.id)
val database = if (useIPC) {
    DatabaseAccessFactory.createIpc(context)
} else {
    DatabaseAccessFactory.createDirect(context)
}
```

**Rollout Schedule**:
- **Week 2**: 10% rollout, monitor for 48 hours
- **Week 3**: 25% rollout if stable, monitor for 48 hours
- **Week 3.5**: 50% rollout if stable, monitor for 48 hours
- **Week 4**: 100% rollout if stable

**Success Criteria** (each stage):
- No crash rate increase (>2%)
- No ANR rate increase (>1%)
- IPC latency <50ms (95th percentile)
- Memory savings visible (~20 MB)

### Phase 4: Full Migration (Week 5+)

**Enable for All Users**:
```kotlin
object DatabaseConfig {
    const val USE_IPC_DATABASE = true  // Enable for everyone
}
```

**Monitor for 2 Weeks**:
- Daily crash reports
- ANR reports
- Performance metrics
- User feedback

**If Issues Arise**:
1. **Minor Issues**: Fix and deploy patch
2. **Major Issues**: Rollback by setting `USE_IPC_DATABASE = false`
3. **Critical Issues**: Immediate rollback + hotfix

### Phase 5: Cleanup (Week 7+)

Once IPC is stable for 2 weeks:

**Remove Legacy Code**:
1. Delete `DatabaseDirectAdapter.kt`
2. Remove feature flag checks
3. Simplify `DatabaseAccessFactory` to always return IPC adapter
4. Update documentation

---

## Testing

### Running Tests

#### All Tests
```bash
# All unit tests
./gradlew :Universal:IDEAMagic:Database:testDebugUnitTest

# All instrumented tests (requires device)
./gradlew :Universal:IDEAMagic:Database:connectedDebugAndroidTest

# Both
./gradlew :Universal:IDEAMagic:Database:test \
          :Universal:IDEAMagic:Database:connectedDebugAndroidTest
```

#### Specific Test Suites
```bash
# End-to-end integration tests
./gradlew :Universal:IDEAMagic:Database:connectedDebugAndroidTest \
  --tests "DatabaseServiceEndToEndTest"

# DatabaseService unit tests (46 tests)
./gradlew :Universal:IDEAMagic:Database:testDebugUnitTest \
  --tests "DatabaseServiceTest"

# DatabaseClient unit tests (51 tests)
./gradlew :Universal:IDEAMagic:Database:testDebugUnitTest \
  --tests "DatabaseClientTest"

# DatabaseContentProvider unit tests (42 tests)
./gradlew :Universal:IDEAMagic:Database:testDebugUnitTest \
  --tests "DatabaseContentProviderTest"

# DatabaseAccessFactory unit tests (15 tests - 100% passing)
./gradlew :Universal:IDEAMagic:Database:testDebugUnitTest \
  --tests "DatabaseAccessFactoryTest"
```

### Test Coverage

**Total Test Cases**: 154

| Test Suite | Test Cases | Coverage |
|------------|------------|----------|
| DatabaseServiceEndToEndTest | 28 | All 22 operations + scenarios |
| DatabaseServiceTest | 46 | Service + helpers + error handling |
| DatabaseClientTest | 51 | Connection + all operations + errors |
| DatabaseContentProviderTest | 42 | URI + CRUD + notifications |
| DatabaseAccessFactory Test | 15 | Feature flag + factory logic |

### Coverage Report
```bash
# Generate coverage report
./gradlew :Universal:IDEAMagic:Database:testDebugUnitTestCoverage

# Open report
open Universal/IDEAMagic/Database/build/reports/coverage/test/debug/index.html
```

---

## Performance

### Expected Metrics

| Metric | Target | Measured |
|--------|--------|----------|
| IPC Latency (average) | <30ms | TBD |
| IPC Latency (95th percentile) | <50ms | TBD |
| Memory Savings (main process) | ~20 MB | TBD |
| Service Startup Time | <200ms | TBD |
| Database Operations Throughput | 100+ ops/sec | TBD |

### Measuring Performance

#### IPC Latency

```kotlin
val startTime = System.currentTimeMillis()
val users = database.getAllUsers()
val latency = System.currentTimeMillis() - startTime
Log.d("Perf", "getAllUsers latency: ${latency}ms")
```

#### Memory Usage

Use Android Profiler or adb:

```bash
# Main process memory
adb shell dumpsys meminfo com.augmentalis.avanues

# Database process memory
adb shell dumpsys meminfo com.augmentalis.avanues:database

# Compare total memory before/after enabling IPC
```

#### Bulk Operation Performance

```kotlin
val startTime = System.currentTimeMillis()

// Insert 100 users
repeat(100) { i ->
    database.insertUser(User(i, "User $i", "user$i@example.com", System.currentTimeMillis(), null))
}

val elapsed = System.currentTimeMillis() - startTime
val avgPerOp = elapsed / 100.0
Log.d("Perf", "100 inserts: ${elapsed}ms (avg ${avgPerOp}ms per insert)")
```

### Performance Tips

1. **Batch Operations**: Group multiple operations when possible
2. **Connection Reuse**: Keep connection open for multiple operations
3. **Avoid Main Thread**: Use coroutines for database operations
4. **Monitor Health**: Check `isHealthy()` periodically
5. **Vacuum Periodically**: Call `vacuum()` after bulk operations

---

## Troubleshooting

### Connection Issues

#### Problem: `connect()` returns `false`

**Possible Causes**:
1. Service not declared in AndroidManifest.xml
2. Permission not granted
3. Service process crashed
4. Binding timeout (5 seconds)

**Solutions**:
```kotlin
// Check service declaration
// Ensure AndroidManifest.xml has:
<service android:name="...DatabaseService" android:process=":database" />

// Check permissions
<uses-permission android:name="...BIND_DATABASE_SERVICE" />

// Increase timeout if needed
// Edit DatabaseConfig.IPC_CONNECT_TIMEOUT_MS

// Check logs
adb logcat | grep "DatabaseClient\|DatabaseService"
```

---

### RemoteException Issues

#### Problem: `RemoteException` in logs

**Possible Causes**:
1. Database process crashed
2. IPC buffer overflow (data too large)
3. Service killed by system (low memory)

**Solutions**:
```kotlin
// Auto-reconnect is automatic, but you can force it:
database.disconnect()
database.connect()

// Check service status
if (!database.isHealthy()) {
    // Service is unhealthy, reconnect
}

// Reduce data size (pagination)
// Instead of: database.getAllUsers() (could be 1000s)
// Use ContentProvider with LIMIT or implement pagination
```

---

### Performance Issues

#### Problem: Operations taking >50ms

**Possible Causes**:
1. Database process under load
2. Main thread blocking (use coroutines!)
3. Large dataset being transferred
4. Slow device storage

**Solutions**:
```kotlin
// Use coroutines, not blocking calls
lifecycleScope.launch(Dispatchers.IO) {  // Use IO dispatcher
    val users = database.getAllUsers()
    // Process results
}

// Pagination for large datasets
// Use ContentProvider with LIMIT:
val uri = Uri.parse("content://.../users?limit=50&offset=0")

// Vacuum periodically to optimize database
database.vacuum()

// Monitor latency
val start = System.currentTimeMillis()
val result = database.getUserById(1)
Log.d("Perf", "Latency: ${System.currentTimeMillis() - start}ms")
```

---

### Memory Issues

#### Problem: Memory not being freed

**Possible Causes**:
1. Service not shutting down (5 min idle timeout)
2. Connection leak (not disconnecting)
3. Large result sets cached in client

**Solutions**:
```kotlin
// Always disconnect when done
override fun onDestroy() {
    database.disconnect()  // Critical!
    super.onDestroy()
}

// Check if service is running
adb shell ps | grep database
// Should see: com.augmentalis.avanues:database

// Force stop service (development only)
adb shell am force-stop com.augmentalis.avanues:database

// Adjust idle timeout if needed
// Edit DatabaseConfig.SERVICE_IDLE_TIMEOUT_MS
```

---

### ContentProvider Issues

#### Problem: External app can't access data

**Possible Causes**:
1. Permission not requested
2. App not signed with same certificate
3. ContentProvider not exported correctly

**Solutions**:
```xml
<!-- External app's AndroidManifest.xml -->
<uses-permission android:name="com.augmentalis.avanues.permission.ACCESS_DATABASE_DATA" />

<!-- Check signature match -->
<!-- Both apps must be signed with the same certificate for signature-level permissions -->

<!-- Avanues's AndroidManifest.xml should have -->
<provider
    android:name="...DatabaseContentProvider"
    android:exported="true"
    android:readPermission="...ACCESS_DATABASE_DATA"
    android:writePermission="...ACCESS_DATABASE_DATA" />
```

---

## Best Practices

### Connection Management

✅ **DO**:
- Connect in `onCreate()` or when database is first needed
- Disconnect in `onDestroy()` to release resources
- Reuse connections for multiple operations
- Check `isConnected()` before critical operations

❌ **DON'T**:
- Connect/disconnect for every operation (high overhead)
- Keep connections alive unnecessarily
- Forget to disconnect (memory leak)

---

### Error Handling

✅ **DO**:
- Check return values (`null`, empty list, `false`)
- Log errors for debugging
- Provide user feedback on failures
- Have fallback behavior

❌ **DON'T**:
- Assume operations always succeed
- Crash on database errors
- Ignore connection failures

---

### Performance

✅ **DO**:
- Use coroutines (Dispatchers.IO) for database operations
- Batch operations when possible
- Call `vacuum()` after bulk inserts/deletes
- Monitor `isHealthy()` periodically

❌ **DON'T**:
- Perform database operations on main thread
- Transfer huge datasets via IPC (use pagination)
- Ignore performance metrics

---

### Threading

✅ **DO**:
```kotlin
// Correct: Use coroutines
lifecycleScope.launch(Dispatchers.IO) {
    val users = database.getAllUsers()
    withContext(Dispatchers.Main) {
        // Update UI
    }
}
```

❌ **DON'T**:
```kotlin
// Wrong: Blocking main thread
runBlocking {  // Blocks UI thread!
    val users = database.getAllUsers()
}
```

---

## FAQ

### Q: Should I use IPC or direct access?

**A**: Use `DatabaseAccessFactory.create(context)` which respects the feature flag. This allows gradual migration and easy rollback.

---

### Q: What happens if the database process crashes?

**A**: The IPC client automatically attempts to reconnect (up to 3 attempts with exponential backoff). Your app won't crash, but operations may return safe defaults during reconnection.

---

### Q: How do I migrate existing code?

**A**:
1. Replace direct database calls with `DatabaseAccessFactory.create(context)`
2. Wrap operations in coroutines (`lifecycleScope.launch`)
3. Handle connection lifecycle (`connect()` / `disconnect()`)
4. Test thoroughly before enabling IPC

---

### Q: Can I use both IPC and direct access simultaneously?

**A**: Yes! Create two instances:
```kotlin
val ipcDb = DatabaseAccessFactory.createIpc(context)
val directDb = DatabaseAccessFactory.createDirect(context)
// Use for A/B testing or performance comparison
```

---

### Q: How do I enable IPC for a specific user?

**A**:
```kotlin
object DatabaseConfig {
    fun shouldUseIPC(userId: Int): Boolean {
        // Gradual rollout based on user ID
        return (userId % 100) < 25  // 25% rollout
    }
}

val database = if (DatabaseConfig.shouldUseIPC(userId)) {
    DatabaseAccessFactory.createIpc(context)
} else {
    DatabaseAccessFactory.createDirect(context)
}
```

---

### Q: What's the overhead of IPC?

**A**: Target latency is <50ms per operation. For most use cases, this is acceptable. Bulk operations (100+ items) should use pagination.

---

### Q: How do I debug IPC issues?

**A**:
```bash
# Watch real-time logs
adb logcat | grep "DatabaseClient\|DatabaseService"

# Check if service is running
adb shell ps | grep database

# Check binder transactions
adb shell dumpsys activity services | grep Database

# Monitor memory
adb shell dumpsys meminfo com.augmentalis.avanues:database
```

---

### Q: Can external apps write to the database?

**A**: Yes, if they have `ACCESS_DATABASE_DATA` permission and are signed with the same certificate. Use the ContentProvider URIs for CRUD operations.

---

### Q: How do I rollback if IPC causes issues?

**A**:
1. Set `USE_IPC_DATABASE = false` in `DatabaseConfig.kt`
2. Deploy hotfix update immediately
3. Users will automatically switch back to direct access
4. No data loss (both use the same underlying database)

---

## Changelog

### v1.0.0 (2025-11-04)
- Initial release
- 22 database operations via AIDL
- ContentProvider for external apps
- Feature flag migration strategy
- Comprehensive test coverage (154 tests)
- Process isolation with `:database` process
- Auto-reconnect on IPC failures
- Health monitoring

---

## References

- **Source Code**: `/Universal/IDEAMagic/Database/`
- **Tests**: `/Universal/IDEAMagic/Database/src/test/` and `/androidTest/`
- **Architecture Doc**: `Protocol-Hybrid-IPC-Architecture.md`
- **Migration Doc**: `Protocol-Module-IPC-Migration-Master.md`
- **Status Updates**: `/docs/Status-Database-*-*.md`

---

**End of Developer Manual**

For questions or support, contact: manoj@ideahq.net
