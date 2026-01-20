package com.augmentalis.webavanue

import com.augmentalis.voiceoscoreng.common.Bounds
import com.augmentalis.voiceoscoreng.common.ElementInfo
import com.augmentalis.avid.AvidGenerator
import com.augmentalis.avid.TypeCode
import com.augmentalis.voiceoscoreng.common.ElementFingerprint
import com.augmentalis.voiceoscoreng.extraction.ElementParser
import com.augmentalis.voiceoscoreng.extraction.ExtractionBundle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * TDD tests for WebAvanue page extraction integration with VoiceOSCoreNG.
 *
 * Tests the browser's ability to extract interactive elements from web pages
 * for voice command targeting using the shared extraction framework.
 */
class WebAvanuePageExtractorTest {

    // ==========================================================================
    // ExtractionBundle Tests (shared JavaScript for DOM extraction)
    // ==========================================================================

    @Test
    fun extractionBundle_jsScriptIsValid() {
        // The extraction bundle should provide valid JavaScript
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
        // Script should be under 10KB for efficient injection
        val size = ExtractionBundle.getScriptSize()
        assertTrue(size > 0, "Script should have content")
        assertTrue(size < 10_000, "Script should be under 10KB for efficient injection")
    }

    // ==========================================================================
    // ElementParser JSON Parsing Tests (accessibility tree JSON)
    // ==========================================================================

    @Test
    fun elementParser_parsesWebExtractionJson() {
        // JSON format returned by ELEMENT_EXTRACTOR_JS
        val json = """
        {
            "elements": [
                {
                    "className": "button",
                    "resourceId": "submit-btn",
                    "text": "Submit",
                    "contentDescription": "",
                    "bounds": "10,20,100,50",
                    "clickable": true,
                    "scrollable": false,
                    "enabled": true,
                    "packageName": "example.com"
                }
            ],
            "metadata": {
                "url": "https://example.com",
                "title": "Test Page",
                "timestamp": 1234567890,
                "elementCount": 1
            }
        }
        """.trimIndent()

        val elements = ElementParser.parseAccessibilityJson(json)
        assertEquals(1, elements.size)

        val button = elements.first()
        assertEquals("button", button.className)
        assertEquals("submit-btn", button.resourceId)
        assertEquals("Submit", button.text)
        assertTrue(button.isClickable)
        assertFalse(button.isScrollable)
        assertTrue(button.isEnabled)
        assertEquals("example.com", button.packageName)
    }

    @Test
    fun elementParser_parsesMultipleElements() {
        val json = """
        {
            "elements": [
                {
                    "className": "button",
                    "resourceId": "login-btn",
                    "text": "Login",
                    "contentDescription": "",
                    "bounds": "10,20,100,50",
                    "clickable": true,
                    "scrollable": false,
                    "enabled": true,
                    "packageName": "example.com"
                },
                {
                    "className": "input",
                    "resourceId": "username",
                    "text": "",
                    "contentDescription": "Enter username",
                    "bounds": "10,60,200,90",
                    "clickable": true,
                    "scrollable": false,
                    "enabled": true,
                    "packageName": "example.com"
                },
                {
                    "className": "a",
                    "resourceId": "",
                    "text": "Forgot password?",
                    "contentDescription": "",
                    "bounds": "10,100,150,120",
                    "clickable": true,
                    "scrollable": false,
                    "enabled": true,
                    "packageName": "example.com"
                }
            ]
        }
        """.trimIndent()

        val elements = ElementParser.parseAccessibilityJson(json)
        assertEquals(3, elements.size)

        // Verify different element types
        assertEquals("button", elements[0].className)
        assertEquals("input", elements[1].className)
        assertEquals("a", elements[2].className)
    }

    @Test
    fun elementParser_handlesEmptyJson() {
        val emptyJson = "{}"
        val elements = ElementParser.parseAccessibilityJson(emptyJson)
        assertTrue(elements.isEmpty())
    }

    @Test
    fun elementParser_handlesInvalidJson() {
        val invalidJson = "not valid json"
        val elements = ElementParser.parseAccessibilityJson(invalidJson)
        assertTrue(elements.isEmpty())
    }

    @Test
    fun elementParser_handlesEmptyElementsArray() {
        val json = """{"elements": []}"""
        val elements = ElementParser.parseAccessibilityJson(json)
        assertTrue(elements.isEmpty())
    }

    // ==========================================================================
    // ElementParser HTML Parsing Tests (fallback for raw HTML)
    // ==========================================================================

