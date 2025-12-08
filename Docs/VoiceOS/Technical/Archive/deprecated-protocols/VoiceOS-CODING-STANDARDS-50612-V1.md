# ⚠️ DEPRECATED - This file has been superseded

**Status:** DEPRECATED as of 2025-10-15
**New Location:** `/Volumes/M Drive/Coding/Warp/Agent-Instructions/VOS4-CODING-PROTOCOL.md`
**Reason:** Consolidated into VOS4-CODING-PROTOCOL.md
**Archived By:** Documentation Consolidation Agent

This file is kept for historical reference only. DO NOT use for new development.

---

[Original content below]

# VOS4 Coding Standards & Universal Instructions
**Author:** Manoj Jhawar
**Code-Reviewed-By:** CCA
**Last Modified:** 2025-09-03 - Added mandatory specialized agents & parallel processing requirements
**Version:** 1.2.0

## CRITICAL: Documentation Standards
**ALWAYS consult `/docs/DOCUMENT-CONTROL-MASTER.md` before creating or updating ANY documentation.**
- This master file contains the document registry, naming conventions, and locations
- See `/Agent-Instructions/DOCUMENT-STANDARDS.md` for detailed documentation rules

## CRITICAL: Modular Architecture - Self-Contained Modules
**Each module MUST be completely self-contained with ALL related components in the same module.**

### Module Self-Containment Rules (MANDATORY)
- **Android Components**: Services, activities, receivers declared in same module as implementation classes
- **Resources**: Strings, XML configs, drawables in same module that references them
- **Permissions**: Required permissions declared in module that needs them
- **Dependencies**: Minimize cross-module dependencies
- **Testing**: Each module should be buildable and testable independently

### ✅ CORRECT - Self-contained module structure:
```
/modules/apps/VoiceAccessibility/
├── AndroidManifest.xml          # Service declarations
├── src/main/java/com/ai/        # Implementation classes
├── res/values/strings.xml       # Module-specific strings
├── res/xml/config.xml           # Module-specific configs
└── All related functionality
```

### ❌ WRONG - Split responsibilities:
```
- Service declared in main app manifest
- Implementation classes in VoiceAccessibility module
- Resources scattered across modules
- Cross-module resource dependencies
```

### Key Benefits of Self-Contained Modules:
- **Independent Building**: Each module builds without external resource dependencies
- **Clear Ownership**: No confusion about where components belong
- **Easy Debugging**: All related code and resources in one location
- **Simple Testing**: Module can be tested in isolation
- **Maintainability**: Changes don't create unexpected cross-module issues

## MANDATORY: Class Implementation Decision Framework
**Before creating ANY new class, evaluate these criteria:**

### Decision Criteria (MUST evaluate in order):
1. **Is the class required?** - Can existing classes handle this functionality?
2. **Does it add complexity or intermediate steps?** - Will it create unnecessary hops?
3. **Will it lead to issues requiring multiple updates?** - Will changes cascade across multiple files?
4. **Is there an easier cleaner way?** - Can direct access or delegation work better?

### Decision Process:
- If answer to #1 is NO → Don't create the class
- If answer to #2 or #3 is YES → Look for alternatives
- If answer to #4 is YES → Use the simpler approach
- If unclear → Ask user for decision

## MANDATORY: Interactive Development Process
**When proposing changes, optimizations, or implementations:**

### Presentation Format:
1. **First: Present ALL options at once** - Complete list with pros/cons
2. **Include for each option:**
   - Description of the change
   - Pros (benefits)
   - Cons (drawbacks)
   - Performance/memory impact
   - Implementation complexity
3. **Provide recommendation** with clear reasoning

### Interactive Decision Process:
1. **After presenting all options** → Wait for user to read
2. **Ask about ONE option at a time** → "Should we proceed with Option 1: [specific change]?"
3. **Wait for user decision** before moving to next
4. **Document decisions** in code comments/headers

