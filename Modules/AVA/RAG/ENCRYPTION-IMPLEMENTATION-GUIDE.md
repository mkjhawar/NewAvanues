# RAG Embedding Encryption Implementation Guide

Implementation of AES-256-GCM encryption for RAG stored embeddings with Android Keystore integration.

---

## Overview

| Component | Purpose |
|-----------|---------|
| EmbeddingEncryptionManager | Core encryption service using Android Keystore |
| EncryptedEmbeddingRepository | Wrapper for embedding serialization with encryption |
| EncryptionMigration | Migration utility for existing unencrypted data |
| SQLDelight Schema Updates | Database schema changes for encryption metadata |

---

## Security Features

| Feature | Implementation |
|---------|---------------|
| Algorithm | AES-256-GCM authenticated encryption |
| Key Storage | Android Keystore (hardware-backed when available) |
| IV Generation | 96-bit random IV per encryption |
| Authentication | 128-bit GCM authentication tag |
| Checksum | SHA-256 for document integrity validation |
| Key Rotation | Multi-version key support with migration |

---

## Architecture

### Encryption Flow

```
Embedding (FloatArray)
    ↓
EncryptedEmbeddingRepository.serializeEmbedding()
    ↓
EmbeddingEncryptionManager.encryptEmbedding()
    ↓
[version(1)] + [IV(12)] + [ciphertext + tag]
    ↓
Database (embedding_blob BLOB)
```

### Decryption Flow

```
Database (embedding_blob BLOB)
    ↓
EncryptedEmbeddingRepository.deserializeEmbedding()
    ↓
EmbeddingEncryptionManager.decryptEmbedding()
    ↓
Embedding (FloatArray)
```

---

## Database Schema Changes

### RAGChunk Table

```sql
ALTER TABLE rag_chunk ADD COLUMN is_encrypted INTEGER AS kotlin.Boolean NOT NULL DEFAULT 0;
ALTER TABLE rag_chunk ADD COLUMN encryption_key_version INTEGER DEFAULT 1;
```

### RAGDocument Table

```sql
ALTER TABLE rag_document ADD COLUMN content_checksum TEXT;
```

### New Queries

| Query | Purpose |
|-------|---------|
| selectUnencrypted | Get unencrypted chunks for migration |
| countUnencrypted | Count chunks needing migration |
| updateEncryptionStatus | Update encryption metadata |

---

## Usage

### Basic Encryption

```kotlin
val encryptedRepo = EncryptedEmbeddingRepository(context)

// Serialize embedding with encryption
val embedding = Embedding.Float32(floatArrayOf(1.0f, 2.0f, 3.0f))
val encryptedData = encryptedRepo.serializeEmbedding(embedding)

// Store in database
chunkQueries.insert(
    // ... other params ...
    embedding_blob = encryptedData.blob,
    is_encrypted = encryptedData.isEncrypted,
    encryption_key_version = encryptedData.keyVersion?.toLong()
)

// Deserialize from database
val chunk = chunkQueries.selectById(chunkId).executeAsOne()
val decrypted = encryptedRepo.deserializeEmbedding(chunk)
```

### Document Checksum Validation

```kotlin
val encryptedRepo = EncryptedEmbeddingRepository(context)

// Calculate checksum during document ingestion
val documentContent = File("/path/to/doc.pdf").readBytes()
val checksum = encryptedRepo.calculateChecksum(documentContent)

// Store checksum
documentQueries.insert(
    // ... other params ...
    content_checksum = checksum
)

// Verify on retrieval
val document = documentQueries.selectById(docId).executeAsOne()
val isValid = encryptedRepo.verifyChecksum(documentContent, document.content_checksum)
if (!isValid) {
    // Document has been tampered with
    throw SecurityException("Document checksum validation failed")
}
```

### Migration from Unencrypted

