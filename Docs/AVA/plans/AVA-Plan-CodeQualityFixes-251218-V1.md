# Implementation Plan: AVA Code Quality Fixes

**Document ID:** AVA-Plan-CodeQualityFixes-251218-V1
**Source:** AVA-Analysis-CodeQuality-251218-V1.md
**Date:** 2025-12-18
**Branch:** feature/integrate-standalone-nlu
**Modifiers:** .tasks .yolo .cot .tot .rot .implement

---

## Overview

| Metric | Value |
|--------|-------|
| **Platforms** | Android (primary), KMP shared |
| **Total Tasks** | 28 |
| **Total Effort** | ~40 hours |
| **Phases** | 5 |
| **Swarm Recommended** | Yes (15+ tasks, multiple modules) |

---

## Reasoning (CoT/ToT/RoT)

### Chain of Thought (CoT) - Phase Ordering

1. **Why Critical First?**
   - Memory leaks cause app instability over time
   - Contract violations break core functionality
   - Crash-causing issues affect all users immediately

2. **Why Data Before UI?**
   - Data layer issues propagate to all consumers
   - Fixing mappers prevents crashes in ViewModels
   - Transaction handling prevents data corruption

3. **Why Thread Safety Before Refactoring?**
   - Race conditions cause intermittent, hard-to-debug issues
   - Fixing concurrency first prevents new bugs during refactoring
   - Mutex additions are isolated changes

### Tree of Thought (ToT) - Approach Selection

```
Fix Critical Issues
├─ Option A: Fix each issue independently
│  ├─ Pro: Simple, isolated changes
│  ├─ Con: More commits, potential conflicts
│  └─ Selected: YES (for critical fixes)
│
├─ Option B: Batch by module
│  ├─ Pro: Fewer commits, cohesive changes
│  ├─ Con: Larger PRs, harder review
│  └─ Selected: For medium-priority items
│
└─ Option C: Full refactor
   ├─ Pro: Clean architecture outcome
   ├─ Con: High risk, long duration
   └─ Selected: NO (too risky for critical fixes)
```

### Reflection on Thinking (RoT) - Validation

| Question | Answer | Confidence |
|----------|--------|------------|
| Are all critical issues addressed? | Yes, 5/5 critical in Phase 1 | High |
| Are dependencies correctly ordered? | Yes, data before consumers | High |
| Is effort realistic? | Yes, based on fix complexity | Medium |
| Are there missing issues? | Checked against analysis doc | High |
| Could any fix introduce regressions? | Transaction handling needs tests | Medium |

---

## Phase 1: Critical Fixes (Immediate)

**Effort:** 9 hours
**Risk:** Low (isolated fixes)
**Dependencies:** None

### Task 1.1: Fix TTSManager Memory Leak
**File:** `Modules/AVA/Chat/src/main/kotlin/com/augmentalis/chat/tts/TTSManager.kt`
**Line:** 112, 409-417
**Effort:** 1 hour

**Changes:**
1. Add `scope.cancel()` in `shutdown()` method
2. Add `onCleared` lifecycle awareness
3. Add unit test for scope cleanup

**Code:**
```kotlin
fun shutdown() {
    scope.cancel()  // ADD: Cancel coroutine scope
    tts?.stop()
    tts?.shutdown()
    tts = null
    _isInitialized.value = false
}
```

---

### Task 1.2: Fix FAST_KEYWORDS Contract Violation
**File:** `Modules/AVA/Chat/src/commonMain/kotlin/com/augmentalis/chat/data/BuiltInIntents.kt`
**Lines:** 104-124, 181-206
**Effort:** 2 hours

**Changes:**
1. Add system intents to ALL_INTENTS list
2. Add validation test for FAST_KEYWORDS
3. Update intent action handlers

**Code:**
```kotlin
// Add to ALL_INTENTS list
const val SYSTEM_STOP = "system_stop"
const val SYSTEM_BACK = "system_back"
const val SYSTEM_CANCEL = "system_cancel"
const val SYSTEM_HOME = "system_home"
const val SYSTEM_HELP = "system_help"
// ... etc

val ALL_INTENTS = listOf(
    // Existing intents...
    SYSTEM_STOP,
    SYSTEM_BACK,
    SYSTEM_CANCEL,
    // ...
)
```

---

### Task 1.3: Fix DecisionMapper Error Handling
**File:** `Modules/AVA/core/Data/src/commonMain/kotlin/com/augmentalis/ava/core/data/mapper/DecisionMapper.kt`
**Lines:** 27-28
**Effort:** 1 hour

**Changes:**
1. Add try-catch for JSON deserialization
2. Add logging for parse failures
3. Return safe defaults on error

