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
import com.augmentalis.database.dto.UUIDElementDTO
import com.augmentalis.database.dto.UUIDHierarchyDTO
import com.augmentalis.database.dto.UUIDAnalyticsDTO
import com.augmentalis.database.dto.UUIDAliasDTO
import com.augmentalis.database.dto.ScrapedAppDTO
import com.augmentalis.database.dto.VoiceCommandDTO
import com.augmentalis.database.dto.plugin.PluginDTO
import com.augmentalis.database.dto.plugin.PluginDependencyDTO
import com.augmentalis.database.dto.plugin.PluginPermissionDTO
import com.augmentalis.database.dto.plugin.SystemCheckpointDTO
import com.augmentalis.database.dto.plugin.PluginState
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Comprehensive integration tests for all four repositories:
 * - IUUIDRepository
 * - IScrapedAppRepository
 * - IVoiceCommandRepository
 * - IPluginRepository
 */
class RepositoryIntegrationTest {

    private lateinit var databaseManager: VoiceOSDatabaseManager

    @BeforeTest
    fun setup() {
        val driverFactory = DatabaseDriverFactory()
        databaseManager = VoiceOSDatabaseManager(driverFactory)
    }

    private fun now() = System.currentTimeMillis()
    private fun past(millis: Long) = now() - millis

    // ==================== UUID Repository Tests ====================

    @Test
    fun testUuidElementCRUD() = runTest {
        val repo = databaseManager.uuids
        val element = UUIDElementDTO(
            uuid = "elem-001",
            name = "Test Button",
            type = "BUTTON",
            description = "Test",
            parentUuid = null,
            isEnabled = true,
            priority = 0,
            timestamp = now(),
            metadataJson = null,
            positionJson = null
        )

        // Insert
        repo.insertElement(element)

        // Get
        val retrieved = repo.getElementByUuid("elem-001")
        assertNotNull(retrieved)
        assertEquals("Test Button", retrieved.name)

        // Update
        repo.updateElement(element.copy(name = "Updated"))
        val updated = repo.getElementByUuid("elem-001")
        assertEquals("Updated", updated?.name)

        // Delete
        repo.deleteElement("elem-001")
        assertNull(repo.getElementByUuid("elem-001"))
    }

    @Test
    fun testUuidAliaOperations() = runTest {
        val repo = databaseManager.uuids

        // Insert element first
        repo.insertElement(UUIDElementDTO("elem-001", "Test", "BUTTON", null, null, true, 0, now(), null, null))

        // Insert alias
        val alias = UUIDAliasDTO(0, "my-alias", "elem-001", true, now())
        repo.insertAlias(alias)

        // Get alias
        assertTrue(repo.aliasExists("my-alias"))
        assertEquals("elem-001", repo.getUuidByAlias("my-alias"))

        // Delete alias
        repo.deleteAliasByName("my-alias")
        assertFalse(repo.aliasExists("my-alias"))
    }

    @Test
    fun testUuidAnalyticsTracking() = runTest {
        val repo = databaseManager.uuids

        // Insert element
        repo.insertElement(UUIDElementDTO("elem-001", "Test", "BUTTON", null, null, true, 0, now(), null, null))

        // Insert analytics
        val analytics = UUIDAnalyticsDTO("elem-001", 0, now(), now(), 100, 0, 0, "ACTIVE")
        repo.insertAnalytics(analytics)

        // Increment access
        repo.incrementAccessCount("elem-001", now())

        val retrieved = repo.getAnalyticsByUuid("elem-001")
        assertEquals(1, retrieved?.accessCount)
    }

    @Test
    fun testUuidHierarchyManagement() = runTest {
        val repo = databaseManager.uuids

        // Insert parent and child elements
        repo.insertElement(UUIDElementDTO("parent", "Parent", "CONTAINER", null, null, true, 0, now(), null, null))
        repo.insertElement(UUIDElementDTO("child", "Child", "BUTTON", null, "parent", true, 0, now(), null, null))

        // Insert hierarchy
        val hierarchy = UUIDHierarchyDTO(0, "parent", "child", 1, "/parent/child", 0)
        repo.insertHierarchy(hierarchy)

        val children = repo.getHierarchyByParent("parent")
        assertEquals(1, children.size)
        assertEquals("child", children[0].childUuid)
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
            action = "OPEN_SETTINGS",
            category = "SYSTEM",
            priority = 0,
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
        repo.insert(VoiceCommandDTO(0, "nav-home", "en-US", "go home", "NAV", "NAV", 0, 1, timestamp, timestamp))
        repo.insert(VoiceCommandDTO(0, "nav-home", "es-ES", "ir a inicio", "NAV", "NAV", 0, 1, timestamp, timestamp))
        repo.insert(VoiceCommandDTO(0, "nav-home", "fr-FR", "aller à accueil", "NAV", "NAV", 0, 1, timestamp, timestamp))

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

        repo.insert(VoiceCommandDTO(0, "cmd-001", "en-US", "open settings", "ACT", "SYS", 0, 1, timestamp, timestamp))
        repo.insert(VoiceCommandDTO(0, "cmd-002", "en-US", "open browser", "ACT", "SYS", 0, 1, timestamp, timestamp))
        repo.insert(VoiceCommandDTO(0, "cmd-003", "en-US", "close window", "ACT", "SYS", 0, 1, timestamp, timestamp))

        val openCommands = repo.searchByTrigger("%open%")
        assertEquals(2, openCommands.size)
    }

