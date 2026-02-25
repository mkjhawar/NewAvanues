/**
 * DiscoveryIntegrationTest.kt - Plugin discovery integration tests
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2026-01-22
 *
 * Comprehensive integration tests for plugin discovery including:
 * - BuiltinPluginDiscovery tests
 * - CompositePluginDiscovery tests
 * - Priority-based deduplication
 * - PluginManifestReader tests
 * - Discovery error handling
 */
package com.augmentalis.magiccode.plugins.integration

import com.augmentalis.magiccode.plugins.discovery.*
import com.augmentalis.magiccode.plugins.universal.*
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Integration tests for plugin discovery functionality.
 *
 * Tests discovery sources, composite discovery, manifest parsing,
 * and error handling.
 */
class DiscoveryIntegrationTest {

    // =========================================================================
    // BuiltinPluginDiscovery Tests
    // =========================================================================

    @Test
    fun testBuiltinDiscovery() = runBlocking {
        // Arrange
        val discovery = BuiltinPluginDiscovery()

        // Register test plugins
        val plugin1 = TestPlugin(pluginId = "builtin.test.1")
        val plugin2 = TestPlugin(pluginId = "builtin.test.2")
        discovery.registerBuiltin { plugin1 }
        discovery.registerBuiltin { plugin2 }

        // Act
        val descriptors = discovery.discoverPlugins()

        // Assert
        assertEquals(2, descriptors.size, "Should discover 2 plugins")
        assertTrue(
            descriptors.any { it.pluginId == "builtin.test.1" },
            "Should find builtin.test.1"
        )
        assertTrue(
            descriptors.any { it.pluginId == "builtin.test.2" },
            "Should find builtin.test.2"
        )
    }

    @Test
    fun testBuiltinPluginLoading() = runBlocking {
        // Arrange
        val discovery = BuiltinPluginDiscovery()
        val plugin = TestPlugin(pluginId = "builtin.loadable")
        discovery.registerBuiltin { plugin }

        // Act: Discover and load
        val descriptors = discovery.discoverPlugins()
        val descriptor = descriptors.first()
        val loadResult = discovery.loadPlugin(descriptor)

        // Assert
        assertTrue(loadResult.isSuccess, "Plugin should load successfully")
        assertEquals(plugin, loadResult.getOrNull(), "Loaded plugin should be the same instance")
    }

    @Test
    fun testBuiltinDiscoveryPriority() = runBlocking {
        // Arrange
        val discovery = BuiltinPluginDiscovery()

        // Assert
        assertEquals(BuiltinPluginDiscovery.PRIORITY_BUILTIN, discovery.priority)
        assertEquals(0, discovery.priority, "Builtin priority should be 0 (highest)")
    }

    @Test
    fun testBuiltinPluginDescriptorProperties() = runBlocking {
        // Arrange
        val discovery = BuiltinPluginDiscovery()
        val plugin = TestPlugin(pluginId = "builtin.properties")
        discovery.registerBuiltin { plugin }

        // Act
        val descriptors = discovery.discoverPlugins()
        val descriptor = descriptors.first()

        // Assert
        assertEquals("builtin.properties", descriptor.pluginId)
        assertEquals("Test Plugin: builtin.properties", descriptor.name)
        assertEquals("1.0.0", descriptor.version)
        assertEquals(PluginSource.Builtin, descriptor.source)
        assertTrue(descriptor.capabilities.isNotEmpty(), "Should have capabilities")
    }

    @Test
    fun testBuiltinPluginUnregistration() = runBlocking {
        // Arrange
        val discovery = BuiltinPluginDiscovery()
        val plugin = TestPlugin(pluginId = "builtin.unregister")
        discovery.registerBuiltin { plugin }

        // Verify registered
        var descriptors = discovery.discoverPlugins()
        assertEquals(1, descriptors.size)

        // Act: Unregister
        val removed = discovery.unregister("builtin.unregister")

        // Assert
        assertTrue(removed, "Should return true for successful unregistration")
        descriptors = discovery.discoverPlugins()
        assertTrue(descriptors.isEmpty(), "Should have no plugins after unregistration")
    }

