# ADR-014: Unified Learning Architecture (VoiceOS-AVA Integration)

**Status**: Accepted
**Date**: 2025-12-18
**Authors**: Manoj Jhawar
**Deciders**: Architecture Team
**Platform**: Android, iOS, Desktop (KMP)
**Related**: ADR-013 (Self-Learning NLU), ADR-006 (VoiceOS Command Delegation)

---

## Context

AVA and VoiceOS have separate learning systems:

| System | Learning Method | Storage | Matching |
|--------|-----------------|---------|----------|
| **AVA NLU** | LLM-as-Teacher (ADR-013) | `train_example` + `intent_embedding` | Semantic (BERT embeddings) |
| **VoiceOS** | UI Scraping + JIT | `commands_generated` | Pattern (regex/fuzzy) |

**Problem**: These systems operate in isolation:
- VoiceOS learns thousands of UI commands that AVA cannot leverage
- AVA's semantic understanding doesn't enhance VoiceOS command matching
- Users teach the same concepts twice
- No unified view of learned commands

**Goal**: Create a unified learning architecture where:
1. VoiceOS scraped commands flow to AVA's NLU for semantic understanding
2. Learning from one system benefits both
3. Single source of truth for command learning statistics
4. Battery-efficient background synchronization

---

## Decision

**We will implement a Bridge Service Architecture that syncs VoiceOS commands to AVA's NLU system using WorkManager for battery-efficient background processing.**

### Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                      UNIFIED LEARNING ARCHITECTURE                           │
│                              (ADR-014)                                       │
└─────────────────────────────────────────────────────────────────────────────┘

┌────────────────────────┐          ┌────────────────────────────────────────┐
│       VoiceOS          │          │                AVA                      │
│ ────────────────────── │   Sync   │ ────────────────────────────────────── │
│ Pattern-based Learning │ ───────► │ Semantic/Embedding-based Learning      │
│ - UI Scraping (JIT)    │          │ - LLM-as-Teacher (ADR-013)             │
│ - LearnApp Exploration │          │ - User Teaching (Teach AVA)            │
│ commands_generated     │          │ train_example + intent_embedding       │
└───────────┬────────────┘          └───────────────────┬────────────────────┘
            │                                           │
            │ ILearningSource                           │ ILearningConsumer
            ▼                                           ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                      UnifiedLearningService                                  │
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │                  VoiceOSLearningSyncWorker (WorkManager)              │  │
│  │  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────────┐   │  │
│  │  │ High-Confidence │  │ Low-Confidence  │  │ User-Approved       │   │  │
│  │  │ Every 5 min     │  │ Every 6 hours   │  │ Immediate           │   │  │
│  │  │ (≥0.85)         │  │ (0.6-0.85)      │  │ (Priority)          │   │  │
│  │  └─────────────────┘  └─────────────────┘  └─────────────────────┘   │  │
│  └───────────────────────────────────────────────────────────────────────┘  │
│                                    │                                         │
│                                    ▼                                         │
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │                    Shared Learning Domain (KMP)                       │  │
│  │  - LearnedCommand (Value Object)                                      │  │
│  │  - LearningEvent (Domain Events)                                      │  │
│  │  - LearningSource (enum with priority)                                │  │
│  │  - ILearningSource / ILearningConsumer (interfaces)                   │  │
│  └───────────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Key Components

| Component | Location | Purpose |
|-----------|----------|---------|
| `LearningDomain.kt` | `Shared/NLU/src/commonMain` | Cross-platform domain model |
| `UnifiedLearningService` | `Shared/NLU/src/androidMain` | Central orchestration |
| `VoiceOSLearningSyncWorker` | `Shared/NLU/src/androidMain` | Background sync |
| `VoiceOSLearningSource` | `Shared/NLU/src/androidMain` | VoiceOS adapter |

### Sync Strategy

| Command Type | Sync Frequency | Constraints | Rationale |
|--------------|----------------|-------------|-----------|
| High-confidence (≥0.85) | Every 5 minutes | Battery not low | Quick value from quality commands |
| Low-confidence (0.6-0.85) | Every 6 hours | Charging + Battery OK | Bulk processing when idle |
| User-approved | Immediate (expedited) | None | User explicitly trusts these |

