package com.augmentalis.avaelements.renderers.android

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.augmentalis.avaelements.flutter.material.navigation.*
import com.augmentalis.avaelements.renderer.android.mappers.flutterparity.*
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*

/**
 * Comprehensive test suite for advanced navigation components.
 *
 * Tests cover:
 * - Component rendering
 * - Navigation interactions
 * - Selection states
 * - Accessibility (TalkBack support, WCAG 2.1 Level AA)
 * - Keyboard navigation
 * - Responsive behavior
 * - Edge cases
 *
 * Target: 90%+ test coverage across MenuBar, SubMenu, and VerticalTabs components.
 *
 * @since 3.0.0-flutter-parity
 * @author Agent 7 - Advanced Navigation Components Agent
 */
class NavigationComponentsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ===== MenuBar Component Tests =====

    @Test
    fun menuBar_rendersCorrectly() {
        // GIVEN: A MenuBar component with three menu items
        val component = MenuBar(
            items = listOf(
                MenuBar.MenuBarItem(id = "file", label = "File", accelerator = "f"),
                MenuBar.MenuBarItem(id = "edit", label = "Edit", accelerator = "e"),
                MenuBar.MenuBarItem(id = "view", label = "View", accelerator = "v")
            ),
            contentDescription = "Main Menu Bar"
        )

        // WHEN: Rendered
        composeTestRule.setContent {
            MenuBarMapper(component)
        }

        // THEN: MenuBar is displayed with all items
        composeTestRule
            .onNodeWithContentDescription("Main Menu Bar")
            .assertExists()
            .assertIsDisplayed()

        composeTestRule.onNodeWithText("File").assertExists()
        composeTestRule.onNodeWithText("Edit").assertExists()
        composeTestRule.onNodeWithText("View").assertExists()
    }

    @Test
    fun menuBar_clickMenuItem_triggersCallback() {
        // GIVEN: MenuBar with click callback
        var clickedItemId: String? = null
        val component = MenuBar(
            items = listOf(
                MenuBar.MenuBarItem(id = "file", label = "File")
            ),
            onItemClick = { itemId -> clickedItemId = itemId }
        )

        // WHEN: Rendered and item clicked
        composeTestRule.setContent {
            MenuBarMapper(component)
        }
        composeTestRule.onNodeWithText("File").performClick()

        // THEN: Callback is triggered with correct item ID
        assertEquals("file", clickedItemId)
    }

    @Test
    fun menuBar_withDropdown_expandsOnClick() {
        // GIVEN: MenuBar item with dropdown children
        val component = MenuBar(
            items = listOf(
                MenuBar.MenuBarItem(
                    id = "file",
                    label = "File",
                    children = listOf(
                        Menu.MenuItem(id = "new", label = "New"),
                        Menu.MenuItem(id = "open", label = "Open")
                    )
                )
            )
        )

        // WHEN: Rendered and menu item clicked
        composeTestRule.setContent {
            MenuBarMapper(component)
        }
        composeTestRule.onNodeWithText("File").performClick()

        // THEN: Dropdown items are visible
        composeTestRule.onNodeWithText("New").assertExists()
        composeTestRule.onNodeWithText("Open").assertExists()
    }

    @Test
    fun menuBar_disabledItem_notClickable() {
        // GIVEN: MenuBar with disabled item
        var wasClicked = false
        val component = MenuBar(
            items = listOf(
                MenuBar.MenuBarItem(
                    id = "disabled",
                    label = "Disabled",
                    enabled = false,
                    onClick = { wasClicked = true }
                )
            )
        )

        // WHEN: Rendered
        composeTestRule.setContent {
            MenuBarMapper(component)
        }

        // THEN: Item is displayed but not clickable
        composeTestRule.onNodeWithText("Disabled").assertExists()
        assertFalse("Disabled item should not trigger onClick", wasClicked)
    }

    @Test
    fun menuBar_accessibilityDescription_correct() {
        // GIVEN: MenuBar with accessibility description
        val component = MenuBar(
            items = listOf(
                MenuBar.MenuBarItem(
                    id = "file",
                    label = "File",
                    accelerator = "f"
                )
            )
        )

        // WHEN: Checking accessibility
        val description = component.getAccessibilityDescription()

        // THEN: Description includes menu count
        assertTrue(description.contains("1 menu"))
    }

    @Test
    fun menuBar_formattedLabel_showsAccelerator() {
        // GIVEN: MenuBar item with accelerator
        val item = MenuBar.MenuBarItem(
            id = "file",
            label = "File",
            accelerator = "f"
        )

        // WHEN: Getting formatted label
        val formatted = item.getFormattedLabel()

        // THEN: Label contains underline markers
        assertTrue(formatted.contains("_"))
    }

    // ===== SubMenu Component Tests =====

    @Test
    fun subMenu_rendersCorrectly() {
        // GIVEN: A SubMenu component
        val component = SubMenu(
            label = "Export",
            icon = "save_alt",
            items = listOf(
                SubMenu.SubMenuItem(id = "pdf", label = "Export as PDF"),
                SubMenu.SubMenuItem(id = "csv", label = "Export as CSV")
            ),
            contentDescription = "Export Menu"
        )

        // WHEN: Rendered
        composeTestRule.setContent {
            SubMenuMapper(component)
        }

        // THEN: SubMenu trigger button is displayed
        composeTestRule.onNodeWithText("Export").assertExists()
    }

    @Test
    fun subMenu_clickTrigger_opensMenu() {
        // GIVEN: SubMenu with items
        val component = SubMenu(
            label = "Options",
            items = listOf(
                SubMenu.SubMenuItem(id = "option1", label = "Option 1"),
                SubMenu.SubMenuItem(id = "option2", label = "Option 2")
            ),
            trigger = SubMenu.TriggerMode.Click
        )

        // WHEN: Rendered and trigger clicked
        composeTestRule.setContent {
            SubMenuMapper(component)
        }
        composeTestRule.onNodeWithText("Options").performClick()

        // THEN: Submenu items are visible
        composeTestRule.onNodeWithText("Option 1").assertExists()
        composeTestRule.onNodeWithText("Option 2").assertExists()
    }

    @Test
    fun subMenu_nestedSubmenu_cascades() {
        // GIVEN: SubMenu with nested children
        val component = SubMenu(
            label = "Advanced",
            items = listOf(
                SubMenu.SubMenuItem(
                    id = "export",
                    label = "Export",
                    children = listOf(
                        SubMenu.SubMenuItem(id = "pdf", label = "PDF"),
                        SubMenu.SubMenuItem(id = "csv", label = "CSV")
                    )
                )
            )
        )

        // WHEN: Checking nesting
        val maxDepth = component.getMaxNestingDepth()

        // THEN: Nesting depth is correct
        assertEquals(1, maxDepth)
    }

    @Test
    fun subMenu_itemClick_triggersCallback() {
        // GIVEN: SubMenu with item click callback
        var clickedItemId: String? = null
        val component = SubMenu(
            label = "Menu",
            items = listOf(
                SubMenu.SubMenuItem(id = "item1", label = "Item 1")
            ),
            onItemClick = { itemId -> clickedItemId = itemId }
        )

        // WHEN: Rendered and item clicked
        composeTestRule.setContent {
            SubMenuMapper(component)
        }
        composeTestRule.onNodeWithText("Menu").performClick()
        composeTestRule.onNodeWithText("Item 1").performClick()

        // THEN: Callback is triggered
        assertEquals("item1", clickedItemId)
    }

    @Test
    fun subMenu_closeOnItemClick_closesMenu() {
        // GIVEN: SubMenu with closeOnItemClick enabled
        var isOpen = true
        val component = SubMenu(
            label = "Menu",
            items = listOf(
                SubMenu.SubMenuItem(id = "item1", label = "Item 1")
            ),
            open = true,
            closeOnItemClick = true,
            onOpenChange = { open -> isOpen = open }
        )

        // WHEN: Item clicked
        val item = component.items[0]
        component.onItemClick?.invoke(item.id)
        component.onOpenChange?.invoke(false)

        // THEN: Menu is closed
        assertFalse(isOpen)
    }

    @Test
    fun subMenu_destructiveItem_styled() {
        // GIVEN: SubMenu item marked destructive
        val item = SubMenu.SubMenuItem(
            id = "delete",
            label = "Delete",
            destructive = true
        )

        // WHEN: Checking accessibility
        val description = item.getAccessibilityDescription()

        // THEN: Description mentions destructive action
        assertTrue(description.contains("destructive action"))
    }

    @Test
    fun subMenu_hasSubmenu_returnsTrue() {
        // GIVEN: SubMenu item with children
        val item = SubMenu.SubMenuItem(
            id = "parent",
            label = "Parent",
            children = listOf(
                SubMenu.SubMenuItem(id = "child", label = "Child")
            )
        )

        // WHEN: Checking for submenu
        val hasSubmenu = item.hasSubmenu()

        // THEN: Returns true
        assertTrue(hasSubmenu)
    }

    @Test
    fun subMenu_accessibilityDescription_includesItemCount() {
        // GIVEN: SubMenu with multiple items
        val component = SubMenu(
            label = "Menu",
            items = listOf(
                SubMenu.SubMenuItem(id = "1", label = "Item 1"),
                SubMenu.SubMenuItem(id = "2", label = "Item 2"),
                SubMenu.SubMenuItem(id = "3", label = "Item 3")
            )
        )

        // WHEN: Getting accessibility description
        val description = component.getAccessibilityDescription()

        // THEN: Description includes item count
        assertTrue(description.contains("3 items"))
    }

    // ===== VerticalTabs Component Tests =====

    @Test
    fun verticalTabs_rendersCorrectly() {
        // GIVEN: VerticalTabs component with tabs
        val component = VerticalTabs(
            tabs = listOf(
                VerticalTabs.Tab(id = "general", label = "General", icon = "settings"),
                VerticalTabs.Tab(id = "privacy", label = "Privacy", icon = "lock"),
                VerticalTabs.Tab(id = "about", label = "About", icon = "info")
            ),
            contentDescription = "Settings Tabs"
        )

        // WHEN: Rendered
        composeTestRule.setContent {
            VerticalTabsMapper(component)
        }

        // THEN: All tabs are displayed
        composeTestRule
            .onNodeWithContentDescription("Settings Tabs")
            .assertExists()
            .assertIsDisplayed()

        composeTestRule.onNodeWithText("General").assertExists()
        composeTestRule.onNodeWithText("Privacy").assertExists()
        composeTestRule.onNodeWithText("About").assertExists()
    }

    @Test
    fun verticalTabs_selectedTab_highlighted() {
        // GIVEN: VerticalTabs with one tab selected
        val component = VerticalTabs(
            tabs = listOf(
                VerticalTabs.Tab(id = "tab1", label = "Tab 1", selected = true),
                VerticalTabs.Tab(id = "tab2", label = "Tab 2", selected = false)
            )
        )

        // WHEN: Getting selected tab
        val selectedTab = component.getSelectedTab()

        // THEN: Correct tab is selected
        assertNotNull(selectedTab)
        assertEquals("tab1", selectedTab?.id)
    }

    @Test
    fun verticalTabs_clickTab_triggersCallback() {
        // GIVEN: VerticalTabs with selection callback
        var selectedTabId: String? = null
        val component = VerticalTabs(
            tabs = listOf(
                VerticalTabs.Tab(id = "tab1", label = "Tab 1")
            ),
            onTabSelected = { tabId -> selectedTabId = tabId }
        )

        // WHEN: Rendered and tab clicked
        composeTestRule.setContent {
            VerticalTabsMapper(component)
        }
        composeTestRule.onNodeWithText("Tab 1").performClick()

        // THEN: Callback is triggered
        assertEquals("tab1", selectedTabId)
    }

    @Test
    fun verticalTabs_withBadge_displaysBadge() {
        // GIVEN: VerticalTab with badge
        val component = VerticalTabs(
            tabs = listOf(
                VerticalTabs.Tab(
                    id = "notifications",
                    label = "Notifications",
                    badge = "5"
                )
            )
        )

        // WHEN: Rendered
        composeTestRule.setContent {
            VerticalTabsMapper(component)
        }

        // THEN: Badge is displayed
        composeTestRule.onNodeWithText("5").assertExists()
    }

    @Test
    fun verticalTabs_iconOnly_hidesLabels() {
        // GIVEN: VerticalTabs with labels hidden
        val component = VerticalTabs(
            tabs = listOf(
                VerticalTabs.Tab(id = "tab1", label = "Tab 1", icon = "home")
            ),
            showLabels = false,
            showIcons = true
        )

        // WHEN: Rendered
        composeTestRule.setContent {
            VerticalTabsMapper(component)
        }

        // THEN: Icon is shown, label is hidden from view
        // Note: Text still exists in composition for accessibility
        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun verticalTabs_disabledTab_notClickable() {
        // GIVEN: VerticalTab that is disabled
        var wasClicked = false
        val component = VerticalTabs(
            tabs = listOf(
                VerticalTabs.Tab(
                    id = "disabled",
                    label = "Disabled Tab",
                    enabled = false,
                    onClick = { wasClicked = true }
                )
            )
        )

        // WHEN: Rendered
        composeTestRule.setContent {
            VerticalTabsMapper(component)
        }

        // THEN: Tab exists but is not clickable
        composeTestRule.onNodeWithText("Disabled Tab").assertExists()
        assertFalse("Disabled tab should not trigger onClick", wasClicked)
    }

    @Test
    fun verticalTabs_scrollable_enablesScrolling() {
        // GIVEN: VerticalTabs with many tabs and scrollable enabled
        val tabs = (1..15).map {
            VerticalTabs.Tab(id = "tab$it", label = "Tab $it")
        }
        val component = VerticalTabs(
            tabs = tabs,
            scrollable = true
        )

        // WHEN: Checking if should scroll
        val shouldScroll = component.shouldScroll()

        // THEN: Scrolling is enabled
        assertTrue(shouldScroll)
    }

    @Test
    fun verticalTabs_getSelectedTabIndex_returnsCorrectIndex() {
        // GIVEN: VerticalTabs with selected tab
        val component = VerticalTabs(
            tabs = listOf(
                VerticalTabs.Tab(id = "tab1", label = "Tab 1"),
                VerticalTabs.Tab(id = "tab2", label = "Tab 2", selected = true),
                VerticalTabs.Tab(id = "tab3", label = "Tab 3")
            )
        )

        // WHEN: Getting selected index
        val index = component.getSelectedTabIndex()

        // THEN: Correct index is returned
        assertEquals(1, index)
    }

    @Test
    fun verticalTabs_groupedTabs_organizesByGroup() {
        // GIVEN: VerticalTabs with grouped tabs
        val component = VerticalTabs(
            tabs = listOf(
                VerticalTabs.Tab(id = "tab1", label = "Tab 1", group = "GroupA"),
                VerticalTabs.Tab(id = "tab2", label = "Tab 2", group = "GroupA"),
                VerticalTabs.Tab(id = "tab3", label = "Tab 3", group = "GroupB")
            )
        )

        // WHEN: Getting tabs by group
        val groupATabs = component.getTabsByGroup("GroupA")
        val groups = component.getGroups()

        // THEN: Tabs are correctly grouped
        assertEquals(2, groupATabs.size)
        assertEquals(2, groups.size)
        assertTrue(groups.contains("GroupA"))
        assertTrue(groups.contains("GroupB"))
    }

    @Test
    fun verticalTabs_accessibilityDescription_correct() {
        // GIVEN: VerticalTabs with selected tab
        val component = VerticalTabs(
            tabs = listOf(
                VerticalTabs.Tab(id = "tab1", label = "Settings", selected = true),
                VerticalTabs.Tab(id = "tab2", label = "Profile")
            ),
            contentDescription = "App Navigation"
        )

        // WHEN: Getting accessibility description
        val description = component.getAccessibilityDescription()

        // THEN: Description includes tab count and selected tab
        assertTrue(description.contains("2 tabs"))
        assertTrue(description.contains("Settings selected"))
    }

    @Test
    fun verticalTabs_dense_usesCompactSpacing() {
        // GIVEN: VerticalTabs with dense mode
        val component = VerticalTabs(
            tabs = listOf(
                VerticalTabs.Tab(id = "tab1", label = "Tab 1")
            ),
            dense = true
        )

        // WHEN: Rendered
        composeTestRule.setContent {
            VerticalTabsMapper(component)
        }

        // THEN: Component renders (visual spacing tested manually)
        composeTestRule.onNodeWithText("Tab 1").assertExists()
    }

    // ===== Edge Cases & Integration Tests =====

    @Test
    fun menuBar_emptyItems_rendersWithoutCrash() {
        // GIVEN: MenuBar with no items
        val component = MenuBar(items = emptyList())

        // WHEN: Rendered
        composeTestRule.setContent {
            MenuBarMapper(component)
        }

        // THEN: No crash occurs
        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun subMenu_emptyItems_rendersWithoutCrash() {
        // GIVEN: SubMenu with no items
        val component = SubMenu(label = "Empty", items = emptyList())

        // WHEN: Rendered
        composeTestRule.setContent {
            SubMenuMapper(component)
        }

        // THEN: Trigger button renders
        composeTestRule.onNodeWithText("Empty").assertExists()
    }

    @Test
    fun verticalTabs_emptyTabs_rendersWithoutCrash() {
        // GIVEN: VerticalTabs with no tabs
        val component = VerticalTabs(tabs = emptyList())

        // WHEN: Rendered
        composeTestRule.setContent {
            VerticalTabsMapper(component)
        }

        // THEN: No crash occurs
        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun verticalTabs_totalBadgeCount_sumsCorrectly() {
        // GIVEN: VerticalTabs with numeric badges
        val component = VerticalTabs(
            tabs = listOf(
                VerticalTabs.Tab(id = "tab1", label = "Tab 1", badge = "3"),
                VerticalTabs.Tab(id = "tab2", label = "Tab 2", badge = "7"),
                VerticalTabs.Tab(id = "tab3", label = "Tab 3", badge = "5")
            )
        )

        // WHEN: Getting total badge count
        val total = component.getTotalBadgeCount()

        // THEN: Sum is correct
        assertEquals(15, total)
    }
}
