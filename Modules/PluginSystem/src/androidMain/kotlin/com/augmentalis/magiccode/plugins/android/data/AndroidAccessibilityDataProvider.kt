/**
 * AndroidAccessibilityDataProvider.kt - Android implementation of AccessibilityDataProvider
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2026-01-22
 *
 * Provides the Android-specific implementation of AccessibilityDataProvider that
 * connects to VoiceOSCore's repository layer for real accessibility data.
 */
package com.augmentalis.magiccode.plugins.android.data

import com.augmentalis.database.dto.CommandHistoryDTO
import com.augmentalis.database.dto.ContextPreferenceDTO
import com.augmentalis.database.dto.ScreenContextDTO
import com.augmentalis.database.dto.ScreenTransitionDTO
import com.augmentalis.database.repositories.ICommandHistoryRepository
import com.augmentalis.database.repositories.IContextPreferenceRepository
import com.augmentalis.database.repositories.IScreenContextRepository
import com.augmentalis.database.repositories.IScreenTransitionRepository
import com.augmentalis.database.repositories.IScrapedAppRepository
import com.augmentalis.database.repositories.IVoiceCommandRepository
import com.augmentalis.magiccode.plugins.universal.contracts.voiceoscore.ScreenContext
import com.augmentalis.magiccode.plugins.universal.data.AccessibilityDataProvider
import com.augmentalis.magiccode.plugins.universal.data.CommandHistoryEntry
import com.augmentalis.magiccode.plugins.universal.data.ContextPreference
import com.augmentalis.magiccode.plugins.universal.data.NavigationEdge
import com.augmentalis.magiccode.plugins.universal.data.NavigationGraph
import com.augmentalis.magiccode.plugins.universal.data.NavigationNode
import com.augmentalis.magiccode.plugins.universal.data.RankedCommand
import com.augmentalis.voiceoscore.CommandActionType
import com.augmentalis.voiceoscore.QuantizedCommand
import com.augmentalis.voiceoscore.QuantizedElement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Android implementation of [AccessibilityDataProvider].
 *
 * This implementation connects to VoiceOSCore's existing repository layer to provide
 * real accessibility data to plugins. It bridges the gap between the plugin system's
 * data contracts and VoiceOSCore's database repositories.
 *
 * ## Features
 * - Thread-safe StateFlow updates for reactive UI
 * - Proper coroutine scope management with cleanup
 * - Efficient data mapping from repository DTOs to plugin types
 * - Support for command history and ranking
 * - Navigation graph construction from screen transitions
 *
 * ## Repository Dependencies
 * The provider uses these VoiceOSCore repositories:
 * - [IScrapedAppRepository] - App metadata and learning status
 * - [IVoiceCommandRepository] - Learned voice commands
 * - [IContextPreferenceRepository] - User preferences per context
 * - [ICommandHistoryRepository] - Command execution history
 * - [IScreenContextRepository] - Screen metadata
 * - [IScreenTransitionRepository] - Navigation patterns
 *
 * ## Usage Example
 * ```kotlin
 * val provider = AndroidAccessibilityDataProvider(
 *     elementDataSource = repositoryAdapter.adaptElementRepository(scrapedAppRepo),
 *     commandDataSource = repositoryAdapter.adaptCommandRepository(voiceCommandRepo),
 *     preferenceDataSource = repositoryAdapter.adaptPreferenceRepository(prefRepo),
 *     commandHistoryRepository = commandHistoryRepo,
 *     screenContextRepository = screenContextRepo,
 *     screenTransitionRepository = screenTransitionRepo,
 *     coroutineScope = lifecycleScope
 * )
 *
 * // Use the provider
 * val elements = provider.getCurrentScreenElements()
 * val commands = provider.getScreenCommands()
 *
 * // Cleanup when done
 * provider.dispose()
 * ```
 *
 * @property elementDataSource Adapted source for screen elements
 * @property commandDataSource Adapted source for voice commands
 * @property preferenceDataSource Adapted source for preferences
 * @property commandHistoryRepository Repository for command history
 * @property screenContextRepository Repository for screen context
 * @property screenTransitionRepository Repository for navigation transitions
 * @property coroutineScope Scope for Flow collection and async operations
 *
 * @since 1.0.0
 * @see AccessibilityDataProvider
 * @see RepositoryAdapter
 */
