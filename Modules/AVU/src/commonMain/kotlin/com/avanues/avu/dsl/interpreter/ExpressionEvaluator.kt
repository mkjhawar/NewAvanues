package com.avanues.avu.dsl.interpreter

import com.avanues.avu.dsl.ast.*

/**
 * Internal marker for a built-in callable (e.g., screen.contains).
 * Created during MemberAccess evaluation, consumed during CallExpression evaluation.
 * Dispatched as a QRY code through the dispatcher.
 */
internal data class BuiltInCallable(
    val target: String,
    val method: String
)

/**
 * Evaluates AVU DSL expressions to runtime values.
 *
 * Handles all 11 expression types from the AST with JavaScript-like type coercion rules.
 * Delegates built-in method calls (e.g., `screen.contains("text")`) to the dispatcher
 * as QRY code invocations.
 *
 * ## Type Coercion Rules
 * - `+`: String+Any=concat, Number+Number=add (Int if both Int, else Double)
 * - `-`, `*`: Number only, Int if both Int else Double
 * - `/`: Number only, always Double
 * - `==`/`!=`: structural equality
 * - `<`/`>`/`<=`/`>=`: Number only
 * - `and`/`or`: Boolean coercion, non-short-circuit (both sides always evaluated)
 * - Boolean coercion: null/0/0.0/"" = false, else true
 */
