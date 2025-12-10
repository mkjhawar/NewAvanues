# AVA RAG: Room to SQLDelight Migration Guide

**Created:** 2025-11-28
**Module:** Universal/AVA/Features/RAG
**Status:** Implementation Guide
**Complexity:** HIGH (Quantized embeddings, FTS, caching)
**Estimated Time:** 2 days

---

## Overview

This guide provides step-by-step instructions for migrating the AVA RAG (Retrieval-Augmented Generation) module from Room ORM to SQLDelight.

**Critical Requirements:**
- ✅ Preserve quantized embedding storage (75% space savings)
- ✅ Preserve FTS (Full-Text Search) functionality
- ✅ Preserve query caching (30-50% performance improvement)
- ✅ Zero data loss during migration
- ✅ Maintain search latency ≤ 100ms

---

## Current Room Implementation

### Database Structure

**Location:** `Universal/AVA/Features/RAG/src/androidMain/kotlin/com/augmentalis/ava/features/rag/data/room/`

#### Entities

```kotlin
// File: Entities.kt

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "type") val documentType: String,
    @ColumnInfo(name = "file_path") val filePath: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "metadata") val metadata: Map<String, String> = emptyMap()
)

@Entity(tableName = "chunks")
data class ChunkEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "document_id") val documentId: String,
    @ColumnInfo(name = "content") val content: String,
    @ColumnInfo(name = "page_number") val pageNumber: Int?,
    @ColumnInfo(name = "start_offset") val startOffset: Int,
    @ColumnInfo(name = "end_offset") val endOffset: Int,
    @ColumnInfo(name = "token_count") val tokenCount: Int,
    @ColumnInfo(name = "metadata") val metadata: Map<String, String> = emptyMap()
)

@Entity(
    tableName = "chunk_embeddings",
    foreignKeys = [ForeignKey(
        entity = ChunkEntity::class,
        parentColumns = ["id"],
        childColumns = ["chunk_id"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class ChunkEmbeddingEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "chunk_id") val chunkId: String,
    @ColumnInfo(name = "embedding_data", typeAffinity = ColumnInfo.BLOB)
    val embeddingData: ByteArray,  // Quantized INT8 embedding
    @ColumnInfo(name = "scale") val scale: Float,
    @ColumnInfo(name = "offset") val offset: Float,
    @ColumnInfo(name = "dimension") val dimension: Int,
    @ColumnInfo(name = "model_id") val modelId: String,
    @ColumnInfo(name = "created_at") val createdAt: Long
)

@Entity(tableName = "document_metadata")
data class DocumentMetadataEntity(
    @PrimaryKey val documentId: String,
    @ColumnInfo(name = "author") val author: String?,
    @ColumnInfo(name = "created_date") val createdDate: String?,
    @ColumnInfo(name = "language") val language: String?,
    @ColumnInfo(name = "keywords") val keywords: String?
)
```

#### DAOs

```kotlin
// File: Daos.kt

@Dao
interface DocumentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(document: DocumentEntity)

    @Query("SELECT * FROM documents WHERE id = :id")
    fun getById(id: String): Flow<DocumentEntity?>

    @Query("SELECT * FROM documents ORDER BY created_at DESC")
    fun getAll(): Flow<List<DocumentEntity>>

    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface ChunkDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(chunk: ChunkEntity)

    @Query("SELECT * FROM chunks WHERE document_id = :documentId")
    fun getChunksByDocumentId(documentId: String): Flow<List<ChunkEntity>>

    @Query("SELECT * FROM chunks WHERE id = :id")
    suspend fun getById(id: String): ChunkEntity?
}

@Dao
interface ChunkEmbeddingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(embedding: ChunkEmbeddingEntity)

    @Query("""
        SELECT * FROM chunk_embeddings
        WHERE chunk_id IN (:chunkIds)
    """)
    suspend fun getByChunkIds(chunkIds: List<String>): List<ChunkEmbeddingEntity>

    @Query("SELECT * FROM chunk_embeddings")
    suspend fun getAll(): List<ChunkEmbeddingEntity>
}
```

#### Type Converters

```kotlin
// File: EmbeddingConversions.kt

class TypeConverters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromStringMap(value: Map<String, String>): String {
        return json.encodeToString(value)
    }

    @TypeConverter
    fun toStringMap(value: String): Map<String, String> {
        return json.decodeFromString(value)
    }
}
```

---

## SQLDelight Migration

### Step 1: Setup SQLDelight Plugin

