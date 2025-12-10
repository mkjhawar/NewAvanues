package com.augmentalis.ava.core.data.mapper

import com.augmentalis.ava.core.data.entity.MessageEntity
import com.augmentalis.ava.core.domain.model.Message
import com.augmentalis.ava.core.domain.model.MessageRole
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for MessageMapper
 * P8 Week 3: Strategic coverage for Core Data mappers
 */
class MessageMapperTest {

    // ========================================
    // Entity -> Domain (toMessage)
    // ========================================

    @Test
    fun `test toMessage with all fields for USER role`() {
        val entity = MessageEntity(
            id = "msg-123",
            conversationId = "conv-456",
            role = "USER",
            content = "Hello AVA",
            timestamp = 1000000L,
            intent = "greeting",
            confidence = 0.95f,
            metadata = """{"source":"voice","language":"en"}"""
        )

        val domain = entity.toMessage()

        assertEquals("msg-123", domain.id)
        assertEquals("conv-456", domain.conversationId)
        assertEquals(MessageRole.USER, domain.role)
        assertEquals("Hello AVA", domain.content)
        assertEquals(1000000L, domain.timestamp)
        assertEquals("greeting", domain.intent)
        assertEquals(0.95f, domain.confidence)
        assertNotNull(domain.metadata)
        assertEquals("voice", domain.metadata!!["source"])
        assertEquals("en", domain.metadata!!["language"])
    }

    @Test
    fun `test toMessage with ASSISTANT role`() {
        val entity = MessageEntity(
            id = "msg-assistant",
            conversationId = "conv-789",
            role = "ASSISTANT",
            content = "Hello! How can I help you?",
            timestamp = 2000000L,
            intent = null,
            confidence = null,
            metadata = null
        )

        val domain = entity.toMessage()

        assertEquals(MessageRole.ASSISTANT, domain.role)
        assertEquals("Hello! How can I help you?", domain.content)
        assertNull(domain.intent)
        assertNull(domain.confidence)
        assertNull(domain.metadata)
    }

    @Test
    fun `test toMessage with SYSTEM role`() {
        val entity = MessageEntity(
            id = "msg-system",
            conversationId = "conv-system",
            role = "SYSTEM",
            content = "System initialized",
            timestamp = 500000L,
            intent = null,
            confidence = null,
            metadata = """{"type":"init"}"""
        )

        val domain = entity.toMessage()

        assertEquals(MessageRole.SYSTEM, domain.role)
        assertEquals("System initialized", domain.content)
        assertNotNull(domain.metadata)
        assertEquals("init", domain.metadata!!["type"])
    }

    @Test
    fun `test toMessage with null optional fields`() {
        val entity = MessageEntity(
            id = "msg-minimal",
            conversationId = "conv-minimal",
            role = "USER",
            content = "Test",
            timestamp = 3000000L,
            intent = null,
            confidence = null,
            metadata = null
        )

        val domain = entity.toMessage()

        assertEquals("msg-minimal", domain.id)
        assertNull(domain.intent)
        assertNull(domain.confidence)
        assertNull(domain.metadata)
    }

    @Test
    fun `test toMessage with invalid JSON metadata returns null`() {
        val entity = MessageEntity(
            id = "msg-bad-json",
            conversationId = "conv-bad",
            role = "USER",
            content = "Test",
            timestamp = 1000L,
            intent = null,
            confidence = null,
            metadata = "invalid-json"
        )

        val domain = entity.toMessage()

        assertNull(domain.metadata)  // Invalid JSON should return null
    }

    @Test
    fun `test toMessage preserves confidence exactly`() {
        val entity = MessageEntity(
            id = "msg-conf",
            conversationId = "conv-conf",
            role = "USER",
            content = "Test",
            timestamp = 1000L,
            intent = "test",
            confidence = 0.123456f,
            metadata = null
        )

        val domain = entity.toMessage()

        assertEquals(0.123456f, domain.confidence)
    }

    @Test
    fun `test toMessage with zero confidence`() {
        val entity = MessageEntity(
            id = "msg-zero",
            conversationId = "conv-zero",
            role = "USER",
            content = "Test",
            timestamp = 1000L,
            intent = "test",
            confidence = 0.0f,
            metadata = null
        )

        val domain = entity.toMessage()

        assertEquals(0.0f, domain.confidence)
    }

    @Test
    fun `test toMessage with max confidence`() {
        val entity = MessageEntity(
            id = "msg-max",
            conversationId = "conv-max",
            role = "USER",
            content = "Test",
            timestamp = 1000L,
            intent = "test",
            confidence = 1.0f,
            metadata = null
        )

        val domain = entity.toMessage()

        assertEquals(1.0f, domain.confidence)
    }

    // ========================================
    // Domain -> Entity (toEntity)
    // ========================================

