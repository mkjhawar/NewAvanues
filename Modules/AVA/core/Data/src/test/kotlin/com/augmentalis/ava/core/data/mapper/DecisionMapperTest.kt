package com.augmentalis.ava.core.data.mapper

import com.augmentalis.ava.core.data.entity.DecisionEntity
import com.augmentalis.ava.core.domain.model.Decision
import com.augmentalis.ava.core.domain.model.DecisionType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Tests for DecisionMapper
 * P8 Week 3: Strategic coverage for Core Data mappers
 */
class DecisionMapperTest {

    // ========================================
    // Entity -> Domain (toDomain)
    // ========================================

    @Test
    fun `test toDomain with INTENT_CLASSIFICATION type`() {
        val entity = DecisionEntity(
            id = "dec-123",
            conversationId = "conv-456",
            decisionType = "INTENT_CLASSIFICATION",
            inputData = """{"utterance":"hello"}""",
            outputData = """{"intent":"greeting","confidence":"0.95"}""",
            confidence = 0.95f,
            timestamp = 1000000L,
            reasoning = "High confidence greeting detected"
        )

        val domain = entity.toDomain()

        assertEquals("dec-123", domain.id)
        assertEquals("conv-456", domain.conversationId)
        assertEquals(DecisionType.INTENT_CLASSIFICATION, domain.decisionType)
        assertEquals("hello", domain.inputData["utterance"])
        assertEquals("greeting", domain.outputData["intent"])
        assertEquals(0.95f, domain.confidence)
        assertEquals(1000000L, domain.timestamp)
        assertEquals("High confidence greeting detected", domain.reasoning)
    }

    @Test
    fun `test toDomain with ACTION_SELECTION type`() {
        val entity = DecisionEntity(
            id = "dec-action",
            conversationId = "conv-action",
            decisionType = "ACTION_SELECTION",
            inputData = """{"intent":"show_time"}""",
            outputData = """{"action":"open_clock_app"}""",
            confidence = 0.88f,
            timestamp = 2000000L,
            reasoning = "Standard time intent mapping"
        )

        val domain = entity.toDomain()

        assertEquals(DecisionType.ACTION_SELECTION, domain.decisionType)
        assertEquals("show_time", domain.inputData["intent"])
        assertEquals("open_clock_app", domain.outputData["action"])
    }

    @Test
    fun `test toDomain with RESPONSE_GENERATION type`() {
        val entity = DecisionEntity(
            id = "dec-response",
            conversationId = "conv-response",
            decisionType = "RESPONSE_GENERATION",
            inputData = """{"intent":"greeting","context":"morning"}""",
            outputData = """{"response":"Good morning!"}""",
            confidence = 0.92f,
            timestamp = 3000000L,
            reasoning = null
        )

        val domain = entity.toDomain()

        assertEquals(DecisionType.RESPONSE_GENERATION, domain.decisionType)
        assertNotNull(domain.inputData)
        assertEquals("greeting", domain.inputData["intent"])
        assertEquals("morning", domain.inputData["context"])
        assertNull(domain.reasoning)
    }

    @Test
    fun `test toDomain with CONTEXT_RETRIEVAL type`() {
        val entity = DecisionEntity(
            id = "dec-context",
            conversationId = "conv-context",
            decisionType = "CONTEXT_RETRIEVAL",
            inputData = """{"query":"user preferences"}""",
            outputData = """{"context":"theme:dark,language:en"}""",
            confidence = 0.75f,
            timestamp = 4000000L,
            reasoning = "Retrieved from memory store"
        )

        val domain = entity.toDomain()

        assertEquals(DecisionType.CONTEXT_RETRIEVAL, domain.decisionType)
        assertEquals("user preferences", domain.inputData["query"])
    }

    @Test
    fun `test toDomain with MEMORY_RECALL type`() {
        val entity = DecisionEntity(
            id = "dec-memory",
            conversationId = "conv-memory",
            decisionType = "MEMORY_RECALL",
            inputData = """{"trigger":"name"}""",
            outputData = """{"recalled":"John"}""",
            confidence = 1.0f,
            timestamp = 5000000L,
            reasoning = "Exact match from long-term memory"
        )

        val domain = entity.toDomain()

        assertEquals(DecisionType.MEMORY_RECALL, domain.decisionType)
        assertEquals(1.0f, domain.confidence)
    }

