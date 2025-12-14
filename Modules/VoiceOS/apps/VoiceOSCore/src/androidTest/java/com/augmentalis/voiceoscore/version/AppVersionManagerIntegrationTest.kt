/**
 * AppVersionManagerIntegrationTest.kt - Integration tests for AppVersionManager
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-12-14
 *
 * End-to-end integration tests for complete version lifecycle workflow.
 * Tests real database, PackageManager, and all repository interactions.
 */

package com.augmentalis.voiceoscore.version

import android.content.Context
import android.content.pm.PackageInfo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.augmentalis.database.VoiceOSDatabase
import com.augmentalis.database.dto.GeneratedCommandDTO
import com.augmentalis.database.migrations.DatabaseMigrations
import com.augmentalis.database.repositories.IAppVersionRepository
import com.augmentalis.database.repositories.IGeneratedCommandRepository
import com.augmentalis.database.repositories.impl.SQLDelightAppVersionRepository
import com.augmentalis.database.repositories.impl.SQLDelightGeneratedCommandRepository
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Integration tests for AppVersionManager.
 *
 * ## Test Strategy:
 * - Real SQLDelight database (in-memory)
 * - Real repository implementations
 * - Robolectric shadow PackageManager for app installation
 * - Tests complete end-to-end workflows
 *
 * ## Coverage:
 * - Version change detection → database updates → command lifecycle
 * - Batch operations on multiple apps
 * - Deprecated command cleanup with grace period
 * - Statistics and monitoring
 */
@RunWith(AndroidJUnit4::class)
class AppVersionManagerIntegrationTest {

    private lateinit var context: Context
    private lateinit var database: VoiceOSDatabase
    private lateinit var versionRepo: IAppVersionRepository
    private lateinit var commandRepo: IGeneratedCommandRepository
    private lateinit var detector: AppVersionDetector
    private lateinit var manager: AppVersionManager

    companion object {
        private const val TEST_APP_GMAIL = "com.google.android.gm"
        private const val TEST_APP_CHROME = "com.android.chrome"
        private const val TEST_APP_SPOTIFY = "com.spotify.music"
    }

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()

        // Create in-memory database
        val driver = AndroidSqliteDriver(
            schema = VoiceOSDatabase.Schema,
            context = context,
            name = null // null = in-memory
        )

        // Run migrations
        DatabaseMigrations.migrate(driver, 1, 3)

        database = VoiceOSDatabase(driver)

        // Create repositories
        versionRepo = SQLDelightAppVersionRepository(database)
        commandRepo = SQLDelightGeneratedCommandRepository(database)

