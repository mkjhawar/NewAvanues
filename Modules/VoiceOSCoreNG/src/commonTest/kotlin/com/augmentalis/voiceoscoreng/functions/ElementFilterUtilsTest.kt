package com.augmentalis.voiceoscoreng.functions

import com.augmentalis.voiceoscoreng.common.Bounds
import com.augmentalis.voiceoscoreng.common.ElementInfo
import com.augmentalis.voiceoscoreng.common.FrameworkInfo
import com.augmentalis.voiceoscoreng.common.FrameworkType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ElementFilterUtilsTest {

    // ==================== Test Data ====================

    private fun createButton(text: String, isClickable: Boolean = true) = ElementInfo(
        className = "Button",
        text = text,
        isClickable = isClickable,
        isEnabled = true
    )

    private fun createTextView(text: String) = ElementInfo(
        className = "TextView",
        text = text,
        isClickable = false
    )

    private fun createScrollView(contentDescription: String = "") = ElementInfo(
        className = "ScrollView",
        contentDescription = contentDescription,
        isScrollable = true
    )

    private fun createSystemElement(className: String, packageName: String) = ElementInfo(
        className = className,
        packageName = packageName
    )

    private fun createContainer(className: String) = ElementInfo(
        className = "android.widget.$className"
    )

    private fun createElementWithBounds(
        text: String,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int
    ) = ElementInfo(
        className = "Button",
        text = text,
        bounds = Bounds(left, top, right, bottom),
        isClickable = true
    )

    // ==================== Actionable Filter Tests ====================

    @Test
    fun `filterActionable returns only clickable and scrollable elements`() {
        val elements = listOf(
            createButton("Submit"),
            createTextView("Label"),
            createScrollView(),
            ElementInfo(className = "View")
        )

        val result = ElementFilterUtils.filterActionable(elements)

        assertEquals(2, result.size)
        assertTrue(result.all { it.isActionable })
    }

    @Test
    fun `filterActionable returns empty list when no actionable elements`() {
        val elements = listOf(
            createTextView("Label 1"),
            createTextView("Label 2"),
            ElementInfo(className = "ImageView")
        )

        val result = ElementFilterUtils.filterActionable(elements)

        assertTrue(result.isEmpty())
    }

    // ==================== Voice Content Filter Tests ====================

    @Test
    fun `filterWithVoiceContent returns elements with text`() {
        val elements = listOf(
            createButton("Submit"),
            ElementInfo(className = "View")
        )

        val result = ElementFilterUtils.filterWithVoiceContent(elements)

        assertEquals(1, result.size)
        assertEquals("Submit", result[0].text)
    }

    @Test
    fun `filterWithVoiceContent returns elements with content description`() {
        val elements = listOf(
            createScrollView("Scroll area"),
            ElementInfo(className = "View")
        )

        val result = ElementFilterUtils.filterWithVoiceContent(elements)

        assertEquals(1, result.size)
        assertEquals("Scroll area", result[0].contentDescription)
    }

    @Test
    fun `filterWithVoiceContent returns elements with resource ID`() {
        val elements = listOf(
            ElementInfo(className = "ImageView", resourceId = "com.app:id/icon"),
            ElementInfo(className = "View")
        )

        val result = ElementFilterUtils.filterWithVoiceContent(elements)

        assertEquals(1, result.size)
    }

    // ==================== System Element Filter Tests ====================

    @Test
    fun `filterOutSystemElements removes system UI elements`() {
        val elements = listOf(
            createButton("OK"),
            createSystemElement("StatusBarWindowView", "com.android.systemui"),
            createSystemElement("NavigationBarView", "com.android.systemui")
        )

        val result = ElementFilterUtils.filterOutSystemElements(elements)

        assertEquals(1, result.size)
        assertEquals("OK", result[0].text)
    }

    @Test
    fun `isSystemElement returns true for system packages`() {
        val systemElement = createSystemElement("View", "com.android.systemui")
        assertTrue(ElementFilterUtils.isSystemElement(systemElement))
    }

    @Test
    fun `isSystemElement returns true for system class names`() {
        val statusBar = ElementInfo(
            className = "com.android.systemui.StatusBarWindowView",
            packageName = "com.example.app"
        )
        assertTrue(ElementFilterUtils.isSystemElement(statusBar))
    }

    @Test
    fun `isSystemElement returns false for app elements`() {
        val appElement = createButton("Submit")
        assertFalse(ElementFilterUtils.isSystemElement(appElement))
    }

    // ==================== Voice Commands Filter Tests ====================

    @Test
    fun `filterForVoiceCommands returns actionable elements with content`() {
        val elements = listOf(
            createButton("Submit"),
            createTextView("Label"),
            createScrollView("Scroll area"),
            ElementInfo(className = "View", isClickable = true), // clickable but no content
            createSystemElement("View", "com.android.systemui")
        )

        val result = ElementFilterUtils.filterForVoiceCommands(elements)

        assertEquals(2, result.size)
        assertTrue(result.any { it.text == "Submit" })
        assertTrue(result.any { it.contentDescription == "Scroll area" })
    }

    // ==================== Container Element Tests ====================

    @Test
    fun `isContainerElement returns true for layout containers`() {
        val containers = listOf("FrameLayout", "LinearLayout", "RelativeLayout", "ConstraintLayout")

        for (containerName in containers) {
            val element = createContainer(containerName)
            assertTrue(ElementFilterUtils.isContainerElement(element), "$containerName should be container")
        }
    }

    @Test
    fun `isContainerElement returns false for clickable containers`() {
        val element = ElementInfo(
            className = "android.widget.LinearLayout",
            isClickable = true
        )
        assertFalse(ElementFilterUtils.isContainerElement(element))
    }

    @Test
    fun `isContainerElement returns false for containers with content`() {
        val element = ElementInfo(
            className = "android.widget.FrameLayout",
            text = "Content"
        )
        assertFalse(ElementFilterUtils.isContainerElement(element))
    }

    // ==================== Group By Label Tests ====================

    @Test
    fun `groupByVoiceLabel groups elements correctly`() {
        val elements = listOf(
            createButton("OK"),
            createButton("Cancel"),
            createButton("OK") // duplicate
        )

        val grouped = ElementFilterUtils.groupByVoiceLabel(elements)

        assertEquals(2, grouped.size)
        assertEquals(2, grouped["OK"]?.size)
        assertEquals(1, grouped["Cancel"]?.size)
    }

    @Test
    fun `findDuplicateLabels returns elements with shared labels`() {
        val elements = listOf(
            createButton("OK"),
            createButton("Cancel"),
            createButton("OK")
        )

        val duplicates = ElementFilterUtils.findDuplicateLabels(elements)

        assertEquals(2, duplicates.size)
        assertTrue(duplicates.all { it.text == "OK" })
    }

    @Test
    fun `findDuplicateLabels returns empty when no duplicates`() {
        val elements = listOf(
            createButton("OK"),
            createButton("Cancel"),
            createButton("Submit")
        )

        val duplicates = ElementFilterUtils.findDuplicateLabels(elements)

        assertTrue(duplicates.isEmpty())
    }

    // ==================== Region Filter Tests ====================

    @Test
    fun `filterInRegion returns elements within bounds`() {
        val elements = listOf(
            createElementWithBounds("Inside", 50, 50, 150, 150),
            createElementWithBounds("Outside", 200, 200, 300, 300)
        )

        val result = ElementFilterUtils.filterInRegion(elements, 0, 0, 100, 100)

        assertEquals(1, result.size)
        assertEquals("Inside", result[0].text)
    }

    @Test
    fun `filterInRegion includes partially overlapping elements`() {
        val elements = listOf(
            createElementWithBounds("Partial", 50, 50, 150, 150)
        )

        val result = ElementFilterUtils.filterInRegion(elements, 100, 100, 200, 200)

        assertEquals(1, result.size)
    }

    @Test
    fun `filterInRegion excludes non-overlapping elements`() {
        val elements = listOf(
            createElementWithBounds("NoOverlap", 200, 200, 300, 300)
        )

        val result = ElementFilterUtils.filterInRegion(elements, 0, 0, 100, 100)

        assertTrue(result.isEmpty())
    }

    // ==================== Sort By Position Tests ====================

    @Test
    fun `sortByPosition orders top to bottom then left to right`() {
        val elements = listOf(
            createElementWithBounds("Bottom-Right", 100, 200, 200, 300),
            createElementWithBounds("Top-Left", 0, 0, 100, 100),
            createElementWithBounds("Top-Right", 100, 0, 200, 100),
            createElementWithBounds("Bottom-Left", 0, 200, 100, 300)
        )

        val sorted = ElementFilterUtils.sortByPosition(elements)

        assertEquals("Top-Left", sorted[0].text)
        assertEquals("Top-Right", sorted[1].text)
        assertEquals("Bottom-Left", sorted[2].text)
        assertEquals("Bottom-Right", sorted[3].text)
    }

    // ==================== Framework Filter Tests ====================

    @Test
    fun `filterForFramework with Flutter filters platform views without content`() {
        val elements = listOf(
            ElementInfo(className = "FlutterPlatformView", text = "Content"),
            ElementInfo(className = "FlutterPlatformView")
        )
        val frameworkInfo = FrameworkInfo(FrameworkType.FLUTTER, "3.0", emptyList())

        val result = ElementFilterUtils.filterForFramework(elements, frameworkInfo)

        assertEquals(1, result.size)
        assertEquals("Content", result[0].text)
    }

    @Test
    fun `filterForFramework with ReactNative keeps actionable or content elements`() {
        val elements = listOf(
            ElementInfo(className = "ReactTextView", text = "Text"),
            ElementInfo(className = "ReactViewGroup", isClickable = true),
            ElementInfo(className = "ReactViewGroup")
        )
        val frameworkInfo = FrameworkInfo(FrameworkType.REACT_NATIVE, null, emptyList())

        val result = ElementFilterUtils.filterForFramework(elements, frameworkInfo)

        assertEquals(2, result.size)
    }

    @Test
    fun `filterForFramework with Native returns all elements`() {
        val elements = listOf(
            createButton("Button"),
            createTextView("Text")
        )
        val frameworkInfo = FrameworkInfo(FrameworkType.NATIVE, null, emptyList())

        val result = ElementFilterUtils.filterForFramework(elements, frameworkInfo)

        assertEquals(2, result.size)
    }

    @Test
    fun `filterForFramework with WebView filters empty containers`() {
        val elements = listOf(
            ElementInfo(className = "WebView", text = "Web content"),
            ElementInfo(className = "WebView"),
            createButton("Button")
        )
        val frameworkInfo = FrameworkInfo(FrameworkType.WEBVIEW, null, emptyList())

        val result = ElementFilterUtils.filterForFramework(elements, frameworkInfo)

        assertEquals(2, result.size)
        assertTrue(result.any { it.text == "Web content" })
        assertTrue(result.any { it.text == "Button" })
    }
}
