package com.augmentalis.avaelements.flutter.material.input

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Modifier
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * OTPInput component - Flutter Material parity
 *
 * A specialized input field for one-time password (OTP) entry.
 * Similar to PinInput but specifically designed for OTP verification flows.
 *
 * **Flutter Equivalent:** `OtpTextField` (from otp_text_field package)
 * **Material Design 3:** Custom implementation using TextField
 *
 * ## Features
 * - Individual digit/character boxes (typically 6 digits)
 * - Automatic focus advancement
 * - Paste support for full OTP
 * - Auto-submit on completion
 * - Timer display for resend
 * - Error state handling
 * - Material3 theming support
 * - Dark mode support
 * - TalkBack accessibility
 * - WCAG 2.1 Level AA compliant
 *
 * ## Usage Example
 * ```kotlin
 * OTPInput(
 *     value = "123456",
 *     length = 6,
 *     label = "Enter OTP",
 *     onValueChange = { otp ->
 *         // Handle OTP change
 *     },
 *     onComplete = { otp ->
 *         // Verify OTP
 *     },
 *     autoSubmit = true
 * )
 * ```
 *
 * @property id Unique identifier for the component
 * @property value Current OTP value
 * @property length Number of OTP characters (typically 4-6)
 * @property label Label text displayed above the input
 * @property enabled Whether the input is enabled
 * @property autoSubmit Auto-submit when OTP is complete
 * @property errorText Error message to display (null if valid)
 * @property contentDescription Accessibility description
 * @property onValueChange Callback invoked when OTP changes
 * @property onComplete Callback invoked when OTP is complete
 * @property style Optional style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 3.0.0-flutter-parity
 */
data class OTPInput(
    override val type: String = "OTPInput",
    override val id: String? = null,
    val value: String = "",
    val length: Int = 6,
    val label: String? = null,
    val enabled: Boolean = true,
    val autoSubmit: Boolean = true,
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
        require(length in 4..8) { "OTP length must be between 4 and 8 characters" }
    }

    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }

    /**
     * Get effective accessibility description
     */
    fun getAccessibilityDescription(): String {
        val base = contentDescription ?: label ?: "OTP input"
        val lengthInfo = ", $length characters"
        val errorState = if (errorText != null) ", error: $errorText" else ""
        return "$base$lengthInfo$errorState"
    }

    /**
     * Check if OTP is complete
     */
    fun isComplete(): Boolean = value.length == length

    /**
     * Validate that value contains only alphanumeric characters
     */
    fun isValid(): Boolean = value.all { it.isLetterOrDigit() } && value.length <= length
}
