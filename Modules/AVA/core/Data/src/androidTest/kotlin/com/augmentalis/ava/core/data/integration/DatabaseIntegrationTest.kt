package com.augmentalis.ava.core.data.integration

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.augmentalis.ava.core.data.db.AVADatabase
import com.augmentalis.ava.core.data.db.Conversation
import com.augmentalis.ava.core.data.db.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for AVA database
 * Tests end-to-end workflows across multiple SQLDelight queries
 *
 * Migrated from Room to SQLDelight
 */
@RunWith(AndroidJUnit4::class)
class DatabaseIntegrationTest {

    private lateinit var database: AVADatabase
    private lateinit var driver: AndroidSqliteDriver

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Create in-memory database for testing
        driver = AndroidSqliteDriver(
            schema = AVADatabase.Schema,
            context = context,
            name = null // null = in-memory database
        )
        database = AVADatabase(driver)
    }

    @After
    fun teardown() {
        driver.close()
    }

    @Test
    fun createConversationAndAddMessages_endToEndFlow() = runTest {
        // Given - Create a conversation
        database.conversationQueries.insert(
            id = "conv-123",
            title = "Integration Test Chat",
            created_at = 1000L,
            updated_at = 1000L,
            message_count = 0,
            is_archived = false,
            metadata = null
        )

        // When - Add multiple messages
        database.messageQueries.insert(
            id = "msg-1",
            conversation_id = "conv-123",
            role = "USER",
            content = "Hello AVA",
            timestamp = 1100L,
            intent = null,
            confidence = null,
            metadata = null
        )
        database.messageQueries.insert(
            id = "msg-2",
            conversation_id = "conv-123",
            role = "ASSISTANT",
            content = "Hello! How can I help?",
            timestamp = 1200L,
            intent = null,
            confidence = null,
            metadata = null
        )

        // Update conversation count
        database.conversationQueries.incrementMessageCount(id = "conv-123", updated_at = 1200L)
        database.conversationQueries.incrementMessageCount(id = "conv-123", updated_at = 1200L)

        // Then - Verify conversation updated
        val updatedConv = database.conversationQueries.selectById("conv-123").executeAsOneOrNull()
        assertNotNull(updatedConv)
        assertEquals(2L, updatedConv?.message_count)
        assertEquals(1200L, updatedConv?.updated_at)

        // Then - Verify messages retrievable
        val messages = database.messageQueries.selectByConversationId("conv-123")
            .asFlow()
            .mapToList(Dispatchers.IO)
            .first()
        assertEquals(2, messages.size)
        assertEquals("msg-1", messages[0].id)
        assertEquals("msg-2", messages[1].id)
    }

    @Test
    fun cascadeDelete_deletingConversationDeletesMessages() = runTest {
        // Given - Conversation with messages
        database.conversationQueries.insert(
            id = "conv-123",
            title = "Test",
            created_at = 1000L,
            updated_at = 1000L,
            message_count = 0,
            is_archived = false,
            metadata = null
        )
        database.messageQueries.insert(
            id = "msg-1",
            conversation_id = "conv-123",
            role = "USER",
            content = "Test message",
            timestamp = 1000L,
            intent = null,
            confidence = null,
            metadata = null
        )

        // When - Delete conversation
        database.conversationQueries.delete("conv-123")

        // Then - Messages should be cascade deleted
        val messages = database.messageQueries.selectByConversationId("conv-123")
            .asFlow()
            .mapToList(Dispatchers.IO)
            .first()
        assertTrue(messages.isEmpty())
    }

    @Test
    fun trainExampleDeduplication_preventsInsertingDuplicates() = runTest {
        // Given - Insert first example
        database.train_exampleQueries.insert(
            example_hash = "hash123",
            utterance = "Open settings",
            intent = "open_app",
            locale = "en-US",
            source = "MANUAL",
            created_at = 1000L,
            usage_count = 0,
            last_used = null
        )

        // When - Try to insert duplicate (same hash)
        var exceptionThrown = false
        try {
            database.train_exampleQueries.insert(
                example_hash = "hash123",
                utterance = "Open settings",
                intent = "open_app",
                locale = "en-US",
                source = "MANUAL",
                created_at = 2000L,
                usage_count = 0,
                last_used = null
            )
        } catch (e: Exception) {
            exceptionThrown = true
        }

        // Then - Duplicate insert should fail
        assertTrue("Unique constraint should prevent duplicate hash", exceptionThrown)

        // Verify only one example exists
        val allExamples = database.train_exampleQueries.selectAll()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .first()
        assertEquals(1, allExamples.size)
    }

    @Test
    fun multipleConversationsWithMessages_isolationTest() = runTest {
        // Given - Two conversations
        database.conversationQueries.insert(
            id = "conv-1",
            title = "Chat 1",
            created_at = 1000L,
            updated_at = 1000L,
            message_count = 0,
            is_archived = false,
            metadata = null
        )
        database.conversationQueries.insert(
            id = "conv-2",
            title = "Chat 2",
            created_at = 2000L,
            updated_at = 2000L,
            message_count = 0,
            is_archived = false,
            metadata = null
        )

        // When - Add messages to each
        database.messageQueries.insert(
            id = "msg-1",
            conversation_id = "conv-1",
            role = "USER",
            content = "Message in chat 1",
            timestamp = 1000L,
            intent = null,
            confidence = null,
            metadata = null
        )
        database.messageQueries.insert(
            id = "msg-2",
            conversation_id = "conv-2",
            role = "USER",
            content = "Message in chat 2",
            timestamp = 2000L,
            intent = null,
            confidence = null,
            metadata = null
        )

        // Then - Messages properly isolated
        val conv1Messages = database.messageQueries.selectByConversationId("conv-1")
            .asFlow()
            .mapToList(Dispatchers.IO)
            .first()
        val conv2Messages = database.messageQueries.selectByConversationId("conv-2")
            .asFlow()
            .mapToList(Dispatchers.IO)
            .first()

        assertEquals(1, conv1Messages.size)
        assertEquals(1, conv2Messages.size)
        assertEquals("msg-1", conv1Messages[0].id)
        assertEquals("msg-2", conv2Messages[0].id)
    }

    @Test
    fun pagination_returnsCorrectSubset() = runTest {
        // Given - Conversation with 20 messages
        database.conversationQueries.insert(
            id = "conv-123",
            title = "Test",
            created_at = 1000L,
            updated_at = 1000L,
            message_count = 0,
            is_archived = false,
            metadata = null
        )

        repeat(20) { i ->
            database.messageQueries.insert(
                id = "msg-$i",
                conversation_id = "conv-123",
                role = "USER",
                content = "Message $i",
                timestamp = 1000L + i,
                intent = null,
                confidence = null,
                metadata = null
            )
        }

        // When - Request pages
        val page1 = database.messageQueries.selectByConversationIdPaginated(
            conversation_id = "conv-123",
            limit = 5,
            offset = 0
        ).executeAsList()
        val page2 = database.messageQueries.selectByConversationIdPaginated(
            conversation_id = "conv-123",
            limit = 5,
            offset = 5
        ).executeAsList()
        val page3 = database.messageQueries.selectByConversationIdPaginated(
            conversation_id = "conv-123",
            limit = 5,
            offset = 10
        ).executeAsList()

        // Then - Each page has 5 messages
        assertEquals(5, page1.size)
        assertEquals(5, page2.size)
        assertEquals(5, page3.size)

        // Then - Pages are different
        assertNotEquals(page1[0].id, page2[0].id)
        assertNotEquals(page2[0].id, page3[0].id)
    }

    @Test
    fun foreignKeyEnforcement_preventsOrphanMessages() = runTest {
        // Given - No conversation exists
        // When/Then - Inserting message with invalid FK should fail
        var exceptionThrown = false
        try {
            database.messageQueries.insert(
                id = "msg-1",
                conversation_id = "nonexistent-conv",
                role = "USER",
                content = "Orphan message",
                timestamp = 1000L,
                intent = null,
                confidence = null,
                metadata = null
            )
        } catch (e: Exception) {
            exceptionThrown = true
        }

        assertTrue("Foreign key constraint should prevent orphan messages", exceptionThrown)
    }
}
