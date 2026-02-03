package com.augmentalis.magicelements.core.mel

/**
 * Event binder that wires UI events to reducer dispatch calls
 *
 * Parses event handler syntax from UINode definitions and creates actual
 * event handler functions that dispatch actions to the reducer engine.
 *
 * ## Supported Event Syntax
 *
 * ### Simple reducer call (no params)
 * ```yaml
 * events:
 *   onTap: "clear"
 * ```
 *
 * ### Reducer with single param
 * ```yaml
 * events:
 *   onTap: "appendDigit('7')"
 *   onChange: "updateValue(newValue)"
 * ```
 *
 * ### Reducer with multiple params
 * ```yaml
 * events:
 *   onTap: "setRange(0, 100)"
 *   onSubmit: "submitForm(name, email, password)"
 * ```
 *
 * ## Supported Events
 * - `onTap` / `onClick`: Click/tap events
 * - `onChange`: Value change events
 * - `onSubmit`: Form submission
 * - `onLongPress`: Long press gesture
 * - `onDoubleTap`: Double tap gesture
 * - `onSwipe`: Swipe gesture
 * - `onFocus`: Focus gained
 * - `onBlur`: Focus lost
 * - `onHover`: Hover state change
 *
 * @property reducerEngine Reducer engine to dispatch actions to
 *
 * @since 2.0.0
 */
