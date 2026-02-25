package com.augmentalis.avacode

import com.augmentalis.avacode.forms.ValidationException
import com.augmentalis.avacode.forms.ValidationResult
import com.augmentalis.avacode.forms.form
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FormBindingTest {

    private fun profileForm() = form("profile") {
        textField("name") {
            required()
            minLength(2)
        }
        textField("bio") {}
    }

    // ─── FormBinding tests ─────────────────────────────────────────────────────

    @Test
    fun getReturnsInitialData() {
        val binding = profileForm().bind(mapOf("name" to "Alice", "bio" to "Engineer"))
        assertEquals("Alice", binding["name"])
        assertEquals("Engineer", binding["bio"])
    }

    @Test
    fun setValidValueUpdatesData() {
        val binding = profileForm().bind()
        binding["name"] = "Bob"
        assertEquals("Bob", binding["name"])
    }

    @Test
    fun setInvalidValueThrowsValidationException() {
        val binding = profileForm().bind()
        val ex = assertFailsWith<ValidationException> {
            binding["name"] = ""  // Required field — empty string fails Required rule
        }
        assertEquals("name", ex.fieldId)
    }

    @Test
    fun hasChangesReturnsTrueAfterModification() {
        val binding = profileForm().bind(mapOf("name" to "Alice"))
        assertFalse(binding.hasChanges())
        binding["name"] = "Bob"
        assertTrue(binding.hasChanges())
    }

    @Test
    fun resetRestoresInitialValues() {
        val binding = profileForm().bind(mapOf("name" to "Alice"))
        binding["name"] = "Bob"
        binding.reset()
        assertEquals("Alice", binding["name"])
        assertFalse(binding.hasChanges())
    }

    @Test
    fun getChangesReturnsOnlyModifiedFields() {
        val binding = profileForm().bind(mapOf("name" to "Alice"))
        binding["name"] = "Carol"
        val changes = binding.getChanges()
        assertTrue(changes.containsKey("name"))
        assertEquals("Alice", changes["name"]?.oldValue)
        assertEquals("Carol", changes["name"]?.newValue)
    }

    @Test
    fun onChangeListenerFiredOnUpdate() {
        val binding = profileForm().bind()
        var fired = false
        binding.onChange { fieldId, _ -> if (fieldId == "name") fired = true }
        binding["name"] = "Dave"
        assertTrue(fired, "onChange listener was not fired")
    }

    @Test
    fun validateFiresValidationListeners() {
        val binding = profileForm().bind()
        var lastResult: ValidationResult? = null
        binding.onValidation { lastResult = it }
        binding.validate()
        assertTrue(lastResult != null, "onValidation listener was not fired")
        assertTrue(lastResult!!.isInvalid, "Expected validation failure for empty required field")
    }
}
