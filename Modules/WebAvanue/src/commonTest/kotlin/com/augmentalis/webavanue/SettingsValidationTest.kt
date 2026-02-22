package com.augmentalis.webavanue

import com.augmentalis.webavanue.BrowserSettings
import kotlin.test.*

/**
 * SettingsValidationTest - Comprehensive unit tests for SettingsValidation
 *
 * Tests cover:
 * - Zoom clamping and validation
 * - Scale clamping and validation
 * - WebXR dependency validation
 * - Auto-correction of invalid values
 * - Warning generation for suspicious values
 * - Error generation for invalid values
 * - Constraint checking
 *
 * Coverage Target: 90%
 */
class SettingsValidationTest {

    // ========== Test 1: Zoom clamping - value too high ==========
    @Test
    fun `validate clamps zoom above maximum to 200`() {
        // Given - Zoom of 300 exceeds maximum of 200
        val settings = BrowserSettings(desktopModeDefaultZoom = 300)

        // When
        val result = SettingsValidation.validate(settings)

        // Then
        assertTrue(result.isValid)
        assertEquals(200, result.correctedSettings.desktopModeDefaultZoom)
        assertTrue(result.warnings.any { it.contains("300") && it.contains("200") })
    }

    // ========== Test 2: Scale clamping - value too high ==========
    @Test
    fun `validate clamps mobilePortraitScale above maximum to 2_0`() {
        // Given - Initial scale of 3.0 exceeds maximum of 2.0
        val settings = BrowserSettings(mobilePortraitScale = 3.0f)

        // When
        val result = SettingsValidation.validate(settings)

        // Then
        assertTrue(result.isValid)
        assertEquals(2.0f, result.correctedSettings.mobilePortraitScale)
        assertTrue(result.warnings.any { it.contains("3.0") && it.contains("2.0") })
    }

    // ========== Test 3: Negative zoom rejected ==========
    @Test
    fun `validate clamps negative zoom to minimum 50`() {
        // Given - Negative zoom is invalid
        val settings = BrowserSettings(desktopModeDefaultZoom = -10)

        // When
        val result = SettingsValidation.validate(settings)

        // Then
        assertTrue(result.isValid)
        assertEquals(50, result.correctedSettings.desktopModeDefaultZoom)
        assertTrue(result.warnings.any { it.contains("-10") && it.contains("50") })
    }

    // ========== Test 4: Zero scale rejected ==========
    @Test
    fun `validate clamps zero scale to minimum 0_5`() {
        // Given - Zero scale is invalid
        val settings = BrowserSettings(mobilePortraitScale = 0.0f)

        // When
        val result = SettingsValidation.validate(settings)

        // Then
        assertTrue(result.isValid)
        assertEquals(0.5f, result.correctedSettings.mobilePortraitScale)
        assertTrue(result.warnings.any { it.contains("0.0") && it.contains("0.5") })
    }

    // ========== Test 5: Valid zoom passes unchanged ==========
    @Test
    fun `validate passes valid zoom unchanged`() {
        // Given - Zoom of 125 is valid (within 50-200)
        val settings = BrowserSettings(desktopModeDefaultZoom = 125)

        // When
        val result = SettingsValidation.validate(settings)

        // Then
        assertTrue(result.isValid)
        assertEquals(125, result.correctedSettings.desktopModeDefaultZoom)
        assertTrue(result.warnings.isEmpty())
        assertTrue(result.errors.isEmpty())
    }

