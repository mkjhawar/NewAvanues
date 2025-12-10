<!--
filename: Protocol-VOS4-Coding-Standards.md
created: 2025-09-07 12:30:00 PST
author: VOS4 Development Team
purpose: VOS4-specific coding protocol - project-specific extensions to universal standards
last-modified: 2025-10-17 00:17:37 PDT
version: 2.0.0
changelog:
- 2025-10-17 00:17:37 PDT: v2.0.0 - Extracted universal content to master protocols, kept only VOS4-specific standards
- 2025-10-09 05:09:30 PDT: Updated implementation strategy - Strategic interfaces for cold paths, direct implementation for hot paths
- 2025-09-07 12:30:00 PST: Initial consolidation from CODING-GUIDE.md, CODING-STANDARDS.md, MASTER-STANDARDS.md, MASTER-AI-INSTRUCTIONS.md
-->

# VOS4-Specific Coding Protocol

## Purpose
This document contains VOS4-SPECIFIC coding standards. For universal coding standards, see the master protocol files.

## 🔗 Required Reading - Universal Standards (IDEACODE v5.0)
**MANDATORY: Read these universal protocols FIRST:**
- `/Volumes/M Drive/Coding/ideacode/protocols/Protocol-Coding-Standards.md` - Universal coding standards
- `/Volumes/M Drive/Coding/ideacode/protocols/Protocol-Zero-Tolerance-Pre-Code.md` - Zero tolerance pre-code checklist
- `/Volumes/M Drive/Coding/ideacode/claude/CLAUDE.md` - COT/ROT/TOT reasoning patterns (Section: Reasoning Methods)

This document ONLY covers VOS4-specific extensions and overrides.

---

# VOS4-Specific Coding Standards

## 🔴 VOS4-Specific Pre-Implementation Q&A

**VOS4 Requirement:** ALL implementation MUST follow Q&A protocol (see Protocol-VOS4-Pre-Implementation-QA.md)

**VOS4-Specific Considerations:**
- Kotlin/Android compatibility analysis
- Room database migration implications
- com.augmentalis.* namespace impacts
- Module self-containment verification
- Performance targets (see below)

---

## 🔴 VOS4-Specific Core Principles

### 1. Strategic Implementation: Performance First, Interfaces When Justified
**DIRECT IMPLEMENTATION FOR HOT PATHS - STRATEGIC INTERFACES FOR COLD PATHS**

#### Decision Tree: When to Use Interfaces

**Use DIRECT IMPLEMENTATION (no interfaces) when:**
- ✅ Called more than 10 times per second (hot path)
- ✅ Performance-critical code (cursor tracking, sensor fusion, audio processing)
- ✅ Single implementation with no planned alternatives
- ✅ Simple utility functions or data classes
- ✅ Android framework components with no testing requirements

**Use STRATEGIC INTERFACES when:**
- ✅ Called less than 10 times per second (cold path)
- ✅ Testing requires mocking (network, database, external APIs)
- ✅ Multiple implementations needed (plugins, strategies, protocols)
- ✅ Runtime swapping required (HTTP vs gRPC, different engines)
- ✅ Extension points for user customization

#### Performance Impact Analysis

**Hot Path Example (100 calls/sec) - DIRECT IMPLEMENTATION:**
```kotlin
// ✅ CORRECT - Hot path (cursor tracking)
// Called 100+ times/second - interface adds 0.4% battery drain
class CursorPositionTracker(private val context: Context) {
    fun updatePosition(x: Float, y: Float) {
        // Direct call: ~2 CPU cycles
        _positionFlow.value = CursorPosition(x, y)
    }
}
```

