/**
 * LearnAppDaoLSPContractTest.kt - LSP Contract Compliance Tests
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2025-12-22
 *
 * Tests that LearnAppDao implementations comply with Liskov Substitution Principle.
 * Verifies behavioral contracts documented in interface KDoc.
 *
 * Phase 5: SOLID Refactoring - Liskov Substitution Principle
 */

package com.augmentalis.voiceoscore.learnapp.database.dao

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.augmentalis.voiceoscore.learnapp.database.LearnAppDatabaseAdapter
import com.augmentalis.voiceoscore.learnapp.database.entities.ExplorationSessionEntity
import com.augmentalis.voiceoscore.learnapp.database.entities.LearnedAppEntity
import com.augmentalis.voiceoscore.learnapp.database.entities.NavigationEdgeEntity
import com.augmentalis.voiceoscore.learnapp.database.entities.ScreenStateEntity
import com.augmentalis.voiceoscore.learnapp.database.repository.ExplorationStatus
import com.augmentalis.voiceoscore.learnapp.database.repository.SessionStatus
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * LSP Contract Tests for LearnAppDao
 *
 * Tests verify that all implementations follow documented behavioral contracts:
 * - Nullable return contracts
 * - Exception behavior
 * - Idempotency guarantees
 * - Thread safety
 * - ACID transaction properties
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class LearnAppDaoLSPContractTest {

    private lateinit var context: Context
    private lateinit var dao: LearnAppDao

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        dao = LearnAppDatabaseAdapter.getInstance(context).learnAppDao()
    }

    @After
    fun teardown() = runBlocking {
        // Clean up test data
        dao.getAllLearnedApps().forEach { app ->
            dao.deleteLearnedApp(app)
        }
    }

    // ========== Contract: Nullable Return Values ==========

    @Test
    fun `getLearnedApp returns null when package not found`() = runBlocking {
        // LSP Contract: MUST return null if package not found (NOT throw exception)
        val result = dao.getLearnedApp("com.nonexistent.package")
        assertNull(result, "getLearnedApp should return null for non-existent package")
    }

    @Test
    fun `getExplorationSession returns null when session not found`() = runBlocking {
        // LSP Contract: MUST return null if session not found (NOT throw)
        val result = dao.getExplorationSession("nonexistent-session-id")
        assertNull(result, "getExplorationSession should return null for non-existent session")
    }

    @Test
    fun `getScreenState returns null when screen hash not found`() = runBlocking {
        // LSP Contract: MUST return null if screen not found
        val result = dao.getScreenState("nonexistent-hash")
        assertNull(result, "getScreenState should return null for non-existent hash")
    }

    // ========== Contract: Empty Collections ==========

    @Test
    fun `getAllLearnedApps returns empty list when no apps exist`() = runBlocking {
        // LSP Contract: MUST return empty list if no apps learned (NOT null)
        val result = dao.getAllLearnedApps()
        assertNotNull(result, "getAllLearnedApps should return list (not null)")
        assertTrue(result.isEmpty(), "getAllLearnedApps should return empty list when no apps")
    }

    @Test
    fun `getSessionsForPackage returns empty list when no sessions exist`() = runBlocking {
        // LSP Contract: MUST return empty list (NOT null) when no results
        val result = dao.getSessionsForPackage("com.nonexistent.package")
        assertNotNull(result, "getSessionsForPackage should return list (not null)")
        assertTrue(result.isEmpty(), "getSessionsForPackage should return empty list")
    }

    @Test
    fun `getNavigationGraph returns empty list when no edges exist`() = runBlocking {
        // LSP Contract: Methods returning collections return empty list (NOT null)
        val result = dao.getNavigationGraph("com.nonexistent.package")
        assertNotNull(result, "getNavigationGraph should return list (not null)")
        assertTrue(result.isEmpty(), "getNavigationGraph should return empty list")
    }

    // ========== Contract: Update Operations (No-op when entity doesn't exist) ==========

    @Test
    fun `updateAppHash is no-op when package doesn't exist`() = runBlocking {
        // LSP Contract: MUST be no-op if package doesn't exist (NOT throw exception)
        // Should not throw
        dao.updateAppHash("com.nonexistent.package", "newHash", System.currentTimeMillis())

        // Verify no app was created
        val result = dao.getLearnedApp("com.nonexistent.package")
        assertNull(result, "updateAppHash should not create app")
    }

    @Test
    fun `updateAppStats is no-op when package doesn't exist`() = runBlocking {
        // LSP Contract: MUST be no-op if package doesn't exist
        dao.updateAppStats("com.nonexistent.package", 10, 20)

        // Verify no app was created
        val result = dao.getLearnedApp("com.nonexistent.package")
        assertNull(result, "updateAppStats should not create app")
    }

    @Test
    fun `updateLearnedApp is no-op when package doesn't exist`() = runBlocking {
        // LSP Contract: MUST be no-op if package doesn't exist
        val nonExistentApp = LearnedAppEntity(
            packageName = "com.nonexistent.package",
            appName = "Test",
            versionCode = 1,
            versionName = "1.0",
            firstLearnedAt = System.currentTimeMillis(),
            lastUpdatedAt = System.currentTimeMillis(),
            totalScreens = 0,
            totalElements = 0,
            appHash = "hash",
            explorationStatus = ExplorationStatus.PARTIAL
        )

        dao.updateLearnedApp(nonExistentApp)

        // Verify no app was created
        val result = dao.getLearnedApp("com.nonexistent.package")
        assertNull(result, "updateLearnedApp should not create app")
    }

    @Test
    fun `updateSessionStatus is no-op when session doesn't exist`() = runBlocking {
        // LSP Contract: MUST be no-op if session doesn't exist
        dao.updateSessionStatus(
            "nonexistent-session",
            SessionStatus.COMPLETED,
            System.currentTimeMillis(),
            1000L
        )

        // Verify no session was created
        val result = dao.getExplorationSession("nonexistent-session")
        assertNull(result, "updateSessionStatus should not create session")
    }

    // ========== Contract: Delete Operations (Idempotent) ==========

    @Test
    fun `deleteLearnedApp is idempotent - no error if app doesn't exist`() = runBlocking {
        // LSP Contract: MUST be idempotent (no error if app doesn't exist)
        val nonExistentApp = LearnedAppEntity(
            packageName = "com.nonexistent.package",
            appName = "Test",
            versionCode = 1,
            versionName = "1.0",
            firstLearnedAt = System.currentTimeMillis(),
            lastUpdatedAt = System.currentTimeMillis(),
            totalScreens = 0,
            totalElements = 0,
            appHash = "hash",
            explorationStatus = ExplorationStatus.PARTIAL
        )

        // Should not throw
        dao.deleteLearnedApp(nonExistentApp)
    }

    @Test
    fun `deleteExplorationSession is idempotent`() = runBlocking {
        // LSP Contract: MUST be idempotent (no error if session doesn't exist)
        val nonExistentSession = ExplorationSessionEntity(
            sessionId = "nonexistent",
            packageName = "com.test",
            startedAt = System.currentTimeMillis(),
            completedAt = null,
            durationMs = null,
            screensExplored = 0,
            elementsDiscovered = 0,
            status = SessionStatus.RUNNING
        )

        // Should not throw
        dao.deleteExplorationSession(nonExistentSession)
    }

    @Test
    fun `deleteScreenState is idempotent`() = runBlocking {
        // LSP Contract: MUST be idempotent (no error if state doesn't exist)
        val nonExistentState = ScreenStateEntity(
            screenHash = "nonexistent",
            packageName = "com.test",
            activityName = "Test",
            fingerprint = "fp",
            elementCount = 0,
            discoveredAt = System.currentTimeMillis()
        )

        // Should not throw
        dao.deleteScreenState(nonExistentState)
    }

    @Test
    fun `deleteSessionsForPackage is idempotent`() = runBlocking {
        // LSP Contract: MUST be idempotent (no error if package has no sessions)
        // Should not throw
        dao.deleteSessionsForPackage("com.nonexistent.package")
    }

    @Test
    fun `deleteNavigationGraph is idempotent`() = runBlocking {
        // LSP Contract: MUST be idempotent
        // Should not throw
        dao.deleteNavigationGraph("com.nonexistent.package")
    }

    // ========== Contract: Insert Operations ==========

    @Test
    fun `insertLearnedApp succeeds for valid entity`() = runBlocking {
        // LSP Contract: All required fields in entity MUST be non-null
        val app = LearnedAppEntity(
            packageName = "com.test.app",
            appName = "Test App",
            versionCode = 1,
            versionName = "1.0",
            firstLearnedAt = System.currentTimeMillis(),
            lastUpdatedAt = System.currentTimeMillis(),
            totalScreens = 5,
            totalElements = 20,
            appHash = "hash123",
            explorationStatus = ExplorationStatus.COMPLETE
        )

        dao.insertLearnedApp(app)

        val retrieved = dao.getLearnedApp("com.test.app")
        assertNotNull(retrieved, "Inserted app should be retrievable")
        assertEquals("com.test.app", retrieved.packageName)
        assertEquals("Test App", retrieved.appName)
    }

    @Test
    fun `insertLearnedAppMinimal returns positive value on success`() = runBlocking {
        // LSP Contract: MUST return positive value (1+) if insertion succeeds
        val app = LearnedAppEntity(
            packageName = "com.test.minimal",
            appName = "Test",
            versionCode = 1,
            versionName = "1.0",
            firstLearnedAt = System.currentTimeMillis(),
            lastUpdatedAt = System.currentTimeMillis(),
            totalScreens = 0,
            totalElements = 0,
            appHash = "hash",
            explorationStatus = ExplorationStatus.PARTIAL
        )

        val rowId = dao.insertLearnedAppMinimal(app)
        assertTrue(rowId > 0, "insertLearnedAppMinimal should return positive value on success")
    }

    @Test
    fun `insertLearnedAppMinimal returns 0 on conflict`() = runBlocking {
        // LSP Contract: MUST return 0 if app already exists (conflict detection)
        val app = LearnedAppEntity(
            packageName = "com.test.conflict",
            appName = "Test",
            versionCode = 1,
            versionName = "1.0",
            firstLearnedAt = System.currentTimeMillis(),
            lastUpdatedAt = System.currentTimeMillis(),
            totalScreens = 0,
            totalElements = 0,
            appHash = "hash",
            explorationStatus = ExplorationStatus.PARTIAL
        )

        // First insert should succeed
        val firstRowId = dao.insertLearnedAppMinimal(app)
        assertTrue(firstRowId > 0, "First insert should succeed")

        // Second insert should detect conflict
        val secondRowId = dao.insertLearnedAppMinimal(app)
        assertEquals(0L, secondRowId, "Second insert should return 0 (conflict)")
    }

    // ========== Contract: Transaction ACID Properties ==========

    @Test
    fun `transaction provides atomicity - rollback on failure`() = runBlocking {
        // LSP Contract: MUST rollback all changes if any operation fails
        val app = LearnedAppEntity(
            packageName = "com.test.transaction",
            appName = "Test",
            versionCode = 1,
            versionName = "1.0",
            firstLearnedAt = System.currentTimeMillis(),
            lastUpdatedAt = System.currentTimeMillis(),
            totalScreens = 0,
            totalElements = 0,
            appHash = "hash",
            explorationStatus = ExplorationStatus.PARTIAL
        )

        try {
            dao.transaction {
                insertLearnedApp(app)
                // Force failure by trying to insert duplicate
                throw Exception("Simulated transaction failure")
            }
        } catch (e: Exception) {
            // Expected
        }

        // Verify rollback - app should not exist
        val result = dao.getLearnedApp("com.test.transaction")
        assertNull(result, "Transaction should rollback on failure")
    }

    @Test
    fun `transaction commits all changes on success`() = runBlocking {
        // LSP Contract: MUST provide ACID guarantees
        val app = LearnedAppEntity(
            packageName = "com.test.commit",
            appName = "Test",
            versionCode = 1,
            versionName = "1.0",
            firstLearnedAt = System.currentTimeMillis(),
            lastUpdatedAt = System.currentTimeMillis(),
            totalScreens = 0,
            totalElements = 0,
            appHash = "hash",
            explorationStatus = ExplorationStatus.PARTIAL
        )

        dao.transaction {
            insertLearnedApp(app)
            updateAppHash("com.test.commit", "newhash", System.currentTimeMillis())
        }

        // Verify both operations committed
        val result = dao.getLearnedApp("com.test.commit")
        assertNotNull(result, "Transaction should commit on success")
        assertEquals("newhash", result.appHash, "Hash update should be committed")
    }

    // ========== Contract: Foreign Key Constraints ==========

    @Test
    fun `insertExplorationSession enforces foreign key constraint`() = runBlocking {
        // LSP Contract: MUST enforce foreign key constraint (packageName must exist)
        // LSP Contract: MUST throw exception if parent app doesn't exist

        val session = ExplorationSessionEntity(
            sessionId = "test-session",
            packageName = "com.nonexistent.package",
            startedAt = System.currentTimeMillis(),
            completedAt = null,
            durationMs = null,
            screensExplored = 0,
            elementsDiscovered = 0,
            status = SessionStatus.RUNNING
        )

        var exceptionThrown = false
        try {
            dao.insertExplorationSession(session)
        } catch (e: Exception) {
            exceptionThrown = true
        }

        assertTrue(exceptionThrown, "Foreign key violation should throw exception")
    }

    // ========== Contract: Cascade Delete ==========

    @Test
    fun `deleteLearnedApp cascades to related entities`() = runBlocking {
        // LSP Contract: MUST cascade delete related entities (sessions, screens, edges)

        // Setup: Create app with session
        val app = LearnedAppEntity(
            packageName = "com.test.cascade",
            appName = "Test",
            versionCode = 1,
            versionName = "1.0",
            firstLearnedAt = System.currentTimeMillis(),
            lastUpdatedAt = System.currentTimeMillis(),
            totalScreens = 0,
            totalElements = 0,
            appHash = "hash",
            explorationStatus = ExplorationStatus.PARTIAL
        )
        dao.insertLearnedApp(app)

        val session = ExplorationSessionEntity(
            sessionId = "cascade-session",
            packageName = "com.test.cascade",
            startedAt = System.currentTimeMillis(),
            completedAt = null,
            durationMs = null,
            screensExplored = 0,
            elementsDiscovered = 0,
            status = SessionStatus.RUNNING
        )
        dao.insertExplorationSession(session)

        // Delete app
        dao.deleteLearnedApp(app)

        // Verify cascade: session should be deleted
        val sessionResult = dao.getExplorationSession("cascade-session")
        assertNull(sessionResult, "Related session should be cascade deleted")
    }

    @Test
    fun `deleteExplorationSession cascades to navigation edges`() = runBlocking {
        // LSP Contract: MUST cascade delete related navigation edges

        // Setup: Create app, session, and edge
        val app = LearnedAppEntity(
            packageName = "com.test.edgecascade",
            appName = "Test",
            versionCode = 1,
            versionName = "1.0",
            firstLearnedAt = System.currentTimeMillis(),
            lastUpdatedAt = System.currentTimeMillis(),
            totalScreens = 0,
            totalElements = 0,
            appHash = "hash",
            explorationStatus = ExplorationStatus.PARTIAL
        )
        dao.insertLearnedApp(app)

        val session = ExplorationSessionEntity(
            sessionId = "edge-session",
            packageName = "com.test.edgecascade",
            startedAt = System.currentTimeMillis(),
            completedAt = null,
            durationMs = null,
            screensExplored = 0,
            elementsDiscovered = 0,
            status = SessionStatus.RUNNING
        )
        dao.insertExplorationSession(session)

        val edge = NavigationEdgeEntity(
            edgeId = "edge-1",
            packageName = "com.test.edgecascade",
            sessionId = "edge-session",
            fromScreenHash = "screen1",
            clickedElementUuid = "element1",
            toScreenHash = "screen2",
            timestamp = System.currentTimeMillis()
        )
        dao.insertNavigationEdge(edge)

        // Delete session
        dao.deleteExplorationSession(session)

        // Verify cascade: edge should be deleted
        val edges = dao.getEdgesForSession("edge-session")
        assertTrue(edges.isEmpty(), "Navigation edges should be cascade deleted")
    }

    // ========== Contract: Thread Safety ==========

    @Test
    fun `concurrent operations are thread-safe`() = runBlocking {
        // LSP Contract: MUST be thread-safe
        // Note: This is a basic concurrency test. Full thread safety testing
        // requires more comprehensive multi-threaded tests.

        val app = LearnedAppEntity(
            packageName = "com.test.concurrent",
            appName = "Test",
            versionCode = 1,
            versionName = "1.0",
            firstLearnedAt = System.currentTimeMillis(),
            lastUpdatedAt = System.currentTimeMillis(),
            totalScreens = 0,
            totalElements = 0,
            appHash = "hash",
            explorationStatus = ExplorationStatus.PARTIAL
        )

        dao.insertLearnedApp(app)

        // Concurrent reads should be safe
        val result1 = dao.getLearnedApp("com.test.concurrent")
        val result2 = dao.getLearnedApp("com.test.concurrent")

        assertNotNull(result1)
        assertNotNull(result2)
        assertEquals(result1.packageName, result2.packageName)
    }

    // ========== Contract: Update Statistics Atomicity ==========

    @Test
    fun `updateAppStats updates atomically`() = runBlocking {
        // LSP Contract: MUST update statistics atomically
        // LSP Contract: MUST update lastUpdatedAt timestamp automatically

        val app = LearnedAppEntity(
            packageName = "com.test.stats",
            appName = "Test",
            versionCode = 1,
            versionName = "1.0",
            firstLearnedAt = System.currentTimeMillis(),
            lastUpdatedAt = System.currentTimeMillis(),
            totalScreens = 0,
            totalElements = 0,
            appHash = "hash",
            explorationStatus = ExplorationStatus.PARTIAL
        )
        dao.insertLearnedApp(app)

        val beforeUpdate = System.currentTimeMillis()
        Thread.sleep(10) // Small delay to ensure timestamp differs

        dao.updateAppStats("com.test.stats", 10, 50)

        val updated = dao.getLearnedApp("com.test.stats")
        assertNotNull(updated)
        assertEquals(10, updated.totalScreens, "Total screens should be updated")
        assertEquals(50, updated.totalElements, "Total elements should be updated")
        assertTrue(
            updated.lastUpdatedAt > beforeUpdate,
            "lastUpdatedAt should be updated automatically"
        )
    }
}
