package com.augmentalis.magicelements.core.mel

import kotlin.test.*

/**
 * Unit tests for ExpressionParser.
 * Tests parsing of state references, function calls, binary operations, and complex expressions.
 */
class ExpressionParserTest {

    private fun parse(input: String): ExpressionNode {
        val lexer = ExpressionLexer(input)
        val tokens = lexer.tokenize()
        val parser = ExpressionParser(tokens)
        return parser.parse()
    }

    // ========== Literals ==========

    @Test
    fun `parses number literal`() {
        val node = parse("42")
        assertTrue(node is ExpressionNode.Literal)
        val literal = (node as ExpressionNode.Literal).value
        assertTrue(literal is LiteralValue.NumberValue)
        assertEquals(42.0, (literal as LiteralValue.NumberValue).value)
    }

    @Test
    fun `parses float literal`() {
        val node = parse("3.14")
        assertTrue(node is ExpressionNode.Literal)
        val literal = (node as ExpressionNode.Literal).value
        assertEquals(3.14, (literal as LiteralValue.NumberValue).value)
    }

    @Test
    fun `parses string literal`() {
        val node = parse("\"hello\"")
        assertTrue(node is ExpressionNode.Literal)
        val literal = (node as ExpressionNode.Literal).value
        assertTrue(literal is LiteralValue.StringValue)
        assertEquals("hello", (literal as LiteralValue.StringValue).value)
    }

    @Test
    fun `parses true literal`() {
        val node = parse("true")
        assertTrue(node is ExpressionNode.Literal)
        val literal = (node as ExpressionNode.Literal).value
        assertTrue(literal is LiteralValue.BooleanValue)
        assertEquals(true, (literal as LiteralValue.BooleanValue).value)
    }

    @Test
    fun `parses false literal`() {
        val node = parse("false")
        assertTrue(node is ExpressionNode.Literal)
        val literal = (node as ExpressionNode.Literal).value
        assertEquals(false, (literal as LiteralValue.BooleanValue).value)
    }

    @Test
    fun `parses null literal`() {
        val node = parse("null")
        assertTrue(node is ExpressionNode.Literal)
        val literal = (node as ExpressionNode.Literal).value
        assertTrue(literal is LiteralValue.NullValue)
    }

    // ========== State References ==========

    @Test
    fun `parses simple state reference`() {
        val node = parse("\$state.display")
        assertTrue(node is ExpressionNode.StateRef)
        assertEquals(listOf("display"), (node as ExpressionNode.StateRef).path)
    }

    @Test
    fun `parses nested state reference`() {
        val node = parse("\$state.user.name")
        assertTrue(node is ExpressionNode.StateRef)
        assertEquals(listOf("user", "name"), (node as ExpressionNode.StateRef).path)
    }

    @Test
    fun `parses deep nested state reference`() {
        val node = parse("\$state.config.theme.primaryColor")
        assertTrue(node is ExpressionNode.StateRef)
        assertEquals(listOf("config", "theme", "primaryColor"), (node as ExpressionNode.StateRef).path)
    }

    @Test
    fun `parses state array index`() {
        val node = parse("\$state.items[0]")
        assertTrue(node is ExpressionNode.StateRef)
        assertEquals(listOf("items", "0"), (node as ExpressionNode.StateRef).path)
    }

    @Test
    fun `parses state nested array index`() {
        val node = parse("\$state.items[0].name")
        assertTrue(node is ExpressionNode.StateRef)
        assertEquals(listOf("items", "0", "name"), (node as ExpressionNode.StateRef).path)
    }

    @Test
    fun `throws on state reference without path`() {
        assertFailsWith<ParserException> {
            parse("\$state")
        }
    }

    // ========== Function Calls ==========

    @Test
    fun `parses function call with no arguments`() {
        val node = parse("\$date.now()")
        assertTrue(node is ExpressionNode.FunctionCall)
        val call = node as ExpressionNode.FunctionCall
        assertEquals("date", call.category)
        assertEquals("now", call.name)
        assertEquals(0, call.args.size)
    }

    @Test
    fun `parses function call with one argument`() {
        val node = parse("\$math.abs(-5)")
        assertTrue(node is ExpressionNode.FunctionCall)
        val call = node as ExpressionNode.FunctionCall
        assertEquals("math", call.category)
        assertEquals("abs", call.name)
        assertEquals(1, call.args.size)
    }

