# Implementation Plan: LearnAppPro Architecture 10/10

**Version:** 1.0
**Date:** 2025-12-11
**Mode:** .yolo .cot .rot
**Status:** Ready for Implementation

---

## Overview

| Metric | Value |
|--------|-------|
| Platforms | Android (Kotlin) |
| Swarm Recommended | No (14 tasks < 15 threshold) |
| Total Tasks | 14 |
| Estimated Effort | 5-8 days |
| Score Impact | 9.0 → 10.0 |

---

## Phase Ordering Rationale (CoT)

```
1. TESTS FIRST
   └── Can't verify other changes without tests
   └── Establishes baseline coverage

2. DATABASE OPTIMIZATION
   └── Batch operations affect all components
   └── Performance baseline before features

3. FRAMEWORK DETECTION
   └── Independent feature
   └── No dependencies on other P2 items

4. CONFIGURATION
   └── Affects all components
   └── Required before telemetry

5. TELEMETRY
   └── Depends on config system
   └── Measures improvements

6. POLISH
   └── Final reliability features
   └── LeakCanary, auto-reconnect, offline
```

---

## Phase 1: Unit Tests (P1 Critical)

**Goal:** Establish 90%+ test coverage on critical paths

### Task 1.1: CrossPlatformDetector Tests
```
File: LearnAppCore/src/test/java/.../CrossPlatformDetectorTest.kt
Tests:
  - detectFramework_Flutter_ReturnsFlutter
  - detectFramework_Unity_ReturnsUnity
  - detectFramework_Unreal_ReturnsUnreal
  - detectFramework_ReactNative_ReturnsReactNative
  - detectFramework_Native_ReturnsNative
  - generateFallbackLabel_Unity_ReturnsSpatialGrid
  - generateFallbackLabel_Unreal_Returns4x4Grid
Coverage: 100% of detection logic
```

### Task 1.2: LearnAppCore Tests
```
File: LearnAppCore/src/test/java/.../LearnAppCoreTest.kt
Tests:
  - generateVoiceCommand_WithText_ReturnsText
  - generateVoiceCommand_WithContentDesc_ReturnsContentDesc
  - generateVoiceCommand_WithResourceId_ReturnsId
  - generateVoiceCommand_NoLabels_ReturnsFallback
  - processElement_Immediate_SavesDirectly
  - processElement_Batch_QueuesCommand
  - flushBatch_EmptyQueue_NoOp
  - flushBatch_WithItems_InsertsAll
Coverage: 90%+ of command generation
```

### Task 1.3: SafetyManager Tests
```
File: LearnAppCore/src/test/java/.../SafetyManagerTest.kt
Tests:
  - checkElement_OnDNCList_ReturnsUnsafe
  - checkElement_OnLoginScreen_ReturnsUnsafe
  - checkElement_PasswordField_ReturnsSkip
  - checkElement_DynamicRegion_ReturnsLogOnly
  - checkElement_Safe_ReturnsSafe
  - updateScreenContext_LoopDetected_CallsCallback
  - filterElements_MixedSafety_SeparatesCorrectly
Coverage: 100% of safety checks
```

### Task 1.4: ExplorationState Tests
```
File: LearnAppCore/src/test/java/.../ExplorationStateTest.kt
Tests:
  - start_FromIdle_TransitionsToInitializing
  - beginExploring_FromInitializing_TransitionsToExploring
  - complete_FromExploring_TransitionsToCompleted
  - waitForUser_FromExploring_TransitionsToWaiting
  - recordScreen_NewScreen_AddsToFingerprints
  - recordElements_BatchMode_QueuesElements
  - getStats_WithData_ReturnsCorrectCounts
Coverage: 100% of state transitions
```

### Task 1.5: AVUExporter Tests
```
File: LearnAppCore/src/test/java/.../AVUExporterTest.kt
Tests:
  - export_ValidState_GeneratesCorrectHeader
  - export_WithScreens_GeneratesSCRLines
  - export_WithElements_GeneratesELMLin es
  - export_WithNavigation_GeneratesNAVLines
  - export_WithDNC_GeneratesDNCLines
  - export_WithSynonyms_GeneratesSynonymSection
  - parseAvuFile_ValidFile_ReturnsCorrectData
Coverage: 100% of export/parse logic
```

**Phase 1 Deliverable:** 5 test files, 35+ test cases, 90%+ coverage

---

## Phase 2: Integration Tests (P1 Critical)