#### 1.1 Update build.gradle.kts

```kotlin
// File: Universal/AVA/Features/RAG/build.gradle.kts

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    id("app.cash.sqldelight") version "2.0.1"
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    // Add iOS targets
    listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach {
        it.binaries.framework {
            baseName = "RAG"
            isStatic = true
        }
    }

    // Add Desktop target
    jvm("desktop") {
        compilations.all {
            kotlinOptions.jvmTarget = "17"
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":Universal:AVA:Core:Domain"))
                implementation(project(":Universal:AVA:Core:Common"))

                // SQLDelight
                implementation("app.cash.sqldelight:runtime:2.0.1")
                implementation("app.cash.sqldelight:coroutines-extensions:2.0.1")

                // Serialization
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
            }
        }

        androidMain {
            dependencies {
                // Android SQLDelight driver
                implementation("app.cash.sqldelight:android-driver:2.0.1")

                // ONNX Runtime for embeddings
                implementation("ai.onnxruntime:onnxruntime-android:1.16.3")
            }
        }

        iosMain {
            dependencies {
                implementation("app.cash.sqldelight:native-driver:2.0.1")
            }
        }

        val desktopMain by getting {
            dependencies {
                implementation("app.cash.sqldelight:sqlite-driver:2.0.1")
            }
        }
    }
}

sqldelight {
    databases {
        create("RAGDatabase") {
            packageName.set("com.augmentalis.ava.features.rag.data.db")
            dialect("app.cash.sqldelight:sqlite-3-38-dialect:2.0.1")
            schemaOutputDirectory.set(file("src/commonMain/sqldelight/databases"))
            deriveSchemaFromMigrations.set(true)
        }
    }
}
```

---

### Step 2: Create SQLDelight Schema Files

#### 2.1 Document.sq

```sql
-- File: src/commonMain/sqldelight/com/augmentalis/ava/features/rag/data/db/Document.sq

CREATE TABLE document (
    id TEXT PRIMARY KEY NOT NULL,
    title TEXT NOT NULL,
    document_type TEXT NOT NULL,
    file_path TEXT NOT NULL,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    metadata TEXT NOT NULL DEFAULT '{}'
);

-- Indexes for performance
CREATE INDEX idx_document_created_at ON document(created_at DESC);
CREATE INDEX idx_document_type ON document(document_type);

-- Queries
insert:
INSERT OR REPLACE INTO document (id, title, document_type, file_path, created_at, updated_at, metadata)
VALUES (?, ?, ?, ?, ?, ?, ?);

selectById:
SELECT * FROM document WHERE id = ?;

selectAll:
SELECT * FROM document ORDER BY created_at DESC;

selectByType:
SELECT * FROM document WHERE document_type = ? ORDER BY created_at DESC;

deleteById:
DELETE FROM document WHERE id = ?;

count:
SELECT COUNT(*) FROM document;
```

#### 2.2 Chunk.sq

```sql
-- File: src/commonMain/sqldelight/com/augmentalis/ava/features/rag/data/db/Chunk.sq

CREATE TABLE chunk (
    id TEXT PRIMARY KEY NOT NULL,
    document_id TEXT NOT NULL,
    content TEXT NOT NULL,
    page_number INTEGER,
    start_offset INTEGER NOT NULL,
    end_offset INTEGER NOT NULL,
    token_count INTEGER NOT NULL,
    metadata TEXT NOT NULL DEFAULT '{}',
    FOREIGN KEY (document_id) REFERENCES document(id) ON DELETE CASCADE
);

-- Indexes
CREATE INDEX idx_chunk_document_id ON chunk(document_id);
CREATE INDEX idx_chunk_page_number ON chunk(page_number);

-- FTS (Full-Text Search) table for semantic search
CREATE VIRTUAL TABLE chunk_fts USING fts4(
    content TEXT,
    tokenize=unicode61
);

-- Queries
insert:
INSERT OR REPLACE INTO chunk (id, document_id, content, page_number, start_offset, end_offset, token_count, metadata)
VALUES (?, ?, ?, ?, ?, ?, ?, ?);

insertFts:
INSERT INTO chunk_fts (docid, content) VALUES (?, ?);

selectById:
SELECT * FROM chunk WHERE id = ?;

selectByDocumentId:
SELECT * FROM chunk WHERE document_id = ? ORDER BY start_offset;

selectByDocumentIdAndPage:
SELECT * FROM chunk WHERE document_id = ? AND page_number = ?;

-- Full-text search
searchFts:
SELECT c.*
FROM chunk c
JOIN chunk_fts fts ON c.rowid = fts.docid
WHERE chunk_fts MATCH ?
ORDER BY rank;

deleteById:
DELETE FROM chunk WHERE id = ?;

deleteByDocumentId:
DELETE FROM chunk WHERE document_id = ?;

deleteFtsByRowId:
DELETE FROM chunk_fts WHERE docid = ?;
```