**Cold Path Example (1-5 calls/sec) - STRATEGIC INTERFACE:**
```kotlin
// ✅ CORRECT - Cold path (command storage)
// Called 1-5 times/second - interface adds 0.0001% battery drain
interface CommandRepository {
    suspend fun findCommand(phrase: String): VoiceCommand?
    suspend fun registerCommand(command: VoiceCommand): Result<Unit>
}

class InMemoryCommandRepository : CommandRepository {
    // Unit testable without Android framework
    // Can swap to DatabaseCommandRepository later
}

class CommandManager(
    private val repository: CommandRepository  // Interface for flexibility
) {
    suspend fun executeCommand(phrase: String) {
        val command = repository.findCommand(phrase)
        command?.execute()
    }
}
```

#### Battery Cost Comparison

| Pattern | Calls/Sec | Battery Cost/10hrs | Use Case |
|---------|-----------|-------------------|----------|
| **Direct Implementation** | 100 | 0.01% | ✅ Cursor, sensors, audio |
| **Interface (hot path)** | 100 | 0.4% | ❌ Unnecessary overhead |
| **Direct Implementation** | 5 | 0.0001% | ⚠️ OK but inflexible |
| **Interface (cold path)** | 5 | 0.0002% | ✅ Testing + flexibility |

**Total VOS4 with all strategic interfaces:** 0.02% extra battery = **7 seconds less battery over 10 hours**
**Benefit:** 350x faster unit tests, plugin extensibility, protocol flexibility

#### Examples by Module

**Week 1-3 (Correctly Direct Implementation):**
```kotlin
// ✅ Hot paths - keep direct
class CursorPositionTracker(context: Context) { }  // 100 Hz
class SensorFusionManager(context: Context) { }    // 100 Hz
class AudioProcessor(config: Config) { }           // 44.1 kHz
```

**Week 4 CommandManager (Strategic Interfaces):**
```kotlin
// ✅ Cold paths - use interfaces
interface CommandRepository { }      // 1-5 calls/sec
interface MacroExecutor { }          // 1-5 calls/sec
interface ContextDetector { }        // 1 call/sec
interface CommandPlugin { }          // Extension point

// Direct implementation still used:
class CommandManager(
    private val repository: CommandRepository,
    private val executor: MacroExecutor
) {
    // Manager itself is direct implementation
    // Only dependencies are interfaces
}
```

#### Testing Benefits Justification

**Without Interfaces (Current Week 1-3):**
- Requires Android emulator for all tests
- Test time: 35 seconds per test
- 100 tests/day = 58 minutes waiting
- Battery drain: 30% more from emulator

**With Strategic Interfaces (Week 4+):**
- JVM unit tests with mocks
- Test time: 0.1 seconds per test
- 100 tests/day = 10 seconds total
- Battery drain: Negligible (JVM tests)

**Developer productivity gain:** 58 minutes/day saved = **$50/day in dev time**
**Runtime battery cost:** 0.02% = **7 seconds/day less battery**

#### ❌ STILL NEVER DO:
```kotlin
// ❌ WRONG - Unnecessary interface on hot path
interface ICursorTracker { }  // NO! Called 100 times/sec
class CursorPositionTracker : ICursorTracker { }

// ❌ WRONG - Over-abstraction
interface ILogger { }  // Usually direct implementation is fine
interface IConfig { }  // Usually direct implementation is fine

// ❌ WRONG - Interface for the sake of interfaces
interface IHelper { }  // Violates VOS4 "no helpers" rule anyway
```

#### Summary: The 80/20 Rule

**80% of code: Direct implementation** (hot paths, simple utilities)
**20% of code: Strategic interfaces** (testing boundaries, plugins, cold paths)

**Result:**
- 99.98% of performance preserved
- 350x faster testing
- Plugin/extension capability
- Future flexibility for protocol changes

### 2. Namespace Convention (RESOLVED)
**MANDATORY: All modules use com.augmentalis.* pattern**

