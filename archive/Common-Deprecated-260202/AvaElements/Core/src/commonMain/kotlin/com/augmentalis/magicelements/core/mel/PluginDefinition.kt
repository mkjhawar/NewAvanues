package com.augmentalis.magicelements.core.mel

import com.augmentalis.avaelements.core.PluginMetadata
import com.augmentalis.magicelements.core.mel.functions.PluginTier
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Complete plugin definition for MEL-based plugins.
 *
 * Represents a self-contained plugin with metadata, state, reducers, optional scripts,
 * and UI definition. Supports both Tier 1 (DATA) and Tier 2 (LOGIC) plugins.
 *
 * @property metadata Plugin identification and versioning
 * @property tier Required plugin tier (defaults to DATA for Apple compliance)
 * @property state State variable definitions
 * @property reducers Named state transition functions
 * @property scripts Optional scripts (Tier 2 only, ignored on Apple platforms)
 * @property ui Root UI component tree
 */
@Serializable
data class PluginDefinition(
    val metadata: PluginMetadataJson,
    val tier: PluginTier = PluginTier.DATA,
    val state: Map<String, StateVariable> = emptyMap(),
    val reducers: Map<String, Reducer> = emptyMap(),
    val scripts: Map<String, Script>? = null,  // Tier 2 only
    val ui: UINode
) {
    init {
        // Validate that scripts are only present in Tier 2 plugins
        if (tier == PluginTier.DATA && !scripts.isNullOrEmpty()) {
            throw IllegalArgumentException("Scripts are only allowed in Tier 2 (LOGIC) plugins")
        }
    }

    /**
     * Get the state schema for this plugin.
     */
    fun getStateSchema(): StateSchema {
        return StateSchema(state)
    }

    /**
     * Check if this plugin requires tier downgrade on the given platform.
     */
    fun requiresDowngrade(maxTier: PluginTier): Boolean {
        return tier == PluginTier.LOGIC && maxTier == PluginTier.DATA
    }

    /**
     * Get effective tier after platform downgrade.
     */
    fun getEffectiveTier(maxTier: PluginTier): PluginTier {
        return if (requiresDowngrade(maxTier)) maxTier else tier
    }
}

/**
 * Serializable version of PluginMetadata for use in plugin definitions.
 *
 * This is a simplified version that can be parsed from YAML/JSON.
 */
@Serializable
data class PluginMetadataJson(
    val id: String,
    val name: String,
    val version: String,
    val author: String? = null,
    val description: String? = null,
    val minSdkVersion: String = "1.0.0"
) {
    /**
     * Convert to full PluginMetadata.
     */
    fun toPluginMetadata(): PluginMetadata {
        return PluginMetadata(
            id = id,
            name = name,
            version = version,
            author = author,
            description = description,
            minSdkVersion = minSdkVersion,
            permissions = emptySet(),
            dependencies = emptyList()
        )
    }
}

/**
 * Reducer definition for declarative state transitions.
 *
 * Reducers are pure functions that compute the next state based on current state
 * and optional parameters. They are the primary mechanism for state updates in
 * both Tier 1 and Tier 2 plugins.
 *
 * @property params List of parameter names accepted by this reducer
 * @property nextState Map of state variable names to expressions computing their next values
 * @property effects Optional side effects (Tier 2 only, e.g., HTTP calls, storage)
 */
@Serializable
data class Reducer(
    val params: List<String> = emptyList(),
    val nextState: Map<String, Expression>,
    val effects: List<Effect>? = null  // Tier 2 only
) {
    /**
     * Check if this reducer has any Tier 2-only features.
     */
    fun isTier2Only(): Boolean {
        return !effects.isNullOrEmpty()
    }
}

/**
 * Expression wrapper with raw string and optional parsed AST.
 *
 * Expressions can be:
 * - Simple literals: `42`, `"hello"`, `true`
 * - State references: `$state.count`, `$state.user.name`
 * - Function calls: `$math.add($state.a, 1)`, `$string.concat("Hello", " World")`
 * - Binary operations: `$state.x + 1`, `$state.enabled && $state.count > 0`
 * - Parameter references: `$digit`, `$op` (in reducers with params)
 *
 * @property raw Raw expression string
 * @property parsed Parsed AST node (populated by parser)
 */
