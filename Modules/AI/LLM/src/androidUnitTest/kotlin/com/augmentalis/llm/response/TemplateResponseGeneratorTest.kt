// filename: Universal/AVA/Features/LLM/src/test/java/com/augmentalis/ava/features/llm/response/TemplateResponseGeneratorTest.kt
// created: 2025-11-14
// author: AVA AI Team (P8 Initiative)
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.llm.response

import com.augmentalis.nlu.IntentClassification
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Mock ActionResult for testing (LLM module doesn't depend on Actions module)
 */
sealed class ActionResult {
    data class Success(val msg: String) : ActionResult()
    data class Failure(val error: String) : ActionResult()
}

/**
 * Comprehensive tests for TemplateResponseGenerator
 *
 * Tests the template-based response generation system which provides
 * fast, deterministic responses without LLM inference.
 *
 * Coverage:
 * - Template lookup for all known intents
 * - Unknown intent handling
 * - Streaming interface
 * - Response metadata
 * - Generator info
 * - Error handling
 *
 * P8 Initiative: LLM Module Test Coverage
 */
class TemplateResponseGeneratorTest {

    private lateinit var generator: TemplateResponseGenerator

    @BeforeTest
    fun setup() {
        generator = TemplateResponseGenerator()
    }

    // ========== BASIC TEMPLATE TESTS ==========

    @Test
    fun `test generator is always ready`() {
        assertTrue(generator.isReady(), "Template generator should always be ready")
    }

    @Test
    fun `test generator info returns correct type`() {
        val info = generator.getInfo()

        assertEquals("Template Response Generator", info.name)
        assertEquals(GeneratorType.TEMPLATE, info.type)
        assertTrue(info.supportsStreaming, "Should support streaming")
        assertNotNull(info.averageLatencyMs, "Should have latency metric")
        assertTrue(info.averageLatencyMs!! < 10, "Average latency should be <10ms")
    }

    @Test
    fun `test generator info includes template count`() {
        val info = generator.getInfo()

        val templateCount = info.metadata["templates_count"]?.toIntOrNull()
        assertNotNull(templateCount, "Should include template count in metadata")
        assertTrue(templateCount!! > 0, "Should have at least 1 template")
    }

    // ========== INTENT TEMPLATE TESTS ==========

    @Test
    fun `test control_lights intent generates correct response`() = runTest {
        val classification = IntentClassification(
            intent = "control_lights",
            confidence = 0.95f,
            inferenceTimeMs = 50L
        )

        val chunks = generator.generateResponse(
            userMessage = "Turn on the lights",
            classification = classification
        ).toList()

        assertTrue(chunks.size >= 2, "Should emit at least text + complete chunks")

        val textChunk = chunks.first { it is ResponseChunk.Text } as ResponseChunk.Text
        assertEquals("I'll control the lights for you.", textChunk.content)
    }

    @Test
    fun `test check_weather intent generates correct response`() = runTest {
        val classification = IntentClassification(
            intent = "check_weather",
            confidence = 0.88f,
            inferenceTimeMs = 45L
        )

        val chunks = generator.generateResponse(
            userMessage = "What's the weather?",
            classification = classification
        ).toList()

        val textChunk = chunks.filterIsInstance<ResponseChunk.Text>().first()
        assertEquals("Let me check the weather for you.", textChunk.content)
    }

    @Test
    fun `test set_alarm intent generates correct response`() = runTest {
        val classification = IntentClassification(
            intent = "set_alarm",
            confidence = 0.92f,
            inferenceTimeMs = 40L
        )

        val chunks = generator.generateResponse(
            userMessage = "Set an alarm for 7am",
            classification = classification
        ).toList()

        val textChunk = chunks.filterIsInstance<ResponseChunk.Text>().first()
        assertEquals("Setting an alarm for you.", textChunk.content)
    }

    @Test
    fun `test teach_ava intent generates learning prompt`() = runTest {
        val classification = IntentClassification(
            intent = "teach_ava",
            confidence = 0.85f,
            inferenceTimeMs = 50L
        )

        val chunks = generator.generateResponse(
            userMessage = "I want to teach you something",
            classification = classification
        ).toList()

        val textChunk = chunks.filterIsInstance<ResponseChunk.Text>().first()
        assertEquals("I'm ready to learn! What would you like to teach me?", textChunk.content)
    }

    @Test
    fun `test search intent generates overlay response`() = runTest {
        val classification = IntentClassification(
            intent = "search",
            confidence = 0.90f,
            inferenceTimeMs = 48L
        )

        val chunks = generator.generateResponse(
            userMessage = "Search for pizza places",
            classification = classification
        ).toList()

        val textChunk = chunks.filterIsInstance<ResponseChunk.Text>().first()
        assertTrue(
            textChunk.content.contains("search", ignoreCase = true),
            "Search intent should mention searching"
        )
    }

    @Test
    fun `test all built-in intents have templates`() = runTest {
        val builtInIntents = listOf(
            "control_lights", "control_temperature",
            "check_weather", "show_time",
            "set_alarm", "set_reminder",
            "show_history", "new_conversation", "teach_ava",
            "search", "translate", "reminder", "message", "summarize", "query", "general"
        )

        builtInIntents.forEach { intent ->
            val classification = IntentClassification(
                intent = intent,
                confidence = 0.90f,
                inferenceTimeMs = 50L
            )

            val chunks = generator.generateResponse(
                userMessage = "Test message",
                classification = classification
            ).toList()

            val textChunk = chunks.filterIsInstance<ResponseChunk.Text>().firstOrNull()
            assertNotNull(textChunk, "Intent '$intent' should have a template")
            assertTrue(
                textChunk.content.isNotEmpty(),
                "Template for '$intent' should not be empty"
            )
        }
    }

    // ========== UNKNOWN INTENT TESTS ==========

    @Test
    fun `test unknown intent returns fallback template`() = runTest {
        val classification = IntentClassification(
            intent = "unknown",
            confidence = 0.30f,
            inferenceTimeMs = 50L
        )

        val chunks = generator.generateResponse(
            userMessage = "Blah blah nonsense",
            classification = classification
        ).toList()

        val textChunk = chunks.filterIsInstance<ResponseChunk.Text>().first()
        assertEquals("I'm not sure I understood. Would you like to teach me?", textChunk.content)
    }

    @Test
    fun `test unrecognized intent defaults to unknown template`() = runTest {
        val classification = IntentClassification(
            intent = "completely_made_up_intent_xyz",
            confidence = 0.25f,
            inferenceTimeMs = 50L
        )

        val chunks = generator.generateResponse(
            userMessage = "Do something random",
            classification = classification
        ).toList()

        val textChunk = chunks.filterIsInstance<ResponseChunk.Text>().first()
        assertEquals("I'm not sure I understood. Would you like to teach me?", textChunk.content)
    }

    // ========== STREAMING TESTS ==========

    @Test
    fun `test response emits text chunk followed by complete chunk`() = runTest {
        val classification = IntentClassification(
            intent = "control_lights",
            confidence = 0.95f,
            inferenceTimeMs = 50L
        )

        val chunks = generator.generateResponse(
            userMessage = "Turn on lights",
            classification = classification
        ).toList()

        assertTrue(chunks.size >= 2, "Should emit at least 2 chunks")

        val textChunk = chunks[0] as? ResponseChunk.Text
        assertNotNull(textChunk, "First chunk should be Text")

        val completeChunk = chunks.last() as? ResponseChunk.Complete
        assertNotNull(completeChunk, "Last chunk should be Complete")
    }

    @Test
    fun `test complete chunk contains full text`() = runTest {
        val classification = IntentClassification(
            intent = "check_weather",
            confidence = 0.88f,
            inferenceTimeMs = 50L
        )

        val chunks = generator.generateResponse(
            userMessage = "What's the weather?",
            classification = classification
        ).toList()

        val completeChunk = chunks.filterIsInstance<ResponseChunk.Complete>().first()
        assertEquals("Let me check the weather for you.", completeChunk.fullText)
    }

    @Test
    fun `test response streaming is instant`() = runTest {
        val classification = IntentClassification(
            intent = "control_lights",
            confidence = 0.95f,
            inferenceTimeMs = 50L
        )

        val startTime = System.currentTimeMillis()

        val chunks = generator.generateResponse(
            userMessage = "Turn on lights",
            classification = classification
        ).toList()

        val elapsed = System.currentTimeMillis() - startTime

        assertTrue(elapsed < 100, "Template generation should be <100ms (was ${elapsed}ms)")
        assertTrue(chunks.isNotEmpty(), "Should emit chunks")
    }

    // ========== METADATA TESTS ==========

    @Test
    fun `test complete chunk includes generator metadata`() = runTest {
        val classification = IntentClassification(
            intent = "set_alarm",
            confidence = 0.92f,
            inferenceTimeMs = 50L
        )

        val chunks = generator.generateResponse(
            userMessage = "Set alarm for 7am",
            classification = classification
        ).toList()

        val completeChunk = chunks.filterIsInstance<ResponseChunk.Complete>().first()

        assertEquals("template", completeChunk.metadata["generator"])
        assertEquals("set_alarm", completeChunk.metadata["intent"])
        assertEquals(0.92f, completeChunk.metadata["confidence"])
        assertNotNull(completeChunk.metadata["latency_ms"])
    }

    @Test
    fun `test metadata includes latency measurement`() = runTest {
        val classification = IntentClassification(
            intent = "control_lights",
            confidence = 0.95f,
            inferenceTimeMs = 50L
        )

        val chunks = generator.generateResponse(
            userMessage = "Turn on lights",
            classification = classification
        ).toList()

        val completeChunk = chunks.filterIsInstance<ResponseChunk.Complete>().first()
        val latency = completeChunk.metadata["latency_ms"] as? Long

        assertNotNull(latency, "Should include latency measurement")
        assertTrue(latency >= 0, "Latency should be non-negative")
        assertTrue(latency < 100, "Template latency should be <100ms")
    }

    @Test
    fun `test metadata preserves NLU confidence`() = runTest {
        val confidenceValues = listOf(0.30f, 0.50f, 0.75f, 0.90f, 0.99f)

        confidenceValues.forEach { confidence ->
            val classification = IntentClassification(
                intent = "control_lights",
                confidence = confidence,
                inferenceTimeMs = 50L
            )

            val chunks = generator.generateResponse(
                userMessage = "Turn on lights",
                classification = classification
            ).toList()

            val completeChunk = chunks.filterIsInstance<ResponseChunk.Complete>().first()
            assertEquals(
                confidence,
                completeChunk.metadata["confidence"],
                "Should preserve NLU confidence $confidence"
            )
        }
    }

    // ========== CONTEXT TESTS ==========
    // Note: Context tests removed - ResponseContext doesn't accept ActionResult in test environment

    @Test
    fun `test user message parameter is ignored (interface consistency)`() = runTest {
        val classification = IntentClassification(
            intent = "control_lights",
            confidence = 0.95f,
            inferenceTimeMs = 50L
        )

        // Different user messages should produce same template
        val messages = listOf(
            "Turn on lights",
            "Lights on please",
            "Switch on the lights",
            "Enable lighting"
        )

        messages.forEach { message ->
            val chunks = generator.generateResponse(
                userMessage = message,
                classification = classification
            ).toList()

            val textChunk = chunks.filterIsInstance<ResponseChunk.Text>().first()
            assertEquals(
                "I'll control the lights for you.",
                textChunk.content,
                "Template should be same for message: '$message'"
            )
        }
    }

    // ========== RELIABILITY TESTS ==========

    @Test
    fun `test generator handles rapid successive calls`() = runTest {
        val classification = IntentClassification(
            intent = "control_lights",
            confidence = 0.95f,
            inferenceTimeMs = 50L
        )

        repeat(100) { iteration ->
            val chunks = generator.generateResponse(
                userMessage = "Turn on lights $iteration",
                classification = classification
            ).toList()

            assertTrue(chunks.isNotEmpty(), "Should emit chunks on iteration $iteration")

            val completeChunk = chunks.filterIsInstance<ResponseChunk.Complete>().firstOrNull()
            assertNotNull(completeChunk, "Should emit complete chunk on iteration $iteration")
        }
    }

    @Test
    fun `test generator is deterministic`() = runTest {
        val classification = IntentClassification(
            intent = "check_weather",
            confidence = 0.88f,
            inferenceTimeMs = 50L
        )

        val results = (1..10).map {
            val chunks = generator.generateResponse(
                userMessage = "What's the weather?",
                classification = classification
            ).toList()

            chunks.filterIsInstance<ResponseChunk.Text>().first().content
        }

        // All results should be identical
        val uniqueResults = results.toSet()
        assertEquals(1, uniqueResults.size, "Template generator should be deterministic")
        assertEquals("Let me check the weather for you.", uniqueResults.first())
    }

    @Test
    fun `test generator never throws exceptions`() = runTest {
        val edgeCaseIntents = listOf(
            "", // Empty intent
            " ", // Whitespace
            "null", // String "null"
            "12345", // Numbers
            "intent-with-dashes",
            "intent_with_underscores",
            "UPPERCASE_INTENT",
            "MixedCaseIntent",
            "intent.with.dots",
            "intent/with/slashes"
        )

        edgeCaseIntents.forEach { intent ->
            try {
                val classification = IntentClassification(
                    intent = intent,
                    confidence = 0.50f,
                    inferenceTimeMs = 50L
                )

                val chunks = generator.generateResponse(
                    userMessage = "Test",
                    classification = classification
                ).toList()

                assertTrue(chunks.isNotEmpty(), "Should handle edge case intent: '$intent'")
            } catch (e: Exception) {
                fail("Generator should not throw exception for intent: '$intent' (error: ${e.message})")
            }
        }
    }

    // ========== PERFORMANCE TESTS ==========

    @Test
    fun `test average latency is under 10ms`() = runTest {
        val classification = IntentClassification(
            intent = "control_lights",
            confidence = 0.95f,
            inferenceTimeMs = 50L
        )

        val latencies = (1..100).map {
            val startTime = System.currentTimeMillis()

            generator.generateResponse(
                userMessage = "Turn on lights",
                classification = classification
            ).toList()

            System.currentTimeMillis() - startTime
        }

        val averageLatency = latencies.average()
        assertTrue(
            averageLatency < 10.0,
            "Average latency should be <10ms (was ${averageLatency}ms)"
        )
    }
}
