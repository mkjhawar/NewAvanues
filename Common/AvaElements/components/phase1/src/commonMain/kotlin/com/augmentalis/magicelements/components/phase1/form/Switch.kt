package com.augmentalis.avaelements.components.phase1.form

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Modifier
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * Switch component for toggle selection
 *
 * A cross-platform toggle switch (like iOS UISwitch) for on/off states.
 * Similar to checkbox but with different visual representation.
 *
 * @property id Unique identifier for the component
 * @property checked Current checked state (on/off)
 * @property label Optional label text displayed next to switch
 * @property enabled Whether the switch is enabled for user interaction
 * @property onChange Callback invoked when checked state changes (not serialized)
 * @property style Optional style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 2.0.0
 */

data class Switch(
    override val type: String = "Switch",
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
    fun toggle(): Switch = copy(checked = !checked)

    companion object {
        /**
         * Create a switch with Material Design style
         */
        fun material(
            checked: Boolean = false,
            label: String? = null,
            enabled: Boolean = true,
            onChange: ((Boolean) -> Unit)? = null
        ) = Switch(
            checked = checked,
            label = label,
            enabled = enabled,
            onChange = onChange
        )
    }
}
