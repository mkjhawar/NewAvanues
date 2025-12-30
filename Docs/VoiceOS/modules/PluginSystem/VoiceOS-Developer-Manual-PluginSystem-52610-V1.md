# PluginSystem Developer Manual

**Module**: PluginSystem
**Type**: Android Library (Kotlin)
**Package**: `com.augmentalis.magiccode.plugins.*`
**Created**: 2025-10-26 11:46 PDT
**Copyright**: Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC

---

## Table of Contents

1. [Overview & Architecture](#overview--architecture)
2. [Package-by-Package Deep Dive](#package-by-package-deep-dive)
3. [Integration Patterns](#integration-patterns)
4. [Common Use Cases](#common-use-cases)
5. [Pitfalls & Gotchas](#pitfalls--gotchas)
6. [Design Decisions](#design-decisions)
7. [Code Examples](#code-examples)
8. [Testing Guide](#testing-guide)
9. [Performance Considerations](#performance-considerations)
10. [Security Considerations](#security-considerations)

---

## Overview & Architecture

### What is PluginSystem?

The PluginSystem module provides a comprehensive plugin infrastructure for VOS4 (VoiceOS), enabling third-party developers to extend VOS4's capabilities through installable plugins. The system was originally part of MagicCode (a multiplatform KMP framework supporting iOS, JVM, and Android) but has been **simplified to Android-only** for VOS4 integration.

### Why Does PluginSystem Exist?

**Extensibility**: VOS4 is an Android accessibility service with voice control capabilities. The plugin system allows third-party developers to:
- Add custom voice commands
- Integrate with the accessibility service
- Customize cursor behavior
- Extend speech recognition capabilities
- Provide themes and UI customizations

**Separation of Concerns**: Core VOS4 functionality remains stable while plugins provide experimental or specialized features without requiring core updates.

**Ecosystem Growth**: Enable a marketplace (AppAvenue Store) where users can discover and install plugins to customize their VOS4 experience.

### Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                     VOS4 Application                         │
│  ┌────────────────────────────────────────────────────────┐ │
│  │              Plugin System Core                        │ │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐ │ │
│  │  │ PluginLoader │←→│ PluginRegistry│←→│PluginManifest│ │ │
│  │  └──────────────┘  └──────────────┘  └──────────────┘ │ │
│  └────────────────────────────────────────────────────────┘ │
│                            ↕                                 │
│  ┌────────────────────────────────────────────────────────┐ │
│  │         Plugin Subsystems (8 packages)                 │ │
│  │  • Assets (resolution, caching)                        │ │
│  │  • Security (permissions, signatures)                  │ │
│  │  • Dependencies (semver, resolution)                   │ │
│  │  • Themes (management, validation)                     │ │
│  │  • Database (Room persistence)                         │ │
│  │  • Platform (file I/O, class loading)                  │ │
│  │  • Distribution (installation)                         │ │
│  │  • Transactions (checkpoints, rollback)                │ │
│  └────────────────────────────────────────────────────────┘ │
│                            ↕                                 │
│  ┌────────────────────────────────────────────────────────┐ │
│  │         VOS4 Plugin Interfaces (3 types)               │ │
│  │  • AccessibilityPluginInterface                        │ │
│  │  • CursorPluginInterface                               │ │
│  │  • SpeechEnginePluginInterface                         │ │
│  └────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
                            ↕
         ┌─────────────────────────────────────┐
         │   Third-Party Plugin Packages       │
         │   (APK/JAR with plugin.yaml)        │
         └─────────────────────────────────────┘
```

### Key Components

1. **PluginLoader** (`core/PluginLoader.kt`)
   - Orchestrates the 8-step plugin loading lifecycle
   - Reads and validates plugin.yaml manifests
   - Creates isolated namespaces
   - Loads plugin classes via platform-specific ClassLoader
   - Registers plugins in the registry

2. **PluginRegistry** (`core/PluginRegistry.kt`)
   - Central in-memory index of all installed plugins
   - Thread-safe with mutex synchronization
   - Multiple indexes for efficient lookups (by state, source, verification level)
   - Optional persistence via Room database

3. **PluginManifest** (`core/PluginManifest.kt`)
   - YAML-based plugin metadata (plugin.yaml)
   - Defines ID, version, dependencies, permissions, assets
   - Validated by ManifestValidator against schema rules

4. **AssetResolver** (`assets/AssetResolver.kt`)
   - Resolves plugin asset URIs to platform file handles
   - Caching for performance
   - Fallback support for missing assets
   - Security validation (namespace boundaries)

5. **PermissionManager** (`security/PermissionManager.kt`)
   - Manages plugin permission requests
   - Platform-specific UI dialogs
   - Persistent storage of permission grants
   - "Don't ask again" support

6. **Room Database** (`database/PluginDatabase.kt`)
   - Persists plugin metadata across app restarts
   - 4 entities: PluginEntity, DependencyEntity, PermissionEntity, CheckpointEntity
   - DAOs for efficient querying

---

## Package-by-Package Deep Dive

### 1. Core Package (`com.augmentalis.magiccode.plugins.core`)

**Location**: `src/commonMain/kotlin/com/augmentalis/magiccode/plugins/core/`
**Purpose**: Foundation of the plugin system - loading, registration, validation

#### Files (11 files):

| File | Purpose | Key Concepts |
|------|---------|-------------|
| `PluginLoader.kt` | Plugin loading orchestration | 8-step lifecycle, manifest parsing, namespace creation |
| `PluginRegistry.kt` | Plugin state management | Thread-safe registry, multiple indexes, persistence |
| `PluginManifest.kt` | Plugin metadata model | YAML serialization, dependencies, permissions |
| `PluginEnums.kt` | Core enumerations | PluginState, Permission, GrantStatus, AssetCategory |
| `PluginConfig.kt` | Configuration settings | Load timeout, cache size, validation rules |
| `PluginLogger.kt` | Logging abstraction | Cross-platform logging (expect/actual pattern) |
| `PluginNamespace.kt` | Plugin isolation | Sandboxed directories, resource access control |
| `PluginException.kt` | Exception hierarchy | Typed exceptions for error handling |
| `PluginErrorHandler.kt` | Error handling utilities | Categorized errors, recovery strategies |
| `PluginRequirementValidator.kt` | Functional requirement validation | FR-001, FR-002, FR-018 compliance |
| `ManifestValidator.kt` | Manifest schema validation | ID format, version semver, entrypoint validation |

#### Deep Dive: PluginLoader

**File**: `PluginLoader.kt:8-448`

**The 8-Step Loading Lifecycle**:

```kotlin
// Step 1: Read and parse manifest
val manifestContent = readManifestFile(manifestPath)  // Line 143
val manifest = parseManifest(manifestContent)         // Line 151

// Step 2: Validate manifest schema
validator.validate(manifest)                          // Line 164

// Step 2b: Validate functional requirements (FR-001, FR-002, FR-018)
requirementValidator.validateAll(pluginRoot, manifest) // Line 180

// Step 3: Check for ID conflicts
if (registry.isRegistered(pluginId)) { error }        // Line 195

// Step 4: Create namespace
val namespace = PluginNamespace.create(pluginId, appDataDir) // Line 209

// Step 5: Validate directory structure
validateDirectoryStructure(manifestPath, libraryPath, manifest) // Line 218

// Step 6: Load plugin class (Android-specific)
classLoader.loadClass(manifest.entrypoint, libraryPath) // Line 228

// Step 7: Register plugin
registry.register(manifest, namespace)                 // Line 238

// Step 8: Return success
return LoadResult.Success(pluginInfo)                  // Line 253
```

**WHY this design?**
- **Early validation**: Catches errors before resource allocation
- **Namespace isolation**: Security boundary prevents unauthorized file access
- **Atomic operation**: Either succeeds completely or fails cleanly
- **Extensible**: Easy to add validation steps without breaking existing code

**Common Pitfall**: Plugin ID mismatch
```kotlin
// WRONG: pluginId parameter doesn't match manifest
loader.loadPlugin(
    pluginId = "com.example.myplugin",  // Parameter
    manifestPath = "/path/plugin.yaml"   // Contains id: "com.other.plugin"
)
// Result: PluginException at line 155-161

// CORRECT: Must match
loader.loadPlugin(
    pluginId = "com.example.myplugin",  // Same
    manifestPath = "/path/plugin.yaml"   // Contains id: "com.example.myplugin"
)
```

#### Deep Dive: PluginRegistry

**File**: `PluginRegistry.kt:8-475`

**Thread Safety Architecture**:
```kotlin
class PluginRegistry {
    private val mutex = Mutex()                                    // Line 51
    private val plugins = mutableMapOf<String, PluginInfo>()      // Line 52

    // All public methods use mutex.withLock { }
    suspend fun register(...): Boolean {
        return mutex.withLock {                                    // Line 108
            // Safe concurrent access
        }
    }
}
```

**Multiple Indexes for O(1) Lookups**:
```kotlin
// Line 54-57
private val pluginsByState = mutableMapOf<PluginState, MutableSet<String>>()
private val pluginsBySource = mutableMapOf<PluginSource, MutableSet<String>>()
private val pluginsByVerificationLevel = mutableMapOf<DeveloperVerificationLevel, MutableSet<String>>()

// Optimized queries (O(1) instead of O(n))
suspend fun getPluginsByState(state: PluginState): List<PluginInfo> {
    // Uses index, not filtering all plugins - Line 271-276
}
```

**WHY multiple indexes?**
- **Performance**: Common queries (get all active plugins) are O(1) instead of O(n)
- **Use case driven**: VOS4 needs to quickly find plugins by state/source
- **Trade-off**: Memory overhead for speed (acceptable for ~100s of plugins)

**Gotcha: Persistence is optional**
```kotlin
// With persistence
val registry = PluginRegistry(persistence = RoomPluginPersistence())
registry.register(manifest, namespace)  // Saved to database

// Without persistence (testing)
val registry = PluginRegistry(persistence = null)
registry.register(manifest, namespace)  // In-memory only!
```

#### Deep Dive: PluginManifest

**File**: `PluginManifest.kt:6-306`

**Example plugin.yaml**:
```yaml
id: com.example.hello-world
name: Hello World Plugin
version: 1.0.0
author: Your Name
description: A simple example plugin
entrypoint: com.example.helloworld.HelloWorldPlugin
capabilities:
  - ui_components
  - themes
dependencies:
  - pluginId: com.augmentalis.magicui
    version: ^1.0.0
permissions:
  - NETWORK
  - STORAGE_READ
permissionRationales:
  NETWORK: "Required to download theme updates"
  STORAGE_READ: "Needed to access user's custom themes"
source: THIRD_PARTY
verificationLevel: UNVERIFIED
assets:
  images:
    - icon.png
    - background.jpg
  themes:
    - dark-theme.yaml
homepage: https://example.com/plugin
license: MIT
```

**Validation Rules** (enforced by ManifestValidator):
- **ID**: Must match `^[a-z][a-z0-9-]*(\.[a-z][a-z0-9-]*)+$` (reverse-domain)
- **Version**: Valid semver (1.2.3, 2.0.0-beta.1)
- **Entrypoint**: Valid Kotlin class path
- **Dependencies**: Valid semver constraints (^, ~, >=, *)

**Version Constraint Formats** (Line 186-190):
```kotlin
// Exact: "1.2.3" - Exactly version 1.2.3
// Caret: "^1.2.3" - Compatible with 1.2.3 (>=1.2.3 <2.0.0)
// Tilde: "~1.2.3" - Approximately 1.2.3 (>=1.2.3 <1.3.0)
// Range: ">=1.5.0 <2.0.0" - Between 1.5.0 and 2.0.0
// Wildcard: "1.2.*" - Any patch version of 1.2
```

---

### 2. Database Package (`com.augmentalis.magiccode.database`)

**Location**: `src/androidMain/kotlin/com/augmentalis/magiccode/database/`
**Purpose**: Android Room persistence for plugin metadata
**Note**: This is **Android-only** (no iOS/JVM implementations)

#### Files (5 files):

| File | Purpose | Database Table |
|------|---------|----------------|
| `PluginEntity.kt` | Core plugin metadata | `plugins` |
| `DependencyEntity.kt` | Plugin dependencies | `plugin_dependencies` |
| `PermissionEntity.kt` | Permission grants | `plugin_permissions` |
| `CheckpointEntity.kt` | Transaction checkpoints | `system_checkpoints` |
| `PluginDatabase.kt` | Room database + DAOs | N/A (database class) |

#### Deep Dive: Room Database Architecture

**File**: `PluginDatabase.kt:3-126`

**Database Schema** (Version 1):
```kotlin
@Database(
    entities = [
        PluginEntity::class,        // Plugins table
        DependencyEntity::class,    // Dependencies table
        PermissionEntity::class,    // Permissions table
        CheckpointEntity::class     // Checkpoints table
    ],
    version = 1,
    exportSchema = true
)
abstract class PluginDatabase : RoomDatabase() {
    abstract fun pluginDao(): PluginDao
    abstract fun dependencyDao(): DependencyDao
    abstract fun permissionDao(): PermissionDao
    abstract fun checkpointDao(): CheckpointDao
}
```

**DAO Pattern**:
```kotlin
@Dao
interface PluginDao {
    @Query("SELECT * FROM plugins WHERE state = :state")
    suspend fun getPluginsByState(state: PluginState): List<PluginEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlugin(plugin: PluginEntity)
}
```

**WHY Room?**
- **Type-safe SQL**: Compile-time query validation
- **Coroutines support**: Suspending functions for async operations
- **Migration support**: Schema versioning for future updates
- **LiveData/Flow**: Reactive queries (not yet used but available)

**Database Location**:
```
/data/data/com.augmentalis.voiceoscore/databases/plugin_system.db
```

**Type Converters** (Line 71-90):
```kotlin
class PluginConverters {
    @TypeConverter
    fun fromPluginSource(value: PluginSource): String = value.name

    @TypeConverter
    fun toPluginSource(value: String): PluginSource = PluginSource.valueOf(value)
}
```
Required because Room doesn't natively support Kotlin enums.

---

### 3. VOS4 Interfaces Package (`com.augmentalis.magiccode.plugins.vos4`)

**Location**: `src/androidMain/kotlin/com/augmentalis/magiccode/plugins/vos4/`
**Purpose**: VOS4-specific plugin interfaces for accessibility, cursor, and speech
**Note**: These are **Android-only** and unique to VOS4

#### Files (3 files):

| File | Purpose | Plugin Type |
|------|---------|-------------|
| `AccessibilityPluginInterface.kt` | Accessibility service integration | Accessibility plugins |
| `CursorPluginInterface.kt` | Cursor control extensions | Cursor mode plugins |
| `SpeechEnginePluginInterface.kt` | Speech recognition engines | TTS/STT plugins |

#### Deep Dive: AccessibilityPluginInterface

**File**: `AccessibilityPluginInterface.kt:1-124`

**Interface Design**:
```kotlin
interface AccessibilityPluginInterface {
    val manifest: PluginManifest

    // React to accessibility events
    fun onAccessibilityEvent(event: AccessibilityEvent, rootNode: AccessibilityNodeInfo?): Boolean

    // Provide custom voice commands
    fun provideCommands(): List<VoiceCommandDefinition>

    // Execute voice commands
    suspend fun executeCommand(commandId: String, parameters: Map<String, Any>?): CommandResult

    // Lifecycle callbacks
    fun onPluginInitialized()
    fun onPluginDisabled()
}
```

**Example Implementation**:
```kotlin
class MyAccessibilityPlugin : AccessibilityPluginInterface {
    override val manifest = PluginManifest(
        id = "com.example.reader",
        name = "Smart Reader",
        // ... other fields
    )

    override fun onAccessibilityEvent(event: AccessibilityEvent, rootNode: AccessibilityNodeInfo?): Boolean {
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_FOCUSED) {
            // Custom logic for focus changes
            val text = rootNode?.text
            speakText(text)
            return true  // Event handled
        }
        return false  // Pass to next plugin
    }

    override fun provideCommands(): List<VoiceCommandDefinition> {
        return listOf(
            VoiceCommandDefinition(
                id = "read_screen",
                primaryText = "read screen",
                synonyms = listOf("read this", "what's on screen"),
                description = "Read all text on current screen"
            )
        )
    }

    override suspend fun executeCommand(commandId: String, parameters: Map<String, Any>?): CommandResult {
        return when (commandId) {
            "read_screen" -> {
                val text = getAllTextFromScreen()
                speakText(text)
                CommandResult.success("Reading screen")
            }
            else -> CommandResult.failure("Unknown command")
        }
    }
}
```

**WHY separate interfaces?**
- **Type safety**: Each plugin type has specific methods
- **Clear contracts**: Plugin developers know exactly what to implement
- **VOS4 integration**: Maps directly to VOS4 services (VoiceAccessibility, VoiceCursor, VoiceRecognition)

---

### 4. Assets Package (`com.augmentalis.magiccode.plugins.assets`)

**Location**: `src/commonMain/kotlin/com/augmentalis/magiccode/plugins/assets/`
**Purpose**: Plugin asset resolution, caching, and fallback

#### Files (8 files):

| File | Purpose | Key Feature |
|------|---------|-------------|
| `AssetResolver.kt` | URI → file handle resolution | Cache, fallback, security |
| `AssetCache.kt` | LRU asset caching | Memory optimization |
| `AssetHandle.kt` | Platform file abstraction | Cross-platform access |
| `AssetReference.kt` | Asset URI parsing | URI validation |
| `AssetMetadata.kt` | Asset metadata | MIME type, size, checksum |
| `AssetAccessLogger.kt` | Access auditing | Security monitoring |
| `ChecksumCalculator.kt` | File integrity | MD5/SHA256 |
| `FallbackAssetProvider.kt` | Default assets | Graceful degradation |

#### Deep Dive: AssetResolver

**File**: `AssetResolver.kt:6-448`

**URI Format**:
```
plugin://<plugin-id>/<category>/<path>

Examples:
plugin://com.augmentalis.theme-pack/themes/dark-theme.yaml
plugin://my.plugin/icons/app.png
plugin://example.fonts/fonts/custom-font.ttf
```

**10-Step Resolution Process** (Line 23-32):
```kotlin
// 1. Check cache (fast path)
cache.get(uri)?.let { return Success(it) }

// 2. Parse URI → AssetReference
val assetRef = AssetReference.fromUri(uri)

// 3. Verify plugin exists in registry
val pluginInfo = registry.getPlugin(assetRef.pluginId)

// 4. Validate asset within plugin namespace (FR-038 security)
namespace.validateAccess(assetPath)

// 5. Verify file exists on filesystem
fileIO.fileExists(assetPath)

// 6. Validate file extension for category
assetRef.category.isValidExtension(filename)

// 7. Extract metadata (MIME, size, checksum)
val metadata = extractAssetMetadata(assetPath, assetRef)

// 8. Create AssetHandle
val handle = AssetHandle(reference, absolutePath, metadata)

// 9. Cache the result
cache.put(uri, handle)

// 10. Log access for auditing
logAssetAccess(uri, "SUCCESS")
```

**Security: Namespace Validation** (Line 173-186):
```kotlin
// PREVENT: Path traversal attacks
// plugin://my.plugin/images/../../../etc/passwd

try {
    namespace.validateAccess(assetPath)
} catch (e: SecurityException) {
    // Asset access denied - outside plugin namespace
    return ResolutionResult.Failure(error, e)
}
```

**Fallback Strategy** (Line 277-309):
```kotlin
// If primary asset fails, try fallback
// Example: Missing icon → default icon
val fallbackPath = fallbackProvider.getFallbackAsset(category, filename)

// Fallback metadata flagged
metadata.copy(isFallback = true)
```

**Performance: Batch Resolution** (Line 257-266):
```kotlin
// Resolve multiple assets efficiently
val uris = listOf(
    "plugin://my.plugin/icons/home.png",
    "plugin://my.plugin/icons/settings.png",
    "plugin://my.plugin/themes/dark.yaml"
)
val results = resolver.resolveAssetsBatch(uris)
```

#### Deep Dive: AssetCache

**File**: `AssetCache.kt`

**LRU Eviction** (Least Recently Used):
```kotlin
// When cache full, evict oldest accessed entry
// Keeps frequently used assets in memory
```

**Cache Statistics**:
```kotlin
suspend fun getCacheStats(): Map<String, Int> {
    return mapOf(
        "size" to cache.size(),
        "capacity" to cache.capacity()
    )
}
```

---

### 5. Security Package (`com.augmentalis.magiccode.plugins.security`)

**Location**: `src/commonMain/kotlin/com/augmentalis/magiccode/plugins/security/` (common)
             `src/androidMain/kotlin/com/augmentalis/magiccode/plugins/security/` (Android)

#### Files (5 files):

| File | Purpose | Platform |
|------|---------|----------|
| `PermissionManager.kt` | Permission request lifecycle | Common |
| `PermissionUIHandler.kt` | UI dialogs for permissions | Android (expect/actual) |
| `PermissionPersistence.kt` | Permission storage interface | Common (expect/actual) |
| `PermissionStorage.kt` | Android SharedPreferences impl | Android (actual) |
| `SignatureVerifier.kt` | APK signature validation | Android (actual) |

#### Deep Dive: PermissionManager

**File**: `PermissionManager.kt:1-562`

**Permission Flow** (Line 27-33):
```
1. Plugin requests permissions → requestPermissions()
2. Manager checks cache and persistence
3. If needed, show UI dialog → PermissionUIHandler
4. User grants/denies
5. Decisions cached + persisted
6. Later checks → hasPermission() or enforcePermission()
```

**Thread Safety**:
```kotlin
class PermissionManager {
    private val mutex = Mutex()  // Line 86

    // All public methods are suspend + mutex.withLock
    suspend fun requestPermissions(...): Boolean {
        return mutex.withLock {  // Line 166
            // Safe concurrent access
        }
    }
}
```

**"Don't Ask Again" Logic** (Line 231-237):
```kotlin
// Check persistence for "don't ask again" flag
val dontAskAgain = persistence.isDontAskAgain(pluginId, permission)
if (dontAskAgain) {
    // Auto-deny without showing UI
    return@filter false
}
```

**Permission Enforcement** (Line 343-347):
```kotlin
suspend fun enforcePermission(pluginId: String, permission: Permission) {
    if (!hasPermission(pluginId, permission)) {
        throw PermissionDeniedException(pluginId, permission)
    }
}

// Usage
try {
    permissionManager.enforcePermission("com.example.plugin", Permission.NETWORK)
    // Safe to proceed with network operation
    downloadData()
} catch (e: PermissionDeniedException) {
    showError("Network access denied")
}
```

**Android UI Implementation** (PermissionUIHandler.kt):
```kotlin
actual class PermissionUIHandler(private val context: Context) {
    actual suspend fun showPermissionDialog(request: PermissionRequest): PermissionResult {
        // Uses AlertDialog with Material Design 3
        // Shows permission list with rationales
        // Returns PermissionResult with granted/denied sets
    }
}
```

**TODO: Enhanced UI** (Line 33-46):
- Custom DialogFragment for better UX
- Material Design 3 components
- Permission icons and descriptions
- Multi-choice checkboxes

---

### 6. Dependencies Package (`com.augmentalis.magiccode.plugins.dependencies`)

**Location**: `src/commonMain/kotlin/com/augmentalis/magiccode/plugins/dependencies/`

#### Files (2 files):

| File | Purpose |
|------|---------|
| `DependencyResolver.kt` | Topological sort, cycle detection |
| `SemverConstraintValidator.kt` | Semver parsing and validation |

#### Deep Dive: DependencyResolver

**File**: `DependencyResolver.kt:1-171`

**Topological Sort for Load Order** (Line 149-169):
```kotlin
// Dependencies must load before dependents
// Example: PluginB depends on PluginA → Load order: [PluginA, PluginB]

suspend fun resolveDependencies(
    pluginId: String,
    availablePlugins: Map<String, PluginManifest>
): ResolutionResult {
    // Build dependency graph
    val graph = buildDependencyGraph(pluginId, availablePlugins)

    // Detect circular dependencies (Line 45-48)
    val cycle = detectCycle(graph, pluginId)
    if (cycle.isNotEmpty()) {
        return Failure("Circular dependency detected", cycle)
    }

    // Topological sort
    val loadOrder = topologicalSort(graph, pluginId)
    return Success(loadOrder)
}
```

**Version Constraint Validation** (Line 90-97):
```kotlin
// Validate that available version satisfies constraint
if (!semverValidator.satisfies(availableVersion, requiredConstraint)) {
    throw IllegalStateException(
        "Plugin 'A' requires 'B' version '^1.0.0', " +
        "but found version '2.0.0' which does not satisfy the constraint"
    )
}
```

**Circular Dependency Detection** (DFS algorithm, Line 115-144):
```kotlin
// Example cycle: A → B → C → A
// Detection via recursion stack
// Returns path: [A, B, C, A]
```

---

### 7. Themes Package (`com.augmentalis.magiccode.plugins.themes`)

**Location**: `src/commonMain/kotlin/com/augmentalis/magiccode/plugins/themes/`

#### Files (5 files):

| File | Purpose |
|------|---------|
| `ThemeDefinition.kt` | Theme YAML structure |
| `ThemeComponents.kt` | UI component theming |
| `ThemeManager.kt` | Theme lifecycle management |
| `ThemeValidator.kt` | Theme schema validation |
| `FontLoader.kt` | Custom font loading (expect/actual) |

**Theme YAML Example**:
```yaml
id: dark-theme-v2
name: Dark Theme v2
colors:
  primary: "#BB86FC"
  secondary: "#03DAC6"
  background: "#121212"
  surface: "#1E1E1E"
  error: "#CF6679"
fonts:
  heading: "Roboto-Bold.ttf"
  body: "Roboto-Regular.ttf"
components:
  button:
    borderRadius: 8
    elevation: 2
  card:
    borderRadius: 12
    elevation: 4
```

---

### 8. Platform Package (`com.augmentalis.magiccode.plugins.platform`)

**Location**: `src/commonMain/kotlin/...` (expect declarations)
             `src/androidMain/kotlin/...` (Android actual implementations)

#### Files (3 files):

| File | Purpose | Pattern |
|------|---------|---------|
| `FileIO.kt` | File operations | expect/actual |
| `ZipExtractor.kt` | Plugin archive extraction | expect/actual |
| `PluginClassLoader.kt` | Dynamic class loading | expect/actual |

**Expect/Actual Pattern** (Kotlin Multiplatform):
```kotlin
// commonMain/FileIO.kt
expect class FileIO() {
    fun readFileAsString(path: String): String
    fun fileExists(path: String): Boolean
}

// androidMain/FileIO.kt
actual class FileIO {
    actual fun readFileAsString(path: String): String {
        return File(path).readText()  // Android-specific
    }

    actual fun fileExists(path: String): Boolean {
        return File(path).exists()
    }
}
```

**WHY expect/actual?**
- **Original design**: MagicCode supported iOS/JVM/Android
- **VOS4 simplification**: Only Android implementations exist now
- **Future-proof**: Could re-add other platforms if needed

---

### 9. Distribution Package (`com.augmentalis.magiccode.plugins.distribution`)

**Location**: `src/commonMain/kotlin/com/augmentalis/magiccode/plugins/distribution/`

#### Files (1 file):

| File | Purpose |
|------|---------|
| `PluginInstaller.kt` | Plugin installation workflow |

**Installation Steps**:
```kotlin
// 1. Download plugin archive (APK/ZIP)
// 2. Verify signature (if required)
// 3. Extract to temporary directory
// 4. Validate manifest
// 5. Check dependencies
// 6. Request permissions
// 7. Move to plugin directory
// 8. Load and register
```

---

### 10. Transactions Package (`com.augmentalis.magiccode.plugins.transactions`)

**Location**: `src/commonMain/kotlin/com/augmentalis/magiccode/plugins/transactions/`

#### Files (1 file):

| File | Purpose |
|------|---------|
| `TransactionManager.kt` | Checkpoint-based rollback |

**Checkpoint Strategy**:
```kotlin
// Before risky operation (install/update)
val checkpoint = transactionManager.createCheckpoint()

try {
    installPlugin(plugin)
} catch (e: Exception) {
    // Rollback to checkpoint
    transactionManager.rollback(checkpoint)
}
```

**Stored in Database** (CheckpointEntity):
```kotlin
@Entity(tableName = "system_checkpoints")
data class CheckpointEntity(
    val id: String,
    val createdAt: Long,
    val pluginStates: String,  // JSON snapshot
    val transactionType: TransactionType
)
```

---

## Integration Patterns

### Pattern 1: Simple Plugin Loading

```kotlin
// Initialize plugin system
val config = PluginConfig()
val registry = PluginRegistry()
val loader = PluginLoader(config, registry)

// Load a plugin
val result = loader.loadPlugin(
    pluginId = "com.example.myplugin",
    manifestPath = "/plugins/com.example.myplugin/plugin.yaml",
    libraryPath = "/plugins/com.example.myplugin/lib/plugin.jar",
    appDataDir = "/data/app"
)

when (result) {
    is LoadResult.Success -> {
        val plugin = result.pluginInfo
        println("Loaded: ${plugin.manifest.name} v${plugin.manifest.version}")
    }
    is LoadResult.Failure -> {
        println("Failed: ${result.error.message}")
    }
}
```

### Pattern 2: With Persistence

```kotlin
// Create Room database
val database = Room.databaseBuilder(
    context,
    PluginDatabase::class.java,
    "plugin_system.db"
).build()

// Create persistence adapter
val persistence = RoomPluginPersistence(database)

// Registry with persistence
val registry = PluginRegistry(persistence)

// Load persisted plugins on startup
GlobalScope.launch {
    val result = registry.loadFromPersistence()
    result.onSuccess { count ->
        println("Loaded $count plugins from database")
    }
}
```

### Pattern 3: Permission Request

```kotlin
// Initialize permission manager
val uiHandler = PermissionUIHandler(context)
val storage = PermissionStorage(context)
val manager = PermissionManager(uiHandler, storage)

// Request permissions
val granted = manager.requestPermissions(
    pluginId = "com.example.plugin",
    pluginName = "Example Plugin",
    permissions = setOf(Permission.NETWORK, Permission.STORAGE_READ),
    rationales = mapOf(
        Permission.NETWORK to "Download theme updates",
        Permission.STORAGE_READ to "Access custom themes"
    )
)

if (granted) {
    // All permissions granted
} else {
    val denied = manager.getDeniedPermissions("com.example.plugin")
    println("Denied: $denied")
}
```

### Pattern 4: Asset Resolution

```kotlin
// Initialize asset resolver
val resolver = AssetResolver(registry)

// Resolve asset
val result = resolver.resolveAsset(
    uri = "plugin://com.theme.pack/themes/dark-theme.yaml"
)

when (result) {
    is ResolutionResult.Success -> {
        val handle = result.assetHandle
        val content = handle.readAsString()
        println("Theme: $content")
    }
    is ResolutionResult.Failure -> {
        println("Failed: ${result.reason}")
    }
}
```

### Pattern 5: VOS4 Integration

```kotlin
// VoiceOSCore service
class VoiceOSService : AccessibilityService() {
    private val pluginRegistry = PluginRegistry()
    private val accessibilityPlugins = mutableListOf<AccessibilityPluginInterface>()

    override fun onServiceConnected() {
        // Load accessibility plugins
        val plugins = pluginRegistry.getPluginsByState(PluginState.ENABLED)
        plugins.forEach { pluginInfo ->
            val plugin = loadAccessibilityPlugin(pluginInfo)
            plugin.onPluginInitialized()
            accessibilityPlugins.add(plugin)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val rootNode = rootInActiveWindow

        // Dispatch to plugins
        for (plugin in accessibilityPlugins) {
            val handled = plugin.onAccessibilityEvent(event, rootNode)
            if (handled) break  // Plugin consumed event
        }
    }
}
```

---

## Common Use Cases

### Use Case 1: Creating an Accessibility Plugin

**Goal**: Add custom voice commands to VOS4

**Steps**:

1. **Create plugin manifest** (`plugin.yaml`):
```yaml
id: com.mycompany.reader
name: Smart Reader
version: 1.0.0
author: My Company
entrypoint: com.mycompany.reader.ReaderPlugin
capabilities:
  - voice_commands
  - text_to_speech
permissions:
  - ACCESSIBILITY_SERVICES
permissionRationales:
  ACCESSIBILITY_SERVICES: "Read screen content for text-to-speech"
source: THIRD_PARTY
verificationLevel: UNVERIFIED
```

2. **Implement interface**:
```kotlin
class ReaderPlugin : AccessibilityPluginInterface {
    override val manifest: PluginManifest = loadManifest()

    override fun provideCommands(): List<VoiceCommandDefinition> {
        return listOf(
            VoiceCommandDefinition(
                id = "read_screen",
                primaryText = "read screen",
                synonyms = listOf("read this", "what's here"),
                description = "Read all text on screen"
            ),
            VoiceCommandDefinition(
                id = "read_focused",
                primaryText = "read focused",
                synonyms = listOf("read this element"),
                description = "Read currently focused element"
            )
        )
    }

    override suspend fun executeCommand(commandId: String, parameters: Map<String, Any>?): CommandResult {
        return when (commandId) {
            "read_screen" -> readEntireScreen()
            "read_focused" -> readFocusedElement()
            else -> CommandResult.failure("Unknown command")
        }
    }

    private fun readEntireScreen(): CommandResult {
        val allText = getAllTextNodes()
        speakText(allText.joinToString(" "))
        return CommandResult.success("Reading screen")
    }
}
```

3. **Package plugin**:
```
plugin-root/
  plugin.yaml
  lib/
    reader-plugin.jar
  assets/
    icons/
      app-icon.png
```

4. **Install**:
```kotlin
val installer = PluginInstaller(registry, loader)
val result = installer.installFromZip("/downloads/reader-plugin.zip")
```

### Use Case 2: Creating a Cursor Plugin

**Goal**: Add custom cursor control mode

```kotlin
class GazeCursorPlugin : CursorPluginInterface {
    override val manifest = loadManifest()

    override fun provideCursorMode(): CursorModeDefinition {
        return CursorModeDefinition(
            id = "gaze_tracking",
            name = "Gaze Tracking",
            description = "Control cursor with eye tracking",
            inputSource = CursorInputSource.GAZE,
            smoothingEnabled = true,
            edgeDetectionEnabled = true
        )
    }

    override fun onCursorMove(position: Point, delta: Point, velocity: Float) {
        // Update gaze tracker
        gazeTracker.updatePosition(position)
    }

    override fun onCursorClick(position: Point, clickType: ClickType): Boolean {
        if (clickType == ClickType.LONG_PRESS) {
            // Custom long press behavior
            showContextMenu(position)
            return true
        }
        return false
    }

    override fun provideCursorAppearance(): CursorAppearance {
        return CursorAppearance(
            cursorType = CursorType.CROSSHAIR,
            size = 64,
            color = 0xFF00FF00.toInt(),
            opacity = 0.8f
        )
    }
}
```

### Use Case 3: Resolving Plugin Dependencies

**Goal**: Load plugins in correct order

```kotlin
val resolver = DependencyResolver(registry)

// Available plugins
val availablePlugins = mapOf(
    "com.base.ui" to uiManifest,
    "com.theme.dark" to darkThemeManifest  // depends on com.base.ui
)

// Resolve load order
val result = resolver.resolveDependencies("com.theme.dark", availablePlugins)

when (result) {
    is ResolutionResult.Success -> {
        val loadOrder = result.loadOrder  // ["com.base.ui", "com.theme.dark"]
        loadOrder.forEach { pluginId ->
            loader.loadPlugin(pluginId, ...)
        }
    }
    is ResolutionResult.Failure -> {
        if (result.cycle.isNotEmpty()) {
            println("Circular dependency: ${result.cycle.joinToString(" → ")}")
        }
    }
}
```

---

## Pitfalls & Gotchas

### Pitfall 1: Plugin ID Mismatch

**Problem**: Plugin ID parameter doesn't match manifest ID

```kotlin
// WRONG
loader.loadPlugin(
    pluginId = "com.example.my-plugin",  // Hyphen in parameter
    manifestPath = "/path/plugin.yaml"    // Contains: com.example.myplugin
)
// Throws: PluginException at PluginLoader.kt:155-161
```

**Solution**: Ensure consistency
```kotlin
// plugin.yaml
id: com.example.myplugin  # Lowercase, no hyphens in plugin ID

// Code
loader.loadPlugin(
    pluginId = "com.example.myplugin",  // EXACT match
    ...
)
```

### Pitfall 2: Coroutine Scope Issues

**Problem**: Permission dialogs require UI thread but called from background

```kotlin
// WRONG
GlobalScope.launch(Dispatchers.IO) {
    permissionManager.requestPermissions(...)  // Tries to show dialog on IO thread
}
```

**Solution**: Use Main dispatcher
```kotlin
// CORRECT
GlobalScope.launch(Dispatchers.Main) {
    permissionManager.requestPermissions(...)  // UI operations on Main thread
}
```

### Pitfall 3: Missing Namespace Cleanup

**Problem**: Uninstalled plugins leave files behind

```kotlin
// WRONG
registry.unregister(pluginId)  // Only removes from registry
```

**Solution**: Use PluginLoader.uninstallPlugin
```kotlin
// CORRECT
loader.uninstallPlugin(pluginId)  // Cleans namespace + unregisters
```

### Pitfall 4: Cache Invalidation After Updates

**Problem**: Asset cache not cleared after plugin update

```kotlin
// Update plugin
installer.updatePlugin(pluginId, newVersion)

// Old assets still cached!
val result = assetResolver.resolveAsset("plugin://...")  // Returns old cached version
```

**Solution**: Clear cache after updates
```kotlin
installer.updatePlugin(pluginId, newVersion)
assetResolver.clearCache()  // Force re-resolution
```

### Pitfall 5: Forgetting to Initialize PermissionManager

**Problem**: Permissions not persisted after app restart

```kotlin
// WRONG
val manager = PermissionManager(uiHandler, persistence)
// Never calls initialize()

// After app restart, all permissions forgotten!
```

**Solution**: Initialize on startup
```kotlin
// CORRECT
val manager = PermissionManager(uiHandler, persistence)
GlobalScope.launch {
    manager.initialize()  // Load persisted permissions
}
```

---

## Design Decisions

### Decision 1: Why Android-Only?

**Original**: MagicCode was KMP (Kotlin Multiplatform) supporting iOS, JVM, Android

**VOS4 Change**: Simplified to Android-only

**Rationale**:
- VOS4 is an Android accessibility service (Android-specific by nature)
- Reduced complexity: No need to maintain iOS/JVM implementations
- Faster development: Focus on one platform
- Still uses expect/actual for future extensibility

**Evidence**:
- `iosMain/` and `jvmMain/` folders exist but contain TODO stubs
- Only `androidMain/` has complete implementations

### Decision 2: Why Room Database?

**Alternatives Considered**:
- ObjectBox (used elsewhere in VOS4)
- SQLDelight
- Raw SQLite

**Chosen**: Room

**Rationale**:
- Type-safe SQL with compile-time validation
- Better coroutines support than ObjectBox
- Well-integrated with Android lifecycle
- Easy migration path for schema updates
- Lightweight for plugin metadata (not heavy data)

**Trade-off**: ObjectBox has better performance for large datasets, but plugin metadata is small (<1000 records)

### Decision 3: Why Separate PluginLoader and PluginRegistry?

**Alternative**: Combined class

**Rationale**:
- **Separation of Concerns**: Loader handles I/O and validation, Registry handles state
- **Testability**: Can test registry without file I/O
- **Reusability**: Registry can be used without loader (e.g., mock testing)
- **Single Responsibility**: Each class has one clear purpose

### Decision 4: Why Multiple Registry Indexes?

**Alternative**: Filter all plugins on every query

**Rationale**:
- **Performance**: O(1) lookups instead of O(n) filtering
- **Common operations**: Queries by state/source are frequent in VOS4
- **Memory trade-off**: Acceptable overhead (~3 HashMaps) for 100s of plugins
- **Scalability**: Handles 1000+ plugins efficiently

### Decision 5: Why YAML Manifests?

**Alternatives**: JSON, TOML, XML

**Chosen**: YAML

**Rationale**:
- Human-readable (plugin developers write by hand)
- Comments supported (helps documentation)
- Less verbose than XML/JSON
- Common in plugin ecosystems (Docker Compose, Kubernetes)
- Good Kotlin libraries (yamlkt)

**Trade-off**: YAML parsing slightly slower than JSON, but manifests are small and parsed once

### Decision 6: Why expect/actual Pattern?

**Alternative**: Platform-specific implementations with interfaces

**Rationale**:
- **Future-proof**: Easy to re-add iOS/JVM if needed
- **Clean API**: Common code uses expect declarations
- **Compile-time safety**: Actual implementations verified at compile time
- **Kotlin Multiplatform standard**: Idiomatic for KMP projects

**Current Reality**: Only Android implementations exist, but pattern preserved for flexibility

---

## Code Examples

### Example 1: Complete Plugin Loading Flow

```kotlin
// Initialize system
val context: Context = applicationContext
val database = Room.databaseBuilder(context, PluginDatabase::class.java, "plugins.db").build()
val persistence = RoomPluginPersistence(database)
val registry = PluginRegistry(persistence)
val loader = PluginLoader(PluginConfig(), registry)

// Load on startup
GlobalScope.launch {
    registry.loadFromPersistence()
}

// Load new plugin
GlobalScope.launch {
    val result = loader.loadPlugin(
        pluginId = "com.example.myplugin",
        manifestPath = "/data/plugins/com.example.myplugin/plugin.yaml",
        libraryPath = "/data/plugins/com.example.myplugin/lib/plugin.jar",
        appDataDir = "/data/app"
    )

    when (result) {
        is LoadResult.Success -> {
            val plugin = result.pluginInfo
            Log.i("PluginSystem", "Loaded: ${plugin.manifest.name}")

            // Update state to active
            registry.updateState(plugin.manifest.id, PluginState.ENABLED)
        }
        is LoadResult.Failure -> {
            Log.e("PluginSystem", "Failed: ${result.error.message}", result.error)
        }
    }
}
```

### Example 2: Permission Management

```kotlin
// Setup
val uiHandler = PermissionUIHandler(context)
val storage = PermissionStorage(context)
val manager = PermissionManager(uiHandler, storage)

// Initialize (load persisted)
GlobalScope.launch {
    manager.initialize()
}

// Request permissions
GlobalScope.launch(Dispatchers.Main) {
    val granted = manager.requestPermissions(
        pluginId = "com.example.plugin",
        pluginName = "Example Plugin",
        permissions = setOf(Permission.NETWORK, Permission.STORAGE_READ, Permission.CAMERA),
        rationales = mapOf(
            Permission.NETWORK to "Download updates from server",
            Permission.STORAGE_READ to "Load user preferences",
            Permission.CAMERA to "Scan QR codes"
        )
    )

    if (granted) {
        startPlugin()
    } else {
        val denied = manager.getDeniedPermissions("com.example.plugin")
        showDeniedDialog(denied)
    }
}

// Enforce permission before operation
GlobalScope.launch {
    try {
        manager.enforcePermission("com.example.plugin", Permission.NETWORK)
        downloadData()
    } catch (e: PermissionDeniedException) {
        Log.e("Plugin", "Network permission denied")
    }
}
```

### Example 3: Asset Resolution with Fallback

```kotlin
val resolver = AssetResolver(registry)

// Resolve with fallback
GlobalScope.launch {
    val result = resolver.resolveAsset(
        uri = "plugin://com.theme.pack/icons/missing-icon.png",
        useFallback = true
    )

    when (result) {
        is ResolutionResult.Success -> {
            val handle = result.assetHandle
            if (handle.metadata.isFallback) {
                Log.w("Assets", "Using fallback icon")
            }

            // Load as bitmap
            val bitmap = BitmapFactory.decodeFile(handle.absolutePath)
            imageView.setImageBitmap(bitmap)
        }
        is ResolutionResult.Failure -> {
            Log.e("Assets", "No fallback available: ${result.reason}")
        }
    }
}

// Batch resolution
GlobalScope.launch {
    val uris = listOf(
        "plugin://theme/icons/home.png",
        "plugin://theme/icons/settings.png",
        "plugin://theme/icons/profile.png"
    )

    val results = resolver.resolveAssetsBatch(uris)
    results.forEach { (uri, result) ->
        when (result) {
            is ResolutionResult.Success -> loadIcon(result.assetHandle)
            is ResolutionResult.Failure -> Log.e("Assets", "Failed: $uri")
        }
    }
}
```

### Example 4: Dependency Resolution

```kotlin
val resolver = DependencyResolver(registry)

// All available plugins
val availablePlugins = mapOf(
    "com.base.ui" to loadManifest("com.base.ui"),
    "com.theme.material" to loadManifest("com.theme.material"),  // depends on com.base.ui
    "com.widgets.custom" to loadManifest("com.widgets.custom")   // depends on com.theme.material
)

// Resolve for com.widgets.custom
GlobalScope.launch {
    val result = resolver.resolveDependencies("com.widgets.custom", availablePlugins)

    when (result) {
        is ResolutionResult.Success -> {
            val loadOrder = result.loadOrder
            // ["com.base.ui", "com.theme.material", "com.widgets.custom"]

            loadOrder.forEach { pluginId ->
                loader.loadPlugin(pluginId, ...)
            }
        }
        is ResolutionResult.Failure -> {
            if (result.cycle.isNotEmpty()) {
                Log.e("Deps", "Circular: ${result.cycle.joinToString(" → ")}")
            } else {
                Log.e("Deps", result.reason)
            }
        }
    }
}
```

### Example 5: Transaction Rollback

```kotlin
val transactionManager = TransactionManager(database.checkpointDao())

GlobalScope.launch {
    // Create checkpoint before risky operation
    val checkpoint = transactionManager.createCheckpoint(TransactionType.UPDATE)

    try {
        // Update plugin
        updatePlugin("com.example.plugin", newVersion)

        // Commit if successful
        transactionManager.commit(checkpoint)
    } catch (e: Exception) {
        Log.e("Transaction", "Update failed, rolling back", e)

        // Rollback to checkpoint
        transactionManager.rollback(checkpoint)
    }
}
```

---

## Testing Guide

### Unit Testing Core Components

**Testing PluginLoader**:
```kotlin
@Test
fun `loadPlugin should validate manifest schema`() = runBlocking {
    val mockRegistry = MockPluginRegistry()
    val loader = PluginLoader(PluginConfig(), mockRegistry)

    val result = loader.loadPlugin(
        pluginId = "invalid.plugin",
        manifestPath = "/test/invalid-plugin.yaml",
        libraryPath = "/test/plugin.jar",
        appDataDir = "/test/data"
    )

    assertTrue(result is LoadResult.Failure)
    assertTrue((result as LoadResult.Failure).error is ManifestInvalidException)
}
```

**Testing PermissionManager**:
```kotlin
@Test
fun `requestPermissions should cache granted permissions`() = runBlocking {
    val mockUI = MockPermissionUIHandler()
    val mockStorage = MockPermissionStorage()
    val manager = PermissionManager(mockUI, mockStorage)

    mockUI.grantAll = true  // User grants all permissions

    val granted = manager.requestPermissions(
        "com.test.plugin",
        setOf(Permission.NETWORK, Permission.STORAGE_READ)
    )

    assertTrue(granted)
    assertTrue(manager.hasPermission("com.test.plugin", Permission.NETWORK))
    assertTrue(manager.hasPermission("com.test.plugin", Permission.STORAGE_READ))
}
```

**Testing AssetResolver**:
```kotlin
@Test
fun `resolveAsset should return cached result on second call`() = runBlocking {
    val registry = PluginRegistry()
    val cache = AssetCache()
    val resolver = AssetResolver(registry, cache)

    // First call
    val result1 = resolver.resolveAsset("plugin://test/icons/app.png")

    // Second call (should hit cache)
    val result2 = resolver.resolveAsset("plugin://test/icons/app.png")

    val stats = resolver.getCacheStats()
    assertEquals(1, stats["size"])  // One entry cached
}
```

### Integration Testing

**Testing Full Plugin Load Cycle**:
```kotlin
@Test
fun `full plugin load cycle with persistence`() = runBlocking {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val db = Room.inMemoryDatabaseBuilder(context, PluginDatabase::class.java).build()
    val persistence = RoomPluginPersistence(db)
    val registry = PluginRegistry(persistence)
    val loader = PluginLoader(PluginConfig(), registry)

    // Load plugin
    val result = loader.loadPlugin(
        pluginId = "com.test.plugin",
        manifestPath = createTestManifest(),
        libraryPath = createTestJar(),
        appDataDir = context.filesDir.absolutePath
    )

    assertTrue(result is LoadResult.Success)

    // Verify persisted
    val plugins = db.pluginDao().getAllPlugins()
    assertEquals(1, plugins.size)
    assertEquals("com.test.plugin", plugins[0].id)
}
```

### Mock Objects

**Location**: `src/commonTest/kotlin/com/augmentalis/magiccode/plugins/mocks/`

Available mocks:
- `MockPluginRegistry.kt`
- `MockPluginLoader.kt`
- `MockPermissionUIHandler.kt`
- `MockPermissionStorage.kt`
- `MockFileIO.kt`
- `MockZipExtractor.kt`

---

## Performance Considerations

### 1. Registry Index Overhead

**Trade-off**: Memory vs. Speed

```kotlin
// Multiple indexes use extra memory
private val pluginsByState = mutableMapOf<PluginState, MutableSet<String>>()
private val pluginsBySource = mutableMapOf<PluginSource, MutableSet<String>>()
private val pluginsByVerificationLevel = mutableMapOf<DeveloperVerificationLevel, MutableSet<String>>()
```

**Memory**: ~3 HashMaps × 100 plugins = ~3KB (negligible)
**Speed**: O(1) queries instead of O(n)
**Verdict**: Worth the trade-off

### 2. Asset Cache Size

**Default**: 100 entries (configurable in PluginConfig)

**Tuning**:
```kotlin
val config = PluginConfig(
    assetCacheMaxSize = 200  // Increase for asset-heavy apps
)
```

**When to increase**:
- App with many theme plugins
- Frequent asset access
- Low memory constraints (decrease instead)

### 3. Permission Manager Initialization

**Cost**: Loads all permissions from database on startup

```kotlin
suspend fun initialize() {
    persistence?.getAllPermissionStates()?.forEach { ... }
}
```

**Optimization**: If you have 1000s of plugins, consider lazy loading:
```kotlin
// Instead of loading all on startup
// Load on-demand when plugin is activated
```

### 4. Dependency Resolution Complexity

**Worst Case**: O(n²) for cyclic dependency detection

**Typical Case**: O(n) for acyclic graphs

**Recommendation**: Keep dependency trees shallow (<5 levels)

---

## Security Considerations

### 1. Namespace Isolation (FR-038)

**Enforcement**: `PluginNamespace.validateAccess(path)`

```kotlin
// PREVENTS: Path traversal attacks
// plugin://my.plugin/images/../../../etc/passwd

namespace.validateAccess(assetPath)  // Throws SecurityException
```

**How It Works**:
```kotlin
fun validateAccess(path: String) {
    val canonical = File(path).canonicalPath
    if (!canonical.startsWith(baseDir)) {
        throw SecurityException("Access denied: $path outside namespace")
    }
}
```

### 2. Signature Verification

**File**: `SignatureVerifier.kt` (Android actual)

```kotlin
// Verify APK signature matches trusted certificate
val isValid = signatureVerifier.verify(apkPath, trustedCert)
if (!isValid) {
    throw SecurityException("Invalid plugin signature")
}
```

**Use Cases**:
- Pre-bundled plugins: Verify against app certificate
- Store plugins: Verify against AppAvenue certificate
- Third-party: User accepts risk (UNVERIFIED level)

### 3. Permission Enforcement

**Always enforce before sensitive operations**:

```kotlin
// WRONG: Assume permission
fun readFile(pluginId: String, path: String) {
    File(path).readText()  // No check!
}

// CORRECT: Enforce permission
suspend fun readFile(pluginId: String, path: String): String {
    permissionManager.enforcePermission(pluginId, Permission.STORAGE_READ)
    return File(path).readText()
}
```

### 4. Sandboxing

**Each plugin has isolated namespace**:

```
/data/data/com.augmentalis.voiceoscore/plugins/
  com.example.plugin1/
    data/          # Plugin-specific data
    cache/         # Plugin-specific cache
    preferences/   # Plugin-specific prefs
  com.example.plugin2/
    data/
    cache/
    preferences/
```

**Plugins CANNOT access each other's data** (enforced by validateAccess)

### 5. Developer Verification Levels

**VERIFIED**: Manual review passed
- Pre-bundled VOS4 plugins
- Highest trust

**REGISTERED**: Code signing only
- AppAvenue Store plugins
- Signature verified, limited review

**UNVERIFIED**: No verification
- Third-party sideloaded plugins
- User accepts risk
- Maximum sandboxing

---

## Appendix A: File Checklist

### Production Files (56 total)

**Core (11)**:
- [x] PluginLoader.kt
- [x] PluginRegistry.kt
- [x] PluginManifest.kt
- [x] PluginEnums.kt
- [x] PluginConfig.kt
- [x] PluginLogger.kt
- [x] PluginNamespace.kt
- [x] PluginException.kt
- [x] PluginErrorHandler.kt
- [x] PluginRequirementValidator.kt
- [x] ManifestValidator.kt

**Database (5 - Android only)**:
- [x] PluginEntity.kt
- [x] DependencyEntity.kt
- [x] PermissionEntity.kt
- [x] CheckpointEntity.kt
- [x] PluginDatabase.kt

**VOS4 Interfaces (3 - Android only)**:
- [x] AccessibilityPluginInterface.kt
- [x] CursorPluginInterface.kt
- [x] SpeechEnginePluginInterface.kt

**Assets (8)**:
- [x] AssetResolver.kt
- [x] AssetCache.kt
- [x] AssetHandle.kt
- [x] AssetReference.kt
- [x] AssetMetadata.kt
- [x] AssetAccessLogger.kt
- [x] ChecksumCalculator.kt
- [x] FallbackAssetProvider.kt

**Security (5)**:
- [x] PermissionManager.kt
- [x] PermissionUIHandler.kt
- [x] PermissionPersistence.kt
- [x] PermissionStorage.kt
- [x] SignatureVerifier.kt

**Dependencies (2)**:
- [x] DependencyResolver.kt
- [x] SemverConstraintValidator.kt

**Themes (5)**:
- [x] ThemeDefinition.kt
- [x] ThemeComponents.kt
- [x] ThemeManager.kt
- [x] ThemeValidator.kt
- [x] FontLoader.kt

**Platform (3)**:
- [x] FileIO.kt
- [x] ZipExtractor.kt
- [x] PluginClassLoader.kt

**Distribution (1)**:
- [x] PluginInstaller.kt

**Transactions (1)**:
- [x] TransactionManager.kt

**AI (1)**:
- [x] AIPluginInterface.kt

**Total**: 56 files

---

## Appendix B: Quick Reference

### Common Operations

| Operation | Code |
|-----------|------|
| Load plugin | `loader.loadPlugin(id, manifestPath, libPath, dataDir)` |
| Register plugin | `registry.register(manifest, namespace)` |
| Check permission | `manager.hasPermission(pluginId, permission)` |
| Resolve asset | `resolver.resolveAsset(uri)` |
| Update state | `registry.updateState(pluginId, PluginState.ENABLED)` |

### File Paths

| Resource | Path |
|----------|------|
| Database | `/data/data/com.augmentalis.voiceoscore/databases/plugin_system.db` |
| Plugin root | `/data/data/com.augmentalis.voiceoscore/plugins/` |
| Manifest | `{plugin-root}/plugin.yaml` |
| Library | `{plugin-root}/lib/plugin.jar` |
| Assets | `{plugin-root}/assets/{category}/` |

### Enum Values

**PluginState**: PENDING, INSTALLING, INSTALLED, ENABLED, DISABLED, UPDATING, UNINSTALLING, FAILED

**Permission**: CAMERA, LOCATION, STORAGE_READ, STORAGE_WRITE, NETWORK, MICROPHONE, CONTACTS, CALENDAR, BLUETOOTH, SENSORS, ACCESSIBILITY_SERVICES, PAYMENTS

**AssetCategory**: FONTS, ICONS, IMAGES, THEMES, CUSTOM

---

**Document Version**: 1.0
**Last Updated**: 2025-10-26 11:46 PDT
**Maintainer**: VOS4 Documentation Team