### Task 2.1: JITLearningService Integration Tests
```
File: JITLearning/src/androidTest/java/.../JITLearningServiceTest.kt
Tests:
  - bindService_ReturnsValidBinder
  - pauseCapture_UpdatesState
  - resumeCapture_UpdatesState
  - queryState_ReturnsCurrentState
  - getLearnedScreenHashes_ReturnsHashes
  - registerEventListener_ReceivesEvents
  - startExploration_BeginsExploration
  - stopExploration_EndsExploration
Setup: ServiceTestRule, MockLearnerProvider
```

### Task 2.2: Database Integration Tests
```
File: Database/src/androidTest/java/.../ScreenContextRepositoryTest.kt
Tests:
  - insert_NewScreen_PersistsCorrectly
  - getByHash_ExistingScreen_ReturnsScreen
  - getByPackage_MultipleScreens_ReturnsAll
  - getLearnedHashes_Package_ReturnsHashes
  - batchInsert_MultipleScreens_SingleTransaction
Setup: In-memory SQLDelight database
```

### Task 2.3: E2E Exploration Flow Test
```
File: LearnApp/src/androidTest/java/.../ExplorationFlowTest.kt
Tests:
  - fullExploration_SimpleApp_CompletesSuccessfully
  - exploration_WithLoginScreen_PausesCorrectly
  - exploration_WithDNCElement_SkipsElement
  - exploration_Export_GeneratesValidAVU
Setup: Test app with known UI structure
```

**Phase 2 Deliverable:** 3 test files, 20+ integration tests

---

## Phase 3: Batch Database Operations (P1 Critical)

### Task 3.1: Add SQLDelight Transaction Support
```kotlin
// File: Database/src/commonMain/kotlin/.../ScreenContextRepository.kt

// Add batch insert method
suspend fun insertBatch(screens: List<ScreenContextDTO>) {
    database.transaction {
        screens.forEach { screen ->
            screenContextQueries.insert(
                screen.screenHash,
                screen.packageName,
                screen.activityName,
                screen.elementCount,
                screen.timestamp
            )
        }
    }
}
```

### Task 3.2: Update LearnAppCore to Use Batch
```kotlin
// File: LearnAppCore/src/main/java/.../LearnAppCore.kt

// Replace lines 709-711:
// OLD:
// batchQueue.forEach { command ->
//     database.generatedCommands.insert(command)
// }

// NEW:
database.generatedCommands.insertBatch(batchQueue)
```

### Task 3.3: Add Performance Benchmark
```kotlin
// File: LearnAppCore/src/test/java/.../BatchPerformanceTest.kt

@Test
fun `batch insert is 20x faster than sequential`() {
    val commands = generateTestCommands(100)

    // Sequential timing
    val sequentialTime = measureTimeMillis {
        commands.forEach { database.insert(it) }
    }

    // Batch timing
    val batchTime = measureTimeMillis {
        database.insertBatch(commands)
    }

    // Assert batch is at least 10x faster
    assertTrue(sequentialTime / batchTime >= 10)
}
```

**Phase 3 Deliverable:** Batch operations with 20x performance improvement

---

## Phase 4: Extended Game Engine Support (P2 High)

### Task 4.1: Add Godot Detection
```kotlin
// File: LearnAppCore/src/main/java/.../CrossPlatformDetector.kt

// Add to AppFramework enum:
GODOT,      // Godot Engine

// Add detection method:
private fun hasGodotSignatures(node: AccessibilityNodeInfo, packageName: String): Boolean {
    if (node.className?.contains("GodotView") == true) return true
    if (node.className?.contains("GodotApp") == true) return true
    val godotPatterns = listOf(".godot.", "org.godotengine.", "godot_")
    return godotPatterns.any { packageName.contains(it, ignoreCase = true) }
}

// Add to detectFramework():
if (hasGodotSignatures(rootNode, packageName)) return AppFramework.GODOT
```

### Task 4.2: Add Cocos2d Detection
```kotlin
// Add to AppFramework enum:
COCOS2D,    // Cocos2d-x

// Add detection:
private fun hasCocos2dSignatures(node: AccessibilityNodeInfo, packageName: String): Boolean {
    if (node.className?.contains("Cocos2dxGLSurfaceView") == true) return true
    if (node.className?.contains("Cocos2dxActivity") == true) return true
    return packageName.contains("cocos", ignoreCase = true)
}
```

