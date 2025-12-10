package com.augmentalis.ava.core.data.migration

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.augmentalis.ava.core.data.db.AVADatabase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Database Migration Tests for AVA (SQLDelight)
 *
 * Tests database schema migrations to ensure:
 * - No data loss during upgrades
 * - Schema changes are applied correctly
 * - Foreign key constraints remain valid
 * - Indices are preserved
 *
 * CRITICAL: These tests prevent data loss in production!
 *
 * SQLDelight Migration Strategy:
 * ================================
 * Unlike Room (which uses Migration classes), SQLDelight uses .sqm (migration) files
 * located in the sqldelight folder alongside .sq schema files.
 *
 * Migration Files:
 * - Create files named: 1.sqm, 2.sqm, 3.sqm, etc. in:
 *   src/main/sqldelight/com/augmentalis/ava/core/data/db/
 *
 * - Each .sqm file contains SQL statements to migrate from version N to N+1
 * - SQLDelight automatically applies migrations in order
 *
 * Current Schema Version: 1
 * - Base schema defined in .sq files (Conversation.sq, Message.sq, etc.)
 * - No migrations exist yet (first version)
 *
 * How to Add Future Migrations:
 * 1. Create new .sqm file (e.g., 1.sqm for v1->v2)
 * 2. Add SQL migration statements (ALTER TABLE, CREATE TABLE, etc.)
 * 3. Update tests below to verify migration
 * 4. Set deriveSchemaFromMigrations = true in build.gradle.kts (already set)
 *
 * Example Migration File (1.sqm):
 * --------------------------------
 * -- Migration from version 1 to version 2
 * ALTER TABLE conversation ADD COLUMN tags TEXT;
 * CREATE INDEX idx_conversation_tags ON conversation(tags);
 *
 * Testing Strategy:
 * 1. Create database at old version
 * 2. Insert test data
 * 3. Verify migration to new version
 * 4. Verify data integrity and schema changes
 *
 * References:
 * - SQLDelight Migrations: https://cashapp.github.io/sqldelight/2.0.1/android_sqlite/migrations/
 *
 * Created: 2025-11-03
 * Updated: 2025-12-01 (Migrated from Room to SQLDelight)
 */
