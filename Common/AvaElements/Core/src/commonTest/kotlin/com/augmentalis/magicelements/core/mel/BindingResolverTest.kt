package com.augmentalis.magicelements.core.mel

import kotlinx.serialization.json.*
import kotlin.test.*

/**
 * Unit tests for BindingResolver.
 * Tests simple bindings, expression bindings, nested bindings, and error handling.
 */
class BindingResolverTest {

    // ========== Simple Bindings ==========

    @Test
    fun `resolves simple state reference`() {
        val state = createState("display" to "42")
        val parser = createParser()
        val resolver = BindingResolver(state, parser)

        val result = resolver.resolveExpression("\$state.display")

        assertEquals("42", result)
    }

    @Test
    fun `resolves nested state reference`() {
        val schema = StateSchema.fromDefaults(mapOf(
            "user" to JsonObject(mapOf(
                "name" to JsonPrimitive("John")
            ))
        ))
        val state = PluginState(schema)
        val parser = createParser()
        val resolver = BindingResolver(state, parser)

        val result = resolver.resolveExpression("\$state.user.name")

        assertEquals("John", result)
    }

    @Test
    fun `resolves number state reference`() {
        val state = createState("count" to 42)
        val parser = createParser()
        val resolver = BindingResolver(state, parser)

        val result = resolver.resolveExpression("\$state.count")

        assertEquals(42, result)
    }

    // ========== Expression Bindings ==========

    @Test
    fun `resolves arithmetic expression`() {
        val state = createState("count" to 5)
        val parser = createParser()
        val resolver = BindingResolver(state, parser)

        val result = resolver.resolveExpression("\$state.count + 1")

        assertEquals(6.0, result)
    }

    @Test
    fun `resolves function call`() {
        val state = createState("a" to 2, "b" to 3)
        val parser = createParser()
        val resolver = BindingResolver(state, parser)

        val result = resolver.resolveExpression("\$math.add(\$state.a, \$state.b)")

        assertEquals(5.0, result)
    }

    @Test
    fun `resolves comparison expression`() {
        val state = createState("count" to 5)
        val parser = createParser()
        val resolver = BindingResolver(state, parser)

        val result = resolver.resolveExpression("\$state.count > 3")

        assertEquals(true, result)
    }

    @Test
    fun `resolves logical expression`() {
        val state = createState("enabled" to true, "count" to 5)
        val parser = createParser()
        val resolver = BindingResolver(state, parser)

        val result = resolver.resolveExpression("\$state.enabled && \$state.count > 0")

        assertEquals(true, result)
    }

    // ========== Node Resolution ==========

    @Test
    fun `resolves node with single binding`() {
        val state = createState("display" to "42")
        val parser = createParser()
        val resolver = BindingResolver(state, parser)

        val node = UINode(
            type = "Text",
            props = mapOf("fontSize" to JsonPrimitive(48)),
            bindings = mapOf("value" to "\$state.display")
        )

        val resolved = resolver.resolve(node)

        assertEquals(JsonPrimitive(48), resolved["fontSize"])
        assertEquals(JsonPrimitive("42"), resolved["value"])
    }

    @Test
    fun `resolves node with multiple bindings`() {
        val state = createState("x" to 10, "y" to 20)
        val parser = createParser()
        val resolver = BindingResolver(state, parser)

        val node = UINode(
            type = "Box",
            bindings = mapOf(
                "left" to "\$state.x",
                "top" to "\$state.y"
            )
        )

        val resolved = resolver.resolve(node)

        assertEquals(JsonPrimitive(10), resolved["left"])
        assertEquals(JsonPrimitive(20), resolved["top"])
    }

    @Test
    fun `bindings override static props`() {
        val state = createState("count" to 42)
        val parser = createParser()
        val resolver = BindingResolver(state, parser)

        val node = UINode(
            type = "Text",
            props = mapOf("value" to JsonPrimitive("static")),
            bindings = mapOf("value" to "\$state.count")
        )

        val resolved = resolver.resolve(node)

        assertEquals(JsonPrimitive(42.0), resolved["value"])
    }

