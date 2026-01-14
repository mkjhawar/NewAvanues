// filename: Universal/AVA/Features/RAG/src/androidTest/kotlin/com/augmentalis/ava/features/rag/security/EncryptionMigrationTest.kt
// created: 2025-12-05
// author: AVA AI Team
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.rag.security

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.augmentalis.ava.core.data.db.AVADatabase
import com.augmentalis.ava.core.data.db.DatabaseDriverFactory
import com.augmentalis.ava.core.data.db.createDatabase
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.nio.ByteBuffer
import com.augmentalis.ava.core.data.util.AvidHelper

/**
 * Integration tests for encryption migration
 *
 * Tests:
 * - Migration from unencrypted to encrypted
 * - Key rotation migration
 * - Rollback to unencrypted
 * - Progress tracking
 * - Error recovery
 */
@RunWith(AndroidJUnit4::class)
class EncryptionMigrationTest {

    private lateinit var context: Context
    private lateinit var migration: EncryptionMigration
    private lateinit var database: AVADatabase
    private lateinit var encryptedRepo: EncryptedEmbeddingRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        migration = EncryptionMigration(context)
        database = DatabaseDriverFactory(context).createDriver().createDatabase()
        encryptedRepo = EncryptedEmbeddingRepository(context)

        // Clean database before tests
        database.rAGChunkQueries.deleteAll()
        database.rAGDocumentQueries.deleteAll()
    }

    @After
    fun cleanup() {
        // Clean up test data
        database.rAGChunkQueries.deleteAll()
        database.rAGDocumentQueries.deleteAll()
    }

    @Test
    fun testMigrationStatus_Empty() = runBlocking {
        val status = migration.getMigrationStatus()

        assertEquals(0, status.totalChunks)
        assertEquals(0, status.encryptedChunks)
        assertEquals(0, status.unencryptedChunks)
        assertFalse(status.requiresMigration)
        assertEquals(100, status.percentComplete)
    }

    @Test
    fun testMigrationStatus_WithUnencryptedChunks() = runBlocking {
        // Add test document
        val docId = createTestDocument()

        // Add unencrypted chunks
        createTestChunks(docId, count = 10, encrypted = false)

        val status = migration.getMigrationStatus()

        assertEquals(10, status.totalChunks)
        assertEquals(0, status.encryptedChunks)
        assertEquals(10, status.unencryptedChunks)
        assertTrue(status.requiresMigration)
        assertEquals(0, status.percentComplete)
    }

    @Test
    fun testMigrationStatus_PartiallyEncrypted() = runBlocking {
        val docId = createTestDocument()

        // Add mix of encrypted and unencrypted chunks
        createTestChunks(docId, count = 5, encrypted = false)
        createTestChunks(docId, count = 5, encrypted = true)

        val status = migration.getMigrationStatus()

        assertEquals(10, status.totalChunks)
        assertEquals(5, status.encryptedChunks)
        assertEquals(5, status.unencryptedChunks)
        assertTrue(status.requiresMigration)
        assertEquals(50, status.percentComplete)
    }

    @Test
    fun testMigration_SmallBatch() = runBlocking {
        val docId = createTestDocument()
        createTestChunks(docId, count = 10, encrypted = false)

        // Migrate in small batches
        val progressUpdates = migration.migrateToEncrypted(batchSize = 3).toList()

        // Should have multiple progress updates
        assertTrue(progressUpdates.size > 1)

        // Check final progress
        val finalProgress = progressUpdates.last()
        assertEquals(10, finalProgress.processed)
        assertEquals(10, finalProgress.total)
        assertEquals(0, finalProgress.failed)
        assertEquals(100, finalProgress.percentage)

        // Verify all chunks are now encrypted
        val postStatus = migration.getMigrationStatus()
        assertEquals(10, postStatus.encryptedChunks)
        assertEquals(0, postStatus.unencryptedChunks)
        assertFalse(postStatus.requiresMigration)
    }

    @Test
    fun testMigration_LargeBatch() = runBlocking {
        val docId = createTestDocument()
        createTestChunks(docId, count = 50, encrypted = false)

        val progressUpdates = migration.migrateToEncrypted(batchSize = 100).toList()

        // Check final result
        val finalProgress = progressUpdates.last()
        assertEquals(50, finalProgress.processed)
        assertEquals(0, finalProgress.failed)

        val postStatus = migration.getMigrationStatus()
        assertEquals(50, postStatus.encryptedChunks)
        assertEquals(0, postStatus.unencryptedChunks)
    }

    @Test
    fun testMigration_ProgressTracking() = runBlocking {
        val docId = createTestDocument()
        createTestChunks(docId, count = 20, encrypted = false)

        val progressUpdates = mutableListOf<MigrationProgress>()
        migration.migrateToEncrypted(batchSize = 5).collect { progress ->
            progressUpdates.add(progress)
        }

        // Verify progress increases monotonically
        for (i in 1 until progressUpdates.size) {
            assertTrue(
                progressUpdates[i].processed >= progressUpdates[i - 1].processed
            )
            assertTrue(
                progressUpdates[i].percentage >= progressUpdates[i - 1].percentage
            )
        }

        // Verify time estimates
        progressUpdates.forEach { progress ->
            assertTrue(progress.elapsedMs >= 0)
            assertTrue(progress.estimatedRemainingMs >= 0)
        }
    }

    @Test
    fun testMigration_AlreadyMigrated() = runBlocking {
        val docId = createTestDocument()
        createTestChunks(docId, count = 10, encrypted = true)

        // Try to migrate already encrypted data
        val progressUpdates = migration.migrateToEncrypted().toList()

        // Should complete immediately with no work
        val finalProgress = progressUpdates.last()
        assertEquals(0, finalProgress.processed)
        assertEquals(0, finalProgress.total)
    }

    @Test
    fun testMigration_DataIntegrity() = runBlocking {
        val docId = createTestDocument()

        // Create chunks with known embeddings
        val testEmbeddings = (0 until 5).map { idx ->
            FloatArray(384) { it.toFloat() + (idx * 1000) }
        }

        testEmbeddings.forEachIndexed { idx, embedding ->
            createTestChunk(docId, idx, embedding, encrypted = false)
        }

        // Migrate
        migration.migrateToEncrypted().toList()

        // Verify data integrity after migration
        val chunks = database.rAGChunkQueries.selectByDocument(docId).executeAsList()
        assertEquals(5, chunks.size)

        chunks.forEachIndexed { idx, chunk ->
            assertTrue(chunk.is_encrypted == true)
            assertNotNull(chunk.encryption_key_version)

            // Decrypt and verify values match original
            val decrypted = encryptedRepo.deserializeEmbedding(chunk)
            assertArrayEquals(testEmbeddings[idx], decrypted.values, 0.0001f)
        }
    }

    @Test
    fun testKeyRotationMigration() = runBlocking {
        val docId = createTestDocument()
        createTestChunks(docId, count = 10, encrypted = true)

        val originalKeyVersion = encryptedRepo.getEncryptionStats().currentKeyVersion

        // Rotate key
        val newKeyVersion = encryptedRepo.rotateKey()

        // Migrate to new key
        val progressUpdates = migration.migrateToNewKey(newKeyVersion).toList()

        val finalProgress = progressUpdates.last()
        assertEquals(10, finalProgress.processed)
        assertEquals(0, finalProgress.failed)

        // Verify all chunks use new key version
        val chunks = database.rAGChunkQueries.selectAll().executeAsList()
        chunks.forEach { chunk ->
            assertEquals(newKeyVersion.toLong(), chunk.encryption_key_version)
        }
    }

    @Test
    fun testRollbackToUnencrypted() = runBlocking {
        val docId = createTestDocument()
        createTestChunks(docId, count = 10, encrypted = true)

        // Rollback
        val progressUpdates = migration.rollbackToUnencrypted().toList()

        val finalProgress = progressUpdates.last()
        assertEquals(10, finalProgress.processed)
        assertEquals(0, finalProgress.failed)

        // Verify all chunks are now unencrypted
        val status = migration.getMigrationStatus()
        assertEquals(0, status.encryptedChunks)
        assertEquals(10, status.unencryptedChunks)
    }

    @Test
    fun testRollback_DataIntegrity() = runBlocking {
        val docId = createTestDocument()

        // Create encrypted chunks with known data
        val testEmbeddings = (0 until 5).map { idx ->
            FloatArray(384) { it.toFloat() + (idx * 1000) }
        }

        testEmbeddings.forEachIndexed { idx, embedding ->
            createTestChunk(docId, idx, embedding, encrypted = true)
        }

        // Rollback
        migration.rollbackToUnencrypted().toList()

        // Verify data integrity
        val chunks = database.rAGChunkQueries.selectByDocument(docId).executeAsList()
        chunks.forEachIndexed { idx, chunk ->
            assertFalse(chunk.is_encrypted == true)

            // Deserialize and verify
            val deserialized = encryptedRepo.deserializeEmbedding(chunk)
            assertArrayEquals(testEmbeddings[idx], deserialized.values, 0.0001f)
        }
    }

    // Helper methods

    private fun createTestDocument(): String {
        val docId = VuidHelper.randomVUID()
        database.rAGDocumentQueries.insert(
            id = docId,
            title = "Test Document",
            file_path = "/test/path.pdf",
            document_type = "PDF",
            total_pages = 1,
            size_bytes = 1024,
            added_timestamp = "2025-12-05T00:00:00Z",
            last_accessed_timestamp = null,
            metadata_json = "{}",
            content_checksum = null
        )
        return docId
    }

    private fun createTestChunks(
        documentId: String,
        count: Int,
        encrypted: Boolean
    ) {
        repeat(count) { idx ->
            val embedding = FloatArray(384) { it.toFloat() }
            createTestChunk(documentId, idx, embedding, encrypted)
        }
    }

    private fun createTestChunk(
        documentId: String,
        index: Int,
        embedding: FloatArray,
        encrypted: Boolean
    ) {
        val embeddingBlob = if (encrypted) {
            val mgr = EmbeddingEncryptionManager(context)
            mgr.encryptEmbedding(embedding)
        } else {
            val buffer = ByteBuffer.allocate(embedding.size * 4)
            embedding.forEach { buffer.putFloat(it) }
            buffer.array()
        }

        val keyVersion = if (encrypted) {
            encryptedRepo.getEncryptionStats().currentKeyVersion
        } else {
            null
        }

        database.rAGChunkQueries.insert(
            id = VuidHelper.randomVUID(),
            document_id = documentId,
            chunk_index = index.toLong(),
            content = "Test chunk $index",
            token_count = 10,
            start_offset = (index * 100).toLong(),
            end_offset = ((index + 1) * 100).toLong(),
            page_number = 1,
            section_title = "Test Section",
            embedding_blob = embeddingBlob,
            embedding_type = "float32",
            embedding_dimension = embedding.size.toLong(),
            quant_scale = null,
            quant_offset = null,
            cluster_id = null,
            distance_to_centroid = null,
            created_timestamp = "2025-12-05T00:00:00Z",
            is_encrypted = encrypted,
            encryption_key_version = keyVersion?.toLong()
        )
    }
}
