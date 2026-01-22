/**
 * AccessibilityDataProvider.kt - Read-only data provider interface for plugins
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2026-01-22
 *
 * Provides plugins with read-only access to VoiceOSCore data including
 * UI elements, commands, screen context, and user preferences.
 */
package com.augmentalis.magiccode.plugins.universal.data

import com.augmentalis.magiccode.plugins.universal.contracts.voiceoscore.ScreenContext
import com.augmentalis.voiceoscore.QuantizedCommand
import com.augmentalis.voiceoscore.QuantizedElement
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

/**
 * Read-only data provider for plugin access to VoiceOSCore data.
 *
 * This interface defines the contract for plugins to query accessibility
 * data without the ability to modify it. All data access is through
 * suspend functions for proper coroutine integration.
 *
 * ## Design Goals
 * - **Read-Only**: Plugins cannot modify core accessibility data
 * - **Reactive**: Flow-based APIs for real-time updates
 * - **Efficient**: Caching layer for frequently accessed data
 * - **Thread-Safe**: All operations are safe for concurrent access
 *
 * ## Usage Example
 * ```kotlin
 * class MyPlugin(private val dataProvider: AccessibilityDataProvider) {
 *     suspend fun processScreen() {
 *         val elements = dataProvider.getCurrentScreenElements()
 *         val context = dataProvider.getScreenContext()
 *
 *         elements.forEach { element ->
 *             println("Element: ${element.label} [${element.avid}]")
 *         }
 *     }
 *
 *     fun observeScreenChanges() {
 *         dataProvider.screenContextFlow.collect { context ->
 *             context?.let { onScreenChanged(it) }
 *         }
 *     }
 * }
 * ```
 *
 * @since 1.0.0
 * @see AccessibilityDataProviderImpl
 * @see CachedAccessibilityData
 */
interface AccessibilityDataProvider {

    // =========================================================================
    // UI Element Data
    // =========================================================================

    /**
     * Get all accessible elements on the current screen.
     *
     * Returns the list of quantized elements that represent
     * the current UI state. Elements include buttons, text fields,
     * lists, and other accessible components.
     *
     * @return List of QuantizedElement on current screen
     */
    suspend fun getCurrentScreenElements(): List<QuantizedElement>

    /**
     * Get a specific element by its AVID (Accessibility Voice ID).
     *
     * @param avid The AVID of the element to retrieve
     * @return QuantizedElement or null if not found
     */
    suspend fun getElement(avid: String): QuantizedElement?

    // =========================================================================
    // Command Data
    // =========================================================================

    /**
     * Get available voice commands for the current screen.
     *
     * Returns commands that are applicable to the current
     * screen context, including learned and generated commands.
     *
     * @return List of QuantizedCommand for current screen
     */
    suspend fun getScreenCommands(): List<QuantizedCommand>

    /**
     * Get command execution history.
     *
     * Retrieves historical command executions for analysis
     * and learning purposes.
     *
     * @param limit Maximum number of entries to return (default 100)
     * @param successOnly If true, only return successful commands
     * @return List of CommandHistoryEntry ordered by most recent first
     */
    suspend fun getCommandHistory(
        limit: Int = 100,
        successOnly: Boolean = false
    ): List<CommandHistoryEntry>

    /**
     * Get top-ranked commands based on usage patterns.
     *
     * Returns commands ranked by frequency and recency,
     * optionally filtered by context.
     *
     * @param limit Maximum number of commands to return (default 20)
     * @param context Optional context filter (package name or screen ID)
     * @return List of RankedCommand ordered by rank
     */
    suspend fun getTopCommands(
        limit: Int = 20,
        context: String? = null
    ): List<RankedCommand>

    // =========================================================================
    // Screen Context
    // =========================================================================

    /**
     * Get the current screen context.
     *
     * Returns metadata about the current screen including
     * package name, activity, element counts, and classification.
     *
     * @return Current ScreenContext
     */
    suspend fun getScreenContext(): ScreenContext

    /**
     * Get the navigation graph for an application.
     *
     * Returns the learned navigation structure showing
     * how screens connect within an application.
     *
     * @param packageName Package name to get navigation for
     * @return NavigationGraph for the application
     */
    suspend fun getNavigationGraph(packageName: String): NavigationGraph

    // =========================================================================
    // User Preferences
    // =========================================================================

    /**
     * Get user context preferences.
     *
     * Returns user-defined preferences for specific contexts,
     * such as preferred actions or custom command mappings.
     *
     * @return List of ContextPreference
     */
    suspend fun getContextPreferences(): List<ContextPreference>

