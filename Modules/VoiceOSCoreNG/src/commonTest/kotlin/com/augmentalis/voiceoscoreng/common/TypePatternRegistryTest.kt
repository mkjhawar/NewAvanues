package com.augmentalis.voiceoscoreng.common

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for TypePatternRegistry, TypePatternProvider interface,
 * and default providers (ComposePatternProvider, NativePatternProvider).
 */
class TypePatternRegistryTest {

    @BeforeTest
    fun setUp() {
        // Clear and register defaults before each test
        TypePatternRegistry.clear()
        TypePatternRegistry.registerDefaults()
    }

    // ========================================
    // Compose Pattern Tests
    // ========================================

    @Test
    fun `getTypeCode returns BUTTON for IconButton`() {
        val result = TypePatternRegistry.getTypeCode("IconButton")
        assertEquals(VUIDTypeCode.BUTTON, result)
    }

    @Test
    fun `getTypeCode returns INPUT for OutlinedTextField`() {
        val result = TypePatternRegistry.getTypeCode("OutlinedTextField")
        assertEquals(VUIDTypeCode.INPUT, result)
    }

    @Test
    fun `getTypeCode returns SCROLL for LazyColumn`() {
        val result = TypePatternRegistry.getTypeCode("LazyColumn")
        assertEquals(VUIDTypeCode.SCROLL, result)
    }

    @Test
    fun `getTypeCode returns TAB for TabRow`() {
        val result = TypePatternRegistry.getTypeCode("TabRow")
        assertEquals(VUIDTypeCode.TAB, result)
    }

    @Test
    fun `getTypeCode returns MENU for NavigationBar`() {
        val result = TypePatternRegistry.getTypeCode("NavigationBar")
        assertEquals(VUIDTypeCode.MENU, result)
    }

    @Test
    fun `getTypeCode returns BUTTON for FilterChip`() {
        val result = TypePatternRegistry.getTypeCode("FilterChip")
        assertEquals(VUIDTypeCode.BUTTON, result)
    }

    // ========================================
    // Native Pattern Tests
    // ========================================

    @Test
    fun `getTypeCode returns BUTTON for native Button`() {
        val result = TypePatternRegistry.getTypeCode("Button")
        assertEquals(VUIDTypeCode.BUTTON, result)
    }

    @Test
    fun `getTypeCode returns INPUT for EditText`() {
        val result = TypePatternRegistry.getTypeCode("EditText")
        assertEquals(VUIDTypeCode.INPUT, result)
    }

    // ========================================
    // Fallback Tests
    // ========================================

    @Test
    fun `getTypeCode returns ELEMENT for unknown class`() {
        val result = TypePatternRegistry.getTypeCode("SomeUnknownCustomWidget")
        assertEquals(VUIDTypeCode.ELEMENT, result)
    }

    // ========================================
    // Priority Tests
    // ========================================

    @Test
    fun `Compose patterns take priority over Native patterns`() {
        // Verify Compose provider has higher priority than Native
        val providers = TypePatternRegistry.getProviders()

        val composeProvider = providers.find { it.name == "Compose" }
        val nativeProvider = providers.find { it.name == "Native" }

        assertTrue(composeProvider != null, "Compose provider should be registered")
        assertTrue(nativeProvider != null, "Native provider should be registered")
        assertTrue(
            composeProvider.priority > nativeProvider.priority,
            "Compose priority (${composeProvider.priority}) should be greater than Native priority (${nativeProvider.priority})"
        )

        // Verify Compose appears before Native in the list (higher priority first)
        val composeIndex = providers.indexOf(composeProvider)
        val nativeIndex = providers.indexOf(nativeProvider)
        assertTrue(
            composeIndex < nativeIndex,
            "Compose should be checked before Native (index $composeIndex < $nativeIndex)"
        )
    }

    // ========================================
    // Registry Management Tests
    // ========================================

    @Test
    fun `registerDefaults registers both providers`() {
        val providers = TypePatternRegistry.getProviders()

        assertEquals(2, providers.size, "Should have exactly 2 providers registered")

        val providerNames = providers.map { it.name }
        assertTrue(providerNames.contains("Compose"), "Should contain Compose provider")
        assertTrue(providerNames.contains("Native"), "Should contain Native provider")
    }

    @Test
    fun `clear removes all providers`() {
        // Verify providers exist before clear
        assertTrue(TypePatternRegistry.getProviders().isNotEmpty(), "Providers should exist before clear")

        // Clear the registry
        TypePatternRegistry.clear()

        // Verify no providers remain
        assertTrue(TypePatternRegistry.getProviders().isEmpty(), "Providers should be empty after clear")

        // Verify getTypeCode returns ELEMENT (default) when no providers
        val result = TypePatternRegistry.getTypeCode("Button")
        assertEquals(VUIDTypeCode.ELEMENT, result, "Should return ELEMENT when no providers are registered")
    }

    // ========================================
    // Edge Cases
    // ========================================

    @Test
    fun `getTypeCode handles empty string`() {
        val result = TypePatternRegistry.getTypeCode("")
        assertEquals(VUIDTypeCode.ELEMENT, result)
    }

    @Test
    fun `getTypeCode handles whitespace string`() {
        val result = TypePatternRegistry.getTypeCode("   ")
        assertEquals(VUIDTypeCode.ELEMENT, result)
    }

    @Test
    fun `getTypeCode is case insensitive`() {
        // Test various case combinations
        assertEquals(VUIDTypeCode.BUTTON, TypePatternRegistry.getTypeCode("ICONBUTTON"))
        assertEquals(VUIDTypeCode.BUTTON, TypePatternRegistry.getTypeCode("iconbutton"))
        assertEquals(VUIDTypeCode.BUTTON, TypePatternRegistry.getTypeCode("IconButton"))
        assertEquals(VUIDTypeCode.BUTTON, TypePatternRegistry.getTypeCode("iCoNbUtToN"))
    }

    @Test
    fun `getTypeCode handles class name with package prefix`() {
        // The pattern matching uses contains(), so partial matches work
        val result = TypePatternRegistry.getTypeCode("androidx.compose.material3.IconButton")
        assertEquals(VUIDTypeCode.BUTTON, result)
    }
}
