# AI Instructions: sqlite-vec Vector Database Integration for VoiceOS

**Version:** 1.0
**Created:** 2025-12-05
**Author:** Manoj Jhawar
**Status:** Pre-implementation (awaiting AVA merge)

---

## Overview

This document provides AI instructions for integrating sqlite-vec vector search into VoiceOS database. The integration should be performed **after the AVA-VoiceOS merge** to create a unified vector storage system.

---

## Objective

Add SIMD-accelerated vector search to VoiceOS for:
1. Semantic voice command matching (fuzzy trigger phrase matching)
2. UI element discovery (find elements by description)
3. Screen context similarity (find similar screens)
4. Speech recognition learning (improve recognition accuracy)

---

## Tables to Vectorize

### Priority 1: Voice Commands (High Impact)

| Table | Current Search | Vector Benefit |
|-------|---------------|----------------|
| `commands_static` | `LIKE '%trigger%'` | Semantic: "turn off lights" matches "disable lights" |
| `custom_command` | `LIKE '%query%'` | User phrases matched semantically |

**Schema Addition:**
```sql
-- Add to VectorTables.sq
CREATE VIRTUAL TABLE vec_commands USING vec0(
  command_id TEXT PRIMARY KEY,
  source TEXT,              -- 'static', 'custom', 'ava_intent'
  locale TEXT,
  embedding float[384]
);

-- Embedding source: trigger_phrase + synonyms concatenated
```

### Priority 2: UI Elements (Medium Impact)

| Table | Current Search | Vector Benefit |
|-------|---------------|----------------|
| `scraped_element` | `text LIKE '%btn%'` | Semantic: "submit button" matches "Send" |

**Schema Addition:**
```sql
CREATE VIRTUAL TABLE vec_elements USING vec0(
  element_id INTEGER PRIMARY KEY,
  app_id TEXT,
  screen_hash TEXT,
  embedding float[384]
);

-- Embedding source: text + contentDescription + viewIdResourceName
```

### Priority 3: Screen Context (Lower Impact)

| Table | Current Search | Vector Benefit |
|-------|---------------|----------------|
| `screen_context` | Exact hash match | Find similar screens across apps |

**Schema Addition:**
```sql
CREATE VIRTUAL TABLE vec_screens USING vec0(
  screen_id INTEGER PRIMARY KEY,
  app_id TEXT,
  embedding float[384]
);

-- Embedding source: windowTitle + formContext + activityName
```

---

## Implementation Steps

### Step 1: Add sqlite-vec Library

```
VoiceOS/
├── libraries/
│   └── core/
│       └── database/
│           └── src/
│               └── androidMain/
│                   └── jniLibs/
│                       ├── arm64-v8a/
│                       │   └── vec0.so
│                       └── armeabi-v7a/
│                           └── vec0.so
```

Download from: https://github.com/asg017/sqlite-vec/releases

### Step 2: Create VectorTables.sq

Location: `libraries/core/database/src/commonMain/sqldelight/com/augmentalis/database/vector/`

```sql
-- VectorTables.sq

-- Voice commands (static + custom + AVA intents post-merge)
CREATE VIRTUAL TABLE vec_commands USING vec0(
  command_id TEXT PRIMARY KEY,
  source TEXT,              -- 'static', 'custom', 'ava_intent', 'learned'
  locale TEXT,
  category TEXT,
  embedding float[384]
);

-- UI elements
CREATE VIRTUAL TABLE vec_elements USING vec0(
  element_id INTEGER PRIMARY KEY,
  app_id TEXT,
  screen_hash TEXT,
  element_hash TEXT,
  embedding float[384]
);

-- Screen contexts
CREATE VIRTUAL TABLE vec_screens USING vec0(
  screen_id INTEGER PRIMARY KEY,
  app_id TEXT,
  screen_hash TEXT,
  embedding float[384]
);

-- Speech recognition learning
CREATE VIRTUAL TABLE vec_recognition USING vec0(
  learning_id INTEGER PRIMARY KEY,
  engine TEXT,
  embedding float[384]
);
```