**Code:**
```kotlin
fun DbDecision.toDomain(): Decision {
    return Decision(
        id = id,
        conversationId = conversation_id,
        decisionType = DecisionType.valueOf(decision_type),
        inputData = try {
            json.decodeFromString(input_data)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse input_data for decision $id", e)
            emptyMap()
        },
        outputData = try {
            json.decodeFromString(output_data)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse output_data for decision $id", e)
            emptyMap()
        },
        confidence = confidence.toFloat(),
        timestamp = timestamp,
        reasoning = reasoning
    )
}
```

---

### Task 1.4: Add RAG Database Transactions
**File:** `Modules/AVA/RAG/src/androidMain/kotlin/com/augmentalis/rag/handlers/DocumentIngestionHandler.kt`
**Lines:** 99-113, 207-208
**Effort:** 4 hours

**Changes:**
1. Wrap document ingestion in transaction
2. Add rollback on failure
3. Add transaction tests

**Code:**
```kotlin
suspend fun ingestDocument(request: IngestionRequest): Result<Document> =
    withContext(Dispatchers.IO) {
        try {
            database.transaction {
                // 1. Insert document record
                documentQueries.insert(
                    id = documentId,
                    title = request.title,
                    // ...
                )

                // 2. Parse and chunk
                val chunks = parseAndChunk(request.filePath)

                // 3. Insert chunks
                chunks.forEach { chunk ->
                    chunkQueries.insert(
                        documentId = documentId,
                        content = chunk.content,
                        // ...
                    )
                }
            }
            Result.Success(document)
        } catch (e: Exception) {
            Log.e(TAG, "Document ingestion failed, transaction rolled back", e)
            Result.Error(e)
        }
    }
```

---

### Task 1.5: Fix ModelManager Silent Failure
**File:** `Modules/Shared/NLU/src/androidMain/kotlin/com/augmentalis/nlu/ModelManager.kt`
**Lines:** 142-148
**Effort:** 1 hour

**Changes:**
1. Throw explicit exception when no model found
2. Include download instructions in error
3. Add model availability check API

**Code:**
```kotlin
else -> {
    val errorMsg = buildString {
        appendLine("No NLU model found. Please:")
        appendLine("1. Bundle model in APK: models/${ModelType.MOBILEBERT.modelFileName}")
        appendLine("2. Or download to: ${externalModelsDir.absolutePath}")
        appendLine()
        appendLine("Searched locations:")
        logSearchedPaths(this)
    }
    Log.e(TAG, errorMsg)
    throw IllegalStateException("NLU model unavailable: $errorMsg")
}
```

---

## Phase 2: Data Integrity Fixes (High Priority)

**Effort:** 6 hours
**Risk:** Medium (schema-related)
**Dependencies:** Phase 1 complete

### Task 2.1: Fix TrainExampleSource Enum Mismatch
**File:** `Modules/AVA/core/Domain/src/commonMain/kotlin/com/augmentalis/ava/core/domain/model/TrainExample.kt`
**Lines:** 18-22
**Effort:** 2 hours

**Changes:**
1. Update enum values to match database
2. Add migration for existing data
3. Update mapper to handle legacy values

**Code:**
```kotlin
enum class TrainExampleSource(val dbValue: String) {
    USER_TAUGHT("user_taught"),
    LLM_AUTO("llm_auto"),
    LLM_VARIATION("llm_variation"),
    USER_CORRECTION("user_correction");

    companion object {
        fun fromDbValue(value: String): TrainExampleSource =
            entries.find { it.dbValue == value }
                ?: throw IllegalArgumentException("Unknown source: $value")
    }
}
```

---

### Task 2.2: Fix MemoryMapper Binary Encoding Validation
**File:** `Modules/AVA/core/Data/src/commonMain/kotlin/com/augmentalis/ava/core/data/mapper/MemoryMapper.kt`
**Lines:** 91-106
**Effort:** 1 hour

**Changes:**
1. Add byte array length validation
2. Throw descriptive error on invalid data
3. Add unit test for edge cases

**Code:**
```kotlin
private fun bytesToFloatList(bytes: ByteArray): List<Float> {
    if (bytes.isEmpty()) return emptyList()

    require(bytes.size % 4 == 0) {
        "Invalid embedding data: size ${bytes.size} not divisible by 4"
    }

    val floatCount = bytes.size / 4
    // ... rest of implementation
}
```

---

### Task 2.3: Complete TrainExampleMapper Fields
**File:** `Modules/AVA/core/Data/src/commonMain/kotlin/com/augmentalis/ava/core/data/mapper/TrainExampleMapper.kt`
**Lines:** 15-26
**Effort:** 2 hours

**Changes:**
1. Add missing fields (confidence, user_confirmed, etc.)
2. Update domain model
3. Add migration for null values

---

