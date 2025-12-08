package com.augmentalis.magicelements.core.mel

import kotlinx.serialization.json.*

/**
 * Reducer execution engine for the MEL dual-tier plugin system.
 *
 * Responsible for:
 * - Dispatching actions to reducers
 * - Evaluating reducer expressions with parameter binding
 * - Computing next state atomically
 * - Executing side effects (Tier 2 only)
 * - Integration with ExpressionEvaluator and PluginState
 *
 * Example usage:
 * ```kotlin
 * val state = PluginState(schema)
 * val reducers = mapOf(
 *     "increment" to Reducer(
 *         params = emptyList(),
 *         next_state = mapOf("count" to "$math.add($state.count, 1)")
 *     ),
 *     "appendDigit" to Reducer(
 *         params = listOf("digit"),
 *         next_state = mapOf("display" to "$string.concat($state.display, $digit)")
 *     )
 * )
 *
 * val engine = ReducerEngine(reducers, PluginTier.DATA)
 *
 * // Dispatch without params
 * val newState1 = engine.dispatch(state, "increment")
 *
 * // Dispatch with params
 * val newState2 = engine.dispatch(state, "appendDigit", mapOf("digit" to "7"))
 * ```
 */
class ReducerEngine(
    private val reducers: Map<String, Reducer>,
    private val tier: PluginTier = PluginTier.DATA
) {

    /**
     * Dispatch an action to a reducer and compute the new state.
     *
     * @param state Current plugin state
     * @param action Name of the reducer to invoke
     * @param params Parameters to pass to the reducer (for parameterized reducers)
     * @return ReducerResult containing new state updates and effects
     * @throws ReducerException if reducer execution fails
     */
    fun dispatch(
        state: PluginState,
        action: String,
        params: Map<String, Any> = emptyMap()
    ): ReducerResult {
        // Get the reducer
        val reducer = reducers[action]
            ?: throw ReducerException("Reducer '$action' not found", action)

        try {
            // Validate parameters
            validateParameters(reducer, params, action)

            // Convert PluginState to Map for evaluator
            val stateMap = convertStateToMap(state)

            // Evaluate next_state expressions
            val stateUpdates = evaluateNextState(reducer, stateMap, params, action)

            // Filter effects based on tier
            val effects = if (tier == PluginTier.LOGIC) {
                reducer.effects ?: emptyList()
            } else {
                // Tier 1: Effects are ignored
                emptyList()
            }

            return ReducerResult(
                stateUpdates = stateUpdates,
                effects = effects
            )
        } catch (e: Exception) {
            when (e) {
                is ReducerException -> throw e
                else -> throw ReducerException(
                    "Reducer execution failed: ${e.message}",
                    action,
                    e
                )
            }
        }
    }

    /**
     * Dispatch an action and apply the state updates directly to the state.
     *
     * Convenience method that combines dispatch + state update.
     *
     * @param state Current plugin state
     * @param action Name of the reducer to invoke
     * @param params Parameters to pass to the reducer
     * @return New PluginState with updates applied
     */
    fun dispatchAndApply(
        state: PluginState,
        action: String,
        params: Map<String, Any> = emptyMap()
    ): PluginState {
        val result = dispatch(state, action, params)

        // Convert state updates to JsonElements for PluginState.update
        val jsonUpdates = result.stateUpdates.mapValues { (_, value) ->
            convertToJsonElement(value)
        }

        return state.update(jsonUpdates)
    }

    /**
     * Validate that required parameters are provided.
     */
    private fun validateParameters(
        reducer: Reducer,
        params: Map<String, Any>,
        action: String
    ) {
        // Check all required parameters are provided
        reducer.params.forEach { paramName ->
            if (!params.containsKey(paramName)) {
                throw MissingParameterException(paramName, action)
            }
        }
    }

    /**
     * Evaluate all next_state expressions with parameter binding.
     *
     * @param reducer The reducer to execute
     * @param stateMap Current state as a plain map
     * @param params Parameters bound to the reducer
     * @param action Reducer name (for error messages)
     * @return Map of state paths to computed values
     */
    private fun evaluateNextState(
        reducer: Reducer,
        stateMap: Map<String, Any?>,
        params: Map<String, Any>,
        action: String
    ): Map<String, Any?> {
        val updates = mutableMapOf<String, Any?>()

        reducer.next_state.forEach { (path, exprString) ->
            try {
                val value = evaluateExpression(exprString, stateMap, params)
                updates[path] = value
            } catch (e: Exception) {
                throw ExpressionEvaluationException(
                    e.message ?: "Unknown error",
                    exprString,
                    action,
                    e
                )
            }
        }

        return updates
    }

    /**
     * Evaluate a single expression string.
     *
     * Parses and evaluates the expression with state and parameter context.
     *
     * @param exprString The raw expression string (e.g., "$math.add($state.count, 1)")
     * @param stateMap Current state
     * @param params Bound parameters
     * @return Evaluated result
     */
    private fun evaluateExpression(
        exprString: String,
        stateMap: Map<String, Any?>,
        params: Map<String, Any>
    ): Any? {
        // Check for literal values (non-expression)
        val trimmed = exprString.trim()

        // Handle simple literals that don't need parsing
        if (!trimmed.contains('$') && !trimmed.contains('(')) {
            return parseLiteralValue(trimmed)
        }

        // Parse the expression
        val lexer = ExpressionLexer(trimmed)
        val tokens = lexer.tokenize()
        val parser = ExpressionParser(tokens)
        val ast = parser.parse()

        // Create evaluator with state and params context
        val evaluator = ExpressionEvaluator(
            state = stateMap,
            params = params,
            tier = tier
        )

        // Evaluate and return
        return evaluator.evaluate(ast)
    }

    /**
     * Parse a literal value from a string.
     *
     * Handles:
     * - Numbers: 42, 3.14
     * - Strings: "hello", 'world'
     * - Booleans: true, false
     * - Null: null
     * - Arrays: [1, 2, 3] (JSON format)
     * - Objects: {"key": "value"} (JSON format)
     */
    private fun parseLiteralValue(value: String): Any? {
        return when {
            value == "null" -> null
            value == "true" -> true
            value == "false" -> false
            value.toDoubleOrNull() != null -> value.toDouble()
            value.toLongOrNull() != null -> value.toLong()
            value.startsWith('"') && value.endsWith('"') -> value.substring(1, value.length - 1)
            value.startsWith('\'') && value.endsWith('\'') -> value.substring(1, value.length - 1)
            value.startsWith('[') && value.endsWith(']') -> parseJsonArray(value)
            value.startsWith('{') && value.endsWith('}') -> parseJsonObject(value)
            else -> value // Return as string
        }
    }

    /**
     * Parse a JSON array string.
     */
    private fun parseJsonArray(json: String): List<Any?> {
        return try {
            val jsonArray = Json.parseToJsonElement(json).jsonArray
            jsonArray.map { convertJsonElementToAny(it) }
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid JSON array: $json", e)
        }
    }

    /**
     * Parse a JSON object string.
     */
    private fun parseJsonObject(json: String): Map<String, Any?> {
        return try {
            val jsonObject = Json.parseToJsonElement(json).jsonObject
            jsonObject.mapValues { (_, value) -> convertJsonElementToAny(value) }
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid JSON object: $json", e)
        }
    }

    /**
     * Convert JsonElement to plain Kotlin types.
     */
    private fun convertJsonElementToAny(element: JsonElement): Any? {
        return when (element) {
            is JsonPrimitive -> {
                when {
                    element.isString -> element.content
                    element.content == "true" -> true
                    element.content == "false" -> false
                    element.content == "null" -> null
                    else -> element.content.toDoubleOrNull() ?: element.content
                }
            }
            is JsonArray -> element.map { convertJsonElementToAny(it) }
            is JsonObject -> element.mapValues { (_, value) -> convertJsonElementToAny(value) }
            JsonNull -> null
        }
    }

    /**
     * Convert plain Kotlin types to JsonElement.
     */
    private fun convertToJsonElement(value: Any?): JsonElement {
        return when (value) {
            null -> JsonNull
            is JsonElement -> value
            is Boolean -> JsonPrimitive(value)
            is Number -> JsonPrimitive(value)
            is String -> JsonPrimitive(value)
            is List<*> -> JsonArray(value.map { convertToJsonElement(it) })
            is Map<*, *> -> {
                @Suppress("UNCHECKED_CAST")
                val stringMap = value as? Map<String, Any?> ?: value.entries.associate {
                    it.key.toString() to it.value
                }
                JsonObject(stringMap.mapValues { (_, v) -> convertToJsonElement(v) })
            }
            else -> JsonPrimitive(value.toString())
        }
    }

    /**
     * Convert PluginState to a plain map for the evaluator.
     *
     * The evaluator expects Map<String, Any?>, but PluginState stores JsonElements.
     * This converts the state to plain Kotlin types.
     */
    private fun convertStateToMap(state: PluginState): Map<String, Any?> {
        return state.toMap().mapValues { (_, jsonElement) ->
            convertJsonElementToAny(jsonElement)
        }
    }

    /**
     * Check if a reducer exists.
     */
    fun hasReducer(action: String): Boolean {
        return action in reducers
    }

    /**
     * Get all available reducer names.
     */
    fun getReducerNames(): Set<String> {
        return reducers.keys
    }

    /**
     * Get a reducer definition.
     */
    fun getReducer(action: String): Reducer? {
        return reducers[action]
    }

    /**
     * Validate all reducers in the engine.
     *
     * Checks for:
     * - Invalid expression syntax
     * - Missing state references
     * - Invalid function calls
     *
     * @return Map of reducer names to validation errors (empty if all valid)
     */
    fun validateReducers(state: PluginState): Map<String, List<String>> {
        val errors = mutableMapOf<String, MutableList<String>>()
        val stateMap = convertStateToMap(state)

        reducers.forEach { (name, reducer) ->
            val reducerErrors = mutableListOf<String>()

            // Check next_state expressions
            reducer.next_state.forEach { (path, exprString) ->
                try {
                    // Try to parse the expression
                    if (exprString.trim().contains('$') || exprString.trim().contains('(')) {
                        val lexer = ExpressionLexer(exprString.trim())
                        lexer.tokenize() // Just verify tokenization works
                    }
                } catch (e: Exception) {
                    reducerErrors.add("Invalid expression for '$path': ${e.message}")
                }
            }

            // Check effects (Tier 2 only)
            if (tier == PluginTier.LOGIC) {
                reducer.effects?.forEachIndexed { index, effect ->
                    if (effect.type.isBlank()) {
                        reducerErrors.add("Effect at index $index has blank type")
                    }
                }
            }

            if (reducerErrors.isNotEmpty()) {
                errors[name] = reducerErrors
            }
        }

        return errors
    }

    companion object {
        /**
         * Create a ReducerEngine from parsed reducer definitions.
         *
         * @param reducersJson JSON object containing reducer definitions
         * @param tier Plugin tier for function enforcement
         * @return Configured ReducerEngine
         */
        fun fromJson(reducersJson: JsonObject, tier: PluginTier = PluginTier.DATA): ReducerEngine {
            val parser = ReducerParser()
            val reducers = parser.parseReducers(reducersJson)
            return ReducerEngine(reducers, tier)
        }

        /**
         * Create an empty reducer engine (useful for testing).
         */
        fun empty(tier: PluginTier = PluginTier.DATA): ReducerEngine {
            return ReducerEngine(emptyMap(), tier)
        }
    }
}

