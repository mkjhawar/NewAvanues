package com.augmentalis.webavanue

import com.augmentalis.webavanue.BrowserSettings
import com.augmentalis.webavanue.BrowserRepository
import com.augmentalis.webavanue.DownloadPathValidator
import com.augmentalis.webavanue.NetworkChecker
import com.augmentalis.webavanue.ValidationResult
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Test helpers for download integration tests
 *
 * Provides reusable mock configurations and test utilities
 * to reduce boilerplate across integration test files.
 */
object TestHelpers {

    /**
     * Create a mock BrowserRepository with configurable behavior
     *
     * @param initialSettings Initial settings to return
     * @return Mocked repository
     */
    fun createMockRepository(
        initialSettings: BrowserSettings = BrowserSettings.default()
    ): Pair<BrowserRepository, MutableStateFlow<BrowserSettings>> {
        val settingsFlow = MutableStateFlow(initialSettings)

        val repository = mockk<BrowserRepository>(relaxed = true) {
            coEvery { observeSettings() } returns settingsFlow
            coEvery { getSettings() } returns Result.success(settingsFlow.value)
            coEvery { updateSettings(any()) } answers {
                val newSettings = firstArg<BrowserSettings>()
                settingsFlow.value = newSettings
                Result.success(Unit)
            }
        }

        return repository to settingsFlow
    }

    /**
     * Create a mock NetworkChecker with WiFi connected
     *
     * @return Mocked network checker (WiFi enabled)
     */
    fun createMockNetworkChecker_WiFiConnected(): NetworkChecker {
        return mockk(relaxed = true) {
            every { isWiFiConnected() } returns true
            every { isCellularConnected() } returns false
            every { isConnected() } returns true
            every { getWiFiRequiredMessage() } returns null
        }
    }

    /**
     * Create a mock NetworkChecker with Cellular connected
     *
     * @return Mocked network checker (Cellular enabled)
     */
    fun createMockNetworkChecker_CellularConnected(): NetworkChecker {
        return mockk(relaxed = true) {
            every { isWiFiConnected() } returns false
            every { isCellularConnected() } returns true
            every { isConnected() } returns true
            every { getWiFiRequiredMessage() } returns "WiFi required. Currently on Cellular."
        }
    }

    /**
     * Create a mock NetworkChecker with no connection
     *
     * @return Mocked network checker (Offline)
     */
    fun createMockNetworkChecker_Offline(): NetworkChecker {
        return mockk(relaxed = true) {
            every { isWiFiConnected() } returns false
            every { isCellularConnected() } returns false
            every { isConnected() } returns false
            every { getWiFiRequiredMessage() } returns "WiFi required. No network connection."
        }
    }

    /**
     * Create a mock DownloadPathValidator with valid path
     *
     * @param availableSpaceMB Available space to report
     * @return Mocked validator (valid result)
     */
    fun createMockPathValidator_ValidPath(availableSpaceMB: Long = 500): DownloadPathValidator {
        return mockk(relaxed = true) {
            coEvery { validate(any()) } returns ValidationResult.success(availableSpaceMB)
        }
    }

    /**
     * Create a mock DownloadPathValidator with invalid path
     *
     * @param errorMessage Error message to return
     * @return Mocked validator (invalid result)
     */
    fun createMockPathValidator_InvalidPath(
        errorMessage: String = "Path no longer exists or permission revoked"
    ): DownloadPathValidator {
        return mockk(relaxed = true) {
            coEvery { validate(any()) } returns ValidationResult.failure(errorMessage)
        }
    }

    /**
     * Create a mock DownloadPathValidator with low space
     *
     * @param availableSpaceMB Available space (< 100MB for warning)
     * @return Mocked validator (low space warning)
     */
    fun createMockPathValidator_LowSpace(availableSpaceMB: Long = 50): DownloadPathValidator {
        return mockk(relaxed = true) {
            coEvery { validate(any()) } returns ValidationResult.success(availableSpaceMB)
        }
    }

    /**
     * Test settings presets for common scenarios
     */
    object Settings {
        /**
         * All download features disabled (default)
         */
        val allDisabled = BrowserSettings.default().copy(
            askDownloadLocation = false,
            downloadOverWiFiOnly = false,
            downloadPath = null
        )

        /**
         * Only askDownloadLocation enabled
         */
        val dialogOnly = BrowserSettings.default().copy(
            askDownloadLocation = true,
            downloadOverWiFiOnly = false,
            downloadPath = null
        )

        /**
         * Only downloadOverWiFiOnly enabled
         */
        val wifiOnly = BrowserSettings.default().copy(
            askDownloadLocation = false,
            downloadOverWiFiOnly = true,
            downloadPath = null
        )

        /**
         * Both dialog and WiFi check enabled
         */
        val dialogAndWiFi = BrowserSettings.default().copy(
            askDownloadLocation = true,
            downloadOverWiFiOnly = true,
            downloadPath = null
        )

        /**
         * All features enabled with custom path
         */
        val allEnabled = BrowserSettings.default().copy(
            askDownloadLocation = true,
            downloadOverWiFiOnly = true,
            downloadPath = "content://documents/custom"
        )
    }

    /**
     * Test paths for validation scenarios
     */
    object Paths {
        const val VALID_PATH = "content://com.android.externalstorage.documents/tree/primary%3ADownload"
        const val INVALID_PATH = "content://documents/removed-storage"
        const val CUSTOM_PATH = "content://documents/custom-folder"
        const val DEFAULT_PATH = "Downloads"
    }

    /**
     * Common error messages
     */
    object ErrorMessages {
        const val WIFI_REQUIRED_CELLULAR = "WiFi required. Currently on Cellular."
        const val WIFI_REQUIRED_OFFLINE = "WiFi required. No network connection."
        const val PATH_NOT_EXISTS = "Storage location no longer exists or permission revoked"
        const val PATH_NOT_WRITABLE = "Cannot write to this location"
        const val LOW_SPACE = "Low storage space available"
    }

    /**
     * Common test filenames
     */
    object Filenames {
        const val PDF = "document.pdf"
        const val IMAGE = "photo.jpg"
        const val VIDEO = "video.mp4"
        const val ZIP = "archive.zip"
        const val LARGE_FILE = "large-file.bin"
    }
}
