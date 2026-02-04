/**
 * ScreenHierarchySerializationTest.kt - Unit Tests for AVU Format Serialization
 *
 * Tests for AVU (Avanues Universal) format serialization and validation.
 *
 * Created: 2025-12-06
 * Part of: Universal Screen Hierarchy System - Test Suite
 *
 * @author IDEACODE v10.3
 */

package com.augmentalis.avaelements.context

import kotlinx.serialization.json.Json
import kotlin.test.*

class ScreenHierarchySerializationTest {

    private val json = Json {
        prettyPrint = false
        ignoreUnknownKeys = true
    }

    private fun createTestHierarchy(
        commandableElements: List<CommandableElement> = emptyList(),
        formFields: List<FormField> = emptyList()
    ): ScreenHierarchy {
        val root = ComponentNode(
            id = "root",
            type = "Container",
            role = ComponentRole.CONTAINER,
            voiceLabel = "main container",
            isInteractive = false,
            depth = 0
        )

        return ScreenHierarchy(
            screenId = "test-screen",
            screenHash = "hash-123",
            screenType = ScreenType.LOGIN,
            screenPurpose = "User authentication",
            primaryAction = PrimaryAction.SUBMIT,
            appContext = AppContext("test.app", "Test App", "test.app", "MainActivity", "productivity"),
            navigationContext = NavigationContext("test-screen", null, listOf("test-screen"), true, false),
            root = root,
            commandableElements = commandableElements,
            formFields = formFields,
            actions = emptyList(),
            dataDisplay = emptyList(),
            complexity = ComplexityScore(10, 5, 3, 2, 1)
        )
    }

    @Test
    fun testToAvuBasic() {
        val hierarchy = createTestHierarchy()
        val avuString = ScreenHierarchySerializer.toAvu(hierarchy, pretty = false)

        assertNotNull(avuString, "AVU string should not be null")
        assertTrue(avuString.isNotEmpty(), "AVU string should not be empty")
        assertTrue(avuString.contains("test-screen"), "Should contain screen ID")
        assertTrue(avuString.contains("LOGIN"), "Should contain screen type")
    }

    @Test
    fun testToAvuPrettyPrint() {
        val hierarchy = createTestHierarchy()
        val avuString = ScreenHierarchySerializer.toAvu(hierarchy, pretty = true)

        assertTrue(avuString.contains("\n"), "Pretty printed AVU should have newlines")
        assertTrue(avuString.contains("  "), "Pretty printed AVU should have indentation")
    }

    @Test
    fun testToAvuCompact() {
        val hierarchy = createTestHierarchy()
        val avuString = ScreenHierarchySerializer.toAvu(hierarchy, pretty = false)

        assertFalse(avuString.contains("\n  "), "Compact AVU should not have indentation")
    }

    @Test
    fun testToAvuWithCommandableElements() {
        val commandables = listOf(
            CommandableElement(
                id = "btn-submit",
                voiceLabel = "submit button",
                componentType = "Button",
                primaryCommand = "click",
                alternateCommands = listOf("tap", "press"),
                parameters = mapOf("action" to "submit"),
                priority = 10
            )
        )

        val hierarchy = createTestHierarchy(commandableElements = commandables)
        val avuString = ScreenHierarchySerializer.toAvu(hierarchy, pretty = false)

        assertTrue(avuString.contains("submit button"), "Should contain commandable element label")
        assertTrue(avuString.contains("click"), "Should contain primary command")
        assertTrue(avuString.contains("tap"), "Should contain alternate commands")
    }

    @Test
    fun testToAvuWithFormFields() {
        val formFields = listOf(
            FormField(
                id = "email-field",
                fieldType = FieldType.EMAIL,
                label = "Email Address",
                placeholder = "Enter email",
                currentValue = "test@example.com",
                isRequired = true,
                voiceLabel = "email field"
            )
        )

        val hierarchy = createTestHierarchy(formFields = formFields)
        val avuString = ScreenHierarchySerializer.toAvu(hierarchy, pretty = false)

        assertTrue(avuString.contains("email-field"), "Should contain field ID")
        assertTrue(avuString.contains("EMAIL"), "Should contain field type")
        assertTrue(avuString.contains("Email Address"), "Should contain label")
        assertTrue(avuString.contains("test@example.com"), "Should contain current value")
    }

