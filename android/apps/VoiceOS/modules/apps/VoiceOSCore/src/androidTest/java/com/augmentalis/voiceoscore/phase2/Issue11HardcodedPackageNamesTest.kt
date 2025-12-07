/**
 * Issue11HardcodedPackageNamesTest.kt - Tests for configuration-based package detection
 *
 * Phase 2 - High Priority Issue #11: Hardcoded Package Names for Feature Detection
 * File: VoiceOSService.kt:99-103
 *
 * Problem: Device-specific packages hardcoded (RealWear), not portable
 * Solution: Configuration-based package detection system
 *
 * Test Coverage:
 * - Package configuration loading
 * - Dynamic package detection
 * - Multiple device vendors support
 * - Configuration updates at runtime
 * - Fallback behavior when config missing
 *
 * Run with: ./gradlew :modules:apps:VoiceOSCore:connectedDebugAndroidTest
 */
package com.augmentalis.voiceoscore.phase2

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test suite for configuration-based package detection system
 *
 * Tests verify that hardcoded package names are replaced with
 * flexible configuration that supports multiple device vendors.
 */
@RunWith(AndroidJUnit4::class)
class Issue11HardcodedPackageNamesTest {

    private lateinit var context: Context
    private lateinit var packageDetector: DevicePackageDetector

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        packageDetector = DevicePackageDetector(context)
    }

    /**
     * TEST 1: Verify default RealWear packages loaded from config
     */
    @Test
    fun testDefaultRealWearPackagesLoaded() {
        val validPackages = packageDetector.getValidWindowChangePackages()

        // Should contain all RealWear packages from original code
        assertThat(validPackages).contains("com.realwear.deviceinfo")
        assertThat(validPackages).contains("com.realwear.sysinfo")
        assertThat(validPackages).contains("com.android.systemui")
    }

    /**
     * TEST 2: Verify configuration loading from preferences
     */
    @Test
    fun testConfigurationLoadedFromPreferences() {
        // Set custom package in preferences
        val prefs = context.getSharedPreferences("voiceos_device_packages", Context.MODE_PRIVATE)
        prefs.edit()
            .putStringSet("valid_window_change_packages", setOf(
                "com.example.custompackage",
                "com.vendor.appname"
            ))
            .apply()

        // Reload configuration
        packageDetector.reloadConfiguration()

        val validPackages = packageDetector.getValidWindowChangePackages()
        assertThat(validPackages).contains("com.example.custompackage")
        assertThat(validPackages).contains("com.vendor.appname")
    }

    /**
     * TEST 3: Verify package detection for known vendor
     */
    @Test
    fun testPackageDetectionForKnownVendor() {
        val isValid = packageDetector.isValidWindowChangePackage("com.realwear.deviceinfo")
        assertThat(isValid).isTrue()
    }

    /**
     * TEST 4: Verify package detection for unknown package
     */
    @Test
    fun testPackageDetectionForUnknownPackage() {
        val isValid = packageDetector.isValidWindowChangePackage("com.unknown.package")
        assertThat(isValid).isFalse()
    }

    /**
     * TEST 5: Verify multiple vendor support
     */
    @Test
    fun testMultipleVendorSupport() {
        // Add packages for multiple vendors
        packageDetector.addVendorPackages("vuzix", setOf(
            "com.vuzix.sysinfo",
            "com.vuzix.settings"
        ))

        packageDetector.addVendorPackages("google", setOf(
            "com.google.glass.home",
            "com.google.glass.settings"
        ))

        // Verify all vendors' packages are recognized
        assertThat(packageDetector.isValidWindowChangePackage("com.vuzix.sysinfo")).isTrue()
        assertThat(packageDetector.isValidWindowChangePackage("com.google.glass.home")).isTrue()
        assertThat(packageDetector.isValidWindowChangePackage("com.realwear.deviceinfo")).isTrue()
    }

    /**
     * TEST 6: Verify vendor detection by package name
     */
    @Test
    fun testVendorDetectionByPackageName() {
        packageDetector.addVendorPackages("vuzix", setOf("com.vuzix.sysinfo"))

        val vendor = packageDetector.detectVendorFromPackage("com.vuzix.sysinfo")
        assertThat(vendor).isEqualTo("vuzix")
    }

    /**
     * TEST 7: Verify vendor detection returns null for unknown package
     */
    @Test
    fun testVendorDetectionReturnsNullForUnknown() {
        val vendor = packageDetector.detectVendorFromPackage("com.unknown.package")
        assertThat(vendor).isNull()
    }

    /**
     * TEST 8: Verify runtime configuration update
     */
    @Test
    fun testRuntimeConfigurationUpdate() {
        // Initial state - package not recognized
        assertThat(packageDetector.isValidWindowChangePackage("com.newvendor.app")).isFalse()

        // Add package at runtime
        packageDetector.addValidPackage("com.newvendor.app")

        // Now should be recognized
        assertThat(packageDetector.isValidWindowChangePackage("com.newvendor.app")).isTrue()
    }

    /**
     * TEST 9: Verify package removal at runtime
     */
    @Test
    fun testRuntimePackageRemoval() {
        // Add then remove package
        packageDetector.addValidPackage("com.temporary.package")
        assertThat(packageDetector.isValidWindowChangePackage("com.temporary.package")).isTrue()

        packageDetector.removeValidPackage("com.temporary.package")
        assertThat(packageDetector.isValidWindowChangePackage("com.temporary.package")).isFalse()
    }

    /**
     * TEST 10: Verify configuration persistence
     */
    @Test
    fun testConfigurationPersistence() {
        // Add custom packages
        packageDetector.addValidPackage("com.persistent.package1")
        packageDetector.addValidPackage("com.persistent.package2")

        // Save configuration
        packageDetector.saveConfiguration()

        // Create new instance - should load saved config
        val newDetector = DevicePackageDetector(context)
        assertThat(newDetector.isValidWindowChangePackage("com.persistent.package1")).isTrue()
        assertThat(newDetector.isValidWindowChangePackage("com.persistent.package2")).isTrue()
    }

    /**
     * TEST 11: Verify fallback to defaults when config missing
     */
    @Test
    fun testFallbackToDefaultsWhenConfigMissing() {
        // Clear preferences
        val prefs = context.getSharedPreferences("voiceos_device_packages", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()

        // Create new detector - should use defaults
        val detector = DevicePackageDetector(context)
        assertThat(detector.isValidWindowChangePackage("com.realwear.deviceinfo")).isTrue()
        assertThat(detector.isValidWindowChangePackage("com.android.systemui")).isTrue()
    }

    /**
     * TEST 12: Verify configuration reset to defaults
     */
    @Test
    fun testConfigurationResetToDefaults() {
        // Add custom packages
        packageDetector.addValidPackage("com.custom.package")

        // Reset to defaults
        packageDetector.resetToDefaults()

        // Custom package should be gone
        assertThat(packageDetector.isValidWindowChangePackage("com.custom.package")).isFalse()

        // Defaults should be present
        assertThat(packageDetector.isValidWindowChangePackage("com.realwear.deviceinfo")).isTrue()
    }
}

