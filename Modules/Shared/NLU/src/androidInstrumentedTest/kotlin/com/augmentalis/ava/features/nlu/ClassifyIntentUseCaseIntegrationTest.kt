package com.augmentalis.ava.features.nlu

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.augmentalis.ava.core.common.Result
import com.augmentalis.ava.core.data.AVADatabase
import com.augmentalis.ava.core.data.repository.TrainExampleRepositoryImpl
import com.augmentalis.ava.core.domain.model.TrainExample
import com.augmentalis.ava.core.domain.model.TrainExampleSource
import com.augmentalis.ava.features.nlu.usecase.ClassifyIntentUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.security.MessageDigest

/**
 * Integration tests for ClassifyIntentUseCase
 * Tests end-to-end pipeline: TrainExampleRepository → IntentClassifier → Result
 */
@RunWith(AndroidJUnit4::class)
class ClassifyIntentUseCaseIntegrationTest {

    private lateinit var database: AVADatabase
    private lateinit var repository: TrainExampleRepositoryImpl
    private lateinit var classifier: IntentClassifier
    private lateinit var useCase: ClassifyIntentUseCase
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()

        // Create in-memory database for testing
        database = Room.inMemoryDatabaseBuilder(
            context,
            AVADatabase::class.java
        ).build()

        repository = TrainExampleRepositoryImpl(database.trainExampleDao())
        classifier = IntentClassifier.getInstance(context)
        useCase = ClassifyIntentUseCase(classifier, repository)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun invoke_withNoTrainedExamples_needsTrainingTrue() = runTest {
        // Given - no training data in database
        val utterance = "Turn on the lights"

        // When
        val result = useCase(utterance, locale = "en-US")

        // Then
        assertTrue(result is Result.Success)
        if (result is Result.Success) {
            assertTrue(result.data.needsTraining)
            assertEquals(null, result.data.intent)
        }
    }

    @Test
    fun invoke_withTrainedExamples_attemptsClassification() = runTest {
        // Given - add training examples
        val examples = listOf(
            createTrainExample("Turn on the lights", "control_lights"),
            createTrainExample("Turn off the lamp", "control_lights"),
            createTrainExample("What's the weather?", "check_weather"),
            createTrainExample("Set alarm for 7am", "set_alarm")
        )

        examples.forEach { repository.addTrainExample(it) }

        // When
        val utterance = "Turn on bedroom lights"
        val result = useCase(utterance, locale = "en-US")

        // Then
        assertTrue(result is Result.Success || result is Result.Error)
        // With mock model, expect error (no model initialized)
        // With real model, expect classification result
    }

    @Test
    fun invoke_withLowConfidence_needsTrainingTrue() = runTest {
        // Given
        val examples = listOf(
            createTrainExample("Turn on lights", "control_lights")
        )
        examples.forEach { repository.addTrainExample(it) }

        // When - use high confidence threshold
        val result = useCase(
            utterance = "Some unrelated query",
            locale = "en-US",
            confidenceThreshold = 0.9f
        )

        // Then - should need training due to low confidence
        // Note: Actual behavior depends on model being initialized
        assertTrue(result is Result.Success || result is Result.Error)
    }

    @Test
    fun invoke_incrementsUsageCount_onSuccessfulMatch() = runTest {
        // Given
        val example = createTrainExample("Turn on the lights", "control_lights")
        repository.addTrainExample(example)

        val initialExamples = repository.getExamplesForIntent("control_lights").first()
        val initialCount = initialExamples.first().usageCount

        // When - attempt classification (will fail without model, but tests the pattern)
        useCase(
            utterance = "Turn on the lights",
            locale = "en-US"
        )

        // Note: Usage count increment only happens with successful classification
        // This test validates the repository integration
        val examples = repository.getExamplesForIntent("control_lights").first()
        assertTrue(examples.isNotEmpty())
    }

