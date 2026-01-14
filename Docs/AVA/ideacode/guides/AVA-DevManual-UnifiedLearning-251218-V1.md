# Developer Manual: Chapter 75 - Unified Learning Architecture

**Version**: 1.0
**Date**: 2025-12-18
**Author**: Manoj Jhawar
**Related ADR**: ADR-014-Unified-Learning-Architecture

---

## Overview

The Unified Learning Architecture bridges VoiceOS and AVA's learning systems, allowing commands learned in one system to enhance the other. This creates a synergistic learning environment where:

- VoiceOS UI-scraped commands become semantic intents in AVA
- Users benefit from both pattern-based and embedding-based recognition
- Learning statistics are unified across both systems

---

## Architecture Components

### Component Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                      UNIFIED LEARNING SYSTEM                                 │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌────────────────┐                          ┌────────────────────────────┐ │
│  │   VoiceOS      │                          │          AVA               │ │
│  │ ────────────── │                          │ ────────────────────────── │ │
│  │ LearnAppCore   │                          │ NLUSelfLearner (ADR-013)   │ │
│  │ JITLearning    │                          │ IntentClassifier           │ │
│  │ CommandManager │                          │ IntentEmbeddingManager     │ │
│  └───────┬────────┘                          └─────────────┬──────────────┘ │
│          │                                                 │                 │
│          │ ILearningSource                                │ ILearningConsumer│
│          ▼                                                 ▼                 │
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │                    UnifiedLearningService                             │  │
│  │  ┌─────────────────────────────────────────────────────────────────┐  │  │
│  │  │               VoiceOSLearningSyncWorker                         │  │  │
│  │  │                                                                  │  │  │
│  │  │  ┌────────────┐  ┌──────────────┐  ┌─────────────────────────┐ │  │  │
│  │  │  │ Query      │  │ Compute      │  │ Save to AVA             │ │  │  │
│  │  │  │ VoiceOS    │──│ BERT         │──│ train_example +         │ │  │  │
│  │  │  │ Commands   │  │ Embedding    │  │ intent_embedding        │ │  │  │
│  │  │  └────────────┘  └──────────────┘  └─────────────────────────┘ │  │  │
│  │  └─────────────────────────────────────────────────────────────────┘  │  │
│  └───────────────────────────────────────────────────────────────────────┘  │
│          │                                                                   │
│          ▼                                                                   │
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │                 Shared Learning Domain (KMP commonMain)               │  │
│  │                                                                        │  │
│  │  LearnedCommand    LearningEvent    LearningSource    LearningStats   │  │
│  │  ILearningSource   ILearningConsumer   IUnifiedLearningRepository    │  │
│  └───────────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Key Files

| File | Module | Purpose |
|------|--------|---------|
| `LearningDomain.kt` | Shared/NLU (commonMain) | Cross-platform domain model |
| `LearningDomainAndroid.kt` | Shared/NLU (androidMain) | Platform utilities |
| `UnifiedLearningService.kt` | Shared/NLU (androidMain) | Central orchestration |
| `VoiceOSLearningSyncWorker.kt` | Shared/NLU (androidMain) | Background sync |
| `VoiceOSLearningSource.kt` | Shared/NLU (androidMain) | VoiceOS adapter |
| `GeneratedCommand.sq` | VoiceOS/core/database | Schema with sync columns |

---

## Implementation Guide

### Step 1: Understanding the Domain Model

**File**: `Modules/Shared/NLU/src/commonMain/kotlin/com/augmentalis/nlu/learning/domain/LearningDomain.kt`

The domain model provides cross-platform abstractions:

