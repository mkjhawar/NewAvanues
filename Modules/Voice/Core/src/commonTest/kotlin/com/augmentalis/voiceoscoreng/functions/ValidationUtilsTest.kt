package com.augmentalis.voiceoscoreng.functions

import com.augmentalis.voiceoscoreng.common.Bounds
import com.augmentalis.voiceoscoreng.common.ElementInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ValidationUtilsTest {

    // ==================== VUID Validation Tests ====================

    @Test
    fun `isValidVUID returns true for compact format`() {
        assertTrue(ValidationUtils.isValidVUID("a3f2e1-b917cc9dc"))
        assertTrue(ValidationUtils.isValidVUID("000000-e00000000"))
        assertTrue(ValidationUtils.isValidVUID("abcdef-i12345678"))
    }

    @Test
    fun `isValidVUID returns false for invalid compact format`() {
        assertFalse(ValidationUtils.isValidVUID("a3f2e1-917cc9dc")) // missing type code
        assertFalse(ValidationUtils.isValidVUID("a3f2e-b917cc9dc")) // short package hash
        assertFalse(ValidationUtils.isValidVUID("a3f2e1-b917cc9d")) // short element hash
        assertFalse(ValidationUtils.isValidVUID("A3F2E1-B917CC9DC")) // uppercase
    }

    @Test
    fun `isValidVUID returns false for empty string`() {
        assertFalse(ValidationUtils.isValidVUID(""))
    }

    @Test
    fun `isValidVUID returns false for too long string`() {
        val tooLong = "a".repeat(ValidationUtils.MAX_VUID_LENGTH + 1)
        assertFalse(ValidationUtils.isValidVUID(tooLong))
    }

    @Test
    fun `isCompactVUID returns true for valid compact VUIDs`() {
        assertTrue(ValidationUtils.isCompactVUID("a3f2e1-b917cc9dc"))
        assertTrue(ValidationUtils.isCompactVUID("123456-e87654321"))
    }

    @Test
    fun `isCompactVUID returns false for legacy format`() {
        assertFalse(ValidationUtils.isCompactVUID("com.app.v1.0.button-123456789012"))
    }

    // ==================== Package Name Validation Tests ====================

    @Test
    fun `isValidPackageName returns true for valid names`() {
        assertTrue(ValidationUtils.isValidPackageName("com.example.app"))
        assertTrue(ValidationUtils.isValidPackageName("com.instagram.android"))
        assertTrue(ValidationUtils.isValidPackageName("org.mozilla.firefox"))
        assertTrue(ValidationUtils.isValidPackageName("com.example123.app_name"))
        assertTrue(ValidationUtils.isValidPackageName("com._private.app"))
    }

    @Test
    fun `isValidPackageName returns false for invalid names`() {
        assertFalse(ValidationUtils.isValidPackageName("")) // empty
        assertFalse(ValidationUtils.isValidPackageName("nopackage")) // no dot
        assertFalse(ValidationUtils.isValidPackageName("com..app")) // empty segment
        assertFalse(ValidationUtils.isValidPackageName("com.123.app")) // starts with number
        assertFalse(ValidationUtils.isValidPackageName("com.app-name.test")) // hyphen
        assertFalse(ValidationUtils.isValidPackageName(".com.app")) // starts with dot
    }

    @Test
    fun `isValidPackageName returns false for too long names`() {
        val longPackage = (1..50).joinToString(".") { "segment$it" }
        assertFalse(ValidationUtils.isValidPackageName(longPackage))
    }

    // ==================== Resource ID Validation Tests ====================

    @Test
    fun `isValidResourceId returns true for valid IDs`() {
        assertTrue(ValidationUtils.isValidResourceId("com.example:id/button"))
        assertTrue(ValidationUtils.isValidResourceId("com.app:id/my_button_123"))
        assertTrue(ValidationUtils.isValidResourceId("android:id/text1"))
    }

    @Test
    fun `isValidResourceId returns true for empty string`() {
        assertTrue(ValidationUtils.isValidResourceId(""))
    }

    @Test
    fun `isValidResourceId returns false for invalid IDs`() {
        assertFalse(ValidationUtils.isValidResourceId("no_colon"))
        assertFalse(ValidationUtils.isValidResourceId("com.app:id/"))
        assertFalse(ValidationUtils.isValidResourceId("com.app:id/123start"))
        assertFalse(ValidationUtils.isValidResourceId("COM.APP:ID/UPPER"))
    }

    // ==================== Bounds Validation Tests ====================

    @Test
    fun `isValidBounds returns true for valid bounds`() {
        assertTrue(ValidationUtils.isValidBounds(Bounds(0, 0, 100, 100)))
        assertTrue(ValidationUtils.isValidBounds(Bounds(10, 20, 110, 120)))
        assertTrue(ValidationUtils.isValidBounds(Bounds(0, 0, 1, 1)))
    }

    @Test
    fun `isValidBounds returns false for invalid bounds`() {
        assertFalse(ValidationUtils.isValidBounds(Bounds(-1, 0, 100, 100))) // negative left
        assertFalse(ValidationUtils.isValidBounds(Bounds(0, -1, 100, 100))) // negative top
        assertFalse(ValidationUtils.isValidBounds(Bounds(100, 0, 50, 100))) // right < left
        assertFalse(ValidationUtils.isValidBounds(Bounds(0, 100, 100, 50))) // bottom < top
        assertFalse(ValidationUtils.isValidBounds(Bounds(0, 0, 0, 100))) // zero width
        assertFalse(ValidationUtils.isValidBounds(Bounds(0, 0, 100, 0))) // zero height
    }

    @Test
    fun `isValidBoundsString returns true for valid format`() {
        assertTrue(ValidationUtils.isValidBoundsString("10,20,110,70"))
        assertTrue(ValidationUtils.isValidBoundsString("0,0,1920,1080"))
    }

    @Test
    fun `isValidBoundsString returns false for invalid format`() {
        assertFalse(ValidationUtils.isValidBoundsString("invalid"))
        assertFalse(ValidationUtils.isValidBoundsString("10,20,30"))
        assertFalse(ValidationUtils.isValidBoundsString(""))
        assertFalse(ValidationUtils.isValidBoundsString("100,100,50,50")) // right < left
    }

    // ==================== Element Validation Tests ====================

    @Test
    fun `validateElement returns valid for correct element`() {
        val element = ElementInfo(
            className = "Button",
            text = "Submit",
            bounds = Bounds(0, 0, 100, 50)
        )

        val result = ValidationUtils.validateElement(element)

        assertTrue(result.isValid)
        assertFalse(result.hasIssues)
    }

    @Test
    fun `validateElement returns invalid for empty className`() {
        val element = ElementInfo(className = "")

        val result = ValidationUtils.validateElement(element)

        assertFalse(result.isValid)
        assertTrue(result.issues.contains("className is required"))
    }

    @Test
    fun `validateElement returns invalid for bad resourceId`() {
        val element = ElementInfo(
            className = "Button",
            resourceId = "invalid_format"
        )

        val result = ValidationUtils.validateElement(element)

        assertFalse(result.isValid)
        assertTrue(result.issues.any { it.contains("resourceId") })
    }

    @Test
    fun `validateElement returns invalid for bad bounds`() {
        val element = ElementInfo(
            className = "Button",
            bounds = Bounds(100, 100, 50, 50) // invalid
        )

        val result = ValidationUtils.validateElement(element)

        assertFalse(result.isValid)
        assertTrue(result.issues.any { it.contains("bounds") })
    }

    @Test
    fun `validateElement accepts empty bounds`() {
        val element = ElementInfo(
            className = "Button",
            bounds = Bounds.EMPTY
        )

        val result = ValidationUtils.validateElement(element)

        assertTrue(result.isValid)
    }

    // ==================== Voice Label Validation Tests ====================

    @Test
    fun `isValidVoiceLabel returns true for valid labels`() {
        assertTrue(ValidationUtils.isValidVoiceLabel("Submit"))
        assertTrue(ValidationUtils.isValidVoiceLabel("Click Here"))
        assertTrue(ValidationUtils.isValidVoiceLabel("Button 1"))
    }

    @Test
    fun `isValidVoiceLabel returns false for invalid labels`() {
        assertFalse(ValidationUtils.isValidVoiceLabel(""))
        assertFalse(ValidationUtils.isValidVoiceLabel("   "))
        assertFalse(ValidationUtils.isValidVoiceLabel("12345")) // no letters
        assertFalse(ValidationUtils.isValidVoiceLabel("a".repeat(101))) // too long
    }

    // ==================== Sanitize Tests ====================

    @Test
    fun `sanitize removes control characters`() {
        val input = "Hello\u0000World\u001F"
        val result = ValidationUtils.sanitize(input)
        assertEquals("HelloWorld", result)
    }

    @Test
    fun `sanitize removes SQL injection characters`() {
        val input = "'; DROP TABLE users;--"
        val result = ValidationUtils.sanitize(input)
        assertFalse(result.contains("'"))
        assertFalse(result.contains(";"))
    }

    @Test
    fun `sanitize removes shell injection characters`() {
        val input = "test|whoami && cat /etc/passwd"
        val result = ValidationUtils.sanitize(input)
        assertFalse(result.contains("|"))
        assertFalse(result.contains("&"))
    }

    @Test
    fun `sanitize trims whitespace`() {
        val input = "  Hello World  "
        val result = ValidationUtils.sanitize(input)
        assertEquals("Hello World", result)
    }

    @Test
    fun `sanitize preserves safe content`() {
        val input = "Hello World 123"
        val result = ValidationUtils.sanitize(input)
        assertEquals("Hello World 123", result)
    }

    // ==================== Dangerous Content Detection Tests ====================

    @Test
    fun `containsDangerousContent detects SQL keywords`() {
        assertTrue(ValidationUtils.containsDangerousContent("DROP TABLE users"))
        assertTrue(ValidationUtils.containsDangerousContent("DELETE FROM users"))
        assertTrue(ValidationUtils.containsDangerousContent("INSERT INTO users"))
        assertTrue(ValidationUtils.containsDangerousContent("UPDATE SET password"))
    }

    @Test
    fun `containsDangerousContent detects SQL comment injection`() {
        assertTrue(ValidationUtils.containsDangerousContent("'; --"))
        assertTrue(ValidationUtils.containsDangerousContent("'; -- comment"))
    }

    @Test
    fun `containsDangerousContent detects template injection`() {
        assertTrue(ValidationUtils.containsDangerousContent("\${dangerous}"))
        assertTrue(ValidationUtils.containsDangerousContent("`command`"))
    }

    @Test
    fun `containsDangerousContent returns false for safe content`() {
        assertFalse(ValidationUtils.containsDangerousContent("Hello World"))
        assertFalse(ValidationUtils.containsDangerousContent("Click button to delete item"))
        assertFalse(ValidationUtils.containsDangerousContent("Update your profile"))
    }

    // ==================== ValidationResult Tests ====================

    @Test
    fun `ValidationResult getIssuesString formats correctly`() {
        val result = ValidationResult(
            isValid = false,
            issues = listOf("Error 1", "Error 2", "Error 3")
        )

        assertEquals("Error 1; Error 2; Error 3", result.getIssuesString())
        assertEquals("Error 1, Error 2, Error 3", result.getIssuesString(", "))
    }

    @Test
    fun `ValidationResult hasIssues reflects issues presence`() {
        val valid = ValidationResult(isValid = true)
        val invalid = ValidationResult(isValid = false, issues = listOf("Error"))

        assertFalse(valid.hasIssues)
        assertTrue(invalid.hasIssues)
    }
}
