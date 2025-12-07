/**
 * ElementClassifierExplorationBehaviorTest.kt - Unit tests for exploration behavior classification
 * Path: test/.../learnapp/elements/ElementClassifierExplorationBehaviorTest.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-12-04
 *
 * Tests the classifyExplorationBehavior() method for all 13 behavior types.
 * Tier 1 Enhancement: Validates classification logic and priority ordering.
 */

package com.augmentalis.voiceoscore.learnapp.elements

import android.content.Context
import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.voiceoscore.learnapp.models.ExplorationBehavior
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Test suite for ExplorationBehavior classification
 *
 * Tests all 13 behavior types:
 * - CLICKABLE (Priority 1)
 * - MENU_TRIGGER (Priority 1)
 * - TAB (Priority 1)
 * - DRAWER (Priority 2)
 * - DROPDOWN (Priority 2)
 * - BOTTOM_SHEET (Priority 2)
 * - SCROLLABLE (Priority 3)
 * - CHIP_GROUP (Priority 3)
 * - COLLAPSING_TOOLBAR (Priority 3)
 * - EXPANDABLE (Priority 4)
 * - LONG_CLICKABLE (Priority 5)
 * - CONTAINER (Priority 6)
 * - SKIP (Priority 7)
 */
class ElementClassifierExplorationBehaviorTest {

    private lateinit var classifier: ElementClassifier
    private lateinit var mockContext: Context

    @Before
    fun setup() {
        mockContext = mockk(relaxed = true)
        classifier = ElementClassifier(mockContext)
    }

    // ========================================================================
    // Priority 1: Direct Interactions
    // ========================================================================

    @Test
    fun `test MENU_TRIGGER - overflow menu by resource ID`() {
        val node = createMockNode(
            className = "android.widget.ImageButton",
            resourceId = "com.example:id/more_options",
            isClickable = true,
            isEnabled = true
        )

        val behavior = classifier.classifyExplorationBehavior(node)
        assertEquals(ExplorationBehavior.MENU_TRIGGER, behavior)
    }

    @Test
    fun `test MENU_TRIGGER - hamburger menu by content description`() {
        val node = createMockNode(
            className = "android.widget.ImageButton",
            resourceId = "",
            contentDescription = "Navigation menu",
            isClickable = true,
            isEnabled = true
        )

        val behavior = classifier.classifyExplorationBehavior(node)
        assertEquals(ExplorationBehavior.MENU_TRIGGER, behavior)
    }

    @Test
    fun `test MENU_TRIGGER - ActionMenuView`() {
        val node = createMockNode(
            className = "androidx.appcompat.widget.ActionMenuView",
            isClickable = true,
            isEnabled = true
        )

        val behavior = classifier.classifyExplorationBehavior(node)
        assertEquals(ExplorationBehavior.MENU_TRIGGER, behavior)
    }

    @Test
    fun `test TAB - BottomNavigationItemView`() {
        val node = createMockNode(
            className = "com.google.android.material.bottomnavigation.BottomNavigationItemView",
            isClickable = true,
            isEnabled = true
        )

        val behavior = classifier.classifyExplorationBehavior(node)
        assertEquals(ExplorationBehavior.TAB, behavior)
    }

    @Test
    fun `test TAB - TabLayout`() {
        val node = createMockNode(
            className = "com.google.android.material.tabs.TabLayout",
            isClickable = true,
            isEnabled = true
        )

        val behavior = classifier.classifyExplorationBehavior(node)
        assertEquals(ExplorationBehavior.TAB, behavior)
    }

    @Test
    fun `test CLICKABLE - standard button`() {
        val node = createMockNode(
            className = "android.widget.Button",
            text = "Submit",
            isClickable = true,
            isEnabled = true
        )

        val behavior = classifier.classifyExplorationBehavior(node)
        assertEquals(ExplorationBehavior.CLICKABLE, behavior)
    }