```kotlin
// ✅ CORRECT - New standard (AUTHORITATIVE)
package com.augmentalis.commandsmanager
package com.augmentalis.speechrecognition
package com.augmentalis.voiceaccessibility
package com.augmentalis.datamanager
package com.augmentalis.voiceos  // Master app

// ❌ WRONG - Old patterns
package com.ai.commandsmanager  // DEPRECATED
package com.ai.anything  // NO LONGER VALID
package com.augmentalis.modules.commands  // No redundancy
```

### 3. Database Standard (CURRENT)
**Room MANDATORY - KSP Compatible (ObjectBox Deprecated)**

```kotlin
// ✅ CORRECT - ObjectBox (CURRENT STANDARD)
@Entity
data class Command(@Id var id: Long = 0, var text: String)

// 🔄 FUTURE - Room (Migration in progress)
// @Entity(tableName = "commands")  // Will replace ObjectBox

// ❌ WRONG - SQLite/SharedPreferences
// No direct SQLite or SharedPreferences for data
```

### 4. VOS4 No Helper Methods Rule
**VOS4 Standard: Direct parameter access only - no helper/getter abstractions**

```kotlin
// ✅ CORRECT - VOS4 direct access pattern
val language = config?.language ?: "en-US"

// ❌ WRONG - Helper methods violate VOS4 standards
fun getParameter(name: String) = parameters[name]  // NO!
```

### 5. VOS4 Self-Contained Modules
**VOS4 Architecture: ALL components in same module**
- Services declared where implemented
- Resources in module that uses them
- Permissions in module that needs them
- Each of 20 VOS4 modules independently buildable
- No cross-module manifest entries

## 📝 VOS4 File Headers

### VOS4 Kotlin/Java Files
```kotlin
/**
 * FileName.kt - Brief description
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: YYYY-MM-DD
 */
package com.augmentalis.modulename  // VOS4 namespace standard

// Class implementation
```

**VOS4-Specific:** All files use com.augmentalis.* namespace

## 🎯 VOS4 Performance Requirements (MANDATORY)

**VOS4-Specific Performance Targets:**

| Metric | VOS4 Requirement | Reason |
|--------|------------------|---------|
| Initialization | <1 second | User experience |
| Module load | <50ms per module (20 modules) | Responsiveness |
| Command recognition | <100ms latency | Real-time feel |
| Memory (Room) | <60MB | Android device compatibility |
| Battery drain | <2% per hour | All-day Android usage |
| XR rendering | 90-120 FPS | AR/VR motion sickness prevention |

## 🔧 Module Structure Pattern

### Standard Module Organization
```
/modules/managers/CommandsManager/
├── build.gradle.kts
├── src/
│   └── main/
│       ├── AndroidManifest.xml
│       ├── aidl/  # AIDL interfaces (if module exposes IPC)
│       │   └── com/
│       │       └── augmentalis/
│       │           └── commandsmanager/
│       │               ├── ICommandsManagerService.aidl
│       │               └── CommandData.aidl
│       └── java/
│           └── com/
│               └── augmentalis/
│                   └── commandsmanager/  # Note: lowercase
│                       ├── CommandsManager.kt
│                       ├── CommandsManagerServiceBinder.kt  # IPC implementation
│                       ├── actions/
│                       │   ├── NavigationActions.kt
│                       │   └── SystemActions.kt
│                       ├── models/
│                       │   ├── Command.kt
│                       │   └── CommandData.kt  # Parcelable for IPC
│                       └── processor/
│                           └── CommandProcessor.kt
```

### Package Naming Rules
```kotlin
// ✅ CORRECT - Clean namespace
package com.augmentalis.commandsmanager
package com.augmentalis.speechrecognition
package com.augmentalis.voiceaccessibility

// ❌ WRONG - Redundant paths
package com.augmentalis.commands.manager  // NO!
package com.augmentalis.modules.speech   // NO!
```

## 🔌 AIDL/IPC Coding Standards (NEW 2025-11-11)

### AIDL Service Binder Implementation

