# ⚠️ DEPRECATED - This file has been superseded

**Status:** DEPRECATED as of 2025-10-15
**New Location:** `/Volumes/M Drive/Coding/Warp/Agent-Instructions/VOS4-CODING-PROTOCOL.md`
**Reason:** Consolidated into VOS4-CODING-PROTOCOL.md
**Archived By:** Documentation Consolidation Agent

This file is kept for historical reference only. DO NOT use for new development.

---

[Original content below]

<!--
filename: CODING-GUIDE.md
created: 2025-01-23 18:56:00 PST
author: VOS4 Development Team
purpose: Detailed coding patterns and examples
last-modified: 2025-08-27 23:00:00 PDT
version: 1.3.0
changelog:
- 2025-08-27 23:00:00 PDT: Added MANDATORY COT/ROT/TOT analysis requirements
- 2025-01-27 20:00:00 PST: Added duplicate prevention standards
- 2025-01-23 22:35:00 PST: Added mandatory benefits documentation template
- 2025-01-23 22:30:00 PST: Added functional equivalency requirements with examples
- 2025-01-23: Initial creation - extracted from CLAUDE.md and .warp.md
-->

# VOS4 Coding Guide - Patterns & Examples

## 🚨 MANDATORY: COT/ROT/TOT Analysis for ALL Code Issues

### When Analysis is REQUIRED:
**ALL code issues, bugs, warnings, or problems MUST undergo formal analysis:**

1. **COT (Chain of Thought)** - ALWAYS REQUIRED
   - Linear step-by-step analysis of the problem
   - Identify root cause
   - Propose initial solution

2. **ROT (Reflection on Thought)** - ALWAYS REQUIRED
   - Evaluate the COT solution
   - Check for edge cases
   - Verify correctness

3. **TOT (Train of Thought)** - REQUIRED IF:
   - Multiple solutions exist
   - ROT reveals potential issues
   - Complex architectural decisions

4. **Final COT + ROT** - REQUIRED AFTER TOT
   - Re-analyze chosen solution
   - Final verification

### Analysis Process:

```markdown
## COT Analysis:
1. Problem: [Describe the issue]
2. Root Cause: [Why it's happening]
3. Solution: [Proposed fix]
4. Implementation: [How to fix]

## ROT Analysis:
1. Solution Review: [Is COT solution correct?]
2. Edge Cases: [What could go wrong?]
3. Side Effects: [Any unintended consequences?]
4. Confidence: [High/Medium/Low]

## TOT Analysis (if needed):
### Option 1: [Solution A]
- Pros: [Benefits]
- Cons: [Drawbacks]

### Option 2: [Solution B]
- Pros: [Benefits]
- Cons: [Drawbacks]

### Recommendation: [Which option and why]

## Final Decision:
- Selected Solution: [What we're doing]
- Reasoning: [Why this approach]
- User Options: [Present to user unless told to work independently]
```

### When to Present Options vs Decide:
- **DEFAULT:** Present analysis + options + recommendation to user
- **EXCEPTION:** If user says "work independently" → Make decision based on analysis
- **ALWAYS:** Document the analysis in commit messages or comments

### Example Analysis:

```markdown
## Issue: Unnecessary safe call warning on non-nullable Long

### COT:
1. Problem: Compiler warning about `?.let` on non-nullable type
2. Root Cause: `timeoutDuration` is Long (not Long?)
3. Solution: Remove safe call operator
4. Implementation: Change `?.let` to `.let`

### ROT:
1. Solution correct - type is indeed non-nullable
2. No edge cases - compiler enforces type safety
3. No side effects - behavior unchanged
4. Confidence: High

### Decision:
Remove safe call operators (user approved)
```

## 🔴 CRITICAL: Functional Equivalency Rule

### MANDATORY for ALL Refactoring/Imports/Merges:
**100% FUNCTIONAL EQUIVALENCY - ZERO TOLERANCE POLICY**
**UNLESS EXPLICITLY TOLD OTHERWISE BY USER**

**UNIVERSAL RULE**: ALL imports, merges, refactoring, or code integration MUST maintain 100% feature parity unless the user explicitly approves changes.

When refactoring, merging, or importing code:
1. **NEVER remove features** - Even if they seem unused (without approval)
2. **NEVER change behavior** - Maintain exact functionality
3. **ALWAYS preserve signatures** - Keep method/parameter names
4. **ALWAYS maintain compatibility** - No breaking changes
5. **Enhancements are OK** - But original functionality MUST remain
6. **ALL code mergers** - MUST be 100% functionally equivalent
7. **Document equivalency** - Create feature comparison matrix before/after