    @Test
    fun `parses function call with two arguments`() {
        val node = parse("\$math.add(1, 2)")
        assertTrue(node is ExpressionNode.FunctionCall)
        val call = node as ExpressionNode.FunctionCall
        assertEquals("math", call.category)
        assertEquals("add", call.name)
        assertEquals(2, call.args.size)
    }

    @Test
    fun `parses function call with state reference argument`() {
        val node = parse("\$math.add(\$state.count, 1)")
        assertTrue(node is ExpressionNode.FunctionCall)
        val call = node as ExpressionNode.FunctionCall
        assertEquals(2, call.args.size)
        assertTrue(call.args[0] is ExpressionNode.StateRef)
        assertTrue(call.args[1] is ExpressionNode.Literal)
    }

    @Test
    fun `parses nested function calls`() {
        val node = parse("\$math.add(\$math.multiply(2, 3), 1)")
        assertTrue(node is ExpressionNode.FunctionCall)
        val call = node as ExpressionNode.FunctionCall
        assertEquals("add", call.name)
        assertTrue(call.args[0] is ExpressionNode.FunctionCall)
        assertTrue(call.args[1] is ExpressionNode.Literal)
    }

    // ========== Parameter References ==========

    @Test
    fun `parses parameter reference`() {
        val node = parse("\$digit")
        assertTrue(node is ExpressionNode.ParamRef)
        assertEquals("digit", (node as ExpressionNode.ParamRef).name)
    }

    // ========== Binary Operations ==========

    @Test
    fun `parses addition`() {
        val node = parse("1 + 2")
        assertTrue(node is ExpressionNode.BinaryOp)
        val op = node as ExpressionNode.BinaryOp
        assertEquals("+", op.op)
        assertTrue(op.left is ExpressionNode.Literal)
        assertTrue(op.right is ExpressionNode.Literal)
    }

    @Test
    fun `parses subtraction`() {
        val node = parse("5 - 3")
        assertTrue(node is ExpressionNode.BinaryOp)
        assertEquals("-", (node as ExpressionNode.BinaryOp).op)
    }

    @Test
    fun `parses multiplication`() {
        val node = parse("2 * 3")
        assertTrue(node is ExpressionNode.BinaryOp)
        assertEquals("*", (node as ExpressionNode.BinaryOp).op)
    }

    @Test
    fun `parses division`() {
        val node = parse("10 / 2")
        assertTrue(node is ExpressionNode.BinaryOp)
        assertEquals("/", (node as ExpressionNode.BinaryOp).op)
    }

    @Test
    fun `parses modulo`() {
        val node = parse("10 % 3")
        assertTrue(node is ExpressionNode.BinaryOp)
        assertEquals("%", (node as ExpressionNode.BinaryOp).op)
    }

    @Test
    fun `parses state reference with addition`() {
        val node = parse("\$state.count + 1")
        assertTrue(node is ExpressionNode.BinaryOp)
        val op = node as ExpressionNode.BinaryOp
        assertEquals("+", op.op)
        assertTrue(op.left is ExpressionNode.StateRef)
        assertTrue(op.right is ExpressionNode.Literal)
    }

    // ========== Comparison Operations ==========

    @Test
    fun `parses equals comparison`() {
        val node = parse("1 == 2")
        assertTrue(node is ExpressionNode.BinaryOp)
        assertEquals("==", (node as ExpressionNode.BinaryOp).op)
    }

    @Test
    fun `parses not equals comparison`() {
        val node = parse("1 != 2")
        assertTrue(node is ExpressionNode.BinaryOp)
        assertEquals("!=", (node as ExpressionNode.BinaryOp).op)
    }

    @Test
    fun `parses greater than comparison`() {
        val node = parse("\$state.count > 5")
        assertTrue(node is ExpressionNode.BinaryOp)
        assertEquals(">", (node as ExpressionNode.BinaryOp).op)
    }

    @Test
    fun `parses less than comparison`() {
        val node = parse("\$state.count < 10")
        assertTrue(node is ExpressionNode.BinaryOp)
        assertEquals("<", (node as ExpressionNode.BinaryOp).op)
    }

    @Test
    fun `parses greater than or equal comparison`() {
        val node = parse("\$state.count >= 5")
        assertTrue(node is ExpressionNode.BinaryOp)
        assertEquals(">=", (node as ExpressionNode.BinaryOp).op)
    }

    @Test
    fun `parses less than or equal comparison`() {
        val node = parse("\$state.count <= 10")
        assertTrue(node is ExpressionNode.BinaryOp)
        assertEquals("<=", (node as ExpressionNode.BinaryOp).op)
    }

    // ========== Logical Operations ==========

