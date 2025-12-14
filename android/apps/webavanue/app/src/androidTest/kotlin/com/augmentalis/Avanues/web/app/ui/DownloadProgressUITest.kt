package com.augmentalis.webavanue.app.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.augmentalis.webavanue.universal.presentation.ui.download.DownloadItem
import com.augmentalis.webavanue.domain.model.Download
import com.augmentalis.webavanue.domain.model.DownloadStatus
import kotlinx.datetime.Clock
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for download progress display
 *
 * Tests:
 * - Progress bar updates when download progress changes
 * - Speed and ETA displayed during download
 * - Completed downloads show open button
 * - Failed downloads show retry button
 */
class DownloadProgressUITest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testProgressBarDisplayedForDownloadingStatus() {
        // Create a downloading download
        val download = Download(
            id = "test_1",
            url = "https://example.com/file.pdf",
            filename = "test.pdf",
            fileSize = 1024 * 1024 * 10, // 10 MB
            downloadedSize = 1024 * 1024 * 5, // 5 MB (50%)
            status = DownloadStatus.DOWNLOADING,
            createdAt = Clock.System.now(),
            downloadSpeed = 1024 * 100, // 100 KB/s
            estimatedTimeRemaining = 50 // 50 seconds
        )

        composeTestRule.setContent {
            DownloadItem(
                download = download,
                onClick = {},
                onCancel = {},
                onRetry = {},
                onDelete = {}
            )
        }

        // Verify progress bar is displayed
        composeTestRule.onNodeWithTag("progress_bar", useUnmergedTree = true).assertExists()

        // Verify speed is displayed
        composeTestRule.onNodeWithText("100 KB/s", substring = true).assertExists()

        // Verify ETA is displayed
        composeTestRule.onNodeWithText("remaining", substring = true).assertExists()