```kotlin
val migration = EncryptionMigration(context)

// Check migration status
val status = migration.getMigrationStatus()
println("Unencrypted chunks: ${status.unencryptedChunks}")
println("Migration required: ${status.requiresMigration}")

// Migrate with progress tracking
lifecycleScope.launch {
    migration.migrateToEncrypted(batchSize = 100)
        .collect { progress ->
            updateUI(
                percentage = progress.percentage,
                processed = progress.processed,
                total = progress.total,
                estimatedRemaining = progress.estimatedRemainingMs
            )
        }
}
```

### Key Rotation

```kotlin
val encryptedRepo = EncryptedEmbeddingRepository(context)
val migration = EncryptionMigration(context)

// Rotate encryption key
val newKeyVersion = encryptedRepo.rotateKey()
println("Rotated to key version: $newKeyVersion")

// Migrate existing encrypted data to new key
lifecycleScope.launch {
    migration.migrateToNewKey(newKeyVersion)
        .collect { progress ->
            println("Re-encryption progress: ${progress.percentage}%")
        }
}
```

### Integration with SQLiteRAGRepository

```kotlin
class SQLiteRAGRepository(
    private val context: Context,
    private val embeddingProvider: EmbeddingProvider,
    // ... other params ...
) : RAGRepository {

    private val encryptedRepo = EncryptedEmbeddingRepository(context)

    // Modified serialization
    private fun serializeEmbedding(embedding: Embedding.Float32): EmbeddingData {
        return encryptedRepo.serializeEmbedding(embedding, encrypt = true)
    }

    // Modified deserialization
    private fun deserializeEmbedding(chunkEntity: Rag_chunk): Embedding.Float32 {
        return encryptedRepo.deserializeEmbedding(chunkEntity)
    }

    // Modified document ingestion with checksum
    override suspend fun addDocument(request: AddDocumentRequest): Result<AddDocumentResult> {
        val file = File(request.filePath)
        val checksum = encryptedRepo.calculateChecksum(file.readBytes())

        documentQueries.insert(
            // ... other params ...
            content_checksum = checksum
        )
        // ... rest of implementation ...
    }
}
```

---

## Performance

### Benchmarks (384-dimension embeddings)

| Operation | Average Time | Impact |
|-----------|--------------|--------|
| Encryption | ~2-5ms | +5-10% overhead |
| Decryption | ~2-5ms | +5-10% overhead |
| Checksum (1MB) | ~10-20ms | One-time per document |

### Optimization Tips

| Tip | Benefit |
|-----|---------|
| Batch migrations | Process 100+ chunks per batch |
| Background processing | Run migration during idle time |
| Cache decrypted embeddings | Reduce repeated decryption |
| Use streaming for large docs | Checksum in chunks |

---

## Error Handling

### Encryption Failures

```kotlin
try {
    val encrypted = encryptedRepo.serializeEmbedding(embedding)
} catch (e: EncryptionException) {
    Timber.e(e, "Encryption failed")
    // Fallback: Store unencrypted with warning
    // Or: Retry with exponential backoff
}
```

### Decryption Failures

```kotlin
try {
    val decrypted = encryptedRepo.deserializeEmbedding(chunk)
} catch (e: DecryptionException) {
    Timber.e(e, "Decryption failed for chunk ${chunk.id}")
    // Possible causes:
    // 1. Corrupted data (checksum validation would catch this)
    // 2. Key rotation in progress
    // 3. Keystore issues

    // Recovery options:
    // - Flag chunk for re-indexing
    // - Attempt with older key version
    // - Remove corrupted chunk
}
```

### Migration Failures

```kotlin
lifecycleScope.launch {
    try {
        migration.migrateToEncrypted()
            .catch { e ->
                Timber.e(e, "Migration failed")
                // Partial migration is safe - can resume later
                showError("Migration paused. Will resume on next startup.")
            }
            .collect { progress ->
                updateProgress(progress)
            }
    } catch (e: Exception) {
        // Fatal error - likely database corruption
        reportCriticalError(e)
    }
}
```

---

## Security Considerations

### Key Management

