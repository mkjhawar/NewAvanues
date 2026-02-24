package com.augmentalis.magiccode.plugins

import com.augmentalis.magiccode.plugins.security.DefaultPluginSandbox
import com.augmentalis.magiccode.plugins.security.PermissionDeniedException
import com.augmentalis.magiccode.plugins.security.PluginPermission
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class DefaultPluginSandboxTest {

    private val pluginId = "com.example.test.plugin"

    // ─── DefaultPluginSandbox tests ────────────────────────────────────────────

    @Test
    fun newSandboxHasNoPermissions() {
        val sandbox = DefaultPluginSandbox()

        assertFalse(sandbox.checkPermission(pluginId, PluginPermission.NETWORK_ACCESS))
        assertTrue(sandbox.getGrantedPermissions(pluginId).isEmpty())
    }

    @Test
    fun grantAndCheckPermission() {
        val sandbox = DefaultPluginSandbox()

        sandbox.grantPermission(pluginId, PluginPermission.NETWORK_ACCESS)

        assertTrue(sandbox.checkPermission(pluginId, PluginPermission.NETWORK_ACCESS))
        assertFalse(sandbox.checkPermission(pluginId, PluginPermission.FILE_SYSTEM_WRITE))
    }

    @Test
    fun enforcePermissionThrowsWhenNotGranted() {
        val sandbox = DefaultPluginSandbox()

        val ex = assertFailsWith<PermissionDeniedException> {
            sandbox.enforcePermission(pluginId, PluginPermission.ACCESSIBILITY_DATA)
        }
        assertEquals(pluginId, ex.pluginId)
        assertEquals(PluginPermission.ACCESSIBILITY_DATA, ex.permission)
    }

    @Test
    fun revokePermissionRemovesAccess() {
        val sandbox = DefaultPluginSandbox()
        sandbox.grantPermission(pluginId, PluginPermission.NOTIFICATIONS)
        sandbox.revokePermission(pluginId, PluginPermission.NOTIFICATIONS)

        assertFalse(sandbox.checkPermission(pluginId, PluginPermission.NOTIFICATIONS))
    }

    @Test
    fun hasAllPermissionsAndHasAnyPermission() {
        val sandbox = DefaultPluginSandbox()
        sandbox.grantPermission(pluginId, PluginPermission.NETWORK_ACCESS)
        sandbox.grantPermission(pluginId, PluginPermission.FILE_SYSTEM_READ)

        assertTrue(
            sandbox.hasAllPermissions(
                pluginId,
                setOf(PluginPermission.NETWORK_ACCESS, PluginPermission.FILE_SYSTEM_READ)
            )
        )
        assertFalse(
            sandbox.hasAllPermissions(
                pluginId,
                setOf(PluginPermission.NETWORK_ACCESS, PluginPermission.FILE_SYSTEM_WRITE)
            )
        )
        assertTrue(
            sandbox.hasAnyPermission(
                pluginId,
                setOf(PluginPermission.FILE_SYSTEM_WRITE, PluginPermission.NETWORK_ACCESS)
            )
        )
    }

    @Test
    fun revokeAllClearsAllPermissions() {
        val sandbox = DefaultPluginSandbox()
        sandbox.grantPermission(pluginId, PluginPermission.NETWORK_ACCESS)
        sandbox.grantPermission(pluginId, PluginPermission.NOTIFICATIONS)
        sandbox.revokeAllPermissions(pluginId)

        assertTrue(sandbox.getGrantedPermissions(pluginId).isEmpty())
    }

    @Test
    fun separatePluginsHaveIndependentPermissions() {
        val sandbox = DefaultPluginSandbox()
        val pluginA = "com.example.plugin.a"
        val pluginB = "com.example.plugin.b"

        sandbox.grantPermission(pluginA, PluginPermission.DEVICE_INFO)

        assertTrue(sandbox.checkPermission(pluginA, PluginPermission.DEVICE_INFO))
        assertFalse(sandbox.checkPermission(pluginB, PluginPermission.DEVICE_INFO))
    }
}