### Learning Source Priority

```kotlin
enum class LearningSource(val priority: Int) {
    USER_TAUGHT(100),        // Highest - user explicitly taught
    USER_CONFIRMED(90),      // User confirmed LLM suggestion
    VOICEOS_APPROVED(85),    // User approved VoiceOS command
    LLM_AUTO(70),            // LLM auto-classification
    LLM_VARIATION(60),       // LLM-generated variations
    VOICEOS_JIT(55),         // JIT learning from UI
    VOICEOS_SCRAPE(50),      // Auto-scraped from exploration
    BUNDLED(30),             // Bundled with app
    UNKNOWN(0)               // Legacy/unknown
}
```

---

## Implementation

### 1. Shared Domain Model (KMP)

**File**: `Modules/Shared/NLU/src/commonMain/kotlin/com/augmentalis/nlu/learning/domain/LearningDomain.kt`

```kotlin
/**
 * Unified command representation for both VoiceOS and AVA
 */
data class LearnedCommand(
    val id: String,
    val utterance: String,
    val intent: String,
    val actionType: LearningActionType,
    val confidence: Float,
    val source: LearningSource,
    val locale: String = "en-US",
    val synonyms: List<String> = emptyList(),
    val embedding: FloatArray? = null,
    val createdAt: Long = 0L,
    val isUserApproved: Boolean = false,
    val packageName: String? = null,
    val elementHash: String? = null
) {
    val hasEmbedding: Boolean get() = embedding != null
    val isHighConfidence: Boolean get() = confidence >= 0.85f
    val isVoiceOSCommand: Boolean get() = source in listOf(
        LearningSource.VOICEOS_SCRAPE,
        LearningSource.VOICEOS_APPROVED,
        LearningSource.VOICEOS_JIT
    )
}
```

### 2. VoiceOS Learning Source Adapter

**File**: `Modules/Shared/NLU/src/androidMain/kotlin/com/augmentalis/nlu/learning/VoiceOSLearningSource.kt`

```kotlin
@Singleton
class VoiceOSLearningSource @Inject constructor(
    private val context: Context
) : ILearningSource {

    override suspend fun getUnsyncedCommands(limit: Int): List<LearnedCommand>
    override suspend fun markSynced(commandIds: List<String>)
    override suspend fun getCommandCount(): Int

    // Queries VoiceOS via ContentProvider:
    // content://com.avanues.voiceos.provider/commands_generated
}
```

### 3. Background Sync Worker

**File**: `Modules/Shared/NLU/src/androidMain/kotlin/com/augmentalis/nlu/learning/VoiceOSLearningSyncWorker.kt`

```kotlin
@HiltWorker
class VoiceOSLearningSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val intentClassifier: IntentClassifier
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // 1. Query VoiceOS for unsynced commands
        // 2. Compute BERT embeddings
        // 3. Save to AVA's train_example table
        // 4. Mark as synced in VoiceOS
    }

    companion object {
        fun enqueueHighConfidenceSync(workManager: WorkManager)
        fun enqueueLowConfidenceSync(workManager: WorkManager)
        fun enqueueUserApprovedSync(workManager: WorkManager)
    }
}
```

### 4. Database Schema Extension

**VoiceOS** - `GeneratedCommand.sq`:
```sql
-- ADR-014: Unified Learning columns
synced_to_ava INTEGER NOT NULL DEFAULT 0,
synced_at INTEGER,

-- New queries
getUnsyncedCommands:
SELECT * FROM commands_generated
WHERE synced_to_ava = 0 AND confidence >= ?
ORDER BY isUserApproved DESC, confidence DESC
LIMIT ?;

markSyncedToAva:
UPDATE commands_generated
SET synced_to_ava = 1, synced_at = ?
WHERE id = ?;
```

---

## Alternatives Considered

### Option A: VoiceOS Generates Embeddings Directly

**Approach**: Add IntentClassifier to VoiceOS, compute embeddings at scrape time.

**Pros**:
- No sync delay
- Embeddings available immediately

