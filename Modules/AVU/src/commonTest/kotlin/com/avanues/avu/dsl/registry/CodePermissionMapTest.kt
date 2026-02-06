package com.avanues.avu.dsl.registry

import com.avanues.avu.dsl.plugin.PluginPermission
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CodePermissionMapTest {

    @Test
    fun VCM_requires_GESTURES_permission() {
        val perms = CodePermissionMap.getRequiredPermissions("VCM")
        assertTrue(PluginPermission.GESTURES in perms)
    }

    @Test
    fun QRY_requires_no_permissions() {
        val perms = CodePermissionMap.getRequiredPermissions("QRY")
        assertTrue(perms.isEmpty())
    }

    @Test
    fun CHT_requires_no_permissions() {
        val perms = CodePermissionMap.getRequiredPermissions("CHT")
        assertTrue(perms.isEmpty())
    }

    @Test
    fun unknown_code_returns_empty_permissions() {
        val perms = CodePermissionMap.getRequiredPermissions("ZZZ")
        assertTrue(perms.isEmpty())
    }

    @Test
    fun AAC_requires_ACCESSIBILITY_and_GESTURES() {
        val perms = CodePermissionMap.getRequiredPermissions("AAC")
        assertTrue(PluginPermission.ACCESSIBILITY in perms)
        assertTrue(PluginPermission.GESTURES in perms)
    }

    @Test
    fun CAM_requires_CAMERA() {
        val perms = CodePermissionMap.getRequiredPermissions("CAM")
        assertTrue(PluginPermission.CAMERA in perms)
    }

    @Test
    fun validation_passes_when_all_permissions_granted() {
        val result = CodePermissionMap.validateCodePermissions(
            declaredCodes = setOf("VCM", "QRY", "CHT"),
            grantedPermissions = setOf(PluginPermission.GESTURES)
        )
        assertTrue(result.isValid)
        assertTrue(result.violations.isEmpty())
    }

    @Test
    fun validation_fails_when_permission_missing() {
        val result = CodePermissionMap.validateCodePermissions(
            declaredCodes = setOf("VCM", "CAM"),
            grantedPermissions = setOf(PluginPermission.GESTURES)
        )
        assertFalse(result.isValid)
        assertEquals(1, result.violations.size)
        assertEquals("CAM", result.violations[0].code)
        assertTrue(PluginPermission.CAMERA in result.violations[0].missingPermissions)
    }

    @Test
    fun validation_reports_multiple_violations() {
        val result = CodePermissionMap.validateCodePermissions(
            declaredCodes = setOf("CAM", "MIC", "NET"),
            grantedPermissions = emptySet()
        )
        assertFalse(result.isValid)
        assertEquals(3, result.violations.size)
    }

    @Test
    fun custom_mapping_overrides_default() {
        CodePermissionMap.registerCustomMapping("XYZ", setOf(PluginPermission.NETWORK))
        try {
            val perms = CodePermissionMap.getRequiredPermissions("XYZ")
            assertTrue(PluginPermission.NETWORK in perms)
        } finally {
            CodePermissionMap.clearCustomMappings()
        }
    }
}
