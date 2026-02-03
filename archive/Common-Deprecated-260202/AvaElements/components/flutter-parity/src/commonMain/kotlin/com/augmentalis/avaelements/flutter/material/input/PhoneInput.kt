package com.augmentalis.avaelements.flutter.material.input

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Modifier
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * PhoneInput component - Flutter Material parity
 *
 * A specialized input field for phone numbers with country code selection and automatic formatting.
 * Supports international phone number standards.
 *
 * **Flutter Equivalent:** `PhoneNumberField` (from flutter_phone_number_input)
 * **Material Design 3:** Custom implementation using TextField
 *
 * ## Features
 * - Country code selection with dropdown
 * - Automatic phone number formatting per country
 * - Input validation
 * - Error state handling
 * - Material3 theming support
 * - Dark mode support
 * - TalkBack accessibility
 * - WCAG 2.1 Level AA compliant
 *
 * ## Usage Example
 * ```kotlin
 * PhoneInput(
 *     value = "+1 (555) 123-4567",
 *     countryCode = "US",
 *     label = "Phone Number",
 *     onValueChange = { phone ->
 *         // Handle phone number change
 *     },
 *     onCountryCodeChange = { code ->
 *         // Handle country code change
 *     }
 * )
 * ```
 *
 * @property id Unique identifier for the component
 * @property value Current phone number value
 * @property countryCode ISO 3166-1 alpha-2 country code (e.g., "US", "GB")
 * @property label Label text displayed above the input
 * @property placeholder Placeholder text when empty
 * @property enabled Whether the input is enabled
 * @property required Whether the field is required
 * @property errorText Error message to display (null if valid)
 * @property contentDescription Accessibility description
 * @property onValueChange Callback invoked when phone number changes
 * @property onCountryCodeChange Callback invoked when country code changes
 * @property style Optional style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 3.0.0-flutter-parity
 */
data class PhoneInput(
    override val type: String = "PhoneInput",
    override val id: String? = null,
    val value: String = "",
    val countryCode: String = "US",
    val label: String? = null,
    val placeholder: String? = null,
    val enabled: Boolean = true,
    val required: Boolean = false,
    val errorText: String? = null,
    val contentDescription: String? = null,
    @Transient
    val onValueChange: ((String) -> Unit)? = null,
    @Transient
    val onCountryCodeChange: ((String) -> Unit)? = null,
    override val style: ComponentStyle? = null,
    @Transient
    override val modifiers: List<Modifier> = emptyList()
) : Component {

    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }

    /**
     * Get effective accessibility description
     */
    fun getAccessibilityDescription(): String {
        val base = contentDescription ?: label ?: "Phone number input"
        val requiredState = if (required) ", required" else ""
        val errorState = if (errorText != null) ", error: $errorText" else ""
        return "$base$requiredState$errorState"
    }

    companion object {
        /**
         * Common country codes with dial codes
         */
        val COUNTRY_CODES = mapOf(
            "US" to "+1",
            "GB" to "+44",
            "CA" to "+1",
            "IN" to "+91",
            "AU" to "+61",
            "DE" to "+49",
            "FR" to "+33",
            "JP" to "+81",
            "CN" to "+86"
        )
    }
}
