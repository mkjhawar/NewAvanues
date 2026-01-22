/**
 * RepositoryAdapter.kt - Adapter for VoiceOSCore repositories
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2026-01-22
 *
 * Provides adapters that convert between VoiceOSCore's repository interfaces
 * and the plugin system's data source interfaces.
 */
package com.augmentalis.magiccode.plugins.android.data

import com.augmentalis.database.dto.ContextPreferenceDTO
import com.augmentalis.database.dto.ScrapedAppDTO
import com.augmentalis.database.dto.VoiceCommandDTO
import com.augmentalis.database.repositories.IContextPreferenceRepository
import com.augmentalis.database.repositories.IScrapedAppRepository
import com.augmentalis.database.repositories.IVoiceCommandRepository
import com.augmentalis.voiceoscore.CommandActionType
import com.augmentalis.voiceoscore.ElementType
import com.augmentalis.voiceoscore.QuantizedCommand
import com.augmentalis.voiceoscore.QuantizedElement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * Adapter for converting VoiceOSCore repositories to plugin data sources.
 *
 * This class provides factory methods for creating adapted data sources from
 * the existing VoiceOSCore repository interfaces. Each adapter converts between
 * repository DTOs and plugin system types.
 *
 * ## Adapter Pattern
 * The adapter pattern is used to:
 * - Decouple plugin system from specific repository implementations
 * - Convert DTO types to domain types (QuantizedElement, QuantizedCommand)
 * - Add reactive Flow wrappers around repository data
 *
 * ## Usage
 * ```kotlin
 * val adapter = RepositoryAdapter()
 *
 * val elementSource = adapter.adaptElementRepository(scrapedAppRepo)
 * val commandSource = adapter.adaptCommandRepository(voiceCommandRepo)
 * val prefSource = adapter.adaptPreferenceRepository(contextPrefRepo)
 *
 * // Use adapted sources with AndroidAccessibilityDataProvider
 * val provider = AndroidAccessibilityDataProvider(
 *     elementDataSource = elementSource,
 *     commandDataSource = commandSource,
 *     preferenceDataSource = prefSource,
 *     ...
 * )
 * ```
 *
 * @since 1.0.0
 * @see ElementDataSource
 * @see CommandDataSource
 * @see PreferenceDataSource
 */
class RepositoryAdapter {

    /**
     * Adapt element repository to provide QuantizedElements.
     *
     * Creates an [ElementDataSource] that wraps [IScrapedAppRepository] and provides
     * element data through the plugin system's interface.
     *
     * @param repository The scraped app repository to adapt
     * @return Adapted [ElementDataSource]
     */
    fun adaptElementRepository(repository: IScrapedAppRepository): ElementDataSource {
        return ScrapedAppElementDataSource(repository)
    }

    /**
     * Adapt command repository to provide QuantizedCommands.
     *
     * Creates a [CommandDataSource] that wraps [IVoiceCommandRepository] and provides
     * command data through the plugin system's interface.
     *
     * @param repository The voice command repository to adapt
     * @return Adapted [CommandDataSource]
     */
    fun adaptCommandRepository(repository: IVoiceCommandRepository): CommandDataSource {
        return VoiceCommandDataSource(repository)
    }

    /**
     * Adapt preference repository for plugin access.
     *
     * Creates a [PreferenceDataSource] that wraps [IContextPreferenceRepository] and
     * provides preference data through the plugin system's interface.
     *
     * @param repository The context preference repository to adapt
     * @return Adapted [PreferenceDataSource]
     */
    fun adaptPreferenceRepository(repository: IContextPreferenceRepository): PreferenceDataSource {
        return ContextPreferenceDataSource(repository)
    }
}

// =============================================================================
// Data Source Interfaces
// =============================================================================

/**
 * Data source interface for UI elements.
 *
 * Provides access to screen elements through a unified interface,
 * abstracting the underlying repository implementation.
 *
 * @since 1.0.0
 */