    @Test
    fun testBuiltinPluginClear() = runBlocking {
        // Arrange
        val discovery = BuiltinPluginDiscovery()
        discovery.registerBuiltin { TestPlugin(pluginId = "clear.1") }
        discovery.registerBuiltin { TestPlugin(pluginId = "clear.2") }
        discovery.registerBuiltin { TestPlugin(pluginId = "clear.3") }

        assertEquals(3, discovery.discoverPlugins().size)

        // Act
        discovery.clear()

        // Assert
        assertTrue(discovery.discoverPlugins().isEmpty(), "Should be empty after clear")
    }

    // =========================================================================
    // CompositePluginDiscovery Tests
    // =========================================================================

    @Test
    fun testCompositeDiscovery() = runBlocking {
        // Arrange
        val builtinDiscovery = BuiltinPluginDiscovery()
        builtinDiscovery.registerBuiltin { TestPlugin(pluginId = "builtin.plugin") }

        val testDiscovery = TestPluginDiscovery(priority = 100)
        testDiscovery.addDescriptor(
            PluginDescriptor(
                pluginId = "test.plugin",
                name = "Test Plugin",
                version = "1.0.0",
                capabilities = setOf("test.capability"),
                source = PluginSource.FileSystem("/test/path")
            )
        )

        val composite = CompositePluginDiscovery()
        composite.addSource(builtinDiscovery)
        composite.addSource(testDiscovery)

        // Act
        val allPlugins = composite.discoverPlugins()

        // Assert
        assertEquals(2, allPlugins.size, "Should discover plugins from all sources")
        assertTrue(allPlugins.any { it.pluginId == "builtin.plugin" })
        assertTrue(allPlugins.any { it.pluginId == "test.plugin" })
    }

    @Test
    fun testPriorityDeduplication() = runBlocking {
        // Arrange: Two sources with same plugin ID but different priorities
        val highPriorityDiscovery = TestPluginDiscovery(priority = 10)
        highPriorityDiscovery.addDescriptor(
            PluginDescriptor(
                pluginId = "duplicate.plugin",
                name = "High Priority Version",
                version = "2.0.0",
                capabilities = setOf("capability"),
                source = PluginSource.Builtin
            )
        )

        val lowPriorityDiscovery = TestPluginDiscovery(priority = 100)
        lowPriorityDiscovery.addDescriptor(
            PluginDescriptor(
                pluginId = "duplicate.plugin",
                name = "Low Priority Version",
                version = "1.0.0",
                capabilities = setOf("capability"),
                source = PluginSource.FileSystem("/path")
            )
        )

        val composite = CompositePluginDiscovery()
        composite.addSource(highPriorityDiscovery)
        composite.addSource(lowPriorityDiscovery)

        // Act
        val plugins = composite.discoverPlugins()

        // Assert: Should only have one plugin with high priority version
        assertEquals(1, plugins.size, "Should deduplicate by plugin ID")
        assertEquals(
            "High Priority Version",
            plugins[0].name,
            "Should use plugin from higher priority source"
        )
        assertEquals("2.0.0", plugins[0].version)
    }

    @Test
    fun testCompositeDiscoveryWithDetails() = runBlocking {
        // Arrange
        val discovery1 = TestPluginDiscovery(priority = 0)
        discovery1.addDescriptor(
            PluginDescriptor(
                pluginId = "plugin.1",
                name = "Plugin 1",
                version = "1.0.0",
                capabilities = emptySet(),
                source = PluginSource.Builtin
            )
        )

        val errorDiscovery = ErrorThrowingDiscovery(priority = 100)

        val composite = CompositePluginDiscovery()
        composite.addSource(discovery1)
        composite.addSource(errorDiscovery)

        // Act
        val result = composite.discoverWithDetails()

        // Assert
        assertEquals(1, result.plugins.size, "Should have 1 plugin from working source")
        assertEquals(1, result.errors.size, "Should have 1 error from failing source")
        assertFalse(result.isSuccessful, "Should not be fully successful due to error")
    }

    @Test
    fun testCompositeSourceManagement() = runBlocking {
        // Arrange
        val composite = CompositePluginDiscovery()
        val source1 = TestPluginDiscovery(priority = 10)
        val source2 = TestPluginDiscovery(priority = 20)

        // Act: Add sources
        composite.addSource(source1)
        composite.addSource(source2)

        // Assert
        assertEquals(2, composite.sourceCount(), "Should have 2 sources")

        // Act: Remove source
        val removed = composite.removeSource(source1)

        // Assert
        assertTrue(removed, "Should return true for successful removal")
        assertEquals(1, composite.sourceCount(), "Should have 1 source after removal")

        // Act: Clear sources
        composite.clearSources()

        // Assert
        assertEquals(0, composite.sourceCount(), "Should have no sources after clear")
    }