internal class ExpressionEvaluator(
    private val dispatcher: IAvuDispatcher
) {
    // =========================================================================
    // PUBLIC API
    // =========================================================================

    /** Evaluate an expression to a runtime value. */
    suspend fun evaluate(expr: AvuAstNode.Expression, context: ExecutionContext): Any? {
        return when (expr) {
            is AvuAstNode.Expression.StringLiteral -> expr.value
            is AvuAstNode.Expression.IntLiteral -> expr.value
            is AvuAstNode.Expression.FloatLiteral -> expr.value
            is AvuAstNode.Expression.BooleanLiteral -> expr.value
            is AvuAstNode.Expression.VariableRef -> {
                if (!context.hasVariable(expr.name)) {
                    throw RuntimeError.UndefinedVariable(expr.name, expr.location)
                }
                context.getVariable(expr.name)
            }
            is AvuAstNode.Expression.Identifier -> expr.name
            is AvuAstNode.Expression.BinaryOp -> evaluateBinaryOp(expr, context)
            is AvuAstNode.Expression.UnaryOp -> evaluateUnaryOp(expr, context)
            is AvuAstNode.Expression.MemberAccess -> evaluateMemberAccess(expr, context)
            is AvuAstNode.Expression.CallExpression -> evaluateCallExpression(expr, context)
            is AvuAstNode.Expression.Grouped -> evaluate(expr.inner, context)
        }
    }

    /** Coerce any value to Boolean. Falsy: null, false, 0, 0.0, "". */
    fun toBooleanValue(value: Any?): Boolean = when (value) {
        null -> false
        is Boolean -> value
        is Int -> value != 0
        is Double -> value != 0.0
        is String -> value.isNotEmpty()
        else -> true
    }

    /** Convert to Number or throw TypeError. */
    fun toNumber(value: Any?, location: SourceLocation): Number {
        return toNumberOrNull(value)
            ?: throw RuntimeError.TypeError(
                "Number", typeNameOf(value), "numeric operation", location
            )
    }

    // =========================================================================
    // BINARY OPERATIONS
    // =========================================================================

    private suspend fun evaluateBinaryOp(
        expr: AvuAstNode.Expression.BinaryOp,
        context: ExecutionContext
    ): Any? {
        // Non-short-circuit: both sides always evaluated
        val left = evaluate(expr.left, context)
        val right = evaluate(expr.right, context)

        return when (expr.operator) {
            BinaryOperator.PLUS -> applyPlus(left, right, expr.location)
            BinaryOperator.MINUS -> applyArithmetic(left, right, "-", expr.location) { a, b -> a - b }
            BinaryOperator.STAR -> applyArithmetic(left, right, "*", expr.location) { a, b -> a * b }
            BinaryOperator.SLASH -> applyDivision(left, right, expr.location)
            BinaryOperator.EQ -> left == right
            BinaryOperator.NEQ -> left != right
            BinaryOperator.LT -> compareNumbers(left, right, expr.location) < 0
            BinaryOperator.GT -> compareNumbers(left, right, expr.location) > 0
            BinaryOperator.LTE -> compareNumbers(left, right, expr.location) <= 0
            BinaryOperator.GTE -> compareNumbers(left, right, expr.location) >= 0
            BinaryOperator.AND -> toBooleanValue(left) && toBooleanValue(right)
            BinaryOperator.OR -> toBooleanValue(left) || toBooleanValue(right)
        }
    }

    private fun applyPlus(left: Any?, right: Any?, location: SourceLocation): Any {
        // String concatenation if either side is String
        if (left is String || right is String) {
            return "${left ?: "null"}${right ?: "null"}"
        }
        return applyArithmetic(left, right, "+", location) { a, b -> a + b }
    }

    private fun applyArithmetic(
        left: Any?, right: Any?, op: String, location: SourceLocation,
        operation: (Double, Double) -> Double
    ): Any {
        val leftNum = toNumberOrNull(left)
        val rightNum = toNumberOrNull(right)
        if (leftNum == null || rightNum == null) {
            throw RuntimeError.TypeError(typeNameOf(left), typeNameOf(right), op, location)
        }
        val result = operation(leftNum.toDouble(), rightNum.toDouble())
        // Preserve Int type if both operands are Int and result is a whole number
        if (left is Int && right is Int && result == result.toLong().toDouble()) {
            return result.toInt()
        }
        return result
    }

    private fun applyDivision(left: Any?, right: Any?, location: SourceLocation): Double {
        val leftNum = toNumberOrNull(left)
        val rightNum = toNumberOrNull(right)
        if (leftNum == null || rightNum == null) {
            throw RuntimeError.TypeError(typeNameOf(left), typeNameOf(right), "/", location)
        }
        if (rightNum.toDouble() == 0.0) {
            throw RuntimeError.General("Division by zero", location)
        }
        return leftNum.toDouble() / rightNum.toDouble()
    }

    private fun compareNumbers(left: Any?, right: Any?, location: SourceLocation): Int {
        val leftNum = toNumberOrNull(left)
        val rightNum = toNumberOrNull(right)
        if (leftNum == null || rightNum == null) {
            throw RuntimeError.TypeError(typeNameOf(left), typeNameOf(right), "comparison", location)
        }
        return leftNum.toDouble().compareTo(rightNum.toDouble())
    }

    // =========================================================================
    // UNARY OPERATIONS
    // =========================================================================

    private suspend fun evaluateUnaryOp(
        expr: AvuAstNode.Expression.UnaryOp,
        context: ExecutionContext
    ): Any {
        val operand = evaluate(expr.operand, context)
        return when (expr.operator) {
            UnaryOperator.NOT -> !toBooleanValue(operand)
            UnaryOperator.NEGATE -> {
                val num = toNumberOrNull(operand)
                    ?: throw RuntimeError.TypeError(
                        "Number", typeNameOf(operand), "negate", expr.location
                    )
                if (operand is Int) -num.toInt() else -num.toDouble()
            }
        }
    }

    // =========================================================================
    // MEMBER ACCESS & CALL EXPRESSION
    // =========================================================================

    private suspend fun evaluateMemberAccess(
        expr: AvuAstNode.Expression.MemberAccess,
        context: ExecutionContext
    ): Any? {
        val target = evaluate(expr.target, context)
        // Built-in objects (screen, context) resolve to String via Identifier
        // Create a callable that will be dispatched as QRY code
        if (target is String) {
            return BuiltInCallable(target, expr.member)
        }
        throw RuntimeError.TypeError(
            typeNameOf(target), "object", "member access .${expr.member}", expr.location
        )
    }

    private suspend fun evaluateCallExpression(
        expr: AvuAstNode.Expression.CallExpression,
        context: ExecutionContext
    ): Any? {
        val callee = evaluate(expr.callee, context)
        if (callee is BuiltInCallable) {
            return dispatchBuiltIn(callee, expr.arguments, context, expr.location)
        }
        throw RuntimeError.TypeError(
            typeNameOf(callee), "callable", "function call", expr.location
        )
    }

    /**
     * Dispatch a built-in method call (e.g., screen.contains("text")) via QRY code.
     * The dispatcher receives: query, target, method, args.
     */
    private suspend fun dispatchBuiltIn(
        callable: BuiltInCallable,
        arguments: List<AvuAstNode.Expression>,
        context: ExecutionContext,
        location: SourceLocation
    ): Any? {
        val args = arguments.map { evaluate(it, context) }
        val queryArgs = mapOf(
            "query" to "${callable.target}_${callable.method}",
            "target" to callable.target,
            "method" to callable.method,
            "args" to args
        )
        return when (val result = dispatcher.dispatch("QRY", queryArgs)) {
            is DispatchResult.Success -> result.data
            is DispatchResult.Error -> throw RuntimeError.DispatchError(
                "QRY", result.message, location
            )
            is DispatchResult.Timeout -> throw RuntimeError.TimeoutError(
                result.timeoutMs,
                "Built-in call '${callable.target}.${callable.method}' timed out",
                location
            )
        }
    }

    // =========================================================================
    // TYPE HELPERS
    // =========================================================================

    private fun toNumberOrNull(value: Any?): Number? = when (value) {
        is Int -> value
        is Double -> value
        is Long -> value
        is Float -> value
        is String -> value.toIntOrNull() ?: value.toDoubleOrNull()
        is Boolean -> if (value) 1 else 0
        else -> null
    }

    private fun typeNameOf(value: Any?): String = when (value) {
        null -> "null"
        is String -> "String"
        is Int -> "Int"
        is Double -> "Double"
        is Long -> "Long"
        is Boolean -> "Boolean"
        is BuiltInCallable -> "BuiltInCallable"
        else -> value::class.simpleName ?: "Unknown"
    }
}
