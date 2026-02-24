package com.augmentalis.avacode

import com.augmentalis.avacode.forms.ValidationRule
import com.augmentalis.avacode.forms.ValidationRuleResult
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ValidationRuleTest {

    // ─── ValidationRule tests ──────────────────────────────────────────────────

    @Test
    fun requiredRejectsNullAndBlank() {
        val rule = ValidationRule.Required
        assertIs<ValidationRuleResult.Invalid>(rule.validate(null))
        assertIs<ValidationRuleResult.Invalid>(rule.validate(""))
        assertIs<ValidationRuleResult.Invalid>(rule.validate("   "))
        assertIs<ValidationRuleResult.Valid>(rule.validate("hello"))
    }

    @Test
    fun minLengthPassesNullAsOptional() {
        val rule = ValidationRule.MinLength(5)
        assertIs<ValidationRuleResult.Valid>(rule.validate(null))
        assertIs<ValidationRuleResult.Invalid>(rule.validate("abc"))
        assertIs<ValidationRuleResult.Valid>(rule.validate("abcde"))
    }

    @Test
    fun maxLengthPassesNullAsOptional() {
        val rule = ValidationRule.MaxLength(3)
        assertIs<ValidationRuleResult.Valid>(rule.validate(null))
        assertIs<ValidationRuleResult.Invalid>(rule.validate("abcd"))
        assertIs<ValidationRuleResult.Valid>(rule.validate("ab"))
    }

    @Test
    fun emailRejectsInvalidFormats() {
        val rule = ValidationRule.Email
        assertIs<ValidationRuleResult.Invalid>(rule.validate("notanemail"))
        assertIs<ValidationRuleResult.Invalid>(rule.validate("missing@"))
        assertIs<ValidationRuleResult.Invalid>(rule.validate("@nodomain.com"))
        assertIs<ValidationRuleResult.Valid>(rule.validate("user@example.com"))
        assertIs<ValidationRuleResult.Valid>(rule.validate(null)) // optional
    }

    @Test
    fun patternMatchesOrRejects() {
        val rule = ValidationRule.Pattern(Regex("[0-9]+"), "Digits only")
        assertIs<ValidationRuleResult.Valid>(rule.validate("12345"))
        val invalid = rule.validate("abc")
        assertIs<ValidationRuleResult.Invalid>(invalid)
        assertTrue(invalid.errorMessage == "Digits only")
    }

    @Test
    fun customValidatorUsed() {
        val rule = ValidationRule.Custom(
            validator = { it is String && (it as String).startsWith("AVA") },
            errorMessage = "Must start with AVA"
        )
        assertIs<ValidationRuleResult.Valid>(rule.validate("AVACode"))
        assertIs<ValidationRuleResult.Invalid>(rule.validate("XYZ"))
    }

    @Test
    fun rangeRejectsOutOfBounds() {
        val rule = ValidationRule.Range(min = 1, max = 100)
        assertIs<ValidationRuleResult.Valid>(rule.validate(50))
        assertIs<ValidationRuleResult.Invalid>(rule.validate(0))
        assertIs<ValidationRuleResult.Invalid>(rule.validate(101))
        assertIs<ValidationRuleResult.Valid>(rule.validate(null)) // optional
    }
}
