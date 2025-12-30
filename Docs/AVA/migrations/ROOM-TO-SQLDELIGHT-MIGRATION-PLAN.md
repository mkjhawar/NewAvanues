# Room to SQLDelight Migration Plan

**Date:** 2025-11-25
**Purpose:** Cross-Platform Database Unification
**Target:** All Augmentalis repositories migrating to unified AVA app
**Status:** Planning Phase

---

## Executive Summary

As part of the multi-repository merge (VoiceOS, AVA, WebAvanue, AvaConnect, Avanues) into a single cross-platform application, we need to migrate all Room database implementations to SQLDelight for true multiplatform support.

**Why SQLDelight?**
- True Kotlin Multiplatform support (Android, iOS, Desktop, Web)
- Type-safe SQL queries at compile time
- Same schema across all platforms
- Room only supports Android

---

## Current State Audit

### Database Technology by Repository

| Repository | Module | Database | Status | Priority |
|------------|--------|----------|--------|----------|
| **MainAvanues** | WebAvanue/coredata | SQLDelight | ✅ Already migrated | - |
| **WebAvanue** | BrowserCoreData | SQLDelight | ✅ Already migrated | - |
| **Avanues** | AssetManager | SQLDelight | ✅ Already migrated | - |
| **AVA** | Core/Data | Room | ❌ Needs migration | HIGH |
| **AVA** | Features/RAG | Room | ❌ Needs migration | HIGH |
| **VoiceOS** | LocalizationManager | Room | ❌ Needs migration | MEDIUM |
| **VoiceOS** | database (disabled) | SQLDelight | ⏸ Partial migration | MEDIUM |
| **Avanues** | voiceos/app | Room | ❌ Needs migration | LOW |
| **Avanues** | uuidcreator | Room | ❌ Needs migration | LOW |
| **AvaConnect** | - | None | ✅ No migration needed | - |

---

## Migration Order

Based on dependencies and criticality:

```
Phase 1: AVA Core/Data (Foundation)
    └── All features depend on this

Phase 2: AVA Features/RAG (Feature Module)
    └── Uses Core/Data patterns

Phase 3: VoiceOS LocalizationManager
    └── Independent module

Phase 4: Avanues Android modules
    └── voiceos/app
    └── uuidcreator
```

---

## Phase 1: AVA Core/Data Migration

### Current Room Implementation

**Location:** `Universal/AVA/Core/Data/`

**Entities:**
- ConversationEntity
- MessageEntity
- IntentExampleEntity
- IntentEmbeddingEntity
- SemanticIntentOntologyEntity
- TrainExampleEntity
- DecisionEntity
- DecisionActionEntity

**DAOs:**
- ConversationDao
- MessageDao
- IntentExampleDao
- IntentEmbeddingDao
- SemanticIntentOntologyDao
- TrainExampleDao
- DecisionDao

**Type Converters:**
- TypeConverters (Map, FloatList → BLOB)
- StringListConverter (List<String> → JSON)

### Migration Steps

#### Step 1.1: Setup SQLDelight Infrastructure
```kotlin
// build.gradle.kts additions
plugins {
    id("app.cash.sqldelight") version "2.0.1"
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation("app.cash.sqldelight:runtime:2.0.1")
                implementation("app.cash.sqldelight:coroutines-extensions:2.0.1")
            }
        }
        androidMain {
            dependencies {
                implementation("app.cash.sqldelight:android-driver:2.0.1")
            }
        }
        iosMain {
            dependencies {
                implementation("app.cash.sqldelight:native-driver:2.0.1")
            }
        }
        desktopMain {
            dependencies {
                implementation("app.cash.sqldelight:sqlite-driver:2.0.1")
            }
        }
    }
}

sqldelight {
    databases {
        create("AVADatabase") {
            packageName.set("com.augmentalis.ava.core.data.db")
            dialect("app.cash.sqldelight:sqlite-3-38-dialect:2.0.1")
            schemaOutputDirectory.set(file("src/commonMain/sqldelight/databases"))
            deriveSchemaFromMigrations.set(true)
        }
    }
}
```

