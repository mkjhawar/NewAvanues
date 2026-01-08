package com.augmentalis.avamagic.renderer.ios

import com.augmentalis.magicui.ui.core.navigation.*
import com.augmentalis.magicui.ui.core.form.*
import com.augmentalis.magicui.ui.core.feedback.*
import com.augmentalis.magicui.components.core.*
import kotlin.test.*

/**
 * Comprehensive Unit Tests for iOS Renderer Phase 2 Components
 *
 * Tests all 15 new components added in Phase 2:
 * - Navigation: AppBar, BottomNav, Tabs, Drawer (4 components)
 * - Form: DatePicker, TimePicker, SearchBar, Dropdown (4 components)
 * - Feedback: Dialog, Snackbar, Toast, ProgressBar, CircularProgress (5 components)
 * - Display: WebView, VideoPlayer (2 components)
 *
 * Total: 40+ tests
 *
 * @author Manoj Jhawar
 * @since 2025-11-21
 */
class IOSRendererPhase2Test {

    // ====================
    // NAVIGATION COMPONENTS (10 tests)
    // ====================

    @Test
    fun testAppBarRendererCreation() {
        val renderer = IOSAppBarRenderer()
        assertNotNull(renderer)
    }

    @Test
    fun testAppBarWithTitle() {
        val component = AppBarComponent(
            title = "My App",
            navigationIcon = "menu"
        )
        assertEquals("My App", component.title)
        assertEquals("menu", component.navigationIcon)
    }

    @Test
    fun testAppBarWithActions() {
        val actions = listOf(
            AppBarAction("search", "Search") {},
            AppBarAction("settings", "Settings") {}
        )
        val component = AppBarComponent(
            title = "Home",
            actions = actions
        )
        assertEquals(2, component.actions.size)
        assertEquals("search", component.actions[0].icon)
    }

    @Test
    fun testBottomNavRendererCreation() {
        val renderer = IOSBottomNavRenderer()
        assertNotNull(renderer)
    }

    @Test
    fun testBottomNavWithItems() {
        val items = listOf(
            BottomNavItem("home", "Home"),
            BottomNavItem("search", "Search"),
            BottomNavItem("profile", "Profile")
        )
        val component = BottomNavComponent(
            items = items,
            selectedIndex = 0
        )
        assertEquals(3, component.items.size)
        assertEquals(0, component.selectedIndex)
    }

    @Test
    fun testBottomNavValidation() {
        // Must have 2-5 items
        assertFails {
            BottomNavComponent(
                items = listOf(BottomNavItem("home", "Home")),
                selectedIndex = 0
            )
        }
    }

    @Test
    fun testTabsRendererCreation() {
        val renderer = IOSTabsRenderer()
        assertNotNull(renderer)
    }

    @Test
    fun testTabsWithMultipleTabs() {
        val tabs = listOf(
            Tab("Overview", "dashboard"),
            Tab("Details", "info"),
            Tab("Settings", "settings")
        )
        val component = TabsComponent(
            tabs = tabs,
            selectedIndex = 0
        )
        assertEquals(3, component.tabs.size)
        assertEquals("Overview", component.tabs[0].label)
    }

    @Test
    fun testDrawerRendererCreation() {
        val renderer = IOSDrawerRenderer()
        assertNotNull(renderer)
    }

    @Test
    fun testDrawerWithItems() {
        val items = listOf(
            DrawerItem("home", "home", "Home"),
            DrawerItem("settings", "settings", "Settings")
        )
        val component = DrawerComponent(
            isOpen = false,
            position = DrawerPosition.Left,
            items = items
        )
        assertEquals(2, component.items.size)
        assertFalse(component.isOpen)
    }

    // ====================
    // FORM COMPONENTS (12 tests)
    // ====================

    @Test
    fun testDatePickerRendererCreation() {
        val renderer = IOSDatePickerRenderer()
        assertNotNull(renderer)
    }

    @Test
    fun testDatePickerWithSelectedDate() {
        val timestamp = System.currentTimeMillis()
        val component = DatePickerComponent(
            selectedDate = timestamp,
            label = "Select Date"
        )
        assertEquals(timestamp, component.selectedDate)
        assertEquals("Select Date", component.label)
    }

    @Test
    fun testDatePickerWithMinMaxDates() {
        val now = System.currentTimeMillis()
        val minDate = now - 86400000 // Yesterday
        val maxDate = now + 86400000 // Tomorrow
        val component = DatePickerComponent(
            selectedDate = now,
            minDate = minDate,
            maxDate = maxDate
        )
        assertEquals(minDate, component.minDate)
        assertEquals(maxDate, component.maxDate)
    }

    @Test
    fun testDatePickerValidation() {
        val now = System.currentTimeMillis()
        assertFails {
            DatePickerComponent(
                minDate = now + 86400000,
                maxDate = now - 86400000 // Max before min
            )
        }
    }

    @Test
    fun testTimePickerRendererCreation() {
        val renderer = IOSTimePickerRenderer()
        assertNotNull(renderer)
    }

    @Test
    fun testTimePickerWith24HourFormat() {
        val time = Time(14, 30)
        val component = TimePickerComponent(
            selectedTime = time,
            is24Hour = true
        )
        assertTrue(component.is24Hour)
        assertEquals(14, component.selectedTime?.hour)
        assertEquals(30, component.selectedTime?.minute)
    }

    @Test
    fun testTimePickerWith12HourFormat() {
        val time = Time(14, 30)
        val component = TimePickerComponent(
            selectedTime = time,
            is24Hour = false
        )
        assertFalse(component.is24Hour)
    }

    @Test
    fun testSearchBarRendererCreation() {
        val renderer = IOSSearchBarRenderer()
        assertNotNull(renderer)
    }