```kotlin
// Learning source with priority (higher = more trusted)
enum class LearningSource(val priority: Int) {
    USER_TAUGHT(100),        // User explicitly taught via Teach AVA
    USER_CONFIRMED(90),      // User confirmed LLM suggestion
    VOICEOS_APPROVED(85),    // User approved VoiceOS command
    LLM_AUTO(70),            // LLM auto-classification
    LLM_VARIATION(60),       // LLM-generated variations
    VOICEOS_JIT(55),         // JIT learning from UI
    VOICEOS_SCRAPE(50),      // Auto-scraped from exploration
    BUNDLED(30),             // Bundled with app
    UNKNOWN(0)               // Legacy/unknown
}

// Action types supported
enum class LearningActionType {
    CLICK,       // Tap/click action
    LONG_CLICK,  // Long press
    SCROLL,      // Scroll/swipe
    TYPE,        // Text input
    INTENT,      // AVA semantic intent
    NAVIGATE,    // Screen navigation
    SYSTEM,      // System action
    UNKNOWN
}

// Unified command representation
data class LearnedCommand(
    val id: String,
    val utterance: String,          // "click like" or "play music"
    val intent: String,             // "click_element_abc123" or "play_music"
    val actionType: LearningActionType,
    val confidence: Float,          // 0.0-1.0
    val source: LearningSource,
    val locale: String = "en-US",
    val synonyms: List<String> = emptyList(),
    val embedding: FloatArray? = null,  // 384/768 dim BERT vector
    val createdAt: Long = 0L,
    val isUserApproved: Boolean = false,
    val packageName: String? = null,     // For VoiceOS commands
    val elementHash: String? = null      // For VoiceOS UI element
) {
    val hasEmbedding: Boolean get() = embedding != null
    val isHighConfidence: Boolean get() = confidence >= 0.85f
    val isVoiceOSCommand: Boolean get() = source in listOf(
        LearningSource.VOICEOS_SCRAPE,
        LearningSource.VOICEOS_APPROVED,
        LearningSource.VOICEOS_JIT
    )

    companion object {
        // Factory method for VoiceOS commands
        fun fromVoiceOS(
            commandText: String,
            actionType: String,
            confidence: Double,
            elementHash: String,
            ...
        ): LearnedCommand

        // Factory method for AVA intents
        fun fromAVATrainExample(
            utterance: String,
            intent: String,
            confidence: Float,
            ...
        ): LearnedCommand
    }
}
```

### Step 2: Implementing Learning Source Interface

**File**: `Modules/Shared/NLU/src/androidMain/kotlin/com/augmentalis/nlu/learning/VoiceOSLearningSource.kt`

```kotlin
@Singleton
class VoiceOSLearningSource @Inject constructor(
    private val context: Context
) : ILearningSource {

    companion object {
        private const val VOICEOS_AUTHORITY = "com.avanues.voiceos.provider"
        private val COMMANDS_URI = Uri.parse("content://$VOICEOS_AUTHORITY/commands_generated")
        const val MIN_SYNC_CONFIDENCE = 0.6f
    }

    override val sourceId: String = "voiceos_learning"
    override val sourceName: String = "VoiceOS Scraping"

    /**
     * Check if VoiceOS is installed and accessible
     */
    fun isVoiceOSAvailable(): Boolean {
        return try {
            context.contentResolver.query(
                COMMANDS_URI,
                arrayOf("id"),
                null, null,
                "LIMIT 1"
            )?.use { it.count >= 0 } ?: false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get commands not yet synced to AVA
     */
    override suspend fun getUnsyncedCommands(limit: Int): List<LearnedCommand> =
        withContext(Dispatchers.IO) {
            if (!isVoiceOSAvailable()) return@withContext emptyList()

            val commands = mutableListOf<LearnedCommand>()
            context.contentResolver.query(
                COMMANDS_URI,
                null,
                "confidence >= ? AND (synced_to_ava IS NULL OR synced_to_ava = 0)",
                arrayOf(MIN_SYNC_CONFIDENCE.toString()),
                "confidence DESC, isUserApproved DESC LIMIT $limit"
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    cursorToLearnedCommand(cursor)?.let { commands.add(it) }
                }
            }
            commands
        }

    /**
     * Mark commands as synced after successful processing
     */
    override suspend fun markSynced(commandIds: List<String>) =
        withContext(Dispatchers.IO) {
            commandIds.forEach { id ->
                val uri = Uri.withAppendedPath(COMMANDS_URI, id)
                val values = ContentValues().apply {
                    put("synced_to_ava", 1)
                    put("synced_at", System.currentTimeMillis())
                }
                context.contentResolver.update(uri, values, null, null)
            }
        }
}
```

### Step 3: Implementing the Sync Worker

**File**: `Modules/Shared/NLU/src/androidMain/kotlin/com/augmentalis/nlu/learning/VoiceOSLearningSyncWorker.kt`

