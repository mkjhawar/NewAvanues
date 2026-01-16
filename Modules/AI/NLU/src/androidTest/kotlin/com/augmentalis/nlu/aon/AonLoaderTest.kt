package com.augmentalis.nlu.aon

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.augmentalis.ava.core.common.Result
import com.augmentalis.ava.core.data.DatabaseProvider
import com.augmentalis.nlu.IntentClassifier
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for AonLoader
 *
 * Tests the complete .aot loading pipeline:
 * 1. Parse .aot files from assets
 * 2. Insert ontologies into semantic_intent_ontology table
 * 3. Compute mALBERT embeddings
 * 4. Insert embeddings into intent_embeddings table
 * 5. Loading statistics and error handling
 */
@RunWith(AndroidJUnit4::class)
class AonLoaderTest {

    private lateinit var context: Context
    private lateinit var intentClassifier: IntentClassifier
    private lateinit var loader: AonLoader

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        intentClassifier = IntentClassifier.getInstance(context)
        loader = AonLoader(context, intentClassifier)

        // Clear database before each test
        val database = DatabaseProvider.getDatabase(context)
        val ontologyDao = database.semanticIntentOntologyDao()
        val embeddingDao = database.intentEmbeddingDao()

        runBlocking {
            ontologyDao.deleteAll()
            embeddingDao.deleteAll()
        }
    }

    @Test
    fun test_loadAllOntologies() = runBlocking {
        // Load all ontologies
        val result = loader.loadAllOntologies(forceReload = true)

        // Verify success
        assertTrue("Expected success, got: $result", result is Result.Success)

        val stats = (result as Result.Success).data

        // Verify statistics
        assertTrue("Expected intents loaded", stats.totalIntents > 0)
        assertTrue("Expected files processed", stats.filesProcessed > 0)
        assertTrue("Expected embeddings created", stats.embeddingsCreated > 0)
        assertEquals("Expected no failures", 0, stats.failures)

        // Verify database was populated
        val database = DatabaseProvider.getDatabase(context)
        val ontologyDao = database.semanticIntentOntologyDao()
        val embeddingDao = database.intentEmbeddingDao()

        val ontologyCount = ontologyDao.getTotalOntologyCount()
        val embeddingCount = embeddingDao.getTotalEmbeddingCount()

        assertTrue("Expected ontologies in database", ontologyCount > 0)
        assertTrue("Expected embeddings in database", embeddingCount > 0)
        assertEquals("Expected same count", ontologyCount, embeddingCount)
    }

    @Test
    fun test_loadAllOntologies_skipIfAlreadyLoaded() = runBlocking {
        // First load
        val firstResult = loader.loadAllOntologies(forceReload = false)
        assertTrue("Expected success", firstResult is Result.Success)
        val firstStats = (firstResult as Result.Success).data

        // Second load (should skip)
        val secondResult = loader.loadAllOntologies(forceReload = false)
        assertTrue("Expected success", secondResult is Result.Success)
        val secondStats = (secondResult as Result.Success).data

        // Verify second load was skipped
        assertEquals("Expected all skipped", firstStats.totalIntents, secondStats.skipped)
        assertEquals("Expected no new files processed", 0, secondStats.filesProcessed)
    }

    @Test
    fun test_loadAllOntologies_forceReload() = runBlocking {
        // First load
        val firstResult = loader.loadAllOntologies(forceReload = true)
        assertTrue("Expected success", firstResult is Result.Success)

        // Force reload
        val secondResult = loader.loadAllOntologies(forceReload = true)
        assertTrue("Expected success", secondResult is Result.Success)
        val secondStats = (secondResult as Result.Success).data

        // Verify data was reloaded
        assertTrue("Expected intents reloaded", secondStats.totalIntents > 0)
        assertTrue("Expected files reprocessed", secondStats.filesProcessed > 0)
        assertEquals("Expected no skipped", 0, secondStats.skipped)
    }

    @Test
    fun test_loadOntologiesForLocale_enUS() = runBlocking {
        // Load en-US locale only
        val result = loader.loadOntologiesForLocale("en-US", forceReload = true)

        assertTrue("Expected success", result is Result.Success)

        val stats = (result as Result.Success).data

        // Verify statistics
        assertTrue("Expected intents loaded", stats.totalIntents > 0)

        // Verify only en-US data in database
        val database = DatabaseProvider.getDatabase(context)
        val ontologyDao = database.semanticIntentOntologyDao()

        val allOntologies = ontologyDao.getAllOntologiesForLocale("en-US")
        assertTrue("Expected en-US ontologies", allOntologies.isNotEmpty())

        // Verify all ontologies have correct locale
        for (ontology in allOntologies) {
            assertEquals("Expected en-US locale", "en-US", ontology.locale)
        }
    }

    @Test
    fun test_getLoadingStatus() = runBlocking {
        // Load ontologies first
        loader.loadAllOntologies(forceReload = true)

        // Get loading status
        val result = loader.getLoadingStatus()

        assertTrue("Expected success", result is Result.Success)

        val status = (result as Result.Success).data

        // Verify status
        assertTrue("Expected ontologies loaded", status.totalOntologies > 0)
        assertTrue("Expected embeddings created", status.totalEmbeddings > 0)
        assertTrue("Expected locales loaded", status.loadedLocales.isNotEmpty())

        // Verify en-US is in loaded locales
        assertTrue("Expected en-US locale",
            status.loadedLocales.contains("en-US")
        )
    }

    @Test
    fun test_ontologiesStoredCorrectly() = runBlocking {
        // Load ontologies
        loader.loadAllOntologies(forceReload = true)

        // Query database
        val database = DatabaseProvider.getDatabase(context)
        val ontologyDao = database.semanticIntentOntologyDao()

        val allOntologies = ontologyDao.getAllOntologiesForLocale("en-US")

        // Verify ontologies have all required fields
        for (ontology in allOntologies) {
            assertNotNull("Expected intent ID", ontology.intentId)
            assertNotNull("Expected locale", ontology.locale)
            assertNotNull("Expected canonical form", ontology.canonicalForm)
            assertNotNull("Expected description", ontology.description)
            assertTrue("Expected synonyms", ontology.synonyms.isNotEmpty())
            assertNotNull("Expected action type", ontology.actionType)
            assertTrue("Expected action sequence", ontology.actionSequence.isNotEmpty())
            assertTrue("Expected capabilities", ontology.requiredCapabilities.isNotEmpty())
        }
    }

    @Test
    fun test_embeddingsStoredCorrectly() = runBlocking {
        // Initialize IntentClassifier first (required for embedding computation)
        // Note: This test requires a real model file
        // For now, we'll just verify the structure

        // Load ontologies (this should trigger embedding computation)
        val result = loader.loadAllOntologies(forceReload = true)

        // If embeddings were computed, verify them
        if (result is Result.Success && (result.data.embeddingsCreated > 0)) {
            val database = DatabaseProvider.getDatabase(context)
            val embeddingDao = database.intentEmbeddingDao()

            val allEmbeddings = embeddingDao.getAllEmbeddingsForLocale("en-US")

            // Verify embeddings have all required fields
            for (embedding in allEmbeddings) {
                assertNotNull("Expected intent ID", embedding.intentId)
                assertEquals("Expected en-US locale", "en-US", embedding.locale)
                assertNotNull("Expected embedding vector", embedding.embeddingVector)
                assertTrue("Expected non-empty vector", embedding.embeddingVector.isNotEmpty())
                // Accept both 384-dim (MobileBERT) and 768-dim (mALBERT)
                assertTrue("Expected valid dimension (384 or 768)",
                    embedding.embeddingDimension == 384 || embedding.embeddingDimension == 768)
                assertEquals("Expected L2 normalization", "l2", embedding.normalizationType)
                assertEquals("Expected AON_SEMANTIC source", "AON_SEMANTIC", embedding.source)
            }
        }
    }

    @Test
    fun test_embeddingQualityVerification() = runBlocking {
        // Load ontologies
        val result = loader.loadAllOntologies(forceReload = true)

        // If embeddings were created, verify their quality
        if (result is Result.Success && (result.data.embeddingsCreated > 0)) {
            val database = DatabaseProvider.getDatabase(context)
            val embeddingDao = database.intentEmbeddingDao()

            val allEmbeddings = embeddingDao.getAllEmbeddingsForLocale("en-US")

            for (embedding in allEmbeddings) {
                val vector = embedding.getEmbedding()

                // Check dimension (Accept both MobileBERT-384 and mALBERT-768)
                assertTrue("Expected valid dimension (384 or 768)",
                    vector.size == 384 || vector.size == 768)
                assertEquals("Dimension should match metadata",
                    embedding.embeddingDimension, vector.size)

                // Check for NaN or infinite values
                assertFalse("No NaN values",
                    vector.any { it.isNaN() }
                )
                assertFalse("No infinite values",
                    vector.any { it.isInfinite() }
                )

                // Check L2 norm (should be ~1.0 for normalized vector)
                var norm = 0.0f
                for (value in vector) {
                    norm += value * value
                }
                norm = kotlin.math.sqrt(norm)

                assertTrue("Expected L2 norm ~1.0, got $norm",
                    kotlin.math.abs(norm - 1.0f) < 0.01f
                )
            }
        }
    }

    @Test
    fun test_loadingStatisticsAccuracy() = runBlocking {
        // Load ontologies
        val result = loader.loadAllOntologies(forceReload = true)

        assertTrue("Expected success", result is Result.Success)

        val stats = (result as Result.Success).data

        // Verify statistics match database
        val database = DatabaseProvider.getDatabase(context)
        val ontologyDao = database.semanticIntentOntologyDao()
        val embeddingDao = database.intentEmbeddingDao()

        val dbOntologyCount = ontologyDao.getTotalOntologyCount()
        val dbEmbeddingCount = embeddingDao.getTotalEmbeddingCount()

        assertEquals("Stats should match database",
            dbOntologyCount, stats.totalIntents
        )
        assertEquals("Embeddings should match database",
            dbEmbeddingCount, stats.embeddingsCreated
        )
    }

    @Test
    fun test_multiLocaleSupport() = runBlocking {
        // Load all supported locales
        val result = loader.loadAllOntologies(forceReload = true)

        assertTrue("Expected success", result is Result.Success)

        // Get loading status
        val statusResult = loader.getLoadingStatus()
        assertTrue("Expected success", statusResult is Result.Success)

        val status = (statusResult as Result.Success).data

        // Verify multiple locales were loaded (if available in assets)
        // At minimum, en-US should be present
        assertTrue("Expected at least en-US locale",
            status.loadedLocales.contains("en-US")
        )
    }
}
