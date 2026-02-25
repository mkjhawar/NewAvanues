/*
 * Copyright (c) 2024-2025 Intelligent Devices LLC / Augmentalis
 * All Rights Reserved.
 *
 * This file is part of AVA AI and is proprietary software.
 * See LICENSE file in the project root for license information.
 */

package com.augmentalis.nlu

import android.content.Context
import com.augmentalis.ava.core.common.Result
import com.augmentalis.ava.core.data.db.AVADatabase
import com.augmentalis.ava.core.data.db.DatabaseDriverFactory
import com.augmentalis.ava.core.data.db.createDatabase
import com.augmentalis.ava.core.data.db.EmbeddingMetadataQueries
import com.augmentalis.ava.core.data.db.IntentEmbeddingQueries
import com.augmentalis.ava.core.data.db.SemanticIntentOntologyQueries
import com.augmentalis.nlu.aon.AonEmbeddingComputer
import com.augmentalis.nlu.aon.SemanticIntentOntologyData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Handles automatic migration of embeddings when model changes
 *
 * When the embedding model changes (e.g., MobileBERT-384 → mALBERT-768),
 * all existing embeddings become invalid due to dimension mismatch.
 * This class automatically recomputes embeddings with the new model.
 *
 * Migration Triggers:
 * - Model dimension changed (384 ↔ 768)
 * - Model name changed (MobileBERT ↔ mALBERT)
 * - Model version updated (v1.0 → v1.1)
 * - Model file checksum changed (file replaced)
 *
 * Migration Strategy:
 * 1. Clear old embeddings from database
 * 2. Get all ontologies that need embeddings
 * 3. Recompute embeddings with new model
 * 4. Insert new embeddings into database
 * 5. Save new metadata
 *
 * Performance:
 * - First launch after model change: +5-10s (one-time migration)
 * - Progress tracking for UI feedback
 * - Batch insertion for efficiency
 *
 * Example usage:
 * ```kotlin
 * val migrator = EmbeddingMigrator(context, intentClassifier)
 * migrator.migrateEmbeddings(newModelVersion) { progress ->
 *     println("Migration progress: ${(progress * 100).toInt()}%")
 * }
 * ```
 *
 * @param context Android context
 * @param intentClassifier Intent classifier with loaded model
 */
