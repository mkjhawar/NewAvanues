# AVA AI - JSON Data Flow Architecture

**Date:** 2025-11-13
**Author:** Analysis by Claude Code
**Status:** Current Implementation (with issues identified)

---

## Executive Summary

AVA uses JSON in 3 distinct patterns:
1. ✅ **Static Assets** → One-time migration (GOOD)
2. ❌ **Embeddings** → Stored as JSON in SQLite (BAD - performance issue)
3. ⚠️ **Metadata** → Stored as JSON in SQLite (QUESTIONABLE - flexibility vs performance)

---

## High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         AVA AI - JSON Data Flow                              │
│                                                                              │
│  INPUT → JSON Processing → SQLite Storage → Runtime Deserialization → APP   │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Detailed Data Flow Diagram

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                           INPUT SOURCES (3 Types)                             │
└──────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────┐  ┌─────────────────────────┐  ┌──────────────────┐
│   1. Static Assets      │  │   2. Runtime Generated  │  │ 3. ML Embeddings │
│   (JSON Files)          │  │   (User Data)           │  │ (Neural Network) │
├─────────────────────────┤  ├─────────────────────────┤  ├──────────────────┤
│                         │  │                         │  │                  │
│ intent_examples.json    │  │ User creates:           │  │ ONNX Model       │
│ ┌─────────────────────┐ │  │ • Conversations         │  │ Output:          │
│ │{                    │ │  │ • Messages              │  │ List<Float>      │
│ │ "control_lights": [ │ │  │ • Memories              │  │ [0.123, -0.456,  │
│ │   "Turn on lights", │ │  │                         │  │  0.789, ...]     │
│ │   "Lights on"       │ │  │ With metadata:          │  │                  │
│ │ ],                  │ │  │ Map<String, String>     │  │ 384 dimensions   │
│ │ "set_alarm": [...]  │ │  │ {"key": "value"}        │  │ ~1.5KB per vector│
│ │}                    │ │  │                         │  │                  │
│ └─────────────────────┘ │  └─────────────────────────┘  └──────────────────┘
│                         │
│ Size: 2.1 KB            │
│ Intents: 9              │
│ Examples: 69            │
└────────┬────────────────┘
         │
         │ ✅ ONE-TIME MIGRATION
         │ (App first launch)
         │
         ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                    IntentExamplesMigration.kt                                 │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                               │
│  fun migrate() {                                                              │
│    val json = context.assets.open("intent_examples.json")                    │
│    val data = JSONObject(json.readText())                                    │
│                                                                               │
│    data.keys().forEach { intent ->                                           │
│      val examples = data.getJSONArray(intent)                                │
│      examples.forEach { example ->                                           │
│        val hash = MD5("$intent|$example")                                    │
│        dao.insert(IntentExampleEntity(                                       │
│          id = hash,                                                          │
│          intentId = intent,           // ✅ Proper column                   │
│          exampleText = example        // ✅ Proper column                   │
│        ))                                                                    │
│      }                                                                       │
│    }                                                                         │
│  }                                                                           │
│                                                                               │
│  ✅ GOOD PATTERN:                                                            │
│  • JSON → Parse once → Proper DB columns                                    │
│  • Deduplication via hash                                                   │
│  • No JSON in database                                                      │
└───────────────────────────────┬──────────────────────────────────────────────┘
                                │
                                │ Store as proper columns
                                ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                         SQLite Database (Room)                                │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                               │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ Table: intent_examples                                              │    │
│  │ ┌─────────────────┬─────────────────┬──────────────────────────┐   │    │
│  │ │ id (TEXT)       │ intentId (TEXT) │ exampleText (TEXT)       │   │    │
│  │ ├─────────────────┼─────────────────┼──────────────────────────┤   │    │
│  │ │ a3f8c9d...      │ control_lights  │ Turn on the lights       │   │    │
│  │ │ b2e7a1c...      │ control_lights  │ Switch off the lights    │   │    │
│  │ │ c9d4f2b...      │ set_alarm       │ Set alarm for 7am        │   │    │
│  │ └─────────────────┴─────────────────┴──────────────────────────┘   │    │
│  │                                                                     │    │
│  │ ✅ GOOD: Proper normalized structure, indexable, queryable         │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                                                               │
└──────────────────────────────────────────────────────────────────────────────┘

         ┌──────────────────────────────────────────────────────┐
         │                                                       │
         │  MEANWHILE: Runtime User Data Flow                   │
         │                                                       │
         └──────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────────────┐
