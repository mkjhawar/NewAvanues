// filename: Universal/AVA/Features/RAG/src/androidTest/kotlin/com/augmentalis/ava/features/rag/embeddings/AONPackageManagerTest.kt
// created: 2025-11-24
// author: AVA AI Team
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.rag.embeddings

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests for AONPackageManager
 *
 * Tests:
 * - Package list presets
 * - Distribution strategy selection
 * - Package list validation
 * - Ecosystem package detection
 */
@RunWith(AndroidJUnit4::class)
class AONPackageManagerTest {

    // ==================== Preset Tests ====================

    @Test
    fun testAVAStandardApps_containsThreePackages() {
        val packages = AONPackageManager.AVA_STANDARD_APPS

        assertEquals("Should have exactly 3 packages", 3, packages.size)
        assertTrue(packages.contains("com.augmentalis.ava"))
        assertTrue(packages.contains("com.augmentalis.avaconnect"))
        assertTrue(packages.contains("com.augmentalis.voiceos"))
    }

    @Test
    fun testAvanuesPlatformApps_containsThreePackages() {
        val packages = AONPackageManager.AVANUES_PLATFORM_APPS

        assertEquals("Should have exactly 3 packages", 3, packages.size)
        assertTrue(packages.contains("com.augmentalis.avanues"))
        assertTrue(packages.contains("com.augmentalis.ava"))
        assertTrue(packages.contains("com.augmentalis.avaconnect"))
    }

    @Test
    fun testDevelopmentApps_containsThreePackages() {
        val packages = AONPackageManager.DEVELOPMENT_APPS

        assertEquals("Should have exactly 3 packages", 3, packages.size)
        assertTrue(packages.contains("com.augmentalis.ava.debug"))
        assertTrue(packages.contains("com.augmentalis.ava.staging"))
        assertTrue(packages.contains("com.augmentalis.ava.test"))
    }

    @Test
    fun testAllAVAApps_containsThreePackages() {
        val packages = AONPackageManager.ALL_AVA_APPS

        assertEquals("Should have exactly 3 packages", 3, packages.size)
        assertTrue(packages.contains("com.augmentalis.ava"))
        assertTrue(packages.contains("com.augmentalis.avaconnect"))
        assertTrue(packages.contains("com.augmentalis.voiceos"))
    }

    // ==================== Strategy Selection Tests ====================

    @Test
    fun testGetPackagesForStrategy_AVAStandard() {
        val packages = AONPackageManager.getPackagesForStrategy(
            AONPackageManager.DistributionStrategy.AVA_STANDARD
        )

        assertEquals("Should return AVA_STANDARD_APPS",
            AONPackageManager.AVA_STANDARD_APPS,
            packages)
    }

    @Test
    fun testGetPackagesForStrategy_AvanuesPlatform() {
        val packages = AONPackageManager.getPackagesForStrategy(
            AONPackageManager.DistributionStrategy.AVANUES_PLATFORM
        )

        assertEquals("Should return AVANUES_PLATFORM_APPS",
            AONPackageManager.AVANUES_PLATFORM_APPS,
            packages)
    }

    @Test
    fun testGetPackagesForStrategy_Development() {
        val packages = AONPackageManager.getPackagesForStrategy(
            AONPackageManager.DistributionStrategy.DEVELOPMENT
        )

        assertEquals("Should return DEVELOPMENT_APPS",
            AONPackageManager.DEVELOPMENT_APPS,
            packages)
    }

    @Test
    fun testGetPackagesForStrategy_AllAVA() {
        val packages = AONPackageManager.getPackagesForStrategy(
            AONPackageManager.DistributionStrategy.ALL_AVA
        )

        assertEquals("Should return ALL_AVA_APPS",
            AONPackageManager.ALL_AVA_APPS,
            packages)
    }

    // ==================== Validation Tests ====================

    @Test
    fun testValidatePackageList_acceptsOnePackage() {
        val packages = listOf("com.augmentalis.ava")

        // Should not throw
        AONPackageManager.validatePackageList(packages)
    }

    @Test
    fun testValidatePackageList_acceptsTwoPackages() {
        val packages = listOf(
            "com.augmentalis.ava",
            "com.augmentalis.avaconnect"
        )

        // Should not throw
        AONPackageManager.validatePackageList(packages)
    }

