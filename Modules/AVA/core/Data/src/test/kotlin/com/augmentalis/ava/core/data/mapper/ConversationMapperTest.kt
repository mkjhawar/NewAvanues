package com.augmentalis.ava.core.data.mapper

import com.augmentalis.ava.core.data.entity.ConversationEntity
import com.augmentalis.ava.core.domain.model.Conversation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for ConversationMapper
 * P8 Week 3: Strategic coverage for Core Data mappers
 */
class ConversationMapperTest {

    // ========================================
    // Entity -> Domain (toConversation)
    // ========================================

    @Test
    fun `test toConversation with all fields`() {
        val entity = ConversationEntity(
            id = "conv-123",
            title = "Test Conversation",
            createdAt = 1000000L,
            updatedAt = 2000000L,
            messageCount = 5,
            isArchived = false,
            metadata = """{"key1":"value1","key2":"value2"}"""
        )

        val domain = entity.toConversation()

        assertEquals("conv-123", domain.id)
        assertEquals("Test Conversation", domain.title)
        assertEquals(1000000L, domain.createdAt)
        assertEquals(2000000L, domain.updatedAt)
        assertEquals(5, domain.messageCount)
        assertFalse(domain.isArchived)
        assertNotNull(domain.metadata)
        assertEquals("value1", domain.metadata!!["key1"])
        assertEquals("value2", domain.metadata!!["key2"])
    }

    @Test
    fun `test toConversation with null metadata`() {
        val entity = ConversationEntity(
            id = "conv-456",
            title = "No Metadata",
            createdAt = 1000000L,
            updatedAt = 2000000L,
            messageCount = 0,
            isArchived = true,
            metadata = null
        )

        val domain = entity.toConversation()

        assertEquals("conv-456", domain.id)
        assertEquals("No Metadata", domain.title)
        assertTrue(domain.isArchived)
        assertNull(domain.metadata)
    }

    @Test
    fun `test toConversation with archived conversation`() {
        val entity = ConversationEntity(
            id = "conv-archived",
            title = "Archived",
            createdAt = 500000L,
            updatedAt = 600000L,
            messageCount = 10,
            isArchived = true,
            metadata = null
        )

        val domain = entity.toConversation()

        assertTrue(domain.isArchived)
        assertEquals(10, domain.messageCount)
    }

    @Test
    fun `test toConversation with invalid JSON metadata returns null`() {
        val entity = ConversationEntity(
            id = "conv-bad-json",
            title = "Bad JSON",
            createdAt = 1000000L,
            updatedAt = 2000000L,
            messageCount = 3,
            isArchived = false,
            metadata = "not-valid-json"
        )

        val domain = entity.toConversation()

        assertEquals("conv-bad-json", domain.id)
        assertNull(domain.metadata)  // Invalid JSON should return null
    }

    @Test
    fun `test toConversation with empty JSON object metadata`() {
        val entity = ConversationEntity(
            id = "conv-empty",
            title = "Empty Metadata",
            createdAt = 1000000L,
            updatedAt = 2000000L,
            messageCount = 0,
            isArchived = false,
            metadata = "{}"
        )

        val domain = entity.toConversation()

        assertNotNull(domain.metadata)
        assertTrue(domain.metadata!!.isEmpty())
    }

    @Test
    fun `test toConversation preserves timestamps exactly`() {
        val createdAt = 123456789012345L
        val updatedAt = 987654321098765L

        val entity = ConversationEntity(
            id = "conv-timestamps",
            title = "Timestamp Test",
            createdAt = createdAt,
            updatedAt = updatedAt,
            messageCount = 1,
            isArchived = false,
            metadata = null
        )

        val domain = entity.toConversation()

        assertEquals(createdAt, domain.createdAt)
        assertEquals(updatedAt, domain.updatedAt)
    }

    // ========================================
    // Domain -> Entity (toEntity)
    // ========================================

    @Test
    fun `test toEntity with all fields`() {
        val domain = Conversation(
            id = "conv-789",
            title = "Domain Conversation",
            createdAt = 3000000L,
            updatedAt = 4000000L,
            messageCount = 15,
            isArchived = false,
            metadata = mapOf("author" to "user123", "topic" to "testing")
        )

        val entity = domain.toEntity()

        assertEquals("conv-789", entity.id)
        assertEquals("Domain Conversation", entity.title)
        assertEquals(3000000L, entity.createdAt)
        assertEquals(4000000L, entity.updatedAt)
        assertEquals(15, entity.messageCount)
        assertFalse(entity.isArchived)
        assertNotNull(entity.metadata)
        assertTrue(entity.metadata!!.contains("author"))
        assertTrue(entity.metadata!!.contains("user123"))
    }

    @Test
    fun `test toEntity with null metadata`() {
        val domain = Conversation(
            id = "conv-no-meta",
            title = "No Metadata Domain",
            createdAt = 5000000L,
            updatedAt = 6000000L,
            messageCount = 2,
            isArchived = true,
            metadata = null
        )

        val entity = domain.toEntity()

        assertEquals("conv-no-meta", entity.id)
        assertTrue(entity.isArchived)
        assertNull(entity.metadata)
    }