    @Test
    fun `test CLICKABLE - ImageView`() {
        val node = createMockNode(
            className = "android.widget.ImageView",
            contentDescription = "Like button",
            isClickable = true,
            isEnabled = true
        )

        val behavior = classifier.classifyExplorationBehavior(node)
        assertEquals(ExplorationBehavior.CLICKABLE, behavior)
    }

    // ========================================================================
    // Priority 2: Expandable UI
    // ========================================================================

    @Test
    fun `test DRAWER - DrawerLayout`() {
        val node = createMockNode(
            className = "androidx.drawerlayout.widget.DrawerLayout",
            isClickable = false,
            isEnabled = true,
            childCount = 2
        )

        val behavior = classifier.classifyExplorationBehavior(node)
        assertEquals(ExplorationBehavior.DRAWER, behavior)
    }

    @Test
    fun `test DRAWER - NavigationView`() {
        val node = createMockNode(
            className = "com.google.android.material.navigation.NavigationView",
            isClickable = false,
            isEnabled = true
        )

        val behavior = classifier.classifyExplorationBehavior(node)
        assertEquals(ExplorationBehavior.DRAWER, behavior)
    }

    @Test
    fun `test DROPDOWN - Spinner`() {
        val node = createMockNode(
            className = "android.widget.Spinner",
            isClickable = true,
            isEnabled = true
        )

        val behavior = classifier.classifyExplorationBehavior(node)
        assertEquals(ExplorationBehavior.DROPDOWN, behavior)
    }

    @Test
    fun `test DROPDOWN - AutoCompleteTextView`() {
        val node = createMockNode(
            className = "android.widget.AutoCompleteTextView",
            isClickable = true,
            isEnabled = true
        )

        val behavior = classifier.classifyExplorationBehavior(node)
        assertEquals(ExplorationBehavior.DROPDOWN, behavior)
    }

    @Test
    fun `test BOTTOM_SHEET - by className`() {
        val node = createMockNode(
            className = "com.google.android.material.bottomsheet.BottomSheetBehavior",
            isClickable = false,
            isEnabled = true
        )

        val behavior = classifier.classifyExplorationBehavior(node)
        assertEquals(ExplorationBehavior.BOTTOM_SHEET, behavior)
    }

    @Test
    fun `test BOTTOM_SHEET - by resource ID`() {
        val node = createMockNode(
            className = "android.widget.FrameLayout",
            resourceId = "com.example:id/bottom_sheet",
            isClickable = false,
            isEnabled = true
        )

        val behavior = classifier.classifyExplorationBehavior(node)
        assertEquals(ExplorationBehavior.BOTTOM_SHEET, behavior)
    }

    // ========================================================================
    // Priority 3: Content Discovery
    // ========================================================================

    @Test
    fun `test SCROLLABLE - RecyclerView`() {
        val node = createMockNode(
            className = "androidx.recyclerview.widget.RecyclerView",
            isClickable = false,
            isScrollable = true,
            childCount = 10
        )

        val behavior = classifier.classifyExplorationBehavior(node)
        assertEquals(ExplorationBehavior.SCROLLABLE, behavior)
    }

    @Test
    fun `test SCROLLABLE - ListView`() {
        val node = createMockNode(
            className = "android.widget.ListView",
            isClickable = false,
            isScrollable = true,
            childCount = 8
        )

        val behavior = classifier.classifyExplorationBehavior(node)
        assertEquals(ExplorationBehavior.SCROLLABLE, behavior)
    }

    @Test
    fun `test SCROLLABLE - ScrollView`() {
        val node = createMockNode(
            className = "android.widget.ScrollView",
            isClickable = false,
            isScrollable = true,
            childCount = 1
        )

        val behavior = classifier.classifyExplorationBehavior(node)
        assertEquals(ExplorationBehavior.SCROLLABLE, behavior)
    }