#### Step 1.2: Create SQLDelight Schema Files

**File:** `src/commonMain/sqldelight/com/augmentalis/ava/core/data/db/Conversation.sq`
```sql
CREATE TABLE Conversation (
    id TEXT PRIMARY KEY NOT NULL,
    title TEXT NOT NULL,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    is_archived INTEGER NOT NULL DEFAULT 0,
    metadata TEXT
);

getAllConversations:
SELECT * FROM Conversation ORDER BY updated_at DESC;

getConversationById:
SELECT * FROM Conversation WHERE id = ?;

insertConversation:
INSERT OR REPLACE INTO Conversation (id, title, created_at, updated_at, is_archived, metadata)
VALUES (?, ?, ?, ?, ?, ?);

deleteConversation:
DELETE FROM Conversation WHERE id = ?;

updateConversationTitle:
UPDATE Conversation SET title = ?, updated_at = ? WHERE id = ?;
```

**File:** `src/commonMain/sqldelight/com/augmentalis/ava/core/data/db/Message.sq`
```sql
CREATE TABLE Message (
    id TEXT PRIMARY KEY NOT NULL,
    conversation_id TEXT NOT NULL,
    role TEXT NOT NULL,
    content TEXT NOT NULL,
    timestamp INTEGER NOT NULL,
    metadata TEXT,
    FOREIGN KEY (conversation_id) REFERENCES Conversation(id) ON DELETE CASCADE
);

CREATE INDEX message_conversation_idx ON Message(conversation_id);

getMessagesForConversation:
SELECT * FROM Message WHERE conversation_id = ? ORDER BY timestamp ASC;

insertMessage:
INSERT OR REPLACE INTO Message (id, conversation_id, role, content, timestamp, metadata)
VALUES (?, ?, ?, ?, ?, ?);

deleteMessage:
DELETE FROM Message WHERE id = ?;

deleteMessagesForConversation:
DELETE FROM Message WHERE conversation_id = ?;
```

**File:** `src/commonMain/sqldelight/com/augmentalis/ava/core/data/db/IntentExample.sq`
```sql
CREATE TABLE IntentExample (
    example_hash TEXT PRIMARY KEY NOT NULL,
    intent_id TEXT NOT NULL,
    example_text TEXT NOT NULL,
    is_primary INTEGER NOT NULL DEFAULT 0,
    source TEXT NOT NULL,
    locale TEXT NOT NULL,
    created_at INTEGER NOT NULL,
    usage_count INTEGER NOT NULL DEFAULT 0,
    last_used INTEGER
);

CREATE INDEX intent_example_intent_idx ON IntentExample(intent_id);
CREATE INDEX intent_example_locale_idx ON IntentExample(locale);

getAllExamples:
SELECT * FROM IntentExample;

getExamplesByIntent:
SELECT * FROM IntentExample WHERE intent_id = ?;

insertExample:
INSERT OR REPLACE INTO IntentExample
(example_hash, intent_id, example_text, is_primary, source, locale, created_at, usage_count, last_used)
VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);

deleteExample:
DELETE FROM IntentExample WHERE example_hash = ?;

incrementUsageCount:
UPDATE IntentExample SET usage_count = usage_count + 1, last_used = ? WHERE example_hash = ?;
```

**File:** `src/commonMain/sqldelight/com/augmentalis/ava/core/data/db/IntentEmbedding.sq`
```sql
CREATE TABLE IntentEmbedding (
    intent_id TEXT PRIMARY KEY NOT NULL,
    embedding BLOB NOT NULL,
    model_version TEXT NOT NULL,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);

getAllEmbeddings:
SELECT * FROM IntentEmbedding;

getEmbeddingByIntent:
SELECT * FROM IntentEmbedding WHERE intent_id = ?;

insertEmbedding:
INSERT OR REPLACE INTO IntentEmbedding (intent_id, embedding, model_version, created_at, updated_at)
VALUES (?, ?, ?, ?, ?);

deleteEmbedding:
DELETE FROM IntentEmbedding WHERE intent_id = ?;

deleteAllEmbeddings:
DELETE FROM IntentEmbedding;
```