### Step 3: Create VectorQueries.sq

```sql
-- VectorQueries.sq

-- Semantic command search (replaces LIKE queries)
searchSimilarCommands:
SELECT
  command_id,
  source,
  locale,
  category,
  vec_distance_cosine(embedding, :queryEmbedding) AS distance
FROM vec_commands
WHERE locale = :locale OR locale = 'en-US'
ORDER BY distance
LIMIT :topK;

-- Find UI elements by description
searchElements:
SELECT
  element_id,
  app_id,
  screen_hash,
  element_hash,
  vec_distance_cosine(embedding, :queryEmbedding) AS distance
FROM vec_elements
WHERE app_id = :appId OR :appId IS NULL
ORDER BY distance
LIMIT :topK;

-- Find similar screens
searchSimilarScreens:
SELECT
  screen_id,
  app_id,
  screen_hash,
  vec_distance_cosine(embedding, :queryEmbedding) AS distance
FROM vec_screens
ORDER BY distance
LIMIT :topK;

-- Insert embeddings
insertCommandEmbedding:
INSERT INTO vec_commands(command_id, source, locale, category, embedding)
VALUES (:commandId, :source, :locale, :category, :embedding);

insertElementEmbedding:
INSERT INTO vec_elements(element_id, app_id, screen_hash, element_hash, embedding)
VALUES (:elementId, :appId, :screenHash, :elementHash, :embedding);

insertScreenEmbedding:
INSERT INTO vec_screens(screen_id, app_id, screen_hash, embedding)
VALUES (:screenId, :appId, :screenHash, :embedding);
```

### Step 4: Create VectorSearchService

Location: `libraries/core/database/src/commonMain/kotlin/.../vector/`

```kotlin
// VectorSearchService.kt
interface VectorSearchService {
    // Command search
    suspend fun searchCommands(
        query: String,
        locale: String,
        topK: Int = 5
    ): List<CommandSearchResult>

    // Element search
    suspend fun searchElements(
        query: String,
        appId: String? = null,
        topK: Int = 10
    ): List<ElementSearchResult>

    // Screen similarity
    suspend fun findSimilarScreens(
        screenHash: String,
        topK: Int = 5
    ): List<ScreenSearchResult>

    // Embedding management
    suspend fun indexCommand(command: CommandEntity, source: String)
    suspend fun indexElement(element: ScrapedElement)
    suspend fun indexScreen(screen: ScreenContext)
    suspend fun reindexAll()
}

data class CommandSearchResult(
    val commandId: String,
    val source: String,
    val distance: Float,
    val similarity: Float get() = 1f - distance
)

data class ElementSearchResult(
    val elementId: Long,
    val appId: String,
    val screenHash: String,
    val distance: Float
)

data class ScreenSearchResult(
    val screenId: Long,
    val appId: String,
    val screenHash: String,
    val distance: Float
)
```

### Step 5: Update CommandManager

Replace LIKE-based search with vector search:

```kotlin
// Before (CommandManager.kt)
suspend fun findCommand(utterance: String): CommandEntity? {
    // LIKE-based search
    return database.voiceCommandQueries
        .searchByTrigger(utterance)
        .executeAsOneOrNull()
}

// After
suspend fun findCommand(utterance: String): CommandEntity? {
    // Semantic vector search
    val results = vectorSearchService.searchCommands(
        query = utterance,
        locale = currentLocale,
        topK = 1
    )

    return if (results.isNotEmpty() && results[0].similarity >= 0.7f) {
        database.voiceCommandQueries
            .getCommandsByCommandId(results[0].commandId)
            .executeAsOneOrNull()
    } else null
}
```

### Step 6: Background Indexing

Create a worker to index embeddings:

