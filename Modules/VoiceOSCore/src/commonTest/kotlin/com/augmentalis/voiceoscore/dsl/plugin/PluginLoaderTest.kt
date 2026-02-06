package com.augmentalis.voiceoscore.dsl.plugin

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PluginLoaderTest {

    private val validAvpContent = """
---
schema: avu-2.2
version: 1.0.0
type: plugin
metadata:
  plugin_id: com.augmentalis.test
  name: Test Plugin
  author: Augmentalis Engineering
codes:
  VCM: Voice Command
  CHT: Chat Message
permissions:
  GESTURES
triggers:
  test action
  do something
---

@define run_test()
  VCM(action: "test")
  CHT(text: "Test executed")

@on "test action"
  run_test()

@on "do something"
  CHT(text: "Done!")
""".trimIndent()

    @Test
    fun load_valid_plugin_succeeds() {
        val result = PluginLoader.load(validAvpContent)
        assertTrue(result.isSuccess, "Expected success but got: $result")
        val plugin = result.pluginOrNull()!!
        assertEquals("com.augmentalis.test", plugin.pluginId)
        assertEquals("Test Plugin", plugin.name)
        assertEquals(PluginState.VALIDATED, plugin.state)
    }

    @Test
    fun load_extracts_manifest_correctly() {
        val plugin = (PluginLoader.load(validAvpContent) as PluginLoadResult.Success).plugin
        val manifest = plugin.manifest
        assertEquals("com.augmentalis.test", manifest.pluginId)
        assertEquals("Test Plugin", manifest.name)
        assertEquals("1.0.0", manifest.version)
        assertEquals("Augmentalis Engineering", manifest.author)
        assertTrue(PluginPermission.GESTURES in manifest.permissions)
        assertEquals(2, manifest.triggers.size)
        assertTrue("test action" in manifest.triggers)
        assertTrue("do something" in manifest.triggers)
    }

    @Test
    fun load_assigns_system_trust_for_augmentalis_plugins() {
        val plugin = (PluginLoader.load(validAvpContent) as PluginLoadResult.Success).plugin
        // System trust = SYSTEM sandbox = 60s execution time
        assertEquals(60_000L, plugin.sandboxConfig.maxExecutionTimeMs)
    }

    @Test
    fun load_rejects_workflow_type_files() {
        val workflowContent = validAvpContent.replace("type: plugin", "type: workflow")
        val result = PluginLoader.load(workflowContent)
        assertIs<PluginLoadResult.ValidationError>(result)
    }

    @Test
    fun load_rejects_invalid_syntax() {
        val badContent = "this is not valid AVU DSL"
        val result = PluginLoader.load(badContent)
        assertIs<PluginLoadResult.ParseError>(result)
    }

    @Test
    fun load_rejects_permission_violations() {
        // CAM code requires CAMERA permission which is not declared
        val withCamera = validAvpContent.replace(
            "  VCM: Voice Command",
            "  VCM: Voice Command\n  CAM: Camera Access"
        )
        val result = PluginLoader.load(withCamera)
        assertIs<PluginLoadResult.PermissionError>(result,
            "Expected PermissionError but got: $result")
    }

    @Test
    fun load_with_explicit_trust_level_overrides() {
        val plugin = (PluginLoader.load(
            validAvpContent, PluginTrustLevel.UNTRUSTED
        ) as PluginLoadResult.Success).plugin
        // UNTRUSTED → STRICT sandbox = 5s execution time
        assertEquals(5_000L, plugin.sandboxConfig.maxExecutionTimeMs)
    }

    @Test
    fun load_user_plugin_gets_USER_trust() {
        val userContent = validAvpContent
            .replace("com.augmentalis.test", "com.thirdparty.myplugin")
            .replace("author: Augmentalis Engineering", "author: Third Party Dev")
        val plugin = (PluginLoader.load(userContent) as PluginLoadResult.Success).plugin
        // USER trust = 8s execution time
        assertEquals(8_000L, plugin.sandboxConfig.maxExecutionTimeMs)
    }

    @Test
    fun pluginOrNull_returns_null_on_error() {
        val result = PluginLoader.load("invalid")
        assertFalse(result.isSuccess)
        assertEquals(null, result.pluginOrNull())
    }
}