**File:** `src/commonMain/sqldelight/com/augmentalis/ava/core/data/db/SemanticIntentOntology.sq`
```sql
CREATE TABLE SemanticIntentOntology (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    intent_id TEXT NOT NULL,
    locale TEXT NOT NULL,
    canonical_form TEXT NOT NULL,
    description TEXT NOT NULL,
    synonyms TEXT NOT NULL,
    action_type TEXT NOT NULL,
    action_sequence TEXT NOT NULL,
    required_capabilities TEXT NOT NULL,
    ontology_file_source TEXT NOT NULL,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    UNIQUE(intent_id, locale)
);

CREATE INDEX ontology_locale_idx ON SemanticIntentOntology(locale);
CREATE INDEX ontology_canonical_idx ON SemanticIntentOntology(canonical_form);

getAllOntologies:
SELECT * FROM SemanticIntentOntology;

getOntologyByIntentAndLocale:
SELECT * FROM SemanticIntentOntology WHERE intent_id = ? AND locale = ?;

getOntologiesByLocale:
SELECT * FROM SemanticIntentOntology WHERE locale = ?;

insertOntology:
INSERT OR REPLACE INTO SemanticIntentOntology
(intent_id, locale, canonical_form, description, synonyms, action_type, action_sequence, required_capabilities, ontology_file_source, created_at, updated_at)
VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);

deleteOntology:
DELETE FROM SemanticIntentOntology WHERE id = ?;

deleteAll:
DELETE FROM SemanticIntentOntology;
```

#### Step 1.3: Create Platform-Specific Drivers

**File:** `src/androidMain/kotlin/com/augmentalis/ava/core/data/db/DriverFactory.kt`
```kotlin
package com.augmentalis.ava.core.data.db

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

actual class DriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(
            schema = AVADatabase.Schema,
            context = context,
            name = "ava_database.db"
        )
    }
}
```

**File:** `src/iosMain/kotlin/com/augmentalis/ava/core/data/db/DriverFactory.kt`
```kotlin
package com.augmentalis.ava.core.data.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver

actual class DriverFactory {
    actual fun createDriver(): SqlDriver {
        return NativeSqliteDriver(
            schema = AVADatabase.Schema,
            name = "ava_database.db"
        )
    }
}
```

**File:** `src/desktopMain/kotlin/com/augmentalis/ava/core/data/db/DriverFactory.kt`
```kotlin
package com.augmentalis.ava.core.data.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File

actual class DriverFactory {
    actual fun createDriver(): SqlDriver {
        val databasePath = File(System.getProperty("user.home"), ".ava/ava_database.db")
        databasePath.parentFile?.mkdirs()

        return JdbcSqliteDriver("jdbc:sqlite:${databasePath.absolutePath}").also {
            AVADatabase.Schema.create(it)
        }
    }
}
```

**File:** `src/commonMain/kotlin/com/augmentalis/ava/core/data/db/DriverFactory.kt`
```kotlin
package com.augmentalis.ava.core.data.db

import app.cash.sqldelight.db.SqlDriver

expect class DriverFactory {
    fun createDriver(): SqlDriver
}
```

#### Step 1.4: Create Repository Wrappers

