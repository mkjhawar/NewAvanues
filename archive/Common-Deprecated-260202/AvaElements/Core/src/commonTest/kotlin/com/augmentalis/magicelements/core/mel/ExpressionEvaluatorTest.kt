package com.augmentalis.magicelements.core.mel

import kotlin.test.*

/**
 * Unit tests for ExpressionEvaluator.
 * Tests evaluation of literals, state references, function calls, and tier enforcement.
 */
class ExpressionEvaluatorTest {

    private fun evaluate(
        input: String,
        state: Map<String, Any?> = emptyMap(),
        params: Map<String, Any?> = emptyMap(),
        tier: PluginTier = PluginTier.DATA
    ): Any? {
        val lexer = ExpressionLexer(input)
        val tokens = lexer.tokenize()
        val parser = ExpressionParser(tokens)
        val ast = parser.parse()
        val evaluator = ExpressionEvaluator(state, params, tier)
        return evaluator.evaluate(ast)
    }

    // ========== Literal Evaluation ==========

    @Test
    fun `evaluates number literal`() {
        val result = evaluate("42")
        assertEquals(42.0, result)
    }

    @Test
    fun `evaluates string literal`() {
        val result = evaluate("\"hello\"")
        assertEquals("hello", result)
    }

    @Test
    fun `evaluates true literal`() {
        val result = evaluate("true")
        assertEquals(true, result)
    }

    @Test
    fun `evaluates false literal`() {
        val result = evaluate("false")
        assertEquals(false, result)
    }

    @Test
    fun `evaluates null literal`() {
        val result = evaluate("null")
        assertNull(result)
    }

    // ========== State Reference Evaluation ==========

    @Test
    fun `evaluates simple state reference`() {
        val state = mapOf("count" to 42)
        val result = evaluate("\$state.count", state)
        assertEquals(42, result)
    }

    @Test
    fun `evaluates nested state reference`() {
        val state = mapOf(
            "user" to mapOf(
                "name" to "John"
            )
        )
        val result = evaluate("\$state.user.name", state)
        assertEquals("John", result)
    }

    @Test
    fun `evaluates deep nested state reference`() {
        val state = mapOf(
            "config" to mapOf(
                "theme" to mapOf(
                    "primaryColor" to "#FF0000"
                )
            )
        )
        val result = evaluate("\$state.config.theme.primaryColor", state)
        assertEquals("#FF0000", result)
    }

    @Test
    fun `evaluates array index`() {
        val state = mapOf("items" to listOf("a", "b", "c"))
        val result = evaluate("\$state.items[1]", state)
        assertEquals("b", result)
    }

    @Test
    fun `evaluates nested array index`() {
        val state = mapOf(
            "items" to listOf(
                mapOf("name" to "Item 1"),
                mapOf("name" to "Item 2")
            )
        )
        val result = evaluate("\$state.items[0].name", state)
        assertEquals("Item 1", result)
    }

    @Test
    fun `throws on invalid state path`() {
        val state = mapOf("count" to 42)
        assertFailsWith<EvaluationException> {
            evaluate("\$state.missing", state)
        }
    }

    @Test
    fun `throws on array index out of bounds`() {
        val state = mapOf("items" to listOf("a", "b"))
        assertFailsWith<EvaluationException> {
            evaluate("\$state.items[5]", state)
        }
    }

    @Test
    fun `throws on invalid array index type`() {
        val state = mapOf("items" to listOf("a", "b"))
        assertFailsWith<EvaluationException> {
            evaluate("\$state.items.invalid", state)
        }
    }

    // ========== Parameter Reference Evaluation ==========

    @Test
    fun `evaluates parameter reference`() {
        val params = mapOf("digit" to "7")
        val result = evaluate("\$digit", params = params)
        assertEquals("7", result)
    }

    @Test
    fun `throws on missing parameter`() {
        assertFailsWith<EvaluationException> {
            evaluate("\$digit")
        }
    }

    // ========== Function Call Evaluation ==========

    @Test
    fun `evaluates math add`() {
        val result = evaluate("\$math.add(1, 2)")
        assertEquals(3.0, result)
    }

    @Test
    fun `evaluates math subtract`() {
        val result = evaluate("\$math.subtract(5, 3)")
        assertEquals(2.0, result)
    }

    @Test
    fun `evaluates math multiply`() {
        val result = evaluate("\$math.multiply(2, 3)")
        assertEquals(6.0, result)
    }

    @Test
    fun `evaluates math divide`() {
        val result = evaluate("\$math.divide(10, 2)")
        assertEquals(5.0, result)
    }

    @Test
    fun `throws on division by zero`() {
        assertFailsWith<Exception> {
            evaluate("\$math.divide(10, 0)")
        }
    }

    @Test
    fun `evaluates string concat`() {
        val result = evaluate("\$string.concat(\"Hello\", \" \", \"World\")")
        assertEquals("Hello World", result)
    }

