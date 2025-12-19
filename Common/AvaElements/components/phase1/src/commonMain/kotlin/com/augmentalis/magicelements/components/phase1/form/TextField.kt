package com.augmentalis.avaelements.components.phase1.form

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Modifier
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * Text input field component
 *
 * A cross-platform text input field for user text entry.
 * Supports single/multi-line input, placeholders, validation, and callbacks.
 *
 * @property id Unique identifier for the component
 * @property value Current text value
 * @property placeholder Optional placeholder text shown when empty
 * @property label Optional label text displayed above field
 * @property enabled Whether the field is enabled for user interaction
 * @property readOnly Whether the field is read-only (displays but cannot edit)
 * @property multiline Whether the field supports multiple lines
 * @property maxLength Maximum number of characters allowed (null = unlimited)
 * @property error Optional error message to display
 * @property inputType Type of input (text, number, email, password, etc.)
 * @property onChange Callback invoked when text value changes (not serialized)
 * @property onFocus Callback invoked when field gains focus (not serialized)
 * @property onBlur Callback invoked when field loses focus (not serialized)
 * @property style Optional style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 2.0.0
 */

data class TextField(
    override val type: String = "TextField",
    override val id: String? = null,
    val value: String = "",
    val placeholder: String? = null,
    val label: String? = null,
    val enabled: Boolean = true,
    val readOnly: Boolean = false,
    val multiline: Boolean = false,
    val maxLength: Int? = null,
    val error: String? = null,
    val inputType: InputType = InputType.Text,
    @Transient
    val onChange: ((String) -> Unit)? = null,
    @Transient
    val onFocus: (() -> Unit)? = null,
    @Transient
    val onBlur: (() -> Unit)? = null,
    override val style: ComponentStyle? = null,
    @Transient
    override val modifiers: List<Modifier> = emptyList()
) : Component {

    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }

    /**
     * Input type enumeration
     */
    
    enum class InputType {
        /** Plain text input */
        Text,

        /** Numeric input */
        Number,

        /** Email address input */
        Email,

        /** Password input (masked) */
        Password,

        /** Phone number input */
        Phone,

        /** URL input */
        Url,

        /** Search input */
        Search
    }

    companion object {
        /**
         * Create a standard text field
         */
        fun text(
            value: String = "",
            placeholder: String? = null,
            label: String? = null,
            onChange: ((String) -> Unit)? = null
        ) = TextField(
            value = value,
            placeholder = placeholder,
            label = label,
            onChange = onChange
        )

        /**
         * Create a password field
         */
        fun password(
            value: String = "",
            placeholder: String? = null,
            label: String? = null,
            onChange: ((String) -> Unit)? = null
        ) = TextField(
            value = value,
            placeholder = placeholder,
            label = label,
            inputType = InputType.Password,
            onChange = onChange
        )

        /**
         * Create a multiline text area
         */
        fun multiline(
            value: String = "",
            placeholder: String? = null,
            label: String? = null,
            onChange: ((String) -> Unit)? = null
        ) = TextField(
            value = value,
            placeholder = placeholder,
            label = label,
            multiline = true,
            onChange = onChange
        )
    }
}
