/**
 * CompositePluginDiscovery.kt - Combines multiple plugin discovery sources
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2026-01-22
 *
 * Aggregates multiple plugin discovery sources into a unified interface.
 * Handles priority-based deduplication and provides grouped discovery results.
 */
package com.augmentalis.magiccode.plugins.discovery

import com.augmentalis.magiccode.plugins.universal.UniversalPlugin
import com.augmentalis.magiccode.plugins.universal.currentTimeMillis
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Combines multiple plugin discovery sources into a unified interface.
 *
 * CompositePluginDiscovery aggregates results from all registered discovery sources
 * and handles:
 * - **Priority-based ordering**: Sources with lower priority values are queried first
 * - **Deduplication**: When the same plugin ID is found in multiple sources,
 *   the one from the highest-priority (lowest value) source wins
 * - **Parallel discovery**: Sources are queried concurrently for performance
 * - **Error isolation**: Errors in one source don't prevent discovery from others
 *
 * ## Usage Example
 * ```kotlin
 * val composite = CompositePluginDiscovery()
 *
 * // Add discovery sources
 * composite.addSource(builtinDiscovery)
 * composite.addSource(fileSystemDiscovery)
 * composite.addSource(remoteDiscovery)
 *
 * // Discover all plugins
 * val allPlugins = composite.discoverPlugins()
 *
 * // Discover grouped by source
 * val grouped = composite.discoverPluginsGrouped()
 * grouped.forEach { (source, plugins) ->
 *     println("${source.displayName()}: ${plugins.size} plugins")
 * }
 *
 * // Find plugins by capability
 * val llmPlugins = composite.findByCapability("llm.text-generation")
 * ```
 *
 * ## Thread Safety
 * All operations are thread-safe. Sources can be added/removed while discovery
 * is in progress.
 *
 * @since 1.0.0
 * @see PluginDiscovery
 */
class CompositePluginDiscovery : PluginDiscovery {
    /**
     * Priority for composite discovery (acts as aggregator).
     *
     * The composite itself has the lowest priority since it delegates
     * to its sources.
     */
    override val priority: Int = PRIORITY_COMPOSITE

    /**
     * Registered discovery sources.
     */
    private val sources = mutableListOf<PluginDiscovery>()

    /**
     * Mutex for thread-safe source management.
     */
    private val mutex = Mutex()

    /**
     * Cache of last discovery results.
     */
    private var lastDiscoveryResult: DiscoveryResult? = null

    /**
     * Discovery listeners.
     */
    private val discoveryListeners = mutableListOf<DiscoveryListener>()

    /**
     * Add a discovery source.
     *
     * Sources are automatically sorted by priority (lowest first).
     *
     * @param source Discovery source to add
     * @throws IllegalArgumentException if source is already added
     */
    suspend fun addSource(source: PluginDiscovery) {
        mutex.withLock {
            if (source in sources) {
                throw IllegalArgumentException("Source already added: ${source::class.simpleName}")
            }
            sources.add(source)
            sources.sortBy { it.priority }
        }
    }

    /**
     * Add a discovery source without checking for duplicates.
     *
     * @param source Discovery source to add
     */
    suspend fun addSourceUnchecked(source: PluginDiscovery) {
        mutex.withLock {
            sources.add(source)
            sources.sortBy { it.priority }
        }
    }

    /**
     * Remove a discovery source.
     *
     * @param source Discovery source to remove
     * @return true if the source was found and removed
     */
    suspend fun removeSource(source: PluginDiscovery): Boolean {
        return mutex.withLock {
            sources.remove(source)
        }
    }

    /**
     * Remove all discovery sources.
     */
    suspend fun clearSources() {
        mutex.withLock {
            sources.clear()
        }
    }

    /**
     * Get the number of registered sources.
     *
     * @return Number of discovery sources
     */
    suspend fun sourceCount(): Int {
        return mutex.withLock {
            sources.size
        }
    }

