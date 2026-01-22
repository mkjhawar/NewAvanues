/**
 * AccessibilityDataProviderFactory.kt - Factory for creating AccessibilityDataProvider instances
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2026-01-22
 *
 * Provides factory methods for creating and configuring AccessibilityDataProvider
 * instances with proper dependency injection and caching.
 */
package com.augmentalis.magiccode.plugins.android.data

import android.content.Context
import com.augmentalis.database.repositories.ICommandHistoryRepository
import com.augmentalis.database.repositories.IContextPreferenceRepository
import com.augmentalis.database.repositories.IScrapedAppRepository
import com.augmentalis.database.repositories.IScreenContextRepository
import com.augmentalis.database.repositories.IScreenTransitionRepository
import com.augmentalis.database.repositories.IVoiceCommandRepository
import com.augmentalis.magiccode.plugins.universal.contracts.voiceoscore.ScreenContext
import com.augmentalis.magiccode.plugins.universal.data.AccessibilityDataProvider
import com.augmentalis.magiccode.plugins.universal.data.CachedAccessibilityData
import com.augmentalis.magiccode.plugins.universal.data.CommandHistoryEntry
import com.augmentalis.magiccode.plugins.universal.data.ContextPreference
import com.augmentalis.magiccode.plugins.universal.data.NavigationGraph
import com.augmentalis.magiccode.plugins.universal.data.RankedCommand
import com.augmentalis.voiceoscore.CommandActionType
import com.augmentalis.voiceoscore.QuantizedCommand
import com.augmentalis.voiceoscore.QuantizedElement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.lang.ref.WeakReference

/**
 * Factory for creating [AccessibilityDataProvider] instances.
 *
 * This factory handles dependency injection, configuration, and instance management
 * for AccessibilityDataProvider implementations. It provides methods for creating
 * providers with different configurations and caching strategies.
 *
 * ## Factory Pattern Benefits
 * - Centralized provider creation and configuration
 * - Proper dependency injection handling
 * - Support for different caching strategies
 * - Mock provider support for testing
 * - Thread-safe instance management
 *
 * ## Usage Example
 * ```kotlin
 * // Create a provider with repositories
 * val provider = AccessibilityDataProviderFactory.create(
 *     context = applicationContext,
 *     elementRepository = scrapedAppRepo,
 *     commandRepository = voiceCommandRepo,
 *     preferenceRepository = contextPrefRepo,
 *     scope = lifecycleScope
 * )
 *
 * // Create a cached provider
 * val cachedProvider = AccessibilityDataProviderFactory.createCached(
 *     provider = provider,
 *     cacheConfig = CacheConfiguration(
 *         elementCacheTtlMs = 2000L,
 *         commandCacheTtlMs = 10000L
 *     )
 * )
 *
 * // Create a mock for testing
 * val mockProvider = AccessibilityDataProviderFactory.createMock()
 * mockProvider.setMockElements(testElements)
 * ```
 *
 * @since 1.0.0
 * @see AndroidAccessibilityDataProvider
 * @see CachedAccessibilityData
 * @see MockAccessibilityDataProvider
 */
object AccessibilityDataProviderFactory {

    /**
     * Weak reference to the last created provider for reuse.
     */
    private var cachedProvider: WeakReference<AndroidAccessibilityDataProvider>? = null

    /**
     * Lock for thread-safe provider creation.
     */
    private val createLock = Any()