    @Test
    fun `test SCROLLABLE - ViewPager2`() {
        val node = createMockNode(
            className = "androidx.viewpager2.widget.ViewPager2",
            isClickable = false,
            isScrollable = false,
            childCount = 3
        )

        val behavior = classifier.classifyExplorationBehavior(node)
        assertEquals(ExplorationBehavior.SCROLLABLE, behavior)
    }

    @Test
    fun `test SCROLLABLE - isScrollable flag`() {
        val node = createMockNode(
            className = "android.view.ViewGroup",
            isClickable = false,
            isScrollable = true,
            childCount = 5
        )

        val behavior = classifier.classifyExplorationBehavior(node)
        assertEquals(ExplorationBehavior.SCROLLABLE, behavior)
    }

    @Test
    fun `test CHIP_GROUP - ChipGroup`() {
        val node = createMockNode(
            className = "com.google.android.material.chip.ChipGroup",
            isClickable = false,
            isScrollable = false,
            childCount = 5
        )

        val behavior = classifier.classifyExplorationBehavior(node)
        assertEquals(ExplorationBehavior.CHIP_GROUP, behavior)
    }

    @Test
    fun `test COLLAPSING_TOOLBAR - CollapsingToolbarLayout`() {
        val node = createMockNode(
            className = "com.google.android.material.appbar.CollapsingToolbarLayout",
            isClickable = false,
            childCount = 2
        )

        val behavior = classifier.classifyExplorationBehavior(node)
        assertEquals(ExplorationBehavior.COLLAPSING_TOOLBAR, behavior)
    }

    @Test
    fun `test COLLAPSING_TOOLBAR - AppBarLayout`() {
        val node = createMockNode(
            className = "com.google.android.material.appbar.AppBarLayout",
            isClickable = false,
            childCount = 1
        )

        val behavior = classifier.classifyExplorationBehavior(node)
        assertEquals(ExplorationBehavior.COLLAPSING_TOOLBAR, behavior)
    }

    // ========================================================================
    // Priority 4: Secondary Interactions
    // ========================================================================

    @Test
    fun `test EXPANDABLE - ExpandableListView`() {
        val node = createMockNode(
            className = "android.widget.ExpandableListView",
            isClickable = true,
            childCount = 3
        )

        val behavior = classifier.classifyExplorationBehavior(node)
        assertEquals(ExplorationBehavior.EXPANDABLE, behavior)
    }

    @Test
    fun `test EXPANDABLE - element with expand resourceId`() {
        val node = createMockNode(
            className = "android.widget.LinearLayout",
            resourceId = "com.example:id/expand_button",
            isClickable = true,
            childCount = 2
        )

        val behavior = classifier.classifyExplorationBehavior(node)
        assertEquals(ExplorationBehavior.EXPANDABLE, behavior)
    }

    @Test
    fun `test EXPANDABLE - element with expand contentDescription`() {
        val node = createMockNode(
            className = "android.widget.FrameLayout",
            contentDescription = "Expand details",
            isClickable = true,
            childCount = 1
        )

        val behavior = classifier.classifyExplorationBehavior(node)
        assertEquals(ExplorationBehavior.EXPANDABLE, behavior)
    }

    // ========================================================================
    // Priority 5: Special Interactions
    // ========================================================================

    @Test
    fun `test LONG_CLICKABLE - long press only (not clickable)`() {
        val node = createMockNode(
            className = "android.widget.TextView",
            text = "Long press me",
            isClickable = false,
            isLongClickable = true
        )

        val behavior = classifier.classifyExplorationBehavior(node)
        assertEquals(ExplorationBehavior.LONG_CLICKABLE, behavior)
    }

    @Test
    fun `test LONG_CLICKABLE - long clickable takes priority over container`() {
        val node = createMockNode(
            className = "android.widget.LinearLayout",
            isClickable = false,
            isLongClickable = true,
            childCount = 3
        )

        val behavior = classifier.classifyExplorationBehavior(node)
        assertEquals(ExplorationBehavior.LONG_CLICKABLE, behavior)
    }