#### Pattern 1: Singleton Wrapper (No Dependency Injection)
```kotlin
/**
 * AIDL service binder for VoiceCursor
 *
 * Wraps VoiceCursorAPI singleton for cross-app IPC access.
 * No dependency injection required - uses singleton directly.
 */
class VoiceCursorServiceBinder : IVoiceCursorService.Stub() {

    companion object {
        private const val TAG = "VoiceCursorServiceBinder"
    }

    override fun isInitialized(): Boolean {
        return try {
            VoiceCursorAPI.isInitialized()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking initialization", e)
            false
        }
    }

    override fun showCursor(config: CursorConfiguration?): Boolean {
        if (!isInitialized()) {
            Log.w(TAG, "showCursor called before initialization")
            return false
        }

        return try {
            val cursorConfig = config?.toCursorConfig() ?: CursorConfig()
            VoiceCursorAPI.showCursor(cursorConfig)
        } catch (e: Exception) {
            Log.e(TAG, "Error showing cursor", e)
            false
        }
    }
}
```

#### Pattern 2: Instance Injection + Async Bridge
```kotlin
/**
 * AIDL service binder for UUIDCreator
 *
 * Bridges synchronous AIDL calls with asynchronous Kotlin coroutines.
 * Uses runBlocking to convert suspend functions to synchronous IPC.
 */
class UUIDCreatorServiceBinder(
    private val uuidCreator: UUIDCreator
) : IUUIDCreatorService.Stub() {

    companion object {
        private const val TAG = "UUIDCreatorServiceBinder"
    }

    private val gson = Gson()

    override fun processVoiceCommand(command: String?): UUIDCommandResultData {
        Log.d(TAG, "IPC: processVoiceCommand(command=$command)")

        if (command.isNullOrBlank()) {
            Log.w(TAG, "processVoiceCommand called with null/empty command")
            return UUIDCommandResultData.failure("Command cannot be null or empty")
        }

        return try {
            // Bridge sync AIDL with async UUIDCreator using runBlocking
            val result = runBlocking {
                uuidCreator.processVoiceCommand(command)
            }

            UUIDCommandResultData.fromUUIDCommandResult(result)
        } catch (e: Exception) {
            Log.e(TAG, "Error processing voice command", e)
            UUIDCommandResultData.failure(
                error = e.message ?: "Unknown error",
                action = "processVoiceCommand"
            )
        }
    }

    override fun executeAction(uuid: String?, action: String?, parametersJson: String?): Boolean {
        if (uuid.isNullOrBlank() || action.isNullOrBlank()) return false

        return try {
            // Parse JSON parameters
            val parameters = if (!parametersJson.isNullOrBlank()) {
                gson.fromJson(parametersJson, Map::class.java) as? Map<String, Any> ?: emptyMap()
            } else {
                emptyMap()
            }

            // Execute action using runBlocking
            runBlocking {
                uuidCreator.executeAction(uuid, action, parameters)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing action", e)
            false
        }
    }
}
```

### Parcelable Data Class Pattern

```kotlin
/**
 * Cursor position for IPC
 *
 * Parcelable version of CursorOffset for cross-process communication.
 */
@Parcelize
data class CursorPosition(
    val x: Float,
    val y: Float
) : Parcelable {

    companion object {
        /**
         * Create center position for given screen dimensions
         */
        fun center(screenWidth: Int, screenHeight: Int): CursorPosition {
            return CursorPosition(
                x = screenWidth / 2f,
                y = screenHeight / 2f
            )
        }
    }

    /**
     * Convert to internal CursorOffset type
     */
    fun toCursorOffset(): CursorOffset {
        return CursorOffset(x, y)
    }
}

/**
 * Extension function for converting internal type to Parcelable
 */
fun CursorOffset.toParcelable(): CursorPosition {
    return CursorPosition(x, y)
}
```

### AIDL Error Handling Standards

