# AI-Assisted Module Porting Guide

**Version**: 1.0.0
**Date**: 2025-11-06
**Purpose**: Guide AI assistants through systematic module porting to Avanues

---

## Overview

This document provides a **comprehensive checklist and methodology** for AI assistants to follow when porting modules from other projects (AVA AI, AVAConnect, BrowserAvanue) to the Avanues unified architecture.

**Target AI**: Claude Code, GitHub Copilot, or any AI assistant helping with codebase migrations

---

## Prerequisites - READ FIRST

### 1. Required Context Documents

Before porting ANY module, AI must read these documents:

✅ **Architecture Documents**:
- `/docs/IPC-Module-Plugin-Data-Exchange-Flow.md` - IPC communication patterns
- `/docs/IDEAMAGIC-UI-DEVELOPER-MANUAL-251105.md` - IDEAMagic framework reference
- `/CLAUDE.md` - Project instructions and IDEACODE framework

✅ **Active Status Documents**:
- `/docs/Active/Status-IDEAMagic-Phase*.md` - Current phase completion status
- `/docs/Active/PHASE-*-PLAN-*.md` - Phase implementation plans

### 2. Understand the Target Architecture

**Avanues Architecture**:
```
Avanues (Main Process)
├── :database (Separate Process via AIDL)
├── Internal Modules (Same Process)
│   ├── VoiceRecognition
│   ├── VoiceAccessibility
│   ├── VoiceKeyboard
│   └── DeviceManager
└── External Plugins (Separate Apps via ContentProvider)
    ├── AVA AI
    ├── AVAConnect
    ├── BrowserAvanue
    └── NoteAvanue
```

**IPC Methods**:
- **AIDL** (30-50ms): Internal modules → Database
- **ContentProvider** (60-100ms): External plugins → Database

---

## Phase 1: Module Discovery & Analysis

### Step 1: Identify the Module

**Questions to Answer**:
1. What is the module name?
2. Where is it currently located? (project, path)
3. What is its primary function?
4. Is it Android-specific or cross-platform?

**Document in**:
```markdown
## Module: [NAME]
- **Current Location**: `/path/to/module`
- **Project**: AVA AI / AVAConnect / BrowserAvanue / VoiceOS4
- **Type**: Internal Module / External Plugin / Library
- **Platform**: Android / iOS / KMP
- **Primary Function**: [Brief description]
```

### Step 2: Analyze Dependencies

**Create Dependency Map**:
```kotlin
// Example: VoiceRecognition module
Dependencies:
├── Android SDK
│   ├── android.speech.SpeechRecognizer
│   └── android.media.AudioRecord
├── Google Services
│   └── com.google.android.gms:play-services-speech
├── Internal
│   ├── Database (via IPC)
│   ├── Preferences
│   └── Logging
└── Third-Party
    └── kotlinx-coroutines-android
```

**AI Action**:
```bash
# Analyze build.gradle.kts dependencies
grep -A 20 "dependencies {" [module]/build.gradle.kts > dependencies.txt

# Find internal imports
grep -r "import com\." [module]/src/ | sort | uniq > imports.txt

# Identify database calls
grep -r "database\|Database\|DatabaseClient" [module]/src/ > db-calls.txt
```

### Step 3: Map Database Operations

**Identify ALL Database Operations**:
```kotlin
// Find these patterns:
database.insert(...)
database.update(...)
database.delete(...)
database.query(...)
database.getCollection(...)
```

**Create Database Operation Inventory**:
```markdown
## Database Operations

### Read Operations
- [ ] Get user preferences
- [ ] Query recognition history
- [ ] Load voice profiles

### Write Operations
- [ ] Save recognition result
- [ ] Update user settings
- [ ] Insert voice command

### Collections Used
- `voice_commands` (Collection<VoiceCommand>)
- `user_preferences` (Collection<Preference>)
- `recognition_history` (Collection<RecognitionEvent>)
```

### Step 4: Identify UI Components

**Check for UI Elements**:
```bash
# Find layout files
find [module] -name "*.xml" -path "*/res/layout/*"

# Find Compose files
grep -r "@Composable" [module]/src/

# Find Activities/Fragments
grep -r "Activity\|Fragment" [module]/src/ | grep "class "
```