    @Test
    fun `test toDomain with null reasoning`() {
        val entity = DecisionEntity(
            id = "dec-null",
            conversationId = "conv-null",
            decisionType = "INTENT_CLASSIFICATION",
            inputData = """{"test":"value"}""",
            outputData = """{"result":"value"}""",
            confidence = 0.5f,
            timestamp = 1000L,
            reasoning = null
        )

        val domain = entity.toDomain()

        assertNull(domain.reasoning)
    }

    @Test
    fun `test toDomain with empty JSON objects`() {
        val entity = DecisionEntity(
            id = "dec-empty",
            conversationId = "conv-empty",
            decisionType = "INTENT_CLASSIFICATION",
            inputData = "{}",
            outputData = "{}",
            confidence = 0.0f,
            timestamp = 1000L,
            reasoning = null
        )

        val domain = entity.toDomain()

        assertNotNull(domain.inputData)
        assertNotNull(domain.outputData)
        assertEquals(0, domain.inputData.size)
        assertEquals(0, domain.outputData.size)
    }

    @Test
    fun `test toDomain preserves confidence precision`() {
        val entity = DecisionEntity(
            id = "dec-precision",
            conversationId = "conv-precision",
            decisionType = "INTENT_CLASSIFICATION",
            inputData = "{}",
            outputData = "{}",
            confidence = 0.123456f,
            timestamp = 1000L,
            reasoning = null
        )

        val domain = entity.toDomain()

        assertEquals(0.123456f, domain.confidence)
    }

    // ========================================
    // Domain -> Entity (toEntity)
    // ========================================

    @Test
    fun `test toEntity with INTENT_CLASSIFICATION type`() {
        val domain = Decision(
            id = "dec-789",
            conversationId = "conv-987",
            decisionType = DecisionType.INTENT_CLASSIFICATION,
            inputData = mapOf("utterance" to "what time is it"),
            outputData = mapOf("intent" to "show_time", "confidence" to "0.99"),
            confidence = 0.99f,
            timestamp = 6000000L,
            reasoning = "Clear time-related query"
        )

        val entity = domain.toEntity()

        assertEquals("dec-789", entity.id)
        assertEquals("conv-987", entity.conversationId)
        assertEquals("INTENT_CLASSIFICATION", entity.decisionType)
        assertNotNull(entity.inputData)
        assertNotNull(entity.outputData)
        assertEquals(0.99f, entity.confidence)
        assertEquals(6000000L, entity.timestamp)
        assertEquals("Clear time-related query", entity.reasoning)
    }

    @Test
    fun `test toEntity with ACTION_SELECTION type`() {
        val domain = Decision(
            id = "dec-action2",
            conversationId = "conv-action2",
            decisionType = DecisionType.ACTION_SELECTION,
            inputData = mapOf("intent" to "set_alarm"),
            outputData = mapOf("action" to "launch_alarm_app"),
            confidence = 0.85f,
            timestamp = 7000000L,
            reasoning = null
        )

        val entity = domain.toEntity()

        assertEquals("ACTION_SELECTION", entity.decisionType)
        assertNull(entity.reasoning)
    }

    @Test
    fun `test toEntity with RESPONSE_GENERATION type`() {
        val domain = Decision(
            id = "dec-gen",
            conversationId = "conv-gen",
            decisionType = DecisionType.RESPONSE_GENERATION,
            inputData = mapOf("template" to "greeting", "name" to "Alice"),
            outputData = mapOf("response" to "Hello Alice!"),
            confidence = 0.95f,
            timestamp = 8000000L,
            reasoning = "Template-based generation"
        )

        val entity = domain.toEntity()

        assertEquals("RESPONSE_GENERATION", entity.decisionType)
        assertEquals("Template-based generation", entity.reasoning)
    }

    @Test
    fun `test toEntity with CONTEXT_RETRIEVAL type`() {
        val domain = Decision(
            id = "dec-ctx",
            conversationId = "conv-ctx",
            decisionType = DecisionType.CONTEXT_RETRIEVAL,
            inputData = mapOf("query" to "last conversation"),
            outputData = mapOf("context_id" to "conv-previous"),
            confidence = 0.70f,
            timestamp = 9000000L,
            reasoning = "Found 1 match in history"
        )

        val entity = domain.toEntity()

        assertEquals("CONTEXT_RETRIEVAL", entity.decisionType)
    }

