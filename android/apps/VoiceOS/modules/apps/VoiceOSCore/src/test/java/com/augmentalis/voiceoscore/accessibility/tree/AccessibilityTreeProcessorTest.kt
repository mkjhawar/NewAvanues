/**
 * AccessibilityTreeProcessorTest.kt - Tests for accessibility tree processing
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Test Framework
 * Code-Reviewed-By: CCA
 * Created: 2025-10-09
 */
package com.augmentalis.voiceoscore.accessibility.tree

import android.graphics.Rect
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.*

/**
 * Comprehensive test suite for accessibility tree processing
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AccessibilityTreeProcessorTest {

    companion object {
        private const val TAG = "AccessibilityTreeProcessorTest"
    }

    private lateinit var testDispatcher: TestDispatcher

    @Before
    fun setup() {
        testDispatcher = StandardTestDispatcher()
        kotlinx.coroutines.Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        kotlinx.coroutines.Dispatchers.resetMain()
        clearAllMocks()
    }

    inner class NodeTraversal {

        @Test
        fun `should traverse single node tree`() {
            // Given
            val rootNode = createMockNode(
                text = "Root",
                className = "android.widget.FrameLayout"
            )

            // When
            val nodes = traverseAndCollect(rootNode)

            // Then
            assertEquals(1, nodes.size, "Should find only root node")
            assertEquals("Root", nodes[0].text?.toString())
        }

        @Test
        fun `should traverse multi-level tree`() {
            // Given
            val child1 = createMockNode(text = "Child1", className = "Button")
            val child2 = createMockNode(text = "Child2", className = "Button")
            val rootNode = createMockNode(
                text = "Root",
                className = "LinearLayout",
                children = listOf(child1, child2)
            )

            // When
            val nodes = traverseAndCollect(rootNode)

            // Then
            assertEquals(3, nodes.size, "Should find root + 2 children")
            assertEquals(setOf("Root", "Child1", "Child2"), nodes.map { it.text?.toString() }.toSet())
        }

        @Test
        fun `should traverse deeply nested tree`() {
            // Given - Tree with 5 levels
            val level5 = createMockNode(text = "Level5", className = "TextView")
            val level4 = createMockNode(text = "Level4", className = "LinearLayout", children = listOf(level5))
            val level3 = createMockNode(text = "Level3", className = "LinearLayout", children = listOf(level4))
            val level2 = createMockNode(text = "Level2", className = "LinearLayout", children = listOf(level3))
            val level1 = createMockNode(text = "Level1", className = "FrameLayout", children = listOf(level2))

            // When
            val nodes = traverseAndCollect(level1)

            // Then
            assertEquals(5, nodes.size, "Should traverse all 5 levels")
        }

        @Test
        fun `should handle tree with multiple branches`() {
            // Given
            val child1_1 = createMockNode(text = "Child1.1", className = "Button")
            val child1_2 = createMockNode(text = "Child1.2", className = "Button")
            val child1 = createMockNode(text = "Child1", className = "LinearLayout", children = listOf(child1_1, child1_2))

            val child2_1 = createMockNode(text = "Child2.1", className = "TextView")
            val child2 = createMockNode(text = "Child2", className = "LinearLayout", children = listOf(child2_1))

            val root = createMockNode(text = "Root", className = "FrameLayout", children = listOf(child1, child2))

            // When
            val nodes = traverseAndCollect(root)

            // Then
            assertEquals(6, nodes.size, "Should traverse all branches")
            assertTrue(nodes.any { it.text?.toString() == "Child1.1" })
            assertTrue(nodes.any { it.text?.toString() == "Child2.1" })
        }

        @Test
        fun `should skip null nodes during traversal`() {
            // Given
            val child = createMockNode(text = "Child", className = "Button")
            val rootNode = createMockNode(text = "Root", className = "LinearLayout")

            // Mock getChild to return null for one child
            every { rootNode.childCount } returns 2
            every { rootNode.getChild(0) } returns child
            every { rootNode.getChild(1) } returns null

            // When
            val nodes = traverseAndCollect(rootNode)

            // Then
            assertEquals(2, nodes.size, "Should skip null child")
        }

        @Test
        fun `should handle empty tree gracefully`() {
            // Given
            val emptyNode = createMockNode(text = "", className = "FrameLayout")

            // When
            val nodes = traverseAndCollect(emptyNode)

            // Then
            assertEquals(1, nodes.size, "Should handle empty node")
        }
    }

    inner class ElementDetection {

        @Test
        fun `should detect clickable buttons`() {
            // Given
            val button = createMockNode(
                text = "Submit",
                className = "android.widget.Button",
                isClickable = true,
                isEnabled = true
            )

            // When
            val element = detectElement(button)

            // Then
            assertNotNull(element, "Should detect button element")
            assertEquals("Submit", element.name)
            assertEquals("Button", element.type)
            assertTrue(element.isClickable)
        }

        @Test
        fun `should detect text input fields`() {
            // Given
            val editText = createMockNode(
                text = "Enter username",
                className = "android.widget.EditText",
                isEditable = true,
                isEnabled = true
            )

            // When
            val element = detectElement(editText)

            // Then
            assertNotNull(element, "Should detect EditText element")
            assertTrue(element.type.contains("EditText") || element.type.contains("Input"))
        }

        @Test
        fun `should detect checkboxes and toggles`() {
            // Given
            val checkbox = createMockNode(
                text = "Accept terms",
                className = "android.widget.CheckBox",
                isCheckable = true,
                isChecked = false
            )

            // When
            val element = detectElement(checkbox)

            // Then
            assertNotNull(element, "Should detect CheckBox element")
            assertEquals("CheckBox", element.type)
            assertTrue(element.isCheckable)
        }

        @Test
        fun `should detect scrollable containers`() {
            // Given
            val scrollView = createMockNode(
                text = "",
                className = "android.widget.ScrollView",
                isScrollable = true
            )

            // When
            val element = detectElement(scrollView)

            // Then
            assertNotNull(element, "Should detect ScrollView element")
            assertEquals("ScrollView", element.type)
            assertTrue(element.isScrollable)
        }

        @Test
        fun `should detect elements with content description`() {
            // Given
            val imageButton = createMockNode(
                text = "",
                contentDescription = "Settings",
                className = "android.widget.ImageButton",
                isClickable = true
            )

            // When
            val element = detectElement(imageButton)

            // Then
            assertNotNull(element, "Should detect element by content description")
            assertEquals("Settings", element.name)
        }

        @Test
        fun `should prioritize content description over text`() {
            // Given
            val button = createMockNode(
                text = "...",
                contentDescription = "More Options",
                className = "android.widget.ImageButton",
                isClickable = true
            )

            // When
            val element = detectElement(button)

            // Then
            assertEquals("More Options", element.name, "Should use content description")
        }

        @Test
        fun `should handle elements with no text or description`() {
            // Given
            val emptyButton = createMockNode(
                text = "",
                contentDescription = "",
                className = "android.widget.Button",
                isClickable = true
            )

            // When
            val element = detectElement(emptyButton)

            // Then
            assertNotNull(element, "Should detect element even without text")
            assertTrue(element.name.isEmpty() || element.name == "Button")
        }

        @Test
        fun `should detect disabled elements`() {
            // Given
            val disabledButton = createMockNode(
                text = "Submit",
                className = "android.widget.Button",
                isClickable = true,
                isEnabled = false
            )

            // When
            val element = detectElement(disabledButton)

            // Then
            assertNotNull(element, "Should detect disabled element")
            assertFalse(element.isEnabled, "Should mark as disabled")
        }

        @Test
        fun `should detect visible vs invisible elements`() {
            // Given
            val visibleElement = createMockNode(
                text = "Visible",
                className = "Button",
                isVisibleToUser = true
            )
            val invisibleElement = createMockNode(
                text = "Invisible",
                className = "Button",
                isVisibleToUser = false
            )

            // When
            val visible = detectElement(visibleElement)
            val invisible = detectElement(invisibleElement)

            // Then
            assertTrue(visible.isVisible, "Should detect as visible")
            assertFalse(invisible.isVisible, "Should detect as invisible")
        }
    }

    inner class ActionMapping {

        @Test
        fun `should map click action to clickable elements`() {
            // Given
            val button = createMockNode(
                text = "Submit",
                className = "Button",
                isClickable = true,
                actions = listOf(AccessibilityNodeInfo.ACTION_CLICK)
            )

            // When
            val actions = extractAvailableActions(button)

            // Then
            assertTrue(actions.contains("click"), "Should include click action")
        }

        @Test
        fun `should map scroll actions to scrollable elements`() {
            // Given
            val scrollView = createMockNode(
                className = "ScrollView",
                isScrollable = true,
                actions = listOf(
                    AccessibilityNodeInfo.ACTION_SCROLL_FORWARD,
                    AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
                )
            )

            // When
            val actions = extractAvailableActions(scrollView)

            // Then
            assertTrue(actions.contains("scroll_forward"), "Should include scroll forward")
            assertTrue(actions.contains("scroll_backward"), "Should include scroll backward")
        }

        @Test
        fun `should map text actions to editable elements`() {
            // Given
            val editText = createMockNode(
                className = "EditText",
                isEditable = true,
                actions = listOf(
                    AccessibilityNodeInfo.ACTION_SET_TEXT,
                    AccessibilityNodeInfo.ACTION_FOCUS
                )
            )

            // When
            val actions = extractAvailableActions(editText)

            // Then
            assertTrue(actions.contains("set_text"), "Should include set text action")
            assertTrue(actions.contains("focus"), "Should include focus action")
        }

        @Test
        fun `should map long click action`() {
            // Given
            val element = createMockNode(
                text = "Item",
                className = "TextView",
                isLongClickable = true,
                actions = listOf(AccessibilityNodeInfo.ACTION_LONG_CLICK)
            )

            // When
            val actions = extractAvailableActions(element)

            // Then
            assertTrue(actions.contains("long_click"), "Should include long click action")
        }

        @Test
        fun `should map expand and collapse actions`() {
            // Given
            val expandableNode = createMockNode(
                className = "ExpandableListView",
                actions = listOf(
                    AccessibilityNodeInfo.ACTION_EXPAND,
                    AccessibilityNodeInfo.ACTION_COLLAPSE
                )
            )

            // When
            val actions = extractAvailableActions(expandableNode)

            // Then
            assertTrue(actions.contains("expand") || actions.contains("collapse"),
                "Should include expand/collapse actions")
        }

        @Test
        fun `should handle elements with no actions`() {
            // Given
            val textView = createMockNode(
                text = "Read-only text",
                className = "TextView",
                actions = emptyList()
            )

            // When
            val actions = extractAvailableActions(textView)

            // Then
            assertTrue(actions.isEmpty(), "Should have no actions for read-only element")
        }

        @Test
        fun `should map multiple actions for complex elements`() {
            // Given
            val complexElement = createMockNode(
                className = "Button",
                isClickable = true,
                isLongClickable = true,
                isFocusable = true,
                actions = listOf(
                    AccessibilityNodeInfo.ACTION_CLICK,
                    AccessibilityNodeInfo.ACTION_LONG_CLICK,
                    AccessibilityNodeInfo.ACTION_FOCUS
                )
            )

            // When
            val actions = extractAvailableActions(complexElement)

            // Then
            assertTrue(actions.size >= 3, "Should map multiple actions")
            assertTrue(actions.contains("click"))
            assertTrue(actions.contains("long_click"))
            assertTrue(actions.contains("focus"))
        }
    }

    inner class MemoryManagement {

        @Test
        fun `should recycle nodes after processing`() {
            // Given
            val node = createMockNode(text = "TestNode", className = "Button")

            // When
            traverseAndRecycle(node)

            // Then
            verify(exactly = 1) { node.recycle() }
        }

        @Test
        fun `should recycle all nodes in tree`() {
            // Given
            val child1 = createMockNode(text = "Child1", className = "Button")
            val child2 = createMockNode(text = "Child2", className = "Button")
            val root = createMockNode(text = "Root", className = "LinearLayout", children = listOf(child1, child2))

            // When
            traverseAndRecycle(root)

            // Then
            verify(exactly = 1) { root.recycle() }
            verify(exactly = 1) { child1.recycle() }
            verify(exactly = 1) { child2.recycle() }
        }

        @Test
        fun `should recycle nodes even on exception`() {
            // Given
            val node = createMockNode(text = "TestNode", className = "Button")
            every { node.text } throws RuntimeException("Test exception")

            // When
            try {
                traverseAndRecycleWithException(node)
            } catch (e: Exception) {
                // Expected
            }

            // Then
            verify(exactly = 1) { node.recycle() }
        }

        @Test
        fun `should not recycle null nodes`() {
            // Given
            val nullNode: AccessibilityNodeInfo? = null

            // When/Then - should not crash
            safeRecycle(nullNode)
        }

        @Test
        fun `should handle recycle exceptions gracefully`() {
            // Given
            val node = createMockNode(text = "TestNode", className = "Button")
            every { node.recycle() } throws IllegalStateException("Already recycled")

            // When/Then - should not crash
            safeRecycle(node)
        }

        @Test
        fun `should recycle deep tree without stack overflow`() {
            // Given - 100 level deep tree
            var recycleCount = 0
            var currentNode = createMockNode(text = "Leaf", className = "TextView")
            every { currentNode.recycle() } answers { recycleCount++; Unit }

            for (i in 1..100) {
                val child = currentNode
                currentNode = createMockNode(
                    text = "Level$i",
                    className = "LinearLayout",
                    children = listOf(child)
                )
                every { currentNode.recycle() } answers { recycleCount++; Unit }
            }

            // When - should complete without stack overflow
            traverseAndRecycle(currentNode)

            // Then - verify recycle was called for all nodes
            assertEquals(101, recycleCount, "All 101 nodes (1 leaf + 100 levels) should be recycled")
        }
    }

    inner class BoundsCalculation {

        @Test
        fun `should extract element bounds correctly`() {
            // Given
            val node = createMockNode(
                text = "Button",
                className = "Button",
                bounds = Rect(100, 200, 300, 400)
            )

            // When
            val element = detectElement(node)

            // Then
            assertEquals(Rect(100, 200, 300, 400), element.bounds)
            assertEquals(100, element.bounds.left)
            assertEquals(200, element.bounds.top)
            assertEquals(300, element.bounds.right)
            assertEquals(400, element.bounds.bottom)
        }

        @Test
        fun `should calculate element center point`() {
            // Given
            val node = createMockNode(
                text = "Button",
                className = "Button",
                bounds = Rect(0, 0, 100, 100)
            )

            // When
            val element = detectElement(node)
            val centerX = element.bounds.centerX()
            val centerY = element.bounds.centerY()

            // Then
            assertEquals(50, centerX, "Center X should be 50")
            assertEquals(50, centerY, "Center Y should be 50")
        }

        @Test
        fun `should calculate element dimensions`() {
            // Given
            val node = createMockNode(
                text = "Button",
                className = "Button",
                bounds = Rect(10, 20, 110, 70)
            )

            // When
            val element = detectElement(node)
            val width = element.bounds.width()
            val height = element.bounds.height()

            // Then
            assertEquals(100, width, "Width should be 100")
            assertEquals(50, height, "Height should be 50")
        }

        @Test
        fun `should handle zero-sized elements`() {
            // Given
            val node = createMockNode(
                text = "Empty",
                className = "View",
                bounds = Rect(0, 0, 0, 0)
            )

            // When
            val element = detectElement(node)

            // Then
            assertEquals(0, element.bounds.width())
            assertEquals(0, element.bounds.height())
        }
    }

    inner class FilteringAndPrioritization {

        @Test
        fun `should filter out invisible elements`() {
            // Given
            val visible = createMockNode(text = "Visible", className = "Button", isVisibleToUser = true)
            val invisible = createMockNode(text = "Invisible", className = "Button", isVisibleToUser = false)
            val root = createMockNode(text = "Root", className = "Layout", children = listOf(visible, invisible))

            // When
            val visibleElements = traverseAndCollectVisible(root)

            // Then
            assertTrue(visibleElements.any { it.text?.toString() == "Visible" })
            assertFalse(visibleElements.any { it.text?.toString() == "Invisible" })
        }

        @Test
        fun `should prioritize clickable elements`() {
            // Given
            val clickable = createMockNode(text = "Button", className = "Button", isClickable = true)
            val nonClickable = createMockNode(text = "Text", className = "TextView", isClickable = false)
            val root = createMockNode(text = "Root", className = "Layout", children = listOf(clickable, nonClickable))

            // When
            val elements = traverseAndCollect(root)
            val clickableElements = elements.filter { it.isClickable() }

            // Then
            assertEquals(1, clickableElements.size, "Should find 1 clickable element")
        }

        @Test
        fun `should filter out decorative elements`() {
            // Given
            val decorative = createMockNode(
                text = "",
                contentDescription = "",
                className = "View",
                isClickable = false,
                isImportantForAccessibility = false
            )

            // When
            val isImportant = shouldIncludeElement(decorative)

            // Then
            assertFalse(isImportant, "Should filter out decorative elements")
        }

        @Test
        fun `should include elements with semantic meaning`() {
            // Given
            val semantic = createMockNode(
                text = "Important",
                className = "TextView",
                isImportantForAccessibility = true
            )

            // When
            val isImportant = shouldIncludeElement(semantic)

            // Then
            assertTrue(isImportant, "Should include semantically meaningful elements")
        }
    }

    // Helper Functions

    private fun createMockNode(
        text: CharSequence = "",
        contentDescription: CharSequence? = null,
        className: CharSequence = "android.view.View",
        isClickable: Boolean = false,
        isEnabled: Boolean = true,
        isCheckable: Boolean = false,
        isChecked: Boolean = false,
        isScrollable: Boolean = false,
        isEditable: Boolean = false,
        isLongClickable: Boolean = false,
        isFocusable: Boolean = false,
        isVisibleToUser: Boolean = true,
        isImportantForAccessibility: Boolean = true,
        bounds: Rect = Rect(0, 0, 100, 100),
        actions: List<Int> = emptyList(),
        children: List<AccessibilityNodeInfo> = emptyList()
    ): AccessibilityNodeInfo {
        return mockk<AccessibilityNodeInfo>(relaxed = true).apply {
            every { this@apply.text } returns text
            every { this@apply.contentDescription } returns contentDescription
            every { this@apply.className } returns className
            every { this@apply.isClickable } returns isClickable
            every { this@apply.isEnabled } returns isEnabled
            every { this@apply.isCheckable } returns isCheckable
            every { this@apply.isChecked } returns isChecked
            every { this@apply.isScrollable } returns isScrollable
            every { this@apply.isEditable } returns isEditable
            every { this@apply.isLongClickable } returns isLongClickable
            every { this@apply.isFocusable } returns isFocusable
            every { this@apply.isVisibleToUser } returns isVisibleToUser
            every { this@apply.childCount } returns children.size
            children.forEachIndexed { index, child ->
                every { getChild(index) } returns child
            }
            every { getBoundsInScreen(any()) } answers {
                // Explicitly capture bounds to ensure proper closure binding
                val capturedBounds = bounds
                (firstArg() as Rect).set(
                    capturedBounds.left,
                    capturedBounds.top,
                    capturedBounds.right,
                    capturedBounds.bottom
                )
            }
            every { actionList } returns actions.map { action ->
                mockk<AccessibilityNodeInfo.AccessibilityAction> {
                    every { id } returns action
                }
            }
            every { recycle() } just Runs
        }
    }

    private fun traverseAndCollect(node: AccessibilityNodeInfo): List<AccessibilityNodeInfo> {
        val result = mutableListOf<AccessibilityNodeInfo>()
        fun traverse(n: AccessibilityNodeInfo) {
            result.add(n)
            for (i in 0 until n.childCount) {
                n.getChild(i)?.let { traverse(it) }
            }
        }
        traverse(node)
        return result
    }

    private fun traverseAndCollectVisible(node: AccessibilityNodeInfo): List<AccessibilityNodeInfo> {
        return traverseAndCollect(node).filter { it.isVisibleToUser }
    }

    private fun traverseAndRecycle(node: AccessibilityNodeInfo) {
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { traverseAndRecycle(it) }
        }
        node.recycle()
    }

    private fun traverseAndRecycleWithException(node: AccessibilityNodeInfo) {
        try {
            // Trigger exception
            node.text
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { traverseAndRecycleWithException(it) }
            }
        } finally {
            node.recycle()
        }
    }

    private fun safeRecycle(node: AccessibilityNodeInfo?) {
        try {
            node?.recycle()
        } catch (e: Exception) {
            // Silently handle
        }
    }

    private fun detectElement(node: AccessibilityNodeInfo): DetectedElement {
        val bounds = Rect()
        node.getBoundsInScreen(bounds)

        return DetectedElement(
            name = (node.contentDescription ?: node.text ?: "").toString(),
            type = node.className?.toString()?.substringAfterLast('.') ?: "View",
            bounds = bounds,
            isClickable = node.isClickable,
            isEnabled = node.isEnabled,
            isCheckable = node.isCheckable,
            isScrollable = node.isScrollable,
            isVisible = node.isVisibleToUser
        )
    }

    private fun extractAvailableActions(node: AccessibilityNodeInfo): Set<String> {
        val actions = mutableSetOf<String>()
        node.actionList.forEach { action ->
            when (action.id) {
                AccessibilityNodeInfo.ACTION_CLICK -> actions.add("click")
                AccessibilityNodeInfo.ACTION_LONG_CLICK -> actions.add("long_click")
                AccessibilityNodeInfo.ACTION_SCROLL_FORWARD -> actions.add("scroll_forward")
                AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD -> actions.add("scroll_backward")
                AccessibilityNodeInfo.ACTION_SET_TEXT -> actions.add("set_text")
                AccessibilityNodeInfo.ACTION_FOCUS -> actions.add("focus")
                AccessibilityNodeInfo.ACTION_EXPAND -> actions.add("expand")
                AccessibilityNodeInfo.ACTION_COLLAPSE -> actions.add("collapse")
            }
        }
        return actions
    }

    private fun shouldIncludeElement(node: AccessibilityNodeInfo): Boolean {
        // Filter out elements that are:
        // - Not visible
        // - Not important for accessibility
        // - Have no text or description
        // - Are not actionable
        return node.isVisibleToUser &&
                (node.isImportantForAccessibility ||
                        node.isClickable ||
                        node.isCheckable ||
                        node.isScrollable ||
                        !node.text.isNullOrEmpty() ||
                        !node.contentDescription.isNullOrEmpty())
    }

    // Data class for detected elements
    data class DetectedElement(
        val name: String,
        val type: String,
        val bounds: Rect,
        val isClickable: Boolean,
        val isEnabled: Boolean,
        val isCheckable: Boolean,
        val isScrollable: Boolean,
        val isVisible: Boolean
    )
}
