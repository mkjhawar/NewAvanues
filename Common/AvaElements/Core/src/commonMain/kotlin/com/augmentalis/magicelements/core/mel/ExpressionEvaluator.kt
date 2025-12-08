package com.augmentalis.magicelements.core.mel

import com.augmentalis.magicelements.core.mel.functions.PluginTier
import com.augmentalis.magicelements.core.modules.ModuleRegistry
import kotlinx.coroutines.runBlocking

/**
 * Evaluator for MagicUI Expression Language (MEL) with tier enforcement.
 *
 * Executes an Abstract Syntax Tree (AST) against a state context and function registry.
 *
 * Features:
 * - Evaluates state references: `$state.path.to.value`
 * - Executes function calls: `$math.add(1, 2)`
 * - Evaluates binary operations: `+, -, *, /, ==, !=, >, <, >=, <=, &&, ||`
 * - Evaluates unary operations: `!, -`
 * - Tier 1 whitelist enforcement for Apple compliance
 * - Parameter substitution for reducer definitions
 *
 * Example:
 * ```
 * val state = mapOf("count" to 5)
 * val params = emptyMap<String, Any>()
 * val evaluator = ExpressionEvaluator(state, params, PluginTier.DATA, registry)
 *
 * val ast = ExpressionNode.BinaryOp(
 *     "+",
 *     ExpressionNode.StateRef(listOf("count")),
 *     ExpressionNode.Literal(LiteralValue.NumberValue(1.0))
 * )
 *
 * val result = evaluator.evaluate(ast) // Returns 6.0
 * ```
 */
