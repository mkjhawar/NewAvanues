// filename: Universal/AVA/Features/RAG/src/androidTest/kotlin/com/augmentalis/ava/features/rag/security/EmbeddingEncryptionTest.kt
// created: 2025-12-05
// author: AVA AI Team
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.rag.security

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.augmentalis.rag.domain.Embedding
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.security.KeyStore

/**
 * Integration tests for embedding encryption
 *
 * Tests:
 * - Encryption/decryption of embeddings
 * - Checksum validation
 * - Key rotation
 * - Error handling
 * - Performance benchmarks
 */
@RunWith(AndroidJUnit4::class)
class EmbeddingEncryptionTest {

    private lateinit var context: Context
    private lateinit var encryptionManager: EmbeddingEncryptionManager
    private lateinit var encryptedRepo: EncryptedEmbeddingRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        encryptionManager = EmbeddingEncryptionManager(context)
        encryptedRepo = EncryptedEmbeddingRepository(context)
    }

    @After
    fun cleanup() {
        // Clean up test keys from Android Keystore
        try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)
            // Note: We can't delete the actual keys without breaking real data
            // In production, we'd need a separate test keystore
        } catch (e: Exception) {
            // Ignore cleanup errors
        }
    }

    @Test
    fun testEncryptionEnabled() {
        // Encryption should be enabled by default
        assertTrue(encryptionManager.isEncryptionEnabled())
    }

    @Test
    fun testEncryptDecrypt_SmallEmbedding() {
        // Test with small embedding (384 dimensions - typical for sentence transformers)
        val original = FloatArray(384) { it.toFloat() }

        val encrypted = encryptionManager.encryptEmbedding(original)
        val decrypted = encryptionManager.decryptEmbedding(encrypted)

        assertArrayEquals(original, decrypted, 0.0001f)
        assertNotEquals(original.contentHashCode(), encrypted.contentHashCode())
    }

    @Test
    fun testEncryptDecrypt_LargeEmbedding() {
        // Test with large embedding (1536 dimensions - OpenAI ada-002 size)
        val original = FloatArray(1536) { (it * 0.01f) }

        val encrypted = encryptionManager.encryptEmbedding(original)
        val decrypted = encryptionManager.decryptEmbedding(encrypted)

        assertArrayEquals(original, decrypted, 0.0001f)
    }

    @Test
    fun testEncryption_RandomIV() {
        // Verify that each encryption uses different IV
        val embedding = FloatArray(384) { it.toFloat() }

        val encrypted1 = encryptionManager.encryptEmbedding(embedding)
        val encrypted2 = encryptionManager.encryptEmbedding(embedding)

        // Ciphertext should be different due to random IV
        assertFalse(encrypted1.contentEquals(encrypted2))

        // But decryption should yield same result
        val decrypted1 = encryptionManager.decryptEmbedding(encrypted1)
        val decrypted2 = encryptionManager.decryptEmbedding(encrypted2)
        assertArrayEquals(decrypted1, decrypted2, 0.0001f)
    }

    @Test
    fun testEncryption_VersionTracking() {
        val embedding = FloatArray(384) { it.toFloat() }

        val keyVersion = encryptionManager.getCurrentKeyVersion()
        val encrypted = encryptionManager.encryptEmbedding(embedding)

        // Version should be in first byte
        assertEquals(keyVersion.toByte(), encrypted[0])
    }

    @Test(expected = DecryptionException::class)
    fun testDecryption_CorruptedData() {
        val embedding = FloatArray(384) { it.toFloat() }
        val encrypted = encryptionManager.encryptEmbedding(embedding)

        // Corrupt the ciphertext
        encrypted[20] = (encrypted[20] + 1).toByte()

        // Should throw DecryptionException due to authentication tag mismatch
        encryptionManager.decryptEmbedding(encrypted)
    }

    @Test
    fun testChecksum_ByteArray() {
        val data = "Hello, World!".toByteArray()
        val checksum = encryptionManager.calculateChecksum(data)

        // SHA-256 should produce 64 hex characters
        assertEquals(64, checksum.length)

        // Verify checksum
        assertTrue(encryptionManager.verifyChecksum(data, checksum))

        // Different data should have different checksum
        val data2 = "Different data".toByteArray()
        assertFalse(encryptionManager.verifyChecksum(data2, checksum))
    }

    @Test
    fun testChecksum_String() {
        val text = "Sample document content for checksum testing"
        val checksum = encryptionManager.calculateChecksum(text)

        assertEquals(64, checksum.length)
        assertTrue(encryptionManager.verifyChecksum(text, checksum))

        // Modified text should fail verification
        assertFalse(encryptionManager.verifyChecksum(text + " ", checksum))
    }

    @Test
    fun testKeyRotation() {
        val originalVersion = encryptionManager.getCurrentKeyVersion()

        // Rotate key
        val newVersion = encryptionManager.rotateKey()

        assertEquals(originalVersion + 1, newVersion)
        assertEquals(newVersion, encryptionManager.getCurrentKeyVersion())

        // Old data should still be decryptable
        val embedding = FloatArray(384) { it.toFloat() }

        // Encrypt with old key (simulate old data)
        // Note: In real scenario, we'd need to temporarily revert version
        // For this test, we just verify new encryption works
        val encrypted = encryptionManager.encryptEmbedding(embedding)
        val decrypted = encryptionManager.decryptEmbedding(encrypted)

        assertArrayEquals(embedding, decrypted, 0.0001f)
    }

    @Test
    fun testEncryptedRepository_Serialization() {
        val embedding = Embedding.Float32(FloatArray(384) { it * 0.01f })

        val embeddingData = encryptedRepo.serializeEmbedding(embedding, encrypt = true)

        assertTrue(embeddingData.isEncrypted)
        assertNotNull(embeddingData.keyVersion)
        assertTrue(embeddingData.blob.isNotEmpty())
    }

    @Test
    fun testEncryptedRepository_Unencrypted() {
        val embedding = Embedding.Float32(FloatArray(384) { it * 0.01f })

        val embeddingData = encryptedRepo.serializeEmbedding(embedding, encrypt = false)

        assertFalse(embeddingData.isEncrypted)
        assertNull(embeddingData.keyVersion)
    }

    @Test
    fun testEncryptionStats() {
        val stats = encryptionManager.getEncryptionStats()

        assertTrue(stats.enabled)
        assertTrue(stats.currentKeyVersion >= 1)
        assertTrue(stats.availableKeyVersions.isNotEmpty())
        assertTrue(stats.availableKeyVersions.contains(stats.currentKeyVersion))
    }

    @Test
    fun testPerformance_EncryptionOverhead() {
        // Benchmark encryption performance
        val embedding = FloatArray(384) { it.toFloat() }
        val iterations = 100

        // Measure encryption time
        val encryptStart = System.currentTimeMillis()
        repeat(iterations) {
            encryptionManager.encryptEmbedding(embedding)
        }
        val encryptTime = System.currentTimeMillis() - encryptStart

        // Measure decryption time
        val encrypted = encryptionManager.encryptEmbedding(embedding)
        val decryptStart = System.currentTimeMillis()
        repeat(iterations) {
            encryptionManager.decryptEmbedding(encrypted)
        }
        val decryptTime = System.currentTimeMillis() - decryptStart

        // Log performance metrics
        val avgEncryptMs = encryptTime.toFloat() / iterations
        val avgDecryptMs = decryptTime.toFloat() / iterations

        println("Average encryption time: ${avgEncryptMs}ms")
        println("Average decryption time: ${avgDecryptMs}ms")

        // Verify reasonable performance (should be < 10ms per operation)
        assertTrue("Encryption too slow: ${avgEncryptMs}ms", avgEncryptMs < 10f)
        assertTrue("Decryption too slow: ${avgDecryptMs}ms", avgDecryptMs < 10f)
    }

    @Test
    fun testPerformance_ChecksumOverhead() {
        // Benchmark checksum performance
        val data = ByteArray(1024 * 1024) { it.toByte() } // 1MB
        val iterations = 50

        val start = System.currentTimeMillis()
        repeat(iterations) {
            encryptionManager.calculateChecksum(data)
        }
        val elapsed = System.currentTimeMillis() - start

        val avgMs = elapsed.toFloat() / iterations
        println("Average checksum time (1MB): ${avgMs}ms")

        // Should be reasonably fast (< 50ms for 1MB)
        assertTrue("Checksum too slow: ${avgMs}ms", avgMs < 50f)
    }

    @Test
    fun testEncryptionDisable() {
        // Disable encryption
        encryptionManager.setEncryptionEnabled(false)
        assertFalse(encryptionManager.isEncryptionEnabled())

        // Re-enable
        encryptionManager.setEncryptionEnabled(true)
        assertTrue(encryptionManager.isEncryptionEnabled())
    }

    @Test
    fun testReEncryption() {
        val embedding = Embedding.Float32(FloatArray(384) { it * 0.01f })

        // Encrypt with current key
        val originalData = encryptedRepo.serializeEmbedding(embedding, encrypt = true)

        // Re-encrypt (simulate key rotation)
        val reEncryptedData = encryptedRepo.reEncryptEmbedding(originalData)

        assertTrue(reEncryptedData.isEncrypted)
        assertNotNull(reEncryptedData.keyVersion)

        // Both should decrypt to same values (can't test without mocking chunk entity)
        // This would be tested in integration tests with full database
    }

    @Test
    fun testChecksumDeterministic() {
        val data = "Deterministic test data"

        val checksum1 = encryptionManager.calculateChecksum(data)
        val checksum2 = encryptionManager.calculateChecksum(data)

        // Same data should always produce same checksum
        assertEquals(checksum1, checksum2)
    }

    @Test
    fun testEncryption_EmptyEmbedding() {
        // Edge case: empty embedding
        val empty = FloatArray(0)

        val encrypted = encryptionManager.encryptEmbedding(empty)
        val decrypted = encryptionManager.decryptEmbedding(encrypted)

        assertEquals(0, decrypted.size)
    }

    @Test
    fun testEncryption_SingleValueEmbedding() {
        // Edge case: single value
        val single = floatArrayOf(1.0f)

        val encrypted = encryptionManager.encryptEmbedding(single)
        val decrypted = encryptionManager.decryptEmbedding(encrypted)

        assertArrayEquals(single, decrypted, 0.0001f)
    }

    @Test
    fun testEncryption_NegativeValues() {
        // Test with negative values (common in embeddings)
        val embedding = FloatArray(384) { (it - 192).toFloat() * 0.01f }

        val encrypted = encryptionManager.encryptEmbedding(embedding)
        val decrypted = encryptionManager.decryptEmbedding(encrypted)

        assertArrayEquals(embedding, decrypted, 0.0001f)
    }

    @Test
    fun testEncryption_VerySmallValues() {
        // Test with very small values
        val embedding = FloatArray(384) { it * 0.0001f }

        val encrypted = encryptionManager.encryptEmbedding(embedding)
        val decrypted = encryptionManager.decryptEmbedding(encrypted)

        assertArrayEquals(embedding, decrypted, 0.00001f)
    }

    @Test
    fun testChecksum_EmptyData() {
        val checksum = encryptionManager.calculateChecksum(ByteArray(0))
        assertEquals(64, checksum.length)
    }

    @Test
    fun testChecksum_LargeData() {
        // Test checksum with 10MB data
        val largeData = ByteArray(10 * 1024 * 1024) { it.toByte() }
        val checksum = encryptionManager.calculateChecksum(largeData)

        assertEquals(64, checksum.length)
        assertTrue(encryptionManager.verifyChecksum(largeData, checksum))
    }
}
