package com.augmentalis.voiceoscore.dsl.interpreter

import com.augmentalis.voiceoscore.dsl.ast.*
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExpressionEvaluatorTest {

    private val dispatcher = MockDispatcher()
    private val evaluator = ExpressionEvaluator(dispatcher)
    private val context = ExecutionContext(SandboxConfig.TESTING)
    private val loc = SourceLocation(1, 1)

    // ==================== Literal Evaluation ====================

    @Test
    fun evaluate_stringLiteral() = runTest {
        val expr = AvuAstNode.Expression.StringLiteral("hello", loc)
        assertEquals("hello", evaluator.evaluate(expr, context))
    }

    @Test
    fun evaluate_intLiteral() = runTest {
        val expr = AvuAstNode.Expression.IntLiteral(42, loc)
        assertEquals(42, evaluator.evaluate(expr, context))
    }

    @Test
    fun evaluate_floatLiteral() = runTest {
        val expr = AvuAstNode.Expression.FloatLiteral(3.14, loc)
        assertEquals(3.14, evaluator.evaluate(expr, context))
    }

    @Test
    fun evaluate_booleanLiteral() = runTest {
        val trueExpr = AvuAstNode.Expression.BooleanLiteral(true, loc)
        val falseExpr = AvuAstNode.Expression.BooleanLiteral(false, loc)
        assertEquals(true, evaluator.evaluate(trueExpr, context))
        assertEquals(false, evaluator.evaluate(falseExpr, context))
    }

    // ==================== Variable Reference ====================

    @Test
    fun evaluate_variableRef_existingVariable() = runTest {
        context.setVariable("name", "John", loc)
        val expr = AvuAstNode.Expression.VariableRef("name", loc)
        assertEquals("John", evaluator.evaluate(expr, context))
    }

    @Test
    fun evaluate_variableRef_undefinedThrows() = runTest {
        val expr = AvuAstNode.Expression.VariableRef("missing", loc)
        assertFailsWith<RuntimeError.UndefinedVariable> {
            evaluator.evaluate(expr, context)
        }
    }

    // ==================== Identifier ====================

    @Test
    fun evaluate_identifier_returnsName() = runTest {
        val expr = AvuAstNode.Expression.Identifier("screen", loc)
        assertEquals("screen", evaluator.evaluate(expr, context))
    }

    // ==================== Binary Operations - Arithmetic ====================

    @Test
    fun evaluate_plusIntegers() = runTest {
        val expr = binOp(intLit(3), BinaryOperator.PLUS, intLit(4))
        assertEquals(7, evaluator.evaluate(expr, context))
    }

    @Test
    fun evaluate_plusDoubles() = runTest {
        val expr = binOp(floatLit(1.5), BinaryOperator.PLUS, floatLit(2.5))
        assertEquals(4.0, evaluator.evaluate(expr, context))
    }

    @Test
    fun evaluate_plusStringConcat() = runTest {
        val expr = binOp(strLit("Hello, "), BinaryOperator.PLUS, strLit("world"))
        assertEquals("Hello, world", evaluator.evaluate(expr, context))
    }

    @Test
    fun evaluate_plusStringAndInt() = runTest {
        val expr = binOp(strLit("Count: "), BinaryOperator.PLUS, intLit(5))
        assertEquals("Count: 5", evaluator.evaluate(expr, context))
    }

    @Test
    fun evaluate_minusIntegers() = runTest {
        val expr = binOp(intLit(10), BinaryOperator.MINUS, intLit(3))
        assertEquals(7, evaluator.evaluate(expr, context))
    }

    @Test
    fun evaluate_multiplyIntegers() = runTest {
        val expr = binOp(intLit(4), BinaryOperator.STAR, intLit(5))
        assertEquals(20, evaluator.evaluate(expr, context))
    }

    @Test
    fun evaluate_divisionAlwaysDouble() = runTest {
        val expr = binOp(intLit(10), BinaryOperator.SLASH, intLit(3))
        val result = evaluator.evaluate(expr, context) as Double
        assertTrue(result > 3.33 && result < 3.34)
    }

    @Test
    fun evaluate_divisionByZeroThrows() = runTest {
        val expr = binOp(intLit(10), BinaryOperator.SLASH, intLit(0))
        assertFailsWith<RuntimeError.General> {
            evaluator.evaluate(expr, context)
        }
    }

    // ==================== Binary Operations - Comparison ====================

    @Test
    fun evaluate_equality() = runTest {
        val eqTrue = binOp(intLit(5), BinaryOperator.EQ, intLit(5))
        val eqFalse = binOp(intLit(5), BinaryOperator.EQ, intLit(3))
        assertEquals(true, evaluator.evaluate(eqTrue, context))
        assertEquals(false, evaluator.evaluate(eqFalse, context))
    }

    @Test
    fun evaluate_notEqual() = runTest {
        val neq = binOp(strLit("a"), BinaryOperator.NEQ, strLit("b"))
        assertEquals(true, evaluator.evaluate(neq, context))
    }

    @Test
    fun evaluate_lessThan() = runTest {
        val lt = binOp(intLit(3), BinaryOperator.LT, intLit(5))
        assertEquals(true, evaluator.evaluate(lt, context))
    }

    @Test
    fun evaluate_greaterThanOrEqual() = runTest {
        val gte = binOp(intLit(5), BinaryOperator.GTE, intLit(5))
        assertEquals(true, evaluator.evaluate(gte, context))
    }

    // ==================== Binary Operations - Logical ====================

    @Test
    fun evaluate_andBothTrue() = runTest {
        val expr = binOp(boolLit(true), BinaryOperator.AND, boolLit(true))
        assertEquals(true, evaluator.evaluate(expr, context))
    }

    @Test
    fun evaluate_andOneFalse() = runTest {
        val expr = binOp(boolLit(true), BinaryOperator.AND, boolLit(false))
        assertEquals(false, evaluator.evaluate(expr, context))
    }

    @Test
    fun evaluate_orOneFalse() = runTest {
        val expr = binOp(boolLit(false), BinaryOperator.OR, boolLit(true))
        assertEquals(true, evaluator.evaluate(expr, context))
    }

    // ==================== Unary Operations ====================

    @Test
    fun evaluate_notTrue() = runTest {
        val expr = AvuAstNode.Expression.UnaryOp(UnaryOperator.NOT, boolLit(true), loc)
        assertEquals(false, evaluator.evaluate(expr, context))
    }

    @Test
    fun evaluate_negateInt() = runTest {
        val expr = AvuAstNode.Expression.UnaryOp(UnaryOperator.NEGATE, intLit(5), loc)
        assertEquals(-5, evaluator.evaluate(expr, context))
    }

    @Test
    fun evaluate_negateDouble() = runTest {
        val expr = AvuAstNode.Expression.UnaryOp(UnaryOperator.NEGATE, floatLit(3.14), loc)
        assertEquals(-3.14, evaluator.evaluate(expr, context))
    }

    // ==================== Boolean Coercion ====================

    @Test
    fun toBooleanValue_falsyCases() {
        assertFalse(evaluator.toBooleanValue(null))
        assertFalse(evaluator.toBooleanValue(false))
        assertFalse(evaluator.toBooleanValue(0))
        assertFalse(evaluator.toBooleanValue(0.0))
        assertFalse(evaluator.toBooleanValue(""))
    }

    @Test
    fun toBooleanValue_truthyCases() {
        assertTrue(evaluator.toBooleanValue(true))
        assertTrue(evaluator.toBooleanValue(1))
        assertTrue(evaluator.toBooleanValue(42))
        assertTrue(evaluator.toBooleanValue(3.14))
        assertTrue(evaluator.toBooleanValue("hello"))
    }

    // ==================== Member Access + Call Expression ====================

    @Test
    fun evaluate_memberAccess_createsCallable() = runTest {
        // screen.contains -> BuiltInCallable("screen", "contains")
        val target = AvuAstNode.Expression.Identifier("screen", loc)
        val memberAccess = AvuAstNode.Expression.MemberAccess(target, "contains", loc)
        val result = evaluator.evaluate(memberAccess, context)
        assertTrue(result is BuiltInCallable)
        assertEquals("screen", (result as BuiltInCallable).target)
        assertEquals("contains", result.method)
    }

    @Test
    fun evaluate_callExpression_dispatchesQRY() = runTest {
        dispatcher.onQuery("screen_contains", true)
        val target = AvuAstNode.Expression.Identifier("screen", loc)
        val memberAccess = AvuAstNode.Expression.MemberAccess(target, "contains", loc)
        val call = AvuAstNode.Expression.CallExpression(
            callee = memberAccess,
            arguments = listOf(strLit("Welcome")),
            location = loc
        )
        val result = evaluator.evaluate(call, context)
        assertEquals(true, result)
        // Verify QRY was dispatched
        assertTrue(dispatcher.dispatched.any { it.first == "QRY" })
    }

    // ==================== Grouped Expression ====================

    @Test
    fun evaluate_grouped() = runTest {
        val inner = binOp(intLit(2), BinaryOperator.PLUS, intLit(3))
        val grouped = AvuAstNode.Expression.Grouped(inner, loc)
        assertEquals(5, evaluator.evaluate(grouped, context))
    }

    // ==================== Type Error Cases ====================

    @Test
    fun evaluate_subtractStringsThrows() = runTest {
        val expr = binOp(strLit("hello"), BinaryOperator.MINUS, strLit("world"))
        assertFailsWith<RuntimeError.TypeError> {
            evaluator.evaluate(expr, context)
        }
    }

    @Test
    fun evaluate_comparisonNonNumericThrows() = runTest {
        val expr = binOp(strLit("a"), BinaryOperator.LT, strLit("b"))
        assertFailsWith<RuntimeError.TypeError> {
            evaluator.evaluate(expr, context)
        }
    }

    // ==================== Helpers ====================

    private fun intLit(v: Int) = AvuAstNode.Expression.IntLiteral(v, loc)
    private fun floatLit(v: Double) = AvuAstNode.Expression.FloatLiteral(v, loc)
    private fun strLit(v: String) = AvuAstNode.Expression.StringLiteral(v, loc)
    private fun boolLit(v: Boolean) = AvuAstNode.Expression.BooleanLiteral(v, loc)
    private fun binOp(left: AvuAstNode.Expression, op: BinaryOperator, right: AvuAstNode.Expression) =
        AvuAstNode.Expression.BinaryOp(left, op, right, loc)
}