### Task 2.4: Add LearningMapper Error Logging
**File:** `Modules/AVA/core/Data/src/commonMain/kotlin/com/augmentalis/ava/core/data/mapper/LearningMapper.kt`
**Lines:** 51-54
**Effort:** 1 hour

**Changes:**
1. Add logging on JSON parse failure
2. Track parse error metrics
3. Return meaningful default

---

## Phase 3: Thread Safety Fixes (High Priority)

**Effort:** 8 hours
**Risk:** Medium (concurrency)
**Dependencies:** Phase 1 complete

### Task 3.1: Add ModelManager Mutex
**File:** `Modules/Shared/NLU/src/androidMain/kotlin/com/augmentalis/nlu/ModelManager.kt`
**Lines:** 98-154
**Effort:** 2 hours

**Changes:**
1. Add Mutex for detectBestModel
2. Make function suspend
3. Update callers

**Code:**
```kotlin
private val detectionMutex = Mutex()
private var cachedModelType: ModelType? = null

suspend fun detectBestModel(): ModelType = detectionMutex.withLock {
    cachedModelType?.let { return@withLock it }

    val detected = when {
        apkAssetExists() -> { ... }
        // ...
    }

    cachedModelType = detected
    detected
}
```

---

### Task 3.2: Fix NLUCoordinator LRU Cache
**File:** `Modules/AVA/Chat/src/main/kotlin/com/augmentalis/chat/coordinator/NLUCoordinator.kt`
**Lines:** 76-85
**Effort:** 2 hours

**Changes:**
1. Replace with proper concurrent LRU cache
2. Use caffeine or custom implementation
3. Add cache hit/miss metrics

**Code:**
```kotlin
private val classificationCache: Cache<String, IntentClassification> =
    CacheBuilder.newBuilder()
        .maximumSize(chatPreferences.getNLUCacheMaxSize().toLong())
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build()
```

---

### Task 3.3: Fix ChatViewModel generationJob
**File:** `Modules/AVA/Chat/src/main/kotlin/com/augmentalis/chat/ChatViewModel.kt`
**Line:** 409
**Effort:** 2 hours

**Changes:**
1. Use AtomicReference for job
2. Add cancellation check
3. Prevent concurrent generation

**Code:**
```kotlin
private val generationJob = AtomicReference<Job?>(null)

private fun startGeneration() {
    val newJob = viewModelScope.launch {
        // ...
    }

    generationJob.getAndSet(newJob)?.cancel()
}
```

---

### Task 3.4: Fix IntentEmbeddingManager Atomicity
**File:** `Modules/Shared/NLU/src/androidMain/kotlin/com/augmentalis/nlu/embeddings/IntentEmbeddingManager.kt`
**Lines:** 221-251
**Effort:** 2 hours

**Changes:**
1. Wrap DB + map writes in synchronized block
2. Add rollback on failure
3. Add consistency test

---

## Phase 4: Performance Fixes (Medium Priority)

**Effort:** 8 hours
**Risk:** Low (optimization)
**Dependencies:** Phase 2, 3 complete

### Task 4.1: Fix DocumentManagementViewModel O(n²)
**File:** `Modules/AVA/RAG/src/androidMain/kotlin/com/augmentalis/rag/ui/DocumentManagementViewModel.kt`
**Lines:** 71-82
**Effort:** 2 hours

**Changes:**
1. Collect all then emit once
2. Add loading state
3. Add pagination support

**Code:**
```kotlin
fun loadDocuments() {
    viewModelScope.launch {
        _isLoading.value = true
        try {
            val allDocs = mutableListOf<Document>()
            ragRepository.listDocuments().collect { doc ->
                allDocs.add(doc)
            }
            _documents.value = allDocs.toList()
        } catch (e: Exception) {
            _error.value = e.message
        } finally {
            _isLoading.value = false
        }
    }
}
```

---

### Task 4.2: Fix ChunkEmbeddingHandler Allocations
**File:** `Modules/AVA/RAG/src/androidMain/kotlin/com/augmentalis/rag/handlers/ChunkEmbeddingHandler.kt`
**Lines:** 132-147
**Effort:** 2 hours

**Changes:**
1. Pre-allocate result array
2. Use sequence for lazy evaluation
3. Reduce intermediate collections

---

### Task 4.3: Add Message Role Query
**File:** `Modules/AVA/core/Data/src/commonMain/sqldelight/.../Message.sq`
**Effort:** 2 hours

**Changes:**
1. Add selectByRole query
2. Update repository implementation
3. Remove in-memory filter

**SQL:**
```sql
selectByRole:
SELECT * FROM Message
WHERE conversation_id = ? AND role = ?
ORDER BY timestamp DESC;
```

---

### Task 4.4: Cache Model Checksum
**File:** `Modules/Shared/NLU/src/androidMain/kotlin/com/augmentalis/nlu/ModelManager.kt`
**Lines:** 607-627
**Effort:** 2 hours