    // =========================================================================
    // Reactive Flows
    // =========================================================================

    /**
     * Observable flow of current screen elements.
     *
     * Emits the current list of elements and updates
     * whenever the screen content changes.
     */
    val screenElementsFlow: StateFlow<List<QuantizedElement>>

    /**
     * Observable flow of current screen context.
     *
     * Emits the current context and updates when the
     * user navigates to a different screen.
     */
    val screenContextFlow: StateFlow<ScreenContext?>
}

// =============================================================================
// Data Classes
// =============================================================================

/**
 * Historical command execution record.
 *
 * Captures details about a command that was executed,
 * including timing and success status.
 *
 * @property command The command that was executed
 * @property timestamp When the command was executed (epoch millis)
 * @property success Whether the command executed successfully
 * @property executionTimeMs How long the execution took in milliseconds
 * @property errorMessage Error message if execution failed
 * @since 1.0.0
 */
@Serializable
data class CommandHistoryEntry(
    val command: QuantizedCommand,
    val timestamp: Long,
    val success: Boolean,
    val executionTimeMs: Long,
    val errorMessage: String? = null
) {
    /**
     * Check if this execution was fast (under 100ms).
     */
    val isFast: Boolean get() = executionTimeMs < 100

    /**
     * Check if this execution was slow (over 1000ms).
     */
    val isSlow: Boolean get() = executionTimeMs > 1000
}

/**
 * Command ranked by usage patterns.
 *
 * Represents a command with its usage statistics
 * for prioritization and suggestion purposes.
 *
 * @property command The ranked command
 * @property usageCount Total number of times this command was used
 * @property lastUsed Timestamp of last usage (epoch millis)
 * @property contextScore Relevance score for current context (0.0-1.0)
 * @property successRate Ratio of successful executions (0.0-1.0)
 * @since 1.0.0
 */
@Serializable
data class RankedCommand(
    val command: QuantizedCommand,
    val usageCount: Int,
    val lastUsed: Long,
    val contextScore: Float,
    val successRate: Float = 1.0f
) {
    /**
     * Combined rank score based on usage and context.
     *
     * Higher values indicate more relevant commands.
     */
    val rankScore: Float get() = (usageCount * 0.3f) + (contextScore * 0.7f)

    /**
     * Check if this command was used recently (within last hour).
     */
    fun isRecent(currentTime: Long): Boolean {
        return (currentTime - lastUsed) < 3600_000
    }
}

/**
 * Navigation graph representing screen connections in an app.
 *
 * Models the navigation structure learned from user interactions,
 * showing how screens connect and which actions lead between them.
 *
 * @property nodes List of screens in the navigation graph
 * @property edges Connections between screens
 * @property packageName Package name this graph belongs to
 * @property lastUpdated When this graph was last modified
 * @since 1.0.0
 */
@Serializable
data class NavigationGraph(
    val nodes: List<NavigationNode>,
    val edges: List<NavigationEdge>,
    val packageName: String,
    val lastUpdated: Long = 0L
) {
    /**
     * Find a node by screen ID.
     *
     * @param screenId Screen ID to find
     * @return NavigationNode or null
     */
    fun findNode(screenId: String): NavigationNode? {
        return nodes.find { it.screenId == screenId }
    }

    /**
     * Get all edges from a specific screen.
     *
     * @param screenId Source screen ID
     * @return List of outgoing edges
     */
    fun getOutgoingEdges(screenId: String): List<NavigationEdge> {
        return edges.filter { it.from == screenId }
    }

    /**
     * Get all edges leading to a specific screen.
     *
     * @param screenId Target screen ID
     * @return List of incoming edges
     */
    fun getIncomingEdges(screenId: String): List<NavigationEdge> {
        return edges.filter { it.to == screenId }
    }

    /**
     * Check if navigation exists between two screens.
     *
     * @param from Source screen ID
     * @param to Target screen ID
     * @return true if direct navigation exists
     */
    fun hasPath(from: String, to: String): Boolean {
        return edges.any { it.from == from && it.to == to }
    }

    companion object {
        /**
         * Empty navigation graph.
         */
        fun empty(packageName: String): NavigationGraph {
            return NavigationGraph(
                nodes = emptyList(),
                edges = emptyList(),
                packageName = packageName
            )
        }
    }
}

/**
 * Node in the navigation graph representing a screen.
 *
 * @property screenId Unique identifier for this screen
 * @property title Human-readable screen title
 * @property packageName Package name containing this screen
 * @property activityName Optional activity name
 * @property visitCount Number of times this screen was visited
 * @since 1.0.0
 */
