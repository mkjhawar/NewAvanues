/**
 * VersionChangeTest.kt - Unit tests for VersionChange sealed class
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-12-15
 *
 * Tests VersionChange sealed class behavior for Phase 2 Task 2.1
 * (Version-aware command lifecycle management)
 */

package com.augmentalis.voiceoscore.version

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for VersionChange sealed class and its 5 variants.
 *
 * Tests:
 * - All 5 variants: FirstInstall, Updated, Downgraded, NoChange, AppNotInstalled
 * - packageName property access (abstract property)
 * - getCurrentVersion() for each variant
 * - requiresVerification() logic
 * - requiresCleanup() logic
 * - getVersionDelta() for Updated/Downgraded
 */
class VersionChangeTest {

    companion object {
        private const val TEST_PACKAGE = "com.test.app"
        private val VERSION_1 = AppVersion("1.0.0", 100L)
        private val VERSION_2 = AppVersion("2.0.0", 200L)
    }

    // ========================================================================
    // Test: FirstInstall Variant
    // ========================================================================

    @Test
    fun `FirstInstall - has correct package name`() {
        val change = VersionChange.FirstInstall(
            packageName = TEST_PACKAGE,
            current = VERSION_1
        )

        assertEquals("Package name should match", TEST_PACKAGE, change.packageName)
    }

    @Test
    fun `FirstInstall - getCurrentVersion returns current version`() {
        val change = VersionChange.FirstInstall(
            packageName = TEST_PACKAGE,
            current = VERSION_2
        )

        val currentVersion = change.getCurrentVersion()
        assertNotNull("Current version should not be null", currentVersion)
        assertEquals("Current version code should be 200", 200L, currentVersion?.versionCode)
        assertEquals("Current version name should be 2.0.0", "2.0.0", currentVersion?.versionName)
    }

    @Test
    fun `FirstInstall - does not require verification`() {
        val change = VersionChange.FirstInstall(
            packageName = TEST_PACKAGE,
            current = VERSION_1
        )

        assertFalse("FirstInstall should not require verification", change.requiresVerification())
    }

    @Test
    fun `FirstInstall - does not require cleanup`() {
        val change = VersionChange.FirstInstall(
            packageName = TEST_PACKAGE,
            current = VERSION_1
        )

        assertFalse("FirstInstall should not require cleanup", change.requiresCleanup())
    }

    @Test
    fun `FirstInstall - toString contains package and version`() {
        val change = VersionChange.FirstInstall(
            packageName = TEST_PACKAGE,
            current = VERSION_1
        )

        val str = change.toString()
        assertTrue("toString should contain 'FirstInstall'", str.contains("FirstInstall"))
        assertTrue("toString should contain package name", str.contains(TEST_PACKAGE))
    }

    // ========================================================================
    // Test: Updated Variant
    // ========================================================================

    @Test
    fun `Updated - has correct package name`() {
        val change = VersionChange.Updated(
            packageName = TEST_PACKAGE,
            previous = VERSION_1,
            current = VERSION_2
        )

        assertEquals("Package name should match", TEST_PACKAGE, change.packageName)
    }

    @Test
    fun `Updated - getCurrentVersion returns current version`() {
        val change = VersionChange.Updated(
            packageName = TEST_PACKAGE,
            previous = VERSION_1,
            current = VERSION_2
        )

        val currentVersion = change.getCurrentVersion()
        assertNotNull("Current version should not be null", currentVersion)
        assertEquals("Current version code should be 200", 200L, currentVersion?.versionCode)
    }

    @Test
    fun `Updated - requires verification`() {
        val change = VersionChange.Updated(
            packageName = TEST_PACKAGE,
            previous = VERSION_1,
            current = VERSION_2
        )

        assertTrue("Updated should require verification", change.requiresVerification())
    }

    @Test
    fun `Updated - does not require cleanup`() {
        val change = VersionChange.Updated(
            packageName = TEST_PACKAGE,
            previous = VERSION_1,
            current = VERSION_2
        )

        assertFalse("Updated should not require cleanup", change.requiresCleanup())
    }

