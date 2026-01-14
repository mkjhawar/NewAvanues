package com.augmentalis.magicui.codegen.parser

import com.augmentalis.magicui.codegen.ast.*
import kotlin.test.*

/**
 * VosParser Tests
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */
class VosParserTest {

    private val parser = VosParser()

    @Test
    fun `test parseComponentType for button`() {
        val json = """
            {
                "type": "BUTTON",
                "properties": {
                    "text": "Click Me"
                }
            }
        """.trimIndent()

        val result = parser.parseComponent(json)
        assertTrue(result.isSuccess)

        val component = result.getOrThrow()
        assertEquals(ComponentType.BUTTON, component.type)
    }

    @Test
    fun `test parseComponent with properties`() {
        val json = """
            {
                "type": "TEXT",
                "properties": {
                    "content": "Hello World",
                    "variant": "H1"
                }
            }
        """.trimIndent()

        val result = parser.parseComponent(json)
        assertTrue(result.isSuccess)

        val component = result.getOrThrow()
        assertEquals(ComponentType.TEXT, component.type)
        assertTrue(component.properties.containsKey("content"))
    }

    @Test
    fun `test parseComponent with children`() {
        val json = """
            {
                "type": "COLUMN",
                "children": [
                    {
                        "type": "TEXT",
                        "properties": { "content": "Title" }
                    },
                    {
                        "type": "BUTTON",
                        "properties": { "text": "Submit" }
                    }
                ]
            }
        """.trimIndent()

        val result = parser.parseComponent(json)
        assertTrue(result.isSuccess)

        val component = result.getOrThrow()
        assertEquals(ComponentType.COLUMN, component.type)
        assertEquals(2, component.children.size)
        assertEquals(ComponentType.TEXT, component.children[0].type)
        assertEquals(ComponentType.BUTTON, component.children[1].type)
    }

    @Test
    fun `test parseComponent with event handlers`() {
        val json = """
            {
                "type": "BUTTON",
                "properties": { "text": "Click" },
                "events": {
                    "onClick": "handleClick"
                }
            }
        """.trimIndent()

        val result = parser.parseComponent(json)
        assertTrue(result.isSuccess)

        val component = result.getOrThrow()
        assertTrue(component.eventHandlers.containsKey("onClick"))
        assertEquals("handleClick", component.eventHandlers["onClick"])
    }

    @Test
    fun `test parseScreen with state variables`() {
        val json = """
            {
                "name": "LoginScreen",
                "state": [
                    {
                        "name": "email",
                        "type": "String",
                        "initialValue": "",
                        "mutable": true
                    },
                    {
                        "name": "password",
                        "type": "String",
                        "initialValue": "",
                        "mutable": true
                    }
                ],
                "root": {
                    "type": "COLUMN",
                    "children": []
                }
            }
        """.trimIndent()

        val result = parser.parseScreen(json)
        assertTrue(result.isSuccess)

        val screen = result.getOrThrow()
        assertEquals("LoginScreen", screen.name)
        assertEquals(2, screen.stateVariables.size)
        assertEquals("email", screen.stateVariables[0].name)
        assertEquals("String", screen.stateVariables[0].type)
    }

    @Test
    fun `test validate with valid DSL`() {
        val json = """
            {
                "type": "BUTTON",
                "properties": { "text": "Valid" }
            }
        """.trimIndent()

        val result = parser.validate(json)
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `test validate with missing type`() {
        val json = """
            {
                "properties": { "text": "Invalid" }
            }
        """.trimIndent()

        val result = parser.validate(json)
        assertFalse(result.isValid)
        assertTrue(result.errors.isNotEmpty())
    }

    @Test
    fun `test validate with invalid component type`() {
        val json = """
            {
                "type": "INVALID_TYPE",
                "properties": {}
            }
        """.trimIndent()

        val result = parser.validate(json)
        assertFalse(result.isValid)
    }

    @Test
    fun `test parseTheme`() {
        val json = """
            {
                "name": "DarkTheme",
                "colors": {
                    "primary": "#2196F3",
                    "secondary": "#FF5722",
                    "background": "#121212"
                }
            }
        """.trimIndent()

        val result = parser.parseTheme(json)
        assertTrue(result.isSuccess)

        val theme = result.getOrThrow()
        assertEquals("DarkTheme", theme.name)
        assertEquals("#2196F3", theme.colors["primary"])
    }

    @Test
    fun `test complex nested structure`() {
        val json = """
            {
                "type": "CARD",
                "children": [
                    {
                        "type": "COLUMN",
                        "children": [
                            {
                                "type": "TEXT",
                                "properties": { "content": "Title", "variant": "H2" }
                            },
                            {
                                "type": "TEXT",
                                "properties": { "content": "Subtitle", "variant": "BODY2" }
                            },
                            {
                                "type": "ROW",
                                "children": [
                                    {
                                        "type": "BUTTON",
                                        "properties": { "text": "Cancel" }
                                    },
                                    {
                                        "type": "BUTTON",
                                        "properties": { "text": "Confirm" }
                                    }
                                ]
                            }
                        ]
                    }
                ]
            }
        """.trimIndent()

        val result = parser.parseComponent(json)
        assertTrue(result.isSuccess)

        val card = result.getOrThrow()
        assertEquals(ComponentType.CARD, card.type)
        assertEquals(1, card.children.size)

        val column = card.children[0]
        assertEquals(ComponentType.COLUMN, column.type)
        assertEquals(3, column.children.size)

        val row = column.children[2]
        assertEquals(ComponentType.ROW, row.type)
        assertEquals(2, row.children.size)
    }
}