    @Test
    fun `test toEntity with all fields for USER role`() {
        val domain = Message(
            id = "msg-321",
            conversationId = "conv-654",
            role = MessageRole.USER,
            content = "What's the weather?",
            timestamp = 4000000L,
            intent = "check_weather",
            confidence = 0.88f,
            metadata = mapOf("location" to "NYC", "units" to "metric")
        )

        val entity = domain.toEntity()

        assertEquals("msg-321", entity.id)
        assertEquals("conv-654", entity.conversationId)
        assertEquals("USER", entity.role)
        assertEquals("What's the weather?", entity.content)
        assertEquals(4000000L, entity.timestamp)
        assertEquals("check_weather", entity.intent)
        assertEquals(0.88f, entity.confidence)
        assertNotNull(entity.metadata)
        assertTrue(entity.metadata!!.contains("location"))
        assertTrue(entity.metadata!!.contains("NYC"))
    }

    @Test
    fun `test toEntity with ASSISTANT role`() {
        val domain = Message(
            id = "msg-asst",
            conversationId = "conv-asst",
            role = MessageRole.ASSISTANT,
            content = "The weather is sunny",
            timestamp = 5000000L,
            intent = null,
            confidence = null,
            metadata = null
        )

        val entity = domain.toEntity()

        assertEquals("ASSISTANT", entity.role)
        assertEquals("The weather is sunny", entity.content)
        assertNull(entity.intent)
        assertNull(entity.confidence)
        assertNull(entity.metadata)
    }

    @Test
    fun `test toEntity with SYSTEM role`() {
        val domain = Message(
            id = "msg-sys",
            conversationId = "conv-sys",
            role = MessageRole.SYSTEM,
            content = "Error occurred",
            timestamp = 6000000L,
            intent = null,
            confidence = null,
            metadata = mapOf("error" to "timeout")
        )

        val entity = domain.toEntity()

        assertEquals("SYSTEM", entity.role)
        assertNotNull(entity.metadata)
        assertTrue(entity.metadata!!.contains("error"))
    }

    @Test
    fun `test toEntity with null optional fields`() {
        val domain = Message(
            id = "msg-null",
            conversationId = "conv-null",
            role = MessageRole.USER,
            content = "Simple message",
            timestamp = 7000000L,
            intent = null,
            confidence = null,
            metadata = null
        )

        val entity = domain.toEntity()

        assertNull(entity.intent)
        assertNull(entity.confidence)
        assertNull(entity.metadata)
    }

    @Test
    fun `test toEntity with empty metadata map`() {
        val domain = Message(
            id = "msg-empty",
            conversationId = "conv-empty",
            role = MessageRole.USER,
            content = "Test",
            timestamp = 1000L,
            intent = null,
            confidence = null,
            metadata = emptyMap()
        )

        val entity = domain.toEntity()

        assertNotNull(entity.metadata)
        assertEquals("{}", entity.metadata)
    }

    // ========================================
    // Round-trip tests
    // ========================================

    @Test
    fun `test round-trip entity to domain to entity preserves data`() {
        val original = MessageEntity(
            id = "msg-roundtrip",
            conversationId = "conv-roundtrip",
            role = "USER",
            content = "Round trip test",
            timestamp = 8000000L,
            intent = "test_intent",
            confidence = 0.75f,
            metadata = """{"test":"value"}"""
        )

        val domain = original.toMessage()
        val backToEntity = domain.toEntity()

        assertEquals(original.id, backToEntity.id)
        assertEquals(original.conversationId, backToEntity.conversationId)
        assertEquals(original.role, backToEntity.role)
        assertEquals(original.content, backToEntity.content)
        assertEquals(original.timestamp, backToEntity.timestamp)
        assertEquals(original.intent, backToEntity.intent)
        assertEquals(original.confidence, backToEntity.confidence)
        assertNotNull(backToEntity.metadata)
    }

    @Test
    fun `test round-trip domain to entity to domain preserves data`() {
        val original = Message(
            id = "msg-reverse",
            conversationId = "conv-reverse",
            role = MessageRole.ASSISTANT,
            content = "Response message",
            timestamp = 9000000L,
            intent = "response",
            confidence = 0.99f,
            metadata = mapOf("key1" to "val1", "key2" to "val2")
        )

        val entity = original.toEntity()
        val backToDomain = entity.toMessage()

        assertEquals(original.id, backToDomain.id)
        assertEquals(original.conversationId, backToDomain.conversationId)
        assertEquals(original.role, backToDomain.role)
        assertEquals(original.content, backToDomain.content)
        assertEquals(original.timestamp, backToDomain.timestamp)
        assertEquals(original.intent, backToDomain.intent)
        assertEquals(original.confidence, backToDomain.confidence)
        assertEquals(original.metadata, backToDomain.metadata)
    }

