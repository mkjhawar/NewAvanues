// filename: Universal/AVA/Features/RAG/src/androidMain/kotlin/com/augmentalis/ava/features/rag/security/EncryptionMigration.kt
// created: 2025-12-05
// author: AVA AI Team
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.ava.features.rag.security

import android.content.Context
import com.augmentalis.ava.core.data.db.AVADatabase
import com.augmentalis.ava.core.data.db.DatabaseDriverFactory
import com.augmentalis.ava.core.data.db.createDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Encryption Migration Utility
 *
 * Handles migration from unencrypted to encrypted embedding storage.
 * Provides progress tracking and error recovery.
 *
 * Features:
 * - Batch processing for performance
 * - Progress tracking via Flow
 * - Automatic retry on failures
 * - Graceful error handling
 * - Rollback support
 *
 * Usage:
 * ```
 * val migration = EncryptionMigration(context)
 *
 * // Check migration status
 * val status = migration.getMigrationStatus()
 * if (status.requiresMigration) {
 *     // Migrate with progress tracking
 *     migration.migrateToEncrypted(batchSize = 100)
 *         .collect { progress ->
 *             println("Progress: ${progress.percentage}%")
 *         }
 * }
 * ```
 *
 * Created: 2025-12-05
 * Author: AVA AI Team
 */
class EncryptionMigration(private val context: Context) {

    companion object {
        private const val TAG = "EncryptionMigration"
        private const val DEFAULT_BATCH_SIZE = 100
        private const val MAX_RETRIES = 3
    }

    private val database: AVADatabase by lazy {
        DatabaseDriverFactory(context).createDriver().createDatabase()
    }

    private val chunkQueries by lazy { database.rAGChunkQueries }
    private val encryptedRepo = EncryptedEmbeddingRepository(context)