    @Test
    fun testCompositeDiscoveryGrouped() = runBlocking {
        // Arrange
        val builtinDiscovery = BuiltinPluginDiscovery()
        builtinDiscovery.registerBuiltin { TestPlugin(pluginId = "builtin.1") }
        builtinDiscovery.registerBuiltin { TestPlugin(pluginId = "builtin.2") }

        val fileDiscovery = TestPluginDiscovery(priority = 100)
        fileDiscovery.addDescriptor(
            PluginDescriptor(
                pluginId = "file.1",
                name = "File Plugin",
                version = "1.0.0",
                capabilities = emptySet(),
                source = PluginSource.FileSystem("/path")
            )
        )

        val composite = CompositePluginDiscovery()
        composite.addSource(builtinDiscovery)
        composite.addSource(fileDiscovery)

        // Act
        val grouped = composite.discoverPluginsGrouped()

        // Assert
        assertTrue(grouped.containsKey(PluginSource.Builtin), "Should have builtin group")
        assertEquals(2, grouped[PluginSource.Builtin]?.size, "Should have 2 builtin plugins")
    }

    @Test
    fun testCompositeDiscoveryByCapability() = runBlocking {
        // Arrange
        val discovery = TestPluginDiscovery(priority = 0)
        discovery.addDescriptor(
            PluginDescriptor(
                pluginId = "llm.plugin",
                name = "LLM Plugin",
                version = "1.0.0",
                capabilities = setOf("llm.text-generation", "llm.embedding"),
                source = PluginSource.Builtin
            )
        )
        discovery.addDescriptor(
            PluginDescriptor(
                pluginId = "speech.plugin",
                name = "Speech Plugin",
                version = "1.0.0",
                capabilities = setOf("speech.recognition"),
                source = PluginSource.Builtin
            )
        )

        val composite = CompositePluginDiscovery()
        composite.addSource(discovery)

        // Act
        val llmPlugins = composite.findByCapability("llm.text-generation")
        val speechPlugins = composite.findByCapability("speech.recognition")

        // Assert
        assertEquals(1, llmPlugins.size, "Should find 1 LLM plugin")
        assertEquals("llm.plugin", llmPlugins[0].pluginId)
        assertEquals(1, speechPlugins.size, "Should find 1 speech plugin")
        assertEquals("speech.plugin", speechPlugins[0].pluginId)
    }

    @Test
    fun testCompositeGetPlugin() = runBlocking {
        // Arrange
        val discovery = TestPluginDiscovery(priority = 0)
        discovery.addDescriptor(
            PluginDescriptor(
                pluginId = "target.plugin",
                name = "Target Plugin",
                version = "1.0.0",
                capabilities = emptySet(),
                source = PluginSource.Builtin
            )
        )

        val composite = CompositePluginDiscovery()
        composite.addSource(discovery)

        // Act
        val found = composite.getPlugin("target.plugin")
        val notFound = composite.getPlugin("nonexistent.plugin")

        // Assert
        assertNotNull(found, "Should find existing plugin")
        assertEquals("Target Plugin", found.name)
        assertNull(notFound, "Should return null for non-existent plugin")
    }

    @Test
    fun testCompositeHasPlugin() = runBlocking {
        // Arrange
        val discovery = TestPluginDiscovery(priority = 0)
        discovery.addDescriptor(
            PluginDescriptor(
                pluginId = "existing.plugin",
                name = "Existing",
                version = "1.0.0",
                capabilities = emptySet(),
                source = PluginSource.Builtin
            )
        )

        val composite = CompositePluginDiscovery()
        composite.addSource(discovery)

        // Act & Assert
        assertTrue(composite.hasPlugin("existing.plugin"), "Should have existing plugin")
        assertFalse(composite.hasPlugin("missing.plugin"), "Should not have missing plugin")
    }

    // =========================================================================
    // PluginDescriptor Tests
    // =========================================================================