    @Test
    fun `test round-trip with null fields preserves nulls`() {
        val original = MessageEntity(
            id = "msg-null-roundtrip",
            conversationId = "conv-null-roundtrip",
            role = "SYSTEM",
            content = "Null test",
            timestamp = 1000L,
            intent = null,
            confidence = null,
            metadata = null
        )

        val domain = original.toMessage()
        val backToEntity = domain.toEntity()

        assertNull(backToEntity.intent)
        assertNull(backToEntity.confidence)
        assertNull(backToEntity.metadata)
    }

    @Test
    fun `test round-trip preserves all MessageRole values`() {
        val roles = listOf(MessageRole.USER, MessageRole.ASSISTANT, MessageRole.SYSTEM)

        roles.forEach { role ->
            val domain = Message(
                id = "msg-${role.name}",
                conversationId = "conv-role",
                role = role,
                content = "Role test",
                timestamp = 1000L,
                intent = null,
                confidence = null,
                metadata = null
            )

            val entity = domain.toEntity()
            val backToDomain = entity.toMessage()

            assertEquals(role, backToDomain.role)
        }
    }

    // ========================================
    // Edge cases
    // ========================================

    @Test
    fun `test toMessage with very long content`() {
        val longContent = "x".repeat(10000)
        val entity = MessageEntity(
            id = "msg-long",
            conversationId = "conv-long",
            role = "USER",
            content = longContent,
            timestamp = 1000L,
            intent = null,
            confidence = null,
            metadata = null
        )

        val domain = entity.toMessage()

        assertEquals(longContent, domain.content)
    }

    @Test
    fun `test toMessage with empty content`() {
        val entity = MessageEntity(
            id = "msg-empty",
            conversationId = "conv-empty",
            role = "USER",
            content = "",
            timestamp = 1000L,
            intent = null,
            confidence = null,
            metadata = null
        )

        val domain = entity.toMessage()

        assertEquals("", domain.content)
    }

    @Test
    fun `test toEntity with complex metadata`() {
        val domain = Message(
            id = "msg-complex",
            conversationId = "conv-complex",
            role = MessageRole.USER,
            content = "Complex test",
            timestamp = 1000L,
            intent = "test",
            confidence = 0.5f,
            metadata = mapOf(
                "user" to "john",
                "device" to "android",
                "version" to "1.0.0",
                "language" to "en-US",
                "timezone" to "America/New_York"
            )
        )

        val entity = domain.toEntity()

        assertNotNull(entity.metadata)

        // Round-trip to verify
        val backToDomain = entity.toMessage()
        assertEquals(domain.metadata, backToDomain.metadata)
    }

    @Test
    fun `test toEntity with special characters in content`() {
        val content = "Special: \"quotes\", 'apostrophes', \n newlines, \t tabs, 😀 emoji"
        val domain = Message(
            id = "msg-special",
            conversationId = "conv-special",
            role = MessageRole.USER,
            content = content,
            timestamp = 1000L,
            intent = null,
            confidence = null,
            metadata = null
        )

        val entity = domain.toEntity()

        assertEquals(content, entity.content)
    }

    @Test
    fun `test toMessage preserves timestamp precision`() {
        val timestamp = 123456789012345L

        val entity = MessageEntity(
            id = "msg-ts",
            conversationId = "conv-ts",
            role = "USER",
            content = "Timestamp test",
            timestamp = timestamp,
            intent = null,
            confidence = null,
            metadata = null
        )

        val domain = entity.toMessage()

        assertEquals(timestamp, domain.timestamp)
    }

    @Test
    fun `test toMessage with empty JSON object metadata`() {
        val entity = MessageEntity(
            id = "msg-empty-json",
            conversationId = "conv-empty-json",
            role = "USER",
            content = "Test",
            timestamp = 1000L,
            intent = null,
            confidence = null,
            metadata = "{}"
        )

        val domain = entity.toMessage()

        assertNotNull(domain.metadata)
        assertTrue(domain.metadata!!.isEmpty())
    }

    @Test
    fun `test toEntity with special characters in metadata values`() {
        val domain = Message(
            id = "msg-meta-special",
            conversationId = "conv-meta-special",
            role = MessageRole.USER,
            content = "Test",
            timestamp = 1000L,
            intent = null,
            confidence = null,
            metadata = mapOf(
                "quote" to "He said \"hello\"",
                "newline" to "line1\nline2",
                "unicode" to "emoji 😀 🎉",
                "escape" to "backslash \\ test"
            )
        )

        val entity = domain.toEntity()

        assertNotNull(entity.metadata)

        // Round-trip to verify correct encoding
        val backToDomain = entity.toMessage()
        assertEquals(domain.metadata, backToDomain.metadata)
    }
}
