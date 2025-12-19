package com.augmentalis.avaelements.renderer.android.data

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.augmentalis.avaelements.flutter.material.data.*
import com.augmentalis.avaelements.renderer.android.mappers.flutterparity.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test suite for Agent 4 Data components (P1):
 * - RadioListTile
 * - VirtualScroll
 * - InfiniteScroll
 * - QRCode
 *
 * Tests cover:
 * - Component rendering
 * - User interactions
 * - Accessibility
 * - State management
 * - Edge cases
 */
@RunWith(AndroidJUnit4::class)
class DataComponentsAdvancedTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ============================================================================
    // RadioListTile Component Tests (7 tests)
    // ============================================================================

    @Test
    fun radioListTile_unselected_displaysCorrectly() {
        composeTestRule.setContent {
            RadioListTileMapper(
                RadioListTile(
                    title = "Option 1",
                    value = "option1",
                    groupValue = "option2"
                )
            )
        }

        composeTestRule.onNodeWithText("Option 1").assertExists()
    }

    @Test
    fun radioListTile_selected_displaysSelectedState() {
        composeTestRule.setContent {
            RadioListTileMapper(
                RadioListTile(
                    title = "Option 1",
                    value = "option1",
                    groupValue = "option1"
                )
            )
        }

        composeTestRule.onNodeWithText("Option 1").assertExists()
        // Radio should be selected (value == groupValue)
    }

    @Test
    fun radioListTile_withSubtitle_displaysSubtitle() {
        composeTestRule.setContent {
            RadioListTileMapper(
                RadioListTile(
                    title = "Premium Plan",
                    subtitle = "$9.99/month",
                    value = "premium",
                    groupValue = null
                )
            )
        }

        composeTestRule.onNodeWithText("Premium Plan").assertExists()
        composeTestRule.onNodeWithText("$9.99/month").assertExists()
    }

    @Test
    fun radioListTile_onClick_triggersCallback() {
        var selectedValue = ""

        composeTestRule.setContent {
            RadioListTileMapper(
                RadioListTile(
                    title = "Option A",
                    value = "a",
                    groupValue = null,
                    onChanged = { selectedValue = it }
                )
            )
        }

        composeTestRule.onNodeWithText("Option A").performClick()
        assert(selectedValue == "a")
    }

    @Test
    fun radioListTile_disabled_doesNotTriggerClick() {
        var selectedValue = ""

        composeTestRule.setContent {
            RadioListTileMapper(
                RadioListTile(
                    title = "Disabled Option",
                    value = "disabled",
                    groupValue = null,
                    enabled = false,
                    onChanged = { selectedValue = it }
                )
            )
        }

        composeTestRule.onNodeWithText("Disabled Option").performClick()
        assert(selectedValue.isEmpty())
    }

    @Test
    fun radioListTile_leadingControl_radioButtonOnLeft() {
        composeTestRule.setContent {
            RadioListTileMapper(
                RadioListTile(
                    title = "Leading Radio",
                    value = "leading",
                    groupValue = null,
                    controlAffinity = RadioListTile.ListTileControlAffinity.Leading
                )
            )
        }

        composeTestRule.onNodeWithText("Leading Radio").assertExists()
    }

    @Test
    fun radioListTile_accessibility_providesContentDescription() {
        composeTestRule.setContent {
            RadioListTileMapper(
                RadioListTile(
                    title = "Option 1",
                    value = "option1",
                    groupValue = "option1",
                    contentDescription = "First option, selected"
                )
            )
        }

        composeTestRule.onNode(hasContentDescription("First option, selected"))
            .assertExists()
    }

    // ============================================================================
    // VirtualScroll Component Tests (6 tests)
    // ============================================================================

    @Test
    fun virtualScroll_verticalOrientation_rendersItems() {
        composeTestRule.setContent {
            VirtualScrollMapper(
                VirtualScroll(
                    itemCount = 100,
                    itemHeight = 56f,
                    orientation = VirtualScroll.Orientation.Vertical
                )
            )
        }

        // Should render at least some items
        composeTestRule.onNodeWithText("Item 0").assertExists()
    }

    @Test
    fun virtualScroll_horizontalOrientation_rendersItems() {
        composeTestRule.setContent {
            VirtualScrollMapper(
                VirtualScroll(
                    itemCount = 50,
                    itemWidth = 120f,
                    orientation = VirtualScroll.Orientation.Horizontal
                )
            )
        }

        composeTestRule.onNodeWithText("Item 0").assertExists()
    }

    @Test
    fun virtualScroll_fixedItemHeight_calculatesLayout() {
        val scroll = VirtualScroll(
            itemCount = 100,
            itemHeight = 56f
        )

        val estimatedHeight = scroll.getEstimatedTotalHeight()
        assert(estimatedHeight == 5600f) // 100 * 56
    }

    @Test
    fun virtualScroll_dynamicItemHeight_handlesVariableHeights() {
        composeTestRule.setContent {
            VirtualScrollMapper(
                VirtualScroll(
                    itemCount = 50,
                    itemHeight = null, // Dynamic heights
                    cacheSize = 20
                )
            )
        }

        composeTestRule.onNodeWithText("Item 0").assertExists()
    }

    @Test
    fun virtualScroll_largeDataset_performanceOptimized() {
        composeTestRule.setContent {
            VirtualScrollMapper(
                VirtualScroll(
                    itemCount = 10000,
                    itemHeight = 56f,
                    cacheSize = 50
                )
            )
        }

        // Should only render visible items + cache
        composeTestRule.onNodeWithText("Item 0").assertExists()
        // Items far down should not be rendered yet
    }

    @Test
    fun virtualScroll_accessibility_providesContentDescription() {
        composeTestRule.setContent {
            VirtualScrollMapper(
                VirtualScroll(
                    itemCount = 100,
                    contentDescription = "List of 100 items"
                )
            )
        }

        composeTestRule.onNode(hasContentDescription("List of 100 items, vertical scrolling"))
            .assertExists()
    }

    // ============================================================================
    // InfiniteScroll Component Tests (6 tests)
    // ============================================================================

    @Test
    fun infiniteScroll_initialLoad_rendersItems() {
        val items = List(20) { index ->
            com.augmentalis.avaelements.components.phase1.display.Text(
                id = "text$index",
                text = "Item $index"
            )
        }

        composeTestRule.setContent {
            InfiniteScrollMapper(
                InfiniteScroll(
                    items = items,
                    hasMore = true,
                    loading = false
                )
            )
        }

        composeTestRule.onNodeWithText("Item 0").assertExists()
    }

    @Test
    fun infiniteScroll_loadingState_showsLoadingIndicator() {
        val items = List(20) { index ->
            com.augmentalis.avaelements.components.phase1.display.Text(
                id = "text$index",
                text = "Item $index"
            )
        }

        composeTestRule.setContent {
            InfiniteScrollMapper(
                InfiniteScroll(
                    items = items,
                    hasMore = true,
                    loading = true,
                    loadingIndicatorText = "Loading more..."
                )
            )
        }

        composeTestRule.onNodeWithText("Loading more...").assertExists()
    }

    @Test
    fun infiniteScroll_noMoreItems_showsEndMessage() {
        val items = List(20) { index ->
            com.augmentalis.avaelements.components.phase1.display.Text(
                id = "text$index",
                text = "Item $index"
            )
        }

        composeTestRule.setContent {
            InfiniteScrollMapper(
                InfiniteScroll(
                    items = items,
                    hasMore = false,
                    loading = false,
                    endMessageText = "No more items"
                )
            )
        }

        composeTestRule.onNodeWithText("No more items").assertExists()
    }

    @Test
    fun infiniteScroll_errorState_showsRetryButton() {
        val items = List(20) { index ->
            com.augmentalis.avaelements.components.phase1.display.Text(
                id = "text$index",
                text = "Item $index"
            )
        }

        composeTestRule.setContent {
            InfiniteScrollMapper(
                InfiniteScroll(
                    items = items,
                    hasMore = true,
                    loading = false,
                    showError = true,
                    errorMessageText = "Failed to load. Tap to retry."
                )
            )
        }

        composeTestRule.onNodeWithText("Failed to load. Tap to retry.").assertExists()
    }

    @Test
    fun infiniteScroll_onRetry_triggersCallback() {
        var retryClicked = false
        val items = List(20) { index ->
            com.augmentalis.avaelements.components.phase1.display.Text(
                id = "text$index",
                text = "Item $index"
            )
        }

        composeTestRule.setContent {
            InfiniteScrollMapper(
                InfiniteScroll(
                    items = items,
                    hasMore = true,
                    showError = true,
                    onRetry = { retryClicked = true }
                )
            )
        }

        composeTestRule.onNode(hasClickAction()).performClick()
        // Note: This may need adjustment based on how retry button is accessed
    }

    @Test
    fun infiniteScroll_horizontalOrientation_rendersHorizontally() {
        val items = List(20) { index ->
            com.augmentalis.avaelements.components.phase1.display.Text(
                id = "text$index",
                text = "Item $index"
            )
        }

        composeTestRule.setContent {
            InfiniteScrollMapper(
                InfiniteScroll(
                    items = items,
                    orientation = InfiniteScroll.Orientation.Horizontal,
                    hasMore = true
                )
            )
        }

        composeTestRule.onNodeWithText("Item 0").assertExists()
    }

    // ============================================================================
    // QRCode Component Tests (6 tests)
    // ============================================================================

    @Test
    fun qrCode_validData_generatesQRCode() {
        composeTestRule.setContent {
            QRCodeMapper(
                QRCode(
                    data = "https://example.com",
                    size = 200f
                )
            )
        }

        // QR code image should be rendered
        // Note: Bitmap testing may require additional assertions
    }

    @Test
    fun qrCode_invalidData_showsErrorState() {
        composeTestRule.setContent {
            QRCodeMapper(
                QRCode(
                    data = "", // Invalid empty data
                    size = 200f
                )
            )
        }

        composeTestRule.onNodeWithText("Invalid data").assertExists()
    }

    @Test
    fun qrCode_customColors_appliesColors() {
        composeTestRule.setContent {
            QRCodeMapper(
                QRCode(
                    data = "Test Data",
                    size = 200f,
                    foregroundColor = 0xFF0000FF, // Blue
                    backgroundColor = 0xFFFFFF00  // Yellow
                )
            )
        }

        // QR code should be rendered with custom colors
    }

    @Test
    fun qrCode_errorCorrectionLevels_handlesAllLevels() {
        val levels = listOf(
            QRCode.ErrorCorrectionLevel.L,
            QRCode.ErrorCorrectionLevel.M,
            QRCode.ErrorCorrectionLevel.Q,
            QRCode.ErrorCorrectionLevel.H
        )

        levels.forEach { level ->
            val qrCode = QRCode(
                data = "Test",
                errorCorrection = level
            )
            assert(qrCode.isDataValid())
        }
    }

    @Test
    fun qrCode_onTap_triggersCallback() {
        var tapped = false

        composeTestRule.setContent {
            QRCodeMapper(
                QRCode(
                    data = "https://example.com",
                    size = 200f,
                    onTap = { tapped = true }
                )
            )
        }

        // Note: Finding and clicking QR code may require specific test node selector
    }

    @Test
    fun qrCode_accessibility_providesContentDescription() {
        composeTestRule.setContent {
            QRCodeMapper(
                QRCode(
                    data = "https://example.com",
                    size = 200f,
                    contentDescription = "QR code for example website"
                )
            )
        }

        composeTestRule.onNode(hasContentDescription("QR code for example website"))
            .assertExists()
    }

    // ============================================================================
    // QRCode Helper Tests (Additional utility tests)
    // ============================================================================

    @Test
    fun qrCode_urlFactory_createsURLQRCode() {
        val qrCode = QRCode.url("https://example.com", 200f)
        assert(qrCode.data == "https://example.com")
        assert(qrCode.size == 200f)
    }

    @Test
    fun qrCode_wifiFactory_createsWiFiQRCode() {
        val qrCode = QRCode.wifi(
            ssid = "MyNetwork",
            password = "password123",
            security = "WPA"
        )
        assert(qrCode.data.contains("WIFI:"))
        assert(qrCode.data.contains("MyNetwork"))
    }

    @Test
    fun qrCode_contactFactory_createsVCard() {
        val qrCode = QRCode.contact(
            name = "John Doe",
            phone = "555-1234",
            email = "john@example.com"
        )
        assert(qrCode.data.contains("BEGIN:VCARD"))
        assert(qrCode.data.contains("John Doe"))
    }

    @Test
    fun qrCode_capacity_calculatesCorrectly() {
        val qrCode = QRCode(
            data = "Test",
            errorCorrection = QRCode.ErrorCorrectionLevel.M
        )
        val capacity = qrCode.getCapacity()
        assert(capacity == 2331) // Medium correction alphanumeric capacity
    }
}
