# Chapter 53: SQLDelight Migration

**Status:** ✅ **MIGRATION COMPLETE**
**Last Updated:** 2025-11-30
**Module:** Universal/AVA/Core/Data
**Migration Type:** Room → SQLDelight
**Completion Date:** 2025-11-30

---

## Table of Contents

1. [Overview](#overview)
2. [Migration Summary](#migration-summary)
3. [What Was Removed](#what-was-removed)
4. [What Was Added/Updated](#what-was-addedupdated)
5. [Architecture After Migration](#architecture-after-migration)
6. [Code Examples](#code-examples)
7. [Token Cache System](#token-cache-system)
8. [References](#references)

---

## Overview

The AVA AI Core/Data module has been **fully migrated** from Room ORM to SQLDelight. This migration establishes a unified database technology across all repositories and enables Kotlin Multiplatform (KMP) support for future iOS/Desktop deployments.

### Migration Status

| Phase | Description | Status | Completion Date |
|-------|-------------|--------|-----------------|
| **Phase 1** | Schema Setup | ✅ COMPLETED | 2025-11-28 |
| **Phase 2** | Repository Migration | ✅ COMPLETED | 2025-11-30 |
| **Phase 3** | Room Removal | ✅ COMPLETED | 2025-11-30 |
| **Phase 4** | Token Cache Addition | ✅ COMPLETED | 2025-11-30 |
| **Phase 5** | Documentation | ✅ COMPLETED | 2025-11-30 |

### Key Metrics

| Metric | Value |
|--------|-------|
| **Room Code Removed** | 8,683 lines |
| **SQLDelight Code Added** | 1,367 lines |
| **Tables** | 12 (including TokenCache) |
| **Repositories Updated** | 6 |
| **Mappers Updated** | 7 |
| **Commits** | 2a3e3d9f, 09d53e12 |

---

## Migration Summary

### Commits

```
09d53e12 feat(data): add token cache for pre-tokenized database content
2a3e3d9f refactor(data): complete Room to SQLDelight migration
```

### Technology Stack

| Before | After |
|--------|-------|
| Room ORM | SQLDelight |
| Room DAOs | SQLDelight Queries |
| Room Entities | SQLDelight generated classes |
| KSP annotation processor | SQLDelight Gradle plugin |
| Android-only | KMP-ready |

---

## What Was Removed

### Files Deleted (8,683 lines)

**Room Entities (11 files)**
```
Universal/AVA/Core/Data/src/main/java/com/augmentalis/ava/core/data/entity/
├── ConversationEntity.kt
├── MessageEntity.kt
├── DecisionEntity.kt
├── LearningEntity.kt
├── MemoryEntity.kt
├── TrainExampleEntity.kt
├── TrainExampleFts.kt
├── IntentEmbeddingEntity.kt
├── IntentExampleEntity.kt
├── EmbeddingMetadata.kt
├── SemanticIntentOntologyEntity.kt
└── UserSequenceEntity.kt
```

**Room DAOs (11 files)**
```
Universal/AVA/Core/Data/src/main/java/com/augmentalis/ava/core/data/dao/
├── ConversationDao.kt
├── MessageDao.kt
├── DecisionDao.kt
├── LearningDao.kt
├── MemoryDao.kt
├── TrainExampleDao.kt
├── IntentEmbeddingDao.kt
├── IntentExampleDao.kt
├── EmbeddingMetadataDao.kt
├── SemanticIntentOntologyDao.kt
└── UserSequenceDao.kt
```

**Room Infrastructure**
```
├── AVADatabase.kt
├── DatabaseProvider.kt
├── converter/TypeConverters.kt
└── migration/DatabaseMigrations.kt
```

**RAG Module Room Code**
```
Universal/AVA/Features/RAG/src/androidMain/kotlin/.../room/
├── Daos.kt
├── Entities.kt
├── RAGDatabase.kt
└── EmbeddingConversions.kt
```

**Obsolete Tests (17 files)**
```
Universal/AVA/Core/Data/src/androidUnitTest/kotlin/.../
├── dao/*.kt (5 files)
├── entity/*.kt (6 files)
├── repository/*.kt (6 files)
└── converter/TypeConvertersTest.kt
```

### Dependencies Removed

```kotlin
// build.gradle.kts - REMOVED
plugins {
    alias(libs.plugins.ksp)  // No longer needed
}

dependencies {
    // Room - REMOVED
    api(libs.room.runtime)
    api(libs.room.ktx)
    ksp(libs.room.compiler)
}
```

---

## What Was Added/Updated

### SQLDelight Schema Files (12 tables)

```
Universal/AVA/Core/Data/src/main/sqldelight/com/augmentalis/ava/core/data/db/
├── Conversation.sq          # Chat conversations
├── Message.sq               # Chat messages
├── Decision.sq              # Decision logging
├── Learning.sq              # Feedback tracking
├── Memory.sq                # Long-term memory
├── TrainExample.sq          # Training examples
├── TrainExampleFts.sq       # FTS4 search
├── IntentEmbedding.sq       # Pre-computed embeddings
├── IntentExample.sq         # Intent examples
├── EmbeddingMetadata.sq     # Model versions
├── SemanticIntentOntology.sq # AON support
└── TokenCache.sq            # NEW: Token caching
```

### Updated Mappers (7 files)

All mappers now convert between SQLDelight generated classes and domain models:

```kotlin
// Example: MessageMapper.kt
import com.augmentalis.ava.core.data.db.Message as DbMessage

fun DbMessage.toDomain(): Message {
    return Message(
        id = id,
        conversationId = conversation_id,
        role = MessageRole.valueOf(role),
        content = content,
        timestamp = timestamp,
        // ...
    )
}
```

| Mapper | SQLDelight Class | Domain Class |
|--------|------------------|--------------|
| MessageMapper | `Message` | `Message` |
| ConversationMapper | `Conversation` | `Conversation` |
| TrainExampleMapper | `Train_example` | `TrainExample` |
| LearningMapper | `Learning` | `Learning` |
| DecisionMapper | `Decision` | `Decision` |
| MemoryMapper | `Memory` | `Memory` |
| TokenCacheMapper | `Token_cache` | `TokenCache` |

### Updated Repositories (6 files)

All repositories now inject SQLDelight `*Queries` instead of Room DAOs:

```kotlin
// Before (Room)
class MessageRepositoryImpl(
    private val messageDao: MessageDao,
    private val conversationDao: ConversationDao
)

// After (SQLDelight)
class MessageRepositoryImpl(
    private val messageQueries: MessageQueries,
    private val conversationQueries: ConversationQueries
)
```

---

## Architecture After Migration

### Data Layer Structure

```
Universal/AVA/Core/Data/
├── build.gradle.kts                    # SQLDelight config only
└── src/main/
    ├── java/.../data/
    │   ├── mapper/                     # Domain ↔ SQLDelight
    │   │   ├── MessageMapper.kt
    │   │   ├── ConversationMapper.kt
    │   │   ├── TrainExampleMapper.kt
    │   │   ├── LearningMapper.kt
    │   │   ├── DecisionMapper.kt
    │   │   ├── MemoryMapper.kt
    │   │   └── TokenCacheMapper.kt
    │   └── repository/                 # SQLDelight implementations
    │       ├── MessageRepositoryImpl.kt
    │       ├── ConversationRepositoryImpl.kt
    │       ├── TrainExampleRepositoryImpl.kt
    │       ├── LearningRepositoryImpl.kt
    │       ├── DecisionRepositoryImpl.kt
    │       ├── MemoryRepositoryImpl.kt
    │       └── TokenCacheRepositoryImpl.kt
    └── sqldelight/.../db/              # Schema files
        ├── *.sq (12 files)
        └── databases/                  # Generated schema
```

### Build Configuration

```kotlin
// build.gradle.kts
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.sqldelight)
    // KSP removed - was only used for Room
}

dependencies {
    // SQLDelight - KMP database (primary)
    // Room has been removed - SQLDelight is now the only database layer
    api(libs.sqldelight.runtime)
    api(libs.sqldelight.android.driver)
}

sqldelight {
    databases {
        create("AVADatabase") {
            packageName.set("com.augmentalis.ava.core.data.db")
            dialect("app.cash.sqldelight:sqlite-3-38-dialect:2.0.1")
            schemaOutputDirectory.set(file("src/main/sqldelight/databases"))
            deriveSchemaFromMigrations.set(true)
        }
    }
}
```

---

## Code Examples

### Repository Pattern (After Migration)

```kotlin
class MessageRepositoryImpl(
    private val messageQueries: MessageQueries,
    private val conversationQueries: ConversationQueries
) : MessageRepository {

    override fun getMessagesForConversation(conversationId: String): Flow<List<Message>> {
        return messageQueries.selectByConversationId(conversationId)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { messages -> messages.map { it.toDomain() } }
    }

    override suspend fun addMessage(message: Message): Result<Message> =
        withContext(Dispatchers.IO) {
            try {
                val params = message.toInsertParams()
                messageQueries.insert(
                    id = params.id,
                    conversation_id = params.conversation_id,
                    role = params.role,
                    content = params.content,
                    timestamp = params.timestamp,
                    intent = params.intent,
                    confidence = params.confidence,
                    metadata = params.metadata
                )

                // Update denormalized count
                conversationQueries.incrementMessageCount(
                    updated_at = message.timestamp,
                    id = message.conversationId
                )

                Result.Success(message)
            } catch (e: Exception) {
                Result.Error(exception = e, message = "Failed to add message")
            }
        }
}
```

### Mapper Pattern

```kotlin
// MessageMapper.kt
import com.augmentalis.ava.core.data.db.Message as DbMessage

fun DbMessage.toDomain(): Message {
    return Message(
        id = id,
        conversationId = conversation_id,
        role = MessageRole.valueOf(role),
        content = content,
        timestamp = timestamp,
        intent = intent,
        confidence = confidence?.toFloat(),
        metadata = metadata?.let { json.decodeFromString(it) }
    )
}

fun Message.toInsertParams(): MessageInsertParams {
    return MessageInsertParams(
        id = id,
        conversation_id = conversationId,
        role = role.name,
        content = content,
        timestamp = timestamp,
        intent = intent,
        confidence = confidence?.toDouble(),
        metadata = metadata?.let { json.encodeToString(it) }
    )
}
```

---

## Token Cache System

A new Token Cache table was added during the migration to support pre-tokenized database content:

### Schema (TokenCache.sq)

```sql
CREATE TABLE token_cache (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    text_hash TEXT NOT NULL,
    model_id TEXT NOT NULL,
    token_ids BLOB NOT NULL,
    token_count INTEGER NOT NULL,
    created_at INTEGER NOT NULL,
    last_accessed INTEGER NOT NULL,
    access_count INTEGER NOT NULL DEFAULT 0,
    source_type TEXT NOT NULL,
    source_id TEXT
);

CREATE UNIQUE INDEX idx_token_cache_lookup
ON token_cache(text_hash, model_id);
```

### Features

| Feature | Description |
|---------|-------------|
| **Hash-based lookup** | MD5 hash of text for fast cache hits |
| **Model-aware** | Invalidates when model changes (different vocabularies) |
| **Source tracking** | Links to MESSAGE, MEMORY, TRAIN_EXAMPLE, etc. |
| **Binary storage** | Token IDs as BLOB (4 bytes per token) |
| **LRU cleanup** | Removes old entries based on last_accessed |

### Usage

```kotlin
// In LLM module
val tokenCacheManager = TokenCacheManager(tokenCacheRepository)
tokenCacheManager.setTokenizer(tokenizer, modelId)

// Get cached or tokenize new
val tokens = tokenCacheManager.getOrTokenize(
    text = "Hello, world!",
    sourceType = TokenCacheSourceType.MESSAGE,
    sourceId = "msg-123"
)
```

See **Chapter 55: Token Cache System** for full documentation.

---

## References

### Related Commits

```
09d53e12 feat(data): add token cache for pre-tokenized database content
2a3e3d9f refactor(data): complete Room to SQLDelight migration
f27d6b54 feat(llm): add binary caching for HuggingFace tokenizer
4337bf5f fix(llm): implement HuggingFace tokenizer for model-specific vocab
```

### Related Documentation

- **Chapter 55:** Token Cache System
- **Chapter 28:** RAG System Architecture
- **Chapter 52:** RAG System Architecture (updated)

### External Resources

- [SQLDelight Documentation](https://cashapp.github.io/sqldelight/)
- [SQLite FTS4](https://www.sqlite.org/fts3.html)
- [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html)

---

## Summary

The Room to SQLDelight migration is **100% complete**:

| Component | Status |
|-----------|--------|
| Room entities | ✅ Deleted |
| Room DAOs | ✅ Deleted |
| Room database | ✅ Deleted |
| Room dependencies | ✅ Removed |
| Repositories | ✅ Updated to SQLDelight |
| Mappers | ✅ Updated for SQLDelight |
| Token Cache | ✅ Added |
| Tests | ✅ Obsolete tests removed |

**SQLDelight is now the only database layer for AVA.**

---

**Chapter Status:** ✅ **COMPLETE**
**Migration Status:** ✅ **FULLY COMPLETE**
**Last Updated:** 2025-11-30