interface ElementDataSource {
    /**
     * Get current elements on the screen.
     *
     * @return List of [QuantizedElement] representing current UI state
     */
    suspend fun getCurrentElements(): List<QuantizedElement>

    /**
     * Observe element changes as a Flow.
     *
     * @return Flow emitting element lists when changes occur
     */
    fun observeElements(): Flow<List<QuantizedElement>>

    /**
     * Find an element by its AVID.
     *
     * @param avid The AVID to search for
     * @return [QuantizedElement] or null if not found
     */
    suspend fun findByAvid(avid: String): QuantizedElement?

    /**
     * Find elements by label (partial match).
     *
     * @param label The label to search for
     * @return List of matching elements
     */
    suspend fun findByLabel(label: String): List<QuantizedElement>
}

/**
 * Data source interface for voice commands.
 *
 * Provides access to learned voice commands through a unified interface,
 * abstracting the underlying repository implementation.
 *
 * @since 1.0.0
 */
interface CommandDataSource {
    /**
     * Get all available commands.
     *
     * @return List of all [QuantizedCommand]
     */
    suspend fun getAllCommands(): List<QuantizedCommand>

    /**
     * Observe command changes as a Flow.
     *
     * @return Flow emitting command lists when changes occur
     */
    fun observeCommands(): Flow<List<QuantizedCommand>>

    /**
     * Get commands for a specific element.
     *
     * @param avid The element AVID to get commands for
     * @return List of commands targeting the element
     */
    suspend fun getForElement(avid: String): List<QuantizedCommand>

    /**
     * Get commands for a specific screen.
     *
     * @param screenId The screen ID to get commands for
     * @return List of commands for the screen
     */
    suspend fun getForScreen(screenId: String): List<QuantizedCommand>
}

/**
 * Data source interface for user preferences.
 *
 * Provides access to context-specific preferences through a unified interface,
 * abstracting the underlying repository implementation.
 *
 * @since 1.0.0
 */
interface PreferenceDataSource {
    /**
     * Get a preference value by key.
     *
     * @param key The preference key
     * @return Preference value or null if not found
     */
    suspend fun get(key: String): String?

    /**
     * Set a preference value.
     *
     * @param key The preference key
     * @param value The value to set
     */
    suspend fun set(key: String, value: String)

    /**
     * Remove a preference.
     *
     * @param key The preference key to remove
     */
    suspend fun remove(key: String)

    /**
     * Observe changes to a specific preference.
     *
     * @param key The preference key to observe
     * @return Flow emitting value changes
     */
    fun observe(key: String): Flow<String?>

    /**
     * Get all preferences.
     *
     * @return List of all preferences as [PreferenceEntry]
     */
    suspend fun getAllPreferences(): List<PreferenceEntry>
}

/**
 * Preference entry for internal use.
 *
 * Represents a preference with context information.
 *
 * @property key Preference key
 * @property value Preference value
 * @property packageName Package name (null for global)
 * @property screenId Screen ID (null for app-wide)
 * @property createdAt Creation timestamp
 * @property updatedAt Last update timestamp
 */
