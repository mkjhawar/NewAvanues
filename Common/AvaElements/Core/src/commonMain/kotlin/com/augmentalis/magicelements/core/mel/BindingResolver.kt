package com.augmentalis.magicelements.core.mel

import com.augmentalis.magicelements.core.mel.ExpressionLexer
import kotlinx.serialization.json.*

/**
 * Resolves MEL binding expressions in UI props against plugin state
 *
 * Takes UINode definitions with binding expressions like `$state.display` and
 * resolves them to actual values from the current plugin state. Supports:
 * - Simple state references: `$state.count`
 * - Nested paths: `$state.user.name`
 * - Expression evaluation (via ExpressionEvaluator integration)
 *
 * ## Usage Example
 *
 * ```kotlin
 * val schema = StateSchema.fromDefaults(mapOf(
 *     "display" to JsonPrimitive("42"),
 *     "count" to JsonPrimitive(7)
 * ))
 * val state = PluginState(schema)
 * val stateMap = state.toMap().mapValues { it.value.toAny() }
 * val evaluator = ExpressionEvaluator(stateMap, emptyMap(), PluginTier.DATA)
 * val resolver = BindingResolver(state, evaluator)
 *
 * val node = UINode(
 *     type = "Text",
 *     props = mapOf("fontSize" to JsonPrimitive(48)),
 *     bindings = mapOf("value" to "$state.display")
 * )
 *
 * val resolved = resolver.resolve(node)
 * // resolved.props["value"] == JsonPrimitive("42")
 * ```
 *
 * @property state Current plugin state
 * @property parser Expression parser for parsing MEL expressions
 *
 * @since 2.0.0
 */
class BindingResolver(
    private val state: PluginState,
    private val parser: ExpressionParser
) {
    /**
     * Resolve all bindings in a UINode tree
     *
     * Returns a new map of props with bindings resolved to their current values.
     * Static props are merged with resolved bindings (bindings take precedence).
     *
     * @param node UI node with potential bindings
     * @return Map of all props with bindings resolved
     */
    fun resolve(node: UINode): Map<String, JsonElement> {
        val resolvedProps = node.props.toMutableMap()

        // Resolve each binding
        node.bindings.forEach { (propName, expressionStr) ->
            try {
                val value = resolveExpression(expressionStr)
                resolvedProps[propName] = toJsonElement(value)
            } catch (e: Exception) {
                // Log error but don't crash - use null or keep original
                println("Warning: Failed to resolve binding '$propName': ${e.message}")
                resolvedProps[propName] = JsonNull
            }
        }

        return resolvedProps
    }

    /**
     * Resolve a single binding expression
     *
     * Handles:
     * - Simple state refs: `$state.display` -> state value
     * - Literals: `"hello"`, `42`, `true`
     * - Expressions: `$state.count + 1` -> evaluated result
     *
     * @param expression MEL binding expression
     * @return Resolved value
     */
    fun resolveExpression(expression: String): Any? {
        val trimmed = expression.trim()

        // Quick path for simple state references
        if (trimmed.startsWith("$state.") && !trimmed.contains(" ") && !trimmed.contains("+")) {
            val path = trimmed.removePrefix("$state.")
            return state.get(path).toAny()
        }

        // Parse and evaluate the full expression
        // First tokenize the expression, then parse
        val lexer = ExpressionLexer(expression)
        val tokens = lexer.tokenize()
        val ast = parser.parse(tokens)
        val stateMap = state.toMap().mapValues { it.value.toAny() }
        val evaluator = ExpressionEvaluator(stateMap, emptyMap(), PluginTier.DATA)
        return evaluator.evaluate(ast)
    }

    /**
     * Resolve bindings for all children recursively
     *
     * @param nodes List of UI nodes
     * @return List of nodes with resolved props (maintains tree structure)
     */
    fun resolveChildren(nodes: List<UINode>?): List<UINode>? {
        return nodes?.map { node ->
            node.copy(
                props = resolve(node),
                children = resolveChildren(node.children)
            )
        }
    }

    /**
     * Check if an expression contains any bindings
     */
    fun hasBindings(expression: String): Boolean {
        return expression.contains("$state.") ||
               expression.contains("$math.") ||
               expression.contains("$string.") ||
               expression.contains("$array.") ||
               expression.contains("$logic.")
    }

    /**
     * Extract all state paths referenced in an expression
     *
     * Examples:
     * - "$state.display" -> ["display"]
     * - "$state.user.name" -> ["user.name"]
     * - "$state.a + $state.b" -> ["a", "b"]
     */
    fun extractStatePaths(expression: String): Set<String> {
        val paths = mutableSetOf<String>()
        val stateRefRegex = Regex("""\$state\.([a-zA-Z_][a-zA-Z0-9_.]*)""")

        stateRefRegex.findAll(expression).forEach { match ->
            paths.add(match.groupValues[1])
        }

        return paths
    }

    /**
     * Convert a resolved value to JsonElement
     */
    private fun toJsonElement(value: Any): JsonElement {
        return when (value) {
            is JsonElement -> value
            is String -> JsonPrimitive(value)
            is Number -> JsonPrimitive(value)
            is Boolean -> JsonPrimitive(value)
            is List<*> -> JsonArray(value.map { toJsonElement(it ?: JsonNull) })
            is Map<*, *> -> JsonObject(
                value.entries.associate { (k, v) ->
                    k.toString() to toJsonElement(v ?: JsonNull)
                }
            )
            null -> JsonNull
            else -> JsonPrimitive(value.toString())
        }
    }

    companion object {
        /**
         * Check if a prop value is a binding expression
         */
        fun isBinding(value: String): Boolean {
            return value.contains("$state.") ||
                   value.contains("$math.") ||
                   value.contains("$string.") ||
                   value.contains("$array.") ||
                   value.contains("$logic.") ||
                   value.contains("$object.") ||
                   value.contains("$date.")
        }
    }
}

/**
 * Helper extension to convert JsonElement to Any
 */
private fun JsonElement.toAny(): Any? {
    return when (this) {
        is JsonPrimitive -> {
            when {
                isString -> content
                content == "true" -> true
                content == "false" -> false
                content.toIntOrNull() != null -> content.toInt()
                content.toDoubleOrNull() != null -> content.toDouble()
                else -> content
            }
        }
        is JsonObject -> this.toMap().mapValues { it.value.toAny() }
        is JsonArray -> this.map { it.toAny() }
        JsonNull -> null
    }
}
