# VoiceOSCoreNG Migration Guide

**Version:** 2.0.0
**Date:** 2026-01-06
**From:** VoiceOSCore/learnapp, LearnAppCore, JITLearning
**To:** VoiceOSCoreNG

---

## Table of Contents

1. [Overview](#1-overview)
2. [Breaking Changes](#2-breaking-changes)
3. [Step-by-Step Migration](#3-step-by-step-migration)
4. [Type Mappings](#4-type-mappings)
5. [API Changes](#5-api-changes)
6. [Package Changes](#6-package-changes)
7. [Feature Mapping](#7-feature-mapping)
8. [Deprecation Timeline](#8-deprecation-timeline)
9. [Common Migration Patterns](#9-common-migration-patterns)
10. [Troubleshooting](#10-troubleshooting)

---

## 1. Overview

VoiceOSCoreNG consolidates three legacy modules into a single KMP (Kotlin Multiplatform) module:

| Legacy Module | Purpose | Status in VoiceOSCoreNG |
|---------------|---------|------------------------|
| VoiceOSCore/learnapp | Android-specific learning | Merged into androidMain |
| LearnAppCore | Shared learning logic | Merged into commonMain |
| JITLearning | Just-in-time processing | Merged into commonMain + androidMain |

### Benefits of Migration

- **Single Dependency**: Replace 3 modules with 1
- **Cross-Platform**: KMP support for Android, iOS, Desktop
- **Smaller Footprint**: Compact VUID (16 chars vs 36)
- **Feature Tiers**: Lite/Dev modes with runtime switching
- **Modern Architecture**: Coroutines, Flow, immutable data

---

## 2. Breaking Changes

### 2.1 UUID to VUID

**The most significant change.** All UUID-based identifiers are replaced with VUID.

| Change | Old | New |
|--------|-----|-----|
| Type name | `UUID` | `VUID` |
| Format | 36 chars (8-4-4-4-12) | 16 chars (6-9) |
| Generator | `UUID.randomUUID()` | `VUIDGenerator.generate()` |
| Property names | `uuid`, `elementUuid` | `vuid`, `elementVuid` |

### 2.2 Package Changes

```
OLD: com.augmentalis.voiceos.core.*
     com.augmentalis.voiceos.learnapp.*
     com.augmentalis.learnappcore.*
     com.augmentalis.jitlearning.*

NEW: com.augmentalis.voiceoscoreng.*
```

### 2.3 Removed Dependencies

- Room Database (replaced with SQLDelight via adapters)
- Legacy Android-specific classes
- Deprecated learning APIs

### 2.4 API Signature Changes

Many methods now use:
- `suspend` functions instead of callbacks
- `Flow` instead of LiveData
- `Result<T>` instead of nullable returns
- Immutable data classes instead of mutable

---

## 3. Step-by-Step Migration

### Step 1: Update Dependencies

```kotlin
// settings.gradle.kts
// REMOVE:
// include(":VoiceOSCore")
// include(":LearnAppCore")
// include(":JITLearning")

// ADD:
include(":Modules:VoiceOSCoreNG")
```

```kotlin
// app/build.gradle.kts
dependencies {
    // REMOVE:
    // implementation(project(":VoiceOSCore"))
    // implementation(project(":LearnAppCore"))
    // implementation(project(":JITLearning"))

    // ADD:
    implementation(project(":Modules:VoiceOSCoreNG"))
}
```

### Step 2: Update Imports

```kotlin
// BEFORE
import com.augmentalis.voiceos.core.VoiceOSCore
import com.augmentalis.voiceos.learnapp.LearnAppManager
import com.augmentalis.learnappcore.model.ElementData
import com.augmentalis.learnappcore.model.CommandData
import com.augmentalis.jitlearning.JitProcessor
import java.util.UUID

// AFTER
import com.augmentalis.voiceoscoreng.core.VoiceOSCoreNG
import com.augmentalis.voiceoscoreng.features.LearnAppConfig
import com.augmentalis.voiceoscoreng.features.LearnAppDevToggle
import com.augmentalis.voiceoscoreng.common.ElementInfo
import com.augmentalis.voiceoscoreng.avu.QuantizedCommand
import com.augmentalis.voiceoscoreng.common.VUIDGenerator
```

### Step 3: Update Initialization

```kotlin
// BEFORE (Application.onCreate)
VoiceOSCore.initialize(this)
LearnAppManager.getInstance().init(this)
JitProcessor.initialize(context)

// AFTER
VoiceOSCoreNG.initialize(
    tier = LearnAppDevToggle.Tier.LITE,
    isDebug = BuildConfig.DEBUG
)
```

### Step 4: Update ID Generation

```kotlin
// BEFORE
val uuid = UUID.randomUUID().toString()
val elementId = generateUUID(element)

// AFTER
val vuid = VUIDGenerator.generate(
    packageName = "com.example.app",
    typeCode = VUIDGenerator.getTypeCode(element.className),
    elementHash = element.resourceId
)
```

### Step 5: Update Data Models

```kotlin
// BEFORE
data class ElementData(
    val uuid: String,
    val className: String,
    val text: String?,
    val resourceId: String?,
    val contentDescription: String?,
    val bounds: Rect,
    val clickable: Boolean,
    val scrollable: Boolean
)

// AFTER
// Use ElementInfo directly
val element = ElementInfo(
    className = "Button",
    text = "Submit",
    resourceId = "com.app:id/submit",
    contentDescription = "",
    bounds = Bounds(0, 0, 200, 50),
    isClickable = true,
    isScrollable = false,
    isEnabled = true,
    packageName = "com.example.app"
)
```

### Step 6: Update Repository Usage

```kotlin
// BEFORE (Room-based)
@Dao
interface CommandDao {
    @Query("SELECT * FROM commands WHERE packageName = :pkg")
    fun getByPackage(pkg: String): LiveData<List<CommandEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(command: CommandEntity)
}

// AFTER (Repository interface)
val commands = commandRepository.getByApp(packageName)

commandRepository.observeByScreen(packageName, screenId)
    .collect { commands -> updateUI(commands) }

commandRepository.save(command)
```

### Step 7: Update Coroutine Usage

```kotlin
// BEFORE (callbacks)
learnApp.processElements(elements, object : ProcessingCallback {
    override fun onSuccess(results: List<ProcessedElement>) { }
    override fun onError(error: Exception) { }
})

// AFTER (coroutines)
viewModelScope.launch {
    val result = processElements(elements)
    result.fold(
        onSuccess = { processed -> updateUI(processed) },
        onFailure = { error -> showError(error) }
    )
}
```

---

## 4. Type Mappings

### 4.1 Data Types

| Old Type | New Type | Notes |
|----------|----------|-------|
| `UUID` | `VUID` (String) | 16-char compact format |
| `ElementData` | `ElementInfo` | Immutable data class |
| `CommandData` | `QuantizedCommand` | AVU-compatible |
| `ProcessedElement` | `QuantizedElement` | Compact representation |
| `Rect` | `Bounds` | Custom KMP type |
| `LiveData<T>` | `Flow<T>` | Coroutines-based |
| `Callback<T>` | `suspend fun(): Result<T>` | Structured concurrency |

### 4.2 Enum Mappings

```kotlin
// Element Types
OLD: ElementType.BUTTON, INPUT, TEXT, CONTAINER, ...
NEW: ElementType.BUTTON, TEXT_FIELD, TEXT, CONTAINER, ...

// Action Types
OLD: ActionType.CLICK, LONG_CLICK, SCROLL, TYPE, ...
NEW: CommandActionType.CLICK, LONG_CLICK, TYPE, NAVIGATE, CUSTOM

// Framework Types
OLD: FrameworkType.NATIVE, FLUTTER, UNITY, ...
NEW: FrameworkType.NATIVE, FLUTTER, UNITY, UNREAL_ENGINE, REACT_NATIVE, WEBVIEW, UNKNOWN
```

### 4.3 ID Format Mappings

| Context | Old Format | New Format |
|---------|------------|------------|
| Element ID | `550e8400-e29b-41d4-a716-446655440000` | `a3f2e1-b917cc9dc` |
| Command ID | UUID v4 (36 chars) | VUID (16 chars) |
| Screen ID | Arbitrary string | `{packageHash}-{screenHash}` |
| Message ID | UUID | `ava:elm:a7f3e2c1` |

---

## 5. API Changes

### 5.1 Initialization

```kotlin
// OLD
VoiceOSCore.initialize(context)
VoiceOSCore.getInstance().setDebugMode(true)

// NEW
VoiceOSCoreNG.initialize(
    tier = LearnAppDevToggle.Tier.LITE,
    isDebug = BuildConfig.DEBUG,
    enableTestMode = false
)
```

### 5.2 Configuration

```kotlin
// OLD
LearnAppConfig.setMaxElements(100)
LearnAppConfig.setEnableAI(true)
JitProcessor.setMode(JitMode.IMMEDIATE)

// NEW
VoiceOSCoreNG.configureLimits(
    maxElementsPerScan = 100,
    maxAppsLearned = 50
)
VoiceOSCoreNG.configureFeatures(
    enableAI = true
)
VoiceOSCoreNG.setProcessingMode(LearnAppConfig.ProcessingMode.IMMEDIATE)
```

### 5.3 Feature Checking

```kotlin
// OLD
if (LearnAppConfig.isFeatureEnabled("ai_classification")) { }
if (BuildConfig.DEBUG || isPremium) { }

// NEW
if (LearnAppDevToggle.isEnabled(LearnAppDevToggle.Feature.AI_CLASSIFICATION)) { }
LearnAppDevToggle.ifEnabled(LearnAppDevToggle.Feature.AI_CLASSIFICATION) {
    // AI code here
}
```

### 5.4 Element Processing

```kotlin
// OLD
val processor = ElementProcessor(context)
val results = processor.process(accessibilityNode)
results.forEach { element ->
    val uuid = element.uuid
    val command = CommandBuilder.build(element)
    dao.insert(command.toEntity())
}

// NEW
val elements = PlatformExtractor.extract(accessibilityNode)
elements.forEach { element ->
    val command = CommandGenerator.fromElement(element, packageName)
    command?.let { commandRepository.save(it) }
}
```

### 5.5 Command Generation

```kotlin
// OLD
val command = CommandBuilder()
    .setTrigger("click submit")
    .setAction(ActionType.CLICK)
    .setTarget(elementUuid)
    .setConfidence(0.95f)
    .build()

// NEW
val command = CommandGenerator.fromElement(element, packageName)
// Or manually:
val command = QuantizedCommand(
    phrase = "click submit",
    actionType = CommandActionType.CLICK,
    targetVuid = vuid,
    confidence = 0.95f
)
```

### 5.6 Framework Detection

```kotlin
// OLD
val framework = FrameworkAnalyzer.detect(packageInfo)

// NEW
val framework = FrameworkDetector.detect(
    packageName = packageInfo.packageName,
    classNames = extractClassNames(packageInfo)
)
```

---

## 6. Package Changes

### 6.1 Full Package Mapping

| Old Package | New Package |
|-------------|-------------|
| `com.augmentalis.voiceos.core` | `com.augmentalis.voiceoscoreng.core` |
| `com.augmentalis.voiceos.core.model` | `com.augmentalis.voiceoscoreng.common` |
| `com.augmentalis.voiceos.core.processing` | `com.augmentalis.voiceoscoreng.extraction` |
| `com.augmentalis.voiceos.core.command` | `com.augmentalis.voiceoscoreng.command` |
| `com.augmentalis.voiceos.core.framework` | `com.augmentalis.voiceoscoreng.handlers` |
| `com.augmentalis.voiceos.learnapp` | `com.augmentalis.voiceoscoreng.features` |
| `com.augmentalis.voiceos.learnapp.ui` | `com.augmentalis.voiceoscoreng.ui` |
| `com.augmentalis.learnappcore.model` | `com.augmentalis.voiceoscoreng.common` |
| `com.augmentalis.learnappcore.repository` | `com.augmentalis.voiceoscoreng.repository` |
| `com.augmentalis.learnappcore.serialization` | `com.augmentalis.voiceoscoreng.avu` |
| `com.augmentalis.jitlearning` | `com.augmentalis.voiceoscoreng.jit` (androidMain) |
| `com.augmentalis.jitlearning.config` | `com.augmentalis.voiceoscoreng.features` |

### 6.2 Class Renames

| Old Class | New Class |
|-----------|-----------|
| `VoiceOSCore` | `VoiceOSCoreNG` |
| `LearnAppManager` | Merged into `VoiceOSCoreNG` |
| `ElementData` | `ElementInfo` |
| `CommandData` | `QuantizedCommand` |
| `ProcessedElement` | `QuantizedElement` |
| `ScreenData` | `QuantizedScreen` |
| `ElementProcessor` | `ElementParser` + `PlatformExtractor` |
| `CommandBuilder` | `CommandGenerator` |
| `FrameworkAnalyzer` | `FrameworkDetector` |
| `JitProcessor` | `JitProcessor` (androidMain) |
| `LearnAppConfig` | `LearnAppConfig` (reorganized) |
| `UUIDGenerator` | `VUIDGenerator` |

---

## 7. Feature Mapping

### 7.1 Legacy Features to New Tiers

| Legacy Feature | New Location | Tier |
|----------------|--------------|------|
| Basic scraping | `LearnAppDevToggle.Feature.ELEMENT_SCRAPING` | Lite |
| ID generation | `LearnAppDevToggle.Feature.VUID_GENERATION` | Lite |
| Voice commands | `LearnAppDevToggle.Feature.VOICE_COMMANDS` | Lite |
| JIT processing | `LearnAppDevToggle.Feature.JIT_PROCESSING` | Lite |
| Full exploration | `LearnAppDevToggle.Feature.EXPLORATION_MODE` | Dev |
| Batch processing | `LearnAppDevToggle.Feature.BATCH_PROCESSING` | Dev |
| Flutter detection | `LearnAppDevToggle.Feature.FLUTTER_DETECTION` | Dev |
| Unity detection | `LearnAppDevToggle.Feature.UNITY_DETECTION` | Dev |
| AI classification | `LearnAppDevToggle.Feature.AI_CLASSIFICATION` | Dev |
| Debug overlay | `LearnAppDevToggle.Feature.DEBUG_OVERLAY` | Dev |
| Analytics | `LearnAppDevToggle.Feature.USAGE_ANALYTICS` | Dev |

### 7.2 Configuration Mapping

| Legacy Config | New Config |
|---------------|------------|
| `MAX_ELEMENTS` | `LearnAppConfig.LiteDefaults.MAX_ELEMENTS_PER_SCAN` (100) |
| `MAX_APPS` | `LearnAppConfig.LiteDefaults.MAX_APPS_LEARNED` (25) |
| `BATCH_TIMEOUT` | `LearnAppConfig.LiteDefaults.BATCH_TIMEOUT_MS` (3000) |
| `EXPLORATION_DEPTH` | `LearnAppConfig.LiteDefaults.EXPLORATION_DEPTH` (5) |
| Premium values | `LearnAppConfig.DevDefaults.*` |

---

## 8. Deprecation Timeline

### Phase 1: Parallel Support (Complete - Jan 2026)

- Legacy modules still functional
- VoiceOSCoreNG available for new development
- Migration tools available
- **Status:** COMPLETE

### Phase 2: Soft Deprecation (Current - Jan 2026)

- Legacy modules marked `@Deprecated` with DEPRECATED.md files
- Compile-time warnings (see deprecation markers)
- Documentation points to VoiceOSCoreNG
- **Affected modules:**
  - `Modules/VoiceOS/libraries/LearnAppCore/DEPRECATED.md`
  - `Modules/VoiceOS/libraries/JITLearning/DEPRECATED.md`
  - `Modules/VoiceOS/apps/VoiceOSCore/src/main/java/.../learnapp/DEPRECATED.md`
- **Status:** COMPLETE (cc1f07b3)

### Phase 3: Hard Deprecation (Q2 2026)

- Legacy modules removed from default builds
- Available only via opt-in flag
- Migration required for new features

### Phase 4: Removal (Q3 2026)

- Legacy modules removed entirely
- All code must use VoiceOSCoreNG

---

## 9. Common Migration Patterns

### 9.1 Converting UUID to VUID

```kotlin
// Helper function for migration
fun migrateUuidToVuid(
    legacyUuid: String,
    element: ElementInfo,
    packageName: String
): String {
    // Option 1: Generate new VUID (recommended)
    return VUIDGenerator.generate(
        packageName = packageName,
        typeCode = VUIDGenerator.getTypeCode(element.className),
        elementHash = element.resourceId.ifBlank { element.text }
    )

    // Option 2: Keep mapping (for gradual migration)
    // Store: legacyUuid -> newVuid in migration table
}
```

### 9.2 Converting Callbacks to Coroutines

```kotlin
// BEFORE
fun processAsync(callback: ProcessingCallback) {
    thread {
        try {
            val result = heavyProcessing()
            mainHandler.post { callback.onSuccess(result) }
        } catch (e: Exception) {
            mainHandler.post { callback.onError(e) }
        }
    }
}

// AFTER
suspend fun process(): Result<ProcessingResult> = withContext(Dispatchers.Default) {
    try {
        Result.success(heavyProcessing())
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

### 9.3 Converting LiveData to Flow

```kotlin
// BEFORE
val commands: LiveData<List<Command>> = dao.observeCommands()
commands.observe(lifecycleOwner) { list ->
    updateUI(list)
}

// AFTER
val commands: Flow<List<QuantizedCommand>> = repository.observeByScreen(pkg, screen)
lifecycleScope.launch {
    commands.collect { list ->
        updateUI(list)
    }
}
```

### 9.4 Converting Room Entities

```kotlin
// BEFORE (Room Entity)
@Entity(tableName = "commands")
data class CommandEntity(
    @PrimaryKey val uuid: String,
    val trigger: String,
    val action: String,
    val targetUuid: String?,
    val confidence: Float,
    val packageName: String
)

// AFTER (Pure Kotlin data class + repository adapter)
data class QuantizedCommand(
    val uuid: String,
    val phrase: String,
    val actionType: CommandActionType,
    val targetVuid: String?,
    val confidence: Float,
    val metadata: Map<String, String>
)

// Use SQLDelightCommandRepositoryAdapter for persistence
```

### 9.5 Feature Flag Migration

```kotlin
// BEFORE
if (sharedPrefs.getBoolean("feature_ai", false)) {
    // AI feature
}

// AFTER
LearnAppDevToggle.ifEnabled(LearnAppDevToggle.Feature.AI_CLASSIFICATION) {
    // AI feature
}
```

---

## 10. Troubleshooting

### 10.1 Common Issues

#### Issue: "Unresolved reference: VoiceOSCore"

**Cause:** Old import still present
**Solution:** Update imports to `com.augmentalis.voiceoscoreng.*`

#### Issue: "Type mismatch: UUID expected, VUID found"

**Cause:** Code expects UUID format
**Solution:** Use `VUIDGenerator` and update type annotations

#### Issue: "Room database not found"

**Cause:** VoiceOSCoreNG doesn't use Room directly
**Solution:** Implement `ICommandRepository` with SQLDelight adapter

#### Issue: "LiveData cannot be collected"

**Cause:** Flow API requires different collection
**Solution:** Use `collect` instead of `observe`, provide CoroutineScope

### 10.2 Build Issues

```kotlin
// If Gradle can't resolve VoiceOSCoreNG
// Check settings.gradle.kts:
include(":Modules:VoiceOSCoreNG")

// Check module path matches actual folder structure
```

### 10.3 Runtime Issues

```kotlin
// If features behave unexpectedly
// Check initialization:
if (!VoiceOSCoreNG.isInitialized()) {
    VoiceOSCoreNG.initialize(tier, isDebug)
}

// Check current tier:
val tier = VoiceOSCoreNG.getCurrentTier()
Log.d("Migration", "Current tier: $tier")

// Print full config:
Log.d("Migration", VoiceOSCoreNG.getConfigSummary())
```

### 10.4 VUID Migration Issues

```kotlin
// If VUIDs don't match expected format
val format = VUIDGenerator.detectFormat(vuid)
when (format) {
    VuidFormat.LEGACY_UUID -> {
        // Old UUID format - needs migration
        val newVuid = generateNewVuid(element)
    }
    VuidFormat.LEGACY_VOICEOS -> {
        // Old VoiceOS format - can migrate
        val newVuid = VUIDGenerator.migrateToCompact(vuid)
    }
    VuidFormat.COMPACT -> {
        // Already new format - OK
    }
    else -> {
        // Unknown format
    }
}
```

---

## Migration Checklist

- [ ] Update `settings.gradle.kts` to include VoiceOSCoreNG
- [ ] Update app `build.gradle.kts` dependencies
- [ ] Update all imports from old packages to new
- [ ] Replace `UUID` with `VUID` throughout codebase
- [ ] Update initialization code in Application class
- [ ] Convert callbacks to coroutines
- [ ] Convert LiveData to Flow
- [ ] Update repository implementations
- [ ] Update feature flag checks
- [ ] Test Lite tier functionality
- [ ] Test Dev tier functionality (if applicable)
- [ ] Run all unit tests
- [ ] Run instrumentation tests
- [ ] Verify performance metrics

---

## Support

For migration assistance:
- Review API Reference: `VoiceOSCoreNG-API-Reference-60106-V1.md`
- Check README: `Modules/VoiceOSCoreNG/README.md`
- Contact: VoiceOS Development Team

---

**Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC**
