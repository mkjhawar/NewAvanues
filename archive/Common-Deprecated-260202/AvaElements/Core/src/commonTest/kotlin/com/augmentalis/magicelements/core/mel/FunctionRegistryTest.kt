package com.augmentalis.magicelements.core.mel

import kotlin.test.*

/**
 * Unit tests for FunctionRegistry (DefaultFunctionRegistry).
 * Tests math, string, array, logic functions, and tier whitelist enforcement.
 */
class FunctionRegistryTest {

    private val registry = FunctionRegistry.default()

    // ========== Math Functions ==========

    @Test
    fun `math add function`() {
        val result = registry.execute("math.add", listOf(1.0, 2.0))
        assertEquals(3.0, result)
    }

    @Test
    fun `math subtract function`() {
        val result = registry.execute("math.subtract", listOf(5.0, 3.0))
        assertEquals(2.0, result)
    }

    @Test
    fun `math multiply function`() {
        val result = registry.execute("math.multiply", listOf(2.0, 3.0))
        assertEquals(6.0, result)
    }

    @Test
    fun `math divide function`() {
        val result = registry.execute("math.divide", listOf(10.0, 2.0))
        assertEquals(5.0, result)
    }

    @Test
    fun `math divide throws on division by zero`() {
        assertFailsWith<Exception> {
            registry.execute("math.divide", listOf(10.0, 0.0))
        }
    }

    @Test
    fun `math functions require correct argument count`() {
        assertFailsWith<Exception> {
            registry.execute("math.add", listOf(1.0))
        }
    }

    // ========== String Functions ==========

    @Test
    fun `string concat function`() {
        val result = registry.execute("string.concat", listOf("Hello", " ", "World"))
        assertEquals("Hello World", result)
    }

    @Test
    fun `string concat with nulls`() {
        val result = registry.execute("string.concat", listOf("Hello", null, "World"))
        assertEquals("HelloWorld", result)
    }

    @Test
    fun `string length function`() {
        val result = registry.execute("string.length", listOf("hello"))
        assertEquals(5, result)
    }

    @Test
    fun `string length with null`() {
        val result = registry.execute("string.length", listOf(null))
        assertEquals(0, result)
    }

    @Test
    fun `string length requires one argument`() {
        assertFailsWith<Exception> {
            registry.execute("string.length", emptyList())
        }
    }

    // ========== Logic Functions ==========

    @Test
    fun `logic if returns true branch`() {
        val result = registry.execute("logic.if", listOf(true, "yes", "no"))
        assertEquals("yes", result)
    }

    @Test
    fun `logic if returns false branch`() {
        val result = registry.execute("logic.if", listOf(false, "yes", "no"))
        assertEquals("no", result)
    }

    @Test
    fun `logic if with truthy value`() {
        val result = registry.execute("logic.if", listOf(1, "yes", "no"))
        assertEquals("yes", result)
    }

    @Test
    fun `logic if with falsy value`() {
        val result = registry.execute("logic.if", listOf(0, "yes", "no"))
        assertEquals("no", result)
    }

    @Test
    fun `logic if requires three arguments`() {
        assertFailsWith<Exception> {
            registry.execute("logic.if", listOf(true, "yes"))
        }
    }

    @Test
    fun `logic and function`() {
        assertTrue(registry.execute("logic.and", listOf(true, true)) as Boolean)
        assertFalse(registry.execute("logic.and", listOf(true, false)) as Boolean)
        assertFalse(registry.execute("logic.and", listOf(false, false)) as Boolean)
    }

    @Test
    fun `logic or function`() {
        assertTrue(registry.execute("logic.or", listOf(true, false)) as Boolean)
        assertTrue(registry.execute("logic.or", listOf(true, true)) as Boolean)
        assertFalse(registry.execute("logic.or", listOf(false, false)) as Boolean)
    }

    @Test
    fun `logic not function`() {
        assertEquals(false, registry.execute("logic.not", listOf(true)))
        assertEquals(true, registry.execute("logic.not", listOf(false)))
    }

    @Test
    fun `logic not requires one argument`() {
        assertFailsWith<Exception> {
            registry.execute("logic.not", emptyList())
        }
    }

    @Test
    fun `logic equals function`() {
        assertTrue(registry.execute("logic.equals", listOf(5, 5)) as Boolean)
        assertFalse(registry.execute("logic.equals", listOf(5, 6)) as Boolean)
    }

    @Test
    fun `logic equals requires two arguments`() {
        assertFailsWith<Exception> {
            registry.execute("logic.equals", listOf(5))
        }
    }

    // ========== Tier 1 Whitelist ==========

    @Test
    fun `math functions are tier 1`() {
        assertTrue(registry.isTier1Function("math.add"))
        assertTrue(registry.isTier1Function("math.subtract"))
        assertTrue(registry.isTier1Function("math.multiply"))
        assertTrue(registry.isTier1Function("math.divide"))
        assertTrue(registry.isTier1Function("math.mod"))
        assertTrue(registry.isTier1Function("math.abs"))
        assertTrue(registry.isTier1Function("math.round"))
        assertTrue(registry.isTier1Function("math.floor"))
        assertTrue(registry.isTier1Function("math.ceil"))
        assertTrue(registry.isTier1Function("math.min"))
        assertTrue(registry.isTier1Function("math.max"))
    }