│                  User Creates Data at Runtime                                 │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                               │
│  ChatViewModel.sendMessage("Turn on lights") {                                │
│                                                                               │
│    // 1. Create Message domain object                                        │
│    val message = Message(                                                    │
│      id = UUID.randomUUID(),                                                 │
│      conversationId = activeConversation,                                    │
│      content = "Turn on lights",                                             │
│      sender = MessageSender.USER,                                            │
│      timestamp = System.currentTimeMillis(),                                 │
│      intent = "control_lights",       // From NLU                           │
│      confidence = 0.95f,                                                     │
│      metadata = mapOf(                // ⚠️ Flexible metadata               │
│        "mood" to "neutral",                                                  │
│        "language" to "en"                                                    │
│      )                                                                       │
│    )                                                                         │
│                                                                               │
│    // 2. Save to repository                                                  │
│    messageRepository.addMessage(message)                                     │
│  }                                                                           │
│                                                                               │
└────────────────────────────────────┬─────────────────────────────────────────┘
                                     │
                                     │ Map to entity
                                     ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                       MessageMapper.kt (Data Layer)                           │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                               │
│  fun toEntity(message: Message): MessageEntity {                              │
│    return MessageEntity(                                                     │
│      id = message.id,                                                        │
│      conversationId = message.conversationId,                                │
│      content = message.content,                   // ✅ Proper column       │
│      sender = message.sender.name,                // ✅ Proper column       │
│      timestamp = message.timestamp,               // ✅ Proper column       │
│      intent = message.intent,                     // ✅ Proper column       │
│      confidence = message.confidence,             // ✅ Proper column       │
│                                                                               │
│      metadata = json.encodeToString(              // ❌ JSON BLOB!          │
│        message.metadata                                                      │
│      )  // Result: '{"mood":"neutral","language":"en"}'                     │
│    )                                                                         │
│  }                                                                           │
│                                                                               │
│  ⚠️ PROBLEM:                                                                 │
│  • Can't query: WHERE metadata.mood = 'happy'                               │
│  • Can't index metadata fields                                              │
│  • Must parse JSON on every read                                            │
│                                                                               │
└────────────────────────────────────┬─────────────────────────────────────────┘
                                     │
                                     │ Save with JSON string
                                     ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                         SQLite Database (Room)                                │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                               │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ Table: messages                                                     │    │
│  │ ┌────────┬─────────┬────────┬──────────────────────────────────┐   │    │
│  │ │ id     │ content │ intent │ metadata (TEXT)                  │   │    │
│  │ ├────────┼─────────┼────────┼──────────────────────────────────┤   │    │
│  │ │ msg1   │ Turn... │ contr..│ {"mood":"neutral","lang":"en"}   │   │    │
│  │ │ msg2   │ What... │ check..│ {"mood":"curious","lang":"en"}   │   │    │
│  │ └────────┴─────────┴────────┴──────────────────────────────────┘   │    │
│  │                                                                     │    │
│  │ ⚠️ ISSUE: metadata stored as JSON string                           │    │
│  │ • Size: Small (~100 bytes) - ACCEPTABLE                            │    │
│  │ • Queries: Rare - ACCEPTABLE                                       │    │
│  │ • Flexibility: High - GOOD                                         │    │
│  │ Verdict: ⚠️ TOLERABLE (but not ideal)                             │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                                                               │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ Table: conversations                                                │    │
│  │ ┌────────┬─────────┬──────────────────────────────────────────┐    │    │
│  │ │ id     │ title   │ metadata (TEXT)                          │    │    │
│  │ ├────────┼─────────┼──────────────────────────────────────────┤    │    │
│  │ │ conv1  │ Chat... │ {"language":"en","timezone":"PST"}       │    │    │
│  │ └────────┴─────────┴──────────────────────────────────────────┘    │    │
│  │                                                                     │    │
│  │ ⚠️ ISSUE: Same as messages                                         │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                                                               │
└──────────────────────────────────────────────────────────────────────────────┘

         ┌──────────────────────────────────────────────────────┐
         │                                                       │
         │  CRITICAL PROBLEM: Embeddings (ML Vectors)           │
         │                                                       │
         └──────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────────────┐