class AndroidAccessibilityDataProvider(
    private val elementDataSource: ElementDataSource,
    private val commandDataSource: CommandDataSource,
    private val preferenceDataSource: PreferenceDataSource,
    private val commandHistoryRepository: ICommandHistoryRepository,
    private val screenContextRepository: IScreenContextRepository,
    private val screenTransitionRepository: IScreenTransitionRepository,
    private val coroutineScope: CoroutineScope
) : AccessibilityDataProvider {

    // =========================================================================
    // Internal State
    // =========================================================================

    /**
     * Mutex for thread-safe state updates.
     */
    private val mutex = Mutex()

    /**
     * Flag to track disposal state.
     */
    private val isDisposed = AtomicBoolean(false)

    /**
     * Internal job for managing subscriptions.
     */
    private val subscriptionJob = SupervisorJob()

    /**
     * Scope for internal operations.
     */
    private val internalScope = CoroutineScope(Dispatchers.Default + subscriptionJob)

    /**
     * Current package name being observed.
     */
    @Volatile
    private var currentPackageName: String = ""

    /**
     * Current activity name being observed.
     */
    @Volatile
    private var currentActivityName: String = ""

    // =========================================================================
    // StateFlows for Reactive Updates
    // =========================================================================

    /**
     * Internal mutable state for screen elements.
     */
    private val _screenElementsFlow = MutableStateFlow<List<QuantizedElement>>(emptyList())

    /**
     * Public read-only flow of current screen elements.
     */
    override val screenElementsFlow: StateFlow<List<QuantizedElement>> = _screenElementsFlow.asStateFlow()

    /**
     * Internal mutable state for screen context.
     */
    private val _screenContextFlow = MutableStateFlow<ScreenContext?>(null)

    /**
     * Public read-only flow of current screen context.
     */
    override val screenContextFlow: StateFlow<ScreenContext?> = _screenContextFlow.asStateFlow()

    // =========================================================================
    // Initialization
    // =========================================================================

    init {
        setupObservers()
    }

    /**
     * Setup observers for repository data changes.
     *
     * Subscribes to Flow emissions from adapted repositories and updates
     * internal StateFlows accordingly.
     */
    private fun setupObservers() {
        // Observe element changes
        internalScope.launch {
            elementDataSource.observeElements().collect { elements ->
                mutex.withLock {
                    _screenElementsFlow.value = elements
                }
            }
        }

        // Observe command changes (for potential future use in context updates)
        internalScope.launch {
            commandDataSource.observeCommands().collect { commands ->
                // Commands are fetched on-demand, but we could use this
                // for proactive caching or notifications
            }
        }
    }

    // =========================================================================
    // UI Element Data
    // =========================================================================

    /**
     * Get all accessible elements on the current screen.
     *
     * Returns the list of quantized elements representing the current UI state.
     * Elements are retrieved from the adapted element repository.
     *
     * @return List of [QuantizedElement] on current screen
     * @throws IllegalStateException if the provider has been disposed
     */
    override suspend fun getCurrentScreenElements(): List<QuantizedElement> {
        checkNotDisposed()
        return withContext(Dispatchers.IO) {
            elementDataSource.getCurrentElements()
        }
    }

    /**
     * Get a specific element by its AVID (Accessibility Voice ID).
     *
     * Searches the current screen elements for one matching the given AVID.
     *
     * @param avid The AVID of the element to retrieve
     * @return [QuantizedElement] or null if not found
     * @throws IllegalStateException if the provider has been disposed
     */
    override suspend fun getElement(avid: String): QuantizedElement? {
        checkNotDisposed()
        return withContext(Dispatchers.IO) {
            elementDataSource.findByAvid(avid)
        }
    }

    // =========================================================================
    // Command Data
    // =========================================================================

    /**
     * Get available voice commands for the current screen.
     *
     * Returns commands applicable to the current screen context, including
     * both learned and generated commands.
     *
     * @return List of [QuantizedCommand] for current screen
     * @throws IllegalStateException if the provider has been disposed
     */
    override suspend fun getScreenCommands(): List<QuantizedCommand> {
        checkNotDisposed()
        return withContext(Dispatchers.IO) {
            val context = getScreenContext()
            commandDataSource.getForScreen(context.screenId())
        }
    }

    /**
     * Get command execution history.
     *
     * Retrieves historical command executions from the database for analysis
     * and learning purposes.
     *
     * @param limit Maximum number of entries to return (default 100)
     * @param successOnly If true, only return successful commands
     * @return List of [CommandHistoryEntry] ordered by most recent first
     * @throws IllegalStateException if the provider has been disposed
     */
    override suspend fun getCommandHistory(limit: Int, successOnly: Boolean): List<CommandHistoryEntry> {
        checkNotDisposed()
        return withContext(Dispatchers.IO) {
            val history = if (successOnly) {
                commandHistoryRepository.getSuccessful()
            } else {
                commandHistoryRepository.getRecent(limit)
            }

            history.take(limit).map { dto ->
                mapCommandHistoryDTOToEntry(dto)
            }
        }
    }

    /**
     * Get top-ranked commands based on usage patterns.
     *
     * Returns commands ranked by frequency and recency, optionally filtered
     * by context (package name or screen ID).
     *
     * @param limit Maximum number of commands to return (default 20)
     * @param context Optional context filter (package name or screen ID)
     * @return List of [RankedCommand] ordered by rank
     * @throws IllegalStateException if the provider has been disposed
     */
    override suspend fun getTopCommands(limit: Int, context: String?): List<RankedCommand> {
        checkNotDisposed()
        return withContext(Dispatchers.IO) {
            val commands = if (context != null) {
                commandDataSource.getForScreen(context)
            } else {
                commandDataSource.getAllCommands()
            }

            // Get usage statistics from preference repository
            val usageStats = preferenceDataSource.getAllPreferences()
                .associateBy { it.key }

            // Rank commands based on usage
            commands.mapNotNull { command ->
                val stats = usageStats[command.avid]
                val usageCount = stats?.let { parseUsageCount(it.value) } ?: 0
                val lastUsed = stats?.updatedAt ?: 0L

                RankedCommand(
                    command = command,
                    usageCount = usageCount,
                    lastUsed = lastUsed,
                    contextScore = calculateContextScore(command, context),
                    successRate = calculateSuccessRate(command.avid)
                )
            }.sortedByDescending { it.rankScore }
                .take(limit)
        }
    }

    // =========================================================================
    // Screen Context
    // =========================================================================

    /**
     * Get the current screen context.
     *
     * Returns metadata about the current screen including package name,
     * activity, element counts, and classification.
     *
     * @return Current [ScreenContext]
     * @throws IllegalStateException if the provider has been disposed
     */
    override suspend fun getScreenContext(): ScreenContext {
        checkNotDisposed()
        return mutex.withLock {
            _screenContextFlow.value ?: ScreenContext.UNKNOWN
        }
    }

    /**
     * Get the navigation graph for an application.
     *
     * Returns the learned navigation structure showing how screens connect
     * within an application. Constructed from screen transition data.
     *
     * @param packageName Package name to get navigation for
     * @return [NavigationGraph] for the application
     * @throws IllegalStateException if the provider has been disposed
     */
    override suspend fun getNavigationGraph(packageName: String): NavigationGraph {
        checkNotDisposed()
        return withContext(Dispatchers.IO) {
            // Get all screens for this package
            val screens = screenContextRepository.getByPackage(packageName)

            // Get all transitions and filter for this package
            val allTransitions = mutableListOf<ScreenTransitionDTO>()
            screens.forEach { screen ->
                allTransitions.addAll(screenTransitionRepository.getFromScreen(screen.screenHash))
            }

            // Build navigation nodes
            val nodes = screens.map { screen ->
                NavigationNode(
                    screenId = screen.screenHash,
                    title = screen.windowTitle ?: screen.activityName ?: "Unknown",
                    packageName = packageName,
                    activityName = screen.activityName,
                    visitCount = screen.visitCount.toInt()
                )
            }

            // Build navigation edges
            val edges = allTransitions.map { transition ->
                NavigationEdge(
                    from = transition.fromScreenHash,
                    to = transition.toScreenHash,
                    action = transition.triggerAction,
                    frequency = transition.transitionCount.toInt(),
                    avgTimeMs = transition.avgDurationMs
                )
            }

            NavigationGraph(
                nodes = nodes,
                edges = edges,
                packageName = packageName,
                lastUpdated = System.currentTimeMillis()
            )
        }
    }

    // =========================================================================
    // User Preferences
    // =========================================================================

    /**
     * Get user context preferences.
     *
     * Returns user-defined preferences for specific contexts, such as
     * preferred actions or custom command mappings.
     *
     * @return List of [ContextPreference]
     * @throws IllegalStateException if the provider has been disposed
     */
    override suspend fun getContextPreferences(): List<ContextPreference> {
        checkNotDisposed()
        return withContext(Dispatchers.IO) {
            preferenceDataSource.getAllPreferences().map { dto ->
                ContextPreference(
                    key = dto.key,
                    value = dto.value,
                    packageName = dto.packageName,
                    screenId = dto.screenId,
                    createdAt = dto.createdAt,
                    updatedAt = dto.updatedAt
                )
            }
        }
    }

    // =========================================================================
    // State Update Methods (for platform integration)
    // =========================================================================

    /**
     * Update the current screen elements.
     *
     * Called by the accessibility service when screen content changes.
     * This method is thread-safe and updates the StateFlow.
     *
     * @param elements New list of screen elements
     */
    suspend fun updateScreenElements(elements: List<QuantizedElement>) {
        checkNotDisposed()
        mutex.withLock {
            _screenElementsFlow.value = elements
        }
    }

    /**
     * Update the current screen context.
     *
     * Called by the accessibility service when the user navigates to a new screen.
     * This method is thread-safe and updates the StateFlow.
     *
     * @param context New screen context
     */
    suspend fun updateScreenContext(context: ScreenContext) {
        checkNotDisposed()
        mutex.withLock {
            currentPackageName = context.packageName
            currentActivityName = context.activityName
            _screenContextFlow.value = context
        }
    }

    /**
     * Update screen context from package and activity names.
     *
     * Convenience method for updating context with minimal information.
     *
     * @param packageName Current package name
     * @param activityName Current activity name
     * @param elementCount Number of elements on screen
     */
    suspend fun updateScreenContext(
        packageName: String,
        activityName: String,
        elementCount: Int = 0
    ) {
        val context = ScreenContext(
            packageName = packageName,
            activityName = activityName,
            screenTitle = null,
            elementCount = elementCount,
            primaryAction = null
        )
        updateScreenContext(context)
    }

    /**
     * Get the current package name.
     *
     * @return Current package name or empty string if not set
     */
    suspend fun getCurrentPackageName(): String {
        checkNotDisposed()
        return mutex.withLock {
            currentPackageName
        }
    }

    /**
     * Get the current activity name.
     *
     * @return Current activity name or empty string if not set
     */
    suspend fun getCurrentActivityName(): String {
        checkNotDisposed()
        return mutex.withLock {
            currentActivityName
        }
    }

    // =========================================================================
    // Lifecycle Management
    // =========================================================================

    /**
     * Dispose of this provider and release resources.
     *
     * Cancels all active coroutines and clears internal state.
     * After disposal, the provider cannot be used.
     */
    fun dispose() {
        if (isDisposed.compareAndSet(false, true)) {
            subscriptionJob.cancel()
            internalScope.cancel()
        }
    }

    /**
     * Check if this provider has been disposed.
     *
     * @return true if disposed
     */
    fun isDisposed(): Boolean = isDisposed.get()

    // =========================================================================
    // Private Helpers
    // =========================================================================

    /**
     * Check that the provider has not been disposed.
     *
     * @throws IllegalStateException if disposed
     */
    private fun checkNotDisposed() {
        check(!isDisposed.get()) {
            "AndroidAccessibilityDataProvider has been disposed"
        }
    }

    /**
     * Map CommandHistoryDTO to CommandHistoryEntry.
     *
     * Note: CommandHistoryDTO has these fields:
     * - originalText: The original spoken text
     * - processedCommand: The processed command
     * - confidence: Double confidence value
     * - timestamp: Execution timestamp
     * - engineUsed: Speech engine used
     * - success: Boolean success flag
     * - executionTimeMs: Execution time
     * - errorMessage: Optional error message
     */
    private suspend fun mapCommandHistoryDTOToEntry(dto: CommandHistoryDTO): CommandHistoryEntry {
        // Reconstruct the command from the DTO
        val command = QuantizedCommand(
            avid = dto.processedCommand.hashCode().toString(),
            phrase = dto.originalText,
            actionType = CommandActionType.CLICK, // Default action
            targetAvid = null,
            confidence = dto.confidence.toFloat(),
            metadata = mapOf("engineUsed" to dto.engineUsed)
        )

        return CommandHistoryEntry(
            command = command,
            timestamp = dto.timestamp,
            success = dto.success,
            executionTimeMs = dto.executionTimeMs,
            errorMessage = dto.errorMessage
        )
    }

    /**
     * Parse usage count from preference value.
     */
    private fun parseUsageCount(value: String): Int {
        return try {
            value.toInt()
        } catch (e: NumberFormatException) {
            0
        }
    }

    /**
     * Calculate context relevance score for a command.
     */
    private fun calculateContextScore(command: QuantizedCommand, context: String?): Float {
        if (context == null) return 0.5f

        val packageMatch = command.packageName == context
        val screenMatch = command.screenId == context

        return when {
            screenMatch -> 1.0f
            packageMatch -> 0.7f
            else -> 0.3f
        }
    }

    /**
     * Calculate success rate for a command from history.
     */
    private suspend fun calculateSuccessRate(commandId: String): Float {
        return try {
            val history = commandHistoryRepository.getRecent(100)
                .filter { it.processedCommand == commandId }

            if (history.isEmpty()) return 1.0f

            val successCount = history.count { it.success }
            successCount.toFloat() / history.size
        } catch (e: Exception) {
            1.0f
        }
    }

    companion object {
        /**
         * Create an AndroidAccessibilityDataProvider from raw repositories.
         *
         * This factory method handles repository adaptation automatically.
         *
         * @param scrapedAppRepository Repository for app data
         * @param voiceCommandRepository Repository for commands
         * @param contextPreferenceRepository Repository for preferences
         * @param commandHistoryRepository Repository for command history
         * @param screenContextRepository Repository for screen context
         * @param screenTransitionRepository Repository for transitions
         * @param coroutineScope Scope for async operations
         * @return Configured AndroidAccessibilityDataProvider
         */
        fun create(
            scrapedAppRepository: IScrapedAppRepository,
            voiceCommandRepository: IVoiceCommandRepository,
            contextPreferenceRepository: IContextPreferenceRepository,
            commandHistoryRepository: ICommandHistoryRepository,
            screenContextRepository: IScreenContextRepository,
            screenTransitionRepository: IScreenTransitionRepository,
            coroutineScope: CoroutineScope
        ): AndroidAccessibilityDataProvider {
            val adapter = RepositoryAdapter()

            return AndroidAccessibilityDataProvider(
                elementDataSource = adapter.adaptElementRepository(scrapedAppRepository),
                commandDataSource = adapter.adaptCommandRepository(voiceCommandRepository),
                preferenceDataSource = adapter.adaptPreferenceRepository(contextPreferenceRepository),
                commandHistoryRepository = commandHistoryRepository,
                screenContextRepository = screenContextRepository,
                screenTransitionRepository = screenTransitionRepository,
                coroutineScope = coroutineScope
            )
        }
    }
}