    @Test
    fun `string functions are tier 1`() {
        assertTrue(registry.isTier1Function("string.concat"))
        assertTrue(registry.isTier1Function("string.length"))
        assertTrue(registry.isTier1Function("string.substring"))
        assertTrue(registry.isTier1Function("string.uppercase"))
        assertTrue(registry.isTier1Function("string.lowercase"))
        assertTrue(registry.isTier1Function("string.trim"))
        assertTrue(registry.isTier1Function("string.replace"))
        assertTrue(registry.isTier1Function("string.split"))
        assertTrue(registry.isTier1Function("string.join"))
    }

    @Test
    fun `array functions are tier 1`() {
        assertTrue(registry.isTier1Function("array.length"))
        assertTrue(registry.isTier1Function("array.get"))
        assertTrue(registry.isTier1Function("array.first"))
        assertTrue(registry.isTier1Function("array.last"))
        assertTrue(registry.isTier1Function("array.append"))
        assertTrue(registry.isTier1Function("array.prepend"))
        assertTrue(registry.isTier1Function("array.remove"))
        assertTrue(registry.isTier1Function("array.filter"))
        assertTrue(registry.isTier1Function("array.map"))
        assertTrue(registry.isTier1Function("array.sort"))
    }

    @Test
    fun `object functions are tier 1`() {
        assertTrue(registry.isTier1Function("object.get"))
        assertTrue(registry.isTier1Function("object.set"))
        assertTrue(registry.isTier1Function("object.keys"))
        assertTrue(registry.isTier1Function("object.values"))
        assertTrue(registry.isTier1Function("object.merge"))
    }

    @Test
    fun `date functions are tier 1`() {
        assertTrue(registry.isTier1Function("date.now"))
        assertTrue(registry.isTier1Function("date.format"))
        assertTrue(registry.isTier1Function("date.parse"))
        assertTrue(registry.isTier1Function("date.add"))
        assertTrue(registry.isTier1Function("date.subtract"))
        assertTrue(registry.isTier1Function("date.diff"))
    }

    @Test
    fun `logic functions are tier 1`() {
        assertTrue(registry.isTier1Function("logic.if"))
        assertTrue(registry.isTier1Function("logic.and"))
        assertTrue(registry.isTier1Function("logic.or"))
        assertTrue(registry.isTier1Function("logic.not"))
        assertTrue(registry.isTier1Function("logic.equals"))
        assertTrue(registry.isTier1Function("logic.gt"))
        assertTrue(registry.isTier1Function("logic.lt"))
        assertTrue(registry.isTier1Function("logic.gte"))
        assertTrue(registry.isTier1Function("logic.lte"))
    }

    @Test
    fun `unknown functions are not tier 1`() {
        assertFalse(registry.isTier1Function("unknown.function"))
        assertFalse(registry.isTier1Function("system.exit"))
        assertFalse(registry.isTier1Function("network.fetch"))
    }

    // ========== Error Handling ==========

    @Test
    fun `throws on unknown function`() {
        assertFailsWith<EvaluationException> {
            registry.execute("unknown.function", emptyList())
        }
    }

    @Test
    fun `handles type conversions`() {
        val result = registry.execute("math.add", listOf(1, 2))
        assertEquals(3.0, result)
    }

    @Test
    fun `handles string to number conversion in add`() {
        val result = registry.execute("math.add", listOf("5", "3"))
        assertEquals(8.0, result)
    }

    @Test
    fun `throws on invalid type conversion`() {
        assertFailsWith<EvaluationException> {
            registry.execute("math.add", listOf("not a number", "also not"))
        }
    }

    // ========== Truthiness ==========

    @Test
    fun `null is falsy`() {
        val result = registry.execute("logic.if", listOf(null, "yes", "no"))
        assertEquals("no", result)
    }

    @Test
    fun `zero is falsy`() {
        val result = registry.execute("logic.if", listOf(0, "yes", "no"))
        assertEquals("no", result)
    }

    @Test
    fun `empty string is falsy`() {
        val result = registry.execute("logic.if", listOf("", "yes", "no"))
        assertEquals("no", result)
    }

    @Test
    fun `empty list is falsy`() {
        val result = registry.execute("logic.if", listOf(emptyList<Any>(), "yes", "no"))
        assertEquals("no", result)
    }

    @Test
    fun `non-zero number is truthy`() {
        val result = registry.execute("logic.if", listOf(1, "yes", "no"))
        assertEquals("yes", result)
    }

    @Test
    fun `non-empty string is truthy`() {
        val result = registry.execute("logic.if", listOf("hello", "yes", "no"))
        assertEquals("yes", result)
    }

    // ========== Complex Use Cases ==========

    @Test
    fun `nested function calls work`() {
        val result = registry.execute(
            "math.add",
            listOf(
                registry.execute("math.multiply", listOf(2, 3)),
                1
            )
        )
        assertEquals(7.0, result)
    }

    @Test
    fun `string concat with numbers`() {
        val result = registry.execute("string.concat", listOf("Count: ", 42))
        assertEquals("Count: 42", result)
    }

    @Test
    fun `logic and with multiple args`() {
        val result = registry.execute("logic.and", listOf(true, true, true))
        assertTrue(result as Boolean)
    }

    @Test
    fun `logic and with mixed truthiness`() {
        val result = registry.execute("logic.and", listOf(1, "hello", true))
        assertTrue(result as Boolean)
    }

    @Test
    fun `logic or short circuits`() {
        val result = registry.execute("logic.or", listOf(true, false, false))
        assertTrue(result as Boolean)
    }
}