│                    ONNX Model / RAG System                                    │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                               │
│  ONNXEmbeddingProvider.embed("Turn on the lights") {                          │
│                                                                               │
│    // 1. Tokenize text                                                       │
│    val tokens = tokenizer.encode("Turn on the lights")                       │
│    // [101, 2735, 2006, 1996, 4597, 102]                                    │
│                                                                               │
│    // 2. Run ONNX inference                                                  │
│    val output = onnxSession.run(tokens)                                      │
│                                                                               │
│    // 3. Extract embedding vector                                            │
│    val embedding: List<Float> = output.getFloatArray()                       │
│    // [0.12345, -0.67890, 0.11111, ..., 384 values]                         │
│    // Size: 384 floats × 4 bytes = 1,536 bytes                              │
│                                                                               │
│    return embedding                                                          │
│  }                                                                           │
│                                                                               │
└────────────────────────────────────┬─────────────────────────────────────────┘
                                     │
                                     │ Store embedding
                                     ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                    TypeConverters.kt (Room Converter)                         │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                               │
│  @TypeConverter                                                               │
│  fun fromFloatList(value: List<Float>?): String? {                            │
│    return value?.let { json.encodeToString(it) }                             │
│    // Result: "[0.12345,-0.67890,0.11111,...,0.54321]"                      │
│    // Size: 384 floats × ~10 chars = ~3,840 bytes                           │
│  }                                                                           │
│                                                                               │
│  ❌ CRITICAL PROBLEM:                                                        │
│  • 2.5x larger than binary (3,840 vs 1,536 bytes)                           │
│  • JSON parsing on EVERY read                                               │
│  • Can't use SQL for vector operations                                      │
│  • No indexing possible                                                     │
│  • Memory inefficient                                                       │
│                                                                               │
│  IMPACT:                                                                     │
│  • 10,000 embeddings = 38 MB (vs 15 MB binary)                              │
│  • Every query requires JSON.parse() × count                                │
│  • Can't do: SELECT * ORDER BY cosine_similarity(embedding, ?)             │
│                                                                               │
└────────────────────────────────────┬─────────────────────────────────────────┘
                                     │
                                     │ ❌ Store as JSON TEXT
                                     ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                         SQLite Database (Room)                                │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                               │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ Table: memory (Long-term Memory)                                    │    │
│  │ ┌────────┬─────────┬──────────────────────────────────────────┐    │    │
│  │ │ id     │ content │ embedding (TEXT) ❌                      │    │    │
│  │ ├────────┼─────────┼──────────────────────────────────────────┤    │    │
│  │ │ mem1   │ User... │ [0.123,-0.456,0.789,...384 values]       │    │    │
│  │ │        │         │ Size: ~3.8 KB per row                    │    │    │
│  │ │ mem2   │ User... │ [0.111,-0.222,0.333,...384 values]       │    │    │
│  │ └────────┴─────────┴──────────────────────────────────────────┘    │    │
│  │                                                                     │    │
│  │ ❌ CRITICAL: No vector search possible                             │    │
│  │   Must load ALL embeddings, parse JSON, compute similarity in RAM  │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                                                               │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ Table: chunks (RAG Documents)                                       │    │
│  │ ┌────────┬─────────┬──────────────────────┬──────────────────┐    │    │
│  │ │ id     │ content │ embedding_blob (BLOB)│ metadata_json    │    │    │
│  │ ├────────┼─────────┼──────────────────────┼──────────────────┤    │    │
│  │ │ chunk1 │ Docum...│ <binary data>        │ {"page":1,...}   │    │    │
│  │ │        │         │ Size: 1.5 KB ✅      │ ❌ JSON          │    │    │
│  │ │ chunk2 │ Docum...│ <binary data>        │ {"page":2,...}   │    │    │
│  │ └────────┴─────────┴──────────────────────┴──────────────────┘    │    │
│  │                                                                     │    │
│  │ ✅ GOOD: Binary embeddings                                         │    │
│  │ ❌ BAD: Still can't do vector search in SQL                        │    │
│  │ ⚠️ BAD: metadata_json should be normalized                        │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                                                               │
└────────────────────────────────────┬─────────────────────────────────────────┘
                                     │
                                     │ Application queries
                                     ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                      Application Runtime (Reading Data)                       │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                               │