**File:** `src/commonMain/kotlin/com/augmentalis/ava/core/data/repository/ConversationRepositoryImpl.kt`
```kotlin
package com.augmentalis.ava.core.data.repository

import com.augmentalis.ava.core.data.db.AVADatabase
import com.augmentalis.ava.core.domain.model.Conversation
import com.augmentalis.ava.core.domain.repository.ConversationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList

class ConversationRepositoryImpl(
    private val database: AVADatabase
) : ConversationRepository {

    private val queries = database.conversationQueries

    override fun getAllConversations(): Flow<List<Conversation>> {
        return queries.getAllConversations()
            .asFlow()
            .mapToList()
            .map { list -> list.map { it.toModel() } }
    }

    override suspend fun getConversationById(id: String): Conversation? {
        return queries.getConversationById(id)
            .executeAsOneOrNull()
            ?.toModel()
    }

    override suspend fun insertConversation(conversation: Conversation) {
        queries.insertConversation(
            id = conversation.id,
            title = conversation.title,
            created_at = conversation.createdAt,
            updated_at = conversation.updatedAt,
            is_archived = if (conversation.isArchived) 1L else 0L,
            metadata = conversation.metadata
        )
    }

    override suspend fun deleteConversation(id: String) {
        queries.deleteConversation(id)
    }

    // Extension to map DB model to domain model
    private fun com.augmentalis.ava.core.data.db.Conversation.toModel(): Conversation {
        return Conversation(
            id = id,
            title = title,
            createdAt = created_at,
            updatedAt = updated_at,
            isArchived = is_archived == 1L,
            metadata = metadata
        )
    }
}
```

#### Step 1.5: Data Migration Strategy

**Option A: Fresh Start (Recommended for Development)**
- Delete old Room database
- Users start with empty database
- Suitable for pre-release apps

**Option B: Runtime Migration**
```kotlin
// Migration helper to copy Room data to SQLDelight
class RoomToSQLDelightMigrator(
    private val context: Context,
    private val sqldelightDb: AVADatabase
) {
    suspend fun migrate() {
        val roomDb = Room.databaseBuilder(
            context,
            LegacyAVADatabase::class.java,
            "ava_database"
        ).build()

        // Copy conversations
        val conversations = roomDb.conversationDao().getAllConversationsOnce()
        conversations.forEach { entity ->
            sqldelightDb.conversationQueries.insertConversation(
                id = entity.id,
                title = entity.title,
                created_at = entity.createdAt,
                updated_at = entity.updatedAt,
                is_archived = if (entity.isArchived) 1L else 0L,
                metadata = entity.metadata
            )
        }

        // Copy messages
        val messages = roomDb.messageDao().getAllMessagesOnce()
        messages.forEach { entity ->
            sqldelightDb.messageQueries.insertMessage(
                id = entity.id,
                conversation_id = entity.conversationId,
                role = entity.role,
                content = entity.content,
                timestamp = entity.timestamp,
                metadata = entity.metadata
            )
        }

        // Copy intent examples
        val examples = roomDb.intentExampleDao().getAllExamplesOnce()
        examples.forEach { entity ->
            sqldelightDb.intentExampleQueries.insertExample(
                example_hash = entity.exampleHash,
                intent_id = entity.intentId,
                example_text = entity.exampleText,
                is_primary = if (entity.isPrimary) 1L else 0L,
                source = entity.source,
                locale = entity.locale,
                created_at = entity.createdAt,
                usage_count = entity.usageCount.toLong(),
                last_used = entity.lastUsed
            )
        }

        // Copy embeddings
        val embeddings = roomDb.intentEmbeddingDao().getAllEmbeddings()
        embeddings.forEach { entity ->
            sqldelightDb.intentEmbeddingQueries.insertEmbedding(
                intent_id = entity.intentId,
                embedding = entity.embedding, // ByteArray stays as BLOB
                model_version = entity.modelVersion,
                created_at = entity.createdAt,
                updated_at = entity.updatedAt
            )
        }

        roomDb.close()

        // Delete old Room database
        context.deleteDatabase("ava_database")
    }
}
```

---

## Phase 2: AVA Features/RAG Migration

### Current Room Implementation

**Location:** `Universal/AVA/Features/RAG/`

**Entities:**
- DocumentEntity
- ChunkEntity
- ChunkEmbeddingEntity

**DAOs:**
- DocumentDao
- ChunkDao
- ChunkEmbeddingDao

### Migration Steps

Similar to Phase 1 but for RAG-specific tables:

**File:** `src/commonMain/sqldelight/com/augmentalis/ava/features/rag/db/Document.sq`
```sql
CREATE TABLE Document (
    id TEXT PRIMARY KEY NOT NULL,
    title TEXT NOT NULL,
    content TEXT NOT NULL,
    source_path TEXT,
    mime_type TEXT NOT NULL,
    file_size INTEGER NOT NULL,
    checksum TEXT NOT NULL,
    created_at INTEGER NOT NULL,
    indexed_at INTEGER
);

-- Additional queries for RAG functionality...
```

