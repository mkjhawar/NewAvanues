package com.augmentalis.avamagic.events

import com.augmentalis.avamagic.dsl.VosLambda
import com.augmentalis.avamagic.dsl.VosStatement
import com.augmentalis.avamagic.dsl.VosValue

/**
 * Adapts DSL lambdas to Kotlin callbacks.
 *
 * CallbackAdapter is the bridge between declarative VoiceOS DSL lambdas and
 * imperative Kotlin callback functions. It converts parsed lambda AST nodes
 * into executable Kotlin functions that can be attached to component event
 * handlers.
 *
 * Key responsibilities:
 * - **Lambda execution**: Evaluate DSL statements in order
 * - **Parameter binding**: Map event parameters to lambda parameters
 * - **Variable scoping**: Maintain execution context with proper scope chains
 * - **Function dispatch**: Route function calls to appropriate target APIs
 * - **Error handling**: Provide clear error messages for runtime issues
 *
 * Example usage:
 * ```kotlin
 * // Parse DSL lambda
 * val lambda = VosLambda(
 *     parameters = listOf("color"),
 *     body = listOf(
 *         VosStatement.FunctionCall(
 *             target = "VoiceOS.speak",
 *             arguments = listOf(VosValue.StringValue("Color selected"))
 *         )
 *     )
 * )
 *
 * // Create adapter
 * val adapter = CallbackAdapter(eventBus, context)
 *
 * // Generate Kotlin callback
 * val callback = adapter.createCallback(lambda, "colorPicker1", "onColorChange")
 *
 * // Invoke callback with parameters
 * callback(mapOf("color" to "#FF5733"))
 * ```
 *
 * Architecture notes:
 * - Stateless design: All state is in EventContext, not the adapter
 * - Extensible dispatch: Easy to add new function targets (VoiceOS, Preferences, etc.)
 * - Synchronous execution: Statements execute sequentially (async support via launch)
 * - Type-safe resolution: VosValue types map directly to Kotlin types
 *
 * Created by Manoj Jhawar, manoj@ideahq.net
 * Date: 2025-10-27 12:18:38 PDT
 *
 * @property eventBus EventBus for emitting component events
 * @property context Root execution context with global variables
 *
 * @see VosLambda for DSL lambda representation
 * @see EventContext for variable scoping
 * @see ComponentEvent for event structure
 */
