/*
 * Copyright (c) 2024-2025 Intelligent Devices LLC / Augmentalis
 * All Rights Reserved.
 *
 * This file is part of AVA AI and is proprietary software.
 * See LICENSE file in the project root for license information.
 */

package com.augmentalis.ava.features.nlu

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.augmentalis.ava.core.common.Result
import com.augmentalis.ava.core.data.DatabaseProvider
import com.augmentalis.ava.core.data.entity.EmbeddingMetadata
import com.augmentalis.ava.core.data.entity.SemanticIntentOntologyEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for embedding migration functionality
 *
 * Tests the complete migration pipeline:
 * 1. Version detection (dimension, name, version, checksum changes)
 * 2. Automatic migration triggering
 * 3. Embedding re-computation
 * 4. Metadata updates
 *
 * These tests verify that model changes are detected and handled correctly,
 * preventing silent failures from dimension mismatches.
 */
@RunWith(AndroidJUnit4::class)
class EmbeddingMigrationTest {

    private lateinit var context: Context
    private lateinit var modelManager: ModelManager
    private lateinit var intentClassifier: IntentClassifier

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        modelManager = ModelManager(context)
        intentClassifier = IntentClassifier.getInstance(context)

        // Clear database before each test
        runBlocking {
            val database = DatabaseProvider.getDatabase(context)
            database.clearAllTables()
        }
    }

    @After
    fun tearDown() {
        runBlocking {
            val database = DatabaseProvider.getDatabase(context)
            database.clearAllTables()
        }
    }

    /**
     * Test: Version check detects new install
     */
    @Test
    fun testVersionCheck_NewInstall() = runBlocking {
        val database = DatabaseProvider.getDatabase(context)
        val metadataDao = database.embeddingMetadataDao()

        // No metadata exists yet
        val result = modelManager.checkVersionStatus(metadataDao)

        assertTrue("Version check should succeed", result is Result.Success)
        val status = (result as Result.Success).data
        assertTrue("Should detect new install", status is VersionStatus.NewInstall)
    }

    /**
     * Test: Version check detects model dimension change
     */
    @Test
    fun testVersionCheck_DimensionChange() = runBlocking {
        val database = DatabaseProvider.getDatabase(context)
        val metadataDao = database.embeddingMetadataDao()

        // Insert metadata with different dimension (simulate old model)
        val oldMetadata = EmbeddingMetadata(
            modelName = "MobileBERT Lite",
            modelVersion = "MobileBERT-uncased-onnx-384",
            embeddingDimension = 384,  // Different from current
            modelChecksum = "abc123",
            createdAt = System.currentTimeMillis(),
            isActive = true,
            totalEmbeddings = 100
        )
        metadataDao.insert(oldMetadata)

        // Check version (assuming current model is mALBERT-768)
        val result = modelManager.checkVersionStatus(metadataDao)

        assertTrue("Version check should succeed", result is Result.Success)
        val status = (result as Result.Success).data

        // If current model dimension differs from stored, should need migration
        val currentDimension = modelManager.getCurrentModelVersion().dimension
        if (currentDimension != 384) {
            assertTrue("Should detect dimension change", status is VersionStatus.NeedsMigration)
            val migration = status as VersionStatus.NeedsMigration
            assertTrue("Reason should mention dimension", migration.reason.contains("dimension", ignoreCase = true))
        }
    }

    /**
     * Test: Version check detects model name change
     */
    @Test
    fun testVersionCheck_ModelNameChange() = runBlocking {
        val database = DatabaseProvider.getDatabase(context)
        val metadataDao = database.embeddingMetadataDao()

        val currentModel = modelManager.getCurrentModelVersion()

        // Insert metadata with different model name
        val oldMetadata = EmbeddingMetadata(
            modelName = if (currentModel.name.contains("mALBERT")) "MobileBERT Lite" else "mALBERT Multilingual",
            modelVersion = "test-v1.0",
            embeddingDimension = currentModel.dimension,  // Same dimension
            modelChecksum = currentModel.checksum,
            createdAt = System.currentTimeMillis(),
            isActive = true,
            totalEmbeddings = 100
        )
        metadataDao.insert(oldMetadata)

        val result = modelManager.checkVersionStatus(metadataDao)

        assertTrue("Version check should succeed", result is Result.Success)
        val status = (result as Result.Success).data
        assertTrue("Should detect model name change", status is VersionStatus.NeedsMigration)
    }

    /**
     * Test: Version check passes when model matches
     */
    @Test
    fun testVersionCheck_CurrentVersion() = runBlocking {
        val database = DatabaseProvider.getDatabase(context)
        val metadataDao = database.embeddingMetadataDao()

        val currentModel = modelManager.getCurrentModelVersion()

        // Insert metadata matching current model
        val metadata = EmbeddingMetadata(
            modelName = currentModel.name,
            modelVersion = currentModel.version,
            embeddingDimension = currentModel.dimension,
            modelChecksum = currentModel.checksum,
            createdAt = System.currentTimeMillis(),
            isActive = true,
            totalEmbeddings = 100
        )
        metadataDao.insert(metadata)

        val result = modelManager.checkVersionStatus(metadataDao)

        assertTrue("Version check should succeed", result is Result.Success)
        val status = (result as Result.Success).data
        assertTrue("Should detect current version", status is VersionStatus.Current)
    }

    /**
     * Test: Migration recomputes all embeddings
     */
    @Test
    fun testMigration_RecomputesEmbeddings() = runBlocking {
        // Initialize classifier first
        modelManager.copyModelFromAssets()
        val modelPath = modelManager.getModelPath()
        intentClassifier.initialize(modelPath)

        val database = DatabaseProvider.getDatabase(context)
        val ontologyDao = database.semanticIntentOntologyDao()
        val embeddingDao = database.intentEmbeddingDao()
        val metadataDao = database.embeddingMetadataDao()

        // Create test ontologies
        val testOntologies = listOf(
            SemanticIntentOntologyEntity(
                intentId = "test_intent_1",
                locale = "en-US",
                canonicalForm = "test intent one",
                description = "Test description one",
                synonyms = """["synonym 1", "synonym 2"]""",
                actionType = "simple",
                actionSequence = """[]""",
                requiredCapabilities = """[]""",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            ),
            SemanticIntentOntologyEntity(
                intentId = "test_intent_2",
                locale = "en-US",
                canonicalForm = "test intent two",
                description = "Test description two",
                synonyms = """["synonym 3", "synonym 4"]""",
                actionType = "simple",
                actionSequence = """[]""",
                requiredCapabilities = """[]""",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )

        ontologyDao.insertAll(testOntologies)

        // Run migration
        val migrator = EmbeddingMigrator(context, intentClassifier)
        val currentModel = modelManager.getCurrentModelVersion()

        var progressCallbackCount = 0
        val result = migrator.migrateEmbeddings(currentModel) { progress ->
            progressCallbackCount++
            assertTrue("Progress should be 0-1", progress >= 0f && progress <= 1f)
        }

        // Verify migration succeeded
        assertTrue("Migration should succeed", result is Result.Success)
        val stats = (result as Result.Success).data

        assertEquals("Should process 2 ontologies", 2, stats.totalProcessed)
        assertTrue("Should have successful embeddings", stats.successful > 0)
        assertTrue("Progress callback should be called", progressCallbackCount > 0)

        // Verify embeddings were created
        val embeddings = embeddingDao.getAllEmbeddings()
        assertTrue("Embeddings should be created", embeddings.isNotEmpty())

        // Verify metadata was saved
        val metadata = metadataDao.getActiveMetadata()
        assertNotNull("Metadata should be saved", metadata)
        assertEquals("Model name should match", currentModel.name, metadata?.modelName)
        assertEquals("Dimension should match", currentModel.dimension, metadata?.embeddingDimension)
    }

    /**
     * Test: Migration handles empty ontologies gracefully
     */
    @Test
    fun testMigration_EmptyOntologies() = runBlocking {
        modelManager.copyModelFromAssets()
        val modelPath = modelManager.getModelPath()
        intentClassifier.initialize(modelPath)

        val database = DatabaseProvider.getDatabase(context)
        val metadataDao = database.embeddingMetadataDao()

        // No ontologies exist
        val migrator = EmbeddingMigrator(context, intentClassifier)
        val currentModel = modelManager.getCurrentModelVersion()

        val result = migrator.migrateEmbeddings(currentModel)

        assertTrue("Migration should succeed even with no ontologies", result is Result.Success)
        val stats = (result as Result.Success).data

        assertEquals("Should process 0 ontologies", 0, stats.totalProcessed)
        assertEquals("Should have 0 successful", 0, stats.successful)
        assertEquals("Should have 0 failed", 0, stats.failed)

        // Metadata should still be saved
        val metadata = metadataDao.getActiveMetadata()
        assertNotNull("Metadata should be saved", metadata)
        assertEquals("Total embeddings should be 0", 0, metadata?.totalEmbeddings)
    }

    /**
     * Test: Metadata tracks embedding count correctly
     */
    @Test
    fun testMetadata_TracksEmbeddingCount() = runBlocking {
        modelManager.copyModelFromAssets()
        val modelPath = modelManager.getModelPath()
        intentClassifier.initialize(modelPath)

        val database = DatabaseProvider.getDatabase(context)
        val ontologyDao = database.semanticIntentOntologyDao()
        val metadataDao = database.embeddingMetadataDao()

        // Create 5 test ontologies
        val testOntologies = (1..5).map { i ->
            SemanticIntentOntologyEntity(
                intentId = "test_intent_$i",
                locale = "en-US",
                canonicalForm = "test intent $i",
                description = "Test description $i",
                synonyms = """[]""",
                actionType = "simple",
                actionSequence = """[]""",
                requiredCapabilities = """[]""",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        }

        ontologyDao.insertAll(testOntologies)

        // Run migration
        val migrator = EmbeddingMigrator(context, intentClassifier)
        val currentModel = modelManager.getCurrentModelVersion()
        migrator.migrateEmbeddings(currentModel)

        // Verify metadata count
        val metadata = metadataDao.getActiveMetadata()
        assertNotNull("Metadata should exist", metadata)
        assertEquals("Should track 5 embeddings", 5, metadata?.totalEmbeddings)
    }

    /**
     * Test: Model checksum calculation
     */
    @Test
    fun testModelChecksum_Calculated() = runBlocking {
        modelManager.copyModelFromAssets()

        val currentModel = modelManager.getCurrentModelVersion()

        assertNotNull("Checksum should be calculated", currentModel.checksum)
        assertNotEquals("Checksum should not be 'unknown'", "unknown", currentModel.checksum)
        assertTrue("Checksum should be hex string", currentModel.checksum.matches(Regex("[0-9a-f]+")))
        assertTrue("Checksum should be reasonable length", currentModel.checksum.length >= 32)
    }

    /**
     * Test: Only one metadata is active at a time
     */
    @Test
    fun testMetadata_OnlyOneActive() = runBlocking {
        val database = DatabaseProvider.getDatabase(context)
        val metadataDao = database.embeddingMetadataDao()

        // Insert first metadata
        val metadata1 = EmbeddingMetadata(
            modelName = "Model 1",
            modelVersion = "v1.0",
            embeddingDimension = 384,
            modelChecksum = "abc123",
            createdAt = System.currentTimeMillis(),
            isActive = true,
            totalEmbeddings = 10
        )
        metadataDao.insert(metadata1)

        // Deactivate all and insert second metadata
        metadataDao.deactivateAll()
        val metadata2 = EmbeddingMetadata(
            modelName = "Model 2",
            modelVersion = "v2.0",
            embeddingDimension = 768,
            modelChecksum = "def456",
            createdAt = System.currentTimeMillis(),
            isActive = true,
            totalEmbeddings = 20
        )
        metadataDao.insert(metadata2)

        // Verify only one active
        val activeMetadata = metadataDao.getActiveMetadata()
        assertNotNull("Should have active metadata", activeMetadata)
        assertEquals("Active should be latest", "Model 2", activeMetadata?.modelName)

        // Verify both exist in history
        val allMetadata = metadataDao.getAllMetadata()
        assertEquals("Should have 2 metadata records", 2, allMetadata.size)
    }
}
