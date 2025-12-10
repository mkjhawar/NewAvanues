// filename: Universal/AVA/Features/RAG/src/androidMain/kotlin/com/augmentalis/ava/features/rag/security/EncryptedEmbeddingRepository.kt
// created: 2025-12-05
// author: AVA AI Team
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.ava.features.rag.security

import android.content.Context
import com.augmentalis.ava.core.data.db.Rag_chunk
import com.augmentalis.ava.features.rag.domain.Embedding
import timber.log.Timber
import java.nio.ByteBuffer

/**
 * Encrypted Embedding Repository
 *
 * Wraps embedding serialization/deserialization with encryption layer.
 * Provides transparent encryption for RAG embedding storage.
 *
 * Features:
 * - Automatic encryption/decryption of embeddings
 * - Checksum validation for document integrity
 * - Migration support from unencrypted to encrypted storage
 * - Backward compatibility with unencrypted data
 *
 * Usage:
 * ```
 * val repo = EncryptedEmbeddingRepository(context)
 *
 * // Serialize with encryption
 * val encrypted = repo.serializeEmbedding(embedding, encrypt = true)
 *
 * // Deserialize with decryption
 * val decrypted = repo.deserializeEmbedding(chunkEntity)
 * ```
 *
 * Created: 2025-12-05
 * Author: AVA AI Team
 */
class EncryptedEmbeddingRepository(context: Context) {

    private val encryptionManager = EmbeddingEncryptionManager(context)

    /**
     * Serialize embedding with optional encryption
     *
     * @param embedding Embedding to serialize
     * @param encrypt Whether to encrypt (default: true if encryption enabled)
     * @return Serialized (and optionally encrypted) embedding data
     */
    fun serializeEmbedding(
        embedding: Embedding.Float32,
        encrypt: Boolean = encryptionManager.isEncryptionEnabled()
    ): EmbeddingData {
        return try {
            if (encrypt) {
                val encryptedBytes = encryptionManager.encryptEmbedding(embedding.values)
                EmbeddingData(
                    blob = encryptedBytes,
                    isEncrypted = true,
                    keyVersion = encryptionManager.getCurrentKeyVersion()
                )
            } else {
                val plainBytes = floatArrayToBytes(embedding.values)
                EmbeddingData(
                    blob = plainBytes,
                    isEncrypted = false,
                    keyVersion = null
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to serialize embedding")
            throw EmbeddingSerializationException("Failed to serialize embedding", e)
        }
    }

    /**
     * Deserialize embedding with automatic decryption
     *
     * Automatically detects if data is encrypted and decrypts if needed.
     *
     * @param chunkEntity Chunk entity from database
     * @return Deserialized embedding
     */
    fun deserializeEmbedding(chunkEntity: Rag_chunk): Embedding.Float32 {
        return try {
            if (chunkEntity.is_encrypted == true) {
                // Decrypt
                val decryptedValues = encryptionManager.decryptEmbedding(chunkEntity.embedding_blob)
                Embedding.Float32(decryptedValues)
            } else {
                // Plain (legacy or encryption disabled)
                val values = bytesToFloatArray(chunkEntity.embedding_blob)
                Embedding.Float32(values)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to deserialize embedding for chunk ${chunkEntity.id}")
            throw EmbeddingDeserializationException("Failed to deserialize embedding", e)
        }
    }

    /**
     * Calculate checksum for document content
     *
     * @param content Document content as bytes
     * @return SHA-256 checksum
     */
    fun calculateChecksum(content: ByteArray): String {
        return encryptionManager.calculateChecksum(content)
    }

    /**
     * Calculate checksum for document content
     *
     * @param content Document content as string
     * @return SHA-256 checksum
     */
    fun calculateChecksum(content: String): String {
        return encryptionManager.calculateChecksum(content)
    }

    /**
     * Verify document checksum
     *
     * @param content Document content
     * @param expectedChecksum Expected checksum
     * @return true if checksums match
     */
    fun verifyChecksum(content: ByteArray, expectedChecksum: String): Boolean {
        return encryptionManager.verifyChecksum(content, expectedChecksum)
    }

    /**
     * Verify document checksum (string overload)
     */
    fun verifyChecksum(content: String, expectedChecksum: String): Boolean {
        return encryptionManager.verifyChecksum(content, expectedChecksum)
    }

    /**
     * Re-encrypt embedding with new key version
     *
     * Used during key rotation migration.
     *
     * @param oldData Existing encrypted data
     * @return Re-encrypted data with new key version
     */
    fun reEncryptEmbedding(oldData: EmbeddingData): EmbeddingData {
        return try {
            // Decrypt with old key
            val decryptedValues = encryptionManager.decryptEmbedding(oldData.blob)

            // Re-encrypt with current key
            val newEncryptedBytes = encryptionManager.encryptEmbedding(decryptedValues)

            EmbeddingData(
                blob = newEncryptedBytes,
                isEncrypted = true,
                keyVersion = encryptionManager.getCurrentKeyVersion()
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to re-encrypt embedding")
            throw EmbeddingReEncryptionException("Failed to re-encrypt embedding", e)
        }
    }

    /**
     * Get encryption statistics
     */
    fun getEncryptionStats(): EncryptionStats {
        return encryptionManager.getEncryptionStats()
    }

    /**
     * Check if encryption is enabled
     */
    fun isEncryptionEnabled(): Boolean {
        return encryptionManager.isEncryptionEnabled()
    }

    /**
     * Enable or disable encryption
     *
     * Note: Only affects new data. Use migration for existing data.
     */
    fun setEncryptionEnabled(enabled: Boolean) {
        encryptionManager.setEncryptionEnabled(enabled)
    }

    /**
     * Rotate encryption key
     *
     * @return New key version
     */
    fun rotateKey(): Int {
        return encryptionManager.rotateKey()
    }

    // Helper functions

    private fun floatArrayToBytes(array: FloatArray): ByteArray {
        val buffer = ByteBuffer.allocate(array.size * 4)
        array.forEach { buffer.putFloat(it) }
        return buffer.array()
    }

    private fun bytesToFloatArray(bytes: ByteArray): FloatArray {
        val buffer = ByteBuffer.wrap(bytes)
        return FloatArray(bytes.size / 4) { buffer.getFloat() }
    }
}

/**
 * Embedding data container
 */
data class EmbeddingData(
    val blob: ByteArray,
    val isEncrypted: Boolean,
    val keyVersion: Int?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EmbeddingData

        if (!blob.contentEquals(other.blob)) return false
        if (isEncrypted != other.isEncrypted) return false
        if (keyVersion != other.keyVersion) return false

        return true
    }

    override fun hashCode(): Int {
        var result = blob.contentHashCode()
        result = 31 * result + isEncrypted.hashCode()
        result = 31 * result + (keyVersion ?: 0)
        return result
    }
}

/**
 * Embedding serialization exception
 */
class EmbeddingSerializationException(message: String, cause: Throwable? = null) :
    Exception(message, cause)

/**
 * Embedding deserialization exception
 */
class EmbeddingDeserializationException(message: String, cause: Throwable? = null) :
    Exception(message, cause)

/**
 * Embedding re-encryption exception
 */
class EmbeddingReEncryptionException(message: String, cause: Throwable? = null) :
    Exception(message, cause)