#### 2.3 ChunkEmbedding.sq

```sql
-- File: src/commonMain/sqldelight/com/augmentalis/ava/features/rag/data/db/ChunkEmbedding.sq

CREATE TABLE chunk_embedding (
    id TEXT PRIMARY KEY NOT NULL,
    chunk_id TEXT NOT NULL,
    embedding_data BLOB NOT NULL,  -- Quantized INT8 embedding (ByteArray)
    scale REAL NOT NULL,            -- Quantization scale factor
    offset REAL NOT NULL,           -- Quantization offset
    dimension INTEGER NOT NULL,     -- Embedding dimension (384 or 768)
    model_id TEXT NOT NULL,         -- Model used for embedding
    created_at INTEGER NOT NULL,
    FOREIGN KEY (chunk_id) REFERENCES chunk(id) ON DELETE CASCADE
);

-- Indexes for performance
CREATE INDEX idx_chunk_embedding_chunk_id ON chunk_embedding(chunk_id);
CREATE INDEX idx_chunk_embedding_model_id ON chunk_embedding(model_id);

-- Queries
insert:
INSERT OR REPLACE INTO chunk_embedding (id, chunk_id, embedding_data, scale, offset, dimension, model_id, created_at)
VALUES (?, ?, ?, ?, ?, ?, ?, ?);

selectByChunkId:
SELECT * FROM chunk_embedding WHERE chunk_id = ?;

selectByChunkIds:
SELECT * FROM chunk_embedding WHERE chunk_id IN ?;

selectAll:
SELECT * FROM chunk_embedding;

selectByModelId:
SELECT * FROM chunk_embedding WHERE model_id = ?;

deleteByChunkId:
DELETE FROM chunk_embedding WHERE chunk_id = ?;

count:
SELECT COUNT(*) FROM chunk_embedding;
```

#### 2.4 DocumentMetadata.sq

```sql
-- File: src/commonMain/sqldelight/com/augmentalis/ava/features/rag/data/db/DocumentMetadata.sq

CREATE TABLE document_metadata (
    document_id TEXT PRIMARY KEY NOT NULL,
    author TEXT,
    created_date TEXT,
    language TEXT,
    keywords TEXT,
    FOREIGN KEY (document_id) REFERENCES document(id) ON DELETE CASCADE
);

-- Queries
insert:
INSERT OR REPLACE INTO document_metadata (document_id, author, created_date, language, keywords)
VALUES (?, ?, ?, ?, ?);

selectByDocumentId:
SELECT * FROM document_metadata WHERE document_id = ?;

deleteByDocumentId:
DELETE FROM document_metadata WHERE document_id = ?;
```

---

### Step 3: Create Platform Drivers

#### 3.1 Common Interface

```kotlin
// File: src/commonMain/kotlin/com/augmentalis/ava/features/rag/data/db/DatabaseDriverFactory.kt

package com.augmentalis.ava.features.rag.data.db

import app.cash.sqldelight.db.SqlDriver

expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}
```

#### 3.2 Android Driver

```kotlin
// File: src/androidMain/kotlin/com/augmentalis/ava/features/rag/data/db/DatabaseDriverFactory.android.kt

package com.augmentalis.ava.features.rag.data.db

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

actual class DatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(
            schema = RAGDatabase.Schema,
            context = context,
            name = "rag.db",
            callback = object : AndroidSqliteDriver.Callback(RAGDatabase.Schema) {
                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    // Enable foreign keys
                    db.execSQL("PRAGMA foreign_keys = ON")
                }
            }
        )
    }
}
```

#### 3.3 iOS Driver

```kotlin
// File: src/iosMain/kotlin/com/augmentalis/ava/features/rag/data/db/DatabaseDriverFactory.ios.kt

package com.augmentalis.ava.features.rag.data.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        return NativeSqliteDriver(
            schema = RAGDatabase.Schema,
            name = "rag.db",
            onConfiguration = { config ->
                config.copy(
                    extendedConfig = config.extendedConfig.copy(
                        foreignKeyConstraints = true
                    )
                )
            }
        )
    }
}
```