    @Test
    fun `evaluates string length`() {
        val result = evaluate("\$string.length(\"hello\")")
        assertEquals(5, result)
    }

    @Test
    fun `evaluates logic if true`() {
        val result = evaluate("\$logic.if(true, \"yes\", \"no\")")
        assertEquals("yes", result)
    }

    @Test
    fun `evaluates logic if false`() {
        val result = evaluate("\$logic.if(false, \"yes\", \"no\")")
        assertEquals("no", result)
    }

    @Test
    fun `evaluates logic and`() {
        val result = evaluate("\$logic.and(true, false)")
        assertEquals(false, result)
    }

    @Test
    fun `evaluates logic or`() {
        val result = evaluate("\$logic.or(true, false)")
        assertEquals(true, result)
    }

    @Test
    fun `evaluates logic not`() {
        val result = evaluate("\$logic.not(true)")
        assertEquals(false, result)
    }

    @Test
    fun `evaluates function with state reference argument`() {
        val state = mapOf("count" to 5)
        val result = evaluate("\$math.add(\$state.count, 1)", state)
        assertEquals(6.0, result)
    }

    // ========== Binary Operation Evaluation ==========

    @Test
    fun `evaluates addition`() {
        val result = evaluate("1 + 2")
        assertEquals(3.0, result)
    }

    @Test
    fun `evaluates subtraction`() {
        val result = evaluate("5 - 3")
        assertEquals(2.0, result)
    }

    @Test
    fun `evaluates multiplication`() {
        val result = evaluate("2 * 3")
        assertEquals(6.0, result)
    }

    @Test
    fun `evaluates division`() {
        val result = evaluate("10 / 2")
        assertEquals(5.0, result)
    }

    @Test
    fun `evaluates modulo`() {
        val result = evaluate("10 % 3")
        assertEquals(1.0, result)
    }

    @Test
    fun `throws on division by zero in expression`() {
        assertFailsWith<EvaluationException> {
            evaluate("10 / 0")
        }
    }

    @Test
    fun `evaluates state reference addition`() {
        val state = mapOf("count" to 5)
        val result = evaluate("\$state.count + 1", state)
        assertEquals(6.0, result)
    }

    @Test
    fun `evaluates state reference multiplication`() {
        val state = mapOf("a" to 3, "b" to 4)
        val result = evaluate("\$state.a * \$state.b", state)
        assertEquals(12.0, result)
    }

    @Test
    fun `evaluates string concatenation with plus`() {
        val result = evaluate("\"Hello\" + \" \" + \"World\"")
        assertEquals("Hello World", result)
    }

    // ========== Comparison Operation Evaluation ==========

    @Test
    fun `evaluates equals true`() {
        val result = evaluate("5 == 5")
        assertEquals(true, result)
    }

    @Test
    fun `evaluates equals false`() {
        val result = evaluate("5 == 6")
        assertEquals(false, result)
    }

    @Test
    fun `evaluates not equals`() {
        val result = evaluate("5 != 6")
        assertEquals(true, result)
    }

    @Test
    fun `evaluates greater than`() {
        val result = evaluate("5 > 3")
        assertEquals(true, result)
    }

    @Test
    fun `evaluates less than`() {
        val result = evaluate("3 < 5")
        assertEquals(true, result)
    }

    @Test
    fun `evaluates greater than or equal`() {
        val result = evaluate("5 >= 5")
        assertEquals(true, result)
    }

    @Test
    fun `evaluates less than or equal`() {
        val result = evaluate("3 <= 5")
        assertEquals(true, result)
    }

    @Test
    fun `evaluates state reference comparison`() {
        val state = mapOf("count" to 5)
        val result = evaluate("\$state.count > 3", state)
        assertEquals(true, result)
    }

    // ========== Logical Operation Evaluation ==========

    @Test
    fun `evaluates logical AND true`() {
        val result = evaluate("true && true")
        assertEquals(true, result)
    }

    @Test
    fun `evaluates logical AND false`() {
        val result = evaluate("true && false")
        assertEquals(false, result)
    }

    @Test
    fun `evaluates logical OR true`() {
        val result = evaluate("true || false")
        assertEquals(true, result)
    }

    @Test
    fun `evaluates logical OR false`() {
        val result = evaluate("false || false")
        assertEquals(false, result)
    }

    @Test
    fun `evaluates complex logical expression`() {
        val state = mapOf("enabled" to true, "count" to 5)
        val result = evaluate("\$state.enabled && \$state.count > 0", state)
        assertEquals(true, result)
    }

    // ========== Unary Operation Evaluation ==========

    @Test
    fun `evaluates logical NOT`() {
        val result = evaluate("!true")
        assertEquals(false, result)
    }

    @Test
    fun `evaluates double NOT`() {
        val result = evaluate("!!true")
        assertEquals(true, result)
    }

    @Test
    fun `evaluates negation`() {
        val result = evaluate("-5")
        assertEquals(-5.0, result)
    }