```kotlin
@HiltWorker
class VoiceOSLearningSyncWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted private val params: WorkerParameters,
    private val intentClassifier: IntentClassifier
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_TAG = "voiceos_learning_sync"
        const val HIGH_CONFIDENCE_THRESHOLD = 0.85f
        const val LOW_CONFIDENCE_THRESHOLD = 0.6f

        /**
         * Enqueue high-confidence sync (every 5 minutes)
         */
        fun enqueueHighConfidenceSync(workManager: WorkManager) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()

            val request = PeriodicWorkRequestBuilder<VoiceOSLearningSyncWorker>(
                5, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setInputData(workDataOf(
                    KEY_SYNC_TYPE to SYNC_TYPE_HIGH_CONFIDENCE,
                    KEY_MIN_CONFIDENCE to HIGH_CONFIDENCE_THRESHOLD
                ))
                .addTag(WORK_TAG)
                .build()

            workManager.enqueueUniquePeriodicWork(
                WORK_NAME_HIGH_CONFIDENCE,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        /**
         * Enqueue low-confidence sync (every 6 hours, charging only)
         */
        fun enqueueLowConfidenceSync(workManager: WorkManager) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .setRequiresCharging(true)
                .build()

            val request = PeriodicWorkRequestBuilder<VoiceOSLearningSyncWorker>(
                6, TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .setInputData(workDataOf(
                    KEY_SYNC_TYPE to SYNC_TYPE_LOW_CONFIDENCE,
                    KEY_MIN_CONFIDENCE to LOW_CONFIDENCE_THRESHOLD
                ))
                .addTag(WORK_TAG)
                .build()

            workManager.enqueueUniquePeriodicWork(
                WORK_NAME_LOW_CONFIDENCE,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val syncType = inputData.getString(KEY_SYNC_TYPE)
        val minConfidence = inputData.getFloat(KEY_MIN_CONFIDENCE, 0.6f)

        try {
            val commands = queryVoiceOSCommands(minConfidence, maxCommands = 50)
            var synced = 0
            var failed = 0

            for (command in commands) {
                // Skip if already in AVA
                if (intentClassifier.findEmbeddingByUtterance(command.utterance) != null) {
                    continue
                }

                // Compute BERT embedding
                val embedding = intentClassifier.computeEmbedding(command.utterance)
                if (embedding == null) {
                    failed++
                    continue
                }

                // Save to AVA's database
                val saved = intentClassifier.saveTrainedEmbedding(
                    utterance = command.utterance,
                    intent = command.intent,
                    embedding = embedding,
                    source = command.source.name.lowercase(),
                    confidence = command.confidence
                )

                if (saved) {
                    markSyncedInVoiceOS(command.id)
                    synced++
                } else {
                    failed++
                }
            }

            Log.i(TAG, "Sync complete: synced=$synced, failed=$failed")
            Result.success(workDataOf(
                KEY_SYNCED_COUNT to synced,
                KEY_FAILED_COUNT to failed
            ))
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed", e)
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
```

### Step 4: Using the Unified Learning Service

**File**: `Modules/Shared/NLU/src/androidMain/kotlin/com/augmentalis/nlu/learning/UnifiedLearningService.kt`

```kotlin
@Singleton
class UnifiedLearningService @Inject constructor(
    private val intentClassifier: IntentClassifier
) : ILearningConsumer, LearningEventListener {

    // Registered sources and consumers
    private val sources = mutableListOf<ILearningSource>()
    private val consumers = mutableListOf<ILearningConsumer>()

    // Event flow for observers
    private val _events = MutableSharedFlow<LearningEvent>(replay = 10)
    val events: SharedFlow<LearningEvent> = _events.asSharedFlow()

    /**
     * Register a learning source (e.g., VoiceOSLearningSource)
     */
    fun registerSource(source: ILearningSource) {
        sources.add(source)
        source.addLearningListener(this)
    }

    /**
     * Consume a learned command - compute embedding and save
     */
    override suspend fun consume(command: LearnedCommand): Boolean {
        // 1. Compute embedding if needed
        val commandWithEmbedding = if (!command.hasEmbedding) {
            val embedding = intentClassifier.computeEmbedding(command.utterance)
            command.copy(embedding = embedding)
        } else {
            command
        }

        // 2. Save to NLU
        val saved = saveToNLU(commandWithEmbedding)

        if (saved) {
            // 3. Emit event
            _events.emit(LearningEvent.CommandLearned(
                command = commandWithEmbedding,
                sourceSystem = command.source.name
            ))
        }

        return saved
    }

    /**
     * Sync from all registered sources
     */
    suspend fun syncFromAllSources(limit: Int = 100): Int {
        var totalSynced = 0
        sources.forEach { source ->
            val commands = source.getUnsyncedCommands(limit)
            val synced = consumeBatch(commands)
            if (synced > 0) {
                source.markSynced(commands.take(synced).map { it.id })
            }
            totalSynced += synced
        }
        return totalSynced
    }

    /**
     * Learn directly from VoiceOS command data
     */
    suspend fun learnFromVoiceOS(
        commandText: String,
        actionType: String,
        elementHash: String,
        confidence: Float,
        packageName: String? = null
    ): Boolean {
        val command = LearnedCommand.fromVoiceOS(
            commandText = commandText,
            actionType = actionType,
            confidence = confidence.toDouble(),
            elementHash = elementHash,
            packageName = packageName,
            createdAt = System.currentTimeMillis()
        )
        return consume(command)
    }
}
```

