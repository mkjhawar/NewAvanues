package com.augmentalis.magicelements.core.mel

import kotlinx.serialization.json.*
import kotlin.test.*

/**
 * Unit tests for UINode.
 * Tests node creation, binding parsing, event parsing, and tree traversal.
 */
class UINodeTest {

    // ========== Basic Creation ==========

    @Test
    fun `creates simple node`() {
        val node = UINode(
            type = "Text",
            props = mapOf("fontSize" to JsonPrimitive(48))
        )

        assertEquals("Text", node.type)
        assertEquals(1, node.props.size)
        assertEquals(JsonPrimitive(48), node.props["fontSize"])
    }

    @Test
    fun `creates node with bindings`() {
        val node = UINode(
            type = "Text",
            bindings = mapOf("value" to "\$state.display")
        )

        assertEquals(1, node.bindings.size)
        assertEquals("\$state.display", node.bindings["value"])
    }

    @Test
    fun `creates node with events`() {
        val node = UINode(
            type = "Button",
            events = mapOf("onTap" to "increment")
        )

        assertEquals(1, node.events.size)
        assertEquals("increment", node.events["onTap"])
    }

    @Test
    fun `creates node with children`() {
        val child1 = UINode(type = "Text")
        val child2 = UINode(type = "Button")
        val parent = UINode(
            type = "Column",
            children = listOf(child1, child2)
        )

        assertEquals(2, parent.children?.size)
        assertEquals("Text", parent.children?.get(0)?.type)
        assertEquals("Button", parent.children?.get(1)?.type)
    }

    @Test
    fun `creates node with id`() {
        val node = UINode(type = "Text", id = "mainText")

        assertEquals("mainText", node.id)
    }

    // ========== getAllPropNames ==========

    @Test
    fun `getAllPropNames includes static props`() {
        val node = UINode(
            type = "Text",
            props = mapOf("fontSize" to JsonPrimitive(48))
        )

        val propNames = node.getAllPropNames()

        assertTrue(propNames.contains("fontSize"))
    }

    @Test
    fun `getAllPropNames includes bindings`() {
        val node = UINode(
            type = "Text",
            bindings = mapOf("value" to "\$state.display")
        )

        val propNames = node.getAllPropNames()

        assertTrue(propNames.contains("value"))
    }

    @Test
    fun `getAllPropNames includes both static and bound props`() {
        val node = UINode(
            type = "Text",
            props = mapOf("fontSize" to JsonPrimitive(48)),
            bindings = mapOf("value" to "\$state.display")
        )

        val propNames = node.getAllPropNames()

        assertEquals(2, propNames.size)
        assertTrue(propNames.contains("fontSize"))
        assertTrue(propNames.contains("value"))
    }

    // ========== hasBindings ==========

    @Test
    fun `hasBindings returns true when node has bindings`() {
        val node = UINode(
            type = "Text",
            bindings = mapOf("value" to "\$state.display")
        )

        assertTrue(node.hasBindings())
    }

    @Test
    fun `hasBindings returns false when node has no bindings`() {
        val node = UINode(
            type = "Text",
            props = mapOf("value" to JsonPrimitive("static"))
        )

        assertFalse(node.hasBindings())
    }

    @Test
    fun `hasBindings returns true when child has bindings`() {
        val child = UINode(
            type = "Text",
            bindings = mapOf("value" to "\$state.display")
        )
        val parent = UINode(
            type = "Column",
            children = listOf(child)
        )

        assertTrue(parent.hasBindings())
    }

    // ========== hasEvents ==========

    @Test
    fun `hasEvents returns true when node has events`() {
        val node = UINode(
            type = "Button",
            events = mapOf("onTap" to "increment")
        )

        assertTrue(node.hasEvents())
    }

    @Test
    fun `hasEvents returns false when node has no events`() {
        val node = UINode(type = "Text")

        assertFalse(node.hasEvents())
    }

    @Test
    fun `hasEvents returns true when child has events`() {
        val child = UINode(
            type = "Button",
            events = mapOf("onTap" to "increment")
        )
        val parent = UINode(
            type = "Column",
            children = listOf(child)
        )

        assertTrue(parent.hasEvents())
    }

    // ========== getReferencedStatePaths ==========

    @Test
    fun `extracts simple state reference`() {
        val node = UINode(
            type = "Text",
            bindings = mapOf("value" to "\$state.display")
        )

        val paths = node.getReferencedStatePaths()

        assertEquals(1, paths.size)
        assertTrue(paths.contains("state.display"))
    }

    @Test
    fun `extracts nested state reference`() {
        val node = UINode(
            type = "Text",
            bindings = mapOf("value" to "\$state.user.name")
        )

        val paths = node.getReferencedStatePaths()

        assertTrue(paths.contains("state.user.name"))
    }

    @Test
    fun `extracts multiple state references from expression`() {
        val node = UINode(
            type = "Text",
            bindings = mapOf("value" to "\$state.a + \$state.b")
        )

        val paths = node.getReferencedStatePaths()

        assertEquals(2, paths.size)
        assertTrue(paths.contains("state.a"))
        assertTrue(paths.contains("state.b"))
    }