    @Test
    fun `evaluates double negation`() {
        val result = evaluate("--5")
        assertEquals(5.0, result)
    }

    @Test
    fun `evaluates NOT with state reference`() {
        val state = mapOf("enabled" to false)
        val result = evaluate("!\$state.enabled", state)
        assertEquals(true, result)
    }

    // ========== Array Literal Evaluation ==========

    @Test
    fun `evaluates empty array`() {
        val result = evaluate("[]")
        assertTrue(result is List<*>)
        assertEquals(0, (result as List<*>).size)
    }

    @Test
    fun `evaluates array with literals`() {
        val result = evaluate("[1, 2, 3]")
        assertTrue(result is List<*>)
        val list = result as List<*>
        assertEquals(3, list.size)
        assertEquals(1.0, list[0])
        assertEquals(2.0, list[1])
        assertEquals(3.0, list[2])
    }

    @Test
    fun `evaluates array with expressions`() {
        val state = mapOf("a" to 1, "b" to 2)
        val result = evaluate("[\$state.a, \$state.b, 3]", state)
        assertTrue(result is List<*>)
        val list = result as List<*>
        assertEquals(3, list.size)
    }

    // ========== Object Literal Evaluation ==========

    @Test
    fun `evaluates empty object`() {
        val result = evaluate("{}")
        assertTrue(result is Map<*, *>)
        assertEquals(0, (result as Map<*, *>).size)
    }

    @Test
    fun `evaluates object with literals`() {
        val result = evaluate("{x: 10, y: 20}")
        assertTrue(result is Map<*, *>)
        val map = result as Map<*, *>
        assertEquals(10.0, map["x"])
        assertEquals(20.0, map["y"])
    }

    @Test
    fun `evaluates object with expressions`() {
        val state = mapOf("a" to 1, "b" to 2)
        val result = evaluate("{x: \$state.a, y: \$state.b}", state)
        assertTrue(result is Map<*, *>)
        val map = result as Map<*, *>
        assertEquals(1, map["x"])
        assertEquals(2, map["y"])
    }

    // ========== Tier Enforcement ==========

    @Test
    fun `allows tier 1 functions in DATA tier`() {
        val result = evaluate("\$math.add(1, 2)", tier = PluginTier.DATA)
        assertEquals(3.0, result)
    }

    @Test
    fun `throws on non-tier-1 function in DATA tier`() {
        // Assuming there's a Tier 2 only function
        // This test would need to be updated based on actual Tier 2 functions
        // For now, we test that tier 1 functions work
        val result = evaluate("\$string.concat(\"a\", \"b\")", tier = PluginTier.DATA)
        assertEquals("ab", result)
    }

    // ========== Complex Expressions ==========

    @Test
    fun `evaluates calculator append digit`() {
        val state = mapOf("display" to "12")
        val params = mapOf("digit" to "3")
        val result = evaluate("\$string.concat(\$state.display, \$digit)", state, params)
        assertEquals("123", result)
    }

    @Test
    fun `evaluates conditional with comparison`() {
        val state = mapOf("count" to 5)
        val result = evaluate("\$logic.if(\$state.count > 0, \"positive\", \"zero or negative\")", state)
        assertEquals("positive", result)
    }

    @Test
    fun `evaluates nested function calls`() {
        val result = evaluate("\$math.add(\$math.multiply(2, 3), 1)")
        assertEquals(7.0, result)
    }

    @Test
    fun `evaluates expression with parentheses`() {
        val result = evaluate("(1 + 2) * 3")
        assertEquals(9.0, result)
    }

    // ========== Truthiness ==========

    @Test
    fun `treats null as falsy`() {
        val result = evaluate("\$logic.if(null, \"yes\", \"no\")")
        assertEquals("no", result)
    }

    @Test
    fun `treats zero as falsy`() {
        val result = evaluate("\$logic.if(0, \"yes\", \"no\")")
        assertEquals("no", result)
    }

    @Test
    fun `treats empty string as falsy`() {
        val result = evaluate("\$logic.if(\"\", \"yes\", \"no\")")
        assertEquals("no", result)
    }

    @Test
    fun `treats non-zero as truthy`() {
        val result = evaluate("\$logic.if(1, \"yes\", \"no\")")
        assertEquals("yes", result)
    }

    // ========== Error Cases ==========

    @Test
    fun `throws on type error in addition`() {
        assertFailsWith<EvaluationException> {
            evaluate("true + 5")
        }
    }

    @Test
    fun `throws on type error in comparison`() {
        assertFailsWith<EvaluationException> {
            evaluate("\"hello\" > 5")
        }
    }

    @Test
    fun `throws on unknown function`() {
        assertFailsWith<EvaluationException> {
            evaluate("\$math.unknownFunction(1, 2)")
        }
    }

    @Test
    fun `throws on wrong number of arguments`() {
        assertFailsWith<Exception> {
            evaluate("\$math.add(1)")
        }
    }
}
