/**
 * ScreenHierarchyTest.kt - Unit Tests for Screen Hierarchy Core
 *
 * Tests for ScreenHierarchy data models and helper methods.
 *
 * Created: 2025-12-06
 * Part of: Universal Screen Hierarchy System - Test Suite
 *
 * @author IDEACODE v10.3
 */

package com.augmentalis.avaelements.context

import kotlin.test.*

class ScreenHierarchyTest {

    private fun createTestNode(
        id: String,
        type: String = "Container",
        role: ComponentRole = ComponentRole.CONTAINER,
        children: List<ComponentNode> = emptyList(),
        isInteractive: Boolean = false,
        voiceLabel: String? = null
    ): ComponentNode {
        return ComponentNode(
            id = id,
            type = type,
            role = role,
            children = children,
            isInteractive = isInteractive,
            voiceLabel = voiceLabel,
            depth = 0
        )
    }

    private fun createTestHierarchy(
        root: ComponentNode,
        commandableElements: List<CommandableElement> = emptyList(),
        formFields: List<FormField> = emptyList(),
        actions: List<ActionElement> = emptyList()
    ): ScreenHierarchy {
        return ScreenHierarchy(
            screenId = "test-screen",
            screenHash = "hash-123",
            screenType = ScreenType.FORM,
            appContext = AppContext("test.app", "Test App", "test.app"),
            navigationContext = NavigationContext("test-screen", null, listOf("test-screen")),
            root = root,
            commandableElements = commandableElements,
            formFields = formFields,
            actions = actions,
            complexity = ComplexityScore(10, 5, 3, 2, 1)
        )
    }

    @Test
    fun testComponentNodeFlatten() {
        // Create a tree: root -> child1, child2 -> grandchild
        val grandchild = createTestNode("grandchild")
        val child1 = createTestNode("child1")
        val child2 = createTestNode("child2", children = listOf(grandchild))
        val root = createTestNode("root", children = listOf(child1, child2))

        val flattened = root.flatten()

        assertEquals(4, flattened.size, "Should flatten all 4 nodes")
        assertTrue(flattened.any { it.id == "root" }, "Should contain root")
        assertTrue(flattened.any { it.id == "child1" }, "Should contain child1")
        assertTrue(flattened.any { it.id == "child2" }, "Should contain child2")
        assertTrue(flattened.any { it.id == "grandchild" }, "Should contain grandchild")
    }

    @Test
    fun testComponentNodeFindById() {
        val grandchild = createTestNode("grandchild")
        val child1 = createTestNode("child1")
        val child2 = createTestNode("child2", children = listOf(grandchild))
        val root = createTestNode("root", children = listOf(child1, child2))

        val found = root.findById("grandchild")
        assertNotNull(found, "Should find grandchild")
        assertEquals("grandchild", found.id)

        val notFound = root.findById("nonexistent")
        assertNull(notFound, "Should return null for nonexistent ID")
    }

    @Test
    fun testComponentNodeGetMaxDepth() {
        // Tree depth: root(0) -> child(1) -> grandchild(2) -> great-grandchild(3)
        val greatGrandchild = createTestNode("ggc").copy(depth = 3)
        val grandchild = createTestNode("gc", children = listOf(greatGrandchild)).copy(depth = 2)
        val child = createTestNode("c", children = listOf(grandchild)).copy(depth = 1)
        val root = createTestNode("root", children = listOf(child)).copy(depth = 0)

        assertEquals(3, root.getMaxDepth(), "Max depth should be 3")
    }

    @Test
    fun testComponentNodeGetMaxDepthSingleNode() {
        val root = createTestNode("root").copy(depth = 0)
        assertEquals(0, root.getMaxDepth(), "Single node should have depth 0")
    }

    @Test
    fun testScreenHierarchyGetInteractiveCount() {
        val child1 = createTestNode("btn1", isInteractive = true)
        val child2 = createTestNode("text", isInteractive = false)
        val child3 = createTestNode("btn2", isInteractive = true)
        val root = createTestNode("root", children = listOf(child1, child2, child3))

        val hierarchy = createTestHierarchy(root)

        assertEquals(2, hierarchy.getInteractiveCount(), "Should count 2 interactive elements")
    }

    @Test
    fun testScreenHierarchyIsFormScreen() {
        val root = createTestNode("root")

        val formFields = listOf(
            FormField("field1", FieldType.TEXT, "Email", null, null, true, "email field"),
            FormField("field2", FieldType.PASSWORD, "Password", null, null, true, "password field")
        )

        val formHierarchy = createTestHierarchy(root, formFields = formFields)
        assertTrue(formHierarchy.isFormScreen(), "Should be form screen with 2+ fields")

        val nonFormHierarchy = createTestHierarchy(root)
        assertFalse(nonFormHierarchy.isFormScreen(), "Should not be form screen with 0 fields")
    }

    @Test
    fun testScreenHierarchyGetAvailableCommands() {
        val root = createTestNode("root")
        val commandables = listOf(
            CommandableElement("btn1", "submit button", "Button", "click", listOf("tap", "press"), priority = 10),
            CommandableElement("btn2", "cancel button", "Button", "click", listOf("tap"), priority = 5)
        )

        val hierarchy = createTestHierarchy(root, commandableElements = commandables)
        val commands = hierarchy.getAvailableCommands()

        assertEquals(2, commands.size, "Should have 2 primary commands")
        assertTrue(commands.contains("submit button"), "Should contain 'submit button'")
        assertTrue(commands.contains("cancel button"), "Should contain 'cancel button'")
    }