    // ========== Children Resolution ==========

    @Test
    fun `resolves children recursively`() {
        val state = createState("a" to "A", "b" to "B")
        val parser = createParser()
        val resolver = BindingResolver(state, parser)

        val child1 = UINode(
            type = "Text",
            bindings = mapOf("value" to "\$state.a")
        )
        val child2 = UINode(
            type = "Text",
            bindings = mapOf("value" to "\$state.b")
        )
        val parent = UINode(
            type = "Column",
            children = listOf(child1, child2)
        )

        val resolved = resolver.resolveChildren(listOf(parent))

        assertNotNull(resolved)
        assertEquals(2, resolved[0].children?.size)
        assertEquals(JsonPrimitive("A"), resolved[0].children?.get(0)?.props?.get("value"))
        assertEquals(JsonPrimitive("B"), resolved[0].children?.get(1)?.props?.get("value"))
    }

    @Test
    fun `resolves deeply nested children`() {
        val state = createState("value" to "test")
        val parser = createParser()
        val resolver = BindingResolver(state, parser)

        val deepChild = UINode(
            type = "Text",
            bindings = mapOf("value" to "\$state.value")
        )
        val child = UINode(type = "Column", children = listOf(deepChild))
        val root = UINode(type = "Column", children = listOf(child))

        val resolved = resolver.resolveChildren(listOf(root))

        val deepResolved = resolved?.get(0)?.children?.get(0)?.children?.get(0)
        assertEquals(JsonPrimitive("test"), deepResolved?.props?.get("value"))
    }

    // ========== Binding Detection ==========

    @Test
    fun `hasBindings detects state reference`() {
        val state = createState("count" to 0)
        val parser = createParser()
        val resolver = BindingResolver(state, parser)

        assertTrue(resolver.hasBindings("\$state.count"))
        assertFalse(resolver.hasBindings("42"))
    }

    @Test
    fun `hasBindings detects function calls`() {
        val state = createState("count" to 0)
        val parser = createParser()
        val resolver = BindingResolver(state, parser)

        assertTrue(resolver.hasBindings("\$math.add(1, 2)"))
        assertTrue(resolver.hasBindings("\$string.concat(\"a\", \"b\")"))
    }

    @Test
    fun `isBinding static method detects bindings`() {
        assertTrue(BindingResolver.isBinding("\$state.count"))
        assertTrue(BindingResolver.isBinding("\$math.add(1, 2)"))
        assertTrue(BindingResolver.isBinding("\$string.concat(\"a\", \"b\")"))
        assertFalse(BindingResolver.isBinding("42"))
        assertFalse(BindingResolver.isBinding("\"static\""))
    }

    // ========== State Path Extraction ==========

    @Test
    fun `extracts single state path`() {
        val state = createState("count" to 0)
        val parser = createParser()
        val resolver = BindingResolver(state, parser)

        val paths = resolver.extractStatePaths("\$state.count")

        assertEquals(1, paths.size)
        assertTrue(paths.contains("count"))
    }

    @Test
    fun `extracts nested state path`() {
        val state = createState("count" to 0)
        val parser = createParser()
        val resolver = BindingResolver(state, parser)

        val paths = resolver.extractStatePaths("\$state.user.name")

        assertEquals(1, paths.size)
        assertTrue(paths.contains("user.name"))
    }

    @Test
    fun `extracts multiple state paths from expression`() {
        val state = createState("count" to 0)
        val parser = createParser()
        val resolver = BindingResolver(state, parser)

        val paths = resolver.extractStatePaths("\$state.a + \$state.b")

        assertEquals(2, paths.size)
        assertTrue(paths.contains("a"))
        assertTrue(paths.contains("b"))
    }

    @Test
    fun `extracts state paths from complex expression`() {
        val state = createState("count" to 0)
        val parser = createParser()
        val resolver = BindingResolver(state, parser)

        val paths = resolver.extractStatePaths("\$logic.if(\$state.enabled && \$state.count > 0, \$state.display, \"0\")")

        assertEquals(3, paths.size)
        assertTrue(paths.contains("enabled"))
        assertTrue(paths.contains("count"))
        assertTrue(paths.contains("display"))
    }

