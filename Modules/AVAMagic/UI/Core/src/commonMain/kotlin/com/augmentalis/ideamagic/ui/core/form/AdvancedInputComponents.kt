package com.augmentalis.avanues.avamagic.ui.core.form

import com.augmentalis.avanues.avamagic.components.core.*

/**
 * SegmentedButton - Toggle button groups
 */
data class SegmentedButtonComponent(
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList(),
    val segments: List<SegmentItem>,
    val selectedIndices: List<Int> = emptyList(),
    val multiSelect: Boolean = false,
    val enabled: Boolean = true,
    val onSelectionChanged: ((List<Int>) -> Unit)? = null
) : Component {
    override fun render(renderer: Renderer): Any = renderer.renderComponent(this)
}

data class SegmentItem(
    val label: String,
    val icon: String? = null,
    val enabled: Boolean = true
)

/**
 * TextButton - Minimal button for secondary actions
 */
data class TextButtonComponent(
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList(),
    val label: String,
    val icon: String? = null,
    val iconPosition: IconPosition = IconPosition.Start,
    val enabled: Boolean = true,
    val onClick: (() -> Unit)? = null
) : Component {
    override fun render(renderer: Renderer): Any = renderer.renderComponent(this)
}

/**
 * OutlinedButton - Medium-emphasis outlined button
 */
data class OutlinedButtonComponent(
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList(),
    val label: String,
    val icon: String? = null,
    val iconPosition: IconPosition = IconPosition.Start,
    val enabled: Boolean = true,
    val onClick: (() -> Unit)? = null
) : Component {
    override fun render(renderer: Renderer): Any = renderer.renderComponent(this)
}

/**
 * FilledButton - Primary filled button (high emphasis)
 */
data class FilledButtonComponent(
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList(),
    val label: String,
    val icon: String? = null,
    val iconPosition: IconPosition = IconPosition.Start,
    val enabled: Boolean = true,
    val containerColor: Color? = null,
    val contentColor: Color? = null,
    val onClick: (() -> Unit)? = null
) : Component {
    override fun render(renderer: Renderer): Any = renderer.renderComponent(this)
}

/**
 * IconButton - Button with just an icon
 */
data class IconButtonComponent(
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList(),
    val icon: String,
    val contentDescription: String? = null,
    val enabled: Boolean = true,
    val selected: Boolean = false,
    val variant: IconButtonVariant = IconButtonVariant.STANDARD,
    val onClick: (() -> Unit)? = null
) : Component {
    override fun render(renderer: Renderer): Any = renderer.renderComponent(this)
}

enum class IconButtonVariant {
    STANDARD, FILLED, FILLED_TONAL, OUTLINED
}