@Serializable
data class Expression(
    val raw: String,
    val parsed: ExpressionNode? = null
) {
    companion object {
        /**
         * Create expression from raw string.
         */
        fun of(raw: String): Expression = Expression(raw)

        /**
         * Create expression from parsed AST.
         */
        fun of(node: ExpressionNode): Expression = Expression("", node)
    }
}

/**
 * Side effect definition (Tier 2 only).
 *
 * Effects represent actions with side effects like HTTP requests, storage operations,
 * navigation, etc. They are executed after state updates and are NOT allowed in
 * Tier 1 (DATA) plugins.
 *
 * @property type Effect type (http, storage, nav, clipboard, haptics)
 * @property action Specific action within the type
 * @property params Parameters for the effect
 */
@Serializable
data class Effect(
    val type: String,
    val action: String,
    val params: Map<String, JsonElement> = emptyMap()
) {
    companion object {
        // Tier 2 effect types
        const val HTTP = "http"
        const val STORAGE = "storage"
        const val NAVIGATION = "nav"
        const val CLIPBOARD = "clipboard"
        const val HAPTICS = "haptics"
    }
}

/**
 * Script definition (Tier 2 only).
 *
 * Scripts allow imperative logic with loops, conditionals, and custom functions.
 * They are ONLY available in Tier 2 (LOGIC) plugins and will be ignored on Apple platforms.
 *
 * @property params Parameter names for the script
 * @property body Script body as MEL code
 */
@Serializable
data class Script(
    val params: List<String> = emptyList(),
    val body: String
) {
    /**
     * Parse the script body into executable statements.
     * TODO: Implement full script parsing in future phase.
     */
    fun parse(): List<ExpressionNode> {
        // For now, treat as single expression
        // Full script parsing with statements will be added later
        throw NotImplementedError("Script parsing not yet implemented")
    }
}

/**
 * UI node definition in the plugin tree.
 *
 * Represents a component in the UI hierarchy with type, properties, bindings,
 * events, and optional children.
 *
 * @property type Component type (Button, Text, Column, Row, etc.)
 * @property props Static properties
 * @property bindings Reactive bindings (expressions that re-evaluate on state change)
 * @property events Event handlers mapping to reducer names
 * @property children Child UI nodes (for container components)
 */
@Serializable
data class UINode(
    val type: String,
    val props: Map<String, JsonElement> = emptyMap(),
    val bindings: Map<String, Expression> = emptyMap(),
    val events: Map<String, EventHandler> = emptyMap(),
    val children: List<UINode>? = null
) {
    /**
     * Check if this node has any reactive bindings.
     */
    fun hasBindings(): Boolean = bindings.isNotEmpty()

    /**
     * Check if this node has any event handlers.
     */
    fun hasEvents(): Boolean = events.isNotEmpty()

    /**
     * Check if this node is a container (has children).
     */
    fun isContainer(): Boolean = !children.isNullOrEmpty()

    /**
     * Get all bindings in this node and its children recursively.
     */
    fun getAllBindings(): List<Pair<String, Expression>> {
        val result = mutableListOf<Pair<String, Expression>>()
        result.addAll(bindings.entries.map { it.key to it.value })
        children?.forEach { child ->
            result.addAll(child.getAllBindings())
        }
        return result
    }

    /**
     * Get all event handlers in this node and its children recursively.
     */
    fun getAllEvents(): List<Pair<String, EventHandler>> {
        val result = mutableListOf<Pair<String, EventHandler>>()
        result.addAll(events.entries.map { it.key to it.value })
        children?.forEach { child ->
            result.addAll(child.getAllEvents())
        }
        return result
    }
}

/**
 * Event handler definition.
 *
 * Maps UI events (onTap, onChange, etc.) to reducer invocations with optional parameters.
 *
 * @property reducer Name of reducer to invoke
 * @property params Parameters to pass to reducer (can be expressions)
 */
@Serializable
data class EventHandler(
    val reducer: String,
    val params: Map<String, Expression> = emptyMap()
) {
    companion object {
        /**
         * Create simple event handler with just reducer name.
         */
        fun simple(reducerName: String): EventHandler {
            return EventHandler(reducer = reducerName)
        }

        /**
         * Create event handler with literal parameters.
         */
        fun withParams(reducerName: String, vararg params: Pair<String, String>): EventHandler {
            return EventHandler(
                reducer = reducerName,
                params = params.associate { it.first to Expression.of(it.second) }
            )
        }
    }
}
