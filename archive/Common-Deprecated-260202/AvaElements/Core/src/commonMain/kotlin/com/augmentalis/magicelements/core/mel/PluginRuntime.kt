package com.augmentalis.magicelements.core.mel

import com.augmentalis.avaelements.core.Component
import com.augmentalis.magicelements.core.mel.functions.PluginTier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Main runtime coordinator for MEL-based plugins.
 *
 * Integrates all MEL components:
 * - PluginState: Immutable state management
 * - ExpressionEvaluator: Expression parsing and evaluation
 * - ReducerEngine: State transitions
 * - ReactiveRenderer: UI rendering with bindings
 * - TierDetector: Platform-specific tier enforcement
 *
 * Lifecycle:
 * 1. Load: Parse and validate plugin definition
 * 2. Initialize: Create state, evaluator, reducer engine, renderer
 * 3. Run: Handle dispatches and render updates
 * 4. Destroy: Clean up resources
 *
 * Usage:
 * ```kotlin
 * val runtime = PluginRuntime(pluginDefinition)
 * val component = runtime.render()
 * runtime.dispatch("increment")
 * runtime.destroy()
 * ```
 */
class PluginRuntime(
    private val definition: PluginDefinition,
    private val platform: Platform = TierDetector.detectPlatform()
) {
    /**
     * Effective tier after platform-specific downgrade.
     */
    val effectiveTier: PluginTier

    /**
     * Plugin state container (immutable, copy-on-write).
     */
    private var _state: PluginState

    /**
     * Reactive state flow for UI observation.
     */
    private val _stateFlow: MutableStateFlow<PluginState>

    /**
     * Public read-only state flow.
     */
    val stateFlow: StateFlow<PluginState>

    /**
     * Current plugin state.
     */
    val state: PluginState
        get() = _state

    /**
     * Reducer execution engine.
     */
    private val reducerEngine: ReducerEngine

    /**
     * Reactive renderer for UI binding and updates.
     */
    private val renderer: ReactiveRenderer

    /**
     * Effect executor (Tier 2 only).
     */
    private val effectExecutor: EffectExecutor

    /**
     * Lifecycle state.
     */
    private var isDestroyed = false

    /**
     * Dispatch count for debugging.
     */
    private var dispatchCount = 0L

    init {
        // Determine effective tier
        effectiveTier = definition.getEffectiveTier(TierDetector.getMaxTier(platform))

        // Warn if plugin was downgraded
        if (definition.requiresDowngrade(TierDetector.getMaxTier(platform))) {
            logWarning(TierDetector.getDowngradeWarning(platform))
        }

        // Initialize state from schema
        val schema = definition.getStateSchema()
        _state = PluginState(schema)
        _stateFlow = MutableStateFlow(_state)
        stateFlow = _stateFlow.asStateFlow()

        // Initialize reducer engine with effective tier
        reducerEngine = ReducerEngine(definition.reducers, effectiveTier)

        // Initialize expression parser and binding resolver
        val parser = ExpressionParser(emptyList())
        val resolver = BindingResolver(_state, parser)

        // Initialize component factory
        val componentFactory = DefaultComponentFactory()

        // Initialize renderer
        renderer = ReactiveRenderer(
            uiRoot = definition.ui,
            state = _state,
            reducerEngine = reducerEngine,
            tier = effectiveTier,
            resolver = resolver,
            componentFactory = componentFactory,
            onDispatch = { action, params -> dispatch(action, params) }
        )

        // Initialize effect executor
        effectExecutor = EffectExecutor(effectiveTier)

        // Validate the plugin definition at startup
        validate()
    }

    /**
     * Validate the plugin definition and log any errors.
     */
    private fun validate() {
        val validator = PluginValidator()
        val errors = validator.validate(definition, platform)

        val criticalErrors = validator.getErrors(errors)
        val warnings = validator.getWarnings(errors)

        if (warnings.isNotEmpty()) {
            logWarning("Plugin validation warnings:")
            warnings.forEach { logWarning("  - $it") }
        }

        if (criticalErrors.isNotEmpty()) {
            val message = "Plugin validation failed:\n${criticalErrors.joinToString("\n") { "  - $it" }}"
            throw PluginRuntimeException(message)
        }
    }

    /**
     * Dispatch an action to a reducer.
     *
     * This is the main entry point for state updates triggered by UI events,
     * timers, or external actions.
     *
     * @param action Reducer name to invoke
     * @param params Parameters to pass to the reducer
     * @throws PluginRuntimeException if dispatch fails
     */
    fun dispatch(action: String, params: Map<String, Any> = emptyMap()) {
        checkNotDestroyed()

        try {
            dispatchCount++

            // Execute reducer and get state updates + effects
            val result = reducerEngine.dispatch(_state, action, params)

            // Apply state updates
            val jsonUpdates = result.stateUpdates.mapValues { (_, value) ->
                convertToJsonElement(value)
            }

            _state = _state.update(jsonUpdates)
            _stateFlow.value = _state

            // Execute effects (Tier 2 only)
            if (effectiveTier == PluginTier.LOGIC && result.effects.isNotEmpty()) {
                effectExecutor.executeEffects(result.effects)
            }

            // Notify renderer of state change
            renderer.onStateChanged(_state)

        } catch (e: Exception) {
            throw PluginRuntimeException(
                "Dispatch failed for action '$action': ${e.message}",
                e
            )
        }
    }

    /**
     * Render the plugin UI.
     *
     * @return Platform-specific Component
     */
    fun render(): Component {
        checkNotDestroyed()
        return renderer.render()
    }

    /**
     * Get current state snapshot.
     *
     * @return Immutable copy of current state
     */
    fun getState(): Map<String, kotlinx.serialization.json.JsonElement> {
        return _state.snapshot()
    }

    /**
     * Update state directly (use with caution - prefer dispatching reducers).
     *
     * @param updates State updates to apply
     */
    fun updateState(updates: Map<String, kotlinx.serialization.json.JsonElement>) {
        checkNotDestroyed()
        _state = _state.update(updates)
        _stateFlow.value = _state
        renderer.onStateChanged(_state)
    }

    /**
     * Reset state to defaults.
     */
    fun resetState() {
        checkNotDestroyed()
        _state = _state.reset()
        _stateFlow.value = _state
        renderer.onStateChanged(_state)
    }

    /**
     * Reset specific state variables to defaults.
     *
     * @param paths Paths to reset
     */
    fun resetState(paths: List<String>) {
        checkNotDestroyed()
        _state = _state.reset(paths)
        _stateFlow.value = _state
        renderer.onStateChanged(_state)
    }

    /**
     * Undo last state change.
     *
     * @return true if undo was successful
     */
    fun undo(): Boolean {
        checkNotDestroyed()
        if (_state.canUndo()) {
            _state = _state.undo()
            _stateFlow.value = _state
            renderer.onStateChanged(_state)
            return true
        }
        return false
    }

    /**
     * Check if undo is available.
     */
    fun canUndo(): Boolean {
        return _state.canUndo()
    }

    /**
     * Get plugin metadata.
     */
    fun getMetadata(): PluginMetadataJson {
        return definition.metadata
    }

    /**
     * Get the UI root node from the plugin definition.
     */
    fun getUIRoot(): UINode {
        return definition.ui
    }

    /**
     * Get plugin tier information.
     */
    fun getTierInfo(): TierInfo {
        return TierInfo(
            requested = definition.tier,
            effective = effectiveTier,
            platform = platform,
            downgraded = definition.tier != effectiveTier
        )
    }

    /**
     * Get a reducer definition by name.
     *
     * @param name Reducer name
     * @return Reducer definition or null if not found
     */
    fun getReducer(name: String): Reducer? {
        return definition.reducers[name]
    }

    /**
     * Get runtime statistics.
     */
    fun getStats(): RuntimeStats {
        return RuntimeStats(
            dispatchCount = dispatchCount,
            stateSize = _state.toMap().size,
            reducerCount = reducerEngine.getReducerNames().size,
            canUndo = _state.canUndo(),
            isDestroyed = isDestroyed
        )
    }

    /**
     * Destroy the runtime and clean up resources.
     *
     * After calling destroy(), this runtime cannot be used anymore.
     */
    fun destroy() {
        if (!isDestroyed) {
            isDestroyed = true
            renderer.destroy()
            logInfo("PluginRuntime destroyed for plugin: ${definition.metadata.id}")
        }
    }

    /**
     * Check if runtime has been destroyed.
     */
    private fun checkNotDestroyed() {
        if (isDestroyed) {
            throw PluginRuntimeException("PluginRuntime has been destroyed")
        }
    }

    /**
     * Convert Kotlin types to JsonElement for state updates.
     */
    private fun convertToJsonElement(value: Any?): kotlinx.serialization.json.JsonElement {
        return when (value) {
            null -> kotlinx.serialization.json.JsonNull
            is kotlinx.serialization.json.JsonElement -> value
            is Boolean -> kotlinx.serialization.json.JsonPrimitive(value)
            is Number -> kotlinx.serialization.json.JsonPrimitive(value)
            is String -> kotlinx.serialization.json.JsonPrimitive(value)
            is List<*> -> kotlinx.serialization.json.JsonArray(value.map { convertToJsonElement(it) })
            is Map<*, *> -> {
                @Suppress("UNCHECKED_CAST")
                val stringMap = value as? Map<String, Any?> ?: value.entries.associate {
                    it.key.toString() to it.value
                }
                kotlinx.serialization.json.JsonObject(stringMap.mapValues { (_, v) -> convertToJsonElement(v) })
            }
            else -> kotlinx.serialization.json.JsonPrimitive(value.toString())
        }
    }

    /**
     * Platform-agnostic logging (can be overridden by platform implementations).
     */
    private fun logInfo(message: String) {
        println("[PluginRuntime] INFO: $message")
    }

    private fun logWarning(message: String) {
        println("[PluginRuntime] WARN: $message")
    }

    companion object {
        /**
         * Create a runtime from a plugin definition.
         *
         * @param definition Plugin definition
         * @param platform Target platform (auto-detected if not specified)
         * @return Initialized runtime
         */
        fun create(definition: PluginDefinition, platform: Platform? = null): PluginRuntime {
            return PluginRuntime(definition, platform ?: TierDetector.detectPlatform())
        }

        /**
         * Create a runtime from YAML/JSON source.
         *
         * @param source Plugin source as YAML or JSON string
         * @param format Source format (YAML or JSON)
         * @param platform Target platform (auto-detected if not specified)
         * @return Initialized runtime
         * @throws PluginRuntimeException if parsing or initialization fails
         */
        fun fromSource(
            source: String,
            format: PluginSourceFormat,
            platform: Platform? = null
        ): PluginRuntime {
            val parser = PluginDefinitionParser()
            val definition = when (format) {
                PluginSourceFormat.JSON -> parser.parseJson(source).getOrThrow()
                PluginSourceFormat.YAML -> {
                    // For YAML, we need to convert to JSON first
                    // This requires the YAML parser integration
                    throw NotImplementedError("YAML parsing will be integrated with YamlPluginParser")
                }
            }
            return create(definition, platform)
        }
    }
}

