package com.augmentalis.avaelements.flutter.material.input

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Modifier
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * UrlInput component - Flutter Material parity
 *
 * A specialized input field for URLs with validation and protocol handling.
 * Automatically validates URL format and provides protocol suggestions.
 *
 * **Flutter Equivalent:** `TextFormField` with URL keyboard type and validation
 * **Material Design 3:** TextField with specialized configuration
 *
 * ## Features
 * - URL format validation
 * - Protocol prefix handling (http://, https://)
 * - Keyboard type optimization (URL)
 * - Error state handling
 * - Material3 theming support
 * - Dark mode support
 * - TalkBack accessibility
 * - WCAG 2.1 Level AA compliant
 *
 * ## Usage Example
 * ```kotlin
 * UrlInput(
 *     value = "https://example.com",
 *     label = "Website",
 *     placeholder = "Enter URL",
 *     onValueChange = { url ->
 *         // Handle URL change
 *     },
 *     autoAddProtocol = true
 * )
 * ```
 *
 * @property id Unique identifier for the component
 * @property value Current URL value
 * @property label Label text displayed above the input
 * @property placeholder Placeholder text when empty
 * @property enabled Whether the input is enabled
 * @property required Whether the field is required
 * @property errorText Error message to display (null if valid)
 * @property autoAddProtocol Automatically add "https://" if missing
 * @property contentDescription Accessibility description
 * @property onValueChange Callback invoked when URL changes
 * @property style Optional style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 3.0.0-flutter-parity
 */
data class UrlInput(
    override val type: String = "UrlInput",
    override val id: String? = null,
    val value: String = "",
    val label: String? = null,
    val placeholder: String? = null,
    val enabled: Boolean = true,
    val required: Boolean = false,
    val errorText: String? = null,
    val autoAddProtocol: Boolean = true,
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
        val base = contentDescription ?: label ?: "URL input"
        val requiredState = if (required) ", required" else ""
        val errorState = if (errorText != null) ", error: $errorText" else ""
        return "$base$requiredState$errorState"
    }

    /**
     * Validate URL format
     */
    fun isValid(): Boolean {
        if (value.isBlank()) return !required
        return value.matches(Regex("^(https?://)?[\\w\\-]+(\\.[\\w\\-]+)+[/#?]?.*\$"))
    }

    companion object {
        /**
         * Common URL protocols
         */
        val PROTOCOLS = listOf("https://", "http://", "ftp://")
    }
}