    @Test
    fun testComplexityScoreGetLevel() {
        val low = ComplexityScore(5, 2, 2, 0, 1)
        assertEquals(ComplexityLevel.LOW, low.getLevel(), "Should be LOW complexity")

        val medium = ComplexityScore(15, 8, 4, 3, 2)
        assertEquals(ComplexityLevel.MEDIUM, medium.getLevel(), "Should be MEDIUM complexity")

        val high = ComplexityScore(35, 20, 6, 8, 5)
        assertEquals(ComplexityLevel.HIGH, high.getLevel(), "Should be HIGH complexity")

        val veryHigh = ComplexityScore(60, 40, 9, 15, 10)
        assertEquals(ComplexityLevel.VERY_HIGH, veryHigh.getLevel(), "Should be VERY_HIGH complexity")
    }

    @Test
    fun testComplexityScoreCalculate() {
        val root = createTestNode(
            "root",
            children = listOf(
                createTestNode("child1", isInteractive = true),
                createTestNode("child2", isInteractive = true),
                createTestNode("child3", isInteractive = false)
            )
        ).copy(depth = 0)

        val formFields = listOf(
            FormField("f1", FieldType.TEXT, null, null, null, false, null),
            FormField("f2", FieldType.EMAIL, null, null, null, false, null)
        )

        val actions = listOf(
            ActionElement("a1", ActionType.SUBMIT, "Submit", null, false, "submit")
        )

        val complexity = ComplexityScore.calculate(root, formFields, actions)

        assertEquals(4, complexity.totalComponents, "Should count 4 total components")
        assertEquals(2, complexity.interactiveComponents, "Should count 2 interactive")
        assertEquals(0, complexity.maxDepth, "Max depth should be 0 for this structure")
        assertEquals(2, complexity.formFieldCount, "Should count 2 form fields")
        assertEquals(1, complexity.actionCount, "Should count 1 action")
    }

    @Test
    fun testAppContextCreation() {
        val context = AppContext(
            appId = "com.test.app",
            appName = "Test App",
            packageName = "com.test.app",
            activityName = "MainActivity",
            category = "productivity"
        )

        assertEquals("com.test.app", context.appId)
        assertEquals("Test App", context.appName)
        assertEquals("com.test.app", context.packageName)
        assertEquals("MainActivity", context.activityName)
        assertEquals("productivity", context.category)
    }

    @Test
    fun testNavigationContextCreation() {
        val navContext = NavigationContext(
            currentScreen = "screen2",
            previousScreen = "screen1",
            navigationStack = listOf("screen1", "screen2"),
            canNavigateBack = true,
            canNavigateForward = false
        )

        assertEquals("screen2", navContext.currentScreen)
        assertEquals("screen1", navContext.previousScreen)
        assertEquals(2, navContext.navigationStack.size)
        assertTrue(navContext.canNavigateBack)
        assertFalse(navContext.canNavigateForward)
    }

    @Test
    fun testCommandableElementCreation() {
        val element = CommandableElement(
            id = "btn-submit",
            voiceLabel = "submit button",
            componentType = "Button",
            primaryCommand = "click",
            alternateCommands = listOf("tap", "press", "activate"),
            parameters = mapOf("action" to "submit", "form" to "login"),
            priority = 10
        )

        assertEquals("btn-submit", element.id)
        assertEquals("submit button", element.voiceLabel)
        assertEquals("Button", element.componentType)
        assertEquals("click", element.primaryCommand)
        assertEquals(3, element.alternateCommands.size)
        assertEquals(2, element.parameters.size)
        assertEquals(10, element.priority)
    }

    @Test
    fun testFormFieldCreation() {
        val field = FormField(
            id = "email-field",
            fieldType = FieldType.EMAIL,
            label = "Email Address",
            placeholder = "Enter your email",
            currentValue = "test@example.com",
            isRequired = true,
            voiceLabel = "email address field"
        )

        assertEquals("email-field", field.id)
        assertEquals(FieldType.EMAIL, field.fieldType)
        assertEquals("Email Address", field.label)
        assertEquals("Enter your email", field.placeholder)
        assertEquals("test@example.com", field.currentValue)
        assertTrue(field.isRequired)
        assertEquals("email address field", field.voiceLabel)
    }

    @Test
    fun testActionElementCreation() {
        val action = ActionElement(
            id = "submit-action",
            actionType = ActionType.SUBMIT,
            label = "Submit Form",
            destination = null,
            confirmationRequired = false,
            voiceCommand = "submit form"
        )

        assertEquals("submit-action", action.id)
        assertEquals(ActionType.SUBMIT, action.actionType)
        assertEquals("Submit Form", action.label)
        assertNull(action.destination)
        assertFalse(action.confirmationRequired)
        assertEquals("submit form", action.voiceCommand)
    }

    @Test
    fun testDataElementCreation() {
        val dataElement = DataElement(
            id = "product-list",
            dataType = DataType.LIST,
            content = "5 items",
            semanticMeaning = "Product catalog",
            isScrollable = true
        )

        assertEquals("product-list", dataElement.id)
        assertEquals(DataType.LIST, dataElement.dataType)
        assertEquals("5 items", dataElement.content)
        assertEquals("Product catalog", dataElement.semanticMeaning)
        assertTrue(dataElement.isScrollable)
    }

    @Test
    fun testRectangleCreation() {
        val rect = Rectangle(10f, 20f, 100f, 200f)

        assertEquals(10f, rect.left)
        assertEquals(20f, rect.top)
        assertEquals(100f, rect.right)
        assertEquals(200f, rect.bottom)
    }
}
