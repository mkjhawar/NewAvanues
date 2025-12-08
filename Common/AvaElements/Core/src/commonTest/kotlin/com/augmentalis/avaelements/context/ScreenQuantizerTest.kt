/**
 * ScreenQuantizerTest.kt - Unit Tests for Screen Quantization
 *
 * Tests for AI context generation and quantization logic.
 *
 * Created: 2025-12-06
 * Part of: Universal Screen Hierarchy System - Test Suite
 *
 * @author IDEACODE v10.3
 */

package com.augmentalis.avaelements.context

import kotlin.test.*

class ScreenQuantizerTest {

    private fun createTestHierarchy(
        screenType: ScreenType = ScreenType.FORM,
        commandableElements: List<CommandableElement> = emptyList(),
        formFields: List<FormField> = emptyList(),
        actions: List<ActionElement> = emptyList(),
        complexity: ComplexityScore = ComplexityScore(10, 5, 3, 2, 1)
    ): ScreenHierarchy {
        val root = ComponentNode(
            id = "root",
            type = "Container",
            role = ComponentRole.CONTAINER,
            isInteractive = false,
            depth = 0
        )

        return ScreenHierarchy(
            screenId = "test-screen",
            screenHash = "hash-123",
            screenType = screenType,
            screenPurpose = "Test screen for unit testing",
            primaryAction = PrimaryAction.SUBMIT,
            appContext = AppContext("test.app", "Test App", "test.app"),
            navigationContext = NavigationContext("test-screen", null, listOf("test-screen")),
            root = root,
            commandableElements = commandableElements,
            formFields = formFields,
            actions = actions,
            complexity = complexity
        )
    }

    @Test
    fun testQuantizeBasicStructure() {
        val hierarchy = createTestHierarchy()
        val quantized = ScreenQuantizer.quantize(hierarchy)

        assertNotNull(quantized.summary, "Summary should not be null")
        assertNotNull(quantized.screen, "Screen info should not be null")
        assertNotNull(quantized.commands, "Commands should not be null")
        assertNotNull(quantized.context, "Context should not be null")
        assertNotNull(quantized.componentGraph, "Component graph should not be null")
        assertNotNull(quantized.metadata, "Metadata should not be null")
    }

    @Test
    fun testGenerateSummaryLoginScreen() {
        val formFields = listOf(
            FormField("email", FieldType.EMAIL, "Email", null, null, true, "email field"),
            FormField("password", FieldType.PASSWORD, "Password", null, null, true, "password field")
        )

        val actions = listOf(
            ActionElement("submit", ActionType.SUBMIT, "Login", null, false, "login button")
        )

        val hierarchy = createTestHierarchy(
            screenType = ScreenType.LOGIN,
            formFields = formFields,
            actions = actions
        )

        val summary = ScreenQuantizer.generateSummary(hierarchy)

        assertTrue(summary.contains("Login"), "Summary should mention screen type")
        assertTrue(summary.contains("2 text fields") || summary.contains("2 form fields"), "Summary should count fields")
        assertTrue(summary.contains("1 action") || summary.contains("button"), "Summary should mention actions")
    }

    @Test
    fun testGenerateCompactSummary() {
        val formFields = listOf(
            FormField("email", FieldType.EMAIL, "Email", null, null, true, "email field")
        )

        val hierarchy = createTestHierarchy(
            screenType = ScreenType.LOGIN,
            formFields = formFields
        )

        val compact = ScreenQuantizer.generateCompactSummary(hierarchy)

        assertNotNull(compact, "Compact summary should not be null")
        assertTrue(compact.length < 100, "Compact summary should be short (< 100 chars)")
        assertTrue(compact.contains("Login") || compact.contains("login"), "Should mention screen type")
    }

    @Test
    fun testExtractEntitiesFromFormFields() {
        val formFields = listOf(
            FormField("email", FieldType.EMAIL, "Email Address", null, null, true, "email field"),
            FormField("password", FieldType.PASSWORD, "Password", null, null, true, "password field"),
            FormField("phone", FieldType.PHONE, "Phone Number", null, null, false, "phone field")
        )

        val hierarchy = createTestHierarchy(formFields = formFields)
        val entities = ScreenQuantizer.extractEntities(hierarchy)

        assertTrue(entities.isNotEmpty(), "Should extract entities from form fields")

        val emailEntity = entities.find { it.type == "email" || it.value.contains("email") }
        assertNotNull(emailEntity, "Should extract email entity")

        val passwordEntity = entities.find { it.type == "password" || it.value.contains("password") }
        assertNotNull(passwordEntity, "Should extract password entity")
    }

