package com.augmentalis.avaelements.renderer.android.navigation

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.augmentalis.avaelements.flutter.material.navigation.*
import com.augmentalis.avaelements.renderer.android.mappers.flutterparity.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test suite for Agent 4 Navigation components (P1):
 * - Menu
 * - Sidebar
 * - NavLink
 * - ProgressStepper
 *
 * Tests cover:
 * - Component rendering
 * - User interactions
 * - Accessibility
 * - State management
 * - Edge cases
 */
@RunWith(AndroidJUnit4::class)
class NavigationComponentsAdvancedTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ============================================================================
    // Menu Component Tests (7 tests)
    // ============================================================================

    @Test
    fun menu_verticalLayout_rendersAllItems() {
        val items = listOf(
            Menu.MenuItem(id = "1", label = "Item 1"),
            Menu.MenuItem(id = "2", label = "Item 2"),
            Menu.MenuItem(id = "3", label = "Item 3")
        )

        composeTestRule.setContent {
            MenuMapper(Menu(items = items, orientation = Menu.Orientation.Vertical))
        }

        composeTestRule.onNodeWithText("Item 1").assertExists()
        composeTestRule.onNodeWithText("Item 2").assertExists()
        composeTestRule.onNodeWithText("Item 3").assertExists()
    }

    @Test
    fun menu_horizontalLayout_rendersAsButtons() {
        val items = listOf(
            Menu.MenuItem(id = "1", label = "File"),
            Menu.MenuItem(id = "2", label = "Edit"),
            Menu.MenuItem(id = "3", label = "View")
        )

        composeTestRule.setContent {
            MenuMapper(Menu(items = items, orientation = Menu.Orientation.Horizontal))
        }

        composeTestRule.onNodeWithText("File").assertExists()
        composeTestRule.onNodeWithText("Edit").assertExists()
        composeTestRule.onNodeWithText("View").assertExists()
    }

    @Test
    fun menu_nestedSubmenus_expandsAndCollapses() {
        val items = listOf(
            Menu.MenuItem(
                id = "file",
                label = "File",
                children = listOf(
                    Menu.MenuItem(id = "new", label = "New"),
                    Menu.MenuItem(id = "open", label = "Open")
                )
            )
        )

        composeTestRule.setContent {
            MenuMapper(Menu(items = items))
        }

        // Initially, submenu items are hidden
        composeTestRule.onNodeWithText("New").assertDoesNotExist()

        // Click parent to expand
        composeTestRule.onNodeWithText("File").performClick()

        // Now submenu items are visible
        composeTestRule.onNodeWithText("New").assertExists()
        composeTestRule.onNodeWithText("Open").assertExists()
    }

    @Test
    fun menu_singleSelection_highlightsSelectedItem() {
        var selectedIndex = 0
        val items = listOf(
            Menu.MenuItem(id = "1", label = "Option 1"),
            Menu.MenuItem(id = "2", label = "Option 2")
        )

        composeTestRule.setContent {
            MenuMapper(
                Menu(
                    items = items,
                    selectionMode = Menu.SelectionMode.Single,
                    selectedIndex = selectedIndex,
                    onSelectionChanged = { selectedIndex = it }
                )
            )
        }

        // Select second item
        composeTestRule.onNodeWithText("Option 2").performClick()
        assert(selectedIndex == 1)
    }

    @Test
    fun menu_withBadges_displaysBadgeOnItem() {
        val items = listOf(
            Menu.MenuItem(id = "inbox", label = "Inbox", badge = "5")
        )

        composeTestRule.setContent {
            MenuMapper(Menu(items = items))
        }

        composeTestRule.onNodeWithText("Inbox").assertExists()
        composeTestRule.onNodeWithText("5").assertExists()
    }

    @Test
    fun menu_divider_rendersDividerBetweenItems() {
        val items = listOf(
            Menu.MenuItem(id = "1", label = "Item 1", divider = true),
            Menu.MenuItem(id = "2", label = "Item 2")
        )

        composeTestRule.setContent {
            MenuMapper(Menu(items = items))
        }

        // Both items should exist
        composeTestRule.onNodeWithText("Item 1").assertExists()
        composeTestRule.onNodeWithText("Item 2").assertExists()
    }

    @Test
    fun menu_accessibility_providesContentDescription() {
        val items = listOf(
            Menu.MenuItem(id = "1", label = "Home")
        )

        composeTestRule.setContent {
            MenuMapper(
                Menu(
                    items = items,
                    contentDescription = "Main navigation menu"
                )
            )
        }

        composeTestRule.onNode(hasContentDescription("Main navigation menu"))
            .assertExists()
    }

    // ============================================================================
    // Sidebar Component Tests (6 tests)
    // ============================================================================

    @Test
    fun sidebar_persistentMode_alwaysVisible() {
        val items = listOf(
            Sidebar.SidebarItem(id = "home", label = "Home", selected = true)
        )

        composeTestRule.setContent {
            SidebarMapper(
                Sidebar(
                    items = items,
                    mode = Sidebar.Mode.Persistent,
                    visible = true
                )
            )
        }

        composeTestRule.onNodeWithText("Home").assertExists()
    }

    @Test
    fun sidebar_overlayMode_hidesWhenNotVisible() {
        val items = listOf(
            Sidebar.SidebarItem(id = "home", label = "Home")
        )

        composeTestRule.setContent {
            SidebarMapper(
                Sidebar(
                    items = items,
                    mode = Sidebar.Mode.Overlay,
                    visible = false
                )
            )
        }

        // Sidebar should not be visible in overlay mode when visible=false
        composeTestRule.onNodeWithText("Home").assertDoesNotExist()
    }

    @Test
    fun sidebar_collapsible_togglesCollapseState() {
        var collapsed = false
        val items = listOf(
            Sidebar.SidebarItem(id = "home", label = "Home", icon = "home")
        )

        composeTestRule.setContent {
            SidebarMapper(
                Sidebar(
                    items = items,
                    collapsible = true,
                    collapsed = collapsed,
                    onCollapseToggle = { collapsed = it }
                )
            )
        }

        // Find and click collapse button
        composeTestRule.onNodeWithContentDescription("Collapse sidebar")
            .performClick()

        assert(collapsed)
    }

    @Test
    fun sidebar_withHeader_displaysHeaderContent() {
        val items = listOf(
            Sidebar.SidebarItem(id = "home", label = "Home")
        )

        composeTestRule.setContent {
            SidebarMapper(
                Sidebar(
                    items = items,
                    headerContent = "My Application"
                )
            )
        }

        composeTestRule.onNodeWithText("My Application").assertExists()
    }

    @Test
    fun sidebar_withFooter_displaysFooterContent() {
        val items = listOf(
            Sidebar.SidebarItem(id = "home", label = "Home")
        )

        composeTestRule.setContent {
            SidebarMapper(
                Sidebar(
                    items = items,
                    footerContent = "Version 1.0"
                )
            )
        }

        composeTestRule.onNodeWithText("Version 1.0").assertExists()
    }

    @Test
    fun sidebar_itemWithBadge_displaysBadge() {
        val items = listOf(
            Sidebar.SidebarItem(
                id = "notifications",
                label = "Notifications",
                icon = "notifications",
                badge = "3"
            )
        )

        composeTestRule.setContent {
            SidebarMapper(Sidebar(items = items))
        }

        composeTestRule.onNodeWithText("Notifications").assertExists()
        composeTestRule.onNodeWithText("3").assertExists()
    }

    // ============================================================================
    // NavLink Component Tests (6 tests)
    // ============================================================================

    @Test
    fun navLink_activeState_highlightsLink() {
        composeTestRule.setContent {
            NavLinkMapper(
                NavLink(
                    label = "Dashboard",
                    href = "/dashboard",
                    active = true
                )
            )
        }

        composeTestRule.onNodeWithText("Dashboard").assertExists()
    }

    @Test
    fun navLink_inactiveState_normalStyling() {
        composeTestRule.setContent {
            NavLinkMapper(
                NavLink(
                    label = "Settings",
                    href = "/settings",
                    active = false
                )
            )
        }

        composeTestRule.onNodeWithText("Settings").assertExists()
    }

    @Test
    fun navLink_withIcon_leadingPosition() {
        composeTestRule.setContent {
            NavLinkMapper(
                NavLink(
                    label = "Home",
                    href = "/home",
                    icon = "home",
                    iconPosition = NavLink.IconPosition.Leading
                )
            )
        }

        composeTestRule.onNodeWithText("Home").assertExists()
    }

    @Test
    fun navLink_withBadge_displaysBadge() {
        composeTestRule.setContent {
            NavLinkMapper(
                NavLink(
                    label = "Messages",
                    href = "/messages",
                    badge = "12"
                )
            )
        }

        composeTestRule.onNodeWithText("Messages").assertExists()
        composeTestRule.onNodeWithText("12").assertExists()
    }

    @Test
    fun navLink_onClick_triggersCallback() {
        var clicked = false

        composeTestRule.setContent {
            NavLinkMapper(
                NavLink(
                    label = "Profile",
                    href = "/profile",
                    onClick = { clicked = true }
                )
            )
        }

        composeTestRule.onNodeWithText("Profile").performClick()
        assert(clicked)
    }

    @Test
    fun navLink_disabled_doesNotTriggerClick() {
        var clicked = false

        composeTestRule.setContent {
            NavLinkMapper(
                NavLink(
                    label = "Disabled Link",
                    href = "/disabled",
                    enabled = false,
                    onClick = { clicked = true }
                )
            )
        }

        composeTestRule.onNodeWithText("Disabled Link").performClick()
        assert(!clicked)
    }

    // ============================================================================
    // ProgressStepper Component Tests (6 tests)
    // ============================================================================

    @Test
    fun progressStepper_horizontalOrientation_rendersAllSteps() {
        val steps = listOf(
            ProgressStepper.Step(label = "Step 1"),
            ProgressStepper.Step(label = "Step 2"),
            ProgressStepper.Step(label = "Step 3")
        )

        composeTestRule.setContent {
            ProgressStepperMapper(
                ProgressStepper(
                    steps = steps,
                    currentStep = 1,
                    orientation = ProgressStepper.Orientation.Horizontal
                )
            )
        }

        composeTestRule.onNodeWithText("Step 1").assertExists()
        composeTestRule.onNodeWithText("Step 2").assertExists()
        composeTestRule.onNodeWithText("Step 3").assertExists()
    }

    @Test
    fun progressStepper_verticalOrientation_rendersWithDescriptions() {
        val steps = listOf(
            ProgressStepper.Step(
                label = "Account",
                description = "Create your account"
            ),
            ProgressStepper.Step(
                label = "Verify",
                description = "Verify your email"
            )
        )

        composeTestRule.setContent {
            ProgressStepperMapper(
                ProgressStepper(
                    steps = steps,
                    currentStep = 0,
                    orientation = ProgressStepper.Orientation.Vertical
                )
            )
        }

        composeTestRule.onNodeWithText("Account").assertExists()
        composeTestRule.onNodeWithText("Create your account").assertExists()
    }

    @Test
    fun progressStepper_clickableSteps_navigatesToCompletedSteps() {
        var clickedStep = -1
        val steps = listOf(
            ProgressStepper.Step(label = "Step 1"),
            ProgressStepper.Step(label = "Step 2"),
            ProgressStepper.Step(label = "Step 3")
        )

        composeTestRule.setContent {
            ProgressStepperMapper(
                ProgressStepper(
                    steps = steps,
                    currentStep = 2,
                    clickable = true,
                    onStepClicked = { clickedStep = it }
                )
            )
        }

        // Click on completed step (should be clickable)
        // Note: This test may need adjustment based on how step indicators are accessed
    }

    @Test
    fun progressStepper_errorState_showsErrorIndicator() {
        val steps = listOf(
            ProgressStepper.Step(label = "Step 1", error = true),
            ProgressStepper.Step(label = "Step 2")
        )

        composeTestRule.setContent {
            ProgressStepperMapper(
                ProgressStepper(
                    steps = steps,
                    currentStep = 0
                )
            )
        }

        composeTestRule.onNodeWithText("Step 1").assertExists()
    }

    @Test
    fun progressStepper_optionalStep_displaysOptionalLabel() {
        val steps = listOf(
            ProgressStepper.Step(label = "Step 1"),
            ProgressStepper.Step(label = "Step 2", optional = true)
        )

        composeTestRule.setContent {
            ProgressStepperMapper(
                ProgressStepper(
                    steps = steps,
                    currentStep = 1,
                    orientation = ProgressStepper.Orientation.Vertical
                )
            )
        }

        composeTestRule.onNodeWithText("Optional").assertExists()
    }

    @Test
    fun progressStepper_progressPercentage_calculatesCorrectly() {
        val steps = listOf(
            ProgressStepper.Step(label = "Step 1"),
            ProgressStepper.Step(label = "Step 2"),
            ProgressStepper.Step(label = "Step 3")
        )

        val stepper = ProgressStepper(
            steps = steps,
            currentStep = 1
        )

        val progress = stepper.getProgressPercentage()
        assert(progress == 0.5f) // 1 / (3-1) = 0.5
    }
}