class EventBinder(
    private val reducerEngine: ReducerEngine
) {
    /**
     * Bind all events in a UINode tree
     *
     * Returns a map of event names to handler functions that can be
     * attached to AvaElements components.
     *
     * @param node UI node with potential events
     * @return Map of event name -> handler function
     */
    fun bind(node: UINode): Map<String, () -> Unit> {
        val handlers = mutableMapOf<String, () -> Unit>()

        node.events.forEach { (eventName, actionSpec) ->
            handlers[eventName] = createHandler(actionSpec)
        }

        return handlers
    }

    /**
     * Create an event handler function from an action specification
     *
     * Parses the action spec and returns a function that dispatches
     * the appropriate reducer call.
     *
     * Examples:
     * - "clear" -> { reducerEngine.dispatch("clear") }
     * - "appendDigit('7')" -> { reducerEngine.dispatch("appendDigit", mapOf("digit" to "7")) }
     * - "setRange(0, 100)" -> { reducerEngine.dispatch("setRange", mapOf("0" to 0, "1" to 100)) }
     *
     * @param actionSpec Action specification string
     * @return Event handler function
     */
    fun createHandler(actionSpec: String): () -> Unit {
        val parsed = parseActionSpec(actionSpec)

        return {
            try {
                reducerEngine.dispatch(parsed.reducerName, parsed.params)
            } catch (e: Exception) {
                println("Error executing action '$actionSpec': ${e.message}")
                throw e
            }
        }
    }

    /**
     * Parse an action specification into reducer name and parameters
     *
     * @param spec Action spec string
     * @return Parsed action
     */
    private fun parseActionSpec(spec: String): ParsedAction {
        val trimmed = spec.trim()

        // Check if it has parameters (contains parentheses)
        val openParenIndex = trimmed.indexOf('(')

        if (openParenIndex == -1) {
            // Simple reducer name with no params
            return ParsedAction(
                reducerName = trimmed,
                params = emptyMap()
            )
        }

        // Extract reducer name
        val reducerName = trimmed.substring(0, openParenIndex)

        // Extract parameters
        val closeParenIndex = trimmed.lastIndexOf(')')
        if (closeParenIndex == -1) {
            throw IllegalArgumentException("Malformed action spec: $spec (missing closing parenthesis)")
        }

        val paramsString = trimmed.substring(openParenIndex + 1, closeParenIndex)
        val params = parseParams(paramsString)

        return ParsedAction(
            reducerName = reducerName,
            params = params
        )
    }

    /**
     * Parse parameter list from string
     *
     * Handles:
     * - String literals: 'hello', "world"
     * - Numbers: 42, 3.14
     * - Booleans: true, false
     * - Variable names: newValue, count
     *
     * @param paramsString Parameter string (contents between parentheses)
     * @return Map of parameter names/indices to values
     */
    private fun parseParams(paramsString: String): Map<String, Any> {
        if (paramsString.isBlank()) {
            return emptyMap()
        }

        val params = mutableMapOf<String, Any>()
        val tokens = tokenizeParams(paramsString)

        tokens.forEachIndexed { index, token ->
            val value = parseParamValue(token)
            params[index.toString()] = value
        }

        return params
    }

    /**
     * Tokenize parameter string, respecting string literals
     *
     * Examples:
     * - "7" -> ["7"]
     * - "'hello', 'world'" -> ["'hello'", "'world'"]
     * - "42, true, 'test'" -> ["42", "true", "'test'"]
     */
    private fun tokenizeParams(paramsString: String): List<String> {
        val tokens = mutableListOf<String>()
        val currentToken = StringBuilder()
        var inString = false
        var stringDelimiter = ' '

        for (char in paramsString) {
            when {
                char == '\'' || char == '"' -> {
                    if (!inString) {
                        inString = true
                        stringDelimiter = char
                        currentToken.append(char)
                    } else if (char == stringDelimiter) {
                        inString = false
                        currentToken.append(char)
                    } else {
                        currentToken.append(char)
                    }
                }

                char == ',' && !inString -> {
                    if (currentToken.isNotEmpty()) {
                        tokens.add(currentToken.toString().trim())
                        currentToken.clear()
                    }
                }

                else -> {
                    currentToken.append(char)
                }
            }
        }

        if (currentToken.isNotEmpty()) {
            tokens.add(currentToken.toString().trim())
        }

        return tokens
    }

    /**
     * Parse a single parameter value
     *
     * @param token Parameter token string
     * @return Parsed value
     */
    private fun parseParamValue(token: String): Any {
        return when {
            // String literal with quotes
            (token.startsWith("'") && token.endsWith("'")) ||
            (token.startsWith("\"") && token.endsWith("\"")) -> {
                token.substring(1, token.length - 1)
            }

            // Boolean
            token == "true" -> true
            token == "false" -> false

            // Number (int)
            token.toIntOrNull() != null -> token.toInt()

            // Number (double)
            token.toDoubleOrNull() != null -> token.toDouble()

            // Variable name (return as-is, will be resolved later)
            else -> token
        }
    }

    /**
     * Bind events for parametric handlers (with event data)
     *
     * For events that pass data (like onChange, onSubmit), create handlers
     * that accept the event data and pass it to the reducer.
     *
     * @param node UI node with events
     * @return Map of event name -> parametric handler
     */
    fun bindParametric(node: UINode): Map<String, (Any?) -> Unit> {
        val handlers = mutableMapOf<String, (Any?) -> Unit>()

        node.events.forEach { (eventName, actionSpec) ->
            handlers[eventName] = createParametricHandler(actionSpec)
        }

        return handlers
    }

    /**
     * Create a parametric event handler
     *
     * @param actionSpec Action specification string
     * @return Parametric handler function
     */
    private fun createParametricHandler(actionSpec: String): (Any?) -> Unit {
        val parsed = parseActionSpec(actionSpec)

        return { eventData ->
            try {
                // Merge event data into params
                val finalParams = parsed.params.toMutableMap()
                if (eventData != null) {
                    finalParams["eventData"] = eventData
                }

                reducerEngine.dispatch(parsed.reducerName, finalParams)
            } catch (e: Exception) {
                println("Error executing parametric action '$actionSpec': ${e.message}")
                throw e
            }
        }
    }

    /**
     * Get all supported event types
     */
    companion object {
        val SUPPORTED_EVENTS = setOf(
            "onTap",
            "onClick",
            "onChange",
            "onSubmit",
            "onLongPress",
            "onDoubleTap",
            "onSwipe",
            "onFocus",
            "onBlur",
            "onHover",
            "onDragStart",
            "onDragEnd",
            "onDrop"
        )

        /**
         * Check if an event name is supported
         */
        fun isSupported(eventName: String): Boolean {
            return eventName in SUPPORTED_EVENTS
        }
    }
}

/**
 * Parsed action specification
 */
private data class ParsedAction(
    val reducerName: String,
    val params: Map<String, Any>
)