    @Test
    fun `test toEntity with default values`() {
        val domain = Conversation(
            id = "conv-defaults",
            title = "Defaults",
            createdAt = 1000L,
            updatedAt = 2000L
        )

        val entity = domain.toEntity()

        assertEquals(0, entity.messageCount)
        assertFalse(entity.isArchived)
        assertNull(entity.metadata)
    }

    // ========================================
    // Round-trip tests (Entity -> Domain -> Entity)
    // ========================================

    @Test
    fun `test round-trip entity to domain to entity preserves data`() {
        val original = ConversationEntity(
            id = "conv-roundtrip",
            title = "Round Trip",
            createdAt = 7000000L,
            updatedAt = 8000000L,
            messageCount = 20,
            isArchived = false,
            metadata = """{"test":"data"}"""
        )

        val domain = original.toConversation()
        val backToEntity = domain.toEntity()

        assertEquals(original.id, backToEntity.id)
        assertEquals(original.title, backToEntity.title)
        assertEquals(original.createdAt, backToEntity.createdAt)
        assertEquals(original.updatedAt, backToEntity.updatedAt)
        assertEquals(original.messageCount, backToEntity.messageCount)
        assertEquals(original.isArchived, backToEntity.isArchived)
        assertNotNull(backToEntity.metadata)
    }

    @Test
    fun `test round-trip domain to entity to domain preserves data`() {
        val original = Conversation(
            id = "conv-reverse",
            title = "Reverse Round Trip",
            createdAt = 9000000L,
            updatedAt = 10000000L,
            messageCount = 7,
            isArchived = true,
            metadata = mapOf("key" to "value")
        )

        val entity = original.toEntity()
        val backToDomain = entity.toConversation()

        assertEquals(original.id, backToDomain.id)
        assertEquals(original.title, backToDomain.title)
        assertEquals(original.createdAt, backToDomain.createdAt)
        assertEquals(original.updatedAt, backToDomain.updatedAt)
        assertEquals(original.messageCount, backToDomain.messageCount)
        assertEquals(original.isArchived, backToDomain.isArchived)
        assertEquals(original.metadata, backToDomain.metadata)
    }

    @Test
    fun `test round-trip with null metadata preserves null`() {
        val original = ConversationEntity(
            id = "conv-null-roundtrip",
            title = "Null Round Trip",
            createdAt = 1000L,
            updatedAt = 2000L,
            messageCount = 1,
            isArchived = false,
            metadata = null
        )

        val domain = original.toConversation()
        val backToEntity = domain.toEntity()

        assertNull(backToEntity.metadata)
    }

    // ========================================
    // Edge cases
    // ========================================

    @Test
    fun `test toConversation with very long title`() {
        val longTitle = "a".repeat(1000)
        val entity = ConversationEntity(
            id = "conv-long",
            title = longTitle,
            createdAt = 1000L,
            updatedAt = 2000L,
            messageCount = 0,
            isArchived = false,
            metadata = null
        )

        val domain = entity.toConversation()

        assertEquals(longTitle, domain.title)
    }

    @Test
    fun `test toEntity with empty metadata map`() {
        val domain = Conversation(
            id = "conv-empty-map",
            title = "Empty Map",
            createdAt = 1000L,
            updatedAt = 2000L,
            messageCount = 0,
            isArchived = false,
            metadata = emptyMap()
        )

        val entity = domain.toEntity()

        assertNotNull(entity.metadata)
        assertEquals("{}", entity.metadata)
    }

    @Test
    fun `test toConversation with complex metadata JSON`() {
        val entity = ConversationEntity(
            id = "conv-complex",
            title = "Complex Metadata",
            createdAt = 1000L,
            updatedAt = 2000L,
            messageCount = 0,
            isArchived = false,
            metadata = """{"user":"john","session":"abc123","device":"android","version":"1.0"}"""
        )

        val domain = entity.toConversation()

        assertNotNull(domain.metadata)
        assertEquals(4, domain.metadata!!.size)
        assertEquals("john", domain.metadata!!["user"])
        assertEquals("abc123", domain.metadata!!["session"])
        assertEquals("android", domain.metadata!!["device"])
        assertEquals("1.0", domain.metadata!!["version"])
    }

    @Test
    fun `test toEntity with special characters in metadata values`() {
        val domain = Conversation(
            id = "conv-special",
            title = "Special Chars",
            createdAt = 1000L,
            updatedAt = 2000L,
            messageCount = 0,
            isArchived = false,
            metadata = mapOf(
                "quote" to "He said \"hello\"",
                "newline" to "line1\nline2",
                "unicode" to "emoji 😀"
            )
        )

        val entity = domain.toEntity()

        assertNotNull(entity.metadata)

        // Round-trip to verify correct encoding
        val backToDomain = entity.toConversation()
        assertEquals(domain.metadata, backToDomain.metadata)
    }
}