    @Test
    fun testSearchBarWithValue() {
        val component = SearchBarComponent(
            value = "test query",
            placeholder = "Search...",
            showClearButton = true
        )
        assertEquals("test query", component.value)
        assertTrue(component.showClearButton)
    }

    @Test
    fun testSearchBarWithSuggestions() {
        val suggestions = listOf("suggestion1", "suggestion2", "suggestion3")
        val component = SearchBarComponent(
            value = "",
            suggestions = suggestions
        )
        assertEquals(3, component.suggestions.size)
    }

    @Test
    fun testDropdownRendererCreation() {
        val renderer = IOSDropdownRenderer()
        assertNotNull(renderer)
    }

    @Test
    fun testDropdownWithOptions() {
        val options = listOf(
            DropdownOption("us", "United States"),
            DropdownOption("uk", "United Kingdom"),
            DropdownOption("ca", "Canada")
        )
        val component = DropdownComponent(
            options = options,
            selectedValue = "us"
        )
        assertEquals(3, component.options.size)
        assertEquals("us", component.selectedValue)
    }

    // ====================
    // FEEDBACK COMPONENTS (13 tests)
    // ====================

    @Test
    fun testDialogRendererCreation() {
        val renderer = IOSDialogRenderer()
        assertNotNull(renderer)
    }

    @Test
    fun testDialogAlert() {
        val component = DialogComponent.alert(
            title = "Alert",
            message = "This is an alert message"
        )
        assertEquals("Alert", component.title)
        assertEquals("This is an alert message", component.content)
        assertNull(component.cancelLabel)
    }

    @Test
    fun testDialogConfirm() {
        val component = DialogComponent.confirm(
            title = "Confirm",
            message = "Are you sure?"
        )
        assertEquals("Confirm", component.title)
        assertEquals("Yes", component.confirmLabel)
        assertEquals("No", component.cancelLabel)
    }

    @Test
    fun testSnackbarRendererCreation() {
        val renderer = IOSSnackbarRenderer()
        assertNotNull(renderer)
    }

    @Test
    fun testSnackbarSimple() {
        val component = SnackbarComponent.simple("Operation completed")
        assertEquals("Operation completed", component.message)
        assertNull(component.actionLabel)
    }

    @Test
    fun testSnackbarWithAction() {
        val component = SnackbarComponent.withAction(
            message = "Item deleted",
            action = "UNDO"
        )
        assertEquals("Item deleted", component.message)
        assertEquals("UNDO", component.actionLabel)
    }

    @Test
    fun testToastRendererCreation() {
        val renderer = IOSToastRenderer()
        assertNotNull(renderer)
    }

    @Test
    fun testToastSuccess() {
        val component = ToastComponent.success("Success!")
        assertEquals("Success!", component.message)
        assertEquals(Severity.SUCCESS, component.severity)
    }

    @Test
    fun testToastError() {
        val component = ToastComponent.error("Error occurred")
        assertEquals("Error occurred", component.message)
        assertEquals(Severity.ERROR, component.severity)
        assertEquals(5000L, component.duration) // Errors last longer
    }

    @Test
    fun testToastWarning() {
        val component = ToastComponent.warning("Warning!")
        assertEquals(Severity.WARNING, component.severity)
    }

    @Test
    fun testProgressBarRendererCreation() {
        val renderer = IOSProgressBarRenderer()
        assertNotNull(renderer)
    }

    @Test
    fun testProgressBarDeterminate() {
        val component = ProgressBarComponent(
            value = 50f,
            max = 100f,
            indeterminate = false
        )
        assertEquals(50f, component.value)
        assertEquals(50f, component.percentage)
        assertFalse(component.indeterminate)
    }

    @Test
    fun testProgressBarIndeterminate() {
        val component = ProgressBarComponent(
            value = 0f,
            indeterminate = true
        )
        assertTrue(component.indeterminate)
    }

    // ====================
    // DISPLAY COMPONENTS (5 tests)
    // ====================

    @Test
    fun testCircularProgressRendererCreation() {
        val renderer = IOSCircularProgressRenderer()
        assertNotNull(renderer)
    }

    @Test
    fun testWebViewRendererCreation() {
        val renderer = IOSWebViewRenderer()
        assertNotNull(renderer)
    }

    @Test
    fun testVideoPlayerRendererCreation() {
        val renderer = IOSVideoPlayerRenderer()
        assertNotNull(renderer)
    }

    // ====================
    // INTEGRATION TESTS (5 tests)
    // ====================

    @Test
    fun testTimeFormatting() {
        val time = Time(14, 30)
        assertEquals("14:30", time.format(is24Hour = true))
        assertEquals("2:30 PM", time.format(is24Hour = false))
    }

    @Test
    fun testDropdownOptionValidation() {
        assertFails {
            DropdownOption("", "Empty value")
        }
        assertFails {
            DropdownOption("value", "")
        }
    }

    @Test
    fun testBottomNavItemWithBadge() {
        val item = BottomNavItem("home", "Home", badge = "5")
        assertEquals("5", item.badge)
    }

    @Test
    fun testTabWithContent() {
        val tab = Tab("Overview", "dashboard", content = "Tab content")
        assertEquals("Tab content", tab.content)
    }

    @Test
    fun testDrawerPositions() {
        val leftDrawer = DrawerComponent(
            position = DrawerPosition.Left,
            items = listOf(DrawerItem("id", "home", "Home"))
        )
        assertEquals(DrawerPosition.Left, leftDrawer.position)

        val rightDrawer = DrawerComponent(
            position = DrawerPosition.Right,
            items = listOf(DrawerItem("id", "home", "Home"))
        )
        assertEquals(DrawerPosition.Right, rightDrawer.position)
    }
}
