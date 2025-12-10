package com.augmentalis.ava.core.data.integration

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.augmentalis.ava.core.data.db.AVADatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.system.measureTimeMillis

/**
 * Performance benchmarks for AVA database
 * Validates performance budgets from spec
 *
 * Performance Budgets (from spec):
 * - Insert 1000 messages: < 500ms
 * - Query conversation with 100 messages: < 100ms
 * - Database size with 1000 conversations: < 50MB
 * - Memory footprint: < 100MB peak
 *
 * Migrated to SQLDelight from Room
 */
@RunWith(AndroidJUnit4::class)
class PerformanceBenchmarkTest {

    private lateinit var database: AVADatabase
    private lateinit var driver: AndroidSqliteDriver

    @Before
    fun setup() {
        driver = AndroidSqliteDriver(
            schema = AVADatabase.Schema,
            context = ApplicationProvider.getApplicationContext(),
            name = null  // null = in-memory database
        )
        database = AVADatabase(driver)
    }

    @After
    fun teardown() {
        driver.close()
    }

    @Test
    fun benchmark_insert1000Messages_under500ms() = runTest {
        // Given - Conversation exists
        database.conversationQueries.insert(
            id = "conv-123",
            title = "Benchmark Test",
            created_at = 1000L,
            updated_at = 1000L,
            message_count = 0,
            is_archived = false,
            metadata = null
        )

        // When - Insert 1000 messages
        val duration = measureTimeMillis {
            repeat(1000) { i ->
                database.messageQueries.insert(
                    id = "msg-$i",
                    conversation_id = "conv-123",
                    role = if (i % 2 == 0) "USER" else "ASSISTANT",
                    content = "Message $i content",
                    timestamp = 1000L + i,
                    intent = null,
                    confidence = null,
                    metadata = null
                )
            }
        }

        // Then - Should complete in < 500ms
        println("⏱️ Insert 1000 messages: ${duration}ms")
        assertTrue("Insert 1000 messages took ${duration}ms (budget: 500ms)", duration < 500)
    }