    /**
     * Create an AndroidAccessibilityDataProvider with repository injection.
     *
     * Creates a fully configured provider that connects to VoiceOSCore repositories
     * through the adapted data sources.
     *
     * ## Thread Safety
     * This method is thread-safe and can be called from any thread.
     *
     * ## Lifecycle
     * The created provider should be disposed when no longer needed to prevent
     * memory leaks. Call [AndroidAccessibilityDataProvider.dispose] when done.
     *
     * @param context Android context (application context recommended)
     * @param elementRepository Repository for scraped app/element data
     * @param commandRepository Repository for voice commands
     * @param preferenceRepository Repository for context preferences
     * @param commandHistoryRepository Repository for command history
     * @param screenContextRepository Repository for screen context
     * @param screenTransitionRepository Repository for screen transitions
     * @param scope Coroutine scope for async operations (lifecycle-bound recommended)
     * @return Configured [AndroidAccessibilityDataProvider]
     */
    @JvmStatic
    fun create(
        context: Context,
        elementRepository: IScrapedAppRepository,
        commandRepository: IVoiceCommandRepository,
        preferenceRepository: IContextPreferenceRepository,
        commandHistoryRepository: ICommandHistoryRepository,
        screenContextRepository: IScreenContextRepository,
        screenTransitionRepository: IScreenTransitionRepository,
        scope: CoroutineScope
    ): AndroidAccessibilityDataProvider {
        return synchronized(createLock) {
            val adapter = RepositoryAdapter()

            val provider = AndroidAccessibilityDataProvider(
                elementDataSource = adapter.adaptElementRepository(elementRepository),
                commandDataSource = adapter.adaptCommandRepository(commandRepository),
                preferenceDataSource = adapter.adaptPreferenceRepository(preferenceRepository),
                commandHistoryRepository = commandHistoryRepository,
                screenContextRepository = screenContextRepository,
                screenTransitionRepository = screenTransitionRepository,
                coroutineScope = scope
            )

            // Cache for potential reuse
            cachedProvider = WeakReference(provider)

            provider
        }
    }

    /**
     * Create a provider with minimal dependencies.
     *
     * Creates a provider with only the essential repositories, using no-op
     * implementations for optional ones. Useful for simplified setups.
     *
     * @param context Android context
     * @param elementRepository Repository for element data
     * @param commandRepository Repository for commands
     * @param scope Coroutine scope
     * @return Configured [AndroidAccessibilityDataProvider]
     */
    @JvmStatic
    fun createMinimal(
        context: Context,
        elementRepository: IScrapedAppRepository,
        commandRepository: IVoiceCommandRepository,
        scope: CoroutineScope
    ): AndroidAccessibilityDataProvider {
        val adapter = RepositoryAdapter()

        return AndroidAccessibilityDataProvider(
            elementDataSource = adapter.adaptElementRepository(elementRepository),
            commandDataSource = adapter.adaptCommandRepository(commandRepository),
            preferenceDataSource = NoOpPreferenceDataSource(),
            commandHistoryRepository = NoOpCommandHistoryRepository(),
            screenContextRepository = NoOpScreenContextRepository(),
            screenTransitionRepository = NoOpScreenTransitionRepository(),
            coroutineScope = scope
        )
    }

    /**
     * Create a cached wrapper around an existing provider.
     *
     * Wraps the provider with a caching layer that reduces repository calls
     * by caching frequently accessed data.
     *
     * ## Caching Strategy
     * - Elements: Cached with configurable TTL, refreshed on screen change
     * - Commands: Cached with configurable TTL
     * - Preferences: Optionally cached with longer TTL
     *
     * @param provider The base provider to wrap
     * @param cacheConfig Cache configuration settings
     * @return [CachedAccessibilityData] wrapping the provider
     */
    @JvmStatic
    fun createCached(
        provider: AccessibilityDataProvider,
        cacheConfig: CacheConfiguration = CacheConfiguration.default()
    ): CachedAccessibilityData {
        return CachedAccessibilityData(
            delegate = provider,
            elementCacheSize = cacheConfig.maxCacheSize,
            commandCacheTtlMs = cacheConfig.commandCacheTtlMs
        )
    }

    /**
     * Create a mock provider for testing.
     *
     * Creates a mock implementation that allows setting test data programmatically.
     * Useful for unit tests and UI previews.
     *
     * @return [MockAccessibilityDataProvider] for testing
     */
    @JvmStatic
    fun createMock(): MockAccessibilityDataProvider {
        return MockAccessibilityDataProvider()
    }

    /**
     * Create a mock provider with pre-configured test data.
     *
     * Convenience method for creating a mock with initial data.
     *
     * @param elements Initial elements to set
     * @param commands Initial commands to set
     * @param preferences Initial preferences to set
     * @return Configured [MockAccessibilityDataProvider]
     */
    @JvmStatic
    fun createMockWithData(
        elements: List<QuantizedElement> = emptyList(),
        commands: List<QuantizedCommand> = emptyList(),
        preferences: Map<String, String> = emptyMap()
    ): MockAccessibilityDataProvider {
        return MockAccessibilityDataProvider().apply {
            setMockElements(elements)
            setMockCommands(commands)
            preferences.forEach { (key, value) ->
                setMockPreference(key, value)
            }
        }
    }