    /**
     * Get a read-only list of registered sources.
     *
     * @return List of discovery sources, sorted by priority
     */
    suspend fun getSources(): List<PluginDiscovery> {
        return mutex.withLock {
            sources.toList()
        }
    }

    /**
     * Discover plugins from all registered sources.
     *
     * Queries all sources concurrently and merges results, respecting priority
     * order for duplicate plugin IDs.
     *
     * @return List of unique plugin descriptors
     */
    override suspend fun discoverPlugins(): List<PluginDescriptor> {
        val result = discoverWithDetails()
        return result.plugins
    }

    /**
     * Discover plugins with full details including errors.
     *
     * @return DiscoveryResult with plugins and any errors
     */
    suspend fun discoverWithDetails(): DiscoveryResult {
        val startTime = currentTimeMillis()
        val sourcesCopy = mutex.withLock { sources.toList() }

        if (sourcesCopy.isEmpty()) {
            return DiscoveryResult.EMPTY
        }

        // Discover from all sources concurrently
        val sourceResults = coroutineScope {
            sourcesCopy.map { source ->
                async {
                    try {
                        val plugins = source.discoverPlugins()
                        SourceDiscoveryResult(
                            source = source,
                            plugins = plugins,
                            error = null
                        )
                    } catch (e: Exception) {
                        SourceDiscoveryResult(
                            source = source,
                            plugins = emptyList(),
                            error = DiscoveryError.fromException(e, source::class.simpleName ?: "Unknown")
                        )
                    }
                }
            }.awaitAll()
        }

        // Merge results with priority-based deduplication
        val pluginMap = mutableMapOf<String, PluginDescriptor>()
        val priorityMap = mutableMapOf<String, Int>()
        val errors = mutableListOf<DiscoveryError>()

        for (result in sourceResults) {
            if (result.error != null) {
                errors.add(result.error)
            }

            for (plugin in result.plugins) {
                val existingPriority = priorityMap[plugin.pluginId]
                if (existingPriority == null || result.source.priority < existingPriority) {
                    pluginMap[plugin.pluginId] = plugin
                    priorityMap[plugin.pluginId] = result.source.priority
                }
            }
        }

        val discoveryResult = DiscoveryResult(
            plugins = pluginMap.values.toList(),
            errors = errors,
            sourceType = "Composite",
            durationMs = currentTimeMillis() - startTime
        )

        // Cache result
        lastDiscoveryResult = discoveryResult

        // Notify listeners
        discoveryListeners.forEach { listener ->
            try {
                listener.onDiscoveryComplete(discoveryResult)
            } catch (e: Exception) {
                // Listener errors should not propagate
            }
        }

        return discoveryResult
    }

    /**
     * Load a plugin from its descriptor.
     *
     * Finds the appropriate source based on the descriptor's source type
     * and delegates loading to it.
     *
     * @param descriptor Plugin descriptor
     * @return Result containing the loaded plugin or error
     */
    override suspend fun loadPlugin(descriptor: PluginDescriptor): Result<UniversalPlugin> {
        val sourcesCopy = mutex.withLock { sources.toList() }

        // Find a source that can load this plugin
        for (source in sourcesCopy) {
            if (source.canLoad(descriptor)) {
                return source.loadPlugin(descriptor)
            }
        }

        return Result.failure(PluginLoadException(
            "No source found that can load plugin '${descriptor.pluginId}' from ${descriptor.source}",
            descriptor.pluginId
        ))
    }

    /**
     * Refresh all discovery sources.
     */
    override suspend fun refresh() {
        val sourcesCopy = mutex.withLock { sources.toList() }

        coroutineScope {
            sourcesCopy.map { source ->
                async {
                    try {
                        source.refresh()
                    } catch (e: Exception) {
                        // Log but don't fail the whole refresh
                    }
                }
            }.awaitAll()
        }
    }

    /**
     * Discover plugins grouped by source type.
     *
     * @return Map of source type to list of plugins from that source
     */
    suspend fun discoverPluginsGrouped(): Map<PluginSource, List<PluginDescriptor>> {
        val allPlugins = discoverPlugins()
        return allPlugins.groupBy { it.source }
    }