| Concern | Mitigation |
|---------|-----------|
| Key extraction | Android Keystore hardware-backed |
| Key backup | Keys not included in backups |
| Root compromise | StrongBox if available |
| Key rotation | Periodic rotation recommended |

### Data Integrity

| Mechanism | Purpose |
|-----------|---------|
| GCM authentication tag | Detect tampering of encrypted data |
| SHA-256 checksums | Detect document modifications |
| Version tracking | Support key rotation without data loss |

### Best Practices

| Practice | Rationale |
|----------|-----------|
| Enable encryption by default | Protect user data |
| Rotate keys annually | Limit exposure window |
| Validate checksums on read | Detect corruption early |
| Monitor decryption failures | Detect attacks |
| Use hardware keystore | Maximum security |

---

## Testing

### Unit Tests

```kotlin
@Test
fun testEncryptDecrypt() {
    val original = FloatArray(384) { it.toFloat() }
    val encrypted = encryptionManager.encryptEmbedding(original)
    val decrypted = encryptionManager.decryptEmbedding(encrypted)
    assertArrayEquals(original, decrypted, 0.0001f)
}

@Test
fun testChecksum() {
    val data = "Test document content"
    val checksum = encryptionManager.calculateChecksum(data)
    assertTrue(encryptionManager.verifyChecksum(data, checksum))
}
```

### Integration Tests

```kotlin
@Test
fun testMigration() = runBlocking {
    // Create unencrypted test data
    createTestChunks(count = 100, encrypted = false)

    // Migrate
    migration.migrateToEncrypted().collect()

    // Verify all encrypted
    val status = migration.getMigrationStatus()
    assertEquals(0, status.unencryptedChunks)
}
```

### Performance Tests

```kotlin
@Test
fun testPerformance() {
    val embedding = FloatArray(384) { it.toFloat() }
    val iterations = 1000

    val start = System.currentTimeMillis()
    repeat(iterations) {
        encryptionManager.encryptEmbedding(embedding)
    }
    val avgMs = (System.currentTimeMillis() - start) / iterations.toFloat()

    assertTrue("Too slow: ${avgMs}ms", avgMs < 10f)
}
```

---

## Rollback Plan

### Disable Encryption

```kotlin
// Stop encrypting new data
encryptedRepo.setEncryptionEnabled(false)

// New embeddings will be stored unencrypted
// Existing encrypted data remains encrypted
```

### Full Rollback

```kotlin
// Decrypt all existing data
migration.rollbackToUnencrypted()
    .collect { progress ->
        println("Rollback progress: ${progress.percentage}%")
    }

// Disable encryption
encryptedRepo.setEncryptionEnabled(false)
```

---

## Future Enhancements

| Enhancement | Priority | Complexity |
|-------------|----------|------------|
| Hardware StrongBox support | High | Low |
| Biometric unlock for keys | Medium | Medium |
| Cloud key backup (encrypted) | Low | High |
| Per-document encryption keys | Low | High |
| Quantum-resistant algorithms | Low | High |

---

## Files Created/Modified

### New Files

| File | Purpose |
|------|---------|
| EmbeddingEncryptionManager.kt | Core encryption service |
| EncryptedEmbeddingRepository.kt | Embedding serialization wrapper |
| EncryptionMigration.kt | Migration utility |
| EmbeddingEncryptionTest.kt | Unit/integration tests |
| EncryptionMigrationTest.kt | Migration tests |

### Modified Files

| File | Changes |
|------|---------|
| RAGChunk.sq | Added is_encrypted, encryption_key_version |
| RAGDocument.sq | Added content_checksum |

---

## References

| Resource | Link |
|----------|------|
| Android Keystore | https://developer.android.com/training/articles/keystore |
| AES-GCM Specification | NIST SP 800-38D |
| SHA-256 Specification | FIPS 180-4 |
| Security Best Practices | Android Security Guidelines |

---

**Author:** AVA AI Team
**Created:** 2025-12-05
**Version:** 1.0
