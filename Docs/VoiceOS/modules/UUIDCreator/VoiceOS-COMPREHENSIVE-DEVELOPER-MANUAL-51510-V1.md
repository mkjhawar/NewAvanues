# UUIDCreator Comprehensive Developer Programming Manual
## From Novice to Expert: Complete Implementation Guide

**Version**: 1.0.0
**Date**: 2025-10-08
**Audience**: Novice to Advanced Developers
**Purpose**: Complete reference for VoiceOS UUID System

---

# Table of Contents

1. [Introduction](#1-introduction)
2. [Architecture Overview](#2-architecture-overview)
3. [Getting Started](#3-getting-started)
4. [Core Concepts](#4-core-concepts)
5. [UUID Generation](#5-uuid-generation)
6. [Storage & Persistence](#6-storage--persistence)
7. [Third-Party App Integration](#7-third-party-app-integration)
8. [Alias System](#8-alias-system)
9. [Hierarchical UUIDs](#9-hierarchical-uuids)
10. [Analytics & Monitoring](#10-analytics--monitoring)
11. [Advanced Topics](#11-advanced-topics)
12. [Best Practices](#12-best-practices)
13. [Troubleshooting](#13-troubleshooting)
14. [API Reference](#14-api-reference)

---

# 1. Introduction

## What is UUIDCreator?

UUIDCreator is a **Universal Unique Identifier Management System** for Android applications, specifically designed for voice-controlled user interfaces.

### Core Purpose

Enable **universal voice control** for:
- ✅ Your own app (with SDK integration)
- ✅ ANY third-party Android app (WITHOUT SDK - via accessibility)
- ✅ Complex nested UI hierarchies
- ✅ Cross-session persistence

### Key Features

| Feature | Description | Benefit |
|---------|-------------|---------|
| **Room Database** | On-disk persistence | Data survives restarts |
| **In-Memory Cache** | O(1) lookups | Lightning-fast reads |
| **Third-Party Generation** | Accessibility scanning | Universal voice control |
| **Alias System** | Human-readable names | Easy voice commands |
| **Hierarchical UUIDs** | Parent-child trees | Nested layout support |
| **Analytics** | Usage tracking | Optimization insights |
| **Collision Monitoring** | Integrity checks | System reliability |

---

# 2. Architecture Overview

## System Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                         UUIDCreator                             │
│                      (Main Entry Point)                         │
└────────────┬────────────────────────────────────────────────────┘
             │
             ├─────────────────┬──────────────────┬─────────────────┐
             │                 │                  │                 │
             ▼                 ▼                  ▼                 ▼
    ┌────────────────┐ ┌─────────────┐  ┌──────────────┐ ┌──────────────┐
    │  UUIDRegistry  │ │ UUIDGenerator│  │   Hierarchy  │ │  Analytics   │
    │   (Facade)     │ │   (Factory)  │  │   Manager    │ │   Tracker    │
    └───────┬────────┘ └──────┬──────┘  └──────┬───────┘ └──────┬───────┘
            │                  │                 │                 │
            └──────────────────┴─────────────────┴─────────────────┘
                                       │
                                       ▼
                            ┌──────────────────────┐
                            │   UUIDRepository     │
                            │  (Hybrid Storage)    │
                            └──────────┬───────────┘
                                       │
                    ┌──────────────────┴──────────────────┐
                    │                                     │
                    ▼                                     ▼
          ┌──────────────────┐                 ┌──────────────────┐
          │  In-Memory Cache │                 │  Room Database   │
          │ (ConcurrentMap)  │                 │  (SQLite)        │
          │                  │                 │                  │
          │  O(1) Reads      │                 │  Persistence     │
          └──────────────────┘                 └──────────────────┘
```

## Component Layers

### Layer 1: Application Interface
- **UUIDCreator**: Main entry point
- **UUIDRegistry**: Facade for registration/lookup

### Layer 2: Feature Modules
- **CustomUuidGenerator**: Format customization
- **ThirdPartyUuidGenerator**: Accessibility-based generation
- **UuidAliasManager**: Human-readable aliases
- **HierarchicalUuidManager**: Tree operations
- **UuidAnalytics**: Usage tracking
- **CollisionMonitor**: Integrity monitoring

### Layer 3: Storage Layer
- **UUIDRepository**: Hybrid storage abstraction
- **Room DAOs**: Database access objects
- **In-Memory Cache**: Performance optimization

### Layer 4: Data Layer
- **Room Entities**: Database models
- **Domain Models**: Business logic models
- **Converters**: Entity ↔ Model transformation

---

# 3. Getting Started

## Prerequisites

```gradle
// build.gradle (Module: app)
dependencies {
    implementation(project(":modules:libraries:UUIDCreator"))
}
```

## Initialization

### Step 1: Application Setup

```kotlin
/**
 * Application.kt
 *
 * Initialize UUIDCreator on app startup.
 * CRITICAL: Must be called before any UUID operations.
 */
class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize UUIDCreator (REQUIRED)
        UUIDCreator.initialize(applicationContext)

        // Optional: Enable logging
        UUIDCreator.getInstance().setLogLevel(LogLevel.DEBUG)
    }
}
```

**⚠️ Important**:
- Call `initialize()` ONCE in `Application.onCreate()`
- Passing `applicationContext` (not activity context)
- Do NOT call `getInstance()` before `initialize()`

### Step 2: Verify Initialization

```kotlin
/**
 * MainActivity.kt
 *
 * Verify UUIDCreator is ready to use.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get instance (will throw if not initialized)
        val uuidCreator = try {
            UUIDCreator.getInstance()
        } catch (e: IllegalStateException) {
            Log.e("MainActivity", "UUIDCreator not initialized!", e)
            return
        }

        // Check if database loaded
        lifecycleScope.launch {
            uuidCreator.ensureLoaded()
            Log.d("MainActivity", "UUIDCreator ready!")
        }
    }
}
```

---

# 4. Core Concepts

## UUID Formats

UUIDCreator supports 3 UUID formats:

### 1. Standard UUID (RFC 4122)

```
Format: 8-4-4-4-12 hexadecimal
Example: 550e8400-e29b-41d4-a716-446655440000
```

**When to Use**:
- Default for new elements
- Maximum compatibility
- No special requirements

**Generation**:
```kotlin
val uuid = UUIDGenerator.generate()
// Returns: "550e8400-e29b-41d4-a716-446655440000"
```

### 2. Custom Prefixed UUID

```
Format: {prefix}-{uuid}
Example: btn-550e8400-e29b-41d4-a716-446655440000
```

**When to Use**:
- Organizational clarity
- Type-based categorization
- Debugging convenience

**Generation**:
```kotlin
// With predefined prefix
val buttonUuid = CustomUuidGenerator.generate("btn")
// Returns: "btn-550e8400-e29b-41d4-a716-446655440000"

// With namespace
val nsUuid = CustomUuidGenerator.generate("com.myapp", "btn")
// Returns: "com.myapp.btn-550e8400-e29b-41d4-a716-446655440000"

// Auto-generate from type
val autoUuid = CustomUuidGenerator.generateByType("button")
// Returns: "btn-550e8400-e29b-41d4-a716-446655440000"
```

### 3. Third-Party UUID

```
Format: {package}.v{version}.{type}-{hash}
Example: com.instagram.android.v12.0.0.button-a7f3e2c1d4b5
```

**When to Use**:
- Third-party app voice control
- Apps without SDK integration
- Accessibility-based UI automation

**Generation**:
```kotlin
val thirdPartyGenerator = ThirdPartyUuidGenerator(context)

// Generate from AccessibilityNodeInfo
val uuid = thirdPartyGenerator.generateUuid(
    node = accessibilityNodeInfo,
    packageName = "com.instagram.android"
)
// Returns: "com.instagram.android.v12.0.0.button-a7f3e2c1d4b5"
```

---

## UUID Lifecycle

```
┌─────────────────────────────────────────────────────────────┐
│ 1. GENERATION                                               │
│    UUIDGenerator.generate() or CustomUuidGenerator          │
│    ↓                                                         │
│ 2. REGISTRATION                                             │
│    registry.register(element)                               │
│    ↓                                                         │
│ 3. STORAGE                                                  │
│    → Room Database (on-disk)                                │
│    → In-Memory Cache (fast lookup)                          │
│    ↓                                                         │
│ 4. ACTIVE USE                                               │
│    → Voice commands                                         │
│    → UI interactions                                        │
│    → Analytics tracking                                     │
│    ↓                                                         │
│ 5. UPDATES                                                  │
│    registry.update(element)                                 │
│    ↓                                                         │
│ 6. DELETION                                                 │
│    registry.unregister(uuid)                                │
│    → Cascade delete children (if hierarchical)             │
│    → Remove from cache and database                         │
└─────────────────────────────────────────────────────────────┘
```

---

# 5. UUID Generation

## Basic Generation

### Generate Standard UUID

```kotlin
/**
 * Generate a standard RFC 4122 compliant UUID.
 *
 * Uses java.util.UUID.randomUUID() internally.
 * Format: 8-4-4-4-12 hexadecimal
 *
 * Example Output:
 * "550e8400-e29b-41d4-a716-446655440000"
 */
fun generateStandardUuid(): String {
    // Simple generation
    val uuid = UUIDGenerator.generate()

    // Validate format (optional)
    require(uuid.matches(Regex("[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}"))) {
        "Invalid UUID format: $uuid"
    }

    return uuid
}
```

### Generate Custom Prefix UUID

```kotlin
/**
 * Generate UUID with organizational prefix.
 *
 * Prefixes help identify component types in logs and debugging.
 * Base UUID remains RFC 4122 compliant.
 *
 * Predefined Prefixes:
 * - btn (button)
 * - txt (text)
 * - img (image)
 * - input (input field)
 * - layout (layout container)
 * - menu (menu item)
 * - dialog (dialog window)
 * - theme (theme/style)
 */
fun generateCustomPrefixUuid() {
    // Method 1: Use predefined prefix
    val buttonUuid = CustomUuidGenerator.generate("btn")
    // Returns: "btn-550e8400-e29b-41d4-a716-446655440000"

    // Method 2: Use custom prefix
    val customUuid = CustomUuidGenerator.generate("submit-btn")
    // Returns: "submit-btn-550e8400-e29b-41d4-a716-446655440000"

    // Method 3: Auto-generate from component type
    val autoUuid = CustomUuidGenerator.generateByType("button")
    // Returns: "btn-{uuid}"  (automatically maps button → btn)

    // Method 4: With namespace
    val nsUuid = CustomUuidGenerator.generate("com.myapp", "btn")
    // Returns: "com.myapp.btn-550e8400-e29b-41d4-a716-446655440000"
}
```

### Generate Third-Party UUID

```kotlin
/**
 * Generate UUID for third-party app element.
 *
 * Uses accessibility fingerprinting to create stable, deterministic UUIDs
 * for apps that don't have SDK integration.
 *
 * Requirements:
 * - Accessibility service enabled
 * - AccessibilityNodeInfo available
 *
 * UUID Stability:
 * - STABLE across app sessions (same version)
 * - CHANGES when app updates (version isolation)
 */
suspend fun generateThirdPartyUuid(
    node: AccessibilityNodeInfo,
    context: Context
): String {
    // Create generator
    val generator = ThirdPartyUuidGenerator(context)

    // Extract package name from node
    val packageName = node.packageName?.toString()
        ?: throw IllegalArgumentException("Node has no package name")

    // Generate UUID
    val uuid = generator.generateUuid(
        node = node,
        packageName = packageName
    )

    // UUID format: com.instagram.android.v12.0.0.button-a7f3e2c1d4b5
    //               └─package───────────┘ └─ver─┘ └type┘ └─hash──┘

    return uuid
}
```

---

## Advanced Generation

### Batch Generation

```kotlin
/**
 * Generate multiple UUIDs efficiently.
 *
 * Use case: Registering many UI elements at once.
 */
fun batchGenerateUuids(count: Int): List<String> {
    return List(count) { UUIDGenerator.generate() }
}

/**
 * Generate UUIDs for entire accessibility tree.
 *
 * Recursively scans all nodes in UI hierarchy.
 */
suspend fun generateUuidsForTree(
    rootNode: AccessibilityNodeInfo,
    context: Context
): Map<AccessibilityNodeInfo, String> {
    val generator = ThirdPartyUuidGenerator(context)

    return generator.generateUuidsForTree(
        rootNode = rootNode,
        packageName = rootNode.packageName?.toString()
    )
    // Returns: Map of node → UUID for entire tree
}
```

### Type-Safe Generation

```kotlin
/**
 * Generate UUIDs with compile-time type safety.
 *
 * Enum-based prefix selection prevents typos.
 */
enum class ComponentType(val prefix: String) {
    BUTTON("btn"),
    TEXT("txt"),
    IMAGE("img"),
    INPUT("input"),
    CONTAINER("container"),
    MENU("menu")
}

fun generateTypeSafeUuid(type: ComponentType): String {
    return CustomUuidGenerator.generate(type.prefix)
}

// Usage
val buttonUuid = generateTypeSafeUuid(ComponentType.BUTTON)
// Returns: "btn-550e8400-e29b-41d4-a716-446655440000"
```

---

# 6. Storage & Persistence

## Understanding Hybrid Storage

UUIDCreator uses a **hybrid storage pattern**:

1. **Room Database** (SQLite): On-disk persistence
2. **In-Memory Cache** (ConcurrentHashMap): Fast lookups

### Why Hybrid?

| Approach | Speed | Persistence | Chosen |
|----------|-------|-------------|--------|
| **In-Memory Only** | ⚡ O(1) | ❌ Lost on restart | ❌ No |
| **Database Only** | 🐢 O(log n) | ✅ Survives restart | ❌ No |
| **Hybrid (Both)** | ⚡ O(1) | ✅ Survives restart | ✅ YES |

---

## Registering Elements

### Basic Registration

```kotlin
/**
 * Register a UUID element.
 *
 * This:
 * 1. Validates UUID uniqueness
 * 2. Stores in Room database
 * 3. Adds to in-memory cache
 * 4. Updates indexes (name, type, hierarchy)
 * 5. Emits registration event
 */
suspend fun registerElement() {
    // Create element
    val element = UUIDElement(
        uuid = UUIDGenerator.generate(),
        name = "Submit Button",
        type = "button",
        description = "Main submit button for login form",
        position = UUIDPosition(x = 100f, y = 200f, index = 0),
        isEnabled = true,
        priority = 1
    )

    // Register
    val registry = UUIDCreator.getInstance().registry
    val registeredUuid = registry.register(element)

    println("Registered: $registeredUuid")
    // Output: "550e8400-e29b-41d4-a716-446655440000"
}
```

### Registration with Validation

```kotlin
/**
 * Register element with collision detection.
 *
 * Checks for duplicates before registration.
 */
suspend fun registerWithValidation(element: UUIDElement) {
    val repository = // get repository
    val collisionMonitor = CollisionMonitor(repository, scope)

    // Check for collision
    val result = collisionMonitor.checkCollision(element.uuid, element)

    when (result) {
        is CollisionResult.NoCollision -> {
            // Safe to register
            registry.register(element)
            println("✅ Registered: ${element.uuid}")
        }

        is CollisionResult.Collision -> {
            // Handle collision
            println("❌ Collision detected!")
            println("Existing: ${result.existing.name}")
            println("Proposed: ${result.proposed.name}")

            when (result.suggestedResolution) {
                ResolutionStrategy.GenerateNewUuid -> {
                    // Generate new UUID
                    val newUuid = UUIDGenerator.generate()
                    val fixedElement = element.copy(uuid = newUuid)
                    registry.register(fixedElement)
                    println("✅ Registered with new UUID: $newUuid")
                }

                ResolutionStrategy.SkipRegistration -> {
                    println("⏭️  Skipped (duplicate)")
                }

                else -> {
                    println("⚠️  Manual resolution required")
                }
            }
        }
    }
}
```

---

## Querying Elements

### Find by UUID (O(1))

```kotlin
/**
 * Lookup element by UUID.
 *
 * Performance: O(1) - in-memory cache lookup
 */
fun findByUuid(uuid: String): UUIDElement? {
    val registry = UUIDCreator.getInstance().registry

    return registry.findByUUID(uuid)
}

// Usage
val element = findByUuid("550e8400-e29b-41d4-a716-446655440000")
if (element != null) {
    println("Found: ${element.name}")
} else {
    println("Not found")
}
```

### Find by Name

```kotlin
/**
 * Search elements by name.
 *
 * Supports exact and partial matching.
 */
fun findByName(name: String, exactMatch: Boolean = false): List<UUIDElement> {
    val registry = UUIDCreator.getInstance().registry

    return registry.findByName(name, exactMatch)
}

// Exact match
val submitButtons = findByName("Submit Button", exactMatch = true)
// Returns: Elements with name exactly "Submit Button"

// Partial match (case-insensitive)
val allButtons = findByName("button", exactMatch = false)
// Returns: All elements containing "button" in name
```

### Find by Type

```kotlin
/**
 * Get all elements of specific type.
 */
fun findByType(type: String): List<UUIDElement> {
    val registry = UUIDCreator.getInstance().registry

    return registry.findByType(type)
}

// Usage
val allButtons = findByType("button")
val allInputs = findByType("input")
```

### Advanced Queries

```kotlin
/**
 * Complex search with multiple criteria.
 */
fun advancedSearch(
    name: String? = null,
    type: String? = null,
    description: String? = null,
    enabledOnly: Boolean = true
): List<UUIDElement> {
    val registry = UUIDCreator.getInstance().registry

    return registry.search(
        name = name,
        type = type,
        description = description,
        enabledOnly = enabledOnly
    )
}

// Find enabled buttons with "submit" in name
val results = advancedSearch(
    name = "submit",
    type = "button",
    enabledOnly = true
)
```

---

## Updating Elements

```kotlin
/**
 * Update existing element.
 *
 * Changes:
 * - Updates Room database
 * - Updates in-memory cache
 * - Rebuilds indexes if name/type changed
 */
suspend fun updateElement(uuid: String) {
    val registry = UUIDCreator.getInstance().registry

    // Get existing element
    val element = registry.findByUUID(uuid)
        ?: throw IllegalArgumentException("Element not found: $uuid")

    // Modify element
    val updated = element.copy(
        name = "Updated Button Name",
        isEnabled = false,
        priority = 5
    )

    // Update
    val success = registry.update(updated)

    if (success) {
        println("✅ Updated: $uuid")
    } else {
        println("❌ Update failed: $uuid")
    }
}
```

---

## Deleting Elements

```kotlin
/**
 * Delete element by UUID.
 *
 * This:
 * - Removes from Room database (CASCADE deletes related data)
 * - Removes from in-memory cache
 * - Cleans up indexes
 * - Removes from parent's children list
 */
suspend fun deleteElement(uuid: String) {
    val registry = UUIDCreator.getInstance().registry

    val deleted = registry.unregister(uuid)

    if (deleted) {
        println("✅ Deleted: $uuid")
    } else {
        println("❌ Not found: $uuid")
    }
}
```

---

# 7. Third-Party App Integration

## Overview

Third-party UUID generation enables voice control for **ANY Android app** without requiring SDK integration.

### How It Works

```
1. User opens Instagram app
   ↓
2. VoiceAccessibility service detects app launch
   ↓
3. Service scans AccessibilityNodeInfo tree
   ↓
4. ThirdPartyUuidGenerator creates UUIDs for all elements
   ↓
5. UUIDs registered with UUIDCreator
   ↓
6. User can now use voice commands:
   "Click Instagram like button"
   "Scroll Instagram feed"
```

---

## Fingerprint Generation

```kotlin
/**
 * Create accessibility fingerprint for UI element.
 *
 * Fingerprint Components (in order of stability):
 * 1. Resource ID (most stable)
 * 2. Hierarchy path (stable if layout unchanged)
 * 3. Class name (stable)
 * 4. Text + Content description (less stable - localization)
 * 5. Bounds (least stable - screen size dependent)
 */
fun createFingerprint(
    node: AccessibilityNodeInfo,
    packageName: String,
    appVersion: String
): AccessibilityFingerprint {
    return AccessibilityFingerprint.fromNode(
        node = node,
        packageName = packageName,
        appVersion = appVersion
    )
}

/**
 * Example fingerprint for Instagram like button:
 *
 * AccessibilityFingerprint(
 *     resourceId = "com.instagram.android:id/row_feed_button_like",
 *     className = "android.widget.ImageView",
 *     text = null,
 *     contentDescription = "Like",
 *     hierarchyPath = "/0/2/1/3",
 *     packageName = "com.instagram.android",
 *     appVersion = "12.0.0"
 * )
 */
```

---

## UUID Generation Process

```kotlin
/**
 * Complete third-party UUID generation flow.
 *
 * Step-by-step process with error handling.
 */
suspend fun generateThirdPartyUuidComplete(
    node: AccessibilityNodeInfo,
    context: Context
): String? {
    // Step 1: Extract package name
    val packageName = node.packageName?.toString()
    if (packageName == null) {
        Log.e("UUID", "Node has no package name")
        return null
    }

    // Step 2: Create generator
    val generator = ThirdPartyUuidGenerator(context)

    // Step 3: Get app version
    val versionResolver = PackageVersionResolver(context)
    val version = try {
        versionResolver.getVersionString(packageName)
    } catch (e: PackageManager.NameNotFoundException) {
        Log.e("UUID", "Package not found: $packageName", e)
        return null
    }

    // Step 4: Create fingerprint
    val fingerprint = AccessibilityFingerprint.fromNode(
        node = node,
        packageName = packageName,
        appVersion = version
    )

    // Step 5: Check fingerprint stability
    val stabilityScore = fingerprint.calculateStabilityScore()
    if (stabilityScore < 0.5f) {
        Log.w("UUID", "Low stability score: $stabilityScore for ${fingerprint.className}")
    }

    // Step 6: Generate UUID
    val uuid = generator.generateUuid(node, packageName)

    // Step 7: Log for debugging
    Log.d("UUID", """
        Generated third-party UUID:
        - UUID: $uuid
        - Package: $packageName
        - Version: $version
        - Type: ${fingerprint.getElementType()}
        - Stability: ${"%.2f".format(stabilityScore)}
    """.trimIndent())

    return uuid
}
```

---

## App Update Handling

```kotlin
/**
 * Handle app updates with UUID stability tracking.
 *
 * When Instagram updates from v12.0.0 to v12.0.1:
 * - Old UUIDs: com.instagram.android.v12.0.0.button-abc123
 * - New UUIDs: com.instagram.android.v12.0.1.button-xyz789
 * - Tracker creates mapping: abc123 → xyz789
 */
suspend fun handleAppUpdate(packageName: String) {
    val context = // get context
    val tracker = UuidStabilityTracker(context)

    // Detect if app updated
    val updated = tracker.detectAppUpdate(packageName)

    if (updated) {
        println("📱 $packageName updated!")

        // Remap UUIDs
        val mappings = tracker.remapUuidsForUpdatedApp(packageName)

        println("🔄 Created ${mappings.size} UUID mappings")

        // Show mapping report
        val report = tracker.getStabilityReport(packageName)
        println(report)

        // Apply mappings to voice commands
        // (Voice command system would use these to update stored commands)
        mappings.forEach { mapping ->
            println("""
                Mapping:
                - Old: ${mapping.oldUuid}
                - New: ${mapping.newUuid}
                - Confidence: ${mapping.confidence}
            """.trimIndent())
        }
    }
}
```

---

# 8. Alias System

## Why Aliases?

Compare voice commands:

```
❌ Without Aliases:
"Click 550e8400-e29b-41d4-a716-446655440000"
"Click com.instagram.android.v12.0.0.button-a7f3e2c1d4b5"

✅ With Aliases:
"Click submit button"
"Click instagram like"
```

Aliases make UUIDs **human-readable** and **easy to remember**.

---

## Auto-Generated Aliases

```kotlin
/**
 * Auto-generate alias from element properties.
 *
 * Format: {app}_{content}_{type}
 *
 * Examples:
 * - Instagram like button → "instagram_like_btn"
 * - Facebook share menu → "facebook_share_menu"
 * - Submit button → "submit_btn"
 */
suspend fun createAutoAlias(element: UUIDElement): String {
    val aliasManager = UuidAliasManager(database)

    val alias = aliasManager.createAutoAlias(
        uuid = element.uuid,
        elementName = element.name,
        elementType = element.type,
        useAbbreviation = true  // Instagram → ig, Facebook → fb
    )

    println("Created alias: $alias → ${element.uuid}")
    return alias
}

/**
 * Example output:
 * "instagram_like_btn" → "com.instagram.android.v12.0.0.button-a7f3e2c1d4b5"
 * "fb_share_menu" → "com.facebook.katana.v8.0.0.menu-def456ghi789"
 */
```

---

## Manual Aliases

```kotlin
/**
 * Create custom alias manually.
 *
 * Rules:
 * - Lowercase letters + numbers + underscores
 * - 3-50 characters
 * - Must start with letter
 */
suspend fun createManualAlias() {
    val aliasManager = UuidAliasManager(database)

    // Create custom alias
    try {
        aliasManager.setAlias(
            uuid = "550e8400-e29b-41d4-a716-446655440000",
            alias = "my_favorite_button"
        )
        println("✅ Alias created")
    } catch (e: IllegalArgumentException) {
        println("❌ Alias already exists or invalid format")
    }
}
```

---

## Resolving Aliases

```kotlin
/**
 * Resolve alias to UUID.
 */
fun resolveAlias(alias: String): String? {
    val aliasManager = UuidAliasManager(database)

    val uuid = aliasManager.resolveAlias(alias)

    return uuid
}

// Usage in voice command handler
fun handleVoiceCommand(command: String) {
    // Command: "click instagram_like"
    val parts = command.split(" ")
    val action = parts[0]  // "click"
    val target = parts[1]   // "instagram_like"

    // Resolve alias
    val uuid = resolveAlias(target)

    if (uuid != null) {
        // Execute action on UUID
        executeAction(uuid, action)
    } else {
        println("❌ Unknown alias: $target")
    }
}
```

---

## Batch Alias Creation

```kotlin
/**
 * Create aliases for all elements in a package.
 *
 * Use case: User installs Instagram, generate aliases for all buttons.
 */
suspend fun createAliasesForApp(packageName: String) {
    val aliasManager = UuidAliasManager(database)

    val aliases = aliasManager.createAliasesForPackage(packageName)

    println("Created ${aliases.size} aliases for $packageName")

    aliases.forEach { (uuid, alias) ->
        println("$alias → $uuid")
    }
}

/**
 * Example output:
 * Created 47 aliases for com.instagram.android
 * instagram_like_btn → com.instagram.android.v12.0.0.button-abc123
 * instagram_comment_btn → com.instagram.android.v12.0.0.button-def456
 * instagram_share_menu → com.instagram.android.v12.0.0.menu-ghi789
 * ...
 */
```

---

# 9. Hierarchical UUIDs

## Understanding Hierarchies

UI elements are naturally hierarchical:

```
LoginForm
├─ UsernameContainer
│  ├─ UsernameLabel
│  └─ UsernameInput
└─ PasswordContainer
   ├─ PasswordLabel
   ├─ PasswordInput
   └─ ShowPasswordToggle
```

Hierarchical UUIDs preserve this structure.

---

## Creating Hierarchies

```kotlin
/**
 * Build UI hierarchy with parent-child relationships.
 */
suspend fun createLoginFormHierarchy() {
    val hierarchyManager = HierarchicalUuidManager(repository)
    val registry = UUIDCreator.getInstance().registry

    // Create form (root)
    val formUuid = UUIDGenerator.generate()
    val form = UUIDElement(
        uuid = formUuid,
        name = "Login Form",
        type = "layout"
    )
    registry.register(form)

    // Create username container
    val usernameContainerUuid = UUIDGenerator.generate()
    val usernameContainer = UUIDElement(
        uuid = usernameContainerUuid,
        name = "Username Container",
        type = "container",
        parent = formUuid  // Set parent
    )
    registry.register(usernameContainer)

    // Add to hierarchy
    hierarchyManager.addChild(
        parentUuid = formUuid,
        childUuid = usernameContainerUuid,
        position = 0  // First child
    )

    // Create username input
    val usernameInputUuid = UUIDGenerator.generate()
    val usernameInput = UUIDElement(
        uuid = usernameInputUuid,
        name = "Username Input",
        type = "input",
        parent = usernameContainerUuid
    )
    registry.register(usernameInput)

    hierarchyManager.addChild(
        parentUuid = usernameContainerUuid,
        childUuid = usernameInputUuid,
        position = 0
    )

    // Continue building tree...
}
```

---

## Traversing Hierarchies

```kotlin
/**
 * Get all children of parent.
 */
suspend fun getChildren(parentUuid: String): List<String> {
    val hierarchyManager = HierarchicalUuidManager(repository)

    return hierarchyManager.getChildren(parentUuid)
}

/**
 * Get all ancestors (path to root).
 */
suspend fun getAncestors(uuid: String): List<String> {
    val hierarchyManager = HierarchicalUuidManager(repository)

    return hierarchyManager.getAncestors(uuid)
}

/**
 * Example:
 * UsernameInput → [UsernameContainer, LoginForm]
 */

/**
 * Get all descendants (entire subtree).
 */
suspend fun getDescendants(uuid: String): List<String> {
    val hierarchyManager = HierarchicalUuidManager(repository)

    return hierarchyManager.getDescendants(uuid)
}

/**
 * Example:
 * LoginForm → [UsernameContainer, UsernameLabel, UsernameInput,
 *              PasswordContainer, PasswordLabel, PasswordInput, ShowPasswordToggle]
 */
```

---

## Building Trees

```kotlin
/**
 * Build complete tree structure.
 */
suspend fun buildCompleteTree(rootUuid: String): UuidTree {
    val hierarchyManager = HierarchicalUuidManager(repository)

    val tree = hierarchyManager.buildTree(rootUuid)

    // Print tree
    println(tree.toPrettyString())

    return tree
}

/**
 * Example output:
 * - Login Form (layout)
 *   - Username Container (container)
 *     - Username Label (text)
 *     - Username Input (input)
 *   - Password Container (container)
 *     - Password Label (text)
 *     - Password Input (input)
 *     - Show Password Toggle (checkbox)
 */
```

---

## Cascade Operations

```kotlin
/**
 * Delete element and all descendants.
 *
 * Example: Delete Login Form → deletes entire form including all inputs
 */
suspend fun cascadeDelete(uuid: String) {
    val hierarchyManager = HierarchicalUuidManager(repository)

    val deletedCount = hierarchyManager.deleteWithDescendants(uuid)

    println("Deleted $deletedCount elements")
}

/**
 * Example:
 * cascadeDelete("login-form-uuid")
 * → Deletes: form + 2 containers + 2 labels + 2 inputs + 1 toggle = 8 elements
 */
```

---

## Hierarchy Validation

```kotlin
/**
 * Validate hierarchy integrity.
 *
 * Checks for:
 * - Orphaned children (parent doesn't exist)
 * - Circular references
 * - Inconsistent parent-child links
 */
suspend fun validateHierarchy() {
    val hierarchyManager = HierarchicalUuidManager(repository)

    val result = hierarchyManager.validateIntegrity()

    if (result.isValid) {
        println("✅ Hierarchy is valid")
    } else {
        println("❌ Hierarchy validation failed:")
        result.issues.forEach { issue ->
            println("  - $issue")
        }
    }
}
```

---

# 10. Analytics & Monitoring

## Usage Analytics

```kotlin
/**
 * Track element access.
 */
suspend fun trackElementAccess(uuid: String) {
    val analytics = UuidAnalytics(repository)

    analytics.trackAccess(uuid)
    // Increments access counter
    // Updates last access timestamp
}

/**
 * Track action execution.
 */
suspend fun trackActionExecution(
    uuid: String,
    action: String,
    startTime: Long
) {
    val analytics = UuidAnalytics(repository)

    val endTime = System.currentTimeMillis()
    val executionTime = endTime - startTime

    analytics.trackExecution(
        uuid = uuid,
        action = action,
        executionTimeMs = executionTime,
        success = true
    )
}
```

---

## Analytics Insights

```kotlin
/**
 * Get most used elements.
 */
suspend fun getMostUsedElements() {
    val analytics = UuidAnalytics(repository)

    val mostUsed = analytics.getMostUsed(limit = 10)

    println("Top 10 Most Used Elements:")
    mostUsed.forEachIndexed { index, stats ->
        println("${index + 1}. ${stats.name ?: stats.uuid}")
        println("   Accesses: ${stats.accessCount}")
        println("   Last used: ${stats.lastAccessed}")
    }
}

/**
 * Example output:
 * Top 10 Most Used Elements:
 * 1. Submit Button
 *    Accesses: 247
 *    Last used: 1704067200000
 * 2. Instagram Like Button
 *    Accesses: 189
 *    Last used: 1704066800000
 * ...
 */
```

---

## Collision Monitoring

```kotlin
/**
 * Monitor for UUID collisions.
 */
fun startCollisionMonitoring() {
    val collisionMonitor = CollisionMonitor(repository, scope)

    // Start background monitoring (every 60 minutes)
    collisionMonitor.startMonitoring(intervalMinutes = 60)

    // Listen for collision events
    scope.launch {
        collisionMonitor.collisions.collect { event ->
            when (event) {
                is CollisionEvent.DuplicateUuid -> {
                    Log.e("Collision", "Duplicate UUID: ${event.uuid}")
                }
                is CollisionEvent.DatabaseCorruption -> {
                    Log.e("Collision", "Database corruption: ${event.details}")
                }
                is CollisionEvent.OrphanedReference -> {
                    Log.w("Collision", "Orphaned child: ${event.uuid}")
                }
                is CollisionEvent.InvalidFormat -> {
                    Log.w("Collision", "Invalid UUID format: ${event.uuid}")
                }
                is CollisionEvent.MonitoringError -> {
                    Log.e("Collision", "Monitoring error: ${event.message}")
                }
            }
        }
    }
}
```

---

# 11. Advanced Topics

## Performance Optimization

```kotlin
/**
 * Optimize cache performance.
 *
 * Tips:
 * 1. Use batch operations when possible
 * 2. Lazy load third-party UUIDs
 * 3. Prune old cache entries
 */
suspend fun optimizePerformance() {
    val generator = ThirdPartyUuidGenerator(context)

    // Get cache stats
    val stats = generator.getCacheStats()
    println("Cache hit rate: ${stats.hitRate}")

    // Clear old entries if hit rate is low
    if (stats.hitRate < 0.5f) {
        generator.clearCache()
    }
}
```

---

## Custom Fingerprinting

```kotlin
/**
 * Create custom fingerprint calculator.
 *
 * Override default hierarchy path calculation.
 */
fun customFingerprintCalculation(node: AccessibilityNodeInfo): String {
    val fingerprint = AccessibilityFingerprint.fromNode(
        node = node,
        packageName = "com.myapp",
        appVersion = "1.0.0",
        calculateHierarchyPath = { customNode ->
            // Custom path calculation
            buildCustomPath(customNode)
        }
    )

    return fingerprint.generateHash()
}

fun buildCustomPath(node: AccessibilityNodeInfo): String {
    // Custom implementation
    return "/custom/path"
}
```

---

# 12. Best Practices

## DO ✅

1. **Initialize Once**
   ```kotlin
   // In Application.onCreate()
   UUIDCreator.initialize(applicationContext)
   ```

2. **Use Aliases**
   ```kotlin
   // Create aliases for all elements
   aliasManager.createAutoAlias(uuid, name, type)
   ```

3. **Validate Hierarchies**
   ```kotlin
   // Periodically check integrity
   hierarchyManager.validateIntegrity()
   ```

4. **Monitor Collisions**
   ```kotlin
   // Start background monitoring
   collisionMonitor.startMonitoring(60)
   ```

5. **Track Analytics**
   ```kotlin
   // Track every access
   analytics.trackAccess(uuid)
   ```

## DON'T ❌

1. **Don't Initialize Multiple Times**
   ```kotlin
   // ❌ WRONG - Don't call initialize() again
   UUIDCreator.initialize(context)  // Already called
   ```

2. **Don't Skip Lazy Loading**
   ```kotlin
   // ❌ WRONG - May cause data not found
   val element = registry.findByUUID(uuid)

   // ✅ CORRECT - Ensure loaded first
   uuidCreator.ensureLoaded()
   val element = registry.findByUUID(uuid)
   ```

3. **Don't Create Circular References**
   ```kotlin
   // ❌ WRONG - Creates infinite loop
   hierarchyManager.addChild("parent-1", "child-1")
   hierarchyManager.addChild("child-1", "parent-1")  // Circular!
   ```

---

# 13. Troubleshooting

## Common Issues

### Issue 1: "UUIDCreator not initialized"

**Error**:
```
IllegalStateException: UUIDCreator not initialized. Call UUIDCreator.initialize(context) first.
```

**Solution**:
```kotlin
// Add to Application.onCreate()
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        UUIDCreator.initialize(applicationContext)  // ← Add this
    }
}
```

---

### Issue 2: Third-Party UUIDs Change

**Problem**: Instagram UUIDs changed after app update

**Explanation**: UUIDs are version-scoped:
```
v12.0.0 → com.instagram.android.v12.0.0.button-abc123
v12.0.1 → com.instagram.android.v12.0.1.button-xyz789  (Different!)
```

**Solution**: Use `UuidStabilityTracker`
```kotlin
val tracker = UuidStabilityTracker(context)
val mappings = tracker.remapUuidsForUpdatedApp("com.instagram.android")
// Apply mappings to voice commands
```

---

### Issue 3: Slow Performance

**Problem**: UUID lookups are slow

**Diagnosis**:
```kotlin
val stats = uuidCreator.getCacheStats()
if (!repository.isCacheLoaded()) {
    // Cache not loaded!
    uuidCreator.ensureLoaded()
}
```

**Solution**: Always call `ensureLoaded()` first

---

# 14. API Reference

## Core Classes

| Class | Purpose | Key Methods |
|-------|---------|-------------|
| `UUIDCreator` | Main entry point | `initialize()`, `getInstance()` |
| `UUIDRegistry` | Element registration | `register()`, `findByUUID()` |
| `UUIDGenerator` | Standard UUID generation | `generate()` |
| `CustomUuidGenerator` | Custom format generation | `generate(prefix)` |
| `ThirdPartyUuidGenerator` | Third-party generation | `generateUuid(node)` |
| `UuidAliasManager` | Alias management | `createAutoAlias()`, `resolveAlias()` |
| `HierarchicalUuidManager` | Hierarchy operations | `addChild()`, `getDescendants()` |
| `UuidAnalytics` | Usage tracking | `trackAccess()`, `getMostUsed()` |
| `CollisionMonitor` | Integrity monitoring | `checkCollision()`, `startMonitoring()` |

---

## Complete Example Application

```kotlin
/**
 * Complete example: Instagram voice control.
 *
 * Demonstrates:
 * - Initialization
 * - Third-party UUID generation
 * - Alias creation
 * - Voice command handling
 * - Analytics tracking
 */
class InstagramVoiceControl : Application() {

    private lateinit var uuidCreator: UUIDCreator
    private lateinit var aliasManager: UuidAliasManager
    private lateinit var analytics: UuidAnalytics

    override fun onCreate() {
        super.onCreate()

        // 1. Initialize UUIDCreator
        uuidCreator = UUIDCreator.initialize(applicationContext)

        // 2. Setup components
        val database = UUIDCreatorDatabase.getInstance(this)
        aliasManager = UuidAliasManager(database)
        analytics = UuidAnalytics(uuidCreator.repository)

        // 3. Start monitoring
        setupMonitoring()
    }

    /**
     * Scan Instagram app and generate UUIDs.
     */
    suspend fun scanInstagram(rootNode: AccessibilityNodeInfo) {
        val generator = ThirdPartyUuidGenerator(applicationContext)

        // Generate UUIDs for entire app
        val uuids = generator.generateUuidsForTree(
            rootNode = rootNode,
            packageName = "com.instagram.android"
        )

        println("Generated ${uuids.size} UUIDs for Instagram")

        // Register all elements
        uuids.forEach { (node, uuid) ->
            val element = UUIDElement(
                uuid = uuid,
                name = node.text?.toString() ?: node.contentDescription?.toString(),
                type = detectType(node),
                position = extractPosition(node)
            )

            uuidCreator.registry.register(element)

            // Create alias
            val alias = aliasManager.createAutoAlias(
                uuid = uuid,
                elementName = element.name,
                elementType = element.type
            )

            println("Registered: $alias → $uuid")
        }
    }

    /**
     * Handle voice command.
     */
    suspend fun handleVoiceCommand(command: String) {
        // Parse command: "click instagram like"
        val parts = command.split(" ")
        val action = parts[0]  // "click"
        val target = parts.drop(1).joinToString("_")  // "instagram_like"

        // Resolve alias
        val uuid = aliasManager.resolveAlias(target)

        if (uuid != null) {
            // Track access
            analytics.trackAccess(uuid)

            // Execute action
            val startTime = System.currentTimeMillis()
            val success = executeAction(uuid, action)
            val endTime = System.currentTimeMillis()

            // Track execution
            analytics.trackExecution(
                uuid = uuid,
                action = action,
                executionTimeMs = endTime - startTime,
                success = success
            )

            if (success) {
                println("✅ Executed: $command")
            } else {
                println("❌ Failed: $command")
            }
        } else {
            println("❌ Unknown command: $command")
        }
    }

    /**
     * Setup monitoring.
     */
    private fun setupMonitoring() {
        val monitor = CollisionMonitor(
            repository = uuidCreator.repository,
            scope = GlobalScope
        )

        monitor.startMonitoring(intervalMinutes = 60)

        GlobalScope.launch {
            monitor.collisions.collect { event ->
                Log.w("Collision", "Event: $event")
            }
        }
    }

    /**
     * Helper: Detect element type.
     */
    private fun detectType(node: AccessibilityNodeInfo): String {
        val className = node.className?.toString() ?: return "unknown"

        return when {
            className.contains("Button") -> "button"
            className.contains("TextView") -> "text"
            className.contains("EditText") -> "input"
            className.contains("ImageView") -> "image"
            else -> "view"
        }
    }

    /**
     * Helper: Extract position.
     */
    private fun extractPosition(node: AccessibilityNodeInfo): UUIDPosition {
        val rect = android.graphics.Rect()
        node.getBoundsInScreen(rect)

        return UUIDPosition(
            x = rect.left.toFloat(),
            y = rect.top.toFloat(),
            index = 0
        )
    }

    /**
     * Helper: Execute action.
     */
    private suspend fun executeAction(uuid: String, action: String): Boolean {
        val element = uuidCreator.registry.findByUUID(uuid) ?: return false

        return uuidCreator.executeAction(
            uuid = uuid,
            action = action,
            parameters = emptyMap()
        )
    }
}
```

---

**End of Comprehensive Developer Manual**

**Total Lines**: 1,500+
**Covers**: All features from novice to expert
**Format**: VoiceOS Coding Book Ready

🤖 Generated with [Claude Code](https://claude.com/claude-code)

Co-Authored-By: Claude <noreply@anthropic.com>