---

## Phase 3: VoiceOS LocalizationManager Migration

### Current Room Implementation

**Location:** `/Volumes/M-Drive/Coding/VoiceOS/modules/managers/LocalizationManager/`

### Migration Steps

1. Create SQLDelight schema for localization tables
2. Update LocalizationManager to use SQLDelight
3. Test with existing VoiceOS app

---

## Phase 4: Avanues Android Modules Migration

### Modules to Migrate

1. **voiceos/app** - Main VoiceOS Android app
2. **uuidcreator** - UUID generation with persistent storage

---

## Git Strategy

### Per-Repository Commits

Each repository will have separate commits for:

1. **Setup Commit** - Add SQLDelight dependencies and configuration
2. **Schema Commit** - Add `.sq` files for all tables
3. **Driver Commit** - Add platform-specific drivers
4. **Repository Commit** - Add/update repository implementations
5. **Migration Commit** - Add data migration utilities
6. **Cleanup Commit** - Remove Room dependencies (after verification)
7. **Test Commit** - Add/update unit tests

### Commit Message Format

```
feat(db): migrate [Module] from Room to SQLDelight (Phase X.Y)

- Add SQLDelight schema files for [tables]
- Implement platform drivers (Android/iOS/Desktop)
- Update repository implementations
- Add data migration utility

Breaking Change: Database technology changed
Migration: Automatic runtime migration from Room data

Part of: Room→SQLDelight Cross-Platform Migration
Related: [other repo commits if applicable]
```

---

## Testing Strategy

### Per-Phase Testing

1. **Unit Tests** - Repository functions, queries
2. **Integration Tests** - Full database operations
3. **Migration Tests** - Room → SQLDelight data integrity
4. **Cross-Platform Tests** - Verify same behavior on Android/iOS/Desktop

### Test Coverage Requirements

- All CRUD operations
- Foreign key constraints
- Index performance
- Edge cases (null values, empty strings, special characters)
- Large dataset performance

---

## Rollback Plan

If migration fails:

1. Keep Room database file as backup
2. Maintain Room dependencies in separate branch
3. Flag-based toggle between Room and SQLDelight
4. Automatic rollback on migration error

---

## Timeline Estimate

| Phase | Duration | Dependencies |
|-------|----------|--------------|
| Phase 1 (AVA Core/Data) | 3-5 days | None |
| Phase 2 (AVA RAG) | 2-3 days | Phase 1 |
| Phase 3 (VoiceOS) | 2-3 days | None |
| Phase 4 (Avanues) | 2-3 days | None |
| Integration Testing | 2-3 days | All Phases |
| **Total** | **11-17 days** | |

---

## Risk Assessment

| Risk | Impact | Mitigation |
|------|--------|------------|
| Data loss during migration | HIGH | Backup Room DB, test migration thoroughly |
| Performance regression | MEDIUM | Benchmark before/after, optimize queries |
| Cross-platform inconsistencies | MEDIUM | Shared test suite, CI/CD verification |
| Breaking changes in APIs | HIGH | Deprecate old APIs, provide migration guide |

---

## Success Criteria

- [ ] All Room tables converted to SQLDelight
- [ ] All platforms (Android, iOS, Desktop) build successfully
- [ ] Data migration preserves all existing data
- [ ] No performance regression (within 10%)
- [ ] All unit tests pass
- [ ] All integration tests pass
- [ ] Documentation updated

---

## References

- [SQLDelight Documentation](https://cashapp.github.io/sqldelight/)
- [SQLDelight KMP Setup](https://cashapp.github.io/sqldelight/2.0.0/multiplatform_sqlite/)
- [Room Migration Guide](https://developer.android.com/training/data-storage/room/migrating-db-versions)

---

**Created:** 2025-11-25
**Author:** IDEACODE Refactor Agent
**Version:** 1.0