// =============================================================================
// Extension Functions
// =============================================================================

/**
 * Create a provider from a repository holder.
 *
 * Convenience extension for creating providers from dependency containers.
 */
suspend fun AndroidAccessibilityDataProvider.Companion.fromRepositories(
    repositories: RepositoryHolder,
    scope: CoroutineScope
): AndroidAccessibilityDataProvider {
    return create(
        scrapedAppRepository = repositories.scrapedAppRepository,
        voiceCommandRepository = repositories.voiceCommandRepository,
        contextPreferenceRepository = repositories.contextPreferenceRepository,
        commandHistoryRepository = repositories.commandHistoryRepository,
        screenContextRepository = repositories.screenContextRepository,
        screenTransitionRepository = repositories.screenTransitionRepository,
        coroutineScope = scope
    )
}

/**
 * Interface for holding repository references.
 *
 * Implementations provide access to all required repositories.
 */
interface RepositoryHolder {
    val scrapedAppRepository: IScrapedAppRepository
    val voiceCommandRepository: IVoiceCommandRepository
    val contextPreferenceRepository: IContextPreferenceRepository
    val commandHistoryRepository: ICommandHistoryRepository
    val screenContextRepository: IScreenContextRepository
    val screenTransitionRepository: IScreenTransitionRepository
}