    @Test
    fun `test toEntity with MEMORY_RECALL type`() {
        val domain = Decision(
            id = "dec-mem",
            conversationId = "conv-mem",
            decisionType = DecisionType.MEMORY_RECALL,
            inputData = mapOf("key" to "user_name"),
            outputData = mapOf("value" to "Bob"),
            confidence = 1.0f,
            timestamp = 10000000L,
            reasoning = "Exact key match"
        )

        val entity = domain.toEntity()

        assertEquals("MEMORY_RECALL", entity.decisionType)
    }

    @Test
    fun `test toEntity with empty maps`() {
        val domain = Decision(
            id = "dec-empty2",
            conversationId = "conv-empty2",
            decisionType = DecisionType.INTENT_CLASSIFICATION,
            inputData = emptyMap(),
            outputData = emptyMap(),
            confidence = 0.0f,
            timestamp = 1000L,
            reasoning = null
        )

        val entity = domain.toEntity()

        assertEquals("{}", entity.inputData)
        assertEquals("{}", entity.outputData)
    }

    @Test
    fun `test toEntity with complex nested data`() {
        val domain = Decision(
            id = "dec-complex",
            conversationId = "conv-complex",
            decisionType = DecisionType.INTENT_CLASSIFICATION,
            inputData = mapOf(
                "utterance" to "complex query",
                "context" to "previous context",
                "metadata" to "additional info"
            ),
            outputData = mapOf(
                "intent" to "result",
                "entities" to "entity1,entity2",
                "confidence" to "0.88"
            ),
            confidence = 0.88f,
            timestamp = 1000L,
            reasoning = "Complex reasoning"
        )

        val entity = domain.toEntity()

        assertNotNull(entity.inputData)
        assertNotNull(entity.outputData)
    }

    // ========================================
    // Round-trip tests
    // ========================================

    @Test
    fun `test round-trip entity to domain to entity preserves data`() {
        val original = DecisionEntity(
            id = "dec-roundtrip",
            conversationId = "conv-roundtrip",
            decisionType = "INTENT_CLASSIFICATION",
            inputData = """{"key1":"value1","key2":"value2"}""",
            outputData = """{"result":"success"}""",
            confidence = 0.85f,
            timestamp = 11000000L,
            reasoning = "Test reasoning"
        )

        val domain = original.toDomain()
        val backToEntity = domain.toEntity()

        assertEquals(original.id, backToEntity.id)
        assertEquals(original.conversationId, backToEntity.conversationId)
        assertEquals(original.decisionType, backToEntity.decisionType)
        assertEquals(original.confidence, backToEntity.confidence)
        assertEquals(original.timestamp, backToEntity.timestamp)
        assertEquals(original.reasoning, backToEntity.reasoning)
    }

    @Test
    fun `test round-trip domain to entity to domain preserves data`() {
        val original = Decision(
            id = "dec-reverse",
            conversationId = "conv-reverse",
            decisionType = DecisionType.ACTION_SELECTION,
            inputData = mapOf("intent" to "test"),
            outputData = mapOf("action" to "result"),
            confidence = 0.77f,
            timestamp = 12000000L,
            reasoning = "Reverse test"
        )

        val entity = original.toEntity()
        val backToDomain = entity.toDomain()

        assertEquals(original.id, backToDomain.id)
        assertEquals(original.conversationId, backToDomain.conversationId)
        assertEquals(original.decisionType, backToDomain.decisionType)
        assertEquals(original.inputData, backToDomain.inputData)
        assertEquals(original.outputData, backToDomain.outputData)
        assertEquals(original.confidence, backToDomain.confidence)
        assertEquals(original.timestamp, backToDomain.timestamp)
        assertEquals(original.reasoning, backToDomain.reasoning)
    }

    @Test
    fun `test round-trip preserves all DecisionType values`() {
        val types = listOf(
            DecisionType.INTENT_CLASSIFICATION,
            DecisionType.ACTION_SELECTION,
            DecisionType.RESPONSE_GENERATION,
            DecisionType.CONTEXT_RETRIEVAL,
            DecisionType.MEMORY_RECALL
        )

        types.forEach { type ->
            val domain = Decision(
                id = "dec-${type.name}",
                conversationId = "conv-type",
                decisionType = type,
                inputData = emptyMap(),
                outputData = emptyMap(),
                confidence = 0.5f,
                timestamp = 1000L,
                reasoning = null
            )

            val entity = domain.toEntity()
            val backToDomain = entity.toDomain()

            assertEquals(type, backToDomain.decisionType)
        }
    }