### MANDATORY: Human-Readable Permission Requests
When asking for permission to execute actions:
1. **Provide detailed explanation** in plain language
2. **Show exactly what will happen** with specific examples
3. **List all files affected** with full paths
4. **Explain the impact** of each change
5. **Present alternatives** if applicable

### MANDATORY: Question Presentation Format
When multiple decisions are needed:
1. **LIST ALL QUESTIONS at once** with numbers
2. **THEN ask for ONE answer at a time** starting with Question 1
3. **For each question provide:**
   - Options (if applicable)
   - Recommendation with reasoning
   - Impact/consequences of each option

**Example Format:**
```
Here are all the decisions needed:
1. Should I move 9 documentation files to docs/ folders?
2. Should I delete 8 duplicate Kotlin files?
3. What should we do with CursorSystemMigrationGuide.kt?

Let's start with Question 1:
Should I move 9 documentation files to docs/ folders?

Options:
A) Yes - Move all files with proper naming
B) No - Keep them where they are
C) Partial - Only move some files

Recommendation: Option A
Why: Follows VOS4 documentation standards, improves organization,
consistent with other modules

Impact: Better organization, easier to find docs, follows standards
```

### Example:
```
"Here are all optimization opportunities:
1. Event-driven updates - 90% CPU reduction
2. Memory optimization - 60% heap reduction
3. Caching strategy - 95% faster lookups
[full details...]

Let's start with Option 1: Should we implement event-driven updates using BroadcastReceiver?"
```

## AI Review Patterns (CRT, COT, ROT, TOT)

### Quick Reference:
- **COT** = Chain of Thought - Linear step-by-step reasoning
- **ROT** = Reflection - Evaluation and self-assessment
- **TOT** = Train/Tree of Thought - Explore multiple paths
- **CRT** = Combined (COT+ROT+TOT) - Full analysis with options

### Usage in Code Review:
- **Simple changes:** Use COT for reasoning
- **Complex refactoring:** Use CRT for full analysis
- **Performance critical:** Use CRT-P (performance focus)
- **Architecture changes:** Always use CRT

### CRT Response Format:
1. Chain of Thought analysis
2. Reflection on approach
3. Alternative paths explored
4. Options with pros/cons
5. Clear recommendation
6. Request for approval

See [AI-REVIEW-ABBREVIATIONS.md](./AI-REVIEW-ABBREVIATIONS.md) for complete patterns

### Example Application:
```kotlin
// ❌ BAD - Unnecessary orchestrator class
class CommandOrchestrator {
    fun process(cmd: String) {
        processor.process(cmd)  // Just forwards - adds no value
    }
}

// ✅ GOOD - Direct access
class Service {
    private val processor: CommandProcessor

    fun process(cmd: String) {
        processor.process(cmd)  // Direct call, no intermediary
    }
}
```

## 🚀 MANDATORY: Specialized Agents & Parallel Processing

### When to Use Multiple Specialized Agents (REQUIRED):
1. **Phase Transitions** - Deploy agents for each subphase in parallel
2. **Independent Tasks** - Run non-dependent tasks simultaneously
3. **Analysis & Implementation** - Analyze next phase while implementing current
4. **Documentation Updates** - Update different docs in parallel
5. **Testing & Development** - Test completed work while developing next features

### Parallel Execution Rules:
- **ALWAYS** use parallel agents when tasks are independent
- **ALWAYS** use specialized agents for their domain (coding, testing, docs)
- **MAXIMIZE** throughput by running multiple subphases in parallel
- **Example**: While testing Phase 1.1c, start analyzing Phase 1.2a

### Sequential Execution (When Required):
- Same file modifications (avoid conflicts)
- Dependent tasks (output feeds input)
- Critical path items (order matters)

### Efficiency Targets:
- Phase 0: Reduced from 1 week to 45 minutes using parallel agents
- Aim for 60-80% time reduction through parallelization
- Deploy 3-5 specialized agents when possible

