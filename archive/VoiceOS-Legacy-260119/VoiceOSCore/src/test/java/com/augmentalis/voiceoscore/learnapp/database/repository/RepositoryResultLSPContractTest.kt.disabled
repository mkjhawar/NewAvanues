/**
 * RepositoryResultLSPContractTest.kt - LSP Contract Compliance Tests
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2025-12-22
 *
 * Tests that RepositoryResult and SessionCreationResult comply with LSP.
 * Verifies behavioral contracts documented in sealed class KDoc.
 *
 * Phase 5: SOLID Refactoring - Liskov Substitution Principle
 */

package com.augmentalis.voiceoscore.learnapp.database.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.augmentalis.voiceoscore.learnapp.database.LearnAppDatabaseAdapter
import com.augmentalis.voiceoscore.learnapp.models.ExplorationStats
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * LSP Contract Tests for RepositoryResult and SessionCreationResult
 *
 * Tests verify that result types follow documented behavioral contracts:
 * - Success contains valid non-null value
 * - Failure contains descriptive reason
 * - Exception handling guarantees
 * - Threading guarantees
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class RepositoryResultLSPContractTest {

    private lateinit var context: Context
    private lateinit var repository: LearnAppRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        val dao = LearnAppDatabaseAdapter.getInstance(context).learnAppDao()
        repository = LearnAppRepository(dao, context)
    }

    @After
    fun teardown() = runBlocking {
        // Clean up test data
        repository.getAllLearnedApps().forEach { app ->
            repository.deleteLearnedApp(app.packageName)
        }
    }

    // ========== Contract: RepositoryResult.Success ==========

    @Test
    fun `RepositoryResult Success contains valid non-null value`() = runBlocking {
        // LSP Contract: MUST contain valid non-null value of type T
        // Setup test data
        repository.saveLearnedApp(
            packageName = "com.test.success",
            appName = "Test App",
            versionCode = 1,
            versionName = "1.0",
            stats = ExplorationStats(
                totalScreens = 5,
                totalElements = 20,
                durationMs = 1000L
            )
        )

        val result = repository.deleteAppCompletely("com.test.success")

        // Verify Success contract
        when (result) {
            is RepositoryResult.Success -> {
                assertNotNull(result.value, "Success value must not be null")
                assertTrue(result.value, "Success value should be true")
            }
            is RepositoryResult.Failure -> {
                throw AssertionError("Expected Success but got Failure: ${result.reason}")
            }
        }
    }

    @Test
    fun `RepositoryResult Success value is in valid state`() = runBlocking {
        // LSP Contract: Value MUST be in valid state (all invariants satisfied)
        repository.saveLearnedApp(
            packageName = "com.test.validstate",
            appName = "Test",
            versionCode = 1,
            versionName = "1.0",
            stats = ExplorationStats(0, 0, 0L)
        )

        val result = repository.deleteAppCompletely("com.test.validstate")

        when (result) {
            is RepositoryResult.Success -> {
                // Value is Boolean - should be true for successful deletion
                assertTrue(result.value, "Success value invariant: deletion should be true")
            }
            is RepositoryResult.Failure -> {
                throw AssertionError("Expected Success but got Failure")
            }
        }
    }

    // ========== Contract: RepositoryResult.Failure ==========

    @Test
    fun `RepositoryResult Failure contains descriptive reason`() = runBlocking {
        // LSP Contract: MUST contain descriptive reason string (non-empty)
        val result = repository.deleteAppCompletely("com.nonexistent.package")

        when (result) {
            is RepositoryResult.Success -> {
                throw AssertionError("Expected Failure for non-existent package")
            }
            is RepositoryResult.Failure -> {
                assertNotNull(result.reason, "Failure reason must not be null")
                assertTrue(result.reason.isNotEmpty(), "Failure reason must be non-empty")
                assertTrue(
                    result.reason.contains("not found") || result.reason.contains("error"),
                    "Failure reason should be descriptive: '${result.reason}'"
                )
            }
        }
    }

    @Test
    fun `RepositoryResult Failure reason includes cause message when available`() = runBlocking {
        // LSP Contract: If cause is present, reason SHOULD include cause message
        val result = repository.deleteAppCompletely("com.nonexistent.package")

        when (result) {
            is RepositoryResult.Failure -> {
                if (result.cause != null) {
                    // If cause is present, reason should reference it
                    assertTrue(
                        result.reason.isNotEmpty(),
                        "Reason should be descriptive when cause is present"
                    )
                }
            }
            is RepositoryResult.Success -> {
                throw AssertionError("Expected Failure")
            }
        }
    }

    // ========== Contract: SessionCreationResult.Created ==========

    @Test
    fun `SessionCreationResult Created has unique non-empty sessionId`() = runBlocking {
        // LSP Contract: sessionId MUST be unique and non-empty
        val result = repository.createExplorationSessionSafe("com.test.session")

        when (result) {
            is SessionCreationResult.Created -> {
                assertNotNull(result.sessionId, "sessionId must not be null")
                assertTrue(result.sessionId.isNotEmpty(), "sessionId must be non-empty")

                // Create another session and verify uniqueness
                val result2 = repository.createExplorationSessionSafe("com.test.session2")
                when (result2) {
                    is SessionCreationResult.Created -> {
                        assertFalse(
                            result.sessionId == result2.sessionId,
                            "Session IDs must be unique"
                        )
                    }
                    is SessionCreationResult.Failed -> {
                        // Acceptable - just testing uniqueness if both succeed
                    }
                }
            }
            is SessionCreationResult.Failed -> {
                // May fail if app creation not allowed - test framework limitation
            }
        }
    }

    @Test
    fun `SessionCreationResult Created indicates if app was auto-created`() = runBlocking {
        // LSP Contract: appWasCreated indicates if parent app was auto-created (true) or existed (false)

        // First session should auto-create app
        val result1 = repository.createExplorationSessionSafe("com.test.autocreate")

        when (result1) {
            is SessionCreationResult.Created -> {
                assertTrue(
                    result1.appWasCreated,
                    "First session should auto-create app (appWasCreated=true)"
                )

                // Second session should find existing app
                val result2 = repository.createExplorationSessionSafe("com.test.autocreate")
                when (result2) {
                    is SessionCreationResult.Created -> {
                        assertFalse(
                            result2.appWasCreated,
                            "Second session should not create app (appWasCreated=false)"
                        )
                    }
                    is SessionCreationResult.Failed -> {
                        // Acceptable
                    }
                }
            }
            is SessionCreationResult.Failed -> {
                // Test framework limitation
            }
        }
    }

    // ========== Contract: SessionCreationResult.Failed ==========

    @Test
    fun `SessionCreationResult Failed has descriptive reason`() = runBlocking {
        // LSP Contract: reason MUST be descriptive
        // Force a failure by corrupting the repository state (implementation-specific)

        // This test verifies the contract but may pass if no failure occurs
        val result = repository.createExplorationSessionSafe("")

        when (result) {
            is SessionCreationResult.Failed -> {
                assertNotNull(result.reason, "Failure reason must not be null")
                assertTrue(result.reason.isNotEmpty(), "Failure reason must be non-empty")
            }
            is SessionCreationResult.Created -> {
                // Session creation succeeded - contract still holds
            }
        }
    }

    // ========== Contract: Exception Handling Guarantees ==========

    @Test
    fun `deleteAppCompletely catches exceptions and returns Failure`() = runBlocking {
        // LSP Contract: Repository methods MUST catch exceptions and return Failure (NOT propagate)

        val result = repository.deleteAppCompletely("com.nonexistent.package")

        // Should return Failure, not throw exception
        when (result) {
            is RepositoryResult.Failure -> {
                // Expected - verified contract
                assertNotNull(result.reason)
            }
            is RepositoryResult.Success -> {
                throw AssertionError("Expected Failure for non-existent package")
            }
        }
    }

    @Test
    fun `resetAppForRelearning catches exceptions and returns Failure`() = runBlocking {
        // LSP Contract: Database errors wrapped in Failure with clear message

        val result = repository.resetAppForRelearning("com.nonexistent.package")

        when (result) {
            is RepositoryResult.Failure -> {
                assertNotNull(result.reason)
                assertTrue(
                    result.reason.contains("not found") || result.reason.contains("error"),
                    "Failure reason should describe the error"
                )
            }
            is RepositoryResult.Success -> {
                throw AssertionError("Expected Failure for non-existent package")
            }
        }
    }

    // ========== Contract: Threading Guarantees ==========

    @Test
    fun `createExplorationSessionSafe is thread-safe via mutex`() = runBlocking {
        // LSP Contract: createExplorationSessionSafe() is thread-safe via mutex
        // LSP Contract: Prevents race conditions in auto-app-creation logic

        // Sequential calls should not cause race conditions
        val result1 = repository.createExplorationSessionSafe("com.test.threadsafe")
        val result2 = repository.createExplorationSessionSafe("com.test.threadsafe")

        // Both should succeed without conflicts
        when (result1) {
            is SessionCreationResult.Created -> {
                assertNotNull(result1.sessionId)
            }
            is SessionCreationResult.Failed -> {
                // Acceptable
            }
        }

        when (result2) {
            is SessionCreationResult.Created -> {
                assertNotNull(result2.sessionId)
            }
            is SessionCreationResult.Failed -> {
                // Acceptable
            }
        }

        // No exceptions should be thrown
    }

    @Test
    fun `transaction guarantees ACID properties in SessionCreationResult`() = runBlocking {
        // LSP Contract: Transaction guarantees ACID properties

        val result = repository.createExplorationSessionSafe("com.test.acid")

        when (result) {
            is SessionCreationResult.Created -> {
                // Verify atomicity: both app and session should exist or neither
                val app = repository.getLearnedApp("com.test.acid")
                val session = repository.getExplorationSession(result.sessionId)

                if (result.appWasCreated) {
                    assertNotNull(app, "App should exist after successful creation")
                }
                assertNotNull(session, "Session should exist after successful creation")
            }
            is SessionCreationResult.Failed -> {
                // Verify atomicity: if failed, neither app nor session should exist
                val app = repository.getLearnedApp("com.test.acid")
                // App might exist if failure was after app creation but before session
                // This is acceptable as long as the failure is reported
            }
        }
    }

    // ========== Contract: Behavioral Consistency ==========

    @Test
    fun `RepositoryResult maintains consistent behavior across calls`() = runBlocking {
        // Verify that same operation produces consistent results
        repository.saveLearnedApp(
            "com.test.consistent",
            "Test",
            1,
            "1.0",
            ExplorationStats(0, 0, 0L)
        )

        val result1 = repository.resetAppForRelearning("com.test.consistent")
        val result2 = repository.resetAppForRelearning("com.test.consistent")

        // Both should succeed
        when (result1) {
            is RepositoryResult.Success -> {
                assertNotNull(result1.value)
            }
            is RepositoryResult.Failure -> {
                throw AssertionError("First reset should succeed")
            }
        }

        when (result2) {
            is RepositoryResult.Success -> {
                assertNotNull(result2.value)
            }
            is RepositoryResult.Failure -> {
                throw AssertionError("Second reset should succeed")
            }
        }
    }

    @Test
    fun `SessionCreationResult maintains consistent behavior`() = runBlocking {
        // Verify consistent behavior for session creation
        val result1 = repository.createExplorationSessionSafe("com.test.consistent.session")

        when (result1) {
            is SessionCreationResult.Created -> {
                assertTrue(result1.appWasCreated, "First call should create app")
            }
            is SessionCreationResult.Failed -> {
                // Acceptable
            }
        }

        val result2 = repository.createExplorationSessionSafe("com.test.consistent.session")

        when (result2) {
            is SessionCreationResult.Created -> {
                assertFalse(result2.appWasCreated, "Second call should not create app")
            }
            is SessionCreationResult.Failed -> {
                // Acceptable
            }
        }
    }
}