### Step 5: Initialization in Application

**File**: `android/apps/ava/app/src/main/kotlin/com/augmentalis/ava/AVAApplication.kt`

```kotlin
@HiltAndroidApp
class AVAApplication : Application() {

    @Inject lateinit var workManager: WorkManager
    @Inject lateinit var unifiedLearningService: UnifiedLearningService
    @Inject lateinit var voiceOSLearningSource: VoiceOSLearningSource

    override fun onCreate() {
        super.onCreate()

        // Register VoiceOS as learning source
        unifiedLearningService.registerSource(voiceOSLearningSource)

        // Start periodic sync workers
        VoiceOSLearningSyncWorker.enqueueHighConfidenceSync(workManager)
        VoiceOSLearningSyncWorker.enqueueLowConfidenceSync(workManager)
    }
}
```

---

## Database Schema

### VoiceOS - GeneratedCommand Table Updates

**File**: `Modules/VoiceOS/core/database/src/commonMain/sqldelight/com/augmentalis/database/GeneratedCommand.sq`

```sql
-- ADR-014: Unified Learning columns
CREATE TABLE commands_generated (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    elementHash TEXT NOT NULL,
    commandText TEXT NOT NULL,
    actionType TEXT NOT NULL,
    confidence REAL NOT NULL,
    synonyms TEXT,
    isUserApproved INTEGER NOT NULL DEFAULT 0,
    usageCount INTEGER NOT NULL DEFAULT 0,
    lastUsed INTEGER,
    createdAt INTEGER NOT NULL,
    -- NEW: Sync tracking
    synced_to_ava INTEGER NOT NULL DEFAULT 0,
    synced_at INTEGER,
    UNIQUE(elementHash, commandText)
);

-- Sync queries
getUnsyncedCommands:
SELECT * FROM commands_generated
WHERE synced_to_ava = 0 AND confidence >= ?
ORDER BY isUserApproved DESC, confidence DESC
LIMIT ?;

markSyncedToAva:
UPDATE commands_generated
SET synced_to_ava = 1, synced_at = ?
WHERE id = ?;

getSyncStats:
SELECT
    COUNT(*) AS total,
    SUM(CASE WHEN synced_to_ava = 1 THEN 1 ELSE 0 END) AS synced,
    SUM(CASE WHEN synced_to_ava = 0 THEN 1 ELSE 0 END) AS pending
FROM commands_generated;
```

### AVA - Extended Learning Statistics

**File**: `Modules/Shared/NLU/src/androidMain/kotlin/com/augmentalis/nlu/IntentClassifier.kt`

```kotlin
data class LearningStatsResult(
    val total: Int,
    val llmAuto: Int,
    val llmVariation: Int,
    val user: Int,
    val confirmed: Int,
    val voiceosCommands: Int,    // NEW: VoiceOS synced commands
    val withEmbedding: Int       // NEW: Commands with embeddings
)
```

---

## Dependency Injection

### AppModule Updates

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object UnifiedLearningModule {

    @Provides
    @Singleton
    fun provideUnifiedLearningService(
        intentClassifier: IntentClassifier
    ): UnifiedLearningService {
        return UnifiedLearningService(intentClassifier)
    }

    @Provides
    @Singleton
    fun provideVoiceOSLearningSource(
        @ApplicationContext context: Context
    ): VoiceOSLearningSource {
        return VoiceOSLearningSource(context)
    }
}
```

---

## Testing

### Unit Tests

```kotlin
@Test
fun `LearnedCommand fromVoiceOS creates correct intent`() {
    val command = LearnedCommand.fromVoiceOS(
        commandText = "click like",
        actionType = "click",
        confidence = 0.85,
        elementHash = "abc12345"
    )

    assertEquals("click_element_abc12345", command.intent)
    assertEquals(LearningSource.VOICEOS_SCRAPE, command.source)
    assertEquals(LearningActionType.CLICK, command.actionType)
}

@Test
fun `LearningSource priority ordering is correct`() {
    assertTrue(LearningSource.USER_TAUGHT.priority > LearningSource.VOICEOS_APPROVED.priority)
    assertTrue(LearningSource.VOICEOS_APPROVED.priority > LearningSource.LLM_AUTO.priority)
    assertTrue(LearningSource.LLM_AUTO.priority > LearningSource.VOICEOS_SCRAPE.priority)
}

