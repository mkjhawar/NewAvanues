package com.augmentalis.avaelements.flutter.material.input

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Modifier
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * FormSection component - Flutter Material parity
 *
 * A container for grouping related form fields with an optional header and description.
 * Provides visual separation and semantic grouping of form inputs.
 *
 * **Flutter Equivalent:** `FormSection` (custom or from form_builder_validators)
 * **Material Design 3:** Card with header and content area
 *
 * ## Features
 * - Groups related form fields
 * - Optional section header
 * - Optional description text
 * - Visual separation (dividers/spacing)
 * - Collapsible sections (optional)
 * - Material3 theming support
 * - Dark mode support
 * - TalkBack accessibility
 * - WCAG 2.1 Level AA compliant
 *
 * ## Usage Example
 * ```kotlin
 * FormSection(
 *     title = "Personal Information",
 *     description = "Enter your basic details",
 *     children = listOf(
 *         TextField(label = "First Name"),
 *         TextField(label = "Last Name"),
 *         PhoneInput(label = "Phone")
 *     ),
 *     collapsible = true,
 *     expanded = true
 * )
 * ```
 *
 * @property id Unique identifier for the component
 * @property title Section header title
 * @property description Optional description text below title
 * @property children List of form field components
 * @property collapsible Whether the section can be collapsed
 * @property expanded Whether the section is expanded (if collapsible)
 * @property showDivider Whether to show divider at bottom
 * @property contentDescription Accessibility description
 * @property onExpandChange Callback invoked when expand state changes
 * @property style Optional style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 3.0.0-flutter-parity
 */
data class FormSection(
    override val type: String = "FormSection",
    override val id: String? = null,
    val title: String? = null,
    val description: String? = null,
    val children: List<Component> = emptyList(),
    val collapsible: Boolean = false,
    val expanded: Boolean = true,
    val showDivider: Boolean = true,
    val contentDescription: String? = null,
    @Transient
    val onExpandChange: ((Boolean) -> Unit)? = null,
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
        val base = contentDescription ?: title ?: "Form section"
        val fieldCount = ", ${children.size} fields"
        val collapsibleState = if (collapsible) {
            if (expanded) ", expanded" else ", collapsed"
        } else ""
        return "$base$fieldCount$collapsibleState"
    }
}
