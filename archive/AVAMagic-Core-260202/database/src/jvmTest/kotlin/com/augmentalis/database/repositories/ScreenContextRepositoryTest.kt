/**
 * ScreenContextRepositoryTest.kt - Tests for ScreenContext repository
 *
 * Tests all CRUD operations and query methods for IScreenContextRepository:
 * - insert / getByHash
 * - getByApp
 * - getByActivity
 * - deleteByHash, deleteByApp, deleteAll
 * - count, countByApp
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2025-11-27
 */

package com.augmentalis.database.repositories

import com.augmentalis.database.dto.ScreenContextDTO
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class ScreenContextRepositoryTest : BaseRepositoryTest() {

    @BeforeTest
    fun setupApps() {
        // Ensure common test apps exist to satisfy foreign key constraints
        ensureScrapedAppExists("com.example.app")
        ensureScrapedAppExists("com.app1")
        ensureScrapedAppExists("com.app2")
    }

    // ==================== Helper Functions ====================

    /**
     * Create test ScreenContextDTO
     */
    private fun createScreenContext(
        screenHash: String,
        appId: String = "com.example.app",
        packageName: String = "com.example.app",
        activityName: String? = "MainActivity",
        windowTitle: String? = "Main Screen",
        screenType: String? = "main",
        formContext: String? = null,
        navigationLevel: Long = 1,
        primaryAction: String? = null,
        elementCount: Long = 10,
        hasBackButton: Long = 0,
        firstScraped: Long = now(),
        lastScraped: Long = now(),
        visitCount: Long = 1
    ): ScreenContextDTO {
        return ScreenContextDTO(
            id = 0, // Auto-generated
            screenHash = screenHash,
            appId = appId,
            packageName = packageName,
            activityName = activityName,
            windowTitle = windowTitle,
            screenType = screenType,
            formContext = formContext,
            navigationLevel = navigationLevel,
            primaryAction = primaryAction,
            elementCount = elementCount,
            hasBackButton = hasBackButton,
            firstScraped = firstScraped,
            lastScraped = lastScraped,
            visitCount = visitCount
        )
    }

    // ==================== Insert / Get Tests ====================

    @Test
    fun testInsertAndGetByHash() = runTest {
        val repo = databaseManager.screenContexts

        val screen = createScreenContext(
            screenHash = "screen-1",
            appId = "com.example.app",
            activityName = "MainActivity"
        )

        repo.insert(screen)

        val retrieved = repo.getByHash("screen-1")
        assertNotNull(retrieved)
        assertEquals("screen-1", retrieved.screenHash)
        assertEquals("com.example.app", retrieved.appId)
        assertEquals("MainActivity", retrieved.activityName)
    }

    @Test
    fun testGetByHashNull() = runTest {
        val repo = databaseManager.screenContexts

        val retrieved = repo.getByHash("nonexistent")
        assertNull(retrieved)
    }

    @Test
    fun testInsertReplaceExisting() = runTest {
        val repo = databaseManager.screenContexts

        // Insert original
        repo.insert(createScreenContext(
            screenHash = "screen-1",
            windowTitle = "Original Title",
            visitCount = 1
        ))

        // Insert replacement
        repo.insert(createScreenContext(
            screenHash = "screen-1",
            windowTitle = "Updated Title",
            visitCount = 5
        ))

        val retrieved = repo.getByHash("screen-1")
        assertNotNull(retrieved)
        assertEquals("Updated Title", retrieved.windowTitle)
        assertEquals(5, retrieved.visitCount)
    }

    // ==================== Query Tests ====================

    @Test
    fun testGetByAppEmpty() = runTest {
        val repo = databaseManager.screenContexts

        val screens = repo.getByApp("com.example.app")
        assertEquals(0, screens.size)
    }

    @Test
    fun testGetByAppMultiple() = runTest {
        val repo = databaseManager.screenContexts

        // Insert screens for app1
        repo.insert(createScreenContext("screen-1", appId = "com.app1"))
        repo.insert(createScreenContext("screen-2", appId = "com.app1"))
        repo.insert(createScreenContext("screen-3", appId = "com.app1"))

        // Insert screens for app2
        repo.insert(createScreenContext("screen-4", appId = "com.app2"))

        val app1Screens = repo.getByApp("com.app1")
        assertEquals(3, app1Screens.size)

        val app2Screens = repo.getByApp("com.app2")
        assertEquals(1, app2Screens.size)
    }

    @Test
    fun testGetByActivityEmpty() = runTest {
        val repo = databaseManager.screenContexts

        val screens = repo.getByActivity("MainActivity")
        assertEquals(0, screens.size)
    }

    @Test
    fun testGetByActivityMultiple() = runTest {
        val repo = databaseManager.screenContexts

        repo.insert(createScreenContext("screen-1", activityName = "MainActivity"))
        repo.insert(createScreenContext("screen-2", activityName = "MainActivity"))
        repo.insert(createScreenContext("screen-3", activityName = "SettingsActivity"))

        val mainScreens = repo.getByActivity("MainActivity")
        assertEquals(2, mainScreens.size)

        val settingsScreens = repo.getByActivity("SettingsActivity")
        assertEquals(1, settingsScreens.size)
    }

    @Test
    fun testGetAllEmpty() = runTest {
        val repo = databaseManager.screenContexts

        val screens = repo.getAll()
        assertEquals(0, screens.size)
    }

    @Test
    fun testGetAllMultiple() = runTest {
        val repo = databaseManager.screenContexts

        repo.insert(createScreenContext("screen-1"))
        repo.insert(createScreenContext("screen-2"))
        repo.insert(createScreenContext("screen-3"))

        val screens = repo.getAll()
        assertEquals(3, screens.size)
    }

    // ==================== Delete Tests ====================

    @Test
    fun testDeleteByHash() = runTest {
        val repo = databaseManager.screenContexts

        repo.insert(createScreenContext("screen-1"))
        repo.insert(createScreenContext("screen-2"))

        assertEquals(2, repo.count())

        repo.deleteByHash("screen-1")

        assertEquals(1, repo.count())
        assertNull(repo.getByHash("screen-1"))
        assertNotNull(repo.getByHash("screen-2"))
    }

    @Test
    fun testDeleteByHashNonexistent() = runTest {
        val repo = databaseManager.screenContexts

        repo.insert(createScreenContext("screen-1"))
        repo.deleteByHash("nonexistent")

        assertEquals(1, repo.count())
    }

    @Test
    fun testDeleteByApp() = runTest {
        val repo = databaseManager.screenContexts

        repo.insert(createScreenContext("screen-1", appId = "com.app1"))
        repo.insert(createScreenContext("screen-2", appId = "com.app1"))
        repo.insert(createScreenContext("screen-3", appId = "com.app2"))

        assertEquals(3, repo.count())

        repo.deleteByApp("com.app1")

        assertEquals(1, repo.count())
        assertEquals(0, repo.getByApp("com.app1").size)
        assertEquals(1, repo.getByApp("com.app2").size)
    }

    @Test
    fun testDeleteAll() = runTest {
        val repo = databaseManager.screenContexts

        repo.insert(createScreenContext("screen-1"))
        repo.insert(createScreenContext("screen-2"))
        repo.insert(createScreenContext("screen-3"))

        assertEquals(3, repo.count())

        repo.deleteAll()

        assertEquals(0, repo.count())
        assertEquals(0, repo.getAll().size)
    }

    // ==================== Count Tests ====================

    @Test
    fun testCountEmpty() = runTest {
        val repo = databaseManager.screenContexts

        assertEquals(0, repo.count())
    }

    @Test
    fun testCountMultiple() = runTest {
        val repo = databaseManager.screenContexts

        repo.insert(createScreenContext("screen-1"))
        repo.insert(createScreenContext("screen-2"))
        repo.insert(createScreenContext("screen-3"))

        assertEquals(3, repo.count())
    }

    @Test
    fun testCountByAppEmpty() = runTest {
        val repo = databaseManager.screenContexts

        assertEquals(0, repo.countByApp("com.example.app"))
    }

    @Test
    fun testCountByAppMultiple() = runTest {
        val repo = databaseManager.screenContexts

        repo.insert(createScreenContext("screen-1", appId = "com.app1"))
        repo.insert(createScreenContext("screen-2", appId = "com.app1"))
        repo.insert(createScreenContext("screen-3", appId = "com.app1"))
        repo.insert(createScreenContext("screen-4", appId = "com.app2"))

        assertEquals(3, repo.countByApp("com.app1"))
        assertEquals(1, repo.countByApp("com.app2"))
        assertEquals(0, repo.countByApp("com.app3"))
    }

    @Test
    fun testCountAfterDelete() = runTest {
        val repo = databaseManager.screenContexts

        repo.insert(createScreenContext("screen-1", appId = "com.app1"))
        repo.insert(createScreenContext("screen-2", appId = "com.app1"))

        assertEquals(2, repo.count())
        assertEquals(2, repo.countByApp("com.app1"))

        repo.deleteByHash("screen-1")

        assertEquals(1, repo.count())
        assertEquals(1, repo.countByApp("com.app1"))
    }

    // ==================== Edge Cases ====================

    @Test
    fun testNullableFieldsStored() = runTest {
        val repo = databaseManager.screenContexts

        val screen = createScreenContext(
            screenHash = "screen-1",
            activityName = null,
            windowTitle = null,
            screenType = null,
            formContext = null,
            primaryAction = null
        )

        repo.insert(screen)

        val retrieved = repo.getByHash("screen-1")
        assertNotNull(retrieved)
        assertNull(retrieved.activityName)
        assertNull(retrieved.windowTitle)
        assertNull(retrieved.screenType)
        assertNull(retrieved.formContext)
        assertNull(retrieved.primaryAction)
    }

    @Test
    fun testEmptyStringFields() = runTest {
        val repo = databaseManager.screenContexts

        val screen = createScreenContext(
            screenHash = "",
            appId = "",
            packageName = ""
        )

        repo.insert(screen)

        val retrieved = repo.getByHash("")
        assertNotNull(retrieved)
        assertEquals("", retrieved.screenHash)
        assertEquals("", retrieved.appId)
        assertEquals("", retrieved.packageName)
    }

    @Test
    fun testLongNumericValues() = runTest {
        val repo = databaseManager.screenContexts

        val screen = createScreenContext(
            screenHash = "screen-1",
            navigationLevel = Long.MAX_VALUE,
            elementCount = Long.MAX_VALUE,
            hasBackButton = 1,
            visitCount = Long.MAX_VALUE
        )

        repo.insert(screen)

        val retrieved = repo.getByHash("screen-1")
        assertNotNull(retrieved)
        assertEquals(Long.MAX_VALUE, retrieved.navigationLevel)
        assertEquals(Long.MAX_VALUE, retrieved.elementCount)
        assertEquals(1, retrieved.hasBackButton)
        assertEquals(Long.MAX_VALUE, retrieved.visitCount)
    }

    // ==================== Integration Tests ====================

    @Test
    fun testCompleteWorkflow() = runTest {
        val repo = databaseManager.screenContexts

        // Insert screens
        repo.insert(createScreenContext(
            screenHash = "screen-1",
            appId = "com.app1",
            activityName = "MainActivity",
            visitCount = 1
        ))

        assertEquals(1, repo.count())

        // Update (replace)
        repo.insert(createScreenContext(
            screenHash = "screen-1",
            appId = "com.app1",
            activityName = "MainActivity",
            visitCount = 5
        ))

        assertEquals(1, repo.count())
        assertEquals(5, repo.getByHash("screen-1")?.visitCount)

        // Add more screens
        repo.insert(createScreenContext("screen-2", appId = "com.app1"))
        repo.insert(createScreenContext("screen-3", appId = "com.app2"))

        assertEquals(3, repo.count())
        assertEquals(2, repo.countByApp("com.app1"))
        assertEquals(1, repo.countByApp("com.app2"))

        // Delete by app
        repo.deleteByApp("com.app1")

        assertEquals(1, repo.count())
        assertEquals(0, repo.countByApp("com.app1"))
        assertEquals(1, repo.countByApp("com.app2"))

        // Clean up
        repo.deleteAll()

        assertEquals(0, repo.count())
    }

    @Test
    fun testMultipleAppsIsolation() = runTest {
        val repo = databaseManager.screenContexts

        // App1: 3 screens
        repo.insert(createScreenContext("app1-screen1", appId = "com.app1"))
        repo.insert(createScreenContext("app1-screen2", appId = "com.app1"))
        repo.insert(createScreenContext("app1-screen3", appId = "com.app1"))

        // App2: 2 screens
        repo.insert(createScreenContext("app2-screen1", appId = "com.app2"))
        repo.insert(createScreenContext("app2-screen2", appId = "com.app2"))

        // Verify isolation
        assertEquals(3, repo.getByApp("com.app1").size)
        assertEquals(2, repo.getByApp("com.app2").size)
        assertEquals(5, repo.count())

        // Delete app1
        repo.deleteByApp("com.app1")

        assertEquals(0, repo.getByApp("com.app1").size)
        assertEquals(2, repo.getByApp("com.app2").size)
        assertEquals(2, repo.count())
    }

    @Test
    fun testActivityQueryIsolation() = runTest {
        val repo = databaseManager.screenContexts

        repo.insert(createScreenContext("screen-1", activityName = "MainActivity"))
        repo.insert(createScreenContext("screen-2", activityName = "MainActivity"))
        repo.insert(createScreenContext("screen-3", activityName = "SettingsActivity"))
        repo.insert(createScreenContext("screen-4", activityName = null))

        assertEquals(2, repo.getByActivity("MainActivity").size)
        assertEquals(1, repo.getByActivity("SettingsActivity").size)
        assertEquals(0, repo.getByActivity("NonexistentActivity").size)
    }
}