    @Test
    fun benchmark_queryConversationWith100Messages_under100ms() = runTest {
        // Given - Conversation with 100 messages
        database.conversationQueries.insert(
            id = "conv-123",
            title = "Test",
            created_at = 1000L,
            updated_at = 1000L,
            message_count = 0,
            is_archived = false,
            metadata = null
        )

        repeat(100) { i ->
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

        // When - Query messages (composite index should help)
        val duration = measureTimeMillis {
            val messages = database.messageQueries.selectByConversationId("conv-123")
                .asFlow()
                .mapToList(Dispatchers.IO)
                .first()
            assertEquals(100, messages.size)
        }

        // Then - Should complete in < 100ms
        println("⏱️ Query conversation with 100 messages: ${duration}ms")
        assertTrue("Query took ${duration}ms (budget: 100ms)", duration < 100)
    }

    @Test
    fun benchmark_trainExampleHashLookup_under10ms() = runTest {
        // Given - 1000 training examples
        repeat(1000) { i ->
            database.trainExampleQueries.insert(
                example_hash = "hash-$i",
                utterance = "Utterance $i",
                intent = "intent_${i % 10}",
                locale = "en-US",
                source = "MANUAL",
                created_at = 1000L + i,
                usage_count = 0,
                last_used = null
            )
        }

        // When - Lookup by hash (unique index should help)
        val duration = measureTimeMillis {
            val example = database.trainExampleQueries.findByHash("hash-500").executeAsOneOrNull()
            assertNotNull(example)
        }

        // Then - Should complete in < 10ms (hash index is very fast)
        println("⏱️ Hash lookup in 1000 examples: ${duration}ms")
        assertTrue("Hash lookup took ${duration}ms (budget: 10ms)", duration < 10)
    }

    @Test
    fun benchmark_pagination_consistentPerformance() = runTest {
        // Given - Large conversation
        database.conversationQueries.insert(
            id = "conv-123",
            title = "Test",
            created_at = 1000L,
            updated_at = 1000L,
            message_count = 0,
            is_archived = false,
            metadata = null
        )

        repeat(1000) { i ->
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

        // When - Test pagination at different offsets
        val page1Duration = measureTimeMillis {
            val page1 = database.messageQueries.selectByConversationIdPaginated(
                conversation_id = "conv-123",
                limit = 20,
                offset = 0
            ).executeAsList()
            assertEquals(20, page1.size)
        }

        val page50Duration = measureTimeMillis {
            val page50 = database.messageQueries.selectByConversationIdPaginated(
                conversation_id = "conv-123",
                limit = 20,
                offset = 980
            ).executeAsList()
            assertEquals(20, page50.size)
        }

        // Then - Performance should be consistent (composite index helps)
        println("⏱️ Page 1 (offset 0): ${page1Duration}ms")
        println("⏱️ Page 50 (offset 980): ${page50Duration}ms")

        // Both should be under 50ms
        assertTrue("Page 1 took ${page1Duration}ms", page1Duration < 50)
        assertTrue("Page 50 took ${page50Duration}ms", page50Duration < 50)

        // Performance should not degrade significantly
        val degradation = (page50Duration - page1Duration).toFloat() / page1Duration
        assertTrue("Performance degraded by ${degradation * 100}%", degradation < 0.5)
    }

    @Test
    fun benchmark_searchConversations_under200ms() = runTest {
        // Given - 100 conversations with varied titles
        repeat(100) { i ->
            database.conversationQueries.insert(
                id = "conv-$i",
                title = "Conversation about ${listOf("Kotlin", "Android", "Testing", "Performance")[i % 4]} $i",
                created_at = 1000L + i,
                updated_at = 1000L + i,
                message_count = 0,
                is_archived = false,
                metadata = null
            )
        }

        // When - Search for keyword
        val duration = measureTimeMillis {
            val results = database.conversationQueries.searchByTitle("Kotlin")
                .asFlow()
                .mapToList(Dispatchers.IO)
                .first()
            assertTrue(results.size >= 20)  // Should find ~25 matches
        }

        // Then - Should complete in < 200ms
        println("⏱️ Search 100 conversations: ${duration}ms")
        assertTrue("Search took ${duration}ms (budget: 200ms)", duration < 200)
    }

    @Test
    fun benchmark_batchInsertTrainExamples_under1000ms() = runTest {
        // When - Insert 1000 training examples
        val duration = measureTimeMillis {
            repeat(1000) { i ->
                database.trainExampleQueries.insert(
                    example_hash = "hash-$i",
                    utterance = "Training utterance $i",
                    intent = "intent_${i % 20}",
                    locale = "en-US",
                    source = "AUTO_LEARN",
                    created_at = 1000L + i,
                    usage_count = 0,
                    last_used = null
                )
            }
        }

        // Then - Should complete in < 1000ms
        println("⏱️ Insert 1000 training examples: ${duration}ms")
        assertTrue("Batch insert took ${duration}ms (budget: 1000ms)", duration < 1000)

        // Verify all inserted
        val count = database.trainExampleQueries.count().executeAsOne()
        assertEquals(1000, count)
    }

    @Test
    fun benchmark_compositeIndexEffectiveness() = runTest {
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

        repeat(500) { i ->
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

        // When - Query using composite index (conversation_id, timestamp)
        val withIndexDuration = measureTimeMillis {
            val messages = database.messageQueries.selectByConversationId("conv-123")
                .asFlow()
                .mapToList(Dispatchers.IO)
                .first()
            assertEquals(500, messages.size)
        }

        // Then - Should be very fast due to composite index
        println("⏱️ Composite index query (500 messages): ${withIndexDuration}ms")
        assertTrue("Composite index query took ${withIndexDuration}ms (budget: 50ms)", withIndexDuration < 50)
    }
}
