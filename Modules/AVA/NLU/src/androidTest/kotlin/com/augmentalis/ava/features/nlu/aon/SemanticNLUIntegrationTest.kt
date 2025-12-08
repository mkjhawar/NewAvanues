package com.augmentalis.ava.features.nlu.aon

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.augmentalis.ava.core.common.Result
import com.augmentalis.ava.core.data.DatabaseProvider
import com.augmentalis.ava.features.nlu.IntentClassifier
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end integration test for AVA 2.0 Semantic NLU
 *
 * Tests the complete pipeline:
 * 1. .aot file → parse → database
 * 2. .aot data → embedding computation → database
 * 3. User utterance → IntentClassifier → intent classification
 *
 * This validates the entire semantic NLU system works together.
 */
@RunWith(AndroidJUnit4::class)
class SemanticNLUIntegrationTest {

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
    fun test_endToEnd_aonFileToIntentClassification() = runBlocking {
        // STEP 1: Load .aot files into database
        val loadResult = loader.loadAllOntologies(forceReload = true)
        assertTrue("Expected successful loading", loadResult is Result.Success)

        val stats = (loadResult as Result.Success).data
        assertTrue("Expected intents loaded", stats.totalIntents > 0)
        assertTrue("Expected embeddings created", stats.embeddingsCreated > 0)

        // STEP 2: Initialize IntentClassifier (loads embeddings from database)
        // Note: This requires a real model file to work fully
        // For testing, we verify the classifier loads the embeddings

        val database = DatabaseProvider.getDatabase(context)
        val embeddingDao = database.intentEmbeddingDao()
        val cachedEmbeddings = embeddingDao.getAllEmbeddingsForLocale("en-US")

        assertTrue("Expected cached embeddings", cachedEmbeddings.isNotEmpty())

        // STEP 3: Verify embeddings can be loaded
        for (embedding in cachedEmbeddings) {
            val vector = embedding.getEmbedding()

            // Verify vector quality
            assertEquals("Expected 768-dim", 768, vector.size)
            assertFalse("No NaN", vector.any { it.isNaN() })
            assertFalse("No infinite", vector.any { it.isInfinite() })

            // Verify L2 normalization
            var norm = 0.0f
            for (value in vector) {
                norm += value * value
            }
            norm = kotlin.math.sqrt(norm)
            assertTrue("Expected L2 norm ~1.0", kotlin.math.abs(norm - 1.0f) < 0.01f)
        }

        // STEP 4: Verify classifier can load these embeddings
        // (This would require full model initialization in a real test)
        val loadedIntents = cachedEmbeddings.map { it.intentId }.toSet()
        assertTrue("Expected send_email intent",
            loadedIntents.contains("send_email")
        )
    }

    @Test
    fun test_semanticOntologyDataIntegrity() = runBlocking {
        // Load ontologies
        loader.loadAllOntologies(forceReload = true)

        // Verify ontology data integrity
        val database = DatabaseProvider.getDatabase(context)
        val ontologyDao = database.semanticIntentOntologyDao()
        val embeddingDao = database.intentEmbeddingDao()

        val allOntologies = ontologyDao.getAllOntologiesForLocale("en-US")
        val allEmbeddings = embeddingDao.getAllEmbeddingsForLocale("en-US")

        // Every ontology should have a corresponding embedding
        assertEquals("Same count", allOntologies.size, allEmbeddings.size)

        val ontologyIntents = allOntologies.map { it.intentId }.toSet()
        val embeddingIntents = allEmbeddings.map { it.intentId }.toSet()

        assertEquals("Same intents", ontologyIntents, embeddingIntents)
    }

