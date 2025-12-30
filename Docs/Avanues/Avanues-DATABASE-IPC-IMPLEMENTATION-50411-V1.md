# Database IPC Implementation Guide

**Version:** 1.0.0
**Created:** 2025-11-04 00:45 PST
**Author:** Manoj Jhawar, manoj@ideahq.net
**Status:** 🔴 Ready to Implement
**Priority:** CRITICAL - Week 1 of 8-week roadmap

---

## 📋 Executive Summary

This document provides the **complete implementation specification** for migrating the Avanues Database module to a Hybrid IPC Architecture. This is the first module in the 8-week optimization roadmap.

**Expected Benefits:**
- 💾 20 MB memory savings (freed from main process)
- 🔒 Crash isolation (database crashes don't affect UI)
- 🚀 Better performance isolation
- 🌐 Cross-app data sharing (AVA AI, AVAConnect, BrowserAvanue)

**Implementation Timeline:** Week 1 (5 days)

---

## 🎯 Current State Analysis

### Current Architecture

**Location:** `/Universal/IDEAMagic/Database/`

**Structure:**
```
Universal/IDEAMagic/Database/
├── src/
│   ├── commonMain/kotlin/
│   │   └── com/augmentalis/avamagic/database/
│   │       ├── AppDatabase.kt (Room database)
│   │       ├── dao/
│   │       │   ├── UserDao.kt
│   │       │   ├── VoiceCommandDao.kt
│   │       │   └── SettingsDao.kt
│   │       └── entities/
│   │           ├── UserEntity.kt
│   │           ├── VoiceCommandEntity.kt
│   │           └── SettingsEntity.kt
│   └── androidMain/kotlin/
└── build.gradle.kts
```

**Current Usage Pattern:**
```kotlin
// Direct in-process access (current)
val database = AppDatabase.getInstance(context)
val users = database.userDao().getAllUsers()
```

**Problems:**
- ❌ 20 MB always loaded in main process
- ❌ Database queries can block UI thread if not careful
- ❌ Database corruption crashes entire app
- ❌ Cannot share data with companion apps (AVA AI, AVAConnect)

---

## 🏗️ Target Architecture

### New Hybrid IPC Architecture

```
┌─────────────────────────────────────────────────────────────┐
│               Avanues Main Process                      │
│  ┌──────────────┐         ┌──────────────────────────────┐ │
│  │  UI Layer    │  AIDL   │  Database Process (:db)      │ │
│  │  (Activities)│────────►│  ┌────────────────────────┐  │ │
│  └──────────────┘         │  │  DatabaseService       │  │ │
│         ▲                 │  │  (AIDL Binder)         │  │ │
│         │ (uses)          │  └────────────────────────┘  │ │
│  ┌──────┴───────┐         │  ┌────────────────────────┐  │ │
│  │DatabaseClient│         │  │  Room Database         │  │ │
│  │(Coroutine API)         │  │  - UserDao             │  │ │
│  └──────────────┘         │  │  - VoiceCommandDao     │  │ │
│         ▲                 │  │  - SettingsDao         │  │ │
│         │                 │  └────────────────────────┘  │ │
│  ┌──────┴───────┐         └──────────────────────────────┘ │
│  │ContentProvider│                                           │
│  │(Public API)  │                                           │
│  └──────────────┘                                           │
│         ▲                                                    │
└─────────┼────────────────────────────────────────────────────┘
          │ ContentProvider queries
          │ (Standard Android API)
┌─────────▼─────────┐     ┌──────────────┐     ┌──────────────┐
│  AVA AI           │     │ AVAConnect   │     │BrowserAvanue │
└───────────────────┘     └──────────────┘     └──────────────┘
```

**New Usage Pattern:**
```kotlin
// New IPC-based access
val client = DatabaseClient.getInstance(context)
client.connect()
val users = client.getAllUsers() // Suspend function
```

---

## 📦 Implementation Components

### 1. AIDL Interface

**File:** `src/main/aidl/com/augmentalis/avanues/IDatabase.aidl`

**Operations to Support:**

**User Operations (6 methods):**
- `getAllUsers()`
- `getUserById(int userId)`
- `insertUser(in User user)`
- `updateUser(in User user)`
- `deleteUser(int userId)`
- `getUserCount()`

**Voice Command Operations (6 methods):**
- `getAllVoiceCommands()`
- `getVoiceCommandById(int commandId)`
- `getVoiceCommandsByCategory(String category)`
- `insertVoiceCommand(in VoiceCommand command)`
- `updateVoiceCommand(in VoiceCommand command)`
- `deleteVoiceCommand(int commandId)`

**Settings Operations (4 methods):**
- `getSettings()`
- `updateSettings(in AppSettings settings)`
- `getSettingValue(String key)`
- `setSettingValue(String key, String value)`

**Maintenance Operations (4 methods):**
- `clearAllData()`
- `getDatabaseSize()`
- `vacuum()`
- `getDatabaseVersion()`

**Health Check (2 methods):**
- `isHealthy()`
- `getLastAccessTime()`

**Total:** 22 AIDL methods

### 2. Parcelable Data Models

**File:** `src/main/kotlin/com/augmentalis/avanues/models/`

**Models to Create:**

**User.kt:**
```kotlin
@Parcelize
data class User(
    val id: Int,
    val name: String,
    val email: String,
    val createdAt: Long,
    val lastLoginAt: Long?
) : Parcelable
```

**VoiceCommand.kt:**
```kotlin
@Parcelize
data class VoiceCommand(
    val id: Int,
    val command: String,
    val action: String,
    val category: String,
    val enabled: Boolean,
    val usageCount: Int
) : Parcelable
```

**AppSettings.kt:**
```kotlin
@Parcelize
data class AppSettings(
    val id: Int,
    val voiceEnabled: Boolean,
    val theme: String,
    val language: String,
    val notificationsEnabled: Boolean
) : Parcelable
```

**Corresponding AIDL Declarations:**
- `User.aidl`
- `VoiceCommand.aidl`
- `AppSettings.aidl`

### 3. Service Implementation

**File:** `src/main/kotlin/com/augmentalis/avanues/service/DatabaseService.kt`

**Key Features:**
- Runs in separate process (`:database`)
- Implements all 22 AIDL methods
- Uses Room database internally
- Idle timeout monitoring (5 minutes)
- Health check implementation
- Proper error handling and logging

**Process Name:** `com.augmentalis.avanues:database`

### 4. Client Wrapper

**File:** `src/main/kotlin/com/augmentalis/avanues/client/DatabaseClient.kt`

**Key Features:**
- Singleton pattern
- Coroutine-based API (all methods suspend)
- Connection lifecycle management
- Automatic reconnection on failure
- Health check support

**Usage Pattern:**
```kotlin
val client = DatabaseClient.getInstance(context)
client.connect()

// All operations are suspend functions
val users = client.getAllUsers()
val user = client.getUserById(1)
client.insertUser(newUser)

client.disconnect()
```

### 5. ContentProvider Bridge

**File:** `src/main/kotlin/com/augmentalis/avanues/provider/DatabaseContentProvider.kt`

**Authority:** `com.augmentalis.avanues.database`

**URIs:**
```
content://com.augmentalis.avanues.database/users
content://com.augmentalis.avanues.database/users/{id}
content://com.augmentalis.avanues.database/commands
content://com.augmentalis.avanues.database/commands/{id}
content://com.augmentalis.avanues.database/settings
```

**External App Usage (e.g., AVA AI):**
```kotlin
val cursor = contentResolver.query(
    Uri.parse("content://com.augmentalis.avanues.database/users"),
    null, null, null, null
)

cursor?.use {
    while (it.moveToNext()) {
        val name = it.getString(it.getColumnIndex("name"))
        // Use data...
    }
}
```

### 6. Manifest Configuration

**Avanues Manifest:**
```xml
<!-- Custom permissions -->
<permission
    android:name="com.augmentalis.avanues.permission.BIND_DATABASE_SERVICE"
    android:protectionLevel="signature" />

<permission
    android:name="com.augmentalis.avanues.permission.ACCESS_DATABASE_DATA"
    android:protectionLevel="signature" />

<!-- Database Service (Internal IPC) -->
<service
    android:name=".service.DatabaseService"
    android:process=":database"
    android:exported="false"
    android:enabled="true" />

<!-- Database ContentProvider (Cross-App) -->
<provider
    android:name=".provider.DatabaseContentProvider"
    android:authorities="com.augmentalis.avanues.database"
    android:exported="true"
    android:readPermission="com.augmentalis.avanues.permission.ACCESS_DATABASE_DATA"
    android:writePermission="com.augmentalis.avanues.permission.ACCESS_DATABASE_DATA" />
```

**Companion App Manifests (AVA AI, AVAConnect, BrowserAvanue):**
```xml
<uses-permission
    android:name="com.augmentalis.avanues.permission.ACCESS_DATABASE_DATA" />
```

---

## 🗂️ File Structure (After Implementation)

```
Universal/IDEAMagic/Database/
├── src/
│   ├── main/
│   │   ├── aidl/com/augmentalis/avanues/
│   │   │   ├── IDatabase.aidl (NEW)
│   │   │   └── models/
│   │   │       ├── User.aidl (NEW)
│   │   │       ├── VoiceCommand.aidl (NEW)
│   │   │       └── AppSettings.aidl (NEW)
│   │   └── kotlin/com/augmentalis/avanues/
│   │       ├── models/ (NEW)
│   │       │   ├── User.kt
│   │       │   ├── VoiceCommand.kt
│   │       │   ├── AppSettings.kt
│   │       │   └── ModelMappers.kt
│   │       ├── service/ (NEW)
│   │       │   └── DatabaseService.kt
│   │       ├── client/ (NEW)
│   │       │   └── DatabaseClient.kt
│   │       ├── provider/ (NEW)
│   │       │   └── DatabaseContentProvider.kt
│   │       └── database/ (EXISTING)
│   │           ├── AppDatabase.kt
│   │           ├── dao/
│   │           └── entities/
│   ├── test/
│   │   └── kotlin/
│   │       ├── DatabaseServiceTest.kt (NEW)
│   │       ├── DatabaseClientTest.kt (NEW)
│   │       └── DatabaseContentProviderTest.kt (NEW)
│   └── androidTest/
│       └── kotlin/
│           └── DatabaseIPCIntegrationTest.kt (NEW)
└── build.gradle.kts (UPDATE - enable AIDL)
```

---

## 📝 Implementation Phases

### Phase 1: AIDL Interface Design (Day 1 - 3 hours)

**Tasks:**
1. Create AIDL directory structure
2. Define `IDatabase.aidl` with all 22 methods
3. Create Parcelable data models (User, VoiceCommand, AppSettings)
4. Create corresponding AIDL parcelable declarations
5. Update build.gradle.kts to enable AIDL compilation
6. Verify AIDL compiles successfully

**Deliverables:**
- `IDatabase.aidl` (complete interface)
- 3 Parcelable models
- 3 AIDL parcelable declarations
- Build compiles without errors

**Success Criteria:**
- ✅ AIDL generates stub classes in build/generated/
- ✅ All data models are Parcelable
- ✅ Build succeeds with zero errors

---

### Phase 2: Data Model Mapping (Day 1 - 2 hours)

**Tasks:**
1. Create `ModelMappers.kt` with extension functions
2. Implement `User.toEntity()` and `UserEntity.toParcelable()`
3. Implement `VoiceCommand.toEntity()` and `VoiceCommandEntity.toParcelable()`
4. Implement `AppSettings.toEntity()` and `SettingsEntity.toParcelable()`
5. Add list conversion helpers
6. Write unit tests for mappers

**Deliverables:**
- `ModelMappers.kt` (all mapping functions)
- `ModelMappersTest.kt` (unit tests)

**Success Criteria:**
- ✅ All mappings preserve data integrity
- ✅ All tests pass
- ✅ No data loss in conversions

---

### Phase 3: Service Implementation (Day 2 - 5 hours)

**Tasks:**
1. Create `DatabaseService.kt` class
2. Implement all 22 AIDL methods
3. Add error handling for each method
4. Implement idle timeout monitoring
5. Add health check implementation
6. Add comprehensive logging
7. Write unit tests for service

**Deliverables:**
- `DatabaseService.kt` (complete implementation)
- `DatabaseServiceTest.kt` (unit tests)

**Success Criteria:**
- ✅ All AIDL methods implemented
- ✅ Error handling for all edge cases
- ✅ Health check returns true
- ✅ All unit tests pass

---

### Phase 4: Client Wrapper (Day 3 - 4 hours)

**Tasks:**
1. Create `DatabaseClient.kt` class
2. Implement connection lifecycle (connect/disconnect)
3. Wrap all 22 methods as suspend functions
4. Add automatic reconnection logic
5. Implement health check methods
6. Add comprehensive KDoc documentation
7. Write unit tests for client

**Deliverables:**
- `DatabaseClient.kt` (complete wrapper)
- `DatabaseClientTest.kt` (unit tests)

**Success Criteria:**
- ✅ Singleton pattern working
- ✅ Connection lifecycle managed
- ✅ All methods wrapped as suspend functions
- ✅ All unit tests pass

---

### Phase 5: ContentProvider Bridge (Day 3-4 - 5 hours)

**Tasks:**
1. Create `DatabaseContentProvider.kt` class
2. Implement URI matcher for all tables
3. Implement query() for users, commands, settings
4. Implement insert() for all tables
5. Implement update() for all tables
6. Implement delete() for all tables
7. Add change notifications
8. Write unit tests for provider

**Deliverables:**
- `DatabaseContentProvider.kt` (complete provider)
- `DatabaseContentProviderTest.kt` (unit tests)

**Success Criteria:**
- ✅ All CRUD operations work
- ✅ URIs resolve correctly
- ✅ Change notifications sent
- ✅ All unit tests pass

---

### Phase 6: Manifest Configuration (Day 4 - 1 hour)

**Tasks:**
1. Add custom permission definitions
2. Add permission strings to strings.xml
3. Declare DatabaseService with process isolation
4. Declare DatabaseContentProvider with authority
5. Add permissions to companion app manifests (AVA AI, AVAConnect)

**Deliverables:**
- Updated `AndroidManifest.xml` (Avanues)
- Updated `AndroidManifest.xml` (AVA AI)
- Updated `AndroidManifest.xml` (AVAConnect)
- Updated `strings.xml`

**Success Criteria:**
- ✅ Service starts in separate process
- ✅ ContentProvider accessible from AVA AI
- ✅ Permissions enforced correctly

---

### Phase 7: Integration & Migration (Day 5 - 6 hours)

**Tasks:**
1. Add feature flag `USE_IPC_DATABASE = false`
2. Create adapter interface for both implementations
3. Update one ViewModel to use DatabaseClient (Settings)
4. Test thoroughly in Settings module
5. Enable feature flag: `USE_IPC_DATABASE = true`
6. Monitor for issues
7. Write integration tests
8. Document lessons learned

**Deliverables:**
- Feature flag implementation
- Updated Settings ViewModel
- `DatabaseIPCIntegrationTest.kt`
- Migration documentation

**Success Criteria:**
- ✅ Settings module works via IPC
- ✅ No regressions in functionality
- ✅ Memory reduced by ~20 MB
- ✅ All integration tests pass

---

## 🧪 Testing Strategy

### Unit Tests (20 tests minimum)

**DatabaseServiceTest.kt:**
- Test all CRUD operations
- Test error handling
- Test health check
- Test idle timeout logic

**DatabaseClientTest.kt:**
- Test connection lifecycle
- Test all API methods
- Test reconnection logic
- Test error handling

**DatabaseContentProviderTest.kt:**
- Test all URIs
- Test query/insert/update/delete
- Test change notifications

### Integration Tests (5 tests minimum)

**DatabaseIPCIntegrationTest.kt:**
- Test end-to-end user flow
- Test cross-process communication
- Test service crash recovery
- Test concurrent access
- Test memory usage

### Performance Tests (3 tests minimum)

**DatabasePerformanceTest.kt:**
- Measure IPC latency (target: <50ms)
- Measure memory usage (target: 20MB savings)
- Measure throughput (target: >100 ops/sec)

---

## 📊 Success Metrics

### Performance Metrics

**Before Implementation:**
- Main process memory: ~225 MB
- Database always loaded: 20 MB
- Cold start time: ~2.5 seconds

**After Implementation (Expected):**
- Main process memory: ~205 MB (-20 MB) ✅
- Database process: 20 MB (separate)
- Cold start time: ~2.2 seconds (-300ms) ✅
- IPC latency: <50ms average ✅

### Quality Metrics

**Test Coverage:**
- Unit tests: >80% coverage
- Integration tests: All critical paths
- Performance tests: All key operations

**Reliability:**
- Zero crashes in 7-day monitoring
- Zero data loss
- Zero permission errors

---

## 🚨 Risk Assessment

### High Risk Items

**1. Data Migration Issues**
- **Risk:** Existing data incompatible with new models
- **Mitigation:** Create migration scripts, test with production data snapshots
- **Rollback:** Feature flag to disable IPC

**2. Performance Degradation**
- **Risk:** IPC overhead makes operations slower
- **Mitigation:** Implement caching, batch operations, measure latency
- **Rollback:** Disable feature flag

**3. Permission Problems**
- **Risk:** Companion apps can't access ContentProvider
- **Mitigation:** Test with all apps before release
- **Rollback:** N/A (doesn't affect main app)

### Medium Risk Items

**1. Service Crashes**
- **Risk:** Service crashes frequently under load
- **Mitigation:** Comprehensive error handling, crash reporting
- **Rollback:** Disable feature flag

**2. Memory Leaks**
- **Risk:** Service process leaks memory over time
- **Mitigation:** Profile with Memory Profiler, implement idle timeout
- **Rollback:** Disable feature flag

---

## 🔄 Rollback Plan

### If Issues Arise

**Step 1: Immediate Action**
```kotlin
// Set feature flag to false
object DatabaseConfig {
    const val USE_IPC = false // Rollback to direct access
}
```

**Step 2: Verify Rollback**
- Test all database operations work with direct access
- Monitor for stability

**Step 3: Investigate & Fix**
- Analyze logs for root cause
- Fix issues in separate branch
- Re-test thoroughly

**Step 4: Re-Enable**
- Set `USE_IPC = true` after fix verified
- Monitor closely for 48 hours

---

## 📚 Documentation Updates

### Developer Manual Updates

**Section to Update:** "Database Architecture"

**New Content:**
```markdown
## Database Architecture

### IPC-Based Architecture (Current)

Avanues uses a Hybrid IPC architecture for database access:

**Internal Access (Avanues app):**
- Use `DatabaseClient` for all database operations
- All methods are suspend functions
- Automatic connection management

**External Access (AVA AI, AVAConnect, BrowserAvanue):**
- Use ContentProvider with URIs:
  - `content://com.augmentalis.avanues.database/users`
  - `content://com.augmentalis.avanues.database/commands`
  - `content://com.augmentalis.avanues.database/settings`

**Example Usage:**
```kotlin
// Internal (Avanues)
val client = DatabaseClient.getInstance(context)
client.connect()
val users = client.getAllUsers()
client.disconnect()

// External (AVA AI)
val cursor = contentResolver.query(
    Uri.parse("content://com.augmentalis.avanues.database/users"),
    null, null, null, null
)
```

**Benefits:**
- 20 MB memory savings (process isolation)
- Crash isolation (database crashes don't affect UI)
- Cross-app data sharing
```

---

## 🎓 Lessons Learned (To Be Updated Post-Implementation)

This section will be updated after implementation with:
- Actual implementation time vs. estimates
- Issues encountered and solutions
- Performance metrics achieved
- Recommendations for next module (SpeechRecognition)

---

## 📅 Implementation Schedule

**Week 1 Schedule:**

| Day | Phase | Tasks | Hours | Status |
|-----|-------|-------|-------|--------|
| Mon | 1 | AIDL Interface + Data Models | 5h | ⏳ Pending |
| Tue | 2-3 | Mapping + Service Implementation | 7h | ⏳ Pending |
| Wed | 4 | Client Wrapper | 4h | ⏳ Pending |
| Thu | 5-6 | ContentProvider + Manifest | 6h | ⏳ Pending |
| Fri | 7 | Integration + Testing | 6h | ⏳ Pending |

**Total Estimated Time:** 28 hours (5.6 hours/day)

---

## 🔗 Related Documents

- **Protocol:** `/Volumes/M-Drive/Coding/ideacode/protocols/Protocol-Hybrid-IPC-Architecture.md`
- **Analysis:** `/Volumes/M-Drive/Coding/Avanues/docs/DEPENDENCY-ANALYSIS-251104.md`
- **Session Context:** `/Volumes/M-Drive/Coding/Avanues/.ideacode/context/session-251104-0019-phase3-tests-complete.md`

---

## ✅ Pre-Implementation Checklist

- [ ] Protocol-Hybrid-IPC-Architecture.md reviewed
- [ ] Current database structure analyzed
- [ ] Development environment ready
- [ ] Git branch created: `feature/database-ipc`
- [ ] Test device/emulator ready
- [ ] Backup of current database taken
- [ ] Team notified of implementation start

---

**Status:** 🔴 READY TO IMPLEMENT
**Next Step:** Create specification using IDEACODE MCP
**Assigned To:** AI Agent (IDEACODE workflow)

**Created by:** Manoj Jhawar, manoj@ideahq.net
**Avanues Project** | **IDEAMagic System** ✨💡