    @Test
    fun testPluginDescriptorCapabilityCheck() {
        // Arrange
        val descriptor = PluginDescriptor(
            pluginId = "test.plugin",
            name = "Test",
            version = "1.0.0",
            capabilities = setOf("cap.a", "cap.b", "cap.c.sub"),
            source = PluginSource.Builtin
        )

        // Assert: hasCapability
        assertTrue(descriptor.hasCapability("cap.a"), "Should have cap.a")
        assertTrue(descriptor.hasCapability("cap.b"), "Should have cap.b")
        assertFalse(descriptor.hasCapability("cap.d"), "Should not have cap.d")

        // Assert: matchesCapability (with prefix support)
        assertTrue(descriptor.matchesCapability("cap.a"), "Should match cap.a")
        assertTrue(descriptor.matchesCapability("cap.c"), "Should match cap.c prefix")
        assertFalse(descriptor.matchesCapability("cap.d"), "Should not match cap.d")
    }

    @Test
    fun testPluginDescriptorFromPlugin() {
        // Arrange
        val plugin = TestPlugin(pluginId = "from.plugin.test")

        // Act
        val descriptor = PluginDescriptor.fromPlugin(plugin)

        // Assert
        assertEquals("from.plugin.test", descriptor.pluginId)
        assertEquals("Test Plugin: from.plugin.test", descriptor.name)
        assertEquals("1.0.0", descriptor.version)
        assertEquals(PluginSource.Builtin, descriptor.source)
        assertTrue(descriptor.capabilities.isNotEmpty())
    }

    @Test
    fun testPluginDescriptorMetadata() {
        // Arrange
        val descriptor = PluginDescriptor(
            pluginId = "test.metadata",
            name = "Test",
            version = "1.0.0",
            capabilities = emptySet(),
            source = PluginSource.Builtin,
            metadata = mapOf(
                "author" to "Test Author",
                "license" to "MIT",
                "homepage" to "https://example.com"
            )
        )

        // Assert
        assertEquals("Test Author", descriptor.getMetadata("author"))
        assertEquals("MIT", descriptor.getMetadata("license"))
        assertNull(descriptor.getMetadata("nonexistent"))
    }

    @Test
    fun testPluginDescriptorToCapabilities() {
        // Arrange
        val descriptor = PluginDescriptor(
            pluginId = "test.caps",
            name = "Test",
            version = "1.0.0",
            capabilities = setOf("llm.text-generation", "nlu.intent"),
            source = PluginSource.Builtin
        )

        // Act
        val capabilities = descriptor.toCapabilities()

        // Assert
        assertEquals(2, capabilities.size)
        assertTrue(capabilities.any { it.id == "llm.text-generation" })
        assertTrue(capabilities.any { it.id == "nlu.intent" })
        assertTrue(capabilities.all { it.version == "1.0.0" })
    }

    // =========================================================================
    // PluginSource Tests
    // =========================================================================

    @Test
    fun testPluginSourceTypes() {
        // Test Builtin
        val builtin = PluginSource.Builtin
        assertEquals("Built-in", builtin.displayName())
        assertEquals("Builtin", builtin.toString())

        // Test FileSystem
        val fileSystem = PluginSource.FileSystem("/plugins/test")
        assertEquals("File System", fileSystem.displayName())
        assertEquals("/plugins/test", fileSystem.path)
        assertTrue(fileSystem.toString().contains("/plugins/test"))

        // Test Remote
        val remote = PluginSource.Remote(
            url = "https://example.com/plugin.zip",
            checksum = "sha256:abc123"
        )
        assertEquals("Remote", remote.displayName())
        assertEquals("https://example.com/plugin.zip", remote.url)
        assertEquals("sha256:abc123", remote.checksum)

        // Test AndroidPackage
        val android = PluginSource.AndroidPackage(
            packageName = "com.example.plugin",
            className = "PluginService",
            versionCode = 123L
        )
        assertEquals("Android Package", android.displayName())
        assertEquals("com.example.plugin", android.packageName)
        assertEquals("PluginService", android.className)
        assertEquals(123L, android.versionCode)
    }

    // =========================================================================
    // DiscoveryResult Tests
    // =========================================================================