class CallbackAdapter(
    private val eventBus: EventBus,
    private val context: EventContext
) {

    /**
     * Create a Kotlin callback from a DSL lambda.
     *
     * This method generates a Kotlin function that:
     * 1. Creates a child context with lambda parameters
     * 2. Executes each statement in the lambda body
     * 3. Emits a ComponentEvent for tracking (optional)
     *
     * The returned callback accepts a map of parameter names to values,
     * which are bound to the lambda's declared parameters.
     *
     * Example:
     * ```kotlin
     * val lambda = VosLambda(
     *     parameters = listOf("color"),
     *     body = listOf(...)
     * )
     *
     * val callback = adapter.createCallback(
     *     lambda = lambda,
     *     componentId = "colorPicker1",
     *     eventName = "onColorChange"
     * )
     *
     * // Invoke with parameters
     * callback(mapOf("color" to "#FF5733"))
     * ```
     *
     * @param lambda DSL lambda with parameters and statements
     * @param componentId Component ID for event tracking
     * @param eventName Event name for tracking and debugging
     * @return Kotlin callback function accepting parameter map
     * @throws CallbackException if lambda execution fails
     */
    fun createCallback(
        lambda: VosLambda,
        componentId: String,
        eventName: String
    ): (Map<String, Any?>) -> Unit {
        return { parameters ->
            // Execute lambda with parameters
            executeLambda(lambda, parameters, componentId, eventName)
        }
    }

    /**
     * Execute DSL lambda statements.
     *
     * Internal method that:
     * 1. Creates a child context with lambda parameters
     * 2. Executes each statement in sequence
     * 3. Handles errors and provides diagnostic information
     *
     * @param lambda DSL lambda to execute
     * @param parameters Runtime parameter values
     * @param componentId Component that triggered the callback
     * @param eventName Event that triggered the callback
     * @throws CallbackException if statement execution fails
     */
    private fun executeLambda(
        lambda: VosLambda,
        parameters: Map<String, Any?>,
        componentId: String,
        eventName: String
    ) {
        // Validate parameter count matches
        if (parameters.size != lambda.parameters.size) {
            throw CallbackException(
                "Parameter count mismatch for $componentId.$eventName: " +
                "expected ${lambda.parameters.size} (${lambda.parameters.joinToString()}), " +
                "got ${parameters.size} (${parameters.keys.joinToString()})"
            )
        }

        // Create execution context with parameters
        val localContext = context.createChild(parameters)

        // Execute each statement
        try {
            lambda.body.forEach { statement ->
                executeStatement(statement, localContext)
            }
        } catch (e: CallbackException) {
            // Re-throw with context
            throw CallbackException(
                "Error in $componentId.$eventName callback: ${e.message}",
                e
            )
        } catch (e: Exception) {
            // Wrap unexpected errors
            throw CallbackException(
                "Unexpected error in $componentId.$eventName callback: ${e.message}",
                e
            )
        }

        // TODO: Emit event for tracking (requires suspend context)
        // launch {
        //     eventBus.emit(ComponentEvent(componentId, eventName, parameters))
        // }
    }

    /**
     * Execute a single DSL statement.
     *
     * Dispatches to the appropriate execution method based on statement type:
     * - FunctionCall: Invoke a function (VoiceOS.speak, etc.)
     * - Assignment: Set a variable value
     * - IfStatement: Conditional execution
     *
     * @param statement Statement to execute
     * @param context Execution context with variables
     * @throws CallbackException if statement execution fails
     */
    private fun executeStatement(
        statement: VosStatement,
        context: EventContext
    ) {
        when (statement) {
            is VosStatement.FunctionCall -> executeFunctionCall(statement, context)
            is VosStatement.Assignment -> executeAssignment(statement, context)
            is VosStatement.IfStatement -> executeIfStatement(statement, context)
            is VosStatement.Return -> {
                // Return statement - currently no-op (would need control flow support)
                // TODO: Implement return value handling if needed
            }
        }
    }

    /**
     * Execute a function call statement.
     *
     * Parses the target (e.g., "VoiceOS.speak"), resolves arguments,
     * and dispatches to the appropriate handler.
     *
     * Supported targets:
     * - VoiceOS.speak(text): Text-to-speech
     * - Preferences.set(key, value): Save preference
     * - Preferences.get(key): Load preference
     *
     * @param call Function call statement
     * @param context Execution context for variable resolution
     * @throws CallbackException if target is invalid or execution fails
     */
    private fun executeFunctionCall(call: VosStatement.FunctionCall, context: EventContext) {
        // Parse target: "VoiceOS.speak", "Preferences.set", etc.
        val parts = call.target.split(".")
        if (parts.size != 2) {
            throw CallbackException(
                "Invalid function call: ${call.target}. " +
                "Expected format: 'Target.method' (e.g., 'VoiceOS.speak')"
            )
        }

        val (target, method) = parts
        val args = call.arguments.map { resolveValue(it, context) }

        // Dispatch to target
        when (target) {
            "VoiceOS" -> executeVoiceOSMethod(method, args)
            "Preferences" -> executePreferencesMethod(method, args)
            else -> throw CallbackException("Unknown target: $target. Supported targets: VoiceOS, Preferences")
        }
    }

    /**
     * Execute an assignment statement.
     *
     * Evaluates the right-hand side value and assigns it to the target
     * variable in the current context.
     *
     * Example DSL:
     * ```
     * currentColor = color
     * count = 42
     * enabled = true
     * ```
     *
     * @param assignment Assignment statement
     * @param context Execution context
     */
    private fun executeAssignment(assignment: VosStatement.Assignment, context: EventContext) {
        val value = resolveValue(assignment.value, context)
        context.set(assignment.target, value)
    }

    /**
     * Execute a conditional statement.
     *
     * Evaluates the condition and executes the appropriate block
     * (then-block or else-block).
     *
     * Example DSL:
     * ```
     * if (enabled) {
     *     VoiceOS.speak("Enabled")
     * } else {
     *     VoiceOS.speak("Disabled")
     * }
     * ```
     *
     * @param ifStmt Conditional statement
     * @param context Execution context
     * @throws CallbackException if condition is not boolean
     */
    private fun executeIfStatement(ifStmt: VosStatement.IfStatement, context: EventContext) {
        val condition = resolveValue(ifStmt.condition, context) as? Boolean
            ?: throw CallbackException(
                "If condition must evaluate to boolean, got: ${ifStmt.condition}"
            )

        val block = if (condition) ifStmt.thenBlock else ifStmt.elseBlock
        block.forEach { executeStatement(it, context) }
    }

    /**
     * Resolve a VosValue to a Kotlin runtime value.
     *
     * Handles variable references, literals, and complex structures:
     * - StringValue: Check if it's a variable reference, else use literal
     * - IntValue, FloatValue, BoolValue: Return wrapped value
     * - ArrayValue: Recursively resolve elements
     * - ObjectValue: Recursively resolve properties
     * - NullValue: Return null
     *
     * Example:
     * ```kotlin
     * // Variable reference
     * val value1 = resolveValue(VosValue.StringValue("color"), context)
     * // returns "#FF5733" if color is set in context
     *
     * // String literal
     * val value2 = resolveValue(VosValue.StringValue("Hello"), context)
     * // returns "Hello" (not a variable)
     *
     * // Array
     * val value3 = resolveValue(VosValue.ArrayValue(...), context)
     * // returns List<Any?>
     * ```
     *
     * @param value VosValue to resolve
     * @param context Execution context for variable lookup
     * @return Resolved Kotlin value
     */
    private fun resolveValue(value: VosValue, context: EventContext): Any? {
        return when (value) {
            is VosValue.StringValue -> {
                // Check if it's a variable reference
                if (context.has(value.value)) {
                    context.get(value.value)
                } else {
                    value.value
                }
            }
            is VosValue.IntValue -> value.value
            is VosValue.FloatValue -> value.value
            is VosValue.BoolValue -> value.value
            is VosValue.ArrayValue -> value.items.map { resolveValue(it, context) }
            is VosValue.ObjectValue -> value.properties.mapValues { resolveValue(it.value, context) }
            is VosValue.NullValue -> null
        }
    }

    /**
     * Execute a VoiceOS API method call.
     *
     * Placeholder implementation for VoiceOS system functions.
     * Will be replaced with actual VoiceOS API integration.
     *
     * Supported methods:
     * - speak(text): Text-to-speech
     *
     * @param method Method name
     * @param args Method arguments
     * @throws CallbackException if method is unknown
     */
    private fun executeVoiceOSMethod(method: String, args: List<Any?>) {
        when (method) {
            "speak" -> {
                val text = args.firstOrNull()?.toString() ?: ""
                println("[VoiceOS.speak] $text")  // Placeholder
                // TODO: Integrate with actual VoiceOS TTS
                // VoiceOSApi.speak(text)
            }
            else -> throw CallbackException(
                "Unknown VoiceOS method: $method. Supported methods: speak"
            )
        }
    }

    /**
     * Execute a Preferences API method call.
     *
     * Placeholder implementation for preferences storage.
     * Will be replaced with actual PreferencesManager integration.
     *
     * Supported methods:
     * - set(key, value): Save preference
     * - get(key): Load preference
     *
     * @param method Method name
     * @param args Method arguments
     * @throws CallbackException if method is unknown or arguments are invalid
     */
    private fun executePreferencesMethod(method: String, args: List<Any?>) {
        when (method) {
            "set" -> {
                val key = args.getOrNull(0)?.toString()
                    ?: throw CallbackException("Preferences.set requires key argument")
                val value = args.getOrNull(1)
                println("[Preferences.set] $key = $value")  // Placeholder
                // TODO: Integrate with PreferencesManager
                // PreferencesManager.set(key, value)
            }
            "get" -> {
                val key = args.firstOrNull()?.toString()
                    ?: throw CallbackException("Preferences.get requires key argument")
                println("[Preferences.get] $key")  // Placeholder
                // TODO: Integrate with PreferencesManager
                // return PreferencesManager.get(key)
            }
            else -> throw CallbackException(
                "Unknown Preferences method: $method. Supported methods: set, get"
            )
        }
    }

    companion object {
        /**
         * Creates a CallbackAdapter with a fresh EventBus and EventContext.
         *
         * Convenience factory for standalone usage.
         *
         * @return CallbackAdapter with new dependencies
         */
        fun create(): CallbackAdapter {
            return CallbackAdapter(
                eventBus = EventBus(),
                context = EventContext()
            )
        }

        /**
         * Creates a CallbackAdapter with standard VoiceOS globals.
         *
         * Includes pre-configured context with system objects.
         *
         * @param eventBus EventBus to use (creates new if null)
         * @return CallbackAdapter with standard globals
         */
        fun withStandardGlobals(eventBus: EventBus = EventBus()): CallbackAdapter {
            return CallbackAdapter(
                eventBus = eventBus,
                context = EventContext.withStandardGlobals()
            )
        }
    }
}