**Document UI Structure**:
```markdown
## UI Components

### Activities
- `MainActivity.kt` - Main voice input screen

### Fragments
- `SettingsFragment.kt` - Voice recognition settings

### Composables
- `VoiceInputButton()` - Recording trigger
- `RecognitionResultCard()` - Display results

### Layouts (XML)
- `activity_main.xml`
- `fragment_settings.xml`
```

### Step 5: Check for Background Services

**Identify Services**:
```bash
# Find Service classes
grep -r "Service" [module]/src/ | grep "class " | grep -v "//\|/\*"

# Find BroadcastReceivers
grep -r "BroadcastReceiver" [module]/src/ | grep "class "

# Find WorkManager workers
grep -r "Worker" [module]/src/ | grep "class "
```

**Document Background Tasks**:
```markdown
## Background Components

### Services
- `VoiceRecognitionService` - Continuous listening
- `AudioProcessingService` - Audio preprocessing

### BroadcastReceivers
- `VoiceCommandReceiver` - Receives voice triggers

### WorkManager
- `VoiceDataSyncWorker` - Sync voice data
```

---

## Phase 2: Pre-Port Preparation

### Step 1: Create Module Documentation

**File**: `/docs/modules/[MODULE_NAME]-porting-plan.md`

```markdown
# [Module Name] Porting Plan

## Overview
- **Module**: [Name]
- **Source Project**: [AVA AI / AVAConnect / etc.]
- **Target Location**: `Universal/[Module]` or `android/avanues/[module]`
- **Porting Date**: [Date]
- **AI Assistant**: Claude Code / [Assistant Name]

## Module Analysis

### Dependencies
[List from Phase 1 Step 2]

### Database Operations
[List from Phase 1 Step 3]

### UI Components
[List from Phase 1 Step 4]

### Background Services
[List from Phase 1 Step 5]

## IPC Migration Plan

### Database Access Changes

**Before (Direct Access)**:
```kotlin
val database = Database.getInstance()
val users = database.getCollection<User>("users")
users.insert(newUser)
```

**After (IPC via AIDL)**:
```kotlin
val databaseClient = DatabaseClient.getInstance(context)
val users = databaseClient.getCollection<User>("users")
users.insert(newUser)
```

### Required Changes
- [ ] Replace `Database` with `DatabaseClient`
- [ ] Add IPC error handling
- [ ] Update coroutine scopes for async IPC
- [ ] Add connection lifecycle management

## File Migration Map

### Source → Target
- `src/old/VoiceRecognition.kt` → `Universal/VoiceRecognition/src/.../VoiceRecognition.kt`
- `src/old/VoiceSettings.kt` → `Universal/VoiceRecognition/src/.../VoiceSettings.kt`

## Testing Strategy
- [ ] Unit tests for business logic
- [ ] IPC integration tests
- [ ] UI instrumented tests
- [ ] Performance benchmarks (IPC latency)

## Rollout Plan
1. Create new module structure
2. Migrate core logic
3. Update database calls to IPC
4. Migrate UI components
5. Add tests
6. Integration testing
7. Documentation
```

### Step 2: Prepare Target Module Structure

**AI Action**:
```bash
# Create module directory
mkdir -p "Universal/IDEAMagic/[ModuleName]/src/commonMain/kotlin/com/augmentalis/[modulename]"

# Create build.gradle.kts
cat > "Universal/IDEAMagic/[ModuleName]/build.gradle.kts" << 'EOF'
plugins {
    kotlin("multiplatform")
    id("com.android.library")
}

kotlin {
    androidTarget()

    sourceSets {
        val commonMain by getting {
            dependencies {
                // Add dependencies here
            }
        }
    }
}
EOF
```

### Step 3: Create Migration Checklist

**File**: `/docs/modules/[MODULE_NAME]-checklist.md`

