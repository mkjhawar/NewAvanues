package com.augmentalis.magiccode.plugins

import com.augmentalis.magiccode.plugins.core.PluginAssets
import com.augmentalis.magiccode.plugins.core.PluginDependency
import com.augmentalis.magiccode.plugins.core.PluginManifest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PluginManifestTest {

    private fun minimalManifest(
        id: String = "com.example.plugin",
        version: String = "1.0.0"
    ) = PluginManifest(
        id = id,
        name = "Example Plugin",
        version = version,
        author = "Manoj Jhawar",
        entrypoint = "com.example.plugin.ExamplePlugin",
        source = "THIRD_PARTY",
        verificationLevel = "UNVERIFIED"
    )

    // ─── PluginManifest tests ──────────────────────────────────────────────────

    @Test
    fun defaultFieldsAreCorrect() {
        val manifest = minimalManifest()

        assertEquals("1.0", manifest.manifestVersion)
        assertEquals(emptyList(), manifest.capabilities)
        assertEquals(emptyList(), manifest.dependencies)
        assertEquals(emptyList(), manifest.permissions)
        assertEquals(emptyMap(), manifest.permissionRationales)
        assertNull(manifest.description)
        assertNull(manifest.assets)
        assertNull(manifest.homepage)
        assertNull(manifest.license)
    }

    @Test
    fun dependencyOptionalDefaultIsFalse() {
        val dep = PluginDependency(pluginId = "com.other.plugin", version = "^1.0.0")
        assertEquals(false, dep.optional)
    }

    @Test
    fun assetsDefaultsAreEmptyLists() {
        val assets = PluginAssets()

        assertTrue(assets.images.isEmpty())
        assertTrue(assets.fonts.isEmpty())
        assertTrue(assets.icons.isEmpty())
        assertTrue(assets.themes.isEmpty())
        assertTrue(assets.custom.isEmpty())
    }

    @Test
    fun manifestWithCapabilitiesAndPermissionsStoredCorrectly() {
        val manifest = minimalManifest().copy(
            capabilities = listOf("nlp.sentiment", "ui_components"),
            permissions = listOf("NETWORK", "STORAGE_READ"),
            permissionRationales = mapOf("NETWORK" to "Download updates")
        )

        assertEquals(2, manifest.capabilities.size)
        assertTrue(manifest.capabilities.contains("nlp.sentiment"))
        assertEquals("Download updates", manifest.permissionRationales["NETWORK"])
    }

    @Test
    fun dependencyWithAllFields() {
        val dep = PluginDependency(
            pluginId = "com.augmentalis.base",
            version = "~2.3.0",
            optional = true
        )

        assertEquals("com.augmentalis.base", dep.pluginId)
        assertEquals("~2.3.0", dep.version)
        assertEquals(true, dep.optional)
    }
}