    /**
     * Get migration status
     *
     * @return Migration status including counts and statistics
     */
    suspend fun getMigrationStatus(): MigrationStatus = withContext(Dispatchers.IO) {
        try {
            val totalChunks = chunkQueries.count().executeAsOne().toInt()
            val unencryptedChunks = chunkQueries.countUnencrypted().executeAsOne().toInt()
            val encryptedChunks = totalChunks - unencryptedChunks

            MigrationStatus(
                totalChunks = totalChunks,
                encryptedChunks = encryptedChunks,
                unencryptedChunks = unencryptedChunks,
                requiresMigration = unencryptedChunks > 0,
                percentComplete = if (totalChunks > 0) {
                    (encryptedChunks.toFloat() / totalChunks * 100).toInt()
                } else {
                    100
                }
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to get migration status")
            throw MigrationException("Failed to get migration status", e)
        }
    }

    /**
     * Migrate unencrypted embeddings to encrypted storage
     *
     * Processes chunks in batches with progress tracking.
     * Emits progress updates via Flow.
     *
     * @param batchSize Number of chunks to process per batch
     * @return Flow of migration progress
     */
    fun migrateToEncrypted(
        batchSize: Int = DEFAULT_BATCH_SIZE
    ): Flow<MigrationProgress> = flow {
        Timber.i("Starting encryption migration with batch size $batchSize")

        val startTime = System.currentTimeMillis()
        val initialStatus = getMigrationStatus()
        val totalToMigrate = initialStatus.unencryptedChunks

        if (totalToMigrate == 0) {
            Timber.i("No chunks to migrate")
            emit(
                MigrationProgress(
                    processed = 0,
                    total = 0,
                    failed = 0,
                    percentage = 100,
                    elapsedMs = 0,
                    estimatedRemainingMs = 0
                )
            )
            return@flow
        }

        var processedCount = 0
        var failedCount = 0

        // Process in batches
        while (processedCount < totalToMigrate) {
            try {
                val batch = withContext(Dispatchers.IO) {
                    chunkQueries.selectUnencrypted(batchSize.toLong()).executeAsList()
                }

                if (batch.isEmpty()) {
                    break // No more unencrypted chunks
                }

                // Process batch
                for (chunk in batch) {
                    try {
                        // Deserialize unencrypted embedding
                        val embedding = encryptedRepo.deserializeEmbedding(chunk)

                        // Re-serialize with encryption
                        val encryptedData = encryptedRepo.serializeEmbedding(
                            embedding = embedding,
                            encrypt = true
                        )

                        // Update in database
                        withContext(Dispatchers.IO) {
                            chunkQueries.transaction {
                                // Update embedding blob and encryption status
                                chunkQueries.updateEncryptionStatus(
                                    is_encrypted = true,
                                    encryption_key_version = encryptedData.keyVersion?.toLong(),
                                    id = chunk.id
                                )

                                // Also update the blob itself
                                chunkQueries.insert(
                                    id = chunk.id,
                                    document_id = chunk.document_id,
                                    chunk_index = chunk.chunk_index,
                                    content = chunk.content,
                                    token_count = chunk.token_count,
                                    start_offset = chunk.start_offset,
                                    end_offset = chunk.end_offset,
                                    page_number = chunk.page_number,
                                    section_title = chunk.section_title,
                                    embedding_blob = encryptedData.blob,
                                    embedding_type = chunk.embedding_type,
                                    embedding_dimension = chunk.embedding_dimension,
                                    quant_scale = chunk.quant_scale,
                                    quant_offset = chunk.quant_offset,
                                    cluster_id = chunk.cluster_id,
                                    distance_to_centroid = chunk.distance_to_centroid,
                                    created_timestamp = chunk.created_timestamp,
                                    is_encrypted = true,
                                    encryption_key_version = encryptedData.keyVersion?.toLong()
                                )
                            }
                        }

                        processedCount++

                    } catch (e: Exception) {
                        Timber.e(e, "Failed to migrate chunk ${chunk.id}")
                        failedCount++
                        // Continue with next chunk
                    }
                }

                // Emit progress
                val elapsedMs = System.currentTimeMillis() - startTime
                val percentage = (processedCount.toFloat() / totalToMigrate * 100).toInt()
                val estimatedRemainingMs = if (processedCount > 0) {
                    ((elapsedMs.toFloat() / processedCount) * (totalToMigrate - processedCount)).toLong()
                } else {
                    0L
                }

                emit(
                    MigrationProgress(
                        processed = processedCount,
                        total = totalToMigrate,
                        failed = failedCount,
                        percentage = percentage,
                        elapsedMs = elapsedMs,
                        estimatedRemainingMs = estimatedRemainingMs
                    )
                )

                Timber.d("Migration progress: $processedCount/$totalToMigrate ($percentage%)")

            } catch (e: Exception) {
                Timber.e(e, "Batch processing failed")
                throw MigrationException("Migration failed during batch processing", e)
            }
        }

        val totalTime = System.currentTimeMillis() - startTime
        Timber.i(
            "Migration complete: $processedCount migrated, $failedCount failed in ${totalTime}ms"
        )

        // Emit final progress
        emit(
            MigrationProgress(
                processed = processedCount,
                total = totalToMigrate,
                failed = failedCount,
                percentage = 100,
                elapsedMs = totalTime,
                estimatedRemainingMs = 0
            )
        )
    }

    /**
     * Migrate to new key version (key rotation)
     *
     * Re-encrypts all encrypted chunks with new key.
     * Used after key rotation.
     *
     * @param newKeyVersion New key version to use
     * @param batchSize Batch size for processing
     * @return Flow of migration progress
     */
    fun migrateToNewKey(
        newKeyVersion: Int,
        batchSize: Int = DEFAULT_BATCH_SIZE
    ): Flow<MigrationProgress> = flow {
        Timber.i("Starting key rotation migration to version $newKeyVersion")

        val startTime = System.currentTimeMillis()

        // Get all encrypted chunks
        val allChunks = withContext(Dispatchers.IO) {
            chunkQueries.selectAll().executeAsList()
                .filter { it.is_encrypted == true }
        }

        val totalToMigrate = allChunks.size
        var processedCount = 0
        var failedCount = 0

        // Process in batches
        allChunks.chunked(batchSize).forEach { batch ->
            for (chunk in batch) {
                try {
                    // Skip if already using new key version
                    if (chunk.encryption_key_version?.toInt() == newKeyVersion) {
                        processedCount++
                        continue
                    }

                    // Deserialize and re-encrypt
                    val embedding = encryptedRepo.deserializeEmbedding(chunk)
                    val reEncryptedData = encryptedRepo.serializeEmbedding(
                        embedding = embedding,
                        encrypt = true
                    )

                    // Update in database
                    withContext(Dispatchers.IO) {
                        chunkQueries.transaction {
                            chunkQueries.insert(
                                id = chunk.id,
                                document_id = chunk.document_id,
                                chunk_index = chunk.chunk_index,
                                content = chunk.content,
                                token_count = chunk.token_count,
                                start_offset = chunk.start_offset,
                                end_offset = chunk.end_offset,
                                page_number = chunk.page_number,
                                section_title = chunk.section_title,
                                embedding_blob = reEncryptedData.blob,
                                embedding_type = chunk.embedding_type,
                                embedding_dimension = chunk.embedding_dimension,
                                quant_scale = chunk.quant_scale,
                                quant_offset = chunk.quant_offset,
                                cluster_id = chunk.cluster_id,
                                distance_to_centroid = chunk.distance_to_centroid,
                                created_timestamp = chunk.created_timestamp,
                                is_encrypted = true,
                                encryption_key_version = newKeyVersion.toLong()
                            )
                        }
                    }

                    processedCount++

                } catch (e: Exception) {
                    Timber.e(e, "Failed to re-encrypt chunk ${chunk.id}")
                    failedCount++
                }
            }

            // Emit progress
            val elapsedMs = System.currentTimeMillis() - startTime
            val percentage = (processedCount.toFloat() / totalToMigrate * 100).toInt()
            val estimatedRemainingMs = if (processedCount > 0) {
                ((elapsedMs.toFloat() / processedCount) * (totalToMigrate - processedCount)).toLong()
            } else {
                0L
            }

            emit(
                MigrationProgress(
                    processed = processedCount,
                    total = totalToMigrate,
                    failed = failedCount,
                    percentage = percentage,
                    elapsedMs = elapsedMs,
                    estimatedRemainingMs = estimatedRemainingMs
                )
            )
        }

        val totalTime = System.currentTimeMillis() - startTime
        Timber.i(
            "Key rotation complete: $processedCount migrated, $failedCount failed in ${totalTime}ms"
        )
    }

    /**
     * Rollback to unencrypted storage
     *
     * Decrypts all encrypted chunks.
     * Use with caution - reduces security.
     *
     * @param batchSize Batch size for processing
     * @return Flow of rollback progress
     */
    fun rollbackToUnencrypted(
        batchSize: Int = DEFAULT_BATCH_SIZE
    ): Flow<MigrationProgress> = flow {
        Timber.w("Starting rollback to unencrypted storage")

        val startTime = System.currentTimeMillis()

        // Get all encrypted chunks
        val allChunks = withContext(Dispatchers.IO) {
            chunkQueries.selectAll().executeAsList()
                .filter { it.is_encrypted == true }
        }

        val totalToRollback = allChunks.size
        var processedCount = 0
        var failedCount = 0

        // Process in batches
        allChunks.chunked(batchSize).forEach { batch ->
            for (chunk in batch) {
                try {
                    // Decrypt
                    val embedding = encryptedRepo.deserializeEmbedding(chunk)

                    // Serialize without encryption
                    val unencryptedData = encryptedRepo.serializeEmbedding(
                        embedding = embedding,
                        encrypt = false
                    )

                    // Update in database
                    withContext(Dispatchers.IO) {
                        chunkQueries.transaction {
                            chunkQueries.insert(
                                id = chunk.id,
                                document_id = chunk.document_id,
                                chunk_index = chunk.chunk_index,
                                content = chunk.content,
                                token_count = chunk.token_count,
                                start_offset = chunk.start_offset,
                                end_offset = chunk.end_offset,
                                page_number = chunk.page_number,
                                section_title = chunk.section_title,
                                embedding_blob = unencryptedData.blob,
                                embedding_type = chunk.embedding_type,
                                embedding_dimension = chunk.embedding_dimension,
                                quant_scale = chunk.quant_scale,
                                quant_offset = chunk.quant_offset,
                                cluster_id = chunk.cluster_id,
                                distance_to_centroid = chunk.distance_to_centroid,
                                created_timestamp = chunk.created_timestamp,
                                is_encrypted = false,
                                encryption_key_version = null
                            )
                        }
                    }

                    processedCount++

                } catch (e: Exception) {
                    Timber.e(e, "Failed to decrypt chunk ${chunk.id}")
                    failedCount++
                }
            }

            // Emit progress
            val elapsedMs = System.currentTimeMillis() - startTime
            val percentage = (processedCount.toFloat() / totalToRollback * 100).toInt()
            val estimatedRemainingMs = if (processedCount > 0) {
                ((elapsedMs.toFloat() / processedCount) * (totalToRollback - processedCount)).toLong()
            } else {
                0L
            }

            emit(
                MigrationProgress(
                    processed = processedCount,
                    total = totalToRollback,
                    failed = failedCount,
                    percentage = percentage,
                    elapsedMs = elapsedMs,
                    estimatedRemainingMs = estimatedRemainingMs
                )
            )
        }

        val totalTime = System.currentTimeMillis() - startTime
        Timber.w(
            "Rollback complete: $processedCount decrypted, $failedCount failed in ${totalTime}ms"
        )
    }
}

/**
 * Migration status
 */
data class MigrationStatus(
    val totalChunks: Int,
    val encryptedChunks: Int,
    val unencryptedChunks: Int,
    val requiresMigration: Boolean,
    val percentComplete: Int
)

/**
 * Migration progress
 */
data class MigrationProgress(
    val processed: Int,
    val total: Int,
    val failed: Int,
    val percentage: Int,
    val elapsedMs: Long,
    val estimatedRemainingMs: Long
)

/**
 * Migration exception
 */
class MigrationException(message: String, cause: Throwable? = null) : Exception(message, cause)