    // ========== Error Handling ==========

    @Test
    fun `handles missing state gracefully`() {
        val state = createState("count" to 0)
        val parser = createParser()
        val resolver = BindingResolver(state, parser)

        val node = UINode(
            type = "Text",
            bindings = mapOf("value" to "\$state.missing")
        )

        val resolved = resolver.resolve(node)

        // Should not throw, should use JsonNull or keep original
        assertTrue(resolved.containsKey("value"))
    }

    @Test
    fun `handles invalid expression gracefully`() {
        val state = createState("count" to 0)
        val parser = createParser()
        val resolver = BindingResolver(state, parser)

        val node = UINode(
            type = "Text",
            bindings = mapOf("value" to "\$state.count +")
        )

        // Should not crash, but may log warning
        val resolved = resolver.resolve(node)
        assertTrue(resolved.containsKey("value"))
    }

    // ========== Type Conversion ==========

    @Test
    fun `converts number to JsonPrimitive`() {
        val state = createState("count" to 42)
        val parser = createParser()
        val resolver = BindingResolver(state, parser)

        val result = resolver.resolveExpression("\$state.count")
        val node = UINode(
            type = "Text",
            bindings = mapOf("value" to "\$state.count")
        )
        val resolved = resolver.resolve(node)

        assertTrue(resolved["value"] is JsonPrimitive)
    }

    @Test
    fun `converts string to JsonPrimitive`() {
        val state = createState("display" to "test")
        val parser = createParser()
        val resolver = BindingResolver(state, parser)

        val node = UINode(
            type = "Text",
            bindings = mapOf("value" to "\$state.display")
        )
        val resolved = resolver.resolve(node)

        assertTrue(resolved["value"] is JsonPrimitive)
        assertEquals("test", (resolved["value"] as JsonPrimitive).content)
    }

    @Test
    fun `converts boolean to JsonPrimitive`() {
        val state = createState("enabled" to true)
        val parser = createParser()
        val resolver = BindingResolver(state, parser)

        val node = UINode(
            type = "Switch",
            bindings = mapOf("value" to "\$state.enabled")
        )
        val resolved = resolver.resolve(node)

        assertTrue(resolved["value"] is JsonPrimitive)
    }

    // ========== Complex Use Cases ==========

    @Test
    fun `resolves calculator display binding`() {
        val state = createState("display" to "123")
        val parser = createParser()
        val resolver = BindingResolver(state, parser)

        val node = UINode(
            type = "Text",
            props = mapOf("fontSize" to JsonPrimitive(48)),
            bindings = mapOf("value" to "\$state.display")
        )

        val resolved = resolver.resolve(node)

        assertEquals(JsonPrimitive("123"), resolved["value"])
    }

    @Test
    fun `resolves conditional display`() {
        val state = createState("count" to 5, "display" to "Five")
        val parser = createParser()
        val resolver = BindingResolver(state, parser)

        val node = UINode(
            type = "Text",
            bindings = mapOf("value" to "\$logic.if(\$state.count > 0, \$state.display, \"Zero\")")
        )

        val resolved = resolver.resolve(node)

        assertEquals(JsonPrimitive("Five"), resolved["value"])
    }

    // ========== Helper Methods ==========

    private fun createState(vararg pairs: Pair<String, Any>): PluginState {
        val defaults = pairs.associate { (key, value) ->
            key to when (value) {
                is Int -> JsonPrimitive(value)
                is String -> JsonPrimitive(value)
                is Boolean -> JsonPrimitive(value)
                else -> throw IllegalArgumentException("Unsupported type: ${value::class}")
            }
        }
        val schema = StateSchema.fromDefaults(defaults)
        return PluginState(schema)
    }

    private fun createParser(): ExpressionParser {
        // Return a parser that can be reused
        // In actual implementation, we'd need to create a new parser for each expression
        return ExpressionParser(emptyList())
    }
}
