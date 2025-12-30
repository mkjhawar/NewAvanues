package com.augmentalis.magiccode.generator.parser

import com.augmentalis.magiccode.generator.ast.*
import kotlin.test.*

/**
 * CompactSyntaxParser Tests - AvaMagicUCD Format
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */
class CompactSyntaxParserTest {

    private val parser = CompactSyntaxParser()

    // Basic Component Parsing

    @Test
    fun `test parse simple button`() {
        val input = """MagicButton("Click Me")"""

        val result = parser.parseComponent(input)
        assertTrue(result.isSuccess)

        val component = result.getOrThrow()
        assertEquals(ComponentType.BUTTON, component.type)
        assertEquals("Click Me", component.properties["text"])
    }

    @Test
    fun `test parse button with variant`() {
        val input = """MagicButton.Primary("Submit", onClick: handleSubmit)"""

        val result = parser.parseComponent(input)
        assertTrue(result.isSuccess)

        val component = result.getOrThrow()
        assertEquals(ComponentType.BUTTON, component.type)
        assertEquals("Submit", component.properties["text"])
        assertEquals("Primary", component.properties["variant"])
        assertTrue(component.eventHandlers.containsKey("onClick"))
    }

    @Test
    fun `test parse text field with properties`() {
        val input = """MagicTextField.Email(placeholder: "Enter email", bind: user.email)"""

        val result = parser.parseComponent(input)
        assertTrue(result.isSuccess)

        val component = result.getOrThrow()
        assertEquals(ComponentType.TEXT_FIELD, component.type)
        assertEquals("Enter email", component.properties["placeholder"])
        assertEquals("user.email", component.properties["bind"])
        assertEquals("Email", component.properties["variant"])
    }

    @Test
    fun `test parse text with content`() {
        val input = """MagicText(content: "Hello World", variant: "H1")"""

        val result = parser.parseComponent(input)
        assertTrue(result.isSuccess)

        val component = result.getOrThrow()
        assertEquals(ComponentType.TEXT, component.type)
        assertEquals("Hello World", component.properties["content"])
        assertEquals("H1", component.properties["variant"])
    }

    // Screen Parsing

    @Test
    fun `test parse login screen`() {
        val input = """
            MagicScreen.Login {
                MagicColumn {
                    MagicTextField.Email(placeholder: "Email", bind: email)
                    MagicTextField.Password(placeholder: "Password", bind: password)
                    MagicButton.Submit("Login", onClick: doLogin)
                }
            }
        """.trimIndent()

        val result = parser.parseScreen(input)
        assertTrue(result.isSuccess)

        val screen = result.getOrThrow()
        assertEquals("Login", screen.name)
        assertEquals(ComponentType.COLUMN, screen.root.type)
        assertEquals(3, screen.root.children.size)
    }

    @Test
    fun `test parse screen with state declarations`() {
        val input = """
            MagicScreen.Profile {
                @state name: String = ""
                @state age: Int = 0
                @state isActive: Boolean = true

                MagicColumn {
                    MagicText(content: "Profile")
                    MagicTextField(bind: name)
                }
            }
        """.trimIndent()

        val result = parser.parseScreen(input)
        assertTrue(result.isSuccess)

        val screen = result.getOrThrow()
        assertEquals(3, screen.stateVariables.size)

        assertEquals("name", screen.stateVariables[0].name)
        assertEquals("String", screen.stateVariables[0].type)

        assertEquals("age", screen.stateVariables[1].name)
        assertEquals("Int", screen.stateVariables[1].type)

        assertEquals("isActive", screen.stateVariables[2].name)
        assertEquals("Boolean", screen.stateVariables[2].type)
    }

    @Test
    fun `test parse screen with single child`() {
        val input = """
            MagicScreen.Home {
                MagicCard {
                    MagicText(content: "Welcome")
                }
            }
        """.trimIndent()

        val result = parser.parseScreen(input)
        assertTrue(result.isSuccess)

        val screen = result.getOrThrow()
        // Single child should be used directly, not wrapped in Column
        assertEquals(ComponentType.CARD, screen.root.type)
    }

    // Nested Components

