package com.augmentalis.avaelements.flutter.material.input

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Modifier
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * FormGroup component - Flutter Material parity
 *
 * A container for grouping related form fields with label, helper text, and validation state.
 * Provides visual grouping and validation feedback for sets of related form inputs.
 *
 * **Flutter Equivalent:** Custom FormGroup widget (commonly used in form builders)
 * **Material Design 3:** Grouped form fields with validation styling
 *
 * ## Features
 * - Groups related form fields visually
 * - Optional group label
 * - Helper text below group
 * - Required field indicator (*)
 * - Error state styling (red border)
 * - Error message display
 * - Theme-aware styling
 * - Dark mode support
 * - Accessibility labels
 * - WCAG 2.1 Level AA compliant
 *
 * ## Usage Example
 * ```kotlin
 * FormGroup(
 *     label = "Contact Information",
 *     helperText = "Provide at least one contact method",
 *     required = true,
 *     error = false,
 *     children = listOf(
 *         TextField(label = "Email"),
 *         PhoneInput(label = "Phone"),
 *         TextField(label = "Address")
 *     )
 * )
 * ```
 *
 * ## Error State Example
 * ```kotlin
 * FormGroup(
 *     label = "Payment Method",
 *     required = true,
 *     error = true,
 *     errorText = "Please select a payment method",
 *     children = listOf(
 *         RadioGroup(options = paymentOptions)
 *     )
 * )
 * ```
 *
 * @property id Unique identifier for the component
 * @property label Optional label text for the group
 * @property helperText Optional helper text shown below group (when no error)
 * @property required Whether the group contains required fields
 * @property error Whether the group is in error state
 * @property errorText Error message text (shown instead of helperText when error is true)
 * @property children List of form field components in this group
 * @property contentDescription Accessibility description
 * @property style Optional style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 3.0.0-flutter-parity
 */
data class FormGroup(
    override val type: String = "FormGroup",
    override val id: String? = null,
    val label: String? = null,
    val helperText: String? = null,
    val required: Boolean = false,
    val error: Boolean = false,
    val errorText: String? = null,
    val children: List<Component> = emptyList(),
    val contentDescription: String? = null,
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
        val base = contentDescription ?: label ?: "Form group"
        val requiredText = if (required) ", required" else ""
        val errorText = if (error) ", error: ${this.errorText}" else ""
        val fieldCount = ", ${children.size} fields"
        return "$base$requiredText$fieldCount$errorText"
    }

    /**
     * Get the text to display below the group (error text takes precedence)
     */
    fun getDisplayText(): String? {
        return if (error && errorText != null) {
            errorText
        } else {
            helperText
        }
    }
}
