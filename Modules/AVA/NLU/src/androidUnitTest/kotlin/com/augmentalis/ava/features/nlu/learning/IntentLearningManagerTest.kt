// filename: Universal/AVA/Features/NLU/src/androidUnitTest/kotlin/com/augmentalis/ava/features/nlu/learning/IntentLearningManagerTest.kt
// created: 2025-11-25
// author: Testing Swarm Agent 1 - AVA AI Features 003 + 004
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.ava.features.nlu.learning

import android.content.Context
import com.augmentalis.ava.core.data.AVADatabase
import com.augmentalis.ava.core.data.DatabaseProvider
import com.augmentalis.ava.core.data.dao.IntentExampleDao
import com.augmentalis.ava.core.data.entity.IntentExampleEntity
import com.augmentalis.ava.features.nlu.IntentClassifier
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertTrue

/**
 * Unit tests for IntentLearningManager.saveLearnedExample() (REQ-004)
 *
 * Tests:
 * - saveLearnedExample with USER_CONFIRMED source should persist to database
 * - saveLearnedExample with USER_CORRECTED source should persist to database
 * - saveLearnedExample with duplicate should not fail
 *
 * Uses Robolectric + MockK for Android unit testing.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class IntentLearningManagerTest {

    private lateinit var context: Context
    private lateinit var mockDatabase: AVADatabase
    private lateinit var mockDao: IntentExampleDao
    private lateinit var mockClassifier: IntentClassifier
    private lateinit var learningManager: IntentLearningManager

    @Before
    fun setup() {
        // Mock Android Context
        context = mockk(relaxed = true)

        // Mock DAO
        mockDao = mockk(relaxed = true)

        // Mock Database
        mockDatabase = mockk(relaxed = true) {
            every { intentExampleDao() } returns mockDao
        }

        // Mock DatabaseProvider to return our mock database
        mockkObject(DatabaseProvider)
        every { DatabaseProvider.getDatabase(any()) } returns mockDatabase

        // Mock IntentClassifier to prevent actual initialization
        mockClassifier = mockk(relaxed = true)
        mockkObject(IntentClassifier)
        every { IntentClassifier.getInstance(any()) } returns mockClassifier
        coEvery { mockClassifier.initialize(any()) } returns Result.Success(Unit)

        // Mock external files directory for model path
        val mockFileDir = mockk<java.io.File>(relaxed = true) {
            every { absolutePath } returns "/mock/path"
        }
        every { context.getExternalFilesDir(null) } returns mockFileDir

        // Create IntentLearningManager instance
        learningManager = IntentLearningManager(context)
    }

    /**
     * Test 1: saveLearnedExample with USER_CONFIRMED source should persist to database
     */
    @Test
    fun `saveLearnedExample with USER_CONFIRMED source should persist to database`() = runBlocking {
        // Given
        val userText = "switch off the lamp"
        val intentId = "LIGHT_OFF"
        val source = "USER_CONFIRMED"

        // Mock DAO to return empty list initially (no existing examples)
        coEvery { mockDao.getAllExamplesOnce() } returns emptyList()

        // Mock successful insert (return row ID > 0)
        coEvery { mockDao.insertIntentExample(any()) } returns 1L

        // When
        val result = learningManager.saveLearnedExample(userText, intentId, source)

        // Then
        assertTrue(result, "saveLearnedExample should return true on success")

        // Verify DAO was called to insert example
        coVerify {
            mockDao.insertIntentExample(
                match<IntentExampleEntity> { entity ->
                    entity.exampleText == userText &&
                    entity.intentId == intentId &&
                    entity.source == source &&
                    entity.isPrimary == true && // First example should be primary
                    entity.locale == "en-US"
                }
            )
        }
    }

    /**
     * Test 2: saveLearnedExample with USER_CORRECTED source should persist to database
     */
    @Test
    fun `saveLearnedExample with USER_CORRECTED source should persist to database`() = runBlocking {
        // Given
        val userText = "turn on the lights"
        val intentId = "LIGHT_ON"
        val source = "USER_CORRECTED"

        // Mock DAO to return existing example for the intent
        val existingExample = IntentExampleEntity(
            exampleHash = "existing_hash",
            intentId = intentId,
            exampleText = "lights on please",
            isPrimary = true,
            source = "STATIC_JSON",
            locale = "en-US",
            createdAt = System.currentTimeMillis(),
            usageCount = 5,
            lastUsed = null
        )
        coEvery { mockDao.getAllExamplesOnce() } returns listOf(existingExample)

        // Mock successful insert (return row ID > 0)
        coEvery { mockDao.insertIntentExample(any()) } returns 2L

        // When
        val result = learningManager.saveLearnedExample(userText, intentId, source)

        // Then
        assertTrue(result, "saveLearnedExample should return true on success")

        // Verify DAO was called to insert example with USER_CORRECTED source
        coVerify {
            mockDao.insertIntentExample(
                match<IntentExampleEntity> { entity ->
                    entity.exampleText == userText &&
                    entity.intentId == intentId &&
                    entity.source == source &&
                    entity.isPrimary == false && // Not primary (intent already exists)
                    entity.locale == "en-US"
                }
            )
        }
    }

    /**
     * Test 3: saveLearnedExample with duplicate should not fail
     */
    @Test
    fun `saveLearnedExample with duplicate should not fail`() = runBlocking {
        // Given
        val userText = "hello ava"
        val intentId = "greeting"

        // Mock DAO to return empty list initially
        coEvery { mockDao.getAllExamplesOnce() } returns emptyList()

        // Mock insert to fail silently (return -1 for duplicate, as per Room behavior)
        // But IntentLearningManager should handle this gracefully
        coEvery { mockDao.insertIntentExample(any()) } returns -1L

        // When
        val result = learningManager.saveLearnedExample(userText, intentId, "USER_CONFIRMED")

        // Then
        // Should still return true (idempotent behavior)
        // The method logs warning but doesn't throw exception
        assertTrue(result, "saveLearnedExample should return true even for duplicates")

        // Verify DAO was called to attempt insert
        coVerify(exactly = 1) {
            mockDao.insertIntentExample(any())
        }
    }
}