│  ┌───────────────────────────────────────────────────────────────────┐      │
│  │ QUERY 1: Get all memories (with embeddings)                      │      │
│  │                                                                   │      │
│  │ MemoryRepository.getAllMemories() {                               │      │
│  │   val entities = dao.getAll()  // SQL query                      │      │
│  │                                                                   │      │
│  │   return entities.map { entity ->                                │      │
│  │     Memory(                                                      │      │
│  │       id = entity.id,                                            │      │
│  │       content = entity.content,                                  │      │
│  │       embedding = json.decodeFromString<List<Float>>(            │      │
│  │         entity.embedding  // ❌ Parse 3.8KB JSON per row!       │      │
│  │       ),                                                         │      │
│  │       metadata = json.decodeFromString<Map<String,String>>(      │      │
│  │         entity.metadata   // ⚠️ Parse JSON again                │      │
│  │       )                                                          │      │
│  │     )                                                            │      │
│  │   }                                                              │      │
│  │ }                                                                │      │
│  │                                                                   │      │
│  │ Performance: O(n) JSON parsing where n = result count            │      │
│  │ • 1,000 results = 1,000 × JSON.parse() = ~500ms                 │      │
│  │ • 10,000 results = 10,000 × JSON.parse() = ~5,000ms (5 sec!)    │      │
│  └───────────────────────────────────────────────────────────────────┘      │
│                                                                               │
│  ┌───────────────────────────────────────────────────────────────────┐      │
│  │ QUERY 2: Semantic search (find similar memories)                 │      │
│  │                                                                   │      │
│  │ MemoryRepository.findSimilar(queryEmbedding: List<Float>) {      │      │
│  │   // ❌ PROBLEM: Must load ALL memories into RAM                 │      │
│  │   val allMemories = dao.getAll()  // 10,000 rows                │      │
│  │                                                                   │      │
│  │   // ❌ Parse all JSON embeddings                                │      │
│  │   val parsed = allMemories.map { entity ->                       │      │
│  │     entity to json.decodeFromString<List<Float>>(                │      │
│  │       entity.embedding  // 10,000 × JSON.parse()!               │      │
│  │     )                                                            │      │
│  │   }                                                              │      │
│  │                                                                   │      │
│  │   // ❌ Compute similarity in application (not SQL)              │      │
│  │   val results = parsed.map { (entity, embedding) ->              │      │
│  │     entity to cosineSimilarity(queryEmbedding, embedding)        │      │
│  │   }                                                              │      │
│  │   .sortedByDescending { it.second }                              │      │
│  │   .take(10)                                                      │      │
│  │                                                                   │      │
│  │   return results                                                 │      │
│  │ }                                                                │      │
│  │                                                                   │      │
│  │ Performance: TERRIBLE                                            │      │
│  │ • Load 10,000 rows from DB = ~100ms                              │      │
│  │ • Parse 10,000 JSON strings = ~5,000ms                           │      │
│  │ • Compute 10,000 similarities = ~1,000ms                         │      │
│  │ • TOTAL: ~6,100ms (6 seconds!) for ONE query                     │      │
│  │                                                                   │      │
│  │ ❌ Should be: ~10ms with proper vector index                     │      │
│  └───────────────────────────────────────────────────────────────────┘      │
│                                                                               │
└──────────────────────────────────────────────────────────────────────────────┘
```

---

## Performance Comparison Table

| Operation | Current (JSON) | Binary BLOB | Vector Index | Improvement |
|-----------|---------------|-------------|--------------|-------------|
| **Storage Size** | | | | |
| Single embedding | 3,840 bytes | 1,536 bytes | 1,536 bytes | 60% smaller |
| 10,000 embeddings | 38 MB | 15 MB | 15 MB + index | 60% smaller |
| **Query Performance** | | | | |
| Load & parse 1 embedding | ~0.5 ms | ~0.05 ms | ~0.05 ms | 10x faster |
| Load & parse 1,000 embeddings | ~500 ms | ~50 ms | ~50 ms | 10x faster |
| Semantic search (10k vectors) | ~6,000 ms | ~1,100 ms | ~10 ms | **600x faster** |
| **Functionality** | | | | |
| SQL WHERE on embedding | ❌ No | ❌ No | ✅ Yes | Possible |
| Vector similarity in SQL | ❌ No | ❌ No | ✅ Yes | Possible |
| Index support | ❌ No | ❌ No | ✅ Yes | Possible |

---

## Summary by Storage Type

### ✅ GOOD: Static JSON → Proper Columns
```
intent_examples.json → IntentExampleEntity
  ✅ JSON parsed once at migration
  ✅ Stored as proper columns (intentId, exampleText)
  ✅ Fully queryable and indexable
  ✅ No runtime JSON overhead