@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {

    companion object {
        private const val TEST_DB_NAME = "migration_test.db"
        private const val CURRENT_VERSION = 1
    }

    private lateinit var context: Context
    private lateinit var driver: SqlDriver
    private lateinit var database: AVADatabase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        // Clean up any existing test database
        context.deleteDatabase(TEST_DB_NAME)
    }

    @After
    fun tearDown() {
        if (::driver.isInitialized) {
            driver.close()
        }
        context.deleteDatabase(TEST_DB_NAME)
    }

    @Test
    fun database_creates_with_current_schema() = runTest {
        // Given - Clean database creation
        driver = AndroidSqliteDriver(
            schema = AVADatabase.Schema,
            context = context,
            name = TEST_DB_NAME
        )
        database = AVADatabase(driver)

        // When - Database is created
        val conversationQueries = database.conversationQueries

        // Then - Should be able to perform basic operations
        conversationQueries.insert(
            id = "conv-test",
            title = "Test Conversation",
            created_at = 1000L,
            updated_at = 1000L,
            message_count = 0L,
            is_archived = false,
            metadata = null
        )

        val conversation = conversationQueries.selectById("conv-test").executeAsOne()
        assertEquals("Test Conversation", conversation.title)
        assertEquals(0L, conversation.message_count)
        assertFalse(conversation.is_archived)
    }

    @Test
    fun database_preserves_foreign_key_constraints() = runTest {
        // Given - Database with foreign key relationships
        driver = AndroidSqliteDriver(
            schema = AVADatabase.Schema,
            context = context,
            name = TEST_DB_NAME
        )
        database = AVADatabase(driver)

        // Enable foreign keys (SQLDelight requires explicit enable)
        driver.execute(null, "PRAGMA foreign_keys=ON", 0)

        // Insert conversation
        database.conversationQueries.insert(
            id = "conv-123",
            title = "Test",
            created_at = 1000L,
            updated_at = 1000L,
            message_count = 0L,
            is_archived = false,
            metadata = null
        )

        // Insert message with valid FK
        database.messageQueries.insert(
            id = "msg-1",
            conversation_id = "conv-123",
            role = "USER",
            content = "Test message",
            timestamp_ = 1000L,
            model_id = null,
            prompt_tokens = null,
            completion_tokens = null,
            is_error = false,
            error_message = null,
            tool_calls = null
        )

        // When/Then - Foreign key constraint should be enforced
        try {
            database.messageQueries.insert(
                id = "msg-orphan",
                conversation_id = "nonexistent",
                role = "USER",
                content = "Orphan message",
                timestamp_ = 1000L,
                model_id = null,
                prompt_tokens = null,
                completion_tokens = null,
                is_error = false,
                error_message = null,
                tool_calls = null
            )
            fail("Should have thrown foreign key constraint exception")
        } catch (e: Exception) {
            // Expected - foreign key constraint violation
            assertTrue(
                "Should be FK constraint error: ${e.message}",
                e.message?.contains("FOREIGN KEY", ignoreCase = true) == true ||
                e.message?.contains("foreign key", ignoreCase = true) == true
            )
        }
    }

    @Test
    fun database_preserves_unique_constraints() = runTest {
        // Given - Database with unique constraints
        driver = AndroidSqliteDriver(
            schema = AVADatabase.Schema,
            context = context,
            name = TEST_DB_NAME
        )
        database = AVADatabase(driver)

        // Insert train example with unique hash
        database.trainExampleQueries.insert(
            example_hash = "hash123",
            utterance = "Open settings",
            intent = "open_app",
            locale = "en-US",
            source = "MANUAL",
            created_at = 1000L,
            usage_count = 0L
        )

        // When/Then - Unique constraint should be enforced
        try {
            database.trainExampleQueries.insert(
                example_hash = "hash123",
                utterance = "Open settings again",
                intent = "open_app",
                locale = "en-US",
                source = "MANUAL",
                created_at = 2000L,
                usage_count = 0L
            )
            fail("Should have thrown unique constraint exception")
        } catch (e: Exception) {
            // Expected - unique constraint violation
            assertTrue(
                "Should be unique constraint error: ${e.message}",
                e.message?.contains("UNIQUE", ignoreCase = true) == true ||
                e.message?.contains("unique", ignoreCase = true) == true
            )
        }
    }

    @Test
    fun database_preserves_indices() = runTest {
        // Given - Database with indices
        driver = AndroidSqliteDriver(
            schema = AVADatabase.Schema,
            context = context,
            name = TEST_DB_NAME
        )
        database = AVADatabase(driver)

        // Insert test conversations
        repeat(100) { i ->
            database.conversationQueries.insert(
                id = "conv-$i",
                title = "Chat $i",
                created_at = (1000 + i).toLong(),
                updated_at = (1000 + i).toLong(),
                message_count = i.toLong(),
                is_archived = false,
                metadata = null
            )
        }

        // When - Query using indexed column
        val cursor = driver.executeQuery(
            identifier = null,
            sql = "EXPLAIN QUERY PLAN SELECT * FROM conversation WHERE updated_at > 1050",
            parameters = 0
        )

        // Then - Verify index is used (not full table scan)
        val queryPlan = buildString {
            while (cursor.next()) {
                for (i in 0 until cursor.getLong(0)?.toInt() ?: 0) {
                    append(cursor.getString(i) ?: "").append(" ")
                }
                append("\n")
            }
        }

        assertTrue(
            "Query should use index, but got: $queryPlan",
            queryPlan.contains("INDEX", ignoreCase = true) ||
            queryPlan.contains("SEARCH", ignoreCase = true) ||
            queryPlan.contains("idx_conversation_updated_at", ignoreCase = true)
        )
    }

    @Test
    fun cascade_delete_works_correctly() = runTest {
        // Given - Database with cascade delete relationships
        driver = AndroidSqliteDriver(
            schema = AVADatabase.Schema,
            context = context,
            name = TEST_DB_NAME
        )
        database = AVADatabase(driver)

        // Enable foreign keys
        driver.execute(null, "PRAGMA foreign_keys=ON", 0)

        // Insert conversation
        database.conversationQueries.insert(
            id = "conv-123",
            title = "Test",
            created_at = 1000L,
            updated_at = 1000L,
            message_count = 0L,
            is_archived = false,
            metadata = null
        )

        // Insert messages
        database.messageQueries.insert(
            id = "msg-1",
            conversation_id = "conv-123",
            role = "USER",
            content = "Test 1",
            timestamp_ = 1100L,
            model_id = null,
            prompt_tokens = null,
            completion_tokens = null,
            is_error = false,
            error_message = null,
            tool_calls = null
        )

        database.messageQueries.insert(
            id = "msg-2",
            conversation_id = "conv-123",
            role = "USER",
            content = "Test 2",
            timestamp_ = 1200L,
            model_id = null,
            prompt_tokens = null,
            completion_tokens = null,
            is_error = false,
            error_message = null,
            tool_calls = null
        )

        // When - Delete conversation
        database.conversationQueries.delete("conv-123")

        // Then - Messages should be cascade deleted
        val messages = database.messageQueries.selectByConversationId("conv-123").executeAsList()
        assertEquals(0, messages.size)
    }

    @Test
    fun empty_database_opens_successfully() = runTest {
        // Given - Empty database
        driver = AndroidSqliteDriver(
            schema = AVADatabase.Schema,
            context = context,
            name = TEST_DB_NAME
        )
        database = AVADatabase(driver)

        // When/Then - Should open successfully with no errors
        val conversations = database.conversationQueries.selectAll().executeAsList()
        assertEquals(0, conversations.size)
    }

    @Test
    fun large_dataset_operations_perform_correctly() = runTest {
        // Given - Database with large dataset (stress test)
        driver = AndroidSqliteDriver(
            schema = AVADatabase.Schema,
            context = context,
            name = TEST_DB_NAME
        )
        database = AVADatabase(driver)

        // When - Insert 1000 conversations
        repeat(1000) { i ->
            database.conversationQueries.insert(
                id = "conv-$i",
                title = "Chat $i",
                created_at = (1000 + i).toLong(),
                updated_at = (1000 + i).toLong(),
                message_count = 0L,
                is_archived = false,
                metadata = null
            )
        }

        // Then - All data should be accessible
        val count = database.conversationQueries.count().executeAsOne()
        assertEquals(1000L, count)

        // Verify specific record
        val conv500 = database.conversationQueries.selectById("conv-500").executeAsOneOrNull()
        assertNotNull(conv500)
        assertEquals("Chat 500", conv500?.title)
    }

    @Test
    fun database_maintains_column_types() = runTest {
        // Given - Database with typed columns
        driver = AndroidSqliteDriver(
            schema = AVADatabase.Schema,
            context = context,
            name = TEST_DB_NAME
        )
        database = AVADatabase(driver)

        database.conversationQueries.insert(
            id = "conv-123",
            title = "Test",
            created_at = 1000L,
            updated_at = 1000L,
            message_count = 5L,
            is_archived = false,
            metadata = null
        )

        // When - Query schema information
        val cursor = driver.executeQuery(
            identifier = null,
            sql = "PRAGMA table_info(conversation)",
            parameters = 0
        )

        // Then - Verify column types
        val columnTypes = mutableMapOf<String, String>()
        while (cursor.next()) {
            val name = cursor.getString(1) // name column
            val type = cursor.getString(2) // type column
            if (name != null && type != null) {
                columnTypes[name] = type
            }
        }

        // Verify critical column types
        assertEquals("TEXT", columnTypes["id"])
        assertEquals("TEXT", columnTypes["title"])
        assertEquals("INTEGER", columnTypes["created_at"])
        assertEquals("INTEGER", columnTypes["updated_at"])
        assertEquals("INTEGER", columnTypes["message_count"])
        assertEquals("INTEGER", columnTypes["is_archived"]) // Boolean stored as INTEGER
    }

    // ============================================================
    // FUTURE MIGRATION TESTS
    // ============================================================
    // When adding schema version 2, uncomment and implement:

    /*
    @Test
    fun migrate_v1_to_v2_preserves_conversation_data() = runTest {
        // Given - Database at version 1 with data
        // 1. Create 1.sqm migration file in src/main/sqldelight/com/augmentalis/ava/core/data/db/
        // 2. Add migration SQL (e.g., ALTER TABLE conversation ADD COLUMN tags TEXT)
        // 3. Create driver with old schema
        // 4. Insert test data
        // 5. Verify migration preserves data

        // Create v1 database
        driver = AndroidSqliteDriver(
            schema = AVADatabase.Schema.createVersion(1),
            context = context,
            name = TEST_DB_NAME
        )
        database = AVADatabase(driver)

        // Insert test data
        database.conversationQueries.insert(
            id = "conv-123",
            title = "Test Chat",
            created_at = 1000L,
            updated_at = 1000L,
            message_count = 5L,
            is_archived = false,
            metadata = null
        )

        driver.close()

        // Migrate to v2
        driver = AndroidSqliteDriver(
            schema = AVADatabase.Schema,
            context = context,
            name = TEST_DB_NAME
        )
        database = AVADatabase(driver)

        // Verify data preserved
        val conversation = database.conversationQueries.selectById("conv-123").executeAsOne()
        assertEquals("Test Chat", conversation.title)
        assertEquals(5L, conversation.message_count)
    }
    */
}