### Autonomous Execution:
- Continue through phases without waiting for approval UNLESS:
  - Architectural decisions needed
  - Unclear requirements
  - Errors that block progress
  - Explicitly told to wait
- Update tracking documents continuously
- Commit after each subphase completion

### Agent Assignment Pattern
For any development task, assign AT MINIMUM 3 specialized agents:
1. **Analysis Agent** - Examine existing code and requirements
2. **Implementation Agent** - Execute the actual changes
3. **Verification Agent** - Validate correctness and completeness

### Common Agent Types
- **Architecture Agent** - Design system structure
- **Migration Agent** - Port code between systems
- **Build Configuration Agent** - Handle Gradle/Maven setup
- **Testing Agent** - Create and run tests
- **Documentation Agent** - Update docs and comments
- **Cleanup Agent** - Fix redundancies and organize
- **API Design Agent** - Design public interfaces
- **Integration Agent** - Connect components

### Example Task Distribution
```
Task: Create new library
- Agent 1: Architecture Agent - Design structure
- Agent 2: Implementation Agent - Write code
- Agent 3: Build Agent - Configure Gradle
- Agent 4: Testing Agent - Create tests
- Agent 5: Documentation Agent - Write docs
```

## MANDATORY: Database Standard
**ALL VOS4 modules MUST use Room for local data persistence.**

### Room Configuration
```kotlin
// build.gradle.kts
plugins {
    id("com.google.devtools.ksp") version "1.9.25-1.0.20"
}

dependencies {
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
}
```

### Hybrid Storage Pattern (Room + In-Memory)
**VOS4 Standard**: Combine Room persistence with in-memory caching for performance.

```kotlin
// Entity
@Entity(tableName = "your_entities")
data class YourEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

// DAO
@Dao
interface YourDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: YourEntity)

    @Query("SELECT * FROM your_entities")
    suspend fun getAll(): List<YourEntity>

    @Query("SELECT * FROM your_entities WHERE id = :id")
    suspend fun getById(id: String): YourEntity?
}

// Database
@Database(entities = [YourEntity::class], version = 1)
abstract class YourDatabase : RoomDatabase() {
    abstract fun yourDao(): YourDao
}

// Repository with Hybrid Storage
class YourRepository(
    private val dao: YourDao,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    // In-memory cache for fast O(1) lookups
    private val cache = ConcurrentHashMap<String, YourEntity>()
    private var isLoaded = false

    // Lazy load from Room on first access
    suspend fun loadCache() = withContext(dispatcher) {
        if (!isLoaded) {
            val entities = dao.getAll()
            entities.forEach { cache[it.id] = it }
            isLoaded = true
        }
    }

    suspend fun insert(entity: YourEntity) = withContext(dispatcher) {
        dao.insert(entity)
        cache[entity.id] = entity
    }

    fun getById(id: String): YourEntity? = cache[id]

    fun getAll(): List<YourEntity> = cache.values.toList()
}
```

### Lazy Loading Pattern
```kotlin
class YourManager(private val repository: YourRepository) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        // Load database lazily on first access
        scope.launch {
            repository.loadCache()
        }
    }
}
```

## MANDATORY: Third-Party App UUID Generation
**For accessibility-based UUID generation from third-party Android apps.**

### UUID Format Standard
```
{packageName}.v{version}.{elementType}-{contentHash}

Examples:
- com.instagram.android.v12.0.0.button-a7f3e2c1d4b5
- com.twitter.android.v9.45.0.edittext-f2b8d3a6c9e1
- com.google.android.gm.v2023.01.15.button-e4a7c2d9f1b3
```