    @Test
    fun `test parse nested structure`() {
        val input = """
            MagicCard {
                MagicColumn {
                    MagicText(content: "Title", variant: "H2")
                    MagicRow {
                        MagicButton("Cancel", onClick: cancel)
                        MagicButton("OK", onClick: confirm)
                    }
                }
            }
        """.trimIndent()

        val result = parser.parseComponent(input)
        assertTrue(result.isSuccess)

        val card = result.getOrThrow()
        assertEquals(ComponentType.CARD, card.type)

        val column = card.children[0]
        assertEquals(ComponentType.COLUMN, column.type)
        assertEquals(2, column.children.size)

        val row = column.children[1]
        assertEquals(ComponentType.ROW, row.type)
        assertEquals(2, row.children.size)
    }

    @Test
    fun `test parse deeply nested structure`() {
        val input = """
            MagicContainer {
                MagicColumn {
                    MagicCard {
                        MagicRow {
                            MagicIcon(name: "star")
                            MagicText(content: "Rating")
                        }
                    }
                }
            }
        """.trimIndent()

        val result = parser.parseComponent(input)
        assertTrue(result.isSuccess)

        val container = result.getOrThrow()
        assertEquals(ComponentType.CONTAINER, container.type)

        val column = container.children[0]
        val card = column.children[0]
        val row = card.children[0]

        assertEquals(2, row.children.size)
        assertEquals(ComponentType.ICON, row.children[0].type)
        assertEquals(ComponentType.TEXT, row.children[1].type)
    }

    // Property Types

    @Test
    fun `test parse string properties`() {
        val input = """MagicText(content: "Hello \"World\"", style: 'italic')"""

        val result = parser.parseComponent(input)
        assertTrue(result.isSuccess)

        val component = result.getOrThrow()
        assertEquals("Hello \"World\"", component.properties["content"])
        assertEquals("italic", component.properties["style"])
    }

    @Test
    fun `test parse numeric properties`() {
        val input = """MagicSlider(min: 0, max: 100, value: 50, step: 0.5)"""

        val result = parser.parseComponent(input)
        assertTrue(result.isSuccess)

        val component = result.getOrThrow()
        assertEquals(0, component.properties["min"])
        assertEquals(100, component.properties["max"])
        assertEquals(50, component.properties["value"])
        assertEquals(0.5, component.properties["step"])
    }

    @Test
    fun `test parse boolean properties`() {
        val input = """MagicTextField(enabled: true, readOnly: false, required: true)"""

        val result = parser.parseComponent(input)
        assertTrue(result.isSuccess)

        val component = result.getOrThrow()
        assertEquals(true, component.properties["enabled"])
        assertEquals(false, component.properties["readOnly"])
        assertEquals(true, component.properties["required"])
    }

    @Test
    fun `test parse array properties`() {
        val input = """MagicDropdown(options: ["Red", "Green", "Blue"], selected: 0)"""

        val result = parser.parseComponent(input)
        assertTrue(result.isSuccess)

        val component = result.getOrThrow()
        val options = component.properties["options"] as List<*>
        assertEquals(3, options.size)
        assertEquals("Red", options[0])
        assertEquals("Green", options[1])
        assertEquals("Blue", options[2])
    }

    @Test
    fun `test parse object properties`() {
        val input = """MagicText(style: {fontSize: 16, fontWeight: "bold", color: "#333"})"""

        val result = parser.parseComponent(input)
        assertTrue(result.isSuccess)

        val component = result.getOrThrow()
        val style = component.properties["style"] as Map<*, *>
        assertEquals(16, style["fontSize"])
        assertEquals("bold", style["fontWeight"])
        assertEquals("#333", style["color"])
    }

    // Event Handlers

    @Test
    fun `test parse multiple event handlers`() {
        val input = """MagicButton("Press", onClick: handleClick, onLongPress: handleLongPress)"""

        val result = parser.parseComponent(input)
        assertTrue(result.isSuccess)

        val component = result.getOrThrow()
        assertEquals("handleClick", component.eventHandlers["onClick"])
        assertEquals("handleLongPress", component.eventHandlers["onLongPress"])
    }

    @Test
    fun `test parse event handlers with dot notation`() {
        val input = """MagicButton("Save", onClick: viewModel.save)"""

        val result = parser.parseComponent(input)
        assertTrue(result.isSuccess)

        val component = result.getOrThrow()
        assertEquals("viewModel.save", component.eventHandlers["onClick"])
    }

    // All Component Types