    // ========================================================================
    // Priority 6: Container Exploration
    // ========================================================================

    @Test
    fun `test CONTAINER - ViewGroup with children`() {
        val node = createMockNode(
            className = "android.widget.LinearLayout",
            isClickable = false,
            childCount = 4
        )

        val behavior = classifier.classifyExplorationBehavior(node)
        assertEquals(ExplorationBehavior.CONTAINER, behavior)
    }

    @Test
    fun `test CONTAINER - FrameLayout with children`() {
        val node = createMockNode(
            className = "android.widget.FrameLayout",
            isClickable = false,
            childCount = 2
        )

        val behavior = classifier.classifyExplorationBehavior(node)
        assertEquals(ExplorationBehavior.CONTAINER, behavior)
    }

    // ========================================================================
    // Priority 7: Skip (Non-Interactive)
    // ========================================================================

    @Test
    fun `test SKIP - disabled element`() {
        val node = createMockNode(
            className = "android.widget.Button",
            text = "Disabled Button",
            isClickable = true,
            isEnabled = false
        )

        val behavior = classifier.classifyExplorationBehavior(node)
        assertEquals(ExplorationBehavior.SKIP, behavior)
    }

    @Test
    fun `test SKIP - EditText field`() {
        val node = createMockNode(
            className = "android.widget.EditText",
            text = "",
            isClickable = true,
            isEnabled = true
        )

        val behavior = classifier.classifyExplorationBehavior(node)
        assertEquals(ExplorationBehavior.SKIP, behavior)
    }

    @Test
    fun `test SKIP - TextInputEditText (Material Design)`() {
        val node = createMockNode(
            className = "com.google.android.material.textfield.TextInputEditText",
            text = "",
            isClickable = true,
            isEnabled = true
        )

        val behavior = classifier.classifyExplorationBehavior(node)
        assertEquals(ExplorationBehavior.SKIP, behavior)
    }

    @Test
    fun `test SKIP - decorative TextView`() {
        val node = createMockNode(
            className = "android.widget.TextView",
            text = "Static Label",
            isClickable = false,
            childCount = 0
        )

        val behavior = classifier.classifyExplorationBehavior(node)
        assertEquals(ExplorationBehavior.SKIP, behavior)
    }

    @Test
    fun `test SKIP - decorative ImageView`() {
        val node = createMockNode(
            className = "android.widget.ImageView",
            contentDescription = "",
            isClickable = false,
            childCount = 0
        )

        val behavior = classifier.classifyExplorationBehavior(node)
        assertEquals(ExplorationBehavior.SKIP, behavior)
    }

    // ========================================================================
    // Edge Cases and Priority Testing
    // ========================================================================

    @Test
    fun `test priority - MENU_TRIGGER beats CLICKABLE`() {
        // Overflow menu is both clickable AND menu trigger
        // MENU_TRIGGER should win (checked first)
        val node = createMockNode(
            className = "android.widget.ImageButton",
            resourceId = "com.example:id/overflow_menu",
            isClickable = true,
            isEnabled = true
        )

        val behavior = classifier.classifyExplorationBehavior(node)
        assertEquals(ExplorationBehavior.MENU_TRIGGER, behavior)
    }

    @Test
    fun `test priority - TAB beats CLICKABLE`() {
        // Bottom nav item is both clickable AND tab
        // TAB should win (checked first)
        val node = createMockNode(
            className = "com.google.android.material.bottomnavigation.BottomNavigationItemView",
            isClickable = true,
            isEnabled = true
        )

        val behavior = classifier.classifyExplorationBehavior(node)
        assertEquals(ExplorationBehavior.TAB, behavior)
    }