/**
 * Plugin source format.
 */
enum class PluginSourceFormat {
    YAML,
    JSON
}

/**
 * Tier information for a plugin runtime.
 *
 * @property requested Tier requested by plugin definition
 * @property effective Actual tier being used (may be downgraded)
 * @property platform Current platform
 * @property downgraded Whether tier was downgraded from requested
 */
data class TierInfo(
    val requested: PluginTier,
    val effective: PluginTier,
    val platform: Platform,
    val downgraded: Boolean
) {
    override fun toString(): String {
        val status = if (downgraded) "DOWNGRADED" else "OK"
        return "TierInfo(requested=$requested, effective=$effective, platform=$platform, status=$status)"
    }
}

/**
 * Runtime statistics.
 *
 * @property dispatchCount Number of dispatches executed
 * @property stateSize Number of state variables
 * @property reducerCount Number of reducers
 * @property canUndo Whether undo is available
 * @property isDestroyed Whether runtime is destroyed
 */
data class RuntimeStats(
    val dispatchCount: Long,
    val stateSize: Int,
    val reducerCount: Int,
    val canUndo: Boolean,
    val isDestroyed: Boolean
) {
    override fun toString(): String {
        return """
            RuntimeStats:
              Dispatches: $dispatchCount
              State variables: $stateSize
              Reducers: $reducerCount
              Can undo: $canUndo
              Destroyed: $isDestroyed
        """.trimIndent()
    }
}


/**
 * Exception thrown by plugin runtime.
 */
class PluginRuntimeException(
    message: String,
    cause: Throwable? = null
) : Exception("PluginRuntime error: $message", cause)

/**
 * Exception thrown when a reducer is not found.
 */
class ReducerException(
    message: String,
    val reducerName: String,
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * Exception thrown when a required parameter is missing.
 */
class MissingParameterException(
    val paramName: String,
    val reducerName: String
) : Exception("Missing required parameter '$paramName' for reducer '$reducerName'")

/**
 * Exception thrown when expression evaluation fails.
 */
class ExpressionEvaluationException(
    message: String,
    val expression: String,
    val reducerName: String,
    cause: Throwable? = null
) : Exception("Expression evaluation failed in reducer '$reducerName': $message\nExpression: $expression", cause)
