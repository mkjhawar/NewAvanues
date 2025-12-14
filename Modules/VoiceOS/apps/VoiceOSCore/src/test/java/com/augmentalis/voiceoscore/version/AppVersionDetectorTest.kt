/**
 * AppVersionDetectorTest.kt - Unit tests for AppVersionDetector
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-12-14
 *
 * Tests version detection logic using Robolectric + Mockito.
 */

package com.augmentalis.voiceoscore.version

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import com.augmentalis.database.dto.AppVersionDTO
import com.augmentalis.database.repositories.IAppVersionRepository
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for AppVersionDetector.
 *
 * ## Test Coverage:
 * - All 5 VersionChange variants (FirstInstall, Updated, Downgraded, NoChange, AppNotInstalled)
 * - PackageManager integration (mocked)
 * - Repository integration (mocked)
 * - Batch operations
 * - API compatibility (handled by Robolectric)
 *
 * ## Testing Strategy:
 * - Robolectric for Android framework (Context, PackageManager)
 * - Mockito for repository mocking
 * - Coroutines test for suspend functions
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28]) // API 28 for longVersionCode testing
class AppVersionDetectorTest {

    private lateinit var context: Context

    @Mock
    private lateinit var mockRepository: IAppVersionRepository

    @Mock
    private lateinit var mockPackageManager: PackageManager

    private lateinit var detector: AppVersionDetector

    companion object {
        private const val TEST_PACKAGE = "com.google.android.gm"
        private const val TEST_VERSION_NAME_V1 = "8.2024.11.100"
        private const val TEST_VERSION_CODE_V1 = 82024100L
        private const val TEST_VERSION_NAME_V2 = "8.2024.12.123"
        private const val TEST_VERSION_CODE_V2 = 82024123L
    }

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        context = ApplicationProvider.getApplicationContext()