    @Test
    fun testExtractEntitiesFromCommandableElements() {
        val commandables = listOf(
            CommandableElement("btn1", "submit button", "Button", "click", emptyList(), priority = 10),
            CommandableElement("btn2", "cancel button", "Button", "click", emptyList(), priority = 5)
        )

        val hierarchy = createTestHierarchy(commandableElements = commandables)
        val entities = ScreenQuantizer.extractEntities(hierarchy)

        assertTrue(entities.isNotEmpty(), "Should extract entities from commandable elements")

        val submitEntity = entities.find { it.value.contains("submit") }
        assertNotNull(submitEntity, "Should extract submit button entity")

        val cancelEntity = entities.find { it.value.contains("cancel") }
        assertNotNull(cancelEntity, "Should extract cancel button entity")
    }

    @Test
    fun testGenerateIntentSchemaForLoginScreen() {
        val formFields = listOf(
            FormField("email", FieldType.EMAIL, "Email", null, null, true, "email field"),
            FormField("password", FieldType.PASSWORD, "Password", null, null, true, "password field")
        )

        val actions = listOf(
            ActionElement("submit", ActionType.SUBMIT, "Login", null, false, "login")
        )

        val hierarchy = createTestHierarchy(
            screenType = ScreenType.LOGIN,
            formFields = formFields,
            actions = actions
        )

        val intentSchema = ScreenQuantizer.generateIntentSchema(hierarchy)

        assertNotNull(intentSchema, "Intent schema should not be null")
        assertTrue(intentSchema.intents.isNotEmpty(), "Should have at least one intent")

        val fillFieldIntent = intentSchema.intents.find { it.name.contains("fill") || it.name.contains("enter") }
        assertNotNull(fillFieldIntent, "Should have fill_field intent")

        val submitIntent = intentSchema.intents.find { it.name.contains("submit") || it.name.contains("login") }
        assertNotNull(submitIntent, "Should have submit intent")
    }

    @Test
    fun testQuantizeFormInfo() {
        val formFields = listOf(
            FormField("email", FieldType.EMAIL, "Email", null, null, true, "email field"),
            FormField("password", FieldType.PASSWORD, "Password", null, null, true, "password field"),
            FormField("remember", FieldType.CHECKBOX, "Remember me", null, "false", false, "remember me checkbox")
        )

        val hierarchy = createTestHierarchy(formFields = formFields)
        val quantized = ScreenQuantizer.quantize(hierarchy)

        assertNotNull(quantized.formInfo, "Form info should not be null for form screen")
        assertEquals(3, quantized.formInfo?.fields?.size, "Should have 3 fields")
        assertEquals(2, quantized.formInfo?.requiredFields, "Should have 2 required fields")
    }

    @Test
    fun testQuantizeNoFormInfo() {
        val hierarchy = createTestHierarchy(
            screenType = ScreenType.HOME,
            formFields = emptyList()
        )

        val quantized = ScreenQuantizer.quantize(hierarchy)

        assertNull(quantized.formInfo, "Form info should be null for non-form screen")
    }

    @Test
    fun testQuantizeCommandsLimited() {
        val commandables = (1..50).map { i ->
            CommandableElement("btn$i", "button $i", "Button", "click", emptyList(), priority = i)
        }

        val hierarchy = createTestHierarchy(commandableElements = commandables)
        val quantized = ScreenQuantizer.quantize(hierarchy)

        assertTrue(quantized.commands.size <= 20, "Should limit commands to top 20")
    }

    @Test
    fun testQuantizeCommandsSortedByPriority() {
        val commandables = listOf(
            CommandableElement("btn1", "low priority", "Button", "click", emptyList(), priority = 1),
            CommandableElement("btn2", "high priority", "Button", "click", emptyList(), priority = 100),
            CommandableElement("btn3", "medium priority", "Button", "click", emptyList(), priority = 50)
        )

        val hierarchy = createTestHierarchy(commandableElements = commandables)
        val quantized = ScreenQuantizer.quantize(hierarchy)

        assertEquals("high priority", quantized.commands.first().voiceLabel, "First command should be highest priority")
    }

