package com.augmentalis.avamagic.dsl

import kotlinx.serialization.Serializable

/**
 * Represents a lambda/callback function in the DSL.
 *
 * Lambdas are used for event handlers and callbacks in VoiceOS components.
 * They can capture parameters from events (e.g., color, position) and execute
 * statements in response.
 *
 * Example DSL:
 * ```
 * onColorChange = { color ->
 *     VoiceOS.speak("Selected color: " + color)
 *     currentColor = color
 * }
 *
 * onClick = { x, y ->
 *     if (x > 100) {
 *         VoiceOS.speak("Right side clicked")
 *     }
 * }
 * ```
 *
 * Created by Manoj Jhawar, manoj@ideahq.net
 * Date: 2025-10-27 12:12:37 PDT
 *
 * @property parameters Parameter names for the lambda (e.g., ["color"], ["x", "y"])
 * @property body Statements executed when the lambda is invoked
 *
 * @see VosStatement for statement types
 */
@Serializable
data class VosLambda(
    val parameters: List<String>,
    val body: List<VosStatement>
) {
    /**
     * Returns a human-readable string representation of this lambda.
     * Shows parameter list and statement count.
     */
    override fun toString(): String {
        val params = if (parameters.isEmpty()) "()" else parameters.joinToString(", ")
        return "{ $params -> ${body.size} statements }"
    }

    /**
     * Returns true if this lambda has no parameters.
     */
    val isParameterless: Boolean get() = parameters.isEmpty()

    /**
     * Returns true if this lambda has no statements.
     */
    val isEmpty: Boolean get() = body.isEmpty()

    companion object {
        /**
         * Creates an empty lambda with no parameters or statements.
         * Useful for default/placeholder callbacks.
         *
         * @return Empty VosLambda
         */
        fun empty(): VosLambda = VosLambda(emptyList(), emptyList())

        /**
         * Creates a simple lambda with a single statement.
         * Common pattern for basic event handlers.
         *
         * @param parameters Lambda parameter names
         * @param statement Single statement to execute
         * @return VosLambda with one statement
         */
        fun simple(
            parameters: List<String> = emptyList(),
            statement: VosStatement
        ): VosLambda = VosLambda(parameters, listOf(statement))

        /**
         * Creates a lambda that calls a single function.
         * Convenience for simple callback forwarding.
         *
         * @param parameters Lambda parameter names
         * @param target Function target (e.g., "VoiceOS.speak")
         * @param arguments Function arguments
         * @return VosLambda with single function call
         */
        fun call(
            parameters: List<String> = emptyList(),
            target: String,
            arguments: List<VosValue> = emptyList()
        ): VosLambda = VosLambda(
            parameters,
            listOf(VosStatement.FunctionCall(target, arguments))
        )
    }
}

/**
 * Represents a statement in a lambda body.
 *
 * Statements are executable instructions within lambdas, including function
 * calls, variable assignments, and control flow (if/else).
 *
 * Example statements:
 * ```
 * VoiceOS.speak("Hello")              // FunctionCall
 * color = "#FF5733"                   // Assignment
 * if (enabled) { ... } else { ... }   // IfStatement
 * ```
 *
 * @see VosLambda for lambda/callback representation
 */
@Serializable
sealed class VosStatement {

    /**
     * Function call statement.
     *
     * Invokes a function with arguments. The target can be a simple name
     * (e.g., "speak") or a qualified path (e.g., "VoiceOS.speak").
     *
     * Example DSL:
     * ```
     * VoiceOS.speak("Hello World")
     * updateColor("#FF5733")
     * ```
     *
     * @property target Function identifier (possibly qualified with dots)
     * @property arguments List of argument values
     */
    @Serializable
    data class FunctionCall(
        val target: String,
        val arguments: List<VosValue>
    ) : VosStatement() {
        override fun toString(): String =
            "$target(${arguments.joinToString(", ")})"

        companion object {
            /**
             * Creates a function call with no arguments.
             *
             * @param target Function identifier
             * @return FunctionCall with empty argument list
             */
            fun noArgs(target: String): FunctionCall =
                FunctionCall(target, emptyList())
        }
    }

    /**
     * Assignment statement.
     *
     * Assigns a value to a variable or property. The target can be a simple
     * variable name or a property path.
     *
     * Example DSL:
     * ```
     * currentColor = "#FF5733"
     * config.theme = "dark"
     * ```
     *
     * @property target Variable/property identifier
     * @property value Value to assign
     */
    @Serializable
    data class Assignment(
        val target: String,
        val value: VosValue
    ) : VosStatement() {
        override fun toString(): String = "$target = $value"
    }

    /**
     * Conditional statement (if/else).
     *
     * Executes different statement blocks based on a condition. The condition
     * is evaluated at runtime, and either the then-block or else-block is executed.
     *
     * Example DSL:
     * ```
     * if (x > 100) {
     *     VoiceOS.speak("Large value")
     *     color = "red"
     * } else {
     *     VoiceOS.speak("Small value")
     *     color = "blue"
     * }
     * ```
     *
     * @property condition Boolean expression to evaluate
     * @property thenBlock Statements to execute if condition is true
     * @property elseBlock Statements to execute if condition is false (optional)
     */
    @Serializable
    data class IfStatement(
        val condition: VosValue,
        val thenBlock: List<VosStatement>,
        val elseBlock: List<VosStatement> = emptyList()
    ) : VosStatement() {
        override fun toString(): String {
            val elseClause = if (elseBlock.isNotEmpty()) {
                " else { ${elseBlock.size} statements }"
            } else ""
            return "if ($condition) { ${thenBlock.size} statements }$elseClause"
        }

        /**
         * Returns true if this if-statement has an else block.
         */
        val hasElse: Boolean get() = elseBlock.isNotEmpty()

        companion object {
            /**
             * Creates a simple if-statement with no else block.
             *
             * @param condition Condition to evaluate
             * @param thenBlock Statements to execute if true
             * @return IfStatement with no else clause
             */
            fun simple(
                condition: VosValue,
                thenBlock: List<VosStatement>
            ): IfStatement = IfStatement(condition, thenBlock)

            /**
             * Creates a simple if-statement with a single statement.
             * Convenience for basic conditional logic.
             *
             * @param condition Condition to evaluate
             * @param thenStatement Single statement to execute if true
             * @return IfStatement with one-statement then-block
             */
            fun singleStatement(
                condition: VosValue,
                thenStatement: VosStatement
            ): IfStatement = IfStatement(condition, listOf(thenStatement))
        }
    }

    /**
     * Return statement.
     *
     * Returns a value from a lambda/function.
     *
     * Example DSL:
     * ```
     * return color
     * return true
     * ```
     *
     * @property value Optional value to return (null for void return)
     */
    @Serializable
    data class Return(
        val value: VosValue? = null
    ) : VosStatement() {
        override fun toString(): String = if (value != null) "return $value" else "return"
    }
}