    @Test
    fun `Updated - getVersionDelta calculates positive delta`() {
        val change = VersionChange.Updated(
            packageName = TEST_PACKAGE,
            previous = VERSION_1,  // 100
            current = VERSION_2     // 200
        )

        assertEquals("Version delta should be 100", 100L, change.getVersionDelta())
    }

    @Test
    fun `Updated - getVersionDelta with large jump`() {
        val oldVersion = AppVersion("1.0.0", 100L)
        val newVersion = AppVersion("10.0.0", 1000L)

        val change = VersionChange.Updated(
            packageName = TEST_PACKAGE,
            previous = oldVersion,
            current = newVersion
        )

        assertEquals("Version delta should be 900", 900L, change.getVersionDelta())
    }

    @Test
    fun `Updated - toString contains previous and current versions`() {
        val change = VersionChange.Updated(
            packageName = TEST_PACKAGE,
            previous = VERSION_1,
            current = VERSION_2
        )

        val str = change.toString()
        assertTrue("toString should contain 'Updated'", str.contains("Updated"))
        assertTrue("toString should contain →", str.contains("→"))
    }

    // ========================================================================
    // Test: Downgraded Variant
    // ========================================================================

    @Test
    fun `Downgraded - has correct package name`() {
        val change = VersionChange.Downgraded(
            packageName = TEST_PACKAGE,
            previous = VERSION_2,
            current = VERSION_1
        )

        assertEquals("Package name should match", TEST_PACKAGE, change.packageName)
    }

    @Test
    fun `Downgraded - getCurrentVersion returns current version`() {
        val change = VersionChange.Downgraded(
            packageName = TEST_PACKAGE,
            previous = VERSION_2,
            current = VERSION_1
        )

        val currentVersion = change.getCurrentVersion()
        assertNotNull("Current version should not be null", currentVersion)
        assertEquals("Current version code should be 100", 100L, currentVersion?.versionCode)
    }

    @Test
    fun `Downgraded - requires verification`() {
        val change = VersionChange.Downgraded(
            packageName = TEST_PACKAGE,
            previous = VERSION_2,
            current = VERSION_1
        )

        assertTrue("Downgraded should require verification", change.requiresVerification())
    }

    @Test
    fun `Downgraded - does not require cleanup`() {
        val change = VersionChange.Downgraded(
            packageName = TEST_PACKAGE,
            previous = VERSION_2,
            current = VERSION_1
        )

        assertFalse("Downgraded should not require cleanup", change.requiresCleanup())
    }

    @Test
    fun `Downgraded - getVersionDelta calculates negative delta`() {
        val change = VersionChange.Downgraded(
            packageName = TEST_PACKAGE,
            previous = VERSION_2,  // 200
            current = VERSION_1     // 100
        )

        assertEquals("Version delta should be -100", -100L, change.getVersionDelta())
    }

    @Test
    fun `Downgraded - toString contains previous and current versions`() {
        val change = VersionChange.Downgraded(
            packageName = TEST_PACKAGE,
            previous = VERSION_2,
            current = VERSION_1
        )

        val str = change.toString()
        assertTrue("toString should contain 'Downgraded'", str.contains("Downgraded"))
        assertTrue("toString should contain →", str.contains("→"))
    }

    // ========================================================================
    // Test: NoChange Variant
    // ========================================================================

    @Test
    fun `NoChange - has correct package name`() {
        val change = VersionChange.NoChange(
            packageName = TEST_PACKAGE,
            version = VERSION_1
        )

        assertEquals("Package name should match", TEST_PACKAGE, change.packageName)
    }

    @Test
    fun `NoChange - getCurrentVersion returns version`() {
        val change = VersionChange.NoChange(
            packageName = TEST_PACKAGE,
            version = VERSION_1
        )

        val currentVersion = change.getCurrentVersion()
        assertNotNull("Current version should not be null", currentVersion)
        assertEquals("Current version code should be 100", 100L, currentVersion?.versionCode)
    }