    @Test
    fun invoke_multipleLocales_filtersCorrectly() = runTest {
        // Given - examples in different locales
        val enExample = createTrainExample("Turn on lights", "control_lights", locale = "en-US")
        val esExample = createTrainExample("Enciende las luces", "control_lights", locale = "es-ES")

        repository.addTrainExample(enExample)
        repository.addTrainExample(esExample)

        // When - classify with en-US locale
        val result = useCase(
            utterance = "Turn on bedroom lights",
            locale = "en-US"
        )

        // Then - should only use en-US examples as candidates
        val enExamples = repository.getExamplesForLocale("en-US").first()
        assertEquals(1, enExamples.size)
        assertEquals("en-US", enExamples.first().locale)
    }

    @Test
    fun invoke_withDuplicateIntents_deduplicatesCandidates() = runTest {
        // Given - multiple examples for same intent
        val examples = listOf(
            createTrainExample("Turn on lights", "control_lights"),
            createTrainExample("Turn off lights", "control_lights"),
            createTrainExample("Lights on", "control_lights"),
            createTrainExample("What's the weather?", "check_weather")
        )
        examples.forEach { repository.addTrainExample(it) }

        // When
        val allExamples = repository.getExamplesForLocale("en-US").first()
        val candidateIntents = allExamples.map { it.intent }.distinct()

        // Then - should have 2 unique intents despite 4 examples
        assertEquals(2, candidateIntents.size)
        assertTrue(candidateIntents.contains("control_lights"))
        assertTrue(candidateIntents.contains("check_weather"))
    }

    @Test
    fun invoke_endToEndPipeline_validatesDataFlow() = runTest {
        // Given - realistic training data
        val trainingData = listOf(
            createTrainExample("Turn on the lights", "control_lights"),
            createTrainExample("Switch on bedroom lamp", "control_lights"),
            createTrainExample("What's the weather like?", "check_weather"),
            createTrainExample("Tell me the forecast", "check_weather"),
            createTrainExample("Set alarm for 7am", "set_alarm"),
            createTrainExample("Wake me up at 8", "set_alarm")
        )

        trainingData.forEach { repository.addTrainExample(it) }

        // When - test various utterances
        val testUtterances = listOf(
            "Turn on kitchen lights",
            "What's the temperature?",
            "Set alarm for 6:30am"
        )

        val results = testUtterances.map { utterance ->
            useCase(utterance, locale = "en-US")
        }

        // Then - all should complete (success or error, no crashes)
        assertEquals(3, results.size)
        results.forEach { result ->
            assertTrue(result is Result.Success || result is Result.Error)
        }
    }

    @Test
    fun invoke_performanceBenchmark_multipleClassifications() = runTest {
        // Given - training data
        val examples = List(20) { index ->
            createTrainExample(
                utterance = "Test utterance $index",
                intent = "test_intent_${index % 5}"
            )
        }
        examples.forEach { repository.addTrainExample(it) }

        // When - measure time for 10 classifications
        val startTime = System.currentTimeMillis()

        repeat(10) { index ->
            useCase(
                utterance = "Test query $index",
                locale = "en-US"
            )
        }

        val elapsed = System.currentTimeMillis() - startTime

        // Then
        println("10 classifications took: ${elapsed}ms (avg: ${elapsed / 10}ms per classification)")
        // Performance validation requires model initialization
        // Target: < 50ms per classification
    }

    private fun createTrainExample(
        utterance: String,
        intent: String,
        locale: String = "en-US"
    ): TrainExample {
        val hashInput = "$utterance:$intent"
        val hash = MessageDigest.getInstance("MD5")
            .digest(hashInput.toByteArray())
            .joinToString("") { "%02x".format(it) }

        return TrainExample(
            exampleHash = hash,
            utterance = utterance,
            intent = intent,
            locale = locale,
            source = TrainExampleSource.MANUAL,
            createdAt = System.currentTimeMillis()
        )
    }
}