    @Test
    fun `test round-trip with null reasoning preserves null`() {
        val original = DecisionEntity(
            id = "dec-null-roundtrip",
            conversationId = "conv-null-roundtrip",
            decisionType = "MEMORY_RECALL",
            inputData = "{}",
            outputData = "{}",
            confidence = 0.5f,
            timestamp = 1000L,
            reasoning = null
        )

        val domain = original.toDomain()
        val backToEntity = domain.toEntity()

        assertNull(backToEntity.reasoning)
    }

    // ========================================
    // Edge cases
    // ========================================

    @Test
    fun `test toDomain with very long reasoning`() {
        val longReasoning = "x".repeat(5000)
        val entity = DecisionEntity(
            id = "dec-long",
            conversationId = "conv-long",
            decisionType = "INTENT_CLASSIFICATION",
            inputData = "{}",
            outputData = "{}",
            confidence = 0.5f,
            timestamp = 1000L,
            reasoning = longReasoning
        )

        val domain = entity.toDomain()

        assertEquals(longReasoning, domain.reasoning)
    }

    @Test
    fun `test toEntity with special characters in map values`() {
        val domain = Decision(
            id = "dec-special",
            conversationId = "conv-special",
            decisionType = DecisionType.INTENT_CLASSIFICATION,
            inputData = mapOf(
                "quote" to "He said \"hello\"",
                "newline" to "line1\nline2",
                "unicode" to "emoji 😀"
            ),
            outputData = mapOf(
                "result" to "success \"quoted\"",
                "special" to "tab\ttab"
            ),
            confidence = 0.5f,
            timestamp = 1000L,
            reasoning = "Reasoning with \"quotes\" and \n newlines"
        )

        val entity = domain.toEntity()

        // Round-trip to verify correct encoding
        val backToDomain = entity.toDomain()
        assertEquals(domain.inputData, backToDomain.inputData)
        assertEquals(domain.outputData, backToDomain.outputData)
        assertEquals(domain.reasoning, backToDomain.reasoning)
    }

    @Test
    fun `test toDomain preserves timestamp precision`() {
        val timestamp = 123456789012345L

        val entity = DecisionEntity(
            id = "dec-ts",
            conversationId = "conv-ts",
            decisionType = "INTENT_CLASSIFICATION",
            inputData = "{}",
            outputData = "{}",
            confidence = 0.5f,
            timestamp = timestamp,
            reasoning = null
        )

        val domain = entity.toDomain()

        assertEquals(timestamp, domain.timestamp)
    }

    @Test
    fun `test toEntity with zero confidence`() {
        val domain = Decision(
            id = "dec-zero",
            conversationId = "conv-zero",
            decisionType = DecisionType.INTENT_CLASSIFICATION,
            inputData = emptyMap(),
            outputData = emptyMap(),
            confidence = 0.0f,
            timestamp = 1000L,
            reasoning = null
        )

        val entity = domain.toEntity()

        assertEquals(0.0f, entity.confidence)
    }

    @Test
    fun `test toEntity with max confidence`() {
        val domain = Decision(
            id = "dec-max",
            conversationId = "conv-max",
            decisionType = DecisionType.INTENT_CLASSIFICATION,
            inputData = emptyMap(),
            outputData = emptyMap(),
            confidence = 1.0f,
            timestamp = 1000L,
            reasoning = null
        )

        val entity = domain.toEntity()

        assertEquals(1.0f, entity.confidence)
    }

    @Test
    fun `test toDomain with large data maps`() {
        val inputJson = buildString {
            append("{")
            for (i in 1..50) {
                append("\"key$i\":\"value$i\"")
                if (i < 50) append(",")
            }
            append("}")
        }

        val entity = DecisionEntity(
            id = "dec-large",
            conversationId = "conv-large",
            decisionType = "INTENT_CLASSIFICATION",
            inputData = inputJson,
            outputData = "{}",
            confidence = 0.5f,
            timestamp = 1000L,
            reasoning = null
        )

        val domain = entity.toDomain()

        assertEquals(50, domain.inputData.size)
        assertEquals("value1", domain.inputData["key1"])
        assertEquals("value50", domain.inputData["key50"])
    }
}
