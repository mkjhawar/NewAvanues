/**
 * AccessibilityDataProviderImpl.kt - Default implementation of AccessibilityDataProvider
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2026-01-22
 *
 * Provides the default implementation of AccessibilityDataProvider that wraps
 * platform-specific repositories and provides thread-safe data access.
 */
package com.augmentalis.magiccode.plugins.universal.data

import com.augmentalis.magiccode.plugins.universal.contracts.voiceoscore.ScreenContext
import com.augmentalis.voiceoscore.QuantizedCommand
import com.augmentalis.voiceoscore.QuantizedElement
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Default implementation of [AccessibilityDataProvider].
 *
 * This implementation wraps repository references (typed as Any for now since
 * actual repositories are platform-specific) and provides:
 * - Thread-safe data access via Mutex
 * - MutableStateFlow for reactive updates
 * - Placeholder implementations for repository calls
 *
 * ## Platform Integration
 * The repository parameters are typed as `Any` because the actual repository
 * implementations vary by platform (Android, iOS, Desktop). Platform-specific
 * code should cast these to the correct types.
 *
 * ## Usage
 * ```kotlin
 * // Create with platform-specific repositories
 * val provider = AccessibilityDataProviderImpl(
 *     elementRepository = androidElementRepo,
 *     commandRepository = androidCommandRepo,
 *     preferenceRepository = androidPrefRepo
 * )
 *
 * // Update screen state (called by accessibility service)
 * provider.updateScreenElements(newElements)
 * provider.updateScreenContext(newContext)
 * ```
 *
 * @property elementRepository Repository for UI element data (platform-specific)
 * @property commandRepository Repository for command data (platform-specific)
 * @property preferenceRepository Repository for user preferences (platform-specific)
 * @since 1.0.0
 * @see AccessibilityDataProvider
 * @see CachedAccessibilityData
 */