    @Test
    fun `extracts state references from children`() {
        val child1 = UINode(
            type = "Text",
            bindings = mapOf("value" to "\$state.display")
        )
        val child2 = UINode(
            type = "Text",
            bindings = mapOf("value" to "\$state.count")
        )
        val parent = UINode(
            type = "Column",
            children = listOf(child1, child2)
        )

        val paths = parent.getReferencedStatePaths()

        assertEquals(2, paths.size)
        assertTrue(paths.contains("state.display"))
        assertTrue(paths.contains("state.count"))
    }

    @Test
    fun `extracts state references from complex expression`() {
        val node = UINode(
            type = "Text",
            bindings = mapOf("value" to "\$logic.if(\$state.count > 0, \$state.display, \"0\")")
        )

        val paths = node.getReferencedStatePaths()

        assertTrue(paths.contains("state.count"))
        assertTrue(paths.contains("state.display"))
    }

    // ========== withChildren ==========

    @Test
    fun `withChildren creates new node with updated children`() {
        val original = UINode(
            type = "Column",
            children = listOf(UINode(type = "Text"))
        )

        val newChildren = listOf(
            UINode(type = "Button"),
            UINode(type = "Button")
        )
        val updated = original.withChildren(newChildren)

        assertEquals(1, original.children?.size)
        assertEquals(2, updated.children?.size)
        assertEquals("Button", updated.children?.get(0)?.type)
    }

    // ========== withProps ==========

    @Test
    fun `withProps creates new node with additional props`() {
        val original = UINode(
            type = "Text",
            props = mapOf("fontSize" to JsonPrimitive(48))
        )

        val updated = original.withProps(mapOf("color" to JsonPrimitive("red")))

        assertEquals(1, original.props.size)
        assertEquals(2, updated.props.size)
        assertEquals(JsonPrimitive("red"), updated.props["color"])
        assertEquals(JsonPrimitive(48), updated.props["fontSize"])
    }

    // ========== Companion Factory Methods ==========

    @Test
    fun `text factory creates text node`() {
        val node = UINode.text("Hello")

        assertEquals("Text", node.type)
        assertEquals("Hello", node.bindings["content"])
    }

    @Test
    fun `text factory with id`() {
        val node = UINode.text("Hello", id = "greeting")

        assertEquals("greeting", node.id)
    }

    @Test
    fun `button factory creates button node`() {
        val node = UINode.button("Click Me", "handleClick")

        assertEquals("Button", node.type)
        assertEquals(JsonPrimitive("Click Me"), node.props["label"])
        assertEquals("handleClick", node.events["onTap"])
    }

    @Test
    fun `column factory creates column node`() {
        val children = listOf(
            UINode.text("One"),
            UINode.text("Two")
        )
        val node = UINode.column(children, spacing = 16)

        assertEquals("Column", node.type)
        assertEquals(2, node.children?.size)
        assertEquals(JsonPrimitive(16), node.props["spacing"])
    }

    @Test
    fun `row factory creates row node`() {
        val children = listOf(
            UINode.text("One"),
            UINode.text("Two")
        )
        val node = UINode.row(children, spacing = 8)

        assertEquals("Row", node.type)
        assertEquals(2, node.children?.size)
        assertEquals(JsonPrimitive(8), node.props["spacing"])
    }

    // ========== Complex Trees ==========

    @Test
    fun `handles nested tree structure`() {
        val deepChild = UINode.text("Deep")
        val child = UINode.column(listOf(deepChild))
        val root = UINode.column(listOf(child))

        assertEquals("Column", root.type)
        assertEquals(1, root.children?.size)
        assertEquals("Column", root.children?.get(0)?.type)
        assertEquals("Text", root.children?.get(0)?.children?.get(0)?.type)
    }

    @Test
    fun `calculator UI tree structure`() {
        val display = UINode(
            type = "Text",
            props = mapOf("fontSize" to JsonPrimitive(48)),
            bindings = mapOf("value" to "\$state.display")
        )

        val buttons = listOf(
            UINode.button("7", "appendDigit('7')"),
            UINode.button("8", "appendDigit('8')"),
            UINode.button("9", "appendDigit('9')")
        )

        val buttonRow = UINode.row(buttons)
        val root = UINode.column(listOf(display, buttonRow))

        assertTrue(root.hasBindings())
        assertTrue(root.hasEvents())

        val paths = root.getReferencedStatePaths()
        assertTrue(paths.contains("state.display"))
    }

    // ========== Edge Cases ==========

    @Test
    fun `handles empty children list`() {
        val node = UINode(
            type = "Column",
            children = emptyList()
        )

        assertEquals(0, node.children?.size)
        assertFalse(node.hasBindings())
        assertFalse(node.hasEvents())
    }

    @Test
    fun `handles null children`() {
        val node = UINode(
            type = "Text",
            children = null
        )

        assertNull(node.children)
    }

    @Test
    fun `handles empty binding expression`() {
        val node = UINode(
            type = "Text",
            bindings = mapOf("value" to "")
        )

        assertEquals(0, node.getReferencedStatePaths().size)
    }

    @Test
    fun `handles binding without state reference`() {
        val node = UINode(
            type = "Text",
            bindings = mapOf("value" to "\"static value\"")
        )

        assertEquals(0, node.getReferencedStatePaths().size)
    }
}