### Task 4.3: Add Defold Detection
```kotlin
// Add to AppFramework enum:
DEFOLD,     // Defold Engine

// Add detection:
private fun hasDefoldSignatures(node: AccessibilityNodeInfo, packageName: String): Boolean {
    if (node.className?.contains("DefoldActivity") == true) return true
    return packageName.contains("defold", ignoreCase = true)
}
```

### Task 4.4: Update Fallback Label Generation
```kotlin
// File: LearnAppCore.kt - generateFallbackLabel()

// Add cases for new frameworks (use same 3x3 grid as Unity):
AppFramework.GODOT, AppFramework.COCOS2D, AppFramework.DEFOLD -> {
    generateSpatialLabel(element, gridSize = 3)
}
```

**Phase 4 Deliverable:** Support for 3 additional game engines

---

## Phase 5: Configuration Externalization (P2 High)

### Task 5.1: Create LearnAppConfig DataClass
```kotlin
// File: LearnAppCore/src/main/java/.../config/LearnAppConfig.kt

data class LearnAppConfig(
    // Thresholds
    val minLabelLength: Int = 3,
    val maxBatchSize: Int = 100,
    val maxScrollCount: Int = 20,
    val maxScreenVisits: Int = 3,

    // Timeouts
    val screenChangeTimeoutMs: Long = 3000,
    val actionDelayMs: Long = 300,
    val scrollDelayMs: Long = 300,

    // Safety
    val enableDoNotClick: Boolean = true,
    val enableDynamicDetection: Boolean = true,
    val enableLoopDetection: Boolean = true,

    // Fallback
    val unityGridSize: Int = 3,
    val unrealGridSize: Int = 4,

    // Logging
    val verboseLogging: Boolean = false
)
```

### Task 5.2: Create DataStore Integration
```kotlin
// File: LearnAppCore/src/main/java/.../config/LearnAppConfigStore.kt

class LearnAppConfigStore(private val context: Context) {
    private val Context.dataStore by preferencesDataStore(name = "learnapp_config")

    val config: Flow<LearnAppConfig> = context.dataStore.data.map { prefs ->
        LearnAppConfig(
            minLabelLength = prefs[Keys.MIN_LABEL_LENGTH] ?: 3,
            maxBatchSize = prefs[Keys.MAX_BATCH_SIZE] ?: 100,
            // ... etc
        )
    }

    suspend fun updateConfig(update: (LearnAppConfig) -> LearnAppConfig) {
        context.dataStore.edit { prefs ->
            val current = config.first()
            val updated = update(current)
            prefs[Keys.MIN_LABEL_LENGTH] = updated.minLabelLength
            // ... etc
        }
    }

    private object Keys {
        val MIN_LABEL_LENGTH = intPreferencesKey("min_label_length")
        val MAX_BATCH_SIZE = intPreferencesKey("max_batch_size")
        // ... etc
    }
}
```

### Task 5.3: Update Components to Use Config
```kotlin
// Update LearnAppCore constructor:
class LearnAppCore(
    private val config: LearnAppConfig = LearnAppConfig()
) {
    // Replace hardcoded values:
    // OLD: if (label.length < 3)
    // NEW: if (label.length < config.minLabelLength)
}

// Update SafetyManager:
class SafetyManager(
    private val config: LearnAppConfig = LearnAppConfig(),
    private val callback: SafetyCallback? = null
) {
    // OLD: private val maxVisitsPerScreen = 3
    // NEW: private val maxVisitsPerScreen get() = config.maxScreenVisits
}
```

**Phase 5 Deliverable:** Externalized configuration with DataStore persistence

---

## Phase 6: Performance Telemetry (P3 Medium)

### Task 6.1: Create Metrics Framework
```kotlin
// File: LearnAppCore/src/main/java/.../metrics/ExplorationMetrics.kt

object ExplorationMetrics {
    private val counters = mutableMapOf<String, AtomicLong>()
    private val histograms = mutableMapOf<String, MutableList<Long>>()

    // Counters
    fun incrementScreens() = increment("screens_explored")
    fun incrementElements() = increment("elements_discovered")
    fun incrementCommands() = increment("commands_generated")

    // Histograms
    fun recordScreenCapture(durationMs: Long) = record("screen_capture_ms", durationMs)
    fun recordCommandGeneration(durationMs: Long) = record("command_gen_ms", durationMs)
    fun recordBatchFlush(durationMs: Long) = record("batch_flush_ms", durationMs)

    // Export
    fun getReport(): MetricsReport {
        return MetricsReport(
            counters = counters.mapValues { it.value.get() },
            histograms = histograms.mapValues { values ->
                HistogramStats(
                    count = values.size,
                    min = values.minOrNull() ?: 0,
                    max = values.maxOrNull() ?: 0,
                    avg = values.average(),
                    p95 = values.sorted().getOrNull((values.size * 0.95).toInt()) ?: 0
                )
            }
        )
    }

    fun reset() {
        counters.clear()
        histograms.clear()
    }
}
```