    /**
     * Discover plugins grouped by capability.
     *
     * @return Map of capability ID to list of plugins with that capability
     */
    suspend fun discoverPluginsByCapability(): Map<String, List<PluginDescriptor>> {
        val allPlugins = discoverPlugins()
        val result = mutableMapOf<String, MutableList<PluginDescriptor>>()

        for (plugin in allPlugins) {
            for (capability in plugin.capabilities) {
                result.getOrPut(capability) { mutableListOf() }.add(plugin)
            }
        }

        return result
    }

    /**
     * Find plugins by capability.
     *
     * @param capabilityId Capability ID to search for
     * @return List of plugins with the specified capability
     */
    suspend fun findByCapability(capabilityId: String): List<PluginDescriptor> {
        val allPlugins = discoverPlugins()
        return allPlugins.filter { it.hasCapability(capabilityId) }
    }

    /**
     * Find plugins matching a capability filter.
     *
     * @param filter Capability ID or prefix to match
     * @return List of plugins matching the filter
     */
    suspend fun findMatchingCapability(filter: String): List<PluginDescriptor> {
        val allPlugins = discoverPlugins()
        return allPlugins.filter { it.matchesCapability(filter) }
    }

    /**
     * Get a specific plugin by ID.
     *
     * @param pluginId Plugin ID to find
     * @return PluginDescriptor or null if not found
     */
    suspend fun getPlugin(pluginId: String): PluginDescriptor? {
        val allPlugins = discoverPlugins()
        return allPlugins.find { it.pluginId == pluginId }
    }

    /**
     * Check if a plugin exists.
     *
     * @param pluginId Plugin ID to check
     * @return true if the plugin exists in any source
     */
    suspend fun hasPlugin(pluginId: String): Boolean {
        return getPlugin(pluginId) != null
    }

    /**
     * Get the last discovery result from cache.
     *
     * @return Last DiscoveryResult or null if never discovered
     */
    fun getLastResult(): DiscoveryResult? = lastDiscoveryResult

    /**
     * Add a discovery listener.
     *
     * @param listener Listener to add
     */
    fun addDiscoveryListener(listener: DiscoveryListener) {
        discoveryListeners.add(listener)
    }

    /**
     * Remove a discovery listener.
     *
     * @param listener Listener to remove
     */
    fun removeDiscoveryListener(listener: DiscoveryListener) {
        discoveryListeners.remove(listener)
    }

    /**
     * Get discovery statistics.
     *
     * @return DiscoveryStatistics from the last discovery
     */
    suspend fun getStatistics(): DiscoveryStatistics {
        val result = lastDiscoveryResult ?: discoverWithDetails()
        val grouped = result.plugins.groupBy { it.source }

        return DiscoveryStatistics(
            totalPlugins = result.plugins.size,
            totalSources = sources.size,
            pluginsBySource = grouped.mapValues { it.value.size },
            errors = result.errors.size,
            durationMs = result.durationMs
        )
    }

    /**
     * Internal result from a single source.
     */
    private data class SourceDiscoveryResult(
        val source: PluginDiscovery,
        val plugins: List<PluginDescriptor>,
        val error: DiscoveryError?
    )

    /**
     * Listener for discovery events.
     */
    interface DiscoveryListener {
        /**
         * Called when discovery completes.
         *
         * @param result Discovery result
         */
        fun onDiscoveryComplete(result: DiscoveryResult)
    }

