package com.augmentalis.avanueui.renderer.android.mappers

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.augmentalis.avanueui.ui.core.feedback.*
import com.augmentalis.avanueui.renderer.android.ComposeRenderer
import org.junit.Rule
import org.junit.Test

/**
 * Comprehensive test suite for Navigation and Feedback components
 *
 * Tests all 10 components (4 Navigation + 6 Feedback) with various scenarios:
 * - Component rendering
 * - State management
 * - User interactions
 * - Callbacks
 * - Edge cases
 */
class NavigationAndFeedbackComponentsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val renderer = ComposeRenderer()

    // ==================== Navigation Components Tests ====================

    @Test
    fun appBar_displaysTitle() {
        val appBar = AppBar(
            id = "test-appbar",
            title = "Test Title"
        )

        composeTestRule.setContent {
            val composable = renderer.render(appBar) as @Composable () -> Unit
            composable()
        }

        composeTestRule
            .onNodeWithText("Test Title")
            .assertIsDisplayed()
    }

    @Test
    fun appBar_displaysSubtitle() {
        val appBar = AppBar(
            id = "test-appbar",
            title = "Main Title",
            subtitle = "Subtitle Text"
        )

        composeTestRule.setContent {
            val composable = renderer.render(appBar) as @Composable () -> Unit
            composable()
        }

        composeTestRule
            .onNodeWithText("Main Title\nSubtitle Text", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun appBar_navigationIconClick() {
        var clicked = false
        val appBar = AppBar(
            id = "test-appbar",
            title = "Title",
            navigationIcon = "back",
            onNavigationClick = { clicked = true }
        )

        composeTestRule.setContent {
            val composable = renderer.render(appBar) as @Composable () -> Unit
            composable()
        }

        composeTestRule
            .onNodeWithContentDescription("Navigate back")
            .performClick()

        assert(clicked)
    }

    @Test
    fun bottomNav_displaysAllItems() {
        val items = listOf(
            BottomNavItem("home", "Home", "home"),
            BottomNavItem("search", "Search", "search"),
            BottomNavItem("profile", "Profile", "profile")
        )

        val bottomNav = BottomNav(
            id = "test-nav",
            items = items,
            selectedIndex = 0
        )

        composeTestRule.setContent {
            val composable = renderer.render(bottomNav) as @Composable () -> Unit
            composable()
        }

        composeTestRule.onNodeWithText("Home").assertIsDisplayed()
        composeTestRule.onNodeWithText("Search").assertIsDisplayed()
        composeTestRule.onNodeWithText("Profile").assertIsDisplayed()
    }

    @Test
    fun bottomNav_itemSelection() {
        var selectedIndex = 0
        val items = listOf(
            BottomNavItem("home", "Home", "home"),
            BottomNavItem("search", "Search", "search")
        )

        val bottomNav = BottomNav(
            id = "test-nav",
            items = items,
            selectedIndex = selectedIndex,
            onItemSelected = { selectedIndex = it }
        )

        composeTestRule.setContent {
            val composable = renderer.render(bottomNav) as @Composable () -> Unit
            composable()
        }

        composeTestRule.onNodeWithText("Search").performClick()

        assert(selectedIndex == 1)
    }

    @Test
    fun breadcrumb_displaysAllItems() {
        val items = listOf(
            BreadcrumbItem("1", "Home"),
            BreadcrumbItem("2", "Products"),
            BreadcrumbItem("3", "Category")
        )

        val breadcrumb = Breadcrumb(
            id = "test-breadcrumb",
            items = items
        )

        composeTestRule.setContent {
            val composable = renderer.render(breadcrumb) as @Composable () -> Unit
            composable()
        }

        composeTestRule.onNodeWithText("Home").assertIsDisplayed()
        composeTestRule.onNodeWithText("Products").assertIsDisplayed()
        composeTestRule.onNodeWithText("Category").assertIsDisplayed()
    }

    @Test
    fun breadcrumb_itemClick() {
        var clickedIndex = -1
        val items = listOf(
            BreadcrumbItem("1", "Home"),
            BreadcrumbItem("2", "Products")
        )

        val breadcrumb = Breadcrumb(
            id = "test-breadcrumb",
            items = items,
            onItemClick = { clickedIndex = it }
        )

        composeTestRule.setContent {
            val composable = renderer.render(breadcrumb) as @Composable () -> Unit
            composable()
        }

        composeTestRule.onNodeWithText("Home").performClick()

        assert(clickedIndex == 0)
    }

    @Test
    fun pagination_displaysPageInfo() {
        val pagination = Pagination(
            id = "test-pagination",
            currentPage = 3,
            totalPages = 10,
            variant = PaginationVariant.Simple
        )

        composeTestRule.setContent {
            val composable = renderer.render(pagination) as @Composable () -> Unit
            composable()
        }

        composeTestRule
            .onNodeWithText("Page 3 of 10")
            .assertIsDisplayed()
    }

    @Test
    fun pagination_navigationButtons() {
        var currentPage = 5
        val pagination = Pagination(
            id = "test-pagination",
            currentPage = currentPage,
            totalPages = 10,
            variant = PaginationVariant.Simple,
            onPageChange = { currentPage = it }
        )

        composeTestRule.setContent {
            val composable = renderer.render(pagination) as @Composable () -> Unit
            composable()
        }

        composeTestRule
            .onNodeWithContentDescription("Next page")
            .performClick()

        assert(currentPage == 6)
    }

    // ==================== Feedback Components Tests ====================

    @Test
    fun alert_displaysMessage() {
        val alert = Alert(
            id = "test-alert",
            title = "Warning",
            message = "This is a warning message",
            severity = AlertSeverity.Warning
        )

        composeTestRule.setContent {
            val composable = renderer.render(alert) as @Composable () -> Unit
            composable()
        }

        composeTestRule.onNodeWithText("Warning").assertIsDisplayed()
        composeTestRule.onNodeWithText("This is a warning message").assertIsDisplayed()
    }

    @Test
    fun alert_closeButton() {
        var closed = false
        val alert = Alert(
            id = "test-alert",
            message = "Test message",
            closeable = true,
            onClose = { closed = true }
        )

        composeTestRule.setContent {
            val composable = renderer.render(alert) as @Composable () -> Unit
            composable()
        }

        composeTestRule
            .onNodeWithContentDescription("Close alert")
            .performClick()

        assert(closed)
    }

    @Test
    fun snackbar_displaysMessage() {
        val snackbar = Snackbar(
            id = "test-snackbar",
            message = "Action completed successfully"
        )

        composeTestRule.setContent {
            val composable = renderer.render(snackbar) as @Composable () -> Unit
            composable()
        }

        composeTestRule
            .onNodeWithText("Action completed successfully")
            .assertIsDisplayed()
    }

    @Test
    fun snackbar_actionButton() {
        var actionClicked = false
        val snackbar = Snackbar(
            id = "test-snackbar",
            message = "Item deleted",
            action = SnackbarAction("Undo") { actionClicked = true }
        )

        composeTestRule.setContent {
            val composable = renderer.render(snackbar) as @Composable () -> Unit
            composable()
        }

        composeTestRule.onNodeWithText("Undo").performClick()

        assert(actionClicked)
    }

    @Test
    fun modal_displaysContent() {
        val modal = Modal(
            id = "test-modal",
            open = true,
            title = "Modal Title",
            size = ModalSize.Medium
        )

        composeTestRule.setContent {
            val composable = renderer.render(modal) as @Composable () -> Unit
            composable()
        }

        composeTestRule.onNodeWithText("Modal Title").assertIsDisplayed()
    }

    @Test
    fun modal_closeButton() {
        var closed = false
        val modal = Modal(
            id = "test-modal",
            open = true,
            title = "Test Modal",
            closeable = true,
            onClose = { closed = true }
        )

        composeTestRule.setContent {
            val composable = renderer.render(modal) as @Composable () -> Unit
            composable()
        }

        composeTestRule
            .onNodeWithContentDescription("Close")
            .performClick()

        assert(closed)
    }

    @Test
    fun toast_displaysMessage() {
        val toast = Toast(
            id = "test-toast",
            message = "Toast notification",
            type = ToastType.Success,
            position = ToastPosition.BottomCenter,
            duration = 5000
        )

        composeTestRule.setContent {
            val composable = renderer.render(toast) as @Composable () -> Unit
            composable()
        }

        composeTestRule
            .onNodeWithText("Toast notification")
            .assertIsDisplayed()
    }

    @Test
    fun confirm_displaysDialog() {
        val confirm = Confirm(
            id = "test-confirm",
            open = true,
            title = "Confirm Action",
            message = "Are you sure?",
            severity = ConfirmSeverity.Warning
        )

        composeTestRule.setContent {
            val composable = renderer.render(confirm) as @Composable () -> Unit
            composable()
        }

        composeTestRule.onNodeWithText("Confirm Action").assertIsDisplayed()
        composeTestRule.onNodeWithText("Are you sure?").assertIsDisplayed()
    }

    @Test
    fun confirm_confirmButton() {
        var confirmed = false
        val confirm = Confirm(
            id = "test-confirm",
            open = true,
            title = "Delete",
            message = "Delete this item?",
            onConfirm = { confirmed = true }
        )

        composeTestRule.setContent {
            val composable = renderer.render(confirm) as @Composable () -> Unit
            composable()
        }

        composeTestRule.onNodeWithText("Confirm").performClick()

        assert(confirmed)
    }

    @Test
    fun confirm_cancelButton() {
        var cancelled = false
        val confirm = Confirm(
            id = "test-confirm",
            open = true,
            title = "Delete",
            message = "Delete this item?",
            onCancel = { cancelled = true }
        )

        composeTestRule.setContent {
            val composable = renderer.render(confirm) as @Composable () -> Unit
            composable()
        }

        composeTestRule.onNodeWithText("Cancel").performClick()

        assert(cancelled)
    }

    @Test
    fun contextMenu_displaysItems() {
        val items = listOf(
            ContextMenuItem("1", "Copy", "copy"),
            ContextMenuItem("2", "Paste", "paste"),
            ContextMenuItem("3", "Delete", "delete")
        )

        val contextMenu = ContextMenu(
            id = "test-menu",
            items = items,
            open = true
        )

        composeTestRule.setContent {
            val composable = renderer.render(contextMenu) as @Composable () -> Unit
            composable()
        }

        composeTestRule.onNodeWithText("Copy").assertIsDisplayed()
        composeTestRule.onNodeWithText("Paste").assertIsDisplayed()
        composeTestRule.onNodeWithText("Delete").assertIsDisplayed()
    }

    @Test
    fun contextMenu_itemClick() {
        var clickedItem = ""
        val items = listOf(
            ContextMenuItem("1", "Copy", onClick = { clickedItem = "Copy" }),
            ContextMenuItem("2", "Paste", onClick = { clickedItem = "Paste" })
        )

        val contextMenu = ContextMenu(
            id = "test-menu",
            items = items,
            open = true
        )

        composeTestRule.setContent {
            val composable = renderer.render(contextMenu) as @Composable () -> Unit
            composable()
        }

        composeTestRule.onNodeWithText("Paste").performClick()

        assert(clickedItem == "Paste")
    }

    // ==================== Edge Cases and Integration Tests ====================

    @Test
    fun bottomNav_withBadges() {
        val items = listOf(
            BottomNavItem("notifications", "Notifications", "bell", badge = "5"),
            BottomNavItem("messages", "Messages", "message", badge = "12")
        )

        val bottomNav = BottomNav(
            id = "test-nav",
            items = items
        )

        composeTestRule.setContent {
            val composable = renderer.render(bottomNav) as @Composable () -> Unit
            composable()
        }

        composeTestRule.onNodeWithText("5").assertIsDisplayed()
        composeTestRule.onNodeWithText("12").assertIsDisplayed()
    }

    @Test
    fun alert_withActions() {
        var action1Clicked = false
        var action2Clicked = false

        val alert = Alert(
            id = "test-alert",
            message = "Alert with actions",
            actions = listOf(
                AlertAction("action1", "Action 1", onClick = { action1Clicked = true }),
                AlertAction("action2", "Action 2", onClick = { action2Clicked = true })
            )
        )

        composeTestRule.setContent {
            val composable = renderer.render(alert) as @Composable () -> Unit
            composable()
        }

        composeTestRule.onNodeWithText("Action 1").performClick()

        assert(action1Clicked)
        assert(!action2Clicked)
    }

    @Test
    fun pagination_standardVariant() {
        val pagination = Pagination(
            id = "test-pagination",
            currentPage = 5,
            totalPages = 10,
            variant = PaginationVariant.Standard,
            showFirstLast = true
        )

        composeTestRule.setContent {
            val composable = renderer.render(pagination) as @Composable () -> Unit
            composable()
        }

        composeTestRule.onNodeWithText("First").assertIsDisplayed()
        composeTestRule.onNodeWithText("Last").assertIsDisplayed()
    }

    @Test
    fun modal_fullScreen() {
        val modal = Modal(
            id = "test-modal",
            open = true,
            title = "Full Screen Modal",
            size = ModalSize.FullScreen
        )

        composeTestRule.setContent {
            val composable = renderer.render(modal) as @Composable () -> Unit
            composable()
        }

        composeTestRule.onNodeWithText("Full Screen Modal").assertIsDisplayed()
    }
}