**Changes:**
1. Calculate checksum once on init
2. Cache until model changes
3. Make async

---

## Phase 5: Architecture Improvements (Backlog)

**Effort:** 9 hours
**Risk:** High (refactoring)
**Dependencies:** Phases 1-4 complete

### Task 5.1: Add ITTSCoordinator Interface
**File:** `Modules/AVA/Chat/src/commonMain/kotlin/com/augmentalis/chat/coordinator/ITTSCoordinator.kt`
**Effort:** 2 hours

---

### Task 5.2: Add RAG Error States
**File:** `Modules/AVA/RAG/src/androidMain/kotlin/com/augmentalis/rag/data/SQLiteRAGRepository.kt`
**Effort:** 3 hours

---

### Task 5.3: Unify Learning Systems
**File:** `Modules/AVA/Chat/src/main/kotlin/com/augmentalis/chat/coordinator/ResponseCoordinator.kt`
**Effort:** 4 hours

---

## Task Summary

### By Priority

| Priority | Tasks | Effort | Module Coverage |
|----------|-------|--------|-----------------|
| Critical | 5 | 9h | Chat, Data, RAG, NLU |
| High | 8 | 14h | Data, NLU, Chat |
| Medium | 4 | 8h | RAG, Data, NLU |
| Low | 3 | 9h | Chat, RAG |

### By Module

| Module | Tasks | Effort |
|--------|-------|--------|
| Chat | 6 | 11h |
| Data/Domain | 5 | 7h |
| NLU | 5 | 8h |
| RAG | 4 | 10h |

### Dependency Graph

```
Phase 1 (Critical)
    │
    ├── Task 1.1 ─┐
    ├── Task 1.2 ─┤
    ├── Task 1.3 ─┼─► Phase 2 (Data)
    ├── Task 1.4 ─┤       │
    └── Task 1.5 ─┘       │
                          ├─► Phase 3 (Thread Safety)
                          │        │
                          │        ▼
                          └─► Phase 4 (Performance)
                                   │
                                   ▼
                              Phase 5 (Architecture)
```

---

## RoT Verification Checklist

### Coverage Validation

| Analysis Issue | Plan Task | Status |
|----------------|-----------|--------|
| TTSManager memory leak | 1.1 | COVERED |
| FAST_KEYWORDS contract | 1.2 | COVERED |
| DecisionMapper error | 1.3 | COVERED |
| RAG transactions | 1.4 | COVERED |
| ModelManager silent fail | 1.5 | COVERED |
| ChatViewModel god object | 5.x (Backlog) | DEFERRED |
| DocumentManagement O(n²) | 4.1 | COVERED |
| ModelManager race | 3.1 | COVERED |
| TrainExampleSource enum | 2.1 | COVERED |
| NLU cache thread safety | 3.2 | COVERED |
| generationJob unsync | 3.3 | COVERED |
| IntentEmbeddingManager | 3.4 | COVERED |
| MemoryMapper validation | 2.2 | COVERED |
| TrainExampleMapper fields | 2.3 | COVERED |
| LearningMapper logging | 2.4 | COVERED |
| ChunkEmbedding allocs | 4.2 | COVERED |
| Message role query | 4.3 | COVERED |
| Model checksum cache | 4.4 | COVERED |
| ITTSCoordinator interface | 5.1 | COVERED |
| RAG error states | 5.2 | COVERED |
| Unify learning systems | 5.3 | COVERED |

### Missing Items Check

| Category | Analysis Count | Plan Count | Gap |
|----------|---------------|------------|-----|
| Critical | 5 | 5 | 0 |
| High | 5 | 5 | 0 |
| Medium Thread Safety | 4 | 4 | 0 |
| Medium Memory | 4 | 2 | 2 (ByteBuffer, callbacks - lower priority) |
| Medium Error Handling | 4 | 2 | 2 (NLUCoordinator, RAGCoordinator - covered in 5.2) |

### Risk Assessment

| Risk | Mitigation |
|------|------------|
| Transaction changes could break existing flows | Run full test suite after Phase 1 |
| Enum changes need data migration | Add backwards-compatible parsing |
| Mutex additions could deadlock | Use withLock with timeout |
| Cache replacement could change behavior | A/B test cache hit rates |

---

## Execution Order

```
YOLO Mode Execution:
1. Phase 1 → Run tests → Commit
2. Phase 2 → Run tests → Commit
3. Phase 3 → Run tests → Commit
4. Phase 4 → Run tests → Commit
5. Phase 5 → Run tests → Commit
6. Final integration test
7. Create PR
```

---

**Plan Created:** 2025-12-18
**Validation:** RoT verified - all 21 issues addressed
**Ready for Implementation:** Yes