    // ==================== Plugin Repository Tests ====================

    @Test
    fun testPluginCRUD() = runTest {
        val repo = databaseManager.plugins
        val timestamp = now()
        val plugin = PluginDTO(
            id = "plugin-001",
            name = "Test Plugin",
            version = "1.0.0",
            description = "Test",
            author = "Test Author",
            state = PluginState.INSTALLED,
            enabled = true,
            installPath = "/plugins/test",
            installedAt = timestamp,
            updatedAt = timestamp,
            configJson = "{}"
        )

        // Upsert
        repo.upsertPlugin(plugin)

        // Get
        val retrieved = repo.getPluginById("plugin-001")
        assertNotNull(retrieved)
        assertEquals("Test Plugin", retrieved.name)

        // Update state
        repo.updateState("plugin-001", PluginState.ENABLED, now())
        val updated = repo.getPluginById("plugin-001")
        assertEquals(PluginState.ENABLED, updated?.state)

        // Delete
        repo.deletePlugin("plugin-001")
        assertNull(repo.getPluginById("plugin-001"))
    }

    @Test
    fun testPluginDependencies() = runTest {
        val repo = databaseManager.plugins
        val timestamp = now()

        // Create plugins
        repo.upsertPlugin(PluginDTO("base", "Base", "1.0", null, null, PluginState.INSTALLED, true, "/base", timestamp, timestamp, null))
        repo.upsertPlugin(PluginDTO("dependent", "Dependent", "1.0", null, null, PluginState.INSTALLED, true, "/dep", timestamp, timestamp, null))

        // Add dependency
        val dependency = PluginDependencyDTO(null, "dependent", "base", ">=1.0.0", false)
        repo.insertDependency(dependency)

        val deps = repo.getDependencies("dependent")
        assertEquals(1, deps.size)
        assertEquals("base", deps[0].dependsOnPluginId)

        // Get dependents
        val dependents = repo.getDependents("base")
        assertEquals(1, dependents.size)
    }

    @Test
    fun testPluginPermissions() = runTest {
        val repo = databaseManager.plugins
        val timestamp = now()

        repo.upsertPlugin(PluginDTO("plugin-001", "Test", "1.0", null, null, PluginState.INSTALLED, true, "/test", timestamp, timestamp, null))

        // Insert permission
        val perm = PluginPermissionDTO(null, "plugin-001", "android.permission.CAMERA", false, null, null)
        repo.insertPermission(perm)

        // Grant permission
        repo.grantPermission("plugin-001", "android.permission.CAMERA", now(), "USER")

        val granted = repo.getGrantedPermissions("plugin-001")
        assertEquals(1, granted.size)
        assertTrue(granted[0].granted)

        // Revoke
        repo.revokePermission("plugin-001", "android.permission.CAMERA")
        assertEquals(0, repo.getGrantedPermissions("plugin-001").size)
    }

    @Test
    fun testPluginCheckpoints() = runTest {
        val repo = databaseManager.plugins
        val timestamp = now()

        val checkpoint = SystemCheckpointDTO(
            id = "cp-001",
            name = "Test Checkpoint",
            description = "Before update",
            createdAt = timestamp,
            stateJson = "{}",
            pluginStatesJson = "{}"
        )

        repo.insertCheckpoint(checkpoint)

        val retrieved = repo.getCheckpointById("cp-001")
        assertNotNull(retrieved)
        assertEquals("Test Checkpoint", retrieved.name)

        // Update
        repo.updateCheckpoint(checkpoint.copy(name = "Updated"))
        val updated = repo.getCheckpointById("cp-001")
        assertEquals("Updated", updated?.name)
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