```markdown
# [Module Name] Migration Checklist

## Pre-Migration
- [ ] Read architecture documents
- [ ] Analyze dependencies
- [ ] Map database operations
- [ ] Identify UI components
- [ ] Document background services
- [ ] Create porting plan

## Module Setup
- [ ] Create directory structure
- [ ] Create build.gradle.kts
- [ ] Add to settings.gradle.kts
- [ ] Create package structure

## Code Migration
- [ ] Migrate data models
- [ ] Migrate business logic
- [ ] Migrate database operations → IPC
- [ ] Migrate UI components
- [ ] Migrate background services
- [ ] Migrate tests

## IPC Integration
- [ ] Replace Database with DatabaseClient
- [ ] Add AIDL bindings
- [ ] Add connection lifecycle
- [ ] Add error handling
- [ ] Add retry logic
- [ ] Add timeout handling

## Testing
- [ ] Unit tests pass
- [ ] IPC tests pass
- [ ] UI tests pass
- [ ] Performance tests pass
- [ ] Integration tests pass

## Documentation
- [ ] Update module README
- [ ] Document IPC calls
- [ ] Update architecture diagrams
- [ ] Add API documentation

## Post-Migration
- [ ] Code review
- [ ] Performance benchmarking
- [ ] Commit changes
- [ ] Update status documents
```

---

## Phase 3: Code Migration

### Step 1: Migrate Data Models

**Pattern**:
```kotlin
// Source (old)
data class User(
    val id: Int,
    val name: String,
    val email: String
)

// Target (new) - Add Parcelable for IPC
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class User(
    val id: Int,
    val name: String,
    val email: String
) : Parcelable
```

**AI Action**:
1. Copy data class
2. Add `@Parcelize` annotation
3. Extend `Parcelable`
4. Add import statements

### Step 2: Migrate Database Operations

**Before (Direct Access)**:
```kotlin
class UserRepository(private val database: Database) {
    fun getUser(id: Int): User? {
        val users = database.getCollection<User>("users")
        return users.find { it.id == id }
    }

    fun saveUser(user: User) {
        val users = database.getCollection<User>("users")
        users.insert(user)
    }
}
```

**After (IPC Access)**:
```kotlin
class UserRepository(private val context: Context) {
    private val databaseClient = DatabaseClient.getInstance(context)

    suspend fun getUser(id: Int): User? = withContext(Dispatchers.IO) {
        try {
            val users = databaseClient.getCollection<User>("users")
            users.find { it.id == id }
        } catch (e: RemoteException) {
            Log.e(TAG, "IPC error getting user", e)
            null
        }
    }

    suspend fun saveUser(user: User): Boolean = withContext(Dispatchers.IO) {
        try {
            val users = databaseClient.getCollection<User>("users")
            users.insert(user)
            true
        } catch (e: RemoteException) {
            Log.e(TAG, "IPC error saving user", e)
            false
        }
    }

    companion object {
        private const val TAG = "UserRepository"
    }
}
```

**Required Changes**:
1. Replace `Database` with `DatabaseClient.getInstance(context)`
2. Wrap IPC calls in `try-catch RemoteException`
3. Make functions `suspend` for async IPC
4. Use `withContext(Dispatchers.IO)` for background threads
5. Add logging for IPC errors
6. Return `Boolean` for success/failure

### Step 3: Update Dependency Injection

**Before**:
```kotlin
class VoiceRecognition(private val database: Database) {
    // ...
}
```

**After**:
```kotlin
class VoiceRecognition(private val context: Context) {
    private val databaseClient = DatabaseClient.getInstance(context)
    // ...
}
```

**AI Action**:
1. Replace `Database` constructor parameter with `Context`
2. Initialize `DatabaseClient` as private property
3. Update all call sites to pass `context`

### Step 4: Add IPC Lifecycle Management

**Pattern**:
```kotlin
class VoiceRecognitionService : Service() {
    private lateinit var databaseClient: DatabaseClient

    override fun onCreate() {
        super.onCreate()
        databaseClient = DatabaseClient.getInstance(this)
        databaseClient.bind()  // Connect to database service
    }

    override fun onDestroy() {
        databaseClient.unbind()  // Disconnect from database service
        super.onDestroy()
    }
}
```

**Required for**:
- Activities (onCreate/onDestroy)
- Services (onCreate/onDestroy)
- Fragments (onAttach/onDetach)

---

## Phase 4: Testing & Validation

### Step 1: Unit Tests