    @Test
    fun testDiscoveryResultProperties() {
        // Test empty result
        val empty = DiscoveryResult.EMPTY
        assertTrue(empty.plugins.isEmpty())
        assertTrue(empty.errors.isEmpty())
        assertTrue(empty.isSuccessful)
        assertEquals(0, empty.pluginCount)

        // Test result with plugins
        val withPlugins = DiscoveryResult(
            plugins = listOf(
                PluginDescriptor(
                    pluginId = "test",
                    name = "Test",
                    version = "1.0.0",
                    capabilities = setOf("cap.test"),
                    source = PluginSource.Builtin
                )
            ),
            durationMs = 100
        )
        assertEquals(1, withPlugins.pluginCount)
        assertTrue(withPlugins.isSuccessful)
        assertEquals(100L, withPlugins.durationMs)

        // Test result with errors
        val withErrors = DiscoveryResult.fromError(
            DiscoveryError("Test error", source = "TestSource")
        )
        assertFalse(withErrors.isSuccessful)
        assertEquals(1, withErrors.errors.size)
    }

    @Test
    fun testDiscoveryResultFilterByCapability() {
        // Arrange
        val result = DiscoveryResult(
            plugins = listOf(
                PluginDescriptor(
                    pluginId = "llm.1",
                    name = "LLM 1",
                    version = "1.0.0",
                    capabilities = setOf("llm.text-generation"),
                    source = PluginSource.Builtin
                ),
                PluginDescriptor(
                    pluginId = "speech.1",
                    name = "Speech 1",
                    version = "1.0.0",
                    capabilities = setOf("speech.recognition"),
                    source = PluginSource.Builtin
                )
            )
        )

        // Act
        val llmPlugins = result.filterByCapability("llm")

        // Assert
        assertEquals(1, llmPlugins.size)
        assertEquals("llm.1", llmPlugins[0].pluginId)
    }

    // =========================================================================
    // DiscoveryError Tests
    // =========================================================================

    @Test
    fun testDiscoveryErrorFromException() {
        // Arrange
        val exception = RuntimeException("Test error message")

        // Act
        val error = DiscoveryError.fromException(
            exception,
            source = "TestDiscovery",
            pluginId = "failed.plugin"
        )

        // Assert
        assertEquals("Test error message", error.message)
        assertEquals("TestDiscovery", error.source)
        assertEquals("failed.plugin", error.pluginId)
    }

    // =========================================================================
    // DiscoveryStatistics Tests
    // =========================================================================

    @Test
    fun testDiscoveryStatistics() = runBlocking {
        // Arrange
        val discovery1 = TestPluginDiscovery(priority = 0)
        discovery1.addDescriptor(
            PluginDescriptor("p1", "P1", "1.0.0", emptySet(), PluginSource.Builtin)
        )
        discovery1.addDescriptor(
            PluginDescriptor("p2", "P2", "1.0.0", emptySet(), PluginSource.Builtin)
        )

        val composite = CompositePluginDiscovery()
        composite.addSource(discovery1)

        // Perform discovery first
        composite.discoverWithDetails()

        // Act
        val stats = composite.getStatistics()

        // Assert
        assertEquals(2, stats.totalPlugins)
        assertEquals(1, stats.totalSources)
        assertEquals(0, stats.errors)
    }

    // =========================================================================
    // Discovery Listener Tests
    // =========================================================================

    @Test
    fun testDiscoveryListener() = runBlocking {
        // Arrange
        val composite = CompositePluginDiscovery()
        val discovery = TestPluginDiscovery(priority = 0)
        discovery.addDescriptor(
            PluginDescriptor("test", "Test", "1.0.0", emptySet(), PluginSource.Builtin)
        )
        composite.addSource(discovery)

        var receivedResult: DiscoveryResult? = null
        composite.addDiscoveryListener(object : CompositePluginDiscovery.DiscoveryListener {
            override fun onDiscoveryComplete(result: DiscoveryResult) {
                receivedResult = result
            }
        })

        // Act
        composite.discoverWithDetails()

        // Assert
        val actualResult = receivedResult
        assertNotNull(actualResult, "Listener should receive result")
        assertEquals(1, actualResult.pluginCount)
    }

    // =========================================================================
    // Error Handling Tests
    // =========================================================================