    @Test
    fun `parses logical AND`() {
        val node = parse("true && false")
        assertTrue(node is ExpressionNode.BinaryOp)
        assertEquals("&&", (node as ExpressionNode.BinaryOp).op)
    }

    @Test
    fun `parses logical OR`() {
        val node = parse("true || false")
        assertTrue(node is ExpressionNode.BinaryOp)
        assertEquals("||", (node as ExpressionNode.BinaryOp).op)
    }

    @Test
    fun `parses complex logical expression`() {
        val node = parse("\$state.enabled && \$state.count > 0")
        assertTrue(node is ExpressionNode.BinaryOp)
        val op = node as ExpressionNode.BinaryOp
        assertEquals("&&", op.op)
        assertTrue(op.left is ExpressionNode.StateRef)
        assertTrue(op.right is ExpressionNode.BinaryOp)
    }

    // ========== Unary Operations ==========

    @Test
    fun `parses logical NOT`() {
        val node = parse("!\$state.enabled")
        assertTrue(node is ExpressionNode.UnaryOp)
        val op = node as ExpressionNode.UnaryOp
        assertEquals("!", op.op)
        assertTrue(op.operand is ExpressionNode.StateRef)
    }

    @Test
    fun `parses negation`() {
        val node = parse("-5")
        assertTrue(node is ExpressionNode.UnaryOp)
        val op = node as ExpressionNode.UnaryOp
        assertEquals("-", op.op)
        assertTrue(op.operand is ExpressionNode.Literal)
    }

    @Test
    fun `parses double negation`() {
        val node = parse("!!\$state.enabled")
        assertTrue(node is ExpressionNode.UnaryOp)
        val outer = node as ExpressionNode.UnaryOp
        assertEquals("!", outer.op)
        assertTrue(outer.operand is ExpressionNode.UnaryOp)
        val inner = outer.operand as ExpressionNode.UnaryOp
        assertEquals("!", inner.op)
    }

    // ========== Operator Precedence ==========

    @Test
    fun `respects multiplication over addition`() {
        val node = parse("1 + 2 * 3")
        assertTrue(node is ExpressionNode.BinaryOp)
        val add = node as ExpressionNode.BinaryOp
        assertEquals("+", add.op)
        assertTrue(add.right is ExpressionNode.BinaryOp)
        assertEquals("*", (add.right as ExpressionNode.BinaryOp).op)
    }

    @Test
    fun `respects comparison over logical AND`() {
        val node = parse("\$state.a > 5 && \$state.b < 10")
        assertTrue(node is ExpressionNode.BinaryOp)
        val and = node as ExpressionNode.BinaryOp
        assertEquals("&&", and.op)
        assertTrue(and.left is ExpressionNode.BinaryOp)
        assertTrue(and.right is ExpressionNode.BinaryOp)
    }

    @Test
    fun `respects AND over OR`() {
        val node = parse("true || false && true")
        assertTrue(node is ExpressionNode.BinaryOp)
        val or = node as ExpressionNode.BinaryOp
        assertEquals("||", or.op)
        assertTrue(or.right is ExpressionNode.BinaryOp)
    }

    // ========== Parentheses ==========

    @Test
    fun `parses grouped expression`() {
        val node = parse("(1 + 2)")
        assertTrue(node is ExpressionNode.BinaryOp)
        assertEquals("+", (node as ExpressionNode.BinaryOp).op)
    }

    @Test
    fun `respects parentheses precedence`() {
        val node = parse("(1 + 2) * 3")
        assertTrue(node is ExpressionNode.BinaryOp)
        val mul = node as ExpressionNode.BinaryOp
        assertEquals("*", mul.op)
        assertTrue(mul.left is ExpressionNode.BinaryOp)
        assertEquals("+", (mul.left as ExpressionNode.BinaryOp).op)
    }

    // ========== Array Literals ==========

    @Test
    fun `parses empty array`() {
        val node = parse("[]")
        assertTrue(node is ExpressionNode.ArrayLiteral)
        assertEquals(0, (node as ExpressionNode.ArrayLiteral).elements.size)
    }

    @Test
    fun `parses array with one element`() {
        val node = parse("[1]")
        assertTrue(node is ExpressionNode.ArrayLiteral)
        assertEquals(1, (node as ExpressionNode.ArrayLiteral).elements.size)
    }

    @Test
    fun `parses array with multiple elements`() {
        val node = parse("[1, 2, 3]")
        assertTrue(node is ExpressionNode.ArrayLiteral)
        assertEquals(3, (node as ExpressionNode.ArrayLiteral).elements.size)
    }