### Implementation Pattern
```kotlin
data class AccessibilityFingerprint(
    val resourceId: String?,
    val className: String?,
    val text: String?,
    val contentDescription: String?,
    val hierarchyPath: String,
    val packageName: String,
    val appVersion: String
)

class ThirdPartyUuidGenerator(
    private val contentHasher: ContentHasher,
    private val packageVersionResolver: PackageVersionResolver
) {
    suspend fun generateUuid(node: AccessibilityNodeInfo): String {
        val packageName = node.packageName?.toString()
            ?: throw IllegalArgumentException("Node has no package name")

        val fingerprint = extractFingerprint(node, packageName)
        val version = packageVersionResolver.getVersion(packageName)
        val contentHash = contentHasher.generateHash(fingerprint)
        val elementType = getElementType(node)

        return formatThirdPartyUuid(
            packageName = packageName,
            version = version,
            elementType = elementType,
            contentHash = contentHash
        )
    }

    private fun extractFingerprint(node: AccessibilityNodeInfo, packageName: String): AccessibilityFingerprint {
        return AccessibilityFingerprint(
            resourceId = node.viewIdResourceName,
            className = node.className?.toString(),
            text = node.text?.toString(),
            contentDescription = node.contentDescription?.toString(),
            hierarchyPath = calculateHierarchyPath(node),
            packageName = packageName,
            appVersion = packageVersionResolver.getVersion(packageName)
        )
    }
}
```

### Content Hashing Standard
```kotlin
class ContentHasher {
    fun generateHash(fingerprint: AccessibilityFingerprint): String {
        val content = buildString {
            append(fingerprint.resourceId ?: "")
            append(fingerprint.className ?: "")
            append(fingerprint.text ?: "")
            append(fingerprint.contentDescription ?: "")
            append(fingerprint.hierarchyPath)
        }

        val bytes = MessageDigest.getInstance("SHA-256").digest(content.toByteArray())
        return bytes.take(6).joinToString("") { "%02x".format(it) }
    }
}
```

## MANDATORY: UUID Analytics Tracking
**All UUID systems MUST track analytics for usage patterns and optimization.**

### Analytics Data Model
```kotlin
@Entity(tableName = "uuid_analytics")
data class UuidAnalytics(
    @PrimaryKey @ColumnInfo(name = "uuid") val uuid: String,
    @ColumnInfo(name = "access_count") val accessCount: Long = 0,
    @ColumnInfo(name = "first_accessed") val firstAccessed: Long,
    @ColumnInfo(name = "last_accessed") val lastAccessed: Long,
    @ColumnInfo(name = "execution_time_ms") val executionTimeMs: Long = 0,
    @ColumnInfo(name = "success_count") val successCount: Long = 0,
    @ColumnInfo(name = "failure_count") val failureCount: Long = 0,
    @ColumnInfo(name = "lifecycle_state") val lifecycleState: String // created, active, deprecated, deleted
)
```

### Analytics Tracking Pattern
```kotlin
class UuidAnalyticsTracker(private val dao: UuidAnalyticsDao) {
    suspend fun recordAccess(uuid: String, executionTimeMs: Long, success: Boolean) {
        val current = dao.getByUuid(uuid) ?: UuidAnalytics(
            uuid = uuid,
            firstAccessed = System.currentTimeMillis(),
            lastAccessed = System.currentTimeMillis()
        )

        dao.upsert(
            current.copy(
                accessCount = current.accessCount + 1,
                lastAccessed = System.currentTimeMillis(),
                executionTimeMs = current.executionTimeMs + executionTimeMs,
                successCount = if (success) current.successCount + 1 else current.successCount,
                failureCount = if (!success) current.failureCount + 1 else current.failureCount
            )
        )
    }

    suspend fun getMostUsed(limit: Int = 10): List<UuidAnalytics> {
        return dao.getMostUsed(limit)
    }

    suspend fun getLeastUsed(limit: Int = 10): List<UuidAnalytics> {
        return dao.getLeastUsed(limit)
    }
}
```

## MANDATORY: Custom UUID Formats
**Support user-defined UUID prefixes for organizational purposes.**