    @Test
    fun testToQuantizedAvu() {
        val hierarchy = createTestHierarchy()
        val quantizedAvu = ScreenHierarchySerializer.toQuantizedAvu(hierarchy, pretty = false)

        assertNotNull(quantizedAvu, "Quantized AVU should not be null")
        assertTrue(quantizedAvu.isNotEmpty(), "Quantized AVU should not be empty")
        assertTrue(quantizedAvu.contains("summary"), "Should contain summary field")
        assertTrue(quantizedAvu.contains("screen"), "Should contain screen info")
        assertTrue(quantizedAvu.contains("commands"), "Should contain commands")
    }

    @Test
    fun testToQuantizedAvuPretty() {
        val hierarchy = createTestHierarchy()
        val quantizedAvu = ScreenHierarchySerializer.toQuantizedAvu(hierarchy, pretty = true)

        assertTrue(quantizedAvu.contains("\n"), "Pretty printed quantized AVU should have newlines")
    }

    @Test
    fun testToNLUAvu() {
        val hierarchy = createTestHierarchy()
        val provider = AIContextProvider()
        provider.updateScreen(hierarchy)

        val nluContext = provider.getContextForNLU()
        val nluAvu = ScreenHierarchySerializer.toNLUAvu(nluContext, pretty = false)

        assertNotNull(nluAvu, "NLU AVU should not be null")
        assertTrue(nluAvu.contains("screen"), "Should contain screen info")
        assertTrue(nluAvu.contains("commands"), "Should contain commands")
        assertTrue(nluAvu.contains("entities"), "Should contain entities")
        assertTrue(nluAvu.contains("intents"), "Should contain intents")
    }

    @Test
    fun testComponentTreeToAvu() {
        val child1 = ComponentNode(
            id = "child1",
            type = "Button",
            role = ComponentRole.ACTION,
            voiceLabel = "submit",
            isInteractive = true,
            depth = 1
        )

        val root = ComponentNode(
            id = "root",
            type = "Container",
            role = ComponentRole.CONTAINER,
            children = listOf(child1),
            isInteractive = false,
            depth = 0
        )

        val treeAvu = ScreenHierarchySerializer.componentTreeToAvu(root, pretty = false)

        assertNotNull(treeAvu, "Component tree AVU should not be null")
        assertTrue(treeAvu.contains("root"), "Should contain root ID")
        assertTrue(treeAvu.contains("child1"), "Should contain child ID")
        assertTrue(treeAvu.contains("Container"), "Should contain root type")
        assertTrue(treeAvu.contains("Button"), "Should contain child type")
    }

    @Test
    fun testSerializationPreservesAllFields() {
        val commandables = listOf(
            CommandableElement("btn1", "submit", "Button", "click", listOf("tap"), mapOf("k" to "v"), 10)
        )

        val formFields = listOf(
            FormField("email", FieldType.EMAIL, "Email", "Enter email", "test@test.com", true, "email field")
        )

        val hierarchy = createTestHierarchy(commandableElements = commandables, formFields = formFields)
        val avuString = ScreenHierarchySerializer.toAvu(hierarchy, pretty = false)

        // Verify all major fields are present
        assertTrue(avuString.contains("\"screenId\":\"test-screen\""), "Should preserve screen ID")
        assertTrue(avuString.contains("\"screenHash\":\"hash-123\""), "Should preserve screen hash")
        assertTrue(avuString.contains("\"screenType\":\"LOGIN\""), "Should preserve screen type")
        assertTrue(avuString.contains("\"screenPurpose\":\"User authentication\""), "Should preserve purpose")
        assertTrue(avuString.contains("\"primaryAction\":\"SUBMIT\""), "Should preserve primary action")
        assertTrue(avuString.contains("\"appId\":\"test.app\""), "Should preserve app context")
        assertTrue(avuString.contains("\"commandableElements\""), "Should preserve commandable elements")
        assertTrue(avuString.contains("\"formFields\""), "Should preserve form fields")
    }

    @Test
    fun testSerializationWithComplexTree() {
        val grandchild = ComponentNode(
            id = "grandchild",
            type = "Text",
            role = ComponentRole.DISPLAY,
            text = "Welcome",
            depth = 2
        )

        val child = ComponentNode(
            id = "child",
            type = "Container",
            role = ComponentRole.CONTAINER,
            children = listOf(grandchild),
            depth = 1
        )

        val root = ComponentNode(
            id = "root",
            type = "Container",
            role = ComponentRole.CONTAINER,
            children = listOf(child),
            depth = 0
        )

        val hierarchy = createTestHierarchy().copy(root = root)
        val avuString = ScreenHierarchySerializer.toAvu(hierarchy, pretty = false)

        assertTrue(avuString.contains("root"), "Should contain root")
        assertTrue(avuString.contains("child"), "Should contain child")
        assertTrue(avuString.contains("grandchild"), "Should contain grandchild")
        assertTrue(avuString.contains("Welcome"), "Should contain text content")
    }

