/**
 * RepositoryIntegrationTest.kt - Integration tests for SQLDelight repositories
 *
 * Tests all repository operations using the repository layer (not direct queries).
 * Uses kotlinx-coroutines-test for suspend function testing.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.database.repositories

import com.augmentalis.database.DatabaseDriverFactory
import com.augmentalis.database.VoiceOSDatabaseManager
import com.augmentalis.database.dto.AvidElementDTO
import com.augmentalis.database.dto.AvidHierarchyDTO
import com.augmentalis.database.dto.AvidAnalyticsDTO
import com.augmentalis.database.dto.AvidAliasDTO
import com.augmentalis.database.dto.ScrapedAppDTO
import com.augmentalis.database.dto.VoiceCommandDTO
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Comprehensive integration tests for repositories:
 * - IAvidRepository
 * - IScrapedAppRepository
 * - IVoiceCommandRepository
 */
class RepositoryIntegrationTest {

    private lateinit var databaseManager: VoiceOSDatabaseManager

    @BeforeTest
    fun setup() {
        val driverFactory = DatabaseDriverFactory()
        databaseManager = VoiceOSDatabaseManager.getInstance(driverFactory)
    }

    @AfterTest
    fun teardown() {
        VoiceOSDatabaseManager.clearInstance()
    }

    private fun now() = System.currentTimeMillis()
    private fun past(millis: Long) = now() - millis

    // ==================== AVID Repository Tests ====================

    @Test
    fun testAvidElementCRUD() = runTest {
        val repo = databaseManager.avidRepository
        val element = AvidElementDTO(
            avid = "elem-001",
            name = "Test Button",
            type = "BUTTON",
            description = "Test",
            parentAvid = null,
            isEnabled = true,
            priority = 0,
            timestamp = now(),
            metadataJson = null,
            positionJson = null
        )

        // Insert
        repo.insertElement(element)

        // Get
        val retrieved = repo.getElementByAvid("elem-001")
        assertNotNull(retrieved)
        assertEquals("Test Button", retrieved.name)

        // Update
        repo.updateElement(element.copy(name = "Updated"))
        val updated = repo.getElementByAvid("elem-001")
        assertEquals("Updated", updated?.name)

        // Delete
        repo.deleteElement("elem-001")
        assertNull(repo.getElementByAvid("elem-001"))
    }

    @Test
    fun testAvidAliasOperations() = runTest {
        val repo = databaseManager.avidRepository

        // Insert element first
        repo.insertElement(AvidElementDTO(
            avid = "elem-001",
            name = "Test",
            type = "BUTTON",
            description = null,
            parentAvid = null,
            isEnabled = true,
            priority = 0,
            timestamp = now(),
            metadataJson = null,
            positionJson = null
        ))

        // Insert alias
        val alias = AvidAliasDTO(0, "my-alias", "elem-001", true, now())
        repo.insertAlias(alias)

        // Get alias
        assertTrue(repo.aliasExists("my-alias"))
        assertEquals("elem-001", repo.getAvidByAlias("my-alias"))

        // Delete alias
        repo.deleteAliasByName("my-alias")
        assertFalse(repo.aliasExists("my-alias"))
    }

    @Test
    fun testAvidAnalyticsTracking() = runTest {
        val repo = databaseManager.avidRepository

        // Insert element
        repo.insertElement(AvidElementDTO(
            avid = "elem-001",
            name = "Test",
            type = "BUTTON",
            description = null,
            parentAvid = null,
            isEnabled = true,
            priority = 0,
            timestamp = now(),
            metadataJson = null,
            positionJson = null
        ))

        // Insert analytics
        val analytics = AvidAnalyticsDTO("elem-001", 0, now(), now(), 100, 0, 0, "ACTIVE")
        repo.insertAnalytics(analytics)

        // Increment access
        repo.incrementAccessCount("elem-001", now())

        val retrieved = repo.getAnalyticsByAvid("elem-001")
        assertEquals(1, retrieved?.accessCount)
    }

    @Test
    fun testAvidHierarchyManagement() = runTest {
        val repo = databaseManager.avidRepository

        // Insert parent and child elements
        repo.insertElement(AvidElementDTO(
            avid = "parent",
            name = "Parent",
            type = "CONTAINER",
            description = null,
            parentAvid = null,
            isEnabled = true,
            priority = 0,
            timestamp = now(),
            metadataJson = null,
            positionJson = null
        ))
        repo.insertElement(AvidElementDTO(
            avid = "child",
            name = "Child",
            type = "BUTTON",
            description = null,
            parentAvid = "parent",
            isEnabled = true,
            priority = 0,
            timestamp = now(),
            metadataJson = null,
            positionJson = null
        ))

        // Insert hierarchy
        val hierarchy = AvidHierarchyDTO(0, "parent", "child", 1, "/parent/child", 0)
        repo.insertHierarchy(hierarchy)

        val children = repo.getHierarchyByParent("parent")
        assertEquals(1, children.size)
        assertEquals("child", children[0].childAvid)
    }

    // ==================== ScrapedApp Repository Tests ====================