    @Test
    fun testDiscoveryErrorIsolation() = runBlocking {
        // Arrange: One working source and one that throws
        val workingDiscovery = TestPluginDiscovery(priority = 0)
        workingDiscovery.addDescriptor(
            PluginDescriptor("working", "Working", "1.0.0", emptySet(), PluginSource.Builtin)
        )

        val errorDiscovery = ErrorThrowingDiscovery(priority = 100)

        val composite = CompositePluginDiscovery()
        composite.addSource(workingDiscovery)
        composite.addSource(errorDiscovery)

        // Act: Discovery should not throw
        val result = composite.discoverWithDetails()

        // Assert: Working plugins still discovered
        assertEquals(1, result.plugins.size, "Should still have plugin from working source")
        assertEquals(1, result.errors.size, "Should have error from failing source")
    }

    @Test
    fun testLoadPluginFromInvalidSource() = runBlocking {
        // Arrange
        val composite = CompositePluginDiscovery()
        val descriptor = PluginDescriptor(
            pluginId = "orphan.plugin",
            name = "Orphan",
            version = "1.0.0",
            capabilities = emptySet(),
            source = PluginSource.Remote("https://invalid.url")
        )

        // Act
        val result = composite.loadPlugin(descriptor)

        // Assert
        assertTrue(result.isFailure, "Should fail to load from unavailable source")
    }

    // =========================================================================
    // Duplicate Source Prevention Tests
    // =========================================================================

    @Test
    fun testDuplicateSourcePrevention() = runBlocking {
        // Arrange
        val composite = CompositePluginDiscovery()
        val source = TestPluginDiscovery(priority = 0)

        // Act: Add source twice
        composite.addSource(source)
        var exceptionThrown = false
        try {
            composite.addSource(source)
        } catch (_: IllegalArgumentException) {
            exceptionThrown = true
        }

        // Assert
        assertTrue(exceptionThrown, "Should throw exception for duplicate source")
        assertEquals(1, composite.sourceCount(), "Should only have one source")
    }

    // =========================================================================
    // Refresh Tests
    // =========================================================================

    @Test
    fun testCompositeRefresh() = runBlocking {
        // Arrange
        val discovery = TestPluginDiscovery(priority = 0)
        discovery.addDescriptor(
            PluginDescriptor("initial", "Initial", "1.0.0", emptySet(), PluginSource.Builtin)
        )

        val composite = CompositePluginDiscovery()
        composite.addSource(discovery)

        // Initial discovery
        assertEquals(1, composite.discoverPlugins().size)

        // Add another plugin
        discovery.addDescriptor(
            PluginDescriptor("added", "Added", "1.0.0", emptySet(), PluginSource.Builtin)
        )

        // Act: Refresh
        composite.refresh()
        val plugins = composite.discoverPlugins()

        // Assert
        assertEquals(2, plugins.size, "Should discover new plugin after refresh")
    }
}

// =============================================================================
// Test Discovery Implementations
// =============================================================================

/**
 * Test plugin discovery implementation for testing.
 */
class TestPluginDiscovery(
    override val priority: Int = 100
) : PluginDiscovery {

    private val descriptors = mutableListOf<PluginDescriptor>()
    private val plugins = mutableMapOf<String, UniversalPlugin>()

    fun addDescriptor(descriptor: PluginDescriptor) {
        descriptors.add(descriptor)
    }

    @Suppress("unused")
    fun addPlugin(descriptor: PluginDescriptor, plugin: UniversalPlugin) {
        descriptors.add(descriptor)
        plugins[descriptor.pluginId] = plugin
    }

    fun clear() {
        descriptors.clear()
        plugins.clear()
    }

    override suspend fun discoverPlugins(): List<PluginDescriptor> {
        return descriptors.toList()
    }

    override suspend fun loadPlugin(descriptor: PluginDescriptor): Result<UniversalPlugin> {
        val plugin = plugins[descriptor.pluginId]
        return if (plugin != null) {
            Result.success(plugin)
        } else {
            Result.failure(PluginLoadException("Plugin not found: ${descriptor.pluginId}", descriptor.pluginId))
        }
    }
}

/**
 * Discovery implementation that throws errors for testing error handling.
 */
class ErrorThrowingDiscovery(
    override val priority: Int = 100
) : PluginDiscovery {

    override suspend fun discoverPlugins(): List<PluginDescriptor> {
        throw RuntimeException("Simulated discovery error")
    }

    override suspend fun loadPlugin(descriptor: PluginDescriptor): Result<UniversalPlugin> {
        return Result.failure(RuntimeException("Simulated load error"))
    }
}