    @Test
    fun elementParser_parsesButtonsFromHtml() {
        val html = """
        <html>
            <body>
                <button id="submit" aria-label="Submit form">Submit</button>
                <button id="cancel">Cancel</button>
            </body>
        </html>
        """.trimIndent()

        val elements = ElementParser.parseHtml(html)
        assertTrue(elements.size >= 2, "Should find at least 2 buttons")

        val submitButton = elements.find { it.resourceId == "submit" }
        assertNotNull(submitButton)
        assertEquals("Submit", submitButton.text)
        assertEquals("Submit form", submitButton.contentDescription)
    }

    @Test
    fun elementParser_parsesLinksFromHtml() {
        val html = """
        <html>
            <body>
                <a href="/home" id="home-link">Home</a>
                <a href="/about" aria-label="About us">About</a>
            </body>
        </html>
        """.trimIndent()

        val elements = ElementParser.parseHtml(html)
        assertTrue(elements.isNotEmpty(), "Should find links")

        val homeLink = elements.find { it.resourceId == "home-link" }
        assertNotNull(homeLink, "Should find home link")
        assertEquals("Home", homeLink.text)
    }

    @Test
    fun elementParser_parsesInputsFromHtml() {
        val html = """
        <html>
            <body>
                <input type="text" id="email" placeholder="Enter email" aria-label="Email address"/>
                <input type="password" name="password" placeholder="Password"/>
            </body>
        </html>
        """.trimIndent()

        val elements = ElementParser.parseHtml(html)
        assertTrue(elements.isNotEmpty(), "Should find inputs")

        val emailInput = elements.find { it.resourceId == "email" }
        assertNotNull(emailInput, "Should find email input")
        assertEquals("Email address", emailInput.contentDescription)
    }

    // ==========================================================================
    // AVID Generation Tests (compact voice IDs for web elements)
    // ==========================================================================

    @Test
    fun avidGenerator_generatesValidAvidForWebElement() {
        // Test basic AVID generation
        val avid = AvidGenerator.generate()

        assertTrue(avid.isNotBlank())
        assertTrue(AvidGenerator.isValid(avid), "Generated AVID should be valid")
    }

    @Test
    fun avidGenerator_parsesGeneratedAvid() {
        val avid = AvidGenerator.generate()
        val parsed = AvidGenerator.parse(avid)

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
    // Element Filtering Tests
    // ==========================================================================

    @Test
    fun elementParser_filtersToActionableElements() {
        val elements = listOf(
            ElementInfo(className = "button", text = "Click", isClickable = true),
            ElementInfo(className = "div", text = "Container", isClickable = false),
            ElementInfo(className = "scroll", text = "", isScrollable = true),
            ElementInfo(className = "span", text = "Label", isClickable = false)
        )

        val actionable = ElementParser.filterActionable(elements)

        assertEquals(2, actionable.size)
        assertTrue(actionable.all { it.isClickable || it.isScrollable })
    }

    @Test
    fun elementParser_filtersToElementsWithContent() {
        val elements = listOf(
            ElementInfo(className = "button", text = "Submit"),
            ElementInfo(className = "div", contentDescription = "Container"),
            ElementInfo(className = "span", resourceId = "label-1"),
            ElementInfo(className = "br")  // No content
        )

        val withContent = ElementParser.filterWithContent(elements)

        assertEquals(3, withContent.size)
        assertTrue(withContent.all { it.hasVoiceContent })
    }

    @Test
    fun elementParser_deduplicatesElements() {
        val elements = listOf(
            ElementInfo(className = "button", resourceId = "submit-btn", text = "Submit"),
            ElementInfo(className = "button", resourceId = "submit-btn", text = "Submit"),  // Duplicate
            ElementInfo(className = "button", resourceId = "cancel-btn", text = "Cancel")
        )

        val deduped = ElementParser.deduplicate(elements)

        assertEquals(2, deduped.size)
    }

    // ==========================================================================
    // XPath Generation Tests
    // ==========================================================================

    @Test
    fun elementParser_generatesXPathById() {
        val element = ElementInfo(className = "button", resourceId = "submit-btn")
        val xpath = ElementParser.generateXPath(element)

        assertEquals("//button[@id='submit-btn']", xpath)
    }

    @Test
    fun elementParser_generatesXPathByText() {
        val element = ElementInfo(className = "a", text = "Click here")
        val xpath = ElementParser.generateXPath(element)

        assertEquals("//a[contains(text(),'Click here')]", xpath)
    }

    @Test
    fun elementParser_generatesXPathByAriaLabel() {
        val element = ElementInfo(className = "div", contentDescription = "Navigation menu")
        val xpath = ElementParser.generateXPath(element)

        assertEquals("//div[@aria-label='Navigation menu']", xpath)
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