    @Test
    fun testValidatePackageList_acceptsThreePackages() {
        val packages = listOf(
            "com.augmentalis.ava",
            "com.augmentalis.avaconnect",
            "com.augmentalis.voiceos"
        )

        // Should not throw
        AONPackageManager.validatePackageList(packages)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testValidatePackageList_rejectsFourPackages() {
        val packages = listOf(
            "com.augmentalis.ava",
            "com.augmentalis.avaconnect",
            "com.augmentalis.voiceos",
            "com.augmentalis.extra"
        )

        AONPackageManager.validatePackageList(packages)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testValidatePackageList_rejectsEmptyList() {
        AONPackageManager.validatePackageList(emptyList())
    }

    // ==================== Ecosystem Detection Tests ====================

    @Test
    fun testIsAVAEcosystemPackage_recognizesAVA() {
        assertTrue(AONPackageManager.isAVAEcosystemPackage("com.augmentalis.ava"))
        assertTrue(AONPackageManager.isAVAEcosystemPackage("com.augmentalis.ava.debug"))
        assertTrue(AONPackageManager.isAVAEcosystemPackage("com.augmentalis.avaconnect"))
    }

    @Test
    fun testIsAVAEcosystemPackage_recognizesVoiceOS() {
        assertTrue(AONPackageManager.isAVAEcosystemPackage("com.augmentalis.voiceos"))
        assertTrue(AONPackageManager.isAVAEcosystemPackage("com.augmentalis.voiceos.debug"))
    }

    @Test
    fun testIsAVAEcosystemPackage_recognizesAvanues() {
        assertTrue(AONPackageManager.isAVAEcosystemPackage("com.augmentalis.avanues"))
    }

    @Test
    fun testIsAVAEcosystemPackage_rejectsNonAVAPackages() {
        assertFalse(AONPackageManager.isAVAEcosystemPackage("com.other.app"))
        assertFalse(AONPackageManager.isAVAEcosystemPackage("com.example.test"))
        assertFalse(AONPackageManager.isAVAEcosystemPackage("org.augmentalis.ava"))  // Wrong prefix
    }

    // ==================== Preset Uniqueness Tests ====================

    @Test
    fun testPresets_haveNoOverlappingPackages() {
        val standard = AONPackageManager.AVA_STANDARD_APPS.toSet()
        val avanues = AONPackageManager.AVANUES_PLATFORM_APPS.toSet()
        val development = AONPackageManager.DEVELOPMENT_APPS.toSet()

        // Development should not overlap with production presets
        assertTrue("Development should not overlap with standard",
            (development intersect standard).isEmpty())
        assertTrue("Development should not overlap with avanues",
            (development intersect avanues).isEmpty())

        // Standard and Avanues can have some overlap (by design)
        val overlap = standard intersect avanues
        assertEquals("Standard and Avanues should share AVA and AVAConnect",
            setOf("com.augmentalis.ava", "com.augmentalis.avaconnect"),
            overlap)
    }

    @Test
    fun testPresets_haveNoDuplicates() {
        listOf(
            AONPackageManager.AVA_STANDARD_APPS,
            AONPackageManager.AVANUES_PLATFORM_APPS,
            AONPackageManager.DEVELOPMENT_APPS,
            AONPackageManager.ALL_AVA_APPS
        ).forEach { preset ->
            assertEquals(
                "Preset should have no duplicates",
                preset.toSet().size,
                preset.size
            )
        }
    }

    // ==================== Package Name Format Tests ====================

    @Test
    fun testPresets_followPackageNamingConvention() {
        val allPackages = listOf(
            AONPackageManager.AVA_STANDARD_APPS,
            AONPackageManager.AVANUES_PLATFORM_APPS,
            AONPackageManager.DEVELOPMENT_APPS,
            AONPackageManager.ALL_AVA_APPS
        ).flatten().distinct()

        allPackages.forEach { pkg ->
            assertTrue("Package should start with com.augmentalis",
                pkg.startsWith("com.augmentalis"))
            assertFalse("Package should not have trailing dot",
                pkg.endsWith("."))
            assertFalse("Package should not have double dots",
                pkg.contains(".."))
        }
    }
}