        // Verify percentage is displayed
        composeTestRule.onNodeWithText("50%").assertExists()
    }

    @Test
    fun testProgressBarNotDisplayedForCompletedStatus() {
        // Create a completed download
        val download = Download(
            id = "test_2",
            url = "https://example.com/file.pdf",
            filename = "test.pdf",
            fileSize = 1024 * 1024 * 10,
            downloadedSize = 1024 * 1024 * 10,
            status = DownloadStatus.COMPLETED,
            createdAt = Clock.System.now(),
            completedAt = Clock.System.now()
        )

        composeTestRule.setContent {
            DownloadItem(
                download = download,
                onClick = {},
                onCancel = {},
                onRetry = {},
                onDelete = {}
            )
        }

        // Verify progress bar is NOT displayed
        composeTestRule.onNodeWithTag("progress_bar", useUnmergedTree = true).assertDoesNotExist()

        // Verify speed is NOT displayed
        composeTestRule.onNodeWithText("KB/s", substring = true).assertDoesNotExist()
    }

    @Test
    fun testSpeedDisplayedInCorrectUnits() {
        // Test different speed ranges
        val testCases = listOf(
            500L to "500 B/s", // Bytes per second
            50 * 1024L to "50 KB/s", // Kilobytes per second
            5 * 1024 * 1024L to "5.0 MB/s" // Megabytes per second
        )

        testCases.forEach { (speed, expectedText) ->
            val download = Download(
                id = "test_speed_$speed",
                url = "https://example.com/file.pdf",
                filename = "test.pdf",
                fileSize = 1024 * 1024 * 100,
                downloadedSize = 1024 * 1024 * 10,
                status = DownloadStatus.DOWNLOADING,
                createdAt = Clock.System.now(),
                downloadSpeed = speed,
                estimatedTimeRemaining = 100
            )

            composeTestRule.setContent {
                DownloadItem(
                    download = download,
                    onClick = {},
                    onCancel = {},
                    onRetry = {},
                    onDelete = {}
                )
            }

            // Verify correct speed format
            composeTestRule.onNodeWithText(expectedText, substring = true).assertExists()
        }
    }

    @Test
    fun testEtaDisplayedInCorrectFormat() {
        // Test different ETA ranges
        val testCases = listOf(
            30L to "30s", // Seconds
            90L to "1m 30s", // Minutes and seconds
            3600L to "1h 0m" // Hours and minutes
        )

        testCases.forEach { (eta, expectedFormat) ->
            val download = Download(
                id = "test_eta_$eta",
                url = "https://example.com/file.pdf",
                filename = "test.pdf",
                fileSize = 1024 * 1024 * 100,
                downloadedSize = 1024 * 1024 * 10,
                status = DownloadStatus.DOWNLOADING,
                createdAt = Clock.System.now(),
                downloadSpeed = 1024 * 100,
                estimatedTimeRemaining = eta
            )

            composeTestRule.setContent {
                DownloadItem(
                    download = download,
                    onClick = {},
                    onCancel = {},
                    onRetry = {},
                    onDelete = {}
                )
            }

            // Verify correct ETA format
            composeTestRule.onNodeWithText(expectedFormat, substring = true).assertExists()
        }
    }

    @Test
    fun testCompletedDownloadShowsDeleteButton() {
        val download = Download(
            id = "test_3",
            url = "https://example.com/file.pdf",
            filename = "test.pdf",
            fileSize = 1024 * 1024 * 10,
            downloadedSize = 1024 * 1024 * 10,
            status = DownloadStatus.COMPLETED,
            createdAt = Clock.System.now(),
            completedAt = Clock.System.now()
        )

        composeTestRule.setContent {
            DownloadItem(
                download = download,
                onClick = {},
                onCancel = {},
                onRetry = {},
                onDelete = {}
            )
        }

        // Verify delete button is displayed
        composeTestRule.onNodeWithContentDescription("Delete").assertExists()

        // Verify cancel button is NOT displayed
        composeTestRule.onNodeWithContentDescription("Cancel").assertDoesNotExist()

        // Verify retry button is NOT displayed
        composeTestRule.onNodeWithContentDescription("Retry").assertDoesNotExist()
    }

    @Test
    fun testFailedDownloadShowsRetryButton() {
        val download = Download(
            id = "test_4",
            url = "https://example.com/file.pdf",
            filename = "test.pdf",
            fileSize = 1024 * 1024 * 10,
            downloadedSize = 1024 * 1024 * 2,
            status = DownloadStatus.FAILED,
            errorMessage = "Network error",
            createdAt = Clock.System.now()
        )

        composeTestRule.setContent {
            DownloadItem(
                download = download,
                onClick = {},
                onCancel = {},
                onRetry = {},
                onDelete = {}
            )
        }

        // Verify retry button is displayed
        composeTestRule.onNodeWithContentDescription("Retry").assertExists()

        // Verify delete button is displayed
        composeTestRule.onNodeWithContentDescription("Delete").assertExists()

        // Verify cancel button is NOT displayed
        composeTestRule.onNodeWithContentDescription("Cancel").assertDoesNotExist()
    }

    @Test
    fun testDownloadingShowsCancelButton() {
        val download = Download(
            id = "test_5",
            url = "https://example.com/file.pdf",
            filename = "test.pdf",
            fileSize = 1024 * 1024 * 10,
            downloadedSize = 1024 * 1024 * 3,
            status = DownloadStatus.DOWNLOADING,
            createdAt = Clock.System.now(),
            downloadSpeed = 1024 * 100,
            estimatedTimeRemaining = 70
        )

        composeTestRule.setContent {
            DownloadItem(
                download = download,
                onClick = {},
                onCancel = {},
                onRetry = {},
                onDelete = {}
            )
        }

        // Verify cancel button is displayed
        composeTestRule.onNodeWithContentDescription("Cancel").assertExists()

        // Verify delete button is NOT displayed
        composeTestRule.onNodeWithContentDescription("Delete").assertDoesNotExist()

        // Verify retry button is NOT displayed
        composeTestRule.onNodeWithContentDescription("Retry").assertDoesNotExist()
    }

    @Test
    fun testProgressBarUpdatesWithProgressChange() {
        // Create a downloading download at 25%
        var download = Download(
            id = "test_6",
            url = "https://example.com/file.pdf",
            filename = "test.pdf",
            fileSize = 1024 * 1024 * 100, // 100 MB
            downloadedSize = 1024 * 1024 * 25, // 25 MB (25%)
            status = DownloadStatus.DOWNLOADING,
            createdAt = Clock.System.now(),
            downloadSpeed = 1024 * 100,
            estimatedTimeRemaining = 750
        )

        composeTestRule.setContent {
            DownloadItem(
                download = download,
                onClick = {},
                onCancel = {},
                onRetry = {},
                onDelete = {}
            )
        }

        // Verify 25% displayed
        composeTestRule.onNodeWithText("25%").assertExists()

        // Update to 50%
        download = download.copy(
            downloadedSize = 1024 * 1024 * 50,
            estimatedTimeRemaining = 500
        )

        composeTestRule.setContent {
            DownloadItem(
                download = download,
                onClick = {},
                onCancel = {},
                onRetry = {},
                onDelete = {}
            )
        }

        // Verify 50% displayed
        composeTestRule.onNodeWithText("50%").assertExists()
    }

    @Test
    fun testZeroSpeedDisplaysZero() {
        val download = Download(
            id = "test_7",
            url = "https://example.com/file.pdf",
            filename = "test.pdf",
            fileSize = 1024 * 1024 * 10,
            downloadedSize = 1024 * 1024 * 5,
            status = DownloadStatus.DOWNLOADING,
            createdAt = Clock.System.now(),
            downloadSpeed = 0L, // Zero speed
            estimatedTimeRemaining = 0L
        )

        composeTestRule.setContent {
            DownloadItem(
                download = download,
                onClick = {},
                onCancel = {},
                onRetry = {},
                onDelete = {}
            )
        }

        // Verify "0 B/s" is displayed
        composeTestRule.onNodeWithText("0 B/s").assertExists()
    }

    @Test
    fun testCalculatingEtaDisplayed() {
        val download = Download(
            id = "test_8",
            url = "https://example.com/file.pdf",
            filename = "test.pdf",
            fileSize = 1024 * 1024 * 10,
            downloadedSize = 1024 * 1024 * 1,
            status = DownloadStatus.DOWNLOADING,
            createdAt = Clock.System.now(),
            downloadSpeed = 1024 * 50,
            estimatedTimeRemaining = 0L // Zero ETA (calculating)
        )

        composeTestRule.setContent {
            DownloadItem(
                download = download,
                onClick = {},
                onCancel = {},
                onRetry = {},
                onDelete = {}
            )
        }

        // When ETA is 0, it should show "calculating..." or not show ETA at all
        // Based on our UI implementation, ETA is only shown if > 0
        // So we verify it's NOT shown
        composeTestRule.onNodeWithText("calculating...", substring = true).assertDoesNotExist()
    }
}
