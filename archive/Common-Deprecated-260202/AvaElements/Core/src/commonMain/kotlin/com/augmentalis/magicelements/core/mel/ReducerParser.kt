package com.augmentalis.magicelements.core.mel

import kotlinx.serialization.json.*

/**
 * Parser for reducer definitions from YAML/JSON.
 *
 * Converts raw reducer definitions into typed Reducer objects.
 * Handles parameter lists, next_state expressions, and effects.
 *
 * Example input:
 * ```yaml
 * reducers:
 *   appendDigit:
 *     params: [digit]
 *     next_state:
 *       display: $string.concat($state.display, $digit)
 *     effects:
 *       - type: haptic
 *         config: { intensity: "light" }
 * ```
 */
class ReducerParser {
    /**
     * Parse a map of reducer definitions.
     *
     * @param reducersJson JSON object containing reducer definitions
     * @return Map of reducer names to Reducer objects
     * @throws ReducerParseException if parsing fails
     */
    fun parseReducers(reducersJson: JsonObject): Map<String, Reducer> {
        return reducersJson.mapValues { (name, value) ->
            try {
                parseReducer(name, value.jsonObject)
            } catch (e: Exception) {
                throw ReducerParseException(
                    "Failed to parse reducer '$name'",
                    name,
                    e
                )
            }
        }
    }