    /**
     * Get the cached provider if still available.
     *
     * Returns the last created provider if it hasn't been garbage collected.
     *
     * @return Cached provider or null
     */
    @JvmStatic
    fun getCachedProvider(): AndroidAccessibilityDataProvider? {
        return cachedProvider?.get()
    }

    /**
     * Clear the cached provider reference.
     */
    @JvmStatic
    fun clearCache() {
        cachedProvider = null
    }
}

/**
 * Configuration for AccessibilityDataProvider caching.
 *
 * Defines TTL (Time To Live) values and size limits for different
 * types of cached data.
 *
 * @property elementCacheTtlMs TTL for element cache in milliseconds
 * @property commandCacheTtlMs TTL for command cache in milliseconds
 * @property preferenceCacheTtlMs TTL for preference cache in milliseconds
 * @property maxCacheSize Maximum number of items to cache
 *
 * @since 1.0.0
 */
data class CacheConfiguration(
    val elementCacheTtlMs: Long = 1000L,
    val commandCacheTtlMs: Long = 5000L,
    val preferenceCacheTtlMs: Long = 30000L,
    val maxCacheSize: Int = 100
) {
    init {
        require(elementCacheTtlMs > 0) { "elementCacheTtlMs must be positive" }
        require(commandCacheTtlMs > 0) { "commandCacheTtlMs must be positive" }
        require(preferenceCacheTtlMs > 0) { "preferenceCacheTtlMs must be positive" }
        require(maxCacheSize > 0) { "maxCacheSize must be positive" }
    }

    companion object {
        /**
         * Default caching configuration.
         *
         * Provides reasonable defaults for typical usage:
         * - Elements: 1 second TTL (changes frequently)
         * - Commands: 5 seconds TTL (changes on learning)
         * - Preferences: 30 seconds TTL (changes rarely)
         * - Max size: 100 items
         */
        fun default() = CacheConfiguration(
            elementCacheTtlMs = 1000L,
            commandCacheTtlMs = 5000L,
            preferenceCacheTtlMs = 30000L,
            maxCacheSize = 100
        )

        /**
         * Aggressive caching for low-power mode.
         *
         * Longer TTLs to reduce repository access:
         * - Elements: 5 seconds TTL
         * - Commands: 30 seconds TTL
         * - Preferences: 2 minutes TTL
         */
        fun lowPower() = CacheConfiguration(
            elementCacheTtlMs = 5000L,
            commandCacheTtlMs = 30000L,
            preferenceCacheTtlMs = 120000L,
            maxCacheSize = 50
        )

        /**
         * Minimal caching for real-time needs.
         *
         * Short TTLs for frequently updating data:
         * - Elements: 500ms TTL
         * - Commands: 2 seconds TTL
         * - Preferences: 10 seconds TTL
         */
        fun realTime() = CacheConfiguration(
            elementCacheTtlMs = 500L,
            commandCacheTtlMs = 2000L,
            preferenceCacheTtlMs = 10000L,
            maxCacheSize = 200
        )

        /**
         * No caching - all data fetched from repository.
         *
         * Use for debugging or when data freshness is critical.
         */
        fun noCache() = CacheConfiguration(
            elementCacheTtlMs = 1L,
            commandCacheTtlMs = 1L,
            preferenceCacheTtlMs = 1L,
            maxCacheSize = 0
        )
    }
}

/**
 * Mock implementation of AccessibilityDataProvider for testing.
 *
 * Allows setting mock data programmatically and observing calls for
 * verification in unit tests.
 *
 * ## Usage Example
 * ```kotlin
 * val mock = MockAccessibilityDataProvider()
 *
 * // Setup mock data
 * mock.setMockElements(listOf(
 *     QuantizedElement(avid = "test1", type = ElementType.BUTTON, label = "Test")
 * ))
 * mock.setMockCommands(listOf(
 *     QuantizedCommand(avid = "cmd1", phrase = "click test", ...)
 * ))
 *
 * // Use in tests
 * val elements = mock.getCurrentScreenElements()
 * assertEquals(1, elements.size)
 * assertEquals("test1", elements[0].avid)
 * ```
 *
 * @since 1.0.0
 */