    @Test
    fun `test priority - SCROLLABLE beats CONTAINER`() {
        // RecyclerView is both scrollable AND container (has children)
        // SCROLLABLE should win (checked first)
        val node = createMockNode(
            className = "androidx.recyclerview.widget.RecyclerView",
            isClickable = false,
            isScrollable = true,
            childCount = 10
        )

        val behavior = classifier.classifyExplorationBehavior(node)
        assertEquals(ExplorationBehavior.SCROLLABLE, behavior)
    }

    @Test
    fun `test priority - LONG_CLICKABLE beats CONTAINER`() {
        // Element is both long clickable AND has children
        // LONG_CLICKABLE should win (checked first)
        val node = createMockNode(
            className = "android.widget.LinearLayout",
            isClickable = false,
            isLongClickable = true,
            childCount = 3
        )

        val behavior = classifier.classifyExplorationBehavior(node)
        assertEquals(ExplorationBehavior.LONG_CLICKABLE, behavior)
    }

    @Test
    fun `test edge case - nested scrollables`() {
        // RecyclerView inside ScrollView - both should be SCROLLABLE
        val outerNode = createMockNode(
            className = "android.widget.ScrollView",
            isScrollable = true,
            childCount = 1
        )

        val innerNode = createMockNode(
            className = "androidx.recyclerview.widget.RecyclerView",
            isScrollable = true,
            childCount = 10
        )

        val outerBehavior = classifier.classifyExplorationBehavior(outerNode)
        val innerBehavior = classifier.classifyExplorationBehavior(innerNode)

        assertEquals(ExplorationBehavior.SCROLLABLE, outerBehavior)
        assertEquals(ExplorationBehavior.SCROLLABLE, innerBehavior)
    }

    @Test
    fun `test edge case - clickable and long clickable both true`() {
        // When BOTH clickable and long clickable, CLICKABLE wins
        val node = createMockNode(
            className = "android.widget.Button",
            text = "Both clickable",
            isClickable = true,
            isLongClickable = true,
            isEnabled = true
        )

        val behavior = classifier.classifyExplorationBehavior(node)
        assertEquals(ExplorationBehavior.CLICKABLE, behavior)
    }

    @Test
    fun `test edge case - empty container (no children)`() {
        // Container with 0 children should be SKIP
        val node = createMockNode(
            className = "android.widget.LinearLayout",
            isClickable = false,
            childCount = 0
        )

        val behavior = classifier.classifyExplorationBehavior(node)
        assertEquals(ExplorationBehavior.SKIP, behavior)
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    /**
     * Create mock AccessibilityNodeInfo for testing
     *
     * @param className Class name (e.g., "android.widget.Button")
     * @param resourceId Resource ID (e.g., "com.example:id/button")
     * @param contentDescription Content description
     * @param text Text content
     * @param isClickable Whether clickable
     * @param isEnabled Whether enabled
     * @param isScrollable Whether scrollable
     * @param isLongClickable Whether long clickable
     * @param childCount Number of children
     * @return Mock node
     */
    private fun createMockNode(
        className: String = "android.widget.View",
        resourceId: String = "",
        contentDescription: String = "",
        text: String = "",
        isClickable: Boolean = false,
        isEnabled: Boolean = true,
        isScrollable: Boolean = false,
        isLongClickable: Boolean = false,
        childCount: Int = 0
    ): AccessibilityNodeInfo {
        val node = mockk<AccessibilityNodeInfo>(relaxed = true)

        every { node.className } returns className
        every { node.viewIdResourceName } returns resourceId.ifEmpty { null }
        every { node.contentDescription } returns contentDescription.ifEmpty { null }
        every { node.text } returns text.ifEmpty { null }
        every { node.isClickable } returns isClickable
        every { node.isEnabled } returns isEnabled
        every { node.isScrollable } returns isScrollable
        every { node.isLongClickable } returns isLongClickable
        every { node.isPassword } returns false
        every { node.childCount } returns childCount

        return node
    }
}
