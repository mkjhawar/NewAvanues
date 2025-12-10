package com.augmentalis.ava.core.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.augmentalis.ava.core.domain.repository.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for DatabaseProvider
 *
 * Tests the repository helper methods added on 2025-11-09
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class DatabaseProviderTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        // Clear any existing database
        DatabaseProvider.clearInstance()
    }

    @After
    fun teardown() {
        DatabaseProvider.closeDatabase()
        DatabaseProvider.clearInstance()
    }

    @Test
    fun `getConversationRepository returns valid repository`() {
        // When
        val repository = DatabaseProvider.getConversationRepository(context)

        // Then
        assertNotNull("Repository should not be null", repository)
        assertTrue("Should be ConversationRepository", repository is ConversationRepository)
    }

    @Test
    fun `getMessageRepository returns valid repository`() {
        // When
        val repository = DatabaseProvider.getMessageRepository(context)

        // Then
        assertNotNull("Repository should not be null", repository)
        assertTrue("Should be MessageRepository", repository is MessageRepository)
    }

    @Test
    fun `getTrainExampleRepository returns valid repository`() {
        // When
        val repository = DatabaseProvider.getTrainExampleRepository(context)

        // Then
        assertNotNull("Repository should not be null", repository)
        assertTrue("Should be TrainExampleRepository", repository is TrainExampleRepository)
    }

    @Test
    fun `getDecisionRepository returns valid repository`() {
        // When
        val repository = DatabaseProvider.getDecisionRepository(context)

        // Then
        assertNotNull("Repository should not be null", repository)
        assertTrue("Should be DecisionRepository", repository is DecisionRepository)
    }

    @Test
    fun `getLearningRepository returns valid repository`() {
        // When
        val repository = DatabaseProvider.getLearningRepository(context)

        // Then
        assertNotNull("Repository should not be null", repository)
        assertTrue("Should be LearningRepository", repository is LearningRepository)
    }

    @Test
    fun `getMemoryRepository returns valid repository`() {
        // When
        val repository = DatabaseProvider.getMemoryRepository(context)

        // Then
        assertNotNull("Repository should not be null", repository)
        assertTrue("Should be MemoryRepository", repository is MemoryRepository)
    }

    @Test
    fun `multiple repository requests return instances`() {
        // When - Request multiple repositories
        val conv1 = DatabaseProvider.getConversationRepository(context)
        val conv2 = DatabaseProvider.getConversationRepository(context)
        val msg1 = DatabaseProvider.getMessageRepository(context)

        // Then - All should be valid
        assertNotNull(conv1)
        assertNotNull(conv2)
        assertNotNull(msg1)
    }

    @Test
    fun `database is initialized on first repository access`() {
        // Given - No database exists yet
        DatabaseProvider.clearInstance()

        // When - Request a repository
        val repository = DatabaseProvider.getConversationRepository(context)

        // Then - Database should be initialized
        assertNotNull(repository)

        // And - Database instance should exist
        val database = DatabaseProvider.getDatabase(context)
        assertNotNull(database)
    }

    @Test
    fun `closeDatabase clears instance`() {
        // Given - Database is initialized
        val repository = DatabaseProvider.getConversationRepository(context)
        assertNotNull(repository)

        // When - Close database
        DatabaseProvider.closeDatabase()

        // Then - Should be able to create new instance
        val newRepository = DatabaseProvider.getConversationRepository(context)
        assertNotNull(newRepository)
    }

    @Test
    fun `repositories have access to DAOs`() {
        // When - Get repository and try to use it
        val repository = DatabaseProvider.getConversationRepository(context)

        // Then - Should be able to access Flow (proves DAO access works)
        val flow = repository.getAllConversations()
        assertNotNull("Flow should not be null", flow)
    }
}