    /**
     * Parse a single reducer definition.
     *
     * @param name Name of the reducer
     * @param json JSON object representing the reducer
     * @return Parsed Reducer object
     */
    private fun parseReducer(name: String, json: JsonObject): Reducer {
        // Parse params list (optional)
        val params = json["params"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()

        // Parse next_state (required)
        val nextStateJson = json["next_state"]?.jsonObject
            ?: throw ReducerParseException("Missing 'next_state' in reducer", name)

        val nextState = parseNextState(nextStateJson, name)

        // Parse effects list (optional, Tier 2 only)
        val effects = json["effects"]?.jsonArray?.let { effectsArray ->
            parseEffects(effectsArray, name)
        }

        return Reducer(
            params = params,
            next_state = nextState,
            effects = effects
        )
    }

    /**
     * Parse next_state expressions.
     *
     * @param json JSON object mapping state paths to expressions
     * @param reducerName Name of the containing reducer (for error messages)
     * @return Map of state paths to raw expression strings
     */
    private fun parseNextState(json: JsonObject, reducerName: String): Map<String, String> {
        return json.mapValues { (path, value) ->
            when (value) {
                is JsonPrimitive -> {
                    // Simple value or expression string
                    when {
                        value.isString -> value.content
                        else -> value.toString() // Numbers, booleans become literals
                    }
                }
                is JsonObject, is JsonArray -> {
                    // Complex objects/arrays - serialize to JSON string for now
                    // These will be handled by the expression evaluator
                    value.toString()
                }
                else -> throw ReducerParseException(
                    "Invalid next_state value for '$path': $value",
                    reducerName
                )
            }
        }
    }

    /**
     * Parse effects list.
     *
     * @param array JSON array of effect definitions
     * @param reducerName Name of the containing reducer (for error messages)
     * @return List of Effect objects
     */
    private fun parseEffects(array: JsonArray, reducerName: String): List<Effect> {
        return array.mapIndexed { index, element ->
            try {
                parseEffect(element.jsonObject)
            } catch (e: Exception) {
                throw ReducerParseException(
                    "Failed to parse effect at index $index",
                    reducerName,
                    e
                )
            }
        }
    }

    /**
     * Parse a single effect definition.
     *
     * @param json JSON object representing the effect
     * @return Parsed Effect object
     */
    private fun parseEffect(json: JsonObject): Effect {
        val type = json["type"]?.jsonPrimitive?.content
            ?: throw IllegalArgumentException("Effect missing 'type' field")

        val config = json["config"]?.jsonObject?.toMap() ?: emptyMap()

        return Effect(
            type = type,
            config = config
        )
    }

    /**
     * Parse a reducer dispatch expression.
     *
     * Extracts the reducer name and parameter values from a dispatch call.
     *
     * Examples:
     * - "increment" -> ("increment", emptyMap)
     * - "appendDigit(7)" -> ("appendDigit", {"digit": 7})
     * - "setOperator('+')" -> ("setOperator", {"op": "+"})
     *
     * @param dispatchExpr The dispatch expression string
     * @return Pair of (reducer name, parameter map)
     */
    fun parseDispatchExpression(dispatchExpr: String): Pair<String, Map<String, Any>> {
        val trimmed = dispatchExpr.trim()

        // Check if there are parameters
        if (!trimmed.contains('(')) {
            return trimmed to emptyMap()
        }

        // Extract reducer name and parameter list
        val nameEndIndex = trimmed.indexOf('(')
        val name = trimmed.substring(0, nameEndIndex).trim()

        val paramsEndIndex = trimmed.lastIndexOf(')')
        if (paramsEndIndex == -1) {
            throw IllegalArgumentException("Malformed dispatch expression: missing closing parenthesis")
        }

        val paramsString = trimmed.substring(nameEndIndex + 1, paramsEndIndex).trim()

        // Parse parameter values
        val params = if (paramsString.isEmpty()) {
            emptyMap()
        } else {
            parseParameterList(paramsString)
        }

        return name to params
    }

    /**
     * Parse a comma-separated parameter list.
     *
     * Supports:
     * - Numbers: 7, 3.14, -5
     * - Strings: 'hello', "world"
     * - Booleans: true, false
     *
     * @param paramsString The parameter list string
     * @return Map of parameter values (positional, keyed by index)
     */
    private fun parseParameterList(paramsString: String): Map<String, Any> {
        val params = mutableMapOf<String, Any>()
        val parts = splitParameters(paramsString)

        parts.forEachIndexed { index, part ->
            val value = parseParameterValue(part.trim())
            // Store by position index for now
            // Will be matched to param names in ReducerEngine
            params["_$index"] = value
        }

        return params
    }

    /**
     * Split parameter list by commas, respecting quoted strings.
     *
     * Example: "7, 'hello, world', true" -> ["7", "'hello, world'", "true"]
     */
    private fun splitParameters(paramsString: String): List<String> {
        val parts = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false
        var quoteChar = '\u0000'

        for (char in paramsString) {
            when {
                (char == '"' || char == '\'') && !inQuotes -> {
                    inQuotes = true
                    quoteChar = char
                    current.append(char)
                }
                char == quoteChar && inQuotes -> {
                    inQuotes = false
                    current.append(char)
                }
                char == ',' && !inQuotes -> {
                    parts.add(current.toString())
                    current = StringBuilder()
                }
                else -> {
                    current.append(char)
                }
            }
        }

        if (current.isNotEmpty()) {
            parts.add(current.toString())
        }

        return parts
    }

    /**
     * Parse a single parameter value.
     *
     * @param value String representation of the value
     * @return Parsed value (String, Number, Boolean)
     */
    private fun parseParameterValue(value: String): Any {
        return when {
            // String (quoted)
            value.startsWith('"') && value.endsWith('"') -> {
                value.substring(1, value.length - 1)
            }
            value.startsWith('\'') && value.endsWith('\'') -> {
                value.substring(1, value.length - 1)
            }
            // Boolean
            value == "true" -> true
            value == "false" -> false
            // Number
            value.contains('.') -> value.toDoubleOrNull() ?: value
            else -> value.toLongOrNull() ?: value
        }
    }

    /**
     * Validate a reducer definition.
     *
     * Checks for common issues:
     * - Missing next_state
     * - Invalid parameter names
     * - Invalid effect types
     *
     * @param name Name of the reducer
     * @param reducer Reducer to validate
     * @return List of validation errors (empty if valid)
     */
    fun validateReducer(name: String, reducer: Reducer): List<String> {
        val errors = mutableListOf<String>()

        // Check next_state is not empty
        if (reducer.next_state.isEmpty()) {
            errors.add("Reducer '$name' has empty next_state")
        }

        // Check parameter names are valid identifiers
        reducer.params.forEach { param ->
            if (!isValidIdentifier(param)) {
                errors.add("Reducer '$name' has invalid parameter name: '$param'")
            }
        }

        // Check effect types are recognized
        reducer.effects?.forEachIndexed { index, effect ->
            if (effect.type.isBlank()) {
                errors.add("Reducer '$name' has effect at index $index with blank type")
            }
        }

        return errors
    }

    /**
     * Check if a string is a valid identifier.
     */
    private fun isValidIdentifier(name: String): Boolean {
        if (name.isEmpty()) return false
        if (!name[0].isLetter() && name[0] != '_') return false
        return name.all { it.isLetterOrDigit() || it == '_' }
    }
}

/**
 * Exception thrown when reducer parsing fails.
 */
class ReducerParseException(
    message: String,
    val reducerName: String,
    cause: Throwable? = null
) : Exception("Failed to parse reducer '$reducerName': $message", cause)