    @Test
    fun `NoChange - does not require verification`() {
        val change = VersionChange.NoChange(
            packageName = TEST_PACKAGE,
            version = VERSION_1
        )

        assertFalse("NoChange should not require verification", change.requiresVerification())
    }

    @Test
    fun `NoChange - does not require cleanup`() {
        val change = VersionChange.NoChange(
            packageName = TEST_PACKAGE,
            version = VERSION_1
        )

        assertFalse("NoChange should not require cleanup", change.requiresCleanup())
    }

    @Test
    fun `NoChange - toString contains package and version`() {
        val change = VersionChange.NoChange(
            packageName = TEST_PACKAGE,
            version = VERSION_1
        )

        val str = change.toString()
        assertTrue("toString should contain 'NoChange'", str.contains("NoChange"))
        assertTrue("toString should contain package name", str.contains(TEST_PACKAGE))
    }

    // ========================================================================
    // Test: AppNotInstalled Variant
    // ========================================================================

    @Test
    fun `AppNotInstalled - has correct package name`() {
        val change = VersionChange.AppNotInstalled(
            packageName = TEST_PACKAGE
        )

        assertEquals("Package name should match", TEST_PACKAGE, change.packageName)
    }

    @Test
    fun `AppNotInstalled - getCurrentVersion returns null`() {
        val change = VersionChange.AppNotInstalled(
            packageName = TEST_PACKAGE
        )

        val currentVersion = change.getCurrentVersion()
        assertNull("Current version should be null for uninstalled app", currentVersion)
    }

    @Test
    fun `AppNotInstalled - does not require verification`() {
        val change = VersionChange.AppNotInstalled(
            packageName = TEST_PACKAGE
        )

        assertFalse("AppNotInstalled should not require verification", change.requiresVerification())
    }

    @Test
    fun `AppNotInstalled - requires cleanup`() {
        val change = VersionChange.AppNotInstalled(
            packageName = TEST_PACKAGE
        )

        assertTrue("AppNotInstalled should require cleanup", change.requiresCleanup())
    }

    @Test
    fun `AppNotInstalled - toString contains package name`() {
        val change = VersionChange.AppNotInstalled(
            packageName = TEST_PACKAGE
        )

        val str = change.toString()
        assertTrue("toString should contain 'AppNotInstalled'", str.contains("AppNotInstalled"))
        assertTrue("toString should contain package name", str.contains(TEST_PACKAGE))
    }

    // ========================================================================
    // Test: Polymorphic Behavior (Sealed Class)
    // ========================================================================

    @Test
    fun `sealed class - all variants have packageName property`() {
        val variants: List<VersionChange> = listOf(
            VersionChange.FirstInstall(TEST_PACKAGE, VERSION_1),
            VersionChange.Updated(TEST_PACKAGE, VERSION_1, VERSION_2),
            VersionChange.Downgraded(TEST_PACKAGE, VERSION_2, VERSION_1),
            VersionChange.NoChange(TEST_PACKAGE, VERSION_1),
            VersionChange.AppNotInstalled(TEST_PACKAGE)
        )

        variants.forEach { variant ->
            assertEquals("All variants should have same package name", TEST_PACKAGE, variant.packageName)
        }
    }

    @Test
    fun `sealed class - when expression is exhaustive`() {
        val change: VersionChange = VersionChange.FirstInstall(TEST_PACKAGE, VERSION_1)

        // Compiler enforces exhaustiveness - this should compile without else branch
        val result = when (change) {
            is VersionChange.FirstInstall -> "first"
            is VersionChange.Updated -> "updated"
            is VersionChange.Downgraded -> "downgraded"
            is VersionChange.NoChange -> "nochange"
            is VersionChange.AppNotInstalled -> "notinstalled"
        }

        assertEquals("Should match FirstInstall", "first", result)
    }