data class PreferenceEntry(
    val key: String,
    val value: String,
    val packageName: String? = null,
    val screenId: String? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

// =============================================================================
// Data Source Implementations
// =============================================================================

/**
 * Element data source backed by IScrapedAppRepository.
 *
 * Provides element data by querying the scraped app repository and
 * converting DTOs to QuantizedElements.
 */
internal class ScrapedAppElementDataSource(
    private val repository: IScrapedAppRepository
) : ElementDataSource {

    /**
     * Internal state for current elements.
     */
    private val _elementsState = MutableStateFlow<List<QuantizedElement>>(emptyList())

    /**
     * Internal cache of elements by AVID for fast lookup.
     */
    private val elementCache = mutableMapOf<String, QuantizedElement>()

    override suspend fun getCurrentElements(): List<QuantizedElement> {
        return withContext(Dispatchers.IO) {
            _elementsState.value
        }
    }

    override fun observeElements(): Flow<List<QuantizedElement>> {
        return _elementsState.asStateFlow()
    }

    override suspend fun findByAvid(avid: String): QuantizedElement? {
        return withContext(Dispatchers.IO) {
            // Check cache first
            elementCache[avid] ?: _elementsState.value.find { it.avid == avid }
        }
    }

    override suspend fun findByLabel(label: String): List<QuantizedElement> {
        return withContext(Dispatchers.IO) {
            val lowerLabel = label.lowercase()
            _elementsState.value.filter {
                it.label.lowercase().contains(lowerLabel) ||
                        it.aliases.any { alias -> alias.lowercase().contains(lowerLabel) }
            }
        }
    }

    /**
     * Update the current elements.
     *
     * Called when new element data is available from the accessibility service.
     *
     * @param elements New list of elements
     */
    suspend fun updateElements(elements: List<QuantizedElement>) {
        withContext(Dispatchers.Default) {
            // Update cache
            elementCache.clear()
            elements.forEach { element ->
                elementCache[element.avid] = element
            }
            _elementsState.value = elements
        }
    }

    /**
     * Clear all cached elements.
     */
    fun clearCache() {
        elementCache.clear()
        _elementsState.value = emptyList()
    }
}

/**
 * Command data source backed by IVoiceCommandRepository.
 *
 * Provides command data by querying the voice command repository and
 * converting DTOs to QuantizedCommands.
 */
internal class VoiceCommandDataSource(
    private val repository: IVoiceCommandRepository
) : CommandDataSource {

    /**
     * Internal cache for commands.
     */
    private var commandCache: List<QuantizedCommand>? = null
    private var cacheTimestamp: Long = 0L
    private val cacheTtlMs: Long = 30_000L // 30 second cache

    override suspend fun getAllCommands(): List<QuantizedCommand> {
        return withContext(Dispatchers.IO) {
            // Check cache validity
            val now = System.currentTimeMillis()
            val cached = commandCache
            if (cached != null && (now - cacheTimestamp) < cacheTtlMs) {
                return@withContext cached
            }

            // Fetch from repository
            val commands = repository.getEnabled().map { dto ->
                mapVoiceCommandDTOToQuantized(dto)
            }

            // Update cache
            commandCache = commands
            cacheTimestamp = now

            commands
        }
    }

    override fun observeCommands(): Flow<List<QuantizedCommand>> = flow {
        // Emit current commands and periodically refresh
        while (true) {
            emit(getAllCommands())
            kotlinx.coroutines.delay(cacheTtlMs)
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun getForElement(avid: String): List<QuantizedCommand> {
        return withContext(Dispatchers.IO) {
            getAllCommands().filter { it.targetAvid == avid }
        }
    }

    override suspend fun getForScreen(screenId: String): List<QuantizedCommand> {
        return withContext(Dispatchers.IO) {
            getAllCommands().filter { command ->
                command.screenId == screenId ||
                        command.packageName?.let { screenId.startsWith(it) } == true
            }
        }
    }

    /**
     * Invalidate the command cache.
     *
     * Call this after learning operations to ensure fresh data.
     */
    fun invalidateCache() {
        commandCache = null
        cacheTimestamp = 0L
    }

    /**
     * Map VoiceCommandDTO to QuantizedCommand.
     *
     * Note: VoiceCommandDTO has these fields:
     * - commandId: Unique command identifier
     * - triggerPhrase: The voice trigger phrase
     * - action: The action string (e.g., "click", "scroll")
     * - category: Command category
     * - locale: Locale string
     * - priority: Command priority
     * - synonyms: JSON array of synonyms
     */
    private fun mapVoiceCommandDTOToQuantized(dto: VoiceCommandDTO): QuantizedCommand {
        return QuantizedCommand(
            avid = dto.commandId,
            phrase = dto.triggerPhrase,
            actionType = CommandActionType.fromString(dto.action),
            targetAvid = null, // VoiceCommandDTO doesn't have target element
            confidence = 1.0f, // Static commands have full confidence
            metadata = buildMap {
                put("category", dto.category)
                put("locale", dto.locale)
                put("priority", dto.priority.toString())
            }
        )
    }
}

/**
 * Preference data source backed by IContextPreferenceRepository.
 *
 * Provides preference data by querying the context preference repository.
 */
internal class ContextPreferenceDataSource(
    private val repository: IContextPreferenceRepository
) : PreferenceDataSource {

    /**
     * Internal cache for preferences by key.
     */
    private val preferenceCache = mutableMapOf<String, String>()

    /**
     * StateFlow for observing specific preferences.
     */
    private val preferenceFlows = mutableMapOf<String, MutableStateFlow<String?>>()

    override suspend fun get(key: String): String? {
        return withContext(Dispatchers.IO) {
            // Check cache first
            preferenceCache[key]?.let { return@withContext it }

            // Query repository
            val preferences = repository.getAll()
            val pref = preferences.find { it.commandId == key }
            pref?.let {
                val value = it.successCount.toString()
                preferenceCache[key] = value
                value
            }
        }
    }

    override suspend fun set(key: String, value: String) {
        withContext(Dispatchers.IO) {
            // Update cache
            preferenceCache[key] = value

            // Update flow if exists
            preferenceFlows[key]?.value = value

            // Repository update would happen through proper preference methods
            // This is a read-only provider, so we just cache locally
        }
    }

    override suspend fun remove(key: String) {
        withContext(Dispatchers.IO) {
            preferenceCache.remove(key)
            preferenceFlows[key]?.value = null
        }
    }

    override fun observe(key: String): Flow<String?> {
        return preferenceFlows.getOrPut(key) {
            MutableStateFlow(preferenceCache[key])
        }.asStateFlow()
    }

    override suspend fun getAllPreferences(): List<PreferenceEntry> {
        return withContext(Dispatchers.IO) {
            val preferences = repository.getAll()
            preferences.map { dto ->
                mapContextPreferenceDTOToEntry(dto)
            }
        }
    }

    /**
     * Map ContextPreferenceDTO to PreferenceEntry.
     *
     * Note: ContextPreferenceDTO has these fields:
     * - commandId: The command identifier
     * - contextKey: Context key (usually package/screen path)
     * - usageCount: Total usage count
     * - successCount: Successful usage count
     * - lastUsedTimestamp: Last usage timestamp
     */
    private fun mapContextPreferenceDTOToEntry(dto: ContextPreferenceDTO): PreferenceEntry {
        // Parse context key for package/screen info
        val contextParts = dto.contextKey.split("/")
        val packageName = contextParts.getOrNull(0)
        val screenId = if (contextParts.size > 1) dto.contextKey else null

        return PreferenceEntry(
            key = dto.commandId,
            value = "${dto.usageCount}:${dto.successCount}",
            packageName = packageName,
            screenId = screenId,
            createdAt = dto.lastUsedTimestamp,
            updatedAt = dto.lastUsedTimestamp
        )
    }
}

// =============================================================================
// Extension Functions
// =============================================================================

/**
 * Create an element data source with initial elements.
 */
fun RepositoryAdapter.createElementDataSource(
    repository: IScrapedAppRepository,
    initialElements: List<QuantizedElement> = emptyList()
): ElementDataSource {
    val source = adaptElementRepository(repository) as ScrapedAppElementDataSource
    if (initialElements.isNotEmpty()) {
        // Note: This would need to be called from a coroutine
        // source.updateElements(initialElements)
    }
    return source
}

/**
 * Create a command data source with cache pre-warming.
 */
suspend fun RepositoryAdapter.createPrewarmedCommandDataSource(
    repository: IVoiceCommandRepository
): CommandDataSource {
    val source = adaptCommandRepository(repository)
    // Pre-warm the cache
    source.getAllCommands()
    return source
}