Example:
```kotlin
// ORIGINAL CODE (VOS3)
fun processCommand(text: String, useAI: Boolean = false): Result {
    // Feature A
    // Feature B
    // Feature C
}

// ✅ CORRECT REFACTOR (VOS4)
fun processCommand(text: String, useAI: Boolean = false): Result {
    // Feature A (maintained)
    // Feature B (maintained)
    // Feature C (maintained)
    // Feature D (enhancement OK)
}

// ❌ WRONG REFACTOR
fun processCommand(text: String): Result {  // REMOVED parameter!
    // Feature A
    // Feature B
    // Feature C removed - VIOLATION!
}
```

### Document Benefits (MANDATORY):
After ANY refactoring or import, document in changelog:

```markdown
## Refactoring Benefits Achieved

### Performance Improvements
- Initialization: 50ms → 10ms (80% reduction)
- Memory usage: 20MB → 15MB (25% reduction)
- Response time: 100ms → 50ms (50% faster)

### Code Quality
- Lines of code: 3163 removed, 958 added (70% reduction)
- Cyclomatic complexity: Reduced by 40%
- Removed 15 duplicate code blocks

### Maintainability
- Eliminated abstract classes (direct implementation)
- Reduced inheritance depth from 3 to 0
- Consolidated 5 packages into 3

### Bug Fixes
- Fixed memory leak in XYZ
- Resolved race condition in ABC
- Corrected null pointer in DEF

### New Features (Enhancements)
- Added performance monitoring
- Improved error handling
- Enhanced logging
```

## 🔴 NEW: Duplicate Code Prevention Standards

### MANDATORY: Check for Duplicates Before Creating
**ZERO TOLERANCE for unnecessary duplication**

Before creating ANY new class, interface, enum, or function:

1. **SEARCH FIRST** - Use Grep/Glob to find similar names
2. **ANALYZE** - Compare fields, methods, and purpose
3. **DECIDE**:
   - If 100% duplicate → DELETE one, use the other
   - If 80%+ similar → MERGE into single class
   - If <80% similar → Keep separate (document why)

### Duplicate Detection Process:
```kotlin
// STEP 1: Search for similar classes
grep -r "class.*Result" --include="*.kt"
grep -r "data class.*Result" --include="*.kt"

// STEP 2: Compare if found
// Class A: RecognitionResult (api package)
// Class B: SpeechResult (models package)
// Fields: 95% identical
// Methods: 80% overlap
// Decision: MERGE → Keep RecognitionResult (api is public-facing)

// STEP 3: Document decision
// Create: MERGE-DECISION-YYYY-MM-DD-ClassA-ClassB.md
```

### Merge Process When Duplicates Found:
1. **Choose Survivor**:
   - Prefer `api` package over `models`
   - Prefer public-facing over internal
   - Prefer better naming
   - Prefer class with more features

2. **Merge Features**:
   ```kotlin
   // Class A has methods: a(), b(), c()
   // Class B has methods: b(), c(), d()
   // Merged class has: a(), b(), c(), d()
   ```

3. **Update References**:
   - Replace all imports
   - Update all usages
   - Delete redundant class

4. **Document Decision**:
   - Create merge decision document
   - Update changelog
   - Note performance improvements

### Example Duplicate Prevention:
```kotlin
// ❌ WRONG - Creating duplicate
class SpeechResult(val text: String, val confidence: Float)
class RecognitionResult(val text: String, val confidence: Float)

// ✅ CORRECT - Single unified class
class RecognitionResult(
    val text: String,
    val confidence: Float,
    // Merged features from both
    fun isEmpty() = text.isBlank()
    fun meetsThreshold(t: Float) = confidence >= t
)
```

### Performance Impact of Duplicates:
- **Memory**: 2x object allocations
- **GC Pressure**: Doubled
- **Maintenance**: 2x bug fixes needed
- **Confusion**: Which class to use?

## File Headers (MANDATORY)

### Kotlin/Java Files
```kotlin
/**
 * FileName.kt - Brief description
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: YYYY-MM-DD
 */
package com.ai.modulename

// Class implementation
```

### XML Files
```xml
<!--
filename: layout_name.xml
created: 2025-01-23 15:30:00 PST
author: Manoj Jhawar
Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
© Augmentalis Inc
-->
```

## Configuration Access Patterns

### ✅ CORRECT Patterns