    @Test
    fun `test parse all foundation components`() {
        val components = listOf(
            "MagicButton(text: \"OK\")" to ComponentType.BUTTON,
            "MagicCard {}" to ComponentType.CARD,
            "MagicCheckbox(checked: true)" to ComponentType.CHECKBOX,
            "MagicChip(label: \"Tag\")" to ComponentType.CHIP,
            "MagicDivider()" to ComponentType.DIVIDER,
            "MagicImage(src: \"url\")" to ComponentType.IMAGE,
            "MagicListItem(title: \"Item\")" to ComponentType.LIST_ITEM,
            "MagicText(content: \"Hi\")" to ComponentType.TEXT,
            "MagicTextField(value: \"\")" to ComponentType.TEXT_FIELD
        )

        for ((input, expectedType) in components) {
            val result = parser.parseComponent(input)
            assertTrue(result.isSuccess, "Failed to parse: $input")
            assertEquals(expectedType, result.getOrThrow().type, "Wrong type for: $input")
        }
    }

    @Test
    fun `test parse all layout components`() {
        val components = listOf(
            "MagicContainer {}" to ComponentType.CONTAINER,
            "MagicRow {}" to ComponentType.ROW,
            "MagicColumn {}" to ComponentType.COLUMN,
            "MagicSpacer()" to ComponentType.SPACER,
            "MagicStack {}" to ComponentType.STACK,
            "MagicGrid {}" to ComponentType.GRID,
            "MagicScrollView {}" to ComponentType.SCROLL_VIEW
        )

        for ((input, expectedType) in components) {
            val result = parser.parseComponent(input)
            assertTrue(result.isSuccess, "Failed to parse: $input")
            assertEquals(expectedType, result.getOrThrow().type, "Wrong type for: $input")
        }
    }

    @Test
    fun `test parse all advanced components`() {
        val components = listOf(
            "MagicSwitch(checked: false)" to ComponentType.SWITCH,
            "MagicSlider(value: 50)" to ComponentType.SLIDER,
            "MagicProgressBar(progress: 0.5)" to ComponentType.PROGRESS_BAR,
            "MagicSpinner()" to ComponentType.SPINNER,
            "MagicAlert(message: \"Info\")" to ComponentType.ALERT,
            "MagicDialog {}" to ComponentType.DIALOG,
            "MagicToast(message: \"Done\")" to ComponentType.TOAST,
            "MagicTooltip(text: \"Help\")" to ComponentType.TOOLTIP,
            "MagicRadio(selected: 0)" to ComponentType.RADIO,
            "MagicDropdown(options: [])" to ComponentType.DROPDOWN,
            "MagicDatePicker()" to ComponentType.DATE_PICKER,
            "MagicTimePicker()" to ComponentType.TIME_PICKER,
            "MagicSearchBar(placeholder: \"Search\")" to ComponentType.SEARCH_BAR,
            "MagicRating(value: 4)" to ComponentType.RATING,
            "MagicBadge(count: 5)" to ComponentType.BADGE,
            "MagicAppBar(title: \"App\")" to ComponentType.APP_BAR,
            "MagicBottomNav {}" to ComponentType.BOTTOM_NAV,
            "MagicDrawer {}" to ComponentType.DRAWER,
            "MagicTabs {}" to ComponentType.TABS
        )

        for ((input, expectedType) in components) {
            val result = parser.parseComponent(input)
            assertTrue(result.isSuccess, "Failed to parse: $input")
            assertEquals(expectedType, result.getOrThrow().type, "Wrong type for: $input")
        }
    }

    // Validation

