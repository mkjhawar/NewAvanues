package com.augmentalis.webavanue

import com.augmentalis.voiceoscore.Bounds
import com.augmentalis.voiceoscore.ElementInfo
import com.augmentalis.avid.AvidGlobalID
import com.augmentalis.avid.TypeCode
import com.augmentalis.voiceoscore.ElementFingerprint
import com.augmentalis.voiceoscore.ExtractionBundle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Tests for WebAvanue page extraction integration with VoiceOSCore.
 *
 * Tests the browser's ability to extract interactive elements from web pages
 * for voice command targeting using the shared extraction framework.
 *
 * Note: ElementParser tests removed — class was deleted during voiceoscoreng→voiceoscore migration.
 * ElementParser functionality is now handled directly by ExtractionBundle + platform extractors.
 */
class WebAvanuePageExtractorTest {

    // ==========================================================================
    // ExtractionBundle Tests (shared JavaScript for DOM extraction)
    // ==========================================================================

    @Test
    fun extractionBundle_jsScriptIsValid() {
        assertTrue(ExtractionBundle.isScriptValid())
    }

    @Test
    fun extractionBundle_jsScriptContainsRequiredFunctions() {
        val script = ExtractionBundle.ELEMENT_EXTRACTOR_JS
        assertTrue(script.contains("extractElements"), "Should contain extractElements function")
        assertTrue(script.contains("JSON.stringify"), "Should return JSON")
        assertTrue(script.contains("INTERACTIVE_SELECTORS"), "Should have interactive selectors")
    }

    @Test
    fun extractionBundle_jsScriptHasReasonableSize() {
        val size = ExtractionBundle.getScriptSize()
        assertTrue(size > 0, "Script should have content")
        assertTrue(size < 10_000, "Script should be under 10KB for efficient injection")
    }

    // ==========================================================================
    // AVID Generation Tests (compact voice IDs for web elements)
    // ==========================================================================

    @Test
    fun avidGenerator_generatesValidAvidForWebElement() {
        val avid = AvidGlobalID.generate()

        assertTrue(avid.isNotBlank())
        assertTrue(AvidGlobalID.isValid(avid), "Generated AVID should be valid")
    }

    @Test
    fun avidGenerator_parsesGeneratedAvid() {
        val avid = AvidGlobalID.generate()
        val parsed = AvidGlobalID.parse(avid)

        assertNotNull(parsed, "Should parse generated AVID")
    }

    @Test
    fun elementFingerprint_generatesConsistentHashForSameContent() {
        val hash1 = ElementFingerprint.deterministicHash("example.com", 8)
        val hash2 = ElementFingerprint.deterministicHash("example.com", 8)

        assertEquals(hash1, hash2, "Same content should produce same hash")
    }

    @Test
    fun elementFingerprint_generatesDifferentHashesForDifferentDomains() {
        val hash1 = ElementFingerprint.deterministicHash("example.com", 8)
        val hash2 = ElementFingerprint.deterministicHash("google.com", 8)

        assertTrue(hash1 != hash2, "Different domains should produce different hashes")
    }

    @Test
    fun typeCode_detectsElementTypes() {
        assertEquals(TypeCode.BUTTON, TypeCode.fromTypeName("button"))
        assertEquals(TypeCode.INPUT, TypeCode.fromTypeName("input"))
        assertEquals(TypeCode.TEXT, TypeCode.fromTypeName("span"))
        assertEquals(TypeCode.ELEMENT, TypeCode.fromTypeName("div"))
    }

    // ==========================================================================
    // ElementInfo Voice Label Tests
    // ==========================================================================

    @Test
    fun elementInfo_voiceLabelPrioritizesText() {
        val element = ElementInfo(
            className = "button",
            text = "Click Me",
            contentDescription = "A button",
            resourceId = "btn-submit"
        )

        assertEquals("Click Me", element.voiceLabel)
    }

    @Test
    fun elementInfo_voiceLabelFallsBackToContentDescription() {
        val element = ElementInfo(
            className = "input",
            text = "",
            contentDescription = "Enter your email",
            resourceId = "email-input"
        )

        assertEquals("Enter your email", element.voiceLabel)
    }

    @Test
    fun elementInfo_voiceLabelFallsBackToResourceId() {
        val element = ElementInfo(
            className = "div",
            text = "",
            contentDescription = "",
            resourceId = "user_profile_button"
        )

        assertEquals("user profile button", element.voiceLabel)
    }

    @Test
    fun elementInfo_hasVoiceContent() {
        val withText = ElementInfo(className = "button", text = "Submit")
        val withDesc = ElementInfo(className = "input", contentDescription = "Email")
        val withId = ElementInfo(className = "div", resourceId = "submit-btn")
        val empty = ElementInfo(className = "span")

        assertTrue(withText.hasVoiceContent)
        assertTrue(withDesc.hasVoiceContent)
        assertTrue(withId.hasVoiceContent)
        assertFalse(empty.hasVoiceContent)
    }

    // ==========================================================================
    // Bounds Parsing Tests
    // ==========================================================================

    @Test
    fun bounds_parsesFromString() {
        val bounds = Bounds.fromString("10,20,100,50")

        assertNotNull(bounds)
        assertEquals(10, bounds.left)
        assertEquals(20, bounds.top)
        assertEquals(100, bounds.right)
        assertEquals(50, bounds.bottom)
    }

    @Test
    fun bounds_calculatesWidthAndHeight() {
        val bounds = Bounds(10, 20, 110, 70)

        assertEquals(100, bounds.width)
        assertEquals(50, bounds.height)
    }

    @Test
    fun bounds_calculatesCenter() {
        val bounds = Bounds(0, 0, 100, 100)

        assertEquals(50, bounds.centerX)
        assertEquals(50, bounds.centerY)
    }

    @Test
    fun bounds_handlesInvalidString() {
        val bounds = Bounds.fromString("invalid")
        assertEquals(null, bounds)
    }
}
