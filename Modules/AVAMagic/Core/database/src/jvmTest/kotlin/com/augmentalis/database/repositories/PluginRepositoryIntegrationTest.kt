/**
 * PluginRepositoryIntegrationTest.kt - Comprehensive Plugin repository tests
 *
 * Tests plugin lifecycle, dependencies, permissions, and checkpoint operations.
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.database.repositories

import com.augmentalis.database.dto.PluginDTO
import com.augmentalis.database.dto.PluginDependencyDTO
import com.augmentalis.database.dto.PluginPermissionDTO
import com.augmentalis.database.dto.SystemCheckpointDTO
import com.augmentalis.database.dto.PluginState
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class PluginRepositoryIntegrationTest : BaseRepositoryTest() {

    // ==================== Plugin CRUD Tests ====================

    @Test
    fun testPluginUpsertAndGet() = runTest {
        val repo = databaseManager.plugins
        val plugin = createPlugin("plugin-001", "Test Plugin")

        repo.upsertPlugin(plugin)

        val retrieved = repo.getPluginById("plugin-001")
        assertNotNull(retrieved)
        assertEquals("Test Plugin", retrieved.name)
        assertEquals(PluginState.INSTALLED, retrieved.state)
    }

    @Test
    fun testPluginUpdate() = runTest {
        val repo = databaseManager.plugins
        val plugin = createPlugin("plugin-001", "Original")

        repo.upsertPlugin(plugin)

        repo.updateState("plugin-001", PluginState.ENABLED, now())

        val updated = repo.getPluginById("plugin-001")
        assertEquals(PluginState.ENABLED, updated?.state)
    }

    @Test
    fun testPluginDeletion() = runTest {
        val repo = databaseManager.plugins

        repo.upsertPlugin(createPlugin("plugin-001", "Test"))

        assertNotNull(repo.getPluginById("plugin-001"))

        repo.deletePlugin("plugin-001")

        assertNull(repo.getPluginById("plugin-001"))
    }

    // ==================== Plugin State & Filtering Tests ====================

    @Test
    fun testGetPluginsByState() = runTest {
        val repo = databaseManager.plugins

        repo.upsertPlugin(createPlugin("p1", "P1").copy(state = PluginState.INSTALLED))
        repo.upsertPlugin(createPlugin("p2", "P2").copy(state = PluginState.ENABLED))
        repo.upsertPlugin(createPlugin("p3", "P3").copy(state = PluginState.INSTALLED))

        val installed = repo.getPluginsByState(PluginState.INSTALLED)
        assertEquals(2, installed.size)
        assertTrue(installed.all { it.state == PluginState.INSTALLED })
    }

    @Test
    fun testGetEnabledPlugins() = runTest {
        val repo = databaseManager.plugins

        repo.upsertPlugin(createPlugin("p1", "P1").copy(enabled = true))
        repo.upsertPlugin(createPlugin("p2", "P2").copy(enabled = false))
        repo.upsertPlugin(createPlugin("p3", "P3").copy(enabled = true))

        val enabled = repo.getEnabledPlugins()
        assertEquals(2, enabled.size)
        assertTrue(enabled.all { it.enabled })
    }

    @Test
    fun testSearchByName() = runTest {
        val repo = databaseManager.plugins

        repo.upsertPlugin(createPlugin("p1", "Audio Recorder"))
        repo.upsertPlugin(createPlugin("p2", "Video Player"))
        repo.upsertPlugin(createPlugin("p3", "Audio Enhancer"))

        val audioPlugins = repo.searchByName("%Audio%")
        assertEquals(2, audioPlugins.size)
        assertTrue(audioPlugins.all { it.name.contains("Audio") })
    }

    // ==================== Dependency Tests ====================

    @Test
    fun testPluginDependencies() = runTest {
        val repo = databaseManager.plugins

        repo.upsertPlugin(createPlugin("base", "Base Plugin"))
        repo.upsertPlugin(createPlugin("dependent", "Dependent Plugin"))

        val dependency = PluginDependencyDTO(null, "dependent", "base", ">=1.0.0", false)
        repo.insertDependency(dependency)

        val deps = repo.getDependencies("dependent")
        assertEquals(1, deps.size)
        assertEquals("base", deps[0].dependsOnPluginId)

        val dependents = repo.getDependents("base")
        assertEquals(1, dependents.size)
    }

    @Test
    fun testGetRequiredDependencies() = runTest {
        val repo = databaseManager.plugins

        repo.upsertPlugin(createPlugin("plugin", "Plugin"))
        repo.upsertPlugin(createPlugin("req", "Required"))
        repo.upsertPlugin(createPlugin("opt", "Optional"))

        repo.insertDependency(PluginDependencyDTO(null, "plugin", "req", ">=1.0.0", false))
        repo.insertDependency(PluginDependencyDTO(null, "plugin", "opt", ">=1.0.0", true))

        val required = repo.getRequiredDependencies("plugin")
        assertEquals(1, required.size)
        assertEquals("req", required[0].dependsOnPluginId)
        assertFalse(required[0].isOptional)
    }

    @Test
    fun testDeleteDependenciesForPlugin() = runTest {
        val repo = databaseManager.plugins

        repo.upsertPlugin(createPlugin("plugin", "Plugin"))
        repo.upsertPlugin(createPlugin("dep1", "Dep1"))
        repo.upsertPlugin(createPlugin("dep2", "Dep2"))

        repo.insertDependency(PluginDependencyDTO(null, "plugin", "dep1", null, false))
        repo.insertDependency(PluginDependencyDTO(null, "plugin", "dep2", null, false))

        assertEquals(2, repo.countDependencies("plugin"))

        repo.deleteDependenciesForPlugin("plugin")

        assertEquals(0, repo.countDependencies("plugin"))
    }

    // ==================== Permission Tests ====================

    @Test
    fun testPluginPermissions() = runTest {
        val repo = databaseManager.plugins

        repo.upsertPlugin(createPlugin("plugin-001", "Test"))

        val perm = PluginPermissionDTO(null, "plugin-001", "android.permission.CAMERA", false, null, null)
        repo.insertPermission(perm)

        val perms = repo.getPermissions("plugin-001")
        assertEquals(1, perms.size)
        assertFalse(perms[0].granted)
    }

    @Test
    fun testGrantAndRevokePermission() = runTest {
        val repo = databaseManager.plugins

        repo.upsertPlugin(createPlugin("plugin-001", "Test"))
        repo.insertPermission(PluginPermissionDTO(null, "plugin-001", "android.permission.CAMERA", false, null, null))

        // Grant
        repo.grantPermission("plugin-001", "android.permission.CAMERA", now(), "USER")

        var granted = repo.getGrantedPermissions("plugin-001")
        assertEquals(1, granted.size)

        // Revoke
        repo.revokePermission("plugin-001", "android.permission.CAMERA")

        granted = repo.getGrantedPermissions("plugin-001")
        assertEquals(0, granted.size)
    }

    @Test
    fun testGetGrantedPermissions() = runTest {
        val repo = databaseManager.plugins

        repo.upsertPlugin(createPlugin("plugin-001", "Test"))

        repo.insertPermission(PluginPermissionDTO(null, "plugin-001", "android.permission.CAMERA", true, now(), "USER"))
        repo.insertPermission(PluginPermissionDTO(null, "plugin-001", "android.permission.LOCATION", false, null, null))
        repo.insertPermission(PluginPermissionDTO(null, "plugin-001", "android.permission.STORAGE", true, now(), "USER"))

        val granted = repo.getGrantedPermissions("plugin-001")
        assertEquals(2, granted.size)
        assertTrue(granted.all { it.granted })
    }

    @Test
    fun testDeletePermissionsForPlugin() = runTest {
        val repo = databaseManager.plugins

        repo.upsertPlugin(createPlugin("plugin-001", "Test"))

        repo.insertPermission(PluginPermissionDTO(null, "plugin-001", "android.permission.CAMERA", false, null, null))
        repo.insertPermission(PluginPermissionDTO(null, "plugin-001", "android.permission.LOCATION", false, null, null))

        assertEquals(2, repo.countPermissions("plugin-001"))

        repo.deletePermissionsForPlugin("plugin-001")

        assertEquals(0, repo.countPermissions("plugin-001"))
    }

    // ==================== Checkpoint Tests ====================

    @Test
    fun testCheckpointInsertAndGet() = runTest {
        val repo = databaseManager.plugins

        val checkpoint = SystemCheckpointDTO(
            id = "cp-001",
            name = "Before Update",
            description = "System state",
            createdAt = now(),
            stateJson = "{\"version\": \"1.0\"}",
            pluginStatesJson = "{}"
        )

        repo.insertCheckpoint(checkpoint)

        val retrieved = repo.getCheckpointById("cp-001")
        assertNotNull(retrieved)
        assertEquals("Before Update", retrieved.name)
    }

    @Test
    fun testGetLatestCheckpoint() = runTest {
        val repo = databaseManager.plugins

        repo.insertCheckpoint(SystemCheckpointDTO("cp-001", "Old", null, past(10000), "{}", "{}"))
        repo.insertCheckpoint(SystemCheckpointDTO("cp-002", "Latest", null, now(), "{}", "{}"))

        val latest = repo.getLatestCheckpoint()
        assertEquals("cp-002", latest?.id)
    }

    @Test
    fun testGetRecentCheckpoints() = runTest {
        val repo = databaseManager.plugins

        repo.insertCheckpoint(SystemCheckpointDTO("cp-001", "CP1", null, past(10000), "{}", "{}"))
        repo.insertCheckpoint(SystemCheckpointDTO("cp-002", "CP2", null, past(5000), "{}", "{}"))
        repo.insertCheckpoint(SystemCheckpointDTO("cp-003", "CP3", null, now(), "{}", "{}"))

        val recent = repo.getRecentCheckpoints(2)
        assertEquals(2, recent.size)
        assertEquals("cp-003", recent[0].id) // Most recent first
    }

    @Test
    fun testDeleteOldCheckpointsTest() = runTest {
        val repo = databaseManager.plugins

        val cutoffTime = now() - (7 * 24 * 60 * 60 * 1000) // 7 days ago

        repo.insertCheckpoint(SystemCheckpointDTO("cp-old", "Old", null, past(10 * 24 * 60 * 60 * 1000), "{}", "{}"))
        repo.insertCheckpoint(SystemCheckpointDTO("cp-recent", "Recent", null, now(), "{}", "{}"))

        assertEquals(2, repo.countCheckpoints())

        repo.deleteOldCheckpoints(cutoffTime)

        assertEquals(1, repo.countCheckpoints())
        assertNull(repo.getCheckpointById("cp-old"))
        assertNotNull(repo.getCheckpointById("cp-recent"))
    }

    // ==================== Complex Scenario Tests ====================

    @Test
    fun testPluginLifecycleWithDependencies() = runTest {
        val repo = databaseManager.plugins

        // Create base plugin
        repo.upsertPlugin(createPlugin("base", "Base Library"))
        repo.updateState("base", PluginState.ENABLED, now())

        // Create dependent plugin
        repo.upsertPlugin(createPlugin("feature", "Feature Plugin").copy(state = PluginState.INSTALLING))

        // Add dependency
        repo.insertDependency(PluginDependencyDTO(null, "feature", "base", ">=1.0.0", false))

        // Add permission
        repo.insertPermission(PluginPermissionDTO(null, "feature", "android.permission.CAMERA", false, null, null))
        repo.grantPermission("feature", "android.permission.CAMERA", now(), "USER")

        // Complete installation
        repo.updateState("feature", PluginState.INSTALLED, now())
        repo.updateEnabled("feature", true, now())

        // Verify
        val plugin = repo.getPluginById("feature")
        assertEquals(PluginState.INSTALLED, plugin?.state)
        assertTrue(plugin?.enabled ?: false)

        assertEquals(1, repo.getDependencies("feature").size)
        assertEquals(1, repo.getGrantedPermissions("feature").size)
    }

    // ==================== Helpers ====================

    private fun createPlugin(
        id: String,
        name: String,
        version: String = "1.0.0",
        state: PluginState = PluginState.INSTALLED
    ): PluginDTO {
        val timestamp = now()
        return PluginDTO(
            id = id,
            name = name,
            version = version,
            description = "Test plugin",
            author = "Test Author",
            state = state,
            enabled = true,
            installPath = "/plugins/$id",
            installedAt = timestamp,
            updatedAt = timestamp,
            configJson = "{}"
        )
    }
}