class AccessibilityDataProviderImpl(
    private val elementRepository: Any,
    private val commandRepository: Any,
    private val preferenceRepository: Any
) : AccessibilityDataProvider {

    // =========================================================================
    // State Management
    // =========================================================================

    /**
     * Mutex for thread-safe operations on mutable state.
     */
    private val mutex = Mutex()

    /**
     * Internal mutable state for current screen elements.
     */
    private val _screenElementsFlow = MutableStateFlow<List<QuantizedElement>>(emptyList())

    /**
     * Internal mutable state for current screen context.
     */
    private val _screenContextFlow = MutableStateFlow<ScreenContext?>(null)

    /**
     * Cache for command history to reduce repository calls.
     */
    private var cachedCommandHistory: List<CommandHistoryEntry>? = null
    private var commandHistoryCacheTime: Long = 0L
    private val commandHistoryCacheTtlMs: Long = 30_000L // 30 seconds

    /**
     * Cache for navigation graphs by package name.
     */
    private val navigationGraphCache = mutableMapOf<String, NavigationGraph>()
    private val navigationGraphCacheTime = mutableMapOf<String, Long>()
    private val navigationGraphCacheTtlMs: Long = 60_000L // 1 minute

    // =========================================================================
    // Public Flows
    // =========================================================================

    override val screenElementsFlow: StateFlow<List<QuantizedElement>>
        get() = _screenElementsFlow.asStateFlow()

    override val screenContextFlow: StateFlow<ScreenContext?>
        get() = _screenContextFlow.asStateFlow()

    // =========================================================================
    // UI Element Data
    // =========================================================================

    override suspend fun getCurrentScreenElements(): List<QuantizedElement> {
        return mutex.withLock {
            _screenElementsFlow.value
        }
    }

    override suspend fun getElement(avid: String): QuantizedElement? {
        return mutex.withLock {
            _screenElementsFlow.value.find { it.avid == avid }
        }
    }

    // =========================================================================
    // Command Data
    // =========================================================================

    override suspend fun getScreenCommands(): List<QuantizedCommand> {
        val context = getScreenContext()
        return mutex.withLock {
            // In actual implementation, this would query commandRepository
            // filtered by current package and screen
            fetchCommandsForContext(context.packageName, context.screenId())
        }
    }

    override suspend fun getCommandHistory(limit: Int, successOnly: Boolean): List<CommandHistoryEntry> {
        return mutex.withLock {
            val now = currentTimeMillis()

            // Check cache validity
            val cached = cachedCommandHistory
            if (cached != null && (now - commandHistoryCacheTime) < commandHistoryCacheTtlMs) {
                return@withLock filterCommandHistory(cached, limit, successOnly)
            }

            // Fetch from repository (placeholder - actual impl would use commandRepository)
            val history = fetchCommandHistory()
            cachedCommandHistory = history
            commandHistoryCacheTime = now

            filterCommandHistory(history, limit, successOnly)
        }
    }

    override suspend fun getTopCommands(limit: Int, context: String?): List<RankedCommand> {
        return mutex.withLock {
            // In actual implementation, this would aggregate from commandRepository
            // with usage statistics
            fetchTopCommands(limit, context)
        }
    }

    // =========================================================================
    // Screen Context
    // =========================================================================

    override suspend fun getScreenContext(): ScreenContext {
        return mutex.withLock {
            _screenContextFlow.value
        } ?: ScreenContext.EMPTY
    }

    override suspend fun getNavigationGraph(packageName: String): NavigationGraph {
        return mutex.withLock {
            val now = currentTimeMillis()

            // Check cache
            val cached = navigationGraphCache[packageName]
            val cacheTime = navigationGraphCacheTime[packageName] ?: 0L

            if (cached != null && (now - cacheTime) < navigationGraphCacheTtlMs) {
                return@withLock cached
            }

            // Fetch from repository (placeholder)
            val graph = fetchNavigationGraph(packageName)
            navigationGraphCache[packageName] = graph
            navigationGraphCacheTime[packageName] = now

            graph
        }
    }

    // =========================================================================
    // User Preferences
    // =========================================================================

    override suspend fun getContextPreferences(): List<ContextPreference> {
        return mutex.withLock {
            // In actual implementation, this would query preferenceRepository
            fetchContextPreferences()
        }
    }

    // =========================================================================
    // State Update Methods (for platform integration)
    // =========================================================================

    /**
     * Update the current screen elements.
     *
     * Called by the accessibility service when screen content changes.
     *
     * @param elements New list of screen elements
     */
    suspend fun updateScreenElements(elements: List<QuantizedElement>) {
        mutex.withLock {
            _screenElementsFlow.value = elements
        }
    }

    /**
     * Update the current screen context.
     *
     * Called by the accessibility service when the user navigates.
     *
     * @param context New screen context
     */
    suspend fun updateScreenContext(context: ScreenContext) {
        mutex.withLock {
            _screenContextFlow.value = context
            // Invalidate command history cache on screen change
            cachedCommandHistory = null
        }
    }

    /**
     * Clear all cached data.
     *
     * Called when caches should be invalidated (e.g., after learning).
     */
    suspend fun clearCache() {
        mutex.withLock {
            cachedCommandHistory = null
            navigationGraphCache.clear()
            navigationGraphCacheTime.clear()
        }
    }

    /**
     * Invalidate navigation graph cache for a specific package.
     *
     * @param packageName Package to invalidate
     */
    suspend fun invalidateNavigationCache(packageName: String) {
        mutex.withLock {
            navigationGraphCache.remove(packageName)
            navigationGraphCacheTime.remove(packageName)
        }
    }

    // =========================================================================
    // Repository Fetch Methods (Placeholders)
    // =========================================================================

    /**
     * Fetch commands for a specific context.
     *
     * Placeholder - actual implementation would use commandRepository.
     */
    private fun fetchCommandsForContext(packageName: String, screenId: String?): List<QuantizedCommand> {
        // TODO: Implement with actual repository call
        // Example: (commandRepository as CommandRepository).getCommandsForScreen(packageName, screenId)
        return emptyList()
    }

    /**
     * Fetch command history from repository.
     *
     * Placeholder - actual implementation would use commandRepository.
     */
    private fun fetchCommandHistory(): List<CommandHistoryEntry> {
        // TODO: Implement with actual repository call
        // Example: (commandRepository as CommandRepository).getHistory()
        return emptyList()
    }

    /**
     * Filter command history by criteria.
     */
    private fun filterCommandHistory(
        history: List<CommandHistoryEntry>,
        limit: Int,
        successOnly: Boolean
    ): List<CommandHistoryEntry> {
        var filtered = history
        if (successOnly) {
            filtered = filtered.filter { it.success }
        }
        return filtered.take(limit)
    }

    /**
     * Fetch top commands with ranking.
     *
     * Placeholder - actual implementation would aggregate usage data.
     */
    private fun fetchTopCommands(limit: Int, context: String?): List<RankedCommand> {
        // TODO: Implement with actual repository call
        // Example: (commandRepository as CommandRepository).getTopCommands(limit, context)
        return emptyList()
    }

    /**
     * Fetch navigation graph for a package.
     *
     * Placeholder - actual implementation would use navigation repository.
     */
    private fun fetchNavigationGraph(packageName: String): NavigationGraph {
        // TODO: Implement with actual repository call
        // Example: (navigationRepository as NavigationRepository).getGraph(packageName)
        return NavigationGraph.empty(packageName)
    }

    /**
     * Fetch context preferences.
     *
     * Placeholder - actual implementation would use preferenceRepository.
     */
    private fun fetchContextPreferences(): List<ContextPreference> {
        // TODO: Implement with actual repository call
        // Example: (preferenceRepository as PreferenceRepository).getAll()
        return emptyList()
    }

    // =========================================================================
    // Utility
    // =========================================================================

    /**
     * Platform-agnostic current time.
     */
    private fun currentTimeMillis(): Long {
        return com.augmentalis.magiccode.plugins.universal.currentTimeMillis()
    }

    companion object {
        /**
         * Create a provider with no-op repositories for testing.
         */
        fun createForTesting(): AccessibilityDataProviderImpl {
            return AccessibilityDataProviderImpl(
                elementRepository = Unit,
                commandRepository = Unit,
                preferenceRepository = Unit
            )
        }
    }
}

// =============================================================================
// Extension Functions
// =============================================================================

/**
 * Create an AccessibilityDataProviderImpl with caching wrapper.
 *
 * @param elementCacheSize Maximum elements to cache
 * @param commandCacheTtlMs TTL for command cache in milliseconds
 * @return CachedAccessibilityData wrapping this provider
 */
fun AccessibilityDataProviderImpl.withCaching(
    elementCacheSize: Int = 100,
    commandCacheTtlMs: Long = 60_000
): CachedAccessibilityData {
    return CachedAccessibilityData(
        delegate = this,
        elementCacheSize = elementCacheSize,
        commandCacheTtlMs = commandCacheTtlMs
    )
}