class MockAccessibilityDataProvider : AccessibilityDataProvider {

    // =========================================================================
    // Mock Data Storage
    // =========================================================================

    private val _mockElements = MutableStateFlow<List<QuantizedElement>>(emptyList())
    private val _mockCommands = MutableStateFlow<List<QuantizedCommand>>(emptyList())
    private val _mockPreferences = mutableMapOf<String, String>()
    private val _mockScreenContext = MutableStateFlow<ScreenContext?>(null)
    private val _mockCommandHistory = mutableListOf<CommandHistoryEntry>()
    private val _mockNavigationGraphs = mutableMapOf<String, NavigationGraph>()

    // =========================================================================
    // Call Tracking
    // =========================================================================

    /**
     * Track method calls for verification.
     */
    private val _methodCalls = mutableListOf<MethodCall>()

    /**
     * Get all recorded method calls.
     */
    val methodCalls: List<MethodCall> get() = _methodCalls.toList()

    /**
     * Represents a recorded method call.
     */
    data class MethodCall(
        val methodName: String,
        val arguments: Map<String, Any?>,
        val timestamp: Long = System.currentTimeMillis()
    )

    // =========================================================================
    // StateFlows
    // =========================================================================

    override val screenElementsFlow: StateFlow<List<QuantizedElement>> =
        _mockElements.asStateFlow()

    override val screenContextFlow: StateFlow<ScreenContext?> =
        _mockScreenContext.asStateFlow()

    // =========================================================================
    // Mock Data Setters
    // =========================================================================

    /**
     * Set mock elements for testing.
     *
     * @param elements List of elements to return from queries
     */
    fun setMockElements(elements: List<QuantizedElement>) {
        _mockElements.value = elements
    }

    /**
     * Set mock commands for testing.
     *
     * @param commands List of commands to return from queries
     */
    fun setMockCommands(commands: List<QuantizedCommand>) {
        _mockCommands.value = commands
    }

    /**
     * Set a mock preference value.
     *
     * @param key Preference key
     * @param value Preference value
     */
    fun setMockPreference(key: String, value: String) {
        _mockPreferences[key] = value
    }

    /**
     * Set mock screen context.
     *
     * @param context Screen context to return
     */
    fun setMockScreenContext(context: ScreenContext) {
        _mockScreenContext.value = context
    }

    /**
     * Set mock command history.
     *
     * @param history List of command history entries
     */
    fun setMockCommandHistory(history: List<CommandHistoryEntry>) {
        _mockCommandHistory.clear()
        _mockCommandHistory.addAll(history)
    }

    /**
     * Set mock navigation graph for a package.
     *
     * @param packageName Package name
     * @param graph Navigation graph
     */
    fun setMockNavigationGraph(packageName: String, graph: NavigationGraph) {
        _mockNavigationGraphs[packageName] = graph
    }

    /**
     * Add a single mock element.
     *
     * @param element Element to add
     */
    fun addMockElement(element: QuantizedElement) {
        _mockElements.value = _mockElements.value + element
    }

    /**
     * Add a single mock command.
     *
     * @param command Command to add
     */
    fun addMockCommand(command: QuantizedCommand) {
        _mockCommands.value = _mockCommands.value + command
    }

    /**
     * Clear all mock data.
     */
    fun clearMockData() {
        _mockElements.value = emptyList()
        _mockCommands.value = emptyList()
        _mockPreferences.clear()
        _mockScreenContext.value = null
        _mockCommandHistory.clear()
        _mockNavigationGraphs.clear()
        _methodCalls.clear()
    }

    // =========================================================================
    // AccessibilityDataProvider Implementation
    // =========================================================================

    override suspend fun getCurrentScreenElements(): List<QuantizedElement> {
        recordCall("getCurrentScreenElements")
        return _mockElements.value
    }

    override suspend fun getElement(avid: String): QuantizedElement? {
        recordCall("getElement", mapOf("avid" to avid))
        return _mockElements.value.find { it.avid == avid }
    }

    override suspend fun getScreenCommands(): List<QuantizedCommand> {
        recordCall("getScreenCommands")
        return _mockCommands.value
    }

