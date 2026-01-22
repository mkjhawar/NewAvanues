package com.augmentalis.voiceavanue.access

import android.content.Context
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Comprehensive unit tests for DatabaseAccessFactory.
 *
 * Tests factory method behavior, feature flag handling, explicit creation methods,
 * context handling, and implementation verification.
 *
 * Strategy:
 * - Use Robolectric for Android Context
 * - Verify correct adapter types returned
 * - Test applicationContext usage
 * - Verify interface implementation
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class DatabaseAccessFactoryTest {

    private lateinit var applicationContext: Context
    private lateinit var mockActivityContext: Context

    @Before
    fun setup() {
        // Get Robolectric application context
        applicationContext = RuntimeEnvironment.getApplication().applicationContext

        // Create mock activity context with applicationContext
        mockActivityContext = mockk(relaxed = true)
        every { mockActivityContext.applicationContext } returns applicationContext
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    // ===== Feature Flag Selection Tests =====

    /**
     * Test: create() returns DatabaseDirectAdapter when USE_IPC_DATABASE = false
     *
     * Since USE_IPC_DATABASE is currently false (as per DatabaseConfig.kt),
     * the factory should return DatabaseDirectAdapter.
     */
    @Test
    fun `create returns DatabaseDirectAdapter when USE_IPC_DATABASE is false`() {
        // Given: DatabaseConfig.USE_IPC_DATABASE = false (default)

        // When: Creating database access via factory
        val database = DatabaseAccessFactory.create(applicationContext)

        // Then: Should return DatabaseDirectAdapter
        assertTrue(
            database is DatabaseDirectAdapter,
            "Expected DatabaseDirectAdapter when USE_IPC_DATABASE = false"
        )
    }

    /**
     * Test: Feature flag defaults to false (safe migration approach)
     *
     * Verifies that the default configuration is safe (direct access),
     * requiring explicit opt-in for IPC-based access.
     */
    @Test
    fun `feature flag defaults to false for safe migration`() {
        // Given: Fresh factory state

        // When: Creating database without explicit flag change
        val database = DatabaseAccessFactory.create(applicationContext)

        // Then: Should use safe default (direct adapter)
        assertTrue(
            database is DatabaseDirectAdapter,
            "Feature flag should default to false (DatabaseDirectAdapter)"
        )
    }

    /**
     * Test: Verify DatabaseConfig.USE_IPC_DATABASE constant value
     *
     * This test documents the current flag value and will fail if changed,
     * serving as a reminder to update related tests.
     */
    @Test
    fun `verify USE_IPC_DATABASE constant is false`() {
        // Given: DatabaseConfig constant

        // When: Reading flag value
        val flagValue = com.augmentalis.voiceavanue.config.DatabaseConfig.USE_IPC_DATABASE

        // Then: Should be false for safe migration
        assertFalse(
            flagValue,
            "USE_IPC_DATABASE should be false (safe default)"
        )
    }

    // ===== Explicit Creation Methods Tests =====

    /**
     * Test: createIpc() always returns DatabaseClientAdapter
     *
     * Explicit IPC creation should ignore the USE_IPC_DATABASE flag
     * and always return DatabaseClientAdapter.
     */
    @Test
    fun `createIpc always returns DatabaseClientAdapter`() {
        // Given: Factory with explicit IPC creation

        // When: Creating IPC database explicitly
        val database = DatabaseAccessFactory.createIpc(applicationContext)

        // Then: Should return DatabaseClientAdapter
        assertTrue(
            database is DatabaseClientAdapter,
            "createIpc() should always return DatabaseClientAdapter"
        )
    }

    /**
     * Test: createIpc() ignores USE_IPC_DATABASE flag
     *
     * Verifies that explicit creation methods bypass feature flag logic.
     */
    @Test
    fun `createIpc ignores USE_IPC_DATABASE flag`() {
        // Given: DatabaseConfig.USE_IPC_DATABASE = false

        // When: Creating IPC database explicitly
        val database = DatabaseAccessFactory.createIpc(applicationContext)

        // Then: Should return DatabaseClientAdapter despite flag being false
        assertTrue(
            database is DatabaseClientAdapter,
            "createIpc() should ignore USE_IPC_DATABASE flag"
        )
    }

    /**
     * Test: createDirect() always returns DatabaseDirectAdapter
     *
     * Explicit direct creation should ignore the USE_IPC_DATABASE flag
     * and always return DatabaseDirectAdapter.
     */
    @Test
    fun `createDirect always returns DatabaseDirectAdapter`() {
        // Given: Factory with explicit direct creation

        // When: Creating direct database explicitly
        val database = DatabaseAccessFactory.createDirect(applicationContext)

        // Then: Should return DatabaseDirectAdapter
        assertTrue(
            database is DatabaseDirectAdapter,
            "createDirect() should always return DatabaseDirectAdapter"
        )
    }

    /**
     * Test: createDirect() ignores USE_IPC_DATABASE flag
     *
     * Verifies that explicit creation methods bypass feature flag logic.
     */
    @Test
    fun `createDirect ignores USE_IPC_DATABASE flag`() {
        // Given: DatabaseConfig.USE_IPC_DATABASE = false (current state)

        // When: Creating direct database explicitly
        val database = DatabaseAccessFactory.createDirect(applicationContext)

        // Then: Should return DatabaseDirectAdapter
        assertTrue(
            database is DatabaseDirectAdapter,
            "createDirect() should ignore USE_IPC_DATABASE flag"
        )
    }

    // ===== Context Handling Tests =====

    /**
     * Test: Factory uses applicationContext, not activity context
     *
     * Verifies that the factory correctly converts activity context to
     * application context to avoid memory leaks.
     */
    @Test
    fun `factory uses applicationContext not activity context`() {
        // Given: Mock activity context with application context
        // (using mockActivityContext from setup)

        // When: Creating database with activity context
        DatabaseAccessFactory.create(mockActivityContext)

        // Then: Should access applicationContext
        verify(exactly = 1) { mockActivityContext.applicationContext }
    }

    /**
     * Test: createIpc uses applicationContext
     *
     * Verifies explicit IPC creation uses application context.
     */
    @Test
    fun `createIpc uses applicationContext`() {
        // Given: Mock activity context (from setup)

        // When: Creating IPC database explicitly
        DatabaseAccessFactory.createIpc(mockActivityContext)

        // Then: Should access applicationContext
        verify(exactly = 1) { mockActivityContext.applicationContext }
    }

    /**
     * Test: createDirect uses applicationContext
     *
     * Verifies explicit direct creation uses application context.
     */
    @Test
    fun `createDirect uses applicationContext`() {
        // Given: Mock activity context (from setup)

        // When: Creating direct database explicitly
        DatabaseAccessFactory.createDirect(mockActivityContext)

        // Then: Should access applicationContext
        verify(exactly = 1) { mockActivityContext.applicationContext }
    }

    // ===== Implementation Verification Tests =====

    /**
     * Test: Returned instances implement DatabaseAccess interface
     *
     * Verifies all factory methods return objects implementing the
     * DatabaseAccess interface.
     */
    @Test
    fun `all factory methods return DatabaseAccess implementations`() {
        // When: Creating databases via all factory methods
        val defaultDatabase = DatabaseAccessFactory.create(applicationContext)
        val ipcDatabase = DatabaseAccessFactory.createIpc(applicationContext)
        val directDatabase = DatabaseAccessFactory.createDirect(applicationContext)

        // Then: All should implement DatabaseAccess
        assertTrue(
            defaultDatabase is DatabaseAccess,
            "create() should return DatabaseAccess implementation"
        )
        assertTrue(
            ipcDatabase is DatabaseAccess,
            "createIpc() should return DatabaseAccess implementation"
        )
        assertTrue(
            directDatabase is DatabaseAccess,
            "createDirect() should return DatabaseAccess implementation"
        )
    }

    /**
     * Test: DatabaseClientAdapter instance is properly initialized
     *
     * Verifies that DatabaseClientAdapter instances can be used
     * for basic operations without throwing exceptions.
     */
    @Test
    fun `DatabaseClientAdapter instance is properly initialized`() = runTest {
        // Given: IPC database instance
        val database = DatabaseAccessFactory.createIpc(applicationContext)

        // When: Accessing instance
        // Then: Should not throw exceptions (basic smoke test)
        assertNotNull(database, "DatabaseClientAdapter should be initialized")
        assertTrue(
            database is DatabaseClientAdapter,
            "Should be DatabaseClientAdapter type"
        )
    }

    /**
     * Test: DatabaseDirectAdapter instance is properly initialized
     *
     * Verifies that DatabaseDirectAdapter instances can be used
     * for basic operations without throwing exceptions.
     */
    @Test
    fun `DatabaseDirectAdapter instance is properly initialized`() = runTest {
        // Given: Direct database instance
        val database = DatabaseAccessFactory.createDirect(applicationContext)

        // When: Accessing instance
        // Then: Should not throw exceptions (basic smoke test)
        assertNotNull(database, "DatabaseDirectAdapter should be initialized")
        assertTrue(
            database is DatabaseDirectAdapter,
            "Should be DatabaseDirectAdapter type"
        )
    }

    // ===== Edge Cases & Documentation Tests =====

    /**
     * Test: Factory is singleton object
     *
     * Verifies that DatabaseAccessFactory is an object (singleton)
     * and doesn't require instantiation.
     */
    @Test
    fun `factory is singleton object`() {
        // Given: Factory object

        // When: Accessing factory methods
        val db1 = DatabaseAccessFactory.create(applicationContext)
        val db2 = DatabaseAccessFactory.create(applicationContext)

        // Then: Both calls should succeed (factory doesn't need instantiation)
        assertNotNull(db1, "First factory call should succeed")
        assertNotNull(db2, "Second factory call should succeed")

        // Note: Each call creates new adapter instance (not singleton adapters)
        // Factory is singleton, adapters are not
    }

    /**
     * Test: Different adapter types have different characteristics
     *
     * Documents the key differences between IPC and direct adapters
     * for maintainability and understanding.
     */
    @Test
    fun `adapter types have expected characteristics`() {
        // Given: Both adapter types
        val ipcAdapter = DatabaseAccessFactory.createIpc(applicationContext)
        val directAdapter = DatabaseAccessFactory.createDirect(applicationContext)

        // When: Checking types
        val ipcClass = ipcAdapter::class.simpleName
        val directClass = directAdapter::class.simpleName

        // Then: Should have expected names
        assertEquals(
            "DatabaseClientAdapter",
            ipcClass,
            "IPC adapter should be DatabaseClientAdapter"
        )
        assertEquals(
            "DatabaseDirectAdapter",
            directClass,
            "Direct adapter should be DatabaseDirectAdapter"
        )
    }
}