class ExpressionEvaluator(
    private val state: Map<String, Any?>,
    private val params: Map<String, Any?> = emptyMap(),
    private val tier: PluginTier = PluginTier.DATA,
    private val functionRegistry: FunctionRegistry = FunctionRegistry.default()
) {

    /**
     * Evaluate an expression node and return the result.
     */
    fun evaluate(node: ExpressionNode): Any? {
        return when (node) {
            is ExpressionNode.Literal -> evaluateLiteral(node)
            is ExpressionNode.StateRef -> evaluateStateRef(node)
            is ExpressionNode.FunctionCall -> evaluateFunctionCall(node)
            is ExpressionNode.BinaryOp -> evaluateBinaryOp(node)
            is ExpressionNode.UnaryOp -> evaluateUnaryOp(node)
            is ExpressionNode.ParamRef -> evaluateParamRef(node)
            is ExpressionNode.ArrayLiteral -> evaluateArrayLiteral(node)
            is ExpressionNode.ObjectLiteral -> evaluateObjectLiteral(node)
            is ExpressionNode.ModuleCall -> evaluateModuleCall(node)
        }
    }

    /**
     * Evaluate an expression node asynchronously (for module calls).
     */
    suspend fun evaluateAsync(node: ExpressionNode): Any? {
        return when (node) {
            is ExpressionNode.ModuleCall -> evaluateModuleCallAsync(node)
            else -> evaluate(node)
        }
    }

    /**
     * Evaluate a literal value.
     */
    private fun evaluateLiteral(node: ExpressionNode.Literal): Any? {
        return node.value.toAny()
    }

    /**
     * Evaluate a state reference.
     * Example: $state.count -> state["count"]
     * Example: $state.user.name -> state["user"]["name"]
     */
    private fun evaluateStateRef(node: ExpressionNode.StateRef): Any? {
        var current: Any? = state

        for (key in node.path) {
            current = when (current) {
                is Map<*, *> -> {
                    @Suppress("UNCHECKED_CAST")
                    (current as? Map<String, Any?>)?.get(key)
                }
                is List<*> -> {
                    val index = key.toIntOrNull()
                        ?: throw EvaluationException("Invalid array index: $key")
                    if (index < 0 || index >= current.size) {
                        throw EvaluationException("Array index out of bounds: $index")
                    }
                    current[index]
                }
                else -> throw EvaluationException("Cannot access property '$key' on non-object/array")
            }
        }

        return current
    }

    /**
     * Evaluate a function call with tier enforcement.
     */
    private fun evaluateFunctionCall(node: ExpressionNode.FunctionCall): Any? {
        val funcKey = "${node.category}.${node.name}"

        // Tier 1 whitelist enforcement
        if (tier == PluginTier.DATA && !functionRegistry.isTier1Function(funcKey)) {
            throw EvaluationException("Function '$funcKey' is not allowed in Tier 1 (DATA mode)")
        }

        // Evaluate arguments
        val args = node.args.map { evaluate(it) }

        // Execute function
        return functionRegistry.execute(funcKey, args)
    }

    /**
     * Evaluate a binary operation.
     */
    private fun evaluateBinaryOp(node: ExpressionNode.BinaryOp): Any? {
        val left = evaluate(node.left)
        val right = evaluate(node.right)

        return when (node.op) {
            // Arithmetic
            "+" -> add(left, right)
            "-" -> subtract(left, right)
            "*" -> multiply(left, right)
            "/" -> divide(left, right)
            "%" -> modulo(left, right)

            // Comparison
            "==" -> equals(left, right)
            "!=" -> !equals(left, right)
            ">" -> greaterThan(left, right)
            "<" -> lessThan(left, right)
            ">=" -> greaterThanOrEqual(left, right)
            "<=" -> lessThanOrEqual(left, right)

            // Logical
            "&&" -> logicalAnd(left, right)
            "||" -> logicalOr(left, right)

            else -> throw EvaluationException("Unknown binary operator: ${node.op}")
        }
    }

    /**
     * Evaluate a unary operation.
     */
    private fun evaluateUnaryOp(node: ExpressionNode.UnaryOp): Any? {
        val operand = evaluate(node.operand)

        return when (node.op) {
            "!" -> logicalNot(operand)
            "-" -> negate(operand)
            else -> throw EvaluationException("Unknown unary operator: ${node.op}")
        }
    }

    /**
     * Evaluate a parameter reference.
     * Example: $digit in reducer with params: [digit]
     */
    private fun evaluateParamRef(node: ExpressionNode.ParamRef): Any? {
        return params[node.name]
            ?: throw EvaluationException("Parameter '${node.name}' not found")
    }

    /**
     * Evaluate an array literal.
     */
    private fun evaluateArrayLiteral(node: ExpressionNode.ArrayLiteral): Any {
        return node.elements.map { evaluate(it) }
    }

    /**
     * Evaluate an object literal.
     */
    private fun evaluateObjectLiteral(node: ExpressionNode.ObjectLiteral): Any {
        return node.properties.mapValues { (_, expr) -> evaluate(expr) }
    }

    /**
     * Evaluate a module call synchronously.
     * Uses runBlocking for non-suspend contexts - prefer evaluateAsync when possible.
     */
    private fun evaluateModuleCall(node: ExpressionNode.ModuleCall): Any? {
        // Check if module is available
        if (!ModuleRegistry.isRegistered(node.module)) {
            throw EvaluationException("Module '${node.module}' is not registered")
        }

        // Check if method is available at current tier
        if (!ModuleRegistry.isMethodAvailable(node.module, node.method, tier)) {
            throw EvaluationException(
                "Module method '@${node.module}.${node.method}' is not available in tier $tier"
            )
        }

        // Evaluate arguments
        val args = node.args.map { evaluate(it) }

        // Execute module call (blocking)
        return runBlocking {
            ModuleRegistry.execute(node.module, node.method, args, tier)
        }
    }

    /**
     * Evaluate a module call asynchronously.
     * Preferred method for module calls.
     */
    private suspend fun evaluateModuleCallAsync(node: ExpressionNode.ModuleCall): Any? {
        // Check if module is available
        if (!ModuleRegistry.isRegistered(node.module)) {
            throw EvaluationException("Module '${node.module}' is not registered")
        }

        // Check if method is available at current tier
        if (!ModuleRegistry.isMethodAvailable(node.module, node.method, tier)) {
            throw EvaluationException(
                "Module method '@${node.module}.${node.method}' is not available in tier $tier"
            )
        }

        // Evaluate arguments
        val args = node.args.map { evaluate(it) }

        // Execute module call
        return ModuleRegistry.execute(node.module, node.method, args, tier)
    }

    // ========== Arithmetic Operations ==========

    private fun add(left: Any?, right: Any?): Any {
        return when {
            left is Number && right is Number -> toDouble(left) + toDouble(right)
            left is String || right is String -> left.toString() + right.toString()
            else -> throw EvaluationException("Cannot add $left and $right")
        }
    }

    private fun subtract(left: Any?, right: Any?): Double {
        if (left !is Number || right !is Number) {
            throw EvaluationException("Cannot subtract non-numbers: $left - $right")
        }
        return toDouble(left) - toDouble(right)
    }

    private fun multiply(left: Any?, right: Any?): Double {
        if (left !is Number || right !is Number) {
            throw EvaluationException("Cannot multiply non-numbers: $left * $right")
        }
        return toDouble(left) * toDouble(right)
    }

    private fun divide(left: Any?, right: Any?): Double {
        if (left !is Number || right !is Number) {
            throw EvaluationException("Cannot divide non-numbers: $left / $right")
        }
        val divisor = toDouble(right)
        if (divisor == 0.0) {
            throw EvaluationException("Division by zero")
        }
        return toDouble(left) / divisor
    }

    private fun modulo(left: Any?, right: Any?): Double {
        if (left !is Number || right !is Number) {
            throw EvaluationException("Cannot modulo non-numbers: $left % $right")
        }
        return toDouble(left) % toDouble(right)
    }

    private fun negate(operand: Any?): Double {
        if (operand !is Number) {
            throw EvaluationException("Cannot negate non-number: $operand")
        }
        return -toDouble(operand)
    }

    // ========== Comparison Operations ==========

    private fun equals(left: Any?, right: Any?): Boolean {
        return when {
            left is Number && right is Number -> toDouble(left) == toDouble(right)
            else -> left == right
        }
    }

    private fun greaterThan(left: Any?, right: Any?): Boolean {
        if (left !is Number || right !is Number) {
            throw EvaluationException("Cannot compare non-numbers: $left > $right")
        }
        return toDouble(left) > toDouble(right)
    }

    private fun lessThan(left: Any?, right: Any?): Boolean {
        if (left !is Number || right !is Number) {
            throw EvaluationException("Cannot compare non-numbers: $left < $right")
        }
        return toDouble(left) < toDouble(right)
    }

    private fun greaterThanOrEqual(left: Any?, right: Any?): Boolean {
        if (left !is Number || right !is Number) {
            throw EvaluationException("Cannot compare non-numbers: $left >= $right")
        }
        return toDouble(left) >= toDouble(right)
    }

    private fun lessThanOrEqual(left: Any?, right: Any?): Boolean {
        if (left !is Number || right !is Number) {
            throw EvaluationException("Cannot compare non-numbers: $left <= $right")
        }
        return toDouble(left) <= toDouble(right)
    }

    // ========== Logical Operations ==========

    private fun logicalAnd(left: Any?, right: Any?): Boolean {
        return isTruthy(left) && isTruthy(right)
    }

    private fun logicalOr(left: Any?, right: Any?): Boolean {
        return isTruthy(left) || isTruthy(right)
    }

    private fun logicalNot(operand: Any?): Boolean {
        return !isTruthy(operand)
    }

    // ========== Helper Methods ==========

    /**
     * Convert a Number to Double.
     */
    private fun toDouble(value: Any): Double {
        return when (value) {
            is Double -> value
            is Float -> value.toDouble()
            is Int -> value.toDouble()
            is Long -> value.toDouble()
            is Short -> value.toDouble()
            is Byte -> value.toDouble()
            else -> throw EvaluationException("Cannot convert $value to number")
        }
    }

    /**
     * Check if a value is truthy.
     * - false, null, 0, "" are falsy
     * - Everything else is truthy
     */
    private fun isTruthy(value: Any?): Boolean {
        return when (value) {
            null -> false
            is Boolean -> value
            is Number -> toDouble(value) != 0.0
            is String -> value.isNotEmpty()
            is Collection<*> -> value.isNotEmpty()
            else -> true
        }
    }
}