    @Test
    fun testContextDataFormMode() {
        val formFields = listOf(
            FormField("email", FieldType.EMAIL, "Email", null, null, true, "email field"),
            FormField("password", FieldType.PASSWORD, "Password", null, null, true, "password field")
        )

        val hierarchy = createTestHierarchy(formFields = formFields)
        val quantized = ScreenQuantizer.quantize(hierarchy)

        assertTrue(quantized.context.formMode, "Context should indicate form mode")
    }

    @Test
    fun testContextDataNavigationState() {
        val navContext = NavigationContext(
            currentScreen = "screen2",
            previousScreen = "screen1",
            navigationStack = listOf("screen1", "screen2"),
            canNavigateBack = true,
            canNavigateForward = false
        )

        val root = ComponentNode(
            id = "root",
            type = "Container",
            role = ComponentRole.CONTAINER,
            isInteractive = false,
            depth = 0
        )

        val hierarchy = ScreenHierarchy(
            screenId = "test-screen",
            screenHash = "hash-123",
            screenType = ScreenType.HOME,
            appContext = AppContext("test.app", "Test App", "test.app"),
            navigationContext = navContext,
            root = root,
            commandableElements = emptyList(),
            formFields = emptyList(),
            actions = emptyList(),
            dataDisplay = emptyList(),
            complexity = ComplexityScore(5, 2, 1, 0, 0)
        )

        val quantized = ScreenQuantizer.quantize(hierarchy)

        assertTrue(quantized.context.canGoBack, "Should indicate can go back")
        assertFalse(quantized.context.canGoForward, "Should indicate cannot go forward")
    }

    @Test
    fun testMetadataGeneration() {
        val hierarchy = createTestHierarchy(
            complexity = ComplexityScore(50, 30, 5, 10, 5)
        )

        val quantized = ScreenQuantizer.quantize(hierarchy)

        assertEquals(50, quantized.metadata.totalComponents, "Should record total components")
        assertEquals(30, quantized.metadata.interactiveElements, "Should record interactive count")
        assertEquals(5, quantized.metadata.maxDepth, "Should record max depth")
        assertNotNull(quantized.metadata.quantizationTimestamp, "Should have timestamp")
        assertEquals("1.0", quantized.metadata.version, "Should have version")
    }

    @Test
    fun testComponentGraphGeneration() {
        val child1 = ComponentNode(
            id = "child1",
            type = "Button",
            role = ComponentRole.ACTION,
            voiceLabel = "submit",
            isInteractive = true,
            depth = 1
        )

        val child2 = ComponentNode(
            id = "child2",
            type = "TextField",
            role = ComponentRole.INPUT,
            voiceLabel = "email field",
            isInteractive = true,
            depth = 1
        )

        val root = ComponentNode(
            id = "root",
            type = "Container",
            role = ComponentRole.CONTAINER,
            children = listOf(child1, child2),
            isInteractive = false,
            depth = 0
        )

        val hierarchy = createTestHierarchy().copy(root = root)
        val quantized = ScreenQuantizer.quantize(hierarchy)

        assertEquals(3, quantized.componentGraph.nodes.size, "Should have 3 nodes in graph")

        val rootNode = quantized.componentGraph.nodes.find { it.id == "root" }
        assertNotNull(rootNode, "Should have root node")
        assertEquals(2, rootNode.childCount, "Root should have 2 children")
    }

    @Test
    fun testEmptyHierarchyQuantization() {
        val root = ComponentNode(
            id = "root",
            type = "Container",
            role = ComponentRole.CONTAINER,
            isInteractive = false,
            depth = 0
        )

        val hierarchy = createTestHierarchy().copy(
            root = root,
            commandableElements = emptyList(),
            formFields = emptyList(),
            actions = emptyList()
        )

        val quantized = ScreenQuantizer.quantize(hierarchy)

        assertNotNull(quantized, "Should handle empty hierarchy")
        assertTrue(quantized.commands.isEmpty(), "Should have no commands")
        assertNull(quantized.formInfo, "Should have no form info")
    }
}