    companion object {
        /** Priority value for composite discovery. */
        const val PRIORITY_COMPOSITE = -1

        /**
         * Create a composite discovery with standard sources.
         *
         * @param builtinDiscovery Built-in plugin discovery
         * @param fileSystemDiscovery File system plugin discovery (optional)
         * @return Configured CompositePluginDiscovery
         */
        suspend fun withStandardSources(
            builtinDiscovery: BuiltinPluginDiscovery,
            fileSystemDiscovery: FileSystemPluginDiscovery? = null
        ): CompositePluginDiscovery {
            return CompositePluginDiscovery().apply {
                addSource(builtinDiscovery)
                fileSystemDiscovery?.let { addSource(it) }
            }
        }

        /**
         * Create a composite discovery with provided sources.
         *
         * @param sources Discovery sources to add
         * @return Configured CompositePluginDiscovery
         */
        suspend fun withSources(vararg sources: PluginDiscovery): CompositePluginDiscovery {
            return CompositePluginDiscovery().apply {
                sources.forEach { addSource(it) }
            }
        }
    }
}

/**
 * Statistics about plugin discovery.
 *
 * @property totalPlugins Total number of unique plugins discovered
 * @property totalSources Number of discovery sources queried
 * @property pluginsBySource Count of plugins per source type
 * @property errors Number of errors encountered
 * @property durationMs Total discovery duration in milliseconds
 * @since 1.0.0
 */
data class DiscoveryStatistics(
    val totalPlugins: Int,
    val totalSources: Int,
    val pluginsBySource: Map<PluginSource, Int>,
    val errors: Int,
    val durationMs: Long
) {
    /**
     * Get a formatted summary string.
     */
    override fun toString(): String {
        return buildString {
            appendLine("Discovery Statistics:")
            appendLine("  Total plugins: $totalPlugins")
            appendLine("  Total sources: $totalSources")
            appendLine("  Errors: $errors")
            appendLine("  Duration: ${durationMs}ms")
            appendLine("  By source:")
            pluginsBySource.forEach { (source, count) ->
                appendLine("    ${source.displayName()}: $count")
            }
        }
    }
}

/**
 * Builder for creating CompositePluginDiscovery with fluent API.
 *
 * ## Usage Example
 * ```kotlin
 * val discovery = CompositePluginDiscoveryBuilder()
 *     .addBuiltin(builtinDiscovery)
 *     .addFileSystem(fileSystemDiscovery)
 *     .withListener { result ->
 *         println("Discovered ${result.pluginCount} plugins")
 *     }
 *     .build()
 * ```
 *
 * @since 1.0.0
 */
class CompositePluginDiscoveryBuilder {
    private val sources = mutableListOf<PluginDiscovery>()
    private val listeners = mutableListOf<CompositePluginDiscovery.DiscoveryListener>()

    /**
     * Add a built-in discovery source.
     */
    fun addBuiltin(source: BuiltinPluginDiscovery): CompositePluginDiscoveryBuilder {
        sources.add(source)
        return this
    }

    /**
     * Add a file system discovery source.
     */
    fun addFileSystem(source: FileSystemPluginDiscovery): CompositePluginDiscoveryBuilder {
        sources.add(source)
        return this
    }

    /**
     * Add any discovery source.
     */
    fun addSource(source: PluginDiscovery): CompositePluginDiscoveryBuilder {
        sources.add(source)
        return this
    }

    /**
     * Add a discovery listener.
     */
    fun withListener(listener: CompositePluginDiscovery.DiscoveryListener): CompositePluginDiscoveryBuilder {
        listeners.add(listener)
        return this
    }

    /**
     * Add a discovery listener using lambda.
     */
    fun withListener(onComplete: (DiscoveryResult) -> Unit): CompositePluginDiscoveryBuilder {
        listeners.add(object : CompositePluginDiscovery.DiscoveryListener {
            override fun onDiscoveryComplete(result: DiscoveryResult) {
                onComplete(result)
            }
        })
        return this
    }

    /**
     * Build the CompositePluginDiscovery.
     *
     * @return Configured CompositePluginDiscovery
     */
    suspend fun build(): CompositePluginDiscovery {
        val composite = CompositePluginDiscovery()

        // Add sources (sorted by priority automatically)
        sources.forEach { composite.addSourceUnchecked(it) }

        // Add listeners
        listeners.forEach { composite.addDiscoveryListener(it) }

        return composite
    }
}

// Uses currentTimeMillis() from com.augmentalis.magiccode.plugins.universal