@Serializable
data class NavigationNode(
    val screenId: String,
    val title: String,
    val packageName: String,
    val activityName: String? = null,
    val visitCount: Int = 0
) {
    /**
     * Display name for this node.
     */
    val displayName: String get() = activityName ?: title
}

/**
 * Edge in the navigation graph representing a transition.
 *
 * Captures how users navigate between screens, including
 * the action that triggers the transition.
 *
 * @property from Source screen ID
 * @property to Target screen ID
 * @property action Action that triggers this transition (e.g., "click button X")
 * @property frequency How often this navigation path is used
 * @property avgTimeMs Average time spent on source screen before navigation
 * @since 1.0.0
 */
@Serializable
data class NavigationEdge(
    val from: String,
    val to: String,
    val action: String,
    val frequency: Int = 1,
    val avgTimeMs: Long = 0L
) {
    /**
     * Check if this is a frequently used path (more than 10 uses).
     */
    val isFrequent: Boolean get() = frequency > 10

    /**
     * Check if this is a quick navigation (under 2 seconds average).
     */
    val isQuick: Boolean get() = avgTimeMs < 2000
}

/**
 * User preference for a specific context.
 *
 * Stores user-defined preferences that apply to specific
 * applications, screens, or global contexts.
 *
 * @property key Preference key
 * @property value Preference value
 * @property packageName Package name this applies to (null for global)
 * @property screenId Screen ID this applies to (null for app-wide)
 * @property createdAt When this preference was created
 * @property updatedAt When this preference was last updated
 * @since 1.0.0
 */
@Serializable
data class ContextPreference(
    val key: String,
    val value: String,
    val packageName: String? = null,
    val screenId: String? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
) {
    /**
     * Check if this is a global preference.
     */
    val isGlobal: Boolean get() = packageName == null

    /**
     * Check if this is a screen-specific preference.
     */
    val isScreenSpecific: Boolean get() = screenId != null

    /**
     * Check if this preference applies to a given context.
     *
     * @param pkg Package name to check
     * @param screen Screen ID to check (optional)
     * @return true if this preference applies
     */
    fun appliesTo(pkg: String, screen: String? = null): Boolean {
        // Global preferences apply to everything
        if (isGlobal) return true
        // Check package match
        if (packageName != pkg) return false
        // If screen-specific, must match screen
        if (isScreenSpecific) return screenId == screen
        // Package-wide preference applies
        return true
    }

    companion object {
        /**
         * Create a global preference.
         *
         * @param key Preference key
         * @param value Preference value
         * @return Global ContextPreference
         */
        fun global(key: String, value: String): ContextPreference {
            val now = System.currentTimeMillis()
            return ContextPreference(
                key = key,
                value = value,
                createdAt = now,
                updatedAt = now
            )
        }

        /**
         * Create a package-specific preference.
         *
         * @param key Preference key
         * @param value Preference value
         * @param packageName Package this applies to
         * @return Package-scoped ContextPreference
         */
        fun forPackage(key: String, value: String, packageName: String): ContextPreference {
            val now = System.currentTimeMillis()
            return ContextPreference(
                key = key,
                value = value,
                packageName = packageName,
                createdAt = now,
                updatedAt = now
            )
        }
    }
}

// =============================================================================
// Extension Functions
// =============================================================================

/**
 * Find elements by label pattern.
 *
 * @param pattern Regex pattern to match against labels
 * @return List of matching elements
 */
suspend fun AccessibilityDataProvider.findElementsByLabel(pattern: Regex): List<QuantizedElement> {
    return getCurrentScreenElements().filter { pattern.matches(it.label) }
}

/**
 * Get the most frequently used command for current screen.
 *
 * @return Most used RankedCommand or null if none available
 */
suspend fun AccessibilityDataProvider.getMostUsedCommand(): RankedCommand? {
    return getTopCommands(limit = 1).firstOrNull()
}

/**
 * Check if screen has been learned.
 *
 * @return true if current screen has learned commands
 */
suspend fun AccessibilityDataProvider.isScreenLearned(): Boolean {
    return getScreenContext().isLearned
}

/**
 * Get command success rate for current screen.
 *
 * @return Success rate between 0.0 and 1.0
 */
suspend fun AccessibilityDataProvider.getScreenCommandSuccessRate(): Float {
    val history = getCommandHistory(limit = 50, successOnly = false)
    if (history.isEmpty()) return 1.0f
    val successCount = history.count { it.success }
    return successCount.toFloat() / history.size
}
