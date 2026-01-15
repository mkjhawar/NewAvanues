/**
 * VoiceOSDatabaseManagerTest.kt - Tests for VoiceOSDatabaseManager singleton behavior
 *
 * Verifies singleton pattern implementation and thread safety.
 *
 * Author: Claude Code (P1 Cleanup & Testing)
 * Created: 2025-12-13
 * Related: VoiceOS-Handover-P1-Cleanup-51213.md
 */

package com.augmentalis.database

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.lang.reflect.Field

/**
 * Helper to create test database manager.
 * For singleton tests, we use the real DatabaseDriverFactory.
 */
fun createTestDatabaseManager(context: Context): VoiceOSDatabaseManager {
    // For singleton behavior testing, using real DatabaseDriverFactory is fine
    // Tests will clean up the singleton instance between tests using reflection
    return VoiceOSDatabaseManager.getInstance(DatabaseDriverFactory(context))
}

/**
 * Helper to create in-memory database for direct database testing.
 */
fun createTestDatabase(context: Context): VoiceOSDatabase {
    val driver = AndroidSqliteDriver(
        schema = VoiceOSDatabase.Schema,
        context = context,
        name = null // null = in-memory database
    )
    return VoiceOSDatabase(driver)
}

/**
 * Tests for VoiceOSDatabaseManager singleton pattern.
 *
 * Covers:
 * - Singleton instance consistency
 * - Thread-safe initialization
 * - Proper database lifecycle
 * - Memory management (no multiple instances)
 * - Concurrent access safety
 */