/**
 * Function registry interface.
 */
interface FunctionRegistry {
    /**
     * Check if a function is allowed in Tier 1.
     */
    fun isTier1Function(funcKey: String): Boolean

    /**
     * Execute a function with the given arguments.
     */
    fun execute(funcKey: String, args: List<Any?>): Any?

    companion object {
        /**
         * Get the default function registry with built-in functions.
         */
        fun default(): FunctionRegistry = DefaultFunctionRegistry()
    }
}

/**
 * Default function registry with placeholder implementations.
 * Full implementations will be provided in Phase 4 (Built-in Functions).
 */
private class DefaultFunctionRegistry : FunctionRegistry {

    private val tier1Functions = setOf(
        // Math
        "math.add", "math.subtract", "math.multiply", "math.divide", "math.mod",
        "math.abs", "math.round", "math.floor", "math.ceil", "math.min", "math.max",

        // String
        "string.concat", "string.length", "string.substring",
        "string.uppercase", "string.lowercase", "string.trim",
        "string.replace", "string.split", "string.join",

        // Array
        "array.length", "array.get", "array.first", "array.last",
        "array.append", "array.prepend", "array.remove",
        "array.filter", "array.map", "array.sort",

        // Object
        "object.get", "object.set", "object.keys", "object.values", "object.merge",

        // Date
        "date.now", "date.format", "date.parse", "date.add", "date.subtract", "date.diff",

        // Logic
        "logic.if", "logic.and", "logic.or", "logic.not",
        "logic.equals", "logic.gt", "logic.lt", "logic.gte", "logic.lte"
    )