#### 3.4 Desktop Driver

```kotlin
// File: src/desktopMain/kotlin/com/augmentalis/ava/features/rag/data/db/DatabaseDriverFactory.desktop.kt

package com.augmentalis.ava.features.rag.data.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        val databasePath = File(System.getProperty("user.home"), ".ava/rag.db")
        databasePath.parentFile?.mkdirs()

        val driver = JdbcSqliteDriver("jdbc:sqlite:${databasePath.absolutePath}")
        RAGDatabase.Schema.create(driver)

        // Enable foreign keys
        driver.execute(null, "PRAGMA foreign_keys = ON", 0)

        return driver
    }
}
```

---

### Step 4: Migrate Repository Implementation

#### 4.1 SQLiteRAGRepository (SQLDelight Version)

```kotlin
// File: src/commonMain/kotlin/com/augmentalis/ava/features/rag/data/SQLiteRAGRepository.kt

package com.augmentalis.ava.features.rag.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.augmentalis.ava.features.rag.data.db.RAGDatabase
import com.augmentalis.ava.features.rag.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

class SQLiteRAGRepository(
    private val database: RAGDatabase
) : RAGRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun addDocument(document: Document): Result<String> {
        return try {
            // Insert document
            database.documentQueries.insert(
                id = document.id,
                title = document.title,
                document_type = document.type.name,
                file_path = document.filePath,
                created_at = document.createdAt,
                updated_at = document.updatedAt,
                metadata = json.encodeToString(document.metadata)
            )

            // Insert metadata if present
            if (document.metadata.isNotEmpty()) {
                database.documentMetadataQueries.insert(
                    document_id = document.id,
                    author = document.metadata["author"],
                    created_date = document.metadata["created_date"],
                    language = document.metadata["language"],
                    keywords = document.metadata["keywords"]
                )
            }

            Result.success(document.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addChunk(
        documentId: String,
        chunk: Chunk,
        embedding: QuantizedEmbedding
    ): Result<String> {
        return try {
            // Insert chunk
            database.chunkQueries.insert(
                id = chunk.id,
                document_id = documentId,
                content = chunk.content,
                page_number = chunk.metadata.pageNumber?.toLong(),
                start_offset = chunk.startOffset.toLong(),
                end_offset = chunk.endOffset.toLong(),
                token_count = chunk.tokenCount.toLong(),
                metadata = json.encodeToString(chunk.metadata.toMap())
            )

            // Insert FTS entry
            database.chunkQueries.insertFts(
                docid = chunk.id.hashCode().toLong(),  // Use hash as rowid
                content = chunk.content
            )

            // Insert embedding
            database.chunkEmbeddingQueries.insert(
                id = "${chunk.id}_emb",
                chunk_id = chunk.id,
                embedding_data = embedding.data,  // ByteArray (quantized INT8)
                scale = embedding.scale,
                offset = embedding.offset,
                dimension = embedding.dimension.toLong(),
                model_id = "AVA-384-Base-INT8",  // Or dynamic
                created_at = System.currentTimeMillis()
            )

            Result.success(chunk.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun search(query: SearchQuery): Result<SearchResults> {
        return try {
            // Get query embedding
            val queryEmbedding = embeddingProvider.generateEmbedding(query.query)
            val quantizedQuery = Quantization.quantizeToInt8(queryEmbedding)

            // Get all chunk embeddings
            val allEmbeddings = database.chunkEmbeddingQueries.selectAll()
                .executeAsList()

            // Calculate cosine similarity for each
            val results = allEmbeddings.map { embEntity ->
                val chunkEmbedding = QuantizedEmbedding(
                    data = embEntity.embedding_data,
                    scale = embEntity.scale.toFloat(),
                    offset = embEntity.offset.toFloat(),
                    dimension = embEntity.dimension.toInt()
                )

                val similarity = Quantization.cosineSimilarityQuantized(
                    quantizedQuery,
                    chunkEmbedding
                )

                embEntity.chunk_id to similarity
            }
            .filter { it.second >= query.minSimilarity }
            .sortedByDescending { it.second }
            .take(query.maxResults)

            // Fetch chunk details
            val searchResults = results.map { (chunkId, similarity) ->
                val chunkEntity = database.chunkQueries.selectById(chunkId).executeAsOne()
                val documentEntity = database.documentQueries.selectById(chunkEntity.document_id).executeAsOne()

                SearchResult(
                    chunk = Chunk(
                        id = chunkEntity.id,
                        content = chunkEntity.content,
                        startOffset = chunkEntity.start_offset.toInt(),
                        endOffset = chunkEntity.end_offset.toInt(),
                        tokenCount = chunkEntity.token_count.toInt(),
                        metadata = ChunkMetadata(
                            pageNumber = chunkEntity.page_number?.toInt()
                        )
                    ),
                    document = Document(
                        id = documentEntity.id,
                        title = documentEntity.title,
                        type = DocumentType.valueOf(documentEntity.document_type),
                        filePath = documentEntity.file_path,
                        createdAt = documentEntity.created_at,
                        updatedAt = documentEntity.updated_at,
                        metadata = json.decodeFromString(documentEntity.metadata)
                    ),
                    similarity = similarity
                )
            }

            Result.success(SearchResults(results = searchResults))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getAllDocuments(): Flow<List<Document>> {
        return database.documentQueries.selectAll()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { entities ->
                entities.map { entity ->
                    Document(
                        id = entity.id,
                        title = entity.title,
                        type = DocumentType.valueOf(entity.document_type),
                        filePath = entity.file_path,
                        createdAt = entity.created_at,
                        updatedAt = entity.updated_at,
                        metadata = json.decodeFromString(entity.metadata)
                    )
                }
            }
    }

    override suspend fun deleteDocument(documentId: String): Result<Unit> {
        return try {
            // Get all chunks for this document
            val chunks = database.chunkQueries.selectByDocumentId(documentId).executeAsList()

            // Delete FTS entries
            chunks.forEach { chunk ->
                database.chunkQueries.deleteFtsByRowId(chunk.id.hashCode().toLong())
            }

            // Delete document (cascades to chunks and embeddings)
            database.documentQueries.deleteById(documentId)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

---

### Step 5: Data Migration Script

#### 5.1 Room to SQLDelight Migration

```kotlin
// File: src/androidMain/kotlin/com/augmentalis/ava/features/rag/migration/RoomToSQLDelightMigration.kt