    @Test
    fun `parses array with mixed types`() {
        val node = parse("[1, \"hello\", true]")
        assertTrue(node is ExpressionNode.ArrayLiteral)
        val array = node as ExpressionNode.ArrayLiteral
        assertEquals(3, array.elements.size)
        assertTrue(array.elements[0] is ExpressionNode.Literal)
        assertTrue(array.elements[1] is ExpressionNode.Literal)
        assertTrue(array.elements[2] is ExpressionNode.Literal)
    }

    @Test
    fun `parses array with expressions`() {
        val node = parse("[\$state.a, \$state.b + 1]")
        assertTrue(node is ExpressionNode.ArrayLiteral)
        val array = node as ExpressionNode.ArrayLiteral
        assertEquals(2, array.elements.size)
        assertTrue(array.elements[0] is ExpressionNode.StateRef)
        assertTrue(array.elements[1] is ExpressionNode.BinaryOp)
    }

    // ========== Object Literals ==========

    @Test
    fun `parses empty object`() {
        val node = parse("{}")
        assertTrue(node is ExpressionNode.ObjectLiteral)
        assertEquals(0, (node as ExpressionNode.ObjectLiteral).properties.size)
    }

    @Test
    fun `parses object with one property`() {
        val node = parse("{x: 10}")
        assertTrue(node is ExpressionNode.ObjectLiteral)
        val obj = node as ExpressionNode.ObjectLiteral
        assertEquals(1, obj.properties.size)
        assertTrue(obj.properties.containsKey("x"))
    }

    @Test
    fun `parses object with multiple properties`() {
        val node = parse("{x: 10, y: 20}")
        assertTrue(node is ExpressionNode.ObjectLiteral)
        val obj = node as ExpressionNode.ObjectLiteral
        assertEquals(2, obj.properties.size)
        assertTrue(obj.properties.containsKey("x"))
        assertTrue(obj.properties.containsKey("y"))
    }

    @Test
    fun `parses object with string keys`() {
        val node = parse("{\"name\": \"John\", \"age\": 30}")
        assertTrue(node is ExpressionNode.ObjectLiteral)
        val obj = node as ExpressionNode.ObjectLiteral
        assertEquals(2, obj.properties.size)
    }

    @Test
    fun `parses object with expression values`() {
        val node = parse("{x: \$state.a, y: \$state.b + 1}")
        assertTrue(node is ExpressionNode.ObjectLiteral)
        val obj = node as ExpressionNode.ObjectLiteral
        assertTrue(obj.properties["x"] is ExpressionNode.StateRef)
        assertTrue(obj.properties["y"] is ExpressionNode.BinaryOp)
    }

    // ========== Complex Expressions ==========

    @Test
    fun `parses calculator append digit expression`() {
        val node = parse("\$string.concat(\$state.display, \$digit)")
        assertTrue(node is ExpressionNode.FunctionCall)
        val call = node as ExpressionNode.FunctionCall
        assertEquals("string", call.category)
        assertEquals("concat", call.name)
        assertEquals(2, call.args.size)
        assertTrue(call.args[0] is ExpressionNode.StateRef)
        assertTrue(call.args[1] is ExpressionNode.ParamRef)
    }

    @Test
    fun `parses conditional logic expression`() {
        val node = parse("\$logic.if(\$state.count > 0, \$state.count, 0)")
        assertTrue(node is ExpressionNode.FunctionCall)
        val call = node as ExpressionNode.FunctionCall
        assertEquals("logic", call.category)
        assertEquals("if", call.name)
        assertEquals(3, call.args.size)
        assertTrue(call.args[0] is ExpressionNode.BinaryOp)
    }

    // ========== Error Cases ==========

    @Test
    fun `throws on unexpected token`() {
        assertFailsWith<ParserException> {
            parse("1 2")
        }
    }

    @Test
    fun `throws on missing closing parenthesis`() {
        assertFailsWith<ParserException> {
            parse("(1 + 2")
        }
    }

    @Test
    fun `throws on missing closing bracket`() {
        assertFailsWith<ParserException> {
            parse("[1, 2")
        }
    }

    @Test
    fun `throws on missing closing brace`() {
        assertFailsWith<ParserException> {
            parse("{x: 10")
        }
    }

    @Test
    fun `throws on function call without parentheses`() {
        assertFailsWith<ParserException> {
            parse("\$math.add")
        }
    }

    @Test
    fun `throws on invalid array index`() {
        assertFailsWith<ParserException> {
            parse("\$state.items[invalid]")
        }
    }
}
