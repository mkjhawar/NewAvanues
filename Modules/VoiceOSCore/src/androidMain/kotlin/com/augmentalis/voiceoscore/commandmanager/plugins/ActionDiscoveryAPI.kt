/**
 * ActionDiscoveryAPI.kt - Enhancement 1: Action Discovery API
 *
 * Programmatic way to query available actions, search by criteria,
 * and discover plugin capabilities.
 *
 * Part of Q12 Enhancement 1
 *
 * @since VOS4 Phase 4.1
 */

package com.augmentalis.voiceoscore.commandmanager.plugins

import com.augmentalis.voiceoscore.commandmanager.dynamic.CommandCategory

/**
 * API for discovering available actions and plugins
 *
 * Allows programmatic querying of:
 * - What plugins are loaded
 * - What commands each plugin handles
 * - Plugin capabilities and permissions
 * - Action metadata
 *
 * Use cases:
 * - Voice assistant: "What commands can I say?"
 * - UI: Display available commands by category
 * - Plugin development: Check for command conflicts
 * - Analytics: Track which actions are available
 */
interface ActionDiscoveryAPI {
    /**
     * Get all available actions across all plugins
     *
     * @return List of all action metadata from all loaded plugins
     */
    fun getAvailableActions(): List<ActionMetadata>

    /**
     * Search for actions by query string
     *
     * Searches across:
     * - Command phrases
     * - Action descriptions
     * - Plugin names
     *
     * @param query Search query (case-insensitive)
     * @return Matching actions sorted by relevance
     */
    fun searchActions(query: String): List<ActionMetadata>

    /**
     * Get metadata for a specific action
     *
     * @param actionId Unique action identifier
     * @return Action metadata or null if not found
     */
    fun getActionMetadata(actionId: String): ActionMetadata?

    /**
     * Check if an action is currently available
     *
     * An action may be unavailable if:
     * - Plugin is disabled
     * - Plugin is in degraded state
     * - Required permissions are not granted
     *
     * @param actionId Action identifier
     * @return true if action can be executed, false otherwise
     */
    fun isActionAvailable(actionId: String): Boolean

    /**
     * Get actions by category
     *
     * @param category Command category to filter by
     * @return Actions in the specified category
     */
    fun getActionsByCategory(category: CommandCategory): List<ActionMetadata>

    /**
     * Get actions by plugin
     *
     * @param pluginId Plugin identifier
     * @return All actions provided by the specified plugin
     */
    fun getActionsByPlugin(pluginId: String): List<ActionMetadata>

    /**
     * Get actions that require specific permissions
     *
     * @param permission Permission to filter by
     * @return Actions requiring the specified permission
     */
    fun getActionsByPermission(permission: PluginPermission): List<ActionMetadata>

    /**
     * Get plugin information
     *
     * @param pluginId Plugin identifier
     * @return Plugin info or null if not found
     */
    fun getPluginInfo(pluginId: String): PluginInfo?

    /**
     * Get all loaded plugins
     *
     * @return List of all plugin information
     */
    fun getAllPlugins(): List<PluginInfo>

    /**
     * Get similar actions to a given action
     *
     * Useful for suggesting alternatives when an action fails.
     *
     * @param actionId Action to find similar actions for
     * @param limit Maximum number of similar actions to return
     * @return Similar actions sorted by similarity
     */
    fun getSimilarActions(actionId: String, limit: Int = 5): List<ActionMetadata>
}

/**
 * Metadata about an action
 *
 * Describes what an action does, what commands trigger it,
 * and what capabilities it has.
 */