    override suspend fun getCommandHistory(limit: Int, successOnly: Boolean): List<CommandHistoryEntry> {
        recordCall("getCommandHistory", mapOf("limit" to limit, "successOnly" to successOnly))
        var result = _mockCommandHistory.toList()
        if (successOnly) {
            result = result.filter { it.success }
        }
        return result.take(limit)
    }

    override suspend fun getTopCommands(limit: Int, context: String?): List<RankedCommand> {
        recordCall("getTopCommands", mapOf("limit" to limit, "context" to context))
        return _mockCommands.value
            .take(limit)
            .mapIndexed { index, command ->
                RankedCommand(
                    command = command,
                    usageCount = 10 - index,
                    lastUsed = System.currentTimeMillis() - (index * 1000),
                    contextScore = 1.0f - (index * 0.1f)
                )
            }
    }

    override suspend fun getScreenContext(): ScreenContext {
        recordCall("getScreenContext")
        return _mockScreenContext.value ?: ScreenContext.UNKNOWN
    }

    override suspend fun getNavigationGraph(packageName: String): NavigationGraph {
        recordCall("getNavigationGraph", mapOf("packageName" to packageName))
        return _mockNavigationGraphs[packageName] ?: NavigationGraph.empty(packageName)
    }

    override suspend fun getContextPreferences(): List<ContextPreference> {
        recordCall("getContextPreferences")
        return _mockPreferences.map { (key, value) ->
            ContextPreference(
                key = key,
                value = value,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        }
    }

    // =========================================================================
    // Test Verification Methods
    // =========================================================================

    /**
     * Record a method call for verification.
     */
    private fun recordCall(methodName: String, arguments: Map<String, Any?> = emptyMap()) {
        _methodCalls.add(MethodCall(methodName, arguments))
    }

    /**
     * Verify that a method was called.
     *
     * @param methodName Method name to verify
     * @return true if the method was called at least once
     */
    fun verifyCalled(methodName: String): Boolean {
        return _methodCalls.any { it.methodName == methodName }
    }

    /**
     * Verify that a method was called with specific arguments.
     *
     * @param methodName Method name to verify
     * @param arguments Expected arguments
     * @return true if the method was called with matching arguments
     */
    fun verifyCalledWith(methodName: String, arguments: Map<String, Any?>): Boolean {
        return _methodCalls.any { call ->
            call.methodName == methodName && call.arguments == arguments
        }
    }

    /**
     * Get the number of times a method was called.
     *
     * @param methodName Method name to count
     * @return Number of calls
     */
    fun callCount(methodName: String): Int {
        return _methodCalls.count { it.methodName == methodName }
    }

    /**
     * Clear recorded method calls.
     */
    fun clearCallHistory() {
        _methodCalls.clear()
    }
}

// =============================================================================
// No-Op Repository Implementations
// =============================================================================

/**
 * No-op implementation of PreferenceDataSource.
 */
internal class NoOpPreferenceDataSource : PreferenceDataSource {
    override suspend fun get(key: String): String? = null
    override suspend fun set(key: String, value: String) {}
    override suspend fun remove(key: String) {}
    override fun observe(key: String) = MutableStateFlow<String?>(null).asStateFlow()
    override suspend fun getAllPreferences(): List<PreferenceEntry> = emptyList()
}

/**
 * No-op implementation of ICommandHistoryRepository.
 */
internal class NoOpCommandHistoryRepository : ICommandHistoryRepository {
    override suspend fun insert(entry: com.augmentalis.database.dto.CommandHistoryDTO): Long = 0L
    override suspend fun getById(id: Long) = null
    override suspend fun getAll() = emptyList<com.augmentalis.database.dto.CommandHistoryDTO>()
    override suspend fun getByTimeRange(startTime: Long, endTime: Long) = emptyList<com.augmentalis.database.dto.CommandHistoryDTO>()
    override suspend fun getAfterTime(timestamp: Long) = emptyList<com.augmentalis.database.dto.CommandHistoryDTO>()
    override suspend fun getSuccessful() = emptyList<com.augmentalis.database.dto.CommandHistoryDTO>()
    override suspend fun getByEngine(engine: String) = emptyList<com.augmentalis.database.dto.CommandHistoryDTO>()
    override suspend fun getByLanguage(language: String) = emptyList<com.augmentalis.database.dto.CommandHistoryDTO>()
    override suspend fun getRecent(limit: Int) = emptyList<com.augmentalis.database.dto.CommandHistoryDTO>()
    override suspend fun getSuccessRate(): Double = 1.0
    override suspend fun getAverageExecutionTime(): Double = 0.0
    override suspend fun deleteOlderThan(timestamp: Long) {}
    override suspend fun cleanupOldEntries(cutoffTime: Long, retainCount: Long) {}
    override suspend fun deleteAll() {}
    override suspend fun count(): Long = 0L
    override suspend fun countSuccessful(): Long = 0L
}

/**
 * No-op implementation of IScreenContextRepository.
 */
internal class NoOpScreenContextRepository : IScreenContextRepository {
    override suspend fun insert(context: com.augmentalis.database.dto.ScreenContextDTO) {}
    override suspend fun insertBatch(contexts: List<com.augmentalis.database.dto.ScreenContextDTO>) {}
    override suspend fun getByHash(screenHash: String) = null
    override suspend fun getByApp(appId: String) = emptyList<com.augmentalis.database.dto.ScreenContextDTO>()
    override suspend fun getByPackage(packageName: String) = emptyList<com.augmentalis.database.dto.ScreenContextDTO>()
    override suspend fun getByActivity(activityName: String) = emptyList<com.augmentalis.database.dto.ScreenContextDTO>()
    override suspend fun getAll() = emptyList<com.augmentalis.database.dto.ScreenContextDTO>()
    override suspend fun deleteByHash(screenHash: String) {}
    override suspend fun deleteByApp(appId: String) {}
    override suspend fun deleteAll() {}
    override suspend fun count(): Long = 0L
    override suspend fun countByApp(appId: String): Long = 0L
}

/**
 * No-op implementation of IScreenTransitionRepository.
 */
internal class NoOpScreenTransitionRepository : IScreenTransitionRepository {
    override suspend fun insert(transition: com.augmentalis.database.dto.ScreenTransitionDTO): Long = 0L
    override suspend fun getById(id: Long) = null
    override suspend fun getFromScreen(fromScreenHash: String) = emptyList<com.augmentalis.database.dto.ScreenTransitionDTO>()
    override suspend fun getToScreen(toScreenHash: String) = emptyList<com.augmentalis.database.dto.ScreenTransitionDTO>()
    override suspend fun getByTrigger(triggerElementHash: String) = emptyList<com.augmentalis.database.dto.ScreenTransitionDTO>()
    override suspend fun getFrequent(limit: Long) = emptyList<com.augmentalis.database.dto.ScreenTransitionDTO>()
    override suspend fun recordTransition(fromScreenHash: String, toScreenHash: String, durationMs: Long, timestamp: Long) {}
    override suspend fun deleteById(id: Long) {}
    override suspend fun deleteByScreen(screenHash: String) {}
    override suspend fun deleteAll() {}
    override suspend fun count(): Long = 0L
}

// =============================================================================
// Extension Functions
// =============================================================================

/**
 * Create a provider from a DI container.
 *
 * Convenience extension for dependency injection frameworks.
 */
fun AccessibilityDataProviderFactory.createFromContainer(
    context: Context,
    container: RepositoryContainer,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
): AndroidAccessibilityDataProvider {
    return create(
        context = context,
        elementRepository = container.scrapedAppRepository,
        commandRepository = container.voiceCommandRepository,
        preferenceRepository = container.contextPreferenceRepository,
        commandHistoryRepository = container.commandHistoryRepository,
        screenContextRepository = container.screenContextRepository,
        screenTransitionRepository = container.screenTransitionRepository,
        scope = scope
    )
}

/**
 * Interface for dependency injection containers.
 */
interface RepositoryContainer {
    val scrapedAppRepository: IScrapedAppRepository
    val voiceCommandRepository: IVoiceCommandRepository
    val contextPreferenceRepository: IContextPreferenceRepository
    val commandHistoryRepository: ICommandHistoryRepository
    val screenContextRepository: IScreenContextRepository
    val screenTransitionRepository: IScreenTransitionRepository
}