        // Create detector and manager
        detector = AppVersionDetector(context, versionRepo)
        manager = AppVersionManager(context, detector, versionRepo, commandRepo)
    }

    @After
    fun teardown() {
        database.close()
    }

    // ========================================================================
    // Test 1: App Updated - Commands Marked Deprecated
    // ========================================================================

    @Test
    fun testCheckAndUpdateApp_appUpdated_marksCommandsDeprecated() = runTest {
        // Arrange: Install Gmail v1 with 50 commands
        installApp(TEST_APP_GMAIL, "1.0.0", 100L)

        // Store v1 in database
        versionRepo.upsertAppVersion(TEST_APP_GMAIL, "1.0.0", 100L)

        // Insert 50 commands for v1
        repeat(50) { i ->
            commandRepo.insert(
                createTestCommand(
                    elementHash = "element_$i",
                    commandText = "command $i",
                    appId = TEST_APP_GMAIL,
                    versionCode = 100L
                )
            )
        }

        // Verify setup
        assertEquals(50, commandRepo.getByPackage(TEST_APP_GMAIL).size)

        // Act: Update Gmail to v2
        uninstallApp(TEST_APP_GMAIL)
        installApp(TEST_APP_GMAIL, "2.0.0", 200L)

        val change = manager.checkAndUpdateApp(TEST_APP_GMAIL)

        // Assert: Version change detected
        assertTrue(change is VersionChange.Updated)
        assertEquals(100L, (change as VersionChange.Updated).previous.versionCode)
        assertEquals(200L, change.current.versionCode)

        // Assert: All 50 commands marked deprecated
        val deprecated = commandRepo.getDeprecatedCommands(TEST_APP_GMAIL)
        assertEquals(50, deprecated.size)
        assertTrue(deprecated.all { it.isDeprecated == 1L })
        assertTrue(deprecated.all { it.versionCode == 100L })

        // Assert: Version updated in database
        val storedVersion = versionRepo.getAppVersion(TEST_APP_GMAIL)
        assertNotNull(storedVersion)
        assertEquals(200L, storedVersion.versionCode)
        assertEquals("2.0.0", storedVersion.versionName)
    }

    // ========================================================================
    // Test 2: App Uninstalled - Commands Deleted
    // ========================================================================

    @Test
    fun testCheckAndUpdateApp_appUninstalled_deletesCommands() = runTest {
        // Arrange: Install Gmail with 50 commands
        installApp(TEST_APP_GMAIL, "1.0.0", 100L)
        versionRepo.upsertAppVersion(TEST_APP_GMAIL, "1.0.0", 100L)

        repeat(50) { i ->
            commandRepo.insert(
                createTestCommand(
                    elementHash = "element_$i",
                    commandText = "command $i",
                    appId = TEST_APP_GMAIL,
                    versionCode = 100L
                )
            )
        }

        // Verify setup
        assertEquals(50, commandRepo.getByPackage(TEST_APP_GMAIL).size)
        assertNotNull(versionRepo.getAppVersion(TEST_APP_GMAIL))

        // Act: Uninstall Gmail
        uninstallApp(TEST_APP_GMAIL)
        val change = manager.checkAndUpdateApp(TEST_APP_GMAIL)

        // Assert: App detected as not installed
        assertTrue(change is VersionChange.AppNotInstalled)

        // Assert: All commands deleted
        assertEquals(0, commandRepo.getByPackage(TEST_APP_GMAIL).size)

        // Assert: Version record deleted
        assertEquals(null, versionRepo.getAppVersion(TEST_APP_GMAIL))
    }

    // ========================================================================
    // Test 3: First Install - Version Stored
    // ========================================================================

    @Test
    fun testCheckAndUpdateApp_firstInstall_insertsVersion() = runTest {
        // Arrange: Install Gmail (not in database yet)
        installApp(TEST_APP_GMAIL, "1.0.0", 100L)

        // Verify not in database
        assertEquals(null, versionRepo.getAppVersion(TEST_APP_GMAIL))

        // Act
        val change = manager.checkAndUpdateApp(TEST_APP_GMAIL)

        // Assert: First install detected
        assertTrue(change is VersionChange.FirstInstall)
        assertEquals(100L, (change as VersionChange.FirstInstall).current.versionCode)

        // Assert: Version stored in database
        val storedVersion = versionRepo.getAppVersion(TEST_APP_GMAIL)
        assertNotNull(storedVersion)
        assertEquals(100L, storedVersion.versionCode)
        assertEquals("1.0.0", storedVersion.versionName)
    }

    // ========================================================================
    // Test 4: Check All Apps - Multiple Updates Processed
    // ========================================================================

    @Test
    fun testCheckAllApps_multipleUpdates_processesAll() = runTest {
        // Arrange: Install 3 apps with different states
        // Gmail: Updated (v1 → v2)
        installApp(TEST_APP_GMAIL, "2.0.0", 200L)
        versionRepo.upsertAppVersion(TEST_APP_GMAIL, "1.0.0", 100L)
        repeat(20) { i ->
            commandRepo.insert(createTestCommand("gmail_$i", "cmd $i", TEST_APP_GMAIL, 100L))
        }

        // Chrome: First install
        installApp(TEST_APP_CHROME, "1.0.0", 100L)
        // Not in database

        // Spotify: Uninstalled
        versionRepo.upsertAppVersion(TEST_APP_SPOTIFY, "1.0.0", 100L)
        repeat(10) { i ->
            commandRepo.insert(createTestCommand("spotify_$i", "cmd $i", TEST_APP_SPOTIFY, 100L))
        }
        // Not installed

        // Act: Check all apps
        val processed = manager.checkAllTrackedApps()

        // Assert: All 3 apps processed (Gmail, Chrome via detectAll won't catch it, Spotify)
        // Actually, checkAllTrackedApps only checks apps in version DB
        // So: Gmail (updated), Spotify (not installed)
        assertTrue(processed >= 2)

        // Assert: Gmail commands deprecated
        val gmailDeprecated = commandRepo.getDeprecatedCommands(TEST_APP_GMAIL)
        assertEquals(20, gmailDeprecated.size)

        // Assert: Gmail version updated
        val gmailVersion = versionRepo.getAppVersion(TEST_APP_GMAIL)
        assertEquals(200L, gmailVersion?.versionCode)

        // Assert: Spotify commands deleted
        assertEquals(0, commandRepo.getByPackage(TEST_APP_SPOTIFY).size)

        // Assert: Spotify version deleted
        assertEquals(null, versionRepo.getAppVersion(TEST_APP_SPOTIFY))
    }

    // ========================================================================
    // Test 5: Cleanup Deprecated Commands - Old Commands Deleted
    // ========================================================================

    @Test
    fun testCleanupDeprecatedCommands_olderThan30Days_deletesOld() = runTest {
        // Arrange: Create commands with different deprecation times
        val now = System.currentTimeMillis()
        val thirtyDaysAgo = now - (30L * 24 * 60 * 60 * 1000)
        val fortyDaysAgo = now - (40L * 24 * 60 * 60 * 1000)

        // Old deprecated commands (40 days old) - should be deleted
        repeat(20) { i ->
            commandRepo.insert(
                createTestCommand(
                    elementHash = "old_$i",
                    commandText = "old cmd $i",
                    appId = TEST_APP_GMAIL,
                    versionCode = 100L,
                    isDeprecated = 1L,
                    lastVerified = fortyDaysAgo
                )
            )
        }

        // Recent deprecated commands (20 days old) - should be kept
        val twentyDaysAgo = now - (20L * 24 * 60 * 60 * 1000)
        repeat(15) { i ->
            commandRepo.insert(
                createTestCommand(
                    elementHash = "recent_$i",
                    commandText = "recent cmd $i",
                    appId = TEST_APP_GMAIL,
                    versionCode = 100L,
                    isDeprecated = 1L,
                    lastVerified = twentyDaysAgo
                )
            )
        }

        // Active commands - should be kept
        repeat(10) { i ->
            commandRepo.insert(
                createTestCommand(
                    elementHash = "active_$i",
                    commandText = "active cmd $i",
                    appId = TEST_APP_GMAIL,
                    versionCode = 200L,
                    isDeprecated = 0L
                )
            )
        }

        // Verify setup
        assertEquals(45, commandRepo.count())

        // Act: Cleanup commands older than 30 days
        val deletedCount = manager.cleanupDeprecatedCommands(
            olderThan = thirtyDaysAgo,
            keepUserApproved = false
        )

        // Assert: Only old deprecated commands deleted (20)
        assertEquals(20, deletedCount)

        // Assert: Recent deprecated + active still exist (15 + 10 = 25)
        assertEquals(25, commandRepo.count())
    }

    // ========================================================================
    // Test 6: Cleanup Deprecated Commands - User Approved Preserved
    // ========================================================================

    @Test
    fun testCleanupDeprecatedCommands_userApproved_preserves() = runTest {
        // Arrange: Old deprecated commands, some user-approved
        val fortyDaysAgo = System.currentTimeMillis() - (40L * 24 * 60 * 60 * 1000)

        // User-approved deprecated commands - should be preserved
        repeat(10) { i ->
            commandRepo.insert(
                createTestCommand(
                    elementHash = "approved_$i",
                    commandText = "approved cmd $i",
                    appId = TEST_APP_GMAIL,
                    versionCode = 100L,
                    isDeprecated = 1L,
                    lastVerified = fortyDaysAgo,
                    isUserApproved = 1L
                )
            )
        }

        // Non-approved deprecated commands - should be deleted
        repeat(15) { i ->
            commandRepo.insert(
                createTestCommand(
                    elementHash = "not_approved_$i",
                    commandText = "not approved cmd $i",
                    appId = TEST_APP_GMAIL,
                    versionCode = 100L,
                    isDeprecated = 1L,
                    lastVerified = fortyDaysAgo,
                    isUserApproved = 0L
                )
            )
        }

        // Verify setup
        assertEquals(25, commandRepo.count())

        // Act: Cleanup with keepUserApproved = true
        val deletedCount = manager.cleanupDeprecatedCommands(
            olderThan = fortyDaysAgo + 1000, // Slightly after
            keepUserApproved = true
        )

        // Assert: Only non-approved deleted (15)
        assertEquals(15, deletedCount)

        // Assert: User-approved still exist (10)
        assertEquals(10, commandRepo.count())

        // Verify all remaining are user-approved
        val remaining = commandRepo.getByPackage(TEST_APP_GMAIL)
        assertTrue(remaining.all { it.isUserApproved == 1L })
    }

    // ========================================================================
    // Test 7: Get Version Stats - Returns Accurate Metrics
    // ========================================================================

    @Test
    fun testGetVersionStats_multipleApps_returnsAccurate() = runTest {
        // Arrange: Multiple apps with commands
        // Gmail: 50 commands (20 deprecated)
        installApp(TEST_APP_GMAIL, "1.0.0", 100L)
        versionRepo.upsertAppVersion(TEST_APP_GMAIL, "1.0.0", 100L)
        repeat(30) { i ->
            commandRepo.insert(
                createTestCommand("gmail_$i", "cmd $i", TEST_APP_GMAIL, 100L, isDeprecated = 0L)
            )
        }
        repeat(20) { i ->
            commandRepo.insert(
                createTestCommand("gmail_dep_$i", "cmd $i", TEST_APP_GMAIL, 100L, isDeprecated = 1L)
            )
        }

        // Chrome: 30 commands (10 deprecated)
        installApp(TEST_APP_CHROME, "2.0.0", 200L)
        versionRepo.upsertAppVersion(TEST_APP_CHROME, "2.0.0", 200L)
        repeat(20) { i ->
            commandRepo.insert(
                createTestCommand("chrome_$i", "cmd $i", TEST_APP_CHROME, 200L, isDeprecated = 0L)
            )
        }
        repeat(10) { i ->
            commandRepo.insert(
                createTestCommand("chrome_dep_$i", "cmd $i", TEST_APP_CHROME, 200L, isDeprecated = 1L)
            )
        }

        // Act
        val stats = manager.getVersionStats()

        // Assert
        assertEquals(2, stats.trackedApps) // Gmail, Chrome
        assertEquals(80, stats.totalCommands) // 50 + 30
        assertEquals(30, stats.deprecatedCommands) // 20 + 10
        assertEquals(37.5, stats.deprecationRate, 0.1) // 30/80 * 100 = 37.5%
    }

    // ========================================================================
    // Test 8: Force Recheck App - Updates Timestamp
    // ========================================================================

    @Test
    fun testForceRecheckApp_noChanges_updatesTimestamp() = runTest {
        // Arrange: Install Gmail
        installApp(TEST_APP_GMAIL, "1.0.0", 100L)
        versionRepo.upsertAppVersion(TEST_APP_GMAIL, "1.0.0", 100L)

        // Get initial timestamp
        val initialVersion = versionRepo.getAppVersion(TEST_APP_GMAIL)
        assertNotNull(initialVersion)
        val initialTimestamp = initialVersion.lastChecked

        // Wait a bit
        Thread.sleep(100)

        // Act: Force recheck
        manager.forceRecheckApp(TEST_APP_GMAIL)

        // Assert: Timestamp updated
        val updatedVersion = versionRepo.getAppVersion(TEST_APP_GMAIL)
        assertNotNull(updatedVersion)
        assertTrue(updatedVersion.lastChecked > initialTimestamp)

        // Assert: Version unchanged
        assertEquals(100L, updatedVersion.versionCode)
        assertEquals("1.0.0", updatedVersion.versionName)
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    /**
     * Install an app in Robolectric's shadow PackageManager.
     */
    @Suppress("DEPRECATION")
    private fun installApp(packageName: String, versionName: String, versionCode: Long) {
        val packageInfo = PackageInfo().apply {
            this.packageName = packageName
            this.versionName = versionName

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                this.longVersionCode = versionCode
            } else {
                this.versionCode = versionCode.toInt()
            }
        }

        Shadows.shadowOf(context.packageManager).installPackage(packageInfo)
    }

    /**
     * Uninstall an app from Robolectric's shadow PackageManager.
     */
    private fun uninstallApp(packageName: String) {
        Shadows.shadowOf(context.packageManager).removePackage(packageName)
    }

    /**
     * Create a test command DTO.
     */
    private fun createTestCommand(
        elementHash: String,
        commandText: String,
        appId: String,
        versionCode: Long,
        isDeprecated: Long = 0L,
        lastVerified: Long? = System.currentTimeMillis(),
        isUserApproved: Long = 0L
    ): GeneratedCommandDTO {
        return GeneratedCommandDTO(
            id = 0, // Auto-generated
            elementHash = elementHash,
            commandText = commandText,
            actionType = "CLICK",
            confidence = 0.95,
            synonyms = null,
            isUserApproved = isUserApproved,
            usageCount = 0,
            lastUsed = null,
            createdAt = System.currentTimeMillis(),
            appId = appId,
            appVersion = "1.0.0",
            versionCode = versionCode,
            lastVerified = lastVerified,
            isDeprecated = isDeprecated
        )
    }
}