    @Test
    fun testScrapedAppCRUD() = runTest {
        val repo = databaseManager.scrapedApps
        val app = ScrapedAppDTO(
            appId = "app-001",
            packageName = "com.example.test",
            versionCode = 1,
            versionName = "1.0.0",
            appHash = "hash123",
            isFullyLearned = 0,
            learnCompletedAt = null,
            scrapingMode = "DYNAMIC",
            scrapeCount = 0,
            elementCount = 0,
            commandCount = 0,
            firstScrapedAt = now(),
            lastScrapedAt = now()
        )

        // Insert
        repo.insert(app)

        // Get
        val retrieved = repo.getById("app-001")
        assertNotNull(retrieved)
        assertEquals("com.example.test", retrieved.packageName)

        // Update stats
        repo.updateStats("app-001", 5, 100, 25, now())
        val updated = repo.getById("app-001")
        assertEquals(5, updated?.scrapeCount)
        assertEquals(100, updated?.elementCount)

        // Mark learned
        repo.markFullyLearned("app-001", now())
        val learned = repo.getById("app-001")
        assertEquals(1, learned?.isFullyLearned)

        // Delete
        repo.deleteById("app-001")
        assertNull(repo.getById("app-001"))
    }

    @Test
    fun testScrapedAppFiltering() = runTest {
        val repo = databaseManager.scrapedApps

        repo.insert(createTestApp("app-001", "com.test.one", isLearned = true))
        repo.insert(createTestApp("app-002", "com.test.two", isLearned = false))
        repo.insert(createTestApp("app-003", "com.test.three", isLearned = true))

        val fullyLearned = repo.getFullyLearned()
        assertEquals(2, fullyLearned.size)
        assertTrue(fullyLearned.all { it.isFullyLearned == 1L })
    }

    // ==================== VoiceCommand Repository Tests ====================

    @Test
    fun testVoiceCommandCRUD() = runTest {
        val repo = databaseManager.voiceCommands
        val timestamp = now()
        val command = VoiceCommandDTO(
            id = 0,
            commandId = "cmd-001",
            locale = "en-US",
            triggerPhrase = "open settings",
            synonyms = "[]",
            action = "OPEN_SETTINGS",
            description = "",
            category = "SYSTEM",
            priority = 0,
            isFallback = 0,
            isEnabled = 1,
            createdAt = timestamp,
            updatedAt = timestamp
        )

        // Insert
        val id = repo.insert(command)
        assertTrue(id > 0)

        // Get
        val retrieved = repo.getById(id)
        assertNotNull(retrieved)
        assertEquals("open settings", retrieved.triggerPhrase)

        // Update
        repo.updateEnabledState(id, false)
        val disabled = repo.getById(id)
        assertEquals(0, disabled?.isEnabled)

        // Delete
        repo.delete(id)
        assertNull(repo.getById(id))
    }

    @Test
    fun testVoiceCommandLocaleSupport() = runTest {
        val repo = databaseManager.voiceCommands
        val timestamp = now()

        // Insert same command in multiple locales
        repo.insert(VoiceCommandDTO(0, "nav-home", "en-US", "go home", "[]", "NAV", "", "NAV", 0, 0, 1, timestamp, timestamp))
        repo.insert(VoiceCommandDTO(0, "nav-home", "es-ES", "ir a inicio", "[]", "NAV", "", "NAV", 0, 0, 1, timestamp, timestamp))
        repo.insert(VoiceCommandDTO(0, "nav-home", "fr-FR", "aller à accueil", "[]", "NAV", "", "NAV", 0, 0, 1, timestamp, timestamp))

        // Query by command ID
        val allLocales = repo.getByCommandId("nav-home")
        assertEquals(3, allLocales.size)

        // Query by locale
        val enCommands = repo.getByLocale("en-US")
        assertEquals(1, enCommands.size)
        assertEquals("go home", enCommands[0].triggerPhrase)

        // Count by locale
        assertEquals(1, repo.countByLocale("es-ES"))
    }

    @Test
    fun testVoiceCommandSearch() = runTest {
        val repo = databaseManager.voiceCommands
        val timestamp = now()

        repo.insert(VoiceCommandDTO(0, "cmd-001", "en-US", "open settings", "[]", "ACT", "", "SYS", 0, 0, 1, timestamp, timestamp))
        repo.insert(VoiceCommandDTO(0, "cmd-002", "en-US", "open browser", "[]", "ACT", "", "SYS", 0, 0, 1, timestamp, timestamp))
        repo.insert(VoiceCommandDTO(0, "cmd-003", "en-US", "close window", "[]", "ACT", "", "SYS", 0, 0, 1, timestamp, timestamp))

        val openCommands = repo.searchByTrigger("%open%")
        assertEquals(2, openCommands.size)
    }

    // ==================== Helper Functions ====================

    private fun createTestApp(appId: String, packageName: String, isLearned: Boolean = false): ScrapedAppDTO {
        val timestamp = now()
        return ScrapedAppDTO(
            appId = appId,
            packageName = packageName,
            versionCode = 1,
            versionName = "1.0.0",
            appHash = "hash-$appId",
            isFullyLearned = if (isLearned) 1 else 0,
            learnCompletedAt = if (isLearned) timestamp else null,
            scrapingMode = "DYNAMIC",
            scrapeCount = 0,
            elementCount = 0,
            commandCount = 0,
            firstScrapedAt = timestamp,
            lastScrapedAt = timestamp
        )
    }
}