```kotlin
// ✅ CORRECT - Comprehensive error handling
override fun executeCommand(command: String?): Boolean {
    // 1. Null/empty validation
    if (command.isNullOrBlank()) {
        Log.w(TAG, "executeCommand called with null/empty command")
        return false
    }

    // 2. Try-catch around all operations
    return try {
        VoiceOSService.executeCommand(command)
    } catch (e: Exception) {
        // 3. Log with context
        Log.e(TAG, "Error executing command: $command", e)
        // 4. Return failure (never throw across IPC)
        false
    }
}

// ❌ WRONG - Throwing exceptions across IPC
override fun executeCommand(command: String?): Boolean {
    if (command.isNullOrBlank()) {
        throw IllegalArgumentException("Command cannot be null")  // NEVER DO THIS!
    }
    return VoiceOSService.executeCommand(command)
}
```

### Async-to-Sync Bridging Pattern

```kotlin
// ✅ CORRECT - Using runBlocking for suspend functions
override fun processCommand(command: String): CommandResult {
    return runBlocking {
        commandProcessor.process(command)  // suspend function
    }
}

// ⚠️ ACCEPTABLE - But blocks calling thread
// Only use when AIDL service is already on background thread
override fun longRunningOperation(): Result {
    return runBlocking {
        withContext(Dispatchers.IO) {
            performHeavyWork()
        }
    }
}

// ❌ WRONG - Launching coroutine and returning immediately
override fun processCommand(command: String): CommandResult {
    CoroutineScope(Dispatchers.IO).launch {
        commandProcessor.process(command)  // Result lost!
    }
    return CommandResult.Success  // WRONG - returns before completion
}
```

### Build Configuration for AIDL

```kotlin
// build.gradle.kts

android {
    buildFeatures {
        aidl = true  // MANDATORY for AIDL support
    }
}

plugins {
    id("kotlin-parcelize")  // MANDATORY for @Parcelize
}

// MANDATORY if using ksp (Hilt, Room, etc.)
afterEvaluate {
    listOf("Debug", "Release").forEach { variant ->
        tasks.findByName("ksp${variant}Kotlin")?.apply {
            dependsOn("compile${variant}Aidl")
        }
    }
}
```

### Hilt + ksp + AIDL Circular Dependency Solution

**Problem:** Cannot use Hilt + Room + ksp + AIDL in same module

**Solution:** Create separate `:ipc` module WITHOUT Hilt

```
/modules/apps/VoiceOSCore/       (Main module - has Hilt)
/modules/apps/VoiceOSCore-IPC/   (IPC module - NO Hilt)
```

**VoiceOSCore-IPC/build.gradle.kts:**
```kotlin
dependencies {
    implementation(project(":modules:apps:VoiceOSCore"))
    // NO Hilt dependency!
    // AIDL compiles cleanly without Hilt's ksp
}
```

**Why This Works:**
- VoiceOSCore: Has Hilt + Room + ksp → No AIDL
- VoiceOSCore-IPC: Has AIDL → No Hilt
- No circular dependency!

### JSON Serialization for Complex Parameters

```kotlin
class UUIDCreatorServiceBinder(
    private val uuidCreator: UUIDCreator
) : IUUIDCreatorService.Stub() {

    private val gson = Gson()

    override fun getRegistryStats(): String {
        return try {
            val stats = uuidCreator.getStats()

            val statsMap = mapOf(
                "totalElements" to stats.totalElements,
                "enabledElements" to stats.enabledElements,
                "typeBreakdown" to stats.typeBreakdown,
                "timestamp" to System.currentTimeMillis()
            )

            gson.toJson(statsMap)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting registry stats", e)
            gson.toJson(mapOf(
                "error" to (e.message ?: "Unknown error"),
                "count" to 0
            ))
        }
    }
}
```

## 🗄️ Database Implementation