### Custom Format Pattern
```kotlin
sealed class UuidFormat {
    data object Standard : UuidFormat() // 550e8400-e29b-41d4-a716-446655440000
    data class Prefixed(val prefix: String) : UuidFormat() // btn-550e8400-e29b-41d4-a716-446655440000
    data class ThirdParty(val packageName: String, val version: String) : UuidFormat() // com.app.v1.0.button-abc123
}

class CustomUuidGenerator {
    fun generate(format: UuidFormat): String = when (format) {
        is UuidFormat.Standard -> UUID.randomUUID().toString()
        is UuidFormat.Prefixed -> "${format.prefix}-${UUID.randomUUID()}"
        is UuidFormat.ThirdParty -> {
            // Use ThirdPartyUuidGenerator for this format
            throw IllegalArgumentException("Use ThirdPartyUuidGenerator for third-party format")
        }
    }
}
```

### Hierarchical UUID Pattern
```kotlin
@Entity(tableName = "uuid_hierarchy")
data class UuidHierarchy(
    @PrimaryKey @ColumnInfo(name = "uuid") val uuid: String,
    @ColumnInfo(name = "parent_uuid") val parentUuid: String?,
    @ColumnInfo(name = "depth") val depth: Int = 0,
    @ColumnInfo(name = "path") val path: String // /root/parent/child format
)

class HierarchyManager(private val dao: UuidHierarchyDao) {
    suspend fun registerWithParent(uuid: String, parentUuid: String?) {
        val parent = parentUuid?.let { dao.getByUuid(it) }
        val depth = (parent?.depth ?: -1) + 1
        val path = if (parent != null) "${parent.path}/$uuid" else "/$uuid"

        dao.insert(UuidHierarchy(uuid, parentUuid, depth, path))
    }

    suspend fun getChildren(parentUuid: String): List<UuidHierarchy> {
        return dao.getChildren(parentUuid)
    }

    suspend fun getAncestors(uuid: String): List<UuidHierarchy> {
        val hierarchy = dao.getByUuid(uuid) ?: return emptyList()
        return hierarchy.path.split("/")
            .filter { it.isNotEmpty() && it != uuid }
            .mapNotNull { dao.getByUuid(it) }
    }
}
```

## Package Naming Standards
- **Base Pattern:** `com.ai.[modulename]` (ai = Augmentalis Inc)
- **Maven Group ID:** `com.augmentalis` (company domain)
- **Standalone Apps:**
  - VoiceAccessibility: `com.ai.voiceaccessibility`
  - SpeechRecognition: `com.ai.speechrecognition`
  - VoiceUI: `com.ai.voiceui`
  - DeviceMGR: `com.ai.devicemgr.*`
    - Audio: `com.ai.devicemgr.audio`
    - Display: `com.ai.devicemgr.display`
    - IMU: `com.ai.devicemgr.imu`
    - Device Info: `com.ai.devicemgr.info`
- **System Managers:**
  - ~~CoreMGR: `com.ai.coremgr`~~ (REMOVED - Use direct access instead)
  - CommandsMGR: `com.ai.commandsmgr`
  - DataMGR: `com.ai.datamgr`
  - GlassesMGR: `com.ai.glassesmgr`
  - LocalizationMGR: `com.ai.localizationmgr`
  - LicenseMGR: `com.ai.licensemgr`
- **Libraries:**
  - UUIDManager: `com.ai.uuidmgr`
  - VoiceUIElements: `com.ai.voiceuielements`
- **IMPORTANT:** Always confirm namespace with team before creating new ones
- **Avoid deep nesting** - max 3 levels deep (e.g., com.ai.devicemgr.audio)
- **Module Independence:** Each module should be self-contained where possible

## Class Naming Standards
### VOS/Vos Prefix Usage
**DO NOT use VOS/Vos prefix by default.** Only use it when:
1. **Clarity is needed** - When the class name would be ambiguous without it
2. **Naming conflicts** - To avoid conflicts with Android OS or manufacturer classes
3. **Legacy compatibility** - When maintaining compatibility with existing VOS3 code

