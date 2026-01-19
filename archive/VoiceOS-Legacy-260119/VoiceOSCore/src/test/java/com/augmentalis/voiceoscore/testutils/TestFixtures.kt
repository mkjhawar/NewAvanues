package com.augmentalis.voiceoscore.testutils

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import io.mockk.every
import io.mockk.mockk

/**
 * Shared test fixtures and utilities for VoiceOSCore unit tests.
 * Provides mock factories and test data generators.
 */
object TestFixtures {

    // ============================================================
    // AccessibilityNodeInfo Mocks
    // ============================================================

    /**
     * Creates a mock AccessibilityNodeInfo with configurable properties.
     */
    fun createMockAccessibilityNodeInfo(
        className: String = "android.widget.Button",
        text: CharSequence? = "Click me",
        contentDescription: CharSequence? = null,
        viewIdResourceName: String? = "com.example:id/button1",
        isClickable: Boolean = true,
        isScrollable: Boolean = false,
        isEnabled: Boolean = true,
        isVisibleToUser: Boolean = true,
        bounds: Rect = Rect(0, 0, 100, 50)
    ): AccessibilityNodeInfo = mockk(relaxed = true) {
        every { this@mockk.className } returns className
        every { this@mockk.text } returns text
        every { this@mockk.contentDescription } returns contentDescription
        every { this@mockk.viewIdResourceName } returns viewIdResourceName
        every { this@mockk.isClickable } returns isClickable
        every { this@mockk.isScrollable } returns isScrollable
        every { this@mockk.isEnabled } returns isEnabled
        every { this@mockk.isVisibleToUser } returns isVisibleToUser
        every { getBoundsInScreen(any()) } answers {
            val rect = firstArg<Rect>()
            rect.set(bounds)
        }
    }

    /**
     * Creates a mock button node.
     */
    fun createMockButton(
        text: String = "Button",
        id: String = "button1"
    ): AccessibilityNodeInfo = createMockAccessibilityNodeInfo(
        className = "android.widget.Button",
        text = text,
        viewIdResourceName = "com.example:id/$id",
        isClickable = true
    )

    /**
     * Creates a mock text field node.
     */
    fun createMockTextField(
        text: String = "",
        hint: String = "Enter text",
        id: String = "edittext1"
    ): AccessibilityNodeInfo = createMockAccessibilityNodeInfo(
        className = "android.widget.EditText",
        text = text,
        contentDescription = hint,
        viewIdResourceName = "com.example:id/$id",
        isClickable = true
    )

    /**
     * Creates a mock scrollable container.
     */
    fun createMockScrollableContainer(
        id: String = "recycler1"
    ): AccessibilityNodeInfo = createMockAccessibilityNodeInfo(
        className = "androidx.recyclerview.widget.RecyclerView",
        text = null,
        viewIdResourceName = "com.example:id/$id",
        isClickable = false,
        isScrollable = true
    )

    // ============================================================
    // Element Info Data Classes
    // ============================================================

    /**
     * Simple data class representing an element for testing.
     */
    data class TestElementInfo(
        val id: String,
        val text: String?,
        val className: String,
        val bounds: Rect,
        val isClickable: Boolean = true,
        val isScrollable: Boolean = false,
        val isDangerous: Boolean = false
    )

    /**
     * Creates a standard test element.
     */
    fun createTestElement(
        id: String = "element_1",
        text: String = "Test Element",
        className: String = "android.widget.Button",
        x: Int = 0,
        y: Int = 0,
        width: Int = 100,
        height: Int = 50
    ): TestElementInfo = TestElementInfo(
        id = id,
        text = text,
        className = className,
        bounds = Rect(x, y, x + width, y + height),
        isClickable = true
    )

    /**
     * Creates a list of test elements for batch processing tests.
     */
    fun createTestElementList(count: Int): List<TestElementInfo> =
        (1..count).map { i ->
            createTestElement(
                id = "element_$i",
                text = "Element $i",
                y = (i - 1) * 60
            )
        }

    // ============================================================
    // Command Test Data
    // ============================================================

    /**
     * Standard command patterns for testing.
     */
    object CommandPatterns {
        val CLICK_COMMANDS = listOf("click", "tap", "press", "select")
        val SCROLL_COMMANDS = listOf("scroll", "swipe", "move")
        val INPUT_COMMANDS = listOf("type", "enter", "input", "write")
        val NAVIGATION_COMMANDS = listOf("go to", "navigate", "open")
    }

    /**
     * Dangerous element keywords for testing safety filters.
     */
    object DangerousPatterns {
        val KEYWORDS = listOf(
            "delete", "remove", "uninstall",
            "factory reset", "format", "erase",
            "logout", "sign out", "deactivate"
        )
    }

    // ============================================================
    // Coroutine Test Utilities
    // ============================================================

    /**
     * Default timeout for async operations in tests (milliseconds).
     */
    const val TEST_TIMEOUT_MS = 5000L

    /**
     * Short delay for simulating async operations.
     */
    const val SHORT_DELAY_MS = 100L
}