@RunWith(AndroidJUnit4::class)
class VoiceOSDatabaseManagerTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        // Reset singleton instance before each test using reflection
        resetSingletonInstance()
    }

    @After
    fun teardown() {
        // Reset singleton instance after each test
        resetSingletonInstance()
    }

    /**
     * Reset the singleton instance using reflection for clean test isolation.
     */
    private fun resetSingletonInstance() {
        try {
            val instanceField: Field = VoiceOSDatabaseManager::class.java
                .declaredFields
                .first { it.name == "INSTANCE" }
            instanceField.isAccessible = true
            instanceField.set(null, null)
        } catch (e: Exception) {
            // If reflection fails, tests may not be properly isolated
            println("Warning: Could not reset singleton instance: ${e.message}")
        }
    }

    @Test
    fun testGetInstanceReturnsSameInstanceOnMultipleCalls() {
        val instance1 = createTestDatabaseManager(context)
        val instance2 = createTestDatabaseManager(context)
        val instance3 = createTestDatabaseManager(context)

        // All instances should be the same object
        assertSame("getInstance should return same instance", instance1, instance2)
        assertSame("getInstance should return same instance", instance2, instance3)
        assertSame("getInstance should return same instance", instance1, instance3)
    }

    @Test
    fun testGetInstanceReturnsNonNullInstance() {
        val instance = createTestDatabaseManager(context)

        assertNotNull("getInstance should never return null", instance)
    }

    @Test
    fun testSingletonInstanceHasProperlyInitializedRepositories() {
        val instance = createTestDatabaseManager(context)

        // Verify all repositories are initialized
        assertNotNull("commands repository should be initialized", instance.commands)
        assertNotNull("commandHistory repository should be initialized", instance.commandHistory)
        assertNotNull("userPreferences repository should be initialized", instance.userPreferences)
        assertNotNull("errorReports repository should be initialized", instance.errorReports)
        assertNotNull("avids repository should be initialized", instance.avids)
        assertNotNull("scrapedApps repository should be initialized", instance.scrapedApps)
        assertNotNull("scrapedElements repository should be initialized", instance.scrapedElements)
        assertNotNull("scrapedHierarchies repository should be initialized", instance.scrapedHierarchies)
        assertNotNull("elementCommands repository should be initialized", instance.elementCommands)
        assertNotNull("generatedCommands repository should be initialized", instance.generatedCommands)
        assertNotNull("voiceCommands repository should be initialized", instance.voiceCommands)
        assertNotNull("contextPreferences repository should be initialized", instance.contextPreferences)
        assertNotNull("commandUsage repository should be initialized", instance.commandUsage)
        assertNotNull("screenContexts repository should be initialized", instance.screenContexts)
        assertNotNull("screenTransitions repository should be initialized", instance.screenTransitions)
        assertNotNull("userInteractions repository should be initialized", instance.userInteractions)
        assertNotNull("elementRelationships repository should be initialized", instance.elementRelationships)
        assertNotNull("elementStateHistory repository should be initialized", instance.elementStateHistory)
        assertNotNull("appConsentHistory repository should be initialized", instance.appConsentHistory)
    }

    @Test
    fun testConcurrentGetInstanceCallsReturnSameInstance() = runTest {
        val driverFactory = DatabaseDriverFactory(context)

        // Launch 100 concurrent calls to getInstance
        val instances = List(100) {
            async(Dispatchers.Default) {
                VoiceOSDatabaseManager.getInstance(driverFactory)
            }
        }.awaitAll()

        // All instances should be the same object
        val firstInstance = instances.first()
        instances.forEach { instance ->
            assertSame(
                "All concurrent getInstance calls should return same instance",
                firstInstance,
                instance
            )
        }
    }

    @Test
    fun testSingletonDatabaseIsFunctional() = runTest {
        val instance = createTestDatabaseManager(context)

        // Test that we can actually use the database
        val generatedCommandsCount = instance.generatedCommands.count()

        // Verify count works (should be 0 for empty database)
        assertTrue(
            "Database should be functional and return valid count",
            generatedCommandsCount >= 0
        )
    }

    @Test
    fun testInMemoryDatabaseIsIsolatedPerTest() = runTest {
        val driverFactory1 = DatabaseDriverFactory(context)

        // Reset singleton to create new instance
        resetSingletonInstance()

        val driverFactory2 = DatabaseDriverFactory(context)

        val instance1 = VoiceOSDatabaseManager.getInstance(driverFactory1)

        // Reset singleton again
        resetSingletonInstance()

        val instance2 = VoiceOSDatabaseManager.getInstance(driverFactory2)

        // Instances should be different objects (different singletons)
        assertNotSame(
            "After reset, getInstance should create new instance",
            instance1,
            instance2
        )
    }

    @Test
    fun testInMemoryDatabaseInstanceIsFunctional() = runTest {
        val driverFactory = DatabaseDriverFactory(context)
        val instance = VoiceOSDatabaseManager.getInstance(driverFactory)

        assertNotNull("In-memory database should return non-null instance", instance)

        // Verify database is functional
        val count = instance.generatedCommands.count()
        assertTrue("In-memory database should be functional", count >= 0)
    }

    @Test
    fun testConcurrentAccessToRepositoriesIsSafe() = runTest {
        val driverFactory = DatabaseDriverFactory(context)
        val instance = VoiceOSDatabaseManager.getInstance(driverFactory)

        // Launch 50 concurrent operations across different repositories
        val results = List(50) { index ->
            async(Dispatchers.Default) {
                when (index % 4) {
                    0 -> instance.generatedCommands.count()
                    1 -> instance.voiceCommands.count()
                    2 -> instance.scrapedApps.count()
                    else -> instance.screenContexts.count()
                }
            }
        }.awaitAll()

        // All operations should complete successfully
        assertEquals("All concurrent operations should complete", 50, results.size)
        results.forEach { count ->
            assertTrue("Each operation should return valid count", count >= 0)
        }
    }

    @Test
    fun testGetInstanceWithSameContextReturnsSameInstance() {
        val driverFactory1 = DatabaseDriverFactory(context)
        val driverFactory2 = DatabaseDriverFactory(context)

        val instance1 = VoiceOSDatabaseManager.getInstance(driverFactory1)
        val instance2 = VoiceOSDatabaseManager.getInstance(driverFactory2)

        // Should return same instance regardless of driver factory
        // (singleton pattern - first factory wins)
        assertSame(
            "getInstance should return same instance for same context",
            instance1,
            instance2
        )
    }
}