**Direct Access with Extensions:**
```kotlin
// Feature parity pattern - direct access
val muteCommand = config?.muteCommand ?: "mute ava"
val language = config?.language ?: "en-US"
val timeout = config?.timeout ?: 5000

// Direct handler assignment with invoke
actionRegistry["nav_back"] = NavigationActions.BackAction()::invoke
actionRegistry["nav_home"] = NavigationActions.HomeAction()::invoke
```

**Direct Module Implementation:**
```kotlin
class CommandsManager(private val context: Context) {
    private val commands = mutableListOf<Command>()

    fun processCommand(text: String): CommandResult {
        // Direct implementation, no interfaces
        val command = findCommand(text)
        return executeCommand(command)
    }

    private fun findCommand(text: String): Command? {
        return commands.firstOrNull { it.matches(text) }
    }
}
```

### ❌ WRONG Patterns

**Creating Adapters or Interfaces:**
```kotlin
// WRONG - Creating adapter
val engineConfig = config.toEngineConfig()

// WRONG - Interface abstraction
interface IModule {
    fun initialize()
    fun process()
}

// WRONG - Helper methods
fun getParameter(name: String) = parameters[name]
fun setParameter(name: String, value: Any) {
    parameters[name] = value
}
```

## Module Structure Pattern

### Standard Module Organization
```
/modules/managers/CommandsManager/
├── build.gradle.kts
├── src/
│   └── main/
│       ├── AndroidManifest.xml
│       └── java/
│           └── com/
│               └── ai/
│                   └── commandsmanager/  # Note: lowercase
│                       ├── CommandsManager.kt
│                       ├── actions/
│                       │   ├── NavigationActions.kt
│                       │   └── SystemActions.kt
│                       ├── models/
│                       │   └── Command.kt
│                       └── processor/
│                           └── CommandProcessor.kt
```

### Package Naming Rules
```kotlin
// ✅ CORRECT - Clean namespace
package com.ai.commandsmanager
package com.ai.speechrecognition
package com.ai.voiceaccessibility

// ❌ WRONG - Redundant paths
package com.ai.commands.manager  // NO!
package com.ai.modules.speech   // NO!
```

## Database Implementation (Room with Hybrid Storage)

### Entity Definition
```kotlin
@Entity(tableName = "recognition_results")
data class RecognitionResult(
    @PrimaryKey @ColumnInfo(name = "id") val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "text") val text: String = "",
    @ColumnInfo(name = "confidence") val confidence: Float = 0f,
    @ColumnInfo(name = "timestamp") val timestamp: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "engine_type") val engineType: String = ""
)
```

### DAO Definition
```kotlin
@Dao
interface CommandDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(command: Command)

    @Query("SELECT * FROM commands")
    suspend fun getAll(): List<Command>

    @Query("SELECT * FROM commands WHERE text = :text LIMIT 1")
    suspend fun findByText(text: String): Command?
}
```

### Database Definition
```kotlin
@Database(entities = [Command::class], version = 1)
abstract class CommandDatabase : RoomDatabase() {
    abstract fun commandDao(): CommandDao
}
```

### Repository Pattern with Hybrid Storage
```kotlin
class CommandRepository(
    private val dao: CommandDao,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    // In-memory cache for fast O(1) lookups
    private val cache = ConcurrentHashMap<String, Command>()
    private var isLoaded = false

    // Lazy load from Room on first access
    suspend fun loadCache() = withContext(dispatcher) {
        if (!isLoaded) {
            val commands = dao.getAll()
            commands.forEach { cache[it.id] = it }
            isLoaded = true
        }
    }

    suspend fun insert(command: Command) = withContext(dispatcher) {
        dao.insert(command)
        cache[command.id] = command
    }

    suspend fun getAll(): List<Command> = cache.values.toList()

    suspend fun findByText(text: String): Command? =
        cache.values.firstOrNull { it.text == text }
}
```

## Coroutine Patterns

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

## Error Handling Patterns

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

## Common Compilation Fixes

### Null Safety
```kotlin
// Common null safety patterns
view.findViewById<TextView>(R.id.text)?.text = "Hello"
bounds?.let { rect ->
    // Use rect safely
}

// Elvis operator for defaults
val className = node.className?.toString() ?: ""
```

### When Expressions
```kotlin
// Must be exhaustive
when (action) {
    is NavigationAction -> handleNavigation(action)
    is SystemAction -> handleSystem(action)
    is CustomAction -> handleCustom(action)
    // No else needed if all cases covered
}
```

## Build Commands

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

## Performance Optimization Patterns

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

## Testing Patterns

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

---

**Note:** Always refer to MASTER-STANDARDS.md for core principles before applying these patterns.