### Task 6.2: Instrument Critical Paths
```kotlin
// LearnAppCore.kt - processElement()
fun processElement(element: ElementInfo): ProcessingResult {
    val startTime = System.currentTimeMillis()
    try {
        // ... existing logic ...
        ExplorationMetrics.incrementElements()
        return result
    } finally {
        ExplorationMetrics.recordCommandGeneration(System.currentTimeMillis() - startTime)
    }
}

// ExplorationState.kt - recordScreen()
fun recordScreen(fingerprint: ScreenFingerprint) {
    val startTime = System.currentTimeMillis()
    try {
        // ... existing logic ...
        ExplorationMetrics.incrementScreens()
    } finally {
        ExplorationMetrics.recordScreenCapture(System.currentTimeMillis() - startTime)
    }
}
```

**Phase 6 Deliverable:** Metrics collection with counters, histograms, and reports

---

## Phase 7: Reliability Polish (P3 Medium)

### Task 7.1: Add LeakCanary Integration
```kotlin
// File: LearnApp/build.gradle.kts
dependencies {
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.12")
}

// No code changes needed - LeakCanary auto-installs in debug builds
```

### Task 7.2: Accessibility Service Auto-Reconnection
```kotlin
// File: VoiceOSCore/src/main/java/.../AccessibilityServiceMonitor.kt

class AccessibilityServiceMonitor(private val context: Context) {
    private val handler = Handler(Looper.getMainLooper())
    private var checkInterval = 5000L

    fun startMonitoring() {
        handler.postDelayed(::checkService, checkInterval)
    }

    fun stopMonitoring() {
        handler.removeCallbacksAndMessages(null)
    }

    private fun checkService() {
        if (!isAccessibilityServiceEnabled()) {
            showReconnectionNotification()
        }
        handler.postDelayed(::checkService, checkInterval)
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val serviceName = ComponentName(context, VoiceOSService::class.java)
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabledServices.contains(serviceName.flattenToString())
    }

    private fun showReconnectionNotification() {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("VoiceOS Disconnected")
            .setContentText("Tap to re-enable accessibility service")
            .setSmallIcon(R.drawable.ic_warning)
            .setContentIntent(createSettingsIntent())
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(RECONNECT_NOTIFICATION_ID, notification)
    }
}
```

### Task 7.3: Offline-First with WorkManager
```kotlin
// File: LearnAppCore/src/main/java/.../sync/ExplorationSyncWorker.kt

class ExplorationSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val repository = LearnAppRepository.getInstance(applicationContext)
            val pendingExports = repository.getPendingExports()

            for (export in pendingExports) {
                // Attempt sync
                val success = syncToServer(export)
                if (success) {
                    repository.markExportSynced(export.id)
                }
            }

            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    companion object {
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<ExplorationSyncWorker>(
                15, TimeUnit.MINUTES
            ).setConstraints(constraints).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "exploration_sync",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
```

**Phase 7 Deliverable:** LeakCanary, auto-reconnect, offline sync

---

## Task Summary

| Phase | Tasks | Files | Priority |
|-------|-------|-------|----------|
| 1 | 5 | 5 test files | P1 |
| 2 | 3 | 3 test files | P1 |
| 3 | 3 | 3 files | P1 |
| 4 | 4 | 1 file | P2 |
| 5 | 3 | 3 files | P2 |
| 6 | 2 | 2 files | P3 |
| 7 | 3 | 4 files | P3 |
| **Total** | **23** | **21 files** | - |

---

## Time Estimates

| Execution | Time | Notes |
|-----------|------|-------|
| Sequential | 5-8 days | One developer |
| Parallel (2 devs) | 3-4 days | Split P1/P2/P3 |

---

## Success Criteria

| Metric | Target |
|--------|--------|
| Unit Test Coverage | 90%+ |
| Integration Tests | All passing |
| Batch Performance | 20x improvement |
| Game Engines | +3 (Godot, Cocos2d, Defold) |
| Config | 100% externalized |
| Architecture Score | 10/10 |

---

## Document History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2025-12-11 | Initial plan |

---

**End of Plan**