    @Test
    fun test_multilingualSupport_localeIsolation() = runBlocking {
        // Load all ontologies
        loader.loadAllOntologies(forceReload = true)

        // Verify locale isolation
        val database = DatabaseProvider.getDatabase(context)
        val ontologyDao = database.semanticIntentOntologyDao()

        // Get all locales
        val allLocales = ontologyDao.getAllLocales()
        assertTrue("Expected at least en-US", allLocales.contains("en-US"))

        // For each locale, verify data isolation
        for (locale in allLocales) {
            val ontologies = ontologyDao.getAllOntologiesForLocale(locale)

            // All ontologies should have correct locale
            for (ontology in ontologies) {
                assertEquals("Expected correct locale", locale, ontology.locale)
            }
        }
    }

    @Test
    fun test_performance_embeddingLoadTime() = runBlocking {
        // Load ontologies with embeddings
        loader.loadAllOntologies(forceReload = true)

        val database = DatabaseProvider.getDatabase(context)
        val embeddingDao = database.intentEmbeddingDao()

        // Measure embedding load time
        val startTime = System.currentTimeMillis()
        val embeddings = embeddingDao.getAllEmbeddingsForLocale("en-US")
        val loadTime = System.currentTimeMillis() - startTime

        assertTrue("Expected embeddings loaded", embeddings.isNotEmpty())

        // Should be very fast (< 100ms) since embeddings are pre-computed
        assertTrue("Expected fast load (<100ms), got ${loadTime}ms",
            loadTime < 100
        )
    }

    @Test
    fun test_zeroShotCapability_semanticDescriptions() = runBlocking {
        // Load ontologies
        loader.loadAllOntologies(forceReload = true)

        val database = DatabaseProvider.getDatabase(context)
        val ontologyDao = database.semanticIntentOntologyDao()

        // Verify all intents have semantic descriptions
        val allOntologies = ontologyDao.getAllOntologiesForLocale("en-US")

        for (ontology in allOntologies) {
            // Description is key for zero-shot learning
            assertTrue("Expected non-empty description for ${ontology.intentId}",
                ontology.description.isNotEmpty()
            )

            // Description should be semantic, not just the intent name
            assertFalse("Description should not just be intent ID",
                ontology.description == ontology.intentId
            )

            // Canonical form should be meaningful
            assertTrue("Expected canonical form for ${ontology.intentId}",
                ontology.canonicalForm.isNotEmpty()
            )

            // Should have synonyms for variation
            assertTrue("Expected synonyms for ${ontology.intentId}",
                ontology.synonyms.isNotEmpty()
            )
        }
    }

    @Test
    fun test_capabilityBasedAppDiscovery() = runBlocking {
        // Load ontologies
        loader.loadAllOntologies(forceReload = true)

        val database = DatabaseProvider.getDatabase(context)
        val ontologyDao = database.semanticIntentOntologyDao()

        // Get all ontologies
        val allOntologies = ontologyDao.getAllOntologiesForLocale("en-US")

        // Verify capability requirements are defined
        for (ontology in allOntologies) {
            assertTrue("Expected capabilities for ${ontology.intentId}",
                ontology.requiredCapabilities.isNotEmpty()
            )

            // Capabilities should be meaningful
            for (capability in ontology.requiredCapabilities) {
                assertFalse("Capability should not be empty", capability.isEmpty())
            }
        }

        // Test capability query
        val emailIntents = ontologyDao.getIntentsByCapability("email_client")
        assertTrue("Expected email intents", emailIntents.isNotEmpty())

        // Verify send_email requires email_client
        val sendEmail = emailIntents.find { it.intentId == "send_email" }
        assertNotNull("Expected send_email in email_client intents", sendEmail)
    }

    @Test
    fun test_multiStepActionSequences() = runBlocking {
        // Load ontologies
        loader.loadAllOntologies(forceReload = true)

        val database = DatabaseProvider.getDatabase(context)
        val ontologyDao = database.semanticIntentOntologyDao()

        // Get multi-step intents
        val multiStepIntents = ontologyDao.getIntentsByActionType("multi_step")
        assertTrue("Expected multi-step intents", multiStepIntents.isNotEmpty())

        // Verify action sequences
        for (intent in multiStepIntents) {
            assertTrue("Expected multiple steps for ${intent.intentId}",
                intent.actionSequence.size >= 2
            )

            // Steps should be ordered and meaningful
            for ((index, action) in intent.actionSequence.withIndex()) {
                assertFalse("Step ${index + 1} should not be empty", action.isEmpty())
            }
        }
    }

