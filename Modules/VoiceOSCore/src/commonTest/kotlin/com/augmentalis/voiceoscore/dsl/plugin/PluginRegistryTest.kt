package com.augmentalis.voiceoscore.dsl.plugin

import com.augmentalis.voiceoscore.dsl.interpreter.DispatchResult
import com.augmentalis.voiceoscore.dsl.interpreter.IAvuDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PluginRegistryTest {

    private val mockDispatcher = object : IAvuDispatcher {
        override suspend fun dispatch(code: String, arguments: Map<String, Any?>) =
            DispatchResult.Success()
        override fun canDispatch(code: String) = true
    }

    private fun createRegistry() = PluginRegistry(mockDispatcher)

    private fun loadTestPlugin(
        pluginId: String = "com.test.plugin",
        triggers: List<String> = listOf("test action")
    ): LoadedPlugin {
        val content = buildString {
            appendLine("---")
            appendLine("schema: avu-2.2")
            appendLine("version: 1.0.0")
            appendLine("type: plugin")
            appendLine("metadata:")
            appendLine("  plugin_id: $pluginId")
            appendLine("  name: Test Plugin")
            appendLine("codes:")
            appendLine("  CHT: Chat Message")
            appendLine("triggers:")
            for (trigger in triggers) {
                appendLine("  $trigger")
            }
            appendLine("---")
            appendLine()
            for (trigger in triggers) {
                appendLine("@on \"$trigger\"")
                appendLine("  CHT(text: \"Executed\")")
                appendLine()
            }
        }

        val result = PluginLoader.load(content, PluginTrustLevel.USER)
        assertTrue(result.isSuccess, "Failed to load test plugin: $result")
        return result.pluginOrNull()!!
    }

    @Test
    fun register_plugin_succeeds() {
        val registry = createRegistry()
        val plugin = loadTestPlugin()

        val result = registry.register(plugin)
        assertTrue(result.isSuccess)
        assertEquals(PluginState.REGISTERED, registry.getPlugin("com.test.plugin")?.state)
    }

    @Test
    fun register_duplicate_plugin_fails() {
        val registry = createRegistry()
        val plugin = loadTestPlugin()

        registry.register(plugin)
        val result = registry.register(plugin.copy(state = PluginState.VALIDATED))
        assertIs<PluginRegistrationResult.Error>(result)
    }

    @Test
    fun register_non_validated_plugin_fails() {
        val registry = createRegistry()
        val plugin = loadTestPlugin().withState(PluginState.ACTIVE)

        val result = registry.register(plugin)
        assertIs<PluginRegistrationResult.Error>(result)
    }

    @Test
    fun activate_and_deactivate_plugin() {
        val registry = createRegistry()
        val plugin = loadTestPlugin()

        registry.register(plugin)
        assertTrue(registry.activate("com.test.plugin"))
        assertEquals(PluginState.ACTIVE, registry.getPlugin("com.test.plugin")?.state)

        assertTrue(registry.deactivate("com.test.plugin"))
        assertEquals(PluginState.INACTIVE, registry.getPlugin("com.test.plugin")?.state)
    }

    @Test
    fun reactivate_from_inactive() {
        val registry = createRegistry()
        val plugin = loadTestPlugin()

        registry.register(plugin)
        registry.activate("com.test.plugin")
        registry.deactivate("com.test.plugin")
        assertTrue(registry.activate("com.test.plugin"))
        assertEquals(PluginState.ACTIVE, registry.getPlugin("com.test.plugin")?.state)
    }

    @Test
    fun unregister_removes_plugin_and_triggers() {
        val registry = createRegistry()
        val plugin = loadTestPlugin()

        registry.register(plugin)
        assertTrue(registry.unregister("com.test.plugin"))
        assertNull(registry.getPlugin("com.test.plugin"))
        assertTrue(registry.getRegisteredTriggers().isEmpty())
    }

    @Test
    fun trigger_conflict_detection() {
        val registry = createRegistry()

        val plugin1 = loadTestPlugin("com.test.plugin1", listOf("shared trigger"))
        val plugin2 = loadTestPlugin("com.test.plugin2", listOf("shared trigger"))

        registry.register(plugin1)
        val result = registry.register(plugin2)
        assertIs<PluginRegistrationResult.Conflict>(result)
    }

    @Test
    fun find_plugin_for_trigger() {
        val registry = createRegistry()
        val plugin = loadTestPlugin()

        registry.register(plugin)
        val found = registry.findPluginForTrigger("test action")
        assertNotNull(found)
        assertEquals("com.test.plugin", found.pluginId)
    }

    @Test
    fun find_plugin_for_unknown_trigger_returns_null() {
        val registry = createRegistry()
        assertNull(registry.findPluginForTrigger("unknown"))
    }

    @Test
    fun handle_trigger_on_active_plugin() = runTest {
        val registry = createRegistry()
        val plugin = loadTestPlugin()

        registry.register(plugin)
        registry.activate("com.test.plugin")

        val result = registry.handleTrigger("test action")
        assertTrue(result.isSuccess, "Expected success but got: $result")
    }

    @Test
    fun handle_trigger_on_inactive_plugin_fails() = runTest {
        val registry = createRegistry()
        val plugin = loadTestPlugin()

        registry.register(plugin)
        // NOT activated

        val result = registry.handleTrigger("test action")
        assertIs<PluginTriggerResult.Error>(result)
    }

    @Test
    fun handle_unknown_trigger_returns_NoHandler() = runTest {
        val registry = createRegistry()
        val result = registry.handleTrigger("unknown trigger")
        assertIs<PluginTriggerResult.NoHandler>(result)
    }

    @Test
    fun get_statistics() {
        val registry = createRegistry()
        val plugin = loadTestPlugin()

        registry.register(plugin)
        val stats = registry.getStatistics()
        assertEquals(1, stats[PluginState.REGISTERED])
    }

    @Test
    fun getActivePlugins_returns_only_active() {
        val registry = createRegistry()
        val plugin1 = loadTestPlugin("com.test.p1", listOf("trigger1"))
        val plugin2 = loadTestPlugin("com.test.p2", listOf("trigger2"))

        registry.register(plugin1)
        registry.register(plugin2)
        registry.activate("com.test.p1")

        assertEquals(1, registry.getActivePlugins().size)
        assertEquals(2, registry.getAllPlugins().size)
    }

    @Test
    fun clear_removes_everything() {
        val registry = createRegistry()
        val plugin = loadTestPlugin()

        registry.register(plugin)
        registry.clear()

        assertTrue(registry.getAllPlugins().isEmpty())
        assertTrue(registry.getRegisteredTriggers().isEmpty())
    }
}