    @Test
    fun `sealed class - getCurrentVersion returns correct type for each variant`() {
        val variants = mapOf<VersionChange, AppVersion?>(
            VersionChange.FirstInstall(TEST_PACKAGE, VERSION_1) to VERSION_1,
            VersionChange.Updated(TEST_PACKAGE, VERSION_1, VERSION_2) to VERSION_2,
            VersionChange.Downgraded(TEST_PACKAGE, VERSION_2, VERSION_1) to VERSION_1,
            VersionChange.NoChange(TEST_PACKAGE, VERSION_1) to VERSION_1,
            VersionChange.AppNotInstalled(TEST_PACKAGE) to null
        )

        variants.forEach { (change, expectedVersion) ->
            assertEquals(
                "getCurrentVersion should match expected for ${change::class.simpleName}",
                expectedVersion,
                change.getCurrentVersion()
            )
        }
    }

    @Test
    fun `sealed class - requiresVerification only for Updated and Downgraded`() {
        assertTrue("Updated should require verification",
            VersionChange.Updated(TEST_PACKAGE, VERSION_1, VERSION_2).requiresVerification())
        assertTrue("Downgraded should require verification",
            VersionChange.Downgraded(TEST_PACKAGE, VERSION_2, VERSION_1).requiresVerification())

        assertFalse("FirstInstall should not require verification",
            VersionChange.FirstInstall(TEST_PACKAGE, VERSION_1).requiresVerification())
        assertFalse("NoChange should not require verification",
            VersionChange.NoChange(TEST_PACKAGE, VERSION_1).requiresVerification())
        assertFalse("AppNotInstalled should not require verification",
            VersionChange.AppNotInstalled(TEST_PACKAGE).requiresVerification())
    }

    @Test
    fun `sealed class - requiresCleanup only for AppNotInstalled`() {
        assertTrue("AppNotInstalled should require cleanup",
            VersionChange.AppNotInstalled(TEST_PACKAGE).requiresCleanup())

        assertFalse("FirstInstall should not require cleanup",
            VersionChange.FirstInstall(TEST_PACKAGE, VERSION_1).requiresCleanup())
        assertFalse("Updated should not require cleanup",
            VersionChange.Updated(TEST_PACKAGE, VERSION_1, VERSION_2).requiresCleanup())
        assertFalse("Downgraded should not require cleanup",
            VersionChange.Downgraded(TEST_PACKAGE, VERSION_2, VERSION_1).requiresCleanup())
        assertFalse("NoChange should not require cleanup",
            VersionChange.NoChange(TEST_PACKAGE, VERSION_1).requiresCleanup())
    }

    // ========================================================================
    // Test: Real-World Scenarios
    // ========================================================================

    @Test
    fun `real world - Gmail update from older to newer version`() {
        val previous = AppVersion("8.2024.11.100", 82024100L)
        val current = AppVersion("8.2024.12.123", 82024123L)

        val change = VersionChange.Updated(
            packageName = "com.google.android.gm",
            previous = previous,
            current = current
        )

        assertTrue("Gmail update should require verification", change.requiresVerification())
        assertEquals("Version delta should be 23", 23L, change.getVersionDelta())
    }

    @Test
    fun `real world - User manually installs older APK (downgrade)`() {
        val previous = AppVersion("2.0.0", 200L)
        val current = AppVersion("1.5.0", 150L)

        val change = VersionChange.Downgraded(
            packageName = "com.example.app",
            previous = previous,
            current = current
        )

        assertTrue("Downgrade should require verification", change.requiresVerification())
        assertEquals("Version delta should be -50", -50L, change.getVersionDelta())
    }

    @Test
    fun `real world - App uninstalled - cleanup commands`() {
        val change = VersionChange.AppNotInstalled(
            packageName = "com.uninstalled.app"
        )

        assertTrue("Uninstalled app should require cleanup", change.requiresCleanup())
        assertNull("Uninstalled app should have no current version", change.getCurrentVersion())
    }

    @Test
    fun `real world - First time VoiceOS sees app`() {
        val change = VersionChange.FirstInstall(
            packageName = "com.newapp.test",
            current = AppVersion("1.0.0", 1)
        )

        assertFalse("First install should not require verification", change.requiresVerification())
        assertNotNull("First install should have current version", change.getCurrentVersion())
    }
}