data class ActionMetadata(
    /** Unique identifier for this action */
    val actionId: String,

    /** Plugin that provides this action */
    val pluginId: String,

    /** Command category */
    val category: CommandCategory,

    /** Voice command phrases that trigger this action */
    val supportedCommands: List<String>,

    /** Human-readable description of what this action does */
    val description: String,

    /** Version of the plugin providing this action */
    val version: String,

    /** Whether this action is from a plugin or built-in */
    val isPlugin: Boolean,

    /** Permissions required by this action */
    val requiredPermissions: List<PluginPermission> = emptyList(),

    /** Whether this action is currently available */
    val isAvailable: Boolean = true,

    /** Estimated execution time (milliseconds) */
    val estimatedExecutionTime: Long? = null,

    /** Usage statistics */
    val usageCount: Long = 0,

    /** Success rate (0.0 to 1.0) */
    val successRate: Double = 0.0,

    /** Additional metadata */
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Plugin information
 */
data class PluginInfo(
    /** Unique plugin identifier */
    val pluginId: String,

    /** Plugin name */
    val name: String,

    /** Plugin version */
    val version: String,

    /** Plugin description */
    val description: String,

    /** Plugin author */
    val author: String,

    /** Minimum VOS version required */
    val minVOSVersion: Int,

    /** Current plugin state */
    val state: PluginState,

    /** Number of actions provided by this plugin */
    val actionCount: Int,

    /** Granted permissions */
    val grantedPermissions: Set<PluginPermission>,

    /** When plugin was loaded */
    val loadedAt: Long,

    /** Plugin statistics */
    val statistics: Map<String, Any> = emptyMap()
)

/**
 * Default implementation of ActionDiscoveryAPI
 */
class ActionDiscoveryAPIImpl(
    private val pluginManager: PluginManager
) : ActionDiscoveryAPI {

    override fun getAvailableActions(): List<ActionMetadata> {
        val actions = mutableListOf<ActionMetadata>()

        for ((pluginId, plugin) in pluginManager.getLoadedPlugins()) {
            val metadata = pluginManager.getPluginMetadata(pluginId) ?: continue
            val state = pluginManager.getPluginState(pluginId) ?: continue
            val stats = pluginManager.getPluginStatistics(pluginId)

            // Get category from metadata or plugin, convert to CommandCategory
            val categoryStr = metadata.category.ifEmpty { plugin.category }
            val category = try {
                CommandCategory.valueOf(categoryStr.uppercase())
            } catch (e: IllegalArgumentException) {
                CommandCategory.CUSTOM
            }

            for (command in plugin.supportedCommands) {
                actions.add(ActionMetadata(
                    actionId = "$pluginId:$command",
                    pluginId = pluginId,
                    category = category,
                    supportedCommands = listOf(command),
                    description = plugin.description,
                    version = plugin.version,
                    isPlugin = true,
                    requiredPermissions = plugin.requestedPermissions,
                    isAvailable = state == PluginState.LOADED,
                    usageCount = stats?.commandsExecuted ?: 0,
                    successRate = stats?.getSuccessRate() ?: 0.0
                ))
            }
        }

        return actions
    }

    override fun searchActions(query: String): List<ActionMetadata> {
        val normalizedQuery = query.lowercase()

        return getAvailableActions()
            .filter { action ->
                // Search in commands
                action.supportedCommands.any { it.lowercase().contains(normalizedQuery) } ||
                // Search in description
                action.description.lowercase().contains(normalizedQuery) ||
                // Search in plugin name
                action.pluginId.lowercase().contains(normalizedQuery)
            }
            .sortedByDescending { action ->
                // Calculate relevance score
                var score = 0.0

                // Exact match in commands (highest priority)
                if (action.supportedCommands.any { it.lowercase() == normalizedQuery }) {
                    score += 100.0
                }

                // Starts with query in commands
                if (action.supportedCommands.any { it.lowercase().startsWith(normalizedQuery) }) {
                    score += 50.0
                }

                // Contains query in commands
                if (action.supportedCommands.any { it.lowercase().contains(normalizedQuery) }) {
                    score += 25.0
                }

                // Contains query in description
                if (action.description.lowercase().contains(normalizedQuery)) {
                    score += 10.0
                }

                // Boost by usage count
                score += (action.usageCount / 100.0)

                // Boost by success rate
                score += (action.successRate * 10.0)

                score
            }
    }

    override fun getActionMetadata(actionId: String): ActionMetadata? {
        return getAvailableActions().find { it.actionId == actionId }
    }

    override fun isActionAvailable(actionId: String): Boolean {
        val action = getActionMetadata(actionId) ?: return false
        return action.isAvailable
    }

    override fun getActionsByCategory(category: CommandCategory): List<ActionMetadata> {
        return getAvailableActions().filter { it.category == category }
    }

    override fun getActionsByPlugin(pluginId: String): List<ActionMetadata> {
        return getAvailableActions().filter { it.pluginId == pluginId }
    }

    override fun getActionsByPermission(permission: PluginPermission): List<ActionMetadata> {
        return getAvailableActions().filter { permission in it.requiredPermissions }
    }

    override fun getPluginInfo(pluginId: String): PluginInfo? {
        val plugin = pluginManager.getPlugin(pluginId) ?: return null
        val metadata = pluginManager.getPluginMetadata(pluginId) ?: return null
        val state = pluginManager.getPluginState(pluginId) ?: return null
        val permissions = pluginManager.getPluginPermissions(pluginId)
        val loadedAt = pluginManager.getPluginLoadedAt(pluginId) ?: 0L
        val stats = pluginManager.getPluginStatistics(pluginId)

        return PluginInfo(
            pluginId = pluginId,
            name = plugin.name,
            version = plugin.version,
            description = plugin.description,
            author = plugin.author,
            minVOSVersion = plugin.minVOSVersion,
            state = state,
            actionCount = plugin.supportedCommands.size,
            grantedPermissions = permissions?.getGrantedPermissions() ?: emptySet(),
            loadedAt = loadedAt,
            statistics = stats?.let {
                mapOf(
                    "commandsExecuted" to it.commandsExecuted,
                    "successCount" to it.successCount,
                    "errorCount" to it.errorCount,
                    "successRate" to it.getSuccessRate(),
                    "avgExecutionTime" to it.getAverageExecutionTime()
                )
            } ?: plugin.getStatistics()
        )
    }

    override fun getAllPlugins(): List<PluginInfo> {
        return pluginManager.getLoadedPlugins().keys.mapNotNull { getPluginInfo(it) }
    }

    override fun getSimilarActions(actionId: String, limit: Int): List<ActionMetadata> {
        val action = getActionMetadata(actionId) ?: return emptyList()

        return getAvailableActions()
            .filter { it.actionId != actionId }
            .map { other ->
                val similarity = calculateSimilarity(action, other)
                other to similarity
            }
            .filter { it.second > 0.3 } // Minimum 30% similarity
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }
    }

    /**
     * Calculate similarity between two actions
     */
    private fun calculateSimilarity(a: ActionMetadata, b: ActionMetadata): Double {
        var similarity = 0.0

        // Same category
        if (a.category == b.category) {
            similarity += 0.3
        }

        // Overlapping commands
        val commonCommands = a.supportedCommands.intersect(b.supportedCommands.toSet())
        if (commonCommands.isNotEmpty()) {
            similarity += 0.4 * (commonCommands.size.toDouble() / a.supportedCommands.size)
        }

        // Similar descriptions
        val commonWords = a.description.lowercase().split(" ")
            .intersect(b.description.lowercase().split(" ").toSet())
        if (commonWords.isNotEmpty()) {
            similarity += 0.3 * (commonWords.size.toDouble() / 10.0).coerceAtMost(1.0)
        }

        return similarity.coerceIn(0.0, 1.0)
    }
}