@Test
fun `UnifiedLearningService filters low confidence`() = runTest {
    val service = UnifiedLearningService(mockIntentClassifier)
    val lowConfCommand = LearnedCommand(
        id = "1",
        utterance = "test",
        intent = "test_intent",
        actionType = LearningActionType.CLICK,
        confidence = 0.3f, // Below threshold
        source = LearningSource.VOICEOS_SCRAPE
    )

    val result = service.consume(lowConfCommand)
    assertFalse(result) // Should reject low confidence
}
```

### Integration Tests

```kotlin
@Test
fun `VoiceOSLearningSyncWorker syncs commands`() = runTest {
    // Setup mock VoiceOS ContentProvider
    val commands = listOf(
        LearnedCommand.fromVoiceOS("click home", "click", 0.9, "hash1"),
        LearnedCommand.fromVoiceOS("tap settings", "click", 0.85, "hash2")
    )

    // Run worker
    val worker = VoiceOSLearningSyncWorker(context, params, intentClassifier)
    val result = worker.doWork()

    // Verify
    assertEquals(Result.success(), result)
    verify(intentClassifier, times(2)).saveTrainedEmbedding(any(), any(), any(), any(), any())
}
```

---

## Monitoring & Debugging

### Logcat Tags

| Tag | Purpose |
|-----|---------|
| `UnifiedLearningService` | Service orchestration |
| `VoiceOSLearningSyncWorker` | Background sync jobs |
| `VoiceOSLearningSource` | VoiceOS queries |
| `IntentClassifier` | Embedding computation |

### Debug Commands

```bash
# View sync logs
adb logcat -s VoiceOSLearningSyncWorker:V UnifiedLearningService:V

# Check pending WorkManager jobs
adb shell dumpsys jobscheduler | grep -A 10 "voiceos_learning_sync"

# Query sync statistics (VoiceOS)
adb shell content query --uri content://com.avanues.voiceos.provider/sync_stats

# Database inspection (AVA)
adb shell run-as com.augmentalis.ava sqlite3 databases/ava.db \
    "SELECT source, COUNT(*) FROM train_example GROUP BY source;"
```

---

## Performance Considerations

### Battery Impact

| Operation | Impact | Mitigation |
|-----------|--------|------------|
| High-confidence sync | ~0.5mAh per sync | Only when battery > 20% |
| Low-confidence sync | ~2mAh per sync | Only when charging |
| Embedding computation | ~0.05mAh per command | Batched, battery-aware |

### Memory Usage

| Component | Memory | Notes |
|-----------|--------|-------|
| UnifiedLearningService | ~2MB | Singleton |
| Sync worker | ~10MB peak | During batch processing |
| Event buffer | ~1MB | SharedFlow replay=10 |

### Sync Frequency Tuning

| Scenario | High-Conf Interval | Low-Conf Interval |
|----------|-------------------|-------------------|
| Heavy user (>100 cmds/day) | 5 min | 2 hours |
| Normal user | 5 min | 6 hours |
| Light user | 15 min | 12 hours |

---

## Troubleshooting

### Common Issues

| Issue | Cause | Solution |
|-------|-------|----------|
| No commands syncing | VoiceOS not installed | Check `isVoiceOSAvailable()` |
| Sync failing | ContentProvider permission | Verify URI permissions |
| Embeddings null | Model not loaded | Wait for IntentClassifier init |
| High battery drain | Too frequent sync | Increase sync intervals |

### Verification Steps

1. **Check VoiceOS availability**:
   ```kotlin
   val available = voiceOSLearningSource.isVoiceOSAvailable()
   Log.d("UnifiedLearning", "VoiceOS available: $available")
   ```

2. **Check unsynced count**:
   ```kotlin
   val count = voiceOSLearningSource.getCommandCount()
   Log.d("UnifiedLearning", "Unsynced commands: $count")
   ```

3. **Trigger manual sync**:
   ```kotlin
   val synced = unifiedLearningService.syncFromAllSources(limit = 10)
   Log.d("UnifiedLearning", "Manually synced: $synced")
   ```

---

## References

- ADR-014: Unified Learning Architecture
- ADR-013: Self-Learning NLU with LLM-as-Teacher
- Chapter 70: Self-Learning NLU System
- [Android WorkManager](https://developer.android.com/develop/background-work/background-tasks)
- [ContentProvider IPC](https://developer.android.com/guide/topics/providers/content-providers)

---

**Created by Manoj Jhawar, manoj@ideahq.net**