    @Test
    fun testSerializationWithBounds() {
        val root = ComponentNode(
            id = "root",
            type = "Container",
            role = ComponentRole.CONTAINER,
            bounds = Rectangle(10f, 20f, 100f, 200f),
            depth = 0
        )

        val hierarchy = createTestHierarchy().copy(root = root)
        val avuString = ScreenHierarchySerializer.toAvu(hierarchy, pretty = false)

        assertTrue(avuString.contains("\"left\":10"), "Should contain left bound")
        assertTrue(avuString.contains("\"top\":20"), "Should contain top bound")
        assertTrue(avuString.contains("\"right\":100"), "Should contain right bound")
        assertTrue(avuString.contains("\"bottom\":200"), "Should contain bottom bound")
    }

    @Test
    fun testSerializationWithNullOptionalFields() {
        val root = ComponentNode(
            id = "root",
            type = "Container",
            role = ComponentRole.CONTAINER,
            bounds = null,
            voiceLabel = null,
            text = null,
            contentDescription = null,
            value = null,
            depth = 0
        )

        val hierarchy = createTestHierarchy().copy(
            root = root,
            screenPurpose = null,
            primaryAction = null
        )

        val avuString = ScreenHierarchySerializer.toAvu(hierarchy, pretty = false)

        // Should not throw exception
        assertNotNull(avuString, "Should handle null optional fields")
        assertTrue(avuString.isNotEmpty(), "Should produce valid JSON")
    }

    @Test
    fun testQuantizedAvuSize() {
        val commandables = (1..50).map { i ->
            CommandableElement("btn$i", "button $i", "Button", "click", emptyList(), priority = i)
        }

        val hierarchy = createTestHierarchy(commandableElements = commandables)

        val fullAvu = ScreenHierarchySerializer.toAvu(hierarchy, pretty = false)
        val quantizedAvu = ScreenHierarchySerializer.toQuantizedAvu(hierarchy, pretty = false)

        assertTrue(quantizedAvu.length < fullAvu.length,
            "Quantized AVU should be smaller than full JSON")
    }

    @Test
    fun testAvuValidStructure() {
        val hierarchy = createTestHierarchy()
        val avuString = ScreenHierarchySerializer.toAvu(hierarchy, pretty = false)

        // Verify it's valid JSON by parsing it
        assertNotNull(json.parseToJsonElement(avuString), "Should be valid AVU format")
    }

    @Test
    fun testQuantizedAvuValidStructure() {
        val hierarchy = createTestHierarchy()
        val quantizedAvu = ScreenHierarchySerializer.toQuantizedAvu(hierarchy, pretty = false)

        // Verify it's valid JSON by parsing it
        assertNotNull(json.parseToJsonElement(quantizedAvu), "Should be valid AVU format")
    }

    @Test
    fun testNLUAvuValidStructure() {
        val hierarchy = createTestHierarchy()
        val provider = AIContextProvider()
        provider.updateScreen(hierarchy)

        val nluContext = provider.getContextForNLU()
        val nluAvu = ScreenHierarchySerializer.toNLUAvu(nluContext, pretty = false)

        // Verify it's valid JSON by parsing it
        assertNotNull(json.parseToJsonElement(nluAvu), "Should be valid AVU format")
    }

    @Test
    fun testSerializationDoesNotLoseData() {
        val commandables = listOf(
            CommandableElement("btn1", "submit", "Button", "click", listOf("tap", "press"), mapOf("a" to "b"), 10)
        )

        val hierarchy = createTestHierarchy(commandableElements = commandables)
        val avuString = ScreenHierarchySerializer.toAvu(hierarchy, pretty = false)

        // Verify critical data is preserved
        assertTrue(avuString.contains("submit"), "Should preserve voice label")
        assertTrue(avuString.contains("click"), "Should preserve primary command")
        assertTrue(avuString.contains("tap"), "Should preserve alternate commands")
        assertTrue(avuString.contains("press"), "Should preserve all alternate commands")
        assertTrue(avuString.contains("\"priority\":10"), "Should preserve priority")
    }
}
