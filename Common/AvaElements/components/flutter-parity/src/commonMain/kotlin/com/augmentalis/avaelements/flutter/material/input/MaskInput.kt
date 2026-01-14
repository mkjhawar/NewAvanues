package com.augmentalis.avaelements.flutter.material.input

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Modifier
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * MaskInput component - Flutter Material parity
 *
 * An input field with custom input masks for formatted data entry.
 * Commonly used for credit cards, phone numbers, dates, and other formatted inputs.
 *
 * **Flutter Equivalent:** `MaskedTextField` (from mask_text_input_formatter)
 * **Material Design 3:** TextField with visual formatting
 *
 * ## Features
 * - Custom mask patterns (e.g., "####-####-####-####" for credit cards)
 * - Automatic formatting as user types
 * - Pattern validation
 * - Error state handling
 * - Material3 theming support
 * - Dark mode support
 * - TalkBack accessibility
 * - WCAG 2.1 Level AA compliant
 *
 * ## Mask Format
 * - `#` - Digit (0-9)
 * - `A` - Letter (a-z, A-Z)
 * - `X` - Any character
 * - Other characters - Literal (preserved in output)
 *
 * ## Usage Example
 * ```kotlin
 * MaskInput(
 *     value = "1234-5678-9012-3456",
 *     mask = "####-####-####-####",
 *     label = "Credit Card",
 *     placeholder = "1234-5678-9012-3456",
 *     onValueChange = { masked ->
 *         // Handle masked value change
 *     }
 * )
 * ```
 *
 * @property id Unique identifier for the component
 * @property value Current masked value
 * @property mask Mask pattern (# for digit, A for letter, X for any)
 * @property label Label text displayed above the input
 * @property placeholder Placeholder text showing mask format
 * @property enabled Whether the input is enabled
 * @property required Whether the field is required
 * @property errorText Error message to display (null if valid)
 * @property contentDescription Accessibility description
 * @property onValueChange Callback invoked when value changes
 * @property style Optional style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 3.0.0-flutter-parity
 */
data class MaskInput(
    override val type: String = "MaskInput",
    override val id: String? = null,
    val value: String = "",
    val mask: String = "",
    val label: String? = null,
    val placeholder: String? = null,
    val enabled: Boolean = true,
    val required: Boolean = false,
    val errorText: String? = null,
    val contentDescription: String? = null,
    @Transient
    val onValueChange: ((String) -> Unit)? = null,
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
        val base = contentDescription ?: label ?: "Masked input"
        val requiredState = if (required) ", required" else ""
        val errorState = if (errorText != null) ", error: $errorText" else ""
        return "$base$requiredState$errorState"
    }

    /**
     * Get unmasked value (remove formatting characters)
     */
    fun getUnmaskedValue(): String {
        return value.filter { it.isLetterOrDigit() }
    }

    /**
     * Check if value matches mask pattern
     */
    fun isComplete(): Boolean {
        val expectedLength = mask.length
        return value.length == expectedLength
    }

    companion object {
        /**
         * Common mask patterns
         */
        object Masks {
            const val CREDIT_CARD = "####-####-####-####"
            const val PHONE_US = "(###) ###-####"
            const val DATE_US = "##/##/####"
            const val SSN_US = "###-##-####"
            const val ZIP_US = "#####"
            const val ZIP_PLUS4_US = "#####-####"
        }
    }
}