class EmbeddingMigrator(
    private val context: Context,
    private val intentClassifier: IntentClassifier
) {
    companion object {
        private const val TAG = "EmbeddingMigrator"
    }

    private val database: AVADatabase by lazy {
        DatabaseDriverFactory(context).createDriver().createDatabase()
    }
    private val embeddingQueries: IntentEmbeddingQueries by lazy { database.intentEmbeddingQueries }
    private val metadataQueries: EmbeddingMetadataQueries by lazy { database.embeddingMetadataQueries }
    private val ontologyQueries: SemanticIntentOntologyQueries by lazy { database.semanticIntentOntologyQueries }
    private val embeddingComputer = AonEmbeddingComputer(context, intentClassifier)

    /**
     * Migrate all embeddings to new model
     *
     * This is the main entry point for migration. It:
     * 1. Clears old embeddings
     * 2. Loads all ontologies
     * 3. Recomputes embeddings with new model
     * 4. Saves new metadata
     *
     * @param newModelVersion New model version to migrate to
     * @param onProgress Progress callback (0.0 to 1.0)
     * @return Result indicating success or failure
     */
    suspend fun migrateEmbeddings(
        newModelVersion: ModelVersion,
        onProgress: (Float) -> Unit = {}
    ): Result<MigrationStats> = withContext(Dispatchers.IO) {
        try {
            val startTime = System.currentTimeMillis()

            nluLogInfo(TAG, "======================================")
            nluLogInfo(TAG, "  Embedding Migration Started")
            nluLogInfo(TAG, "======================================")
            nluLogInfo(TAG, "Target model: ${newModelVersion.name}")
            nluLogInfo(TAG, "Version: ${newModelVersion.version}")
            nluLogInfo(TAG, "Dimension: ${newModelVersion.dimension}")
            nluLogInfo(TAG, "======================================")

            onProgress(0f)

            // Step 1: Clear old embeddings
            nluLogInfo(TAG, "Step 1/5: Clearing old embeddings...")
            embeddingQueries.deleteAll()
            metadataQueries.deactivateAll()
            onProgress(0.05f)

            // Step 2: Get all ontologies that need embeddings
            nluLogInfo(TAG, "Step 2/5: Loading ontologies...")
            val ontologyRecords = ontologyQueries.selectAll().executeAsList()

            // Convert SQLDelight records to SemanticIntentOntologyData
            val ontologies = ontologyRecords.map { record ->
                SemanticIntentOntologyData(
                    intentId = record.intent_id,
                    locale = record.locale,
                    canonicalForm = record.canonical_form,
                    description = record.description,
                    synonyms = record.synonyms.split(",").filter { it.isNotBlank() },
                    actionType = record.action_type,
                    actionSequence = record.action_sequence.split(",").filter { it.isNotBlank() },
                    requiredCapabilities = record.required_capabilities.split(",").filter { it.isNotBlank() },
                    ontologyFileSource = record.ontology_file_source
                )
            }

            val total = ontologies.size
            nluLogInfo(TAG, "Found $total ontologies to process")
            onProgress(0.1f)

            if (total == 0) {
                nluLogWarn(TAG, "No ontologies found - nothing to migrate")

                // Still save metadata even if no embeddings
                saveMetadata(newModelVersion, 0)

                val stats = MigrationStats(
                    totalProcessed = 0,
                    successful = 0,
                    failed = 0,
                    duration = System.currentTimeMillis() - startTime
                )

                onProgress(1.0f)
                return@withContext Result.Success(stats)
            }

            // Step 3: Recompute embeddings with progress tracking
            nluLogInfo(TAG, "Step 3/5: Recomputing embeddings...")
            var successful = 0
            var failed = 0

            ontologies.forEachIndexed { index, ontology ->
                // Compute new embedding
                when (val result = embeddingComputer.computeEmbeddingFromOntology(ontology)) {
                    is Result.Success -> {
                        val embedding = result.data
                        val currentTime = System.currentTimeMillis()

                        embeddingQueries.insert(
                            intent_id = embedding.intentId,
                            locale = embedding.locale,
                            embedding_vector = embedding.embeddingVector,
                            embedding_dimension = embedding.embeddingDimension.toLong(),
                            model_version = embedding.modelVersion,
                            normalization_type = embedding.normalizationType,
                            ontology_id = embedding.ontologyId,
                            created_at = currentTime,
                            updated_at = currentTime,
                            example_count = embedding.exampleCount.toLong(),
                            source = embedding.source
                        )
                        successful++

                        // Report progress every 50 embeddings or on last item
                        if ((index + 1) % 50 == 0 || index == total - 1) {
                            val progress = 0.1f + ((index + 1).toFloat() / total * 0.8f)
                            onProgress(progress)
                            nluLogInfo(TAG, "Migration progress: ${index + 1}/$total (${(progress * 100).toInt()}%)")
                        }
                    }
                    is Result.Error -> {
                        nluLogError(TAG, "Failed to compute embedding for ${ontology.intentId}", result.exception)
                        failed++
                        // Continue with others
                    }
                }
            }

            nluLogInfo(TAG, "Step 4/5: Completed recomputing embeddings")
            nluLogInfo(TAG, "  Successful: $successful")
            nluLogInfo(TAG, "  Failed: $failed")
            onProgress(0.9f)

            // Step 4: Save new metadata
            nluLogInfo(TAG, "Step 5/5: Saving metadata...")
            saveMetadata(newModelVersion, successful)
            onProgress(0.95f)

            val duration = System.currentTimeMillis() - startTime
            val stats = MigrationStats(
                totalProcessed = total,
                successful = successful,
                failed = failed,
                duration = duration
            )

            nluLogInfo(TAG, "======================================")
            nluLogInfo(TAG, "  Migration Complete!")
            nluLogInfo(TAG, "======================================")
            nluLogInfo(TAG, "Total processed: $total")
            nluLogInfo(TAG, "Successful: $successful")
            nluLogInfo(TAG, "Failed: $failed")
            nluLogInfo(TAG, "Duration: ${duration}ms (${duration / 1000.0}s)")
            nluLogInfo(TAG, "======================================")

            onProgress(1.0f)

            Result.Success(stats)

        } catch (e: Exception) {
            nluLogError(TAG, "Migration failed", e)
            Result.Error(e, "Embedding migration failed: ${e.message}")
        }
    }

    /**
     * Save metadata for new model version
     */
    private suspend fun saveMetadata(modelVersion: ModelVersion, embeddingCount: Int) {
        metadataQueries.insert(
            model_name = modelVersion.name,
            model_version = modelVersion.version,
            embedding_dimension = modelVersion.dimension.toLong(),
            model_checksum = modelVersion.checksum,
            created_at = System.currentTimeMillis(),
            is_active = true,
            total_embeddings = embeddingCount.toLong()
        )
        nluLogInfo(TAG, "Saved new metadata: $embeddingCount embeddings")
    }

    /**
     * Check if migration is needed (convenience method)
     */
    suspend fun isMigrationNeeded(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val modelManager = ModelManager(context)
            when (val statusResult = modelManager.checkVersionStatus(metadataQueries)) {
                is Result.Success -> {
                    val needsMigration = statusResult.data is VersionStatus.NeedsMigration
                    Result.Success(needsMigration)
                }
                is Result.Error -> statusResult
            }
        } catch (e: Exception) {
            Result.Error(e, "Failed to check migration status: ${e.message}")
        }
    }
}

/**
 * Migration statistics
 */
data class MigrationStats(
    val totalProcessed: Int,
    val successful: Int,
    val failed: Int,
    val duration: Long  // milliseconds
) {
    val successRate: Float
        get() = if (totalProcessed > 0) successful.toFloat() / totalProcessed else 0f
}