/**
 * Exception thrown when callback execution fails.
 *
 * CallbackException wraps errors that occur during DSL lambda execution,
 * providing context about what went wrong and where.
 *
 * Example usage:
 * ```kotlin
 * try {
 *     executeCallback(...)
 * } catch (e: CallbackException) {
 *     println("Callback error: ${e.message}")
 *     e.cause?.let { println("Caused by: ${it.message}") }
 * }
 * ```
 *
 * @property message Error description
 * @property cause Optional underlying exception
 */
class CallbackException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause) {

    companion object {
        /**
         * Creates a CallbackException for parameter validation errors.
         *
         * @param paramName Parameter name
         * @param expected Expected type or value
         * @param actual Actual type or value
         * @return CallbackException with detailed message
         */
        fun parameterError(paramName: String, expected: String, actual: String): CallbackException {
            return CallbackException(
                "Parameter '$paramName' error: expected $expected, got $actual"
            )
        }

        /**
         * Creates a CallbackException for missing required parameters.
         *
         * @param paramName Parameter name
         * @param eventName Event that triggered the callback
         * @return CallbackException with detailed message
         */
        fun missingParameter(paramName: String, eventName: String): CallbackException {
            return CallbackException(
                "Required parameter '$paramName' not provided for event '$eventName'"
            )
        }

        /**
         * Creates a CallbackException for unknown function targets.
         *
         * @param target Target name (e.g., "UnknownAPI")
         * @param supportedTargets List of supported targets
         * @return CallbackException with detailed message
         */
        fun unknownTarget(target: String, supportedTargets: List<String>): CallbackException {
            return CallbackException(
                "Unknown target '$target'. Supported targets: ${supportedTargets.joinToString()}"
            )
        }
    }
}
