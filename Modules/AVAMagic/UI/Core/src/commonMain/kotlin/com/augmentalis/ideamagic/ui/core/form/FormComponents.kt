package com.augmentalis.avanues.avamagic.ui.core.form

import com.augmentalis.avanues.avamagic.components.core.*

/**
 * Button component - clickable button with various styles
 */
data class ButtonComponent(
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList(),
    val text: String,
    val onClick: (() -> Unit)? = null,
    val variant: ButtonVariant = ButtonVariant.Filled,
    val buttonStyle: ButtonScope = ButtonScope.Filled, // For mapper compatibility
    val size: ComponentSize = ComponentSize.MD,
    val enabled: Boolean = true,
    val loading: Boolean = false,
    val icon: String? = null,
    val iconPosition: IconPosition = IconPosition.Start,
    val fullWidth: Boolean = false
) : Component {
    override fun render(renderer: Renderer): Any = renderer.renderComponent(this)
}

/**
 * TextField component - text input field
 */
data class TextFieldComponent(
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList(),
    val value: String = "",
    val label: String? = null,
    val placeholder: String? = null,
    val helperText: String? = null,
    val errorText: String? = null,
    val isError: Boolean = false, // For mapper compatibility
    val errorMessage: String? = null, // For mapper compatibility
    val enabled: Boolean = true,
    val readOnly: Boolean = false,
    val required: Boolean = false,
    val multiline: Boolean = false,
    val maxLines: Int = 1,
    val minLines: Int = 1,
    val maxLength: Int? = null,
    val keyboardType: KeyboardType = KeyboardType.Text,
    val imeAction: ImeAction = ImeAction.Done,
    val size: ComponentSize = ComponentSize.MD,
    val leadingIcon: String? = null,
    val trailingIcon: String? = null,
    val onValueChange: ((String) -> Unit)? = null,
    val onFocus: (() -> Unit)? = null,
    val onBlur: (() -> Unit)? = null
) : Component {
    override fun render(renderer: Renderer): Any = renderer.renderComponent(this)
}

// Button enums

enum class ButtonVariant {
    Filled,
    Outlined,
    Text,
    Elevated,
    Tonal
}

enum class IconPosition {
    Start,
    End
}

// ButtonScope for mapper compatibility
enum class ButtonScope {
    Filled,
    Outlined,
    Text,
    Elevated,
    Tonal
}

// TextField enums

enum class KeyboardType {
    Text,
    Number,
    Email,
    Phone,
    Password,
    Url,
    Decimal
}

enum class ImeAction {
    None,
    Go,
    Search,
    Send,
    Next,
    Done
}
