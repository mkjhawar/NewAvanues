/**
 * ElementCommandDTOTest.kt - Unit tests for ElementCommandDTO validation
 *
 * Part of VOS-META-001 Phase 1 testing
 * Created: 2025-12-03
 *
 * Tests ElementCommandDTO data class, validation methods, and extension functions.
 */
package com.augmentalis.voiceoscore.commands

import com.augmentalis.database.dto.ElementCommandDTO
import org.junit.Assert.*
import org.junit.Test

class ElementCommandDTOTest {

    @Test
    fun `valid command passes validation`() {
        val command = ElementCommandDTO(
            elementUuid = "abc-123",
            commandPhrase = "submit button",
            createdAt = System.currentTimeMillis(),
            appId = "com.example.app"
        )

        assertTrue("Valid command should pass validation", command.isValid())
    }

    @Test
    fun `command phrase too short fails validation`() {
        val command = ElementCommandDTO(
            elementUuid = "abc-123",
            commandPhrase = "ab", // Only 2 chars
            createdAt = System.currentTimeMillis(),
            appId = "com.example.app"
        )

        assertFalse("Command phrase < 3 chars should fail", command.isValid())
    }

    @Test
    fun `command phrase too long fails validation`() {
        val command = ElementCommandDTO(
            elementUuid = "abc-123",
            commandPhrase = "a".repeat(51), // 51 chars
            createdAt = System.currentTimeMillis(),
            appId = "com.example.app"
        )

        assertFalse("Command phrase > 50 chars should fail", command.isValid())
    }

    @Test
    fun `command phrase with special characters fails validation`() {
        val command = ElementCommandDTO(
            elementUuid = "abc-123",
            commandPhrase = "submit@button", // Contains @
            createdAt = System.currentTimeMillis(),
            appId = "com.example.app"
        )

        assertFalse("Command phrase with special chars should fail", command.isValid())
    }

    @Test
    fun `command phrase with numbers passes validation`() {
        val command = ElementCommandDTO(
            elementUuid = "abc-123",
            commandPhrase = "item 123",
            createdAt = System.currentTimeMillis(),
            appId = "com.example.app"
        )

        assertTrue("Command phrase with numbers should pass", command.isValid())
    }

    @Test
    fun `empty element uuid fails validation`() {
        val command = ElementCommandDTO(
            elementUuid = "", // Empty
            commandPhrase = "submit button",
            createdAt = System.currentTimeMillis(),
            appId = "com.example.app"
        )

        assertFalse("Empty element UUID should fail", command.isValid())
    }

    @Test
    fun `blank element uuid fails validation`() {
        val command = ElementCommandDTO(
            elementUuid = "   ", // Blank
            commandPhrase = "submit button",
            createdAt = System.currentTimeMillis(),
            appId = "com.example.app"
        )

        assertFalse("Blank element UUID should fail", command.isValid())
    }

    @Test
    fun `empty app id fails validation`() {
        val command = ElementCommandDTO(
            elementUuid = "abc-123",
            commandPhrase = "submit button",
            createdAt = System.currentTimeMillis(),
            appId = "" // Empty
        )

        assertFalse("Empty app ID should fail", command.isValid())
    }

    @Test
    fun `getSanitizedPhrase removes extra spaces`() {
        val command = ElementCommandDTO(
            elementUuid = "abc-123",
            commandPhrase = "  submit   button  ",
            createdAt = System.currentTimeMillis(),
            appId = "com.example.app"
        )

        assertEquals("submit button", command.getSanitizedPhrase())
    }

    @Test
    fun `getSanitizedPhrase converts to lowercase`() {
        val command = ElementCommandDTO(
            elementUuid = "abc-123",
            commandPhrase = "Submit Button",
            createdAt = System.currentTimeMillis(),
            appId = "com.example.app"
        )

        assertEquals("submit button", command.getSanitizedPhrase())
    }

    @Test
    fun `getSanitizedPhrase handles mixed case and spaces`() {
        val command = ElementCommandDTO(
            elementUuid = "abc-123",
            commandPhrase = "  SUBMIT   Button  ",
            createdAt = System.currentTimeMillis(),
            appId = "com.example.app"
        )

        assertEquals("submit button", command.getSanitizedPhrase())
    }

    @Test
    fun `default values are set correctly`() {
        val command = ElementCommandDTO(
            elementUuid = "abc-123",
            commandPhrase = "submit button",
            createdAt = System.currentTimeMillis(),
            appId = "com.example.app"
        )

        assertEquals("Default ID should be 0", 0L, command.id)
        assertEquals("Default confidence should be 1.0", 1.0, command.confidence, 0.001)
        assertEquals("Default createdBy should be 'user'", "user", command.createdBy)
        assertFalse("Default isSynonym should be false", command.isSynonym)
    }

    @Test
    fun `synonym flag is preserved`() {
        val command = ElementCommandDTO(
            elementUuid = "abc-123",
            commandPhrase = "submit button",
            createdAt = System.currentTimeMillis(),
            appId = "com.example.app",
            isSynonym = true
        )

        assertTrue("Synonym flag should be preserved", command.isSynonym)
    }

    @Test
    fun `confidence value is preserved`() {
        val command = ElementCommandDTO(
            elementUuid = "abc-123",
            commandPhrase = "submit button",
            confidence = 0.85,
            createdAt = System.currentTimeMillis(),
            appId = "com.example.app"
        )

        assertEquals("Confidence should be preserved", 0.85, command.confidence, 0.001)
    }
}