**Test Pattern**:
```kotlin
class UserRepositoryTest {
    private lateinit var context: Context
    private lateinit var repository: UserRepository

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        repository = UserRepository(context)
    }

    @Test
    fun testSaveAndRetrieveUser() = runBlocking {
        val user = User(1, "Test", "test@example.com")

        val saved = repository.saveUser(user)
        assertTrue(saved)

        val retrieved = repository.getUser(1)
        assertEquals(user, retrieved)
    }

    @Test
    fun testIPCError() = runBlocking {
        // Simulate IPC failure
        // Verify graceful error handling
    }
}
```

### Step 2: IPC Performance Tests

**Benchmark Pattern**:
```kotlin
@Test
fun benchmarkIPCLatency() = runBlocking {
    val iterations = 100
    val times = mutableListOf<Long>()

    repeat(iterations) {
        val start = System.nanoTime()
        repository.getUser(1)
        val end = System.nanoTime()
        times.add((end - start) / 1_000_000)  // ms
    }

    val avg = times.average()
    println("Average IPC latency: ${avg}ms")
    assertTrue(avg < 50)  // Should be < 50ms for AIDL
}
```

### Step 3: Integration Tests

**Test IPC Flow**:
```kotlin
@Test
fun testFullIPCFlow() = runBlocking {
    // 1. Save data via IPC
    val user = User(1, "Test", "test@example.com")
    repository.saveUser(user)

    // 2. Verify in database process
    val retrieved = repository.getUser(1)
    assertEquals(user, retrieved)

    // 3. Update via IPC
    val updated = user.copy(name = "Updated")
    repository.updateUser(updated)

    // 4. Verify update
    val final = repository.getUser(1)
    assertEquals("Updated", final?.name)
}
```

---

## Phase 5: Documentation

### Step 1: Update Module README

**File**: `Universal/[Module]/README.md`

```markdown
# [Module Name]

**Version**: 1.0.0
**Platform**: KMP (Android, iOS, Desktop)
**IPC**: AIDL (Internal Module)

## Overview
[Brief description]

## Architecture

### IPC Communication
This module communicates with the database via **AIDL** (30-50ms latency).

```
[Module] → DatabaseClient → AIDL → DatabaseService → Database
```

## Dependencies
- `DatabaseClient` (IPC)
- `kotlinx-coroutines-android`
- [Other dependencies]

## Usage

```kotlin
val module = ModuleName(context)
module.doSomething()
```

## IPC Operations
- `getUser(id)` - 35ms avg latency
- `saveUser(user)` - 40ms avg latency
- `updateUser(user)` - 38ms avg latency

## Testing
```bash
./gradlew :Universal:[Module]:test
```

## Performance
- IPC latency: 30-50ms (AIDL)
- Memory: [X]MB
- CPU: [Y]%
```

### Step 2: Update Architecture Diagrams

**Add to** `/docs/IPC-Module-Plugin-Data-Exchange-Flow.md`:

```markdown
### [Module Name] IPC Flow

```
[Module] (Client)
    ↓
    1. Call DatabaseClient.getUser(id)
    ↓
DatabaseClient (AIDL Proxy)
    ↓
    2. Binder IPC call
    ↓
DatabaseService (AIDL Stub)
    ↓
    3. Execute database.getCollection("users").find(id)
    ↓
    4. Return User object
    ↓
DatabaseClient
    ↓
    5. Return to [Module]
```

**Latency**: 30-50ms (AIDL)
```
```

### Step 3: Document API Changes

**File**: `/docs/modules/[MODULE]-api-changes.md`

```markdown
# [Module Name] API Changes

## Database Access Changes

### Before (Direct Access)
```kotlin
val database = Database.getInstance()
val users = database.getCollection<User>("users")
users.insert(user)
```

### After (IPC Access)
```kotlin
val databaseClient = DatabaseClient.getInstance(context)
val users = databaseClient.getCollection<User>("users")
users.insert(user)  // Now uses AIDL IPC
```

## Breaking Changes
- ❌ `Database.getInstance()` - Use `DatabaseClient.getInstance(context)`
- ❌ Synchronous calls - All database operations now `suspend` functions
- ✅ Add `try-catch RemoteException` for IPC errors