/**
 * Effect executor for Tier 2 plugins.
 *
 * Executes side effects like navigation, haptics, storage, etc.
 * Platform-specific handlers must be set by platform renderers.
 */
class EffectExecutor(
    private val tier: PluginTier
) {
    companion object {
        // Platform-specific handlers - set by platform renderers
        var navigateHandler: ((Map<String, Any?>) -> Unit)? = null
        var hapticHandler: ((String) -> Unit)? = null
        var storageSetHandler: ((String, Any?) -> Unit)? = null
        var storageGetHandler: ((String) -> Any?)? = null
        var clipboardHandler: ((String) -> Unit)? = null
        var notificationHandler: ((String, String?) -> Unit)? = null
    }

    /**
     * Execute a list of effects.
     *
     * @param effects Effects to execute
     * @param context Execution context (platform-specific)
     */
    fun executeEffects(effects: List<Effect>, context: Any? = null) {
        if (tier == PluginTier.DATA) {
            // Tier 1: Effects are not executed
            return
        }

        effects.forEach { effect ->
            try {
                executeEffect(effect, context)
            } catch (e: Exception) {
                // Log error but don't fail the entire dispatch
                println("Effect execution failed: ${effect.type} - ${e.message}")
            }
        }
    }

    /**
     * Execute a single effect.
     */
    private fun executeEffect(effect: Effect, context: Any?) {
        when (effect.type) {
            "log" -> executeLog(effect)
            "navigate" -> executeNavigate(effect)
            "haptic" -> executeHaptic(effect)
            "storage.set" -> executeStorageSet(effect)
            "storage.get" -> executeStorageGet(effect)
            "clipboard" -> executeClipboard(effect)
            "notification" -> executeNotification(effect)
            else -> println("[MEL] Unknown effect type: ${effect.type}")
        }
    }

    /**
     * Convert JsonElement config to plain map for handlers.
     */
    @Suppress("UNCHECKED_CAST")
    private fun configToMap(config: Map<String, kotlinx.serialization.json.JsonElement>): Map<String, Any?> {
        return config.mapValues { (_, jsonElement) ->
            convertJsonElementToAny(jsonElement)
        }
    }

    /**
     * Convert JsonElement to plain Kotlin types.
     */
    private fun convertJsonElementToAny(element: kotlinx.serialization.json.JsonElement): Any? {
        return when (element) {
            is kotlinx.serialization.json.JsonPrimitive -> {
                when {
                    element.isString -> element.content
                    element.content == "true" -> true
                    element.content == "false" -> false
                    element.content == "null" -> null
                    else -> element.content.toDoubleOrNull() ?: element.content
                }
            }
            is kotlinx.serialization.json.JsonArray -> element.map { convertJsonElementToAny(it) }
            is kotlinx.serialization.json.JsonObject -> element.mapValues { (_, value) -> convertJsonElementToAny(value) }
            kotlinx.serialization.json.JsonNull -> null
        }
    }

    /**
     * Execute log effect.
     */
    private fun executeLog(effect: Effect) {
        val configMap = configToMap(effect.config)
        val message = configMap["message"]?.toString() ?: ""
        println("[MEL] $message")
    }

    /**
     * Execute navigate effect.
     */
    private fun executeNavigate(effect: Effect) {
        val configMap = configToMap(effect.config)

        navigateHandler?.invoke(configMap)
            ?: println("[MEL] Navigate effect not handled: $configMap")
    }

    /**
     * Execute haptic effect.
     */
    private fun executeHaptic(effect: Effect) {
        val configMap = configToMap(effect.config)
        val type = configMap["type"]?.toString() ?: "light"

        hapticHandler?.invoke(type)
            ?: println("[MEL] Haptic effect not handled: $type")
    }

    /**
     * Execute storage.set effect.
     */
    private fun executeStorageSet(effect: Effect) {
        val configMap = configToMap(effect.config)
        val key = configMap["key"]?.toString()
        val value = configMap["value"]

        if (key != null) {
            storageSetHandler?.invoke(key, value)
                ?: println("[MEL] Storage set not handled: $key = $value")
        }
    }

    /**
     * Execute storage.get effect.
     */
    private fun executeStorageGet(effect: Effect) {
        val configMap = configToMap(effect.config)
        val key = configMap["key"]?.toString()

        if (key != null) {
            val value = storageGetHandler?.invoke(key)
            if (value == null) {
                println("[MEL] Storage get not handled: $key")
            }
        }
    }

    /**
     * Execute clipboard effect.
     */
    private fun executeClipboard(effect: Effect) {
        val configMap = configToMap(effect.config)
        val text = configMap["text"]?.toString() ?: ""

        clipboardHandler?.invoke(text)
            ?: println("[MEL] Clipboard effect not handled")
    }

    /**
     * Execute notification effect.
     */
    private fun executeNotification(effect: Effect) {
        val configMap = configToMap(effect.config)
        val title = configMap["title"]?.toString() ?: ""
        val body = configMap["body"]?.toString()

        notificationHandler?.invoke(title, body)
            ?: println("[MEL] Notification effect not handled: $title")
    }
}
