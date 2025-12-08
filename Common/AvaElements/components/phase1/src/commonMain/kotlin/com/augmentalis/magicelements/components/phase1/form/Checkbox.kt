package com.augmentalis.avaelements.components.phase1.form

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Modifier
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * Checkbox component for binary selection
 *
 * A cross-platform checkbox that allows users to select/deselect options.
 * Supports checked/unchecked states, labels, enabled/disabled states, and callbacks.
 *
 * @property id Unique identifier for the component
 * @property checked Current checked state
 * @property label Optional label text displayed next to checkbox
 * @property enabled Whether the checkbox is enabled for user interaction
 * @property onChange Callback invoked when checked state changes (not serialized)
 * @property style Optional style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 2.0.0
 */

data class Checkbox(
    override val type: String = "Checkbox",
    override val id: String? = null,
    val checked: Boolean = false,
    val label: String? = null,
    val enabled: Boolean = true,
    @Transient
    val onChange: ((Boolean) -> Unit)? = null,
    override val style: ComponentStyle? = null,
    @Transient
    override val modifiers: List<Modifier> = emptyList()
) : Component {

    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }

    /**
     * Create a copy with the checked state toggled
     */
    fun toggle(): Checkbox = copy(checked = !checked)

    companion object {
        /**
         * Create a checkbox with Material Design style
         */
        fun material(
            checked: Boolean = false,
            label: String? = null,
            enabled: Boolean = true,
            onChange: ((Boolean) -> Unit)? = null
        ) = Checkbox(
            checked = checked,
            label = label,
            enabled = enabled,
            onChange = onChange
        )
    }
}
