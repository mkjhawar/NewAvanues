package com.augmentalis.voiceoscoreng.handlers

import com.augmentalis.voiceoscoreng.common.Bounds
import com.augmentalis.voiceoscoreng.common.ElementInfo
import com.augmentalis.voiceoscoreng.common.FrameworkType
import com.augmentalis.avid.TypeCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ComposeHandlerTest {

    private val handler = ComposeHandler()

    // ==================== canHandle Tests ====================

    @Test
    fun `canHandle returns true for AndroidComposeView`() {
        val elements = listOf(createAndroidComposeViewElement())
        assertTrue(handler.canHandle(elements))
    }

    @Test
    fun `canHandle returns true for ComposeView`() {
        val elements = listOf(createComposeViewElement())
        assertTrue(handler.canHandle(elements))
    }

    @Test
    fun `canHandle returns false for native only elements`() {
        val elements = listOf(createNativeElement())
        assertFalse(handler.canHandle(elements))
    }

    @Test
    fun `canHandle returns true for androidx compose elements`() {
        val elements = listOf(
            ElementInfo(
                className = "androidx.compose.material.Button",
                bounds = Bounds(0, 0, 100, 50)
            )
        )
        assertTrue(handler.canHandle(elements))
    }

    // ==================== getComposeTypeCode Tests ====================

    @Test
    fun `getComposeTypeCode returns BUTTON for Button role`() {
        val element = ElementInfo(className = "SemanticsNode")
        assertEquals(TypeCode.BUTTON, handler.getComposeTypeCode(element, "Button"))
    }

    @Test
    fun `getComposeTypeCode returns INPUT for TextField`() {
        val element = ElementInfo(className = "TextField")
        assertEquals(TypeCode.INPUT, handler.getComposeTypeCode(element))
    }

    @Test
    fun `getComposeTypeCode returns SCROLL for LazyColumn`() {
        val element = ElementInfo(className = "LazyColumn")
        assertEquals(TypeCode.SCROLL, handler.getComposeTypeCode(element))
    }

    @Test
    fun `getComposeTypeCode returns SCROLL for LazyRow`() {
        val element = ElementInfo(className = "LazyRow")
        assertEquals(TypeCode.SCROLL, handler.getComposeTypeCode(element))
    }

    @Test
    fun `getComposeTypeCode returns SCROLL for LazyGrid`() {
        val element = ElementInfo(className = "LazyGrid")
        assertEquals(TypeCode.SCROLL, handler.getComposeTypeCode(element))
    }

    @Test
    fun `getComposeTypeCode returns CHECKBOX for Checkbox role`() {
        val element = ElementInfo(className = "SemanticsNode")
        assertEquals(TypeCode.CHECKBOX, handler.getComposeTypeCode(element, "Checkbox"))
    }

    @Test
    fun `getComposeTypeCode returns SWITCH for Switch className`() {
        val element = ElementInfo(className = "Switch")
        assertEquals(TypeCode.SWITCH, handler.getComposeTypeCode(element))
    }

    @Test
    fun `getComposeTypeCode returns ELEMENT for unknown type`() {
        val element = ElementInfo(className = "UnknownWidget")
        assertEquals(TypeCode.ELEMENT, handler.getComposeTypeCode(element))
    }

    // ==================== processElements Tests ====================

    @Test
    fun `processElements filters out compose containers`() {
        val elements = listOf(
            createAndroidComposeViewElement(),
            createComposeViewElement(),
            createComposeButtonWithContent()
        )
        val processed = handler.processElements(elements)

        assertEquals(1, processed.size)
        assertTrue(processed[0].text == "Submit")
    }

    @Test
    fun `processElements keeps elements with voice content`() {
        val elements = listOf(
            ElementInfo(
                className = "ComposeText",
                text = "Hello World"
            )
        )
        val processed = handler.processElements(elements)

        assertEquals(1, processed.size)
    }

    @Test
    fun `processElements filters out empty layout containers`() {
        val elements = listOf(
            ElementInfo(className = "Column"),
            ElementInfo(className = "Row"),
            ElementInfo(className = "Box"),
            createComposeButtonWithContent()
        )
        val processed = handler.processElements(elements)

        assertEquals(1, processed.size)
    }

    // ==================== isActionable Tests ====================

    @Test
    fun `isActionable returns true for clickable elements`() {
        val element = ElementInfo(
            className = "ComposeButton",
            isClickable = true
        )
        assertTrue(handler.isActionable(element))
    }

    @Test
    fun `isActionable returns true for long clickable elements`() {
        val element = ElementInfo(
            className = "ComposeCard",
            isLongClickable = true
        )
        assertTrue(handler.isActionable(element))
    }

    @Test
    fun `isActionable returns true for scrollable elements`() {
        val element = ElementInfo(
            className = "LazyColumn",
            isScrollable = true
        )
        assertTrue(handler.isActionable(element))
    }

    @Test
    fun `isActionable returns true for elements with voice content`() {
        val element = ElementInfo(
            className = "ComposeText",
            text = "Click me"
        )
        assertTrue(handler.isActionable(element))
    }

    @Test
    fun `isActionable returns false for non-actionable elements`() {
        val element = ElementInfo(
            className = "Box"
        )
        assertFalse(handler.isActionable(element))
    }

    // ==================== Priority Tests ====================

    @Test
    fun `priority is 90`() {
        assertEquals(90, handler.getPriority())
    }

    // ==================== Framework Type Tests ====================

    @Test
    fun `frameworkType is COMPOSE`() {
        assertEquals(FrameworkType.COMPOSE, handler.frameworkType)
    }

    // ==================== isComposeContainer Tests ====================

    @Test
    fun `isComposeContainer returns true for AndroidComposeView`() {
        val element = createAndroidComposeViewElement()
        assertTrue(handler.isComposeContainer(element))
    }

    @Test
    fun `isComposeContainer returns true for ComposeView`() {
        val element = createComposeViewElement()
        assertTrue(handler.isComposeContainer(element))
    }

    @Test
    fun `isComposeContainer returns false for non-container`() {
        val element = ElementInfo(className = "ComposeButton")
        assertFalse(handler.isComposeContainer(element))
    }

    // ==================== getSelectors Tests ====================

    @Test
    fun `getSelectors returns compose patterns`() {
        val selectors = handler.getSelectors()
        assertTrue(selectors.contains("AndroidComposeView"))
        assertTrue(selectors.contains("ComposeView"))
        assertTrue(selectors.any { it.contains("androidx.compose") })
    }

    // ==================== Helper Methods ====================

    private fun createAndroidComposeViewElement(): ElementInfo {
        return ElementInfo(
            className = "androidx.compose.ui.platform.AndroidComposeView",
            bounds = Bounds(0, 0, 1080, 1920)
        )
    }

    private fun createComposeViewElement(): ElementInfo {
        return ElementInfo(
            className = "ComposeView",
            bounds = Bounds(0, 0, 1080, 1920)
        )
    }

    private fun createComposeButtonWithContent(): ElementInfo {
        return ElementInfo(
            className = "ComposeButton",
            text = "Submit",
            isClickable = true,
            bounds = Bounds(0, 0, 200, 50)
        )
    }

    private fun createNativeElement(): ElementInfo {
        return ElementInfo(
            className = "android.widget.Button",
            text = "Click Me",
            isClickable = true,
            bounds = Bounds(0, 0, 100, 50)
        )
    }
}