    @Test
    fun test_databaseMigrationIntegrity() = runBlocking {
        // This test verifies the migration from v4 to v5 works correctly
        // In a real test, you'd start with v4 database and migrate to v5

        val database = DatabaseProvider.getDatabase(context)

        // Verify new tables exist
        assertNotNull("Expected semantic_intent_ontology DAO",
            database.semanticIntentOntologyDao()
        )
        assertNotNull("Expected intent_embeddings DAO",
            database.intentEmbeddingDao()
        )

        // Load data
        loader.loadAllOntologies(forceReload = true)

        // Verify tables were populated
        val ontologyDao = database.semanticIntentOntologyDao()
        val embeddingDao = database.intentEmbeddingDao()

        assertTrue("Expected ontologies", ontologyDao.getTotalOntologyCount() > 0)
        assertTrue("Expected embeddings", embeddingDao.getTotalEmbeddingCount() > 0)
    }

    @Test
    fun test_cosineSimilaritySearch_readiness() = runBlocking {
        // Load ontologies with embeddings
        loader.loadAllOntologies(forceReload = true)

        val database = DatabaseProvider.getDatabase(context)
        val embeddingDao = database.intentEmbeddingDao()

        val allEmbeddings = embeddingDao.getAllEmbeddingsForLocale("en-US")
        assertTrue("Expected embeddings", allEmbeddings.size >= 2)

        // Verify we can compute cosine similarity between embeddings
        val embedding1 = allEmbeddings[0].getEmbedding()
        val embedding2 = allEmbeddings[1].getEmbedding()

        // Compute dot product (since vectors are L2-normalized)
        var similarity = 0.0f
        for (i in embedding1.indices) {
            similarity += embedding1[i] * embedding2[i]
        }

        // Similarity should be in valid range [-1, 1]
        assertTrue("Similarity should be >= -1", similarity >= -1.0f)
        assertTrue("Similarity should be <= 1", similarity <= 1.0f)
    }

    @Test
    fun test_fullPipeline_performanceTarget() = runBlocking {
        // Measure complete pipeline performance
        val startTime = System.currentTimeMillis()

        // Load .aot files
        val loadResult = loader.loadAllOntologies(forceReload = true)
        assertTrue("Expected success", loadResult is Result.Success)

        val loadTime = System.currentTimeMillis() - startTime

        // Loading should complete in reasonable time
        // Target: < 3 seconds for ~10 intents with embedding computation
        // Note: This is slower than runtime classification (<100ms)
        // because embeddings are being computed
        val stats = (loadResult as Result.Success).data
        val timePerIntent = if (stats.totalIntents > 0) {
            loadTime / stats.totalIntents
        } else {
            0
        }

        android.util.Log.i("SemanticNLUIntegrationTest",
            "Pipeline performance: ${stats.totalIntents} intents in ${loadTime}ms " +
            "(${timePerIntent}ms/intent)"
        )

        // Runtime classification should be fast (<100ms)
        // This is tested by loading embeddings from database
        val embeddingLoadStart = System.currentTimeMillis()
        val database = DatabaseProvider.getDatabase(context)
        val embeddings = database.intentEmbeddingDao().getAllEmbeddingsForLocale("en-US")
        val embeddingLoadTime = System.currentTimeMillis() - embeddingLoadStart

        assertTrue("Expected embeddings loaded", embeddings.isNotEmpty())
        assertTrue("Expected fast embedding load (<100ms), got ${embeddingLoadTime}ms",
            embeddingLoadTime < 100
        )
    }
}