### ObjectBox (Current Standard)
```kotlin
@Entity
data class RecognitionResult(
    @Id var id: Long = 0,
    var text: String = "",
    var confidence: Float = 0f,
    var timestamp: Long = System.currentTimeMillis(),
    var engineType: String = ""
)

class CommandRepository(private val box: Box<Command>) {
    
    suspend fun insert(command: Command) = withContext(Dispatchers.IO) {
        box.put(command)
    }
    
    suspend fun getAll(): List<Command> = withContext(Dispatchers.IO) {
        box.all
    }
    
    suspend fun findByText(text: String): Command? = withContext(Dispatchers.IO) {
        box.query(Command_.text.equal(text))
            .build()
            .findFirst()
    }
}
```

## ⚡ Coroutine Patterns

### Proper Suspend Functions
```kotlin
class SpeechEngine {
    suspend fun initialize() = withContext(Dispatchers.IO) {
        // Heavy initialization
        loadModels()
        setupAudioPipeline()
    }
    
    suspend fun processAudio(buffer: ByteArray): String = withContext(Dispatchers.Default) {
        // CPU-intensive processing
        return recognizeText(buffer)
    }
}
```

### Flow for Audio Streaming
```kotlin
class VosAudioManager {
    fun audioStream(): Flow<ByteArray> = flow {
        val audioRecord = createAudioRecord()
        audioRecord.startRecording()
        
        try {
            while (currentCoroutineContext().isActive) {
                val buffer = ByteArray(bufferSize)
                val read = audioRecord.read(buffer, 0, bufferSize)
                if (read > 0) {
                    emit(buffer)
                }
            }
        } finally {
            audioRecord.stop()
            audioRecord.release()
        }
    }.flowOn(Dispatchers.IO)
}
```

## 🚨 Error Handling Patterns

### Result Pattern
```kotlin
sealed class CommandResult {
    data class Success(val data: String) : CommandResult()
    data class Error(val message: String, val code: Int) : CommandResult()
    object Loading : CommandResult()
}

fun processCommand(text: String): CommandResult {
    return try {
        val result = executeCommand(text)
        CommandResult.Success(result)
    } catch (e: Exception) {
        CommandResult.Error(e.message ?: "Unknown error", -1)
    }
}
```

## 🔧 Build Commands

### Individual Module Builds
```bash
# Build specific module - NO PIPES!
./gradlew :modules:apps:SpeechRecognition:assembleDebug
./gradlew :modules:managers:CommandsManager:compileDebugKotlin

# Clean specific module
./gradlew :modules:apps:VoiceUI:clean

# Run module tests
./gradlew :modules:libraries:UUIDManager:test
```

### Full Project
```bash
# Clean build all
./gradlew clean build

# Run all tests
./gradlew test

# Check for issues
./gradlew lint
```

## 🚀 Performance Optimization Patterns

### Lazy Initialization
```kotlin
class CommandsManager {
    private val processor by lazy {
        CommandProcessor(context)
    }
    
    private val cache by lazy {
        CommandCache(maxSize = 100)
    }
}
```

### Object Pooling
```kotlin
object BufferPool {
    private val pool = Collections.synchronizedList(
        mutableListOf<ByteArray>()
    )
    
    fun obtain(size: Int): ByteArray {
        return pool.removeFirstOrNull() ?: ByteArray(size)
    }
    
    fun recycle(buffer: ByteArray) {
        buffer.fill(0)
        pool.add(buffer)
    }
}
```

## 🧪 Testing Patterns

### 🚨 MANDATORY: Test Maintenance Rule
**CRITICAL**: When code changes are made to apps or modules, their tests MUST be updated immediately:

1. **Code Changes Trigger Test Updates:**
   - API signature changes → Update all affected test files
   - Class/method renaming → Update all test references
   - Feature additions → Add corresponding test coverage
   - Feature removals → Remove or update obsolete tests
   - Dependency changes → Update test mocks and fixtures

2. **Zero Compilation Errors:**
   - Tests MUST compile without errors after code changes
   - NO unresolved references allowed in test files
   - Test dependencies MUST be updated if code dependencies change