### Examples:
```kotlin
// ✅ GOOD - Clear names without unnecessary prefix
class CursorView        // Not VosCursorView
class AudioManager      // Not VosAudioManager
class CommandProcessor  // Not VosCommandProcessor

// ✅ GOOD - VOS prefix used for clarity/conflicts
class VosDataManager    // Distinguishes from Android's DataManager
class VosDisplayManager // Avoids conflict with Android's DisplayManager
class VosService        // Legacy compatibility with VOS3

// ❌ BAD - Unnecessary VOS prefix
class VosCursorHelper   // Should be CursorHelper
class VosMenuItem       // Should be MenuItem
class VosButton         // Should be Button
```

## Directory Structure Standards
**AVOID REDUNDANCY in folder structures:**
- ❌ BAD: `/modules/managers/CommandsMGR/commands/` (redundant)
- ✅ GOOD: `/modules/managers/CommandsMGR/` (files directly here)
- Keep folder names concise - the context is already clear

## Documentation Standards
- **Naming Convention:** All documentation files follow: `Module-Purpose.md`
  - Examples: `SRSTT-Architecture.md`, `Core-API.md`, `Database-Schema.md`
- **Document Headers:** Every .md file must include:
  ```markdown
  # [Module] - [Purpose]
  **Module:** [Module Name]
  **Author:** Manoj Jhawar
  **Created:** YYMMDD
  **Last Updated:** YYMMDD

  ## Changelog
  - YYMMDD: Initial creation
  - YYMMDD: [Description of changes]
  ```

## Code Style Requirements
- **File Headers:** Every code file must include:
  ```kotlin
  // File: [full path]
  // Author: Manoj Jhawar
  // Code-Reviewed-By: CCA
  ```

- **Module Structure:** Follow VOS4 modular architecture
- **Memory Optimization:** Target <30MB (Vosk) or <60MB (Vivoka)
- **Error Handling:** Comprehensive try-catch with logging
- **Coroutines:** Use structured concurrency
- **EventBus:** Use for inter-module communication

## Migration Standards
**When porting code:**
1. **PORT EXACT FUNCTIONALITY** - Don't create new theoretical implementations
2. **PRESERVE WORKING CODE** - If it works, don't reinvent it
3. **UPDATE ONLY WHAT'S NECESSARY** - Package names, imports, dependencies
4. **TEST AFTER PORTING** - Ensure functionality is preserved

## Testing Requirements
- Unit test coverage >80%
- Repository pattern for all data access
- Mock external dependencies
- Performance benchmarking for critical paths

## Security Requirements
- No audio storage without explicit permission
- Encrypt sensitive data (license keys)
- Local-first processing
- Clear data deletion capabilities

## Build Configuration
- **Maven Publishing:** Use `com.augmentalis` as group ID
- **AAR/JAR Export:** All libraries should support both
- **Standalone Apps:** Should also function as libraries where applicable

## Version Control

### Commit Message Format
```
[type]: Brief description (50 chars max)

- Detailed bullet points of changes
- Each change on its own line
- Focus on what and why, not how

Affected modules: Module1, Module2
```

### Commit Types
- `feat:` New feature
- `fix:` Bug fix
- `refactor:` Code restructuring
- `docs:` Documentation changes
- `test:` Test additions/changes
- `chore:` Build/config changes

### Commit Rules
- **NO AI ATTRIBUTION**: Do not mention AI tools, Claude, or code generation
- **Author Attribution**: Use Manoj Jhawar as author
- **Clear Purpose**: Explain the business/technical reason
- **Module Impact**: List affected modules
- **Branch Strategy:** Feature branches for major changes
- **Code Review:** All code must be reviewed before merge

### Example Commit
```
refactor: Remove CoreManager for direct access pattern

- Eliminated service locator anti-pattern
- Implemented direct property access via Application
- Updated all modules to use constructor injection
- Removed runtime lookups for compile-time safety

Affected modules: Application, all managers, all apps
```