        // Create detector with mocked repository
        // Note: We can't easily mock PackageManager in constructor,
        // so we'll test with real Robolectric PackageManager
        detector = AppVersionDetector(context, mockRepository)
    }

    // ========================================================================
    // Test 1: First Install - App never seen before
    // ========================================================================

    @Test
    fun `detectVersionChange - app not in database - returns FirstInstall`() = runTest {
        // Arrange: App is installed but not in database
        installApp(TEST_PACKAGE, TEST_VERSION_NAME_V1, TEST_VERSION_CODE_V1)
        whenever(mockRepository.getAppVersion(TEST_PACKAGE)).thenReturn(null)

        // Act
        val result = detector.detectVersionChange(TEST_PACKAGE)

        // Assert
        assertIs<VersionChange.FirstInstall>(result)
        assertEquals(TEST_PACKAGE, result.packageName)
        assertEquals(TEST_VERSION_NAME_V1, result.current.versionName)
        assertEquals(TEST_VERSION_CODE_V1, result.current.versionCode)

        // Verify repository was queried
        verify(mockRepository).getAppVersion(TEST_PACKAGE)
    }

    // ========================================================================
    // Test 2: App Updated - Version code increased
    // ========================================================================

    @Test
    fun `detectVersionChange - app updated to newer version - returns Updated`() = runTest {
        // Arrange: App installed with V2, database has V1
        installApp(TEST_PACKAGE, TEST_VERSION_NAME_V2, TEST_VERSION_CODE_V2)

        val dbVersion = AppVersionDTO(
            packageName = TEST_PACKAGE,
            versionName = TEST_VERSION_NAME_V1,
            versionCode = TEST_VERSION_CODE_V1,
            lastChecked = System.currentTimeMillis()
        )
        whenever(mockRepository.getAppVersion(TEST_PACKAGE)).thenReturn(dbVersion)

        // Act
        val result = detector.detectVersionChange(TEST_PACKAGE)

        // Assert
        assertIs<VersionChange.Updated>(result)
        assertEquals(TEST_PACKAGE, result.packageName)
        assertEquals(TEST_VERSION_CODE_V1, result.previous.versionCode)
        assertEquals(TEST_VERSION_CODE_V2, result.current.versionCode)
        assertEquals(TEST_VERSION_CODE_V2 - TEST_VERSION_CODE_V1, result.getVersionDelta())

        // Verify it requires verification
        assertTrue(result.requiresVerification())
        assertFalse(result.requiresCleanup())
    }

    // ========================================================================
    // Test 3: App Downgraded - Version code decreased
    // ========================================================================

    @Test
    fun `detectVersionChange - app downgraded to older version - returns Downgraded`() = runTest {
        // Arrange: App installed with V1, database has V2
        installApp(TEST_PACKAGE, TEST_VERSION_NAME_V1, TEST_VERSION_CODE_V1)

        val dbVersion = AppVersionDTO(
            packageName = TEST_PACKAGE,
            versionName = TEST_VERSION_NAME_V2,
            versionCode = TEST_VERSION_CODE_V2,
            lastChecked = System.currentTimeMillis()
        )
        whenever(mockRepository.getAppVersion(TEST_PACKAGE)).thenReturn(dbVersion)

        // Act
        val result = detector.detectVersionChange(TEST_PACKAGE)

        // Assert
        assertIs<VersionChange.Downgraded>(result)
        assertEquals(TEST_PACKAGE, result.packageName)
        assertEquals(TEST_VERSION_CODE_V2, result.previous.versionCode)
        assertEquals(TEST_VERSION_CODE_V1, result.current.versionCode)
        assertEquals(TEST_VERSION_CODE_V1 - TEST_VERSION_CODE_V2, result.getVersionDelta())

        // Verify it requires verification
        assertTrue(result.requiresVerification())
        assertFalse(result.requiresCleanup())
    }

    // ========================================================================
    // Test 4: No Change - Same version
    // ========================================================================

    @Test
    fun `detectVersionChange - same version as database - returns NoChange`() = runTest {
        // Arrange: App and database have same version
        installApp(TEST_PACKAGE, TEST_VERSION_NAME_V1, TEST_VERSION_CODE_V1)

        val dbVersion = AppVersionDTO(
            packageName = TEST_PACKAGE,
            versionName = TEST_VERSION_NAME_V1,
            versionCode = TEST_VERSION_CODE_V1,
            lastChecked = System.currentTimeMillis()
        )
        whenever(mockRepository.getAppVersion(TEST_PACKAGE)).thenReturn(dbVersion)

        // Act
        val result = detector.detectVersionChange(TEST_PACKAGE)

        // Assert
        assertIs<VersionChange.NoChange>(result)
        assertEquals(TEST_PACKAGE, result.packageName)
        assertEquals(TEST_VERSION_CODE_V1, result.version.versionCode)

        // Verify no action required
        assertFalse(result.requiresVerification())
        assertFalse(result.requiresCleanup())
    }

    // ========================================================================
    // Test 5: App Not Installed - Uninstalled or never existed
    // ========================================================================

    @Test
    fun `detectVersionChange - app not installed - returns AppNotInstalled`() = runTest {
        // Arrange: App not installed
        // (Don't call installApp - leave PackageManager empty)
        whenever(mockRepository.getAppVersion(TEST_PACKAGE)).thenReturn(null)

        // Act
        val result = detector.detectVersionChange(TEST_PACKAGE)

        // Assert
        assertIs<VersionChange.AppNotInstalled>(result)
        assertEquals(TEST_PACKAGE, result.packageName)

        // Verify it requires cleanup
        assertFalse(result.requiresVerification())
        assertTrue(result.requiresCleanup())
    }

    // ========================================================================
    // Test 6: Detect All Version Changes - Batch operation
    // ========================================================================

    @Test
    fun `detectAllVersionChanges - multiple apps - returns all changes`() = runTest {
        // Arrange: 3 apps with different states
        val app1 = "com.google.android.gm"
        val app2 = "com.android.chrome"
        val app3 = "com.spotify.music"

        // App1: Updated
        installApp(app1, "2.0.0", 200L)
        val dbVersions = mapOf(
            app1 to AppVersionDTO(app1, "1.0.0", 100L, System.currentTimeMillis()),
            app2 to AppVersionDTO(app2, "3.0.0", 300L, System.currentTimeMillis()),
            app3 to AppVersionDTO(app3, "5.0.0", 500L, System.currentTimeMillis())
        )
        whenever(mockRepository.getAllAppVersions()).thenReturn(dbVersions)
        whenever(mockRepository.getAppVersion(app1)).thenReturn(dbVersions[app1])
        whenever(mockRepository.getAppVersion(app2)).thenReturn(dbVersions[app2])
        whenever(mockRepository.getAppVersion(app3)).thenReturn(dbVersions[app3])

        // App2: Not installed (uninstalled)
        // App3: Not installed (uninstalled)

        // Act
        val results = detector.detectAllVersionChanges()

        // Assert
        assertEquals(3, results.size)

        // App1 should be Updated
        val app1Result = results.find { it.getPackageName() == app1 }
        assertNotNull(app1Result)
        assertIs<VersionChange.Updated>(app1Result)

        // App2 and App3 should be AppNotInstalled
        val app2Result = results.find { it.getPackageName() == app2 }
        assertNotNull(app2Result)
        assertIs<VersionChange.AppNotInstalled>(app2Result)

        val app3Result = results.find { it.getPackageName() == app3 }
        assertNotNull(app3Result)
        assertIs<VersionChange.AppNotInstalled>(app3Result)
    }

    // ========================================================================
    // Test 7: Get Installed Version - Valid app
    // ========================================================================

    @Test
    fun `isAppInstalled - installed app - returns true`() = runTest {
        // Arrange
        installApp(TEST_PACKAGE, TEST_VERSION_NAME_V1, TEST_VERSION_CODE_V1)

        // Act
        val result = detector.isAppInstalled(TEST_PACKAGE)

        // Assert
        assertTrue(result)
    }

    // ========================================================================
    // Test 8: Get Installed Version - Invalid app
    // ========================================================================

    @Test
    fun `isAppInstalled - not installed app - returns false`() = runTest {
        // Arrange: Don't install app

        // Act
        val result = detector.isAppInstalled(TEST_PACKAGE)

        // Assert
        assertFalse(result)
    }

    // ========================================================================
    // Test 9: Get Installed Versions - Batch query
    // ========================================================================

    @Test
    fun `getInstalledVersions - multiple packages - returns map with nulls for missing`() = runTest {
        // Arrange
        val app1 = "com.google.android.gm"
        val app2 = "com.android.chrome"
        val app3 = "com.nonexistent.app"

        installApp(app1, "1.0.0", 100L)
        installApp(app2, "2.0.0", 200L)
        // Don't install app3

        // Act
        val results = detector.getInstalledVersions(listOf(app1, app2, app3))

        // Assert
        assertEquals(3, results.size)

        // App1 and App2 should have versions
        assertNotNull(results[app1])
        assertEquals(100L, results[app1]?.versionCode)

        assertNotNull(results[app2])
        assertEquals(200L, results[app2]?.versionCode)

        // App3 should be null
        assertNull(results[app3])
    }

    // ========================================================================
    // Test 10: Input Validation - Blank package name
    // ========================================================================

    @Test(expected = IllegalArgumentException::class)
    fun `detectVersionChange - blank package name - throws IllegalArgumentException`() = runTest {
        // Act: Should throw
        detector.detectVersionChange("")
    }

    // ========================================================================
    // Test 11: VersionChange Helper Methods
    // ========================================================================

    @Test
    fun `VersionChange helper methods - return correct values`() {
        // Test getPackageName()
        val firstInstall = VersionChange.FirstInstall(
            TEST_PACKAGE,
            AppVersion(TEST_VERSION_NAME_V1, TEST_VERSION_CODE_V1)
        )
        assertEquals(TEST_PACKAGE, firstInstall.getPackageName())

        val updated = VersionChange.Updated(
            TEST_PACKAGE,
            AppVersion(TEST_VERSION_NAME_V1, TEST_VERSION_CODE_V1),
            AppVersion(TEST_VERSION_NAME_V2, TEST_VERSION_CODE_V2)
        )
        assertEquals(TEST_PACKAGE, updated.getPackageName())

        // Test getCurrentVersion()
        val currentVersion = updated.getCurrentVersion()
        assertNotNull(currentVersion)
        assertEquals(TEST_VERSION_CODE_V2, currentVersion.versionCode)

        val notInstalled = VersionChange.AppNotInstalled(TEST_PACKAGE)
        assertNull(notInstalled.getCurrentVersion())

        // Test requiresVerification()
        assertTrue(updated.requiresVerification())
        assertFalse(firstInstall.requiresVerification())
        assertFalse(notInstalled.requiresVerification())

        // Test requiresCleanup()
        assertTrue(notInstalled.requiresCleanup())
        assertFalse(updated.requiresCleanup())
        assertFalse(firstInstall.requiresCleanup())
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    /**
     * Install an app in Robolectric's PackageManager.
     *
     * @param packageName App package
     * @param versionName Version name string
     * @param versionCode Version code number
     */
    @Suppress("DEPRECATION")
    private fun installApp(packageName: String, versionName: String, versionCode: Long) {
        val packageInfo = PackageInfo().apply {
            this.packageName = packageName
            this.versionName = versionName

            // Set version code (handle API 28+ compatibility)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                this.longVersionCode = versionCode
            } else {
                this.versionCode = versionCode.toInt()
            }
        }

        // Install package via Robolectric's shadow PackageManager
        org.robolectric.Shadows.shadowOf(context.packageManager).installPackage(packageInfo)
    }
}
