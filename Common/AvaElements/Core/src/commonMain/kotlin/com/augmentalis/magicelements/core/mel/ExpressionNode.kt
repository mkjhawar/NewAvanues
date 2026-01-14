package com.augmentalis.magicelements.core.mel

import kotlinx.serialization.Serializable

/**
 * Abstract Syntax Tree (AST) node types for MagicUI Expression Language (MEL).
 *
 * MEL supports:
 * - State references: `$state.path.to.value`
 * - Function calls: `$math.add(1, 2)`, `$string.concat("Hello", " World")`
 * - Literals: numbers, strings, booleans, null
 * - Binary operations: +, -, *, /, ==, !=, >, <, >=, <=, &&, ||
 * - Parameter references: `$param` (in reducer definitions)
 */
@Serializable
sealed class ExpressionNode {

    /**
     * Literal value node.
     * Examples: 42, "Hello", true, null
     */
    @Serializable
    data class Literal(val value: LiteralValue) : ExpressionNode()

    /**
     * State reference node.
     * Examples:
     * - `$state.count` -> StateRef(path = ["count"])
     * - `$state.user.name` -> StateRef(path = ["user", "name"])
     * - `$state.items[0]` -> StateRef(path = ["items", "0"])
     */
    @Serializable
    data class StateRef(val path: List<String>) : ExpressionNode()

    /**
     * Function call node.
     * Examples:
     * - `$math.add(1, 2)` -> FunctionCall(category = "math", name = "add", args = [Literal(1), Literal(2)])
     * - `$string.concat($state.name, "!")` -> FunctionCall(category = "string", name = "concat", args = [StateRef(["name"]), Literal("!")])
     */
    @Serializable
    data class FunctionCall(
        val category: String,
        val name: String,
        val args: List<ExpressionNode>
    ) : ExpressionNode()

    /**
     * Binary operation node.
     * Examples:
     * - `$state.a + 1` -> BinaryOp(op = "+", left = StateRef(["a"]), right = Literal(1))
     * - `$state.x > 5` -> BinaryOp(op = ">", left = StateRef(["x"]), right = Literal(5))
     * - `$state.enabled && $state.count > 0` -> BinaryOp(op = "&&", ...)
     */
    @Serializable
    data class BinaryOp(
        val op: String,
        val left: ExpressionNode,
        val right: ExpressionNode
    ) : ExpressionNode()

    /**
     * Parameter reference node (used in reducer definitions).
     * Example: In reducer with `params: [digit]`, `$digit` -> ParamRef(name = "digit")
     */
    @Serializable
    data class ParamRef(val name: String) : ExpressionNode()

    /**
     * Unary operation node.
     * Examples:
     * - `!$state.enabled` -> UnaryOp(op = "!", operand = StateRef(["enabled"]))
     * - `-$state.value` -> UnaryOp(op = "-", operand = StateRef(["value"]))
     */
    @Serializable
    data class UnaryOp(
        val op: String,
        val operand: ExpressionNode
    ) : ExpressionNode()

    /**
     * Array literal node.
     * Example: `[1, 2, 3]` -> ArrayLiteral(elements = [Literal(1), Literal(2), Literal(3)])
     */
    @Serializable
    data class ArrayLiteral(val elements: List<ExpressionNode>) : ExpressionNode()

    /**
     * Object literal node.
     * Example: `{ x: 10, y: 20 }` -> ObjectLiteral(properties = {"x" to Literal(10), "y" to Literal(20)})
     */
    @Serializable
    data class ObjectLiteral(val properties: Map<String, ExpressionNode>) : ExpressionNode()

    /**
     * Module call node for platform module invocation.
     * Examples:
     * - `@voice.listen()` -> ModuleCall(module = "voice", method = "listen", args = [])
     * - `@browser.open("https://example.com")` -> ModuleCall(module = "browser", method = "open", args = [Literal("...")])
     * - `@device.screen.width()` -> ModuleCall(module = "device", method = "screen.width", args = [])
     */
    @Serializable
    data class ModuleCall(
        val module: String,
        val method: String,
        val args: List<ExpressionNode>
    ) : ExpressionNode()
}

/**
 * Serializable wrapper for literal values.
 */
@Serializable
sealed class LiteralValue {
    @Serializable
    data class NumberValue(val value: Double) : LiteralValue()

    @Serializable
    data class StringValue(val value: String) : LiteralValue()

    @Serializable
    data class BooleanValue(val value: Boolean) : LiteralValue()

    @Serializable
    object NullValue : LiteralValue()

    /**
     * Convert to platform-native type.
     */
    fun toAny(): Any? = when (this) {
        is NumberValue -> value
        is StringValue -> value
        is BooleanValue -> value
        is NullValue -> null
    }
}

/**
 * Binary operators supported in MEL.
 */
enum class BinaryOperator(val symbol: String, val precedence: Int) {
    // Arithmetic
    ADD("+", 4),
    SUBTRACT("-", 4),
    MULTIPLY("*", 5),
    DIVIDE("/", 5),
    MODULO("%", 5),

    // Comparison
    EQUALS("==", 3),
    NOT_EQUALS("!=", 3),
    GREATER_THAN(">", 3),
    LESS_THAN("<", 3),
    GREATER_THAN_OR_EQUAL(">=", 3),
    LESS_THAN_OR_EQUAL("<=", 3),

    // Logical
    AND("&&", 2),
    OR("||", 1);

    companion object {
        fun fromSymbol(symbol: String): BinaryOperator? =
            entries.find { it.symbol == symbol }
    }
}

/**
 * Unary operators supported in MEL.
 */
enum class UnaryOperator(val symbol: String) {
    NOT("!"),
    NEGATE("-");

    companion object {
        fun fromSymbol(symbol: String): UnaryOperator? =
            entries.find { it.symbol == symbol }
    }
}