    override fun isTier1Function(funcKey: String): Boolean {
        return funcKey in tier1Functions
    }

    override fun execute(funcKey: String, args: List<Any?>): Any? {
        // Placeholder implementations for basic functions
        // Full implementations will be in Phase 4
        return when (funcKey) {
            // Math
            "math.add" -> {
                require(args.size == 2) { "math.add requires 2 arguments" }
                val a = toDouble(args[0])
                val b = toDouble(args[1])
                a + b
            }
            "math.subtract" -> {
                require(args.size == 2) { "math.subtract requires 2 arguments" }
                val a = toDouble(args[0])
                val b = toDouble(args[1])
                a - b
            }
            "math.multiply" -> {
                require(args.size == 2) { "math.multiply requires 2 arguments" }
                val a = toDouble(args[0])
                val b = toDouble(args[1])
                a * b
            }
            "math.divide" -> {
                require(args.size == 2) { "math.divide requires 2 arguments" }
                val a = toDouble(args[0])
                val b = toDouble(args[1])
                require(b != 0.0) { "Division by zero" }
                a / b
            }

            // String
            "string.concat" -> {
                args.joinToString("") { it?.toString() ?: "" }
            }
            "string.length" -> {
                require(args.size == 1) { "string.length requires 1 argument" }
                args[0]?.toString()?.length ?: 0
            }

            // Logic
            "logic.if" -> {
                require(args.size == 3) { "logic.if requires 3 arguments (condition, ifTrue, ifFalse)" }
                val condition = isTruthy(args[0])
                if (condition) args[1] else args[2]
            }
            "logic.and" -> {
                args.all { isTruthy(it) }
            }
            "logic.or" -> {
                args.any { isTruthy(it) }
            }
            "logic.not" -> {
                require(args.size == 1) { "logic.not requires 1 argument" }
                !isTruthy(args[0])
            }
            "logic.equals" -> {
                require(args.size == 2) { "logic.equals requires 2 arguments" }
                args[0] == args[1]
            }

            else -> throw EvaluationException("Unknown function: $funcKey")
        }
    }

    private fun toDouble(value: Any?): Double {
        return when (value) {
            is Double -> value
            is Float -> value.toDouble()
            is Int -> value.toDouble()
            is Long -> value.toDouble()
            is Short -> value.toDouble()
            is Byte -> value.toDouble()
            is String -> value.toDoubleOrNull() ?: throw EvaluationException("Cannot convert '$value' to number")
            else -> throw EvaluationException("Cannot convert $value to number")
        }
    }

    private fun isTruthy(value: Any?): Boolean {
        return when (value) {
            null -> false
            is Boolean -> value
            is Number -> toDouble(value) != 0.0
            is String -> value.isNotEmpty()
            is Collection<*> -> value.isNotEmpty()
            else -> true
        }
    }
}

/**
 * Exception thrown during expression evaluation.
 */
class EvaluationException(message: String) : Exception(message)
