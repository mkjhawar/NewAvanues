package com.augmentalis.avacode

import com.augmentalis.avacode.forms.FormMetadata
import com.augmentalis.avacode.forms.ValidationResult
import com.augmentalis.avacode.forms.form
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class FormDefinitionTest {

    private fun simpleForm() = form("user_reg") {
        textField("username") {
            required()
            minLength(3)
        }
        emailField("email") {
            required()
        }
    }

    // ─── FormDefinition tests ──────────────────────────────────────────────────

    @Test
    fun formIdAndFieldCountAreCorrect() {
        val def = simpleForm()
        assertEquals("user_reg", def.id)
        assertEquals(2, def.fields.size)
    }

    @Test
    fun blankFormIdThrows() {
        assertFailsWith<IllegalArgumentException> {
            form(" ") {
                textField("field") {}
            }
        }
    }

    @Test
    fun emptyFieldsThrows() {
        assertFailsWith<IllegalArgumentException> {
            form("empty_form") { /* no fields */ }
        }
    }

    @Test
    fun duplicateFieldIdThrows() {
        assertFailsWith<IllegalArgumentException> {
            form("dup_form") {
                textField("name") {}
                textField("name") {}
            }
        }
    }

    @Test
    fun validateReturnsSuccessForValidData() {
        val def = simpleForm()
        val data = mapOf<String, Any?>("username" to "alice", "email" to "alice@example.com")

        val result = def.validate(data)
        assertIs<ValidationResult.Success>(result)
        assertTrue(result.isValid)
    }

    @Test
    fun validateReturnsFailureForMissingRequired() {
        val def = simpleForm()
        val data = mapOf<String, Any?>("username" to "")

        val result = def.validate(data)
        assertIs<ValidationResult.Failure>(result)
        assertTrue(result.errors.isNotEmpty())
    }

    @Test
    fun completionStatusCalculatedCorrectly() {
        val def = simpleForm()
        val data = mapOf<String, Any?>("username" to "alice", "email" to null)

        val status = def.calculateCompletion(data)
        assertTrue(status.missingRequired.contains("email"))
        assertEquals(false, status.requiredComplete)
    }

    @Test
    fun metadataDefaultsAreApplied() {
        val def = form("settings_form", FormMetadata(title = "Settings")) {
            textField("pref") {}
        }
        assertEquals("Settings", def.metadata.title)
        assertEquals("1.0.0", def.metadata.version)
    }
}