```

### ⚠️ TOLERABLE: Small Metadata JSON
```
Message.metadata → MessageEntity.metadata: String (JSON)
  ⚠️ Small size (~100 bytes)
  ⚠️ Rarely queried
  ⚠️ Flexible schema beneficial
  ⚠️ Acceptable for this use case

  RECOMMENDATION: Monitor; extract to columns if queries needed
```

### ❌ CRITICAL: Embeddings as JSON
```
Memory.embedding → MemoryEntity.embedding: String (JSON)
  ❌ Large size (3.8 KB per row)
  ❌ Frequently accessed
  ❌ No vector search capability
  ❌ Slow JSON parsing on every read
  ❌ Memory inefficient

  RECOMMENDATION: Migrate to BLOB immediately (60% size reduction)
  FUTURE: Add vector search extension (600x performance gain)
```

---

## Files Referenced

**JSON Source Files:**
- `apps/ava-standalone/src/main/assets/intent_examples.json`
- `Universal/AVA/Features/LLM/src/main/assets/mlc-app-config.json`

**JSON Processing:**
- `Universal/AVA/Features/NLU/src/androidMain/kotlin/.../IntentExamplesMigration.kt`
- `Universal/AVA/Core/Data/src/main/java/.../TypeConverters.kt`

**Entities with JSON:**
- `MemoryEntity.kt` - embedding: String?, metadata: String?
- `MessageEntity.kt` - metadata: String?
- `ConversationEntity.kt` - metadata: String?
- `DocumentEntity.kt` (RAG) - metadata_json: String
- `ChunkEntity.kt` (RAG) - embedding_blob: ByteArray ✅, but no vector search

---

## Next Steps

1. **Immediate (High Priority):**
   - Migrate `MemoryEntity.embedding` from String (JSON) to ByteArray (BLOB)
   - Document performance improvement

2. **Short-term (Medium Priority):**
   - Extract common metadata fields to proper columns
   - Add indexes on extracted fields

3. **Long-term (Future):**
   - Evaluate SQLite vector extensions (sqlite-vec, sqlite-vss)
   - Consider separate vector database (Milvus, Qdrant, Chroma)
   - Benchmark hybrid approach (SQLite + external vector DB)

---

**End of Diagram**