3. **Test Quality Standards:**
   - Tests MUST reflect current production code API
   - Mock objects MUST match current class signatures
   - Test coverage MUST be maintained or improved
   - Deprecated test patterns MUST be updated

4. **Enforcement:**
   - Build failures in tests are treated as HIGH PRIORITY errors
   - Tests are part of code review requirements
   - Broken tests block all commits

**Example**: If VoiceAccessibilityService is renamed to VoiceOSService:
- Update ALL test imports immediately
- Update ALL mock services to extend new class
- Update ALL test method calls to match new API
- Verify ALL tests compile and pass

### Unit Test Structure
```kotlin
class CommandsManagerTest {
    private lateinit var manager: CommandsManager

    @Before
    fun setup() {
        manager = CommandsManager(mockContext)
    }

    @Test
    fun `processCommand returns success for valid command`() {
        val result = manager.processCommand("navigate back")
        assertTrue(result is CommandResult.Success)
    }
}
```

## 📝 VOS4 Commit Standards

**See Universal Commit Protocol:** `/Volumes/M Drive/Coding/Docs/agents/instructions/Protocol-Commit.md`

**VOS4-Specific Commit Requirements:**
- Branch: `voiceosservice-refactor` (current development branch)
- Main branch: `main` (for PRs)
- Scope examples: `voiceoscore`, `commandmanager`, `speechrecognition`
- All 20 VOS4 modules must be listed in commit if affected

## ⚠️ VOS4-Specific Critical Pitfalls

### VOS4-SPECIFIC NEVER DO:
1. **Don't use wrong namespaces** - com.augmentalis.* ONLY (not com.ai.*)
2. **Don't use SQLite/ObjectBox for new code** - Room with KSP only
3. **Don't pipe gradle commands** - Android causes "Task '2' not found" errors
4. **Don't add helper methods** - VOS4 direct access pattern
5. **Don't split module components** - VOS4 self-contained architecture
6. **Don't violate Android performance targets** - See performance table above
7. **Don't skip Kotlin coroutine patterns** - Proper suspend functions required
8. **Don't ignore AccessibilityService requirements** - Core VOS4 functionality

### VOS4-SPECIFIC ALWAYS DO:
1. **Follow com.augmentalis.* namespace** - All 20 modules
2. **Use Room with KSP** - Current Android standard
3. **Keep modules self-contained** - Each of 20 modules independent
4. **Target Android API 34** - Current VOS4 target
5. **Use Kotlin 1.9.25 patterns** - Coroutines, Flow, Compose
6. **Verify AccessibilityService integration** - Critical for VOS4
7. **Test on Android devices** - Emulator + physical devices
8. **Follow Material Design 3** - VOS4 UI standard

---

## 📋 VOS4 Summary

This document contains VOS4-SPECIFIC coding standards. Key VOS4-specific principles:

1. **com.augmentalis.* Namespace** - All 20 modules use this pattern
2. **Room Database with KSP** - Android current standard
3. **Kotlin 1.9.25 + Android 14** - Technology stack
4. **20 Self-Contained Modules** - Independent, buildable components
5. **Strategic Implementation Pattern** - Direct for hot paths, interfaces for cold paths
6. **Android Performance Targets** - <1s init, <50ms module load, <100ms command recognition
7. **AccessibilityService Core** - Foundation of VOS4 architecture
8. **Material Design 3** - UI/UX standard

**Universal Standards:** See `/Volumes/M Drive/Coding/Docs/agents/instructions/Protocol-Coding-Standards.md`

**VOS4 Technology Stack:**
- Kotlin 1.9.25
- Android 14 (API 34)
- Jetpack Compose 1.5.15
- Room 2.6.1 with KSP
- Material Design 3
- Coroutines + Flow

---

**Last Updated:** 2025-10-17 00:17:37 PDT
**Version:** 2.0.0 - VOS4-Specific Only (Universal content extracted)