    @Test
    fun `test validate valid screen`() {
        val input = """
            MagicScreen.Login {
                MagicButton("OK")
            }
        """.trimIndent()

        val result = parser.validate(input)
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `test validate unmatched braces`() {
        val input = """
            MagicScreen.Login {
                MagicColumn {
                    MagicButton("OK")
            }
        """.trimIndent()

        val result = parser.validate(input)
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.message.contains("brace") })
    }

    @Test
    fun `test validate non-screen root`() {
        val input = """
            MagicButton.Primary {
                MagicText(content: "Hi")
            }
        """.trimIndent()

        val result = parser.validate(input)
        assertTrue(result.warnings.isNotEmpty())
    }

    // Edge Cases

    @Test
    fun `test parse with comments`() {
        val input = """
            MagicScreen.Home {
                // Header section
                MagicText(content: "Title")
                // Button section
                MagicButton("Click")
            }
        """.trimIndent()

        val result = parser.parseScreen(input)
        assertTrue(result.isSuccess)

        val screen = result.getOrThrow()
        assertEquals(2, screen.root.children.size)
    }

    @Test
    fun `test parse with semicolons`() {
        val input = """
            MagicScreen.Form {
                MagicTextField(bind: name);
                MagicTextField(bind: email);
                MagicButton("Submit");
            }
        """.trimIndent()

        val result = parser.parseScreen(input)
        assertTrue(result.isSuccess)

        val screen = result.getOrThrow()
        assertEquals(3, screen.root.children.size)
    }

    @Test
    fun `test parse empty children block`() {
        val input = """MagicCard {}"""

        val result = parser.parseComponent(input)
        assertTrue(result.isSuccess)

        val component = result.getOrThrow()
        assertTrue(component.children.isEmpty())
    }

    @Test
    fun `test parse component without children block`() {
        val input = """MagicButton("OK")"""

        val result = parser.parseComponent(input)
        assertTrue(result.isSuccess)

        val component = result.getOrThrow()
        assertTrue(component.children.isEmpty())
    }

    @Test
    fun `test parse with Ava prefix`() {
        val input = """AvaButton("OK", onClick: submit)"""

        val result = parser.parseComponent(input)
        assertTrue(result.isSuccess)

        val component = result.getOrThrow()
        assertEquals(ComponentType.BUTTON, component.type)
    }

    @Test
    fun `test parse extension function`() {
        val input = """MagicScreen.Dashboard { MagicText(content: "Welcome") }"""

        val result = input.parseAsUltraCompactDSL()
        assertTrue(result.isSuccess)
        assertEquals("Dashboard", result.getOrThrow().name)
    }

    // Real-world Examples

    @Test
    fun `test parse maintenance workflow screen`() {
        val input = """
            MagicScreen.WorkflowList {
                MagicColumn {
                    MagicAppBar(title: "Workflows")
                    MagicScrollView {
                        MagicColumn {
                            MagicListItem(title: "HVAC Inspection", subtitle: "8 steps", onClick: startHVAC)
                            MagicListItem(title: "Electrical Check", subtitle: "5 steps", onClick: startElectrical)
                            MagicListItem(title: "Plumbing Review", subtitle: "6 steps", onClick: startPlumbing)
                        }
                    }
                }
            }
        """.trimIndent()

        val result = parser.parseScreen(input)
        assertTrue(result.isSuccess)

        val screen = result.getOrThrow()
        assertEquals("WorkflowList", screen.name)
    }

    @Test
    fun `test parse login form with validation`() {
        val input = """
            MagicScreen.Login {
                @state email: String = ""
                @state password: String = ""
                @state isLoading: Boolean = false

                MagicCard {
                    MagicColumn {
                        MagicText(content: "Sign In", variant: "H1")
                        MagicTextField.Email(
                            placeholder: "Email address",
                            bind: email,
                            required: true,
                            validation: "email"
                        )
                        MagicTextField.Password(
                            placeholder: "Password",
                            bind: password,
                            required: true,
                            minLength: 8
                        )
                        MagicButton.Primary(
                            "Sign In",
                            onClick: handleLogin,
                            loading: isLoading
                        )
                        MagicButton.Link("Forgot Password?", onClick: forgotPassword)
                    }
                }
            }
        """.trimIndent()

        val result = parser.parseScreen(input)
        assertTrue(result.isSuccess)

        val screen = result.getOrThrow()
        assertEquals("Login", screen.name)
        assertEquals(3, screen.stateVariables.size)
    }

    // Error Handling

    @Test
    fun `test error on invalid syntax`() {
        val input = """MagicButton(text: """

        val result = parser.parseComponent(input)
        assertTrue(result.isFailure)
    }

    @Test
    fun `test error on missing closing paren`() {
        val input = """MagicButton("OK""""

        val result = parser.parseComponent(input)
        assertTrue(result.isFailure)
    }

    @Test
    fun `test error message contains line info`() {
        val input = """
            MagicScreen.Test {
                MagicButton(text:
            }
        """.trimIndent()

        val result = parser.parseScreen(input)
        assertTrue(result.isFailure)

        val message = result.exceptionOrNull()?.message ?: ""
        assertTrue(message.contains("line"), "Error should contain line number: $message")
    }
}