    // ========== Test 6: WebXR dependencies validated - JavaScript required ==========
    @Test
    fun `validate errors when WebXR enabled without JavaScript`() {
        // Given - WebXR requires JavaScript
        val settings = BrowserSettings(
            enableWebXR = true,
            enableJavaScript = false
        )

        // When
        val result = SettingsValidation.validate(settings)

        // Then
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("WebXR requires JavaScript") })
        // Should auto-enable JavaScript
        assertTrue(result.correctedSettings.enableJavaScript)
    }

    // ========== Test 7: WebXR dependencies - AR requires WebXR ==========
    @Test
    fun `validate warns and auto-enables WebXR when AR enabled`() {
        // Given - AR enabled but WebXR disabled
        val settings = BrowserSettings(
            enableAR = true,
            enableWebXR = false
        )

        // When
        val result = SettingsValidation.validate(settings)

        // Then
        assertTrue(result.isValid)
        assertTrue(result.warnings.any { it.contains("AR") && it.contains("WebXR") })
        // Should auto-enable WebXR
        assertTrue(result.correctedSettings.enableWebXR)
    }

    // ========== Test 8: WebXR dependencies - VR requires WebXR ==========
    @Test
    fun `validate warns and auto-enables WebXR when VR enabled`() {
        // Given - VR enabled but WebXR disabled
        val settings = BrowserSettings(
            enableVR = true,
            enableWebXR = false
        )

        // When
        val result = SettingsValidation.validate(settings)

        // Then
        assertTrue(result.isValid)
        assertTrue(result.warnings.any { it.contains("VR") && it.contains("WebXR") })
        // Should auto-enable WebXR
        assertTrue(result.correctedSettings.enableWebXR)
    }

    // ========== Test 9: Performance warning - hardware acceleration + data saver ==========
    @Test
    fun `validate warns about hardware acceleration with data saver conflict`() {
        // Given - Both hardware acceleration and data saver enabled
        val settings = BrowserSettings(
            hardwareAcceleration = true,
            dataSaver = true
        )

        // When
        val result = SettingsValidation.validate(settings)

        // Then
        assertTrue(result.isValid)
        assertTrue(result.warnings.any {
            it.contains("Hardware acceleration") && it.contains("data saver")
        })
    }

    // ========== Test 10: All constraints documented and enforced ==========
    @Test
    fun `Constraints object has correct values and validation methods work`() {
        // Verify zoom constraints
        assertEquals(50, SettingsValidation.Constraints.MIN_ZOOM)
        assertEquals(200, SettingsValidation.Constraints.MAX_ZOOM)
        assertEquals(100, SettingsValidation.Constraints.DEFAULT_ZOOM)

        // Verify scale constraints
        assertEquals(0.5f, SettingsValidation.Constraints.MIN_INITIAL_SCALE)
        assertEquals(2.0f, SettingsValidation.Constraints.MAX_INITIAL_SCALE)
        assertEquals(0.75f, SettingsValidation.Constraints.DEFAULT_INITIAL_SCALE)

        // Verify validation methods
        assertTrue(SettingsValidation.Constraints.isValidZoom(100))
        assertFalse(SettingsValidation.Constraints.isValidZoom(300))
        assertFalse(SettingsValidation.Constraints.isValidZoom(-10))

        assertTrue(SettingsValidation.Constraints.isValidInitialScale(1.0f))
        assertFalse(SettingsValidation.Constraints.isValidInitialScale(3.0f))
        assertFalse(SettingsValidation.Constraints.isValidInitialScale(0.0f))
    }

    // ========== Bonus Test 11: Error message generation ==========
    @Test
    fun `getErrorMessage returns correct messages for invalid settings`() {
        // Test invalid zoom
        val zoomError = SettingsValidation.getErrorMessage("desktopModeDefaultZoom", 300)
        assertNotNull(zoomError)
        assertTrue(zoomError.contains("50") && zoomError.contains("200"))

        // Test invalid scale
        val scaleError = SettingsValidation.getErrorMessage("mobilePortraitScale", 3.0f)
        assertNotNull(scaleError)
        assertTrue(scaleError.contains("0.5") && scaleError.contains("2.0"))

        // Test valid zoom returns null
        val validZoom = SettingsValidation.getErrorMessage("desktopModeDefaultZoom", 125)
        assertNull(validZoom)
    }

    // ========== Bonus Test 12: Security warning - trackers without popup blocking ==========
    @Test
    fun `validate warns about tracker blocking without popup blocking`() {
        // Given - Tracker blocking enabled but popups allowed
        val settings = BrowserSettings(
            blockTrackers = true,
            blockPopups = false
        )

        // When
        val result = SettingsValidation.validate(settings)

        // Then
        assertTrue(result.isValid)
        assertTrue(result.warnings.any {
            it.contains("Tracker blocking") && it.contains("popups")
        })
    }

    // ========== Bonus Test 13: Privacy warning - cookies without tracker blocking ==========
    @Test
    fun `validate warns about cookies enabled without tracker blocking`() {
        // Given - Cookies enabled without tracker blocking
        val settings = BrowserSettings(
            enableCookies = true,
            blockTrackers = false
        )

        // When
        val result = SettingsValidation.validate(settings)

        // Then
        assertTrue(result.isValid)
        assertTrue(result.warnings.any {
            it.contains("Cookies") && it.contains("tracker blocking")
        })
    }

    // ========== Bonus Test 14: Performance warning - WebXR with data saver ==========
    @Test
    fun `validate warns about WebXR with data saver performance impact`() {
        // Given - WebXR enabled with data saver
        val settings = BrowserSettings(
            enableWebXR = true,
            dataSaver = true
        )

        // When
        val result = SettingsValidation.validate(settings)

        // Then
        assertTrue(result.isValid)
        assertTrue(result.warnings.any {
            it.contains("WebXR") && it.contains("data saver")
        })
    }

    // ========== Bonus Test 15: Complex settings validation ==========
    @Test
    fun `validate handles complex settings with multiple corrections`() {
        // Given - Multiple invalid settings
        val settings = BrowserSettings(
            desktopModeDefaultZoom = 500,  // Too high
            mobilePortraitScale = -1.0f,          // Too low
            enableWebXR = true,
            enableJavaScript = false,      // Conflict with WebXR
            enableAR = true,
            enableVR = true,
            hardwareAcceleration = true,
            dataSaver = true               // Performance warning
        )

        // When
        val result = SettingsValidation.validate(settings)

        // Then
        assertFalse(result.isValid) // Has errors (WebXR requires JavaScript)
        assertEquals(200, result.correctedSettings.desktopModeDefaultZoom) // Clamped
        assertEquals(0.5f, result.correctedSettings.mobilePortraitScale)         // Clamped
        assertTrue(result.correctedSettings.enableWebXR)                  // Auto-enabled
        assertTrue(result.correctedSettings.enableJavaScript)             // Auto-enabled
        assertTrue(result.warnings.size >= 3)  // Multiple warnings
        assertTrue(result.errors.size >= 1)    // At least one error
    }
}