## Migration Guide
[Step-by-step guide for existing code]
```

---

## AI Assistant Workflow Checklist

### Before Starting
- [ ] Read `/docs/IPC-Module-Plugin-Data-Exchange-Flow.md`
- [ ] Read `/docs/IDEAMAGIC-UI-DEVELOPER-MANUAL-251105.md`
- [ ] Read `/CLAUDE.md`
- [ ] Understand Avanues architecture
- [ ] Identify module to port

### Phase 1: Analysis
- [ ] Analyze dependencies
- [ ] Map database operations
- [ ] Identify UI components
- [ ] Document background services
- [ ] Create porting plan document

### Phase 2: Preparation
- [ ] Create target module structure
- [ ] Create build.gradle.kts
- [ ] Add to settings.gradle.kts
- [ ] Create migration checklist

### Phase 3: Migration
- [ ] Migrate data models (add @Parcelize)
- [ ] Migrate business logic
- [ ] Replace Database with DatabaseClient
- [ ] Add IPC error handling
- [ ] Make functions suspend
- [ ] Add lifecycle management
- [ ] Migrate UI components
- [ ] Migrate tests

### Phase 4: Testing
- [ ] Write unit tests
- [ ] Write IPC integration tests
- [ ] Run performance benchmarks
- [ ] Verify IPC latency < 50ms
- [ ] Test error handling

### Phase 5: Documentation
- [ ] Create module README
- [ ] Update architecture diagrams
- [ ] Document API changes
- [ ] Add usage examples
- [ ] Update status documents

### Post-Migration
- [ ] Code review
- [ ] Commit with detailed message
- [ ] Update project status
- [ ] Create completion document

---

## Common Pitfalls & Solutions

### Pitfall 1: Forgetting RemoteException Handling
**Problem**:
```kotlin
suspend fun getUser(id: Int) = withContext(Dispatchers.IO) {
    databaseClient.getCollection<User>("users").find { it.id == id }
}
```

**Solution**:
```kotlin
suspend fun getUser(id: Int) = withContext(Dispatchers.IO) {
    try {
        databaseClient.getCollection<User>("users").find { it.id == id }
    } catch (e: RemoteException) {
        Log.e(TAG, "IPC error", e)
        null
    }
}
```

### Pitfall 2: Not Using Suspend Functions
**Problem**:
```kotlin
fun saveUser(user: User) {
    databaseClient.getCollection<User>("users").insert(user)  // Blocks main thread!
}
```

**Solution**:
```kotlin
suspend fun saveUser(user: User) = withContext(Dispatchers.IO) {
    try {
        databaseClient.getCollection<User>("users").insert(user)
        true
    } catch (e: RemoteException) {
        Log.e(TAG, "IPC error", e)
        false
    }
}
```

### Pitfall 3: Missing Lifecycle Management
**Problem**:
```kotlin
class MyActivity : AppCompatActivity() {
    private val databaseClient = DatabaseClient.getInstance(this)
    // No bind/unbind!
}
```

**Solution**:
```kotlin
class MyActivity : AppCompatActivity() {
    private lateinit var databaseClient: DatabaseClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        databaseClient = DatabaseClient.getInstance(this)
        databaseClient.bind()
    }

    override fun onDestroy() {
        databaseClient.unbind()
        super.onDestroy()
    }
}
```

### Pitfall 4: Not Making Data Classes Parcelable
**Problem**:
```kotlin
data class User(val id: Int, val name: String)
// IPC will fail!
```

**Solution**:
```kotlin
@Parcelize
data class User(val id: Int, val name: String) : Parcelable
```

---

## Performance Guidelines

### IPC Latency Targets
- **AIDL (Internal Modules)**: 30-50ms
- **ContentProvider (External Plugins)**: 60-100ms

### Best Practices
1. **Batch Operations**: Combine multiple IPC calls
2. **Cache Results**: Cache frequently accessed data
3. **Async Everything**: Use coroutines for all IPC
4. **Error Handling**: Always catch RemoteException
5. **Timeout**: Add timeout for IPC calls

### Example: Batch Operations
**Bad** (3 IPC calls = 90-150ms):
```kotlin
suspend fun getUsers(ids: List<Int>): List<User> {
    return ids.mapNotNull { id ->
        databaseClient.getCollection<User>("users").find { it.id == id }
    }
}
```

**Good** (1 IPC call = 30-50ms):
```kotlin
suspend fun getUsers(ids: List<Int>): List<User> {
    return databaseClient.getCollection<User>("users")
        .filter { it.id in ids }
}
```

---

## Example: Complete Module Port

### Source Module (AVA AI - VoiceRecognition)

**Before**:
```kotlin
// AVA AI: VoiceRecognition.kt
class VoiceRecognition(private val database: Database) {
    fun saveRecognitionResult(text: String) {
        val results = database.getCollection<RecognitionResult>("recognition_results")
        results.insert(RecognitionResult(text, System.currentTimeMillis()))
    }