**Cons**:
- Adds 50MB model to VoiceOS
- Duplicates NLU code
- Memory pressure during scraping
- Coupling between systems

**Score**: 6.35/10

### Option B: Shared IntentClassifier Singleton

**Approach**: Single IntentClassifier instance shared via ContentProvider.

**Pros**:
- No duplication
- Real-time embedding

**Cons**:
- IPC overhead for every command
- Complex threading
- Lifecycle management issues

**Score**: 5.95/10

### Option C: Bridge Service (SELECTED)

**Approach**: Background WorkManager syncs commands periodically.

**Pros**:
- Loose coupling
- Battery-efficient
- Fault-tolerant
- Clean separation

**Cons**:
- Sync delay (5min-6hr)
- Requires both apps installed

**Score**: 7.60/10

### Option D: Unified Database

**Approach**: Single SQLDelight database shared between apps.

**Pros**:
- No sync needed
- Single source of truth

**Cons**:
- File locking issues
- Migration complexity
- Breaks app isolation

**Score**: 4.95/10

---

## Consequences

### Positive

| Benefit | Impact |
|---------|--------|
| **Unified Learning** | VoiceOS commands enhance AVA's NLU |
| **Battery Efficient** | WorkManager handles scheduling |
| **Fault Tolerant** | Retry logic, exponential backoff |
| **Loose Coupling** | Systems remain independent |
| **Extensible** | New sources can implement ILearningSource |
| **Cross-Platform Ready** | Domain model in KMP commonMain |

### Negative

| Drawback | Mitigation |
|----------|------------|
| **Sync Delay** | High-confidence syncs every 5 minutes |
| **Requires Both Apps** | Graceful fallback when VoiceOS not installed |
| **Storage Duplication** | Commands stored in both DBs (different schemas) |

### Metrics to Track

| Metric | Target | Current |
|--------|--------|---------|
| Commands synced/day | > 50 | TBD |
| Sync success rate | > 95% | TBD |
| Battery impact | < 1% | TBD |
| NLU accuracy improvement | > 5% | TBD |
| Embedding computation time | < 50ms | ~30ms |

---

## Migration Path

### Phase 1: Schema Migration
1. Add `synced_to_ava`, `synced_at` columns to VoiceOS `commands_generated`
2. Create index on sync status
3. No data migration needed (defaults to 0)

### Phase 2: Enable Sync Workers
1. Deploy AVA update with UnifiedLearningService
2. Workers start syncing existing commands
3. Backfill happens automatically over 24-48 hours

### Phase 3: Monitor & Optimize
1. Track sync metrics
2. Adjust sync frequencies based on battery impact
3. Add analytics for learning source effectiveness

---

## Testing Strategy

### Unit Tests

```kotlin
@Test fun `LearnedCommand converts from VoiceOS format correctly`()
@Test fun `LearningSource priority ordering is correct`()
@Test fun `UnifiedLearningService filters low confidence commands`()
@Test fun `VoiceOSLearningSource parses ContentProvider cursor`()
```

### Integration Tests

```kotlin
@Test fun `VoiceOSLearningSyncWorker syncs commands to AVA`()
@Test fun `Synced commands are recognized by NLU`()
@Test fun `Worker respects battery constraints`()
```

### Device Tests

| Scenario | Expected |
|----------|----------|
| VoiceOS installed | Sync runs on schedule |
| VoiceOS not installed | Graceful no-op |
| Battery low | Only user-approved syncs |
| Charging | Full sync runs |

---

## References

- ADR-013: Self-Learning NLU with LLM-as-Teacher
- ADR-006: VoiceOS Command Delegation
- [Android WorkManager](https://developer.android.com/develop/background-work/background-tasks)
- [ContentProvider IPC](https://developer.android.com/guide/topics/providers/content-providers)
- [KMP Architecture](https://kotlinlang.org/docs/multiplatform.html)

---

## Changelog

**v1.0 (2025-12-18)**: Initial decision - Bridge Service Architecture for VoiceOS-AVA learning integration

---

**Created by Manoj Jhawar, manoj@ideahq.net**