```kotlin
// EmbeddingIndexWorker.kt
class EmbeddingIndexWorker(
    context: Context,
    params: WorkerParameters,
    private val embeddingProvider: EmbeddingProvider,
    private val vectorService: VectorSearchService,
    private val database: VoiceOSDatabase
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // Index static commands
        database.voiceCommandQueries.getAllCommands().executeAsList().forEach { command ->
            val text = "${command.trigger_phrase} ${command.synonyms}"
            val embedding = embeddingProvider.embed(text)
            vectorService.indexCommand(command, source = "static")
        }

        // Index custom commands
        database.customCommandQueries.getAll().executeAsList().forEach { command ->
            vectorService.indexCommand(command.toEntity(), source = "custom")
        }

        // Index UI elements (batch for performance)
        database.scrapedElementQueries.getAll().executeAsList()
            .chunked(100)
            .forEach { batch ->
                batch.forEach { element ->
                    vectorService.indexElement(element)
                }
            }

        return Result.success()
    }
}
```

---

## Unified Schema (Post-AVA Merge)

After merging AVA + VoiceOS, the vector tables should be unified:

```sql
-- Unified vec_commands serves:
-- 1. AVA intents (source = 'ava_intent')
-- 2. VoiceOS static commands (source = 'static')
-- 3. VoiceOS custom commands (source = 'custom')
-- 4. Self-learned intents (source = 'learned')

CREATE VIRTUAL TABLE vec_commands USING vec0(
  command_id TEXT PRIMARY KEY,
  source TEXT NOT NULL,           -- 'ava_intent', 'static', 'custom', 'learned'
  locale TEXT NOT NULL,
  category TEXT,
  embedding float[384]
);

-- Single search query for all command types
searchAllCommands:
SELECT
  command_id,
  source,
  locale,
  category,
  vec_distance_cosine(embedding, :queryEmbedding) AS distance
FROM vec_commands
WHERE (locale = :locale OR locale = 'en-US')
  AND (source IN :sources OR :sources IS NULL)
ORDER BY distance
LIMIT :topK;
```

---

## Migration Path

### Pre-Merge (VoiceOS standalone)

1. Add sqlite-vec library to VoiceOS
2. Create vec_commands, vec_elements, vec_screens tables
3. Run background indexing
4. Update CommandManager to use vector search
5. Keep LIKE search as fallback

### Post-Merge (AVA + VoiceOS)

1. Merge vec_intents (AVA) + vec_commands (VoiceOS) → unified vec_commands
2. Migrate AVA RAG vec_chunks table
3. Share EmbeddingProvider between NLU and VoiceOS
4. Single VectorSearchService for all vector operations

---

## Testing Checklist

- [ ] sqlite-vec extension loads on Android API 24+
- [ ] Vector tables created successfully
- [ ] Command embedding insertion works
- [ ] Semantic search returns relevant results
- [ ] "turn off lights" matches "disable lighting"
- [ ] UI element search finds buttons by description
- [ ] Performance: <10ms for 10k vectors
- [ ] Fallback to LIKE search if extension fails

---

## Related Documentation

| Document | Purpose |
|----------|---------|
| [AVA sqlite-vec Spec](../../AVA/specs/AVA-Spec-SqliteVec-Integration-50512-V1.md) | AVA integration spec |
| [sqlite-vec docs](https://alexgarcia.xyz/sqlite-vec/) | Extension documentation |
| [SQLDelight Guide](https://sqldelight.github.io/sqldelight/) | SQLDelight usage |

---

## Notes for AI Implementation

1. **Embedding Model:** Use the same model as AVA (MobileBERT, 384-dim) for compatibility
2. **Extension Loading:** Must load vec0 extension before creating virtual tables
3. **Batch Processing:** Index elements in batches of 100 to avoid memory issues
4. **Fallback:** Always keep LIKE-based search as fallback if extension fails
5. **Post-Merge:** Coordinate with AVA's VectorSearchService for unified API

---

**Implementation Status:** WAITING FOR AVA MERGE

**Next Steps:**
1. Complete AVA-VoiceOS merge
2. Design unified database schema
3. Implement sqlite-vec for merged project
4. Migrate both AVA intents and VoiceOS commands to vec_commands