    fun getHistory(): List<RecognitionResult> {
        val results = database.getCollection<RecognitionResult>("recognition_results")
        return results.toList()
    }
}

data class RecognitionResult(val text: String, val timestamp: Long)
```

### Target Module (Avanues - VoiceRecognition)

**After**:
```kotlin
// Avanues: Universal/VoiceRecognition/src/.../VoiceRecognition.kt
import android.content.Context
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.os.RemoteException
import android.util.Log

class VoiceRecognition(private val context: Context) {
    private val databaseClient = DatabaseClient.getInstance(context)

    suspend fun saveRecognitionResult(text: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val results = databaseClient.getCollection<RecognitionResult>("recognition_results")
            results.insert(RecognitionResult(text, System.currentTimeMillis()))
            Log.d(TAG, "Saved recognition result: $text")
            true
        } catch (e: RemoteException) {
            Log.e(TAG, "IPC error saving recognition result", e)
            false
        }
    }

    suspend fun getHistory(): List<RecognitionResult> = withContext(Dispatchers.IO) {
        try {
            val results = databaseClient.getCollection<RecognitionResult>("recognition_results")
            results.toList()
        } catch (e: RemoteException) {
            Log.e(TAG, "IPC error getting history", e)
            emptyList()
        }
    }

    companion object {
        private const val TAG = "VoiceRecognition"
    }
}

@Parcelize
data class RecognitionResult(
    val text: String,
    val timestamp: Long
) : Parcelable
```

**Changes Made**:
1. ✅ Changed constructor from `Database` to `Context`
2. ✅ Initialize `DatabaseClient.getInstance(context)`
3. ✅ Made functions `suspend`
4. ✅ Wrapped in `withContext(Dispatchers.IO)`
5. ✅ Added `try-catch RemoteException`
6. ✅ Added logging
7. ✅ Made `RecognitionResult` Parcelable
8. ✅ Return `Boolean` for success/failure

---

## Template: Module Porting Document

Use this template for each module:

```markdown
# [Module Name] Porting Document

## Module Info
- **Name**: [Module Name]
- **Source**: [AVA AI / AVAConnect / BrowserAvanue]
- **Target**: `Universal/[Module]` or `android/avanues/[module]`
- **Type**: Internal Module / External Plugin
- **IPC Method**: AIDL / ContentProvider
- **AI Assistant**: [Assistant Name]
- **Start Date**: [Date]
- **Completion Date**: [Date]

## Dependencies
[List dependencies]

## Database Operations
[List all database calls]

## IPC Changes
### Before
```kotlin
[Old code]
```

### After
```kotlin
[New code with IPC]
```

## Files Migrated
- [ ] `[Source]` → `[Target]`
- [ ] `[Source]` → `[Target]`

## Tests Added
- [ ] Unit tests
- [ ] IPC tests
- [ ] Performance tests

## Documentation
- [ ] Module README
- [ ] API documentation
- [ ] Architecture diagrams

## Performance Metrics
- IPC Latency: [X]ms
- Memory: [Y]MB
- Success Rate: [Z]%

## Status
- [x] Analysis complete
- [x] Preparation complete
- [x] Migration complete
- [x] Testing complete
- [x] Documentation complete
- [x] Reviewed
- [x] Merged

## Notes
[Any special notes or issues encountered]
```

---

## Summary

This guide provides:
- ✅ Step-by-step module porting methodology
- ✅ IPC migration patterns
- ✅ Code transformation examples
- ✅ Testing strategies
- ✅ Documentation templates
- ✅ Performance guidelines
- ✅ Common pitfalls and solutions

**Use this guide** every time you port a module to ensure consistency and completeness.

---

**Document Version**: 1.0.0
**Author**: Manoj Jhawar, manoj@ideahq.net
**AI-Friendly**: Yes ✅
