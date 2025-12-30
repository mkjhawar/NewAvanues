package com.augmentalis.avaelements.flutter.material.input

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Modifier
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * PinInput component - Flutter Material parity
 *
 * A specialized input field for PIN code entry (4-8 digits).
 * Displays individual boxes for each digit with automatic focus advancement.
 *
 * **Flutter Equivalent:** `PinCodeTextField` (from pin_code_fields package)
 * **Material Design 3:** Custom implementation using TextField
 *
 * ## Features
 * - Individual digit boxes (4-8 digits)
 * - Automatic focus advancement
 * - Backspace handling
 * - Masked/unmasked display
 * - Error state handling
 * - Material3 theming support
 * - Dark mode support
 * - TalkBack accessibility
 * - WCAG 2.1 Level AA compliant
 *
 * ## Usage Example
 * ```kotlin
 * PinInput(
 *     value = "1234",
 *     length = 4,
 *     label = "Enter PIN",
 *     masked = true,
 *     onValueChange = { pin ->
 *         // Handle PIN change
 *     },
 *     onComplete = { pin ->
 *         // Handle PIN completion
 *     }
 * )
 * ```
 *
 * @property id Unique identifier for the component
 * @property value Current PIN value (digits only)
 * @property length Number of PIN digits (4-8)
 * @property label Label text displayed above the input
 * @property enabled Whether the input is enabled
 * @property masked Whether to mask digits (show bullets)
 * @property errorText Error message to display (null if valid)
 * @property contentDescription Accessibility description
 * @property onValueChange Callback invoked when PIN changes
 * @property onComplete Callback invoked when PIN is complete
 * @property style Optional style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 3.0.0-flutter-parity
 */
data class PinInput(
    override val type: String = "PinInput",
    override val id: String? = null,
    val value: String = "",
    val length: Int = 4,
    val label: String? = null,
    val enabled: Boolean = true,
    val masked: Boolean = true,
    val errorText: String? = null,
    val contentDescription: String? = null,
    @Transient
    val onValueChange: ((String) -> Unit)? = null,
    @Transient
    val onComplete: ((String) -> Unit)? = null,
    override val style: ComponentStyle? = null,
    @Transient
    override val modifiers: List<Modifier> = emptyList()
) : Component {

    init {
        require(length in 4..8) { "PIN length must be between 4 and 8 digits" }
    }

    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }

    /**
     * Get effective accessibility description
     */
    fun getAccessibilityDescription(): String {
        val base = contentDescription ?: label ?: "PIN input"
        val lengthInfo = ", $length digits"
        val maskedState = if (masked) ", masked" else ""
        val errorState = if (errorText != null) ", error: $errorText" else ""
        return "$base$lengthInfo$maskedState$errorState"
    }

    /**
     * Check if PIN is complete
     */
    fun isComplete(): Boolean = value.length == length

    /**
     * Validate that value contains only digits
     */
    fun isValid(): Boolean = value.all { it.isDigit() } && value.length <= length
}
