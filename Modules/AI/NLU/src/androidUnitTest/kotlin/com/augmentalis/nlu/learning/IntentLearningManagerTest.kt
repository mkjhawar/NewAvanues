// filename: Universal/AVA/Features/NLU/src/androidUnitTest/kotlin/com/augmentalis/nlu/learning/IntentLearningManagerTest.kt
// created: 2025-11-25
// updated: 2025-12-18 (migrated from Room to SQLDelight)
// author: Testing Swarm Agent 1 - AVA AI Features 003 + 004
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.nlu.learning

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Unit tests for IntentLearningManager (REQ-004)
 *
 * Tests intent hint extraction and response cleaning (pure functions).
 *
 * Note: Database operations require instrumented tests due to SQLDelight driver.
 * These unit tests focus on the non-database logic.
 *
 * Uses Robolectric + MockK for Android unit testing.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class IntentLearningManagerTest {

    private lateinit var context: Context
    private lateinit var learningManager: IntentLearningManager

    @Before
    fun setup() {
        // Mock Android Context
        context = mockk(relaxed = true)

        // Mock external files directory for model path
        val mockFileDir = mockk<java.io.File>(relaxed = true) {
            every { absolutePath } returns "/mock/path"
        }
        every { context.getExternalFilesDir(null) } returns mockFileDir

        // Create IntentLearningManager instance
        learningManager = IntentLearningManager(context)
    }

    // ==================== Intent Hint Extraction Tests ====================

    /**
     * Test extractIntentHint with valid markers
     */
    @Test
    fun `extractIntentHint should extract intent and confidence from response`() {
        // Given
        val llmResponse = "Hello! How can I help you? [INTENT: greeting] [CONFIDENCE: 95]"

        // When
        val hint = learningManager.extractIntentHint(llmResponse)

        // Then
        assertNotNull(hint)
        assertEquals("greeting", hint.intentName)
        assertEquals(95, hint.confidence)
    }

    /**
     * Test extractIntentHint with missing markers
     */
    @Test
    fun `extractIntentHint should return null when no markers present`() {
        // Given
        val llmResponse = "Hello! How can I help you?"

        // When
        val hint = learningManager.extractIntentHint(llmResponse)

        // Then
        assertNull(hint)
    }

    /**
     * Test extractIntentHint with only intent marker
     */
    @Test
    fun `extractIntentHint should return null when only intent marker present`() {
        // Given
        val llmResponse = "Hello! [INTENT: greeting]"

        // When
        val hint = learningManager.extractIntentHint(llmResponse)

        // Then
        assertNull(hint)
    }

    /**
     * Test extractIntentHint with only confidence marker
     */
    @Test
    fun `extractIntentHint should return null when only confidence marker present`() {
        // Given
        val llmResponse = "Hello! [CONFIDENCE: 95]"

        // When
        val hint = learningManager.extractIntentHint(llmResponse)

        // Then
        assertNull(hint)
    }

    /**
     * Test extractIntentHint with different intents
     */
    @Test
    fun `extractIntentHint should work with various intent names`() {
        val testCases = listOf(
            "test [INTENT: show_time] [CONFIDENCE: 80]" to ("show_time" to 80),
            "test [INTENT: set_alarm] [CONFIDENCE: 100]" to ("set_alarm" to 100),
            "test [INTENT: check_weather] [CONFIDENCE: 50]" to ("check_weather" to 50)
        )

        testCases.forEach { (response, expected) ->
            val hint = learningManager.extractIntentHint(response)
            assertNotNull(hint, "Expected hint for: $response")
            assertEquals(expected.first, hint.intentName)
            assertEquals(expected.second, hint.confidence)
        }
    }

    // ==================== Clean Response Tests ====================

    /**
     * Test cleanResponse removes markers
     */
    @Test
    fun `cleanResponse should remove intent markers from response`() {
        // Given
        val llmResponse = "Hello! How can I help you? [INTENT: greeting] [CONFIDENCE: 95]"

        // When
        val cleaned = learningManager.cleanResponse(llmResponse)

        // Then
        assertEquals("Hello! How can I help you?", cleaned)
    }

    /**
     * Test cleanResponse with no markers
     */
    @Test
    fun `cleanResponse should preserve response without markers`() {
        // Given
        val llmResponse = "Hello! How can I help you?"

        // When
        val cleaned = learningManager.cleanResponse(llmResponse)

        // Then
        assertEquals("Hello! How can I help you?", cleaned)
    }

    /**
     * Test cleanResponse with markers in middle
     */
    @Test
    fun `cleanResponse should handle markers in middle of text`() {
        // Given
        val llmResponse = "Hello! [INTENT: greeting] How can I help? [CONFIDENCE: 95]"

        // When
        val cleaned = learningManager.cleanResponse(llmResponse)

        // Then
        assertEquals("Hello!  How can I help?", cleaned)
    }

    /**
     * Test cleanResponse with multiple markers
     */
    @Test
    fun `cleanResponse should handle multiple markers`() {
        // Given - although unusual, should handle gracefully
        val llmResponse = "Hello! [INTENT: greeting] [INTENT: unknown] [CONFIDENCE: 95] [CONFIDENCE: 50]"

        // When
        val cleaned = learningManager.cleanResponse(llmResponse)

        // Then - should remove all markers
        assertEquals("Hello!", cleaned)
    }

    // ==================== Edge Cases ====================

    /**
     * Test with empty response
     */
    @Test
    fun `extractIntentHint should handle empty response`() {
        assertNull(learningManager.extractIntentHint(""))
    }

    /**
     * Test with whitespace response
     */
    @Test
    fun `cleanResponse should trim whitespace`() {
        val response = "   Hello!   [INTENT: greeting] [CONFIDENCE: 90]   "
        val cleaned = learningManager.cleanResponse(response)
        assertEquals("Hello!", cleaned)
    }

    /**
     * Test with zero confidence
     */
    @Test
    fun `extractIntentHint should handle zero confidence`() {
        val response = "test [INTENT: unknown] [CONFIDENCE: 0]"
        val hint = learningManager.extractIntentHint(response)

        assertNotNull(hint)
        assertEquals("unknown", hint.intentName)
        assertEquals(0, hint.confidence)
    }

    /**
     * Test with high confidence (100)
     */
    @Test
    fun `extractIntentHint should handle max confidence`() {
        val response = "test [INTENT: greeting] [CONFIDENCE: 100]"
        val hint = learningManager.extractIntentHint(response)

        assertNotNull(hint)
        assertEquals(100, hint.confidence)
    }
}