/**
 * DevicePackageDetector - Configuration-based package detection system
 *
 * Replaces hardcoded package names with flexible configuration
 * that supports multiple device vendors and runtime updates.
 */
class DevicePackageDetector(private val context: Context) {

    companion object {
        private const val PREF_NAME = "voiceos_device_packages"
        private const val KEY_VALID_PACKAGES = "valid_window_change_packages"

        // Default packages (RealWear original values)
        private val DEFAULT_PACKAGES = setOf(
            "com.realwear.deviceinfo",
            "com.realwear.sysinfo",
            "com.android.systemui"
        )
    }

    private val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val validPackages = mutableSetOf<String>()
    private val vendorPackages = mutableMapOf<String, MutableSet<String>>()

    init {
        loadConfiguration()
    }

    /**
     * Load configuration from preferences or use defaults
     */
    private fun loadConfiguration() {
        val savedPackages = prefs.getStringSet(KEY_VALID_PACKAGES, null)

        if (savedPackages != null) {
            validPackages.clear()
            validPackages.addAll(savedPackages)
        } else {
            // Use defaults on first run
            validPackages.clear()
            validPackages.addAll(DEFAULT_PACKAGES)
        }
    }

    /**
     * Reload configuration from preferences
     */
    fun reloadConfiguration() {
        loadConfiguration()
    }

    /**
     * Get all valid window change packages
     */
    fun getValidWindowChangePackages(): Set<String> {
        return validPackages.toSet()
    }

    /**
     * Check if package is valid for window content change events
     */
    fun isValidWindowChangePackage(packageName: String): Boolean {
        return validPackages.contains(packageName)
    }

    /**
     * Add vendor-specific packages
     */
    fun addVendorPackages(vendorName: String, packages: Set<String>) {
        vendorPackages.getOrPut(vendorName) { mutableSetOf() }.addAll(packages)
        validPackages.addAll(packages)
    }

    /**
     * Detect vendor from package name
     */
    fun detectVendorFromPackage(packageName: String): String? {
        return vendorPackages.entries
            .firstOrNull { (_, packages) -> packageName in packages }
            ?.key
    }

    /**
     * Add single package at runtime
     */
    fun addValidPackage(packageName: String) {
        validPackages.add(packageName)
    }

    /**
     * Remove package at runtime
     */
    fun removeValidPackage(packageName: String) {
        validPackages.remove(packageName)
    }

    /**
     * Save configuration to preferences
     */
    fun saveConfiguration() {
        prefs.edit()
            .putStringSet(KEY_VALID_PACKAGES, validPackages.toSet())
            .apply()
    }

    /**
     * Reset to default packages
     */
    fun resetToDefaults() {
        validPackages.clear()
        validPackages.addAll(DEFAULT_PACKAGES)
        vendorPackages.clear()
    }
}