package com.augmentalis.ava.features.rag.migration

import android.content.Context
import com.augmentalis.ava.features.rag.data.db.DatabaseDriverFactory
import com.augmentalis.ava.features.rag.data.db.RAGDatabase
import com.augmentalis.ava.features.rag.data.room.RAGDatabase as RoomDatabase

/**
 * Migrates data from Room to SQLDelight
 *
 * Run this once on app update to migrate existing user data
 */
class RoomToSQLDelightMigration(private val context: Context) {

    suspend fun migrate(): Result<MigrationStats> {
        return try {
            val stats = MigrationStats()

            // Get Room database
            val roomDb = Room.databaseBuilder(
                context,
                RoomDatabase::class.java,
                "rag_room.db"
            ).build()

            // Get SQLDelight database
            val driverFactory = DatabaseDriverFactory(context)
            val sqlDelightDb = RAGDatabase(driverFactory.createDriver())

            // Migrate documents
            val documents = roomDb.documentDao().getAll().first()
            documents.forEach { doc ->
                sqlDelightDb.documentQueries.insert(
                    id = doc.id,
                    title = doc.title,
                    document_type = doc.documentType,
                    file_path = doc.filePath,
                    created_at = doc.createdAt,
                    updated_at = doc.updatedAt,
                    metadata = Json.encodeToString(doc.metadata)
                )
                stats.documentsM migrated++
            }

            // Migrate chunks
            val chunks = roomDb.chunkDao().getAllChunks()
            chunks.forEach { chunk ->
                sqlDelightDb.chunkQueries.insert(
                    id = chunk.id,
                    document_id = chunk.documentId,
                    content = chunk.content,
                    page_number = chunk.pageNumber?.toLong(),
                    start_offset = chunk.startOffset.toLong(),
                    end_offset = chunk.endOffset.toLong(),
                    token_count = chunk.tokenCount.toLong(),
                    metadata = Json.encodeToString(chunk.metadata)
                )

                // Insert FTS
                sqlDelightDb.chunkQueries.insertFts(
                    docid = chunk.id.hashCode().toLong(),
                    content = chunk.content
                )

                stats.chunksMigrated++
            }

            // Migrate embeddings
            val embeddings = roomDb.chunkEmbeddingDao().getAll()
            embeddings.forEach { emb ->
                sqlDelightDb.chunkEmbeddingQueries.insert(
                    id = emb.id,
                    chunk_id = emb.chunkId,
                    embedding_data = emb.embeddingData,
                    scale = emb.scale,
                    offset = emb.offset,
                    dimension = emb.dimension.toLong(),
                    model_id = emb.modelId,
                    created_at = emb.createdAt
                )
                stats.embeddingsMigrated++
            }

            // Close Room database
            roomDb.close()

            // Rename Room database file (backup)
            val roomDbFile = context.getDatabasePath("rag_room.db")
            val backupFile = context.getDatabasePath("rag_room.db.backup")
            roomDbFile.renameTo(backupFile)

            Result.success(stats)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class MigrationStats(
    var documentsMigrated: Int = 0,
    var chunksMigrated: Int = 0,
    var embeddingsMigrated: Int = 0
)
```

---

### Step 6: Performance Verification

#### 6.1 Benchmark Tests

```kotlin
// File: src/androidTest/kotlin/com/augmentalis/ava/features/rag/SQLDelightPerformanceTest.kt

@RunWith(AndroidJUnit4::class)
class SQLDelightPerformanceTest {

    @Test
    fun `verify search latency under 100ms`() = runBlocking {
        // Given: 1000 chunks with embeddings
        val repository = createRepositoryWithData(chunkCount = 1000)

        // When: Search query
        val startTime = System.currentTimeMillis()
        val results = repository.search(SearchQuery(
            query = "How to use SQLDelight?",
            maxResults = 10
        ))
        val latency = System.currentTimeMillis() - startTime

        // Then: Latency must be under 100ms
        assertTrue("Search latency ${latency}ms exceeds 100ms", latency < 100)
        assertTrue(results.isSuccess)
    }

    @Test
    fun `verify quantized embedding storage saves 75 percent space`() {
        // Given: Original float embedding
        val floatEmbedding = FloatArray(384) { Random.nextFloat() }
        val floatSize = floatEmbedding.size * 4  // 4 bytes per float

        // When: Quantize to INT8
        val quantized = Quantization.quantizeToInt8(floatEmbedding)
        val quantizedSize = quantized.data.size + 8  // ByteArray + scale + offset

        // Then: Should be ~75% reduction
        val reduction = ((floatSize - quantizedSize).toFloat() / floatSize) * 100
        assertTrue("Reduction ${reduction}% is less than 70%", reduction > 70)
    }
}
```

---

## Migration Checklist

### Pre-Migration

- [ ] Backup production database
- [ ] Run performance benchmarks (record baseline)
- [ ] Test migration script on development data
- [ ] Review all Room queries for SQLDelight equivalents
- [ ] Prepare rollback plan

### Migration Day

- [ ] Feature freeze (stop all commits to RAG module)
- [ ] Create migration branch
- [ ] Add SQLDelight dependencies
- [ ] Create .sq schema files
- [ ] Create platform drivers
- [ ] Migrate repository implementation
- [ ] Run data migration script
- [ ] Run all unit tests (must pass 100%)
- [ ] Run performance benchmarks (verify no regression)

### Post-Migration

- [ ] Verify search latency ≤ 100ms
- [ ] Verify quantization still saves 75% space
- [ ] Verify FTS still works
- [ ] Verify all UI features work
- [ ] Beta test with select users
- [ ] Monitor error logs for 48 hours
- [ ] Merge to main if stable
- [ ] Delete Room code after 1 week of stability

---

## Rollback Plan

If critical issues occur:

1. Stop deployment immediately
2. Restore Room database from backup
3. Revert to previous app version
4. Analyze failure logs
5. Fix issues in migration branch
6. Re-test thoroughly
7. Retry migration when stable

---

## Success Metrics

- ✅ Zero data loss (100% documents/chunks/embeddings migrated)
- ✅ Search latency ≤ 100ms (same as Room)
- ✅ Quantization saves 75% space (same as Room)
- ✅ FTS search works correctly
- ✅ All unit tests pass
- ✅ All integration tests pass
- ✅ Zero user-reported bugs for 48 hours

---

**Timeline:** 2 days
**Risk:** MEDIUM (mitigated by VoiceOS success, thorough testing)
**Confidence:** HIGH (well-planned, proven migration path)

**See Also:**
- `AVACONNECT-SQLDELIGHT-WORKFLOW-GENERATION.md` - Use SQLDelight in generated apps
- `AVA-SQLDELIGHT-MIGRATION-PRIORITY.md` - Why migrate before Q2
- `VoiceOS/docs/planning/architecture/decisions/ADR-010-Room-SQLDelight-Migration-Completion-251128-0349.md` - VoiceOS success story
